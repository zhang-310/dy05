import { useCallback, useMemo, useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Grid, MenuItem, Stack, TextField, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ConfirmDialog, DataGridEmptyOverlay, ErrorAlert, FormDialog, PageHeader, StandardDataGrid } from '@/components/base'
import { scriptApi, type ScriptTemplate, type ScriptTemplateQuery } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

const SCENES = [
  { value: 'opening', label: '开场' },
  { value: 'product', label: '产品介绍' },
  { value: 'promotion', label: '促销' },
  { value: 'interaction', label: '互动' },
  { value: 'closing', label: '结尾' },
  { value: 'general', label: '通用' },
]

const TEMPLATE_TYPES = [
  { value: 'user', label: '用户模板' },
  { value: 'system', label: '系统模板' },
]

const TEMPLATE_ROUTE = '/admin/script/templates'
const TEMPLATE_READY_ENDPOINTS = [
  '/script/template/search',
  '/script/template/save',
  '/script/template/delete',
] as const
const TEMPLATE_CONTEXT_ENDPOINTS = [
  '/script/template/get',
  '/script/template/by-scene',
  '/script/template/use-count',
] as const
const TEMPLATE_ADMIN_ENDPOINTS = [
  '/script/admin/template/list',
  '/script/admin/template/save',
  '/script/admin/template/delete',
] as const
const TEMPLATE_UNSUPPORTED_ACTIONS = [
  'inline-admin-system-template-management',
  'server-export',
  'batch-import',
  'inline-use-count-increment',
] as const

const contractList = (items: readonly string[]) => items.join('|')

const defaultForm: Partial<ScriptTemplate> = {
  templateName: '',
  templateType: 'user',
  scene: 'general',
  content: '',
  description: '',
  status: 1,
}

function templateContent(row: ScriptTemplate) {
  return row.content ?? row.templateContent ?? ''
}

function templateDescription(row: ScriptTemplate) {
  return row.description ?? row.tags ?? row.industry ?? ''
}

function sceneLabel(value?: string) {
  return SCENES.find((item) => item.value === value)?.label ?? value ?? '-'
}

function templateTypeLabel(value?: string) {
  return TEMPLATE_TYPES.find((item) => item.value === value)?.label ?? value ?? '用户模板'
}

