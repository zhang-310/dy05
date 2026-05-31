import { useState, useEffect } from 'react'
import {
  Box, TextField, Button, Chip, Stack, Typography, Tabs, Tab,
  Drawer, CircularProgress, LinearProgress, Grid,
  IconButton, Tooltip, MenuItem, FormControl, InputLabel, Select,
  Paper, Avatar, Alert,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import SyncIcon from '@mui/icons-material/Sync'
import RefreshIcon from '@mui/icons-material/Refresh'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import LinkIcon from '@mui/icons-material/Link'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import PersonOutlineIcon from '@mui/icons-material/PersonOutline'
import CloseIcon from '@mui/icons-material/Close'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader, DataGridEmptyOverlay } from '@/components/base'
import {
  douyinApi,
  type DyAccount,
  type DyAccountSave,
  type DyFanProfileStatItem,
  type DyFanProfileStats,
  type DyPersonaSave,
  type DyTokenStatus,
} from '@/api/douyin'
import { useToast } from '@/contexts/ToastContext'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import ReactECharts from 'echarts-for-react'

// ─── Health Score ─────────────────────────────────────────────────────────────
type HealthItem = {
  label: string
  value: string
  points: number
  max: number
  color: 'success' | 'warning' | 'error' | 'default'
}

type CoreMetricTone = 'primary' | 'success' | 'warning' | 'info'
type FanProfileChartTone = 'primary' | 'secondary' | 'success' | 'warning' | 'info'

function getEffectiveAuthStatus(acc: DyAccount, tokenStatus?: DyTokenStatus | null) {
  if (tokenStatus?.status === 'valid' || tokenStatus?.status === 'expired' || tokenStatus?.status === 'refreshing') {
    return tokenStatus.status
  }
  return acc.authStatus
}

function getHealthItems(acc: DyAccount, tokenStatus?: DyTokenStatus | null): HealthItem[] {
  const authStatus = getEffectiveAuthStatus(acc, tokenStatus)
  const fanCount = Number(acc.fanCount ?? 0)
  const videoCount = Number(acc.videoCount ?? 0)
  const hasSync = Boolean(acc.lastSyncTime)

  const authPoints = authStatus === 'valid' ? 40 : authStatus === 'refreshing' ? 20 : 0
  const audiencePoints = fanCount >= 100000 ? 20 : fanCount > 0 ? 10 : 0

  return [
    {
      label: 'Token',
      value: authStatus === 'valid' ? '已核验有效' : authStatus === 'refreshing' ? '刷新中' : authStatus === 'expired' ? '已过期' : '未授权/未知',
      points: authPoints,
      max: 40,
      color: authPoints === 40 ? 'success' : authPoints > 0 ? 'warning' : 'error',
    },
    {
      label: '数据同步',
      value: hasSync ? formatDate(acc.lastSyncTime!) : '账号列表未返回同步时间',
      points: hasSync ? 20 : 0,
      max: 20,
      color: hasSync ? 'success' : 'warning',
    },
    {
      label: '内容资产',
      value: videoCount > 0 ? `${videoCount} 条视频` : '暂无视频',
      points: videoCount > 0 ? 20 : 0,
      max: 20,
      color: videoCount > 0 ? 'success' : 'warning',
    },
    {
      label: '受众规模',
      value: `${fmt(fanCount)} 粉丝`,
      points: audiencePoints,
      max: 20,
      color: audiencePoints === 20 ? 'success' : audiencePoints > 0 ? 'warning' : 'default',
    },
  ]
}

function calcHealthScore(acc: DyAccount, tokenStatus?: DyTokenStatus | null): number {
  const score = getHealthItems(acc, tokenStatus).reduce((sum, item) => sum + item.points, 0)
  return Math.max(0, Math.min(score, 100))
}

function HealthBar({ score }: { score: number }) {
  const color = score >= 80 ? 'success' : score >= 60 ? 'warning' : 'error'
  return (
    <Stack direction="row" alignItems="center" spacing={1} sx={{ width: '100%', height: '100%' }}>
      <LinearProgress variant="determinate" value={score} color={color} sx={{ flex: 1, height: 6, borderRadius: 3 }} />
      <Typography variant="caption" sx={{ minWidth: 30 }}>{score}</Typography>
    </Stack>
  )
}

