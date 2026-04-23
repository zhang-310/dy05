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
import { useToast } from '@/contexts/ToastContext'
import { liveApi } from '@/api/live'

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

export default function LiveRealtimePanelPage() {
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
  const prevGmvRef = useRef(0)

  const fetchData = useCallback(() => {
    if (!sessionId) return
    liveApi.realtimePanelInit(sessionId)
      .then((res) => {
        const d: PanelData = {
          currentSlotIndex: Number(res.currentSlotIndex ?? 0),
          totalSlots: Number(res.totalSlots ?? 0),
          currentScript: String(res.currentScript ?? ''),
          currentProductName: String(res.currentProductName ?? ''),
          onlineCount: Number(res.onlineCount ?? 0),
          likeCount: Number(res.likeCount ?? 0),
          commentCount: Number(res.commentCount ?? 0),
          salesAmount: Number(res.salesAmount ?? 0),
          gmv: res.gmv != null ? Number(res.gmv) : undefined,
          targetGmv: res.targetGmv != null ? Number(res.targetGmv) : undefined,
          slotTitle: res.slotTitle != null ? String(res.slotTitle) : undefined,
          slotType: res.slotType != null ? String(res.slotType) : undefined,
          duration: res.duration != null ? Number(res.duration) : undefined,
        }
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
      .catch((e) => { console.error('Failed to fetch realtime panel data:', e); toast('获取实时数据失败', 'error') })
      .finally(() => setLoading(false))
  }, [sessionId])

  useEffect(() => {
    fetchData()
    const timer = setInterval(fetchData, 10000)
    return () => clearInterval(timer)
  }, [fetchData])

  // elapsed timer
  useEffect(() => {
    const t = setInterval(() => setElapsed(e => e + 1), 1000)
    return () => clearInterval(t)
  }, [])

  const formatTime = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

  const handleNavigate = async (dir: 'prev' | 'next') => {
    if (!data) return
    setNavigating(true)
    try {
      const nextIdx = dir === 'next' ? data.currentSlotIndex + 1 : data.currentSlotIndex - 1
      const res = await liveApi.realtimePanelNextSlot({ sessionId, currentSlotIndex: nextIdx })
      const d: PanelData = {
        currentSlotIndex: Number(res.currentSlotIndex ?? 0),
        totalSlots: Number(res.totalSlots ?? 0),
        currentScript: String(res.currentScript ?? ''),
        currentProductName: String(res.currentProductName ?? ''),
        onlineCount: Number(res.onlineCount ?? 0),
        likeCount: Number(res.likeCount ?? 0),
        commentCount: Number(res.commentCount ?? 0),
        salesAmount: Number(res.salesAmount ?? 0),
        gmv: res.gmv != null ? Number(res.gmv) : undefined,
        targetGmv: res.targetGmv != null ? Number(res.targetGmv) : undefined,
        slotTitle: res.slotTitle != null ? String(res.slotTitle) : undefined,
        slotType: res.slotType != null ? String(res.slotType) : undefined,
        duration: res.duration != null ? Number(res.duration) : undefined,
      }
      setData(d)
    } catch {
      toast('切换失败', 'error')
    } finally {
      setNavigating(false)
    }
  }

  if (loading) return <Box sx={{ p: 4, display: 'flex', justifyContent: 'center' }}><CircularProgress /></Box>
  if (!data) return <Box sx={{ p: 4 }}><Alert severity="warning">场次数据加载失败，请检查场次ID</Alert></Box>

  const kpis = [
    { label: '在线人数', value: (data.onlineCount ?? 0).toLocaleString(), color: '#4fc3f7', icon: '👥' },
    { label: '点赞数', value: (data.likeCount ?? 0).toLocaleString(), color: '#f48fb1', icon: '❤️' },
    { label: '评论数', value: (data.commentCount ?? 0).toLocaleString(), color: '#a5d6a7', icon: '💬' },
  ]
  const targetGmv = data.targetGmv ?? 0
  const gmvPct = targetGmv > 0 ? Math.min(100, (displayGmv / targetGmv) * 100) : 0
  const gmvBarColor = gmvPct >= 80 ? '#4caf50' : gmvPct >= 60 ? '#ff9800' : '#1976d2'

  const progress = data.totalSlots > 0 ? ((data.currentSlotIndex + 1) / data.totalSlots) * 100 : 0

  // Mock slot list for sidebar
  const slots: SlotItem[] = Array.from({ length: data.totalSlots }, (_, i) => ({
    index: i,
    title: i === data.currentSlotIndex ? (data.slotTitle ?? `槽位 ${i + 1}`) : `槽位 ${i + 1}`,
    type: i === data.currentSlotIndex ? (data.slotType ?? 'product') : 'product',
    duration: data.duration ?? 120,
    done: i < data.currentSlotIndex,
  }))
  const bgColor = fullscreen ? '#0a0a0a' : 'background.default'

  return (
    <Box sx={{ height: fullscreen ? '100vh' : 'calc(100vh - 48px)', bgcolor: bgColor, p: fullscreen ? 0 : 1.5, overflowY: 'auto' }}>
      {/* Top bar */}
      <Stack direction="row" justifyContent="space-between" alignItems="center"
        sx={{ px: fullscreen ? 2 : 0, py: 1, bgcolor: fullscreen ? '#111' : 'transparent' }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <FiberManualRecordIcon sx={{ color: '#f44336', fontSize: 14 }} />
          <Typography variant="body2" sx={{ color: fullscreen ? '#fff' : 'text.primary', fontWeight: 600 }}>直播中</Typography>
          <Chip label={formatTime(elapsed)} size="small" sx={{ bgcolor: fullscreen ? '#333' : undefined, color: fullscreen ? '#fff' : undefined }} />
          <Chip
            label={`${data.currentSlotIndex + 1} / ${data.totalSlots}`}
            size="small" variant="outlined"
            sx={{ color: fullscreen ? '#aaa' : undefined, borderColor: fullscreen ? '#555' : undefined }}
          />
        </Stack>
        <Stack direction="row" spacing={1}>
          <Tooltip title="话术列表">
            <IconButton size="small" onClick={() => setSidebarOpen(true)} sx={{ color: fullscreen ? '#aaa' : undefined }}>
              <MenuOpenIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={fullscreen ? '退出全屏' : '全屏提词'}>
            <IconButton size="small" onClick={() => setFullscreen(f => !f)} sx={{ color: fullscreen ? '#aaa' : undefined }}>
              {fullscreen ? <FullscreenExitIcon /> : <FullscreenIcon />}
            </IconButton>
          </Tooltip>
        </Stack>
      </Stack>

      {/* Progress bar */}
      <LinearProgress variant="determinate" value={progress} sx={{ height: 4, bgcolor: fullscreen ? '#333' : undefined }} />

      {/* Teleprompter */}
      <Paper
        elevation={fullscreen ? 0 : 2}
        sx={{
          mx: fullscreen ? 0 : 0, my: fullscreen ? 0 : 2,
          p: fullscreen ? 4 : 3,
          bgcolor: fullscreen ? '#111' : '#1a1a2e',
          minHeight: fullscreen ? 'calc(100vh - 200px)' : 280,
          display: 'flex', flexDirection: 'column', justifyContent: 'center',
          cursor: 'text', userSelect: 'text',
        }}
      >
        {data.currentProductName && (
          <Typography variant="caption" sx={{ color: '#888', mb: 2, display: 'block' }}>
            当前商品：{data.currentProductName}
          </Typography>
        )}
        <Typography
          sx={{
            color: '#fff',
            fontSize: fullscreen ? '2.2rem' : '1.5rem',
            lineHeight: 1.8,
            fontWeight: 500,
            letterSpacing: fullscreen ? '0.05em' : 0,
            textShadow: fullscreen ? '0 0 20px rgba(255,255,255,0.1)' : 'none',
          }}
        >
          {data.currentScript || '（当前槽位暂无话术）'}
        </Typography>
      </Paper>

      {/* GMV 大计数器 */}
      <Box sx={{ px: fullscreen ? 2 : 0, py: 1.5, position: 'relative' }}>
        <Card sx={{ bgcolor: fullscreen ? '#1a1a1a' : 'background.paper', border: '1px solid #ffe08244' }}>
          <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Box>
                <Typography variant="caption" sx={{ color: '#888' }}>💰 GMV 今日累计</Typography>
                <Box sx={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
                  <Typography sx={{
                    fontSize: 48, fontWeight: 700, color: '#ffe082',
                    fontVariantNumeric: 'tabular-nums', letterSpacing: 2, lineHeight: 1.1,
                  }}>
                    ¥{displayGmv.toLocaleString('zh-CN')}
                  </Typography>
                  {gmvDelta != null && gmvDelta > 0 && (
                    <Typography sx={{
                      position: 'absolute', top: -8, right: -60,
                      color: '#4caf50', fontWeight: 700, fontSize: 14,
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
                      <Typography variant="caption" sx={{ color: '#888' }}>目标 ¥{targetGmv.toLocaleString()}</Typography>
                      <Typography variant="caption" sx={{ color: gmvBarColor, fontWeight: 600 }}>{gmvPct.toFixed(1)}%</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={gmvPct}
                      sx={{ height: 6, borderRadius: 3, bgcolor: '#333',
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
        {kpis.map((k) => (
          <Grid item xs={4} key={k.label}>
            <Card sx={{ bgcolor: fullscreen ? '#1a1a1a' : 'background.paper', border: `1px solid ${k.color}33` }}>
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" sx={{ color: '#888' }}>{k.icon} {k.label}</Typography>
                <Typography variant="h6" sx={{ color: k.color, fontWeight: 700 }}>{k.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Navigation */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', mt: 2, pb: fullscreen ? 2 : 0 }}>
        <Button
          variant="outlined"
          startIcon={<NavigateBeforeIcon />}
          onClick={() => handleNavigate('prev')}
          disabled={navigating || (data.currentSlotIndex ?? 0) <= 0}
          sx={{ color: fullscreen ? '#fff' : undefined, borderColor: fullscreen ? '#444' : undefined, minWidth: 140 }}
        >
          上一条
        </Button>
        <Button
          variant="outlined"
          endIcon={<NavigateNextIcon />}
          onClick={() => handleNavigate('next')}
          disabled={navigating || (data.currentSlotIndex ?? 0) >= (data.totalSlots ?? 1) - 1}
          sx={{ color: fullscreen ? '#fff' : undefined, borderColor: fullscreen ? '#444' : undefined, minWidth: 140 }}
        >
          {navigating ? <CircularProgress size={16} /> : '下一条'}
        </Button>
      </Box>

      {/* Sidebar: slot list */}
      <Drawer anchor="right" open={sidebarOpen} onClose={() => setSidebarOpen(false)}>
        <Box sx={{ width: 300, p: 2 }}>
          <Typography variant="subtitle1" fontWeight={600} mb={1}>话术槽位列表</Typography>
          <Divider sx={{ mb: 1 }} />
          <List dense disablePadding>
            {slots.map((slot) => (
              <ListItem
                key={slot.index}
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

