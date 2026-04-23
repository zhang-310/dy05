# 直播模块角色功能矩阵

**版本**：v1.0  
**创建**：2026-03-26  
**维护方**：前端研发 + 产品

---

## 一、功能矩阵

| 路由路径 | 功能描述 | admin | talent（主播/运营） | org（机构管理员） | 裁剪意图 |
|---------|---------|:-----:|:-------------------:|:-----------------:|---------|
| `live/sessions` | 场次列表 | Y | Y | Y（`OrgLiveSessionPage`） | 三端均可见，org 用独立组件 |
| `live/sessions/:id` | 场次详情 | Y | Y | Y | 三端均支持 |
| `live/sessions/create` | 创建场次 | Y | Y | Y | 三端均可创建 |
| `live/workbench/:id` | 一站式工作台 | Y | Y | Y | 核心功能，全端开放 |
| `live/realtime` | 实时提词面板 | Y | Y | Y（通过 `OrgLayout`） | 播中核心，全端开放 |
| `live/history` | 历史场次对比 | Y | Y | **N** | org 端当前不可用，待确认需求 |
| `live/script-ranking` | 话术效果排行 | Y | Y | **N** | org 端当前不可用，待确认需求 |
| `live/script` | 话术工作台 V2 | Y | Y | **N** | org 端当前不可用，待确认需求 |
| `live/product` | 直播选品 | Y | Y | **N** | org 端当前不可用，待确认需求 |
| `live/rhythm` | 节奏优化 | Y | Y | Y（通过 `OrgLayout`） | 三端均支持 |
| `live/script-versions` | 话术版本管理 | Y | **N** | **N** | 版本管理定位为管理员权限 |
| `live/reviews` | 场次复盘查看 | **N** | **N** | Y | org 专属，汇总机构主播场次复盘 |

---

## 二、待确认事项

以下路由的 org 端能力空缺，需产品团队确认是否为**刻意设计**：

| 路由 | 空缺角色 | 建议 | 优先级 |
|-----|---------|------|--------|
| `live/history` | org 端 | 机构管理员有对比分析诉求，建议添加 | P2 |
| `live/script-ranking` | org 端 | 机构管理员需查看各主播话术排名，建议添加 | P2 |
| `live/script` | org 端 | 机构级话术库管理有价值，待讨论 | P3 |
| `live/product` | org 端 | 机构统一选品有价值，待讨论 | P3 |
| `live/script-versions` | talent 端 | talent 可能需要管理自己的话术版本，建议添加 | P2 |

---

## 三、实现现状

### admin 端（`/admin`）

完整功能，包含所有直播子路由。

```
/admin/live/sessions         → LiveSessionPage
/admin/live/workbench/:id    → LiveWorkbenchPage
/admin/live/history          → LiveHistoryComparePage
/admin/live/realtime         → LiveRealtimePanelPage
/admin/live/rhythm           → LiveRhythmPage
/admin/live/script-ranking   → LiveScriptRankingPage
/admin/live/script           → LiveScriptBuilderPage
/admin/live/script-versions  → LiveScriptVersionPage
/admin/live/product          → LiveProductPage
```

### talent 端（`/talent`）

接近 admin，缺少 `script-versions` 管理：

```
/talent/live/sessions         → LiveSessionPage
/talent/live/workbench/:id    → LiveWorkbenchPage
/talent/live/history          → LiveHistoryComparePage
/talent/live/realtime         → LiveRealtimePanelPage
/talent/live/rhythm           → LiveRhythmPage
/talent/live/script-ranking   → LiveScriptRankingPage
/talent/live/script           → LiveScriptBuilderPage
/talent/live/product          → LiveProductPage
❌ /talent/live/script-versions  （待评估是否添加）
```

### org 端（`/org`）

仅包含场次管理、复盘、实时提词、节奏优化：

```
/org/live/sessions        → OrgLiveSessionPage（机构视角列表）
/org/live/realtime        → LiveRealtimePanelPage
/org/live/rhythm          → LiveRhythmPage
/org/live/reviews         → OrgLiveReviewsPage（机构复盘列表）
❌ /org/live/history       （待评估：建议添加）
❌ /org/live/script-ranking（待评估：建议添加）
❌ /org/live/script        （待评估）
❌ /org/live/product       （待评估）
```

---

## 四、路由注释规范

在 `frontend-react/src/router/index.tsx` 中，每个角色区块的路由项应添加注释：

```tsx
// === ORG 端直播路由 ===
// 机构管理员视角：场次管理 + 复盘 + 实时提词 + 节奏优化
// 待确认：history、script-ranking、script-versions 是否需要添加
// 参考：docs/design/live-role-capability-matrix.md
```

---

## 五、变更记录

| 日期 | 变更内容 | 责任人 |
|------|---------|--------|
| 2026-03-26 | 初始版本：梳理现有矩阵，标注待确认项 | 系统生成 |
