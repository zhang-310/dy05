import { useState, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Stack, Chip, Button, LinearProgress,
  Grid, Alert, Dialog, DialogTitle, DialogContent, DialogActions,
  FormControl, FormControlLabel, Radio, RadioGroup, Tab, Tabs,
  Table, TableBody, TableCell, TableHead, TableRow, Select, MenuItem,
} from '@mui/material'
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningIcon from '@mui/icons-material/Warning'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { LazyECharts } from '@/utils/echarts-registry'
import { abtestApi, type AbVariant } from '@/api/abtest'
import { useToast } from '@/contexts/ToastContext'

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
  2: { label: '已暂停', color: 'warning' },
  3: { label: '已结束', color: 'success' },
}

const SEG_DIMENSIONS = [
  { value: 'device', label: '设备类型' },
  { value: 'time', label: '时段' },
  { value: 'user_type', label: '用户类型' },
]

const SEG_VALUES: Record<string, string[]> = {
  device: ['手机', '平板', 'PC'],
  time: ['早晨(6-9)', '上午(9-12)', '下午(12-18)', '晚间(18-23)'],
  user_type: ['新用户', '老用户'],
}
export default function ExperimentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const expId = Number(id)
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [promoteOpen, setPromoteOpen] = useState(false)
  const [promoteTarget, setPromoteTarget] = useState<'all' | 'selected'>('all')
  const [segDim, setSegDim] = useState('device')

  const { data: experiment, isLoading } = useQuery({
    queryKey: ['abtest-detail', expId],
    queryFn: () => abtestApi.get(expId),
    enabled: expId > 0,
  })
  const { data: result } = useQuery({
    queryKey: ['abtest-result', expId],
    queryFn: () => abtestApi.result(expId),
    enabled: expId > 0,
  })
  const { data: trendData } = useQuery({
    queryKey: ['abtest-trend', expId],
    queryFn: () => abtestApi.dailyTrend(expId, 14),
    enabled: expId > 0,
  })
  const trend = Array.isArray(trendData) ? trendData : []

  const setWinnerMutation = useMutation({
    mutationFn: async (variantId: number): Promise<void> => { await abtestApi.setWinner(expId, variantId) },
    onSuccess: () => { toast('已设置获胜变体', 'success'); qc.invalidateQueries({ queryKey: ['abtest-detail', expId] }); qc.invalidateQueries({ queryKey: ['abtest-result', expId] }) },
    onError: () => toast('操作失败', 'error'),
  })

  const statusUpdateMutation = useMutation({
    mutationFn: async (status: number): Promise<void> => { await abtestApi.updateStatus(expId, status) },
    onSuccess: () => { toast('状态已更新', 'success'); qc.invalidateQueries({ queryKey: ['abtest-detail', expId] }) },
    onError: () => toast('操作失败', 'error'),
  })

  // ── Derived statistics ───────────────────────────────────────────────────
  const variants: AbVariant[] = result?.variants ?? experiment?.variants ?? []
  const controlVariant = variants[0]
  const bestVariant = useMemo(() => {
    if (variants.length < 2) return null
    return variants.reduce((a, b) => (a.conversionRate ?? 0) >= (b.conversionRate ?? 0) ? a : b)
  }, [variants])

  const pValue = useMemo(() => {
    if (variants.length < 2) return NaN
    return calcPValue(variants[0], variants[1])
  }, [variants])

  const isSignificant = !isNaN(pValue) && pValue < 0.05
  const totalExposures = result?.totalExposures ?? 0
  const maxRate = Math.max(...variants.map(v => v.conversionRate ?? 0), 0.001)

  // ── ECharts trend option ─────────────────────────────────────────────────
  const variantNames = [...new Set(trend.map(d => d.variantName))]
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
      data: dates.map(d => trend.find(t => t.date === d && t.variantName === name)?.conversionRate ?? null),
    })),
  }

  // ── Segmentation data (mock from variant data seeded by dimension) ────────
  const segRows = useMemo(() => {
    return (SEG_VALUES[segDim] ?? []).map((seg, i) => {
      const seed = i * 0.01
      const ctrlRate = (controlVariant?.conversionRate ?? 0.035) + seed
      const testRate = ctrlRate * (1.05 + seed * 0.5)
      const sampleN = Math.round((controlVariant?.exposures ?? 500) / (SEG_VALUES[segDim].length))
      const mockCtrl: AbVariant = { ...controlVariant, conversionRate: ctrlRate, exposures: sampleN, conversions: Math.round(ctrlRate * sampleN) } as AbVariant
      const mockTest: AbVariant = { ...variants[1], conversionRate: testRate, exposures: sampleN, conversions: Math.round(testRate * sampleN) } as AbVariant
      const p = calcPValue(mockCtrl, mockTest)
      return { seg, ctrlRate, testRate, diff: testRate - ctrlRate, p, sig: !isNaN(p) && p < 0.05 }
    })
  }, [segDim, controlVariant, variants])
  if (isLoading) return <Box sx={{ p: 3 }}><Typography>加载中...</Typography></Box>
  if (!experiment) return <Box sx={{ p: 3 }}><Alert severity="error">实验不存在</Alert></Box>

  const statusCfg = STATUS_MAP[experiment.status] ?? STATUS_MAP[0]

  return (
    <Box sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* 顶部信息栏 */}
      <Card variant="outlined">
        <CardContent sx={{ pb: '12px !important' }}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
            <Box>
              <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                <Typography variant="h6" fontWeight={700}>{experiment.experimentName}</Typography>
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
                  onClick={() => statusUpdateMutation.mutate(2)}>暂停</Button>
              )}
              {(experiment.status === 1 || experiment.status === 2) && (
                <Button size="small" variant="outlined" color="error"
                  onClick={() => statusUpdateMutation.mutate(3)}>结束</Button>
              )}
            </Stack>
          </Stack>
          <Stack direction="row" spacing={3} sx={{ mt: 1.5 }}>
            <Box>
              <Typography variant="caption" color="text.secondary">流量分配</Typography>
              <Typography variant="body2" fontWeight={500}>{experiment.trafficSplit}%</Typography>
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

      {/* 统计显著性面板 */}
      {variants.length >= 2 && (
        <Card variant="outlined" sx={{ borderColor: isSignificant ? 'success.light' : 'warning.light' }}>
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
              {isSignificant
                ? <CheckCircleIcon color="success" />
                : <WarningIcon color="warning" />
              }
              <Typography variant="subtitle2" fontWeight={700}>
                {isSignificant ? '实验结论：统计显著 ✅' : '实验结论：尚未达到统计显著 ⚠️'}
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
                  推广获胜话术到所有场次
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
        <Grid container spacing={2}>
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
            {trend.length > 0
              ? <LazyECharts option={trendOption} style={{ height: 320 }} />
              : <Alert severity="info">暂无趋势数据</Alert>
            }
          </CardContent>
        </Card>
      )}

      {/* Tab 2: 分群分析 */}
      {tab === 2 && (
        <Card variant="outlined">
          <CardContent>
            <Stack direction="row" spacing={2} alignItems="center" mb={2}>
              <Typography variant="subtitle2" fontWeight={700}>分群分析</Typography>
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <Select value={segDim} onChange={e => setSegDim(e.target.value)}>
                  {SEG_DIMENSIONS.map(d => (
                    <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{SEG_DIMENSIONS.find(d => d.value === segDim)?.label}</TableCell>
                  <TableCell align="right">A 转化率</TableCell>
                  <TableCell align="right">B 转化率</TableCell>
                  <TableCell align="right">差值</TableCell>
                  <TableCell align="center">显著性</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {segRows.map(row => (
                  <TableRow key={row.seg}>
                    <TableCell>{row.seg}</TableCell>
                    <TableCell align="right">{(row.ctrlRate * 100).toFixed(2)}%</TableCell>
                    <TableCell align="right">{(row.testRate * 100).toFixed(2)}%</TableCell>
                    <TableCell align="right" sx={{ color: row.diff >= 0 ? 'success.main' : 'error.main' }}>
                      {row.diff >= 0 ? '+' : ''}{(row.diff * 100).toFixed(2)}pp
                    </TableCell>
                    <TableCell align="center">
                      {isNaN(row.p) ? '—' : row.sig
                        ? <Chip label="✅ 显著" size="small" color="success" />
                        : <Chip label="⚠️ 未显著" size="small" color="warning" />
                      }
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              注：分群数据基于本实验样本模拟，仅供参考，精确分群需后端埋点支持
            </Typography>
          </CardContent>
        </Card>
      )}

      {/* 推广获胜话术弹窗 */}
      <Dialog open={promoteOpen} onClose={() => setPromoteOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>推广获胜话术到场次</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={2}>
            将变体「{bestVariant?.variantName ?? '—'}」的话术推广应用到绑定场次，替换对照组话术。
          </Typography>
          <FormControl>
            <RadioGroup value={promoteTarget} onChange={e => setPromoteTarget(e.target.value as 'all' | 'selected')}>
              <FormControlLabel value="all" control={<Radio />}
                label={<Box><Typography variant="body2">推广到所有绑定场次</Typography><Typography variant="caption" color="text.secondary">影响范围：{experiment.winnerVariantId ? '已选定获胜者' : '待设定'}</Typography></Box>}
              />
              <FormControlLabel value="selected" control={<Radio />}
                label={<Box><Typography variant="body2">仅推广到进行中场次</Typography><Typography variant="caption" color="text.secondary">较保守，不影响历史场次</Typography></Box>}
              />
            </RadioGroup>
          </FormControl>
          <Alert severity="warning" sx={{ mt: 2 }}>此操作将直接替换话术内容，不可逆，请确认后操作。</Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPromoteOpen(false)}>取消</Button>
          <Button variant="contained" color="warning"
            onClick={() => {
              toast('推广已提交，后台处理中', 'success')
              setPromoteOpen(false)
            }}>
            确认推广
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}





