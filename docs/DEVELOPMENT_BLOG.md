# InfiniteChat 开发者日志

> **作者**：Dustopus  
> **技术栈**：Spring Boot 3.2.5 + Netty 4.1.108 + WebSocket + Kafka + Redis + MySQL + Nacos + Spring Cloud  
> **GitHub**：https://github.com/Dustopus/InfiniteChat  
> **协议**：AGPL-3.0

---

## 📌 项目概述

InfiniteChat（千言）是一个基于分布式微服务架构的企业级即时通讯系统。项目目标是从零搭建一个完整的 IM 后端，核心解决以下技术挑战：

- **实时消息推送**：摒弃 HTTP 轮询，采用 WebSocket 长连接实现毫秒级消息投递
- **高并发连接管理**：Netty 单机支撑 50,000+ 并发 WebSocket 连接
- **消息可靠性**：离线消息自动存储，上线同步，消息不丢不漏
- **消息有序性**：雪花算法生成全局唯一 ID，保证消息顺序
- **微服务架构**：服务独立部署，通过 Nacos 服务注册发现和 Kafka 消息队列实现异步解耦

这篇日志记录了从项目立项到 Docker 一键部署的完整开发过程，包括技术选型思考、踩坑记录和解决方案。

---

## 🗓️ Day 1: 立项与技术选型

**日期**：2024年3月15日

### 为什么做这个项目

在后端开发的日常工作中，我发现自己对"长连接"和"实时通信"的理解只停留在概念层面。HTTP 请求-响应模式是我最熟悉的范式，但对于 IM、直播弹幕、实时协作这类场景，传统的"一问一答"根本不够用。

我想深入理解：
1. 长连接是怎么管理的？成千上万条 TCP 连接怎么高效维护？
2. 消息怎么保证不丢？用户下线期间的消息怎么处理？
3. 高并发场景下系统怎么扛住？

所以决定从零搭建一个分布式 IM 系统——InfiniteChat。

### 技术选型思考

#### 为什么选择微服务而不是单体？

一开始其实考虑过单体架构——简单，一个人好管理。但想了几个问题后放弃了：

| 维度 | 单体架构 | 微服务架构 |
|------|---------|-----------|
| 扩展性 | 整体扩展，资源浪费 | 按服务粒度独立扩展 |
| 部署 | 全量部署，风险高 | 独立部署，回滚方便 |
| 技术栈 | 统一技术栈 | 各服务可选最优技术 |
| 故障隔离 | 一个模块挂了全挂 | 故障隔离，限流熔断 |

IM 系统的特点是：WebSocket 服务和普通 HTTP 服务的资源消耗模式完全不同。WebSocket 是"连接密集型"，需要大量内存维护连接；HTTP API 是"请求密集型"，需要更多 CPU。混在一起部署会互相影响。

**结论**：微服务架构。WebSocket 服务独立部署，按需扩展。

#### 为什么是 WebSocket 而不是轮询或 SSE？

这是整个项目最核心的技术决策，我认真对比了三种方案：

**1. HTTP 轮询（Polling）**
```
客户端每隔 2 秒发一次请求：GET /api/message/poll
服务器：有消息就返回，没消息就返回空
```
- 优点：实现简单，兼容性好
- 缺点：99% 的请求是空响应（浪费资源）；延迟取决于轮询间隔（至少 2 秒）；服务器压力随用户数线性增长
- 结论：**不适合 IM**。5000 个用户 = 每秒 2500 个请求，大部分是无意义的

**2. SSE（Server-Sent Events）**
```
客户端建立一个长连接，服务器单向推送数据
```
- 优点：比轮询高效，服务器可主动推送
- 缺点：**单向通信**，服务器→客户端。客户端发消息还是要走 HTTP
- 结论：**半适合**。IM 需要双向通信（随时收发），SSE 只能满足"接收"方向

**3. WebSocket**
```
客户端和服务端建立全双工长连接，双方可随时互发消息
一次 TCP 握手 → 升级为 WebSocket 协议 → 持续通信
```
- 优点：全双工通信，毫秒级延迟，连接开销极低
- 缺点：协议比 HTTP 复杂，需要额外的心跳管理
- 结论：**IM 的最佳选择**

#### 为什么 Netty 而不是 Spring WebSocket？

Spring WebSocket 基于 Servlet 容器（Tomcat），受限于 Servlet 线程模型。一个 Tomcat 默认 200 个线程，撑死支持几千个并发连接。

Netty 基于 NIO，一个 EventLoop 线程可以处理数千个 Channel。对于 IM 这种"连接密集型"场景，Netty 是不二之选。

| 对比维度 | Spring WebSocket | Netty |
|---------|-----------------|-------|
| 并发能力 | 数千连接 | 数十万连接 |
| 内存管理 | JVM GC 压力大 | ByteBuf 池化内存 |
| 灵活度 | 较低 | Pipeline 机制高度可定制 |

#### 消息中间件选型

**Redis Pub/Sub**：轻量，适合小规模广播，但不持久化，消息可能丢失  
**RabbitMQ**：功能丰富，但高吞吐场景性能一般  
**Kafka**：高吞吐、持久化、分区有序，天然适合 IM 场景

最终决定**同时使用 Redis 和 Kafka**：
- **Kafka**：核心消息管道。消息持久化、离线消息、通知推送都走 Kafka
- **Redis**：高频读写场景。在线状态、联系人缓存、抢红包
- **Redis Pub/Sub**：群聊消息实时广播（群成员在线时直接推）

#### 版本选择

