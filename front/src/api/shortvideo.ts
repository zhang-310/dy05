import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  Img2VideoKeyframeMap,
  SvShot,
  SvShotListVO,
  VideoTaskSubmitBody,
  VideoTaskSubmitResult,
} from '@/types/shortvideo'

/** 与后端 SvProjectVO 对齐 */
export interface SvProject {
  id: number
  ownerId?: number
  accountId?: number
  title: string
  projectType: string
  personaId?: number
  scheduleDate?: string
  shootStatus?: string
  status?: string
  scriptId?: number
  shotListId?: number
  finalVideoUrl?: string
  thumbnailUrl?: string
  characterReferenceUrl?: string
  sceneReferenceUrl?: string
  duration?: number
  publishTitle?: string
  publishPlatforms?: string
  publishTime?: string
  reviewStatus?: string
  createTime?: string
  updateTime?: string
}

/** 与后端 SvProjectSearchVO 对齐 */
export interface SvProjectQuery {
  page?: number
  rows?: number
  title?: string
  status?: string
  projectType?: string
  sortName?: string
  sortOrder?: string
}

/** 与后端 SvProjectSaveVO 对齐 */
export interface SvProjectSave {
  id?: number
  accountId?: number
  title: string
  projectType: string
  personaId?: number
  scheduleDate?: string
  shootStatus?: string
  status?: string
  scriptId?: number
  shotListId?: number
  finalVideoUrl?: string
  thumbnailUrl?: string
  characterReferenceUrl?: string
  sceneReferenceUrl?: string
  duration?: number
  publishTitle?: string
  publishPlatforms?: string
  publishTime?: string
  reviewStatus?: string
  reviewerId?: number
  reviewComment?: string
  relatedProductIds?: number[]
}

export interface SvVideo {
  id: number; projectId: number; title: string; status: number
  playCount: number; likeCount: number; commentCount: number
  shareCount: number; duration: number; coverUrl: string; createTime: string
}
export interface SvVideoQuery { page?: number; rows?: number; projectId?: number; keyword?: string; status?: number }

/** 与后端 AccountCollectTaskVO 对齐（账号短视频采集任务） */
export interface AccountCollectTask {
  id: number
  ownerId?: number
  accountId?: number | null
  svAccountId?: number | null
  accountUrl?: string | null
  accountName?: string | null
  secUid?: string | null
  inputType?: string | null
  originalInput?: string | null
  /** pending | collecting | collected | analyzing | indexing | completed | failed */
  status: string
  totalVideos?: number
  collectedVideos?: number
  analyzedVideos?: number
  indexedVideos?: number
  targetKbId?: number | null
  errorMessage?: string | null
  createTime?: string
  updateTime?: string
}

/** @deprecated 请使用 AccountCollectTask */
export type CollectTask = AccountCollectTask
export interface CollectedVideo {
  id: number; taskId: number; videoId: string; title: string
  playCount: number; likeCount: number; coverUrl: string; createTime: string
  evidenceLevel?: string
  transcriptEvidenceLevel?: string
  sceneEvidenceLevel?: string
  transcriptLabel?: string
  sceneLabel?: string
  inferenceRisk?: boolean
}

export interface RemakeTemplate {
  id: number; templateName: string; templateType: string
  content: string; status: number; createTime: string
}
export interface RemakeTemplateQuery { page?: number; rows?: number; templateName?: string; templateType?: string }

/** 与后端 SvViralVideo JSON 一致：播放量为 viewCount，收藏为 favoriteCount */
export interface ViralVideo {
  id: number
  title: string
  authorName: string
  viewCount?: number
  likeCount?: number
  /** 评论数 */
  commentCount?: number
  favoriteCount?: number
  shareCount?: number
  /** 抖音分享/播放页链接 */
  videoUrl?: string
  coverUrl?: string
  /** BOS 封面 CDN（与 coverUrl 二选一或并存） */
  coverBosUrl?: string
  /** BOS 视频 CDN，可直接 <video src>（需服务端允许跨域或同源） */
  videoBosUrl?: string
  /** 关键帧 BOS URL：库表为 JSON 字符串数组；若后端改为直接数组亦兼容 */
  keyframeBosUrls?: string | string[]
  /** 分镜/场景说明（文本或多段，后端可能写入推演说明） */
  sceneDescriptions?: string
  /** 视频时长（秒） */
  videoDuration?: number
  /** 视频简介 */
  description?: string
  /** 抖音侧视频 ID */
  douyinVideoId?: string
  createTime?: string
  viralScore?: number
  /** 深度分析状态 */
  deepAnalyzeStatus?: string
  deepAnalyzeProgress?: string
  /** 步骤 JSON（与详情里「深度分析」进度一致） */
  deepAnalyzeSteps?: string
  /** 深度拆解 JSON */
  deepAnalysisResult?: string
  lightAnalysisResult?: string
  transcript?: string
  /** 旧前端字段名（兼容） */
  playCount?: number
  collectCount?: number
}
export interface ViralVideoQuery { page?: number; rows?: number; keyword?: string; minPlayCount?: number; mode?: string; category?: string; sortBy?: string }
export interface ContentCalendarMonthStats { plannedCount?: number; publishedCount?: number; completionRate?: number }