export default function ScriptTemplatePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState<ScriptTemplateQuery>({ page: 0, rows: 20 })
  const [draft, setDraft] = useState<ScriptTemplateQuery>({ page: 0, rows: 20 })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ScriptTemplate>>(defaultForm)
  const [deleteTarget, setDeleteTarget] = useState<ScriptTemplate | null>(null)
  const [formError, setFormError] = useState('')
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['script-templates', search],
    queryFn: () => scriptApi.templateSearch(search),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<ScriptTemplate>) => scriptApi.templateSave(p),
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormError('')
      setFormOpen(false)
      qc.invalidateQueries({ queryKey: ['script-templates'] })
    },
    onError: (e, payload) => {
      const message = `话术模板保存失败（POST /script/template/save）：${getErrorMessage(e)}。接口来源：/script/template/save（route=${TEMPLATE_ROUTE}; templateId=${payload.id ?? '新增'}; templateName=${payload.templateName ?? '空'}; templateType=${payload.templateType ?? 'user'}; scene=${payload.scene ?? 'general'}; contentLength=${String(payload.content ?? payload.templateContent ?? '').length}）`
      setFormError(message)
      toast(message, 'error')
    },
  })

  const delMut = useMutation({
    mutationFn: (row: ScriptTemplate) => scriptApi.templateDelete(row.id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setActionError('')
      setDeleteTarget(null)
      qc.invalidateQueries({ queryKey: ['script-templates'] })
    },
    onError: (e, row) => {
      const message = `话术模板删除失败（POST /script/template/delete）：${getErrorMessage(e)}。接口来源：/script/template/delete（route=${TEMPLATE_ROUTE}; templateId=${row.id}; templateName=${row.templateName}; templateType=${row.templateType ?? 'user'}; scene=${row.scene ?? 'general'}）`
      setActionError(message)
      toast(message, 'error')
    },
  })

  const rows = data?.list ?? []
  const diagnostics = useMemo(() => {
    const enabled = rows.filter((row) => row.status === 1).length
    const system = rows.filter((row) => row.templateType === 'system').length
    const withVars = rows.filter((row) => /\{[^}]+\}/.test(templateContent(row))).length
    return { enabled, disabled: rows.length - enabled, system, withVars }
  }, [rows])

  const openAdd = useCallback(() => {
    setFormError('')
    setForm(defaultForm)
    setFormOpen(true)
  }, [])

  const openEdit = useCallback((row: ScriptTemplate) => {
    setFormError('')
    setForm({
      id: row.id,
      templateName: row.templateName,
      templateType: row.templateType ?? 'user',
      scene: row.scene ?? 'general',
      content: templateContent(row),
      description: templateDescription(row),
      userId: row.userId,
      status: row.status ?? 1,
    })
    setFormOpen(true)
  }, [])

  const handleSearch = useCallback(() => setSearch({ ...draft, page: 0 }), [draft])

  const handleSave = () => {
    setFormError('')
    const templateName = String(form.templateName ?? '').trim()
    const content = String(form.content ?? form.templateContent ?? '').trim()
    if (!templateName) {
      toast('请填写模板名称', 'warning')
      return
    }
    if (!content) {
      toast('请填写模板内容', 'warning')
      return
    }
    saveMut.mutate({
      id: form.id,
      templateName,
      templateType: form.templateType ?? 'user',
      scene: form.scene ?? 'general',
      content,
      description: String(form.description ?? '').trim() || undefined,
      status: Number(form.status ?? 1),
    })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 150 },
    { field: 'templateType', headerName: '类型', width: 100, valueFormatter: (v: string) => templateTypeLabel(v) },
    { field: 'scene', headerName: '场景', width: 110, valueFormatter: (v: string) => sceneLabel(v) },
    {
      field: 'content',
      headerName: '模板内容',
      flex: 1.6,
      minWidth: 220,
      valueGetter: (_, row) => templateContent(row as ScriptTemplate),
      renderCell: ({ row }) => (
        <Typography variant="body2" noWrap title={templateContent(row as ScriptTemplate)}>
          {templateContent(row as ScriptTemplate)}
        </Typography>
      ),
    },
    { field: 'useCount', headerName: '使用次数', width: 90, valueFormatter: (v: number) => v ?? 0 },
    {
      field: 'status',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: '_actions',
      headerName: '操作',
      width: 140,
      sortable: false,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => openEdit(row as ScriptTemplate)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteTarget(row as ScriptTemplate)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField
        size="small"
        label="模板名称"
        value={draft.keyword ?? ''}
        onChange={(e) => setDraft((d) => ({ ...d, keyword: e.target.value }))}
        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        sx={{ width: 160 }}
      />
      <TextField
        select
        size="small"
        label="场景"
        value={draft.scene ?? ''}
        onChange={(e) => setDraft((d) => ({ ...d, scene: e.target.value }))}
        sx={{ width: 130 }}
      >
        <MenuItem value="">全部</MenuItem>
        {SCENES.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
      </TextField>
      <TextField
        select
        size="small"
        label="类型"
        value={draft.templateType ?? ''}
        onChange={(e) => setDraft((d) => ({ ...d, templateType: e.target.value }))}
        sx={{ width: 130 }}
      >
        <MenuItem value="">全部</MenuItem>
        {TEMPLATE_TYPES.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>查询</Button>
    </>
  )

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2, minHeight: 'calc(100vh - 48px)' }}
      data-testid="script-template-workbench"
      data-contract-scope="script-template-business"
      data-ready-endpoints={contractList(TEMPLATE_READY_ENDPOINTS)}
      data-context-endpoints={contractList(TEMPLATE_CONTEXT_ENDPOINTS)}
      data-admin-endpoints={contractList(TEMPLATE_ADMIN_ENDPOINTS)}
      data-unsupported-actions={contractList(TEMPLATE_UNSUPPORTED_ACTIONS)}
      data-keyword={search.keyword ?? ''}
      data-scene={search.scene ?? ''}
      data-template-type={search.templateType ?? ''}
      data-page={search.page ?? 0}
      data-rows={search.rows ?? 20}
      data-row-count={rows.length}
      data-total={data?.total ?? 0}
      data-enabled-count={diagnostics.enabled}
      data-system-count={diagnostics.system}
    >
      <PageHeader
        title="话术模板"
        subtitle="对齐 /script/template：模板正文保存为 content，描述保存为 description，templateType 区分 system/user。"
        breadcrumbs={[{ label: '话术' }, { label: '话术模板' }]}
        actions={<Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增模板</Button>}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="script-template-contract-alert"
        data-contract-gap="business-template-vs-admin-system-template"
        data-no-admin-template-request="true"
        data-no-server-export-request="true"
        data-no-batch-import-request="true"
        data-no-inline-use-count-request="true"
      >
        管理系统模板的专用接口是 <code>/script/admin/template/*</code>；当前业务页使用登录用户模板接口 <code>/script/template/*</code>。
        变量建议写在模板正文中，例如 <code>{'{产品名}'}</code>。
      </Alert>

      <Grid container spacing={2}>
        {[
          { key: 'current-page', label: '当前页模板', value: rows.length, source: '/script/template/search list.length' },
          { key: 'enabled', label: '启用', value: diagnostics.enabled, source: 'local-derived status=1' },
          { key: 'system', label: '系统模板', value: diagnostics.system, source: 'local-derived templateType=system' },
          { key: 'variables', label: '含变量', value: diagnostics.withVars, source: 'local-derived content variables' },
        ].map((item) => (
          <Grid item xs={6} md={3} key={item.key}>
            <Card
              variant="outlined"
              data-testid={`script-template-kpi-${item.key}`}
              data-contract-source={item.source}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <ErrorAlert
          title="话术模板加载失败"
          message={`${getErrorMessage(error)}。接口来源：/script/template/search，请检查登录态和模板表数据。`}
          onRetry={() => refetch()}
        />
      )}
      {saveMut.isError && (
        <Alert severity="error">
          {formError || `保存失败：${getErrorMessage(saveMut.error)}。接口来源：/script/template/save`}
        </Alert>
      )}
      {delMut.isError && (
        <Alert severity="error">
          {actionError || `删除失败：${getErrorMessage(delMut.error)}。接口来源：/script/template/delete`}；列表不会本地移除该模板。
        </Alert>
      )}

      <Box sx={{ flex: 1, minHeight: 460 }}>
        <StandardDataGrid
          rows={rows}
          columns={columns}
          rowCount={data?.total ?? 0}
          loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={(m) => setSearch((s) => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          showExport={false}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      <FormDialog
        open={formOpen}
        title={form.id ? '编辑话术模板' : '新增话术模板'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave}
        loading={saveMut.isPending}
      >
        <Stack spacing={2} sx={{ pt: 1 }}>
          {formError ? (
            <Alert severity="error">
              {formError}。保存失败会保留当前模板表单输入。
            </Alert>
          ) : null}
          <TextField label="模板名称" value={form.templateName ?? ''} onChange={(e) => setForm((f) => ({ ...f, templateName: e.target.value }))} fullWidth />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField select label="类型" value={form.templateType ?? 'user'} onChange={(e) => setForm((f) => ({ ...f, templateType: e.target.value }))} fullWidth>
              {TEMPLATE_TYPES.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
            </TextField>
            <TextField select label="场景" value={form.scene ?? 'general'} onChange={(e) => setForm((f) => ({ ...f, scene: e.target.value }))} fullWidth>
              {SCENES.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
            </TextField>
          </Stack>
          <TextField label="模板内容" value={form.content ?? ''} onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))} fullWidth multiline minRows={6} />
          <TextField label="描述" value={form.description ?? ''} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} fullWidth multiline minRows={2} />
          <TextField select label="状态" value={form.status ?? 1} onChange={(e) => setForm((f) => ({ ...f, status: Number(e.target.value) }))} fullWidth>
            <MenuItem value={1}>启用</MenuItem>
            <MenuItem value={0}>禁用</MenuItem>
          </TextField>
        </Stack>
      </FormDialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        content={`确定要删除该话术模板吗？endpoint=/script/template/delete; templateId=${deleteTarget?.id ?? '-'}; templateName=${deleteTarget?.templateName ?? '未知模板'}。删除失败会保留模板行和当前筛选。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending}
      />
    </Box>
  )
}
