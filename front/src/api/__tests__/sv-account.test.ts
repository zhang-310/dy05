import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  accountAnalytics,
  accountDelete,
  accountGet,
  accountList,
  accountRefreshStats,
  accountUpdate,
  accountVideos,
} from '../sv-account'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('sv-account API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uses real short-video account backend endpoints', async () => {
    const params = { page: 0, rows: 20, keyword: '护肤', sortName: 'updateTime', sortOrder: 'desc' }
    mockPost.mockResolvedValueOnce({ total: 0, list: [], pageNum: 0, pageSize: 20 })
    mockPost.mockResolvedValueOnce({ id: 7, secUid: 'sec-7' })
    mockPost.mockResolvedValue(undefined)

    await accountList(params)
    await accountGet(7)
    await accountUpdate({ id: 7, notes: '重点账号' })
    await accountDelete(7)
    await accountRefreshStats(7)

    expect(mockPost).toHaveBeenCalledWith('/short-video/account/list', params)
    expect(mockPost).toHaveBeenCalledWith('/short-video/account/get', { id: 7 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/account/update', { id: 7, notes: '重点账号' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/account/delete', { id: 7 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/account/refresh-stats', { id: 7 })
  })

  it('normalizes wrapped account list and detail aliases', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        records: [
          {
            accountId: '7',
            sec_uid: 'sec-7',
            uniqueId: 'skin-ops',
            nickName: '护肤实验室',
            followers: '125000',
            followings: '12',
            totalLikeCount: '500000',
            worksCount: '88',
            verified: 'true',
            collectionCount: '3',
            viralVideoCount: '2',
            averageViewCount: '22000',
            averageLikeCount: '1300',
            averageViralScore: '82.6',
            maxViralScore: '96.3',
            category: '美妆',
            source: 'keyword_search',
            keyword: '精华液',
            createdAt: '2026-05-01 10:00:00',
            updatedAt: '2026-05-10 10:00:00',
          },
        ],
        totalRecords: '1',
        page: '0',
        size: '20',
      },
    })
    mockPost.mockResolvedValueOnce({
      data: {
        id: '7',
        secUid: 'sec-7',
        collectTaskCount: '3',
        deepPendingCount: '1',
        deepCompletedCount: '2',
        lastTaskId: '88',
      },
    })

    const page = await accountList({ page: 0, rows: 20 })
    const detail = await accountGet(7)

    expect(page).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [
        {
          id: 7,
          secUid: 'sec-7',
          douyinId: 'skin-ops',
          nickname: '护肤实验室',
          followerCount: 125000,
          isVerified: true,
          totalCollectedVideos: 2,
          avgViralScore: 82.6,
          topViralScore: 96.3,
          sourceType: 'keyword_search',
          sourceKeyword: '精华液',
        },
      ],
    })
    expect(detail).toMatchObject({
      id: 7,
      taskCount: 3,
      pendingAnalysisCount: 1,
      analyzedCount: 2,
      latestTaskId: 88,
    })
  })

  it('normalizes wrapped account videos and analytics aliases', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        items: [
          {
            viralVideoId: '101',
            desc: '精华液三秒开场',
            cover: 'https://cdn.test/cover.jpg',
            playUrl: 'https://douyin.test/video/101',
            awemeId: 'aweme-101',
            nickname: '护肤实验室',
            playCount: '31000',
            diggCount: '2100',
            shareCount: '130',
            score: '91',
            analysisStatus: 'completed',
            analysisProgress: '已拆解',
            publishedAt: '2026-05-11 09:00:00',
            createdAt: '2026-05-11 10:00:00',
          },
        ],
        totalCount: '1',
      },
    })
    mockPost.mockResolvedValueOnce({
      data: {
        totalVideoCount: '2',
        totalViewCount: '62000',
        totalLikeCount: '4200',
        totalShareCount: '260',
        avgViewCount: '31000',
        avgLikeCount: '2100',
        avgShareCount: '130',
        averageViralScore: '91.5',
        topViewCount: '45000',
        minViewCount: '17000',
        pendingAnalysisCount: '1',
        processingAnalysisCount: '0',
        analyzedCount: '1',
        failedAnalysisCount: '0',
        otherAnalysisCount: '0',
      },
    })

    const videos = await accountVideos({ accountId: 7, page: 0, rows: 20 })
    const analytics = await accountAnalytics(7)

    expect(videos).toMatchObject({
      total: 1,
      list: [
        {
          id: 101,
          title: '精华液三秒开场',
          coverUrl: 'https://cdn.test/cover.jpg',
          videoUrl: 'https://douyin.test/video/101',
          douyinVideoId: 'aweme-101',
          authorName: '护肤实验室',
          viewCount: 31000,
          likeCount: 2100,
          shareCount: 130,
          viralScore: 91,
          deepAnalyzeStatus: 'completed',
          deepAnalyzeProgress: '已拆解',
        },
      ],
    })
    expect(analytics).toEqual({
      videoCount: 2,
      sumViewCount: 62000,
      sumLikeCount: 4200,
      sumShareCount: 260,
      avgViewPerVideo: 31000,
      avgLikePerVideo: 2100,
      avgSharePerVideo: 130,
      avgViralScore: 91.5,
      maxViewCount: 45000,
      minViewCount: 17000,
      deepPendingCount: 1,
      deepProcessingCount: 0,
      deepCompletedCount: 1,
      deepFailedCount: 0,
      deepOtherCount: 0,
    })
  })

  it('normalizes detail and analytics from non-data wrappers', async () => {
    mockPost.mockResolvedValueOnce({
      record: {
        accountId: '9',
        sec_uid: 'sec-9',
        nickName: '包装账号详情',
        collectTaskCount: '4',
        deepPendingCount: '2',
        deepCompletedCount: '1',
      },
    })
    mockPost.mockResolvedValueOnce({
      result: {
        totalVideoCount: '3',
        totalViewCount: '9000',
        totalLikeCount: '600',
        totalShareCount: '90',
        avgViewCount: '3000',
        avgLikeCount: '200',
        avgShareCount: '30',
        averageViralScore: '88',
        topViewCount: '5000',
        minViewCount: '1000',
        pendingAnalysisCount: '1',
        processingAnalysisCount: '1',
        analyzedCount: '1',
        failedAnalysisCount: '0',
      },
    })

    await expect(accountGet(9)).resolves.toEqual(expect.objectContaining({
      id: 9,
      secUid: 'sec-9',
      nickname: '包装账号详情',
      taskCount: 4,
      pendingAnalysisCount: 2,
      analyzedCount: 1,
    }))
    await expect(accountAnalytics(9)).resolves.toEqual(expect.objectContaining({
      videoCount: 3,
      sumViewCount: 9000,
      avgViewPerVideo: 3000,
      deepProcessingCount: 1,
    }))
  })
})
