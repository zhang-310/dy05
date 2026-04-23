# LiveSessionPage 退化修复 + 全面质量恢复方案

> 2026-03-22 · Cursor 升级导致场次管理页严重退化的诊断与修复
> 优先级：**紧急** — 核心业务功能丢失

---

## 一、诊断结果：LiveSessionPage 被 Cursor 退化

Cursor 在升级过程中将 `LiveSessionPage.tsx` 回退为一个**原始版本**，丢失了大量已实现的功能。

### 1.1 丢失功能清单

| # | 丢失功能 | 原有实现 | 当前状态 |
|---|---------|---------|---------|
| 1 | **「进入工作台」按钮** | `navigate(liveWorkbenchBase/${id}?step=1&panel=workspace)` | ❌ 完全丢失，无法从列表进入工作台 |
| 2 | **「克隆」按钮** | `cloneLiveSession(id)` + toast 反馈 | ❌ 完全丢失，无克隆功能 |
| 3 | **StandardDataGrid 表格** | MUI X DataGrid + 服务端分页 + 自定义 Toolbar | ❌ 退化为 HTML `<Table>` + `<TablePagination>` |
| 4 | **卡片视图状态分组** | 「直播中」「准备中/即将开播」「已结束/已取消（可折叠）」三组 | ❌ 退化为平铺无分组 |
| 5 | **就绪度迷你步骤** | `renderReadinessSteps` 显示 ✅选品 ✅话术 ○就绪 | ❌ 退化为纯文本「产品：N个 \| 话术：N条」 |
| 6 | **SessionActions 共享组件** | `SessionActions.tsx` 卡片/表格共用 | ❌ 未使用，每个视图硬编码 actions |
| 7 | **sessionWorkbenchNav 动态跳转** | `getNextWorkbenchStep` 根据就绪度决定步骤 | ❌ 未使用 |
| 8 | **直播中脉冲动画** | 直播中状态 Chip 带 `FiberManualRecordIcon` 脉冲动效 | ❌ 退化为普通 Chip |
| 9 | **状态快捷统计** | Toolbar 显示各状态数量 Chip | ❌ 丢失 |
| 10 | **ConfirmDialog** | 使用共享 `ConfirmDialog` 组件 | ❌ 退化为原始 `<Dialog>` |
| 11 | **DATAGRID_ZH_LOCALE** | 使用全局中文本地化 | ❌ 退化（无 DataGrid） |
| 12 | **SPACING 常量** | 使用 `SPACING.CARD_GAP` 等标准间距 | ❌ 退化为硬编码数字 |
| 13 | **时间提示** | `getTimeHint` 显示「5分钟后开播」等 | ❌ 丢失 |
| 14 | **状态筛选 Chip 组** | 准备中(N) / 直播中(N) / 已结束(N) 快速筛选 | ❌ 退化为普通 Select |

### 1.2 已存在但未被使用的组件

Cursor 创建了以下正确的组件/工具函数，但 **LiveSessionPage 完全没用上**：

| 文件 | 功能 | 状态 |
|------|------|------|
| `pages/live/components/SessionActions.tsx` (259行) | 卡片/表格/紧凑三种变体的操作按钮组，含工作台/克隆/编辑/删除 | ✅ 已创建，❌ 未被 LiveSessionPage 引用 |
| `pages/live/sessionWorkbenchNav.ts` (17行) | `getNextWorkbenchStep` + `sessionWorkbenchHref` 动态跳转 | ✅ 已创建，❌ 未被 LiveSessionPage 引用 |
| `api/live-session.ts` 的 `cloneLiveSession` | 克隆场次 API | ✅ 存在，❌ 未被 LiveSessionPage 导入 |

---

## 二、修复方案

### FIX-01：LiveSessionPage 完整重写（最高优先级）

**目标**：恢复所有丢失功能，使用已有共享组件。

**核心改动**：

#### A. 导入恢复

```typescript
// 当前缺失的导入——需要加回
import { ConfirmDialog, PageHeader, PageSkeleton, StandardDataGrid } from '@/components/base'
import { DATAGRID_ZH_LOCALE } from '@/utils/datagrid-locale'
import { SPACING } from '@/theme/spacingScale'
import { cloneLiveSession } from '@/api/live-session'   // 或 '@/api/live' 看实际导出位置
import { SessionActions } from './components/SessionActions'
import { sessionWorkbenchHref } from './sessionWorkbenchNav'
// 以及状态分组/动画/时间提示等所需图标
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import FilterListIcon from '@mui/icons-material/FilterList'
import EventNoteIcon from '@mui/icons-material/EventNote'
import { Collapse } from '@mui/material'
```

#### B. 克隆功能恢复

```typescript
const handleClone = async (row: Record<string, unknown>) => {
  try {
    const newId = await cloneLiveSession(row.id as number)
    toast('场次已克隆', 'success')
    loadData()
    // 可选：直接跳转到新场次
    // navigate(`${sessionsBase}/${newId}`)
  } catch (e) {
    toast(e instanceof Error ? e.message : '克隆失败', 'error')
  }
}
```

#### C. 工作台导航变量恢复

