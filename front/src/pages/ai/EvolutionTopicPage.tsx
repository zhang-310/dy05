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
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
} from '@mui/icons-material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import type { AiEvolveTopicVO } from '@/types/ai'
import { displayTopicTier } from '@/pages/ai/evolution/topicPriority'

const PAGE_SIZE = 12

const CATEGORY_OPTIONS = ['', 'live', 'basic', 'product', 'script', 'viral']

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

  const { data: topics = [], isLoading } = useQuery({
    queryKey: ['evolve-topics-global'],
    queryFn: () => aiApi.topicList({ scopeGlobal: true }),
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
    } catch {
      toast('保存失败', 'error')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('确认删除该主题？')) return
    try {
      await aiApi.topicDelete(id)
      toast('已删除', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-topics'] })
      queryClient.invalidateQueries({ queryKey: ['evolve-topics-global'] })
    } catch {
      toast('删除失败', 'error')
    }
  }

  const handleImport = async () => {
    if (!importPath.trim()) return
    try {
      await aiApi.topicImport(importPath.trim())
      toast('导入成功', 'success')
      queryClient.invalidateQueries({ queryKey: ['evolve-topics'] })
      queryClient.invalidateQueries({ queryKey: ['evolve-topics-global'] })
      setImportOpen(false)
      setImportPath('')
    } catch {
      toast('导入失败', 'error')
    }
  }

  return (
    <Box>
      <PageHeader
        title="进化主题池"
        breadcrumbs={[{ label: 'AI中心' }, { label: '进化主题池' }]}
        actions={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button variant="outlined" startIcon={<UploadIcon />} onClick={() => setImportOpen(true)}>批量导入</Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => openEdit()}>新增主题</Button>
          </Box>
        }
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
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

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
      ) : paged.length === 0 ? (
        <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>暂无主题</Typography>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 2 }}>
          {paged.map((t) => (
            <Card key={String(t.id)} variant="outlined">
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
                <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(t)}>编辑</Button>
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDelete(t.id as number)}>删除</Button>
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
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth>
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
          <Button variant="contained" onClick={handleSave} disabled={!topicText.trim() || saving}>
            {saving ? <CircularProgress size={16} /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 批量导入对话框 */}
      <Dialog open={importOpen} onClose={() => setImportOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>批量导入主题</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="本地路径"
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
          <Button variant="contained" onClick={handleImport} disabled={!importPath.trim()}>开始导入</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
