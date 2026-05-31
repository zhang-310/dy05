import { useState, useCallback } from 'react'
import { Alert, Box, TextField, Stack, Card, CardContent, Typography, Grid, Button } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { systemApi, type ApiLog, type ApiLogQuery } from '@/api/system'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const API_LOG_ENDPOINTS = [
  '/system/api-log/list',
  '/system/api-log/stats',
].join('|')
const API_LOG_UNSUPPORTED_ACTIONS = [
  'local-api-log-fallback',
  'local-stats-fallback',
  'server-export-from-current-page',
  'client-side-success-rate-synthesis',
  'api-log-detail-fetch',
  'local-csv-export',
].join('|')
const API_LOG_UNSUPPORTED_ENDPOINTS = [
  '/system/api-log/export',
  '/system/api-log/get',
  '/system/api-log/delete',
].join('|')

export default function ApiLogPage() {
  const [search, setSearch] = useState<ApiLogQuery>({ page: 0, rows: 20 })
  const [draft, setDraft] = useState<ApiLogQuery>({ page: 0, rows: 20 })

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['api-logs', search],
    queryFn: () => systemApi.apiLogList(search),
  })
  const { data: stats, isError: statsIsError, error: statsError, refetch: refetchStats } = useQuery({
    queryKey: ['api-log-stats'],
    queryFn: () => systemApi.apiLogStats(),
    refetchInterval: 30000,
  })

  const handleSearch = useCallback(() => setSearch({ ...draft, page: 0 }), [draft])
  const rows = normalizeRows<ApiLog>(data)
  const total = readTotal(data, rows.length)

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'apiPath', headerName: '接口路径', flex: 1, minWidth: 200 },
    { field: 'method', headerName: '方法', width: 80 },
    {
      field: 'statusCode', headerName: '状态码', width: 90,
      renderCell: ({ value }) => (
        <Typography variant="caption" sx={{ color: value >= 400 ? 'error.main' : 'success.main', fontWeight: 600 }}>{value}</Typography>
      ),
    },
    {
      field: 'responseTime', headerName: '耗时(ms)', width: 100,
      renderCell: ({ value }) => (
        <Typography variant="caption" sx={{ color: value > 1000 ? 'warning.main' : 'text.primary' }}>{value}</Typography>
      ),
    },
    { field: 'ip', headerName: 'IP', width: 130 },
    { field: 'errorMsg', headerName: '错误信息', flex: 1 },
    { field: 'createTime', headerName: '时间', width: 160, renderCell: ({ value }) => formatDate(value) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField size="small" label="接口路径" value={draft.apiPath ?? ''}
        onChange={e => setDraft(d => ({ ...d, apiPath: e.target.value }))} sx={{ width: 200 }} />
      <TextField size="small" label="状态码" type="number" value={draft.statusCode ?? ''}
        onChange={e => setDraft(d => ({ ...d, statusCode: e.target.value ? Number(e.target.value) : undefined }))} sx={{ width: 100 }} />
      <TextField size="small" type="date" label="开始时间" InputLabelProps={{ shrink: true }}
        value={draft.startTime ?? ''} onChange={e => setDraft(d => ({ ...d, startTime: e.target.value }))} sx={{ width: 160 }} />
      <TextField size="small" type="date" label="结束时间" InputLabelProps={{ shrink: true }}
        value={draft.endTime ?? ''} onChange={e => setDraft(d => ({ ...d, endTime: e.target.value }))} sx={{ width: 160 }} />
      <Button variant="contained" onClick={handleSearch}>查询</Button>
    </Stack>
  )

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="api-log-page-workbench"
      data-contract-scope="system-api-log-readonly-observability"
      data-ready-endpoints={API_LOG_ENDPOINTS}
      data-unsupported-actions={API_LOG_UNSUPPORTED_ACTIONS}
      data-unsupported-endpoints={API_LOG_UNSUPPORTED_ENDPOINTS}
      data-row-count={rows.length}
      data-total-count={total}
      data-list-error={String(isError)}
      data-stats-error={String(statsIsError)}
      data-has-stats={String(Boolean(stats))}
      data-query-path={search.apiPath ?? ''}
      data-query-status={search.statusCode ?? ''}
      data-no-local-api-log-fallback="true"
      data-no-local-stats-fallback="true"
      data-no-local-export-csv-fallback="true"
      data-no-detail-fetch="true"
    >
      <PageHeader
        title="API 调用日志"
        subtitle="记录系统接口调用、响应耗时和错误信息；导出与详情由后端接口单独处理。"
        actions={(
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => {
            void refetch()
            void refetchStats()
          }} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        data-testid="api-log-source-contract"
        data-contract-source="/system/api-log/list|/system/api-log/stats"
        data-query-field-map="apiPath->apiName|statusCode->status"
        data-unsupported-endpoints={API_LOG_UNSUPPORTED_ENDPOINTS}
        data-no-local-api-log-fallback="true"
        data-no-local-stats-fallback="true"
        data-no-local-export-csv-fallback="true"
      >
        列表来自 `/system/api-log/list`，统计来自 `/system/api-log/stats`；本页只展示真实接口结果，不伪造接口成功率或趋势。
      </Alert>

      {stats && (
        <Grid
          container
          spacing={2}
          data-testid="api-log-stats-contract"
          data-contract-source="/system/api-log/stats"
          data-total-calls={stats.totalCalls}
          data-success-calls={stats.successCalls}
          data-error-calls={stats.errorCalls}
          data-no-local-stats-fallback="true"
        >
          {[
            { label: '总调用量', value: stats.totalCalls },
            { label: '成功调用', value: stats.successCalls },
            { label: '失败调用', value: stats.errorCalls },
            { label: '平均耗时(ms)', value: stats.avgResponseTime },
            { label: 'P99(ms)', value: stats.p99ResponseTime },
          ].map(item => (
            <Grid item xs={6} sm={4} md={2.4} key={item.label}>
              <Card variant="outlined">
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                  <Typography variant="h6" fontWeight={700}>{String(item.value ?? 0)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {statsIsError && (
        <Box
          data-testid="api-log-stats-error"
          data-contract-source="/system/api-log/stats"
          data-no-local-stats-fallback="true"
        >
          <ErrorAlert
            title="API 日志统计加载失败"
            message={statsError instanceof Error ? statsError.message : 'API 日志统计接口异常'}
            onRetry={() => void refetchStats()}
          />
        </Box>
      )}

      {isError && (
        <Box
          data-testid="api-log-list-error"
          data-contract-source="/system/api-log/list"
          data-no-local-api-log-fallback="true"
        >
          <ErrorAlert
            title="API 日志列表加载失败"
            message={error instanceof Error ? error.message : 'API 日志列表接口异常'}
            onRetry={() => void refetch()}
          />
        </Box>
      )}

      <Box
        data-testid="api-log-grid-contract"
        data-contract-source="/system/api-log/list"
        data-row-count={rows.length}
        data-total-count={total}
        data-query-path={search.apiPath ?? ''}
        data-query-status={search.statusCode ?? ''}
        data-no-local-api-log-fallback="true"
        data-no-detail-fetch="true"
        data-no-local-export-csv-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={total}
          loading={isFetching} paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={(
            <Box
              data-testid="api-log-search-contract"
              data-contract-source="/system/api-log/list"
              data-query-field-map="apiPath->apiName|statusCode->status"
              data-no-local-search-filter="true"
              data-draft-path={draft.apiPath ?? ''}
              data-draft-status={draft.statusCode ?? ''}
              data-applied-path={search.apiPath ?? ''}
              data-applied-status={search.statusCode ?? ''}
              data-no-local-api-log-fallback="true"
              sx={{ display: 'contents' }}
            >
              {searchSlot}
            </Box>
          )}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>
    </Box>
  )
}
