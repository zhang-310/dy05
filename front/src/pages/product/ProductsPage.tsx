import { useState, useCallback, useEffect } from 'react'
import {
  Box, TextField, Button, Chip, Stack, MenuItem, LinearProgress, Tooltip,
  Drawer, Tab, Tabs, Typography, IconButton, Divider, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions,
  CircularProgress, Paper, Alert,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CloseIcon from '@mui/icons-material/Close'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import LinkIcon from '@mui/icons-material/Link'
import RefreshIcon from '@mui/icons-material/Refresh'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import {
  GridToolbarColumnsButton,
  GridToolbarContainer,
  GridToolbarDensitySelector,
  GridToolbarFilterButton,
  type GridColDef,
  type GridRowSelectionModel,
} from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, DataGridEmptyOverlay } from '@/components/base'
import { productApi, type DyProduct, type ProductSave, type ProductExtractResult, exportProductToShortVideo } from '@/api/product'
import type { GmvContribItem } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { LazyECharts } from '@/utils/echarts-registry'
import { useNavigate } from 'react-router-dom'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'
import {
  buildBatchExtractItems,
  getBatchExtractSummary,
  getProductPreviewUrl,
  getProductThumbUrl,
  toDisplayPct,
  toStorePct,
  type BatchExtractItem,
} from './productPageModel'

const STATUS_OPTIONS = [
  { value: 1, label: '上架', color: 'success' as const },
  { value: 0, label: '下架', color: 'default' as const },
]

const CATEGORIES = ['护肤', '彩妆', '面膜', '精华', '洁面', '防晒', '眼霜', '身体护理', '其他']
const SV_STYLES = ['种草', '测评', '成分解析', '对比实测']
const SV_DURATIONS = [{ value: 30, label: '30秒' }, { value: 60, label: '60秒' }, { value: 90, label: '90秒' }]
type ChannelTone = 'primary' | 'secondary' | 'success' | 'warning' | 'info' | 'error'
const CHANNEL_TONES: ChannelTone[] = ['primary', 'secondary', 'success', 'warning', 'info', 'error']
const PRODUCT_ENDPOINTS = {
  search: '/product/search',
  save: '/product/save',
  delete: '/product/delete',
  batchDelete: '/product/batch-delete',
  extractFromLink: '/product/extract-from-link',
  scriptGenerate: '/product/script/generate',
  scriptList: '/product/script/search',
  effectivenessRanking: '/product/script-effectiveness/ranking',
  scriptUsageList: '/product/script/usage-list',
  salesHistorySearch: '/product/sales-history/search',
  exportShortVideo: '/product/script/export-to-shortvideo',
} as const
const PRODUCT_READY_ENDPOINTS = [
  PRODUCT_ENDPOINTS.search,
  PRODUCT_ENDPOINTS.save,
  PRODUCT_ENDPOINTS.delete,
  PRODUCT_ENDPOINTS.batchDelete,
  PRODUCT_ENDPOINTS.extractFromLink,
  PRODUCT_ENDPOINTS.scriptGenerate,
  PRODUCT_ENDPOINTS.exportShortVideo,
] as const
const PRODUCT_CONTEXT_ENDPOINTS = [
  PRODUCT_ENDPOINTS.scriptList,
  PRODUCT_ENDPOINTS.effectivenessRanking,
  PRODUCT_ENDPOINTS.scriptUsageList,
  PRODUCT_ENDPOINTS.salesHistorySearch,
] as const
const PRODUCT_UNSUPPORTED_ACTIONS = [
  'store-batch-sync',
  'static-product-fallback',
  'local-delete-on-error',
  'local-shortvideo-project',
  'server-product-export',
  'inventory-auto-sync-ui',
] as const

const defaultForm: Partial<ProductSave> = {
  productName: '', productCode: '', category: '', brand: '',
  price: 0, costPrice: 0, profitMarginPct: 0,
  inventory: 0, unit: '件', imageUrl: '', productLink: '',
  description: '', sellingPoints: '', status: 1,
}

function MetricPill({ label, value, tone = 'default' }: { label: string; value: string; tone?: 'default' | 'primary' | 'success' | 'warning' | 'error' }) {
  return (
    <Box
      data-testid="product-kpi-card"
      data-compact-testid="product-compact-metric"
      data-contract-source={PRODUCT_ENDPOINTS.search}
      data-metric-label={label}
      sx={(theme) => ({
        display: 'flex',
        alignItems: 'center',
        gap: 0.75,
        px: 1,
        py: 0.5,
        border: '1px solid',
        borderColor: tone === 'default' ? 'divider' : alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.45 : 0.28),
        bgcolor: tone === 'default' ? 'background.paper' : alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.16 : 0.08),
        borderRadius: 1,
        minHeight: 30,
      })}
    >
      <Typography variant="caption" color="text.secondary" noWrap>{label}</Typography>
      <Typography variant="body2" fontWeight={700} color={tone === 'default' ? 'text.primary' : `${tone}.main`} noWrap>{value}</Typography>
    </Box>
  )
}

function DetailField({ label, value, wide = false }: { label: string; value?: React.ReactNode; wide?: boolean }) {
  return (
    <Grid item xs={wide ? 12 : 6} sm={wide ? 12 : 4}>
      <Box
        data-testid="product-detail-field"
        sx={{
          minHeight: 44,
          px: 1,
          py: 0.75,
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 1,
          bgcolor: 'background.paper',
        }}
      >
        <Typography variant="caption" color="text.secondary" display="block" noWrap>{label}</Typography>
        <Typography variant="body2" fontWeight={600} sx={{ wordBreak: 'break-word' }}>{value || '--'}</Typography>
      </Box>
    </Grid>
  )
}

