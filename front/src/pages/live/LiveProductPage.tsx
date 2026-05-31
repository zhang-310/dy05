import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Alert, Box, Button, Chip, Stack, Typography, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, FormControl, InputLabel, Select, MenuItem,
  IconButton, Tooltip, CircularProgress, Paper, Divider,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import DragIndicatorIcon from '@mui/icons-material/DragIndicator'
import { useTheme } from '@mui/material/styles'
import { DataGrid, GridColDef, GridToolbarContainer, GridRowSelectionModel } from '@mui/x-data-grid'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader, ConfirmDialog, TableSkeleton, EmptyState } from '@/components/base'
import { liveApi, type LiveProduct } from '@/api/live'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

interface ToolbarProps {
  onAdd: () => void
  sessionId: string
  setSessionId: (v: string) => void
  sessions: Array<{ id: number; liveTitle: string }>
  selection: GridRowSelectionModel
  onBatchAddToSession: () => void
}

type KpiTone = 'primary' | 'success' | 'warning' | 'secondary'

const LIVE_PRODUCT_READY_ENDPOINTS = {
  sessions: '/live/session/search',
  products: '/live/product/search',
  save: '/live/product/save',
  delete: '/live/product/delete',
  batchAdd: '/live/product/batch-add',
} as const

const LIVE_PRODUCT_CONTEXT_ENDPOINTS = [
  LIVE_PRODUCT_READY_ENDPOINTS.sessions,
  LIVE_PRODUCT_READY_ENDPOINTS.products,
]

const LIVE_PRODUCT_UNSUPPORTED_ACTIONS = [
  'price-status-write',
  'server-export',
  'shortvideo-project-create',
  'ai-product-script-generate',
  'douyin-store-sync',
]

function Toolbar(props: ToolbarProps) {
  const { onAdd, sessionId, setSessionId, sessions, selection, onBatchAddToSession } = props
  return (
    <GridToolbarContainer sx={{ px: 1, py: 0.5, gap: 1 }}>
      <FormControl size="small" sx={{ minWidth: 200 }}>
        <InputLabel id="live-product-session-filter-label">场次筛选</InputLabel>
        <Select
          id="live-product-session-filter"
          labelId="live-product-session-filter-label"
          value={sessionId}
          label="场次筛选"
          onChange={e => setSessionId(String(e.target.value))}
        >
          <MenuItem value="">全部场次</MenuItem>
          {sessions.map((s) => (
            <MenuItem key={s.id} value={String(s.id)}>{s.liveTitle}</MenuItem>
          ))}
        </Select>
      </FormControl>
      <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={onAdd}>添加商品</Button>
      {selection.length > 0 && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
            已选 {selection.length} 条
          </Typography>
          <Button size="small" variant="outlined" onClick={onBatchAddToSession}>
            批量添加到场次
          </Button>
        </>
      )}
    </GridToolbarContainer>
  )
}

function buildToolbar(
  onAdd: () => void,
  sessionId: string,
  setSessionId: (v: string) => void,
  sessions: Array<{ id: number; liveTitle: string }>,
  selection: GridRowSelectionModel,
  onBatchAddToSession: () => void
) {
  return function ToolbarWrapper() {
    return <Toolbar onAdd={onAdd} sessionId={sessionId} setSessionId={setSessionId} sessions={sessions} selection={selection} onBatchAddToSession={onBatchAddToSession} />
  }
}

