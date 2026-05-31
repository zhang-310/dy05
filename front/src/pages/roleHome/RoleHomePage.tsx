import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Grid,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import BusinessIcon from '@mui/icons-material/Business'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import GroupIcon from '@mui/icons-material/Group'
import LiveTvIcon from '@mui/icons-material/LiveTv'
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart'
import PsychologyIcon from '@mui/icons-material/Psychology'
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import { useNavigate } from 'react-router-dom'
import {
  roleHomeApi,
  type RoleHomeBusinessChain,
  type RoleHomeData,
  type RoleHomeKind,
  type RoleHomeLearningLoop,
} from '@/api/role-home'
import { getErrorMessage } from '@/utils/errorHandler'

interface RoleHomePageProps {
  role: RoleHomeKind
}

interface MetricItem {
  label: string
  value: string
  hint?: string
  icon: ReactNode
}

interface ActionItem {
  label: string
  path: string
  icon: ReactNode
  desc: string
}

interface RoleConfig {
  title: string
  subtitle: string
  badge: string
  shellPath: string
  readyEndpoints: string[]
  actions: ActionItem[]
}

const roleConfigs: Record<RoleHomeKind, RoleConfig> = {
  admin: {
    title: '平台总控台',
    subtitle: '负责 AI 中心、知识库、系统配置、支付和全局审计，不承载机构/达人业务操作。',
    badge: 'admin-console',
    shellPath: '/admin',
    readyEndpoints: ['/api/v1/admin/home', '/dashboard/admin/stats', '/ai/admin/dashboard/stats', '/monitoring/alerts/active'],
    actions: [
      { label: 'AI 总控', path: '/admin/ai/dashboard', icon: <PsychologyIcon />, desc: '查看模型、知识库、索引和门禁状态。' },
      { label: '系统监控', path: '/admin/system', icon: <MonitorHeartIcon />, desc: '检查服务、日志、告警和配置健康度。' },
      { label: '资源权限', path: '/admin/auth/resources', icon: <FactCheckIcon />, desc: '管理菜单、接口和角色授权。' },
      { label: '天API面板', path: '/admin/system/tianapi', icon: <AutoAwesomeIcon />, desc: '核对每日采集额度和失败补采。' },
    ],
  },
  org: {
    title: '机构经营台',
    subtitle: '围绕机构成员、商品、直播场次、话术、复盘和内容资产开展经营管理。',
    badge: 'institution-console',
    shellPath: '/org',
    readyEndpoints: ['/api/v1/org/home', '/dashboard/org/stats', '/live/session/search', '/live/approval/pending'],
    actions: [
      { label: '直播场次', path: '/org/live/sessions', icon: <LiveTvIcon />, desc: '管理排期、脚本、排品和复盘。' },
      { label: '商品管理', path: '/org/product/list', icon: <ShoppingBagIcon />, desc: '维护商品卖点、素材和上播准备度。' },
      { label: '话术脚本', path: '/org/script/list', icon: <PsychologyIcon />, desc: '沉淀机构话术库和合规表达。' },
      { label: '成员管理', path: '/org/members', icon: <GroupIcon />, desc: '管理机构成员和协作权限。' },
    ],
  },
  talent: {
    title: '达人创作台',
    subtitle: '聚焦抖音账号、短视频项目、爆款采集、对标分析和直播执行。',
    badge: 'creator-studio',
    shellPath: '/talent',
    readyEndpoints: ['/api/v1/talent/home', '/dashboard/org/stats', '/short-video/project/list', '/short-video/dashboard/stats'],
    actions: [
      { label: '短视频项目', path: '/talent/shortvideo', icon: <VideoLibraryIcon />, desc: '策划、分镜、素材、生成和发布。' },
      { label: '账号采集', path: '/talent/shortvideo/collect', icon: <AutoAwesomeIcon />, desc: '采集对标账号和热门视频。' },
      { label: '抖音账号', path: '/talent/douyin/accounts', icon: <BusinessIcon />, desc: '管理授权账号和基础数据。' },
      { label: '直播场次', path: '/talent/live/sessions', icon: <LiveTvIcon />, desc: '进入达人直播执行工作台。' },
    ],
  },
  user: {
    title: '个人创作台',
    subtitle: '面向普通用户的轻量短视频创作、个人项目、发布检查和个人资产。',
    badge: 'user-studio',
    shellPath: '/user',
    readyEndpoints: ['/api/v1/user/home', '/short-video/project/list', '/short-video/project/save', '/short-video/publish/ai-review'],
    actions: [
      { label: '我的短视频', path: '/user/shortvideo', icon: <VideoLibraryIcon />, desc: '查看个人项目和创作进度。' },
      { label: 'AI 创作', path: '/user/shortvideo/create', icon: <AutoAwesomeIcon />, desc: '创建个人短视频草稿。' },
      { label: '脚本策划', path: '/user/shortvideo/planning', icon: <PsychologyIcon />, desc: '完善口播结构和镜头节奏。' },
      { label: '发布检查', path: '/user/shortvideo/publish', icon: <FactCheckIcon />, desc: '发布前检查违规风险和官方引用。' },
    ],
  },
}

