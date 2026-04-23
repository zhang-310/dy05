# D5-01: 业务驱动型 Dashboard 重构（增强版 v2）

> **优先级**: P0 | **复杂度**: L | **维度**: D5 数据与分析
> **前置**: 无 | **验收**: Dashboard 首屏可见 GMV 数字 + 环比 + 场次数据 + 在播入口，系统指标折叠到底部
> **版本**: v2（2026-03-21 深度分析后增强）

---

## 一、问题诊断

### 1.1 当前代码分析

`DashboardPage.tsx`（398 行）三个角色的首屏内容：

| 角色 | 当前首屏内容 | 问题 |
|------|------------|------|
| **Admin** | 用户总数 / 机构数量 / 达人数 / API调用 / 登录日志 / 同步日志 / API成功率 / 系统状态 | **8 张卡片全是系统监控，零 GMV** |
| **Org** | 旗下达人 / 视频数 / 直播场次 / 待审文案 | **零 GMV，"旗下达人"无排名** |
| **Talent** | 视频数 / 直播场次 / 粉丝数 / 互动量 | **零 GMV，粉丝数=0 互动量=0（后端硬编码返回 0）** |

**共同问题**: 快捷入口 Admin 21 个 / Org 7 个 / Talent 6 个——Admin 的 21 个按钮中有 11 个是系统管理类（占 52%），只有 10 个是业务类。

### 1.2 数据加载瀑布

当前 DashboardPage 首屏发 **2 个串行 API 请求**：
1. `getAdminDashboard()` / `getOrgDashboard()` / `getTalentDashboard()` — 统计概览
2. `getProductReadinessSummary()` — 商品就绪度

原方案的 `getBusinessDashboard()` 额外增加 3 个请求（共 5 个），且 #1 `POST /dashboard/admin/stats` 对 Org/Talent 角色会 **403 报错**。

---

## 二、目标布局

### 2.1 视觉分区（四行结构）

```
┌────────────────────────────────────────────────────────────────┐
│ Row 1: GMV 核心指标（3 列）                                      │
│ ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│ │ 今日 GMV     │  │ 今日场次     │  │ 下一场       │          │
│ │ ¥38,200      │  │ 5            │  │ 晚间美妆专场 │          │
│ │ ↑12% 较昨日  │  │ 🟢2进行中   │  │ 2小时30分后  │          │
│ │ [迷你趋势图] │  │ ✅3已完成    │  │ ▓▓▓░░ 60%   │          │
│ └──────────────┘  └──────────────┘  └──────────────┘          │
├────────────────────────────────────────────────────────────────┤
│ Row 2: 在播场次横幅（仅当有 status=1 时显示）                     │
│ 🔴 正在直播: 「下午护肤品专场」  观众 1,234  GMV ¥12,800       │
│                                              [进入实时面板 →]  │
├────────────────────────────────────────────────────────────────┤
│ Row 3: 商品就绪度 + AI 用量（2 列）                              │
│ ┌────────────────────────┐  ┌──────────────────────┐          │
│ │ 商品库就绪度           │  │ AI 助手              │          │
│ │ ●●●○○ 60% 就绪        │  │ 今日调用 128         │          │
│ │ 32品 / 19有卖点 / 12激活│  │ 成功率 97.2%         │          │
│ └────────────────────────┘  └──────────────────────┘          │
├────────────────────────────────────────────────────────────────┤
│ Row 4: 快捷操作（业务优先 8 个 + 折叠"更多"）                    │
│ [直播场次] [商品管理] [话术] [短视频] [AI] [文案] [账号] [数据]│
│ ▼ 更多 (点击展开系统管理入口)                                   │
├────────────────────────────────────────────────────────────────┤
│ Row 5: 系统状态（Admin Only，默认折叠）                          │
│ ▸ 系统状态 ✅正常                                               │
└────────────────────────────────────────────────────────────────┘
```

### 2.2 三角色差异化矩阵

| 区域 | Admin | Org (institution) | Talent |
|------|-------|-------------------|--------|
| **Row 1 左** | 全局 GMV + 较昨日% | 旗下总 GMV + 较昨日% | 我的 GMV + 较昨日% |
| **Row 1 中** | 全局场次(进行中/完成) | 旗下场次(进行中/完成) | 我的场次(进行中/完成) |
| **Row 1 右** | 下一场倒计时 | 下一场倒计时 | 下一场倒计时 |
| **Row 2** | 在播横幅(全局) | 在播横幅(旗下) | 在播横幅(我的) |
| **Row 3 左** | 商品就绪度(全库) | 商品就绪度(旗下) | 商品就绪度(我的) |
| **Row 3 右** | AI 用量 + 系统成功率 | AI 用量 | AI 用量 |
| **Row 4** | 8 业务入口 + 折叠更多 | 7 业务入口 | 6 业务入口 |
| **Row 5** | 系统状态(折叠) | 不显示 | 不显示 |

---

## 三、改动文件

### 3.1 `frontend-react/src/types/dashboard.ts` — 新建

