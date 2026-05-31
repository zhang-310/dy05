import { useState, useRef, useEffect } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { alpha } from '@mui/material/styles'
import { PlayArrow, FiberManualRecord } from '@mui/icons-material'

const TIMELINE_CLIP_LABEL_OPACITY = 0.7

export interface TimelineClip {
  url: string
  duration?: number
  label?: string
}

export interface TimelineAudioTrack {
  url: string
  label?: string
}

interface VideoTimelineProps {
  clips: TimelineClip[]
  audioTrack?: TimelineAudioTrack
  bgmUrl?: string
  onOrderChange?: (urls: string[]) => void
  defaultDuration?: number
}

/** 视频时间轴：支持视频轨 + 可选配音/BGM 轨 */
export function VideoTimeline({ clips, audioTrack, bgmUrl, defaultDuration = 5 }: VideoTimelineProps) {
  const [durations, setDurations] = useState<Record<number, number>>({})
  const [playingIndex, setPlayingIndex] = useState<number | null>(null)
  const videoRefs = useRef<Record<number, HTMLVideoElement | null>>({})

  useEffect(() => {
    const next: Record<number, number> = {}
    clips.forEach((c, i) => {
      next[i] = c.duration ?? defaultDuration
    })
    setDurations((prev) => ({ ...prev, ...next }))
  }, [clips, defaultDuration])

  const totalDuration = clips.reduce((sum, _, i) => sum + (durations[i] ?? defaultDuration), 0)
  const scale = 120 // px per second

  const handleLoadedMetadata = (index: number, e: React.SyntheticEvent<HTMLVideoElement>) => {
    const v = e.currentTarget
    if (v.duration && !isNaN(v.duration)) {
      setDurations((prev) => ({ ...prev, [index]: Math.round(v.duration) }))
    }
  }

  const handlePlay = (index: number) => {
    Object.values(videoRefs.current).forEach((el, i) => {
      if (el && i !== index) el.pause()
    })
    setPlayingIndex(index)
  }

  const handlePause = () => setPlayingIndex(null)

  if (clips.length === 0) return null

  return (
    <Paper
      variant="outlined"
      data-testid="shortvideo-video-timeline"
      data-media-tone="timeline-stage"
      sx={{ p: 2, bgcolor: 'grey.900', color: 'grey.100' }}
    >
      <Typography variant="caption" color="grey.400" sx={{ mb: 1, display: 'block' }}>
        时间轴 · 共 {clips.length} 段 · 总时长约 {totalDuration}s
        {audioTrack && ' · 配音轨'}
        {bgmUrl && ' · BGM'}
      </Typography>
      <Box
        sx={{
          display: 'flex',
          gap: 0.5,
          overflowX: 'auto',
          pb: 1,
          minHeight: 88,
          alignItems: 'flex-end',
        }}
      >
        {clips.map((clip, index) => {
          const dur = durations[index] ?? defaultDuration
          const width = Math.max(60, Math.min(160, dur * scale * 0.25))
          return (
            <Box
              key={`${index}-${clip.url}`}
              sx={{
                flexShrink: 0,
                width,
                borderRadius: 1,
                overflow: 'hidden',
                bgcolor: 'grey.800',
                border: '1px solid',
                borderColor: playingIndex === index ? 'primary.main' : 'grey.700',
                cursor: 'pointer',
                '&:hover': { borderColor: 'grey.500' },
              }}
              onClick={() => {
                const v = videoRefs.current[index]
                if (v) {
                  if (v.paused) {
                    v.play()
                    handlePlay(index)
                  } else {
                    v.pause()
                    handlePause()
                  }
                }
              }}
            >
              <Box sx={{ position: 'relative', height: 56, bgcolor: 'grey.800' }}>
                <video
                  ref={(el) => { videoRefs.current[index] = el }}
                  src={clip.url}
                  muted
                  preload="metadata"
                  playsInline
                  onLoadedMetadata={(e) => handleLoadedMetadata(index, e)}
                  onPlay={() => handlePlay(index)}
                  onPause={handlePause}
                  onEnded={handlePause}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                    display: 'block',
                  }}
                />
                <Box
                  data-testid="shortvideo-video-timeline-clip-label"
                  data-media-tone="clip-label-overlay"
                  sx={{
                    position: 'absolute',
                    top: 4,
                    left: 4,
                    bgcolor: (theme) => alpha(theme.palette.common.black, TIMELINE_CLIP_LABEL_OPACITY),
                    px: 0.5,
                    borderRadius: 0.5,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.25,
                  }}
                >
                  {playingIndex === index ? (
                    <FiberManualRecord sx={{ fontSize: 8, color: 'error.main' }} />
                  ) : (
                    <PlayArrow sx={{ fontSize: 14 }} />
                  )}
                  <Typography variant="caption" component="span">
                    #{index + 1}
                  </Typography>
                </Box>
              </Box>
              <Box sx={{ px: 0.5, py: 0.25, textAlign: 'center' }}>
                <Typography variant="caption" color="grey.400">
                  {dur}s
                </Typography>
              </Box>
            </Box>
          )
        })}
      </Box>
    </Paper>
  )
}
