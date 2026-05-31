import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { addCompetitor, analyzeCompetitor, generateWeeklyReport, listCompetitors, removeCompetitor } from '../competitor'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('competitor API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('calls competitor monitor endpoints with backend contracts', async () => {
    mockPost.mockResolvedValueOnce('添加成功')
    await addCompetitor({ accountId: 'acc-1', accountName: '竞品 A', platform: 'douyin' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/competitor/add', {
      accountId: 'acc-1',
      accountName: '竞品 A',
      platform: 'douyin',
    })

    mockPost.mockResolvedValueOnce([])
    await listCompetitors()
    expect(mockPost).toHaveBeenCalledWith('/short-video/competitor/list', {})

    mockPost.mockResolvedValueOnce({ analysis: '内容策略' })
    await analyzeCompetitor(9)
    expect(mockPost).toHaveBeenCalledWith('/short-video/competitor/analyze', { competitorId: 9 })

    mockPost.mockResolvedValueOnce('周报')
    await generateWeeklyReport()
    expect(mockPost).toHaveBeenCalledWith('/short-video/competitor/weekly-report', {})

    mockPost.mockResolvedValueOnce(undefined)
    await removeCompetitor(9)
    expect(mockPost).toHaveBeenCalledWith('/short-video/competitor/remove', { competitorId: 9 })
  })

  it('normalizes wrapped list, analysis and weekly report payloads', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [
            { id: '9', accountName: '竞品 A', accountId: 'acc-1', followerCount: '12000' },
          ],
        },
      })
      .mockResolvedValueOnce({
        detail: {
          summary: '内容节奏更快',
          score: '88',
        },
      })
      .mockResolvedValueOnce({
        result: {
          report: '本周竞品周报',
        },
      })

    await expect(listCompetitors()).resolves.toEqual([
      expect.objectContaining({ id: '9', accountName: '竞品 A' }),
    ])
    await expect(analyzeCompetitor(9)).resolves.toEqual({
      summary: '内容节奏更快',
      score: '88',
    })
    await expect(generateWeeklyReport()).resolves.toBe('本周竞品周报')
  })
})
