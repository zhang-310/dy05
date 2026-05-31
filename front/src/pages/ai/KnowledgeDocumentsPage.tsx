import { useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box, Typography, Button, Stack, Chip, TextField,
  Select, MenuItem, IconButton, Drawer, Divider,
  Alert, Dialog, DialogTitle, DialogContent, DialogActions,
  FormControlLabel, Grid, Paper, Switch,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import SearchIcon from '@mui/icons-material/Search'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader } from '@/components/base'
import { aiApi, type KbDocument } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import MarkdownViewer from '@/components/MarkdownViewer'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
  0: { label: '待向量同步', color: 'warning' },
  1: { label: '已索引', color: 'success' },
  2: { label: '失败', color: 'error' },
}

const SOURCE_TYPES = ['全部', 'file', 'url', 'text', 'api']

function qualityLabel(score?: number): { label: string; color: 'success' | 'warning' | 'error' | 'default' } {
  if (score == null || !Number.isFinite(Number(score))) return { label: '未评估', color: 'default' }
  if (score >= 80) return { label: `高质 ${score}`, color: 'success' }
  if (score >= 60) return { label: `待优化 ${score}`, color: 'warning' }
  return { label: `低质 ${score}`, color: 'error' }
}

function documentIssueHint(doc: KbDocument): string | null {
  if (doc.status === 0) return '文档已落库但向量未完成，请检查嵌入模型、Ollama 地址、Milvus/ES 状态或索引队列 error_msg。'
  if (doc.status !== 1) return '索引失败，请在知识库抽屉索引队列查看失败详情。'
  if ((doc.chunkCount ?? 0) === 0) return '分块数为 0，内容可能过短或解析失败，检索命中率会很低。'
  if ((doc.qualityHeuristicScore ?? 100) < 60) return '质量分偏低，通常是正文过短、结构弱、缺少标题层级或缺少可检索事实。'
  return null
}

