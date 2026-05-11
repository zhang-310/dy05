# API 路径映射表（前端 → 后端实际路径）

## 已确认的路径映射

| 前端错误路径 | 后端正确路径 | 修复文件 | 状态 |
|-------------|-------------|---------|------|
| `/api/v1/live/persona/search` | `/api/v1/douyin/persona/list` | `front/src/api/live.ts` | ✅ 已确认 |
| `/api/v1/live/navigation/search` | `/api/v1/live/script-navigation/search` | `front/src/api/live.ts` | ✅ 已确认 |
| `/api/v1/copy/search` | `/api/v1/copy/library/search` | `front/src/api/copy.ts` | ✅ 已确认 |
| `/api/v1/copy/tag/search` | 需要检查是否存在 | `front/src/api/copy.ts` | ⚠️ 待确认 |
| `/api/v1/sms/template/search` | `/api/v1/sms/template/list` | `front/src/api/sms.ts` | ✅ 已确认 |
| `/api/v1/sms/log/search` | `/api/v1/sms/log/list` | `front/src/api/sms.ts` | ✅ 已确认 |
| `/api/v1/script/search` | `/api/v1/script/list` | `front/src/api/script.ts` | ✅ 已确认 |
| `/api/v1/workflow/search` | 需要检查 | `front/src/api/workflow.ts` | ⚠️ 待确认 |
| `/api/v1/shortvideo/video/search` | 需要检查 | `front/src/api/shortvideo.ts` | ⚠️ 待确认 |
| `/api/v1/shortvideo/script/search` | 需要检查 | `front/src/api/shortvideo.ts` | ⚠️ 待确认 |
| `/api/v1/shortvideo/storyboard/search` | 需要检查 | `front/src/api/shortvideo.ts` | ⚠️ 待确认 |
| `/api/v1/shortvideo/template/search` | 需要检查 | `front/src/api/shortvideo.ts` | ⚠️ 待确认 |
| `/api/v1/agent/search` | `/api/v1/agent/list` | `front/src/api/agent.ts` | ✅ 已确认 |
| `/api/v1/agent/market/list` | 需要检查 | `front/src/api/agent.ts` | ⚠️ 待确认 |
| `/api/v1/ai/multi-agent/list` | `/api/v1/agent/workflow/list` | `front/src/api/agent.ts` | ✅ 已确认 |
| `/api/v1/ai/knowledge-base/search` | `/api/v1/ai/knowledge-base/list` | `front/src/api/ai.ts` | ✅ 已确认 |
| `/api/v1/ai/prompt-template/search` | `/api/v1/ai/prompt/list` | `front/src/api/ai.ts` | ✅ 已确认 |
| `/api/v1/ai/call-log/search` | `/api/v1/ai/admin/call-log/search` | `front/src/api/ai.ts` | ✅ 已确认 |
| `/api/v1/abtest/experiment/search` | `/api/v1/abtest/experiment/list` | `front/src/api/abtest.ts` | ✅ 已确认 |
| `/api/v1/benchmark/account/search` | 需要检查 | `front/src/api/benchmark.ts` | ⚠️ 待确认 |
| `/api/v1/benchmark/video/search` | 需要检查 | `front/src/api/benchmark.ts` | ⚠️ 待确认 |
| `/api/v1/benchmark/quality-script/search` | 需要检查 | `front/src/api/benchmark.ts` | ⚠️ 待确认 |
| `/api/v1/auth/permission/search` | 需要检查 | `front/src/api/system.ts` | ⚠️ 待确认 |
| `/api/v1/auth/organization/search` | 需要检查 | `front/src/api/system.ts` | ⚠️ 待确认 |
| `/api/v1/config/search` | 需要检查 | `front/src/api/system.ts` | ⚠️ 待确认 |
| `/api/v1/log/operation/search` | 需要检查 | `front/src/api/system.ts` | ⚠️ 待确认 |
| `/api/v1/log/audit/search` | 需要检查 | `front/src/api/system.ts` | ⚠️ 待确认 |
| `/api/v1/system/alert-rule/search` | 需要检查 | `front/src/api/system.ts` | ⚠️ 待确认 |
| `/api/v1/messaging/notification/search` | `/api/v1/messaging/config/list` | `front/src/api/system.ts` | ✅ 已确认 |
| `/api/v1/storage/file/search` | `/api/v1/storage/list` | `front/src/api/system.ts` | ✅ 已确认 |
| `/api/v1/payment/order/search` | 需要检查（405错误） | `front/src/api/payment.ts` | ⚠️ 待确认 |
| `/api/v1/payment/transaction/search` | 需要检查 | `front/src/api/payment.ts` | ⚠️ 待确认 |
| `/api/v1/payment/refund/search` | 需要检查 | `front/src/api/payment.ts` | ⚠️ 待确认 |
| `/api/v1/product/category/search` | 需要检查 | `front/src/api/product.ts` | ⚠️ 待确认 |

