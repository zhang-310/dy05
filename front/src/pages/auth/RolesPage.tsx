import { useState, useCallback } from 'react'
import { Alert, Box, Card, CardContent, Grid, MenuItem, TextField, Button, Chip, Stack, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { authApi, type AuthRole, type AuthRoleSave } from '@/api/auth'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const defaultForm: Partial<AuthRoleSave> = { roleName: '', roleCode: '', sortOrder: 0, status: 1 }
const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
]
const ROLES_ROUTE = '/admin/auth/roles'
const ROLE_ENDPOINTS = {
  search: '/auth/role/search',
  save: '/auth/role/save',
  delete: '/auth/role/delete',
} as const

function roleContext(role?: Partial<AuthRole> | null, fallbackId?: number | string) {
  return `route=${ROLES_ROUTE}; roleId=${role?.id ?? fallbackId ?? '新增'}; roleName=${role?.roleName || '未填写'}; roleCode=${role?.roleCode || '未填写'}; status=${role?.status ?? '-'}`
}

function roleSaveContext(payload?: Partial<AuthRoleSave> | null) {
  return `route=${ROLES_ROUTE}; roleId=${payload?.id ?? '新增'}; roleName=${payload?.roleName || '未填写'}; roleCode=${payload?.roleCode || '未填写'}; status=${payload?.status ?? 1}`
}

function roleFilterContext(search: { page: number; rows: number; roleName: string; status?: number }) {
  return `route=${ROLES_ROUTE}; roleName=${search.roleName.trim() || '空'}; status=${search.status ?? '全部'}; page=${search.page}; rows=${search.rows}`
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

export default function RolesPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState<{ page: number; rows: number; roleName: string; status?: number }>({ page: 0, rows: 20, roleName: '' })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<AuthRoleSave>>(defaultForm)
  const [deleteTarget, setDeleteTarget] = useState<AuthRole | null>(null)
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['auth-roles', search],
    queryFn: () => authApi.roleList(search),
  })
  const rows = normalizeRows<AuthRole>(data)
  const total = readTotal(data, rows.length)
  const enabledCount = rows.filter(item => item.status === 1).length
  const disabledCount = rows.filter(item => item.status === 0).length
  const saveMut = useMutation({
    mutationFn: (payload: Partial<AuthRoleSave>) => { setActionError(''); return authApi.roleSave(payload) },
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['auth-roles'] }) },
    onError: (e, payload) => { setActionError(`${ROLE_ENDPOINTS.save} 保存失败：${getErrorMessage(e)}（${roleSaveContext(payload)}）`); toast('保存失败', 'error') },
  })
  const delMut = useMutation({
    mutationFn: (role: AuthRole) => { setActionError(''); return authApi.roleDelete(role.id) },
    onSuccess: () => { toast('删除成功', 'success'); setDeleteTarget(null); qc.invalidateQueries({ queryKey: ['auth-roles'] }) },
    onError: (e, role) => { setActionError(`${ROLE_ENDPOINTS.delete} 删除失败：${getErrorMessage(e)}（${roleContext(role)}）`); toast('删除失败', 'error') },
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setActionError(''); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AuthRole) => {
    setForm({ id: row.id, roleName: row.roleName, roleCode: row.roleCode, sortOrder: row.sortOrder ?? 0, status: row.status })
    setActionError('')
    setFormOpen(true)
  }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])
  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'roleName', headerName: '角色名称', flex: 1, minWidth: 140 },
    { field: 'roleCode', headerName: '角色编码', width: 160 },
    { field: 'sortOrder', headerName: '排序', width: 90 },
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
          <Button size="small" color="error" onClick={() => { setActionError(''); setDeleteTarget(row as AuthRole) }}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField size="small" label="角色名称" value={query.roleName}
        onChange={e => setQuery(q => ({ ...q, roleName: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 180 }} />
      <TextField
        select
        size="small"
        label="状态"
        value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 120 }}
      >
        {STATUS_OPTIONS.map(option => <MenuItem key={option.label} value={option.value}>{option.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
      <Button size="small" onClick={() => { setQuery({ page: 0, rows: 20, roleName: '' }); setSearch({ page: 0, rows: 20, roleName: '' }) }}>重置</Button>
    </Stack>
  )
  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建角色</Button>
  )

  const listErrorMessage = error instanceof Error
    ? `${ROLE_ENDPOINTS.search} 角色列表加载失败：${error.message}（${roleFilterContext(search)}）`
    : `${ROLE_ENDPOINTS.search} 角色列表加载失败（${roleFilterContext(search)}）`

  return (
    <Box
      data-testid="auth-roles-workbench"
      data-contract-scope="auth-roles"
      data-ready-endpoints="/auth/role/search,/auth/role/save,/auth/role/delete"
      data-degraded-endpoints="/auth/role/resources,/auth/role/resources/save"
      data-unsupported-actions="role-export,inline-resource-assignment,description-field"
      data-no-local-role-fallback="true"
      data-row-retained-on-action-error="true"
      data-no-inline-resource-assignment="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="角色管理"
        subtitle="管理员通过 /auth/role/search 管理角色编码、排序和启停状态。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        data-testid="auth-roles-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-inline-resource-assignment="true"
      >
        角色表当前无 description 字段，页面只展示真实落库字段；资源授权走 /auth/role/resources 与 /auth/role/resources/save。
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={4}><MetricCard label="角色总数" value={total} helper="后端分页 total" /></Grid>
        <Grid item xs={12} sm={4}><MetricCard label="本页启用" value={enabledCount} helper="当前页 status=1" /></Grid>
        <Grid item xs={12} sm={4}><MetricCard label="本页禁用" value={disabledCount} helper="当前页 status=0" /></Grid>
      </Grid>

      {isError && (
        <Box data-testid="auth-roles-list-error" data-no-local-role-fallback="true">
          <ErrorAlert title="角色列表加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {actionError ? (
        <Alert severity="error" data-testid="auth-roles-action-error" data-row-retained-on-action-error="true">
          {actionError}。失败不会关闭表单或移除角色行。
        </Alert>
      ) : null}

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
      />
      <FormDialog open={formOpen} title={form.id ? '编辑角色' : '新建角色'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="角色名称" required value={form.roleName ?? ''} onChange={e => setForm(f => ({ ...f, roleName: e.target.value }))} fullWidth size="small" />
          <TextField label="角色编码" required value={form.roleCode ?? ''} onChange={e => setForm(f => ({ ...f, roleCode: e.target.value }))} fullWidth size="small" />
          <TextField label="排序" type="number" value={form.sortOrder ?? 0} onChange={e => setForm(f => ({ ...f, sortOrder: Number(e.target.value) }))} fullWidth size="small" />
          <TextField select label="状态" value={form.status ?? 1} onChange={e => setForm(f => ({ ...f, status: Number(e.target.value) }))} fullWidth size="small">
            <MenuItem value={1}>启用</MenuItem>
            <MenuItem value={0}>禁用</MenuItem>
          </TextField>
          {actionError ? <Alert severity="error" data-testid="auth-roles-save-error" data-input-retained="true">{actionError}。保存失败会保留当前输入。</Alert> : null}
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteTarget !== null}
        content={actionError.startsWith(ROLE_ENDPOINTS.delete) ? `${actionError}。失败不会移除角色行。` : `确定要删除该角色吗？endpoint=${ROLE_ENDPOINTS.delete}; ${roleContext(deleteTarget)}。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending} />
    </Box>
  )
}
