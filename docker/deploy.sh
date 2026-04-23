#!/bin/bash
# ============================================================
# 抖音运营平台 - 一键部署脚本
# ============================================================

set -e

echo "=========================================="
echo "抖音运营平台 - Docker 部署"
echo "=========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    exit 1
fi

# 进入 docker 目录
cd "$(dirname "$0")"

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "创建 .env 文件..."
    cp .env.example .env
    echo "请编辑 .env 文件配置数据库密码等信息"
    echo "然后重新运行此脚本"
    exit 0
fi

# 加载环境变量
source .env

echo ""
echo "1. 构建镜像..."
docker compose build

echo ""
echo "2. 启动服务..."
docker compose up -d

echo ""
echo "3. 等待服务就绪..."
sleep 10

# 检查数据库
echo "检查数据库连接..."
for i in {1..30}; do
    if docker compose exec -T postgres pg_isready -U ${POSTGRES_USER} &> /dev/null; then
        echo "数据库已就绪"
        break
    fi
    echo "等待数据库启动... ($i/30)"
    sleep 2
done

echo ""
echo "4. 初始化数据库..."
docker run --rm \
  -v "$(pwd)/../sql:/sql" \
  --network docker_dy-net \
  -e PGPASSWORD=${POSTGRES_PASSWORD} \
  postgres:15-alpine \
  psql -h dy-postgres -U ${POSTGRES_USER} -d ${POSTGRES_DB} -f /sql/init.sql

echo ""
echo "5. 验证部署..."

# 检查后端健康
echo "检查后端服务..."
for i in {1..30}; do
    if curl -f http://localhost:${APP_PORT}/actuator/health &> /dev/null; then
        echo "后端服务正常"
        break
    fi
    echo "等待后端启动... ($i/30)"
    sleep 2
done

# 检查前端
echo "检查前端服务..."
if curl -I http://localhost:${NGINX_PORT} &> /dev/null; then
    echo "前端服务正常"
else
    echo "警告: 前端服务未响应"
fi

echo ""
echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  前端: http://localhost:${NGINX_PORT}"
echo "  后端: http://localhost:${APP_PORT}"
echo "  API 文档: http://localhost:${APP_PORT}/swagger-ui.html"
echo "  健康检查: http://localhost:${APP_PORT}/actuator/health"
echo ""
echo "管理界面:"
echo "  RabbitMQ: http://localhost:${RABBITMQ_MGMT_PORT} (${RABBITMQ_USER}/${RABBITMQ_PASS})"
echo ""
echo "查看日志: docker compose logs -f app"
echo "停止服务: docker compose down"
echo ""