- Spring Boot 3.2.5（需要 JDK 17+）
- Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.0
- Netty 4.1.108.Final
- MyBatis-Plus 3.5.5
- JJWT 0.12.5

选择 Spring Boot 3 而不是 2，是因为想学习最新的技术栈。Jakarta EE 命名空间的变更确实踩了一些坑，但长痛不如短痛。

### 模块划分

基于领域驱动设计（DDD）的思路，将系统拆分为以下微服务：

```
com.dustopus
├── common                    # 公共模块（DTO、工具类、常量）
├── GatewayService            # API 网关（统一鉴权、路由）
├── AuthenticationService     # 认证服务（注册、登录）
├── RealTimeCommunicationService  # WebSocket 长连接管理（Netty）
├── ContactService            # 联系人服务（好友、群组）
├── MessagingService          # 消息服务（发送、历史记录）
├── OfflineService            # 离线消息服务
├── MomentService             # 朋友圈服务
└── NotifyService             # 通知服务
```

---

## 🗓️ Day 2: 环境搭建与项目初始化

**日期**：2024年3月16日

### 开发环境

- **JDK**：17（Spring Boot 3 要求 JDK 17+）
- **Maven**：3.9.x
- **MySQL**：8.0
- **Redis**：7.x
- **Kafka**：3.x
- **Nacos**：2.x
- **IDE**：IntelliJ IDEA 2024

### Maven 多模块项目搭建

使用 Maven 多模块管理整个项目。父 `pom.xml` 统一管理依赖版本：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<modules>
    <module>common</module>
    <module>AuthenticationService</module>
    <module>GatewayService</module>
    <module>RealTimeCommunicationService</module>
    <module>ContactService</module>
    <module>MessagingService</module>
    <module>OfflineService</module>
    <module>MomentService</module>
    <module>NotifyService</module>
</modules>
```

核心依赖版本统一在 `<dependencyManagement>` 中管理：

```xml
<spring-cloud.version>2023.0.1</spring-cloud.version>
<spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
<mybatis-plus.version>3.5.5</mybatis-plus.version>
<netty.version>4.1.108.Final</netty.version>
<jjwt.version>0.12.5</jjwt.version>
<redisson.version>3.29.0</redisson.version>
```

### common 模块设计

common 模块是整个项目的基石。所有微服务都依赖它，所以要保持干净、无状态。

**1. 统一响应体 `Result<T>`**

```java
public class Result<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) { ... }
    public static <T> Result<T> ok() { return ok(null); }
    public static <T> Result<T> fail(int code, String msg) { ... }
}
```

统一响应格式是微服务通信的基础。所有 API 返回 `Result<T>`，前端统一解析。

**2. 雪花算法 ID 生成器**

分布式系统中，数据库自增 ID 不能用——多个服务实例会产生冲突。Twitter 雪花算法（Snowflake）是业界标准方案：

```
64 位结构：
1 位符号位（始终为 0）
41 位时间戳（毫秒级，可用 69 年）
5 位数据中心 ID
5 位机器 ID
12 位序列号（每毫秒 4096 个 ID）
```

核心逻辑（简化版）：
```java
public synchronized long nextId() {
    long timestamp = System.currentTimeMillis();
    if (timestamp == lastTimestamp) {
        sequence = (sequence + 1) & MAX_SEQUENCE;
        if (sequence == 0) {
            timestamp = waitNextMillis(lastTimestamp); // 自旋等待下一毫秒
        }
    } else {
        sequence = 0L;
    }
    lastTimestamp = timestamp;
    return ((timestamp - START_TIMESTAMP) << 22)
         | (datacenterId << 17)
         | (machineId << 12)
         | sequence;
}
```

每个微服务配置不同的 `machineId`，保证全局唯一。

**3. JWT 工具类**

基于 JJWT 0.12.5 实现，HMAC-SHA256 签名：

```java
public static String generateToken(Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    return Jwts.builder()
            .claims(claims)
            .subject(String.valueOf(userId))
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000L * 7))
            .signWith(getKey())
            .compact();
}
```

Token 有效期 7 天。JWT Secret 通过环境变量注入，不同环境使用不同密钥。

**4. 全局异常处理**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
}
```

BusinessException 包含错误码，前端可根据错误码做不同的 UI 处理。

**5. 常量定义**

`RedisConstants`：Redis key 前缀统一管理  
`MessageConstants`：消息类型、Kafka Topic、Redis Channel 等常量

### Gateway Service 搭建

API 网关是微服务的统一入口。基于 Spring Cloud Gateway 实现：

**路由配置**：
```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
            .route("auth-service", r -> r.path("/api/v1/user/**")
                    .uri("lb://AuthenticationService"))
            .route("contact-service", r -> r.path("/api/v1/contact/**", "/api/v1/group/**")
                    .uri("lb://ContactService"))
            .route("messaging-service", r -> r.path("/api/v1/message/**", "/api/v1/redpacket/**")
                    .uri("lb://MessagingService"))
            // ... 其他路由
            .build();
}
```

**全局鉴权过滤器**：

```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    private static final String[] WHITE_LIST = {
        "/api/v1/user/login", "/api/v1/user/register", "/api/v1/user/sms/send"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        for (String white : WHITE_LIST) {
            if (path.startsWith(white)) return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = JwtUtil.getUserId(token);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
```

核心思路：白名单放行登录/注册接口；其他请求检查 Authorization 头的 JWT Token；验证通过后从 Token 提取 userId，通过 `X-User-Id` 请求头传递给下游服务。

### Nacos 服务注册

