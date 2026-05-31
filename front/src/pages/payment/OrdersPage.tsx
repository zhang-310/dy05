import { useState } from 'react'
import {
  Box, TextField, Button, Chip, MenuItem, Select, FormControl, InputLabel,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions,
  Typography, Grid, Paper, Alert, Stack,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader, ConfirmDialog, DataGridEmptyOverlay } from '@/components/base'
import { paymentApi, type PaymentOrder, type PaymentOrderStatus } from '@/api/payment'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

const STATUS_MAP: Record<PaymentOrderStatus, { label: string; color: 'default' | 'warning' | 'success' | 'error' | 'info' }> = {
  PENDING_PAYMENT: { label: '待支付', color: 'warning' },
  PAID: { label: '已支付', color: 'info' },
  SHIPPED: { label: '已发货', color: 'info' },
  COMPLETED: { label: '已完成', color: 'success' },
  CANCELLED: { label: '已取消', color: 'default' },
  REFUNDED: { label: '已退款', color: 'error' },
}

const ORDER_READY_ENDPOINTS = '/payment/order/list,/payment/order/getByOrderNo,/payment/order/confirmPayment,/payment/order/ship,/payment/order/complete,/payment/order/cancel'
const ORDER_UNSUPPORTED_ACTIONS = 'payment-method-filter,order-export,invoice-apply'
const ORDER_CONTRACT_DOWNGRADE_MESSAGE = '订单导出、发票申请和支付方式筛选尚未落库；页面只保留真实订单列表、订单号精确检索和状态机操作。确认支付必须输入后端可追溯交易凭证，前端不会自动拼接 manual-* 交易号。'

function OrderCapabilityCards() {
  const items = [
    { label: '订单列表', status: 'ready', action: 'order-list', endpoint: '/payment/order/list', detail: '/payment/order/list' },
    { label: '状态变更', status: 'ready', action: 'order-state-machine', endpoint: '/payment/order/confirmPayment,/payment/order/ship,/payment/order/complete,/payment/order/cancel', detail: '支付凭证确认/发货/完成/取消' },
    { label: '订单号检索', status: 'ready', action: 'order-no-search', endpoint: '/payment/order/getByOrderNo', detail: '/payment/order/getByOrderNo' },
    { label: '支付方式筛选', status: 'degraded', action: 'payment-method-filter', endpoint: '/payment/order/list', detail: 'OrderSearchVO 未提供 paymentMethod/payType' },
    { label: '导出/发票', status: 'unsupported', action: 'order-export,invoice-apply', endpoint: 'unavailable', detail: 'exportOrders、invoiceApply 未落库' },
  ] as const
  return (
    <Grid container spacing={2}>
      {items.map(item => (
        <Grid item xs={6} md={3} key={item.label}>
          <Paper
            variant="outlined"
            data-testid="payment-order-capability-card"
            data-contract-status={item.status}
            data-contract-action={item.action}
            data-contract-endpoint={item.endpoint}
            sx={{ p: 1.5, height: '100%', borderColor: item.status === 'ready' ? 'success.light' : 'warning.light' }}
          >
            <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
              {item.status === 'ready'
                ? <CheckCircleIcon sx={{ fontSize: 17, color: 'success.main' }} />
                : <WarningAmberIcon sx={{ fontSize: 17, color: 'warning.main' }} />}
              <Typography variant="subtitle2" fontWeight={700}>{item.label}</Typography>
            </Stack>
            <Chip size="small" label={item.status === 'ready' ? '已接入' : '显式降级'} color={item.status === 'ready' ? 'success' : 'warning'} variant="outlined" />
            <Typography variant="caption" color="text.secondary" display="block" mt={0.75}>{item.detail}</Typography>
          </Paper>
        </Grid>
      ))}
    </Grid>
  )
}