```typescript
/** 业务 Dashboard 指标（增强版 v2） */
export interface BusinessDashboardData {
  // === GMV 核心 ===
  todayGmv: number
  yesterdayGmv: number         // 新增: 昨日同期，用于计算环比
  weekGmv: number
  monthGmv: number
  gmvTrend: Array<{ date: string; amount: number }>  // 最近7天，用于迷你趋势图

  // === 场次统计 ===
  liveSessionsToday: number
  liveSessionsOngoing: number
  liveSessionsCompleted: number
  liveSessionsTotal: number
  avgGmvPerSession: number     // 新增: 场均 GMV

  // === 在播场次（status=1 的第一个场次） ===
  ongoingSession: {
    id: number
    title: string
    viewers: number
    gmv: number
  } | null

  // === 话术 TOP3 ===
  topScripts: Array<{
    id: number
    content: string            // 截取前50字
    productName: string
    gmvContribution: number
    usageCount: number
  }>

  // === 下一场倒计时 ===
  nextSession: {
    id: number
    title: string
    scheduledTime: string
    readinessPercent: number    // 0-100
    productCount: number
    scriptCount: number
    accountName?: string       // 新增: 关联账号名
  } | null

  // === AI 用量 ===
  aiCallsToday: number
  aiSuccessRate: number
}

/** GMV 环比计算结果 */
export interface GmvDelta {
  value: number                // 差值
  percent: number              // 百分比变化
  direction: 'up' | 'down' | 'flat'
}

/** 计算 GMV 环比 */
export function calcGmvDelta(today: number, yesterday: number): GmvDelta {
  if (yesterday === 0 && today === 0) return { value: 0, percent: 0, direction: 'flat' }
  if (yesterday === 0) return { value: today, percent: 100, direction: 'up' }
  const diff = today - yesterday
  const pct = Math.round((diff / yesterday) * 100)
  return {
    value: diff,
    percent: Math.abs(pct),
    direction: pct > 0 ? 'up' : pct < 0 ? 'down' : 'flat',
  }
}
```

### 3.2 `frontend-react/src/api/dashboard.ts` — 修改

在现有文件末尾新增（**修复原方案的权限问题**）：

```typescript
import type { BusinessDashboardData } from '@/types/dashboard'

/**
 * 业务 Dashboard 数据（v2）
 *
 * 关键修复:
 * 1. Admin 用 /dashboard/admin/stats, Org/Talent 用 /dashboard/org/stats（修复 403 问题）
 * 2. 复用已有的 getAdminDashboard/getOrgDashboard/getTalentDashboard 避免重复请求
 * 3. 从 ongoing sessions 列表中提取在播场次信息
 */
export async function getBusinessDashboard(
  roleCode: string
): Promise<BusinessDashboardData> {
  // 根据角色选择正确的统计端点（避免非 admin 调 admin 端点 403）
  const statsPromise =
    roleCode === 'admin'
      ? request.post<unknown, BackendDashboardStats>('/dashboard/admin/stats', {})
      : request.post<unknown, BackendDashboardStats>('/dashboard/org/stats', {})

  const [statsResult, ongoingResult, nextResult] = await Promise.allSettled([
    statsPromise,
    request.post<unknown, { total: number; list: Record<string, unknown>[] }>(
      '/live/session/search',
      { page: 0, rows: 3, status: 1, sortName: 'startTime', sortOrder: 'desc' }
    ),
    request.post<unknown, { total: number; list: Record<string, unknown>[] }>(
      '/live/session/search',
      { page: 0, rows: 1, status: 0, sortName: 'scheduledTime', sortOrder: 'asc' }
    ),
  ])

  const stats = statsResult.status === 'fulfilled' ? statsResult.value : ({} as BackendDashboardStats)
  const ongoingData = ongoingResult.status === 'fulfilled' ? ongoingResult.value : { total: 0, list: [] }
  const nextData = nextResult.status === 'fulfilled' ? nextResult.value : { total: 0, list: [] }

  const ongoingRow = ongoingData.list?.[0]
  const nextRow = nextData.list?.[0]

  const todayGmv = Number(stats.todayRevenue ?? 0)
  const completed = stats.completedSessions ?? 0

  return {
    todayGmv,
    yesterdayGmv: 0,         // TODO: 后端 DashboardService 增加 yesterdayRevenue 查询
    weekGmv: 0,              // TODO: 后端增加周聚合
    monthGmv: 0,             // TODO: 后端增加月聚合
    gmvTrend: [],            // TODO: 后端增加 group by date 趋势

    liveSessionsToday: stats.todaySessions ?? 0,
    liveSessionsOngoing: ongoingData.total ?? 0,
    liveSessionsCompleted: completed,
    liveSessionsTotal: stats.totalLiveSessions ?? 0,
    avgGmvPerSession: completed > 0 ? Math.round(todayGmv / completed) : 0,

    ongoingSession: ongoingRow
      ? {
          id: Number(ongoingRow.id),
          title: String(ongoingRow.liveTitle ?? ''),
          viewers: Number(ongoingRow.viewers ?? 0),
          gmv: Number(ongoingRow.totalRevenue ?? 0),
        }
      : null,

    topScripts: [],          // TODO: 后端增加 TOP N 话术查询

    nextSession: nextRow
      ? {
          id: Number(nextRow.id),
          title: String(nextRow.liveTitle ?? ''),
          scheduledTime: String(nextRow.scheduledTime ?? nextRow.startTime ?? ''),
          readinessPercent:
            Number(nextRow.productCount ?? 0) > 0 && Number(nextRow.scriptCount ?? 0) > 0
              ? 100
              : Number(nextRow.productCount ?? 0) > 0
                ? 50
                : 0,
          productCount: Number(nextRow.productCount ?? 0),
          scriptCount: Number(nextRow.scriptCount ?? 0),
        }
      : null,

    aiCallsToday: stats.todayAiCalls ?? 0,
    aiSuccessRate: stats.todayAiSuccessRate ?? 0,
  }
}
```

