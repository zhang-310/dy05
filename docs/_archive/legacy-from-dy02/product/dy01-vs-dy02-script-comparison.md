# dy01 vs dy02 商品话术系统对比分析

## 一、核心架构对比

### 1.1 多维度生成能力

| 维度 | dy01 | dy02 | 差异说明 |
|------|------|------|---------|
| **风格维度** | 8种（professional, friendly, passionate, seeding, promotion, enthusiastic, casual, warm） | 8种（相同） | ✅ 一致 |
| **类型维度** | 3种（seed, promotion, formal） | 3种（相同） | ✅ 一致 |
| **场景维度** | ✅ 5种（short_video, guopin, cangbo, danpin, yubo） | ✅ 5种（已实现） | ✅ 一致 |
| **时长维度** | ✅ 30/60/90/120秒 | ✅ 30/60/90/120秒 | ✅ 一致 |
| **人设维度** | ✅ 支持 persona_id | ✅ 支持 persona_id | ✅ 一致 |

### 1.2 数据库表结构对比

#### dy_product_script 表

| 字段 | dy01 | dy02 | 说明 |
|------|------|------|------|
| scene | ✅ VARCHAR(32) | ✅ VARCHAR(32) | 都支持场景字段 |
| is_emotional | ❌ 无 | ✅ BOOLEAN | dy02 支持情绪价值话术 |
| source | ✅ VARCHAR(16) | ✅ VARCHAR(16) | 都支持来源标记 |
| 版本管理 | ✅ 按 (productId, scriptType, style) | ✅ 按 (productId, scriptType, style) | ✅ 一致 |

**dy02 优势**：
- 支持情绪价值话术（is_emotional 字段）
- product_id 可为 null（用于纯情绪话术）

**dy01 优势**：
- （无明显优势，dy02 已实现场景字段）

---

## 二、功能特性对比

### 2.1 话术生成流程

#### dy01 特有功能

1. **A/B 实验集成**
   ```typescript
   // dy01: ProductScriptManageDialog.tsx
   const assignRes = await assignScriptStyle({
     productId, scriptType, experimentType: 'script_style'
   })
   let stylesToUse = assignRes?.styleCode ? [assignRes.styleCode] : genStyles
   ```
   - 生成前检查是否有运行中的风格实验
   - 若有实验，自动使用实验分配的风格
   - ✅ **dy02 已实现**：在 `ProductScriptServiceImpl.generateMultiStyleScripts()` 中集成

2. **推荐风格 API**
   ```java
   // dy01: StylePresetController.java
   @PostMapping("/recommend")
   public RESTResult<List<String>> recommendStyles(
       @RequestBody Map<String, Object> body) {
       // 根据产品特征推荐最佳风格
   }
   ```
   - ✅ **dy02 已实现**：`StylePresetServiceImpl.recommendStyles()` 基于分类/价格/卖点推荐

3. **风格融合模式**
   ```typescript
   // dy01: 支持 fusionMode
   const [fusionMode, setFusionMode] = useState(false)
   // 将多种风格融合为一个话术
   ```
   - ⚠️ **dy02 部分支持**：前端 UI 有 fusionMode 开关，后端需在 Prompt 中实现融合逻辑

4. **场景化生成**
   ```java
   // dy01: ProductScriptGenerateVO.java
   private String scene; // short_video/guopin/cangbo/danpin/yubo
   ```
   - 不同场景生成不同话术风格
   - ✅ **dy02 已实现**：完整支持 scene 维度，Prompt 中已加入场景说明

#### dy02 特有功能

1. **情绪价值话术**
   ```java
   // dy02: DyProductScript.java
   @Column(name = "is_emotional")
   private Boolean isEmotional = false;
   ```
   - 支持不绑定产品的纯情绪话术
   - **dy01 缺失**

2. **商品上播准备度检测**
   ```java
   // dy02: ProductReadinessController.java
   @PostMapping("/readiness")
   public RESTResult<ProductReadinessVO> checkReadiness(...)
   ```
   - 5 维度检测（基础信息、图片、价格库存、话术、卖点）
   - **dy01 缺失**

