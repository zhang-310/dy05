import { useState } from 'react'
import { Box, TextField, Button } from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { logApi } from '@/api/log'
import { useToast } from '@/contexts/ToastContext'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

export default function OperationLogPage() {
  const toast = useToast()
  const [search, setSearch] = useState({ page: 0, rows: 20, username: '', module: '' })
  const [query, setQuery] = useState(search)
  const { data, isFetching } = useQuery({ queryKey: ['op-logs', search], queryFn: () => logApi.list(search) })

  const handleExport = async () => {
    try {
      const token = localStorage.getItem('token')
      const res = await fetch('/api/v1/log/operation/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify({ username: search.username, module: search.module }),
      })
      if (!res.ok) throw new Error('导出失败')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = 'operation-logs.xlsx'; a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      toast(e instanceof Error ? e.message : '导出失败', 'error')
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'username', headerName: '操作人', width: 120 },
    { field: 'module', headerName: '模块', width: 120 },
    { field: 'action', headerName: '操作', flex: 1 },
    { field: 'ip', headerName: 'IP', width: 130 },
    { field: 'createTime', headerName: '时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
  ]

  const searchSlot = (
    <>
      <TextField label="操作人" size="small" value={query.username} onChange={e => setQuery(q => ({ ...q, username: e.target.value }))} />
      <TextField label="模块" size="small" value={query.module} onChange={e => setQuery(q => ({ ...q, module: e.target.value }))} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { setQuery({ page: 0, rows: 20, username: '', module: '' }); setSearch({ page: 0, rows: 20, username: '', module: '' }) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport}>导出</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching} paginationMode="server" paginationModel={{ page: search.page, pageSize: search.rows }} onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))} searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }} />
    </Box>
  )
}
