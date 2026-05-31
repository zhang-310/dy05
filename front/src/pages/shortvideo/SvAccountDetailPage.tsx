import { useState, useMemo } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Avatar,
  Stack,
  Chip,
  Grid,
  Button,
  Tabs,
  Tab,
  TextField,
  IconButton,
  Tooltip,
  Divider,
  LinearProgress,
  Alert,
  MenuItem,
  Paper,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'
import EditIcon from '@mui/icons-material/Edit'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/Cancel'
import VerifiedIcon from '@mui/icons-material/Verified'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import PsychologyIcon from '@mui/icons-material/Psychology'
import type { GridColDef, GridRowSelectionModel, GridSortModel } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import {
  accountGet,
  accountUpdate,
  accountVideos,
  accountRefreshStats,
  accountAnalytics,
  type AccountVideo,
  type AccountVideosQueryParams,
  type SvAccountAnalytics,
  type SvAccountDetail,
} from '@/api/sv-account'
import { batchDeepAnalyze } from '@/api/viral-analysis'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'

function formatNumber(num: number | null | undefined): string {
  if (num == null) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return num.toString()
}

function formatFloat(n: number | null | undefined, digits = 1): string {
  if (n == null || Number.isNaN(n)) return '0'
  return n.toFixed(digits)
}

const ACCOUNT_DETAIL_ENDPOINTS = {
  get: '/short-video/account/get',
  update: '/short-video/account/update',
  refreshStats: '/short-video/account/refresh-stats',
  videos: '/short-video/account/videos',
  analytics: '/short-video/account/analytics',
  deepAnalyzeBatch: '/short-video/viral/deep-analyze/batch',
} as const
const ACCOUNT_DETAIL_READY_ENDPOINTS = [
  ACCOUNT_DETAIL_ENDPOINTS.get,
  ACCOUNT_DETAIL_ENDPOINTS.update,
  ACCOUNT_DETAIL_ENDPOINTS.refreshStats,
  ACCOUNT_DETAIL_ENDPOINTS.videos,
  ACCOUNT_DETAIL_ENDPOINTS.analytics,
  ACCOUNT_DETAIL_ENDPOINTS.deepAnalyzeBatch,
] as const
const ACCOUNT_DETAIL_UNSUPPORTED_ENDPOINTS = [
  '/short-video/account/mock',
  '/short-video/account/local-get',
  '/short-video/account/local-update',
  '/short-video/account/local-refresh-stats',
  '/short-video/account/local-videos',
  '/short-video/account/static-analytics',
  '/short-video/viral/local-deep-analyze',
  '/short-video/viral/static-deep-progress',
] as const
const ACCOUNT_DETAIL_READY_ROUTES = [
  shortvideoRoutes.accounts,
  `${shortvideoRoutes.accounts}/:id`,
  `${shortvideoRoutes.accounts}/:id?tab=videos`,
  `${shortvideoRoutes.accounts}/:id?tab=analysis`,
  `${shortvideoRoutes.viralVideos}?videoId=:id`,
].join('|')
const ACCOUNT_DETAIL_SUPPORTED_ACTIONS = [
  'refresh-account-stats',
  'edit-account-metadata',
  'server-filter-account-videos',
  'navigate-viral-detail',
  'submit-single-deep-analyze',
  'submit-selected-deep-analyze',
  'submit-page-pending-deep-analyze',
  'view-account-analytics',
].join('|')

