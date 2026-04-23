# Admin 前端第四轮升级方案 — 导航布局重构 + 工作台修复 + 全页面审计

> 2026-03-22 · 基于第三轮 Cursor 升级后的深度分析
> 聚焦：顶部 Tab → 左侧图标栏 / 话术工作台双导航冲突 / 全模块逐页审计

---

## 零、文档与代码核对（2026-03-22 修订基线）

以下为仓库对照后的**事实基线**。原稿中失效行号、LOC、以及「BaseLayout 已存在 useTopModules + 水平 Tab」等描述与旧快照不一致；**实施以当前代码与下表为准**。

### 0.1 已完成项（无需重复开发）

| 编号 | 说明 |
|------|------|
| WB-01 / WB-04 | `SessionWorkspacePage` 使用 `embeddedInWorkbench` 隐藏嵌套 header；`basePath` 已用 `useRolePrefix()` |
| WB-02 | `LiveWorkbenchPage` 的 `completed` 已与方案 2.3 节建议对齐 |
| WB-03 | `sessionWorkbenchNav.ts`：`getNextWorkbenchStep` / `sessionWorkbenchHref` |
| NAV-07 | `adminModuleConfig` 直播场次文案已为 **场次管理**（无括号说明） |
| QA-01 方向 | 已存在 `frontend-react/src/pages/live/components/SessionActions.tsx` 供卡片/表格共用 |
| DLG-01 / DLG-04 | `ProductScriptVersionPage`、`ProjectManagementPage` 主表单已改用 `FormDialog` |
| DLG-05 | `ApiLogPage` 详情 Drawer 已统一为 600px |
| DLG-02 | `CopyLibraryPage` **保留**自定义 `Dialog`：含「AI 写文案」折叠区，与 `FormDialog`（`extraTopContent` 在字段上方）顺序不一致，强行迁移收益低 |
| DLG-03 | **不适用**：当前 `MaterialLibraryPage` 无批量打标弹窗（无 `Dialog`） |
| QA-02 | 当前 `LiveSessionPage` 未使用 MUI `DataGrid`，无独立 `localeText` / `LIVE_SESSION_GRID_LOCALE` 重复问题 |

### 0.2 原稿与现状不符（已更正表述）

| 原表述 | 实际情况 |
|--------|----------|
| BaseLayout 已有 `useTopModules` 与水平 `moduleTabsRow` | 旧版仅有 `primarySections` + `secondaryMenus`；**无**水平模块 Tab |
| `AdminLayout`「不变」且已传 `topModules` | 旧版仅传双栏配置；**需在 AdminLayout 传入** `topModules`、`topModuleSidebars`、`topModuleIcons`（与 BaseLayout 新 props 对齐） |
| `ViralLibraryPage` 约 1754 LOC | 当前仓库该文件约 **260 行**量级；拆分优先级需 **重扫** |
| `LiveSessionPage` 约 827 LOC | 当前约 **487 行** |
| `DashboardPage` 5 个 `useEffect` | 当前多为 **1 个**；QA-03 以「是否仍需合并」为准，非硬性 |
| NAV 改动「Line ~423」等 | **行号不固化**，以 `BaseLayout.tsx` 实际结构为准 |

### 0.3 大文件 LOC 重扫（可复现）

在 `frontend-react/` 下执行：

```bash
node scripts/count-large-pages.mjs
```

输出 `>=800` 行的页面列表，用于 SPLIT/DLG 批次排序（见第六节补充）。

---

## 一、导航布局重构：顶部 Tab → 左侧图标栏

### 1.1 当前问题

截图可见，当前布局是：

