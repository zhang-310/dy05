import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { abtestApi } from '../abtest'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('abtest API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts experiment query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await abtestApi.list({ page: 0, rows: 20, experimentName: '直播实验' })
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/list', {
      page: 0,
      rows: 20,
      keyword: '直播实验',
      experimentType: undefined,
      status: undefined,
    })
  })

  it('normalizes wrapped experiment list and variants', async () => {
    mockPost.mockResolvedValue({
      records: [{
        id: 7,
        experimentName: '包装实验',
        status: 1,
        experimentType: 'script_style',
        variants: {
          items: [
            { variantId: 2, variantName: 'B', viewCount: 100, conversionCount: 8, conversionRate: 8 },
          ],
        },
      }],
      totalElements: 1,
      page: 0,
      size: 20,
    })

    const result = await abtestApi.list({ page: 0, rows: 20 })

    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [{
        id: 7,
        name: '包装实验',
        variants: [{ id: 2, exposures: 100, conversions: 8, conversionRate: 0.08 }],
      }],
    })
  })

  it('get/delete/status use request params contract', async () => {
    mockPost.mockResolvedValue({
      id: 3,
      name: '直播实验',
      status: 1,
      experimentType: 'script_style',
      variants: [],
    })
    await abtestApi.get(3)
    await abtestApi.delete(3)
    await abtestApi.updateStatus(3, 2)

    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/get', undefined, { params: { id: 3 } })
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/delete', undefined, { params: { id: 3 } })
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/update-status', undefined, { params: { id: 3, status: 2 } })
  })

  it('save maps frontend aliases to real backend payload with variants', async () => {
    mockPost.mockResolvedValue(12)

    await abtestApi.save({
      experimentName: '直播开场测试',
      description: '测试话术风格',
      variants: [
        { variantName: 'A', variantType: 'A', styleCode: 'warm' },
        { variantName: 'B', variantType: 'B', scriptStyle: 'direct' },
      ],
    })

    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/save', {
      id: undefined,
      name: '直播开场测试',
      description: '测试话术风格',
      experimentType: 'script_style',
      status: 0,
      conclusion: undefined,
      targetEntityType: undefined,
      targetEntityId: undefined,
      variants: [
        { id: undefined, experimentId: 0, variantName: 'A', variantType: 'A', content: undefined, entityType: undefined, entityId: undefined, styleCode: 'warm' },
        { id: undefined, experimentId: 0, variantName: 'B', variantType: 'B', content: undefined, entityType: undefined, entityId: undefined, styleCode: 'direct' },
      ],
    })
  })

  it('setWinner posts experiment and variant ids', async () => {
    mockPost.mockResolvedValue(undefined)
    await abtestApi.setWinner(3, 5)
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/set-winner', {
      experimentId: 3,
      variantId: 5,
    })
  })

  it('start posts running status payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await abtestApi.start(9)
    await abtestApi.pause(9)
    await abtestApi.stop(9)
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/update-status', undefined, { params: { id: 9, status: 1 } })
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/update-status', undefined, { params: { id: 9, status: 3 } })
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/update-status', undefined, { params: { id: 9, status: 2 } })
  })

  it('eventRecord posts tracking event payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await abtestApi.eventRecord({ experimentId: 7, variantId: 2, eventType: 'conversion', userFingerprint: 'u1' })
    expect(mockPost).toHaveBeenCalledWith('/abtest/event/record', {
      experimentId: 7,
      variantId: 2,
      eventType: 'conversion',
      userFingerprint: 'u1',
      sessionId: undefined,
    })
  })

  it('normalizes statistics result aliases', async () => {
    mockPost.mockResolvedValue({
      experimentId: 7,
      experimentName: '测试',
      variantStats: {
        records: [
          { variantId: 1, variantName: 'A', viewCount: 100, conversionCount: 4, conversionRate: 4 },
        ],
      },
      totalSamples: 100,
      totalConversions: 4,
      overallConversionRate: 4,
    })

    const result = await abtestApi.result(7)

    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/result', undefined, { params: { experimentId: 7 } })
    expect(result.totalExposures).toBe(100)
    expect(result.overallConversionRate).toBe(0.04)
    expect(result.variants[0]).toMatchObject({ id: 1, exposures: 100, conversions: 4, conversionRate: 0.04 })
  })

  it('normalizes wrapped daily trend rows', async () => {
    mockPost.mockResolvedValue({
      items: [
        { date: '2026-05-22', variantAConversionRate: 4, variantBConversionRate: 8 },
      ],
    })

    const result = await abtestApi.dailyTrend(7, 14)

    expect(result).toEqual([
      expect.objectContaining({ date: '2026-05-22', variantAConversionRate: 4, variantBConversionRate: 8 }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/abtest/experiment/daily-trend', undefined, {
      params: expect.objectContaining({ experimentId: 7 }),
    })
  })
})
