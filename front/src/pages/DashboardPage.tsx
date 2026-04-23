import { useState } from 'react'
import {
  Box, Grid, Card, CardContent, Typography, Divider,
  ToggleButtonGroup, ToggleButton, Skeleton, Stack, Chip, Button,
  List, ListItem, ListItemText, ListItemIcon, Paper, MenuItem, TextField,
  LinearProgress,
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
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import ReactECharts from 'echarts-for-react'
import { ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION } from '@/constants/shortvideoRoutes'
import { dashboardApi } from '@/api/dashboard'
import { aiApi } from '@/api/ai'
import type { AiCallTypeDistributionItem } from '@/types/ai'
import { liveApi } from '@/api/live'

function fmt(n: number | undefined, digits = 0) {
  if (n == null) return '--'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: digits })
}

function fmtMoney(n: number | undefined) {
  if (n == null) return '--'
  if (n >= 10000) return '¥' + (n / 10000).toFixed(2) + '万'
  return '¥' + (n ?? 0).toFixed(2)
}

function fmtPct(n: number | undefined) {
  if (n == null) return '--'
  return n.toFixed(1) + '%'
}

interface StatCardProps {
  title: string
  value: string
  sub?: string
  subValue?: string
  icon: React.ReactNode
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
    <Box sx={{ textAlign: 'center', px: 2 }}>
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

// ─── Live Sessions Panel ─────────────────────────────────────────
function LiveSessionsPanel() {
  const navigate = useNavigate()
  const { data, refetch } = useQuery({
    queryKey: ['dashboard-live-sessions'],
    queryFn: () => liveApi.sessionSearch({ rows: 4 }),
    refetchInterval: 30000,
  })
  const sessions = data?.list ?? []

  const statusInfo = (s: number) => {
    if (s === 2) return { label: '直播中', color: '#f44336' as const }
    if (s === 1) return { label: '准备中', color: '#ff9800' as const }
    return { label: '已结束', color: '#9e9e9e' as const }
  }

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
          <Typography variant="subtitle2" fontWeight={600}>实时场次状态</Typography>
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()}>刷新</Button>
        </Stack>
        {sessions.length === 0
          ? <Typography color="text.secondary" variant="body2" py={2} textAlign="center">暂无场次数据</Typography>
          : sessions.map(s => {
            const info = statusInfo(s.status)
            return (
              <Box key={s.id} sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider', '&:last-child': { borderBottom: 'none' } }}>
                <Stack direction="row" alignItems="center" spacing={1} mb={0.5}>
                  <Chip label={info.label} size="small" sx={{ bgcolor: info.color, color: '#fff', fontWeight: 600, height: 20 }} />
                  <Typography variant="body2" fontWeight={600} noWrap sx={{ flex: 1 }}>{s.liveTitle}</Typography>
                  {s.status === 2 && (
                    <Typography variant="caption" color="text.secondary">
                      在线 {(s.viewers ?? 0).toLocaleString()}人
                    </Typography>
                  )}
                </Stack>
                <Stack direction="row" spacing={1}>
                  <Button size="small" variant="outlined" sx={{ fontSize: 11, py: 0.2 }}
                    onClick={() => navigate(`/admin/live/sessions/${s.id}`)}>进入工作台</Button>
                  {s.status === 2 && (
                    <Button size="small" variant="outlined" sx={{ fontSize: 11, py: 0.2 }}
                      onClick={() => navigate(`/admin/live/sessions/${s.id}/realtime`)}>实时面板</Button>
                  )}
                </Stack>
              </Box>
            )
          })}
      </CardContent>
    </Card>
  )
}

