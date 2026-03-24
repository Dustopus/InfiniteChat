# InfiniteChat (千言)

> 基于分布式微服务架构的企业级即时通讯系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)
[![Netty](https://img.shields.io/badge/Netty-4.1.108-blue.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📖 项目简介

InfiniteChat 是一个基于分布式微服务架构的即时通讯应用，支持单聊、群聊、聊天室等多种聊天模式。核心解决高并发场景下的实时消息推送、可靠消息投递、离线消息同步等技术难题。

### 核心特性

- 🔗 **WebSocket 长连接**：基于 Netty 实现，单机支持 50,000+ 并发连接
- 📨 **可靠消息投递**：消息不丢不漏，离线消息自动同步
- 🔄 **消息有序性**：雪花算法生成全局唯一 ID，保证消息顺序
- 💓 **智能心跳机制**：自动检测连接状态，支持断线重连
- 🏗️ **微服务架构**：服务独立部署，按需扩展
- 🔐 **安全认证**：JWT Token 认证 + BCrypt 密码加密

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
- **MongoDB** — 非结构化数据存储（可选）

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

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.x
- Kafka 3.x
- Nacos 2.x

### 1. 克隆项目

```bash
git clone https://github.com/Dustopus/InfiniteChat.git
cd InfiniteChat
```

### 2. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
```

### 3. 启动基础设施

确保以下服务已启动：
- MySQL
- Redis
- Kafka (with Zookeeper)
- Nacos

### 4. 编译打包

```bash
mvn clean package -DskipTests
```

### 5. 启动服务（按顺序）

```bash
# 1. 启动网关
java -jar GatewayService/target/GatewayService-1.0.0-SNAPSHOT.jar &

# 2. 启动认证服务
java -jar AuthenticationService/target/AuthenticationService-1.0.0-SNAPSHOT.jar &

# 3. 启动其他服务...
java -jar ContactService/target/ContactService-1.0.0-SNAPSHOT.jar &
java -jar MessagingService/target/MessagingService-1.0.0-SNAPSHOT.jar &
java -jar RealTimeCommunicationService/target/RealTimeCommunicationService-1.0.0-SNAPSHOT.jar &
java -jar OfflineService/target/OfflineService-1.0.0-SNAPSHOT.jar &
java -jar MomentService/target/MomentService-1.0.0-SNAPSHOT.jar &
java -jar NotifyService/target/NotifyService-1.0.0-SNAPSHOT.jar &
```

### 6. WebSocket 连接测试

```javascript
const ws = new WebSocket('ws://localhost:9090/ws?token=YOUR_JWT_TOKEN');
ws.onopen = () => console.log('Connected');
ws.onmessage = (e) => console.log('Received:', e.data);
ws.send(JSON.stringify({type: 'heartbeat'}));
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
POST /api/v1/friend/apply       — 申请好友
POST /api/v1/friend/handle      — 处理好友申请
GET  /api/v1/contact/list       — 获取联系人列表
GET  /api/v1/friend/requests    — 获取好友申请列表
DELETE /api/v1/friend/{id}      — 删除好友
```

### 消息管理
```
POST /api/v1/message/send       — 发送消息
GET  /api/v1/message/history    — 获取聊天记录
POST /api/v1/message/recall     — 撤回消息
```

### 朋友圈
```
POST /api/v1/moment/publish     — 发布动态
GET  /api/v1/moment/list        — 获取朋友圈列表
POST /api/v1/moment/like        — 点赞
POST /api/v1/moment/comment     — 评论
```

## 📄 开发者日志

详细的开发记录请查看：[开发者日志](docs/DEVELOPMENT_BLOG.md)

## 📝 License

MIT License