### 2.2 RAG 知识库参考

| 特性 | dy01 | dy02 | 说明 |
|------|------|------|------|
| 基础 RAG 检索 | ✅ | ✅ | 都支持 |
| 查询重写服务 | ✅ QueryRewriteService | ❌ 无 | dy01 会扩展查询变体 |
| 并行检索 | ✅ 最多 12 个查询 | ✅ 支持 | 一致 |
| 超时控制 | ✅ 5 秒 | ✅ 5 秒 | 一致 |
| 混合搜索 | ✅ 向量 + 关键词 | ✅ 支持 | 一致 |

**dy01 优势**：
- 查询重写提升检索质量
- 更智能的查询扩展

### 2.3 批量生成与进度推送

| 特性 | dy01 | dy02 | 说明 |
|------|------|------|------|
| SSE 流式推送 | ✅ | ✅ | 都支持 |
| 并发生成 | ✅ aiTaskExecutor | ✅ aiTaskExecutor | 一致 |
| 进度事件结构 | ✅ 详细 | ✅ 详细 | 一致 |
| 最大产品数 | 100 | 100 | 一致 |
| 最大风格数 | 10 | 10 | 一致 |

**一致性**：批量生成机制完全一致

### 2.4 版本管理

| 特性 | dy01 | dy02 | 说明 |
|------|------|------|------|
| 版本号维度 | (productId, scriptType, style) | (productId, scriptType, style) | ✅ 一致 |
| 激活状态隔离 | ✅ 按风格隔离 | ✅ 按风格隔离 | ✅ 一致 |
| 版本历史表 | ✅ script_version_history | ✅ script_version_history | ✅ 一致 |
| 回滚功能 | ✅ | ✅ | ✅ 一致 |

**一致性**：版本管理机制完全一致

---

## 三、API 端点对比

### 3.1 dy01 独有端点

| 端点 | 功能 |
|------|------|
| `/api/v1/product/style-preset/recommend` | 推荐风格 |
| `/api/v1/abtest/script-style/assign` | A/B 实验分配 |

### 3.2 dy02 独有端点

| 端点 | 功能 |
|------|------|
| `/api/v1/product/readiness` | 商品上播准备度检测 |
| `/api/v1/product/script/generate` | 单个话术生成（新增） |
| `/api/v1/product/script/usage-list` | 话术使用统计（新增） |
| `/api/v1/product/script/statistics` | 话术统计信息（新增） |

### 3.3 共有端点

| 端点 | dy01 | dy02 | 说明 |
|------|------|------|------|
| `/product/script/list` | ✅ | ✅ | 获取产品所有话术 |
| `/product/script/list-by-type` | ✅ | ✅ | 按类型获取 |
| `/product/script/list-by-style` | ✅ | ✅ | 按风格分组 |
| `/product/script/active-by-style` | ✅ | ✅ | 获取激活话术 |
| `/product/script/generate-multi-style` | ✅ | ✅ | 多风格生成 |
| `/product/script/generate-batch-stream` | ✅ | ✅ | 批量生成 SSE |
| `/product/script/activate/{id}` | ✅ | ✅ | 激活话术 |
| `/product/script/update/{id}` | ✅ | ✅ | 更新话术 |
| `/product/script/{id}` | ✅ | ✅ | 删除话术 |
| `/product/script/history` | ✅ | ✅ | 版本历史 |
| `/product/script/rollback/{id}` | ✅ | ✅ | 回滚版本 |
| `/product/style-preset/list` | ✅ | ✅ | 风格预设列表 |

---

## 四、前端交互对比

### 4.1 话术生成对话框

