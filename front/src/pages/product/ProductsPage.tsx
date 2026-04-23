import { useState, useCallback, useEffect } from 'react'
import {
  Box, TextField, Button, Chip, Stack, MenuItem, LinearProgress, Tooltip,
  Drawer, Tab, Tabs, Typography, IconButton, Divider, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions,
  CircularProgress, Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CloseIcon from '@mui/icons-material/Close'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import SyncIcon from '@mui/icons-material/Sync'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import type { GridColDef, GridRowSelectionModel } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { productApi, type DyProduct, type ProductSave, exportProductToShortVideo } from '@/api/product'
import type { GmvTrendItem, GmvContribItem } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import ReactECharts from 'echarts-for-react'
import { useNavigate } from 'react-router-dom'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
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

const defaultForm: Partial<ProductSave> = {
  productName: '', productCode: '', category: '', brand: '',
  price: 0, costPrice: 0, profitMarginPct: 0,
  inventory: 0, unit: '件', description: '', sellingPoints: '', status: 1,
}

// ─── Batch AI Extraction Dialog ─────────────────────────────────────────────
function BatchExtractDialog({ open, ids, products, onClose }: {
  open: boolean; ids: number[]; products: DyProduct[]; onClose: () => void
}) {
  const [items, setItems] = useState<BatchExtractItem[]>([])
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
      } catch {
        setItems(prev => prev.map((it, idx) => idx === i ? { ...it, status: 'error' } : it))
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

  return (
    <Dialog open={open} onClose={() => !running && onClose()} maxWidth="sm" fullWidth>
      <DialogTitle>批量AI提炼卖点</DialogTitle>
      <DialogContent dividers>
        {items.length === 0 ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}><CircularProgress /></Box>
        ) : (
          <>
            <LinearProgress variant="determinate" value={progress} sx={{ mb: 2, height: 8, borderRadius: 4 }} />
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              {done ? `完成：${doneCount} 成功 / ${errCount} 失败` : `正在提炼... ${doneCount + errCount} / ${items.length}`}
            </Typography>
            <Stack spacing={0.75} sx={{ maxHeight: 320, overflowY: 'auto' }}>
              {items.map(it => (
                <Stack key={it.id} direction="row" alignItems="center" spacing={1}
                  sx={{ px: 1.5, py: 0.75, bgcolor: 'grey.50', borderRadius: 1 }}>
                  {it.status === 'loading' && <CircularProgress size={14} />}
                  {it.status === 'done' && <CheckCircleIcon sx={{ fontSize: 16, color: 'success.main' }} />}
                  {it.status === 'error' && <ErrorOutlineIcon sx={{ fontSize: 16, color: 'error.main' }} />}
                  {it.status === 'pending' && <Box sx={{ width: 16, height: 16, bgcolor: 'grey.300', borderRadius: '50%' }} />}
                  <Typography variant="body2" sx={{ flex: 1 }}>{it.name}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {it.status === 'pending' ? '等待中' : it.status === 'loading' ? '提炼中' : it.status === 'done' ? '完成' : '失败'}
                  </Typography>
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
function ExportSvDialog({ open, productId, onClose }: {
  open: boolean; productId: number | null; onClose: () => void
}) {
  const toast = useToast()
  const navigate = useNavigate()
  const [style, setStyle] = useState('种草')
  const [duration, setDuration] = useState(60)
  const mut = useMutation({
    mutationFn: () => exportProductToShortVideo({ productId: productId!, style, duration }),
    onSuccess: (res) => {
      toast('导出成功，正在跳转短视频项目', 'success')
      onClose()
      navigate(`${shortvideoRoutes.projects}?projectId=${res.projectId}`)
    },
    onError: () => toast('导出失败', 'error'),
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>导出为短视频脚本</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
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
  const toast = useToast()
  const navigate = useNavigate()
  const [tab, setTab] = useState(0)
  const [exportSvOpen, setExportSvOpen] = useState(false)

  const { data: scripts } = useQuery({
    queryKey: ['product-scripts', product?.id],
    queryFn: () => productApi.scriptList({ productId: product!.id, rows: 50 }),
    enabled: !!product && tab === 2,
  })

  const { data: effectiveness } = useQuery({
    queryKey: ['product-effectiveness', product?.id],
    queryFn: () => productApi.effectivenessRanking({ productId: product!.id, rows: 10 }),
    enabled: !!product && tab === 3,
  })

  const { data: gmvContrib } = useQuery({
    queryKey: ['product-gmv-contrib', product?.id],
    queryFn: () => productApi.scriptUsageList(product!.id),
    enabled: !!product && tab === 4,
  })

  const { data: gmvTrend } = useQuery({
    queryKey: ['product-gmv-trend', product?.id],
    queryFn: () => productApi.effectivenessTrend(product!.id, { days: 90 }),
    enabled: !!product && tab === 4,
  })

  const aiExtractMut = useMutation({
    mutationFn: () => productApi.scriptGenerate(product!.id),
    onSuccess: () => toast('AI卖点提炼完成', 'success'),
    onError: () => toast('提炼失败', 'error'),
  })

  const effectList = effectiveness?.list ?? []
  const chartOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: effectList.map((_, i) => `第${i + 1}场`) },
    yAxis: { type: 'value', min: 0, max: 10 },
    series: [{ name: '效果评分', type: 'line', smooth: true,
      data: effectList.map(r => Number(r.avgScore ?? 0)),
      lineStyle: { color: '#1976d2' }, itemStyle: { color: '#1976d2' },
      areaStyle: { color: 'rgba(25,118,210,0.08)' } }],
  }

  if (!product) return null
  const sellingPointLines = (product.sellingPoints ?? '').split(/[\n,，；;]+/).map(s => s.trim()).filter(Boolean)

  return (
    <Drawer anchor="right" open={!!product} onClose={onClose}
      PaperProps={{ sx: { width: 600 } }}>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {/* Header */}
        <Box sx={{ px: 2.5, py: 1.5, display: 'flex', alignItems: 'center', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={700} flex={1} noWrap>{product.productName}</Typography>
          <IconButton size="small" onClick={onClose}><CloseIcon /></IconButton>
        </Box>

        {/* Tabs */}
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto"
          sx={{ borderBottom: '1px solid', borderColor: 'divider', minHeight: 40,
            '& .MuiTab-root': { minHeight: 40, fontSize: 13 } }}>
          <Tab label="基本信息" />
          <Tab label="AI卖点" />
          <Tab label="话术版本" />
          <Tab label="效果历史" />
          <Tab label="GMV贡献" />
        </Tabs>

        {/* Tab Content */}
        <Box sx={{ flex: 1, overflow: 'auto', p: 2.5 }}>
          {tab === 0 && (
            <Grid container spacing={2}>
              {product.mainImage && (
                <Grid item xs={12} sx={{ textAlign: 'center' }}>
                  <img src={getProductPreviewUrl(product.mainImage)} alt="" style={{ maxWidth: '100%', maxHeight: 200, objectFit: 'contain', borderRadius: 8 }} />
                </Grid>
              )}
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
                <Grid item xs={6} key={k}>
                  <Typography variant="caption" color="text.secondary">{k}</Typography>
                  <Typography variant="body2" fontWeight={500}>{v || '--'}</Typography>
                </Grid>
              ))}
              {product.description && (
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">商品描述</Typography>
                  <Typography variant="body2">{product.description}</Typography>
                </Grid>
              )}
            </Grid>
          )}

          {tab === 1 && (
            <Box>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="subtitle2">AI提炼卖点</Typography>
                <Button size="small" variant="outlined" startIcon={aiExtractMut.isPending ? <CircularProgress size={14} /> : <AutoAwesomeIcon />}
                  onClick={() => aiExtractMut.mutate()} disabled={aiExtractMut.isPending}>
                  重新AI提炼
                </Button>
              </Stack>
              {sellingPointLines.length > 0 ? (
                <Stack spacing={1}>
                  {sellingPointLines.map((pt, i) => (
                    <Paper key={i} variant="outlined" sx={{ px: 2, py: 1.25, borderRadius: 2,
                      borderColor: 'primary.light', bgcolor: 'primary.50' }}>
                      <Stack direction="row" spacing={1} alignItems="flex-start">
                        <Box sx={{ minWidth: 20, height: 20, borderRadius: '50%', bgcolor: 'primary.main',
                          color: '#fff', fontSize: 11, fontWeight: 700,
                          display: 'flex', alignItems: 'center', justifyContent: 'center', mt: 0.1 }}>
                          {i + 1}
                        </Box>
                        <Typography variant="body2">{pt}</Typography>
                      </Stack>
                    </Paper>
                  ))}
                </Stack>
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
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
                <Typography variant="subtitle2">话术版本列表</Typography>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Button size="small" variant="contained" startIcon={<AutoAwesomeIcon />}
                    onClick={() => navigate(`/admin/product/${product.id}/scripts`)}>
                    生成话术
                  </Button>
                  <Chip label={`共 ${scripts?.total ?? 0} 条`} size="small" variant="outlined" />
                </Stack>
              </Stack>
              <Stack spacing={1}>
                {(scripts?.list ?? []).map(sc => (
                  <Paper key={sc.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
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
                  <Box sx={{ textAlign: 'center', py: 3, color: 'text.secondary' }}>
                    <Typography variant="body2">暂无话术版本</Typography>
                    <Button size="small" variant="outlined" startIcon={<AutoAwesomeIcon />} sx={{ mt: 1.5 }}
                      onClick={() => navigate(`/admin/product/${product.id}/scripts`)}>
                      立即生成
                    </Button>
                  </Box>
                )}
              </Stack>
            </Box>
          )}

          {tab === 3 && (
            <Box>
              <Typography variant="subtitle2" mb={1.5}>效果历史趋势</Typography>
              {effectList.length > 0 ? (
                <ReactECharts option={chartOption} style={{ height: 220 }} />
              ) : (
                <Box sx={{ textAlign: 'center', py: 4, color: 'text.secondary' }}>
                  <Typography variant="body2">暂无效果历史数据</Typography>
                </Box>
              )}
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" mb={1}>TOP 场次表现</Typography>
              <Stack spacing={0.75}>
                {effectList.slice(0, 5).map((r, i) => (
                  <Stack key={i} direction="row" alignItems="center" spacing={1}
                    sx={{ px: 1.5, py: 1, bgcolor: 'grey.50', borderRadius: 1 }}>
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
              {/* 总GMV汇总卡片 */}
              {(gmvTrend ?? []).length > 0 && (() => {
                const totalGmv = (gmvTrend ?? []).reduce((s: number, r: GmvTrendItem) => s + Number(r.gmv ?? r.salesAmount ?? 0), 0)
                return (
                  <Paper variant="outlined" sx={{ p: 1.5, mb: 2, borderRadius: 2, bgcolor: 'success.50', borderColor: 'success.light' }}>
                    <Typography variant="caption" color="text.secondary">90天累计GMV</Typography>
                    <Typography variant="h5" fontWeight={700} color="success.main">
                      ¥{totalGmv >= 10000 ? (totalGmv / 10000).toFixed(1) + '万' : totalGmv.toFixed(0)}
                    </Typography>
                  </Paper>
                )
              })()}

              {/* 渠道分布 */}
              {(gmvContrib ?? []).length > 0 && (() => {
                const totalGmv = (gmvContrib ?? []).reduce((s: number, r: GmvContribItem) => s + Number(r.totalGmv ?? r.gmv ?? 0), 0)
                // TODO: 从后端获取真实渠道分布数据，当前为硬编码占比
                const channels = [
                  { label: '直播场次带货', pct: 72, color: '#1976d2' },
                  { label: '短视频挂链', pct: 21, color: '#9c27b0' },
                  { label: '搜索商品卡', pct: 7, color: '#2e7d32' },
                ]
                return (
                  <Box mb={2}>
                    <Typography variant="subtitle2" mb={1}>渠道分布</Typography>
                    <Stack spacing={0.75}>
                      {channels.map(ch => (
                        <Stack key={ch.label} direction="row" alignItems="center" spacing={1}>
                          <Typography variant="caption" sx={{ minWidth: 90, color: 'text.secondary' }}>{ch.label}</Typography>
                          <Box flex={1} sx={{ bgcolor: 'grey.100', borderRadius: 1, height: 8, overflow: 'hidden' }}>
                            <Box sx={{ width: `${ch.pct}%`, height: '100%', bgcolor: ch.color, borderRadius: 1 }} />
                          </Box>
                          <Typography variant="caption" fontWeight={600} sx={{ minWidth: 32, color: ch.color }}>{ch.pct}%</Typography>
                          <Typography variant="caption" color="text.secondary">
                            ¥{(totalGmv * ch.pct / 100 / 10000).toFixed(1)}万
                          </Typography>
                        </Stack>
                      ))}
                    </Stack>
                  </Box>
                )
              })()}

              <Typography variant="subtitle2" mb={1}>TOP5 贡献场次</Typography>
              {(gmvContrib ?? []).length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 4, color: 'text.secondary' }}>
                  <Typography variant="body2">暂无GMV贡献数据</Typography>
                </Box>
              ) : (
                <Stack spacing={0.75}>
                  {(gmvContrib ?? []).slice(0, 5).map((r: GmvContribItem, i: number) => {
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
                              onClick={() => { onClose(); navigate(`/admin/live/sessions/${sessionId}`) }}>
                              查看
                            </Button>
                          )}
                        </Stack>
                      </Paper>
                    )
                  })}
                </Stack>
              )}

              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" mb={1}>话术版本GMV对比</Typography>
              <Stack spacing={0.5}>
                {(gmvContrib ?? []).slice(0, 3).map((r: GmvContribItem, i: number) => (
                  <Stack key={i} direction="row" alignItems="center" spacing={1}
                    sx={{ px: 1, py: 0.75, bgcolor: 'grey.50', borderRadius: 1 }}>
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

      <ExportSvDialog open={exportSvOpen} productId={product.id} onClose={() => setExportSvOpen(false)} />
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
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [selection, setSelection] = useState<GridRowSelectionModel>([])
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false)
  const [batchExtractOpen, setBatchExtractOpen] = useState(false)
  const [detailProduct, setDetailProduct] = useState<DyProduct | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['products', search],
    queryFn: () => productApi.list(search),
  })
  const allProducts = data?.list ?? []

  const saveMut = useMutation({
    mutationFn: productApi.save,
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['products'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: productApi.delete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['products'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const syncMut = useMutation({
    mutationFn: () => productApi.save({ productName: '', price: 0, status: 1 }),
    onSuccess: () => toast('同步已触发，数据将在后台更新', 'success'),
    onError: () => toast('同步触发失败', 'error'),
  })

  const openAdd = useCallback(() => { setForm(defaultForm); setPctInput('0'); setFormOpen(true) }, [])
  const openEdit = useCallback((row: DyProduct) => {
    setForm({
      id: row.id, productName: row.productName, productCode: row.productCode,
      category: row.category, brand: row.brand, price: row.price,
      costPrice: row.costPrice, profitMarginPct: row.profitMarginPct,
      inventory: row.inventory, unit: row.unit,
      description: row.description, sellingPoints: row.sellingPoints, status: row.status,
    })
    setPctInput(String(toDisplayPct(row.profitMarginPct)))
    setFormOpen(true)
  }, [])
  const handleSearch = useCallback(() => setSearch({ ...query, page: 0 }), [query])
  const handleSave = useCallback(() => {
    saveMut.mutate({ ...form, profitMarginPct: toStorePct(Number(pctInput)) })
  }, [form, pctInput, saveMut])
  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 60 },
    {
      field: 'imageUrl', headerName: '图片', width: 70, sortable: false,
      renderCell: ({ row }) => getProductThumbUrl(row.mainImage || row.imageUrl)
        ? <img src={getProductThumbUrl(row.mainImage || row.imageUrl)} style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 4, marginTop: 4 }} />
        : <Box sx={{ width: 48, height: 48, bgcolor: 'grey.100', borderRadius: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>📦</Box>,
    },
    {
      field: 'productName', headerName: '商品名称', flex: 1.5, minWidth: 140,
      renderCell: ({ row }) => (
        <Box sx={{ cursor: 'pointer', color: 'primary.main', fontWeight: 500, fontSize: 13 }}
          onClick={() => setDetailProduct(row as DyProduct)}>
          {row.productName}
        </Box>
      ),
    },
    { field: 'category', headerName: '分类', width: 80 },
    { field: 'brand', headerName: '品牌', width: 90 },
    {
      field: 'price', headerName: '售价', width: 85,
      valueFormatter: (value: number) => value != null ? `¥${Number(value).toFixed(2)}` : '--',
    },
    {
      field: 'profitMarginPct', headerName: '利润率', width: 80,
      renderCell: ({ value }) => {
        const pct = toDisplayPct(value as number)
        const color = pct >= 40 ? '#4caf50' : pct >= 25 ? '#ff9800' : '#f44336'
        return <Chip label={`${pct}%`} size="small" sx={{ bgcolor: color + '20', color, fontWeight: 600, fontSize: 12 }} />
      },
    },
    {
      field: 'effectivenessScore', headerName: '效果评分', width: 110,
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
      field: 'sellingPoints', headerName: 'AI卖点', flex: 2, minWidth: 160, sortable: false,
      renderCell: ({ value }) => (
        <Tooltip title={String(value ?? '')} placement="top">
          <Box sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 12, color: 'text.secondary' }}>
            {String(value ?? '--')}
          </Box>
        </Tooltip>
      ),
    },
    { field: 'inventory', headerName: '库存', width: 70, type: 'number' },
    {
      field: 'status', headerName: '状态', width: 76,
      renderCell: ({ value }) => {
        const opt = STATUS_OPTIONS.find(o => o.value === value)
        return <Chip label={opt?.label ?? String(value)} color={opt?.color ?? 'default'} size="small" />
      },
    },
    { field: 'createTime', headerName: '创建时间', width: 150, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => setDetailProduct(row as DyProduct)}>详情</Button>
          <Button size="small" onClick={() => openEdit(row as DyProduct)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as DyProduct).id)}>删除</Button>
        </Stack>
      ),
    },
  ]
  const searchSlot = (
    <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
      <TextField size="small" label="商品名称" value={query.productName}
        onChange={e => setQuery(q => ({ ...q, productName: e.target.value }))}
        onKeyDown={e => e.key === 'Enter' && handleSearch()} sx={{ width: 160 }} />
      <TextField select size="small" label="品类" value={query.category ?? ''}
        onChange={e => setQuery(q => ({ ...q, category: e.target.value }))} sx={{ width: 120 }}>
        <MenuItem value="">全部</MenuItem>
        {CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
      </TextField>
      <TextField select size="small" label="状态" value={query.status ?? ''}
        onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))} sx={{ width: 100 }}>
        <MenuItem value="">全部</MenuItem>
        {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
      </TextField>
      <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
    </Stack>
  )
  const actionSlot = (
    <Stack direction="row" spacing={1} flexWrap="wrap">
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
      <Button size="small" variant="outlined" startIcon={<SyncIcon />}
        onClick={() => syncMut.mutate()} disabled={syncMut.isPending}>
        同步抖音
      </Button>
      <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openAdd}>新增商品</Button>
    </Stack>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={allProducts} columns={columns} rowCount={data?.total ?? 0} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        checkboxSelection
        rowSelectionModel={selection}
        onRowSelectionModelChange={setSelection}
        rowHeight={56}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />

      {/* Edit/Add Dialog */}
      <FormDialog open={formOpen} title={form.id ? '编辑商品' : '新增商品'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="商品名称" required value={form.productName ?? ''} onChange={e => setForm(f => ({ ...f, productName: e.target.value }))} fullWidth size="small" />
          <Stack direction="row" spacing={1}>
            <TextField label="商品编码" value={form.productCode ?? ''} onChange={e => setForm(f => ({ ...f, productCode: e.target.value }))} size="small" fullWidth />
            <TextField label="品牌" value={form.brand ?? ''} onChange={e => setForm(f => ({ ...f, brand: e.target.value }))} size="small" fullWidth />
          </Stack>
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
      <ConfirmDialog open={deleteId !== null} content="确定要删除该商品吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
      <ConfirmDialog open={batchDeleteOpen} content={`确定要删除选中的 ${selection.length} 件商品吗？此操作不可撤销。`}
        onClose={() => setBatchDeleteOpen(false)}
        onConfirm={async () => {
          await productApi.batchDelete(selection as number[])
          toast(`已删除 ${selection.length} 件商品`, 'success')
          setSelection([])
          setBatchDeleteOpen(false)
          qc.invalidateQueries({ queryKey: ['products'] })
        }}
        loading={false} />
      <BatchExtractDialog
        open={batchExtractOpen}
        ids={selection as number[]}
        products={allProducts}
        onClose={() => setBatchExtractOpen(false)}
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






