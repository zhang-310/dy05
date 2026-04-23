# Phase 2 - 知识库系统 API 文档

## 概述

Phase 2 实现了对标账号分析系统的知识库功能，包括：
1. **自动入库逻辑** - 自动评估和存储高质量脚本
2. **语义相似度计算** - 向量嵌入和 Milvus 集成
3. **脚本推荐引擎** - 7 种推荐策略

---

## 1. 自动入库服务 API

### 1.1 获取入库阈值

**请求**
```
POST /api/v1/benchmark/auto-ingest/get-threshold
```

**响应**
```json
{
  "status": 200,
  "data": 70.0,
  "message": "success"
}
```

### 1.2 设置入库阈值

**请求**
```
POST /api/v1/benchmark/auto-ingest/set-threshold
Content-Type: application/json

{
  "threshold": 80.0
}
```

**响应**
```json
{
  "status": 200,
  "message": "success"
}
```

### 1.3 批量自动入库

**请求**
```
POST /api/v1/benchmark/auto-ingest/batch
Content-Type: application/json

{
  "analysisIds": [100, 101, 102]
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "id": 1,
      "videoId": 1000,
      "analysisId": 100,
      "scriptContent": "高质量脚本内容",
      "qualityScore": 85.5,
      "industry": "护肤",
      "sceneType": "直播"
    }
  ],
  "message": "success"
}
```

---

## 2. 语义相似度服务 API

### 2.1 生成向量嵌入

**请求**
```
POST /api/v1/benchmark/similarity/generate-embedding
Content-Type: application/json

{
  "scriptId": 100
}
```

**响应**
```json
{
  "status": 200,
  "data": true,
  "message": "success"
}
```

### 2.2 批量生成向量嵌入

**请求**
```
POST /api/v1/benchmark/similarity/batch-generate-embeddings
Content-Type: application/json

{
  "scriptIds": [100, 101, 102]
}
```

**响应**
```json
{
  "status": 200,
  "data": 3,
  "message": "success"
}
```

### 2.3 查找相似脚本

**请求**
```
POST /api/v1/benchmark/similarity/find-similar
Content-Type: application/json

{
  "scriptId": 100,
  "topK": 10,
  "minScore": 0.7
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 101,
      "videoId": 1001,
      "scriptContent": "相似脚本内容",
      "qualityScore": 88.0,
      "similarityScore": 0.85,
      "industry": "护肤",
      "sceneType": "直播"
    }
  ],
  "message": "success"
}
```

### 2.4 根据文本查找相似脚本

**请求**
```
POST /api/v1/benchmark/similarity/find-similar-by-text
Content-Type: application/json

{
  "text": "护肤品直播话术",
  "topK": 5,
  "minScore": 0.6
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "scriptContent": "护肤品推广脚本",
      "qualityScore": 90.0,
      "similarityScore": 0.78
    }
  ],
  "message": "success"
}
```

### 2.5 计算两个脚本的相似度

**请求**
```
POST /api/v1/benchmark/similarity/calculate
Content-Type: application/json

{
  "scriptId1": 100,
  "scriptId2": 101
}
```

**响应**
```json
{
  "status": 200,
  "data": 0.82,
  "message": "success"
}
```

### 2.6 获取未生成向量的脚本 ID

**请求**
```
POST /api/v1/benchmark/similarity/unembedded-ids
Content-Type: application/json

{
  "limit": 100
}
```

**响应**
```json
{
  "status": 200,
  "data": [100, 101, 102],
  "message": "success"
}
```

---

## 3. 推荐引擎服务 API

### 3.1 根据需求推荐脚本

**请求**
```
POST /api/v1/benchmark/recommendation/by-requirement
Content-Type: application/json

{
  "requirement": "护肤品直播话术",
  "topK": 10
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "scriptContent": "高质量护肤品直播脚本",
      "qualityScore": 90.0,
      "similarityScore": 0.85,
      "industry": "护肤",
      "sceneType": "直播"
    }
  ],
  "message": "success"
}
```

