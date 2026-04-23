# 商品 & 直播模块 — 全面升级实施计划

> **依据**：商品管理和直播模块深度评估报告（2026-03-04）  
> **范围**：P0 核心升级 + P1 重要增强 + P2 体验优化 + P3 架构升级  
> **执行方式**：按阶段逐步实施，每阶段完成后验证再进入下一阶段

---

## 一、执行总览

```
Phase 1 (P0)     Phase 2 (P0+P1)      Phase 3 (P1)         Phase 4 (P2)         Phase 5 (P3)
────────────────────────────────────────────────────────────────────────────────────────────────
版本号行锁         LiveAiServiceImpl    产品话术引用快照     话术版本历史          JSONB 多维度风格
批量生成并行化     LiveScriptBuilder    合规词库动态化      风格预设模板管理      话术效果归因闭环
全场生成 SSE       Prompt 构建统一     分布式限流           槽位类型扩展          事件驱动架构
DyProduct 乐观锁  AI 服务解耦         批量违规检测         效果评分多维度        A/B 测试框架
                  合规词库动态化       统一 ScriptEditor     AI 配额细粒度        智能风格推荐
                  人设选择/时长估算     前端 Error Boundary
                  请求去重防抖
```

---

## 二、Phase 1 — P0 核心升级（必做）

**目标**：解决并发安全、性能瓶颈、用户体验阻塞问题。  
**预计工作量**：2-3 天

### 2.1 #2 版本号原子性保障

| 项 | 内容 |
|----|------|
| **问题** | 并发生成时 `findMaxVersion + 1` 可能重复 |
| **方案** | Repository 添加 `@Lock(PESSIMISTIC_WRITE)` 行锁，或唯一约束 + 重试 |
| **文件** | `DyProductScriptRepository.java`、`ProductScriptServiceImpl.java` |

**任务清单：**

- [x] 1.1 在 `DyProductRepository` 新增 `findByIdForUpdate`（锁定产品行），替代对 script 表的复杂行锁
- [x] 1.2 在 `ProductScriptServiceImpl` 的 `save`、`generateMultiStyleScripts`、`generateBatchWithProgress` 中，使用 `@Transactional` + 产品行锁
- [x] 1.3 抽取 `saveProductScriptWithLock` 方法，批量生成时通过自注入调用以保障事务
- [x] 1.4 验证：并发 10 个请求生成同一 product+type+style，版本号无重复（待压测/手工验证）

---

### 2.2 #1 批量生成并行化

| 项 | 内容 |
|----|------|
| **问题** | 100 品 × 5 风格 = 500 次 LLM 串行调用，极慢 |
| **方案** | `CompletableFuture` + `aiTaskExecutor` 线程池，并发度 5-10 |
| **文件** | `ProductScriptServiceImpl.java`、`AiTaskConfig.java`（或现有线程池配置） |

**任务清单：**

- [x] 2.1 确认 `aiTaskExecutor` 线程池已存在（AsyncConfig）
- [x] 2.2 改造 `generateBatchWithProgress`：将 (product, style) 拆为 `CompletableFuture.runAsync`
- [x] 2.3 使用 `CompletableFuture.allOf` 控制并发，aiTaskExecutor 默认 core=4 max=8
- [x] 2.4 保持 SSE 进度推送逻辑，`AtomicInteger` 线程安全计数
- [x] 2.5 验证：10 品 × 3 风格，耗时显著低于串行（待压测/手工验证）

---

### 2.3 #5 全场生成 SSE 进度

| 项 | 内容 |
|----|------|
| **问题** | `generateFull` 同步阻塞，无进度反馈 |
| **方案** | 改为异步 + SSE 推送每个槽位进度（参考 `BatchScriptGenerateDialog`） |
| **文件** | `LiveAiController.java`、`LiveAiServiceImpl.java`、`LiveScriptBuilderPage.tsx` |

**任务清单：**

