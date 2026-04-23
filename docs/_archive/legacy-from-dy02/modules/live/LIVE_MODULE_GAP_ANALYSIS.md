# 直播模块 — 文档与实现差距分析

> 分析日期：2026-03-03 | 更新日期：2026-03-03（P0-P3 全面升级完成） | 对照文档版本：3.0

---

## 一、总体结论

直播模块**核心能力已基本实现**。场次详情、开播准备、话术编辑、数据看板、AI 复盘、历史对比、话术排行、机构端、效果归因、高效话术入库等均已落地。文档设计的 30 个 API 中约 **85%** 已实现（路径略有差异）。

**核心产品价值「为主播构建直播话术」** 已完成 P0-P2 全面升级：产品话术写入 product_id、转场话术传入 from/to 产品名、保存到话术库、导出话术、话术风格透传、BR-11 失败策略、产品话术引用、预计时长均已落地。

---

## 二、「为主播构建直播话术」核心场景深度分析

### 2.1 场景定位

根据需求文档 01、话术系统 04，**为主播构建直播话术** 是直播模块的核心 AI 能力，覆盖从选品到话术落地的完整链路：

```
主播创建场次 → 选品排序 → 一键/分段生成话术（开场/产品/转场/结尾）→ 违规检测 → 编辑确认 → 直播执行追踪 → 效果归因 → 高效话术入库
```

用户故事 LV-04：*我要能一键生成整场直播话术 | 按人设+风格，AI 生成开场→各产品→转场→结尾完整话术*

### 2.2 设计 vs 实现对照

| 环节 | 文档设计（04-直播话术系统） | 当前实现 | 差距 |
|------|---------------------------|----------|------|
| **输入** | 人设 + 产品 + 场次主题 + 话术风格 | 人设 + 产品 + 场次 + 风格 | ✅ 前端风格选择 + session.scriptStyle 透传 |
| **开场话术** | live_script_opening 模板 | 通用 prompt，含人设/主题 | ⚠️ 未用 AI 模块 templateCode（可选增强） |
| **产品话术** | live_script_product，按产品卖点/价格 | 按产品生成，含描述/价格 | ✅ product_id 已写入 |
| **转场话术** | fromProductName → toProductName | 传入 from/to 产品名 | ✅ 已实现 |
| **结尾话术** | live_script_closing | 含人设/主题 | ✅ |
| **整场流程** | 开场 → 产品1 → 转场1→2 → 产品2 → ... → 结尾 | ✅ 流程正确 | — |
| **失败策略 BR-11** | 单产品失败不回滚，标记 failed | doGenerateWithFallback | ✅ 已实现 |
| **违规检测** | 生成后自动检测，scope=live | ✅ 自动检测 | — |
| **编辑/确认** | 可编辑，编辑后需重新检测 | ✅ 支持 | — |
| **保存到话术库** | LV-08：好的话术可收藏到 script 模块 | save-to-library、save-batch-to-library | ✅ 已实现 |
| **导出话术** | 文档 06 话术编辑器有 [导出话术] | export 接口 + 前端下载 | ✅ 已实现 |
| **预计时长** | 每条话术预计时长，整场预计时长 | estimatedDurationSeconds，字数/3 | ✅ 已实现 |

### 2.3 关键差距清单（为主播构建话术）

| 优先级 | 差距 | 影响 | 建议 |
|--------|------|------|------|
| **P0** | 产品话术写入 product_id | ✅ 已实现 | LiveAiServiceImpl.doGenerate 产品话术时 `script.setProductId(vo.getProductId())` |
| **P0** | 转场话术 from/to 产品名 | ✅ 已实现 | generateFull 中 transition 传入 fromProductName/toProductName |
| **P1** | 保存到话术库 | ✅ 已实现 | POST /live/script/save-to-library、save-batch-to-library，对接 script 模块 |
| **P1** | 导出话术 | ✅ 已实现 | POST /live/script/export，Markdown 格式 |
| **P1** | 话术风格透传 | ✅ 已实现 | 前端风格选择器 + session.scriptStyle 透传 |
| **P2** | BR-11 失败策略 | ✅ 已实现 | doGenerateWithFallback，单条失败标记 failed，继续生成 |
| **P2** | 产品话术引用（BR-09） | ✅ 已实现 | LiveProduct.scriptSource=product + productScriptId，generateFull 时引用 |
| **P3** | 预计时长 | ✅ 已实现 | estimatedDurationSeconds（字数/3），单条+整场展示 |

