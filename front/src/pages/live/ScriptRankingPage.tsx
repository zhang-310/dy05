import { useState } from 'react'
import { Box, Stack, Card, CardContent, Typography, TextField, MenuItem, Grid, LinearProgress, Skeleton, Chip } from '@mui/material'
import ReactECharts from 'echarts-for-react'
import { liveApi } from '@/api/live'
import { useQuery } from '@tanstack/react-query'

const SCRIPT_TYPE_LABELS: Record<string, string> = {
  opening: '开场话术',
  product: '商品介绍',
  transition: '过渡话术',
  closing: '结尾话术',
}

export default function ScriptRankingPage() {
  const [days, setDays] = useState(7)
  const [scriptType, setScriptType] = useState('')

  const { data: ranking, isLoading } = useQuery({
    queryKey: ['effectiveness-ranking', days, scriptType],
    queryFn: () => liveApi.effectivenessRanking({ days, scriptType: scriptType || undefined }),
  })
  const { data: topScripts } = useQuery({
    queryKey: ['top-scripts', days],
    queryFn: () => liveApi.effectivenessTopScripts({ days, limit: 10 }),
  })

  const rankList = ranking?.list ?? []
  const topList = topScripts ?? []

  const barOption = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: rankList.map((r) => String(r.scriptTitle ?? r.title ?? r.id ?? '')),
      axisLabel: { rotate: 30, fontSize: 11 },
    },
    yAxis: { type: 'value', name: '效果评分' },
    series: [{
      type: 'bar',
      data: rankList.map((r) => r.score ?? r.effectivenessScore ?? 0),
      itemStyle: { color: '#1976d2' },
    }],
    grid: { left: 40, right: 20, bottom: 60, top: 20 },
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, py: 1 }}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField select size="small" label="时间范围" value={days} onChange={e => setDays(Number(e.target.value))} sx={{ width: 120 }}>
          {[7, 14, 30].map(d => <MenuItem key={d} value={d}>近 {d} 天</MenuItem>)}
        </TextField>
        <TextField select size="small" label="话术类型" value={scriptType} onChange={e => setScriptType(e.target.value)} sx={{ width: 120 }}>
          <MenuItem value="">全部</MenuItem>
          {Object.entries(SCRIPT_TYPE_LABELS).map(([v, label]) => <MenuItem key={v} value={v}>{label}</MenuItem>)}
        </TextField>
      </Stack>

      {isLoading && <LinearProgress sx={{ borderRadius: 1 }} />}

      {isLoading && (
        <Card variant="outlined">
          <CardContent>
            <Skeleton variant="text" width={160} height={24} sx={{ mb: 1 }} />
            <Skeleton variant="rectangular" height={300} sx={{ borderRadius: 1 }} />
          </CardContent>
        </Card>
      )}

      {!isLoading && rankList.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>效果评分排行（柱图）</Typography>
            <ReactECharts option={barOption} style={{ height: 300 }} />
          </CardContent>
        </Card>
      )}

      <Typography variant="subtitle2">Top 话术列表</Typography>

      {isLoading && (
        <Grid container spacing={2}>
          {[0,1,2,3,4,5].map(i => (
            <Grid item xs={12} sm={6} md={4} key={i}>
              <Card variant="outlined">
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                    <Skeleton variant="circular" width={20} height={20} />
                    <Skeleton variant="text" width="60%" />
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Skeleton variant="text" width={60} />
                    <Skeleton variant="text" width={60} />
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {!isLoading && (
        <Grid container spacing={2}>
          {topList.map((r, idx) => {
            const scoreNum = typeof r.score === 'number' ? r.score : typeof r.effectivenessScore === 'number' ? r.effectivenessScore : null
            const typeLabelStr = SCRIPT_TYPE_LABELS[String(r.scriptType ?? '')] ?? String(r.scriptType ?? '—')
            return (
              <Grid item xs={12} sm={6} md={4} key={String(r.id ?? idx)}>
                <Card variant="outlined" sx={{ '&:hover': { boxShadow: 2 } }}>
                  <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                      <Typography variant="caption" sx={{ bgcolor: idx < 3 ? 'warning.main' : 'primary.main', color: 'white', borderRadius: '50%', width: 20, height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, flexShrink: 0, fontWeight: 700 }}>
                        {idx + 1}
                      </Typography>
                      <Typography variant="body2" fontWeight={600} noWrap sx={{ flex: 1 }}>{String(r.scriptTitle ?? r.title ?? '—')}</Typography>
                      <Chip label={typeLabelStr} size="small" variant="outlined" sx={{ fontSize: 10, height: 18 }} />
                    </Stack>
                    {scoreNum !== null && (
                      <Box sx={{ mt: 0.5 }}>
                        <Stack direction="row" justifyContent="space-between" mb={0.25}>
                          <Typography variant="caption" color="text.secondary">效果评分</Typography>
                          <Typography variant="caption" fontWeight={600} color={scoreNum >= 7 ? 'success.main' : scoreNum >= 4 ? 'warning.main' : 'error.main'}>{scoreNum.toFixed(1)}</Typography>
                        </Stack>
                        <LinearProgress variant="determinate" value={Math.min(scoreNum * 10, 100)} color={scoreNum >= 7 ? 'success' : scoreNum >= 4 ? 'warning' : 'error'} sx={{ height: 6, borderRadius: 3 }} />
                      </Box>
                    )}
                    {scoreNum === null && (
                      <Typography variant="caption" color="text.secondary">评分：{String(r.score ?? r.effectivenessScore ?? '—')}</Typography>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            )
          })}
          {topList.length === 0 && (
            <Grid item xs={12}><Typography color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>暂无数据</Typography></Grid>
          )}
        </Grid>
      )}
    </Box>
  )
}