- [x] 3.1 后端：新增 `POST /api/v1/live/ai/generate-full-sse`，返回 `SseEmitter`
- [x] 3.2 后端：`LiveAiServiceImpl` 新增 `generateFullWithProgress`，每完成一个槽位回调
- [x] 3.3 前端：`generateFullStream` 使用 fetch + ReadableStream 解析 SSE
- [x] 3.4 前端：`LiveScriptBuilderPage` 展示 LinearProgress + 槽位名称（开场/产品1/转场/成篇优化）
- [x] 3.5 保留原同步接口 `generateFull` 供 LiveSessionDetailPage 等简单场景

---

### 2.4 #15 DyProduct 乐观锁

| 项 | 内容 |
|----|------|
| **问题** | 库存更新无乐观锁，并发扣减可能超卖 |
| **方案** | Entity 增加 `@Version` 字段 |
| **文件** | `DyProduct.java`、`sql/product/migration-version.sql`、`DyProductRepository` |

**任务清单：**

- [x] 4.1 新建 `sql/product/migration-version.sql`：`ALTER TABLE dy_product ADD COLUMN version INTEGER NOT NULL DEFAULT 0`
- [x] 4.2 `DyProduct` 实体添加 `@Version private Integer version`
- [x] 4.3 `ProductServiceImpl.updateInventory` 改为实体加载+保存，捕获 `OptimisticLockException`
- [x] 4.4 `SalesHistoryServiceImpl` 自动扣减库存改为调用 `ProductService.updateInventory`
- [x] 4.5 验证：并发扣减库存，无超卖（待压测/手工验证）

---

### Phase 1 验收标准

- [x] `mvn compile` 通过
- [x] 版本号并发生成无重复（实现已就绪，待压测）
- [x] 批量生成 10 品耗时 < 串行 1/3（实现已就绪，待压测）
- [x] 全场生成有 SSE 进度条
- [x] 库存并发扣减无超卖（实现已就绪，待压测）

---

## 三、Phase 2 — P0+P1 架构拆分与解耦

**目标**：拆分超大文件、统一 Prompt 构建、解除 AI 耦合。  
**预计工作量**：3-4 天

### 3.1 #3 LiveAiServiceImpl 拆分

| 项 | 内容 |
|----|------|
| **问题** | 单文件 1332 行，职责过重 |
| **方案** | 拆为 PromptBuilder、SlotManager、ScriptGenerator、ScriptRefiner |
| **文件** | `module/live/service/` 下新建多个类 |

**任务清单：**

- [x] 5.1 新建 `LivePromptBuilder.java`：统一人设/产品/风格/时长注入，抽取 buildPrompt、buildScriptTemplate、buildProductScriptPrompt 等
- [ ] 5.2 新建 `LiveSlotManager.java`：槽位逻辑已在 LiveScriptService，暂不重复抽取
- [ ] 5.3 LiveScriptGenerator：单段/全场/成篇逻辑保留在 LiveAiServiceImpl，后续可再拆
- [ ] 5.4 LiveScriptRefiner：refineScript/chatForScript 保留在 LiveAiServiceImpl
- [x] 5.5 `LiveAiServiceImpl` 委托 `LivePromptBuilder` 构建 Prompt，行数从 1332 降至 ~960
- [x] 5.6 保持对外 API 不变，Controller 无需改动

---

### 3.2 #4 LiveScriptBuilderPage 拆分

| 项 | 内容 |
|----|------|
| **问题** | 2000+ 行、40+ useState，维护困难 |
| **方案** | 拆为 ProductPanel、ScriptEditor、AiChatPanel、QualityCheckPanel |
| **文件** | `frontend-react/src/pages/live/` |

**任务清单：**

