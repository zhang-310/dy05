# 直播模块 — 深度分析与升级建议

> 分析日期：2026-03-03 | 对照：00-大纲 v3.0、LIVE_MODULE_GAP_ANALYSIS

---

## 一、总体结论

直播模块**核心闭环已打通**，文档与实现整体对齐度约 **90%**。主要差距集中在：**命名历史遗留**、**抖音 API 同步未对接**、**部分文档字段与实现不一致**。以下从架构、数据、接口、前端、技术债五个维度深度分析，并给出升级建议。

---

## 二、架构层分析

### 2.1 设计 vs 实现对照

| 设计组件 | 实现 | 差距 |
|----------|------|------|
| 场次管理引擎 | LiveSessionController + LiveSessionServiceImpl | ✅ 完整 |
| AI 话术引擎 | LiveAiController + LiveAiServiceImpl | ✅ 完整 |
| 数据同步引擎 | LiveDataSyncController + LiveDataSyncServiceImpl | ⚠️ 无抖音 API 对接 |
| AI 复盘与分析层 | LiveAnalysisController + LiveAnalysisServiceImpl | ✅ 完整 |
| 话术效果归因 | LiveScriptAttributionServiceImpl | ✅ 完整 |
| 高效话术入库 | LiveScriptTemplateScheduler（每日 04:00） | ✅ 完整 |
| 实时监控推送 | LiveMonitorSseController（SSE） | ⚠️ 文档写 WebSocket，实现用 SSE |

### 2.2 实时推送方式

- **文档**：WebSocket `ws://host/api/v1/live/ws/{sessionId}`
- **实现**：SSE `GET /api/v1/live/monitor/stream/{sessionId}`
- **结论**：SSE 与 WebSocket 功能等价（服务端推），实现更轻量。建议文档统一为「SSE 或 WebSocket」，或补充 SSE 端点说明。

---

## 三、数据层分析

### 3.1 命名差异（文档 vs 实现）

| 文档（02-数据库设计） | 实现（Entity/Schema） | 说明 |
|---------------------|----------------------|------|
| owner_id | user_id | 语义等价，历史命名 |
| session_title | live_title | 历史命名 |
| planned_start_time | scheduled_time | 历史命名 |
| actual_start_time | start_time | 历史命名 |
| actual_end_time | end_time | 历史命名 |
| record_time（live_monitor） | timestamp | 实现用 timestamp |
| last_sync_time | sync_time | live_session_data |
| total_gmv | total_revenue | live_session_data |

**建议**：保持实现不变，在 02-数据库设计或 README 中增加「实现字段映射表」，避免新人困惑。

### 3.2 live_session_data 字段对照

| 文档字段 | 实现字段 | 状态 |
|----------|----------|------|
| total_viewers | total_viewers | ✅ |
| peak_viewers | peak_viewers | ✅ |
| avg_watch_duration | avg_stay_time | ⚠️ 命名不同 |
| new_followers | new_followers | ✅ |
| likes | total_likes | ⚠️ 命名不同 |
| total_gmv | total_revenue | ⚠️ 命名不同 |
| ai_analysis | ai_analysis | ✅ |
| ai_review_id | ai_review_id | ✅ |
| last_sync_time | sync_time | ⚠️ 命名不同 |

### 3.3 live_product_data 字段对照

| 文档字段 | 实现字段 | 状态 |
|----------|----------|------|
| gmv | revenue | ⚠️ 命名不同 |
| click_count | clicks | ⚠️ 命名不同 |
| conversion_rate | conversion_rate | ✅ |
| refund_rate | — | ❌ 实现用 refund_quantity |
| exposure_count | impressions | ⚠️ 命名不同 |

---

## 四、接口层分析

### 4.1 API 实现覆盖度

| 分组 | 文档数 | 实现数 | 覆盖 |
|------|--------|--------|------|
| 场次管理 | 8 | 10+（含 viewers/likes/trend） | ✅ 超设计 |
| 产品管理 | 5 | 6（含 batch-sort） | ✅ |
| 话术管理 | 12 | 10（script）+ 7（ai） | ✅ |
| 数据同步 | 4 | 6（含 history、product/save） | ✅ |
| AI 分析 | 3 | 3（generate、get、review） | ✅ |
| 实时监控 | — | 3（stream、snapshot、push） | ✅ |

### 4.2 路径风格

- **文档**：RESTful 嵌套 `GET /sessions/{id}`
- **实现**：扁平 POST `POST /session/get?id=`
- **结论**：功能等价，统一 POST 符合项目规范。无需强制改为 RESTful。

---

## 五、业务逻辑分析

### 5.1 抖音数据同步

- **文档设计**：调用抖音开放平台 API 拉取直播汇总数据、分产品数据
- **当前实现**：`LiveDataSyncServiceImpl.saveSessionData` 接收前端/内部 VO，手动写入；或从 LiveMonitor 聚合
- **差距**：**未对接真实抖音 API**，数据来源为手动录入或 LiveMonitor 聚合