```typescript
const liveWorkbenchBase = pathname.replace(/\/live\/sessions\/?$/, '/live/workbench')
```

#### D. 表格改回 StandardDataGrid

从 HTML `<Table>` 回到 `<StandardDataGrid>`，使用 `DATAGRID_ZH_LOCALE`，服务端分页。

#### E. 卡片视图恢复状态分组

```typescript
const groupedSessionCards = useMemo(() => {
  const live = data.filter((r) => Number(r.status) === 1)
  const prep = data.filter((r) => Number(r.status) === 0)
  const ended = data.filter((r) => Number(r.status) >= 2)
  return { live, prep, ended }
}, [data])
```

分三个区块渲染，「已结束」默认折叠（`Collapse`）。

#### F. 使用 SessionActions 组件

卡片视图和 DataGrid 操作列统一使用 `SessionActions`：

```typescript
// 卡片视图
<SessionActions
  row={row}
  variant="card"
  sessionsBase={sessionsBase}
  liveWorkbenchBase={liveWorkbenchBase}
  navigate={navigate}
  onClone={handleClone}
  onEdit={handleEdit}
  onDeleteClick={handleDeleteClick}
/>

// DataGrid 操作列
renderCell: (params) => (
  <SessionActions
    row={params.row}
    variant={isNarrow ? 'toolbarNarrow' : 'toolbar'}
    sessionsBase={sessionsBase}
    liveWorkbenchBase={liveWorkbenchBase}
    navigate={navigate}
    onClone={handleClone}
    onEdit={handleEdit}
    onDeleteClick={handleDeleteClick}
  />
)
```

#### G. 就绪度迷你步骤恢复

```typescript
function ReadinessSteps({ row }: { row: Record<string, unknown> }) {
  const pc = Number(row.productCount ?? 0)
  const sc = Number(row.scriptCount ?? 0)
  const progress = pc === 0 ? 0 : sc === 0 ? 1 : 2
  const steps = ['选品', '话术', '就绪']
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
      {steps.map((label, i) => (
        <Box key={label} sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
          {i > 0 && <Box sx={{ width: 12, height: 1, bgcolor: i <= progress ? 'success.main' : 'divider' }} />}
          {i < progress
            ? <CheckCircleOutlineIcon sx={{ fontSize: 14, color: 'success.main' }} />
            : <RadioButtonUncheckedIcon sx={{ fontSize: 14, color: i === progress ? 'primary.main' : 'text.disabled' }} />}
          <Typography variant="caption" color={i < progress ? 'success.main' : i === progress ? 'primary.main' : 'text.disabled'} sx={{ fontWeight: i === progress ? 600 : 400 }}>
            {label}
          </Typography>
        </Box>
      ))}
    </Box>
  )
}
```

#### H. 直播中脉冲动画恢复

```typescript
{status === 1 ? (
  <Chip
    label={statusInfo.label}
    size="small"
    color="success"
    icon={<FiberManualRecordIcon sx={{
      fontSize: '10px !important',
      '@keyframes pulse': { '0%, 100%': { opacity: 1 }, '50%': { opacity: 0.4 } },
      animation: 'pulse 1.5s ease-in-out infinite',
    }} />}
  />
) : (
  <Chip label={statusInfo.label} size="small" color={statusInfo.color} variant="outlined" />
)}
```

#### I. ConfirmDialog 替换原始 Dialog

```typescript
// 当前的原始 Dialog → 改为
<ConfirmDialog
  open={!!deleteConfirm}
  title="确认删除"
  message={deleteConfirm ? `确定删除场次「${String(deleteConfirm.liveTitle)}」？` : ''}
  confirmText="删除"
  confirmColor="error"
  onConfirm={handleDeleteConfirm}
  onCancel={() => setDeleteConfirm(null)}
/>
```

#### J. SPACING 常量替换硬编码

```typescript
// 当前 gap: 2 → 改为
gap: SPACING.CARD_GAP

// 当前 mb: 2 → 改为
mb: SPACING.CARD_GAP
```

### 改动文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `pages/live/LiveSessionPage.tsx` | **重写** | 恢复全部 14 项丢失功能 |

---

## 三、其他需同步修复的回退问题

Cursor 升级中可能还有其他页面被类似退化。以下是需要逐一核对的高风险页面：

### 3.1 需要检查的页面（可能被退化）

| 页面 | 检查项 | 风险 |
|------|--------|------|
| `pages/crud/ProductPage.tsx` | 是否仍有 StandardDataGrid + 编辑抽屉 + 批量操作 | 高 |
| `pages/DashboardPage.tsx` | 是否仍有 KpiCard + GettingStartedChecklist + hero 区 | 中 |
| `pages/live/LiveWorkbenchPage.tsx` | WorkflowStepper 是否完整 | 中 |
| `pages/live/SessionWorkspacePage.tsx` | embeddedInWorkbench 检测是否工作 | 中 |
| `pages/shortvideo/ViralLibraryPage.tsx` | 爆款库详情弹窗是否完整 | 中 |

### 3.2 检查方法

对每个页面执行以下检查：

