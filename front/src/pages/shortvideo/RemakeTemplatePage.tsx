import { useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Grid, MenuItem, Select, FormControl, InputLabel, Stack, TextField, Typography } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { ConfirmDialog, DataGridEmptyOverlay, PageHeader, StandardDataGrid } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import type { RemakeTemplate } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { Dialog, DialogTitle, DialogContent, DialogActions, TextField as MuiTextField } from '@mui/material'
import { getErrorMessage } from '@/utils/errorHandler'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const TEMPLATE_TYPES = [
  { value: 'form_copy', label: '形式复刻' },
  { value: 'structure_copy', label: '结构复刻' },
  { value: 'emotion_curve', label: '情绪曲线' },
  { value: 'scene_copy', label: '场景复刻' },
]

const DEFAULT_FORM: Partial<RemakeTemplate> = { templateName: '', remakeType: 'form_copy', adaptationGuide: '', structureTemplate: '{}' }
const REMAKE_TEMPLATE_ENDPOINTS = {
  list: '/short-video/remake-template/list',
  save: '/short-video/remake-template/save',
  delete: '/short-video/remake-template/delete',
} as const
const REMAKE_TEMPLATE_READY_ENDPOINTS = [
  REMAKE_TEMPLATE_ENDPOINTS.list,
  REMAKE_TEMPLATE_ENDPOINTS.save,
  REMAKE_TEMPLATE_ENDPOINTS.delete,
].join('|')
const REMAKE_TEMPLATE_READY_ROUTES = [
  shortvideoRoutes.remakeTemplates,
  shortvideoRoutes.viralVideos,
].join('|')
const REMAKE_TEMPLATE_SUPPORTED_ACTIONS = [
  'query-remake-templates',
  'create-remake-template',
  'edit-remake-template',
  'delete-remake-template',
  'validate-structure-template-json',
].join('|')
const REMAKE_TEMPLATE_UNSUPPORTED_ENDPOINTS = [
  '/short-video/remake-template/mock',
  '/short-video/remake-template/local-list',
  '/short-video/remake-template/local-save',
  '/short-video/remake-template/local-delete',
  '/short-video/remake-template/local-generate',
  '/short-video/remake-template/generate',
  '/short-video/remake-template/create-from-viral',
  '/short-video/remake-template/export',
  '/short-video/viral-remake/recommend',
  '/short-video/viral-remake/batch-recommend',
  '/short-video/viral-remake/confirm',
  '/short-video/viral-remake/generate-script',
  '/short-video/viral-remake/assign-task',
  '/short-video/viral-remake/complete',
].join('|')

