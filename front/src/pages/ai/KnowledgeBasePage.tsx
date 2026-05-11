import { useState, useCallback, useEffect } from 'react'
import {
  Box, TextField, Button, Stack, Drawer, Typography,
  Tab, Tabs, Chip, CircularProgress, IconButton,
  LinearProgress, Pagination, Paper, Grid, Alert,
} from '@mui/material'
import Accordion from '@mui/material/Accordion'
import AccordionSummary from '@mui/material/AccordionSummary'
import AccordionDetails from '@mui/material/AccordionDetails'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import AddIcon from '@mui/icons-material/Add'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import DeleteIcon from '@mui/icons-material/Delete'
import SearchIcon from '@mui/icons-material/Search'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import RefreshIcon from '@mui/icons-material/Refresh'
import CloseIcon from '@mui/icons-material/Close'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { aiApi, type KnowledgeBase, type KbDocument } from '@/api/ai'
import type { KbIndexQueueRow, EvolutionFitnessRecordVO } from '@/types/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

interface RagChunk {
  title?: string
  docTitle?: string
  content: string
  score: number
  source?: string
}

/** 后端 score 可能为 0–1 相似度或 0–100 */
function ragScoreLabel(score: number): string {
  if (!Number.isFinite(score)) return '相关度 —'
  const pct = score > 1 ? Math.min(100, Math.round(score)) : Math.round(score * 100)
  return `相关度 ${pct}%`
}

function ragScoreColor(score: number): 'success' | 'warning' | 'default' {
  if (!Number.isFinite(score)) return 'default'
  const n = score > 1 ? score / 100 : score
  if (n > 0.8) return 'success'
  if (n > 0.5) return 'warning'
  return 'default'
}

interface IndexQueueItem {
  id: number
  summary: string
  status: string
  chipColor: 'default' | 'warning' | 'success' | 'error'
  chipLabel: string
  createTime: string
  errorMsg?: string
  retryCount: number
  sourceType?: string
}

/** 根据后端 error_msg 给出简短排查提示（与 KnowledgeBaseServiceImpl / 索引消费者一致） */
function hintForIndexQueueError(errorMsg: string): string | null {
  const msg = errorMsg.toLowerCase()
  if (msg.includes('jsonb') || (msg.includes('metadata') && msg.includes('character varying'))) {
    return '数据库 `ai_kb_document.metadata` 需为 TEXT（与 JPA 一致）。请执行 Flyway `V148` 或手工将列从 JSONB 改为 TEXT 后重试入队。'
  }
  if (msg.includes('request body is required') || msg.includes('parse_exception')) {
    return 'Elasticsearch 收到空 bulk 请求（常见于分块数为 0 仍删索引）。请更新后端或等待修复版本后重试。'
  }
  if (msg.includes('recovering') || msg.includes('do not found any channel')) {
    return 'Milvus 集合加载中或不可用，请等待集群就绪或检查 Milvus/etcd 与集合名 `kb_*`。'
  }
  if (msg.includes('field value cannot be empty') || msg.includes('无可索引分块')) {
    return '无向量可写入（内容过短或 chunk 去重全部被跳过）。请加长正文或调整去重/分块配置。'
  }
  if (msg.includes('嵌入') || msg.includes('embedding') || msg.includes('生成嵌入向量')) {
    return '嵌入服务失败：检查 `AI_EMBEDDING_*`、模型可用性及网络；查看 `application` 日志中「上传文档失败」。'
  }
  if (msg.includes('上传文档失败') || msg.includes('批量索引')) {
    return '入库链路异常（向量/ES/库）。请同时查看运维「基础设施」中 ES、Milvus 与 PostgreSQL 状态。'
  }
  return null
}

