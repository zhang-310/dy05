import { useState } from 'react'
import {
  Alert, Box, Card, CardContent, Grid, Typography, Stack, Button, TextField, Chip, Switch,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay, ErrorAlert } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { slangApi, type SlangEntry, type SlangSave } from '@/api/slangdict'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

const SLANG_ROUTE = '/admin/slangdict'
const SLANG_ENDPOINTS = {
  search: '/slangdict/entry/search',
  save: '/slangdict/entry/save',
  delete: '/slangdict/entry/delete?id=',
} as const
const SLANG_READY_ENDPOINTS = [
  '/slangdict/entry/search',
  '/slangdict/entry/save',
  '/slangdict/entry/delete',
] as const
const SLANG_RELATED_CONTEXT_ENDPOINTS = [
  '/slangdict/entry/by-product',
  '/slangdict/entry/bind-product',
  '/slangdict/entry/unbind-product',
  '/slangdict/entry/ai-generate',
] as const
const SLANG_UNSUPPORTED_ENDPOINTS = [
  '/slangdict/entry/enable',
  '/slangdict/entry/disable',
  '/slangdict/entry/export',
  '/slangdict/entry/batch-import',
] as const
const SLANG_UNSUPPORTED_ACTIONS = [
  'independent-enable-disable',
  'server-export',
  'batch-import',
  'inline-product-binding',
  'main-page-ai-generate',
] as const

const contractList = (items: readonly string[]) => items.join('|')

function formatStatus(status?: number) {
  return Number(status ?? 1) === 1 ? '1/启用' : '0/停用'
}

function entryContext(entry?: Partial<SlangEntry> | null, fallbackId?: number | string) {
  return `route=${SLANG_ROUTE}; entryId=${entry?.id ?? fallbackId ?? '新增'}; phrase=${String(entry?.phrase ?? '').trim() || '未填写'}; category=${String(entry?.category ?? '').trim() || '未填写'}; usageScene=${String(entry?.usageScene ?? '').trim() || '未填写'}; status=${formatStatus(entry?.status)}`
}

