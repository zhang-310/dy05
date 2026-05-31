import { useParams, Navigate, useSearchParams, useLocation } from 'react-router-dom'
import {
  Box, Chip, IconButton, Typography, Tooltip,
  Skeleton, Breadcrumbs, Link, Button, LinearProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Stack, FormControlLabel, Checkbox, Select, MenuItem, FormControl, InputLabel, Alert,
} from '@mui/material'
import { alpha, type Theme } from '@mui/material/styles'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import VideocamIcon from '@mui/icons-material/Videocam'
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord'
import MovieIcon from '@mui/icons-material/Movie'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { SessionWorkspacePage } from './SessionWorkspacePage'
import {
  STEP_TO_TAB, STEP_LABELS,
  STEP_COLOR_TONES,
  getStepThemeColors,
  parseStepFromSearch, buildStepParams,
  type WorkspaceTab,
} from './sessionWorkbenchNav'
import { liveApi } from '@/api/live'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { useState } from 'react'
import { getErrorMessage } from '@/utils/errorHandler'
import {
  inferLiveRouteScope,
  liveRealtimePath,
  liveScopeLabel,
  liveSessionListPath,
  liveSessionPath,
  shortVideoProjectPath,
} from './liveRouteScope'

const LIVE_WORKBENCH_READY_ENDPOINTS = {
  sessionGet: '/live/session/get',
  readiness: '/live/session/readiness',
  overview: '/live/session/overview',
  clone: '/live/session/clone',
  exportShortVideo: '/live/session/export-to-short-video',
} as const

const LIVE_WORKBENCH_CONTEXT_ENDPOINTS = [
  LIVE_WORKBENCH_READY_ENDPOINTS.sessionGet,
  LIVE_WORKBENCH_READY_ENDPOINTS.readiness,
  LIVE_WORKBENCH_READY_ENDPOINTS.overview,
  LIVE_WORKBENCH_READY_ENDPOINTS.clone,
  LIVE_WORKBENCH_READY_ENDPOINTS.exportShortVideo,
  '/live/product/by-session',
  '/live/script/by-session',
]

const LIVE_WORKBENCH_UNSUPPORTED_ACTIONS = [
  'org-realtime-panel',
  'org-shortvideo-workbench-route',
  'talent-admin-shortvideo-route',
  'local-session-header-fallback',
  'local-readiness-fallback',
  'local-overview-fallback',
  'direct-product-mutation',
  'direct-script-mutation',
]

interface ReadinessData {
  score?: number
  [key: string]: unknown
}

type HeaderTone = 'error' | 'text' | 'warning' | 'success'

function getStatusTone(status?: number): HeaderTone {
  if (status === 1) return 'error'
  if (status === 2) return 'text'
  return 'warning'
}

function getReadinessTone(score: number): Exclude<HeaderTone, 'text'> {
  if (score >= 85) return 'success'
  if (score >= 60) return 'warning'
  return 'error'
}

function getHeaderToneColor(theme: Theme, tone: HeaderTone) {
  if (tone === 'text') {
    return theme.palette.text.secondary
  }
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
}

function getOperationErrorSource(error: string) {
  if (error.includes(LIVE_WORKBENCH_READY_ENDPOINTS.clone)) return LIVE_WORKBENCH_READY_ENDPOINTS.clone
  if (error.includes(LIVE_WORKBENCH_READY_ENDPOINTS.exportShortVideo)) return LIVE_WORKBENCH_READY_ENDPOINTS.exportShortVideo
  return ''
}

