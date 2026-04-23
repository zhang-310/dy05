import { useState, useCallback } from 'react'
import { Box, TextField, Button, Chip, Stack, MenuItem } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { authApi } from '@/api/auth'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const STATUS_OPTIONS = [
  { value: 1, label: '成功', color: 'success' as const },
  { value: 0, label: '失败', color: 'error' as const },
]

export default function LoginLogsPage() {
  const [query, setQuery] = useState({ page: 0, rows: 20, username: '', status: undefined as number | undefined })
  const [search, setSearch] = useState(query)
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])

  const { data, isFetching } = useQuery({
    queryKey: ['login-logs', search],
    queryFn: () => authApi.loginLogs(search),
  })

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
      <TextField select size="small" label="状态" value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 100 }}>
        <MenuItem value="">全部</MenuItem>
        {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>查询</Button>
    </Stack>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} sx={{ flex: 1 }}
      />
    </Box>
  )
}
