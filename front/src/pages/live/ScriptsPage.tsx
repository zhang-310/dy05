import { useState, useEffect, useCallback } from 'react'
import {
  Box, Button, TextField, Select, MenuItem, FormControl, InputLabel,
  Chip, IconButton, Tooltip, Dialog, DialogTitle, DialogContent,
  DialogActions, Stack, SelectChangeEvent, Typography,
} from '@mui/material'
import { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { liveApi, LiveScript, LiveScriptSave } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { StandardDataGrid } from '@/components/base/StandardDataGrid'
import { formatDate } from '@/utils/date'

const SCRIPT_TYPE_OPTIONS = ['开场白', '产品介绍', '促单', '互动', '结尾']

const SCRIPT_TYPE_COLOR: Record<string, 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'> = {
  '开场白': 'primary',
  '产品介绍': 'info',
  '促单': 'error',
  '互动': 'success',
  '结尾': 'default',
}

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

export default function ScriptsPage() {
  const toast = useToast()
  const [rows, setRows] = useState<LiveScript[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [sessionIdFilter, setSessionIdFilter] = useState('')
  const [scriptTypeFilter, setScriptTypeFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<FormState>(defaultForm())
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await liveApi.scriptSearch({
        page, rows: pageSize,
        sessionId: sessionIdFilter ? Number(sessionIdFilter) : undefined,
        scriptType: scriptTypeFilter || undefined,
        keyword: keyword || undefined,
      })
      setRows(res.list ?? [])
      setTotal(res.total ?? 0)
    } catch {
      toast('加载失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, sessionIdFilter, scriptTypeFilter, keyword, toast])

  useEffect(() => { load() }, [load])

  const handleSearch = () => { setPage(0); load() }
  const handleReset = () => {
    setSessionIdFilter(''); setScriptTypeFilter(''); setKeyword(''); setPage(0)
  }

  const openCreate = () => { setForm(defaultForm()); setDialogOpen(true) }
  const openEdit = (row: LiveScript) => {
    setForm({
      id: row.id,
      sessionId: String(row.sessionId),
      scriptTitle: String(row.scriptType || '话术'),
      scriptContent: row.scriptContent,
      scriptType: row.scriptType || '产品介绍',
      sortOrder: String(typeof row.sortOrder === 'number' ? row.sortOrder : 0),
      duration: String(typeof row.duration === 'number' ? row.duration : 0),
    })
    setDialogOpen(true)
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('确认删除该话术？')) return
    try { await liveApi.scriptDelete(id); toast('删除成功', 'success'); load() }
    catch { toast('删除失败', 'error') }
  }

  const handleSave = async () => {
    if (!form.scriptTitle.trim()) { toast('请填写话术标题', 'warning'); return }
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
        sortOrder: Number(form.sortOrder) || 0,
        duration: Number(form.duration) || 0,
      }
      await liveApi.scriptSave(payload)
      toast(form.id ? '更新成功' : '创建成功', 'success')
      setDialogOpen(false); load()
    } catch { toast('保存失败', 'error') } finally { setSaving(false) }
  }

  const columns: GridColDef[] = [
    { field: 'scriptTitle', headerName: '话术标题', flex: 2, minWidth: 160 },
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
      field: 'status', headerName: '状态', width: 80,
      renderCell: (p: GridRenderCellParams) =>
        <Chip label={p.value === 1 ? '启用' : '禁用'} color={p.value === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 110, sortable: false,
      renderCell: (p: GridRenderCellParams<LiveScript>) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="编辑"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => handleDelete(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField size="small" label="场次ID" value={sessionIdFilter}
        onChange={e => setSessionIdFilter(e.target.value)} sx={{ width: 110 }}
        inputProps={{ inputMode: 'numeric' }} />
      <FormControl size="small" sx={{ width: 130 }}>
        <InputLabel>话术类型</InputLabel>
        <Select value={scriptTypeFilter} label="话术类型"
          onChange={(e: SelectChangeEvent) => setScriptTypeFilter(e.target.value)}>
          <MenuItem value="">全部</MenuItem>
          {SCRIPT_TYPE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
        </Select>
      </FormControl>
      <TextField size="small" label="关键词" value={keyword}
        onChange={e => setKeyword(e.target.value)} sx={{ width: 160 }} />
      <Button variant="contained" size="small" onClick={handleSearch}>查询</Button>
      <Button variant="outlined" size="small" onClick={handleReset}>重置</Button>
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} size="small" onClick={openCreate}>新建话术</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>话术管理</Typography>
      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={loading}
        paginationMode="server" paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        searchSlot={searchSlot} actionSlot={actionSlot}
        sx={{ flex: 1 }}
      />

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? '编辑话术' : '新建话术'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="场次ID" value={form.sessionId}
              onChange={e => setForm(f => ({ ...f, sessionId: e.target.value }))}
              required fullWidth size="small" inputProps={{ inputMode: 'numeric' }} />
            <TextField label="话术标题" value={form.scriptTitle}
              onChange={e => setForm(f => ({ ...f, scriptTitle: e.target.value }))}
              required fullWidth size="small" />
            <TextField label="话术内容" value={form.scriptContent}
              onChange={e => setForm(f => ({ ...f, scriptContent: e.target.value }))}
              required fullWidth size="small" multiline rows={4} />
            <FormControl fullWidth size="small">
              <InputLabel>话术类型</InputLabel>
              <Select value={form.scriptType} label="话术类型"
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
    </Box>
  )
}