function ProductGridToolbar({
  searchSlot,
  actionSlot,
}: {
  searchSlot?: React.ReactNode
  actionSlot?: React.ReactNode
}) {
  return (
    <GridToolbarContainer
      data-testid="product-compact-grid-toolbar"
      sx={{
        px: 1,
        py: 0.75,
        minHeight: 42,
        gap: 1,
        alignItems: 'center',
        borderBottom: '1px solid',
        borderColor: 'divider',
      }}
    >
      <Box sx={{ flex: 1, minWidth: 320, display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
        {searchSlot}
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
        {actionSlot}
        <Divider orientation="vertical" flexItem sx={{ mx: 0.25 }} />
        <GridToolbarColumnsButton slotProps={{ button: { size: 'small' } }} />
        <GridToolbarFilterButton slotProps={{ button: { size: 'small' } }} />
        <GridToolbarDensitySelector slotProps={{ button: { size: 'small' } }} />
      </Box>
    </GridToolbarContainer>
  )
}

function hasUsageStats(stats?: { totalScripts: number; activeScripts: number; avgDuration: number; totalTokens: number; byStyle: Record<string, number>; byType: Record<string, number>; bySource: Record<string, number> }) {
  return Boolean(stats && (
    stats.totalScripts > 0 ||
    stats.activeScripts > 0 ||
    stats.avgDuration > 0 ||
    stats.totalTokens > 0 ||
    Object.keys(stats.byStyle).length > 0 ||
    Object.keys(stats.byType).length > 0 ||
    Object.keys(stats.bySource).length > 0
  ))
}

function topUsageEntries(values?: Record<string, number>, limit = 6) {
  return Object.entries(values ?? {})
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit)
}

// ─── Link Extraction Dialog ─────────────────────────────────────────────────
function LinkExtractDialog({ open, onClose, onApply }: {
  open: boolean
  onClose: () => void
  onApply: (productLink: string, result: ProductExtractResult) => void
}) {
  const toast = useToast()
  const [productLink, setProductLink] = useState('')
  const [result, setResult] = useState<ProductExtractResult | null>(null)
  const [extractError, setExtractError] = useState('')

  const extractMut = useMutation({
    mutationFn: () => productApi.extractFromLink(productLink.trim()),
    onSuccess: (res) => {
      setExtractError('')
      setResult(res)
      const hasAnyField = Boolean(res.productName || res.imageUrl || res.description || res.aiSellingPoints)
      toast(hasAnyField ? '链接提取完成' : '链接已请求，未提取到可用字段', hasAnyField ? 'success' : 'warning')
    },
    onError: (e: Error) => {
      const message = `链接提取失败（POST ${PRODUCT_ENDPOINTS.extractFromLink}）：${getErrorMessage(e)}（productLink=${productLink.trim() || '空'}）`
      setExtractError(message)
      toast(message, 'error')
    },
  })

  const handleClose = useCallback(() => {
    if (extractMut.isPending) return
    onClose()
  }, [extractMut.isPending, onClose])

  useEffect(() => {
    if (!open) {
      setProductLink('')
      setResult(null)
      setExtractError('')
    }
  }, [open])

  const canExtract = productLink.trim().length > 0 && !extractMut.isPending

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>商品链接提取</DialogTitle>
      <DialogContent
        dividers
        data-testid="product-link-extract-dialog"
        data-contract-source={PRODUCT_ENDPOINTS.extractFromLink}
        data-link-length={productLink.trim().length}
        data-has-result={String(Boolean(result))}
        data-no-empty-product-fill="true"
      >
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          <TextField
            label="商品链接"
            value={productLink}
            onChange={(e) => { setProductLink(e.target.value); setResult(null) }}
            placeholder="https://..."
            size="small"
            fullWidth
          />
          <Alert
            severity="info"
            data-testid="product-link-extract-contract-alert"
            data-no-store-sync-endpoint="true"
            data-contract-source={PRODUCT_ENDPOINTS.extractFromLink}
          >
            店铺批量同步没有后端真实端点；当前使用商品链接提取作为可执行录入链路。
          </Alert>
          {extractError ? (
            <Alert
              severity="error"
              data-testid="product-link-extract-error"
              data-contract-source={PRODUCT_ENDPOINTS.extractFromLink}
              data-input-preserved="true"
            >
              {extractError}。失败会保留当前链接输入，不会填入空商品。
            </Alert>
          ) : null}
          {result && (
            <Paper
              variant="outlined"
              data-testid="product-link-extract-result"
              data-contract-source={PRODUCT_ENDPOINTS.extractFromLink}
              sx={{ p: 1.5 }}
            >
              <Stack spacing={1.25}>
                {result.imageUrl && (
                  <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                    <img
                      src={result.imageUrl}
                      alt=""
                      style={{ maxWidth: '100%', maxHeight: 160, objectFit: 'contain', borderRadius: 4 }}
                    />
                  </Box>
                )}
                {([
                  ['商品名称', result.productName],
                  ['主图链接', result.imageUrl],
                  ['商品描述', result.description],
                  ['AI卖点', result.aiSellingPoints],
                ] as [string, string | undefined][]).map(([label, value]) => (
                  <Box key={label}>
                    <Typography variant="caption" color="text.secondary">{label}</Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                      {value || '--'}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </Paper>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={extractMut.isPending}>取消</Button>
        <Button
          variant="outlined"
          startIcon={extractMut.isPending ? <CircularProgress size={14} /> : <LinkIcon />}
          onClick={() => extractMut.mutate()}
          disabled={!canExtract}
        >
          开始提取
        </Button>
        <Button
          variant="contained"
          onClick={() => result && onApply(productLink.trim(), result)}
          disabled={!result || extractMut.isPending}
        >
          填入新增商品
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Batch AI Extraction Dialog ─────────────────────────────────────────────
function BatchExtractDialog({ open, ids, products, onClose }: {
  open: boolean; ids: number[]; products: DyProduct[]; onClose: () => void
}) {
  const [items, setItems] = useState<Array<BatchExtractItem & { error?: string }>>([])
  const [running, setRunning] = useState(false)
  const [done, setDone] = useState(false)
  const qc = useQueryClient()

  const start = useCallback(async () => {
    const list = buildBatchExtractItems(ids, products)
    setItems(list)
    setRunning(true)
    setDone(false)
    for (let i = 0; i < list.length; i++) {
      setItems(prev => prev.map((it, idx) => idx === i ? { ...it, status: 'loading' } : it))
      try {
        await productApi.scriptGenerate(list[i].id)
        setItems(prev => prev.map((it, idx) => idx === i ? { ...it, status: 'done' } : it))
      } catch (error) {
        const message = `POST ${PRODUCT_ENDPOINTS.scriptGenerate}: ${getErrorMessage(error)}（productId=${list[i].id}; productName=${list[i].name}）`
        setItems(prev => prev.map((it, idx) => idx === i ? { ...it, status: 'error', error: message } : it))
      }
    }
    setRunning(false)
    setDone(true)
    qc.invalidateQueries({ queryKey: ['products'] })
  }, [ids, products, qc])

  const handleOpen = useCallback(() => {
    if (!running && !done) start()
  }, [running, done, start])

  useEffect(() => {
    if (!open) {
      setItems([])
      setRunning(false)
      setDone(false)
      return
    }
    handleOpen()
  }, [open, handleOpen])

  const { doneCount, errCount, progress } = getBatchExtractSummary(items)
  const errorItems = items.filter(item => item.status === 'error')

  return (
    <Dialog open={open} onClose={() => !running && onClose()} maxWidth="sm" fullWidth>
      <DialogTitle>批量AI提炼卖点</DialogTitle>
      <DialogContent
        dividers
        data-testid="product-batch-extract-dialog"
        data-contract-source={PRODUCT_ENDPOINTS.scriptGenerate}
        data-selected-count={ids.length}
        data-done-count={doneCount}
        data-error-count={errCount}
        data-no-local-selling-point-write="true"
      >
        {items.length === 0 ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}><CircularProgress /></Box>
        ) : (
          <>
            <LinearProgress variant="determinate" value={progress} sx={{ mb: 2, height: 8, borderRadius: 4 }} />
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              {done ? `完成：${doneCount} 成功 / ${errCount} 失败` : `正在提炼... ${doneCount + errCount} / ${items.length}`}
            </Typography>
            {done && errorItems.length > 0 ? (
              <Alert
                severity="error"
                sx={{ mb: 1.5 }}
                data-testid="product-batch-extract-error"
                data-contract-source={PRODUCT_ENDPOINTS.scriptGenerate}
                data-no-local-selling-point-write="true"
              >
                批量 AI 提炼部分失败（POST {PRODUCT_ENDPOINTS.scriptGenerate}）。失败商品会保留在列表中，请按错误商品重试；本次不会本地改写商品卖点。
              </Alert>
            ) : null}
            <Stack spacing={0.75} sx={{ maxHeight: 320, overflowY: 'auto' }}>
              {items.map(it => (
                <Stack key={it.id} direction="row" alignItems="center" spacing={1}
                  data-testid="product-batch-extract-row-surface"
                  sx={(theme) => ({
                    px: 1.5,
                    py: 0.75,
                    bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                  })}>
                  {it.status === 'loading' && <CircularProgress size={14} />}
                  {it.status === 'done' && <CheckCircleIcon sx={{ fontSize: 16, color: 'success.main' }} />}
                  {it.status === 'error' && <ErrorOutlineIcon sx={{ fontSize: 16, color: 'error.main' }} />}
                  {it.status === 'pending' && <Box sx={{ width: 16, height: 16, bgcolor: 'action.disabledBackground', borderRadius: '50%' }} />}
                  <Typography variant="body2" sx={{ flex: 1 }}>{it.name}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {it.status === 'pending' ? '等待中' : it.status === 'loading' ? '提炼中' : it.status === 'done' ? '完成' : '失败'}
                  </Typography>
                  {it.error ? <Typography variant="caption" color="error.main" sx={{ maxWidth: 240 }} noWrap>{it.error}</Typography> : null}
                </Stack>
              ))}
            </Stack>
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={running}>关闭</Button>
        {!running && !done && <Button variant="contained" onClick={start}>开始提炼</Button>}
      </DialogActions>
    </Dialog>
  )
}

// ─── Export to Short Video Dialog ───────────────────────────────────────────
function ExportSvDialog({ open, product, onClose }: {
  open: boolean; product: DyProduct | null; onClose: () => void
}) {
  const toast = useToast()
  const navigate = useNavigate()
  const [style, setStyle] = useState('种草')
  const [duration, setDuration] = useState(60)
  const [exportError, setExportError] = useState('')
  useEffect(() => {
    if (open) setExportError('')
  }, [open, product?.id])
  const mut = useMutation({
    mutationFn: () => exportProductToShortVideo({ productId: product!.id, style, duration }),
    onSuccess: (res) => {
      toast('导出成功，正在跳转短视频项目', 'success')
      onClose()
      navigate(`${shortvideoRoutes.workbench}?projectId=${res.projectId}`)
    },
    onError: (error: Error) => {
      const message = `导出短视频失败（POST ${PRODUCT_ENDPOINTS.exportShortVideo}）：${getErrorMessage(error)}（productId=${product?.id ?? '-'}; productName=${product?.productName ?? '未知商品'}; style=${style}; duration=${duration}）`
      setExportError(message)
      toast(message, 'error')
    },
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>导出为短视频脚本</DialogTitle>
      <DialogContent
        dividers
        data-testid="product-export-sv-dialog"
        data-contract-source={PRODUCT_ENDPOINTS.exportShortVideo}
        data-product-id={product?.id ?? ''}
        data-style={style}
        data-duration={duration}
        data-no-local-shortvideo-project="true"
      >
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          {exportError ? (
            <Alert
              severity="error"
              data-testid="product-export-sv-error"
              data-contract-source={PRODUCT_ENDPOINTS.exportShortVideo}
              data-dialog-input-preserved="true"
            >
              {exportError}。失败不会创建本地假项目，也不会跳转短视频工作台。
            </Alert>
          ) : null}
          <TextField select label="短视频风格" value={style} onChange={e => setStyle(e.target.value)} size="small" fullWidth>
            {SV_STYLES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
          </TextField>
          <TextField select label="时长目标" value={duration} onChange={e => setDuration(Number(e.target.value))} size="small" fullWidth>
            {SV_DURATIONS.map(d => <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>)}
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => mut.mutate()} disabled={mut.isPending}>
          {mut.isPending ? <CircularProgress size={16} sx={{ mr: 1 }} /> : null}确认导出
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Product Detail Drawer ───────────────────────────────────────────────────
function ProductDetailDrawer({ product, onClose, onEdit }: {
  product: DyProduct | null; onClose: () => void; onEdit: (p: DyProduct) => void
}) {
  const theme = useTheme()
  const toast = useToast()
  const navigate = useNavigate()
  const [tab, setTab] = useState(0)
  const [exportSvOpen, setExportSvOpen] = useState(false)

  const { data: scripts, isError: scriptsIsError, error: scriptsError, refetch: refetchScripts } = useQuery({
    queryKey: ['product-scripts', product?.id],
    queryFn: () => productApi.scriptList({ productId: product!.id, rows: 50 }),
    enabled: !!product && tab === 2,
  })

  const { data: effectiveness, isError: effectivenessIsError, error: effectivenessError, refetch: refetchEffectiveness } = useQuery({
    queryKey: ['product-effectiveness', product?.id],
    queryFn: () => productApi.effectivenessRanking({ productId: product!.id, rows: 10 }),
    enabled: !!product && tab === 3,
  })

  const { data: gmvContrib, isError: gmvContribIsError, error: gmvContribError, refetch: refetchGmvContrib } = useQuery({
    queryKey: ['product-gmv-contrib', product?.id],
    queryFn: () => productApi.scriptUsageList(product!.id),
    enabled: !!product && tab === 4,
  })

  const { data: salesHistory, isError: salesHistoryIsError, error: salesHistoryError, refetch: refetchSalesHistory } = useQuery({
    queryKey: ['product-sales-history-channel', product?.id],
    queryFn: () => productApi.salesHistorySearch({ productId: product!.id, page: 0, rows: 1000 }),
    enabled: !!product && tab === 4,
  })

  const aiExtractMut = useMutation({
    mutationFn: () => productApi.scriptGenerate(product!.id),
    onSuccess: () => toast('AI卖点提炼完成', 'success'),
    onError: (error: Error) => toast(`AI 卖点提炼失败：${getErrorMessage(error)}`, 'error'),
  })

  const effectList = effectiveness?.list ?? []
  const effectivenessChartColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main
  const effectivenessChartAreaColor = alpha(
    theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main,
    theme.palette.mode === 'dark' ? 0.18 : 0.08,
  )
  const chartOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: effectList.map((_, i) => `第${i + 1}场`) },
    yAxis: { type: 'value', min: 0, max: 10 },
    series: [{ name: '效果评分', type: 'line', smooth: true,
      data: effectList.map(r => Number(r.avgScore ?? 0)),
      lineStyle: { color: effectivenessChartColor }, itemStyle: { color: effectivenessChartColor },
      areaStyle: { color: effectivenessChartAreaColor } }],
  }

  if (!product) return null
  const sellingPointLines = (product.sellingPoints ?? '').split(/[\n,，；;]+/).map(s => s.trim()).filter(Boolean)
  const salesRows = salesHistory?.list ?? []
  const gmvContribRows = Array.isArray(gmvContrib) ? gmvContrib : gmvContrib?.list ?? []
  const usageStats = Array.isArray(gmvContrib) ? undefined : gmvContrib?.stats
  const usageStatsVisible = hasUsageStats(usageStats)
  const usageStyleEntries = topUsageEntries(usageStats?.byStyle)
  const usageTypeEntries = topUsageEntries(usageStats?.byType, 4)
  const usageSourceEntries = topUsageEntries(usageStats?.bySource, 4)
  const totalSalesAmount = salesRows.reduce((sum, item) => sum + Number(item.saleAmount ?? item.revenue ?? 0), 0)
  const channelRows = (() => {
    const totals = new Map<string, number>()
    for (const item of salesRows) {
      const label = String(item.channelSource ?? item.platform ?? '未标记渠道').trim() || '未标记渠道'
      const amount = Number(item.saleAmount ?? item.revenue ?? 0)
      totals.set(label, (totals.get(label) ?? 0) + amount)
    }
    const total = Array.from(totals.values()).reduce((sum, value) => sum + value, 0)
    return Array.from(totals.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([label, amount], index) => ({
        label,
        amount,
        pct: total > 0 ? Math.round((amount / total) * 1000) / 10 : 0,
        colorTone: CHANNEL_TONES[index % CHANNEL_TONES.length],
      }))
  })()

  return (
    <Drawer anchor="right" open={!!product} onClose={onClose}
      PaperProps={{
        'data-testid': 'product-detail-drawer',
        'data-contract-source': `${PRODUCT_ENDPOINTS.scriptList}|${PRODUCT_ENDPOINTS.effectivenessRanking}|${PRODUCT_ENDPOINTS.scriptUsageList}|${PRODUCT_ENDPOINTS.salesHistorySearch}`,
        'data-product-id': product.id,
        'data-active-tab': tab,
        sx: { width: { xs: '100vw', sm: 760, lg: 840 }, maxWidth: '100vw' },
      }}>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {/* Header */}
        <Box
          data-testid="product-detail-compact-header"
          sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 1.25, borderBottom: '1px solid', borderColor: 'divider' }}
        >
          {getProductThumbUrl(product.mainImage || product.imageUrl) ? (
            <img
              src={getProductThumbUrl(product.mainImage || product.imageUrl)}
              alt=""
              style={{ width: 44, height: 44, objectFit: 'cover', borderRadius: 6, flexShrink: 0 }}
            />
          ) : null}
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="subtitle1" fontWeight={700} noWrap>{product.productName}</Typography>
            <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap sx={{ mt: 0.5 }}>
              <Chip label={product.category || '未分类'} size="small" variant="outlined" />
              <Chip label={product.brand || '未填品牌'} size="small" variant="outlined" />
              <Chip label={product.status === 1 ? '上架' : '下架'} size="small" color={product.status === 1 ? 'success' : 'default'} />
              <Chip label={`¥${Number(product.price ?? 0).toFixed(2)}`} size="small" color="primary" variant="outlined" />
              <Chip label={`库存 ${product.inventory ?? 0}${product.unit ?? '件'}`} size="small" variant="outlined" />
            </Stack>
          </Box>
          <IconButton size="small" onClick={onClose}><CloseIcon /></IconButton>
        </Box>

        {/* Tabs */}
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto"
          data-testid="product-detail-compact-tabs"
          sx={{ borderBottom: '1px solid', borderColor: 'divider', minHeight: 36,
            '& .MuiTab-root': { minHeight: 36, fontSize: 12, px: 1.5 } }}>
          <Tab label="基本信息" />
          <Tab label="AI卖点" />
          <Tab label="话术版本" />
          <Tab label="效果历史" />
          <Tab label="GMV贡献" />
        </Tabs>

        {/* Tab Content */}
        <Box sx={{ flex: 1, overflow: 'auto', p: 1.5 }}>
          {tab === 0 && (
            <Grid container spacing={1}>
              {product.mainImage && (
                <Grid item xs={12} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1, height: '100%', minHeight: 178, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 1 }}>
                    <img src={getProductPreviewUrl(product.mainImage)} alt="" style={{ maxWidth: '100%', maxHeight: 170, objectFit: 'contain', borderRadius: 6 }} />
                  </Paper>
                </Grid>
              )}
              <Grid item xs={12} sm={product.mainImage ? 8 : 12}>
                <Grid container spacing={1}>
                  {([
                    ['商品名称', product.productName],
                    ['商品编码', product.productCode],
                    ['品类', product.category],
                    ['品牌', product.brand],
                    ['售价', `¥${Number(product.price ?? 0).toFixed(2)}`],
                    ['成本价', `¥${Number(product.costPrice ?? 0).toFixed(2)}`],
                    ['利润率', `${toDisplayPct(product.profitMarginPct)}%`],
                    ['库存', `${product.inventory ?? 0} ${product.unit ?? '件'}`],
                    ['状态', product.status === 1 ? '上架' : '下架'],
                    ['创建时间', formatDate(product.createTime)],
                  ] as [string, string | number][]).map(([k, v]) => (
                    <DetailField key={k} label={k} value={v} />
                  ))}
                </Grid>
              </Grid>
              {product.productLink && (
                <DetailField label="商品链接" value={product.productLink} wide />
              )}
              {product.description && (
                <DetailField label="商品描述" value={product.description} wide />
              )}
            </Grid>
          )}

          {tab === 1 && (
            <Box>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Box>
                  <Typography variant="subtitle2">AI提炼卖点</Typography>
                  <Typography variant="caption" color="text.secondary">当前 {sellingPointLines.length} 条，可重新调用真实生成接口。</Typography>
                </Box>
                <Button size="small" variant="outlined" startIcon={aiExtractMut.isPending ? <CircularProgress size={14} /> : <AutoAwesomeIcon />}
                  onClick={() => aiExtractMut.mutate()} disabled={aiExtractMut.isPending}>
                  重新AI提炼
                </Button>
              </Stack>
              {aiExtractMut.isError ? (
                <Alert
                  severity="error"
                  sx={{ mb: 1.5 }}
                  data-testid="product-detail-ai-extract-error"
                  data-contract-source={PRODUCT_ENDPOINTS.scriptGenerate}
                  data-no-local-selling-point-write="true"
                >
                  AI 卖点提炼失败（POST {PRODUCT_ENDPOINTS.scriptGenerate}）：{getErrorMessage(aiExtractMut.error)}（productId={product.id}; productName={product.productName}）。失败不会本地改写卖点。
                </Alert>
              ) : null}
              {sellingPointLines.length > 0 ? (
                <Box
                  data-testid="product-selling-point-grid"
                  sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 1 }}
                >
                  {sellingPointLines.map((pt, i) => (
                    <Paper
                      key={i}
                      variant="outlined"
                      data-testid="product-selling-point-surface"
                      sx={(theme) => ({
                        px: 2,
                        py: 1.25,
                        borderRadius: 2,
                        borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.42 : 0.24),
                        bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
                      })}>
                      <Stack direction="row" spacing={1} alignItems="flex-start">
                        <Box sx={{ minWidth: 20, height: 20, borderRadius: '50%', bgcolor: 'primary.main',
                          color: 'primary.contrastText', fontSize: 11, fontWeight: 700,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', mt: 0.1 }}>
                          {i + 1}
                        </Box>
                        <Typography variant="body2">{pt}</Typography>
                      </Stack>
                    </Paper>
                  ))}
                </Box>
              ) : (
                <Box sx={{ textAlign: 'center', py: 4, color: 'text.secondary' }}>
                  <AutoAwesomeIcon sx={{ fontSize: 40, mb: 1, opacity: 0.3 }} />
                  <Typography variant="body2">暂无AI卖点，点击「重新AI提炼」生成</Typography>
                </Box>
              )}
              {product.highlights && (
                <Box mt={2}>
                  <Divider sx={{ mb: 1.5 }} />
                  <Typography variant="caption" color="text.secondary">商品亮点</Typography>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>{product.highlights}</Typography>
                </Box>
              )}
            </Box>
          )}

          {tab === 2 && (
            <Box>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Box>
                  <Typography variant="subtitle2">话术版本列表</Typography>
                  <Typography variant="caption" color="text.secondary">读取商品话术版本，支持跳转完整工作台。</Typography>
                </Box>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Button size="small" variant="contained" startIcon={<AutoAwesomeIcon />}
                    onClick={() => navigate(`/org/product/${product.id}/scripts`)}>
                    生成话术
                  </Button>
                  <Chip label={`共 ${scripts?.total ?? 0} 条`} size="small" variant="outlined" />
                </Stack>
              </Stack>
              {scriptsIsError ? (
                <Alert
                  severity="error"
                  action={<Button color="inherit" size="small" onClick={() => void refetchScripts()}>重试</Button>}
                  sx={{ mb: 1 }}
                  data-testid="product-detail-script-list-error"
                  data-contract-source={PRODUCT_ENDPOINTS.scriptList}
                  data-no-static-script-fallback="true"
                >
                  话术版本加载失败（POST {PRODUCT_ENDPOINTS.scriptList}）：{getErrorMessage(scriptsError)}（productId={product.id}; productName={product.productName}）
                </Alert>
              ) : null}
              <Stack spacing={0.75}>
                {(scripts?.list ?? []).map(sc => (
                  <Paper key={sc.id} variant="outlined" sx={{ p: 1, borderRadius: 1 }}>
                    <Stack direction="row" alignItems="center" spacing={1} mb={0.5}>
                      <Typography variant="body2" fontWeight={600} flex={1}>{sc.scriptTitle}</Typography>
                      {sc.status === 1 && <Chip label="激活" size="small" color="success" />}
                      <Chip label={sc.style || sc.scriptType || '通用'} size="small" variant="outlined" />
                    </Stack>
                    <Typography variant="caption" color="text.secondary"
                      sx={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {sc.scriptContent}
                    </Typography>
                    <Stack direction="row" spacing={1} mt={0.75}>
                      <Typography variant="caption" color="text.secondary">使用 {sc.useCount ?? 0} 次</Typography>
                      <Typography variant="caption" color="text.secondary">评分 {Number(sc.rating ?? 0).toFixed(1)}</Typography>
                      <Typography variant="caption" color="text.secondary">{formatDate(sc.createTime)}</Typography>
                    </Stack>
                  </Paper>
                ))}
                {(scripts?.list ?? []).length === 0 && (
                  <Box
                    data-testid="product-detail-script-empty"
                    data-contract-source={PRODUCT_ENDPOINTS.scriptList}
                    data-no-static-script-fallback="true"
                    sx={{ textAlign: 'center', py: 3, color: 'text.secondary' }}
                  >
                    <Typography variant="body2">暂无话术版本</Typography>
                    <Button size="small" variant="outlined" startIcon={<AutoAwesomeIcon />} sx={{ mt: 1.5 }}
                      onClick={() => navigate(`/org/product/${product.id}/scripts`)}>
                      立即生成
                    </Button>
                  </Box>
                )}
              </Stack>
            </Box>
          )}

          {tab === 3 && (
            <Box>
              <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
                <Box>
                  <Typography variant="subtitle2">效果历史趋势</Typography>
                  <Typography variant="caption" color="text.secondary">按话术效果排名接口返回绘制，不补静态数据。</Typography>
                </Box>
                <Chip label={`${effectList.length} 条`} size="small" variant="outlined" />
              </Stack>
              {effectivenessIsError ? (
                <Alert
                  severity="error"
                  action={<Button color="inherit" size="small" onClick={() => void refetchEffectiveness()}>重试</Button>}
                  sx={{ mb: 1 }}
                  data-testid="product-detail-effectiveness-error"
                  data-contract-source={PRODUCT_ENDPOINTS.effectivenessRanking}
                  data-no-static-ranking-fallback="true"
                >
                  效果历史加载失败（POST {PRODUCT_ENDPOINTS.effectivenessRanking}）：{getErrorMessage(effectivenessError)}（productId={product.id}; productName={product.productName}）
                </Alert>
              ) : null}
              {effectList.length > 0 ? (
                <Box
                  data-testid="product-effectiveness-chart-surface"
                  data-chart-color={effectivenessChartColor}
                  data-area-color={effectivenessChartAreaColor}
                >
                  <LazyECharts option={chartOption} style={{ height: 190 }} />
                </Box>
              ) : (
                <Box
                  data-testid="product-detail-effectiveness-empty"
                  data-contract-source={PRODUCT_ENDPOINTS.effectivenessRanking}
                  data-no-static-ranking-fallback="true"
                  sx={{ textAlign: 'center', py: 4, color: 'text.secondary' }}
                >
                  <Typography variant="body2">暂无效果历史数据</Typography>
                </Box>
              )}
              <Divider sx={{ my: 1.5 }} />
              <Typography variant="subtitle2" mb={0.75}>TOP 场次表现</Typography>
              <Stack spacing={0.75}>
                {effectList.slice(0, 5).map((r, i) => (
                  <Stack key={i} direction="row" alignItems="center" spacing={1}
                    data-testid="product-effectiveness-row-surface"
                    sx={(theme) => ({
                      px: 1.5,
                      py: 1,
                      bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 1,
                    })}>
                    <Typography variant="caption" sx={{ minWidth: 20, fontWeight: 700, color: 'primary.main' }}>{i + 1}</Typography>
                    <Typography variant="body2" flex={1}>{r.productName ?? `场次 ${i + 1}`}</Typography>
                    <Chip label={`${r.avgScore.toFixed(1)}分`} size="small"
                      color={r.avgScore >= 8 ? 'success' : r.avgScore >= 5 ? 'warning' : 'default'} />
                  </Stack>
                ))}
              </Stack>
            </Box>
          )}

          {tab === 4 && (
            <Box>
              {salesHistoryIsError ? (
                <Alert
                  severity="error"
                  action={<Button color="inherit" size="small" onClick={() => void refetchSalesHistory()}>重试</Button>}
                  sx={{ mb: 1.5 }}
                  data-testid="product-detail-sales-history-error"
                  data-contract-source={PRODUCT_ENDPOINTS.salesHistorySearch}
                  data-no-dashboard-gmv-fallback="true"
                >
                  销售记录加载失败（POST {PRODUCT_ENDPOINTS.salesHistorySearch}）：{getErrorMessage(salesHistoryError)}（productId={product.id}; productName={product.productName}）
                </Alert>
              ) : null}
              {gmvContribIsError ? (
                <Alert
                  severity="error"
                  action={<Button color="inherit" size="small" onClick={() => void refetchGmvContrib()}>重试</Button>}
                  sx={{ mb: 1.5 }}
                  data-testid="product-detail-gmv-contrib-error"
                  data-contract-source={PRODUCT_ENDPOINTS.scriptUsageList}
                  data-no-dashboard-gmv-fallback="true"
                >
                  GMV 贡献加载失败（POST {PRODUCT_ENDPOINTS.scriptUsageList}）：{getErrorMessage(gmvContribError)}（productId={product.id}; productName={product.productName}）
                </Alert>
              ) : null}
              {salesRows.length > 0 && (
                <Paper
                  variant="outlined"
                  data-testid="product-gmv-summary-surface"
                  sx={(theme) => ({
                    p: 1.5,
                    mb: 1.5,
                    borderRadius: 1,
                    bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
                    borderColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.42 : 0.24),
                  })}
                >
                  <Typography variant="caption" color="text.secondary">销售记录累计GMV</Typography>
                  <Typography variant="h5" fontWeight={700} color="success.main">
                    ¥{totalSalesAmount >= 10000 ? (totalSalesAmount / 10000).toFixed(1) + '万' : totalSalesAmount.toFixed(0)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    来自 /product/sales-history/search，效果趋势接口按话术版本 versionId 查询。
                  </Typography>
                </Paper>
              )}

              {channelRows.length > 0 && (
                <Box mb={1.5}>
                  <Typography variant="subtitle2" mb={0.75}>渠道分布</Typography>
                  <Stack spacing={0.75}>
                    {channelRows.map(ch => (
                      <Stack key={ch.label} direction="row" alignItems="center" spacing={1}>
                        <Typography variant="caption" sx={{ minWidth: 90, color: 'text.secondary' }}>{ch.label}</Typography>
                        <Box
                          flex={1}
                          data-testid="product-channel-track-surface"
                          sx={(theme) => ({
                            bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.08) : alpha(theme.palette.common.black, 0.08),
                            borderRadius: 1,
                            height: 8,
                            overflow: 'hidden',
                          })}
                        >
                          <Box sx={{ width: `${ch.pct}%`, height: '100%', bgcolor: `${ch.colorTone}.main`, borderRadius: 1 }} />
                        </Box>
                        <Typography variant="caption" fontWeight={600} sx={{ minWidth: 36, color: `${ch.colorTone}.main` }}>{ch.pct}%</Typography>
                        <Typography variant="caption" color="text.secondary">
                          ¥{ch.amount >= 10000 ? `${(ch.amount / 10000).toFixed(1)}万` : ch.amount.toFixed(0)}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                </Box>
              )}

              {usageStatsVisible && usageStats ? (
                <Paper
                  variant="outlined"
                  data-testid="product-script-usage-stats-surface"
                  sx={(theme) => ({
                    p: 1.5,
                    mb: 1.5,
                    borderRadius: 1,
                    bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.info.main, 0.14) : alpha(theme.palette.info.main, 0.06),
                    borderColor: alpha(theme.palette.info.main, theme.palette.mode === 'dark' ? 0.4 : 0.22),
                  })}
                >
                  <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1} mb={1}>
                    <Box>
                      <Typography variant="subtitle2">话术使用统计</Typography>
                      <Typography variant="caption" color="text.secondary">
                        来自 /product/script/usage-list，当前后端返回统计对象，不是 GMV 明细列表。
                      </Typography>
                    </Box>
                    <Chip label={`${usageStats.activeScripts}/${usageStats.totalScripts} 激活`} size="small" color="info" variant="outlined" />
                  </Stack>
                  <Grid container spacing={1}>
                    <DetailField label="话术总数" value={usageStats.totalScripts} />
                    <DetailField label="激活话术" value={usageStats.activeScripts} />
                    <DetailField label="平均时长" value={`${usageStats.avgDuration.toFixed(0)} 秒`} />
                    <DetailField label="累计Token" value={usageStats.totalTokens.toLocaleString()} />
                  </Grid>
                  {usageStyleEntries.length > 0 && (
                    <Box mt={1}>
                      <Typography variant="caption" color="text.secondary">风格分布</Typography>
                      <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap" mt={0.75}>
                        {usageStyleEntries.map(([label, count]) => (
                          <Chip key={label} label={`${label} ${count}`} size="small" variant="outlined" />
                        ))}
                      </Stack>
                    </Box>
                  )}
                  {(usageTypeEntries.length > 0 || usageSourceEntries.length > 0) && (
                    <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap" mt={1}>
                      {usageTypeEntries.map(([label, count]) => (
                        <Chip key={`type-${label}`} label={`类型 ${label}: ${count}`} size="small" />
                      ))}
                      {usageSourceEntries.map(([label, count]) => (
                        <Chip key={`source-${label}`} label={`来源 ${label}: ${count}`} size="small" />
                      ))}
                    </Stack>
                  )}
                </Paper>
              ) : null}

              <Typography variant="subtitle2" mb={0.75}>TOP5 贡献场次</Typography>
              {gmvContribRows.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 4, color: 'text.secondary' }}>
                  <Typography variant="body2">暂无GMV贡献明细</Typography>
                  {usageStatsVisible && (
                    <Typography variant="caption" color="text.secondary">
                      当前接口只返回话术统计，未返回场次级 GMV 贡献数组。
                    </Typography>
                  )}
                </Box>
              ) : (
                <Stack spacing={0.75}>
                  {gmvContribRows.slice(0, 5).map((r: GmvContribItem, i: number) => {
                    const gmv = Number(r.totalGmv ?? r.gmv ?? 0)
                    const sessionTitle = String(r.sessionTitle ?? r.liveTitle ?? `场次 ${i + 1}`)
                    const orders = Number(r.totalOrders ?? r.orders ?? 0)
                    const sessionId = Number(r.sessionId ?? r.liveSessionId ?? 0)
                    return (
                      <Paper key={i} variant="outlined" sx={{ px: 1.5, py: 1, borderRadius: 1.5 }}>
                        <Stack direction="row" alignItems="center" spacing={1}>
                          <Typography variant="caption" sx={{ minWidth: 18, fontWeight: 700,
                            color: i < 3 ? 'warning.main' : 'text.disabled' }}>{i + 1}</Typography>
                          <Typography variant="body2" flex={1} noWrap>{sessionTitle}</Typography>
                          <Typography variant="caption" color="text.secondary">{orders}单</Typography>
                          <Typography variant="caption" fontWeight={600} color="success.main">
                            ¥{gmv >= 10000 ? (gmv / 10000).toFixed(1) + '万' : gmv.toFixed(0)}
                          </Typography>
                          {sessionId > 0 && (
                            <Button size="small" variant="text" sx={{ fontSize: 11, py: 0, px: 0.5, minWidth: 0 }}
                              onClick={() => { onClose(); navigate(`/org/live/sessions/${sessionId}`) }}>
                              查看
                            </Button>
                          )}
                        </Stack>
                      </Paper>
                    )
                  })}
                </Stack>
              )}

              <Divider sx={{ my: 1.5 }} />
              <Typography variant="subtitle2" mb={0.75}>话术版本GMV对比</Typography>
              <Stack spacing={0.5}>
                {gmvContribRows.length === 0 && usageStatsVisible && (
                  <Stack direction="row" alignItems="center" spacing={1}
                    data-testid="product-version-compare-surface"
                    sx={(theme) => ({
                      px: 1,
                      py: 0.75,
                      bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 1,
                    })}>
                    <Typography variant="caption" color="text.secondary" flex={1}>
                      后端当前未返回版本 GMV 对比明细，已展示话术使用统计。
                    </Typography>
                  </Stack>
                )}
                {gmvContribRows.slice(0, 3).map((r: GmvContribItem, i: number) => (
                  <Stack key={i} direction="row" alignItems="center" spacing={1}
                    data-testid="product-version-compare-surface"
                    sx={(theme) => ({
                      px: 1,
                      py: 0.75,
                      bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 1,
                    })}>
                    <Typography variant="caption" color="text.secondary" flex={1}>
                      {String(r.versionLabel ?? `v${i + 1} → v${i + 2}`)}
                    </Typography>
                    {r.gmvLift != null && (
                      <Chip label={`+${Number(r.gmvLift).toFixed(0)}%`} size="small" color="success" />
                    )}
                  </Stack>
                ))}
              </Stack>
            </Box>
          )}
        </Box>

        {/* Footer actions */}
        <Box sx={{ px: 2.5, py: 1.5, borderTop: '1px solid', borderColor: 'divider', display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button size="small" variant="outlined" startIcon={<VideoLibraryIcon />} onClick={() => setExportSvOpen(true)}>
            导出为短视频脚本
          </Button>
          <Box flex={1} />
          <Button size="small" onClick={onClose}>关闭</Button>
          <Button size="small" variant="contained" onClick={() => { onClose(); onEdit(product) }}>编辑商品</Button>
        </Box>
      </Box>

      <ExportSvDialog open={exportSvOpen} product={product} onClose={() => setExportSvOpen(false)} />
    </Drawer>
  )
}

// ─── Main Page ───────────────────────────────────────────────────────────────
export default function ProductsPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [query, setQuery] = useState({ page: 0, rows: 20, productName: '', category: '', status: undefined as number | undefined })
  const [search, setSearch] = useState(query)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<ProductSave>>(defaultForm)
  const [pctInput, setPctInput] = useState('0')
  const [deleteTarget, setDeleteTarget] = useState<DyProduct | null>(null)
  const [selection, setSelection] = useState<GridRowSelectionModel>([])
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false)
  const [batchExtractOpen, setBatchExtractOpen] = useState(false)
  const [linkExtractOpen, setLinkExtractOpen] = useState(false)
  const [detailProduct, setDetailProduct] = useState<DyProduct | null>(null)
  const [formError, setFormError] = useState('')
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['products', search],
    queryFn: () => productApi.list(search),
  })
  const allProducts = data?.list ?? []
  const listedCount = allProducts.length
  const listedActiveCount = allProducts.filter(p => p.status === 1).length
  const noSellingPointCount = allProducts.filter(p => !String(p.sellingPoints ?? '').trim()).length
  const lowInventoryCount = allProducts.filter(p => Number(p.inventory ?? 0) <= 10).length
  const listContext = () => {
    const statusLabel = query.status == null ? '全部' : STATUS_OPTIONS.find(option => option.value === query.status)?.label ?? String(query.status)
    return `route=/org/product/list; keyword=${query.productName.trim() || '空'}; category=${query.category || '全部'}; status=${statusLabel}; page=${search.page}; rows=${search.rows}`
  }
  const formContext = () => {
    const statusLabel = form.status == null ? '-' : STATUS_OPTIONS.find(option => option.value === form.status)?.label ?? String(form.status)
    return `productId=${form.id ?? '-'}; productName=${String(form.productName ?? '').trim() || '未填写'}; sku=${String(form.productCode ?? '').trim() || '-'}; category=${String(form.category ?? '').trim() || '-'}; brand=${String(form.brand ?? '').trim() || '-'}; price=${form.price ?? 0}; inventory=${form.inventory ?? 0}; status=${statusLabel}; productLink=${String(form.productLink ?? '').trim() || '-'}`
  }
  const productContext = (product?: Partial<DyProduct> | null) => (
    `productId=${product?.id ?? '-'}; productName=${product?.productName ?? '未知商品'}; category=${product?.category ?? '-'}; brand=${product?.brand ?? '-'}; status=${product?.status ?? '-'}; ${listContext()}`
  )
  const selectedProductContext = () => {
    const selectedIds = (selection as number[]).join(',') || '空'
    const selectedTitles = (selection as number[])
      .map(id => allProducts.find(product => product.id === id)?.productName ?? `#${id}`)
      .join(',') || '空'
    return `selectedIds=${selectedIds}; selectedProducts=${selectedTitles}; ${listContext()}`
  }
  const listErrorMessage = `商品列表加载失败（POST ${PRODUCT_ENDPOINTS.search}）：${getErrorMessage(error)}（${listContext()}）`

  const saveMut = useMutation({
    mutationFn: (payload: Partial<ProductSave>) => productApi.save(payload),
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormError('')
      setFormOpen(false)
      qc.invalidateQueries({ queryKey: ['products'] })
    },
    onError: (e: Error) => {
      const message = `商品保存失败（POST ${PRODUCT_ENDPOINTS.save}）：${getErrorMessage(e)}（${formContext()}）`
      setFormError(message)
      toast(message, 'error')
    },
  })
  const delMut = useMutation({
    mutationFn: (row: DyProduct) => productApi.delete(row.id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setActionError('')
      setDeleteTarget(null)
      qc.invalidateQueries({ queryKey: ['products'] })
    },
    onError: (e: Error, row) => {
      const message = `商品删除失败（POST ${PRODUCT_ENDPOINTS.delete}）：${getErrorMessage(e)}（${productContext(row)}）`
      setActionError(message)
      toast(message, 'error')
    },
  })
  const batchDelMut = useMutation({
    mutationFn: () => productApi.batchDelete(selection as number[]),
    onSuccess: () => {
      toast(`已删除 ${selection.length} 件商品`, 'success')
      setSelection([])
      setBatchDeleteOpen(false)
      qc.invalidateQueries({ queryKey: ['products'] })
    },
    onError: (e: Error) => {
      const message = `商品批量删除失败（POST ${PRODUCT_ENDPOINTS.batchDelete}）：${getErrorMessage(e)}（${selectedProductContext()}）`
      setActionError(message)
      toast(message, 'error')
    },
  })

  const openAdd = useCallback(() => { setFormError(''); setForm(defaultForm); setPctInput('0'); setFormOpen(true) }, [])
  const openEdit = useCallback((row: DyProduct) => {
    setFormError('')
    setForm({
      id: row.id, productName: row.productName, productCode: row.productCode,
      category: row.category, brand: row.brand, price: row.price,
      costPrice: row.costPrice, profitMarginPct: row.profitMarginPct,
      inventory: row.inventory, unit: row.unit,
      imageUrl: row.imageUrl || row.mainImage, productLink: row.productLink,
      description: row.description, sellingPoints: row.sellingPoints, status: row.status,
    })
    setPctInput(String(toDisplayPct(row.profitMarginPct)))
    setFormOpen(true)
  }, [])
  const applyExtractResult = useCallback((productLink: string, result: ProductExtractResult) => {
    setFormError('')
    setForm({
      ...defaultForm,
      productName: result.productName ?? '',
      imageUrl: result.imageUrl ?? '',
      productLink,
      description: result.description ?? '',
      sellingPoints: result.aiSellingPoints ?? '',
    })
    setPctInput('0')
    setLinkExtractOpen(false)
    setFormOpen(true)
  }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])
  const handleSave = useCallback(() => {
    setFormError('')
    saveMut.mutate({ ...form, profitMarginPct: toStorePct(Number(pctInput)) })
  }, [form, pctInput, saveMut])
  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 60 },
    {
      field: 'imageUrl', headerName: '图', width: 54, sortable: false,
      renderCell: ({ row }) => getProductThumbUrl(row.mainImage || row.imageUrl)
        ? <img src={getProductThumbUrl(row.mainImage || row.imageUrl)} style={{ width: 38, height: 38, objectFit: 'cover', borderRadius: 4, marginTop: 3 }} />
        : <Box
            data-testid="product-image-placeholder-surface"
            sx={(theme) => ({
              width: 38,
              height: 38,
              bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.08) : alpha(theme.palette.common.black, 0.06),
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20,
            })}
          >📦</Box>,
    },
    {
      field: 'productName', headerName: '商品名称', flex: 1.4, minWidth: 180,
      renderCell: ({ row }) => (
        <Box sx={{ cursor: 'pointer', color: 'primary.main', fontWeight: 500, fontSize: 13 }}
          onClick={() => setDetailProduct(row as DyProduct)}>
          {row.productName}
        </Box>
      ),
    },
    { field: 'category', headerName: '分类', width: 76 },
    { field: 'brand', headerName: '品牌', width: 84 },
    {
      field: 'price', headerName: '售价', width: 78,
      valueFormatter: (value: number) => value != null ? `¥${Number(value).toFixed(2)}` : '--',
    },
    {
      field: 'profitMarginPct', headerName: '利润率', width: 80,
      renderCell: ({ value }) => {
        const pct = toDisplayPct(value as number)
        const tone = pct >= 40 ? 'success' : pct >= 25 ? 'warning' : 'error'
        return (
          <Chip
            label={`${pct}%`}
            size="small"
            data-testid="product-profit-chip-surface"
            sx={(theme) => ({
              bgcolor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.2 : 0.12),
              color: `${tone}.main`,
              fontWeight: 600,
              fontSize: 12,
            })}
          />
        )
      },
    },
    {
      field: 'effectivenessScore', headerName: '效果', width: 90,
      renderCell: ({ value }) => {
        const score = Number(value ?? 0)
        return (
          <Stack direction="row" alignItems="center" spacing={0.5} sx={{ width: '100%', pr: 1 }}>
            <LinearProgress variant="determinate" value={score * 10}
              sx={{ flex: 1, height: 6, borderRadius: 3,
                '& .MuiLinearProgress-bar': { bgcolor: score >= 8 ? 'success.main' : score >= 5 ? 'warning.main' : 'error.main' } }} />
            <Box sx={{ fontSize: 11, color: 'text.secondary', minWidth: 24 }}>{score.toFixed(1)}</Box>
          </Stack>
        )
      },
    },
    {
      field: 'sellingPoints', headerName: 'AI卖点', flex: 1.6, minWidth: 180, sortable: false,
      renderCell: ({ value }) => (
        <Tooltip title={String(value ?? '')} placement="top">
          <Box sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 12, color: 'text.secondary' }}>
            {String(value ?? '--')}
          </Box>
        </Tooltip>
      ),
    },
    { field: 'inventory', headerName: '库存', width: 64, type: 'number' },
    {
      field: 'status', headerName: '状态', width: 76,
      renderCell: ({ value }) => {
        const opt = STATUS_OPTIONS.find(o => o.value === value)
        return <Chip label={opt?.label ?? String(value)} color={opt?.color ?? 'default'} size="small" />
      },
    },
    { field: 'createTime', headerName: '创建时间', width: 136, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 150, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.25}>
          <Button size="small" onClick={() => setDetailProduct(row as DyProduct)}>详情</Button>
          <Button size="small" onClick={() => openEdit(row as DyProduct)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteTarget(row as DyProduct)}>删除</Button>
        </Stack>
      ),
    },
  ]
  const searchSlot = (
    <Stack direction="row" spacing={0.75} flexWrap="wrap" alignItems="center">
      <TextField size="small" label="商品名称" value={query.productName}
        onChange={e => setQuery(q => ({ ...q, productName: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 180 }} />
      <TextField select size="small" label="品类" value={query.category ?? ''}
        onChange={e => setQuery(q => ({ ...q, category: e.target.value }))} sx={{ width: 112 }}>
        <MenuItem value="">全部</MenuItem>
        {CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
      </TextField>
      <TextField select size="small" label="状态" value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))} sx={{ width: 96 }}>
        <MenuItem value="">全部</MenuItem>
        {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </Stack>
  )
  const actionSlot = (
    <Stack direction="row" spacing={0.75} flexWrap="wrap">
      {selection.length > 0 && (
        <>
          <Button size="small" variant="outlined" startIcon={<AutoAwesomeIcon />}
            onClick={() => setBatchExtractOpen(true)}>
            AI提炼卖点({selection.length})
          </Button>
          <Button size="small" variant="outlined" color="error" startIcon={<DeleteSweepIcon />}
            onClick={() => setBatchDeleteOpen(true)}>
            批量删除({selection.length})
          </Button>
        </>
      )}
      <Button size="small" variant="outlined" startIcon={<LinkIcon />} onClick={() => setLinkExtractOpen(true)}>
        链接提取
      </Button>
      <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openAdd}>新增商品</Button>
    </Stack>
  )

  return (
    <Box
      data-testid="products-workbench"
      data-contract-scope="product-assets"
      data-ready-endpoints={PRODUCT_READY_ENDPOINTS.join('|')}
      data-context-endpoints={PRODUCT_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={PRODUCT_UNSUPPORTED_ACTIONS.join('|')}
      data-keyword={query.productName.trim()}
      data-category={query.category || 'all'}
      data-status={query.status == null ? 'all' : String(query.status)}
      data-page={search.page}
      data-rows={search.rows}
      data-row-count={allProducts.length}
      data-total-count={data?.total ?? 0}
      data-selected-count={selection.length}
      data-active-count={listedActiveCount}
      data-no-selling-point-count={noSellingPointCount}
      data-low-inventory-count={lowInventoryCount}
      data-no-static-product-fallback="true"
      sx={{ p: 1.25, height: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', gap: 1, overflow: 'hidden' }}
    >
      <Paper
        variant="outlined"
        data-testid="products-compact-header"
        data-contract-source={PRODUCT_ENDPOINTS.search}
        sx={{ px: 1.25, py: 1, flexShrink: 0 }}
      >
        <Stack direction="row" alignItems="center" spacing={1.25} flexWrap="wrap" useFlexGap>
          <Box sx={{ minWidth: 150, mr: 0.5 }}>
            <Typography variant="h6" fontWeight={700} sx={{ lineHeight: 1.15 }}>商品资产</Typography>
            <Typography variant="caption" color="text.secondary">商品、卖点、库存、效果统一入口</Typography>
          </Box>
          <MetricPill label="商品总数" value={String(data?.total ?? '--')} tone="primary" />
          <MetricPill label="当前页上架" value={`${listedActiveCount}/${listedCount}`} tone="success" />
          <MetricPill label="缺卖点" value={String(noSellingPointCount)} tone={noSellingPointCount > 0 ? 'warning' : 'default'} />
          <MetricPill label="低库存" value={String(lowInventoryCount)} tone={lowInventoryCount > 0 ? 'error' : 'default'} />
          <Chip size="small" variant="outlined" color="success" label="列表已接入" data-testid="product-capability-card" data-capability-label="商品列表" data-capability-status="ready" data-contract-source={PRODUCT_ENDPOINTS.search} />
          <Chip size="small" variant="outlined" color="success" label="链接提取" data-testid="product-capability-card" data-capability-label="链接提取" data-capability-status="ready" data-contract-source={PRODUCT_ENDPOINTS.extractFromLink} onClick={() => setLinkExtractOpen(true)} />
          <Button size="small" variant="text" sx={{ px: 0.5, minWidth: 0 }} onClick={() => setLinkExtractOpen(true)}>
            立即提取
          </Button>
          <Chip size="small" variant="outlined" color="warning" label="店铺同步" data-testid="product-capability-card" data-capability-label="店铺同步" data-capability-status="degraded" data-no-store-sync-endpoint="true" data-contract-source="explicit-degraded-no-backend-endpoint" />
          <Chip size="small" variant="outlined" color="success" label="短视频导出" data-testid="product-capability-card" data-capability-label="短视频导出" data-capability-status="ready" data-contract-source={PRODUCT_ENDPOINTS.exportShortVideo} />
          <Box sx={{ flex: 1, minWidth: 12 }} />
          <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
          <Button size="small" variant="outlined" startIcon={<LinkIcon />} onClick={() => setLinkExtractOpen(true)}>
            链接提取
          </Button>
          <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openAdd}>新增商品</Button>
        </Stack>
      </Paper>

      <Box
        data-testid="products-contract-alert"
        data-list-source={PRODUCT_ENDPOINTS.search}
        data-link-extract-source={PRODUCT_ENDPOINTS.extractFromLink}
        data-export-source={PRODUCT_ENDPOINTS.exportShortVideo}
        data-no-store-sync-endpoint="true"
        data-no-static-product-fallback="true"
        data-no-server-export-request="true"
        sx={{
          px: 1.25,
          py: 0.5,
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 1,
          bgcolor: 'background.paper',
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          flexShrink: 0,
        }}
      >
        <WarningAmberIcon sx={{ fontSize: 16, color: 'warning.main' }} />
        <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
          未发现真实同步端点：店铺批量同步暂无真实端点；当前用链接提取录入商品，列表不会注入静态商品或本地假数据。
        </Typography>
      </Box>

      {isError && (
        <Alert
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
          data-testid="products-list-error"
          data-contract-source={PRODUCT_ENDPOINTS.search}
          data-no-static-product-fallback="true"
        >
          {listErrorMessage}
        </Alert>
      )}
      {actionError ? (
        <Alert
          severity="error"
          data-testid="products-action-error"
          data-contract-source={`${PRODUCT_ENDPOINTS.delete}|${PRODUCT_ENDPOINTS.batchDelete}`}
          data-no-local-delete-on-error="true"
        >
          {actionError}。失败不会本地移除商品行、清空选择或关闭关键确认上下文。
        </Alert>
      ) : null}

      {!isFetching && !isError && allProducts.length === 0 && (
        <Alert
          severity="warning"
          data-testid="products-empty"
          data-contract-source={PRODUCT_ENDPOINTS.search}
          data-no-static-product-fallback="true"
        >
          当前筛选条件下没有商品。请新增商品或清空筛选；没有商品资产时，直播话术生成、商品准备度检测和短视频导出都会降级为空结果。
        </Alert>
      )}

      <Box sx={{ flex: 1, minHeight: 0 }}>
        <StandardDataGrid
          rows={allProducts} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          checkboxSelection
          rowSelectionModel={selection}
          onRowSelectionModelChange={setSelection}
          rowHeight={44}
          columnHeaderHeight={40}
          searchSlot={searchSlot} actionSlot={actionSlot}
          toolbar={ProductGridToolbar}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          showExport={false}
          sx={{
            '& .MuiDataGrid-cell': { py: 0.25 },
            '& .MuiButton-root': { minWidth: 0, px: 0.75 },
          }}
        />
      </Box>

      {/* Edit/Add Dialog */}
      <FormDialog open={formOpen} title={form.id ? '编辑商品' : '新增商品'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack
          spacing={2}
          data-testid="product-save-dialog"
          data-contract-source={PRODUCT_ENDPOINTS.save}
          data-product-id={form.id ?? ''}
          data-input-preserved="true"
          sx={{ pt: 1 }}
        >
          {formError ? (
            <Alert
              severity="error"
              data-testid="product-save-error"
              data-contract-source={PRODUCT_ENDPOINTS.save}
              data-input-preserved="true"
            >
              {formError}。保存失败会保留当前表单输入。
            </Alert>
          ) : null}
          <TextField label="商品名称" required value={form.productName ?? ''} onChange={e => setForm(f => ({ ...f, productName: e.target.value }))} fullWidth size="small" />
          <TextField label="商品链接" value={form.productLink ?? ''} onChange={e => setForm(f => ({ ...f, productLink: e.target.value }))} fullWidth size="small" />
          <Stack direction="row" spacing={1}>
            <TextField label="商品编码" value={form.productCode ?? ''} onChange={e => setForm(f => ({ ...f, productCode: e.target.value }))} size="small" fullWidth />
            <TextField label="品牌" value={form.brand ?? ''} onChange={e => setForm(f => ({ ...f, brand: e.target.value }))} size="small" fullWidth />
          </Stack>
          <TextField label="主图链接" value={form.imageUrl ?? ''} onChange={e => setForm(f => ({ ...f, imageUrl: e.target.value }))} fullWidth size="small" />
          <TextField select label="分类" value={form.category ?? ''} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} size="small" fullWidth>
            <MenuItem value="">请选择</MenuItem>
            {CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </TextField>
          <Stack direction="row" spacing={1}>
            <TextField label="售价" type="number" value={form.price ?? 0} onChange={e => setForm(f => ({ ...f, price: Number(e.target.value) }))} size="small" fullWidth />
            <TextField label="成本价" type="number" value={form.costPrice ?? 0} onChange={e => setForm(f => ({ ...f, costPrice: Number(e.target.value) }))} size="small" fullWidth />
          </Stack>
          <Stack direction="row" spacing={1}>
            <TextField label="利润率%" type="number" value={pctInput} onChange={e => setPctInput(e.target.value)} size="small" fullWidth />
            <TextField label="库存" type="number" value={form.inventory ?? 0} onChange={e => setForm(f => ({ ...f, inventory: Number(e.target.value) }))} size="small" fullWidth />
            <TextField label="单位" value={form.unit ?? '件'} onChange={e => setForm(f => ({ ...f, unit: e.target.value }))} size="small" sx={{ width: 80 }} />
          </Stack>
          <TextField label="商品描述" value={form.description ?? ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} fullWidth size="small" multiline rows={2} />
          <TextField label="卖点" value={form.sellingPoints ?? ''} onChange={e => setForm(f => ({ ...f, sellingPoints: e.target.value }))} fullWidth size="small" multiline minRows={4} />
          <TextField select label="状态" value={form.status ?? 1} onChange={e => setForm(f => ({ ...f, status: Number(e.target.value) }))} size="small" fullWidth>
            {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </TextField>
        </Stack>
      </FormDialog>

      {/* Dialogs */}
      <ConfirmDialog open={deleteTarget !== null}
        content={`确定要删除该商品吗？endpoint=${PRODUCT_ENDPOINTS.delete}; ${productContext(deleteTarget)}。删除失败会保留商品行和当前筛选。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        loading={delMut.isPending} />
      <ConfirmDialog open={batchDeleteOpen}
        content={`确定要删除选中的 ${selection.length} 件商品吗？endpoint=${PRODUCT_ENDPOINTS.batchDelete}; ${selectedProductContext()}。删除失败会保留商品行和当前选择。`}
        onClose={() => setBatchDeleteOpen(false)}
        onConfirm={() => batchDelMut.mutate()}
        loading={batchDelMut.isPending} />
      <BatchExtractDialog
        open={batchExtractOpen}
        ids={selection as number[]}
        products={allProducts}
        onClose={() => setBatchExtractOpen(false)}
      />
      <LinkExtractDialog
        open={linkExtractOpen}
        onClose={() => setLinkExtractOpen(false)}
        onApply={applyExtractResult}
      />

      {/* Detail Drawer */}
      <ProductDetailDrawer
        product={detailProduct}
        onClose={() => setDetailProduct(null)}
        onEdit={(p) => { setDetailProduct(null); openEdit(p) }}
      />
    </Box>
  )
}






