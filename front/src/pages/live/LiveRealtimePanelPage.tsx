import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Box, Button, Card, CardContent, CircularProgress,
  Grid, Paper, Typography, Stack, Chip, IconButton,
  Drawer, List, ListItem, ListItemText, Divider,
  LinearProgress, Tooltip, Alert,
} from '@mui/material'
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import FullscreenIcon from '@mui/icons-material/Fullscreen'
import FullscreenExitIcon from '@mui/icons-material/FullscreenExit'
import MenuOpenIcon from '@mui/icons-material/MenuOpen'
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord'
import SyncIcon from '@mui/icons-material/Sync'
import { alpha, useTheme } from '@mui/material/styles'
import { useToast } from '@/contexts/ToastContext'
import { initializePanel, nextSlot, prevSlot, subscribeWithRetry } from '@/api/live-realtime'
import type { LiveSessionScriptSlotVO, PanelInitVO, SSEDataUpdateEvent, SSESlotChangeEvent } from '@/types/live-realtime'
import { getErrorMessage } from '@/utils/errorHandler'

interface PanelData {
  currentSlotIndex: number
  totalSlots: number
  currentScript: string
  currentProductName: string
  onlineCount: number
  likeCount: number
  commentCount: number
  salesAmount: number
  gmv?: number
  targetGmv?: number
  slotTitle?: string
  slotType?: string
  duration?: number
}

interface SlotItem {
  index: number
  title: string
  type: string
  duration: number
  done: boolean
}

type RealtimeMetricTone = 'info' | 'secondary' | 'success'
type GmvProgressTone = 'primary' | 'warning' | 'success'
type RealtimePanelTone = RealtimeMetricTone | GmvProgressTone | 'error'

const REALTIME_PANEL_READY_ENDPOINTS = {
  init: '/live/realtime-panel/init',
  stream: '/live/realtime-panel/stream/:liveSessionId',
  nextSlot: '/live/realtime-panel/next-slot',
  prevSlot: '/live/realtime-panel/prev-slot',
} as const

const REALTIME_PANEL_CONTEXT_ENDPOINTS = [
  REALTIME_PANEL_READY_ENDPOINTS.init,
  REALTIME_PANEL_READY_ENDPOINTS.stream,
  REALTIME_PANEL_READY_ENDPOINTS.nextSlot,
  REALTIME_PANEL_READY_ENDPOINTS.prevSlot,
]

const REALTIME_PANEL_UNSUPPORTED_ACTIONS = [
  'jump-slot-from-panel',
  'complete-slot-from-panel',
  'manual-realtime-data-write',
  'standalone-realtime-data-read',
  'local-panel-mock-fallback',
  'live-script-save',
  'shortvideo-export',
]

function firstFiniteNumber(...values: unknown[]): number | undefined {
  for (const value of values) {
    if (value == null || value === '') continue
    const numeric = Number(value)
    if (Number.isFinite(numeric)) return numeric
  }
  return undefined
}

function mapPanelData(panel: PanelInitVO): PanelData {
  const slots = Array.isArray(panel.slots) ? panel.slots : []
  const currentIndex = Number(panel.currentSlotIndex ?? panel.realtimeData?.currentSlotIndex ?? 0)
  const currentSlot = slots.find(slot => Number(slot.slotIndex ?? -1) === currentIndex) ?? slots[currentIndex]
  const realtimeData = panel.realtimeData
  const panelRecord = panel as unknown as Record<string, unknown>
  const realtimeRecord = (realtimeData ?? {}) as unknown as Record<string, unknown>
  const salesAmount = Number(realtimeData?.productPurchaseAmount ?? realtimeData?.giftAmount ?? 0)
  return {
    currentSlotIndex: currentIndex,
    totalSlots: slots.length,
    currentScript: String(currentSlot?.content ?? ''),
    currentProductName: '',
    onlineCount: Number(realtimeData?.viewerCount ?? 0),
    likeCount: Number(realtimeData?.likeCount ?? 0),
    commentCount: Number(realtimeData?.commentCount ?? 0),
    salesAmount,
    gmv: salesAmount,
    targetGmv: firstFiniteNumber(
      panelRecord.targetGmv,
      panelRecord.gmvTarget,
      panelRecord.targetSalesAmount,
      realtimeRecord.targetGmv,
      realtimeRecord.gmvTarget,
      realtimeRecord.targetSalesAmount,
    ),
    slotTitle: currentSlot ? `第${currentIndex + 1}段` : undefined,
    slotType: currentSlot?.scriptType,
    duration: currentSlot?.durationSeconds,
  }
}

