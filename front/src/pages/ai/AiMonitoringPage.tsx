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
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import RefreshIcon from '@mui/icons-material/Refresh'
import StorageIcon from '@mui/icons-material/Storage'
import SpeedIcon from '@mui/icons-material/Speed'
import TroubleshootIcon from '@mui/icons-material/Troubleshoot'
import { Link as RouterLink } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { aiApi } from '@/api/ai'
import type { AiCacheDiagnosticsVO, AiCacheStatsVO, AiInfraHealthItem, AiSearchStatsVO } from '@/types/ai'

const AI_MONITORING_READY_ENDPOINTS = [
  '/ai/admin/infra/health',
  '/ai/admin/infra/cache/stats',
  '/ai/admin/infra/cache/diagnostics',
  '/ai/admin/infra/search/stats',
].join('|')

const AI_MONITORING_UNSUPPORTED_ENDPOINTS = [
  '/ai/admin/infra/cache/ttl/update',
  '/ai/admin/infra/cache/flush',
  '/ai/admin/infra/cache/prewarm',
  '/ai/admin/infra/cache/hot-key/mock',
  '/ai/admin/infra/search/mock',
  '/ai/admin/infra/health/local-probe',
].join('|')

function formatStatValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '--'
  if (typeof value === 'number') return Number.isFinite(value) ? value.toLocaleString() : '--'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function toOptionalPercent(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  if (!Number.isFinite(n)) return null
  return Math.max(0, Math.min(100, n > 1 ? n : n * 100))
}

function formatPercent(value: unknown): string {
  const percent = toOptionalPercent(value)
  return percent === null ? '未上报' : `${percent.toFixed(1)}%`
}

function formatSeconds(value: unknown): string {
  const seconds = Number(value ?? 0)
  if (!Number.isFinite(seconds) || seconds <= 0) return '--'
  if (seconds >= 86400) return `${(seconds / 86400).toFixed(1)} 天`
  if (seconds >= 3600) return `${(seconds / 3600).toFixed(1)} 小时`
  if (seconds >= 60) return `${Math.round(seconds / 60)} 分钟`
  return `${Math.round(seconds)} 秒`
}

function formatLatency(value: unknown): string {
  const n = Number(value)
  if (!Number.isFinite(n)) return '--'
  return `${Math.round(n).toLocaleString()} ms`
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return String(error || '加载失败')
}

function compactStrings(values: Array<string | null>): string[] {
  return values.filter((value): value is string => Boolean(value))
}

function SummaryCard(props: {
  title: string
  value: string
  helper: string
  icon: React.ReactNode
  severity?: 'success' | 'warning' | 'error' | 'info'
}) {
  const { title, value, helper, icon, severity = 'info' } = props
  const colorMap = {
    success: 'success.main',
    warning: 'warning.main',
    error: 'error.main',
    info: 'primary.main',
  } as const
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="caption" color="text.secondary">{title}</Typography>
            <Typography variant="h5" fontWeight={700} sx={{ mt: 0.5 }}>{value}</Typography>
          </Box>
          <Box sx={{ color: colorMap[severity], display: 'flex' }}>{icon}</Box>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          {helper}
        </Typography>
      </CardContent>
    </Card>
  )
}

function HealthCard({ item }: { item: AiInfraHealthItem }) {
  const isOk = item.ok === true
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
          <Typography variant="subtitle2" noWrap title={item.component}>{item.component}</Typography>
          <Chip
            icon={isOk ? <CheckCircleIcon /> : <ErrorOutlineIcon />}
            label={isOk ? '正常' : '异常'}
            color={isOk ? 'success' : 'error'}
            size="small"
          />
        </Stack>
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ display: 'block', mt: 1, minHeight: 36, wordBreak: 'break-word' }}
        >
          {item.message || (isOk ? '服务可用，最近一次健康检查通过' : '未返回异常详情')}
        </Typography>
      </CardContent>
    </Card>
  )
}

function ProgressRow(props: { label: string; value: number | null; helper?: string; missingHelper?: string }) {
  const { label, value, helper } = props
  const color = value === null ? 'inherit' : value >= 80 ? 'success' : value >= 50 ? 'warning' : 'error'
  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
        <Typography variant="body2">{label}</Typography>
        <Typography variant="body2" fontWeight={700}>{value === null ? '未上报' : `${value.toFixed(1)}%`}</Typography>
      </Stack>
      <LinearProgress color={color} variant="determinate" value={value ?? 0} sx={{ height: 8, borderRadius: 1, mt: 0.75 }} />
      {helper || props.missingHelper ? (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          {value === null ? props.missingHelper : helper}
        </Typography>
      ) : null}
    </Box>
  )
}

