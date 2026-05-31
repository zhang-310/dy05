import { useMemo, useState } from 'react'
import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  Typography,
  Button,
  CircularProgress,
  Chip,
  Stack,
  Divider,
  Alert,
  Grid,
} from '@mui/material'
import { useTheme } from '@mui/material/styles'
import {
  Movie as VideoIcon,
  Rocket as RocketIcon,
  LocalFireDepartment as ViralIcon,
  CalendarMonth as CalendarIcon,
  Collections as BatchIcon,
  Theaters as DramaIcon,
  TrackChanges as TrackChangesIcon,
  Refresh as RefreshIcon,
  Paid as CostIcon,
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import ReactECharts from 'echarts-for-react'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRecord, normalizeRows } from '@/utils/response-normalize'

const QUICK_CARDS = [
  { id: 'insights', label: '洞见中心', sub: '账号·爆款库·进化分析', icon: TrackChangesIcon, path: shortvideoRoutes.insights },
  { id: 'quick', label: '一键生成', sub: '3分钟成片', icon: RocketIcon, path: shortvideoRoutes.quickGenerate },
  { id: 'viral', label: '爆款视频库', sub: '入库与深度拆解', icon: ViralIcon, path: shortvideoRoutes.viralVideos },
  { id: 'drama', label: '短剧创作', sub: '多集剧本 AI 生成', icon: DramaIcon, path: shortvideoRoutes.drama },
  { id: 'batch', label: '批量生产', sub: '高效批量', icon: BatchIcon, path: shortvideoRoutes.projects },
  { id: 'calendar', label: '内容日历', sub: '排期与发布', icon: CalendarIcon, path: shortvideoRoutes.contentCalendar },
]

const TREND_OPTIONS = [{ label: '近7天', days: 7 }, { label: '近30天', days: 30 }]
const DASHBOARD_STATS_ENDPOINT = '/short-video/dashboard/stats'
const DASHBOARD_TREND_ENDPOINT = '/short-video/dashboard/trend'
const DASHBOARD_PROJECTS_ENDPOINT = '/short-video/dashboard/projects'
const DASHBOARD_COST_ENDPOINT = '/short-video/dashboard/cost-breakdown'
const DASHBOARD_READY_ENDPOINTS = [
  DASHBOARD_STATS_ENDPOINT,
  DASHBOARD_TREND_ENDPOINT,
  DASHBOARD_PROJECTS_ENDPOINT,
  DASHBOARD_COST_ENDPOINT,
].join('|')
const DASHBOARD_UNSUPPORTED_ENDPOINTS = [
  '/short-video/dashboard/mock',
  '/short-video/dashboard/local-stats',
  '/short-video/dashboard/local-trend',
  '/short-video/dashboard/local-projects',
  '/short-video/dashboard/local-cost-breakdown',
  '/short-video/dashboard/static-kpi',
  '/short-video/dashboard/prewarm',
  '/short-video/project/mock',
  '/short-video/content/mock-trend',
].join('|')
const DASHBOARD_READY_ROUTES = QUICK_CARDS.map((card) => card.path).join('|')
const DASHBOARD_SUPPORTED_ACTIONS = [
  'refresh-dashboard',
  'navigate-insights',
  'navigate-quick-generate',
  'navigate-viral-videos',
  'navigate-drama',
  'navigate-projects',
  'navigate-content-calendar',
  'switch-trend-range',
].join('|')

const KPI_CARDS = [
  { key: 'totalVideoCount', label: '成片项目', hint: 'finalVideoUrl 已生成' },
  { key: 'totalPlayCount', label: '总播放量', hint: '来自入库视频指标' },
  { key: 'totalCost', label: '估算成本', hint: '脚本/视频/配音/存储' },
  { key: 'roi', label: 'ROI', hint: '播放/千次 ÷ 成本' },
] as const

function formatMetricValue(value: unknown, key?: string) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return String(value ?? '-')
  if (key === 'totalCost') return `¥${num.toFixed(2)}`
  if (key === 'roi') return num.toFixed(2)
  return num.toLocaleString()
}

function getProjectTitle(row: Record<string, unknown>, fallback: string) {
  return String(row.title ?? row.name ?? fallback)
}