### 3.3 `frontend-react/src/pages/DashboardPage.tsx` — 全面重构

**当前**: 398 行，纯系统监控视角
**目标**: ~600 行，业务指标优先 + 在播横幅 + 系统状态折叠

#### 完整组件结构

```tsx
import { useEffect, useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Typography, Grid, Card, CardContent, CircularProgress,
  Button, Chip, Divider, Collapse, LinearProgress, Alert, Skeleton,
  Tooltip, IconButton,
} from '@mui/material'
import {
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  Business as BusinessIcon,
  Person as PersonIcon,
  SmartToy as SmartToyIcon,
  VideoLibrary as VideoIcon,
  Mic as MicIcon,
  Edit as EditIcon,
  Settings as SettingsIcon,
  Storage as StorageIcon,
  List as ListIcon,
  Monitor as MonitorIcon,
  Psychology as PsychologyIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
  ShoppingBag as ShoppingBagIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Add as AddIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  FiberManualRecord as FiberManualRecordIcon,
  OpenInNew as OpenInNewIcon,
  BarChart as BarChartIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { useUserStore } from '@/stores/user'
import { useRolePrefix } from '@/hooks/useRolePrefix'
import { getBusinessDashboard } from '@/api/dashboard'
import { getProductReadinessSummary } from '@/api/product'
import type { ProductReadinessSummary } from '@/types/product'
import type { BusinessDashboardData } from '@/types/dashboard'
import { calcGmvDelta } from '@/types/dashboard'

// ==================== StatCard ====================
// 保留现有 StatCard 组件不变（用于系统状态折叠区）

// ==================== NextSessionCountdown ====================
function NextSessionCountdown({ scheduledTime }: { scheduledTime: string }) {
  const [remaining, setRemaining] = useState('')
  const [isUrgent, setIsUrgent] = useState(false)

  useEffect(() => {
    const update = () => {
      const diff = new Date(scheduledTime).getTime() - Date.now()
      if (diff <= 0) {
        setRemaining('已超过计划时间')  // 修复: 区分"即将开播"和"已超时"
        setIsUrgent(true)
        return
      }
      const h = Math.floor(diff / 3600000)
      const m = Math.floor((diff % 3600000) / 60000)
      const s = Math.floor((diff % 60000) / 1000)

      setIsUrgent(h === 0 && m < 30)  // 30分钟内标记为紧急

      if (h > 24) setRemaining(`${Math.floor(h / 24)}天${h % 24}小时后`)
      else if (h > 0) setRemaining(`${h}小时${m}分钟后`)
      else if (m > 0) setRemaining(`${m}分${s}秒后`)
      else setRemaining(`${s}秒后`)
    }
    update()
    // 修复: 临近开播时更频繁更新（每秒 vs 每分钟）
    const timer = setInterval(update, isUrgent ? 1000 : 60000)
    return () => clearInterval(timer)
  }, [scheduledTime, isUrgent])

  return (
    <Typography
      variant="body2"
      fontWeight={600}
      color={isUrgent ? 'warning.main' : 'info.main'}
    >
      {remaining}
    </Typography>
  )
}

// ==================== GmvDeltaChip ====================
// 新增: GMV 环比展示组件
function GmvDeltaChip({ today, yesterday }: { today: number; yesterday: number }) {
  const delta = calcGmvDelta(today, yesterday)
  if (delta.direction === 'flat') return null
  return (
    <Chip
      size="small"
      icon={delta.direction === 'up' ? <TrendingUpIcon /> : <TrendingDownIcon />}
      label={`${delta.direction === 'up' ? '+' : '-'}${delta.percent}% 较昨日`}
      color={delta.direction === 'up' ? 'success' : 'error'}
      variant="outlined"
      sx={{ height: 22, '& .MuiChip-label': { fontSize: '0.7rem' } }}
    />
  )
}

// ==================== DashboardSkeleton ====================
// 新增: 首屏骨架屏（替代 CircularProgress 白屏）
function DashboardSkeleton() {
  return (
    <Box>
      <Skeleton variant="text" width={120} height={36} sx={{ mb: 2 }} />
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[0, 1, 2].map((i) => (
          <Grid item xs={12} sm={4} key={i}>
            <Skeleton variant="rounded" height={120} />
          </Grid>
        ))}
      </Grid>
      <Skeleton variant="rounded" height={80} sx={{ mb: 2 }} />
      <Skeleton variant="rounded" height={100} />
    </Box>
  )
}

// ==================== 快捷入口配置 ====================
// 修复: 分为业务入口（固定显示）和管理入口（折叠）
const buildBusinessQuickLinks = (prefix: string) => [
  { label: '直播场次', path: `${prefix}/live/sessions`, icon: <MicIcon fontSize="small" /> },
  { label: '商品管理', path: `${prefix}/product`, icon: <ShoppingBagIcon fontSize="small" /> },
  { label: '话术管理', path: `${prefix}/script/list`, icon: <EditIcon fontSize="small" /> },
  { label: '短视频工作台', path: `${prefix}/shortvideo/dashboard`, icon: <VideoIcon fontSize="small" /> },
  { label: 'AI 知识库', path: `${prefix}/ai/knowledge`, icon: <PsychologyIcon fontSize="small" /> },
  { label: '文案库', path: `${prefix}/copy/library`, icon: <EditIcon fontSize="small" /> },
  { label: '账号管理', path: `${prefix}/douyin/accounts`, icon: <VideoIcon fontSize="small" /> },
  { label: '数据对比', path: `${prefix}/live/compare`, icon: <BarChartIcon fontSize="small" /> },
]

const buildAdminSystemLinks = (prefix: string) => [
  { label: '用户管理', path: `${prefix}/auth/users`, icon: <PeopleIcon fontSize="small" /> },
  { label: '角色管理', path: `${prefix}/auth/roles`, icon: <SettingsIcon fontSize="small" /> },
  { label: '人设管理', path: `${prefix}/douyin/personas`, icon: <PersonIcon fontSize="small" /> },
  { label: '配置管理', path: `${prefix}/config`, icon: <SettingsIcon fontSize="small" /> },
  { label: '存储管理', path: `${prefix}/storage`, icon: <StorageIcon fontSize="small" /> },
  { label: '系统监控', path: `${prefix}/system`, icon: <MonitorIcon fontSize="small" /> },
  { label: 'API 日志', path: `${prefix}/system/api-log`, icon: <MonitorIcon fontSize="small" /> },
  { label: '操作日志', path: `${prefix}/log/operation`, icon: <ListIcon fontSize="small" /> },
  { label: '登录日志', path: `${prefix}/auth/login-logs`, icon: <ListIcon fontSize="small" /> },
  { label: '文案审批', path: `${prefix}/copy/approval`, icon: <EditIcon fontSize="small" /> },
  { label: 'AI 监控', path: `${prefix}/ai/monitoring`, icon: <MonitorIcon fontSize="small" /> },
  { label: '企微机器人', path: `${prefix}/wecom/robots`, icon: <SettingsIcon fontSize="small" /> },
]

const buildOrgQuickLinks = (prefix: string) => [
  { label: '直播场次', path: `${prefix}/live/sessions`, icon: <MicIcon fontSize="small" /> },
  { label: '旗下达人', path: `${prefix}/auth/users`, icon: <PeopleIcon fontSize="small" /> },
  { label: '商品库', path: `${prefix}/product`, icon: <ShoppingBagIcon fontSize="small" /> },
  { label: '商品就绪度', path: `${prefix}/product/readiness`, icon: <CheckIcon fontSize="small" /> },
  { label: '文案库', path: `${prefix}/copy/library`, icon: <EditIcon fontSize="small" /> },
  { label: '话术管理', path: `${prefix}/script/list`, icon: <EditIcon fontSize="small" /> },
  { label: '短视频工作台', path: `${prefix}/shortvideo/dashboard`, icon: <VideoIcon fontSize="small" /> },
]

const buildTalentQuickLinks = (prefix: string) => [
  { label: '直播场次', path: `${prefix}/live/sessions`, icon: <MicIcon fontSize="small" /> },
  { label: '短视频工作台', path: `${prefix}/shortvideo/dashboard`, icon: <VideoIcon fontSize="small" /> },
  { label: '商品库', path: `${prefix}/product`, icon: <ShoppingBagIcon fontSize="small" /> },
  { label: '商品就绪度', path: `${prefix}/product/readiness`, icon: <CheckIcon fontSize="small" /> },
  { label: '文案库', path: `${prefix}/copy/library`, icon: <EditIcon fontSize="small" /> },
  { label: '话术管理', path: `${prefix}/script/list`, icon: <EditIcon fontSize="small" /> },
]

// ==================== 主组件 ====================
export function DashboardPage() {
  const userStore = useUserStore()
  const navigate = useNavigate()
  const prefix = useRolePrefix()

  // 原有 state
  const [stats, setStats] = useState<Record<string, unknown>>({})
  const [loading, setLoading] = useState(true)
  const [productReadiness, setProductReadiness] = useState<ProductReadinessSummary | null>(null)
  const [productReadinessLoading, setProductReadinessLoading] = useState(true)

  // 新增 state
  const [bizData, setBizData] = useState<BusinessDashboardData | null>(null)
  const [bizLoading, setBizLoading] = useState(true)
  const [bizError, setBizError] = useState(false)
  const [systemExpanded, setSystemExpanded] = useState(false)
  const [moreLinksExpanded, setMoreLinksExpanded] = useState(false)

  const roleCode = userStore.roleCode || 'talent'
  const isAdmin = roleCode === 'admin'
  const isOrg = roleCode === 'institution'

  // 保留原有的 stats 加载（供系统状态折叠区使用）
  useEffect(() => {
    const fetchFn =
      roleCode === 'admin' ? getAdminDashboard
        : roleCode === 'institution' ? getOrgDashboard
          : getTalentDashboard
    fetchFn()
      .then(setStats)
      .catch(() => setStats({}))
      .finally(() => setLoading(false))
  }, [roleCode])

  // 新增: 业务 Dashboard 数据加载
  useEffect(() => {
    setBizLoading(true)
    setBizError(false)
    getBusinessDashboard(roleCode)
      .then(setBizData)
      .catch(() => { setBizData(null); setBizError(true) })
      .finally(() => setBizLoading(false))
  }, [roleCode])

  // 保留原有的 productReadiness 加载
  useEffect(() => {
    let cancelled = false
    setProductReadinessLoading(true)
    void getProductReadinessSummary({ page: 0, rows: 1 })
      .then((r) => { if (!cancelled) setProductReadiness(r) })
      .catch(() => { if (!cancelled) setProductReadiness(null) })
      .finally(() => { if (!cancelled) setProductReadinessLoading(false) })
    return () => { cancelled = true }
  }, [roleCode])

  // 刷新
  const handleRefresh = () => {
    setBizLoading(true)
    getBusinessDashboard(roleCode)
      .then(setBizData)
      .catch(() => setBizData(null))
      .finally(() => setBizLoading(false))
  }

  // 骨架屏（替代 CircularProgress 白屏）
  if (loading && bizLoading) {
    return <DashboardSkeleton />
  }

  const systemOverall = stats.systemOverall as string | undefined

  return (
    <Box>
      {/* === 标题 + 刷新 === */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ flex: 1, fontWeight: 600 }}>工作台</Typography>
        <Tooltip title="刷新数据">
          <IconButton size="small" onClick={handleRefresh} disabled={bizLoading}>
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Box>

      {/* === 数据加载失败降级提示 === */}
      {bizError && (
        <Alert severity="warning" sx={{ mb: 2 }} action={
          <Button size="small" onClick={handleRefresh}>重试</Button>
        }>
          业务数据加载失败，部分指标可能不准确
        </Alert>
      )}

      {/* ==================== Row 1: GMV 核心指标（3列） ==================== */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {/* 列1: 今日 GMV + 环比 */}
        <Grid item xs={12} sm={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                {isAdmin ? '全局 GMV' : '今日 GMV'}
              </Typography>
              {bizLoading ? (
                <Skeleton variant="text" width="60%" height={48} />
              ) : (
                <>
                  <Typography variant="h4" fontWeight={700} color="success.main" sx={{ lineHeight: 1.2 }}>
                    ¥{(bizData?.todayGmv ?? 0).toLocaleString()}
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                    <GmvDeltaChip
                      today={bizData?.todayGmv ?? 0}
                      yesterday={bizData?.yesterdayGmv ?? 0}
                    />
                    {bizData?.avgGmvPerSession != null && bizData.avgGmvPerSession > 0 && (
                      <Typography variant="caption" color="text.secondary">
                        场均 ¥{bizData.avgGmvPerSession.toLocaleString()}
                      </Typography>
                    )}
                  </Box>
                  {/* TODO: 迷你 7 天趋势图（ECharts sparkline），待后端 gmvTrend 数据就绪后启用 */}
                  {/* {bizData?.gmvTrend && bizData.gmvTrend.length > 0 && (
                    <Box sx={{ height: 40, mt: 1 }}>
                      <GmvSparkline data={bizData.gmvTrend} />
                    </Box>
                  )} */}
                </>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 列2: 场次统计 */}
        <Grid item xs={12} sm={4}>
          <Card
            sx={{ height: '100%', cursor: 'pointer', '&:hover': { boxShadow: 3 } }}
            onClick={() => navigate(`${prefix}/live/sessions`)}
          >
            <CardContent>
              <Typography color="text.secondary" variant="body2">今日场次</Typography>
              {bizLoading ? (
                <Skeleton variant="text" width="40%" height={48} />
              ) : (
                <>
                  <Typography variant="h4" fontWeight={700}>
                    {bizData?.liveSessionsToday ?? 0}
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1, mt: 0.5, flexWrap: 'wrap' }}>
                    {(bizData?.liveSessionsOngoing ?? 0) > 0 && (
                      <Chip
                        size="small"
                        color="success"
                        icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important' }} />}
                        label={`进行中 ${bizData?.liveSessionsOngoing}`}
                        sx={{ height: 22 }}
                      />
                    )}
                    <Chip
                      size="small"
                      variant="outlined"
                      label={`已完成 ${bizData?.liveSessionsCompleted ?? 0}`}
                      sx={{ height: 22 }}
                    />
                  </Box>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 列3: 下一场倒计时 */}
        <Grid item xs={12} sm={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              {bizLoading ? (
                <>
                  <Skeleton variant="text" width="50%" />
                  <Skeleton variant="text" width="80%" height={32} />
                  <Skeleton variant="rectangular" height={6} sx={{ mt: 1, borderRadius: 3 }} />
                </>
              ) : bizData?.nextSession ? (
                <>
                  <Typography color="text.secondary" variant="body2">下一场</Typography>
                  <Typography
                    variant="subtitle1" fontWeight={600} noWrap
                    sx={{ cursor: 'pointer', '&:hover': { color: 'primary.main' } }}
                    onClick={() => navigate(`${prefix}/live/sessions/${bizData.nextSession!.id}`)}
                  >
                    {bizData.nextSession.title}
                  </Typography>
                  <NextSessionCountdown scheduledTime={bizData.nextSession.scheduledTime} />
                  <LinearProgress
                    variant="determinate"
                    value={bizData.nextSession.readinessPercent}
                    color={bizData.nextSession.readinessPercent >= 100 ? 'success' : 'primary'}
                    sx={{ mt: 1, height: 6, borderRadius: 3 }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    已选 {bizData.nextSession.productCount} 品 / 已备 {bizData.nextSession.scriptCount} 段话术
                  </Typography>
                </>
              ) : (
                <>
                  <Typography color="text.secondary" variant="body2">下一场</Typography>
                  <Typography color="text.disabled" sx={{ my: 1 }}>暂无计划场次</Typography>
                  <Button
                    size="small" variant="outlined" startIcon={<AddIcon />}
                    onClick={() => navigate(`${prefix}/live/sessions/create`)}
                  >
                    创建场次
                  </Button>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* ==================== Row 2: 在播场次横幅 ==================== */}
      {bizData?.ongoingSession && (
        <Alert
          severity="success"
          icon={
            <FiberManualRecordIcon
              sx={{
                fontSize: 12,
                color: 'success.main',
                '@keyframes pulse': { '0%, 100%': { opacity: 1 }, '50%': { opacity: 0.3 } },
                animation: 'pulse 1.5s ease-in-out infinite',
              }}
            />
          }
          action={
            <Button
              size="small" variant="contained" color="success"
              endIcon={<OpenInNewIcon />}
              onClick={() => navigate(`${prefix}/live/realtime?sessionId=${bizData.ongoingSession!.id}`)}
            >
              进入实时面板
            </Button>
          }
          sx={{ mb: 2, '& .MuiAlert-message': { display: 'flex', alignItems: 'center', gap: 2, flex: 1 } }}
        >
          <Typography variant="body2" fontWeight={600}>
            正在直播: {bizData.ongoingSession.title}
          </Typography>
          <Chip size="small" label={`观众 ${bizData.ongoingSession.viewers.toLocaleString()}`} variant="outlined" />
          {bizData.ongoingSession.gmv > 0 && (
            <Chip size="small" label={`GMV ¥${bizData.ongoingSession.gmv.toLocaleString()}`} color="success" />
          )}
        </Alert>
      )}

      {/* ==================== Row 3: 商品就绪度 + AI 用量 ==================== */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {/* 左: 商品就绪度 */}
        <Grid item xs={12} md={7}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography
                variant="subtitle1" fontWeight={600} gutterBottom
                sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
              >
                <ShoppingBagIcon fontSize="small" color="primary" />
                商品库就绪度
              </Typography>
              {productReadinessLoading ? (
                <Skeleton variant="rectangular" height={48} />
              ) : productReadiness ? (
                <>
                  {/* 就绪率进度条 */}
                  <Box sx={{ mb: 1.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="body2" color="text.secondary">
                        已激活 / 总商品
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {productReadiness.withActiveScript} / {productReadiness.totalProducts}
                        ({productReadiness.totalProducts > 0
                          ? Math.round((productReadiness.withActiveScript / productReadiness.totalProducts) * 100)
                          : 0}%)
                      </Typography>
                    </Box>
                    <LinearProgress
                      variant="determinate"
                      value={productReadiness.totalProducts > 0
                        ? (productReadiness.withActiveScript / productReadiness.totalProducts) * 100
                        : 0}
                      color="success"
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                  </Box>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    <Chip size="small" variant="outlined" label={`商品 ${productReadiness.totalProducts}`} />
                    <Chip size="small" color="primary" variant="outlined" label={`有链接 ${productReadiness.withProductLink}`} />
                    <Chip size="small" color="secondary" variant="outlined" label={`有卖点 ${productReadiness.withSellingPoints}`} />
                    <Chip size="small" variant="outlined" label={`话术≥1 ${productReadiness.withAtLeastOneScript}`} />
                    <Chip size="small" color="success" variant="outlined" label={`已激活 ${productReadiness.withActiveScript}`} />
                  </Box>
                </>
              ) : (
                <Typography variant="body2" color="text.secondary">暂无数据</Typography>
              )}
              <Box sx={{ mt: 1.5 }}>
                <Button size="small" variant="outlined" onClick={() => navigate(`${prefix}/product/readiness`)}>
                  就绪看板
                </Button>
                <Button size="small" sx={{ ml: 1 }} onClick={() => navigate(`${prefix}/product`)}>
                  商品库
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* 右: AI 用量 */}
        <Grid item xs={12} md={5}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography
                variant="subtitle1" fontWeight={600} gutterBottom
                sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
              >
                <SmartToyIcon fontSize="small" color="secondary" />
                AI 助手
              </Typography>
              {bizLoading ? (
                <Skeleton variant="rectangular" height={48} />
              ) : (
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography color="text.secondary" variant="caption">今日调用</Typography>
                    <Typography variant="h5" fontWeight={600}>
                      {bizData?.aiCallsToday ?? 0}
                    </Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography color="text.secondary" variant="caption">成功率</Typography>
                    <Typography
                      variant="h5" fontWeight={600}
                      color={(bizData?.aiSuccessRate ?? 0) >= 95 ? 'success.main' : 'warning.main'}
                    >
                      {bizData?.aiSuccessRate != null ? `${bizData.aiSuccessRate.toFixed(1)}%` : '-'}
                    </Typography>
                  </Grid>
                </Grid>
              )}
              <Button
                size="small" sx={{ mt: 1 }}
                onClick={() => navigate(`${prefix}/ai/monitoring`)}
              >
                AI 监控详情
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* ==================== Row 4: 快捷操作 ==================== */}
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>快捷操作</Typography>
          <Divider sx={{ mb: 1.5 }} />
          {/* 业务入口（始终显示） */}
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
            {(isAdmin ? buildBusinessQuickLinks(prefix)
              : isOrg ? buildOrgQuickLinks(prefix)
                : buildTalentQuickLinks(prefix)
            ).map((item) => (
              <Button
                key={item.path} variant="outlined" size="small"
                startIcon={item.icon}
                onClick={() => navigate(item.path)}
              >
                {item.label}
              </Button>
            ))}
          </Box>
          {/* Admin: 管理入口（折叠） */}
          {isAdmin && (
            <>
              <Box
                sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer', mt: 1.5 }}
                onClick={() => setMoreLinksExpanded(!moreLinksExpanded)}
              >
                <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
                  系统管理
                </Typography>
                {moreLinksExpanded ? <ExpandLessIcon fontSize="small" color="action" /> : <ExpandMoreIcon fontSize="small" color="action" />}
              </Box>
              <Collapse in={moreLinksExpanded}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
                  {buildAdminSystemLinks(prefix).map((item) => (
                    <Button
                      key={item.path} variant="text" size="small"
                      startIcon={item.icon}
                      onClick={() => navigate(item.path)}
                      sx={{ color: 'text.secondary' }}
                    >
                      {item.label}
                    </Button>
                  ))}
                </Box>
              </Collapse>
            </>
          )}
        </CardContent>
      </Card>

      {/* ==================== Row 5: 系统状态（Admin Only, 折叠） ==================== */}
      {isAdmin && (
        <Card>
          <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
            <Box
              sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}
              onClick={() => setSystemExpanded(!systemExpanded)}
            >
              <Typography variant="subtitle2" color="text.secondary" sx={{ flex: 1 }}>
                系统状态
              </Typography>
              {/* 修复: 从实际数据读取状态而非硬编码 */}
              <Chip
                size="small"
                icon={systemOverall === 'UP' ? <CheckIcon /> : systemOverall === 'DEGRADED' ? <WarningIcon /> : undefined}
                label={systemOverall === 'UP' ? '正常' : systemOverall === 'DEGRADED' ? '部分异常' : '未知'}
                color={systemOverall === 'UP' ? 'success' : systemOverall === 'DEGRADED' ? 'warning' : 'default'}
                sx={{ mr: 1 }}
              />
              {systemExpanded ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
            </Box>
            <Collapse in={systemExpanded}>
              <Grid container spacing={2} sx={{ mt: 1 }}>
                <Grid item xs={6} sm={3}>
                  <StatCard title="用户总数" value={Number(stats.userCount) || 0} icon={<PeopleIcon />} onClick={() => navigate(`${prefix}/auth/users`)} />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <StatCard title="机构数量" value={Number(stats.orgCount) || 0} icon={<BusinessIcon />} color="info.main" />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <StatCard title="达人数" value={Number(stats.talentCount) || 0} icon={<PersonIcon />} color="success.main" />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <StatCard title="API 调用" value={Number(stats.apiLogCount) ?? Number(stats.aiCallCount) ?? 0} icon={<SmartToyIcon />} color="secondary.main" onClick={() => navigate(`${prefix}/system/api-log`)} />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <StatCard title="登录日志" value={Number(stats.loginLogCount) || 0} icon={<ListIcon />} color="info.main" onClick={() => navigate(`${prefix}/auth/login-logs`)} />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <StatCard title="同步日志" value={Number(stats.syncLogCount) || 0} icon={<ListIcon />} color="info.main" onClick={() => navigate(`${prefix}/system/sync-log`)} />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <StatCard title="API 成功率" value={stats.apiSuccessRate != null ? `${Number(stats.apiSuccessRate).toFixed(1)}%` : '-'} icon={<MonitorIcon />} color="success.main" onClick={() => navigate(`${prefix}/system/api-log`)} />
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Card sx={{ height: '100%' }}>
                    <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <Box sx={{ flex: 1 }}>
                        <Typography color="text.secondary" variant="body2">系统状态</Typography>
                        <Chip
                          size="small"
                          icon={systemOverall === 'UP' ? <CheckIcon /> : systemOverall === 'DEGRADED' ? <WarningIcon /> : <ErrorIcon />}
                          label={systemOverall === 'UP' ? '正常' : systemOverall === 'DEGRADED' ? '部分异常' : systemOverall ?? '未知'}
                          color={systemOverall === 'UP' ? 'success' : systemOverall === 'DEGRADED' ? 'warning' : 'error'}
                          sx={{ mt: 0.5 }}
                          onClick={() => navigate(`${prefix}/system`)}
                        />
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            </Collapse>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
```

