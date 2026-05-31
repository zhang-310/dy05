import { useState, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Stack, Chip, Button, LinearProgress,
  Grid, Alert, Dialog, DialogTitle, DialogContent, DialogActions,
  FormControl, FormControlLabel, Radio, RadioGroup, Tab, Tabs,
  Select, MenuItem,
} from '@mui/material'
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningIcon from '@mui/icons-material/Warning'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { LazyECharts } from '@/utils/echarts-registry'
import { abtestApi, type AbDailyTrend, type AbVariant } from '@/api/abtest'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base/PageHeader'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray } from '@/utils/response-normalize'

// ── Statistical helpers ───────────────────────────────────────────────────────
function normalCDF(z: number): number {
  const t = 1 / (1 + 0.2316419 * Math.abs(z))
  const d = 0.3989423 * Math.exp(-z * z / 2)
  const p = t * (0.3193815 + t * (-0.3565638 + t * (1.7814779 + t * (-1.8212560 + t * 1.3302744))))
  return z > 0 ? 1 - d * p : d * p
}

function calcPValue(v1: AbVariant, v2: AbVariant): number {
  if (!v1 || !v2 || v1.exposures === 0 || v2.exposures === 0) return NaN
  const pPool = (v1.conversions + v2.conversions) / (v1.exposures + v2.exposures)
  const se = Math.sqrt(pPool * (1 - pPool) * (1 / v1.exposures + 1 / v2.exposures))
  if (se === 0) return NaN
  const z = Math.abs(v1.conversionRate - v2.conversionRate) / se
  return 2 * (1 - normalCDF(z))
}

function calcCI(v: AbVariant): [number, number] {
  if (!v || v.exposures === 0) return [0, 0]
  const se = Math.sqrt(v.conversionRate * (1 - v.conversionRate) / v.exposures)
  return [v.conversionRate - 1.96 * se, v.conversionRate + 1.96 * se]
}

// ── Constants ─────────────────────────────────────────────────────────────────
const STATUS_MAP: Record<number, { label: string; color: 'default' | 'info' | 'warning' | 'success' | 'error' }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '进行中', color: 'info' },
  2: { label: '已完成', color: 'success' },
  3: { label: '已暂停', color: 'warning' },
}

const SEG_DIMENSIONS = [
  { value: 'device', label: '设备类型' },
  { value: 'time', label: '时段' },
  { value: 'user_type', label: '用户类型' },
]
const ABTEST_DETAIL_ENDPOINTS = {
  get: '/abtest/experiment/get',
  result: '/abtest/experiment/result',
  trend: '/abtest/experiment/daily-trend',
  status: '/abtest/experiment/update-status',
  winner: '/abtest/experiment/set-winner',
  segment: '/abtest/experiment/segment-analysis',
} as const
const ABTEST_DETAIL_READY_ENDPOINTS = [
  ABTEST_DETAIL_ENDPOINTS.get,
  ABTEST_DETAIL_ENDPOINTS.result,
  ABTEST_DETAIL_ENDPOINTS.trend,
  ABTEST_DETAIL_ENDPOINTS.status,
  ABTEST_DETAIL_ENDPOINTS.winner,
] as const
const ABTEST_DETAIL_UNSUPPORTED_ENDPOINTS = [
  '/abtest/experiment/mock-detail',
  '/abtest/experiment/local-result',
  '/abtest/experiment/local-trend',
  '/abtest/experiment/local-update-status',
  '/abtest/experiment/local-winner',
  '/abtest/experiment/static-result',
  ABTEST_DETAIL_ENDPOINTS.segment,
  '/abtest/live-session/local-promote-winner',
] as const
const SEGMENT_ANALYSIS_DOWNGRADE_MESSAGE =
  '后端当前没有 `/abtest/experiment/segment-analysis` 接口；分群维度选择先保留为产品规划入口，不再展示模拟分群结论。'

