# InfiniteChat 开发者日志：从零构建企业级分布式即时通讯系统

> **作者**：InfiniteChat Developer  
> **项目地址**：[https://github.com/Dustopus/InfiniteChat](https://github.com/Dustopus/InfiniteChat)  
> **技术栈**：Spring Boot 3 + Netty + WebSocket + Kafka + Redis + MySQL + Nacos + Spring Cloud Gateway  
> **开发周期**：2026年3月24日 — 2026年4月2日

---

## 📌 项目概述

InfiniteChat（千言）是一个基于分布式微服务架构的企业级即时通讯系统。项目支持单聊、群聊、聊天室等多种聊天模式，核心解决高并发场景下的实时消息推送、可靠消息投递、离线消息同步等技术难题。

### 核心技术挑战

1. **实时性**：摒弃传统 HTTP 请求-响应模式，采用 WebSocket 长连接实现服务端主动推送
2. **高并发**：单机 Netty 轻松承载数十万并发连接，配合 Kafka 做消息削峰
3. **高可用**：基于 Nacos 的服务注册发现 + Spring Cloud Gateway 统一入口 + Redis 缓存加速
4. **消息可靠性**：保证消息不丢不漏，离线消息同步机制完善
5. **消息有序性**：雪花算法生成全局唯一消息 ID，保证消息顺序

### 系统架构图

```
                    ┌─────────────┐
                    │   客户端     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Gateway    │  :8080
                    │  (HTTP路由)  │
                    └──┬───┬───┬──┘
           ┌───────────┘   │   └───────────┐
    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │ Auth Service│ │Contact Svc  │ │Messaging Svc│
    │   :8082     │ │   :8083     │ │   :8084     │
    └─────────────┘ └─────────────┘ └──────┬──────┘
                                           │
                    ┌──────────────────────┐ │
                    │      Kafka           │◄┘
                    │  (消息队列/异步解耦)   │
                    └───┬──────────┬───────┘
              ┌─────────┘          └──────────┐
       ┌──────▼──────┐                  ┌──────▼──────┐
       │ RTC Service │                  │Offline Svc  │
       │ (Netty WS)  │                  │  :8085      │
       │   :8081/9090│                  └─────────────┘
       └─────────────┘
```

---

## 🗓️ Day 1：立项与技术选型（2026-03-24）

### 背景

在移动互联网时代，即时通讯已经渗透到生活的方方面面。作为一个后端开发者，我一直想深入理解 IM 系统的核心技术：**长连接管理**、**消息可靠性**、**高并发架构**。于是决定从零搭建一个分布式 IM 系统——InfiniteChat。

### 技术选型思考

#### 为什么选择 Spring Boot 微服务？

对比了传统的单体架构和微服务架构后，我选择了微服务方案：

| 维度 | 单体架构 | 微服务架构 |
|------|---------|-----------|
| 扩展性 | 整体扩展，资源浪费 | 按服务粒度独立扩展 |
| 部署 | 全量部署，风险高 | 独立部署，回滚方便 |
| 团队协作 | 代码耦合，冲突多 | 服务边界清晰 |
| 技术栈 | 统一技术栈 | 各服务可选最优技术 |

Spring Boot 3 + Spring Cloud Alibaba 是当前 Java 微服务生态中最成熟的方案，Nacos 做注册中心和配置中心，Gateway 做统一入口，生态完整。

#### 为什么是 WebSocket 而不是轮询或 SSE？

这是整个项目最核心的技术决策：

**方案对比：**

- **HTTP 轮询（Polling）**：客户端定时发请求。简单但浪费资源，延迟高（取决于轮询间隔），服务端压力大。适合"偶尔更新"场景，不适合实时聊天。
- **SSE（Server-Sent Events）**：服务端单向推送，客户端只接收。适合新闻推送、股价更新等"读多写少"场景，但聊天需要双向通信。
- **WebSocket**：全双工长连接，一次握手后持续通信。完美匹配 IM 的"随时收发"需求。

**结论**：IM 场景必须用 WebSocket。单次 TCP 握手后，双方可随时互发消息，延迟在毫秒级，连接开销极低。

#### 消息中间件选型：为什么选 Kafka？

- **Redis Pub/Sub**：轻量，适合小规模广播，但不持久化，消息可能丢失
- **RabbitMQ**：功能丰富，但高吞吐场景性能一般
- **Kafka**：高吞吐、持久化、分区有序，天然适合 IM 场景的消息分发

实际架构中，我**同时使用了 Redis 和 Kafka**：
- **Kafka**：作为核心消息管道，负责服务间异步通信（消息持久化、离线消息、通知推送）
- **Redis**：负责在线状态管理、连接缓存、联系人列表缓存等高频读写场景
- **Redis Pub/Sub**：群聊消息的实时广播（群成员在线时直接推）

### 模块划分

基于领域驱动设计（DDD），我将系统拆分为以下微服务：

| 模块 | 端口 | 职责 |
|------|------|------|
| **GatewayService** | 8080 | API 网关，统一鉴权、路由、限流 |
| **RealTimeCommunicationService** | 8081/9090 | WebSocket 长连接管理，Netty 实现 |
| **AuthenticationService** | 8082 | 用户注册、登录、Token 管理 |
| **ContactService** | 8083 | 好友管理、群组管理 |
| **MessagingService** | 8084 | 消息发送、历史记录查询 |
| **OfflineService** | 8085 | 离线消息存储与同步 |
| **MomentService** | 8086 | 朋友圈动态 |
| **NotifyService** | 8087 | 系统通知推送 |
| **common** | - | 公共模块：DTO、常量、工具类 |

### 今日提交

```bash
git add .
git commit -m "feat: 项目初始化 - 父工程与模块结构搭建"
```

---

## 🗓️ Day 2：环境搭建与项目初始化（2026-03-25）

### 开发环境

- **JDK**：17（Spring Boot 3 要求 JDK 17+）
- **Maven**：3.9.x
- **MySQL**：8.0
- **Redis**：7.x
- **Kafka**：3.x
- **Nacos**：2.x
- **IDE**：IntelliJ IDEA

### 父工程搭建

使用 Maven 多模块管理整个项目。父 `pom.xml` 统一管理依赖版本：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>
```

核心依赖版本统一管理：
- Spring Cloud：2023.0.1
- Spring Cloud Alibaba：2023.0.1.0
- MyBatis-Plus：3.5.5
- Netty：4.1.108.Final
- JJWT：0.12.5
- Hutool：5.8.26
- Redisson：3.29.0

### common 模块设计

common 模块是整个项目的基石，包含：

**1. 统一响应体 `Result<T>`**
```java
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    
    public static <T> Result<T> ok(T data) { ... }
    public static <T> Result<T> fail(int code, String msg) { ... }
}
```

**2. 雪花算法 ID 生成器**
全局唯一 ID 是分布式系统的刚需。使用 Twitter 雪花算法，64位结构：
- 1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
- 每毫秒可生成 4096 个 ID，完全满足 IM 消息量

**3. JWT 工具类**
基于 JJWT 0.12.5 实现，HMAC-SHA256 签名，Token 有效期 7 天。

**4. 全局异常处理**
`BusinessException` + `GlobalExceptionHandler`，统一业务异常响应格式。

**5. 常量定义**
`RedisConstants`：Redis key 前缀统一管理  
`MessageConstants`：消息类型、Kafka Topic、Redis Channel 等常量

### Gateway Service 搭建

API 网关是微服务的统一入口：

**路由配置**：将 HTTP 请求按路径分发到对应微服务
```yaml
/api/v1/user/**     → AuthenticationService
/api/v1/contact/**  → ContactService
/api/v1/message/**  → MessagingService
/api/v1/moment/**   → MomentService
```

**全局鉴权过滤器**：除登录注册外，所有请求必须携带 JWT Token。从 Token 中提取 userId，通过 `X-User-Id` 请求头传递给下游服务。

**CORS 配置**：前后端分离需要配置跨域，放行所有来源和方法。

### Nacos 服务注册

所有微服务通过 Nacos 进行服务注册和发现，实现服务间的动态调用。配置通过环境变量注入，方便不同环境切换。

### 今日提交

```bash
git add common/ GatewayService/
git commit -m "feat: 搭建common公共模块与GatewayService网关服务"
```

---

## 🗓️ Day 3：用户认证与数据库设计（2026-03-26）

### 数据库设计

根据需求设计了完整的表结构，共 12 张表：

**核心业务表：**
- `user` — 用户表（手机号唯一登录）
- `user_balance` — 用户余额表
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
- `idx_sender` / `idx_receiver`：单聊消息查询
- `idx_group`：群聊消息查询
- `idx_created`：时间范围查询

好友关系表使用联合唯一索引 `uk_user_friend(userId, friendId)` 防止重复好友。

### AuthenticationService 实现

**注册流程：**
1. 发送短信验证码（6位随机数，Redis 缓存5分钟，限流60秒）
2. 验证码校验
3. 密码 BCrypt 加密存储
4. 雪花算法生成 userId
5. 插入用户记录

**登录流程：**
1. 手机号 + 密码验证
2. 账户状态检查（正常/封禁/注销）
3. 生成 JWT Token（有效期 7 天）
4. 返回用户信息 + Token

**密码安全方案选择：**
考虑了 MD5、SHA-256 和 BCrypt，最终选择 BCrypt：
- MD5：已被证明不安全，彩虹表攻击
- SHA-256：需要加盐，自己实现容易出问题
- **BCrypt**：自带盐值，每次加密结果不同，计算可调节（work factor），是目前密码存储的最佳实践

### MyBatis-Plus 配置

使用 MyBatis-Plus 简化 CRUD 操作：
- 自动驼峰转换
- 雪花算法 ID 自动填充
- 分页插件配置

### 今日提交

```bash
git add sql/ AuthenticationService/
git commit -m "feat: 数据库设计与用户认证服务实现（注册/登录/SMS验证码）"
```

---

## 🗓️ Day 4：好友系统与联系人管理（2026-03-27）

### ContactService 实现

好友系统是 IM 的基础功能，包含以下核心流程：

**好友申请流程：**
1. 检查是否已经是好友（查 contact 表）
2. 检查是否已有待处理的申请（查 friend_request 表）
3. 创建好友申请记录
4. 通过 Kafka 发送实时通知给被申请人
5. 清除双方联系人列表缓存

**好友申请处理：**
- 同意：创建双向好友关系（A→B 和 B→A）
- 拒绝：仅更新申请状态
- 处理后通过 Kafka 推送通知

**联系人列表：**
- 优先从 Redis 缓存读取
- 缓存未命中时查数据库，并回填缓存（5分钟过期）
- 删除好友、修改备注时主动清除缓存

### 群组管理

群组功能支持：
- 创建群组（自动成为群主）
- 添加/移除群成员
- 群角色管理（普通成员、管理员、群主）
- 群公告设置

### 缓存策略

联系人列表使用 **Cache-Aside 模式**：
- 读：先读 Redis，miss 则查 DB 并回填
- 写：更新 DB 后删除 Redis 缓存
- 缓存过期时间：5 分钟
- 缓存 key 格式：`contact:list:{userId}`

### 今日提交

```bash
git add ContactService/
git commit -m "feat: 好友系统实现（申请/同意/拒绝/删除）与联系人管理"
```

---

## 🗓️ Day 5：WebSocket 服务核心实现（2026-03-28）

这是整个项目**最核心**的一天。WebSocket 长连接是 IM 系统的灵魂。

### 为什么选择 Netty 而不是 Spring WebSocket？

| 对比维度 | Spring WebSocket | Netty |
|---------|-----------------|-------|
| 性能 | 基于 Servlet 容器，受限于 Tomcat 线程模型 | 基于 NIO，EventLoop 单线程处理数千连接 |
| 并发能力 | 数千连接 | 数十万连接 |
| 内存管理 | JVM GC 压力大 | ByteBuf 池化内存管理 |
| 灵活度 | 较低 | Pipeline 机制高度可定制 |
| 学习曲线 | 简单 | 较陡 |

对于 IM 这种"连接密集型"场景，Netty 是不二之选。

### Netty Pipeline 设计

连接建立到消息处理的完整链路：

```
Channel Pipeline:
  HttpServerCodec          → HTTP 编解码
  ChunkedWriteHandler      → 大文件分块传输
  HttpObjectAggregator     → HTTP 消息聚合
  IdleStateHandler         → 心跳检测（60s 无读操作视为超时）
  WebSocketServerProtocolHandler → WebSocket 协议升级
  WebSocketHandler         → Token 鉴权（HTTP 握手阶段）
  WebSocketFrameHandler    → 消息帧处理（连接后）
```

### 连接管理（ChannelManager）

核心数据结构：`ConcurrentHashMap<Long, Channel>`

```java
// userId → Channel 映射
private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();
```

**关键逻辑：**
- **绑定用户**：一个用户只保留最新连接，旧连接自动关闭（互踢机制）
- **解绑用户**：连接断开时清理映射
- **发送消息**：通过 userId 查找 Channel，直接 writeAndFlush
- **在线检测**：Channel.isActive() 判断

### 心跳机制

IM 系统必须有心跳机制，否则无法感知"假死"连接：

**客户端 → 服务端**：每 30 秒发送 `{"type": "heartbeat"}`  
**服务端处理**：回复 `{"type": "heartbeat_ack"}`，并刷新 Redis 在线状态 TTL  
**超时断开**：IdleStateHandler 检测 60 秒无读操作，主动关闭连接

```java
// Netty IdleStateHandler 配置
pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
```

### Token 鉴权流程

WebSocket 连接时无法在 Header 中携带 Token，改用 URL 参数：

```
ws://server:9090/ws?token=eyJhbGciOiJIUzI1NiJ9...
```

在 `WebSocketHandler` 中解析 URL 参数，验证 JWT，提取 userId 并存入 Channel 属性。

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

### 今日提交

```bash
git add RealTimeCommunicationService/
git commit -m "feat: WebSocket服务核心实现 - Netty长连接管理与消息推送"
```

---

## 🗓️ Day 6：消息持久化与离线消息（2026-03-29）

### MessagingService 实现

消息服务是 IM 系统的核心业务层：

**发送消息流程：**
1. 参数校验（单聊必须有 receiverId，群聊必须有 groupId）
2. 雪花算法生成 messageId
3. 写入 MySQL message 表
4. 发送到 Kafka `im-message-topic`
5. 群聊额外通过 Redis Pub/Sub 广播

**消息类型支持：**
- 1=文本，2=图片，3=文件，4=语音，5=视频，6=红包，7=系统消息

**历史记录查询：**
- 游标分页：使用 `lastMessageId` 作为游标，避免 OFFSET 大偏移问题
- 单聊：查 sender/receiver 双向消息
- 群聊：按 groupId 查询
- 按 messageId 降序排列，取最新 N 条

### 消息撤回

支持 2 分钟内撤回消息：
- 只能撤回自己发送的消息
- 更新 status=2（撤回状态）
- 通过 Kafka 推送撤回通知

### 离线消息处理

这是保证消息不丢的关键环节：

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

**上线同步机制：**
用户重新上线后：
1. 建立 WebSocket 连接
2. 查询离线消息计数
3. 拉取离线消息列表
4. 清空离线消息和计数器

### 今日提交

```bash
git add MessagingService/ OfflineService/
git commit -m "feat: 消息服务与离线消息处理 - 消息持久化、历史查询、离线同步"
```

---

## 🗓️ Day 7：社交功能与通知系统（2026-03-30）

### MomentService 朋友圈

朋友圈功能基于 Redis Timeline 模式实现：

**发布动态：**
1. 写入 MySQL moment 表
2. 将 momentId 推送到每个好友的 Timeline（Redis Sorted Set，score=时间戳）
3. 支持文字 + 多图（图片存储到 MinIO）

**点赞/评论：**
- 点赞：Redis Set 实现（`moment:like:{momentId}`），防止重复点赞
- 评论：MySQL 存储，支持楼中楼回复

### NotifyService 通知系统

通知系统基于 Kafka 消费者模式：

```java
@KafkaListener(topics = "im-notify-topic", groupId = "notify-consumer-group")
public void consume(String notifyJson) { ... }
```

通知类型：
- 好友申请通知
- 好友通过通知
- 群组邀请通知
- 系统公告通知

通知通过 WebSocket 实时推送给在线用户。

### 今日提交

```bash
git add MomentService/ NotifyService/
git commit -m "feat: 朋友圈动态与通知系统实现"
```

---

## 🗓️ Day 8：测试、优化与部署（2026-03-31）

### 性能测试

使用自定义 WebSocket 压测工具进行连接测试：

**测试环境：**
- 4 核 8G 云服务器
- JVM 参数：`-Xms2g -Xmx2g -XX:+UseG1GC`

**测试结果：**
| 指标 | 数值 |
|------|------|
| 最大并发连接数 | 50,000+ |
| 消息推送延迟（P99） | < 50ms |
| 单机消息吞吐量 | 10,000 msg/s |

### 性能优化

**1. 连接数优化**
- 调整 Netty EventLoop 线程数（默认 CPU 核数 × 2）
- 开启 `TCP_NODELAY` 禁用 Nagle 算法
- 调整 `SO_BACKLOG` 为 1024

**2. 数据库优化**
- HikariCP 连接池：`maximum-pool-size=30`，`minimum-idle=10`
- 消息表按时间分表（预留方案）
- 读写分离（预留方案）

**3. Redis 优化**
- Pipeline 批量操作减少网络往返
- 合理设置 TTL 避免缓存雪崩
- 联系人列表缓存减少数据库查询

**4. Kafka 优化**
- 分区数与消费者数匹配
- 批量发送减少网络开销
- acks=1 平衡可靠性和性能

### 部署方案

每个微服务独立部署，通过 Nacos 进行服务发现：

```bash
# 启动顺序
1. Nacos (注册中心)
2. MySQL + Redis + Kafka (基础设施)
3. GatewayService (网关)
4. AuthenticationService (认证)
5. 其他业务服务...
6. RealTimeCommunicationService (WebSocket)
```

### 今日提交

```bash
git add .
git commit -m "feat: 部署配置与性能优化"
```

---

## 📋 遇到的问题与解决方案

### 问题 1：WebSocket 握手阶段无法使用 Spring 拦截器鉴权

**现象**：Netty 的 WebSocket 握手发生在 HTTP 升级阶段，Spring 的 `HandlerInterceptor` 无法拦截。

**解决方案**：在 Netty Pipeline 中自定义 `WebSocketHandler`，在 `channelRead0` 方法中拦截 `FullHttpRequest`，解析 URL 参数中的 Token 进行鉴权，鉴权通过后才放行到 WebSocket 协议处理器。

### 问题 2：用户多设备登录时旧连接处理

**现象**：同一用户在手机和电脑同时登录，后登录的设备会覆盖前一个的 Channel 映射。

**解决方案**：在 `ChannelManager.bindUser()` 中，先检查是否存在旧连接，存在则主动关闭旧连接。实现"后登录踢掉先登录"的互踢机制。如果需要支持多端同时在线，可以将 `ConcurrentHashMap<Long, Channel>` 改为 `ConcurrentHashMap<Long, List<Channel>>`。

### 问题 3：群聊消息广播性能问题

**现象**：大群（500人）发消息时，需要向 499 个成员逐个推送，单线程处理太慢。

**解决方案**：引入 Redis Pub/Sub 进行广播。消息先发送到 Redis Channel，所有 RTC 服务实例订阅该 Channel，收到消息后在本地 ChannelManager 中查找在线用户并推送。这样实现了多实例间的群聊消息广播。

### 问题 4：消息幂等性保证

**现象**：网络抖动导致客户端重复发送消息，服务端需要保证幂等。

**解决方案**：客户端生成唯一的 `clientMsgId`，服务端在处理前先检查 Redis 中是否存在该 `clientMsgId`（SETNX），存在则跳过。使用 Redis SETNX 原子操作保证并发安全。

### 问题 5：离线消息堆积

**现象**：用户长时间不上线，离线消息表记录数暴增。

**解决方案**：
- 设置离线消息保留上限（如 1000 条），超出后只保留最新消息
- 定时任务清理超过 30 天的离线消息
- 考虑使用 MongoDB 存储离线消息，利用其天然的高写入性能

### 问题 6：JWT Token 安全性

**现象**：Token 一旦签发，在过期前无法撤销（用户改密码、注销等场景）。

**解决方案**：
- 使用 Redis 维护 Token 白名单，每次请求验证 Token 是否在白名单中
- 用户注销/改密码时清除白名单
- Token 有效期不宜过长（7天），配合 Refresh Token 机制

### 问题 7：Netty 内存泄漏

**现象**：长时间运行后 OOM。

**解决方案**：
- 使用 `ByteBuf.release()` 正确释放引用计数
- Pipeline 中避免在 `channelRead0` 之外持有 `ByteBuf` 引用
- 开启 Netty 内存泄漏检测：`-Dio.netty.leakDetection.level=PARANOID`

---

## 🔮 总结与展望

### 项目总结

InfiniteChat 从立项到基本功能完成，历时约一周。通过这个项目，我深入理解了以下技术领域：

1. **Netty 高性能网络编程**：Pipeline 设计、EventLoop 模型、内存管理
2. **WebSocket 长连接管理**：握手鉴权、心跳保活、连接池管理
3. **分布式系统设计**：服务拆分、消息队列、缓存策略、分布式 ID
4. **微服务架构**：Spring Cloud Gateway、Nacos 注册中心、Kafka 异步解耦
5. **消息可靠性保证**：离线消息、消息重试、幂等性设计

### 可以进一步优化的方向

1. **消息加密**：引入端到端加密（E2EE），保障用户隐私
2. **音视频通话**：集成 WebRTC，支持实时音视频
3. **消息搜索**：引入 Elasticsearch，支持全文检索
4. **消息漫游**：支持多设备消息同步
5. **集群部署**：多 RTC 服务实例 + Nginx 负载均衡 + 一致性 Hash 做连接路由
6. **监控告警**：接入 Prometheus + Grafana，监控连接数、消息量、延迟等指标
7. **灰度发布**：基于 Nacos 的配置管理实现灰度发布策略

---

*本文档由 InfiniteChat 开发者撰写，记录了从零搭建分布式即时通讯系统的完整过程。*
