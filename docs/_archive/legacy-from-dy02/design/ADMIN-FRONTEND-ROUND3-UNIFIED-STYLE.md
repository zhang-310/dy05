# Admin 前端统一风格升级方案（第三轮）

> 2026-03-22 · 基于 Cursor 第二轮升级后的代码审计
> 聚焦：路由清理 / 菜单精简 / 表格统一 / 组件规范 / 视觉一致性

---

## 一、当前状态审计数据

| 维度 | 数量 | 问题 |
|------|------|------|
| 总路由 | 97 条（admin 57 + org 18 + talent 22） | 含 10+ redirect 残留 |
| 侧栏菜单项 | 短视频 24 / 商品 16 / AI 15 / 设置 15 | 短视频和商品仍过多 |
| DataGrid 页面 | 5 个（Product/Sales/Prompt/ExternalApi/LiveSession） | 各自实现 CustomToolbar |
| HTML Table 页面 | 3 个（DataTablePage/CrudTablePage/RolesPage） | 与 DataGrid 风格不统一 |
| 使用 PageHeader | 70 页 | 56 页仍用 raw Typography h5 |
| Dialog 实例 | 98+ 个 | FormDialog(27) vs raw Dialog(71) 混用 |
| Drawer 宽度 | 400/480/550/600px 四种 | 无统一常量 |
| Button variant | contained(244) / outlined(614) / text(596) | 基本一致 |
| Toast | 46 处 useToast | ✅ 完全统一 |
| 表单库 | 无 | 全部手工 useState 控制 |

---

## 二、路由清理方案

### R-01：删除冗余 redirect 路由

当前 `router/index.tsx` 中有大量旧路由 redirect 到新页面：

```tsx
// 这些 redirect 路由应在过渡期后清除
/admin/script/list      → /admin/content/library?tab=script
/admin/script/violation  → /admin/content/library?tab=violation
/admin/script/check      → /admin/content/library?tab=check
/admin/script/template   → /admin/content/library?tab=template
/admin/slangdict         → /admin/content/library?tab=slang
/admin/copy/library      → /admin/copy?tab=library
/admin/copy/approval     → /admin/copy?tab=approval
/admin/copy/template     → /admin/copy?tab=template
/admin/product/sales-history → /admin/product?detail=X&tab=history
```

**方案**：保留 redirect（SEO/书签兼容），但从菜单配置中彻底删除旧路径引用。

### R-02：侧栏菜单中的路径去重

`adminModuleConfig.tsx` 中商品模块侧栏仍保留已 redirect 的旧路径：

```typescript
// 当前：商品模块侧栏「内容库·话术与文案」组
children: [
  { path: '/admin/content/library', label: '内容库（统一）' },
  { path: '/admin/script/list', label: '话术库' },       // redirect
  { path: '/admin/script/template', label: '话术模板' },  // redirect
  { path: '/admin/copy', label: '文案库' },               // redirect
  { path: '/admin/slangdict', label: '话术梗库' },         // redirect
]
```

5 个菜单项，点击任意一个都跳到同一个页面。

**方案**：只保留 1 个入口：

```typescript
children: [
  { path: '/admin/content/library', label: '内容库' },
]
```

### R-03：商品模块路由扁平化

7 个独立页面中有 5 个应合并到商品详情抽屉的 Tab 中（PM1-A 方案）。合并后路由变化：

| 旧路由 | 处理 | 新访问方式 |
|--------|------|-----------|
| `/admin/product` | 保留 | 商品列表主页 |
| `/admin/product/readiness` | 保留 | 就绪度独立视图 |
| `/admin/product/sales-history` | 删除 | → 商品详情抽屉「历史」Tab |
| `/admin/product/style-preset` | 删除 | → 商品详情抽屉「话术」Tab 内下拉 |
| `/admin/product/effectiveness` | 删除 | → 商品详情抽屉「效果」Tab |
| `/admin/product/script-versions` | 删除 | → 商品详情抽屉「话术」Tab |
| `/admin/product/script-optimization` | 删除 | → 商品详情抽屉「话术」Tab |

**改动文件**：
- `router/index.tsx` — 删除 5 条路由，加 redirect 兼容
- `adminModuleConfig.tsx` — 商品组从 7 项改为 2 项

---

## 三、侧栏菜单精简方案

### M-01：短视频侧栏（24 → 6 常显 + 折叠）