所有微服务通过 Nacos 进行服务注册和发现。配置通过环境变量注入：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
```

### 遇到的问题

**问题 1**：Spring Cloud Gateway 和 Spring Boot 3 的 WebFlux 依赖冲突。  
**解决**：Gateway 不能引入 `spring-boot-starter-web`（它用的是 WebFlux）。如果引入了，启动时会报 `BeanCreationException`。

**问题 2**：MyBatis-Plus 在 Spring Boot 3 下需要使用 `mybatis-plus-spring-boot3-starter` 而不是 `mybatis-plus-boot-starter`。  
**解决**：从 MyBatis-Plus 3.5.5 开始，提供了专门适配 Spring Boot 3 的 starter。

---

## 🗓️ Day 3: 数据库设计与实体层实现

**日期**：2024年3月17日

### 数据库设计

根据业务需求设计了完整的表结构，共 12 张表：

**核心业务表：**
- `user` — 用户表（手机号唯一登录）
- `user_balance` — 用户余额表（红包功能依赖）
- `contact` — 好友关系表（双向记录）
- `friend_request` — 好友申请表
- `group_info` — 群组表
- `group_member` — 群成员表
- `message` — 消息表（核心，支持单聊和群聊）
- `offline_message` — 离线消息表

**社交功能表：**
- `moment` — 朋友圈表
- `moment_like` — 点赞表
- `moment_comment` — 评论表

**红包功能表：**
- `red_packet` — 红包表
- `red_packet_record` — 红包领取记录

**通知表：**
- `notification` — 系统通知表

### 索引设计要点

消息表是查询最频繁的表，索引设计至关重要：

```sql
-- 消息表核心索引
KEY `idx_sender` (`sender_id`),       -- 按发送者查询
KEY `idx_receiver` (`receiver_id`),   -- 按接收者查询
KEY `idx_group` (`group_id`),         -- 按群组查询
KEY `idx_created` (`created_at`)      -- 时间范围查询
```

好友关系表使用联合唯一索引防止重复好友：
```sql
UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`)
```

### 实体层实现

使用 MyBatis-Plus 简化 CRUD。每个实体对应一张数据库表：

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.INPUT)  // 雪花算法生成ID
    private Long userId;
    private String userName;
    private String password;
    private String email;
    private String phone;
    private String avatar;
    private String signature;
    private Integer gender;      // 0男 1女 2保密
    private Integer status;      // 1正常 2封禁 3注销
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

### MyBatis-Plus 配置

每个微服务都需要配置分页插件和自动填充：

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

### 提交记录

```bash
git add .
git commit -m "feat: 完成数据库设计和实体层实现（12张表）"
```

---

## 🗓️ Day 4: 用户认证与注册登录模块

**日期**：2024年3月18日

### AuthenticationService 实现

认证服务负责用户注册、登录、Token 管理。

#### 短信验证码发送

```java
public void sendSmsCode(String phone) {
    // 限流检查：60秒内只能发送一次
    String rateKey = RedisConstants.RATE_LIMIT_PREFIX + "sms:" + phone;
    if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(rateKey))) {
        throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "验证码发送过于频繁");
    }

    String code = RandomUtil.randomNumbers(6);
    String key = RedisConstants.VERIFY_CODE_PREFIX + phone;
    stringRedisTemplate.opsForValue().set(key, code, 300, TimeUnit.SECONDS); // 5分钟过期
    stringRedisTemplate.opsForValue().set(rateKey, "1", 60, TimeUnit.SECONDS); // 60秒限流

    log.info("SMS code for {}: {}", phone, code);
    // TODO: 集成阿里云短信/腾讯短信
}
```

实际项目中需要接入短信服务商（阿里云 SMS、腾讯云 SMS）。开发阶段直接打印到日志。

#### 用户注册

```java
@Transactional
public Long register(RegisterRequest request) {
    // 1. 检查手机号是否已注册
    User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    if (existing != null) {
        throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
    }

    // 2. 验证短信验证码
    String cachedCode = stringRedisTemplate.opsForValue().get(key);
    if (cachedCode == null || !cachedCode.equals(request.getCode())) {
        throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
    }
    stringRedisTemplate.delete(key); // 验证通过后删除

    // 3. BCrypt 加密密码
    user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

    // 4. 雪花算法生成 userId
    user.setUserId(snowflakeIdGenerator.nextId());
    userMapper.insert(user);
    return user.getUserId();
}
```

#### 用户登录

```java
public LoginResponse login(LoginRequest request) {
    User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    if (user.getStatus() != 1) {
        throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "账户已被封禁或注销");
    }
    if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
        throw new BusinessException(ErrorCode.PASSWORD_ERROR);
    }

    String token = JwtUtil.generateToken(user.getUserId());

    // Token 存入 Redis（用于登出时黑名单检查）
    stringRedisTemplate.opsForValue().set(
            RedisConstants.USER_TOKEN_PREFIX + user.getUserId(),
            token, 604800, TimeUnit.SECONDS); // 7天

    LoginResponse response = new LoginResponse();
    response.setToken(token);
    response.setUser(toDTO(user));
    return response;
}
```

### 密码安全方案选择

| 方案 | 安全性 | 说明 |
|------|--------|------|
| MD5 | ❌ 极低 | 彩虹表攻击，碰撞率高 |
| SHA-256 | ⚠️ 中等 | 需要加盐，自己实现容易出问题 |
| **BCrypt** | ✅ 高 | 自带盐值，每次加密结果不同，work factor 可调 |

选择 BCrypt。Hutool 工具库提供了 BCrypt 实现：
```java
// 加密
String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
// 验证
boolean match = BCrypt.checkpw(plainPassword, hashed);
```

