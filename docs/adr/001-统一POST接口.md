# ADR-001：统一 POST 接口（含例外）

## 状态

已采纳（含明确例外）

## 上下文

传统 RESTful API 使用 GET/POST/PUT/DELETE 区分操作语义，但在实际开发中：
- 前端统一调用方式更简单
- POST 可以携带复杂查询参数（Body），避免 GET 的 URL 长度限制
- 统一方法便于安全审计和日志记录

## 决策

**主规则**：所有业务 API 接口统一使用 POST 方法，包括查询接口。通过 URL 路径的动作词区分语义：
- `/list` / `/search` — 列表查询
- `/get` — 单条查询
- `/save` — 新增或更新
- `/delete` — 删除

**明确例外**：以下场景允许使用其他 HTTP 方法：

### GET 方法（11 处）

| 路径 | 说明 |
|------|------|
| `/api/v1/short-video/ai/generate-copy-sse` | SSE 流式生成文案 |
| `/api/v1/short-video/ai/generate-script-sse` | SSE 流式生成脚本 |
| `/api/v1/live/monitor/stream/{sessionId}` | SSE 直播监控流 |
| `/api/v1/live/realtime-panel/stream/{sessionId}` | SSE 实时面板推送 |
| `/api/v1/douyin/oauth/callback` | 抖音 OAuth 回调（浏览器重定向） |
| `/api/v1/auth/oauth/authorize` | OAuth 授权 URL |
| `/api/v1/auth/oauth/callback` | OAuth 回调 |
| `/api/v1/messaging/webhook/wecom` | 企微 URL 校验 |
| `/api/v1/system/metrics/prometheus` | Prometheus scrape |
| `/api/v1/monitoring/stream/realtime` | SSE 实时指标流 |
| `/api/v1/monitoring/stream/alerts` | SSE 告警流 |

GET 例外原因：
- **SSE 流**：部分 SSE 使用标准 `EventSource` API（仅支持 GET），另一部分使用自定义 `ssePost`（支持 POST）
- **OAuth 回调**：第三方平台回调强制 GET
- **Prometheus**：标准 scrape 协议要求 GET
- **Webhook 校验**：企微 URL 验证强制 GET

### PUT 方法（2 处）

| 路径 | 说明 |
|------|------|
| `/api/v1/product/script/activate/{scriptId}` | 激活话术 |
| `/api/v1/product/script/update/{scriptId}` | 更新话术 |

### DELETE 方法（7 处）

| 路径 | 说明 |
|------|------|
| `/api/v1/ai/knowledge-base/{kbId}` | 删除知识库 |
| `/api/v1/ai/knowledge-base/document/{docId}` | 删除文档 |
| `/api/v1/ai/admin/evolve/topic/{id}` | 删除进化主题 |
| `/api/v1/ai/admin/evolve/task/{id}` | 删除进化任务 |
| `/api/v1/product/script/{scriptId}` | 删除话术 |
| `/api/v1/product/style-preset/{id}` | 删除风格预设 |
| `/api/v1/attribution/session/{sessionId}` | 删除场次归因 |

## SSE 流式接口的两种实现

| 方式 | HTTP 方法 | 前端实现 | 使用场景 |
|------|-----------|----------|----------|
| `ssePost` | POST | `utils/sse-client.ts`（fetch + ReadableStream） | 直播话术生成、Agent 对话、批量生成等 |
| `EventSource` | GET | 浏览器原生 API | 直播实时面板、监控指标流 |

**POST SSE**（10 处）：路径含 `-sse`、`chat-stream`、`-stream` 后缀
**GET SSE**（4 处）：路径含 `/stream/` 前缀

## 前端 HTTP 客户端

`utils/request.ts` 暴露了 `post`、`get`、`put`、`delete` 四个方法，实际使用分布：

| 方法 | 使用数量 | 说明 |
|------|----------|------|
| `request.post` | 绝大多数 | 主要调用方式 |
| `request.get` | 6 处 | agent/payment/upload |
| `request.delete` | 4 处 | ai 知识库/进化模块 |
| `ssePost` | 10 处 | 流式生成 |
| `EventSource` | 4 处 | 实时订阅 |
| `fetch` 直接调用 | 4 处 | AI 流式对话、文件导出 |

## 后果

- 正面：90%+ 接口统一 POST，前端 HTTP 客户端处理简单
- 正面：复杂查询参数通过 Body 传递，无长度限制
- 负面：不符合标准 REST 语义，不利于 HTTP 缓存
- 注意：非 POST 接口共 20 处，需在开发中明确标注