**当前**：3 组 24 项全部展开显示。

**目标**：

```
核心入口（常显 6 项）
├── 我的项目
├── 快速生成
├── 爆款库
├── 素材库
├── 内容日历
├── 数据分析

更多工具 ▼（默认折叠，14 项）
├── 短视频看板 / 项目工作台
├── 脚本策划 / 分镜设计 / 素材准备 / 素材生产 / 拍摄任务
├── 短剧编辑器 / 视频剪辑 / 审核发布 / 一键日更 / 质量仪表板
├── 竞品监控 / 效果预测 / 工作流编辑器
├── 视频管理 / 分类管理 / 视频统计
```

**实现**：`MenuItemDef` 新增 `defaultCollapsed?: boolean`，BaseLayout 渲染时对 `defaultCollapsed: true` 的组显示为折叠状态。

### M-02：商品侧栏（16 → 4 常显）

**目标**：

```
商品库（常显）
├── 商品列表
├── 商品就绪度

内容库（常显）
├── 内容库（统一入口）

抖音
├── 账号管理

合规 ▼（默认折叠）
├── 违规词 / 违规检测
├── 人设管理
```

### M-03：AI 运维组（8 → 4）

**当前**：AI 运维组 8 项平铺。

**目标**：合并为 4 项（每项内部 Tab）：

```
AI 运维
├── 模型管理     ← 模型配置 + 模型基准（Tab）
├── 监控与日志   ← 监控中心 + 调用日志 + 基础设施（Tab）
├── Prompt 工具  ← Prompt 实验室 + Prompt 模板（Tab）
├── 诊断中心
```

**改动文件**：
- `adminModuleConfig.tsx` — 三个模块的菜单重组
- `BaseLayout.tsx` — 支持 `defaultCollapsed` 渲染
- 新建 `pages/ai/AiModelManagementPage.tsx` — Tab 页
- 新建 `pages/ai/AiOpsMonitorPage.tsx` — Tab 页
- 新建 `pages/ai/PromptToolsPage.tsx` — Tab 页

---

## 四、表格统一方案

### 现状：两套表格系统并存

| 系统 | 技术 | 使用页面 | 特征 |
|------|------|---------|------|
| **DataGrid 系** | `@mui/x-data-grid` | ProductPage, SalesHistoryPage, PromptLabPage, ExternalApiConfigPage, LiveSessionPage | 服务端分页、自定义 Toolbar、列排序/筛选 |
| **HTML Table 系** | `<Table>` + `<TablePagination>` | DataTablePage, CrudTablePage, RolesPage + 30 个动态表 | 客户端分页、内联搜索 |

### T-01：统一为 DataGrid + 标准 Toolbar

**原则**：所有列表页统一使用 MUI X DataGrid，废弃 HTML Table 方案。

**标准 Toolbar 组件**：

新建 `components/base/StandardToolbar.tsx`：

```typescript
interface StandardToolbarProps {
  // 通用
  title?: string                    // 表格标题（可选）
  total?: number                    // 总条目数 Chip
  onRefresh?: () => void            // 刷新按钮

  // 搜索
  keyword?: string                  // 搜索关键词
  onKeywordChange?: (v: string) => void
  onSearch?: () => void
  searchPlaceholder?: string

  // 筛选（slot 插槽）
  filters?: ReactNode               // 自定义筛选区域

  // 操作按钮
  primaryAction?: { label: string; icon?: ReactNode; onClick: () => void }  // 主按钮（contained）
  secondaryActions?: { label: string; icon?: ReactNode; onClick: () => void }[]  // 次按钮组（outlined）

  // DataGrid 内置
  showColumnsButton?: boolean       // 列选择器
  showDensitySelector?: boolean     // 密度选择器
  showExportButton?: boolean        // 导出
}
```

**布局**：

```
┌──────────────────────────────────────────────────────────────┐
│ [🔍 搜索框] [筛选区 slot]          [+ 新增] [操作▼] │ ≡ □ ↓  │
│                                                    │分隔│内置│
└──────────────────────────────────────────────────────────────┘
```

- 左侧：搜索 + 筛选 slot
- 右侧：主按钮 + 次按钮组 + 分隔线 + DataGrid 内置工具
- 总数 Chip 紧跟搜索框后

**标准 DataGrid 配置**：