### 遇到的问题

**问题**：JJWT 从 0.9 升级到 0.12.x 后 API 完全变了。旧的 `Jwts.parser().setSigningKey()` 不再可用。  
**解决**：新版使用 `Jwts.parser().verifyWith(key).build()` 和 `Keys.hmacShaKeyFor()` 生成密钥。

```java
// 旧版 (0.9.x)
Jwts.parser().setSigningKey(secret).parseClaimsJws(token);

// 新版 (0.12.x)
Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
    .build().parseSignedClaims(token).getPayload();
```

---

## 🗓️ Day 5: WebSocket 服务核心实现

**日期**：2024年3月19日

这是整个项目**最核心**的一天。WebSocket 长连接是 IM 系统的灵魂。

### Netty Pipeline 设计

从 HTTP 握手到 WebSocket 消息处理的完整链路：

```
Channel Pipeline:
  HttpServerCodec             → HTTP 编解码
  ChunkedWriteHandler         → 大文件分块传输
  HttpObjectAggregator        → HTTP 消息聚合
  IdleStateHandler            → 心跳检测（60s 无读操作视为超时）
  WebSocketServerProtocolHandler → WebSocket 协议升级
  WebSocketHandler            → Token 鉴权（HTTP 握手阶段）
  WebSocketFrameHandler       → 消息帧处理（连接后）
```

Netty 配置类：

```java
@Configuration
public class NettyConfig {
    @Bean
    public ServerBootstrap serverBootstrap(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                                           WebSocketHandler webSocketHandler,
                                           WebSocketFrameHandler webSocketFrameHandler) {
        return new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new HttpServerCodec());
                    pipeline.addLast(new ChunkedWriteHandler());
                    pipeline.addLast(new HttpObjectAggregator(65536));
                    pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                    pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536));
                    pipeline.addLast(webSocketHandler);
                    pipeline.addLast(webSocketFrameHandler);
                }
            })
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true);
    }
}
```

### Token 鉴权

WebSocket 连接时无法在 Header 中携带 Token，改用 URL 参数：

```
ws://server:9090/ws?token=eyJhbGciOiJIUzI1NiJ9...
```

在 `WebSocketHandler` 中解析 URL 参数，验证 JWT，提取 userId：

```java
@Component
public class WebSocketHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String token = decoder.parameters().get("token") != null
                ? decoder.parameters().get("token").get(0) : null;

        if (token == null || !JwtUtil.isTokenValid(token)) {
            ctx.channel().close();
            return;
        }

        Long userId = JwtUtil.getUserId(token);
        // 存入 Channel 属性，后续 handler 可以获取
        ctx.channel().attr(ChannelManager.USER_ID_KEY).set(userId);
        ctx.fireChannelRead(request.retain()); // 传递给 WebSocket 协议升级 handler
    }
}
```

### 连接管理（ChannelManager）

核心数据结构：

```java
@Component
public class ChannelManager {
    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");
    private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public void bindUser(Long userId, Channel channel) {
        // 互踢机制：一个用户只保留最新连接
        Channel oldChannel = userChannelMap.put(userId, channel);
        if (oldChannel != null && oldChannel.isActive()) {
            oldChannel.close();
        }
        allChannels.add(channel);
    }

    public boolean sendToUser(Long userId, String message) {
        Channel channel = userChannelMap.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(message));
            return true;
        }
        return false;
    }
}
```

### 心跳机制

IM 系统必须有心跳机制，否则无法感知"假死"连接：

**客户端 → 服务端**：每 30 秒发送 `{"type": "heartbeat"}`  
**服务端处理**：回复 `{"type": "heartbeat_ack"}`，刷新 Redis 在线状态 TTL  
**超时断开**：IdleStateHandler 检测 60 秒无读操作，主动关闭连接

```java
// WebSocketFrameHandler 中处理心跳
if ("heartbeat".equals(type)) {
    WebSocketMessage pong = WebSocketMessage.of("heartbeat_ack", null);
    ctx.channel().writeAndFlush(new TextWebSocketFrame(
        objectMapper.writeValueAsString(pong)));
    // 刷新在线状态 TTL
    stringRedisTemplate.expire(
        RedisConstants.USER_ONLINE_PREFIX + userId,
        RedisConstants.TOKEN_EXPIRE, TimeUnit.SECONDS);
}

// IdleStateHandler 超时处理
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent) {
        IdleStateEvent event = (IdleStateEvent) evt;
        if (event.state() == IdleState.READER_IDLE) {
            log.warn("User {} heartbeat timeout", userId);
            ctx.channel().close();
        }
    }
}
```

### 消息推送架构

消息从发送到接收的完整链路：

```
发送者 A                    服务端                     接收者 B
   │                         │                          │
   │──HTTP POST /message───→│                          │
   │                         │──写入 MySQL              │
   │                         │──发送 Kafka              │
   │                         │                          │
   │                         │←──Kafka Consumer─────── │
   │                         │──查 ChannelManager       │
   │                         │──WebSocket 推送────────→│
   │←──WebSocket 推送──────│                          │
```

### 提交记录

```bash
git add .
git commit -m "feat: Netty WebSocket 服务核心实现（连接管理+心跳+鉴权）"
```

---

## 🗓️ Day 6: 消息持久化与离线消息

**日期**：2024年3月20日

### MessagingService 实现

#### 发送消息流程

