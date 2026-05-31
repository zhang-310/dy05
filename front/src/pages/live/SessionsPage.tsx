import { useState, useCallback } from 'react'
import {
  Alert, Box, Button, TextField, Select, MenuItem, FormControl, InputLabel,
  Chip, IconButton, Tooltip, Dialog, DialogTitle, DialogContent,
  DialogActions, Stack, SelectChangeEvent, Typography, Grid, Paper,
} from '@mui/material'
import { GridColDef, GridRenderCellParams, GridPaginationModel, GridRowSelectionModel } from '@mui/x-data-grid'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import StopIcon from '@mui/icons-material/Stop'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import BuildIcon from '@mui/icons-material/Build'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router-dom'
import { liveApi, LiveSession, LiveSessionSave } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { ConfirmDialog, PageHeader, StandardDataGrid, TableSkeleton, EmptyState } from '@/components/base'
import { formatDate } from '@/utils/date'
import { inferLiveRouteScope, liveScopeLabel, liveSessionPath } from './liveRouteScope'
import { getErrorMessage } from '@/utils/errorHandler'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' | 'error' | 'info' | 'primary' | 'secondary' }> = {
  0: { label: '待开播', color: 'default' },
  1: { label: '直播中', color: 'success' },
  2: { label: '已结束', color: 'default' },
}

const SCRIPT_STYLE_OPTIONS = ['专业', '友好', '激情', '种草', '促销']
const SESSION_TYPE_OPTIONS = ['普通', '品牌专场', '大促']
const LIVE_FORMAT_OPTIONS = ['单人', '多人', '连麦']
const SESSION_ENDPOINTS = {
  search: '/live/session/search',
  save: '/live/session/save',
  delete: '/live/session/delete',
  status: '/live/session/status',
  clone: '/live/session/clone',
} as const

const SESSION_READY_ENDPOINTS = {
  ...SESSION_ENDPOINTS,
}

const SESSION_CONTEXT_ENDPOINTS = [
  SESSION_READY_ENDPOINTS.search,
  SESSION_READY_ENDPOINTS.save,
  SESSION_READY_ENDPOINTS.delete,
  SESSION_READY_ENDPOINTS.status,
  SESSION_READY_ENDPOINTS.clone,
]

const SESSION_UNSUPPORTED_ACTIONS = [
  'list-shortvideo-export',
  'list-realtime-panel-direct',
  'list-readiness-fetch',
  'list-product-fetch',
  'list-script-fetch',
  'local-session-fallback',
  'server-export',
]

export function LiveSessionGmvCell({ value }: { value?: number | null }) {
  const amount = Number(value ?? 0)
  if (!amount) {
    return (
      <Box
        component="span"
        data-testid="live-session-gmv-empty-surface"
        data-gmv-tone="muted"
        sx={{ color: 'text.disabled' }}
      >
        -
      </Box>
    )
  }

  return (
    <Box
      component="span"
      data-testid="live-session-gmv-value-surface"
      data-gmv-tone="success"
      sx={(theme) => ({
        color: theme.palette.success[theme.palette.mode === 'dark' ? 'light' : 'dark'],
        fontWeight: 700,
      })}
    >
      {amount >= 10000 ? `¥${(amount / 10000).toFixed(1)}万` : `¥${amount}`}
    </Box>
  )
}

interface FormState {
  id?: number; liveTitle: string; scriptStyle: string; sessionType: string
  liveFormat: string; scheduledTime: string; liveDescription: string
  accountId: string; personaId: string
}

const defaultForm = (): FormState => ({
  liveTitle: '', scriptStyle: '专业', sessionType: '普通',
  liveFormat: '单人', scheduledTime: '', liveDescription: '',
  accountId: '', personaId: '',
})

