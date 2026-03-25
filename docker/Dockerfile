# ============================================================
# InfiniteChat - 多阶段构建 Dockerfile
# 用法: docker build --build-arg MODULE=<模块名> --build-arg SERVICE_PORT=<端口> -t ic-<模块名> .
# ============================================================

# ===== Stage 1: Maven 构建 =====
FROM maven:3.9-eclipse-temurin-17 AS builder

ARG MODULE

WORKDIR /build

# 先复制 pom 文件利用 Docker 缓存层
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY AuthenticationService/pom.xml AuthenticationService/pom.xml
COPY GatewayService/pom.xml GatewayService/pom.xml
COPY RealTimeCommunicationService/pom.xml RealTimeCommunicationService/pom.xml
COPY ContactService/pom.xml ContactService/pom.xml
COPY MessagingService/pom.xml MessagingService/pom.xml
COPY OfflineService/pom.xml OfflineService/pom.xml
COPY MomentService/pom.xml MomentService/pom.xml
COPY NotifyService/pom.xml NotifyService/pom.xml

# 预下载依赖（利用缓存层）
RUN mvn dependency:go-offline -B || true

# 复制全部源码
COPY . .

# 编译打包（仅构建目标模块及其依赖）
RUN mvn clean package -pl ${MODULE} -am -DskipTests -B -q

# ===== Stage 2: 轻量运行镜像 =====
FROM eclipse-temurin:17-jre-alpine

ARG MODULE
ARG SERVICE_PORT=8080

WORKDIR /app

# 从构建阶段复制编译产物
COPY --from=builder /build/${MODULE}/target/*.jar app.jar

# 仅暴露该服务实际使用的端口
EXPOSE ${SERVICE_PORT}

# JVM 优化参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