function AccountAnalyticsPanel(props: {
  account: SvAccountDetail
  analytics: SvAccountAnalytics | undefined
  loading: boolean
}) {
  const { account, analytics: a, loading } = props

  if (loading && !a) {
    return null
  }

  if (!a || a.videoCount <= 0) {
    return (
      <Stack spacing={2} data-testid="sv-account-analytics-empty" data-no-static-analytics-fallback="true">
        <Alert severity="warning">
          当前账号在系统中尚无已采集入库的短视频，请先通过「账号视频采集」拉取作品后再查看综合分析。
        </Alert>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>
                  详情页快照（账号表）
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  平均播放 {formatNumber(account.avgViewCount)} · 平均点赞 {formatNumber(account.avgLikeCount)} · 任务 {account.taskCount ?? 0} 个
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Stack>
    )
  }

  const deepTotal =
    a.deepPendingCount + a.deepProcessingCount + a.deepCompletedCount + a.deepFailedCount + a.deepOtherCount

  return (
    <Stack spacing={2} data-testid="sv-account-analytics-panel" data-source-endpoint="/short-video/account/analytics">
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                已采集视频条数
              </Typography>
              <Typography variant="h5">{a.videoCount}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                条均播放 / 累计播放
              </Typography>
              <Typography variant="h5">{formatNumber(Math.round(a.avgViewPerVideo))}</Typography>
              <Typography variant="caption" color="text.secondary">
                累计 {formatNumber(a.sumViewCount)} · 单条最高 {formatNumber(a.maxViewCount)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                条均点赞 / 累计点赞
              </Typography>
              <Typography variant="h5">{formatNumber(Math.round(a.avgLikePerVideo))}</Typography>
              <Typography variant="caption" color="text.secondary">累计 {formatNumber(a.sumLikeCount)}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                条均分享 / 爆款评分均值
              </Typography>
              <Typography variant="h5">{formatNumber(Math.round(a.avgSharePerVideo))}</Typography>
              <Typography variant="caption" color="text.secondary">
                评分均值 {formatFloat(a.avgViralScore, 2)}（单条最低播放 {formatNumber(a.minViewCount)}）
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            深度分析状态（全量已采视频）
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
            <Chip size="small" label={`待分析 ${a.deepPendingCount}`} variant="outlined" />
            <Chip size="small" label={`分析中 ${a.deepProcessingCount}`} color="warning" variant="outlined" />
            <Chip size="small" label={`已完成 ${a.deepCompletedCount}`} color="success" variant="outlined" />
            <Chip size="small" label={`失败 ${a.deepFailedCount}`} color="error" variant="outlined" />
            {a.deepOtherCount > 0 && <Chip size="small" label={`其它 ${a.deepOtherCount}`} variant="outlined" />}
          </Stack>
          {deepTotal > 0 && (
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
              合计 {deepTotal} 条记录；详情页「采集任务」统计：已分析 {account.analyzedCount ?? 0} / 待分析 {account.pendingAnalysisCount ?? 0}
            </Typography>
          )}
        </CardContent>
      </Card>
    </Stack>
  )
}

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  )
}

function tabIndexFromSearchParams(searchParams: URLSearchParams): number {
  const t = searchParams.get('tab')
  if (t === 'videos') return 1
  if (t === 'analysis') return 2
  return 0
}

