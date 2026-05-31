import { useMemo, useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Grid, Stack, TextField, MenuItem, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay, ErrorAlert } from '@/components/base'
import { aiApi } from '@/api/ai'
import type { AiKnowledgeSourceVO } from '@/types/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray, readTotal } from '@/utils/response-normalize'

type KbSource = AiKnowledgeSourceVO

const SOURCE_TYPES = [
  { value: 'local', label: '本地目录' },
  { value: 'remote', label: '远程来源' },
  { value: 'document', label: '文档源' },
  { value: 'qa', label: 'FAQ 源' },
]

const STATUS_MAP: Record<number, { label: string; color: 'success' | 'default' | 'error' }> = {
  1: { label: '启用', color: 'success' },
  0: { label: '停用', color: 'default' },
}

const KNOWLEDGE_SOURCE_READY_ENDPOINTS = [
  '/ai/admin/knowledge-source/search',
  '/ai/admin/knowledge-source/save',
  '/ai/admin/knowledge-source/delete',
].join('|')

const KNOWLEDGE_SOURCE_UNSUPPORTED_ENDPOINTS = [
  '/ai/admin/knowledge-source/get',
  '/ai/admin/knowledge-source/test-connection',
  '/ai/admin/knowledge-source/sync',
  '/ai/admin/knowledge-source/import-local',
  '/ai/knowledge-base/source/sync',
  '/ai/knowledge-base/source/test-connection',
  '/ai/knowledge-base/source/scan-local',
  '/ai/index-queue/local-create',
].join('|')

