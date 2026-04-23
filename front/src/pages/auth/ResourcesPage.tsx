import { useState, useCallback } from 'react'
import { Box, Stack, Button, TextField, MenuItem, Chip } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { authApi, type AuthResource, type AuthResourceSave } from '@/api/auth'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const RESOURCE_TYPES = ['menu', 'button', 'api']
const defaultForm: AuthResourceSave = { parentId: 0, resourceName: '', resourceCode: '', resourceType: 'menu', requestMethod: '', module: '', sortOrder: 0 }

export default function ResourcesPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<AuthResourceSave>(defaultForm)
  const [editId, setEditId] = useState<number | undefined>()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [search, setSearch] = useState({ page: 0, rows: 50 })

  const { data: resources, isFetching } = useQuery({
    queryKey: ['auth-resources'],
    queryFn: () => authApi.resourceList(),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<AuthResourceSave>) => authApi.resourceSave(p),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['auth-resources'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: authApi.resourceDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['auth-resources'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setEditId(undefined); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AuthResource) => {
    setForm({ parentId: row.parentId, resourceName: row.resourceName, resourceCode: row.resourceCode, resourceType: row.resourceType, requestMethod: row.requestMethod, module: row.module, sortOrder: row.sortOrder })
    setEditId(row.id); setFormOpen(true)
  }, [])

  const allResources = resources ?? []
  const pagedRows = allResources.slice(search.page * search.rows, (search.page + 1) * search.rows)

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'resourceName', headerName: '资源名称', flex: 1 },
    { field: 'resourceCode', headerName: '资源编码', flex: 1 },
    {
      field: 'resourceType', headerName: '类型', width: 90,
      renderCell: ({ value }) => {
        const colorMap: Record<string, 'primary' | 'secondary' | 'default'> = { menu: 'primary', button: 'secondary', api: 'default' }
        return <Chip label={String(value)} size="small" color={colorMap[String(value)] ?? 'default'} />
      },
    },
    { field: 'requestMethod', headerName: '请求方法', width: 100 },
    { field: 'module', headerName: '模块', width: 120 },
    { field: 'sortOrder', headerName: '排序', width: 70 },
    {
      field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: ({ value }) => formatDate(value),
    },
    {
      field: '_actions', headerName: '操作', width: 120,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={1}>
          <Box component="span" sx={{ color: 'primary.main', cursor: 'pointer', fontSize: 13 }} onClick={() => openEdit(row as AuthResource)}>编辑</Box>
          <Box component="span" sx={{ color: 'error.main', cursor: 'pointer', fontSize: 13 }} onClick={() => setDeleteId(row.id)}>删除</Box>
        </Stack>
      ),
    },
  ]

  const actionSlot = (
    <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增资源</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={pagedRows} columns={columns}
        loading={isFetching} paginationMode="client"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch({ page: m.page, rows: m.pageSize })}
        actionSlot={actionSlot} sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={editId ? '编辑资源' : '新增资源'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(editId ? { ...form, id: editId } : form)}
        loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="上级ID（0=顶级）" type="number" value={form.parentId ?? 0}
            onChange={e => setForm(f => ({ ...f, parentId: Number(e.target.value) }))} fullWidth />
          <TextField label="资源名称" value={form.resourceName}
            onChange={e => setForm(f => ({ ...f, resourceName: e.target.value }))} fullWidth />
          <TextField label="资源编码" value={form.resourceCode}
            onChange={e => setForm(f => ({ ...f, resourceCode: e.target.value }))} fullWidth />
          <TextField select label="类型" value={form.resourceType}
            onChange={e => setForm(f => ({ ...f, resourceType: e.target.value }))} fullWidth>
            {RESOURCE_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
          </TextField>
          <TextField label="请求方法（如 GET/POST/PUT/DELETE）" value={form.requestMethod ?? ''}
            onChange={e => setForm(f => ({ ...f, requestMethod: e.target.value }))} fullWidth />
          <TextField label="模块" value={form.module ?? ''}
            onChange={e => setForm(f => ({ ...f, module: e.target.value }))} fullWidth />
          <TextField label="排序" type="number" value={form.sortOrder ?? 0}
            onChange={e => setForm(f => ({ ...f, sortOrder: Number(e.target.value) }))} fullWidth />
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该资源吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
    </Box>
  )
}
