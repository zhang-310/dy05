import { useMemo, useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Grid, MenuItem, Stack, TextField, Typography } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { orgApi } from '@/api/org'
import type { LiveSession } from '@/api/live'
import { getErrorMessage } from '@/utils/errorHandler'

const ORG_ANALYTICS_READY_ENDPOINTS = ['/dashboard/org/stats', '/live/session/search'] as const
const ORG_ANALYTICS_UNSUPPORTED_ENDPOINTS = ['/org/analytics/summary', '/org/analytics/trend'] as const
const ORG_ANALYTICS_READY_ENDPOINTS_ATTR = ORG_ANALYTICS_READY_ENDPOINTS.join(',')
const ORG_ANALYTICS_UNSUPPORTED_ENDPOINTS_ATTR = ORG_ANALYTICS_UNSUPPORTED_ENDPOINTS.join(',')
const ORG_ANALYTICS_DOWNGRADE_MESSAGE = '独立组织分析汇总和趋势接口尚未落库，当前页面只组合真实 dashboard 与 live session 数据源。'

function money(value: number) {
  if (value >= 10000) return `¥${(value / 10000).toFixed(2)}万`
  return `¥${value.toLocaleString()}`
}

function sessionGmv(row: LiveSession) {
  return Number(row.totalGmv ?? row.cumulativeGmv ?? row.totalRevenue ?? 0)
}