export default function KnowledgeSourcePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [filters, setFilters] = useState({ keyword: '', sourceType: '', status: '' })
  const [query, setQuery] = useState(filters)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<KbSource>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['kb-sources', page, pageSize, query],
    queryFn: () => aiApi.kbSourceList({
      page,
      rows: pageSize,
      keyword: query.keyword || undefined,
      sourceType: query.sourceType || undefined,
      status: query.status === '' ? undefined : Number(query.status),
    }),
  })

  const saveMut = useMutation({
    mutationFn: (params: { id?: number; sourceName: string; sourcePath: string; sourceType?: string; status?: number }) => aiApi.kbSourceSave(params),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['kb-sources'] }) },
    onError: (e: Error) => toast(`保存失败：${getErrorMessage(e)}`, 'error'),
  })

  const delMut = useMutation({
    mutationFn: (id: number) => aiApi.kbSourceDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['kb-sources'] }) },
    onError: (e: Error) => toast(`删除失败：${getErrorMessage(e)}`, 'error'),
  })

  const rows = normalizeArray<KbSource>(data)
  const total = readTotal(data, rows.length)
  const deleteTarget = useMemo(() => rows.find(row => row.id === deleteId), [deleteId, rows])
  const saveErrorText = saveMut.isError
    ? `保存失败（POST /ai/admin/knowledge-source/save）：${getErrorMessage(saveMut.error)}。编辑弹窗、来源名称、sourcePath、sourceType 和状态会保留。`
    : null
  const deleteErrorText = delMut.isError && deleteId != null
    ? `删除失败（POST /ai/admin/knowledge-source/delete?id=${deleteId}）：${getErrorMessage(delMut.error)}。知识来源行「${deleteTarget?.sourceName ?? deleteId}」会保留。`
    : null

  const summary = useMemo(() => rows.reduce(
    (acc, row) => {
      acc.fileCount += Number(row.fileCount ?? 0)
      acc.indexCount += Number(row.indexCount ?? 0)
      if (Number(row.status ?? 1) === 1) acc.active += 1
      if (Number(row.indexCount ?? 0) > 0) acc.indexed += 1
      return acc
    },
    { active: 0, indexed: 0, fileCount: 0, indexCount: 0 },
  ), [rows])

  const openCreate = () => {
    saveMut.reset()
    setForm({ sourceName: '', sourcePath: '', sourceType: 'local', status: 1 })
    setFormOpen(true)
  }

  const openEdit = (row: KbSource) => {
    saveMut.reset()
    setForm({ ...row, sourceType: row.sourceType ?? 'local', status: row.status ?? 1 })
    setFormOpen(true)
  }

  const handleSave = () => {
    const sourceName = String(form.sourceName ?? '').trim()
    const sourcePath = String(form.sourcePath ?? '').trim()
    if (!sourceName) {
      toast('请填写知识源名称', 'warning')
      return
    }
    if (!sourcePath) {
      toast('请填写后端可访问的知识源路径', 'warning')
      return
    }
    saveMut.mutate({
      id: form.id,
      sourceName,
      sourcePath,
      sourceType: String(form.sourceType ?? 'local'),
      status: Number(form.status ?? 1),
    })
  }

  const cols: GridColDef[] = [
    { field: 'sourceName', headerName: '来源名称', minWidth: 160, flex: 0.8 },
    {
      field: 'sourcePath',
      headerName: '后端路径',
      minWidth: 240,
      flex: 1.2,
      renderCell: ({ value }) => (
        <Typography variant="body2" noWrap title={String(value ?? '')}>
          {String(value ?? '—')}
        </Typography>
      ),
    },
    { field: 'sourceType', headerName: '来源类型', width: 120,
      renderCell: ({ value }) => {
        const t = SOURCE_TYPES.find(s => s.value === value)
        return <Chip label={t?.label ?? value} size="small" />
      },
    },
    { field: 'fileCount', headerName: '文件数', width: 90, type: 'number', valueFormatter: (v) => v ?? 0 },
    { field: 'indexCount', headerName: '索引数', width: 90, type: 'number', valueFormatter: (v) => v ?? 0 },
    {
      field: 'status',
      headerName: '状态',
      width: 110,
      renderCell: ({ value }) => {
        const key = Number(value ?? 1)
        const s = STATUS_MAP[key] ?? { label: `未知(${String(value)})`, color: 'error' as const }
        const icon = key === 1 ? <CheckCircleIcon /> : <ErrorIcon />
        return <Chip icon={icon} label={s.label} size="small" color={s.color} />
      },
    },
    { field: 'lastIndexTime', headerName: '上次索引', width: 160, valueFormatter: (v) => v ? formatDate(String(v)) : '—' },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v) => v ? formatDate(String(v)) : '—' },
    { field: 'actions', headerName: '操作', width: 150, sortable: false,
      renderCell: ({ row }) => {
        const r = row as KbSource
        return (
          <Stack direction="row" spacing={0.5}>
            <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(r)}>编辑</Button>
            <Button
              size="small"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={() => {
                delMut.reset()
                setDeleteId(r.id)
              }}
            >
              删除
            </Button>
          </Stack>
        )
      },
    },
  ]

  const searchSlot = (
    <Stack
      direction="row"
      spacing={1}
      alignItems="center"
      flexWrap="wrap"
      data-testid="knowledge-source-search-contract"
      data-source-endpoint="/ai/admin/knowledge-source/search"
      data-server-filter="true"
    >
      <TextField
        label="名称关键词"
        size="small"
        value={filters.keyword}
        onChange={e => setFilters(s => ({ ...s, keyword: e.target.value }))}
        onKeyDown={e => {
          if (e.key === 'Enter') {
            setPage(0)
            setQuery(filters)
          }
        }}
        sx={{ width: 160 }}
      />
      <TextField
        label="类型"
        size="small"
        select
        value={filters.sourceType}
        onChange={e => setFilters(s => ({ ...s, sourceType: e.target.value }))}
        sx={{ width: 132 }}
      >
        <MenuItem value="">全部</MenuItem>
        {SOURCE_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
      </TextField>
      <TextField
        label="状态"
        size="small"
        select
        value={filters.status}
        onChange={e => setFilters(s => ({ ...s, status: e.target.value }))}
        sx={{ width: 112 }}
      >
        <MenuItem value="">全部</MenuItem>
        <MenuItem value="1">启用</MenuItem>
        <MenuItem value="0">停用</MenuItem>
      </TextField>
      <Button
        variant="contained"
        size="small"
        onClick={() => {
          setPage(0)
          setQuery(filters)
        }}
      >
        搜索
      </Button>
    </Stack>
  )

  return (
    <Box
      data-testid="knowledge-source-page"
      data-ready-endpoints={KNOWLEDGE_SOURCE_READY_ENDPOINTS}
      data-unsupported-endpoints={KNOWLEDGE_SOURCE_UNSUPPORTED_ENDPOINTS}
      data-no-browser-local-file-access="true"
      data-no-legacy-source-sync="true"
      data-no-direct-index-mutation="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <PageHeader
        title="知识来源管理"
        breadcrumbs={[{ label: 'AI中心' }, { label: '知识来源' }]}
        subtitle="管理 ai_knowledge_source 中的后端可访问目录/来源；实际入库和向量化仍走知识库导入、索引队列和文档同步链路。"
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>新增来源</Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="knowledge-source-boundary-contract"
        data-source-endpoints={KNOWLEDGE_SOURCE_READY_ENDPOINTS}
        data-no-detail-fetch="true"
        data-no-legacy-source-sync="true"
        data-no-browser-local-file-access="true"
      >
        真实接口：<code>POST /ai/admin/knowledge-source/search</code>、<code>/get</code>、<code>/save</code>、<code>/delete</code>。
        详情接口存在，但当前表格编辑直接复用行内数据，不额外拉取 <code>/get</code>。
        当前后端尚未提供知识源“连接测试/同步扫描”接口，本页不再调用旧的 <code>/ai/knowledge-base/source/sync</code> 或 <code>/test-connection</code>。
      </Alert>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          ['当前页来源', rows.length],
          ['启用来源', summary.active],
          ['已产生索引', summary.indexed],
          ['索引片段', summary.indexCount],
        ].map(([label, value]) => (
          <Grid item xs={6} md={3} key={String(label)}>
            <Card
              variant="outlined"
              data-testid="knowledge-source-summary-contract"
              data-no-client-index-synthesis="true"
            >
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
          data-testid="knowledge-source-list-error"
          data-source-endpoint="/ai/admin/knowledge-source/search"
          data-no-local-source-fallback="true"
        >
          <ErrorAlert
            title="知识来源加载失败"
            message={`${getErrorMessage(error)}。请检查管理员权限、POST /ai/admin/knowledge-source/search 和登录态。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}

      {deleteErrorText && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="knowledge-source-delete-error"
          data-source-endpoint="/ai/admin/knowledge-source/delete"
          data-no-local-delete-mutation="true"
        >
          {deleteErrorText}
        </Alert>
      )}

      <Box
        sx={{ flex: 1, minHeight: 360, mt: isError ? 2 : 0 }}
        data-testid="knowledge-source-grid-contract"
        data-source-endpoint="/ai/admin/knowledge-source/search"
        data-no-detail-fetch="true"
        data-no-direct-index-mutation="true"
      >
      <StandardDataGrid
        rows={rows} columns={cols} loading={isFetching}
        rowCount={total} paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as KbSource).id}
        searchSlot={searchSlot}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
      </Box>
      <FormDialog
        open={formOpen} title={form.id ? '编辑来源' : '新增来源'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave}
        loading={saveMut.isPending}
      >
        <Stack
          spacing={2}
          sx={{ pt: 1 }}
          data-testid="knowledge-source-form-contract"
          data-source-endpoint="/ai/admin/knowledge-source/save"
          data-input-retained="true"
          data-no-browser-local-file-access="true"
        >
          {saveErrorText && (
            <Alert
              severity="error"
              data-testid="knowledge-source-save-error"
              data-source-endpoint="/ai/admin/knowledge-source/save"
              data-input-retained="true"
            >
              {saveErrorText}
            </Alert>
          )}
          <TextField label="来源名称" required fullWidth value={form.sourceName ?? ''} onChange={e => setForm({ ...form, sourceName: e.target.value })} />
          <TextField label="来源类型" required fullWidth select value={form.sourceType ?? ''} onChange={e => setForm({ ...form, sourceType: e.target.value })}>
            {SOURCE_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
          </TextField>
          <TextField
            label="知识源路径"
            required
            fullWidth
            multiline
            rows={3}
            value={form.sourcePath ?? ''}
            onChange={e => setForm({ ...form, sourcePath: e.target.value })}
            helperText="填写后端容器可访问的 sourcePath，例如 /data/knowledge 或挂载目录；不是浏览器本地文件路径。"
          />
          <TextField label="状态" required fullWidth select value={String(form.status ?? 1)} onChange={e => setForm({ ...form, status: Number(e.target.value) })}>
            <MenuItem value="1">启用</MenuItem>
            <MenuItem value="0">停用</MenuItem>
          </TextField>
        </Stack>
      </FormDialog>
      <ConfirmDialog
        open={deleteId !== null}
        title="确认删除"
        content={deleteErrorText ? `${deleteErrorText}\n\n确认继续重试删除此知识来源？` : '确认删除此知识来源？'}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        onClose={() => setDeleteId(null)}
        loading={delMut.isPending}
      />
    </Box>
  )
}

