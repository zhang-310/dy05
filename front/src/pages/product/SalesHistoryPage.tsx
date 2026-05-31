import { useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, DataGridEmptyOverlay, ErrorAlert } from '@/components/base'
import { productApi, type SalesHistory, type SalesHistorySave, type SalesHistorySearchParams } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getErrorMessage } from '@/utils/errorHandler'

const CHANNEL_OPTIONS = [
  { value: 'douyin_live', label: '抖音直播' },
  { value: 'store', label: '门店' },
  { value: 'manual', label: '手动录入' },
]

const SALES_HISTORY_ENDPOINTS = {
  search: '/product/sales-history/search',
  save: '/product/sales-history/save',
  totalAmount: '/product/sales-history/total-sales-amount',
  totalQuantity: '/product/sales-history/total-sales-quantity',
  products: '/product/search',
} as const
const SALES_HISTORY_READY_ENDPOINTS = Object.values(SALES_HISTORY_ENDPOINTS)
const SALES_HISTORY_CONTEXT_ENDPOINTS = [
  '/product/update-inventory',
  '/product/script-effectiveness/ranking',
  '/live/product/search',
  '/dashboard/product-gmv-summary',
]
const SALES_HISTORY_UNSUPPORTED_ACTIONS = [
  'auto-deduct-inventory-ui',
  'server-export',
  'gmv-dashboard-fallback',
  'effectiveness-ranking-fallback',
  'static-sales-fallback',
]