function fmt(n: number) {
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`
  return String(n)
}

function errMsg(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

function authStatusText(status?: DyAccount['authStatus']) {
  if (status === 'valid') return '已授权'
  if (status === 'expired') return 'Token过期'
  return '未授权'
}

const DOUYIN_ROUTES = {
  accounts: '/talent/douyin/accounts',
  accountDetail: '/talent/douyin/accounts/:id',
  videos: '/talent/douyin/videos',
} as const

const DOUYIN_ENDPOINTS = {
  accountSearch: '/douyin/account/search',
  accountSave: '/douyin/account/save',
  accountDelete: '/douyin/account/delete',
  accountStats: '/douyin/account/statistics',
  videoSearch: '/douyin/video/search',
  videoSync: '/douyin/video/sync',
  personaGetByAccount: '/douyin/persona/get-by-account',
  personaSave: '/douyin/persona/save',
  personaDelete: '/douyin/persona/delete',
  fanProfileGet: '/douyin/fan-profile/get',
  fanProfileStats: '/douyin/fan-profile/stats',
  fanProfileSync: '/douyin/fan-profile/sync/{accountId}',
  tokenStatus: '/douyin/oauth/token-status',
  tokenRefresh: '/douyin/oauth/token-refresh',
  oauthUrl: '/douyin/oauth/auth-url',
  oauthRevoke: '/douyin/oauth/revoke',
} as const

const DOUYIN_READY_ENDPOINTS = Object.values(DOUYIN_ENDPOINTS).join('|')
const DOUYIN_UNSUPPORTED_ENDPOINTS = [
  '/douyin/account/mock',
  '/douyin/account/local-list',
  '/douyin/account/static-account',
  '/douyin/video/local-sync',
  '/douyin/persona/static',
  '/douyin/fan-profile/static',
  '/douyin/oauth/local-token',
].join('|')

function accountContext(account?: Partial<DyAccount> | null, fallbackId?: number | string) {
  return `route=${DOUYIN_ROUTES.accounts}; accountPk=${account?.id ?? fallbackId ?? '-'}; accountName=${account?.accountName || '未知账号'}; douyinAccount=${account?.accountId || '-'}; authStatus=${authStatusText(account?.authStatus)}`
}

function accountSaveContext(payload?: Partial<DyAccountSave> | null) {
  return `route=${DOUYIN_ROUTES.accounts}; accountPk=${payload?.id ?? '新增'}; accountName=${payload?.accountName || '未填写'}; douyinAccount=${payload?.accountId || '未填写'}; status=${payload?.status ?? 1}`
}

function personaContext(account?: Partial<DyAccount> | null, persona?: Partial<DyPersonaSave> | null, fallbackId?: number | string) {
  return `${accountContext(account)}; personaId=${persona?.id ?? fallbackId ?? '-'}; personaName=${persona?.personaName || '未知人设'}; personaType=${persona?.personaType || '-'}`
}

function HealthBreakdown({ account, tokenStatus }: { account: DyAccount; tokenStatus?: DyTokenStatus | null }) {
  const items = getHealthItems(account, tokenStatus)
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1.5}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="subtitle2">健康评分依据</Typography>
          <Chip size="small" label={`${calcHealthScore(account, tokenStatus)}/100`} color={calcHealthScore(account, tokenStatus) >= 80 ? 'success' : calcHealthScore(account, tokenStatus) >= 60 ? 'warning' : 'error'} />
        </Stack>
        {items.map(item => (
          <Stack key={item.label} direction="row" spacing={1.5} alignItems="center" justifyContent="space-between">
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="body2">{item.label}</Typography>
              <Typography variant="caption" color="text.secondary">{item.value}</Typography>
            </Box>
            <Chip size="small" color={item.color} variant="outlined" label={`${item.points}/${item.max}`} />
          </Stack>
        ))}
        <Alert severity="info">
          粉丝画像同步状态需要进入“粉丝画像”页读取 `/douyin/fan-profile/get`，不再用固定加分推高健康分。
        </Alert>
      </Stack>
    </Paper>
  )
}

function profileItemLabel(item: DyFanProfileStatItem) {
  return item.value || item.key || '未分类'
}

function profileItemValue(item: DyFanProfileStatItem) {
  return Number(item.percentage ?? item.count ?? 0)
}

function statItemsByType(stats: DyFanProfileStats[] | undefined, type: string): DyFanProfileStatItem[] {
  return (stats ?? [])
    .filter(item => item.statType === type || (!item.statType && type === 'age' && item.ageRange))
    .map(item => ({
      key: item.statKey ?? item.ageRange,
      value: item.statValue ?? item.ageRange ?? item.statKey,
      count: item.count,
      percentage: item.percentage ?? item.ratio,
    }))
}

function legacyGenderItems(maleRatio?: number, femaleRatio?: number): DyFanProfileStatItem[] {
  if (maleRatio == null && femaleRatio == null) return []
  const male = Number(maleRatio ?? Math.max(0, 100 - Number(femaleRatio ?? 0)))
  const female = Number(femaleRatio ?? Math.max(0, 100 - male))
  return [
    { key: 'male', value: '男', percentage: male },
    { key: 'female', value: '女', percentage: female },
  ].filter(item => Number.isFinite(item.percentage))
}

function topBarOption(items: DyFanProfileStatItem[], color: string) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 24, top: 10, bottom: 24 },
    xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: items.map(profileItemLabel) },
    series: [{
      type: 'bar',
      data: items.map(profileItemValue),
      itemStyle: { color },
      label: { show: true, position: 'right', formatter: '{c}%' },
    }],
  }
}

function csvCell(value: unknown) {
  const text = String(value ?? '')
  return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

function downloadCsv(filename: string, rows: unknown[][]) {
  const csv = rows.map(row => row.map(csvCell).join(',')).join('\n')
  const blob = new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function exportOauthReport(rows: DyAccount[]) {
  downloadCsv('douyin-oauth-report.csv', [
    ['账号名称', '抖音号', '授权状态', '粉丝数', '视频数', '上次同步'],
    ...rows.map(row => [
      row.accountName,
      row.accountId,
      authStatusText(row.authStatus),
      row.fanCount ?? 0,
      row.videoCount ?? 0,
      row.lastSyncTime ? formatDate(row.lastSyncTime) : '未同步',
    ]),
  ])
}
// ─── Account Detail Drawer ───────────────────────────────────────────────────
function AccountDetailDrawer({ account, onClose }: { account: DyAccount | null; onClose: () => void }) {
  const theme = useTheme()
  const toast = useToast()
  const qc = useQueryClient()
  const [subTab, setSubTab] = useState(0)
  const [personaEditOpen, setPersonaEditOpen] = useState(false)
  const [personaEditData, setPersonaEditData] = useState<Partial<DyPersonaSave>>({})
  const [personaDeleteConfirm, setPersonaDeleteConfirm] = useState(false)
  const [detailActionError, setDetailActionError] = useState('')

  const {
    data: persona,
    isFetching: personaLoading,
    isError: personaError,
    error: personaErr,
    refetch: refetchPersona,
  } = useQuery({
    queryKey: ['persona-by-account', account?.id],
    queryFn: () => douyinApi.personaGetByAccount(account!.id),
    enabled: !!account && subTab === 1,
  })
  const personaSaveMut = useMutation({
    mutationFn: (p: Partial<DyPersonaSave>) => douyinApi.personaSave(p),
    onMutate: () => setDetailActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persona-by-account', account?.id] }); setPersonaEditOpen(false); toast('人设保存成功', 'success') },
    onError: (e: unknown, payload) => {
      setDetailActionError(`${DOUYIN_ENDPOINTS.personaSave} 人设保存失败：${errMsg(e, '保存失败')}（${personaContext(account, payload)}）`)
      toast('人设保存失败', 'error')
    },
  })
  const personaDeleteMut = useMutation({
    mutationFn: (id: number) => douyinApi.personaDelete(id),
    onMutate: () => setDetailActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['persona-by-account', account?.id] }); setPersonaDeleteConfirm(false); toast('人设已删除', 'success') },
    onError: (e: unknown, id) => {
      setDetailActionError(`${DOUYIN_ENDPOINTS.personaDelete} 人设删除失败：${errMsg(e, '删除失败')}（${personaContext(account, persona ?? undefined, id)}）`)
      toast('人设删除失败', 'error')
    },
  })

  const {
    data: fanProfile,
    isFetching: profileLoading,
    isError: profileError,
    error: profileErr,
    refetch: refetchProfile,
  } = useQuery({
    queryKey: ['fan-profile', account?.id],
    queryFn: () => douyinApi.fanProfileGet(account!.id),
    enabled: !!account && subTab === 2,
  })
  const {
    data: fanStats,
    isError: fanStatsError,
    error: fanStatsErr,
    refetch: refetchFanStats,
  } = useQuery({
    queryKey: ['fan-stats', account?.id],
    queryFn: () => douyinApi.fanProfileStats(account!.id),
    enabled: !!account && subTab === 2,
  })
  const {
    data: videos,
    isFetching: videosLoading,
    isError: videosError,
    error: videosErr,
    refetch: refetchVideos,
  } = useQuery({
    queryKey: ['account-videos', account?.id],
    queryFn: () => douyinApi.videoSearch({ accountId: account!.id, page: 0, rows: 10 }),
    enabled: !!account && subTab === 3,
  })
  const {
    data: tokenInfo,
    isFetching: tokenLoading,
    isError: tokenError,
    error: tokenErr,
    refetch: refetchToken,
  } = useQuery({
    queryKey: ['token-status', account?.id],
    queryFn: () => douyinApi.tokenStatus(account!.id),
    enabled: !!account && (subTab === 0 || subTab === 4),
  })

  const syncProfileMut = useMutation({
    mutationFn: () => douyinApi.fanProfileSync(account!.id),
    onMutate: () => setDetailActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['fan-profile', account?.id] }); qc.invalidateQueries({ queryKey: ['fan-stats', account?.id] }); toast('粉丝画像同步完成', 'success') },
    onError: (e) => {
      setDetailActionError(`${DOUYIN_ENDPOINTS.fanProfileSync} 粉丝画像同步失败：${errMsg(e, '同步失败')}（${accountContext(account)}）`)
      toast('粉丝画像同步失败', 'error')
    },
  })
  const syncVideosMut = useMutation({
    mutationFn: () => douyinApi.videoSync(account!.id),
    onMutate: () => setDetailActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['account-videos', account?.id] }); qc.invalidateQueries({ queryKey: ['dy-accounts'] }); toast('视频同步完成', 'success') },
    onError: (e) => {
      setDetailActionError(`${DOUYIN_ENDPOINTS.videoSync} 视频同步失败：${errMsg(e, '抖音开放平台同步链路异常')}（${accountContext(account)}）`)
      toast('视频同步失败', 'error')
    },
  })

  const genderData = fanProfile?.genderDistribution?.length
    ? fanProfile.genderDistribution
    : legacyGenderItems(fanProfile?.maleRatio, fanProfile?.femaleRatio)
  const ageData = fanProfile?.ageDistribution?.length
    ? fanProfile.ageDistribution
    : statItemsByType(fanStats, 'age')
  const provinceData = fanProfile?.provinceDistribution?.length
    ? fanProfile.provinceDistribution
    : statItemsByType(fanStats, 'province')
  const cityData = fanProfile?.cityDistribution?.length
    ? fanProfile.cityDistribution
    : statItemsByType(fanStats, 'city')
  const interestData = fanProfile?.interestTags?.length
    ? fanProfile.interestTags
    : statItemsByType(fanStats, 'interest')
  const activeTimeData = fanProfile?.activeTimeDistribution?.length
    ? fanProfile.activeTimeDistribution
    : statItemsByType(fanStats, 'active_time')
  const deviceData = fanProfile?.deviceDistribution?.length
    ? fanProfile.deviceDistribution
    : statItemsByType(fanStats, 'device')
  const hasProfileData = [genderData, ageData, provinceData, cityData, interestData, activeTimeData, deviceData]
    .some(items => items.length > 0)
  const profileChartColor = (tone: FanProfileChartTone) => theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  const fanProfileChartColors = {
    gender: [profileChartColor('secondary'), profileChartColor('primary')],
    age: profileChartColor('primary'),
    province: profileChartColor('info'),
    city: profileChartColor('success'),
    interest: profileChartColor('secondary'),
    activeTime: profileChartColor('warning'),
    device: profileChartColor('success'),
  }

  const videoList = videos?.list ?? (Array.isArray(videos) ? videos : [])

  return (
    <Drawer anchor="right" open={!!account} onClose={onClose} PaperProps={{ sx: { width: 700 } }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">{account?.accountName}</Typography>
        <IconButton onClick={onClose} aria-label="关闭"><CloseIcon /></IconButton>
      </Box>
      <Tabs value={subTab} onChange={(_, v) => setSubTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
        <Tab label="基本信息" />
        <Tab label="人设配置" />
        <Tab label="粉丝画像" />
        <Tab label="近期视频" />
        <Tab label="Token管理" />
      </Tabs>
      <Box
        data-testid="douyin-account-detail-contract"
        data-contract-scope="douyin-account-detail-drawer"
        data-ready-endpoints={[
          DOUYIN_ENDPOINTS.personaGetByAccount,
          DOUYIN_ENDPOINTS.personaSave,
          DOUYIN_ENDPOINTS.personaDelete,
          DOUYIN_ENDPOINTS.fanProfileGet,
          DOUYIN_ENDPOINTS.fanProfileStats,
          DOUYIN_ENDPOINTS.fanProfileSync,
          DOUYIN_ENDPOINTS.videoSearch,
          DOUYIN_ENDPOINTS.videoSync,
          DOUYIN_ENDPOINTS.tokenStatus,
          DOUYIN_ENDPOINTS.tokenRefresh,
          DOUYIN_ENDPOINTS.oauthUrl,
        ].join('|')}
        data-unsupported-endpoints={DOUYIN_UNSUPPORTED_ENDPOINTS}
        data-no-local-detail-fallback="true"
        data-no-static-persona-fallback="true"
        data-no-static-fan-profile-fallback="true"
        sx={{ p: 3, flex: 1, overflow: 'auto' }}
      >
        {detailActionError && (
          <Alert
            data-testid="douyin-account-detail-action-error"
            data-input-retained="true"
            data-no-local-mutation="true"
            severity="error"
            sx={{ mb: 2 }}
          >
            {detailActionError}。失败不会关闭当前抽屉或清空输入。
          </Alert>
        )}
        {subTab === 0 && account && (
          <Stack spacing={3}>
            {/* 头像 + 基本信息 */}
            <Stack direction="row" spacing={2} alignItems="center">
              <Avatar src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${account.accountId}`} sx={{ width: 64, height: 64 }} />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="h6" noWrap>{account.accountName}</Typography>
                <Typography variant="body2" color="text.secondary" noWrap>@{account.accountId} | 粉丝 {fmt(account.fanCount)}</Typography>
                <Box sx={{ mt: 1, maxWidth: 260 }}>
                  <HealthBar score={calcHealthScore(account, tokenInfo)} />
                  <Typography variant="caption" color="text.secondary">健康评分 {calcHealthScore(account, tokenInfo)}/100</Typography>
                </Box>
              </Box>
            </Stack>

            {/* 核心指标 4 卡片 */}
            <Stack direction="row" spacing={1.5}>
              {([
                { label: '粉丝数', value: fmt(account.fanCount), color: 'primary.main', tone: 'primary' },
                { label: '获赞数', value: fmt(account.totalLikes), color: 'success.main', tone: 'success' },
                { label: '视频数', value: String(account.videoCount), color: 'warning.main', tone: 'warning' },
                { label: '本月GMV', value: '—', color: 'info.main', tone: 'info' },
              ] satisfies Array<{ label: string; value: string; color: string; tone: CoreMetricTone }>).map(item => (
                <Paper
                  key={item.label}
                  data-testid="douyin-account-core-metric-surface"
                  sx={(theme) => ({
                    flex: 1,
                    py: 1.5,
                    px: 1,
                    textAlign: 'center',
                    bgcolor: alpha(theme.palette[item.tone].main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
                    border: '1px solid',
                    borderColor: alpha(theme.palette[item.tone].main, theme.palette.mode === 'dark' ? 0.38 : 0.18),
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                  })}
                >
                  <Typography variant="h6" color={item.color} sx={{ fontWeight: 700, lineHeight: 1.3 }}>{item.value}</Typography>
                  <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                </Paper>
              ))}
            </Stack>

            {/* 授权信息 */}
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Stack spacing={1.5}>
                {[
                  {
                    label: '授权状态',
                    node: <Chip
                      label={account.authStatus === 'valid' ? '已授权' : account.authStatus === 'expired' ? 'Token过期' : '未授权'}
                      color={account.authStatus === 'valid' ? 'success' : account.authStatus === 'expired' ? 'error' : 'default'}
                      size="small"
                    />,
                  },
                  {
                    label: 'Token 到期',
                    node: <Typography variant="body2" color="text.secondary">
                      {tokenInfo?.expireTime ? `${tokenInfo.expireTime}（剩余 ${tokenInfo.daysLeft ?? '?'} 天）` : '未知'}
                    </Typography>,
                  },
                  {
                    label: '上次同步',
                    node: <Typography variant="body2" color="text.secondary">
                      {account.lastSyncTime ? formatDate(account.lastSyncTime) : '未同步'}
                    </Typography>,
                  },
                ].map(row => (
                  <Stack key={row.label} direction="row" alignItems="center" justifyContent="space-between" sx={{ minHeight: 28 }}>
                    <Typography variant="body2" sx={{ flexShrink: 0, width: 90 }}>{row.label}</Typography>
                    {row.node}
                  </Stack>
                ))}
              </Stack>
            </Paper>

            <HealthBreakdown account={account} tokenStatus={tokenInfo} />

            {/* 操作按钮 */}
            <Stack direction="row" spacing={2}>
              <Button variant="contained" startIcon={<SyncIcon />} fullWidth onClick={() => syncVideosMut.mutate()} disabled={syncVideosMut.isPending}>
                {syncVideosMut.isPending ? '同步中...' : '同步最新数据'}
              </Button>
              <Button variant="outlined" startIcon={<LinkIcon />} fullWidth onClick={async () => {
                try {
                  setDetailActionError('')
                  const ent = await checkGaifanEntitlement('douyin-ops', 'douyin-ops.account-mgmt')
                  if (ent && ent.granted === false) {
                    toast(commercialDenialMessage({ code: 4421, message: ent.reason ?? '无账号管理权益' }), 'warning')
                    return
                  }
                  const r = await douyinApi.oauthUrl(account.id)
                  window.open(r.authUrl, '_blank')
                } catch (e) {
                  if (isCommercialDenial(e)) {
                    toast(commercialDenialMessage(e), 'warning')
                    return
                  }
                  setDetailActionError(`${DOUYIN_ENDPOINTS.oauthUrl} 获取授权链接失败：${errMsg(e, 'OAuth 服务异常')}（${accountContext(account)}）`)
                  toast('获取授权链接失败', 'error')
                }
              }}>重新授权</Button>
            </Stack>

            {tokenError && (
              <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => void refetchToken()}>重试</Button>}>
                {DOUYIN_ENDPOINTS.tokenStatus} Token 状态暂不可用：{errMsg(tokenErr, '授权有效期需要到 Token 管理页重新核验。')}（{accountContext(account)}）
              </Alert>
            )}
            {syncVideosMut.isError && (
              <Alert severity="error" action={<Button color="inherit" size="small" onClick={() => syncVideosMut.mutate()}>重试同步</Button>}>
                {DOUYIN_ENDPOINTS.videoSync} 视频同步失败：{errMsg(syncVideosMut.error, '抖音开放平台同步链路异常。')}（{accountContext(account)}）
              </Alert>
            )}

            {/* 简介 */}
            <Box>
              <Typography variant="subtitle2" gutterBottom>账号简介</Typography>
              <Typography variant="body2" color="text.secondary">{account.description || '暂无简介'}</Typography>
            </Box>
          </Stack>
        )}
        {subTab === 1 && (
          personaLoading ? <CircularProgress /> : persona ? (
            <Stack spacing={3}>
              {personaError && (
                <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => void refetchPersona()}>重试</Button>}>
                  {DOUYIN_ENDPOINTS.personaGetByAccount} 人设数据暂不可用：{errMsg(personaErr, '查询失败')}（{accountContext(account)}）
                </Alert>
              )}
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="subtitle1" fontWeight={600}>{persona.personaName}</Typography>
                <Stack direction="row" spacing={1}>
                  <Button size="small" variant="outlined" startIcon={<EditIcon />} onClick={() => {
                    setPersonaEditData({ id: persona.id, accountId: account!.id, personaName: persona.personaName, personaType: persona.personaType, targetAudience: persona.targetAudience, tone: persona.tone, contentStyle: persona.contentStyle, keywords: persona.keywords, isDefault: persona.isDefault })
                    setPersonaEditOpen(true)
                  }}>编辑</Button>
                  <Button size="small" variant="outlined" color="error" startIcon={<DeleteIcon />} onClick={() => setPersonaDeleteConfirm(true)}>删除</Button>
                </Stack>
              </Stack>
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Stack spacing={1.5}>
                  {[
                    { label: '人设类型', value: persona.personaType },
                    { label: '目标受众', value: persona.targetAudience },
                    { label: '话术风格', value: persona.tone },
                    { label: '内容风格', value: persona.contentStyle },
                    { label: '关键词', value: persona.keywords },
                  ].map(row => (
                    <Stack key={row.label} direction="row" spacing={2} sx={{ minHeight: 28 }}>
                      <Typography variant="body2" sx={{ flexShrink: 0, width: 80, color: 'text.secondary' }}>{row.label}</Typography>
                      <Typography variant="body2">{row.value || '—'}</Typography>
                    </Stack>
                  ))}
                </Stack>
              </Paper>
              {persona.isDefault === 1 && <Chip label="默认人设" color="primary" size="small" sx={{ alignSelf: 'flex-start' }} />}
              <ConfirmDialog
                open={personaDeleteConfirm} title="确认删除" content={`确认删除此人设？endpoint=${DOUYIN_ENDPOINTS.personaDelete}; ${personaContext(account, persona)}。删除失败不会清空当前人设信息。`}
                onConfirm={() => personaDeleteMut.mutate(persona.id)}
                onClose={() => setPersonaDeleteConfirm(false)}
              />
            </Stack>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 8, gap: 2 }}>
              <PersonOutlineIcon sx={{ fontSize: 48, color: 'text.disabled' }} />
              <Typography color="text.secondary">此账号暂未配置人设</Typography>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => {
                setPersonaEditData({ accountId: account!.id })
                setPersonaEditOpen(true)
              }}>创建人设</Button>
              {personaError && (
                <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => void refetchPersona()}>重试</Button>}>
                  {DOUYIN_ENDPOINTS.personaGetByAccount} 人设查询失败：{errMsg(personaErr, '请稍后重试')}（{accountContext(account)}）
                </Alert>
              )}
            </Box>
          )
        )}
        {subTab === 1 && (
          <PersonaEditDrawer
            open={personaEditOpen} data={personaEditData}
            onClose={() => setPersonaEditOpen(false)}
            onSave={v => personaSaveMut.mutate(v)}
            loading={personaSaveMut.isPending}
            accounts={[]}
            hideAccountSelect
          />
        )}
        {subTab === 2 && (
          profileLoading ? <CircularProgress /> : (
            <Stack spacing={3}>
              <Stack direction="row" justifyContent="flex-end">
                <Button size="small" startIcon={<SyncIcon />} onClick={() => syncProfileMut.mutate()} disabled={syncProfileMut.isPending}>
                  {syncProfileMut.isPending ? '同步中...' : '同步画像'}
                </Button>
              </Stack>
              {(profileError || fanStatsError) && (
                <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => { void refetchProfile(); void refetchFanStats() }}>重试</Button>}>
                  {DOUYIN_ENDPOINTS.fanProfileGet}|{DOUYIN_ENDPOINTS.fanProfileStats} 粉丝画像数据暂不可用：{errMsg(profileErr ?? fanStatsErr, '查询失败')}（{accountContext(account)}）
                </Alert>
              )}
              {fanProfile?.syncTime && (
                <Typography variant="caption" color="text.secondary">
                  画像同步时间：{typeof fanProfile.syncTime === 'number' ? formatDate(new Date(fanProfile.syncTime).toISOString()) : fanProfile.syncTime}
                </Typography>
              )}
              {!hasProfileData
                ? <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>暂无粉丝画像数据，请先同步</Typography>
                : (
                <Grid container spacing={2}>
                  {genderData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>性别分布</Typography>
                      <Box
                        data-testid="douyin-fan-profile-chart-surface"
                        data-chart-kind="gender"
                        data-chart-colors={fanProfileChartColors.gender.join('|')}
                      >
                        <ReactECharts style={{ height: 200 }} option={{
                          tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
                          series: [{ type: 'pie', radius: ['40%', '65%'], data: genderData.map((item, index) => ({ name: profileItemLabel(item), value: profileItemValue(item), itemStyle: { color: fanProfileChartColors.gender[index] ?? fanProfileChartColors.gender[fanProfileChartColors.gender.length - 1] } })),
                            label: { formatter: '{b}\n{d}%' } }]
                        }} />
                      </Box>
                    </Grid>
                  )}
                  {ageData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>年龄分布</Typography>
                      <Box
                        data-testid="douyin-fan-profile-chart-surface"
                        data-chart-kind="age"
                        data-chart-color={fanProfileChartColors.age}
                      >
                        <ReactECharts style={{ height: 200 }} option={{
                          tooltip: {},
                          xAxis: { type: 'category', data: ageData.map(profileItemLabel) },
                          yAxis: { type: 'value' },
                          series: [{ type: 'bar', data: ageData.map(profileItemValue), itemStyle: { color: fanProfileChartColors.age } }]
                        }} />
                      </Box>
                    </Grid>
                  )}
                  {provinceData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>省份 TOP10</Typography>
                      <Box data-testid="douyin-fan-profile-chart-surface" data-chart-kind="province" data-chart-color={fanProfileChartColors.province}>
                        <ReactECharts style={{ height: 200 }} option={topBarOption(provinceData.slice(0, 10), fanProfileChartColors.province)} />
                      </Box>
                    </Grid>
                  )}
                  {cityData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>城市 TOP10</Typography>
                      <Box data-testid="douyin-fan-profile-chart-surface" data-chart-kind="city" data-chart-color={fanProfileChartColors.city}>
                        <ReactECharts style={{ height: 200 }} option={topBarOption(cityData.slice(0, 10), fanProfileChartColors.city)} />
                      </Box>
                    </Grid>
                  )}
                  {interestData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>兴趣标签 TOP10</Typography>
                      <Box data-testid="douyin-fan-profile-chart-surface" data-chart-kind="interest" data-chart-color={fanProfileChartColors.interest}>
                        <ReactECharts style={{ height: 200 }} option={topBarOption(interestData.slice(0, 10), fanProfileChartColors.interest)} />
                      </Box>
                    </Grid>
                  )}
                  {activeTimeData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>活跃时段</Typography>
                      <Box data-testid="douyin-fan-profile-chart-surface" data-chart-kind="active-time" data-chart-color={fanProfileChartColors.activeTime}>
                        <ReactECharts style={{ height: 200 }} option={topBarOption(activeTimeData.slice(0, 10), fanProfileChartColors.activeTime)} />
                      </Box>
                    </Grid>
                  )}
                  {deviceData.length > 0 && (
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" mb={1}>设备分布</Typography>
                      <Box data-testid="douyin-fan-profile-chart-surface" data-chart-kind="device" data-chart-color={fanProfileChartColors.device}>
                        <ReactECharts style={{ height: 200 }} option={topBarOption(deviceData.slice(0, 10), fanProfileChartColors.device)} />
                      </Box>
                    </Grid>
                  )}
                </Grid>
              )}
              <Alert severity="info">
                粉丝画像只展示 `/douyin/fan-profile/get` 和 `/douyin/fan-profile/stats` 返回的真实分布；消费力和趋势未由后端返回时不再使用静态图表补齐。
              </Alert>
            </Stack>
          )
        )}
        {subTab === 3 && (
          videosLoading ? <CircularProgress /> : (
            <Stack spacing={2}>
              <Stack direction="row" justifyContent="flex-end">
                <Button size="small" startIcon={<SyncIcon />} onClick={() => syncVideosMut.mutate()} disabled={syncVideosMut.isPending}>
                  {syncVideosMut.isPending ? '同步中...' : '同步视频'}
                </Button>
              </Stack>
              {videosError && (
                <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => void refetchVideos()}>重试</Button>}>
                  {DOUYIN_ENDPOINTS.videoSearch} 近期视频数据暂不可用：{errMsg(videosErr, '查询失败')}（{accountContext(account)}; page=0; rows=10）
                </Alert>
              )}
              {videoList.map((v, i) => (
                <Paper key={i} sx={{ p: 1.5, display: 'flex', gap: 2, alignItems: 'center' }}>
                  <Box sx={{ width: 80, height: 60, bgcolor: 'grey.200', borderRadius: 1, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Typography variant="caption" color="text.secondary">封面</Typography>
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="body2" noWrap fontWeight={500}>{v.title}</Typography>
                    <Stack direction="row" spacing={1} mt={0.5}>
                      <Typography variant="caption" color="text.secondary">播放 {fmt(v.viewCount ?? 0)}</Typography>
                      <Typography variant="caption" color="text.secondary">点赞 {fmt(v.likeCount ?? 0)}</Typography>
                      {(v.likeCount ?? 0) > 50000 && <Chip size="small" label="爆款" color="warning" />}
                    </Stack>
                  </Box>
                </Paper>
              ))}
              {videoList.length === 0 && <Typography color="text.secondary">暂无视频数据。若已经同步过仍为空，说明当前账号没有可见视频或后端采集链路降级。</Typography>}
            </Stack>
          )
        )}
        {subTab === 4 && (
          tokenLoading ? <CircularProgress /> : (
            <Stack spacing={2}>
              {tokenError && (
                <Alert severity="warning" action={<Button color="inherit" size="small" onClick={() => void refetchToken()}>重试</Button>}>
                  {DOUYIN_ENDPOINTS.tokenStatus} Token 状态暂不可用：{errMsg(tokenErr, '查询失败')}（{accountContext(account)}）
                </Alert>
              )}
              {tokenInfo && (
                <Stack spacing={2}>
                  <Stack direction="row" alignItems="center" spacing={2}>
                    <Typography variant="body2">Token 状态：</Typography>
                    <Chip
                      label={tokenInfo.status === 'valid' ? '有效' : tokenInfo.status === 'expired' ? '已过期' : '刷新中'}
                      color={tokenInfo.status === 'valid' ? 'success' : tokenInfo.status === 'expired' ? 'error' : 'warning'}
                    />
                  </Stack>
                  {tokenInfo.expireTime && <Typography variant="body2" color="text.secondary">过期时间：{tokenInfo.expireTime}</Typography>}
                  {tokenInfo.daysLeft !== undefined && <Typography variant="body2" color="text.secondary">剩余天数：{tokenInfo.daysLeft} 天</Typography>}
                  <Stack direction="row" spacing={2}>
                    <Button variant="outlined" startIcon={<LinkIcon />} onClick={async () => {
                      try {
                        setDetailActionError('')
                        const r = await douyinApi.oauthUrl(account!.id)
                        window.open(r.authUrl, '_blank')
                      } catch (e) {
                        setDetailActionError(`${DOUYIN_ENDPOINTS.oauthUrl} 获取授权链接失败：${errMsg(e, 'OAuth 服务异常')}（${accountContext(account)}）`)
                        toast('获取授权链接失败', 'error')
                      }
                    }}>重新授权</Button>
                    <Button variant="outlined" startIcon={<RefreshIcon />} onClick={async () => {
                      try {
                        setDetailActionError('')
                        await douyinApi.tokenRefresh(account!.id)
                        toast('Token 刷新成功', 'success')
                      } catch (e) {
                        setDetailActionError(`${DOUYIN_ENDPOINTS.tokenRefresh} Token 刷新失败：${errMsg(e, '刷新失败')}（${accountContext(account)}）`)
                        toast('Token 刷新失败', 'error')
                      }
                    }}>刷新 Token</Button>
                  </Stack>
                </Stack>
              )}
              {!tokenInfo && !tokenError && (
                <Typography color="text.secondary">暂无 Token 状态数据。若账号已绑定，说明当前授权信息未同步到 OAuth 服务。</Typography>
              )}
            </Stack>
          )
        )}
      </Box>
    </Drawer>
  )
}
// ─── Account Edit Drawer ─────────────────────────────────────────────────────
function AccountEditDrawer({ open, data, onClose, onSave, loading }: {
  open: boolean; data: Partial<DyAccountSave>; onClose: () => void
  onSave: (v: Partial<DyAccountSave>) => void; loading: boolean
}) {
  const [form, setForm] = useState<Partial<DyAccountSave>>(data)
  const set = (k: keyof DyAccountSave, v: unknown) => setForm(f => ({ ...f, [k]: v }))

  // sync when data changes
  useEffect(() => { setForm(data) }, [data])

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: 480 } }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">{data.id ? '编辑账号' : '新建账号'}</Typography>
        <IconButton onClick={onClose} aria-label="关闭"><CloseIcon /></IconButton>
      </Box>
      <Box sx={{ p: 3, flex: 1, overflow: 'auto' }}>
        <Stack spacing={2.5}>
          <TextField label="账号名称" value={form.accountName ?? ''} onChange={e => set('accountName', e.target.value)} size="small" fullWidth required />
          <TextField label="抖音号" value={form.accountId ?? ''} onChange={e => set('accountId', e.target.value)} size="small" fullWidth />
          <TextField label="描述" value={form.description ?? ''} onChange={e => set('description', e.target.value)} size="small" fullWidth multiline rows={3} />
          <FormControl size="small" fullWidth>
            <InputLabel>状态</InputLabel>
            <Select value={form.status ?? 1} label="状态" onChange={e => set('status', e.target.value)}>
              <MenuItem value={1}>正常</MenuItem>
              <MenuItem value={0}>停用</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </Box>
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1.5, justifyContent: 'flex-end' }}>
        <Button variant="outlined" onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSave(form)} disabled={loading}>
          {loading ? '保存中...' : '保存'}
        </Button>
      </Box>
    </Drawer>
  )
}

