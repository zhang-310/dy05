import { useState, useMemo } from 'react'
import {
  Box, Typography, Grid, Card, CardContent, Chip,
  FormControl, InputLabel, Select, MenuItem, Button,
  LinearProgress, Stack, Alert, Link, ToggleButton, ToggleButtonGroup,
} from '@mui/material'
import {
  Speed as SpeedIcon, TrendingUp as TrendIcon,
  Storage as StorageIcon, Refresh as RefreshIcon,
  CheckCircle as CheckIcon, Error as ErrorIcon,
  OpenInNew as OpenInNewIcon,
} from '@mui/icons-material'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { aiApi } from '@/api/ai'
import type {
  AiDashboardStatsVO,
  AiCallVolumeTrendItem,
  AiCallTypeDistributionItem,
  AiQuotaTrendItem,
  AiCostBreakdownItem,
  AiInfraHealthItem,
  AiCacheStatsVO,
  AiSearchStatsVO,
  AiInfraDetailFullVO,
  AiMonitoringConfigVO,
  AiCallVolumeTrendParams,
} from '@/types/ai'

const CALL_TYPE_LABELS: Record<string, string> = {
  text2img: '图像生成', tts: '语音合成', video: '视频处理',
  kb_search: '知识检索', evolution: '自进化', chat: '对话',
  script_gen: '话术生成', embedding: '向量化', rewrite: '改写',
  live_analysis: '直播分析', live_script_slot: '直播话术槽位', live_script_full: '直播全文话术',
  live_script_skeleton: '直播话术骨架', live_script_refine: '直播话术润色', live_script_chat: '直播话术对话',
}

function KpiCard({ title, value, unit, color, icon }: {
  title: string; value: string | number; unit?: string
  color?: string; icon?: React.ReactNode
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="body2" color="text.secondary">{title}</Typography>
            <Typography variant="h4" fontWeight={700} color={color ?? 'text.primary'}>
              {value}{unit && <Typography component="span" variant="body1" color="text.secondary" ml={0.5}>{unit}</Typography>}
            </Typography>
          </Box>
          {icon && <Box sx={{ color: color ?? 'primary.main', opacity: 0.7 }}>{icon}</Box>}
        </Stack>
      </CardContent>
    </Card>
  )
}

function getInfraStatus(items: AiInfraHealthItem[], component: string): { label: string; ok: boolean } {
  const item = items.find(i => i.component?.toLowerCase() === component.toLowerCase())
  if (!item) return { label: '未接入', ok: false }
  return { label: item.ok ? '正常' : (item.message || '异常'), ok: item.ok }
}

