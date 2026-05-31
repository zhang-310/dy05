import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  TextField,
  Button,
  Chip,
  LinearProgress,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Alert,
  Tooltip,
  Stack,
  IconButton,
  ToggleButtonGroup,
  ToggleButton,
  Link,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Card,
  CardContent,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import InsightsIcon from '@mui/icons-material/Insights'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import CancelIcon from '@mui/icons-material/Cancel'
import RefreshIcon from '@mui/icons-material/Refresh'
import ReplayIcon from '@mui/icons-material/Replay'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader } from '@/components/base'
import { shortvideoApi, type AccountCollectTask } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { shortvideoRoutes, shortvideoAccountDetailPath } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'

type CollectViewMode = 'by_account' | 'flat'

const INPUT_TYPE_LABEL: Record<string, string> = {
  account_url: '主页链接',
  video_url: '视频链接',
  douyin_id: '抖音号',
  search_video: '关键词搜索',
}

const STATUS_VISUAL: Record<
  string,
  { label: string; color: 'default' | 'warning' | 'success' | 'error' | 'info' }
> = {
  pending: { label: '等待中', color: 'default' },
  collecting: { label: '采集中', color: 'warning' },
  collected: { label: '待选视频', color: 'info' },
  analyzing: { label: '分析中', color: 'warning' },
  indexing: { label: '入库中', color: 'warning' },
  completed: { label: '已完成', color: 'success' },
  failed: { label: '失败', color: 'error' },
}

const CANCELABLE_STATUSES = new Set(['pending', 'collecting', 'collected', 'analyzing', 'indexing'])

const ACTIVE_STATUSES = new Set(['pending', 'collecting', 'analyzing', 'indexing'])

const COLLECT_ENDPOINTS = {
  list: '/short-video/account-collect/list',
  start: '/short-video/account-collect/start',
  cancel: '/short-video/account-collect/cancel',
  retry: '/short-video/account-collect/retry',
} as const
const COLLECT_READY_ENDPOINTS = [
  COLLECT_ENDPOINTS.list,
  COLLECT_ENDPOINTS.start,
  COLLECT_ENDPOINTS.cancel,
  COLLECT_ENDPOINTS.retry,
] as const
const COLLECT_UNSUPPORTED_ENDPOINTS = [
  '/short-video/account-collect/mock',
  '/short-video/account-collect/local-list',
  '/short-video/account-collect/local-start',
  '/short-video/account-collect/local-cancel',
  '/short-video/account-collect/local-retry',
  '/short-video/account-collect/static-progress',
  '/short-video/account-collect/browser-direct-scrape',
] as const
const COLLECT_READY_ROUTES = [
  shortvideoRoutes.collect,
  shortvideoRoutes.accounts,
  `${shortvideoRoutes.accounts}/:id?tab=videos`,
  `${shortvideoRoutes.accounts}/:id?tab=analysis`,
].join('|')
const COLLECT_SUPPORTED_ACTIONS = [
  'refresh-collect-list',
  'create-collect-task',
  'cancel-collect-task',
  'retry-collect-task',
  'navigate-account-videos',
  'navigate-account-analysis',
  'toggle-account-group-view',
  'toggle-flat-task-view',
].join('|')

function truncateText(s: string | null | undefined, max: number): string {
  if (s == null || s.trim() === '') return '—'
  const t = s.trim()
  return t.length <= max ? t : `${t.slice(0, max)}…`
}

function displayAccountLabel(row: AccountCollectTask): string {
  const name = row.accountName?.trim()
  if (name) return name
  const input = row.originalInput?.trim()
  if (input) return truncateText(input, 28) === '—' ? '—' : truncateText(input, 28)
  const uid = row.secUid?.trim()
  if (uid) return truncateText(uid, 20)
  return '—'
}

