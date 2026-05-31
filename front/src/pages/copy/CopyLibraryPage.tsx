import { useState } from 'react'
import {
  Box, Tab, Tabs, TextField, Button, Stack, Chip, MenuItem,
  Drawer, Typography, Divider, IconButton, Tooltip, Paper,
  LinearProgress, Dialog, DialogTitle, DialogContent,
  DialogActions, Card, CardContent, ToggleButton,
  Select, FormControl, InputLabel, Grid, RadioGroup, Radio, FormControlLabel,
  Alert,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import SearchIcon from '@mui/icons-material/Search'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import SendIcon from '@mui/icons-material/Send'
import type { GridColDef, GridRenderCellParams, GridRowSelectionModel } from '@mui/x-data-grid'
import { alpha } from '@mui/material/styles'
import { StandardDataGrid, EmptyState, PageHeader } from '@/components/base'
import { copyApi, type CopyItem, type CopyApproval, type CopyTemplate, type CopyAiCandidate } from '@/api/copy'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' | 'error' }> = {
  0: { label: '待审核', color: 'warning' },
  1: { label: '已审核', color: 'success' },
}

const TAG_OPTIONS = ['开场话术', '商品介绍', '促单', '互动', '结尾']
const CATEGORY_OPTIONS = ['护肤', '彩妆']
const COPY_LIBRARY_READY_ENDPOINTS = [
  '/copy/library/search',
  '/copy/library/save',
  '/copy/library/delete',
  '/copy/approval/search',
  '/copy/approval/get',
  '/copy/approval/save',
  '/copy/template/search',
  '/copy/template/save',
  '/copy/template/delete',
  '/copy/ai/generate',
]
const COPY_LIBRARY_UNSUPPORTED_ACTIONS = [
  'semantic-vector-search',
  'csv-export',
  'batch-tag',
  'usage-detail',
  'approval-stats',
  'approval-revise',
  'server-side-batch-delete',
  'batch-ai-save',
]
const COPY_LIBRARY_UNSUPPORTED_ENDPOINTS = [
  '/copy/library/vector-search',
  '/copy/library/export',
  '/copy/library/batch-tag',
  '/copy/library/usage',
  '/copy/approval/stats',
  '/copy/approval/revise',
  '/copy/template/batch-delete',
  '/copy/ai/batch-save',
]
const contractValue = (items: string[]) => items.join(',')

function ScoreBar({ value }: { value?: number }) {
  const v = value ?? 0
  const paletteColor = v >= 9 ? 'success' : v >= 7 ? 'warning' : 'text'
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, width: '100%' }}>
      <LinearProgress variant="determinate" value={v * 10}
        sx={{
          flex: 1,
          height: 6,
          borderRadius: 3,
          bgcolor: (theme) => alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.16 : 0.08),
          '& .MuiLinearProgress-bar': {
            bgcolor: (theme) => paletteColor === 'text' ? theme.palette.text.disabled : theme.palette[paletteColor].main,
          },
        }} />
      <Typography
        variant="caption"
        sx={{
          minWidth: 24,
          color: (theme) => paletteColor === 'text' ? theme.palette.text.secondary : theme.palette[paletteColor].main,
        }}
      >
        {v > 0 ? v.toFixed(1) : '—'}
      </Typography>
    </Box>
  )
}

function TemplateContent({ content }: { content: string }) {
  const parts = content.split(/({[^}]+})/g)
  return (
    <Typography component="span" variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>
      {parts.map((p, i) =>
        /^{[^}]+}$/.test(p)
          ? <Box key={i} component="span" sx={{ color: 'warning.main', fontWeight: 600 }}>{p}</Box>
          : p
      )}
    </Typography>
  )
}

