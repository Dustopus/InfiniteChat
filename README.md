# InfiniteChat (千言)

> 基于分布式微服务架构的企业级即时通讯系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![Netty](https://img.shields.io/badge/Netty-4.1.108-blue.svg)](https://netty.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## 📖 项目简介

InfiniteChat 是一个基于分布式微服务架构的即时通讯应用，支持单聊、群聊、聊天室等多种聊天模式。核心解决高并发场景下的实时消息推送、可靠消息投递、离线消息同步等技术难题。

### 核心特性

- 🔗 **WebSocket 长连接**：基于 Netty 实现，单机支持 50,000+ 并发连接
- 📨 **可靠消息投递**：消息不丢不漏，离线消息自动同步
- 🔄 **消息有序性**：雪花算法生成全局唯一 ID，保证消息顺序
- 💓 **智能心跳机制**：自动检测连接状态，支持断线重连
- 🏗️ **微服务架构**：服务独立部署，按需扩展
- 🔐 **安全认证**：JWT Token 认证 + BCrypt 密码加密
- 🐳 **Docker 一键部署**：一条命令启动全部服务

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────┐
│                    客户端 (Web/App)                    │
└───────────────────────┬──────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────┐
│              Spring Cloud Gateway (:8080)             │
│          (统一鉴权 · 路由转发 · 限流熔断)               │
└──┬─────────┬─────────┬─────────┬─────────┬──────────┘
   │         │         │         │         │
┌──▼──┐  ┌──▼──┐  ┌───▼──┐  ┌──▼──┐  ┌───▼──┐
│Auth │  │Contact│  │Message│  │Moment│  │Notify│
│:8082│  │:8083 │  │:8084 │  │:8086 │  │:8087 │
└─────┘  └──────┘  └───┬──┘  └─────┘  └──────┘
                       │
              ┌────────▼────────┐
              │     Kafka       │
              │  (消息队列)      │
              └───┬─────────┬───┘
            ┌─────▼──┐  ┌───▼─────┐
            │RTC Svc │  │Offline  │
            │:8081   │  │:8085    │
            │Netty WS│  │离线消息  │
            │:9090   │  └─────────┘
            └────────┘
```

## 📦 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| `common` | - | 公共模块：DTO、常量、工具类、异常处理 |
| `GatewayService` | 8080 | API 网关，统一鉴权和路由 |
| `RealTimeCommunicationService` | 8081/9090 | WebSocket 长连接管理（Netty） |
| `AuthenticationService` | 8082 | 用户注册、登录、Token 管理 |
| `ContactService` | 8083 | 好友管理、群组管理 |
| `MessagingService` | 8084 | 消息发送、历史记录查询 |
| `OfflineService` | 8085 | 离线消息存储与同步 |
| `MomentService` | 8086 | 朋友圈动态 |
| `NotifyService` | 8087 | 系统通知推送 |

## 🛠️ 技术栈

### 后端框架
- **Spring Boot 3.2.5** — 应用框架
- **Spring Cloud 2023.0.1** — 微服务治理
- **Spring Cloud Alibaba 2023.0.1.0** — 阿里微服务组件
- **Netty 4.1.108** — 高性能网络框架

### 数据存储
- **MySQL 8.0** — 关系型数据库
- **MyBatis-Plus 3.5.5** — ORM 框架
- **Redis 7.x** — 缓存 & 在线状态管理

### 消息中间件
- **Apache Kafka** — 消息队列，异步解耦
- **Redis Pub/Sub** — 群聊消息实时广播

### 基础设施
- **Nacos 2.x** — 服务注册发现 & 配置中心
- **Docker** — 容器化部署
- **MinIO** — 对象存储（图片、文件）

### 安全组件
- **JJWT 0.12.5** — JWT Token 生成与验证
- **BCrypt** — 密码加密
- **Hutool 5.8.26** — Java 工具类库

## 🚀 快速开始

### 方式一：Docker 一键部署（推荐）

> 确保已安装 **Docker** 和 **Docker Compose V2**

```bash
# 克隆项目
git clone https://github.com/Dustopus/InfiniteChat.git
cd InfiniteChat

# 一键启动全部服务（包含基础设施 + 8个微服务）
./docker-start.sh
```

或者手动执行：

```bash
docker compose up -d --build
```

启动完成后，服务地址如下：

| 服务 | 地址 |
|------|------|
| Gateway API | http://localhost:8080 |
| WebSocket | ws://localhost:9090/ws?token=TOKEN |
| Nacos 控制台 | http://localhost:8848/nacos (nacos/nacos) |
| MinIO 控制台 | http://localhost:9001 (minioadmin/minioadmin) |
| MySQL | localhost:3306 (root/root123) |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |

#### Docker 常用命令

```bash
# 查看服务状态
docker compose ps

# 查看某个服务的日志
docker compose logs -f auth-service

# 停止全部服务
docker compose down

# 停止并清除数据卷（完全重置）
docker compose down -v

# 重新构建并启动
docker compose up -d --build
```

### 方式二：本地手动部署

#### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.x
- Kafka 3.x
- Nacos 2.x

#### 步骤

1. **初始化数据库**

```bash
mysql -u root -p < sql/schema.sql
```

2. **启动基础设施**：确保 MySQL、Redis、Kafka、Nacos 已启动

3. **编译打包**

```bash
mvn clean package -DskipTests
```

4. **按顺序启动服务**

```bash
java -jar GatewayService/target/GatewayService-1.0.0-SNAPSHOT.jar &
java -jar AuthenticationService/target/AuthenticationService-1.0.0-SNAPSHOT.jar &
java -jar ContactService/target/ContactService-1.0.0-SNAPSHOT.jar &
java -jar MessagingService/target/MessagingService-1.0.0-SNAPSHOT.jar &
java -jar RealTimeCommunicationService/target/RealTimeCommunicationService-1.0.0-SNAPSHOT.jar &
java -jar OfflineService/target/OfflineService-1.0.0-SNAPSHOT.jar &
java -jar MomentService/target/MomentService-1.0.0-SNAPSHOT.jar &
java -jar NotifyService/target/NotifyService-1.0.0-SNAPSHOT.jar &
```

## 📡 API 接口

### 用户认证
```
POST /api/v1/user/register      — 用户注册
POST /api/v1/user/login         — 用户登录
POST /api/v1/user/sms/send      — 发送验证码
GET  /api/v1/user/info          — 获取用户信息
```

### 好友管理
```
POST   /api/v1/contact/friend/apply       — 申请好友
POST   /api/v1/contact/friend/handle       — 处理好友申请
GET    /api/v1/contact/friend/list         — 获取联系人列表
GET    /api/v1/contact/friend/requests     — 获取好友申请列表
DELETE /api/v1/contact/friend/{friendId}   — 删除好友
PUT    /api/v1/contact/friend/{friendId}/remark — 更新好友备注
GET    /api/v1/contact/search              — 搜索用户
```

### 群组管理
```
POST   /api/v1/group/create                — 创建群组
POST   /api/v1/group/members/add           — 添加群成员
GET    /api/v1/group/{groupId}/info         — 获取群信息
GET    /api/v1/group/my/list               — 获取我的群列表
GET    /api/v1/group/{groupId}/members      — 获取群成员列表
POST   /api/v1/group/{groupId}/quit         — 退出群聊
DELETE /api/v1/group/{groupId}/dissolve      — 解散群聊
```

### 消息管理
```
POST /api/v1/message/send                  — 发送消息
GET  /api/v1/message/history               — 获取聊天记录
POST /api/v1/message/{messageId}/recall    — 撤回消息
GET  /api/v1/message/recent                — 获取最近会话
```

### 红包功能
```
POST   /api/v1/redpacket/send              — 发送红包
POST   /api/v1/redpacket/grab/{packetId}   — 抢红包
GET    /api/v1/redpacket/{packetId}         — 获取红包详情
GET    /api/v1/redpacket/my                — 获取我的红包列表
GET    /api/v1/redpacket/{packetId}/records — 获取红包领取记录
```

### 通知管理
```
GET    /api/v1/notify/list                 — 获取通知列表
GET    /api/v1/notify/unread/count         — 获取未读通知数量
POST   /api/v1/notify/{notificationId}/read — 标记已读
POST   /api/v1/notify/read/all            — 全部标记已读
```

### 离线消息
```
GET    /api/v1/offline/count              — 获取离线消息数量
GET    /api/v1/offline/pull               — 拉取离线消息
POST   /api/v1/offline/read              — 标记已读
```

### 朋友圈
```
POST /api/v1/moment/publish         — 发布动态
GET  /api/v1/moment/timeline         — 获取朋友圈时间线
POST /api/v1/moment/{momentId}/like  — 点赞
DELETE /api/v1/moment/{momentId}/like — 取消点赞
POST /api/v1/moment/comment          — 评论
GET  /api/v1/moment/{momentId}/comments — 获取评论列表
DELETE /api/v1/moment/{momentId}     — 删除动态
```

### WebSocket 连接

```javascript
const ws = new WebSocket('ws://localhost:9090/ws?token=YOUR_JWT_TOKEN');
ws.onopen = () => console.log('Connected');
ws.onmessage = (e) => console.log('Received:', e.data);

// 发送消息
ws.send(JSON.stringify({
  type: 'message',
  chatType: 'single',
  receiverId: 123456,
  msgType: 1,
  content: 'Hello!'
}));

// 心跳保活
ws.send(JSON.stringify({ type: 'heartbeat' }));
```

## 🐳 Docker 架构说明

```
docker-compose.yml
├── 基础设施层
│   ├── mysql        (MySQL 8.0 - 自动初始化 schema.sql)
│   ├── redis        (Redis 7 - AOF 持久化)
│   ├── zookeeper    (Zookeeper - Kafka 依赖)
│   ├── kafka        (Kafka 7.6 - 消息队列)
│   ├── nacos        (Nacos 2.3 - 服务注册发现)
│   └── minio        (MinIO - 对象存储)
│
└── 业务服务层（全部通过 multi-stage Dockerfile 构建）
    ├── gateway-service      (:8080)
    ├── auth-service         (:8082)
    ├── contact-service      (:8083)
    ├── messaging-service    (:8084)
    ├── rtc-service          (:8081 / :9090)
    ├── offline-service      (:8085)
    ├── moment-service       (:8086)
    └── notify-service       (:8087)
```

所有服务通过 `ic-net` bridge 网络互通，环境变量统一配置，依赖关系自动管理。

## 📄 开发者日志

详细的开发记录请查看：[开发者日志](docs/DEVELOPMENT_BLOG.md)

## 📝 License

AGPL-3.0 License