export default function SessionsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const routeScope = inferLiveRouteScope(useLocation().pathname)
  const isAdminShell = routeScope === 'admin'
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 })
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<FormState>(defaultForm())
  const [saving, setSaving] = useState(false)
  const [selection, setSelection] = useState<GridRowSelectionModel>([])
  const [deleteTarget, setDeleteTarget] = useState<LiveSession | null>(null)
  const [operationError, setOperationError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['live-sessions', pagination.page, pagination.pageSize, keyword, statusFilter],
    queryFn: () => liveApi.sessionSearch({
      page: pagination.page,
      rows: pagination.pageSize,
      keyword: keyword || undefined,
      status: statusFilter !== '' ? Number(statusFilter) : undefined,
    }),
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0
  const liveCount = rows.filter(r => r.status === 1).length
  const pendingCount = rows.filter(r => r.status === 0).length
  const endedCount = rows.filter(r => r.status === 2).length
  const totalGmv = rows.reduce((sum, row) => sum + Number(row.totalGmv ?? row.cumulativeGmv ?? row.totalRevenue ?? 0), 0)

  const invalidate = useCallback(() => qc.invalidateQueries({ queryKey: ['live-sessions'] }), [qc])

  const selectedIds = () => (selection as Array<number | string>).map(id => Number(id)).filter(id => Number.isFinite(id))
  const selectedRows = () => selectedIds().map(id => rows.find(row => Number(row.id) === id)).filter((row): row is LiveSession => !!row)
  const filterContext = () => {
    const statusText = statusFilter === ''
      ? '全部'
      : `${statusFilter}/${STATUS_MAP[Number(statusFilter)]?.label ?? '未知'}`
    return `scope=${routeScope}; shell=${liveScopeLabel(routeScope)}; keyword=${keyword.trim() || '空'}; statusFilter=${statusText}; page=${pagination.page}; rows=${pagination.pageSize}`
  }
  const sessionContext = (row?: LiveSession | null, fallbackId?: number | string) => {
    const id = row?.id ?? fallbackId ?? '-'
    return `sessionId=${id}; liveTitle=${row?.liveTitle || '未知场次'}; accountId=${row?.accountId ?? '-'}; personaId=${row?.personaId ?? '-'}; ${filterContext()}`
  }
  const batchContext = (ids: number[], batchRows: LiveSession[]) => {
    const titles = batchRows.map(row => `${row.id}:${row.liveTitle}`).join(',') || '未命中当前页行'
    return `selectedIds=${ids.join(',') || '空'}; selectedTitles=${titles}; ${filterContext()}`
  }
  const formContext = () => (
    `scope=${routeScope}; shell=${liveScopeLabel(routeScope)}; sessionId=${form.id ?? 'new'}; liveTitle=${form.liveTitle.trim() || '未填写'}; accountId=${form.accountId || '-'}; personaId=${form.personaId || '-'}`
  )
  const isNullPayloadSuccessError = (e: unknown) => getErrorMessage(e).includes('资源或者信息为空')
  const isDeleteAlreadyAppliedError = (e: unknown) => {
    const message = getErrorMessage(e)
    return message.includes('资源或者信息为空') || message.includes('直播场次不存在') || message.includes('场次不存在')
  }
  const runVoidMutation = async (action: () => Promise<unknown>) => {
    try {
      await action()
      return false
    } catch (e: unknown) {
      if (isNullPayloadSuccessError(e)) return true
      throw e
    }
  }
  const deleteSessionIdempotently = async (id: number) => {
    try {
      await liveApi.sessionDelete(id)
      return { id, deleted: true, alreadyAbsent: false }
    } catch (e: unknown) {
      if (isDeleteAlreadyAppliedError(e)) return { id, deleted: false, alreadyAbsent: true }
      throw e
    }
  }

  const deleteMut = useMutation({
    mutationFn: (row: LiveSession) => deleteSessionIdempotently(row.id),
    onMutate: () => setOperationError(null),
    onSuccess: (result) => {
      toast(result.alreadyAbsent ? '场次已不存在，列表已刷新' : '删除成功', 'success')
      invalidate()
      setDeleteTarget(null)
    },
    onError: (e: unknown, row) => {
      setOperationError(`${SESSION_ENDPOINTS.delete} 删除场次失败：${getErrorMessage(e)}（${sessionContext(row)}）`)
      toast('删除失败', 'error')
    },
  })
  const startMut = useMutation({
    mutationFn: (row: LiveSession) => runVoidMutation(() => liveApi.sessionStart(row.id)),
    onMutate: () => setOperationError(null),
    onSuccess: () => { toast('已开播', 'success'); invalidate() },
    onError: (e: unknown, row) => {
      setOperationError(`${SESSION_ENDPOINTS.status} 开播失败：${getErrorMessage(e)}（${sessionContext(row)}）`)
      toast('操作失败', 'error')
    },
  })
  const endMut = useMutation({
    mutationFn: (row: LiveSession) => runVoidMutation(() => liveApi.sessionEnd(row.id)),
    onMutate: () => setOperationError(null),
    onSuccess: () => { toast('已结束', 'success'); invalidate() },
    onError: (e: unknown, row) => {
      setOperationError(`${SESSION_ENDPOINTS.status} 结束场次失败：${getErrorMessage(e)}（${sessionContext(row)}）`)
      toast('操作失败', 'error')
    },
  })
  const cloneMut = useMutation({
    mutationFn: (row: LiveSession) => liveApi.sessionClone(row.id),
    onMutate: () => setOperationError(null),
    onSuccess: () => { toast('克隆成功', 'success'); invalidate() },
    onError: (e: unknown, row) => {
      setOperationError(`${SESSION_ENDPOINTS.clone} 克隆场次失败：${getErrorMessage(e)}（${sessionContext(row)}）`)
      toast('克隆失败', 'error')
    },
  })

  const batchDeleteMut = useMutation({
    mutationFn: async ({ ids }: { ids: number[]; batchRows: LiveSession[] }) => {
      const results = await Promise.all(ids.map(id =>
        deleteSessionIdempotently(id)
          .then(result => ({ ...result, ok: true as const }))
          .catch((error: unknown) => ({ id, ok: false as const, error })),
      ))
      const failed = results.filter(result => !result.ok)
      if (failed.length > 0) {
        const details = failed.map(result => `${result.id}:${getErrorMessage(result.error)}`).join('；')
        throw new Error(`失败 ${failed.length}/${ids.length}：${details}`)
      }
      return {
        deletedCount: results.filter(result => result.ok && result.deleted).length,
        alreadyAbsentCount: results.filter(result => result.ok && result.alreadyAbsent).length,
      }
    },
    onMutate: () => setOperationError(null),
    onSuccess: (result) => {
      const suffix = result.alreadyAbsentCount > 0 ? `，${result.alreadyAbsentCount} 条此前已删除` : ''
      toast(`批量删除成功${suffix}`, 'success')
      invalidate()
      setSelection([])
    },
    onError: (e: unknown, payload) => {
      setOperationError(`${SESSION_ENDPOINTS.delete} 批量删除失败：${getErrorMessage(e)}（${batchContext(payload.ids, payload.batchRows)}）`)
      toast('批量删除失败', 'error')
    },
  })

  const batchStartMut = useMutation({
    mutationFn: async ({ ids }: { ids: number[]; batchRows: LiveSession[] }) => {
      await Promise.all(ids.map(id => runVoidMutation(() => liveApi.sessionStart(id))))
    },
    onMutate: () => setOperationError(null),
    onSuccess: () => { toast('批量开播成功', 'success'); invalidate(); setSelection([]) },
    onError: (e: unknown, payload) => {
      setOperationError(`${SESSION_ENDPOINTS.status} 批量开播失败：${getErrorMessage(e)}（${batchContext(payload.ids, payload.batchRows)}）`)
      toast('批量开播失败', 'error')
    },
  })

  const batchEndMut = useMutation({
    mutationFn: async ({ ids }: { ids: number[]; batchRows: LiveSession[] }) => {
      await Promise.all(ids.map(id => runVoidMutation(() => liveApi.sessionEnd(id))))
    },
    onMutate: () => setOperationError(null),
    onSuccess: () => { toast('批量结束成功', 'success'); invalidate(); setSelection([]) },
    onError: (e: unknown, payload) => {
      setOperationError(`${SESSION_ENDPOINTS.status} 批量结束失败：${getErrorMessage(e)}（${batchContext(payload.ids, payload.batchRows)}）`)
      toast('批量结束失败', 'error')
    },
  })

  const handleSearch = () => { setPagination(p => ({ ...p, page: 0 })); refetch() }
  const handleReset = () => { setKeyword(''); setStatusFilter(''); setPagination(p => ({ ...p, page: 0 })) }

  const openCreate = () => { setForm(defaultForm()); setSaveError(null); setDialogOpen(true) }
  const openEdit = (row: LiveSession) => {
    setSaveError(null)
    setForm({
      id: row.id, liveTitle: row.liveTitle, scriptStyle: row.scriptStyle || '专业',
      sessionType: row.sessionType || '普通', liveFormat: row.liveFormat || '单人',
      scheduledTime: row.scheduledTime ? row.scheduledTime.slice(0, 16) : '',
      liveDescription: row.liveDescription || '',
      accountId: String(row.accountId || ''), personaId: String(row.personaId || ''),
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!form.liveTitle.trim()) { toast('请输入场次标题', 'warning'); return }
    setSaving(true)
    setSaveError(null)
    try {
      const payload: Partial<LiveSessionSave> = {
        id: form.id,
        liveTitle: form.liveTitle,
        scriptStyle: form.scriptStyle,
        sessionType: form.sessionType,
        liveFormat: form.liveFormat,
        scheduledTime: form.scheduledTime || undefined,
        liveDescription: form.liveDescription || undefined,
        accountId: form.accountId ? Number(form.accountId) : undefined,
        personaId: form.personaId ? Number(form.personaId) : undefined,
      }
      await liveApi.sessionSave(payload)
      toast(form.id ? '更新成功' : '创建成功', 'success')
      setDialogOpen(false); invalidate()
    } catch (e: unknown) {
      setSaveError(`${SESSION_ENDPOINTS.save} 保存场次失败：${getErrorMessage(e)}（${formContext()}）`)
      toast('保存失败', 'error')
    } finally { setSaving(false) }
  }

  const columns: GridColDef[] = [
    { field: 'liveTitle', headerName: '场次标题', flex: 1, minWidth: 180 },
    {
      field: 'status', headerName: '状态', width: 92,
      renderCell: (p: GridRenderCellParams) => {
        const s = STATUS_MAP[p.value as number] ?? { label: '未知', color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    { field: 'sessionType', headerName: '场次类型', width: 100 },
    { field: 'liveFormat', headerName: '直播形式', width: 90 },
    { field: 'scheduledTime', headerName: '预定时间', width: 145,
      valueFormatter: (v: string) => v ? v.replace('T', ' ').slice(0, 16) : '-' },
    { field: 'totalGmv', headerName: 'GMV', width: 95, type: 'number',
      renderCell: (p: GridRenderCellParams) => {
        return <LiveSessionGmvCell value={p.value as number} />
      } },
    { field: 'viewers', headerName: '观看', width: 75, type: 'number' },
    { field: 'likes', headerName: '点赞', width: 70, type: 'number' },
    { field: 'createTime', headerName: '创建时间', width: 145, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 184, sortable: false,
      renderCell: (p: GridRenderCellParams<LiveSession>) => {
        const row = p.row
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="进入工作台">
              <IconButton size="small" color="primary" onClick={() => navigate(liveSessionPath(routeScope, row.id))}>
                <BuildIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {row.status === 0 && (
              <Tooltip title="开播"><IconButton size="small" color="success" onClick={() => startMut.mutate(row)}><PlayArrowIcon fontSize="small" /></IconButton></Tooltip>
            )}
            {row.status === 1 && (
              <Tooltip title="结束"><IconButton size="small" color="warning" onClick={() => endMut.mutate(row)}><StopIcon fontSize="small" /></IconButton></Tooltip>
            )}
            <Tooltip title="克隆"><IconButton size="small" onClick={() => cloneMut.mutate(row)}><ContentCopyIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="编辑"><IconButton size="small" onClick={() => openEdit(row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
            {isAdminShell && (
              <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteTarget(row)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
            )}
          </Stack>
        )
      },
    },
  ]

  const searchSlot = (
    <Box
      data-testid="live-sessions-filter-panel"
      data-contract-source={SESSION_READY_ENDPOINTS.search}
      data-scope={routeScope}
      data-keyword={keyword.trim() || 'empty'}
      data-status-filter={statusFilter || 'all'}
      data-page={pagination.page}
      data-page-size={pagination.pageSize}
      data-no-local-session-fallback="true"
      sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', minWidth: 0 }}
    >
      <TextField label="关键词" value={keyword} onChange={e => setKeyword(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} size="small" sx={{ width: 200 }} />
      <FormControl size="small" sx={{ width: 120 }}>
        <InputLabel>状态</InputLabel>
        <Select value={statusFilter} label="状态" onChange={(e: SelectChangeEvent) => setStatusFilter(e.target.value)}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="0">待开播</MenuItem>
          <MenuItem value="1">直播中</MenuItem>
          <MenuItem value="2">已结束</MenuItem>
        </Select>
      </FormControl>
      <Button variant="contained" onClick={handleSearch}>搜索</Button>
      <Button onClick={handleReset}>重置</Button>
      {isAdminShell && selection.length > 0 && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ ml: 2 }}>
            已选 {selection.length} 条
          </Typography>
          <Button
            size="small"
            variant="outlined"
            startIcon={<PlayArrowIcon />}
            disabled={batchStartMut.isPending || batchEndMut.isPending || batchDeleteMut.isPending}
            data-testid="live-sessions-batch-start-button"
            onClick={() => batchStartMut.mutate({ ids: selectedIds(), batchRows: selectedRows() })}
          >
            {batchStartMut.isPending ? '批量开播中...' : '批量开播'}
          </Button>
          <Button
            size="small"
            variant="outlined"
            startIcon={<StopIcon />}
            disabled={batchStartMut.isPending || batchEndMut.isPending || batchDeleteMut.isPending}
            data-testid="live-sessions-batch-end-button"
            onClick={() => batchEndMut.mutate({ ids: selectedIds(), batchRows: selectedRows() })}
          >
            {batchEndMut.isPending ? '批量结束中...' : '批量结束'}
          </Button>
          <Button
            size="small"
            variant="outlined"
            color="error"
            startIcon={<DeleteIcon />}
            disabled={batchStartMut.isPending || batchEndMut.isPending || batchDeleteMut.isPending}
            data-testid="live-sessions-batch-delete-button"
            onClick={() => batchDeleteMut.mutate({ ids: selectedIds(), batchRows: selectedRows() })}
          >
            {batchDeleteMut.isPending ? '批量删除中...' : '批量删除'}
          </Button>
        </>
      )}
    </Box>
  )

  const actionSlot = (
    <Button
      variant="contained"
      startIcon={<AddIcon />}
      onClick={openCreate}
      data-testid="live-sessions-create-button"
      data-contract-source={SESSION_READY_ENDPOINTS.save}
    >
      新建场次
    </Button>
  )

  return (
    <Box
      sx={{ p: 3, minHeight: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}
      data-testid="live-sessions-workbench"
      data-contract-scope="live-session-list"
      data-ready-endpoints={Object.values(SESSION_READY_ENDPOINTS).join('|')}
      data-context-endpoints={SESSION_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SESSION_UNSUPPORTED_ACTIONS.join('|')}
      data-route-scope={routeScope}
      data-is-admin-shell={String(isAdminShell)}
      data-row-count={rows.length}
      data-total-count={total}
      data-selected-count={selection.length}
      data-keyword={keyword.trim() || 'empty'}
      data-status-filter={statusFilter || 'all'}
      data-no-local-session-fallback="true"
    >
      <PageHeader
        title={routeScope === 'admin' ? '直播场次' : `${liveScopeLabel(routeScope)}直播场次`}
        subtitle={
          routeScope === 'admin'
            ? '场次是直播工作台的入口；这里可以确认开播状态、GMV 数据口径、工作台入口和实时面板链路。'
            : '当前在角色壳内复用直播场次列表，工作台入口会留在当前壳；实时面板、系统级管理动作仍在管理员端。'
        }
        breadcrumbs={[{ label: liveScopeLabel(routeScope) }, { label: '直播场次' }]}
        actions={actionSlot}
      />
      {routeScope !== 'admin' ? (
        <Alert
          severity="info"
          variant="outlined"
          sx={{ mb: 2 }}
          data-testid="live-sessions-role-shell-alert"
          data-route-scope={routeScope}
          data-no-admin-batch-actions="true"
          data-no-admin-delete="true"
          data-workbench-path-template={routeScope === 'org' ? '/org/live/sessions/:id' : '/talent/live/sessions/:id'}
        >
          当前是 {liveScopeLabel(routeScope)} 路由。进入工作台会跳转到 {routeScope === 'org' ? '/org/live/sessions/:id' : '/talent/live/sessions/:id'}，避免误回 `/admin/live/sessions/:id`。
          批量开播、批量结束、批量删除和删除场次只在管理员端展示；当前壳保留查看、编辑、开播、结束和克隆。
        </Alert>
      ) : null}
      {isError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-sessions-list-error"
          data-contract-source={SESSION_READY_ENDPOINTS.search}
          data-no-local-session-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          直播场次加载失败：POST {SESSION_ENDPOINTS.search}：{getErrorMessage(error)}（{filterContext()}）。
          降级策略：已保留筛选条件，后端恢复后点击重试即可。
        </Alert>
      ) : null}
      {operationError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setOperationError(null)}
          data-testid="live-sessions-operation-error"
          data-no-local-session-mutation="true"
        >
          {operationError}
        </Alert>
      ) : null}

      <Grid
        container
        spacing={1.5}
        sx={{ mb: 2 }}
        data-testid="live-sessions-kpi-grid"
        data-contract-source={SESSION_READY_ENDPOINTS.search}
        data-live-count={liveCount}
        data-pending-count={pendingCount}
        data-ended-count={endedCount}
        data-total-gmv={totalGmv}
      >
        {[
          { label: '当前页场次', value: total, desc: `待开播 ${pendingCount} / 直播中 ${liveCount} / 已结束 ${endedCount}` },
          { label: '直播中', value: liveCount, desc: liveCount > 0 ? '可进入实时面板跟播' : '暂无进行中场次' },
          { label: '当前页 GMV', value: totalGmv > 0 ? `¥${totalGmv.toLocaleString()}` : '暂无', desc: '来自场次列表返回的 GMV/累计 GMV 字段' },
        ].map(item => (
          <Grid item xs={12} md={4} key={item.label}>
            <Paper
              variant="outlined"
              sx={{ p: 1.5, height: '100%' }}
              data-testid="live-sessions-kpi-card"
              data-kpi-label={item.label}
              data-contract-source={SESSION_READY_ENDPOINTS.search}
            >
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.desc}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      {isFetching && rows.length === 0 ? (
        <TableSkeleton rows={10} columns={7} />
      ) : rows.length === 0 && !keyword && !statusFilter ? (
        <Box
          data-testid="live-sessions-empty-state"
          data-contract-source={SESSION_READY_ENDPOINTS.search}
          data-no-local-session-fallback="true"
        >
          <EmptyState
            title="还没有直播场次"
            description="创建第一个直播场次，开始您的直播运营之旅"
            action={{
              text: '新建场次',
              onClick: openCreate,
            }}
          />
        </Box>
      ) : (
        <Box
          sx={{ height: { xs: 520, md: 'max(420px, calc(100vh - 420px))' }, minHeight: 420 }}
          data-testid="live-sessions-table-surface"
          data-contract-source={SESSION_READY_ENDPOINTS.search}
          data-row-count={rows.length}
          data-total-count={total}
          data-checkbox-selection={String(isAdminShell)}
          data-no-server-export="true"
        >
          <StandardDataGrid
            rows={rows}
            columns={columns}
            loading={isFetching}
            rowCount={total}
            paginationMode="server"
            paginationModel={pagination}
            onPaginationModelChange={setPagination}
            checkboxSelection={isAdminShell}
            rowSelectionModel={selection}
            onRowSelectionModelChange={setSelection}
            searchSlot={searchSlot}
            showExport={false}
            sx={{ height: '100%' }}
          />
        </Box>
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'live-sessions-save-dialog',
          'data-contract-source': SESSION_READY_ENDPOINTS.save,
          'data-session-id': form.id ?? 'new',
          'data-route-scope': routeScope,
          'data-input-retained': saveError ? 'true' : 'false',
        } as never}
      >
        <DialogTitle>{form.id ? '编辑场次' : '新建场次'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {saveError ? (
              <Alert
                severity="error"
                data-testid="live-sessions-save-error"
                data-contract-source={SESSION_READY_ENDPOINTS.save}
                data-input-retained="true"
                data-no-local-session-mutation="true"
              >
                {saveError}
              </Alert>
            ) : null}
            <TextField label="场次标题" value={form.liveTitle} onChange={e => setForm(f => ({ ...f, liveTitle: e.target.value }))} fullWidth size="small" required />
            <TextField label="账号ID" value={form.accountId} onChange={e => setForm(f => ({ ...f, accountId: e.target.value }))} fullWidth size="small" />
            <TextField label="人设ID" value={form.personaId} onChange={e => setForm(f => ({ ...f, personaId: e.target.value }))} fullWidth size="small" />
            <FormControl fullWidth size="small">
              <InputLabel>话术风格</InputLabel>
              <Select value={form.scriptStyle} label="话术风格" onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, scriptStyle: e.target.value }))}>
                {SCRIPT_STYLE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>场次类型</InputLabel>
              <Select value={form.sessionType} label="场次类型" onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, sessionType: e.target.value }))}>
                {SESSION_TYPE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>直播形式</InputLabel>
              <Select value={form.liveFormat} label="直播形式" onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, liveFormat: e.target.value }))}>
                {LIVE_FORMAT_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="预定开始时间" type="datetime-local" value={form.scheduledTime}
              onChange={e => setForm(f => ({ ...f, scheduledTime: e.target.value }))}
              fullWidth size="small" InputLabelProps={{ shrink: true }} />
            <TextField label="场次描述" value={form.liveDescription}
              onChange={e => setForm(f => ({ ...f, liveDescription: e.target.value }))}
              fullWidth size="small" multiline rows={3} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>{saving ? '保存中...' : '保存'}</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="删除直播场次"
        content={`确定删除该直播场次吗？endpoint=${SESSION_ENDPOINTS.delete}; ${sessionContext(deleteTarget)}。相关商品、话术、实时数据可能无法继续通过该场次入口查看。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && deleteMut.mutate(deleteTarget)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
