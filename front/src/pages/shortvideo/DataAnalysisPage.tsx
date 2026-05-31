import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Button, CircularProgress,
  Tabs, Tab, Stack, Chip, Grid, Paper,
  FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableHead, TableRow,
  Alert,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import {
  Summarize as ReportIcon,
  Download as DownloadIcon,
  TrendingUp as TrendIcon,
  Insights as InsightsIcon,
  CompareArrows as CompareIcon,
} from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import ReactECharts from 'echarts-for-react'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRecord, normalizeRows } from '@/utils/response-normalize'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

interface SvDataTrendItem { date?: string; playCount?: number; likeCount?: number; commentCount?: number; shareCount?: number }
interface SvDashboardStats { totalVideoCount?: number; totalPlayCount?: number; totalCost?: number; roi?: number; [key: string]: unknown }
const DASHBOARD_TREND_ENDPOINT = '/short-video/dashboard/trend'
const DASHBOARD_STATS_ENDPOINT = '/short-video/dashboard/stats'
const CONTENT_SEARCH_ENDPOINT = '/short-video/content/search'
const DATA_ANALYSIS_READY_ENDPOINTS = [
  DASHBOARD_TREND_ENDPOINT,
  DASHBOARD_STATS_ENDPOINT,
  CONTENT_SEARCH_ENDPOINT,
].join('|')
const DATA_ANALYSIS_READY_ROUTES = [
  shortvideoRoutes.dataAnalysis,
  `${shortvideoRoutes.dataAnalysis}?days=:days`,
  shortvideoRoutes.dashboard,
].join('|')
const DATA_ANALYSIS_SUPPORTED_ACTIONS = [
  'switch-data-analysis-tab',
  'switch-data-analysis-days',
  'view-dashboard-trend',
  'view-dashboard-funnel',
  'view-video-rank',
  'generate-rule-report',
  'export-rule-report',
].join('|')
const DATA_ANALYSIS_UNSUPPORTED_ENDPOINTS = [
  '/short-video/data-analysis/mock',
  '/short-video/data-analysis/local-trend',
  '/short-video/data-analysis/local-funnel',
  '/short-video/data-analysis/local-rank',
  '/short-video/data-analysis/local-report',
  '/short-video/content/local-search',
  '/short-video/dashboard/static-stats',
].join('|')

