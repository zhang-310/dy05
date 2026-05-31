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
    mockPost.mockResolvedValue({ data: { records: [{ id: '1', ownerId: '6', accountName: '美妆号', accountId: 'beauty001', totalFans: '1200', totalVideos: '3' }], totalElements: 1 } })
    await expect(douyinApi.accountList({ page: 0, rows: 20, accountName: '美妆号', accountId: 'beauty001', status: 1 }))
      .resolves.toEqual(expect.objectContaining({
        total: 1,
        list: [expect.objectContaining({ id: 1, userId: 6, accountName: '美妆号', fanCount: 1200, videoCount: 3 })],
      }))
    expect(mockPost).toHaveBeenCalledWith('/douyin/account/search', {
      page: 0,
      rows: 20,
      accountName: '美妆号',
      accountId: 'beauty001',
      status: 1,
    })
  })

  it('normalizes wrapped persona list payloads', async () => {
    mockPost.mockResolvedValue({ data: { rows: [{ id: 2, personaName: '护肤专家' }] } })
    await expect(douyinApi.personaList()).resolves.toEqual([
      expect.objectContaining({ id: 2, personaName: '护肤专家' }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/douyin/persona/list', undefined, { params: { personaType: undefined } })
  })

  it('account detail endpoints use request params expected by backend controllers', async () => {
    mockPost.mockResolvedValueOnce({ data: { id: '7', ownerId: '2', accountName: '账号详情', accountId: 'acc-7', fanCount: '100' } })
    await expect(douyinApi.accountGet(7)).resolves.toEqual(expect.objectContaining({
      id: 7,
      userId: 2,
      accountName: '账号详情',
      fanCount: 100,
    }))
    mockPost.mockResolvedValueOnce({ data: { accountId: '7', videoCount: '8', viewCount: '9000', likeCount: '600', averageViews: '1125' } })
    await expect(douyinApi.accountStats(7)).resolves.toEqual(expect.objectContaining({
      accountId: '7',
      totalVideos: 8,
      totalViews: 9000,
      totalLikes: 600,
      avgViewsPerVideo: 1125,
    }))
    mockPost.mockResolvedValueOnce({})
    await douyinApi.accountDelete(7)
    expect(mockPost).toHaveBeenCalledWith('/douyin/account/get', undefined, { params: { id: 7 } })
    expect(mockPost).toHaveBeenCalledWith('/douyin/account/statistics', undefined, { params: { id: 7 } })
    expect(mockPost).toHaveBeenCalledWith('/douyin/account/delete', undefined, { params: { id: 7 } })
  })

  it('personaGetByAccount posts accountId payload', async () => {
    mockPost.mockResolvedValue({ data: { id: 9, personaName: '包装人设' } })
    await expect(douyinApi.personaGetByAccount(8)).resolves.toEqual(
      expect.objectContaining({ id: 9, personaName: '包装人设' }),
    )
    expect(mockPost).toHaveBeenCalledWith('/douyin/persona/get-by-account', { accountId: 8 })
  })

  it('persona id endpoints use request params expected by backend controllers', async () => {
    mockPost.mockResolvedValue(undefined)
    await douyinApi.personaGet(9)
    await douyinApi.personaDelete(9)
    await douyinApi.personaSetDefault(9)
    expect(mockPost).toHaveBeenCalledWith('/douyin/persona/get', undefined, { params: { id: 9 } })
    expect(mockPost).toHaveBeenCalledWith('/douyin/persona/delete', undefined, { params: { id: 9 } })
    expect(mockPost).toHaveBeenCalledWith('/douyin/persona/set-default', undefined, { params: { id: 9 } })
  })

  it('fanProfileSync uses sync path with account id', async () => {
    mockPost.mockResolvedValue(undefined)
    await douyinApi.fanProfileSync(6)
    expect(mockPost).toHaveBeenCalledWith('/douyin/fan-profile/sync/6', {})
  })

  it('normalizes wrapped fan profile and stat payloads', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        accountId: 6,
        accountName: '美妆号',
        totalFans: '125000',
        genderDistribution: [
          { key: 'female', value: '女', percentage: '82.5' },
          { key: 'male', value: '男', percentage: 17.5 },
        ],
        ageDistribution: [{ key: '25-34', value: '25-34', percentage: 45 }],
        provinceDistribution: { records: [{ key: 'GD', value: '广东', percentage: 31 }] },
        syncTime: 1770000000000,
      },
    })
    await expect(douyinApi.fanProfileGet(6)).resolves.toEqual(expect.objectContaining({
      accountId: 6,
      accountName: '美妆号',
      totalFans: 125000,
      genderDistribution: [
        expect.objectContaining({ value: '女', percentage: 82.5 }),
        expect.objectContaining({ value: '男', percentage: 17.5 }),
      ],
      provinceDistribution: [expect.objectContaining({ value: '广东', percentage: 31 })],
    }))
    expect(mockPost).toHaveBeenCalledWith('/douyin/fan-profile/get', { accountId: 6 })

    mockPost.mockResolvedValueOnce({
      data: {
        rows: [
          { statType: 'age', statKey: '25-34', statValue: '25-34岁', percentage: '46' },
          { ageRange: '35-44', ratio: 30 },
        ],
      },
    })
    await expect(douyinApi.fanProfileStats(6)).resolves.toEqual([
      expect.objectContaining({ statType: 'age', statValue: '25-34岁', percentage: 46, ratio: 46 }),
      expect.objectContaining({ ageRange: '35-44', ratio: 30 }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/douyin/fan-profile/stats', { accountId: 6 })
  })

  it('video id endpoints use request params expected by backend controllers', async () => {
    mockPost.mockResolvedValueOnce({ data: { id: '12', accountId: '5', itemId: 'v-12', title: '包装视频', playCount: '1234', type: 'normal' } })
    await expect(douyinApi.videoGet(12)).resolves.toEqual(expect.objectContaining({
      id: 12,
      accountId: 5,
      videoId: 'v-12',
      title: '包装视频',
      viewCount: 1234,
      videoType: 'normal',
    }))
    mockPost.mockResolvedValueOnce(undefined)
    await douyinApi.videoSync(12)
    expect(mockPost).toHaveBeenCalledWith('/douyin/video/get', undefined, { params: { id: 12 } })
    expect(mockPost).toHaveBeenCalledWith('/douyin/video/sync', undefined, { params: { accountId: 12 } })
  })

  it('videoSearch posts only backend supported search fields', async () => {
    mockPost.mockResolvedValue({ data: { content: [{ id: 12, itemId: 'item-12', title: '爆款', playCount: '88000' }], totalCount: 1 } })
    await expect(douyinApi.videoSearch({ page: 0, rows: 20, accountId: 5, title: '爆款', videoType: 'normal' }))
      .resolves.toEqual(expect.objectContaining({
        total: 1,
        list: [expect.objectContaining({ id: 12, videoId: 'item-12', title: '爆款', viewCount: 88000 })],
      }))
    expect(mockPost).toHaveBeenCalledWith('/douyin/video/search', {
      page: 0,
      rows: 20,
      accountId: 5,
      title: '爆款',
      videoType: 'normal',
    })
  })

  it('oauthUrl posts account id payload', async () => {
    mockPost.mockResolvedValue({ data: { url: 'https://example.com', state: 's-1' } })
    await expect(douyinApi.oauthUrl(3)).resolves.toEqual({ authUrl: 'https://example.com', state: 's-1' })
    expect(mockPost).toHaveBeenCalledWith('/douyin/oauth/auth-url', { accountId: 3 })
  })

  it('normalizes wrapped token status payloads', async () => {
    mockPost.mockResolvedValue({ data: { status: 'valid', expiresAt: '2026-06-01 10:00:00', remainingDays: '10' } })
    await expect(douyinApi.tokenStatus(3)).resolves.toEqual({
      status: 'valid',
      expireTime: '2026-06-01 10:00:00',
      daysLeft: 10,
    })
    expect(mockPost).toHaveBeenCalledWith('/douyin/oauth/token-status', { accountId: 3 })
  })
})
