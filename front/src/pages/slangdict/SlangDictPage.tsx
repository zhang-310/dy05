import { useState } from 'react'
import {
  Box, Typography, Stack, Button, TextField, Chip, Switch,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { slangApi } from '@/api/slangdict'
import { formatDate } from '@/utils/date'

interface SlangEntry {
  id: number; term: string; definition: string; example: string
  industry: string; enabled: boolean; createTime: string
}

export default function SlangDictPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20 })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SlangEntry>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['slang-dict', search],
    queryFn: () => slangApi.list(search),
  })

  const saveMut = useMutation({
    mutationFn: (params: Partial<SlangEntry>) => slangApi.save(params),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['slang-dict'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => slangApi.delete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['slang-dict'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const toggleMut = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      enabled ? slangApi.enable(id) : slangApi.disable(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slang-dict'] }),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'term', headerName: '术语', width: 140, renderCell: ({ value }) => <Typography variant="body2" fontWeight={600}>{String(value ?? '')}</Typography> },
    { field: 'definition', headerName: '释义', flex: 1, minWidth: 180 },
    { field: 'example', headerName: '示例', flex: 1, minWidth: 160 },
    { field: 'industry', headerName: '行业', width: 90, renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'enabled', headerName: '状态', width: 90,
      renderCell: ({ row }) => (
        <Switch size="small" checked={Boolean((row as SlangEntry).enabled)}
          onChange={e => toggleMut.mutate({ id: (row as SlangEntry).id, enabled: e.target.checked })} />
      ) },
    { field: 'createTime', headerName: '创建时间', width: 150, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => { setForm(row as SlangEntry); setFormOpen(true) }}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as SlangEntry).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <TextField size="small" placeholder="搜索术语..." sx={{ width: 200 }}
      onChange={e => setSearch(s => ({ ...s, keyword: e.target.value, page: 0 } as typeof s))} />
  )

  const actionSlot = <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setForm({ enabled: true }); setFormOpen(true) }}>新增词条</Button>

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6" fontWeight={600}>行业俚语词典</Typography>

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot}
        sx={{ height: 520 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑词条' : '新增词条'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="术语" value={form.term ?? ''} onChange={e => setForm(f => ({ ...f, term: e.target.value }))} fullWidth />
          <TextField label="释义" value={form.definition ?? ''} onChange={e => setForm(f => ({ ...f, definition: e.target.value }))} fullWidth multiline minRows={2} />
          <TextField label="示例用法" value={form.example ?? ''} onChange={e => setForm(f => ({ ...f, example: e.target.value }))} fullWidth multiline minRows={2} />
          <TextField label="行业分类" value={form.industry ?? ''} onChange={e => setForm(f => ({ ...f, industry: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content="确定删除该词条？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)} loading={deleteMut.isPending} />
    </Box>
  )
}
