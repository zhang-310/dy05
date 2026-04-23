import { useRef, useState, useCallback, useEffect } from 'react'
import { Box, IconButton, Slider, Typography, Tooltip } from '@mui/material'
import {
  PlayArrow,
  Pause,
  VolumeUp,
  VolumeOff,
  Fullscreen,
  FullscreenExit,
  SkipNext,
  Speed as SpeedIcon,
} from '@mui/icons-material'

const SPEED_OPTIONS = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2]

interface VideoPlayerProps {
  src: string
  poster?: string
  controls?: boolean
  autoplay?: boolean
  loop?: boolean
  muted?: boolean
  onPlay?: () => void
  onPause?: () => void
  onEnded?: () => void
  onTimeUpdate?: (currentTime: number) => void
  sx?: object
}

/** 视频播放器：倍速、全屏、逐帧、音量 */
export function VideoPlayer({
  src,
  poster,
  controls = true,
  autoplay = false,
  loop = false,
  muted: initialMuted = false,
  onPlay,
  onPause,
  onEnded,
  onTimeUpdate,
  sx = {},
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const [playing, setPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(1)
  const [muted, setMuted] = useState(initialMuted)
  const [playbackRate, setPlaybackRate] = useState(1)
  const [fullscreen, setFullscreen] = useState(false)
  const [speedMenuOpen, setSpeedMenuOpen] = useState(false)

  const video = videoRef.current

  const togglePlay = useCallback(() => {
    if (!video) return
    if (video.paused) {
      video.play()
      setPlaying(true)
      onPlay?.()
    } else {
      video.pause()
      setPlaying(false)
      onPause?.()
    }
  }, [video, onPlay, onPause])

  const handleTimeUpdate = useCallback(() => {
    if (video) {
      setCurrentTime(video.currentTime)
      onTimeUpdate?.(video.currentTime)
    }
  }, [video, onTimeUpdate])

  const handleLoadedMetadata = useCallback(() => {
    if (video) setDuration(video.duration)
  }, [video])

  const handleSeek = useCallback(
    (_: unknown, value: number | number[]) => {
      const v = typeof value === 'number' ? value : value[0]
      if (video) {
        video.currentTime = v
        setCurrentTime(v)
      }
    },
    [video]
  )

  const handleVolumeChange = useCallback(
    (_: unknown, value: number | number[]) => {
      const v = typeof value === 'number' ? value : value[0]
      setVolume(v)
      if (video) {
        video.volume = v
        video.muted = v === 0
        setMuted(v === 0)
      }
    },
    [video]
  )

  const toggleMute = useCallback(() => {
    if (video) {
      video.muted = !video.muted
      setMuted(video.muted)
      if (!video.muted) setVolume(video.volume)
    }
  }, [video])

  const stepFrame = useCallback(
    (forward: boolean) => {
      if (!video) return
      const fps = 30
      const step = 1 / fps
      video.pause()
      setPlaying(false)
      video.currentTime = Math.min(
        Math.max(0, video.currentTime + (forward ? step : -step)),
        video.duration
      )
      setCurrentTime(video.currentTime)
    },
    [video]
  )

  const toggleFullscreen = useCallback(() => {
    const el = containerRef.current
    if (!el) return
    if (!document.fullscreenElement) {
      el.requestFullscreen?.()
      setFullscreen(true)
    } else {
      document.exitFullscreen?.()
      setFullscreen(false)
    }
  }, [])

  const setSpeed = useCallback(
    (rate: number) => {
      if (video) {
        video.playbackRate = rate
        setPlaybackRate(rate)
      }
      setSpeedMenuOpen(false)
    },
    [video]
  )

  useEffect(() => {
    const v = videoRef.current
    if (!v) return
    const onEnd = () => {
      setPlaying(false)
      onEnded?.()
    }
    v.addEventListener('ended', onEnd)
    return () => v.removeEventListener('ended', onEnd)
  }, [onEnded])

  useEffect(() => {
    const handler = () => setFullscreen(!!document.fullscreenElement)
    document.addEventListener('fullscreenchange', handler)
    return () => document.removeEventListener('fullscreenchange', handler)
  }, [])

  const formatTime = (t: number) => {
    const m = Math.floor(t / 60)
    const s = Math.floor(t % 60)
    return `${m}:${s.toString().padStart(2, '0')}`
  }

  return (
    <Box ref={containerRef} sx={{ position: 'relative', bgcolor: 'black', borderRadius: 1, overflow: 'hidden', ...sx }}>
      <video
        ref={videoRef}
        src={src}
        poster={poster}
        autoPlay={autoplay}
        loop={loop}
        muted={muted}
        playsInline
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
        style={{ width: '100%', display: 'block', maxHeight: '70vh' }}
      />
      {controls && (
        <Box
          sx={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            background: 'linear-gradient(transparent, rgba(0,0,0,0.8))',
            p: 1,
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: 0.5,
          }}
        >
          <IconButton size="small" onClick={togglePlay} sx={{ color: 'white' }}>
            {playing ? <Pause fontSize="small" /> : <PlayArrow fontSize="small" />}
          </IconButton>
          <Typography variant="caption" sx={{ color: 'white', minWidth: 80 }}>
            {formatTime(currentTime)} / {formatTime(duration)}
          </Typography>
          <Slider
            size="small"
            value={currentTime}
            min={0}
            max={duration || 100}
            onChange={handleSeek}
            sx={{ flex: 1, color: 'white', maxWidth: 120 }}
          />
          <Tooltip title="上一帧">
            <IconButton size="small" onClick={() => stepFrame(false)} sx={{ color: 'white' }}>
              <SkipNext sx={{ transform: 'scaleX(-1)' }} fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="下一帧">
            <IconButton size="small" onClick={() => stepFrame(true)} sx={{ color: 'white' }}>
              <SkipNext fontSize="small" />
            </IconButton>
          </Tooltip>
          <Box sx={{ display: 'flex', alignItems: 'center', position: 'relative' }}>
            <IconButton size="small" onClick={toggleMute} sx={{ color: 'white' }}>
              {muted || volume === 0 ? <VolumeOff fontSize="small" /> : <VolumeUp fontSize="small" />}
            </IconButton>
            <Slider
              size="small"
              value={muted ? 0 : volume}
              min={0}
              max={1}
              step={0.1}
              onChange={handleVolumeChange}
              sx={{ width: 60, color: 'white' }}
            />
          </Box>
          <Box sx={{ position: 'relative' }}>
            <Tooltip title={`倍速 ${playbackRate}x`}>
              <IconButton
                size="small"
                onClick={() => setSpeedMenuOpen((o) => !o)}
                sx={{ color: 'white' }}
              >
                <SpeedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {speedMenuOpen && (
              <Box
                sx={{
                  position: 'absolute',
                  bottom: '100%',
                  right: 0,
                  mb: 0.5,
                  bgcolor: 'grey.900',
                  borderRadius: 1,
                  p: 0.5,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 0.25,
                }}
              >
                {SPEED_OPTIONS.map((r) => (
                  <Typography
                    key={r}
                    variant="caption"
                    onClick={() => setSpeed(r)}
                    sx={{
                      cursor: 'pointer',
                      color: playbackRate === r ? 'primary.main' : 'grey.300',
                      '&:hover': { color: 'white' },
                    }}
                  >
                    {r}x
                  </Typography>
                ))}
              </Box>
            )}
          </Box>
          <IconButton size="small" onClick={toggleFullscreen} sx={{ color: 'white' }}>
            {fullscreen ? <FullscreenExit fontSize="small" /> : <Fullscreen fontSize="small" />}
          </IconButton>
        </Box>
      )}
    </Box>
  )
}
