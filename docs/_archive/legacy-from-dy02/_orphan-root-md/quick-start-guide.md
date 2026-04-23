# 快速启动指南 - 测试新功能

## 前置准备

### 1. 启动依赖服务
```bash
cd docker
docker compose up -d postgres redis rabbitmq elasticsearch
```

### 2. 执行数据库迁移
```bash
# 连接到PostgreSQL
psql -h localhost -p 5433 -U postgres -d dy02

# 执行ML推荐表创建脚本
\i sql/product/create-ml-recommendation-tables.sql

# 验证表创建
\dt style_*
\dt product_feature_cache
```

### 3. 启动后端
```bash
mvn spring-boot:run
```

### 4. 启动前端
```bash
cd front
npm run dev
```

访问：http://localhost:5173

---

## 功能测试指南

### 测试1：预览功能

#### 步骤1：进入商品话术管理页面
1. 登录系统
2. 进入"商品管理"
3. 选择任意商品，点击"话术管理"

#### 步骤2：配置生成参数
1. 选择2-3个风格（如：专业、温暖、热情）
2. 选择场景（如：开场、产品介绍）
3. 设置时长（默认60秒）

#### 步骤3：点击预览
1. 点击"预览"按钮（在"生成"按钮左侧）
2. 等待预览生成（约5-10秒）
3. 查看预览对话框

#### 预期结果
- ✅ 显示所有选中风格的15秒片段
- ✅ 每个风格卡片显示风格名称和内容
- ✅ 可以点击复制按钮复制内容
- ✅ 可以点击"调整配置"返回修改
- ✅ 可以点击"满意，开始生成"触发完整生成

#### API测试（可选）
```bash
# 使用curl测试预览API
curl -X POST http://localhost:8080/api/v1/product/script/preview-styles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "productId": 1,
    "styles": ["professional", "warm", "enthusiastic"],
    "scriptType": "formal",
    "scene": "opening",
    "duration": 60,
    "useKbRef": true
  }'
```

---

### 测试2：智能推荐（规则引擎 - 冷启动）

#### 步骤1：创建新商品
1. 进入"商品管理"
2. 点击"新增商品"
3. 填写商品信息：
   - 商品名称：护肤精华液
   - 分类：护肤品
   - 价格：299元
   - 卖点：科技成分、专利配方
4. 保存商品

#### 步骤2：查看推荐风格
1. 进入该商品的"话术管理"页面
2. 观察风格选择区域
3. 查看带星标⭐的推荐风格

#### 预期结果
- ✅ 推荐风格：专业、温暖（基于规则引擎）
- ✅ 推荐风格带星标图标
- ✅ 推荐理由：基于分类（护肤品）和价格（中端）

---

### 测试3：智能推荐（ML模型）

**注意**：此功能需要先训练模型，需要至少10个商品数据。

#### 步骤1：准备训练数据
1. 创建至少10个商品
2. 为每个商品生成至少1条话术
3. 模拟一些效果数据（使用次数、评分等）

#### 步骤2：训练模型（API调用）
```bash
# 训练ML模型
curl -X POST http://localhost:8080/api/v1/product/style-recommendation/train \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 查询模型指标
curl -X GET http://localhost:8080/api/v1/product/style-recommendation/metrics \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 步骤3：测试ML推荐
1. 创建新商品
2. 进入话术管理页面
3. 观察推荐风格（应该基于相似商品）

#### 预期结果
- ✅ 推荐风格基于相似商品的历史效果
- ✅ 显示推荐置信度
- ✅ 显示相似商品列表（如果前端已集成）

---

### 测试4：集成测试（预览 + 推荐 + 生成）

#### 完整流程
1. 进入商品话术管理页面
2. 观察推荐的风格（带星标）
3. 选择推荐的风格
4. 点击"预览"查看效果
5. 满意后点击"满意，开始生成"
6. 等待生成完成
7. 查看生成的话术

#### 预期结果
- ✅ 推荐准确
- ✅ 预览快速
- ✅ 生成成功
- ✅ 话术质量高

---

## 数据库验证

### 查看预览调用记录
```sql
-- 查看最近的预览调用（通过日志）
SELECT * FROM sys_log_operation
WHERE request_uri LIKE '%preview-styles%'
ORDER BY create_time DESC
LIMIT 10;
```

### 查看ML模型记录
```sql
-- 查看已训练的模型
SELECT * FROM style_recommendation_model
WHERE deleted = 0
ORDER BY trained_at DESC;