export default function SlangDictPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState<{ page: number; rows: number; keyword?: string }>({ page: 0, rows: 20 })
  const [query, setQuery] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<SlangEntry>>({})
  const [deleteTarget, setDeleteTarget] = useState<SlangEntry | null>(null)
  const [formError, setFormError] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [toggleError, setToggleError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['slang-dict', search],
    queryFn: () => slangApi.list(search),
  })

  const saveMut = useMutation({
    mutationFn: (params: SlangSave) => slangApi.save(params),
    onMutate: () => setFormError(''),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); setFormError(''); qc.invalidateQueries({ queryKey: ['slang-dict'] }) },
    onError: (e: unknown, payload) => {
      setFormError(`${SLANG_ENDPOINTS.save} 词条保存失败：${getErrorMessage(e)}（${entryContext(payload)}）`)
      toast('保存失败', 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (entry: SlangEntry) => slangApi.delete(entry.id),
    onMutate: () => setDeleteError(''),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteTarget(null); setDeleteError(''); qc.invalidateQueries({ queryKey: ['slang-dict'] }) },
    onError: (e: unknown, entry) => {
      setDeleteError(`${SLANG_ENDPOINTS.delete}${entry.id} 词条删除失败：${getErrorMessage(e)}（${entryContext(entry)}）`)
      toast('删除失败', 'error')
    },
  })

  const toggleMut = useMutation({
    mutationFn: ({ row, enabled }: { row: SlangEntry; enabled: boolean }) =>
      slangApi.save({
        id: row.id,
        phrase: row.phrase,
        meaning: row.meaning,
        category: row.category,
        usageScene: row.usageScene,
        example: row.example,
        source: row.source,
        status: enabled ? 1 : 0,
      }),
    onMutate: () => setToggleError(''),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slang-dict'] }),
    onError: (e: unknown, variables) => {
      const targetStatus = variables.enabled ? 1 : 0
      setToggleError(`${SLANG_ENDPOINTS.save} 词条启停失败：${getErrorMessage(e)}（${entryContext(variables.row)}; targetStatus=${formatStatus(targetStatus)}; strategy=启停通过保存 status 降级实现，后端没有独立 enable/disable 接口）`)
      toast('状态更新失败', 'error')
    },
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0
  const enabledCount = rows.filter(r => Number(r.status ?? 1) === 1).length
  const disabledCount = rows.length - enabledCount

  const handleSave = () => {
    const phrase = String(form.phrase ?? '').trim()
    if (!phrase) { toast('请填写梗/暗语', 'warning'); return }
    saveMut.mutate({
      id: form.id,
      phrase,
      meaning: String(form.meaning ?? '').trim() || undefined,
      category: String(form.category ?? '').trim() || undefined,
      usageScene: String(form.usageScene ?? '').trim() || undefined,
      example: String(form.example ?? '').trim() || undefined,
      source: String(form.source ?? '').trim() || undefined,
      status: Number(form.status ?? 1),
    })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'phrase', headerName: '梗/暗语', width: 140, renderCell: ({ value }) => <Typography variant="body2" fontWeight={600}>{String(value ?? '')}</Typography> },
    { field: 'meaning', headerName: '释义', flex: 1, minWidth: 180 },
    { field: 'example', headerName: '示例', flex: 1, minWidth: 160 },
    { field: 'category', headerName: '分类', width: 100, renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'usageScene', headerName: '使用场景', width: 120, renderCell: ({ value }) => String(value ?? '-') },
    { field: 'useCount', headerName: '使用次数', width: 90, type: 'number', valueFormatter: (v) => v ?? 0 },
    { field: 'status', headerName: '状态', width: 90,
      renderCell: ({ row }) => (
        <Switch size="small" checked={Number((row as SlangEntry).status ?? 1) === 1}
          disabled={toggleMut.isPending}
          onChange={e => toggleMut.mutate({ row: row as SlangEntry, enabled: e.target.checked })} />
      ) },
    { field: 'createTime', headerName: '创建时间', width: 150, valueFormatter: (v) => v ? formatDate(String(v)) : '—' },
    { field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => { setForm(row as SlangEntry); setFormError(''); setFormOpen(true) }}>编辑</Button>
          <Button size="small" color="error" onClick={() => { setDeleteError(''); setDeleteTarget(row as SlangEntry) }}>删除</Button>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <>
      <TextField size="small" placeholder="搜索术语..." sx={{ width: 200 }}
        value={query}
        onChange={e => setQuery(e.target.value)} />
      <Button variant="contained" onClick={() => setSearch(s => ({ ...s, keyword: query.trim() || undefined, page: 0 }))}>查询</Button>
      <Button onClick={() => { setQuery(''); setSearch(s => ({ ...s, keyword: undefined, page: 0 })) }}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button
      variant="contained"
      startIcon={<AddIcon />}
      onClick={() => { setForm({ status: 1 }); setFormError(''); setFormOpen(true) }}
    >
      新增词条
    </Button>
  )
  const searchContext = `route=${SLANG_ROUTE}; keyword=${search.keyword ?? '空'}; page=${search.page}; rows=${search.rows}`

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
      data-testid="slang-dict-workbench"
      data-contract-scope="content-slangdict-main"
      data-ready-endpoints={contractList(SLANG_READY_ENDPOINTS)}
      data-related-context-endpoints={contractList(SLANG_RELATED_CONTEXT_ENDPOINTS)}
      data-unsupported-endpoints={contractList(SLANG_UNSUPPORTED_ENDPOINTS)}
      data-unsupported-actions={contractList(SLANG_UNSUPPORTED_ACTIONS)}
      data-keyword={search.keyword ?? ''}
      data-page={search.page}
      data-rows={search.rows}
      data-row-count={rows.length}
      data-total={total}
      data-api-normalize="slangApi.list:normalizePage(slangEntries|entries)"
      data-enabled-count={enabledCount}
      data-disabled-count={disabledCount}
      data-no-local-slang-fallback="true"
      data-no-local-status-mutation="true"
      data-row-retained-on-action-error={formError || deleteError || toggleError ? 'true' : 'false'}
    >
      <PageHeader
        title="行业俚语词典"
        subtitle="管理 slangdict entry：phrase / meaning / category / usageScene / example，供话术生成和商品暗语链路引用。"
        breadcrumbs={[{ label: '内容' }, { label: '梗库' }]}
        actions={actionSlot}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="slang-dict-contract-alert"
        data-contract-gap="main-page-context-boundary"
        data-enable-disable-strategy="save-status"
        data-no-inline-product-binding-request="true"
        data-no-ai-generate-request="true"
        data-no-server-export-request="true"
      >
        词典主工作台真实接口：<code>/slangdict/entry/search</code>、<code>/save</code>、<code>/delete?id=</code>。
        后端没有独立 enable/disable 接口，启停通过保存 <code>status</code> 完成；商品绑定和 AI 生成已有上下文接口，但本页不内联触发。
      </Alert>

      <Grid container spacing={2}>
        {[
          { key: 'current-page', label: '当前页词条', value: rows.length, source: '/slangdict/entry/search list.length' },
          { key: 'enabled', label: '启用', value: enabledCount, source: 'local-derived status=1' },
          { key: 'disabled', label: '停用', value: disabledCount, source: 'local-derived status=0' },
          { key: 'total', label: '总量', value: total, source: '/slangdict/entry/search total' },
        ].map((item) => (
          <Grid item xs={6} md={3} key={item.key}>
            <Card
              variant="outlined"
              data-testid={`slang-dict-kpi-${item.key}`}
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
          title="梗库加载失败"
          message={`${SLANG_ENDPOINTS.search} 加载失败：${getErrorMessage(error)}（${searchContext}）。请检查登录态、数据权限和 content 模块 slangdict 查询链路。`}
          onRetry={() => refetch()}
        />
      )}

      {formError && <Alert data-testid="slang-save-error" data-contract-source={SLANG_ENDPOINTS.save} data-input-retained="true" data-no-local-slang-fallback="true" severity="error">{formError}。保存失败会保留当前表单输入，不会写入本地假词条。</Alert>}
      {deleteError && <Alert data-testid="slang-delete-error" data-contract-source="/slangdict/entry/delete" data-row-retained="true" data-no-local-slang-fallback="true" severity="error">{deleteError}。删除失败不会本地移除词条，确认上下文会保留。</Alert>}
      {toggleError && <Alert data-testid="slang-toggle-error" data-contract-source={SLANG_ENDPOINTS.save} data-row-retained="true" data-no-local-status-mutation="true" severity="error">{toggleError}。失败不会本地切换词条状态。</Alert>}

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
        showExport={false}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ height: 520 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑词条' : '新增词条'}
        onClose={() => setFormOpen(false)} onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {formError && <Alert data-testid="slang-form-save-error" data-contract-source={SLANG_ENDPOINTS.save} data-input-retained="true" severity="error">{formError}。保存失败会保留当前输入。</Alert>}
          <TextField label="梗/暗语" value={form.phrase ?? ''} onChange={e => setForm(f => ({ ...f, phrase: e.target.value }))} fullWidth />
          <TextField label="释义" value={form.meaning ?? ''} onChange={e => setForm(f => ({ ...f, meaning: e.target.value }))} fullWidth multiline minRows={2} />
          <TextField label="示例用法" value={form.example ?? ''} onChange={e => setForm(f => ({ ...f, example: e.target.value }))} fullWidth multiline minRows={2} />
          <TextField label="分类" value={form.category ?? ''} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} fullWidth />
          <TextField label="使用场景" value={form.usageScene ?? ''} onChange={e => setForm(f => ({ ...f, usageScene: e.target.value }))} fullWidth />
          <TextField label="来源" value={form.source ?? ''} onChange={e => setForm(f => ({ ...f, source: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        content={`确定删除「${deleteTarget?.phrase ?? ''}」吗？endpoint=${SLANG_ENDPOINTS.delete}${deleteTarget?.id ?? '-'}; ${entryContext(deleteTarget)}。删除失败时不会本地移除该词条。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && deleteMut.mutate(deleteTarget)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
