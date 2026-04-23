import { useState, useEffect, useRef } from 'react'
import { Alert, Collapse, Box, Chip } from '@mui/material'
import WifiOffIcon from '@mui/icons-material/WifiOff'
import CloudOffIcon from '@mui/icons-material/CloudOff'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import type { NetworkStatus } from '@/hooks/useNetworkStatus'

interface OfflineBannerProps {
  status: NetworkStatus
}

export function OfflineBanner({ status }: OfflineBannerProps) {
  const [showRecovered, setShowRecovered] = useState(false)
  const wasDisconnectedRef = useRef(false)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (!status.connected) {
      wasDisconnectedRef.current = true
      // Clear any pending recovery timer
      if (timerRef.current) {
        clearTimeout(timerRef.current)
        timerRef.current = null
      }
      setShowRecovered(false)
    } else if (wasDisconnectedRef.current) {
      // Just reconnected
      wasDisconnectedRef.current = false
      setShowRecovered(true)
      timerRef.current = setTimeout(() => {
        setShowRecovered(false)
        timerRef.current = null
      }, 3000)
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
      }
    }
  }, [status.connected])

  const showBanner = !status.connected || showRecovered

  let severity: 'warning' | 'success' = 'warning'
  let icon = <WifiOffIcon fontSize="small" />
  let message = ''

  if (!status.online) {
    message = '网络已断开，请检查网络连接'
    icon = <WifiOffIcon fontSize="small" />
  } else if (!status.serverReachable) {
    message = '服务器连接中断，正在尝试重连...'
    icon = <CloudOffIcon fontSize="small" />
  } else if (showRecovered) {
    severity = 'success'
    icon = <CheckCircleIcon fontSize="small" />
    message = '网络已恢复'
  }

  return (
    <Collapse in={showBanner}>
      <Box sx={{ position: 'relative', zIndex: 1100, flexShrink: 0 }}>
        <Alert
          severity={severity}
          icon={icon}
          sx={{
            borderRadius: 0,
            py: 0.25,
            '& .MuiAlert-message': {
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              py: 0,
            },
          }}
        >
          {message}
          {status.queuedOps > 0 && !status.connected && (
            <Chip
              label={`${status.queuedOps} 个操作等待同步`}
              size="small"
              color="warning"
              variant="outlined"
              sx={{ ml: 1, height: 22, fontSize: '0.75rem' }}
            />
          )}
        </Alert>
      </Box>
    </Collapse>
  )
}
