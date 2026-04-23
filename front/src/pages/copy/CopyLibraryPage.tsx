import { useState } from 'react'
import {
  Box, Tab, Tabs, TextField, Button, Stack, Chip, MenuItem,
  Drawer, Typography, Divider, IconButton, Tooltip, Paper,
  LinearProgress, Dialog, DialogTitle, DialogContent,
  DialogActions, Card, CardContent, ToggleButton,
  Select, FormControl, InputLabel, Grid, RadioGroup, Radio, FormControlLabel,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import SearchIcon from '@mui/icons-material/Search'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import LabelIcon from '@mui/icons-material/Label'
import SendIcon from '@mui/icons-material/Send'
import type { GridColDef, GridRenderCellParams, GridRowSelectionModel } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { copyApi, type CopyItem, type CopyApproval, type CopyTemplate } from '@/api/copy'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' | 'error' }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'warning' },
  2: { label: '已通过', color: 'success' },
  3: { label: '已拒绝', color: 'error' },
  4: { label: '已归档', color: 'default' },
}

const TAG_OPTIONS = ['开场话术', '商品介绍', '促单', '互动', '结尾']
const CATEGORY_OPTIONS = ['护肤', '彩妆']

function ScoreBar({ value }: { value?: number }) {
  const v = value ?? 0
  const color = v >= 9 ? '#4caf50' : v >= 7 ? '#ff9800' : '#bdbdbd'
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, width: '100%' }}>
      <LinearProgress variant="determinate" value={v * 10}
        sx={{ flex: 1, height: 6, borderRadius: 3, '& .MuiLinearProgress-bar': { bgcolor: color } }} />
      <Typography variant="caption" sx={{ minWidth: 24, color }}>{v > 0 ? v.toFixed(1) : '—'}</Typography>
    </Box>
  )
}