export default function OrgAnalyticsPage() {
  const [days, setDays] = useState(30)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['org-analytics-snapshot', days],
    queryFn: () => orgApi.analyticsSnapshot(days),
  })

  const sessions = Array.isArray(data?.sessions) ? data.sessions : []
  const partialErrors = data?.errors
  const endedSessions = sessions.filter(s => s.status === 2)
  const liveSessions = sessions.filter(s => s.status === 1)
  const totalGmv = sessions.reduce((sum, row) => sum + sessionGmv(row), 0)
  const avgGmv = sessions.length > 0 ? totalGmv / sessions.length : 0
  const totalViewers = sessions.reduce((sum, row) => sum + Number(row.viewers ?? 0), 0)

  const formatRows = useMemo(() => {
    const map = new Map<string, { format: string; sessions: number; gmv: number }>()
    sessions.forEach(row => {
      const key = row.liveFormat || '未设置'
      const current = map.get(key) ?? { format: key, sessions: 0, gmv: 0 }
      current.sessions += 1
      current.gmv += sessionGmv(row)
      map.set(key, current)
    })
    return [...map.values()]
  }, [sessions])

  const trendRows = useMemo(() => {
    const map = new Map<string, { date: string; sessions: number; gmv: number; viewers: number }>()
    sessions.forEach(row => {
      const date = String(row.scheduledTime || row.createTime || '').slice(0, 10) || '未排期'
      const current = map.get(date) ?? { date, sessions: 0, gmv: 0, viewers: 0 }
      current.sessions += 1
      current.gmv += sessionGmv(row)
      current.viewers += Number(row.viewers ?? 0)
      map.set(date, current)
    })
    return [...map.values()].sort((a, b) => a.date.localeCompare(b.date))
  }, [sessions])

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['GMV', '场次数', '观看人数'], top: 0 },
    xAxis: { type: 'category', data: trendRows.map(r => r.date), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: [
      { type: 'value', name: 'GMV(元)' },
      { type: 'value', name: '场次/观看', splitLine: { show: false } },
    ],
    series: [
      { name: 'GMV', type: 'line', smooth: true, yAxisIndex: 0, data: trendRows.map(r => r.gmv) },
      { name: '场次数', type: 'bar', yAxisIndex: 1, data: trendRows.map(r => r.sessions) },
      { name: '观看人数', type: 'line', smooth: true, yAxisIndex: 1, data: trendRows.map(r => r.viewers) },
    ],
    grid: { left: 64, right: 64, bottom: 56, top: 44 },
  }

  const kpiCards = [
    { label: '总 GMV', value: money(totalGmv), hint: '场次 totalGmv/cumulativeGmv 聚合', contractStatus: 'local-derived', sourceEndpoint: '/live/session/search' },
    { label: '场次数', value: sessions.length, hint: `直播中 ${liveSessions.length} / 已结束 ${endedSessions.length}`, contractStatus: 'local-derived', sourceEndpoint: '/live/session/search' },
    { label: '平均 GMV', value: money(avgGmv), hint: '按当前返回场次计算', contractStatus: 'local-derived', sourceEndpoint: '/live/session/search' },
    { label: '观看人数', value: totalViewers.toLocaleString(), hint: '来自 live session viewers', contractStatus: 'local-derived', sourceEndpoint: '/live/session/search' },
    { label: '用户数', value: data?.stats.totalUsers ?? '--', hint: '来自 dashboard/org/stats', contractStatus: 'server-source', sourceEndpoint: '/dashboard/org/stats' },
    { label: '今日 AI 调用', value: data?.stats.todayAiCalls ?? '--', hint: '机构 Dashboard 指标', contractStatus: 'server-source', sourceEndpoint: '/dashboard/org/stats' },
  ]

  return (
    <Box
      data-testid="org-analytics-workbench"
      data-contract-scope="org-analytics"
      data-ready-endpoints={ORG_ANALYTICS_READY_ENDPOINTS_ATTR}
      data-unsupported-endpoints={ORG_ANALYTICS_UNSUPPORTED_ENDPOINTS_ATTR}
      data-lookback-days={days}
      data-session-count={sessions.length}
      data-trend-row-count={trendRows.length}
      data-no-mock-sessions="true"
      data-no-fake-stats="true"
      data-no-unsupported-analytics-endpoints="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="机构数据分析"
        subtitle="当前以后端真实 `/dashboard/org/stats` 和 `/live/session/search` 聚合展示；独立 `/org/analytics/summary|trend` 尚未落库。"
        breadcrumbs={[{ label: '机构' }, { label: '数据分析' }]}
        actions={
          <Stack direction="row" spacing={1}>
            <TextField select size="small" label="分析窗口" value={days} onChange={e => setDays(Number(e.target.value))} sx={{ width: 130 }}>
              {[7, 14, 30, 90].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
            </TextField>
            <Button startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>
          </Stack>
        }
      />

      {isError && (
        <Alert
          severity="error"
          data-testid="org-analytics-snapshot-error"
          data-source-endpoints={ORG_ANALYTICS_READY_ENDPOINTS_ATTR}
          data-no-mock-sessions="true"
          data-no-fake-stats="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          机构分析加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      {partialErrors?.stats && (
        <Alert
          severity="error"
          data-testid="org-analytics-stats-source-error"
          data-source-endpoint="/dashboard/org/stats"
          data-no-fake-zero="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          机构统计加载失败：{partialErrors.stats}。请检查 `/dashboard/org/stats`；直播场次聚合不会用 0 值冒充统计成功。
        </Alert>
      )}
      {partialErrors?.sessions && (
        <Alert
          severity="error"
          data-testid="org-analytics-sessions-source-error"
          data-source-endpoint="/live/session/search"
          data-no-mock-sessions="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          直播场次加载失败：{partialErrors.sessions}。请检查 `/live/session/search`；趋势和形式分布不会注入模拟场次。
        </Alert>
      )}
      <Alert
        severity="info"
        data-testid="org-analytics-contract-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="degraded"
        data-source-endpoints={ORG_ANALYTICS_READY_ENDPOINTS_ATTR}
        data-unsupported-endpoints={ORG_ANALYTICS_UNSUPPORTED_ENDPOINTS_ATTR}
      >
        {ORG_ANALYTICS_DOWNGRADE_MESSAGE} 分析窗口当前用于页面解释和后续后端查询规划；直播场次搜索接口尚未支持组织端按日期聚合趋势，页面基于返回场次的排期日期做前端聚合。
      </Alert>

      <Grid container spacing={1.5}>
        {kpiCards.map(card => (
          <Grid item xs={12} sm={6} md={4} key={card.label}>
            <Card
              variant="outlined"
              data-testid="org-analytics-kpi-card"
              data-contract-status={card.contractStatus}
              data-source-endpoint={card.sourceEndpoint}
              data-kpi-label={card.label}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{card.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{isFetching ? '加载中...' : card.value}</Typography>
                <Typography variant="caption" color="text.secondary">{card.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} lg={8}>
            <Card
              variant="outlined"
              data-testid="org-analytics-trend-surface"
              data-contract-status="local-derived"
              data-source-endpoint="/live/session/search"
              data-unsupported-endpoint="/org/analytics/trend"
              data-trend-row-count={trendRows.length}
              data-no-mock-trend-rows="true"
            >
            <CardContent>
              <Typography variant="subtitle2" mb={1}>场次趋势聚合</Typography>
              {trendRows.length > 0
                ? <ReactECharts option={option} style={{ height: 360 }} />
                : <Alert severity="info">暂无可聚合的场次数据</Alert>
              }
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Card
            variant="outlined"
            data-testid="org-analytics-format-surface"
            data-contract-status="local-derived"
            data-source-endpoint="/live/session/search"
            data-unsupported-endpoint="/org/analytics/summary"
            data-format-row-count={formatRows.length}
            data-no-mock-format-rows="true"
          >
            <CardContent>
              <Typography variant="subtitle2" mb={1}>直播形式分布</Typography>
              <Stack spacing={1}>
                {formatRows.length > 0 ? formatRows.map(row => (
                  <Box key={row.format} sx={{ p: 1.5, border: 1, borderColor: 'divider', borderRadius: 1 }}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2" fontWeight={600}>{row.format}</Typography>
                      <Typography variant="body2">{row.sessions} 场</Typography>
                    </Stack>
                    <Typography variant="caption" color="text.secondary">GMV {money(row.gmv)}</Typography>
                  </Box>
                )) : <Alert severity="info">暂无直播形式数据</Alert>}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
