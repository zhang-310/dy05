/**
 * product API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import * as request from '@/utils/request'
import * as auth from '@/utils/auth'
import { searchProducts, productApi, generateMultiStyleScriptsSse } from '../product'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'mock-token'),
}))

describe('product API', () => {
  const mockPost = vi.mocked(request.default.post)
  const mockPut = vi.mocked(request.default.put)
  const mockDelete = vi.mocked(request.default.delete)
  const mockGetToken = vi.mocked(auth.getToken)

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetToken.mockReturnValue('mock-token')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('searchProducts calls post with correct path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await searchProducts({ page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/product/search', { page: 0, rows: 10 })
  })

  it('list adapts page filters to backend product search contract and normalizes rows', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [{
        id: 1,
        userId: 7,
        productName: '链接精华',
        productCategory: '护肤',
        manufacturer: '品牌方',
        sku: 'SKU-1',
        imageUrl: 'https://img.example/a.png',
        aiSellingPoints: '修护屏障',
        price: '199',
        costPrice: '80',
        profitMarginPct: '0.55',
        inventory: '12',
        status: 1,
      }],
      pageNum: 0,
      pageSize: 20,
    })

    const result = await productApi.list({ page: 0, rows: 20, productName: '精华', category: '护肤', brand: '不传给后端' })

    expect(mockPost).toHaveBeenCalledWith('/product/search', {
      page: 0,
      rows: 20,
      keyword: '精华',
      productCategory: '护肤',
    })
    expect(result.list[0]).toMatchObject({
      productCode: 'SKU-1',
      category: '护肤',
      brand: '品牌方',
      mainImage: 'https://img.example/a.png',
      sellingPoints: '修护屏障',
      price: 199,
    })
  })

  it('list normalizes wrapped product page variants', async () => {
    mockPost.mockResolvedValue({
      data: {
        rows: [{
          id: 2,
          productName: '包装商品',
          productCategory: '彩妆',
          manufacturer: '品牌方',
          sku: 'SKU-2',
          imageUrl: 'https://img.example/b.png',
          aiSellingPoints: '高遮瑕',
          price: '129',
          inventory: '5',
          status: 1,
        }],
        totalRecords: 1,
        page: 0,
        size: 20,
      },
    })

    const result = await productApi.list({ page: 0, rows: 20 })

    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [{
        productName: '包装商品',
        category: '彩妆',
        brand: '品牌方',
        productCode: 'SKU-2',
        sellingPoints: '高遮瑕',
      }],
    })
  })

  it('get calls post with id', async () => {
    mockPost.mockResolvedValue({ id: 1, productName: '测试商品', userId: 1, productCode: '', category: '', brand: '', price: 0, costPrice: 0, profitMarginPct: 0, inventory: 0, unit: '', weight: 0, mainImage: '', description: '', highlights: '', sellingPoints: '', status: 1, createTime: '', updateTime: '' })
    await productApi.get(1)
    expect(mockPost).toHaveBeenCalledWith('/product/get', { id: 1 })
  })

  it('inferProductType uses real backend path and productId body', async () => {
    mockPost.mockResolvedValue('hot,profit')
    const { inferProductType } = await import('../product')
    await inferProductType(9)
    expect(mockPost).toHaveBeenCalledWith('/product/infer-product-type', { productId: 9 })
  })

  it('save calls post with data', async () => {
    mockPost.mockResolvedValue(undefined)
    const data = {
      productName: '新商品',
      productCode: 'SKU-9',
      category: '护肤',
      brand: '品牌方',
      imageUrl: 'https://img.example/p.png',
      productLink: 'https://shop.example/item/9',
      sellingPoints: '温和修护',
      price: 100,
      status: 1,
    }
    await productApi.save(data)
    expect(mockPost).toHaveBeenCalledWith('/product/save', {
      productName: '新商品',
      imageUrl: 'https://img.example/p.png',
      productLink: 'https://shop.example/item/9',
      price: 100,
      status: 1,
      sku: 'SKU-9',
      productCategory: '护肤',
      manufacturer: '品牌方',
      aiSellingPoints: '温和修护',
    })
  })

  it('extractFromLink posts productLink required by backend controller', async () => {
    mockPost.mockResolvedValue({ productName: '链接商品' })
    await productApi.extractFromLink('  https://shop.example/item/1  ')
    expect(mockPost).toHaveBeenCalledWith('/product/extract-from-link', {
      productLink: 'https://shop.example/item/1',
    })
  })

  it('delete calls post with id', async () => {
    mockPost.mockResolvedValue(undefined)
    await productApi.delete(1)
    expect(mockPost).toHaveBeenCalledWith('/product/delete', { id: 1 })
  })

  it('stylePresetList uses management list-all endpoint', async () => {
    mockPost.mockResolvedValue([])
    await productApi.stylePresetList()
    expect(mockPost).toHaveBeenCalledWith('/product/style-preset/list-all', {})
  })

  it('stylePresetList normalizes wrapped presets and backend aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            presetId: '7',
            name: '温柔种草',
            code: 'warm_seed',
            tone: '温暖',
            scene: '护肤',
            desc: '弱促单',
            enabled: 'false',
            orderNo: '12',
          },
        ],
      },
    })

    const result = await productApi.stylePresetList()

    expect(result).toEqual([
      expect.objectContaining({
        id: 7,
        presetName: '温柔种草',
        presetCode: 'warm_seed',
        styleValue: '温暖',
        category: '护肤',
        description: '弱促单',
        isEnabled: false,
        sortOrder: 12,
      }),
    ])
  })

  it('stylePresetRecommend posts productId to recommendation endpoint', async () => {
    mockPost.mockResolvedValue(['professional', 'warm'])
    await productApi.stylePresetRecommend(3)
    expect(mockPost).toHaveBeenCalledWith('/product/style-preset/recommend', { productId: 3 })
  })

  it('stylePresetRecommend normalizes wrapped object recommendation rows', async () => {
    mockPost.mockResolvedValue({
      data: {
        items: [
          { presetCode: 'professional' },
          { styleCode: 'warm' },
          'seed',
        ],
      },
    })

    const result = await productApi.stylePresetRecommend(3)

    expect(result).toEqual(['professional', 'warm', 'seed'])
  })

  it('salesHistorySave posts backend sales fields', async () => {
    mockPost.mockResolvedValue(9)
    await productApi.salesHistorySave({
      productId: 1,
      saleAmount: 299,
      saleQuantity: 3,
      saleTime: '2026-05-21T10:00',
      channelSource: 'douyin_live',
    })
    expect(mockPost).toHaveBeenCalledWith('/product/sales-history/save', {
      productId: 1,
      saleAmount: 299,
      saleQuantity: 3,
      saleTime: '2026-05-21T10:00',
      channelSource: 'douyin_live',
    })
  })

  it('sales history search and totals use backend contracts', async () => {
    mockPost.mockResolvedValueOnce({ total: 0, list: [], pageNum: 0, pageSize: 20 })
    mockPost.mockResolvedValueOnce(1299)
    mockPost.mockResolvedValueOnce(12)

    await productApi.salesHistorySearch({
      productId: 1,
      page: 0,
      rows: 20,
      channelSource: 'douyin_live',
      sessionId: 'live-1',
      startTime: '2026-05-21T00:00',
      endTime: '2026-05-21T23:59',
    })
    await productApi.salesHistoryTotalSalesAmount(1)
    await productApi.salesHistoryTotalSalesQuantity(1)

    expect(mockPost).toHaveBeenCalledWith('/product/sales-history/search', {
      productId: 1,
      page: 0,
      rows: 20,
      channelSource: 'douyin_live',
      sessionId: 'live-1',
      startTime: '2026-05-21T00:00',
      endTime: '2026-05-21T23:59',
    })
    expect(mockPost).toHaveBeenCalledWith('/product/sales-history/total-sales-amount', { productId: 1 })
    expect(mockPost).toHaveBeenCalledWith('/product/sales-history/total-sales-quantity', { productId: 1 })
  })

  it('salesHistorySearch and totals normalize wrapped rows and string numbers', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        rows: [
          {
            historyId: '11',
            productId: '3',
            revenue: '399.5',
            quantity: '4',
            saleDate: '2026-05-22 10:00:00',
            platform: 'douyin_live',
            createdAt: '2026-05-22 10:01:00',
          },
        ],
        totalRecords: '1',
        page: '0',
        size: '20',
      },
    })
    mockPost.mockResolvedValueOnce({ data: { amount: '1299.8' } })
    mockPost.mockResolvedValueOnce({ data: { quantity: '12' } })

    const page = await productApi.salesHistorySearch({ page: 0, rows: 20 })
    const amount = await productApi.salesHistoryTotalSalesAmount(3)
    const quantity = await productApi.salesHistoryTotalSalesQuantity(3)

    expect(page).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [
        {
          id: 11,
          productId: 3,
          saleAmount: 399.5,
          saleQuantity: 4,
          saleTime: '2026-05-22 10:00:00',
          channelSource: 'douyin_live',
          createTime: '2026-05-22 10:01:00',
        },
      ],
    })
    expect(amount).toBe(1299.8)
    expect(quantity).toBe(12)
  })

  it('scriptList normalizes array response from real product script search endpoint', async () => {
    mockPost.mockResolvedValue([
      {
        id: 8,
        productId: 1,
        scriptContent: '真实话术',
        scriptType: 'formal',
        style: 'professional',
        isActive: true,
        version: 2,
        createTime: '2026-05-21 10:00:00',
      },
    ])

    const result = await productApi.scriptList({ productId: 1, rows: 100 })

    expect(mockPost).toHaveBeenCalledWith('/product/script/search', { productId: 1, rows: 100 })
    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 100,
      list: [
        {
          id: 8,
          status: 1,
          scriptTitle: 'V2',
          scriptContent: '真实话术',
        },
      ],
    })
  })

  it('scriptList normalizes wrapped script rows and backend aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        rows: [
          {
            scriptId: '18',
            productId: '9',
            title: '',
            content: '包装话术内容',
            type: 'seed',
            styleCode: 'warm',
            durationSec: '45',
            usageCount: '6',
            effectivenessScore: '82.5',
            active: 'true',
            versionNumber: '3',
            createdAt: '2026-05-22 10:00:00',
          },
        ],
        totalRecords: '1',
        page: '0',
        size: '50',
      },
    })

    const result = await productApi.scriptList({ productId: 9, page: 0, rows: 50 })

    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 50,
      list: [
        {
          id: 18,
          productId: 9,
          scriptTitle: 'V3',
          scriptContent: '包装话术内容',
          scriptType: 'seed',
          style: 'warm',
          duration: 45,
          useCount: 6,
          rating: 82.5,
          status: 1,
          isActive: true,
          version: 3,
        },
      ],
    })
  })

  it('script activate and delete use path-style backend contract', async () => {
    mockPut.mockResolvedValue(undefined)
    mockDelete.mockResolvedValue(undefined)

    await productApi.scriptActivate(8)
    await productApi.scriptDelete(8)

    expect(mockPut).toHaveBeenCalledWith('/product/script/activate/8')
    expect(mockDelete).toHaveBeenCalledWith('/product/script/8')
  })

  it('effectivenessRanking uses query params and normalizes 0-100 backend scores', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [
        {
          productId: 1,
          versionId: 22,
          versionNumber: 2,
          style: '专业',
          score: 86,
          scoreLevel: 'A',
          usageCount: 18,
          conversionRate: 12.5,
          likesCount: 320,
          commentsCount: 18,
          isRecommended: true,
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })

    const result = await productApi.effectivenessRanking({ productId: 1, sortBy: 'score', page: 0, rows: 20, topN: 0 })

    expect(mockPost).toHaveBeenCalledWith('/product/script-effectiveness/ranking', undefined, {
      params: { productId: 1, sortBy: 'score', page: 0, rows: 20, topN: 0 },
    })
    expect(result).toMatchObject({
      total: 1,
      list: [
        {
          productId: 1,
          productName: 'V2 专业',
          avgScore: 8.6,
          useCount: 18,
          tag: '专业',
        },
      ],
      summary: {
        avgScore: 8.6,
        maxScore: 8.6,
        scoredCount: 1,
        avgConversionRate: 12.5,
      },
    })
  })

  it('effectivenessRanking normalizes records pagination aliases', async () => {
    mockPost.mockResolvedValue({
      records: [
        {
          productId: 3,
          versionId: 21,
          versionNumber: 3,
          style: 'warm',
          score: '88',
          usageCount: '4',
          conversionRate: '12.5',
          isRecommended: true,
        },
      ],
      totalElements: '1',
      summary: {
        dates: [],
        avgScores: [],
        avgScore: '8.8',
        maxScore: '8.8',
        scoredCount: '1',
        avgConversionRate: '12.5',
      },
    })

    const result = await productApi.effectivenessRanking({ productId: 3, page: 0, rows: 20 })

    expect(result).toMatchObject({
      total: 1,
      list: [
        {
          productId: 3,
          productName: 'V3 warm',
          avgScore: 8.8,
          useCount: 4,
          conversionRate: 12.5,
          isRecommended: true,
        },
      ],
      summary: {
        avgScore: 8.8,
        maxScore: 8.8,
        scoredCount: 1,
        avgConversionRate: 12.5,
      },
    })
  })

  it('effectiveness actions use backend query-param contract', async () => {
    mockPost.mockResolvedValue(3)

    await productApi.effectivenessRecalculate(1)
    await productApi.effectivenessCompare({ versionIds: [11, 12] })
    await productApi.effectivenessTrend(22, { days: 30 })

    expect(mockPost).toHaveBeenCalledWith('/product/script-effectiveness/recalculate', undefined, {
      params: { productId: 1 },
    })
    expect(mockPost).toHaveBeenCalledWith('/product/script-effectiveness/compare', undefined, {
      params: { versionIds: [11, 12] },
    })
    expect(mockPost).toHaveBeenCalledWith('/product/script-effectiveness/trend', undefined, {
      params: { versionId: 22, days: 30 },
    })
  })

  it('scriptUsageList normalizes real usage stats map without pretending it is a GMV array', async () => {
    mockPost.mockResolvedValue({
      totalScripts: 15,
      activeScripts: 9,
      byType: { formal: 15 },
      byStyle: { humorous: 3, proverb: 3, friendly: 1 },
      bySource: { ai: 15 },
      avgDuration: '70',
      totalTokens: '6103',
    })

    const result = await productApi.scriptUsageList(131)

    expect(mockPost).toHaveBeenCalledWith('/product/script/usage-list', { productId: 131 })
    expect(result.list).toEqual([])
    expect(result.stats).toMatchObject({
      totalScripts: 15,
      activeScripts: 9,
      byType: { formal: 15 },
      byStyle: { humorous: 3, proverb: 3, friendly: 1 },
      bySource: { ai: 15 },
      avgDuration: 70,
      totalTokens: 6103,
    })
  })

  it('scriptUsageList still accepts wrapped GMV contribution rows', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            liveTitle: '直播场次 A',
            revenue: '68000',
            orderCount: '42',
            liveSessionId: '8',
            versionName: 'v1 -> v2',
            liftPct: '18',
          },
        ],
      },
    })

    const result = await productApi.scriptUsageList(1)

    expect(result.list).toEqual([
      expect.objectContaining({
        sessionTitle: '直播场次 A',
        totalGmv: 68000,
        totalOrders: 42,
        sessionId: 8,
        versionLabel: 'v1 -> v2',
        gmvLift: 18,
      }),
    ])
    expect(result.stats.totalScripts).toBe(0)
  })

  it('generateMultiStyleScriptsSse reads GET SSE stream with bearer auth', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      [
        'event: progress',
        'data: {"style":"professional","status":"loading","message":"生成中"}',
        '',
        'event: progress',
        'data: {"style":"professional","status":"done","success":true}',
        '',
        'event: done',
        'data: {"status":"ok"}',
        '',
      ].join('\n'),
      { status: 200, headers: { 'Content-Type': 'text/event-stream;charset=UTF-8' } },
    ))
    vi.stubGlobal('fetch', fetchMock)
    const onProgress = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    generateMultiStyleScriptsSse(
      {
        productId: 131,
        styles: ['professional'],
        scriptType: 'formal',
        duration: 60,
        useKbRef: true,
        kbCategories: ['douyin'],
      },
      { onProgress, onDone, onError },
    )

    await vi.waitFor(() => expect(onDone).toHaveBeenCalledTimes(1))

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/product/script/generate-multi-sse?'),
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({
          Accept: 'text/event-stream',
          Authorization: 'Bearer mock-token',
        }),
      }),
    )
    const requestUrl = String(fetchMock.mock.calls[0][0])
    expect(requestUrl).toContain('productId=131')
    expect(requestUrl).toContain('styles=professional')
    expect(requestUrl).toContain('kbCategories=douyin')
    expect(onProgress).toHaveBeenCalledWith(expect.objectContaining({
      style: 'professional',
      status: 'loading',
    }))
    expect(onProgress).toHaveBeenCalledWith(expect.objectContaining({
      style: 'professional',
      status: 'done',
      success: true,
    }))
    expect(onError).not.toHaveBeenCalled()
  })

  it('generateMultiStyleScriptsSse exposes backend SSE error messages', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      'event: error\ndata: {"error":"产品不存在或无权限"}\n\n',
      { status: 200, headers: { 'Content-Type': 'text/event-stream' } },
    ))
    vi.stubGlobal('fetch', fetchMock)
    const onError = vi.fn()

    generateMultiStyleScriptsSse(
      { productId: 404, styles: ['professional'] },
      { onError },
    )

    await vi.waitFor(() => expect(onError).toHaveBeenCalledTimes(1))
    expect(onError.mock.calls[0][0]).toBeInstanceOf(Error)
    expect(onError.mock.calls[0][0].message).toBe('产品不存在或无权限')
  })
})
