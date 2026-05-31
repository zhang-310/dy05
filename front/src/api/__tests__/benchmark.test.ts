import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  benchmarkAccountApi,
  benchmarkAnalysisApi,
  benchmarkVideoApi,
  douyinCookieApi,
  benchmarkQualityScriptApi,
  benchmarkScriptSimilarityApi,
  benchmarkScriptRecommendationApi,
} from '../benchmark'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('benchmark API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('normalizes account pages and maps legacy follower filters to backend fan fields', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            id: 7,
            accountName: '护肤竞品号',
            fanCount: '120000',
            avgLikeCount: 5800,
            category: '护肤',
            isActive: 'true',
          },
        ],
        totalElements: 1,
      },
    })

    const res = await benchmarkAccountApi.list({ page: 0, rows: 30, minFollowerCount: 50000 })

    expect(mockPost).toHaveBeenCalledWith('/benchmark/account/list', {
      page: 0,
      rows: 30,
      minFanCount: 50000,
    })
    expect(res.total).toBe(1)
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 7,
      accountName: '护肤竞品号',
      fanCount: 120000,
      followerCount: 120000,
      avgLikeCount: 5800,
      category: '护肤',
      isActive: true,
    }))
  })

  it('normalizes account pages wrapped by result accounts payloads', async () => {
    mockPost.mockResolvedValue({
      status: 200,
      result: {
        accounts: [
          {
            accountId: '21',
            nickname: '包装账号',
            followerCount: '78000',
            profileUrl: 'https://www.douyin.com/user/wrapped',
            sec_uid: 'sec-wrapped',
            active: 1,
          },
        ],
        totalCount: 3,
      },
    })

    const res = await benchmarkAccountApi.list({ page: 0, rows: 30 })

    expect(res.total).toBe(3)
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 21,
      accountName: '包装账号',
      fanCount: 78000,
      followerCount: 78000,
      accountUrl: 'https://www.douyin.com/user/wrapped',
      secUid: 'sec-wrapped',
      isActive: true,
    }))
  })

  it('normalizes account save and keyword search payloads to backend VO names', async () => {
    mockPost.mockResolvedValueOnce({ id: 8, accountName: '手动账号', fanCount: 99000 })
    await expect(benchmarkAccountApi.save({
      accountName: '手动账号',
      accountUrl: 'https://www.douyin.com/user/abc',
      secUid: 'abc',
      followerCount: 99000,
      avatarUrl: 'ignored',
      description: 'ignored',
      tags: 'ignored',
    })).resolves.toEqual(expect.objectContaining({ fanCount: 99000, followerCount: 99000 }))

    expect(mockPost).toHaveBeenLastCalledWith('/benchmark/account/save', {
      accountName: '手动账号',
      platform: 'douyin',
      accountUrl: 'https://www.douyin.com/user/abc',
      secUid: 'abc',
      fanCount: 99000,
    })

    mockPost.mockResolvedValueOnce([{ id: 9, accountName: '搜索账号', fanCount: 51000 }])
    await benchmarkAccountApi.searchByKeyword({ keyword: '护肤', minFollowerCount: 50000, cookieId: 3 })
    expect(mockPost).toHaveBeenLastCalledWith('/benchmark/account/search-by-keyword', {
      keyword: '护肤',
      minFanCount: 50000,
      cookieId: 3,
    })
  })

  it('normalizes video pages and archive field aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        rows: [
          {
            id: 11,
            benchmarkAccountId: 7,
            videoId: 'v11',
            videoUrl: 'https://www.douyin.com/video/11',
            viewCount: '88000',
            likeCount: '24000',
            favoriteCount: 90,
            isQualified: 1,
            localVideoPath: 'temp/videos/v11.mp4',
            bosVideoUrl: 'https://bos.test/v11.mp4',
            analysisStatus: 'completed',
          },
        ],
        count: 1,
      },
    })

    const res = await benchmarkVideoApi.list({ page: 0, rows: 30, benchmarkAccountId: 7 })

    expect(mockPost).toHaveBeenCalledWith('/benchmark/video/list', {
      page: 0,
      rows: 30,
      benchmarkAccountId: 7,
    })
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 11,
      viewCount: 88000,
      likeCount: 24000,
      favoriteCount: 90,
      collectCount: 90,
      isQualified: true,
      localPath: 'temp/videos/v11.mp4',
      bosUrl: 'https://bos.test/v11.mp4',
    }))
  })

  it('normalizes video pages wrapped by detail videos payloads', async () => {
    mockPost.mockResolvedValue({
      detail: {
        videos: [
          {
            videoPk: '31',
            accountId: '21',
            awemeId: 'aweme-31',
            awemeUrl: 'https://www.douyin.com/video/31',
            playCount: '99000',
            diggCount: '4500',
            collectCount: '88',
            qualified: 'true',
            localPath: 'local/v31.mp4',
            bosUrl: 'https://bos.test/v31.mp4',
          },
        ],
        totalRecords: 5,
      },
    })

    const res = await benchmarkVideoApi.list({ page: 0, rows: 30 })

    expect(res.total).toBe(5)
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 31,
      benchmarkAccountId: 21,
      videoId: 'aweme-31',
      videoUrl: 'https://www.douyin.com/video/31',
      viewCount: 99000,
      likeCount: 4500,
      favoriteCount: 88,
      collectCount: 88,
      isQualified: true,
      localVideoPath: 'local/v31.mp4',
      bosVideoUrl: 'https://bos.test/v31.mp4',
    }))
  })

  it('normalizes analysis responses and posts optional cookie id only when present', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        analysisId: 18,
        benchmarkVideoId: 11,
        summary: '适合复刻',
        tokensUsed: '1200',
      },
    })

    await expect(benchmarkAnalysisApi.analyze({
      benchmarkVideoId: 11,
      enableAsr: true,
      enableOcr: false,
      enableApi: true,
      cookieId: 2,
    })).resolves.toEqual(expect.objectContaining({
      id: 18,
      benchmarkVideoId: 11,
      aiSummary: '适合复刻',
      tokensUsed: 1200,
    }))

    expect(mockPost).toHaveBeenCalledWith('/benchmark/analysis/analyze', {
      benchmarkVideoId: 11,
      enableAsr: true,
      enableOcr: false,
      enableApi: true,
      cookieId: 2,
    })
  })

  it('normalizes analysis detail wrappers and key frame array aliases', async () => {
    mockPost.mockResolvedValue({
      payload: {
        analysis: {
          analysisId: '28',
          videoId: '31',
          summary: '包装分析',
          keyFrames: [
            { url: 'https://cdn.test/frame-a.jpg', time: 1.5 },
            { frameUrl: 'https://cdn.test/frame-b.jpg', timestamp: '3.2' },
          ],
          tokensUsed: '3000',
        },
      },
    })

    const res = await benchmarkAnalysisApi.getByVideo(31)

    expect(res).toEqual(expect.objectContaining({
      id: 28,
      benchmarkVideoId: 31,
      aiSummary: '包装分析',
      tokensUsed: 3000,
    }))
    expect(res.keyFramesJson).toBe(JSON.stringify([
      { url: 'https://cdn.test/frame-a.jpg', time: 1.5 },
      { frameUrl: 'https://cdn.test/frame-b.jpg', timestamp: '3.2' },
    ]))
  })

  it('quality script search posts paging payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await benchmarkQualityScriptApi.search({ page: 0, rows: 20, keyword: '护肤' } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/quality-script/search', {
      page: 0,
      rows: 20,
      keyword: '护肤',
    })
  })

  it('normalizes wrapped quality script pages and field aliases', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            scriptId: 9,
            benchmarkVideoId: 100,
            benchmarkAnalysisId: 200,
            content: '包装质量脚本',
            type: '种草',
            category: '护肤',
            scene: '短视频',
            score: 88.5,
            interactionRate: 12.3,
            viewCount: 10000,
            likeCount: 1200,
            createdAt: '2026-05-22 10:00:00',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 20,
      },
    })

    const res = await benchmarkQualityScriptApi.search({ page: 0, rows: 20 } as never)

    expect(res.total).toBe(1)
    expect(res.list[0]).toEqual(expect.objectContaining({
      id: 9,
      videoId: 100,
      analysisId: 200,
      scriptContent: '包装质量脚本',
      scriptType: '种草',
      industry: '护肤',
      sceneType: '短视频',
      qualityScore: 88.5,
      engagementRate: 12.3,
      viewsCount: 10000,
      likesCount: 1200,
    }))
  })

  it('normalizes quality score and saved script responses', async () => {
    mockPost.mockResolvedValueOnce({ data: { score: 86.25 } })
    await expect(benchmarkQualityScriptApi.calculateQualityScore({
      videoId: 1,
      analysisId: 2,
      scriptContent: '脚本',
      qualityScore: 50,
    } as never)).resolves.toBe(86.25)

    mockPost.mockResolvedValueOnce({
      data: {
        id: 18,
        videoId: 1,
        analysisId: 2,
        scriptContent: '已保存脚本',
        qualityScore: 86.25,
      },
    })
    await expect(benchmarkQualityScriptApi.save({
      videoId: 1,
      analysisId: 2,
      scriptContent: '已保存脚本',
      qualityScore: 86.25,
    } as never)).resolves.toEqual(expect.objectContaining({
      id: 18,
      scriptContent: '已保存脚本',
      qualityScore: 86.25,
    }))
  })

  it('generateEmbedding posts script id payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await benchmarkScriptSimilarityApi.generateEmbedding(5)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-similarity/generate-embedding', { scriptId: 5 })
  })

  it('findSimilarByText posts similarity query payload', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            id: 33,
            benchmarkVideoId: 44,
            content: '相似脚本正文',
            score: 88.5,
            similarity: '0.82',
            interactionRate: '12.3',
            playCount: '30000',
          },
        ],
      },
    })
    const res = await benchmarkScriptSimilarityApi.findSimilarByText({
      text: '护肤品直播话术',
      topK: 10,
      minScore: 0.7,
    } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-similarity/find-similar-by-text', {
      text: '护肤品直播话术',
      topK: 10,
      minScore: 0.7,
    })
    expect(res[0]).toEqual(expect.objectContaining({
      scriptId: 33,
      videoId: 44,
      scriptContent: '相似脚本正文',
      qualityScore: 88.5,
      similarityScore: 0.82,
      engagementRate: 12.3,
      viewsCount: 30000,
    }))
  })

  it('recommendByRequirement posts requirement payload', async () => {
    mockPost.mockResolvedValue({
      data: {
        items: [
          {
            qualityScriptId: 77,
            scriptContent: '推荐脚本',
            scriptQualityScore: '92',
            matchScore: '0.91',
            category: '护肤',
            scene: '直播',
          },
        ],
      },
    })
    const res = await benchmarkScriptRecommendationApi.recommendByRequirement({
      requirement: '提高护肤品直播转化',
      topK: 5,
    } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-recommendation/recommend-by-requirement', {
      requirement: '提高护肤品直播转化',
      topK: 5,
    })
    expect(res[0]).toEqual(expect.objectContaining({
      scriptId: 77,
      scriptContent: '推荐脚本',
      qualityScore: 92,
      similarityScore: 0.91,
      industry: '护肤',
      sceneType: '直播',
    }))
  })

  it('recommendImprovementScripts posts analysis id payload', async () => {
    mockPost.mockResolvedValue({ data: [] })
    await benchmarkScriptRecommendationApi.recommendImprovementScripts({
      analysisId: 99,
      topK: 3,
    } as never)
    expect(mockPost).toHaveBeenCalledWith('/benchmark/script-recommendation/recommend-improvement-scripts', {
      analysisId: 99,
      topK: 3,
    })
  })

  it('normalizes similarity numeric responses and script id arrays', async () => {
    mockPost.mockResolvedValueOnce({ data: { value: '0.76' } })
    await expect(benchmarkScriptSimilarityApi.calculateSimilarity(1, 2)).resolves.toBe(0.76)

    mockPost.mockResolvedValueOnce({ data: { rows: ['1', 2, 'bad', 3] } })
    await expect(benchmarkScriptSimilarityApi.getUnembeddedScripts(50)).resolves.toEqual([1, 2, 3])
    expect(mockPost).toHaveBeenLastCalledWith('/benchmark/script-similarity/get-unembedded-scripts', { limit: 50 })

    mockPost.mockResolvedValueOnce({ records: [4, '5'] })
    await expect(benchmarkScriptSimilarityApi.getUnindexedScripts()).resolves.toEqual([4, 5])
  })

  it('douyinCookieApi.list normalizes wrapped page payloads', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            id: 8,
            cookieName: '包装 Cookie',
            cookieValue: 'a=b',
            isValid: true,
            useCount: 2,
          },
        ],
        totalElements: 6,
        page: 0,
        size: 30,
      },
    })

    const res = await douyinCookieApi.list({ page: 0, rows: 30 } as never)

    expect(mockPost).toHaveBeenCalledWith('/benchmark/cookie/list', { page: 0, rows: 30 })
    expect(res.total).toBe(6)
    expect(res.list[0]).toEqual(expect.objectContaining({ id: 8, cookieName: '包装 Cookie', isValid: true }))
  })

  it('douyinCookieApi normalizes cookie aliases, validate results and QR payloads', async () => {
    mockPost.mockResolvedValueOnce({
      result: {
        record: {
          cookieId: '18',
          userId: '2',
          name: '扫码 Cookie',
          header: 'sid=abc',
          status: 'valid',
          use_count: '9',
          last_validate_time: '2026-05-22 09:00:00',
          createdAt: '2026-05-22 08:00:00',
        },
      },
    })
    await expect(douyinCookieApi.get(18)).resolves.toEqual(expect.objectContaining({
      id: 18,
      ownerId: 2,
      cookieName: '扫码 Cookie',
      cookieValue: 'sid=abc',
      isValid: true,
      useCount: 9,
      lastValidateTime: '2026-05-22 09:00:00',
      createTime: '2026-05-22 08:00:00',
    }))

    mockPost.mockResolvedValueOnce({ data: { valid: 'true' } })
    await expect(douyinCookieApi.validate({ cookieId: 18 })).resolves.toBe(true)

    mockPost.mockResolvedValueOnce({
      data: {
        session_id: 'qr-1',
        screenshotBase64: 'base64-image',
        msg: '请扫码',
      },
    })
    await expect(douyinCookieApi.qrLoginStart()).resolves.toEqual({
      sessionId: 'qr-1',
      qrImageBase64: 'base64-image',
      message: '请扫码',
    })

    mockPost.mockResolvedValueOnce({
      data: {
        state: 'success',
        cookie: 'sid=abc',
        msg: '已登录',
      },
    })
    await expect(douyinCookieApi.qrLoginPoll('qr-1')).resolves.toEqual({
      status: 'success',
      cookieValue: 'sid=abc',
      message: '已登录',
    })
  })
})