```
┌──────────────────────────────────────────────────────────────┐
│ ≡ 首页 > 管理后台 > 直播场次                    🔔 ⭐ 🌐 🎨 🌙 用户 │  ← AppBar
├──────────────────────────────────────────────────────────────┤
│ [🏠 工作台] [📺 直播] [🎬 短视频] [📦 商品] [🤖 AI] [⚙ 设置] │  ← 水平 Tab（浪费整行高度）
├──────────┬───────────────────────────────────────────────────┤
│ 直播中心  │                                                   │
│ ├ 场次管理│               内容区                               │
│ ├ 实时提词│                                                   │
│ ├ 历史对比│                                                   │
│ ├ 话术排行│                                                   │
│ ├ 节奏优化│                                                   │
│ └ 直播选品│                                                   │
└──────────┴───────────────────────────────────────────────────┘
```

**问题**：
1. 水平 Tab 占据整行高度（~44px），压缩内容区垂直空间
2. 6 个 Tab + 文字标签是水平排列，不符合 B 端后台常见的「左侧主导航 + 右侧子菜单」范式（类似飞书/企业微信/Slack 风格）
3. 用户明确要求：**总分类导航放在最左边**

### 1.2 目标布局

```
┌────┬────────────┬──────────────────────────────────────────┐
│    │            │ ≡ 首页 > 直播 > 场次管理    🔔 ⭐ 🌙 用户 │  ← AppBar（去掉模块 Tab）
│ DY ├────────────┼──────────────────────────────────────────┤
│    │            │                                          │
│ 🏠 │  直播中心   │                                          │
│    │ ├ 场次管理  │            内容区                         │
│ 📺 │ ├ 实时提词  │                                          │
│    │ ├ 历史对比  │                                          │
│ 🎬 │ ├ 话术排行  │                                          │
│    │ ├ 节奏优化  │                                          │
│ 📦 │ └ 直播选品  │                                          │
│    │            │                                          │
│ 🤖 │            │                                          │
│    │            │                                          │
│ ⚙  │            │                                          │
└────┴────────────┴──────────────────────────────────────────┘
 64px    200px                   flex
```

### 1.3 技术方案（复用已有 primaryBar 基础设施）

BaseLayout.tsx 已有 `primaryBar`（64px 图标条）和 `secondaryDrawer`（200px 菜单），在 `useTwoPanel`（`primarySections` + `secondaryMenus`）下启用。**若曾在外部用水平 Tab 切换模块，该路径应废弃**；统一改为在 `useTopModules` 下由 `primaryBar` 渲染 `topModules`，不再占用 AppBar 下一整行 Tab。

**核心改动**：新增 `topModules` + `topModuleSidebars`（及可选 `topModuleIcons`），与 `useTwoPanel` 一样走 `primaryBar` + `secondaryDrawer`；`hideSidebar` 的模块（如工作台）仅保留 64px 图标条。

#### 改动 1：primaryBar 启用条件（Line ~423）

```typescript
// 旧：
const primaryBar = useTwoPanel && (...)

// 新：
const primaryBar = (useTwoPanel || useTopModules) && (...)
```

#### 改动 2：primaryBar 渲染内容（Line ~454-471）

当 `useTopModules` 时，遍历 `topModules` 而非 `primarySections`：

```typescript
const items = useTopModules ? topModules! : primarySections!
items.map((s) => {
  const isActive = useTopModules
    ? s.id === activeTopModule?.id
    : s.id === resolvedActiveSection
  const handleClick = useTopModules
    ? () => handleTopModuleTabChange({} as SyntheticEvent, s.id)
    : () => handlePrimaryClick(s.id)
  return (
    <Tooltip key={s.id} title={s.label} placement="right">
      <IconButton onClick={handleClick} sx={{
        my: 0.5,
        borderRadius: 1.5,
        color: isActive ? 'primary.main' : 'text.secondary',
        bgcolor: isActive ? 'action.selected' : 'transparent',
        '&:hover': { bgcolor: isActive ? 'action.selected' : 'action.hover' },
      }}>
        {useTopModules ? (topModuleTabIcons?.[s.id] ?? <DashboardIcon fontSize="small" />) : s.icon}
      </IconButton>
    </Tooltip>
  )
})
```

在图标下方显示缩写文字标签（shortLabel），增强识别度：