export default function LiveProductPage() {
  const theme = useTheme()
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
  const [selection, setSelection] = useState<GridRowSelectionModel>([])
  const [batchSessionDialogOpen, setBatchSessionDialogOpen] = useState(false)
  const [targetSessionId, setTargetSessionId] = useState('')
  const [saveError, setSaveError] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [batchError, setBatchError] = useState('')

  const errorText = (e: unknown, fallback: string) => e instanceof Error ? e.message : fallback

  const { data: sessionsData, isError: sessionsError, error: sessionsLoadError, refetch: refetchSessions } = useQuery({
    queryKey: ['live-sessions-select'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
  })
  const sessions = sessionsData?.list ?? []

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['live-products', sessionId, page, pageSize],
    queryFn: () => liveApi.productSearch({ page, rows: pageSize, sessionId: sessionId ? Number(sessionId) : undefined }),
  })
  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const saveMut = useMutation({
    mutationFn: (payload: Partial<LiveProduct>) => {
      setSaveError('')
      return liveApi.productSave({
        id: payload.id,
        sessionId: Number(payload.sessionId),
        productId: Number(payload.productId),
        productName: payload.productName,
        saleQuantity: Number(payload.saleQuantity ?? 0),
        position: payload.position == null ? undefined : Number(payload.position),
        productType: payload.productType,
        scriptSource: payload.scriptSource,
        productScriptId: payload.productScriptId == null ? undefined : Number(payload.productScriptId),
      } as Parameters<typeof liveApi.productSave>[0])
    },
    onSuccess: () => { toast('保存成功', 'success'); qc.invalidateQueries({ queryKey: ['live-products'] }); setAddOpen(false); setEditRow(null); setForm({}); setSaveError('') },
    onError: (e) => { setSaveError(`${LIVE_PRODUCT_READY_ENDPOINTS.save}：${errorText(e, '保存失败')}`); toast('保存失败', 'error') },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => { setDeleteError(''); return liveApi.productDelete(id) },
    onSuccess: () => { toast('已删除', 'success'); qc.invalidateQueries({ queryKey: ['live-products'] }); setDeleteId(null); setDeleteError('') },
    onError: (e) => { setDeleteError(`${LIVE_PRODUCT_READY_ENDPOINTS.delete}：${errorText(e, '删除失败')}`); toast('删除失败', 'error'); setDeleteId(null) },
  })

  const batchAddToSessionMut = useMutation({
    mutationFn: async ({ productIds, sessionId }: { productIds: number[]; sessionId: number }) => {
      setBatchError('')
      const items = productIds
        .map(id => rows.find(r => r.id === id))
        .filter((product): product is LiveProduct => Boolean(product))
        .map(product => ({
          productId: Number(product.productId ?? product.id),
          productName: String(product.productName ?? ''),
          productType: String(product.productType ?? product.productCategory ?? '直播商品'),
          imageUrl: product.imageUrl,
          price: Number(product.price ?? 0),
          productScriptId: product.productScriptId,
        }))
      if (items.length === 0) return
      await liveApi.productBatchAdd(sessionId, items)
    },
    onSuccess: () => {
      toast('批量添加成功', 'success')
      qc.invalidateQueries({ queryKey: ['live-products'] })
      setBatchSessionDialogOpen(false)
      setSelection([])
      setTargetSessionId('')
      setBatchError('')
    },
    onError: (e) => { setBatchError(`${LIVE_PRODUCT_READY_ENDPOINTS.batchAdd}：${errorText(e, '批量添加失败')}`); toast('批量添加失败', 'error') },
  })

  const handleBatchAddToSession = () => {
    if (!targetSessionId) {
      toast('请选择目标场次', 'warning')
      return
    }
    batchAddToSessionMut.mutate({
      productIds: selection as number[],
      sessionId: Number(targetSessionId),
    })
  }

  const openAdd = () => { setForm({ sessionId: sessionId ? Number(sessionId) : undefined, saleQuantity: 0, scriptSource: 'session' }); setSaveError(''); setEditRow(null); setAddOpen(true) }
  const openEdit = (row: LiveProduct) => { setForm({ ...row }); setSaveError(''); setEditRow(row); setAddOpen(true) }

  const columns: GridColDef[] = [
    { field: 'sortHandle', headerName: '拖拽', width: 70, sortable: false, filterable: false, renderCell: () => <DragIndicatorIcon sx={{ color: 'text.disabled', fontSize: 18 }} /> },
    { field: 'productId', headerName: '商品ID', width: 90, type: 'number' },
    { field: 'productName', headerName: '商品名称', flex: 1, minWidth: 160 },
    {
      field: 'revenue', headerName: '收益', width: 110,
      renderCell: (p) => <Typography color="error.main" fontWeight={600}>¥{Number(p.value ?? 0).toFixed(2)}</Typography>,
    },
    { field: 'saleQuantity', headerName: '销量', width: 90, type: 'number' },
    {
      field: 'position', headerName: '讲解位次', width: 100,
      renderCell: (p) => <Chip label={`第${p.value ?? '--'}位`} size="small" variant="outlined" />,
    },
    {
      field: 'productType', headerName: '商品类型', width: 120,
      renderCell: (p) => <Chip label={String(p.value ?? '未设置')} size="small" color={p.value ? 'info' : 'default'} />,
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
  const totalRevenue = rows.reduce((a, r) => a + Number(r.revenue ?? 0), 0)
  const totalSaleQuantity = rows.reduce((a, r) => a + Number(r.saleQuantity ?? 0), 0)
  const typedCount = rows.filter(r => Boolean(r.productType)).length
  const kpiColor = (tone: KpiTone) => theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main

  return (
    <Box
      sx={{ p: 3 }}
      data-testid="live-product-workbench"
      data-contract-scope="live-product-session-relations"
      data-ready-endpoints={Object.values(LIVE_PRODUCT_READY_ENDPOINTS).join('|')}
      data-context-endpoints={LIVE_PRODUCT_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={LIVE_PRODUCT_UNSUPPORTED_ACTIONS.join('|')}
    >
      <PageHeader
        title="场次商品管理"
        subtitle="管理直播场次中的商品排列与状态；批量添加使用后端 batch-add，避免页面层循环保存造成字段漂移。"
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" size="small" onClick={() => refetchSessions()}>刷新场次</Button>
            <Button variant="outlined" size="small" onClick={() => refetch()}>刷新商品</Button>
          </Stack>
        }
      />
      {(isError || sessionsError) ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-product-load-error"
          data-contract-source={LIVE_PRODUCT_CONTEXT_ENDPOINTS.join('|')}
          data-no-static-live-product-fallback="true"
        >
          数据加载失败：
          {isError ? ` ${LIVE_PRODUCT_READY_ENDPOINTS.products} 商品列表（${error instanceof Error ? error.message : 'product/search 不可用'}）` : ''}
          {sessionsError ? ` ${LIVE_PRODUCT_READY_ENDPOINTS.sessions} 场次下拉（${sessionsLoadError instanceof Error ? sessionsLoadError.message : 'session/search 不可用'}）` : ''}
          。降级策略：已保留当前筛选，后端恢复后点击刷新。
        </Alert>
      ) : null}
      {deleteError ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-product-delete-error"
          data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.delete}
          data-no-local-delete-on-error="true"
        >
          {deleteError}。删除失败不会本地移除商品。
        </Alert>
      ) : null}
      {batchError && !batchSessionDialogOpen ? (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="live-product-batch-add-error"
          data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.batchAdd}
          data-no-local-batch-result="true"
        >
          {batchError}。批量添加失败不会伪造目标场次商品。
        </Alert>
      ) : null}
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="live-product-contract-alert"
        data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.products}
        data-save-source={LIVE_PRODUCT_READY_ENDPOINTS.save}
        data-no-price-status-save="true"
        data-no-shortvideo-project-create="true"
        data-no-store-sync-endpoint="true"
      >
        数据源：{LIVE_PRODUCT_READY_ENDPOINTS.products} 返回直播场次商品关系；保存接口真实接收 sessionId、productId、productName、saleQuantity、position、productType、scriptSource、productScriptId。价格/上下架不属于 live_product 保存契约，本页不把它们伪造成可写字段。
      </Alert>
      {sessionId && rows.length === 0 && !isLoading && !isError ? (
        <Alert
          severity="info"
          sx={{ mb: 2 }}
          data-testid="live-product-session-empty"
          data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.products}
          data-no-static-live-product-fallback="true"
        >
          当前场次暂无商品。请添加商品后再进入话术生成，否则生成链路会缺少商品卖点、价格和排序依据。
        </Alert>
      ) : null}

      {/* Summary cards */}
      <Grid container spacing={2} mb={2}>
        {[
          { label: '商品总数', value: total, tone: 'primary' as const },
          { label: '当前页销量', value: totalSaleQuantity, tone: 'success' as const },
          { label: '当前页收益', value: `¥${totalRevenue.toFixed(0)}`, tone: 'warning' as const },
          { label: '已设类型', value: typedCount, tone: 'secondary' as const },
        ].map((kpi, i) => (
          <Grid item xs={12} sm={3} key={i}>
            <Paper
              variant="outlined"
              sx={{ p: 2, textAlign: 'center' }}
              data-testid="live-product-kpi-card"
              data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.products}
            >
              <Typography
                variant="h5"
                fontWeight={700}
                data-testid="live-product-kpi-value-surface"
                data-kpi-tone={kpi.tone}
                sx={{ color: kpiColor(kpi.tone) }}
              >
                {kpi.value}
              </Typography>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Box
        sx={{ height: 520 }}
        data-testid="live-product-table-surface"
        data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.products}
        data-no-server-export-request="true"
      >
        {isLoading && rows.length === 0 ? (
          <TableSkeleton rows={10} columns={6} />
        ) : rows.length === 0 && !sessionId ? (
          <Box
            data-testid="live-product-empty"
            data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.products}
            data-no-static-live-product-fallback="true"
          >
            <EmptyState
              title="还没有商品"
              description="添加第一个商品到直播场次，开始商品讲解"
              action={{
                text: '添加商品',
                onClick: openAdd,
              }}
            />
          </Box>
        ) : (
          <DataGrid
            rows={rows}
            columns={columns}
            loading={isLoading}
            rowCount={total}
            paginationMode="server"
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
            pageSizeOptions={[10, 20, 50]}
            checkboxSelection
            rowSelectionModel={selection}
            onRowSelectionModelChange={setSelection}
            slots={{ toolbar: buildToolbar(openAdd, sessionId, setSessionId, sessions, selection, () => setBatchSessionDialogOpen(true)) }}
            slotProps={undefined}
            disableRowSelectionOnClick
            getRowId={(r) => (r as LiveProduct).id ?? 0}
          />
        )}
      </Box>

      {/* Add / Edit dialog */}
      <Dialog
        open={addOpen}
        onClose={() => { setAddOpen(false); setForm({}) }}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'live-product-save-dialog',
          'data-contract-source': LIVE_PRODUCT_READY_ENDPOINTS.save,
          'data-no-price-status-save': 'true',
        }}
      >
        <DialogTitle>{editRow ? '编辑商品' : '添加商品'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              label="商品名称" size="small" fullWidth required
              value={form.productName ?? ''}
              onChange={e => setForm(f => ({ ...f, productName: e.target.value }))}
            />
            <TextField
              label="商品ID" size="small" type="number" fullWidth required
              value={form.productId ?? ''}
              onChange={e => setForm(f => ({ ...f, productId: Number(e.target.value) }))}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="销量" size="small" type="number" sx={{ flex: 1 }}
                value={form.saleQuantity ?? ''}
                onChange={e => setForm(f => ({ ...f, saleQuantity: Number(e.target.value) }))}
              />
              <TextField
                label="讲解位次" size="small" type="number" sx={{ flex: 1 }}
                value={form.position ?? ''}
                onChange={e => setForm(f => ({ ...f, position: Number(e.target.value) }))}
              />
            </Stack>
            <FormControl size="small" fullWidth>
              <InputLabel id="live-product-session-label">关联场次</InputLabel>
              <Select id="live-product-session" labelId="live-product-session-label" value={String(form.sessionId ?? '')} label="关联场次"
                onChange={e => setForm(f => ({ ...f, sessionId: Number(e.target.value) }))}
              >
                {sessions.map((s: { id: number; liveTitle: string }) => (
                  <MenuItem key={s.id} value={String(s.id)}>{s.liveTitle}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel id="live-product-type-label">商品类型</InputLabel>
              <Select id="live-product-type" labelId="live-product-type-label" value={String(form.productType ?? '')} label="商品类型"
                onChange={e => setForm(f => ({ ...f, productType: e.target.value }))}
              >
                <MenuItem value="">未设置</MenuItem>
                <MenuItem value="hot">爆品</MenuItem>
                <MenuItem value="control">控单品</MenuItem>
                <MenuItem value="profit">利润品</MenuItem>
                <MenuItem value="loss">亏品</MenuItem>
                <MenuItem value="flat">平价品</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="商品话术ID" size="small" type="number" fullWidth
              value={form.productScriptId ?? ''}
              onChange={e => setForm(f => ({ ...f, productScriptId: e.target.value === '' ? undefined : Number(e.target.value) }))}
            />
            <Alert severity="info">
              价格、图片和上下架状态来自商品域，不由 {LIVE_PRODUCT_READY_ENDPOINTS.save} 写入；本表只维护场次内商品关系、讲解顺序、销量和话术绑定。
            </Alert>
            {saveError ? (
              <Alert
                severity="error"
                data-testid="live-product-save-error"
                data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.save}
                data-input-preserved="true"
              >
                {saveError}。保存失败不会关闭弹窗或伪造更新。
              </Alert>
            ) : null}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setAddOpen(false); setForm({}) }}>取消</Button>
          <Button
            variant="contained"
            onClick={() => saveMut.mutate(form)}
            disabled={saveMut.isPending || !form.productName || !form.sessionId || !form.productId}
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

      {/* Batch add to session dialog */}
      <Dialog
        open={batchSessionDialogOpen}
        onClose={() => setBatchSessionDialogOpen(false)}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          'data-testid': 'live-product-batch-dialog',
          'data-contract-source': LIVE_PRODUCT_READY_ENDPOINTS.batchAdd,
          'data-no-local-batch-result': 'true',
        }}
      >
        <DialogTitle>批量添加到场次</DialogTitle>
        <DialogContent>
          {batchError ? (
            <Alert
              severity="error"
              sx={{ mt: 2 }}
              data-testid="live-product-batch-dialog-error"
              data-contract-source={LIVE_PRODUCT_READY_ENDPOINTS.batchAdd}
              data-no-local-batch-result="true"
            >
              {batchError}
            </Alert>
          ) : null}
          <FormControl fullWidth sx={{ mt: 2 }}>
            <InputLabel id="live-product-batch-session-label">选择目标场次</InputLabel>
            <Select
              id="live-product-batch-session"
              labelId="live-product-batch-session-label"
              value={targetSessionId}
              label="选择目标场次"
              onChange={(e) => setTargetSessionId(e.target.value)}
            >
              {sessions.map((s) => (
                <MenuItem key={s.id} value={String(s.id)}>{s.liveTitle}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            将 {selection.length} 个商品添加到选定的场次
          </Typography>
          <Divider sx={{ my: 2 }} />
          <Typography variant="caption" color="text.secondary">
            批量添加提交 productId、productName、productType、productScriptId。price/imageUrl 仅在后端 VO 支持时透传，不作为本页保存后的真实字段展示；失败时不创建本地目标场次商品。
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBatchSessionDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleBatchAddToSession}
            disabled={!targetSessionId || batchAddToSessionMut.isPending}
          >
            {batchAddToSessionMut.isPending ? '添加中...' : '确认添加'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

