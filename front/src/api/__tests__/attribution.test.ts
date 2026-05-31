import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { attributionApi } from '../attribution'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('attribution API', () => {
  const mockPost = vi.mocked(request.default.post)
  const mockDelete = vi.mocked(request.default.delete)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('trigger posts session id payload', async () => {
    mockPost.mockResolvedValue(10)
    await attributionApi.trigger(10)
    expect(mockPost).toHaveBeenCalledWith('/ai/attribution/trigger', { sessionId: 10 })
  })

  it('session posts session id payload', async () => {
    mockPost.mockResolvedValue([])
    await attributionApi.session(10)
    expect(mockPost).toHaveBeenCalledWith('/ai/attribution/session', { sessionId: 10 })
  })

  it('summary posts session id payload', async () => {
    mockPost.mockResolvedValue({ sessionId: 10, totalGmv: 1000 })
    await attributionApi.summary(10)
    expect(mockPost).toHaveBeenCalledWith('/ai/attribution/summary', { sessionId: 10 })
  })

  it('get posts attribution id payload', async () => {
    mockPost.mockResolvedValue({ id: 20 })
    await attributionApi.get(20)
    expect(mockPost).toHaveBeenCalledWith('/ai/attribution/get', { id: 20 })
  })

  it('deleteSession deletes by session id path', async () => {
    mockDelete.mockResolvedValue(undefined)
    await attributionApi.deleteSession(10)
    expect(mockDelete).toHaveBeenCalledWith('/ai/attribution/session/10')
  })

  it('normalizes wrapped attribution session, summary and detail responses', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ id: 1, sessionId: '10', type: 'product_gmv', gmv: '1200', sales: '8', ratio: '0.6', score: '88', createdAt: '2026-05-22' }] } })
      .mockResolvedValueOnce({ data: { sessionId: '10', gmv: '2000', sales: '12', productCount: 2, scriptCount: 3, score: 91, status: 'completed' } })
      .mockResolvedValueOnce({ data: { id: 2, sessionId: 10, attributionType: 'script_sales', gmvContribution: 500, orderCount: 4, createTime: '2026-05-22' } })

    await expect(attributionApi.session(10)).resolves.toEqual([
      expect.objectContaining({
        id: 1,
        sessionId: 10,
        attributionType: 'product_gmv',
        contributedGmv: 1200,
        contributedSales: 8,
        contributionRatio: 0.6,
        effectScore: 88,
      }),
    ])
    await expect(attributionApi.summary(10)).resolves.toMatchObject({
      sessionId: 10,
      totalGmv: 2000,
      totalSales: 12,
      productAttributions: 2,
      scriptAttributions: 3,
      overallScore: 91,
      status: 'completed',
    })
    await expect(attributionApi.get(2)).resolves.toMatchObject({
      id: 2,
      contributedGmv: 500,
      contributedSales: 4,
    })
  })
})