```java
@Transactional
public MessageDTO sendMessage(Long senderId, SendMessageVO vo) {
    // 1. 参数校验
    if ("single".equals(vo.getChatType()) && vo.getReceiverId() == null) {
        throw new BusinessException(400, "单聊必须指定接收者");
    }
    if ("group".equals(vo.getChatType()) && vo.getGroupId() == null) {
        throw new BusinessException(400, "群聊必须指定群组ID");
    }

    // 2. 雪花算法生成 messageId
    Message message = new Message();
    message.setMessageId(snowflakeIdGenerator.nextId());
    message.setSenderId(senderId);
    // ... 设置其他字段
    messageMapper.insert(message);

    // 3. 发送到 Kafka
    kafkaTemplate.send("im-message-topic", objectMapper.writeValueAsString(toDTO(message)));

    return toDTO(message);
}
```

#### 历史记录查询（游标分页）

传统分页使用 `LIMIT offset, size`，当 offset 很大时性能急剧下降。IM 场景适合用**游标分页**：

```java
// 使用 lastMessageId 作为游标
if (lastMessageId != null) {
    wrapper.lt(Message::getMessageId, lastMessageId);
}
wrapper.orderByDesc(Message::getMessageId);
wrapper.last("LIMIT " + pageSize);
```

优点：每次查询都是 `WHERE messageId < ? ORDER BY messageId DESC LIMIT ?`，可以利用索引，性能稳定。

### 离线消息处理

这是保证消息不丢的关键环节。

#### 核心流程

```
Kafka Topic: im-message-topic
       │
       ├──→ RTC Service (实时推送)
       │
       └──→ Offline Service (离线存储)
              │
              ├── 检查用户在线状态 (Redis)
              │     ├── 在线 → 跳过（已由RTC推送）
              │     └── 离线 → 写入 offline_message 表
              │
              └── Redis 计数器自增 (offline:count:{userId})
```

OfflineService 通过 Kafka Consumer 监听消息：

```java
@KafkaListener(topics = "im-message-topic", groupId = "offline-consumer-group")
public void consumeMessage(String messageJson) {
    MessageDTO message = objectMapper.readValue(messageJson, MessageDTO.class);

    if ("single".equals(message.getChatType())) {
        storeIfOffline(message.getReceiverId(), message);
    } else if ("group".equals(message.getChatType())) {
        storeGroupOffline(message);
    }
}

private void storeIfOffline(Long userId, MessageDTO message) {
    // 检查用户是否在线
    Boolean online = stringRedisTemplate.hasKey("user:online:" + userId);
    if (!Boolean.TRUE.equals(online)) {
        // 用户离线，存储消息
        OfflineMessage offline = new OfflineMessage();
        offline.setUserId(userId);
        offline.setMessageId(message.getMessageId());
        offlineMessageMapper.insert(offline);

        // 计数器自增
        stringRedisTemplate.opsForValue().increment("offline:count:" + userId);
    }
}
```

#### 上线同步机制

用户重新上线后：
1. 建立 WebSocket 连接
2. 调用 `GET /api/v1/offline/count` 获取离线消息计数
3. 调用 `GET /api/v1/offline/pull` 拉取离线消息
4. 调用 `POST /api/v1/offline/read` 清空计数器

### 消息撤回

支持 2 分钟内撤回消息：

```java
public void recallMessage(Long userId, Long messageId) {
    Message message = messageMapper.selectById(messageId);
    if (message == null || !message.getSenderId().equals(userId)) {
        throw new BusinessException(403, "只能撤回自己发送的消息");
    }
    message.setStatus(2); // 撤回状态
    messageMapper.updateById(message);
    // 通过 Kafka 推送撤回通知
}
```

---

## 🗓️ Day 7: 好友系统与联系人管理

**日期**：2024年3月21日

### ContactService 实现

#### 好友申请流程

```java
public void applyFriend(Long userId, FriendApplyRequest request) {
    Long toId = request.getToId();
    // 不能添加自己
    if (userId.equals(toId)) throw new BusinessException("不能添加自己为好友");

    // 检查是否已经是好友
    Contact existing = contactMapper.selectOne(
        new LambdaQueryWrapper<Contact>()
            .eq(Contact::getUserId, userId)
            .eq(Contact::getFriendId, toId));
    if (existing != null) throw new BusinessException("已经是好友了");

    // 检查是否已有待处理的申请
    FriendRequest pending = friendRequestMapper.selectOne(
        new LambdaQueryWrapper<FriendRequest>()
            .eq(FriendRequest::getFromId, userId)
            .eq(FriendRequest::getToId, toId)
            .eq(FriendRequest::getStatus, 0));
    if (pending != null) throw new BusinessException("好友请求已发送");

    // 创建好友申请
    FriendRequest friendRequest = new FriendRequest();
    friendRequest.setId(snowflakeIdGenerator.nextId());
    friendRequest.setFromId(userId);
    friendRequest.setToId(toId);
    friendRequestMapper.insert(friendRequest);

    // 通过 Kafka 发送实时通知
    kafkaTemplate.send("im-notify-topic", notifyJson);
}
```

#### 好友申请处理

```java
public void handleFriendRequest(Long userId, FriendHandleRequest request) {
    FriendRequest friendRequest = friendRequestMapper.selectById(request.getRequestId());
    // 权限校验：只有被申请人可以处理
    if (!friendRequest.getToId().equals(userId)) throw new BusinessException(403);

    friendRequest.setStatus(request.getAction()); // 1同意 2拒绝
    friendRequestMapper.updateById(friendRequest);

    if (request.getAction() == 1) {
        // 同意：创建双向好友关系
        createContact(friendRequest.getFromId(), friendRequest.getToId());
        createContact(friendRequest.getToId(), friendRequest.getFromId());
        // 清除缓存
        stringRedisTemplate.delete("contact:list:" + userId);
        stringRedisTemplate.delete("contact:list:" + friendRequest.getFromId());
    }
}
```

