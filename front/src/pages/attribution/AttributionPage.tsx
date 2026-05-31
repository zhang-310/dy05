import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Stack, Tabs, Tab,
  ToggleButtonGroup, ToggleButton, LinearProgress,
  Grid, Select, MenuItem, FormControl, InputLabel, Chip,
  CircularProgress, Alert, Button, Skeleton, Paper,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useQuery } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { ErrorAlert, PageHeader } from '@/components/base'
import { liveApi, type LiveSession } from '@/api/live'
import { recommendPublishTime } from '@/api/shortvideo'
import { attributionApi, type AttributionDetail, type AttributionSummaryVO } from '@/api/attribution'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'

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

const ATTRIBUTION_READY_ENDPOINTS = '/live/session/search,/ai/attribution/trigger,/ai/attribution/summary,/ai/attribution/session,/shortvideo/seo/suggest-publish-time'
const ATTRIBUTION_UNSUPPORTED_ACTIONS = 'pdf-export,heatmap-export,adopt-template,jump-to-script,local-ai-analysis-fallback,local-attribution-fallback'
const ATTRIBUTION_CONTRACT_DOWNGRADE_MESSAGE = 'PDF 导出、热力图导出、“设为标准模板”和“跳转场次话术”暂无后端落库接口；页面只展示真实归因结果和本地可追溯摘要。'

function hasPersistedAttributionSummary(summary: AttributionSummaryVO | undefined, selectedSessionId: number | '') {
  if (!summary || !selectedSessionId) return false
  const status = String(summary.status ?? '').toLowerCase()
  const hasPersistedIdentity = Number(summary.sessionId) === Number(selectedSessionId)
  const hasMetricSignal = [
    summary.totalGmv,
    summary.totalSales,
    summary.productAttributions,
    summary.scriptAttributions,
    summary.overallScore,
  ].some(value => Number(value ?? 0) > 0)
  return hasPersistedIdentity || hasMetricSignal || (status !== '' && status !== 'unknown')
}

