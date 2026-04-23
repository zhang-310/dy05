import { useState } from 'react'
import { Box, MenuItem, TextField } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader } from '@/components/base'
import { logApi, type OperationLog } from '@/api/log'
import { useQuery } from '@tanstack/react-query'

const MODULE_OPTIONS = [
  { value: '', label: '全部模块' },
  { value: 'auth', label: '认证' },
  { value: 'douyin', label: '抖音' },
  { value: 'live', label: '直播' },
  { value: 'product', label: '商品' },
  { value: 'system', label: '系统' },
]

export default function SystemLogPage() {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [module, setModule] = useState('')
  const [username, setUsername] = useState('')

  const { data, isFetching } = useQuery({
    queryKey: ['system-log', page, pageSize, module, username],
    queryFn: () => logApi.systemList({
      page, rows: pageSize,
      module: module || undefined,
      username: username || undefined,
    }),
  })

  const rows: OperationLog[] = data?.list ?? []
  const total = data?.total ?? 0

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'username', headerName: '操作用户', width: 130 },
    { field: 'module', headerName: '模块', width: 110 },
    { field: 'action', headerName: '操作', flex: 1 },
    { field: 'ip', headerName: 'IP地址', width: 140 },
    { field: 'createTime', headerName: '操作时间', width: 180 },
  ]

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="系统日志" subtitle="系统操作日志记录" />

      <Box sx={{ mb: 2, display: 'flex', gap: 2 }}>
        <TextField
          size="small" label="用户名" value={username}
          onChange={(e) => { setUsername(e.target.value); setPage(0) }}
          sx={{ width: 180 }}
        />
        <TextField
          select size="small" label="模块" value={module}
          onChange={(e) => { setModule(e.target.value); setPage(0) }}
          sx={{ width: 150 }}
        >
          {MODULE_OPTIONS.map((o) => (
            <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
          ))}
        </TextField>
      </Box>

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
        getRowId={(r) => (r as OperationLog).id}
        autoHeight
      />
    </Box>
  )
}
