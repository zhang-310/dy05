import { useMemo } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Grid, Stack, Typography } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useTheme } from '@mui/material/styles'
import { useQuery } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { systemApi } from '@/api/system'
import { DataGridEmptyOverlay, ErrorAlert, EmptyState, PageHeader, StandardDataGrid } from '@/components/base'
import { normalizeRecord, normalizeRows } from '@/utils/response-normalize'
import type { GridColDef } from '@mui/x-data-grid'

interface PerformancePoint {
  time?: string
  ts?: string
  p50?: number
  p95?: number
  p99?: number
  value?: number
  avg?: number
  avgMs?: number
}

interface SlowQueryRow {
  path?: string
  avgMs?: number
  callCount?: number
  maxMs?: number
}

interface PerformanceAnalysis {
  status?: string
  message?: string
  data?: Record<string, unknown>
}

type ContractStatus = 'ready' | 'degraded' | 'error'

const PERFORMANCE_ENDPOINTS = {
  current: '/system/performance/metrics/current',
  timeseries: '/system/performance/api/timeseries',
  slowQuery: '/system/performance/query/slow',
  analysis: '/system/performance/query/analysis',
  cache: '/system/performance/cache/statistics',
} as const
const PERFORMANCE_READY_ENDPOINTS = Object.values(PERFORMANCE_ENDPOINTS).join('|')
const PERFORMANCE_UNSUPPORTED_ENDPOINTS = [
  '/system/performance/mock',
  '/system/performance/cache/hot-keys/mock',
  '/system/performance/cache/ttl/update',
  '/system/performance/cache/flush',
  '/system/performance/query/slow/mock',
  '/system/performance/api/timeseries/mock',
].join('|')
const PERFORMANCE_UNSUPPORTED_ACTIONS = [
  'local-performance-metric-fallback',
  'mock-timeseries-injection',
  'mock-slow-query-injection',
  'mock-cache-hot-key-injection',
  'automatic-cache-ttl-mutation',
].join('|')

const PERFORMANCE_COLLECTOR_DOWNGRADE_MESSAGE =
  '后端 system/performance 已接入 JVM、请求性能窗口与 Redis INFO。若最近没有请求样本、没有慢接口或 Redis 尚无命中/未命中计数，页面会显式降级为空态，不再填充 mock 指标。'

const CACHE_HIT_RATE_WARNING_RATIO = 0.5
const CACHE_HIT_RATE_CRITICAL_RATIO = 0.3

function statValue(value: unknown) {
  if (value == null) return '--'
  if (typeof value === 'number') return value.toLocaleString('zh-CN')
  return String(value)
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function formatPercent(value: unknown): string {
  if (value == null) return '--'
  const raw = toNumber(value, Number.NaN)
  if (!Number.isFinite(raw)) return '--'
  const normalized = raw > 0 && raw <= 1 ? raw * 100 : raw
  return `${normalized.toFixed(1)}%`
}

function normalizePercentRatio(value: unknown): number | null {
  if (value == null) return null
  const raw = toNumber(value, Number.NaN)
  if (!Number.isFinite(raw)) return null
  return raw > 1 ? raw / 100 : raw
}

function MetricCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" fontWeight={700}>{value}</Typography>
        {hint && <Typography variant="caption" color="text.secondary">{hint}</Typography>}
      </CardContent>
    </Card>
  )
}

function contractChipColor(status: ContractStatus): 'success' | 'warning' | 'error' {
  if (status === 'ready') return 'success'
  if (status === 'error') return 'error'
  return 'warning'
}