export default function ExperimentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const expId = Number(id)
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [promoteOpen, setPromoteOpen] = useState(false)
  const [promoteTarget, setPromoteTarget] = useState<'all' | 'selected'>('all')
  const [segDim, setSegDim] = useState('device')
  const [actionError, setActionError] = useState('')

  const { data: experiment, isLoading, isError: experimentError, error: experimentLoadError, refetch: refetchExperiment } = useQuery({
    queryKey: ['abtest-detail', expId],
    queryFn: () => abtestApi.get(expId),
    enabled: expId > 0,
  })
  const { data: result, isError: resultError, error: resultLoadError, refetch: refetchResult } = useQuery({
    queryKey: ['abtest-result', expId],
    queryFn: () => abtestApi.result(expId),
    enabled: expId > 0,
  })
  const { data: trendData, isError: trendError, error: trendLoadError, refetch: refetchTrend } = useQuery({
    queryKey: ['abtest-trend', expId],
    queryFn: () => abtestApi.dailyTrend(expId, 14),
    enabled: expId > 0,
  })
  const trend = normalizeArray<AbDailyTrend>(trendData)
  const detailContext = () => {
    const name = experiment?.name ?? experiment?.experimentName ?? '未知实验'
    const status = experiment?.status == null ? '-' : `${experiment.status}/${STATUS_MAP[Number(experiment.status)]?.label ?? '未知'}`
    return `route=/admin/ai/abtest/${expId}; experimentId=${expId}; experimentName=${name}; experimentType=${experiment?.experimentType ?? '-'}; target=${experiment?.targetEntityType ?? '-'}#${experiment?.targetEntityId ?? '-'}; status=${status}; tab=${tab}; segmentDimension=${segDim}`
  }
  const variantContext = (variant?: AbVariant | null) => (
    `variantId=${variant?.id ?? '-'}; variantName=${variant?.variantName ?? '-'}; conversionRate=${variant?.conversionRate ?? '-'}; ${detailContext()}`
  )

  const setWinnerMutation = useMutation({
    mutationFn: async (variantId: number): Promise<void> => { setActionError(''); await abtestApi.setWinner(expId, variantId, '前端根据统计结果设置获胜变体') },
    onSuccess: () => { toast('已设置获胜变体', 'success'); qc.invalidateQueries({ queryKey: ['abtest-detail', expId] }); qc.invalidateQueries({ queryKey: ['abtest-result', expId] }) },
    onError: (error: Error, variantId) => {
      const variant = variants.find(item => Number(item.id) === Number(variantId))
      setActionError(`设置获胜变体失败（POST ${ABTEST_DETAIL_ENDPOINTS.winner}）：${getErrorMessage(error)}（${variantContext(variant)}）`)
      toast(`操作失败：${getErrorMessage(error)}`, 'error')
    },
  })

  const statusUpdateMutation = useMutation({
    mutationFn: async (status: number): Promise<void> => { setActionError(''); await abtestApi.updateStatus(expId, status) },
    onSuccess: () => { toast('状态已更新', 'success'); qc.invalidateQueries({ queryKey: ['abtest-detail', expId] }) },
    onError: (error: Error, status) => { setActionError(`状态更新失败（POST ${ABTEST_DETAIL_ENDPOINTS.status}）：${getErrorMessage(error)}（targetStatus=${status}/${STATUS_MAP[status]?.label ?? '未知'}; ${detailContext()}）`); toast(`操作失败：${getErrorMessage(error)}`, 'error') },
  })

  // ── Derived statistics ───────────────────────────────────────────────────
  const resultVariants = normalizeArray<AbVariant>(result?.variants ?? result?.variantStats)
  const experimentVariants = normalizeArray<AbVariant>(experiment?.variants)
  const variants: AbVariant[] = resultVariants.length > 0 ? resultVariants : experimentVariants
  const bestVariant = useMemo(() => {
    if (variants.length < 2) return null
    return variants.reduce((a, b) => (a.conversionRate ?? 0) >= (b.conversionRate ?? 0) ? a : b)
  }, [variants])

  const pValue = useMemo(() => {
    if (variants.length < 2) return NaN
    return calcPValue(variants[0], variants[1])
  }, [variants])

  const isSignificant = !isNaN(pValue) && pValue < 0.05
  const totalExposures = result?.totalExposures ?? variants.reduce((sum, v) => sum + (v.exposures ?? 0), 0)
  const maxRate = Math.max(...variants.map(v => v.conversionRate ?? 0), 0.001)

  // ── ECharts trend option ─────────────────────────────────────────────────
  const trendRows = trend.flatMap((d) => {
    if (d.variantName) return [d]
    return [
      {
        date: d.date,
        variantName: 'A',
        conversionRate: (d.variantAConversionRate ?? 0) > 1 ? (d.variantAConversionRate ?? 0) / 100 : (d.variantAConversionRate ?? 0),
      },
      {
        date: d.date,
        variantName: 'B',
        conversionRate: (d.variantBConversionRate ?? 0) > 1 ? (d.variantBConversionRate ?? 0) / 100 : (d.variantBConversionRate ?? 0),
      },
    ]
  })
  const variantNames = [...new Set(trendRows.map(d => d.variantName).filter(Boolean))]
  const dates = [...new Set(trend.map(d => d.date))].sort()
  const trendOption = {
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${(v * 100).toFixed(2)}%` },
    legend: { data: variantNames },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => `${(v * 100).toFixed(1)}%` } },
    series: variantNames.map(name => ({
      name,
      type: 'line',
      smooth: true,
      data: dates.map(d => trendRows.find(t => t.date === d && t.variantName === name)?.conversionRate ?? null),
    })),
  }

  if (isLoading) {
    return (
      <Box
        data-testid="abtest-experiment-detail-page"
        data-ready-endpoints={ABTEST_DETAIL_READY_ENDPOINTS.join('|')}
        data-unsupported-endpoints={ABTEST_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')}
        data-no-local-abtest-result="true"
        sx={{ p: 3 }}
      >
        <Typography>加载中...</Typography>
      </Box>
    )
  }
  if (experimentError) {
    return (
      <Box
        data-testid="abtest-experiment-detail-page"
        data-ready-endpoints={ABTEST_DETAIL_READY_ENDPOINTS.join('|')}
        data-unsupported-endpoints={ABTEST_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')}
        data-no-local-abtest-result="true"
        sx={{ p: 3 }}
      >
        <Alert
          data-testid="abtest-detail-load-error"
          data-no-local-abtest-result="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetchExperiment()}>重试</Button>}
        >
          实验详情加载失败（POST {ABTEST_DETAIL_ENDPOINTS.get}）：{getErrorMessage(experimentLoadError)}（route=/admin/ai/abtest/{expId}; experimentId={expId}）。请确认实验是否存在或当前账号是否有权限。
        </Alert>
      </Box>
    )
  }
  if (!experiment) return <Box data-testid="abtest-experiment-detail-page" data-ready-endpoints={ABTEST_DETAIL_READY_ENDPOINTS.join('|')} data-unsupported-endpoints={ABTEST_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')} sx={{ p: 3 }}><Alert data-testid="abtest-detail-empty" data-no-local-abtest-result="true" severity="error">实验不存在或无权访问（POST {ABTEST_DETAIL_ENDPOINTS.get} 返回空数据，route=/admin/ai/abtest/{expId}; experimentId={expId}）</Alert></Box>

  const statusCfg = STATUS_MAP[experiment.status] ?? STATUS_MAP[0]

  return (
    <Box
      data-testid="abtest-experiment-detail-page"
      data-ready-endpoints={ABTEST_DETAIL_READY_ENDPOINTS.join('|')}
      data-unsupported-endpoints={ABTEST_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-abtest-result="true"
      data-no-local-abtest-mutation="true"
      data-no-static-abtest-result="true"
      data-segment-analysis-supported="false"
      data-no-segment-analysis-call="true"
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title={experiment.name}
        subtitle="A/B 实验详情使用 `/abtest/experiment/get|result|daily-trend|set-winner` 真实契约；分群分析和场次级话术替换尚未提供后端接口。"
        breadcrumbs={[{ label: 'AI' }, { label: 'A/B 实验' }, { label: '详情' }]}
      />
      {actionError ? (
        <Alert data-testid="abtest-detail-action-error" data-no-local-abtest-mutation="true" severity="error">
          {actionError}。失败不会本地切换状态、关闭确认弹窗或标记获胜者。
        </Alert>
      ) : null}
      {/* 顶部信息栏 */}
      <Card variant="outlined">
        <CardContent sx={{ pb: '12px !important' }}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
            <Box>
              <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                <Typography variant="h6" fontWeight={700}>{experiment.name}</Typography>
                <Chip label={statusCfg.label} color={statusCfg.color} size="small" />
                {experiment.winnerVariantId && (
                  <Chip icon={<EmojiEventsIcon />} label="已定胜出" color="warning" size="small" />
                )}
              </Stack>
              <Typography variant="body2" color="text.secondary">{experiment.description}</Typography>
            </Box>
            <Stack direction="row" spacing={1}>
              {experiment.status === 0 && (
                <Button size="small" variant="outlined" color="success"
                  onClick={() => statusUpdateMutation.mutate(1)}>启动</Button>
              )}
              {experiment.status === 1 && (
                <Button size="small" variant="outlined" color="warning"
                  onClick={() => statusUpdateMutation.mutate(3)}>暂停</Button>
              )}
              {(experiment.status === 1 || experiment.status === 3) && (
                <Button size="small" variant="outlined" color="error"
                  onClick={() => statusUpdateMutation.mutate(2)}>结束</Button>
              )}
            </Stack>
          </Stack>
          <Stack direction="row" spacing={3} sx={{ mt: 1.5 }}>
            <Box>
              <Typography variant="caption" color="text.secondary">实验类型</Typography>
              <Typography variant="body2" fontWeight={500}>{experiment.experimentType}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">目标实体</Typography>
              <Typography variant="body2" fontWeight={500}>{experiment.targetEntityId ? `${experiment.targetEntityType ?? '-'} #${experiment.targetEntityId}` : '-'}</Typography>
            </Box>
            {result && (
              <>
                <Box>
                  <Typography variant="caption" color="text.secondary">总曝光</Typography>
                  <Typography variant="body2" fontWeight={500}>{result.totalExposures.toLocaleString()}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">总转化</Typography>
                  <Typography variant="body2" fontWeight={500}>{result.totalConversions.toLocaleString()}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">综合转化率</Typography>
                  <Typography variant="body2" fontWeight={500}>{((result.overallConversionRate ?? 0) * 100).toFixed(2)}%</Typography>
                </Box>
              </>
            )}
          </Stack>
        </CardContent>
      </Card>

      {resultError && (
        <Alert
          data-testid="abtest-detail-result-error"
          data-no-static-abtest-result="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetchResult()}>重试</Button>}
        >
          统计结果加载失败（POST {ABTEST_DETAIL_ENDPOINTS.result}）：{getErrorMessage(resultLoadError)}（{detailContext()}）
        </Alert>
      )}

      {/* 统计显著性面板 */}
      {variants.length >= 2 && (
        <Card data-testid="abtest-detail-statistical-panel" data-derived-from-endpoints={`${ABTEST_DETAIL_ENDPOINTS.get}|${ABTEST_DETAIL_ENDPOINTS.result}`} variant="outlined" sx={{ borderColor: isSignificant ? 'success.light' : 'warning.light' }}>
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
              {isSignificant
                ? <CheckCircleIcon color="success" />
                : <WarningIcon color="warning" />
              }
              <Typography variant="subtitle2" fontWeight={700}>
                {isSignificant ? '实验结论：统计显著' : '实验结论：尚未达到统计显著'}
              </Typography>
            </Stack>
            {totalExposures < 50 ? (
              <Alert severity="warning">数据不足（{'<'} 50 样本），结论不可信</Alert>
            ) : (
              <Grid container spacing={2}>
                <Grid item xs={6} sm={3}>
                  <Typography variant="caption" color="text.secondary">p 值</Typography>
                  <Typography variant="body1" fontWeight={700} color={isSignificant ? 'success.main' : 'warning.main'}>
                    {isNaN(pValue) ? '—' : pValue.toFixed(4)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">（阈值 0.05）</Typography>
                </Grid>
                {variants.slice(0, 2).map((v, _i) => {
                  const [lo, hi] = calcCI(v)
                  return (
                    <Grid item xs={6} sm={3} key={v.id}>
                      <Typography variant="caption" color="text.secondary">{v.variantName} 95% 置信区间</Typography>
                      <Typography variant="body2" fontWeight={600}>
                        [{(lo * 100).toFixed(2)}%, {(hi * 100).toFixed(2)}%]
                      </Typography>
                      <Typography variant="caption" color="text.secondary">转化率 {((v.conversionRate ?? 0) * 100).toFixed(2)}%</Typography>
                    </Grid>
                  )
                })}
                {variants.length >= 2 && variants[0].conversionRate > 0 && (
                  <Grid item xs={6} sm={3}>
                    <Typography variant="caption" color="text.secondary">相对提升（效果量）</Typography>
                    <Typography variant="body1" fontWeight={700} color="primary.main">
                      {(((variants[1].conversionRate - variants[0].conversionRate) / variants[0].conversionRate) * 100).toFixed(1)}%
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      绝对提升 {((variants[1].conversionRate - variants[0].conversionRate) * 100).toFixed(2)}pp
                    </Typography>
                  </Grid>
                )}
              </Grid>
            )}
            {isSignificant && !experiment.winnerVariantId && (
              <Box sx={{ mt: 1.5 }}>
                <Button variant="contained" color="warning" size="small"
                  startIcon={<EmojiEventsIcon />}
                  onClick={() => setPromoteOpen(true)}>
                  确认获胜变体
                </Button>
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab 区域 */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label="变体对比" />
          <Tab label="每日趋势" />
          <Tab label="分群分析" />
        </Tabs>
      </Box>

      {/* Tab 0: 变体对比 */}
      {tab === 0 && (
        <Grid container spacing={2} data-testid="abtest-detail-variant-grid" data-source-endpoint={ABTEST_DETAIL_ENDPOINTS.result}>
          {variants.map(v => {
            const isBest = bestVariant?.id === v.id && variants.length >= 2
            const isWinner = experiment.winnerVariantId === v.id
            return (
              <Grid item xs={12} sm={6} md={4} key={v.id}>
                <Card variant="outlined" sx={{
                  borderColor: isWinner ? 'warning.main' : isBest ? 'success.light' : 'divider',
                  borderWidth: isWinner || isBest ? 2 : 1,
                }}>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                      <Typography variant="subtitle2" fontWeight={700}>{v.variantName}</Typography>
                      <Stack direction="row" spacing={0.5}>
                        {isWinner && <Chip icon={<EmojiEventsIcon />} label="获胜" color="warning" size="small" />}
                        {isBest && !isWinner && <Chip label="最优" color="success" size="small" variant="outlined" />}
                      </Stack>
                    </Stack>
                    <Typography variant="caption" color="text.secondary">话术风格：{v.scriptStyle || '—'}</Typography>
                    <Stack direction="row" justifyContent="space-between" alignItems="baseline" mt={1}>
                      <Typography variant="caption" color="text.secondary">转化率</Typography>
                      <Typography variant="h6" fontWeight={700} color="primary.main">
                        {((v.conversionRate ?? 0) * 100).toFixed(2)}%
                      </Typography>
                    </Stack>
                    <LinearProgress
                      variant="determinate"
                      value={maxRate > 0 ? ((v.conversionRate ?? 0) / maxRate) * 100 : 0}
                      sx={{ height: 6, borderRadius: 3, mt: 0.5 }}
                      color={isWinner ? 'warning' : 'primary'}
                    />
                    <Stack direction="row" justifyContent="space-between" mt={1}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">曝光</Typography>
                        <Typography variant="body2" fontWeight={500}>{(v.exposures ?? 0).toLocaleString()}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" color="text.secondary">转化</Typography>
                        <Typography variant="body2" fontWeight={500}>{(v.conversions ?? 0).toLocaleString()}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" color="text.secondary">流量占比</Typography>
                        <Typography variant="body2" fontWeight={500}>{v.trafficRatio}%</Typography>
                      </Box>
                    </Stack>
                    {experiment.status !== 3 && !experiment.winnerVariantId && (
                      <Button size="small" variant="outlined" sx={{ mt: 1.5 }} fullWidth
                        onClick={() => setWinnerMutation.mutate(v.id)}>设为获胜者</Button>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}

      {/* Tab 1: 每日趋势 */}
      {tab === 1 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>每日转化率趋势（近14天）</Typography>
            {trendError && (
              <Alert data-testid="abtest-detail-trend-error" data-no-local-trend-fallback="true" severity="error" sx={{ mb: 1 }} action={<Button color="inherit" size="small" onClick={() => refetchTrend()}>重试</Button>}>
                趋势加载失败（POST {ABTEST_DETAIL_ENDPOINTS.trend}）：{getErrorMessage(trendLoadError)}（{detailContext()}）
              </Alert>
            )}
            {trend.length > 0
              ? <Box data-testid="abtest-detail-trend-chart" data-source-endpoint={ABTEST_DETAIL_ENDPOINTS.trend}><LazyECharts option={trendOption} style={{ height: 320 }} /></Box>
              : <Alert data-testid="abtest-detail-trend-empty" data-no-local-trend-fallback="true" severity="info">暂无趋势数据</Alert>
            }
          </CardContent>
        </Card>
      )}

      {/* Tab 2: 分群分析 */}
      {tab === 2 && (
        <Card
          variant="outlined"
          data-testid="abtest-segment-analysis-panel"
          data-contract-status="unsupported"
          data-contract-endpoint={ABTEST_DETAIL_ENDPOINTS.segment}
          data-no-segment-analysis-call="true"
        >
          <CardContent>
            <Stack direction="row" spacing={2} alignItems="center" mb={2}>
              <Typography variant="subtitle2" fontWeight={700}>分群分析</Typography>
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <Select
                  value={segDim}
                  onChange={e => setSegDim(e.target.value)}
                  data-testid="abtest-segment-dimension-select"
                  inputProps={{
                    'data-testid': 'abtest-segment-dimension-input',
                    'data-contract-status': 'planning-only',
                    'data-contract-endpoint': ABTEST_DETAIL_ENDPOINTS.segment,
                  }}
                >
                  {SEG_DIMENSIONS.map(d => (
                    <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
            <Alert
              severity="warning"
              sx={{ mb: 2 }}
              data-testid="abtest-segment-analysis-downgrade"
              data-downgrade-tone="contract-gap"
              data-contract-endpoint={ABTEST_DETAIL_ENDPOINTS.segment}
              data-no-segment-analysis-call="true"
            >
              {SEGMENT_ANALYSIS_DOWNGRADE_MESSAGE} 当前上下文：{detailContext()}。
            </Alert>
            <Stack spacing={1.5}>
              <Alert severity="info">
                当前选择维度：{SEG_DIMENSIONS.find(d => d.value === segDim)?.label ?? segDim}。待后端提供按维度聚合的曝光、转化、转化率和显著性字段后再启用明细表。
              </Alert>
              <Grid container spacing={1.5}>
                {['请求参数：experimentId + dimension', '返回字段：segment + variants[]', '统计字段：exposures / conversions / conversionRate / pValue'].map(item => (
                  <Grid item xs={12} md={4} key={item}>
                    <Box
                      data-testid="abtest-segment-contract-card"
                      data-contract-status="required-before-enable"
                      sx={{ p: 1.5, border: 1, borderColor: 'divider', borderRadius: 1, bgcolor: 'action.hover' }}
                    >
                      <Typography variant="caption" color="text.secondary">{item}</Typography>
                    </Box>
                  </Grid>
                ))}
              </Grid>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* 推广获胜话术弹窗 */}
      <Dialog open={promoteOpen} onClose={() => setPromoteOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>确认获胜变体并结束实验</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            将变体「{bestVariant?.variantName ?? '—'}」设置为获胜变体，并把实验状态更新为已完成。
          </Typography>
          <FormControl>
            <RadioGroup value={promoteTarget} onChange={e => setPromoteTarget(e.target.value as 'all' | 'selected')}>
              <FormControlLabel value="all" control={<Radio />}
                label={<Box><Typography variant="body2">确认当前统计最优变体</Typography><Typography variant="caption" color="text.secondary">写入 winnerVariantId，并结束当前实验</Typography></Box>}
              />
              <FormControlLabel value="selected" control={<Radio />}
                label={<Box><Typography variant="body2">仅记录结论，后续人工应用</Typography><Typography variant="caption" color="text.secondary">同样写入获胜变体；场次替换需要后端新增接口</Typography></Box>}
              />
            </RadioGroup>
          </FormControl>
          <Alert severity="info" sx={{ mt: 2 }}>
            当前后端支持 `/abtest/experiment/set-winner`：会设置获胜变体并结束实验；不会自动替换直播场次或短视频项目中的话术。
          </Alert>
          {actionError ? <Alert data-testid="abtest-detail-winner-dialog-error" data-input-retained="true" severity="error" sx={{ mt: 1 }}>{actionError}。确认失败会保留当前弹窗。</Alert> : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPromoteOpen(false)}>取消</Button>
          <Button
            variant="contained"
            color="warning"
            disabled={!bestVariant || setWinnerMutation.isPending}
            onClick={() => {
              if (!bestVariant) return
              setWinnerMutation.mutate(bestVariant.id, {
                onSuccess: () => setPromoteOpen(false),
              })
            }}
          >
            确认获胜
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}