```typescript
<Box sx={{ textAlign: 'center' }}>
  <IconButton ...>{icon}</IconButton>
  <Typography variant="caption" sx={{ fontSize: '0.625rem', display: 'block', mt: -0.5 }}>
    {s.shortLabel ?? s.label.slice(0, 2)}
  </Typography>
</Box>
```

#### 改动 3：隐藏水平 moduleTabsRow（Line ~675-711）

```typescript
// 旧：
const moduleTabsRow = useTopModules && !showBottomModuleNav && topModules && activeTopModule && (
  <Box>...<Tabs>...</Tabs></Box>
)

// 新：完全移除或设为 null
const moduleTabsRow = null  // 模块选择已在左侧 primaryBar 中
```

#### 改动 4：sidebar 宽度计算（Line ~658-666）

```typescript
const totalSidebarWidth = sidebarHidden
  ? 0
  : useTopModules
    ? (open ? PRIMARY_BAR_WIDTH + SECONDARY_DRAWER_WIDTH : PRIMARY_BAR_WIDTH)
    : useTwoPanel
      ? (open ? PRIMARY_BAR_WIDTH + SECONDARY_DRAWER_WIDTH : PRIMARY_BAR_WIDTH)
      : (open ? DRAWER_WIDTH : 0)
```

**关键**：`useTopModules` 时，即使侧栏关闭（`open=false`），左侧图标条（64px）始终可见，只是二级菜单折叠。

#### 改动 5：图标条底部放置 Logo/快捷操作

primaryBar 顶部是 Logo（`DY`），底部放 `设置` 和 `用户头像`，与飞书/Slack 风格一致：

```typescript
// primaryBar 结构
<Box sx={{ width: PRIMARY_BAR_WIDTH, height: '100vh', display: 'flex', flexDirection: 'column' }}>
  {/* Logo */}
  <Box sx={{ p: 1.5, textAlign: 'center' }}>
    <Typography variant="h6" fontWeight={700} color="primary">{shortTitle || 'DY'}</Typography>
  </Box>
  <Divider />

  {/* 模块图标 */}
  <Box sx={{ flex: 1, py: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
    {items.map(...)}
  </Box>

  {/* 底部：设置 + 用户 */}
  <Divider />
  <Box sx={{ p: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
    <IconButton size="small" onClick={toggleTheme}>
      {darkMode ? <LightModeIcon /> : <DarkModeIcon />}
    </IconButton>
    <Avatar sx={{ width: 32, height: 32, cursor: 'pointer' }} onClick={openUserMenu}>
      {username?.[0]}
    </Avatar>
  </Box>
</Box>
```

#### 改动 6：移动端适配

- **xs 屏**：保留 BottomNavigation（已有），primaryBar 隐藏
- **sm-md 屏**：primaryBar 变为 temporary Drawer，点击图标展开 secondaryDrawer
- **md+ 屏**：primaryBar 常驻，secondaryDrawer 可折叠

#### 改动文件清单

| 文件 | 改动 | 说明 |
|------|------|------|
| `layouts/BaseLayout.tsx` | 核心重构 | primaryBar 兼容 topModules、去掉 moduleTabsRow、宽度计算 |
| `layouts/adminModuleConfig.tsx` | 微调 | 确认 shortLabel 字段完整（已有） |
| `layouts/AdminLayout.tsx` | **接入配置** | 从 `adminModuleConfig` 传入 `topModules`、`topModuleSidebars`、`topModuleIcons`（替代仅 `primarySections` / `secondaryMenus`） |

---

## 二、话术工作台逻辑问题修复

### 2.1 当前问题：双导航冲突

`LiveWorkbenchPage` 和 `SessionWorkspacePage` 各自有独立的步骤导航，嵌套渲染时出现**双重导航**：

**LiveWorkbenchPage**（外层，5 步 Stepper）：
```
[基础信息] → [选品排品] → [话术微调] → [开播准备] → [数据复盘]
```

**SessionWorkspacePage**（内层，5 个 Chip 面包屑）：
```
[选品排品(N)] → [AI生成话术] → [话术微调(N)] → [准备发布] → [数据]
```