// ─── Persona Edit Drawer ──────────────────────────────────────────────────────
function PersonaEditDrawer({ open, data, onClose, onSave, loading, accounts, hideAccountSelect }: {
  open: boolean; data: Partial<DyPersonaSave>; onClose: () => void
  onSave: (v: Partial<DyPersonaSave>) => void; loading: boolean
  accounts: DyAccount[]
  hideAccountSelect?: boolean
}) {
  const [form, setForm] = useState<Partial<DyPersonaSave>>(data)
  const set = (k: keyof DyPersonaSave, v: unknown) => setForm(f => ({ ...f, [k]: v }))

  useEffect(() => { setForm(data) }, [data])

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: 480 } }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">{data.id ? '编辑人设' : '新建人设'}</Typography>
        <IconButton onClick={onClose} aria-label="关闭"><CloseIcon /></IconButton>
      </Box>
      <Box sx={{ p: 3, flex: 1, overflow: 'auto' }}>
        <Stack spacing={2.5}>
          {!hideAccountSelect && (
          <FormControl size="small" fullWidth required>
            <InputLabel>绑定账号</InputLabel>
            <Select value={form.accountId ?? ''} label="绑定账号" onChange={e => set('accountId', e.target.value)}>
              {accounts.map(a => (
                <MenuItem key={a.id} value={a.id}>{a.accountName}（{a.accountId}）</MenuItem>
              ))}
            </Select>
          </FormControl>
          )}
          <TextField label="人设名称" value={form.personaName ?? ''} onChange={e => set('personaName', e.target.value)} size="small" fullWidth required />
          <TextField label="人设类型" value={form.personaType ?? ''} onChange={e => set('personaType', e.target.value)} size="small" fullWidth />
          <TextField label="目标受众" value={form.targetAudience ?? ''} onChange={e => set('targetAudience', e.target.value)} size="small" fullWidth />
          <TextField label="话术风格" value={form.tone ?? ''} onChange={e => set('tone', e.target.value)} size="small" fullWidth />
          <TextField label="内容风格" value={form.contentStyle ?? ''} onChange={e => set('contentStyle', e.target.value)} size="small" fullWidth />
          <TextField label="关键词" value={form.keywords ?? ''} onChange={e => set('keywords', e.target.value)} size="small" fullWidth helperText="多个关键词用逗号分隔" />
          <FormControl size="small" fullWidth>
            <InputLabel>设为默认</InputLabel>
            <Select value={form.isDefault ?? 0} label="设为默认" onChange={e => set('isDefault', e.target.value)}>
              <MenuItem value={0}>否</MenuItem>
              <MenuItem value={1}>是</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </Box>
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1.5, justifyContent: 'flex-end' }}>
        <Button variant="outlined" onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={() => onSave(form)} disabled={loading}>
          {loading ? '保存中...' : '保存'}
        </Button>
      </Box>
    </Drawer>
  )
}
// ─── Account List Tab ────────────────────────────────────────────────────────
function AccountListTab({ tab, onTabChange }: { tab: number; onTabChange: (v: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [accountName, setAccountName] = useState('')
  const [accountIdKeyword, setAccountIdKeyword] = useState('')
  const [authStatusFilter, setAuthStatusFilter] = useState<string>('all')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [detailAcc, setDetailAcc] = useState<DyAccount | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [editData, setEditData] = useState<Partial<DyAccountSave>>({})
  const [deleteTarget, setDeleteTarget] = useState<DyAccount | null>(null)
  const [batchSyncResult, setBatchSyncResult] = useState<{ total: number; success: number; fail: number } | null>(null)
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['dy-accounts', accountName, accountIdKeyword, page, pageSize],
    queryFn: () => douyinApi.accountList({
      accountName: accountName.trim() || undefined,
      accountId: accountIdKeyword.trim() || undefined,
      page, rows: pageSize,
    }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<DyAccountSave>) => douyinApi.accountSave(p),
    onMutate: () => setActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts'] }); setEditOpen(false); toast('保存成功', 'success') },
    onError: (e: unknown, payload) => {
      const message = isCommercialDenial(e) ? commercialDenialMessage(e) : errMsg(e, '保存失败')
      setActionError(`${DOUYIN_ENDPOINTS.accountSave} 账号保存失败：${message}（${accountSaveContext(payload)}）`)
      toast(message, 'error')
    },
  })
  const delMut = useMutation({
    mutationFn: (account: DyAccount) => douyinApi.accountDelete(account.id),
    onMutate: () => setActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts'] }); setDeleteTarget(null); toast('删除成功', 'success') },
    onError: (e: unknown, account) => {
      setActionError(`${DOUYIN_ENDPOINTS.accountDelete} 账号删除失败：${errMsg(e, '删除失败')}（${accountContext(account)}）`)
      toast('删除失败', 'error')
    },
  })
  const syncMut = useMutation({
    mutationFn: (account: DyAccount) => douyinApi.videoSync(account.id),
    onMutate: () => setActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts'] }); toast('同步完成', 'success') },
    onError: (e: unknown, account) => {
      setActionError(`${DOUYIN_ENDPOINTS.videoSync} 账号视频同步失败：${errMsg(e, '同步失败')}（${accountContext(account)}）`)
      toast('同步失败', 'error')
    },
  })
  const batchSyncMut = useMutation({
    mutationFn: async (targets: DyAccount[]) => {
      let success = 0
      let fail = 0
      for (const account of targets) {
        try {
          await douyinApi.videoSync(account.id)
          success += 1
        } catch {
          fail += 1
        }
      }
      return { total: targets.length, success, fail }
    },
    onSuccess: (result) => {
      setActionError('')
      setBatchSyncResult(result)
      qc.invalidateQueries({ queryKey: ['dy-accounts'] })
      toast(`批量同步完成：成功 ${result.success}，失败 ${result.fail}`, result.fail > 0 ? 'warning' : 'success')
    },
    onError: (e) => {
      setBatchSyncResult({ total: rows.length, success: 0, fail: rows.length })
      setActionError(`${DOUYIN_ENDPOINTS.videoSync} 当前页批量同步失败：${errMsg(e, '同步任务异常')}（route=${DOUYIN_ROUTES.accounts}; page=${page}; rows=${pageSize}; accountCount=${rows.length}）`)
      toast(`批量同步失败：${errMsg(e, '同步任务异常')}`, 'error')
    },
  })

  const sourceRows = data?.list ?? (Array.isArray(data) ? data as DyAccount[] : [])
  const rows = authStatusFilter === 'all'
    ? sourceRows
    : sourceRows.filter(row => authStatusFilter === 'unbound'
      ? row.authStatus === 'unbound' || !row.authStatus
      : row.authStatus === authStatusFilter)
  const total = data?.total ?? sourceRows.length
  const validCount = rows.filter(r => r.authStatus === 'valid').length
  const expiredCount = rows.filter(r => r.authStatus === 'expired').length
  const unboundCount = rows.filter(r => r.authStatus === 'unbound' || !r.authStatus).length
  const unsyncedCount = rows.filter(r => !r.lastSyncTime).length
  const noVideoCount = rows.filter(r => Number(r.videoCount ?? 0) === 0).length

  const cols: GridColDef[] = [
    {
      field: 'accountName', headerName: '账号名称', flex: 1, minWidth: 150,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" spacing={1} sx={{ cursor: 'pointer', height: '100%' }}
          onClick={() => setDetailAcc(row as DyAccount)}>
          <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: 13 }}>
            {(row as DyAccount).accountName?.[0] ?? 'A'}
          </Avatar>
          <Typography variant="body2" fontWeight={500} noWrap>{(row as DyAccount).accountName}</Typography>
        </Stack>
      ),
    },
    { field: 'accountId', headerName: '抖音号', flex: 0.8, minWidth: 130, renderCell: ({ row }) => <Stack justifyContent="center" sx={{ height: '100%' }}><Typography variant="body2" color="text.secondary" noWrap>{(row as DyAccount).accountId}</Typography></Stack> },
    { field: 'fanCount', headerName: '粉丝数', flex: 0.6, minWidth: 90, renderCell: ({ row }) => fmt((row as DyAccount).fanCount ?? 0) },
    { field: 'totalLikes', headerName: '获赞数', flex: 0.6, minWidth: 90, renderCell: ({ row }) => fmt((row as DyAccount).totalLikes ?? 0) },
    { field: 'health', headerName: '健康评分', flex: 1, minWidth: 140, renderCell: ({ row }) => <HealthBar score={calcHealthScore(row as DyAccount)} /> },
    {
      field: 'authStatus', headerName: '授权状态', flex: 0.6, minWidth: 90,
      renderCell: ({ row }) => {
        const s = (row as DyAccount).authStatus
        if (s === 'valid') return <Chip size="small" label="已授权" color="success" />
        if (s === 'expired') return <Chip size="small" label="Token过期" color="error" />
        return <Chip size="small" label="未授权" color="default" />
      },
    },
    { field: 'lastSyncTime', headerName: '上次同步', flex: 0.8, minWidth: 120, renderCell: ({ row }) => { const t = (row as DyAccount).lastSyncTime; return t ? <Typography variant="caption">{formatDate(t)}</Typography> : <Typography variant="caption" color="text.secondary">未同步</Typography> } },
    {
      field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} sx={{ height: '100%', alignItems: 'center' }}>
          <Tooltip title="详情"><IconButton size="small" aria-label="详情" onClick={() => setDetailAcc(row as DyAccount)}><InfoOutlinedIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="同步"><IconButton size="small" aria-label="同步" onClick={() => syncMut.mutate(row as DyAccount)} disabled={syncMut.isPending}><SyncIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="编辑"><IconButton size="small" aria-label="编辑" onClick={() => { setActionError(''); setEditData({ id: (row as DyAccount).id, accountName: (row as DyAccount).accountName, accountId: (row as DyAccount).accountId, description: (row as DyAccount).description, status: (row as DyAccount).status }); setEditOpen(true) }}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="删除"><IconButton size="small" aria-label="删除" color="error" onClick={() => { setActionError(''); setDeleteTarget(row as DyAccount) }}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="douyin-account-list-tab"
      data-contract-scope="douyin-account-list-server-pagination"
      data-ready-endpoints={[
        DOUYIN_ENDPOINTS.accountSearch,
        DOUYIN_ENDPOINTS.accountSave,
        DOUYIN_ENDPOINTS.accountDelete,
        DOUYIN_ENDPOINTS.videoSync,
      ].join('|')}
      data-unsupported-endpoints={DOUYIN_UNSUPPORTED_ENDPOINTS}
      data-no-local-account-fallback="true"
      data-no-static-account-fallback="true"
      data-server-pagination="true"
      data-row-count={rows.length}
      data-total-count={total}
      data-list-error={isError ? 'true' : 'false'}
      sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden', gap: 1.5 }}
    >
      <Grid container spacing={1.5}>
        {[
          { label: '当前页账号', value: rows.length, hint: `服务端总数 ${total}` },
          { label: '授权正常', value: validCount, hint: '可同步视频和画像' },
          { label: 'Token异常', value: expiredCount + unboundCount, hint: `${expiredCount} 过期 / ${unboundCount} 未授权` },
          { label: '待补数据', value: unsyncedCount + noVideoCount, hint: `${unsyncedCount} 未同步 / ${noVideoCount} 无视频` },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      {isError && (
        <Alert
          data-testid="douyin-account-list-error"
          data-no-local-account-fallback="true"
          data-input-retained="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          {DOUYIN_ENDPOINTS.accountSearch} 抖音账号列表加载失败：{errMsg(error, '查询失败')}（route={DOUYIN_ROUTES.accounts}; accountName={accountName.trim() || '空'}; accountId={accountIdKeyword.trim() || '空'}; page={page}; rows={pageSize}）
        </Alert>
      )}
      {actionError && (
        <Alert data-testid="douyin-account-action-error" data-input-retained="true" data-row-retained="true" data-no-local-mutation="true" severity="error">
          {actionError}。失败不会本地移除账号、关闭关键弹窗或伪造同步成功。
        </Alert>
      )}
      {!isFetching && !isError && rows.length === 0 && (
        <Alert data-testid="douyin-account-empty" data-no-static-account-fallback="true" severity="warning">
          当前筛选条件下没有抖音账号。请绑定账号并完成 OAuth 授权后再执行同步。
        </Alert>
      )}
      {authStatusFilter !== 'all' && !isError && (
        <Alert severity="info">
          授权状态由前端按当前页本地筛选；后端 /douyin/account/search 当前只支持账号名称、抖音号、状态和分页。
        </Alert>
      )}
      <Alert severity="info">
        批量同步会对当前筛选页逐账号调用 `/douyin/video/sync`；后端暂未提供跨页批量任务，本页会保留成功/失败汇总。
      </Alert>
      {batchSyncResult && (
        <Alert severity={batchSyncResult.fail > 0 ? 'warning' : 'success'}>
          当前页批量同步结果：共 {batchSyncResult.total} 个账号，成功 {batchSyncResult.success} 个，失败 {batchSyncResult.fail} 个。
        </Alert>
      )}
      <StandardDataGrid
        sx={{ flex: 1, minHeight: 0 }}
        rows={rows} columns={cols} loading={isFetching}
        rowCount={authStatusFilter === 'all' ? total : rows.length} paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as DyAccount).id}
        rowHeight={52}
        searchSlot={
          <>
            <Tabs value={tab} onChange={(_, v) => onTabChange(v)} sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0 } }}>
              <Tab label="账号列表" />
              <Tab label="OAuth授权" />
            </Tabs>
            <TextField size="small" placeholder="搜索账号名称" value={accountName}
              onChange={e => { setAccountName(e.target.value); setPage(0) }} sx={{ width: 180 }} />
            <TextField size="small" placeholder="搜索抖音号" value={accountIdKeyword}
              onChange={e => { setAccountIdKeyword(e.target.value); setPage(0) }} sx={{ width: 160 }} />
            {(['all', 'valid', 'expired', 'unbound'] as const).map(v => (
              <Chip key={v}
                label={v === 'all' ? '全部' : v === 'valid' ? '已授权' : v === 'expired' ? 'Token过期' : '未绑定'}
                color={authStatusFilter === v ? 'primary' : 'default'}
                variant={authStatusFilter === v ? 'filled' : 'outlined'}
                size="small"
                onClick={() => { setAuthStatusFilter(v); setPage(0) }}
              />
            ))}
          </>
        }
        actionSlot={
          <>
            <Button
              size="small"
              variant="outlined"
              startIcon={<SyncIcon />}
              disabled={rows.length === 0 || batchSyncMut.isPending}
              onClick={() => batchSyncMut.mutate(rows)}
            >
              {batchSyncMut.isPending ? '批量同步中...' : '批量同步'}
            </Button>
            <Button size="small" variant="contained" startIcon={<AddIcon />}
              onClick={() => { setActionError(''); setEditData({}); setEditOpen(true) }}>绑定新账号</Button>
          </>
        }
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
      <AccountDetailDrawer account={detailAcc} onClose={() => setDetailAcc(null)} />
      <AccountEditDrawer
        open={editOpen} data={editData}
        onClose={() => setEditOpen(false)}
        onSave={v => saveMut.mutate(v)}
        loading={saveMut.isPending}
      />
      <ConfirmDialog
        open={deleteTarget !== null} title="确认删除" content={`此操作不可恢复，确认删除？endpoint=${DOUYIN_ENDPOINTS.accountDelete}; ${accountContext(deleteTarget)}。删除失败不会本地移除账号。`}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