### 3.2 根据行业和场景推荐脚本

**请求**
```
POST /api/v1/benchmark/recommendation/by-industry-scene
Content-Type: application/json

{
  "industry": "护肤",
  "sceneType": "直播",
  "topK": 10
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "industry": "护肤",
      "sceneType": "直播",
      "qualityScore": 90.0
    }
  ],
  "message": "success"
}
```

### 3.3 根据脚本类型推荐

**请求**
```
POST /api/v1/benchmark/recommendation/by-script-type
Content-Type: application/json

{
  "scriptType": "产品介绍",
  "referenceScriptId": 100,
  "topK": 5
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 101,
      "scriptType": "产品介绍",
      "qualityScore": 88.0
    }
  ],
  "message": "success"
}
```

### 3.4 智能推荐

**请求**
```
POST /api/v1/benchmark/recommendation/smart
Content-Type: application/json

{
  "filters": {
    "industry": "护肤",
    "sceneType": "直播",
    "minQualityScore": 80.0
  },
  "referenceText": "优质话术",
  "topK": 10
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "qualityScore": 90.0,
      "industry": "护肤"
    }
  ],
  "message": "success"
}
```

### 3.5 获取热门脚本

**请求**
```
POST /api/v1/benchmark/recommendation/popular
Content-Type: application/json

{
  "topK": 10
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "qualityScore": 90.0,
      "engagementRate": 15.0,
      "viralScore": 85.0
    }
  ],
  "message": "success"
}
```

### 3.6 获取最新高质量脚本

**请求**
```
POST /api/v1/benchmark/recommendation/latest
Content-Type: application/json

{
  "topK": 10,
  "minQualityScore": 80.0
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "qualityScore": 90.0,
      "createTime": "2026-04-09 10:00:00"
    }
  ],
  "message": "success"
}
```

### 3.7 推荐改进脚本

**请求**
```
POST /api/v1/benchmark/recommendation/improvement
Content-Type: application/json

{
  "analysisId": 200,
  "topK": 5
}
```

**响应**
```json
{
  "status": 200,
  "data": [
    {
      "scriptId": 100,
      "qualityScore": 90.0,
      "similarityScore": 0.82
    }
  ],
  "message": "success"
}
```

---

## 数据模型

### BenchmarkQualityScript

```json
{
  "id": 100,
  "videoId": 1000,
  "analysisId": 2000,
  "scriptContent": "脚本内容",
  "scriptType": "产品介绍",
  "industry": "护肤",
  "sceneType": "直播",
  "qualityScore": 90.0,
  "engagementRate": 15.0,
  "viralScore": 85.0,
  "completionRate": 75.0,
  "aiRating": 88.0,
  "likesCount": 10000,
  "commentsCount": 500,
  "sharesCount": 200,
  "collectionsCount": 300,
  "viewsCount": 50000,
  "videoDuration": 60,
  "keyFeatures": "特色要素",
  "creativeElements": "创意元素",
  "hookStrategy": "钩子策略",
  "contentStructure": "内容结构",
  "referenceCount": 5,
  "lastReferencedAt": "2026-04-09 10:00:00",
  "createTime": "2026-04-09 10:00:00",
  "updateTime": "2026-04-09 10:00:00"
}
```

### BenchmarkScriptSimilarityVO

```json
{
  "scriptId": 100,
  "videoId": 1000,
  "scriptContent": "脚本内容",
  "scriptType": "产品介绍",
  "industry": "护肤",
  "sceneType": "直播",
  "qualityScore": 90.0,
  "similarityScore": 0.85,
  "engagementRate": 15.0,
  "viralScore": 85.0,
  "likesCount": 10000,
  "commentsCount": 500,
  "sharesCount": 200,
  "collectionsCount": 300,
  "viewsCount": 50000
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 性能指标

- 单个脚本向量生成：< 1 秒
- 相似度查询（Milvus）：< 500ms
- 推荐查询：< 1 秒
- 批量操作支持并发：最多 3 个同时处理
