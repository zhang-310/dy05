import { useMemo } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import RefreshIcon from '@mui/icons-material/Refresh'
import StorageIcon from '@mui/icons-material/Storage'
import TravelExploreIcon from '@mui/icons-material/TravelExplore'
import { Link as RouterLink } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { PageHeader } from '@/components/base'
import { aiApi } from '@/api/ai'
import type { AiCacheDiagnosticsVO, AiCacheStatsVO, AiInfraHealthItem, AiSearchStatsVO } from '@/types/ai'

const ADMIN_INFRA_READY_ENDPOINTS = [
  '/ai/admin/infra/health',
  '/ai/admin/infra/cache/stats',
  '/ai/admin/infra/cache/diagnostics',
  '/ai/admin/infra/search/stats',
].join('|')

const ADMIN_INFRA_UNSUPPORTED_ENDPOINTS = [
  '/ai/admin/infra/cache/ttl/update',
  '/ai/admin/infra/cache/flush',
  '/ai/admin/infra/cache/prewarm',
  '/ai/admin/infra/cache/hot-key/mock',
  '/ai/admin/infra/search/mock',
  '/ai/admin/infra/health/local-probe',
].join('|')

function parseInfraHealth(data: unknown): AiInfraHealthItem[] {
  return Array.isArray(data) ? data : []
}

function getStatus(items: AiInfraHealthItem[], component: string): { ok: boolean; message?: string | null } {
  const item = items.find((i) => i.component?.toLowerCase() === component.toLowerCase())
  if (!item) return { ok: false, message: '未上报' }
  return { ok: item.ok, message: item.message }
}

function formatPercent(value: unknown): string {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? `${n.toFixed(1)}%` : '--'
}

function formatNumber(value: unknown): string {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? n.toLocaleString() : '--'
}

function formatSeconds(value: unknown): string {
  const seconds = Number(value ?? 0)
  if (!Number.isFinite(seconds) || seconds <= 0) return '--'
  if (seconds >= 86400) return `${(seconds / 86400).toFixed(1)} 天`
  if (seconds >= 3600) return `${(seconds / 3600).toFixed(1)} 小时`
  if (seconds >= 60) return `${Math.round(seconds / 60)} 分钟`
  return `${Math.round(seconds)} 秒`
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error || '加载失败')
}

function StatusChip({ ok, message }: { ok: boolean; message?: string | null }) {
  return (
    <Chip
      label={ok ? '正常' : (message || '异常')}
      color={ok ? 'success' : 'error'}
      size="small"
      icon={ok ? <CheckCircleIcon /> : <ErrorIcon />}
    />
  )
}