当用户点击 Stepper 的「选品排品」或「话术微调」时，URL 加上 `panel=workspace`，LiveWorkbenchPage 渲染 SessionWorkspacePage。此时用户看到**两层步骤导航**叠加，且步骤定义不一致：

| LiveWorkbenchPage Stepper | SessionWorkspacePage Breadcrumb | 冲突 |
|---------------------------|--------------------------------|------|
| 选品排品（step=1） | 选品排品 | 重复 |
| 话术微调（step=2） | AI生成话术 + 话术微调 | 粒度不同 |
| 开播准备（step=3） | 准备发布 | 名称不同 |
| 数据复盘（step=4） | 数据 | 名称不同 |

### 2.2 修复方案

#### 方案 A（推荐）：SessionWorkspacePage 检测外层 Stepper，隐藏自身面包屑

当 SessionWorkspacePage 被 LiveWorkbenchPage 嵌套渲染时（通过 URL 中存在 `step` 参数判断），隐藏自身的 header 和 breadcrumb 导航，只保留内容区。

**改动文件**：`pages/live/SessionWorkspacePage.tsx`

```typescript
// 检测是否被 LiveWorkbenchPage 嵌套
const isNestedInWorkbench = searchParams.has('step')

// 渲染时：
return (
  <Box>
    {/* 仅独立访问时显示 header + breadcrumb */}
    {!isNestedInWorkbench && (
      <>
        {/* Header bar: title + breadcrumb */}
        <Box sx={{ ... }}>...</Box>
      </>
    )}

    {/* 内容区始终显示 */}
    <Box sx={{ flex: 1, overflow: 'auto' }}>
      {renderTabContent()}
    </Box>
  </Box>
)
```

#### 方案 B：统一步骤定义

合并两页的步骤到 LiveWorkbenchPage 的 5 步 Stepper 中，去掉 SessionWorkspacePage 的独立 Breadcrumb，改为 Stepper 驱动 Tab 切换：

**统一 6 步 Stepper**：
```
[基础信息] → [选品排品] → [AI生成话术] → [话术微调] → [开播准备] → [数据复盘]
     ↓              ↓              ↓              ↓              ↓            ↓
  DetailPage   Workspace      Workspace      Workspace     DetailPage   DetailPage
  tab=info     tab=products   tab=generate   tab=scripts   tab=readiness tab=data
```

LiveWorkbenchPage 的 `FLOW_STEPS` 改为 6 步，`stepToSearchParams` 相应调整。

### 2.3 Stepper 步骤显示「完成」状态优化

当前 `completed` 数组逻辑有问题：

```typescript
// 当前（LiveWorkbenchPage line 106-109）：
const completed = [true, productsCount > 0, scriptsCount > 0, complianceOk, readyOk]
```

- `completed[0] = true`：基础信息永远「完成」，但实际可能未填必填项
- `complianceOk` 和 `readyOk` 来自 readiness API，但可能因 API 失败而始终 false

**优化**：

```typescript
const completed = useMemo(() => [
  Boolean(session?.liveTitle),           // step 0: 有标题即完成
  productsCount > 0,                     // step 1: 有选品
  scriptsCount > 0,                      // step 2: 有话术
  complianceOk || scriptsCount > 0,      // step 3: 合规通过 或 有话术（宽松判断）
  status >= 2,                           // step 4: 已结束才算复盘完成
], [session, productsCount, scriptsCount, complianceOk, status])
```

### 2.4 工作台入口导航优化

当前 LiveSessionPage 卡片视图中，「进入工作台」按钮导航到 `?step=1&panel=workspace&tab=products`，即直接跳到选品步骤。但用户如果已选品完成，应该跳到**下一个未完成步骤**。

**优化**：根据就绪度动态决定跳转步骤：