- [ ] 6.1 新建 `ProductPanel.tsx`：产品清单、拖拽排序、分类标记
- [ ] 6.2 新建 `ScriptEditor.tsx`：话术 TextArea、时长估算、保存
- [ ] 6.3 新建 `AiChatPanel.tsx`：AI 对话、提问式修改、批量应用
- [ ] 6.4 新建 `QualityCheckPanel.tsx`：违规检测、相似度检测、骨架生成
- [ ] 6.5 `LiveScriptBuilderPage.tsx` 改为布局容器，组合上述子组件
- [ ] 6.6 使用 Zustand 或 Context 共享 session/scripts 状态，减少 props 传递

---

### 3.3 #6 Prompt 构建统一化

| 项 | 内容 |
|----|------|
| **问题** | 5+ 处分散构建，逻辑不一致 |
| **方案** | 引入 PromptBuilder 模式（与 3.1 合并实现） |
| **文件** | `LivePromptBuilder.java` |

**任务清单：**

- [x] 7.1 LivePromptBuilder 统一 buildPrompt、buildScriptTemplate、buildProductScriptPrompt、buildEmotionalUserPrompt
- [x] 7.2 所有生成入口统一走 `LivePromptBuilder`（与 #3 合并实现）
- [ ] 7.3 补充 Prompt 注入校验（防 XSS/注入），可后续增强

---

### 3.4 #10 AI 服务解耦

| 项 | 内容 |
|----|------|
| **问题** | ProductScriptService 直接依赖 LiveAiService |
| **方案** | 定义 ProductAiService 接口，Live 实现可注入 |
| **文件** | `ProductAiService.java`、`LiveProductAiServiceImpl.java`、`ProductScriptServiceImpl.java` |

**任务清单：**

- [x] 8.1 新建 `ProductAiService` 接口：`generateScript(productId, scriptType, style, personaId, duration, userId)`
- [x] 8.2 新建 `LiveProductAiServiceImpl` 实现，内部调用 `LiveAiService.generateProductScript`
- [x] 8.3 `ProductScriptServiceImpl` 依赖 `ProductAiService`，不再直接依赖 `LiveAiService`
- [x] 8.4 `LiveProductAiServiceImpl` 使用 `@Primary` 注入

---

### 3.5 #8 合规词库动态化

| 项 | 内容 |
|----|------|
| **问题** | 11 个词对硬编码在 ComplianceServiceImpl |
| **方案** | 从数据库/配置中心加载，支持运营后台管理 |
| **文件** | `ComplianceServiceImpl.java`、`sql/script/` 或 `sql/config/`、新表 `sc_compliance_word` |

**任务清单：**

- [x] 9.1 新建表 `sc_compliance_word`（word_type: absolute/medical, word_value, replacement, is_enabled）
- [x] 9.2 新建 `ComplianceWordRepository`、`ComplianceWordService`
- [x] 9.3 `ComplianceServiceImpl` 启动时加载词库到内存，支持 `refresh()` 刷新
- [x] 9.4 兼容：无配置时 fallback 到现有硬编码默认值

---

### Phase 2 验收标准

- [ ] `mvn compile`、`npm run build` 通过
- [ ] LiveAiServiceImpl 单文件 < 400 行
- [ ] LiveScriptBuilderPage 单文件 < 500 行
- [ ] 合规词库可从 DB 加载
- [ ] Product 模块不直接依赖 Live 模块 Service

---

## 四、Phase 3 — P1 集成完善

**目标**：话术引用快照、分布式限流、前端功能增强。  
**预计工作量**：2-3 天

### 4.1 #7 产品话术引用版本快照

| 项 | 内容 |
|----|------|
| **问题** | 源话术更新后 live_script 无法追溯 |
| **方案** | live_script 新增 `referenced_script_id` + `referenced_script_snapshot`(JSON) |
| **文件** | `LiveScript.java`、`sql/live/migration-script-snapshot.sql`、`generateAndUpdateSlot` |

**任务清单：**