export default function ShortVideoDashboardPage() {
  const theme = useTheme()
  const navigate = useNavigate()
  const [days, setDays] = useState(7)

  const {
    data: stats,
    isLoading: loadingStats,
    isError: statsIsError,
    error: statsError,
    refetch: refetchStats,
  } = useQuery({
    queryKey: ['sv-dashboard-stats'],
    queryFn: () => shortvideoApi.dashboardStats(),
  })

  const {
    data: projects = [],
    isLoading: loadingProjects,
    isError: projectsIsError,
    error: projectsError,
    refetch: refetchProjects,
  } = useQuery({
    queryKey: ['sv-dashboard-projects-top'],
    queryFn: () => shortvideoApi.dashboardProjects({ page: 0, rows: 6 }),
  })

  const {
    data: costBreakdown,
    isLoading: loadingCost,
    isError: costIsError,
    error: costError,
    refetch: refetchCost,
  } = useQuery({
    queryKey: ['sv-dashboard-cost-breakdown'],
    queryFn: () => shortvideoApi.dashboardCostBreakdown(),
  })

  const {
    data: trend = [],
    isLoading: loadingTrend,
    isError: trendIsError,
    error: trendError,
    refetch: refetchTrend,
  } = useQuery({
    queryKey: ['sv-trend', days],
    queryFn: () => shortvideoApi.dataTrend({ days }),
  })

  const statsData = normalizeRecord(stats, ['dashboardStats', 'stats', 'summary'])
  const costData = normalizeRecord(costBreakdown, ['costBreakdown', 'breakdown', 'costs'])
  const projectRows = normalizeRows<Record<string, unknown>>(projects)
  const trendData = normalizeRows<Record<string, unknown>>(trend)

  const chartOption = useMemo(
    () => ({
      color: [theme.palette.primary.main, theme.palette.secondary.main, theme.palette.warning.main],
      tooltip: { trigger: 'axis' },
      legend: { data: ['播放量', '点赞数', '评论数'], textStyle: { color: theme.palette.text.secondary } },
      grid: { left: 48, right: 24, top: 40, bottom: 32 },
      xAxis: {
        type: 'category',
        data: trendData.map((d) => String(d.date ?? '').slice(5)),
        axisLine: { lineStyle: { color: theme.palette.divider } },
        axisLabel: { color: theme.palette.text.secondary },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: theme.palette.divider } },
        axisLabel: { color: theme.palette.text.secondary },
      },
      series: [
        { name: '播放量', type: 'line', smooth: true, data: trendData.map((d) => Number(d.playCount ?? 0)) },
        { name: '点赞数', type: 'line', smooth: true, data: trendData.map((d) => Number(d.likeCount ?? 0)) },
        { name: '评论数', type: 'line', smooth: true, data: trendData.map((d) => Number(d.commentCount ?? 0)) },
      ],
    }),
    [theme, trendData],
  )

  return (
    <Box
      data-testid="shortvideo-dashboard-page"
      data-ready-endpoints={DASHBOARD_READY_ENDPOINTS}
      data-ready-routes={DASHBOARD_READY_ROUTES}
      data-supported-actions={DASHBOARD_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={DASHBOARD_UNSUPPORTED_ENDPOINTS}
      data-no-local-dashboard-fallback="true"
      data-no-static-kpi-fallback="true"
      data-no-mock-project-fallback="true"
      sx={{ bgcolor: 'background.default', minHeight: '100%', py: 2, px: { xs: 2, md: 3 } }}
    >
      <PageHeader
        title="短视频总览"
        breadcrumbs={[{ label: '短视频' }, { label: '总览' }]}
        subtitle="短视频工作台入口、内容趋势和近期项目状态总览。"
        actions={
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => { refetchStats(); refetchTrend(); refetchProjects(); refetchCost() }}
            disabled={loadingStats || loadingTrend || loadingProjects || loadingCost}
            data-testid="shortvideo-dashboard-refresh-button"
            data-source-endpoints={DASHBOARD_READY_ENDPOINTS}
          >
            刷新
          </Button>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-dashboard-boundary-contract"
        data-no-local-dashboard-fallback="true"
        data-no-static-kpi-fallback="true"
        data-no-local-trend-synthesis="true"
        data-supported-actions={DASHBOARD_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        本页使用真实 POST {DASHBOARD_STATS_ENDPOINT} / {DASHBOARD_TREND_ENDPOINT} / {DASHBOARD_PROJECTS_ENDPOINT} / {DASHBOARD_COST_ENDPOINT}。趋势为后端按已入库视频指标补齐的时间序列，若全为 0 通常表示尚未发布或未同步 `sv_video_data`。
      </Alert>

      {statsIsError && (
        <Alert
          severity="error"
          data-testid="shortvideo-dashboard-stats-error"
          data-no-static-kpi-fallback="true"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetchStats()}>重试</Button>}
        >
          总览指标加载失败（POST {DASHBOARD_STATS_ENDPOINT}）：{getErrorMessage(statsError)}。KPI 不会使用静态默认值。
        </Alert>
      )}

      <Grid
        container
        spacing={2}
        data-testid="shortvideo-dashboard-kpi-contract"
        data-no-client-kpi-synthesis="true"
        data-source-endpoint={DASHBOARD_STATS_ENDPOINT}
        sx={{ mb: 3 }}
      >
        {KPI_CARDS.map((card) => (
          <Grid item xs={12} sm={6} md={3} key={card.key}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="caption" color="text.secondary">{card.label}</Typography>
                <Typography variant="h5" fontWeight={700} sx={{ mt: 0.5 }}>
                  {loadingStats ? '-' : formatMetricValue(statsData[card.key], card.key)}
                </Typography>
                <Typography variant="caption" color="text.secondary">{card.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Stack
        direction="row"
        flexWrap="wrap"
        useFlexGap
        spacing={2}
        data-testid="shortvideo-dashboard-navigation-contract"
        data-navigation-only="true"
        data-no-inline-project-mutation="true"
        sx={{ mb: 3 }}
      >
        {QUICK_CARDS.map((card) => {
          const Icon = card.icon
          return (
            <Box
              key={card.id}
              sx={{
                flex: '1 1 140px',
                minWidth: { xs: '100%', sm: 'calc(50% - 8px)', md: 'calc(33.333% - 11px)', lg: 'calc(20% - 13px)' },
                maxWidth: { lg: 'calc(20% - 13px)' },
              }}
            >
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardActionArea
                  onClick={() => navigate(card.path)}
                  sx={{ py: 1 }}
                  data-testid={`shortvideo-dashboard-card-action-${card.id}`}
                  data-target-route={card.path}
                >
                  <CardContent>
                    <Box sx={{ color: 'primary.main', mb: 1, display: 'flex', justifyContent: 'center' }}>
                      <Icon sx={{ fontSize: 32 }} />
                    </Box>
                    <Typography variant="subtitle2" align="center" fontWeight={600}>
                      {card.label}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" align="center" display="block">
                      {card.sub}
                    </Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Box>
          )
        })}
      </Stack>

      <Card variant="outlined" data-testid="shortvideo-dashboard-cost-card" data-no-default-cost-fallback="true" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
            <CostIcon color="primary" />
            <Typography variant="h6" component="h2">
              成本拆解
            </Typography>
          </Stack>
          {loadingCost ? (
            <CircularProgress sx={{ display: 'block', mx: 'auto', my: 3 }} />
          ) : costIsError ? (
            <Alert
              severity="error"
              data-testid="shortvideo-dashboard-cost-error"
              data-no-default-cost-fallback="true"
              action={<Button color="inherit" size="small" onClick={() => refetchCost()}>重试</Button>}
            >
              成本拆解加载失败（POST {DASHBOARD_COST_ENDPOINT}）：{getErrorMessage(costError)}。页面不会补默认成本。
            </Alert>
          ) : (
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap data-testid="shortvideo-dashboard-cost-breakdown" data-source-endpoint={DASHBOARD_COST_ENDPOINT}>
              {[
                ['scriptCost', '脚本'],
                ['imageCost', '图像'],
                ['videoCost', '视频'],
                ['voiceCost', '配音'],
                ['storageCost', '存储'],
                ['total', '合计'],
              ].map(([key, label]) => (
                <Chip
                  key={key}
                  icon={key === 'total' ? <CostIcon /> : undefined}
                  label={`${label} ${formatMetricValue(costData[key], 'totalCost')}`}
                  color={key === 'total' ? 'primary' : 'default'}
                  variant={key === 'total' ? 'filled' : 'outlined'}
                />
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>

      <Card variant="outlined" data-testid="shortvideo-dashboard-trend-card" data-no-local-trend-synthesis="true" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1} sx={{ mb: 2 }}>
            <Typography variant="h6" component="h2">
              内容数据趋势
            </Typography>
            <Stack direction="row" spacing={1}>
              {TREND_OPTIONS.map((o) => (
                <Chip
                  key={o.days}
                  label={o.label}
                  size="small"
                  onClick={() => setDays(o.days)}
                  color={days === o.days ? 'primary' : 'default'}
                  variant={days === o.days ? 'filled' : 'outlined'}
                />
              ))}
            </Stack>
          </Stack>
          {loadingTrend ? (
            <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
          ) : trendIsError ? (
            <Alert
              severity="error"
              data-testid="shortvideo-dashboard-trend-error"
              data-no-local-trend-synthesis="true"
              action={<Button color="inherit" size="small" onClick={() => refetchTrend()}>重试</Button>}
            >
              趋势加载失败（POST {DASHBOARD_TREND_ENDPOINT}）：{getErrorMessage(trendError)}。趋势图不会用假播放量补齐。
            </Alert>
          ) : trendData.length === 0 ? (
            <Alert severity="info" data-testid="shortvideo-dashboard-trend-empty" data-no-mock-trend-fallback="true">
              暂无趋势数据。完成发布、同步或导入内容数据后，这里会显示播放、点赞和评论趋势。
            </Alert>
          ) : trendData.every((d) => Number(d.playCount ?? 0) === 0 && Number(d.likeCount ?? 0) === 0 && Number(d.commentCount ?? 0) === 0) ? (
            <Alert severity="info" data-testid="shortvideo-dashboard-trend-zero" data-no-client-trend-mutation="true">
              趋势接口已返回日期骨架，但所有指标为 0。请先同步已发布短视频指标或确认 `sv_video_data` 是否有快照数据。
            </Alert>
          ) : (
            <Box data-testid="shortvideo-dashboard-trend-chart" data-source-endpoint={DASHBOARD_TREND_ENDPOINT}>
              <ReactECharts option={chartOption} style={{ height: 300 }} />
            </Box>
          )}
        </CardContent>
      </Card>

      <Card variant="outlined" data-testid="shortvideo-dashboard-projects-card" data-no-mock-project-fallback="true">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6" component="h2">
              近期项目
            </Typography>
            <Button size="small" onClick={() => navigate(shortvideoRoutes.projects)}>
              查看全部
            </Button>
          </Stack>
          <Divider sx={{ mb: 2 }} />
          {loadingProjects ? (
            <CircularProgress sx={{ display: 'block', mx: 'auto', my: 2 }} />
          ) : projectsIsError ? (
            <Alert
              severity="error"
              data-testid="shortvideo-dashboard-projects-error"
              data-no-mock-project-fallback="true"
              action={<Button color="inherit" size="small" onClick={() => refetchProjects()}>重试</Button>}
            >
              项目列表加载失败（POST {DASHBOARD_PROJECTS_ENDPOINT}）：{getErrorMessage(projectsError)}。近期项目不会从本地 mock 补齐。
            </Alert>
          ) : projectRows.length === 0 ? (
            <Alert
              severity="info"
              data-testid="shortvideo-dashboard-projects-empty"
              data-no-mock-project-fallback="true"
              action={<Button color="inherit" size="small" onClick={() => navigate(shortvideoRoutes.quickGenerate)}>去生成</Button>}
            >
              暂无短视频项目。可以从一键生成、脚本规划或爆款复刻开始创建。
            </Alert>
          ) : (
            <Stack divider={<Divider flexItem />} spacing={0} data-testid="shortvideo-dashboard-project-list" data-source-endpoint={DASHBOARD_PROJECTS_ENDPOINT}>
              {projectRows.map((p, i) => (
                <Stack
                  key={i}
                  direction="row"
                  justifyContent="space-between"
                  alignItems="center"
                  spacing={2}
                  sx={{ py: 1.5 }}
                >
                  <Stack direction="row" alignItems="center" spacing={1.5} sx={{ minWidth: 0, flex: 1 }}>
                    <VideoIcon fontSize="small" color="action" />
                    <Typography variant="body2" noWrap title={getProjectTitle(p, `项目${i + 1}`)}>
                      {getProjectTitle(p, `项目${i + 1}`)}
                    </Typography>
                  </Stack>
                  <Stack direction="row" spacing={1} alignItems="center">
                    {p.stage != null && <Chip size="small" label={String(p.stage)} variant="outlined" color="primary" />}
                    {p.progress != null && <Chip size="small" label={`${Number(p.progress ?? 0)}%`} variant="outlined" />}
                    <Chip size="small" label={String(p.status ?? 'draft')} variant="outlined" />
                  </Stack>
                </Stack>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}