/** 将内容日历聚合接口的 days 转为页面用数组 */
export function adaptContentCalendarMonthView(data: Record<string, unknown>): Array<{ date: string; items: Array<{ title: string; status: string }> }> {
  const days = data.days as Record<string, Array<Record<string, unknown>>> | undefined
  if (!days) return []
  return Object.keys(days).sort().map((date) => ({
    date,
    items: (days[date] ?? []).map((it) => {
      const typ = String(it.type ?? '')
      const published = typ.includes('published')
      return {
        title: String(it.title ?? ''),
        status: published ? 'published' : 'planned',
      }
    }),
  }))
}

/** 内容日历计划条目 → 发布页/日更用扁平列表 */
export function adaptContentCalendarDateRange(entries: Array<Record<string, unknown>>): Array<Record<string, unknown>> {
  return entries.map((e) => ({
    id: e.id,
    date: String(e.planDate ?? e.publishDate ?? '').slice(0, 10),
    publishDate: e.planDate,
    scheduledDate: e.planDate,
    title: String(e.title ?? ''),
    status: e.status,
    projectId: e.projectId,
    platform: e.contentType,
  }))
}

/** 分镜镜头 → 图生视频 submit 的 keyframes（仅含已有关键帧 URL 的镜头） */
export function shotsToImg2VideoKeyframes(shots: SvShot[]): Img2VideoKeyframeMap[] {
  const withFrame = shots.filter(
    (s) =>
      s.id != null &&
      Number(s.id) > 0 &&
      typeof s.keyframeUrl === 'string' &&
      s.keyframeUrl.trim() !== ''
  )
  const sorted = [...withFrame].sort((a, b) => {
    const an = a.shotNumber ?? 9999
    const bn = b.shotNumber ?? 9999
    return an - bn
  })
  return sorted.map((s) => {
    const motion =
      typeof s.cameraType === 'string' && s.cameraType.trim() !== ''
        ? s.cameraType.trim()
        : 'zoom-in'
    return {
      shotId: Number(s.id ?? 0),
      shotNumber: Number(s.shotNumber ?? 0),
      imageUrl: String(s.keyframeUrl ?? '').trim(),
      endFrameUrl: s.endFrameUrl?.trim() || undefined,
      duration: typeof s.duration === 'number' && s.duration > 0 ? s.duration : 5,
      motion,
      sceneDescription: s.sceneDescription ?? undefined,
      cameraType: s.cameraType ?? undefined,
      mood: s.mood ?? undefined,
      action: s.action ?? undefined,
    }
  })
}

