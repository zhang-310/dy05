import { describe, it, expect, vi, beforeEach } from 'vitest'
import { subscribeEvolveProgressPost } from '../evolveProgressSse'

function createErrorResponse(status = 500): Response {
  return new Response(null, { status })
}

function createSseResponse(chunks: string[]): Response {
  const encoder = new TextEncoder()
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)))
      controller.close()
    },
  })

  return new Response(stream, {
    status: 200,
    headers: { 'Content-Type': 'text/event-stream' },
  })
}

describe('evolveProgressSse', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('subscribeEvolveProgressPost', () => {
    it('returns close function', () => {
      const mockFetch = vi.fn(() => Promise.reject(new Error('network')))
      vi.stubGlobal('fetch', mockFetch)

      const result = subscribeEvolveProgressPost(
        'job-123',
        { onProgress: vi.fn() },
        'token'
      )

      expect(result).toHaveProperty('close')
      expect(typeof result.close).toBe('function')
    })

    it('sends POST request with jobId and token', async () => {
      const mockFetch = vi.fn(() => Promise.resolve(createErrorResponse()))
      vi.stubGlobal('fetch', mockFetch)

      subscribeEvolveProgressPost(
        'job-456',
        { onProgress: vi.fn(), onError: vi.fn() },
        'my-token'
      )

      await vi.waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/ai/admin/evolve/task/progress-sse',
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'application/json',
              Accept: 'text/event-stream',
              Authorization: 'Bearer my-token',
            }),
            body: JSON.stringify({ jobId: 'job-456' }),
          })
        )
      })
    })

    it('calls onError when response is not ok', async () => {
      const mockFetch = vi.fn(() => Promise.resolve(createErrorResponse()))
      vi.stubGlobal('fetch', mockFetch)

      const onError = vi.fn()
      subscribeEvolveProgressPost('job-123', { onProgress: vi.fn(), onError }, 'token')

      await vi.waitFor(() => {
        expect(onError).toHaveBeenCalled()
      })
    })

    it('calls onProgress when receiving progress event', async () => {
      const mockFetch = vi.fn(() =>
        Promise.resolve(
          createSseResponse([
            'event: progress\ndata: {"step":"processing","message":"test"}\n\n',
          ])
        )
      )
      vi.stubGlobal('fetch', mockFetch)

      const onProgress = vi.fn()
      subscribeEvolveProgressPost('job-123', { onProgress }, 'token')

      await vi.waitFor(() => {
        expect(onProgress).toHaveBeenCalledWith({
          step: 'processing',
          message: 'test',
        })
      })
    })

    it('calls onDone and aborts when step is completed', async () => {
      const mockFetch = vi.fn(() =>
        Promise.resolve(createSseResponse(['event: progress\ndata: {"step":"completed"}\n\n']))
      )
      vi.stubGlobal('fetch', mockFetch)

      const onDone = vi.fn()
      const onProgress = vi.fn()
      subscribeEvolveProgressPost('job-123', { onProgress, onDone }, 'token')

      await vi.waitFor(() => {
        expect(onProgress).toHaveBeenCalledWith({ step: 'completed' })
        expect(onDone).toHaveBeenCalled()
      })
    })

    it('close function aborts the request', () => {
      const mockFetch = vi.fn(() => new Promise(() => {}))
      vi.stubGlobal('fetch', mockFetch)

      const subscription = subscribeEvolveProgressPost(
        'job-123',
        { onProgress: vi.fn() },
        'token'
      )

      subscription.close()

      // Verify abort was called by checking the signal
      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          signal: expect.any(AbortSignal),
        })
      )

      const [, requestInit] = mockFetch.mock.calls[0]
      expect(requestInit?.signal).toBeDefined()
      expect(requestInit?.signal?.aborted).toBe(true)
    })
  })
})
