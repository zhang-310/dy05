# StyleRecommendationController API 测试指南

## API端点列表

### 1. 混合推荐风格
**端点**：`POST /api/v1/product/style-recommendation/recommend-hybrid`

**描述**：结合ML模型（70%）和规则引擎（30%）推荐最佳风格

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/recommend-hybrid \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "productId": 1,
    "topK": 5
  }'
```

**请求参数**：
```json
{
  "productId": 1,      // 必填，商品ID
  "topK": 5            // 可选，返回Top-K个推荐，默认5
}
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": [
    {
      "styleCode": "professional",
      "styleName": "专业",
      "confidence": 0.85,
      "reason": "混合推荐：ML模型 + 规则引擎",
      "source": "hybrid",
      "expectedScore": 82.5,
      "similarProducts": [
        {
          "productId": 5,
          "productName": "护肤精华液A",
          "similarity": 0.92,
          "bestStyle": "professional",
          "bestStyleScore": 85.0
        }
      ]
    },
    {
      "styleCode": "warm",
      "styleName": "温暖",
      "confidence": 0.72,
      "reason": "混合推荐：ML模型 + 规则引擎",
      "source": "hybrid",
      "expectedScore": 78.3,
      "similarProducts": []
    }
  ],
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

### 2. ML模型推荐
**端点**：`POST /api/v1/product/style-recommendation/recommend-ml`

**描述**：仅使用机器学习模型推荐风格（不混合规则引擎）

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/recommend-ml \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "productId": 1,
    "topK": 3
  }'
```

**响应**：与混合推荐类似，但 `source` 字段为 `"ml_model"`

---

### 3. 训练推荐模型（同步）
**端点**：`POST /api/v1/product/style-recommendation/train`

**描述**：使用当前用户的历史数据训练ML模型（同步，会阻塞直到训练完成）

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/train \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": {
    "modelId": 123,
    "message": "模型训练成功"
  },
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

**错误响应**（训练样本不足）：
```json
{
  "status": 400,
  "message": "训练样本不足，至少需要10个商品，当前只有5个",
  "data": null,
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

### 4. 训练推荐模型（异步）
**端点**：`POST /api/v1/product/style-recommendation/train-async`

**描述**：后台异步训练ML模型，不阻塞请求

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/train-async \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": {
    "modelId": 456,
    "message": "模型训练任务已提交，请稍后查询结果"
  },
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

### 5. 获取模型性能指标
**端点**：`GET /api/v1/product/style-recommendation/metrics`

**描述**：查询当前激活模型的性能指标

**请求示例**：
```bash
curl -X GET http://localhost:8080/api/v1/product/style-recommendation/metrics \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": {
    "modelId": 123,
    "modelType": "collaborative_filtering",
    "modelVersion": "1.0",
    "trainingSamples": 50,
    "accuracy": 0.85,
    "precision": 0.82,
    "recall": 0.78,
    "f1Score": 0.80,
    "top3HitRate": 0.72,
    "avgScoreImprovement": 5.3,
    "trainedAt": "2026-04-05T00:00:00",
    "isActive": true,
    "recentFeedbackCount": 25,
    "totalRecommendations": 100,
    "status": "active"
  },
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

**响应示例（无模型）**：
```json
{
  "status": 200,
  "message": "success",
  "data": {
    "status": "no_model"
  },
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

### 6. 记录推荐反馈
**端点**：`POST /api/v1/product/style-recommendation/record-feedback`

**描述**：记录用户对推荐结果的选择，用于模型优化

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/record-feedback \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "productId": 1,
    "recommendedStyles": ["professional", "warm", "enthusiastic"],
    "selectedStyles": ["professional", "warm"]
  }'
```

**请求参数**：
```json
{
  "productId": 1,
  "recommendedStyles": ["professional", "warm", "enthusiastic"],  // 推荐的风格列表
  "selectedStyles": ["professional", "warm"]                      // 用户实际选择的风格
}
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": null,
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

### 7. 查找相似商品
**端点**：`POST /api/v1/product/style-recommendation/similar-products`

