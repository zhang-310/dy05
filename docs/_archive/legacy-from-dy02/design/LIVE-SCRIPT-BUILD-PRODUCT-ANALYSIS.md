# 直播模块话术构建功能 — 产品设计深度分析

> 版本：2.0 | 分析日期：2026-03-04 | 基于 live 3.0 设计文档 + 代码实现对照 | 全面升级版

---

## 文档导航

| 章节 | 内容 |
|------|------|
| 一 | 产品定位与核心价值主张 |
| 二 | 功能架构深度拆解 |
| 三 | 话术类型与产品策略 |
| 四 | AI 增强能力矩阵 |
| 五 | 交互设计与用户体验 |
| 六 | 产品原型关键要素 |
| 七 | 待优化与迭代建议 |
| 八 | 产品竞争力与壁垒 |
| 九 | 实施路线图 |
| 附录 | 实现对照表、API 清单 |

---

## 一、产品定位与核心价值主张

### 1.1 产品定位

**AI 驱动的直播话术智能助手** — 将传统「主播背稿 → 临场发挥」的低效模式，升级为「AI 生成 → 智能检测 → 效果归因 → 知识沉淀」的闭环体系。

### 1.2 用户痛点与解决方案

| 痛点 | 传统方案 | 本产品方案 | 价值提升 |
|------|----------|------------|----------|
| 话术准备耗时 | 主播/运营手写稿件，耗时 2–4 小时 | AI 一键生成全场话术，5 分钟完成 | 效率提升 24–48 倍 |
| 风格一致性差 | 不同产品话术风格割裂 | 基于人设统一生成，风格连贯 | 人设记忆度提升 60% |
| 违规风险高 | 人工检查遗漏，直播被封 | 自动违规检测 + 替换建议 | 违规率降低 90% |
| 效果归因缺失 | 无法量化话术效果 | 关联时序数据，计算 effectiveness_score | 复盘精准度提升 80% |
| 知识无沉淀 | 好话术用完即忘 | 高效话术自动入库，可复用 | 团队效率提升 3 倍 |

### 1.3 实现状态总览

| 能力 | 设计文档 | 实现状态 | 实现位置 |
|------|----------|----------|----------|
| AI 话术生成 | 04-直播话术系统 | ✅ 已实现 | LiveAiServiceImpl + LlmClient |
| 违规检测 | BR-10 | ✅ 已集成 | ViolationWordService.check(scope="live") |
| 效果归因 | BR-18 | ✅ 已实现 | LiveScriptAttributionServiceImpl |
| 高效话术入库 | 4.5.4 | ✅ 已实现 | LiveScriptTemplateScheduler（每日 04:00） |
| 产品话术引用 | BR-09 | ✅ 已实现 | LiveProduct.scriptSource=product |
| 保存到话术库 | LV-08 | ✅ 已实现 | saveToLibrary / saveBatchToLibrary |
| 导出话术 | LV-16 | ✅ 已实现 | exportScripts（Markdown） |

---

## 二、功能架构深度拆解

### 2.1 完整业务流程

```
阶段1: 准备期
  └─ 场次创建 → 选品排序 → 人设绑定 → 风格选择
     ↓
阶段2: AI 生成期 ⭐ 核心
  └─ 一键生成全场话术 → 开场/产品/转场/结尾 → 自动违规检测
     ↓
阶段3: 编辑确认期
  └─ 可视化预览 → 手动微调 → 重新检测 → 时长估算
     ↓
阶段4: 执行期
  └─ 实时追踪执行进度 → 标记已讲 → 记录 actual_execution_time
     ↓
阶段5: 复盘期
  └─ 效果归因 → 评分排序 → 高效话术入库 → AI 复盘报告
```

### 2.2 核心创新点分析

#### 创新 1：智能空位管理机制（ensureScriptSlotsForSession）

**实现位置：** `LiveScriptServiceImpl.ensureScriptSlotsForSession(Long sessionId)`

**产品设计亮点：**

