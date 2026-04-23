import { useState, useEffect, useRef } from 'react'
import { Typography, Box, IconButton } from '@mui/material'
import { PlayArrow, Pause } from '@mui/icons-material'

interface StreamingTextProps {
  text: string
  speed?: number
  onComplete?: () => void
  paused?: boolean
  onPausedChange?: (paused: boolean) => void
}

/** 流式文本展示：逐字显示，模拟 AI 流式输出 */
export function StreamingText({
  text,
  speed = 50,
  onComplete,
  paused: controlledPaused,
  onPausedChange,
}: StreamingTextProps) {
  const [displayedLength, setDisplayedLength] = useState(0)
  const [internalPaused, setInternalPaused] = useState(false)
  const paused = controlledPaused ?? internalPaused
  const setPaused = onPausedChange ?? setInternalPaused
  const completedRef = useRef(false)

  useEffect(() => {
    setDisplayedLength(0)
    completedRef.current = false
  }, [text])

  useEffect(() => {
    if (paused || displayedLength >= text.length) {
      if (displayedLength >= text.length && !completedRef.current) {
        completedRef.current = true
        onComplete?.()
      }
      return
    }
    const t = setTimeout(() => {
      setDisplayedLength((n) => Math.min(n + 1, text.length))
    }, speed)
    return () => clearTimeout(t)
  }, [text, displayedLength, paused, speed, onComplete])

  return (
    <Box>
      <Typography component="span" sx={{ whiteSpace: 'pre-wrap' }}>
        {text.slice(0, displayedLength)}
      </Typography>
      {displayedLength < text.length && (
        <Typography component="span" sx={{ opacity: 0.8, animation: 'pulse 1s ease-in-out infinite' }}>
          |
        </Typography>
      )}
      <IconButton size="small" onClick={() => setPaused(!paused)} sx={{ ml: 0.5, verticalAlign: 'middle' }}>
        {paused ? <PlayArrow fontSize="small" /> : <Pause fontSize="small" />}
      </IconButton>
    </Box>
  )
}