export default function KnowledgeDocumentsPage() {
  const { kbId } = useParams<{ kbId: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const kbIdNum = Number(kbId)

  const [search, setSearch] = useState({ page: 0, rows: 20, keyword: '', sourceType: '' })
  const [keyword, setKeyword] = useState('')
  const [sourceType, setSourceType] = useState('全部')
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [detail, setDetail] = useState<KbDocument | null>(null)
  const [importOpen, setImportOpen] = useState(false)
  const [importError, setImportError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [importForm, setImportForm] = useState({
    sourcePath: '/data/knowledge-base-imports/',
    autoClassify: false,
  })

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['kb-docs', kbIdNum, search],
    queryFn: () => aiApi.docList(kbIdNum, {
      page: search.page,
      rows: search.rows,
      keyword: search.keyword || undefined,
      sourceType: search.sourceType || undefined,
    }),
    enabled: !!kbIdNum,
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0
  const indexedCount = rows.filter(row => row.status === 1).length
  const pendingCount = rows.filter(row => row.status === 0).length
  const failedCount = rows.filter(row => row.status !== 0 && row.status !== 1).length
  const lowQualityCount = rows.filter(row => row.qualityHeuristicScore != null && row.qualityHeuristicScore < 60).length
  const emptyChunkCount = rows.filter(row => (row.chunkCount ?? 0) === 0).length

  const delMut = useMutation({
    mutationFn: aiApi.docDelete,
    onSuccess: () => {
      toast('已删除', 'success')
      setDeleteError(null)
      setDeleteId(null)
      qc.invalidateQueries({ queryKey: ['kb-docs', kbIdNum] })
    },
    onError: (e: Error, id: number) => {
      const message = `文档删除失败（DELETE /ai/knowledge-base/document/${id}）：${e.message}`
      setDeleteError(message)
      toast(e.message, 'error')
    },
  })

  const importMut = useMutation({
    mutationFn: () => aiApi.kbImportFromPath({
      kbId: kbIdNum,
      sourcePath: importForm.sourcePath.trim(),
      autoClassify: importForm.autoClassify,
    }),
    onSuccess: () => {
      toast('导入任务已提交', 'success')
      setImportError(null)
      setImportOpen(false)
      qc.invalidateQueries({ queryKey: ['kb-docs', kbIdNum] })
    },
    onError: (e: Error) => {
      const message = `路径导入失败（/ai/knowledge-base/import-from-path）：${e.message}`
      setImportError(message)
      toast(e.message, 'error')
    },
  })

  const handleImport = useCallback(() => { setImportError(null); setImportOpen(true) }, [])

  const handleSearch = () => {
    setSearch(s => ({
      ...s,
      page: 0,
      keyword,
      sourceType: sourceType === '全部' ? '' : sourceType,
    }))
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '文件名', flex: 1, minWidth: 180,
      renderCell: ({ row, value }) => (
        <Button size="small" variant="text" sx={{ justifyContent: 'flex-start' }}
          onClick={() => setDetail(row as KbDocument)}>{String(value)}</Button>
      ) },
    { field: 'fileType', headerName: '类型', width: 90,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
    { field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => {
        const s = STATUS_MAP[value as number] ?? { label: String(value), color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      } },
    { field: 'qualityHeuristicScore', headerName: '质量', width: 110,
      renderCell: ({ row }) => {
        const q = qualityLabel((row as KbDocument).qualityHeuristicScore)
        return <Chip label={q.label} color={q.color} size="small" variant={q.color === 'default' ? 'outlined' : 'filled'} />
      } },
    { field: 'chunkCount', headerName: '分块', width: 90,
      renderCell: ({ row }) => (
        <Chip
          label={`${(row as KbDocument).chunkCount ?? 0} 块`}
          color={((row as KbDocument).chunkCount ?? 0) > 0 ? 'default' : 'warning'}
          size="small"
          variant="outlined"
        />
      ) },
    { field: 'syncRetryCount', headerName: '补偿', width: 80,
      renderCell: ({ row }) => (
        (row as KbDocument).status === 0
          ? <Chip label={`${(row as KbDocument).syncRetryCount ?? 0}/3`} size="small" variant="outlined" />
          : <Typography variant="body2" color="text.secondary">-</Typography>
      ) },
    { field: 'createTime', headerName: '创建时间', width: 155, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Button size="small" color="error" onClick={() => { setDeleteError(null); setDeleteId((row as KbDocument).id) }}>删除</Button>
      ) },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} alignItems="center">
      <Select size="small" value={sourceType} onChange={e => setSourceType(e.target.value)} sx={{ minWidth: 100, borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& .MuiOutlinedInput-notchedOutline': { borderColor: 'var(--color-surface-light)' }, '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'var(--color-primary)' } }}>
        {SOURCE_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
      </Select>
      <TextField size="small" placeholder="搜索文件名" value={keyword}
        onChange={e => setKeyword(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && handleSearch()}
        sx={{ width: 180, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
      <Button
        variant="contained"
        size="small"
        startIcon={<SearchIcon />}
        onClick={handleSearch}
        data-testid="kb-doc-search-action-surface"
        sx={{
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          '&:hover': { bgcolor: 'primary.dark' },
        }}
      >
        查询
      </Button>
    </Stack>
  )

  const actionSlot = (
    <Button
      variant="contained"
      size="small"
      startIcon={<AddIcon />}
      onClick={handleImport}
      data-testid="kb-doc-import-action-surface"
      sx={{
        bgcolor: 'primary.main',
        color: 'primary.contrastText',
        '&:hover': { bgcolor: 'primary.dark' },
      }}
    >
      导入文档
    </Button>
  )

  return (
    <Box
      data-testid="knowledge-documents-page"
      data-ready-endpoints="/ai/knowledge-base/{kbId}/documents,/ai/knowledge-base/import-from-path,DELETE /ai/knowledge-base/document/{docId}"
      data-unsupported-endpoints="/ai/knowledge-base/document/upload-browser-file,/ai/knowledge-base/document/local-list,/ai/knowledge-base/document/static-content,/ai/knowledge-base/import-local-path"
      data-no-local-document-fallback="true"
      data-no-static-document-fallback="true"
      data-server-path-import="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 2, bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)' }}
    >
      <PageHeader
        title="知识库文档管理"
        subtitle="查看文档索引状态、质量分、Markdown 正文和导入失败原因"
        breadcrumbs={[{ label: 'AI中心' }, { label: '知识库', href: '/admin/ai/knowledge' }, { label: `KB #${kbIdNum}` }]}
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate('/admin/ai/knowledge')}>返回</Button>
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => { void refetch() }}>刷新</Button>
          </Stack>
        }
      />

      <Grid container spacing={1.5}>
        {[
          ['本页文档', rows.length.toLocaleString(), `总计 ${total.toLocaleString()}`],
          ['已索引', indexedCount.toLocaleString(), '可进入 RAG 检索'],
          ['待补偿/失败', `${pendingCount}/${failedCount}`, '优先看嵌入与索引队列'],
          ['低质/空分块', `${lowQualityCount}/${emptyChunkCount}`, '影响质量均分与命中率'],
        ].map(([title, value, helper]) => (
          <Grid item xs={12} sm={6} md={3} key={title}>
            <Paper
              variant="outlined"
              data-testid={`kb-doc-summary-${title}`}
              data-source-endpoint={`/ai/knowledge-base/${kbIdNum}/documents`}
              sx={{ p: 1.5, borderRadius: 1, height: '100%' }}
            >
              <Typography variant="caption" color="text.secondary">{title}</Typography>
              <Typography variant="h6" fontWeight={700}>{value}</Typography>
              <Typography variant="caption" color="text.secondary">{helper}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Alert
        severity="info"
        data-testid="kb-doc-import-boundary-notice"
        data-server-path-import="true"
        data-no-browser-file-read="true"
        sx={{ py: 0.75 }}
      >
        路径导入只允许容器内 `/data/knowledge-base-imports` 目录；如果要连接本机文件，请先挂载到 Docker 再导入。
      </Alert>

      {isError && (
        <Alert
          severity="error"
          data-testid="kb-doc-list-error"
          data-source-endpoint={`/ai/knowledge-base/${kbIdNum}/documents`}
          data-no-local-document-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => { void refetch() }}>重试</Button>}
        >
          文档列表加载失败：{error instanceof Error ? error.message : String(error)}
        </Alert>
      )}

      {deleteError != null && (
        <Alert
          severity="error"
          data-testid="kb-doc-delete-error"
          data-source-endpoint="/ai/knowledge-base/document/{docId}"
          data-no-local-delete-fallback="true"
          onClose={() => setDeleteError(null)}
        >
          {deleteError}。删除失败不会本地移除文档或分块，请确认登录态、归属权限和索引服务后重试。
        </Alert>
      )}

      <Box
        data-testid="kb-doc-list-grid"
        data-source-endpoint={`/ai/knowledge-base/${kbIdNum}/documents`}
        data-pagination-mode="server"
        data-no-local-document-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={total} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
        />
      </Box>

      <ConfirmDialog open={deleteId !== null} content={deleteError ?? '确定要删除该文档吗？（将同时删除分块索引）'}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />

      <Dialog open={importOpen} onClose={() => setImportOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>导入文档</DialogTitle>
        <DialogContent>
          <Stack
            spacing={2}
            data-testid="kb-doc-import-dialog"
            data-source-endpoint="/ai/knowledge-base/import-from-path"
            data-server-path-import="true"
            data-no-browser-file-read="true"
            data-preserves-form-input="true"
            sx={{ pt: 1 }}
          >
            <Alert
              severity="warning"
              data-testid="kb-doc-import-path-boundary"
              data-allowed-root="/data/knowledge-base-imports"
              sx={{ py: 0.75 }}
            >
              后端字段为 `sourcePath`，并会校验路径必须位于 `/data/knowledge-base-imports` 下。
            </Alert>
            <TextField
              label="容器内路径"
              value={importForm.sourcePath}
              onChange={e => setImportForm(v => ({ ...v, sourcePath: e.target.value }))}
              fullWidth
              placeholder="/data/knowledge-base-imports/product/faq.md"
            />
            <FormControlLabel
              control={<Switch checked={importForm.autoClassify} onChange={e => setImportForm(v => ({ ...v, autoClassify: e.target.checked }))} />}
              label="自动分类"
            />
            {importError != null && (
              <Alert
                severity="error"
                data-testid="kb-doc-import-error"
                data-source-endpoint="/ai/knowledge-base/import-from-path"
                data-preserves-form-input="true"
              >
                {importError}。当前路径和自动分类选项会保留；Docker 环境需先把本机目录挂载到容器内 `/data/knowledge-base-imports`。
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={() => importMut.mutate()}
            disabled={importMut.isPending || !importForm.sourcePath.trim()}
          >
            提交导入
          </Button>
        </DialogActions>
      </Dialog>

      <Drawer anchor="right" open={detail !== null} onClose={() => setDetail(null)}
        PaperProps={{ sx: { width: 480, p: 3, bgcolor: 'var(--color-surface)', borderLeft: '1px solid var(--color-surface-light)' } }}>
        {detail && (
          <Stack
            spacing={2}
            data-testid="kb-doc-detail-drawer"
            data-source-endpoint={`/ai/knowledge-base/${kbIdNum}/documents`}
            data-no-static-document-fallback="true"
          >
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="h6" sx={{ wordBreak: 'break-all', fontSize: 15, color: 'var(--color-text-primary)' }}>{detail.title}</Typography>
              <IconButton onClick={() => setDetail(null)} sx={{ color: 'var(--color-text-primary)' }}><CloseIcon /></IconButton>
            </Stack>
            <Divider sx={{ borderColor: 'var(--color-surface-light)' }} />
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>文档ID</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{detail.id}</Typography></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>知识库ID</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{detail.kbId}</Typography></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>文件类型</Typography>
              <Chip label={detail.fileType} size="small" variant="outlined" sx={{ borderColor: 'var(--color-surface-light)' }} /></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>索引状态</Typography>
              <Chip label={STATUS_MAP[detail.status]?.label ?? String(detail.status)}
                color={STATUS_MAP[detail.status]?.color ?? 'default'} size="small" /></Box>
            {documentIssueHint(detail) != null && (
              <Alert
                severity={detail.status === 1 ? 'warning' : 'error'}
                data-testid="kb-doc-detail-issue-hint"
                data-docker-ollama-diagnostic={detail.status === 0 ? 'true' : undefined}
              >
                {documentIssueHint(detail)}
              </Alert>
            )}
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>质量分</Typography>
              <Box sx={{ mt: 0.5 }}>
                <Chip label={qualityLabel(detail.qualityHeuristicScore).label}
                  color={qualityLabel(detail.qualityHeuristicScore).color} size="small" />
              </Box></Box>
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>分块 / Token</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{detail.chunkCount ?? 0} 块 · {detail.tokenCount ?? 0} tokens</Typography></Box>
            {detail.status === 0 && (
              <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>向量补偿</Typography>
                <Typography sx={{ color: 'var(--color-text-primary)' }}>{detail.syncRetryCount ?? 0}/3</Typography></Box>
            )}
            <Box><Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>创建时间</Typography>
              <Typography sx={{ color: 'var(--color-text-primary)' }}>{formatDate(detail.createTime)}</Typography></Box>
            {detail.content && (
              <Box>
                <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>正文预览</Typography>
                <Box
                  data-testid="kb-doc-detail-markdown"
                  data-renderer="MarkdownViewer"
                  sx={{ mt: 1, p: 1.5, maxHeight: 360, overflow: 'auto', border: '1px solid var(--color-surface-light)', borderRadius: 1, color: 'var(--color-text-primary)' }}
                >
                  <MarkdownViewer
                    content={detail.content.length > 3000 ? detail.content.slice(0, 3000) + '…' : detail.content}
                    compact
                  />
                </Box>
              </Box>
            )}
            <Button
              size="small"
              color="error"
              variant="outlined"
              data-testid="kb-doc-detail-delete-button"
              data-source-endpoint="/ai/knowledge-base/document/{docId}"
              onClick={() => { setDeleteError(null); setDeleteId(detail.id); setDetail(null) }}
              sx={(theme) => ({
                borderColor: theme.palette.error.main,
                color: theme.palette.error.main,
                '&:hover': {
                  borderColor: theme.palette.error.dark,
                  bgcolor: alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.16 : 0.06),
                },
              })}
            >
              删除文档
            </Button>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