export default function OrdersPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState<{ page: number; rows: number; status: PaymentOrderStatus | ''; orderNo: string }>({ page: 0, rows: 20, status: '', orderNo: '' })
  const [query, setQuery] = useState(search)
  const [shipDialog, setShipDialog] = useState<{ open: boolean; id: number | null; orderNo?: string }>({ open: false, id: null })
  const [confirmAction, setConfirmAction] = useState<{ type: 'confirm' | 'complete' | 'cancel'; id: number; orderNo?: string } | null>(null)
  const [trackingNo, setTrackingNo] = useState('')
  const [paymentProof, setPaymentProof] = useState({ transactionId: '', paymentMethod: 'manual' })
  const [actionError, setActionError] = useState<string | null>(null)
  const [shipError, setShipError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['payment-orders', search],
    queryFn: async () => {
      const orderNo = search.orderNo.trim()
      if (!orderNo) {
        return paymentApi.list({ page: search.page, rows: search.rows, status: search.status || undefined })
      }
      const row = await paymentApi.getByOrderNo(orderNo)
      const matchedByStatus = !search.status || row.status === search.status
      const list: PaymentOrder[] = matchedByStatus ? [row] : []
      return {
        total: list.length,
        list,
        pageNum: 0,
        pageSize: search.rows,
      }
    },
  })
  const rows = data?.list ?? []
  const trimmedOrderNo = search.orderNo.trim()
  const activeEndpoint = trimmedOrderNo ? '/payment/order/getByOrderNo' : '/payment/order/list'
  const pendingPay = rows.filter(row => row.status === 'PENDING_PAYMENT').length
  const paidPendingShip = rows.filter(row => row.status === 'PAID').length
  const shippedPendingComplete = rows.filter(row => row.status === 'SHIPPED').length
  const abnormalCount = rows.filter(row => row.status === 'CANCELLED' || row.status === 'REFUNDED').length
  const listErrorMessage = error instanceof Error ? error.message : `订单接口异常，请检查 ${activeEndpoint}。`
  const currentOrderContext = confirmAction
    ? `orderId=${confirmAction.id}，orderNo=${confirmAction.orderNo || '-'}`
    : 'orderId=-，orderNo=-'
  const currentPaymentContext = `${currentOrderContext}，transactionId=${paymentProof.transactionId || '-'}，paymentMethod=${paymentProof.paymentMethod || '-'}`
  const currentShipContext = `orderId=${shipDialog.id ?? '-'}，orderNo=${shipDialog.orderNo || '-'}，trackingNo=${trackingNo || '-'}`

  const confirmMut = useMutation({
    mutationFn: ({ id, transactionId, paymentMethod }: { id: number; transactionId: string; paymentMethod: string }) =>
      paymentApi.confirmPayment(id, { transactionId, paymentMethod }),
    onSuccess: () => {
      toast('已确认支付', 'success')
      setActionError(null)
      setConfirmAction(null)
      setPaymentProof({ transactionId: '', paymentMethod: 'manual' })
      qc.invalidateQueries({ queryKey: ['payment-orders'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`确认支付失败：${message}。endpoint=/payment/order/confirmPayment，${currentPaymentContext}。`)
      toast(message, 'error')
    },
  })
  const shipMut = useMutation({
    mutationFn: ({ id, trackingNo: tn }: { id: number; trackingNo: string }) => paymentApi.ship(id, tn),
    onSuccess: () => { toast('发货成功', 'success'); setShipError(null); setShipDialog({ open: false, id: null }); setTrackingNo(''); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e) => {
      const message = getErrorMessage(e)
      setShipError(`发货失败：${message}。endpoint=/payment/order/ship，${currentShipContext}；物流单号已保留。`)
      toast(message, 'error')
    },
  })
  const completeMut = useMutation({
    mutationFn: (id: number) => paymentApi.complete(id),
    onSuccess: () => { toast('已完成', 'success'); setActionError(null); setConfirmAction(null); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`完成订单失败：${message}。endpoint=/payment/order/complete，${currentOrderContext}。`)
      toast(message, 'error')
    },
  })
  const cancelMut = useMutation({
    mutationFn: (id: number) => paymentApi.cancel(id),
    onSuccess: () => { toast('已取消', 'success'); setActionError(null); setConfirmAction(null); qc.invalidateQueries({ queryKey: ['payment-orders'] }) },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`取消订单失败：${message}。endpoint=/payment/order/cancel，${currentOrderContext}。`)
      toast(message, 'error')
    },
  })
  const activeMutationPending = confirmMut.isPending || shipMut.isPending || completeMut.isPending || cancelMut.isPending

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'orderNo', headerName: '订单号', width: 200 },
    { field: 'amount', headerName: '金额', width: 100, valueFormatter: (value: number) => `¥${(value ?? 0).toFixed(2)}` },
    { field: 'payType', headerName: '支付方式', width: 110 },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const s = STATUS_MAP[value as PaymentOrderStatus] ?? { label: '未知', color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    { field: 'payTime', headerName: '支付时间', width: 160 },
    { field: 'trackingNo', headerName: '物流单号', width: 150 },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 220, sortable: false,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {row.status === 'PENDING_PAYMENT' && <Button size="small" onClick={() => { setActionError(null); setPaymentProof({ transactionId: '', paymentMethod: 'manual' }); setConfirmAction({ type: 'confirm', id: row.id, orderNo: row.orderNo }) }} disabled={confirmMut.isPending}>确认</Button>}
          {row.status === 'PAID' && <Button size="small" color="primary" onClick={() => { setShipError(null); setShipDialog({ open: true, id: row.id, orderNo: row.orderNo }); setTrackingNo('') }} disabled={shipMut.isPending}>发货</Button>}
          {row.status === 'SHIPPED' && <Button size="small" color="success" onClick={() => { setActionError(null); setConfirmAction({ type: 'complete', id: row.id, orderNo: row.orderNo }) }} disabled={completeMut.isPending}>完成</Button>}
          {(row.status === 'PENDING_PAYMENT' || row.status === 'PAID') && <Button size="small" color="error" onClick={() => { setActionError(null); setConfirmAction({ type: 'cancel', id: row.id, orderNo: row.orderNo }) }} disabled={cancelMut.isPending}>取消</Button>}
        </Box>
      ),
    },
  ]

  const searchSlot = (
    <>
      <FormControl size="small" sx={{ minWidth: 110 }}>
        <InputLabel>状态</InputLabel>
        <Select label="状态" value={query.status} onChange={e => setQuery(q => ({ ...q, status: e.target.value as PaymentOrderStatus | '' }))}>
          <MenuItem value="">全部</MenuItem>
          {Object.entries(STATUS_MAP).map(([k, v]) => <MenuItem key={k} value={k}>{v.label}</MenuItem>)}
        </Select>
      </FormControl>
      <TextField
        size="small"
        label="订单号"
        placeholder="输入订单号精确检索"
        value={query.orderNo}
        onChange={e => setQuery(q => ({ ...q, orderNo: e.target.value }))}
        sx={{ minWidth: 220 }}
      />
      <Button variant="contained" onClick={() => setSearch({ ...query, orderNo: query.orderNo.trim(), page: 0 })}>查询</Button>
      <Button onClick={() => { const d = { page: 0, rows: 20, status: '' as const, orderNo: '' }; setQuery(d); setSearch(d) }}>重置</Button>
    </>
  )

  const confirmActionCopy = {
    confirm: { title: '确认支付', content: '请输入支付平台交易号或人工收款凭证后再确认。该动作会影响后续发货和账务状态。endpoint=/payment/order/confirmPayment' },
    complete: { title: '确认完成订单', content: '确定要将该订单标记为已完成吗？完成后将进入售后/发票阶段。endpoint=/payment/order/complete' },
    cancel: { title: '取消订单', content: '确定要取消该订单吗？取消后该订单不能继续发货，必要时需重新下单。endpoint=/payment/order/cancel' },
  } as const

  const handleConfirmAction = () => {
    if (!confirmAction) return
    if (confirmAction.type === 'confirm') {
      const transactionId = paymentProof.transactionId.trim()
      const paymentMethod = paymentProof.paymentMethod.trim()
      if (!transactionId || !paymentMethod) {
        setActionError(`确认支付失败：必须填写交易凭证和支付方式。endpoint=/payment/order/confirmPayment，${currentPaymentContext}；前端不会生成 manual-* 交易号。`)
        return
      }
      confirmMut.mutate({ id: confirmAction.id, transactionId, paymentMethod })
    }
    if (confirmAction.type === 'complete') completeMut.mutate(confirmAction.id)
    if (confirmAction.type === 'cancel') cancelMut.mutate(confirmAction.id)
  }

  return (
    <Box
      data-testid="payment-order-workbench"
      data-contract-scope="payment-order"
      data-ready-endpoints={ORDER_READY_ENDPOINTS}
      data-unsupported-actions={ORDER_UNSUPPORTED_ACTIONS}
      data-no-synthetic-transaction-id="true"
      data-row-retained-on-action-error="true"
      sx={{ p: 2, height: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="订单管理"
        subtitle="支付、发货、完成、取消均走真实订单接口；状态变更前需要二次确认。"
        breadcrumbs={[{ label: '支付中心' }, { label: '订单管理' }]}
        actions={<Button variant="outlined" size="small" onClick={() => void refetch()} disabled={isFetching}>刷新</Button>}
      />

      <Grid container spacing={2}>
        {[
          { label: '待支付', value: pendingPay, hint: '需确认支付或取消' },
          { label: '待发货', value: paidPendingShip, hint: '已支付未发货' },
          { label: '待完成', value: shippedPendingComplete, hint: '已发货未完成' },
          { label: '异常/关闭', value: abnormalCount, hint: '取消或退款订单' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <OrderCapabilityCards />

      <Alert
        severity="info"
        data-testid="payment-order-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-scope="payment-order"
        data-ready-endpoints={ORDER_READY_ENDPOINTS}
        data-unsupported-actions={ORDER_UNSUPPORTED_ACTIONS}
        data-no-synthetic-transaction-id="true"
        data-no-local-order-fallback="true"
      >
        {ORDER_CONTRACT_DOWNGRADE_MESSAGE} 订单列表来自 `/payment/order/list`，订单号精确检索来自 `/payment/order/getByOrderNo`；确认支付、发货、完成、取消分别调用真实接口。
      </Alert>

      {isError && (
        <Alert severity="error" action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}>
          订单加载失败：{listErrorMessage} endpoint={activeEndpoint}；当前查询条件已保留。
        </Alert>
      )}

      {actionError && (
        <Alert severity="error" data-testid="payment-order-action-error" data-row-retained-on-action-error="true" data-no-synthetic-transaction-id="true">
          {actionError} 当前订单行不会被本地改写，请修复后重新操作。
        </Alert>
      )}

      {!isFetching && !isError && rows.length === 0 && (
        <Alert severity="warning">当前筛选条件下没有订单；如刚完成支付，请检查支付回调或订单号筛选条件。endpoint={activeEndpoint}</Alert>
      )}

      <Box sx={{ flex: 1, minHeight: 360 }}>
        <StandardDataGrid
          rows={rows} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: s.orderNo.trim() ? 0 : m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          showExport={false}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      {/* 发货对话框 */}
      <Dialog open={shipDialog.open} onClose={() => setShipDialog({ open: false, id: null })} maxWidth="xs" fullWidth>
        <DialogTitle>填写物流单号</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            请输入快递物流单号。endpoint=/payment/order/ship，orderId={shipDialog.id ?? '-'}，orderNo={shipDialog.orderNo || '-'}。
          </DialogContentText>
          {shipError && <Alert severity="error" sx={{ mb: 2 }}>{shipError}</Alert>}
          <TextField autoFocus label="物流单号" fullWidth value={trackingNo} onChange={e => setTrackingNo(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShipDialog({ open: false, id: null })}>取消</Button>
          <Button variant="contained" disabled={!trackingNo || shipMut.isPending} onClick={() => shipDialog.id !== null && shipMut.mutate({ id: shipDialog.id, trackingNo })}>确认发货</Button>
        </DialogActions>
      </Dialog>

      {confirmAction?.type === 'confirm' ? (
        <Dialog open onClose={() => setConfirmAction(null)} maxWidth="sm" fullWidth>
          <DialogTitle>{confirmActionCopy.confirm.title}</DialogTitle>
          <DialogContent
            data-testid="payment-confirm-proof-form"
            data-contract-endpoint="/payment/order/confirmPayment"
            data-no-synthetic-transaction-id="true"
          >
            <DialogContentText sx={{ mb: 2 }}>
              {confirmActionCopy.confirm.content}，orderId={confirmAction.id}，orderNo={confirmAction.orderNo || '-'}。
            </DialogContentText>
            <Stack spacing={2}>
              <TextField
                label="交易凭证"
                placeholder="支付平台 transactionId 或人工收款凭证"
                fullWidth
                required
                value={paymentProof.transactionId}
                onChange={e => setPaymentProof(prev => ({ ...prev, transactionId: e.target.value }))}
              />
              <TextField
                label="支付方式"
                placeholder="wechat/alipay/manual-bank-transfer"
                fullWidth
                required
                value={paymentProof.paymentMethod}
                onChange={e => setPaymentProof(prev => ({ ...prev, paymentMethod: e.target.value }))}
              />
              <Alert severity="warning">
                前端不会生成 manual-* 这类交易号；请填入已核验凭证，失败时当前订单行和输入会保留。
              </Alert>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfirmAction(null)} disabled={activeMutationPending}>取消</Button>
            <Button
              color="error"
              variant="contained"
              onClick={handleConfirmAction}
              disabled={activeMutationPending || !paymentProof.transactionId.trim() || !paymentProof.paymentMethod.trim()}
            >
              {activeMutationPending ? '处理中...' : '确认'}
            </Button>
          </DialogActions>
        </Dialog>
      ) : (
        <ConfirmDialog
          open={confirmAction !== null}
          title={confirmAction ? confirmActionCopy[confirmAction.type].title : '确认操作'}
          content={confirmAction ? `${confirmActionCopy[confirmAction.type].content}，orderId=${confirmAction.id}，orderNo=${confirmAction.orderNo || '-'}` : ''}
          onClose={() => setConfirmAction(null)}
          onConfirm={handleConfirmAction}
          loading={activeMutationPending}
        />
      )}
    </Box>
  )
}