新建 `components/base/StandardDataGrid.tsx`：

```typescript
interface StandardDataGridProps<T> {
  rows: T[]
  columns: GridColDef<T>[]
  loading?: boolean
  rowCount: number

  // 分页（统一服务端）
  paginationModel: GridPaginationModel
  onPaginationModelChange: (m: GridPaginationModel) => void
  pageSizeOptions?: number[]  // 默认 [10, 20, 50]

  // Toolbar
  toolbarProps?: StandardToolbarProps

  // 可选功能
  checkboxSelection?: boolean
  onRowClick?: (row: T) => void
  getRowId?: (row: T) => string | number

  // 本地化
  // 内置中文 LOCALE_TEXT，无需每页重复定义
}
```

内置功能：
- 中文本地化文本（`localeText` 统一定义，不再每页写 `LOCALE_TEXT`）
- 默认 `disableRowSelectionOnClick`
- 默认 `autoHeight` + `minHeight: 400`
- 默认 `pageSizeOptions: [10, 20, 50]`
- 自动注入 `StandardToolbar`

**迁移清单**：

| 页面 | 当前方案 | 改为 | 工作量 |
|------|---------|------|--------|
| DataTablePage (30+ 动态表) | HTML Table | StandardDataGrid | 中（重构核心） |
| CrudTablePage | HTML Table (via DataTablePage) | StandardDataGrid | 低（随 DataTablePage） |
| RolesPage | HTML Table | StandardDataGrid | 低 |
| ProductPage | 自定义 DataGrid + Toolbar | StandardDataGrid | 低（适配 Toolbar） |
| SalesHistoryPage | 自定义 DataGrid + Toolbar | StandardDataGrid | 低 |
| PromptLabPage | 自定义 DataGrid + Toolbar | StandardDataGrid | 低 |
| ExternalApiConfigPage | 自定义 DataGrid + Toolbar | StandardDataGrid | 低 |
| LiveSessionPage | 自定义 DataGrid + Toolbar | StandardDataGrid | 低 |

**改动文件**：
- 新建 `components/base/StandardToolbar.tsx`
- 新建 `components/base/StandardDataGrid.tsx`
- 新建 `utils/datagrid-locale.ts` — 中文本地化常量
- `components/base/index.ts` — 导出新组件
- 逐页迁移（8 个文件 + DataTablePage 影响 30+ 动态表）

### T-02：统一行操作模式

**当前**：行操作有两种模式混用：

```
模式 A（内联图标）：
[EditIcon] [DeleteIcon] [ViewIcon]    ← ProductPage, LiveSessionPage

模式 B（操作菜单）：
[更多 ▼]
├── 编辑
├── 删除
├── 克隆                              ← 部分页面
```

**统一规则**：

| 操作数 | 方案 | 示例 |
|--------|------|------|
| ≤ 3 个 | 内联图标 | 编辑 · 删除 · 查看 |
| > 3 个 | 前 2 图标 + 更多菜单 | 编辑 · 删除 · [更多▼: 克隆/导出/...] |

**标准行操作组件**：

新建 `components/base/RowActions.tsx`：

```typescript
interface RowAction {
  key: string
  icon: ReactNode
  label: string
  onClick: () => void
  color?: 'default' | 'primary' | 'error'
  confirm?: string        // 需要确认时的提示文本
  disabled?: boolean
}

interface RowActionsProps {
  actions: RowAction[]
  inlineMax?: number      // 内联显示的最大数量，默认 3
}
```

**改动文件**：
- 新建 `components/base/RowActions.tsx`
- 各 DataGrid 页面统一使用

### T-03：统一单元格渲染器

**当前**：各页面重复实现相同的 renderCell 逻辑：

| 渲染类型 | 重复次数 | 页面 |
|---------|---------|------|
| 状态 Chip | 8+ 次 | Product/Live/Prompt/ExternalApi/... |
| 金额格式化 | 3+ 次 | Product/Sales/KPI |
| 日期格式化 | 5+ 次 | 各列表页 |
| 布尔开关 | 3+ 次 | Prompt/ExternalApi/Product |
| 图片缩略图 | 2+ 次 | Product/Material |
| 进度条 | 2+ 次 | Live/Product readiness |

**方案**：新建 `utils/cell-renderers.tsx`：

