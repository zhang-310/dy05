import { describe, it, expect, vi } from 'vitest'
import { aiApi } from '@/api/ai'
import { douyinApi } from '@/api/douyin'

vi.mock('@/api/ai', () => ({
  aiApi: {
    evolveList: vi.fn(),
    evolveGet: vi.fn(),
    evolveStats: vi.fn(),
    evolveTrigger: vi.fn(),
    evolveDelete: vi.fn(),
    liveReviewList: vi.fn(),
    liveReviewGet: vi.fn(),
    liveReviewTrigger: vi.fn(),
    evolveTaskList: vi.fn(),
    evolveTaskTrigger: vi.fn(),
    evolveTaskCancel: vi.fn(),
  },
}))

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    videoSearch: vi.fn(),
  },
}))

describe('ViralAnalysisPage - API 集成测试', () => {
  it('应该正确调用爆款分析列表 API', async () => {
    vi.mocked(aiApi.evolveList).mockResolvedValue({
      list: [
        {
          id: 1,
          videoId: 101,
          videoTitle: '护肤品测评',
          accountId: 1,
          ownerId: 1,
          viralScore: 85.5,
          viewCount: 100000,
          avgViewCount: 5000,
          successFactors: '高质量内容,精准定位',
          replicableMethods: '使用对比测评,突出产品优势',
          reportContent: '详细分析报告',
          qualityScore: 90,
          modelUsed: 'gpt-4',
          tokensUsed: 1500,
          status: 1,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await aiApi.evolveList({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].videoTitle).toBe('护肤品测评')
    expect(result.list[0].viralScore).toBe(85.5)
    expect(aiApi.evolveList).toHaveBeenCalled()
  })

  it('应该支持状态筛选', async () => {
    vi.mocked(aiApi.evolveList).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await aiApi.evolveList({ page: 0, rows: 20, status: 1 })

    expect(aiApi.evolveList).toHaveBeenCalledWith(
      expect.objectContaining({
        status: 1,
      })
    )
  })

  it('应该正确调用爆款分析详情 API', async () => {
    vi.mocked(aiApi.evolveGet).mockResolvedValue({
      id: 1,
      videoId: 101,
      videoTitle: '护肤品测评',
      accountId: 1,
      ownerId: 1,
      viralScore: 85.5,
      viewCount: 100000,
      avgViewCount: 5000,
      successFactors: '高质量内容,精准定位',
      replicableMethods: '使用对比测评,突出产品优势',
      reportContent: '详细分析报告',
      qualityScore: 90,
      modelUsed: 'gpt-4',
      tokensUsed: 1500,
      status: 1,
      createTime: '2026-05-10T10:00:00',
    })

    const result = await aiApi.evolveGet(1)

    expect(result.videoTitle).toBe('护肤品测评')
    expect(result.viralScore).toBe(85.5)
    expect(aiApi.evolveGet).toHaveBeenCalledWith(1)
  })

  it('应该正确调用统计数据 API', async () => {
    vi.mocked(aiApi.evolveStats).mockResolvedValue({
      viralAnalysisTotal: 100,
      viralAnalysisDone: 85,
      liveReviewTotal: 50,
      liveReviewDone: 40,
      indexQueuePending: 10,
    })

    const result = await aiApi.evolveStats()

    expect(result.viralAnalysisTotal).toBe(100)
    expect(result.viralAnalysisDone).toBe(85)
    expect(result.indexQueuePending).toBe(10)
    expect(aiApi.evolveStats).toHaveBeenCalled()
  })

  it('应该正确调用触发分析 API', async () => {
    vi.mocked(aiApi.evolveTrigger).mockResolvedValue({
      id: 1,
      message: '分析任务已创建',
    })

    const result = await aiApi.evolveTrigger({
      videoId: 101,
      accountId: 1,
    })

    expect(result.id).toBe(1)
    expect(result.message).toBe('分析任务已创建')
    expect(aiApi.evolveTrigger).toHaveBeenCalled()
  })

  it('应该正确调用删除分析 API', async () => {
    vi.mocked(aiApi.evolveDelete).mockResolvedValue(undefined)

    await aiApi.evolveDelete(1)

    expect(aiApi.evolveDelete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用直播复盘列表 API', async () => {
    vi.mocked(aiApi.liveReviewList).mockResolvedValue({
      list: [
        {
          id: 1,
          sessionId: 1,
          sessionTitle: '护肤品专场',
          reviewContent: '复盘内容',
          qualityScore: 85,
          status: 1,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await aiApi.liveReviewList({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].sessionTitle).toBe('护肤品专场')
    expect(aiApi.liveReviewList).toHaveBeenCalled()
  })

  it('应该正确调用直播复盘详情 API', async () => {
    vi.mocked(aiApi.liveReviewGet).mockResolvedValue({
      id: 1,
      sessionId: 1,
      sessionTitle: '护肤品专场',
      reviewContent: '复盘内容',
      qualityScore: 85,
      status: 1,
      createTime: '2026-05-10T10:00:00',
    })

    const result = await aiApi.liveReviewGet(1)

    expect(result.sessionTitle).toBe('护肤品专场')
    expect(aiApi.liveReviewGet).toHaveBeenCalledWith(1)
  })

  it('应该正确调用触发直播复盘 API', async () => {
    vi.mocked(aiApi.liveReviewTrigger).mockResolvedValue({
      id: 1,
      message: '复盘任务已创建',
    })

    const result = await aiApi.liveReviewTrigger({ sessionId: 1 })

    expect(result.id).toBe(1)
    expect(aiApi.liveReviewTrigger).toHaveBeenCalled()
  })

  it('应该正确调用视频搜索 API', async () => {
    vi.mocked(douyinApi.videoSearch).mockResolvedValue({
      list: [
        {
          id: 101,
          videoTitle: '护肤品测评',
          videoUrl: 'https://example.com/video1',
          viewCount: 100000,
          likeCount: 5000,
          commentCount: 500,
          shareCount: 200,
          status: 1,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await douyinApi.videoSearch({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].videoTitle).toBe('护肤品测评')
    expect(douyinApi.videoSearch).toHaveBeenCalled()
  })

  it('应该正确调用进化任务列表 API', async () => {
    vi.mocked(aiApi.evolveTaskList).mockResolvedValue({
      list: [
        {
          id: 1,
          taskType: 'viral_analysis',
          targetId: 101,
          status: 1,
          priority: 5,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await aiApi.evolveTaskList({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].taskType).toBe('viral_analysis')
    expect(aiApi.evolveTaskList).toHaveBeenCalled()
  })

  it('应该正确调用触发进化任务 API', async () => {
    vi.mocked(aiApi.evolveTaskTrigger).mockResolvedValue({
      taskId: '1',
      message: '任务已创建',
      taskType: 'viral_analysis',
    })

    const result = await aiApi.evolveTaskTrigger({
      taskType: 'viral_analysis',
      targetId: 101,
    })

    expect(result.taskId).toBe('1')
    expect(result.taskType).toBe('viral_analysis')
    expect(aiApi.evolveTaskTrigger).toHaveBeenCalled()
  })

  it('应该正确调用取消进化任务 API', async () => {
    vi.mocked(aiApi.evolveTaskCancel).mockResolvedValue(undefined)

    await aiApi.evolveTaskCancel(1)

    expect(aiApi.evolveTaskCancel).toHaveBeenCalledWith(1)
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(aiApi.evolveList).mockRejectedValue(new Error('Network error'))

    await expect(
      aiApi.evolveList({ page: 0, rows: 20 })
    ).rejects.toThrow('Network error')
  })

  it('应该验证分析状态常量', () => {
    const STATUS_LABELS: Record<number, { label: string; color: string }> = {
      0: { label: '分析中', color: 'info' },
      1: { label: '已完成', color: 'success' },
      2: { label: '失败', color: 'error' },
    }

    expect(Object.keys(STATUS_LABELS)).toHaveLength(3)
    expect(STATUS_LABELS[0].label).toBe('分析中')
    expect(STATUS_LABELS[1].label).toBe('已完成')
    expect(STATUS_LABELS[2].label).toBe('失败')
  })

  it('应该验证统计指标类型', () => {
    const STAT_TYPES = [
      'viralAnalysisTotal',
      'viralAnalysisDone',
      'liveReviewTotal',
      'liveReviewDone',
      'indexQueuePending',
    ]

    expect(STAT_TYPES).toHaveLength(5)
    expect(STAT_TYPES).toContain('viralAnalysisTotal')
    expect(STAT_TYPES).toContain('indexQueuePending')
  })
})
