# 01 后端安全与架构分析

## 1. 安全漏洞

### 1.1 CRITICAL: userId 硬编码

| 文件 | 行号 | 代码 |
|------|------|------|
| `module/script/controller/ScriptGenerationController.java` | 22 | `Long userId = 1L; // 临时使用` |
| `module/payment/service/impl/OrderServiceImpl.java` | 195 | `return 1L; // TODO：从上下文获取` |

### 1.2 HIGH: owner_id 校验缺失

| 文件 | 方法 | 问题 |
|------|------|------|
| `module/product/controller/ProductController.java:63` | `productService.getById(id)` | 无 userId 参数 |

### 1.3 @CurrentUserId 采用率

- 已采用：4 个 Controller（product 模块）
- 未采用：88 个 Controller（使用 AuthTokenFilter.getUserId）
- 硬编码：1 个 Controller

## 2. 架构问题

### 2.1 BaseSpecificationBuilder 采用率

仅测试文件使用。**0 个 ServiceImpl** 实际使用 BaseSpecificationBuilder。

### 2.2 N+1 查询

| 文件 | 行号 | 严重性 |
|------|------|--------|
| `live/service/impl/LiveProductServiceImpl.java` | 118-131 | HIGH: batchSort() 循环 findById+save |
| `ai/service/EvolveEngineService.java` | 519 | MEDIUM |
| `ai/service/impl/EvolveRoiServiceImpl.java` | 92 | MEDIUM |

### 2.3 RuntimeException 直接抛出（13个文件）

应统一替换为 BusinessException：
1. `payment/service/DouyinPaymentService.java`
2. `ai/service/impl/VectorServiceImpl.java`
3. `ai/service/impl/OpenAiCompatibleLlmClient.java`
4. `ai/service/VideoQualityScoreService.java`
5. `ai/service/impl/VideoGenerationServiceImpl.java`
6. `ai/service/impl/ComfyUIServiceImpl.java`
7. `common/util/MappedBiggerFileWriterUtil.java`
8. `ai/service/VideoAnalysisService.java`
9. `auth/service/impl/CaptchaServiceImpl.java`
10. `common/util/Md5Hasher.java`
11. `product/service/impl/ScriptOptimizationServiceImpl.java`
12. `common/service/AsyncProcessingService.java`
13. `common/service/PressureTestingService.java`

### 2.4 超大类（>500行）

| 文件 | 行数 |
|------|------|
| `live/service/impl/LiveAiServiceImpl.java` | 1193 |
| `ai/service/impl/KnowledgeBaseServiceImpl.java` | 1015 |
| `ai/service/impl/EvolutionServiceImpl.java` | 848 |
| `product/service/impl/ProductScriptVersionServiceImpl.java` | 761 |
| `ai/service/EvolveEngineService.java` | 721 |
| `shortvideo/service/WorkflowExecutionService.java` | 703 |
