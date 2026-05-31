import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { copyApi } from '../copy'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('copy API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts copy library query', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await copyApi.list({ page: 0, rows: 20, keyword: '面膜' })
    expect(mockPost).toHaveBeenCalledWith('/copy/library/search', { page: 0, rows: 20, keyword: '面膜' })
  })

  it('id endpoints use request params expected by backend controllers', async () => {
    mockPost.mockResolvedValue(undefined)
    await copyApi.get(9)
    await copyApi.delete(9)
    await copyApi.updateStatus(9, 1)
    await copyApi.incrementUseCount(9)
    expect(mockPost).toHaveBeenCalledWith('/copy/library/get', undefined, { params: { id: 9 } })
    expect(mockPost).toHaveBeenCalledWith('/copy/library/delete', undefined, { params: { id: 9 } })
    expect(mockPost).toHaveBeenCalledWith('/copy/library/update-status', undefined, { params: { id: 9, status: 1 } })
    expect(mockPost).toHaveBeenCalledWith('/copy/library/increment-use-count', undefined, { params: { id: 9 } })
  })

  it('approvalApprove reads detail and updates through save endpoint', async () => {
    mockPost.mockResolvedValueOnce({ id: 9, copyId: 88, approvalStatus: 2 })
    mockPost.mockResolvedValueOnce(undefined)
    await copyApi.approvalApprove(9, '通过')
    expect(mockPost).toHaveBeenNthCalledWith(1, '/copy/approval/get', { id: 9 })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/copy/approval/save', {
      id: 9,
      copyId: 88,
      approvalStatus: 1,
      comments: '通过',
    })
  })

  it('template list maps templateName to backend keyword and normalizes templateContent', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [{ id: 1, templateName: '开场', templateContent: '欢迎 {productName}', category: '开场白', status: 1 }],
    })
    const result = await copyApi.templateList({ page: 0, rows: 20, templateName: '开场' })
    expect(mockPost).toHaveBeenCalledWith('/copy/template/search', { page: 0, rows: 20, keyword: '开场' })
    expect(result.list[0].content).toBe('欢迎 {productName}')
    expect(result.list[0].variables).toBe('productName')
  })

  it('template save maps frontend content to backend templateContent and strips derived variables', async () => {
    mockPost.mockResolvedValue(1)
    await copyApi.templateSave({
      templateName: '促单模板',
      content: '现在下单享 {discount}',
      variables: 'discount',
      category: '促销话术',
    })
    expect(mockPost).toHaveBeenCalledWith('/copy/template/save', {
      templateName: '促单模板',
      templateContent: '现在下单享 {discount}',
      category: '促销话术',
    })
  })

  it('semanticSearch degrades to keyword search because backend vector endpoint is not present', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await copyApi.semanticSearch('护肤卖点', { page: 1, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/copy/library/search', {
      keyword: '护肤卖点',
      page: 1,
      rows: 10,
    })
  })

  it('aiGenerate calls real copy AI endpoint and normalizes generated candidates', async () => {
    mockPost
      .mockResolvedValueOnce('姐妹们，换季干燥先看这瓶补水精华。')
      .mockResolvedValueOnce('补水精华第二版，主打轻薄吸收和妆前不搓泥。')

    const result = await copyApi.aiGenerate({
      prompt: '补水精华',
      category: '护肤',
      copyType: '商品介绍',
      style: '专业科学',
      keywords: '补水,精华',
      duration: '30s',
      count: 2,
    })

    expect(mockPost).toHaveBeenNthCalledWith(1, '/copy/ai/generate', {
      topic: '补水精华 / 商品介绍 / 护肤',
      category: '护肤',
      style: 'professional',
      keywords: '补水,精华',
      personaId: undefined,
      length: 1,
    })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/copy/ai/generate', {
      topic: '补水精华 / 商品介绍 / 护肤（第 2 个差异化版本）',
      category: '护肤',
      style: 'professional',
      keywords: '补水,精华',
      personaId: undefined,
      length: 1,
    })
    expect(result).toEqual([
      expect.objectContaining({
        title: '姐妹们，换季干燥先看这瓶补水精华。',
        content: '姐妹们，换季干燥先看这瓶补水精华。',
        category: '护肤',
        tags: '商品介绍,补水,精华',
        status: 0,
        source: 'copy_ai',
      }),
      expect.objectContaining({
        title: '补水精华第二版，主打轻薄吸收和妆前不搓泥。',
        content: '补水精华第二版，主打轻薄吸收和妆前不搓泥。',
      }),
    ])
  })

  it('unsupported copy capabilities fail locally with explicit degradation message', async () => {
    await expect(copyApi.batchTag([1, 2], ['促销'])).rejects.toThrow('后端未提供文案批量打标签接口')
    await expect(copyApi.exportCsv({})).rejects.toThrow('后端未提供文案 CSV 导出接口')
    await expect(copyApi.usageList(1)).rejects.toThrow('后端未提供文案使用明细接口')
    await expect(copyApi.approvalStats()).rejects.toThrow('后端未提供文案审批统计接口')
  })
})
