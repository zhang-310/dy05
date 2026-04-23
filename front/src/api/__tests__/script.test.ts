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
    await scriptApi.list({ page: 0, rows: 10, keyword: '护肤' })
    expect(mockPost).toHaveBeenCalledWith('/script/list', { page: 0, rows: 10, keyword: '护肤' })
  })

  it('generate posts script generation payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await scriptApi.generate({ topic: '面膜转化话术', scriptType: 'product' })
    expect(mockPost).toHaveBeenCalledWith('/script/generate', {
      topic: '面膜转化话术',
      scriptType: 'product',
    })
  })

  it('templateByScene posts scene payload', async () => {
    mockPost.mockResolvedValue([])
    await scriptApi.templateByScene('live')
    expect(mockPost).toHaveBeenCalledWith('/script/template/by-scene', { scene: 'live' })
  })

  it('violationCheck wraps text payload', async () => {
    mockPost.mockResolvedValue({ violations: [] })
    await scriptApi.violationCheck('绝对有效')
    expect(mockPost).toHaveBeenCalledWith('/script/violation/check', { text: '绝对有效' })
  })

  it('violationCheckBatch posts texts array', async () => {
    mockPost.mockResolvedValue([])
    await scriptApi.violationCheckBatch(['话术1', '话术2'])
    expect(mockPost).toHaveBeenCalledWith('/script/violation/check-batch', { texts: ['话术1', '话术2'] })
  })

  it('searchSuggest posts keyword payload', async () => {
    mockPost.mockResolvedValue([])
    await scriptApi.searchSuggest('面膜')
    expect(mockPost).toHaveBeenCalledWith('/script/search/suggest', { keyword: '面膜' })
  })
})