function fmtNumber(value: unknown): string {
  const n = Number(value ?? 0)
  if (!Number.isFinite(n)) return '0'
  if (Math.abs(n) >= 10000) return `${(n / 10000).toFixed(1)}万`
  return n.toLocaleString('zh-CN')
}

function fmtMoney(value: unknown): string {
  const n = Number(value ?? 0)
  if (!Number.isFinite(n)) return '¥0'
  if (Math.abs(n) >= 10000) return `¥${(n / 10000).toFixed(2)}万`
  return `¥${n.toFixed(2)}`
}

function metricNumber(metrics: Record<string, unknown>, ...keys: string[]): unknown {
  for (const key of keys) {
    if (metrics[key] != null) return metrics[key]
  }
  return 0
}

function metricText(metrics: Record<string, unknown>, key: string, fallback: string): string {
  const value = metrics[key]
  return value == null || value === '' ? fallback : String(value)
}

function statusColor(status?: string): 'success' | 'warning' | 'error' | 'default' {
  if (status === 'active' || status === 'completed' || status === 'running') return 'success'
  if (status === 'degraded' || status === 'empty' || status === 'unknown') return 'warning'
  if (status === 'blocked' || status === 'failed') return 'error'
  return 'default'
}

function statusText(status?: string): string {
  const labels: Record<string, string> = {
    active: '已接入',
    blocked: '应阻断',
    degraded: '需处理',
    empty: '暂无数据',
    failed: '失败',
    running: '运行中',
    completed: '已完成',
    unknown: '未知',
  }
  return labels[status ?? ''] ?? (status || '未知')
}

const metricLabels: Record<string, string> = {
  douyinDocs: '抖音文档',
  douyinWeiguiDocs: '违规文档',
  officialSchoolDocs: '官方学习',
  aiCalls7d: 'AI 7天',
  referencedCalls7d: '引用调用',
  officialReferencedCalls7d: '官方引用',
  referenceCoverage7d: '引用率',
  indexPending: '待索引',
  indexRecoverableFailed: '索引失败',
  totalImported: '导入',
  totalCalls: '调用',
  completedCategories: '分类',
  progressPercent: '进度',
  viralVideos: '爆款库',
  deepAnalyzedViralVideos: '已拆解',
  benchmarkVideos: '对标视频',
  benchmarkAnalyzed: '已分析',
  targetKbDocs: '目标库',
  publishedProjects: '已发布',
  reviewedAiCalls7d: '复盘引用',
}

function formatMetricValue(key: string, value: unknown): string {
  if (value == null || value === '') return '-'
  if (key.toLowerCase().includes('percent') || key.toLowerCase().includes('coverage')) {
    const n = Number(value)
    return Number.isFinite(n) ? `${n.toFixed(1)}%` : String(value)
  }
  if (typeof value === 'number') return fmtNumber(value)
  return String(value)
}

function metricEntries(metrics?: Record<string, unknown>, max = 4) {
  return Object.entries(metrics ?? {})
    .filter(([key, value]) => metricLabels[key] && value != null && value !== '')
    .slice(0, max)
    .map(([key, value]) => ({ key, label: metricLabels[key], value: formatMetricValue(key, value) }))
}

function MetricSummary({ metrics, max = 4 }: { metrics?: Record<string, unknown>, max?: number }) {
  const entries = metricEntries(metrics, max)
  if (!entries.length) return null
  return (
    <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
      {entries.map(item => (
        <Chip
          key={item.key}
          size="small"
          variant="outlined"
          label={`${item.label} ${item.value}`}
        />
      ))}
    </Stack>
  )
}

function ownerScopeText(data?: RoleHomeData): string {
  const boundary = data?.boundary
  if (!boundary) return '数据边界读取中'
  if (boundary.unrestricted) return '全局可见'
  const owners = boundary.visibleOwnerIds ?? data?.visibleOwnerIds ?? []
  return owners.length > 0 ? `owner ${owners.join(',')}` : '未返回 owner 范围'
}

