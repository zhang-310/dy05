import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  complianceCheck,
  scriptComplianceCheck,
  scriptComplianceIndustryCodes,
  scriptComplianceRules,
  douyinOfficialReferences,
} from '../compliance'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('compliance API', () => {
  const mockPost = vi.mocked(request.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts common compliance check with backend content fields', async () => {
    mockPost.mockResolvedValue({ result: 'pass', riskScore: 0, matchedRules: [] })

    await complianceCheck('安全话术', 'script')

    expect(mockPost).toHaveBeenCalledWith('/compliance/check', {
      content: '安全话术',
      contentType: 'script',
    })
  })

  it('posts industry script compliance check and normalizes violations', async () => {
    mockPost.mockResolvedValue([
      {
        matchedText: '全网最好',
        level: 'error',
        reason: '绝对化用语',
        reference: '广告法',
        position: 3,
        source: 'industry',
      },
    ])

    const result = await scriptComplianceCheck({ text: '这个全网最好', industryCode: 'cosmetics' })

    expect(mockPost).toHaveBeenCalledWith('/script/compliance/check', {
      text: '这个全网最好',
      industryCode: 'cosmetics',
    })
    expect(result[0]).toMatchObject({
      matchedText: '全网最好',
      level: 'error',
      reason: '绝对化用语',
      reference: '广告法',
      position: 3,
    })
  })

  it('loads script compliance metadata endpoints', async () => {
    mockPost
      .mockResolvedValueOnce([{ pattern: '最好', level: 'error', reason: '绝对化', reference: '广告法' }])
      .mockResolvedValueOnce({ verticalCodes: ['cosmetics'], defaultVerticalCode: 'cosmetics', note: '叠加通用规则' })
      .mockResolvedValueOnce({ notice: '维护入口', referenceUrls: ['https://example.com/rule'], hint: '只返回配置' })

    await expect(scriptComplianceRules('cosmetics')).resolves.toEqual([
      { pattern: '最好', level: 'error', reason: '绝对化', reference: '广告法', source: undefined },
    ])
    await expect(scriptComplianceIndustryCodes()).resolves.toEqual({
      verticalCodes: ['cosmetics'],
      defaultVerticalCode: 'cosmetics',
      note: '叠加通用规则',
    })
    await expect(douyinOfficialReferences()).resolves.toEqual({
      notice: '维护入口',
      referenceUrls: ['https://example.com/rule'],
      hint: '只返回配置',
    })
  })

  it('normalizes wrapped script compliance arrays and metadata', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ word: '第一', severity: 'high', matchReason: '绝对化', punishment: '广告法', position: '1' }] } })
      .mockResolvedValueOnce({ data: { items: [{ pattern: '最好', level: 'error', reason: '绝对化', reference: '广告法' }] } })
      .mockResolvedValueOnce({ data: { codes: ['food', 'general'], defaultVerticalCode: 'food' } })
      .mockResolvedValueOnce({ data: { urls: ['https://example.com/rule'] } })

    await expect(scriptComplianceCheck({ text: '第一', industryCode: 'food' })).resolves.toEqual([
      expect.objectContaining({ matchedText: '第一', level: 'high', reason: '绝对化', reference: '广告法', position: 1 }),
    ])
    await expect(scriptComplianceRules('food')).resolves.toEqual([
      expect.objectContaining({ pattern: '最好', level: 'error' }),
    ])
    await expect(scriptComplianceIndustryCodes()).resolves.toMatchObject({
      verticalCodes: ['food', 'general'],
      defaultVerticalCode: 'food',
    })
    await expect(douyinOfficialReferences()).resolves.toMatchObject({
      referenceUrls: ['https://example.com/rule'],
    })
  })
})