export default function SalesHistoryPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [filters, setFilters] = useState({
    productId: '' as number | '',
    channelSource: '',
    sessionId: '',
    startTime: '',
    endTime: '',
  })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({ productId: '' as number | '', saleAmount: '', saleQuantity: '', channelSource: 'manual', saleTime: '', sessionId: '' })

  const searchPayload: SalesHistorySearchParams = {
    page,
    rows: pageSize,
    productId: filters.productId !== '' ? filters.productId : undefined,
    channelSource: filters.channelSource || undefined,
    sessionId: filters.sessionId.trim() || undefined,
    startTime: filters.startTime || undefined,
    endTime: filters.endTime || undefined,
  }

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sales-history', searchPayload],
    queryFn: () => productApi.salesHistorySearch(searchPayload),
  })

  const { data: products, isError: productsIsError, error: productsError, refetch: refetchProducts } = useQuery({
    queryKey: ['products-simple'],
    queryFn: () => productApi.list({ page: 0, rows: 200 }),
  })

  const selectedProductId = filters.productId !== '' ? filters.productId : undefined

  const {
    data: totalSalesAmount,
    isError: totalAmountIsError,
    error: totalAmountError,
    refetch: refetchTotalAmount,
  } = useQuery({
    queryKey: ['sales-history-total-amount', selectedProductId],
    queryFn: () => productApi.salesHistoryTotalSalesAmount(selectedProductId!),
    enabled: selectedProductId != null,
  })

  const {
    data: totalSalesQuantity,
    isError: totalQuantityIsError,
    error: totalQuantityError,
    refetch: refetchTotalQuantity,
  } = useQuery({
    queryKey: ['sales-history-total-quantity', selectedProductId],
    queryFn: () => productApi.salesHistoryTotalSalesQuantity(selectedProductId!),
    enabled: selectedProductId != null,
  })

  const saveMut = useMutation({
    mutationFn: (payload: SalesHistorySave) => productApi.salesHistorySave(payload),
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormOpen(false)
      setForm({ productId: '', saleAmount: '', saleQuantity: '', channelSource: 'manual', saleTime: '', sessionId: '' })
      qc.invalidateQueries({ queryKey: ['sales-history'] })
      qc.invalidateQueries({ queryKey: ['sales-history-total-amount'] })
      qc.invalidateQueries({ queryKey: ['sales-history-total-quantity'] })
    },
    onError: (e: Error) => toast(getErrorMessage(e), 'error'),
  })

  const rows: SalesHistory[] = data?.list ?? []
  const total = data?.total ?? 0
  const productList = products?.list ?? []
  const selectedProduct = productList.find((product) => product.id === selectedProductId)
  const summary = rows.reduce(
    (acc, row) => {
      acc.amount += Number(row.saleAmount ?? row.revenue ?? 0)
      acc.quantity += Number(row.saleQuantity ?? row.quantity ?? 0)
      if (String(row.channelSource ?? row.platform ?? '') === 'douyin_live') acc.live += 1
      return acc
    },
    { amount: 0, quantity: 0, live: 0 },
  )

  const openAdd = () => {
    saveMut.reset()
    setForm({ productId: '', saleAmount: '', saleQuantity: '', channelSource: 'manual', saleTime: '', sessionId: '' })
    setFormOpen(true)
  }

  const resetFilters = () => {
    setFilters({ productId: '', channelSource: '', sessionId: '', startTime: '', endTime: '' })
    setPage(0)
  }

  const handleSave = () => {
    if (form.productId === '') { toast('请选择商品', 'warning'); return }
    const saleAmount = Number(form.saleAmount)
    if (!Number.isFinite(saleAmount) || saleAmount <= 0) { toast('请填写有效销售金额', 'warning'); return }
    const saleQuantity = form.saleQuantity === '' ? undefined : Number(form.saleQuantity)
    if (saleQuantity != null && (!Number.isFinite(saleQuantity) || saleQuantity < 0)) { toast('销售数量不能小于 0', 'warning'); return }
    saveMut.mutate({
      productId: form.productId,
      saleAmount,
      saleQuantity,
      channelSource: form.channelSource || undefined,
      saleTime: form.saleTime || undefined,
      sessionId: form.sessionId || undefined,
    })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    {
      field: 'productId', headerName: '商品', flex: 1,
      renderCell: (p) => {
        const prod = productList.find((x) => x.id === p.value)
        return prod ? String(prod.productName ?? prod.id) : String(p.value)
      },
    },
    {
      field: 'saleAmount', headerName: '销售金额', width: 130,
      renderCell: (p) => `¥${Number(p.value ?? (p.row as SalesHistory).revenue ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
    },
    { field: 'saleQuantity', headerName: '数量', width: 90, valueFormatter: (v) => v ?? '—' },
    {
      field: 'channelSource', headerName: '渠道', width: 120,
      renderCell: (p) => CHANNEL_OPTIONS.find((o) => o.value === (p.value ?? (p.row as SalesHistory).platform))?.label ?? String(p.value ?? (p.row as SalesHistory).platform ?? '-'),
    },
    { field: 'saleTime', headerName: '销售时间', width: 180, valueGetter: (_, row) => row.saleTime ?? row.saleDate ?? '' },
    { field: 'sessionId', headerName: '场次 ID', width: 120, valueFormatter: (v) => v ?? '—' },
    { field: 'createTime', headerName: '录入时间', width: 180 },
  ]

  return (
    <Box
      data-testid="sales-history-workbench"
      data-contract-scope="product-sales-history"
      data-ready-endpoints={SALES_HISTORY_READY_ENDPOINTS.join('|')}
      data-context-endpoints={SALES_HISTORY_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SALES_HISTORY_UNSUPPORTED_ACTIONS.join('|')}
      data-product-id={filters.productId || ''}
      data-channel={filters.channelSource || '全部'}
      data-session-id={filters.sessionId || ''}
      data-page={page}
      data-rows={pageSize}
      data-result-count={rows.length}
      data-total={total}
      data-selected-product-id={selectedProductId ?? ''}
      data-current-page-amount={summary.amount}
      data-current-page-quantity={summary.quantity}
      data-no-static-sales-fallback="true"
      sx={{ p: 3 }}
    >
      <PageHeader
        title="销售记录"
        subtitle="商品销售历史数据，保存字段对齐 SalesHistorySaveVO：saleAmount / saleQuantity / saleTime / channelSource。"
        breadcrumbs={[{ label: '商品' }, { label: '销售记录' }]}
        actions={(
          <Stack direction="row" spacing={1}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>录入记录</Button>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => {
                refetch()
                refetchProducts()
                if (selectedProductId != null) {
                  refetchTotalAmount()
                  refetchTotalQuantity()
                }
              }}
            >
              刷新
            </Button>
          </Stack>
        )}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="sales-history-contract-alert"
        data-contract-source={`${SALES_HISTORY_ENDPOINTS.search}|${SALES_HISTORY_ENDPOINTS.save}`}
        data-no-auto-deduct-inventory-ui="true"
        data-no-server-export-request="true"
        data-no-gmv-dashboard-fallback="true"
        sx={{ mb: 2 }}
      >
        真实接口：<code>{SALES_HISTORY_ENDPOINTS.search}</code>、<code>{SALES_HISTORY_ENDPOINTS.save}</code>。
        搜索支持 productId / channelSource / sessionId / startTime / endTime。销售记录默认只做历史录入，不会自动回写商品库存；
        若后端开启 product.sales.auto-deduct-inventory，会在保存后扣减库存。
      </Alert>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          ['当前页记录', rows.length],
          ['当前页销售额', `¥${summary.amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`],
          ['当前页销量', summary.quantity],
          ['直播渠道记录', summary.live],
          ['选中商品累计销售额', selectedProductId == null ? '请选择商品' : totalAmountIsError ? '加载失败' : `¥${Number(totalSalesAmount ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`],
          ['选中商品累计销量', selectedProductId == null ? '请选择商品' : totalQuantityIsError ? '加载失败' : Number(totalSalesQuantity ?? 0).toLocaleString('zh-CN')],
        ].map(([label, value]) => (
          <Grid item xs={6} md={2} key={String(label)}>
            <Card
              variant="outlined"
              data-testid="sales-history-kpi-card"
              data-kpi-label={String(label)}
              data-contract-source={String(label).startsWith('选中商品累计') ? 'sales-history-total-endpoint' : 'local-derived-from-current-page'}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight={700}>{value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Box
          data-testid="sales-history-list-error"
          data-contract-source={SALES_HISTORY_ENDPOINTS.search}
          data-no-static-sales-fallback="true"
        >
          <ErrorAlert
            title="销售记录加载失败"
            message={`${getErrorMessage(error)}。请检查 ${SALES_HISTORY_ENDPOINTS.search}、登录态和商品数据权限。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}

      {productsIsError && (
        <Box
          data-testid="sales-history-products-error"
          data-contract-source={SALES_HISTORY_ENDPOINTS.products}
          data-no-local-product-fallback="true"
        >
          <ErrorAlert
            severity="warning"
            title="商品列表不可用"
            message={`${getErrorMessage(productsError)}。请检查 ${SALES_HISTORY_ENDPOINTS.products}；销售记录仍会显示 productId，但新增记录前需要商品列表。`}
            onRetry={() => refetchProducts()}
          />
        </Box>
      )}

      {saveMut.isError && (
        <Alert
          severity="error"
          data-testid="sales-history-save-error-page"
          data-contract-source={SALES_HISTORY_ENDPOINTS.save}
          data-dialog-input-preserved="true"
          sx={{ mb: 2 }}
        >
          保存失败：{getErrorMessage(saveMut.error)}。请检查 {SALES_HISTORY_ENDPOINTS.save}；录入弹窗和已填写内容已保留。
        </Alert>
      )}
      {(totalAmountIsError || totalQuantityIsError) && selectedProductId != null && (
        <Alert
          severity="warning"
          data-testid="sales-history-total-error"
          data-contract-source={`${totalAmountIsError ? SALES_HISTORY_ENDPOINTS.totalAmount : ''}|${totalQuantityIsError ? SALES_HISTORY_ENDPOINTS.totalQuantity : ''}`}
          data-selected-product-id={selectedProductId}
          data-no-dashboard-fallback="true"
          sx={{ mb: 2 }}
        >
          选中商品累计指标加载失败：
          {totalAmountIsError ? `${getErrorMessage(totalAmountError)}（${SALES_HISTORY_ENDPOINTS.totalAmount}）` : ''}
          {totalAmountIsError && totalQuantityIsError ? '；' : ''}
          {totalQuantityIsError ? `${getErrorMessage(totalQuantityError)}（${SALES_HISTORY_ENDPOINTS.totalQuantity}）` : ''}
        </Alert>
      )}

      <Box
        data-testid="sales-history-filter-panel"
        data-contract-source={SALES_HISTORY_ENDPOINTS.search}
        data-product-id={filters.productId || ''}
        data-channel={filters.channelSource || '全部'}
        data-session-id={filters.sessionId || ''}
        data-start-time={filters.startTime || ''}
        data-end-time={filters.endTime || ''}
        sx={{ mb: 2 }}
      >
        <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center" useFlexGap>
          <TextField
            select size="small" label="商品筛选" value={filters.productId}
            onChange={(e) => {
              setFilters((prev) => ({ ...prev, productId: e.target.value === '' ? '' : Number(e.target.value) }))
              setPage(0)
            }}
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="">全部商品</MenuItem>
            {productList.map((p) => (
              <MenuItem key={p.id} value={p.id}>{String(p.productName ?? p.id)}</MenuItem>
            ))}
          </TextField>
          <TextField
            select size="small" label="渠道筛选" value={filters.channelSource}
            onChange={(e) => { setFilters((prev) => ({ ...prev, channelSource: e.target.value })); setPage(0) }}
            sx={{ minWidth: 140 }}
          >
            <MenuItem value="">全部渠道</MenuItem>
            {CHANNEL_OPTIONS.map((option) => (
              <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
            ))}
          </TextField>
          <TextField
            size="small"
            label="场次 ID"
            value={filters.sessionId}
            onChange={(e) => { setFilters((prev) => ({ ...prev, sessionId: e.target.value })); setPage(0) }}
            sx={{ width: 150 }}
          />
          <TextField
            size="small"
            label="开始时间"
            type="datetime-local"
            InputLabelProps={{ shrink: true }}
            value={filters.startTime}
            onChange={(e) => { setFilters((prev) => ({ ...prev, startTime: e.target.value })); setPage(0) }}
            sx={{ width: 190 }}
          />
          <TextField
            size="small"
            label="结束时间"
            type="datetime-local"
            InputLabelProps={{ shrink: true }}
            value={filters.endTime}
            onChange={(e) => { setFilters((prev) => ({ ...prev, endTime: e.target.value })); setPage(0) }}
            sx={{ width: 190 }}
          />
          <Button variant="outlined" size="small" onClick={resetFilters}>清空筛选</Button>
        </Stack>
        {selectedProduct && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            当前累计指标来自选中商品：{selectedProduct.productName}。
          </Typography>
        )}
      </Box>

      <Box sx={{ height: 560 }}>
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={total}
          paginationMode="server"
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
          pageSizeOptions={[10, 20, 50]}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          slotProps={undefined}
          showExport={false}
          disableRowSelectionOnClick
          getRowId={(r) => (r as SalesHistory).id ?? 0}
        />
      </Box>

      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'sales-history-save-dialog',
          'data-contract-source': SALES_HISTORY_ENDPOINTS.save,
          'data-product-id': form.productId || '',
          'data-channel': form.channelSource || '',
        }}
      >
        <DialogTitle>录入销售记录</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          {saveMut.isError && (
            <Alert
              severity="error"
              data-testid="sales-history-save-error-dialog"
              data-contract-source={SALES_HISTORY_ENDPOINTS.save}
              data-input-preserved="true"
            >
              保存失败：{getErrorMessage(saveMut.error)}。请检查 {SALES_HISTORY_ENDPOINTS.save}，当前输入不会被清空。
            </Alert>
          )}
          <TextField
            select label="商品" value={form.productId}
            onChange={(e) => setForm((f) => ({ ...f, productId: Number(e.target.value) }))}
            size="small" fullWidth
          >
            {productList.map((p) => (
              <MenuItem key={p.id} value={p.id}>{String(p.productName ?? p.id)}</MenuItem>
            ))}
          </TextField>
          <TextField label="销售金额" size="small" fullWidth value={form.saleAmount}
            onChange={(e) => setForm((f) => ({ ...f, saleAmount: e.target.value }))} />
          <TextField label="数量" size="small" fullWidth value={form.saleQuantity}
            onChange={(e) => setForm((f) => ({ ...f, saleQuantity: e.target.value }))} />
          <TextField select label="渠道" size="small" fullWidth value={form.channelSource}
            onChange={(e) => setForm((f) => ({ ...f, channelSource: e.target.value }))}>
            {CHANNEL_OPTIONS.map((o) => (
              <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
            ))}
          </TextField>
          <TextField label="销售时间" size="small" fullWidth type="datetime-local"
            InputLabelProps={{ shrink: true }} value={form.saleTime}
            onChange={(e) => setForm((f) => ({ ...f, saleTime: e.target.value }))} />
          <TextField label="场次 ID（可选）" size="small" fullWidth value={form.sessionId}
            onChange={(e) => setForm((f) => ({ ...f, sessionId: e.target.value }))} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>取消</Button>
          <Button
            variant="contained"
            data-testid="sales-history-save-action"
            data-contract-source={SALES_HISTORY_ENDPOINTS.save}
            onClick={handleSave}
            disabled={saveMut.isPending}
          >
            保存
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
