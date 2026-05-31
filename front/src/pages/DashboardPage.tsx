import { useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  Box, Grid, Card, CardContent, Typography, Divider,
  ToggleButtonGroup, ToggleButton, Skeleton, Stack, Chip, Button,
  List, ListItem, ListItemText, ListItemIcon, Paper, MenuItem, TextField,
  LinearProgress, Alert,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone'
import GavelIcon from '@mui/icons-material/Gavel'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import PeopleIcon from '@mui/icons-material/People'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import ArticleIcon from '@mui/icons-material/Article'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import WhatshotIcon from '@mui/icons-material/Whatshot'
import { alpha, useTheme } from '@mui/material/styles'
import { useQuery } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router-dom'
import { ErrorAlert, PageHeader } from '@/components/base'
import { GettingStartedChecklist } from '@/components/onboarding/GettingStartedChecklist'
import { LazyECharts } from '@/utils/echarts-registry'
import { ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION } from '@/constants/shortvideoRoutes'
import { dashboardApi } from '@/api/dashboard'
import { aiApi } from '@/api/ai'
import type { AiCallTypeDistributionItem } from '@/types/ai'
import { liveApi, type LiveSession } from '@/api/live'
import { getErrorMessage } from '@/utils/errorHandler'
import { isRecord, normalizeRows } from '@/utils/response-normalize'

type DashboardRole = 'admin' | 'org' | 'talent'

const FUNNEL_CHANNEL_DOWNGRADE_MESSAGE =
  '/dashboard/conversion-funnel 当前仅按 lookbackDays 聚合，后端暂未支持 channel 参数；渠道筛选保持只读，不向接口追加伪造过滤条件。'

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function inferRouteScope(pathname: string): DashboardRole {
  if (pathname.startsWith('/org')) return 'org'
  if (pathname.startsWith('/talent')) return 'talent'
  return 'admin'
}

function scopeLabel(scope: DashboardRole) {
  if (scope === 'org') return '机构端'
  if (scope === 'talent') return '达人端'
  return '管理员端'
}

function liveSessionListPath(scope: DashboardRole) {
  if (scope === 'org') return '/org/live/sessions'
  if (scope === 'talent') return '/talent/live/sessions'
  return '/org/live/sessions'
}

function liveSessionDetailPath(scope: DashboardRole, id: number | string) {
  return `${liveSessionListPath(scope)}/${id}`
}

function liveSessionScriptReviewPath(scope: DashboardRole, id: number | string) {
  if (scope === 'admin') return `/org/live/sessions/${id}?step=2&tab=scripts`
  return liveSessionDetailPath(scope, id)
}

function fmt(n: number | undefined, digits = 0) {
  if (n == null) return '--'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: digits })
}

function fmtMoney(n: number | undefined) {
  if (n == null) return '--'
  if (n >= 10000) return '¥' + (n / 10000).toFixed(2) + '万'
  return '¥' + n.toFixed(2)
}

function fmtPct(n: number | undefined) {
  if (n == null) return '--'
  return n.toFixed(1) + '%'
}

function liveStatusInfo(s: number) {
  if (s === 1) return { label: '直播中', color: 'error' as const }
  if (s === 0) return { label: '准备中', color: 'warning' as const }
  if (s === 3) return { label: '已取消', color: 'info' as const }
  return { label: '已结束', color: 'default' as const }
}

interface StatCardProps {
  title: string
  value: string
  sub?: string
  subValue?: string
  icon: ReactNode
  color?: string
  loading?: boolean
}

function StatCard({ title, value, sub, subValue, icon, color = 'primary.main', loading }: StatCardProps) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Stack direction="row" alignItems="flex-start" justifyContent="space-between">
          <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={500}>{title}</Typography>
            {loading
              ? <Skeleton width={80} height={36} />
              : <Typography variant="h5" fontWeight={700} color={color} sx={{ mt: 0.5, lineHeight: 1 }}>{value}</Typography>
            }
            {sub && (
              <Typography variant="caption" color="text.disabled" sx={{ mt: 0.5, display: 'block' }}>
                {sub}：{loading ? '--' : subValue}
              </Typography>
            )}
          </Box>
          <Box sx={{ color, opacity: 0.8, mt: 0.5 }}>{icon}</Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

interface KpiBoxProps {
  label: string
  value: string
  trend?: number
  loading?: boolean
}

