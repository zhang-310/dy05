import { Box, Card, CardContent, Typography, Stack } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type { AiCallVolumeTrendItem, AiCallTypeDistributionItem } from '@/types/ai'

export default function AiCallLogPage() {
  const { data: trendData } = useQuery({
    queryKey: ['ai-call-trend'],
    queryFn: () => aiApi.callVolumeTrend(),
    refetchInterval: 60000,
  })
  const { data: distData } = useQuery({
    queryKey: ['ai-call-distribution'],
    queryFn: () => aiApi.callTypeDistribution(),
    refetchInterval: 60000,
  })

  const trend: AiCallVolumeTrendItem[] = Array.isArray(trendData) ? trendData : []
  const dist: AiCallTypeDistributionItem[] = Array.isArray(distData) ? distData : []

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['调用量', '成功量', '失败量'], top: 0 },
    xAxis: {
      type: 'category',
      data: trend.map(r => String(r.date ?? r.time ?? r.hour ?? '')),
      axisLabel: { rotate: 30, fontSize: 11 },
    },
    yAxis: { type: 'value', name: '调用次数' },
    series: [
      { name: '调用量', type: 'line', smooth: true,
        data: trend.map(r => r.total ?? r.callCount ?? r.count ?? 0),
        itemStyle: { color: '#1976d2' } },
      { name: '成功量', type: 'line', smooth: true,
        data: trend.map(r => r.success ?? r.successCount ?? 0),
        itemStyle: { color: '#2e7d32' } },
      { name: '失败量', type: 'line', smooth: true,
        data: trend.map(r => r.failed ?? r.failedCount ?? r.error ?? 0),
        itemStyle: { color: '#d32f2f' } },
    ],
    grid: { left: 50, right: 20, bottom: 60, top: 40 },
  }

  const distOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '调用类型',
      type: 'pie',
      radius: ['40%', '70%'],
      data: dist.map(r => ({ name: String(r.type ?? r.taskType ?? r.callType ?? ''), value: r.count ?? r.total ?? 0 })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
    }],
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={2}>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="subtitle2" mb={1}>调用量趋势</Typography>
            {trend.length > 0 ? (
              <ReactECharts option={trendOption} style={{ height: 320 }} />
            ) : (
              <Box sx={{ height: 320, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Typography color="text.secondary">暂无数据</Typography>
              </Box>
            )}
          </CardContent>
        </Card>

        <Card variant="outlined" sx={{ width: 380 }}>
          <CardContent>
            <Typography variant="subtitle2" mb={1}>调用类型分布</Typography>
            {dist.length > 0 ? (
              <ReactECharts option={distOption} style={{ height: 320 }} />
            ) : (
              <Box sx={{ height: 320, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Typography color="text.secondary">暂无数据</Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </Stack>

      {dist.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>类型明细</Typography>
            <Stack direction="row" spacing={2} flexWrap="wrap">
              {dist.map((r, i) => (
                <Box key={i} sx={{ minWidth: 140, p: 1.5, bgcolor: 'grey.50', borderRadius: 1, textAlign: 'center' }}>
                  <Typography variant="caption" color="text.secondary">{String(r.type ?? r.callType ?? '—')}</Typography>
                  <Typography variant="h6" fontWeight={700}>{String(r.count ?? r.total ?? 0)}</Typography>
                </Box>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
