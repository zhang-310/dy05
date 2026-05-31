import { useState, type ReactNode } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, MenuItem, Stack, TextField, Typography } from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { logApi, type SystemLog, type SystemLogQuery } from '@/api/log'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const MODULE_OPTIONS = [
  { value: '', label: '全部模块' },
  { value: 'auth', label: '认证' },
  { value: 'douyin', label: '抖音' },
  { value: 'live', label: '直播' },
  { value: 'product', label: '商品' },
  { value: 'system', label: '系统' },
]

const EVENT_OPTIONS = [
  { value: '', label: '全部事件' },
  { value: 'startup', label: '启动' },
  { value: 'shutdown', label: '停止' },
  { value: 'error', label: '错误' },
  { value: 'warn', label: '告警' },
  { value: 'info', label: '信息' },
]

const STATUS_OPTIONS = [
  { value: '', label: '全部结果' },
  { value: '1', label: '成功' },
  { value: '0', label: '失败' },
]
const SYSTEM_LOG_ENDPOINTS = [
  '/log/system/page',
  '/log/system/export',
].join('|')
const SYSTEM_LOG_UNSUPPORTED_ACTIONS = [
  'local-system-log-fallback',
  'local-export-csv-fallback',
  'client-side-filter-only',
  'username-filter-query',
].join('|')

