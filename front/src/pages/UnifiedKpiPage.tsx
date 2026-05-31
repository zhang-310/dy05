import { useMemo, useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid, LinearProgress,
  MenuItem, Paper, Stack, TextField, ToggleButton, ToggleButtonGroup,
  Typography,
} from '@mui/material'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import AutoStoriesIcon from '@mui/icons-material/AutoStories'
import RefreshIcon from '@mui/icons-material/Refresh'
import DownloadIcon from '@mui/icons-material/Download'
import PreviewIcon from '@mui/icons-material/Visibility'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag'
import { useTheme } from '@mui/material/styles'
import { useQuery } from '@tanstack/react-query'
import type { GridColDef } from '@mui/x-data-grid'
import { ErrorAlert, PageHeader, StandardDataGrid } from '@/components/base'
import { LazyECharts } from '@/utils/echarts-registry'
import {
  dashboardApi,
  type AdminStats,
  type CockpitSessionRow,
  type KpiUnified,
  type ProductGmvSummary,
  type ProfitMatrixRow,
} from '@/api/dashboard'
import { getErrorMessage } from '@/utils/errorHandler'

interface KpiCardProps {
  label: string
  value: string
  sub?: string
  tone?: KpiTone
  icon: React.ReactNode
  loading?: boolean
}

type KpiTone = 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'

function fmt(n: number | undefined, digits = 0) {
  if (n == null) return '--'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: digits })
}

function fmtMoney(n: number | undefined) {
  if (n == null) return '--'
  return `¥${n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`
}

function fmtPct(n: number | undefined) {
  if (n == null) return '--'
  const value = Math.abs(n) <= 1 ? n * 100 : n
  return `${value.toFixed(1)}%`
}

