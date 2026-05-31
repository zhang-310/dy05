import { useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Grid, LinearProgress, MenuItem, Stack, TextField, Typography, useTheme } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'
import { PageHeader } from '@/components/base'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'

interface TrendRow {
  date?: string; time?: string
  playCount?: number; likeCount?: number; commentCount?: number; shareCount?: number
  completionRate?: number; finishRate?: number
  [key: string]: unknown
}

interface QualityTrendRow {
  date?: string; day?: string
  score?: number; avgScore?: number
  [key: string]: unknown
}

interface RankRow {
  rank?: number
  model?: string
  cameraType?: string
  avgScore?: number
  count?: number
  [key: string]: unknown
}

type MetricTone = 'primary' | 'secondary' | 'warning' | 'success'

const METRICS = [
  { key: 'playCount', label: '播放量', tone: 'primary' },
  { key: 'likeCount', label: '点赞量', tone: 'secondary' },
  { key: 'commentCount', label: '评论量', tone: 'warning' },
  { key: 'shareCount', label: '分享量', tone: 'success' },
] satisfies Array<{ key: keyof Pick<TrendRow, 'playCount' | 'likeCount' | 'commentCount' | 'shareCount'>; label: string; tone: MetricTone }>
const DASHBOARD_TREND_ENDPOINT = '/short-video/dashboard/trend'
const DASHBOARD_STATS_ENDPOINT = '/short-video/dashboard/stats'
const QUALITY_OVERVIEW_ENDPOINT = '/short-video/quality-dashboard/overview'
const QUALITY_TREND_ENDPOINT = '/short-video/quality-dashboard/trend'
const QUALITY_MODEL_RANKING_ENDPOINT = '/short-video/quality-dashboard/model-ranking'
const QUALITY_CAMERA_RANKING_ENDPOINT = '/short-video/quality-dashboard/camera-ranking'
const QUALITY_AI_REFLECTIONS_ENDPOINT = '/short-video/quality-dashboard/ai-reflections'
const FEEDBACK_WEEKLY_REPORT_ENDPOINT = '/short-video/feedback/weekly-report'
const QUALITY_READY_ENDPOINTS = [
  DASHBOARD_TREND_ENDPOINT,
  DASHBOARD_STATS_ENDPOINT,
  QUALITY_OVERVIEW_ENDPOINT,
  QUALITY_TREND_ENDPOINT,
  QUALITY_MODEL_RANKING_ENDPOINT,
  QUALITY_CAMERA_RANKING_ENDPOINT,
  QUALITY_AI_REFLECTIONS_ENDPOINT,
  FEEDBACK_WEEKLY_REPORT_ENDPOINT,
].join('|')
const QUALITY_READY_ROUTES = [
  shortvideoRoutes.quality,
  `${shortvideoRoutes.quality}?days=:days`,
  shortvideoRoutes.dashboard,
].join('|')
const QUALITY_SUPPORTED_ACTIONS = [
  'refresh-quality-dashboard',
  'switch-quality-time-range',
  'view-dashboard-trend',
  'view-quality-trend',
  'view-model-ranking',
  'view-camera-ranking',
  'view-ai-reflections',
].join('|')
const QUALITY_UNSUPPORTED_ENDPOINTS = [
  '/short-video/quality-dashboard/mock',
  '/short-video/quality-dashboard/local-overview',
  '/short-video/quality-dashboard/local-trend',
  '/short-video/quality-dashboard/local-model-ranking',
  '/short-video/quality-dashboard/local-camera-ranking',
  '/short-video/quality-dashboard/local-reflections',
  '/short-video/feedback/local-weekly-report',
  '/short-video/dashboard/static-trend',
  '/short-video/dashboard/static-stats',
].join('|')