## 修复优先级

### P0 - 立即修复（已确认路径，10个）
1. ✅ Live persona: `/api/v1/live/persona/search` → `/api/v1/douyin/persona/list`
2. ✅ Copy library: `/api/v1/copy/search` → `/api/v1/copy/library/search`
3. ✅ SMS template: `/api/v1/sms/template/search` → `/api/v1/sms/template/list`
4. ✅ SMS log: `/api/v1/sms/log/search` → `/api/v1/sms/log/list`
5. ✅ Script: `/api/v1/script/search` → `/api/v1/script/list`
6. ✅ Agent: `/api/v1/agent/search` → `/api/v1/agent/list`
7. ✅ Multi-agent: `/api/v1/ai/multi-agent/list` → `/api/v1/agent/workflow/list`
8. ✅ Knowledge base: `/api/v1/ai/knowledge-base/search` → `/api/v1/ai/knowledge-base/list`
9. ✅ Prompt template: `/api/v1/ai/prompt-template/search` → `/api/v1/ai/prompt/list`
10. ✅ ABTest: `/api/v1/abtest/experiment/search` → `/api/v1/abtest/experiment/list`
11. ✅ Messaging: `/api/v1/messaging/notification/search` → `/api/v1/messaging/config/list`
12. ✅ Storage: `/api/v1/storage/file/search` → `/api/v1/storage/list`

### P1 - 需要扫描确认（23个）
需要在 backend-api-paths.md 中查找实际路径

## 自动修复脚本

```bash
#!/bin/bash
# 修复已确认的 P0 路径

cd front/src/api

# 1. Live persona
sed -i "s|/api/v1/live/persona/search|/api/v1/douyin/persona/list|g" live.ts

# 2. Copy library
sed -i "s|/api/v1/copy/search|/api/v1/copy/library/search|g" copy.ts

# 3. SMS template
sed -i "s|/api/v1/sms/template/search|/api/v1/sms/template/list|g" sms.ts

# 4. SMS log
sed -i "s|/api/v1/sms/log/search|/api/v1/sms/log/list|g" sms.ts

# 5. Script
sed -i "s|/api/v1/script/search|/api/v1/script/list|g" script.ts

# 6. Agent
sed -i "s|/api/v1/agent/search|/api/v1/agent/list|g" agent.ts

# 7. Multi-agent
sed -i "s|/api/v1/ai/multi-agent/list|/api/v1/agent/workflow/list|g" agent.ts

# 8. Knowledge base
sed -i "s|/api/v1/ai/knowledge-base/search|/api/v1/ai/knowledge-base/list|g" ai.ts

# 9. Prompt template
sed -i "s|/api/v1/ai/prompt-template/search|/api/v1/ai/prompt/list|g" ai.ts

# 10. ABTest
sed -i "s|/api/v1/abtest/experiment/search|/api/v1/abtest/experiment/list|g" abtest.ts

# 11. Messaging
sed -i "s|/api/v1/messaging/notification/search|/api/v1/messaging/config/list|g" system.ts

# 12. Storage
sed -i "s|/api/v1/storage/file/search|/api/v1/storage/list|g" system.ts

echo "P0 路径修复完成"
```