---

## 四、原方案修复清单

| # | 原方案问题 | 修复内容 |
|---|-----------|---------|
| F1 | `getBusinessDashboard()` 非 admin 调 `/dashboard/admin/stats` → 403 | 根据 roleCode 选择正确端点 |
| F2 | GMV 数字无对比锚点 | 新增 `yesterdayGmv` + `GmvDeltaChip` 环比组件 |
| F3 | 无在播场次入口 | 新增 Row 2 在播横幅（一键进入实时面板） |
| F4 | 无场均 GMV | 新增 `avgGmvPerSession` 字段 |
| F5 | 系统状态 Chip 硬编码"正常" | 改为从 `systemOverall` 实际值读取 |
| F6 | 快捷入口从 21 砍到 8 太激进 | 分为业务入口（8 个显示）+ 管理入口（折叠） |
| F7 | 倒计时 `diff <= 0` 显示"即将开播"而非"已超时" | 区分负值场景 + 30 分钟内高频刷新 |
| F8 | loading 时 CircularProgress 白屏 | 改为 Skeleton 骨架屏 |
| F9 | API 全部失败时无反馈 | 新增 `bizError` + Alert 降级提示 + 重试按钮 |
| F10 | 无刷新按钮 | 标题栏增加刷新 IconButton |
| F11 | Org/Talent 角色 Dashboard 无具体实现 | 三角色快捷入口差异化配置 |
| F12 | 商品就绪度只有 Chip 数字 | 增加就绪率进度条（百分比可视化） |

