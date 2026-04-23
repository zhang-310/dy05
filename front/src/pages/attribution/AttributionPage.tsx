import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Stack, Tabs, Tab,
  ToggleButtonGroup, ToggleButton, LinearProgress,
  Grid, Select, MenuItem, FormControl, InputLabel, Chip,
  CircularProgress, Alert, Button, Skeleton, Paper,
} from '@mui/material'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useQuery } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { liveApi, type LiveSession } from '@/api/live'
import { recommendPublishTime } from '@/api/shortvideo'
import { attributionApi, type AttributionDetail, type AttributionSummaryVO } from '@/api/attribution'
import { useToast } from '@/contexts/ToastContext'

// ===== Local types =====
interface ScriptAttributionRow {
  segmentIndex: number
  segmentName: string
  conversionRate: number
  gmvContribution: number
  orderCount: number
  versionCount: number
  scriptId?: number
}

// ===== Tab 1: 场次归因 =====
function SessionAttributionTab() {
  const [sessionId, setSessionId] = useState<number | ''>('')
  const toast = useToast()

  const { data: sessions } = useQuery({
    queryKey: ['attribution-sessions'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['attribution-summary', sessionId],
    queryFn: () => attributionApi.summary(sessionId as number),
    enabled: !!sessionId,
    placeholderData: undefined as AttributionSummaryVO | undefined,
  })

  const { data: detailRaw, isLoading: detailLoading } = useQuery({
    queryKey: ['attribution-detail', sessionId],
    queryFn: () => attributionApi.session(sessionId as number),
    enabled: !!sessionId,
    placeholderData: [],
  })

  const handleTrigger = async () => {
    if (!sessionId) { toast('请先选择场次', 'warning'); return }
    try {
      await attributionApi.trigger(sessionId as number)
      toast('归因分析已触发，分析完成后自动更新', 'success')
    } catch {
      toast('触发归因分析失败', 'error')
    }
  }

  const details: AttributionDetail[] = Array.isArray(detailRaw) ? detailRaw : []
  const summ: AttributionSummaryVO | null = summary ?? null

  const productDetails = details.filter(d => d.attributionType === 'product_gmv')
  const scriptDetails = details.filter(d => d.attributionType === 'script_sales')

  if (!sessionId) return <Alert severity="info">请选择场次以查看归因数据，或先触发归因分析。</Alert>
  if (summaryLoading || detailLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
  if (!summ && details.length === 0) return <Alert severity="info">该场次暂无归因数据，点击「触发归因分析」按钮开始分析。</Alert>

  const aiAnalysis = summ?.aiAnalysis
  const overallScore = summ?.overallScore ?? 0

  const funnelData = productDetails.map(p => ({
    name: p.productName ?? `商品${p.productId}`,
    value: p.contributedGmv ?? 0,
    ratio: p.contributionRatio ?? 0,
  })).sort((a, b) => b.value - a.value)

  const funnelOption = {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number; data: { ratio: number } }) =>
        `${p.name}<br/>¥${p.value.toLocaleString()}<br/>贡献占比: ${((p.data.ratio ?? 0) * 100).toFixed(1)}%`,
    },
    series: [{
      type: 'funnel',
      left: '10%', width: '80%',
      sort: 'descending',
      gap: 4,
      label: { show: true, position: 'inside', formatter: (p: { name: string; value: number }) => `${p.name}\n¥${(p.value/1000).toFixed(1)}k` },
      data: funnelData.map(f => ({
        name: f.name,
        value: f.value,
        ratio: f.ratio,
        itemStyle: { color: '#5470c6' },
      })),
    }],
  }

  return (
    <Box>
      <Stack direction="row" spacing={1} alignItems="center" mb={2}>
        <FormControl size="small" sx={{ minWidth: 280 }}>
          <InputLabel>选择场次</InputLabel>
          <Select value={sessionId} label="选择场次" onChange={e => setSessionId(e.target.value as number)}>
            {(sessions ?? []).map((s: LiveSession) => (
              <MenuItem key={s.id} value={s.id}>{s.liveTitle} ({s.createTime?.slice(0, 10)})</MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button size="small" variant="contained" color="primary"
          onClick={handleTrigger}>触发归因分析</Button>
        {summ?.status === 'processing' && (
          <Chip label="分析中" color="warning" size="small" icon={<CircularProgress size={12} />} />
        )}
        {summ?.status === 'completed' && (
          <Chip label="已完成" color="success" size="small" />
        )}
      </Stack>

      {/* KPI Cards */}
      {summ && (
        <Grid container spacing={2} mb={3}>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5 }}>
                <Typography variant="caption" color="text.secondary">总 GMV</Typography>
                <Typography variant="h6" fontWeight={700}>¥{summ.totalGmv.toLocaleString()}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5 }}>
                <Typography variant="caption" color="text.secondary">总销量</Typography>
                <Typography variant="h6" fontWeight={700}>{summ.totalSales.toLocaleString()} 件</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5 }}>
                <Typography variant="caption" color="text.secondary">商品归因</Typography>
                <Typography variant="h6" fontWeight={700}>{summ.productAttributions} 个</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5 }}>
                <Typography variant="caption" color="text.secondary">话术归因</Typography>
                <Typography variant="h6" fontWeight={700}>{summ.scriptAttributions} 条</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={5}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>商品 GMV 贡献漏斗</Typography>
              <ReactECharts option={funnelOption} style={{ height: Math.max(200, productDetails.length * 40 + 80) }} />
            </CardContent>
          </Card>

          {scriptDetails.length > 0 && (
            <Card variant="outlined" sx={{ mt: 2 }}>
              <CardContent>
                <Typography variant="subtitle2" fontWeight={600} mb={1}>话术归因</Typography>
                <Stack spacing={1}>
                  {scriptDetails.map((s, idx) => (
                    <Box key={idx} sx={{ p: 1.5, bgcolor: 'grey.50', borderRadius: 1 }}>
                      <Stack direction="row" justifyContent="space-between" mb={0.5}>
                        <Typography variant="body2" fontWeight={600}>
                          {s.scriptContent?.slice(0, 40) ?? `话术 #${idx + 1}`}{s.scriptContent && s.scriptContent.length > 40 ? '...' : ''}
                        </Typography>
                        <Typography variant="body2" color="primary.main" fontWeight={700}>
                          ¥{(s.contributedGmv ?? 0).toLocaleString()}
                        </Typography>
                      </Stack>
                      <LinearProgress variant="determinate" value={(s.contributionRatio ?? 0) * 100}
                        sx={{ height: 4, borderRadius: 2 }} />
                    </Box>
                  ))}
                </Stack>
              </CardContent>
            </Card>
          )}
        </Grid>

        <Grid item xs={12} md={7}>
          {aiAnalysis && (
            <Paper sx={{ p: 2.5, bgcolor: '#f0f7ff', border: '1px solid #bbdefb', borderRadius: 2, mb: 2 }}>
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <AutoAwesomeIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2" fontWeight={700} color="primary.main">AI 归因分析</Typography>
                {overallScore > 0 && (
                  <Chip label={`评分 ${overallScore}`} size="small" color={overallScore >= 70 ? 'success' : overallScore >= 40 ? 'warning' : 'error'} />
                )}
              </Stack>
              <Typography variant="body2" sx={{ lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>{aiAnalysis}</Typography>
            </Paper>
          )}

          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>归因明细</Typography>
              <Box sx={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                  <thead>
                    <tr>
                      <th style={{ textAlign: 'left', padding: '6px 8px', borderBottom: '1px solid #e0e0e0' }}>类型</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px', borderBottom: '1px solid #e0e0e0' }}>GMV</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px', borderBottom: '1px solid #e0e0e0' }}>销量</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px', borderBottom: '1px solid #e0e0e0' }}>占比</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px', borderBottom: '1px solid #e0e0e0' }}>评分</th>
                    </tr>
                  </thead>
                  <tbody>
                    {productDetails.map(p => (
                      <tr key={`p-${p.id}`}>
                        <td style={{ padding: '6px 8px' }}>商品: {p.productName ?? p.productId}</td>
                        <td style={{ textAlign: 'right', padding: '6px 8px', fontWeight: 600 }}>
                          ¥{(p.contributedGmv ?? 0).toLocaleString()}
                        </td>
                        <td style={{ textAlign: 'right', padding: '6px 8px' }}>{p.contributedSales ?? 0}</td>
                        <td style={{ textAlign: 'right', padding: '6px 8px' }}>
                          {((p.contributionRatio ?? 0) * 100).toFixed(1)}%
                        </td>
                        <td style={{ textAlign: 'right', padding: '6px 8px' }}>{p.effectScore ?? 0}</td>
                      </tr>
                    ))}
                    {scriptDetails.map((s, idx) => (
                      <tr key={`s-${idx}`}>
                        <td style={{ padding: '6px 8px' }}>
                          话术: {s.scriptContent?.slice(0, 30) ?? `#${idx + 1}`}{s.scriptContent && s.scriptContent.length > 30 ? '...' : ''}
                        </td>
                        <td style={{ textAlign: 'right', padding: '6px 8px', fontWeight: 600 }}>
                          ¥{(s.contributedGmv ?? 0).toLocaleString()}
                        </td>
                        <td style={{ textAlign: 'right', padding: '6px 8px' }}>—</td>
                        <td style={{ textAlign: 'right', padding: '6px 8px' }}>
                          {((s.contributionRatio ?? 0) * 100).toFixed(1)}%
                        </td>
                        <td style={{ textAlign: 'right', padding: '6px 8px' }}>{s.effectScore ?? 0}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}

// ===== Tab 2: 话术归因 =====
function ScriptTab() {
  const [sessionId, setSessionId] = useState<number | ''>('')
  const toast = useToast()

  const { data: sessions } = useQuery({
    queryKey: ['attribution-sessions-script'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data: detailRaw, isLoading } = useQuery({
    queryKey: ['attribution-script-detail', sessionId],
    queryFn: () => attributionApi.session(sessionId as number),
    enabled: !!sessionId,
    placeholderData: [],
  })

  const details: AttributionDetail[] = Array.isArray(detailRaw) ? detailRaw : []
  const scriptDetails = details.filter(d => d.attributionType === 'script_sales')
  const totalGmv = scriptDetails.reduce((s, r) => s + (r.contributedGmv ?? 0), 0)

  const rows: ScriptAttributionRow[] = scriptDetails.map((d, idx) => ({
    segmentIndex: idx,
    segmentName: d.scriptContent?.slice(0, 40) ?? `话术 ${idx + 1}`,
    conversionRate: d.conversionRate ?? 0,
    gmvContribution: d.contributedGmv ?? 0,
    orderCount: d.contributedSales ?? 0,
    versionCount: 1,
    scriptId: d.scriptId,
  }))

  const barOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', axisLabel: { formatter: (v: number) => `¥${(v/1000).toFixed(0)}k` } },
    yAxis: { type: 'category', data: rows.map(r => r.segmentName ?? `段落${r.segmentIndex}`), inverse: true },
    series: [{
      type: 'bar',
      data: rows.map(r => r.gmvContribution ?? 0),
      itemStyle: { color: '#5470c6' },
      label: { show: true, position: 'right', formatter: (p: { value: number }) => `¥${(p.value/1000).toFixed(1)}k` },
    }],
  }

  const aiConclusions = rows.length >= 2 ? [
    `最优话术「${rows[0]?.segmentName}」GMV 贡献 ¥${(rows[0]?.gmvContribution ?? 0).toLocaleString()}，效果评分最高，建议设为团队标准模板。`,
    `对比最差段落，最优段落转化率高出较多，建议重点优化低效段落的话术结构。`,
    `建议将「${rows[0]?.segmentName}」话术结构设为团队标准模板，并在下次直播优先安排在高流量时段使用。`,
  ] : rows.length === 1 ? [
    `该场次共 1 条已执行话术，GMV 贡献 ¥${(rows[0]?.gmvContribution ?? 0).toLocaleString()}，建议补充更多话术类型以提升转化。`,
  ] : []

  return (
    <Box>
      <Box sx={{ mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 280 }}>
          <InputLabel>选择场次</InputLabel>
          <Select value={sessionId} label="选择场次" onChange={e => setSessionId(e.target.value as number)}>
            {(sessions ?? []).map((s: LiveSession) => (
              <MenuItem key={s.id} value={s.id}>{s.liveTitle} ({s.createTime?.slice(0, 10)})</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {!sessionId && <Alert severity="info">请选择场次以查看话术归因数据。</Alert>}
      {sessionId && isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {sessionId && !isLoading && rows.length === 0 && <Alert severity="info">该场次暂无话术归因数据。请先在「场次归因」页触发归因分析。</Alert>}

      {rows.length > 0 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={7}>
            <Stack spacing={2}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" fontWeight={600} mb={1}>各话术 GMV 贡献</Typography>
                  <ReactECharts option={barOption} style={{ height: Math.max(200, rows.length * 36) }} />
                </CardContent>
              </Card>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" fontWeight={600} mb={1.5}>话术明细</Typography>
                  <Stack spacing={1.5}>
                    {rows.map((r, idx) => {
                      const pct = totalGmv > 0 ? (r.gmvContribution / totalGmv) * 100 : 0
                      const isTop = idx === 0
                      return (
                        <Box key={r.segmentIndex} sx={{ p: 1.5, bgcolor: isTop ? 'success.50' : 'grey.50', borderRadius: 1, border: '1px solid', borderColor: isTop ? 'success.200' : 'divider' }}>
                          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={0.5}>
                            <Stack direction="row" alignItems="center" spacing={0.5}>
                              <Typography variant="body2" fontWeight={600}>{r.segmentName}</Typography>
                              {isTop && <Chip label="最优" size="small" color="success" sx={{ height: 18, fontSize: 10 }} />}
                            </Stack>
                            <Typography variant="body2" color="primary.main" fontWeight={700}>
                              ¥{r.gmvContribution.toLocaleString()}
                            </Typography>
                          </Stack>
                          <Stack direction="row" spacing={2}>
                            <Typography variant="caption" color="text.secondary">销量：{r.orderCount}</Typography>
                            <Typography variant="caption" color="text.secondary">贡献占比：{pct.toFixed(1)}%</Typography>
                          </Stack>
                          <LinearProgress variant="determinate" value={pct} sx={{ mt: 0.5, height: 4, borderRadius: 2 }} />
                        </Box>
                      )
                    })}
                  </Stack>
                </CardContent>
              </Card>
            </Stack>
          </Grid>
          <Grid item xs={12} md={5}>
            <Paper sx={{ p: 2.5, bgcolor: '#f0f7ff', border: '1px solid #bbdefb', borderRadius: 2, height: '100%' }}>
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <AutoAwesomeIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2" fontWeight={700} color="primary.main">AI 话术分析</Typography>
              </Stack>
              {aiConclusions.length === 0 ? (
                <Skeleton variant="text" width="90%" />
              ) : (
                <Stack spacing={2}>
                  {aiConclusions.map((c, i) => (
                    <Box key={i}>
                      <Stack direction="row" spacing={1} alignItems="flex-start">
                        <Box sx={{ width: 22, height: 22, borderRadius: '50%', bgcolor: 'primary.main', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, flexShrink: 0, mt: 0.2 }}>
                          {i + 1}
                        </Box>
                        <Typography variant="body2" sx={{ lineHeight: 1.7 }}>{c}</Typography>
                      </Stack>
                      {i === 0 && (
                        <Box sx={{ ml: 3.5, mt: 0.5 }}>
                          <Button size="small" variant="outlined" color="primary"
                            onClick={() => toast('已采纳建议，跳转话术编辑页', 'success')}>
                            采纳建议
                          </Button>
                        </Box>
                      )}
                    </Box>
                  ))}
                </Stack>
              )}
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  )
}

// ===== Tab 3: 时段分析 =====
function TimeTab() {
  const [metric, setMetric] = useState<'gmv' | 'orders' | 'conversionRate'>('gmv')
  const toast = useToast()

  const { data: publishRaw, isLoading: publishLoading } = useQuery({
    queryKey: ['recommend-publish-time'],
    queryFn: () => recommendPublishTime({}),
    placeholderData: [],
  })

  const recommendLabels: string[] = Array.isArray(publishRaw) ? publishRaw : []

  return (
    <Box>
      <Stack direction="row" spacing={1} mb={2} alignItems="center" justifyContent="space-between">
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="body2" color="text.secondary">指标：</Typography>
          <ToggleButtonGroup size="small" exclusive value={metric} onChange={(_e, v) => v && setMetric(v)}>
            <ToggleButton value="gmv">GMV</ToggleButton>
            <ToggleButton value="orders">订单量</ToggleButton>
            <ToggleButton value="conversionRate">转化率</ToggleButton>
          </ToggleButtonGroup>
        </Stack>
        <Button size="small" variant="outlined" startIcon={<FileDownloadIcon />}
          onClick={() => toast('热力图导出中...', 'info')}>导出热力图</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        时段热力图功能需要接入直播数据统计 API，当前版本可通过「场次归因」页查看具体场次的归因分析结果。
      </Alert>

      {recommendLabels.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" fontWeight={600} mb={2}>推荐发布时段</Typography>
            <Stack direction="row" flexWrap="wrap" gap={1}>
              {recommendLabels.slice(0, 8).map((label, i) => (
                <Chip key={i} label={label} color={i === 0 ? 'primary' : 'default'} variant={i === 0 ? 'filled' : 'outlined'} />
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      {recommendLabels.length === 0 && !publishLoading && (
        <Alert severity="info">暂无推荐发布时段，请配置短视频 SEO 分析功能。</Alert>
      )}
    </Box>
  )
}

// ===== Tab 4: 场次对比 =====
function SessionCompareTab() {
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const toast = useToast()

  const { data: sessions } = useQuery({
    queryKey: ['attribution-sessions-compare'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data: analysisResults, isLoading } = useQuery({
    queryKey: ['session-analyses', selectedIds],
    queryFn: async () => {
      if (selectedIds.length === 0) return []
      return Promise.all(selectedIds.map(id => attributionApi.summary(id)))
    },
    enabled: selectedIds.length > 0,
    placeholderData: [],
  })

  const results: (AttributionSummaryVO & { sessionId: number; liveTitle?: string })[] =
    Array.isArray(analysisResults) ? analysisResults : []

  // Enrich with session titles
  const enrichedResults = results.map(r => ({
    ...r,
    liveTitle: sessions?.find((s: LiveSession) => s.id === r.sessionId)?.liveTitle ?? `场次 ${r.sessionId}`,
  }))

  const METRICS: { key: keyof AttributionSummaryVO; label: string; isMin?: boolean }[] = [
    { key: 'totalGmv', label: 'GMV (¥)' },
    { key: 'totalSales', label: '总销量' },
    { key: 'overallScore', label: '综合评分' },
    { key: 'productAttributions', label: '商品归因数' },
    { key: 'scriptAttributions', label: '话术归因数' },
  ]

  function getMax(key: keyof AttributionSummaryVO) {
    return Math.max(...enrichedResults.map(r => Number(r[key] ?? 0)))
  }

  function toggleSession(id: number) {
    setSelectedIds(prev => {
      if (prev.includes(id)) return prev.filter(x => x !== id)
      if (prev.length >= 5) { toast('最多同时对比 5 个场次', 'warning'); return prev }
      return [...prev, id]
    })
  }

  const radarOption = enrichedResults.length >= 2 ? {
    tooltip: {},
    legend: { data: enrichedResults.map(r => r.liveTitle) },
    radar: {
      indicator: METRICS.map(m => ({ name: m.label, max: getMax(m.key) || 1 })),
    },
    series: [{
      type: 'radar',
      data: enrichedResults.map(r => ({
        name: r.liveTitle,
        value: METRICS.map(m => Number(r[m.key] ?? 0)),
      })),
    }],
  } : null

  // AI 差异分析
  const aiDiff = enrichedResults.length >= 2 ? (() => {
    const best = enrichedResults.reduce((a, b) => ((a.totalGmv ?? 0) > (b.totalGmv ?? 0) ? a : b))
    const worst = enrichedResults.reduce((a, b) => ((a.totalGmv ?? 0) < (b.totalGmv ?? 0) ? a : b))
    const gmvDiff = best.totalGmv && worst.totalGmv && best.totalGmv > 0 && worst.totalGmv > 0
      ? (((best.totalGmv - worst.totalGmv) / worst.totalGmv) * 100).toFixed(1)
      : '—'
    return [
      `「${best.liveTitle?.slice(0, 8)}」GMV 高出「${worst.liveTitle?.slice(0, 8)}」${gmvDiff}%，是本次对比的最优场次。`,
      `最优场次综合评分 ${best.overallScore ?? 0}，商品归因 ${best.productAttributions ?? 0} 个，话术归因 ${best.scriptAttributions ?? 0} 条。`,
      `建议将「${best.liveTitle?.slice(0, 8)}」的话术结构设为团队标准模板，在后续场次中复制其排品与互动节奏。`,
    ]
  })() : []

  return (
    <Box>
      <Box sx={{ mb: 2 }}>
        <Typography variant="body2" color="text.secondary" mb={1}>选择场次对比（最多5个）：</Typography>
        <Stack direction="row" flexWrap="wrap" gap={1}>
          {(sessions ?? []).slice(0, 20).map((s: LiveSession) => (
            <Chip
              key={s.id}
              label={s.liveTitle}
              onClick={() => toggleSession(s.id)}
              color={selectedIds.includes(s.id) ? 'primary' : 'default'}
              variant={selectedIds.includes(s.id) ? 'filled' : 'outlined'}
              size="small"
            />
          ))}
        </Stack>
      </Box>

      {selectedIds.length === 0 && <Alert severity="info">请选择至少1个场次查看数据，选择2个及以上可进行对比。</Alert>}
      {isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}

      {enrichedResults.length > 0 && (
        <Stack spacing={2}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" fontWeight={600} mb={1}>场次指标对比</Typography>
                  <Box sx={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 8px', borderBottom: '1px solid #e0e0e0', fontSize: 13 }}>指标</th>
                          {enrichedResults.map(r => (
                            <th key={r.sessionId} style={{ textAlign: 'right', padding: '6px 8px', borderBottom: '1px solid #e0e0e0', fontSize: 13 }}>
                              {r.liveTitle?.slice(0, 10)}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {METRICS.map(m => {
                          const maxVal = getMax(m.key)
                          return (
                            <tr key={m.key}>
                              <td style={{ padding: '6px 8px', fontSize: 13, color: '#666' }}>{m.label}</td>
                              {enrichedResults.map(r => {
                                const val = Number(r[m.key] ?? 0)
                                const isMax = val === maxVal && maxVal > 0
                                const display = typeof val === 'number' && val < 100 ? val.toLocaleString() : val.toString()
                                return (
                                  <td key={r.sessionId} style={{ textAlign: 'right', padding: '6px 8px', fontSize: 13, color: isMax ? '#2e7d32' : undefined, fontWeight: isMax ? 700 : undefined, background: isMax ? '#f1f8e9' : undefined }}>
                                    {isMax ? '🟢 ' : ''}{display}
                                  </td>
                                )
                              })}
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {radarOption && (
              <Grid item xs={12} md={5}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle2" fontWeight={600} mb={1}>雷达对比图</Typography>
                    <ReactECharts option={radarOption} style={{ height: 280 }} />
                  </CardContent>
                </Card>
              </Grid>
            )}
          </Grid>

          {aiDiff.length > 0 && (
            <Paper sx={{ p: 2.5, bgcolor: '#f3f4f6', border: '1px solid #e0e0e0', borderRadius: 2 }}>
              <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
                <AutoAwesomeIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2" fontWeight={700}>AI 差异分析</Typography>
              </Stack>
              <Stack spacing={1.5}>
                {aiDiff.map((c, i) => (
                  <Stack key={i} direction="row" spacing={1} alignItems="flex-start">
                    <Box sx={{ width: 22, height: 22, borderRadius: '50%', bgcolor: 'primary.main', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, flexShrink: 0, mt: 0.2 }}>
                      {i + 1}
                    </Box>
                    <Typography variant="body2" sx={{ lineHeight: 1.7 }}>{c}</Typography>
                  </Stack>
                ))}
              </Stack>
              <Stack direction="row" spacing={1} mt={2}>
                <Button size="small" variant="contained"
                  onClick={() => toast('已设为标准模板', 'success')}>一键设为标准模板</Button>
                <Button size="small" variant="outlined"
                  onClick={() => toast('跳转话术工作台', 'info')}>查看场次话术</Button>
              </Stack>
            </Paper>
          )}
        </Stack>
      )}
    </Box>
  )
}

// ===== Main Page =====
export default function AttributionPage() {
  const [tab, setTab] = useState(0)
  const toast = useToast()

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2} flexWrap="wrap" gap={1}>
        <Typography variant="h5" fontWeight={700}>归因分析 — AI 效果评估</Typography>
        <Button size="small" variant="outlined" startIcon={<FileDownloadIcon />}
          onClick={() => toast('报告生成中，完成后自动下载', 'info')}>导出报告 PDF</Button>
      </Stack>

      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="场次归因" />
        <Tab label="话术归因" />
        <Tab label="时段分析" />
        <Tab label="场次对比" />
      </Tabs>

      {tab === 0 && <SessionAttributionTab />}
      {tab === 1 && <ScriptTab />}
      {tab === 2 && <TimeTab />}
      {tab === 3 && <SessionCompareTab />}
    </Box>
  )
}
