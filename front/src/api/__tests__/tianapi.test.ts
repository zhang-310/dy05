import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { tianapi } from '../tianapi'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('tianapi API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('hotDouyin posts without payload', async () => {
    mockPost.mockResolvedValue([])
    await tianapi.hotDouyin()
    expect(mockPost).toHaveBeenCalledWith('/tianapi/hot/douyin')
  })

  it('hotWeibo posts without payload', async () => {
    mockPost.mockResolvedValue([])
    await tianapi.hotWeibo()
    expect(mockPost).toHaveBeenCalledWith('/tianapi/hot/weibo')
  })

  it('status posts without payload', async () => {
    mockPost.mockResolvedValue({ enabled: true })
    await tianapi.status()
    expect(mockPost).toHaveBeenCalledWith('/tianapi/status')
  })

  it('normalizes wrapped hot list and status payloads', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ title: '包装热点', hotValue: '12,300', rank: 1 }] } })
      .mockResolvedValueOnce({ data: { configured: true } })

    const list = await tianapi.hotDouyin()
    const status = await tianapi.status()

    expect(list[0]).toMatchObject({ word: '包装热点', hotIndex: 12300, position: 1 })
    expect(status.enabled).toBe(true)
  })
})
