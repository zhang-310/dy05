import { useState } from 'react'
import { Box, Card, CardContent, Typography, Stack, TextField, MenuItem, Grid } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { shortvideoApi } from '@/api/shortvideo'

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

const METRICS = [
  { key: 'playCount', label: '播放量', color: '#1976d2' },
  { key: 'likeCount', label: '点赞量', color: '#e91e63' },
  { key: 'commentCount', label: '评论量', color: '#ff9800' },
  { key: 'shareCount', label: '分享量', color: '#4caf50' },
]

export default function QualityPage() {
  const [days, setDays] = useState(7)

  const { data: trendData } = useQuery({
    queryKey: ['sv-data-trend', days],
    queryFn: () => shortvideoApi.dataTrend({ days }),
  })
  const trend = Array.isArray(trendData) ? trendData as TrendRow[] : [] as TrendRow[]

  const { data: statsData } = useQuery({
    queryKey: ['sv-dashboard-stats'],
    queryFn: () => shortvideoApi.dashboardStats(),
  })
  const stats = statsData ?? {}

  const { data: qualityOverview } = useQuery({
    queryKey: ['sv-quality-overview'],
    queryFn: () => shortvideoApi.qualityOverview(),
  })
  const quality = qualityOverview ?? {}

  const { data: qualityTrendData } = useQuery({
    queryKey: ['sv-quality-trend', days],
    queryFn: () => shortvideoApi.qualityTrend({ days }),
  })
  const qualityTrend = (Array.isArray(qualityTrendData) ? qualityTrendData : []) as QualityTrendRow[]

  const { data: feedbackReport } = useQuery({
    queryKey: ['sv-feedback-weekly'],
    queryFn: () => shortvideoApi.feedbackWeeklyReport(),
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
      itemStyle: { color: m.color },
    })),
    grid: { left: 60, right: 20, bottom: 60, top: 40 },
  }

  const kpiEntries = [
    ...(stats ? Object.entries(stats).filter(([, v]) => typeof v === 'number' || typeof v === 'string') : []),
    ...(quality ? Object.entries(quality).filter(([, v]) => typeof v === 'number' || typeof v === 'string') : []),
  ]

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField select size="small" label="时间范围" value={days}
          onChange={e => setDays(Number(e.target.value))} sx={{ width: 120 }}>
          {[7, 14, 30].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
        </TextField>
      </Stack>

      {kpiEntries.length > 0 && (
        <Grid container spacing={2}>
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

      {feedbackReport && Object.keys(feedbackReport as object).length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>反馈周报（摘要）</Typography>
            <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', m: 0 }}>
              {JSON.stringify(feedbackReport, null, 2)}
            </Typography>
          </CardContent>
        </Card>
      )}

      {qualityTrend.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>质量趋势（近 {days} 天）</Typography>
            <ReactECharts
              option={{
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: qualityTrend.map(r => String(r.date ?? r.day ?? '')) },
                yAxis: { type: 'value' },
                series: [{ type: 'line', smooth: true, data: qualityTrend.map(r => Number(r.score ?? r.avgScore ?? 0)) }],
                grid: { left: 48, right: 16, bottom: 40, top: 24 },
              }}
              style={{ height: 260 }}
            />
          </CardContent>
        </Card>
      )}

      {trend.length > 0 ? (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>数据趋势</Typography>
            <ReactECharts option={trendOption} style={{ height: 340 }} />
          </CardContent>
        </Card>
      ) : (
        <Card variant="outlined">
          <CardContent>
            <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary">暂无趋势数据</Typography>
            </Box>
          </CardContent>
        </Card>
      )}

      {trend.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>完播率趋势</Typography>
            <ReactECharts
              option={{
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30, fontSize: 11 } },
                yAxis: { type: 'value', name: '完播率', axisLabel: { formatter: (v: number) => `${(v * 100).toFixed(0)}%` } },
                series: [{
                  type: 'bar',
                  data: trend.map(r => r.completionRate ?? r.finishRate ?? 0),
                  itemStyle: { color: '#7b1fa2' },
                }],
                grid: { left: 60, right: 20, bottom: 60, top: 20 },
              }}
              style={{ height: 220 }}
            />
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