### 缓存策略

联系人列表使用 **Cache-Aside 模式**：

```java
public List<ContactDTO> getContactList(Long userId) {
    // 1. 尝试从缓存读取
    String cached = stringRedisTemplate.opsForValue().get("contact:list:" + userId);
    if (cached != null) {
        return objectMapper.readValue(cached, List.class);
    }

    // 2. 缓存未命中，查数据库
    List<Contact> contacts = contactMapper.selectList(...);

    // 3. 回填缓存（5分钟过期）
    stringRedisTemplate.opsForValue().set(
        "contact:list:" + userId,
        objectMapper.writeValueAsString(result),
        300, TimeUnit.SECONDS);

    return result;
}
```

### 群组管理

群组功能支持：创建群组、添加/移除群成员、角色管理（普通成员/管理员/群主）、群公告。

---

## 🗓️ Day 8: 网关整合与 API 接口

**日期**：2024年3月22日

### Gateway 路由配置

所有 HTTP 请求通过 Gateway 统一入口：

```yaml
/api/v1/user/**     → AuthenticationService (8082)
/api/v1/contact/**  → ContactService (8083)
/api/v1/group/**    → ContactService (8083)
/api/v1/message/**  → MessagingService (8084)
/api/v1/redpacket/**→ MessagingService (8084)
/api/v1/moment/**   → MomentService (8086)
/api/v1/notify/**   → NotifyService (8087)
/api/v1/offline/**  → OfflineService (8085)
```

WebSocket 连接直连 RTC Service（`ws://server:9090/ws?token=TOKEN`），不经过 Gateway。

### API 接口清单

**用户认证**：
```
POST /api/v1/user/sms/send    — 发送验证码
POST /api/v1/user/register    — 用户注册
POST /api/v1/user/login       — 用户登录
GET  /api/v1/user/info        — 获取用户信息
POST /api/v1/user/logout      — 用户登出
```

**好友管理**：
```
POST   /api/v1/contact/friend/apply         — 申请好友
POST   /api/v1/contact/friend/handle         — 处理好友申请
GET    /api/v1/contact/friend/list           — 获取联系人列表
GET    /api/v1/contact/friend/requests       — 获取好友申请列表
DELETE /api/v1/contact/friend/{friendId}     — 删除好友
PUT    /api/v1/contact/friend/{friendId}/remark — 更新好友备注
GET    /api/v1/contact/search                — 搜索用户
```

**群组管理**：
```
POST   /api/v1/group/create                  — 创建群组
POST   /api/v1/group/members/add             — 添加群成员
GET    /api/v1/group/{groupId}/info           — 获取群信息
GET    /api/v1/group/my/list                  — 获取我的群列表
GET    /api/v1/group/{groupId}/members        — 获取群成员列表
POST   /api/v1/group/{groupId}/quit           — 退出群聊
DELETE /api/v1/group/{groupId}/dissolve       — 解散群聊
```

**消息管理**：
```
POST /api/v1/message/send                    — 发送消息
GET  /api/v1/message/history                  — 获取聊天记录
POST /api/v1/message/{messageId}/recall      — 撤回消息
GET  /api/v1/message/recent                   — 获取最近会话
```

---

## 🗓️ Day 9: 社交功能与通知系统

**日期**：2024年3月23日

### MomentService 朋友圈

朋友圈功能基于 Redis Timeline 模式实现：

**发布动态**：
```java
public Long publishMoment(Long userId, PublishMomentVO vo) {
    Moment moment = new Moment();
    moment.setMomentId(snowflakeIdGenerator.nextId());
    moment.setUserId(userId);
    moment.setContent(vo.getContent());
    moment.setImages(objectMapper.writeValueAsString(vo.getImages()));
    momentMapper.insert(moment);

    // 添加到用户 Timeline 缓存
    stringRedisTemplate.opsForList().leftPush(
        "moment:timeline:" + userId,
        String.valueOf(moment.getMomentId()));
    return moment.getMomentId();
}
```

**点赞**：Redis Set 实现，防止重复点赞
**评论**：MySQL 存储，支持楼中楼回复

### NotifyService 通知系统

通知系统基于 Kafka 消费者模式：

```java
@KafkaListener(topics = "im-notify-topic", groupId = "notify-consumer-group")
public void consumeNotification(String message) {
    JsonNode node = objectMapper.readTree(message);
    String type = node.get("type").asText();

    if ("friend_request".equals(type)) {
        Long toId = node.get("toId").asLong();
        Long fromId = node.get("fromId").asLong();
        notificationService.createNotification(toId, "friend",
            "用户 " + fromId + " 请求添加你为好友", fromId);
    }
}
```

---

## 🗓️ Day 10: 红包功能

**日期**：2024年3月24日

### 红包功能设计

红包功能是 IM 系统的重要社交特性。核心挑战在于**高并发下的金额分配**和**防重复领取**。

#### 发红包

