#!/bin/bash
# 快速 E2E 测试验证脚本

echo "=========================================="
echo "🧪 快速 E2E 测试验证"
echo "=========================================="

# 检查后端服务
echo "🔍 检查后端服务..."
curl -s http://localhost:8080/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ 后端服务运行中"
else
    echo "❌ 后端服务未运行"
    exit 1
fi

# 检查前端开发服务器
echo "🔍 检查前端服务..."
curl -s http://localhost:3000 > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ 前端服务运行中"
else
    echo "⚠️ 前端服务未运行，将由 Playwright 启动"
fi

# 运行单个测试验证配置
echo ""
echo "🧪 运行验证测试..."
npx playwright test e2e/tests/auth.spec.ts:37 --project=chromium --reporter=list --max-failures=1

echo ""
echo "=========================================="
echo "✅ 测试完成"
echo "=========================================="
