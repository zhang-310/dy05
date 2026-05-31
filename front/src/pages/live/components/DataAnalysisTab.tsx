import { useState } from 'react'
import {
  Box, Typography, Card, CardContent, Grid, CircularProgress, Alert, Chip,
  Button, Stack, Divider, Table, TableBody, TableCell, TableHead, TableRow,
  Tooltip, IconButton, Paper,
} from '@mui/material'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import PeopleIcon from '@mui/icons-material/People'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import MonetizationOnIcon from '@mui/icons-material/MonetizationOn'
import SyncIcon from '@mui/icons-material/Sync'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useCoreData } from '../contexts'
import { liveApi } from '@/api/live'
import type { LiveSessionData, LiveProductData } from '@/api/live'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'

interface SessionCompareData { prev?: LiveSessionData; curr?: LiveSessionData; [key: string]: unknown }
interface SessionAnalysisReport { summary?: string; highlights?: string[]; issues?: string[]; suggestions?: string[]; rating?: unknown; [key: string]: unknown }

const DATA_ANALYSIS_READY_ENDPOINTS = [
  '/live/data/session',
  '/live/data/session/with-compare',
  '/live/data/product',
  '/live/data/session/sync',
  '/live/analysis/get',
  '/live/analysis/generate',
]

const DATA_ANALYSIS_CONTEXT_ENDPOINTS = [
  '/live/session/get',
  '/live/product/by-session',
  '/live/script/by-session',
]

const DATA_ANALYSIS_UNSUPPORTED_ACTIONS = [
  'local-session-data-fallback',
  'local-product-data-fallback',
  'local-analysis-fallback',
  'data-session-save',
  'data-product-save',
  'script-export',
  'analysis-review',
  'shortvideo-export',
]

