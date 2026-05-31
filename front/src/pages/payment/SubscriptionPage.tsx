import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  InputLabel,
  LinearProgress,
  MenuItem,
  Select,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import UpgradeIcon from '@mui/icons-material/Upgrade'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid, DataGridEmptyOverlay } from '@/components/base'
import { paymentApi, type PaymentOrder, type PaymentOrderStatus, type PaymentRefund } from '@/api/payment'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import {
  buildQuotaCards,
  getSubscriptionProgress,
  getUpgradeSummary,
  normalizePaymentPlans,
  type UsageQuotaResponse,
} from './subscriptionPageModel'

const ORDER_STATUS_MAP: Record<PaymentOrderStatus, { label: string; color: 'default' | 'warning' | 'success' | 'error' | 'info' }> = {
  PENDING_PAYMENT: { label: '待支付', color: 'warning' },
  PAID: { label: '已支付', color: 'info' },
  SHIPPED: { label: '已发货', color: 'info' },
  COMPLETED: { label: '已完成', color: 'success' },
  REFUNDED: { label: '已退款', color: 'error' },
  CANCELLED: { label: '已取消', color: 'default' },
}

const REFUND_SEARCH_ENDPOINT = '/payment/refund/search'
const REFUND_CREATE_ENDPOINT = '/payment/refund/create'
const PAYMENT_SUBSCRIPTION_READY_ENDPOINTS = [
  '/payment/subscription/current',
  '/payment/subscription/plans',
  '/payment/subscription/upgrade',
  '/payment/subscription/check-quota',
  '/payment/usage/quota',
  '/payment/order/list',
  '/payment/refund/create',
  '/payment/refund/search',
].join(',')
const PAYMENT_SUBSCRIPTION_UNSUPPORTED_ACTIONS = [
  'renew-cashier',
  'invoice-apply',
  'invoice-list',
  'invoice-download',
  'quota-alert-config',
  'order-export',
].join(',')
const REFUND_SEARCH_DOWNGRADE_MESSAGE =
  `${REFUND_SEARCH_ENDPOINT} 已接入真实退款分页；页面只展示接口真实返回的退款记录，不补本地退款单或审批状态。`