function MetricCard({ metric }: { metric: MetricItem }) {
  return (
    <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 1, height: '100%' }}>
      <Stack spacing={1}>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ color: 'text.secondary' }}>
          {metric.icon}
          <Typography variant="caption">{metric.label}</Typography>
        </Stack>
        <Typography variant="h6" fontWeight={700}>{metric.value}</Typography>
        {metric.hint ? <Typography variant="caption" color="text.secondary">{metric.hint}</Typography> : null}
      </Stack>
    </Paper>
  )
}

function ActionCard({ action }: { action: ActionItem }) {
  const navigate = useNavigate()
  return (
    <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 1, height: '100%' }}>
      <Stack spacing={1.25} sx={{ height: '100%' }}>
        <Stack direction="row" spacing={1} alignItems="center">
          <Box sx={{ display: 'flex', color: 'primary.main' }}>{action.icon}</Box>
          <Typography variant="subtitle2" fontWeight={700}>{action.label}</Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ minHeight: 40 }}>
          {action.desc}
        </Typography>
        <Button size="small" variant="outlined" onClick={() => navigate(action.path)} sx={{ mt: 'auto' }}>
          进入
        </Button>
      </Stack>
    </Paper>
  )
}

function splitKnowledge(value: string): string[] {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function KnowledgeGuardPanel({ home, role }: { home?: RoleHomeData, role: RoleHomeKind }) {
  const guard = home?.knowledgeGuard
  const requiredKbCodes = guard?.requiredKbCodes?.length ? guard.requiredKbCodes : ['douyin', 'douyin_weigui']
  const appliesTo = guard?.appliesTo ?? []
  const status = guard?.status
  return (
    <Paper
      variant="outlined"
      sx={{ p: 2, borderRadius: 1, height: '100%' }}
      data-testid={`${role}-knowledge-guard`}
      data-official-reference-required={String(Boolean(guard?.officialReferenceRequired ?? true))}
      data-block-without-official-reference={String(Boolean(guard?.blockWithoutOfficialReference))}
    >
      <Stack spacing={1.5}>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          <FactCheckIcon fontSize="small" color="primary" />
          <Typography variant="subtitle1" fontWeight={700}>知识门禁</Typography>
          <Chip size="small" color={statusColor(status)} variant="outlined" label={statusText(status)} />
          <Chip size="small" color="warning" variant="outlined" label="官方规则引用必需" />
          {guard?.blockWithoutOfficialReference ? (
            <Chip size="small" color="error" variant="outlined" label="缺引用阻断" />
          ) : (
            <Chip size="small" variant="outlined" label="治理观察" />
          )}
        </Stack>
        <Typography variant="body2" color="text.secondary">
          {guard?.message ?? '业务链路必须检索 douyin + douyin_weigui，并在 AI 输出中暴露官方规则引用。'}
        </Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {requiredKbCodes.map(code => (
            <Chip key={code} size="small" label={code} />
          ))}
        </Stack>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {appliesTo.map(item => (
            <Chip key={item} size="small" variant="outlined" label={item} />
          ))}
        </Stack>
        <MetricSummary metrics={guard?.metrics} max={6} />
      </Stack>
    </Paper>
  )
}

function BusinessChainItem({ chain }: { chain: RoleHomeBusinessChain }) {
  const navigate = useNavigate()
  return (
    <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 1, height: '100%' }}>
      <Stack spacing={1.25} sx={{ height: '100%' }}>
        <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
          <Typography variant="subtitle2" fontWeight={700}>{chain.title}</Typography>
          <Chip
            size="small"
            color={statusColor(chain.status)}
            variant="outlined"
            label={statusText(chain.status)}
          />
        </Stack>
        <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
          <Chip
            size="small"
            color={chain.guardRequired ? 'warning' : 'default'}
            variant="outlined"
            label={chain.guardRequired ? '门禁' : '学习'}
          />
          {chain.realTime ? <Chip size="small" variant="outlined" label="实时指标" /> : null}
        </Stack>
        <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
          {splitKnowledge(chain.requiredKnowledge).map(code => (
            <Chip key={`${chain.key}-${code}`} size="small" label={code} />
          ))}
        </Stack>
        <MetricSummary metrics={chain.metrics} />
        <Button size="small" variant="outlined" onClick={() => navigate(chain.path)} sx={{ mt: 'auto', alignSelf: 'flex-start' }}>
          进入链路
        </Button>
      </Stack>
    </Paper>
  )
}