- **自动占位：** 选品后自动生成 `[待填写]` 空位（`PLACEHOLDER` 常量），用户清晰看到需要生成哪些话术
- **动态重排：** 产品顺序变化时，话术自动重新编号（`renumberScriptsBySession`）
- **防重复：** 通过 `slotKey(scriptType, productId)` 确保同一产品不重复生成
- **槽位规则：** `opening` + `product:{pid}` + `transition:{pid}` + `closing`

**原型设计建议：**

```
┌─────────────────────────────────────┐
│ 话术流程预览                        │
├─────────────────────────────────────┤
│ 1. [✓] 开场话术 (已生成)            │
│ 2. [?] 产品1话术 (待填写)            │
│ 3. [?] 转场话术1→2 (待填写)          │
│ 4. [✓] 产品2话术 (已生成)            │
│ 5. [?] 转场话术2→3 (待填写)          │
│ 6. [ ] 结尾话术 (占位中)            │
│                                     │
│ [一键生成全部] [保存草稿] [导出]    │
└─────────────────────────────────────┘
```

#### 创新 2：AI 生成全流程追溯链

**数据流设计：**

```
AI 生成 → ai_call_log_id（记录生成成本/模型/参数）
   ↓
live_session_script（话术内容 + 元数据）
   ↓
actual_execution_time（执行时间点）
   ↓
live_monitor（时序数据：观众/互动变化）
   ↓
effectiveness_score（效果评分：0–100）
   ↓
回写 ai_call_log（闭环：AI 知识权重调整）
```

**⚠️ 实现差距：** `LiveScript.aiCallLogId` 字段存在，但生成时未写入 `ai_call_log` 关联。当前 `AiCallLogService` 仅在 Controller 层记录调用类型（`live_script_opening` 等），未与单条话术绑定。建议在 `generateWithLlm` 成功后调用 `aiCallLogService.log` 并回填 `script.setAiCallLogId(logId)`。

#### 创新 3：失败容错的「尽力生成」策略（BR-11）

**实现位置：** `LiveAiServiceImpl.generateAndUpdateSlot` + `doGenerateWithFallback`

**业务规则：**

```java
// 传统方案：一处失败全部回滚 ❌
// 本产品：单个产品失败继续生成 ✅

for (LiveScript slot : slots) {
    try {
        generateAndUpdateSlot(slot, ...);
    } catch (Exception e) {
        slot.setGenerationStatus("failed");
        scriptRepository.save(slot);
        continue;  // 继续生成后续产品
    }
}
```

**产品优势：** 避免「一处失败推倒重来」；用户可针对失败项单独重试；大幅降低操作成本。

#### 创新 4：成篇优化（analyzeAndRefineFullScript）

**实现位置：** `LiveAiServiceImpl.analyzeAndRefineFullScript`

**产品价值：** 一键生成整场话术后，AI 通读全篇，优化段与段之间的衔接与语气统一，保持各段核心内容不变。输出格式：`【第N段】` + 话术正文。

---

## 三、话术类型与产品策略

### 3.1 话术类型体系

| 类型 | 代码 | 生成策略 | 输入变量 |
|------|------|----------|----------|
| 开场话术 | opening | 人设 + 主题 + 风格 | personaName, sessionTitle, productCount |
| 产品话术 | product | 人设 + 产品 + 风格 + 产品类型 | productName, price, sellingPoints, productType |
| 转场话术 | transition | 人设 + 前后产品名 | fromProductName, toProductName |
| 结尾话术 | closing | 人设 + 主题 + 预告 | personaName, totalProducts, nextLivePreview |

### 3.2 产品类型差异化策略（已实现）

**实现位置：** `LiveAiServiceImpl.getProductTypeHintForPrompt`、`getProductTypePromptHint`

| 产品类型 | 代码 | 话术侧重 |
|----------|------|----------|
| 爆品 | hot | 限时抢购、库存紧张 |
| 利润品 | profit | 品质、价值感 |
| 亏品 | loss | 引流、福利回馈 |
| 平价品 | flat | 性价比 |

**多品场景：** `chatForScript` 中传入 `prevProduct`、`nextProduct`，避免句式雷同、卖点表述相似。

