import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Button, Chip, Stack, Typography, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, FormControl, InputLabel, Select, MenuItem,
  IconButton, Tooltip, CircularProgress, Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import DragIndicatorIcon from '@mui/icons-material/DragIndicator'
import { DataGrid, GridColDef, GridToolbarContainer } from '@mui/x-data-grid'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader, ConfirmDialog } from '@/components/base'
import { liveApi, type LiveProduct } from '@/api/live'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

interface ToolbarProps {
  onAdd: () => void
  sessionId: string
  setSessionId: (v: string) => void
  sessions: Array<{ id: number; liveTitle: string }>
}

function Toolbar(props: ToolbarProps) {
  const { onAdd, sessionId, setSessionId, sessions } = props
  return (
    <GridToolbarContainer sx={{ px: 1, py: 0.5, gap: 1 }}>
      <FormControl size="small" sx={{ minWidth: 200 }}>
        <InputLabel>场次筛选</InputLabel>
        <Select value={sessionId} label="场次筛选" onChange={e => setSessionId(String(e.target.value))}>
          <MenuItem value="">全部场次</MenuItem>
          {sessions.map((s) => (
            <MenuItem key={s.id} value={String(s.id)}>{s.liveTitle}</MenuItem>
          ))}
        </Select>
      </FormControl>
      <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={onAdd}>添加商品</Button>
    </GridToolbarContainer>
  )
}

function buildToolbar(onAdd: () => void, sessionId: string, setSessionId: (v: string) => void, sessions: Array<{ id: number; liveTitle: string }>) {
  return function ToolbarWrapper() {
    return <Toolbar onAdd={onAdd} sessionId={sessionId} setSessionId={setSessionId} sessions={sessions} />
  }
}