```typescript
// 状态 Chip
export function renderStatusChip(statusMap: Record<string, { label: string; color: ChipProps['color'] }>): GridRenderCellParams => JSX
// 例：renderStatusChip({ active: { label: '启用', color: 'success' }, inactive: { label: '停用', color: 'default' } })

// 金额
export function renderCurrency(params: GridRenderCellParams): JSX
// 输出：¥1,234.56

// 日期
export function renderDateTime(params: GridRenderCellParams): JSX
// 输出：2026-03-22 15:30

// 布尔
export function renderBooleanChip(trueLabel?: string, falseLabel?: string): GridRenderCellParams => JSX

// 缩略图
export function renderThumbnail(fallbackIcon?: ReactNode): GridRenderCellParams => JSX

// 进度条
export function renderProgress(params: GridRenderCellParams): JSX
```

**改动文件**：
- 新建 `utils/cell-renderers.tsx`
- 各 DataGrid 页面的 columns 定义引用共享渲染器

---

## 五、组件规范统一方案

### C-01：PageHeader 全面覆盖

**现状**：70 页用 PageHeader，56 页用 raw Typography h5。

**规则**：
- 所有页面**必须**使用 `<PageHeader>` 作为标题
- 禁止在页面顶部直接使用 `<Typography variant="h5">`

**重点迁移页面**（行数多、影响大）：

| 页面 | 当前 h5 实例数 | 说明 |
|------|--------------|------|
| ViralLibraryPage | 26 | 多数是弹窗内标题，可保留；页面主标题需改 |
| DramaEditorPage | 23 | 编辑器区块标题 |
| ShortVideoDashboardPage | 24 | 各卡片标题 |
| UnifiedKpiPage | 13 | 各图表区标题 |
| DashboardPage | 2 | 页面主标题 |

**注意**：弹窗/卡片内部的 h5 标题可以保留（它们不是页面主标题），只需将页面**最顶部**的标题改为 PageHeader。

**改动文件**：
- 约 15-20 个页面文件的首行标题改为 PageHeader

### C-02：Dialog/Drawer 尺寸统一

**已有** `theme/dialogSizes.ts`，但使用率低。

**强制规范**：

| 场景 | 尺寸常量 | 值 |
|------|---------|-----|
| 确认弹窗 | `DIALOG_XS` | maxWidth="xs" (444px) |
| 表单弹窗 | `DIALOG_SM` | maxWidth="sm" (600px) |
| 复杂弹窗 | `DIALOG_MD` | maxWidth="md" (900px) |
| 编辑抽屉 | `DRAWER_STANDARD` | 600px (xs:100%) |
| 详情抽屉 | `DRAWER_DETAIL` | 640px (xs:100%) |

**当前不一致**：

| 文件 | 当前宽度 | 应改为 |
|------|---------|--------|
| ExternalApiConfigPage Drawer | 550px 固定 | DRAWER_STANDARD (600px) |
| ApiLogPage Drawer | 480px | DRAWER_STANDARD (600px) |
| SyncLogPage Drawer | 400px | DRAWER_STANDARD (600px) |
| ProductEditDrawer (live) | 400px | DRAWER_STANDARD (600px) |
| KbRefPreviewPopover | 400px + maxHeight 480 | 保留（Popover 非 Drawer） |

**改动文件**：
- 4 个 Drawer 组件统一宽度
- 各 Dialog 组件改用 `dialogSizes` 常量

### C-03：Loading 状态标准化

**规则**：

| 场景 | 组件 | 示例 |
|------|------|------|
| 整页加载 | `<PageSkeleton variant="card" count={N} />` | 页面首次加载 |
| 表格加载 | DataGrid `loading={true}`（内置） | 表格数据刷新 |
| 按钮操作 | `<CircularProgress size={20} />`（内联） | 提交/生成按钮 |
| 区域加载 | `<Skeleton variant="rounded" height={H} />` | 卡片/图表加载 |
| 列表项加载 | `<Skeleton variant="text" />` × N | 列表占位 |

**禁止**：
- 居中的裸 `<CircularProgress />` 替代整页加载（应用 PageSkeleton）
- 无 loading 状态的 API 请求（必须有加载反馈）

### C-04：Empty 状态标准化

**已有** `EmptyState` 组件，使用率良好（30+ 页）。

