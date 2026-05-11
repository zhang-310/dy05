import { useState, useCallback } from 'react'
import {
  Box, Button, TextField, Select, MenuItem, FormControl, InputLabel,
  Chip, IconButton, Tooltip, Dialog, DialogTitle, DialogContent,
  DialogActions, Stack, SelectChangeEvent, Typography,
} from '@mui/material'
import { GridColDef, GridRenderCellParams, GridPaginationModel, GridRowSelectionModel } from '@mui/x-data-grid'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import StopIcon from '@mui/icons-material/Stop'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import BuildIcon from '@mui/icons-material/Build'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { liveApi, LiveSession, LiveSessionSave } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { StandardDataGrid, TableSkeleton, EmptyState } from '@/components/base'
import { formatDate } from '@/utils/date'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' | 'error' | 'info' | 'primary' | 'secondary' }> = {
  0: { label: '待开播', color: 'default' },
  1: { label: '直播中', color: 'success' },
  2: { label: '已结束', color: 'default' },
}

const SCRIPT_STYLE_OPTIONS = ['专业', '友好', '激情', '种草', '促销']
const SESSION_TYPE_OPTIONS = ['普通', '品牌专场', '大促']
const LIVE_FORMAT_OPTIONS = ['单人', '多人', '连麦']

interface FormState {
  id?: number; liveTitle: string; scriptStyle: string; sessionType: string
  liveFormat: string; scheduledTime: string; liveDescription: string
  accountId: string; personaId: string
}

const defaultForm = (): FormState => ({
  liveTitle: '', scriptStyle: '专业', sessionType: '普通',
  liveFormat: '单人', scheduledTime: '', liveDescription: '',
  accountId: '', personaId: '',
})