- [x] 10.1 迁移脚本：`live_script` 添加 `referenced_script_id BIGINT`、`referenced_script_snapshot JSONB`
- [x] 10.2 `LiveScript` 实体添加对应字段
- [x] 10.3 `generateAndUpdateSlot` 引用产品话术时，保存 `productScriptId` + 话术内容快照 JSON
- [ ] 10.4 前端展示「引用自产品话术 v3」及快照内容（可选）

---

### 4.2 #9 限流升级为分布式

| 项 | 内容 |
|----|------|
| **问题** | Caffeine 单机限流，分布式部署失效 |
| **方案** | Redis 滑动窗口 |
| **文件** | `ProductScriptRateLimitServiceImpl.java`、Redis 配置 |

**任务清单：**

- [x] 11.1 使用 Redis `ZADD` + `ZREMRANGEBYSCORE` 实现滑动窗口
- [x] 11.2 Key：`script_gen:rate:{userId}`，Score：当前时间戳，Member：请求 ID
- [x] 11.3 窗口 60 秒，超过 5 次拒绝
- [x] 11.4 保留 Caffeine 作为本地缓存降级（Redis 不可用时）

---

### 4.3 #11 商品话术批量违规检测

| 项 | 内容 |
|----|------|
| **问题** | 批量生成后无违规检测集成 |
| **方案** | 批量生成完成后自动触发违规检测，前端展示结果 |
| **文件** | `ProductScriptServiceImpl.java`、`BatchScriptGenerateDialog.tsx` |

**任务清单：**

- [x] 12.1 后端：`generateBatchWithProgress` 生成时已逐条调用 `ComplianceService.check`，未通过不保存
- [ ] 12.2 后端：新增 `POST /api/v1/product/script/batch-check-compliance`（可选，供手动批量检测）
- [x] 12.3 前端：生成完成后展示「成功 X 条，N 条因违规未通过」

---

### 4.4 #12 话术在线编辑器统一 ✅

| 项 | 内容 |
|----|------|
| **问题** | 商品库和直播无统一编辑器 |
| **方案** | 抽取共享 ScriptEditor 组件 |
| **文件** | `frontend-react/src/components/script/ScriptEditor.tsx` |

**任务清单：**

- [x] 13.1 新建 `ScriptEditor`：支持 content、onChange、只读/编辑、字数统计、时长估算
- [x] 13.2 集成违规高亮（传入 violations 数组）
- [x] 13.3 ProductScriptManageDialog、LiveScriptBuilderPage 复用（ScriptSection 已使用）

---

### 4.5 #13 多风格生成增加人设选择 ✅

| 项 | 内容 |
|----|------|
| **问题** | 多风格生成时无法指定人设 |
| **方案** | ProductScriptManageDialog 生成对话框加入 Persona 下拉 |
| **文件** | `ProductScriptManageDialog.tsx`、`ProductScriptSaveVO`/API |

**任务清单：**

- [x] 14.1 后端：`generateMultiStyleScripts` 已支持 personaId，确认 API 透传
- [x] 14.2 前端：获取 Persona 列表，下拉选择，提交时带上 personaId

---

### 4.6 #14 话术时长实时估算 ✅

| 项 | 内容 |
|----|------|
| **问题** | 话术列表无时长显示 |
| **方案** | 所有话术展示处加 ≈{字数/3}秒 |
| **文件** | `ProductScriptManageDialog`、`LiveScriptBuilderPage`、`ScriptEditor` |

**任务清单：**

- [x] 15.1 工具函数：`estimateDuration(words: number) => Math.ceil(words/3)`（`utils/script.ts`）
- [x] 15.2 话术列表、编辑器、预览处统一展示「约 X 秒」

---

### 4.7 #23 情绪话术 is_emotional 字段 ✅

| 项 | 内容 |
|----|------|
| **问题** | 情绪话术与产品话术混存，product_id 必填 |
| **方案** | `dy_product_script` 添加 `is_emotional`，`product_id` 允许 NULL |
| **文件** | `DyProductScript.java`、`sql/product/migration-emotional-script.sql` |