export const shortvideoApi = {
  list: (params: SvProjectQuery) => request.post<PageResult<SvProject>>('/short-video/project/list', params),
  get: (id: number) => request.post<SvProject>('/short-video/project/get', { id }),
  save: (params: SvProjectSave) => request.post<number>('/short-video/project/save', params),
  delete: (id: number) => request.post<void>('/short-video/project/delete', { id }),
  generateDaily: (id: number) => request.post<Record<string, unknown>>('/short-video/project/generate-daily', { id }),
  exportScript: (id: number) => request.post<string>('/short-video/project/export-script', { id }),

  videoSearch: (params: SvVideoQuery) => request.post<PageResult<SvVideo>>('/short-video/content/search', params),
  videoGet: (id: number) => request.post<SvVideo>('/short-video/content/get', { id }),
  videoSave: (params: Record<string, unknown>) => request.post<number>('/short-video/content/save', params),
  videoDelete: (id: number) => request.post<void>('/short-video/content/delete', { id }),
  /** 单条视频指标趋势（需 videoId） */
  videoDataTrend: (videoId: number) => request.post<Record<string, unknown>[]>('/short-video/content/data-trend', { videoId }),

  collectStart: (params: {
    input: string
    maxCount?: number
    accountId?: number
    targetKbId?: number
    /** keyword_video：按关键词打开抖音视频搜索页采集 */
    collectMode?: 'keyword_video'
  }) => request.post<AccountCollectTask>('/short-video/account-collect/start', params),
  collectList: (params: { page?: number; rows?: number }) =>
    request.post<PageResult<AccountCollectTask>>('/short-video/account-collect/list', params),
  collectStatus: (taskId: number) => request.post<AccountCollectTask>('/short-video/account-collect/status', { taskId }),
  collectCancel: (taskId: number) => request.post<void>('/short-video/account-collect/cancel', { taskId }),
  collectRetry: (taskId: number) => request.post<AccountCollectTask>('/short-video/account-collect/retry', { taskId }),
  collectVideos: (taskId: number, params?: { page?: number; rows?: number }) =>
    request.post<PageResult<CollectedVideo>>('/short-video/account-collect/videos', { taskId, ...params }),

  remakeTemplateList: (params: RemakeTemplateQuery) => request.post<PageResult<RemakeTemplate>>('/short-video/remake-template/list', params),
  remakeTemplateSave: (params: Partial<RemakeTemplate>) => request.post<RemakeTemplate>('/short-video/remake-template/save', params),
  remakeTemplateDelete: (id: number) => request.post<void>('/short-video/remake-template/delete', { id }),
  remakeTemplateGenerate: (params: Record<string, unknown>) => request.post<string>('/short-video/remake-template/generate', params),
  remakeTemplateCreateFromViral: (params: Record<string, unknown>) => request.post<RemakeTemplate>('/short-video/remake-template/create-from-viral', params),

  /** 爆款列表（后端返回数组，无分页包装） */
  viralList: (params: ViralVideoQuery) =>
    request.post<ViralVideo[]>('/short-video/viral/list', {
      mode: params.mode ?? 'my',
      category: params.keyword ?? params.category,
      sortBy: params.sortBy ?? 'viralScore',
      page: params.page ?? 0,
      rows: params.rows ?? 20,
    }),
  /** 单条详情（含深度拆解字段，列表可能截断或字段不全时用于刷新） */
  viralGet: (id: number) => request.post<ViralVideo>('/short-video/viral/get', { id }),
  viralAnalyze: (id: number) => request.post<{ ok?: boolean; id?: number }>('/short-video/viral/analyze', { id }),
  /** 收藏爆款（写入 sv_viral_favorite） */
  viralCollect: (viralVideoId: number) => request.post<number>('/short-video/viral/collect', { viralVideoId }),
  /** @deprecated 使用 viralCollect */
  viralFavorite: (viralVideoId: number) => request.post<number>('/short-video/viral/collect', { viralVideoId }),

  aiGenerateCopy: (params: Record<string, unknown>) => request.post<string>('/short-video/ai/generate-copy', params),
  aiGenerateScript: (params: Record<string, unknown>) => request.post<string>('/short-video/ai/generate-script', params),
  aiGenerateTitle: (params: Record<string, unknown>) => request.post<string>('/short-video/ai/generate-title', params),
  aiCheckViolation: (text: string) => request.post<Record<string, unknown>>('/short-video/ai/check-violation', { text }),

  svScriptList: (params: Record<string, unknown>) => request.post<PageResult<Record<string, unknown>>>('/short-video/script/list', params),
  svScriptSave: (params: Record<string, unknown>) => request.post<void>('/short-video/script/save', params),
  svScriptDelete: (id: number) => request.post<void>('/short-video/script/delete', { id }),

  shotListGet: (id: number) => request.post<SvShotListVO>('/short-video/shot-list/get', { id }),
  shotListGetByScript: (scriptId: number) =>
    request.post<SvShotListVO>('/short-video/shot-list/get-by-script', { scriptId }),

  /** 素材库分页（非 material 生产接口） */
  materialList: (params: { page?: number; rows?: number; materialType?: string; projectId?: number }) =>
    request.post<PageResult<Record<string, unknown>>>('/short-video/library/list', params),
  materialDelete: (id: number) => request.post<void>('/short-video/library/delete', { id }),
  /** 成片上传需 query: projectId；见 MaterialPage */
  materialUploadBase: '/api/v1/short-video/upload/final-video',

  /** multipart：人物参考图（characterId、file 必填；projectId 可选） */
  uploadReferenceCharacter: (params: { characterId: string; projectId?: number; file: File }) => {
    const fd = new FormData()
    fd.append('file', params.file)
    const q = new URLSearchParams({ characterId: params.characterId })
    if (params.projectId != null) q.set('projectId', String(params.projectId))
    return request.post<Record<string, string>>(`/short-video/upload/reference/character?${q}`, fd)
  },
  /** multipart：场景参考图 */
  uploadReferenceScene: (params: { sceneId: string; projectId?: number; file: File }) => {
    const fd = new FormData()
    fd.append('file', params.file)
    const q = new URLSearchParams({ sceneId: params.sceneId })
    if (params.projectId != null) q.set('projectId', String(params.projectId))
    return request.post<Record<string, string>>(`/short-video/upload/reference/scene?${q}`, fd)
  },

  dramaList: () => request.post<SvDrama[]>('/short-video/drama/list', {}),
  dramaCreate: (body: { title: string; description?: string; genre?: string; totalEpisodes?: number }) =>
    request.post<SvDrama>('/short-video/drama/create', body),
  dramaUpdate: (body: { id: number; title?: string; description?: string; genre?: string; totalEpisodes?: number }) =>
    request.post<SvDrama>('/short-video/drama/update', body),
  dramaDelete: (id: number) => request.post<void>('/short-video/drama/delete', { id }),
  dramaGenerateScript: (body: { dramaId: number; theme?: string; style?: string }) =>
    request.post<string>('/short-video/drama/generate-script', body),

  videoTaskSubmit: (params: VideoTaskSubmitBody) =>
    request.post<VideoTaskSubmitResult>('/short-video/video-task/submit', params),
  videoTaskStatus: (taskId: number) => request.post<VideoGenerationTaskStatus>('/short-video/video-task/status', { taskId }),
  videoTaskList: (params: { page?: number; rows?: number; projectId?: number }) =>
    request.post<PageResult<Record<string, unknown>>>('/short-video/video-task/list', params),
  videoTaskCancel: (taskId: number) => request.post<void>('/short-video/video-task/cancel', { taskId }),
  videoTaskRetry: (taskId: number) => request.post<Record<string, unknown>>('/short-video/video-task/retry', { taskId }),

  /** Dashboard 播放量等趋势 */
  dataTrend: (params: { days?: number }) => request.post<Record<string, unknown>[]>('/short-video/dashboard/trend', params),
  dashboardStats: () => request.post<Record<string, unknown>>('/short-video/dashboard/stats', {}),

  /** 内容日历聚合（月视图 raw Map，含 days） */
  contentCalendarView: (year: number, month: number) =>
    request.post<Record<string, unknown>>('/short-video/content/calendar', { year, month }),
  contentCalendarMonthStats: (year: number, month: number) =>
    request.post<ContentCalendarMonthStats>('/short-video/content/calendar-stats', { year, month }),
  /** accountId 必填 */
  contentPublishTimeRecommend: (accountId: number) =>
    request.post<Array<Record<string, unknown>>>('/short-video/content/publish-time-recommend', { accountId }),

  generateBgm: (params: { styleDescription?: string; durationSec?: number; instrumental?: boolean }) =>
    request.post<Record<string, unknown>>('/short-video/music/generate-bgm', {
      styleDescription: params.styleDescription ?? 'cinematic background music',
      durationSec: params.durationSec ?? 30,
      instrumental: params.instrumental !== false,
    }),
  generateSfx: (params: { sceneDescription?: string; durationSec?: number }) =>
    request.post<Array<Record<string, unknown>>>('/short-video/music/generate-sfx', {
      sceneDescription: params.sceneDescription ?? '',
      durationSec: params.durationSec ?? 5,
    }),
  /** 后端暂无历史接口 */
  musicHistory: (_params?: Record<string, unknown>) => Promise.resolve([] as Record<string, unknown>[]),

  seoSuggestTags: (params: { title: string; description?: string; industry?: string }) =>
    request.post<string[]>('/short-video/seo/suggest-tags', params),
  seoSuggestPublishTime: (params: { accountId?: number }) =>
    request.post<string[]>('/short-video/seo/suggest-publish-time', params),
  seoSuggestCover: (params: { frameUrls: string[] }) =>
    request.post<string>('/short-video/seo/suggest-cover', params),
  seoSuggestAbTitles: (params: { baseTitle: string }) =>
    request.post<string[]>('/short-video/seo/suggest-ab-titles', params),

  contentCalendarCrudList: (params: Record<string, unknown>) =>
    request.post<PageResult<Record<string, unknown>>>('/short-video/content-calendar/list', params),
  contentCalendarDateRange: (params: { from: string; to: string; personaId?: number }) =>
    request.post<Array<Record<string, unknown>>>('/short-video/content-calendar/date-range', params),

  /** 质量看板 / 反馈 / 工作流模板 */
  qualityOverview: () => request.post<Record<string, unknown>>('/short-video/quality-dashboard/overview', {}),
  qualityTrend: (params: { days?: number }) =>
    request.post<Array<Record<string, unknown>>>('/short-video/quality-dashboard/trend', params),
  feedbackWeeklyReport: () => request.post<Record<string, unknown>>('/short-video/feedback/weekly-report', {}),
  workflowTemplateList: () => request.post<Array<Record<string, unknown>>>('/short-video/workflow-template/list', {}),
  workflowTemplateGet: (id: number) => request.post<Record<string, unknown>>('/short-video/workflow-template/get', { id }),

  /**
   * @deprecated 使用 contentCalendarView + adaptContentCalendarMonthView，或 contentCalendarDateRange
   */
  calendar: (params: Record<string, unknown>) => {
    if (params.year != null && params.month != null) {
      return shortvideoApi.contentCalendarView(Number(params.year), Number(params.month)).then(adaptContentCalendarMonthView)
    }
    return shortvideoApi.contentCalendarDateRange({
      from: String(params.startDate ?? ''),
      to: String(params.endDate ?? ''),
    }).then(adaptContentCalendarDateRange)
  },
  /**
   * @deprecated 使用 contentCalendarMonthStats(year,month) 或 dashboardStats
   */
  calendarStats: (params: Record<string, unknown>) => {
    if (params.year != null && params.month != null) {
      return shortvideoApi.contentCalendarMonthStats(Number(params.year), Number(params.month))
    }
    return shortvideoApi.dashboardStats()
  },
  recommendPublishTime: (params: { accountId?: number }) =>
    shortvideoApi.seoSuggestPublishTime({ accountId: params.accountId }),
}