function CapabilityStateCards({ items }: { items: Array<{ label: string; status: 'ready' | 'degraded'; detail: string }> }) {
  return (
    <Grid container spacing={1.5} sx={{ mb: 2 }}>
      {items.map(item => (
        <Grid item xs={12} sm={6} md={3} key={item.label}>
          <Card
            variant="outlined"
            data-testid="payment-capability-card"
            data-contract-status={item.status === 'ready' ? 'ready' : 'degraded'}
            sx={{ height: '100%', borderColor: item.status === 'ready' ? 'success.light' : 'warning.light' }}
          >
            <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 0.75 }}>
                {item.status === 'ready'
                  ? <CheckCircleIcon sx={{ fontSize: 18, color: 'success.main' }} />
                  : <WarningAmberIcon sx={{ fontSize: 18, color: 'warning.main' }} />}
                <Typography variant="subtitle2" fontWeight={700}>{item.label}</Typography>
              </Stack>
              <Chip
                size="small"
                label={item.status === 'ready' ? '已接入' : '显式降级'}
                color={item.status === 'ready' ? 'success' : 'warning'}
                variant="outlined"
                sx={{ mb: 0.75 }}
              />
              <Typography variant="caption" color="text.secondary" display="block">{item.detail}</Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  )
}

function SubscriptionTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [upgradeDialogOpen, setUpgradeDialogOpen] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null)
  const [upgradeError, setUpgradeError] = useState<string | null>(null)

  const { data: sub, isLoading, isError, error, refetch: refetchSub } = useQuery({
    queryKey: ['subscription-current'],
    queryFn: () => paymentApi.subscriptionCurrent(),
  })
  const { data: plans, isError: plansIsError, error: plansError, refetch: refetchPlans } = useQuery({
    queryKey: ['subscription-plans'],
    queryFn: () => paymentApi.subscriptionPlans(),
  })

  const upgradeMut = useMutation({
    mutationFn: (planCode: string) => paymentApi.subscriptionUpgrade(planCode),
    onSuccess: () => {
      toast('升级成功', 'success')
      setUpgradeError(null)
      qc.invalidateQueries({ queryKey: ['subscription-current'] })
      setUpgradeDialogOpen(false)
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setUpgradeError(`${message}。endpoint=/payment/subscription/upgrade，selectedPlan=${selectedPlan || '-'}，currentPlan=${sub?.planCode ?? '-'}`)
      toast(message, 'error')
    },
  })

  const planList = normalizePaymentPlans(plans)
  const { totalDays, remainDays, remainPct } = getSubscriptionProgress(sub)
  const upgradeSummary = getUpgradeSummary(planList, sub?.planCode, selectedPlan, remainPct)
  const subErrorMessage = error instanceof Error ? error.message : '请检查 /payment/subscription/current。'
  const planErrorMessage = plansError instanceof Error ? plansError.message : '请检查 /payment/subscription/plans。'

  if (isLoading) return <LinearProgress />

  return (
    <Box
      data-testid="payment-subscription-plan-workbench"
      data-contract-scope="payment-subscription-plan"
      data-ready-endpoints="/payment/subscription/current,/payment/subscription/plans,/payment/subscription/upgrade"
      data-unsupported-actions="renew-cashier,invoice-apply,invoice-list,invoice-download"
      data-no-local-plan-fallback="true"
      data-row-retained-on-action-error="true"
    >
      {(isError || plansIsError) && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => { void refetchSub(); void refetchPlans() }}>重试</Button>}
        >
          {isError ? `订阅接口异常：${subErrorMessage}` : `套餐接口异常：${planErrorMessage}`}
        </Alert>
      )}

      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="payment-subscription-plan-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-local-plan-fallback="true"
      >
        当前订阅、升级和套餐详情走 `/payment/subscription/current|upgrade|plans`。续费、发票、账单下载和配额包购买后端接口尚未落库，本页仅展示明确降级说明。
      </Alert>

      <CapabilityStateCards
        items={[
          { label: '订阅读取', status: 'ready', detail: '/payment/subscription/current' },
          { label: '套餐升级', status: 'ready', detail: '/payment/subscription/upgrade' },
          { label: '续费收银台', status: 'degraded', detail: '后端未提供续费/差价结算端点' },
          { label: '发票/账单', status: 'degraded', detail: '申请、列表和下载接口未落库' },
        ]}
      />

      <Card variant="outlined" sx={{ mb: 3, overflow: 'hidden' }}>
        <CardContent sx={{ p: 0 }}>
          <Box
            data-testid="subscription-current-plan-surface"
            sx={(theme) => ({
              p: 3,
              bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
              borderBottom: '1px solid',
              borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.36 : 0.16),
            })}
          >
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1} mb={2}>
              <Typography variant="h5" fontWeight={700} color="primary.main">
                {sub?.planName ?? '暂无有效订阅'}
              </Typography>
              <Chip label={sub?.status === 'active' ? '已激活' : '未激活'} color={sub?.status === 'active' ? 'success' : 'default'} size="small" />
            </Stack>
            <Typography color="text.secondary" variant="body2" mb={1.5}>
              有效期：{formatDate(sub?.createTime)} ~ {formatDate(sub?.expireTime)}
            </Typography>
            <LinearProgress variant="determinate" value={remainPct} sx={{ height: 10, borderRadius: 5, mb: 1 }} />
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="caption" color="text.secondary">剩余 {remainDays} 天 / {totalDays} 天</Typography>
              <Typography variant="caption" fontWeight={600}>{Math.round(remainPct)}%</Typography>
            </Stack>
            <Stack direction="row" spacing={1.5} flexWrap="wrap" mt={2}>
              <Button variant="outlined" startIcon={<UpgradeIcon />} onClick={() => setUpgradeDialogOpen(true)}>升级套餐</Button>
            </Stack>
          </Box>
        </CardContent>
      </Card>

      {!planList.length && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          套餐详情为空。后端当前按 plan 返回单个套餐详情，SDK 会请求 free/pro/enterprise 三个真实 plan 并归一为对比列表。
        </Alert>
      )}

      <Typography variant="subtitle1" fontWeight={700} mb={2}>套餐选择与对比</Typography>
      <Grid container spacing={2}>
        {planList.map(plan => {
          const isCurrent = sub?.planCode === plan.planCode
          return (
            <Grid item xs={12} sm={6} md={4} key={plan.planCode}>
              <Card variant="outlined" sx={{ height: '100%', borderColor: isCurrent ? 'primary.main' : 'divider' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" mb={1}>
                    <Typography variant="h6" fontWeight={700}>{plan.planName}</Typography>
                    {isCurrent && <Chip label="当前" color="primary" size="small" />}
                  </Stack>
                  <Typography variant="h4" fontWeight={800} color="primary" sx={{ mb: 2 }}>
                    ¥{plan.price}<Typography component="span" variant="body2" color="text.secondary">/月</Typography>
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Stack spacing={1}>
                    {plan.features.map(feature => (
                      <Stack key={feature} direction="row" spacing={1} alignItems="center">
                        <CheckCircleIcon sx={{ fontSize: 18, color: 'success.main' }} />
                        <Typography variant="body2">{feature}</Typography>
                      </Stack>
                    ))}
                  </Stack>
                  <Button
                    fullWidth
                    variant={isCurrent ? 'outlined' : 'contained'}
                    disabled={isCurrent || upgradeMut.isPending}
                    sx={{ mt: 2 }}
                  onClick={() => { setUpgradeError(null); setSelectedPlan(plan.planCode); setUpgradeDialogOpen(true) }}
                  >
                    {isCurrent ? '当前套餐' : '升级到此套餐'}
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>

      <Dialog open={upgradeDialogOpen} onClose={() => setUpgradeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>确认升级套餐</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">
              后端 `/payment/subscription/upgrade` 当前只负责切换 plan 并重置订阅周期，未接入真实支付收银台和差价结算。
            </Alert>
            {upgradeError && (
              <Alert severity="error" data-testid="payment-subscription-upgrade-error" data-input-retained="true" data-row-retained-on-action-error="true">
                升级套餐失败：{upgradeError}。当前选择已保留。
              </Alert>
            )}
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">当前方案</Typography>
              <Typography variant="body2" fontWeight={600}>{sub?.planName ?? '-'}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">升级至</Typography>
              <Typography variant="body2" fontWeight={600}>{upgradeSummary.targetPlanName || '请选择套餐'}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">展示价格</Typography>
              <Typography variant="body2" fontWeight={600}>¥{upgradeSummary.targetPlanPrice.toFixed(2)}/月</Typography>
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUpgradeDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={() => selectedPlan && upgradeMut.mutate(selectedPlan)}
            disabled={upgradeMut.isPending || !selectedPlan}
          >
            {upgradeMut.isPending ? <CircularProgress size={20} /> : '确认升级'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

function QuotaTab() {
  const { data: quota, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['usage-quota'],
    queryFn: () => paymentApi.usageQuota(),
  })
  const q = quota as UsageQuotaResponse | undefined
  const quotas = buildQuotaCards(q)
  const errorMessage = error instanceof Error ? error.message : '请检查 /payment/subscription/check-quota。'

  if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>

  return (
    <Box
      data-testid="payment-subscription-quota-workbench"
      data-contract-scope="payment-subscription-quota"
      data-ready-endpoints="/payment/usage/quota,/payment/subscription/check-quota"
      data-degraded-endpoints="quota-alert-config"
      data-no-mock-quota-dimensions="true"
      data-no-quota-alert-config-call="true"
    >
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="payment-subscription-quota-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-mock-quota-dimensions="true"
      >
        `/payment/usage/quota` 已接入真实用量汇总；本页展示 AI、直播、短视频项目和存储额度，配额告警配置读取/保存仍显式降级。
      </Alert>
      <CapabilityStateCards
        items={[
          { label: 'AI 额度', status: 'ready', detail: '/payment/usage/quota + check-quota' },
          { label: '直播额度', status: 'ready', detail: '/payment/usage/quota liveSessions' },
          { label: '存储额度', status: 'ready', detail: '/payment/usage/quota storageMb' },
          { label: '配额预警', status: 'degraded', detail: '告警配置读取/保存未落库' },
        ]}
      />
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }} data-testid="payment-subscription-quota-error" data-no-mock-quota-dimensions="true" action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}>
          配额加载失败：{errorMessage}
        </Alert>
      )}
      <Grid container spacing={2} mb={3}>
        {quotas.map(item => {
          const pct = item.limit > 0 ? Math.min((item.used / item.limit) * 100, 100) : 0
          const color = pct > 90 ? 'error' : pct > 70 ? 'warning' : 'primary'
          return (
            <Grid item xs={12} sm={6} key={item.key}>
              <Card variant="outlined">
                <CardContent
                  data-testid="payment-subscription-quota-card"
                  data-quota-key={item.key}
                  data-contract-status={item.key === 'kb' ? 'degraded' : 'ready'}
                  data-contract-endpoint={item.key === 'kb' ? 'unavailable' : '/payment/usage/quota'}
                  data-no-mock-quota-dimensions={item.key === 'kb' ? 'true' : 'false'}
                >
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2" fontWeight={600}>{item.label}</Typography>
                    <Chip label={item.limit > 0 ? `${pct.toFixed(0)}%` : '未接入'} size="small" color={item.limit > 0 ? 'default' : 'warning'} variant="outlined" />
                  </Stack>
                  <Typography variant="h5" fontWeight={700} color={`${color}.main`}>
                    {item.used.toLocaleString()}
                    <Typography component="span" variant="body2" color="text.secondary"> / {item.limit > 0 ? item.limit.toLocaleString() : '未配置'} {item.unit}</Typography>
                  </Typography>
                  <LinearProgress variant="determinate" value={pct} color={color as 'primary' | 'warning' | 'error'} sx={{ mt: 1, height: 8, borderRadius: 4 }} />
                  {!item.limit && (
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                      {item.key === 'kb' ? '知识库文档额度尚无支付域用量指标，等待后端指标接入。' : '该维度未配置上限，当前仅展示真实消耗量。'}
                    </Typography>
                  )}
                  {item.prediction && <Alert severity="warning" sx={{ mt: 1, py: 0 }}>{item.prediction}</Alert>}
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>
      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" fontWeight={600} mb={1}>配额预警设置</Typography>
          <Typography variant="body2" color="text.secondary">
            配额预警读取和保存接口尚未接入，当前不提供可点击保存按钮，避免误导用户以为配置已落库。
          </Typography>
        </CardContent>
      </Card>
    </Box>
  )
}

function OrdersTab() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<PaymentOrderStatus | ''>('')
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['payment-orders-history', page, status],
    queryFn: () => paymentApi.list({ page, rows: 20, status: status || undefined }),
  })
  const errorMessage = error instanceof Error ? error.message : '请检查 /payment/order/list。'

  const columns: GridColDef<PaymentOrder>[] = [
    { field: 'orderNo', headerName: '订单号', width: 180 },
    { field: 'amount', headerName: '金额', width: 100, renderCell: ({ row }) => `¥${Number(row.amount ?? 0).toFixed(2)}` },
    {
      field: 'status',
      headerName: '状态',
      width: 100,
      renderCell: ({ row }) => {
        const statusMeta = ORDER_STATUS_MAP[row.status] ?? { label: '未知', color: 'default' as const }
        return <Chip label={statusMeta.label} color={statusMeta.color} size="small" />
      },
    },
    { field: 'payType', headerName: '支付方式', width: 120 },
    { field: 'createTime', headerName: '下单时间', width: 180, renderCell: ({ row }) => formatDate(row.createTime) },
    { field: 'trackingNo', headerName: '物流单号', width: 160 },
  ]

  return (
    <Box
      data-testid="payment-subscription-orders-workbench"
      data-contract-scope="payment-subscription-orders"
      data-ready-endpoints="/payment/order/list"
      data-unsupported-actions="order-export,invoice-apply"
      data-no-local-order-fallback="true"
    >
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="payment-subscription-orders-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-no-local-order-fallback="true"
      >
        订单历史复用真实 `/payment/order/list`；订单导出和发票申请接口尚未接入，已从可点击动作中移除。
      </Alert>
      <CapabilityStateCards
        items={[
          { label: '订单历史', status: 'ready', detail: '/payment/order/list' },
          { label: '状态筛选', status: 'ready', detail: 'status enum 入参' },
          { label: '订单导出', status: 'degraded', detail: 'exportOrders 未落库' },
          { label: '发票申请', status: 'degraded', detail: 'invoiceApply 未落库' },
        ]}
      />
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }} data-testid="payment-subscription-orders-error" data-no-local-order-fallback="true" action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}>
          订单历史加载失败：{errorMessage}
        </Alert>
      )}
      <Stack direction="row" spacing={2} mb={2} alignItems="center">
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>状态</InputLabel>
          <Select value={status} label="状态" onChange={e => setStatus(e.target.value as PaymentOrderStatus | '')}>
            <MenuItem value="">全部</MenuItem>
            {Object.entries(ORDER_STATUS_MAP).map(([key, value]) => <MenuItem key={key} value={key}>{value.label}</MenuItem>)}
          </Select>
        </FormControl>
      </Stack>
      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        loading={isLoading}
        getRowId={(row) => row.id}
        paginationMode="server"
        rowCount={data?.total ?? 0}
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={(model) => setPage(model.page)}
        autoHeight
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
    </Box>
  )
}

function RefundTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [refundDialogOpen, setRefundDialogOpen] = useState(false)
  const [refundForm, setRefundForm] = useState({ orderId: '', amount: '', reason: '' })
  const [refundPage, setRefundPage] = useState(0)
  const [refundError, setRefundError] = useState<string | null>(null)

  const { data: refundData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['payment-refunds', refundPage],
    queryFn: () => paymentApi.refundSearch({ page: refundPage, rows: 10 }),
  })
  const refundMut = useMutation({
    mutationFn: () => paymentApi.refundCreate({
      orderId: Number(refundForm.orderId),
      amount: Number(refundForm.amount),
      reason: refundForm.reason,
    }),
    onSuccess: () => {
      toast('退款申请已提交', 'success')
      setRefundError(null)
      qc.invalidateQueries({ queryKey: ['payment-refunds'] })
      setRefundDialogOpen(false)
      setRefundForm({ orderId: '', amount: '', reason: '' })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setRefundError(`${message}。endpoint=/payment/refund/create，orderId=${refundForm.orderId || '-'}，amount=${refundForm.amount || '-'}，reason=${refundForm.reason || '-'}`)
      toast(message, 'error')
    },
  })
  const errorMessage = error instanceof Error ? error.message : '请检查 /payment/refund/search。'

  const columns: GridColDef<PaymentRefund>[] = [
    { field: 'refundNo', headerName: '退款单号', width: 180 },
    { field: 'transactionNo', headerName: '交易单号', width: 160 },
    { field: 'amount', headerName: '退款金额', width: 120, renderCell: ({ row }) => `¥${Number(row.amount ?? 0).toFixed(2)}` },
    { field: 'status', headerName: '状态', width: 120, renderCell: ({ row }) => <Chip label={row.status || '待审核'} size="small" /> },
    { field: 'reason', headerName: '退款原因', flex: 1 },
    { field: 'createTime', headerName: '申请时间', width: 180, renderCell: ({ row }) => formatDate(row.createTime) },
  ]

  return (
    <Box
      data-testid="subscription-refund-workbench"
      data-contract-scope="payment-refund"
      data-refund-create-endpoint={REFUND_CREATE_ENDPOINT}
      data-refund-search-endpoint={REFUND_SEARCH_ENDPOINT}
      data-no-local-refund-fallback="true"
      data-row-retained-on-action-error="true"
      data-unsupported-actions="invoice-apply,invoice-list,invoice-download"
      data-no-refund-admin-actions="true"
    >
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="payment-refund-search-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="ready"
        data-contract-endpoint={REFUND_SEARCH_ENDPOINT}
        data-no-local-refund-fallback="true"
      >
        退款申请走 `{REFUND_CREATE_ENDPOINT}`；{REFUND_SEARCH_DOWNGRADE_MESSAGE} 退款审批/完成接口需要管理员权限，前端普通工作台不暴露。
      </Alert>
      <CapabilityStateCards
        items={[
          { label: '退款申请', status: 'ready', detail: REFUND_CREATE_ENDPOINT },
          { label: '退款列表', status: 'ready', detail: `${REFUND_SEARCH_ENDPOINT} 真实分页，仅展示真实返回` },
          { label: '退款审批', status: 'degraded', detail: '管理员 approve/reject 接口不在普通工作台暴露' },
          { label: '退款完成', status: 'degraded', detail: '管理员 complete 接口不在普通工作台暴露' },
        ]}
      />
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}>
          退款记录加载失败：{errorMessage}
        </Alert>
      )}
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="subtitle1" fontWeight={700}>售后退款</Typography>
        <Button variant="contained" onClick={() => { setRefundError(null); setRefundDialogOpen(true) }}>申请退款</Button>
      </Stack>
      <Box
        data-testid="payment-refund-search-grid"
        data-contract-status="ready"
        data-contract-endpoint={REFUND_SEARCH_ENDPOINT}
        data-refund-row-count={refundData?.list?.length ?? 0}
      >
        <StandardDataGrid
          rows={refundData?.list ?? []}
          columns={columns}
          loading={isLoading}
          rowCount={refundData?.total ?? 0}
          paginationMode="server"
          paginationModel={{ page: refundPage, pageSize: 10 }}
          onPaginationModelChange={(model) => setRefundPage(model.page)}
          autoHeight
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>
      <Grid container spacing={2} sx={{ mt: 2 }}>
        <Grid item xs={12} md={6}>
          <Alert severity="info" icon={<CheckCircleIcon />}>
            <Typography variant="subtitle2" fontWeight={700}>退款时效说明</Typography>
            <Typography variant="caption" display="block">退款提交后进入后端审核流；审批、拒绝、完成均由管理员接口处理。</Typography>
          </Alert>
        </Grid>
        <Grid item xs={12} md={6}>
          <Alert severity="warning" icon={<WarningAmberIcon />}>
            <Typography variant="subtitle2" fontWeight={700}>发票能力降级</Typography>
            <Typography variant="caption" display="block">发票申请、列表和下载接口尚未落库，本批不再展示可点击发票按钮。</Typography>
          </Alert>
        </Grid>
      </Grid>

      <Dialog open={refundDialogOpen} onClose={() => setRefundDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>申请售后退款</DialogTitle>
        <DialogContent data-testid="payment-refund-create-form" data-contract-status="ready" data-contract-endpoint={REFUND_CREATE_ENDPOINT}>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            {refundError && (
              <Alert severity="error" data-testid="payment-refund-create-error" data-input-retained="true">
                退款申请失败：{refundError}。当前申请内容已保留。
              </Alert>
            )}
            <TextField
              label="订单 ID"
              type="number"
              fullWidth
              required
              value={refundForm.orderId}
              onChange={(e) => setRefundForm({ ...refundForm, orderId: e.target.value })}
            />
            <TextField
              label="申请退款金额"
              type="number"
              fullWidth
              required
              value={refundForm.amount}
              onChange={(e) => setRefundForm({ ...refundForm, amount: e.target.value })}
            />
            <TextField
              label="退款原因"
              fullWidth
              required
              multiline
              minRows={2}
              value={refundForm.reason}
              onChange={(e) => setRefundForm({ ...refundForm, reason: e.target.value })}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRefundDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={() => refundMut.mutate()}
            disabled={refundMut.isPending || !refundForm.orderId || !refundForm.amount || !refundForm.reason}
          >
            {refundMut.isPending ? <CircularProgress size={20} /> : '提交申请'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default function SubscriptionPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box
      data-testid="payment-subscription-workbench"
      data-contract-scope="payment-subscription"
      data-ready-endpoints={PAYMENT_SUBSCRIPTION_READY_ENDPOINTS}
      data-unsupported-actions={PAYMENT_SUBSCRIPTION_UNSUPPORTED_ACTIONS}
      data-no-local-subscription-fallback="true"
      data-no-mock-quota-dimensions="true"
      data-no-refund-approval-actions="true"
    >
      <PageHeader
        title="订阅与配额管理"
        breadcrumbs={[{ label: '支付中心' }, { label: '订阅管理' }]}
        subtitle="管理套餐订阅、AI 配额、订单历史和退款；未落库的发票/导出能力已显式降级。"
        actions={<Button size="small" variant="outlined" onClick={() => setTab(1)}>查看配额</Button>}
      />
      <Tabs value={tab} onChange={(_e, value) => setTab(value)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tab label="当前订阅" />
        <Tab label="配额监控" />
        <Tab label="订单历史" />
        <Tab label="售后中心" />
      </Tabs>
      {tab === 0 && <SubscriptionTab />}
      {tab === 1 && <QuotaTab />}
      {tab === 2 && <OrdersTab />}
      {tab === 3 && <RefundTab />}
    </Box>
  )
}