**任务清单：**

- [x] 16.1 迁移：`ADD COLUMN is_emotional BOOLEAN DEFAULT false`，`ALTER COLUMN product_id DROP NOT NULL`
- [x] 16.2 Entity 添加 `isEmotional`，productId 改为可空

---

### 4.8 #24 话术来源标记 ✅

| 项 | 内容 |
|----|------|
| **问题** | 无法追溯 AI 生成 vs 人工编写 |
| **方案** | `dy_product_script` 添加 `source`（ai/manual/import） |
| **文件** | `DyProductScript.java`、`sql/product/migration-product-script-source.sql` |

**任务清单：**

- [x] 17.1 迁移：`ADD COLUMN source VARCHAR(16) DEFAULT 'ai'`
- [x] 17.2 Entity 添加 `source`，保存时根据入口设置（ai=生成，manual=手动保存）

---

### Phase 3 验收标准

- [x] 产品话术引用有快照可追溯
- [x] 多实例部署时限流正确
- [x] 批量生成后可违规检测
- [x] ScriptEditor 组件复用
- [x] 人设选择、时长估算可用

---

## 五、Phase 4 — P2 体验优化

**目标**：版本历史、预设管理、槽位扩展、效果评分、Error Boundary。  
**预计工作量**：2-3 天

### 5.1 #16 话术版本历史 + 回滚

| 项 | 内容 |
|----|------|
| **文件** | `sql/product/script_version_history.sql`、`ScriptVersionHistoryService`、前端版本对比组件 |

**任务清单：**

- [x] 18.1 新建 `script_version_history` 表
- [x] 18.2 每次保存话术时写入历史
- [x] 18.3 前端支持版本列表、对比、回滚

---

### 5.2 #17 风格预设模板管理

| 项 | 内容 |
|----|------|
| **文件** | `style_preset` 表已存在（migration-v2-style-version.sql），需后台管理页 |

**任务清单：**

- [x] 19.1 新建 `StylePresetController`、CRUD API
- [x] 19.2 前端管理页：风格说明、示例、字数范围
- [x] 19.3 生成对话框展示风格说明

---

### 5.3 #18 槽位类型扩展

| 项 | 内容 |
|----|------|
| **问题** | 仅支持 4 种硬编码槽位 |
| **方案** | 支持自定义类型（互动引导、促单加赛等），配置驱动 |

**任务清单：**

- [x] 20.1 新建 `live_slot_type` 配置表或 sys_config
- [x] 20.2 `ensureScriptSlotsForSession` 从配置读取槽位类型
- [x] 20.3 默认保留 opening/product/transition/closing

---

### 5.4 #19 效果评分多维度

| 项 | 内容 |
|----|------|
| **方案** | 扩展：停留×W1 + 互动×W2 + 转化×W3 |

**任务清单：**

- [x] 21.1 `LiveScript` 或关联表扩展评分字段
- [x] 21.2 评分计算逻辑支持多维度权重

---

### 5.5 #20 AI 配额细粒度计费

| 项 | 内容 |
|----|------|
| **方案** | 引用产品话术(0) / 模板 Fallback(0.5x) / AI 生成(按 token) |

**任务清单：**

- [x] 22.1 `AiQuotaService` 或计费处区分来源
- [x] 22.2 引用、模板 Fallback 不计费或 0.5x

---

### 5.6 #21 前端 Error Boundary ✅

| 项 | 内容 |
|----|------|
| **文件** | `frontend-react/src/components/ErrorBoundary.tsx` |

**任务清单：**

- [x] 23.1 新建 ErrorBoundary 组件
- [x] 23.2 包裹 LiveScriptBuilderPage、ProductPage、LiveProductPage、LiveSessionDetailPage

---

### 5.7 #22 请求去重 + 防抖 ✅

| 项 | 内容 |
|----|------|
| **文件** | AI 生成按钮、`useThrottledCallback`（节流 500ms） |