function dateFromLookback(days: number) {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

function statusLabel(status?: number) {
  if (status === 0) return '准备中'
  if (status === 1) return '直播中'
  if (status === 2) return '已结束'
  if (status === 3) return '已取消'
  return '-'
}

function filterSummary(lookbackDays: number, dateFrom: string, sessionStatus: number | '') {
  const status = sessionStatus === '' ? '全部状态' : statusLabel(sessionStatus)
  return `lookbackDays=${lookbackDays}，dateFrom=${dateFrom}，sessionStatus=${status}`
}

function KpiCard({ label, value, sub, tone = 'primary', icon, loading }: KpiCardProps) {
  const theme = useTheme()
  const color = theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main

  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent sx={{ height: '100%', display: 'flex', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', width: '100%', gap: 2 }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography
              variant="h5"
              data-testid="unified-kpi-card-value-surface"
              data-kpi-tone={tone}
              sx={{ fontWeight: 700, color, mt: 0.5 }}
            >
              {loading ? '检查中...' : value}
            </Typography>
            {sub && <Typography variant="caption" color="text.secondary">{sub}</Typography>}
          </Box>
          <Box sx={{ color, opacity: 0.75, display: 'flex' }}>{icon}</Box>
        </Box>
      </CardContent>
    </Card>
  )
}

function downloadCsv(filename: string, csv: string) {
  const blob = new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || `cockpit-${Date.now()}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

export default function UnifiedKpiPage() {
  const theme = useTheme()
  const [lookbackDays, setLookbackDays] = useState(30)
  const [dateFrom, setDateFrom] = useState(() => dateFromLookback(30))
  const [sessionStatus, setSessionStatus] = useState<number | ''>('')
  const [exporting, setExporting] = useState(false)
  const [operationError, setOperationError] = useState('')
  const [exportResult, setExportResult] = useState('')

  const statsQuery = useQuery({
    queryKey: ['unified-kpi-admin-stats'],
    queryFn: () => dashboardApi.adminStats(),
  })
  const kpiQuery = useQuery({
    queryKey: ['unified-kpi-gmv', lookbackDays],
    queryFn: () => dashboardApi.kpiUnified(lookbackDays),
  })
  const formatQuery = useQuery({
    queryKey: ['unified-kpi-live-format', lookbackDays],
    queryFn: () => dashboardApi.liveFormatGmv(lookbackDays),
  })
  const productQuery = useQuery({
    queryKey: ['unified-kpi-product-gmv', lookbackDays],
    queryFn: () => dashboardApi.productGmvSummary(lookbackDays),
  })
  const profitQuery = useQuery({
    queryKey: ['unified-kpi-profit-matrix', lookbackDays],
    queryFn: () => dashboardApi.profitMatrixPreview(lookbackDays),
  })
  const funnelQuery = useQuery({
    queryKey: ['unified-kpi-funnel', lookbackDays],
    queryFn: () => dashboardApi.conversionFunnel(lookbackDays),
  })
  const cockpitQuery = useQuery({
    queryKey: ['unified-kpi-cockpit-preview', dateFrom, sessionStatus],
    queryFn: () => dashboardApi.cockpitPreviewRows({ lookbackDays, dateFrom, sessionStatus }),
    enabled: false,
  })

  const stats = statsQuery.data as AdminStats | undefined
  const kpi = kpiQuery.data as KpiUnified | undefined
  const liveFormats = formatQuery.data ?? []
  const products = productQuery.data ?? []
  const profitMatrix = profitQuery.data
  const funnel = funnelQuery.data
  const cockpitRows = cockpitQuery.data ?? []
  const cockpitFilterText = filterSummary(lookbackDays, dateFrom, sessionStatus)
  const dataSourceErrors = [
    statsQuery.isError,
    kpiQuery.isError,
    formatQuery.isError,
    productQuery.isError,
    profitQuery.isError,
    funnelQuery.isError,
    cockpitQuery.isError,
  ].filter(Boolean).length

  const cockpitTotal = useMemo(() => cockpitRows.reduce((acc, row) => ({
    gmv: acc.gmv + row.gmv,
    productLineCount: acc.productLineCount + row.productLineCount,
  }), { gmv: 0, productLineCount: 0 }), [cockpitRows])

  const productColumns = useMemo<GridColDef<ProductGmvSummary>[]>(() => [
    { field: 'productId', headerName: '商品ID', width: 100 },
    { field: 'productName', headerName: '商品名称', flex: 1, minWidth: 160 },
    { field: 'sessionCount', headerName: '覆盖场次', width: 110, type: 'number' },
    { field: 'totalGmv', headerName: 'GMV', width: 130, valueGetter: (_v, row) => fmtMoney(row.totalGmv) },
  ], [])

  const profitColumns = useMemo<GridColDef<ProfitMatrixRow>[]>(() => [
    { field: 'liveFormat', headerName: '直播形式', flex: 1, minWidth: 130 },
    { field: 'sessionCount', headerName: '场次数', width: 100, type: 'number' },
    { field: 'totalGmv', headerName: 'GMV', width: 130, valueGetter: (_v, row) => fmtMoney(row.totalGmv) },
    { field: 'estimatedMarginRate', headerName: '毛利率', width: 110, valueGetter: (_v, row) => `${(row.estimatedMarginRate * 100).toFixed(1)}%${row.isEstimated ? ' *' : ''}` },
    { field: 'note', headerName: '备注', flex: 1, minWidth: 160 },
  ], [])

  const cockpitColumns = useMemo<GridColDef<CockpitSessionRow>[]>(() => [
    { field: 'sessionId', headerName: '场次ID', width: 100 },
    { field: 'liveTitle', headerName: '标题', flex: 1, minWidth: 160, valueFormatter: (v) => v || '未命名场次' },
    { field: 'status', headerName: '状态', width: 100, valueGetter: (_v, row) => statusLabel(row.status) },
    { field: 'startTime', headerName: '开始时间', width: 165, valueFormatter: (v) => v || '-' },
    { field: 'gmv', headerName: 'GMV', width: 130, valueGetter: (_v, row) => fmtMoney(row.gmv) },
    { field: 'productLineCount', headerName: '排品线数', width: 110, type: 'number' },
  ], [])

  const chartColor = (tone: KpiTone) => theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  const liveFormatChartColor = chartColor('success')
  const funnelChartColors = [
    chartColor('primary'),
    chartColor('success'),
    chartColor('warning'),
    chartColor('secondary'),
    chartColor('info'),
  ]

  const liveFormatOption = useMemo(() => ({
    tooltip: { trigger: 'axis' },
    grid: { left: 64, right: 18, bottom: 52, top: 26 },
    xAxis: { type: 'category', data: liveFormats.map((item) => item.format), axisLabel: { rotate: 22 } },
    yAxis: { type: 'value', name: 'GMV' },
    series: [{ type: 'bar', data: liveFormats.map((item) => item.gmv), itemStyle: { color: liveFormatChartColor } }],
  }), [liveFormatChartColor, liveFormats])

  const funnelOption = useMemo(() => {
    const steps = funnel?.steps ?? []
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'funnel',
        left: '8%',
        width: '84%',
        min: 0,
        max: Math.max(...steps.map((step) => step.value), 1),
        sort: 'none',
        data: steps.map((step, index) => ({
          name: step.name,
          value: step.value,
          itemStyle: { color: funnelChartColors[index] ?? funnelChartColors[funnelChartColors.length - 1] },
        })),
      }],
    }
  }, [funnel, funnelChartColors])

  const refetchAll = () => {
    void statsQuery.refetch()
    void kpiQuery.refetch()
    void formatQuery.refetch()
    void productQuery.refetch()
    void profitQuery.refetch()
    void funnelQuery.refetch()
    void cockpitQuery.refetch()
  }

  const handleLookbackChange = (days: number) => {
    setLookbackDays(days)
    setDateFrom(dateFromLookback(days))
  }

  const handlePreview = () => {
    setOperationError('')
    void cockpitQuery.refetch()
  }

  const handleExport = async () => {
    setOperationError('')
    setExportResult('')
    setExporting(true)
    try {
      const result = await dashboardApi.cockpitExport({ lookbackDays, dateFrom, sessionStatus })
      downloadCsv(result.filename, result.csv)
      setExportResult(`已从 /dashboard/cockpit-export 导出 ${result.rowCount} 行：${result.filename}`)
    } catch (error) {
      setOperationError(`/dashboard/cockpit-export 导出失败（${cockpitFilterText}）：${getErrorMessage(error)}。当前筛选和预览结果已保留。`)
    } finally {
      setExporting(false)
    }
  }

  const readyEndpoints = [
    '/dashboard/admin/stats',
    '/dashboard/kpi-unified',
    '/dashboard/live-format-gmv',
    '/dashboard/product-gmv-summary',
    '/dashboard/profit-matrix-preview',
    '/dashboard/conversion-funnel',
    '/dashboard/cockpit-preview',
    '/dashboard/cockpit-export',
  ].join('|')
  const unsupportedEndpoints = [
    '/finance/settlement',
    '/dashboard/conversion-funnel?channel',
    '/dashboard/cockpit-export-local',
    '/inventory/update',
    '/payment/order-funnel',
    '/dashboard/order-payment-funnel',
  ].join('|')

  return (
    <Box
      data-testid="unified-kpi-workbench"
      data-contract-scope="unified-kpi-dashboard"
      data-ready-endpoints={readyEndpoints}
      data-unsupported-endpoints={unsupportedEndpoints}
      data-no-local-kpi-fallback="true"
      data-no-finance-settlement="true"
      data-row-retained-on-export-error="true"
      sx={{ p: 3 }}
    >
      <PageHeader
        title="统一 KPI"
        subtitle="按真实 dashboard 契约汇总经营、直播形式、商品 GMV、利润矩阵、漏斗和驾驶舱预览。"
        breadcrumbs={[{ label: '运营' }, { label: '统一 KPI' }]}
        actions={<Button variant="outlined" startIcon={<RefreshIcon />} onClick={refetchAll}>刷新全部</Button>}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="unified-kpi-contract-alert"
        data-no-finance-settlement="true"
        data-profit-source="/dashboard/profit-matrix-preview"
        sx={{ mb: 2 }}
      >
        `/dashboard/admin/stats` 需要管理员权限；`/dashboard/profit-matrix-preview` 当前毛利率是后端估算值，页面用星号标记，不把它当财务结算。
      </Alert>

      <Grid
        container
        spacing={1.5}
        sx={{ mb: 2 }}
        data-testid="unified-kpi-source-chip-grid"
        data-ready-endpoints={readyEndpoints}
      >
        {[
          ['管理员统计', '/dashboard/admin/stats', statsQuery.isError ? '异常' : statsQuery.isLoading ? '检查中' : '已连接'],
          ['GMV KPI', '/dashboard/kpi-unified', kpiQuery.isError ? '异常' : kpiQuery.isLoading ? '检查中' : '已连接'],
          ['直播形式', '/dashboard/live-format-gmv', formatQuery.isError ? '异常' : formatQuery.isLoading ? '检查中' : '已连接'],
          ['商品/利润/漏斗', '/dashboard/product-gmv-summary 等', dataSourceErrors > 0 ? `${dataSourceErrors} 个异常` : '已连接'],
        ].map(([label, source, status]) => (
          <Grid item xs={12} md={3} key={label}>
            <Chip
              data-testid="unified-kpi-source-chip"
              variant="outlined"
              color={String(status).includes('异常') ? 'error' : String(status).includes('检查中') ? 'default' : 'success'}
              label={`${label}: ${status} · ${source}`}
              sx={{ borderRadius: 1, maxWidth: '100%' }}
            />
          </Grid>
        ))}
      </Grid>

      <Stack spacing={1.25} sx={{ mb: 2 }} data-testid="unified-kpi-error-stack" data-row-retained-on-error="true">
        {statsQuery.isError && <Box data-testid="unified-kpi-admin-stats-error" data-contract-endpoint="/dashboard/admin/stats"><ErrorAlert severity="warning" title="管理员统计加载失败" message={`/dashboard/admin/stats：${getErrorMessage(statsQuery.error)}`} onRetry={() => void statsQuery.refetch()} /></Box>}
        {kpiQuery.isError && <Box data-testid="unified-kpi-kpi-error" data-contract-endpoint="/dashboard/kpi-unified"><ErrorAlert severity="warning" title="GMV KPI 加载失败" message={`/dashboard/kpi-unified：${getErrorMessage(kpiQuery.error)}`} onRetry={() => void kpiQuery.refetch()} /></Box>}
        {formatQuery.isError && <Box data-testid="unified-kpi-live-format-error" data-contract-endpoint="/dashboard/live-format-gmv"><ErrorAlert severity="warning" title="直播形式 GMV 加载失败" message={`/dashboard/live-format-gmv：${getErrorMessage(formatQuery.error)}`} onRetry={() => void formatQuery.refetch()} /></Box>}
        {productQuery.isError && <Box data-testid="unified-kpi-product-error" data-contract-endpoint="/dashboard/product-gmv-summary"><ErrorAlert severity="warning" title="商品 GMV 加载失败" message={`/dashboard/product-gmv-summary：${getErrorMessage(productQuery.error)}`} onRetry={() => void productQuery.refetch()} /></Box>}
        {profitQuery.isError && <Box data-testid="unified-kpi-profit-error" data-contract-endpoint="/dashboard/profit-matrix-preview" data-no-finance-settlement="true"><ErrorAlert severity="warning" title="利润矩阵加载失败" message={`/dashboard/profit-matrix-preview：${getErrorMessage(profitQuery.error)}`} onRetry={() => void profitQuery.refetch()} /></Box>}
        {funnelQuery.isError && <Box data-testid="unified-kpi-funnel-error" data-contract-endpoint="/dashboard/conversion-funnel"><ErrorAlert severity="warning" title="转化漏斗加载失败" message={`/dashboard/conversion-funnel：${getErrorMessage(funnelQuery.error)}`} onRetry={() => void funnelQuery.refetch()} /></Box>}
        {cockpitQuery.isError && <Box data-testid="unified-kpi-cockpit-preview-error" data-contract-endpoint="/dashboard/cockpit-preview" data-filter-retained="true"><ErrorAlert severity="warning" title="驾驶舱预览加载失败" message={`/dashboard/cockpit-preview 加载失败（${cockpitFilterText}）：${getErrorMessage(cockpitQuery.error)}。当前筛选已保留。`} onRetry={() => void cockpitQuery.refetch()} /></Box>}
        {operationError && <Box data-testid="unified-kpi-export-error" data-contract-endpoint="/dashboard/cockpit-export" data-row-retained-on-export-error="true"><ErrorAlert severity="warning" title="导出失败" message={operationError} /></Box>}
        {exportResult && <Alert severity="success" variant="outlined" data-testid="unified-kpi-export-success" data-contract-endpoint="/dashboard/cockpit-export">{exportResult}</Alert>}
      </Stack>

      <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
        <ToggleButtonGroup size="small" exclusive value={lookbackDays} onChange={(_, value) => value && handleLookbackChange(value)}>
          <ToggleButton value={7}>近7天</ToggleButton>
          <ToggleButton value={30}>近30天</ToggleButton>
          <ToggleButton value={90}>近90天</ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>GMV 与交易</Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="今日 GMV" value={fmtMoney(kpi?.gmvToday)} sub={`环比 ${fmtPct(kpi?.gmvMom)}`} tone="warning" icon={<TrendingUpIcon />} loading={kpiQuery.isLoading} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="今日订单数" value={fmt(kpi?.ordersToday)} sub={`均单价 ${fmtMoney(kpi?.avgOrderValue)}`} tone="error" icon={<ShoppingBagIcon />} loading={kpiQuery.isLoading} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="转化率" value={fmtPct(kpi?.conversionRate)} sub="来自 /dashboard/kpi-unified" tone="success" icon={<TrendingUpIcon />} loading={kpiQuery.isLoading} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="今日收入" value={fmtMoney(stats?.todayRevenue)} sub="来自管理员统计" tone="error" icon={<AttachMoneyIcon />} loading={statsQuery.isLoading} />
        </Grid>
      </Grid>

      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>运营数据</Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="今日直播场次" value={fmt(stats?.todaySessions)} sub={`累计 ${fmt(stats?.totalLiveSessions)}`} tone="primary" icon={<LiveTvIcon />} loading={statsQuery.isLoading} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="今日 AI 调用" value={fmt(stats?.todayAiCalls)} sub={`成功率 ${fmtPct(stats?.aiSuccessRate)}`} tone="secondary" icon={<SmartToyIcon />} loading={statsQuery.isLoading} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="今日短视频" value={fmt(stats?.todayVideos)} sub={`累计 ${fmt(stats?.totalVideos)}`} tone="info" icon={<VideoLibraryIcon />} loading={statsQuery.isLoading} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard label="文案库总量" value={fmt(stats?.totalCopyItems)} sub={`已审批 ${fmt(stats?.approvedCopyItems)}`} tone="success" icon={<AutoStoriesIcon />} loading={statsQuery.isLoading} />
        </Grid>
      </Grid>

      {kpi?.gmvTarget != null && kpi.gmvTarget > 0 && (
        <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
          <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.75 }}>
            <Typography variant="subtitle2">今日 GMV 目标完成率</Typography>
            <Typography variant="body2">{fmtPct((kpi.gmvToday / kpi.gmvTarget) * 100)} / 目标 {fmtMoney(kpi.gmvTarget)}</Typography>
          </Stack>
          <LinearProgress variant="determinate" value={Math.min(100, (kpi.gmvToday / kpi.gmvTarget) * 100)} sx={{ height: 8, borderRadius: 1 }} />
        </Paper>
      )}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} md={7}>
          <Card variant="outlined">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}>
                <Box>
                  <Typography variant="subtitle2" fontWeight={700}>直播形式 GMV</Typography>
                  <Typography variant="caption" color="text.secondary">数据源 /dashboard/live-format-gmv</Typography>
                </Box>
              </Stack>
              {liveFormats.length > 0 ? (
                <Box data-testid="unified-kpi-live-format-chart-surface" data-chart-color={liveFormatChartColor}>
                  <LazyECharts option={liveFormatOption} style={{ height: 280 }} />
                </Box>
              ) : (
                <Box data-testid="unified-kpi-live-format-empty" data-contract-endpoint="/dashboard/live-format-gmv" sx={{ height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
                  <Typography color="text.secondary">暂无直播形式 GMV；请检查场次 `sessionType` 和排品 `revenue`。</Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={5}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" fontWeight={700}>转化漏斗</Typography>
              <Typography variant="caption" color="text.secondary">数据源 /dashboard/conversion-funnel；当前后端返回观看、点赞、进入商品三步。</Typography>
              {funnel?.steps?.length ? (
                <Box data-testid="unified-kpi-funnel-chart-surface" data-chart-colors={funnelChartColors.join('|')}>
                  <LazyECharts option={funnelOption} style={{ height: 280 }} />
                </Box>
              ) : (
                <Box
                  data-testid="unified-kpi-funnel-empty"
                  data-contract-endpoint="/dashboard/conversion-funnel"
                  data-unsupported-endpoints="/dashboard/conversion-funnel?channel|/payment/order-funnel"
                  sx={{ height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}
                >
                  <Typography color="text.secondary">暂无漏斗 steps，订单/支付需接交易链路后展示。</Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} lg={6}>
          <Card variant="outlined" data-testid="unified-kpi-product-table-card" data-contract-endpoint="/dashboard/product-gmv-summary" sx={{ p: 2 }}>
            <Typography variant="subtitle2" fontWeight={700}>商品 GMV 汇总</Typography>
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
              数据源 /dashboard/product-gmv-summary；`sessionCount` 表示覆盖场次，不伪造成订单数。
            </Typography>
            <Box sx={{ height: 320 }}>
              <StandardDataGrid
                rows={products}
                columns={productColumns as GridColDef[]}
                loading={productQuery.isLoading}
                getRowId={(row) => String(row.productId)}
                hideFooter
                disableColumnMenu
              />
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} lg={6}>
          <Card
            variant="outlined"
            data-testid="unified-kpi-profit-table-card"
            data-contract-endpoint="/dashboard/profit-matrix-preview"
            data-no-finance-settlement="true"
            sx={{ p: 2 }}
          >
            <Typography variant="subtitle2" fontWeight={700}>利润矩阵预览</Typography>
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
              数据源 /dashboard/profit-matrix-preview；星号毛利率为估算值。
            </Typography>
            <Box sx={{ height: 320 }}>
              <StandardDataGrid
                rows={profitMatrix?.rows ?? []}
                columns={profitColumns as GridColDef[]}
                loading={profitQuery.isLoading}
                getRowId={(row) => String(row.liveFormat)}
                hideFooter
                disableColumnMenu
              />
            </Box>
          </Card>
        </Grid>
      </Grid>

      <Card
        variant="outlined"
        data-testid="unified-kpi-cockpit-card"
        data-contract-preview-endpoint="/dashboard/cockpit-preview"
        data-contract-export-endpoint="/dashboard/cockpit-export"
        data-filter-retained="true"
        sx={{ p: 2 }}
      >
        <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" spacing={2} sx={{ mb: 2 }}>
          <Box>
            <Typography variant="subtitle2" fontWeight={700}>驾驶舱场次预览</Typography>
            <Typography variant="caption" color="text.secondary">
              数据源 /dashboard/cockpit-preview；导出使用 /dashboard/cockpit-export，同一筛选条件。
            </Typography>
          </Box>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <TextField
              label="开始日期"
              type="date"
              size="small"
              value={dateFrom}
              inputProps={{ 'data-testid': 'unified-kpi-cockpit-date-input' }}
              InputLabelProps={{ shrink: true }}
              onChange={(event) => setDateFrom(event.target.value)}
            />
            <TextField
              label="场次状态"
              select
              size="small"
              value={sessionStatus}
              sx={{ minWidth: 120 }}
              inputProps={{ 'data-testid': 'unified-kpi-cockpit-status-input' }}
              onChange={(event) => setSessionStatus(event.target.value === '' ? '' : Number(event.target.value))}
            >
              <MenuItem value="">全部</MenuItem>
              <MenuItem value={0}>准备中</MenuItem>
              <MenuItem value={1}>直播中</MenuItem>
              <MenuItem value={2}>已结束</MenuItem>
              <MenuItem value={3}>已取消</MenuItem>
            </TextField>
            <Button variant="outlined" startIcon={<PreviewIcon />} onClick={handlePreview} disabled={cockpitQuery.isFetching}>
              {cockpitQuery.isFetching ? '预览中...' : '预览'}
            </Button>
            <Button variant="contained" startIcon={<DownloadIcon />} onClick={handleExport} disabled={exporting}>
              {exporting ? '导出中...' : '导出 CSV'}
            </Button>
          </Stack>
        </Stack>
        <Alert
          severity="info"
          variant="outlined"
          data-testid="unified-kpi-cockpit-filter-summary"
          data-filter-retained="true"
          sx={{ mb: 1.5 }}
        >
          当前预览合计 {cockpitRows.length} 场，GMV {fmtMoney(cockpitTotal.gmv)}，排品线数 {fmt(cockpitTotal.productLineCount)}。
          筛选：{cockpitFilterText}。
        </Alert>
        <Box sx={{ height: 360 }} data-testid="unified-kpi-cockpit-grid" data-row-retained-on-export-error="true">
          <StandardDataGrid
            rows={cockpitRows}
            columns={cockpitColumns as GridColDef[]}
            loading={cockpitQuery.isFetching}
            getRowId={(row) => String(row.sessionId)}
            disableColumnMenu
            hideFooter
          />
        </Box>
      </Card>
    </Box>
  )
}