| 特性 | dy01 | dy02 | 说明 |
|------|------|------|------|
| 风格多选 | ✅ | ✅ | 一致 |
| 推荐风格标签 | ✅ | ❌ | dy01 有智能推荐 |
| 风格融合开关 | ✅ | ❌ | dy01 支持融合模式 |
| 场景选择 | ✅ | ❌ | dy01 有场景下拉 |
| 人设选择 | ✅ | ✅ | 一致 |
| 时长选择 | ✅ | ✅ | 一致 |
| RAG 开关 | ✅ | ✅ | 一致 |
| A/B 实验提示 | ✅ | ❌ | dy01 显示实验状态 |

### 4.2 批量生成进度

| 特性 | dy01 | dy02 | 说明 |
|------|------|------|------|
| 步骤列表展示 | ✅ | ✅ | 一致 |
| 实时进度更新 | ✅ | ✅ | 一致 |
| 失败重试 | ✅ | ✅ | 一致 |
| 取消生成 | ✅ | ✅ | 一致 |

---

## 五、核心差异总结

### dy01 优势

1. ~~✅ **场景化生成**：5 种场景（short_video, guopin, cangbo, danpin, yubo）~~ → ✅ dy02 已实现
2. ~~✅ **A/B 实验集成**：自动使用实验分配的风格~~ → ✅ dy02 已实现
3. ~~✅ **智能推荐风格**：根据产品特征推荐最佳风格~~ → ✅ dy02 已实现
4. ⚠️ **风格融合模式**：将多种风格融合为一个话术（dy02 前端有 UI，后端需完善 Prompt）
5. ✅ **查询重写服务**：RAG 检索前扩展查询变体

### dy02 优势

1. ✅ **情绪价值话术**：支持不绑定产品的纯情绪话术
2. ✅ **商品上播准备度检测**：5 维度智能检测
3. ✅ **单个话术生成 API**：快速生成单个风格
4. ✅ **话术使用统计**：详细的使用数据分析
5. ✅ **场景化生成**：完整实现 5 种场景维度
6. ✅ **A/B 实验集成**：自动分配风格并记录实验数据
7. ✅ **智能推荐风格**：基于分类/价格/卖点三维度推荐

### 共同优势

1. ✅ 多维度生成（风格、类型、时长、人设、场景）
2. ✅ 版本管理（按 productId+scriptType+style 维度）
3. ✅ 批量生成 + SSE 进度推送
4. ✅ RAG 知识库参考
5. ✅ 合规检测
6. ✅ 激活状态管理
7. ✅ 版本历史与回滚

---

## 六、dy02 升级建议

### ✅ 已完成（2026-04-04）

1. **添加场景维度**
   - ✅ 数据库：添加 `scene VARCHAR(32)` 字段
   - ✅ 后端：在生成接口中支持 scene 参数
   - ✅ 前端：API 已支持 scene 参数传递
   - ✅ Prompt：根据场景调整生成策略（LivePromptBuilder）

2. **A/B 实验集成**
   - ✅ 在 `ProductScriptServiceImpl.generateMultiStyleScripts()` 中集成实验分配
   - ✅ 返回结果包含 abExperimentId 和 abVariantId
   - ⚠️ 前端显示实验状态提示（待实现）

3. **智能推荐风格**
   - ✅ 实现 `/product/style-preset/recommend` 接口
   - ✅ 根据产品分类、价格、AI卖点推荐风格
   - ⚠️ 前端调用推荐接口并显示推荐标签（待实现）

### 优先级 P1（增强功能）

4. **风格融合模式完善**
   - ⚠️ 在 Prompt 中加入多风格融合逻辑（前端 UI 已有，后端需实现）
   - ✅ 前端已有融合模式开关

5. **查询重写服务**
   - 实现 QueryRewriteService
   - 扩展 RAG 检索查询变体
   - 提升检索质量

### 优先级 P2（优化功能）

6. **话术效果追踪**
   - 记录话术在直播中的使用次数
   - 统计话术带来的 GMV
   - 生成效果报告

7. **话术模板库**
   - 预置高质量话术模板
   - 支持模板变量替换
   - 快速生成标准话术

---

## 七、实施路线图

