import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  diffProductScriptVersions,
  getCurrentProductScriptVersion,
  getProductScriptVersion,
  getProductScriptVersionHistory,
  ensureProductScriptOptimizationVersion,
  listProductScriptVersions,
  recommendProductScriptVersions,
  rollbackProductScriptVersion,
  searchProductScriptVersions,
  updateProductScriptVersionStatus,
} from '../product-script-version'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('product script version API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('search uses request params contract and normalizes legacy fields', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [
        {
          id: 3,
          productId: 1,
          versionNumber: 2,
          scriptContent: '旧字段内容',
          score: 88,
          status: 'active',
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })

    const result = await searchProductScriptVersions({ keyword: '精华', style: 'warm', minScore: 70, page: 0, rows: 20 })

    expect(mockPost).toHaveBeenCalledWith('/product/script-version/search', undefined, {
      params: {
        keyword: '精华',
        style: 'warm',
        minScore: 70,
        page: 0,
        rows: 20,
      },
    })
    expect(result.list[0]).toMatchObject({
      content: '旧字段内容',
      scriptContent: '旧字段内容',
      isActive: true,
      effectivenessScore: 88,
      createdAt: '2026-05-21 10:00:00',
    })
  })

  it('list posts ProductScriptVersionSearchVO body', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 50 })

    await listProductScriptVersions({ productId: 9, isActive: true, minScore: 80, rows: 50 })

    expect(mockPost).toHaveBeenCalledWith('/product/script-version/list', {
      productId: 9,
      keyword: undefined,
      style: undefined,
      isActive: true,
      isRecommended: undefined,
      archived: undefined,
      minEffectivenessScore: 80,
      page: 0,
      rows: 50,
    })
  })

  it('detail best history recommend and status use real backend paths', async () => {
    mockPost.mockResolvedValue({ id: 1, productId: 9, versionNumber: 1, content: '内容', isActive: false })

    await getCurrentProductScriptVersion(9)
    await getProductScriptVersion(1)
    await getProductScriptVersionHistory(9)
    await ensureProductScriptOptimizationVersion(8)
    await recommendProductScriptVersions(9, 'warm', 3)
    await updateProductScriptVersionStatus(1, true)

    expect(mockPost).toHaveBeenCalledWith('/product/script-version/best', undefined, { params: { productId: 9 } })
    expect(mockPost).toHaveBeenCalledWith('/product/script-version/detail/1')
    expect(mockPost).toHaveBeenCalledWith('/product/script-version/list-by-product', undefined, { params: { productId: 9 } })
    expect(mockPost).toHaveBeenCalledWith('/product/script-version/ensure-optimization-version', { scriptId: 8, forceNew: false })
    expect(mockPost).toHaveBeenCalledWith('/product/script-version/recommend', undefined, { params: { productId: 9, topN: 3 } })
    expect(mockPost).toHaveBeenCalledWith('/product/script-version/update-status', undefined, { params: { id: 1, isActive: true } })
  })

  it('history normalizes wrapped rows and backend alias fields', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            versionId: '31',
            productId: '9',
            productScriptId: '8',
            versionNo: '5',
            scriptContent: '包装后的版本内容',
            styleCode: 'warm',
            rating: '91.5',
            useCount: '7',
            convertRate: '13.2',
            active: 'true',
            recommended: 'false',
            createTime: '2026-05-22 10:00:00',
          },
        ],
      },
    })

    const result = await getProductScriptVersionHistory(9)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({
      id: 31,
      productId: 9,
      scriptId: 8,
      versionNumber: 5,
      content: '包装后的版本内容',
      effectivenessScore: 91.5,
      usageCount: 7,
      conversionRate: 13.2,
      isActive: true,
      isRecommended: false,
      createdAt: '2026-05-22 10:00:00',
    })
  })

  it('recommend normalizes wrapped version rows and fallback scores', async () => {
    mockPost.mockResolvedValue({
      data: {
        versions: [
          {
            scriptVersionId: '41',
            version: '6',
            style: 'professional',
            score: '88',
            usedCount: '12',
            conversion: '9.5',
            recommend_score: '82.4',
          },
        ],
      },
    })

    const result = await recommendProductScriptVersions(9, undefined, 1)

    expect(result.versions?.[0]).toMatchObject({
      id: 41,
      versionNumber: 6,
      style: 'professional',
      effectivenessScore: 88,
      usageCount: 12,
      conversionRate: 9.5,
      recommendScore: 82.4,
    })
    expect(result.scores).toEqual([82.4])
  })

  it('unsupported diff and rollback reject explicitly instead of calling fake endpoints', async () => {
    await expect(diffProductScriptVersions()).rejects.toThrow(/暂无商品话术版本 diff 接口/)
    await expect(rollbackProductScriptVersion()).rejects.toThrow(/暂无按 productId/)
    expect(mockPost).not.toHaveBeenCalled()
  })
})