function fmt(v: number | undefined, type: 'money' | 'count' | 'pct' = 'count'): string {
  if (v === undefined || v === null) return '-'
  if (type === 'money') return `¥${v.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
  if (type === 'pct') return `${(v * 100).toFixed(1)}%`
  return v.toLocaleString()
}

function TrendChip({ curr, prev }: { curr?: number; prev?: number }) {
  if (!prev || !curr) return null
  const pct = ((curr - prev) / prev) * 100
  const up = pct >= 0
  return (
    <Chip
      size="small"
      icon={up ? <TrendingUpIcon sx={{ fontSize: '13px !important' }} /> : <TrendingDownIcon sx={{ fontSize: '13px !important' }} />}
      label={`${up ? '+' : ''}${pct.toFixed(1)}%`}
      color={up ? 'success' : 'error'}
      variant="outlined"
      sx={{ fontSize: 10, height: 18 }}
    />
  )
}

function KpiCard({ title, value, icon, color = 'primary', prev, fmtType }: {
  title: string
  value: number | undefined
  icon: React.ReactNode
  color?: string
  prev?: number
  fmtType?: 'money' | 'count' | 'pct'
}) {
  return (
    <Card
      variant="outlined"
      data-testid="live-data-analysis-kpi-card"
      data-contract-source="/live/data/session"
      data-kpi-title={title}
      data-has-compare={prev !== undefined ? 'true' : 'false'}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
          <Box sx={{ color: `${color}.main` }}>{icon}</Box>
          <Typography variant="caption" color="text.secondary">{title}</Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1 }}>
          <Typography variant="h5" fontWeight={700}>{fmt(value, fmtType)}</Typography>
          {prev !== undefined && <TrendChip curr={value} prev={prev} />}
        </Box>
        {prev !== undefined && (
          <Typography variant="caption" color="text.secondary">上场：{fmt(prev, fmtType)}</Typography>
        )}
      </CardContent>
    </Card>
  )
}
export function DataAnalysisTab() {
  const { session, scripts, products } = useCoreData()
  const toast = useToast()
  const qc = useQueryClient()
  const [showCompare, setShowCompare] = useState(false)
  const isEnded = session?.status === 2
  const isLive = session?.status === 1
  const canLoadData = !!session?.id && (isEnded || isLive)

  const { data: sessionData, isLoading: sdLoading, isError: sdIsError, error: sdError } = useQuery({
    queryKey: ['wb-session-data', session?.id],
    queryFn: () => liveApi.dataSession(session!.id),
    enabled: canLoadData,
  })

  const { data: compareData, isError: compareIsError, error: compareError } = useQuery({
    queryKey: ['wb-session-compare', session?.id],
    queryFn: () => liveApi.dataSessionWithCompare({ sessionId: session!.id }),
    enabled: canLoadData && showCompare,
  })

  const { data: productData = [], isLoading: pdLoading, isError: pdIsError, error: pdError } = useQuery({
    queryKey: ['wb-product-data', session?.id],
    queryFn: () => liveApi.dataProduct(session!.id),
    enabled: canLoadData,
  })

  const { data: analysis, isLoading: analysisLoading, isError: analysisIsError, error: analysisError } = useQuery({
    queryKey: ['wb-analysis', session?.id],
    queryFn: () => liveApi.analysisGet({ sessionId: session!.id }),
    enabled: canLoadData,
  })

  const syncMut = useMutation({
    mutationFn: () => liveApi.dataSessionSync(session!.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wb-session-data', session?.id] })
      qc.invalidateQueries({ queryKey: ['wb-product-data', session?.id] })
      toast('数据同步成功', 'success')
    },
    onError: (e: Error) => toast(`/live/data/session/sync 数据同步失败：${e.message}`, 'error'),
  })

  const genAnalysisMut = useMutation({
    mutationFn: () => liveApi.analysisGenerate({ sessionId: session!.id }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wb-analysis', session?.id] })
      toast('AI 复盘分析已生成', 'success')
    },
    onError: (e: Error) => toast(`/live/analysis/generate AI 复盘分析失败：${e.message}`, 'error'),
  })

  const sd = sessionData as LiveSessionData | undefined
  const prev = compareData ? (compareData as SessionCompareData).prev : undefined

  const rootAttrs = {
    'data-testid': 'live-data-analysis-workbench',
    'data-contract-scope': 'live-session-data-review',
    'data-ready-endpoints': DATA_ANALYSIS_READY_ENDPOINTS.join('|'),
    'data-context-endpoints': DATA_ANALYSIS_CONTEXT_ENDPOINTS.join('|'),
    'data-unsupported-actions': DATA_ANALYSIS_UNSUPPORTED_ACTIONS.join('|'),
    'data-session-id': String(session?.id ?? ''),
    'data-session-status': String(session?.status ?? ''),
    'data-script-count': scripts.length,
    'data-product-count': products.length,
    'data-product-data-count': productData.length,
    'data-show-compare': showCompare ? 'true' : 'false',
    'data-no-local-session-data-fallback': 'true',
    'data-no-local-product-data-fallback': 'true',
    'data-no-local-analysis-fallback': 'true',
  }

  if (!isEnded && !isLive) {
    return (
      <Box
        {...rootAttrs}
        data-state="not-started"
        sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}
      >
        <Alert
          severity="info"
          data-testid="live-data-analysis-contract-alert"
          data-contract-ready-endpoints={DATA_ANALYSIS_READY_ENDPOINTS.join('|')}
          data-no-script-export="true"
          sx={{ borderRadius: 0 }}
        >
          数据复盘只读取直播数据与 AI 复盘；话术导出、数据手工保存和短视频导出不在本页执行。
        </Alert>
        <Box
          data-testid="live-data-analysis-unavailable-state"
          data-contract-source="/live/session/get"
          data-no-local-session-data-fallback="true"
          sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2, color: 'text.secondary' }}
        >
        <TrendingUpIcon sx={{ fontSize: 48, opacity: 0.3 }} />
        <Typography variant="body2">直播开始后可查看实时数据，结束后生成完整复盘</Typography>
        </Box>
      </Box>
    )
  }

  if (sdLoading) {
    return (
      <Box
        {...rootAttrs}
        data-state="loading"
        data-testid="live-data-analysis-loading"
        data-contract-source="/live/data/session"
        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}
      >
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box
      {...rootAttrs}
      data-state={sdIsError ? 'session-data-error' : 'ready'}
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'auto', p: 2, gap: 2 }}
    >
      <Alert
        severity="info"
        data-testid="live-data-analysis-contract-alert"
        data-contract-ready-endpoints={DATA_ANALYSIS_READY_ENDPOINTS.join('|')}
        data-no-script-export="true"
        data-no-local-session-data-fallback="true"
        data-no-local-product-data-fallback="true"
      >
        数据复盘只读取 `/live/data/session`、`/live/data/product` 和 `/live/analysis/get`；同步与生成分析失败不写入本地兜底数据。
      </Alert>

      {/* Toolbar */}
      <Box
        data-testid="live-data-analysis-toolbar"
        data-contract-source="/live/data/session|/live/data/session/with-compare|/live/data/session/sync|/live/analysis/generate"
        data-no-script-export="true"
        sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}
      >
        <Typography variant="subtitle2" fontWeight={700}>数据复盘</Typography>
        {session && <Chip label={session.liveTitle} size="small" variant="outlined" sx={{ maxWidth: 180, fontSize: 11 }} />}
        <Chip
          label={showCompare ? '隐藏环比' : '显示环比'}
          size="small"
          variant={showCompare ? 'filled' : 'outlined'}
          color={showCompare ? 'primary' : 'default'}
          onClick={() => setShowCompare(v => !v)}
          sx={{ cursor: 'pointer', fontSize: 11 }}
        />
        <Box sx={{ flex: 1 }} />
        <Tooltip title="从抖音同步最新数据">
          <span>
            <IconButton
              size="small"
              onClick={() => syncMut.mutate()}
              disabled={syncMut.isPending}
              data-testid="live-data-analysis-sync-button"
              data-contract-source="/live/data/session/sync"
            >
              {syncMut.isPending ? <CircularProgress size={16} /> : <SyncIcon fontSize="small" />}
            </IconButton>
          </span>
        </Tooltip>
      </Box>

      {sdIsError && (
        <Alert
          severity="error"
          data-testid="live-data-session-error"
          data-contract-source="/live/data/session"
          data-no-local-session-data-fallback="true"
        >
          /live/data/session 场次数据加载失败：{getErrorMessage(sdError)}
        </Alert>
      )}

      {compareIsError && (
        <Alert
          severity="error"
          data-testid="live-data-compare-error"
          data-contract-source="/live/data/session/with-compare"
          data-no-local-session-data-fallback="true"
        >
          /live/data/session/with-compare 环比数据加载失败：{getErrorMessage(compareError)}
        </Alert>
      )}

      {pdIsError && (
        <Alert
          severity="error"
          data-testid="live-product-data-error"
          data-contract-source="/live/data/product"
          data-no-local-product-data-fallback="true"
        >
          /live/data/product 商品销售明细加载失败：{getErrorMessage(pdError)}
        </Alert>
      )}

      {/* KPI 卡片 */}
      <Grid
        container
        spacing={1.5}
        data-testid="live-data-analysis-kpi-grid"
        data-contract-source="/live/data/session"
        data-no-local-session-data-fallback="true"
      >
        <Grid item xs={6} sm={3}>
          <KpiCard title="场次 GMV" value={sd?.gmv} icon={<MonetizationOnIcon />} color="success" prev={prev?.gmv} fmtType="money" />
        </Grid>
        <Grid item xs={6} sm={3}>
          <KpiCard title="峰值在线" value={sd?.viewerPeak} icon={<PeopleIcon />} color="primary" prev={prev?.viewerPeak} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <KpiCard title="订单数" value={sd?.orderCount} icon={<ShoppingCartIcon />} color="warning" prev={prev?.orderCount} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <KpiCard title="转化率" value={sd?.conversionRate} icon={<TrendingUpIcon />} color="info" prev={prev?.conversionRate} fmtType="pct" />
        </Grid>
      </Grid>

      {/* 商品销售明细表 */}
      {productData.length > 0 && (
        <Card
          variant="outlined"
          data-testid="live-product-data-table-card"
          data-contract-source="/live/data/product"
          data-no-local-product-data-fallback="true"
        >
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ px: 2, pt: 1.5, pb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="subtitle2">商品销售明细</Typography>
              <Chip label={`${productData.length} 件`} size="small" variant="outlined" sx={{ fontSize: 10 }} />
              {pdLoading && <CircularProgress size={14} />}
            </Box>
            <Divider />
            <Box sx={{ overflowX: 'auto' }}>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ '& th': { fontSize: 11, fontWeight: 600, whiteSpace: 'nowrap' } }}>
                    <TableCell>商品名称</TableCell>
                    <TableCell align="right">曝光</TableCell>
                    <TableCell align="right">点击</TableCell>
                    <TableCell align="right">点击率</TableCell>
                    <TableCell align="right">订单</TableCell>
                    <TableCell align="right">GMV</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(productData as LiveProductData[]).map((pd) => (
                    <TableRow
                      key={pd.id}
                      hover
                      data-testid="live-product-data-row"
                      data-contract-product-id={pd.productId}
                      sx={{ '& td': { fontSize: 11 } }}
                    >
                      <TableCell sx={{ maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {pd.productName}
                      </TableCell>
                      <TableCell align="right">{fmt(pd.exposures)}</TableCell>
                      <TableCell align="right">{fmt(pd.clicks)}</TableCell>
                      <TableCell align="right">
                        {pd.exposures > 0 ? `${((pd.clicks / pd.exposures) * 100).toFixed(1)}%` : '-'}
                      </TableCell>
                      <TableCell align="right">{fmt(pd.orders)}</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600, color: 'success.main' }}>
                        {fmt(pd.gmv, 'money')}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </CardContent>
        </Card>
      )}

      {!pdLoading && !pdIsError && productData.length === 0 && (
        <Alert
          severity="info"
          data-testid="live-product-data-empty-state"
          data-contract-source="/live/data/product"
          data-no-local-product-data-fallback="true"
        >
          本场暂无商品销售明细，页面不会注入静态销售数据。
        </Alert>
      )}

      {/* 汇总：话术 & 商品数量 */}
      <Paper
        variant="outlined"
        data-testid="live-data-context-summary"
        data-contract-source="/live/product/by-session|/live/script/by-session"
        sx={{ p: 1.5 }}
      >
        <Stack direction="row" spacing={3} divider={<Divider orientation="vertical" flexItem />}>
          <Box>
            <Typography variant="caption" color="text.secondary">话术数</Typography>
            <Typography variant="h6" fontWeight={700}>{scripts.length}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">商品数</Typography>
            <Typography variant="h6" fontWeight={700}>{products.length}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">已激活话术</Typography>
            <Typography variant="h6" fontWeight={700}>
              {scripts.filter(s => s.status === 1).length}
            </Typography>
          </Box>
        </Stack>
      </Paper>

      {/* AI 复盘分析 */}
      <Card
        variant="outlined"
        data-testid="live-analysis-card"
        data-contract-source="/live/analysis/get|/live/analysis/generate"
        data-no-local-analysis-fallback="true"
      >
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Typography variant="subtitle2">AI 复盘分析</Typography>
            <Box sx={{ flex: 1 }} />
            <Button
              size="small"
              startIcon={genAnalysisMut.isPending ? <CircularProgress size={12} /> : <AutoAwesomeIcon />}
              onClick={() => genAnalysisMut.mutate()}
              disabled={genAnalysisMut.isPending || analysisLoading}
              variant="outlined"
              data-testid="live-analysis-generate-button"
              data-contract-source="/live/analysis/generate"
              sx={{ fontSize: 11 }}
            >
              {analysis ? '重新分析' : '生成分析'}
            </Button>
          </Box>
          {analysisLoading ? (
            <Box data-testid="live-analysis-loading" data-contract-source="/live/analysis/get" sx={{ textAlign: 'center', py: 2 }}><CircularProgress size={20} /></Box>
          ) : analysisIsError ? (
            <Alert
              severity="error"
              data-testid="live-analysis-error"
              data-contract-source="/live/analysis/get"
              data-no-local-analysis-fallback="true"
            >
              /live/analysis/get AI 复盘分析加载失败：{getErrorMessage(analysisError)}
            </Alert>
          ) : analysis ? (
            <Typography
              variant="body2"
              data-testid="live-analysis-summary"
              data-contract-source="/live/analysis/get"
              sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.8 }}
            >
              {(analysis as SessionAnalysisReport).summary ?? '暂无分析结论'}
            </Typography>
          ) : (
            <Alert
              severity="info"
              data-testid="live-analysis-empty-state"
              data-contract-source="/live/analysis/get"
              data-no-local-analysis-fallback="true"
              sx={{ fontSize: 12 }}
            >
              点击「生成分析」让 AI 自动复盘本场直播
            </Alert>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}




