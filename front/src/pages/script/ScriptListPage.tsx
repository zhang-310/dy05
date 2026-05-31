import { useState, useCallback, useRef } from 'react'
import {
  Alert, Box, Tab, Tabs, TextField, Button, Chip, Stack, MenuItem,
  Drawer, Typography, Divider, IconButton, Tooltip, Paper,
  LinearProgress, InputAdornment, Dialog, DialogTitle,
  DialogContent, DialogActions, Switch, FormControlLabel,
  Grid, Card, CardContent, CardActions,
} from '@mui/material'
import { alpha, type SxProps, type Theme } from '@mui/material/styles'
import type { SystemStyleObject } from '@mui/system'
import AddIcon from '@mui/icons-material/Add'
import VisibilityIcon from '@mui/icons-material/Visibility'
import CloseIcon from '@mui/icons-material/Close'
import SearchIcon from '@mui/icons-material/Search'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef, GridRenderCellParams, GridRowSelectionModel } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader } from '@/components/base'
import { scriptApi, type ScriptItem, type ScriptSave, type ScriptTemplate, type ViolationWord, type BatchCheckRow } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const SCRIPT_SOURCES = [
  { value: 'manual', label: '手工录入' },
  { value: 'live', label: '直播生成' },
  { value: 'ai', label: 'AI 生成' },
  { value: 'product', label: '商品话术' },
  { value: 'shortvideo', label: '短视频' },
]
const SCRIPT_SCENES = ['成分党', '敏感肌', '活动', '新品', '日常']
const SEVERITY_COLORS: Record<number, 'error' | 'warning' | 'info'> = { 1: 'error', 2: 'warning', 3: 'info' }
const SEVERITY_LABELS: Record<number, string> = { 1: '高危', 2: '中危', 3: '低危' }
const WORKBENCH_READY_ENDPOINTS = [
  '/script/list',
  '/script/save',
  '/script/delete',
  '/script/search/semantic',
  '/script/violation/check',
  '/script/violation/check-batch',
  '/script/admin/violation/list',
  '/script/admin/violation/save',
  '/script/admin/violation/delete',
  '/script/template/search',
  '/script/template/save',
  '/script/template/delete',
  '/script/search/suggest',
  '/script/search/hybrid',
]
const WORKBENCH_UNSUPPORTED_ENDPOINTS = [
  '/script/get',
  '/script/use-count',
  '/script/categories',
  '/script/export',
  '/script/import',
  '/script/search/lexical',
  '/script/search/analytics',
  '/script/search/feedback',
  '/script/admin/template/list',
  '/script/admin/template/save',
  '/script/admin/template/delete',
]
const SCRIPT_LIBRARY_READY_ENDPOINTS = ['/script/list', '/script/save', '/script/delete']
const SCRIPT_LIBRARY_CONTEXT_ENDPOINTS = ['/script/search/semantic', '/script/violation/check', '/script/violation/check-batch']
const SCRIPT_LIBRARY_UNSUPPORTED_ACTIONS = [
  'server-export',
  'server-import',
  'inline-use-count',
  'inline-get-detail',
  'static-list-fallback',
]
const VIOLATION_TAB_READY_ENDPOINTS = ['/script/admin/violation/list', '/script/admin/violation/save', '/script/admin/violation/delete', '/script/violation/check']
const TEMPLATE_TAB_READY_ENDPOINTS = ['/script/template/search', '/script/template/save', '/script/template/delete']
const HYBRID_TAB_READY_ENDPOINTS = ['/script/search/suggest', '/script/search/hybrid']
const VIOLATION_TAB_UNSUPPORTED_ACTIONS = ['server-export', 'static-violation-fallback', 'public-list-mutation']
const TEMPLATE_TAB_UNSUPPORTED_ACTIONS = ['server-use-count', 'admin-template-endpoint', 'by-scene-shadow-list', 'static-template-fallback']
const HYBRID_TAB_UNSUPPORTED_ACTIONS = ['search-analytics', 'search-feedback', 'lexical-shadow-query', 'static-search-results']

function scriptPrimaryActionSx(extra: SystemStyleObject<Theme> = {}): SxProps<Theme> {
  return (theme) => ({
    bgcolor: theme.palette.primary.main,
    color: theme.palette.primary.contrastText,
    boxShadow: `0 6px 14px ${alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.28 : 0.18)}`,
    '&:hover': {
      bgcolor: theme.palette.primary.dark,
      boxShadow: `0 8px 18px ${alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.36 : 0.24)}`,
    },
    '&.Mui-disabled': {
      bgcolor: theme.palette.action.disabledBackground,
      color: theme.palette.action.disabled,
      boxShadow: 'none',
    },
    ...extra,
  })
}

interface ComplianceViolation { word: string; severity: number; suggestion: string }
interface ComplianceResult { score?: number; violations: (ViolationWord | ComplianceViolation)[] }

function violationLevel(v: Partial<ViolationWord> | ComplianceViolation) {
  return Number((v as ViolationWord).level ?? (v as ComplianceViolation).severity ?? 3)
}

function violationReason(v: Partial<ViolationWord>) {
  return v.reason ?? v.category ?? ''
}

function scriptScene(row: ScriptItem) {
  return row.industry ?? row.category ?? ''
}

function scriptType(row: ScriptItem) {
  return row.scriptType ?? row.source ?? ''
}

