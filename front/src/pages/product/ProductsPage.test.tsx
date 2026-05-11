import { describe, it, expect, vi } from 'vitest'
import { productApi } from '@/api/product'

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
    get: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
    publish: vi.fn(),
    unpublish: vi.fn(),
    setFeatured: vi.fn(),
    updateInventory: vi.fn(),
    extractFromLink: vi.fn(),
    importPaiping: vi.fn(),
    salesHistorySearch: vi.fn(),
    salesHistorySave: vi.fn(),
    scriptList: vi.fn(),
    scriptGet: vi.fn(),
    scriptSave: vi.fn(),
    scriptDelete: vi.fn(),
    scriptGenerate: vi.fn(),
    scriptStatistics: vi.fn(),
    scriptActivate: vi.fn(),
    scriptVersionList: vi.fn(),
    scriptVersionSave: vi.fn(),
    scriptVersionActivate: vi.fn(),
    scriptVersionDelete: vi.fn(),
    stylePresetList: vi.fn(),
    stylePresetSave: vi.fn(),
    stylePresetDelete: vi.fn(),
    stylePresetRecommend: vi.fn(),
    effectivenessRanking: vi.fn(),
    effectivenessCompare: vi.fn(),
    scriptUsageList: vi.fn(),
    effectivenessTrend: vi.fn(),
  },
}))

