import { useState, type ReactNode } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Stack, TextField, Typography, MenuItem } from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { logApi, type OperationLog, type OperationLogQuery } from '@/api/log'
import { useToast } from '@/contexts/ToastContext'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

type OperationLogSearch = Required<Pick<OperationLogQuery, 'page' | 'rows'>> &
  Pick<OperationLogQuery, 'username' | 'module' | 'action' | 'status' | 'startTime' | 'endTime'>

const initialSearch: OperationLogSearch = { page: 0, rows: 20, username: '', module: '', action: '', startTime: '', endTime: '' }

const statusOptions = [
  { value: '', label: '全部结果' },
  { value: '1', label: '成功' },
  { value: '0', label: '失败' },
]
const OPERATION_LOG_ENDPOINTS = [
  '/log/operation/page',
  '/log/operation/export',
].join('|')
const OPERATION_LOG_UNSUPPORTED_ACTIONS = [
  'local-operation-log-fallback',
  'local-export-csv-fallback',
  'client-side-filter-only',
  'cross-user-log-bypass',
].join('|')

export default function OperationLogPage() {
  const toast = useToast()
  const [search, setSearch] = useState<OperationLogSearch>(initialSearch)
  const [query, setQuery] = useState(search)
  const [exportError, setExportError] = useState('')
  const { data, isFetching, isError, error, refetch } = useQuery({ queryKey: ['op-logs', search], queryFn: () => logApi.list(toRequest(search)) })

  const handleExport = async () => {
    try {
      setExportError('')
      const token = localStorage.getItem('token')
      const res = await fetch('/api/v1/log/operation/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify(toRequest({ ...search, rows: 2000 })),
      })
      if (!res.ok) throw new Error(`/log/operation/export 导出失败：HTTP ${res.status}`)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = 'operation-logs.csv'; a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      const message = e instanceof Error ? e.message : '/log/operation/export 导出失败'
      setExportError(message)
      toast(message, 'error')
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'username', headerName: '操作人', width: 120, valueGetter: (_value, row) => row.username ?? '-' },
    { field: 'module', headerName: '模块', width: 120 },
    { field: 'action', headerName: '操作', width: 120 },
    { field: 'requestMethod', headerName: '方法', width: 90, valueGetter: (_value, row) => row.requestMethod ?? '-' },
    { field: 'requestUri', headerName: 'URI', flex: 1, minWidth: 220, valueGetter: (_value, row) => row.requestUri ?? '-' },
    { field: 'ip', headerName: 'IP', width: 130, valueGetter: (_value, row) => row.ip ?? '-' },
    { field: 'durationMs', headerName: '耗时', width: 90, valueFormatter: (value: number | undefined) => value != null ? `${value}ms` : '-' },
    {
      field: 'status',
      headerName: '结果',
      width: 90,
      renderCell: ({ value }) => <Chip size="small" color={value === 0 ? 'error' : 'success'} label={value === 0 ? '失败' : '成功'} />,
    },
    { field: 'createTime', headerName: '时间', width: 170, valueFormatter: (v: string) => formatDate(v) },
  ]

  const rows = normalizeRows<OperationLog>(data)
  const total = readTotal(data, rows.length)
  const failedCount = rows.filter(row => row.status === 0).length
  const avgDuration = averageDuration(rows)

  const searchSlot = (
    <>
      <TextField label="操作人" size="small" value={query.username} onChange={e => setQuery(q => ({ ...q, username: e.target.value }))} />
      <TextField label="模块" size="small" value={query.module} onChange={e => setQuery(q => ({ ...q, module: e.target.value }))} />
      <TextField label="操作" size="small" value={query.action} onChange={e => setQuery(q => ({ ...q, action: e.target.value }))} />
      <TextField
        select
        label="结果"
        size="small"
        value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 120 }}
      >
        {statusOptions.map(option => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
      </TextField>
      <TextField label="开始时间" type="date" size="small" value={query.startTime} InputLabelProps={{ shrink: true }} onChange={e => setQuery(q => ({ ...q, startTime: e.target.value }))} />
      <TextField label="结束时间" type="date" size="small" value={query.endTime} InputLabelProps={{ shrink: true }} onChange={e => setQuery(q => ({ ...q, endTime: e.target.value }))} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { setQuery(initialSearch); setSearch(initialSearch) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport}>导出</Button>
  )

  const listErrorMessage = error instanceof Error ? error.message : '操作日志加载失败，请检查 /log/operation/page。'

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="operation-log-page-workbench"
      data-contract-scope="log-operation-readonly-export"
      data-ready-endpoints={OPERATION_LOG_ENDPOINTS}
      data-unsupported-actions={OPERATION_LOG_UNSUPPORTED_ACTIONS}
      data-row-count={rows.length}
      data-total-count={total}
      data-failed-count={failedCount}
      data-query-username={search.username || ''}
      data-query-module={search.module || ''}
      data-query-action={search.action || ''}
      data-query-status={search.status ?? ''}
      data-list-error={String(isError)}
      data-export-error={exportError}
      data-no-local-operation-log-fallback="true"
      data-no-local-export-csv-fallback="true"
    >
      <PageHeader
        title="操作日志"
        subtitle="记录后台用户操作行为，支持按操作人和模块筛选。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <MetricCard label="当前页日志" value={rows.length} hint={`总数 ${total}`} />
        <MetricCard label="失败请求" value={failedCount} hint="status=0" color={failedCount > 0 ? 'error.main' : 'success.main'} />
        <MetricCard label="平均耗时" value={avgDuration != null ? `${avgDuration}ms` : '-'} hint="仅按当前页计算" />
      </Stack>

      <Alert
        severity="info"
        data-testid="operation-log-source-contract"
        data-contract-source="/log/operation/page|/log/operation/export"
        data-query-fields="username|module|action|status|startTime|endTime"
        data-no-local-operation-log-fallback="true"
        data-no-cross-user-log-bypass="true"
      >
        列表来自 `/log/operation/page`；筛选字段对齐后端 `username/module/action/status/startTime/endTime`，非管理员由后端强制限定当前用户日志。
      </Alert>
      {isError && (
        <Box
          data-testid="operation-log-list-error"
          data-contract-source="/log/operation/page"
          data-no-local-operation-log-fallback="true"
        >
          <ErrorAlert title="操作日志加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {exportError && (
        <Alert
          severity="error"
          data-testid="operation-log-export-error"
          data-contract-source="/log/operation/export"
          data-input-retained="true"
          data-query-username={search.username || ''}
          data-no-local-export-csv-fallback="true"
        >
          {exportError}；当前筛选条件已保留，导出最多请求 2000 条，不会生成本地假 CSV。
        </Alert>
      )}

      <Box
        data-testid="operation-log-grid-contract"
        data-contract-source="/log/operation/page"
        data-row-count={rows.length}
        data-total-count={total}
        data-no-local-operation-log-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          rowCount={total}
          loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={(
            <Box
              data-testid="operation-log-search-contract"
              data-contract-source="/log/operation/page"
              data-draft-username={query.username || ''}
              data-applied-username={search.username || ''}
              data-applied-module={search.module || ''}
              data-applied-action={search.action || ''}
              data-applied-status={search.status ?? ''}
              data-no-client-side-filter-only="true"
              sx={{ display: 'contents' }}
            >
              {searchSlot}
            </Box>
          )}
          actionSlot={(
            <Box
              data-testid="operation-log-export-contract"
              data-contract-source="/log/operation/export"
              data-query-rows="2000"
              data-no-local-export-csv-fallback="true"
              sx={{ display: 'contents' }}
            >
              {actionSlot}
            </Box>
          )}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>
    </Box>
  )
}

function toRequest(search: OperationLogSearch): OperationLogQuery {
  return {
    page: search.page,
    rows: search.rows,
    username: search.username || undefined,
    module: search.module || undefined,
    action: search.action || undefined,
    status: search.status,
    startTime: search.startTime || undefined,
    endTime: search.endTime || undefined,
  }
}

function averageDuration(rows: OperationLog[]) {
  const values = rows.map(row => row.durationMs).filter((value): value is number => typeof value === 'number')
  if (values.length === 0) return null
  return Math.round(values.reduce((sum, value) => sum + value, 0) / values.length)
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
