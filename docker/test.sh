#!/bin/bash
# ============================================================
# 抖音运营平台 - 部署测试脚本
# ============================================================

set -e

echo "=========================================="
echo "抖音运营平台 - 部署测试"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
PASSED=0
FAILED=0

# 测试函数
test_service() {
    local name=$1
    local url=$2
    local expected=$3

    echo -n "测试 $name... "

    if curl -f -s "$url" | grep -q "$expected"; then
        echo -e "${GREEN}✓ 通过${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        ((FAILED++))
        return 1
    fi
}

test_port() {
    local name=$1
    local host=$2
    local port=$3

    echo -n "测试 $name 端口 $port... "

    if nc -z -w5 "$host" "$port" 2>/dev/null; then
        echo -e "${GREEN}✓ 通过${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        ((FAILED++))
        return 1
    fi
}

echo ""
echo "1. 检查 Docker 服务"
echo "----------------------------------------"

# 检查容器状态
echo "检查容器运行状态..."
docker compose ps

echo ""
echo "2. 端口连通性测试"
echo "----------------------------------------"

test_port "PostgreSQL" localhost 5433
test_port "Redis" localhost 6380
test_port "RabbitMQ" localhost 5672
test_port "RabbitMQ 管理" localhost 15672
test_port "Elasticsearch" localhost 9200
test_port "后端应用" localhost 8189
test_port "Nginx" localhost 8888

echo ""
echo "3. 服务健康检查"
echo "----------------------------------------"

test_service "后端健康检查" "http://localhost:8189/actuator/health" "UP"
test_service "Elasticsearch" "http://localhost:9200/_cluster/health" "cluster_name"
test_service "前端页面" "http://localhost:8888" "html"

echo ""
echo "4. 数据库连接测试"
echo "----------------------------------------"

echo -n "测试数据库连接... "
if docker compose exec -T postgres psql -U postgres -d douyin_operations -c "SELECT 1" &>/dev/null; then
    echo -e "${GREEN}✓ 通过${NC}"
    ((PASSED++))
else
    echo -e "${RED}✗ 失败${NC}"
    ((FAILED++))
fi

echo -n "测试数据库表... "
TABLE_COUNT=$(docker compose exec -T postgres psql -U postgres -d douyin_operations -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public'" | tr -d ' ')
if [ "$TABLE_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ 通过 (共 $TABLE_COUNT 张表)${NC}"
    ((PASSED++))
else
    echo -e "${RED}✗ 失败${NC}"
    ((FAILED++))
fi

echo ""
echo "5. Redis 连接测试"
echo "----------------------------------------"

echo -n "测试 Redis 连接... "
if docker compose exec -T redis redis-cli ping | grep -q "PONG"; then
    echo -e "${GREEN}✓ 通过${NC}"
    ((PASSED++))
else
    echo -e "${RED}✗ 失败${NC}"
    ((FAILED++))
fi

echo ""
echo "6. API 接口测试"
echo "----------------------------------------"

# 测试健康检查接口
echo -n "测试健康检查接口... "
HEALTH_RESPONSE=$(curl -s http://localhost:8189/actuator/health)
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✓ 通过${NC}"
    ((PASSED++))
else
    echo -e "${RED}✗ 失败${NC}"
    echo "响应: $HEALTH_RESPONSE"
    ((FAILED++))
fi

# 测试 Swagger 文档
echo -n "测试 API 文档... "
if curl -f -s http://localhost:8189/v3/api-docs &>/dev/null; then
    echo -e "${GREEN}✓ 通过${NC}"
    ((PASSED++))
else
    echo -e "${RED}✗ 失败${NC}"
    ((FAILED++))
fi

echo ""
echo "7. 容器资源使用"
echo "----------------------------------------"

docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" \
    dy-app dy-postgres dy-redis dy-rabbitmq dy-elasticsearch dy-nginx

echo ""
echo "8. 日志检查"
echo "----------------------------------------"

echo "检查应用日志中的错误..."
ERROR_COUNT=$(docker compose logs app | grep -i "error" | wc -l)
if [ "$ERROR_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓ 无错误日志${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠ 发现 $ERROR_COUNT 条错误日志${NC}"
    echo "最近的错误:"
    docker compose logs app | grep -i "error" | tail -5
fi

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
echo -e "通过: ${GREEN}$PASSED${NC}"
echo -e "失败: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！系统运行正常${NC}"
    exit 0
else
    echo -e "${RED}✗ 部分测试失败，请检查日志${NC}"
    echo ""
    echo "查看详细日志:"
    echo "  docker compose logs app"
    echo "  docker compose logs postgres"
    echo "  docker compose logs redis"
    exit 1
fi