export default function LiveProductPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const [sessionId, setSessionId] = useState(searchParams.get('sessionId') ?? '')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [addOpen, setAddOpen] = useState(false)
  const [editRow, setEditRow] = useState<LiveProduct | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [form, setForm] = useState<Partial<LiveProduct>>({})

  const { data: sessionsData } = useQuery({
    queryKey: ['live-sessions-select'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
  })
  const sessions = sessionsData?.list ?? []

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['live-products', sessionId, page, pageSize],
    queryFn: () => liveApi.productSearch({ page, rows: pageSize, sessionId: sessionId ? Number(sessionId) : undefined }),
  })
  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const saveMut = useMutation({
    mutationFn: (payload: Partial<LiveProduct>) => liveApi.productSave(payload as Parameters<typeof liveApi.productSave>[0]),
    onSuccess: () => { toast('保存成功', 'success'); qc.invalidateQueries({ queryKey: ['live-products'] }); setAddOpen(false); setEditRow(null); setForm({}) },
    onError: () => toast('保存失败', 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.productDelete(id),
    onSuccess: () => { toast('已删除', 'success'); qc.invalidateQueries({ queryKey: ['live-products'] }); setDeleteId(null) },
    onError: () => toast('删除失败', 'error'),
  })

  const openAdd = () => { setForm({ sessionId: sessionId ? Number(sessionId) : undefined, status: 1 }); setEditRow(null); setAddOpen(true) }
  const openEdit = (row: LiveProduct) => { setForm({ ...row }); setEditRow(row); setAddOpen(true) }

  const columns: GridColDef[] = [
    { field: 'sortOrder', headerName: '排序', width: 70, renderCell: () => <DragIndicatorIcon sx={{ color: 'text.disabled', fontSize: 18 }} /> },
    { field: 'productName', headerName: '商品名称', flex: 1, minWidth: 160 },
    {
      field: 'price', headerName: '价格', width: 110,
      renderCell: (p) => <Typography color="error.main" fontWeight={600}>¥{Number(p.value ?? 0).toFixed(2)}</Typography>,
    },
    {
      field: 'position', headerName: '讲解位次', width: 100,
      renderCell: (p) => <Chip label={`第${p.value ?? '--'}位`} size="small" variant="outlined" />,
    },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: (p) => <Chip label={p.value === 1 ? '上架' : '下架'} size="small" color={p.value === 1 ? 'success' : 'default'} />,
    },
    { field: 'createTime', headerName: '创建时间', width: 170 },
    {
      field: 'actions', headerName: '操作', width: 130, sortable: false,
      renderCell: (p) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="编辑">
            <IconButton size="small" onClick={() => openEdit(p.row as LiveProduct)}><EditIcon fontSize="small" /></IconButton>
          </Tooltip>
          <Tooltip title="删除">
            <IconButton size="small" color="error" onClick={() => setDeleteId((p.row as LiveProduct).id ?? 0)}><DeleteIcon fontSize="small" /></IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]
  // Stats summary
  const avgPrice = rows.length ? rows.reduce((a, r) => a + Number(r.price ?? 0), 0) / rows.length : 0
  const onlineCount = rows.filter(r => r.status === 1).length

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="场次商品管理"
        subtitle="管理直播场次中的商品排列与状态"
        actions={
          <Button variant="outlined" size="small" onClick={() => refetch()}>刷新</Button>
        }
      />

      {/* Summary cards */}
      <Grid container spacing={2} mb={2}>
        {[
          { label: '商品总数', value: total, color: '#1976d2' },
          { label: '上架商品', value: onlineCount, color: '#4caf50' },
          { label: '平均价格', value: `¥${avgPrice.toFixed(0)}`, color: '#ff9800' },
        ].map((kpi, i) => (
          <Grid item xs={12} sm={4} key={i}>
            <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h5" fontWeight={700} sx={{ color: kpi.color }}>{kpi.value}</Typography>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Box sx={{ height: 520 }}>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={isLoading}
          rowCount={total}
          paginationMode="server"
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
          pageSizeOptions={[10, 20, 50]}
          slots={{ toolbar: buildToolbar(openAdd, sessionId, setSessionId, sessions) }}
          slotProps={undefined}
          disableRowSelectionOnClick
          getRowId={(r) => (r as LiveProduct).id ?? 0}
        />
      </Box>

      {/* Add / Edit dialog */}
      <Dialog open={addOpen} onClose={() => { setAddOpen(false); setForm({}) }} maxWidth="sm" fullWidth>
        <DialogTitle>{editRow ? '编辑商品' : '添加商品'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              label="商品名称" size="small" fullWidth required
              value={form.productName ?? ''}
              onChange={e => setForm(f => ({ ...f, productName: e.target.value }))}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="价格" size="small" type="number" sx={{ flex: 1 }}
                value={form.price ?? ''}
                onChange={e => setForm(f => ({ ...f, price: Number(e.target.value) }))}
                InputProps={{ startAdornment: <Typography mr={0.5}>¥</Typography> }}
              />
              <TextField
                label="讲解位次" size="small" type="number" sx={{ flex: 1 }}
                value={form.position ?? ''}
                onChange={e => setForm(f => ({ ...f, position: Number(e.target.value) }))}
              />
              <TextField
                label="排序权重" size="small" type="number" sx={{ flex: 1 }}
                value={form.sortOrder ?? ''}
                onChange={e => setForm(f => ({ ...f, sortOrder: Number(e.target.value) }))}
              />
            </Stack>
            <FormControl size="small" fullWidth>
              <InputLabel>关联场次</InputLabel>
              <Select value={String(form.sessionId ?? '')} label="关联场次"
                onChange={e => setForm(f => ({ ...f, sessionId: Number(e.target.value) }))}
              >
                {sessions.map((s: { id: number; liveTitle: string }) => (
                  <MenuItem key={s.id} value={String(s.id)}>{s.liveTitle}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel>状态</InputLabel>
              <Select value={String(form.status ?? 1)} label="状态"
                onChange={e => setForm(f => ({ ...f, status: Number(e.target.value) }))}
              >
                <MenuItem value="1">上架</MenuItem>
                <MenuItem value="0">下架</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setAddOpen(false); setForm({}) }}>取消</Button>
          <Button
            variant="contained"
            onClick={() => saveMut.mutate(form)}
            disabled={saveMut.isPending || !form.productName}
            startIcon={saveMut.isPending ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            保存
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleteId !== null}
        content="确定要删除该商品吗？此操作不可恢复。"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}

