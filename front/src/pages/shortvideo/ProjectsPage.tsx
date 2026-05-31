import { useState, useCallback } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Grid, IconButton, MenuItem, Stack, TextField, Tooltip, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { ConfirmDialog, DataGridEmptyOverlay, ErrorAlert, FormDialog, PageHeader, StandardDataGrid } from '@/components/base'
import { shortvideoApi, type SvProject, type SvProjectSave } from '@/api/shortvideo'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router-dom'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

/** 与实体 status 字段一致：draft / processing / completed / failed */
const STATUS_MAP: Record<string, { label: string; color: 'warning' | 'success' | 'default' | 'error' }> = {
  draft: { label: '草稿', color: 'default' },
  processing: { label: '进行中', color: 'warning' },
  completed: { label: '已完成', color: 'success' },
  failed: { label: '失败', color: 'error' },
}

const PROJECT_TYPE_MAP: Record<string, string> = {
  viral_clone: '爆款复刻',
  daily: '日更',
  soft_ad: '软广',
}

const PROJECT_ENDPOINTS = {
  list: '/short-video/project/list',
  save: '/short-video/project/save',
  delete: '/short-video/project/delete',
} as const
const PROJECT_READY_ENDPOINTS = [
  PROJECT_ENDPOINTS.list,
  PROJECT_ENDPOINTS.save,
  PROJECT_ENDPOINTS.delete,
] as const
const PROJECT_UNSUPPORTED_ENDPOINTS = [
  '/short-video/project/mock',
  '/short-video/project/local-list',
  '/short-video/project/local-save',
  '/short-video/project/local-delete',
  '/short-video/project/static-summary',
  '/short-video/workbench/local-route',
] as const
const PROJECT_READY_ROUTES = [
  shortvideoRoutes.projects,
  `${shortvideoRoutes.workbench}?projectId=:id`,
  '/talent/shortvideo?projectId=:id',
].join('|')
const PROJECT_SUPPORTED_ACTIONS = [
  'server-filter-projects',
  'create-project',
  'edit-project',
  'delete-project',
  'navigate-project-workbench',
].join('|')

function inferProjectScope(pathname: string): 'admin' | 'talent' {
  return pathname.startsWith('/talent') ? 'talent' : 'admin'
}

function projectWorkbenchPath(scope: 'admin' | 'talent', projectId: number | string) {
  if (scope === 'talent') return `/talent/shortvideo?projectId=${projectId}`
  return `${shortvideoRoutes.workbench}?projectId=${projectId}`
}

function getProjectBlocker(row: SvProject): { label: string; color: 'default' | 'warning' | 'success' | 'error' } {
  if (String(row.status ?? '') === 'failed') return { label: '项目失败', color: 'error' }
  if (!row.scriptId) return { label: '缺脚本', color: 'warning' }
  if (!row.shotListId) return { label: '缺分镜', color: 'warning' }
  if (!row.finalVideoUrl) return { label: '缺成片', color: 'warning' }
  return { label: '可发布', color: 'success' }
}