---

## 五、后端 API 依赖（增量）

| 前端需要 | 后端改动 | 复杂度 | 优先级 |
|---------|---------|--------|--------|
| `yesterdayGmv` | DashboardService 增加 `calculateYesterdayRevenue()` —— 与 todayRevenue 相同逻辑，改时间范围 | S | P0 |
| `weekGmv` / `monthGmv` | DashboardService 增加周/月聚合 | S | P1 |
| `gmvTrend (7天)` | DashboardService 增加 `getRevenueByDay(days=7)` —— GROUP BY DATE(create_time) | M | P1 |
| `topScripts (TOP3)` | LiveScriptService 增加 `findTopByEffectivenessScore(limit=3)` | S | P1 |
| `systemOverall` | 对接 `/actuator/health` 或自定义健康检查 | S | P2 |

**P0 阶段只需后端增加 1 个查询（yesterdayRevenue），其余 P1/P2 渐进补齐。**

---

## 六、验收标准（增强版）

1. ✅ Dashboard 首屏可见 GMV 大字 + 环比变化
2. ✅ 有在播场次时顶部显示在播横幅 + 一键进入实时面板
3. ✅ 下一场倒计时正确处理超时场景（非硬编码"即将开播"）
4. ✅ 商品就绪度有进度条百分比
5. ✅ 系统状态从实际数据读取（非硬编码"正常"）
6. ✅ Admin 快捷入口分两组（业务 8 个 + 管理折叠 12 个）
7. ✅ Org/Talent 角色 Dashboard 显示相应快捷入口
8. ✅ 首屏 loading 显示骨架屏而非 CircularProgress
9. ✅ API 失败时显示降级 Alert + 重试按钮
10. ✅ 非 admin 角色不会 403 报错
11. ✅ `npm run type-check` 通过
12. ✅ 移动端（375px）三列卡片正确堆叠为单列

