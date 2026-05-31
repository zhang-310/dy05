import { useState, useMemo } from 'react'
import {
  Box, Typography, Grid, Card, CardContent, Chip,
  FormControl, InputLabel, Select, MenuItem, Button,
  LinearProgress, Stack, Alert, Link, ToggleButton, ToggleButtonGroup,
  Accordion, AccordionSummary, AccordionDetails, Divider,
} from '@mui/material'
import {
  Speed as SpeedIcon, TrendingUp as TrendIcon,
  Storage as StorageIcon, Refresh as RefreshIcon,
  CheckCircle as CheckIcon, Error as ErrorIcon,
  OpenInNew as OpenInNewIcon, ExpandMore as ExpandMoreIcon,
  WarningAmber as WarningIcon,
  Hub as HubIcon, Rule as RuleIcon, Psychology as PsychologyIcon,
} from '@mui/icons-material'
import ReactECharts from 'echarts-for-react'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { useSnackbar } from 'notistack'
import { aiApi } from '@/api/ai'
import { getErrorMessage } from '@/utils/errorHandler'
import type {
  AiDashboardStatsVO,
  AiCallVolumeTrendItem,
  AiCallTypeDistributionItem,
  AiQuotaTrendItem,
  AiCostBreakdownItem,
  AiInfraHealthItem,
  AiCacheStatsVO,
  AiCacheDiagnosticsVO,
  AiSearchStatsVO,
  AiInfraDetailFullVO,
  AiMonitoringConfigVO,
  AiCallVolumeTrendParams,
  AiCallLogVO,
} from '@/types/ai'
import type { KnowledgeBase } from '@/api/ai'

const CALL_TYPE_LABELS: Record<string, string> = {
  text2img: '图像生成', tts: '语音合成', video: '视频处理',
  kb_search: '知识检索', evolution: '自进化', chat: '对话',
  script_gen: '话术生成', embedding: '向量化', rewrite: '改写',
  live_analysis: '直播分析', live_script_slot: '直播话术槽位', live_script_full: '直播全文话术',
  live_script_skeleton: '直播话术骨架', live_script_refine: '直播话术润色', live_script_chat: '直播话术对话',
}

const AI_DASHBOARD_READY_ENDPOINTS = [
  '/ai/admin/dashboard/stats',
  '/ai/knowledge-base/list',
  '/ai/admin/call-log/search',
  '/ai/admin/dashboard/call-volume-trend',
  '/ai/admin/dashboard/call-type-distribution',
  '/ai/admin/dashboard/quota-trend',
  '/ai/admin/dashboard/cost-breakdown',
  '/ai/admin/dashboard/kb-quality/rescan',
  '/ai/admin/infra/health',
  '/ai/admin/infra/cache/stats',
  '/ai/admin/infra/cache/diagnostics',
  '/ai/admin/infra/search/stats',
  '/ai/admin/infra/detail',
  '/ai/admin/infra/monitoring-config',
].join('|')

interface BusinessAiChain {
  key: string
  title: string
  description: string
  callTypes: string[]
  requiredKbNames: string[]
  gateEnabled: boolean
  gateDescription: string
  actionPath: string
}

const BUSINESS_AI_CHAINS: BusinessAiChain[] = [
  {
    key: 'live-script',
    title: '直播话术生成',
    description: '生成、微调、排品和违规审核应引用直播知识、官方学习与违规规则。',
    callTypes: [
      'live_script_slot', 'live_script_full', 'live_script_skeleton', 'live_script_refine', 'live_script_chat',
      'live_script_opening', 'live_script_product', 'live_script_transition', 'live_script_closing',
      'live_script_custom', 'live_script_welfare', 'live_script_interaction', 'live_script_parallel',
      'live_script_violation',
    ],
    requiredKbNames: ['douyin', 'douyin_weigui', 'douyin_ops_strategy'],
    gateEnabled: true,
    gateDescription: '后端生成、槽位保存和审批通过均已启用官方引用强门禁。',
    actionPath: '/org/live/sessions',
  },
  {
    key: 'shortvideo',
    title: '短视频脚本/分镜',
    description: '脚本、分镜和数字人成片应引用官方规则、爆款模式和复盘经验。',
    callTypes: [
      'script_gen', 'short_video_script', 'shortvideo_script',
      'short_video_copy', 'short_video_video_plan', 'short_video_generate',
      'video', 'tts',
    ],
    requiredKbNames: ['douyin', 'douyin_weigui', 'douyin_viral_patterns'],
    gateEnabled: true,
    gateDescription: '短视频文案、脚本和制作方案生成已要求 douyin + douyin_weigui 引用。',
    actionPath: '/talent/shortvideo/script-planning',
  },
  {
    key: 'violation',
    title: '违规检测与千川素材审核',
    description: '审核结果必须有 douyin_weigui 或 violation_rule 引用，否则不能判定通过。',
    callTypes: [
      'compliance', 'violation_check',
      'short_video_violation_check', 'live_violation_check', 'live_script_violation',
    ],
    requiredKbNames: ['douyin_weigui'],
    gateEnabled: true,
    gateDescription: '短视频审核、发布审核和直播审核链路缺少 douyin_weigui 时不能通过。',
    actionPath: '/admin/system/compliance',
  },
  {
    key: 'learning',
    title: '爆款学习与复盘回流',
    description: '采集、拆解、复盘应沉淀爆款模式和成功/失败模板，反哺下一次生成。',
    callTypes: ['evolution', 'live_analysis', 'short_video_analysis', 'viral_deep_analysis'],
    requiredKbNames: ['douyin_viral_patterns', 'douyin_performance_reflections'],
    gateEnabled: false,
    gateDescription: '学习回流不是放行口，按知识资产和近期调用验证闭环。',
    actionPath: '/admin/ai/evolution',
  },
]

interface KnowledgeAssetGroup {
  key: string
  title: string
  description: string
  aliases: string[]
  actionPath: string
}

const KNOWLEDGE_ASSET_GROUPS: KnowledgeAssetGroup[] = [
  { key: 'official', title: '官方规则库', description: '抖音学习中心、平台规则和经营规范。', aliases: ['douyin'], actionPath: '/admin/ai/knowledge' },
  { key: 'violation', title: '违规规则库', description: '直播、短视频、千川素材审核的硬门禁规则。', aliases: ['douyin_weigui', 'violation'], actionPath: '/admin/ai/knowledge' },
  { key: 'viral', title: '爆款模式库', description: '采集/拆解沉淀的爆款结构、钩子、节奏和二创变量。', aliases: ['viral', 'douyin_viral_patterns', '爆款'], actionPath: '/admin/ai/viral-analysis' },
  { key: 'reflection', title: '复盘经验库', description: '发布和直播复盘回流出的成功模板与失败原因。', aliases: ['reflection', 'performance', 'douyin_performance_reflections', '复盘'], actionPath: '/admin/ai/knowledge-evolution' },
  { key: 'strategy', title: '运营策略库', description: '排品、时长、话术结构、行业玩法和 AI 推荐策略。', aliases: ['strategy', 'douyin_ops_strategy', '运营'], actionPath: '/admin/ai/industry-brain' },
]

const AI_DASHBOARD_UNSUPPORTED_ENDPOINTS = [
  '/ai/admin/dashboard/mock',
  '/ai/admin/dashboard/local-quality-score',
  '/ai/admin/dashboard/kb-quality/local-rescan',
  '/ai/admin/infra/cache/ttl/update',
  '/ai/admin/infra/cache/flush',
  '/ai/admin/infra/cache/hot-key/mock',
  '/ai/admin/infra/cache/prewarm',
  '/ai/admin/infra/search/mock',
].join('|')

