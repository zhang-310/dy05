import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Chip,
  CircularProgress,
  IconButton,
  Popover,
  Tooltip,
  Typography,
  Button,
} from '@mui/material'
import VideocamIcon from '@mui/icons-material/Videocam'
import RefreshIcon from '@mui/icons-material/Refresh'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { getRecommendedVirals } from '@/api/shortvideo'

interface ViralVideo {
  id: number
  title?: string
  playCount?: number
  likeCount?: number
  viralScore?: number
  hookLine?: string
  [key: string]: unknown
}

interface ShortVideoInspirationPanelProps {
  /** 当将爆款开头改编为直播话术时的回调 */
  onAdaptHook?: (hookLine: string) => void
}

export function ShortVideoInspirationPanel({ onAdaptHook }: ShortVideoInspirationPanelProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const [videos, setVideos] = useState<ViralVideo[]>([])
  const [loading, setLoading] = useState(false)

  const open = Boolean(anchorEl)

  const loadVideos = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getRecommendedVirals({ limit: 10 })
      const list = Array.isArray(data) ? data : []
      setVideos(list.map((v: Record<string, unknown>) => ({
        id: Number(v.id ?? 0),
        title: v.title != null ? String(v.title) : undefined,
        playCount: v.playCount != null ? Number(v.playCount) : undefined,
        likeCount: v.likeCount != null ? Number(v.likeCount) : undefined,
        viralScore: v.viralScore != null ? Number(v.viralScore) : undefined,
        hookLine: v.hookLine != null ? String(v.hookLine) : undefined,
      })))
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (open && videos.length === 0) loadVideos()
  }, [open, videos.length, loadVideos])

  return (
    <>
      <Tooltip title="短视频灵感 — 复用爆款开头">
        <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)} color="secondary">
          <VideocamIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        slotProps={{ paper: { sx: { width: 320, maxHeight: 420 } } }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 1.5, py: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <VideocamIcon fontSize="small" color="secondary" />
            <Typography variant="subtitle2">短视频灵感</Typography>
          </Box>
          <IconButton size="small" onClick={loadVideos} disabled={loading}>
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ px: 1.5, display: 'block', mb: 0.5 }}>
          推荐爆款短视频，一键改编为直播开场话术
        </Typography>
        <Box sx={{ maxHeight: 330, overflow: 'auto', px: 1, pb: 1 }}>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={20} />
            </Box>
          ) : videos.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
              暂无推荐视频
            </Typography>
          ) : (
            videos.map((v) => (
              <Box
                key={v.id}
                sx={{
                  p: 1,
                  mb: 0.5,
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                  '&:hover': { bgcolor: 'action.hover' },
                }}
              >
                <Typography variant="body2" fontWeight={600} sx={{ fontSize: '0.85rem', mb: 0.5 }}>
                  {v.title ?? `视频 #${v.id}`}
                </Typography>
                <Box sx={{ display: 'flex', gap: 0.5, mb: 0.5 }}>
                  {v.playCount != null && (
                    <Chip
                      label={`${v.playCount > 10000 ? `${(v.playCount / 10000).toFixed(1)}万` : v.playCount} 播放`}
                      size="small"
                      sx={{ height: 18, fontSize: '0.7rem' }}
                    />
                  )}
                  {v.viralScore != null && (
                    <Chip label={`热度 ${v.viralScore}`} size="small" color="error" variant="outlined" sx={{ height: 18, fontSize: '0.7rem' }} />
                  )}
                </Box>
                {v.hookLine && (
                  <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem', mb: 0.5 }}>
                    {v.hookLine}
                  </Typography>
                )}
                {v.hookLine && onAdaptHook && (
                  <Button
                    size="small"
                    variant="outlined"
                    startIcon={<AutoAwesomeIcon />}
                    onClick={() => onAdaptHook(v.hookLine!)}
                    sx={{ height: 24, fontSize: '0.75rem' }}
                  >
                    改编为开场
                  </Button>
                )}
              </Box>
            ))
          )}
        </Box>
      </Popover>
    </>
  )
}
