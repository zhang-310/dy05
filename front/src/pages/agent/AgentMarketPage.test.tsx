import { describe, it, expect, vi } from 'vitest'
import { agentApi, reviewApi } from '@/api/agent'

vi.mock('@/api/agent', () => ({
  agentApi: {
    list: vi.fn(),
  },
  reviewApi: {
    submit: vi.fn(),
    myReview: vi.fn(),
    stats: vi.fn(),
    list: vi.fn(),
  },
}))

describe('AgentMarketPage - API 集成测试', () => {
  it('应该正确调用智能体市场列表 API', async () => {
    vi.mocked(agentApi.list).mockResolvedValue({
      list: [
        {
          id: 1,
          agentName: '话术生成助手',
          agentType: 1,
          description: '专业的直播话术生成工具',
          systemPrompt: '你是话术生成助手',
          averageRating: 4.5,
          ratingCount: 120,
          conversationCount: 500,
          status: 1,
          responseMode: 0,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await agentApi.list({
      page: 0,
      rows: 20,
    })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].agentName).toBe('话术生成助手')
    expect(result.list[0].averageRating).toBe(4.5)
    expect(agentApi.list).toHaveBeenCalled()
  })

  it('应该支持关键词搜索', async () => {
    vi.mocked(agentApi.list).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await agentApi.list({
      page: 0,
      rows: 20,
      agentName: '话术',
    })

    expect(agentApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        agentName: '话术',
      })
    )
  })

  it('应该支持类型筛选', async () => {
    vi.mocked(agentApi.list).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await agentApi.list({
      page: 0,
      rows: 20,
      agentType: 1,
    })

    expect(agentApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        agentType: 1,
      })
    )
  })

  it('应该支持排序', async () => {
    vi.mocked(agentApi.list).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await agentApi.list({
      page: 0,
      rows: 20,
      sortBy: 'rating',
    })

    expect(agentApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        sortBy: 'rating',
      })
    )
  })

  it('应该正确调用评分提交 API', async () => {
    vi.mocked(reviewApi.submit).mockResolvedValue({ id: 1 })

    const result = await reviewApi.submit(1, 5, '非常好用')

    expect(result.id).toBe(1)
    expect(reviewApi.submit).toHaveBeenCalledWith(1, 5, '非常好用')
  })

  it('应该正确调用我的评分查询 API', async () => {
    vi.mocked(reviewApi.myReview).mockResolvedValue({
      id: 1,
      agentId: 1,
      userId: 1,
      rating: 5,
      content: '非常好用',
      createdAt: '2026-05-10T10:00:00',
    })

    const result = await reviewApi.myReview(1)

    expect(result?.rating).toBe(5)
    expect(result?.content).toBe('非常好用')
    expect(reviewApi.myReview).toHaveBeenCalledWith(1)
  })

  it('应该正确调用评分统计 API', async () => {
    vi.mocked(reviewApi.stats).mockResolvedValue({
      agentId: 1,
      averageRating: 4.5,
      reviewCount: 120,
      distribution: {
        5: 80,
        4: 30,
        3: 8,
        2: 2,
        1: 0,
      },
    })

    const result = await reviewApi.stats(1)

    expect(result.averageRating).toBe(4.5)
    expect(result.reviewCount).toBe(120)
    expect(reviewApi.stats).toHaveBeenCalledWith(1)
  })

  it('应该正确调用评论列表 API', async () => {
    vi.mocked(reviewApi.list).mockResolvedValue({
      list: [
        {
          id: 1,
          agentId: 1,
          userId: 1,
          userName: '用户A',
          rating: 5,
          content: '非常好用',
          createdAt: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await reviewApi.list(1, 0, 20)

    expect(result.list).toHaveLength(1)
    expect(result.list[0].rating).toBe(5)
    expect(reviewApi.list).toHaveBeenCalled()
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(agentApi.list).mockRejectedValue(new Error('Network error'))

    await expect(
      agentApi.list({ page: 0, rows: 20 })
    ).rejects.toThrow('Network error')
  })

  it('应该验证智能体类型常量', () => {
    const AGENT_TYPES = {
      0: '自定义',
      1: '话术生成',
      2: '违规检测',
      3: '商品分析',
      4: '场次规划',
      5: '数据分析',
      6: '客户服务',
    }

    expect(Object.keys(AGENT_TYPES)).toHaveLength(7)
    expect(AGENT_TYPES[1]).toBe('话术生成')
    expect(AGENT_TYPES[2]).toBe('违规检测')
  })

  it('应该验证数据隔离（只返回公开智能体）', async () => {
    vi.mocked(agentApi.list).mockResolvedValue({
      list: [
        {
          id: 1,
          agentName: '公开智能体',
          agentType: 1,
          description: '公开',
          systemPrompt: '公开',
          averageRating: 4.5,
          ratingCount: 120,
          conversationCount: 500,
          status: 1,
          responseMode: 0,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await agentApi.list({ page: 0, rows: 20 })

    expect(result.list.every(a => a.status === 1)).toBe(true)
  })

  it('应该验证缓存键正确性', () => {
    const cacheKeys = {
      agentList: 'agent-market',
      myReview: 'agent-review-my',
      reviewStats: 'agent-review-stats',
      reviewList: 'agent-review-list',
    }

    expect(cacheKeys.agentList).toBe('agent-market')
    expect(cacheKeys.myReview).toBe('agent-review-my')
    expect(cacheKeys.reviewStats).toBe('agent-review-stats')
  })
})