```typescript
function getNextStep(row: Record<string, unknown>): number {
  const pc = Number(row.productCount ?? 0)
  const sc = Number(row.scriptCount ?? 0)
  if (pc === 0) return 1  // 选品排品
  if (sc === 0) return 2  // 话术微调
  return 3                // 开播准备
}

// 卡片按钮
<Button onClick={() => navigate(`${liveWorkbenchBase}/${row.id}?step=${getNextStep(row)}&panel=workspace`)}>
  进入工作台
</Button>
```

### 2.5 改动文件清单

| 文件 | 改动 | 说明 |
|------|------|------|
| `pages/live/SessionWorkspacePage.tsx` | header 条件隐藏 | 被嵌套时隐藏 breadcrumb |
| `pages/live/LiveWorkbenchPage.tsx` | 步骤优化 | completed 逻辑修正、可选 6 步 |
| `pages/live/LiveSessionPage.tsx` | 入口优化 | 动态 step 跳转 |

---

## 三、全模块逐页审计

### 3.1 直播模块（7 页面）

| 页面 | 问题 | 建议 |
|------|------|------|
| **LiveSessionPage** (827 LOC) | ① 卡片视图的 actions 按钮组按状态分 4 段硬编码（status 0/1/2/3 各写一遍），极冗余；② DataGrid 表格视图同样按状态重复 actions 逻辑；③ 自定义 `LIVE_SESSION_GRID_LOCALE` 与全局 `DATAGRID_ZH_LOCALE` 重复 | ① 提取 `getSessionActions(status, row)` 函数统一两个视图的 actions；② 删除自定义 locale，只用全局的；③ 提取 `renderReadinessSteps` 为独立组件 |
| **LiveWorkbenchPage** (152 LOC) | 双导航冲突（见第二节） | 修复双导航 |
| **SessionWorkspacePage** (~350 LOC) | ① 自身有完整 header+breadcrumb，被嵌套时多余；② `basePath` 硬编码 `/admin/live/sessions` 判断 | ① 条件隐藏 header；② 改用 `useRolePrefix()` |
| **LiveSessionDetailPage** | 正常，Tab 结构清晰 | 无 |
| **LiveRealtimePanelPage** | 正常 | 无 |
| **LiveHistoryComparePage** | 正常 | 无 |
| **LiveScriptRankingPage** | 正常 | 无 |
| **LiveProductPage** | 正常 | 无 |

#### 直播模块侧栏菜单问题

当前侧栏标签是 `场次管理（行内打开工作台）`——这个括号注释不应出现在用户可见的菜单中，应改为简洁的 `场次管理`。

```typescript
// 旧：
{ path: `${prefix}/live/sessions`, label: '场次管理（行内打开工作台）' }
// 新：
{ path: `${prefix}/live/sessions`, label: '场次管理' }
```

### 3.2 短视频模块（21 页面）

| 页面 | 问题 | 建议 |
|------|------|------|
| **ViralLibraryPage** (1,754 LOC) | 远超合理大小，包含详情弹窗、评论列表、BOS 播放器等全部逻辑 | 拆分：① ViralDetailDialog 组件 ② ViralCommentList 组件 ③ ViralVideoPlayer 组件 |
| **DramaEditorPage** (1,165 LOC) | 短剧编辑器包含时间线、场景管理、脚本等全部逻辑 | 拆分各 Tab 为独立组件 |
| **ShortVideoDashboardPage** (908 LOC) | 看板页，多个图表卡片 | 可接受，但建议提取图表卡片为独立组件 |
| **MaterialProductionPage** (842 LOC) | 素材生产流程 | 23+ useState → 提取为 useReducer 或自定义 hook |
| **MaterialLibraryPage** (700+ LOC) | 素材库管理 | 23 个 useState → 提取 `useMaterialLibrary()` hook |
| **VideoEditingPage** (828 LOC) | 视频剪辑 | 可接受（编辑器天然复杂） |
| 其他 15 个页面 | 基本正常 | 无紧急问题 |

### 3.3 商品模块（7 页面）

