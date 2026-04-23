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
    await slangApi.list({ page: 0, rows: 20, term: '拉新' })
    expect(mockPost).toHaveBeenCalledWith('/slangdict/list', { page: 0, rows: 20, term: '拉新' })
  })

  it('save posts slang save payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await slangApi.save({ term: '拉新', definition: '新增用户', category: '运营' })
    expect(mockPost).toHaveBeenCalledWith('/slangdict/save', {
      term: '拉新',
      definition: '新增用户',
      category: '运营',
    })
  })

  it('disable posts slang id payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await slangApi.disable(5)
    expect(mockPost).toHaveBeenCalledWith('/slangdict/disable', { id: 5 })
  })
})