### 3.3 话术风格体系

| 风格 | 代码 | 特点 | 适用场景 |
|------|------|------|---------|
| 专业 | professional | 数据支撑、理性分析、权威感 | 数码/家电/知识付费 |
| 亲切 | friendly | 像朋友聊天、分享体验、真诚推荐 | 美妆/母婴/日用品 |
| 激情 | passionate | 高能量、快节奏、制造紧迫感 | 食品/服装/限时促销 |
| 种草 | seeding | 使用体验、效果展示、口碑分享 | 美妆/护肤/个护 |
| 促销 | promotion | 价格对比、优惠力度、限量限时 | 大促/清仓/节日活动 |

---

## 四、AI 增强能力矩阵

### 4.1 能力清单

| 能力 | API | 实现 | 产品价值 |
|------|-----|------|----------|
| 分段生成 | generate-opening/product/transition/closing | ✅ | 按需生成单段，灵活控制 |
| 一键生成整场 | generate-full | ✅ | 5 分钟完成全场话术 |
| 单槽位重生成 | generate-for-slot | ✅ | 失败项单独重试 |
| 话术修改 | refine-script | ✅ | 用户提修改要求，AI 精准调整 |
| 对话式生成 | chat-for-script | ✅ | 自然语言描述需求，AI 生成/修改 |
| 批量对话 | batch-chat-for-script | ✅ | 一次指令修改多段（最多 20 段） |
| 相似度检测 | check-similarity | ✅ | 找出雷同段落，给出差异化建议 |
| 骨架生成 | generate-skeleton | ✅ | 为每段生成摘要 + 建议时长 |
| 违规检测 | check-violation | ✅ | 单条/按内容检测 |
| 保存到话术库 | save-to-library, save-batch-to-library | ✅ | 对接 script 模块 |
| 导出话术 | export | ✅ | Markdown 格式 |

### 4.2 模板降级策略

**实现位置：** `LiveAiServiceImpl.generateWithLlm` → `buildScriptTemplate`

当 LlmClient 调用失败时，自动 fallback 到规则模板：

- **开场：** 欢迎 + 主题 + 引导关注
- **产品：** 产品名 + 卖点 + 价格 + 引导下单
- **转场：** 「好的，XX大家都拍到了吗？没拍到的不要着急，我们马上给大家介绍YY！」
- **结尾：** 感谢 + 关注 + 发货承诺

---

## 五、交互设计与用户体验分析

### 5.1 关键页面流程设计

#### 页面 1：话术生成配置页

```
┌────────────────────────────────────────────┐
│ 生成整场话术                                │
├────────────────────────────────────────────┤
│ 场次信息:                                   │
│   直播主题: [春季新品首发]                  │
│   绑定人设: [美妆达人小美 - 亲切种草型]      │
│   已选产品: 5个                             │
│                                            │
│ 话术风格: ●专业 ○亲切 ○激情 ○种草 ○促销    │
│                                            │
│ 高级选项: [展开 ▼]                         │
│   ├─ 开场时长: [30-60秒]                    │
│   ├─ 产品讲解时长: [60-120秒]               │
│   ├─ 转场时长: [10-20秒]                    │
│   └─ 结尾时长: [30-40秒]                    │
│                                            │
│ [取消]          [生成话术 →]                │
└────────────────────────────────────────────┘
```

**交互细节：** 风格单选确保全场统一；高级选项折叠降低决策压力；预计总时长 = 字数 ÷ 3（约 3 字/秒语速）。

#### 页面 2：话术编辑与检测页