**补充规则**：
- DataGrid 的空状态用 `slots.noRowsOverlay` 自定义
- 标准化 EmptyState 的三个场景文案：
  - 无数据：「暂无数据，点击上方按钮新增」
  - 搜索无结果：「未找到匹配结果，请调整筛选条件」
  - 加载失败：「数据加载失败」+ 重试按钮

**新建**：

```typescript
// components/base/DataGridEmptyOverlay.tsx
export function DataGridEmptyOverlay({ message, actionLabel, onAction }: Props) {
  return <EmptyState title={message} action={actionLabel ? <Button onClick={onAction}>{actionLabel}</Button> : undefined} />
}
```

### C-05：表单模式标准化

**当前问题**：441 个 TextField，全部手工 `useState` + `onChange`。无验证库。

**短期方案**（不引入新库）：统一表单验证 helper。

新建 `utils/form-helpers.ts`：

```typescript
// 必填验证
export function required(value: string | null | undefined): string | null
// 返回 null 表示通过，返回 string 表示错误消息

// 长度验证
export function maxLength(max: number): (value: string) => string | null

// 数字范围
export function numberRange(min: number, max: number): (value: string) => string | null

// 表单状态 hook
export function useFormFields<T extends Record<string, unknown>>(initial: T) {
  // 返回 { values, errors, setField, validate, reset, isDirty }
}
```

**长期方案**：引入 `react-hook-form`（建议 Q2 排期，改动面大）。

---

## 六、视觉一致性方案

### V-01：语义色彩全面应用

**已有** `theme/semanticColors.ts`，但仅 DashboardPage 使用。

**应用清单**：

| 场景 | 语义 | 颜色 | 应用文件 |
|------|------|------|---------|
| GMV / 销售额 | `revenue` | success.main（绿） | Dashboard, KPI, SalesHistory |
| 数量 / 场次 | `count` | primary.main（蓝） | Dashboard, LiveSession, Product |
| 告警 / 直播中 | `alert` | warning.main（橙） | Dashboard, LiveSession, 合规 |
| AI / 知识 | `ai` | secondary.main（紫） | AI 模块, Agent, 知识库 |
| 系统 / 日志 | `system` | grey.600 | 设置模块, 日志, 监控 |

**改动**：各页面的 Chip / StatCard / 图表配色统一引用语义色彩常量。

### V-02：间距节奏标准化

**定义标准间距层级**（与 MUI theme.spacing 对齐）：

```typescript
// theme/spacingScale.ts
export const SPACING = {
  SECTION_GAP: 3,        // 大区块之间 (24px)
  CARD_GAP: 2,           // 卡片之间 (16px)
  INNER_GAP: 1.5,        // 卡片内部元素 (12px)
  ELEMENT_GAP: 1,        // 紧凑元素 (8px)
  TIGHT_GAP: 0.5,        // 极紧凑 (4px)
} as const
```

**应用规则**：

| 位置 | 间距 | 说明 |
|------|------|------|
| 页面标题下方 | `mb: SECTION_GAP` (3) | PageHeader 底部 |
| 区块之间 | `mt: SECTION_GAP` (3) | Card 与 Card |
| Grid 容器 | `spacing={CARD_GAP}` (2) | Grid 列间距 |
| 卡片内部 | `p: CARD_GAP` (2) 或 `p: INNER_GAP` (1.5) | CardContent |
| 表单字段 | `gap: INNER_GAP` (1.5) | TextField 之间 |
| Chip 组 | `gap: ELEMENT_GAP` (1) | Chip 之间 |
| 图标与文字 | `gap: TIGHT_GAP` (0.5) | IconButton label |

### V-03：状态 Chip 统一

**当前**：各页面自行定义状态映射，颜色不一致。

**标准状态映射**：

```typescript
// utils/status-chips.ts
export const COMMON_STATUS = {
  active:    { label: '启用',   color: 'success' as const },
  inactive:  { label: '停用',   color: 'default' as const },
  pending:   { label: '待处理', color: 'warning' as const },
  error:     { label: '异常',   color: 'error' as const },
  draft:     { label: '草稿',   color: 'default' as const },
  published: { label: '已发布', color: 'success' as const },
  archived:  { label: '已归档', color: 'default' as const },
} as const

export const LIVE_STATUS = {
  prepare:   { label: '准备中', color: 'default' as const },
  live:      { label: '直播中', color: 'error' as const },
  ended:     { label: '已结束', color: 'default' as const },
  cancelled: { label: '已取消', color: 'warning' as const },
} as const
```