export interface SvDrama {
  id: number
  title: string
  genre?: string
  synopsis?: string
  description?: string
  totalEpisodes?: number
  episodesWithSynopsis?: number
  episodesWithProject?: number
  /** 后端为字符串，如 draft */
  status?: string | number
  createTime: string
  updateTime?: string
}

export interface SvDramaEpisode {
  id: number
  dramaId: number
  episodeNo: number
  episodeNumber?: number
  title: string
  synopsis: string
  cliffhanger?: string
  status: number
  createTime: string
  projectId?: number
}

export interface SvDramaCharacter {
  id: number
  dramaId: number
  characterName: string
  name?: string
  role: string
  description: string
  createTime: string
}

export function getRecommendedVirals(params?: { limit?: number }): Promise<Record<string, unknown>[]> {
  return request.post<Record<string, unknown>[]>('/short-video/viral/recommended', params ?? {})
}

export function recommendCamera(params: { sceneDescription?: string; [key: string]: unknown }): Promise<{
  primary: string
  recommendations?: Array<{ type: string; confidence: number }>
}> {
  const sceneDescription = params.sceneDescription ?? String(params.scene ?? '')
  return request.post('/short-video/material/recommend-camera', { sceneDescription, ...params })
}

export interface PublishTimeSlot {
  hour: number
  weekday: number
  score: number
  expectedViews: number
  competition: 'low' | 'medium' | 'high'
  reason: string
}