| 页面 | 问题 | 建议 |
|------|------|------|
| **ProductPage** (780 LOC) | ① 最复杂的 CRUD 页，含编辑抽屉、批量操作、话术生成弹窗等；② Zustand modal store + 本地 useState 混用 | 提取 ProductEditDrawer 为独立组件 |
| **ProductScriptVersionPage** | 自定义 Dialog 而非 FormDialog | 改用 FormDialog |
| **EffectivenessScorePage** (460 LOC) | TabPanel 模式，正常 | 无 |
| **StylePresetPage** | 正常 | 无 |
| **SalesHistoryPage** | 正常，已用 StandardDataGrid | 无 |

### 3.4 AI 模块（22 页面）

| 页面 | 问题 | 建议 |
|------|------|------|
| **KnowledgeBaseListPage** | 使用 `getPrefixFromPathname()` 替代 `useRolePrefix()` | 改用 `useRolePrefix()` |
| **ModelsConfigPage** (552 LOC) | 正常 | 无 |
| **PromptLabPage** (552 LOC) | 正常 | 无 |
| **KnowledgeDocumentsPage** (544 LOC) | 正常 | 无 |
| **EvolutionTasksPage** (524 LOC) | 正常 | 无 |
| 其他 17 个页面 | 基本正常 | 无紧急问题 |

### 3.5 CRUD / 系统页面

| 页面 | 问题 | 建议 |
|------|------|------|
| **RolesPage** | 已迁移到 StandardDataGrid | ✅ |
| **ApiLogPage** | Drawer 宽度 480px 不统一 | 改为 DRAWER_STANDARD (600px) |
| **ConfigPage** | 正常 | 无 |
| **OperationLogPage** | 正常 | 无 |
| **LoginLogsPage** | 正常 | 无 |

### 3.6 文案模块（4 页面）

| 页面 | 问题 | 建议 |
|------|------|------|
| **CopyPage** | Tab 容器页，结构良好 | ✅ |
| **CopyLibraryPage** (501 LOC) | 自定义 Dialog | 改用 FormDialog |
| **CopyTemplatePage** | 已用 FormDialog | ✅ |
| **CopyApprovalPage** | 正常 | 无 |

### 3.7 DashboardPage

| 页面 | 问题 | 建议 |
|------|------|------|
| **DashboardPage** (462 LOC) | ① 5 个 `useEffect` 分别请求 5 个 API，可合并；② `Typography variant="h5"` 在 loading 分支中（line 206）应改用 PageHeader | ① 合并为 1 个 `useEffect` + `Promise.all`；② 统一使用 PageHeader |

---

## 四、组件使用不一致问题汇总

### 4.1 Typography h5 → PageHeader 迁移清单

| 页面文件 | 行号 | 当前 | 改为 |
|---------|------|------|------|
| DashboardPage.tsx | 206 | `<Typography variant="h5">` | `<PageHeader title={...} />` |
| ScriptGenerationPage.tsx | 顶部 | `<h1>` | `<PageHeader title="话术生成" />` |

其他页面已在前几轮迁移，只剩这 2 个。

### 4.2 Dialog 统一

**高优先级**（用原始 Dialog 做表单，应改 FormDialog）：

| 页面 | 当前 | 建议 |
|------|------|------|
| ProductScriptVersionPage | 自定义 Dialog + 表单 | FormDialog |
| CopyLibraryPage | 自定义 Dialog + 表单 | FormDialog |
| MaterialLibraryPage | 自定义 Dialog（批量打标） | FormDialog |
| ProjectManagementPage | 自定义 Dialog（项目表单） | FormDialog |

### 4.3 DataGrid locale 去重

当前至少 3 个页面定义了自己的 locale：
- LiveSessionPage: `LIVE_SESSION_GRID_LOCALE`
- 其他页面: 零散的 `localeText` 对象

**统一**：全部使用 `DATAGRID_ZH_LOCALE`（`utils/datagrid-locale.ts`），删除各页面的自定义 locale。

### 4.4 代码重复：actions 按钮组

LiveSessionPage 中，卡片视图和表格视图的 actions 按钮**完全相同**，但写了两遍（卡片视图 line 636-665，表格视图 line 533-565）。