function KpiBox({ label, value, trend, loading }: KpiBoxProps) {
  return (
    <Box sx={{ textAlign: 'center', px: 2, minWidth: 96 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      {loading ? <Skeleton width={60} sx={{ mx: 'auto' }} /> :
        <Typography variant="h6" fontWeight={700}>{value}</Typography>
      }
      {trend != null && (
        <Chip
          size="small"
          icon={<TrendingUpIcon sx={{ fontSize: 12 }} />}
          label={`${trend > 0 ? '+' : ''}${trend.toFixed(1)}%`}
          color={trend >= 0 ? 'success' : 'error'}
          sx={{ fontSize: 10, height: 18, mt: 0.3 }}
        />
      )}
    </Box>
  )
}

function DiagnosticChip({ label, value, color = 'default' }: { label: string; value: string; color?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info' }) {
  return (
    <Chip
      variant="outlined"
      color={color}
      label={`${label}: ${value}`}
      sx={{ borderRadius: 1, maxWidth: '100%' }}
    />
  )
}

function LiveSessionsPanel({ routeScope }: { routeScope: DashboardRole }) {
  const navigate = useNavigate()
  const { data, refetch, isError, error, isLoading } = useQuery({
    queryKey: ['dashboard-live-sessions', routeScope],
    queryFn: () => liveApi.sessionSearch({ rows: 4 }),
    refetchInterval: 30000,
  })
  const sessions = normalizeRows<LiveSession>(data?.list ?? data).slice(0, 4)

  return (
    <Card
      variant="outlined"
      data-testid="dashboard-live-sessions-panel"
      data-contract-endpoint="/live/session/search"
      data-route-scope={routeScope}
      data-no-local-session-fallback="true"
      sx={{ mb: 2 }}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
          <Stack spacing={0.2}>
            <Typography variant="subtitle2" fontWeight={600}>实时场次状态</Typography>
            <Typography variant="caption" color="text.secondary">
              数据源 `/live/session/search`，状态口径：0 准备中、1 直播中、2 已结束、3 已取消。
            </Typography>
          </Stack>
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()}>刷新</Button>
        </Stack>
        {isError && (
          <Alert
            severity="warning"
            data-testid="dashboard-live-sessions-error"
            data-row-retained-on-error="true"
            sx={{ mb: 1 }}
          >
            实时场次加载失败（/live/session/search）：{getErrorMessage(error)}
          </Alert>
        )}
        {isLoading ? <Skeleton height={96} /> : sessions.length === 0
          ? <Typography data-testid="dashboard-live-sessions-empty" color="text.secondary" variant="body2" py={2} textAlign="center">暂无场次数据；请检查当前账号是否已创建直播场次。</Typography>
          : sessions.map(s => {
            const info = liveStatusInfo(toNumber(s.status))
            return (
              <Box key={s.id} sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider', '&:last-child': { borderBottom: 'none' } }}>
                <Stack direction="row" alignItems="center" spacing={1} mb={0.5}>
                  <Chip
                    label={info.label}
                    size="small"
                    data-testid="dashboard-live-status-chip-surface"
                    sx={{
                      fontWeight: 600,
                      height: 20,
                      bgcolor: (theme) => info.color === 'default'
                        ? alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.14 : 0.08)
                        : alpha(theme.palette[info.color].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                      color: info.color === 'default' ? 'text.secondary' : `${info.color}.main`,
                    }}
                  />
                  <Typography variant="body2" fontWeight={600} noWrap sx={{ flex: 1 }}>{s.liveTitle || `场次 ${s.id}`}</Typography>
                  {toNumber(s.status) === 1 && (
                    <Typography variant="caption" color="text.secondary">
                      在线 {toNumber(s.viewers).toLocaleString()} 人
                    </Typography>
                  )}
                </Stack>
                <Stack direction="row" spacing={1}>
                  <Button size="small" variant="outlined" sx={{ fontSize: 11, py: 0.2 }}
                    onClick={() => navigate(liveSessionDetailPath(routeScope, s.id))}>进入工作台</Button>
                  {routeScope === 'org' && toNumber(s.status) === 1 && (
                    <Button size="small" variant="outlined" sx={{ fontSize: 11, py: 0.2 }}
                      onClick={() => navigate(`/org/live/sessions/${s.id}/realtime`)}>实时面板</Button>
                  )}
                </Stack>
              </Box>
            )
          })}
      </CardContent>
    </Card>
  )
}

function systemAlertsQuery() {
  return import('@/api/system').then(m => m.systemApi.alertActive())
}

function normalizePendingApprovals(value: unknown): Array<{ id?: number | string; liveTitle?: string }> {
  return normalizeRows<{ id?: number | string; liveTitle?: string }>(value)
}

function TodoAndAiPanel({ routeScope }: { routeScope: DashboardRole }) {
  const navigate = useNavigate()
  const { data: alerts, isError: alertsIsError, error: alertsError } = useQuery({
    queryKey: ['dashboard-alerts'],
    queryFn: systemAlertsQuery,
    enabled: routeScope === 'admin',
  })
  const { data: approvals, isError: approvalsIsError, error: approvalsError } = useQuery({
    queryKey: ['dashboard-approvals', routeScope],
    queryFn: () => liveApi.approvalPending(),
  })

  const activeAlerts = normalizeRows(alerts)
  const alertCount = isRecord(alerts) && typeof alerts.total === 'number' ? alerts.total : activeAlerts.length
  const approvalItems = normalizePendingApprovals(approvals)
  const approvalCount = approvalItems.length
  const firstApprovalSessionId = approvalItems.find(item => Number(item.id) > 0)?.id
  const approvalPath = firstApprovalSessionId
    ? liveSessionScriptReviewPath(routeScope, firstApprovalSessionId)
    : liveSessionListPath(routeScope)

  const todos = [
    ...(approvalCount > 0 ? [{ icon: <GavelIcon />, color: 'warning.main', label: `话术待审批 ${approvalCount} 条`, path: approvalPath }] : []),
    ...(alertCount > 0 && routeScope === 'admin' ? [{ icon: <WarningAmberIcon />, color: 'error.main', label: `系统告警 ${alertCount} 条`, path: '/admin/system' }] : []),
  ]

  return (
    <Grid
      container
      spacing={1.5}
      mb={2}
      data-testid="dashboard-todo-ai-panel"
      data-contract-ready-endpoints={routeScope === 'admin' ? '/monitoring/alerts/active|/live/approval/pending' : '/live/approval/pending'}
      data-unsupported-endpoints="/dashboard/ai-recommendations|/ai/recommendations/persist"
      data-no-local-ai-recommendation-fallback="true"
    >
      <Grid item xs={12} md={6}>
        <Card variant="outlined" data-testid="dashboard-todo-panel" sx={{ height: '100%' }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <NotificationsNoneIcon fontSize="small" color="action" />
              <Typography variant="subtitle2" fontWeight={600}>待处理事项</Typography>
            </Stack>
            {alertsIsError && <Alert severity="warning" data-testid="dashboard-alerts-error" data-contract-endpoint="/monitoring/alerts/active" sx={{ mb: 1 }}>告警读取失败（/monitoring/alerts/active）：{getErrorMessage(alertsError)}</Alert>}
            {approvalsIsError && <Alert severity="warning" data-testid="dashboard-approvals-error" data-contract-endpoint="/live/approval/pending" sx={{ mb: 1 }}>审批读取失败（/live/approval/pending）：{getErrorMessage(approvalsError)}</Alert>}
            {todos.length === 0
              ? <Typography data-testid="dashboard-todo-empty" color="text.secondary" variant="body2" py={1}>暂无待处理事项</Typography>
              : (
                <List dense disablePadding>
                  {todos.map((t, i) => (
                    <ListItem key={i} disablePadding sx={{ py: 0.5 }}
                      secondaryAction={
                        <Button size="small" onClick={() => navigate(t.path)}>处理</Button>
                      }>
                      <ListItemIcon sx={{ minWidth: 32 }}>
                        <Box data-testid="dashboard-todo-icon-surface" sx={{ color: t.color }}>{t.icon}</Box>
                      </ListItemIcon>
                      <ListItemText primary={<Typography variant="body2">{t.label}</Typography>} />
                    </ListItem>
                  ))}
                </List>
              )}
          </CardContent>
        </Card>
      </Grid>
      <Grid item xs={12} md={6}>
        <Card variant="outlined" data-testid="dashboard-ai-recommendation-panel" sx={{ height: '100%' }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <AutoAwesomeIcon fontSize="small" color="primary" />
              <Typography variant="subtitle2" fontWeight={600}>AI 推荐链路</Typography>
            </Stack>
            <Alert
              severity="info"
              variant="outlined"
              data-testid="dashboard-ai-recommendation-degradation"
              data-contract-status="unsupported"
              data-no-local-ai-recommendation-fallback="true"
            >
              当前驾驶舱没有独立的 AI 推荐落库接口；页面只展示真实待审批和系统告警。话术优化、知识库进化和发布时机建议需接入推荐任务表或 AI 洞察接口后再展示。
            </Alert>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  )
}

export default function DashboardPage() {
  const theme = useTheme()
  const [days, setDays] = useState(30)
  const navigate = useNavigate()
  const location = useLocation()
  const routeScope = inferRouteScope(location.pathname)
  const [role, setRole] = useState<DashboardRole>(routeScope)

  useEffect(() => {
    setRole(routeScope)
  }, [routeScope])

  const statsQueryFn = routeScope === 'admin' ? dashboardApi.adminStats : dashboardApi.orgStats
  const statsSource = routeScope === 'admin'
    ? '/dashboard/admin/stats（管理员全局统计）'
    : '/dashboard/org/stats（当前用户归属统计）'

  const { data: stats, isLoading: statsLoading, isError: statsIsError, error: statsError, refetch: refetchStats } = useQuery({
    queryKey: ['dashboard-stats', routeScope],
    queryFn: statsQueryFn,
    staleTime: 5 * 60 * 1000,
  })

  const { data: kpi, isLoading: kpiLoading, isError: kpiIsError, error: kpiError, refetch: refetchKpi } = useQuery({
    queryKey: ['dashboard-kpi', routeScope, days],
    queryFn: () => dashboardApi.kpiUnified(days),
    staleTime: 5 * 60 * 1000,
  })

  const { data: funnel, isLoading: funnelLoading, isError: funnelIsError, error: funnelError, refetch: refetchFunnel } = useQuery({
    queryKey: ['dashboard-funnel', days],
    queryFn: () => dashboardApi.conversionFunnel(days),
    staleTime: 5 * 60 * 1000,
  })

  const { data: productsRaw, isError: productsIsError, error: productsError, refetch: refetchProducts } = useQuery({
    queryKey: ['dashboard-products', routeScope, days],
    queryFn: () => dashboardApi.productGmvSummary(days),
    staleTime: 5 * 60 * 1000,
  })
  const products = Array.isArray(productsRaw) ? productsRaw : []

  const { data: cockpitRaw, isLoading: cockpitLoading, isError: cockpitIsError, error: cockpitError, refetch: refetchCockpit } = useQuery({
    queryKey: ['dashboard-cockpit', routeScope, days],
    queryFn: () => dashboardApi.cockpitPreview(days),
    staleTime: 5 * 60 * 1000,
  })
  const cockpit = Array.isArray(cockpitRaw) ? cockpitRaw : []

  const { data: liveFormatsRaw, isError: liveFormatsIsError, error: liveFormatsError, refetch: refetchLiveFormats } = useQuery({
    queryKey: ['dashboard-live-format-gmv', routeScope, days],
    queryFn: () => dashboardApi.liveFormatGmv(days),
    staleTime: 5 * 60 * 1000,
  })
  const liveFormats = Array.isArray(liveFormatsRaw) ? liveFormatsRaw : []

  const { data: aiDistRaw, isError: aiDistIsError, error: aiDistError, refetch: refetchAiDist } = useQuery({
    queryKey: ['dashboard-ai-dist', days],
    queryFn: () => aiApi.callTypeDistribution({ days }),
    staleTime: 5 * 60 * 1000,
    enabled: routeScope === 'admin',
  })
  const aiDist: AiCallTypeDistributionItem[] = Array.isArray(aiDistRaw) ? aiDistRaw : []

  const cockpitTotal = useMemo(() => cockpit.reduce((acc, item) => ({
    gmv: acc.gmv + toNumber(item.gmv),
    sessions: acc.sessions + toNumber(item.sessions),
    orders: acc.orders + toNumber(item.orders),
  }), { gmv: 0, sessions: 0, orders: 0 }), [cockpit])
  const productTotalGmv = products.reduce((sum, p) => sum + toNumber(p.totalGmv), 0)
  const productCoverage = products.reduce((sum, p) => sum + toNumber(p.sessionCount), 0)
  const aiCalls = stats?.todayAiAttempts ?? stats?.todayAiCalls ?? kpi?.aiCallsToday

  const trendDates = cockpit.map(r => r.date ?? '')
  const themedChartColor = (tone: 'primary' | 'secondary' | 'success' | 'warning' | 'info') =>
    theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  const dashboardTrendColors = {
    gmv: themedChartColor('primary'),
    sessions: themedChartColor('success'),
    forecast: themedChartColor('warning'),
  }
  const liveFormatChartColor = themedChartColor('success')
  const funnelChartColors = [
    themedChartColor('primary'),
    themedChartColor('success'),
    themedChartColor('warning'),
    themedChartColor('secondary'),
    themedChartColor('info'),
  ]
  const forecastArr = kpi?.gmvForecast ?? []
  const forecastDates = forecastArr.map((_, i) => {
    const last = cockpit.length > 0 ? new Date(cockpit[cockpit.length - 1].date ?? '') : new Date()
    const d = new Date(last); d.setDate(d.getDate() + i + 1)
    return d.toISOString().slice(0, 10)
  })
  const allDates = [...trendDates, ...forecastDates]
  const forecastSeries = forecastArr.length > 0 ? [{
    name: 'AI预测GMV', type: 'line', smooth: true, yAxisIndex: 0,
    data: [...Array(trendDates.length).fill(null), ...forecastArr],
    lineStyle: { type: 'dashed', color: dashboardTrendColors.forecast },
    itemStyle: { color: dashboardTrendColors.forecast },
    symbol: 'circle', symbolSize: 6,
  }] : []
  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['GMV', '场次数', ...(forecastArr.length > 0 ? ['AI预测GMV'] : [])], top: 0 },
    xAxis: { type: 'category', data: allDates, axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: [
      { type: 'value', name: 'GMV(元)', axisLabel: { fontSize: 10 } },
      { type: 'value', name: '场次', splitLine: { show: false }, axisLabel: { fontSize: 10 } },
    ],
    series: [
      { name: 'GMV', type: 'line', smooth: true, yAxisIndex: 0,
        data: cockpit.map(r => r.gmv ?? 0),
        lineStyle: { color: dashboardTrendColors.gmv },
        itemStyle: { color: dashboardTrendColors.gmv },
        areaStyle: { color: alpha(dashboardTrendColors.gmv, theme.palette.mode === 'dark' ? 0.16 : 0.08) } },
      { name: '场次数', type: 'bar', yAxisIndex: 1,
        data: cockpit.map(r => r.sessions ?? 0), itemStyle: { color: dashboardTrendColors.sessions } },
      ...forecastSeries,
    ],
    grid: { left: 55, right: 45, bottom: 50, top: 36 },
  }

  const liveFormatOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: liveFormats.map(f => f.format), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar',
      data: liveFormats.map(f => f.gmv),
      itemStyle: { color: liveFormatChartColor },
      label: { show: true, position: 'top', fontSize: 10, formatter: (p: { value: number }) => fmtMoney(p.value) },
    }],
    grid: { left: 50, right: 20, bottom: 40, top: 36 },
  }

  const pieDist = aiDist.map(d => ({ value: toNumber(d.count), name: d.callType }))
  const pieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 10 } },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '42%'],
      data: pieDist.length > 0 ? pieDist : [{ value: 1, name: routeScope === 'admin' ? '暂无数据' : '管理员端可见' }],
      label: { show: false },
    }],
  }

  const QUICK_ENTRIES =
    routeScope === 'talent'
      ? [
          { label: '我的场次', icon: <AddCircleOutlineIcon />, color: 'primary.main', path: '/talent/live/sessions' },
          { label: '短视频项目', icon: <VideoLibraryIcon />, color: 'warning.main', path: '/talent/shortvideo' },
        ]
      : routeScope === 'org'
        ? [
            { label: '机构场次', icon: <AddCircleOutlineIcon />, color: 'primary.main', path: '/org/live/sessions' },
            { label: '成员管理', icon: <PeopleIcon />, color: 'secondary.main', path: '/org/members' },
            { label: '数据分析', icon: <TrendingUpIcon />, color: 'success.main', path: '/org/analytics' },
            { label: '复盘审核', icon: <GavelIcon />, color: 'warning.main', path: '/org/live/reviews' },
          ]
        : [
            { label: 'AI 总控', icon: <SmartToyIcon />, color: 'primary.main', path: '/admin/ai/dashboard' },
            { label: '知识库', icon: <LibraryBooksIcon />, color: 'secondary.main', path: '/admin/ai/knowledge' },
            { label: '进化爆款分析', icon: <WhatshotIcon />, color: 'warning.main', path: ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION },
            { label: '系统监控', icon: <TrendingUpIcon />, color: 'success.main', path: '/admin/monitoring' },
          ]

  const funnelSteps = funnel?.steps?.length
    ? funnel.steps.map(step => ({ label: step.name, value: step.value, rate: step.rate }))
    : funnel && typeof funnel === 'object' && 'exposure' in funnel ? [
        { label: '曝光', value: funnel.exposure },
        { label: '点击', value: funnel.clicks },
        { label: '加购', value: funnel.addToCart },
        { label: '下单', value: funnel.orders },
        { label: '支付', value: funnel.payments },
      ] : []

  const dashboardReadyEndpoints = [
    routeScope === 'admin' ? '/dashboard/admin/stats' : '/dashboard/org/stats',
    '/dashboard/kpi-unified',
    '/dashboard/cockpit-preview',
    '/dashboard/conversion-funnel',
    '/dashboard/product-gmv-summary',
    '/dashboard/live-format-gmv',
    '/live/session/search',
    '/live/approval/pending',
    ...(routeScope === 'admin' ? ['/monitoring/alerts/active', '/ai/admin/dashboard/call-type-distribution'] : []),
  ].join('|')
  const dashboardUnsupportedEndpoints = [
    '/dashboard/ai-recommendations',
    '/ai/recommendations/persist',
    '/dashboard/conversion-funnel?channel',
    routeScope === 'admin' ? '' : '/dashboard/admin/stats',
    routeScope === 'admin' ? '' : '/ai/admin/dashboard/call-type-distribution',
  ].filter(Boolean).join('|')

  const refetchDashboard = () => {
    void refetchStats()
    void refetchKpi()
    void refetchFunnel()
    void refetchProducts()
    void refetchCockpit()
    void refetchLiveFormats()
    void refetchAiDist()
  }

  return (
    <Box
      data-testid="dashboard-workbench"
      data-contract-scope="dashboard-role-shell"
      data-route-scope={routeScope}
      data-ready-endpoints={dashboardReadyEndpoints}
      data-unsupported-endpoints={dashboardUnsupportedEndpoints}
      data-no-local-kpi-fallback="true"
      data-no-cross-scope-admin-stats={routeScope === 'admin' ? 'false' : 'true'}
      data-no-cross-scope-ai-distribution={routeScope === 'admin' ? 'false' : 'true'}
      sx={{ height: 'calc(100vh - 48px)', overflowY: 'auto', p: 1.5 }}
    >
      <PageHeader
        title={routeScope === 'admin' ? '管理驾驶舱' : `${scopeLabel(routeScope)}工作台`}
        subtitle={
          routeScope === 'admin'
            ? '聚合 dashboard、live、monitoring 和 AI 调用统计；各区块独立降级，避免单个接口异常拖垮整页。'
            : `${scopeLabel(routeScope)}复用 dashboard 与 live 数据，只展示当前角色可进入的场次、短视频或机构功能入口；系统告警、知识库和 AI 管理入口保留在管理员端。`
        }
        breadcrumbs={[{ label: '运营' }, { label: routeScope === 'admin' ? '管理驾驶舱' : scopeLabel(routeScope) }]}
        actions={<Button variant="outlined" startIcon={<RefreshIcon />} onClick={refetchDashboard}>刷新</Button>}
      />
      <Grid container spacing={1.5} sx={{ mb: 2 }}>
        <Grid item xs={12} md={3}>
          <DiagnosticChip label="统计源" value={statsSource} color={routeScope === 'admin' ? 'primary' : 'info'} />
        </Grid>
        <Grid item xs={12} md={3}>
          <DiagnosticChip label="趋势源" value="/dashboard/cockpit-preview(dateFrom)" color="success" />
        </Grid>
        <Grid item xs={12} md={3}>
          <DiagnosticChip label="漏斗源" value="/dashboard/conversion-funnel" color="warning" />
        </Grid>
        <Grid item xs={12} md={3}>
          <DiagnosticChip label="AI 分布" value={routeScope === 'admin' ? '/ai/admin/dashboard/call-type-distribution' : '管理员端可见'} />
        </Grid>
      </Grid>
      {routeScope !== 'admin' ? (
        <Alert
          severity="info"
          variant="outlined"
          data-testid="dashboard-route-scope-alert"
          data-no-cross-scope-admin-stats="true"
          sx={{ mb: 2 }}
        >
          当前路由是 {scopeLabel(routeScope)} 壳。快捷入口和场次工作台会留在当前壳内，统计接口改走当前用户归属口径，避免从机构/达人端误打管理员全局统计。
        </Alert>
      ) : null}
      <GettingStartedChecklist />
      <Stack spacing={1} mb={2} data-testid="dashboard-error-stack" data-row-retained-on-error="true">
        {statsIsError && <Box data-testid="dashboard-stats-error" data-contract-endpoint={routeScope === 'admin' ? '/dashboard/admin/stats' : '/dashboard/org/stats'}><ErrorAlert severity="warning" title="统计加载失败" message={`${statsSource}：${getErrorMessage(statsError)}`} onRetry={() => void refetchStats()} /></Box>}
        {kpiIsError && <Box data-testid="dashboard-kpi-error" data-contract-endpoint="/dashboard/kpi-unified"><ErrorAlert severity="warning" title="KPI 加载失败" message={`/dashboard/kpi-unified：${getErrorMessage(kpiError)}`} onRetry={() => void refetchKpi()} /></Box>}
        {cockpitIsError && <Box data-testid="dashboard-cockpit-error" data-contract-endpoint="/dashboard/cockpit-preview"><ErrorAlert severity="warning" title="趋势数据加载失败" message={`/dashboard/cockpit-preview：${getErrorMessage(cockpitError)}`} onRetry={() => void refetchCockpit()} /></Box>}
        {funnelIsError && <Box data-testid="dashboard-funnel-error" data-contract-endpoint="/dashboard/conversion-funnel"><ErrorAlert severity="warning" title="转化漏斗加载失败" message={`/dashboard/conversion-funnel：${getErrorMessage(funnelError)}`} onRetry={() => void refetchFunnel()} /></Box>}
        {productsIsError && <Box data-testid="dashboard-products-error" data-contract-endpoint="/dashboard/product-gmv-summary"><ErrorAlert severity="warning" title="商品 GMV 加载失败" message={`/dashboard/product-gmv-summary：${getErrorMessage(productsError)}`} onRetry={() => void refetchProducts()} /></Box>}
        {liveFormatsIsError && <Box data-testid="dashboard-live-format-error" data-contract-endpoint="/dashboard/live-format-gmv"><ErrorAlert severity="warning" title="直播形式 GMV 加载失败" message={`/dashboard/live-format-gmv：${getErrorMessage(liveFormatsError)}`} onRetry={() => void refetchLiveFormats()} /></Box>}
        {aiDistIsError && <Box data-testid="dashboard-ai-distribution-error" data-contract-endpoint="/ai/admin/dashboard/call-type-distribution"><ErrorAlert severity="warning" title="AI 调用分布加载失败" message={`/ai/admin/dashboard/call-type-distribution：${getErrorMessage(aiDistError)}`} onRetry={() => void refetchAiDist()} /></Box>}
      </Stack>

      <Paper
        sx={{ p: 2, mb: 2 }}
        data-testid="dashboard-role-shell"
        data-route-scope={routeScope}
        data-contract-ready="true"
      >
        <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', md: 'center' }} spacing={1.5}>
          <Box>
            <Typography variant="h6">运营指挥中心</Typography>
            <Typography variant="caption" color="text.secondary">
              当前展示角色：{scopeLabel(role)}；路由数据边界：{scopeLabel(routeScope)}。
            </Typography>
          </Box>
          <ToggleButtonGroup size="small" exclusive value={role} onChange={(_, v) => v && setRole(v)} sx={{ flexWrap: 'wrap' }}>
            <ToggleButton value="admin"><PeopleIcon fontSize="small" sx={{ mr: 0.5 }} />管理员视图</ToggleButton>
            <ToggleButton value="org"><LiveTvIcon fontSize="small" sx={{ mr: 0.5 }} />机构视图</ToggleButton>
            <ToggleButton value="talent"><VideoLibraryIcon fontSize="small" sx={{ mr: 0.5 }} />达人视图</ToggleButton>
          </ToggleButtonGroup>
        </Stack>
        {routeScope !== role ? (
          <Alert
            severity="warning"
            variant="outlined"
            data-testid="dashboard-role-view-warning"
            data-no-cross-scope-request="true"
            sx={{ mt: 1.5 }}
          >
            你切到了 {scopeLabel(role)} 指标视图，但数据请求仍遵守 {scopeLabel(routeScope)} 路由边界；如需真实全局统计请进入管理员端。
          </Alert>
        ) : null}
      </Paper>

      <LiveSessionsPanel routeScope={routeScope} />
      <TodoAndAiPanel routeScope={routeScope} />

      <Card
        variant="outlined"
        data-testid="dashboard-kpi-strip"
        data-contract-endpoint="/dashboard/kpi-unified"
        data-no-local-kpi-fallback="true"
        sx={{ mb: 2 }}
      >
        <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Stack direction="row" alignItems="center" spacing={0} divider={<Divider orientation="vertical" flexItem />}
            sx={{ overflowX: 'auto' }}>
            {role === 'admin' && (
              <>
                <KpiBox label="今日GMV" value={fmtMoney(kpi?.gmvToday ?? stats?.todayRevenue)} trend={kpi?.gmvMom} loading={kpiLoading || statsLoading} />
                <KpiBox label="今日订单" value={fmt(kpi?.ordersToday)} loading={kpiLoading} />
                <KpiBox label="客单价" value={fmtMoney(kpi?.avgOrderValue)} loading={kpiLoading} />
                <KpiBox label="转化率" value={fmtPct(kpi?.conversionRate)} loading={kpiLoading} />
                <KpiBox label="进行中场次" value={fmt(kpi?.activeSessionCount ?? kpi?.liveSessions)} loading={kpiLoading} />
                <KpiBox label="AI调用" value={fmt(aiCalls)} loading={kpiLoading || statsLoading} />
                <KpiBox label="已发布视频" value={fmt(kpi?.publishedVideos ?? stats?.publishedShortVideos)} loading={kpiLoading || statsLoading} />
                <KpiBox label="知识库文档" value={fmt(kpi?.docCount)} loading={kpiLoading} />
              </>
            )}
            {role === 'org' && (
              <>
                <KpiBox label="机构GMV" value={fmtMoney(kpi?.gmvToday ?? stats?.todayRevenue)} trend={kpi?.gmvMom} loading={kpiLoading || statsLoading} />
                <KpiBox label="场次数" value={fmt(stats?.totalLiveSessions ?? kpi?.liveSessions)} loading={statsLoading || kpiLoading} />
                <KpiBox label="今日场次" value={fmt(stats?.todaySessions)} loading={statsLoading} />
                <KpiBox label="平均客单" value={fmtMoney(kpi?.avgOrderValue)} loading={kpiLoading} />
                <KpiBox label="转化率" value={fmtPct(kpi?.conversionRate)} loading={kpiLoading} />
                <KpiBox label="AI调用" value={fmt(aiCalls)} loading={kpiLoading || statsLoading} />
              </>
            )}
            {role === 'talent' && (
              <>
                <KpiBox label="我的GMV" value={fmtMoney(kpi?.gmvToday ?? stats?.todayRevenue)} trend={kpi?.gmvMom} loading={kpiLoading || statsLoading} />
                <KpiBox label="今日订单" value={fmt(kpi?.ordersToday)} loading={kpiLoading} />
                <KpiBox label="我的场次" value={fmt(stats?.totalLiveSessions ?? kpi?.liveSessions)} loading={statsLoading || kpiLoading} />
                <KpiBox label="短视频" value={fmt(stats?.totalShortVideos ?? kpi?.publishedVideos)} loading={statsLoading || kpiLoading} />
                <KpiBox label="话术使用" value={fmt(aiCalls)} loading={kpiLoading || statsLoading} />
                <KpiBox label="已发布视频" value={fmt(stats?.publishedShortVideos ?? kpi?.publishedVideos)} loading={statsLoading || kpiLoading} />
              </>
            )}
          </Stack>
          {kpi?.gmvTarget != null && kpi.gmvTarget > 0 && (
            <Box sx={{ mt: 1.5, px: 1 }}>
              {(() => {
                const pct = Math.min(100, ((kpi.gmvToday ?? 0) / kpi.gmvTarget!) * 100)
                const barColor = pct >= 80 ? 'success' : pct >= 60 ? 'primary' : 'warning'
                return (
                  <>
                    <Stack direction="row" justifyContent="space-between" mb={0.3}>
                      <Typography variant="caption" color="text.secondary">今日GMV目标完成率</Typography>
                      <Typography variant="caption" fontWeight={600}
                        color={pct >= 80 ? 'success.main' : pct >= 60 ? 'text.primary' : 'warning.main'}>
                        {pct.toFixed(1)}% / 目标 {fmtMoney(kpi.gmvTarget)}
                      </Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={pct} color={barColor}
                      sx={{ height: 6, borderRadius: 3 }} />
                  </>
                )
              })()}
            </Box>
          )}
        </CardContent>
      </Card>

      <Grid
        container
        spacing={1.5}
        sx={{ mb: 2 }}
        data-testid="dashboard-quick-entry-grid"
        data-route-scope={routeScope}
        data-no-cross-scope-navigation="true"
      >
        {QUICK_ENTRIES.map(e => (
          <Grid item xs={6} sm={3} key={e.label}>
            <Button
              variant="outlined"
              fullWidth
              startIcon={<Box sx={{ color: e.color, display: 'flex' }}>{e.icon}</Box>}
              onClick={() => navigate(e.path)}
              sx={{ justifyContent: 'flex-start', py: 1, textTransform: 'none', borderColor: 'divider', minHeight: 42 }}
            >
              <Typography variant="body2" fontWeight={600}>{e.label}</Typography>
            </Button>
          </Grid>
        ))}
      </Grid>

      <Stack direction={{ xs: 'column', md: 'row' }} alignItems={{ xs: 'stretch', md: 'center' }} justifyContent="space-between" mb={1.5} spacing={1}>
        <Box>
          <Typography variant="subtitle2" fontWeight={600} color="text.secondary">数据看板</Typography>
          <Typography variant="caption" color="text.secondary">
            当前窗口：近 {days} 天；趋势接口按 `dateFrom` 过滤，商品和形式分布按 `lookbackDays` 过滤。
          </Typography>
        </Box>
        <ToggleButtonGroup size="small" exclusive value={days} onChange={(_, v) => v && setDays(v)}>
          <ToggleButton value={7} sx={{ fontSize: 12, py: 0.3, px: 1.5 }}>近7天</ToggleButton>
          <ToggleButton value={30} sx={{ fontSize: 12, py: 0.3, px: 1.5 }}>近30天</ToggleButton>
          <ToggleButton value={90} sx={{ fontSize: 12, py: 0.3, px: 1.5 }}>近90天</ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      <Grid container spacing={1.5} mb={1.5}>
        <Grid item xs={6} sm={4} md={2}>
          <StatCard title="总用户数" value={fmt(stats?.totalUsers)} sub="今日新增" subValue={fmt(stats?.todayUsers)}
            icon={<PeopleIcon />} loading={statsLoading} />
        </Grid>
        <Grid item xs={6} sm={4} md={2}>
          <StatCard title="直播场次" value={fmt(stats?.totalLiveSessions)} sub="今日" subValue={fmt(stats?.todaySessions)}
            icon={<LiveTvIcon />} color="success.main" loading={statsLoading} />
        </Grid>
        <Grid item xs={6} sm={4} md={2}>
          <StatCard title="抖音视频" value={fmt(stats?.totalVideos)} sub="已发布" subValue={fmt(stats?.publishedVideos)}
            icon={<VideoLibraryIcon />} color="warning.main" loading={statsLoading} />
        </Grid>
        <Grid item xs={6} sm={4} md={2}>
          <StatCard title="短视频" value={fmt(stats?.totalShortVideos)} sub="今日" subValue={fmt(stats?.todayShortVideos)}
            icon={<VideoLibraryIcon />} color="info.main" loading={statsLoading} />
        </Grid>
        <Grid item xs={6} sm={4} md={2}>
          <StatCard title="文案库" value={fmt(stats?.totalCopyItems)} sub="已审批" subValue={fmt(stats?.approvedCopyItems)}
            icon={<ArticleIcon />} color="secondary.main" loading={statsLoading} />
        </Grid>
        <Grid item xs={6} sm={4} md={2}>
          <StatCard title="AI今日调用" value={fmt(stats?.todayAiCalls)} sub="成功率" subValue={fmtPct(stats?.aiSuccessRate)}
            icon={<SmartToyIcon />} color="error.main" loading={statsLoading} />
        </Grid>
      </Grid>

      <Grid container spacing={1.5} mb={1.5}>
        <Grid item xs={12} md={8}>
          <Card variant="outlined">
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle2" fontWeight={600}>GMV & 场次趋势</Typography>
                <Typography variant="caption" color="text.secondary">
                  合计 {fmtMoney(cockpitTotal.gmv)} / {fmt(cockpitTotal.sessions)} 场 / {fmt(cockpitTotal.orders)} 条排品
                </Typography>
              </Stack>
              {cockpitLoading ? <Skeleton height={240} /> : (
                cockpit.length > 0
                  ? (
                    <Box data-testid="dashboard-trend-chart-surface" data-chart-colors={Object.values(dashboardTrendColors).join('|')}>
                      <LazyECharts option={trendOption} style={{ height: 240 }} />
                    </Box>
                  )
                  : <Box data-testid="dashboard-trend-empty" data-contract-endpoint="/dashboard/cockpit-preview" sx={{ height: 240, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center', px: 2 }}>
                      <Typography color="text.secondary">暂无趋势数据；请检查 `/dashboard/cockpit-preview` 是否返回场次 rows，或当前账号近 {days} 天是否有场次。</Typography>
                    </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>AI调用类型分布</Typography>
              {routeScope !== 'admin' ? (
                <Box
                  data-testid="dashboard-ai-distribution-unsupported"
                  data-contract-status="unsupported"
                  data-unsupported-endpoint="/ai/admin/dashboard/call-type-distribution"
                  data-no-cross-scope-ai-distribution="true"
                  sx={{ height: 240, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center', px: 2 }}
                >
                  <Typography color="text.secondary">AI 调用分布是管理员运维数据，当前 {scopeLabel(routeScope)} 壳不请求该接口。</Typography>
                </Box>
              ) : (
                <LazyECharts option={pieOption} style={{ height: 240 }} />
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={1.5} mb={1.5}>
        <Grid item xs={12}>
          <Card variant="outlined">
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle2" fontWeight={600}>直播形式GMV分布</Typography>
                <Typography variant="caption" color="text.secondary">
                  数据源 `/dashboard/live-format-gmv`
                </Typography>
              </Stack>
              {liveFormats.length > 0 ? (
                <Box data-testid="dashboard-live-format-chart-surface" data-chart-color={liveFormatChartColor}>
                  <LazyECharts option={liveFormatOption} style={{ height: 200 }} />
                </Box>
              ) : (
                <Box data-testid="dashboard-live-format-empty" data-contract-endpoint="/dashboard/live-format-gmv" sx={{ height: 120, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography color="text.secondary">暂无直播形式 GMV；请检查场次 `sessionType` 与排品 `revenue` 是否已写入。</Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={1.5}>
        <Grid item xs={12} md={5}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ xs: 'stretch', sm: 'center' }} spacing={1} mb={1.5}>
                <Box>
                  <Typography variant="subtitle2" fontWeight={600}>转化漏斗</Typography>
                  <Typography variant="caption" color="text.secondary">当前后端返回观看、点赞、进入商品三步；订单/支付需后续接交易链路。</Typography>
                </Box>
                <Box sx={{ flex: 1 }} />
                <Stack spacing={0.75} sx={{ width: { xs: '100%', sm: 260 } }}>
                  <TextField
                    select
                    size="small"
                    label="渠道"
                    value=""
                    sx={{ width: '100%' }}
                    helperText="显式降级：后端未接收 channel"
                    disabled
                    data-testid="dashboard-funnel-channel-select"
                    inputProps={{
                      'data-testid': 'dashboard-funnel-channel-input',
                      'data-contract-status': 'unsupported',
                      'data-contract-endpoint': '/dashboard/conversion-funnel',
                    }}
                  >
                    <MenuItem value="">全部</MenuItem>
                    <MenuItem value="live">直播带货</MenuItem>
                    <MenuItem value="shortvideo">短视频引流</MenuItem>
                    <MenuItem value="search">搜索</MenuItem>
                    <MenuItem value="recommend">推荐流</MenuItem>
                  </TextField>
                  <Alert
                    severity="info"
                    variant="outlined"
                    data-testid="dashboard-funnel-channel-downgrade"
                    data-downgrade-tone="contract-gap"
                    sx={{ py: 0.25, '& .MuiAlert-message': { py: 0.25 } }}
                  >
                    {FUNNEL_CHANNEL_DOWNGRADE_MESSAGE}
                  </Alert>
                </Stack>
              </Stack>
              {funnelLoading ? <Skeleton height={200} /> : funnelSteps.length === 0 ? (
                <Box data-testid="dashboard-funnel-empty" data-contract-endpoint="/dashboard/conversion-funnel" sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center', px: 2 }}>
                  <Typography color="text.secondary">暂无漏斗数据；请检查 `/dashboard/conversion-funnel` 的 `steps` 是否为空。</Typography>
                </Box>
              ) : (
                <Box data-testid="dashboard-funnel-chart-surface" data-chart-colors={funnelChartColors.join('|')}>
                  <LazyECharts option={{
                    tooltip: {
                      trigger: 'item',
                      formatter: (p: { name: string; value: number; percent: number }) =>
                        `${p.name}: ${fmt(p.value)} (${p.percent?.toFixed(1)}%)`,
                    },
                    series: [{
                      type: 'funnel',
                      left: '5%', width: '90%',
                      min: 0, max: funnelSteps[0]?.value ?? 100,
                      minSize: '20%', maxSize: '100%',
                      sort: 'descending',
                      gap: 4,
                      label: { show: true, position: 'inside', fontSize: 11,
                        formatter: (p: { name: string; value: number }) => `${p.name}\n${fmt(p.value)}` },
                      data: funnelSteps.map((s, i) => ({
                        name: s.label, value: s.value,
                        itemStyle: { color: funnelChartColors[i] ?? funnelChartColors[funnelChartColors.length - 1] },
                      })),
                    }],
                    grid: { left: 0, right: 0, top: 0, bottom: 0 },
                  }} style={{ height: 200 }} />
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={7}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
                <ShoppingBagIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                <Typography variant="subtitle2" fontWeight={600}>商品GMV排行</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
                  合计 {fmtMoney(productTotalGmv)} / 覆盖 {fmt(productCoverage)} 场
                </Typography>
                <AttachMoneyIcon sx={{ fontSize: 16, color: 'success.main' }} />
              </Stack>
              {!products || products.length === 0 ? (
                <Typography data-testid="dashboard-products-empty" data-contract-endpoint="/dashboard/product-gmv-summary" variant="body2" color="text.disabled" textAlign="center" py={3}>暂无商品 GMV；请检查 `/dashboard/product-gmv-summary` 是否返回 rows。</Typography>
              ) : (
                <Stack spacing={0.8}>
                  {products.slice(0, 6).map((p, i) => (
                    <Stack key={`${p.productId}-${i}`} direction="row" alignItems="center" spacing={1}>
                      <Typography variant="caption" sx={{ width: 16, color: i < 3 ? 'warning.main' : 'text.disabled', fontWeight: 700 }}>{i + 1}</Typography>
                      <Typography variant="caption" sx={{ flex: 1 }} noWrap>{p.productName}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {p.totalOrders > 0 ? `${p.totalOrders}单` : `覆盖 ${p.sessionCount} 场`}
                      </Typography>
                      <Typography variant="caption" fontWeight={600} color="success.main">{fmtMoney(p.totalGmv)}</Typography>
                    </Stack>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