function formatTokens(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`
  return n.toLocaleString()
}

function pickNumeric(obj: Record<string, unknown> | null | undefined, keys: string[]): number {
  if (!obj) return 0
  for (const k of keys) {
    const v = obj[k]
    if (typeof v === 'number' && !Number.isNaN(v)) return v
    if (typeof v === 'string' && v.trim() !== '') {
      const n = Number(v)
      if (!Number.isNaN(n)) return n
    }
  }
  return 0
}

export default function AiDashboardPage() {
  const [timeRange, setTimeRange] = useState(7)
  const [trendGranularity, setTrendGranularity] = useState<'day' | 'hour'>('day')
  const [trendCallType, setTrendCallType] = useState('')

  const trendParams = useMemo((): AiCallVolumeTrendParams => {
    if (trendGranularity === 'hour') {
      const hours = timeRange <= 1 ? 24 : Math.min(timeRange * 24, 168)
      const p: AiCallVolumeTrendParams = { hours }
      if (trendCallType) p.callType = trendCallType
      return p
    }
    const p: AiCallVolumeTrendParams = { days: timeRange }
    if (trendCallType) p.callType = trendCallType
    return p
  }, [timeRange, trendGranularity, trendCallType])

  const { data: statsData, refetch: refetchStats } = useQuery({
    queryKey: ['ai-dashboard-stats'],
    queryFn: () => aiApi.dashboardStats(),
    refetchInterval: 60000,
  })

  const { data: trendData = [], refetch: refetchTrend } = useQuery({
    queryKey: ['ai-call-trend', trendParams],
    queryFn: () => aiApi.callVolumeTrend(trendParams),
  })

  const { data: distData = [], refetch: refetchDist } = useQuery({
    queryKey: ['ai-call-dist', timeRange],
    queryFn: () => aiApi.callTypeDistribution({ days: timeRange }),
  })

  const { data: quotaData = [], refetch: refetchQuota } = useQuery({
    queryKey: ['ai-quota-trend', timeRange],
    queryFn: () => aiApi.quotaTrend({ days: timeRange }),
  })

  const { data: costData = [], refetch: refetchCost } = useQuery({
    queryKey: ['ai-cost-breakdown', timeRange],
    queryFn: () => aiApi.dashboardCostBreakdown({ days: timeRange }),
  })

  const { data: infraHealth, refetch: refetchHealth } = useQuery({
    queryKey: ['ai-infra-health'],
    queryFn: aiApi.infraHealth,
    refetchInterval: 30000,
  })

  const { data: cacheStats, refetch: refetchCache } = useQuery({
    queryKey: ['ai-cache-stats'],
    queryFn: aiApi.cacheStats,
    refetchInterval: 30000,
  })

  const { data: searchStats, refetch: refetchSearch } = useQuery({
    queryKey: ['ai-search-stats'],
    queryFn: aiApi.searchStats,
    refetchInterval: 30000,
  })

  const { data: infraDetail, refetch: refetchInfraDetail } = useQuery({
    queryKey: ['ai-infra-detail'],
    queryFn: aiApi.infraDetail,
    staleTime: 120_000,
  })

  const { data: monitoringCfg } = useQuery({
    queryKey: ['ai-monitoring-config'],
    queryFn: aiApi.monitoringConfig,
    staleTime: 300_000,
  })

  const trendArr: AiCallVolumeTrendItem[] = Array.isArray(trendData) ? trendData : []
  const distArr: AiCallTypeDistributionItem[] = Array.isArray(distData) ? distData : []
  const quotaArr: AiQuotaTrendItem[] = Array.isArray(quotaData) ? quotaData : []
  const costArr: AiCostBreakdownItem[] = Array.isArray(costData) ? costData : []

  const trendOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trendArr.map((d) => String(d.date ?? d.hour ?? '')) },
    yAxis: { type: 'value', name: '调用次数' },
    series: [{ name: 'AI调用量', type: 'line', smooth: true,
      data: trendArr.map((d) => Number(d.count ?? d.total ?? 0)),
      areaStyle: { opacity: 0.15 } }],
    grid: { left: 48, right: 20, top: 30, bottom: trendGranularity === 'hour' ? 48 : 30 },
  }

  const distOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      name: '调用类型', type: 'pie', radius: ['40%', '70%'],
      data: distArr.map((d) => ({
        name: CALL_TYPE_LABELS[String(d.callType ?? d.type ?? '')] ?? String(d.callType ?? d.type ?? ''),
        value: Number(d.count ?? d.total ?? 0),
      })),
    }],
  }

  const quotaOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['已用额度', '日上限(汇总)'], bottom: 0 },
    xAxis: { type: 'category', data: quotaArr.map((d) => String(d.date ?? '')) },
    yAxis: { type: 'value', name: '次数' },
    series: [
      { name: '已用额度', type: 'bar', data: quotaArr.map((d) => pickNumeric(d, ['usedCount', 'used'])) },
      { name: '日上限(汇总)', type: 'line', smooth: true, data: quotaArr.map((d) => pickNumeric(d, ['maxCount', 'max', 'limit'])) },
    ],
    grid: { left: 48, right: 20, top: 28, bottom: 56 },
  }

  const costBarOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: costArr.map((d) =>
        CALL_TYPE_LABELS[String(d.callType ?? '')] ?? String(d.callType ?? '')),
      axisLabel: { rotate: costArr.length > 6 ? 30 : 0 },
    },
    yAxis: { type: 'value', name: 'Tokens' },
    series: [{ name: 'Token(汇总)', type: 'bar', data: costArr.map((d) => Number(d.tokens ?? 0)) }],
    grid: { left: 52, right: 20, top: 28, bottom: 72 },
  }

  const costPieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['36%', '62%'],
      data: costArr.map((d) => ({
        name: CALL_TYPE_LABELS[String(d.callType ?? '')] ?? String(d.callType ?? ''),
        value: Number(d.tokens ?? 0),
      })).filter(d => d.value > 0),
    }],
  }

  const infraItems: AiInfraHealthItem[] = Array.isArray(infraHealth) ? infraHealth : []
  const milvusStatus = useMemo(() => getInfraStatus(infraItems, 'milvus'), [infraItems])
  const esStatus = useMemo(() => getInfraStatus(infraItems, 'elasticsearch'), [infraItems])
  const redisStatus = useMemo(() => getInfraStatus(infraItems, 'redis'), [infraItems])
  const llmStatus = useMemo(() => getInfraStatus(infraItems, 'llm'), [infraItems])

  const cache: AiCacheStatsVO | undefined = cacheStats
  const stats: AiDashboardStatsVO | undefined = statsData
  const search: AiSearchStatsVO | undefined = searchStats
  const detail: AiInfraDetailFullVO | undefined = infraDetail
  const monitoringCfgTyped: AiMonitoringConfigVO | undefined = monitoringCfg
  const grafanaUrl = monitoringCfgTyped?.grafanaUrl

  const qualityScore = Number(stats?.avgQualityScore ?? 0)
  const monthTokens = Number(stats?.monthTokens ?? 0)

  const kpiCards = [
    { title: '今日AI调用', value: Number(stats?.todayCalls ?? 0).toLocaleString(), unit: '次', color: 'primary.main', icon: <SpeedIcon /> },
    { title: '本月Token消耗', value: formatTokens(monthTokens), unit: 'tokens', color: 'secondary.main', icon: <StorageIcon /> },
    { title: '话术生成成功率', value: `${Number(stats?.successRate ?? 0).toFixed(1)}%`, color: 'success.main', icon: <TrendIcon /> },
    { title: '知识库质量均分', value: qualityScore.toFixed(1), unit: '/100', color: qualityScore >= 60 ? 'success.main' : 'warning.main', icon: <StorageIcon /> },
  ]

  const infraKpiCards = [
    { title: '向量库 (Milvus)', value: milvusStatus.label, ok: milvusStatus.ok },
    { title: '搜索引擎 (ES)', value: esStatus.label, ok: esStatus.ok },
    { title: 'Redis', value: redisStatus.label, ok: redisStatus.ok },
    { title: 'LLM 网关', value: llmStatus.label, ok: llmStatus.ok },
  ]

  const handleRefresh = () => {
    void refetchStats()
    void refetchTrend()
    void refetchDist()
    void refetchQuota()
    void refetchCost()
    void refetchHealth()
    void refetchCache()
    void refetchSearch()
    void refetchInfraDetail()
  }

  const pg = detail?.postgresql
  const mv = detail?.milvus
  const esd = detail?.elasticsearch

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1}>
        <Typography variant="h5" fontWeight={600}>AI 中心仪表盘</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
          <FormControl size="small" sx={{ minWidth: 110 }}>
            <InputLabel>时间范围</InputLabel>
            <Select value={timeRange} label="时间范围" onChange={e => setTimeRange(Number(e.target.value))}>
              <MenuItem value={1}>今日</MenuItem>
              <MenuItem value={7}>近7天</MenuItem>
              <MenuItem value={30}>近30天</MenuItem>
            </Select>
          </FormControl>
          <ToggleButtonGroup
            size="small"
            value={trendGranularity}
            exclusive
            onChange={(_, v) => v != null && setTrendGranularity(v)}
          >
            <ToggleButton value="day">按日</ToggleButton>
            <ToggleButton value="hour">按小时</ToggleButton>
          </ToggleButtonGroup>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel>趋势类型</InputLabel>
            <Select
              value={trendCallType}
              label="趋势类型"
              onChange={e => setTrendCallType(e.target.value)}
            >
              <MenuItem value="">全部类型</MenuItem>
              {Object.entries(CALL_TYPE_LABELS).map(([k, label]) => (
                <MenuItem key={k} value={k}>{label}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button size="small" startIcon={<RefreshIcon />} onClick={handleRefresh}>刷新</Button>
          <Button size="small" component={RouterLink} to="/admin/ai/admin-infra" variant="outlined">基础设施详情</Button>
          {typeof grafanaUrl === 'string' && grafanaUrl.startsWith('http') && (
            <Button size="small" endIcon={<OpenInNewIcon />} href={grafanaUrl} target="_blank" rel="noopener noreferrer">
              Grafana
            </Button>
          )}
        </Stack>
      </Stack>

      <Grid container spacing={2}>
        {kpiCards.map(k => (
          <Grid item xs={12} sm={6} md={3} key={k.title}>
            <KpiCard title={k.title} value={k.value} unit={k.unit} color={k.color} icon={k.icon} />
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        {infraKpiCards.map(k => (
          <Grid item xs={12} sm={6} md={3} key={k.title}>
            <KpiCard title={k.title} value={k.value}
              color={k.ok ? 'success.main' : 'warning.main'}
              icon={k.ok ? <CheckIcon fontSize="large" /> : <ErrorIcon fontSize="large" />} />
          </Grid>
        ))}
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard title="缓存命中率" value={cache ? `${Number(cache.hitRate ?? 0).toFixed(1)}` : '—'} unit="%"
            color="primary.main" icon={<TrendIcon fontSize="large" />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KpiCard title="缓存统计" value={cache ? `${Number(cache.hit ?? 0)}/${Number(cache.total ?? 0)}` : '—'} unit="命中/总数"
            icon={<StorageIcon fontSize="large" />} />
        </Grid>
      </Grid>

      {search && !search.message && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} mb={2}>检索性能（混合检索采样）</Typography>
            <Grid container spacing={2}>
              {([
                ['总请求', search.totalRequests],
                ['估算 QPS', search.qps],
                ['P50 (ms)', search.p50Ms],
                ['P95 (ms)', search.p95Ms],
                ['P99 (ms)', search.p99Ms],
                ['采样数', search.sampleCount],
              ] as const satisfies ReadonlyArray<readonly [string, unknown]>).map(([k, v]) => (
                <Grid item xs={6} sm={4} md={2} key={k}>
                  <Typography variant="caption" color="text.secondary" display="block">{k}</Typography>
                  <Typography fontWeight={600} component="div">{v == null ? '—' : String(v)}</Typography>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}
      {search?.message != null && (
        <Alert severity="info">{String(search.message)}</Alert>
      )}

      {infraItems.length > 0 && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} mb={2}>基础设施健康</Typography>
            <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
              {infraItems.map(item => (
                <Chip
                  key={item.component}
                  label={`${item.component}${item.ok ? '' : ` — ${item.message || '异常'}`}`}
                  size="small"
                  color={item.ok ? 'success' : 'error'}
                  icon={item.ok ? <CheckIcon /> : <ErrorIcon />}
                />
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      {detail && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} mb={2}>资源摘要</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <Typography variant="caption" color="text.secondary">PostgreSQL</Typography>
                <Typography variant="body2">
                  {pg?.ok ? `${String(pg.host ?? '')} / ${String(pg.database ?? '')} · 连接 ${String(pg.poolActive ?? '—')}/${String(pg.poolTotal ?? '—')} · ${String(pg.responseMs ?? '—')}ms` : String(pg?.error ?? '—')}
                </Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="caption" color="text.secondary">Milvus</Typography>
                <Typography variant="body2">
                  {mv?.ok ? `向量约 ${Number(mv.totalVectors ?? 0).toLocaleString()} · 集合 ${String(mv.collectionCount ?? '—')}` : String(mv?.error ?? '—')}
                </Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="caption" color="text.secondary">Elasticsearch</Typography>
                <Typography variant="body2">
                  {esd?.ok ? `状态 ${String(esd.status ?? '—')} · ${String(esd.clusterName ?? '')}` : String(esd?.error ?? '—')}
                </Typography>
              </Grid>
            </Grid>
            {Array.isArray(detail.suggestions) && detail.suggestions.length > 0 && (
              <Stack spacing={0.5} mt={2}>
                <Typography variant="caption" color="text.secondary">建议</Typography>
                {detail.suggestions.map((s, i) => (
                  <Typography key={i} variant="body2" color="warning.main">
                    • {String(s.component ?? '')}{s.message != null ? `：${String(s.message)}` : ''}
                  </Typography>
                ))}
              </Stack>
            )}
          </CardContent>
        </Card>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={1}>
                调用量趋势{trendGranularity === 'hour' ? '（按小时）' : '（按日）'}
                {trendCallType ? ` · ${CALL_TYPE_LABELS[trendCallType] ?? trendCallType}` : ''}
              </Typography>
              {trendArr.length === 0
                ? <Alert severity="info">暂无趋势数据</Alert>
                : <ReactECharts option={trendOption} style={{ height: 280 }} />}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={1}>调用类型分布</Typography>
              {distArr.length === 0
                ? <Alert severity="info">暂无分布数据</Alert>
                : <ReactECharts option={distOption} style={{ height: 280 }} />}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={1}>额度使用趋势（按日汇总）</Typography>
              {quotaArr.length === 0
                ? <Alert severity="info">暂无额度流水数据（ai_call_quota）</Alert>
                : <ReactECharts option={quotaOption} style={{ height: 300 }} />}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={5}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={1}>Token 用量拆解（按类型）</Typography>
              {costArr.length === 0
                ? <Alert severity="info">暂无 Token 汇总（需调用日志含 total_tokens 或估算值）</Alert>
                : <ReactECharts option={costPieOption} style={{ height: 300 }} />}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {costArr.length > 0 && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} mb={2}>Token 与调用次数（按类型）</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={7}>
                <ReactECharts option={costBarOption} style={{ height: 320 }} />
              </Grid>
              <Grid item xs={12} md={5}>
                <Stack spacing={1}>
                  {costArr.map((row) => {
                    const ct = String(row.callType ?? '')
                    return (
                      <Stack key={ct} direction="row" justifyContent="space-between" alignItems="center">
                        <Typography variant="body2">{CALL_TYPE_LABELS[ct] ?? ct}</Typography>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Chip size="small" label={`${formatTokens(Number(row.tokens ?? 0))} tok`} />
                          <Chip size="small" variant="outlined" label={`${Number(row.calls ?? 0)} 次`} />
                          <Typography variant="caption" color="text.secondary">{Number(row.tokenSharePct ?? 0).toFixed(1)}%</Typography>
                        </Stack>
                      </Stack>
                    )
                  })}
                </Stack>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {cache && (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} mb={2}>缓存统计</Typography>
            <Grid container spacing={2}>
              {Object.entries(cache).map(([k, v]) => (
                <Grid item key={k} xs={6} sm={4} md={2}>
                  <Typography variant="caption" color="text.secondary" display="block">{k}</Typography>
                  <Typography fontWeight={500}>{typeof v === 'number' ? v.toLocaleString() : String(v ?? '')}</Typography>
                  {k === 'hitRate' && <LinearProgress variant="determinate" value={Math.min(Number(v), 100)} sx={{ mt: 0.5 }} />}
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      <Typography variant="caption" color="text.secondary">
        说明：Token 拆解来自成功调用日志汇总；知识检索类可能为估算值。
        更细的 PG/ES/Milvus 分页与索引队列见
        <Link component={RouterLink} to="/admin/ai/admin-infra" ml={0.5}>AI 基础设施</Link>
        。
      </Typography>
    </Box>
  )
}