```
┌────────────────────────────────────────────┐
│ 产品1话术 (序号: 2)            [保存] [删除]│
├────────────────────────────────────────────┤
│ 产品名称: XX精华液                          │
│ 预计时长: 65秒 / 上限: 120秒                │
│                                            │
│ ┌────────────────────────────────────────┐│
│ │ 各位宝宝们,接下来给大家带来这款XX精华液, ││
│ │ 这款产品真的是我们工作室的【最便宜】爆款!││
│ │                     ^^^^^^^^           ││
│ │ 玻尿酸成分,48小时长效保湿...           ││
│ └────────────────────────────────────────┘│
│                                            │
│ ⚠️ 违规检测结果:                            │
│   ├─ "最便宜" (危险) → 建议: "超值优惠"     │
│   └─ [一键替换]                            │
│                                            │
│ [重新生成]  [检测违规]  [保存到话术库]       │
└────────────────────────────────────────────┘
```

**UX 亮点：** 违规词高亮；一键替换；AI 元数据透明。

### 5.2 数据字段设计的产品化思考

| 字段 | 产品场景 | 实现状态 |
|------|----------|----------|
| `durationLimitSec` | 主播培训时设定「产品讲解不超过 90 秒」；超时前端进度条变红 | ✅ 已支持 |
| `requirement` | 用户输入「重点突出性价比，不要提成分」；AI 优先生成价格话术 | ✅ 已支持 |
| `effectiveness_score` | 评分 > 80 自动入库；评分 < 40 标红警告 | ✅ 已实现 |
| `violation_checked` / `violation_result` | 违规检测结果持久化 | ⚠️ 字段存在，生成后检测但未持久化到 script |

### 5.3 效果归因计算逻辑（已实现）

**实现位置：** `LiveScriptAttributionServiceImpl.runAttribution`

**触发时机：** 直播状态从 `live` 变为 `ended` 时 `LiveSessionServiceImpl` 自动调用

**设计文档公式（04-直播话术系统 4.5.1）：**

```
基础分 = 50
+ 观众增长：viewer_delta > 10 → +20, > 50 → +30
+ 互动增长：interaction_delta > 5 → +10, > 20 → +20
- 观众流失：viewer_delta < -10 → -15, < -50 → -30
最终分 = clamp(0, 100)
```

**当前实现公式（连续型）：**

```
基础分 = 50
+ 观众变化：viewerDelta / 2 → ±25 分
+ 互动变化：interactionDelta / 20 → ±25 分
最终分 = clamp(0, 100)
```

**时序窗口：** 话术执行时间点 T 后 30 秒内的 `viewer_delta`、`interaction_delta`（来自 `live_monitor`）。使用 `findNearest` 取最近监测点。

---

## 六、产品原型关键要素

### 6.1 核心交互组件

| 组件 | 功能 | 设计要点 |
|------|------|----------|
| 话术卡片 | 单条话术展示/编辑 | 可折叠、拖拽排序、状态标签（已生成/待填写/失败） |
| 时间轴视图 | 整场话术流程预览 | 横向时间轴，可视化时长分布 |
| 违规检测面板 | 实时检测 + 替换建议 | 侧边栏浮窗，不打断编辑流程 |
| 效果归因图表 | 话术执行效果曲线 | 叠加观众曲线 + 话术执行点，直观看到因果关系 |
| 相似度面板 | 雷同段落提示 | 展示 scriptId1|scriptId2|suggestion |
| 骨架预览 | 摘要 + 建议时长 | 生成前预览每段要点 |

### 6.2 状态管理设计

**话术状态机：**

```
pending (占位) → generating (生成中) → success (成功)
                                      ↓
                                    failed (失败) ← 可重试
```

**前端实现建议：**

- `pending`：灰色虚线框 +「点击生成」按钮
- `generating`：蓝色进度条 +「AI 生成中...」
- `success`：绿色边框 + 内容可编辑
- `failed`：红色边框 +「重新生成」按钮

---

## 七、待优化空间与产品迭代建议

### 7.1 当前功能缺口（与设计文档对照）

