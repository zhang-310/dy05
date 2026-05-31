import { useState, useCallback } from 'react'
import { Alert, Box, Card, CardContent, Grid, Stack, Button, TextField, MenuItem, Chip, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, ErrorAlert, DataGridEmptyOverlay } from '@/components/base'
import { authApi, type AuthResource, type AuthResourceSave } from '@/api/auth'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const RESOURCE_TYPES = ['menu', 'button', 'api']
const defaultForm: AuthResourceSave = { parentId: 0, resourceName: '', resourceCode: '', resourceType: 'menu', requestMethod: '', module: '', sortOrder: 0 }
const RESOURCE_TYPE_OPTIONS = [
  { value: '', label: '全部类型' },
  { value: 'menu', label: 'menu' },
  { value: 'button', label: 'button' },
  { value: 'api', label: 'api' },
]
const RESOURCES_ROUTE = '/admin/auth/resources'
const RESOURCE_ENDPOINTS = {
  list: '/auth/resource/list',
  save: '/auth/resource/save',
  delete: '/auth/resource/delete',
} as const

function resourceContext(resource?: Partial<AuthResource> | null, fallbackId?: number | string) {
  return `route=${RESOURCES_ROUTE}; resourceId=${resource?.id ?? fallbackId ?? '新增'}; resourceName=${resource?.resourceName || '未填写'}; resourceCode=${resource?.resourceCode || '未填写'}; resourceType=${resource?.resourceType || '-'}; module=${resource?.module || '-'}`
}

function resourceSaveContext(payload?: Partial<AuthResourceSave> | null) {
  return `route=${RESOURCES_ROUTE}; resourceId=${payload?.id ?? '新增'}; resourceName=${payload?.resourceName || '未填写'}; resourceCode=${payload?.resourceCode || '未填写'}; resourceType=${payload?.resourceType || '-'}; module=${payload?.module || '-'}`
}

