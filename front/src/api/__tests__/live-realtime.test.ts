import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { subscribeWithRetry } from '../live-realtime'

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'token'),
}))

describe('live realtime SSE retry diagnostics', () => {
  const originalEventSource = globalThis.EventSource

  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    if (originalEventSource) {
      vi.stubGlobal('EventSource', originalEventSource)
    } else {
      vi.unstubAllGlobals()
    }
  })

  it('reports retry schedule and exhaustion through callbacks instead of console logs', async () => {
    const scheduled = vi.fn()
    const exhausted = vi.fn()
    const errors = vi.fn()
    const consoleLog = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    const instances: Array<{ close: () => void; onerror: (() => void) | null }> = []

    class FakeEventSource {
      onerror: (() => void) | null = null

      constructor(public readonly url: string) {
        instances.push(this)
      }

      addEventListener() {}

      close = vi.fn()
    }

    vi.stubGlobal('EventSource', FakeEventSource)

    subscribeWithRetry(18, {
      onError: errors,
      onRetryScheduled: scheduled,
      onRetryExhausted: exhausted,
    }, 1, 1500)

    expect(instances).toHaveLength(1)
    instances[0].onerror?.()

    expect(errors).toHaveBeenCalledTimes(1)
    expect(scheduled).toHaveBeenCalledWith(expect.objectContaining({
      retryCount: 1,
      maxRetries: 1,
      retryDelayMs: 1500,
      error: expect.any(Error),
    }))
    expect(consoleLog).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1500)
    expect(instances).toHaveLength(2)
    instances[1].onerror?.()

    expect(exhausted).toHaveBeenCalledWith(expect.objectContaining({
      retryCount: 1,
      maxRetries: 1,
      error: expect.any(Error),
    }))
    expect(consoleError).not.toHaveBeenCalledWith('SSE 连接失败，已达到最大重试次数')
  })
})