### 2.4 主播端入口与动线

| 入口 | 设计 | 实现 | 说明 |
|------|------|------|------|
| 场次列表 | 卡片「生成话术」→ 进入场次详情话术 Tab | ✅ 有「生成话术」按钮 | 可优化为直接唤起一键生成 |
| 场次详情 | Tab：产品 / 话术 / 数据 / AI 复盘 | ✅ 有 | 话术 Tab 为核心构建入口 |
| 话术 Tab | [一键生成整场话术] 风格：[亲切 ▼] | ✅ 有一键生成 + 风格选择 | — |
| 开播准备 | 话术未生成时 [去生成话术 →] | ✅ 有 | — |

---

## 三、数据库层

### 3.1 表结构对照

| 文档表名 | 实现表名 | 状态 | 说明 |
|----------|----------|------|------|
| live_session | live_session | ✅ | persona_id、session_cover、script_style、readiness_check 已补齐 |
| live_session_product | live_product | ✅ | script_source、product_script_id、deleted、update_time 已补齐 |
| live_session_script | live_script | ✅ | product_id 等字段已补齐，generateFull 已写入 product_id |
| live_session_data | live_session_data | ✅ | 通过 migration 创建，含 ai_analysis |
| live_product_data | live_product_data | ✅ | 通过 migration 创建 |
| live_monitor | live_monitor | ✅ | 核心字段 + 扩展字段（total_viewers、new_followers、online_count、gmv、orders）已补齐 |
| live_script_template | live_script_template | ✅ | 高效话术入库 |

### 3.2 已执行 migration

- `sql/live/migration-fields.sql`：live_session、live_product、live_script 字段补齐
- `sql/live/migration-ai-analysis.sql`：live_session_data.ai_analysis
- `sql/live/migration-script-template.sql`：live_script_template 表
- `sql/live/migration-monitor-fields.sql`：live_monitor 扩展字段

---

## 四、API 接口

### 4.1 实现对照（路径为 /api/v1/live/）

| 文档设计 | 实现路径 | 状态 |
|----------|----------|------|
| GET /sessions/{id} | POST /session/get?id= | ✅ |
| PUT /sessions/{id} | POST /session/save | ✅ |
| GET /sessions/{id}/readiness | POST /session/readiness?id= | ✅ |
| GET /sessions/{id}/overview | POST /session/overview?id= | ✅ |
| 产品 CRUD + 列表 | /product/search、save、delete、by-session、batch-sort | ✅ |
| 话术 CRUD + 编辑 | /script/search、save、delete、by-session | ✅ |
| 开场/产品/转场/结尾/整场生成 | /ai/generate-opening、generate-product、generate-transition、generate-closing、generate-full | ✅ |
| 标记已执行 | POST /script/executed | ✅ |
| 话术效果排行 | POST /script/effectiveness | ✅ |
| 数据同步 | /data/session/sync、session、product | ✅ |
| LiveMonitor 时序 | /monitor/by-session | ✅ |
| AI 分析 | /analysis/generate、get | ✅ |
| 历史对比 | /data/history | ✅ |
| **保存到话术库** | /script/save-to-library、save-batch-to-library | ✅ |
| **导出话术** | /script/export | ✅ |

### 4.2 路径差异说明

文档采用 RESTful 嵌套路径，当前实现采用扁平 POST 路径。功能等价。

### 4.3 状态机与业务规则

- **状态机**：preparing(0) → live(1) → ended(2)，禁止逆向 ✅
- **开播前置**：至少 1 个产品、已关联人设 ✅
- **状态变更**：0→1 记录 startTime，1→2 触发 sync + attribution ✅

---

## 五、前端页面

### 5.1 页面实现对照

| 文档页面 | 路由 | 实现 | 说明 |
|----------|------|------|------|
| 直播场次列表 | /admin/live/sessions | ✅ LiveSessionPage | 表格+卡片，筛选 |
| 创建/编辑场次 | 弹窗 | ✅ FormDialog | sessionCover、scriptStyle |
| 场次详情 | /admin/live/sessions/:id | ✅ LiveSessionDetailPage | Tab：产品/话术/数据/AI 复盘 |
| 选品管理 | 场次详情 Tab + 独立页 | ✅ | 从商品库添加、排序 |
| 话术编辑器 | 场次详情 Tab | ✅ ScriptsTab | 生成开场/产品/一键生成、编辑、违规检测、标记执行 |
| 开播准备清单 | 场次详情 Tab | ✅ ReadinessTab | 产品/人设/话术/合规检查 |
| 直播数据看板 | 场次详情 Tab | ✅ DataTab | 汇总 + ECharts 时序图 |
| AI 复盘报告 | 场次详情 Tab | ✅ AiTab | 生成/获取报告 |
| 历史数据对比 | /admin/live/history | ✅ LiveHistoryComparePage | GMV/观众趋势对比 |
| 话术效果排行 | /admin/live/script-ranking | ✅ LiveScriptRankingPage | 按场次查看效果 |
| 机构直播总览 | /org/live/sessions | ✅ OrgLiveSessionPage | DataScope 校验 |
| 机构复盘查看 | /org/live/reviews | ✅ OrgLiveReviewsPage | 旗下达人复盘 |

