import { useState, useCallback } from 'react'
import { Box, TextField, Stack, Drawer, Typography, Divider, Chip } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { logApi, type AuditLog, type AuditLogQuery } from '@/api/log'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

export default function AuditLogPage() {
  const [search, setSearch] = useState<AuditLogQuery>({ page: 0, rows: 20 })
  const [draft, setDraft] = useState<AuditLogQuery>({ page: 0, rows: 20 })
  const [selected, setSelected] = useState<AuditLog | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['audit-logs', search],
    queryFn: () => logApi.auditLogPage(search),
  })

  const handleSearch = useCallback(() => setSearch({ ...draft, page: 0 }), [draft])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'username', headerName: '操作人', width: 120 },
    { field: 'module', headerName: '模块', width: 100 },
    { field: 'action', headerName: '操作', width: 120 },
    { field: 'targetType', headerName: '对象类型', width: 110 },
    { field: 'targetId', headerName: '对象ID', width: 80 },
    { field: 'ip', headerName: 'IP', width: 130 },
    {
      field: 'createTime', headerName: '时间', width: 160,
      renderCell: ({ value }) => formatDate(value),
    },
    {
      field: '_actions', headerName: '操作', width: 80,
      renderCell: ({ row }) => (
        <Typography
          variant="caption"
          sx={{ color: 'primary.main', cursor: 'pointer' }}
          onClick={() => setSelected(row as AuditLog)}
        >详情</Typography>
      ),
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField size="small" label="操作人" value={draft.username ?? ''}
        onChange={e => setDraft(d => ({ ...d, username: e.target.value }))} sx={{ width: 140 }} />
      <TextField size="small" label="模块" value={draft.module ?? ''}
        onChange={e => setDraft(d => ({ ...d, module: e.target.value }))} sx={{ width: 120 }} />
      <TextField size="small" label="操作" value={draft.action ?? ''}
        onChange={e => setDraft(d => ({ ...d, action: e.target.value }))} sx={{ width: 120 }} />
      <TextField size="small" type="date" label="开始时间" InputLabelProps={{ shrink: true }}
        value={draft.startTime ?? ''}
        onChange={e => setDraft(d => ({ ...d, startTime: e.target.value }))} sx={{ width: 160 }} />
      <TextField size="small" type="date" label="结束时间" InputLabelProps={{ shrink: true }}
        value={draft.endTime ?? ''}
        onChange={e => setDraft(d => ({ ...d, endTime: e.target.value }))} sx={{ width: 160 }} />
      <Box component="button" onClick={handleSearch}
        sx={{ px: 2, py: 0.5, bgcolor: 'primary.main', color: 'white', border: 'none', borderRadius: 1, cursor: 'pointer' }}>查询</Box>
    </Stack>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>审计日志</Typography>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} sx={{ flex: 1 }}
      />
      <Drawer anchor="right" open={!!selected} onClose={() => setSelected(null)}
        PaperProps={{ sx: { width: 560, p: 3 } }}>
        {selected && (
          <Stack spacing={2}>
            <Typography variant="h6">审计日志详情</Typography>
            <Divider />
            <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
              <Chip label={`模块: ${selected.module}`} size="small" />
              <Chip label={`操作: ${selected.action}`} size="small" color="primary" />
              <Chip label={`IP: ${selected.ip}`} size="small" variant="outlined" />
            </Stack>
            <Box>
              <Typography variant="caption" color="text.secondary">操作人</Typography>
              <Typography>{selected.username} (ID: {selected.userId})</Typography>
            </Box>
            {selected.targetType && (
              <Box>
                <Typography variant="caption" color="text.secondary">操作对象</Typography>
                <Typography>{selected.targetType} #{selected.targetId}</Typography>
              </Box>
            )}
            <Divider />
            {selected.beforeValue && (
              <Box>
                <Typography variant="caption" color="text.secondary">修改前</Typography>
                <Box component="pre" sx={{ fontSize: 12, bgcolor: 'grey.100', p: 1, borderRadius: 1, overflow: 'auto', maxHeight: 200 }}>
                  {selected.beforeValue}
                </Box>
              </Box>
            )}
            {selected.afterValue && (
              <Box>
                <Typography variant="caption" color="text.secondary">修改后</Typography>
                <Box component="pre" sx={{ fontSize: 12, bgcolor: 'success.50', p: 1, borderRadius: 1, overflow: 'auto', maxHeight: 200 }}>
                  {selected.afterValue}
                </Box>
              </Box>
            )}
            <Typography variant="caption" color="text.secondary">{formatDate(selected.createTime)}</Typography>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