1. **功能完整性**：对照侧栏菜单中的每个页面入口，点击进入，检查核心功能
2. **组件一致性**：是否使用 StandardDataGrid / PageHeader / ConfirmDialog / SPACING
3. **导入检查**：`grep -c "SessionActions\|StandardDataGrid\|ConfirmDialog\|SPACING" <file>` — 如果为 0 说明可能被退化

---

## 四、LiveSessionPage 完整恢复版参考

以下是恢复后的 LiveSessionPage 应有的完整结构（伪代码）：

```typescript
import { useState, useCallback, useEffect, useMemo } from 'react'
import { Box, Card, CardContent, Typography, Paper, Alert, TextField, Button,
         IconButton, Tooltip, Chip, Select, MenuItem, FormControl, InputLabel,
         Collapse, useMediaQuery, useTheme } from '@mui/material'
import { type GridColDef } from '@mui/x-data-grid'
import { ConfirmDialog, PageHeader, PageSkeleton, StandardDataGrid } from '@/components/base'
import { DATAGRID_ZH_LOCALE } from '@/utils/datagrid-locale'
import { SPACING } from '@/theme/spacingScale'
import { useToast } from '@/contexts/ToastContext'
import { useNavigate, useLocation } from 'react-router-dom'
import { searchSessions, deleteSession } from '@/api/live'
import { cloneLiveSession } from '@/api/live-session'
import { searchAccounts } from '@/api/douyin'
import { normalizePageResult } from '@/utils/pageResult'
import { SessionActions } from './components/SessionActions'
import { sessionWorkbenchHref } from './sessionWorkbenchNav'
// + 所有需要的图标导入

export function LiveSessionPage() {
  // 状态变量（含 viewMode, endedSectionOpen 等）
  // liveWorkbenchBase 变量
  // handleClone 函数
  // getTimeHint 函数
  // ReadinessSteps 子组件

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: SPACING.CARD_GAP }}>
      <PageHeader
        title="直播场次"
        actions={/* 视图切换 + 刷新 + 创建按钮 */}
      />

      {/* 筛选区 */}
      ...

      {/* 内容区 */}
      <Paper>
        {loading ? <PageSkeleton variant="table" /> :
         data.length === 0 ? /* 空状态 + 引导创建 */ :
         viewMode === 'card' ? (
          /* 卡片视图 — 三组分组 */
          <>
            {/* 直播中组 */}
            {groupedSessionCards.live.length > 0 && (
              <Section title="直播中">
                {groupedSessionCards.live.map(row => (
                  <SessionCard row={row}>
                    <ReadinessSteps row={row} />
                    <SessionActions variant="card" ... />
                  </SessionCard>
                ))}
              </Section>
            )}

            {/* 准备中组 */}
            {groupedSessionCards.prep.length > 0 && (
              <Section title="准备中 / 即将开播">
                {groupedSessionCards.prep.map(row => <SessionCard>...</SessionCard>)}
              </Section>
            )}

            {/* 已结束组（折叠） */}
            {groupedSessionCards.ended.length > 0 && (
              <Collapse in={endedSectionOpen}>
                {groupedSessionCards.ended.map(row => <SessionCard>...</SessionCard>)}
              </Collapse>
            )}
          </>
        ) : (
          /* 表格视图 — StandardDataGrid */
          <StandardDataGrid
            rows={data}
            columns={columns}  // 含 SessionActions 操作列
            loading={loading}
            paginationMode="server"
            rowCount={total}
            ...
          />
        )}
      </Paper>

      {/* 删除确认 */}
      <ConfirmDialog ... />
    </Box>
  )
}
```

---

## 五、修复优先级

| 优先级 | 编号 | 说明 | 工作量 |
|--------|------|------|--------|
| **P0 紧急** | FIX-01 | LiveSessionPage 恢复全部功能 | 高（核心页面重写） |
| P1 重要 | CHECK-01 | 检查 ProductPage 是否被退化 | 低（检查 + 可能修复） |
| P1 重要 | CHECK-02 | 检查 DashboardPage 是否被退化 | 低 |
| P2 一般 | CHECK-03~05 | 检查其他高风险页面 | 低 |

---

## 六、根因分析

Cursor 在执行第四轮升级方案时，可能做了以下操作导致退化：

1. **误将升级前的旧版本覆盖了升级后的版本**：LiveSessionPage 之前已经被升级为使用 StandardDataGrid + SessionActions 的版本，但 Cursor 在「统一表格」任务中可能重新生成了整个文件，基于旧的认知生成了一个简化版
2. **只创建了共享组件但没有接入**：`SessionActions.tsx` 和 `sessionWorkbenchNav.ts` 都被正确创建，但 LiveSessionPage 没有引用它们
3. **丢失了 git 暂存的改动**：有可能某次文件操作覆盖了已有改动

**预防措施**：
- 每次 Cursor 升级后，执行 `git diff --stat` 检查改动文件列表
- 对核心页面（LiveSessionPage, DashboardPage, ProductPage）执行功能回归检查
- 在 CLAUDE.md 中明确标注「不得退化」的核心功能列表