**升级建议**：
1. 若抖音 API 已开放直播数据接口：新增 `DouyinLiveDataClient`，在 status→ended 时自动调用并写入
2. 若暂未开放：保留当前实现，文档注明「数据来源：手动录入 / LiveMonitor 聚合，抖音 API 对接为后续迭代」

### 5.2 话术效果归因

- **实现**：`LiveScriptAttributionServiceImpl.runAttribution` 按 actual_execution_time 与 LiveMonitor 时序计算 30s 窗口的 viewer_delta、interaction_delta、effectiveness_score
- **触发**：需在直播结束后显式调用（如 status 变更监听）
- **结论**：逻辑完整，与文档 04-直播话术系统 4.5 一致

### 5.3 高效话术入库

- **实现**：`LiveScriptTemplateScheduler` 每日 04:00 扫描 effectiveness_score >= 80 的话术，入库 live_script_template
- **结论**：与 BR-19 一致

---

## 六、前端分析

### 6.1 路由覆盖

| 角色 | 路由 | 实现 |
|------|------|------|
| admin | /admin/live/sessions、history、script-ranking | ✅ |
| talent | /talent/live/sessions、history、script-ranking | ✅ |
| org | /org/live/sessions、reviews | ✅ |

### 6.2 场次详情 Tab

- 产品、话术、数据、AI 复盘、开播准备 — 均已实现
- 与文档 06-页面设计 一致

---

## 七、技术债与风险

| 项 | 风险 | 建议 | 状态 |
|----|------|------|------|
| user_id vs owner_id | 多租户场景下需确保语义一致 | 代码注释明确 user_id=owner_id | ✅ Entity 已加注释 |
| 抖音 API 未对接 | 汇总数据依赖手动/LiveMonitor | 文档标注，规划 P2 迭代 | 文档已标注 |
| live_monitor 归档 | 文档有 live_monitor_archive | 归档任务已实现 | ✅ 迁移到 archive 后删除 |
| ai_review_id | 复盘报告与 ai 模块关联 | 已补充字段及 review 接口 | ✅ |
| 错误码 3307–3313 | 已注册，部分场景未使用 | 在对应 Service 中按场景使用 | ✅ LiveProductServiceImpl |

---

## 八、升级建议（按优先级）

### P0 — 文档与实现对齐（无代码改动）

1. **02-数据库设计**：增加「实现字段映射表」，说明 user_id=owner_id、live_title=session_title 等
2. **05-数据同步与分析**：补充「当前数据来源：手动录入 / LiveMonitor 聚合」，标注抖音 API 为后续迭代
3. **05-数据同步**：补充 SSE 端点说明，与 WebSocket 二选一或并存

### P1 — 低风险增强

4. **live_session_data**：若 ai 模块有 ai_live_review，补充 ai_review_id 字段及 migration
5. **LiveMonitor 归档**：确认是否有定时任务，若无则新增 `LiveMonitorArchiveScheduler`（每月归档 90 天前数据）
6. **错误码使用**：在 LiveProductServiceImpl 重复添加产品时返回 3313，非法产品时返回 3307

### P2 — 抖音 API 对接（需产品确认）

7. 对接抖音开放平台直播数据接口，实现自动同步
8. 同步失败告警（日志 + 可选企微）

### P3 — 可选优化

9. RESTful 路径统一（工作量大，收益有限，可延后）
10. 表名字段名迁移（owner_id、session_title 等，需全量数据迁移，风险高，不建议短期执行）

---

## 九、总结

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 核心话术闭环 | ~98% | 生成→违规检测→执行→归因→入库 已打通 |
| 数据模型 | ~90% | 命名差异为主，功能完整 |
| API 覆盖 | ~95% | 32 个设计接口基本实现 |
| 抖音数据同步 | ~40% | 依赖手动/LiveMonitor，未对接抖音 API |
| 前端页面 | ~98% | 12 页齐全，talent 路由已补 |

**结论**：直播模块可作为生产可用版本。P0/P1 升级已完成，P2 需产品确认后推进。

---

## 十、升级完成状态（2026-03-03）

| 优先级 | 项 | 状态 |
|--------|-----|------|
| P0 | 02-数据库设计 2.6 实现字段映射表 | ✅ |
| P0 | 05-数据同步 5.1.1 当前数据来源 | ✅ |
| P0 | 05-数据同步 5.2.2 SSE 端点说明 | ✅ |
| P1 | live_session_data.ai_review_id + migration | ✅ |
| P1 | LiveMonitor 归档到 live_monitor_archive | ✅ |
| P1 | 错误码 3313/3307 在 LiveProductServiceImpl | ✅ |
| P1 | AI 复盘报告独立接口 POST /analysis/review | ✅ |
| P1 | LiveSession.userId 注释 user_id=owner_id | ✅ |
| P2 | 抖音 API 对接 | 待产品确认 |
| P2 | 同步失败告警 | 待产品确认 |
