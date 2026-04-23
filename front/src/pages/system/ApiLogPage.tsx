import { useState, useCallback } from 'react'
import { Box, TextField, Stack, Card, CardContent, Typography, Grid } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { systemApi, type ApiLogQuery } from '@/api/system'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

export default function ApiLogPage() {
  const [search, setSearch] = useState<ApiLogQuery>({ page: 0, rows: 20 })
  const [draft, setDraft] = useState<ApiLogQuery>({ page: 0, rows: 20 })

  const { data, isFetching } = useQuery({
    queryKey: ['api-logs', search],
    queryFn: () => systemApi.apiLogList(search),
  })
  const { data: stats } = useQuery({
    queryKey: ['api-log-stats'],
    queryFn: () => systemApi.apiLogStats(),
    refetchInterval: 30000,
  })

  const handleSearch = useCallback(() => setSearch({ ...draft, page: 0 }), [draft])

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
      <Box component="button" onClick={handleSearch}
        sx={{ px: 2, py: 0.5, bgcolor: 'primary.main', color: 'white', border: 'none', borderRadius: 1, cursor: 'pointer' }}>查询</Box>
    </Stack>
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}>
      {stats && (
        <Grid container spacing={2}>
          {[
            { label: '总调用量', value: stats.totalCalls },
            { label: '成功调用', value: stats.successCalls },
            { label: '失败调用', value: stats.errorCalls },
            { label: '平均耗时(ms)', value: stats.avgResponseTime },
          ].map(item => (
            <Grid item xs={6} sm={3} key={item.label}>
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
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} sx={{ flex: 1 }}
      />
    </Box>
  )
}
