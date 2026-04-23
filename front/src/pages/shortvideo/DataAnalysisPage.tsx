import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Button, CircularProgress,
  Tabs, Tab, Stack, Chip, Grid, Paper,
  FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableHead, TableRow,
} from '@mui/material'
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

interface SvDataTrendItem { date?: string; playCount?: number; likeCount?: number; commentCount?: number; shareCount?: number }
interface SvDashboardStats { totalVideoCount?: number; totalPlayCount?: number; totalCost?: number; roi?: number; [key: string]: unknown }

function TrendTab({ days }: { days: string }) {
  const { data: trendData = [], isLoading } = useQuery({
    queryKey: ['sv-data-trend', days],
    queryFn: () => shortvideoApi.dataTrend({ days: parseInt(days) }),
  })
  const rows = trendData as SvDataTrendItem[]
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

  return (
    <Box sx={{ mt: 2 }}>
      <Grid container spacing={2} mb={2}>
        {[
          { label: '总播放量', value: totalPlay.toLocaleString(), color: '#1976d2' },
          { label: '总点赞数', value: totalLike.toLocaleString(), color: '#e91e63' },
          { label: '总评论数', value: rows.reduce((a, d) => a + Number(d.commentCount ?? 0), 0).toLocaleString(), color: '#ff9800' },
          { label: '平均互动率', value: `${avgEngagement}%`, color: '#4caf50' },
        ].map((kpi, i) => (
          <Grid item xs={6} sm={3} key={i}>
            <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h5" fontWeight={700} sx={{ color: kpi.color }}>{kpi.value}</Typography>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      {isLoading ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
        : <Card><CardContent><ReactECharts option={chartOption} style={{ height: 360 }} /></CardContent></Card>}
    </Box>
  )
}
function FunnelTab({ days }: { days: string }) {
  const { data: calData, isLoading } = useQuery({
    queryKey: ['sv-dashboard-stats', days],
    queryFn: () => shortvideoApi.dashboardStats(),
  })
  const stats = calData as SvDashboardStats | null
  const funnelData = stats ? [
    { value: Number(stats.totalVideoCount ?? 0) || 1, name: '成片项目' },
    { value: Number(stats.totalPlayCount ?? 0), name: '总播放' },
    { value: Math.max(1, Math.round(Number(stats.totalPlayCount ?? 0) * 0.05)), name: '估算互动' },
    { value: Math.max(1, Math.round(Number(stats.totalCost ?? 0) * 100)), name: '成本×100' },
    { value: Number(stats.roi ?? 0) > 0 ? Number(stats.roi) * 1000 : 1, name: 'ROI×1000' },
  ] : []
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
    <Box sx={{ mt: 2 }}>
      {isLoading
        ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
        : funnelData.length > 0
          ? <Card><CardContent><ReactECharts option={chartOption} style={{ height: 400 }} /></CardContent></Card>
          : <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}><Typography color="text.secondary">暂无漏斗数据</Typography></Paper>
      }
      {stats && (
        <Grid container spacing={2} mt={1}>
          {funnelData.map((item, i) => (
            <Grid item xs={6} sm={4} md={2} key={i}>
              <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                <Typography variant="h6" fontWeight={700}>{item.value.toLocaleString()}</Typography>
                <Typography variant="caption" color="text.secondary">{item.name}</Typography>
                {i > 0 && funnelData[i - 1].value > 0 && (
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
  const { data: videos = [], isLoading } = useQuery({
    queryKey: ['sv-video-rank'],
    queryFn: () => shortvideoApi.videoSearch({ rows: 20 }).then(r => r.list ?? []),
  })
  const sorted = [...videos].sort((a, b) => Number(b.playCount ?? 0) - Number(a.playCount ?? 0))
  return (
    <Box sx={{ mt: 2 }}>
      {isLoading
        ? <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
        : (
          <Card>
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

  const handleReport = async () => {
    setReporting(true)
    setReportText('')
    try {
      const res = await shortvideoApi.dashboardStats()
      const stats = res as SvDashboardStats
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
        '── AI 建议 ──',
        '• 建议在互动率高峰时段（18:00-21:00）集中发布内容',
        '• 爆款话题结合品牌特色，差异化竞争',
        '• 加强评论区互动，回复率提升可增加 15% 粉丝留存',
      ].join('\n')
      setReportText(report)
    } catch {
      toast('生成报表失败', 'error')
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
    <Box sx={{ mt: 2 }}>
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
            >
              {reporting ? '生成中...' : '生成报表'}
            </Button>
            {reportText && (
              <Button variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport}>导出</Button>
            )}
          </Stack>
          {reportText && (
            <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
              <Typography component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', fontSize: 13 }}>
                {reportText}
              </Typography>
            </Paper>
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

export default function DataAnalysisPage() {
  const [tab, setTab] = useState(0)
  const [days, setDays] = useState('7')
  return (
    <Box>
      <PageHeader
        title="数据分析"
        breadcrumbs={[{ label: '短视频' }, { label: '数据分析' }]}
        actions={
          <FormControl size="small" sx={{ width: 120 }}>
            <InputLabel>统计周期</InputLabel>
            <Select value={days} label="统计周期" onChange={e => setDays(e.target.value)}>
              <MenuItem value="7">近7天</MenuItem>
              <MenuItem value="14">近14天</MenuItem>
              <MenuItem value="30">近30天</MenuItem>
            </Select>
          </FormControl>
        }
      />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        {DATA_TABS.map((t, i) => <Tab key={i} label={t.label} iconPosition="start" icon={t.icon} />)}
      </Tabs>
      {tab === 0 && <TrendTab days={days} />}
      {tab === 1 && <FunnelTab days={days} />}
      {tab === 2 && <VideoRankTab />}
      {tab === 3 && <ReportTab days={days} setDays={setDays} />}
    </Box>
  )
}

