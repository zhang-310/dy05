import { useState, useCallback } from 'react'
import { Alert, Box, Card, CardContent, Grid, MenuItem, TextField, Button, Chip, Stack, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { authApi, type AuthUser, type AuthUserSave } from '@/api/auth'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const defaultForm: AuthUserSave = { username: '', nickname: '', mobile: '', email: '', avatarUrl: '', roleCode: '' }
const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
]
const USERS_ROUTE = '/admin/auth/users'
const USER_ENDPOINTS = {
  search: '/auth/user/search',
  save: '/auth/user/save',
  delete: '/auth/user/delete',
  ban: '/auth/user/ban',
} as const

function userContext(user?: Partial<AuthUser> | null, fallbackId?: number | string) {
  return `route=${USERS_ROUTE}; userId=${user?.id ?? fallbackId ?? '新增'}; username=${user?.username || '未填写'}; nickname=${user?.nickname || '未填写'}; roleCode=${user?.roleCode || '-'}; status=${user?.status ?? '-'}`
}

function userSaveContext(payload?: Partial<AuthUserSave> | null) {
  return `route=${USERS_ROUTE}; userId=${payload?.id ?? '新增'}; username=${payload?.username || '未填写'}; nickname=${payload?.nickname || '未填写'}; roleCode=${payload?.roleCode || '-'}`
}

function userFilterContext(search: { page: number; rows: number; username: string; status?: number }) {
  return `route=${USERS_ROUTE}; username=${search.username.trim() || '空'}; status=${search.status ?? '全部'}; page=${search.page}; rows=${search.rows}`
}

function MetricCard({ label, value, helper }: { label: string; value: string | number; helper?: string }) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" sx={{ mt: 0.5 }}>{value}</Typography>
        {helper && <Typography variant="caption" color="text.secondary">{helper}</Typography>}
      </CardContent>
    </Card>
  )
}