function BusinessChainsPanel({ chains, role }: { chains: RoleHomeBusinessChain[], role: RoleHomeKind }) {
  if (!chains.length) return null
  return (
    <Box data-testid={`${role}-business-chains`}>
      <Stack spacing={1.5}>
        <Stack direction="row" spacing={1} alignItems="center">
          <DashboardIcon fontSize="small" color="primary" />
          <Typography variant="subtitle1" fontWeight={700}>业务链路接入</Typography>
        </Stack>
        <Grid container spacing={2}>
          {chains.map(chain => (
            <Grid key={chain.key} item xs={12} md={4}>
              <BusinessChainItem chain={chain} />
            </Grid>
          ))}
        </Grid>
      </Stack>
    </Box>
  )
}

function LearningLoopsPanel({ loops, role }: { loops: RoleHomeLearningLoop[], role: RoleHomeKind }) {
  if (!loops.length) return null
  return (
    <Box data-testid={`${role}-learning-loops`}>
      <Stack spacing={1.5}>
        <Stack direction="row" spacing={1} alignItems="center">
          <AutoAwesomeIcon fontSize="small" color="primary" />
          <Typography variant="subtitle1" fontWeight={700}>学习回流</Typography>
        </Stack>
        <Grid container spacing={2}>
          {loops.map(loop => (
            <Grid key={loop.key} item xs={12} md={4}>
              <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 1, height: '100%' }}>
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                    <Typography variant="subtitle2" fontWeight={700}>{loop.title}</Typography>
                    <Chip size="small" color={statusColor(loop.status)} variant="outlined" label={statusText(loop.status)} />
                  </Stack>
                  <Typography variant="caption" color="text.secondary">写入知识库</Typography>
                  <Chip size="small" label={loop.targetKb} sx={{ alignSelf: 'flex-start' }} />
                  <MetricSummary metrics={loop.metrics} />
                  <Typography variant="caption" color="text.secondary">来源：{loop.source}</Typography>
                </Stack>
              </Paper>
            </Grid>
          ))}
        </Grid>
      </Stack>
    </Box>
  )
}

