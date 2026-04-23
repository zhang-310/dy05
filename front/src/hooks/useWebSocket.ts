/**
 * WebSocket 通用 Hook（预留）
 *
 * 后端实现 WebSocket 端点后，可替换轮询逻辑：
 * - 知识库导入进度：/ws/import/{jobId}
 * - 监控实时数据：/ws/monitoring
 * - 进化任务进度：/ws/evolve/{taskId}
 *
 * 使用示例（需安装 socket.io-client）：
 * ```ts
 * import { io } from 'socket.io-client'
 * const socket = io(WS_URL)
 * socket.emit('subscribe-import', { jobId })
 * socket.on(`import-progress-${jobId}`, (data) => setProgress(data))
 * socket.on('disconnect', () => { ... })
 * return () => socket.disconnect()
 * ```
 */
import { useEffect, useRef, useState } from 'react'

const WS_BASE = typeof window !== 'undefined' ? `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}` : ''

export interface UseWebSocketOptions {
  url?: string
  onMessage?: (data: unknown) => void
  onOpen?: () => void
  onClose?: () => void
  enabled?: boolean
}

/**
 * 原生 WebSocket Hook（轻量，不依赖 socket.io）
 * 后端需实现标准 WebSocket 端点
 */
export function useWebSocket({ url, onMessage, onOpen, onClose, enabled = true }: UseWebSocketOptions) {
  const [ready, setReady] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!enabled || !url) return

    const fullUrl = url.startsWith('ws') ? url : `${WS_BASE.replace(/\/$/, '')}${url.startsWith('/') ? '' : '/'}${url}`
    const ws = new WebSocket(fullUrl)

    ws.onopen = () => {
      setReady(true)
      onOpen?.()
    }
    ws.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data)
        onMessage?.(data)
      } catch {
        onMessage?.(e.data)
      }
    }
    ws.onclose = () => {
      setReady(false)
      onClose?.()
    }

    wsRef.current = ws
    return () => {
      ws.close()
      wsRef.current = null
    }
  }, [url, enabled, onMessage, onOpen, onClose])

  const send = (data: unknown) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }

  return { ready, send }
}
