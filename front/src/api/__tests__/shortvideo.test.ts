/**
 * shortvideo API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { shortvideoApi, recommendPublishTime, trendsCurrent, shotsToImg2VideoKeyframes } from '../shortvideo'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('shortvideo API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list calls post with correct path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await shortvideoApi.list({ page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/list', { page: 0, rows: 10 })
  })

  it('get calls post with id', async () => {
    mockPost.mockResolvedValue({
      id: 1,
      title: '测试项目',
      projectType: 'viral_clone',
      status: 'draft',
      createTime: '2024-01-01',
    })
    await shortvideoApi.get(1)
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/get', { id: 1 })
  })

  it('save calls post with data', async () => {
    mockPost.mockResolvedValue(99)
    await shortvideoApi.save({ title: '新项目', projectType: 'viral_clone', status: 'draft' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/save', {
      title: '新项目',
      projectType: 'viral_clone',
      status: 'draft',
    })
  })

  it('generateDaily triggers daily generation for project', async () => {
    mockPost.mockResolvedValue({ ok: true })
    await shortvideoApi.generateDaily(8)
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/generate-daily', { id: 8 })
  })

  it('videoSearch posts content search query', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await shortvideoApi.videoSearch({ projectId: 3, keyword: '护肤', page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/content/search', {
      projectId: 3,
      keyword: '护肤',
      page: 0,
      rows: 10,
    })
  })

  it('collectStart posts account collect request', async () => {
    mockPost.mockResolvedValue({ id: 1, status: 0 })
    await shortvideoApi.collectStart({ input: 'MS4wLjABAAAA...', maxCount: 20 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/account-collect/start', {
      input: 'MS4wLjABAAAA...',
      maxCount: 20,
    })
  })

  it('aiCheckViolation sends text field', async () => {
    mockPost.mockResolvedValue({ passed: true })
    await shortvideoApi.aiCheckViolation('测试文案')
    expect(mockPost).toHaveBeenCalledWith('/short-video/ai/check-violation', { text: '测试文案' })
  })

  it('recommendPublishTime calls seo suggest-publish-time', async () => {
    mockPost.mockResolvedValue(['周一 20:00'])
    await recommendPublishTime({ accountId: 9, category: '护肤' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/seo/suggest-publish-time', {
      accountId: 9,
    })
  })

  it('trendsCurrent calls cross hot-topic-pool', async () => {
    mockPost.mockResolvedValue({ hotTopics: [] })
    await trendsCurrent()
    expect(mockPost).toHaveBeenCalledWith('/short-video/cross/hot-topic-pool', { limit: 20 })
  })

  it('shotListGet posts id', async () => {
    mockPost.mockResolvedValue({ id: 1, shots: [] })
    await shortvideoApi.shotListGet(7)
    expect(mockPost).toHaveBeenCalledWith('/short-video/shot-list/get', { id: 7 })
  })

  it('shotListGetByScript posts scriptId', async () => {
    mockPost.mockResolvedValue({ id: 2, shots: [] })
    await shortvideoApi.shotListGetByScript(9)
    expect(mockPost).toHaveBeenCalledWith('/short-video/shot-list/get-by-script', { scriptId: 9 })
  })

  it('videoTaskSubmit posts keyframes body', async () => {
    mockPost.mockResolvedValue({ taskId: 100 })
    await shortvideoApi.videoTaskSubmit({
      projectId: 1,
      shotListId: 2,
      keyframes: [{ shotId: 10, shotNumber: 1, imageUrl: 'https://x/k.jpg', duration: 5, motion: 'zoom-in' }],
    })
    expect(mockPost).toHaveBeenCalledWith('/short-video/video-task/submit', {
      projectId: 1,
      shotListId: 2,
      keyframes: [{ shotId: 10, shotNumber: 1, imageUrl: 'https://x/k.jpg', duration: 5, motion: 'zoom-in' }],
    })
  })
})

describe('shotsToImg2VideoKeyframes', () => {
  it('filters by keyframeUrl and id, sorts by shotNumber, maps imageUrl and motion', () => {
    const kf = shotsToImg2VideoKeyframes([
      { id: 3, shotNumber: 2, keyframeUrl: ' https://b ', cameraType: 'dolly-in', duration: 8 },
      { id: 1, shotNumber: 1, keyframeUrl: 'https://a', endFrameUrl: 'https://end' },
      { id: 0, shotNumber: 0, keyframeUrl: 'https://skip' },
      { id: 4, shotNumber: 3, sceneDescription: 'x' },
    ])
    expect(kf).toHaveLength(2)
    expect(kf[0]).toMatchObject({
      shotId: 1,
      shotNumber: 1,
      imageUrl: 'https://a',
      endFrameUrl: 'https://end',
      motion: 'zoom-in',
      duration: 5,
    })
    expect(kf[1]).toMatchObject({
      shotId: 3,
      shotNumber: 2,
      imageUrl: 'https://b',
      motion: 'dolly-in',
      duration: 8,
    })
  })
})
