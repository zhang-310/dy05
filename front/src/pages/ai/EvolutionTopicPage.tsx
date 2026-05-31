import { useState, useMemo } from 'react'
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Stack,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import { ConfirmDialog, PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import type { AiEvolveTopicVO } from '@/types/ai'
import { displayTopicTier } from '@/pages/ai/evolution/topicPriority'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'

const PAGE_SIZE = 12

const CATEGORY_OPTIONS = ['', 'live', 'basic', 'product', 'script', 'viral']

const EVOLUTION_TOPIC_READY_ENDPOINTS = [
  '/ai/evolution/topic/list',
  '/ai/evolution/topic/save',
  '/ai/evolution/topic/delete',
  '/ai/evolution/topic/import',
].join('|')

const EVOLUTION_TOPIC_UNSUPPORTED_ENDPOINTS = [
  '/ai/evolution/topic/mock',
  '/ai/evolution/topic/local-save',
  '/ai/evolution/topic/local-delete',
  '/ai/evolution/topic/browser-file-read',
  '/ai/evolution/topic/export',
  '/ai/evolution/topic/auto-generate',
].join('|')

function priorityLabel(priority: number): string {
  const t = displayTopicTier(priority)
  return t === 1 ? 'P1' : t === 2 ? 'P2' : 'P3'
}

export default function EvolutionTopicPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [page, setPage] = useState(0)
  const [editOpen, setEditOpen] = useState(false)
  const [editTopic, setEditTopic] = useState<AiEvolveTopicVO | null>(null)
  const [topicText, setTopicText] = useState('')
  const [category, setCategory] = useState('')
  const [priority, setPriority] = useState(2)
  const [importOpen, setImportOpen] = useState(false)
  const [importPath, setImportPath] = useState('')
  const [saving, setSaving] = useState(false)
  const [pageError, setPageError] = useState<string | null>(null)
  const [deleteTopicId, setDeleteTopicId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

  const {
    data: topics = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['evolve-topics-global'],
    queryFn: () => aiApi.topicList({ scopeGlobal: true }).then((data) => normalizeRows<AiEvolveTopicVO>(data)),
  })

  const filtered = useMemo(() => {
    return topics.filter((t) => {
      const title = String(t.topicName ?? t.topic ?? '')
      const matchSearch = !search || title.includes(search)
      const matchCat = !categoryFilter || String(t.category ?? '') === categoryFilter
      return matchSearch && matchCat
    })
  }, [topics, search, categoryFilter])

  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const topicStats = useMemo(() => {
    const p1 = filtered.filter((topic) => displayTopicTier(topic.priority) === 1).length
    const p2 = filtered.filter((topic) => displayTopicTier(topic.priority) === 2).length
    const p3 = filtered.filter((topic) => displayTopicTier(topic.priority) === 3).length
    const categories = new Set(filtered.map((topic) => String(topic.category ?? '')).filter(Boolean)).size
    return { p1, p2, p3, categories }
  }, [filtered])

  const openEdit = (topic?: AiEvolveTopicVO) => {
    setEditTopic(topic ?? null)
    setTopicText(String(topic?.topicName ?? topic?.topic ?? ''))
    setCategory(String(topic?.category ?? ''))
    setPriority(displayTopicTier(topic?.priority))
    setEditOpen(true)
  }

  const handleSave = async () => {
    if (!topicText.trim()) return
    setSaving(true)
    setPageError(null)
    try {
      await aiApi.topicSave({
        id: editTopic?.id,
        kbId: editTopic?.kbId,
        accountId: editTopic?.accountId,
        topicName: topicText.trim(),
        category: category.trim() || undefined,
        priority: displayTopicTier(priority),
      })
      toast(editTopic ? '已更新' : '已创建', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-topics'] })
      queryClient.invalidateQueries({ queryKey: ['evolve-topics-global'] })
      setEditOpen(false)
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`保存失败（POST /ai/evolution/topic/save）：${message}。编辑弹窗和输入值会保留。`)
      toast(`保存失败：${message}`, 'error')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (deleteTopicId == null) return
    setDeleting(true)
    setPageError(null)
    try {
      await aiApi.topicDelete(deleteTopicId)
      toast('已删除', 'success')
      setDeleteTopicId(null)
      queryClient.invalidateQueries({ queryKey: ['evolve-topics'] })
      queryClient.invalidateQueries({ queryKey: ['evolve-topics-global'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`删除失败（POST /ai/evolution/topic/delete）：${message}。主题卡片会保留，避免误删。`)
      toast(`删除失败：${message}`, 'error')
    } finally {
      setDeleting(false)
    }
  }

  const handleImport = async () => {
    if (!importPath.trim()) return
    setPageError(null)
    try {
      await aiApi.topicImport(importPath.trim())
      toast('导入成功', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-topics'] })
      queryClient.invalidateQueries({ queryKey: ['evolve-topics-global'] })
      setImportOpen(false)
      setImportPath('')
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`导入失败（POST /ai/evolution/topic/import）：${message}。导入路径会保留，方便修正服务端路径后重试。`)
      toast(`导入失败：${message}`, 'error')
    }
  }

  return (
    <Box
      data-testid="evolution-topic-page"
      data-ready-endpoints={EVOLUTION_TOPIC_READY_ENDPOINTS}
      data-unsupported-endpoints={EVOLUTION_TOPIC_UNSUPPORTED_ENDPOINTS}
      data-no-mock-topic-fallback="true"
      data-no-browser-file-read="true"
      data-no-local-topic-mutation="true"
    >
      <PageHeader
        title="进化主题池"
        subtitle="全局主题读取 /ai/evolution/topic/list?scopeGlobal=true；新增、编辑、删除和导入均落后端主题池。"
        breadcrumbs={[{ label: 'AI中心' }, { label: '进化主题池' }]}
        actions={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => void refetch()}
              data-testid="evolution-topic-refresh-list"
              data-source-endpoint="/ai/evolution/topic/list"
              data-refresh-scope="topic-list-only"
            >
              刷新
            </Button>
            <Button
              variant="outlined"
              startIcon={<UploadIcon />}
              onClick={() => setImportOpen(true)}
              data-testid="evolution-topic-import-open"
              data-source-endpoint="/ai/evolution/topic/import"
              data-server-path-import="true"
              data-no-browser-file-read="true"
            >
              批量导入
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => openEdit()}
              data-testid="evolution-topic-create-open"
              data-source-endpoint="/ai/evolution/topic/save"
              data-no-local-topic-mutation="true"
            >
              新增主题
            </Button>
          </Box>
        }
      />

      <Stack spacing={1.5} sx={{ mb: 2 }}>
        <Alert
          severity="info"
          data-testid="evolution-topic-boundary-contract"
          data-source-endpoints={EVOLUTION_TOPIC_READY_ENDPOINTS}
          data-server-path-import="true"
          data-no-browser-file-read="true"
          data-local-filter-only="true"
        >
          分类和搜索是前端本地筛选；导入路径是后端容器可访问的服务端路径，不是浏览器本地文件。
        </Alert>
        {isError ? (
          <Alert
            severity="error"
            data-testid="evolution-topic-list-error"
            data-source-endpoint="/ai/evolution/topic/list"
            data-no-mock-topic-fallback="true"
            action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
          >
            主题池加载失败（POST /ai/evolution/topic/list）：{getErrorMessage(error)}
          </Alert>
        ) : null}
        {pageError ? (
          <Alert
            severity="error"
            data-testid="evolution-topic-operation-error"
            data-input-retained="true"
            data-no-local-topic-mutation="true"
            data-no-browser-file-read="true"
          >
            {pageError}
          </Alert>
        ) : null}
      </Stack>

      <Box
        sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}
        data-testid="evolution-topic-local-filter-contract"
        data-local-filter-only="true"
        data-no-server-reload-on-local-filter="true"
      >
        <TextField
          size="small"
          placeholder="搜索主题…"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
          sx={{ minWidth: 240 }}
        />
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>分类</InputLabel>
          <Select value={categoryFilter} label="分类" onChange={(e) => { setCategoryFilter(e.target.value); setPage(0) }}>
            {CATEGORY_OPTIONS.map((c) => <MenuItem key={c} value={c}>{c || '全部'}</MenuItem>)}
          </Select>
        </FormControl>
        <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
          共 {filtered.length} 条
        </Typography>
      </Box>

      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        sx={{ mb: 2 }}
        data-testid="evolution-topic-summary-cards"
        data-source-endpoint="/ai/evolution/topic/list"
        data-filtered-count={filtered.length}
        data-category-count={topicStats.categories}
        data-no-mock-topic-fallback="true"
      >
        {[
          { label: 'P1', value: topicStats.p1, hint: '高优先级' },
          { label: 'P2', value: topicStats.p2, hint: '常规主题' },
          { label: 'P3', value: topicStats.p3, hint: '低优先级' },
          { label: '分类数', value: topicStats.categories, hint: '来自后端主题' },
        ].map((item) => (
          <Card key={item.label} variant="outlined" sx={{ flex: 1, minWidth: 0 }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h6" fontWeight={800}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
      ) : paged.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="evolution-topic-empty"
          data-no-mock-topic-fallback="true"
        >
          当前筛选下暂无主题。可新增主题，或通过服务端路径导入 JSON 主题文件；页面不会预置模拟主题。
        </Alert>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 2 }}>
          {paged.map((t) => (
            <Card
              key={String(t.id)}
              variant="outlined"
              data-testid="evolution-topic-card"
              data-source-endpoint="/ai/evolution/topic/list"
              data-topic-id={String(t.id)}
              data-topic-category={String(t.category ?? '')}
              data-topic-priority={String(displayTopicTier(t.priority))}
              data-topic-used-count={t.usedCount == null ? '' : String(t.usedCount)}
              data-no-local-delete-mutation="true"
              data-no-mock-topic-fallback="true"
            >
              <CardContent sx={{ pb: 1 }}>
                <Box sx={{ display: 'flex', gap: 1, mb: 1, flexWrap: 'wrap' }}>
                  {!!t.category && <Chip size="small" label={String(t.category)} variant="outlined" />}
                  <Chip size="small" label={`优先级: ${priorityLabel(Number(t.priority ?? 2))}`} />
                </Box>
                <Typography variant="body2">{String(t.topicName ?? t.topic ?? '')}</Typography>
                {t.usedCount != null && (
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                    使用 {String(t.usedCount)} 次
                  </Typography>
                )}
              </CardContent>
              <CardActions sx={{ pt: 0 }}>
                <Button
                  size="small"
                  startIcon={<EditIcon />}
                  onClick={() => openEdit(t)}
                  data-testid="evolution-topic-edit-open"
                  data-source-endpoint="/ai/evolution/topic/save"
                  data-topic-id={String(t.id)}
                  data-input-retained="true"
                >
                  编辑
                </Button>
                <Button
                  size="small"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={() => setDeleteTopicId(t.id as number)}
                  data-testid="evolution-topic-delete-open"
                  data-source-endpoint="/ai/evolution/topic/delete"
                  data-topic-id={String(t.id)}
                  data-no-local-topic-mutation="true"
                >
                  删除
                </Button>
              </CardActions>
            </Card>
          ))}
        </Box>
      )}

      {totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mt: 3 }}>
          <Button disabled={page === 0} onClick={() => setPage(page - 1)}>上一页</Button>
          <Typography sx={{ alignSelf: 'center' }}>{page + 1} / {totalPages}</Typography>
          <Button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>下一页</Button>
        </Box>
      )}

      {/* 编辑对话框 */}
      <Dialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        maxWidth="sm"
        fullWidth
        data-testid="evolution-topic-edit-dialog"
        data-source-endpoint="/ai/evolution/topic/save"
        data-input-retained="true"
        data-no-local-topic-mutation="true"
      >
        <DialogTitle>{editTopic ? '编辑主题' : '新增主题'}</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="主题内容"
            multiline
            minRows={3}
            value={topicText}
            onChange={(e) => setTopicText(e.target.value)}
            sx={{ mt: 1 }}
          />
          <TextField
            fullWidth
            label="分类标签"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="例如 live, basic"
            sx={{ mt: 2 }}
          />
          <FormControl fullWidth sx={{ mt: 2 }}>
            <InputLabel>优先级</InputLabel>
            <Select
              label="优先级"
              value={priority}
              onChange={(e) => setPriority(Number(e.target.value))}
            >
              <MenuItem value={1}>P1 紧急</MenuItem>
              <MenuItem value={2}>P2 普通</MenuItem>
              <MenuItem value={3}>P3 低</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!topicText.trim() || saving}
            data-testid="evolution-topic-save-submit"
            data-source-endpoint="/ai/evolution/topic/save"
            data-input-retained="true"
            data-no-local-topic-mutation="true"
          >
            {saving ? <CircularProgress size={16} /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 批量导入对话框 */}
      <Dialog
        open={importOpen}
        onClose={() => setImportOpen(false)}
        maxWidth="sm"
        fullWidth
        data-testid="evolution-topic-import-dialog"
        data-source-endpoint="/ai/evolution/topic/import"
        data-server-path-import="true"
        data-no-browser-file-read="true"
        data-input-retained="true"
      >
        <DialogTitle>批量导入主题</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="服务端路径"
            placeholder="如 D:\topics.json 或 /data/topics.json"
            value={importPath}
            onChange={(e) => setImportPath(e.target.value)}
            sx={{ mt: 1 }}
          />
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            支持 JSON 格式：queries 或 topics 数组
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleImport}
            disabled={!importPath.trim()}
            data-testid="evolution-topic-import-submit"
            data-source-endpoint="/ai/evolution/topic/import"
            data-server-path-import="true"
            data-no-browser-file-read="true"
            data-input-retained="true"
          >
            开始导入
          </Button>
        </DialogActions>
      </Dialog>
      <ConfirmDialog
        open={deleteTopicId !== null}
        title="删除进化主题"
        content="确定要删除该进化主题吗？删除后不会再参与主题池匹配。"
        onClose={() => setDeleteTopicId(null)}
        onConfirm={handleDelete}
        loading={deleting}
      />
    </Box>
  )
}