function mapIndexQueueRow(row: KbIndexQueueRow): IndexQueueItem {
  const id = row.id
  const sourceType = row.sourceType ?? ''
  const sourceId = row.sourceId
  const preview = (row.contentPreview ?? '').trim()
  const summary = preview !== ''
    ? preview
    : [sourceType, sourceId != null ? `#${sourceId}` : ''].filter(Boolean).join(' ') || `任务 #${id}`
  const st = row.status ?? 'pending'
  const statusUi: Record<string, { label: string; chipColor: IndexQueueItem['chipColor'] }> = {
    pending: { label: '待处理', chipColor: 'default' },
    processing: { label: '处理中', chipColor: 'warning' },
    done: { label: '已完成', chipColor: 'success' },
    failed: { label: '失败', chipColor: 'error' },
  }
  const u = statusUi[st] ?? { label: st, chipColor: 'default' as const }
  const rc = typeof row.retryCount === 'number' ? row.retryCount : Number(row.retryCount ?? 0)
  return {
    id,
    summary,
    status: st,
    chipColor: u.chipColor,
    chipLabel: u.label,
    createTime: row.createTime != null ? String(row.createTime) : '',
    errorMsg: row.errorMsg != null ? String(row.errorMsg) : undefined,
    retryCount: Number.isFinite(rc) ? rc : 0,
    sourceType: row.sourceType != null ? String(row.sourceType) : undefined,
  }
}

const DOC_PAGE_SIZE = 20