function mapSlotData(slot: LiveSessionScriptSlotVO, totalSlots: number): PanelData {
  const currentIndex = Number(slot.slotIndex ?? 0)
  return {
    currentSlotIndex: currentIndex,
    totalSlots,
    currentScript: String(slot.content ?? ''),
    currentProductName: '',
    onlineCount: 0,
    likeCount: 0,
    commentCount: 0,
    salesAmount: 0,
    gmv: 0,
    slotTitle: `第${currentIndex + 1}段`,
    slotType: slot.scriptType,
    duration: slot.durationSeconds,
  }
}

function mapSlots(panel: PanelInitVO): SlotItem[] {
  const currentIndex = Number(panel.currentSlotIndex ?? panel.realtimeData?.currentSlotIndex ?? 0)
  return (panel.slots ?? []).map((slot, fallbackIndex) => {
    const index = Number(slot.slotIndex ?? fallbackIndex)
    return {
      index,
      title: `第${index + 1}段`,
      type: String(slot.scriptType ?? 'custom'),
      duration: Number(slot.durationSeconds ?? 0),
      done: Boolean(slot.isCompleted) || index < currentIndex,
    }
  })
}

function mapSseRealtimeUpdate(event: SSEDataUpdateEvent, prev: PanelData | null): PanelData | null {
  if (!prev) return prev
  return {
    ...prev,
    onlineCount: Number(event.viewerCount ?? prev.onlineCount ?? 0),
    likeCount: Number(event.likeCount ?? prev.likeCount ?? 0),
    commentCount: Number(event.commentCount ?? prev.commentCount ?? 0),
  }
}

function mapSseSlotChange(event: SSESlotChangeEvent, prev: PanelData | null): PanelData | null {
  if (!prev) return prev
  const currentIndex = Number(event.currentSlotIndex ?? prev.currentSlotIndex ?? 0)
  return {
    ...prev,
    currentSlotIndex: currentIndex,
    currentScript: String(event.content ?? prev.currentScript ?? ''),
    slotTitle: `第${currentIndex + 1}段`,
    duration: Number(event.durationSeconds ?? prev.duration ?? 0),
  }
}