function resourceFilterContext(search: { page: number; rows: number; resourceName: string; module: string; resourceType: string }) {
  return `route=${RESOURCES_ROUTE}; resourceName=${search.resourceName.trim() || '空'}; module=${search.module.trim() || '空'}; resourceType=${search.resourceType || '全部'}; page=${search.page}; rows=${search.rows}`
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

export default function ResourcesPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<AuthResourceSave>(defaultForm)
  const [editId, setEditId] = useState<number | undefined>()
  const [deleteTarget, setDeleteTarget] = useState<AuthResource | null>(null)
  const [query, setQuery] = useState({ page: 0, rows: 50, resourceName: '', module: '', resourceType: '' })
  const [search, setSearch] = useState(query)
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['auth-resources', search],
    queryFn: () => authApi.resourceList({
      ...search,
      resourceName: search.resourceName || undefined,
      module: search.module || undefined,
      resourceType: search.resourceType || undefined,
    }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<AuthResourceSave>) => { setActionError(''); return authApi.resourceSave(p) },
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['auth-resources'] }) },
    onError: (e, payload) => { setActionError(`${RESOURCE_ENDPOINTS.save} 保存失败：${getErrorMessage(e)}（${resourceSaveContext(payload)}）`); toast('保存失败', 'error') },
  })
  const delMut = useMutation({
    mutationFn: (resource: AuthResource) => { setActionError(''); return authApi.resourceDelete(resource.id) },
    onSuccess: () => { toast('删除成功', 'success'); setDeleteTarget(null); qc.invalidateQueries({ queryKey: ['auth-resources'] }) },
    onError: (e, resource) => { setActionError(`${RESOURCE_ENDPOINTS.delete} 删除失败：${getErrorMessage(e)}（${resourceContext(resource)}）`); toast('删除失败', 'error') },
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setEditId(undefined); setActionError(''); setFormOpen(true) }, [])
  const openEdit = useCallback((row: AuthResource) => {
    setForm({ parentId: row.parentId, resourceName: row.resourceName, resourceCode: row.resourceCode, resourceType: row.resourceType, requestMethod: row.requestMethod, module: row.module, sortOrder: row.sortOrder })
    setActionError('')
    setEditId(row.id); setFormOpen(true)
  }, [])

  const rows = normalizeRows<AuthResource>(data)
  const total = readTotal(data, rows.length)
  const menuCount = rows.filter(item => item.resourceType === 'menu').length
  const buttonCount = rows.filter(item => item.resourceType === 'button').length
  const apiCount = rows.filter(item => item.resourceType === 'api').length

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
          <Box component="span" sx={{ color: 'error.main', cursor: 'pointer', fontSize: 13 }} onClick={() => { setActionError(''); setDeleteTarget(row as AuthResource) }}>删除</Box>
        </Stack>
      ),
    },
  ]

  const actionSlot = (
    <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增资源</Button>
  )
  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
      <TextField size="small" label="资源名称" value={query.resourceName}
        onChange={e => setQuery(q => ({ ...q, resourceName: e.target.value }))} sx={{ width: 160 }} />
      <TextField size="small" label="模块" value={query.module}
        onChange={e => setQuery(q => ({ ...q, module: e.target.value }))} sx={{ width: 140 }} />
      <TextField select size="small" label="类型" value={query.resourceType}
        onChange={e => setQuery(q => ({ ...q, resourceType: e.target.value }))} sx={{ width: 120 }}>
        {RESOURCE_TYPE_OPTIONS.map(option => <MenuItem key={option.label} value={option.value}>{option.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button size="small" onClick={() => {
        const reset = { page: 0, rows: 50, resourceName: '', module: '', resourceType: '' }
        setQuery(reset)
        setSearch(reset)
      }}>重置</Button>
    </Stack>
  )

  const listErrorMessage = error instanceof Error
    ? `${RESOURCE_ENDPOINTS.list} 资源列表加载失败：${error.message}（${resourceFilterContext(search)}）`
    : `${RESOURCE_ENDPOINTS.list} 资源列表加载失败（${resourceFilterContext(search)}）`

  return (
    <Box
      data-testid="auth-resources-workbench"
      data-contract-scope="auth-resources"
      data-ready-endpoints="/auth/resource/list,/auth/resource/save,/auth/resource/delete"
      data-degraded-endpoints="/auth/resource/tree,/auth/resource/tree-full"
      data-unsupported-actions="resource-export,inline-tree-editor,permission-preview"
      data-no-local-resource-fallback="true"
      data-no-paged-list-as-tree="true"
      data-row-retained-on-action-error="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="资源管理"
        subtitle="管理员通过 /auth/resource/list 分页维护菜单、按钮和接口资源。"
        actions={(
          <Button size="small" variant="outlined" onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        )}
      />

      <Alert
        severity="info"
        data-testid="auth-resources-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-paged-list-as-tree="true"
      >
        资源列表来自服务器分页；菜单树和完整资源树另由 /auth/resource/tree 与 /auth/resource/tree-full 提供，页面不再把分页结果伪装成全量树。
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={3}><MetricCard label="资源总数" value={total} helper="后端分页 total" /></Grid>
        <Grid item xs={12} sm={3}><MetricCard label="本页菜单" value={menuCount} helper="resourceType=menu" /></Grid>
        <Grid item xs={12} sm={3}><MetricCard label="本页按钮" value={buttonCount} helper="resourceType=button" /></Grid>
        <Grid item xs={12} sm={3}><MetricCard label="本页接口" value={apiCount} helper="resourceType=api" /></Grid>
      </Grid>

      {isError && (
        <Box data-testid="auth-resources-list-error" data-no-local-resource-fallback="true">
          <ErrorAlert title="资源列表加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {actionError ? (
        <Alert severity="error" data-testid="auth-resources-action-error" data-row-retained-on-action-error="true">
          {actionError}。失败不会关闭表单或移除资源行。
        </Alert>
      ) : null}

      <StandardDataGrid
        rows={rows} columns={columns}
        loading={isFetching} paginationMode="server"
        rowCount={total}
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
        actionSlot={actionSlot}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
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
          {actionError ? <Alert severity="error" data-testid="auth-resources-save-error" data-input-retained="true">{actionError}。保存失败会保留当前输入。</Alert> : null}
        </Stack>
      </FormDialog>
      <ConfirmDialog open={deleteTarget !== null}
        content={actionError.startsWith(RESOURCE_ENDPOINTS.delete) ? `${actionError}。失败不会移除资源行。` : `确定要删除该资源吗？endpoint=${RESOURCE_ENDPOINTS.delete}; ${resourceContext(deleteTarget)}。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending} />
    </Box>
  )
}