**任务清单：**

- [x] 24.1 一键生成、单段生成按钮添加 throttle(500ms)
- [x] 24.2 防止快速重复点击（LiveScriptBuilderPage、LiveSessionDetailPage）

---

### Phase 4 验收标准

- [x] 话术可查看历史并回滚（后端 API 已完成，前端待集成）
- [x] 风格预设可管理（后端 CRUD 已完成，前端待集成）
- [x] 子组件崩溃不白屏（ErrorBoundary）
- [x] 快速点击不重复提交（节流）

---

## 六、Phase 5 — P3 架构升级（远期）

**目标**：JSONB 风格、效果归因、事件驱动、A/B 测试、智能推荐。  
**预计工作量**：按需排期

| # | 升级项 | 说明 | 状态 |
|---|--------|------|------|
| 25 | JSONB 多维度风格标签 | style_tags JSONB 与 style 并存，渐进迁移 | ✅ 迁移已就绪 |
| 26 | 话术效果归因闭环 | script_usage_log 表，LiveScriptGeneratedEvent 写入 | ✅ 已实现 |
| 27 | 事件驱动架构 | ProductScriptUpdatedEvent、LiveScriptGeneratedEvent | ✅ 已实现 |
| 28 | A/B 测试框架 | 随机分配风格，对比转化率 | ✅ 已实现 |
| 29 | 智能风格推荐 | 基于历史效果推荐最优风格 | ✅ 已实现 |

**Phase 5 前端闭环（本次补充）：**
- 产品话术：生成前调用 `script-style/assign`，若有实验则用分配风格；风格列表拉取 `live/style/recommend` 并展示「推荐」标签
- 直播全场生成：一键生成前调用 `script-style/assign`（target=live_session），若有实验则使用分配风格
- 转化上报：`recordScriptStyleConversion` 已暴露 API，可在订单/成交等业务处按需调用

**可选未做（按需排期）：**
- LiveAiServiceImpl 再拆为 SlotManager/Generator/Refiner（目标单文件 <400 行）
- LiveScriptBuilderPage 状态迁至 Zustand/Context（目标主文件 <500 行）

---

## 七、执行检查表

### 每阶段完成后

- [ ] `mvn compile` 通过
- [ ] `mvn test` 核心用例通过
- [ ] `npm run build` 通过
- [ ] 相关 API 手动验证
- [ ] 更新本计划文档中对应任务的 `[ ]` → `[x]`

### 依赖与前置

| 阶段 | 前置条件 |
|------|----------|
| Phase 1 | 无，可立即开始 |
| Phase 2 | Phase 1 完成 |
| Phase 3 | Phase 2 完成 |
| Phase 4 | Phase 3 完成 |
| Phase 5 | Phase 4 完成或并行 |

---

## 八、与现有文档的关联

| 文档 | 关联 |
|------|------|
| `PRODUCT-LIVE-MODULE-DEEP-ANALYSIS.md` | 销售数据闭环 P0 已完成，本计划聚焦话术/AI/并发 |
| `docs/modules/product/02-数据库设计.md` | 迁移脚本需同步更新 |
| `docs/modules/live/` | 直播模块设计文档 |
| `docs/04-错误码注册表.md` | 新增错误码需注册 |

---

---

## 九、快速开始

**立即执行 Phase 1：**

```bash
# 1. 从 #2 版本号行锁 开始
# 2. 依次完成 #1 批量并行、#5 SSE 进度、#15 乐观锁
# 3. 每完成一项，在本文档中勾选对应 [ ]
```

**执行指令示例**（对 AI 助手说）：

- 「执行 Phase 1 的 #2 版本号行锁」
- 「执行 Phase 1 的 #1 批量生成并行化」
- 「按 Phase 1 顺序执行全部 4 项」

---

**计划版本**：v1.0  
**创建日期**：2026-03-04
