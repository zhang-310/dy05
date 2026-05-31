import { useState, useCallback } from 'react'
import {
  Box, Stack, Button, TextField, Card, CardContent,
  Chip, Grid, Select, MenuItem, InputLabel, FormControl, Alert,
  Typography,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { shortvideoApi, type SvDrama } from '@/api/shortvideo'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { useGaifanEntitlementGate } from '@/hooks/useGaifanEntitlementGate'
import { commercialDenialMessage, isCommercialDenial, CREDITS_GOVERNANCE_PATH } from '@/utils/commercialError'
import request from '@/utils/request'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { Link } from 'react-router-dom'

const GENRES = ['都市', '古装', '悬疑', '爱情', '喜剧', '励志', '其他']
const STATUS_MAP: Record<string, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  draft: { label: '草稿', color: 'default' },
  planning: { label: '策划中', color: 'default' },
  shooting: { label: '拍摄中', color: 'warning' },
  post: { label: '后期中', color: 'warning' },
  done: { label: '已完成', color: 'success' },
  '0': { label: '策划中', color: 'default' },
  '1': { label: '拍摄中', color: 'warning' },
  '2': { label: '后期中', color: 'warning' },
  '3': { label: '已完成', color: 'success' },
}

const DRAMA_ENDPOINTS = {
  list: '/short-video/drama/list',
  create: '/short-video/drama/create',
  update: '/short-video/drama/update',
  delete: '/short-video/drama/delete',
  generateScript: '/short-video/drama/generate-script',
} as const
const DRAMA_READY_ENDPOINTS = [
  DRAMA_ENDPOINTS.list,
  DRAMA_ENDPOINTS.create,
  DRAMA_ENDPOINTS.update,
  DRAMA_ENDPOINTS.delete,
  DRAMA_ENDPOINTS.generateScript,
] as const
const DRAMA_READY_ROUTES = [
  shortvideoRoutes.drama,
  shortvideoRoutes.dashboard,
].join('|')
const DRAMA_SUPPORTED_ACTIONS = [
  'refresh-drama-projects',
  'create-drama-project',
  'edit-drama-project',
  'delete-drama-project',
  'generate-drama-script',
].join('|')
const DRAMA_UNSUPPORTED_ENDPOINTS = [
  '/short-video/drama/mock',
  '/short-video/drama/local-list',
  '/short-video/drama/local-save',
  '/short-video/drama/local-delete',
  '/short-video/drama/local-script-template',
  '/short-video/drama/static-summary',
  '/short-video/drama/apply-script-to-episodes-local',
] as const