function MetricCard(props: {
  title: string
  value: string | number
  helper?: string
  unit?: string
  ok?: boolean
  icon?: React.ReactNode
}) {
  const { title, value, helper, unit, ok, icon } = props
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent sx={{ py: 1.5 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
          <Typography variant="caption" color="text.secondary">{title}</Typography>
          {ok !== undefined ? <StatusChip ok={ok} /> : icon ? <Box sx={{ color: 'primary.main' }}>{icon}</Box> : null}
        </Stack>
        <Typography variant="h5" fontWeight={700} mt={0.5} sx={{ wordBreak: 'break-word' }}>
          {value}{unit ? <Typography component="span" variant="body2" color="text.secondary" ml={0.5}>{unit}</Typography> : null}
        </Typography>
        {helper ? (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
            {helper}
          </Typography>
        ) : null}
      </CardContent>
    </Card>
  )
}

function InfoRows({ rows }: { rows: Array<[string, unknown, string?]> }) {
  return (
    <Stack spacing={1}>
      {rows.map(([label, value, unit]) => (
        <Stack key={label} direction="row" justifyContent="space-between" gap={2}>
          <Typography variant="body2" color="text.secondary">{label}</Typography>
          <Typography variant="body2" fontWeight={600} sx={{ textAlign: 'right', wordBreak: 'break-word' }}>
            {value === null || value === undefined || value === '' ? '--' : String(value)}{value !== null && value !== undefined && unit ? unit : ''}
          </Typography>
        </Stack>
      ))}
    </Stack>
  )
}

export default function AdminInfraPage() {
  const queryClient = useQueryClient()
  const healthQuery = useQuery({
    queryKey: ['ai-infra-health'],
    queryFn: () => aiApi.infraHealth(),
    refetchInterval: 30000,
  })
  const cacheQuery = useQuery({
    queryKey: ['ai-cache-stats'],
    queryFn: () => aiApi.cacheStats(),
    refetchInterval: 30000,
  })
  const cacheDiagQuery = useQuery({
    queryKey: ['ai-cache-diagnostics'],
    queryFn: () => aiApi.cacheDiagnostics(),
    refetchInterval: 60000,
  })
  const searchQuery = useQuery({
    queryKey: ['ai-search-stats'],
    queryFn: () => aiApi.searchStats(),
    refetchInterval: 30000,
  })

  const infraItems = useMemo(() => parseInfraHealth(healthQuery.data), [healthQuery.data])
  const milvus = useMemo(() => getStatus(infraItems, 'milvus'), [infraItems])
  const elasticsearch = useMemo(() => getStatus(infraItems, 'elasticsearch'), [infraItems])
  const redis = useMemo(() => getStatus(infraItems, 'redis'), [infraItems])
  const llm = useMemo(() => getStatus(infraItems, 'llm'), [infraItems])
  const knownComponents = new Set(['milvus', 'elasticsearch', 'redis', 'llm'])
  const otherItems = infraItems.filter((item) => !knownComponents.has(item.component?.toLowerCase()))
  const downItems = infraItems.filter((item) => !item.ok)
  const cache = (cacheQuery.data ?? {}) as AiCacheStatsVO
  const cacheDiag = cacheDiagQuery.data as AiCacheDiagnosticsVO | undefined
  const search = (searchQuery.data ?? {}) as AiSearchStatsVO
  const isFetching = healthQuery.isFetching || cacheQuery.isFetching || cacheDiagQuery.isFetching || searchQuery.isFetching
  const errors = [
    ['健康检查', '/ai/admin/infra/health', healthQuery.error],
    ['缓存统计', '/ai/admin/infra/cache/stats', cacheQuery.error],
    ['Redis 诊断', '/ai/admin/infra/cache/diagnostics', cacheDiagQuery.error],
    ['检索统计', '/ai/admin/infra/search/stats', searchQuery.error],
  ].filter(([, , error]) => Boolean(error)) as Array<[string, string, unknown]>

  const refreshAll = () => {
    void queryClient.invalidateQueries({ queryKey: ['ai-infra-health'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-cache-stats'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-cache-diagnostics'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-search-stats'] })
  }

  const cacheChartOption = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['50%', '70%'],
      data: [
        { name: '命中', value: Number(cache.hit ?? 0) },
        { name: '未命中', value: Number(cache.miss ?? 0) },
      ],
      label: { show: true },
    }],
  }

  return (
    <Box
      data-testid="admin-infra-page"
      data-ready-endpoints={ADMIN_INFRA_READY_ENDPOINTS}
      data-unsupported-endpoints={ADMIN_INFRA_UNSUPPORTED_ENDPOINTS}
      data-no-local-health-fallback="true"
      data-no-automatic-cache-ttl-mutation="true"
      data-no-cache-flush-action="true"
      data-no-cache-prewarm-action="true"
      data-no-mock-search-metric-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="AI 基础设施管理"
        subtitle="集中查看向量库、搜索引擎、Redis、LLM 与检索链路的在线状态和运行指标。"
        actions={
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Button component={RouterLink} to="/admin/ai/monitoring" size="small" variant="outlined">
              运行监控
            </Button>
            <Button size="small" variant="contained" startIcon={<RefreshIcon />} onClick={refreshAll} disabled={isFetching}>
              刷新
            </Button>
          </Stack>
        }
      />

      {isFetching ? <LinearProgress /> : null}

      {errors.length > 0 ? (
        <Alert
          severity="error"
          data-testid="admin-infra-load-error"
          data-no-local-health-fallback="true"
          data-no-local-cache-fallback="true"
          data-no-local-search-metric-fallback="true"
          action={<Button color="inherit" size="small" onClick={refreshAll}>重试</Button>}
        >
          {errors.map(([label, endpoint, error]) => `${label}失败（${endpoint}）：${errorMessage(error)}`).join('；')}
        </Alert>
      ) : null}

      {downItems.length > 0 ? (
        <Alert
          severity="warning"
          data-testid="admin-infra-down-components"
          data-source-endpoint="/ai/admin/infra/health"
          data-no-local-health-fallback="true"
          data-no-browser-local-probe="true"
        >
          异常组件：{downItems.map((item) => `${item.component}${item.message ? `（${item.message}）` : ''}`).join('；')}。Docker 后端连接本机 Ollama 时应使用 `OLLAMA_URL=http://host.docker.internal:11434`，向量检索还需确认 Milvus 与 Elasticsearch 已就绪。
        </Alert>
      ) : null}

      {!healthQuery.isError && infraItems.length === 0 ? (
        <Alert
          severity="info"
          data-testid="admin-infra-unreported-health"
          data-source-endpoint="/ai/admin/infra/health"
          data-no-disconnected-inference="true"
          data-no-local-health-fallback="true"
        >
          健康检查接口未返回组件明细，页面按“未上报”展示 Milvus、Elasticsearch、Redis 和 LLM；请确认后端采集器和配置是否启用。
        </Alert>
      ) : null}

      <Grid container spacing={2}>
        {[
          { title: 'Milvus 向量库', ...milvus },
          { title: 'Elasticsearch', ...elasticsearch },
          { title: 'Redis 缓存', ...redis },
          { title: 'LLM 服务', ...llm },
        ].map((item) => (
          <Grid item xs={12} sm={6} md={3} key={item.title}>
            <MetricCard
              title={item.title}
              value={item.ok ? '在线' : (item.message || '离线')}
              helper={item.ok ? '健康检查通过' : item.message === '未上报' ? '后端健康检查未返回该组件' : '请检查服务连接与配置'}
              ok={item.ok}
            />
          </Grid>
        ))}
      </Grid>

      {otherItems.length > 0 ? (
        <Grid container spacing={2}>
          {otherItems.map((item) => (
            <Grid item xs={12} sm={6} md={3} key={item.component}>
              <MetricCard title={item.component} value={item.ok ? '在线' : (item.message || '离线')} ok={item.ok} />
            </Grid>
          ))}
        </Grid>
      ) : null}

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="KB 业务命中率" value={cache.hitRate != null ? Number(cache.hitRate).toFixed(1) : '--'} unit="%" helper={`Hit ${formatNumber(cache.hit)} / Total ${formatNumber(cache.total)}`} icon={<StorageIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="缓存键总数" value={formatNumber(cache.keyCount ?? cacheDiag?.dbSize)} helper={cacheDiag?.host || 'Redis DB size'} icon={<StorageIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Redis 全局命中率" value={cacheDiag?.redisStats?.globalHitRate != null ? formatPercent(cacheDiag.redisStats.globalHitRate) : '--'} helper="Redis INFO keyspace 命中率" icon={<StorageIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="KB 缓存 TTL" value={formatSeconds(cacheDiag?.kbCacheTtlSeconds)} helper={`Embedding TTL ${cacheDiag?.embeddingCacheTtlDays ?? '--'} 天`} icon={<StorageIcon />} />
        </Grid>
      </Grid>

      {cacheDiag ? (
        <Card
          variant="outlined"
          data-testid="admin-infra-cache-diagnostics-contract"
          data-source-endpoint="/ai/admin/infra/cache/diagnostics"
          data-no-automatic-cache-ttl-mutation="true"
          data-no-cache-flush-action="true"
          data-no-cache-prewarm-action="true"
          data-no-mock-hot-key-injection="true"
        >
          <CardContent>
            <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={1.5}>
              <Box>
                <Typography variant="subtitle1" fontWeight={700}>Redis 诊断</Typography>
                <Typography variant="caption" color="text.secondary">
                  {cacheDiag.host || 'Redis'} · Key {formatNumber(cacheDiag.dbSize)} · 最近扫描 {formatNumber(cacheDiag.scan?.scanned)}
                </Typography>
              </Box>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Chip size="small" variant="outlined" label={`Hit ${formatNumber(cacheDiag.redisStats?.keyspaceHits)}`} />
                <Chip size="small" variant="outlined" label={`Miss ${formatNumber(cacheDiag.redisStats?.keyspaceMisses)}`} />
                <Chip size="small" variant="outlined" label={`过期 ${formatNumber(cacheDiag.redisStats?.expiredKeys)}`} />
                <Chip size="small" color={Number(cacheDiag.redisStats?.evictedKeys ?? 0) > 0 ? 'warning' : 'default'} variant="outlined" label={`淘汰 ${formatNumber(cacheDiag.redisStats?.evictedKeys)}`} />
              </Stack>
            </Stack>
            <Divider sx={{ my: 1.5 }} />
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>Key 前缀分布</Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {Object.entries(cacheDiag.scan?.prefixCounts ?? {}).slice(0, 10).map(([prefix, count]) => (
                    <Chip key={prefix} size="small" label={`${prefix} ${Number(count).toLocaleString()}`} />
                  ))}
                </Stack>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>TTL 分布</Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {Object.entries(cacheDiag.scan?.ttlBuckets ?? {}).map(([bucket, count]) => (
                    <Chip key={bucket} size="small" variant="outlined" label={`${bucket} ${Number(count).toLocaleString()}`} />
                  ))}
                </Stack>
              </Grid>
            </Grid>
            {Array.isArray(cacheDiag.suggestions) && cacheDiag.suggestions.length > 0 ? (
              <Alert severity="info" sx={{ mt: 1.5 }}>{cacheDiag.suggestions.slice(0, 4).join('；')}</Alert>
            ) : null}
          </CardContent>
        </Card>
      ) : null}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>缓存命中分布</Typography>
              {(Number(cache.hit ?? 0) + Number(cache.miss ?? 0)) > 0 ? (
                <ReactECharts option={cacheChartOption} style={{ height: 220 }} />
              ) : (
                <Alert
                  severity="info"
                  data-testid="admin-infra-cache-empty"
                  data-source-endpoint="/ai/admin/infra/cache/stats"
                  data-no-local-cache-fallback="true"
                >
                  暂无缓存命中/未命中统计。
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card
            variant="outlined"
            sx={{ height: '100%' }}
            data-testid="admin-infra-search-stats-contract"
            data-source-endpoint="/ai/admin/infra/search/stats"
            data-no-mock-search-metric-fallback="true"
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>检索统计</Typography>
              <InfoRows rows={[
                ['总请求数', search.totalRequests ?? search.totalQueries],
                ['QPS', search.qps],
                ['平均耗时', search.avgLatencyMs, ' ms'],
                ['P95 耗时', search.p95Ms, ' ms'],
                ['P99 耗时', search.p99Ms ?? search.p99LatencyMs, ' ms'],
              ]} />
              <Alert severity="info" sx={{ mt: 1.5 }} icon={<TravelExploreIcon />}>
                检索延迟升高时优先检查 Milvus、Elasticsearch 与 embedding 服务，再决定是否临时降级到全文检索。
              </Alert>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
