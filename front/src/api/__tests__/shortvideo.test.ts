/**
 * shortvideo API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { shortvideoApi, recommendPublishTime, trendsCurrent, shotsToImg2VideoKeyframes, normalizeStringArray, getRecommendedVirals, subtitleGet, subtitleExportSrt, autoCompose, generateSubtitles, videoTaskStatus, adaptContentCalendarMonthView, adaptContentCalendarDateRange } from '../shortvideo'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('shortvideo API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
    mockPost.mockReset()
  })

  it('list calls post with correct path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await shortvideoApi.list({ page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/list', { page: 0, rows: 10 })
  })

  it('normalizes wrapped project list and snake_case project fields', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            project_id: '7',
            project_title: '包装项目',
            project_type: 'daily',
            script_id: '20',
            shot_list_id: '30',
            final_video_url: 'https://cdn.test/final.mp4',
            character_reference_url: 'https://cdn.test/role.png',
            create_time: '2026-05-22 10:00:00',
          },
        ],
        totalRecords: '1',
        page: '0',
        size: '20',
      },
    })

    await expect(shortvideoApi.list({ page: 0, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [
        expect.objectContaining({
          id: 7,
          title: '包装项目',
          projectType: 'daily',
          scriptId: 20,
          shotListId: 30,
          finalVideoUrl: 'https://cdn.test/final.mp4',
          characterReferenceUrl: 'https://cdn.test/role.png',
          createTime: '2026-05-22 10:00:00',
        }),
      ],
    }))
  })

  it('get calls post with id', async () => {
    mockPost.mockResolvedValue({
      detail: {
        project: {
          project_id: '1',
          project_title: '测试项目',
          project_type: 'viral_clone',
          status: 'draft',
          create_time: '2024-01-01',
        },
      },
    })
    await expect(shortvideoApi.get(1)).resolves.toEqual(expect.objectContaining({
      id: 1,
      title: '测试项目',
      projectType: 'viral_clone',
      createTime: '2024-01-01',
    }))
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

  it('generateDaily posts backend daily shoot contract', async () => {
    mockPost.mockResolvedValue({ ok: true })
    await shortvideoApi.generateDaily({
      personaId: 8,
      scheduleDate: '2026-05-21',
      count: 2,
      style: '专业',
      duration: '30秒',
      topic: '屏障修护',
    })
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/generate-daily', {
      personaId: 8,
      scheduleDate: '2026-05-21',
      count: 2,
      style: '专业',
      duration: '30秒',
      topic: '屏障修护',
    })
  })

  it('exportScript posts projectId contract', async () => {
    mockPost.mockResolvedValue('脚本')
    await shortvideoApi.exportScript(8)
    expect(mockPost).toHaveBeenCalledWith('/short-video/project/export-script', { projectId: 8 })
  })

  it('contentCalendarAutoGenerate posts persona date range', async () => {
    mockPost.mockResolvedValue(7)
    await shortvideoApi.contentCalendarAutoGenerate({
      personaId: 3,
      from: '2026-05-18',
      to: '2026-05-24',
    })
    expect(mockPost).toHaveBeenCalledWith('/short-video/content-calendar/auto-generate', {
      personaId: 3,
      from: '2026-05-18',
      to: '2026-05-24',
    })
  })

  it('quickGenerate calls real quick generation endpoint', async () => {
    mockPost.mockResolvedValue({ projectId: 9, scriptId: 10, shotListId: 11 })
    await shortvideoApi.quickGenerate({ theme: '护肤测评', keywords: '屏障修护', style: '温馨' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/quick/generate', {
      theme: '护肤测评',
      keywords: '屏障修护',
      style: '温馨',
    })
  })

  it('normalizes wrapped script-to-finished-video chain payloads', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { id: '9', script_id: '10', shot_list_id: '11', content: '包装脚本', name: '包装项目' } })
      .mockResolvedValueOnce({ data: { records: [{ id: '22', title: '包装脚本', content: '正文' }], totalElements: '1', page: '0', size: '50' } })
      .mockResolvedValueOnce({ data: { result: '生成正文' } })
      .mockResolvedValueOnce({ data: { id: '22' } })
      .mockResolvedValueOnce({ data: { id: '30', shots: [{ id: 1, shotNumber: 1 }] } })
      .mockResolvedValueOnce({ data: { id: '31', rows: [{ id: 2, shotNumber: 1 }] } })
      .mockResolvedValueOnce({ data: { shot_list_id: '32', records: [{ id: 3, shotNumber: 1 }] } })
      .mockResolvedValueOnce({ data: { task_id: '100' } })
      .mockResolvedValueOnce({ data: { rows: [{ id: 100, taskType: 'img2video', status: 'failed', errorMessage: 'RabbitMQ 未配置' }], totalCount: '1' } })

    await expect(shortvideoApi.quickGenerate({ theme: '护肤测评' })).resolves.toEqual({
      projectId: 9,
      scriptId: 10,
      shotListId: 11,
      scriptContent: '包装脚本',
      title: '包装项目',
    })
    await expect(shortvideoApi.svScriptList({ rows: 50 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [expect.objectContaining({ id: '22', title: '包装脚本' })],
    }))
    await expect(shortvideoApi.svScriptGenerate({ theme: '护肤测评' })).resolves.toBe('生成正文')
    await expect(shortvideoApi.svScriptSave({ title: '包装脚本', content: '正文', scriptType: 'daily' })).resolves.toBe(22)
    await expect(shortvideoApi.shotListGet(30)).resolves.toEqual(expect.objectContaining({ id: 30 }))
    await expect(shortvideoApi.shotListGetByScript(22)).resolves.toEqual(expect.objectContaining({ id: 31 }))
    await expect(shortvideoApi.shotListGenerate({ scriptContent: '正文' })).resolves.toEqual({
      shotListId: 32,
      shots: [expect.objectContaining({ id: 3 })],
    })
    await expect(shortvideoApi.videoTaskSubmit({ keyframes: [{ shotId: 1, shotNumber: 1, imageUrl: 'https://x/k.jpg' }] })).resolves.toEqual({ taskId: 100 })
    await expect(shortvideoApi.videoTaskList({ rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [expect.objectContaining({ errorMessage: 'RabbitMQ 未配置' })],
    }))
  })

  it('normalizes wrapped publish title, review and result payloads', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { items: [{ title: '包装标题', confidence: '0.91' }, '备选标题'] } })
      .mockResolvedValueOnce({
        data: {
          ok: 1,
          problems: ['封面偏暗'],
          advice: { records: ['增加字幕'] },
          id: 7,
        },
      })
      .mockResolvedValueOnce({
        data: {
          ok: true,
          projectId: 7,
          platformResults: [
            { channel: 'douyin', ok: 'success', videoId: 'aweme-1' },
            { channel: 'weixin-video', ok: false, message: '平台暂未接入真实发布能力' },
          ],
        },
      })

    await expect(shortvideoApi.publishGenerateTitle({ projectId: 7, count: 2 })).resolves.toEqual({
      titles: [
        { text: '包装标题', score: 0.91 },
        { text: '备选标题', score: undefined },
      ],
    })
    await expect(shortvideoApi.publishAiReview({ projectId: 7, title: '包装标题' })).resolves.toEqual({
      passed: true,
      issues: ['封面偏暗'],
      suggestions: ['增加字幕'],
      projectId: 7,
      officialReferences: [],
    })
    await expect(shortvideoApi.publishSubmit({
      projectId: 7,
      title: '包装标题',
      platforms: ['douyin'],
    })).resolves.toEqual({
      success: true,
      degraded: true,
      projectId: 7,
      platform: undefined,
      results: [
        { platform: 'douyin', success: true, itemId: 'aweme-1', error: undefined },
        { platform: 'weixin-video', success: false, itemId: undefined, error: '平台暂未接入真实发布能力' },
      ],
    })

    expect(mockPost).toHaveBeenCalledWith('/short-video/publish/generate-title', { projectId: 7, count: 2 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/publish/ai-review', { projectId: 7, title: '包装标题' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/publish/publish', {
      projectId: 7,
      title: '包装标题',
      platforms: ['douyin'],
    })
  })

  it('svScriptGenerate posts typed planning payload', async () => {
    mockPost.mockResolvedValue('脚本内容')
    await shortvideoApi.svScriptGenerate({
      type: 'daily',
      theme: '护肤测评',
      viralVideoId: 12,
      productInfo: '屏障修护',
      style: 'professional',
      duration: 45,
    })
    expect(mockPost).toHaveBeenCalledWith('/short-video/script/generate', {
      type: 'daily',
      theme: '护肤测评',
      viralVideoId: 12,
      productInfo: '屏障修护',
      style: 'professional',
      duration: 45,
    })
  })

  it('svScriptSave posts backend save contract', async () => {
    mockPost.mockResolvedValue(22)
    await shortvideoApi.svScriptSave({
      title: '护肤测评',
      content: '脚本内容',
      scriptType: 'daily',
      generationType: 'ai',
      wordCount: 4,
    })
    expect(mockPost).toHaveBeenCalledWith('/short-video/script/save', {
      title: '护肤测评',
      content: '脚本内容',
      scriptType: 'daily',
      generationType: 'ai',
      wordCount: 4,
    })
  })

  it('calendar date-range adapter forwards personaId', async () => {
    mockPost.mockResolvedValue([])
    await shortvideoApi.calendar({ startDate: '2026-05-18', endDate: '2026-05-24', personaId: 12 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/content-calendar/date-range', {
      from: '2026-05-18',
      to: '2026-05-24',
      personaId: 12,
    })
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

  it('viralList normalizes wrapped viral video rows', async () => {
    mockPost.mockResolvedValue({
      data: {
        rows: [
          { id: 8, title: '爆款护肤结构' },
        ],
      },
    })
    await expect(shortvideoApi.viralList({ rows: 50 })).resolves.toEqual([
      expect.objectContaining({ id: 8, title: '爆款护肤结构' }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/short-video/viral/list', {
      mode: 'my',
      category: undefined,
      sortBy: 'viralScore',
      page: 0,
      rows: 50,
    })
  })

  it('normalizes wrapped remake, viral detail/action and AI text payloads', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [
            {
              template_id: '9',
              template_name: '三段式复刻',
              remake_type: 'form_copy',
              structure_template: '{"hook":"前三秒"}',
              adaptation_guide: '保留情绪曲线',
              usage_count: '4',
              avg_viral_score: '91',
              create_time: '2026-05-22 10:00:00',
            },
          ],
          totalElements: '1',
          page: '0',
          size: '20',
        },
      })
      .mockResolvedValueOnce({
        data: {
          template: {
            template_id: '10',
            template_name: '保存模板',
            remake_type: 'scene_copy',
            structure_template: { scene: '厨房' },
            create_time: '2026-05-22 11:00:00',
          },
        },
      })
      .mockResolvedValueOnce({ data: { result: '模板生成脚本' } })
      .mockResolvedValueOnce({
        data: {
          detail: {
            viral_video_id: '8',
            video_title: '包装爆款详情',
            author_name: '护肤达人',
            view_count: '20000',
            like_count: '1200',
            favorite_count: '360',
            cover_bos_url: '//cdn.test/cover.jpg',
            video_bos_url: '//cdn.test/video.mp4',
            keyframe_bos_urls: '["//cdn.test/frame.jpg"]',
            deep_analyze_status: 'completed',
            deep_analysis_result: '{"opening":"前三秒痛点"}',
          },
        },
      })
      .mockResolvedValueOnce({ data: { ok: 'success', viralVideoId: '8' } })
      .mockResolvedValueOnce({ data: { id: '15' } })
      .mockResolvedValueOnce({ data: { scriptContent: '热点包装脚本' } })
      .mockResolvedValueOnce({ data: { content: '热点包装文案' } })
      .mockResolvedValueOnce({ data: { title: '热点包装标题' } })

    await expect(shortvideoApi.remakeTemplateList({ rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [
        expect.objectContaining({
          id: 9,
          templateName: '三段式复刻',
          remakeType: 'form_copy',
          structureTemplate: { hook: '前三秒' },
          usageCount: 4,
          avgViralScore: 91,
        }),
      ],
    }))
    await expect(shortvideoApi.remakeTemplateSave({ templateName: '保存模板' })).resolves.toEqual(expect.objectContaining({
      id: 10,
      templateName: '保存模板',
      structureTemplate: { scene: '厨房' },
    }))
    await expect(shortvideoApi.remakeTemplateGenerate({ templateId: 9 })).resolves.toBe('模板生成脚本')
    await expect(shortvideoApi.viralGet(8)).resolves.toEqual(expect.objectContaining({
      id: 8,
      title: '包装爆款详情',
      authorName: '护肤达人',
      viewCount: 20000,
      favoriteCount: 360,
      coverBosUrl: '//cdn.test/cover.jpg',
      videoBosUrl: '//cdn.test/video.mp4',
      keyframeBosUrls: '["//cdn.test/frame.jpg"]',
      deepAnalyzeStatus: 'completed',
      deepAnalysisResult: '{"opening":"前三秒痛点"}',
    }))
    await expect(shortvideoApi.viralAnalyze(8)).resolves.toEqual(expect.objectContaining({ ok: true, id: 8 }))
    await expect(shortvideoApi.viralCollect(8)).resolves.toBe(15)
    await expect(shortvideoApi.aiGenerateScript({ theme: '热点' })).resolves.toBe('热点包装脚本')
    await expect(shortvideoApi.aiGenerateCopy({ theme: '热点' })).resolves.toBe('热点包装文案')
    await expect(shortvideoApi.aiGenerateTitle({ theme: '热点' })).resolves.toBe('热点包装标题')
  })

  it('collectStart posts account collect request', async () => {
    mockPost.mockResolvedValue({ id: 1, status: 0 })
    await shortvideoApi.collectStart({ input: 'MS4wLjABAAAA...', maxCount: 20 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/account-collect/start', {
      input: 'MS4wLjABAAAA...',
      maxCount: 20,
    })
  })

  it('collectList normalizes accountCollectTasks payload envelope', async () => {
    mockPost.mockResolvedValue({
      payload: {
        accountCollectTasks: [
          { task_id: '11', account_name: '包装采集任务', sv_account_id: '7', status: 'collecting' },
        ],
        totalElements: '1',
        page: '0',
        size: '20',
      },
    })

    await expect(shortvideoApi.collectList({ page: 0, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [expect.objectContaining({ id: 11, accountName: '包装采集任务', svAccountId: 7, status: 'collecting' })],
    }))
    expect(mockPost).toHaveBeenCalledWith('/short-video/account-collect/list', { page: 0, rows: 20 })
  })

  it('normalizes wrapped shortvideo utility page contracts', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ task_id: '1', status: 'collecting', sv_account_id: '7', total_videos: '10', collected_videos: '4' }], totalElements: '1', page: '0', size: '20' } })
      .mockResolvedValueOnce({ data: { items: [{ id: '3', url: 'https://cdn.test/final.mp4', materialType: 'video/mp4' }], totalCount: '1' } })
      .mockResolvedValueOnce({ data: { records: [{ id: '4', name: '历史 BGM', url: 'https://cdn.test/bgm.mp3' }] } })
      .mockResolvedValueOnce({ data: { musicUrl: 'https://cdn.test/new.mp3', durationMs: '30000' } })
      .mockResolvedValueOnce({ data: { list: [{ description: '点击音效', audioUrl: 'https://cdn.test/sfx.mp3' }] } })
      .mockResolvedValueOnce({ data: { records: [{ id: '5', planDate: '2026-05-22', title: '排期' }] } })
      .mockResolvedValueOnce({ data: { id: '6' } })
      .mockResolvedValueOnce({ data: { result: '7' } })

    await expect(shortvideoApi.collectList({ page: 0, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [expect.objectContaining({ id: 1, status: 'collecting', svAccountId: 7, totalVideos: 10, collectedVideos: 4 })],
    }))
    await expect(shortvideoApi.materialList({ page: 0, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [expect.objectContaining({ id: 3, materialType: 'video/mp4' })],
    }))
    await expect(shortvideoApi.musicHistory({ rows: 10 })).resolves.toEqual([
      expect.objectContaining({ name: '历史 BGM' }),
    ])
    await expect(shortvideoApi.generateBgm({ durationSec: 30 })).resolves.toEqual(expect.objectContaining({
      musicUrl: 'https://cdn.test/new.mp3',
      url: 'https://cdn.test/new.mp3',
    }))
    await expect(shortvideoApi.generateSfx({ sceneDescription: '点击', durationSec: 3 })).resolves.toEqual([
      expect.objectContaining({ description: '点击音效', name: '点击音效', url: 'https://cdn.test/sfx.mp3' }),
    ])
    await expect(shortvideoApi.contentCalendarDateRange({ from: '2026-05-18', to: '2026-05-24', personaId: 12 })).resolves.toEqual([
      expect.objectContaining({ title: '排期' }),
    ])
    await expect(shortvideoApi.contentCalendarSave({
      personaId: 12,
      planDate: '2026-05-22',
      contentType: 'video',
      title: '排期',
    })).resolves.toBe(6)
    await expect(shortvideoApi.contentCalendarAutoGenerate({
      personaId: 12,
      from: '2026-05-18',
      to: '2026-05-24',
    })).resolves.toBe(7)
  })

  it('normalizes collect and music arrays from result/detail wrappers', async () => {
    mockPost
      .mockResolvedValueOnce({
        result: {
          tasks: [{ task_id: '11', status: 'collecting', account_name: '包装采集任务', sv_account_id: '7' }],
          totalRecords: '1',
        },
      })
      .mockResolvedValueOnce({
        detail: {
          audios: [{ id: '12', name: '包装历史音频', audioUrl: 'https://cdn.test/audio.mp3' }],
        },
      })
      .mockResolvedValueOnce({
        result: [{ description: '包装音效', audioUrl: 'https://cdn.test/sfx.mp3' }],
      })

    await expect(shortvideoApi.collectList({ page: 0, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [expect.objectContaining({ id: 11, accountName: '包装采集任务', svAccountId: 7 })],
    }))
    await expect(shortvideoApi.musicHistory({ rows: 10 })).resolves.toEqual([
      expect.objectContaining({ name: '包装历史音频', url: 'https://cdn.test/audio.mp3' }),
    ])
    await expect(shortvideoApi.generateSfx({ sceneDescription: '点击', durationSec: 3 })).resolves.toEqual([
      expect.objectContaining({ name: '包装音效', url: 'https://cdn.test/sfx.mp3' }),
    ])
  })

  it('normalizes wrapped material production, video task and edit payloads', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [
            {
              material_id: '5',
              project_id: '7',
              file_url: 'https://cdn.test/final.mp4',
              material_type: 'video/mp4',
              file_size: '2048',
              duration_sec: '30',
              create_time: '2026-05-22 10:00:00',
            },
          ],
          totalElements: '1',
        },
      })
      .mockResolvedValueOnce({
        data: {
          rows: [
            {
              task_id: '101',
              task_type: 'img2video',
              status: 'failed',
              progress_current: '40',
              project_id: '7',
              shot_list_id: '30',
              error_message: '供应商未配置',
              output_url: 'https://cdn.test/out.mp4',
              create_time: '2026-05-22 11:00:00',
            },
          ],
          totalCount: '1',
        },
      })
      .mockResolvedValueOnce({ data: { task_id: '101', status: 'running', progress_current: '4', progress_total: '10', status_message: '生成中' } })
      .mockResolvedValueOnce({ data: { final_video_url: 'https://cdn.test/final.mp4', duration_sec: '30', thumbnail_url: 'https://cdn.test/cover.jpg' } })
      .mockResolvedValueOnce({ data: { records: [{ start_time: '0', end_time: '3', content: '前三秒痛点' }] } })
      .mockResolvedValueOnce({ data: { content: '1\n00:00:00,000 --> 00:00:03,000\n前三秒痛点\n' } })
      .mockResolvedValueOnce({ data: { result: '4' } })
      .mockResolvedValueOnce({ data: { items: [{ day: '2026-05-22', playCount: '100' }] } })
      .mockResolvedValueOnce({ data: { ok: true, count: '2' } })
      .mockResolvedValueOnce({ data: { result: { ok: true } } })
      .mockResolvedValueOnce({ data: { result: { ok: true } } })

    await expect(shortvideoApi.materialList({ projectId: 7, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [
        expect.objectContaining({
          id: 5,
          projectId: 7,
          fileUrl: 'https://cdn.test/final.mp4',
          materialType: 'video/mp4',
          fileSize: 2048,
          duration: 30,
          createTime: '2026-05-22 10:00:00',
        }),
      ],
    }))
    await expect(shortvideoApi.videoTaskList({ projectId: 7, rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 1,
      list: [
        expect.objectContaining({
          id: 101,
          taskType: 'img2video',
          status: 'failed',
          progress: 40,
          projectId: 7,
          shotListId: 30,
          errorMessage: '供应商未配置',
          outputUrl: 'https://cdn.test/out.mp4',
        }),
      ],
    }))
    await expect(videoTaskStatus(101)).resolves.toEqual({
      taskId: 101,
      status: 'running',
      progressCurrent: 4,
      progressTotal: 10,
      message: '生成中',
      errorMessage: undefined,
    })
    await expect(autoCompose({ projectId: 7 })).resolves.toEqual(expect.objectContaining({
      finalVideoUrl: 'https://cdn.test/final.mp4',
      bosKey: undefined,
      duration: 30,
      thumbnail: 'https://cdn.test/cover.jpg',
    }))
    await expect(generateSubtitles({ videoUrl: 'https://cdn.test/final.mp4' })).resolves.toEqual({
      records: [{ start_time: '0', end_time: '3', content: '前三秒痛点' }],
    })
    await expect(subtitleExportSrt(18)).resolves.toContain('前三秒痛点')
    await expect(shortvideoApi.videoSave({ title: '包装视频' })).resolves.toBe(4)
    await expect(shortvideoApi.videoDataTrend(5)).resolves.toEqual([{ day: '2026-05-22', playCount: '100' }])
    await expect(shortvideoApi.materialGenerateKeyframes({ projectId: 7 })).resolves.toEqual({ ok: true, count: '2' })
    await expect(shortvideoApi.materialGenerateVoiceBatch({ projectId: 7 })).resolves.toEqual({ ok: true })
    await expect(shortvideoApi.materialImg2VideoBatch({ projectId: 7 })).resolves.toEqual({ ok: true })
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

  it('normalizes wrapped SEO suggestion arrays', async () => {
    mockPost
      .mockResolvedValueOnce({ records: ['敏感肌', ' 修护 ', null] })
      .mockResolvedValueOnce({ data: ['敏感肌修护标题'] })
      .mockResolvedValueOnce({ items: ['今晚 20:00'] })

    await expect(shortvideoApi.seoSuggestTags({ title: '敏感肌', industry: '护肤' })).resolves.toEqual(['敏感肌', '修护'])
    await expect(shortvideoApi.seoSuggestAbTitles({ baseTitle: '敏感肌' })).resolves.toEqual(['敏感肌修护标题'])
    await expect(shortvideoApi.seoSuggestPublishTime({ accountId: 9 })).resolves.toEqual(['今晚 20:00'])

    expect(mockPost).toHaveBeenCalledWith('/short-video/seo/suggest-tags', { title: '敏感肌', industry: '护肤' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/seo/suggest-ab-titles', { baseTitle: '敏感肌' })
    expect(mockPost).toHaveBeenCalledWith('/short-video/seo/suggest-publish-time', { accountId: 9 })
  })

  it('normalizes wrapped publish time recommendation rows', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          { time_label: '今晚 20:00', avg_view_count: '12000', publish_hour: '20' },
        ],
      },
    })

    await expect(shortvideoApi.contentPublishTimeRecommend(9)).resolves.toEqual([
      expect.objectContaining({ label: '今晚 20:00', avgViewCount: 12000, hour: 20 }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/short-video/content/publish-time-recommend', { accountId: 9 })
  })

  it('adapts content calendar month and date-range wrappers from days maps or row lists', () => {
    expect(adaptContentCalendarMonthView({
      days: {
        '2026-05-21': [
          { type: 'planned', title: '计划内容' },
          { type: 'published_video', title: '已发布内容' },
        ],
      },
    })).toEqual([
      {
        date: '2026-05-21',
        items: [
          { title: '计划内容', status: 'planned' },
          { title: '已发布内容', status: 'published' },
        ],
      },
    ])

    expect(adaptContentCalendarMonthView({
      data: {
        records: [
          { plan_date: '2026-05-22', content_title: '包装排期', status: '0' },
          { publish_date: '2026-05-22 20:00:00', video_title: '包装发布', status: 'published' },
        ],
      },
    })).toEqual([
      {
        date: '2026-05-22',
        items: [
          { title: '包装排期', status: 'planned' },
          { title: '包装发布', status: 'published' },
        ],
      },
    ])

    expect(adaptContentCalendarDateRange({
      data: {
        items: [
          {
            id: '6',
            plan_date: '2026-05-23',
            content_type: 'daily',
            title: '日更排期',
            project_id: '88',
            publish_time: '2026-05-23 20:00:00',
          },
        ],
      },
    })).toEqual([
      expect.objectContaining({
        id: '6',
        date: '2026-05-23',
        publishDate: '2026-05-23',
        scheduledDate: '2026-05-23',
        title: '日更排期',
        projectId: '88',
        platform: 'daily',
        publishTime: '2026-05-23 20:00:00',
      }),
    ])
  })

  it('normalizes nested string arrays without leaking non-string blanks', () => {
    expect(normalizeStringArray({ data: { list: [' 标题 ', 123, '', undefined] } })).toEqual(['标题', '123'])
    expect(normalizeStringArray({ total: 0 })).toEqual([])
  })

  it('trendsCurrent calls cross hot-topic-pool', async () => {
    mockPost.mockResolvedValue({ hotTopics: [] })
    await trendsCurrent()
    expect(mockPost).toHaveBeenCalledWith('/short-video/cross/hot-topic-pool', { limit: 20 })
  })

  it('posts persona fusion constraints and normalizes wrapped fusion payloads', async () => {
    const { personaViralFusion, matchPersonasForViral } = await import('../shortvideo')
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [
            { id: 2, name: '护肤专家', score: 0.91, matchReason: '调性吻合' },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          content: '{"title":"融合脚本"}',
          id: 21,
          score: 0.88,
          tokenUsage: 188,
          type: 'form_imitation',
          constraints: {
            productId: 5,
            productName: '修护精华',
            topic: '春节修护场景',
            durationSeconds: 45,
            count: 3,
          },
        },
      })

    await expect(matchPersonasForViral(8)).resolves.toEqual([
      expect.objectContaining({ personaId: 2, personaName: '护肤专家', matchScore: 0.91, reason: '调性吻合' }),
    ])
    await expect(personaViralFusion({
      personaId: 2,
      viralVideoId: 8,
      productId: 5,
      topic: '春节修护场景',
      duration: 45,
      count: 3,
      fusionMode: 'hybrid',
    })).resolves.toEqual(expect.objectContaining({
      script: '{"title":"融合脚本"}',
      scriptId: 21,
      fusionScore: 0.88,
      tokensUsed: 188,
      remakeType: 'form_imitation',
      constraintsApplied: expect.objectContaining({
        productId: 5,
        productName: '修护精华',
        topic: '春节修护场景',
        durationSeconds: 45,
        count: 3,
      }),
    }))
    expect(mockPost).toHaveBeenCalledWith('/short-video/persona-fusion/match-personas', { viralVideoId: 8 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/persona-fusion/generate-fused-script', {
      viralVideoId: 8,
      personaId: 2,
      remakeType: 'form_imitation',
      productId: 5,
      topic: '春节修护场景',
      duration: 45,
      count: 3,
    })
  })

  it('shotListGet posts id', async () => {
    mockPost.mockResolvedValue({
      result: {
        shot_list_id: '1',
        script_id: '9',
        shots: [
          {
            shot_id: '3',
            shot_number: '2',
            scene_description: '卖点展示',
            keyframe_url: 'https://cdn.test/k.jpg',
            video_url: 'https://cdn.test/v.mp4',
          },
        ],
      },
    })
    await expect(shortvideoApi.shotListGet(7)).resolves.toEqual(expect.objectContaining({
      id: 1,
      scriptId: 9,
      shotCount: 1,
      shots: [
        expect.objectContaining({
          id: 3,
          shotNumber: 2,
          sceneDescription: '卖点展示',
          keyframeUrl: 'https://cdn.test/k.jpg',
          videoUrl: 'https://cdn.test/v.mp4',
        }),
      ],
    }))
    expect(mockPost).toHaveBeenCalledWith('/short-video/shot-list/get', { id: 7 })
  })

  it('shotListGetByScript posts scriptId', async () => {
    mockPost.mockResolvedValue({ id: 2, shots: [] })
    await shortvideoApi.shotListGetByScript(9)
    expect(mockPost).toHaveBeenCalledWith('/short-video/shot-list/get-by-script', { scriptId: 9 })
  })

  it('dashboardProjects posts dashboard project progress contract', async () => {
    mockPost.mockResolvedValue({ data: { records: [{ id: 7, title: '包装项目' }] } })
    await expect(shortvideoApi.dashboardProjects({ page: 0, rows: 6, status: 'processing' })).resolves.toEqual([
      { id: 7, title: '包装项目' },
    ])
    expect(mockPost).toHaveBeenCalledWith('/short-video/dashboard/projects', {
      page: 0,
      rows: 6,
      status: 'processing',
    })
  })

  it('dashboardCostBreakdown posts optional projectId contract', async () => {
    mockPost.mockResolvedValue({ data: { total: 1.2 } })
    await expect(shortvideoApi.dashboardCostBreakdown({ projectId: 7 })).resolves.toEqual({ total: 1.2 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/dashboard/cost-breakdown', { projectId: 7 })
  })

  it('normalizes dashboard trend, stats, video search and hot topic wrappers', async () => {
    mockPost
      .mockResolvedValueOnce({ result: { trend: [{ date: '2026-05-22', playCount: 100 }] } })
      .mockResolvedValueOnce({ payload: { dashboardStats: { totalVideoCount: 3, totalPlayCount: 900 } } })
      .mockResolvedValueOnce({ body: { dashboardProjects: [{ id: 8, title: '包装项目', stage: '成片' }] } })
      .mockResolvedValueOnce({ body: { costBreakdown: { total: 1.2, videoCost: 1 } } })
      .mockResolvedValueOnce({ records: [{ id: 9, title: '包装视频', playCount: 1000 }], totalElements: 4 })
      .mockResolvedValueOnce({ payload: { hotTopics: [{ topic: '包装热点', heat: 8800, category: '护肤' }] } })

    await expect(shortvideoApi.dataTrend({ days: 7 })).resolves.toEqual([{ date: '2026-05-22', playCount: 100 }])
    await expect(shortvideoApi.dashboardStats()).resolves.toEqual({ totalVideoCount: 3, totalPlayCount: 900 })
    await expect(shortvideoApi.dashboardProjects({ page: 0, rows: 6 })).resolves.toEqual([
      expect.objectContaining({ id: 8, title: '包装项目', stage: '成片' }),
    ])
    await expect(shortvideoApi.dashboardCostBreakdown()).resolves.toEqual({ total: 1.2, videoCost: 1 })
    await expect(shortvideoApi.videoSearch({ rows: 20 })).resolves.toEqual(expect.objectContaining({
      total: 4,
      list: [expect.objectContaining({ id: 9, title: '包装视频' })],
    }))
    await expect(trendsCurrent({ limit: 5 })).resolves.toEqual([
      expect.objectContaining({ keyword: '包装热点', hotScore: 8800 }),
    ])
  })

  it('normalizes wrapped quality dashboard, workflow template, viral recommendation and subtitle payloads', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { totalScore: '86.5', scoredCount: '12' } })
      .mockResolvedValueOnce({ data: { records: [{ day: '2026-05-22', avgScore: '8.6' }] } })
      .mockResolvedValueOnce({ items: [{ model: 'qwen-video', avgScore: '8.2' }] })
      .mockResolvedValueOnce({ content: [{ cameraType: 'push-in', avgScore: '8.1' }] })
      .mockResolvedValueOnce({ data: { items: ['开头节奏偏慢', '  补强前三秒钩子  '] } })
      .mockResolvedValueOnce({ data: { weakPoint: '字幕密度偏高' } })
      .mockResolvedValueOnce({
        records: [
          {
            template_id: '3',
            template_name: '短视频标准链路',
            desc: '脚本到成片',
            step_list: [
              { id: 's1', type: 'script', name: '写脚本', days: '1' },
              { id: 's2', type: 'videoGen', name: '生成视频', days: '2' },
            ],
            is_system: true,
          },
        ],
      })
      .mockResolvedValueOnce({
        data: {
          template_id: '3',
          name: '短视频标准链路',
          workflowSteps: [{ id: 's1', type: 'script', label: '写脚本' }],
        },
      })
      .mockResolvedValueOnce({ data: { rows: [{ id: 8, title: '包装爆款', viralScore: '91' }] } })
      .mockResolvedValueOnce({
        data: {
          records: [
            {
              segment_id: 'seg-1',
              start_time: '0',
              end_time: '3',
              content: '前三秒痛点',
              font_size: '24',
              font_color: '#ffffff',
              font_family: 'Inter',
              position: 'bottom',
            },
          ],
        },
      })

    await expect(shortvideoApi.qualityOverview()).resolves.toEqual({ totalScore: '86.5', scoredCount: '12' })
    await expect(shortvideoApi.qualityTrend({ days: 14 })).resolves.toEqual([
      { day: '2026-05-22', avgScore: '8.6' },
    ])
    await expect(shortvideoApi.qualityModelRanking({ days: 14 })).resolves.toEqual([
      { model: 'qwen-video', avgScore: '8.2' },
    ])
    await expect(shortvideoApi.qualityCameraRanking({ days: 14 })).resolves.toEqual([
      { cameraType: 'push-in', avgScore: '8.1' },
    ])
    await expect(shortvideoApi.qualityAiReflections()).resolves.toEqual(['开头节奏偏慢', '补强前三秒钩子'])
    await expect(shortvideoApi.feedbackWeeklyReport()).resolves.toEqual({ weakPoint: '字幕密度偏高' })
    await expect(shortvideoApi.workflowTemplateList()).resolves.toEqual([
      expect.objectContaining({
        id: 3,
        templateName: '短视频标准链路',
        description: '脚本到成片',
        steps: [
          { id: 's1', type: 'script', name: '写脚本', days: '1' },
          { id: 's2', type: 'videoGen', name: '生成视频', days: '2' },
        ],
        isSystem: 1,
      }),
    ])
    await expect(shortvideoApi.workflowTemplateGet(3)).resolves.toEqual(expect.objectContaining({
      id: 3,
      templateName: '短视频标准链路',
      steps: [{ id: 's1', type: 'script', label: '写脚本' }],
    }))
    await expect(getRecommendedVirals({ limit: 5 })).resolves.toEqual([
      { id: 8, title: '包装爆款', viralScore: '91' },
    ])
    await expect(subtitleGet(18)).resolves.toEqual([
      {
        id: 'seg-1',
        startTime: 0,
        endTime: 3,
        text: '前三秒痛点',
        fontSize: 24,
        color: '#ffffff',
        fontFamily: 'Inter',
        position: 'bottom',
      },
    ])

    expect(mockPost).toHaveBeenCalledWith('/short-video/quality-dashboard/overview', {})
    expect(mockPost).toHaveBeenCalledWith('/short-video/quality-dashboard/trend', { days: 14 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/quality-dashboard/model-ranking', { days: 14 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/quality-dashboard/camera-ranking', { days: 14 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/quality-dashboard/ai-reflections', {})
    expect(mockPost).toHaveBeenCalledWith('/short-video/feedback/weekly-report', {})
    expect(mockPost).toHaveBeenCalledWith('/short-video/workflow-template/list', {})
    expect(mockPost).toHaveBeenCalledWith('/short-video/workflow-template/get', { id: 3 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/viral/recommended', { limit: 5 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/edit/subtitles/get', { videoId: 18 })
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

  it('dramaList normalizes wrapped list payloads', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [
          {
            id: 3,
            title: '包装短剧',
            genre: '都市',
            totalEpisodes: 12,
            status: 'draft',
            createTime: '2026-05-22 10:00:00',
          },
        ],
      },
    })

    await expect(shortvideoApi.dramaList()).resolves.toEqual([
      expect.objectContaining({ id: 3, title: '包装短剧' }),
    ])
    expect(mockPost).toHaveBeenCalledWith('/short-video/drama/list', {})
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
