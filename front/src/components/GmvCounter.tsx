import { useEffect, useState, useRef } from 'react'
import { Box, Typography } from '@mui/material'

interface GmvCounterProps {
  value: number
  duration?: number
  fontSize?: number
}

export default function GmvCounter({ value, duration = 1000, fontSize = 48 }: GmvCounterProps) {
  const [displayValue, setDisplayValue] = useState(0)
  const [flyingDigits, setFlyingDigits] = useState<{ id: number; digit: string; delay: number }[]>([])
  const prevValueRef = useRef(0)

  useEffect(() => {
    const start = prevValueRef.current
    const end = value
    const diff = end - start

    if (diff === 0) return

    // CountUp 动画
    const startTime = Date.now()
    const animate = () => {
      const elapsed = Date.now() - startTime
      const progress = Math.min(elapsed / duration, 1)
      const current = start + diff * progress
      setDisplayValue(Math.floor(current))

      if (progress < 1) {
        requestAnimationFrame(animate)
      } else {
        prevValueRef.current = end
      }
    }
    animate()

    // 飞字效果
    if (diff > 0) {
      const digits = String(Math.abs(diff)).split('')
      const flying = digits.map((d, i) => ({
        id: Date.now() + i,
        digit: d,
        delay: i * 50,
      }))
      setFlyingDigits(flying)
      setTimeout(() => setFlyingDigits([]), duration + 500)
    }
  }, [value, duration])

  const formatted = displayValue.toLocaleString('zh-CN')
  const color = displayValue >= 10000000 ? '#4caf50' : displayValue >= 5000000 ? '#ff9800' : '#666'

  return (
    <Box sx={{ position: 'relative', display: 'inline-block' }}>
      <Typography
        sx={{
          fontSize,
          fontWeight: 700,
          fontFamily: 'monospace',
          color,
          letterSpacing: 2,
        }}
      >
        ¥{formatted}
      </Typography>
      {flyingDigits.map(({ id, digit, delay }) => (
        <Box
          key={id}
          sx={{
            position: 'absolute',
            top: 0,
            right: 0,
            fontSize: fontSize * 0.6,
            fontWeight: 700,
            color: '#4caf50',
            animation: 'flyUp 0.8s ease-out forwards',
            animationDelay: `${delay}ms`,
            '@keyframes flyUp': {
              '0%': { opacity: 1, transform: 'translateY(0)' },
              '100%': { opacity: 0, transform: 'translateY(-40px)' },
            },
          }}
        >
          +{digit}
        </Box>
      ))}
    </Box>
  )
}
