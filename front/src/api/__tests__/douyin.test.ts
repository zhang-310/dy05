import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { douyinApi } from '../douyin'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('douyin API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('accountList posts account query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await douyinApi.accountList({ page: 0, rows: 20, accountName: '美妆号' })
    expect(mockPost).toHaveBeenCalledWith('/douyin/account/search', {
      page: 0,
      rows: 20,
      accountName: '美妆号',
    })
  })

  it('personaGetByAccount posts accountId payload', async () => {
    mockPost.mockResolvedValue(null)
    await douyinApi.personaGetByAccount(8)
    expect(mockPost).toHaveBeenCalledWith('/douyin/persona/get-by-account', { accountId: 8 })
  })

  it('fanProfileSync uses sync path with account id', async () => {
    mockPost.mockResolvedValue(undefined)
    await douyinApi.fanProfileSync(6)
    expect(mockPost).toHaveBeenCalledWith('/douyin/fan-profile/sync/6', {})
  })

  it('oauthUrl posts account id payload', async () => {
    mockPost.mockResolvedValue({ authUrl: 'https://example.com' })
    await douyinApi.oauthUrl(3)
    expect(mockPost).toHaveBeenCalledWith('/douyin/oauth/auth-url', { accountId: 3 })
  })
})