function TemplateContent({ content }: { content: string }) {
  const parts = content.split(/({[^}]+})/g)
  return (
    <Typography component="span" variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>
      {parts.map((p, i) =>
        /^{[^}]+}$/.test(p)
          ? <Box key={i} component="span" sx={{ color: '#ed6c02', fontWeight: 600 }}>{p}</Box>
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
  const [aiResults, setAiResults] = useState<CopyItem[]>([])
  const [batchTagOpen, setBatchTagOpen] = useState(false)
  const [batchTags, setBatchTags] = useState<string[]>([])

  const queryParams = {
    page,
    rows: pageSize,
    keyword: keyword || undefined,
    tags: selectedTags.length ? selectedTags.join(',') : undefined,
    minScore: minScore ? Number(minScore) : undefined,
    category: category || undefined,
    status: statusFilter !== '' ? Number(statusFilter) : undefined,
  }

  const { data, isFetching } = useQuery({
    queryKey: ['copy-list', queryParams, semantic],
    queryFn: () =>
      semantic && keyword
        ? copyApi.semanticSearch(keyword, { page, rows: pageSize })
        : copyApi.list(queryParams),
  })

  const { data: usageData } = useQuery({
    queryKey: ['copy-usage', preview?.id, previewTab],
    queryFn: () => copyApi.usageList(preview!.id),
    enabled: !!preview && previewTab === 1,
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => copyApi.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); toast('已删除', 'success') },
  })
  const batchDeleteMut = useMutation({
    mutationFn: (ids: number[]) => copyApi.batchDelete(ids),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); setSelection([]); toast('批量删除成功', 'success') },
  })
  const submitApprovalMut = useMutation({
    mutationFn: (ids: number[]) => {
      if (ids.length > 50) { toast('最多批量操作 50 条', 'warning'); return Promise.reject() }
      return copyApi.batchSubmitApproval(ids)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); setSelection([]); toast('已提交审批', 'success') },
  })
  const batchTagMut = useMutation({
    mutationFn: ({ ids, tags }: { ids: number[]; tags: string[] }) => copyApi.batchTag(ids, tags),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); setSelection([]); setBatchTagOpen(false); toast('标签已更新', 'success') },
  })
  const aiMut = useMutation({
    mutationFn: () => copyApi.aiGenerate({ ...aiForm }),
    onSuccess: (res) => { setAiResults(res) },
    onError: () => toast('AI 生成失败，请重试', 'error'),
  })
  const saveMut = useMutation({
    mutationFn: (item: Partial<CopyItem>) => copyApi.save(item),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['copy-list'] }); toast('已保存', 'success') },
  })

  const handleExport = () => {
    copyApi.exportCsv(queryParams).then(r => {
      window.open(r.downloadUrl, '_blank')
    }).catch(() => toast('导出失败', 'error'))
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
          <Button size="small" onClick={() => submitApprovalMut.mutate([(row as CopyItem).id])}>发审批</Button>
          <Button size="small" color="error" onClick={() => deleteMut.mutate((row as CopyItem).id)}>删除</Button>
        </Stack>
      ) },
  ]

  return (
    <Box>
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
          <Button size="small" variant="outlined" startIcon={<FileDownloadIcon />} onClick={handleExport}>导出CSV</Button>
          <Button size="small" variant="outlined" startIcon={<SmartToyIcon />} onClick={() => { setAiOpen(true); setAiResults([]) }}>✨ AI生成</Button>
          <Button size="small" variant="contained" startIcon={<AddIcon />} sx={{ ml: 'auto' }}
            onClick={() => saveMut.mutate({ title: '新文案', content: '', status: 0 })}>新建</Button>
        </Stack>
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
            <Button size="small" variant="outlined" startIcon={<LabelIcon />}
              onClick={() => setBatchTagOpen(true)}>批量打标签</Button>
            <Button size="small" color="error" variant="outlined"
              onClick={() => batchDeleteMut.mutate(selection as number[])}>批量删除</Button>
          </Stack>
        )}
      </Stack>

      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        rowCount={data?.total ?? 0}
        loading={isFetching}
        checkboxSelection
        rowSelectionModel={selection}
        onRowSelectionModelChange={setSelection}
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as CopyItem).id}
      />

      {/* 预览抽屉 */}
      <Drawer anchor="right" open={!!preview} onClose={() => setPreview(null)}
        PaperProps={{ sx: { width: 600 } }}>
        {preview && (
          <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
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
                  {(usageData ?? []).length === 0
                    ? <Typography color="text.secondary" variant="body2">暂无使用记录</Typography>
                    : (usageData ?? []).map((r, i) => (
                        <Paper key={i} variant="outlined" sx={{ p: 1.5 }}>
                          <Typography variant="body2">场次: {String(r.sessionTitle ?? '')} — {String(r.usedAt ?? '')}</Typography>
                        </Paper>
                      ))
                  }
                </Stack>
              )}
            </Box>
          </Box>
        )}
      </Drawer>

      {/* AI 生成文案 Dialog */}
      <Dialog open={aiOpen} onClose={() => { setAiOpen(false); setAiResults([]) }} maxWidth="md" fullWidth>
        <DialogTitle>AI 生成文案</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2.5}>
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <FormControl size="small" fullWidth>
                  <InputLabel>文案类型</InputLabel>
                  <Select label="文案类型" value={aiForm.copyType} onChange={e => setAiForm(f => ({ ...f, copyType: e.target.value }))}>
                    <MenuItem value="">不限</MenuItem>
                    {['开场话术', '商品介绍', '促单话术', '互动引导', '结束语'].map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={6}>
                <FormControl size="small" fullWidth>
                  <InputLabel>分类</InputLabel>
                  <Select label="分类" value={aiForm.category} onChange={e => setAiForm(f => ({ ...f, category: e.target.value }))}>
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
                  <InputLabel>生成数量</InputLabel>
                  <Select label="生成数量" value={aiForm.count} onChange={e => setAiForm(f => ({ ...f, count: Number(e.target.value) }))}>
                    {[1, 3, 5].map(n => <MenuItem key={n} value={n}>{n} 条</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
            <Button variant="contained" onClick={() => aiMut.mutate()} disabled={aiMut.isPending}>
              {aiMut.isPending ? 'AI 生成中...' : 'AI 生成 ✨'}
            </Button>
            {aiResults.length > 0 && (
              <Stack spacing={1}>
                {aiResults.map((item, idx) => (
                  <Paper key={idx} variant="outlined" sx={{ p: 1.5 }}>
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

      {/* 批量打标签 Dialog */}
      <Dialog open={batchTagOpen} onClose={() => setBatchTagOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>批量打标签</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary" mb={1}>已选 {selection.length} 条文案，选择要添加的标签：</Typography>
          <Stack direction="row" flexWrap="wrap" gap={1}>
            {TAG_OPTIONS.map(t => (
              <Chip
                key={t} label={t}
                onClick={() => setBatchTags(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t])}
                color={batchTags.includes(t) ? 'primary' : 'default'}
                variant={batchTags.includes(t) ? 'filled' : 'outlined'}
              />
            ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBatchTagOpen(false)}>取消</Button>
          <Button variant="contained" disabled={batchTags.length === 0 || batchTagMut.isPending}
            onClick={() => batchTagMut.mutate({ ids: selection as number[], tags: batchTags })}>确认</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

// ─── 审批看板 Tab ──────────────────────────────────────────────────────────────
function ApprovalKanbanTab() {
  const toast = useToast()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['copy-approvals'],
    queryFn: () => copyApi.approvalSearch({ page: 0, rows: 200 }),
  })

  const { data: stats } = useQuery({
    queryKey: ['copy-approval-stats'],
    queryFn: () => copyApi.approvalStats(),
  })

  const approveMut = useMutation({
    mutationFn: (id: number) => copyApi.approvalApprove(id),
    onSuccess: () => { toast('已通过', 'success'); qc.invalidateQueries({ queryKey: ['copy-approvals'] }); qc.invalidateQueries({ queryKey: ['copy-approval-stats'] }) },
    onError: () => toast('操作失败', 'error'),
  })
  const rejectMut = useMutation({
    mutationFn: ({ id, comment }: { id: number; comment: string }) => copyApi.approvalReject(id, comment),
    onSuccess: () => { toast('已拒绝', 'success'); qc.invalidateQueries({ queryKey: ['copy-approvals'] }); qc.invalidateQueries({ queryKey: ['copy-approval-stats'] }) },
    onError: () => toast('操作失败', 'error'),
  })

  const allApprovals = data?.list ?? []
  const statsData = stats
  const now = Date.now()

  const isOverdue = (item: CopyApproval) => {
    if (item.approvalStatus !== 0) return false
    return now - new Date(item.createTime).getTime() > 24 * 60 * 60 * 1000
  }

  const pending = allApprovals.filter(a => a.approvalStatus === 0)
  const approved = allApprovals.filter(a => a.approvalStatus === 1)
  const rejected = allApprovals.filter(a => a.approvalStatus === 2)

  const columns = [
    { label: '待审批', items: pending, statusKey: 0, headerBg: '#fff3e0', headerColor: '#e65100' },
    { label: '已通过', items: approved, statusKey: 1, headerBg: '#e8f5e9', headerColor: '#2e7d32' },
    { label: '已拒绝', items: rejected, statusKey: 2, headerBg: '#ffebee', headerColor: '#c62828' },
  ]

  const SLA_STATS = [
    { label: '待审批', key: 'pending', color: '#f57c00' },
    { label: '今日通过', key: 'approvedToday', color: '#388e3c' },
    { label: '通过率', key: 'passRate', color: '#1565c0', suffix: '%' },
    { label: 'SLA 超时', key: 'overdueCount', color: '#9c27b0' },
  ]

  return (
    <Box>
      {/* SLA 统计卡片 */}
      <Stack direction="row" spacing={2} mb={3}>
        {SLA_STATS.map(s => (
          <Paper key={s.key} variant="outlined" sx={{ p: 1.5, minWidth: 110, textAlign: 'center', flex: 1 }}>
            <Typography variant="h5" fontWeight={700} color={s.color}>
              {statsData ? ((statsData[s.key] as number) ?? 0) : '—'}{s.suffix ?? ''}
            </Typography>
            <Typography variant="caption" color="text.secondary">{s.label}</Typography>
          </Paper>
        ))}
      </Stack>

      {/* 三列看板 */}
      {isLoading ? <Typography color="text.secondary">加载中...</Typography> : (
        <Grid container spacing={2} alignItems="flex-start">
          {columns.map(col => (
            <Grid item xs={12} md={4} key={col.label}>
              <Paper variant="outlined" sx={{ borderRadius: 2, overflow: 'hidden' }}>
                <Box sx={{ px: 2, py: 1.5, bgcolor: col.headerBg }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography fontWeight={700} color={col.headerColor}>{col.label}</Typography>
                    <Chip label={col.items.length} size="small" sx={{ bgcolor: col.headerColor, color: '#fff', fontWeight: 700 }} />
                  </Stack>
                </Box>
                <Stack spacing={1.5} sx={{ p: 1.5, maxHeight: 600, overflowY: 'auto' }}>
                  {col.items.length === 0 ? (
                    <Typography color="text.secondary" variant="body2" align="center" py={3}>暂无记录</Typography>
                  ) : col.items.map(item => {
                    const overdue = isOverdue(item)
                    return (
                      <Card key={item.id} variant="outlined" sx={{ borderColor: overdue ? '#f44336' : 'divider', borderWidth: overdue ? 2 : 1 }}>
                        <CardContent sx={{ pb: '8px !important', pt: 1.5, px: 1.5 }}>
                          <Stack spacing={0.5}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {overdue && <Chip icon={<WarningAmberIcon />} label="SLA超时" size="small" color="error" variant="outlined" />}
                              <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>{formatDate(item.createTime)}</Typography>
                            </Stack>
                            <Typography variant="body2" sx={{ fontSize: 13, lineHeight: 1.5 }}>
                              {String(item.copyContent ?? item.comments ?? '').slice(0, 80)}
                              {String(item.copyContent ?? item.comments ?? '').length > 80 ? '…' : ''}
                            </Typography>
                            {item.comments && col.statusKey !== 0 && (
                              <Typography variant="caption" color="text.secondary">意见：{item.comments}</Typography>
                            )}
                            {item.approvalTime && (
                              <Typography variant="caption" color="text.secondary">审批时间：{formatDate(item.approvalTime)}</Typography>
                            )}
                            {col.statusKey === 0 && (
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

  const { data, isLoading } = useQuery({
    queryKey: ['copy-templates', keyword, category, page, pageSize],
    queryFn: () => copyApi.templateList({ page, rows: pageSize, templateName: keyword || undefined, category: category || undefined }),
  })

  const saveMut = useMutation({
    mutationFn: (params: Partial<CopyTemplate>) => copyApi.templateSave(params),
    onSuccess: () => { toast('保存成功', 'success'); setEditItem(null); qc.invalidateQueries({ queryKey: ['copy-templates'] }) },
    onError: () => toast('保存失败', 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => copyApi.templateDelete(id),
    onSuccess: () => { toast('已删除', 'success'); qc.invalidateQueries({ queryKey: ['copy-templates'] }) },
    onError: () => toast('删除失败', 'error'),
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
    <Box>
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

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        paginationMode="server"
        rowCount={total}
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        pageSizeOptions={[20, 50]}
        getRowId={r => (r as CopyTemplate).id}
      />

      {/* 预览 Drawer */}
      <Drawer anchor="right" open={!!preview} onClose={() => setPreview(null)}
        PaperProps={{ sx: { width: 520, p: 3 } }}>
        {preview && (
          <Box>
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
            <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: '#fafafa' }}>
              <TemplateContent content={preview.content} />
            </Paper>
            {preview.variables && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>变量列表</Typography>
                <Stack direction="row" flexWrap="wrap" gap={1}>
                  {String(preview.variables).split(',').filter(Boolean).map(v => (
                    <Chip key={v} label={v} size="small" sx={{ color: '#e65100', border: '1px solid #e65100', bgcolor: '#fff3e0' }} />
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
          <Stack spacing={2} pt={1}>
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
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" fontWeight={700} mb={2}>文案库</Typography>
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