export default function QualityPage() {
  const theme = useTheme()
  const [days, setDays] = useState(7)
  const toneColor = (tone: MetricTone) =>
    theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  const metricColors = METRICS.map(m => toneColor(m.tone))
  const qualityTrendColor = toneColor('primary')
  const completionRateColor = toneColor('secondary')

  const {
    data: trendData,
    isLoading: trendLoading,
    isError: trendIsError,
    error: trendError,
    refetch: refetchTrend,
  } = useQuery({
    queryKey: ['sv-data-trend', days],
    queryFn: () => shortvideoApi.dataTrend({ days }),
  })
  const trend = Array.isArray(trendData) ? trendData as TrendRow[] : [] as TrendRow[]

  const {
    data: statsData,
    isLoading: statsLoading,
    isError: statsIsError,
    error: statsError,
    refetch: refetchStats,
  } = useQuery({
    queryKey: ['sv-dashboard-stats'],
    queryFn: () => shortvideoApi.dashboardStats(),
  })
  const stats = statsData ?? {}

  const {
    data: qualityOverview,
    isLoading: qualityLoading,
    isError: qualityIsError,
    error: qualityError,
    refetch: refetchQuality,
  } = useQuery({
    queryKey: ['sv-quality-overview'],
    queryFn: () => shortvideoApi.qualityOverview(),
  })
  const quality = qualityOverview ?? {}

  const {
    data: qualityTrendData,
    isLoading: qualityTrendLoading,
    isError: qualityTrendIsError,
    error: qualityTrendError,
    refetch: refetchQualityTrend,
  } = useQuery({
    queryKey: ['sv-quality-trend', days],
    queryFn: () => shortvideoApi.qualityTrend({ days }),
  })
  const qualityTrend = (Array.isArray(qualityTrendData) ? qualityTrendData : []) as QualityTrendRow[]

  const {
    data: modelRankData,
    isLoading: modelRankLoading,
    isError: modelRankIsError,
    error: modelRankError,
    refetch: refetchModelRank,
  } = useQuery({
    queryKey: ['sv-quality-model-ranking', days],
    queryFn: () => shortvideoApi.qualityModelRanking({ days }),
  })
  const modelRank = (Array.isArray(modelRankData) ? modelRankData : []) as RankRow[]

  const {
    data: cameraRankData,
    isLoading: cameraRankLoading,
    isError: cameraRankIsError,
    error: cameraRankError,
    refetch: refetchCameraRank,
  } = useQuery({
    queryKey: ['sv-quality-camera-ranking', days],
    queryFn: () => shortvideoApi.qualityCameraRanking({ days }),
  })
  const cameraRank = (Array.isArray(cameraRankData) ? cameraRankData : []) as RankRow[]

  const {
    data: feedbackReport,
    isLoading: feedbackLoading,
    isError: feedbackIsError,
    error: feedbackError,
    refetch: refetchFeedback,
  } = useQuery({
    queryKey: ['sv-feedback-weekly'],
    queryFn: () => shortvideoApi.feedbackWeeklyReport(),
  })

  const {
    data: reflections = [],
    isLoading: reflectionsLoading,
    isError: reflectionsIsError,
    error: reflectionsError,
    refetch: refetchReflections,
  } = useQuery({
    queryKey: ['sv-quality-reflections'],
    queryFn: () => shortvideoApi.qualityAiReflections(),
  })

  const dates = trend.map(r => String(r.date ?? r.time ?? ''))

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: METRICS.map(m => m.label), top: 0 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '数量' },
    series: METRICS.map(m => ({
      name: m.label,
      type: 'line',
      smooth: true,
      data: trend.map(r => r[m.key] ?? 0),
      lineStyle: { color: toneColor(m.tone) },
      itemStyle: { color: toneColor(m.tone) },
    })),
    grid: { left: 60, right: 20, bottom: 60, top: 40 },
  }

  const kpiEntries = [
    ...(stats ? Object.entries(stats).filter(([, v]) => typeof v === 'number' || typeof v === 'string') : []),
    ...(quality ? Object.entries(quality).filter(([, v]) => typeof v === 'number' || typeof v === 'string') : []),
  ]

  return (
    <Box
      data-testid="shortvideo-quality-page"
      data-ready-endpoints={QUALITY_READY_ENDPOINTS}
      data-ready-routes={QUALITY_READY_ROUTES}
      data-supported-actions={QUALITY_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={QUALITY_UNSUPPORTED_ENDPOINTS}
      data-no-local-quality-fallback="true"
      data-no-static-dashboard-fallback="true"
      data-no-template-reflections="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="短视频质量看板"
        breadcrumbs={[{ label: '短视频' }, { label: '质量看板' }]}
        subtitle="聚合 dashboard 趋势、质量评分趋势和反馈周报；每条链路独立失败时保持其余数据可用。"
        actions={
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => {
              refetchTrend()
              refetchStats()
              refetchQuality()
              refetchQualityTrend()
              refetchFeedback()
              refetchModelRank()
              refetchCameraRank()
              refetchReflections()
            }}
            data-testid="shortvideo-quality-refresh-button"
            data-source-endpoint={DASHBOARD_TREND_ENDPOINT}
          >
            刷新
          </Button>
        }
      />
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField select size="small" label="时间范围" value={days}
          onChange={e => setDays(Number(e.target.value))} sx={{ width: 120 }} data-testid="shortvideo-quality-days-select">
          {[7, 14, 30].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
        </TextField>
      </Stack>
      {(trendLoading || statsLoading || qualityLoading || qualityTrendLoading || feedbackLoading || modelRankLoading || cameraRankLoading || reflectionsLoading) && <LinearProgress />}
      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-quality-boundary-contract"
        data-source-endpoints={QUALITY_READY_ENDPOINTS}
        data-no-local-quality-fallback="true"
        data-no-static-dashboard-fallback="true"
        data-supported-actions={QUALITY_SUPPORTED_ACTIONS}
      >
        质量概览来自 POST {QUALITY_OVERVIEW_ENDPOINT}，互动趋势来自 POST {DASHBOARD_TREND_ENDPOINT}，模型/运镜排名来自 POST {QUALITY_MODEL_RANKING_ENDPOINT} / {QUALITY_CAMERA_RANKING_ENDPOINT}，AI 反思来自 POST {QUALITY_AI_REFLECTIONS_ENDPOINT}。
      </Alert>
      {trendIsError && (
        <Alert severity="error" data-testid="shortvideo-quality-data-trend-error" data-no-static-dashboard-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchTrend()}>重试</Button>}>
          互动趋势加载失败（POST {DASHBOARD_TREND_ENDPOINT}）：{getErrorMessage(trendError)}。趋势图不会用假播放量补齐。
        </Alert>
      )}
      {statsIsError && (
        <Alert severity="error" data-testid="shortvideo-quality-stats-error" data-no-static-dashboard-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchStats()}>重试</Button>}>
          Dashboard 统计加载失败（POST {DASHBOARD_STATS_ENDPOINT}）：{getErrorMessage(statsError)}。KPI 不会使用静态默认值。
        </Alert>
      )}
      {qualityIsError && (
        <Alert severity="error" data-testid="shortvideo-quality-overview-error" data-no-local-quality-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchQuality()}>重试</Button>}>
          质量概览加载失败（POST {QUALITY_OVERVIEW_ENDPOINT}）：{getErrorMessage(qualityError)}。质量均分和关键指标保持空态。
        </Alert>
      )}
      {qualityTrendIsError && (
        <Alert severity="error" data-testid="shortvideo-quality-trend-error" data-no-local-quality-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchQualityTrend()}>重试</Button>}>
          质量趋势加载失败（POST {QUALITY_TREND_ENDPOINT}）：{getErrorMessage(qualityTrendError)}。质量趋势图保持空态。
        </Alert>
      )}
      {feedbackIsError && (
        <Alert severity="warning" data-testid="shortvideo-quality-feedback-error" data-no-local-feedback-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchFeedback()}>重试</Button>}>
          反馈周报不可用（POST {FEEDBACK_WEEKLY_REPORT_ENDPOINT}）：{getErrorMessage(feedbackError)}。不影响趋势和质量概览展示。
        </Alert>
      )}
      {modelRankIsError && (
        <Alert severity="warning" data-testid="shortvideo-quality-model-rank-error" data-no-local-quality-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchModelRank()}>重试</Button>}>
          模型排名加载失败（POST {QUALITY_MODEL_RANKING_ENDPOINT}）：{getErrorMessage(modelRankError)}。不展示静态模型榜。
        </Alert>
      )}
      {cameraRankIsError && (
        <Alert severity="warning" data-testid="shortvideo-quality-camera-rank-error" data-no-local-quality-fallback="true" action={<Button color="inherit" size="small" onClick={() => refetchCameraRank()}>重试</Button>}>
          运镜排名加载失败（POST {QUALITY_CAMERA_RANKING_ENDPOINT}）：{getErrorMessage(cameraRankError)}。不展示静态运镜榜。
        </Alert>
      )}
      {reflectionsIsError && (
        <Alert severity="warning" data-testid="shortvideo-quality-reflections-error" data-no-template-reflections="true" action={<Button color="inherit" size="small" onClick={() => refetchReflections()}>重试</Button>}>
          AI 反思加载失败（POST {QUALITY_AI_REFLECTIONS_ENDPOINT}）：{getErrorMessage(reflectionsError)}。不展示模板化反思。
        </Alert>
      )}

      {kpiEntries.length > 0 && (
        <Grid container spacing={2} data-testid="shortvideo-quality-kpi-contract" data-source-endpoints={`${DASHBOARD_STATS_ENDPOINT}|${QUALITY_OVERVIEW_ENDPOINT}`} data-no-static-dashboard-fallback="true" data-no-local-quality-fallback="true">
          {kpiEntries.slice(0, 8).map(([k, v]) => (
            <Grid item xs={6} sm={3} key={k}>
              <Card variant="outlined">
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Typography variant="caption" color="text.secondary">{k}</Typography>
                  <Typography variant="h6" fontWeight={700}>{String(v)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
      {kpiEntries.length === 0 && !statsLoading && !qualityLoading && !statsIsError && !qualityIsError && (
        <Alert severity="info" data-testid="shortvideo-quality-kpi-empty" data-no-static-dashboard-fallback="true" data-no-local-quality-fallback="true">暂无统计指标。请先完成短视频发布、质量评分或反馈入库。</Alert>
      )}

      {feedbackReport && Object.keys(feedbackReport as object).length > 0 && (
        <Card variant="outlined" data-testid="shortvideo-quality-feedback-report-contract" data-source-endpoint={FEEDBACK_WEEKLY_REPORT_ENDPOINT} data-no-local-feedback-fallback="true">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>反馈周报（摘要）</Typography>
            <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', m: 0 }}>
              {JSON.stringify(feedbackReport, null, 2)}
            </Typography>
          </CardContent>
        </Card>
      )}
      {!feedbackReport && !feedbackLoading && !feedbackIsError && (
        <Alert severity="info" data-testid="shortvideo-quality-feedback-empty" data-no-local-feedback-fallback="true">暂无反馈周报数据。收集评分、评论或人工反馈后，这里会展示周维度摘要。</Alert>
      )}

      {qualityTrend.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>质量趋势（近 {days} 天）</Typography>
            <Box data-testid="shortvideo-quality-score-trend-chart-surface" data-source-endpoint={QUALITY_TREND_ENDPOINT} data-no-local-quality-fallback="true" data-chart-color={qualityTrendColor}>
              <ReactECharts
                option={{
                  tooltip: { trigger: 'axis' },
                  xAxis: { type: 'category', data: qualityTrend.map(r => String(r.date ?? r.day ?? '')) },
                  yAxis: { type: 'value' },
                  series: [{
                    type: 'line',
                    smooth: true,
                    data: qualityTrend.map(r => Number(r.score ?? r.avgScore ?? 0)),
                    lineStyle: { color: qualityTrendColor },
                    itemStyle: { color: qualityTrendColor },
                  }],
                  grid: { left: 48, right: 16, bottom: 40, top: 24 },
                }}
                style={{ height: 260 }}
              />
            </Box>
          </CardContent>
        </Card>
      )}
      {qualityTrend.length === 0 && !qualityTrendLoading && !qualityTrendIsError && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>质量趋势（近 {days} 天）</Typography>
            <Alert severity="info" data-testid="shortvideo-quality-trend-empty" data-no-local-quality-fallback="true">暂无质量趋势数据。请确认质量评分任务已落库。</Alert>
          </CardContent>
        </Card>
      )}

      {trend.length > 0 ? (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>数据趋势</Typography>
            <Box data-testid="shortvideo-quality-data-trend-chart-surface" data-source-endpoint={DASHBOARD_TREND_ENDPOINT} data-no-static-dashboard-fallback="true" data-chart-colors={metricColors.join('|')}>
              <ReactECharts option={trendOption} style={{ height: 340 }} />
            </Box>
          </CardContent>
        </Card>
      ) : (
        <Card variant="outlined">
          <CardContent>
            <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary" data-testid="shortvideo-quality-data-trend-empty" data-no-static-dashboard-fallback="true">暂无趋势数据</Typography>
            </Box>
          </CardContent>
        </Card>
      )}

      {trend.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>完播率趋势</Typography>
            <Box data-testid="shortvideo-quality-completion-chart-surface" data-source-endpoint={DASHBOARD_TREND_ENDPOINT} data-no-static-dashboard-fallback="true" data-chart-color={completionRateColor}>
              <ReactECharts
                option={{
                  tooltip: { trigger: 'axis' },
                  xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30, fontSize: 11 } },
                  yAxis: { type: 'value', name: '完播率', axisLabel: { formatter: (v: number) => `${(v * 100).toFixed(0)}%` } },
                  series: [{
                    type: 'bar',
                    data: trend.map(r => r.completionRate ?? r.finishRate ?? 0),
                    itemStyle: { color: completionRateColor },
                  }],
                  grid: { left: 60, right: 20, bottom: 60, top: 20 },
                }}
                style={{ height: 220 }}
              />
            </Box>
          </CardContent>
        </Card>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card variant="outlined" data-testid="shortvideo-quality-model-ranking-contract" data-source-endpoint={QUALITY_MODEL_RANKING_ENDPOINT} data-no-local-quality-fallback="true">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                <Typography variant="subtitle2">模型排名</Typography>
                <Typography variant="caption" color="text.secondary">真实 `/quality-dashboard/model-ranking`</Typography>
              </Stack>
              {modelRankLoading ? (
                <LinearProgress />
              ) : modelRank.length > 0 ? (
                <Stack spacing={1}>
                  {modelRank.map((row, idx) => (
                    <Stack key={idx} direction="row" justifyContent="space-between" alignItems="center">
                      <Typography variant="body2">{row.rank ?? idx + 1}. {String(row.model ?? row.cameraType ?? '-')}</Typography>
                      <Typography variant="body2" fontWeight={600}>{Number(row.avgScore ?? 0).toFixed(1)}</Typography>
                    </Stack>
                  ))}
                </Stack>
              ) : (
                <Alert severity="info" data-testid="shortvideo-quality-model-ranking-empty" data-no-local-quality-fallback="true">暂无模型排名数据。请先生成带质量分的短视频任务。</Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card variant="outlined" data-testid="shortvideo-quality-camera-ranking-contract" data-source-endpoint={QUALITY_CAMERA_RANKING_ENDPOINT} data-no-local-quality-fallback="true">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                <Typography variant="subtitle2">运镜排名</Typography>
                <Typography variant="caption" color="text.secondary">真实 `/quality-dashboard/camera-ranking`</Typography>
              </Stack>
              {cameraRankLoading ? (
                <LinearProgress />
              ) : cameraRank.length > 0 ? (
                <Stack spacing={1}>
                  {cameraRank.map((row, idx) => (
                    <Stack key={idx} direction="row" justifyContent="space-between" alignItems="center">
                      <Typography variant="body2">{row.rank ?? idx + 1}. {String(row.cameraType ?? '-')}</Typography>
                      <Typography variant="body2" fontWeight={600}>{Number(row.avgScore ?? 0).toFixed(1)}</Typography>
                    </Stack>
                  ))}
                </Stack>
              ) : (
                <Alert severity="info" data-testid="shortvideo-quality-camera-ranking-empty" data-no-local-quality-fallback="true">暂无运镜排名数据。请先让分镜/剪辑任务落库质量评分。</Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card variant="outlined" data-testid="shortvideo-quality-reflections-contract" data-source-endpoint={QUALITY_AI_REFLECTIONS_ENDPOINT} data-no-template-reflections="true">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
            <Typography variant="subtitle2">AI 反思</Typography>
            <Typography variant="caption" color="text.secondary">真实 `/quality-dashboard/ai-reflections`</Typography>
          </Stack>
          {reflectionsLoading ? (
            <LinearProgress />
          ) : reflections.length > 0 ? (
            <Stack spacing={1}>
              {reflections.map((item, idx) => (
                <Typography key={idx} variant="body2" color="text.secondary" data-testid="shortvideo-quality-reflection-item">• {item}</Typography>
              ))}
            </Stack>
          ) : (
            <Alert severity="info" data-testid="shortvideo-quality-reflections-empty" data-no-template-reflections="true">暂无 AI 反思数据。请先完成成片生成、质量评分和模型/运镜聚合入库。</Alert>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}
