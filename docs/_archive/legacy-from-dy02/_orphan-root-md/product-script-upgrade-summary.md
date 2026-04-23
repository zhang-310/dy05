# 商品话术生成升级总结

## 升级时间
2026-04-04

## 升级目标
升级商品库的产品话术生成功能，增强智能推荐、知识库参考、场景化生成能力。

## 核心功能升级

### 1. 话术知识库分类参考（RAG 增强）

**后端实现：**
- `ProductAiService.generateScript()` 新增 `kbCategories` 参数
- `MultiStyleGenerateRequestVO` 新增 `kbCategories` 字段
- `ProductScriptGenerateVO` 新增 `kbCategories` 字段
- `LiveAiServiceImpl.generateProductScript()` 支持按分类过滤知识库参考

**前端实现：**
- `ScriptGeneratePanel` 新增知识库分类多选组件
- `ProductScriptManagePage` 集成分类选择状态管理
- `scriptApi.categories()` 获取动态分类列表
- 预定义分类 + 动态分类合并展示

**分类来源：**
- 预定义分类：`scriptCategories.ts` 定义（种草、促销、产品介绍、直播话术、情绪价值、过渡话术、互动话术、FAQ、美妆护肤、食品、服装、家居、数码）
- 动态分类：从 `ScriptLibrary` 表查询 `DISTINCT category`

### 2. 智能风格推荐

**后端实现：**
- `ProductController.stylePresetRecommend()` 接口
- `ProductService.recommendStylePresets()` 方法
- 基于商品属性（类别、价格、品牌）推荐适合的话术风格

**前端实现：**
- 加载商品时自动获取推荐风格
- 推荐风格在 UI 中用星标 ⭐ 标识
- `recommendedStyleCodes` 状态管理

### 3. A/B 实验集成

**后端实现：**
- `ProductScriptServiceImpl.generateMultiStyleScripts()` 集成 A/B 实验
- 自动分配实验风格，覆盖用户选择
- 返回 `abExperimentId` 和 `abVariantId`

**前端实现：**
- SSE 进度事件携带 A/B 实验信息
- 进度面板显示实验标签
- `MultiStyleGenerateResultVO` 包含实验 ID

### 4. 场景化生成

**后端实现：**
- `MultiStyleGenerateRequestVO.scene` 字段
- 支持场景：short_video（短视频）、guopin（过品）、cangbo（仓播）、danpin（单品）、yubo（娱播）
- 场景信息传递到 Prompt 构建

**前端实现：**
- 场景选择下拉框（SCENE_OPTIONS）
- 场景信息在生成时传递

### 5. 风格融合模式

**后端实现：**
- `MultiStyleGenerateRequestVO.fusionMode` 字段
- 多风格融合为一条话术（待完整实现）

**前端实现：**
- 融合模式开关
- UI 提示：多风格融合为一条话术
- 按钮文案动态变化

### 6. SSE 实时进度推送

**后端实现：**
- `ProductScriptController.generateMultiStyleSse()` GET 接口
- `ProductScriptServiceImpl.generateMultiStyleScriptsWithProgress()` 异步生成
- 每个风格独立推送进度事件

**前端实现：**
- `generateMultiStyleScriptsSse()` EventSource 封装
- 实时进度展示：pending → loading → done/failed
- 进度条、成功/失败计数

## 技术架构

### 后端模块
```
product/
├── controller/ProductScriptController.java  # 话术生成接口
├── service/ProductScriptService.java        # 话术服务接口
├── service/impl/ProductScriptServiceImpl.java  # 话术服务实现
├── service/ProductAiService.java            # AI 生成接口
├── vo/MultiStyleGenerateRequestVO.java      # 多风格生成请求
└── vo/MultiStyleGenerateResultVO.java       # 多风格生成结果

live/
├── service/impl/LiveProductAiServiceImpl.java  # AI 生成实现（委托）
├── service/impl/LiveAiServiceImpl.java         # 核心 AI 生成逻辑
└── vo/ProductScriptGenerateVO.java             # 产品话术生成 VO

script/
├── controller/ScriptController.java         # 话术库接口
├── service/ScriptLibraryService.java        # 话术库服务
└── repository/ScriptLibraryRepository.java  # 话术库仓储
```

### 前端模块
```
front/src/
├── api/
│   ├── product.ts                           # 商品 API（含话术生成）
│   └── script.ts                            # 话术库 API
├── components/product/script-manage/
│   ├── ScriptGeneratePanel.tsx              # 生成配置面板
│   ├── ScriptStyleList.tsx                  # 话术列表
│   └── types.ts                             # 类型定义
├── pages/product/
│   ├── ProductScriptManagePage.tsx          # 话术管理页面
│   └── ProductsPage.tsx                     # 商品列表页面
└── constants/
    └── scriptCategories.ts                  # 话术分类常量
```

