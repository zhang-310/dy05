import { getToken } from '@/utils/auth'

export interface SSECallbacks<T = unknown, D = unknown> {
  onProgress?: (data: T) => void
  onDone?: (data: D) => void
  onError?: (err: Error) => void
  onSlotDone?: (data: { scriptId: number; content: string; scriptType: string; slotLabel: string; sequenceNo?: number; index?: number }) => void
  onSlotFailed?: (data: { scriptId: number; slotLabel: string; errorMsg: string; index?: number }) => void
  onChunk?: (content: string) => void
  onStatus?: (status: string) => void
  onReconnecting?: (attempt: number) => void
  onSkillStart?: (data: { tool: string; description: string }) => void
  onSkillEnd?: (data: { tool: string; status: string; error?: string }) => void
}

export interface SSEOptions {
  batchUpdates?: boolean
  backpressureThreshold?: number
  maxReconnectAttempts?: number
  idleTimeoutMs?: number
}

const DEFAULT_BACKPRESSURE = 50
const MAX_RECONNECT_DELAY = 30000

function getReconnectDelay(attempt: number): number {
  return Math.min(1000 * Math.pow(2, attempt), MAX_RECONNECT_DELAY)
}

export function ssePost<T = unknown, D = unknown>(
  url: string,
  body: Record<string, unknown>,
  callbacks: SSECallbacks<T, D>,
  options: SSEOptions = {},
): AbortController {
  const controller = new AbortController()
  const token = getToken() || ''
  const {
    batchUpdates = false,
    backpressureThreshold = DEFAULT_BACKPRESSURE,
    maxReconnectAttempts = 3,
    idleTimeoutMs = 300000,
  } = options

  let pendingProgressBatch: T[] = []
  let rafId: number | null = null
  let lastEventId: string | null = null
  let idleTimer: ReturnType<typeof setTimeout> | null = null

  function resetIdleTimer() {
    if (idleTimer) clearTimeout(idleTimer)
    if (idleTimeoutMs > 0) {
      idleTimer = setTimeout(() => {
        if (!controller.signal.aborted) {
          controller.abort()
          callbacks.onError?.(new Error(`SSE 连接空闲超时 (${idleTimeoutMs}ms)`))
        }
      }, idleTimeoutMs)
    }
  }

  function flushBatch() {
    rafId = null
    const batch = pendingProgressBatch
    pendingProgressBatch = []
    if (batch.length > 0) callbacks.onProgress?.(batch[batch.length - 1])
  }

  async function doFetch(attempt = 0): Promise<void> {
    try {
      resetIdleTimer()
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      }
      if (token) headers['Authorization'] = `Bearer ${token}`
      if (lastEventId) headers['Last-Event-ID'] = lastEventId

      const resp = await fetch(`/api/v1${url}`, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
        signal: controller.signal,
      })

      if (!resp.ok || !resp.body) {
        throw new Error(`HTTP ${resp.status}: ${resp.statusText}`)
      }

      const reader = resp.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let pendingCount = 0

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        resetIdleTimer()
        buffer += decoder.decode(value, { stream: true })

        const events = buffer.split('\n\n')
        buffer = events.pop() ?? ''

        for (const raw of events) {
          if (!raw.trim()) continue
          const lines = raw.split('\n')
          let eventType = 'message'
          let dataStr = ''
          for (const line of lines) {
            if (line.startsWith('event:')) eventType = line.slice(6).trim()
            else if (line.startsWith('data:')) dataStr = line.slice(5).trim()
            else if (line.startsWith('id:')) lastEventId = line.slice(3).trim()
          }
          if (!dataStr || dataStr === '[DONE]') continue
          let parsed: unknown
          try { parsed = JSON.parse(dataStr) } catch { parsed = dataStr }

          switch (eventType) {
            case 'progress': {
              if (batchUpdates) {
                pendingProgressBatch.push(parsed as T)
                if (!rafId) rafId = requestAnimationFrame(flushBatch)
              } else {
                callbacks.onProgress?.(parsed as T)
              }
              break
            }
            case 'done':
              callbacks.onDone?.(parsed as D)
              if (idleTimer) clearTimeout(idleTimer)
              return
            case 'error': {
              const errObj = typeof parsed === 'object' && parsed !== null ? parsed as Record<string, unknown> : null
              throw new Error(errObj
                ? (errObj.error as string ?? errObj.message as string ?? JSON.stringify(parsed))
                : String(parsed)
              )
            }
            case 'slot_done':
              callbacks.onSlotDone?.(parsed as Parameters<NonNullable<SSECallbacks['onSlotDone']>>[0])
              break
            case 'slot_failed':
              callbacks.onSlotFailed?.(parsed as Parameters<NonNullable<SSECallbacks['onSlotFailed']>>[0])
              break
            case 'chunk':
              callbacks.onChunk?.(typeof parsed === 'string' ? parsed : JSON.stringify(parsed))
              break
            case 'status':
              callbacks.onStatus?.(typeof parsed === 'string' ? parsed : JSON.stringify(parsed))
              break
            case 'skill_start': {
              const ssData = typeof parsed === 'object' && parsed !== null ? parsed as Record<string, unknown> : null
              if (ssData) {
                callbacks.onSkillStart?.({
                  tool: String(ssData.tool ?? ''),
                  description: String(ssData.description ?? ''),
                })
              }
              break
            }
            case 'skill_end': {
              const seData = typeof parsed === 'object' && parsed !== null ? parsed as Record<string, unknown> : null
              if (seData) {
                callbacks.onSkillEnd?.({
                  tool: String(seData.tool ?? ''),
                  status: String(seData.status ?? 'success'),
                  error: seData.error != null ? String(seData.error) : undefined,
                })
              }
              break
            }
          }

          pendingCount++
          if (pendingCount > backpressureThreshold) {
            pendingCount = 0
            await new Promise(r => setTimeout(r, 0))
          }
        }
      }
      if (idleTimer) clearTimeout(idleTimer)
    } catch (e: unknown) {
      if (idleTimer) clearTimeout(idleTimer)
      if (controller.signal.aborted) return
      if (attempt < maxReconnectAttempts) {
        const delay = getReconnectDelay(attempt)
        callbacks.onReconnecting?.(attempt + 1)
        await new Promise(r => setTimeout(r, delay))
        if (!controller.signal.aborted) return doFetch(attempt + 1)
      }
      callbacks.onError?.(e instanceof Error ? e : new Error(String(e)))
    }
  }

  doFetch()
  return controller
}

export async function fetchExport(url: string, params?: Record<string, unknown>): Promise<Blob> {
  const token = getToken()
  const res = await fetch(`/api/v1${url}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: params ? JSON.stringify(params) : undefined,
  })
  if (!res.ok) throw new Error(`导出失败: ${res.statusText}`)
  return res.blob()
}
