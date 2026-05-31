import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { dashboardApi } from '../dashboard'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('dashboard API', () => {
  const mockPost = vi.mocked(request.post)

  beforeEach(() => {
    mockPost.mockReset()
  })

  it('normalizes unified KPI from backend dimension object', async () => {
    mockPost.mockResolvedValue({
      content: { shortVideoCount: 3, kbDocumentCount: 7 },
      conversion: { saleQuantitySinceToday: 12 },
      revenue: { todayGmv: '1000.50', yesterdayGmv: '800.00', avgOrderValueToday: '83.37' },
    })

    const result = await dashboardApi.kpiUnified(30)

    expect(mockPost).toHaveBeenCalledWith('/dashboard/kpi-unified', { lookbackDays: 30 })
    expect(result).toMatchObject({
      gmvToday: 1000.5,
      ordersToday: 12,
      avgOrderValue: 83.37,
      publishedVideos: 3,
      docCount: 7,
    })
    expect(result.gmvMom).toBeCloseTo(25.0625)
  })

  it('keeps backend percentage gmvMom without ratio conversion when provided', async () => {
    mockPost.mockResolvedValue({
      gmvToday: 1000,
      gmvMom: 25.5,
      ordersToday: 8,
      conversionRate: 0.125,
    })

    await expect(dashboardApi.kpiUnified(30)).resolves.toMatchObject({
      gmvMom: 25.5,
      conversionRate: 0.125,
    })
  })

  it('normalizes dashboard row wrappers from real GMV endpoints', async () => {
    mockPost
      .mockResolvedValueOnce({
        rows: [{ liveFormat: '专场', sessionCount: 2, totalGmv: '3000' }],
      })
      .mockResolvedValueOnce({
        rows: [{ productId: 9, productName: '精华', sessionCount: 3, totalGmv: '900' }],
      })
      .mockResolvedValueOnce({
        rows: [{ sessionId: 1, startTime: '2026-05-21 20:00:00', gmv: '199', productLineCount: 4 }],
      })
      .mockResolvedValueOnce({
        steps: [
          { name: '观看', value: 100 },
          { name: '点赞', value: 20 },
          { name: '进入商品', value: 5 },
        ],
      })

    await expect(dashboardApi.liveFormatGmv(7)).resolves.toEqual([
      { format: '专场', gmv: 3000, sessions: 2, avgGmv: 1500 },
    ])
    await expect(dashboardApi.productGmvSummary(7)).resolves.toEqual([
      { productId: 9, productName: '精华', totalGmv: 900, totalOrders: 0, sessionCount: 3, avgPrice: 0 },
    ])
    await expect(dashboardApi.cockpitPreview(7)).resolves.toEqual([
      { date: '2026-05-21', gmv: 199, orders: 4, sessions: 1 },
    ])
    expect(mockPost).toHaveBeenNthCalledWith(3, '/dashboard/cockpit-preview', expect.objectContaining({
      lookbackDays: 7,
      dateFrom: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
    }))
    await expect(dashboardApi.conversionFunnel(7)).resolves.toMatchObject({
      exposure: 100,
      clicks: 20,
      addToCart: 5,
      steps: [
        { name: '观看', value: 100 },
        { name: '点赞', value: 20 },
        { name: '进入商品', value: 5 },
      ],
    })
  })

  it('normalizes RESTResult wrappers, aliases, json forecast, and admin success rate fields', async () => {
    mockPost
      .mockResolvedValueOnce({
        status: 200,
        data: {
          totalLives: '12',
          todayAiCalls: '8',
          todayAiAttempts: '10',
          todayAiSuccessRate: '80%',
          todayRevenue: '¥1200.50',
        },
      })
      .mockResolvedValueOnce({
        status: 200,
        data: {
          revenue: { todayGmv: '2000', yesterdayGmv: '1000' },
          content: { shortVideoCount: '4', kbDocumentCount: '9' },
          gmvForecast: '[2200,2300,"2400"]',
        },
      })
      .mockResolvedValueOnce({
        status: 200,
        data: {
          data: {
            records: [
              { sessionId: 1, startTime: '2026-05-21 10:00:00', gmv: '100', productLineCount: 2 },
              { sessionId: 2, startTime: '2026-05-21 20:00:00', gmv: '300', productLineCount: 3 },
            ],
          },
        },
      })

    await expect(dashboardApi.adminStats()).resolves.toMatchObject({
      totalLiveSessions: 12,
      todayAiCalls: 8,
      todayAiAttempts: 10,
      aiSuccessRate: 80,
      todayRevenue: 1200.5,
    })
    await expect(dashboardApi.kpiUnified(30)).resolves.toMatchObject({
      gmvToday: 2000,
      gmvMom: 100,
      publishedVideos: 4,
      docCount: 9,
      gmvForecast: [2200, 2300, 2400],
    })
    await expect(dashboardApi.cockpitPreview(30)).resolves.toEqual([
      { date: '2026-05-21', gmv: 400, orders: 5, sessions: 2 },
    ])
  })

  it('normalizes cockpit row preview, export result, and profit matrix', async () => {
    mockPost
      .mockResolvedValueOnce({
        rows: [
          { sessionId: '8', liveTitle: '晚场直播', status: '1', startTime: '2026-05-22 20:00:00', gmv: '1299.50', productLineCount: '6' },
        ],
      })
      .mockResolvedValueOnce({
        csv: '场次ID,标题\n8,晚场直播\n',
        filename: 'cockpit.csv',
        rowCount: '1',
      })
      .mockResolvedValueOnce({
        lookbackDays: '30',
        since: '2026-04-22',
        rows: [
          { liveFormat: '专场', sessionCount: '2', totalGmv: '3000', estimatedMarginRate: '0.25', isEstimated: true, note: '估算' },
        ],
      })

    await expect(dashboardApi.cockpitPreviewRows({ lookbackDays: 7, dateFrom: '2026-05-01', sessionStatus: 1 })).resolves.toEqual([
      {
        sessionId: 8,
        liveTitle: '晚场直播',
        status: 1,
        startTime: '2026-05-22 20:00:00',
        gmv: 1299.5,
        productLineCount: 6,
      },
    ])
    expect(mockPost).toHaveBeenNthCalledWith(1, '/dashboard/cockpit-preview', {
      lookbackDays: 7,
      dateFrom: '2026-05-01',
      sessionStatus: 1,
    })

    await expect(dashboardApi.cockpitExport({ lookbackDays: 7, dateFrom: '2026-05-01', sessionStatus: '' })).resolves.toEqual({
      csv: '场次ID,标题\n8,晚场直播\n',
      filename: 'cockpit.csv',
      rowCount: 1,
    })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/dashboard/cockpit-export', {
      lookbackDays: 7,
      dateFrom: '2026-05-01',
      sessionStatus: undefined,
    })

    await expect(dashboardApi.profitMatrixPreview(30)).resolves.toEqual({
      lookbackDays: 30,
      since: '2026-04-22',
      rows: [
        {
          liveFormat: '专场',
          sessionCount: 2,
          totalGmv: 3000,
          estimatedMarginRate: 0.25,
          isEstimated: true,
          note: '估算',
        },
      ],
    })
  })

  it('normalizes named stats wrappers used by organization analytics', async () => {
    mockPost.mockResolvedValueOnce({
      result: {
        stats: {
          totalUsers: '8',
          todayAiCalls: '12',
          totalLiveSessions: '3',
        },
      },
    })

    await expect(dashboardApi.orgStats()).resolves.toMatchObject({
      totalUsers: 8,
      todayAiCalls: 12,
      totalLiveSessions: 3,
    })
  })
})
