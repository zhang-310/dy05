/**
 * sse-client 工具测试（mock fetch）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ssePost, fetchExport } from '../sse-client'

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'mock-token'),
}))

describe('sse-client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('ssePost', () => {
    it('returns AbortController', () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.reject(new Error('network'))))
      const cbs = { onError: vi.fn() }
      const ctrl = ssePost('/live/generate', { sessionId: 1 }, cbs)
      expect(ctrl).toBeInstanceOf(AbortController)
    })

    it('abort stops request', async () => {
      const fetchFn = vi.fn(() => new Promise(() => {}))
      vi.stubGlobal('fetch', fetchFn)
      const ctrl = ssePost('/live/generate', {}, { onError: vi.fn() })
      ctrl.abort()
      await vi.waitFor(() => {
        expect(fetchFn).toHaveBeenCalled()
      }, { timeout: 100 })
    })

    it('passes options to fetch', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response('', { status: 401 }))))
      ssePost('/test', { a: 1 }, { onError: vi.fn() })
      await vi.waitFor(() => {
        expect(fetch).toHaveBeenCalledWith(
          expect.stringContaining('/api/v1/test'),
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'application/json',
              'Accept': 'text/event-stream',
              'Authorization': 'Bearer mock-token',
            }),
            body: JSON.stringify({ a: 1 }),
          })
        )
      }, { timeout: 500 })
    })
  })

  describe('fetchExport', () => {
    it('returns blob on success', async () => {
      const content = 'xlsx-content'
      const blob = new Blob([content], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const mockResponse = {
        ok: true,
        status: 200,
        blob: vi.fn().mockResolvedValue(blob),
      }
      vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(mockResponse)))

      const result = await fetchExport('/export/session', { id: 1 })
      expect(result).toBeInstanceOf(Blob)
      expect(result.type).toBe('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
    })

    it('throws on non-ok response', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response('err', { status: 500 }))))
      await expect(fetchExport('/export/session')).rejects.toThrow('导出失败')
    })
  })
})