function StatRows({ rows }: { rows: Array<[string, unknown, string?]> }) {
  return (
    <Stack spacing={1}>
      {rows.map(([label, value, unit]) => (
        <Stack key={label} direction="row" justifyContent="space-between" gap={2}>
          <Typography variant="body2" color="text.secondary">{label}</Typography>
          <Typography variant="body2" fontWeight={600} textAlign="right" sx={{ wordBreak: 'break-word' }}>
            {formatStatValue(value)}{value !== null && value !== undefined && unit ? unit : ''}
          </Typography>
        </Stack>
      ))}
    </Stack>
  )
}

export default function AiMonitoringPage() {
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

  const infraItems = useMemo<AiInfraHealthItem[]>(() => (
    Array.isArray(healthQuery.data) ? healthQuery.data : []
  ), [healthQuery.data])
  const healthyCount = infraItems.filter(item => item.ok).length
  const downItems = infraItems.filter(item => !item.ok)
  const cache = (cacheQuery.data ?? {}) as AiCacheStatsVO
  const cacheDiag = cacheDiagQuery.data as AiCacheDiagnosticsVO | undefined
  const search = (searchQuery.data ?? {}) as AiSearchStatsVO
  const businessHitRate = toOptionalPercent(cacheDiag?.businessStats?.hitRate ?? cache.hitRate)
  const globalHitRate = toOptionalPercent(cacheDiag?.redisStats?.globalHitRate)
  const searchLatency = search.p95Ms ?? search.p99Ms ?? search.avgLatencyMs
  const isFetching = healthQuery.isFetching || cacheQuery.isFetching || cacheDiagQuery.isFetching || searchQuery.isFetching

  const refreshAll = () => {
    void queryClient.invalidateQueries({ queryKey: ['ai-infra-health'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-cache-stats'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-cache-diagnostics'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-search-stats'] })
  }

  const loadErrors = compactStrings([
    healthQuery.error ? `健康检查加载失败（/ai/admin/infra/health）：${errorMessage(healthQuery.error)}` : null,
    cacheQuery.error ? `缓存统计加载失败（/ai/admin/infra/cache/stats）：${errorMessage(cacheQuery.error)}` : null,
    cacheDiagQuery.error ? `缓存诊断加载失败（/ai/admin/infra/cache/diagnostics）：${errorMessage(cacheDiagQuery.error)}` : null,
    searchQuery.error ? `检索统计加载失败（/ai/admin/infra/search/stats）：${errorMessage(searchQuery.error)}` : null,
  ])
  const healthUnreported = healthQuery.isSuccess && infraItems.length === 0
  const suggestions = cacheDiag?.suggestions ?? []
  const generatedSuggestions = compactStrings([
    businessHitRate !== null && businessHitRate < 50 && suggestions.length === 0
      ? 'KB 业务缓存命中率低于 50%，但后端未返回诊断建议；请检查热点 query 归一化、预热任务和业务缓存 TTL。'
      : null,
    globalHitRate !== null && globalHitRate < 50 && suggestions.length === 0
      ? 'Redis 全局命中率低于 50%，但后端未返回诊断建议；请核对热点 key、淘汰策略和缓存 TTL 配置。'
      : null,
  ])
  const visibleSuggestions = suggestions.length > 0 ? suggestions : generatedSuggestions

  return (
    <Box
      data-testid="ai-monitoring-page"
      data-ready-endpoints={AI_MONITORING_READY_ENDPOINTS}
      data-unsupported-endpoints={AI_MONITORING_UNSUPPORTED_ENDPOINTS}
      data-no-local-health-fallback="true"
      data-no-automatic-cache-ttl-mutation="true"
      data-no-cache-flush-action="true"
      data-no-cache-prewarm-action="true"
      data-no-mock-search-metric-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="AI 运行监控"
        subtitle="持续观察 AI 基础设施、Redis 缓存、知识库检索延迟和降级建议。"
        actions={
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Button component={RouterLink} to="/admin/ai/admin-infra" size="small" variant="outlined">
              基础设施详情
            </Button>
            <Button startIcon={<RefreshIcon />} size="small" variant="contained" onClick={refreshAll} disabled={isFetching}>
              刷新
            </Button>
          </Stack>
        }
      />

      {isFetching ? <LinearProgress /> : null}

      {loadErrors.length > 0 ? (
        <Alert
          severity="error"
          data-testid="ai-monitoring-load-error"
          data-no-local-health-fallback="true"
          data-no-local-cache-fallback="true"
          data-no-local-search-metric-fallback="true"
          action={<Button color="inherit" size="small" onClick={refreshAll}>重试</Button>}
        >
          {loadErrors.join('；')}
        </Alert>
      ) : null}

      {downItems.length > 0 ? (
        <Alert
          severity="warning"
          data-testid="ai-monitoring-down-components"
          data-source-endpoint="/ai/admin/infra/health"
          data-no-local-health-fallback="true"
          data-no-browser-local-probe="true"
        >
          异常组件：{downItems.map(item => `${item.component}${item.message ? `（${item.message}）` : ''}`).join('；')}
        </Alert>
      ) : null}

      {healthUnreported ? (
        <Alert
          severity="info"
          data-testid="ai-monitoring-unreported-health"
          data-source-endpoint="/ai/admin/infra/health"
          data-no-disconnected-inference="true"
          data-no-local-health-fallback="true"
        >
          健康检查接口未返回组件明细，页面按“未上报”处理；请确认后端 `/ai/admin/infra/health` 采集器是否启用。
        </Alert>
      ) : null}

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard
            title="健康组件"
            value={`${healthyCount}/${infraItems.length || 0}`}
            helper={infraItems.length > 0 ? '后端健康检查返回组件数' : '健康检查接口未返回组件明细'}
            severity={downItems.length > 0 ? 'error' : 'success'}
            icon={<CheckCircleIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard
            title="KB 业务命中率"
            value={businessHitRate === null ? '未上报' : `${businessHitRate.toFixed(1)}%`}
            helper={`Hit ${formatStatValue(cacheDiag?.businessStats?.hit ?? cache.hit)} / Total ${formatStatValue(cacheDiag?.businessStats?.total ?? cache.total)}`}
            severity={businessHitRate === null ? 'info' : businessHitRate >= 80 ? 'success' : businessHitRate >= 50 ? 'warning' : 'error'}
            icon={<StorageIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard
            title="Redis 全局命中率"
            value={formatPercent(cacheDiag?.redisStats?.globalHitRate)}
            helper={`Key ${Number(cacheDiag?.dbSize ?? cache.keyCount ?? 0).toLocaleString()}，TTL ${formatSeconds(cacheDiag?.kbCacheTtlSeconds)}`}
            severity={globalHitRate === null ? 'info' : globalHitRate >= 80 ? 'success' : globalHitRate >= 50 ? 'warning' : 'error'}
            icon={<SpeedIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard
            title="检索延迟"
            value={formatLatency(searchLatency)}
            helper={`QPS ${formatStatValue(search.qps)}，请求 ${formatStatValue(search.totalRequests ?? search.totalQueries)}`}
            severity={Number(searchLatency ?? 0) > 2000 ? 'warning' : 'info'}
            icon={<TroubleshootIcon />}
          />
        </Grid>
      </Grid>

      <Card
        variant="outlined"
        data-testid="ai-monitoring-health-contract"
        data-source-endpoint="/ai/admin/infra/health"
        data-no-local-health-fallback="true"
        data-reported-count={String(infraItems.length)}
      >
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1} sx={{ mb: 1.5 }}>
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>基础设施健康状态</Typography>
              <Typography variant="caption" color="text.secondary">每 30 秒自动刷新，异常组件会在顶部提示。</Typography>
            </Box>
            <Chip label={downItems.length > 0 ? '存在异常' : '运行正常'} color={downItems.length > 0 ? 'error' : 'success'} size="small" />
          </Stack>
          {infraItems.length > 0 ? (
            <Grid container spacing={1.5}>
              {infraItems.map((item) => (
                <Grid key={item.component} item xs={12} sm={6} md={3}>
                  <HealthCard item={item} />
                </Grid>
              ))}
            </Grid>
          ) : (
            <Alert severity="info">健康检查接口未返回组件明细，按“未上报”展示；请确认后端 `/ai/admin/infra/health` 已可访问并启用采集。</Alert>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid item xs={12} lg={6}>
          <Card
            variant="outlined"
            sx={{ height: '100%' }}
            data-testid="ai-monitoring-cache-contract"
            data-source-endpoints="/ai/admin/infra/cache/stats|/ai/admin/infra/cache/diagnostics"
            data-no-automatic-cache-ttl-mutation="true"
            data-no-cache-flush-action="true"
            data-no-cache-prewarm-action="true"
            data-no-mock-hot-key-injection="true"
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700}>Redis 与业务缓存</Typography>
              <Typography variant="caption" color="text.secondary">
                {cacheDiag?.host || 'Redis'} · Embedding TTL {formatStatValue(cacheDiag?.embeddingCacheTtlDays)} 天
              </Typography>
              <Stack spacing={2} sx={{ mt: 2 }}>
                <ProgressRow
                  label="KB 业务缓存命中率"
                  value={businessHitRate}
                  helper="来自知识库业务缓存统计，低于 Redis 全局值时优先检查查询规范化和热点预热。"
                  missingHelper="缓存统计接口未上报业务命中率，无法判断热点 query 是否命中。"
                />
                <ProgressRow
                  label="Redis 全局命中率"
                  value={globalHitRate}
                  helper="来自 Redis INFO keyspace_hits/keyspace_misses。"
                  missingHelper="缓存诊断接口未上报 Redis INFO 命中率，不能按 0% 处理。"
                />
              </Stack>
              <Divider sx={{ my: 2 }} />
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>Key 前缀分布</Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {Object.entries(cacheDiag?.scan?.prefixCounts ?? {}).slice(0, 12).map(([prefix, count]) => (
                  <Chip key={prefix} size="small" label={`${prefix} ${Number(count).toLocaleString()}`} />
                ))}
                {Object.keys(cacheDiag?.scan?.prefixCounts ?? {}).length === 0 ? (
                  <Chip size="small" variant="outlined" label="暂无扫描结果" />
                ) : null}
              </Stack>
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 2, mb: 1 }}>TTL 分布</Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {Object.entries(cacheDiag?.scan?.ttlBuckets ?? {}).map(([bucket, count]) => (
                  <Chip key={bucket} size="small" variant="outlined" label={`${bucket} ${Number(count).toLocaleString()}`} />
                ))}
                {Object.keys(cacheDiag?.scan?.ttlBuckets ?? {}).length === 0 ? (
                  <Chip size="small" variant="outlined" label="暂无 TTL 数据" />
                ) : null}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={6}>
          <Card
            variant="outlined"
            sx={{ height: '100%' }}
            data-testid="ai-monitoring-search-contract"
            data-source-endpoint="/ai/admin/infra/search/stats"
            data-no-mock-search-metric-fallback="true"
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700}>检索与告警建议</Typography>
              <Typography variant="caption" color="text.secondary">用于判断知识库检索链路是否需要降级或扩容。</Typography>
              <Divider sx={{ my: 1.5 }} />
              {searchLatency == null ? (
                <Alert
                  severity="info"
                  sx={{ mb: 1.5 }}
                  data-testid="ai-monitoring-search-latency-missing"
                  data-no-local-search-metric-fallback="true"
                >
                  检索统计接口未上报延迟分位数据，当前只展示已返回的请求量和 QPS。
                </Alert>
              ) : null}
              <StatRows rows={[
                ['P50 延迟', search.p50Ms, ' ms'],
                ['P95 延迟', search.p95Ms, ' ms'],
                ['P99 延迟', search.p99Ms ?? search.p99LatencyMs, ' ms'],
                ['平均延迟', search.avgLatencyMs, ' ms'],
                ['总查询量', search.totalQueries ?? search.totalRequests],
              ]} />
              <Divider sx={{ my: 1.5 }} />
              <Stack spacing={1}>
                {visibleSuggestions.slice(0, 5).map((suggestion, index) => (
                  <Alert
                    key={`${suggestion}-${index}`}
                    severity="info"
                    sx={{ py: 0.5 }}
                    data-testid="ai-monitoring-suggestion"
                    data-no-automatic-cache-ttl-mutation="true"
                    data-no-cache-prewarm-action="true"
                  >
                    {suggestion}
                  </Alert>
                ))}
                {visibleSuggestions.length === 0 ? (
                  <Alert severity="success">暂无明确告警建议，当前监控数据未触发缓存或检索侧风险提示。</Alert>
                ) : null}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
