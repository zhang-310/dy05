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
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useCoreData } from '../contexts'
import { liveApi } from '@/api/live'
import type { LiveSessionData, LiveProductData } from '@/api/live'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'

interface SessionCompareData { prev?: LiveSessionData; curr?: LiveSessionData; [key: string]: unknown }
interface SessionAnalysisReport { summary?: string; highlights?: string[]; issues?: string[]; suggestions?: string[]; rating?: unknown; [key: string]: unknown }

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
    <Card variant="outlined">
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

  const { data: sessionData, isLoading: sdLoading } = useQuery({
    queryKey: ['wb-session-data', session?.id],
    queryFn: () => liveApi.dataSession(session!.id),
    enabled: !!session?.id,
  })

  const { data: compareData } = useQuery({
    queryKey: ['wb-session-compare', session?.id],
    queryFn: () => liveApi.dataSessionWithCompare({ sessionId: session!.id }),
    enabled: !!session?.id && showCompare,
  })

  const { data: productData = [], isLoading: pdLoading } = useQuery({
    queryKey: ['wb-product-data', session?.id],
    queryFn: () => liveApi.dataProduct(session!.id),
    enabled: !!session?.id,
  })

  const { data: analysis, isLoading: analysisLoading } = useQuery({
    queryKey: ['wb-analysis', session?.id],
    queryFn: () => liveApi.analysisGet({ sessionId: session!.id }),
    enabled: !!session?.id,
  })

  const syncMut = useMutation({
    mutationFn: () => liveApi.dataSessionSync(session!.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wb-session-data', session?.id] })
      qc.invalidateQueries({ queryKey: ['wb-product-data', session?.id] })
      toast('数据同步成功', 'success')
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const genAnalysisMut = useMutation({
    mutationFn: () => liveApi.analysisGenerate({ sessionId: session!.id }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wb-analysis', session?.id] })
      toast('AI 复盘分析已生成', 'success')
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const sd = sessionData as LiveSessionData | undefined
  const prev = compareData ? (compareData as SessionCompareData).prev : undefined
  const isEnded = session?.status === 2
  const isLive = session?.status === 1
  if (!isEnded && !isLive) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 2, color: 'text.secondary' }}>
        <TrendingUpIcon sx={{ fontSize: 48, opacity: 0.3 }} />
        <Typography variant="body2">直播开始后可查看实时数据，结束后生成完整复盘</Typography>
      </Box>
    )
  }

  if (sdLoading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}><CircularProgress /></Box>
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'auto', p: 2, gap: 2 }}>
      {/* Toolbar */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
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
            <IconButton size="small" onClick={() => syncMut.mutate()} disabled={syncMut.isPending}>
              {syncMut.isPending ? <CircularProgress size={16} /> : <SyncIcon fontSize="small" />}
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="导出话术">
          <span>
            <IconButton size="small" onClick={() => liveApi.scriptExport({ sessionId: session?.id }).then(() => toast('导出成功', 'success')).catch((e: Error) => toast(e.message, 'error'))}>
              <FileDownloadIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      </Box>

      {/* KPI 卡片 */}
      <Grid container spacing={1.5}>
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
        <Card variant="outlined">
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
                    <TableRow key={pd.id} hover sx={{ '& td': { fontSize: 11 } }}>
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

      {/* 汇总：话术 & 商品数量 */}
      <Paper variant="outlined" sx={{ p: 1.5 }}>
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
      <Card variant="outlined">
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
              sx={{ fontSize: 11 }}
            >
              {analysis ? '重新分析' : '生成分析'}
            </Button>
          </Box>
          {analysisLoading ? (
            <Box sx={{ textAlign: 'center', py: 2 }}><CircularProgress size={20} /></Box>
          ) : analysis ? (
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>
              {(analysis as SessionAnalysisReport).summary ?? '暂无分析结论'}
            </Typography>
          ) : (
            <Alert severity="info" sx={{ fontSize: 12 }}>点击「生成分析」让 AI 自动复盘本场直播</Alert>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}




