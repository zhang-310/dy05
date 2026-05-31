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

    it('reports an error when stream closes before done event', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response('event:status\ndata:{"status":"thinking"}\n\n', {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }))))
      const onError = vi.fn()

      ssePost('/test', {}, { onError })

      await vi.waitFor(() => {
        expect(onError).toHaveBeenCalledWith(expect.objectContaining({
          message: 'SSE 连接已中断，未收到完成事件',
        }))
      }, { timeout: 500 })
    })

    it('does not report an error when stream closes after done event', async () => {
      const encoder = new TextEncoder()
      const read = vi.fn()
        .mockResolvedValueOnce({
          done: false,
          value: encoder.encode('event:done\ndata:{"type":"done","content":"OK"}\n\n'),
        })
        .mockRejectedValueOnce(new TypeError('terminated'))

      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve({
          ok: true,
          status: 200,
          body: { getReader: () => ({ read }) },
        })))

      const onDone = vi.fn()
      const onError = vi.fn()

      ssePost('/test', {}, { onDone, onError })

      await vi.waitFor(() => {
        expect(onDone).toHaveBeenCalledWith({ type: 'done', content: 'OK' })
      }, { timeout: 500 })
      await new Promise(resolve => setTimeout(resolve, 0))

      expect(onError).not.toHaveBeenCalled()
    })

    it('normalizes status event objects to readable text', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response([
          'event:status\ndata:{"status":"思考中..."}',
          '',
          'event:status\ndata:{"status":"无需调用工具","type":"status"}',
          '',
          'event:done\ndata:{"type":"done","content":"OK"}',
          '',
        ].join('\n'), {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }))))
      const onStatus = vi.fn()

      ssePost('/test', {}, { onStatus })

      await vi.waitFor(() => {
        expect(onStatus).toHaveBeenCalledWith('思考中...')
        expect(onStatus).toHaveBeenCalledWith('无需调用工具')
      }, { timeout: 500 })
    })

    it('parses CRLF frames, multiline data, default message chunks, and DONE sentinels', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response([
          ': keepalive\r\n',
          'event: status\r\n',
          'data: {"text":"正在分析需求..."}\r\n',
          'id: evt-1\r\n',
          '\r\n',
          'event: chunk\r\n',
          'data: {"content":"第一段"}\r\n',
          '\r\n',
          'data: 默认消息片段\r\n',
          '\r\n',
          'event: done\r\n',
          'data: {"content":"完成"}\r\n',
          '\r\n',
        ].join(''), {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }))))
      const onStatus = vi.fn()
      const onChunk = vi.fn()
      const onDone = vi.fn()
      const onError = vi.fn()

      ssePost('/test', {}, { onStatus, onChunk, onDone, onError })

      await vi.waitFor(() => {
        expect(onStatus).toHaveBeenCalledWith('正在分析需求...')
        expect(onChunk).toHaveBeenCalledWith('第一段')
        expect(onChunk).toHaveBeenCalledWith('默认消息片段')
        expect(onDone).toHaveBeenCalledWith({ content: '完成' })
      }, { timeout: 500 })
      expect(onError).not.toHaveBeenCalled()
    })

    it('treats data DONE sentinel as completed without false interruption errors', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response('data: [DONE]\n\n', {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }))))
      const onDone = vi.fn()
      const onError = vi.fn()

      ssePost('/test', {}, { onDone, onError })

      await vi.waitFor(() => {
        expect(onDone).toHaveBeenCalledWith({ type: 'done' })
      }, { timeout: 500 })
      expect(onError).not.toHaveBeenCalled()
    })

    it('keeps stream frames intact when CRLF delimiters are split across chunks', async () => {
      const encoder = new TextEncoder()
      const read = vi.fn()
        .mockResolvedValueOnce({ done: false, value: encoder.encode('event: status\r') })
        .mockResolvedValueOnce({ done: false, value: encoder.encode('\ndata: {"status":"思考中..."}\r') })
        .mockResolvedValueOnce({ done: false, value: encoder.encode('\n\r\n') })
        .mockResolvedValueOnce({ done: false, value: encoder.encode('event: chunk\r\ndata: {"delta":"中"}\r\n\r\n') })
        .mockResolvedValueOnce({ done: false, value: encoder.encode('data: [DONE]\r\n\r\n') })
        .mockResolvedValueOnce({ done: true, value: undefined })

      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve({
          ok: true,
          status: 200,
          body: { getReader: () => ({ read }) },
        })))

      const onStatus = vi.fn()
      const onChunk = vi.fn()
      const onDone = vi.fn()
      const onError = vi.fn()

      ssePost('/test', {}, { onStatus, onChunk, onDone, onError })

      await vi.waitFor(() => {
        expect(onStatus).toHaveBeenCalledWith('思考中...')
        expect(onChunk).toHaveBeenCalledWith('中')
        expect(onDone).toHaveBeenCalledWith({ type: 'done' })
      }, { timeout: 500 })
      expect(onError).not.toHaveBeenCalled()
    })

    it('maps backend tool events to skill callbacks', async () => {
      vi.stubGlobal('fetch', vi.fn(() =>
        Promise.resolve(new Response([
          'event:tool_start\ndata:{"tool":"douyin_ops_commander"}',
          '',
          'event:tool_end\ndata:{"tool":"douyin_ops_commander","status":"success"}',
          '',
          'event:done\ndata:{"content":"完成"}',
          '',
          '',
        ].join('\n'), {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }))))
      const onSkillStart = vi.fn()
      const onSkillEnd = vi.fn()
      const onError = vi.fn()

      ssePost('/agent/chat-stream', {}, { onSkillStart, onSkillEnd, onError })

      await vi.waitFor(() => {
        expect(onSkillStart).toHaveBeenCalledWith({
          tool: 'douyin_ops_commander',
          description: '',
        })
        expect(onSkillEnd).toHaveBeenCalledWith({
          tool: 'douyin_ops_commander',
          status: 'success',
          error: undefined,
        })
      }, { timeout: 500 })
      expect(onError).not.toHaveBeenCalled()
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