export default function RemakeTemplatePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, templateName: '', remakeType: '' })
  const [query, setQuery] = useState(search)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<Partial<RemakeTemplate>>(DEFAULT_FORM)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [operationError, setOperationError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [deleteError, setDeleteError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sv-remake-templates', search],
    queryFn: () => shortvideoApi.remakeTemplateList({
      ...search,
      templateName: search.templateName || undefined,
      remakeType: search.remakeType || undefined,
    }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<RemakeTemplate>) => shortvideoApi.remakeTemplateSave(p),
    onMutate: () => { setOperationError(''); setSaveError('') },
    onSuccess: () => { toast('保存成功', 'success'); setDialogOpen(false); qc.invalidateQueries({ queryKey: ['sv-remake-templates'] }) },
    onError: (e) => {
      const message = getErrorMessage(e)
      const errorText = `模板保存失败（POST ${REMAKE_TEMPLATE_ENDPOINTS.save}）：${message}`
      setSaveError(errorText)
      setOperationError(errorText)
      toast(message, 'error')
    },
  })
  const deleteMut = useMutation({
    mutationFn: shortvideoApi.remakeTemplateDelete,
    onMutate: () => { setOperationError(''); setDeleteError('') },
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); setDeleteError(''); qc.invalidateQueries({ queryKey: ['sv-remake-templates'] }) },
    onError: (e) => {
      const message = getErrorMessage(e)
      const errorText = `模板删除失败（POST ${REMAKE_TEMPLATE_ENDPOINTS.delete}）：${message}`
      setDeleteError(errorText)
      setOperationError(`${errorText}。失败时不会从列表移除模板。`)
      toast(message, 'error')
    },
  })

  const openAdd = () => { setForm(DEFAULT_FORM); setSaveError(''); setDialogOpen(true) }
  const openEdit = (row: RemakeTemplate) => {
    setSaveError('')
    setForm({
      ...row,
      remakeType: row.remakeType ?? row.templateType ?? 'form_copy',
      adaptationGuide: row.adaptationGuide ?? row.content ?? '',
      structureTemplate: typeof row.structureTemplate === 'string'
        ? row.structureTemplate
        : JSON.stringify(row.structureTemplate ?? {}, null, 2),
    })
    setDialogOpen(true)
  }

  const handleSave = () => {
    const templateName = String(form.templateName ?? '').trim()
    const remakeType = String(form.remakeType ?? form.templateType ?? '').trim()
    if (!templateName) { toast('请填写模板名称', 'warning'); return }
    if (!remakeType) { toast('请选择二创类型', 'warning'); return }
    let structureTemplate: unknown = {}
    try {
      structureTemplate = JSON.parse(String(form.structureTemplate ?? '{}'))
    } catch {
      toast('结构模板必须是合法 JSON', 'warning')
      return
    }
    saveMut.mutate({
      id: form.id,
      templateName,
      remakeType,
      structureTemplate,
      adaptationGuide: String(form.adaptationGuide ?? '').trim() || undefined,
      emotionCurve: form.emotionCurve,
      bgmStyle: form.bgmStyle,
      durationRange: form.durationRange,
    })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 160 },
    {
      field: 'remakeType',
      headerName: '类型',
      width: 130,
      valueGetter: (_, row) => row.remakeType ?? row.templateType,
      valueFormatter: (v: string) => TEMPLATE_TYPES.find(t => t.value === v)?.label ?? v,
    },
    {
      field: 'adaptationGuide',
      headerName: '改编指引',
      flex: 2,
      minWidth: 220,
      valueGetter: (_, row) => row.adaptationGuide ?? row.content,
      valueFormatter: (v: string) => v?.slice(0, 80),
    },
    { field: 'usageCount', headerName: '使用次数', width: 100 },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 150, sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" onClick={() => { setDeleteError(''); setDeleteId(row.id) }}>删除</Button>
        </Box>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField label="模板名称" size="small" value={query.templateName} onChange={e => setQuery(q => ({ ...q, templateName: e.target.value }))} sx={{ width: 160 }} />
      <FormControl size="small" sx={{ minWidth: 120 }}>
        <InputLabel>类型</InputLabel>
        <Select label="类型" value={query.remakeType} onChange={e => setQuery(q => ({ ...q, remakeType: e.target.value }))}>
          <MenuItem value="">全部</MenuItem>
          {TEMPLATE_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
        </Select>
      </FormControl>
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>查询</Button>
      <Button onClick={() => { const d = { page: 0, rows: 20, templateName: '', remakeType: '' }; setQuery(d); setSearch(d) }}>重置</Button>
    </>
  )

  const rows = data?.list ?? []

  return (
    <Box
      data-testid="remake-template-page"
      data-ready-endpoints={REMAKE_TEMPLATE_READY_ENDPOINTS}
      data-ready-routes={REMAKE_TEMPLATE_READY_ROUTES}
      data-supported-actions={REMAKE_TEMPLATE_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={REMAKE_TEMPLATE_UNSUPPORTED_ENDPOINTS}
      data-no-local-template-fallback="true"
      data-no-local-template-mutation="true"
      data-crud-only="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <PageHeader
        title="二创模板"
        breadcrumbs={[{ label: '短视频' }, { label: '二创模板' }]}
        subtitle="对齐 `/short-video/remake-template/*`：模板保存使用 remakeType、structureTemplate 和 adaptationGuide。"
        actions={<Button variant="contained" onClick={openAdd} data-testid="remake-template-open-create-button">新建模板</Button>}
      />
      <Alert
        severity="info"
        variant="outlined"
        data-testid="remake-template-boundary-contract"
        data-no-template-generation-on-page="true"
        data-no-local-template-fallback="true"
        data-supported-actions={REMAKE_TEMPLATE_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        列表来自 POST {REMAKE_TEMPLATE_ENDPOINTS.list}。从爆款分析生成模板需要 `viralVideoId`，本页只负责模板 CRUD；基于模板生成脚本需要变量 map，后续可在爆款详情或脚本策划页串联。
      </Alert>
      <Grid
        container
        spacing={2}
        data-testid="remake-template-diagnostics"
        data-current-page-only="true"
        data-no-client-template-synthesis="true"
        sx={{ mb: 2 }}
      >
        {[
          ['当前页模板', rows.length],
          ['已使用', rows.reduce((sum, r) => sum + Number(r.usageCount ?? 0), 0)],
          ['高评分', rows.filter(r => Number(r.avgViralScore ?? 0) >= 80).length],
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
        <Grid item xs={6} md={3}>
          <Card variant="outlined">
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">类型覆盖</Typography>
              <Stack direction="row" gap={0.5} flexWrap="wrap" useFlexGap sx={{ mt: 0.5 }}>
                {[...new Set(rows.map(r => String(r.remakeType ?? r.templateType ?? '')).filter(Boolean))].slice(0, 3).map(t => (
                  <Chip key={t} label={TEMPLATE_TYPES.find(x => x.value === t)?.label ?? t} size="small" />
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      {isError && (
        <Alert
          severity="error"
          data-testid="remake-template-list-error"
          data-no-local-template-fallback="true"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          二创模板加载失败（POST {REMAKE_TEMPLATE_ENDPOINTS.list}）：{getErrorMessage(error)}。页面不会补本地模板。
        </Alert>
      )}
      {operationError && (
        <Alert
          severity="error"
          data-testid="remake-template-operation-error"
          data-no-local-template-mutation="true"
          sx={{ mb: 2 }}
          onClose={() => setOperationError('')}
        >
          {operationError}
        </Alert>
      )}
      <Box
        data-testid="remake-template-grid-contract"
        data-no-local-template-fallback="true"
        data-no-local-delete-mutation="true"
        data-server-filter-payload="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{form.id ? '编辑模板' : '新建模板'}</DialogTitle>
        <DialogContent data-testid="remake-template-dialog" data-input-retained="true" data-no-local-template-mutation="true">
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            {saveError && (
              <Grid item xs={12}>
                <Alert severity="error" data-testid="remake-template-save-error" data-input-retained="true" data-no-local-template-mutation="true">
                  {saveError}。当前模板输入已保留。
                </Alert>
              </Grid>
            )}
            <Grid item xs={12} sm={8}>
              <MuiTextField label="模板名称" fullWidth value={form.templateName ?? ''} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth>
                <InputLabel>二创类型</InputLabel>
                <Select label="二创类型" value={form.remakeType ?? 'form_copy'} onChange={e => setForm(f => ({ ...f, remakeType: e.target.value }))}>
                  {TEMPLATE_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <MuiTextField label="结构模板 JSON" fullWidth multiline minRows={5} value={String(form.structureTemplate ?? '{}')} onChange={e => setForm(f => ({ ...f, structureTemplate: e.target.value }))} />
            </Grid>
            <Grid item xs={12}>
              <MuiTextField label="改编指引" fullWidth multiline minRows={4} value={form.adaptationGuide ?? ''} onChange={e => setForm(f => ({ ...f, adaptationGuide: e.target.value }))} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" disabled={!form.templateName || saveMut.isPending} onClick={handleSave} data-testid="remake-template-save-button" data-source-endpoint={REMAKE_TEMPLATE_ENDPOINTS.save}>保存</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteId !== null} content={deleteError ? `确定要删除该模板吗？上次删除失败：${deleteError}` : '确定要删除该模板吗？'}
        onClose={() => { setDeleteId(null); setDeleteError('') }}
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending} />
    </Box>
  )
}
