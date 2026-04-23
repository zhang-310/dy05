import { useMemo } from 'react'
import { Box, Typography, Grid, Card, CardContent, Stack, Chip, Button, CircularProgress, Alert } from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import ReactECharts from 'echarts-for-react'

interface InfraHealthItem { component: string; ok: boolean; message?: string | null }

function StatusChip({ ok, message }: { ok: boolean; message?: string | null }) {
  return <Chip label={ok ? '正常' : (message || '异常')} color={ok ? 'success' : 'error'} size="small"
    icon={ok ? <CheckCircleIcon /> : <ErrorIcon />} />
}

function MetricCard({ title, value, unit, ok }: { title: string; value: string | number; unit?: string; ok?: boolean }) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="caption" color="text.secondary">{title}</Typography>
          {ok !== undefined && <StatusChip ok={ok} />}
        </Stack>
        <Typography variant="h5" fontWeight={700} mt={0.5}>
          {value}{unit && <Typography component="span" variant="body2" color="text.secondary" ml={0.5}>{unit}</Typography>}
        </Typography>
      </CardContent>
    </Card>
  )
}

// 从 List<InfraHealthItem> 中解析出各组件状态
function parseInfraHealth(data: unknown): InfraHealthItem[] {
  if (Array.isArray(data)) return data as InfraHealthItem[]
  return []
}

function getStatus(items: InfraHealthItem[], component: string): { ok: boolean; message?: string | null } {
  const item = items.find(i => i.component?.toLowerCase() === component.toLowerCase())
  if (!item) return { ok: false, message: '未接入' }
  return { ok: item.ok, message: item.message }
}

export default function AdminInfraPage() {
  const { data: health, isLoading: healthLoading, refetch: refetchHealth } = useQuery({
    queryKey: ['ai-infra-health'],
    queryFn: () => aiApi.infraHealth(),
    refetchInterval: 30000,
  })

  const { data: cache, isLoading: cacheLoading } = useQuery({
    queryKey: ['ai-cache-stats'],
    queryFn: () => aiApi.cacheStats(),
    refetchInterval: 30000,
  })

  const { data: search, isLoading: searchLoading } = useQuery({
    queryKey: ['ai-search-stats'],
    queryFn: () => aiApi.searchStats(),
    refetchInterval: 30000,
  })

  // 正确解析 List<InfraHealthItem>
  const infraItems = useMemo(() => parseInfraHealth(health), [health])
  const milvus = useMemo(() => getStatus(infraItems, 'milvus'), [infraItems])
  const es = useMemo(() => getStatus(infraItems, 'elasticsearch'), [infraItems])
  const redis = useMemo(() => getStatus(infraItems, 'redis'), [infraItems])
  const llm = useMemo(() => getStatus(infraItems, 'llm'), [infraItems])

  const c = cache ?? {}
  const s = search ?? {}

  const cacheChartOption = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['50%', '70%'],
      data: [
        { name: '命中', value: Number(c.hit ?? c.hitCount ?? 0) },
        { name: '未命中', value: Number(c.miss ?? c.missCount ?? 0) },
      ],
      label: { show: true },
    }],
  }

  const hasAnyDown = infraItems.some(i => !i.ok)
  const isLoading = healthLoading || cacheLoading || searchLoading

  // 除已知组件外的其他组件
  const knownComponents = new Set(['milvus', 'elasticsearch', 'redis', 'llm'])
  const otherItems = infraItems.filter(i => !knownComponents.has(i.component?.toLowerCase()))

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h6" fontWeight={600}>AI 基础设施管理</Typography>
        <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetchHealth()}>刷新</Button>
      </Stack>

      {isLoading && <CircularProgress sx={{ mx: 'auto' }} />}

      <Typography variant="subtitle2" color="text.secondary" fontWeight={600}>服务健康状态</Typography>
      <Grid container spacing={2}>
        {[
          { title: 'Milvus 向量库', ...milvus },
          { title: 'Elasticsearch', ...es },
          { title: 'Redis 缓存', ...redis },
          { title: 'LLM 服务', ...llm },
        ].map(item => (
          <Grid item xs={12} sm={6} md={3} key={item.title}>
            <MetricCard title={item.title} value={item.ok ? '在线' : (item.message || '离线')} ok={item.ok} />
          </Grid>
        ))}
      </Grid>

      {/* 其他组件（动态渲染后端返回的额外组件） */}
      {otherItems.length > 0 && (
        <Grid container spacing={2}>
          {otherItems.map(item => (
            <Grid item xs={12} sm={6} md={3} key={item.component}>
              <MetricCard title={item.component} value={item.ok ? '在线' : (item.message || '离线')} ok={item.ok} />
            </Grid>
          ))}
        </Grid>
      )}

      {!healthLoading && hasAnyDown && (
        <Alert severity="warning">部分 AI 基础设施服务异常，请检查服务状态</Alert>
      )}

      {/* 全部组件健康 Chip 列表 */}
      {infraItems.length > 0 && (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {infraItems.map(item => (
            <Chip key={item.component}
              label={`${item.component}${item.ok ? '' : ` — ${item.message || '异常'}`}`}
              size="small" color={item.ok ? 'success' : 'error'}
              icon={item.ok ? <CheckCircleIcon /> : <ErrorIcon />} />
          ))}
        </Stack>
      )}

      <Typography variant="subtitle2" color="text.secondary" fontWeight={600}>缓存统计</Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="缓存命中率" value={c.hitRate != null ? `${Number(c.hitRate).toFixed(1)}` : '--'} unit="%" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="缓存键总数" value={String(c.keyCount ?? '--')} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="内存使用" value={String(c.memUsed ?? '--')} unit="MB" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="过期键" value={String(c.expiredKeys ?? '--')} />
        </Grid>
      </Grid>

      {(Number(c.hit ?? c.hitCount ?? 0) + Number(c.miss ?? c.missCount ?? 0)) > 0 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Card variant="outlined"><CardContent>
              <Typography variant="subtitle2" gutterBottom>缓存命中分布</Typography>
              <ReactECharts option={cacheChartOption} style={{ height: 220 }} />
            </CardContent></Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card variant="outlined"><CardContent>
              <Typography variant="subtitle2" gutterBottom>检索统计</Typography>
              <Stack spacing={1} mt={1}>
                {([['ES 查询数', s.esQueryCount, ''], ['Milvus 查询数', s.milvusQueryCount, ''], ['平均耗时', s.avgLatencyMs, 'ms'], ['P99 耗时', s.p99LatencyMs, 'ms']] as [string, unknown, string][]).map(([label, val, unit]) => (
                  <Stack key={label} direction="row" justifyContent="space-between">
                    <Typography variant="body2" color="text.secondary">{label}</Typography>
                    <Typography variant="body2" fontWeight={600}>{val != null ? `${String(val)}${unit}` : '--'}</Typography>
                  </Stack>
                ))}
              </Stack>
            </CardContent></Card>
          </Grid>
        </Grid>
      )}
    </Box>
  )
}
