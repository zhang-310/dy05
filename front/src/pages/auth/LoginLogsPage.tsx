import { useState, useCallback } from 'react'
import { Alert, Box, Card, CardContent, Grid, TextField, Button, Chip, Stack, MenuItem, Typography } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { authApi, type LoginLog } from '@/api/auth'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const STATUS_OPTIONS = [
  { value: '', label: '全部', color: 'default' as const },
  { value: 1, label: '成功', color: 'success' as const },
  { value: 0, label: '失败', color: 'error' as const },
]
const LOGIN_LOGS_ROUTE = '/admin/auth/login-logs'
const LOGIN_LOGS_ENDPOINT = '/auth/user/login-logs'

function loginLogFilterContext(search: { page: number; rows: number; username: string; userId: string; status?: number }) {
  return `route=${LOGIN_LOGS_ROUTE}; username=${search.username.trim() || '空'}; userId=${search.userId || '空'}; status=${search.status ?? '全部'}; page=${search.page}; rows=${search.rows}`
}

function MetricCard({ label, value, helper }: { label: string; value: string | number; helper?: string }) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" sx={{ mt: 0.5 }}>{value}</Typography>
        {helper && <Typography variant="caption" color="text.secondary">{helper}</Typography>}
      </CardContent>
    </Card>
  )
}

export default function LoginLogsPage() {
  const [query, setQuery] = useState({ page: 0, rows: 20, username: '', userId: '', status: undefined as number | undefined })
  const [search, setSearch] = useState(query)
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['login-logs', search],
    queryFn: () => authApi.loginLogs({
      page: search.page,
      rows: search.rows,
      username: search.username || undefined,
      userId: search.userId ? Number(search.userId) : undefined,
      status: search.status,
    }),
  })
  const rows = normalizeRows<LoginLog>(data)
  const total = readTotal(data, rows.length)
  const successCount = rows.filter(item => item.status === 1).length
  const failedCount = rows.filter(item => item.status === 0).length

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'userId', headerName: '用户ID', width: 90 },
    { field: 'username', headerName: '用户名', width: 140 },
    { field: 'loginType', headerName: '登录方式', width: 100 },
    { field: 'deviceType', headerName: '设备类型', width: 100 },
    { field: 'ip', headerName: 'IP地址', width: 150 },
    { field: 'userAgent', headerName: '客户端', flex: 1, minWidth: 180 },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => {
        const o = STATUS_OPTIONS.find(x => x.value === value)
        return <Chip label={o?.label ?? '未知'} color={o?.color ?? 'default'} size="small" />
      },
    },
    { field: 'failReason', headerName: '失败原因', width: 150 },
    { field: 'loginTime', headerName: '登录时间', width: 170, valueFormatter: (v: string) => formatDate(v) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
      <TextField size="small" label="用户名" value={query.username}
        onChange={e => setQuery(q => ({ ...q, username: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 160 }} />
      <TextField size="small" label="用户ID" value={query.userId}
        onChange={e => setQuery(q => ({ ...q, userId: e.target.value.replace(/\D/g, '') }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 110 }} />
      <TextField select size="small" label="状态" value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 100 }}>
        {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>查询</Button>
      <Button size="small" onClick={() => {
        const reset = { page: 0, rows: 20, username: '', userId: '', status: undefined as number | undefined }
        setQuery(reset)
        setSearch(reset)
      }}>重置</Button>
    </Stack>
  )

  const listErrorMessage = error instanceof Error
    ? `${LOGIN_LOGS_ENDPOINT} 登录日志加载失败：${error.message}（${loginLogFilterContext(search)}）`
    : `${LOGIN_LOGS_ENDPOINT} 登录日志加载失败（${loginLogFilterContext(search)}）`

  return (
    <Box
      data-testid="auth-login-logs-workbench"
      data-contract-scope="auth-login-logs"
      data-ready-endpoints="/auth/user/login-logs"
      data-unsupported-actions="login-log-export,ip-geo-enrichment,user-agent-parser,force-admin-scope"
      data-no-local-login-log-fallback="true"
      data-no-local-export-csv-fallback="true"
      data-no-client-admin-scope-bypass="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="登录日志"
        subtitle="登录成功/失败记录、设备信息和失败原因统一查看。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        data-testid="auth-login-logs-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-client-admin-scope-bypass="true"
        data-no-local-export-csv-fallback="true"
      >
        登录日志来自 /auth/user/login-logs；管理员可按 userId、username、status 筛选，普通用户后端会强制只返回本人记录。
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={4}><MetricCard label="日志总数" value={total} helper="后端分页 total" /></Grid>
        <Grid item xs={12} sm={4}><MetricCard label="本页成功" value={successCount} helper="当前页 status=1" /></Grid>
        <Grid item xs={12} sm={4}><MetricCard label="本页失败" value={failedCount} helper="当前页 status=0" /></Grid>
      </Grid>

      {isError && (
        <Box data-testid="auth-login-logs-list-error" data-no-local-login-log-fallback="true">
          <ErrorAlert title="登录日志加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
      />
    </Box>
  )
}
