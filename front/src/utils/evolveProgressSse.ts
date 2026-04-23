/**
 * 进化任务进度：POST SSE + Bearer（避免 GET ?token= 泄露）。
 * EventSource 无法带 Authorization，故使用 fetch 流式解析。
 */
const PROGRESS_PATH = '/api/v1/ai/admin/evolve/task/progress-sse'

export interface EvolveProgressPayload {
  step?: string
  message?: string
  taskId?: number
}

export function subscribeEvolveProgressPost(
  jobId: string,
  callbacks: {
    onProgress: (data: EvolveProgressPayload) => void
    onDone?: () => void
    onError?: () => void
  },
  token: string | null,
): { close: () => void } {
  const ac = new AbortController()

  void (async () => {
    try {
      const res = await fetch(PROGRESS_PATH, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ jobId }),
        signal: ac.signal,
      })
      if (!res.ok || !res.body) {
        callbacks.onError?.()
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventName = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const raw = line.slice(5).trim()
            if (eventName === 'progress' && raw) {
              try {
                const data = JSON.parse(raw) as EvolveProgressPayload
                callbacks.onProgress(data)
                if (
                  data.step === 'completed' ||
                  data.step === 'failed' ||
                  data.step === 'skipped'
                ) {
                  callbacks.onDone?.()
                  ac.abort()
                  return
                }
              } catch {
                /* ignore malformed chunk */
              }
            }
          }
        }
      }
    } catch {
      if (!ac.signal.aborted) callbacks.onError?.()
    }
  })()

  return { close: () => ac.abort() }
}