// ─────────────────────────────────────────────
// Tab 0: 文案库
// ─────────────────────────────────────────────
function CopyLibraryTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [semantic, setSemantic] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [selectedTags, setSelectedTags] = useState<string[]>([])
  const [minScore, setMinScore] = useState<string>('')
  const [category, setCategory] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [selection, setSelection] = useState<GridRowSelectionModel>([])
  const [preview, setPreview] = useState<CopyItem | null>(null)
  const [previewTab, setPreviewTab] = useState(0)
  const [aiOpen, setAiOpen] = useState(false)
  const [aiForm, setAiForm] = useState({ prompt: '', category: '', count: 3, copyType: '', style: '', keywords: '', duration: '' })
  const [aiResults, setAiResults] = useState<CopyAiCandidate[]>([])
  const [editOpen, setEditOpen] = useState(false)
  const [editItem, setEditItem] = useState<Partial<CopyItem>>({ title: '', content: '', category: '', tags: '', status: 0 })
  const [libraryError, setLibraryError] = useState('')
  const [editError, setEditError] = useState('')
  const [aiError, setAiError] = useState('')

  const queryParams = {
    page,
    rows: pageSize,
    keyword: keyword || undefined,
    tags: selectedTags.length ? selectedTags.join(',') : undefined,
    minScore: minScore ? Number(minScore) : undefined,
    category: category || undefined,
    status: statusFilter !== '' ? Number(statusFilter) : undefined,
  }

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['copy-list', queryParams, semantic],
    queryFn: () =>
      semantic && keyword
        ? copyApi.semanticSearch(keyword, { page, rows: pageSize })
        : copyApi.list(queryParams),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => { setLibraryError(''); return copyApi.delete(id) },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); toast('已删除', 'success') },
    onError: (e) => { setLibraryError(`/copy/library/delete 删除失败：${getErrorMessage(e)}`); toast('删除失败', 'error') },
  })
  const batchDeleteMut = useMutation({
    mutationFn: (ids: number[]) => { setLibraryError(''); return copyApi.batchDelete(ids) },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); setSelection([]); toast('批量删除成功', 'success') },
    onError: (e) => { setLibraryError(`/copy/library/delete 批量删除失败：${getErrorMessage(e)}`); toast('批量删除失败', 'error') },
  })
  const submitApprovalMut = useMutation({
    mutationFn: (ids: number[]) => {
      setLibraryError('')
      if (ids.length > 50) {
        const error = new Error('最多批量操作 50 条')
        setLibraryError(`/copy/approval/save 提交审批失败：${error.message}`)
        toast('最多批量操作 50 条', 'warning')
        return Promise.reject(error)
      }
      return copyApi.batchSubmitApproval(ids)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); setSelection([]); toast('已提交审批', 'success') },
    onError: (e) => {
      if (e instanceof Error && e.message) {
        setLibraryError(`/copy/approval/save 提交审批失败：${getErrorMessage(e)}`)
        toast('提交审批失败', 'error')
      }
    },
  })
  const aiMut = useMutation({
    mutationFn: () => { setAiError(''); return copyApi.aiGenerate({ ...aiForm }) },
    onSuccess: (res) => { setAiResults(res) },
    onError: (e) => { setAiError(`/copy/ai/generate AI 生成失败：${getErrorMessage(e)}`); toast('AI 生成失败', 'error') },
  })
  const saveMut = useMutation({
    mutationFn: (item: Partial<CopyItem>) => { setEditError(''); setAiError(''); return copyApi.save(item) },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); setEditOpen(false); toast('已保存', 'success') },
    onError: (e) => {
      const message = `/copy/library/save 保存失败：${getErrorMessage(e)}`
      if (aiOpen) setAiError(message)
      else setEditError(message)
      toast('保存失败', 'error')
    },
  })

  const openCreate = () => {
    setEditItem({ title: '', content: '', category: '', tags: '', status: 0 })
    setEditError('')
    setEditOpen(true)
  }

  const openEdit = (item: CopyItem) => {
    setEditItem(item)
    setEditError('')
    setEditOpen(true)
  }

  const handleSave = () => {
    if (!editItem.title?.trim() || !editItem.content?.trim()) {
      toast('请填写标题和文案内容', 'warning')
      return
    }
    saveMut.mutate(editItem)
  }

  const columns: GridColDef[] = [
    { field: 'content', headerName: '内容摘要', flex: 3, minWidth: 200,
      renderCell: ({ row, value }: GridRenderCellParams) => (
        <Button size="small" variant="text" sx={{ justifyContent: 'flex-start', textAlign: 'left', overflow: 'hidden' }}
          onClick={() => { setPreview(row as CopyItem); setPreviewTab(0) }}>
          <Typography noWrap sx={{ fontSize: 13 }}>
            {String(value ?? '').slice(0, 80)}{String(value ?? '').length > 80 ? '…' : ''}
          </Typography>
        </Button>
      ) },
    { field: 'tags', headerName: '标签', width: 160,
      renderCell: ({ value }) => {
        const tagArr = String(value ?? '').split(',').filter(Boolean)
        return (
          <Stack direction="row" gap={0.5} flexWrap="nowrap" overflow="hidden">
            {tagArr.slice(0, 3).map(t => <Chip key={t} label={t.trim()} size="small" variant="outlined" />)}
            {tagArr.length > 3 && <Chip label={`+${tagArr.length - 3}`} size="small" />}
          </Stack>
        )
      } },
    { field: 'rating', headerName: '效果分', width: 100,
      renderCell: ({ value }) => <ScoreBar value={value as number} /> },
    { field: 'useCount', headerName: '使用次数', width: 80, type: 'number',
      renderCell: ({ row, value }) => (
        <Button size="small" variant="text" onClick={() => { setPreview(row as CopyItem); setPreviewTab(1) }}>
          {String(value ?? 0)}
        </Button>
      ) },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => { const s = STATUS_MAP[value as number] ?? STATUS_MAP[0]; return <Chip label={s.label} color={s.color} size="small" /> } },
    { field: 'createTime', headerName: '创建时间', width: 110,
      valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Stack direction="row" gap={0.5}>
          <Button size="small" onClick={() => { setPreview(row as CopyItem); setPreviewTab(0) }}>预览</Button>
          <Button size="small" onClick={() => openEdit(row as CopyItem)}>编辑</Button>
          <Button size="small" onClick={() => submitApprovalMut.mutate([(row as CopyItem).id])}>发审批</Button>
          <Button size="small" color="error" onClick={() => deleteMut.mutate((row as CopyItem).id)}>删除</Button>
        </Stack>
      ) },
  ]

  return (
    <Box
      data-testid="copy-library-list-workbench"
      data-contract-scope="copy-library-list"
      data-ready-endpoints={contractValue(['/copy/library/search', '/copy/library/save', '/copy/library/delete', '/copy/approval/save', '/copy/ai/generate'])}
      data-unsupported-actions={contractValue(['semantic-vector-search', 'csv-export', 'batch-tag', 'usage-detail', 'batch-ai-save'])}
      data-unsupported-endpoints={contractValue(['/copy/library/vector-search', '/copy/library/export', '/copy/library/batch-tag', '/copy/library/usage', '/copy/ai/batch-save'])}
      data-row-count={data?.total ?? 0}
      data-selected-count={selection.length}
      data-query-mode={semantic && keyword ? 'keyword-fallback-for-semantic' : 'keyword'}
      data-no-local-copy-fallback="true"
      data-row-retained-on-action-error="true"
      data-no-local-delete-mutation="true"
    >
      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="copy-library-list-contract-alert"
        data-contract-status="ready-with-explicit-degradation"
        data-ready-endpoints={contractValue(['/copy/library/search', '/copy/library/save', '/copy/library/delete', '/copy/approval/save', '/copy/ai/generate'])}
        data-unsupported-actions={contractValue(['semantic-vector-search', 'csv-export', 'batch-tag', 'usage-detail', 'batch-ai-save'])}
      >
        文案库列表、新建、删除、提交审批和 AI 生成走后端真实接口；语义搜索当前降级为关键词检索，导出 CSV、批量打标签和使用明细接口尚未落库，页面不再提供伪操作按钮。
      </Alert>
      {/* 筛选工具栏 */}
      <Stack spacing={1} mb={1.5}>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
          <TextField size="small" placeholder="关键词搜索" value={keyword}
            onChange={e => setKeyword(e.target.value)} sx={{ width: 180 }}
            onKeyDown={e => e.key === 'Enter' && setPage(0)} />
          <Tooltip title={semantic ? '已启用语义搜索' : '切换为语义搜索'}>
            <ToggleButton value="semantic" selected={semantic} size="small"
              onChange={() => setSemantic(s => !s)}
              sx={{ borderRadius: 1, height: 36 }}>
              <SearchIcon fontSize="small" sx={{ mr: 0.5 }} />
              语义
            </ToggleButton>
          </Tooltip>
          <FormControl size="small" sx={{ width: 100 }}>
            <InputLabel>效果分</InputLabel>
            <Select value={minScore} label="效果分" onChange={e => setMinScore(e.target.value as string)}>
              <MenuItem value="">全部</MenuItem>
              <MenuItem value="7">7分+</MenuItem>
              <MenuItem value="8">8分+</MenuItem>
              <MenuItem value="9">9分+</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ width: 90 }}>
            <InputLabel>品类</InputLabel>
            <Select value={category} label="品类" onChange={e => setCategory(e.target.value as string)}>
              <MenuItem value="">全部</MenuItem>
              {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ width: 100 }}>
            <InputLabel>状态</InputLabel>
            <Select value={statusFilter} label="状态" onChange={e => setStatusFilter(e.target.value as string)}>
              <MenuItem value="">全部</MenuItem>
              {Object.entries(STATUS_MAP).map(([k, v]) => <MenuItem key={k} value={k}>{v.label}</MenuItem>)}
            </Select>
          </FormControl>
          <Button variant="contained" size="small" onClick={() => setPage(0)}>搜索</Button>
          <Button size="small" variant="outlined" startIcon={<SmartToyIcon />} onClick={() => { setAiOpen(true); setAiResults([]) }}>✨ AI生成</Button>
          <Button size="small" variant="contained" startIcon={<AddIcon />} sx={{ ml: 'auto' }}
            onClick={openCreate}>新建</Button>
        </Stack>
        {semantic && (
          <Alert
            severity="warning"
            sx={{ py: 0.5 }}
            data-testid="copy-library-semantic-degradation"
            data-contract-status="fallback-to-keyword"
            data-ready-endpoint="/copy/library/search"
            data-unsupported-endpoint="/copy/library/vector-search"
            data-no-vector-request="true"
          >
            后端未提供文案向量检索接口，当前使用 `/copy/library/search` 关键词检索兜底。
          </Alert>
        )}
        {libraryError ? (
          <Alert
            severity="error"
            sx={{ py: 0.5 }}
            data-testid="copy-library-action-error"
            data-contract-status="source-error"
            data-row-retained-on-action-error="true"
            data-no-local-copy-mutation="true"
            data-source-endpoints="/copy/library/delete,/copy/approval/save"
          >
            {libraryError}。失败不会移除列表行或清空当前选择。
          </Alert>
        ) : null}
        {/* 标签 Chip 行 */}
        <Stack direction="row" spacing={0.5} flexWrap="wrap">
          <Chip label="全部" size="small" variant={selectedTags.length === 0 ? 'filled' : 'outlined'}
            color={selectedTags.length === 0 ? 'primary' : 'default'}
            onClick={() => setSelectedTags([])} />
          {TAG_OPTIONS.map(t => (
            <Chip key={t} label={t} size="small"
              variant={selectedTags.includes(t) ? 'filled' : 'outlined'}
              color={selectedTags.includes(t) ? 'primary' : 'default'}
              onClick={() => setSelectedTags(prev =>
                prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t]
              )} />
          ))}
        </Stack>
        {/* 批量操作栏 */}
        {selection.length > 0 && (
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="body2" color="text.secondary">已选 {selection.length} 条</Typography>
            <Button size="small" variant="outlined" startIcon={<SendIcon />}
              onClick={() => submitApprovalMut.mutate(selection as number[])}>批量发审批</Button>
            <Button size="small" color="error" variant="outlined"
              onClick={() => batchDeleteMut.mutate(selection as number[])}>批量删除</Button>
          </Stack>
        )}
      </Stack>

      {isError ? (
        <Alert
          severity="error"
          action={<Button size="small" color="inherit" onClick={() => refetch()}>重试</Button>}
          data-testid="copy-library-list-error"
          data-contract-status="source-error"
          data-source-endpoint="/copy/library/search"
          data-no-local-copy-fallback="true"
        >
          /copy/library/search 文案库加载失败：{getErrorMessage(error)}
        </Alert>
      ) : isFetching && (data?.list ?? []).length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography color="text.secondary">加载中...</Typography>
        </Box>
      ) : (data?.list ?? []).length === 0 && !keyword && selectedTags.length === 0 && !minScore && !category && !statusFilter ? (
        <EmptyState
          title="还没有文案"
          description="创建第一条文案，开始构建可审批、可复用的文案库"
          action={{
            text: '新建文案',
            onClick: openCreate,
          }}
        />
      ) : (data?.list ?? []).length === 0 ? (
        <Alert
          severity="info"
          data-testid="copy-library-filter-empty"
          data-contract-status="server-empty"
          data-source-endpoint="/copy/library/search"
          data-no-local-copy-fallback="true"
        >
          当前筛选条件下暂无文案，请调整关键词、标签、品类或状态后重试。
        </Alert>
      ) : (
        <StandardDataGrid
          rows={data?.list ?? []}
          columns={columns}
          rowCount={data?.total ?? 0}
          loading={isFetching}
          showExport={false}
          checkboxSelection
          rowSelectionModel={selection}
          onRowSelectionModelChange={setSelection}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
          getRowId={r => (r as CopyItem).id}
        />
      )}

      {/* 预览抽屉 */}
      <Drawer anchor="right" open={!!preview} onClose={() => setPreview(null)}
        PaperProps={{ sx: { width: 600 } }}>
        {preview && (
          <Box
            sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}
            data-testid="copy-library-preview-drawer"
            data-copy-id={preview.id}
            data-use-count={preview.useCount ?? 0}
            data-contract-status="server-source-preview"
            data-source-endpoint="/copy/library/search"
          >
            <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography fontWeight={700} flex={1}>文案预览</Typography>
              <IconButton size="small" onClick={() => setPreview(null)}><CloseIcon /></IconButton>
            </Box>
            <Tabs value={previewTab} onChange={(_e, v) => setPreviewTab(v)} sx={{ px: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Tab label="文案详情" />
              <Tab label="使用记录" />
            </Tabs>
            <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
              {previewTab === 0 && (
                <Stack spacing={2}>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    <Chip label={preview.category || '未分类'} size="small" />
                    {STATUS_MAP[preview.status] && <Chip label={STATUS_MAP[preview.status].label} color={STATUS_MAP[preview.status].color} size="small" />}
                    <Typography variant="body2" color="text.secondary">效果分: <b>{preview.rating?.toFixed(1) ?? '—'}</b></Typography>
                    <Typography variant="body2" color="text.secondary">使用: <b>{preview.useCount}</b>次</Typography>
                  </Stack>
                  {preview.tags && (
                    <Stack direction="row" spacing={0.5} flexWrap="wrap">
                      {preview.tags.split(',').filter(Boolean).map(t => <Chip key={t} label={t.trim()} size="small" variant="outlined" />)}
                    </Stack>
                  )}
                  <Divider />
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 2 }}>{preview.content}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    预估时长：约 {Math.round(preview.content.length / 5)} 秒（{preview.content.length}字 ÷ 5字/秒）
                  </Typography>
                  <Divider />
                  <Stack direction="row" spacing={1}>
                    <Button size="small" startIcon={<ContentCopyIcon />}
                      onClick={() => { navigator.clipboard.writeText(preview.content); toast('已复制', 'success') }}>复制</Button>
                    <Button size="small" variant="outlined" onClick={() => submitApprovalMut.mutate([preview.id])}>发送审批</Button>
                  </Stack>
                </Stack>
              )}
              {previewTab === 1 && (
                <Stack spacing={1}>
                  <Alert
                    severity="warning"
                    data-testid="copy-library-usage-degradation"
                    data-contract-status="local-summary-only"
                    data-unsupported-endpoint="/copy/library/usage"
                    data-no-usage-request="true"
                  >
                    后端未提供文案使用明细接口，当前只展示 `useCount` 汇总值：{preview.useCount ?? 0} 次。
                  </Alert>
                </Stack>
              )}
            </Box>
          </Box>
        )}
      </Drawer>

      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editItem.id ? '编辑文案' : '新建文案'}</DialogTitle>
        <DialogContent dividers>
          <Stack
            spacing={2}
            sx={{ pt: 1 }}
            data-testid="copy-library-edit-dialog"
            data-contract-status="save-to-real-endpoint"
            data-ready-endpoint="/copy/library/save"
            data-input-retained-on-error="true"
          >
            <TextField
              label="标题"
              size="small"
              required
              value={editItem.title ?? ''}
              onChange={e => setEditItem(v => ({ ...v, title: e.target.value }))}
              fullWidth
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <FormControl size="small" fullWidth>
                <InputLabel>品类</InputLabel>
                <Select
                  label="品类"
                  value={editItem.category ?? ''}
                  onChange={e => setEditItem(v => ({ ...v, category: e.target.value }))}
                >
                  <MenuItem value="">未分类</MenuItem>
                  {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl size="small" fullWidth>
                <InputLabel>状态</InputLabel>
                <Select
                  label="状态"
                  value={editItem.status ?? 0}
                  onChange={e => setEditItem(v => ({ ...v, status: Number(e.target.value) }))}
                >
                  {Object.entries(STATUS_MAP).map(([k, v]) => <MenuItem key={k} value={Number(k)}>{v.label}</MenuItem>)}
                </Select>
              </FormControl>
            </Stack>
            <TextField
              label="标签"
              size="small"
              placeholder="开场话术,商品介绍"
              value={editItem.tags ?? ''}
              onChange={e => setEditItem(v => ({ ...v, tags: e.target.value }))}
              fullWidth
            />
            <TextField
              label="文案内容"
              size="small"
              required
              multiline
              minRows={5}
              value={editItem.content ?? ''}
              onChange={e => setEditItem(v => ({ ...v, content: e.target.value }))}
              fullWidth
            />
            {editError ? (
              <Alert
                severity="error"
                data-testid="copy-library-save-error"
                data-contract-status="source-error"
                data-source-endpoint="/copy/library/save"
                data-input-retained="true"
              >
                {editError}。保存失败不会关闭弹窗或丢失当前输入。
              </Alert>
            ) : null}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleSave} disabled={saveMut.isPending}>
            保存
          </Button>
        </DialogActions>
      </Dialog>

      {/* AI 生成文案 Dialog */}
      <Dialog open={aiOpen} onClose={() => { setAiOpen(false); setAiResults([]) }} maxWidth="md" fullWidth>
        <DialogTitle>AI 生成文案</DialogTitle>
        <DialogContent dividers>
          <Stack
            spacing={2.5}
            data-testid="copy-library-ai-dialog-workbench"
            data-ready-endpoints={contractValue(['/copy/ai/generate', '/copy/library/save'])}
            data-unsupported-actions="batch-ai-save"
            data-unsupported-endpoint="/copy/ai/batch-save"
            data-result-count={aiResults.length}
          >
            <Alert
              severity="info"
              variant="outlined"
              data-testid="copy-library-ai-contract-alert"
              data-contract-status="generate-then-explicit-save"
              data-ready-endpoints={contractValue(['/copy/ai/generate', '/copy/library/save'])}
              data-unsupported-endpoint="/copy/ai/batch-save"
            >
              文案 AI 真实调用 `/copy/ai/generate`，使用 `copy_processing` 任务模型配置；生成候选需点击保存后才会写入 `/copy/library/save`。
            </Alert>
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <FormControl size="small" fullWidth>
                  <InputLabel id="copy-ai-type-label">文案类型</InputLabel>
                  <Select
                    labelId="copy-ai-type-label"
                    id="copy-ai-type"
                    label="文案类型"
                    value={aiForm.copyType}
                    onChange={e => setAiForm(f => ({ ...f, copyType: e.target.value }))}
                  >
                    <MenuItem value="">不限</MenuItem>
                    {['开场话术', '商品介绍', '促单话术', '互动引导', '结束语'].map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={6}>
                <FormControl size="small" fullWidth>
                  <InputLabel id="copy-ai-category-label">分类</InputLabel>
                  <Select
                    labelId="copy-ai-category-label"
                    id="copy-ai-category"
                    label="分类"
                    value={aiForm.category}
                    onChange={e => setAiForm(f => ({ ...f, category: e.target.value }))}
                  >
                    <MenuItem value="">全部</MenuItem>
                    {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth size="small" label="生成提示词"
                  placeholder="描述你想要的文案风格、卖点..."
                  value={aiForm.prompt}
                  onChange={e => setAiForm(f => ({ ...f, prompt: e.target.value }))}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth size="small" label="核心关键词（逗号分隔，最多 5 个）"
                  placeholder="补水,美白,精华"
                  value={aiForm.keywords}
                  onChange={e => setAiForm(f => ({ ...f, keywords: e.target.value }))}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>文案风格</Typography>
                <RadioGroup row value={aiForm.style} onChange={e => setAiForm(f => ({ ...f, style: e.target.value }))}>
                  {['专业科学', '亲切温暖', '幽默活泼', '时尚潮流'].map(s => (
                    <FormControlLabel key={s} value={s} control={<Radio size="small" />} label={s} />
                  ))}
                </RadioGroup>
              </Grid>
              <Grid item xs={8}>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>时长</Typography>
                <RadioGroup row value={aiForm.duration} onChange={e => setAiForm(f => ({ ...f, duration: e.target.value }))}>
                  {['30s', '1min', '2min', '不限'].map(d => (
                    <FormControlLabel key={d} value={d} control={<Radio size="small" />} label={d} />
                  ))}
                </RadioGroup>
              </Grid>
              <Grid item xs={4}>
                <FormControl size="small" fullWidth>
                  <InputLabel id="copy-ai-count-label">生成数量</InputLabel>
                  <Select
                    labelId="copy-ai-count-label"
                    id="copy-ai-count"
                    label="生成数量"
                    value={aiForm.count}
                    onChange={e => setAiForm(f => ({ ...f, count: Number(e.target.value) }))}
                  >
                    {[1, 3, 5].map(n => <MenuItem key={n} value={n}>{n} 条</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
            <Button variant="contained" onClick={() => aiMut.mutate()} disabled={aiMut.isPending}>
              {aiMut.isPending ? 'AI 生成中...' : 'AI 生成 ✨'}
            </Button>
            {aiMut.isPending && <LinearProgress />}
            {aiError ? (
              <Alert
                severity="error"
                data-testid="copy-library-ai-error"
                data-contract-status="source-error"
                data-source-endpoint="/copy/ai/generate"
                data-input-retained="true"
                data-no-candidate-clear="true"
              >
                {aiError}。失败不会清空已生成候选。
              </Alert>
            ) : null}
            {aiResults.length > 0 && (
              <Stack spacing={1}>
                {aiResults.map((item, idx) => (
                  <Paper
                    key={idx}
                    variant="outlined"
                    sx={{ p: 1.5 }}
                    data-testid="copy-library-ai-candidate-card"
                    data-contract-status="generated-not-saved"
                    data-save-endpoint="/copy/library/save"
                  >
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="subtitle2" gutterBottom>{item.title}</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>{item.content}</Typography>
                      </Box>
                      <Tooltip title="保存到文案库">
                        <IconButton size="small" onClick={() => saveMut.mutate(item)}>
                          <ContentCopyIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                  </Paper>
                ))}
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setAiOpen(false); setAiResults([]) }}>关闭</Button>
        </DialogActions>
      </Dialog>

    </Box>
  )
}

// ─── 审批看板 Tab ──────────────────────────────────────────────────────────────
function ApprovalKanbanTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [approvalActionError, setApprovalActionError] = useState('')

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['copy-approvals'],
    queryFn: () => copyApi.approvalSearch({ page: 0, rows: 200 }),
  })

  const approveMut = useMutation({
    mutationFn: (id: number) => { setApprovalActionError(''); return copyApi.approvalApprove(id) },
    onSuccess: () => { toast('已通过', 'success'); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e) => { setApprovalActionError(`/copy/approval/get + /copy/approval/save 审批失败：${getErrorMessage(e)}`); toast('审批失败', 'error') },
  })
  const rejectMut = useMutation({
    mutationFn: ({ id, comment }: { id: number; comment: string }) => { setApprovalActionError(''); return copyApi.approvalReject(id, comment) },
    onSuccess: () => { toast('已拒绝', 'success'); qc.invalidateQueries({ queryKey: ['copy-approvals'] }) },
    onError: (e) => { setApprovalActionError(`/copy/approval/get + /copy/approval/save 驳回失败：${getErrorMessage(e)}`); toast('驳回失败', 'error') },
  })

  const allApprovals = data?.list ?? []
  const now = Date.now()

  const isOverdue = (item: CopyApproval) => {
    if (item.approvalStatus !== 2) return false
    return now - new Date(item.createTime).getTime() > 24 * 60 * 60 * 1000
  }

  const pending = allApprovals.filter(a => a.approvalStatus === 2)
  const approved = allApprovals.filter(a => a.approvalStatus === 1)
  const rejected = allApprovals.filter(a => a.approvalStatus === 0)
  const overdueCount = pending.filter(isOverdue).length
  const approvedToday = approved.filter(item => {
    if (!item.approvalTime) return false
    return new Date(item.approvalTime).toDateString() === new Date(now).toDateString()
  }).length
  const passRate = allApprovals.length > 0 ? Math.round((approved.length / allApprovals.length) * 100) : 0
  const localStats = {
    pending: pending.length,
    approvedToday,
    passRate,
    overdueCount,
  }

  const columns: Array<{
    label: string
    items: CopyApproval[]
    statusKey: number
    color: 'warning' | 'success' | 'error'
  }> = [
    { label: '待审核', items: pending, statusKey: 2, color: 'warning' },
    { label: '已通过', items: approved, statusKey: 1, color: 'success' },
    { label: '已拒绝', items: rejected, statusKey: 0, color: 'error' },
  ]

  const SLA_STATS: Array<{
    label: string
    key: 'pending' | 'approvedToday' | 'passRate' | 'overdueCount'
    color: 'warning' | 'success' | 'primary' | 'secondary'
    suffix?: string
  }> = [
    { label: '待审核', key: 'pending', color: 'warning' },
    { label: '今日通过', key: 'approvedToday', color: 'success' },
    { label: '通过率', key: 'passRate', color: 'primary', suffix: '%' },
    { label: 'SLA 超时', key: 'overdueCount', color: 'secondary' },
  ]

  return (
    <Box
      data-testid="copy-library-approval-workbench"
      data-contract-scope="copy-approval-kanban"
      data-ready-endpoints={contractValue(['/copy/approval/search', '/copy/approval/get', '/copy/approval/save'])}
      data-unsupported-actions={contractValue(['approval-stats', 'approval-revise'])}
      data-unsupported-endpoints={contractValue(['/copy/approval/stats', '/copy/approval/revise'])}
      data-stat-source="local-derived"
      data-total-count={allApprovals.length}
      data-pending-count={pending.length}
      data-approved-count={approved.length}
      data-rejected-count={rejected.length}
      data-no-local-approval-fallback="true"
      data-row-retained-on-action-error="true"
    >
      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="copy-library-approval-contract-alert"
        data-contract-status="ready-with-local-stats"
        data-ready-endpoints={contractValue(['/copy/approval/search', '/copy/approval/get', '/copy/approval/save'])}
        data-unsupported-endpoints={contractValue(['/copy/approval/stats', '/copy/approval/revise'])}
        data-no-stats-request="true"
      >
        审批列表、通过、拒绝走 `/copy/approval/search|get|save` 真实接口；统计卡片接口尚未落库，页面会以列表数量兜底。
      </Alert>
      {isError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={<Button size="small" color="inherit" onClick={() => refetch()}>重试</Button>}
          data-testid="copy-library-approval-list-error"
          data-contract-status="source-error"
          data-source-endpoint="/copy/approval/search"
          data-no-local-approval-fallback="true"
        >
          /copy/approval/search 审批列表加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      <Alert
        severity="warning"
        sx={{ mb: 2 }}
        data-testid="copy-library-approval-stats-degradation"
        data-contract-status="local-derived"
        data-unsupported-endpoint="/copy/approval/stats"
        data-no-stats-request="true"
      >
        /copy/approval/stats 审批统计接口尚未落库，当前统计卡片使用本页 `/copy/approval/search` 列表数据本地派生。
      </Alert>
      {approvalActionError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="copy-library-approval-action-error"
          data-contract-status="source-error"
          data-source-endpoints="/copy/approval/get,/copy/approval/save"
          data-row-retained-on-action-error="true"
          data-no-local-card-move="true"
        >
          {approvalActionError}。失败不会本地移动审批卡片。
        </Alert>
      ) : null}
      {/* SLA 统计卡片 */}
      <Stack direction="row" spacing={2} mb={3}>
        {SLA_STATS.map(s => (
          <Paper
            key={s.key}
            variant="outlined"
            data-testid="copy-library-sla-stat-surface"
            data-summary-metric={s.key}
            data-contract-status="local-derived"
            data-source-endpoint="/copy/approval/search"
            sx={{
              p: 1.5,
              minWidth: 110,
              textAlign: 'center',
              flex: 1,
              bgcolor: (theme) => alpha(theme.palette[s.color].main, theme.palette.mode === 'dark' ? 0.14 : 0.06),
              borderColor: (theme) => alpha(theme.palette[s.color].main, theme.palette.mode === 'dark' ? 0.34 : 0.22),
            }}
          >
            <Typography variant="h5" fontWeight={700} color={`${s.color}.main`}>
              {localStats[s.key]}{s.suffix ?? ''}
            </Typography>
            <Typography variant="caption" color="text.secondary">{s.label}</Typography>
          </Paper>
        ))}
      </Stack>

      {/* 三列看板 */}
      {isLoading ? <Typography color="text.secondary">加载中...</Typography> : !isError && allApprovals.length === 0 ? (
        <Alert severity="info">暂无审批记录。请先在文案库选择文案并提交审批。</Alert>
      ) : !isError && (
        <Grid container spacing={2} alignItems="flex-start">
          {columns.map(col => (
            <Grid item xs={12} md={4} key={col.label}>
              <Paper variant="outlined" sx={{ borderRadius: 2, overflow: 'hidden' }}>
                <Box
                  data-testid="copy-library-kanban-header-surface"
                  data-kanban-status={col.statusKey}
                  data-contract-status="server-list-grouped"
                  data-source-endpoint="/copy/approval/search"
                  data-item-count={col.items.length}
                  sx={{
                    px: 2,
                    py: 1.5,
                    bgcolor: (theme) => alpha(theme.palette[col.color].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                    borderBottom: '1px solid',
                    borderColor: (theme) => alpha(theme.palette[col.color].main, theme.palette.mode === 'dark' ? 0.36 : 0.2),
                  }}
                >
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography fontWeight={700} color={`${col.color}.main`}>{col.label}</Typography>
                    <Chip
                      label={col.items.length}
                      size="small"
                      color={col.color}
                      sx={{ fontWeight: 700, color: 'common.white' }}
                    />
                  </Stack>
                </Box>
                <Stack spacing={1.5} sx={{ p: 1.5, maxHeight: 600, overflowY: 'auto' }}>
                  {col.items.length === 0 ? (
                    <Typography color="text.secondary" variant="body2" align="center" py={3}>暂无记录</Typography>
                  ) : col.items.map(item => {
                    const overdue = isOverdue(item)
                    return (
                      <Card
                        key={item.id}
                        variant="outlined"
                        data-testid={overdue ? 'copy-library-overdue-approval-card' : undefined}
                        data-approval-id={item.id}
                        data-approval-status={item.approvalStatus}
                        data-contract-status="server-source"
                        data-source-endpoint="/copy/approval/search"
                        sx={{
                          borderColor: overdue ? 'error.main' : 'divider',
                          borderWidth: overdue ? 2 : 1,
                          bgcolor: overdue
                            ? (theme) => alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.1 : 0.04)
                            : 'background.paper',
                        }}
                      >
                        <CardContent sx={{ pb: '8px !important', pt: 1.5, px: 1.5 }}>
                          <Stack spacing={0.5}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {overdue && <Chip icon={<WarningAmberIcon />} label="SLA超时" size="small" color="error" variant="outlined" />}
                              <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>{formatDate(item.createTime)}</Typography>
                            </Stack>
                            <Typography variant="body2" sx={{ fontSize: 13, lineHeight: 1.5 }}>
                              {String(item.copyContent ?? item.copyTitle ?? item.comments ?? '').slice(0, 80)}
                              {String(item.copyContent ?? item.copyTitle ?? item.comments ?? '').length > 80 ? '…' : ''}
                            </Typography>
                            {item.comments && col.statusKey !== 0 && (
                              <Typography variant="caption" color="text.secondary">意见：{item.comments}</Typography>
                            )}
                            {item.approvalTime && (
                              <Typography variant="caption" color="text.secondary">审批时间：{formatDate(item.approvalTime)}</Typography>
                            )}
                            {col.statusKey === 2 && (
                              <Stack direction="row" spacing={1} pt={0.5}>
                                <Button size="small" variant="contained" color="success" sx={{ flex: 1 }}
                                  onClick={() => approveMut.mutate(item.id)} disabled={approveMut.isPending}>通过</Button>
                                <Button size="small" variant="outlined" color="error" sx={{ flex: 1 }}
                                  onClick={() => rejectMut.mutate({ id: item.id, comment: '' })} disabled={rejectMut.isPending}>拒绝</Button>
                              </Stack>
                            )}
                          </Stack>
                        </CardContent>
                      </Card>
                    )
                  })}
                </Stack>
              </Paper>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  )
}

// ─── 文案模板 Tab ──────────────────────────────────────────────────────────────
function CopyTemplateTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [editItem, setEditItem] = useState<Partial<CopyTemplate> | null>(null)
  const [preview, setPreview] = useState<CopyTemplate | null>(null)
  const [templateActionError, setTemplateActionError] = useState('')

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['copy-templates', keyword, category, page, pageSize],
    queryFn: () => copyApi.templateList({ page, rows: pageSize, keyword: keyword || undefined, category: category || undefined }),
  })

  const saveMut = useMutation({
    mutationFn: (params: Partial<CopyTemplate>) => { setTemplateActionError(''); return copyApi.templateSave(params) },
    onSuccess: () => { toast('保存成功', 'success'); setEditItem(null); qc.invalidateQueries({ queryKey: ['copy-templates'] }) },
    onError: (e) => { setTemplateActionError(`/copy/template/save 保存失败：${getErrorMessage(e)}`); toast('保存失败', 'error') },
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => { setTemplateActionError(''); return copyApi.templateDelete(id) },
    onSuccess: () => { toast('已删除', 'success'); qc.invalidateQueries({ queryKey: ['copy-templates'] }) },
    onError: (e) => { setTemplateActionError(`/copy/template/delete 删除失败：${getErrorMessage(e)}`); toast('删除失败', 'error') },
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const columns: GridColDef[] = [
    { field: 'templateName', headerName: '模板名称', flex: 1.5, minWidth: 140 },
    { field: 'category', headerName: '分类', width: 90,
      renderCell: (p: GridRenderCellParams) => <Chip label={String(p.value ?? '')} size="small" variant="outlined" /> },
    { field: 'content', headerName: '模板内容', flex: 3, minWidth: 200,
      renderCell: (p: GridRenderCellParams) => (
        <Typography variant="body2" noWrap sx={{ maxWidth: 300 }}>{String(p.value ?? '').slice(0, 80)}{String(p.value ?? '').length > 80 ? '…' : ''}</Typography>
      ) },
    { field: 'variables', headerName: '变量', width: 160,
      renderCell: (p: GridRenderCellParams) => {
        const vars = String(p.value ?? '').split(',').filter(Boolean)
        return <Stack direction="row" spacing={0.5} flexWrap="wrap">{vars.slice(0, 3).map(v => <Chip key={v} label={v} size="small" sx={{ fontSize: 11 }} />)}{vars.length > 3 && <Chip label={`+${vars.length - 3}`} size="small" />}</Stack>
      } },
    { field: 'status', headerName: '状态', width: 80,
      renderCell: (p: GridRenderCellParams) => <Chip label={p.value === 1 ? '启用' : '停用'} size="small" color={p.value === 1 ? 'success' : 'default'} /> },
    { field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: (p: GridRenderCellParams) => <Typography variant="body2">{formatDate(String(p.value ?? ''))}</Typography> },
    { field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: (p: GridRenderCellParams) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => setPreview(p.row as CopyTemplate)}>预览</Button>
          <Button size="small" onClick={() => setEditItem(p.row as CopyTemplate)}>编辑</Button>
          <Button size="small" color="error" onClick={() => deleteMut.mutate((p.row as CopyTemplate).id)}>删除</Button>
        </Stack>
      ) },
  ]

  return (
    <Box
      data-testid="copy-library-template-workbench"
      data-contract-scope="copy-template-inline"
      data-ready-endpoints={contractValue(['/copy/template/search', '/copy/template/save', '/copy/template/delete'])}
      data-unsupported-actions="server-side-batch-delete"
      data-unsupported-endpoint="/copy/template/batch-delete"
      data-row-count={total}
      data-no-local-template-fallback="true"
      data-row-retained-on-action-error="true"
    >
      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="copy-library-template-contract-alert"
        data-contract-status="ready-with-explicit-degradation"
        data-ready-endpoints={contractValue(['/copy/template/search', '/copy/template/save', '/copy/template/delete'])}
        data-unsupported-endpoint="/copy/template/batch-delete"
      >
        模板列表、新建、编辑、删除走 `/copy/template/*` 真实接口；服务端批量模板事务删除尚未落库，页面不提供伪批量按钮。
      </Alert>
      <Stack direction="row" spacing={2} mb={2} alignItems="center">
        <TextField size="small" placeholder="搜索模板名称" value={keyword} onChange={e => setKeyword(e.target.value)} sx={{ width: 220 }} />
        <FormControl size="small" sx={{ minWidth: 100 }}>
          <InputLabel>分类</InputLabel>
          <Select label="分类" value={category} onChange={e => setCategory(e.target.value)}>
            <MenuItem value="">全部</MenuItem>
            {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </Select>
        </FormControl>
        <Box sx={{ flex: 1 }} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditItem({ status: 1 })}>新建模板</Button>
      </Stack>

      {isError ? (
        <Alert
          severity="error"
          action={<Button size="small" color="inherit" onClick={() => refetch()}>重试</Button>}
          data-testid="copy-library-template-list-error"
          data-contract-status="source-error"
          data-source-endpoint="/copy/template/search"
          data-no-local-template-fallback="true"
        >
          /copy/template/search 文案模板加载失败：{getErrorMessage(error)}
        </Alert>
      ) : !isLoading && rows.length === 0 ? (
        <EmptyState title="暂无文案模板" description="新增模板后，可在文案生产和复用流程中引用变量占位符。" action={{ text: '新建模板', onClick: () => setEditItem({ status: 1 }) }} />
      ) : (
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isLoading}
          showExport={false}
          paginationMode="server"
          rowCount={total}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
          pageSizeOptions={[20, 50]}
          getRowId={r => (r as CopyTemplate).id}
        />
      )}
      {templateActionError ? (
        <Alert
          severity="error"
          sx={{ mt: 2 }}
          data-testid="copy-library-template-action-error"
          data-contract-status="source-error"
          data-source-endpoints="/copy/template/save,/copy/template/delete"
          data-row-retained-on-action-error="true"
        >
          {templateActionError}。失败不会移除模板行或关闭编辑弹窗。
        </Alert>
      ) : null}

      {/* 预览 Drawer */}
      <Drawer anchor="right" open={!!preview} onClose={() => setPreview(null)}
        PaperProps={{ sx: { width: 520, p: 3 } }}>
        {preview && (
          <Box
            data-testid="copy-library-template-preview-drawer"
            data-template-id={preview.id}
            data-contract-status="server-source-preview"
            data-source-endpoint="/copy/template/search"
          >
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">{preview.templateName}</Typography>
              <IconButton onClick={() => setPreview(null)}><CloseIcon /></IconButton>
            </Stack>
            <Stack direction="row" spacing={1} mb={2}>
              <Chip label={preview.category} size="small" variant="outlined" />
              <Chip label={preview.status === 1 ? '启用' : '停用'} size="small" color={preview.status === 1 ? 'success' : 'default'} />
            </Stack>
            <Divider sx={{ mb: 2 }} />
            <Typography variant="subtitle2" gutterBottom>模板内容（橙色为变量占位符）</Typography>
            <Paper
              variant="outlined"
              data-testid="copy-template-preview-content-surface"
              sx={{
                p: 2,
                mb: 2,
                bgcolor: (theme) => alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.08 : 0.03),
              }}
            >
              <TemplateContent content={preview.content} />
            </Paper>
            {preview.variables && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>变量列表</Typography>
                <Stack direction="row" flexWrap="wrap" gap={1}>
                  {String(preview.variables).split(',').filter(Boolean).map(v => (
                    <Chip
                      key={v}
                      label={v}
                      size="small"
                      data-testid="copy-template-variable-chip-surface"
                      sx={{
                        color: 'warning.main',
                        border: '1px solid',
                        borderColor: 'warning.main',
                        bgcolor: (theme) => alpha(theme.palette.warning.main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                      }}
                    />
                  ))}
                </Stack>
              </Box>
            )}
          </Box>
        )}
      </Drawer>

      {/* 编辑 Dialog */}
      <Dialog open={!!editItem} onClose={() => setEditItem(null)} maxWidth="md" fullWidth>
        <DialogTitle>{editItem?.id ? '编辑模板' : '新建模板'}</DialogTitle>
        <DialogContent dividers>
          <Stack
            spacing={2}
            pt={1}
            data-testid="copy-library-template-edit-dialog"
            data-contract-status="save-to-real-endpoint"
            data-save-endpoint="/copy/template/save"
          >
            <TextField fullWidth size="small" label="模板名称" value={editItem?.templateName ?? ''}
              onChange={e => setEditItem(v => v ? { ...v, templateName: e.target.value } : v)} />
            <Stack direction="row" spacing={2}>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>分类</InputLabel>
                <Select label="分类" value={editItem?.category ?? ''}
                  onChange={e => setEditItem(v => v ? { ...v, category: e.target.value } : v)}>
                  {CATEGORY_OPTIONS.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 100 }}>
                <InputLabel>状态</InputLabel>
                <Select label="状态" value={editItem?.status ?? 1}
                  onChange={e => setEditItem(v => v ? { ...v, status: Number(e.target.value) } : v)}>
                  <MenuItem value={1}>启用</MenuItem>
                  <MenuItem value={0}>停用</MenuItem>
                </Select>
              </FormControl>
            </Stack>
            <TextField fullWidth multiline minRows={5} size="small" label="模板内容（用 {变量名} 标记变量）"
              value={editItem?.content ?? ''}
              onChange={e => setEditItem(v => v ? { ...v, content: e.target.value } : v)} />
            <TextField fullWidth size="small" label="变量列表（逗号分隔）"
              placeholder="变量1,变量2"
              value={editItem?.variables ?? ''}
              onChange={e => setEditItem(v => v ? { ...v, variables: e.target.value } : v)} />
            {templateActionError ? <Alert severity="error">{templateActionError}。保存失败会保留当前输入。</Alert> : null}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditItem(null)}>取消</Button>
          <Button variant="contained" disabled={saveMut.isPending}
            onClick={() => editItem && saveMut.mutate(editItem)}>保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

// ─── 主页面 ────────────────────────────────────────────────────────────────────
export default function CopyLibraryPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box
      sx={{ p: 3 }}
      data-testid="copy-library-workbench"
      data-contract-scope="copy-library-composite"
      data-ready-endpoints={contractValue(COPY_LIBRARY_READY_ENDPOINTS)}
      data-unsupported-actions={contractValue(COPY_LIBRARY_UNSUPPORTED_ACTIONS)}
      data-unsupported-endpoints={contractValue(COPY_LIBRARY_UNSUPPORTED_ENDPOINTS)}
      data-active-tab={tab === 0 ? 'library' : tab === 1 ? 'approval' : 'template'}
    >
      <PageHeader
        title="文案库"
        subtitle="管理可复用文案、审批流和变量模板；未落库能力会在页面内明确降级。"
      />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="文案库" />
        <Tab label="审批看板" />
        <Tab label="文案模板" />
      </Tabs>
      {tab === 0 && <CopyLibraryTab />}
      {tab === 1 && <ApprovalKanbanTab />}
      {tab === 2 && <CopyTemplateTab />}
    </Box>
  )
}