export default function ProjectsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const routeScope = inferProjectScope(useLocation().pathname)
  const [search, setSearch] = useState({ page: 0, rows: 20, title: '', status: undefined as string | undefined })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SvProject>>({})
  const [deleteTarget, setDeleteTarget] = useState<SvProject | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [operationError, setOperationError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sv-projects', search],
    queryFn: () => shortvideoApi.list(search),
  })
  const rows = data?.list ?? []
  const shellLabel = routeScope === 'talent' ? '达人端' : '管理员端'
  const filterContext = () => (
    `scope=${routeScope}; shell=${shellLabel}; titleFilter=${search.title.trim() || '空'}; statusFilter=${search.status || '全部'}; inputTitle=${query.title.trim() || '空'}; inputStatus=${query.status || '全部'}; page=${search.page}; rows=${search.rows}`
  )
  const projectContext = (project?: Partial<SvProject> | null, fallbackId?: number | string) => {
    const id = project?.id ?? fallbackId ?? '-'
    return `projectId=${id}; title=${project?.title || '未知项目'}; projectType=${project?.projectType || '-'}; status=${project?.status || '-'}; scriptId=${project?.scriptId ?? '-'}; shotListId=${project?.shotListId ?? '-'}; ${filterContext()}`
  }
  const saveMut = useMutation({
    mutationFn: (body: SvProjectSave) => shortvideoApi.save(body),
    onMutate: () => setOperationError(null),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['sv-projects'] }) },
    onError: (e: Error, body) => {
      const message = getErrorMessage(e)
      setOperationError(`项目保存失败（POST ${PROJECT_ENDPOINTS.save}）：${message}（${projectContext(body)}）`)
      toast(message, 'error')
    },
  })
  const delMut = useMutation({
    mutationFn: (row: SvProject) => shortvideoApi.delete(row.id),
    onMutate: () => {
      setOperationError(null)
      setDeleteError(null)
    },
    onSuccess: () => {
      toast('删除成功', 'success')
      setDeleteError(null)
      setDeleteTarget(null)
      qc.invalidateQueries({ queryKey: ['sv-projects'] })
    },
    onError: (e: Error, row) => {
      const message = getErrorMessage(e)
      setDeleteError(message)
      setOperationError(`项目删除失败（POST ${PROJECT_ENDPOINTS.delete}）：${message}（${projectContext(row)}）`)
      toast(message, 'error')
    },
  })

  const openAdd = useCallback(() => {
    setForm({ projectType: 'viral_clone', status: 'draft', title: '' })
    setFormOpen(true)
  }, [])
  const openEdit = useCallback((row: SvProject) => { setForm({ ...row }); setFormOpen(true) }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])

  const handleExportScript = useCallback(async (row: SvProject) => {
    setOperationError(null)
    try {
      const decision = await checkGaifanEntitlement('shortvideo-maker', 'shortvideo-maker.export')
      if (!decision?.granted) {
        toast(commercialDenialMessage(decision), 'warning')
        return
      }
      const text = await shortvideoApi.exportScript(row.id)
      toast(text ? '脚本已导出' : '导出完成', 'success')
    } catch (e) {
      const msg = isCommercialDenial(e) ? commercialDenialMessage(e) : getErrorMessage(e)
      setOperationError(`导出脚本失败：${msg}`)
      toast(msg, 'error')
    }
  }, [toast])

  const handleConfirmSave = useCallback(() => {
    const title = String(form.title ?? '').trim()
    const projectType = String(form.projectType ?? '').trim()
    if (!title) { toast('请填写项目标题', 'warning'); return }
    if (!projectType) { toast('请选择项目类型', 'warning'); return }
    const body: SvProjectSave = {
      id: form.id,
      title,
      projectType,
      status: form.status?.trim() || 'draft',
      accountId: form.accountId,
      scriptId: form.scriptId,
      shotListId: form.shotListId,
      finalVideoUrl: form.finalVideoUrl?.trim() || undefined,
      publishTitle: form.publishTitle?.trim() || undefined,
      personaId: form.personaId,
      scheduleDate: form.scheduleDate,
      shootStatus: form.shootStatus,
    }
    saveMut.mutate(body)
  }, [form, saveMut, toast])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '项目标题', flex: 1, minWidth: 160 },
    {
      field: 'projectType', headerName: '类型', width: 100,
      valueFormatter: (v: string) => PROJECT_TYPE_MAP[v] ?? v,
    },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const key = String(value ?? 'draft')
        const s = STATUS_MAP[key] ?? { label: key, color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    {
      field: 'chainStatus',
      headerName: '成片链路',
      width: 150,
      sortable: false,
      renderCell: ({ row }: GridRenderCellParams<SvProject>) => {
        const blocker = getProjectBlocker(row)
        return (
          <Chip
            label={blocker.label}
            color={blocker.color}
            size="small"
            variant={blocker.color === 'success' ? 'filled' : 'outlined'}
          />
        )
      },
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: (p: GridRenderCellParams<SvProject>) => {
        const row = p.row
        return (
          <Stack direction="row" gap={0.5} alignItems="center">
            <Tooltip title="进入工作台">
              <IconButton
                size="small"
                color="primary"
                onClick={() => navigate(projectWorkbenchPath(routeScope, row.id))}
                aria-label="进入工作台"
                data-testid="shortvideo-projects-open-workbench-button"
                data-target-route={projectWorkbenchPath(routeScope, row.id)}
              >
                <OpenInNewIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Button
              size="small"
              onClick={() => void handleExportScript(row)}
              disabled={!row.scriptId}
              data-testid="shortvideo-projects-export-script-button"
            >
              导出脚本
            </Button>
            <Button
              size="small"
              onClick={() => openEdit(row)}
              data-testid="shortvideo-projects-edit-button"
              data-source-endpoint={PROJECT_ENDPOINTS.save}
            >
              编辑
            </Button>
            <Button
              size="small"
              color="error"
              onClick={() => {
                setDeleteError(null)
                setDeleteTarget(row)
              }}
              data-testid="shortvideo-projects-delete-button"
              data-source-endpoint={PROJECT_ENDPOINTS.delete}
            >
              删除
            </Button>
          </Stack>
        )
      },
    },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" data-testid="shortvideo-projects-search-slot">
      <TextField size="small" label="标题" value={query.title}
        onChange={e => setQuery(q => ({ ...q, title: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 180 }} />
      <TextField select size="small" label="状态" value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : e.target.value }))}
        sx={{ width: 120 }}>
        <MenuItem value="">全部</MenuItem>
        {Object.entries(STATUS_MAP).map(([v, s]) => <MenuItem key={v} value={v}>{s.label}</MenuItem>)}
      </TextField>
      <Button
        variant="contained"
        size="small"
        onClick={handleSearch}
        data-testid="shortvideo-projects-search-button"
        data-source-endpoint={PROJECT_ENDPOINTS.list}
      >
        搜索
      </Button>
    </Stack>
  )

  const actionSlot = (
    <Button
      variant="contained"
      startIcon={<AddIcon />}
      onClick={openAdd}
      data-testid="shortvideo-projects-create-button"
      data-source-endpoint={PROJECT_ENDPOINTS.save}
    >
      新建项目
    </Button>
  )
  const summary = rows.reduce(
    (acc, row) => {
      const key = String(row.status ?? 'draft')
      if (key === 'completed') acc.completed += 1
      else if (key === 'failed') acc.failed += 1
      else if (key === 'processing') acc.processing += 1
      else acc.draft += 1
      if (row.finalVideoUrl) acc.finishedVideo += 1
      if (!row.scriptId) acc.missingScript += 1
      if (!row.shotListId) acc.missingShotList += 1
      if (!row.finalVideoUrl) acc.missingFinalVideo += 1
      return acc
    },
    { draft: 0, processing: 0, completed: 0, failed: 0, finishedVideo: 0, missingScript: 0, missingShotList: 0, missingFinalVideo: 0 },
  )

  return (
    <Box
      data-testid="shortvideo-projects-page"
      data-contract-scope={`shortvideo-projects-${routeScope}`}
      data-ready-endpoints={PROJECT_READY_ENDPOINTS.join('|')}
      data-ready-routes={PROJECT_READY_ROUTES}
      data-supported-actions={PROJECT_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={PROJECT_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-project-fallback="true"
      data-server-pagination="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <PageHeader
        title={routeScope === 'talent' ? '达人短视频项目' : '短视频项目'}
        breadcrumbs={[{ label: routeScope === 'talent' ? '达人端' : '短视频' }, { label: '项目' }]}
        subtitle={
          routeScope === 'talent'
            ? '达人端复用短视频项目列表；项目工作台入口保留在 `/talent/shortvideo?projectId=...`，素材生产、剪辑和发布链路仍在达人短视频目录内完成。'
            : '项目 CRUD、工作台入口与成片状态总览；生成链路在工作台、素材生产和剪辑页完成。'
        }
        actions={actionSlot}
      />

      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="shortvideo-projects-boundary-contract"
        data-source-endpoints={PROJECT_READY_ENDPOINTS.join('|')}
        data-no-local-project-fallback="true"
        data-workbench-route-scope={routeScope}
        data-supported-actions={PROJECT_SUPPORTED_ACTIONS}
      >
        列表来自 POST {PROJECT_ENDPOINTS.list}。项目只保存脚本、分镜、成片和发布字段，素材生成、自动剪辑、发布审核分别在独立页面执行。
        {routeScope === 'talent' ? ' 当前为达人端路由，页面不自动跳回管理员短视频工作台。' : ''}
      </Alert>

      {rows.length > 0 && summary.missingFinalVideo > 0 && (
        <Alert
          severity="warning"
          variant="outlined"
          icon={<WarningAmberIcon />}
          sx={{ mb: 2 }}
          data-testid="shortvideo-projects-chain-warning"
          data-source-endpoint={PROJECT_ENDPOINTS.list}
          data-no-static-summary="true"
        >
          当前页 {summary.missingFinalVideo} 个项目未生成成片，其中 {summary.missingScript} 个缺脚本、{summary.missingShotList} 个缺分镜。请从工作台、脚本策划或素材生产页补齐链路后再发布。
        </Alert>
      )}

      <Grid
        container
        spacing={2}
        sx={{ mb: 2 }}
        data-testid="shortvideo-projects-summary"
        data-source-endpoint={PROJECT_ENDPOINTS.list}
        data-no-static-summary="true"
      >
        {[
          ['当前页项目', rows.length],
          ['进行中', summary.processing],
          ['已完成', summary.completed],
          ['已有成片', summary.finishedVideo],
        ].map(([label, value]) => (
          <Grid item xs={6} md={3} key={String(label)}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight={700}>{value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Box
          data-testid="shortvideo-projects-list-error"
          data-source-endpoint={PROJECT_ENDPOINTS.list}
          data-no-local-project-fallback="true"
          data-input-retained="true"
        >
          <ErrorAlert
            title="项目列表加载失败"
            message={`POST ${PROJECT_ENDPOINTS.list}：${getErrorMessage(error)}（${filterContext()}）。请检查登录态和数据权限；页面不会补本地项目。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}
      {operationError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setOperationError(null)}
          data-testid="shortvideo-projects-operation-error"
          data-source-endpoints={`${PROJECT_ENDPOINTS.save}|${PROJECT_ENDPOINTS.delete}`}
          data-no-local-project-mutation="true"
          data-input-retained="true"
        >
          {operationError}
        </Alert>
      )}

      <Box
        sx={{ flex: 1, minHeight: 320, mt: isError ? 2 : 0 }}
        data-testid="shortvideo-projects-grid"
        data-source-endpoint={PROJECT_ENDPOINTS.list}
        data-server-pagination="true"
        data-no-local-project-fallback="true"
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          rowCount={data?.total ?? 0}
          loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>
      <FormDialog open={formOpen} title={form.id ? '编辑项目' : '新建项目'} onClose={() => setFormOpen(false)} onConfirm={handleConfirmSave} loading={saveMut.isPending}>
        <Stack
          spacing={2}
          sx={{ pt: 1 }}
          data-testid="shortvideo-projects-form"
          data-source-endpoint={PROJECT_ENDPOINTS.save}
          data-input-retained={operationError?.includes(PROJECT_ENDPOINTS.save) ? 'true' : 'false'}
        >
          {operationError?.includes(PROJECT_ENDPOINTS.save) && (
            <Alert
              severity="error"
              data-testid="shortvideo-projects-save-error"
              data-source-endpoint={PROJECT_ENDPOINTS.save}
              data-no-local-project-mutation="true"
              data-input-retained="true"
            >
              {operationError}。当前表单输入已保留。
            </Alert>
          )}
          <TextField label="项目标题" required value={form.title ?? ''} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} fullWidth size="small" />
          <TextField select label="项目类型" required value={form.projectType ?? 'viral_clone'} onChange={e => setForm(f => ({ ...f, projectType: e.target.value }))} size="small" fullWidth>
            {Object.entries(PROJECT_TYPE_MAP).map(([v, label]) => <MenuItem key={v} value={v}>{label}</MenuItem>)}
          </TextField>
          <TextField select label="状态" value={form.status ?? 'draft'} onChange={e => setForm(f => ({ ...f, status: e.target.value }))} size="small" fullWidth>
            {Object.entries(STATUS_MAP).map(([v, s]) => <MenuItem key={v} value={v}>{s.label}</MenuItem>)}
          </TextField>
          <Grid container spacing={1.5}>
            <Grid item xs={12} sm={6}>
              <TextField label="Persona ID（可选）" value={form.personaId ?? ''} onChange={e => setForm(f => ({ ...f, personaId: Number(e.target.value) || undefined }))} fullWidth size="small" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="计划日期" type="date" value={form.scheduleDate ? String(form.scheduleDate).slice(0, 10) : ''} onChange={e => setForm(f => ({ ...f, scheduleDate: e.target.value || undefined }))} fullWidth size="small" InputLabelProps={{ shrink: true }} />
            </Grid>
          </Grid>
          <Grid container spacing={1.5}>
            <Grid item xs={12} sm={6}>
              <TextField label="脚本 ID（可选）" value={form.scriptId ?? ''} onChange={e => setForm(f => ({ ...f, scriptId: Number(e.target.value) || undefined }))} fullWidth size="small" helperText="为空时工作台会提示先完成脚本策划" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="分镜 ID（可选）" value={form.shotListId ?? ''} onChange={e => setForm(f => ({ ...f, shotListId: Number(e.target.value) || undefined }))} fullWidth size="small" helperText="为空时素材生产无法批量提交" />
            </Grid>
          </Grid>
          <TextField label="成片 URL（可选）" value={form.finalVideoUrl ?? ''} onChange={e => setForm(f => ({ ...f, finalVideoUrl: e.target.value }))} fullWidth size="small" helperText="写入 finalVideoUrl 后可进入发布审核链路" />
          <TextField label="发布标题 / 备注" value={form.publishTitle ?? ''} onChange={e => setForm(f => ({ ...f, publishTitle: e.target.value }))} fullWidth size="small" multiline minRows={2} helperText="对应后端 publishTitle，可选" />
        </Stack>
      </FormDialog>
      <ConfirmDialog
        open={deleteTarget !== null}
        content={
          deleteError
            ? `确定要删除该项目吗？endpoint=${PROJECT_ENDPOINTS.delete}; ${projectContext(deleteTarget)}。上次删除失败（POST ${PROJECT_ENDPOINTS.delete}）：${deleteError}`
            : `确定要删除该项目吗？endpoint=${PROJECT_ENDPOINTS.delete}; ${projectContext(deleteTarget)}。`
        }
        onClose={() => {
          setDeleteTarget(null)
          setDeleteError(null)
        }}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending}
      />
    </Box>
  )
}