export default function SvAccountDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const toast = useToast()
  const qc = useQueryClient()

  const tabValue = useMemo(() => tabIndexFromSearchParams(searchParams), [searchParams])

  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({
    accountCategory: '',
    industryTags: '',
    contentTags: '',
    notes: '',
  })

  const [videoQuery, setVideoQuery] = useState<{
    page: number
    pageSize: number
    keyword: string
    keywordDraft: string
    deepAnalyzeStatus: string
    sortName: NonNullable<AccountVideosQueryParams['sortName']>
    sortOrder: 'asc' | 'desc'
  }>({
    page: 0,
    pageSize: 20,
    keyword: '',
    keywordDraft: '',
    deepAnalyzeStatus: '',
    sortName: 'createTime',
    sortOrder: 'desc',
  })

  const [videoRowSelection, setVideoRowSelection] = useState<GridRowSelectionModel>([])

  const accountId = Number(id)

  const VIRAL_BATCH_MAX = 30

  // 查询账号详情
  const { data: account, isLoading: accountLoading, isError: accountIsError, error: accountError, refetch: refetchAccount } = useQuery({
    queryKey: ['sv-account', accountId],
    queryFn: () => accountGet(accountId),
    enabled: !!accountId,
  })
  const [updateError, setUpdateError] = useState<string | null>(null)
  const [refreshError, setRefreshError] = useState<string | null>(null)
  const [deepAnalyzeError, setDeepAnalyzeError] = useState<string | null>(null)

  // 查询账号视频（该账号下已采集的全部爆款库视频，分页 / 筛选 / 排序）
  const { data: videosData, isFetching: videosFetching, isError: videosIsError, error: videosError, refetch: refetchVideos } = useQuery({
    queryKey: ['sv-account-videos', accountId, videoQuery],
    queryFn: () =>
      accountVideos({
        accountId,
        page: videoQuery.page,
        rows: videoQuery.pageSize,
        keyword: videoQuery.keyword.trim() || undefined,
        deepAnalyzeStatus: videoQuery.deepAnalyzeStatus || undefined,
        sortName: videoQuery.sortName,
        sortOrder: videoQuery.sortOrder,
      }),
    enabled: !!accountId && tabValue === 1,
    refetchInterval: (q) => {
      if (tabValue !== 1) return false
      const rows = (q.state.data?.list ?? []) as AccountVideo[]
      const busy = rows.some((r) => r.deepAnalyzeStatus === 'processing')
      return busy ? 6000 : false
    },
  })

  const batchDeepMut = useMutation({
    mutationFn: (ids: number[]) => batchDeepAnalyze(ids),
    onSuccess: () => {
      toast('深度拆解任务已提交', 'success')
      setDeepAnalyzeError(null)
      setVideoRowSelection([])
      void qc.invalidateQueries({ queryKey: ['sv-account-videos', accountId] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setDeepAnalyzeError(message)
      toast(`深度拆解提交失败：${message}`, 'error')
    },
  })

  const { data: analyticsData, isFetching: analyticsFetching, isError: analyticsIsError, error: analyticsError, refetch: refetchAnalytics } = useQuery({
    queryKey: ['sv-account-analytics', accountId],
    queryFn: () => accountAnalytics(accountId),
    enabled: !!accountId && tabValue === 2,
  })

  const handleTabChange = (_: React.SyntheticEvent, v: number) => {
    const keys = ['info', 'videos', 'analysis'] as const
    if (v === 0) {
      setSearchParams({}, { replace: true })
    } else {
      setSearchParams({ tab: keys[v] }, { replace: true })
    }
  }

  // 更新账号
  const updateMut = useMutation({
    mutationFn: (params: Parameters<typeof accountUpdate>[0]) => accountUpdate(params),
    onSuccess: () => {
      toast('账号信息已更新', 'success')
      setUpdateError(null)
      setEditing(false)
      void qc.invalidateQueries({ queryKey: ['sv-account', accountId] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setUpdateError(message)
      toast(`账号信息更新失败：${message}`, 'error')
    },
  })

  // 刷新统计
  const refreshMut = useMutation({
    mutationFn: () => accountRefreshStats(accountId),
    onSuccess: () => {
      toast('统计数据已刷新', 'success')
      setRefreshError(null)
      void qc.invalidateQueries({ queryKey: ['sv-account', accountId] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setRefreshError(message)
      toast(`刷新统计失败：${message}`, 'error')
    },
  })

  const handleEdit = () => {
    if (account) {
      setEditForm({
        accountCategory: account.accountCategory || '',
        industryTags: account.industryTags || '',
        contentTags: account.contentTags || '',
        notes: account.notes || '',
      })
      setEditing(true)
    }
  }

  const handleSave = () => {
    updateMut.mutate({
      id: accountId,
      ...editForm,
    })
  }

  const videoSortModel: GridSortModel = [{ field: videoQuery.sortName, sort: videoQuery.sortOrder }]

  const onVideoSortModelChange = (model: GridSortModel) => {
    const m = model[0]
    if (!m?.field || !m.sort) return
    const allowed = new Set(['createTime', 'viewCount', 'viralScore', 'updateTime', 'id'])
    if (!allowed.has(String(m.field))) return
    setVideoQuery((q) => ({
      ...q,
      sortName: m.field as NonNullable<AccountVideosQueryParams['sortName']>,
      sortOrder: m.sort as 'asc' | 'desc',
      page: 0,
    }))
  }

  const applyVideoFilters = () => {
    setVideoQuery((q) => ({ ...q, page: 0, keyword: q.keywordDraft.trim() }))
  }

  const videoColumns: GridColDef<AccountVideo>[] = [
    { field: 'id', headerName: 'ID', width: 72, sortable: true },
    {
      field: 'title',
      headerName: '标题',
      flex: 1,
      minWidth: 200,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ minWidth: 0, width: '100%', py: 0.25 }}>
          {row.coverUrl ? (
            <Box
              component="img"
              src={row.coverUrl}
              alt=""
              sx={{
                width: 44,
                height: 58,
                flexShrink: 0,
                objectFit: 'cover',
                borderRadius: 1,
                bgcolor: 'action.hover',
              }}
            />
          ) : null}
          <Typography variant="body2" noWrap sx={{ minWidth: 0, lineHeight: 1.35 }}>
            {row.title || '（无标题）'}
          </Typography>
        </Stack>
      ),
    },
    {
      field: 'viewCount',
      headerName: '播放量',
      width: 100,
      sortable: true,
      renderCell: ({ value }) => formatNumber(value as number),
    },
    {
      field: 'likeCount',
      headerName: '点赞数',
      width: 100,
      sortable: false,
      renderCell: ({ value }) => formatNumber(value as number),
    },
    {
      field: 'viralScore',
      headerName: '爆款评分',
      width: 100,
      sortable: true,
      renderCell: ({ value }) => {
        const score = Number(value) || 0
        const color = score >= 80 ? 'success' : score >= 60 ? 'warning' : 'default'
        return <Chip label={score} color={color} size="small" />
      },
    },
    {
      field: 'deepAnalyzeStatus',
      headerName: '分析状态',
      width: 108,
      sortable: false,
      renderCell: ({ value }) => {
        const statusMap: Record<string, { label: string; color: 'default' | 'warning' | 'success' | 'error' }> = {
          pending: { label: '待分析', color: 'default' },
          processing: { label: '分析中', color: 'warning' },
          completed: { label: '已完成', color: 'success' },
          failed: { label: '失败', color: 'error' },
        }
        const s = statusMap[value as string] || { label: String(value ?? '—'), color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    {
      field: 'deepAnalyzeProgress',
      headerName: '进度摘要',
      width: 140,
      sortable: false,
      renderCell: ({ row }) => {
        const t = row.deepAnalyzeProgress?.trim()
        if (!t) return <Typography variant="caption" color="text.secondary">—</Typography>
        const short = t.length > 72 ? `${t.slice(0, 72)}…` : t
        return (
          <Tooltip title={t}>
            <Typography variant="caption" noWrap sx={{ maxWidth: 130, display: 'block' }}>
              {short}
            </Typography>
          </Tooltip>
        )
      },
    },
    {
      field: 'createTime',
      headerName: '采集时间',
      width: 168,
      sortable: true,
      renderCell: ({ value }) => formatDate(value as string),
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 112,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.25}>
          <Tooltip title="爆款视频页打开详情">
            <IconButton
              size="small"
              onClick={() => navigate(`${shortvideoRoutes.viralVideos}?videoId=${row.id}`)}
              aria-label="open-viral-detail"
              data-testid="sv-account-video-open-viral-button"
              data-target-route={`${shortvideoRoutes.viralVideos}?videoId=${row.id}`}
            >
              <OpenInNewIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="提交深度拆解（异步）">
            <span>
              <IconButton
                size="small"
                color="secondary"
                disabled={batchDeepMut.isPending || row.deepAnalyzeStatus === 'processing'}
                onClick={() => batchDeepMut.mutate([row.id])}
                aria-label="deep-analyze"
                data-testid="sv-account-video-deep-analyze-button"
                data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.deepAnalyzeBatch}
                data-no-local-deep-analyze="true"
              >
                <PsychologyIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  if (accountLoading && !account) {
    return (
      <Box
        data-testid="sv-account-detail-page"
        data-ready-endpoints={ACCOUNT_DETAIL_READY_ENDPOINTS.join('|')}
        data-ready-routes={ACCOUNT_DETAIL_READY_ROUTES}
        data-supported-actions={ACCOUNT_DETAIL_SUPPORTED_ACTIONS}
        data-unsupported-endpoints={ACCOUNT_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')}
        data-no-local-account-fallback="true"
        data-no-static-analytics-fallback="true"
        sx={{ p: 3 }}
      >
        <Typography>加载中...</Typography>
      </Box>
    )
  }

  if (accountIsError || !account) {
    return (
      <Box
        data-testid="sv-account-detail-page"
        data-ready-endpoints={ACCOUNT_DETAIL_READY_ENDPOINTS.join('|')}
        data-ready-routes={ACCOUNT_DETAIL_READY_ROUTES}
        data-supported-actions={ACCOUNT_DETAIL_SUPPORTED_ACTIONS}
        data-unsupported-endpoints={ACCOUNT_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')}
        data-no-local-account-fallback="true"
        data-no-static-analytics-fallback="true"
        sx={{ p: 3 }}
      >
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} alignItems="center">
            <IconButton
              onClick={() => navigate(shortvideoRoutes.accounts)}
              data-testid="sv-account-detail-back-button"
              data-target-route={shortvideoRoutes.accounts}
            >
              <ArrowBackIcon />
            </IconButton>
            <Typography variant="h5">账号详情</Typography>
          </Stack>
          <Alert
            data-testid="sv-account-detail-load-error"
            data-no-local-account-fallback="true"
            severity="error"
            action={<Button color="inherit" size="small" onClick={() => void refetchAccount()}>重试</Button>}
          >
            账号详情加载失败（POST {ACCOUNT_DETAIL_ENDPOINTS.get}）：{getErrorMessage(accountError)}。请检查账号 ID 和当前用户数据权限。
          </Alert>
        </Stack>
      </Box>
    )
  }

  return (
    <Box
      data-testid="sv-account-detail-page"
      data-ready-endpoints={ACCOUNT_DETAIL_READY_ENDPOINTS.join('|')}
      data-ready-routes={ACCOUNT_DETAIL_READY_ROUTES}
      data-supported-actions={ACCOUNT_DETAIL_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={ACCOUNT_DETAIL_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-account-fallback="true"
      data-no-local-video-fallback="true"
      data-no-static-analytics-fallback="true"
    >
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <IconButton
          onClick={() => navigate(shortvideoRoutes.accounts)}
          data-testid="sv-account-detail-back-button"
          data-target-route={shortvideoRoutes.accounts}
        >
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5">账号详情</Typography>
        <Box sx={{ flex: 1 }} />
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refreshMut.mutate()}
            disabled={refreshMut.isPending}
            data-testid="sv-account-refresh-stats-button"
            data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.refreshStats}
            data-no-local-refresh-stats="true"
          >
            刷新统计
          </Button>
          {!editing && (
            <Button
              variant="outlined"
              startIcon={<EditIcon />}
              onClick={handleEdit}
              data-testid="sv-account-edit-button"
              data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.update}
            >
              编辑
            </Button>
          )}
        </Stack>
      </Stack>

      {refreshError && (
        <Alert data-testid="sv-account-refresh-error" data-no-local-refresh-stats="true" severity="error" sx={{ mb: 2 }}>
          刷新统计失败（POST {ACCOUNT_DETAIL_ENDPOINTS.refreshStats}）：{refreshError}。当前详情数据保持不变。
        </Alert>
      )}

      {/* 基本信息卡片 */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" spacing={3} alignItems="flex-start">
            <Avatar src={account.avatarUrl} sx={{ width: 80, height: 80 }}>
              {account.nickname?.charAt(0) || '?'}
            </Avatar>

            <Box sx={{ flex: 1 }}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                <Typography variant="h6">{account.nickname || account.douyinId}</Typography>
                {account.isVerified && (
                  <Tooltip title={`${account.verificationType || '已认证'}`}>
                    <VerifiedIcon color="primary" fontSize="small" />
                  </Tooltip>
                )}
              </Stack>

              {account.signature && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  {account.signature}
                </Typography>
              )}

              <Grid container spacing={3}>
                <Grid item xs={6} sm={3}>
                  <Typography variant="caption" color="text.secondary">
                    粉丝数
                  </Typography>
                  <Typography variant="h6">{formatNumber(account.followerCount)}</Typography>
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Typography variant="caption" color="text.secondary">
                    获赞总数
                  </Typography>
                  <Typography variant="h6">{formatNumber(account.totalFavorited)}</Typography>
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Typography variant="caption" color="text.secondary">
                    作品数
                  </Typography>
                  <Typography variant="h6">{account.videoCount}</Typography>
                </Grid>
                <Grid item xs={6} sm={3}>
                  <Typography variant="caption" color="text.secondary">
                    采集视频
                  </Typography>
                  <Typography variant="h6">{account.totalCollectedVideos}</Typography>
                </Grid>
              </Grid>
            </Box>

            <Box>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">
                    平均评分
                  </Typography>
                  <Typography variant="h6" color="primary">
                    {Number(account.avgViralScore).toFixed(1)}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="caption" color="text.secondary">
                    最高评分
                  </Typography>
                  <Typography variant="h6" color="success.main">
                    {Number(account.topViralScore).toFixed(1)}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">
                    采集次数
                  </Typography>
                  <Typography variant="h6">{account.collectCount}</Typography>
                </Grid>
              </Grid>
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* Tabs：支持 ?tab=videos | analysis 深链 */}
      <Card>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="详细信息" />
          <Tab label={`采集短视频 (${account.totalCollectedVideos})`} />
          <Tab label="综合分析" />
        </Tabs>

        <Divider />

        {/* Tab 1: 详细信息 */}
        <TabPanel value={tabValue} index={0}>
          {updateError && (
            <Alert data-testid="sv-account-update-error" data-input-retained="true" data-no-local-account-mutation="true" severity="error" sx={{ mb: 2 }}>
              保存账号信息失败（POST {ACCOUNT_DETAIL_ENDPOINTS.update}）：{updateError}。编辑内容已保留。
            </Alert>
          )}
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>
                账号分类
              </Typography>
              {editing ? (
                <TextField
                  fullWidth
                  size="small"
                  value={editForm.accountCategory}
                  onChange={(e) => setEditForm((f) => ({ ...f, accountCategory: e.target.value }))}
                  placeholder="如：美妆、护肤、彩妆"
                />
              ) : (
                <Typography variant="body2" color="text.secondary">
                  {account.accountCategory || '未设置'}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>
                行业标签
              </Typography>
              {editing ? (
                <TextField
                  fullWidth
                  size="small"
                  value={editForm.industryTags}
                  onChange={(e) => setEditForm((f) => ({ ...f, industryTags: e.target.value }))}
                  placeholder='JSON 数组，如：["护肤","彩妆"]'
                />
              ) : (
                <Typography variant="body2" color="text.secondary">
                  {account.industryTags || '未设置'}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>
                内容标签
              </Typography>
              {editing ? (
                <TextField
                  fullWidth
                  size="small"
                  value={editForm.contentTags}
                  onChange={(e) => setEditForm((f) => ({ ...f, contentTags: e.target.value }))}
                  placeholder='JSON 数组，如：["教程","测评"]'
                />
              ) : (
                <Typography variant="body2" color="text.secondary">
                  {account.contentTags || '未设置'}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>
                来源信息
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {account.sourceType === 'keyword_search' && account.sourceKeyword
                  ? `关键词采集：${account.sourceKeyword}`
                  : account.sourceType === 'manual'
                  ? '手动添加'
                  : account.sourceType}
              </Typography>
            </Grid>

            <Grid item xs={12}>
              <Typography variant="subtitle2" gutterBottom>
                备注
              </Typography>
              {editing ? (
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  value={editForm.notes}
                  onChange={(e) => setEditForm((f) => ({ ...f, notes: e.target.value }))}
                  placeholder="添加备注信息"
                />
              ) : (
                <Typography variant="body2" color="text.secondary">
                  {account.notes || '无'}
                </Typography>
              )}
            </Grid>

            {editing && (
              <Grid item xs={12}>
                <Stack direction="row" spacing={2}>
                  <Button
                    variant="contained"
                    startIcon={<SaveIcon />}
                    onClick={handleSave}
                    disabled={updateMut.isPending}
                    data-testid="sv-account-save-button"
                    data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.update}
                    data-no-local-account-mutation="true"
                  >
                    保存
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<CancelIcon />}
                    onClick={() => setEditing(false)}
                    data-testid="sv-account-cancel-edit-button"
                  >
                    取消
                  </Button>
                </Stack>
              </Grid>
            )}
          </Grid>
        </TabPanel>

        {/* Tab 2: 采集视频 */}
        <TabPanel value={tabValue} index={1}>
          <Alert
            severity="info"
            variant="outlined"
            sx={{ mb: 2, '& .MuiAlert-message': { width: '100%' } }}
          >
            <Typography variant="body2" color="text.secondary">
              与抖音主页「作品数」可能不一致。列表不含完整拆解正文，请在爆款页查看详情。「分析中」时本表会自动刷新。
            </Typography>
          </Alert>

          {videosIsError && (
            <Alert
              data-testid="sv-account-videos-error"
              data-input-retained="true"
              data-no-local-video-fallback="true"
              severity="error"
              sx={{ mb: 2 }}
              action={<Button color="inherit" size="small" onClick={() => void refetchVideos()}>重试</Button>}
            >
              账号视频加载失败（POST {ACCOUNT_DETAIL_ENDPOINTS.videos}）：{getErrorMessage(videosError)}。请检查爆款库账号归属和采集任务入库状态；页面不会补本地视频。
            </Alert>
          )}

          {deepAnalyzeError && (
            <Alert data-testid="sv-account-deep-analyze-error" data-input-retained="true" data-no-local-deep-analyze="true" severity="error" sx={{ mb: 2 }}>
              深度拆解提交失败（POST {ACCOUNT_DETAIL_ENDPOINTS.deepAnalyzeBatch}）：{deepAnalyzeError}。当前勾选视频已保留。
            </Alert>
          )}

          <Paper
            variant="outlined"
            sx={{
              mb: 2,
              borderRadius: 2,
              overflow: 'hidden',
            }}
          >
            <Box
              data-testid="sv-account-video-filter-header"
              sx={{
                px: 2,
                py: 1.25,
                bgcolor: (t) => (t.palette.mode === 'dark' ? alpha(t.palette.common.white, 0.04) : t.palette.grey[50]),
                borderBottom: 1,
                borderColor: 'divider',
              }}
            >
              <Typography variant="subtitle2" fontWeight={600}>
                筛选
              </Typography>
            </Box>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              alignItems={{ xs: 'stretch', sm: 'flex-start' }}
              flexWrap="wrap"
              useFlexGap
              sx={{ p: 2 }}
            >
              <TextField
                size="small"
                fullWidth
                label="标题关键词"
                placeholder="模糊匹配"
                value={videoQuery.keywordDraft}
                onChange={(e) => setVideoQuery((q) => ({ ...q, keywordDraft: e.target.value }))}
                onKeyDown={(e) => e.key === 'Enter' && applyVideoFilters()}
                sx={{ flex: { sm: '1 1 220px' }, minWidth: { sm: 200 }, maxWidth: { sm: 360 } }}
              />
              <TextField
                select
                size="small"
                label="深度分析状态"
                value={videoQuery.deepAnalyzeStatus}
                onChange={(e) => setVideoQuery((q) => ({ ...q, deepAnalyzeStatus: e.target.value, page: 0 }))}
                sx={{ flex: { sm: '0 0 168px' }, minWidth: { sm: 168 } }}
              >
                <MenuItem value="">全部</MenuItem>
                <MenuItem value="pending">待分析</MenuItem>
                <MenuItem value="processing">分析中</MenuItem>
                <MenuItem value="completed">已完成</MenuItem>
                <MenuItem value="failed">失败</MenuItem>
              </TextField>
              <Stack direction="row" spacing={1} sx={{ pt: { xs: 0, sm: 0.5 } }}>
                <Button
                  variant="contained"
                  size="small"
                  disableElevation
                  onClick={applyVideoFilters}
                  data-testid="sv-account-videos-apply-filter-button"
                  data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.videos}
                >
                  应用筛选
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<RefreshIcon />}
                  onClick={() => void refetchVideos()}
                  data-testid="sv-account-videos-refresh-button"
                  data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.videos}
                >
                  刷新
                </Button>
              </Stack>
            </Stack>

            <Divider />

            <Box
              data-testid="sv-account-video-batch-panel"
              sx={{
                px: 2,
                py: 2,
                bgcolor: (t) => (t.palette.mode === 'dark' ? alpha(t.palette.common.black, 0.12) : t.palette.grey[50]),
              }}
            >
              <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                alignItems={{ xs: 'flex-start', md: 'center' }}
                justifyContent="space-between"
              >
                <Box>
                  <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                    深度拆解（批量）
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    单次最多 {VIRAL_BATCH_MAX} 条 · 已选 {Math.min(videoRowSelection.length, VIRAL_BATCH_MAX)} 条
                  </Typography>
                </Box>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} width={{ xs: '100%', md: 'auto' }}>
                  <Button
                    fullWidth
                    size="small"
                    variant="contained"
                    color="secondary"
                    disableElevation
                    disabled={batchDeepMut.isPending || videoRowSelection.length === 0}
                    data-testid="sv-account-deep-analyze-selected-button"
                    data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.deepAnalyzeBatch}
                    data-no-local-deep-analyze="true"
                    onClick={() => {
                      const ids = videoRowSelection.map((x) => Number(x)).filter((n) => Number.isFinite(n))
                      const take = ids.slice(0, VIRAL_BATCH_MAX)
                      if (ids.length > VIRAL_BATCH_MAX) toast(`仅提交前 ${VIRAL_BATCH_MAX} 条`, 'success')
                      batchDeepMut.mutate(take)
                    }}
                  >
                    拆解已勾选
                  </Button>
                  <Button
                    fullWidth
                    size="small"
                    variant="outlined"
                    disabled={batchDeepMut.isPending}
                    data-testid="sv-account-deep-analyze-page-pending-button"
                    data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.deepAnalyzeBatch}
                    data-no-local-deep-analyze="true"
                    onClick={() => {
                      const rows = videosData?.list ?? []
                      const pendingIds = rows
                        .filter((r) => r.deepAnalyzeStatus === 'pending')
                        .map((r) => r.id)
                        .slice(0, VIRAL_BATCH_MAX)
                      if (pendingIds.length === 0) {
                        toast('本页无待分析记录', 'success')
                        return
                      }
                      if (rows.filter((r) => r.deepAnalyzeStatus === 'pending').length > VIRAL_BATCH_MAX) {
                        toast(`本页待分析超过 ${VIRAL_BATCH_MAX} 条，仅提交前 ${VIRAL_BATCH_MAX} 条`, 'success')
                      }
                      batchDeepMut.mutate(pendingIds)
                    }}
                  >
                    本页待分析排队
                  </Button>
                </Stack>
              </Stack>
            </Box>
          </Paper>

          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
            共 {Number(videosData?.total ?? 0)} 条 · 支持表头排序（服务端）
          </Typography>
          <Box sx={{ width: '100%', height: { xs: 440, sm: 520 }, minHeight: 360 }}>
            <Box data-testid="sv-account-videos-grid" data-source-endpoint={ACCOUNT_DETAIL_ENDPOINTS.videos} data-no-local-video-fallback="true" sx={{ height: '100%' }}>
              <StandardDataGrid
                rows={videosData?.list || []}
                columns={videoColumns}
                loading={videosFetching}
                rowCount={Number(videosData?.total ?? 0)}
                paginationMode="server"
                sortingMode="server"
                sortModel={videoSortModel}
                onSortModelChange={onVideoSortModelChange}
                paginationModel={{ page: videoQuery.page, pageSize: videoQuery.pageSize }}
                onPaginationModelChange={(model) =>
                  setVideoQuery((q) => ({ ...q, page: model.page, pageSize: model.pageSize }))
                }
                getRowId={(r) => r.id}
                checkboxSelection
                rowSelectionModel={videoRowSelection}
                onRowSelectionModelChange={(m) => setVideoRowSelection(m)}
                pageSizeOptions={[10, 20, 30, 50]}
                density="compact"
                sx={{
                  '& .MuiDataGrid-row': { minHeight: '52px !important' },
                }}
              />
            </Box>
          </Box>
        </TabPanel>

        {/* Tab 3: 基于该账号下全部已采集视频的聚合分析 */}
        <TabPanel value={tabValue} index={2}>
          {analyticsFetching && <LinearProgress sx={{ mb: 2 }} />}
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            以下统计基于该账号关联的全部已采集视频（爆款库）实时聚合，用于横向对比账号整体内容表现与深度分析进度。
          </Typography>
          {analyticsIsError && (
            <Alert
              data-testid="sv-account-analytics-error"
              data-no-static-analytics-fallback="true"
              severity="error"
              sx={{ mb: 2 }}
              action={<Button color="inherit" size="small" onClick={() => void refetchAnalytics()}>重试</Button>}
            >
              账号综合分析加载失败（POST {ACCOUNT_DETAIL_ENDPOINTS.analytics}）：{getErrorMessage(analyticsError)}。请检查爆款视频聚合查询；页面不会展示静态聚合结果。
            </Alert>
          )}
          <AccountAnalyticsPanel account={account} analytics={analyticsData} loading={analyticsFetching} />
        </TabPanel>
      </Card>
    </Box>
  )
}