export default function UsersPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState<{ page: number; rows: number; username: string; status?: number }>({ page: 0, rows: 20, username: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<AuthUserSave>(defaultForm)
  const [deleteTarget, setDeleteTarget] = useState<AuthUser | null>(null)
  const [banTarget, setBanTarget] = useState<AuthUser | null>(null)
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['auth-users', search],
    queryFn: () => authApi.list(search),
  })
  const rows = normalizeRows<AuthUser>(data)
  const total = readTotal(data, rows.length)
  const enabledCount = rows.filter(item => item.status === 1).length
  const disabledCount = rows.filter(item => item.status === 0).length
  const saveMut = useMutation({
    mutationFn: (payload: AuthUserSave) => { setActionError(''); return authApi.save(payload) },
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['auth-users'] }) },
    onError: (e, payload) => { setActionError(`${USER_ENDPOINTS.save} 保存失败：${getErrorMessage(e)}（${userSaveContext(payload)}）`); toast('保存失败', 'error') },
  })
  const delMut = useMutation({
    mutationFn: (user: AuthUser) => { setActionError(''); return authApi.delete(user.id) },
    onSuccess: () => { toast('删除成功', 'success'); setDeleteTarget(null); qc.invalidateQueries({ queryKey: ['auth-users'] }) },
    onError: (e, user) => { setActionError(`${USER_ENDPOINTS.delete} 删除失败：${getErrorMessage(e)}（${userContext(user)}）`); toast('删除失败', 'error') },
  })
  const banMut = useMutation({
    mutationFn: (target: AuthUser) => {
      setActionError('')
      return authApi.ban({
        userId: target.id,
        ban: target.status === 1,
        reason: target.status === 1 ? '管理员在用户管理页禁用' : undefined,
      })
    },
    onSuccess: () => { toast('状态已更新', 'success'); setBanTarget(null); qc.invalidateQueries({ queryKey: ['auth-users'] }) },
    onError: (e, target) => {
      const targetStatus = target.status === 1 ? '0/禁用' : '1/启用'
      setActionError(`${USER_ENDPOINTS.ban} 启停失败：${getErrorMessage(e)}（${userContext(target)}; targetStatus=${targetStatus}）`)
      toast('启停失败', 'error')
    },
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setActionError(''); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AuthUser) => { setForm({ id: row.id, username: row.username, nickname: row.nickname ?? '', mobile: row.mobile ?? '', email: row.email ?? '', avatarUrl: row.avatarUrl ?? '', roleCode: row.roleCode ?? '' }); setActionError(''); setFormOpen(true) }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'username', headerName: '用户名', flex: 1 },
    { field: 'nickname', headerName: '昵称', flex: 1 },
    { field: 'email', headerName: '邮箱', flex: 1.5 },
    { field: 'status', headerName: '状态', width: 90, renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" /> },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 190, sortable: false, renderCell: ({ row }) => {
      const user = row as AuthUser
      return (
        <Stack direction="row" gap={1}>
          <Button size="small" onClick={() => openEdit(user)}>编辑</Button>
          <Button size="small" color={user.status === 1 ? 'warning' : 'success'} onClick={() => { setActionError(''); setBanTarget(user) }}>
            {user.status === 1 ? '禁用' : '启用'}
          </Button>
          <Button size="small" color="error" onClick={() => { setActionError(''); setDeleteTarget(user) }}>删除</Button>
        </Stack>
      )
    } },
  ]

  const searchSlot = (
    <>
      <TextField label="用户名" size="small" value={query.username} onChange={e => setQuery(q => ({ ...q, username: e.target.value }))} />
      <TextField
        select
        label="状态"
        size="small"
        value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 120 }}
      >
        {STATUS_OPTIONS.map(option => <MenuItem key={option.label} value={option.value}>{option.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { setQuery({ page: 0, rows: 20, username: '' }); setSearch({ page: 0, rows: 20, username: '' }) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增用户</Button>
  )

  const listErrorMessage = error instanceof Error
    ? `${USER_ENDPOINTS.search} 用户列表加载失败：${error.message}（${userFilterContext(search)}）`
    : `${USER_ENDPOINTS.search} 用户列表加载失败（${userFilterContext(search)}）`

  return (
    <Box
      data-testid="auth-users-workbench"
      data-contract-scope="auth-users"
      data-ready-endpoints="/auth/user/search,/auth/user/save,/auth/user/delete,/auth/user/ban"
      data-unsupported-actions="user-export,password-reset,online-kick,role-options-autoload"
      data-no-local-user-fallback="true"
      data-row-retained-on-action-error="true"
      data-no-local-status-mutation="true"
      data-no-password-echo="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="用户管理"
        subtitle="管理员通过 /auth/user/search 管理账号、角色编码和启停状态。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        data-testid="auth-users-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-local-status-mutation="true"
        data-no-password-echo="true"
      >
        用户保存接口只接收账号资料、角色编码和密码；启停状态应走封禁/解封接口，页面不伪造本地状态变更。
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={4}><MetricCard label="用户总数" value={total} helper="后端分页 total" /></Grid>
        <Grid item xs={12} sm={4}><MetricCard label="本页启用" value={enabledCount} helper="当前页 status=1" /></Grid>
        <Grid item xs={12} sm={4}><MetricCard label="本页禁用" value={disabledCount} helper="当前页 status=0" /></Grid>
      </Grid>

      {isError && (
        <Box data-testid="auth-users-list-error" data-no-local-user-fallback="true">
          <ErrorAlert title="用户列表加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {actionError ? (
        <Alert severity="error" data-testid="auth-users-action-error" data-row-retained-on-action-error="true" data-no-local-status-mutation="true">
          {actionError}。失败不会关闭表单或移除用户行。
        </Alert>
      ) : null}

      <StandardDataGrid
        rows={rows}
        columns={columns}
        rowCount={total}
        loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
        actionSlot={actionSlot}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={form.id ? '编辑用户' : '新增用户'} onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="用户名" value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} fullWidth />
          <TextField label="昵称" value={form.nickname ?? ''} onChange={e => setForm(f => ({ ...f, nickname: e.target.value }))} fullWidth />
          <TextField label="手机号" value={form.mobile ?? ''} onChange={e => setForm(f => ({ ...f, mobile: e.target.value }))} fullWidth />
          <TextField label="邮箱" value={form.email ?? ''} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} fullWidth />
          <TextField label="头像URL" value={form.avatarUrl ?? ''} onChange={e => setForm(f => ({ ...f, avatarUrl: e.target.value }))} fullWidth />
          <TextField label="角色编码" value={form.roleCode ?? ''} onChange={e => setForm(f => ({ ...f, roleCode: e.target.value }))} fullWidth />
          {!form.id && <TextField label="密码" type="password" value={form.password ?? ''} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} fullWidth />}
          {actionError ? <Alert severity="error" data-testid="auth-users-save-error" data-input-retained="true" data-no-password-echo="true">{actionError}。保存失败会保留当前输入。</Alert> : null}
        </Stack>
      </FormDialog>
      <ConfirmDialog
        open={deleteTarget !== null}
        content={actionError.startsWith(USER_ENDPOINTS.delete) ? `${actionError}。失败不会移除用户行。` : `确定要删除该用户吗？endpoint=${USER_ENDPOINTS.delete}; ${userContext(deleteTarget)}。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending}
      />
      <ConfirmDialog
        open={banTarget !== null}
        content={actionError.startsWith(USER_ENDPOINTS.ban) ? `${actionError}。失败不会切换当前用户状态。` : `确定要${banTarget?.status === 1 ? '禁用' : '启用'}该用户吗？endpoint=${USER_ENDPOINTS.ban}; ${userContext(banTarget)}。`}
        onClose={() => setBanTarget(null)}
        onConfirm={() => banTarget && banMut.mutate(banTarget)}
        loading={banMut.isPending}
      />
    </Box>
  )
}