function KpiCard({ title, value, unit, color, icon, description }: {
  title: string; value: string | number; unit?: string
  color?: string; icon?: React.ReactNode; description?: string
}) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="body2" color="text.secondary" noWrap>{title}</Typography>
            <Typography variant="h4" fontWeight={700} color={color ?? 'text.primary'} sx={{ mt: 0.5, lineHeight: 1.1 }}>
              {value}{unit && <Typography component="span" variant="body1" color="text.secondary" ml={0.5}>{unit}</Typography>}
            </Typography>
            {description && (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75, whiteSpace: 'normal' }}>
                {description}
              </Typography>
            )}
          </Box>
          {icon && <Box sx={{ color: color ?? 'primary.main', opacity: 0.75, flexShrink: 0 }}>{icon}</Box>}
        </Stack>
      </CardContent>
    </Card>
  )
}

function SectionCard({ title, action, children }: {
  title: string
  action?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
          <Typography variant="subtitle1" fontWeight={700}>{title}</Typography>
          {action}
        </Stack>
        {children}
      </CardContent>
    </Card>
  )
}

function getInfraStatus(items: AiInfraHealthItem[], component: string): { label: string; ok: boolean } {
  const item = items.find(i => i.component?.toLowerCase() === component.toLowerCase())
  if (!item) return { label: '未上报', ok: false }
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

function formatPercent(value: unknown, digits = 1): string {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? n.toFixed(digits) : '0.0'
}

function formatSeconds(value: unknown): string {
  const seconds = Number(value ?? 0)
  if (!Number.isFinite(seconds) || seconds <= 0) return '—'
  if (seconds >= 86400) return `${(seconds / 86400).toFixed(1)} 天`
  if (seconds >= 3600) return `${(seconds / 3600).toFixed(1)} 小时`
  if (seconds >= 60) return `${Math.round(seconds / 60)} 分钟`
  return `${Math.round(seconds)} 秒`
}

function bucketLabel(bucket: string): string {
  const labels: Record<string, string> = {
    noExpire: '无 TTL',
    expiredOrMissing: '已过期/丢失',
    lt5m: '<5 分钟',
    lt1h: '<1 小时',
    lt1d: '<1 天',
    gte1d: '>=1 天',
  }
  return labels[bucket] ?? bucket
}

function statusSeverity(okCount: number, total: number): 'success' | 'warning' | 'error' {
  if (total === 0 || okCount === 0) return 'error'
  if (okCount < total) return 'warning'
  return 'success'
}

function isSuccessStatus(status: unknown): boolean {
  if (typeof status === 'number') return status === 1 || status === 200
  if (typeof status === 'string') return ['success', 'succeeded', 'ok', 'passed', '1'].includes(status.toLowerCase())
  return false
}

function matchesKbAlias(kb: KnowledgeBase, alias: string): boolean {
  const target = alias.toLowerCase()
  const text = [kb.kbName, kb.kbType, kb.description].filter(Boolean).join(' ').toLowerCase()
  return text.includes(target)
}

function hasReferencedChunks(log: AiCallLogVO): boolean {
  return typeof log.referencedChunkIds === 'string' && log.referencedChunkIds.trim().length > 0
}

function chainStatusColor(status: 'ready' | 'partial' | 'missing'): 'success' | 'warning' | 'error' {
  if (status === 'ready') return 'success'
  if (status === 'partial') return 'warning'
  return 'error'
}

function chainStatusLabel(status: 'ready' | 'partial' | 'missing'): string {
  if (status === 'ready') return '已接入'
  if (status === 'partial') return '部分接入'
  return '待补强'
}

function asPercent(numerator: number, denominator: number): number {
  if (denominator <= 0) return 0
  return (numerator / denominator) * 100
}

export default function AiDashboardPage() {
  const [timeRange, setTimeRange] = useState(7)
  const [trendGranularity, setTrendGranularity] = useState<'day' | 'hour'>('day')
  const [trendCallType, setTrendCallType] = useState('')
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [isRescanningQuality, setIsRescanningQuality] = useState(false)
  const [qualityRescanError, setQualityRescanError] = useState('')
  const { enqueueSnackbar } = useSnackbar()

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

  const { data: statsData, refetch: refetchStats, isError: isStatsError, error: statsError } = useQuery({
    queryKey: ['ai-dashboard-stats'],
    queryFn: () => aiApi.dashboardStats(),
    refetchInterval: 60000,
  })

  const { data: trendData = [], refetch: refetchTrend, isError: isTrendError, error: trendError } = useQuery({
    queryKey: ['ai-call-trend', trendParams],
    queryFn: () => aiApi.callVolumeTrend(trendParams),
  })

  const { data: distData = [], refetch: refetchDist, isError: isDistError, error: distError } = useQuery({
    queryKey: ['ai-call-dist', timeRange],
    queryFn: () => aiApi.callTypeDistribution({ days: timeRange }),
  })

  const { data: quotaData = [], refetch: refetchQuota, isError: isQuotaError, error: quotaError } = useQuery({
    queryKey: ['ai-quota-trend', timeRange],
    queryFn: () => aiApi.quotaTrend({ days: timeRange }),
  })

  const { data: costData = [], refetch: refetchCost, isError: isCostError, error: costError } = useQuery({
    queryKey: ['ai-cost-breakdown', timeRange],
    queryFn: () => aiApi.dashboardCostBreakdown({ days: timeRange }),
  })

  const { data: infraHealth, refetch: refetchHealth, isError: isInfraHealthError } = useQuery({
    queryKey: ['ai-infra-health'],
    queryFn: aiApi.infraHealth,
    refetchInterval: 30000,
  })

  const { data: cacheStats, refetch: refetchCache, isError: isCacheStatsError, error: cacheStatsError } = useQuery({
    queryKey: ['ai-cache-stats'],
    queryFn: aiApi.cacheStats,
    refetchInterval: 30000,
  })

  const { data: cacheDiagnostics, refetch: refetchCacheDiagnostics, isError: isCacheDiagnosticsError, error: cacheDiagnosticsError } = useQuery({
    queryKey: ['ai-cache-diagnostics'],
    queryFn: aiApi.cacheDiagnostics,
    refetchInterval: 60000,
  })

  const { data: searchStats, refetch: refetchSearch, isError: isSearchError, error: searchError } = useQuery({
    queryKey: ['ai-search-stats'],
    queryFn: aiApi.searchStats,
    refetchInterval: 30000,
  })

  const { data: infraDetail, refetch: refetchInfraDetail, isError: isInfraDetailError, error: infraDetailError } = useQuery({
    queryKey: ['ai-infra-detail'],
    queryFn: aiApi.infraDetail,
    staleTime: 120_000,
  })

  const { data: monitoringCfg } = useQuery({
    queryKey: ['ai-monitoring-config'],
    queryFn: aiApi.monitoringConfig,
    staleTime: 300_000,
  })

  const { data: kbListData = [], refetch: refetchKbList, isError: isKbListError, error: kbListError } = useQuery({
    queryKey: ['ai-dashboard-kb-assets'],
    queryFn: () => aiApi.kbList({ page: 0, rows: 200 }),
    refetchInterval: 60000,
  })

  const { data: recentCallLogs, refetch: refetchRecentCallLogs, isError: isRecentCallLogsError, error: recentCallLogsError } = useQuery({
    queryKey: ['ai-dashboard-recent-call-logs'],
    queryFn: () => aiApi.adminCallLogList({ page: 0, rows: 80 }),
    refetchInterval: 60000,
  })

  const trendArr: AiCallVolumeTrendItem[] = Array.isArray(trendData) ? trendData : []
  const distArr: AiCallTypeDistributionItem[] = Array.isArray(distData) ? distData : []
  const quotaArr: AiQuotaTrendItem[] = Array.isArray(quotaData) ? quotaData : []
  const costArr: AiCostBreakdownItem[] = Array.isArray(costData) ? costData : []
  const kbList: KnowledgeBase[] = Array.isArray(kbListData) ? kbListData : []
  const callLogs: AiCallLogVO[] = recentCallLogs?.list ?? []

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
  const infraStatusItems = useMemo(() => [
    ['milvus', 'Milvus'],
    ['elasticsearch', 'Elasticsearch'],
    ['redis', 'Redis'],
    ['llm', 'LLM 网关'],
  ].map(([key, name]) => {
    const status = getInfraStatus(infraItems, key)
    return { key, name, statusText: status.label, ok: status.ok }
  }), [infraItems])

  const cache: AiCacheStatsVO | undefined = cacheStats
  const cacheDiag: AiCacheDiagnosticsVO | undefined = cacheDiagnostics
  const stats: AiDashboardStatsVO | undefined = statsData
  const search: AiSearchStatsVO | undefined = searchStats
  const detail: AiInfraDetailFullVO | undefined = infraDetail
  const monitoringCfgTyped: AiMonitoringConfigVO | undefined = monitoringCfg
  const grafanaUrl = monitoringCfgTyped?.grafanaUrl

  const qualityScore = Number(stats?.avgQualityScore ?? 0)
  const qualityCoverage = Number(stats?.qualityEvaluationCoverage ?? 0)
  const evaluatedQualityDocCount = Number(stats?.evaluatedQualityDocCount ?? 0)
  const qualityDocCount = Number(stats?.qualityDocCount ?? 0)
  const lowQualityDocCount = Number(stats?.lowQualityDocCount ?? 0)
  const qualityDescription = qualityDocCount > 0
    ? `已评估 ${evaluatedQualityDocCount.toLocaleString()}/${qualityDocCount.toLocaleString()}，覆盖率 ${qualityCoverage.toFixed(1)}%，低质 ${lowQualityDocCount.toLocaleString()}`
    : '暂无知识库文档'
  const monthTokens = Number(stats?.monthTokens ?? 0)
  const successRate = Number(stats?.successRate ?? 0)
  const businessCacheHitRate = Number(cacheDiag?.businessStats?.hitRate ?? cache?.hitRate ?? 0)
  const redisGlobalHitRate = Number(cacheDiag?.redisStats?.globalHitRate ?? 0)
  const cacheWarn = (cacheDiag?.businessStats != null || cache != null) && businessCacheHitRate > 0 && businessCacheHitRate < 60
  const infraOkCount = infraStatusItems.filter(i => i.ok).length
  const infraSeverity = infraItems.length === 0 ? 'warning' : statusSeverity(infraOkCount, infraStatusItems.length)
  const p95Ms = Number(search?.p95Ms ?? 0)
  const p99Ms = Number(search?.p99Ms ?? 0)
  const qualityNeedsAction = qualityDocCount > 0 && (qualityScore < 60 || qualityCoverage < 80 || lowQualityDocCount > 0)
  const qualityActionHints = [
    qualityCoverage < 80 ? `先补齐未评估文档：点击“重评估知识质量”，来源 /ai/admin/dashboard/kb-quality/rescan，当前未评估 ${Number(stats?.unevaluatedQualityDocCount ?? Math.max(qualityDocCount - evaluatedQualityDocCount, 0)).toLocaleString()} 条。` : '',
    qualityScore < 60 ? '优先治理空内容、重复 chunk、过短 chunk、过期资料和无来源文档；质量分只取后端评估结果，不在前端伪造提升。' : '',
    lowQualityDocCount > 0 ? `低质文档 ${lowQualityDocCount.toLocaleString()} 条：到知识库文档页补标题、来源、业务标签、正文分段后重新向量化。` : '',
  ].filter(Boolean)
  const cacheNeedsAction = cacheWarn || Number(cache?.miss ?? 0) > Number(cache?.hit ?? 0)
  const cacheActionHints = [
    cacheNeedsAction ? '先看 Key 前缀分布与 TTL 分布，确认热点 cache:kb 是否集中在短 TTL、过期或无缓存命中的桶。' : '',
    cacheNeedsAction ? '再到 AI 基础设施页调整业务缓存 TTL 或热点预热策略，刷新后以 /ai/admin/infra/cache/diagnostics 的命中率和 miss 数复核。' : '',
    Number(cacheDiag?.redisStats?.evictedKeys ?? 0) > 0 ? 'Redis 出现 key 淘汰，需检查 maxmemory、淘汰策略和热点 key 体积。' : '',
  ].filter(Boolean)

  const assetGroups = KNOWLEDGE_ASSET_GROUPS.map(group => {
    const matched = kbList.filter(kb => group.aliases.some(alias => matchesKbAlias(kb, alias)))
    const documents = matched.reduce((sum, kb) => sum + Number(kb.totalDocuments ?? 0), 0)
    const tokens = matched.reduce((sum, kb) => sum + Number(kb.totalTokens ?? 0), 0)
    const ready = matched.filter(kb => Number(kb.status ?? 0) === 1 && Number(kb.totalDocuments ?? 0) > 0).length
    const status: 'ready' | 'partial' | 'missing' =
      documents > 0 && ready === matched.length && matched.length > 0
        ? 'ready'
        : matched.length > 0 || documents > 0
          ? 'partial'
          : 'missing'
    return { ...group, matched, documents, tokens, ready, status }
  })

  const businessChains = BUSINESS_AI_CHAINS.map(chain => {
    const logs = callLogs.filter(log => chain.callTypes.includes(String(log.callType ?? '')))
    const success = logs.filter(log => isSuccessStatus(log.status)).length
    const failures = logs.length - success
    const referenced = logs.filter(hasReferencedChunks).length
    const requiredKbReady = chain.requiredKbNames.filter(name =>
      kbList.some(kb => matchesKbAlias(kb, name) && Number(kb.totalDocuments ?? 0) > 0),
    )
    const kbCoverage = chain.requiredKbNames.length > 0 ? (requiredKbReady.length / chain.requiredKbNames.length) * 100 : 100
    const referenceCoverage = logs.length > 0 ? (referenced / logs.length) * 100 : 0
    const gateReady = chain.gateEnabled && kbCoverage >= 100
    const telemetrySatisfied = logs.length > 0 && referenceCoverage >= 60
    const status: 'ready' | 'partial' | 'missing' =
      gateReady || (logs.length > 0 && kbCoverage >= 80 && referenceCoverage >= 60)
        ? 'ready'
        : logs.length > 0 || kbCoverage > 0 || chain.gateEnabled
          ? 'partial'
          : 'missing'
    const mustBlock = chain.gateEnabled && kbCoverage < 100
    return { ...chain, logs, success, failures, referenced, kbCoverage, referenceCoverage, status, requiredKbReady, mustBlock, gateReady, telemetrySatisfied }
  })

  const officialRuleChain = businessChains.find(chain => chain.key === 'violation')
  const officialRuleSatisfied = officialRuleChain != null && officialRuleChain.gateReady
  const gateEnabledChainCount = businessChains.filter(chain => chain.gateEnabled).length
  const gateReadyChainCount = businessChains.filter(chain => chain.gateReady).length
  const totalRecentChainCalls = businessChains.reduce((sum, chain) => sum + chain.logs.length, 0)
  const referencedRecentChainCalls = businessChains.reduce((sum, chain) => sum + chain.referenced, 0)
  const overallReferenceCoverage = asPercent(referencedRecentChainCalls, totalRecentChainCalls)
  const readyAssetCount = assetGroups.filter(group => group.status === 'ready').length
  const totalAssetDocuments = assetGroups.reduce((sum, group) => sum + group.documents, 0)
  const totalAssetTokens = assetGroups.reduce((sum, group) => sum + group.tokens, 0)
  const aiOperatingStatus: 'ready' | 'partial' | 'missing' =
    gateReadyChainCount === gateEnabledChainCount && readyAssetCount >= 4 && officialRuleSatisfied
      ? 'ready'
      : gateReadyChainCount > 0 || readyAssetCount > 0 || totalRecentChainCalls > 0
        ? 'partial'
        : 'missing'
  const aiOperatingLabel = aiOperatingStatus === 'ready'
    ? '可用于核心业务'
    : aiOperatingStatus === 'partial'
      ? '仍有链路缺口'
      : '尚未形成闭环'
  const controlCards = [
    {
      title: '业务链路接入',
      value: `${gateReadyChainCount}/${gateEnabledChainCount}`,
      description: '后端强门禁已上线的核心业务链路数；近期调用单独显示。',
      color: chainStatusColor(gateReadyChainCount === gateEnabledChainCount ? 'ready' : gateReadyChainCount > 0 ? 'partial' : 'missing'),
      icon: <HubIcon />,
    },
    {
      title: '官方规则引用',
      value: totalRecentChainCalls > 0 ? `${overallReferenceCoverage.toFixed(0)}%` : '未上报',
      description: '最近业务 AI 调用中携带 referencedChunkIds 的比例。',
      color: overallReferenceCoverage >= 60 ? 'success' : totalRecentChainCalls > 0 ? 'warning' : 'error',
      icon: <RuleIcon />,
    },
    {
      title: '知识资产完备',
      value: `${readyAssetCount}/${assetGroups.length}`,
      description: `已沉淀 ${totalAssetDocuments.toLocaleString()} 篇文档，${formatTokens(totalAssetTokens)} tokens。`,
      color: readyAssetCount >= 4 ? 'success' : readyAssetCount > 0 ? 'warning' : 'error',
      icon: <PsychologyIcon />,
    },
  ]
  const actionItems = [
    isStatsError ? 'AI 统计接口失败，先恢复 /ai/admin/dashboard/stats。' : '',
    isKbListError ? '知识库列表加载失败，业务链路无法确认是否吃到官方资料。' : '',
    isRecentCallLogsError ? '调用日志加载失败，无法判断最近 AI 输出是否引用知识库。' : '',
    totalRecentChainCalls === 0 ? '当前没有近期业务 AI 调用，强门禁已按能力状态展示；需要触发一次话术/短视频生成来验证引用覆盖率。' : '',
    overallReferenceCoverage > 0 && overallReferenceCoverage < 60 ? `官方规则引用覆盖率只有 ${overallReferenceCoverage.toFixed(0)}%，请查看失败日志确认是否被门禁阻断。` : '',
    !officialRuleSatisfied ? '违规检测链路必须补齐 douyin_weigui 知识库，否则 AI 审核不能放行。' : '',
    readyAssetCount < assetGroups.length ? '知识资产中心仍有缺库或空库，需要继续采集官方规则、爆款模式、复盘经验和运营策略。' : '',
    qualityNeedsAction ? '知识库质量低或覆盖率不足，先处理低质/未评估文档。' : '',
    cacheNeedsAction ? 'KB 检索缓存命中率偏低，会影响生成和审核响应速度。' : '',
    infraSeverity !== 'success' ? '基础设施健康检查存在未上报或异常组件。' : '',
  ].filter(Boolean)

  const kpiCards = [
    { title: '今日AI调用', value: Number(stats?.todayCalls ?? 0).toLocaleString(), unit: '次', color: 'primary.main', icon: <SpeedIcon /> },
    { title: '本月 Token 消耗', value: formatTokens(monthTokens), unit: 'tokens', color: 'secondary.main', icon: <StorageIcon /> },
    { title: 'AI调用成功率', value: `${successRate.toFixed(1)}%`, color: successRate >= 95 ? 'success.main' : 'warning.main', icon: <TrendIcon /> },
    {
      title: '知识库质量均分',
      value: qualityScore.toFixed(1),
      unit: '/100',
      color: qualityScore >= 60 && qualityCoverage >= 80 ? 'success.main' : 'warning.main',
      icon: qualityScore >= 60 && qualityCoverage >= 80 ? <StorageIcon /> : <WarningIcon />,
      description: qualityDescription,
    },
  ]

  const handleRefresh = () => {
    setIsRefreshing(true)
    setQualityRescanError('')
    void Promise.allSettled([
      refetchStats(),
      refetchTrend(),
      refetchDist(),
      refetchQuota(),
      refetchCost(),
      refetchHealth(),
      refetchCache(),
      refetchCacheDiagnostics(),
      refetchSearch(),
      refetchInfraDetail(),
      refetchKbList(),
      refetchRecentCallLogs(),
    ]).finally(() => setIsRefreshing(false))
  }

  const handleRescanQuality = () => {
    setIsRescanningQuality(true)
    setQualityRescanError('')
    void aiApi.dashboardKbQualityRescan({ limit: 2000 })
      .then((result) => {
        enqueueSnackbar(result.completed ? '知识库质量评估已全部完成' : `已重评估 ${result.updated.toLocaleString()} 条知识文档`, { variant: 'success' })
        void refetchStats()
      })
      .catch((error) => {
        const message = `知识库质量重评估失败：${getErrorMessage(error)}。来源：/ai/admin/dashboard/kb-quality/rescan。`
        setQualityRescanError(message)
        enqueueSnackbar('知识库质量重评估失败', { variant: 'error' })
      })
      .finally(() => setIsRescanningQuality(false))
  }

  const pg = detail?.postgresql
  const mv = detail?.milvus
  const esd = detail?.elasticsearch

  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}
      data-testid="ai-dashboard-workbench"
      data-ready-endpoints={AI_DASHBOARD_READY_ENDPOINTS}
      data-unsupported-endpoints={AI_DASHBOARD_UNSUPPORTED_ENDPOINTS}
      data-no-local-dashboard-fallback="true"
      data-no-client-quality-score-synthesis="true"
      data-no-automatic-cache-ttl-mutation="true"
      data-no-cache-flush-action="true"
    >
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', md: 'center' }} gap={1.25}>
        <Box>
          <Typography variant="h5" fontWeight={700}>AI 总控首页</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
            确认模型、知识库、官方规则引用、业务链路和复盘回流是否真正形成抖音运营 AI 闭环
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center" sx={{ justifyContent: { xs: 'flex-start', md: 'flex-end' } }}>
          <FormControl size="small" sx={{ minWidth: 110 }}>
            <InputLabel>分析周期</InputLabel>
            <Select value={timeRange} label="分析周期" onChange={e => setTimeRange(Number(e.target.value))}>
              <MenuItem value={1}>今日</MenuItem>
              <MenuItem value={7}>近7天</MenuItem>
              <MenuItem value={30}>近30天</MenuItem>
            </Select>
          </FormControl>
          <Button size="small" startIcon={<RefreshIcon />} onClick={handleRefresh} disabled={isRefreshing}>
            {isRefreshing ? '刷新中' : '刷新'}
          </Button>
          <Button size="small" variant="outlined" onClick={handleRescanQuality} disabled={isRescanningQuality}>
            {isRescanningQuality ? '评估中' : '重评估知识质量'}
          </Button>
          {typeof grafanaUrl === 'string' && grafanaUrl.startsWith('http') && (
            <Button size="small" endIcon={<OpenInNewIcon />} href={grafanaUrl} target="_blank" rel="noopener noreferrer">
              Grafana
            </Button>
          )}
        </Stack>
      </Stack>

      {isStatsError && (
        <Alert
          severity="error"
          data-testid="ai-dashboard-stats-error"
          data-source-endpoint="/ai/admin/dashboard/stats"
          data-no-local-dashboard-fallback="true"
          data-no-client-quality-score-synthesis="true"
        >
          AI 仪表盘统计加载失败：{getErrorMessage(statsError)}。来源：/ai/admin/dashboard/stats；KPI 已按空值降级，不伪造质量分。
        </Alert>
      )}
      {qualityRescanError && (
        <Alert
          severity="error"
          onClose={() => setQualityRescanError('')}
          data-testid="ai-dashboard-quality-rescan-error"
          data-source-endpoint="/ai/admin/dashboard/kb-quality/rescan"
          data-no-local-quality-score-mutation="true"
        >
          {qualityRescanError}
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Card
            variant="outlined"
            sx={{ height: '100%', borderColor: `${chainStatusColor(aiOperatingStatus)}.main`, bgcolor: `${chainStatusColor(aiOperatingStatus)}.50` }}
            data-testid="ai-dashboard-command-center"
            data-source-endpoints="/ai/admin/dashboard/stats|/ai/knowledge-base/list|/ai/admin/call-log/search"
            data-no-static-operating-status="true"
          >
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Stack spacing={1.25}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                  <Typography variant="subtitle1" fontWeight={700}>总控状态</Typography>
                  <Chip color={chainStatusColor(aiOperatingStatus)} label={aiOperatingLabel} />
                </Stack>
                <Typography variant="h4" fontWeight={800} color={`${chainStatusColor(aiOperatingStatus)}.main`}>
                  {gateReadyChainCount}/{gateEnabledChainCount}
                  <Typography component="span" variant="body2" color="text.secondary" ml={0.75}>强门禁链路就绪</Typography>
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  当前分开展示后端强门禁能力和近期调用引用验证；没有近期业务输出调用时显示未上报，不再误判为未接入。
                </Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  <Chip
                    size="small"
                    color={monitoringCfg?.creditEnforce ? 'success' : 'warning'}
                    variant={monitoringCfg?.creditEnforce ? 'filled' : 'outlined'}
                    label={monitoringCfg?.creditEnforce ? '积分 enforce 已开启' : '积分 enforce 关闭（演示）'}
                    data-testid="ai-dashboard-credit-enforce"
                    data-source-endpoint="/ai/admin/infra/monitoring-config"
                  />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={monitoringCfg?.allowHeaderIdentity === false ? 'JWT-only 身份' : 'Header 身份联调'}
                    data-testid="ai-dashboard-header-identity"
                    data-source-endpoint="/ai/admin/infra/monitoring-config"
                  />
                  <Chip size="small" variant="outlined" label={`引用覆盖 ${totalRecentChainCalls > 0 ? `${overallReferenceCoverage.toFixed(0)}%` : '未上报'}`} />
                  <Chip size="small" variant="outlined" label={`知识资产 ${readyAssetCount}/${assetGroups.length}`} />
                  <Chip size="small" variant="outlined" label={`近期调用 ${totalRecentChainCalls}`} />
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        {controlCards.map(card => (
          <Grid item xs={12} sm={4} md={8 / 3} key={card.title}>
            <KpiCard
              title={card.title}
              value={card.value}
              color={`${card.color}.main`}
              icon={card.icon}
              description={card.description}
            />
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        {kpiCards.map(k => (
          <Grid item xs={12} sm={6} md={3} key={k.title}>
            <KpiCard title={k.title} value={k.value} unit={k.unit} color={k.color} icon={k.icon} description={k.description} />
          </Grid>
        ))}
      </Grid>

      {qualityNeedsAction && (
        <Alert
          severity="warning"
          data-testid="ai-dashboard-quality-action-advice"
          data-source-endpoint="/ai/admin/dashboard/stats"
          data-no-client-quality-score-synthesis="true"
          data-no-local-quality-score-mutation="true"
          data-quality-score={qualityScore.toFixed(1)}
          data-quality-coverage={qualityCoverage.toFixed(1)}
        >
          <Typography variant="body2" fontWeight={700} sx={{ mb: 0.75 }}>
            知识库质量分偏低的处理顺序
          </Typography>
          <Stack spacing={0.5}>
            {qualityActionHints.map((hint) => (
              <Typography key={hint} variant="body2">{hint}</Typography>
            ))}
          </Stack>
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <SectionCard
            title="下一步动作"
            action={<Button size="small" component={RouterLink} to="/admin/ai/call-log">查日志</Button>}
          >
            <Stack
              spacing={1}
              data-testid="ai-dashboard-action-center"
              data-source-endpoints="/ai/admin/dashboard/stats|/ai/knowledge-base/list|/ai/admin/call-log/search|/ai/admin/infra/health"
              data-no-local-action-synthesis="true"
            >
              {actionItems.length === 0 ? (
                <Alert severity="success" sx={{ py: 0.75 }}>核心 AI 链路暂无阻断项，可继续观察引用覆盖率和业务复盘回流。</Alert>
              ) : actionItems.slice(0, 5).map((item, index) => (
                <Alert key={`${item}-${index}`} severity={index === 0 ? 'warning' : 'info'} sx={{ py: 0.75 }}>
                  {item}
                </Alert>
              ))}
            </Stack>
          </SectionCard>
        </Grid>
        <Grid item xs={12} md={8}>
          <SectionCard
            title="业务链路 AI 接入看板"
            action={<Button size="small" component={RouterLink} to="/admin/ai/knowledge-search">验证检索</Button>}
          >
            <Grid
              container
              spacing={1.5}
              data-testid="ai-dashboard-business-chain-board"
              data-source-endpoints="/ai/admin/call-log/search|/ai/knowledge-base/list"
              data-no-static-chain-status="true"
            >
              {businessChains.map(chain => (
                <Grid item xs={12} sm={6} key={chain.key}>
                  <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.25, height: '100%' }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="subtitle2" fontWeight={700}>{chain.title}</Typography>
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                          {chain.description}
                        </Typography>
                      </Box>
                      <Chip size="small" label={chainStatusLabel(chain.status)} color={chainStatusColor(chain.status)} />
                    </Stack>
                    {chain.mustBlock && (
                      <Alert severity="error" sx={{ mt: 1, py: 0.5 }}>
                        官方规则库未就绪，此链路应阻断放行，先补知识库或修复 RAG 引用。
                      </Alert>
                    )}
                    {chain.gateReady && (
                      <Alert severity={chain.logs.length > 0 ? 'success' : 'info'} sx={{ mt: 1, py: 0.5 }}>
                        {chain.gateDescription}{chain.logs.length === 0 ? ' 当前暂无近期调用样本。' : ''}
                      </Alert>
                    )}
                    <Grid container spacing={1} sx={{ mt: 1 }}>
                      <Grid item xs={4}>
                        <Typography variant="caption" color="text.secondary">近期调用</Typography>
                        <Typography variant="body2" fontWeight={700}>{chain.logs.length}</Typography>
                      </Grid>
                      <Grid item xs={4}>
                        <Typography variant="caption" color="text.secondary">引用覆盖</Typography>
                        <Typography variant="body2" fontWeight={700}>{chain.logs.length > 0 ? `${chain.referenceCoverage.toFixed(0)}%` : '未上报'}</Typography>
                      </Grid>
                      <Grid item xs={4}>
                        <Typography variant="caption" color="text.secondary">门禁/知识</Typography>
                        <Typography variant="body2" fontWeight={700}>{`${chain.kbCoverage.toFixed(0)}%`}</Typography>
                      </Grid>
                    </Grid>
                    <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                      {chain.requiredKbNames.map(name => (
                        <Chip
                          key={name}
                          size="small"
                          variant="outlined"
                          color={chain.requiredKbReady.includes(name) ? 'success' : 'warning'}
                          label={name}
                        />
                      ))}
                    </Stack>
                    {chain.requiredKbReady.length < chain.requiredKbNames.length && (
                      <Typography variant="caption" color="warning.main" sx={{ display: 'block', mt: 0.75 }}>
                        缺失：{chain.requiredKbNames.filter(name => !chain.requiredKbReady.includes(name)).join('、')}
                      </Typography>
                    )}
                    {chain.failures > 0 && (
                      <Typography variant="caption" color="warning.main" sx={{ display: 'block', mt: 0.75 }}>
                        最近失败 {chain.failures} 次，请查看调用日志。
                      </Typography>
                    )}
                    <Link component={RouterLink} to={chain.actionPath} variant="caption" sx={{ display: 'inline-block', mt: 0.75 }}>
                      打开业务入口
                    </Link>
                  </Box>
                </Grid>
              ))}
            </Grid>
          </SectionCard>
        </Grid>
      </Grid>

      <SectionCard
        title="知识资产中心"
        action={<Button size="small" component={RouterLink} to="/admin/ai/knowledge">管理知识库</Button>}
      >
        {isKbListError ? (
          <Alert
            severity="error"
            data-testid="ai-dashboard-kb-assets-error"
            data-source-endpoint="/ai/knowledge-base/list"
          >
            知识资产加载失败：{getErrorMessage(kbListError)}。页面不会用本地清单伪造知识库状态。
          </Alert>
        ) : (
          <Grid
            container
            spacing={1.5}
            data-testid="ai-dashboard-knowledge-assets"
            data-source-endpoint="/ai/knowledge-base/list"
            data-no-static-kb-assets="true"
          >
            {assetGroups.map(group => (
              <Grid item xs={12} sm={6} md={2.4} key={group.key}>
                <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.25, height: '100%' }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                    <Typography variant="subtitle2" fontWeight={700}>{group.title}</Typography>
                    <Chip size="small" label={chainStatusLabel(group.status)} color={chainStatusColor(group.status)} />
                  </Stack>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5, minHeight: 36 }}>
                    {group.description}
                  </Typography>
                  <Stack spacing={0.5} sx={{ mt: 1 }}>
                    <Typography variant="caption" color="text.secondary">文档 {group.documents.toLocaleString()} · Token {formatTokens(group.tokens)}</Typography>
                    <Typography variant="caption" color="text.secondary">可用知识库 {group.ready}/{group.matched.length}</Typography>
                    <Typography variant="caption" color={group.matched.length > 0 ? 'text.secondary' : 'warning.main'} sx={{ minHeight: 18 }}>
                      {group.matched.length > 0 ? group.matched.map(kb => kb.kbName).join('、') : `缺少 ${group.aliases[0]}`}
                    </Typography>
                    <Link component={RouterLink} to={group.actionPath} variant="caption">查看</Link>
                  </Stack>
                </Box>
              </Grid>
            ))}
          </Grid>
        )}
      </SectionCard>

      {isRecentCallLogsError && (
        <Alert
          severity="error"
          data-testid="ai-dashboard-chain-log-error"
          data-source-endpoint="/ai/admin/call-log/search"
        >
          业务链路调用日志加载失败：{getErrorMessage(recentCallLogsError)}。引用覆盖率不会在前端估算。
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={8}>
          <SectionCard
            title={`调用量趋势${trendGranularity === 'hour' ? '（按小时）' : '（按日）'}${trendCallType ? ` · ${CALL_TYPE_LABELS[trendCallType] ?? trendCallType}` : ''}`}
            action={
              <Stack direction="row" spacing={1} alignItems="center" sx={{ display: { xs: 'none', sm: 'flex' } }}>
                <ToggleButtonGroup
                  size="small"
                  value={trendGranularity}
                  exclusive
                  onChange={(_, v) => v != null && setTrendGranularity(v)}
                >
                  <ToggleButton value="day">按日</ToggleButton>
                  <ToggleButton value="hour">按小时</ToggleButton>
                </ToggleButtonGroup>
                <FormControl size="small" sx={{ minWidth: 132 }}>
                  <InputLabel>调用类型</InputLabel>
                  <Select
                    data-testid="ai-dashboard-trend-call-type-select"
                    value={trendCallType}
                    label="调用类型"
                    onChange={e => setTrendCallType(e.target.value)}
                  >
                    <MenuItem value="">全部类型</MenuItem>
                    {Object.entries(CALL_TYPE_LABELS).map(([k, label]) => (
                      <MenuItem key={k} value={k}>{label}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Stack>
            }
          >
            <Stack direction="row" spacing={1} alignItems="center" sx={{ display: { xs: 'flex', sm: 'none' }, mb: 1, overflowX: 'auto', pb: 0.5 }}>
              <ToggleButtonGroup
                size="small"
                value={trendGranularity}
                exclusive
                onChange={(_, v) => v != null && setTrendGranularity(v)}
              >
                <ToggleButton value="day">按日</ToggleButton>
                <ToggleButton value="hour">按小时</ToggleButton>
              </ToggleButtonGroup>
              <FormControl size="small" sx={{ minWidth: 132 }}>
                <InputLabel>调用类型</InputLabel>
                <Select
                  data-testid="ai-dashboard-trend-call-type-select-mobile"
                  value={trendCallType}
                  label="调用类型"
                  onChange={e => setTrendCallType(e.target.value)}
                >
                  <MenuItem value="">全部类型</MenuItem>
                  {Object.entries(CALL_TYPE_LABELS).map(([k, label]) => (
                    <MenuItem key={k} value={k}>{label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
              {isTrendError
                ? <Alert severity="error">调用量趋势加载失败：{getErrorMessage(trendError)}。来源：/ai/admin/dashboard/call-volume-trend。</Alert>
                : trendArr.length === 0
                ? <Alert severity="info">暂无趋势数据</Alert>
                : <ReactECharts option={trendOption} style={{ height: 280 }} />}
          </SectionCard>
        </Grid>
        <Grid item xs={12} md={4}>
          <SectionCard title="调用类型分布">
            {isDistError
              ? <Alert severity="error">调用类型分布加载失败：{getErrorMessage(distError)}。来源：/ai/admin/dashboard/call-type-distribution。</Alert>
              : distArr.length === 0
              ? <Alert severity="info">暂无分布数据</Alert>
              : <ReactECharts option={distOption} style={{ height: 280 }} />}
          </SectionCard>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} md={5}>
          <SectionCard
            title="基础设施状态"
            action={<Button size="small" component={RouterLink} to="/admin/ai/admin-infra" variant="outlined">详情</Button>}
          >
            <Stack
              direction="row"
              spacing={1}
              alignItems="center"
              sx={{ mb: 1.5 }}
              data-testid="ai-dashboard-infra-health-contract"
              data-source-endpoint="/ai/admin/infra/health"
              data-no-local-health-fallback="true"
              data-reported-count={String(infraItems.length)}
            >
              <Chip
                size="small"
                label={`${infraOkCount}/${infraStatusItems.length} 正常`}
                color={infraSeverity}
                icon={infraSeverity === 'success' ? <CheckIcon /> : <ErrorIcon />}
              />
              {cacheWarn && <Chip size="small" color="warning" icon={<WarningIcon />} label={`业务缓存 ${formatPercent(businessCacheHitRate)}%`} />}
            </Stack>
            <Grid container spacing={1}>
              {infraStatusItems.map(item => (
                <Grid item xs={6} key={item.key}>
                  <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, px: 1, py: 0.75 }}>
                    <Stack direction="row" spacing={0.75} alignItems="center">
                      {item.ok ? <CheckIcon color="success" fontSize="small" /> : <ErrorIcon color="warning" fontSize="small" />}
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="caption" color="text.secondary" display="block">{item.name}</Typography>
                        <Typography variant="body2" fontWeight={700} noWrap color={item.ok ? 'success.main' : 'warning.main'}>{item.name === 'Elasticsearch' && item.ok && esd?.status ? String(esd.status) : item.statusText}</Typography>
                      </Box>
                    </Stack>
                  </Box>
                </Grid>
              ))}
            </Grid>
            {isInfraHealthError && (
              <Alert severity="error" sx={{ mt: 1.5 }}>基础设施健康检查接口加载失败，请稍后重试。</Alert>
            )}
            {!isInfraHealthError && infraItems.length === 0 && (
              <Alert
                severity="info"
                sx={{ mt: 1.5 }}
                data-testid="ai-dashboard-infra-unreported"
                data-no-local-health-fallback="true"
                data-no-disconnected-inference="true"
              >
                健康检查接口未返回组件明细，已按“未上报”展示；请确认后端监控采集是否启用。
              </Alert>
            )}
            {detail && (
              <>
                <Divider sx={{ my: 1.5 }} />
                <Grid container spacing={1.5}>
                  <Grid item xs={12} sm={4}>
                    <Typography variant="caption" color="text.secondary">PostgreSQL</Typography>
                    <Typography variant="body2">{pg?.ok ? `连接 ${String(pg.poolActive ?? '—')}/${String(pg.poolTotal ?? '—')} · ${String(pg.responseMs ?? '—')}ms` : String(pg?.error ?? '—')}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Typography variant="caption" color="text.secondary">Milvus</Typography>
                    <Typography variant="body2">{mv?.ok ? `向量 ${Number(mv.totalVectors ?? 0).toLocaleString()} · 集合 ${String(mv.collectionCount ?? '—')}` : String(mv?.error ?? '—')}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Typography variant="caption" color="text.secondary">Elasticsearch</Typography>
                    <Typography variant="body2">{esd?.ok ? `${String(esd.clusterName ?? '—')}` : String(esd?.error ?? '—')}</Typography>
                  </Grid>
                </Grid>
              </>
            )}
            {isInfraDetailError && (
              <Alert severity="error" sx={{ mt: 1.5 }}>
                基础设施详情加载失败：{getErrorMessage(infraDetailError)}。来源：/ai/admin/infra/detail。
              </Alert>
            )}
            {Array.isArray(detail?.suggestions) && detail.suggestions.length > 0 && (
              <Alert severity="warning" sx={{ mt: 1.5 }}>
                {detail.suggestions.slice(0, 2).map(s => `${String(s.component ?? '')}${s.message != null ? `：${String(s.message)}` : ''}`).join('；')}
              </Alert>
            )}
          </SectionCard>
        </Grid>
        <Grid item xs={12} md={7}>
          <SectionCard title="检索与缓存诊断">
            {isSearchError ? (
              <Alert severity="error">检索统计加载失败：{getErrorMessage(searchError)}。来源：/ai/admin/infra/search/stats。</Alert>
            ) : search?.message != null ? (
              <Alert severity="info">{String(search.message)}</Alert>
            ) : (
              <Grid
                container
                spacing={2}
                data-testid="ai-dashboard-search-stats-contract"
                data-source-endpoint="/ai/admin/infra/search/stats"
                data-no-local-search-metric-fallback="true"
              >
                {([
                  ['总请求', search?.totalRequests],
                  ['估算 QPS', search?.qps],
                  ['P50', search?.p50Ms == null ? undefined : `${String(search.p50Ms)}ms`],
                  ['P95', Number.isFinite(p95Ms) && p95Ms > 0 ? `${p95Ms}ms` : search?.p95Ms],
                  ['P99', Number.isFinite(p99Ms) && p99Ms > 0 ? `${p99Ms}ms` : search?.p99Ms],
                  ['采样数', search?.sampleCount],
                ] as const satisfies ReadonlyArray<readonly [string, unknown]>).map(([k, v]) => (
                  <Grid item xs={6} sm={4} md={2} key={k}>
                    <Typography variant="caption" color="text.secondary" display="block">{k}</Typography>
                    <Typography fontWeight={700} color={(k === 'P95' && p95Ms > 250) || (k === 'P99' && p99Ms > 500) ? 'warning.main' : 'text.primary'}>{v == null ? '—' : String(v)}</Typography>
                  </Grid>
                ))}
              </Grid>
            )}
            {cache && (
              <Box
                sx={{ mt: 2 }}
                data-testid="ai-dashboard-business-cache-contract"
                data-source-endpoint="/ai/admin/infra/cache/stats"
                data-hit-rate={formatPercent(businessCacheHitRate)}
                data-no-automatic-cache-ttl-mutation="true"
                data-no-cache-prewarm-action="true"
              >
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 0.75 }}>
                  <Typography variant="body2" fontWeight={700}>KB 业务缓存命中率</Typography>
                  <Typography variant="body2" fontWeight={700} color={cacheWarn ? 'warning.main' : 'success.main'}>{formatPercent(businessCacheHitRate)}%</Typography>
                </Stack>
                <LinearProgress variant="determinate" value={Math.min(Math.max(businessCacheHitRate, 0), 100)} color={cacheWarn ? 'warning' : 'success'} />
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.75, display: 'block' }}>
                  命中 {Number(cache.hit ?? 0).toLocaleString()} / 总数 {Number(cache.total ?? 0).toLocaleString()}，Key {Number(cache.keyCount ?? 0).toLocaleString()}
                </Typography>
              </Box>
            )}
            {isCacheStatsError && (
              <Alert severity="error" sx={{ mt: 1.5 }}>
                缓存统计加载失败：{getErrorMessage(cacheStatsError)}。来源：/ai/admin/infra/cache/stats。
              </Alert>
            )}
            {cacheDiag && (
              <Box
                sx={{ mt: 2 }}
                data-testid="ai-dashboard-cache-diagnostics-contract"
                data-source-endpoint="/ai/admin/infra/cache/diagnostics"
                data-no-automatic-cache-ttl-mutation="true"
                data-no-cache-flush-action="true"
                data-no-mock-hot-key-injection="true"
              >
                <Grid container spacing={1.5}>
                  {([
                    ['Redis 全局命中率', redisGlobalHitRate > 0 ? `${formatPercent(redisGlobalHitRate)}%` : '—'],
                    ['Redis Key 总数', cacheDiag.dbSize != null ? Number(cacheDiag.dbSize).toLocaleString() : '—'],
                    ['KB TTL', formatSeconds(cacheDiag.kbCacheTtlSeconds)],
                    ['Embedding TTL', cacheDiag.embeddingCacheTtlDays != null ? `${String(cacheDiag.embeddingCacheTtlDays)} 天` : '—'],
                  ] as const).map(([label, value]) => (
                    <Grid item xs={6} sm={3} key={label}>
                      <Typography variant="caption" color="text.secondary" display="block">{label}</Typography>
                      <Typography variant="body2" fontWeight={700}>{value}</Typography>
                    </Grid>
                  ))}
                </Grid>

                {cacheDiag.redisStats && (
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1.25 }}>
                    <Chip size="small" variant="outlined" label={`Redis hit ${Number(cacheDiag.redisStats.keyspaceHits ?? 0).toLocaleString()}`} />
                    <Chip size="small" variant="outlined" label={`Redis miss ${Number(cacheDiag.redisStats.keyspaceMisses ?? 0).toLocaleString()}`} />
                    <Chip size="small" variant="outlined" label={`过期 ${Number(cacheDiag.redisStats.expiredKeys ?? 0).toLocaleString()}`} />
                    <Chip size="small" color={Number(cacheDiag.redisStats.evictedKeys ?? 0) > 0 ? 'warning' : 'default'} variant="outlined" label={`淘汰 ${Number(cacheDiag.redisStats.evictedKeys ?? 0).toLocaleString()}`} />
                    {cacheDiag.redisStats.usedMemoryHuman && <Chip size="small" variant="outlined" label={`内存 ${cacheDiag.redisStats.usedMemoryHuman}`} />}
                  </Stack>
                )}

                {cacheDiag.scan?.prefixCounts && Object.keys(cacheDiag.scan.prefixCounts).length > 0 && (
                  <Box sx={{ mt: 1.5 }}>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
                      Key 前缀分布（SCAN 抽样 {Number(cacheDiag.scan.scanned ?? 0).toLocaleString()}）
                    </Typography>
                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                      {Object.entries(cacheDiag.scan.prefixCounts).slice(0, 8).map(([prefix, count]) => (
                        <Chip key={prefix} size="small" label={`${prefix} ${Number(count).toLocaleString()}`} />
                      ))}
                    </Stack>
                  </Box>
                )}

                {cacheDiag.scan?.ttlBuckets && Object.keys(cacheDiag.scan.ttlBuckets).length > 0 && (
                  <Box sx={{ mt: 1.5 }}>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>TTL 分布</Typography>
                    <Grid container spacing={1}>
                      {Object.entries(cacheDiag.scan.ttlBuckets).map(([bucket, count]) => (
                        <Grid item xs={6} sm={4} md={2} key={bucket}>
                          <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, px: 1, py: 0.75 }}>
                            <Typography variant="caption" color="text.secondary" display="block">{bucketLabel(bucket)}</Typography>
                            <Typography variant="body2" fontWeight={700}>{Number(count).toLocaleString()}</Typography>
                          </Box>
                        </Grid>
                      ))}
                    </Grid>
                  </Box>
                )}

                {Array.isArray(cacheDiag.suggestions) && cacheDiag.suggestions.length > 0 && (
                  <Alert severity={cacheWarn ? 'warning' : 'info'} sx={{ mt: 1.5 }}>
                    {cacheDiag.suggestions.slice(0, 3).join('；')}
                  </Alert>
                )}
              </Box>
            )}
            {isCacheDiagnosticsError && (
              <Alert severity="error" sx={{ mt: 1.5 }}>
                缓存诊断加载失败：{getErrorMessage(cacheDiagnosticsError)}。来源：/ai/admin/infra/cache/diagnostics。
              </Alert>
            )}
            {cacheActionHints.length > 0 && (
              <Alert
                severity="warning"
                sx={{ mt: 1.5 }}
                data-testid="ai-dashboard-cache-action-advice"
                data-source-endpoint="/ai/admin/infra/cache/diagnostics"
                data-no-automatic-cache-ttl-mutation="true"
                data-no-cache-prewarm-action="true"
              >
                <Typography variant="body2" fontWeight={700} sx={{ mb: 0.75 }}>Redis 命中率优化动作</Typography>
                <Stack spacing={0.5}>
                  {cacheActionHints.map((hint) => (
                    <Typography key={hint} variant="body2">{hint}</Typography>
                  ))}
                </Stack>
              </Alert>
            )}
          </SectionCard>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} md={7}>
          <SectionCard
            title="额度使用趋势"
            action={<Button size="small" component={RouterLink} to="/admin/ai/quota">配额管理</Button>}
          >
            {isQuotaError
              ? <Alert severity="error">额度趋势加载失败：{getErrorMessage(quotaError)}。来源：/ai/admin/dashboard/quota-trend。</Alert>
              : quotaArr.length === 0
              ? <Alert severity="info">暂无额度流水数据（ai_call_quota）</Alert>
              : <ReactECharts option={quotaOption} style={{ height: 300 }} />}
          </SectionCard>
        </Grid>
        <Grid item xs={12} md={5}>
          <SectionCard
            title="Token 用量拆解"
            action={<Button size="small" component={RouterLink} to="/admin/ai/call-log">调用日志</Button>}
          >
            {isCostError
              ? <Alert severity="error">Token 用量拆解加载失败：{getErrorMessage(costError)}。来源：/ai/admin/dashboard/cost-breakdown。</Alert>
              : costArr.length === 0
              ? <Alert severity="info">暂无 Token 汇总（需调用日志含 total_tokens 或估算值）</Alert>
              : <ReactECharts option={costPieOption} style={{ height: 300 }} />}
          </SectionCard>
        </Grid>
      </Grid>

      <Accordion disableGutters>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="subtitle1" fontWeight={700}>成本与缓存明细</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            {costArr.length > 0 && (
              <Grid item xs={12} md={8}>
                <Typography variant="body2" fontWeight={700} sx={{ mb: 1 }}>Token 与调用次数（按类型）</Typography>
                <ReactECharts option={costBarOption} style={{ height: 300 }} />
              </Grid>
            )}
            <Grid item xs={12} md={costArr.length > 0 ? 4 : 12}>
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
                {cache && Object.entries({
                  命中: cache.hit,
                  未命中: cache.miss,
                  总数: cache.total,
                  'KB业务命中率': `${formatPercent(businessCacheHitRate)}%`,
                  'Redis全局命中率': redisGlobalHitRate > 0 ? `${formatPercent(redisGlobalHitRate)}%` : '—',
                  Key数: cache.keyCount,
                }).map(([k, v]) => (
                  <Stack key={k} direction="row" justifyContent="space-between">
                    <Typography variant="body2" color="text.secondary">{k}</Typography>
                    <Typography variant="body2" fontWeight={600}>{v == null ? '—' : String(v)}</Typography>
                  </Stack>
                ))}
              </Stack>
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>

      <Typography variant="caption" color="text.secondary">
        说明：Token 拆解来自成功调用日志汇总；知识检索类可能为估算值。
        更细的 PG/ES/Milvus 分页与索引队列见
        <Link component={RouterLink} to="/admin/ai/admin-infra" ml={0.5}>AI 基础设施</Link>
        。
      </Typography>
    </Box>
  )
}
