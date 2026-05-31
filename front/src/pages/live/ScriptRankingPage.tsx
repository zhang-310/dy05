import { useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Chip, Grid, LinearProgress, Skeleton, Stack, TextField, Typography } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { liveApi } from '@/api/live'
import { PageHeader } from '@/components/base/PageHeader'
import { getErrorMessage } from '@/utils/errorHandler'
import { alpha, useTheme } from '@mui/material/styles'
import { normalizeArray } from '@/utils/response-normalize'

type RankingRow = Record<string, unknown>

const SCRIPT_RANKING_READY_ENDPOINTS = {
  ranking: '/live/effectiveness/ranking',
  topScripts: '/live/effectiveness/top-scripts',
} as const

const SCRIPT_RANKING_CONTEXT_ENDPOINTS = [
  SCRIPT_RANKING_READY_ENDPOINTS.ranking,
  SCRIPT_RANKING_READY_ENDPOINTS.topScripts,
]

const SCRIPT_RANKING_UNSUPPORTED_ACTIONS = [
  'days-local-aggregate',
  'script-type-local-aggregate',
  'ai-script-generate',
  'script-save',
  'script-export',
  'shortvideo-export',
]

function numberValue(value: unknown, fallback = 0): number {
  return typeof value === 'number' ? value : Number(value ?? fallback) || fallback
}

function percentLabel(value: unknown) {
  const n = numberValue(value)
  return n <= 1 && n > 0 ? `${(n * 100).toFixed(1)}%` : `${n.toFixed(1)}%`
}

function scoreOf(row: RankingRow) {
  return numberValue(row.totalScore ?? row.score ?? row.effectivenessScore)
}

