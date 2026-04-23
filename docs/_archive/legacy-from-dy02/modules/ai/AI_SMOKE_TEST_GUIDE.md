# AI 模块冒烟测试指南（S1–S9）

> 版本：1.0 | 更新日期：2026-02-28

## 前置条件

1. 后端已启动：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`
2. 依赖服务已就绪：PostgreSQL、Redis、Milvus（可选）、Elasticsearch（可选）
3. 已获取有效 Token：`POST /api/v1/auth/login` 返回的 `data.token`

## 环境变量

```bash
# 示例：获取 Token 后设置
export TOKEN="your_jwt_token_here"
export BASE="http://localhost:8188/api/v1"
```

## 冒烟测试用例

| # | 场景 | 方法 | 路径 | 预期 |
|---|------|------|------|------|
| S1 | AI 任务创建 | POST | `/ai/task/create` | 200，返回 taskId |
| S2 | 知识库检索 | POST | `/ai/knowledge-base/{kbId}/search` | 200，返回知识条目列表 |
| S3 | 额度查询 | GET | `/ai/quota/info` | 200，返回 usedCount/maxCount/remaining |
| S4 | 模型列表 | POST | `/ai/model/list` | 200，返回模型配置列表 |
| S5 | AI 图像生成 | POST | `/ai/media/image/text2img` | 200，返回图片 URL（需 Stable Diffusion） |
| S6 | AI TTS | POST | `/ai/media/tts/generate` | 200，返回音频 URL（需 TTS 服务） |
| S7 | 进化状态 | POST | `/ai/admin/evolve/status` | 200，返回 recentTasks/topicCount 等 |
| S8 | 主题池列表 | GET | `/ai/admin/evolve/topic/list` | 200，返回主题列表 |
| S9 | 手动触发进化 | POST | `/ai/admin/evolve/trigger` | 200，任务创建成功 |

## cURL 示例

### S1：AI 任务创建

```bash
curl -X POST "$BASE/ai/task/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"taskType":"copywriting","inputContent":"测试","prompt":"生成一段文案"}'
```

### S2：知识库检索

```bash
# 需先创建知识库并导入文档，获取 kbId
curl -X POST "$BASE/ai/knowledge-base/1/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"直播话术","topK":5}'
```

### S3：额度查询

```bash
curl -X GET "$BASE/ai/quota/info" \
  -H "Authorization: Bearer $TOKEN"
```

### S4：模型列表

```bash
curl -X POST "$BASE/ai/model/list" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}'
```

### S5：AI 图像生成（需 Stable Diffusion）

```bash
curl -X POST "$BASE/ai/media/image/text2img" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt":"一只可爱的猫","width":512,"height":512}'
```

### S6：AI TTS（需 TTS 服务）

```bash
curl -X POST "$BASE/ai/media/tts/generate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"text":"测试语音","voiceId":"default"}'
```

### S7：进化状态

```bash
curl -X POST "$BASE/ai/admin/evolve/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}'
```

### S8：主题池列表

```bash
curl -X GET "$BASE/ai/admin/evolve/topic/list" \
  -H "Authorization: Bearer $TOKEN"
```

### S9：手动触发进化

```bash
curl -X POST "$BASE/ai/admin/evolve/trigger" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}'
```

## 验收标准

- [ ] 冒烟测试 S1–S9 全部返回 HTTP 200（S5/S6 依赖外部服务时可能 5xx，可跳过）
- [ ] 响应体 `status=200` 表示业务成功
- [ ] 知识库检索（S2）需 Milvus/ES 就绪；进化相关（S7–S9）需进化引擎表就绪

## 单元测试

```bash
# QueryRewrite 与 Reranker 服务单元测试
mvn test -Dtest=QueryRewriteServiceImplTest,RerankerServiceImplTest
```
