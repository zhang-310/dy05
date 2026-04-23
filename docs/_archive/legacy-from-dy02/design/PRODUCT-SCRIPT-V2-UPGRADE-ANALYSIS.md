# 产品话术 AI 多风格生成 v2.0 升级分析

> 分析日期：2026-03-04
> 基于规划文档 v2.0（gentle-doodling-koala.md）
> 对照当前代码库实施状态
> **Phase 1a 已实施完成（2026-03-04）**

---

## 一、现状与缺口对照

### 1.1 已实现（此前迭代）

| 功能 | 位置 | 说明 |
|------|------|------|
| 产品分类自动推断 | ProductService.inferProductType() | 根据利润率/亏损/featured/控单策略推断 |
| 产品分类话术时长 | LiveAiServiceImpl.buildDurationHintForProductType() | 爆品/控单/利润品/亏品/平价品差异化 |
| control 分类值 | LiveProduct.productType | 控单产品支持 |
| 情绪价值话术生成 | LiveAiService.generateEmotionalScript() | 4 类情绪话术 |
| 前端选品分类多选 | LiveScriptBuilderPage | 添加产品时 inferProductType + 多选 |
| dy_product_script.is_emotional | 已迁移 | 情绪话术复用表 |

### 1.2 v2.0 核心缺口（Phase 1）

| 模块 | 缺口项 | 优先级 |
|------|--------|--------|
| **ProductScript 模块** | generateMultiStyleScripts（AI 多风格生成） | P0 |
| **ProductScript 模块** | listScriptsByStyle / getActiveScriptsByStyle | P0 |
| **合规** | ComplianceService 规则引擎 | P0 |
| **Repository** | 按 productId+scriptType+**style** 版本管理、deactivateOthers | P0 |
| **Controller** | POST generate-multi-style、GET list-by-style、GET active-scripts | P0 |
| **VO** | MultiStyleGenerateRequestVO、MultiStyleGenerateResultVO | P0 |
| **错误码** | 3141-3147 | P1 |
| **异步+SSE** | 异步任务、SSE 进度推送 | P1（可延后，先同步） |
| **前端** | 产品话术管理页、AI 生成对话框 | P1 |

### 1.3 版本管理机制差异

| 维度 | 当前实现 | v2.0 要求 |
|------|----------|-----------|
| 版本号维度 | productId + scriptType | productId + scriptType + **style** |
| 激活范围 | 同 scriptType 唯一激活 | 同 **style** 唯一激活 |
| 并发安全 | 无锁 | SELECT FOR UPDATE 行锁 |

---

## 二、升级实施计划

### Phase 1a：同步版 MVP（推荐首轮）

不引入 SSE/异步，先实现同步多风格生成，验证流程。

| 步骤 | 任务 | 预估 |
|------|------|------|
| 1 | SQL：索引 + style 维度注释 | 0.5h |
| 2 | Repository：findMaxVersionByProductIdAndScriptTypeAndStyle、deactivateOthersByStyle | 0.5h |
| 3 | ComplianceService：规则引擎（绝对化用语、医疗功效） | 1h |
| 4 | ProductScriptService：generateMultiStyleScripts（同步） | 2h |
| 5 | ProductScriptService：listScriptsByStyle、getActiveScriptsByStyle | 0.5h |
| 6 | Controller：3 个新 API | 0.5h |
| 7 | VO 类 + 错误码 | 0.5h |
| 8 | 前端：产品话术管理页 + 生成对话框 | 2h |

### Phase 1b：异步 + SSE（可选）

在 Phase 1a 验证通过后，增加：
- 异步任务 + taskId
- SSE 端点 `/generate-progress/{taskId}`
- 前端 GenerationProgress 组件

---

## 三、关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 首轮实现方式 | 同步生成 | 快速验证，避免 SSE 复杂度 |
| 版本号并发 | @Lock(PESSIMISTIC_WRITE) | 简单可靠，与文档一致 |
| 合规检测 | 复用 ViolationWordService 或新建 ComplianceService | 现有 script 模块有违规词检测，可扩展 |
| AI 调用 | 复用 LlmClient / LiveAiServiceImpl 的 LLM 调用 | 避免重复建设 |

---

## 四、与 Live 模块的关系

- **Live 模块**：直播场次话术生成（generateProduct、generateFull 等），使用 LiveScript。
- **Product 模块**：产品库预生成话术，存入 dy_product_script，供 Live 选品引用。
- **打通点**：Live 选品时 scriptSource=product，productScriptId 引用 dy_product_script.id；需支持按风格选择激活版本。

---

## 五、依赖关系

```
ComplianceService (新建)
       ↓
ProductScriptServiceImpl.generateMultiStyleScripts
       ↓
LlmClient (ai 模块) / 或通过 LiveAiService 间接调用
       ↓
DyProductScriptRepository (扩展)
```

---

## 六、风险与缓解

| 风险 | 缓解 |
|------|------|
| LlmClient 在 product 模块不可用 | 已通过 LiveAiService 委托，无循环依赖 |
| 合规规则与 ViolationWordService 重复 | ComplianceService 独立实现，可选注入 ViolationWordService 增强 |

---

## 七、Phase 1a 实施清单（已完成）

| 项 | 文件/位置 | 说明 |
|----|-----------|------|
| SQL 迁移 | sql/product/migration-v2-style-version.sql | 索引 + style_preset 表 |
| Repository | DyProductScriptRepository | findMaxVersionByProductIdAndScriptTypeAndStyle、deactivateByProductIdAndScriptTypeAndStyle、findByProductIdAndScriptTypeAndStyleAndDeletedOrderByVersionDesc |
| ComplianceService | product.service.ComplianceService | 绝对化用语自动修复、医疗功效拦截、违规词库 |
| ProductScriptService | generateMultiStyleScripts、listScriptsByStyle、getActiveScriptsByStyle | 同步多风格生成 |
| Controller | ProductScriptController | POST generate-multi-style、GET list-by-style、GET active-by-style |
| VO | MultiStyleGenerateRequestVO、MultiStyleGenerateResultVO | 请求/响应 |
| 错误码 | ErrorCode 3141-3147 | 已注册 |
| 前端 | ProductScriptManageDialog、ProductPage 话术按钮 | 商品管理页点击「话术」打开对话框 |

**执行迁移：** `psql -U postgres -d douyin_ops -f sql/product/migration-v2-style-version.sql`

---

## 八、Phase 1b 实施清单（已完成）

| 项 | 说明 |
|----|------|
| 限流 | ProductScriptRateLimitService：每用户每分钟 5 次（固定窗口） |
| SSE 进度 | POST /product/script/generate-batch-stream 返回 text/event-stream |
| 批量生成 | generateBatchWithProgress：最多 100 品 × 10 风格 |
| 前端 | 商品管理页勾选产品 → 批量生成话术 → SSE 实时进度条 |
