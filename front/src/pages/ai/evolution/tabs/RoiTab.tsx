import { Box, Card, CardContent, Grid, Typography } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type { EvolveRoiPayload } from '@/types/evolutionEngine'

export interface RoiTabProps {
  scopeKbId: string
}

export function RoiTab({ scopeKbId }: RoiTabProps) {
  const { data: roiData } = useQuery({
    queryKey: ['evolve-roi', scopeKbId || 'all'],
    queryFn: () => aiApi.evolveRoi({ kbId: scopeKbId ? Number(scopeKbId) : undefined }),
  })
  const { data: trendData } = useQuery({
    queryKey: ['evolve-score-trend', scopeKbId || 'all'],
    queryFn: () => aiApi.scoreTrend({
      days: 90,
      kbId: scopeKbId ? Number(scopeKbId) : undefined,
    }),
  })

  const roi = roiData as EvolveRoiPayload | undefined
  const trend = trendData ?? []

  const kpiCards = [
    { label: '高质完成任务数', value: `${roi?.newKnowledge ?? 0} 次`, color: '#1976d2' },
    { label: '平均质量分', value: roi?.avgScore != null ? String(roi.avgScore) : '—', color: '#388e3c' },
    { label: '成功率', value: roi?.successRate != null ? `${roi.successRate}%` : '—', color: '#f57c00' },
    { label: '覆盖知识库数', value: String(roi?.coveredDocs ?? '—'), color: '#7b1fa2' },
  ]

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['质量分'] },
    xAxis: { type: 'category', data: trend.map(p => String(p.date ?? '')) },
    yAxis: { type: 'value', min: 0, max: 10 },
    series: [{ name: '质量分', type: 'line', smooth: true, data: trend.map(p => Number(p.score ?? 0)),
      areaStyle: { opacity: 0.1 }, markPoint: { data: [{ type: 'max', name: '最高分' }] } }],
  }

  return (
    <Box>
      <Grid container spacing={2} mb={3}>
        {kpiCards.map(c => (
          <Grid key={c.label} item xs={12} sm={6} md={3}>
            <Card variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Typography variant="h5" fontWeight={700} sx={{ color: c.color }}>{c.value}</Typography>
                <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>{c.label}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      <Card variant="outlined" sx={{ bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom sx={{ color: 'var(--color-text-primary)' }}>质量分 3 个月趋势（可按顶部 KB 筛选）</Typography>
          <ReactECharts option={trendOption} style={{ height: 280 }} />
        </CardContent>
      </Card>

      {roi && (
        <Card variant="outlined" sx={{ mt: 2, bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
          <CardContent>
            <Typography variant="subtitle2" fontWeight={700} gutterBottom sx={{ color: 'var(--color-text-primary)' }}>任务汇总</Typography>
            <Typography variant="body2" sx={{ color: 'var(--color-text-secondary)' }}>
              总任务 {String(roi.totalTasks ?? '—')}，已完成 {String(roi.totalRuns ?? '—')}；指标来自近期进化任务聚合，非直播 GMV A/B。
            </Typography>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