export default function ScriptRankingPage() {
  const theme = useTheme()
  const [draftSessionId, setDraftSessionId] = useState('')
  const [sessionId, setSessionId] = useState(0)

  const rankingQuery = useQuery({
    queryKey: ['effectiveness-ranking', sessionId],
    queryFn: () => liveApi.effectivenessRanking({ sessionId, page: 0, pageSize: 30 }),
    enabled: sessionId > 0,
  })
  const topScriptsQuery = useQuery({
    queryKey: ['top-scripts', sessionId],
    queryFn: () => liveApi.effectivenessTopScripts({ sessionId, limit: 10 }),
    enabled: sessionId > 0,
  })

  const rankList = normalizeArray<RankingRow>(rankingQuery.data)
  const topList = normalizeArray<RankingRow>(topScriptsQuery.data)
  const isLoading = rankingQuery.isLoading || topScriptsQuery.isLoading

  const handleLoad = () => {
    const id = Number(draftSessionId)
    setSessionId(id > 0 ? id : 0)
  }

  const refresh = () => {
    rankingQuery.refetch()
    topScriptsQuery.refetch()
  }

  const rankingContext = `上下文：route=/admin/live/ranking; sessionId=${sessionId}; page=0; pageSize=30`
  const topScriptsContext = `上下文：route=/admin/live/ranking; sessionId=${sessionId}; limit=10`
  const rankingBarColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main
  const rankBadgeColor = (idx: number) => idx < 3
    ? (theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.main)
    : rankingBarColor

  const barOption = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: rankList.map((r) => `#${String(r.ranking ?? r.scriptId ?? '-')}`),
      axisLabel: { rotate: 30, fontSize: 11 },
    },
    yAxis: { type: 'value', name: '总分' },
    series: [{
      type: 'bar',
      data: rankList.map(scoreOf),
      itemStyle: { color: rankingBarColor },
    }],
    grid: { left: 44, right: 20, bottom: 58, top: 24 },
  }

  return (
    <Box
      sx={{ py: 1 }}
      data-testid="live-script-ranking-workbench"
      data-contract-scope="live-script-effectiveness-ranking"
      data-ready-endpoints={Object.values(SCRIPT_RANKING_READY_ENDPOINTS).join('|')}
      data-context-endpoints={SCRIPT_RANKING_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SCRIPT_RANKING_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={sessionId}
      data-ranking-count={rankList.length}
      data-top-count={topList.length}
    >
      <PageHeader
        title="直播话术效果排行"
        subtitle="按场次读取 `/live/effectiveness/ranking` 与 `/top-scripts`，后端当前不支持按天或话术类型聚合。"
        breadcrumbs={[{ label: '直播' }, { label: '话术效果排行' }]}
        actions={sessionId > 0 ? (
          <Button startIcon={<RefreshIcon />} variant="outlined" onClick={refresh} disabled={isLoading}>
            刷新
          </Button>
        ) : undefined}
      />

      <Stack spacing={2}>
        <Alert
          severity="info"
          data-testid="live-script-ranking-contract-alert"
          data-contract-source={SCRIPT_RANKING_CONTEXT_ENDPOINTS.join('|')}
          data-no-days-local-aggregate="true"
          data-no-script-type-local-aggregate="true"
          data-no-ai-generate="true"
          data-no-shortvideo-export="true"
        >
          该页面使用真实效果评分表，必须输入直播场次 ID。若无数据，通常表示该场次尚未计算话术评分或样本量不足。
        </Alert>

        <Card
          variant="outlined"
          data-testid="live-script-ranking-filter-card"
          data-contract-source={SCRIPT_RANKING_CONTEXT_ENDPOINTS.join('|')}
          data-only-session-filter="true"
        >
          <CardContent>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'stretch', sm: 'center' }}>
              <TextField
                size="small"
                label="直播场次 ID"
                value={draftSessionId}
                onChange={(e) => setDraftSessionId(e.target.value)}
                sx={{ width: { xs: '100%', sm: 180 } }}
                inputProps={{ inputMode: 'numeric' }}
              />
              <Button variant="contained" onClick={handleLoad} disabled={Number(draftSessionId) <= 0}>
                加载排行
              </Button>
              {sessionId > 0 && <Chip label={`当前场次 #${sessionId}`} color="primary" variant="outlined" />}
            </Stack>
          </CardContent>
        </Card>

        {(rankingQuery.isError || topScriptsQuery.isError) && (
          <Stack spacing={1}>
            {rankingQuery.isError && (
              <Alert
                severity="error"
                data-testid="live-script-ranking-error"
                data-contract-source={SCRIPT_RANKING_READY_ENDPOINTS.ranking}
                data-no-static-ranking-fallback="true"
                action={<Button color="inherit" size="small" onClick={() => rankingQuery.refetch()}>重试</Button>}
              >
                {SCRIPT_RANKING_READY_ENDPOINTS.ranking} 排行榜加载失败：{getErrorMessage(rankingQuery.error)}。{rankingContext}
              </Alert>
            )}
            {topScriptsQuery.isError && (
              <Alert
                severity="error"
                data-testid="live-script-top-scripts-error"
                data-contract-source={SCRIPT_RANKING_READY_ENDPOINTS.topScripts}
                data-no-static-top-scripts-fallback="true"
                action={<Button color="inherit" size="small" onClick={() => topScriptsQuery.refetch()}>重试</Button>}
              >
                {SCRIPT_RANKING_READY_ENDPOINTS.topScripts} Top 话术加载失败：{getErrorMessage(topScriptsQuery.error)}。{topScriptsContext}
              </Alert>
            )}
          </Stack>
        )}

        {isLoading && <LinearProgress sx={{ borderRadius: 1 }} />}

        {isLoading && (
          <Card variant="outlined">
            <CardContent>
              <Skeleton variant="text" width={160} height={24} sx={{ mb: 1 }} />
              <Skeleton variant="rectangular" height={300} sx={{ borderRadius: 1 }} />
            </CardContent>
          </Card>
        )}

        {!isLoading && sessionId > 0 && rankList.length > 0 && (
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" mb={1}>场次内效果评分排行</Typography>
              <Box
                data-testid="live-script-ranking-chart-surface"
                data-chart-color={rankingBarColor}
              >
                <ReactECharts option={barOption} style={{ height: 300 }} />
              </Box>
            </CardContent>
          </Card>
        )}

        <Typography variant="subtitle2">Top 话术列表</Typography>

        {!isLoading && sessionId === 0 && (
          <Alert
            severity="warning"
            data-testid="live-script-ranking-empty-session"
            data-no-days-local-aggregate="true"
          >
            请输入直播场次 ID 后加载，页面不会再用“近 7 天”等本地过滤伪造排行。
          </Alert>
        )}

        {!isLoading && sessionId > 0 && (
          <Grid container spacing={2}>
            {topList.map((r, idx) => {
              const scoreNum = scoreOf(r)
              return (
                <Grid item xs={12} sm={6} md={4} key={String(r.scriptId ?? r.id ?? idx)}>
                  <Card variant="outlined" sx={{ height: '100%', '&:hover': { boxShadow: 2 } }}>
                    <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                      <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                        <Typography
                          variant="caption"
                          data-testid="live-script-ranking-badge-surface"
                          data-rank-tone={idx < 3 ? 'warning' : 'primary'}
                          data-rank-color={rankBadgeColor(idx)}
                          sx={{
                            bgcolor: alpha(rankBadgeColor(idx), theme.palette.mode === 'dark' ? 0.22 : 0.12),
                            color: rankBadgeColor(idx),
                            border: '1px solid',
                            borderColor: alpha(rankBadgeColor(idx), theme.palette.mode === 'dark' ? 0.55 : 0.32),
                            borderRadius: '50%',
                            width: 22,
                            height: 22,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: 11,
                            flexShrink: 0,
                            fontWeight: 700,
                          }}
                        >
                          {idx + 1}
                        </Typography>
                        <Typography variant="body2" fontWeight={600} sx={{ flex: 1 }}>
                          话术 #{String(r.scriptId ?? r.id ?? '-')}
                        </Typography>
                        {r.tag != null && <Chip label={String(r.tag)} size="small" variant="outlined" sx={{ height: 22 }} />}
                      </Stack>
                      <Stack spacing={0.5}>
                        <Stack direction="row" justifyContent="space-between">
                          <Typography variant="caption" color="text.secondary">总分</Typography>
                          <Typography variant="caption" fontWeight={700}>{scoreNum.toFixed(1)}</Typography>
                        </Stack>
                        <LinearProgress variant="determinate" value={Math.min(scoreNum, 100)} sx={{ height: 6, borderRadius: 3 }} />
                        <Stack direction="row" flexWrap="wrap" gap={1} sx={{ pt: 0.5 }}>
                          <Chip size="small" label={`转化 ${percentLabel(r.conversionRate)}`} />
                          <Chip size="small" label={`完播 ${percentLabel(r.completionRate)}`} />
                          <Chip size="small" label={`点赞 ${numberValue(r.likes).toLocaleString()}`} />
                          <Chip size="small" label={`评论 ${numberValue(r.comments).toLocaleString()}`} />
                          <Chip size="small" label={`样本 ${numberValue(r.sampleSize).toLocaleString()}`} />
                        </Stack>
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              )
            })}
            {topList.length === 0 && (
              <Grid item xs={12}>
                <Alert
                  severity="info"
                  data-testid="live-script-top-scripts-empty"
                  data-contract-source={SCRIPT_RANKING_READY_ENDPOINTS.topScripts}
                  data-no-static-top-scripts-fallback="true"
                >
                  该场次暂无 Top 话术。请先触发效果评分计算，或确认 `sampleSize` 是否满足评分阈值。
                </Alert>
              </Grid>
            )}
          </Grid>
        )}
      </Stack>
    </Box>
  )
}