**描述**：基于特征相似度查找相似商品

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/similar-products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "productId": 1,
    "topK": 5
  }'
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": [
    {
      "productId": 5,
      "productName": "护肤精华液A",
      "similarity": 0.92,
      "bestStyle": "professional",
      "bestStyleScore": 85.0
    },
    {
      "productId": 8,
      "productName": "护肤精华液B",
      "similarity": 0.87,
      "bestStyle": "warm",
      "bestStyleScore": 82.3
    }
  ],
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

### 8. 计算商品特征向量
**端点**：`POST /api/v1/product/style-recommendation/compute-features`

**描述**：提取商品特征并缓存，用于相似度计算

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/compute-features \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "productId": 1
  }'
```

**响应示例**：
```json
{
  "status": 200,
  "message": "success",
  "data": {
    "productId": 1,
    "featureVector": "{\"category\":\"护肤品\",\"price_normalized\":0.3,\"keywords\":[\"科技\",\"成分\"],\"historical_avg_score\":82.5,\"historical_usage_count\":15}",
    "message": "特征计算成功"
  },
  "traceId": "abc123",
  "timestamp": 1712246400000
}
```

---

## 测试流程

### 场景1：首次使用（冷启动）

1. **创建商品**
   ```bash
   # 创建至少10个商品（训练模型需要）
   ```

2. **查看推荐**（此时使用规则引擎）
   ```bash
   curl -X POST http://localhost:8080/api/v1/product/style-recommendation/recommend-hybrid \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{"productId": 1, "topK": 5}'
   ```

3. **生成话术并添加效果数据**
   ```bash
   # 为每个商品生成话术
   # 模拟使用次数、评分等效果数据
   ```

4. **训练模型**
   ```bash
   curl -X POST http://localhost:8080/api/v1/product/style-recommendation/train \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

5. **查看模型指标**
   ```bash
   curl -X GET http://localhost:8080/api/v1/product/style-recommendation/metrics \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

---

### 场景2：使用ML推荐

1. **创建新商品**

2. **获取混合推荐**
   ```bash
   curl -X POST http://localhost:8080/api/v1/product/style-recommendation/recommend-hybrid \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{"productId": 11, "topK": 5}'
   ```

3. **查看相似商品**
   ```bash
   curl -X POST http://localhost:8080/api/v1/product/style-recommendation/similar-products \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{"productId": 11, "topK": 5}'
   ```

4. **选择推荐的风格生成话术**

5. **记录反馈**
   ```bash
   curl -X POST http://localhost:8080/api/v1/product/style-recommendation/record-feedback \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{
       "productId": 11,
       "recommendedStyles": ["professional", "warm", "enthusiastic"],
       "selectedStyles": ["professional"]
     }'
   ```

---

## Postman Collection

可以导入以下Postman Collection进行测试：

```json
{
  "info": {
    "name": "Style Recommendation API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "混合推荐",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"},
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"productId\": 1, \"topK\": 5}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/v1/product/style-recommendation/recommend-hybrid",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "product", "style-recommendation", "recommend-hybrid"]
        }
      }
    },
    {
      "name": "训练模型",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "url": {
          "raw": "{{baseUrl}}/api/v1/product/style-recommendation/train",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "product", "style-recommendation", "train"]
        }
      }
    },
    {
      "name": "获取模型指标",
      "request": {
        "method": "GET",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "url": {
          "raw": "{{baseUrl}}/api/v1/product/style-recommendation/metrics",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "product", "style-recommendation", "metrics"]
        }
      }
    }
  ],
  "variable": [
    {"key": "baseUrl", "value": "http://localhost:8080"},
    {"key": "token", "value": "YOUR_TOKEN_HERE"}
  ]
}
```

---

## 常见错误

### 错误1：未登录
```json
{
  "status": 2001,
  "message": "未登录",
  "data": null
}
```
**解决**：添加有效的Authorization header

### 错误2：训练样本不足
```json
{
  "status": 400,
  "message": "训练样本不足，至少需要10个商品，当前只有5个",
  "data": null
}
```
**解决**：创建更多商品数据

### 错误3：无相似商品
```json
{
  "status": 200,
  "message": "success",
  "data": []
}
```
**说明**：正常情况，表示没有找到相似商品，会自动回退到规则引擎

---

## 下一步

1. 使用Postman测试所有API端点
2. 验证推荐准确度
3. 测试模型训练流程
4. 集成到前端页面
