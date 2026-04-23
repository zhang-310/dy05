import { useState, useCallback } from 'react'
import { Box, TextField, Button, Stack, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { configApi, type SysConfig } from '@/api/config'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

export default function ConfigPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, configKey: '' })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SysConfig>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({ queryKey: ['sys-configs', search], queryFn: () => configApi.list(search) })
  const saveMut = useMutation({ mutationFn: configApi.save, onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['sys-configs'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const delMut = useMutation({ mutationFn: configApi.delete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['sys-configs'] }) }, onError: (e: Error) => toast(e.message, 'error') })

  const openAdd = useCallback(() => { setForm({}); setFormOpen(true) }, [])
  const openEdit = useCallback((row: SysConfig) => { setForm(row); setFormOpen(true) }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'configKey', headerName: '配置键', flex: 1 },
    { field: 'configValue', headerName: '配置值', flex: 1.5 },
    { field: 'description', headerName: '说明', flex: 1 },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 140, sortable: false, renderCell: ({ row }) => <Stack direction="row" gap={1}><Button size="small" onClick={() => openEdit(row)}>编辑</Button><Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button></Stack> },
  ]

  const searchSlot = (
    <>
      <TextField label="配置键" size="small" value={query.configKey} onChange={e => setQuery(q => ({ ...q, configKey: e.target.value }))} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { setQuery(q => ({ ...q, configKey: '' })); setSearch({ page: 0, rows: 20, configKey: '' }) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增配置</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>系统配置</Typography>
      <StandardDataGrid rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching} paginationMode="server" paginationModel={{ page: search.page, pageSize: search.rows }} onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))} searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }} />
      <FormDialog open={formOpen} title={form.id ? '编辑配置' : '新增配置'} onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="配置键" value={form.configKey ?? ''} onChange={e => setForm(f => ({ ...f, configKey: e.target.value }))} fullWidth />
          <TextField label="配置值" value={form.configValue ?? ''} onChange={e => setForm(f => ({ ...f, configValue: e.target.value }))} fullWidth />
          <TextField label="说明" value={form.description ?? ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该配置吗？" onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}
