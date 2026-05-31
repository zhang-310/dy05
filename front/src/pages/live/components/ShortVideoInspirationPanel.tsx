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
import { getErrorMessage } from '@/utils/errorHandler'

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
  const [loadError, setLoadError] = useState<string | null>(null)

  const open = Boolean(anchorEl)

  const loadVideos = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
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
    } catch (e) {
      setVideos([])
      setLoadError(`/short-video/viral/recommended 短视频灵感加载失败：${getErrorMessage(e)}`)
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
        <IconButton
          size="small"
          onClick={(e) => setAnchorEl(e.currentTarget)}
          color="secondary"
          data-testid="shortvideo-inspiration-open-button"
          data-contract-source="/short-video/viral/recommended"
        >
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
        <Box
          data-testid="shortvideo-inspiration-panel"
          data-contract-scope="live-shortvideo-inspiration-readonly"
          data-ready-endpoints="/short-video/viral/recommended"
          data-unsupported-actions="local-viral-video-fallback|shortvideo-mutation|script-mutation"
          data-video-count={videos.length}
          data-state={loadError ? 'error' : loading ? 'loading' : 'ready'}
          data-no-local-viral-video-fallback="true"
          sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 1.5, py: 1 }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <VideocamIcon fontSize="small" color="secondary" />
            <Typography variant="subtitle2">短视频灵感</Typography>
          </Box>
          <IconButton
            size="small"
            onClick={loadVideos}
            disabled={loading}
            data-testid="shortvideo-inspiration-refresh-button"
            data-contract-source="/short-video/viral/recommended"
          >
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Box>
        <Typography
          variant="caption"
          color="text.secondary"
          data-testid="shortvideo-inspiration-contract-note"
          data-no-local-viral-video-fallback="true"
          sx={{ px: 1.5, display: 'block', mb: 0.5 }}
        >
          推荐爆款短视频，一键改编为直播开场话术
        </Typography>
        <Box
          data-testid="shortvideo-inspiration-list"
          data-contract-source="/short-video/viral/recommended"
          data-no-local-viral-video-fallback="true"
          sx={{ maxHeight: 330, overflow: 'auto', px: 1, pb: 1 }}
        >
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={20} />
            </Box>
          ) : loadError ? (
            <Typography
              data-testid="shortvideo-inspiration-error"
              data-contract-source="/short-video/viral/recommended"
              data-no-local-viral-video-fallback="true"
              variant="body2"
              color="error"
              sx={{ textAlign: 'center', py: 2 }}
            >
              {loadError}
            </Typography>
          ) : videos.length === 0 ? (
            <Typography
              data-testid="shortvideo-inspiration-empty-state"
              data-contract-source="/short-video/viral/recommended"
              data-no-local-viral-video-fallback="true"
              variant="body2"
              color="text.secondary"
              sx={{ textAlign: 'center', py: 2 }}
            >
              暂无推荐视频
            </Typography>
          ) : (
            videos.map((v) => (
              <Box
                key={v.id}
                data-testid="shortvideo-inspiration-item"
                data-contract-source="/short-video/viral/recommended"
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
                    data-testid="shortvideo-inspiration-adapt-button"
                    data-contract-source="onAdaptHook-prop"
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
