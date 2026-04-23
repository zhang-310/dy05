import {
  Box, Typography, Stack, Card, CardContent, Grid, Button, CircularProgress,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery } from '@tanstack/react-query'
import { systemApi } from '@/api/system'
import ReactECharts from 'echarts-for-react'

interface SystemHealthData {
  cpuUsage?: number
  memUsage?: number
  qps?: number
  p99LatencyMs?: number
  timestamps?: string[]
  cpuHistory?: number[]
  memHistory?: number[]
  heapHistory?: number[]
  jvm?: {
    heapUsageRate?: number
    heapUsed?: string
    heapMax?: string
    gcCount?: number
    gcTimeMs?: number
  }
  db?: {
    poolUsageRate?: number
    activeConnections?: number
    pendingConnections?: number
  }
  cache?: {
    hitRate?: number
  }
}

function GaugeCard({ title, value, unit, warn, danger }: {
  title: string; value: number; unit: string; warn: number; danger: number
}) {
  const color = value >= danger ? '#f44336' : value >= warn ? '#ff9800' : '#4caf50'
  const option = {
    series: [{
      type: 'gauge', min: 0, max: 100,
      axisLine: { lineStyle: { width: 12, color: [[warn / 100, '#4caf50'], [danger / 100, '#ff9800'], [1, '#f44336']] } },
      pointer: { itemStyle: { color: 'auto' } },
      detail: { formatter: `{value}${unit}`, fontSize: 14, fontWeight: 700, color },
      data: [{ value: Math.round(value), name: title }],
      title: { fontSize: 12 },
    }],
  }
  return (
    <Card variant="outlined">
      <CardContent>
        <ReactECharts option={option} style={{ height: 160 }} />
        <Typography variant="caption" align="center" display="block" color="text.secondary">{title}</Typography>
      </CardContent>
    </Card>
  )
}

export default function PerformanceMonitoringPage() {
  const { data: rawData, isLoading, refetch } = useQuery({
    queryKey: ['system-performance'],
    queryFn: () => systemApi.health(),
    refetchInterval: 15000,
  })

  const d: SystemHealthData = (rawData as SystemHealthData | undefined) ?? {}
  const jvm = d.jvm ?? {}
  const db = d.db ?? {}
  const cache = d.cache ?? {}

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['CPU', '内存', '堆内存'], bottom: 0 },
    xAxis: { type: 'category', data: d.timestamps ?? [] },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: 'CPU', type: 'line', smooth: true, data: d.cpuHistory ?? [], areaStyle: { opacity: 0.1 } },
      { name: '内存', type: 'line', smooth: true, data: d.memHistory ?? [], areaStyle: { opacity: 0.1 } },
      { name: '堆内存', type: 'line', smooth: true, data: d.heapHistory ?? [], areaStyle: { opacity: 0.1 } },
    ],
  }

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h6" fontWeight={600}>系统性能监控</Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          {isLoading && <CircularProgress size={16} />}
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>
        </Stack>
      </Stack>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <GaugeCard title="CPU 使用率" value={Number(d.cpuUsage ?? 0)} unit="%" warn={70} danger={90} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <GaugeCard title="内存使用率" value={Number(d.memUsage ?? 0)} unit="%" warn={75} danger={90} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <GaugeCard title="JVM 堆内存" value={Number(jvm.heapUsageRate ?? 0)} unit="%" warn={70} danger={85} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <GaugeCard title="数据库连接池" value={Number(db.poolUsageRate ?? 0)} unit="%" warn={60} danger={85} />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        {[
          { label: 'JVM 堆大小', value: `${jvm.heapUsed ?? '--'} / ${jvm.heapMax ?? '--'} MB` },
          { label: 'GC 次数', value: String(jvm.gcCount ?? '--') },
          { label: 'GC 耗时', value: `${jvm.gcTimeMs ?? '--'} ms` },
          { label: 'DB 活跃连接', value: String(db.activeConnections ?? '--') },
          { label: 'DB 等待连接', value: String(db.pendingConnections ?? '--') },
          { label: 'Redis 命中率', value: `${cache.hitRate != null ? (Number(cache.hitRate) * 100).toFixed(1) : '--'}%` },
          { label: '请求 QPS', value: String(d.qps ?? '--') },
          { label: 'P99 延迟', value: `${d.p99LatencyMs ?? '--'} ms` },
        ].map(m => (
          <Grid item xs={6} sm={3} key={m.label}>
            <Card variant="outlined"><CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{m.label}</Typography>
              <Typography variant="body1" fontWeight={600}>{m.value}</Typography>
            </CardContent></Card>
          </Grid>
        ))}
      </Grid>

      {(d.cpuHistory?.length ?? 0) > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" fontWeight={600} gutterBottom>近期趋势</Typography>
            <ReactECharts option={trendOption} style={{ height: 260 }} />
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