---

## 七、注意事项

1. **不要删除已有系统监控代码**，移到 Collapse 折叠区
2. 保留 `useRolePrefix()` / `useUserStore()` / `getAdminDashboard` 等已有函数
3. `StatCard` 组件保持不变，在折叠区继续使用
4. `productReadiness` 相关代码保持不变（单独 useEffect 加载）
5. 使用 MUI Grid legacy API：`<Grid item xs={12} sm={4}>`，不使用 v2 `size` prop
6. `useToast()` 返回函数：`const toast = useToast(); toast('msg', 'success')`
7. ECharts 迷你趋势图（GmvSparkline）代码已注释，待后端 `gmvTrend` 数据就绪后取消注释
8. 需要新增导入：`Skeleton`, `Alert`, `Collapse`, `LinearProgress`, `Tooltip`, `IconButton`, `ExpandMore/ExpandLess`, `TrendingUp/TrendingDown`, `Add`, `FiberManualRecord`, `OpenInNew`, `BarChart`, `Refresh`
9. `getBusinessDashboard` 函数签名增加了 `roleCode` 参数
10. `calcGmvDelta` 是纯函数，放在 types/dashboard.ts 中导出

---

## 附录 A：角色 × Dashboard 区块矩阵（第 0 周决策）

与 [11-方向基线与防漂移.md](./11-方向基线与防漂移.md) §3 一致：**指标与 GMV 相关组件单一实现**，由 [`DashboardPage.tsx`](../../frontend-react/src/pages/DashboardPage.tsx) 按 `roleCode` 组合区块，不维护两套互不等价的首页。

