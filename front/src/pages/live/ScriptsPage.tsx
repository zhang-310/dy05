import { useState, useEffect, useCallback } from 'react'
import {
  Alert, Box, Button, TextField, Select, MenuItem, FormControl, InputLabel,
  Chip, IconButton, Tooltip, Dialog, DialogTitle, DialogContent,
  DialogActions, Stack, SelectChangeEvent, Typography, Grid, Paper,
} from '@mui/material'
import { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import PlayCircleIcon from '@mui/icons-material/PlayCircle'
import { liveApi, LiveScript, LiveScriptSave } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { ConfirmDialog, EmptyState, PageHeader, StandardDataGrid } from '@/components/base'
import { formatDate } from '@/utils/date'

const SCRIPT_TYPE_OPTIONS = ['开场白', '产品介绍', '促单', '互动', '结尾']

const SCRIPT_TYPE_COLOR: Record<string, 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'> = {
  '开场白': 'primary',
  '产品介绍': 'info',
  '促单': 'error',
  '互动': 'success',
  '结尾': 'default',
}

const LIVE_SCRIPT_READY_ENDPOINTS = {
  search: '/live/script/search',
  save: '/live/script/save',
  delete: '/live/script/delete',
  executed: '/live/script/executed',
} as const

const LIVE_SCRIPT_CONTEXT_ENDPOINTS = [
  LIVE_SCRIPT_READY_ENDPOINTS.search,
  LIVE_SCRIPT_READY_ENDPOINTS.save,
  LIVE_SCRIPT_READY_ENDPOINTS.delete,
  LIVE_SCRIPT_READY_ENDPOINTS.executed,
]

const LIVE_SCRIPT_UNSUPPORTED_ACTIONS = [
  'single-segment-ai-generate',
  'sse-generate-full',
  'product-selection',
  'shortvideo-export',
  'script-approval',
  'template-library',
]

interface FormState {
  id?: number
  sessionId: string
  scriptTitle: string
  scriptContent: string
  scriptType: string
  sortOrder: string
  duration: string
}

const defaultForm = (): FormState => ({
  sessionId: '', scriptTitle: '', scriptContent: '',
  scriptType: '产品介绍', sortOrder: '0', duration: '0',
})

interface QueryState {
  sessionId: string
  scriptType: string
  keyword: string
  executed: '' | '0' | '1'
}

const emptyQuery = (): QueryState => ({ sessionId: '', scriptType: '', keyword: '', executed: '' })

const messageOf = (error: unknown, fallback: string) => error instanceof Error ? error.message : fallback