export default function RoleHomePage({ role }: RoleHomePageProps) {
  const config = roleConfigs[role]

  const homeQuery = useQuery({
    queryKey: ['role-home-bff', role],
    queryFn: () => roleHomeApi.getHome(role),
  })

  const home = homeQuery.data
  const homeMetrics = home?.metrics ?? {}

  const metricItems: MetricItem[] = role === 'admin'
    ? [
        { label: '平台用户', value: fmtNumber(metricNumber(homeMetrics, 'totalUsers')), hint: `今日新增 ${fmtNumber(metricNumber(homeMetrics, 'todayUsers'))}`, icon: <GroupIcon fontSize="small" /> },
        { label: 'AI 今日调用', value: fmtNumber(metricNumber(homeMetrics, 'todayAiCalls', 'todayCallCount')), hint: `成功率 ${fmtNumber(metricNumber(homeMetrics, 'aiSuccessRate', 'todayAiSuccessRate', 'successRate'))}%`, icon: <PsychologyIcon fontSize="small" /> },
        { label: '知识库文档', value: fmtNumber(metricNumber(homeMetrics, 'kbDocumentCount', 'documentCount')), hint: 'AI 仪表盘统计', icon: <FactCheckIcon fontSize="small" /> },
        { label: '今日收入', value: fmtMoney(metricNumber(homeMetrics, 'todayRevenue')), hint: '平台全局口径', icon: <DashboardIcon fontSize="small" /> },
      ]
    : role === 'org'
      ? [
          { label: '机构 GMV', value: fmtMoney(metricNumber(homeMetrics, 'todayRevenue')), hint: '当前机构口径', icon: <DashboardIcon fontSize="small" /> },
          { label: '直播场次', value: fmtNumber(metricNumber(homeMetrics, 'totalLiveSessions')), hint: `今日 ${fmtNumber(metricNumber(homeMetrics, 'todaySessions'))}`, icon: <LiveTvIcon fontSize="small" /> },
          { label: '待审话术', value: fmtNumber(metricNumber(homeMetrics, 'pendingApprovalCount')), hint: '来自审批队列', icon: <FactCheckIcon fontSize="small" /> },
          { label: '数据边界', value: home?.boundary?.unrestricted ? '全局' : '已隔离', hint: ownerScopeText(home), icon: <ShoppingBagIcon fontSize="small" /> },
        ]
      : role === 'talent'
        ? [
            { label: '短视频项目', value: fmtNumber(metricNumber(homeMetrics, 'projectCount')), hint: metricText(homeMetrics, 'latestProjectTitle', '暂无项目'), icon: <VideoLibraryIcon fontSize="small" /> },
            { label: '成片数量', value: fmtNumber(metricNumber(homeMetrics, 'totalVideoCount')), hint: '短视频看板统计', icon: <AutoAwesomeIcon fontSize="small" /> },
            { label: '播放量', value: fmtNumber(metricNumber(homeMetrics, 'totalPlayCount')), hint: '已同步内容口径', icon: <DashboardIcon fontSize="small" /> },
            { label: '直播场次', value: fmtNumber(metricNumber(homeMetrics, 'totalLiveSessions')), hint: ownerScopeText(home), icon: <LiveTvIcon fontSize="small" /> },
          ]
        : [
            { label: '个人项目', value: fmtNumber(metricNumber(homeMetrics, 'projectCount')), hint: metricText(homeMetrics, 'latestProjectTitle', '暂无项目'), icon: <VideoLibraryIcon fontSize="small" /> },
            { label: '草稿入口', value: '已接入', hint: '/short-video/project/save', icon: <AutoAwesomeIcon fontSize="small" /> },
            { label: '发布检查', value: '已接入', hint: '/short-video/publish/ai-review', icon: <FactCheckIcon fontSize="small" /> },
            { label: '数据边界', value: home?.boundary?.unrestricted ? '异常' : '个人', hint: ownerScopeText(home), icon: <DashboardIcon fontSize="small" /> },
          ]

  const errors = homeQuery.isError ? [`${config.readyEndpoints[0]}：${getErrorMessage(homeQuery.error)}`] : []
  const loading = homeQuery.isLoading

  return (
    <Box
      data-testid={`${role}-home-page`}
      data-role-home={role}
      data-shell-path={config.shellPath}
      data-ready-endpoints={(home?.readyEndpoints?.length ? home.readyEndpoints : config.readyEndpoints).join(',')}
      data-bff-endpoint={config.readyEndpoints[0]}
      data-owner-boundary={ownerScopeText(home)}
      data-no-cross-role-navigation="true"
      sx={{ width: '100%', maxWidth: 1180, mx: 'auto' }}
    >
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} alignItems={{ xs: 'flex-start', md: 'center' }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <Typography variant="h6" fontWeight={700}>{config.title}</Typography>
                <Chip size="small" variant="outlined" label={config.badge} />
              </Stack>
              <Typography variant="body2" color="text.secondary">{config.subtitle}</Typography>
            </Box>
            {loading ? (
              <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
                <CircularProgress size={18} />
                <Typography variant="body2">读取角色数据...</Typography>
              </Stack>
            ) : null}
          </Stack>
        </Paper>

        {errors.length > 0 ? (
          <Alert severity="warning" variant="outlined" data-testid={`${role}-home-error`} data-row-retained-on-error="true">
            {errors.join('；')}
          </Alert>
        ) : null}

        <Alert severity="info" variant="outlined" data-testid={`${role}-home-bff-ready`}>
          当前首页已接入 {config.readyEndpoints[0]} 聚合接口；业务数据边界：{ownerScopeText(home)}。
        </Alert>

        <Grid container spacing={2}>
          {metricItems.map(metric => (
            <Grid key={metric.label} item xs={12} sm={6} lg={3}>
              <MetricCard metric={metric} />
            </Grid>
          ))}
        </Grid>

        <Box>
          <Stack spacing={1.5}>
            <Stack direction="row" spacing={1} alignItems="center">
              <DashboardIcon fontSize="small" color="primary" />
              <Typography variant="subtitle1" fontWeight={700}>角色产品线入口</Typography>
            </Stack>
            <Grid container spacing={2}>
              {config.actions.map(action => (
                <Grid key={action.path} item xs={12} sm={6} lg={3}>
                  <ActionCard action={action} />
                </Grid>
              ))}
            </Grid>
          </Stack>
        </Box>

        <Grid container spacing={2}>
          <Grid item xs={12} lg={4}>
            <KnowledgeGuardPanel home={home} role={role} />
          </Grid>
          <Grid item xs={12} lg={8}>
            <BusinessChainsPanel chains={home?.businessChains ?? []} role={role} />
          </Grid>
        </Grid>

        <LearningLoopsPanel loops={home?.learningLoops ?? []} role={role} />
      </Stack>
    </Box>
  )
}
