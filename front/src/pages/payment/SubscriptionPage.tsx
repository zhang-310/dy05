import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Stack, Button, Chip, Divider,
  LinearProgress, Tabs, Tab, Grid, Alert, Dialog, DialogTitle,
  DialogContent, DialogActions, TextField, MenuItem, Select,
  FormControl, InputLabel, CircularProgress,
} from '@mui/material'
import UpgradeIcon from '@mui/icons-material/Upgrade'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { paymentApi, type PaymentOrder, type PaymentRefund, type PaymentInvoice } from '@/api/payment'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader } from '@/components/base'
import { formatDate } from '@/utils/date'
import {
  buildQuotaCards,
  getAiQuotaPrediction,
  getSubscriptionProgress,
  getUpgradeSummary,
  normalizePaymentPlans,
  type UsageQuotaResponse,
} from './subscriptionPageModel'

// ===== Tab 1: 订阅管理 =====
function SubscriptionTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [upgradeDialogOpen, setUpgradeDialogOpen] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null)
  const [invoiceDialogOpen, setInvoiceDialogOpen] = useState(false)
  const [invoiceOrderId, setInvoiceOrderId] = useState<number | null>(null)
  const [invoiceForm, setInvoiceForm] = useState({ type: 'personal', title: '', taxNo: '', email: '' })

  const { data: sub, isLoading } = useQuery({
    queryKey: ['subscription-current'],
    queryFn: () => paymentApi.subscriptionCurrent(),
  })
  const { data: plans } = useQuery({
    queryKey: ['subscription-plans'],
    queryFn: () => paymentApi.subscriptionPlans(),
  })
  const { data: quota } = useQuery({
    queryKey: ['usage-quota'],
    queryFn: () => paymentApi.usageQuota(),
  })

  const upgradeMut = useMutation({
    mutationFn: (planCode: string) => paymentApi.subscriptionUpgrade(planCode),
    onSuccess: () => { toast('升级成功', 'success'); qc.invalidateQueries({ queryKey: ['subscription-current'] }); setUpgradeDialogOpen(false) },
    onError: () => toast('升级失败', 'error'),
  })

  const invoiceMut = useMutation({
    mutationFn: () => paymentApi.invoiceApply(invoiceOrderId!, invoiceForm),
    onSuccess: () => { toast('发票申请已提交', 'success'); setInvoiceDialogOpen(false) },
    onError: () => toast('申请失败', 'error'),
  })

  const quotaData = quota as UsageQuotaResponse | undefined
  const planList = normalizePaymentPlans(plans)
  const { totalDays, remainDays, remainPct } = getSubscriptionProgress(sub)
  const aiPrediction = getAiQuotaPrediction(quotaData)
  const upgradeSummary = getUpgradeSummary(planList, sub?.planCode, selectedPlan, remainPct)

  if (isLoading) return <LinearProgress />
  return (
    <Box>
      {/* 当前套餐卡片 v3.0 */}
      <Card sx={{ mb: 4, border: '1px solid', borderColor: 'primary.200', borderRadius: 2, overflow: 'hidden' }}>
        <CardContent sx={{ p: 0 }}>
          <Box sx={{ p: 3, bgcolor: 'primary.50' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h5" fontWeight={700} color="primary.main">
                🏆 {sub?.planName || '基础版'}
              </Typography>
              <Chip label="已激活" color="success" size="small" sx={{ fontWeight: 700 }} />
            </Stack>
            <Typography color="text.secondary" variant="body2" mb={1.5}>
              有效期: {formatDate(sub?.createTime)} ~ {formatDate(sub?.expireTime)}
            </Typography>
            <Box sx={{ mb: 2.5 }}>
              <LinearProgress variant="determinate" value={remainPct} sx={{ height: 10, borderRadius: 5, mb: 1, bgcolor: 'rgba(0,0,0,0.05)' }} />
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="caption" color="text.secondary">剩余 {remainDays} 天 / {totalDays} 天</Typography>
                <Typography variant="caption" fontWeight={600} color="primary">{Math.round(remainPct)}%</Typography>
              </Stack>
            </Box>
            <Stack direction="row" spacing={2}>
              <Button variant="contained" disableElevation sx={{ px: 3 }}>续费</Button>
              <Button variant="outlined" startIcon={<UpgradeIcon />} onClick={() => setUpgradeDialogOpen(true)}>立即升级 →</Button>
              <Button variant="text" size="small" color="inherit" onClick={() => { setInvoiceOrderId(sub?.id ?? null); setInvoiceDialogOpen(true) }}>开发票</Button>
              <Button variant="text" size="small" color="inherit">下载账单</Button>
            </Stack>
          </Box>

          {aiPrediction?.showWarning && (
            <Box sx={{ p: 2, bgcolor: '#fff7ed', borderTop: '1px solid', borderColor: '#fed7aa' }}>
              <Stack direction="row" spacing={1.5} alignItems="flex-start">
                <Typography sx={{ fontSize: 20 }}>⚡</Typography>
                <Box sx={{ flex: 1 }}>
                  <Typography variant="body2" fontWeight={700} color="#9a3412">
                    AI 配额预测：按当前用量，预计 {aiPrediction.daysLeft} 天后耗尽（{aiPrediction.dateStr}）
                  </Typography>
                  <Typography variant="caption" color="#c2410c" display="block" sx={{ mb: 1 }}>
                    建议：升级至企业版 Ultra 或购买配额包，以免影响自动化直播业务
                  </Typography>
                  <Stack direction="row" spacing={2}>
                    <Button size="small" variant="text" sx={{ color: '#ea580c', p: 0, minWidth: 0, fontWeight: 700 }}>查看配额详情</Button>
                    <Button size="small" variant="text" sx={{ color: '#ea580c', p: 0, minWidth: 0, fontWeight: 700 }}>立即购买配额包</Button>
                  </Stack>
                </Box>
              </Stack>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* 套餐对比 v3.0 */}
      <Typography variant="subtitle1" fontWeight={700} mb={2}>套餐选择与对比</Typography>
      <Grid container spacing={3}>
        {planList.map((p) => {
          const isCurrent = sub?.planCode === p.planCode
          return (
            <Grid item xs={12} sm={6} md={4} key={p.planCode || p.planName}>
              <Card
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: '0.3s',
                  position: 'relative',
                  border: isCurrent ? '2px solid' : '1px solid',
                  borderColor: isCurrent ? 'primary.main' : 'divider',
                  '&:hover': { transform: 'translateY(-8px)', boxShadow: 6 }
                }}
              >
                {isCurrent && (
                  <Chip
                    label="当前使用中"
                    color="primary"
                    size="small"
                    sx={{ position: 'absolute', top: 12, right: 12, fontWeight: 700 }}
                  />
                )}
                <CardContent sx={{ flexGrow: 1, p: 3 }}>
                  <Typography variant="h6" fontWeight={700} color="text.primary" gutterBottom>
                    {p.planName}
                  </Typography>
                  <Typography variant="h4" fontWeight={800} color="primary" sx={{ my: 2 }}>
                    ¥{p.price} <Typography component="span" variant="body2" color="text.secondary">/年</Typography>
                  </Typography>
                  <Divider sx={{ my: 2 }} />
                  <Stack spacing={1.5}>
                    {p.features.map((f, i) => (
                      <Stack key={i} direction="row" spacing={1} alignItems="center">
                        <CheckCircleIcon sx={{ fontSize: 18, color: 'success.main' }} />
                        <Typography variant="body2">{f}</Typography>
                      </Stack>
                    ))}
                  </Stack>
                </CardContent>
                <Box sx={{ p: 2, bgcolor: 'grey.50' }}>
                  <Button
                    fullWidth
                    variant={isCurrent ? 'outlined' : 'contained'}
                    disabled={isCurrent || upgradeMut.isPending}
                    onClick={() => { setSelectedPlan(p.planCode); setUpgradeDialogOpen(true) }}
                  >
                    {isCurrent ? '已拥有' : '立即升级'}
                  </Button>
                </Box>
              </Card>
            </Grid>
          );
        })}
      </Grid>


      {/* Upgrade Dialog v3.0 */}
      <Dialog open={upgradeDialogOpen} onClose={() => setUpgradeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>确认升级套餐</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <Box sx={{ p: 2, bgcolor: 'primary.50', borderRadius: 2, border: '1px solid', borderColor: 'primary.100' }}>
              <Grid container spacing={2}>
                <Grid item xs={5}>
                  <Typography variant="caption" color="text.secondary" display="block">当前方案</Typography>
                  <Typography variant="subtitle1" fontWeight={700}>{sub?.planName || '基础版'}</Typography>
                </Grid>
                <Grid item xs={2} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <UpgradeIcon color="primary" />
                </Grid>
                <Grid item xs={5} sx={{ textAlign: 'right' }}>
                  <Typography variant="caption" color="text.secondary" display="block">升级至</Typography>
                  <Typography variant="subtitle1" fontWeight={700} color="primary">
                    {upgradeSummary.targetPlanName}
                  </Typography>
                </Grid>
              </Grid>
            </Box>

            <Stack spacing={1}>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="body2" color="text.secondary">剩余有效期折算</Typography>
                <Typography variant="body2" fontWeight={600}>抵扣 ¥{upgradeSummary.creditAmount.toFixed(2)}</Typography>
              </Stack>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="body2" color="text.secondary">新套餐年费</Typography>
                <Typography variant="body2" fontWeight={600}>¥{upgradeSummary.targetPlanPrice.toFixed(2)}</Typography>
              </Stack>
              <Divider />
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="subtitle1" fontWeight={700}>应付总额</Typography>
                <Typography variant="h5" fontWeight={800} color="primary">
                  ¥{upgradeSummary.payableAmount.toFixed(2)}
                </Typography>
              </Stack>
            </Stack>

            <Alert severity="info" icon={<CheckCircleIcon fontSize="small" />} sx={{ borderRadius: 2 }}>
              升级后 AI 配额将立即重置为新套餐上限，且有效期将顺延一年。
            </Alert>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button onClick={() => setUpgradeDialogOpen(false)} color="inherit">我再想想</Button>
          <Button
            variant="contained"
            size="large"
            onClick={() => upgradeMut.mutate(selectedPlan!)}
            disabled={upgradeMut.isPending}
            sx={{ px: 4, fontWeight: 700, borderRadius: 2 }}
          >
            {upgradeMut.isPending ? <CircularProgress size={24} color="inherit" /> : '立即支付并升级'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Invoice Dialog v3.0 */}
      <Dialog open={invoiceDialogOpen} onClose={() => setInvoiceDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>申请电子增值税发票</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <Alert severity="info" sx={{ borderRadius: 2 }}>
              申请通过后，电子发票将发送至您的邮箱，您也可以在“退款管理-发票记录”中下载。
            </Alert>

            <Grid container spacing={2}>
              <Grid item xs={12}>
                <FormControl fullWidth size="small">
                  <InputLabel>发票类型</InputLabel>
                  <Select
                    value={invoiceForm.type}
                    label="发票类型"
                    onChange={e => setInvoiceForm(prev => ({ ...prev, type: e.target.value }))}
                  >
                    <MenuItem value="personal">个人 / 非企业单位</MenuItem>
                    <MenuItem value="company">企业单位</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  label="发票抬头"
                  fullWidth
                  size="small"
                  placeholder="请输入准确的单位全称或个人姓名"
                  value={invoiceForm.title}
                  onChange={e => setInvoiceForm(prev => ({ ...prev, title: e.target.value }))}
                />
              </Grid>

              {invoiceForm.type === 'company' && (
                <Grid item xs={12}>
                  <TextField
                    label="纳税人识别号"
                    fullWidth
                    size="small"
                    placeholder="15-20位统一社会信用代码"
                    value={invoiceForm.taxNo}
                    onChange={e => setInvoiceForm(prev => ({ ...prev, taxNo: e.target.value }))}
                  />
                </Grid>
              )}

              <Grid item xs={12}>
                <TextField
                  label="接收邮箱"
                  fullWidth
                  size="small"
                  placeholder="example@domain.com"
                  value={invoiceForm.email}
                  onChange={e => setInvoiceForm(prev => ({ ...prev, email: e.target.value }))}
                />
              </Grid>
            </Grid>

            <Typography variant="caption" color="text.secondary">
              * 发票将在审核通过后发送至您的邮箱
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button onClick={() => setInvoiceDialogOpen(false)} color="inherit">取消</Button>
          <Button
            variant="contained"
            onClick={() => invoiceMut.mutate()}
            disabled={invoiceMut.isPending || !invoiceForm.title || !invoiceForm.email}
            sx={{ px: 4, fontWeight: 700 }}
          >
            {invoiceMut.isPending ? <CircularProgress size={24} color="inherit" /> : '提交申请'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
// ===== Tab 2: 配额详情 =====
function QuotaTab() {
  const { data: quota, isLoading } = useQuery({
    queryKey: ['usage-quota'],
    queryFn: () => paymentApi.usageQuota(),
  })
  const { data: alertConfig } = useQuery({
    queryKey: ['quota-alert-config'],
    queryFn: () => paymentApi.quotaAlertConfigGet(),
  })
  const toast = useToast()
  const qc = useQueryClient()
  const [alertThreshold, setAlertThreshold] = useState(80)

  const saveAlertMut = useMutation({
    mutationFn: () => paymentApi.quotaAlertConfigSave({ threshold: alertThreshold }),
    onSuccess: () => { toast('预警配置已保存', 'success'); qc.invalidateQueries({ queryKey: ['quota-alert-config'] }) },
    onError: () => toast('保存失败', 'error'),
  })

  const q = quota as UsageQuotaResponse | undefined
  void alertConfig
  const quotas = buildQuotaCards(q)

  if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>

  return (
    <Box>
      <Grid container spacing={2} mb={3}>
        {quotas.map(item => {
          const pct = item.limit > 0 ? Math.min((item.used / item.limit) * 100, 100) : 0
          const color = pct > 90 ? 'error' : pct > 70 ? 'warning' : 'primary'
          return (
            <Grid item xs={12} sm={6} key={item.label}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle2" fontWeight={600}>{item.label}</Typography>
                    <Chip label={`${pct.toFixed(0)}%`} size="small"
                      color={pct > 90 ? 'error' : pct > 70 ? 'warning' : 'default'} variant="outlined" />
                  </Stack>
                  <Typography variant="h5" fontWeight={700} color={`${color}.main`}>
                    {item.used.toLocaleString()}
                    <Typography component="span" variant="body2" color="text.secondary"> / {item.limit.toLocaleString()} {item.unit}</Typography>
                  </Typography>
                  <LinearProgress variant="determinate" value={pct}
                    color={color as 'primary' | 'warning' | 'error'}
                    sx={{ mt: 1, height: 8, borderRadius: 4 }} />
                  {(pct > 80 || item.prediction) && (
                    <Alert severity={pct > 90 ? 'error' : 'warning'} sx={{ mt: 1, py: 0 }}>
                      {item.prediction || (pct > 90 ? '配额即将耗尽，请尽快升级' : '配额使用超过 80%，建议关注')}
                    </Alert>
                  )}
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" fontWeight={600} mb={2}>配额预警设置</Typography>
          <Stack direction="row" spacing={2} alignItems="center">
            <Typography variant="body2">预警阈值：</Typography>
            <Select size="small" value={alertThreshold} onChange={e => setAlertThreshold(Number(e.target.value))} sx={{ minWidth: 120 }}>
              {[50, 60, 70, 80, 90].map(v => <MenuItem key={v} value={v}>{v}%</MenuItem>)}
            </Select>
            <Button variant="outlined" size="small" onClick={() => saveAlertMut.mutate()} disabled={saveAlertMut.isPending}>保存配置</Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  )
}
// ===== Tab 3: 订单历史 =====
function OrdersTab() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<number | ''>('')
  const [_detailOpen, setDetailOpen] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<PaymentOrder | null>(null)
  const [invoiceDialogOpen, setInvoiceDialogOpen] = useState(false)
  const [invoiceOrderId, setInvoiceOrderId] = useState<number | null>(null)
  const [invoiceForm, setInvoiceForm] = useState({ type: 'company', title: '', taxNo: '', email: '' })
  const toast = useToast()

  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['payment-orders', page, status],
    queryFn: () => paymentApi.list({ page: page + 1, rows: 20, status: status !== '' ? status : undefined }),
  })

  const invoiceMut = useMutation({
    mutationFn: () => paymentApi.invoiceApply(invoiceOrderId!, invoiceForm),
    onSuccess: () => {
      toast('开票申请已提交', 'success')
      setInvoiceDialogOpen(false)
      qc.invalidateQueries({ queryKey: ['payment-orders'] })
      qc.invalidateQueries({ queryKey: ['payment-invoices'] })
    },
    onError: () => toast('开票申请失败', 'error')
  })

  const exportMut = useMutation({
    mutationFn: () => paymentApi.exportOrders({ status: status !== '' ? status : undefined }),
    onSuccess: (res) => {
      if (res.downloadUrl) window.open(res.downloadUrl)
      toast('导出成功', 'success')
    },
    onError: () => toast('导出失败', 'error'),
  })

  const STATUS_MAP: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
    0: { label: '待支付', color: 'warning' },
    1: { label: '已支付', color: 'success' },
    2: { label: '已退款', color: 'error' },
    3: { label: '已取消', color: 'default' },
  }

  const columns: GridColDef<PaymentOrder>[] = [
    { field: 'orderNo', headerName: '订单号', width: 180 },
    { field: 'amount', headerName: '金额', width: 100, renderCell: (p: { row: PaymentOrder }) => `¥${p.row.amount}` },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: (p: { row: PaymentOrder }) => {
        const s = STATUS_MAP[p.row.status] ?? { label: '未知', color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      }
    },
    { field: 'payType', headerName: '支付方式', width: 100 },
    { field: 'createTime', headerName: '下单时间', width: 160, renderCell: (p: { row: PaymentOrder }) => formatDate(p.row.createTime) },
    {
      field: 'invoiceStatus', headerName: '发票', width: 100,
      renderCell: (p: { row: PaymentOrder }) => {
        if (p.row.status !== 1) return '-';
        return <Chip label="未申请" size="small" variant="outlined" color="secondary" />;
      }
    },
    {
      field: 'actions', headerName: '操作', width: 150, sortable: false,
      renderCell: (p: { row: PaymentOrder }) => (
        <Stack direction="row" spacing={1}>
          <Button size="small" onClick={() => { setSelectedOrder(p.row); setDetailOpen(true) }}>详情</Button>
          {p.row.status === 1 && (
            <Button
              size="small"
              variant="contained"
              disableElevation
              sx={{ borderRadius: 1.5, textTransform: 'none' }}
              onClick={() => { setInvoiceOrderId(p.row.id); setInvoiceDialogOpen(true) }}
            >
              申请开票
            </Button>
          )}
        </Stack>
      )
    },
  ]

  const handleApplyInvoice = async () => {
    if (!invoiceOrderId) return
    invoiceMut.mutate()
  }

  return (
    <Box>
      <Stack direction="row" spacing={2} mb={2} alignItems="center">
        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>状态</InputLabel>
          <Select value={status} label="状态" onChange={e => setStatus(e.target.value as number | '')}>
            <MenuItem value="">全部</MenuItem>
            {Object.entries(STATUS_MAP).map(([k, v]) => <MenuItem key={k} value={Number(k)}>{v.label}</MenuItem>)}
          </Select>
        </FormControl>
        <Button variant="outlined" size="small" onClick={() => exportMut.mutate()} disabled={exportMut.isPending}>导出账单</Button>
      </Stack>

      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        loading={isLoading}
        getRowId={(r: PaymentOrder) => r.id}
        paginationMode="server"
        rowCount={data?.total ?? 0}
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={(m: { page: number; pageSize: number }) => setPage(m.page)}
        autoHeight
        slotProps={{ toolbar: undefined }}
      />

      <Dialog open={invoiceDialogOpen} onClose={() => setInvoiceDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>申请电子增值税发票</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <Alert severity="info" sx={{ borderRadius: 2 }}>
              申请通过后，电子发票将发送至您的邮箱，您也可以在“退款管理-发票记录”中下载。
            </Alert>

            <Grid container spacing={2}>
              <Grid item xs={12}>
                <FormControl fullWidth size="small">
                  <InputLabel>发票类型</InputLabel>
                  <Select
                    value={invoiceForm.type}
                    label="发票类型"
                    onChange={e => setInvoiceForm(prev => ({ ...prev, type: e.target.value }))}
                  >
                    <MenuItem value="personal">个人 / 非企业单位</MenuItem>
                    <MenuItem value="company">企业单位</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  label="发票抬头"
                  fullWidth
                  size="small"
                  placeholder="请输入准确的单位全称或个人姓名"
                  value={invoiceForm.title}
                  onChange={e => setInvoiceForm(prev => ({ ...prev, title: e.target.value }))}
                />
              </Grid>

              {invoiceForm.type === 'company' && (
                <Grid item xs={12}>
                  <TextField
                    label="纳税人识别号"
                    fullWidth
                    size="small"
                    placeholder="15-20位统一社会信用代码"
                    value={invoiceForm.taxNo}
                    onChange={e => setInvoiceForm(prev => ({ ...prev, taxNo: e.target.value }))}
                  />
                </Grid>
              )}

              <Grid item xs={12}>
                <TextField
                  label="接收邮箱"
                  fullWidth
                  size="small"
                  placeholder="example@domain.com"
                  value={invoiceForm.email}
                  onChange={e => setInvoiceForm(prev => ({ ...prev, email: e.target.value }))}
                />
              </Grid>
            </Grid>

            <Typography variant="caption" color="text.secondary">
              * 订单金额（含税）：¥{selectedOrder?.amount?.toLocaleString() || '0.00'}
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInvoiceDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleApplyInvoice}
            disabled={invoiceMut.isPending || !invoiceForm.title || !invoiceForm.email}
          >
            {invoiceMut.isPending ? <CircularProgress size={24} color="inherit" /> : '提交申请'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

// ===== Tab 4: 售后与发票中心 (Refunds & Invoices) =====
function RefundTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [subTab, setSubTab] = useState(0) // 0: 退款, 1: 发票
  const [refundDialogOpen, setRefundDialogOpen] = useState(false)
  const [refundForm, setRefundForm] = useState({ orderId: '', amount: '', reason: '' })
  const [invoicePage, setInvoicePage] = useState(0)
  const [refundPage, setRefundPage] = useState(0)

  const { data: invoiceData, isLoading: invoiceLoading } = useQuery({
    queryKey: ['invoice-list', invoicePage],
    queryFn: () => paymentApi.invoiceList({ page: invoicePage + 1, rows: 10 }),
    enabled: subTab === 1,
  })

  const invoiceDownloadMut = useMutation({
    mutationFn: (id: number) => paymentApi.invoiceDownload(id),
    onSuccess: (res) => {
      if (res?.downloadUrl) {
        window.open(res.downloadUrl, '_blank')
        toast('已开始下载发票', 'success')
      } else {
        toast('下载地址无效', 'error')
      }
    },
    onError: () => toast('下载失败', 'error'),
  })

  // 修正：支付 API 中并没有 refundList，但可以通过 orderList 过滤状态，或者如果后端有独立 refund 模块则调用
  // 这里暂时保持 list 过滤模式，但修正 status 可能的偏差（根据 STATUS_MAP，2 是已退款）
  const { data: refundData, isLoading: refundsLoading } = useQuery({
    queryKey: ['payment-refunds', refundPage],
    queryFn: () => paymentApi.list({ page: refundPage + 1, rows: 10, status: 2 }),
    enabled: subTab === 0,
  })

  const refundMut = useMutation({
    mutationFn: () => paymentApi.refundCreate({
      orderId: Number(refundForm.orderId),
      amount: Number(refundForm.amount),
      reason: refundForm.reason,
    }),
    onSuccess: () => {
      toast('退款申请已提交', 'success')
      qc.invalidateQueries({ queryKey: ['payment-refunds'] })
      setRefundDialogOpen(false)
      setRefundForm({ orderId: '', amount: '', reason: '' })
    },
    onError: () => toast('退款申请失败', 'error'),
  })

  const _refundColumns: GridColDef<PaymentRefund>[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'orderNo', headerName: '订单号', width: 180 },
    { field: 'amount', headerName: '退款金额', width: 120, renderCell: ({ row }) => `￥${(row.amount ?? 0).toFixed(2)}` },
    {
      field: 'status',
      headerName: '状态',
      width: 120,
      renderCell: ({ row }) => {
        const s = row.status
        return (
          <Chip
            label={s === 1 ? '已退款' : s === 2 ? '已拒绝' : '审核中'}
            color={s === 1 ? 'success' : s === 2 ? 'error' : 'warning'}
            size="small"
          />
        )
      },
    },
    { field: 'reason', headerName: '退款原因', width: 200 },
    { field: 'createTime', headerName: '申请时间', width: 180, renderCell: ({ row }) => formatDate(row.createTime) },
  ]

  const _invoiceColumns: GridColDef<PaymentInvoice>[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'orderNo', headerName: '订单号', width: 180 },
    { field: 'type', headerName: '类型', width: 100, renderCell: ({ row }) => row.type === 'company' ? '企业' : '个人' },
    { field: 'title', headerName: '发票抬头', width: 200 },
    { field: 'amount', headerName: '金额', width: 120, renderCell: ({ row }) => `￥${(row.amount ?? 0).toFixed(2)}` },
    {
      field: 'status',
      headerName: '状态',
      width: 120,
      renderCell: ({ row }) => {
        const s = row.status
        return (
          <Chip
            label={s === 1 ? '已开具' : s === 2 ? '已拒绝' : '申请中'}
            color={s === 1 ? 'success' : s === 2 ? 'error' : 'warning'}
            size="small"
          />
        )
      },
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 100,
      renderCell: ({ row }) => (
        <Button size="small" disabled={row.status !== 1} onClick={() => invoiceDownloadMut.mutate(row.id)}>
          下载
        </Button>
      ),
    },
  ]

  // 使用 subTab 切换的 v3.0 售后中心布局
  return (
    <Box>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="subtitle1" fontWeight={700}>
              售后与凭证中心
            </Typography>
            {subTab === 0 && (
              <Button
                variant="contained"
                size="small"
                startIcon={<UpgradeIcon sx={{ transform: 'rotate(180deg)' }} />}
                onClick={() => setRefundDialogOpen(true)}
              >
                申请退款
              </Button>
            )}
          </Stack>

          <Tabs
            value={subTab}
            onChange={(_e, v) => setSubTab(v)}
            sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}
          >
            <Tab label="退款记录 (Refunds)" sx={{ fontWeight: 700 }} />
            <Tab label="发票记录 (Invoices)" sx={{ fontWeight: 700 }} />
          </Tabs>

          <Box sx={{ minHeight: 400 }}>
            {subTab === 0 ? (
              <StandardDataGrid
                rows={refundData?.list || []}
                columns={_refundColumns}
                loading={refundsLoading}
                rowCount={refundData?.total || 0}
                paginationMode="server"
                paginationModel={{ page: refundPage, pageSize: 10 }}
                onPaginationModelChange={(m) => setRefundPage(m.page)}
                autoHeight
                slotProps={{ toolbar: undefined }}
              />
            ) : (
              <StandardDataGrid
                rows={invoiceData?.list || []}
                columns={_invoiceColumns}
                loading={invoiceLoading}
                rowCount={invoiceData?.total || 0}
                paginationMode="server"
                paginationModel={{ page: invoicePage, pageSize: 10 }}
                onPaginationModelChange={(m) => setInvoicePage(m.page)}
                autoHeight
                slotProps={{ toolbar: undefined }}
              />
            )}
          </Box>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Alert severity="info" icon={<CheckCircleIcon />}>
            <Typography variant="subtitle2" fontWeight={700}>退款时效说明</Typography>
            <Typography variant="caption" display="block">
              • 支付后 72 小时内且 AI 额度消耗 &lt; 1% 可申请全额秒退。
            </Typography>
            <Typography variant="caption" display="block">
              • 超过 72 小时或额度已使用，将进入人工审核流程，预计 3 个工作日完成。
            </Typography>
          </Alert>
        </Grid>
        <Grid item xs={12} md={6}>
          <Alert severity="warning" icon={<WarningAmberIcon />}>
            <Typography variant="subtitle2" fontWeight={700}>开票注意事项</Typography>
            <Typography variant="caption" display="block">
              • 电子发票将在审核通过后自动发送至账号绑定邮箱。
            </Typography>
            <Typography variant="caption" display="block">
              • 如需增值税专用发票，请在线下联系大客户经理处理。
            </Typography>
          </Alert>
        </Grid>
      </Grid>

      <Dialog open={refundDialogOpen} onClose={() => setRefundDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ fontWeight: 700 }}>申请售后退款</DialogTitle>
        <DialogContent>
          <Alert severity="error" sx={{ mb: 2 }}>
            警告：退款后相关套餐特权将立即失效，正在生成的 AI 任务可能会中断。
          </Alert>
          <Stack spacing={2.5} sx={{ mt: 1 }}>
            <TextField
              label="关联订单号"
              fullWidth
              required
              placeholder="请从订单历史中复制订单号"
              value={refundForm.orderId}
              onChange={(e) => setRefundForm({ ...refundForm, orderId: e.target.value })}
            />
            <TextField
              label="申请退款金额"
              type="number"
              fullWidth
              required
              InputProps={{ startAdornment: <Typography variant="body2" sx={{ mr: 1 }}>￥</Typography> }}
              value={refundForm.amount}
              onChange={(e) => setRefundForm({ ...refundForm, amount: e.target.value })}
            />
            <FormControl fullWidth required>
              <InputLabel>退款原因</InputLabel>
              <Select
                value={refundForm.reason}
                label="退款原因"
                onChange={(e) => setRefundForm({ ...refundForm, reason: e.target.value })}
              >
                <MenuItem value="功能不符合预期">功能不符合预期</MenuItem>
                <MenuItem value="操作失误/误购">操作失误/误购</MenuItem>
                <MenuItem value="价格/方案调整">价格/方案调整</MenuItem>
                <MenuItem value="其他原因">其他原因</MenuItem>
              </Select>
            </FormControl>
            {refundForm.reason === '其他原因' && (
              <TextField
                label="原因详情"
                multiline
                rows={2}
                fullWidth
                placeholder="请描述您的具体需求，以便我们改进产品"
              />
            )}
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

// ===== 主组件 =====
export default function SubscriptionPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box>
      <PageHeader
        title="订阅与配额管理"
        breadcrumbs={[{ label: '支付中心' }, { label: '订阅管理' }]}
        subtitle="管理套餐订阅、AI 配额使用与订单记录"
      />

      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
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