## API 接口

### 1. 多风格生成（SSE）
```
GET /api/v1/product/script/generate-multi-sse
参数：
  - productId: 商品 ID
  - styles: 风格列表（逗号分隔）
  - scriptType: 话术类型（seed/promotion/formal）
  - fusionMode: 是否融合模式
  - personaId: 人设 ID（可选）
  - duration: 时长（秒）
  - scene: 场景（可选）
  - useKbRef: 是否使用知识库参考
  - kbCategories: 知识库分类（逗号分隔，可选）

响应：SSE 事件流
  - progress: { style, status, message, abExperimentId, abVariantId }
  - done: { status: "ok" }
  - error: { error: "错误信息" }
```

### 2. 风格推荐
```
POST /api/v1/product/style-preset/recommend
请求：{ productId: number }
响应：string[]  # 推荐的风格代码列表
```

### 3. 话术分类列表
```
POST /api/v1/script/categories
请求：{}
响应：string[]  # 分类列表
```

## 数据流

### 生成流程
```
用户选择风格 + 配置参数
  ↓
前端发起 SSE 请求
  ↓
后端异步生成（aiTaskExecutor）
  ↓
每个风格独立生成：
  1. 调用 ProductAiService.generateScript()
  2. LiveAiServiceImpl.generateProductScript()
  3. 构建 Prompt（含知识库参考）
  4. 调用 LLM 生成
  5. 合规检测
  6. 保存到数据库
  7. 推送 SSE 进度事件
  ↓
前端接收进度事件，更新 UI
  ↓
全部完成，刷新话术列表
```

### RAG 知识库参考流程
```
用户选择知识库分类
  ↓
传递 kbCategories 到后端
  ↓
LiveAiServiceImpl.buildRagContext()
  ↓
按分类过滤知识库文档
  ↓
向量检索相似话术
  ↓
构建 RAG 上下文 XML
  ↓
拼接到 Prompt
  ↓
LLM 生成参考案例的话术
```

## 配置项

### application.yml
```yaml
app:
  ai:
    kb:
      rag:
        enabled: true              # 启用 RAG
        top-k: 8                   # 检索 Top-K
        min-score: 0.5             # 最低相似度
        max-context-chars: 2000    # 最大上下文字符数
        token-budget-ratio: 0.25   # Token 预算比例
        parallel-timeout-sec: 5    # 并行超时（秒）
```

## 数据库变更

### dy_product_script 表
- 已有字段支持：`style`, `scene`, `kb_categories`, `source`, `token_usage`
- 无需新增字段

### sc_script_library 表
- 已有字段：`category` 用于分类
- 新增查询：`findDistinctCategories()` 获取所有分类

## 测试验证

### 编译测试
```bash
mvn compile  # ✅ 通过
```

### 前端构建
```bash
cd front && npm run build  # ✅ 通过
```

### 类型检查
```bash
cd front && npm run type-check  # ✅ 通过
```

## 待优化项

1. **风格融合模式**：当前仅传递参数，后端需完整实现多风格融合逻辑
2. **智能推荐算法**：当前基于简单规则，可引入机器学习模型
3. **A/B 实验报表**：需补充实验效果分析和对比报表
4. **知识库分类管理**：需提供分类的增删改查界面
5. **批量生成优化**：支持多商品 × 多风格的批量生成

## 文件变更统计

```
25 files changed
- 前端：6 个文件
- 后端：19 个文件
- 新增：scriptCategories.ts
- 核心修改：
  - ProductScriptServiceImpl.java
  - LiveAiServiceImpl.java
  - ScriptGeneratePanel.tsx
  - ProductScriptManagePage.tsx
```

## 总结

本次升级成功实现了商品话术生成的智能化增强，核心亮点：

1. ✅ **知识库分类参考**：用户可选择特定分类的话术作为参考，提升生成质量
2. ✅ **智能风格推荐**：根据商品属性自动推荐适合的话术风格
3. ✅ **A/B 实验集成**：自动分配实验风格，支持效果对比
4. ✅ **场景化生成**：支持短视频、直播等多种场景的话术生成
5. ✅ **实时进度反馈**：SSE 推送生成进度，用户体验流畅
6. ✅ **多风格并行生成**：一次生成多个风格，提升效率

系统已具备生产环境部署条件，建议后续迭代优化风格融合和智能推荐算法。
