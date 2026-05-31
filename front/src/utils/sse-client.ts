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

interface ParsedSseFrame {
  eventType: string
  dataStr: string
  hasData: boolean
}

function getReconnectDelay(attempt: number): number {
  return Math.min(1000 * Math.pow(2, attempt), MAX_RECONNECT_DELAY)
}

function sseFieldValue(line: string, prefixLength: number): string {
  const value = line.slice(prefixLength)
  return value.startsWith(' ') ? value.slice(1) : value
}

function extractSseFrames(buffer: string): { frames: string[]; rest: string } {
  const frames: string[] = []
  let rest = buffer

  while (rest.length > 0) {
    const delimiters = [
      { index: rest.indexOf('\r\n\r\n'), length: 4 },
      { index: rest.indexOf('\n\n'), length: 2 },
      { index: rest.indexOf('\r\r'), length: 2 },
    ].filter((item) => item.index >= 0)

    if (delimiters.length === 0) break
    const next = delimiters.reduce((best, item) => (item.index < best.index ? item : best), delimiters[0])
    frames.push(rest.slice(0, next.index))
    rest = rest.slice(next.index + next.length)
  }

  return { frames, rest }
}

function parseSseFrame(raw: string): ParsedSseFrame {
  const lines = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n')
  let eventType = 'message'
  let hasData = false
  const dataLines: string[] = []
  for (const line of lines) {
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      eventType = sseFieldValue(line, 6).trim() || 'message'
    } else if (line.startsWith('data:')) {
      hasData = true
      dataLines.push(sseFieldValue(line, 5))
    }
  }
  return { eventType, dataStr: dataLines.join('\n'), hasData }
}

function statusToText(data: unknown): string {
  if (typeof data === 'string') return data
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const row = data as Record<string, unknown>
    const value = row.status ?? row.message ?? row.content ?? row.text
    if (value !== undefined && value !== null) return String(value)
  }
  return data === undefined || data === null ? '' : JSON.stringify(data)
}

function chunkToText(data: unknown): string {
  if (typeof data === 'string') return data
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const row = data as Record<string, unknown>
    const value = row.content ?? row.delta ?? row.message ?? row.text ?? row.token
    if (value !== undefined && value !== null) return String(value)
  }
  return data === undefined || data === null ? '' : JSON.stringify(data)
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
  let completed = false

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

      let reading = true
      while (reading) {
        const { done, value } = await reader.read()
        if (done) {
          reading = false
          continue
        }
        resetIdleTimer()
        buffer += decoder.decode(value, { stream: true })

        const { frames, rest } = extractSseFrames(buffer)
        buffer = rest

        for (const raw of frames) {
          if (!raw.trim()) continue
          const lines = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n')
          for (const line of lines) {
            if (line.startsWith('id:')) lastEventId = sseFieldValue(line, 3).trim()
          }
          const { eventType, dataStr, hasData } = parseSseFrame(raw)
          if (dataStr === '[DONE]') {
            if (!completed) {
              completed = true
              callbacks.onDone?.({ type: 'done' } as D)
            }
            if (idleTimer) clearTimeout(idleTimer)
            continue
          }
          if (!hasData && eventType !== 'done') continue
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
              if (!completed) {
                completed = true
                callbacks.onDone?.(parsed as D)
              }
              if (idleTimer) clearTimeout(idleTimer)
              break
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
              callbacks.onChunk?.(chunkToText(parsed))
              break
            case 'status':
              callbacks.onStatus?.(statusToText(parsed))
              break
            case 'message':
              callbacks.onChunk?.(chunkToText(parsed))
              break
            case 'skill_start':
            case 'tool_start': {
              const ssData = typeof parsed === 'object' && parsed !== null ? parsed as Record<string, unknown> : null
              if (ssData) {
                callbacks.onSkillStart?.({
                  tool: String(ssData.tool ?? ''),
                  description: String(ssData.description ?? ''),
                })
              }
              break
            }
            case 'skill_end':
            case 'tool_end': {
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
      if (!completed && !controller.signal.aborted) {
        callbacks.onError?.(new Error('SSE 连接已中断，未收到完成事件'))
      }
    } catch (e: unknown) {
      if (idleTimer) clearTimeout(idleTimer)
      if (completed) return
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
