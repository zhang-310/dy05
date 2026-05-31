/**
 * MetricsDashboard (W-10)
 * 实时指标监控仪表板
 */

import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  CircularProgress,
  Button,
  Chip,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material'
import { useTheme, type Theme } from '@mui/material/styles'
import { RefreshOutlined as RefreshIcon } from '@mui/icons-material'
import { useMonitoringData } from '@/hooks/useMonitoringData'
import { ErrorAlert, PageHeader } from '@/components/base'
import { ComponentStatus } from '@/types/monitoring'
import type { PerformanceTrendData } from '@/types/monitoring'

interface MetricStatCardProps {
  title: string
  value: string | number
  unit?: string
  trend?: number
  color?: string
  metricKey: string
  source: string
}

const MONITORING_READY_ENDPOINTS = [
  '/monitoring/metrics/realtime',
  '/monitoring/alerts/active',
  '/monitoring/health/status',
  '/monitoring/metrics/trend',
  '/monitoring/stream/realtime',
].join('|')

const MONITORING_UNSUPPORTED_ACTIONS = [
  'acknowledge-alert',
  'resolve-alert',
  'edit-alert-rule',
  'local-metric-fallback',
  'local-alert-fallback',
  'automatic-cache-ttl-mutation',
].join('|')

const REQUEST_ERROR_ENDPOINTS: Record<string, string> = {
  实时指标加载: '/monitoring/metrics/realtime',
  活跃告警加载: '/monitoring/alerts/active',
  健康状态加载: '/monitoring/health/status',
  实时推送连接: '/monitoring/stream/realtime',
}

function getRequestErrorEndpoint(label: string) {
  if (label.includes('趋势加载')) return '/monitoring/metrics/trend'
  return REQUEST_ERROR_ENDPOINTS[label] ?? 'unknown'
}

