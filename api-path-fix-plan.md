# API 路径映射修复方案

## 已确认的路径映射

### Live 模块
| 前端错误路径 | 后端正确路径 | 修复文件 |
|-------------|-------------|---------|
| `/api/v1/live/persona/search` | `/api/v1/douyin/persona/list` | `front/src/api/live.ts` |
| `/api/v1/live/navigation/search` | `/api/v1/live/script-navigation/search` | `front/src/api/live.ts` |

### Copy 模块
| 前端错误路径 | 后端正确路径 | 修复文件 |
|-------------|-------------|---------|
| `/api/v1/copy/search` | `/api/v1/copy/library/search` | `front/src/api/copy.ts` |
| `/api/v1/copy/tag/search` | 需要检查是否存在 | `front/src/api/copy.ts` |

### SMS 模块
| 前端错误路径 | 后端正确路径 | 修复文件 |
|-------------|-------------|---------|
| `/api/v1/sms/template/search` | `/api/v1/sms/template/list` | `front/src/api/sms.ts` |
| `/api/v1/sms/log/search` | 需要检查 | `front/src/api/sms.ts` |

### Workflow 模块
| 前端错误路径 | 后端正确路径 | 修复文件 |
|-------------|-------------|---------|
| `/api/v1/workflow/search` | `/api/v1/workflow/list` (需确认) | `front/src/api/workflow.ts` |

## 需要进一步检查的模块

以下模块的 API 端点可能完全不存在，需要：
1. 确认后端是否实现了这些 Controller
2. 如果未实现，需要创建 Controller stub
3. 如果已实现但路径不同，需要更新前端调用

### ShortVideo 模块 (4个API)
- `/api/v1/shortvideo/video/search`
- `/api/v1/shortvideo/script/search`
- `/api/v1/shortvideo/storyboard/search`
- `/api/v1/shortvideo/template/search`

### Agent 模块 (3个API)
- `/api/v1/agent/search`
- `/api/v1/agent/market/list`
- `/api/v1/ai/multi-agent/list`

### AI 模块 (3个API)
- `/api/v1/ai/knowledge-base/search`
- `/api/v1/ai/prompt-template/search`
- `/api/v1/ai/call-log/search`

### ABTest 模块 (1个API)
- `/api/v1/abtest/experiment/search`

### Benchmark 模块 (3个API)
- `/api/v1/benchmark/account/search`
- `/api/v1/benchmark/video/search`
- `/api/v1/benchmark/quality-script/search`

### System 模块 (9个API)
- `/api/v1/auth/permission/search`
- `/api/v1/auth/organization/search`
- `/api/v1/config/search`
- `/api/v1/log/operation/search`
- `/api/v1/log/audit/search`
- `/api/v1/system/alert-rule/search`
- `/api/v1/messaging/notification/search`
- `/api/v1/storage/file/search`

### Payment 模块 (3个API)
- `/api/v1/payment/order/search` (405错误，可能是GET/POST方法问题)
- `/api/v1/payment/transaction/search`
- `/api/v1/payment/refund/search`

### Script 模块 (1个API)
- `/api/v1/script/search` (后端有 `/api/v1/script/list`)

### Product 模块 (1个API)
- `/api/v1/product/category/search`

## 修复优先级

### P0 - 立即修复（已确认路径）
1. Live persona: `/api/v1/live/persona/search` → `/api/v1/douyin/persona/list`
2. Copy library: `/api/v1/copy/search` → `/api/v1/copy/library/search`
3. SMS template: `/api/v1/sms/template/search` → `/api/v1/sms/template/list`
4. Script: `/api/v1/script/search` → `/api/v1/script/list`

### P1 - 需要扫描确认
扫描 backend-api-paths.md 找出所有模块的实际路径

### P2 - 创建缺失的 API
如果某些 API 确实不存在，需要创建 Controller stub 返回空数据

## 自动修复脚本

```bash
# 修复已确认的路径
cd front/src/api

# 1. Live persona
sed -i "s|/api/v1/live/persona/search|/api/v1/douyin/persona/list|g" live.ts

# 2. Copy library
sed -i "s|/api/v1/copy/search|/api/v1/copy/library/search|g" copy.ts

# 3. SMS template
sed -i "s|/api/v1/sms/template/search|/api/v1/sms/template/list|g" sms.ts

# 4. Script
sed -i "s|/api/v1/script/search|/api/v1/script/list|g" script.ts
```
