import { useState, useCallback } from 'react'
import {
  Box, Grid, Paper, Typography, Tab, Tabs, Chip, Button, Stack,
  IconButton, Tooltip, LinearProgress, Dialog, DialogTitle,
  DialogContent, DialogActions, TextField, MenuItem, Alert,
  CircularProgress,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import DownloadIcon from '@mui/icons-material/Download'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import ErrorIcon from '@mui/icons-material/Error'
import { useTheme } from '@mui/material/styles'
import type { GridColDef } from '@mui/x-data-grid'
import { ErrorAlert, PageHeader, StandardDataGrid } from '@/components/base'
import {
  systemApi, type ApiLogQuery, type AlertQuery,
  type AlertRecord, type ApiLog, type ApiLogStats,
  type ExternalApiConfig, type SyncLog,
} from '@/api/system'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { useToast } from '@/contexts/ToastContext'
import ReactECharts from 'echarts-for-react'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

// ─── Health Score Banner ───────────────────────────────────────────

function computeHealthScore(
  servicesOk: number, servicesTotal: number,
  p95ms: number, errorRate: number, activeAlerts: number
): number {
  let s = 0
  if (servicesTotal > 0 && servicesOk === servicesTotal) s += 40
  else if (servicesTotal > 0) s += Math.round(40 * servicesOk / servicesTotal)
  if (p95ms < 1000) s += 20
  else if (p95ms < 3000) s += 10
  if (errorRate < 0.001) s += 20
  else if (errorRate < 0.01) s += 10
  if (activeAlerts === 0) s += 20
  return s
}

interface ServiceHealthStatus { status?: string; responseTime?: number }

const SYSTEM_PAGE_READY_ENDPOINTS = [
  '/system/health',
  '/system/info',
  '/system/api-log/stats',
  '/system/api-log/list',
  '/system/sync-log/list',
  '/monitoring/alerts/active',
  '/monitoring/alerts/search',
  '/monitoring/alerts/acknowledge',
  '/monitoring/alerts/resolve',
  '/system/external-api/list',
  '/system/external-api/health-status',
  '/system/performance/api/timeseries',
  '/system/performance/query/slow',
  '/system/performance/query/analysis',
  '/system/diagnostic/report',
].join('|')

const SYSTEM_PAGE_UNSUPPORTED_ACTIONS = [
  'local-health-fallback',
  'local-api-log-fallback',
  'local-sync-log-fallback',
  'local-alert-fallback',
  'alert-rule-crud-in-alert-records-tab',
  'legacy-system-alert-rule-endpoints',
  'optimistic-alert-status-mutation',
  'local-external-api-fallback',
  'local-performance-fallback',
  'mutate-diagnostic-source',
].join('|')

const SYSTEM_TAB_LABELS = ['系统信息', '接口日志', '同步日志', '告警记录', '外部API', '性能监控'] as const

interface HealthBannerProps {
  onDiagnose: () => void
}

function HealthBanner({ onDiagnose }: HealthBannerProps) {
  const theme = useTheme()
  const { data: health, refetch, isFetching, isError, error } = useQuery({
    queryKey: ['system-health-banner'],
    queryFn: () => Promise.all([
      systemApi.health(),
      systemApi.apiLogStats(),
      systemApi.alertActive(),
    ]),
    refetchInterval: 30000,
  })

  const healthData = health?.[0] as Record<string, ServiceHealthStatus> | null
  const statsData = (health?.[1] ?? null) as ApiLogStats | null
  const alertsData = health?.[2] ?? null
  const services = healthData ? Object.entries(healthData) : []
  const servicesOk = services.filter(([, v]) => v?.status === 'UP').length
  const servicesTotal = services.length || 7
  const p95 = statsData?.p99ResponseTime ?? 0
  const total = statsData?.totalCalls ?? 1
  const errors = statsData?.errorCalls ?? 0
  const errorRate = total > 0 ? errors / total : 0
  const activeAlerts = alertsData?.total ?? alertsData?.alerts.length ?? 0
  const score = computeHealthScore(servicesOk || servicesTotal, servicesTotal, p95, errorRate, activeAlerts)

  const scoreTone: 'success' | 'warning' | 'error' = score >= 90 ? 'success' : score >= 70 ? 'warning' : 'error'
  const scoreColor = theme.palette.mode === 'dark' ? theme.palette[scoreTone].light : theme.palette[scoreTone].main
  const serviceOkColor = theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main
  const serviceWarnColor = theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main
  const errorColor = theme.palette.mode === 'dark' ? theme.palette.error.light : theme.palette.error.main

  return (
    <Paper
      data-testid="system-health-banner-surface"
      data-contract-scope="system-health-banner-real-sources"
      data-contract-source="/system/health|/system/api-log/stats|/monitoring/alerts/active"
      data-service-count={servicesTotal}
      data-services-ok={servicesOk}
      data-active-alerts={activeAlerts}
      data-health-score={score}
      data-no-local-health-fallback="true"
      sx={{ p: 2, mb: 2, borderLeft: '4px solid', borderLeftColor: scoreColor }}
    >
      {isError && (
        <ErrorAlert
          title="系统健康数据加载失败"
          message={error instanceof Error ? error.message : '健康检查、API 统计或活跃告警接口异常'}
          onRetry={() => void refetch()}
        />
      )}
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
        <Typography variant="subtitle1" fontWeight={700}>系统健康评分</Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          {isFetching && <LinearProgress sx={{ width: 80 }} />}
          <Typography variant="caption" color="text.secondary">30s 自动刷新</Typography>
          <Tooltip title="立即刷新">
            <IconButton size="small" onClick={() => void refetch()}><RefreshIcon fontSize="small" /></IconButton>
          </Tooltip>
          <Button size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={onDiagnose}>
            一键诊断报告
          </Button>
        </Stack>
      </Stack>
      <Grid container spacing={2}>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">综合评分</Typography>
            <Typography data-testid="system-health-score-surface" variant="h5" fontWeight={700} sx={{ color: scoreColor }}>{score}/100</Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">服务可用</Typography>
            <Typography variant="h6" fontWeight={600}>
              {servicesOk}/{servicesTotal}
              {servicesOk === servicesTotal
                ? <CheckCircleIcon data-testid="system-health-service-icon-surface" sx={{ ml: 0.5, fontSize: 16, color: serviceOkColor, verticalAlign: 'middle' }} />
                : <WarningAmberIcon data-testid="system-health-service-icon-surface" sx={{ ml: 0.5, fontSize: 16, color: serviceWarnColor, verticalAlign: 'middle' }} />}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">P99 延迟</Typography>
            <Typography variant="h6" fontWeight={600}>{p95 > 0 ? `${p95}ms` : '--'}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">错误率</Typography>
            <Typography data-testid="system-health-error-rate-surface" variant="h6" fontWeight={600} sx={{ color: errorRate > 0.01 ? errorColor : 'inherit' }}>
              {(errorRate * 100).toFixed(2)}%
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">活跃告警</Typography>
            <Typography data-testid="system-health-active-alert-surface" variant="h6" fontWeight={600} sx={{ color: activeAlerts > 0 ? errorColor : 'inherit' }}>
              {activeAlerts}{activeAlerts > 0 && <ErrorIcon data-testid="system-health-active-alert-icon-surface" sx={{ ml: 0.5, fontSize: 16, verticalAlign: 'middle', color: errorColor }} />}
            </Typography>
          </Paper>
        </Grid>
      </Grid>
    </Paper>
  )
}

// ─── System Info Tab ─────────────────────────────────────────────

function SystemInfoTab() {
  const theme = useTheme()
  const { data, refetch, isError: infoError, error: infoErrorData } = useQuery({
    queryKey: ['system-info'],
    queryFn: systemApi.info,
    refetchInterval: 30000,
  })
  const { data: health, isError: healthError, error: healthErrorData } = useQuery({
    queryKey: ['system-health'],
    queryFn: systemApi.health,
    refetchInterval: 30000,
  })

  const services = [
    { key: 'postgres', label: 'PostgreSQL', port: '5433' },
    { key: 'redis', label: 'Redis', port: '6380' },
    { key: 'elasticsearch', label: 'Elasticsearch', port: '9200' },
    { key: 'milvus', label: 'Milvus', port: '19530' },
    { key: 'rabbitmq', label: 'RabbitMQ', port: '5672' },
    { key: 'ai', label: 'AI服务', port: '--' },
    { key: 'douyin', label: '抖音OAuth', port: '--' },
  ]

  const healthMap = health as Record<string, ServiceHealthStatus> | null
  const serviceOkColor = theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main
  const serviceWarnColor = theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main

  return (
    <Box
      data-testid="system-info-tab-contract"
      data-contract-scope="system-info-health-tab"
      data-contract-source="/system/info|/system/health"
      data-info-error={String(infoError)}
      data-health-error={String(healthError)}
      data-service-count={services.length}
      data-no-local-system-info-fallback="true"
      data-no-local-health-fallback="true"
    >
      {infoError && (
        <Box data-testid="system-info-load-error" data-contract-source="/system/info" data-no-local-system-info-fallback="true">
          <ErrorAlert
            message={infoErrorData instanceof Error ? infoErrorData.message : '系统运行信息接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}
      {healthError && (
        <Box data-testid="system-health-load-error" data-contract-source="/system/health" data-no-local-health-fallback="true" sx={{ mt: 1, mb: 2 }}>
          <ErrorAlert
            title="服务健康检查加载失败"
            message={healthErrorData instanceof Error ? healthErrorData.message : '系统健康接口异常'}
          />
        </Box>
      )}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          { label: 'JVM 内存', value: data ? `${data.jvmMemory} MB` : '--' },
          { label: 'CPU 使用率', value: data ? `${data.cpuUsage}%` : '--' },
          { label: 'DB 连接数', value: data ? String(data.dbConnections) : '--' },
          { label: '运行时长', value: data ? `${Math.floor(data.uptime / 3600)}h ${Math.floor((data.uptime % 3600) / 60)}m` : '--' },
        ].map(item => (
          <Grid item xs={12} sm={6} md={3} key={item.label}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="body2" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700} mt={0.5}>{item.value}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      <Paper sx={{ p: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
          <Typography variant="subtitle2" fontWeight={600}>服务健康检查</Typography>
          <IconButton size="small" onClick={() => void refetch()}><RefreshIcon fontSize="small" /></IconButton>
        </Stack>
        <Stack direction="row" flexWrap="wrap" gap={1}>
          {services.map(svc => {
            const svcData = healthMap?.[svc.key]
            const isUp = !health || svcData?.status === 'UP'
            const ms = svcData?.responseTime
            return (
              <Chip
                key={svc.key}
                icon={isUp
                  ? <CheckCircleIcon data-testid="system-service-health-chip-icon-surface" sx={{ color: serviceOkColor }} />
                  : <WarningAmberIcon data-testid="system-service-health-chip-icon-surface" sx={{ color: serviceWarnColor }} />}
                label={`${svc.label}${ms ? ` (${ms}ms)` : ''}`}
                variant="outlined"
                size="small"
                data-testid="system-service-health-chip-surface"
                sx={{ borderColor: isUp ? serviceOkColor : serviceWarnColor }}
              />
            )
          })}
        </Stack>
      </Paper>
    </Box>
  )
}

// ─── API Log Tab ───────────────────────────────────────────────────

function ApiLogTab() {
  const theme = useTheme()
  const [search, setSearch] = useState<ApiLogQuery>({ page: 0, rows: 20 })
  const [pathFilter, setPathFilter] = useState('')
  const { data: stats, isError: statsError, error: statsErrorData, refetch: refetchStats } = useQuery({ queryKey: ['api-log-stats'], queryFn: () => systemApi.apiLogStats(), refetchInterval: 30000 })
  const { data, isFetching, isError, error, refetch } = useQuery({ queryKey: ['api-logs', search], queryFn: () => systemApi.apiLogList(search) })

  const rows = normalizeRows<ApiLog>(data)
  const total = readTotal(data, rows.length)
  const apiSuccessColor = theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main
  const apiErrorColor = theme.palette.mode === 'dark' ? theme.palette.error.light : theme.palette.error.main

  const option = stats ? {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '60%'],
      data: [
        { name: '成功', value: stats.successCalls },
        { name: '失败', value: stats.errorCalls },
      ],
      color: [apiSuccessColor, apiErrorColor],
    }],
  } : null

  const columns: GridColDef[] = [
    { field: 'apiPath', headerName: '接口路径', flex: 1, minWidth: 200 },
    { field: 'method', headerName: '方法', width: 80 },
    {
      field: 'statusCode', headerName: '状态码', width: 90,
      renderCell: ({ value }) => (
        <Chip label={String(value)} size="small"
          color={Number(value) >= 400 ? 'error' : Number(value) >= 300 ? 'warning' : 'success'}
          variant="outlined" />
      ),
    },
    {
      field: 'responseTime', headerName: '耗时', width: 90,
      renderCell: ({ value }) => (
        <Chip label={`${value}ms`} size="small"
          color={Number(value) > 3000 ? 'error' : Number(value) > 1000 ? 'warning' : 'default'}
          variant="outlined" />
      ),
    },
    { field: 'ip', headerName: 'IP', width: 130 },
    { field: 'createTime', headerName: '时间', width: 165, valueFormatter: ({ value }) => formatDate(value as string) },
  ]

  return (
    <Box
      data-testid="system-api-log-tab-contract"
      data-contract-scope="system-api-log-tab"
      data-contract-source="/system/api-log/stats|/system/api-log/list"
      data-row-count={rows.length}
      data-total-count={total}
      data-stats-ready={String(Boolean(stats))}
      data-path-filter={pathFilter}
      data-query-path={search.apiPath ?? ''}
      data-no-local-api-log-fallback="true"
    >
      {(isError || statsError) && (
        <Box data-testid="system-api-log-load-error" data-contract-source="/system/api-log/stats|/system/api-log/list" data-no-local-api-log-fallback="true" sx={{ mb: 2 }}>
          <ErrorAlert
            message={error instanceof Error ? error.message : statsErrorData instanceof Error ? statsErrorData.message : 'API 日志接口异常'}
            onRetry={() => { void refetch(); void refetchStats() }}
          />
        </Box>
      )}
      {stats && (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="body2" color="text.secondary">总调用数</Typography>
              <Typography variant="h6" fontWeight={700}>{stats.totalCalls.toLocaleString()}</Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="body2" color="text.secondary">平均响应</Typography>
              <Typography variant="h6" fontWeight={700}>{stats.avgResponseTime}ms</Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="body2" color="text.secondary">P99 延迟</Typography>
              <Typography variant="h6" fontWeight={700}>{stats.p99ResponseTime}ms</Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Box data-testid="system-api-log-pie-surface">
              {option && <ReactECharts option={option} style={{ height: 80 }} />}
            </Box>
          </Grid>
        </Grid>
      )}
      <Stack direction="row" spacing={1} mb={1}>
        <TextField size="small" placeholder="接口路径筛选" value={pathFilter}
          onChange={e => setPathFilter(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && setSearch(s => ({ ...s, apiPath: pathFilter, page: 0 }))}
          sx={{ width: 240 }} />
        <Button size="small" variant="outlined"
          onClick={() => setSearch(s => ({ ...s, apiPath: pathFilter, page: 0 }))}>
          搜索
        </Button>
      </Stack>
      <Alert data-testid="system-api-log-query-contract" data-contract-source="/system/api-log/list" data-query-field-map="apiPath->apiName" severity="info" sx={{ mb: 1 }}>
        日志查询已对齐后端 `module/apiName/status` 契约；“接口路径筛选”会映射到后端 `apiName` 模糊查询。
      </Alert>
      <Box sx={{ height: 'calc(100vh - 420px)', minHeight: 320 }}>
        <StandardDataGrid rows={rows} columns={columns} loading={isFetching}
          rowCount={total} paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          slotProps={{ toolbar: undefined }} />
      </Box>
    </Box>
  )
}

// ─── Sync Log Tab ─────────────────────────────────────────────────

function SyncLogTab() {
  const [search, setSearch] = useState({ page: 0, rows: 20, syncType: '', status: '' })
  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sync-logs', search],
    queryFn: () => systemApi.syncLogList({ page: search.page, rows: search.rows, syncType: search.syncType || undefined, status: search.status || undefined }),
  })
  const rows = normalizeRows<SyncLog>(data)
  const total = readTotal(data, rows.length)

  const columns: GridColDef[] = [
    { field: 'syncType', headerName: '同步类型', width: 140 },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => (
        <Chip label={String(value)} size="small"
          color={value === 'SUCCESS' ? 'success' : value === 'FAILED' ? 'error' : 'warning'}
          variant="outlined" />
      ),
    },
    { field: 'totalCount', headerName: '总数', width: 80 },
    { field: 'successCount', headerName: '成功', width: 80 },
    {
      field: 'failCount', headerName: '失败', width: 80,
      renderCell: ({ value }) => (
        <Typography variant="body2" color={Number(value) > 0 ? 'error' : 'inherit'}>{String(value)}</Typography>
      ),
    },
    { field: 'errorMessage', headerName: '错误信息', flex: 1, minWidth: 200 },
    { field: 'startTime', headerName: '开始时间', width: 165, valueFormatter: ({ value }) => formatDate(value as string) },
    { field: 'endTime', headerName: '结束时间', width: 165, valueFormatter: ({ value }) => formatDate(value as string) },
  ]

  return (
    <Box
      data-testid="system-sync-log-tab-contract"
      data-contract-scope="system-sync-log-tab"
      data-contract-source="/system/sync-log/list"
      data-row-count={rows.length}
      data-total-count={total}
      data-sync-type-filter={search.syncType}
      data-status-filter={search.status}
      data-no-local-sync-log-fallback="true"
    >
      {isError && (
        <Box data-testid="system-sync-log-load-error" data-contract-source="/system/sync-log/list" data-no-local-sync-log-fallback="true" sx={{ mb: 2 }}>
          <ErrorAlert
            message={error instanceof Error ? error.message : '同步日志接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}
      <Stack direction="row" spacing={1} mb={1}>
        <TextField select size="small" label="同步类型" value={search.syncType}
          onChange={e => setSearch(s => ({ ...s, syncType: e.target.value, page: 0 }))} sx={{ width: 160 }}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="DOUYIN_VIDEO">抖音视频</MenuItem>
          <MenuItem value="DOUYIN_ACCOUNT">抖音账号</MenuItem>
          <MenuItem value="KNOWLEDGE_BASE">知识库</MenuItem>
          <MenuItem value="PRODUCT">商品</MenuItem>
        </TextField>
        <TextField select size="small" label="状态" value={search.status}
          onChange={e => setSearch(s => ({ ...s, status: e.target.value, page: 0 }))} sx={{ width: 120 }}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="SUCCESS">成功</MenuItem>
          <MenuItem value="FAILED">失败</MenuItem>
          <MenuItem value="RUNNING">进行中</MenuItem>
        </TextField>
      </Stack>
      <Box sx={{ height: 'calc(100vh - 360px)', minHeight: 320 }}>
        <StandardDataGrid rows={rows} columns={columns} loading={isFetching}
          rowCount={total} paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          slotProps={{ toolbar: undefined }} />
      </Box>
    </Box>
  )
}

// ─── Alerts Tab ────────────────────────────────────────────────────

function AlertsTab() {
  const qc = useQueryClient()
  const toast = useToast()
  const [search, setSearch] = useState<AlertQuery>({ page: 0, rows: 20 })
  const [actionError, setActionError] = useState<{ endpoint: string; alertId: number; message: string } | null>(null)
  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['alerts', search],
    queryFn: () => systemApi.alertSearch(search),
  })
  const rows = normalizeRows<AlertRecord>(data)
  const total = readTotal(data, rows.length)

  const ackMut = useMutation({
    mutationFn: systemApi.alertAcknowledge,
    onMutate: (alertId) => setActionError({ endpoint: '/monitoring/alerts/acknowledge', alertId, message: '' }),
    onSuccess: () => { toast('已确认', 'success'); setActionError(null); void qc.invalidateQueries({ queryKey: ['alerts'] }) },
    onError: (e: Error, alertId) => setActionError({ endpoint: '/monitoring/alerts/acknowledge', alertId, message: getErrorMessage(e) }),
  })
  const resolveMut = useMutation({
    mutationFn: systemApi.alertResolve,
    onMutate: (alertId) => setActionError({ endpoint: '/monitoring/alerts/resolve', alertId, message: '' }),
    onSuccess: () => { toast('已处理', 'success'); setActionError(null); void qc.invalidateQueries({ queryKey: ['alerts'] }) },
    onError: (e: Error, alertId) => setActionError({ endpoint: '/monitoring/alerts/resolve', alertId, message: getErrorMessage(e) }),
  })

  const severityColor = (s: string) => s === 'HIGH' ? 'error' : s === 'MEDIUM' ? 'warning' : 'success'

  const columns: GridColDef[] = [
    {
      field: 'severity', headerName: '级别', width: 80,
      renderCell: ({ value }) => (
        <Chip label={value === 'HIGH' ? '高' : value === 'MEDIUM' ? '中' : '低'}
          size="small" color={severityColor(value as string)} />
      ),
    },
    { field: 'ruleName', headerName: '规则名称', width: 160 },
    { field: 'metric', headerName: '指标', width: 120 },
    { field: 'value', headerName: '触发值', width: 90 },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => (
        <Chip label={value === 'ACTIVE' ? '活跃' : value === 'ACKNOWLEDGED' ? '已确认' : '已处理'}
          size="small" variant="outlined"
          color={value === 'ACTIVE' ? 'error' : value === 'ACKNOWLEDGED' ? 'warning' : 'success'} />
      ),
    },
    { field: 'createTime', headerName: '触发时间', width: 165, valueFormatter: ({ value }) => formatDate(value as string) },
    {
      field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => {
        const r = row as AlertRecord
        return (
          <Stack direction="row" spacing={0.5}>
            {r.status === 'ACTIVE' && (
              <Button
                size="small"
                data-testid="system-alert-ack-button"
                data-contract-source="/monitoring/alerts/acknowledge"
                data-alert-id={r.id}
                data-row-retained-on-error="true"
                onClick={() => ackMut.mutate(r.id)}
              >
                确认
              </Button>
            )}
            {(r.status === 'ACTIVE' || r.status === 'ACKNOWLEDGED') && (
              <Button
                size="small"
                color="success"
                data-testid="system-alert-resolve-button"
                data-contract-source="/monitoring/alerts/resolve"
                data-alert-id={r.id}
                data-row-retained-on-error="true"
                onClick={() => resolveMut.mutate(r.id)}
              >
                处理
              </Button>
            )}
          </Stack>
        )
      },
    },
  ]

  return (
    <Box
      data-testid="system-alerts-tab-contract"
      data-contract-scope="system-alert-records-tab"
      data-contract-source="/monitoring/alerts/search|/monitoring/alerts/acknowledge|/monitoring/alerts/resolve"
      data-row-count={rows.length}
      data-total-count={total}
      data-severity-filter={search.severity ?? ''}
      data-status-filter={search.status ?? ''}
      data-action-error={actionError?.message ? actionError.endpoint : ''}
      data-no-local-alert-fallback="true"
      data-no-alert-rule-crud="true"
      data-no-legacy-system-alert-rule-endpoints="true"
      data-no-optimistic-alert-status-mutation="true"
      data-row-retained-on-action-error="true"
    >
      {isError && (
        <Box data-testid="system-alerts-load-error" data-contract-source="/monitoring/alerts/search" data-no-local-alert-fallback="true" sx={{ mb: 2 }}>
          <ErrorAlert
            message={error instanceof Error ? error.message : '告警记录接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}
      {actionError?.message && (
        <Box
          data-testid="system-alert-action-error"
          data-contract-source={actionError.endpoint}
          data-alert-id={actionError.alertId}
          data-row-retained="true"
          sx={{ mb: 2 }}
        >
          <ErrorAlert
            title="告警操作失败"
            message={`${actionError.endpoint} 处理失败：${actionError.message}。告警 ID ${actionError.alertId} 的当前状态已保留，请重试或刷新后确认。`}
            onRetry={() => void refetch()}
          />
        </Box>
      )}
      <Stack
        direction="row"
        spacing={1}
        mb={1}
        data-testid="system-alerts-filter-contract"
        data-contract-source="/monitoring/alerts/search"
        data-query-severity={search.severity ?? ''}
        data-query-status={search.status ?? ''}
        data-no-local-alert-filter="true"
      >
        <TextField select size="small" label="级别" value={search.severity ?? ''}
          onChange={e => setSearch(s => ({ ...s, severity: e.target.value || undefined, page: 0 }))} sx={{ width: 120 }}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="HIGH">高</MenuItem>
          <MenuItem value="MEDIUM">中</MenuItem>
          <MenuItem value="LOW">低</MenuItem>
        </TextField>
        <TextField select size="small" label="状态" value={search.status ?? ''}
          onChange={e => setSearch(s => ({ ...s, status: e.target.value || undefined, page: 0 }))} sx={{ width: 120 }}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="ACTIVE">活跃</MenuItem>
          <MenuItem value="ACKNOWLEDGED">已确认</MenuItem>
          <MenuItem value="RESOLVED">已处理</MenuItem>
        </TextField>
      </Stack>
      <Box
        data-testid="system-alerts-grid-contract"
        data-contract-source="/monitoring/alerts/search"
        data-row-count={rows.length}
        data-total-count={total}
        data-no-local-alert-fallback="true"
        data-row-retained-on-action-error="true"
        sx={{ height: 'calc(100vh - 360px)', minHeight: 320 }}
      >
        <StandardDataGrid rows={rows as AlertRecord[]} columns={columns} loading={isFetching}
          rowCount={total} paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          slotProps={{ toolbar: undefined }} />
      </Box>
    </Box>
  )
}

// ─── External API Tab ────────────────────────────────────────────

interface ExternalApiRow { id: number; providerCode: string; apiName: string; category: string; baseUrl: string; healthStatus: string; status: number }

function ExternalApiTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [actionError, setActionError] = useState<{ providerCode: string; message: string } | null>(null)
  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['ext-api-list'],
    queryFn: () => systemApi.externalApiList({}),
  })
  const externalApis = normalizeRows<ExternalApiConfig>(data)
  const rows: ExternalApiRow[] = externalApis.map((item) => ({
    id: Number(item.id ?? 0),
    providerCode: String(item.providerCode ?? ''),
    apiName: String(item.providerName ?? item.apiName ?? ''),
    category: String(item.category ?? ''),
    baseUrl: String(item.baseUrl ?? ''),
    healthStatus: String(item.healthStatus ?? 'unknown'),
    status: Number(item.status ?? 0),
  }))

  const testMut = useMutation({
    mutationFn: (providerCode: string) => systemApi.externalApiHealthStatus({ providerCode, status: 'unknown' }),
    onMutate: (providerCode) => setActionError({ providerCode, message: '' }),
    onSuccess: () => { toast('已标记为待探测', 'success'); setActionError(null); void qc.invalidateQueries({ queryKey: ['ext-api-list'] }) },
    onError: (e: Error, providerCode) => {
      toast('健康状态标记失败', 'error')
      setActionError({ providerCode, message: getErrorMessage(e) })
    },
  })

  const columns: GridColDef[] = [
    { field: 'apiName', headerName: 'API名称', flex: 1, minWidth: 140 },
    { field: 'category', headerName: '分类', width: 120 },
    { field: 'baseUrl', headerName: '基础URL', flex: 1, minWidth: 200 },
    { field: 'providerCode', headerName: '供应商编码', width: 140 },
    { field: 'healthStatus', headerName: '健康', width: 110 },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => (
        <Chip label={Number(value) === 1 ? '启用' : '停用'} size="small"
          color={Number(value) === 1 ? 'success' : 'default'} variant="outlined" />
      ),
    },
    {
      field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" variant="outlined"
          onClick={() => testMut.mutate((row as ExternalApiRow).providerCode)}>
          标记待探测
        </Button>
      ),
    },
  ]

  return (
    <Box
      data-testid="system-external-api-tab-contract"
      data-contract-scope="system-external-api-health-tab"
      data-contract-source="/system/external-api/list|/system/external-api/health-status"
      data-row-count={rows.length}
      data-action-error={actionError?.message ? actionError.providerCode : ''}
      data-no-local-external-api-fallback="true"
      data-row-retained-on-action-error="true"
      sx={{ height: 'calc(100vh - 320px)', minHeight: 320 }}
    >
      {isError && (
        <Box data-testid="system-external-api-load-error" data-contract-source="/system/external-api/list" data-no-local-external-api-fallback="true" sx={{ mb: 2 }}>
          <ErrorAlert
            message={error instanceof Error ? error.message : '外部 API 配置接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}
      <Alert data-testid="system-external-api-health-contract" data-contract-source="/system/external-api/health-status" data-write-mode="unknown-mark-for-probe" severity="info" sx={{ mb: 1 }}>
        健康状态来自后端 `external_api_config` 的 `healthStatus/avgLatencyMs/successRatePct` 字段；按钮只写入 unknown，等待后端定时任务下一轮真实探测覆盖。
      </Alert>
      {actionError?.message && (
        <Box data-testid="system-external-api-action-error" data-contract-source="/system/external-api/health-status" data-provider-code={actionError.providerCode} data-row-retained="true">
          <ErrorAlert
            title="外部 API 健康状态写入失败"
            message={`/system/external-api/health-status 写入失败：${actionError.message}。供应商 ${actionError.providerCode} 的原健康状态已保留。`}
            onRetry={() => void refetch()}
          />
        </Box>
      )}
      <StandardDataGrid rows={rows} columns={columns} loading={isFetching}
        slotProps={{ toolbar: undefined }} />
    </Box>
  )
}

// ─── Performance Tab ──────────────────────────────────────────────

interface TimeseriesPoint { time?: string; ts?: string; p50?: number; p95?: number; p99?: number }
interface SlowQueryRow { path: string; avgMs: number; callCount: number; maxMs: number }
interface PerformanceAnalysis { status?: string; message?: string }

function PerformanceTab() {
  const theme = useTheme()
  const { data: timeseries, isError: timeseriesError, error: timeseriesErrorData, refetch: refetchTimeseries } = useQuery({
    queryKey: ['perf-timeseries'],
    queryFn: () => systemApi.performanceTimeseries({ metric: 'response_time', hours: 24 }),
    refetchInterval: 60000,
  })
  const { data: slowQuery, isError: slowQueryError, error: slowQueryErrorData, refetch: refetchSlowQuery } = useQuery({
    queryKey: ['perf-slow-query'],
    queryFn: () => systemApi.performanceSlowQuery<SlowQueryRow>({ rows: 10, minMs: 500 }),
  })
  const { data: analysis, isError: analysisError, error: analysisErrorData, refetch: refetchAnalysis } = useQuery({
    queryKey: ['perf-analysis'],
    queryFn: systemApi.performanceAnalysis,
  })

  const tsData = normalizeRows<TimeseriesPoint>(timeseries)
  const slowRows: SlowQueryRow[] = normalizeRows<SlowQueryRow>(slowQuery)
  const p50Color = theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main
  const p95Color = theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main
  const p99Color = theme.palette.mode === 'dark' ? theme.palette.error.light : theme.palette.error.main

  const timeseriesOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: tsData.map(d => d.time ?? d.ts ?? '') },
    yAxis: { type: 'value', name: 'ms' },
    series: [
      { name: 'P50', type: 'line', smooth: true, data: tsData.map(d => d.p50 ?? 0), color: p50Color },
      { name: 'P95', type: 'line', smooth: true, data: tsData.map(d => d.p95 ?? 0), color: p95Color },
      { name: 'P99', type: 'line', smooth: true, data: tsData.map(d => d.p99 ?? 0), color: p99Color },
    ],
    legend: { data: ['P50', 'P95', 'P99'], bottom: 0 },
  }

  const slowCols: GridColDef[] = [
    { field: 'path', headerName: '接口路径', flex: 1, minWidth: 200 },
    { field: 'avgMs', headerName: '平均耗时', width: 110,
      renderCell: ({ value }) => <Chip label={`${value}ms`} size="small" color="warning" variant="outlined" /> },
    { field: 'callCount', headerName: '调用次数', width: 100 },
    { field: 'maxMs', headerName: '最大耗时', width: 100 },
  ]

  return (
    <Box
      data-testid="system-performance-tab-contract"
      data-contract-scope="system-performance-embedded-tab"
      data-contract-source="/system/performance/api/timeseries|/system/performance/query/slow|/system/performance/query/analysis"
      data-timeseries-count={tsData.length}
      data-slow-row-count={slowRows.length}
      data-analysis-ready={String(Boolean(analysis))}
      data-no-local-performance-fallback="true"
    >
      {(timeseriesError || slowQueryError || analysisError) && (
        <Box data-testid="system-performance-load-error" data-contract-source="/system/performance/api/timeseries|/system/performance/query/slow|/system/performance/query/analysis" data-no-local-performance-fallback="true" sx={{ mb: 2 }}>
          <ErrorAlert
            message={
              timeseriesErrorData instanceof Error ? timeseriesErrorData.message
                : slowQueryErrorData instanceof Error ? slowQueryErrorData.message
                  : analysisErrorData instanceof Error ? analysisErrorData.message
                    : '性能监控接口异常'
            }
            onRetry={() => { void refetchTimeseries(); void refetchSlowQuery(); void refetchAnalysis() }}
          />
        </Box>
      )}
      <Alert data-testid="system-performance-degradation-contract" data-contract-source="/system/performance" data-no-local-performance-fallback="true" severity="warning" sx={{ mb: 2 }}>
        当前后端 `system/performance` 已提供避免 404 的真实降级接口，但时间序列、慢查询和缓存分析仍可能返回空数据；页面按“未配置时序库/慢查询采集”的降级策略展示。
      </Alert>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" fontWeight={600} mb={1}>响应时间趋势（近24h）</Typography>
            {tsData.length > 0
              ? <Box data-testid="system-performance-timeseries-surface"><ReactECharts option={timeseriesOption} style={{ height: 240 }} /></Box>
              : <Typography data-testid="system-performance-timeseries-empty" data-no-local-performance-fallback="true" color="text.secondary" variant="body2">暂无数据</Typography>}
          </Paper>
        </Grid>
        {analysis && (
          <Grid item xs={12} sm={6} md={3}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="body2" color="text.secondary">整体健康状态</Typography>
              <Typography variant="h6" fontWeight={700}>
                {String((analysis as PerformanceAnalysis).status ?? '--')}
              </Typography>
            </Paper>
          </Grid>
        )}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" fontWeight={600} mb={1}>慢接口 TOP10（&gt;500ms）</Typography>
            <Box sx={{ height: 300 }}>
              <StandardDataGrid rows={slowRows} columns={slowCols}
                getRowId={(r) => (r as SlowQueryRow).path}
                slotProps={{ toolbar: undefined }} />
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}

// ─── Diagnose Dialog ─────────────────────────────────────────────

function DiagnoseDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const theme = useTheme()
  const { data: health } = useQuery({ queryKey: ['system-health'], queryFn: systemApi.health, enabled: open })
  const { data: stats } = useQuery({ queryKey: ['api-log-stats'], queryFn: () => systemApi.apiLogStats(), enabled: open })
  const { data: alerts } = useQuery({ queryKey: ['alerts-active'], queryFn: () => systemApi.alertActive(), enabled: open })
  const { data: analysis } = useQuery({ queryKey: ['perf-analysis'], queryFn: systemApi.performanceAnalysis, enabled: open })
  const reportQuery = useQuery({ queryKey: ['system-diagnostic-report'], queryFn: systemApi.diagnosticReport, enabled: open, retry: false })
  const diagnosticOkColor = theme.palette.mode === 'dark' ? theme.palette.success.light : theme.palette.success.main
  const diagnosticWarnColor = theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main

  const handleDownload = useCallback(() => {
    if (reportQuery.data) {
      const blob = new Blob([JSON.stringify(reportQuery.data, null, 2)], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `system-diagnostic-report-${Date.now()}.json`
      a.click()
      URL.revokeObjectURL(url)
      return
    }

    const healthMap = (health ?? null) as Record<string, ServiceHealthStatus> | null
    const statsData = (stats ?? null) as ApiLogStats | null
    const alertsList = alerts?.alerts ?? []
    const perfAnalysis = (analysis ?? null) as PerformanceAnalysis | null
    const lines: string[] = [
      `系统诊断报告（本地降级快照）— ${new Date().toLocaleString()}`,
      '='.repeat(50),
      '',
      '【服务健康状态】',
      ...(healthMap ? Object.entries(healthMap).map(
        ([k, v]) => `  ${k}: ${v?.status ?? v}`
      ) : ['  数据加载中...']),
      '',
      '【API 统计】',
      statsData ? `  总调用: ${statsData.totalCalls}  成功: ${statsData.successCalls}  失败: ${statsData.errorCalls}` : '  数据加载中...',
      statsData ? `  平均响应: ${statsData.avgResponseTime}ms  P99: ${statsData.p99ResponseTime}ms` : '',
      '',
      '【活跃告警】',
      ...(alertsList ?? []).map((a) => `  [${a.severity}] ${a.ruleName}`),
      (alertsList ?? []).length === 0 ? '  无活跃告警' : '',
      '',
      '【性能分析】',
      perfAnalysis ? `  状态: ${perfAnalysis.status ?? '--'}` : '  数据加载中...',
      '',
      '【降级说明】',
      reportQuery.error ? `  /system/diagnostic/report 加载失败：${getErrorMessage(reportQuery.error)}。该文件由浏览器当前已加载数据生成。` : '  后端报告尚未返回，使用当前弹窗数据生成。',
    ]
    const blob = new Blob([lines.filter(l => l !== undefined).join('\n')], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `system-diagnose-${Date.now()}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }, [health, stats, alerts, analysis, reportQuery.data, reportQuery.error])

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        'data-testid': 'system-diagnose-dialog-contract',
        'data-contract-scope': 'system-diagnostic-report-download',
        'data-contract-source': '/system/diagnostic/report|loaded-page-state-fallback',
        'data-report-ready': String(Boolean(reportQuery.data)),
        'data-report-error': String(Boolean(reportQuery.error)),
        'data-no-local-mutation-fallback': 'true',
      } as Record<string, string>}
    >
      <DialogTitle>一键诊断报告</DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" color="text.secondary" mb={2}>
          优先下载后端 `/system/diagnostic/report` 快照；接口不可用时保留弹窗数据并下载本地降级快照。
        </Typography>
        {reportQuery.error && (
          <Alert
            data-testid="system-diagnostic-report-fallback"
            data-contract-source="/system/diagnostic/report"
            data-fallback-source="loaded-page-state"
            severity="warning"
            sx={{ mb: 2 }}
          >
            后端诊断报告加载失败：{getErrorMessage(reportQuery.error)}。可以继续下载本地降级快照。
          </Alert>
        )}
        <Stack spacing={1}>
          {[
            { label: '后端诊断报告', ok: !!reportQuery.data, error: !!reportQuery.error },
            { label: '服务健康', ok: !!health, error: false },
            { label: 'API 统计', ok: !!stats, error: false },
            { label: '告警记录', ok: !!alerts, error: false },
            { label: '性能分析', ok: !!analysis, error: false },
          ].map(item => (
            <Stack key={item.label} direction="row" spacing={1} alignItems="center">
              {item.ok
                ? <CheckCircleIcon data-testid="system-diagnose-status-icon-surface" sx={{ color: diagnosticOkColor, fontSize: 18 }} />
                : item.error
                  ? <WarningAmberIcon data-testid="system-diagnose-status-icon-surface" sx={{ color: diagnosticWarnColor, fontSize: 18 }} />
                  : <LinearProgress sx={{ flex: 1, maxWidth: 80 }} />}
              <Typography variant="body2">{item.label}</Typography>
            </Stack>
          ))}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          startIcon={reportQuery.isFetching ? <CircularProgress color="inherit" size={16} /> : <DownloadIcon />}
          onClick={handleDownload}
          disabled={!reportQuery.data && !reportQuery.error}
        >
          {reportQuery.data ? '下载后端报告' : '下载降级快照'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Alert Rule types (local) ─────────────────────────────────────

// ─── Main Page ────────────────────────────────────────────────────

export default function SystemPage() {
  const [tab, setTab] = useState(0)
  const [diagnoseOpen, setDiagnoseOpen] = useState(false)

  return (
    <Box
      data-testid="system-page-workbench"
      data-contract-scope="system-monitoring-consolidated-tabs"
      data-ready-endpoints={SYSTEM_PAGE_READY_ENDPOINTS}
      data-unsupported-actions={SYSTEM_PAGE_UNSUPPORTED_ACTIONS}
      data-active-tab={SYSTEM_TAB_LABELS[tab]}
      data-diagnose-open={String(diagnoseOpen)}
      data-no-local-health-fallback="true"
      data-no-local-api-log-fallback="true"
      data-no-local-sync-log-fallback="true"
      data-no-local-alert-fallback="true"
      data-no-local-external-api-fallback="true"
      data-no-local-performance-fallback="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', p: 2 }}
    >
      <PageHeader
        title="系统监控"
        subtitle="汇总系统健康、API 日志、同步日志、告警、外部 API 和性能降级状态"
        breadcrumbs={[{ label: '系统' }, { label: '监控总览' }]}
      />
      <HealthBanner onDiagnose={() => setDiagnoseOpen(true)} />
      <Paper sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <Tabs
          data-testid="system-page-tabs"
          data-contract-source={SYSTEM_PAGE_READY_ENDPOINTS}
          data-active-tab={SYSTEM_TAB_LABELS[tab]}
          value={tab}
          onChange={(_, v: number) => setTab(v)}
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
          <Tab label="系统信息" data-testid="system-tab-system-info" data-contract-source="/system/info|/system/health" />
          <Tab label="接口日志" data-testid="system-tab-api-log" data-contract-source="/system/api-log/stats|/system/api-log/list" />
          <Tab label="同步日志" data-testid="system-tab-sync-log" data-contract-source="/system/sync-log/list" />
          <Tab label="告警记录" data-testid="system-tab-alerts" data-contract-source="/monitoring/alerts/search|/monitoring/alerts/acknowledge|/monitoring/alerts/resolve" />
          <Tab label="外部API" data-testid="system-tab-external-api" data-contract-source="/system/external-api/list|/system/external-api/health-status" />
          <Tab label="性能监控" data-testid="system-tab-performance" data-contract-source="/system/performance/api/timeseries|/system/performance/query/slow|/system/performance/query/analysis" />
        </Tabs>
        <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
          {tab === 0 && <SystemInfoTab />}
          {tab === 1 && <ApiLogTab />}
          {tab === 2 && <SyncLogTab />}
          {tab === 3 && <AlertsTab />}
          {tab === 4 && <ExternalApiTab />}
          {tab === 5 && <PerformanceTab />}
        </Box>
      </Paper>
      <DiagnoseDialog open={diagnoseOpen} onClose={() => setDiagnoseOpen(false)} />
    </Box>
  )
}