| 区块 / 能力 | admin | institution（机构） | talent（达人） | operator（运营，若有） | photographer（摄影，若有） | reviewer（审核，若有） |
|-------------|:-----:|:-------------------:|:--------------:|:----------------------:|:---------------------------:|:---------------------:|
| 今日 / 昨日 GMV（业务接口） | ✅ | ✅（可见范围内达人汇总） | ✅（本人） | ✅（同机构数据范围） | ⬜ | ⬜ |
| 在播横幅 / 进行中场次 | ✅（全局最近一场） | ✅（范围内） | ✅（本人） | ✅ | ⬜ | ⬜ |
| 今日开播场次数 | ✅ | ✅ | ✅ | ✅ | ⬜ | ⬜ |
| 系统监控 / 用户机构统计（折叠） | ✅ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 快捷入口（角色定制） | ✅ 全站 | ✅ 机构常用 | ✅ 达人常用 | 与 institution 对齐 | 拍摄任务等独立菜单 | 审批队列等独立菜单 |
| 商品就绪度摘要 | ✅ | ✅ | ✅ | ✅ | ⬜ | ⬜ |

**说明**：未单独建「六套首页」；摄影 / 审核等角色以侧栏菜单为主，Dashboard 仅当该角色路由落到工作台时显示上表勾选为 ⬜ 的区块可隐藏或极简。后续若新增 `roleCode`，在本表增列并同步 [`10-内部运营体系战略规划.md`](./10-内部运营体系战略规划.md)。
