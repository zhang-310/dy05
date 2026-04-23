import { useState, useCallback } from 'react'
import { Box, TextField, Button, Chip, Stack, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { authApi, type AuthUser, type AuthUserSave } from '@/api/auth'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const defaultForm: AuthUserSave = { username: '', nickname: '', mobile: '', email: '', avatarUrl: '', roleCode: '', status: 1 }

export default function UsersPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, username: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<AuthUserSave>(defaultForm)
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['auth-users', search],
    queryFn: () => authApi.list(search),
  })
  const saveMut = useMutation({
    mutationFn: authApi.save,
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['auth-users'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: authApi.delete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['auth-users'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AuthUser) => { setForm({ id: row.id, username: row.username, nickname: row.nickname ?? '', mobile: row.mobile ?? '', email: row.email ?? '', avatarUrl: row.avatarUrl ?? '', roleCode: row.roleCode ?? '', status: row.status }); setFormOpen(true) }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'username', headerName: '用户名', flex: 1 },
    { field: 'nickname', headerName: '昵称', flex: 1 },
    { field: 'email', headerName: '邮箱', flex: 1.5 },
    { field: 'status', headerName: '状态', width: 90, renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" /> },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 140, sortable: false, renderCell: ({ row }) => <Stack direction="row" gap={1}><Button size="small" onClick={() => openEdit(row)}>编辑</Button><Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button></Stack> },
  ]

  const searchSlot = (
    <>
      <TextField label="用户名" size="small" value={query.username} onChange={e => setQuery(q => ({ ...q, username: e.target.value }))} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { setQuery(q => ({ ...q, username: '' })); setSearch({ page: 0, rows: 20, username: '' }) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增用户</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>用户管理</Typography>
      <StandardDataGrid rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching} paginationMode="server" paginationModel={{ page: search.page, pageSize: search.rows }} onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))} searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }} />
      <FormDialog open={formOpen} title={form.id ? '编辑用户' : '新增用户'} onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="用户名" value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} fullWidth />
          <TextField label="昵称" value={form.nickname ?? ''} onChange={e => setForm(f => ({ ...f, nickname: e.target.value }))} fullWidth />
          <TextField label="手机号" value={form.mobile ?? ''} onChange={e => setForm(f => ({ ...f, mobile: e.target.value }))} fullWidth />
          <TextField label="邮箱" value={form.email ?? ''} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} fullWidth />
          <TextField label="头像URL" value={form.avatarUrl ?? ''} onChange={e => setForm(f => ({ ...f, avatarUrl: e.target.value }))} fullWidth />
          <TextField label="角色编码" value={form.roleCode ?? ''} onChange={e => setForm(f => ({ ...f, roleCode: e.target.value }))} fullWidth />
          {!form.id && <TextField label="密码" type="password" value={form.password ?? ''} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} fullWidth />}
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该用户吗？" onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}

