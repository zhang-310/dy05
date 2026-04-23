import { useState, useEffect, useRef } from 'react'
import { Typography } from '@mui/material'

interface CountUpProps {
  end: number
  start?: number
  duration?: number
  separator?: string
  suffix?: string
  prefix?: string
  decimals?: number
  variant?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'body1' | 'body2' | 'caption'
}

/** 数字滚动动效 */
export function CountUp({
  end,
  start = 0,
  duration = 1500,
  separator = '',
  suffix = '',
  prefix = '',
  decimals = 0,
  variant = 'body1',
}: CountUpProps) {
  const [value, setValue] = useState(start)
  const startTimeRef = useRef<number | null>(null)
  const rafRef = useRef<number>()

  useEffect(() => {
    startTimeRef.current = null
    const animate = (timestamp: number) => {
      if (startTimeRef.current == null) startTimeRef.current = timestamp
      const elapsed = timestamp - startTimeRef.current
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - (1 - progress) ** 2
      setValue(start + (end - start) * eased)
      if (progress < 1) {
        rafRef.current = requestAnimationFrame(animate)
      }
    }
    rafRef.current = requestAnimationFrame(animate)
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
    }
  }, [end, start, duration])

  const formatted = value.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, separator)
  return (
    <Typography variant={variant} component="span">
      {prefix}{formatted}{suffix}
    </Typography>
  )
}