---

## 七、实施路线图

### 第 1 批 · 基础设施（新建共享组件）

| 编号 | 项目 | 文件 | 说明 |
|------|------|------|------|
| T-01a | StandardToolbar | `components/base/StandardToolbar.tsx` | 统一工具栏 |
| T-01b | StandardDataGrid | `components/base/StandardDataGrid.tsx` | 统一 DataGrid 封装 |
| T-01c | DataGrid 本地化 | `utils/datagrid-locale.ts` | 中文文本 |
| T-02 | RowActions | `components/base/RowActions.tsx` | 统一行操作 |
| T-03 | Cell Renderers | `utils/cell-renderers.tsx` | 共享单元格渲染器 |
| C-04 | DataGridEmptyOverlay | `components/base/DataGridEmptyOverlay.tsx` | 空状态 |
| C-05 | Form Helpers | `utils/form-helpers.ts` | 表单验证 |
| V-01 | 状态 Chip 映射 | `utils/status-chips.ts` | 统一状态 |
| V-02 | 间距常量 | `theme/spacingScale.ts` | 统一间距 |

### 第 2 批 · 菜单与路由清理

| 编号 | 项目 | 文件 | 说明 |
|------|------|------|------|
| R-02 | 商品侧栏路径去重 | `adminModuleConfig.tsx` | 5 项→1 项 |
| M-01 | 短视频侧栏折叠 | `adminModuleConfig.tsx` + `BaseLayout.tsx` | 24→6+折叠 |
| M-02 | 商品侧栏精简 | `adminModuleConfig.tsx` | 16→4 |
| M-03 | AI 运维合并 | `adminModuleConfig.tsx` + 3 个新 Tab 页 | 8→4 |
| R-03 | 商品路由扁平化 | `router/index.tsx` | 删 5 条路由 |

### 第 3 批 · 页面迁移（优先级排序）

**高优先级**（高频使用页面）：

| 页面 | 改动 | 说明 |
|------|------|------|
| ProductPage | 适配 StandardDataGrid + StandardToolbar | 最复杂的 DataGrid 页 |
| LiveSessionPage | 适配 StandardDataGrid + StandardToolbar | 核心业务页 |
| DataTablePage | 从 HTML Table 改为 StandardDataGrid | 影响 30+ 动态表 |

**中优先级**：

| 页面 | 改动 |
|------|------|
| SalesHistoryPage | 适配 StandardDataGrid |
| PromptLabPage | 适配 StandardDataGrid |
| ExternalApiConfigPage | 适配 StandardDataGrid |
| RolesPage | 从 HTML Table 改为 StandardDataGrid |
| CrudTablePage | 随 DataTablePage 自动适配 |

**低优先级**（PageHeader 迁移）：

| 页面 | 改动 |
|------|------|
| 15-20 个页面 | 首行标题改为 PageHeader |

### 第 4 批 · Drawer 统一 + 商品抽屉

| 编号 | 项目 | 文件 | 说明 |
|------|------|------|------|
| C-02 | Drawer 宽度统一 | 4 个 Drawer 组件 | 统一为 DRAWER_STANDARD |
| PM1-A | 商品详情多 Tab 抽屉 | 新建 ProductDetailDrawer + 4 Tab | 7 页→2 页 |

---

## 八、文件清单汇总

### 需新建（14 个文件）

```
src/components/base/StandardToolbar.tsx      — 标准工具栏
src/components/base/StandardDataGrid.tsx      — 标准 DataGrid
src/components/base/RowActions.tsx            — 行操作组件
src/components/base/DataGridEmptyOverlay.tsx  — 空状态覆盖
src/utils/datagrid-locale.ts                 — DataGrid 中文
src/utils/cell-renderers.tsx                 — 共享单元格渲染器
src/utils/status-chips.ts                    — 状态 Chip 映射
src/utils/form-helpers.ts                    — 表单验证 helper
src/theme/spacingScale.ts                    — 间距常量
src/pages/ai/AiModelManagementPage.tsx       — AI 模型管理（Tab 合并）
src/pages/ai/AiOpsMonitorPage.tsx            — AI 监控（Tab 合并）
src/pages/ai/PromptToolsPage.tsx             — Prompt 工具（Tab 合并）
src/pages/product/ProductDetailDrawer.tsx    — 商品详情抽屉
src/pages/product/tabs/*                     — 4 个 Tab 组件
```