function groupTasksBySvAccount(tasks: AccountCollectTask[]): Array<{
  key: string
  svAccountId: number | null
  tasks: AccountCollectTask[]
}> {
  const m = new Map<string, AccountCollectTask[]>()
  for (const t of tasks) {
    const k = t.svAccountId != null && t.svAccountId > 0 ? `acc-${t.svAccountId}` : 'no-account'
    if (!m.has(k)) m.set(k, [])
    m.get(k)!.push(t)
  }
  const order = (a: [string, AccountCollectTask[]], b: [string, AccountCollectTask[]]) => {
    if (a[0] === 'no-account') return 1
    if (b[0] === 'no-account') return -1
    return a[0].localeCompare(b[0], undefined, { numeric: true })
  }
  return [...m.entries()]
    .sort(order)
    .map(([key, ts]) => {
      const sorted = [...ts].sort((x, y) => {
        const ax = x.createTime ?? ''
        const ay = y.createTime ?? ''
        return ay.localeCompare(ax)
      })
      const rawId = key.startsWith('acc-') ? Number(key.slice(4)) : NaN
      const svAccountId = Number.isFinite(rawId) ? rawId : null
      return { key, svAccountId, tasks: sorted }
    })
}

function normalizeStatus(raw: string | number | null | undefined): string {
  if (raw == null) return 'pending'
  if (typeof raw === 'number') {
    const legacy = ['pending', 'collecting', 'completed', 'failed']
    return legacy[raw] ?? 'pending'
  }
  return String(raw)
}

type CollectUiMode = 'auto' | 'keyword_video'

