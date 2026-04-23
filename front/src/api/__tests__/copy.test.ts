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

  it('approvalApprove posts id and comment payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await copyApi.approvalApprove(9, '通过')
    expect(mockPost).toHaveBeenCalledWith('/copy/approval/approve', { id: 9, comment: '通过' })
  })

  it('batchTag posts selected ids and tags', async () => {
    mockPost.mockResolvedValue(undefined)
    await copyApi.batchTag([1, 2], ['促销', '直播'])
    expect(mockPost).toHaveBeenCalledWith('/copy/library/batch-tag', {
      ids: [1, 2],
      tags: ['促销', '直播'],
    })
  })

  it('semanticSearch wraps query with paging params', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await copyApi.semanticSearch('护肤卖点', { page: 1, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/copy/library/semantic-search', {
      query: '护肤卖点',
      page: 1,
      rows: 10,
    })
  })

  it('approvalStats posts empty payload', async () => {
    mockPost.mockResolvedValue({})
    await copyApi.approvalStats()
    expect(mockPost).toHaveBeenCalledWith('/copy/approval/stats', {})
  })
})
