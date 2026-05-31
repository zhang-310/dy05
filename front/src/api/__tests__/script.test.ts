import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { scriptApi } from '../script'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('script API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts script search payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await scriptApi.list({ page: 0, rows: 10, keyword: '护肤', scriptType: 'manual', industry: '敏感肌' })
    expect(mockPost).toHaveBeenCalledWith('/script/list', { page: 0, rows: 10, keyword: '护肤', category: '敏感肌', source: 'manual' })
  })

  it('list normalizes wrapped script page and backend field aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            scriptId: '8',
            ownerId: '3',
            scriptTitle: '包装话术',
            scriptContent: '包装返回的话术内容',
            scene: '敏感肌',
            type: 'manual',
            usageCount: '9',
            createdAt: '2026-05-22',
          },
        ],
        totalElements: '12',
        page: '2',
        size: '20',
      },
    })

    const res = await scriptApi.list({ page: 2, rows: 20 })

    expect(res.total).toBe(12)
    expect(res.pageNum).toBe(2)
    expect(res.pageSize).toBe(20)
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 8,
      userId: 3,
      title: '包装话术',
      content: '包装返回的话术内容',
      category: '敏感肌',
      industry: '敏感肌',
      source: 'manual',
      scriptType: 'manual',
      useCount: 9,
      createTime: '2026-05-22',
    }))
  })

  it('save posts only real script library fields with source and category mapping', async () => {
    mockPost.mockResolvedValue(undefined)

    await scriptApi.save({
      id: 9,
      title: '保存话术',
      content: '真实保存内容',
      scriptType: 'manual',
      industry: '敏感肌',
      duration: 60,
      tags: '修护',
      status: 1,
    })

    expect(mockPost).toHaveBeenCalledWith('/script/save', {
      id: 9,
      title: '保存话术',
      content: '真实保存内容',
      category: '敏感肌',
      tags: '修护',
      source: 'manual',
      status: 1,
    })
  })

  it('generate posts script generation payload', async () => {
    mockPost.mockResolvedValue({ id: 1, variants: [] })
    const res = await scriptApi.generate({
      productName: '修护面膜',
      productPrice: 99,
      keyFeatures: ['修护屏障'],
      duration: 60,
      style: 'professional',
      variants: 3,
    })
    expect(res).toEqual({ id: 1, variants: [], generationTime: undefined })
    expect(mockPost).toHaveBeenCalledWith('/script/generate', {
      productName: '修护面膜',
      productPrice: 99,
      keyFeatures: ['修护屏障'],
      duration: 60,
      style: 'professional',
      variants: 3,
    })
  })

  it('generate normalizes wrapped variants and score aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        generationId: '18',
        generationTimeMs: '1200',
        variantList: [
          {
            variantId: 7,
            scriptContent: '包装返回的话术',
            qualityScore: '9.1',
            summary: '强 CTA',
            useCount: '3',
          },
        ],
      },
    })

    const res = await scriptApi.generate({
      productName: '修护面膜',
      productPrice: 99,
      keyFeatures: ['修护屏障'],
      duration: 60,
      style: 'professional',
      variants: 1,
    })

    expect(res.id).toBe(18)
    expect(res.generationTime).toBe(1200)
    expect(res.variants[0]).toEqual(expect.objectContaining({
      id: '7',
      content: '包装返回的话术',
      score: 9.1,
      keyPoints: '强 CTA',
      uses: 3,
    }))
  })

  it('optimize posts real script optimization payload and normalizes result aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        result: '优化后话术',
        beforeScore: '6.5',
        afterScore: '8.7',
        recommendations: '强化开场；补行动号召',
        elapsedMs: '980',
        style: 'urgent',
      },
    })

    const res = await scriptApi.optimize({
      originalContent: '原始话术',
      goal: '增强紧迫感',
      style: 'urgent',
    })

    expect(mockPost).toHaveBeenCalledWith('/script/optimize', {
      originalContent: '原始话术',
      goal: '增强紧迫感',
      style: 'urgent',
    })
    expect(res).toEqual(expect.objectContaining({
      optimizedContent: '优化后话术',
      originalScore: 6.5,
      optimizedScore: 8.7,
      suggestions: ['强化开场', '补行动号召'],
      generationTime: 980,
      style: 'urgent',
    }))
  })

  it('optimize removes blank optional fields', async () => {
    mockPost.mockResolvedValue({ optimizedContent: '优化后话术', suggestions: [] })

    await scriptApi.optimize({
      originalContent: '原始话术',
      goal: '',
      style: undefined,
    })

    expect(mockPost).toHaveBeenCalledWith('/script/optimize', {
      originalContent: '原始话术',
    })
  })

  it('templateByScene posts scene payload', async () => {
    mockPost.mockResolvedValue([])
    await scriptApi.templateByScene('live')
    expect(mockPost).toHaveBeenCalledWith('/script/template/by-scene', { scene: 'live' })
  })

  it('templateSave maps legacy templateContent to backend content field', async () => {
    mockPost.mockResolvedValue(12)
    await scriptApi.templateSave({
      id: 12,
      templateName: '开场模板',
      templateContent: '欢迎来到直播间',
      scene: 'opening',
      tags: '产品名,卖点',
      status: 1,
    })
    expect(mockPost).toHaveBeenCalledWith('/script/template/save', {
      id: 12,
      templateName: '开场模板',
      templateType: 'user',
      scene: 'opening',
      content: '欢迎来到直播间',
      description: '产品名,卖点',
      userId: undefined,
      status: 1,
    })
  })

  it('violationCheck wraps text payload', async () => {
    mockPost.mockResolvedValue({ data: { score: '88', violations: [{ word: '绝对', level: '1', replacement: '较为' }] } })
    const res = await scriptApi.violationCheck('绝对有效')
    expect(mockPost).toHaveBeenCalledWith('/script/violation/check', { text: '绝对有效' })
    expect(res.score).toBe(88)
    expect(res.violations[0]).toEqual(expect.objectContaining({ word: '绝对', level: 1, replacement: '较为' }))
  })

  it('violationList maps reason and level to backend search fields', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 })

    await scriptApi.violationList({
      page: 0,
      rows: 20,
      word: '根治',
      category: '医疗功效宣称',
      severity: 3,
    })

    expect(mockPost).toHaveBeenCalledWith('/script/admin/violation/list', {
      page: 0,
      rows: 20,
      keyword: '根治',
      level: 3,
      reason: '医疗功效宣称',
    })
  })

  it('violationSave posts only real violation fields', async () => {
    mockPost.mockResolvedValue(undefined)

    await scriptApi.violationSave({
      id: 2,
      word: '根治',
      severity: 3,
      category: '医疗功效宣称',
      scope: 'live_only',
      replacement: '改善',
      status: 1,
    })

    expect(mockPost).toHaveBeenCalledWith('/script/admin/violation/save', {
      id: 2,
      word: '根治',
      level: 3,
      reason: '医疗功效宣称',
      scope: 'live_only',
      replacement: '改善',
      status: 1,
    })
  })

  it('violationCheck sends scope when provided', async () => {
    mockPost.mockResolvedValue({ violations: [] })
    await scriptApi.violationCheck('绝对有效', 'live')
    expect(mockPost).toHaveBeenCalledWith('/script/violation/check', { text: '绝对有效', scope: 'live' })
  })

  it('violationCheckBatch posts texts array', async () => {
    mockPost.mockResolvedValue([])
    await scriptApi.violationCheckBatch(['话术1', '话术2'])
    expect(mockPost).toHaveBeenCalledWith('/script/violation/check-batch', {
      texts: [
        { key: '1', text: '话术1' },
        { key: '2', text: '话术2' },
      ],
      scope: 'all',
    })
  })

  it('violationCheckBatch normalizes highest backend level as maxSeverity', async () => {
    mockPost.mockResolvedValue({
      results: {
        'script-1': {
          totalCount: 2,
          violations: [
            { word: '绝对', level: 1 },
            { word: '根治', level: 3 },
          ],
        },
      },
      totalViolations: 2,
    })

    const rows = await scriptApi.violationCheckBatch([{ key: 'script-1', text: '绝对根治' }], 'live')

    expect(mockPost).toHaveBeenCalledWith('/script/violation/check-batch', {
      texts: [{ key: 'script-1', text: '绝对根治' }],
      scope: 'live',
    })
    expect(rows).toEqual([
      expect.objectContaining({
        id: 0,
        title: 'script-1',
        count: 2,
        maxSeverity: 3,
        status: '需修改',
      }),
    ])
  })

  it('searchSuggest posts keyword payload', async () => {
    mockPost.mockResolvedValue([])
    await scriptApi.searchSuggest('面膜')
    expect(mockPost).toHaveBeenCalledWith('/script/search/suggest', { prefix: '面膜', limit: 10 })
  })

  it('searchHybrid posts script search weights and normalizes backend list', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [
        {
          scriptId: 9,
          title: '屏障修护',
          content: '修护屏障话术',
          vectorScore: 0.9,
          lexicalScore: 0.4,
          hybridScore: 0.83,
        },
      ],
    })
    const res = await scriptApi.searchHybrid({ query: '屏障', mode: 'semantic', rows: 20 })
    expect(mockPost).toHaveBeenCalledWith('/script/search/hybrid', {
      query: '屏障',
      page: 0,
      rows: 20,
      topK: 20,
      category: undefined,
      style: undefined,
      vectorWeight: 1,
      lexicalWeight: 0,
      withCrossEncoder: false,
    })
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 9,
      scriptId: 9,
      title: '屏障修护',
      score: 0.83,
      source: 'script',
    }))
  })

  it('searchHybrid normalizes wrapped records and total aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            id: 12,
            title: '包装结果',
            content: '来自 records 的话术',
            score: 0.71,
            ownerUserId: 6,
          },
        ],
        totalElements: 8,
        page: 1,
        size: 20,
        searchTime: '123',
        executedAt: '2026-05-22T23:59:00',
      },
    })

    const res = await scriptApi.searchHybrid({ query: '包装', rows: 20 })

    expect(res.total).toBe(8)
    expect(res.pageNum).toBe(1)
    expect(res.pageSize).toBe(20)
    expect(res.searchTime).toBe(123)
    expect(res.executedAt).toBe('2026-05-22T23:59:00')
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 12,
      scriptId: 12,
      title: '包装结果',
      userId: 6,
      score: 0.71,
      hybridScore: 0.71,
      vectorScore: 0,
      lexicalScore: 0,
    }))
  })
})