```java
public Long sendRedPacket(Long senderId, SendRedPacketVO vo) {
    // 参数校验：金额 > 0，不超过 200 元
    // 创建红包记录
    RedPacket redPacket = new RedPacket();
    redPacket.setPacketId(snowflakeIdGenerator.nextId());
    redPacket.setSenderId(senderId);
    redPacket.setTotalAmount(vo.getTotalAmount());
    redPacket.setTotalCount(vo.getTotalCount());
    redPacket.setStatus(1);
    redPacket.setExpireAt(LocalDateTime.now().plusDays(1));
    redPacketMapper.insert(redPacket);

    // 缓存到 Redis 用于快速抢红包
    stringRedisTemplate.opsForHash().putAll("redpacket:" + packetId, packetInfo);

    // 发送红包类型消息
    Message message = new Message();
    message.setMsgType(6); // 红包类型
    messageMapper.insert(message);
    kafkaTemplate.send("im-message-topic", json);
    return packetId;
}
```

#### 抢红包

使用 Redisson 分布式锁防止并发问题：

```java
public BigDecimal grabRedPacket(Long userId, Long packetId) {
    RLock lock = redissonClient.getLock("redpacket:lock:" + packetId);
    boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
    if (!locked) throw new BusinessException("系统繁忙");

    try {
        return doGrabRedPacket(userId, packetId);
    } finally {
        lock.unlock();
    }
}

private BigDecimal doGrabRedPacket(Long userId, Long packetId) {
    // 从 Redis 获取红包信息
    Map<Object, Object> info = stringRedisTemplate.opsForHash().entries("redpacket:" + packetId);

    // 检查：不能抢自己的、不能重复抢、红包没过期
    // 随机金额计算
    int remaining = totalCount - grabbedCount;
    BigDecimal amount;
    if (remaining == 1) {
        // 最后一个红包拿走剩余金额
        amount = totalAmount.subtract(getGrabbedTotal(packetId));
    } else {
        // 二倍均值法随机分配
        BigDecimal avg = remainingAmount.divide(BigDecimal.valueOf(remaining), 2, HALF_UP);
        BigDecimal maxAmount = avg.multiply(BigDecimal.valueOf(2));
        amount = maxAmount.multiply(BigDecimal.valueOf(random.nextDouble())).setScale(2, HALF_UP);
        amount = amount.max(new BigDecimal("0.01")); // 至少 0.01 元
    }

    // 创建领取记录
    RedPacketRecord record = new RedPacketRecord();
    record.setPacketId(packetId);
    record.setUserId(userId);
    record.setAmount(amount);
    redPacketRecordMapper.insert(record);

    // 更新 Redis
    stringRedisTemplate.opsForHash().increment("redpacket:" + packetId, "grabbedCount", 1);
    stringRedisTemplate.opsForSet().add("redpacket:record:" + packetId, String.valueOf(userId));

    return amount;
}
```

---

## 🗓️ Day 11: Docker 容器化部署

**日期**：2024年3月25日

### 背景

项目共有 8 个微服务 + 6 个基础设施组件，手动部署流程繁琐。为了让面试官能**一条命令启动全部环境**，引入 Docker Compose 实现一键部署。

### Multi-Stage Dockerfile

采用统一的 multi-stage Dockerfile：

```dockerfile
# Stage 1: Builder
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# 复制所有 pom.xml → 预下载依赖（利用 Docker 缓存层）
COPY */pom.xml ./
COPY pom.xml ./
COPY common/ common/
RUN mvn dependency:go-offline -pl common -am || true
# 复制源码 → 编译打包
COPY . .
ARG MODULE
RUN mvn clean package -pl ${MODULE} -am -DskipTests

# Stage 2: Runner
FROM eclipse-temurin:17-jre-alpine
ARG MODULE
ARG SERVICE_PORT
WORKDIR /app
COPY --from=builder /app/${MODULE}/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
EXPOSE ${SERVICE_PORT}
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
```

通过 `--build-arg MODULE=xxx` 参数复用同一个 Dockerfile 构建所有模块。

### docker-compose.yml 设计要点

**健康检查依赖链**确保服务按正确顺序启动：

```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 10s
    retries: 10

nacos:
  depends_on:
    mysql:
      condition: service_healthy

auth-service:
  depends_on:
    nacos:
      condition: service_healthy
    mysql:
      condition: service_healthy
    redis:
      condition: service_healthy
```

**网络隔离**：所有容器挂载到 `ic-net` bridge 网络，服务间通过容器名互通。

**数据持久化**：MySQL、Redis、Kafka、Nacos 均使用 Docker Volume。

### 一键启动脚本

```bash
#!/bin/bash
# docker-start.sh
echo "🚀 启动 InfiniteChat 全部服务..."
docker compose up -d --build
echo "✅ 服务启动完成！"
echo "📡 Gateway API: http://localhost:8080"
echo "🔌 WebSocket: ws://localhost:9090/ws?token=TOKEN"
echo "📊 Nacos: http://localhost:8848/nacos"
```

---

## 🗓️ Day 12: Git 提交与项目收尾

**日期**：2024年3月26日

### 提交到 GitHub

将项目推送到 GitHub 仓库 `https://github.com/Dustopus/InfiniteChat`：

```bash
# 初始化仓库
git init
git remote add origin https://github.com/Dustopus/InfiniteChat.git

# 逐模块提交（遵循"完成一个功能，提交一次"原则）
git add common/ && git commit -m "feat: 公共模块 - 统一响应体、雪花算法、JWT、异常处理"
git add AuthenticationService/ sql/ && git commit -m "feat: 用户认证模块 - 注册登录、短信验证码、BCrypt加密"
git add ContactService/ && git commit -m "feat: 联系人模块 - 好友管理、群组管理"
git add GatewayService/ && git commit -m "feat: API网关 - 路由配置、JWT鉴权、CORS"
git add MessagingService/ && git commit -m "feat: 消息服务 - 消息发送、历史查询、撤回"
git add RealTimeCommunicationService/ && git commit -m "feat: WebSocket服务 - Netty长连接、心跳、消息推送"
git add OfflineService/ && git commit -m "feat: 离线消息服务 - Kafka消费、离线存储、上线同步"
git add MomentService/ && git commit -m "feat: 朋友圈模块 - 发布、点赞、评论"
git add NotifyService/ && git commit -m "feat: 通知服务 - Kafka消费者、实时推送"
git add docker-compose.yml docker/ docker-start.sh .env && git commit -m "feat: Docker一键部署支持"
git add README.md docs/ && git commit -m "docs: 完善README和开发者日志"

git push -u origin main
```