function hasFiniteMetric(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function formatMetricNumber(value: unknown, fractionDigits: number, unit = '') {
  if (!hasFiniteMetric(value)) return '未返回'
  return `${value.toFixed(fractionDigits)}${unit}`
}

function formatStatValue(value: unknown, fractionDigits: number) {
  if (!hasFiniteMetric(value)) return '未返回'
  return value.toFixed(fractionDigits)
}

function cacheHitDiagnostic(value: unknown): { label: string; color: 'success' | 'warning' | 'error' | 'default' } {
  if (!hasFiniteMetric(value)) return { label: '未返回', color: 'default' }
  if (value < 30) return { label: '严重偏低', color: 'error' }
  if (value < 60) return { label: '偏低', color: 'warning' }
  return { label: '健康', color: 'success' }
}

function MetricStatCard({ title, value, unit = '', trend, color, metricKey, source }: MetricStatCardProps) {
  const theme = useTheme()
  const isPositive = hasFiniteMetric(trend) && trend > 0
  const resolvedColor = color ?? (theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main)
  const valueState = value === '未返回' ? 'missing' : 'ready'

  return (
    <Card
      data-testid="metrics-dashboard-stat-card-surface"
      data-metric-key={metricKey}
      data-contract-source={source}
      data-value-state={valueState}
      data-no-local-metric-fallback="true"
      variant="outlined"
      sx={{ height: '100%' }}
    >
      <CardContent sx={{ py: 2 }}>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {title}
        </Typography>
        <Typography data-testid="metrics-dashboard-stat-value-surface" variant="h5" sx={{ color: resolvedColor }}>
          {value}{unit}
        </Typography>
        {trend !== undefined && (
          <Typography variant="caption" sx={{ color: isPositive ? 'error.main' : 'success.main' }}>
            {isPositive ? '↑' : '↓'} {Math.abs(trend).toFixed(2)}%
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}

function formatDateTime(value?: string | null) {
  if (!value) return '尚未刷新'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function getHealthColor(status?: string) {
  if (status === ComponentStatus.UP) return 'success'
  if (status === ComponentStatus.DEGRADED || status === ComponentStatus.UNKNOWN) return 'warning'
  if (status === ComponentStatus.DOWN) return 'error'
  return 'default'
}

function getTrendSummary(trends: Map<string, PerformanceTrendData>, metricName: string) {
  const trend = trends.get(metricName)
  if (!trend || trend.dataPoints.length === 0) return null
  const latest = trend.dataPoints[trend.dataPoints.length - 1]
  const average = trend.summary?.average ?? (trend.dataPoints.reduce((sum, point) => sum + point.value, 0) / trend.dataPoints.length)
  const max = trend.summary?.max ?? Math.max(...trend.dataPoints.map(point => point.value))
  return { latest, average, max, unit: trend.unit ?? '' }
}

type MetricTone = 'primary' | 'success' | 'warning' | 'error'

function metricToneColor(theme: Theme, tone: MetricTone) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
}

function usageTone(value: number, warningThreshold: number, errorThreshold: number): MetricTone {
  if (value > errorThreshold) return 'error'
  if (value > warningThreshold) return 'warning'
  return 'success'
}

export function MetricsDashboard() {
  const theme = useTheme()
  const [timeRange, setTimeRange] = useState<'hour' | 'day' | 'week'>('hour')
  const {
    realtimeMetrics,
    activeAlerts,
    healthStatus,
    isLoading,
    error,
    requestErrors,
    streamStatus,
    lastUpdatedAt,
    alertsCount,
    performanceTrends,
    refreshAll,
    fetchPerformanceTrend,
  } = useMonitoringData({
    pollingIntervalMs: 5000,
    enableAutoUpdate: true,
  })

  const componentRows = useMemo(
    () => Object.entries(healthStatus?.components ?? {}),
    [healthStatus?.components],
  )
  const criticalAlerts = (alertsCount?.critical ?? 0) + (alertsCount?.high ?? 0)
  const activeAlertTotal = activeAlerts?.length ?? 0
  const responseTrend = getTrendSummary(performanceTrends, 'responseTime')
  const errorTrend = getTrendSummary(performanceTrends, 'errorRate')
  const requestErrorEntries = Object.entries(requestErrors ?? {})
  const cacheHitRate = realtimeMetrics?.cacheHitRate
  const cacheHitState = cacheHitDiagnostic(cacheHitRate)
  const streamLabel = streamStatus === 'connected'
    ? 'SSE 已连接'
    : streamStatus === 'connecting'
      ? 'SSE 连接中'
      : streamStatus === 'closed'
        ? 'SSE 已关闭，轮询兜底'
        : 'SSE 未启用，轮询兜底'
  const streamSeverity = streamStatus === 'connected' ? 'success' : streamStatus === 'connecting' ? 'default' : 'warning'
  const systemHealthColor = healthStatus?.status === ComponentStatus.UP
    ? metricToneColor(theme, 'success')
    : healthStatus?.status === ComponentStatus.DOWN
      ? metricToneColor(theme, 'error')
      : metricToneColor(theme, 'warning')
  const activeAlertColor = metricToneColor(theme, activeAlertTotal > 0 ? 'warning' : 'success')
  const criticalAlertColor = metricToneColor(theme, criticalAlerts > 0 ? 'error' : 'success')
  const cpuColor = metricToneColor(theme, usageTone(realtimeMetrics?.cpuUsage || 0, 50, 80))
  const memoryColor = metricToneColor(theme, usageTone(realtimeMetrics?.memoryUsage || 0, 50, 80))
  const errorRateColor = metricToneColor(theme, usageTone(realtimeMetrics?.errorRate || 0, 1, 5))

  useEffect(() => {
    void fetchPerformanceTrend('responseTime', timeRange)
    void fetchPerformanceTrend('errorRate', timeRange)
  }, [fetchPerformanceTrend, timeRange])

  const handleTimeRangeChange = (_: unknown, value: 'hour' | 'day' | 'week' | null) => {
    if (!value) return
    setTimeRange(value)
  }

  return (
    <Box
      data-testid="metrics-dashboard-workbench"
      data-contract-scope="platform-monitoring-realtime-observability"
      data-ready-endpoints={MONITORING_READY_ENDPOINTS}
      data-unsupported-actions={MONITORING_UNSUPPORTED_ACTIONS}
      data-time-range={timeRange}
      data-stream-status={streamStatus}
      data-loading={String(isLoading)}
      data-request-error-count={requestErrorEntries.length}
      data-active-alert-count={activeAlertTotal}
      data-critical-alert-count={criticalAlerts}
      data-component-count={componentRows.length}
      data-trend-metrics={Array.from(performanceTrends.keys()).join('|')}
      data-last-updated-at={lastUpdatedAt ?? ''}
      data-no-local-metric-fallback="true"
      data-no-local-alert-fallback="true"
      data-no-alert-action-buttons="true"
      data-no-automatic-cache-ttl-mutation="true"
      data-polling-fallback="true"
      sx={{ p: 3 }}
    >
      <PageHeader
        title="实时指标监控"
        subtitle="读取 `/monitoring/metrics/realtime`、`/monitoring/alerts/active` 与 `/monitoring/health/status`；SSE 可用时实时推送，轮询作为兜底。"
        actions={(
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'stretch', sm: 'center' }}>
          <ToggleButtonGroup
            data-testid="metrics-dashboard-range-controls"
            data-contract-source="/monitoring/metrics/trend"
            data-current-range={timeRange}
            value={timeRange}
            exclusive
            onChange={handleTimeRangeChange}
            size="small"
          >
            <ToggleButton data-testid="metrics-dashboard-range-hour" data-trend-range="hour" value="hour">1小时</ToggleButton>
            <ToggleButton data-testid="metrics-dashboard-range-day" data-trend-range="day" value="day">1天</ToggleButton>
            <ToggleButton data-testid="metrics-dashboard-range-week" data-trend-range="week" value="week">1周</ToggleButton>
          </ToggleButtonGroup>
          <Button
            data-testid="metrics-dashboard-refresh-button"
            data-contract-source="/monitoring/metrics/realtime|/monitoring/alerts/active|/monitoring/health/status"
            data-disabled-reason={isLoading ? 'loading' : ''}
            variant="outlined"
            size="small"
            startIcon={isLoading ? <CircularProgress size={20} /> : <RefreshIcon />}
            onClick={refreshAll}
            disabled={isLoading}
          >
            刷新
          </Button>
          </Stack>
        )}
      />

      {error && (
        <Box data-testid="metrics-dashboard-global-error" data-error-source="monitoring-hook-request-errors" sx={{ mb: 2 }}>
          <ErrorAlert
            title="监控数据加载失败"
            message={`${error}。请检查 realtime metrics、active alerts、health status 接口、登录态和后端监控表写入。`}
            onRetry={() => void refreshAll()}
          />
        </Box>
      )}

      <Alert
        data-testid="metrics-dashboard-time-range-summary"
        data-contract-source="/monitoring/metrics/trend"
        data-time-range={timeRange}
        severity="info"
        sx={{ mb: 2 }}
      >
        当前选择 {timeRange === 'hour' ? '1小时' : timeRange === 'day' ? '1天' : '1周'} 视图；趋势摘要来自 `/monitoring/metrics/trend`，核心卡片展示实时快照。最后刷新: {formatDateTime(lastUpdatedAt)}
      </Alert>

      <Card
        data-testid="metrics-dashboard-chain-diagnostics"
        data-contract-source={MONITORING_READY_ENDPOINTS}
        data-stream-status={streamStatus}
        data-request-error-count={requestErrorEntries.length}
        variant="outlined"
        sx={{ mb: 3 }}
      >
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" spacing={2}>
            <Box>
              <Typography variant="subtitle2" fontWeight={700}>监控链路诊断</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                REST 快照、趋势接口和 SSE 推送分别展示状态；SSE 关闭时页面继续使用 5 秒轮询。
              </Typography>
            </Box>
            <Stack direction="row" flexWrap="wrap" gap={1}>
              <Chip size="small" label="/monitoring/metrics/realtime" color={requestErrors?.['实时指标加载'] ? 'error' : 'success'} variant="outlined" />
              <Chip size="small" label="/monitoring/alerts/active" color={requestErrors?.['活跃告警加载'] ? 'error' : 'success'} variant="outlined" />
              <Chip size="small" label="/monitoring/health/status" color={requestErrors?.['健康状态加载'] ? 'error' : 'success'} variant="outlined" />
              <Chip size="small" label="/monitoring/metrics/trend" color={(requestErrors?.['responseTime趋势加载'] || requestErrors?.['errorRate趋势加载']) ? 'error' : 'success'} variant="outlined" />
              <Chip size="small" label={streamLabel} color={streamSeverity} variant="outlined" />
            </Stack>
          </Stack>
          {requestErrorEntries.length > 0 && (
            <Alert
              data-testid="metrics-dashboard-request-errors"
              data-no-local-metric-fallback="true"
              severity="warning"
              variant="outlined"
              sx={{ mt: 2 }}
            >
              {requestErrorEntries.map(([label, message]) => (
                <Typography
                  key={label}
                  data-testid="metrics-dashboard-request-error-row"
                  data-error-label={label}
                  data-contract-source={getRequestErrorEndpoint(label)}
                  variant="body2"
                >
                  {message}
                </Typography>
              ))}
            </Alert>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="系统健康"
            value={healthStatus?.status ?? 'UNKNOWN'}
            color={systemHealthColor}
            metricKey="healthStatus"
            source="/monitoring/health/status"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="活跃告警"
            value={activeAlertTotal}
            unit=" 条"
            color={activeAlertColor}
            metricKey="activeAlerts"
            source="/monitoring/alerts/active"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="高危/严重"
            value={criticalAlerts}
            unit=" 条"
            color={criticalAlertColor}
            metricKey="criticalAlerts"
            source="/monitoring/alerts/active"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="组件数"
            value={componentRows.length}
            unit=" 个"
            metricKey="componentCount"
            source="/monitoring/health/status"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="CPU 使用率"
            value={formatStatValue(realtimeMetrics?.cpuUsage, 1)}
            unit={hasFiniteMetric(realtimeMetrics?.cpuUsage) ? '%' : ''}
            color={cpuColor}
            metricKey="cpuUsage"
            source="/monitoring/metrics/realtime|/monitoring/stream/realtime"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="内存使用率"
            value={formatStatValue(realtimeMetrics?.memoryUsage, 1)}
            unit={hasFiniteMetric(realtimeMetrics?.memoryUsage) ? '%' : ''}
            color={memoryColor}
            metricKey="memoryUsage"
            source="/monitoring/metrics/realtime|/monitoring/stream/realtime"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="平均响应时间"
            value={formatStatValue(realtimeMetrics?.responseTime, 0)}
            unit={hasFiniteMetric(realtimeMetrics?.responseTime) ? 'ms' : ''}
            metricKey="responseTime"
            source="/monitoring/metrics/realtime|/monitoring/stream/realtime"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricStatCard
            title="错误率"
            value={formatStatValue(realtimeMetrics?.errorRate, 2)}
            unit={hasFiniteMetric(realtimeMetrics?.errorRate) ? '%' : ''}
            color={errorRateColor}
            metricKey="errorRate"
            source="/monitoring/metrics/realtime|/monitoring/stream/realtime"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} md={6}>
          <Card
            data-testid="metrics-dashboard-response-trend-card"
            data-contract-source="/monitoring/metrics/trend"
            data-metric-name="responseTime"
            data-has-trend={String(Boolean(responseTrend))}
            data-no-local-trend-fallback="true"
            variant="outlined"
            sx={{ height: '100%' }}
          >
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                响应时间趋势摘要
              </Typography>
              {responseTrend ? (
                <Stack spacing={1.25} sx={{ mt: 2 }}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">最新值</Typography>
                    <Typography variant="body2" fontWeight={700}>{responseTrend.latest.value.toFixed(0)}{responseTrend.unit || 'ms'}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">平均值</Typography>
                    <Typography variant="body2" fontWeight={700}>{responseTrend.average.toFixed(0)}{responseTrend.unit || 'ms'}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">峰值</Typography>
                    <Typography variant="body2" fontWeight={700}>{responseTrend.max.toFixed(0)}{responseTrend.unit || 'ms'}</Typography>
                  </Stack>
                </Stack>
              ) : (
                <Alert data-testid="metrics-dashboard-response-trend-empty" data-contract-source="/monitoring/metrics/trend" data-no-local-trend-fallback="true" severity="warning" sx={{ mt: 2 }}>趋势接口未返回响应时间数据，请确认 `/monitoring/metrics/trend` 已写入 responseTime 指标。</Alert>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card
            data-testid="metrics-dashboard-error-trend-card"
            data-contract-source="/monitoring/metrics/trend"
            data-metric-name="errorRate"
            data-has-trend={String(Boolean(errorTrend))}
            data-no-local-trend-fallback="true"
            variant="outlined"
            sx={{ height: '100%' }}
          >
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                错误率趋势摘要
              </Typography>
              {errorTrend ? (
                <Stack spacing={1.25} sx={{ mt: 2 }}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">最新值</Typography>
                    <Typography variant="body2" fontWeight={700}>{errorTrend.latest.value.toFixed(2)}{errorTrend.unit || '%'}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">平均值</Typography>
                    <Typography variant="body2" fontWeight={700}>{errorTrend.average.toFixed(2)}{errorTrend.unit || '%'}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">峰值</Typography>
                    <Typography variant="body2" fontWeight={700}>{errorTrend.max.toFixed(2)}{errorTrend.unit || '%'}</Typography>
                  </Stack>
                </Stack>
              ) : (
                <Alert data-testid="metrics-dashboard-error-trend-empty" data-contract-source="/monitoring/metrics/trend" data-no-local-trend-fallback="true" severity="warning" sx={{ mt: 2 }}>趋势接口未返回错误率数据，请确认 `/monitoring/metrics/trend` 已写入 errorRate 指标。</Alert>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card data-testid="metrics-dashboard-network-metrics-card" data-contract-source="/monitoring/metrics/realtime|/monitoring/stream/realtime" data-no-local-metric-fallback="true" variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                网络指标
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">入站流量</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatMetricNumber(realtimeMetrics?.networkIn, 1, ' Mbps')}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">出站流量</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatMetricNumber(realtimeMetrics?.networkOut, 1, ' Mbps')}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">请求/秒</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatMetricNumber(realtimeMetrics?.requestsPerSecond, 0, ' req/s')}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card
            data-testid="metrics-dashboard-cache-queue-card"
            data-contract-source="/monitoring/metrics/realtime|/monitoring/stream/realtime"
            data-cache-hit-state={cacheHitState.label}
            data-no-local-metric-fallback="true"
            data-no-automatic-cache-ttl-mutation="true"
            variant="outlined"
            sx={{ height: '100%' }}
          >
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                缓存和队列
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">缓存命中率</Typography>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Chip
                      data-testid="metrics-dashboard-cache-hit-diagnostic"
                      data-contract-source="/monitoring/metrics/realtime|/monitoring/stream/realtime"
                      data-cache-hit-rate={hasFiniteMetric(cacheHitRate) ? cacheHitRate.toFixed(1) : ''}
                      data-threshold-warning="60"
                      data-threshold-error="30"
                      data-no-automatic-cache-ttl-mutation="true"
                      size="small"
                      label={cacheHitState.label}
                      color={cacheHitState.color}
                      variant="outlined"
                    />
                    <Typography variant="body2" fontWeight={600}>
                      {formatMetricNumber(realtimeMetrics?.cacheHitRate, 1, ' %')}
                    </Typography>
                  </Stack>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">队列深度</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatMetricNumber(realtimeMetrics?.queueDepth, 0)}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">活跃连接</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatMetricNumber(realtimeMetrics?.activeConnections, 0)}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card
            data-testid="metrics-dashboard-components-health-card"
            data-contract-source="/monitoring/health/status"
            data-component-count={componentRows.length}
            data-no-local-component-fallback="true"
            variant="outlined"
            sx={{ height: '100%' }}
          >
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                组件健康
              </Typography>
              <Stack spacing={1.25} sx={{ mt: 2 }}>
                {componentRows.length > 0 ? componentRows.map(([name, detail]) => (
                  <Box
                    key={name}
                    data-testid="metrics-dashboard-component-row"
                    data-component-name={name}
                    data-component-status={detail?.status ?? 'UNKNOWN'}
                    sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, alignItems: 'center' }}
                  >
                    <Box sx={{ minWidth: 0 }}>
                      <Typography variant="body2" fontWeight={600}>{name}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {detail?.message || detail?.lastCheck || '暂无组件诊断信息'}
                      </Typography>
                    </Box>
                    <Chip size="small" label={detail?.status ?? 'UNKNOWN'} color={getHealthColor(detail?.status)} />
                  </Box>
                )) : (
                  <Alert data-testid="metrics-dashboard-components-empty" data-contract-source="/monitoring/health/status" data-no-local-component-fallback="true" severity="warning">健康接口未返回组件明细，请检查 `/monitoring/health/status` 的 components 字段。</Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card
            data-testid="metrics-dashboard-active-alerts-card"
            data-contract-source="/monitoring/alerts/active|/monitoring/stream/realtime"
            data-active-alert-count={activeAlerts.length}
            data-no-local-alert-fallback="true"
            data-no-alert-action-buttons="true"
            data-unsupported-actions="acknowledge-alert|resolve-alert|close-alert|batch-acknowledge"
            variant="outlined"
            sx={{ height: '100%' }}
          >
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                活跃告警
              </Typography>
              <Stack spacing={1.25} sx={{ mt: 2 }}>
                {activeAlerts.length > 0 ? activeAlerts.slice(0, 5).map((alert, index) => (
                  <Box
                    key={alert.alertId ?? `${alert.alertType}-${index}`}
                    data-testid="metrics-dashboard-active-alert-row"
                    data-alert-id={alert.alertId ?? ''}
                    data-alert-severity={alert.severity}
                    data-alert-status={alert.status}
                    sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}
                  >
                    <Box sx={{ minWidth: 0 }}>
                      <Typography variant="body2" fontWeight={600}>
                        {alert.ruleName || alert.alertType || '未命名告警'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {alert.message}
                      </Typography>
                    </Box>
                    <Chip size="small" label={alert.severity} color={alert.severity === 'critical' || alert.severity === 'high' ? 'error' : 'warning'} />
                  </Box>
                )) : (
                  <Alert data-testid="metrics-dashboard-active-alerts-empty" data-contract-source="/monitoring/alerts/active" data-no-local-alert-fallback="true" severity="success">当前没有活跃告警；如果生产环境确有异常，请确认告警规则和监控落库任务已启用。</Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}

export default MetricsDashboard
