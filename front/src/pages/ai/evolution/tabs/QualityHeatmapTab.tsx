import { Box, Card, CardContent, Grid, LinearProgress, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { aiApi } from '@/api/ai'
import type { QualityScoreTrendPoint } from '@/types/evolutionEngine'

export interface QualityHeatmapTabProps {
  scopeKbId: string
}

function parseLocalDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function buildHeatmapData(points: QualityScoreTrendPoint[]) {
  if (points.length === 0) {
    return { seriesData: [] as { value: [number, number, number]; name: string }[], weekRows: 1 }
  }
  const first = parseLocalDate(points[0].date)
  const startSunday = new Date(first)
  startSunday.setDate(startSunday.getDate() - startSunday.getDay())
  let maxWeek = 0
  const seriesData: { value: [number, number, number]; name: string }[] = []
  for (const p of points) {
    const d = parseLocalDate(p.date)
    const col = d.getDay()
    const row = Math.floor((d.getTime() - startSunday.getTime()) / (86400000 * 7))
    maxWeek = Math.max(maxWeek, row)
    const score = Number(p.score ?? 0)
    const cnt = p.count ?? 0
    seriesData.push({
      value: [col, row, score],
      name: `${p.date} 均分 ${score.toFixed(1)}（${cnt} 条任务）`,
    })
  }
  return { seriesData, weekRows: Math.max(1, maxWeek + 1) }
}

export function QualityHeatmapTab({ scopeKbId }: QualityHeatmapTabProps) {
  const { data: historyData, isFetching } = useQuery({
    queryKey: ['quality-score-history', scopeKbId || 'all'],
    queryFn: () => aiApi.qualityScoreHistory({
      kbId: scopeKbId ? Number(scopeKbId) : undefined,
      days: 90,
    }),
  })

  const points = historyData ?? []
  const { seriesData, weekRows } = buildHeatmapData(points)
  const weekLabels = Array.from({ length: weekRows }, (_, i) => `第${i + 1}周`)

  const heatmapOption = {
    tooltip: {
      formatter: (p: { name?: string; value?: [number, number, number] }) =>
        p.name ?? `质量分: ${p.value?.[2]?.toFixed?.(2) ?? ''}`,
    },
    visualMap: {
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      pieces: [
        { min: 9, label: '≥9 优秀', color: '#4caf50' },
        { min: 8, max: 9, label: '8-9 良好', color: '#2196f3' },
        { min: 7, max: 8, label: '7-8 一般', color: '#ff9800' },
        { max: 7, label: '<7 需优化', color: '#f44336' },
      ],
    },
    xAxis: { type: 'category', data: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'] },
    yAxis: { type: 'category', data: weekLabels },
    series: [{
      type: 'heatmap',
      data: seriesData,
      label: { show: true, formatter: (p: { value?: [number, number, number] }) => (p.value?.[2] ?? 0).toFixed(1) },
    }],
  }

  const trendOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: points.map(p => p.date) },
    yAxis: { type: 'value', min: 0, max: 10 },
    series: [{
      name: '质量分',
      type: 'line',
      smooth: true,
      data: points.map(p => Number(p.score ?? 0)),
      markLine: { data: [{ yAxis: 8, lineStyle: { color: '#ff9800' } }] },
    }],
  }

  return (
    <Box>
      <Stack direction="row" spacing={2} mb={2} alignItems="center" flexWrap="wrap">
        <Typography variant="body2" sx={{ color: 'var(--color-text-secondary)' }}>
          使用顶部「知识库范围」筛选；格子按真实日期落在对应周与星期。颜色：红低 / 绿高。
        </Typography>
      </Stack>
      {isFetching ? <LinearProgress /> : (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Card variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              <CardContent>
                <Typography variant="subtitle2" gutterBottom sx={{ color: 'var(--color-text-primary)' }}>质量分热力图（90 天）</Typography>
                <ReactECharts option={heatmapOption} style={{ height: 300 }} />
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              <CardContent>
                <Typography variant="subtitle2" gutterBottom sx={{ color: 'var(--color-text-primary)' }}>质量分趋势（按日）</Typography>
                <ReactECharts option={trendOption} style={{ height: 300 }} />
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </Box>
  )
}