**解决**：提取 `SessionActions` 组件，两个视图共享：

```typescript
function SessionActions({ row, mode }: { row: Record<string, unknown>; mode: 'card' | 'table' }) {
  const status = Number(row.status ?? 0)
  // 统一 actions 定义...
}
```

---

## 五、实施计划

### 第 1 批 · 导航布局重构（最高优先级）

| 编号 | 项目 | 文件 | 说明 |
|------|------|------|------|
| NAV-01 | primaryBar 支持 topModules | `BaseLayout.tsx` | 启用条件：`useTwoPanel \|\| useTopModules` |
| NAV-02 | primaryBar 渲染 topModules 图标 | `BaseLayout.tsx` | `topModuleIcons` + `shortLabel` 文案 |
| NAV-03 | 无水平模块 Tab | `BaseLayout.tsx` | 不增加 `moduleTabsRow`；模块切换仅在 primaryBar |
| NAV-04 | sidebar 宽度计算 | `BaseLayout.tsx` | `hideSidebar` 时仅 64px；否则折叠规则与双栏一致 |
| NAV-05 | primaryBar 底部主题 + 头像 | `BaseLayout.tsx` | `useTopModules` 时 AppBar 可精简重复入口 |
| NAV-06 | 移动端 | `BaseLayout.tsx` | 延续现有 Drawer；BottomNav 以路由为准 |
| NAV-07 | 侧栏文案 | `adminModuleConfig.tsx` | **已完成**：场次管理 |

### 第 2 批 · 话术工作台修复

| 编号 | 项目 | 文件 | 说明 |
|------|------|------|------|
| WB-01 | SessionWorkspacePage 嵌套检测 | `SessionWorkspacePage.tsx` | 被嵌套时隐藏 header + breadcrumb |
| WB-02 | LiveWorkbenchPage completed 逻辑 | `LiveWorkbenchPage.tsx` L106-109 | 修正完成状态判断 |
| WB-03 | 动态入口步骤 | `LiveSessionPage.tsx` L638 | 根据就绪度跳转到下一未完成步骤 |
| WB-04 | basePath 改用 useRolePrefix | `SessionWorkspacePage.tsx` L89-93 | 删除硬编码 path 判断 |

### 第 3 批 · 代码质量（中优先级）

| 编号 | 项目 | 文件 | 说明 |
|------|------|------|------|
| QA-01 | LiveSessionPage actions 去重 | `LiveSessionPage.tsx` | 提取 SessionActions 组件 |
| QA-02 | 删除自定义 DataGrid locale | `LiveSessionPage.tsx` | 只用全局 DATAGRID_ZH_LOCALE |
| QA-03 | DashboardPage useEffect 合并 | `DashboardPage.tsx` | 5 个 effect 合并为 1 个 |
| QA-04 | DashboardPage h5 → PageHeader | `DashboardPage.tsx` L206 | loading 分支标题 |
| QA-05 | KnowledgeBaseListPage prefix | `KnowledgeBaseListPage.tsx` | 改用 useRolePrefix() |

### 第 4 批 · 大文件拆分（低优先级）

| 编号 | 项目 | 当前 LOC | 目标 |
|------|------|---------|------|
| SPLIT-01 | ViralLibraryPage | **以 `count-large-pages.mjs` 为准** | → 子组件拆分（若仍超阈值） |
| SPLIT-02 | DramaEditorPage | 同上 | → 子组件拆分 |
| SPLIT-03 | MaterialLibraryPage | 同上 | → `useMaterialLibrary` 等 |
| SPLIT-04 | MaterialProductionPage | 同上 | → `useMaterialProduction` 等 |

### 第 5 批 · Dialog 统一（低优先级）

