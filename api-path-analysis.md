# API 路径不匹配分析报告

## 问题概述
前端页面验证发现 35 个 API 返回 500 错误，根本原因是前端调用的 API 路径与后端实际路径不匹配。

## 已确认的路径不匹配

### 1. Live 模块

| 前端调用路径 | 后端实际路径 | 状态 |
|-------------|-------------|------|
| `/api/v1/live/persona/search` | `/api/v1/douyin/persona/search` | ❌ 不匹配 |
| `/api/v1/live/navigation/search` | `/api/v1/live/script-navigation/search` | ❌ 不匹配 |
| `/api/v1/live/analytics/overview` | 需要检查 | ❌ 可能不存在 |

### 2. ShortVideo 模块

| 前端调用路径 | 状态 |
|-------------|------|
| `/api/v1/shortvideo/video/search` | ❌ 500 错误 |
| `/api/v1/shortvideo/script/search` | ❌ 500 错误 |
| `/api/v1/shortvideo/storyboard/search` | ❌ 500 错误 |
| `/api/v1/shortvideo/template/search` | ❌ 500 错误 |

### 3. Agent 模块

| 前端调用路径 | 状态 |
|-------------|------|
| `/api/v1/agent/search` | ❌ 500 错误 |
| `/api/v1/agent/market/list` | ❌ 500 错误 |
| `/api/v1/ai/multi-agent/list` | ❌ 500 错误 |

### 4. 其他模块

| 模块 | 前端调用路径 | 状态 |
|------|-------------|------|
| Product | `/api/v1/product/category/search` | ❌ 500 |
| Copy | `/api/v1/copy/search` | ❌ 500 |
| Copy | `/api/v1/copy/tag/search` | ❌ 500 |
| AI | `/api/v1/ai/knowledge-base/search` | ❌ 500 |
| AI | `/api/v1/ai/prompt-template/search` | ❌ 500 |
| AI | `/api/v1/ai/call-log/search` | ❌ 500 |
| ABTest | `/api/v1/abtest/experiment/search` | ❌ 500 |
| Benchmark | `/api/v1/benchmark/account/search` | ❌ 500 |
| Benchmark | `/api/v1/benchmark/video/search` | ❌ 500 |
| Benchmark | `/api/v1/benchmark/quality-script/search` | ❌ 500 |
| System | `/api/v1/auth/permission/search` | ❌ 500 |
| System | `/api/v1/auth/organization/search` | ❌ 500 |
| System | `/api/v1/config/search` | ❌ 500 |
| System | `/api/v1/log/operation/search` | ❌ 500 |
| System | `/api/v1/log/audit/search` | ❌ 500 |
| System | `/api/v1/system/alert-rule/search` | ❌ 500 |
| System | `/api/v1/messaging/notification/search` | ❌ 500 |
| System | `/api/v1/storage/file/search` | ❌ 500 |
| System | `/api/v1/workflow/search` | ❌ 500 |
| Payment | `/api/v1/payment/order/search` | ❌ 405 (方法不允许) |
| Payment | `/api/v1/payment/transaction/search` | ❌ 500 |
| Payment | `/api/v1/payment/refund/search` | ❌ 500 |
| Script | `/api/v1/script/search` | ❌ 500 |
| SMS | `/api/v1/sms/template/search` | ❌ 500 |
| SMS | `/api/v1/sms/log/search` | ❌ 500 |

## 验证统计

- **总计**: 111 个页面
- **通过**: 72 个 (64.9%)
- **失败**: 35 个 (31.5%)
- **跳过**: 4 个 (3.6%)

## 下一步行动

1. **扫描所有 Controller** - 生成完整的后端 API 路径清单
2. **对比前端调用** - 找出所有不匹配的路径
3. **修复方案选择**:
   - 方案 A: 修改前端 API 调用路径（推荐，改动小）
   - 方案 B: 修改后端 Controller 路径（影响大，需要测试）
   - 方案 C: 添加路由别名/重定向（临时方案）

## 建议

优先修复高频使用的模块：
1. Live 模块（直播核心功能）
2. ShortVideo 模块（短视频核心功能）
3. Agent 模块（智能体核心功能）
4. System 模块（系统管理功能）
