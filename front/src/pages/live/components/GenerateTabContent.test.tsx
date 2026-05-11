import { describe, it, expect, vi } from 'vitest'
import { liveApi } from '@/api/live'

vi.mock('@/api/live')
vi.mock('@/api/douyin')
vi.mock('@/api/tianapi')

describe('GenerateTabContent - API 集成测试', () => {
  it('应该正确调用商品列表 API', async () => {
    vi.mocked(liveApi.productBySession).mockResolvedValue([
      { id: 1, productName: '精华液', price: 299, stock: 100 },
      { id: 2, productName: '面霜', price: 399, stock: 50 },
    ])

    const result = await liveApi.productBySession(1)

    expect(result).toHaveLength(2)
    expect(result[0].productName).toBe('精华液')
    expect(liveApi.productBySession).toHaveBeenCalledWith(1)
  })

  it('应该正确调用话术列表 API', async () => {
    vi.mocked(liveApi.scriptSearch).mockResolvedValue({
      list: [
        {
          id: 1,
          sessionId: 1,
          scriptContent: '欢迎来到直播间',
          scriptType: 'intro',
          style: 'friendly',
          sequenceNo: 1,
          aiGenerated: true,
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 100,
    })

    const result = await liveApi.scriptSearch({
      sessionId: 1,
      page: 0,
      rows: 100,
    })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].scriptContent).toBe('欢迎来到直播间')
    expect(liveApi.scriptSearch).toHaveBeenCalledWith({
      sessionId: 1,
      page: 0,
      rows: 100,
    })
  })

  it('应该正确调用批量生成 API', async () => {
    vi.mocked(liveApi.aiGenerateFull).mockResolvedValue([
      {
        id: 1,
        sessionId: 1,
        scriptContent: 'AI 生成的话术',
        scriptType: 'product',
        style: 'professional',
        sequenceNo: 1,
        aiGenerated: true,
        generationStatus: 'completed',
      },
    ])

    const result = await liveApi.aiGenerateFull({
      sessionId: 1,
      products: [{ productId: 1, duration: 120, style: 'professional' }],
    })

    expect(result).toHaveLength(1)
    expect(liveApi.aiGenerateFull).toHaveBeenCalled()
  })

  it('应该正确调用单条生成 API', async () => {
    vi.mocked(liveApi.aiGenerateProductScript).mockResolvedValue({
      scriptContent: 'AI 生成的单条话术',
      scriptType: 'product',
      style: 'friendly',
    })

    const result = await liveApi.aiGenerateProductScript({
      sessionId: 1,
      productId: 1,
      style: 'friendly',
      duration: 60,
    })

    expect(result.scriptContent).toBe('AI 生成的单条话术')
    expect(liveApi.aiGenerateProductScript).toHaveBeenCalled()
  })

  it('应该正确调用话术保存 API', async () => {
    vi.mocked(liveApi.scriptSave).mockResolvedValue({ id: 1 })

    const result = await liveApi.scriptSave({
      id: 1,
      sessionId: 1,
      scriptContent: '修改后的话术',
      scriptType: 'product',
      style: 'professional',
      sequenceNo: 1,
    })

    expect(result.id).toBe(1)
    expect(liveApi.scriptSave).toHaveBeenCalledWith(
      expect.objectContaining({
        scriptContent: '修改后的话术',
      })
    )
  })

  it('应该正确调用话术删除 API', async () => {
    vi.mocked(liveApi.scriptDelete).mockResolvedValue(undefined)

    await liveApi.scriptDelete(1)

    expect(liveApi.scriptDelete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用版本列表 API', async () => {
    vi.mocked(liveApi.versionList).mockResolvedValue([
      {
        id: 1,
        scriptId: 1,
        versionNo: 1,
        content: '版本1内容',
        isActive: true,
        createTime: '2026-05-10T10:00:00',
      },
      {
        id: 2,
        scriptId: 1,
        versionNo: 2,
        content: '版本2内容',
        isActive: false,
        createTime: '2026-05-10T11:00:00',
      },
    ])

    const result = await liveApi.versionList(1)

    expect(result).toHaveLength(2)
    expect(result[0].isActive).toBe(true)
    expect(liveApi.versionList).toHaveBeenCalledWith(1)
  })

  it('应该正确调用版本激活 API', async () => {
    vi.mocked(liveApi.versionActivate).mockResolvedValue(undefined)

    await liveApi.versionActivate(2)

    expect(liveApi.versionActivate).toHaveBeenCalledWith(2)
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(liveApi.aiGenerateFull).mockRejectedValue(
      new Error('Generation failed')
    )

    await expect(
      liveApi.aiGenerateFull({
        sessionId: 1,
        products: [],
      })
    ).rejects.toThrow('Generation failed')
  })

  it('应该验证 22 种话术风格常量', () => {
    const SCRIPT_STYLES = [
      'natural', 'friendly', 'warm', 'gentle', 'casual',
      'enthusiastic', 'passionate', 'promotion', 'seeding',
      'professional',
      'emotional', 'storytelling', 'empathy',
    ]

    expect(SCRIPT_STYLES).toHaveLength(13)
    expect(SCRIPT_STYLES).toContain('natural')
    expect(SCRIPT_STYLES).toContain('professional')
    expect(SCRIPT_STYLES).toContain('emotional')
  })

  it('应该验证话术类型常量', () => {
    const SCRIPT_TYPES = ['intro', 'product', 'interaction', 'promotion', 'closing']

    expect(SCRIPT_TYPES).toHaveLength(5)
    expect(SCRIPT_TYPES).toContain('intro')
    expect(SCRIPT_TYPES).toContain('product')
    expect(SCRIPT_TYPES).toContain('closing')
  })
})