// ─── Todo + AI Recommendations ───────────────────────────────────
function TodoAndAiPanel() {
  const navigate = useNavigate()
  const { data: alerts } = useQuery({
    queryKey: ['dashboard-alerts'],
    queryFn: systemAlertsQuery,
  })
  const { data: approvals } = useQuery({
    queryKey: ['dashboard-approvals'],
    queryFn: () => liveApi.approvalPending(),
  })

  const alertCount = alerts?.length ?? 0
  const approvalCount = approvals?.length ?? 0

  const todos = [
    ...(approvalCount > 0 ? [{ icon: <GavelIcon />, color: '#ff9800', label: `话术待审批 ${approvalCount} 条`, path: '/admin/live/approval' }] : []),
    ...(alertCount > 0 ? [{ icon: <WarningAmberIcon />, color: '#f44336', label: `系统告警 ${alertCount} 条`, path: '/admin/system' }] : []),
  ]

  const aiRecs = [
    { type: '话术优化', title: '建议重新生成低分话术', detail: '检测到近3场评分<6分话术，建议AI重新生成' },
    { type: '知识库', title: '知识库需要更新', detail: '检测到过期文档，建议触发进化引擎' },
    { type: '发布时机', title: '最佳发布窗口即将到来', detail: '今晚20:00-22:00为历史最佳发布时段' },
  ]

  return (
    <Grid container spacing={1.5} mb={2}>
      <Grid item xs={12} md={6}>
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <NotificationsNoneIcon fontSize="small" color="action" />
              <Typography variant="subtitle2" fontWeight={600}>待处理事项</Typography>
            </Stack>
            {todos.length === 0
              ? <Typography color="text.secondary" variant="body2" py={1}>暂无待处理事项 ✓</Typography>
              : (
                <List dense disablePadding>
                  {todos.map((t, i) => (
                    <ListItem key={i} disablePadding sx={{ py: 0.5 }}
                      secondaryAction={
                        <Button size="small" onClick={() => navigate(t.path)}>处理</Button>
                      }>
                      <ListItemIcon sx={{ minWidth: 32 }}>
                        <Box sx={{ color: t.color }}>{t.icon}</Box>
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
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <AutoAwesomeIcon fontSize="small" color="primary" />
              <Typography variant="subtitle2" fontWeight={600}>AI 智能推荐</Typography>
            </Stack>
            <Stack spacing={1}>
              {aiRecs.map((r, i) => (
                <Paper key={i} variant="outlined" sx={{ p: 1.5 }}>
                  <Stack direction="row" spacing={1} alignItems="flex-start">
                    <Chip label={r.type} size="small" color="primary" variant="outlined" sx={{ flexShrink: 0 }} />
                    <Box>
                      <Typography variant="body2" fontWeight={600}>{r.title}</Typography>
                      <Typography variant="caption" color="text.secondary">{r.detail}</Typography>
                    </Box>
                  </Stack>
                </Paper>
              ))}
            </Stack>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  )
}

function systemAlertsQuery() {
  return import('@/api/system').then(m => m.systemApi.alertActive())
}

export default function DashboardPage() {
  const [days, setDays] = useState(30)
  const [funnelChannel, setFunnelChannel] = useState('')
  const [role, setRole] = useState<'admin' | 'org' | 'talent'>('admin')
  const navigate = useNavigate()

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['dashboard-admin-stats'],
    queryFn: () => dashboardApi.adminStats(),
    staleTime: 5 * 60 * 1000,
  })

  const { data: kpi, isLoading: kpiLoading } = useQuery({
    queryKey: ['dashboard-kpi', days],
    queryFn: () => dashboardApi.kpiUnified(days),
    staleTime: 5 * 60 * 1000,
  })

  const { data: funnel, isLoading: funnelLoading } = useQuery({
    queryKey: ['dashboard-funnel', days, funnelChannel],
    queryFn: () => dashboardApi.conversionFunnel(days),
    staleTime: 5 * 60 * 1000,
  })

  const { data: productsRaw } = useQuery({
    queryKey: ['dashboard-products', days],
    queryFn: () => dashboardApi.productGmvSummary(days),
    staleTime: 5 * 60 * 1000,
  })
  const products = Array.isArray(productsRaw) ? productsRaw : []

  const { data: cockpitRaw, isLoading: cockpitLoading } = useQuery({
    queryKey: ['dashboard-cockpit', days],
    queryFn: () => dashboardApi.cockpitPreview(days),
    staleTime: 5 * 60 * 1000,
  })
  const cockpit = Array.isArray(cockpitRaw) ? cockpitRaw : []

  const { data: liveFormatsRaw } = useQuery({
    queryKey: ['dashboard-live-format-gmv', days],
    queryFn: () => dashboardApi.liveFormatGmv(days),
    staleTime: 5 * 60 * 1000,
  })
  const liveFormats = Array.isArray(liveFormatsRaw) ? liveFormatsRaw : []

  const { data: aiDistRaw } = useQuery({
    queryKey: ['dashboard-ai-dist', days],
    queryFn: () => aiApi.callTypeDistribution({ days }),
    staleTime: 5 * 60 * 1000,
  })
  const aiDist: AiCallTypeDistributionItem[] = Array.isArray(aiDistRaw) ? aiDistRaw : []

  const trendDates = cockpit.map(r => r.date ?? '')
  // AI forecast dates (extend past last real date)
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
    lineStyle: { type: 'dashed', color: '#ff9800' },
    itemStyle: { color: '#ff9800' },
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
        data: cockpit.map(r => r.gmv ?? 0), itemStyle: { color: '#1976d2' }, areaStyle: { opacity: 0.08 } },
      { name: '场次数', type: 'bar', yAxisIndex: 1,
        data: cockpit.map(r => r.sessions ?? 0), itemStyle: { color: '#4caf50' } },
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
      itemStyle: { color: '#9c27b0' },
      label: { show: true, position: 'top', fontSize: 10, formatter: (p: { value: number }) => fmtMoney(p.value) },
    }],
    grid: { left: 50, right: 20, bottom: 40, top: 36 },
  }

  const pieDist = aiDist.map(d => ({ value: d.count, name: d.callType }))
  const pieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 10 } },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '42%'],
      data: pieDist.length > 0 ? pieDist : [{ value: 1, name: '暂无数据' }],
      label: { show: false },
    }],
  }

  const QUICK_ENTRIES = [
    { label: '新建场次', icon: <AddCircleOutlineIcon />, color: 'primary.main', path: '/admin/live/sessions' },
    { label: '知识库', icon: <LibraryBooksIcon />, color: 'secondary.main', path: '/admin/ai/knowledge' },
    { label: '进化爆款分析', icon: <WhatshotIcon />, color: 'warning.main', path: ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION },
    { label: '话术排行', icon: <TrendingUpIcon />, color: 'success.main', path: '/admin/live/ranking' },
  ]

  const funnelSteps = funnel && typeof funnel === 'object' && 'exposure' in funnel ? [
    { label: '曝光', value: funnel.exposure },
    { label: '点击', value: funnel.clicks },
    { label: '加购', value: funnel.addToCart },
    { label: '下单', value: funnel.orders },
    { label: '支付', value: funnel.payments },
  ] : []

  return (
    <Box sx={{ height: 'calc(100vh - 48px)', overflowY: 'auto', p: 1.5 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>管理驾驶舱</Typography>
      {/* ===== 角色切换器 ===== */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">运营指挥中心</Typography>
          <ToggleButtonGroup size="small" exclusive value={role} onChange={(_, v) => v && setRole(v)}>
            <ToggleButton value="admin"><PeopleIcon fontSize="small" sx={{ mr: 0.5 }} />管理员视图</ToggleButton>
            <ToggleButton value="org"><LiveTvIcon fontSize="small" sx={{ mr: 0.5 }} />机构视图</ToggleButton>
            <ToggleButton value="talent"><VideoLibraryIcon fontSize="small" sx={{ mr: 0.5 }} />达人视图</ToggleButton>
          </ToggleButtonGroup>
        </Stack>
      </Paper>
      {/* ===== 实时场次状态 ===== */}
      <LiveSessionsPanel />
      {/* ===== 待处理 + AI推荐 ===== */}
      <TodoAndAiPanel />
      {/* ===== 第一行：核心 KPI + GMV目标进度 ===== */}
      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Stack direction="row" alignItems="center" spacing={0} divider={<Divider orientation="vertical" flexItem />}
            sx={{ overflowX: 'auto' }}>
            {role === 'admin' && (
              <>
                <KpiBox label="今日GMV" value={fmtMoney(kpi?.gmvToday)} trend={kpi?.gmvMom} loading={kpiLoading} />
                <KpiBox label="今日订单" value={fmt(kpi?.ordersToday)} loading={kpiLoading} />
                <KpiBox label="客单价" value={fmtMoney(kpi?.avgOrderValue)} loading={kpiLoading} />
                <KpiBox label="转化率" value={fmtPct(kpi?.conversionRate)} loading={kpiLoading} />
                <KpiBox label="进行中场次" value={fmt(kpi?.activeSessionCount ?? kpi?.liveSessions)} loading={kpiLoading} />
                <KpiBox label="AI调用" value={fmt(kpi?.aiCallCount ?? kpi?.aiCallsToday)} loading={kpiLoading} />
                <KpiBox label="已发布视频" value={fmt(kpi?.publishedVideos)} loading={kpiLoading} />
                <KpiBox label="知识库文档" value={fmt(kpi?.docCount)} loading={kpiLoading} />
              </>
            )}
            {role === 'org' && (
              <>
                <KpiBox label="机构GMV" value={fmtMoney(kpi?.gmvToday)} trend={kpi?.gmvMom} loading={kpiLoading} />
                <KpiBox label="活跃达人" value={fmt(kpi?.activeSessionCount)} loading={kpiLoading} />
                <KpiBox label="场次数" value={fmt(kpi?.liveSessions)} loading={kpiLoading} />
                <KpiBox label="平均客单" value={fmtMoney(kpi?.avgOrderValue)} loading={kpiLoading} />
                <KpiBox label="转化率" value={fmtPct(kpi?.conversionRate)} loading={kpiLoading} />
                <KpiBox label="话术使用" value={fmt(kpi?.aiCallCount)} loading={kpiLoading} />
              </>
            )}
            {role === 'talent' && (
              <>
                <KpiBox label="我的GMV" value={fmtMoney(kpi?.gmvToday)} trend={kpi?.gmvMom} loading={kpiLoading} />
                <KpiBox label="今日订单" value={fmt(kpi?.ordersToday)} loading={kpiLoading} />
                <KpiBox label="场次时长" value={fmt(kpi?.liveSessions) + 'h'} loading={kpiLoading} />
                <KpiBox label="粉丝增长" value={fmt(kpi?.activeSessionCount)} loading={kpiLoading} />
                <KpiBox label="话术使用" value={fmt(kpi?.aiCallCount)} loading={kpiLoading} />
                <KpiBox label="视频发布" value={fmt(kpi?.publishedVideos)} loading={kpiLoading} />
              </>
            )}
          </Stack>
          {/* GMV 目标完成率进度条 */}
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

      {/* ===== 快捷入口 ===== */}
      <Grid container spacing={1.5} sx={{ mb: 2 }}>
        {QUICK_ENTRIES.map(e => (
          <Grid item xs={6} sm={3} key={e.label}>
            <Button
              variant="outlined"
              fullWidth
              startIcon={<Box sx={{ color: e.color, display: 'flex' }}>{e.icon}</Box>}
              onClick={() => navigate(e.path)}
              sx={{ justifyContent: 'flex-start', py: 1, textTransform: 'none', borderColor: 'divider' }}
            >
              <Typography variant="body2" fontWeight={600}>{e.label}</Typography>
            </Button>
          </Grid>
        ))}
      </Grid>

      {/* ===== 时间范围切换 ===== */}
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1.5}>
        <Typography variant="subtitle2" fontWeight={600} color="text.secondary">数据看板</Typography>
        <ToggleButtonGroup size="small" exclusive value={days} onChange={(_, v) => v && setDays(v)}>
          <ToggleButton value={7} sx={{ fontSize: 12, py: 0.3, px: 1.5 }}>近7天</ToggleButton>
          <ToggleButton value={30} sx={{ fontSize: 12, py: 0.3, px: 1.5 }}>近30天</ToggleButton>
          <ToggleButton value={90} sx={{ fontSize: 12, py: 0.3, px: 1.5 }}>近90天</ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      {/* ===== 第二行：运营统计卡片 ===== */}
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

      {/* ===== 第三行：趋势图 + AI分布饼图 ===== */}
      <Grid container spacing={1.5} mb={1.5}>
        <Grid item xs={12} md={8}>
          <Card variant="outlined">
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>GMV & 场次趋势</Typography>
              {cockpitLoading ? <Skeleton height={240} /> : (
                cockpit.length > 0
                  ? <ReactECharts option={trendOption} style={{ height: 240 }} />
                  : <Box sx={{ height: 240, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Typography color="text.secondary">暂无数据</Typography>
                    </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>AI调用类型分布</Typography>
              <ReactECharts option={pieOption} style={{ height: 240 }} />
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* ===== 第三点五行：直播格式GMV分布 ===== */}
      {liveFormats.length > 0 && (
        <Grid container spacing={1.5} mb={1.5}>
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                <Typography variant="subtitle2" fontWeight={600} mb={1}>直播格式GMV分布</Typography>
                <ReactECharts option={liveFormatOption} style={{ height: 200 }} />
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* ===== 第四行：转化漏斗 + 商品 GMV ===== */}
      <Grid container spacing={1.5}>
        <Grid item xs={12} md={5}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
                <Typography variant="subtitle2" fontWeight={600}>转化漏斗</Typography>
                <Box sx={{ flex: 1 }} />
                <TextField select size="small" label="渠道" value={funnelChannel}
                  onChange={e => setFunnelChannel(e.target.value)} sx={{ width: 100 }}>
                  <MenuItem value="">全部</MenuItem>
                  <MenuItem value="live">直播带货</MenuItem>
                  <MenuItem value="shortvideo">短视频引流</MenuItem>
                  <MenuItem value="search">搜索</MenuItem>
                  <MenuItem value="recommend">推荐流</MenuItem>
                </TextField>
              </Stack>
              {funnelLoading ? <Skeleton height={200} /> : funnelSteps.length === 0 ? (
                <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography color="text.secondary">暂无漏斗数据</Typography>
                </Box>
              ) : (
                <ReactECharts option={{
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
                      itemStyle: { color: ['#1976d2','#2196f3','#42a5f5','#64b5f6','#90caf9'][i] ?? '#90caf9' },
                    })),
                  }],
                  grid: { left: 0, right: 0, top: 0, bottom: 0 },
                }} style={{ height: 200 }} />
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
                <AttachMoneyIcon sx={{ fontSize: 16, color: 'success.main', ml: 'auto' }} />
              </Stack>
              {!products || products.length === 0 ? (
                <Typography variant="body2" color="text.disabled" textAlign="center" py={3}>暂无数据</Typography>
              ) : (
                <Stack spacing={0.8}>
                  {products.slice(0, 6).map((p, i) => (
                    <Stack key={p.productId} direction="row" alignItems="center" spacing={1}>
                      <Typography variant="caption" sx={{ width: 16, color: i < 3 ? 'warning.main' : 'text.disabled', fontWeight: 700 }}>{i + 1}</Typography>
                      <Typography variant="caption" sx={{ flex: 1 }} noWrap>{p.productName}</Typography>
                      <Typography variant="caption" color="text.secondary">{p.totalOrders}单</Typography>
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

