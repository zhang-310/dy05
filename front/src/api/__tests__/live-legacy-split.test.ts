import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  batchSaveLiveScripts,
  getScriptApprovalHistory,
  getScriptEffectiveness,
  getScriptVersions,
  getScriptsBySession,
  listScriptComments,
  searchScriptApprovals,
  searchScripts,
  searchTemplates,
} from '../live-script'
import { getProductDataBySession, getProductsBySession, searchProducts } from '../live-product'
import {
  checkPlatformViolation,
  checkSimilarity,
  generateFull,
  generateSkeleton,
  getFormatRoiAnalysis,
  getStylePresets,
  listEffectivenessConfigs,
  listGenerationPresets,
  listPlatforms,
  recommendScripts,
  recommendStyles,
} from '../live-ai'

vi.mock('@/utils/request', () => ({
  default: { post: vi.fn() },
}))

describe('legacy split live SDK normalization', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('normalizes wrapped script arrays used by the legacy workbench', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        records: [
          {
            id: '41',
            sessionId: '18',
            content: '包装话术',
            scriptType: 'product',
            sequenceNo: '2',
            durationLimitSec: '90',
            aiGenerated: '1',
            violationChecked: '0',
          },
        ],
      },
    })
    await expect(getScriptsBySession(18)).resolves.toEqual([
      expect.objectContaining({
        id: 41,
        sessionId: 18,
        scriptContent: '包装话术',
        sequenceNo: 2,
        durationLimitSec: 90,
        aiGenerated: true,
        violationChecked: false,
      }),
    ])
    expect(mockPost).toHaveBeenLastCalledWith('/live/script/by-session', { sessionId: 18 })

    mockPost.mockResolvedValueOnce({ rows: [{ id: '42', sessionId: '18', scriptContent: '保存后话术', score: '86' }] })
    await expect(batchSaveLiveScripts(18, [{ id: 42, scriptContent: '保存后话术' }])).resolves.toEqual([
      expect.objectContaining({ id: 42, effectivenessScore: 86 }),
    ])

    mockPost.mockResolvedValueOnce({ content: [{ id: '43', sessionId: '18', content: '效果话术' }] })
    await expect(getScriptEffectiveness(18)).resolves.toEqual([
      expect.objectContaining({ id: 43, scriptContent: '效果话术' }),
    ])
  })

  it('normalizes wrapped script page, version, approval, template and comment endpoints', async () => {
    mockPost.mockResolvedValueOnce({ records: [{ id: '50', sessionId: '18', content: '搜索话术' }], totalElements: '1' })
    const scripts = await searchScripts({ page: 0, rows: 20, sessionId: 18 })
    expect(scripts.total).toBe(1)
    expect(scripts.list[0]).toMatchObject({ id: 50, scriptContent: '搜索话术' })

    mockPost.mockResolvedValueOnce({ data: { items: [{ id: '7', scriptId: '50', versionNo: '3', content: '版本内容', versionStatus: 'active' }] } })
    await expect(getScriptVersions(50)).resolves.toEqual([
      expect.objectContaining({ id: 7, scriptId: 50, versionNumber: 3, scriptContent: '版本内容', isCurrent: 1 }),
    ])

    mockPost.mockResolvedValueOnce({ rows: [{ id: 1, scriptId: 50, action: 'submit', status: 0 }], total: 1 })
    await expect(searchScriptApprovals({ scriptId: 50, page: 0, rows: 10 })).resolves.toMatchObject({ total: 1, list: [{ id: 1 }] })

    mockPost.mockResolvedValueOnce({ content: [{ id: 2, scriptId: 50, action: 'approve', status: 1 }] })
    await expect(getScriptApprovalHistory(50)).resolves.toEqual([
      expect.objectContaining({ id: 2, action: 'approve' }),
    ])

    mockPost.mockResolvedValueOnce({ list: [{ id: 3, templateName: '成交模板', scriptType: 'closing', content: '下单提醒' }], total: 1 })
    await expect(searchTemplates({ keyword: '成交', page: 0, rows: 10 })).resolves.toMatchObject({ total: 1, list: [{ id: 3 }] })

    mockPost.mockResolvedValueOnce({ data: { records: [{ id: 4, scriptId: 50, content: '需要改一下' }] } })
    await expect(listScriptComments(50)).resolves.toEqual([
      expect.objectContaining({ id: 4, content: '需要改一下' }),
    ])
  })

  it('normalizes wrapped product arrays used by the legacy workbench', async () => {
    mockPost.mockResolvedValueOnce({ rows: [{ id: '51', sessionId: '18', productId: '9001', productName: '修护精华', gmv: '2999.5', sortOrder: '1' }], totalRecords: '1' })
    const page = await searchProducts({ page: 0, rows: 20, sessionId: 18 })
    expect(page.total).toBe(1)
    expect(page.list[0]).toMatchObject({ id: 51, productId: 9001, revenue: 2999.5, position: 1 })

    mockPost.mockResolvedValueOnce({ data: { items: [{ id: '52', sessionId: '18', productId: '9002', productName: '急救面膜', sales: '12' }] } })
    await expect(getProductsBySession(18)).resolves.toEqual([
      expect.objectContaining({ id: 52, productId: 9002, saleQuantity: 12 }),
    ])

    mockPost.mockResolvedValueOnce({ content: [{ productId: 9001, gmv: 1999 }] })
    await expect(getProductDataBySession(18)).resolves.toEqual([
      expect.objectContaining({ productId: 9001, gmv: 1999 }),
    ])
  })

  it('normalizes wrapped live AI arrays across recommendation, platform and config endpoints', async () => {
    mockPost.mockResolvedValueOnce({ data: { rows: [{ content: '整场话术', scriptType: 'opening' }] } })
    await expect(generateFull(18, 'professional')).resolves.toEqual([
      expect.objectContaining({ content: '整场话术', scriptType: 'opening' }),
    ])

    mockPost.mockResolvedValueOnce({ records: [{ scriptId1: 1, scriptId2: 2, similarityLevel: 'high', suggestion: '拆分表达' }] })
    await expect(checkSimilarity(18)).resolves.toEqual([
      expect.objectContaining({ scriptId1: 1, scriptId2: 2 }),
    ])

    mockPost.mockResolvedValueOnce({ data: { list: [{ scriptId: 41, scriptType: 'product', summary: '讲卖点', suggestedDurationSec: 60 }] } })
    await expect(generateSkeleton(18)).resolves.toEqual([
      expect.objectContaining({ scriptId: 41, summary: '讲卖点' }),
    ])

    mockPost.mockResolvedValueOnce({ items: [{ styleCode: 'professional', usageCount: 4 }] })
    await expect(recommendStyles({ productId: 9001 })).resolves.toEqual([
      expect.objectContaining({ styleCode: 'professional' }),
    ])

    mockPost.mockResolvedValueOnce({ content: [{ label: '专业', styles: [{ value: 'professional', label: '专业' }] }] })
    await expect(getStylePresets()).resolves.toEqual([
      expect.objectContaining({ label: '专业' }),
    ])

    mockPost.mockResolvedValueOnce({ data: { records: [{ id: 1, platformCode: 'douyin', platformName: '抖音' }] } })
    await expect(listPlatforms()).resolves.toEqual([
      expect.objectContaining({ platformCode: 'douyin' }),
    ])

    mockPost.mockResolvedValueOnce({ rows: [{ word: '禁用词', position: 3, level: 'high' }] })
    await expect(checkPlatformViolation('禁用词测试', 'douyin')).resolves.toEqual([
      expect.objectContaining({ word: '禁用词' }),
    ])

    mockPost.mockResolvedValueOnce({ data: { items: [{ id: 1, name: '专业预设', style: 'professional' }] } })
    await expect(listGenerationPresets()).resolves.toEqual([
      expect.objectContaining({ name: '专业预设' }),
    ])

    mockPost.mockResolvedValueOnce({ records: [{ id: 2, configName: '默认权重', viewerWeight: 0.3 }] })
    await expect(listEffectivenessConfigs()).resolves.toEqual([
      expect.objectContaining({ configName: '默认权重' }),
    ])

    mockPost.mockResolvedValueOnce({ content: [{ liveFormat: 'content_commerce', sessionCount: 8 }] })
    await expect(getFormatRoiAnalysis({ days: 7 })).resolves.toEqual([
      expect.objectContaining({ liveFormat: 'content_commerce' }),
    ])

    mockPost.mockResolvedValueOnce({ data: { records: [{ scriptId: 41, sourceType: 'live_script', recommendScore: 93 }] } })
    await expect(recommendScripts({ sessionId: 18, topN: 5 })).resolves.toEqual([
      expect.objectContaining({ scriptId: 41, recommendScore: 93 }),
    ])
  })
})