export interface RecommendPublishTimeParams {
  accountId?: number
  category?: string
  targetAudience?: string
  lookbackDays?: number
}

/** SEO 发布时间建议（字符串列表）；归因页等自行适配 */
export function recommendPublishTime(params: RecommendPublishTimeParams): Promise<string[]> {
  return request.post<string[]>('/short-video/seo/suggest-publish-time', {
    accountId: params.accountId,
  })
}

export interface VideoGenerationTaskStatus {
  taskId: number
  status: string
  progressCurrent: number
  progressTotal: number
  message: string
  errorMessage?: string
}

export function videoTaskStatus(taskId: number): Promise<VideoGenerationTaskStatus> {
  return request.post('/short-video/video-task/status', { taskId })
}

export interface CalendarEntry {
  id: number
  date: string
  projectId?: number
  projectName?: string
  title: string
  status: 'planned' | 'producing' | 'ready' | 'published'
  platform?: string
}

export interface CalendarQuery { startDate: string; endDate: string; accountId?: number; status?: string }

export interface CalendarStats {
  total: number; planned: number; producing: number; ready: number; published: number
  publishRate: number
}

export function calendarSave(entry: Partial<CalendarEntry>): Promise<number> {
  return request.post('/short-video/content-calendar/save', entry)
}