-- 查看推荐反馈
SELECT * FROM style_recommendation_feedback
WHERE deleted = 0
ORDER BY created_at DESC
LIMIT 10;

-- 查看特征缓存
SELECT product_id, price_normalized, historical_avg_score, computed_at
FROM product_feature_cache
WHERE deleted = 0
ORDER BY computed_at DESC
LIMIT 10;
```

---

## 常见问题排查

### 问题1：预览按钮不显示
**原因**：前端代码未正确集成
**解决**：
1. 检查 `ScriptGeneratePanel.tsx` 是否有 `onPreview` 属性
2. 检查 `ProductScriptManagePage.tsx` 是否传递了 `onPreview` 回调
3. 清除浏览器缓存，重新加载

### 问题2：预览生成失败
**原因**：后端API错误或参数不正确
**解决**：
1. 检查浏览器控制台错误信息
2. 检查后端日志：`logs/application.log`
3. 验证请求参数是否正确

### 问题3：推荐风格不显示星标
**原因**：推荐API未调用或返回空
**解决**：
1. 检查 `ProductScriptManagePage.tsx` 中的 `loadProduct()` 方法
2. 检查 `productApi.stylePresetRecommend()` 调用
3. 检查后端 `StylePresetService.recommendStyles()` 实现

### 问题4：ML模型训练失败
**原因**：训练样本不足（<10个商品）
**解决**：
1. 创建更多商品数据
2. 为商品生成话术并添加效果数据
3. 重新调用训练API

### 问题5：推荐结果不准确
**原因**：训练数据质量差或算法参数需调整
**解决**：
1. 检查训练数据的多样性
2. 调整相似度阈值（当前0.3）
3. 调整混合推荐权重（当前ML 70% + 规则 30%）

---

## 性能监控

### 监控预览生成时间
```bash
# 查看预览API响应时间
grep "preview-styles" logs/performance.log | tail -20
```

### 监控ML推荐响应时间
```bash
# 查看推荐API响应时间
grep "recommend" logs/performance.log | tail -20
```

### 监控模型训练时间
```sql
-- 查看训练日志
SELECT training_status, training_duration_ms, sample_count, started_at, completed_at
FROM model_training_log
WHERE deleted = 0
ORDER BY started_at DESC
LIMIT 10;
```

---

## 下一步开发

### 待实现：StyleRecommendationController

创建文件：`src/main/java/cn/gaifan/douyinOperations/module/product/controller/StyleRecommendationController.java`

```java
@RestController
@RequestMapping("/api/v1/product/style-recommendation")
public class StyleRecommendationController {

    @Resource
    private StyleRecommendationMLService mlService;

    @PostMapping("/recommend-hybrid")
    public RESTResult<List<StyleRecommendationVO>> recommendHybrid(
            HttpServletRequest request,
            @RequestBody Map<String, Object> params) {
        Long userId = AuthTokenFilter.getUserId(request);
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer topK = params.containsKey("topK") ?
            Integer.valueOf(params.get("topK").toString()) : 5;

        List<StyleRecommendationVO> result = mlService.recommendHybrid(productId, userId, topK);
        return RESTResult.getSuccess(result);
    }

    @PostMapping("/train")
    public RESTResult<Long> trainModel(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        Long modelId = mlService.trainModel(userId);
        return RESTResult.getSuccess(modelId);
    }

    @GetMapping("/metrics")
    public RESTResult<ModelMetricsVO> getMetrics(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        ModelMetricsVO metrics = mlService.getModelMetrics(userId);
        return RESTResult.getSuccess(metrics);
    }
}
```

### 待实现：前端推荐结果展示

在 `ScriptGeneratePanel.tsx` 中显示推荐置信度和相似商品。

---

## 测试检查清单

- [ ] 预览功能UI正常显示
- [ ] 预览生成速度 < 10秒
- [ ] 预览内容可以复制
- [ ] 预览后可以调整配置
- [ ] 预览后可以确认生成
- [ ] 规则引擎推荐显示星标
- [ ] ML模型可以成功训练
- [ ] ML推荐返回结果
- [ ] 混合推荐结合两种策略
- [ ] 推荐反馈自动记录
- [ ] 数据库表创建成功
- [ ] 后端编译无错误
- [ ] 前端构建无错误

---

## 联系支持

如遇到问题，请检查：
1. 后端日志：`logs/application.log`
2. 前端控制台：Chrome DevTools
3. 数据库连接：确保PostgreSQL运行正常
4. 端口占用：8080（后端）、5173（前端）

祝测试顺利！🚀
