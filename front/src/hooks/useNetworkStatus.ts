import { useState, useEffect, useCallback, useRef } from 'react'

export interface NetworkStatus {
  /** Browser reports online */
  online: boolean
  /** API server is reachable */
  serverReachable: boolean
  /** Combined: online && serverReachable */
  connected: boolean
  /** Timestamp of last successful ping */
  lastPingAt: number | null
  /** Number of queued operations waiting to be replayed */
  queuedOps: number
}

type QueuedOperation = {
  id: string
  execute: () => Promise<unknown>
  description: string
  timestamp: number
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
}

interface UseNetworkStatusOptions {
  /** Ping interval in ms (default: 15000) */
  pingInterval?: number
  /** Health check URL (default: '/api/v1/system/health/check') */
  healthUrl?: string
  /** Enable operation queuing (default: false) */
  enableQueue?: boolean
}

let opCounter = 0

export function useNetworkStatus(options?: UseNetworkStatusOptions) {
  const {
    pingInterval = 15_000,
    healthUrl = '/api/v1/system/health/check',
    enableQueue = false,
  } = options ?? {}

  const [online, setOnline] = useState(() => navigator.onLine)
  const [serverReachable, setServerReachable] = useState(true)
  const [lastPingAt, setLastPingAt] = useState<number | null>(null)
  const [queuedOps, setQueuedOps] = useState(0)

  const failCountRef = useRef(0)
  const queueRef = useRef<QueuedOperation[]>([])
  const replayingRef = useRef(false)
  const mountedRef = useRef(true)
  const prevConnectedRef = useRef(true)

  const connected = online && serverReachable

  // Ping the server
  const ping = useCallback(async (signal?: AbortSignal) => {
    if (!navigator.onLine) return

    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 5000)

      // Merge external signal with timeout
      const combinedSignal = signal
        ? AbortSignal.any?.([signal, controller.signal]) ?? controller.signal
        : controller.signal

      const token = localStorage.getItem('token')
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      }
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      const res = await fetch(healthUrl, {
        method: 'POST',
        signal: combinedSignal,
        headers,
        body: '{}',
      })

      clearTimeout(timeoutId)

      // Any response (even 401/403) means server is reachable
      if (res.status < 500) {
        failCountRef.current = 0
        if (mountedRef.current) {
          setServerReachable(true)
          setLastPingAt(Date.now())
        }
      } else {
        failCountRef.current++
        if (failCountRef.current >= 2 && mountedRef.current) {
          setServerReachable(false)
        }
      }
    } catch {
      failCountRef.current++
      if (failCountRef.current >= 2 && mountedRef.current) {
        setServerReachable(false)
      }
    }
  }, [healthUrl])

  // Replay queued operations sequentially
  const replayQueue = useCallback(async () => {
    if (replayingRef.current || queueRef.current.length === 0) return
    replayingRef.current = true

    while (queueRef.current.length > 0) {
      const op = queueRef.current[0]
      try {
        const result = await op.execute()
        op.resolve(result)
        queueRef.current.shift()
        if (mountedRef.current) {
          setQueuedOps(queueRef.current.length)
        }
      } catch (err) {
        // If we're offline again, stop replaying
        if (!navigator.onLine) break
        // Retry once, then reject and move on
        try {
          const result = await op.execute()
          op.resolve(result)
        } catch (retryErr) {
          op.reject(retryErr)
        }
        queueRef.current.shift()
        if (mountedRef.current) {
          setQueuedOps(queueRef.current.length)
        }
      }
    }

    replayingRef.current = false
  }, [])

  // Online/offline event listeners
  useEffect(() => {
    const handleOnline = () => {
      if (mountedRef.current) {
        setOnline(true)
        // Reset fail count and immediately ping
        failCountRef.current = 0
        setServerReachable(true)
        ping()
      }
    }

    const handleOffline = () => {
      if (mountedRef.current) {
        setOnline(false)
      }
    }

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [ping])

  // Periodic ping
  useEffect(() => {
    const abortController = new AbortController()

    // Initial ping
    ping(abortController.signal)

    const intervalId = setInterval(() => {
      ping(abortController.signal)
    }, pingInterval)

    return () => {
      clearInterval(intervalId)
      abortController.abort()
    }
  }, [ping, pingInterval])

  // Replay queue when reconnected
  useEffect(() => {
    if (connected && !prevConnectedRef.current && enableQueue) {
      replayQueue()
    }
    prevConnectedRef.current = connected
  }, [connected, enableQueue, replayQueue])

  // Cleanup on unmount
  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
    }
  }, [])

  const enqueueOperation = useCallback(
    (execute: () => Promise<unknown>, description: string): Promise<unknown> => {
      if (!enableQueue) {
        return execute()
      }

      // If connected, execute immediately
      if (navigator.onLine) {
        return execute()
      }

      return new Promise((resolve, reject) => {
        const op: QueuedOperation = {
          id: `op_${++opCounter}_${Date.now()}`,
          execute,
          description,
          timestamp: Date.now(),
          resolve,
          reject,
        }
        queueRef.current.push(op)
        if (mountedRef.current) {
          setQueuedOps(queueRef.current.length)
        }
      })
    },
    [enableQueue],
  )

  const clearQueue = useCallback(() => {
    for (const op of queueRef.current) {
      op.reject(new Error('Queue cleared'))
    }
    queueRef.current = []
    if (mountedRef.current) {
      setQueuedOps(0)
    }
  }, [])

  const status: NetworkStatus = {
    online,
    serverReachable,
    connected,
    lastPingAt,
    queuedOps,
  }

  return { status, enqueueOperation, clearQueue }
}