### 需修改（核心文件）

```
src/layouts/adminModuleConfig.tsx   — 侧栏菜单重组（3 模块精简）
src/layouts/BaseLayout.tsx          — 支持 defaultCollapsed
src/router/index.tsx                — 路由清理 + redirect
src/components/base/index.ts       — 导出新组件
src/pages/DataTablePage.tsx         — 迁移到 StandardDataGrid
src/pages/crud/ProductPage.tsx      — 适配 StandardDataGrid
src/pages/live/LiveSessionPage.tsx  — 适配 StandardDataGrid
src/pages/crud/RolesPage.tsx        — 迁移到 StandardDataGrid
src/pages/crud/SalesHistoryPage.tsx — 适配 StandardDataGrid
src/pages/ai/PromptLabPage.tsx      — 适配 StandardDataGrid
src/pages/system/ExternalApiConfigPage.tsx — 适配 + Drawer 宽度
```

### 可删除（合并后）

```
src/pages/product/EffectivenessScorePage.tsx      → 商品抽屉「效果」Tab
src/pages/product/ProductScriptVersionPage.tsx     → 商品抽屉「话术」Tab
src/pages/product/StylePresetPage.tsx              → 商品抽屉「话术」Tab
src/pages/product/ScriptOptimizationPage.tsx       → 商品抽屉「话术」Tab
src/pages/crud/SalesHistoryPage.tsx                → 商品抽屉「历史」Tab
```

---

## 附录：统一后的组件使用示例

### 标准列表页模板

```tsx
import { StandardDataGrid, StandardToolbar, RowActions, PageHeader } from '@/components/base'
import { renderStatusChip, renderDateTime, renderCurrency } from '@/utils/cell-renderers'
import { COMMON_STATUS } from '@/utils/status-chips'
import { SPACING } from '@/theme/spacingScale'

export function ExampleListPage() {
  const columns: GridColDef[] = [
    { field: 'name', headerName: '名称', flex: 1, minWidth: 150 },
    { field: 'status', headerName: '状态', width: 100, renderCell: renderStatusChip(COMMON_STATUS) },
    { field: 'amount', headerName: '金额', width: 120, renderCell: renderCurrency },
    { field: 'createdAt', headerName: '创建时间', width: 160, renderCell: renderDateTime },
    {
      field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: (params) => (
        <RowActions actions={[
          { key: 'edit', icon: <EditIcon />, label: '编辑', onClick: () => handleEdit(params.row) },
          { key: 'delete', icon: <DeleteIcon />, label: '删除', onClick: () => handleDelete(params.row), color: 'error', confirm: '确定删除？' },
        ]} />
      ),
    },
  ]

  return (
    <Box>
      <PageHeader title="示例列表" actions={<Button variant="contained" startIcon={<AddIcon />}>新增</Button>} />
      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        rowCount={total}
        paginationModel={paginationModel}
        onPaginationModelChange={setPaginationModel}
        toolbarProps={{
          keyword, onKeywordChange: setKeyword, onSearch: handleSearch,
          total,
          onRefresh: handleRefresh,
          filters: <StatusFilter value={status} onChange={setStatus} />,
        }}
      />
    </Box>
  )
}
```

---

## 附录 · R-03 路由收尾（2026-03-22）

- 商品子路径 `product/sales-history`、`product/style-preset`、`product/effectiveness`、`product/script-versions` 已合并为 **`product/*` 单一 legacy 重定向**：带 `productId` 时跳转至 `/admin/product?detail=&tab=`；无 `productId` 时回退商品列表。
- **`product/script-optimization`** 仍为独立路由（需 `scriptId` / `scriptVersionId` 深链至话术优化页），与抽屉 Tab 并存。
- 侧栏与快捷链接已改为 **`?detail=&tab=`** 深链；机构/达人侧栏已去掉与「商品库」重复的「话术版本库」入口。

### 附录 · 表格迁移二期（复杂页）

以下页面仍含 HTML `Table` 或与图表/嵌套 Tab 强耦合，迁移 `StandardDataGrid` 需单独评估：**UnifiedKpiPage**、**KnowledgeDocumentsPage**、**ModelsConfigPage**、短视频素材/管理多页、直播场次子 Tab 等。