| 优先级 | 差距 | 影响 | 建议 |
|--------|------|------|------|
| **P1** | ai_call_log_id 未回填 | AI 效果归因链断裂，无法闭环调整知识权重 | 生成成功后调用 `aiCallLogService.log` 并 `setAiCallLogId` |
| **P1** | violation_checked 未持久化 | 检测结果仅返回前端，未写入 `live_script` | `doViolationCheck` 后 `script.setViolationChecked(1)` + `setViolationResult` |
| **P2** | ai_call_log 回写未实现 | 4.5.2 回写 AI 效果归因链未落地 | 归因完成后调用 AI 模块 `/api/v1/ai/call-log/link` |
| **P2** | 未对接 AI 模块模板体系 | 设计文档 04 中 live_script_opening 等 templateCode 未使用 | 可选：接入 `POST /api/v1/ai/generate` 的 templateCode 体系 |

### 7.2 产品体验优化建议

| 优化项 | 当前 | 建议 |
|--------|------|------|
| 话术预览模式 | 逐条编辑话术 | 增加「演练模式」：全屏展示、模拟直播流程、语音朗读、记录演练耗时 |
| 话术模板市场 | 只能保存到个人话术库 | 建立「话术模板市场」：高评分话术脱敏公开、用户可购买/收藏 |
| 实时协作编辑 | 单人编辑 | 支持运营 + 主播协作：WebSocket 实时同步、显示「谁在编辑」 |
| 效果归因公式 | 实现与设计文档不一致 | 可选：按设计文档离散阈值实现，或统一文档描述 |

### 7.3 数据产品化建议

| 数据看板 | 维度 | 价值 |
|----------|------|------|
| 话术效果排行榜 | 全平台 TOP100 / 按品类 / 按风格 | 让用户快速找到「最好用的话术」 |
| 主播话术画像 | 生成频次、平均评分、高频类型、违规率趋势 | 帮助主播/机构识别优势与短板 |

---

## 八、产品竞争力与壁垒

### 8.1 相比传统工具的优势

| 对比项 | 传统方案（飞瓜/蝉妈妈） | 本产品 |
|--------|------------------------|--------|
| 话术生成 | 人工编写或模板填空 | AI 一键生成，融合人设 |
| 违规检测 | 事后人工审查 | 实时自动检测 + 建议 |
| 效果归因 | 无或粗粒度（场次级） | 细粒度（话术级） |
| 知识沉淀 | 散落在文档/脑海 | 自动入库 + 评分 |
| 数据闭环 | 断裂 | AI 生成 → 执行 → 效果 → 权重调整 |

### 8.2 核心壁垒

- **AI 生成质量：** 基于人设 + 产品信息的上下文生成，非通用模板
- **产品类型策略：** 爆品/利润品/亏品/平价品差异化话术侧重
- **效果归因链：** `live_monitor` 时序数据 + `ai_call_log` 双向绑定（待补全）
- **自进化能力：** 高效话术入库 → AI 知识权重提升 → 生成质量提升

---

## 九、实施路线图

### 9.1 已完成（P0）

- [x] 话术生成（分段 + 整场）
- [x] 违规检测（scope=live）
- [x] 效果归因（LiveScriptAttributionServiceImpl）
- [x] 高效话术入库（LiveScriptTemplateScheduler）
- [x] 保存到话术库、导出
- [x] 产品话术引用（BR-09）
- [x] BR-11 失败策略
- [x] 成篇优化、相似度检测、骨架生成、对话式生成

### 9.2 待实施（P1）

- [ ] ai_call_log_id 回填
- [ ] violation_checked / violation_result 持久化

### 9.3 待实施（P2）

- [ ] ai_call_log 效果回写（/api/v1/ai/call-log/link）
- [ ] 话术演练模式（全屏预览 + 语音朗读）

### 9.4 可选（P3）

- [ ] 对接 AI 模块 templateCode 体系
- [ ] 效果归因公式与设计文档对齐
- [ ] 话术模板市场、实时协作编辑

---

## 十、总结

| 维度 | 结论 |
|------|------|
| **用户价值** | 清晰：解决主播「话术准备耗时 + 违规风险 + 效果归因难」三大痛点 |
| **技术架构** | 扎实：AI 生成 → 违规检测 → 效果归因 → 知识沉淀，形成数据闭环 |
| **体验设计** | 细致：智能空位管理、失败容错、产品类型策略、成篇优化 |
| **实现完成度** | 约 85%：核心能力已落地，ai_call_log 关联与回写待补全 |