function TrendTab({ days }: { days: string }) {
  const theme = useTheme()
  const { data: trendData = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['sv-data-trend', days],
    queryFn: () => shortvideoApi.dataTrend({ days: parseInt(days) }),
  })
  const rows = normalizeRows<SvDataTrendItem>(trendData)
  const dates = rows.map(d => String(d.date ?? ''))
  const plays = rows.map(d => Number(d.playCount ?? 0))
  const likes = rows.map(d => Number(d.likeCount ?? 0))
  const comments = rows.map(d => Number(d.commentCount ?? 0))
  const shares = rows.map(d => Number(d.shareCount ?? 0))

  const chartOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['播放量', '点赞数', '评论数', '分享数'] },
    xAxis: { type: 'category', data: dates },
    yAxis: [{ type: 'value', name: '播放量' }, { type: 'value', name: '互动' }],
    series: [
      { name: '播放量', type: 'line', smooth: true, data: plays, areaStyle: { opacity: 0.1 } },
      { name: '点赞数', type: 'line', smooth: true, data: likes, yAxisIndex: 1 },
      { name: '评论数', type: 'line', smooth: true, data: comments, yAxisIndex: 1 },
      { name: '分享数', type: 'line', smooth: true, data: shares, yAxisIndex: 1 },
    ],
  }

  const totalPlay = plays.reduce((a, b) => a + b, 0)
  const totalLike = likes.reduce((a, b) => a + b, 0)
  const avgEngagement = plays.length ? ((totalLike / (totalPlay || 1)) * 100).toFixed(2) : '0'
  const kpiColor = (tone: 'primary' | 'secondary' | 'warning' | 'success') => (
    theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  )

  return (
    <Box
      sx={{ mt: 2 }}
      data-testid="data-analysis-trend-tab"
      data-source-endpoint={DASHBOARD_TREND_ENDPOINT}
      data-no-local-trend-synthesis="true"
    >
      <Grid container spacing={2} mb={2}>
        {[
          { label: '总播放量', value: totalPlay.toLocaleString(), tone: 'primary' as const },
          { label: '总点赞数', value: totalLike.toLocaleString(), tone: 'secondary' as const },
          { label: '总评论数', value: rows.reduce((a, d) => a + Number(d.commentCount ?? 0), 0).toLocaleString(), tone: 'warning' as const },
          { label: '平均互动率', value: `${avgEngagement}%`, tone: 'success' as const },
        ].map((kpi, i) => (
          <Grid item xs={6} sm={3} key={i}>
            <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
              <Typography
                variant="h5"
                fontWeight={700}
                data-testid="shortvideo-data-kpi-value-surface"
                data-kpi-tone={kpi.tone}
                sx={{ color: kpiColor(kpi.tone) }}
              >
                {kpi.value}
              </Typography>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      {isLoading ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
        : isError ? (
          <Alert
            severity="error"
            data-testid="data-analysis-trend-error"
            data-no-local-trend-synthesis="true"
            action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          >
            趋势数据加载失败（POST {DASHBOARD_TREND_ENDPOINT}）：{getErrorMessage(error)}。趋势图不会用假播放量补齐。
          </Alert>
        ) : rows.length === 0 ? (
          <Alert severity="info" data-testid="data-analysis-trend-empty" data-no-mock-trend-fallback="true">
            暂无趋势数据。发布或同步短视频指标后，后端趋势接口才会返回时间序列。
          </Alert>
        ) : <Card data-testid="data-analysis-trend-chart" data-source-endpoint={DASHBOARD_TREND_ENDPOINT}><CardContent><ReactECharts option={chartOption} style={{ height: 360 }} /></CardContent></Card>}
    </Box>
  )
}
function FunnelTab({ days }: { days: string }) {
  const { data: calData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['sv-dashboard-stats', days],
    queryFn: () => shortvideoApi.dashboardStats(),
  })
  const stats = normalizeRecord(calData) as SvDashboardStats
  const funnelData = stats ? [
    { value: Number(stats.totalVideoCount ?? 0), name: '成片项目' },
    { value: Number(stats.totalPlayCount ?? 0), name: '总播放' },
    { value: Math.round(Number(stats.totalPlayCount ?? 0) * 0.05), name: '估算互动' },
    { value: Math.round(Number(stats.totalCost ?? 0) * 100), name: '成本×100' },
    { value: Number(stats.roi ?? 0) > 0 ? Math.round(Number(stats.roi) * 1000) : 0, name: 'ROI×1000' },
  ] : []
  const hasRealData = funnelData.some(item => item.value > 0)
  const chartOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'funnel',
      left: '10%', width: '80%',
      minSize: '0%', maxSize: '100%',
      sort: 'descending',
      gap: 4,
      label: { show: true, position: 'inside', formatter: '{b}\n{c}' },
      data: funnelData,
    }],
  }
  return (
    <Box
      sx={{ mt: 2 }}
      data-testid="data-analysis-funnel-tab"
      data-source-endpoint={DASHBOARD_STATS_ENDPOINT}
      data-no-local-funnel-synthesis="true"
    >
      {isLoading
        ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
        : isError
          ? (
            <Alert
              severity="error"
              data-testid="data-analysis-funnel-error"
              data-no-local-funnel-synthesis="true"
              action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
            >
              漏斗数据加载失败（POST {DASHBOARD_STATS_ENDPOINT}）：{getErrorMessage(error)}。转化漏斗不会用 1 或 100% 兜底。
            </Alert>
          )
        : hasRealData
          ? <Card data-testid="data-analysis-funnel-chart" data-source-endpoint={DASHBOARD_STATS_ENDPOINT}><CardContent><ReactECharts option={chartOption} style={{ height: 400 }} /></CardContent></Card>
          : <Alert severity="info" data-testid="data-analysis-funnel-empty" data-no-local-funnel-synthesis="true">
              暂无漏斗数据。Dashboard/stats 仍未形成有效成片、播放、互动或 ROI 记录，请先检查项目、发布和成本落库。
            </Alert>
      }
      {stats && (
        <Grid container spacing={2} mt={1}>
          {funnelData.map((item, i) => (
            <Grid item xs={6} sm={4} md={2} key={i}>
              <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                <Typography variant="h6" fontWeight={700}>{item.value.toLocaleString()}</Typography>
                <Typography variant="caption" color="text.secondary">{item.name}</Typography>
                {i > 0 && funnelData[i - 1].value > 0 && item.value > 0 && (
                  <Typography variant="caption" display="block" color="warning.main">
                    转化 {((item.value / funnelData[i - 1].value) * 100).toFixed(1)}%
                  </Typography>
                )}
              </Paper>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  )
}

function VideoRankTab() {
  const { data: videos = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['sv-video-rank'],
    queryFn: () => shortvideoApi.videoSearch({ rows: 20 }),
  })
  const rows = normalizeRows<Record<string, unknown>>(videos)
  const sorted = [...rows].sort((a, b) => Number(b.playCount ?? 0) - Number(a.playCount ?? 0))
  return (
    <Box
      sx={{ mt: 2 }}
      data-testid="data-analysis-rank-tab"
      data-source-endpoint={CONTENT_SEARCH_ENDPOINT}
      data-no-local-rank-fallback="true"
    >
      {isLoading
        ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
        : isError
          ? (
            <Alert
              severity="error"
              data-testid="data-analysis-rank-error"
              data-no-local-rank-fallback="true"
              action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
            >
              视频排行加载失败（POST {CONTENT_SEARCH_ENDPOINT}）：{getErrorMessage(error)}。页面不会展示本地排行。
            </Alert>
          )
        : sorted.length === 0
          ? <Alert severity="info" data-testid="data-analysis-rank-empty" data-no-local-rank-fallback="true">暂无可排行视频。请先完成项目成片、发布或同步内容数据。</Alert>
        : (
          <Card data-testid="data-analysis-rank-table" data-source-endpoint={CONTENT_SEARCH_ENDPOINT}>
            <CardContent sx={{ p: 0 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell width={40}>#</TableCell>
                    <TableCell>视频标题</TableCell>
                    <TableCell align="right">播放量</TableCell>
                    <TableCell align="right">点赞</TableCell>
                    <TableCell align="right">评论</TableCell>
                    <TableCell align="right">互动率</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sorted.map((v, i) => {
                    const plays = Number(v.playCount ?? 0)
                    const likes = Number(v.likeCount ?? 0)
                    const rate = plays ? ((likes / plays) * 100).toFixed(2) : '0'
                    return (
                      <TableRow key={i} hover>
                        <TableCell>
                          <Typography fontWeight={700} sx={{ color: i < 3 ? 'error.main' : 'text.secondary' }}>{i + 1}</Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" noWrap sx={{ maxWidth: 240 }}>{String(v.title ?? '--')}</Typography>
                        </TableCell>
                        <TableCell align="right">{plays.toLocaleString()}</TableCell>
                        <TableCell align="right">{likes.toLocaleString()}</TableCell>
                        <TableCell align="right">{Number(v.commentCount ?? 0).toLocaleString()}</TableCell>
                        <TableCell align="right">
                          <Chip label={`${rate}%`} size="small"
                            color={Number(rate) > 5 ? 'success' : Number(rate) > 2 ? 'warning' : 'default'}
                          />
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        )
      }
    </Box>
  )
}

function ReportTab({ days, setDays }: { days: string; setDays: (v: string) => void }) {
  const toast = useToast()
  const [reporting, setReporting] = useState(false)
  const [reportText, setReportText] = useState('')
  const [reportError, setReportError] = useState('')

  const handleReport = async () => {
    setReporting(true)
    setReportText('')
    setReportError('')
    try {
      const res = await shortvideoApi.dashboardStats()
      const stats = normalizeRecord(res) as SvDashboardStats
      const report = [
        `📊 短视频运营报表（近${days}天）`,
        `生成时间：${new Date().toLocaleString()}`,
        '',
        '── 核心指标（Dashboard）──',
        `总播放量：${Number(stats.totalPlayCount ?? 0).toLocaleString()}`,
        `成片项目数：${Number(stats.totalVideoCount ?? 0)}`,
        `总成本：${Number(stats.totalCost ?? 0)}`,
        `ROI：${Number(stats.roi ?? 0)}`,
        '',
        '── 运营建议（规则生成，非独立 AI 预测）──',
        '• 若播放量趋势为空，先检查内容发布、抖音同步和 dashboard/trend 数据落库。',
        '• 若 ROI 为 0，先补齐成本、发布结果和归因数据，避免用空数据做投放判断。',
        '• 对高播放低互动视频，优先复盘标题、前三秒 Hook 和评论区承接。',
      ].join('\n')
      setReportText(report)
    } catch (error) {
      const message = getErrorMessage(error)
      setReportError(`报表生成失败（POST ${DASHBOARD_STATS_ENDPOINT}）：${message}。已选择统计周期 ${days} 天会保留。`)
      toast(`生成报表失败：${message}`, 'error')
    } finally {
      setReporting(false)
    }
  }

  const handleExport = () => {
    const blob = new Blob([reportText], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `短视频运营报表-${days}天.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <Box
      sx={{ mt: 2 }}
      data-testid="data-analysis-report-tab"
      data-source-endpoint={DASHBOARD_STATS_ENDPOINT}
      data-no-local-report-fallback="true"
      data-rule-generated-report="true"
    >
      <Card>
        <CardContent>
          <Stack direction="row" spacing={2} mb={2} alignItems="center">
            <FormControl size="small" sx={{ width: 140 }}>
              <InputLabel>统计周期</InputLabel>
              <Select value={days} label="统计周期" onChange={e => setDays(e.target.value)}>
                <MenuItem value="7">近7天</MenuItem>
                <MenuItem value="14">近14天</MenuItem>
                <MenuItem value="30">近30天</MenuItem>
                <MenuItem value="90">近90天</MenuItem>
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={reporting ? <CircularProgress size={16} color="inherit" /> : <ReportIcon />}
              onClick={handleReport} disabled={reporting}
              data-testid="data-analysis-generate-report-button"
              data-source-endpoint={DASHBOARD_STATS_ENDPOINT}
            >
              {reporting ? '生成中...' : '生成报表'}
            </Button>
            {reportText && (
              <Button variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport} data-testid="data-analysis-export-report-button">导出</Button>
            )}
          </Stack>
          {reportText && (
            <Paper
              variant="outlined"
              data-testid="shortvideo-report-preview"
              data-source-endpoint={DASHBOARD_STATS_ENDPOINT}
              data-rule-generated-report="true"
              sx={(theme) => ({
                p: 2,
                bgcolor: theme.palette.mode === 'dark'
                  ? theme.palette.background.default
                  : alpha(theme.palette.common.black, 0.025),
              })}
            >
              <Typography component="pre" sx={{ m: 0, whiteSpace: 'pre-wrap', fontFamily: 'inherit', fontSize: 13 }}>
                {reportText}
              </Typography>
            </Paper>
          )}
          {reportError && (
            <Alert
              severity="error"
              data-testid="data-analysis-report-error"
              data-no-local-report-fallback="true"
              data-input-retained="true"
              action={<Button color="inherit" size="small" onClick={handleReport}>重试</Button>}
            >
              {reportError}
            </Alert>
          )}
          {!reportText && !reportError && (
            <Alert severity="info" data-testid="data-analysis-report-boundary" data-rule-generated-report="true">报表基于 POST {DASHBOARD_STATS_ENDPOINT} 生成，建议文本为规则诊断，不代表模型预测。</Alert>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}

const DATA_TABS = [
  { label: '趋势分析', icon: <TrendIcon fontSize="small" /> },
  { label: '转化漏斗', icon: <InsightsIcon fontSize="small" /> },
  { label: '视频排行', icon: <CompareIcon fontSize="small" /> },
  { label: '运营报表', icon: <ReportIcon fontSize="small" /> },
]

export { TrendTab, FunnelTab, VideoRankTab, ReportTab }

export default function DataAnalysisPage() {
  const [tab, setTab] = useState(0)
  const [days, setDays] = useState('7')
  return (
    <Box
      data-testid="data-analysis-page"
      data-ready-endpoints={DATA_ANALYSIS_READY_ENDPOINTS}
      data-ready-routes={DATA_ANALYSIS_READY_ROUTES}
      data-supported-actions={DATA_ANALYSIS_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={DATA_ANALYSIS_UNSUPPORTED_ENDPOINTS}
      data-no-local-trend-synthesis="true"
      data-no-local-funnel-synthesis="true"
      data-no-local-rank-fallback="true"
      data-no-local-report-fallback="true"
    >
      <PageHeader
        title="数据分析"
        breadcrumbs={[{ label: '短视频' }, { label: '数据分析' }]}
        subtitle={`基于 POST ${DASHBOARD_TREND_ENDPOINT}、${DASHBOARD_STATS_ENDPOINT} 与 ${CONTENT_SEARCH_ENDPOINT} 汇总趋势、漏斗、排行和运营报表。`}
        actions={
          <FormControl size="small" sx={{ width: 120 }}>
            <InputLabel>统计周期</InputLabel>
            <Select value={days} label="统计周期" onChange={e => setDays(e.target.value)} data-testid="data-analysis-days-select">
              <MenuItem value="7">近7天</MenuItem>
              <MenuItem value="14">近14天</MenuItem>
              <MenuItem value="30">近30天</MenuItem>
            </Select>
          </FormControl>
        }
      />
      <Alert severity="info" variant="outlined" data-testid="data-analysis-boundary-contract" data-supported-actions={DATA_ANALYSIS_SUPPORTED_ACTIONS} sx={{ mb: 2 }}>
        页面汇总 POST {DASHBOARD_TREND_ENDPOINT}、{DASHBOARD_STATS_ENDPOINT} 和 {CONTENT_SEARCH_ENDPOINT}，运营报表为规则诊断，不伪装独立模型预测。
      </Alert>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }} data-testid="data-analysis-tabs">
        {DATA_TABS.map((t, i) => <Tab key={i} label={t.label} iconPosition="start" icon={t.icon} />)}
      </Tabs>
      {tab === 0 && <TrendTab days={days} />}
      {tab === 1 && <FunnelTab days={days} />}
      {tab === 2 && <VideoRankTab />}
      {tab === 3 && <ReportTab days={days} setDays={setDays} />}
    </Box>
  )
}