export default function DramaPage() {
  const toast = useToast()
  const gate = useGaifanEntitlementGate()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, genre: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SvDrama> & { episodes?: number }>({ status: 'draft' })
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [genDramaId, setGenDramaId] = useState('')
  const [genQuery, setGenQuery] = useState('')
  const [genResult, setGenResult] = useState('')
  const [actionError, setActionError] = useState('')

  const { data: rawList = [], isFetching, isError, error, refetch } = useQuery({
    queryKey: ['drama-projects'],
    queryFn: () => shortvideoApi.dramaList(),
  })
  const list = normalizeRows<SvDrama>(rawList)

  const filtered = search.genre
    ? list.filter(d => String(d.genre ?? '') === search.genre)
    : list
  const start = search.page * search.rows
  const rows = filtered.slice(start, start + search.rows)
  const total = filtered.length

  const saveMut = useMutation({
    mutationFn: async (params: Partial<SvDrama> & { episodes?: number }) => {
      const totalEpisodes = params.totalEpisodes ?? params.episodes ?? 1
      const description = params.synopsis ?? params.description
      if (params.id) {
        await shortvideoApi.dramaUpdate({
          id: params.id,
          title: params.title,
          description,
          genre: params.genre,
          totalEpisodes,
        })
      } else {
        await shortvideoApi.dramaCreate({
          title: params.title ?? '',
          description,
          genre: params.genre,
          totalEpisodes,
        })
      }
    },
    onSuccess: () => { setActionError(''); toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['drama-projects'] }) },
    onError: (e) => {
      const message = isCommercialDenial(e) ? commercialDenialMessage(e) : getErrorMessage(e)
      const endpoint = form.id ? DRAMA_ENDPOINTS.update : DRAMA_ENDPOINTS.create
      setActionError(`短剧保存失败（POST ${endpoint}）：${message}`)
      toast(message, 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => shortvideoApi.dramaDelete(id),
    onMutate: () => { setActionError(''); setDeleteError('') },
    onSuccess: () => { setActionError(''); toast('删除成功', 'success'); setDeleteId(null); setDeleteError(''); qc.invalidateQueries({ queryKey: ['drama-projects'] }) },
    onError: (e) => {
      const message = getErrorMessage(e)
      const errorText = `短剧删除失败（POST ${DRAMA_ENDPOINTS.delete}）：${message}`
      setDeleteError(errorText)
      setActionError(`${errorText}。失败时不会从列表移除短剧。`)
      toast(message, 'error')
    },
  })

  const genMut = useMutation({
    mutationFn: () => shortvideoApi.dramaGenerateScript({
      dramaId: Number(genDramaId),
      theme: genQuery,
    }),
    onSuccess: (res) => {
      setGenResult(typeof res === 'string' ? res : JSON.stringify(res, null, 2))
      setActionError('')
      toast('剧本已生成', 'success')
    },
    onError: (e) => {
      const message = isCommercialDenial(e) ? commercialDenialMessage(e) : getErrorMessage(e)
      setActionError(`剧本生成失败（POST ${DRAMA_ENDPOINTS.generateScript}）：${message}`)
      toast(message, 'error')
    },
  })

  const exportMut = useMutation({
    mutationFn: (dramaId: number) =>
      request.post<{ projectId: number; traceId: string }>('/drama/export-to-maker', { id: dramaId }),
    onSuccess: (data) => {
      toast(`已提交导出成片（trace=${data?.traceId ?? '—'}）`, 'success')
      setActionError('')
    },
    onError: (e) => {
      const message = isCommercialDenial(e) ? commercialDenialMessage(e) : getErrorMessage(e)
      setActionError(`导出成片失败：${message}`)
      toast(message, 'error')
    },
  })

  const handleCreateWithEntitlement = useCallback(async () => {
    try {
      const decision = await checkGaifanEntitlement('drama-ai', 'drama-ai.storyboard')
      if (!decision?.granted) {
        toast(commercialDenialMessage(decision), 'warning')
        return
      }
      openAddForm()
    } catch (e) {
      toast(getErrorMessage(e), 'error')
    }
  }, [toast])

  const openAddForm = () => {
    setForm({ totalEpisodes: 10, status: 'draft' })
    setFormOpen(true)
  }

  const handleSave = () => {
    const title = String(form.title ?? '').trim()
    if (!title) { toast('请填写剧情标题', 'warning'); return }
    saveMut.mutate({ ...form, title })
  }

  const summary = list.reduce(
    (acc, row) => {
      acc.episodes += Number(row.totalEpisodes ?? (row as { episodes?: number }).episodes ?? 0)
      if (String(row.status ?? 'draft') === 'done' || String(row.status ?? '') === '3') acc.done += 1
      acc.withSynopsis += Number(row.episodesWithSynopsis ?? 0)
      acc.withProject += Number(row.episodesWithProject ?? 0)
      return acc
    },
    { episodes: 0, done: 0, withSynopsis: 0, withProject: 0 },
  )

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '剧情标题', flex: 1, minWidth: 160 },
    { field: 'genre', headerName: '题材', width: 100,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'totalEpisodes', headerName: '集数', width: 80,
      valueGetter: (_v, row) => row.totalEpisodes ?? (row as { episodes?: number }).episodes ?? '—' },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const key = String(value ?? 'draft')
        const s = STATUS_MAP[key] ?? { label: key, color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      } },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => {
            const r = row as SvDrama
            setForm({ ...r, episodes: r.totalEpisodes })
            setFormOpen(true)
          }}>编辑</Button>
          <Button size="small" onClick={() => exportMut.mutate((row as SvDrama).id)} disabled={exportMut.isPending}>
            导出
          </Button>
          <Button size="small" color="error" onClick={() => { setDeleteError(''); setDeleteId((row as SvDrama).id) }}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <FormControl size="small" sx={{ minWidth: 120 }}>
      <InputLabel>题材</InputLabel>
      <Select value={search.genre} label="题材" onChange={e => setSearch(s => ({ ...s, genre: e.target.value, page: 0 }))}>
        <MenuItem value="">全部</MenuItem>
        {GENRES.map(g => <MenuItem key={g} value={g}>{g}</MenuItem>)}
      </Select>
    </FormControl>
  )

  const actionSlot = (
    <Button
      variant="contained"
      startIcon={<AddIcon />}
      onClick={() => { void handleCreateWithEntitlement() }}
      data-testid="drama-open-create-button"
    >
      新建剧情项目
    </Button>
  )

  return (
    <Box
      data-testid="shortvideo-drama-page"
      data-contract-scope="shortvideo-drama"
      data-ready-endpoints={DRAMA_READY_ENDPOINTS.join('|')}
      data-ready-routes={DRAMA_READY_ROUTES}
      data-supported-actions={DRAMA_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={DRAMA_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-drama-fallback="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <PageHeader
        title="剧情短视频创作"
        breadcrumbs={[{ label: '短视频' }, { label: '短剧' }]}
        subtitle="产品码 drama-ai · 创建/写剧本按 Feature 扣费；可导出至短视频制作。"
        actions={
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip label="drama-ai" size="small" color="primary" variant="outlined" />
            <Button component={Link} to={shortvideoRoutes.projects} variant="outlined" size="small">
              导出至 shortvideo-maker
            </Button>
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={isFetching} data-testid="drama-refresh-button" data-source-endpoint={DRAMA_ENDPOINTS.list}>
              刷新
            </Button>
          </Stack>
        }
      />

      <Alert severity="warning" variant="outlined" sx={{ mb: 2 }}>
        产品 <strong>drama-ai</strong>：创建/写剧本消耗权益与积分。无权益或积分不足请前往{' '}
        <Link to={CREDITS_GOVERNANCE_PATH}>积分治理</Link>。
      </Alert>

      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="drama-boundary-contract"
        data-source-endpoints={DRAMA_READY_ENDPOINTS.join('|')}
        data-no-local-drama-fallback="true"
        data-no-local-script-template="true"
        data-supported-actions={DRAMA_SUPPORTED_ACTIONS}
      >
        列表与创建/更新/删除/剧本生成对齐 POST {DRAMA_ENDPOINTS.list}、{DRAMA_ENDPOINTS.create}、{DRAMA_ENDPOINTS.update}、{DRAMA_ENDPOINTS.delete}、{DRAMA_ENDPOINTS.generateScript}；当前列表接口返回全量数组，本页只做前端分页和题材过滤。
      </Alert>

      <Grid
        container
        spacing={2}
        sx={{ mb: 2 }}
        data-testid="drama-summary"
        data-source-endpoint={DRAMA_ENDPOINTS.list}
        data-no-static-summary="true"
      >
        {[
          ['短剧项目', list.length],
          ['总集数', summary.episodes],
          ['已完成短剧', summary.done],
          ['已关联项目集数', summary.withProject],
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
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
          data-testid="drama-list-error"
          data-source-endpoint={DRAMA_ENDPOINTS.list}
          data-no-local-drama-fallback="true"
        >
          短剧列表加载失败（POST {DRAMA_ENDPOINTS.list}）：{getErrorMessage(error)}。页面不会补本地短剧。
        </Alert>
      )}

      {actionError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setActionError('')}
          data-testid="drama-action-error"
          data-source-endpoints={`${DRAMA_ENDPOINTS.create}|${DRAMA_ENDPOINTS.update}|${DRAMA_ENDPOINTS.delete}|${DRAMA_ENDPOINTS.generateScript}`}
          data-no-local-drama-mutation="true"
          data-input-retained="true"
        >
          {actionError}
        </Alert>
      )}

      <Card
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="drama-script-generator"
        data-source-endpoint={DRAMA_ENDPOINTS.generateScript}
        data-no-local-script-template="true"
      >
        <CardContent>
          <Typography variant="subtitle2" fontWeight={600} gutterBottom>AI 剧本生成</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            调用 `/short-video/drama/generate-script`，后端必填 `dramaId`；生成结果只返回文本，应用到剧集需在短剧详情链路调用 `apply-script-to-episodes`。
          </Typography>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
            <TextField size="small" label="短剧 ID" value={genDramaId} onChange={e => setGenDramaId(e.target.value)} sx={{ width: 120 }} />
            <Typography variant="caption" color="text.secondary">在下方表格创建短剧后填写其 ID</Typography>
          </Stack>
          <Stack direction="row" spacing={1}>
            <TextField size="small" value={genQuery} onChange={e => setGenQuery(e.target.value)}
              placeholder="主题 / 风格提示..."
              sx={{ flex: 1 }} />
            <Button variant="outlined" startIcon={<AutoFixHighIcon />}
              onClick={async () => {
                if (!(await gate('drama-ai', 'drama-ai.script'))) return
                genMut.mutate()
              }} disabled={!genQuery.trim() || !genDramaId.trim() || genMut.isPending} data-testid="drama-generate-script-button" data-source-endpoint={DRAMA_ENDPOINTS.generateScript}>
              {genMut.isPending ? '生成中...' : '生成剧本'}
            </Button>
          </Stack>
          {genResult && (
            <Box
              data-testid="drama-script-result-preview"
              data-source-endpoint={DRAMA_ENDPOINTS.generateScript}
              sx={(theme) => ({
                mt: 1.5,
                p: 1.5,
                bgcolor: theme.palette.mode === 'dark'
                  ? theme.palette.background.default
                  : alpha(theme.palette.common.black, 0.025),
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                whiteSpace: 'pre-wrap',
                fontSize: 13,
                color: 'text.primary',
              })}
            >
              {genResult}
            </Box>
          )}
          {genMut.isError && (
            <Alert
              severity="error"
              sx={{ mt: 1.5 }}
              data-testid="drama-script-error"
              data-source-endpoint={DRAMA_ENDPOINTS.generateScript}
              data-no-local-script-template="true"
              data-input-retained="true"
            >
              剧本生成失败（POST {DRAMA_ENDPOINTS.generateScript}）：{getErrorMessage(genMut.error)}。已保留短剧 ID 和主题输入。
            </Alert>
          )}
        </CardContent>
      </Card>

      {!isFetching && !isError && list.length === 0 && (
        <Alert
          severity="info"
          sx={{ mb: 2 }}
          data-testid="drama-empty"
          data-source-endpoint={DRAMA_ENDPOINTS.list}
          data-no-local-drama-fallback="true"
        >
          还没有短剧项目。先新建短剧，再填写短剧 ID 生成剧本；剧本落到剧集需要后端剧集应用接口串联。
        </Alert>
      )}

      <Box
        sx={{ flex: 1, minHeight: 320 }}
        data-testid="drama-grid"
        data-source-endpoint={DRAMA_ENDPOINTS.list}
        data-client-pagination="true"
        data-no-local-drama-fallback="true"
      >
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={total} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot} actionSlot={actionSlot}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>

      <FormDialog open={formOpen} title={form.id ? '编辑剧情项目' : '新建剧情项目'}
        onClose={() => setFormOpen(false)} onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack
          spacing={2}
          sx={{ pt: 1 }}
          data-testid="drama-form"
          data-source-endpoints={`${DRAMA_ENDPOINTS.create}|${DRAMA_ENDPOINTS.update}`}
          data-input-retained={actionError.includes(DRAMA_ENDPOINTS.create) || actionError.includes(DRAMA_ENDPOINTS.update) ? 'true' : 'false'}
        >
          <TextField label="剧情标题" value={form.title ?? ''} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} fullWidth />
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <FormControl fullWidth size="small">
                <InputLabel>题材</InputLabel>
                <Select value={form.genre ?? ''} label="题材" onChange={e => setForm(f => ({ ...f, genre: e.target.value }))}>
                  {GENRES.map(g => <MenuItem key={g} value={g}>{g}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6}>
              <TextField label="集数" type="number" value={form.totalEpisodes ?? form.episodes ?? 10}
                onChange={e => setForm(f => ({ ...f, totalEpisodes: Number(e.target.value), episodes: Number(e.target.value) }))} fullWidth size="small" />
            </Grid>
          </Grid>
          <TextField label="故事简介" value={form.synopsis ?? form.description ?? ''} onChange={e => setForm(f => ({ ...f, synopsis: e.target.value, description: e.target.value }))} fullWidth multiline minRows={3} />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content={deleteError ? `确定删除该剧情项目？上次删除失败：${deleteError}` : '确定删除该剧情项目？'}
        onClose={() => { setDeleteId(null); setDeleteError('') }} onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)} loading={deleteMut.isPending} />
    </Box>
  )
}