export default function ScriptsPage() {
  const toast = useToast()
  const [rows, setRows] = useState<LiveScript[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [draftQuery, setDraftQuery] = useState<QueryState>(emptyQuery)
  const [query, setQuery] = useState<QueryState>(emptyQuery)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<FormState>(defaultForm())
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [executeError, setExecuteError] = useState('')
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [executingId, setExecutingId] = useState<number | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      const res = await liveApi.scriptSearch({
        page, rows: pageSize,
        sessionId: query.sessionId ? Number(query.sessionId) : undefined,
        scriptType: query.scriptType || undefined,
        keyword: query.keyword || undefined,
        executed: query.executed === '' ? undefined : Number(query.executed),
      })
      setRows(res.list ?? [])
      setTotal(res.total ?? 0)
    } catch (e) {
      setLoadError(`${LIVE_SCRIPT_READY_ENDPOINTS.search}：${messageOf(e, '请检查后端直播话术搜索接口')}`)
      toast('加载失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, query, toast])

  useEffect(() => { load() }, [load])

  const handleSearch = () => {
    setPage(0)
    setQuery(draftQuery)
  }
  const handleReset = () => {
    const next = emptyQuery()
    setDraftQuery(next)
    setQuery(next)
    setPage(0)
  }

  const openCreate = () => { setForm(defaultForm()); setSaveError(''); setDialogOpen(true) }
  const openEdit = (row: LiveScript) => {
    setSaveError('')
    setForm({
      id: row.id,
      sessionId: String(row.sessionId),
      scriptTitle: String(row.scriptTitle ?? row.requirement ?? row.scriptType ?? '话术'),
      scriptContent: String(row.scriptContent ?? ''),
      scriptType: row.scriptType || '产品介绍',
      sortOrder: String(typeof row.sequenceNo === 'number' ? row.sequenceNo : row.sortOrder ?? 0),
      duration: String(typeof row.durationLimitSec === 'number' ? row.durationLimitSec : row.duration ?? 0),
    })
    setDialogOpen(true)
  }

  const handleDelete = async () => {
    if (deleteId == null) return
    setDeleteError('')
    try { await liveApi.scriptDelete(deleteId); toast('删除成功', 'success'); load() }
    catch (e) { setDeleteError(`${LIVE_SCRIPT_READY_ENDPOINTS.delete}：${messageOf(e, '删除失败')}`); toast('删除失败', 'error') }
    finally { setDeleteId(null) }
  }

  const handleSave = async () => {
    setSaveError('')
    if (!form.scriptTitle.trim()) { toast('请填写槽位标题/需求', 'warning'); return }
    if (!form.scriptContent.trim()) { toast('请填写话术内容', 'warning'); return }
    if (!form.sessionId) { toast('请填写场次ID', 'warning'); return }
    setSaving(true)
    try {
      const payload: Partial<LiveScriptSave> = {
        ...(form.id ? { id: form.id } : {}),
        sessionId: Number(form.sessionId),
        scriptTitle: form.scriptTitle,
        scriptContent: form.scriptContent,
        scriptType: form.scriptType,
        requirement: form.scriptTitle,
        sequenceNo: Number(form.sortOrder) || 0,
        durationLimitSec: Number(form.duration) || 0,
      }
      await liveApi.scriptSave(payload)
      toast(form.id ? '更新成功' : '创建成功', 'success')
      setDialogOpen(false); load()
    } catch (e) {
      setSaveError(`${LIVE_SCRIPT_READY_ENDPOINTS.save}：${messageOf(e, '保存失败')}`)
      toast('保存失败', 'error')
    } finally { setSaving(false) }
  }

  const handleToggleExecuted = async (row: LiveScript) => {
    const id = Number(row.id)
    const next = Number(row.executed ?? 0) === 1 ? 0 : 1
    setExecuteError('')
    setExecutingId(id)
    try {
      await liveApi.scriptUpdateExecuted({ id, executed: next })
      toast(next === 1 ? '已标记为已执行' : '已取消执行', 'success')
      await load()
    } catch (e) {
      setExecuteError(`${LIVE_SCRIPT_READY_ENDPOINTS.executed}：${messageOf(e, '执行状态更新失败')}`)
      toast('执行状态更新失败', 'error')
    } finally {
      setExecutingId(null)
    }
  }

  const columns: GridColDef[] = [
    { field: 'scriptTitle', headerName: '槽位/需求', flex: 1.3, minWidth: 150 },
    { field: 'scriptContent', headerName: '话术内容', flex: 2.2, minWidth: 260 },
    {
      field: 'scriptType', headerName: '类型', width: 110,
      renderCell: (p: GridRenderCellParams) => {
        const t = p.value as string
        return <Chip label={t || '-'} color={SCRIPT_TYPE_COLOR[t] ?? 'default'} size="small" />
      },
    },
    { field: 'sessionId', headerName: '场次ID', width: 90, type: 'number' },
    { field: 'sortOrder', headerName: '排序', width: 80, type: 'number' },
    { field: 'duration', headerName: '时长(s)', width: 90, type: 'number' },
    {
      field: 'executed', headerName: '执行状态', width: 100,
      renderCell: (p: GridRenderCellParams) =>
        <Chip label={Number(p.value ?? 0) === 1 ? '已执行' : '未执行'} color={Number(p.value ?? 0) === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 150, sortable: false,
      renderCell: (p: GridRenderCellParams<LiveScript>) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title={Number(p.row.executed ?? 0) === 1 ? '取消执行' : '标记已执行'}>
            <span>
              <IconButton
                size="small"
                aria-label={Number(p.row.executed ?? 0) === 1 ? '取消执行' : '标记已执行'}
                onClick={() => handleToggleExecuted(p.row)}
                disabled={executingId === Number(p.row.id)}
                color={Number(p.row.executed ?? 0) === 1 ? 'success' : 'default'}
              >
                {Number(p.row.executed ?? 0) === 1 ? <CheckCircleIcon fontSize="small" /> : <PlayCircleIcon fontSize="small" />}
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="编辑"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => setDeleteId(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField size="small" label="场次ID" value={draftQuery.sessionId}
        onChange={e => setDraftQuery(q => ({ ...q, sessionId: e.target.value }))} sx={{ width: 110 }}
        inputProps={{ inputMode: 'numeric' }} />
      <FormControl size="small" sx={{ width: 130 }}>
        <InputLabel id="scripts-filter-type-label">话术类型</InputLabel>
        <Select id="scripts-filter-type" labelId="scripts-filter-type-label" value={draftQuery.scriptType} label="话术类型"
          onChange={(e: SelectChangeEvent) => setDraftQuery(q => ({ ...q, scriptType: e.target.value }))}>
          <MenuItem value="">全部</MenuItem>
          {SCRIPT_TYPE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
        </Select>
      </FormControl>
      <FormControl size="small" sx={{ width: 120 }}>
        <InputLabel id="scripts-filter-executed-label">执行状态</InputLabel>
        <Select id="scripts-filter-executed" labelId="scripts-filter-executed-label" value={draftQuery.executed} label="执行状态"
          onChange={(e: SelectChangeEvent) => setDraftQuery(q => ({ ...q, executed: e.target.value as QueryState['executed'] }))}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="1">已执行</MenuItem>
          <MenuItem value="0">未执行</MenuItem>
        </Select>
      </FormControl>
      <TextField size="small" label="关键词" value={draftQuery.keyword}
        onChange={e => setDraftQuery(q => ({ ...q, keyword: e.target.value }))} sx={{ width: 160 }} />
      <Button variant="contained" size="small" onClick={handleSearch}>查询</Button>
      <Button variant="outlined" size="small" onClick={handleReset}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} size="small" onClick={openCreate}>新建话术</Button>
  )
  const generatedCount = rows.filter(r => Boolean(r.aiGenerated)).length
  const checkedCount = rows.filter(r => Boolean(r.violationChecked)).length
  const executedCount = rows.filter(r => Number(r.executed ?? 0) > 0).length

  return (
    <Box
      sx={{ p: 3, minHeight: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}
      data-testid="live-scripts-workbench"
      data-contract-scope="live-script-management"
      data-ready-endpoints={Object.values(LIVE_SCRIPT_READY_ENDPOINTS).join('|')}
      data-context-endpoints={LIVE_SCRIPT_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={LIVE_SCRIPT_UNSUPPORTED_ACTIONS.join('|')}
      data-query-session-id={query.sessionId}
      data-query-script-type={query.scriptType}
      data-query-executed={query.executed}
      data-row-count={rows.length}
      data-total={total}
    >
      <PageHeader
        title="直播话术管理"
        subtitle="按场次维护已落库直播话术；列表、保存、删除和执行状态均走真实 live/script 接口。"
        breadcrumbs={[{ label: '直播' }, { label: '话术' }]}
        actions={actionSlot}
      />
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="live-scripts-contract-alert"
        data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.search}
        data-save-source={LIVE_SCRIPT_READY_ENDPOINTS.save}
        data-no-single-segment-ai-generate="true"
        data-no-sse-generate="true"
        data-no-shortvideo-export="true"
        data-no-product-selection="true"
      >
        数据源：{LIVE_SCRIPT_READY_ENDPOINTS.search} 支持 sessionId、scriptType、keyword、executed 真实筛选；本页不调用已废弃单段生成接口，AI 生成请进入场次工作台使用整场或单槽 SSE。
      </Alert>
      {loadError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-scripts-load-error"
          data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.search}
          data-no-static-script-fallback="true"
          action={<Button color="inherit" size="small" onClick={load}>重试</Button>}
        >
          话术列表加载失败：{loadError}。降级策略：保留筛选条件，后端恢复后可直接重试。
        </Alert>
      ) : null}
      {deleteError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-scripts-delete-error"
          data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.delete}
          data-no-local-delete-on-error="true"
        >
          {deleteError}。删除失败不会本地移除话术。
        </Alert>
      ) : null}
      {executeError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-scripts-executed-error"
          data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.executed}
          data-no-local-executed-on-error="true"
        >
          {executeError}。执行状态失败不会本地伪造成功。
        </Alert>
      ) : null}
      <Grid container spacing={1.5} sx={{ mb: 2 }}>
        {[
          { label: '筛选命中话术', value: total, desc: `当前页 AI 生成 ${generatedCount} 条` },
          { label: '合规已检', value: checkedCount, desc: checkedCount === rows.length && rows.length > 0 ? '当前页全部完成检查' : '未检查话术需回工作台质检' },
          { label: '当前页已执行', value: executedCount, desc: '执行状态由 /live/script/executed 写入' },
        ].map(item => (
          <Grid item xs={12} md={4} key={item.label}>
            <Paper
              variant="outlined"
              sx={{ p: 1.5 }}
              data-testid="live-scripts-kpi-card"
              data-contract-source={item.desc.includes('/live/script/executed') ? LIVE_SCRIPT_READY_ENDPOINTS.executed : LIVE_SCRIPT_READY_ENDPOINTS.search}
            >
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.desc}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      {rows.length === 0 && !loading && !loadError && !query.sessionId && !query.scriptType && !query.keyword && !query.executed ? (
        <Box
          data-testid="live-scripts-empty"
          data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.search}
          data-no-static-script-fallback="true"
        >
          <EmptyState
            title="还没有直播话术"
            description="可以先在场次工作台生成完整话术，也可以手工新建单条话术。"
            action={{ text: '新建话术', onClick: openCreate }}
          />
        </Box>
      ) : (
      <Box
        sx={{ flex: 1, minHeight: 420 }}
        data-testid="live-scripts-table-surface"
        data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.search}
        data-no-server-export-request="true"
      >
      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={loading}
        paginationMode="server" paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        searchSlot={searchSlot}
        sx={{ flex: 1 }}
        showExport={false}
      />
      </Box>
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'live-scripts-save-dialog',
          'data-contract-source': LIVE_SCRIPT_READY_ENDPOINTS.save,
          'data-no-ai-generate': 'true',
        }}
      >
        <DialogTitle>{form.id ? '编辑话术' : '新建话术'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {saveError ? (
              <Alert
                severity="error"
                data-testid="live-scripts-save-error"
                data-contract-source={LIVE_SCRIPT_READY_ENDPOINTS.save}
                data-input-preserved="true"
              >
                {saveError}。保存失败不会关闭弹窗或伪造更新。
              </Alert>
            ) : null}
            <TextField label="场次ID" value={form.sessionId}
              onChange={e => setForm(f => ({ ...f, sessionId: e.target.value }))}
              required fullWidth size="small" inputProps={{ inputMode: 'numeric' }} />
            <TextField label="槽位标题/需求" value={form.scriptTitle}
              onChange={e => setForm(f => ({ ...f, scriptTitle: e.target.value }))}
              required fullWidth size="small" />
            <TextField label="话术内容" value={form.scriptContent}
              onChange={e => setForm(f => ({ ...f, scriptContent: e.target.value }))}
              required fullWidth size="small" multiline rows={4} />
            <FormControl fullWidth size="small">
              <InputLabel id="scripts-form-type-label">话术类型</InputLabel>
              <Select id="scripts-form-type" labelId="scripts-form-type-label" value={form.scriptType} label="话术类型"
                onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, scriptType: e.target.value }))}>
                {SCRIPT_TYPE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <Stack direction="row" spacing={2}>
              <TextField label="排序" value={form.sortOrder}
                onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                size="small" sx={{ flex: 1 }} inputProps={{ inputMode: 'numeric' }} />
              <TextField label="时长(秒)" value={form.duration}
                onChange={e => setForm(f => ({ ...f, duration: e.target.value }))}
                size="small" sx={{ flex: 1 }} inputProps={{ inputMode: 'numeric' }} />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleteId !== null}
        title="删除直播话术"
        content="确定删除该话术吗？删除后工作台、提词器和复盘可能无法再引用这条内容。"
        onClose={() => setDeleteId(null)}
        onConfirm={handleDelete}
      />
    </Box>
  )
}
