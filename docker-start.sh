#!/bin/bash
# ============================================================
# InfiniteChat - Docker 一键启动脚本
# Author: Dustopus
# ============================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}"
echo "============================================"
echo "  InfiniteChat (千言) - Docker 一键部署"
echo "============================================"
echo -e "${NC}"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERROR] Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! docker compose version &> /dev/null; then
    echo -e "${RED}[ERROR] Docker Compose 未安装，请先安装 Docker Compose V2${NC}"
    exit 1
fi

echo -e "${GREEN}[1/3] 拉取基础设施镜像...${NC}"
docker compose pull mysql redis zookeeper kafka nacos minio 2>/dev/null || true

echo -e "${GREEN}[2/3] 构建业务服务镜像...${NC}"
docker compose build --parallel gateway-service auth-service contact-service messaging-service rtc-service offline-service moment-service notify-service

echo -e "${GREEN}[3/3] 启动全部服务...${NC}"
docker compose up -d

echo ""
echo -e "${GREEN}============================================"
echo "  🚀 部署完成！服务启动中..."
echo "============================================"
echo ""
echo "  📌 服务地址："
echo "     Gateway API    → http://localhost:8080"
echo "     WebSocket      → ws://localhost:9090/ws?token=TOKEN"
echo "     Nacos 控制台    → http://localhost:8848/nacos"
echo "     MinIO 控制台    → http://localhost:9001"
echo "     MySQL          → localhost:3306 (root/root123)"
echo "     Redis          → localhost:6379"
echo "     Kafka          → localhost:9092"
echo ""
echo "  📋 常用命令："
echo "     查看日志    → docker compose logs -f <服务名>"
echo "     停止服务    → docker compose down"
echo "     重启服务    → docker compose restart <服务名>"
echo "     查看状态    → docker compose ps"
echo -e "============================================${NC}"