export function calendarDelete(id: number): Promise<void> {
  return request.post('/short-video/content-calendar/delete', { id })
}

export interface TrendTopic {
  keyword: string
  hotScore: number
  videoCount: number
  growthRate: number
  category: string
  relatedKeywords: string[]
  source?: string
  competition?: string
}

export function trendsCurrent(params?: { limit?: number; category?: string }): Promise<TrendTopic[]> {
  return request.post<Record<string, unknown>>('/short-video/cross/hot-topic-pool', {
    limit: params?.limit ?? 20,
  }).then((raw) => {
    const list = raw.hotTopics as Array<Record<string, unknown>> | undefined
    if (!Array.isArray(list)) return []
    return list.map((t) => ({
      keyword: String(t.topic ?? t.title ?? t.keyword ?? ''),
      hotScore: Number(t.heat ?? t.hotScore ?? 0),
      videoCount: Number(t.videoCount ?? 0),
      growthRate: Number(t.growthRate ?? 0),
      category: String(t.category ?? params?.category ?? ''),
      relatedKeywords: Array.isArray(t.relatedKeywords) ? t.relatedKeywords.map(String) : [],
      source: t.source != null ? String(t.source) : undefined,
      competition: t.competition != null ? String(t.competition) : undefined,
    }))
  })
}

export interface AutoComposeParams {
  projectId: number
  duration?: number
  style?: string
  bgmStyle?: string
  subtitleEnabled?: boolean
}

export function autoCompose(params: AutoComposeParams): Promise<Record<string, unknown>> {
  return request.post('/short-video/edit/auto-compose', params)
}

export function generateSubtitles(params: { scriptText?: string; language?: string; videoUrl?: string }): Promise<Record<string, unknown>> {
  return request.post('/short-video/edit/generate-subtitles', params)
}

export interface PersonaFusionScript {
  original: string
  fused: string
  predictedConversionRate: number
}

/** 使用 persona-fusion 后端；返回结构依服务实现 */
export function personaViralFusion(params: {
  personaId: number
  viralVideoId: number
  productId?: number
  topic?: string
  duration?: number
  count?: number
  fusionMode?: 'full_viral' | 'hybrid' | 'persona_led'
}): Promise<Record<string, unknown>> {
  const remakeType = params.fusionMode === 'persona_led' ? 'persona_led' : 'form_imitation'
  return request.post('/short-video/persona-fusion/generate-fused-script', {
    viralVideoId: params.viralVideoId,
    personaId: params.personaId,
    remakeType,
  })
}

export interface SubtitleSegment {
  id: string
  startTime: number
  endTime: number
  text: string
  fontSize?: number
  color?: string
  fontFamily?: string
  position?: string
}

/** 后端暂无独立字幕存储 API：由剪辑/时间线侧持久化 */
export function subtitleGet(_videoId: number): Promise<SubtitleSegment[]> {
  return Promise.resolve([])
}

export function subtitleSave(_videoId: number, _segments: SubtitleSegment[]): Promise<void> {
  return Promise.resolve()
}

/** 将字幕段转为 SRT（无后端存储时使用本地导出） */
export function subtitlesToSrt(segments: SubtitleSegment[]): string {
  const pad = (n: number, w: number) => String(n).padStart(w, '0')
  const ts = (sec: number) => {
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    const s = Math.floor(sec % 60)
    const ms = Math.floor((sec % 1) * 1000)
    return `${pad(h, 2)}:${pad(m, 2)}:${pad(s, 2)},${pad(ms, 3)}`
  }
  return segments
    .map((seg, i) => `${i + 1}\n${ts(seg.startTime)} --> ${ts(seg.endTime)}\n${seg.text}\n`)
    .join('\n')
}

export function subtitleExportSrt(_videoId: number, segments?: SubtitleSegment[]): Promise<string> {
  if (segments && segments.length > 0) return Promise.resolve(subtitlesToSrt(segments))
  return Promise.resolve('')
}