### 5.2 话术编辑器差距（文档 06 对照）

| 文档设计 | 实现 | 差距 |
|----------|------|------|
| [一键生成整场话术] 风格：[亲切 ▼] | 有一键生成 + 风格选择 | ✅ |
| 每条话术：预计时长 45s | estimatedDurationSeconds | ✅ |
| 整场话术预计时长：15分钟 | 整场预计时长展示 | ✅ |
| [保存到话术库] [导出话术] | 单条/批量保存、导出 .md | ✅ |
| 转场话术展示：XX精华液 → YY面霜 | 根据 products + sequence 计算展示 | ✅ 已实现 |

### 5.3 场次列表交互

- 卡片式展示 ✅
- 按状态操作：preparing → 编辑/选品/生成话术/开播准备；live → 查看数据；ended → 数据/AI复盘/话术排行 ✅
- 筛选：账号、状态、日期范围 ✅

---

## 六、开发任务完成度

### 6.1 后端任务

| 任务 | 状态 | 说明 |
|------|------|------|
| B01 数据库初始化 | ✅ | migration 已补齐 |
| B02–B07 实体 | ✅ | 已补齐 |
| B08–B25 服务/控制器 | ✅ | 见上表 |
| **B26 产品话术 product_id 写入** | ✅ | doGenerate 产品话术时 setProductId |
| **B27 转场话术 from/to 产品名** | ✅ | transition 传入 fromProductName/toProductName |
| **B28 保存到话术库** | ✅ | saveToLibrary、saveBatchToLibrary |
| **B29 导出话术** | ✅ | exportScripts |
| **B30 BR-11 失败策略** | ✅ | doGenerateWithFallback |

### 6.2 前端任务

| 任务 | 状态 | 说明 |
|------|------|------|
| F01–F12 | ✅ | 见上表 |
| **F13 话术风格选择** | ✅ | 风格下拉 + 透传 session.scriptStyle |
| **F14 保存到话术库** | ✅ | 单条/批量保存按钮 |
| **F15 导出话术** | ✅ | 导出为 .md 文件 |

---

## 七、优先级建议

### P0 — 已补齐 ✅

1. **产品话术写入 product_id**：已实现
2. **转场话术传入 from/to 产品名**：已实现

### P1 — 已补齐 ✅

3. **保存到话术库**：已实现（单条/批量）
4. **导出话术**：已实现（Markdown）
5. **话术风格透传**：已实现

### P2 — 已补齐 ✅

6. **BR-11 失败策略**：已实现
7. **产品话术引用**：已实现（LiveProduct.scriptSource + productScriptId）
8. **预计时长**：已实现（estimatedDurationSeconds）

### P3 — 已补齐 ✅

9. **live_monitor 扩展字段**：total_viewers、new_followers、online_count、gmv、orders
10. **开播准备 Tab + 独立路由**：场次详情新增「开播准备」Tab，路由 `/sessions/:id/readiness` 重定向到 `?tab=readiness`
11. **转场话术展示**：产品A → 产品B

### P4 — 可选

12. RESTful 路径统一

---

## 八、总结

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 数据库 | ~98% | 核心表 + live_monitor 扩展字段已补齐 |
| 后端 API | ~95% | P0-P2 已落地 |
| 前端页面 | ~98% | 开播准备 Tab、转场展示、保存/导出/风格已实现 |
| 业务规则 | ~95% | BR-11 等已落地 |
| **为主播构建话术** | **~98%** | P0-P3 已全部落地 |

**结论**：直播模块 P0-P3 全面升级已完成。「为主播构建直播话术」核心场景已打通。开播准备 Tab、转场话术「产品A→产品B」展示、live_monitor 扩展字段均已落地。
