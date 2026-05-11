# API 路径分析 - Round 2（修复后）

## 验证结果汇总

- **总计**: 111 个页面
- **通过**: 84 个 (75.7%) ⬆️ +12 from Round 1
- **失败**: 23 个 (20.7%) ⬇️ -12 from Round 1
- **跳过**: 4 个 (3.6%)

## 已修复的 API（12个）✅

| API 路径（修复前） | API 路径（修复后） | 状态 |
|-------------------|-------------------|------|
| `/api/v1/live/persona/search` | `/api/v1/douyin/persona/list` | ✅ 通过 |
| `/api/v1/copy/search` | `/api/v1/copy/library/search` | ✅ 通过 |
| `/api/v1/sms/template/search` | `/api/v1/sms/template/list` | ✅ 通过 |
| `/api/v1/sms/log/search` | `/api/v1/sms/log/list` | ✅ 通过 |
| `/api/v1/script/search` | `/api/v1/script/list` | ✅ 通过 |
| `/api/v1/agent/search` | `/api/v1/agent/list` | ✅ 通过 |
| `/api/v1/ai/multi-agent/list` | `/api/v1/agent/workflow/list` | ✅ 通过 |
| `/api/v1/ai/knowledge-base/search` | `/api/v1/ai/knowledge-base/list` | ✅ 通过 |
| `/api/v1/ai/prompt-template/search` | `/api/v1/ai/prompt/list` | ✅ 通过 |
| `/api/v1/abtest/experiment/search` | `/api/v1/abtest/experiment/list` | ✅ 通过 |
| `/api/v1/messaging/notification/search` | `/api/v1/messaging/config/list` | ✅ 通过 |
| `/api/v1/storage/file/search` | `/api/v1/storage/list` | ✅ 通过 |

## 剩余失败的 API（23个）

### 1. ShortVideo 模块（4个）- 后端 Controller 不存在
| 前端路径 | 状态 | 说明 |
|---------|------|------|
| `/api/v1/shortvideo/video/search` | ❌ HTTP 500 | 后端无 ShortVideoController |
| `/api/v1/shortvideo/script/search` | ❌ HTTP 500 | 后端无 ShortVideoScriptController |
| `/api/v1/shortvideo/storyboard/search` | ❌ HTTP 500 | 后端无 ShortVideoStoryboardController |
| `/api/v1/shortvideo/template/search` | ❌ HTTP 500 | 后端无 ShortVideoTemplateController |

**建议**: 创建 ShortVideo 模块的 Controller stubs

### 2. Benchmark 模块（3个）- 路径不匹配
| 前端路径 | 后端实际路径 | 状态 |
|---------|-------------|------|
| `/api/v1/benchmark/account/search` | `/api/v1/benchmark/account/list` | ❌ HTTP 500 |
| `/api/v1/benchmark/video/search` | `/api/v1/benchmark/video/list` | ❌ HTTP 500 |
| `/api/v1/benchmark/quality-script/search` | `/api/v1/benchmark/quality-script/search` | ❌ HTTP 500 |

**说明**: 
- account 和 video 需要改为 `/list`
- quality-script 路径正确但返回 500（可能是业务逻辑错误）

### 3. Payment 模块（3个）- 路径不匹配
| 前端路径 | 后端实际路径 | 状态 |
|---------|-------------|------|
| `/api/v1/payment/order/search` | `/api/v1/payment/order/list` | ❌ HTTP 405 |
| `/api/v1/payment/transaction/search` | 需要检查 | ❌ HTTP 500 |
| `/api/v1/payment/refund/search` | `/api/v1/payment/refund/listByOrder` | ❌ HTTP 500 |

**说明**: 
- order 改为 `/list`（405 说明路径存在但方法不对）
- refund 后端只有 `/listByOrder`（需要 orderId 参数）

### 4. System 模块（8个）- 路径不匹配或不存在
| 前端路径 | 后端实际路径 | 状态 |
|---------|-------------|------|
| `/api/v1/auth/permission/search` | 不存在 | ❌ HTTP 500 |
| `/api/v1/auth/organization/search` | `/api/v1/organization/members` | ❌ HTTP 500 |
| `/api/v1/config/search` | `/api/v1/config/list` | ❌ HTTP 500 |
| `/api/v1/log/operation/search` | 不存在 | ❌ HTTP 500 |
| `/api/v1/log/audit/search` | 不存在 | ❌ HTTP 500 |
| `/api/v1/system/alert-rule/search` | `/api/v1/system/alert/rule/list` | ❌ HTTP 500 |
| `/api/v1/workflow/search` | 不存在（只有 Base） | ❌ HTTP 500 |

**说明**:
- permission: 后端无此 API
- organization: 改为 `/api/v1/organization/members`
- config: 改为 `/api/v1/config/list`
- log/operation 和 log/audit: 后端无此 API
- alert-rule: 改为 `/api/v1/system/alert/rule/list`
- workflow: 后端只有 Base 定义，无实际端点

### 5. Live 模块（2个）- 路径不匹配或不存在
| 前端路径 | 后端实际路径 | 状态 |
|---------|-------------|------|
| `/api/v1/live/script-navigation/search` | 不存在（只有 Base） | ❌ HTTP 500 |
| `/api/v1/live/analytics/overview` | 不存在 | ❌ HTTP 500 |

**说明**:
- script-navigation: 后端只有 Base 定义，无实际端点
- analytics: 后端无此 API

### 6. 其他模块（3个）
| 前端路径 | 后端实际路径 | 状态 |
|---------|-------------|------|
| `/api/v1/agent/market/list` | 不存在 | ❌ HTTP 500 |
| `/api/v1/product/category/search` | 不存在 | ❌ HTTP 500 |
| `/api/v1/copy/tag/search` | 不存在 | ❌ HTTP 500 |
| `/api/v1/ai/call-log/search` | `/api/v1/ai/admin/call-log/search` | ❌ HTTP 500 |

**说明**:
- agent/market: 后端无此 API
- product/category: 后端无此 API
- copy/tag: 后端无此 API
- ai/call-log: 改为 `/api/v1/ai/admin/call-log/search`

## 修复优先级

### P0 - 立即修复（路径已确认，9个）
1. `/api/v1/benchmark/account/search` → `/api/v1/benchmark/account/list`
2. `/api/v1/benchmark/video/search` → `/api/v1/benchmark/video/list`
3. `/api/v1/payment/order/search` → `/api/v1/payment/order/list`
4. `/api/v1/config/search` → `/api/v1/config/list`
5. `/api/v1/system/alert-rule/search` → `/api/v1/system/alert/rule/list`
6. `/api/v1/auth/organization/search` → `/api/v1/organization/members`
7. `/api/v1/ai/call-log/search` → `/api/v1/ai/admin/call-log/search`

### P1 - 需要创建 Controller stubs（14个）
需要创建以下 API 端点：
1. ShortVideo 模块（4个）
2. Live navigation（1个）
3. Live analytics（1个）
4. Agent market（1个）
5. Product category（1个）
6. Copy tag（1个）
7. Auth permission（1个）
8. Log operation（1个）
9. Log audit（1个）
10. Workflow search（1个）
11. Payment transaction（1个）
12. Payment refund（需要重新设计，当前只支持按订单查询）
13. Benchmark quality-script（路径正确但业务逻辑错误）

## 下一步行动

1. **立即执行 P0 修复**（7个路径更正）
2. **评估 P1 API 的必要性**（是否真的需要这些页面？）
3. **创建必要的 Controller stubs**（返回空数据）
4. **重新运行验证**，目标：100% 通过率