function PerformanceContractCards({
  items,
}: {
  items: Array<{ name: string; endpoint: string; status: ContractStatus; detail: string; sampleCount?: number }>
}) {
  return (
    <Grid container spacing={1.5}>
      {items.map(item => (
        <Grid item xs={12} sm={6} md={2.4} key={item.endpoint}>
          <Card
            variant="outlined"
            data-testid="performance-contract-card"
            data-collector-name={item.name}
            data-contract-status={item.status}
            data-contract-endpoint={item.endpoint}
            data-sample-count={item.sampleCount ?? ''}
            sx={{ height: '100%', borderColor: `${contractChipColor(item.status)}.light` }}
          >
            <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Stack spacing={0.75}>
                <Typography variant="subtitle2" fontWeight={700}>{item.name}</Typography>
                <Chip
                  label={item.status === 'ready' ? '已接入' : item.status === 'error' ? '接口异常' : '显式降级'}
                  color={contractChipColor(item.status)}
                  size="small"
                  variant="outlined"
                />
                <Typography variant="caption" color="text.secondary">{item.detail}</Typography>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  )
}

export default function PerformanceMonitoringPage() {
  const theme = useTheme()
  const {
    data: current,
    isError: currentError,
    error: currentErrorData,
    refetch: refetchCurrent,
  } = useQuery({
    queryKey: ['perf-current'],
    queryFn: () => systemApi.performanceCurrent(),
    refetchInterval: 15000,
  })

  const {
    data: apiTimeseries,
    isLoading: tsLoading,
    isError: tsError,
    error: tsErrorData,
    refetch: refetchTimeseries,
  } = useQuery({
    queryKey: ['perf-timeseries'],
    queryFn: () => systemApi.performanceTimeseries({ metric: 'response_time', hours: 24 }),
    refetchInterval: 60000,
  })

  const {
    data: slowQuery,
    isLoading: slowLoading,
    isError: slowError,
    error: slowErrorData,
    refetch: refetchSlowQuery,
  } = useQuery({
    queryKey: ['perf-slow-query'],
    queryFn: () => systemApi.performanceSlowQuery<SlowQueryRow>({ rows: 10, minMs: 500 }),
  })

  const {
    data: analysis,
    isError: analysisError,
    error: analysisErrorData,
    refetch: refetchAnalysis,
  } = useQuery({
    queryKey: ['perf-analysis'],
    queryFn: systemApi.performanceAnalysis,
  })

  const {
    data: cacheStats,
    isLoading: cacheLoading,
    isError: cacheError,
    error: cacheErrorData,
    refetch: refetchCache,
  } = useQuery({
    queryKey: ['perf-cache-stats'],
    queryFn: systemApi.performanceCacheStatistics,
    refetchInterval: 60000,
  })

  const currentData = normalizeRecord(current, ['metrics', 'current', 'currentMetrics', 'performanceMetrics'])
  const timeseries = normalizeRows<PerformancePoint>(apiTimeseries)
  const slowRows = normalizeRows<SlowQueryRow>(slowQuery)
  const cacheData = normalizeRecord(cacheStats, ['cache', 'cacheStats', 'cacheStatistics', 'statistics'])
  const analysisData = normalizeRecord(analysis, ['analysis', 'queryAnalysis', 'performanceAnalysis']) as PerformanceAnalysis
  const cacheHitRate = cacheData.hitRate ?? cacheData.hitRatePercent
    ?? (toNumber(cacheData.hitCount) + toNumber(cacheData.missCount) > 0
      ? toNumber(cacheData.hitCount) / (toNumber(cacheData.hitCount) + toNumber(cacheData.missCount))
      : undefined)
  const cacheHitRatio = normalizePercentRatio(cacheHitRate)
  const currentContractStatus: ContractStatus = currentError ? 'error' : Object.keys(currentData).length > 0 ? 'ready' : 'degraded'
  const timeseriesContractStatus: ContractStatus = tsError ? 'error' : timeseries.length > 0 ? 'ready' : 'degraded'
  const slowQueryContractStatus: ContractStatus = slowError ? 'error' : slowRows.length > 0 ? 'ready' : 'degraded'
  const analysisContractStatus: ContractStatus = analysisError ? 'error' : Object.keys(analysisData).length > 0 ? 'ready' : 'degraded'
  const cacheContractStatus: ContractStatus = cacheError ? 'error' : cacheHitRatio == null ? 'degraded' : 'ready'
  const cacheHealth =
    cacheHitRatio == null
      ? 'unknown'
      : cacheHitRatio < CACHE_HIT_RATE_CRITICAL_RATIO
        ? 'critical'
        : cacheHitRatio < CACHE_HIT_RATE_WARNING_RATIO
          ? 'warning'
          : 'healthy'
  const showCacheHitRateAdvice = cacheHealth === 'critical' || cacheHealth === 'warning'

  const slowColumns: GridColDef<SlowQueryRow>[] = [
    { field: 'path', headerName: '接口路径', flex: 1, minWidth: 220, valueGetter: (_, row) => row.path ?? '--' },
    { field: 'avgMs', headerName: '平均耗时', width: 110, renderCell: ({ value }) => <Chip label={`${value ?? 0}ms`} size="small" color="warning" variant="outlined" /> },
    { field: 'callCount', headerName: '调用次数', width: 100, valueGetter: (_, row) => row.callCount ?? 0 },
    { field: 'maxMs', headerName: '最大耗时', width: 100, valueGetter: (_, row) => row.maxMs ?? 0 },
  ]

  const timeseriesOption = useMemo(() => {
    const statusColors = {
      p50: theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main,
      p95: theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main,
      p99: theme.palette.mode === 'dark' ? theme.palette.error.light : theme.palette.error.main,
    }
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['P50', 'P95', 'P99'], bottom: 0 },
      xAxis: { type: 'category', data: timeseries.map(d => d.time ?? d.ts ?? '') },
      yAxis: { type: 'value', name: 'ms' },
      series: [
        { name: 'P50', type: 'line', smooth: true, data: timeseries.map(d => d.p50 ?? d.avg ?? d.avgMs ?? d.value ?? 0), color: statusColors.p50 },
        { name: 'P95', type: 'line', smooth: true, data: timeseries.map(d => d.p95 ?? d.value ?? 0), color: statusColors.p95 },
        { name: 'P99', type: 'line', smooth: true, data: timeseries.map(d => d.p99 ?? d.p95 ?? d.value ?? 0), color: statusColors.p99 },
      ],
    }
  }, [theme, timeseries])

  const headlineError = currentErrorData ?? tsErrorData ?? slowErrorData ?? analysisErrorData ?? cacheErrorData
  const slowList = slowRows.length > 0
  const hasAnyError = currentError || tsError || slowError || analysisError || cacheError
  const allCollectorsDegraded = [currentContractStatus, timeseriesContractStatus, slowQueryContractStatus, analysisContractStatus, cacheContractStatus]
    .every(status => status === 'degraded')

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
      data-testid="performance-monitoring-page-workbench"
      data-contract-scope="system-performance-collector-observability"
      data-ready-endpoints={PERFORMANCE_READY_ENDPOINTS}
      data-unsupported-endpoints={PERFORMANCE_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={PERFORMANCE_UNSUPPORTED_ACTIONS}
      data-current-status={currentContractStatus}
      data-timeseries-status={timeseriesContractStatus}
      data-slow-query-status={slowQueryContractStatus}
      data-analysis-status={analysisContractStatus}
      data-cache-status={cacheContractStatus}
      data-timeseries-count={timeseries.length}
      data-slow-query-count={slowRows.length}
      data-cache-health={cacheHealth}
      data-cache-hit-rate={cacheHitRatio?.toFixed(4) ?? ''}
      data-has-error={String(hasAnyError)}
      data-all-collectors-degraded={String(allCollectorsDegraded)}
      data-no-local-performance-metric-fallback="true"
      data-no-mock-timeseries-injection="true"
      data-no-mock-slow-query-injection="true"
      data-no-mock-cache-hot-key-injection="true"
      data-no-automatic-cache-ttl-mutation="true"
    >
      <PageHeader
        title="性能监控"
        subtitle="真实性能接口、慢查询、N+1、缓存统计与降级状态"
        actions={(
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => {
            void refetchCurrent()
            void refetchTimeseries()
            void refetchSlowQuery()
            void refetchAnalysis()
            void refetchCache()
          }}>
            刷新
          </Button>
        )}
      />

      {hasAnyError && (
        <Box
          data-testid="performance-monitoring-error-contract"
          data-contract-source={PERFORMANCE_READY_ENDPOINTS}
          data-unsupported-endpoints={PERFORMANCE_UNSUPPORTED_ENDPOINTS}
          data-current-error={String(currentError)}
          data-timeseries-error={String(tsError)}
          data-slow-query-error={String(slowError)}
          data-analysis-error={String(analysisError)}
          data-cache-error={String(cacheError)}
          data-no-local-performance-metric-fallback="true"
        >
          <ErrorAlert
            title="性能监控加载失败"
            message={headlineError instanceof Error ? headlineError.message : '性能监控接口异常'}
            onRetry={() => {
              void refetchCurrent()
              void refetchTimeseries()
              void refetchSlowQuery()
              void refetchAnalysis()
              void refetchCache()
            }}
          />
        </Box>
      )}

      <Alert
        severity="warning"
        data-testid="performance-contract-downgrade"
        data-downgrade-tone="collector-gap"
        data-contract-scope="system-performance"
        data-contract-endpoint="/system/performance"
        data-ready-endpoints={PERFORMANCE_READY_ENDPOINTS}
        data-unsupported-endpoints={PERFORMANCE_UNSUPPORTED_ENDPOINTS}
        data-no-local-performance-metric-fallback="true"
      >
        {PERFORMANCE_COLLECTOR_DOWNGRADE_MESSAGE}
      </Alert>

      <PerformanceContractCards
        items={[
          { name: '当前指标', endpoint: PERFORMANCE_ENDPOINTS.current, status: currentContractStatus, detail: 'CPU、内存、请求总数和健康状态', sampleCount: Object.keys(currentData).length },
          { name: '响应时序', endpoint: PERFORMANCE_ENDPOINTS.timeseries, status: timeseriesContractStatus, detail: '24 小时 P50/P95/P99 响应时间', sampleCount: timeseries.length },
          { name: '慢查询', endpoint: PERFORMANCE_ENDPOINTS.slowQuery, status: slowQueryContractStatus, detail: '慢接口 TOP10，只展示后端真实采集', sampleCount: slowRows.length },
          { name: '分析摘要', endpoint: PERFORMANCE_ENDPOINTS.analysis, status: analysisContractStatus, detail: '系统性能分析和采集状态', sampleCount: Object.keys(analysisData).length },
          { name: 'Redis 缓存', endpoint: PERFORMANCE_ENDPOINTS.cache, status: cacheContractStatus, detail: '命中率、命中/未命中计数与热 key', sampleCount: Array.isArray(cacheData.hotKeys) ? cacheData.hotKeys.length : 0 },
        ]}
      />

      {showCacheHitRateAdvice && (
        <Alert
          severity={cacheHealth === 'critical' ? 'error' : 'warning'}
          data-testid="performance-cache-hit-rate-advice"
          data-cache-health={cacheHealth}
          data-cache-hit-rate={cacheHitRatio?.toFixed(4)}
          data-contract-endpoint={PERFORMANCE_ENDPOINTS.cache}
          data-unsupported-endpoints="/system/performance/cache/ttl/update|/system/performance/cache/flush|/system/performance/cache/hot-keys/mock"
          data-no-automatic-cache-ttl-mutation="true"
          data-no-mock-cache-hot-key-injection="true"
        >
          Redis 缓存命中率偏低（{formatPercent(cacheHitRatio)}）：建议优先检查热点 key 是否稳定、核心查询 TTL 是否过短、是否存在缓存穿透，以及高频查询是否复用同一缓存 key。页面只基于真实缓存统计给出诊断，不自动改写 TTL 或伪造热 key。
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}><MetricCard label="CPU 使用率" value={`${Number(currentData.cpu ?? currentData.cpuUsage ?? 0)}%`} /></Grid>
        <Grid item xs={12} sm={6} md={3}><MetricCard label="内存使用率" value={`${Number(currentData.memory ?? currentData.memoryUsage ?? 0)}%`} /></Grid>
        <Grid item xs={12} sm={6} md={3}><MetricCard label="请求数" value={statValue(currentData.requestCount ?? currentData.totalRequests)} /></Grid>
        <Grid item xs={12} sm={6} md={3}><MetricCard label="缓存命中率" value={formatPercent(cacheHitRate)} hint="兼容 0.22 比例值和 22.0 百分值" /></Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} md={8}>
          <Card
            variant="outlined"
            data-testid="performance-timeseries-panel"
            data-contract-status={timeseriesContractStatus}
            data-contract-endpoint={PERFORMANCE_ENDPOINTS.timeseries}
            data-sample-count={timeseries.length}
            data-no-mock-timeseries-injection="true"
          >
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle2" fontWeight={600}>API 响应时间趋势（24h）</Typography>
                {tsLoading && <CircularProgress size={16} />}
              </Stack>
              {timeseries.length > 0
                ? <ReactECharts option={timeseriesOption} style={{ height: 280 }} />
                : (
                  <Box
                    data-testid="performance-timeseries-empty"
                    data-contract-endpoint={PERFORMANCE_ENDPOINTS.timeseries}
                    data-no-mock-timeseries-injection="true"
                  >
                    <EmptyState title="暂无 API 时序数据" description="时序采集或分析任务未接入时，这里保持真实空态。" />
                  </Box>
                )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Stack spacing={2}>
            <MetricCard label="性能分析状态" value={analysisData.status ?? '--'} hint={analysisData.message} />
            <MetricCard label="慢查询数量" value={statValue((slowQuery as { total?: number } | undefined)?.total ?? slowRows.length)} />
            <MetricCard label="健康状态" value={String(currentData.status ?? currentData.overallStatus ?? '--')} />
          </Stack>
        </Grid>
      </Grid>

      <Card
        variant="outlined"
        data-testid="performance-slow-query-panel"
        data-contract-status={slowQueryContractStatus}
        data-contract-endpoint={PERFORMANCE_ENDPOINTS.slowQuery}
        data-sample-count={slowRows.length}
        data-no-mock-slow-query-injection="true"
      >
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="subtitle2" fontWeight={600}>慢查询 TOP10</Typography>
            {slowLoading && <CircularProgress size={16} />}
          </Stack>
          <Box sx={{ height: 320 }}>
            <StandardDataGrid
              rows={slowRows}
              columns={slowColumns}
              getRowId={(row) => row.path ?? `${row.avgMs ?? 0}-${row.callCount ?? 0}`}
              slots={{ noRowsOverlay: DataGridEmptyOverlay }}
              toolbar={null}
              hideFooter
              disableRowSelectionOnClick
            />
          </Box>
          {!slowList && (
            <Box
              data-testid="performance-slow-query-empty"
              data-contract-endpoint={PERFORMANCE_ENDPOINTS.slowQuery}
              data-no-mock-slow-query-injection="true"
            >
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                暂无慢查询数据。若数据库慢查询采集未开启，这里会保持空态。
              </Typography>
            </Box>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Card
            variant="outlined"
            data-testid="performance-cache-panel"
            data-contract-status={cacheContractStatus}
            data-contract-endpoint={PERFORMANCE_ENDPOINTS.cache}
            data-cache-health={cacheHealth}
            data-cache-hit-rate={cacheHitRatio?.toFixed(4)}
            data-no-mock-cache-hot-key-injection="true"
            data-no-automatic-cache-ttl-mutation="true"
            data-unsupported-endpoints="/system/performance/cache/ttl/update|/system/performance/cache/flush|/system/performance/cache/hot-keys/mock"
          >
            <CardContent>
              <Typography variant="subtitle2" fontWeight={600} gutterBottom>缓存统计</Typography>
              {cacheLoading && <CircularProgress size={16} />}
              <Stack spacing={1}>
                <Typography variant="body2">命中率: {formatPercent(cacheHitRate)}</Typography>
                <Typography variant="body2">命中数: {statValue(cacheData.cacheHits ?? cacheData.hitCount)}</Typography>
                <Typography variant="body2">未命中数: {statValue(cacheData.cacheMisses ?? cacheData.missCount)}</Typography>
                <Typography variant="body2">热 key 数: {Array.isArray(cacheData.hotKeys) ? cacheData.hotKeys.length : 0}</Typography>
                {cacheHitRate == null && (
                  <Typography variant="caption" color="text.secondary">
                    后端当前未返回命中率字段，已按命中/未命中计数尝试计算。
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Card
            variant="outlined"
            data-testid="performance-analysis-panel"
            data-contract-status={analysisContractStatus}
            data-contract-endpoint={PERFORMANCE_ENDPOINTS.analysis}
          >
            <CardContent>
              <Typography variant="subtitle2" fontWeight={600} gutterBottom>分析摘要</Typography>
              <Stack spacing={1}>
                <Typography variant="body2">状态: {analysisData.status ?? '--'}</Typography>
                <Typography variant="body2">消息: {analysisData.message ?? '无'}</Typography>
                <Typography variant="body2">来源: system/performance/query/analysis</Typography>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