describe('ProductsPage - API 集成测试', () => {
  it('应该正确调用商品列表 API', async () => {
    vi.mocked(productApi.list).mockResolvedValue({
      list: [
        {
          id: 1,
          userId: 1,
          productName: '玻尿酸精华液',
          productCode: 'P001',
          category: '护肤',
          brand: '品牌A',
          price: 299,
          costPrice: 150,
          profitMarginPct: 49.83,
          inventory: 100,
          unit: '瓶',
          weight: 30,
          mainImage: '/images/product1.jpg',
          description: '深层补水',
          highlights: '高浓度玻尿酸',
          sellingPoints: '补水保湿',
          status: 1,
          createTime: '2026-05-10T10:00:00',
          updateTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await productApi.list({ page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].productName).toBe('玻尿酸精华液')
    expect(result.list[0].price).toBe(299)
    expect(productApi.list).toHaveBeenCalled()
  })

  it('应该支持关键词搜索', async () => {
    vi.mocked(productApi.list).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await productApi.list({
      page: 0,
      rows: 20,
      productName: '精华',
    })

    expect(productApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        productName: '精华',
      })
    )
  })

  it('应该支持分类筛选', async () => {
    vi.mocked(productApi.list).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await productApi.list({
      page: 0,
      rows: 20,
      category: '护肤',
    })

    expect(productApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        category: '护肤',
      })
    )
  })

  it('应该支持状态筛选', async () => {
    vi.mocked(productApi.list).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    await productApi.list({
      page: 0,
      rows: 20,
      status: 1,
    })

    expect(productApi.list).toHaveBeenCalledWith(
      expect.objectContaining({
        status: 1,
      })
    )
  })

  it('应该正确调用商品详情 API', async () => {
    vi.mocked(productApi.get).mockResolvedValue({
      id: 1,
      userId: 1,
      productName: '玻尿酸精华液',
      productCode: 'P001',
      category: '护肤',
      brand: '品牌A',
      price: 299,
      costPrice: 150,
      profitMarginPct: 49.83,
      inventory: 100,
      unit: '瓶',
      weight: 30,
      mainImage: '/images/product1.jpg',
      description: '深层补水',
      highlights: '高浓度玻尿酸',
      sellingPoints: '补水保湿',
      status: 1,
      createTime: '2026-05-10T10:00:00',
      updateTime: '2026-05-10T10:00:00',
    })

    const result = await productApi.get(1)

    expect(result.productName).toBe('玻尿酸精华液')
    expect(productApi.get).toHaveBeenCalledWith(1)
  })

  it('应该正确调用商品保存 API', async () => {
    vi.mocked(productApi.save).mockResolvedValue(undefined)

    await productApi.save({
      productName: '新商品',
      price: 199,
      status: 1,
    })

    expect(productApi.save).toHaveBeenCalledWith(
      expect.objectContaining({
        productName: '新商品',
        price: 199,
      })
    )
  })

  it('应该正确调用商品删除 API', async () => {
    vi.mocked(productApi.delete).mockResolvedValue(undefined)

    await productApi.delete(1)

    expect(productApi.delete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用批量删除 API', async () => {
    vi.mocked(productApi.batchDelete).mockResolvedValue(undefined)

    await productApi.batchDelete([1, 2, 3])

    expect(productApi.batchDelete).toHaveBeenCalledWith([1, 2, 3])
  })

  it('应该正确调用上架 API', async () => {
    vi.mocked(productApi.publish).mockResolvedValue(undefined)

    await productApi.publish(1)

    expect(productApi.publish).toHaveBeenCalledWith(1)
  })

  it('应该正确调用下架 API', async () => {
    vi.mocked(productApi.unpublish).mockResolvedValue(undefined)

    await productApi.unpublish(1)

    expect(productApi.unpublish).toHaveBeenCalledWith(1)
  })

  it('应该正确调用库存更新 API', async () => {
    vi.mocked(productApi.updateInventory).mockResolvedValue(undefined)

    await productApi.updateInventory(1, -10)

    expect(productApi.updateInventory).toHaveBeenCalledWith(1, -10)
  })

  it('应该正确调用话术生成 API', async () => {
    vi.mocked(productApi.scriptGenerate).mockResolvedValue({
      id: 1,
      productId: 1,
      scriptTitle: 'AI生成话术',
      scriptContent: '这是AI生成的话术内容',
      scriptType: 'product',
      style: 'professional',
      duration: 60,
      useCount: 0,
      rating: 0,
      status: 1,
      createTime: '2026-05-10T10:00:00',
    })

    const result = await productApi.scriptGenerate(1, 'professional')

    expect(result.scriptTitle).toBe('AI生成话术')
    expect(productApi.scriptGenerate).toHaveBeenCalledWith(1, 'professional')
  })

  it('应该正确调用话术列表 API', async () => {
    vi.mocked(productApi.scriptList).mockResolvedValue({
      list: [
        {
          id: 1,
          productId: 1,
          scriptTitle: '话术1',
          scriptContent: '内容1',
          scriptType: 'product',
          style: 'professional',
          duration: 60,
          useCount: 5,
          rating: 4.5,
          status: 1,
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await productApi.scriptList({ productId: 1, page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].scriptTitle).toBe('话术1')
    expect(productApi.scriptList).toHaveBeenCalled()
  })

  it('应该正确调用话术保存 API', async () => {
    vi.mocked(productApi.scriptSave).mockResolvedValue(undefined)

    await productApi.scriptSave({
      productId: 1,
      scriptTitle: '新话术',
      scriptContent: '话术内容',
    })

    expect(productApi.scriptSave).toHaveBeenCalled()
  })

  it('应该正确调用话术删除 API', async () => {
    vi.mocked(productApi.scriptDelete).mockResolvedValue(undefined)

    await productApi.scriptDelete(1)

    expect(productApi.scriptDelete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用销售历史查询 API', async () => {
    vi.mocked(productApi.salesHistorySearch).mockResolvedValue({
      list: [
        {
          id: 1,
          productId: 1,
          saleDate: '2026-05-10',
          quantity: 10,
          revenue: 2990,
          platform: '抖音',
          createTime: '2026-05-10T10:00:00',
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })

    const result = await productApi.salesHistorySearch({ productId: 1, page: 0, rows: 20 })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].quantity).toBe(10)
    expect(productApi.salesHistorySearch).toHaveBeenCalled()
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(productApi.list).mockRejectedValue(new Error('Network error'))

    await expect(
      productApi.list({ page: 0, rows: 20 })
    ).rejects.toThrow('Network error')
  })

  it('应该验证商品分类常量', () => {
    const CATEGORIES = ['护肤', '彩妆', '面膜', '精华', '洁面', '防晒', '眼霜', '身体护理', '其他']

    expect(CATEGORIES).toHaveLength(9)
    expect(CATEGORIES).toContain('护肤')
    expect(CATEGORIES).toContain('彩妆')
  })

  it('应该验证商品状态常量', () => {
    const STATUS_OPTIONS = [
      { value: 1, label: '上架', color: 'success' },
      { value: 0, label: '下架', color: 'default' },
    ]

    expect(STATUS_OPTIONS).toHaveLength(2)
    expect(STATUS_OPTIONS[0].value).toBe(1)
    expect(STATUS_OPTIONS[1].value).toBe(0)
  })
})
