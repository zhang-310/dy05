#!/bin/bash
# 自动修复前端 API 路径（P0 已确认路径）

set -e

echo "开始修复前端 API 路径..."
echo "工作目录: $(pwd)"

cd front/src/api

echo ""
echo "=== 修复 live.ts ==="
sed -i "s|/api/v1/live/persona/search|/api/v1/douyin/persona/list|g" live.ts
echo "✅ Live persona: /api/v1/live/persona/search → /api/v1/douyin/persona/list"

echo ""
echo "=== 修复 copy.ts ==="
sed -i "s|/api/v1/copy/search|/api/v1/copy/library/search|g" copy.ts
echo "✅ Copy library: /api/v1/copy/search → /api/v1/copy/library/search"

echo ""
echo "=== 修复 sms.ts ==="
sed -i "s|/api/v1/sms/template/search|/api/v1/sms/template/list|g" sms.ts
sed -i "s|/api/v1/sms/log/search|/api/v1/sms/log/list|g" sms.ts
echo "✅ SMS template: /api/v1/sms/template/search → /api/v1/sms/template/list"
echo "✅ SMS log: /api/v1/sms/log/search → /api/v1/sms/log/list"

echo ""
echo "=== 修复 script.ts ==="
sed -i "s|/api/v1/script/search|/api/v1/script/list|g" script.ts
echo "✅ Script: /api/v1/script/search → /api/v1/script/list"

echo ""
echo "=== 修复 agent.ts ==="
sed -i "s|/api/v1/agent/search|/api/v1/agent/list|g" agent.ts
sed -i "s|/api/v1/ai/multi-agent/list|/api/v1/agent/workflow/list|g" agent.ts
echo "✅ Agent: /api/v1/agent/search → /api/v1/agent/list"
echo "✅ Multi-agent: /api/v1/ai/multi-agent/list → /api/v1/agent/workflow/list"

echo ""
echo "=== 修复 ai.ts ==="
sed -i "s|/api/v1/ai/knowledge-base/search|/api/v1/ai/knowledge-base/list|g" ai.ts
sed -i "s|/api/v1/ai/prompt-template/search|/api/v1/ai/prompt/list|g" ai.ts
echo "✅ Knowledge base: /api/v1/ai/knowledge-base/search → /api/v1/ai/knowledge-base/list"
echo "✅ Prompt template: /api/v1/ai/prompt-template/search → /api/v1/ai/prompt/list"

echo ""
echo "=== 修复 abtest.ts ==="
sed -i "s|/api/v1/abtest/experiment/search|/api/v1/abtest/experiment/list|g" abtest.ts
echo "✅ ABTest: /api/v1/abtest/experiment/search → /api/v1/abtest/experiment/list"

echo ""
echo "=== 修复 system.ts ==="
sed -i "s|/api/v1/messaging/notification/search|/api/v1/messaging/config/list|g" system.ts
sed -i "s|/api/v1/storage/file/search|/api/v1/storage/list|g" system.ts
echo "✅ Messaging: /api/v1/messaging/notification/search → /api/v1/messaging/config/list"
echo "✅ Storage: /api/v1/storage/file/search → /api/v1/storage/list"

echo ""
echo "=========================================="
echo "✅ P0 路径修复完成（12个路径）"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 重新运行验证: node e2e-validation.js"
echo "2. 检查剩余失败的 API（P1 待确认路径）"