export default function SessionsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 })
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<FormState>(defaultForm())
  const [saving, setSaving] = useState(false)
  const [selection, setSelection] = useState<GridRowSelectionModel>([])

  const { data, isFetching, refetch } = useQuery({
    queryKey: ['live-sessions', pagination.page, pagination.pageSize, keyword, statusFilter],
    queryFn: () => liveApi.sessionSearch({
      page: pagination.page,
      rows: pagination.pageSize,
      keyword: keyword || undefined,
      status: statusFilter !== '' ? Number(statusFilter) : undefined,
    }),
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const invalidate = useCallback(() => qc.invalidateQueries({ queryKey: ['live-sessions'] }), [qc])

  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.sessionDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); invalidate() },
    onError: () => toast('删除失败', 'error'),
  })
  const startMut = useMutation({
    mutationFn: (id: number) => liveApi.sessionStart(id),
    onSuccess: () => { toast('已开播', 'success'); invalidate() },
    onError: () => toast('操作失败', 'error'),
  })
  const endMut = useMutation({
    mutationFn: (id: number) => liveApi.sessionEnd(id),
    onSuccess: () => { toast('已结束', 'success'); invalidate() },
    onError: () => toast('操作失败', 'error'),
  })
  const cloneMut = useMutation({
    mutationFn: (row: LiveSession) => liveApi.sessionClone(row.id),
    onSuccess: () => { toast('克隆成功', 'success'); invalidate() },
    onError: () => toast('克隆失败', 'error'),
  })

  const batchDeleteMut = useMutation({
    mutationFn: async (ids: number[]) => {
      await Promise.all(ids.map(id => liveApi.sessionDelete(id)))
    },
    onSuccess: () => { toast('批量删除成功', 'success'); invalidate(); setSelection([]) },
    onError: () => toast('批量删除失败', 'error'),
  })

  const batchStartMut = useMutation({
    mutationFn: async (ids: number[]) => {
      await Promise.all(ids.map(id => liveApi.sessionStart(id)))
    },
    onSuccess: () => { toast('批量开播成功', 'success'); invalidate(); setSelection([]) },
    onError: () => toast('批量开播失败', 'error'),
  })

  const batchEndMut = useMutation({
    mutationFn: async (ids: number[]) => {
      await Promise.all(ids.map(id => liveApi.sessionEnd(id)))
    },
    onSuccess: () => { toast('批量结束成功', 'success'); invalidate(); setSelection([]) },
    onError: () => toast('批量结束失败', 'error'),
  })

  const handleSearch = () => { setPagination(p => ({ ...p, page: 0 })); refetch() }
  const handleReset = () => { setKeyword(''); setStatusFilter(''); setPagination(p => ({ ...p, page: 0 })) }

  const openCreate = () => { setForm(defaultForm()); setDialogOpen(true) }
  const openEdit = (row: LiveSession) => {
    setForm({
      id: row.id, liveTitle: row.liveTitle, scriptStyle: row.scriptStyle || '专业',
      sessionType: row.sessionType || '普通', liveFormat: row.liveFormat || '单人',
      scheduledTime: row.scheduledTime ? row.scheduledTime.slice(0, 16) : '',
      liveDescription: row.liveDescription || '',
      accountId: String(row.accountId || ''), personaId: String(row.personaId || ''),
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!form.liveTitle.trim()) { toast('请输入场次标题', 'warning'); return }
    setSaving(true)
    try {
      const payload: Partial<LiveSessionSave> = {
        id: form.id,
        liveTitle: form.liveTitle,
        scriptStyle: form.scriptStyle,
        sessionType: form.sessionType,
        liveFormat: form.liveFormat,
        scheduledTime: form.scheduledTime || undefined,
        liveDescription: form.liveDescription || undefined,
        accountId: form.accountId ? Number(form.accountId) : undefined,
        personaId: form.personaId ? Number(form.personaId) : undefined,
      }
      await liveApi.sessionSave(payload)
      toast(form.id ? '更新成功' : '创建成功', 'success')
      setDialogOpen(false); invalidate()
    } catch { toast('保存失败', 'error') } finally { setSaving(false) }
  }

  const columns: GridColDef[] = [
    { field: 'liveTitle', headerName: '场次标题', flex: 2, minWidth: 160 },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: (p: GridRenderCellParams) => {
        const s = STATUS_MAP[p.value as number] ?? { label: '未知', color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    { field: 'sessionType', headerName: '场次类型', width: 110 },
    { field: 'liveFormat', headerName: '直播形式', width: 100 },
    { field: 'scheduledTime', headerName: '预定时间', width: 160,
      valueFormatter: (v: string) => v ? v.replace('T', ' ').slice(0, 16) : '-' },
    { field: 'totalGmv', headerName: 'GMV', width: 110, type: 'number',
      renderCell: (p: GridRenderCellParams) => {
        const v = p.value as number
        if (!v) return <span style={{ color: '#bbb' }}>-</span>
        return <span style={{ color: '#1a7f3c', fontWeight: 600 }}>{v >= 10000 ? `¥${(v/10000).toFixed(1)}万` : `¥${v}`}</span>
      } },
    { field: 'viewers', headerName: '观看', width: 90, type: 'number' },
    { field: 'likes', headerName: '点赞', width: 80, type: 'number' },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 260, sortable: false,
      renderCell: (p: GridRenderCellParams<LiveSession>) => {
        const row = p.row
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="进入工作台">
              <IconButton size="small" color="primary" onClick={() => navigate(`/admin/live/sessions/${row.id}`)}>
                <BuildIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {row.status === 0 && (
              <Tooltip title="开播"><IconButton size="small" color="success" onClick={() => startMut.mutate(row.id)}><PlayArrowIcon fontSize="small" /></IconButton></Tooltip>
            )}
            {row.status === 1 && (
              <Tooltip title="结束"><IconButton size="small" color="warning" onClick={() => endMut.mutate(row.id)}><StopIcon fontSize="small" /></IconButton></Tooltip>
            )}
            <Tooltip title="克隆"><IconButton size="small" onClick={() => cloneMut.mutate(row)}><ContentCopyIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="编辑"><IconButton size="small" onClick={() => openEdit(row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="删除"><IconButton size="small" color="error" onClick={() => deleteMut.mutate(row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
          </Stack>
        )
      },
    },
  ]

  const searchSlot = (
    <>
      <TextField label="关键词" value={keyword} onChange={e => setKeyword(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} size="small" sx={{ width: 200 }} />
      <FormControl size="small" sx={{ width: 120 }}>
        <InputLabel>状态</InputLabel>
        <Select value={statusFilter} label="状态" onChange={(e: SelectChangeEvent) => setStatusFilter(e.target.value)}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="0">待开播</MenuItem>
          <MenuItem value="1">直播中</MenuItem>
          <MenuItem value="2">已结束</MenuItem>
        </Select>
      </FormControl>
      <Button variant="contained" onClick={handleSearch}>搜索</Button>
      <Button onClick={handleReset}>重置</Button>
      {selection.length > 0 && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ ml: 2 }}>
            已选 {selection.length} 条
          </Typography>
          <Button
            size="small"
            variant="outlined"
            startIcon={<PlayArrowIcon />}
            onClick={() => batchStartMut.mutate(selection as number[])}
          >
            批量开播
          </Button>
          <Button
            size="small"
            variant="outlined"
            startIcon={<StopIcon />}
            onClick={() => batchEndMut.mutate(selection as number[])}
          >
            批量结束
          </Button>
          <Button
            size="small"
            variant="outlined"
            color="error"
            startIcon={<DeleteIcon />}
            onClick={() => batchDeleteMut.mutate(selection as number[])}
          >
            批量删除
          </Button>
        </>
      )}
    </>
  )

  const actionSlot = (
    <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>新建场次</Button>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>直播场次</Typography>
      {isFetching && rows.length === 0 ? (
        <TableSkeleton rows={10} columns={7} />
      ) : rows.length === 0 && !keyword && !statusFilter ? (
        <EmptyState
          title="还没有直播场次"
          description="创建第一个直播场次，开始您的直播运营之旅"
          action={{
            text: '新建场次',
            onClick: openCreate,
          }}
        />
      ) : (
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={total}
          paginationMode="server"
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          checkboxSelection
          rowSelectionModel={selection}
          onRowSelectionModelChange={setSelection}
          searchSlot={searchSlot}
          actionSlot={actionSlot}
          sx={{ flex: 1 }}
        />
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{form.id ? '编辑场次' : '新建场次'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="场次标题" value={form.liveTitle} onChange={e => setForm(f => ({ ...f, liveTitle: e.target.value }))} fullWidth size="small" required />
            <TextField label="账号ID" value={form.accountId} onChange={e => setForm(f => ({ ...f, accountId: e.target.value }))} fullWidth size="small" />
            <TextField label="人设ID" value={form.personaId} onChange={e => setForm(f => ({ ...f, personaId: e.target.value }))} fullWidth size="small" />
            <FormControl fullWidth size="small">
              <InputLabel>话术风格</InputLabel>
              <Select value={form.scriptStyle} label="话术风格" onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, scriptStyle: e.target.value }))}>
                {SCRIPT_STYLE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>场次类型</InputLabel>
              <Select value={form.sessionType} label="场次类型" onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, sessionType: e.target.value }))}>
                {SESSION_TYPE_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>直播形式</InputLabel>
              <Select value={form.liveFormat} label="直播形式" onChange={(e: SelectChangeEvent) => setForm(f => ({ ...f, liveFormat: e.target.value }))}>
                {LIVE_FORMAT_OPTIONS.map(o => <MenuItem key={o} value={o}>{o}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="预定开始时间" type="datetime-local" value={form.scheduledTime}
              onChange={e => setForm(f => ({ ...f, scheduledTime: e.target.value }))}
              fullWidth size="small" InputLabelProps={{ shrink: true }} />
            <TextField label="场次描述" value={form.liveDescription}
              onChange={e => setForm(f => ({ ...f, liveDescription: e.target.value }))}
              fullWidth size="small" multiline rows={3} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>取消</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>{saving ? '保存中...' : '保存'}</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