// ─── OAuth Tab ───────────────────────────────────────────────────────────────
function OAuthTab({ tab, onTabChange }: { tab: number; onTabChange: (v: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [batchRefreshResult, setBatchRefreshResult] = useState<{ total: number; success: number; fail: number; skipped: number } | null>(null)
  const [oauthActionError, setOauthActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['dy-accounts-oauth', page, pageSize],
    queryFn: () => douyinApi.accountList({ page, rows: pageSize }),
  })

  const revokeMut = useMutation({
    mutationFn: (account: DyAccount) => douyinApi.oauthRevoke(account.id),
    onMutate: () => setOauthActionError(''),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dy-accounts-oauth'] }); toast('授权已撤销', 'success') },
    onError: (e: unknown, account) => {
      setOauthActionError(`${DOUYIN_ENDPOINTS.oauthRevoke} OAuth 撤销失败：${errMsg(e, '撤销失败')}（${accountContext(account)}）`)
      toast('撤销失败', 'error')
    },
  })
  const batchRefreshMut = useMutation({
    mutationFn: async (targets: DyAccount[]) => {
      let success = 0
      let fail = 0
      let skipped = 0
      for (const account of targets) {
        if (account.authStatus === 'unbound' || !account.authStatus) {
          skipped += 1
          continue
        }
        try {
          await douyinApi.tokenRefresh(account.id)
          success += 1
        } catch {
          fail += 1
        }
      }
      return { total: targets.length, success, fail, skipped }
    },
    onSuccess: (result) => {
      setOauthActionError('')
      setBatchRefreshResult(result)
      qc.invalidateQueries({ queryKey: ['dy-accounts-oauth'] })
      toast(`批量刷新完成：成功 ${result.success}，失败 ${result.fail}，跳过 ${result.skipped}`, result.fail > 0 ? 'warning' : 'success')
    },
    onError: (e) => {
      setBatchRefreshResult({ total: rows.length, success: 0, fail: rows.length, skipped: 0 })
      setOauthActionError(`${DOUYIN_ENDPOINTS.tokenRefresh} 当前页 Token 批量刷新失败：${errMsg(e, '刷新任务异常')}（route=${DOUYIN_ROUTES.accounts}; tab=OAuth授权; page=${page}; rows=${pageSize}; accountCount=${rows.length}）`)
      toast(`批量刷新失败：${errMsg(e, '刷新任务异常')}`, 'error')
    },
  })

  const rows = data?.list ?? (Array.isArray(data) ? data as DyAccount[] : [])
  const total = data?.total ?? rows.length

  const cols: GridColDef[] = [
    { field: 'accountName', headerName: '账号名称', width: 180 },
    {
      field: 'accountId', headerName: '账号ID', width: 180,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" spacing={0.5} sx={{ height: '100%' }}>
          <Typography variant="body2">{(row as DyAccount).accountId}</Typography>
          <IconButton size="small" aria-label="复制账号ID" onClick={() => {
            navigator.clipboard.writeText((row as DyAccount).accountId ?? '')
            toast('已复制', 'success')
          }}>
            <ContentCopyIcon sx={{ fontSize: 14 }} />
          </IconButton>
        </Stack>
      ),
    },
    {
      field: 'scopes', headerName: '授权范围', width: 200,
      renderCell: () => (
        <Stack justifyContent="center" sx={{ height: '100%' }}>
          <Typography variant="caption" color="text.secondary">
            列表接口未返回 scope
          </Typography>
        </Stack>
      ),
    },
    {
      field: 'expiry', headerName: 'Token到期', width: 220,
      renderCell: ({ row }) => {
        const acc = row as DyAccount
        const color = acc.authStatus === 'valid' ? 'primary' : acc.authStatus === 'expired' ? 'error' : 'warning'
        const pct = acc.authStatus === 'valid' ? 100 : acc.authStatus === 'expired' ? 0 : 0
        return (
          <Stack justifyContent="center" sx={{ height: '100%', width: '100%' }}>
            <LinearProgress variant="determinate" value={pct} color={color} sx={{ height: 4, borderRadius: 2 }} />
            <Typography variant="caption" color="text.secondary">
              {acc.authStatus === 'valid' ? '列表状态有效，到期进详情核验' : acc.authStatus === 'expired' ? '列表状态已过期' : '未授权'}
            </Typography>
          </Stack>
        )
      },
    },
    {
      field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={1} sx={{ height: '100%', alignItems: 'center' }}>
          <Button size="small" variant="outlined" startIcon={<LinkIcon />}
            onClick={async () => {
              try {
                setOauthActionError('')
                const r = await douyinApi.oauthUrl((row as DyAccount).id)
                window.open(r.authUrl, '_blank')
              } catch (e) {
                setOauthActionError(`${DOUYIN_ENDPOINTS.oauthUrl} 获取授权链接失败：${errMsg(e, 'OAuth 服务异常')}（${accountContext(row as DyAccount)}）`)
                toast('获取授权链接失败', 'error')
              }
            }}>重新授权</Button>
          <Button size="small" variant="outlined" color="error"
            onClick={() => revokeMut.mutate(row as DyAccount)}
            disabled={revokeMut.isPending}>撤销</Button>
        </Stack>
      ),
    },
  ]

  const stats = {
    valid: rows.filter(r => r.authStatus === 'valid').length,
    unbound: rows.filter(r => r.authStatus === 'unbound' || !r.authStatus).length,
    expired: rows.filter(r => r.authStatus === 'expired').length,
  }

  return (
    <Box
      data-testid="douyin-oauth-tab"
      data-contract-scope="douyin-oauth-current-page-actions"
      data-ready-endpoints={[
        DOUYIN_ENDPOINTS.accountSearch,
        DOUYIN_ENDPOINTS.oauthUrl,
        DOUYIN_ENDPOINTS.oauthRevoke,
        DOUYIN_ENDPOINTS.tokenRefresh,
        DOUYIN_ENDPOINTS.tokenStatus,
      ].join('|')}
      data-unsupported-endpoints={DOUYIN_UNSUPPORTED_ENDPOINTS}
      data-no-local-token-fallback="true"
      data-no-static-oauth-fallback="true"
      data-server-pagination="true"
      data-row-count={rows.length}
      data-total-count={total}
      data-list-error={isError ? 'true' : 'false'}
      sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden', gap: 1.5 }}
    >
      <Grid container spacing={1.5}>
        {[
          { label: 'OAuth账号', value: rows.length, hint: `服务端总数 ${total}` },
          { label: '正常', value: stats.valid, hint: 'Token 可用' },
          { label: '已过期', value: stats.expired, hint: '需要重新授权或刷新' },
          { label: '未授权', value: stats.unbound, hint: '需要打开授权链接' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      {isError && (
        <Alert
          data-testid="douyin-oauth-list-error"
          data-no-local-token-fallback="true"
          data-input-retained="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          {DOUYIN_ENDPOINTS.accountSearch} OAuth 账号列表加载失败：{errMsg(error, '查询失败')}（route={DOUYIN_ROUTES.accounts}; tab=OAuth授权; page={page}; rows={pageSize}）
        </Alert>
      )}
      {oauthActionError && (
        <Alert data-testid="douyin-oauth-action-error" data-row-retained="true" data-no-local-token-mutation="true" severity="error">
          {oauthActionError}。失败不会本地改写授权状态。
        </Alert>
      )}
      {!isFetching && !isError && rows.length === 0 && (
        <Alert data-testid="douyin-oauth-empty" data-no-static-oauth-fallback="true" severity="warning">
          暂无可管理的 OAuth 账号。请先在账号列表中绑定账号。
        </Alert>
      )}
      <Alert severity="info">
        批量刷新会对当前页已授权或过期账号逐个调用 `/douyin/oauth/token-refresh`，未授权账号跳过；Token 精确到期时间需要进入详情调用 `/douyin/oauth/token-status` 核验，导出报告基于当前页列表生成 CSV。
      </Alert>
      {batchRefreshResult && (
        <Alert severity={batchRefreshResult.fail > 0 ? 'warning' : 'success'}>
          当前页 Token 刷新结果：共 {batchRefreshResult.total} 个账号，成功 {batchRefreshResult.success} 个，失败 {batchRefreshResult.fail} 个，跳过 {batchRefreshResult.skipped} 个。
        </Alert>
      )}
      <StandardDataGrid
        sx={{ flex: 1, minHeight: 0 }}
        rows={rows} columns={cols} loading={isFetching}
        rowCount={total} paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={m => { setPage(m.page); setPageSize(m.pageSize) }}
        getRowId={r => (r as DyAccount).id}
        searchSlot={
          <>
            <Tabs value={tab} onChange={(_, v) => onTabChange(v)} sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0 } }}>
              <Tab label="账号列表" />
              <Tab label="OAuth授权" />
            </Tabs>
            <Chip size="small" color="success" variant="outlined" label={`正常 ${stats.valid}`} />
            <Chip size="small" color="warning" variant="outlined" label={`未授权 ${stats.unbound}`} />
            <Chip size="small" color="error" variant="outlined" label={`已过期 ${stats.expired}`} />
          </>
        }
        actionSlot={
          <>
            <Button
              size="small"
              variant="outlined"
              startIcon={<RefreshIcon />}
              disabled={rows.length === 0 || batchRefreshMut.isPending}
              onClick={() => batchRefreshMut.mutate(rows)}
            >
              {batchRefreshMut.isPending ? '批量刷新中...' : '批量刷新'}
            </Button>
            <Button
              size="small"
              variant="outlined"
              startIcon={<FileDownloadIcon />}
              disabled={rows.length === 0}
              onClick={() => {
                exportOauthReport(rows)
                toast(`已导出当前页 ${rows.length} 个 OAuth 账号`, 'success')
              }}
            >
              导出报告
            </Button>
          </>
        }
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
    </Box>
  )
}

// ─── Main Export ──────────────────────────────────────────────────────────────
export default function AccountsPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box
      data-testid="douyin-accounts-page"
      data-contract-scope="douyin-account-oauth-persona-fan-profile-workbench"
      data-ready-endpoints={DOUYIN_READY_ENDPOINTS}
      data-unsupported-endpoints={DOUYIN_UNSUPPORTED_ENDPOINTS}
      data-no-local-account-fallback="true"
      data-no-static-account-fallback="true"
      data-no-local-token-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden', p: 2, gap: 2 }}
    >
      <PageHeader
        title="抖音账号"
        subtitle="账号、授权、人设、粉丝画像和近期视频都从真实接口读取；接口失败时会直接在页面上展示降级原因。"
        breadcrumbs={[{ label: '抖音运营' }, { label: '账号工作台' }]}
      />
      {tab === 0 && <AccountListTab tab={tab} onTabChange={setTab} />}
      {tab === 1 && <OAuthTab tab={tab} onTabChange={setTab} />}
    </Box>
  )
}
