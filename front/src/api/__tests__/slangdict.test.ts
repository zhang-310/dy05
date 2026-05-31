import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { slangApi } from '../slangdict'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('slangdict API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts slang query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await slangApi.list({ page: 0, rows: 20, keyword: '拉新' })
    expect(mockPost).toHaveBeenCalledWith('/slangdict/entry/search', { page: 0, rows: 20, keyword: '拉新' })
  })

  it('list normalizes wrapped slang entry pages', async () => {
    mockPost.mockResolvedValue({
      payload: {
        slangEntries: [
          { id: 9, phrase: '种草', meaning: '内容激发购买兴趣' },
        ],
        totalRecords: '1',
        page: '2',
        rows: '50',
      },
    })

    await expect(slangApi.list({ page: 2, rows: 50, keyword: '种草' })).resolves.toEqual({
      total: 1,
      list: [{ id: 9, phrase: '种草', meaning: '内容激发购买兴趣' }],
      pageNum: 2,
      pageSize: 50,
    })
  })

  it('save posts slang save payload', async () => {
    mockPost.mockResolvedValue(1)
    await slangApi.save({ phrase: '拉新', meaning: '新增用户', category: '运营' })
    expect(mockPost).toHaveBeenCalledWith('/slangdict/entry/save', {
      phrase: '拉新',
      meaning: '新增用户',
      category: '运营',
    })
  })

  it('delete posts slang id request param', async () => {
    mockPost.mockResolvedValue(undefined)
    await slangApi.delete(5)
    expect(mockPost).toHaveBeenCalledWith('/slangdict/entry/delete?id=5', {})
  })
})