| 编号 | 页面 | 状态 / 说明 |
|------|------|---------------|
| DLG-01 | ProductScriptVersionPage | ✅ 已改为 `FormDialog` |
| DLG-02 | CopyLibraryPage | 保留自定义 `Dialog`（见 §0.1 DLG-02） |
| DLG-03 | MaterialLibraryPage 批量打标 | 不适用（见 §0.1 DLG-03） |
| DLG-04 | ProjectManagementPage 项目表单 | ✅ 已改为 `FormDialog` |
| DLG-05 | ApiLogPage Drawer 宽度 | ✅ 已 600px |

---

## 六、改动文件完整清单

### 必改文件（第 1-2 批）

```
src/layouts/BaseLayout.tsx              — 导航布局重构（核心改动）
src/layouts/adminModuleConfig.tsx        — 菜单标签修正
src/pages/live/SessionWorkspacePage.tsx  — 嵌套检测 + basePath 修正
src/pages/live/LiveWorkbenchPage.tsx     — completed 逻辑修正
src/pages/live/LiveSessionPage.tsx       — 动态步骤入口 + actions 去重
```

### 建议改的文件（第 3-5 批）

```
src/pages/DashboardPage.tsx             — useEffect 合并 + PageHeader
src/pages/ai/KnowledgeBaseListPage.tsx  — useRolePrefix
src/pages/shortvideo/ViralLibraryPage.tsx — 拆分大组件
src/pages/shortvideo/DramaEditorPage.tsx  — 拆分大组件
src/pages/shortvideo/MaterialLibraryPage.tsx — 提取 hook
src/pages/product/ProductScriptVersionPage.tsx — FormDialog（已完成）
src/pages/copy/CopyLibraryPage.tsx       — 主表单保留自定义 Dialog（AI 面板）
src/pages/shortvideo/ProjectManagementPage.tsx — FormDialog（已完成）
src/pages/crud/ApiLogPage.tsx            — Drawer 宽度
```

---

## 附录：目标布局 ASCII 参考

### 桌面端（md+）

```
┌─────┬────────────┬───────────────────────────────────────────┐
│ DY  │            │ ≡  首页 > 直播 > 场次管理    🔔 ⭐ 🌙 👤   │
│     │  直播中心   ├───────────────────────────────────────────┤
│ 🏠  │            │                                           │
│工作台│ ► 场次管理  │                                           │
│     │   实时提词  │              内容区                        │
│ 📺  │   历史对比  │                                           │
│直播 │   话术排行  │                                           │
│     │   节奏优化  │                                           │
│ 🎬  │   直播选品  │                                           │
│短视频│            │                                           │
│     │            │                                           │
│ 📦  │            │                                           │
│商品 │            │                                           │
│     │            │                                           │
│ 🤖  │            │                                           │
│ AI  │            │                                           │
│     │            │                                           │
│─────│            │                                           │
│ 🌙  │            │                                           │
│ 👤  │            │                                           │
└─────┴────────────┴───────────────────────────────────────────┘
 64px    200px                  flex: 1
```

### 侧栏折叠状态

```
┌─────┬───────────────────────────────────────────────────────┐
│ DY  │ ≡  首页 > 直播 > 场次管理              🔔 ⭐ 🌙 👤   │
│     ├───────────────────────────────────────────────────────┤
│ 🏠  │                                                       │
│     │                                                       │
│ 📺  │                    内容区                              │
│     │                                                       │
│ 🎬  │                                                       │
│     │                                                       │
│ 📦  │                                                       │
│     │                                                       │
│ 🤖  │                                                       │
│     │                                                       │
│─────│                                                       │
│ 🌙  │                                                       │
│ 👤  │                                                       │
└─────┴───────────────────────────────────────────────────────┘
 64px                         flex: 1
```

### 移动端（xs）— 不变

```
┌──────────────────────────────────────┐
│ ≡  直播 > 场次管理         🌙 👤    │
├──────────────────────────────────────┤
│                                      │
│              内容区                   │
│                                      │
├──────────────────────────────────────┤
│ 🏠 │ 📺 │ 🎬 │ 📦 │ 🤖 │ ⚙       │  ← BottomNavigation（已有）
└──────────────────────────────────────┘
```