export default function LiveRealtimePanelPage() {
  const theme = useTheme()
  const { id } = useParams<{ id: string }>()
  const sessionId = Number(id)
  const toast = useToast()

  const [data, setData] = useState<PanelData | null>(null)
  const [loading, setLoading] = useState(true)
  const [navigating, setNavigating] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [elapsed, setElapsed] = useState(0)
  const [displayGmv, setDisplayGmv] = useState(0)
  const [gmvDelta, setGmvDelta] = useState<number | null>(null)
  const [slots, setSlots] = useState<SlotItem[]>([])
  const [streamMode, setStreamMode] = useState<'connecting' | 'sse' | 'polling'>('connecting')
  const [streamError, setStreamError] = useState('')
  const [streamRetryStatus, setStreamRetryStatus] = useState('')
  const [streamRetryCount, setStreamRetryCount] = useState(0)
  const [operationError, setOperationError] = useState('')
  const prevGmvRef = useRef(0)
  const routeContext = `/admin/live/sessions/${sessionId || id || '空'}/realtime`

  const fetchData = useCallback(() => {
    if (!sessionId) return
    initializePanel(sessionId)
      .then((res) => {
        setOperationError('')
        setSlots(mapSlots(res))
        const d = mapPanelData(res)
        setData(d)
        const newGmv = d.gmv ?? d.salesAmount ?? 0
        const prev = prevGmvRef.current
        if (newGmv > prev) {
          setGmvDelta(newGmv - prev)
          setTimeout(() => setGmvDelta(null), 1500)
        }
        // countUp animation
        const start = prev
        const end = newGmv
        const duration = 500
        const startTime = performance.now()
        const tick = (now: number) => {
          const t = Math.min((now - startTime) / duration, 1)
          setDisplayGmv(Math.round(start + (end - start) * t))
          if (t < 1) requestAnimationFrame(tick)
        }
        requestAnimationFrame(tick)
        prevGmvRef.current = newGmv
      })
      .catch((e) => {
        console.error('Failed to fetch realtime panel data:', e)
        setOperationError(`${REALTIME_PANEL_READY_ENDPOINTS.init} 获取实时面板失败：${getErrorMessage(e)}。上下文：route=${routeContext}; liveSessionId=${sessionId || '空'}`)
        toast('获取实时数据失败', 'error')
      })
      .finally(() => setLoading(false))
  }, [sessionId])

  useEffect(() => {
    fetchData()
    const timer = setInterval(fetchData, 10000)
    return () => clearInterval(timer)
  }, [fetchData])

  useEffect(() => {
    if (!sessionId || typeof EventSource === 'undefined') {
      setStreamMode('polling')
      setStreamError(`当前浏览器或运行环境不支持 EventSource，已使用 10 秒轮询。上下文：route=${routeContext}; endpoint=/live/realtime-panel/stream/${sessionId || '空'}`)
      return
    }

    const subscription = subscribeWithRetry(sessionId, {
      onConnected: () => {
        setStreamMode('sse')
        setStreamError('')
        setStreamRetryStatus('')
        setStreamRetryCount(0)
      },
      onDataUpdate: (event) => {
        setData(prev => mapSseRealtimeUpdate(event, prev))
      },
      onSlotChange: (event) => {
        setData(prev => mapSseSlotChange(event, prev))
        setSlots(prev => prev.map(slot => ({
          ...slot,
          done: slot.done || slot.index < Number(event.currentSlotIndex ?? 0),
        })))
      },
      onError: (error) => {
        setStreamMode('polling')
        setStreamError(`/live/realtime-panel/stream/${sessionId} SSE 连接异常：${getErrorMessage(error)}。上下文：route=${routeContext}; liveSessionId=${sessionId}; retry=2; retryDelayMs=1500。已降级为 10 秒轮询。`)
      },
      onRetryScheduled: ({ retryCount, maxRetries, retryDelayMs, error }) => {
        setStreamMode('polling')
        setStreamRetryCount(retryCount)
        setStreamRetryStatus(`SSE 正在重试：${retryDelayMs}ms 后第 ${retryCount}/${maxRetries} 次重连；最近错误：${getErrorMessage(error)}。`)
      },
      onRetryExhausted: ({ retryCount, maxRetries, error }) => {
        setStreamMode('polling')
        setStreamRetryCount(retryCount)
        setStreamRetryStatus(`SSE 重试已用尽：${retryCount}/${maxRetries} 次失败；最近错误：${getErrorMessage(error)}。继续使用 10 秒轮询。`)
      },
    }, 2, 1500)

    return () => subscription.close()
  }, [sessionId])

  // elapsed timer
  useEffect(() => {
    const t = setInterval(() => setElapsed(e => e + 1), 1000)
    return () => clearInterval(t)
  }, [])

  const formatTime = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

  const handleNavigate = async (dir: 'prev' | 'next') => {
    if (!data) return
    setNavigating(true)
    setOperationError('')
    try {
      const res = dir === 'next' ? await nextSlot(sessionId) : await prevSlot(sessionId)
      setData(prev => ({
        ...mapSlotData(res, prev?.totalSlots ?? slots.length),
        onlineCount: prev?.onlineCount ?? 0,
        likeCount: prev?.likeCount ?? 0,
        commentCount: prev?.commentCount ?? 0,
        salesAmount: prev?.salesAmount ?? 0,
        gmv: prev?.gmv,
        targetGmv: prev?.targetGmv,
      }))
      setSlots(prev => prev.map(slot => ({
        ...slot,
        done: slot.done || (dir === 'next' && slot.index < Number(res.slotIndex ?? 0)),
      })))
    } catch (error) {
      const endpoint = dir === 'next' ? REALTIME_PANEL_READY_ENDPOINTS.nextSlot : REALTIME_PANEL_READY_ENDPOINTS.prevSlot
      setOperationError(`${endpoint} 切换话术失败：${getErrorMessage(error)}。上下文：route=${routeContext}; liveSessionId=${sessionId}; direction=${dir}; currentSlotIndex=${data.currentSlotIndex}; totalSlots=${data.totalSlots}`)
      toast('切换失败', 'error')
    } finally {
      setNavigating(false)
    }
  }

  if (loading) return <Box sx={{ p: 4, display: 'flex', justifyContent: 'center' }}><CircularProgress /></Box>
  if (!data) return (
    <Box
      sx={{ p: 4 }}
      data-testid="live-realtime-init-failure"
      data-contract-scope="live-realtime-panel"
      data-ready-endpoints={Object.values(REALTIME_PANEL_READY_ENDPOINTS).join('|')}
      data-context-endpoints={REALTIME_PANEL_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={REALTIME_PANEL_UNSUPPORTED_ACTIONS.join('|')}
      data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
      data-session-id={sessionId || 0}
      data-no-local-panel-fallback="true"
    >
      <Alert
        severity="warning"
        data-testid="live-realtime-init-error"
        data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
        data-no-local-panel-fallback="true"
        action={<Button color="inherit" size="small" onClick={fetchData}>重试</Button>}
      >
        {operationError || '场次数据加载失败，请检查场次 ID、话术槽位是否初始化，以及 `/live/realtime-panel/init` 后端接口。'}
      </Alert>
    </Box>
  )

  const metricColor = (tone: RealtimePanelTone) => theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
  const kpis: Array<{ label: string; value: string; tone: RealtimeMetricTone; icon: string }> = [
    { label: '在线人数', value: (data.onlineCount ?? 0).toLocaleString(), tone: 'info', icon: '👥' },
    { label: '点赞数', value: (data.likeCount ?? 0).toLocaleString(), tone: 'secondary', icon: '❤️' },
    { label: '评论数', value: (data.commentCount ?? 0).toLocaleString(), tone: 'success', icon: '💬' },
  ]
  const targetGmv = data.targetGmv ?? 0
  const gmvPct = targetGmv > 0 ? Math.min(100, (displayGmv / targetGmv) * 100) : 0
  const gmvProgressTone: GmvProgressTone = gmvPct >= 80 ? 'success' : gmvPct >= 60 ? 'warning' : 'primary'
  const gmvBarColor = metricColor(gmvProgressTone)
  const gmvDeltaColor = metricColor('success')
  const gmvAccentColor = metricColor('warning')

  const progress = data.totalSlots > 0 ? ((data.currentSlotIndex + 1) / data.totalSlots) * 100 : 0

  const liveStatusColor = metricColor('error')
  const fullscreenTextColor = theme.palette.common.white
  const fullscreenMutedColor = alpha(theme.palette.common.white, 0.68)
  const fullscreenSoftBg = alpha(theme.palette.common.white, theme.palette.mode === 'dark' ? 0.07 : 0.1)
  const fullscreenBorderColor = alpha(theme.palette.common.white, theme.palette.mode === 'dark' ? 0.22 : 0.3)
  const prompterBg = fullscreen
    ? alpha(theme.palette.common.white, theme.palette.mode === 'dark' ? 0.07 : 0.09)
    : theme.palette.mode === 'dark'
      ? alpha(theme.palette.common.white, 0.06)
      : alpha(theme.palette.primary.dark, 0.94)
  const prompterBorderColor = fullscreen ? fullscreenBorderColor : alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.28 : 0.2)
  const prompterMutedColor = fullscreen ? fullscreenMutedColor : alpha(theme.palette.common.white, 0.66)
  const slotProgressColor = metricColor('primary')
  const slotProgressTrackColor = fullscreen ? alpha(theme.palette.common.white, 0.16) : alpha(slotProgressColor, theme.palette.mode === 'dark' ? 0.2 : 0.12)
  const realtimeCardBg = fullscreen
    ? alpha(theme.palette.common.white, theme.palette.mode === 'dark' ? 0.07 : 0.09)
    : 'background.paper'
  const realtimeCardCaptionColor = fullscreen ? fullscreenMutedColor : 'text.secondary'
  const gmvCardBorderColor = alpha(gmvAccentColor, theme.palette.mode === 'dark' ? 0.38 : 0.28)
  const bgColor = fullscreen ? theme.palette.common.black : 'background.default'

  return (
    <Box
      data-testid="live-realtime-panel-root-surface"
      data-contract-scope="live-realtime-panel"
      data-ready-endpoints={Object.values(REALTIME_PANEL_READY_ENDPOINTS).join('|')}
      data-context-endpoints={REALTIME_PANEL_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={REALTIME_PANEL_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={sessionId || 0}
      data-current-slot-index={data.currentSlotIndex}
      data-total-slots={data.totalSlots}
      data-slot-count={slots.length}
      data-stream-mode={streamMode}
      data-stream-retry-count={streamRetryCount}
      data-stream-retry-diagnostic={streamRetryStatus}
      data-no-local-panel-fallback="true"
      data-fullscreen={fullscreen ? 'true' : 'false'}
      sx={{ height: fullscreen ? '100vh' : 'calc(100vh - 48px)', bgcolor: bgColor, p: fullscreen ? 0 : 1.5, overflowY: 'auto' }}
    >
      {/* Top bar */}
      <Stack direction="row" justifyContent="space-between" alignItems="center"
        data-testid="live-realtime-topbar-surface"
        data-surface-tone={fullscreen ? 'fullscreen' : 'transparent'}
        sx={{ px: fullscreen ? 2 : 0, py: 1, bgcolor: fullscreen ? fullscreenSoftBg : 'transparent' }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <FiberManualRecordIcon
            data-testid="live-realtime-live-status-dot"
            data-status-tone="error"
            data-status-color={liveStatusColor}
            sx={{ color: liveStatusColor, fontSize: 14 }}
          />
          <Typography variant="body2" sx={{ color: fullscreen ? fullscreenTextColor : 'text.primary', fontWeight: 600 }}>直播中</Typography>
          <Chip
            label={formatTime(elapsed)}
            size="small"
            data-testid="live-realtime-elapsed-chip-surface"
            data-chip-tone={fullscreen ? 'fullscreen' : 'default'}
            sx={{ bgcolor: fullscreen ? fullscreenSoftBg : undefined, color: fullscreen ? fullscreenTextColor : undefined }}
          />
          <Tooltip title={streamMode === 'sse' ? 'SSE 实时推送已连接；轮询仍作为兜底刷新。' : (streamRetryStatus || streamError || '正在连接 SSE，暂以轮询兜底。')}>
            <Chip
              icon={<SyncIcon />}
              label={streamMode === 'sse' ? 'SSE 实时' : '轮询兜底'}
              size="small"
              data-testid="live-realtime-stream-mode-chip"
              data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.stream}
              data-stream-mode={streamMode}
              data-polling-fallback-sec="10"
              color={streamMode === 'sse' ? 'success' : 'warning'}
              variant={fullscreen ? 'outlined' : 'filled'}
              sx={{ color: fullscreen ? fullscreenTextColor : undefined, borderColor: fullscreen ? fullscreenBorderColor : undefined }}
            />
          </Tooltip>
          <Chip
            label={`${data.currentSlotIndex + 1} / ${data.totalSlots}`}
            size="small" variant="outlined"
            data-testid="live-realtime-slot-count-chip-surface"
            data-chip-tone={fullscreen ? 'fullscreen-muted' : 'default'}
            sx={{ color: fullscreen ? fullscreenMutedColor : undefined, borderColor: fullscreen ? fullscreenBorderColor : undefined }}
          />
        </Stack>
        <Stack direction="row" spacing={1}>
          <Tooltip title="话术列表">
            <IconButton aria-label="话术列表" size="small" onClick={() => setSidebarOpen(true)} sx={{ color: fullscreen ? fullscreenMutedColor : undefined }}>
              <MenuOpenIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={fullscreen ? '退出全屏' : '全屏提词'}>
            <IconButton aria-label={fullscreen ? '退出全屏' : '全屏提词'} size="small" onClick={() => setFullscreen(f => !f)} sx={{ color: fullscreen ? fullscreenMutedColor : undefined }}>
              {fullscreen ? <FullscreenExitIcon /> : <FullscreenIcon />}
            </IconButton>
          </Tooltip>
        </Stack>
      </Stack>

      {streamMode === 'polling' && !fullscreen ? (
        <Alert
          severity="info"
          sx={{ mb: 1 }}
          data-testid="live-realtime-stream-downgrade-alert"
          data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.stream}
          data-stream-mode={streamMode}
          data-polling-fallback-sec="10"
          data-stream-retry-count={streamRetryCount}
          data-stream-retry-diagnostic={streamRetryStatus}
          data-no-local-panel-fallback="true"
        >
          {streamError || 'SSE 未连接，当前使用 10 秒轮询刷新实时数据。提词与上下条切换仍可正常使用。'}
          {streamRetryStatus ? ` ${streamRetryStatus}` : ''}
        </Alert>
      ) : null}

      {operationError && !fullscreen ? (
        <Alert
          severity="error"
          sx={{ mb: 1 }}
          data-testid="live-realtime-operation-error"
          data-contract-source={operationError.startsWith(REALTIME_PANEL_READY_ENDPOINTS.prevSlot) ? REALTIME_PANEL_READY_ENDPOINTS.prevSlot : operationError.startsWith(REALTIME_PANEL_READY_ENDPOINTS.nextSlot) ? REALTIME_PANEL_READY_ENDPOINTS.nextSlot : REALTIME_PANEL_READY_ENDPOINTS.init}
          data-current-slot-index={data.currentSlotIndex}
          data-no-local-slot-fallback="true"
          action={<Button color="inherit" size="small" onClick={fetchData}>重试初始化</Button>}
        >
          {operationError}。页面保留当前槽位与指标，不使用本地 mock 覆盖真实状态。
        </Alert>
      ) : null}

      {/* Progress bar */}
      <LinearProgress
        variant="determinate"
        value={progress}
        data-testid="live-realtime-slot-progress-surface"
        data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
        data-progress-tone="primary"
        data-track-tone={fullscreen ? 'fullscreen' : 'primary'}
        data-progress-color={slotProgressColor}
        sx={{ height: 4, bgcolor: slotProgressTrackColor, '& .MuiLinearProgress-bar': { bgcolor: slotProgressColor } }}
      />

      {/* Teleprompter */}
      <Paper
        elevation={fullscreen ? 0 : 2}
        data-testid="live-realtime-teleprompter-surface"
        data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
        data-prompter-tone={fullscreen ? 'fullscreen' : 'primary'}
        data-current-slot-index={data.currentSlotIndex}
        sx={{
          mx: fullscreen ? 0 : 0, my: fullscreen ? 0 : 2,
          p: fullscreen ? 4 : 3,
          bgcolor: prompterBg,
          border: '1px solid',
          borderColor: prompterBorderColor,
          minHeight: fullscreen ? 'calc(100vh - 200px)' : 280,
          display: 'flex', flexDirection: 'column', justifyContent: 'center',
          cursor: 'text', userSelect: 'text',
        }}
      >
        {data.currentProductName && (
          <Typography variant="caption" sx={{ color: prompterMutedColor, mb: 2, display: 'block' }}>
            当前商品：{data.currentProductName}
          </Typography>
        )}
        <Typography
          data-testid="live-realtime-teleprompter-text-surface"
          data-text-tone="inverse"
          sx={{
            color: fullscreenTextColor,
            fontSize: fullscreen ? '2.2rem' : '1.5rem',
            lineHeight: 1.8,
            fontWeight: 500,
            letterSpacing: fullscreen ? '0.05em' : 0,
            textShadow: fullscreen ? `0 0 20px ${alpha(theme.palette.common.white, 0.1)}` : 'none',
          }}
        >
          {data.currentScript || '（当前槽位暂无话术）'}
        </Typography>
      </Paper>

      {/* GMV 大计数器 */}
      <Box sx={{ px: fullscreen ? 2 : 0, py: 1.5, position: 'relative' }}>
        <Card
          data-testid="live-realtime-gmv-card-surface"
          data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
          data-card-tone="warning"
          data-card-bg-tone={fullscreen ? 'fullscreen' : 'paper'}
          data-card-accent-color={gmvAccentColor}
          sx={{ bgcolor: realtimeCardBg, border: '1px solid', borderColor: gmvCardBorderColor }}
        >
          <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Box>
                <Typography
                  variant="caption"
                  data-testid="live-realtime-gmv-caption-surface"
                  data-caption-tone={fullscreen ? 'fullscreen-muted' : 'text-secondary'}
                  sx={{ color: realtimeCardCaptionColor }}
                >💰 GMV 今日累计</Typography>
                <Box sx={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
                  <Typography
                    data-testid="live-realtime-gmv-value-surface"
                    data-gmv-tone="warning"
                    data-gmv-color={gmvAccentColor}
                    sx={{
                    fontSize: 48, fontWeight: 700, color: gmvAccentColor,
                    fontVariantNumeric: 'tabular-nums', letterSpacing: 2, lineHeight: 1.1,
                  }}>
                    ¥{displayGmv.toLocaleString('zh-CN')}
                  </Typography>
                  {gmvDelta != null && gmvDelta > 0 && (
                    <Typography
                      data-testid="live-realtime-gmv-delta-surface"
                      data-delta-color={gmvDeltaColor}
                      sx={{
                      position: 'absolute', top: -8, right: -60,
                      color: gmvDeltaColor, fontWeight: 700, fontSize: 14,
                      animation: 'fadeUp 1.5s ease forwards',
                      '@keyframes fadeUp': {
                        '0%': { opacity: 1, transform: 'translateY(0)' },
                        '100%': { opacity: 0, transform: 'translateY(-20px)' },
                      },
                    }}>
                      +¥{gmvDelta.toLocaleString()}
                    </Typography>
                  )}
                </Box>
                {targetGmv > 0 && (
                  <Box sx={{ mt: 0.5, minWidth: 240 }}>
                    <Stack direction="row" justifyContent="space-between" mb={0.3}>
                      <Typography
                        variant="caption"
                        data-testid="live-realtime-gmv-target-caption-surface"
                        data-caption-tone={fullscreen ? 'fullscreen-muted' : 'text-secondary'}
                        sx={{ color: realtimeCardCaptionColor }}
                      >目标 ¥{targetGmv.toLocaleString()}</Typography>
                      <Typography
                        variant="caption"
                        data-testid="live-realtime-gmv-progress-label"
                        data-progress-tone={gmvProgressTone}
                        data-progress-color={gmvBarColor}
                        sx={{ color: gmvBarColor, fontWeight: 600 }}
                      >{gmvPct.toFixed(1)}%</Typography>
                    </Stack>
                    <LinearProgress
                      variant="determinate"
                      value={gmvPct}
                      data-testid="live-realtime-gmv-progress-surface"
                      data-progress-tone={gmvProgressTone}
                      data-progress-color={gmvBarColor}
                      sx={{ height: 6, borderRadius: 3, bgcolor: alpha(gmvBarColor, theme.palette.mode === 'dark' ? 0.16 : 0.12),
                        '& .MuiLinearProgress-bar': { bgcolor: gmvBarColor } }} />
                  </Box>
                )}
              </Box>
            </Stack>
          </CardContent>
        </Card>
      </Box>

      {/* KPI cards */}
      <Grid container spacing={1.5} sx={{ px: fullscreen ? 2 : 0, py: fullscreen ? 1 : 0 }}>
        {kpis.map((k) => {
          const color = metricColor(k.tone)
          return (
          <Grid item xs={4} key={k.label}>
            <Card
              data-testid="live-realtime-kpi-card-surface"
              data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
              data-kpi-tone={k.tone}
              data-kpi-color={color}
              data-card-bg-tone={fullscreen ? 'fullscreen' : 'paper'}
              sx={{ bgcolor: realtimeCardBg, border: `1px solid ${alpha(color, theme.palette.mode === 'dark' ? 0.36 : 0.22)}` }}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography
                  variant="caption"
                  data-testid="live-realtime-kpi-caption-surface"
                  data-caption-tone={fullscreen ? 'fullscreen-muted' : 'text-secondary'}
                  sx={{ color: realtimeCardCaptionColor }}
                >{k.icon} {k.label}</Typography>
                <Typography
                  variant="h6"
                  data-testid="live-realtime-kpi-value-surface"
                  data-kpi-tone={k.tone}
                  data-kpi-color={color}
                  sx={{ color, fontWeight: 700 }}
                >{k.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        )})}
      </Grid>

      {/* Navigation */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', mt: 2, pb: fullscreen ? 2 : 0 }}>
        <Button
          variant="outlined"
          startIcon={<NavigateBeforeIcon />}
          onClick={() => handleNavigate('prev')}
          disabled={navigating || (data.currentSlotIndex ?? 0) <= 0}
          data-testid="live-realtime-prev-button"
          data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.prevSlot}
          sx={{ color: fullscreen ? fullscreenTextColor : undefined, borderColor: fullscreen ? fullscreenBorderColor : undefined, minWidth: 140 }}
        >
          上一条
        </Button>
        <Button
          variant="outlined"
          endIcon={<NavigateNextIcon />}
          onClick={() => handleNavigate('next')}
          disabled={navigating || (data.currentSlotIndex ?? 0) >= (data.totalSlots ?? 1) - 1}
          data-testid="live-realtime-next-button"
          data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.nextSlot}
          sx={{ color: fullscreen ? fullscreenTextColor : undefined, borderColor: fullscreen ? fullscreenBorderColor : undefined, minWidth: 140 }}
        >
          {navigating ? <CircularProgress size={16} /> : '下一条'}
        </Button>
      </Box>

      {/* Sidebar: slot list */}
      <Drawer anchor="right" open={sidebarOpen} onClose={() => setSidebarOpen(false)}>
        <Box
          sx={{ width: 300, p: 2 }}
          data-testid="live-realtime-slot-drawer"
          data-contract-source={REALTIME_PANEL_READY_ENDPOINTS.init}
          data-slot-count={slots.length}
          data-no-local-slot-fallback="true"
        >
          <Typography variant="subtitle1" fontWeight={600} mb={1}>话术槽位列表</Typography>
          <Divider sx={{ mb: 1 }} />
          <List dense disablePadding>
            {slots.length === 0 && (
              <ListItem>
                <ListItemText primary="暂无话术槽位" secondary="请先生成或初始化本场直播话术" />
              </ListItem>
            )}
            {slots.map((slot) => (
              <ListItem
                key={slot.index}
                data-testid="live-realtime-slot-list-item"
                data-slot-index={slot.index}
                data-slot-done={String(slot.done)}
                sx={{
                  bgcolor: slot.index === data.currentSlotIndex ? 'primary.light' : 'transparent',
                  borderRadius: 1, mb: 0.5,
                  opacity: slot.done ? 0.5 : 1,
                }}
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography variant="body2" fontWeight={slot.index === data.currentSlotIndex ? 700 : 400}>
                        {slot.index + 1}. {slot.title}
                      </Typography>
                      {slot.index === data.currentSlotIndex && <Chip label="当前" size="small" color="primary" />}
                      {slot.done && <Chip label="已播" size="small" color="success" variant="outlined" />}
                    </Stack>
                  }
                  secondary={`${slot.type} · ${slot.duration}s`}
                />
              </ListItem>
            ))}
          </List>
        </Box>
      </Drawer>
    </Box>
  )
}