export default function LiveWorkbenchPage() {
  const { sessionId: rawId } = useParams<{ sessionId: string }>()
  const sessionId = Number(rawId)
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const routeScope = inferLiveRouteScope(useLocation().pathname)
  const listPath = liveSessionListPath(routeScope)
  const realtimePath = liveRealtimePath(routeScope, sessionId)
  const toast = useToast()
  const step = parseStepFromSearch(searchParams)
  const [cloneOpen, setCloneOpen] = useState(false)
  const [cloneName, setCloneName] = useState('')
  const [exportOpen, setExportOpen] = useState(false)
  const [exportStyle, setExportStyle] = useState('professional')
  const [operationError, setOperationError] = useState('')
  const [cloneError, setCloneError] = useState('')
  const [exportError, setExportError] = useState('')

  const { data: session, isLoading, isError: sessionIsError, error: sessionError, refetch: refetchSession } = useQuery({
    queryKey: ['wb-session-header', sessionId],
    queryFn: () => liveApi.sessionGet(sessionId),
    enabled: !isNaN(sessionId),
  })

  const { data: readiness, isError: readinessIsError, error: readinessError, refetch: refetchReadiness } = useQuery({
    queryKey: ['wb-readiness', sessionId],
    queryFn: () => liveApi.sessionReadiness(sessionId),
    enabled: !isNaN(sessionId),
    refetchInterval: 60000,
  })

  const cloneMut = useMutation({
    mutationFn: () => liveApi.sessionClone({ id: sessionId, newTitle: cloneName.trim() || undefined }),
    onMutate: () => { setOperationError(''); setCloneError('') },
    onSuccess: (newId) => {
      toast('场次克隆成功', 'success')
      setCloneOpen(false)
      setCloneError('')
      navigate(liveSessionPath(routeScope, newId))
    },
    onError: (error) => {
      const message = getErrorMessage(error)
      const text = `/live/session/clone 克隆场次失败：${message}`
      setOperationError(text)
      setCloneError(text)
      toast(`克隆失败：${message}`, 'error')
    },
  })

  const exportMut = useMutation({
    mutationFn: () => liveApi.sessionExportToShortVideo(sessionId, exportStyle),
    onMutate: () => { setOperationError(''); setExportError('') },
    onSuccess: (result) => {
      toast('已导出至短视频项目', 'success')
      setExportOpen(false)
      setExportError('')
      const targetPath = shortVideoProjectPath(routeScope, result?.projectId)
      if (targetPath) {
        navigate(targetPath)
      } else {
        setOperationError('/live/session/export-to-short-video 导出成功，但组织端暂未开放短视频项目工作台，请在管理员端查看导出的短视频项目。')
      }
    },
    onError: (error) => {
      const message = getErrorMessage(error)
      const text = `/live/session/export-to-short-video 导出短视频项目失败：${message}`
      setOperationError(text)
      setExportError(text)
      toast(`导出失败：${message}`, 'error')
    },
  })

  const { data: overviewRaw, isError: overviewIsError, error: overviewError, refetch: refetchOverview } = useQuery({
    queryKey: ['wb-session-overview', sessionId],
    queryFn: () => liveApi.sessionOverview(sessionId),
    enabled: !isNaN(sessionId),
    staleTime: 60000,
  })
  const overview = overviewRaw as { gmv?: number; orderCount?: number; scriptCount?: number; productCount?: number; readiness?: number } | undefined

  if (isNaN(sessionId)) return <Navigate to={listPath} replace />

  const readinessPct = Number((readiness as ReadinessData)?.score ?? 0)
  const statusLabel = session?.status === 1 ? '直播中' : session?.status === 2 ? '已结束' : '草稿'
  const statusTone = getStatusTone(session?.status)
  const readinessTone = getReadinessTone(readinessPct)
  const currentTab = STEP_TO_TAB[step] ?? 'products'
  const operationErrorSource = getOperationErrorSource(operationError)
  const errorSources = [
    sessionIsError ? LIVE_WORKBENCH_READY_ENDPOINTS.sessionGet : '',
    readinessIsError ? LIVE_WORKBENCH_READY_ENDPOINTS.readiness : '',
    overviewIsError ? LIVE_WORKBENCH_READY_ENDPOINTS.overview : '',
    operationErrorSource,
  ].filter(Boolean)
  const exportTargetTemplate = routeScope === 'org'
    ? 'unsupported'
    : routeScope === 'talent'
      ? '/talent/shortvideo?projectId=:id'
      : '/admin/shortvideo/workbench?projectId=:id'

  const handleStepChange = (newStep: number) => {
    setSearchParams(buildStepParams(newStep, searchParams), { replace: true })
  }

  return (
    <Box
      sx={{ height: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
      data-testid="live-workbench-shell"
      data-contract-scope="live-workbench-entry-shell"
      data-ready-endpoints={Object.values(LIVE_WORKBENCH_READY_ENDPOINTS).join('|')}
      data-context-endpoints={LIVE_WORKBENCH_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={LIVE_WORKBENCH_UNSUPPORTED_ACTIONS.join('|')}
      data-route-scope={routeScope}
      data-session-id={Number.isFinite(sessionId) ? sessionId : 'invalid'}
      data-step={step}
      data-tab={currentTab}
      data-list-path={listPath}
      data-realtime-path={realtimePath || 'unsupported'}
      data-shortvideo-target-template={exportTargetTemplate}
      data-has-session={session ? 'true' : 'false'}
      data-readiness-status={readinessIsError ? 'error' : readinessPct > 0 ? 'ready' : 'empty'}
      data-overview-status={overviewIsError ? 'error' : overview ? 'ready' : 'empty'}
      data-no-local-session-header-fallback="true"
      data-no-local-readiness-fallback="true"
      data-no-local-overview-fallback="true"
    >
      {/* 顶部导航栏 */}
      <Box
        data-testid="live-workbench-header-surface"
        data-contract-source={`${LIVE_WORKBENCH_READY_ENDPOINTS.sessionGet}|${LIVE_WORKBENCH_READY_ENDPOINTS.readiness}|${LIVE_WORKBENCH_READY_ENDPOINTS.overview}`}
        data-route-scope={routeScope}
        data-status-tone={statusTone}
        data-readiness-tone={readinessTone}
        sx={{
        px: 1.5, py: 0.75,
        borderBottom: '1px solid',
        borderColor: 'divider',
        bgcolor: 'background.paper',
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        flexShrink: 0,
        flexWrap: 'wrap',
      }}>
        {/* 返回按钮 */}
        <Tooltip title="返回场次列表">
          <IconButton
            size="small"
            aria-label="返回场次列表"
            onClick={() => navigate(listPath)}
            data-testid="live-workbench-back-button"
            data-list-path={listPath}
          >
            <ArrowBackIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        {/* 面包屑 */}
        <Breadcrumbs separator={<NavigateNextIcon sx={{ fontSize: 14 }} />} sx={{ flex: 1, minWidth: 0 }}>
          <Link component={RouterLink} to={listPath} underline="hover" color="text.secondary" sx={{ fontSize: 13 }}>
            {liveScopeLabel(routeScope)}直播场次
          </Link>
          <Tooltip
            title={overview ? (
              <Box sx={{ fontSize: 12 }}>
                <div>GMV: ¥{(overview.gmv ?? 0).toLocaleString()}</div>
                <div>订单: {overview.orderCount ?? 0}</div>
                <div>话术: {overview.scriptCount ?? 0} 条</div>
                <div>商品: {overview.productCount ?? 0} 件</div>
                <div>就绪度: {overview.readiness ?? 0}%</div>
              </Box>
            ) : ''}
            arrow
          >
            <Typography sx={{ fontSize: 13, fontWeight: 600, color: 'text.primary', cursor: overview ? 'help' : 'default' }} noWrap>
              {isLoading ? <Skeleton width={120} /> : (session?.liveTitle || `场次 #${sessionId}`)}
            </Typography>
          </Tooltip>
        </Breadcrumbs>

        {/* 状态 Chip */}
        {session && (
          <Chip
            icon={<FiberManualRecordIcon sx={(theme) => {
              const color = getHeaderToneColor(theme, statusTone)
              return { fontSize: '10px !important', color: `${color} !important` }
            }} />}
            label={statusLabel}
            size="small"
            variant="outlined"
            data-testid="live-workbench-status-chip-surface"
            data-status-tone={statusTone}
            sx={(theme) => {
              const color = getHeaderToneColor(theme, statusTone)
              return {
                borderColor: alpha(color, theme.palette.mode === 'dark' ? 0.58 : 0.44),
                bgcolor: alpha(color, theme.palette.mode === 'dark' ? 0.14 : 0.08),
                color,
                fontWeight: 600,
              }
            }}
          />
        )}

        {/* 就绪度进度条 */}
        {readinessPct > 0 && (
          <Tooltip title={`就绪度 ${readinessPct}%`}>
            <Stack direction="row" alignItems="center" spacing={0.5} sx={{ minWidth: 100 }}>
              <LinearProgress
                variant="determinate"
                value={readinessPct}
                data-testid="live-workbench-readiness-progress-surface"
                data-readiness-tone={readinessTone}
                sx={(theme) => {
                  const color = getHeaderToneColor(theme, readinessTone)
                  return {
                    flex: 1,
                    height: 6,
                    borderRadius: 3,
                    bgcolor: alpha(color, theme.palette.mode === 'dark' ? 0.2 : 0.14),
                    '& .MuiLinearProgress-bar': { bgcolor: color },
                  }
                }}
              />
              <Typography
                variant="caption"
                data-testid="live-workbench-readiness-label"
                data-readiness-tone={readinessTone}
                sx={(theme) => ({
                  fontSize: 11,
                  fontWeight: 700,
                  color: getHeaderToneColor(theme, readinessTone),
                })}
              >
                {readinessPct}%
              </Typography>
            </Stack>
          </Tooltip>
        )}

        {/* 操作按钮 */}
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="克隆场次">
            <IconButton
              size="small"
              aria-label="克隆场次"
              onClick={() => { setCloneName(`${session?.liveTitle ?? ''}（副本）`); setCloneError(''); setOperationError(''); setCloneOpen(true) }}
              data-testid="live-workbench-clone-button"
              data-contract-source={LIVE_WORKBENCH_READY_ENDPOINTS.clone}
              data-route-scope={routeScope}
            >
              <ContentCopyIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="导出为短视频项目">
            <IconButton
              size="small"
              aria-label="导出为短视频项目"
              onClick={() => { setExportError(''); setOperationError(''); setExportOpen(true) }}
              disabled={exportMut.isPending}
              data-testid="live-workbench-export-button"
              data-contract-source={LIVE_WORKBENCH_READY_ENDPOINTS.exportShortVideo}
              data-route-scope={routeScope}
              data-shortvideo-target-template={exportTargetTemplate}
            >
              <MovieIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {realtimePath ? (
            <Button size="small" variant="outlined" startIcon={<VideocamIcon />}
              onClick={() => navigate(realtimePath)}
              data-testid="live-workbench-realtime-button"
              data-route-scope={routeScope}
              data-target-path={realtimePath}
              data-contract-status="enabled"
              sx={{ fontSize: 12, py: 0.3 }}>
              实时面板
            </Button>
          ) : (
            <Tooltip title="实时面板仅在管理员端开放">
              <span>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<VideocamIcon />}
                  disabled
                  data-testid="live-workbench-realtime-button"
                  data-route-scope={routeScope}
                  data-target-path="unsupported"
                  data-contract-status="unsupported"
                  sx={{ fontSize: 12, py: 0.3 }}
                >
                  实时面板
                </Button>
              </span>
            </Tooltip>
          )}
        </Stack>

        {/* 5步骤 Chip 导航 */}
        <Box data-testid="live-workbench-step-nav-surface" sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
          {STEP_TO_TAB.map((t: WorkspaceTab, i: number) => {
            const active = step === i
            return (
              <Chip
                key={t}
                label={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Box sx={(theme) => {
                      const colors = getStepThemeColors(theme, t, active)
                      return {
                      '--live-workbench-step-badge-tone': colors.tone,
                      width: 16, height: 16, borderRadius: '50%',
                      bgcolor: colors.badgeBg,
                      color: colors.badgeColor,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 10, fontWeight: 700,
                    }}}>
                      {i + 1}
                    </Box>
                    {STEP_LABELS[t]}
                  </Box>
                }
                onClick={() => handleStepChange(i)}
                size="small"
                data-testid={active ? 'live-workbench-step-chip-active-surface' : undefined}
                data-step-tab={t}
                data-step-tone={STEP_COLOR_TONES[t]}
                data-step-active={active ? 'true' : 'false'}
                sx={(theme) => {
                  const colors = getStepThemeColors(theme, t, active)
                  return {
                  '--live-workbench-step-tone': colors.tone,
                  bgcolor: colors.chipBg,
                  color: colors.chipColor,
                  fontWeight: active ? 700 : 400,
                  border: '1px solid',
                  borderColor: colors.chipBorder,
                  cursor: 'pointer',
                  '&:hover': { bgcolor: colors.chipHoverBg, opacity: 0.92 },
                  '& .MuiChip-label': { px: 1 },
                }}}
              />
            )
          })}
        </Box>
      </Box>

      {(sessionIsError || readinessIsError || overviewIsError || operationError) && (
        <Alert
          severity={sessionIsError ? 'error' : 'warning'}
          sx={{ mx: 1.5, mt: 1, flexShrink: 0 }}
          data-testid="live-workbench-operation-alert"
          data-contract-sources={errorSources.join('|')}
          data-no-local-session-header-fallback="true"
          data-no-local-readiness-fallback="true"
          data-no-local-overview-fallback="true"
          data-no-admin-route-leak={routeScope !== 'admin' ? 'true' : 'n/a'}
          action={
            <Button
              color="inherit"
              size="small"
              onClick={() => { refetchSession(); refetchReadiness(); refetchOverview(); setOperationError('') }}
            >
              重试
            </Button>
          }
        >
          {sessionIsError ? `场次头信息加载失败：${getErrorMessage(sessionError)}。` : ''}
          {readinessIsError ? ` 就绪度不可用：${getErrorMessage(readinessError)}。` : ''}
          {overviewIsError ? ` 总览指标不可用：${getErrorMessage(overviewError)}。` : ''}
          {operationError ? ` ${operationError}` : ''}
        </Alert>
      )}

      {/* 工作区主体 */}
      <Box sx={{ flex: 1, overflow: 'hidden', minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        <SessionWorkspacePage
          sessionId={sessionId}
          step={step}
          onStepChange={handleStepChange}
        />
      </Box>

      {/* 导出短视频弹窗 */}
      <Dialog open={exportOpen} onClose={() => setExportOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>导出为短视频项目</DialogTitle>
        <DialogContent
          dividers
          data-testid="live-workbench-export-dialog"
          data-contract-source={LIVE_WORKBENCH_READY_ENDPOINTS.exportShortVideo}
          data-route-scope={routeScope}
          data-session-id={sessionId}
          data-shortvideo-target-template={exportTargetTemplate}
          data-input-retained={exportError ? 'true' : 'false'}
        >
          <Typography variant="body2" color="text.secondary" mb={2}>
            将「{session?.liveTitle}」的话术导出为短视频脚本，自动创建新项目。
          </Typography>
          {exportError ? (
            <Alert
              severity="error"
              sx={{ mb: 2 }}
              data-testid="live-workbench-export-error"
              data-contract-source={LIVE_WORKBENCH_READY_ENDPOINTS.exportShortVideo}
              data-input-retained="true"
              data-no-local-shortvideo-project="true"
            >
              {exportError}
            </Alert>
          ) : null}
          <FormControl fullWidth size="small">
            <InputLabel>脚本风格</InputLabel>
            <Select value={exportStyle} label="脚本风格" onChange={e => setExportStyle(e.target.value)}>
              <MenuItem value="professional">专业科普</MenuItem>
              <MenuItem value="viral">爆款风格</MenuItem>
              <MenuItem value="storytelling">故事叙述</MenuItem>
              <MenuItem value="interactive">强互动</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExportOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => exportMut.mutate()} disabled={exportMut.isPending}>
            确认导出
          </Button>
        </DialogActions>
      </Dialog>

      {/* 克隆场次弹窗 */}
      <Dialog open={cloneOpen} onClose={() => setCloneOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>克隆场次</DialogTitle>
        <DialogContent
          dividers
          data-testid="live-workbench-clone-dialog"
          data-contract-source={LIVE_WORKBENCH_READY_ENDPOINTS.clone}
          data-route-scope={routeScope}
          data-session-id={sessionId}
          data-target-path-template={liveSessionPath(routeScope, ':id')}
          data-input-retained={cloneError ? 'true' : 'false'}
        >
          <Typography variant="body2" color="text.secondary" mb={2}>
            将基于「{session?.liveTitle}」创建副本，包含商品选品、话术内容、节奏配置和生成参数。
          </Typography>
          {cloneError ? (
            <Alert
              severity="error"
              sx={{ mb: 2 }}
              data-testid="live-workbench-clone-error"
              data-contract-source={LIVE_WORKBENCH_READY_ENDPOINTS.clone}
              data-input-retained="true"
              data-no-local-session-clone="true"
            >
              {cloneError}
            </Alert>
          ) : null}
          <TextField
            fullWidth size="small" label="新场次名称"
            value={cloneName}
            onChange={e => setCloneName(e.target.value)}
            sx={{ mb: 1.5 }}
          />
          <FormControlLabel
            control={<Checkbox defaultChecked />}
            label="克隆商品选品与排序"
          />
          <br />
          <FormControlLabel
            control={<Checkbox defaultChecked />}
            label="克隆话术内容（当前激活版本）"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCloneOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => cloneMut.mutate()} disabled={cloneMut.isPending}>
            确认克隆
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