### ✅ 第一阶段：场景化生成（已完成 2026-04-04）

```sql
-- 1. 数据库迁移 ✅
ALTER TABLE dy_product_script ADD COLUMN scene VARCHAR(32);
CREATE INDEX idx_product_script_scene ON dy_product_script(scene);
COMMENT ON COLUMN dy_product_script.scene IS '应用场景：short_video/guopin/cangbo/danpin/yubo';
```

```java
// 2. 后端实体更新 ✅
@Column(name = "scene", length = 32)
private String scene;
```

```java
// 3. 生成接口支持 scene ✅
public MultiStyleGenerateResultVO generateMultiStyleScripts(
    MultiStyleGenerateRequestVO vo, Long userId) {
    // 在 Prompt 中加入场景说明 ✅
    String scenePrompt = buildScenePrompt(vo.getScene());
    // ...
}
```

```typescript
// 4. 前端对话框添加场景选择 ✅
const SCENES = [
  { value: 'short_video', label: '短视频带货' },
  { value: 'guopin', label: '过品带货' },
  { value: 'cangbo', label: '仓播带货' },
  { value: 'danpin', label: '单品直播间' },
  { value: 'yubo', label: '娱播穿插' },
]
```

### ✅ 第二阶段：A/B 实验集成（已完成 2026-04-04）

```java
// 1. 在生成前检查实验 ✅
@Autowired(required = false)
private ScriptStyleAbService scriptStyleAbService;

public MultiStyleGenerateResultVO generateMultiStyleScripts(...) {
    // 检查是否有运行中的风格实验
    if (scriptStyleAbService != null && !vo.getStyles().isEmpty()) {
        String userFingerprint = "user_" + userId + "_product_" + vo.getProductId();
        ScriptStyleAssignVO assignment = scriptStyleAbService.assignStyle(
            userId, "product", vo.getProductId(), userFingerprint
        );
        if (assignment != null && assignment.getStyleCode() != null) {
            finalStyles = List.of(assignment.getStyleCode());
            result.setAbExperimentId(assignment.getExperimentId());
            result.setAbVariantId(assignment.getVariantId());
        }
    }
    // 继续生成流程...
}
```

### ✅ 第三阶段：智能推荐（已完成 2026-04-04）

```java
// 1. 实现推荐算法 ✅
@Service
public class StylePresetServiceImpl implements StylePresetService {
    public List<String> recommendStyles(Long productId, Long userId) {
        DyProduct product = productRepository.findById(productId).orElseThrow();

        List<String> recommended = new ArrayList<>();

        // 根据分类推荐
        if (product.getProductCategory() != null && product.getProductCategory().contains("护肤")) {
            recommended.add("professional");
            recommended.add("warm");
        } else if ("彩妆".equals(product.getProductCategory())) {
            recommended.add("passionate");
            recommended.add("enthusiastic");
        }

        // 根据价格推荐
        if (product.getPrice().compareTo(new BigDecimal("200")) > 0) {
            recommended.add("professional");
        } else {
            recommended.add("friendly");
        }

        return recommended.stream().distinct().limit(3).toList();
    }
}
```

---

## 八、总结

### dy01 vs dy02 核心差异

| 维度 | dy01 强项 | dy02 强项 |
|------|----------|----------|
| **场景化** | ✅ 5 种场景 | ❌ 无 |
| **实验集成** | ✅ A/B 实验 | ❌ 无 |
| **智能推荐** | ✅ 风格推荐 | ❌ 无 |
| **风格融合** | ✅ 融合模式 | ❌ 无 |
| **情绪话术** | ❌ 无 | ✅ 支持 |
| **准备度检测** | ❌ 无 | ✅ 5 维度检测 |

### 建议

dy02 应优先实现 dy01 的场景化生成和 A/B 实验集成，这两个功能对提升话术质量和效果追踪至关重要。同时保留 dy02 的情绪话术和准备度检测特色功能，形成差异化优势。

最终目标：**dy02 = dy01 核心能力 + dy02 特色功能**
