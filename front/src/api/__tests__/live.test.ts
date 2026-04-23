/**
 * live API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { liveApi, getSession } from '../live'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('live API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sessionSearch calls post with correct path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await liveApi.sessionSearch({ page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/live/session/search', { page: 0, rows: 10 })
  })

  it('getSession calls post with id', async () => {
    mockPost.mockResolvedValue({ id: 1, liveTitle: '测试', userId: 1, accountId: 1, personaId: 1, sessionCover: '', scriptStyle: '', liveDescription: '', scheduledTime: '', scheduledEndTime: '', startTime: '', endTime: '', liveUrl: '', viewers: 0, likes: 0, status: 1, sessionType: '', liveFormat: '', createTime: '', updateTime: '' })
    const result = await getSession(1)
    expect(mockPost).toHaveBeenCalledWith('/live/session/get', { id: 1 })
    expect(result.id).toBe(1)
  })

  it('sessionClone posts clone request', async () => {
    mockPost.mockResolvedValue(99)
    await liveApi.sessionClone(99)
    expect(mockPost).toHaveBeenCalledWith('/live/session/clone', { id: 99 })
  })

  it('scriptSave posts script payload', async () => {
    mockPost.mockResolvedValue(11)
    await liveApi.scriptSave({
      sessionId: 1,
      scriptTitle: '开场话术',
      scriptContent: '欢迎来到直播间',
      scriptType: 'opening',
    })
    expect(mockPost).toHaveBeenCalledWith('/live/script/save', {
      sessionId: 1,
      scriptTitle: '开场话术',
      scriptContent: '欢迎来到直播间',
      scriptType: 'opening',
    })
  })

  it('productBatchSort posts session and product ids', async () => {
    mockPost.mockResolvedValue(undefined)
    await liveApi.productBatchSort(7, [101, 102, 103])
    expect(mockPost).toHaveBeenCalledWith('/live/product/batch-sort', {
      sessionId: 7,
      productIds: [101, 102, 103],
    })
  })

  it('aiGenerateFull posts generation payload', async () => {
    mockPost.mockResolvedValue([])
    await liveApi.aiGenerateFull({ sessionId: 5, style: 'professional' })
    expect(mockPost).toHaveBeenCalledWith('/live/ai/generate-full', {
      sessionId: 5,
      style: 'professional',
    })
  })

  it('exposes SSE endpoint constants for live generation', () => {
    expect(liveApi.aiGenerateFullSse).toBe('/live/ai/generate-full-sse')
    expect(liveApi.aiGenerateFullPipelinedSse).toBe('/live/ai/generate-full-pipelined-sse')
    expect(liveApi.aiGenerateSkeletonSse).toBe('/live/ai/generate-skeleton-sse')
  })
})