---

## 🔧 遇到的问题与解决方案

### 1. JJWT 版本升级 API 不兼容
**问题**：JJWT 从 0.9.x 升级到 0.12.x 后，密钥生成和解析 API 完全变了。  
**解决**：使用 `Keys.hmacShaKeyFor()` 生成 SecretKey，使用新的 `Jwts.parser().verifyWith().build()` API。

### 2. Spring Boot 3 + MyBatis-Plus 兼容性
**问题**：旧版 MyBatis-Plus starter 不支持 Spring Boot 3。  
**解决**：使用 `mybatis-plus-spring-boot3-starter`，且实体类需使用 `jakarta.persistence` 而非 `javax.persistence`。

### 3. Spring Cloud Gateway 和 Web 的依赖冲突
**问题**：Gateway 模块引入 `spring-boot-starter-web` 后启动报错。  
**解决**：Gateway 基于 WebFlux，不能引入 Servlet 相关依赖。移除后恢复正常。

### 4. Netty WebSocket 握手阶段获取 Token
**问题**：WebSocket 连接无法通过 HTTP Header 携带 JWT Token。  
**解决**：改用 URL 参数传递 Token：`ws://server:9090/ws?token=xxx`，在 `WebSocketHandler`（处理 `FullHttpRequest`）中解析。

### 5. 群聊消息的 Redis Pub/Sub 广播
**问题**：群聊消息需要推送给所有在线群成员，但 RTC 服务只持有本实例的 Channel。  
**解决**：使用 Redis Pub/Sub。消息服务将群聊消息发布到 Redis Channel，RTC 服务订阅该 Channel，收到消息后推送给本实例内的在线群成员。

### 6. 抢红包的并发安全
**问题**：多个用户同时抢同一个红包时，会出现超领、金额计算错误等问题。  
**解决**：使用 Redisson 分布式锁（`RLock`），保证同一时间只有一个请求处理单个红包的领取逻辑。

### 7. 离线消息的存储时机
**问题**：OfflineService 和 RTC Service 都消费 `im-message-topic`，如何协调？  
**解决**：RTC Service 负责实时推送；Offline Service 负责离线存储。OfflineService 检查用户在线状态（Redis `user:online:{userId}` key），只对离线用户存储消息。

### 8. Docker 构建速度优化
**问题**：每次构建都要重新下载 Maven 依赖，非常慢。  
**解决**：Multi-stage Dockerfile 中先复制所有 `pom.xml`，执行 `dependency:go-offline` 预下载依赖。依赖不变时，这层 Docker 缓存命中，只重新编译代码。

### 9. HashMap 红黑树转换导致的金额计算精度丢失
**问题**：抢红包时金额计算出现精度问题。  
**解决**：所有金额计算使用 `BigDecimal`，明确指定 `RoundingMode.HALF_UP` 和小数位数。

### 10. 通知推送链路过长导致延迟
**问题**：通知从 Kafka Topic A → NotifyService → Kafka Topic B → RTC Service → WebSocket，链路太长。  
**解决**：简化链路。NotifyService 直接将通知推送到 `im-message-topic`，RTC Service 统一消费该 Topic 进行消息推送，减少一次 Kafka 转发。

---

## 🔮 总结与展望

### 项目总结

InfiniteChat 从立项到功能完成，历时约两周。通过这个项目，我深入理解了以下技术领域：

1. **Netty 高性能网络编程**：Pipeline 设计、EventLoop 模型、ByteBuf 内存管理
2. **WebSocket 长连接管理**：握手鉴权、心跳保活、互踢机制、连接池管理
3. **分布式系统设计**：服务拆分、消息队列异步解耦、分布式锁、分布式 ID
4. **微服务架构**：Spring Cloud Gateway 路由、Nacos 注册中心、Kafka 消息管道
5. **消息可靠性保证**：离线消息存储、消息重试、幂等性设计
6. **Docker 容器化**：Multi-stage 构建、健康检查依赖链、网络隔离

最大的收获不是技术本身，而是**系统设计的思维方式**。以前写代码关注的是"这个功能怎么实现"，现在更关注的是"这个功能在高并发下还能不能正常工作"。

### 可以进一步优化的方向

1. **消息加密**：引入端到端加密（E2EE），保障用户隐私
2. **音视频通话**：集成 WebRTC，支持实时音视频
3. **消息搜索**：引入 Elasticsearch，支持全文检索
4. **消息漫游**：支持多设备消息同步
5. **集群部署**：多 RTC 实例 + Nginx 负载均衡 + 一致性 Hash 做连接路由
6. **监控告警**：接入 Prometheus + Grafana，监控连接数、消息量、延迟等指标
7. **消息分表**：消息表按时间分表（月表），提高查询性能
8. **读写分离**：MySQL 主从复制，读请求走从库

---

*本项目遵守 AGPL-3.0 开源协议。代码仓库：https://github.com/Dustopus/InfiniteChat*