// ===== Tab 1: 场次归因 =====
function SessionAttributionTab() {
  const theme = useTheme()
  const [sessionId, setSessionId] = useState<number | ''>('')
  const [triggerError, setTriggerError] = useState('')
  const [triggerResult, setTriggerResult] = useState('')
  const [triggering, setTriggering] = useState(false)
  const toast = useToast()

  const { data: sessions, isError: sessionsIsError, error: sessionsError, refetch: refetchSessions } = useQuery({
    queryKey: ['attribution-sessions'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
    select: d => normalizeRows<LiveSession>(d),
  })

  const { data: summary, isLoading: summaryLoading, isError: summaryIsError, error: summaryError, refetch: refetchSummary } = useQuery({
    queryKey: ['attribution-summary', sessionId],
    queryFn: () => attributionApi.summary(sessionId as number),
    enabled: !!sessionId,
    placeholderData: undefined as AttributionSummaryVO | undefined,
  })

  const { data: detailRaw, isLoading: detailLoading, isError: detailIsError, error: detailError, refetch: refetchDetail } = useQuery({
    queryKey: ['attribution-detail', sessionId],
    queryFn: () => attributionApi.session(sessionId as number),
    enabled: !!sessionId,
    placeholderData: [],
  })

  const handleTrigger = async () => {
    if (!sessionId) { toast('请先选择场次', 'warning'); return }
    setTriggerError('')
    setTriggerResult('')
    setTriggering(true)
    try {
      const attributionId = await attributionApi.trigger(sessionId as number)
      setTriggerResult(`已向 /ai/attribution/trigger 提交场次 ${sessionId}，任务ID ${attributionId}；结果写入后会从 /summary 和 /session 刷新。`)
      toast('归因分析已触发，分析完成后自动更新', 'success')
    } catch (error) {
      setTriggerError(`/ai/attribution/trigger 触发失败，sessionId=${sessionId}：${getErrorMessage(error)}。当前场次选择已保留，请检查登录态、限流和归因表写入。`)
      toast('触发归因分析失败', 'error')
    } finally {
      setTriggering(false)
    }
  }

  const details: AttributionDetail[] = Array.isArray(detailRaw) ? detailRaw : []
  const summ: AttributionSummaryVO | null = hasPersistedAttributionSummary(summary, sessionId) ? (summary ?? null) : null

  const productDetails = details.filter(d => d.attributionType === 'product_gmv')
  const scriptDetails = details.filter(d => d.attributionType === 'script_sales')

  const aiAnalysis = summ?.aiAnalysis
  const overallScore = summ?.overallScore ?? 0

  const funnelData = productDetails.map(p => ({
    name: p.productName ?? `商品${p.productId}`,
    value: p.contributedGmv ?? 0,
    ratio: p.contributionRatio ?? 0,
  })).sort((a, b) => b.value - a.value)
  const funnelItemColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main

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
        itemStyle: { color: funnelItemColor },
      })),
    }],
  }

  return (
    <Box
      data-testid="attribution-session-tab"
      data-contract-source="/live/session/search|/ai/attribution/trigger|/ai/attribution/summary|/ai/attribution/session"
      data-selected-session-id={sessionId || ''}
      data-summary-state={summ?.status ?? 'none'}
      data-detail-count={details.length}
      data-product-detail-count={productDetails.length}
      data-script-detail-count={scriptDetails.length}
      data-no-local-attribution-fallback="true"
      data-no-local-ai-analysis-fallback="true"
      data-row-retained-on-action-error={triggerError ? 'true' : 'false'}
    >
      {sessionsIsError && (
        <Box data-testid="attribution-session-list-error" data-contract-source="/live/session/search" data-no-local-session-fallback="true">
          <ErrorAlert
            severity="warning"
            title="场次列表加载失败"
            message={getErrorMessage(sessionsError)}
            onRetry={() => void refetchSessions()}
          />
        </Box>
      )}
      <Stack direction="row" spacing={1} alignItems="center" mb={2}>
        <FormControl size="small" sx={{ minWidth: 280 }}>
          <InputLabel id="attribution-session-select-label">选择场次</InputLabel>
          <Select
            labelId="attribution-session-select-label"
            id="attribution-session-select"
            value={sessionId}
            label="选择场次"
            onChange={e => setSessionId(e.target.value as number)}
          >
            {(sessions ?? []).map((s: LiveSession) => (
              <MenuItem key={s.id} value={s.id}>{s.liveTitle} ({s.createTime?.slice(0, 10)})</MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button size="small" variant="contained" color="primary" disabled={triggering}
          onClick={handleTrigger}>{triggering ? '触发中...' : '触发归因分析'}</Button>
        {summ?.status === 'processing' && (
          <Chip label="分析中" color="warning" size="small" icon={<CircularProgress size={12} />} />
        )}
        {summ?.status === 'completed' && (
          <Chip label="已完成" color="success" size="small" />
        )}
      </Stack>
      {summaryIsError && (
        <Box data-testid="attribution-summary-error" data-contract-source="/ai/attribution/summary" data-selected-session-id={sessionId || ''} data-no-local-summary-fallback="true">
          <ErrorAlert
            severity="warning"
            title="归因汇总加载失败"
            message={`/ai/attribution/summary，sessionId=${sessionId}：${getErrorMessage(summaryError)}`}
            onRetry={() => void refetchSummary()}
          />
        </Box>
      )}
      {detailIsError && (
        <Box data-testid="attribution-detail-error" data-contract-source="/ai/attribution/session" data-selected-session-id={sessionId || ''} data-no-local-detail-fallback="true">
          <ErrorAlert
            severity="warning"
            title="归因明细加载失败"
            message={`/ai/attribution/session，sessionId=${sessionId}：${getErrorMessage(detailError)}`}
            onRetry={() => void refetchDetail()}
          />
        </Box>
      )}
      {triggerError && <Alert data-testid="attribution-trigger-error" data-contract-source="/ai/attribution/trigger" data-selected-session-id={sessionId || ''} data-row-retained-on-action-error="true" severity="error" variant="outlined" sx={{ mb: 2 }}>{triggerError}</Alert>}
      {triggerResult && <Alert data-testid="attribution-trigger-result" data-contract-source="/ai/attribution/trigger" severity="success" variant="outlined" sx={{ mb: 2 }}>{triggerResult}</Alert>}

      {!sessionId && <Alert data-testid="attribution-session-empty-select" data-no-local-attribution-fallback="true" severity="info">请选择场次以查看归因数据，或先触发归因分析。</Alert>}
      {sessionId && (summaryLoading || detailLoading) && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {sessionId && !summaryLoading && !detailLoading && !summ && details.length === 0 && (
        <Alert data-testid="attribution-session-empty-no-fallback" data-contract-source="/ai/attribution/summary|/ai/attribution/session" data-selected-session-id={sessionId || ''} data-no-local-attribution-fallback="true" severity="info">该场次暂无归因数据，点击「触发归因分析」按钮开始分析。</Alert>
      )}

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

      {sessionId && !summaryLoading && !detailLoading && (summ || details.length > 0) && <Grid container spacing={2}>
        <Grid item xs={12} md={5}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>商品 GMV 贡献漏斗</Typography>
              <Box
                data-testid="attribution-funnel-chart-surface"
                data-chart-color={funnelItemColor}
              >
                <ReactECharts option={funnelOption} style={{ height: Math.max(200, productDetails.length * 40 + 80) }} />
              </Box>
            </CardContent>
          </Card>

          {scriptDetails.length > 0 && (
            <Card variant="outlined" sx={{ mt: 2 }}>
              <CardContent>
                <Typography variant="subtitle2" fontWeight={600} mb={1}>话术归因</Typography>
                <Stack spacing={1}>
                  {scriptDetails.map((s, idx) => (
                    <Box
                      key={idx}
                      data-testid="session-script-attribution-surface"
                      sx={(theme) => ({
                        p: 1.5,
                        bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                        borderRadius: 1,
                        border: '1px solid',
                        borderColor: 'divider',
                      })}
                    >
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
          {aiAnalysis ? (
            <Paper
              data-testid="attribution-ai-analysis-surface"
              sx={(theme) => ({
                p: 2.5,
                bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.16 : 0.07),
                border: '1px solid',
                borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.38 : 0.2),
                borderRadius: 2,
                mb: 2,
              })}
            >
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <AutoAwesomeIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2" fontWeight={700} color="primary.main">AI 归因分析</Typography>
                {overallScore > 0 && (
                  <Chip label={`评分 ${overallScore}`} size="small" color={overallScore >= 70 ? 'success' : overallScore >= 40 ? 'warning' : 'error'} />
                )}
              </Stack>
              <Typography variant="body2" sx={{ lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>{aiAnalysis}</Typography>
            </Paper>
          ) : (
            <Alert data-testid="attribution-ai-analysis-empty" data-no-local-ai-analysis-fallback="true" severity="info" variant="outlined" sx={{ mb: 2 }}>
              后端本次未返回 <code>aiAnalysis</code>，页面只展示可追溯的归因明细，不再生成本地伪 AI 结论。
            </Alert>
          )}

          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" fontWeight={600} mb={1}>归因明细</Typography>
              <Box sx={{ overflowX: 'auto' }}>
                <Box
                  component="table"
                  sx={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    fontSize: 13,
                    '& th': {
                      borderBottom: '1px solid',
                      borderColor: 'divider',
                      color: 'text.secondary',
                      fontWeight: 600,
                    },
                  }}
                >
                  <thead>
                    <tr>
                      <th style={{ textAlign: 'left', padding: '6px 8px' }}>类型</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px' }}>GMV</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px' }}>销量</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px' }}>占比</th>
                      <th style={{ textAlign: 'right', padding: '6px 8px' }}>评分</th>
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
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>}
    </Box>
  )
}

// ===== Tab 2: 话术归因 =====
function ScriptTab() {
  const theme = useTheme()
  const [sessionId, setSessionId] = useState<number | ''>('')

  const { data: sessions, isError: sessionsIsError, error: sessionsError, refetch: refetchSessions } = useQuery({
    queryKey: ['attribution-sessions-script'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
    select: d => normalizeRows<LiveSession>(d),
  })

  const { data: detailRaw, isLoading, isError, error, refetch } = useQuery({
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
  const scriptBarColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main

  const barOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', axisLabel: { formatter: (v: number) => `¥${(v/1000).toFixed(0)}k` } },
    yAxis: { type: 'category', data: rows.map(r => r.segmentName ?? `段落${r.segmentIndex}`), inverse: true },
    series: [{
      type: 'bar',
      data: rows.map(r => r.gmvContribution ?? 0),
      itemStyle: { color: scriptBarColor },
      label: { show: true, position: 'right', formatter: (p: { value: number }) => `¥${(p.value/1000).toFixed(1)}k` },
    }],
  }

  const localConclusions = rows.length >= 2 ? [
    `最优话术「${rows[0]?.segmentName}」GMV 贡献 ¥${(rows[0]?.gmvContribution ?? 0).toLocaleString()}，效果评分最高，建议设为团队标准模板。`,
    `对比最差段落，最优段落转化率高出较多，建议重点优化低效段落的话术结构。`,
    `建议将「${rows[0]?.segmentName}」话术结构设为团队标准模板，并在下次直播优先安排在高流量时段使用。`,
  ] : rows.length === 1 ? [
    `该场次共 1 条已执行话术，GMV 贡献 ¥${(rows[0]?.gmvContribution ?? 0).toLocaleString()}，建议补充更多话术类型以提升转化。`,
  ] : []

  return (
    <Box
      data-testid="attribution-script-tab"
      data-contract-source="/live/session/search|/ai/attribution/session"
      data-selected-session-id={sessionId || ''}
      data-detail-count={details.length}
      data-script-row-count={rows.length}
      data-local-summary-state={rows.length > 0 ? 'derived-from-session-details' : 'empty'}
      data-no-local-script-fallback="true"
      data-no-template-writeback="true"
    >
      {sessionsIsError && (
        <Box data-testid="script-attribution-session-list-error" data-contract-source="/live/session/search" data-no-local-session-fallback="true">
          <ErrorAlert severity="warning" title="场次列表加载失败" message={getErrorMessage(sessionsError)} onRetry={() => void refetchSessions()} />
        </Box>
      )}
      <Box sx={{ mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 280 }}>
          <InputLabel id="attribution-script-session-select-label">选择场次</InputLabel>
          <Select
            labelId="attribution-script-session-select-label"
            id="attribution-script-session-select"
            value={sessionId}
            label="选择场次"
            onChange={e => setSessionId(e.target.value as number)}
          >
            {(sessions ?? []).map((s: LiveSession) => (
              <MenuItem key={s.id} value={s.id}>{s.liveTitle} ({s.createTime?.slice(0, 10)})</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {!sessionId && <Alert data-testid="script-attribution-empty-select" data-no-local-script-fallback="true" severity="info">请选择场次以查看话术归因数据。</Alert>}
      {sessionId && isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {sessionId && isError && (
        <Box data-testid="script-attribution-detail-error" data-contract-source="/ai/attribution/session" data-selected-session-id={sessionId || ''} data-no-local-script-fallback="true">
          <ErrorAlert severity="warning" title="话术归因加载失败" message={`/ai/attribution/session，sessionId=${sessionId}：${getErrorMessage(error)}`} onRetry={() => void refetch()} />
        </Box>
      )}
      {sessionId && !isLoading && rows.length === 0 && <Alert data-testid="script-attribution-empty-no-fallback" data-contract-source="/ai/attribution/session" data-selected-session-id={sessionId || ''} data-no-local-script-fallback="true" severity="info">该场次暂无话术归因数据。请先在「场次归因」页触发归因分析。</Alert>}

      {rows.length > 0 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={7}>
            <Stack spacing={2}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" fontWeight={600} mb={1}>各话术 GMV 贡献</Typography>
                  <Box
                    data-testid="script-attribution-bar-chart-surface"
                    data-chart-color={scriptBarColor}
                  >
                    <ReactECharts option={barOption} style={{ height: Math.max(200, rows.length * 36) }} />
                  </Box>
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
                        <Box
                          key={r.segmentIndex}
                          data-testid={isTop ? 'script-attribution-top-surface' : 'script-attribution-row-surface'}
                          sx={(theme) => ({
                            p: 1.5,
                            bgcolor: isTop
                              ? alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
                              : theme.palette.mode === 'dark'
                                ? alpha(theme.palette.common.white, 0.04)
                                : alpha(theme.palette.common.black, 0.025),
                            borderRadius: 1,
                            border: '1px solid',
                            borderColor: isTop ? alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.45 : 0.25) : 'divider',
                          })}
                        >
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
            <Paper
              data-testid="script-local-summary-surface"
              data-contract-status="local-only"
              data-contract-action="adopt-template"
              data-contract-endpoint="unavailable"
              data-no-template-writeback="true"
              sx={(theme) => ({
                p: 2.5,
                bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.16 : 0.07),
                border: '1px solid',
                borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.38 : 0.2),
                borderRadius: 2,
                height: '100%',
              })}
            >
              <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                <AutoAwesomeIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2" fontWeight={700} color="primary.main">本地对比摘要</Typography>
              </Stack>
              <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
                当前后端未提供独立话术 AI 分析接口；以下摘要由归因明细本地计算生成，不写入标准模板。
              </Alert>
              {localConclusions.length === 0 ? (
                <Skeleton variant="text" width="90%" />
              ) : (
                <Stack spacing={2}>
                  {localConclusions.map((c, i) => (
                    <Box key={i}>
                      <Stack direction="row" spacing={1} alignItems="flex-start">
                        <Box sx={{ width: 22, height: 22, borderRadius: '50%', bgcolor: 'primary.main', color: 'primary.contrastText', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, flexShrink: 0, mt: 0.2 }}>
                          {i + 1}
                        </Box>
                        <Typography variant="body2" sx={{ lineHeight: 1.7 }}>{c}</Typography>
                      </Stack>
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

  const { data: publishRaw, isLoading: publishLoading, isError: publishIsError, error: publishError, refetch: refetchPublish } = useQuery({
    queryKey: ['recommend-publish-time'],
    queryFn: () => recommendPublishTime({}),
    placeholderData: [],
  })

  const recommendLabels = normalizeRows<string>(publishRaw)

  return (
    <Box
      data-testid="attribution-time-tab"
      data-contract-source="/shortvideo/seo/suggest-publish-time"
      data-metric={metric}
      data-recommend-count={recommendLabels.length}
      data-no-local-heatmap-fallback="true"
      data-no-heatmap-export="true"
    >
      <Stack direction="row" spacing={1} mb={2} alignItems="center" justifyContent="space-between">
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="body2" color="text.secondary">指标：</Typography>
          <ToggleButtonGroup size="small" exclusive value={metric} onChange={(_e, v) => v && setMetric(v)}>
            <ToggleButton value="gmv">GMV</ToggleButton>
            <ToggleButton value="orders">订单量</ToggleButton>
            <ToggleButton value="conversionRate">转化率</ToggleButton>
          </ToggleButtonGroup>
        </Stack>
        <Button
          size="small"
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          disabled
          data-testid="attribution-heatmap-export-action"
          data-contract-status="unsupported"
          data-contract-action="heatmap-export"
          data-contract-endpoint="unavailable"
        >
          导出热力图
        </Button>
      </Stack>

      <Alert data-testid="attribution-time-downgrade" data-contract-source="/shortvideo/seo/suggest-publish-time" data-no-local-heatmap-fallback="true" severity="info" sx={{ mb: 2 }}>
        时段热力图和导出需要接入直播小时级统计 API，当前只展示短视频推荐发布时间；热力图导出按钮保持禁用。
      </Alert>
      {publishIsError && (
        <Box data-testid="attribution-publish-time-error" data-contract-source="/shortvideo/seo/suggest-publish-time" data-no-local-publish-time-fallback="true">
          <ErrorAlert
            severity="warning"
            title="推荐发布时段加载失败"
            message={getErrorMessage(publishError)}
            onRetry={() => void refetchPublish()}
          />
        </Box>
      )}

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
        <Alert data-testid="attribution-publish-time-empty" data-no-local-publish-time-fallback="true" severity="info">暂无推荐发布时段，请配置短视频 SEO 分析功能。</Alert>
      )}
    </Box>
  )
}

// ===== Tab 4: 场次对比 =====
function SessionCompareTab() {
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const toast = useToast()

  const { data: sessions, isError: sessionsIsError, error: sessionsError, refetch: refetchSessions } = useQuery({
    queryKey: ['attribution-sessions-compare'],
    queryFn: () => liveApi.sessionSearch({ rows: 100 }),
    select: d => normalizeRows<LiveSession>(d),
  })

  const { data: analysisResults, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['session-analyses', selectedIds],
    queryFn: async () => {
      if (selectedIds.length === 0) return []
      const settled = await Promise.allSettled(selectedIds.map(id => attributionApi.summary(id)))
      const failed = settled.find(result => result.status === 'rejected')
      if (failed?.status === 'rejected') {
        throw new Error(`selectedSessionIds=${selectedIds.join(',')}，${getErrorMessage(failed.reason)}`)
      }
      return settled
        .filter((result): result is PromiseFulfilledResult<AttributionSummaryVO> => result.status === 'fulfilled')
        .map(result => result.value)
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

  const localDiff = enrichedResults.length >= 2 ? (() => {
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
    <Box
      data-testid="attribution-compare-tab"
      data-contract-source="/live/session/search|/ai/attribution/summary"
      data-selected-session-ids={selectedIds.join(',')}
      data-result-count={enrichedResults.length}
      data-no-local-compare-fallback="true"
      data-no-template-writeback="true"
    >
      {sessionsIsError && (
        <Box data-testid="compare-session-list-error" data-contract-source="/live/session/search" data-no-local-session-fallback="true">
          <ErrorAlert severity="warning" title="场次列表加载失败" message={getErrorMessage(sessionsError)} onRetry={() => void refetchSessions()} />
        </Box>
      )}
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

      {selectedIds.length === 0 && <Alert data-testid="compare-empty-select" data-no-local-compare-fallback="true" severity="info">请选择至少1个场次查看数据，选择2个及以上可进行对比。</Alert>}
      {isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {isError && (
        <Box data-testid="compare-summary-error" data-contract-source="/ai/attribution/summary" data-selected-session-ids={selectedIds.join(',')} data-row-retained-on-action-error="true" data-no-local-compare-fallback="true">
          <ErrorAlert severity="warning" title="场次对比加载失败" message={`/ai/attribution/summary 批量加载失败：${getErrorMessage(error)}。已选场次仍保留，可减少选择后重试。`} onRetry={() => void refetch()} />
        </Box>
      )}

      {enrichedResults.length > 0 && (
        <Stack spacing={2}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" fontWeight={600} mb={1}>场次指标对比</Typography>
                  <Box sx={{ overflowX: 'auto' }}>
                    <Box
                      component="table"
                      sx={{
                        width: '100%',
                        borderCollapse: 'collapse',
                        '& th': {
                          borderBottom: '1px solid',
                          borderColor: 'divider',
                          color: 'text.secondary',
                          fontWeight: 600,
                        },
                      }}
                    >
                      <thead>
                        <tr>
                          <th style={{ textAlign: 'left', padding: '6px 8px', fontSize: 13 }}>指标</th>
                          {enrichedResults.map(r => (
                            <th key={r.sessionId} style={{ textAlign: 'right', padding: '6px 8px', fontSize: 13 }}>
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
                              <Box component="td" sx={{ p: '6px 8px', fontSize: 13, color: 'text.secondary' }}>{m.label}</Box>
                              {enrichedResults.map(r => {
                                const val = Number(r[m.key] ?? 0)
                                const isMax = val === maxVal && maxVal > 0
                                const display = typeof val === 'number' && val < 100 ? val.toLocaleString() : val.toString()
                                return (
                                  <Box
                                    component="td"
                                    key={r.sessionId}
                                    data-testid={isMax ? 'compare-best-metric-cell' : undefined}
                                    sx={(theme) => ({
                                      textAlign: 'right',
                                      p: '6px 8px',
                                      fontSize: 13,
                                      color: isMax ? 'success.main' : undefined,
                                      fontWeight: isMax ? 700 : undefined,
                                      bgcolor: isMax ? alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.18 : 0.08) : undefined,
                                    })}
                                  >
                                    {isMax ? '最佳 ' : ''}{display}
                                  </Box>
                                )
                              })}
                            </tr>
                          )
                        })}
                      </tbody>
                    </Box>
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

          {localDiff.length > 0 && (
            <Paper
              data-testid="compare-local-diff-surface"
              data-contract-status="local-only"
              data-contract-actions="adopt-template,jump-to-script"
              data-contract-endpoint="unavailable"
              data-no-template-writeback="true"
              sx={(theme) => ({
                p: 2.5,
                bgcolor: theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : alpha(theme.palette.common.black, 0.025),
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 2,
              })}
            >
              <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
                <AutoAwesomeIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2" fontWeight={700}>本地差异摘要</Typography>
              </Stack>
              <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
                当前没有“设为标准模板”或“跳转场次话术”的归因落库接口；摘要仅来自已选场次汇总字段。
              </Alert>
              <Stack spacing={1.5}>
                {localDiff.map((c, i) => (
                  <Stack key={i} direction="row" spacing={1} alignItems="flex-start">
                    <Box sx={{ width: 22, height: 22, borderRadius: '50%', bgcolor: 'primary.main', color: 'primary.contrastText', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, flexShrink: 0, mt: 0.2 }}>
                      {i + 1}
                    </Box>
                    <Typography variant="body2" sx={{ lineHeight: 1.7 }}>{c}</Typography>
                  </Stack>
                ))}
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

  return (
    <Box
      sx={{ p: 3 }}
      data-testid="attribution-workbench"
      data-contract-scope="ai-attribution-workbench"
      data-ready-endpoints={ATTRIBUTION_READY_ENDPOINTS}
      data-unsupported-actions={ATTRIBUTION_UNSUPPORTED_ACTIONS}
      data-active-tab={tab}
      data-no-local-attribution-fallback="true"
      data-no-local-ai-analysis-fallback="true"
      data-no-template-writeback="true"
      data-no-unsupported-export="true"
    >
      <PageHeader
        title="归因分析"
        subtitle="对齐 /ai/attribution/trigger、/summary、/session 真实接口；未落库的 PDF 导出、标准模板采纳和跳转动作保持显式降级。"
        breadcrumbs={[{ label: 'AI' }, { label: '归因分析' }]}
        actions={
          <Button
            size="small"
            variant="outlined"
            startIcon={<FileDownloadIcon />}
            disabled
            data-testid="attribution-pdf-export-action"
            data-contract-status="unsupported"
            data-contract-action="pdf-export"
            data-contract-endpoint="unavailable"
          >
            导出报告 PDF
          </Button>
        }
      />
      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="attribution-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-scope="ai-attribution"
        data-ready-endpoints={ATTRIBUTION_READY_ENDPOINTS}
        data-unsupported-actions={ATTRIBUTION_UNSUPPORTED_ACTIONS}
        data-no-local-attribution-fallback="true"
        data-no-template-writeback="true"
      >
        {ATTRIBUTION_CONTRACT_DOWNGRADE_MESSAGE}
      </Alert>

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