export default function KnowledgeBasePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, name: '' })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<KnowledgeBase>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null)
  const [drawerTab, setDrawerTab] = useState(0)
  const [ragQuery, setRagQuery] = useState('')
  const [ragResults, setRagResults] = useState<RagChunk[]>([])
  const [ragLoading, setRagLoading] = useState(false)
  const [deleteDocId, setDeleteDocId] = useState<number | null>(null)
  const [docPage, setDocPage] = useState(0)
  const [viewDoc, setViewDoc] = useState<KbDocument | null>(null)

  // 切换知识库时重置文档分页
  useEffect(() => { setDocPage(0) }, [selectedKb?.id])

  // KB list: backend returns List<AiKnowledgeBase>
  const { data: rawList, isFetching } = useQuery({
    queryKey: ['knowledge-bases', search],
    queryFn: () => aiApi.kbList(search),
  })
  const list = Array.isArray(rawList)
    ? rawList.filter(kb => !search.name || (kb.kbName ?? '').includes(search.name))
    : []

  // Documents: backend returns PageResultVO<AiKbDocument>（服务端分页）
  const { data: docListRaw, isFetching: docFetching } = useQuery({
    queryKey: ['kb-docs', selectedKb?.id, docPage],
    queryFn: () => aiApi.docList(selectedKb!.id, { page: docPage, rows: DOC_PAGE_SIZE }),
    enabled: selectedKb != null && drawerTab === 0,
  })
  const docPageData = docListRaw
  const docs: KbDocument[] = docPageData?.list ?? []
  const docTotal = docPageData?.total ?? 0
  const docTotalPages = Math.max(1, Math.ceil(docTotal / DOC_PAGE_SIZE))

  // Index queue
  const {
    data: indexQueueRaw,
    isFetching: indexFetching,
    isError: indexQueueError,
    error: indexQueueErr,
    refetch: refetchIndexQueue,
  } = useQuery({
    queryKey: ['kb-index-queue', selectedKb?.id],
    queryFn: () => aiApi.indexQueueList(selectedKb!.id, { page: 0, rows: 50 }),
    enabled: selectedKb != null && drawerTab === 2,
    refetchInterval: 5000,
    retry: 2,
  })
  const indexQueue: IndexQueueItem[] = (indexQueueRaw?.list ?? []).map((row: KbIndexQueueRow) => mapIndexQueueRow(row))
  const indexFailedCount = indexQueue.filter(i => i.status === 'failed').length
  const indexPendingCount = indexQueue.filter(i => i.status === 'pending' || i.status === 'processing').length

  // Evolution fitness（适应度时间线）
  const { data: fitnessRaw, isFetching: fitnessFetching } = useQuery({
    queryKey: ['kb-evolution-fitness', selectedKb?.id],
    queryFn: () => aiApi.evolutionFitnessList(selectedKb!.id, { page: 0, rows: 50 }),
    enabled: selectedKb != null && drawerTab === 3,
    refetchInterval: 15000,
  })
  const fitnessRows: EvolutionFitnessRecordVO[] = fitnessRaw?.list ?? []

  const saveMut = useMutation({
    mutationFn: (params: Partial<KnowledgeBase>) => aiApi.kbCreate({ name: params.kbName, description: params.description }),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['knowledge-bases'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: (id: number) => aiApi.kbDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['knowledge-bases'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delDocMut = useMutation({
    mutationFn: (id: number) => aiApi.docDelete(id),
    onSuccess: () => { toast('文档已删除', 'success'); setDeleteDocId(null); qc.invalidateQueries({ queryKey: ['kb-docs'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm({}); setFormOpen(true) }, [])
  const openEdit = useCallback((row: KnowledgeBase) => { setForm(row); setFormOpen(true) }, [])
  const openDrawer = useCallback((row: KnowledgeBase, tab = 0) => {
    setSelectedKb(row); setDrawerTab(tab); setRagResults([])
  }, [])

  // Search: 后端返回 List<SearchResult>（解包后为数组），非 { chunks: [] }
  const handleRagSearch = async () => {
    if (!selectedKb || !ragQuery.trim()) return
    setRagLoading(true)
    try {
      const res = await aiApi.kbSearch(selectedKb.id, { query: ragQuery, topK: 5 })
      const hits = Array.isArray(res) ? res : []
      const chunks: RagChunk[] = hits.map(hit => {
        const score = typeof hit.score === 'number' ? hit.score : Number(hit.score)
        return {
          title: hit.title,
          docTitle: hit.title,
          content: hit.content ?? '',
          score: Number.isFinite(score) ? score : 0,
          source: hit.source,
        }
      })
      setRagResults(chunks)
    } catch (e) {
      const message = e instanceof Error ? e.message : '检索失败'
      toast(message, 'error')
    } finally {
      setRagLoading(false)
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'kbName', headerName: '知识库名称', flex: 1,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" spacing={0.5}>
          <Typography variant="body2" fontWeight={600}>{row.kbName}</Typography>
          {row.kbType && (
            <Chip label={row.kbType} size="small" variant="outlined" sx={{ fontSize: 10 }} />
          )}
        </Stack>
      ),
    },
    { field: 'totalDocuments', headerName: '文档数', width: 90,
      renderCell: ({ value }) => (
        <Chip label={value ?? 0} size="small" variant="outlined" />
      ),
    },
    { field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => (
        <Chip label={value === 1 ? '就绪' : '构建中'} size="small"
          color={value === 1 ? 'success' : 'warning'} variant="outlined" />
      ),
    },
    { field: 'updateTime', headerName: '最近更新', width: 160,
      valueFormatter: (v: string) => v ? formatDate(v) : '—',
    },
    {
      field: 'actions', headerName: '操作', width: 220, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" gap={0.5}>
          <Button size="small" startIcon={<SearchIcon fontSize="inherit" />}
            onClick={() => openDrawer(row, 1)}>检索</Button>
          <Button size="small" startIcon={<LibraryBooksIcon fontSize="inherit" />}
            onClick={() => openDrawer(row, 0)}>文档</Button>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField label="知识库名称" size="small" value={query.name}
        onChange={e => setQuery(q => ({ ...q, name: e.target.value }))}
        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })} sx={{ bgcolor: 'var(--color-primary)', color: '#000', '&:hover': { bgcolor: 'var(--color-primary-dark)' } }}>查询</Button>
      <Button onClick={() => { setQuery(q => ({ ...q, name: '' })); setSearch({ page: 0, rows: 20, name: '' }) }} sx={{ color: 'var(--color-primary)', borderColor: 'var(--color-primary)' }} variant="outlined">重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd} sx={{ bgcolor: 'var(--color-primary)', color: '#000', '&:hover': { bgcolor: 'var(--color-primary-dark)' } }}>新建知识库</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)' }}>
      <Typography variant="h5" sx={{ mb: 'var(--spacing-lg)', fontWeight: 700, color: 'var(--color-text-primary)' }}>知识库</Typography>
      <StandardDataGrid rows={list} columns={columns} loading={isFetching}
        paginationMode="client" searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }} />

      <FormDialog open={formOpen} title={form.id ? '编辑知识库' : '新建知识库'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="名称" value={form.kbName ?? ''}
            onChange={e => setForm(f => ({ ...f, kbName: e.target.value }))} fullWidth
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
          <TextField label="描述" value={form.description ?? ''}
            onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth multiline minRows={3}
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId != null} title="确认删除"
        content="删除后知识库及所有文档将无法恢复，确认删除？"
        onClose={() => setDeleteId(null)} onConfirm={() => deleteId != null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />

      <ConfirmDialog open={deleteDocId != null} title="确认删除文档"
        content="确认删除该文档？"
        onClose={() => setDeleteDocId(null)}
        onConfirm={() => deleteDocId != null && delDocMut.mutate(deleteDocId)}
        loading={delDocMut.isPending} />

      {/* 详情 Drawer — MUI 默认主题色与 Paper */}
      <Drawer
        anchor="right"
        open={selectedKb != null}
        onClose={() => setSelectedKb(null)}
        PaperProps={{
          sx: {
            width: { xs: '100%', sm: 'min(100vw - 24px, 720px)', md: 'min(100vw - 48px, 900px)' },
          },
        }}
      >
        {selectedKb && (
          <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'background.paper' }}>
            <Box sx={{ px: 2, py: 2, borderBottom: 1, borderColor: 'divider' }}>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Stack spacing={0.5}>
                  <Typography variant="subtitle1" fontWeight={600}>{selectedKb.kbName}</Typography>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                    <Chip label={`${selectedKb.totalDocuments ?? 0} 篇文档`} size="small" variant="outlined" />
                    <Chip label={selectedKb.status === 1 ? '就绪' : '构建中'} size="small"
                      color={selectedKb.status === 1 ? 'success' : 'warning'} variant="outlined" />
                  </Stack>
                </Stack>
                <IconButton size="small" onClick={() => setSelectedKb(null)} aria-label="关闭">
                  <CloseIcon />
                </IconButton>
              </Stack>
            </Box>

            <Tabs value={drawerTab} onChange={(_, v) => setDrawerTab(v)} sx={{ px: 2, borderBottom: 1, borderColor: 'divider' }}>
              <Tab label="文档列表" />
              <Tab label="RAG检索" />
              <Tab label="索引队列" />
              <Tab label="适应度" />
            </Tabs>

            <Box sx={{ flex: 1, overflow: 'auto', p: 2, bgcolor: 'background.default' }}>
              {/* Tab 0: 文档列表（服务端分页） */}
              {drawerTab === 0 && (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, height: '100%' }}>
                  {docFetching ? <CircularProgress size={24} /> : (
                    <>
                      <Stack spacing={1} sx={{ flex: 1, overflow: 'auto' }}>
                        {docs.length === 0 && (
                          <Typography color="text.secondary" variant="body2">暂无文档</Typography>
                        )}
                        {docs.map(doc => {
                          const qs = doc.qualityHeuristicScore
                          const expired = doc.expiryStatus === 2
                          return (
                            <Box
                              key={doc.id}
                              sx={{
                                p: 2,
                                border: 1,
                                borderRadius: 1,
                                borderColor: expired ? 'error.light' : 'divider',
                                bgcolor: expired ? 'error.50' : 'background.paper',
                                cursor: 'pointer',
                                '&:hover': { bgcolor: 'action.hover' },
                              }}
                              onClick={() => setViewDoc(doc)}
                            >
                              <Stack direction="row" alignItems="flex-start" justifyContent="space-between" gap={1}>
                                <Stack spacing={1} sx={{ flex: 1, minWidth: 0 }}>
                                  <Stack direction="row" alignItems="center" spacing={1} sx={{ minWidth: 0 }}>
                                    {expired && <WarningAmberIcon fontSize="small" color="warning" sx={{ flexShrink: 0 }} />}
                                    <Typography variant="subtitle2" sx={{ wordBreak: 'break-word' }}>{doc.title}</Typography>
                                  </Stack>
                                  <Stack direction="row" flexWrap="wrap" gap={0.5} useFlexGap>
                                    <Chip label={doc.fileType ?? 'text'} size="small" variant="outlined" />
                                    <Chip label={`${doc.chunkCount ?? 0} 块`} size="small" variant="outlined" />
                                    <Chip label={`${doc.tokenCount ?? 0} tokens`} size="small" variant="outlined" />
                                    {qs != null && (
                                      <Chip label={`质量 ${qs}`} size="small"
                                        color={qs >= 80 ? 'success' : qs >= 60 ? 'warning' : 'error'}
                                        variant="outlined" />
                                    )}
                                    <Chip label={doc.status === 1 ? '已索引' : doc.status === 0 ? '处理中' : '失败'} size="small"
                                      color={doc.status === 1 ? 'success' : doc.status === 0 ? 'warning' : 'error'} />
                                    <Typography variant="caption" color="text.secondary" component="span" sx={{ alignSelf: 'center' }}>
                                      {formatDate(doc.createTime)}
                                    </Typography>
                                  </Stack>
                                </Stack>
                                <IconButton
                                  size="small"
                                  color="error"
                                  sx={{ flexShrink: 0 }}
                                  onClick={e => { e.stopPropagation(); setDeleteDocId(doc.id) }}
                                  aria-label="删除文档"
                                >
                                  <DeleteIcon fontSize="small" />
                                </IconButton>
                              </Stack>
                            </Box>
                          )
                        })}
                      </Stack>
                      {/* 分页控件 */}
                      {docTotalPages > 1 && (
                        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ pt: 1, borderTop: '1px solid', borderColor: 'divider' }}>
                          <Typography variant="caption" color="text.secondary">
                            共 {docTotal} 篇，第 {docPage + 1}/{docTotalPages} 页
                          </Typography>
                          <Pagination
                            count={docTotalPages}
                            page={docPage + 1}
                            onChange={(_, p) => setDocPage(p - 1)}
                            size="small"
                            siblingCount={1}
                          />
                        </Stack>
                      )}
                    </>
                  )}
                </Box>
              )}

              {/* Tab 1: RAG检索 */}
              {drawerTab === 1 && (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
                  <Stack direction="row" spacing={1} alignItems="flex-start">
                    <TextField size="small" placeholder="输入检索问题…" fullWidth
                      value={ragQuery} onChange={e => setRagQuery(e.target.value)}
                      onKeyDown={e => e.key === 'Enter' && handleRagSearch()} />
                    <Button variant="contained" startIcon={<SearchIcon />}
                      onClick={handleRagSearch} disabled={ragLoading || !ragQuery.trim()}
                      sx={{ flexShrink: 0 }}>
                      检索
                    </Button>
                  </Stack>
                  {ragLoading && <LinearProgress />}
                  <Stack spacing={1.5} sx={{ flex: 1, overflow: 'auto' }}>
                    {ragResults.map((chunk, i) => (
                      <Paper key={i} variant="outlined" sx={{ p: 0, overflow: 'hidden', borderRadius: 1 }}>
                        <Box sx={{ px: 2, py: 1, bgcolor: 'action.hover', borderBottom: 1, borderColor: 'divider' }}>
                          <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                            <Typography variant="subtitle2" fontWeight={600} sx={{ flex: 1, wordBreak: 'break-word' }}>
                              {chunk.title ?? chunk.docTitle ?? `结果 ${i + 1}`}
                            </Typography>
                            <Stack direction="row" spacing={0.5} flexShrink={0}>
                              <Chip label={ragScoreLabel(chunk.score)} size="small"
                                color={ragScoreColor(chunk.score)} />
                              {chunk.source && (
                                <Chip label={chunk.source} size="small" variant="outlined" />
                              )}
                            </Stack>
                          </Stack>
                        </Box>
                        <Box sx={{ px: 2, py: 1.5 }}>
                          <Typography
                            component="div"
                            variant="body2"
                            sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', lineHeight: 1.7, color: 'text.primary' }}
                          >
                            {chunk.content}
                          </Typography>
                        </Box>
                      </Paper>
                    ))}
                    {ragResults.length === 0 && !ragLoading && (
                      <Typography variant="body2" color="text.secondary">输入问题后点击检索</Typography>
                    )}
                  </Stack>
                </Box>
              )}

              {/* Tab 2: 索引队列 */}
              {drawerTab === 2 && (
                <Box>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1} flexWrap="wrap" gap={1}>
                    <Typography variant="subtitle2">索引队列</Typography>
                    <Stack direction="row" alignItems="center" spacing={0.5}>
                      {(indexPendingCount > 0 || indexFailedCount > 0) && (
                        <Chip size="small" variant="outlined" label={`待处理 ${indexPendingCount} · 失败 ${indexFailedCount}`} />
                      )}
                      <IconButton size="small" onClick={() => { void refetchIndexQueue() }} aria-label="刷新索引队列">
                        <RefreshIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </Stack>

                  {indexQueueError && (
                    <Alert
                      severity="error"
                      sx={{ mb: 1 }}
                      action={
                        <Button color="inherit" size="small" onClick={() => { void refetchIndexQueue() }}>
                          重试
                        </Button>
                      }
                    >
                      加载失败：{indexQueueErr instanceof Error ? indexQueueErr.message : String(indexQueueErr)}
                    </Alert>
                  )}

                  <Accordion disableGutters elevation={0} sx={{ mb: 1, border: 1, borderColor: 'divider', borderRadius: 1, '&:before': { display: 'none' } }}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                      <Typography variant="body2" fontWeight={600}>排查说明（常见原因）</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                      <Typography component="div" variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                        索引入库会依次写 PostgreSQL 文档表、生成向量（嵌入）、写入 Milvus 与 Elasticsearch。任一步失败会在下列出 `error_msg`，并重试最多 3 次。
                      </Typography>
                      <Box component="ul" sx={{ m: 0, pl: 2.5, typography: 'caption', color: 'text.secondary' }}>
                        <li><strong>metadata / jsonb</strong>：库表列类型需为 TEXT，与迁移 V148 一致。</li>
                        <li><strong>Milvus recovering</strong>：等待集合加载完成或检查 Milvus 服务。</li>
                        <li><strong>ES parse_exception / request body</strong>：空 bulk，多见于分块数为 0；需后端已修复空列表跳过。</li>
                        <li><strong>嵌入失败</strong>：检查嵌入模型配置与 API 可用性。</li>
                      </Box>
                    </AccordionDetails>
                  </Accordion>

                  {indexFetching ? <CircularProgress size={24} /> : (
                    <Stack spacing={1}>
                      {!indexQueueError && indexQueue.length === 0 && (
                        <Typography color="text.secondary" variant="body2">队列为空</Typography>
                      )}
                      {indexQueue.map(item => {
                        const hint = item.errorMsg ? hintForIndexQueueError(item.errorMsg) : null
                        return (
                          <Box key={item.id} sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 1, bgcolor: 'background.paper' }}>
                            <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1} flexWrap="wrap">
                              <Typography variant="body2" sx={{ flex: 1, wordBreak: 'break-word', whiteSpace: 'pre-wrap', minWidth: 0 }}>
                                {item.summary}
                              </Typography>
                              <Stack direction="row" spacing={0.5} alignItems="center" flexShrink={0}>
                                {item.sourceType != null && item.sourceType !== '' && (
                                  <Chip label={item.sourceType} size="small" variant="outlined" />
                                )}
                                <Chip label={`重试 ${item.retryCount}/3`} size="small" variant="outlined" />
                                <Chip label={item.chipLabel} size="small" color={item.chipColor} />
                              </Stack>
                            </Stack>
                            <Typography variant="caption" color="text.secondary" component="div" sx={{ mt: 0.5 }}>
                              {formatDate(item.createTime)}
                            </Typography>
                            {item.errorMsg != null && item.errorMsg !== '' && (
                              <>
                                <Typography
                                  variant="caption"
                                  color={item.status === 'failed' ? 'error' : 'warning.main'}
                                  component="div"
                                  sx={{ mt: 0.75, display: 'block', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}
                                >
                                  {item.errorMsg}
                                </Typography>
                                {hint != null && (
                                  <Alert severity="info" sx={{ mt: 1, py: 0.5 }} icon={false}>
                                    <Typography variant="caption" component="div">{hint}</Typography>
                                  </Alert>
                                )}
                              </>
                            )}
                          </Box>
                        )
                      })}
                    </Stack>
                  )}
                </Box>
              )}

              {/* Tab 3: 进化适应度 */}
              {drawerTab === 3 && (
                <Box>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2">进化适应度</Typography>
                    <IconButton size="small" onClick={() => { void qc.invalidateQueries({ queryKey: ['kb-evolution-fitness'] }) }}>
                      <RefreshIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                  {fitnessFetching ? <CircularProgress size={24} /> : (
                    <Stack spacing={1}>
                      {fitnessRows.length === 0 && (
                        <Typography color="text.secondary" variant="body2">暂无记录（完成进化或索引入队后会出现）</Typography>
                      )}
                      {fitnessRows.map(row => (
                        <Box key={row.id} sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 1, bgcolor: 'background.paper' }}>
                          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1} flexWrap="wrap">
                            <Typography variant="subtitle2" sx={{ wordBreak: 'break-word' }}>{row.metricName}</Typography>
                            <Chip label={row.taskId} size="small" variant="outlined" sx={{ maxWidth: '100%' }} />
                          </Stack>
                          {row.metricValue != null && Number.isFinite(row.metricValue) && (
                            <Typography variant="caption" color="text.secondary" component="div">value: {row.metricValue}</Typography>
                          )}
                          {row.experimentId != null && row.experimentId !== '' && (
                            <Typography variant="caption" color="text.secondary" component="div">experiment: {row.experimentId}</Typography>
                          )}
                          <Typography variant="caption" color="text.secondary" component="div" sx={{ mt: 0.5 }}>
                            {formatDate(row.createTime ?? '')}
                          </Typography>
                          {row.payloadJson != null && row.payloadJson !== '' && (
                            <Typography variant="caption" component="div" sx={{ mt: 0.5, whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontFamily: 'monospace', fontSize: 11 }}>
                              {row.payloadJson.length > 400 ? `${row.payloadJson.slice(0, 400)}…` : row.payloadJson}
                            </Typography>
                          )}
                        </Box>
                      ))}
                    </Stack>
                  )}
                </Box>
              )}
            </Box>
          </Box>
        )}
      </Drawer>
      {/* 文档详情抽屉 */}
      <Drawer
        anchor="right"
        open={viewDoc != null}
        onClose={() => setViewDoc(null)}
        PaperProps={{
          sx: { width: { xs: '100%', sm: 520, md: 560 } },
        }}
      >
        {viewDoc && (
          <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'background.paper' }}>
            <Box sx={{ px: 2.5, py: 2, borderBottom: 1, borderColor: 'divider' }}>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography variant="subtitle1" fontWeight={700} sx={{ wordBreak: 'break-word', lineHeight: 1.35 }}>
                    {viewDoc.title}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
                    文档详情 · 支持话术/长文本预览
                  </Typography>
                </Box>
                <IconButton size="small" onClick={() => setViewDoc(null)} aria-label="关闭">
                  <CloseIcon />
                </IconButton>
              </Stack>
            </Box>
            <Box sx={{ flex: 1, overflow: 'auto', px: 2, py: 2, bgcolor: 'background.default' }}>
              <Stack spacing={2}>
                <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
                  <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: 0.5 }}>元数据</Typography>
                  <Grid container spacing={2} sx={{ mt: 0.5 }}>
                    <Grid item xs={6} sm={4}>
                      <Typography variant="caption" color="text.secondary" display="block">{`文档 ID`}</Typography>
                      <Typography variant="body2" fontWeight={600}>{viewDoc.id}</Typography>
                    </Grid>
                    <Grid item xs={6} sm={4}>
                      <Typography variant="caption" color="text.secondary" display="block">{`知识库 ID`}</Typography>
                      <Typography variant="body2" fontWeight={600}>{viewDoc.kbId}</Typography>
                    </Grid>
                    <Grid item xs={6} sm={4}>
                      <Typography variant="caption" color="text.secondary" display="block">{`文件类型`}</Typography>
                      <Box sx={{ mt: 0.25 }}>
                        <Chip label={viewDoc.fileType ?? 'text'} size="small" variant="outlined" />
                      </Box>
                    </Grid>
                    <Grid item xs={6} sm={4}>
                      <Typography variant="caption" color="text.secondary" display="block">{`索引状态`}</Typography>
                      <Box sx={{ mt: 0.25 }}>
                        <Chip label={viewDoc.status === 1 ? '已索引' : viewDoc.status === 0 ? '处理中' : '失败'} size="small"
                          color={viewDoc.status === 1 ? 'success' : viewDoc.status === 0 ? 'warning' : 'error'} />
                      </Box>
                    </Grid>
                  </Grid>
                </Paper>

                <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
                  <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: 0.5 }}>分块与质量</Typography>
                  <Grid container spacing={2} sx={{ mt: 0.5 }}>
                    <Grid item xs={6} sm={4}>
                      <Typography variant="caption" color="text.secondary" display="block">{`分块数`}</Typography>
                      <Typography variant="body2" fontWeight={600}>{viewDoc.chunkCount ?? 0}</Typography>
                    </Grid>
                    <Grid item xs={6} sm={4}>
                      <Typography variant="caption" color="text.secondary" display="block">{`Token 数`}</Typography>
                      <Typography variant="body2" fontWeight={600}>{viewDoc.tokenCount ?? 0}</Typography>
                    </Grid>
                    {viewDoc.qualityHeuristicScore != null && (
                      <Grid item xs={6} sm={4}>
                        <Typography variant="caption" color="text.secondary" display="block">{`质量评分`}</Typography>
                        <Box sx={{ mt: 0.25 }}>
                          <Chip label={`${viewDoc.qualityHeuristicScore}/100`} size="small"
                            color={viewDoc.qualityHeuristicScore >= 80 ? 'success' : viewDoc.qualityHeuristicScore >= 60 ? 'warning' : 'error'} />
                        </Box>
                      </Grid>
                    )}
                    {viewDoc.expiryStatus != null && (
                      <Grid item xs={6} sm={4}>
                        <Typography variant="caption" color="text.secondary" display="block">{`时效性`}</Typography>
                        <Box sx={{ mt: 0.25 }}>
                          <Chip label={viewDoc.expiryStatus === 2 ? '已过期' : viewDoc.expiryStatus === 1 ? '即将过期' : '有效'} size="small"
                            color={viewDoc.expiryStatus === 2 ? 'error' : viewDoc.expiryStatus === 1 ? 'warning' : 'success'} variant="outlined" />
                        </Box>
                      </Grid>
                    )}
                  </Grid>
                </Paper>

                <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
                  <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: 0.5 }}>时间</Typography>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mt: 1 }}>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">{`创建`}</Typography>
                      <Typography variant="body2">{formatDate(viewDoc.createTime)}</Typography>
                    </Box>
                    {viewDoc.updateTime && (
                      <Box>
                        <Typography variant="caption" color="text.secondary" display="block">{`更新`}</Typography>
                        <Typography variant="body2">{formatDate(viewDoc.updateTime)}</Typography>
                      </Box>
                    )}
                  </Stack>
                </Paper>

                {viewDoc.content && (
                  <Paper variant="outlined" sx={{ borderRadius: 1, overflow: 'hidden' }}>
                    <Box sx={{ px: 2, py: 1.25, bgcolor: 'action.hover', borderBottom: 1, borderColor: 'divider' }}>
                      <Typography variant="subtitle2" fontWeight={600}>话术 / 正文预览</Typography>
                      <Typography variant="caption" color="text.secondary">
                        最长展示 3000 字，超出部分已截断
                      </Typography>
                    </Box>
                    <Box sx={{ p: 2, maxHeight: 440, overflow: 'auto' }}>
                      <Typography
                        component="div"
                        variant="body2"
                        sx={{
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          overflowWrap: 'anywhere',
                          lineHeight: 1.75,
                          fontSize: '0.875rem',
                          color: 'text.primary',
                        }}
                      >
                        {viewDoc.content.length > 3000 ? viewDoc.content.slice(0, 3000) + '…' : viewDoc.content}
                      </Typography>
                    </Box>
                  </Paper>
                )}
              </Stack>
            </Box>
            <Box sx={{ px: 2, py: 2, borderTop: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
              <Stack direction="row" justifyContent="flex-end" spacing={1}>
                <Button color="error" size="small" variant="outlined"
                  onClick={() => { setDeleteDocId(viewDoc.id); setViewDoc(null) }}>删除文档</Button>
                <Button size="small" variant="contained" onClick={() => setViewDoc(null)}>关闭</Button>
              </Stack>
            </Box>
          </Box>
        )}
      </Drawer>
    </Box>
  )
}
