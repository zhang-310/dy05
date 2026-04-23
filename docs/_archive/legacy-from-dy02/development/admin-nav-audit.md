# 管理员侧栏与 `/admin` 路由审计（2026-03-22）

> 与 [`AdminLayout.tsx`](../../frontend-react/src/layouts/AdminLayout.tsx)、[`router/index.tsx`](../../frontend-react/src/router/index.tsx) 同步；变更侧栏或新增管理员路由时请更新本节。

## 一、设计决策摘要

| 项 | 说明 |
|----|------|
| 主栏「首页」移除 | 工作台、统一 KPI 的 `pathPrefixes` 并入 **运营**，避免从运营点「工作台/数据」后主栏漂到「首页」。 |
| 合规入口 | 菜单与路由统一为 **`/admin/system/compliance`**（原错误 `script/compliance` 已修正）。 |
| 创作中心 | 由单组「更多」拆为 **策划与素材 / 成片与发布 / 运营与质量** 三组。 |
| 内容库 | 拆为 **商品 / 话术与文案 / 合规与词库** 三组（合规检测页在系统菜单，API 仍为 `POST /api/v1/script/compliance/*`）。 |
| 孤儿路由收口 | **`/admin/ai/model-benchmark`**、**`/admin/system/external-api-health`** 已挂入侧栏。 |

## 二、`/admin` 路由与菜单对照

### 2.1 已在侧栏覆盖（静态路径）

管理员 `children` 中下列 **path 片段**（相对 `/admin`）在 `AdminLayout` 的 `secondaryMenus` 中均有对应入口，或通过 `Navigate`/重定向从已列父路径进入：

- `dashboard`, `analytics/kpi`
- `auth/*`, `log/*`, `config`, `storage`
- `douyin/*`, `copy`, `copy/library`, `copy/approval`, `copy/template`
- `shortvideo`（重定向）及全部 `shortvideo/*` 业务页
- `live/*`（含 `createLiveRoutes` 与场次动态段）
- `product` 及 `product/*`
- `script/list`, `script/violation`, `script/check`, `script/template`
- `slangdict`, `abtest`, `agent`, `agent/chat/:agentId`
- `ai/*`（含 `knowledge/:kbId/*` 动态段）
- `wecom/*`
- `system`, `system/api-log`, `system/sync-log`, `system/external-api`, **`system/compliance`**, **`system/external-api-health`**
- `tianapi`

### 2.2 动态与重定向说明

| 模式 | 说明 |
|------|------|
| `live/sessions/:id/...` | 由列表/命令面板进入；前缀 `/admin/live` 归属运营主栏。 |
| `ai/knowledge/:kbId/search` 等 | 前缀 `/admin/ai` 归属 AI 主栏。 |
| `douyin/accounts/:id` | 前缀 `/admin/douyin` 归属运营主栏。 |
| `copy` 根路径 | 路由存在；侧栏以 `copy/library` 等为主入口，`/admin/copy` 可视为同模块落地页。 |

### 2.3 重复 URL（刻意保留）

同一 URL 可在 **核心入口** 与 **直播中心 / 内容库** 等分组同时出现，用于不同任务上下文下的快捷访问；若需收敛为「每 URL 仅一处」，可再删次要重复项。

### 2.4 主栏归属规则

`BaseLayout` 使用 `primarySections` 中 **`pathPrefixes` 首次匹配** 决定主栏高亮。当前顺序为 **运营 → 系统 → AI**；`/admin/dashboard`、`/admin/analytics` 已挂在 **运营** 前缀下，与「数据在工作台侧」心智一致。

## 三、单一配置源（`adminNavConfig`）可行性

**收益**：路由与菜单同学一份结构，可减少漏登记、断链（如历史 `script/compliance` 类问题），并便于生成 E2E 路由清单。

**成本与约束**：

1. **懒加载**：`router/index.tsx` 使用 `React.lazy` + `LazyRoute`，若由配置生成 `RouteObject[]`，需保持 `lazy(() => import(...))` 与现有 chunk 分割策略一致。
2. **动态路由**：`:id`、`?tab=` 等无法枚举；配置中宜用 **前缀 + `dynamic?: true`**，校验脚本只验证「静态前缀已登记」。
3. **角色**：`OrgLayout`/`TalentLayout` 与 `AdminLayout` 非同一菜单；单一配置宜按 `role: 'admin' | 'org' | 'talent'` 分表或分文件。
4. **推荐落地顺序**：先维持 `AdminLayout` 为唯一菜单源，使用 **`frontend-react/scripts/check-admin-nav.mjs`**（`npm run check-admin-nav`）做断链与关键入口回归；后续可扩展为与 `router` 的 **前缀集合对比**；待稳定后再合并为一份 `adminNav.config.ts` 生成菜单。

**结论**：可行，建议 **先校验脚本、后配置生成**，避免一次性大改 router。

## 四、面包屑 `PATH_LABELS`

[`BaseLayout.tsx`](../../frontend-react/src/layouts/BaseLayout.tsx) 中 `PATH_LABELS` 已补充 `compliance`、`external-api-health`、`model-benchmark` 等片段；新增深层路径时同步增加键值。