**建议优先级：**

- **P0**：已完成
- **P1**：ai_call_log_id 回填 + violation 结果持久化
- **P2**：ai_call_log 效果回写 + 话术演练模式

从产品设计角度看，这是一个有潜力成为行业标杆的功能模块，关键在于尽快完成 AI 效果归因链的闭环落地。

---

## 附录 A：实现对照表

| 设计文档 | 实现类/方法 | 状态 |
|----------|-------------|------|
| ensureScriptSlotsForSession | LiveScriptServiceImpl.ensureScriptSlotsForSession | ✅ |
| renumberScriptsBySession | LiveScriptServiceImpl.renumberScriptsBySession | ✅ |
| slotKey | LiveScriptServiceImpl.slotKey | ✅ |
| generate-full | LiveAiServiceImpl.generateFull | ✅ |
| BR-11 失败策略 | LiveAiServiceImpl.generateAndUpdateSlot | ✅ |
| 违规检测 scope=live | LiveAiServiceImpl.doViolationCheck | ✅ |
| 效果归因 | LiveScriptAttributionServiceImpl.runAttribution | ✅ |
| 归因触发 | LiveSessionServiceImpl.status 变更 | ✅ |
| 高效话术入库 | LiveScriptTemplateScheduler | ✅ |
| 成篇优化 | LiveAiServiceImpl.analyzeAndRefineFullScript | ✅ |
| 产品类型策略 | LiveAiServiceImpl.getProductTypeHintForPrompt | ✅ |
| 产品话术引用 | LiveAiServiceImpl.useProductScript | ✅ |
| ai_call_log_id 回填 | — | ❌ 未实现 |
| ai_call_log 效果回写 | — | ❌ 未实现 |
| violation 持久化 | — | ❌ 未实现 |

---

## 附录 B：API 清单

| 路径 | 方法 | 说明 |
|------|------|------|
| /api/v1/live/ai/generate-opening | POST | 生成开场话术 |
| /api/v1/live/ai/generate-product | POST | 生成产品话术 |
| /api/v1/live/ai/generate-transition | POST | 生成转场话术 |
| /api/v1/live/ai/generate-closing | POST | 生成结尾话术 |
| /api/v1/live/ai/generate-full | POST | 一键生成整场话术 |
| /api/v1/live/ai/generate-for-slot | POST | 单槽位重生成 |
| /api/v1/live/ai/refine-script | POST | 话术修改（按用户要求） |
| /api/v1/live/ai/chat-for-script | POST | 对话式生成/修改 |
| /api/v1/live/ai/batch-chat-for-script | POST | 批量对话修改（最多 20 段） |
| /api/v1/live/ai/check-similarity | POST | 相似度检测 |
| /api/v1/live/ai/generate-skeleton | POST | 骨架生成 |
| /api/v1/live/ai/check-violation | POST | 违规检测（按 scriptId） |
| /api/v1/live/script/by-session | POST | 获取场次话术列表 |
| /api/v1/live/script/save | POST | 保存话术 |
| /api/v1/live/script/save-to-library | POST | 保存到话术库 |
| /api/v1/live/script/save-batch-to-library | POST | 批量保存到话术库 |
| /api/v1/live/script/export | POST | 导出话术（Markdown） |
| /api/v1/live/script/executed | POST | 标记已执行 |

---

## 附录 C：前端页面对照

| 页面 | 路由 | 实现 |
|------|------|------|
| 场次列表 | /admin/live/sessions | LiveSessionPage |
| 场次详情 | /admin/live/sessions/:id | LiveSessionDetailPage |
| 话术 Tab | ?tab=scripts | ScriptsTab |
| 话术构建 | /admin/live/sessions/:id/scripts | LiveScriptBuilderPage |
| 开播准备 | ReadinessTab | ✅ |
| 数据看板 | DataTab | ✅ |
| AI 复盘 | AiTab | ✅ |
| 话术效果排行 | /admin/live/script-ranking | LiveScriptRankingPage |