export default function SystemLogPage() {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [module, setModule] = useState('')
  const [eventType, setEventType] = useState('')
  const [status, setStatus] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [exportError, setExportError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['system-log', page, pageSize, module, eventType, status, startTime, endTime],
    queryFn: () => logApi.systemList(toRequest({ page, rows: pageSize, module, eventType, status, startTime, endTime })),
  })

  const rows: SystemLog[] = normalizeRows<SystemLog>(data)
  const total = readTotal(data, rows.length)
  const failedCount = rows.filter(row => row.status === 0).length
  const eventTypeCount = new Set(rows.map(row => row.eventType).filter(Boolean)).size

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'module', headerName: '模块', width: 110 },
    { field: 'eventType', headerName: '事件类型', width: 120, valueGetter: (_value, row) => row.eventType ?? '-' },
    { field: 'summary', headerName: '摘要', flex: 1, minWidth: 220, valueGetter: (_value, row) => row.summary ?? '-' },
    { field: 'detail', headerName: '详情', flex: 1, minWidth: 260, valueGetter: (_value, row) => row.detail ?? '-' },
    {
      field: 'status',
      headerName: '结果',
      width: 90,
      renderCell: ({ value }) => <Chip size="small" color={value === 0 ? 'error' : 'success'} label={value === 0 ? '失败' : '成功'} />,
    },
    { field: 'createTime', headerName: '事件时间', width: 180, valueFormatter: (v: string) => formatDate(v) },
  ]

  const listErrorMessage = error instanceof Error ? error.message : '系统日志加载失败，请检查 /log/system/page。'
  const currentRequest = { page, rows: pageSize, module, eventType, status, startTime, endTime }
  const resetFilters = () => {
    setModule('')
    setEventType('')
    setStatus('')
    setStartTime('')
    setEndTime('')
    setPage(0)
  }
  const handleExport = async () => {
    try {
      setExportError('')
      const token = localStorage.getItem('token')
      const res = await fetch('/api/v1/log/system/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify(toRequest({ ...currentRequest, rows: 2000 })),
      })
      if (!res.ok) throw new Error(`/log/system/export 导出失败：HTTP ${res.status}`)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'system-logs.csv'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setExportError(e instanceof Error ? e.message : '/log/system/export 导出失败')
    }
  }

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="system-log-page-workbench"
      data-contract-scope="log-system-readonly-export"
      data-ready-endpoints={SYSTEM_LOG_ENDPOINTS}
      data-unsupported-actions={SYSTEM_LOG_UNSUPPORTED_ACTIONS}
      data-row-count={rows.length}
      data-total-count={total}
      data-failed-count={failedCount}
      data-query-module={module}
      data-query-event-type={eventType}
      data-query-status={status}
      data-list-error={String(isError)}
      data-export-error={exportError}
      data-no-local-system-log-fallback="true"
      data-no-local-export-csv-fallback="true"
      data-no-username-filter-query="true"
    >
      <PageHeader
        title="系统日志"
        subtitle="系统级事件日志，适合追踪启动、错误、告警和平台后台任务行为。"
        actions={(
          <Stack direction="row" spacing={1}>
            <Button size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport}>
              导出
            </Button>
            <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
              刷新
            </Button>
          </Stack>
        )}
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <MetricCard label="当前页事件" value={rows.length} hint={`总数 ${total}`} />
        <MetricCard label="失败事件" value={failedCount} hint="status=0" color={failedCount > 0 ? 'error.main' : 'success.main'} />
        <MetricCard label="事件类型" value={eventTypeCount} hint="按当前页统计" />
      </Stack>

      <Alert
        severity="info"
        data-testid="system-log-source-contract"
        data-contract-source="/log/system/page|/log/system/export"
        data-query-fields="module|eventType|status|startTime|endTime"
        data-no-username-filter-query="true"
        data-no-local-system-log-fallback="true"
      >
        系统日志来自 `/log/system/page`；筛选字段对齐后端 `module/eventType/status/startTime/endTime`，不再发送后端不消费的用户名条件。
      </Alert>
      {isError && (
        <Box
          data-testid="system-log-list-error"
          data-contract-source="/log/system/page"
          data-no-local-system-log-fallback="true"
        >
          <ErrorAlert title="系统日志加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {exportError && (
        <Alert
          severity="error"
          data-testid="system-log-export-error"
          data-contract-source="/log/system/export"
          data-input-retained="true"
          data-query-module={module}
          data-query-event-type={eventType}
          data-query-status={status}
          data-no-local-export-csv-fallback="true"
        >
          {exportError}；当前筛选条件已保留，导出最多请求 2000 条，不会生成本地假 CSV。
        </Alert>
      )}

      <Box
        sx={{ mb: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}
        data-testid="system-log-filter-contract"
        data-contract-source="/log/system/page"
        data-query-fields="module|eventType|status|startTime|endTime"
        data-query-module={module}
        data-query-event-type={eventType}
        data-query-status={status}
        data-query-start-time={startTime}
        data-query-end-time={endTime}
        data-no-client-side-filter-only="true"
        data-no-username-filter-query="true"
      >
        <TextField
          select
          size="small"
          label="模块"
          value={module}
          onChange={(e) => { setModule(e.target.value); setPage(0) }}
          sx={{ width: 150 }}
        >
          {MODULE_OPTIONS.map((o) => (
            <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
          ))}
        </TextField>
        <TextField
          select
          size="small"
          label="事件类型"
          value={eventType}
          onChange={(e) => { setEventType(e.target.value); setPage(0) }}
          sx={{ width: 150 }}
        >
          {EVENT_OPTIONS.map((o) => (
            <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
          ))}
        </TextField>
        <TextField
          select
          size="small"
          label="结果"
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          sx={{ width: 130 }}
        >
          {STATUS_OPTIONS.map((o) => (
            <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          type="date"
          label="开始时间"
          value={startTime}
          InputLabelProps={{ shrink: true }}
          onChange={(e) => { setStartTime(e.target.value); setPage(0) }}
          sx={{ width: 160 }}
        />
        <TextField
          size="small"
          type="date"
          label="结束时间"
          value={endTime}
          InputLabelProps={{ shrink: true }}
          onChange={(e) => { setEndTime(e.target.value); setPage(0) }}
          sx={{ width: 160 }}
        />
        <Button variant="outlined" onClick={resetFilters}>重置</Button>
      </Box>

      <Box
        data-testid="system-log-grid-contract"
        data-contract-source="/log/system/page"
        data-row-count={rows.length}
        data-total-count={total}
        data-no-local-system-log-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <Box
          data-testid="system-log-export-contract"
          data-contract-source="/log/system/export"
          data-query-rows="2000"
          data-no-local-export-csv-fallback="true"
          sx={{ display: 'none' }}
        />
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={total}
          paginationMode="server"
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
          pageSizeOptions={[10, 20, 50]}
          disableRowSelectionOnClick
          getRowId={(r) => (r as SystemLog).id}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>
    </Box>
  )
}

function toRequest(search: { page: number; rows: number; module: string; eventType: string; status: string; startTime: string; endTime: string }): SystemLogQuery {
  return {
    page: search.page,
    rows: search.rows,
    module: search.module || undefined,
    eventType: search.eventType || undefined,
    status: search.status === '' ? undefined : Number(search.status),
    startTime: search.startTime || undefined,
    endTime: search.endTime || undefined,
  }
}

function MetricCard({ label, value, hint, color = 'text.primary' }: { label: string; value: ReactNode; hint: string; color?: string }) {
  return (
    <Card variant="outlined" sx={{ flex: 1 }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" sx={{ color }}>{value}</Typography>
        <Typography variant="caption" color="text.secondary">{hint}</Typography>
      </CardContent>
    </Card>
  )
}
