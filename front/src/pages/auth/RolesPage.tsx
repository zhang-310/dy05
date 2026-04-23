import { useState, useCallback } from 'react'
import { Box, TextField, Button, Chip, Stack, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { authApi, type AuthRole, type AuthRoleSave } from '@/api/auth'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const defaultForm: Partial<AuthRoleSave> = { roleName: '', roleCode: '', description: '', sortOrder: 0, status: 1 }

export default function RolesPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, roleName: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<AuthRoleSave>>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['auth-roles', search],
    queryFn: () => authApi.roleList(search),
  })
  const saveMut = useMutation({
    mutationFn: authApi.roleSave,
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['auth-roles'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: authApi.roleDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['auth-roles'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AuthRole) => {
    setForm({ id: row.id, roleName: row.roleName, roleCode: row.roleCode, description: row.description, sortOrder: row.sortOrder ?? 0, status: row.status })
    setFormOpen(true)
  }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])
  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'roleName', headerName: '角色名称', flex: 1, minWidth: 140 },
    { field: 'roleCode', headerName: '角色编码', width: 160 },
    { field: 'description', headerName: '描述', flex: 1.5 },
    {
      field: 'status', headerName: '状态', width: 80,
      renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 140, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={1}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField size="small" label="角色名称" value={query.roleName}
        onChange={e => setQuery(q => ({ ...q, roleName: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 180 }} />
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </Stack>
  )
  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建角色</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>角色管理</Typography>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={form.id ? '编辑角色' : '新建角色'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="角色名称" required value={form.roleName ?? ''} onChange={e => setForm(f => ({ ...f, roleName: e.target.value }))} fullWidth size="small" />
          <TextField label="角色编码" required value={form.roleCode ?? ''} onChange={e => setForm(f => ({ ...f, roleCode: e.target.value }))} fullWidth size="small" />
          <TextField label="排序" type="number" value={form.sortOrder ?? 0} onChange={e => setForm(f => ({ ...f, sortOrder: Number(e.target.value) }))} fullWidth size="small" />
          <TextField label="描述" value={form.description ?? ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth size="small" multiline rows={2} />
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该角色吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
    </Box>
  )
}

