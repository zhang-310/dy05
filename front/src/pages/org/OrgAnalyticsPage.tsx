import { useState } from 'react'
import { Box, Card, CardContent, Typography, Stack, TextField, MenuItem, Grid } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import request from '@/utils/request'

interface TrendRow {
  date?: string; time?: string
  gmv?: number; totalGmv?: number
  sessionCount?: number; sessions?: number
  efficiency?: number; gmvPerPerson?: number
  [key: string]: unknown
}

interface SummaryRow {
  [key: string]: unknown
}

const orgApi = {
  analytics: (params: Record<string, unknown>) => request.post<Record<string, unknown>>('/org/analytics/summary', params),
  trend: (params: Record<string, unknown>) => request.post<Record<string, unknown>[]>('/org/analytics/trend', params),
}

export default function OrgAnalyticsPage() {
  const [days, setDays] = useState(30)

  const { data: summaryData } = useQuery({
    queryKey: ['org-analytics-summary', days],
    queryFn: () => orgApi.analytics({ days }),
  })
  const { data: trendData } = useQuery({
    queryKey: ['org-analytics-trend', days],
    queryFn: () => orgApi.trend({ days }),
  })

  const summary = (summaryData ?? {}) as SummaryRow
  const trend = Array.isArray(trendData) ? trendData as TrendRow[] : ([] as TrendRow[])
  const dates = trend.map(r => String(r.date ?? r.time ?? ''))

  const gmvOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['GMV', '场次数', '人效'], top: 0 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: [{ type: 'value', name: 'GMV(元)' }, { type: 'value', name: '场次/人效', splitLine: { show: false } }],
    series: [
      { name: 'GMV', type: 'line', smooth: true, yAxisIndex: 0,
        data: trend.map(r => r.gmv ?? r.totalGmv ?? 0), itemStyle: { color: '#1976d2' } },
      { name: '场次数', type: 'bar', yAxisIndex: 1,
        data: trend.map(r => r.sessionCount ?? r.sessions ?? 0), itemStyle: { color: '#4caf50' } },
      { name: '人效', type: 'line', smooth: true, yAxisIndex: 1,
        data: trend.map(r => r.efficiency ?? r.gmvPerPerson ?? 0), itemStyle: { color: '#ff9800' } },
    ],
    grid: { left: 60, right: 60, bottom: 60, top: 40 },
  }

  const kpis = Object.entries(summary).filter(([, v]) => typeof v === 'number' || typeof v === 'string').slice(0, 8)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField select size="small" label="时间范围" value={days}
          onChange={e => setDays(Number(e.target.value))} sx={{ width: 120 }}>
          {[7, 14, 30, 90].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
        </TextField>
      </Stack>

      {kpis.length > 0 && (
        <Grid container spacing={2}>
          {kpis.map(([k, v]) => (
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

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" mb={1}>机构数据趋势</Typography>
          {trend.length > 0 ? (
            <ReactECharts option={gmvOption} style={{ height: 360 }} />
          ) : (
            <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary">暂无数据</Typography>
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}