// orange highlight for {变量名}
function renderHighlighted(content: string) {
  const parts = content.split(/(\{[^}]+\})/g)
  return parts.map((p, i) =>
    p.startsWith('{') && p.endsWith('}')
      ? <Box key={i} component="span" sx={{ color: 'warning.main', fontWeight: 600 }}>{p}</Box>
      : <span key={i}>{p}</span>
  )
}
// yellow highlight for search keyword
function highlight(text: string, kw: string) {
  if (!kw) return <span>{text}</span>
  const parts = text.split(new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'))
  return <>{parts.map((p, i) => p.toLowerCase() === kw.toLowerCase()
    ? <Box key={i} component="span" sx={{ bgcolor: 'warning.light', px: 0.3, borderRadius: 0.5 }}>{p}</Box>
    : <span key={i}>{p}</span>
  )}</>
}
// ─────────────────────────────────────────────
// Tab 0: 话术库
// ─────────────────────────────────────────────
function ScriptLibraryTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, keyword: '', source: '', category: '' })
  const [query, setQuery] = useState(search)
  const [semanticMode, setSemanticMode] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ScriptSave>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [previewScript, setPreviewScript] = useState<ScriptItem | null>(null)
  const [previewTab, setPreviewTab] = useState(0)
  const [complianceResult, setComplianceResult] = useState<ComplianceResult | null>(null)
  const [complianceLoading, setComplianceLoading] = useState(false)
  const [previewContent, setPreviewContent] = useState('')
  const [selectedIds, setSelectedIds] = useState<GridRowSelectionModel>([])
  const [batchCheckOpen, setBatchCheckOpen] = useState(false)
  const [batchCheckRows, setBatchCheckRows] = useState<BatchCheckRow[]>([])
  const [batchCheckLoading, setBatchCheckLoading] = useState(false)
  const [complianceError, setComplianceError] = useState('')
  const [batchCheckError, setBatchCheckError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['scripts', search, semanticMode],
    queryFn: () => semanticMode && search.keyword
      ? scriptApi.searchSemantic({ query: search.keyword, page: search.page, rows: search.rows })
      : scriptApi.list(search)
  })
  const saveMut = useMutation({ mutationFn: scriptApi.save, onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['scripts'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const delMut = useMutation({ mutationFn: scriptApi.delete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['scripts'] }) }, onError: (e: Error) => toast(e.message, 'error') })

  const openEdit = useCallback((row: ScriptItem) => {
    setForm({
      id: row.id,
      title: row.title,
      content: row.content,
      category: scriptScene(row),
      tags: row.tags,
      source: scriptType(row),
      sourceId: row.sourceId,
      status: row.status,
    })
    setFormOpen(true)
  }, [])

  const handleRunCompliance = async (script: ScriptItem) => {
    setPreviewTab(1)
    setComplianceResult(null)
    setComplianceError('')
    setComplianceLoading(true)
    try {
      const res = await scriptApi.violationCheck(script.content ?? '')
      setComplianceResult(res as ComplianceResult)
    } catch (e: unknown) {
      const message = (e as Error).message
      setComplianceError(`话术合规检测失败：${message}。接口来源：/script/violation/check`)
      toast(message, 'error')
    } finally {
      setComplianceLoading(false)
    }
  }

  const handleBatchCheck = async () => {
    if (selectedIds.length === 0) { toast('请先勾选话术', 'warning'); return }
    setBatchCheckRows([])
    setBatchCheckError('')
    setBatchCheckLoading(true)
    setBatchCheckOpen(true)
    try {
      const rows = data?.list ?? []
      const res = await scriptApi.violationCheckBatch(selectedIds.map((id) => {
        const row = rows.find((item) => String(item.id) === String(id))
        return { key: String(id), text: row?.content ?? String(id) }
      }))
      setBatchCheckRows(res ?? [])
    } catch (e: unknown) {
      const message = (e as Error).message
      setBatchCheckError(`批量合规检测失败：${message}。接口来源：/script/violation/check-batch`)
      toast(message, 'error')
    } finally {
      setBatchCheckLoading(false)
    }
  }

  // open preview and auto-set content for compliance tab
  const openPreview = (row: ScriptItem) => {
    setPreviewScript(row)
    setPreviewContent(row.content ?? '')
    setPreviewTab(0)
    setComplianceResult(null)
    setComplianceError('')
  }

  const complianceScore = complianceResult?.score
  const violations = complianceResult?.violations ?? []
  const highCount = violations.filter(v => violationLevel(v) === 1).length
  const midCount = violations.filter(v => violationLevel(v) === 2).length
  const lowCount = violations.filter(v => violationLevel(v) === 3).length
  const scoreColor = complianceScore === undefined ? 'default' : complianceScore >= 90 ? 'success' : complianceScore >= 70 ? 'warning' : 'error'

  const columns: GridColDef[] = [
    { field: 'title', headerName: '话术标题', flex: 2, minWidth: 160,
      renderCell: ({ row, value }: GridRenderCellParams) => (
        <Box component="span" sx={{ cursor: 'pointer', color: 'primary.main', '&:hover': { textDecoration: 'underline' } }}
          onClick={() => openPreview(row as ScriptItem)}>{String(value ?? '')}</Box>
      )
    },
    { field: 'scriptType', headerName: '类型', width: 100,
      renderCell: ({ row, value }) => {
        const label = String(value ?? scriptType(row as ScriptItem))
        return label ? <Chip label={label} size="small" variant="outlined" /> : null
      } },
    { field: 'industry', headerName: '场景', width: 100,
      renderCell: ({ row, value }) => {
        const label = String(value ?? scriptScene(row as ScriptItem))
        return label ? <Chip label={label} size="small" variant="outlined" color="secondary" /> : null
      } },
    { field: 'useCount', headerName: '使用次数', width: 80, type: 'number' },
    { field: 'createTime', headerName: '创建时间', width: 110, valueFormatter: (v: string) => formatDate(v) },
    { field: 'actions', headerName: '操作', width: 200, sortable: false, renderCell: ({ row }: GridRenderCellParams) => (
      <Stack direction="row" gap={0.5} alignItems="center">
        <Tooltip title="预览"><IconButton size="small" onClick={() => openPreview(row as ScriptItem)}><VisibilityIcon fontSize="small" /></IconButton></Tooltip>
        <Tooltip title="编辑"><IconButton size="small" onClick={() => openEdit(row as ScriptItem)}><EditIcon fontSize="small" /></IconButton></Tooltip>
        <Tooltip title="合规检测"><IconButton size="small" onClick={() => { openPreview(row as ScriptItem); handleRunCompliance(row as ScriptItem) }}><CheckCircleOutlineIcon fontSize="small" /></IconButton></Tooltip>
        <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteId((row as ScriptItem).id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
      </Stack>
    ) },
  ]

  const searchSlot = (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, p: 1, alignItems: 'center', bgcolor: 'var(--color-surface-dark)' }}>
      <TextField size="small" placeholder="搜索话术标题/内容" value={query.keyword}
        onChange={e => setQuery(q => ({ ...q, keyword: e.target.value, page: 0 }))}
        onKeyDown={e => e.key === 'Enter' && setSearch(query)}
        InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
        sx={{ width: 220, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
      <FormControlLabel control={<Switch size="small" checked={semanticMode} onChange={e => setSemanticMode(e.target.checked)} />} label="语义搜索" />
      <TextField select size="small" label="来源" value={query.source} onChange={e => setQuery(q => ({ ...q, source: e.target.value, page: 0 }))} sx={{ width: 110, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
        <MenuItem value="">全部</MenuItem>
        {SCRIPT_SOURCES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
      </TextField>
      <TextField select size="small" label="分类" value={query.category} onChange={e => setQuery(q => ({ ...q, category: e.target.value, page: 0 }))} sx={{ width: 110, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
        <MenuItem value="">全部</MenuItem>
        {SCRIPT_SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
      </TextField>
      <Button size="small" variant="contained" data-testid="script-library-search-action" data-surface-tone="primary" onClick={() => setSearch(query)} sx={scriptPrimaryActionSx()}>搜索</Button>
      <Box sx={{ flex: 1 }} />
      {selectedIds.length > 0 && (
        <Button size="small" variant="outlined" color="warning" startIcon={<WarningAmberIcon />} onClick={handleBatchCheck}>
          批量检测 ({selectedIds.length})
        </Button>
      )}
      <Button size="small" variant="contained" data-testid="script-library-create-action" data-surface-tone="primary" startIcon={<AddIcon />} onClick={() => { setForm({}); setFormOpen(true) }} sx={scriptPrimaryActionSx()}>新增话术</Button>
    </Box>
  )

  return (
    <Box
      data-testid="script-library-tab"
      data-contract-scope="script-library"
      data-ready-endpoints={SCRIPT_LIBRARY_READY_ENDPOINTS.join('|')}
      data-context-endpoints={SCRIPT_LIBRARY_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SCRIPT_LIBRARY_UNSUPPORTED_ACTIONS.join('|')}
      data-active-endpoint={semanticMode && search.keyword ? '/script/search/semantic' : '/script/list'}
      data-keyword={search.keyword || ''}
      data-source={search.source || '全部'}
      data-category={search.category || '全部'}
      data-page={search.page}
      data-rows={search.rows}
      data-selected-count={selectedIds.length}
      data-result-count={data?.list?.length ?? 0}
      data-total={data?.total ?? 0}
      data-no-static-list-fallback="true"
      sx={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 2, bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)' }}
    >
      <Alert
        severity="info"
        data-testid="script-library-contract-alert"
        data-contract-source="/script/list"
        data-no-server-export-request="true"
        data-no-server-import-request="true"
        data-no-use-count-request="true"
        data-no-get-detail-request="true"
      >
        真实筛选字段：关键词匹配 title/content，分类对应 `/script/list.category`，来源对应 `/script/list.source`；后端没有独立 scriptType/industry 字段。
      </Alert>
      {isError && (
        <Alert
          severity="error"
          data-testid="script-library-list-error"
          data-contract-source={semanticMode && search.keyword ? '/script/search/semantic' : '/script/list'}
          data-no-static-list-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          话术列表加载失败：{(error as Error)?.message ?? '未知错误'}。接口来源：{semanticMode && search.keyword ? '/script/search/semantic' : '/script/list'}
        </Alert>
      )}
      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        rowCount={data?.total ?? 0}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        loading={isFetching}
        checkboxSelection
        rowSelectionModel={selectedIds}
        onRowSelectionModelChange={setSelectedIds}
        showExport={false}
        slotProps={undefined}
        slots={{ toolbar: () => searchSlot }}
        sx={{ flex: 1 }}
      />

      {/* Preview Drawer */}
      <Drawer
        anchor="right"
        open={!!previewScript}
        onClose={() => setPreviewScript(null)}
        PaperProps={{
          'data-testid': 'script-library-preview-drawer',
          'data-contract-source': 'local-selected-row',
          'data-script-id': previewScript?.id ?? '',
          sx: { width: 560, bgcolor: 'var(--color-surface)', borderLeft: '1px solid var(--color-surface-light)' },
        }}
      >
        {previewScript && (
          <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', p: 2, borderBottom: '1px solid var(--color-surface-light)' }}>
              <Typography variant="h6" sx={{ flex: 1, fontSize: 15, color: 'var(--color-text-primary)' }}>{previewScript.title}</Typography>
              <IconButton size="small" aria-label="关闭预览" onClick={() => setPreviewScript(null)}><CloseIcon /></IconButton>
            </Box>
            <Tabs value={previewTab} onChange={(_, v) => setPreviewTab(v)} sx={{ px: 2, borderBottom: '1px solid var(--color-surface-light)' }}>
              <Tab label="话术内容" />
              <Tab label="合规检测" />
            </Tabs>
            {previewTab === 0 && (
              <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
                <Stack direction="row" spacing={1} mb={1}>
                  {scriptType(previewScript) && <Chip label={scriptType(previewScript)} size="small" />}
                  {scriptScene(previewScript) && <Chip label={scriptScene(previewScript)} size="small" color="secondary" />}
                </Stack>
                <Paper variant="outlined" sx={{ p: 2, whiteSpace: 'pre-wrap', fontSize: 14, lineHeight: 1.8, bgcolor: 'var(--color-surface-dark)', color: 'var(--color-text-primary)', borderColor: 'var(--color-surface-light)' }}>
                  {renderHighlighted(previewContent)}
                </Paper>
                <Stack direction="row" spacing={1} mt={2}>
                  <Tooltip title="复制内容"><IconButton onClick={() => { navigator.clipboard.writeText(previewContent); toast('已复制', 'success') }}><ContentCopyIcon /></IconButton></Tooltip>
                  <Tooltip title="编辑"><IconButton onClick={() => { openEdit(previewScript); setPreviewScript(null) }}><EditIcon /></IconButton></Tooltip>
                </Stack>
              </Box>
            )}
            {previewTab === 1 && (
              <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
                {complianceLoading && <LinearProgress sx={{ mb: 2 }} />}
                {complianceError && (
                  <Alert
                    severity="error"
                    sx={{ mb: 2 }}
                    data-testid="script-library-compliance-error"
                    data-contract-source="/script/violation/check"
                    data-no-local-compliance-fallback="true"
                  >
                    {complianceError}
                  </Alert>
                )}
                {complianceResult && (
                  <Box
                    data-testid="script-library-compliance-result"
                    data-contract-source="/script/violation/check"
                    data-score={complianceScore ?? ''}
                    data-violation-count={violations.length}
                  >
                    <Stack direction="row" spacing={2} alignItems="center" mb={2}>
                      <Typography variant="body2" color="text.secondary">合规分数</Typography>
                      <Chip label={complianceScore !== undefined ? `${complianceScore}分` : '—'} color={scoreColor as 'success' | 'warning' | 'error' | 'default'} />
                      {highCount > 0 && <Chip label={`高危 ${highCount}`} color="error" size="small" />}
                      {midCount > 0 && <Chip label={`中危 ${midCount}`} color="warning" size="small" />}
                      {lowCount > 0 && <Chip label={`低危 ${lowCount}`} size="small" />}
                    </Stack>
                    <Divider sx={{ mb: 2, borderColor: 'var(--color-surface-light)' }} />
                    {violations.length === 0
                      ? <Typography color="success.main">✓ 未发现违规词</Typography>
                      : violations.map((v, i) => {
                          // Handle both ViolationWord and ComplianceViolation types
                          const word = 'word' in v ? v.word : ''
                          const severity = violationLevel(v)
                          const suggestion = 'suggestion' in v ? v.suggestion : ('replacement' in v ? v.replacement : '')
                          return (
                            <Paper key={i} variant="outlined" sx={{ p: 1.5, mb: 1, bgcolor: 'var(--color-surface-dark)', borderColor: SEVERITY_COLORS[severity] + '.main' }}>
                              <Stack direction="row" spacing={1} alignItems="center">
                                <Chip label={SEVERITY_LABELS[severity] ?? '未知'} color={SEVERITY_COLORS[severity]} size="small" />
                                <Typography variant="body2" fontWeight={600}>{word}</Typography>
                                {suggestion && <Typography variant="caption" color="text.secondary">建议替换为：{suggestion}</Typography>}
                              </Stack>
                            </Paper>
                          )
                        })
                    }
                  </Box>
                )}
                {!complianceResult && !complianceLoading && (
                  <Box textAlign="center" py={4}>
                    <Button
                      variant="contained"
                      data-contract-action="violationCheck"
                      data-ready-endpoint="/script/violation/check"
                      onClick={() => handleRunCompliance(previewScript)}
                    >
                      开始检测
                    </Button>
                  </Box>
                )}
                {complianceResult && (
                  <Box mt={2}>
                    <Button size="small" startIcon={<RefreshIcon />} onClick={() => handleRunCompliance(previewScript)}>重新检测</Button>
                  </Box>
                )}
              </Box>
            )}
          </Box>
        )}
      </Drawer>

      {/* Batch Check Dialog */}
      <Dialog
        open={batchCheckOpen}
        onClose={() => setBatchCheckOpen(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{
          'data-testid': 'script-library-batch-check-dialog',
          'data-contract-source': '/script/violation/check-batch',
          'data-selected-count': selectedIds.length,
          sx: { bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)' },
        }}
      >
        <DialogTitle sx={{ bgcolor: 'var(--color-surface-light)', color: 'var(--color-text-primary)' }}>批量合规检测结果</DialogTitle>
        <DialogContent sx={{ bgcolor: 'var(--color-surface)' }}>
          {batchCheckLoading && <LinearProgress />}
          {batchCheckError && (
            <Alert
              severity="error"
              sx={{ my: 1.5 }}
              data-testid="script-library-batch-check-error"
              data-contract-source="/script/violation/check-batch"
              data-no-local-compliance-fallback="true"
            >
              {batchCheckError}
            </Alert>
          )}
          {!batchCheckLoading && !batchCheckError && batchCheckRows.length === 0 && (
            <Alert severity="info" sx={{ my: 1.5 }}>暂无批量检测结果。</Alert>
          )}
          {batchCheckRows.map(row => (
            <Paper key={row.id} variant="outlined" sx={{ p: 1.5, mb: 1, bgcolor: 'var(--color-surface-dark)', borderColor: 'var(--color-surface-light)' }}>
              <Stack direction="row" spacing={2} alignItems="center">
                <Typography sx={{ flex: 1, color: 'var(--color-text-primary)' }}>{row.title}</Typography>
                <Chip label={`${row.count} 违规`} color={row.count === 0 ? 'success' : row.maxSeverity === 1 ? 'error' : 'warning'} size="small" />
                <Chip label={row.status} size="small" variant="outlined" />
              </Stack>
            </Paper>
          ))}
        </DialogContent>
        <DialogActions sx={{ bgcolor: 'var(--color-surface-light)' }}><Button onClick={() => setBatchCheckOpen(false)} sx={{ color: 'var(--color-text-primary)' }}>关闭</Button></DialogActions>
      </Dialog>

      {/* Add/Edit Dialog */}
      <FormDialog open={formOpen} onClose={() => setFormOpen(false)} title={form.id ? '编辑话术' : '新增话术'}
        onConfirm={() => saveMut.mutate(form as ScriptSave)} loading={saveMut.isPending}>
        <Stack spacing={2}>
          {saveMut.isError && (
            <Alert
              severity="error"
              data-testid="script-library-save-error"
              data-contract-source="/script/save"
              data-no-local-save-fallback="true"
            >
              话术保存失败：{(saveMut.error as Error)?.message ?? '未知错误'}。接口来源：/script/save
            </Alert>
          )}
          <TextField label="话术标题" value={form.title ?? ''} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} fullWidth required />
          <TextField label="内容" value={form.content ?? ''} onChange={e => setForm(f => ({ ...f, content: e.target.value }))} multiline minRows={6} fullWidth required />
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <TextField select label="来源" value={form.source ?? form.scriptType ?? ''} onChange={e => setForm(f => ({ ...f, source: e.target.value, scriptType: undefined }))} fullWidth>
                <MenuItem value="">请选择</MenuItem>
                {SCRIPT_SOURCES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField select label="分类" value={form.category ?? form.industry ?? ''} onChange={e => setForm(f => ({ ...f, category: e.target.value, industry: undefined }))} fullWidth>
                <MenuItem value="">请选择</MenuItem>
                {SCRIPT_SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </TextField>
            </Grid>
          </Grid>
          <TextField label="标签（逗号分隔）" value={form.tags ?? ''} onChange={e => setForm(f => ({ ...f, tags: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={deleteId !== null}
        onClose={() => { setDeleteId(null); delMut.reset() }}
        title="确认删除"
        content={delMut.isError
          ? `删除失败：${(delMut.error as Error)?.message ?? '未知错误'}。接口来源：/script/delete`
          : '此话术将被永久删除。'}
        onConfirm={() => delMut.mutate(deleteId!)}
        loading={delMut.isPending}
      />
    </Box>
  )
}

// ─────────────────────────────────────────────
// Tab 1: 违规词管理
// ─────────────────────────────────────────────
function ViolationWordTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, keyword: '', severity: '' as string | number })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ViolationWord>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [checkText, setCheckText] = useState('')
  const [checkResult, setCheckResult] = useState<{ violations: ViolationWord[] } | null>(null)
  const [checkLoading, setCheckLoading] = useState(false)
  const [checkError, setCheckError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['violation-words', search],
    queryFn: () => scriptApi.violationList({
      page: search.page,
      rows: search.rows,
      keyword: search.keyword,
      level: search.severity !== '' ? Number(search.severity) : undefined,
    })
  })
  const saveMut = useMutation({ mutationFn: scriptApi.violationSave, onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['violation-words'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const delMut = useMutation({ mutationFn: scriptApi.violationDelete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['violation-words'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const toggleMut = useMutation({
    mutationFn: ({ row, enabled }: { row: ViolationWord; enabled: boolean }) => scriptApi.violationSave({
      ...row,
      level: violationLevel(row),
      reason: violationReason(row),
      status: enabled ? 1 : 0,
    }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['violation-words'] }),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const handleCheck = async () => {
    if (!checkText.trim()) { toast('请输入待检测内容', 'warning'); return }
    setCheckLoading(true)
    setCheckResult(null)
    setCheckError('')
    try {
      const res = await scriptApi.violationCheck(checkText)
      setCheckResult(res)
    } catch (e: unknown) {
      const message = (e as Error).message
      setCheckError(`单条合规检测失败：${message}。接口来源：/script/violation/check`)
      toast(message, 'error')
    }
    finally { setCheckLoading(false) }
  }

  const columns: GridColDef[] = [
    { field: 'word', headerName: '违规词', flex: 1, minWidth: 120 },
    { field: 'severity', headerName: '危险等级', width: 100,
      renderCell: ({ row }) => {
        const level = violationLevel(row as ViolationWord)
        return <Chip label={SEVERITY_LABELS[level] ?? '未知'} color={SEVERITY_COLORS[level] ?? 'default'} size="small" />
      } },
    { field: 'replacement', headerName: '建议替换词', flex: 1, minWidth: 140 },
    { field: 'category', headerName: '分类', width: 100 },
    { field: 'status', headerName: '启用', width: 80,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Switch size="small" checked={(row as ViolationWord).status === 1}
          onChange={e => toggleMut.mutate({ row: row as ViolationWord, enabled: e.target.checked })} />
      ) },
    { field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: ({ row }: GridRenderCellParams) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="编辑"><IconButton size="small" onClick={() => { setForm(row as ViolationWord); setFormOpen(true) }}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteId((row as ViolationWord).id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ) },
  ]

  const searchSlot = (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, p: 1, alignItems: 'center', bgcolor: 'var(--color-surface-dark)' }}>
      <TextField size="small" placeholder="搜索违规词" value={search.keyword}
        onChange={e => setSearch(s => ({ ...s, keyword: e.target.value, page: 0 }))}
        InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
        sx={{ width: 180, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
      <TextField select size="small" label="危险等级" value={search.severity} onChange={e => setSearch(s => ({ ...s, severity: e.target.value, page: 0 }))} sx={{ width: 110, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
        <MenuItem value="">全部</MenuItem>
        <MenuItem value={1}>高危</MenuItem>
        <MenuItem value={2}>中危</MenuItem>
        <MenuItem value={3}>低危</MenuItem>
      </TextField>
      <Box sx={{ flex: 1 }} />
      <Button size="small" variant="contained" data-testid="script-violation-create-action" data-surface-tone="primary" startIcon={<AddIcon />} onClick={() => { setForm({}); setFormOpen(true) }} sx={scriptPrimaryActionSx()}>新增违规词</Button>
    </Box>
  )

  return (
    <Box
      data-testid="script-violation-tab"
      data-contract-scope="script-violation-words"
      data-ready-endpoints={VIOLATION_TAB_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={VIOLATION_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-keyword={search.keyword || ''}
      data-level={search.severity || '全部'}
      data-page={search.page}
      data-rows={search.rows}
      data-result-count={data?.list?.length ?? 0}
      data-total={data?.total ?? 0}
      data-no-server-export-request="true"
      data-no-static-violation-fallback="true"
      sx={{ display: 'flex', gap: 2, height: '100%' }}
    >
      <Box sx={{ flex: 2, display: 'flex', flexDirection: 'column' }}>
        {isError && (
          <Paper
            variant="outlined"
            data-testid="script-violation-list-error"
            data-contract-source="/script/admin/violation/list"
            data-no-static-violation-fallback="true"
            sx={{ p: 1.5, mb: 1, bgcolor: 'var(--color-surface)', borderColor: 'error.main' }}
          >
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="body2" color="error" sx={{ flex: 1 }}>
                违规词列表加载失败：{(error as Error)?.message ?? '未知错误'}。接口来源：/script/admin/violation/list
              </Typography>
              <Button size="small" onClick={() => refetch()}>重试</Button>
            </Stack>
          </Paper>
        )}
        <StandardDataGrid
          rows={data?.list ?? []}
          columns={columns}
          rowCount={data?.total ?? 0}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          loading={isFetching}
          showExport={false}
          slotProps={undefined}
          slots={{ toolbar: () => searchSlot }}
          sx={{ flex: 1 }}
        />
      </Box>

      {/* 单条检测面板 */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <Paper
          variant="outlined"
          data-testid="script-violation-check-panel"
          data-contract-source="/script/violation/check"
          data-input-length={checkText.length}
          data-result-count={checkResult?.violations.length ?? 0}
          data-no-local-compliance-fallback="true"
          sx={{ p: 2, bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}
        >
          <Typography variant="subtitle2" mb={1} sx={{ color: 'var(--color-text-primary)' }}>合规检测</Typography>
          <TextField multiline minRows={5} fullWidth placeholder="输入文本进行检测..." value={checkText}
            onChange={e => setCheckText(e.target.value)} sx={{ '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface-dark)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
          <Button sx={scriptPrimaryActionSx({ mt: 1 })} data-testid="script-violation-check-action" data-surface-tone="primary" variant="contained" fullWidth onClick={handleCheck} disabled={checkLoading}>
            {checkLoading ? '检测中...' : '开始检测'}
          </Button>
          {checkLoading && <LinearProgress sx={{ mt: 1 }} />}
          {checkError && (
            <Alert
              severity="error"
              sx={{ mt: 1.5 }}
              data-testid="script-violation-check-error"
              data-contract-source="/script/violation/check"
              data-no-local-compliance-fallback="true"
            >
              {checkError}
            </Alert>
          )}
          {checkResult && (
            <Box
              mt={2}
              data-testid="script-violation-check-result"
              data-contract-source="/script/violation/check"
              data-violation-count={checkResult.violations.length}
            >
              {checkResult.violations.length === 0
                ? <Typography sx={{ color: 'var(--color-success)', fontSize: 13 }}>✓ 未发现违规词</Typography>
                : checkResult.violations.map((v, i) => (
                    <Paper key={i} variant="outlined" sx={{ p: 1, mb: 1, bgcolor: 'var(--color-surface-dark)', borderColor: 'var(--color-surface-light)' }}>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                        <Chip label={SEVERITY_LABELS[violationLevel(v)] ?? '未知'} color={SEVERITY_COLORS[violationLevel(v)] ?? 'default'} size="small" />
                        <Typography variant="body2" fontWeight={600} sx={{ color: 'var(--color-text-primary)' }}>{v.word}</Typography>
                        {v.replacement && <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>→ {v.replacement}</Typography>}
                      </Stack>
                    </Paper>
                  ))
              }
            </Box>
          )}
        </Paper>
      </Box>

      {/* Form Dialog */}
      <FormDialog open={formOpen} onClose={() => setFormOpen(false)} title={form.id ? '编辑违规词' : '新增违规词'}
        onConfirm={() => saveMut.mutate(form as ViolationWord)} loading={saveMut.isPending}>
        <Stack spacing={2}>
          <TextField label="违规词" value={form.word ?? ''} onChange={e => setForm(f => ({ ...f, word: e.target.value }))} fullWidth required />
          <TextField select label="危险等级" value={form.level ?? form.severity ?? 1} onChange={e => setForm(f => ({ ...f, level: Number(e.target.value), severity: Number(e.target.value) }))} fullWidth>
            <MenuItem value={1}>高危</MenuItem>
            <MenuItem value={2}>中危</MenuItem>
            <MenuItem value={3}>低危</MenuItem>
          </TextField>
          <TextField label="建议替换词" value={form.replacement ?? ''} onChange={e => setForm(f => ({ ...f, replacement: e.target.value }))} fullWidth />
          <TextField label="分类/原因" value={violationReason(form)} onChange={e => setForm(f => ({ ...f, reason: e.target.value, category: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} onClose={() => setDeleteId(null)} title="确认删除" content="此违规词将被永久删除。"
        onConfirm={() => delMut.mutate(deleteId!)} loading={delMut.isPending} />
    </Box>
  )
}

// ─────────────────────────────────────────────
// Tab 2: 模板库
// ─────────────────────────────────────────────
function ScriptTemplateTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 24, keyword: '', scene: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ScriptTemplate>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [useDialog, setUseDialog] = useState<ScriptTemplate | null>(null)
  const [variables, setVariables] = useState<Record<string, string>>({})

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['script-templates', search],
    queryFn: () => scriptApi.templateSearch({ ...search })
  })
  const saveMut = useMutation({ mutationFn: scriptApi.templateSave, onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['script-templates'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const delMut = useMutation({ mutationFn: scriptApi.templateDelete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['script-templates'] }) }, onError: (e: Error) => toast(e.message, 'error') })

  // Extract {变量名} from template content
  const extractVars = (content: string) => {
    const matches = content.match(/\{([^}]+)\}/g) ?? []
    return [...new Set(matches.map(m => m.slice(1, -1)))]
  }

  const openUseDialog = (tpl: ScriptTemplate) => {
    const vars = extractVars(tpl.tags ?? tpl.templateContent ?? '')
    const initial: Record<string, string> = {}
    vars.forEach(v => { initial[v] = '' })
    setVariables(initial)
    setUseDialog(tpl)
  }

  const applyTemplate = () => {
    if (!useDialog) return
    let result = useDialog.templateContent ?? ''
    Object.entries(variables).forEach(([k, v]) => {
      result = result.replace(new RegExp(`\\{${k}\\}`, 'g'), v || `{${k}}`)
    })
    navigator.clipboard.writeText(result)
    toast('已填充并复制到剪贴板', 'success')
    setUseDialog(null)
  }

  return (
    <Box
      data-testid="script-template-tab"
      data-contract-scope="script-template-library"
      data-ready-endpoints={TEMPLATE_TAB_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={TEMPLATE_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-keyword={search.keyword || ''}
      data-scene={search.scene || '全部'}
      data-page={search.page}
      data-rows={search.rows}
      data-result-count={data?.list?.length ?? 0}
      data-total={data?.total ?? 0}
      data-no-static-template-fallback="true"
      data-no-admin-template-endpoint="true"
      data-no-use-count-request="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2, bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)' }}
    >
      {/* Search bar */}
      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField size="small" placeholder="搜索模板名称" value={search.keyword}
          onChange={e => setSearch(s => ({ ...s, keyword: e.target.value, page: 0 }))}
          onKeyDown={e => e.key === 'Enter' && setSearch(s => ({ ...s }))}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
          sx={{ width: 200, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }} />
        <TextField select size="small" label="场景" value={search.scene} onChange={e => setSearch(s => ({ ...s, scene: e.target.value, page: 0 }))} sx={{ width: 120, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
          <MenuItem value="">全部</MenuItem>
          {SCRIPT_SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
        </TextField>
        <Box sx={{ flex: 1 }} />
        <Button size="small" variant="contained" data-testid="script-template-create-action" data-surface-tone="primary" startIcon={<AddIcon />} onClick={() => { setForm({}); setFormOpen(true) }} sx={scriptPrimaryActionSx()}>新增模板</Button>
      </Box>

      {isFetching && <LinearProgress />}
      {isError && (
        <Alert
          severity="error"
          data-testid="script-template-list-error"
          data-contract-source="/script/template/search"
          data-no-static-template-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          模板列表加载失败：{(error as Error)?.message ?? '未知错误'}。接口来源：/script/template/search
        </Alert>
      )}

      {/* Card grid */}
      <Grid container spacing={2}>
        {(data?.list ?? []).map(tpl => (
          <Grid item xs={12} sm={6} md={4} key={tpl.id}>
            <Card variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              <CardContent
                data-testid="script-template-card"
                data-contract-source="/script/template/search"
                data-template-id={tpl.id}
                data-scene={tpl.scene ?? ''}
                sx={{ flex: 1 }}
              >
                <Typography variant="subtitle2" mb={0.5} noWrap sx={{ color: 'var(--color-text-primary)' }}>{tpl.templateName}</Typography>
                {tpl.scene && <Chip label={tpl.scene} size="small" sx={{ mb: 1 }} />}
                <Typography variant="body2" sx={{
                  display: '-webkit-box', WebkitLineClamp: 4, WebkitBoxOrient: 'vertical', overflow: 'hidden',
                  fontSize: 12, lineHeight: 1.6, color: 'var(--color-text-secondary)'
                }}>
                  {renderHighlighted(tpl.templateContent ?? '')}
                </Typography>
                {tpl.tags && (
                  <Box mt={1}>
                    {extractVars(tpl.tags).map(v => (
                      <Chip key={v} label={`{${v}}`} size="small" sx={{ mr: 0.5, mb: 0.5, color: 'warning.main', borderColor: 'warning.main' }} variant="outlined" />
                    ))}
                  </Box>
                )}
              </CardContent>
              <CardActions sx={{ pt: 0, justifyContent: 'flex-end' }}>
                <Tooltip title="使用模板"><IconButton size="small" color="primary" onClick={() => openUseDialog(tpl)}><ContentCopyIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title="编辑"><IconButton size="small" onClick={() => { setForm(tpl); setFormOpen(true) }}><EditIcon fontSize="small" /></IconButton></Tooltip>
                <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteId(tpl.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Variable-fill dialog */}
      <Dialog
        open={!!useDialog}
        onClose={() => setUseDialog(null)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'script-template-use-dialog',
          'data-contract-source': 'local-template-variable-fill',
          'data-template-id': useDialog?.id ?? '',
          'data-no-use-count-request': 'true',
          sx: { bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)' },
        }}
      >
        <DialogTitle sx={{ bgcolor: 'var(--color-surface-light)', color: 'var(--color-text-primary)' }}>使用模板：{useDialog?.templateName}</DialogTitle>
        <DialogContent sx={{ bgcolor: 'var(--color-surface)' }}>
          <Typography variant="body2" sx={{ color: 'var(--color-text-secondary)', mb: 2 }}>填写变量后，内容将复制到剪贴板</Typography>
          {useDialog && extractVars(useDialog.tags ?? useDialog.templateContent ?? '').map(v => (
            <TextField key={v} label={`{${v}}`} fullWidth size="small" sx={{ mb: 1.5, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface-dark)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}
              value={variables[v] ?? ''}
              onChange={e => setVariables(prev => ({ ...prev, [v]: e.target.value }))} />
          ))}
          {useDialog && (
            <Paper variant="outlined" sx={{ p: 1.5, mt: 1, whiteSpace: 'pre-wrap', fontSize: 13, maxHeight: 200, overflow: 'auto', bgcolor: 'var(--color-surface-dark)', borderColor: 'var(--color-surface-light)', color: 'var(--color-text-primary)' }}>
              {renderHighlighted(useDialog.templateContent ?? '')}
            </Paper>
          )}
        </DialogContent>
        <DialogActions sx={{ bgcolor: 'var(--color-surface-light)' }}>
          <Button onClick={() => setUseDialog(null)} sx={{ color: 'var(--color-text-primary)' }}>取消</Button>
          <Button variant="contained" data-testid="script-template-apply-action" data-surface-tone="primary" onClick={applyTemplate} sx={scriptPrimaryActionSx()}>填充并复制</Button>
        </DialogActions>
      </Dialog>

      {/* Add/Edit Dialog */}
      <FormDialog open={formOpen} onClose={() => setFormOpen(false)} title={form.id ? '编辑模板' : '新增模板'}
        onConfirm={() => saveMut.mutate(form as ScriptTemplate)} loading={saveMut.isPending}>
        <Stack spacing={2}>
          <TextField label="模板名称" value={form.templateName ?? ''} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} fullWidth required />
          <TextField label="场景" value={form.scene ?? ''} onChange={e => setForm(f => ({ ...f, scene: e.target.value }))} fullWidth />
          <TextField label="模板内容（使用 {变量名} 标记变量）" value={form.templateContent ?? ''}
            onChange={e => setForm(f => ({ ...f, templateContent: e.target.value }))} multiline minRows={6} fullWidth required />
          <TextField label="标签/变量说明" value={form.tags ?? ''} onChange={e => setForm(f => ({ ...f, tags: e.target.value }))} fullWidth
            helperText="如：产品名,核心卖点,优惠信息" />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} onClose={() => setDeleteId(null)} title="确认删除" content="此模板将被永久删除。"
        onConfirm={() => delMut.mutate(deleteId!)} loading={delMut.isPending} />
    </Box>
  )
}

// ─────────────────────────────────────────────
// Tab 3: 混合搜索
// ─────────────────────────────────────────────
function HybridSearchTab() {
  const [inputValue, setInputValue] = useState('')
  const [query, setQuery] = useState('')
  const [mode, setMode] = useState<'hybrid' | 'semantic' | 'keyword'>('hybrid')
  const [sources, setSources] = useState<string[]>(['script', 'knowledge', 'product'])
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [suggestOpen, setSuggestOpen] = useState(false)
  const suggestTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['hybrid-search', query, mode, sources],
    queryFn: () => query ? scriptApi.searchHybrid({ query, mode, sources, rows: 20 }) : Promise.resolve({ list: [], total: 0, pageNum: 1, pageSize: 20 } as import('@/types/common').PageResult<ScriptItem>),
    enabled: !!query
  })

  const handleInput = (val: string) => {
    setInputValue(val)
    if (suggestTimer.current) clearTimeout(suggestTimer.current)
    if (val.trim().length < 2) { setSuggestions([]); setSuggestOpen(false); return }
    suggestTimer.current = setTimeout(async () => {
      try {
        const res = await scriptApi.searchSuggest(val)
        setSuggestions(res ?? [])
        setSuggestOpen((res ?? []).length > 0)
      } catch (_e) { /* ignore */ }
    }, 300)
  }

  const doSearch = (kw?: string) => {
    const q = kw ?? inputValue
    setInputValue(q)
    setQuery(q)
    setSuggestOpen(false)
  }

  const toggleSource = (src: string) => {
    setSources(prev => prev.includes(src) ? prev.filter(s => s !== src) : [...prev, src])
  }

  type SearchResult = { id: number; title: string; content?: string; score?: number; source?: string; module?: string }
  const results: SearchResult[] = ((data as { list?: SearchResult[] })?.list ?? []) as SearchResult[]

  const sourceLabels: Record<string, string> = { script: '话术库', knowledge: '知识库', product: '商品库', copy: '文案库' }
  const moduleColor: Record<string, 'primary' | 'secondary' | 'success' | 'warning'> = {
    script: 'primary', knowledge: 'secondary', product: 'success', copy: 'warning'
  }

  return (
    <Box
      data-testid="script-list-hybrid-tab"
      data-contract-scope="script-hybrid-search"
      data-ready-endpoints={HYBRID_TAB_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={HYBRID_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-mode={mode}
      data-sources={sources.join('|')}
      data-query={query}
      data-input={inputValue}
      data-result-count={results.length}
      data-total={(data as { total?: number } | undefined)?.total ?? 0}
      data-no-static-search-results="true"
      sx={{ display: 'flex', gap: 2, bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)', borderRadius: 'var(--border-radius-xl)' }}
    >
      {/* Left: search panel */}
      <Box sx={{ width: 260, flexShrink: 0 }}>
        <Paper variant="outlined" sx={{ p: 2, bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
          <Typography variant="subtitle2" mb={2} sx={{ color: 'var(--color-text-primary)' }}>搜索设置</Typography>

          {/* Search mode */}
          <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>搜索模式</Typography>
          <Stack spacing={0.5} mb={2}>
            {(['hybrid', 'semantic', 'keyword'] as const).map(m => (
              <Box
                key={m}
                data-testid={`script-hybrid-mode-${m}`}
                data-contract-mode={m}
                sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer', py: 0.5 }}
                onClick={() => setMode(m)}>
                <Box sx={{ width: 16, height: 16, borderRadius: '50%', border: '2px solid',
                  borderColor: mode === m ? 'var(--color-primary)' : 'var(--color-surface-light)',
                  bgcolor: mode === m ? 'var(--color-primary)' : 'transparent', mr: 1 }} />
                <Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}>
                  {m === 'hybrid' ? '混合搜索' : m === 'semantic' ? '语义搜索' : '关键词搜索'}
                </Typography>
              </Box>
            ))}
          </Stack>

          {/* Sources */}
          <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>搜索范围</Typography>
          <Stack spacing={0.5}>
            {Object.entries(sourceLabels).map(([src, label]) => (
              <FormControlLabel key={src}
                data-testid={`script-hybrid-source-${src}`}
                data-contract-source-toggle={src}
                control={<Switch size="small" checked={sources.includes(src)} onChange={() => toggleSource(src)} />}
                label={<Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}>{label}</Typography>} />
            ))}
          </Stack>
        </Paper>
      </Box>

      {/* Right: search + results */}
      <Box sx={{ flex: 1 }}>
        {/* Search input with suggestions */}
        <Box sx={{ position: 'relative', mb: 2 }}>
          <TextField fullWidth size="small" placeholder="输入关键词搜索..."
            value={inputValue}
            onChange={e => handleInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && doSearch()}
            onBlur={() => setTimeout(() => setSuggestOpen(false), 150)}
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}
            InputProps={{
              startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment>,
              endAdornment: <InputAdornment position="end"><Button size="small" variant="contained" data-testid="script-hybrid-search-action" data-surface-tone="primary" onClick={() => doSearch()} sx={scriptPrimaryActionSx({ mr: -1 })}>搜索</Button></InputAdornment>
            }} />
          {suggestOpen && suggestions.length > 0 && (
            <Paper variant="outlined" sx={{ position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10, maxHeight: 200, overflow: 'auto', bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}>
              {suggestions.map((s, i) => (
                <Box key={i} sx={{ px: 2, py: 1, cursor: 'pointer', '&:hover': { bgcolor: 'var(--color-surface-light)' }, color: 'var(--color-text-primary)' }}
                  onMouseDown={() => doSearch(s)}>
                  <Typography variant="body2" sx={{ color: 'var(--color-text-primary)' }}>{highlight(s, inputValue)}</Typography>
                </Box>
              ))}
            </Paper>
          )}
        </Box>

        {isFetching && <LinearProgress sx={{ mb: 1 }} />}
        {isError && (
          <Alert
            severity="error"
            sx={{ mb: 1.5 }}
            data-testid="script-hybrid-search-error"
            data-contract-source="/script/search/hybrid"
            data-no-static-search-results="true"
            action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          >
            混合搜索失败：{(error as Error)?.message ?? '未知错误'}。接口来源：/script/search/hybrid
          </Alert>
        )}

        {/* Results */}
        {query && !isFetching && results.length === 0 && (
          <Typography
            data-testid="script-hybrid-empty"
            data-contract-source="/script/search/hybrid"
            data-no-static-search-results="true"
            sx={{ color: 'var(--color-text-secondary)', textAlign: 'center', py: 4 }}
          >
            未找到相关结果
          </Typography>
        )}
        <Stack spacing={1.5}>
          {results.map((item, idx) => {
            const mod = item.source ?? item.module ?? 'script'
            const score = typeof item.score === 'number' ? Math.round(item.score * 100) : null
            return (
              <Paper
                key={item.id ?? idx}
                variant="outlined"
                data-testid="script-hybrid-result-card"
                data-contract-source="/script/search/hybrid"
                data-result-source={mod}
                data-result-score={score ?? ''}
                sx={{ p: 2, bgcolor: 'var(--color-surface)', borderColor: 'var(--color-surface-light)' }}
              >
                <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                  <Chip label={sourceLabels[mod] ?? mod} color={moduleColor[mod] ?? 'primary'} size="small" />
                  <Typography variant="subtitle2" sx={{ flex: 1, color: 'var(--color-text-primary)' }}>{highlight(item.title ?? '', query)}</Typography>
                  {score !== null && (
                    <Box sx={{ width: 80 }}>
                      <Typography variant="caption" sx={{ color: 'var(--color-text-secondary)' }}>相关度 {score}%</Typography>
                      <LinearProgress variant="determinate" value={score} sx={{ height: 4, borderRadius: 2 }} />
                    </Box>
                  )}
                </Stack>
                {item.content && (
                  <Typography variant="body2" sx={{
                    display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden', fontSize: 12, color: 'var(--color-text-secondary)'
                  }}>
                    {highlight(item.content.slice(0, 200), query)}
                  </Typography>
                )}
              </Paper>
            )
          })}
        </Stack>
      </Box>
    </Box>
  )
}

// ─────────────────────────────────────────────
// Main Page
// ─────────────────────────────────────────────
export default function ScriptListPage() {
  const [tab, setTab] = useState(0)
  const moduleHints = [
    { label: '话术库', value: 'CRUD / 语义检索 / 合规预览' },
    { label: '违规词', value: 'Admin 词库 / 单条检测 / 批量检测' },
    { label: '模板库', value: '变量模板 / 填充复制' },
    { label: '混合搜索', value: '向量 + BM25，依赖嵌入服务' },
  ]

  return (
    <Box
      data-testid="script-list-workbench"
      data-contract-scope="script-workbench-composite"
      data-ready-endpoints={WORKBENCH_READY_ENDPOINTS.join('|')}
      data-unsupported-endpoints={WORKBENCH_UNSUPPORTED_ENDPOINTS.join('|')}
      data-active-tab={tab}
      sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: 'var(--color-surface-dark)' }}
    >
      <Box sx={{ px: 'var(--spacing-lg)', pt: 'var(--spacing-lg)' }}>
        <PageHeader
          title="话术工作台"
          breadcrumbs={[{ label: '话术管理' }, { label: '话术脚本' }]}
          subtitle="统一管理话术库、违规词、模板与混合搜索。真实接口以 `/script/*`、`/script/template/*`、`/script/admin/violation/*`、`/script/search/*` 为准。"
        />
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
          {moduleHints.map((item) => (
            <Chip key={item.label} label={`${item.label}：${item.value}`} size="small" variant="outlined" />
          ))}
        </Stack>
      </Box>
      <Box sx={{ borderBottom: '1px solid var(--color-surface-light)', mb: 2, bgcolor: 'var(--color-surface)' }}>
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          data-testid="script-list-tabs"
          data-contract-surface="script-workbench-tabs"
          sx={{ px: 'var(--spacing-lg)', pt: 'var(--spacing-md)' }}
        >
          <Tab label="话术库" data-testid="script-list-tab-library" data-contract-surface="script-library" />
          <Tab label="违规词管理" data-testid="script-list-tab-violation" data-contract-surface="script-violation-words" />
          <Tab label="模板库" data-testid="script-list-tab-template" data-contract-surface="script-template-library" />
          <Tab label="混合搜索" data-testid="script-list-tab-hybrid" data-contract-surface="script-hybrid-search" />
        </Tabs>
      </Box>
      <Box sx={{ flex: 1, overflow: 'auto', p: 'var(--spacing-lg)' }}>
        {tab === 0 && <ScriptLibraryTab />}
        {tab === 1 && <ViolationWordTab />}
        {tab === 2 && <ScriptTemplateTab />}
        {tab === 3 && <HybridSearchTab />}
      </Box>
    </Box>
  )
}