export default function AccountCollectPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const [viewMode, setViewMode] = useState<CollectViewMode>('by_account')
  const [search, setSearch] = useState({ page: 0, rows: 20 })
  const [startDialogOpen, setStartDialogOpen] = useState(false)
  const [startForm, setStartForm] = useState<{ input: string; maxCount: string; collectMode: CollectUiMode }>({
    input: '',
    maxCount: '100',
    collectMode: 'auto',
  })
  const [cancelId, setCancelId] = useState<number | null>(null)
  const [cancelError, setCancelError] = useState('')
  const [operationError, setOperationError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sv-collect-tasks', search],
    queryFn: () => shortvideoApi.collectList(search),
    refetchInterval: (q) => {
      const rows = q.state.data?.list ?? []
      const active = rows.some((r) => ACTIVE_STATUSES.has(normalizeStatus(r.status)))
      return active ? 4000 : false
    },
    refetchOnWindowFocus: true,
  })

  const list = data?.list ?? []

  const accountGroups = useMemo(() => groupTasksBySvAccount(list), [list])
  const summary = useMemo(() => {
    return list.reduce(
      (acc, row) => {
        const status = normalizeStatus(row.status)
        if (ACTIVE_STATUSES.has(status)) acc.active += 1
        if (status === 'failed') acc.failed += 1
        if (status === 'completed') acc.completed += 1
        acc.collected += Number(row.collectedVideos ?? 0)
        acc.indexed += Number(row.indexedVideos ?? 0)
        return acc
      },
      { active: 0, failed: 0, completed: 0, collected: 0, indexed: 0 },
    )
  }, [list])

  const startMut = useMutation({
    mutationFn: () =>
      shortvideoApi.collectStart({
        input: startForm.input.trim(),
        maxCount: Number(startForm.maxCount),
        ...(startForm.collectMode === 'keyword_video' ? { collectMode: 'keyword_video' as const } : {}),
      }),
    onSuccess: () => {
      toast('采集任务已创建', 'success')
      setOperationError('')
      setStartForm((f) => ({ ...f, input: '', collectMode: f.collectMode }))
      setStartDialogOpen(false)
      void qc.invalidateQueries({ queryKey: ['sv-collect-tasks'] })
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setOperationError(`创建采集任务失败（POST ${COLLECT_ENDPOINTS.start}）：${message}`)
      toast(message, 'error')
    },
  })

  const cancelMut = useMutation({
    mutationFn: (taskId: number) => shortvideoApi.collectCancel(taskId),
    onSuccess: () => {
      toast('已取消', 'success')
      setOperationError('')
      setCancelError('')
      setCancelId(null)
      void qc.invalidateQueries({ queryKey: ['sv-collect-tasks'] })
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setOperationError(`取消采集任务失败（POST ${COLLECT_ENDPOINTS.cancel}）：${message}`)
      setCancelError(message)
      toast(message, 'error')
    },
  })

  const retryMut = useMutation({
    mutationFn: (taskId: number) => shortvideoApi.collectRetry(taskId),
    onSuccess: () => {
      toast('已重新排队采集', 'success')
      setOperationError('')
      void qc.invalidateQueries({ queryKey: ['sv-collect-tasks'] })
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setOperationError(`重试采集任务失败（POST ${COLLECT_ENDPOINTS.retry}）：${message}`)
      toast(message, 'error')
    },
  })

  const columns: GridColDef<AccountCollectTask>[] = [
    { field: 'id', headerName: 'ID', width: 72 },
    {
      field: 'accountLabel',
      headerName: '账号 / 输入',
      flex: 1,
      minWidth: 200,
      sortable: false,
      valueGetter: (_v, row) => displayAccountLabel(row),
      renderCell: ({ row }) => {
        const full = row.originalInput?.trim() || row.accountUrl || row.secUid || ''
        const label = displayAccountLabel(row)
        const hasSvAccountId = row.svAccountId != null && row.svAccountId > 0

        const cell = (
          <Box sx={{ py: 0.5, overflow: 'hidden' }}>
            {hasSvAccountId ? (
              <Link
                component="button"
                variant="body2"
                onClick={() => navigate(shortvideoAccountDetailPath(row.svAccountId!, 'videos'))}
                sx={{
                  textAlign: 'left',
                  display: 'block',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  cursor: 'pointer',
                }}
                title={`${label} (点击查看账号详情)`}
              >
                {label}
              </Link>
            ) : (
              <Typography variant="body2" noWrap title={label}>
                {label}
              </Typography>
            )}
            {row.accountName?.trim() && row.originalInput?.trim() && (
              <Typography variant="caption" color="text.secondary" noWrap display="block">
                {truncateText(row.originalInput, 36)}
              </Typography>
            )}
          </Box>
        )
        return full.length > 32 ? (
          <Tooltip title={full} placement="top-start">
            <Box sx={{ width: '100%' }}>{cell}</Box>
          </Tooltip>
        ) : (
          cell
        )
      },
    },
    {
      field: 'inputType',
      headerName: '识别类型',
      width: 100,
      sortable: false,
      valueGetter: (_v, row) => row.inputType ?? '',
      renderCell: ({ value }) => (
        <Typography variant="body2">
          {typeof value === 'string' && value ? INPUT_TYPE_LABEL[value] ?? value : '—'}
        </Typography>
      ),
    },
    {
      field: 'status',
      headerName: '状态',
      width: 108,
      renderCell: ({ row }) => {
        const key = normalizeStatus(row.status)
        const s = STATUS_VISUAL[key] ?? { label: key || '未知', color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    {
      field: 'progress',
      headerName: '列表采集进度',
      minWidth: 180,
      flex: 0.8,
      sortable: false,
      renderCell: ({ row }) => {
        const st = normalizeStatus(row.status)
        const total = row.totalVideos ?? 0
        const done = row.collectedVideos ?? 0
        const pct = total > 0 ? Math.round((done / total) * 100) : 0
        const indeterminate = total === 0 && (st === 'pending' || st === 'collecting')
        return (
          <Box sx={{ width: '100%', pr: 1 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.3 }}>
              <Typography variant="caption" color="text.secondary">
                {indeterminate ? '排队或拉取中…' : `${done} / ${total}`}
              </Typography>
              {!indeterminate && (
                <Typography variant="caption" color="text.secondary">
                  {pct}%
                </Typography>
              )}
            </Box>
            {indeterminate ? <LinearProgress /> : <LinearProgress variant="determinate" value={pct} />}
            {row.analyzedVideos != null && row.analyzedVideos > 0 && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                已分析 {row.analyzedVideos}
                {row.indexedVideos != null ? ` · 已入库 ${row.indexedVideos}` : ''}
              </Typography>
            )}
          </Box>
        )
      },
    },
    {
      field: 'errorMessage',
      headerName: '备注',
      width: 200,
      sortable: false,
      renderCell: ({ row }) => {
        const st = normalizeStatus(row.status)
        if (st === 'failed' && row.errorMessage) {
          return (
            <Tooltip title={row.errorMessage}>
              <Typography variant="caption" color="error" sx={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                {row.errorMessage}
              </Typography>
            </Tooltip>
          )
        }
        if (st === 'collected') {
          return (
            <Typography variant="caption" color="text.secondary">
              请在关联流程中选择视频并分析
            </Typography>
          )
        }
        return <Typography variant="caption" color="text.secondary">—</Typography>
      },
    },
    {
      field: 'createTime',
      headerName: '创建时间',
      width: 168,
      renderCell: ({ row }) => (
        <Typography variant="body2">{formatDate(row.createTime != null ? String(row.createTime) : '')}</Typography>
      ),
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 200,
      sortable: false,
      renderCell: ({ row }) => {
        const st = normalizeStatus(row.status)
        const hasSvAccountId = row.svAccountId != null && row.svAccountId > 0
        return (
          <Stack direction="row" spacing={0.5} alignItems="center">
            {hasSvAccountId && (
              <Tooltip title="查看账号详情和视频列表">
                <IconButton
                  size="small"
                  color="primary"
                  onClick={() => navigate(shortvideoAccountDetailPath(row.svAccountId!, 'videos'))}
                >
                  <AccountCircleIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            {CANCELABLE_STATUSES.has(st) && (
              <Button
                size="small"
                color="error"
                startIcon={<CancelIcon />}
                onClick={() => {
                  setCancelError('')
                  setCancelId(row.id)
                }}
                disabled={cancelMut.isPending}
              >
                取消
              </Button>
            )}
            {st === 'failed' && (
              <Button
                size="small"
                variant="outlined"
                startIcon={<ReplayIcon />}
                onClick={() => retryMut.mutate(row.id)}
                disabled={retryMut.isPending}
              >
                重试
              </Button>
            )}
          </Stack>
        )
      },
    },
  ]

  const collectToolbar = (
    <Stack
      direction="row"
      spacing={1}
      alignItems="center"
      flexWrap="wrap"
      useFlexGap
      data-testid="account-collect-toolbar"
      data-source-endpoints={COLLECT_READY_ENDPOINTS.join('|')}
      data-server-pagination="true"
      data-supported-actions={COLLECT_SUPPORTED_ACTIONS}
    >
      <ToggleButtonGroup
        exclusive
        size="small"
        value={viewMode}
        onChange={(_, v: CollectViewMode | null) => v != null && setViewMode(v)}
      >
        <ToggleButton value="by_account">按账号</ToggleButton>
        <ToggleButton value="flat">全部任务</ToggleButton>
      </ToggleButtonGroup>
      <Tooltip title="立即刷新列表">
        <span>
          <IconButton
            onClick={() => void refetch()}
            disabled={isFetching}
            size="small"
            color="primary"
            data-testid="account-collect-refresh-button"
            data-source-endpoint={COLLECT_ENDPOINTS.list}
          >
            <RefreshIcon />
          </IconButton>
        </span>
      </Tooltip>
      <Button
        variant="contained"
        startIcon={<PlayArrowIcon />}
        onClick={() => setStartDialogOpen(true)}
        data-testid="account-collect-open-start-button"
        data-source-endpoint={COLLECT_ENDPOINTS.start}
      >
        新建采集
      </Button>
    </Stack>
  )

  const totalCount = data?.total ?? 0
  const hasMorePages = totalCount > 0 ? (search.page + 1) * search.rows < totalCount : list.length >= search.rows

  return (
    <Box
      data-testid="shortvideo-account-collect-page"
      data-contract-scope="shortvideo-account-collect"
      data-ready-endpoints={COLLECT_READY_ENDPOINTS.join('|')}
      data-ready-routes={COLLECT_READY_ROUTES}
      data-supported-actions={COLLECT_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={COLLECT_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-collect-fallback="true"
      data-server-pagination="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', px: 2, pt: 2 }}
    >
      <PageHeader
        title="账号视频采集"
        subtitle="按抖音账号聚合采集任务；进入「抖音账号库」可查看该账号下已采集视频、分析与标签。支持智能识别链接/抖音号与关键词视频搜索。"
        actions={
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
            <Button
              variant="contained"
              color="secondary"
              size="small"
              startIcon={<VideoLibraryIcon />}
              onClick={() => navigate(shortvideoRoutes.accounts)}
            >
              抖音账号库
            </Button>
            <Button variant="outlined" size="small" onClick={() => navigate(shortvideoRoutes.douyinCookies)}>
              抖音 Cookie 管理
            </Button>
          </Stack>
        }
      />
      <Alert
        severity="info"
        sx={{ mb: 2 }}
        variant="outlined"
        data-testid="account-collect-boundary-contract"
        data-source-endpoints={COLLECT_READY_ENDPOINTS.join('|')}
        data-service-side-browser-collect="true"
        data-no-frontend-browser-scrape="true"
        data-no-local-collect-fallback="true"
        data-supported-actions={COLLECT_SUPPORTED_ACTIONS}
      >
        「关键词」模式将打开抖音站内视频搜索结果并滚动抓取卡片（与网页版展示一致，可能受登录/验证码影响）。
        服务端 Playwright 可注入 Netscape cookies 文件（配置项 <code>app.video-analysis.yt-dlp-cookies-file</code>）；接口维度的 Cookie 请在{' '}
        <Link component="button" type="button" onClick={() => navigate(shortvideoRoutes.douyinCookies)} sx={{ cursor: 'pointer', verticalAlign: 'baseline' }}>
          抖音 Cookie 管理
        </Link>{' '}
        中维护。
        <strong> 按账号</strong>视图为当前页任务分组；管理某账号下全部视频请打开<strong>抖音账号库</strong>并进入详情「视频」页签。
        进行中任务约每 4 秒自动刷新；若长时间停在「等待中」，请查后端日志与 accountCollectExecutor。
      </Alert>

      <Grid
        container
        spacing={2}
        sx={{ mb: 2 }}
        data-testid="account-collect-summary"
        data-source-endpoint={COLLECT_ENDPOINTS.list}
        data-no-static-progress="true"
      >
        {[
          ['当前页任务', list.length],
          ['运行中', summary.active],
          ['失败任务', summary.failed],
          ['已入库视频', summary.indexed],
        ].map(([label, value]) => (
          <Grid item xs={6} md={3} key={String(label)}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight={700}>{value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
          data-testid="account-collect-list-error"
          data-source-endpoint={COLLECT_ENDPOINTS.list}
          data-no-local-collect-fallback="true"
          data-input-retained="true"
        >
          采集任务加载失败（POST {COLLECT_ENDPOINTS.list}）：{getErrorMessage(error)}。请检查登录态、采集任务表和后端 Playwright 运行环境；页面不会补本地任务。
        </Alert>
      )}
      {operationError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setOperationError('')}
          data-testid="account-collect-operation-error"
          data-source-endpoints={`${COLLECT_ENDPOINTS.start}|${COLLECT_ENDPOINTS.cancel}|${COLLECT_ENDPOINTS.retry}`}
          data-no-local-collect-mutation="true"
          data-input-retained="true"
        >
          {operationError}
        </Alert>
      )}

      {viewMode === 'by_account' && (
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1, flexWrap: 'wrap', gap: 1 }}>
          {collectToolbar}
        </Stack>
      )}

      {viewMode === 'by_account' ? (
        <Box
          sx={{ flex: 1, minHeight: 360, overflow: 'auto', pr: 0.5 }}
          data-testid="account-collect-account-groups"
          data-source-endpoint={COLLECT_ENDPOINTS.list}
          data-no-local-collect-fallback="true"
        >
          {accountGroups.length === 0 ? (
            <Typography
              color="text.secondary"
              sx={{ py: 4, textAlign: 'center' }}
              data-testid="account-collect-empty"
              data-source-endpoint={COLLECT_ENDPOINTS.list}
              data-no-local-collect-fallback="true"
            >
              当前页暂无采集任务
            </Typography>
          ) : (
            accountGroups.map((g) => {
              const headTask = g.tasks[0]
              const label =
                g.svAccountId != null
                  ? displayAccountLabel(headTask)
                  : '未关联抖音账号（关键词搜索等）'
              return (
                <Accordion key={g.key} defaultExpanded disableGutters sx={{ mb: 1, '&:before': { display: 'none' } }}>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Stack
                      direction={{ xs: 'column', sm: 'row' }}
                      spacing={1}
                      alignItems={{ xs: 'flex-start', sm: 'center' }}
                      justifyContent="space-between"
                      sx={{ width: '100%', pr: 1 }}
                    >
                      <Box sx={{ minWidth: 0 }}>
                        <Typography fontWeight={600} noWrap title={label}>
                          {g.svAccountId != null ? `账号 · ${label}` : label}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {g.tasks.length} 个采集任务
                          {g.svAccountId != null && (
                            <>
                              {' · '}
                              svAccountId {g.svAccountId}
                            </>
                          )}
                        </Typography>
                      </Box>
                      <Stack direction="row" spacing={1} alignItems="center" onClick={(e) => e.stopPropagation()}>
                        {g.svAccountId != null && (
                          <>
                            <Button
                              size="small"
                              variant="contained"
                              startIcon={<VideoLibraryIcon />}
                              onClick={() => navigate(shortvideoAccountDetailPath(g.svAccountId!, 'videos'))}
                              data-testid="account-collect-open-account-videos-button"
                              data-target-route={shortvideoAccountDetailPath(g.svAccountId!, 'videos')}
                            >
                              全部采集视频
                            </Button>
                            <Button
                              size="small"
                              variant="outlined"
                              startIcon={<InsightsIcon />}
                              onClick={() => navigate(shortvideoAccountDetailPath(g.svAccountId!, 'analysis'))}
                              data-testid="account-collect-open-account-analysis-button"
                              data-target-route={shortvideoAccountDetailPath(g.svAccountId!, 'analysis')}
                            >
                              综合分析
                            </Button>
                          </>
                        )}
                      </Stack>
                    </Stack>
                  </AccordionSummary>
                  <AccordionDetails sx={{ pt: 0, px: 1, pb: 2 }}>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell width={72}>ID</TableCell>
                          <TableCell>输入 / 类型</TableCell>
                          <TableCell width={100}>状态</TableCell>
                          <TableCell>列表进度</TableCell>
                          <TableCell width={220}>操作</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {g.tasks.map((row) => {
                          const st = normalizeStatus(row.status)
                          const key = st
                          const s = STATUS_VISUAL[key] ?? { label: key || '未知', color: 'default' as const }
                          const total = row.totalVideos ?? 0
                          const done = row.collectedVideos ?? 0
                          const pct = total > 0 ? Math.round((done / total) * 100) : 0
                          const indeterminate = total === 0 && (st === 'pending' || st === 'collecting')
                          const hasSv = row.svAccountId != null && row.svAccountId > 0
                          return (
                            <TableRow key={row.id} hover>
                              <TableCell>{row.id}</TableCell>
                              <TableCell>
                                <Typography variant="body2" noWrap title={row.originalInput ?? ''}>
                                  {truncateText(row.originalInput, 40)}
                                </Typography>
                                <Typography variant="caption" color="text.secondary" display="block">
                                  {row.inputType && INPUT_TYPE_LABEL[row.inputType] ? INPUT_TYPE_LABEL[row.inputType] : row.inputType ?? '—'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Chip label={s.label} color={s.color} size="small" />
                              </TableCell>
                              <TableCell sx={{ minWidth: 160 }}>
                                <Typography variant="caption" color="text.secondary">
                                  {indeterminate ? '排队或拉取中…' : `${done} / ${total}`}
                                  {!indeterminate && ` · ${pct}%`}
                                </Typography>
                                {indeterminate ? <LinearProgress sx={{ mt: 0.5 }} /> : <LinearProgress variant="determinate" value={pct} sx={{ mt: 0.5 }} />}
                                {st === 'failed' && row.errorMessage && (
                                  <Tooltip title={row.errorMessage}>
                                    <Typography
                                      variant="caption"
                                      color="error"
                                      display="block"
                                      sx={{
                                        mt: 0.5,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                        maxWidth: 360,
                                      }}
                                    >
                                      {row.errorMessage}
                                    </Typography>
                                  </Tooltip>
                                )}
                              </TableCell>
                              <TableCell>
                                <Stack direction="row" spacing={0.5} alignItems="center" flexWrap="wrap" useFlexGap>
                                  {hasSv && (
                                    <Tooltip title="查看账号与全部采集视频">
                                      <IconButton size="small" color="primary" onClick={() => navigate(shortvideoAccountDetailPath(row.svAccountId!, 'videos'))}>
                                        <AccountCircleIcon fontSize="small" />
                                      </IconButton>
                                    </Tooltip>
                                  )}
                                  {CANCELABLE_STATUSES.has(st) && (
                                    <Button
                                      size="small"
                                      color="error"
                                      startIcon={<CancelIcon />}
                                      onClick={() => {
                                        setCancelError('')
                                        setCancelId(row.id)
                                      }}
                                      disabled={cancelMut.isPending}
                                      data-testid="account-collect-cancel-button"
                                      data-source-endpoint={COLLECT_ENDPOINTS.cancel}
                                    >
                                      取消
                                    </Button>
                                  )}
                                  {st === 'failed' && (
                                    <Button
                                      size="small"
                                      variant="outlined"
                                      startIcon={<ReplayIcon />}
                                      onClick={() => retryMut.mutate(row.id)}
                                      disabled={retryMut.isPending}
                                      data-testid="account-collect-retry-button"
                                      data-source-endpoint={COLLECT_ENDPOINTS.retry}
                                    >
                                      重试
                                    </Button>
                                  )}
                                </Stack>
                              </TableCell>
                            </TableRow>
                          )
                        })}
                      </TableBody>
                    </Table>
                  </AccordionDetails>
                </Accordion>
              )
            })
          )}
        </Box>
      ) : null}

      {viewMode === 'flat' ? (
        <Box
          data-testid="account-collect-flat-grid"
          data-source-endpoint={COLLECT_ENDPOINTS.list}
          data-server-pagination="true"
          data-no-local-collect-fallback="true"
          sx={{ flex: 1, minHeight: 360 }}
        >
          <StandardDataGrid
            rows={list}
            columns={columns}
            rowCount={totalCount}
            loading={isFetching}
            paginationMode="server"
            paginationModel={{ page: search.page, pageSize: search.rows }}
            onPaginationModelChange={(m) => setSearch((s) => ({ ...s, page: m.page, rows: m.pageSize }))}
            actionSlot={collectToolbar}
            sx={{ flex: 1, minHeight: 360 }}
          />
        </Box>
      ) : (
        <Stack direction="row" justifyContent="flex-end" alignItems="center" sx={{ mt: 1, flexWrap: 'wrap', gap: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mr: 'auto' }}>
            共 {totalCount} 条 · 每页 {search.rows} 条
          </Typography>
          <Button size="small" disabled={search.page <= 0} onClick={() => setSearch((s) => ({ ...s, page: Math.max(0, s.page - 1) }))}>
            上一页
          </Button>
          <Typography variant="body2" color="text.secondary">
            第 {search.page + 1} 页
          </Typography>
          <Button size="small" disabled={!hasMorePages} onClick={() => setSearch((s) => ({ ...s, page: s.page + 1 }))}>
            下一页
          </Button>
        </Stack>
      )}

      <Dialog open={startDialogOpen} onClose={() => setStartDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>新建账号采集任务</DialogTitle>
        <DialogContent
          data-testid="account-collect-start-form"
          data-source-endpoint={COLLECT_ENDPOINTS.start}
          data-input-retained={startMut.isError ? 'true' : 'false'}
        >
          {startMut.isError && (
            <Alert
              severity="error"
              sx={{ mt: 1, mb: 1 }}
              data-testid="account-collect-start-error"
              data-source-endpoint={COLLECT_ENDPOINTS.start}
              data-no-local-collect-mutation="true"
              data-input-retained="true"
            >
              创建采集任务失败（POST {COLLECT_ENDPOINTS.start}）：{getErrorMessage(startMut.error)}
            </Alert>
          )}
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                采集方式
              </Typography>
              <ToggleButtonGroup
                exclusive
                fullWidth
                size="small"
                value={startForm.collectMode}
                onChange={(_, v: CollectUiMode | null) => v != null && setStartForm((f) => ({ ...f, collectMode: v }))}
              >
                <ToggleButton value="auto">智能识别（链接 / 抖音号）</ToggleButton>
                <ToggleButton value="keyword_video">关键词（视频搜索）</ToggleButton>
              </ToggleButtonGroup>
            </Grid>
            <Grid item xs={12}>
              <TextField
                label={startForm.collectMode === 'keyword_video' ? '搜索关键词' : '链接或抖音号'}
                fullWidth
                multiline
                minRows={2}
                placeholder={
                  startForm.collectMode === 'keyword_video'
                    ? '例如：护肤品测评、口红试色（将用于抖音站内视频搜索）'
                    : '示例：https://www.douyin.com/user/… 或 纯数字抖音号'
                }
                helperText={
                  startForm.collectMode === 'keyword_video'
                    ? '后端将访问抖音视频搜索结果页并滚动采集；建议配置 Cookie 文件或 Cookie 管理中的有效登录态。'
                    : '对应后端智能识别（input）；不是本系统「抖音账号管理」里的数据库主键。'
                }
                value={startForm.input}
                onChange={(e) => setStartForm((f) => ({ ...f, input: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="期望最大条数（可选）"
                fullWidth
                type="number"
                helperText="请求体携带给后端；若接口暂未消费该字段，以实际采集逻辑为准。"
                value={startForm.maxCount}
                onChange={(e) => setStartForm((f) => ({ ...f, maxCount: e.target.value }))}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStartDialogOpen(false)}>关闭</Button>
          <Button
            variant="contained"
            disabled={!startForm.input.trim() || startMut.isPending}
            onClick={() => startMut.mutate()}
            data-testid="account-collect-start-button"
            data-source-endpoint={COLLECT_ENDPOINTS.start}
          >
            开始采集
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={cancelId !== null}
        content={
          cancelError
            ? `确定取消该采集任务吗？进行中的任务将标记为失败。上次取消失败（POST ${COLLECT_ENDPOINTS.cancel}）：${cancelError}`
            : '确定取消该采集任务吗？进行中的任务将标记为失败。'
        }
        onClose={() => {
          setCancelId(null)
          setCancelError('')
        }}
        onConfirm={() => cancelId !== null && cancelMut.mutate(cancelId)}
        loading={cancelMut.isPending}
      />
    </Box>
  )
}
