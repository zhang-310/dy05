import { useState, useCallback } from 'react'
import {
  Box, Grid, Paper, Typography, Tab, Tabs, Chip, Button, Stack,
  IconButton, Tooltip, LinearProgress, Dialog, DialogTitle,
  DialogContent, DialogActions, TextField, MenuItem,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import DownloadIcon from '@mui/icons-material/Download'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import ErrorIcon from '@mui/icons-material/Error'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import {
  systemApi, type ApiLogQuery, type AlertQuery,
  type AlertRecord, type ApiLogStats,
} from '@/api/system'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { useToast } from '@/contexts/ToastContext'
import ReactECharts from 'echarts-for-react'

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

interface HealthBannerProps {
  onDiagnose: () => void
}

function HealthBanner({ onDiagnose }: HealthBannerProps) {
  const { data: health, refetch, isFetching } = useQuery({
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
  const alertsData = (health?.[2] ?? null) as AlertRecord[] | null
  const services = healthData ? Object.entries(healthData) : []
  const servicesOk = services.filter(([, v]) => v?.status === 'UP').length
  const servicesTotal = services.length || 7
  const p95 = statsData?.p99ResponseTime ?? 0
  const total = statsData?.totalCalls ?? 1
  const errors = statsData?.errorCalls ?? 0
  const errorRate = total > 0 ? errors / total : 0
  const activeAlerts = alertsData?.length ?? 0
  const score = computeHealthScore(servicesOk || servicesTotal, servicesTotal, p95, errorRate, activeAlerts)

  const scoreColor = score >= 90 ? '#4caf50' : score >= 70 ? '#ff9800' : '#f44336'

  return (
    <Paper sx={{ p: 2, mb: 2, borderLeft: `4px solid ${scoreColor}` }}>
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
            <Typography variant="h5" fontWeight={700} sx={{ color: scoreColor }}>{score}/100</Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">服务可用</Typography>
            <Typography variant="h6" fontWeight={600}>
              {servicesOk}/{servicesTotal}
              {servicesOk === servicesTotal
                ? <CheckCircleIcon sx={{ ml: 0.5, fontSize: 16, color: '#4caf50', verticalAlign: 'middle' }} />
                : <WarningAmberIcon sx={{ ml: 0.5, fontSize: 16, color: '#ff9800', verticalAlign: 'middle' }} />}
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
            <Typography variant="h6" fontWeight={600} sx={{ color: errorRate > 0.01 ? '#f44336' : 'inherit' }}>
              {(errorRate * 100).toFixed(2)}%
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm="auto">
          <Paper variant="outlined" sx={{ p: 1.5, minWidth: 100, textAlign: 'center' }}>
            <Typography variant="caption" color="text.secondary">活跃告警</Typography>
            <Typography variant="h6" fontWeight={600} sx={{ color: activeAlerts > 0 ? '#f44336' : 'inherit' }}>
              {activeAlerts}{activeAlerts > 0 && <ErrorIcon sx={{ ml: 0.5, fontSize: 16, verticalAlign: 'middle', color: '#f44336' }} />}
            </Typography>
          </Paper>
        </Grid>
      </Grid>
    </Paper>
  )
}

// ─── System Info Tab ─────────────────────────────────────────────

function SystemInfoTab() {
  const { data, refetch } = useQuery({
    queryKey: ['system-info'],
    queryFn: systemApi.info,
    refetchInterval: 30000,
  })
  const { data: health } = useQuery({
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

  return (
    <Box>
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
                  ? <CheckCircleIcon style={{ color: '#4caf50' }} />
                  : <WarningAmberIcon style={{ color: '#ff9800' }} />}
                label={`${svc.label}${ms ? ` (${ms}ms)` : ''}`}
                variant="outlined"
                size="small"
                sx={{ borderColor: isUp ? '#4caf50' : '#ff9800' }}
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
  const [search, setSearch] = useState<ApiLogQuery>({ page: 0, rows: 20 })
  const [pathFilter, setPathFilter] = useState('')
  const { data: stats } = useQuery({ queryKey: ['api-log-stats'], queryFn: systemApi.apiLogStats, refetchInterval: 30000 })
  const { data, isFetching } = useQuery({ queryKey: ['api-logs', search], queryFn: () => systemApi.apiLogList(search) })

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const option = stats ? {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '60%'],
      data: [
        { name: '成功', value: stats.successCalls },
        { name: '失败', value: stats.errorCalls },
      ],
      color: ['#4caf50', '#f44336'],
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
    <Box>
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
            {option && <ReactECharts option={option} style={{ height: 80 }} />}
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
  const { data, isFetching } = useQuery({
    queryKey: ['sync-logs', search],
    queryFn: () => systemApi.syncLogList({ page: search.page, rows: search.rows, syncType: search.syncType || undefined, status: search.status || undefined }),
  })
  const rows = data?.list ?? []
  const total = data?.total ?? 0

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
    <Box>
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
  const { data, isFetching } = useQuery({
    queryKey: ['alerts', search],
    queryFn: () => systemApi.alertSearch(search),
  })
  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const ackMut = useMutation({
    mutationFn: systemApi.alertAcknowledge,
    onSuccess: () => { toast('已确认', 'success'); void qc.invalidateQueries({ queryKey: ['alerts'] }) },
  })
  const resolveMut = useMutation({
    mutationFn: systemApi.alertResolve,
    onSuccess: () => { toast('已处理', 'success'); void qc.invalidateQueries({ queryKey: ['alerts'] }) },
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
              <Button size="small" onClick={() => ackMut.mutate(r.id)}>确认</Button>
            )}
            {(r.status === 'ACTIVE' || r.status === 'ACKNOWLEDGED') && (
              <Button size="small" color="success" onClick={() => resolveMut.mutate(r.id)}>处理</Button>
            )}
          </Stack>
        )
      },
    },
  ]

  return (
    <Box>
      <Stack direction="row" spacing={1} mb={1}>
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
      <Box sx={{ height: 'calc(100vh - 360px)', minHeight: 320 }}>
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

interface ExternalApiRow { id: number; apiName: string; category: string; baseUrl: string; authType: string; status: number }

function ExternalApiTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const { data, isFetching } = useQuery({
    queryKey: ['ext-api-list'],
    queryFn: () => systemApi.externalApiList({}),
  })
  const rows: ExternalApiRow[] = Array.isArray(data) ? data.map((item) => ({
    id: Number(item.id ?? 0),
    apiName: String(item.apiName ?? ''),
    category: String(item.category ?? ''),
    baseUrl: String(item.baseUrl ?? ''),
    authType: String(item.authType ?? ''),
    status: Number(item.status ?? 0),
  })) : []

  const testMut = useMutation({
    mutationFn: (id: number) => systemApi.externalApiHealthStatus(id),
    onSuccess: () => { toast('连通性测试成功', 'success'); void qc.invalidateQueries({ queryKey: ['ext-api-list'] }) },
    onError: () => toast('连通性测试失败', 'error'),
  })

  const columns: GridColDef[] = [
    { field: 'apiName', headerName: 'API名称', flex: 1, minWidth: 140 },
    { field: 'category', headerName: '分类', width: 120 },
    { field: 'baseUrl', headerName: '基础URL', flex: 1, minWidth: 200 },
    { field: 'authType', headerName: '认证方式', width: 110 },
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
          onClick={() => testMut.mutate((row as ExternalApiRow).id)}>
          测试连通
        </Button>
      ),
    },
  ]

  return (
    <Box sx={{ height: 'calc(100vh - 320px)', minHeight: 320 }}>
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
  const { data: timeseries } = useQuery({
    queryKey: ['perf-timeseries'],
    queryFn: () => systemApi.performanceTimeseries({ metric: 'response_time', hours: 24 }),
    refetchInterval: 60000,
  })
  const { data: slowQuery } = useQuery({
    queryKey: ['perf-slow-query'],
    queryFn: () => systemApi.performanceSlowQuery<SlowQueryRow>({ rows: 10, minMs: 500 }),
  })
  const { data: analysis } = useQuery({
    queryKey: ['perf-analysis'],
    queryFn: systemApi.performanceAnalysis,
  })

  const tsData = (timeseries ?? []) as TimeseriesPoint[]
  const slowRows: SlowQueryRow[] = slowQuery?.list ?? []

  const timeseriesOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: tsData.map(d => d.time ?? d.ts ?? '') },
    yAxis: { type: 'value', name: 'ms' },
    series: [
      { name: 'P50', type: 'line', smooth: true, data: tsData.map(d => d.p50 ?? 0), color: '#4caf50' },
      { name: 'P95', type: 'line', smooth: true, data: tsData.map(d => d.p95 ?? 0), color: '#ff9800' },
      { name: 'P99', type: 'line', smooth: true, data: tsData.map(d => d.p99 ?? 0), color: '#f44336' },
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
    <Box>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" fontWeight={600} mb={1}>响应时间趋势（近24h）</Typography>
            {tsData.length > 0
              ? <ReactECharts option={timeseriesOption} style={{ height: 240 }} />
              : <Typography color="text.secondary" variant="body2">暂无数据</Typography>}
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
  const { data: health } = useQuery({ queryKey: ['system-health'], queryFn: systemApi.health, enabled: open })
  const { data: stats } = useQuery({ queryKey: ['api-log-stats'], queryFn: systemApi.apiLogStats, enabled: open })
  const { data: alerts } = useQuery({ queryKey: ['alerts-active'], queryFn: systemApi.alertActive, enabled: open })
  const { data: analysis } = useQuery({ queryKey: ['perf-analysis'], queryFn: systemApi.performanceAnalysis, enabled: open })

  const handleDownload = useCallback(() => {
    const healthMap = (health ?? null) as Record<string, ServiceHealthStatus> | null
    const statsData = (stats ?? null) as ApiLogStats | null
    const alertsList = (alerts ?? null) as AlertRecord[] | null
    const perfAnalysis = (analysis ?? null) as PerformanceAnalysis | null
    const lines: string[] = [
      `系统诊断报告 — ${new Date().toLocaleString()}`,
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
    ]
    const blob = new Blob([lines.filter(l => l !== undefined).join('\n')], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `system-diagnose-${Date.now()}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }, [health, stats, alerts, analysis])

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>一键诊断报告</DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" color="text.secondary" mb={2}>
          报告将汇总当前系统状态：服务健康、API统计、活跃告警、性能分析。
        </Typography>
        <Stack spacing={1}>
          {[
            { label: '服务健康', ok: !!health },
            { label: 'API 统计', ok: !!stats },
            { label: '告警记录', ok: !!alerts },
            { label: '性能分析', ok: !!analysis },
          ].map(item => (
            <Stack key={item.label} direction="row" spacing={1} alignItems="center">
              {item.ok
                ? <CheckCircleIcon sx={{ color: '#4caf50', fontSize: 18 }} />
                : <LinearProgress sx={{ flex: 1, maxWidth: 80 }} />}
              <Typography variant="body2">{item.label}</Typography>
            </Stack>
          ))}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" startIcon={<DownloadIcon />} onClick={handleDownload}>
          下载报告
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
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', p: 2 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>系统监控</Typography>
      <HealthBanner onDiagnose={() => setDiagnoseOpen(true)} />
      <Paper sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <Tabs value={tab} onChange={(_, v: number) => setTab(v)}
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
          <Tab label="系统信息" />
          <Tab label="接口日志" />
          <Tab label="同步日志" />
          <Tab label="告警记录" />
          <Tab label="外部API" />
          <Tab label="性能监控" />
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





