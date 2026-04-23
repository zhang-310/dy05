import { Card, CardContent, Typography, Box } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import type { EChartsOption } from 'echarts'
import { echarts } from '@/utils/echarts-registry'

interface ChartCardProps {
  title: string
  option: EChartsOption
  height?: number
  loading?: boolean
}

export function ChartCard({ title, option, height = 300, loading = false }: ChartCardProps) {
  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
          {title}
        </Typography>
        <Box sx={{ height: typeof height === 'number' ? `${height}px` : height }}>
          <ReactECharts echarts={echarts} option={option} style={{ height: '100%', width: '100%' }} showLoading={loading} />
        </Box>
      </CardContent>
    </Card>
  )
}
