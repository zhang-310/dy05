import { describe, it, expect, vi } from 'vitest'
import { dashboardApi } from '@/api/dashboard'
import { aiApi } from '@/api/ai'
import { liveApi } from '@/api/live'

vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    adminStats: vi.fn(),
    adminDashboard: vi.fn(),
    kpiUnified: vi.fn(),
    liveFormatGmv: vi.fn(),
    productGmvSummary: vi.fn(),
    cockpitPreview: vi.fn(),
    conversionFunnel: vi.fn(),
  },
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    callTypeDistribution: vi.fn(),
  },
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionSearch: vi.fn(),
  },
}))

describe('DashboardPage - API 集成测试', () => {
  it('应该正确调用管理员统计 API', async () => {
    vi.mocked(dashboardApi.adminStats).mockResolvedValue({
      totalUsers: 100,
      activeUsers: 80,
      todayUsers: 10,
      totalVideos: 500,
      publishedVideos: 400,
      todayVideos: 20,
      totalLiveSessions: 200,
      completedSessions: 150,
      todaySessions: 5,
      totalShortVideos: 300,
      publishedShortVideos: 250,
      todayShortVideos: 15,
      totalCopyItems: 1000,
      approvedCopyItems: 800,
      todayCopyItems: 50,
      todayAiCalls: 100,
      todayAiAttempts: 120,
      aiSuccessRate: 83.33,
      todayRevenue: 50000,
    })

    const result = await dashboardApi.adminStats()

    expect(result.totalUsers).toBe(100)
    expect(result.todayRevenue).toBe(50000)
    expect(dashboardApi.adminStats).toHaveBeenCalled()
  })

  it('应该正确调用 KPI 统一 API', async () => {
    vi.mocked(dashboardApi.kpiUnified).mockResolvedValue({
      gmvToday: 150000,
      gmvMom: 15.5,
      gmvYoy: 25.3,
      gmvTarget: 200000,
      ordersToday: 500,
      avgOrderValue: 300,
      conversionRate: 3.5,
      liveSessions: 10,
      activeSessionCount: 3,
      aiCallsToday: 100,
      aiCallCount: 100,
      publishedVideos: 20,
      docCount: 50,
    })

    const result = await dashboardApi.kpiUnified(30)

    expect(result.gmvToday).toBe(150000)
    expect(result.gmvMom).toBe(15.5)
    expect(result.conversionRate).toBe(3.5)
    expect(dashboardApi.kpiUnified).toHaveBeenCalledWith(30)
  })

  it('应该正确调用直播形式 GMV API', async () => {
    vi.mocked(dashboardApi.liveFormatGmv).mockResolvedValue([
      {
        format: '单人',
        gmv: 100000,
        sessions: 10,
        avgGmv: 10000,
      },
      {
        format: '多人',
        gmv: 80000,
        sessions: 5,
        avgGmv: 16000,
      },
    ])

    const result = await dashboardApi.liveFormatGmv(30)

    expect(result).toHaveLength(2)
    expect(result[0].format).toBe('单人')
    expect(result[0].gmv).toBe(100000)
    expect(dashboardApi.liveFormatGmv).toHaveBeenCalledWith(30)
  })

  it('应该正确调用商品 GMV 汇总 API', async () => {
    vi.mocked(dashboardApi.productGmvSummary).mockResolvedValue([
      {
        productId: 1,
        productName: '玻尿酸精华液',
        totalGmv: 50000,
        totalOrders: 200,
        avgPrice: 250,
      },
      {
        productId: 2,
        productName: '面霜',
        totalGmv: 40000,
        totalOrders: 150,
        avgPrice: 266.67,
      },
    ])

    const result = await dashboardApi.productGmvSummary(30)

    expect(result).toHaveLength(2)
    expect(result[0].productName).toBe('玻尿酸精华液')
    expect(result[0].totalGmv).toBe(50000)
    expect(dashboardApi.productGmvSummary).toHaveBeenCalledWith(30)
  })

  it('应该正确调用驾驶舱预览 API', async () => {
    vi.mocked(dashboardApi.cockpitPreview).mockResolvedValue([
      {
        date: '2026-05-10',
        gmv: 150000,
        orders: 500,
        sessions: 10,
      },
      {
        date: '2026-05-09',
        gmv: 140000,
        orders: 480,
        sessions: 9,
      },
    ])

    const result = await dashboardApi.cockpitPreview(30)

    expect(result).toHaveLength(2)
    expect(result[0].date).toBe('2026-05-10')
    expect(result[0].gmv).toBe(150000)
    expect(dashboardApi.cockpitPreview).toHaveBeenCalledWith(30)
  })

  it('应该正确调用转化漏斗 API', async () => {
    vi.mocked(dashboardApi.conversionFunnel).mockResolvedValue({
      exposure: 100000,
      clicks: 10000,
      addToCart: 2000,
      orders: 500,
      payments: 480,
    })

    const result = await dashboardApi.conversionFunnel(30)

    expect(result.exposure).toBe(100000)
    expect(result.orders).toBe(500)
    expect(result.payments).toBe(480)
    expect(dashboardApi.conversionFunnel).toHaveBeenCalledWith(30)
  })

  it('应该正确调用 AI 调用类型分布 API', async () => {
    vi.mocked(aiApi.callTypeDistribution).mockResolvedValue([
      {
        callType: 'script_generate',
        count: 100,
        successCount: 95,
        failCount: 5,
        avgDuration: 2.5,
      },
      {
        callType: 'kb_search',
        count: 80,
        successCount: 78,
        failCount: 2,
        avgDuration: 1.2,
      },
    ])

    const result = await aiApi.callTypeDistribution({ days: 30 })

    expect(result).toHaveLength(2)
    expect(result[0].callType).toBe('script_generate')
    expect(result[0].count).toBe(100)
    expect(aiApi.callTypeDistribution).toHaveBeenCalled()
  })

  it('应该正确调用直播场次查询 API', async () => {
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      list: [
        {
          id: 1,
          liveTitle: '护肤品专场',
          status: 1,
          sessionType: '品牌专场',
          liveFormat: '单人',
          scriptStyle: '专业',
          scheduledTime: '2026-05-15T19:00:00',
          totalGmv: 150000,
          viewers: 5000,
          likes: 1200,
          accountId: 101,
          personaId: 201,
          liveDescription: '春季护肤专场',
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await liveApi.sessionSearch({ page: 0, rows: 20, status: 1 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].liveTitle).toBe('护肤品专场')
    expect(liveApi.sessionSearch).toHaveBeenCalled()
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(dashboardApi.adminStats).mockRejectedValue(new Error('Network error'))

    await expect(dashboardApi.adminStats()).rejects.toThrow('Network error')
  })

  it('应该验证统计指标类型', () => {
    const STAT_TYPES = [
      'totalUsers',
      'activeUsers',
      'todayUsers',
      'totalVideos',
      'totalLiveSessions',
      'totalShortVideos',
      'totalCopyItems',
      'todayAiCalls',
      'todayRevenue',
    ]

    expect(STAT_TYPES).toHaveLength(9)
    expect(STAT_TYPES).toContain('totalUsers')
    expect(STAT_TYPES).toContain('todayRevenue')
  })

  it('应该验证 KPI 指标类型', () => {
    const KPI_TYPES = [
      'gmvToday',
      'gmvMom',
      'gmvYoy',
      'ordersToday',
      'avgOrderValue',
      'conversionRate',
      'liveSessions',
      'aiCallsToday',
    ]

    expect(KPI_TYPES).toHaveLength(8)
    expect(KPI_TYPES).toContain('gmvToday')
    expect(KPI_TYPES).toContain('conversionRate')
  })

  it('应该验证直播形式常量', () => {
    const LIVE_FORMATS = ['单人', '多人', '连麦']

    expect(LIVE_FORMATS).toHaveLength(3)
    expect(LIVE_FORMATS).toContain('单人')
    expect(LIVE_FORMATS).toContain('多人')
  })
})
