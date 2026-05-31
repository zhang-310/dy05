import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeArray as normalizeResponseArray,
  normalizePage as normalizeResponsePage,
  normalizeRecord,
  normalizeStringArray,
  parseJsonValue,
} from '@/utils/response-normalize'
import type {
  Img2VideoKeyframeMap,
  SvShot,
  SvShotListVO,
  VideoTaskSubmitBody,
  VideoTaskSubmitResult,
  GenerateShotListResult,
  AutoComposeResult,
  VideoGenerationTaskRow,
  PublishTitleResult,
  PublishReviewResult,
  PublishResult,
  QuickGenerateResult,
  SvScript,
  SvScriptGenerateRequest,
  SvScriptQuery,
  SvScriptSaveRequest,
  WorkflowRuntimeStatus,
  WorkflowTemplate,
} from '@/types/shortvideo'

export { normalizeStringArray }

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
  relatedProductIds?: number[]
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

export interface DouyinOfficialReference {
  kbName?: string
  refType?: string
  docId?: number
  chunkId?: number
  title?: string
  contentPreview?: string
  score?: number
}

export interface AiTextGenerateResult {
  content: string
  scene?: string
  officialReferences: DouyinOfficialReference[]
  referencedChunkIds?: string
  aiCallLogId?: number
  officialReferenceRequired?: boolean
  officialReferenceSatisfied?: boolean
  officialReferenceStatus?: string
}

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
  id: number
  templateName: string
  remakeType?: string
  templateType?: string
  structureTemplate?: unknown
  content?: string
  adaptationGuide?: string
  emotionCurve?: string
  bgmStyle?: string
  durationRange?: string
  usageCount?: number
  avgViralScore?: number
  status?: number
  createTime: string
}
export interface RemakeTemplateQuery { page?: number; rows?: number; templateName?: string; remakeType?: string; templateType?: string }

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

export interface GenerateDailyParams {
  personaId: number
  scheduleDate: string
  count?: number
  style?: string
  duration?: string
  topic?: string
}

export interface ContentCalendarAutoGenerateParams {
  personaId: number
  from: string
  to: string
}

export interface ContentCalendarSaveParams {
  id?: number
  personaId?: number
  planDate: string
  contentType: string
  title?: string
  brief?: string
  scriptId?: number
  projectId?: number
  priority?: number
  publishTime?: string
  accountId?: number
  tags?: string
  status?: number
}

export interface PersonaMatchResult {
  personaId?: number
  id?: number
  personaName?: string
  matchScore?: number
  reason?: string
  tags?: string[] | string
  [key: string]: unknown
}

function contentCalendarDate(row: Record<string, unknown>): string {
  return String(
    row.planDate ??
    row.plan_date ??
    row.publishDate ??
    row.publish_date ??
    row.scheduledDate ??
    row.scheduled_date ??
    row.date ??
    '',
  ).slice(0, 10)
}

function contentCalendarTitle(row: Record<string, unknown>): string {
  return String(row.title ?? row.videoTitle ?? row.video_title ?? row.contentTitle ?? row.content_title ?? row.name ?? row.brief ?? '')
}

function contentCalendarStatus(row: Record<string, unknown>): string {
  const type = String(row.type ?? row.contentType ?? row.content_type ?? '').toLowerCase()
  const status = String(row.status ?? '').toLowerCase()
  if (type.includes('published') || ['2', 'published', 'done', 'finished'].includes(status)) return 'published'
  return 'planned'
}

function contentCalendarRows(raw: unknown): Array<Record<string, unknown>> {
  return normalizeResponseArray<unknown>(raw)
    .map((item) => normalizeObject<Record<string, unknown>>(item))
    .filter((item) => Object.keys(item).length > 0)
}

/** 将内容日历聚合接口的 days 或行列表转为页面用数组 */
export function adaptContentCalendarMonthView(data: unknown): Array<{ date: string; items: Array<{ title: string; status: string }> }> {
  const value = normalizeObject<Record<string, unknown>>(data)
  const daysValue = parseJsonValue(value.days)
  if (daysValue && typeof daysValue === 'object' && !Array.isArray(daysValue)) {
    const days = daysValue as Record<string, unknown>
    return Object.keys(days).sort().map((date) => ({
      date,
      items: contentCalendarRows(days[date]).map((it) => ({
        title: contentCalendarTitle(it),
        status: contentCalendarStatus(it),
      })),
    }))
  }

  const grouped = new Map<string, Array<{ title: string; status: string }>>()
  for (const row of contentCalendarRows(data)) {
    const date = contentCalendarDate(row)
    if (!date) continue
    const items = grouped.get(date) ?? []
    items.push({
      title: contentCalendarTitle(row),
      status: contentCalendarStatus(row),
    })
    grouped.set(date, items)
  }
  return Array.from(grouped.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, items]) => ({ date, items }))
}

/** 内容日历计划条目 → 发布页/日更用扁平列表 */
export function adaptContentCalendarDateRange(entries: unknown): Array<Record<string, unknown>> {
  return contentCalendarRows(entries).map((e) => {
    const date = contentCalendarDate(e)
    return {
      id: e.id,
      date,
      publishDate: date,
      scheduledDate: date,
      title: contentCalendarTitle(e),
      status: e.status,
      projectId: e.projectId ?? e.project_id,
      accountId: e.accountId ?? e.account_id,
      platform: e.contentType ?? e.content_type,
      contentType: e.contentType ?? e.content_type,
      publishTime: e.publishTime ?? e.publish_time,
      priority: e.priority,
      tags: e.tags,
    }
  })
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

function normalizeSvProject(raw: unknown): SvProject {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...(value as Partial<SvProject>),
    id: readFirstNumber(value, ['id', 'projectId', 'project_id']) ?? 0,
    ownerId: readFirstNumber(value, ['ownerId', 'owner_id']),
    accountId: readFirstNumber(value, ['accountId', 'account_id']),
    title: readFirstText(value, ['title', 'projectTitle', 'project_title', 'name']),
    projectType: readFirstText(value, ['projectType', 'project_type', 'type']) || 'daily',
    personaId: readFirstNumber(value, ['personaId', 'persona_id']),
    scheduleDate: readFirstText(value, ['scheduleDate', 'schedule_date', 'planDate', 'plan_date']) || undefined,
    shootStatus: readFirstText(value, ['shootStatus', 'shoot_status']) || undefined,
    status: readFirstText(value, ['status', 'state']) || undefined,
    scriptId: readFirstNumber(value, ['scriptId', 'script_id']),
    shotListId: readFirstNumber(value, ['shotListId', 'shot_list_id']),
    finalVideoUrl: readFirstText(value, ['finalVideoUrl', 'final_video_url', 'videoUrl', 'video_url', 'outputUrl', 'output_url']) || undefined,
    thumbnailUrl: readFirstText(value, ['thumbnailUrl', 'thumbnail_url', 'coverUrl', 'cover_url']) || undefined,
    characterReferenceUrl: readFirstText(value, ['characterReferenceUrl', 'character_reference_url', 'characterRefUrl', 'character_ref_url']) || undefined,
    sceneReferenceUrl: readFirstText(value, ['sceneReferenceUrl', 'scene_reference_url', 'sceneRefUrl', 'scene_ref_url']) || undefined,
    duration: readFirstNumber(value, ['duration', 'durationSec', 'duration_sec']),
    publishTitle: readFirstText(value, ['publishTitle', 'publish_title']) || undefined,
    publishPlatforms: readFirstText(value, ['publishPlatforms', 'publish_platforms']) || undefined,
    publishTime: readFirstText(value, ['publishTime', 'publish_time']) || undefined,
    reviewStatus: readFirstText(value, ['reviewStatus', 'review_status']) || undefined,
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    updateTime: readFirstText(value, ['updateTime', 'updatedAt', 'update_time']) || undefined,
  }
}

function normalizeSvProjectPage(raw: unknown): PageResult<SvProject> {
  return normalizeResponsePage<unknown, SvProject>(raw, normalizeSvProject, 0, normalizeResponseArray<unknown>(raw).length)
}

function normalizeNumberResult(raw: unknown): number {
  const value = parseJsonValue(raw)
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const n = Number(value)
    return Number.isFinite(n) ? n : 0
  }
  if (isRecord(value)) {
    const n = readFirstNumber(value, ['id', 'taskId', 'value', 'result'])
    if (n != null) return n
    if (value.data != null) return normalizeNumberResult(value.data)
  }
  return 0
}

function normalizeTextResult(raw: unknown): string {
  const value = parseJsonValue(raw)
  if (typeof value === 'string') return value
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  if (isRecord(value)) {
    const direct = readFirstText(value, ['content', 'scriptContent', 'script', 'result', 'text', 'output', 'title', 'copy'])
    if (direct) return direct
    if (value.data != null) return normalizeTextResult(value.data)
  }
  return ''
}

function normalizeOfficialReferences(raw: unknown): DouyinOfficialReference[] {
  return normalizeResponseArray<Record<string, unknown>>(raw).map(item => ({
    kbName: readFirstText(item, ['kbName', 'kb_name']),
    refType: readFirstText(item, ['refType', 'ref_type']),
    docId: readFirstNumber(item, ['docId', 'doc_id']),
    chunkId: readFirstNumber(item, ['chunkId', 'chunk_id']),
    title: readFirstText(item, ['title', 'name']),
    contentPreview: readFirstText(item, ['contentPreview', 'content_preview', 'preview', 'content']),
    score: readFirstNumber(item, ['score']),
  })).filter(item => item.title || item.contentPreview || item.docId)
}

function normalizeAiTextGenerateResult(raw: unknown): AiTextGenerateResult {
  const value = parseJsonValue(raw)
  const obj = normalizeObject<Record<string, unknown>>(value)
  return {
    content: normalizeTextResult(obj.content ?? obj.copy ?? obj.script ?? obj.result ?? obj.data ?? value),
    scene: readFirstText(obj, ['scene', 'type']),
    officialReferences: normalizeOfficialReferences(obj.officialReferences ?? obj.official_references ?? obj.references ?? obj.ragRefs),
    referencedChunkIds: readFirstText(obj, ['referencedChunkIds', 'referenced_chunk_ids']),
    aiCallLogId: readFirstNumber(obj, ['aiCallLogId', 'ai_call_log_id']),
    officialReferenceRequired: readBoolean(obj, ['officialReferenceRequired', 'official_reference_required']),
    officialReferenceSatisfied: readBoolean(obj, ['officialReferenceSatisfied', 'official_reference_satisfied']),
    officialReferenceStatus: readFirstText(obj, ['officialReferenceStatus', 'official_reference_status']),
  }
}

function normalizeObject<T>(raw: unknown): T {
  return normalizeRecord(raw, ['project']) as T
}

function normalizeQuickGenerateResult(raw: unknown): QuickGenerateResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const result: QuickGenerateResult = {
    projectId: readFirstNumber(value, ['projectId', 'project_id', 'id']) ?? 0,
    scriptId: readFirstNumber(value, ['scriptId', 'script_id']),
    shotListId: readFirstNumber(value, ['shotListId', 'shot_list_id']),
    scriptContent: readFirstText(value, ['scriptContent', 'content', 'script', 'scriptText']) || undefined,
    title: readFirstText(value, ['title', 'projectTitle', 'name']) || undefined,
  }
  const creativeBrief = normalizeObject<Record<string, unknown>>(value.creativeBrief ?? value.creative_brief)
  const productionPlan = normalizeObject<Record<string, unknown>>(value.productionPlan ?? value.production_plan)
  if (Object.keys(creativeBrief).length > 0) result.creativeBrief = creativeBrief
  if (Object.keys(productionPlan).length > 0) result.productionPlan = productionPlan
  return result
}

function normalizeGenerateShotListResult(raw: unknown): GenerateShotListResult {
  const value = parseJsonValue(raw)
  if (Array.isArray(value)) {
    return { shots: value as SvShot[] }
  }
  const obj = normalizeObject<Record<string, unknown>>(value)
  return {
    shotListId: readFirstNumber(obj, ['shotListId', 'shot_list_id', 'id']),
    shots: normalizeResponseArray<SvShot>(obj.shots ?? obj.list ?? obj.records ?? obj.items ?? obj.rows ?? obj.content ?? obj.data),
  }
}

function normalizeSvShot(raw: unknown): SvShot {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...(value as Partial<SvShot>),
    id: readFirstNumber(value, ['id', 'shotId', 'shot_id']),
    shotListId: readFirstNumber(value, ['shotListId', 'shot_list_id']),
    shotNumber: readFirstNumber(value, ['shotNumber', 'shot_number', 'number', 'index']),
    timeRange: readFirstText(value, ['timeRange', 'time_range']) || undefined,
    sceneDescription: readFirstText(value, ['sceneDescription', 'scene_description', 'scene', 'description']) || undefined,
    dialogue: readFirstText(value, ['dialogue', 'dialog', 'script']) || undefined,
    cameraAngle: readFirstText(value, ['cameraAngle', 'camera_angle']) || undefined,
    cameraType: readFirstText(value, ['cameraType', 'camera_type']) || undefined,
    action: readFirstText(value, ['action']) || undefined,
    mood: readFirstText(value, ['mood']) || undefined,
    reviewStatus: readFirstText(value, ['reviewStatus', 'review_status']) || undefined,
    reviewerNote: readFirstText(value, ['reviewerNote', 'reviewer_note']) || undefined,
    keyframeUrl: readFirstText(value, ['keyframeUrl', 'keyframe_url', 'imageUrl', 'image_url']) || undefined,
    keyframeBosKey: readFirstText(value, ['keyframeBosKey', 'keyframe_bos_key']) || undefined,
    endFrameUrl: readFirstText(value, ['endFrameUrl', 'end_frame_url']) || undefined,
    endFrameBosKey: readFirstText(value, ['endFrameBosKey', 'end_frame_bos_key']) || undefined,
    videoUrl: readFirstText(value, ['videoUrl', 'video_url', 'clipUrl', 'clip_url']) || undefined,
    videoBosKey: readFirstText(value, ['videoBosKey', 'video_bos_key']) || undefined,
    audioUrl: readFirstText(value, ['audioUrl', 'audio_url']) || undefined,
    audioBosKey: readFirstText(value, ['audioBosKey', 'audio_bos_key']) || undefined,
    duration: readFirstNumber(value, ['duration', 'durationSec', 'duration_sec']),
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    updateTime: readFirstText(value, ['updateTime', 'updatedAt', 'update_time']) || undefined,
  }
}

function normalizeSvShotList(raw: unknown): SvShotListVO {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const shots = normalizeResponseArray<unknown>(value.shots ?? value.list ?? value.records ?? value.items ?? value.rows ?? value.content ?? raw)
    .map(normalizeSvShot)
    .filter((shot) => shot.id != null || shot.sceneDescription || shot.keyframeUrl || shot.videoUrl)
  return {
    ...(value as Partial<SvShotListVO>),
    id: readFirstNumber(value, ['id', 'shotListId', 'shot_list_id']) ?? 0,
    scriptId: readFirstNumber(value, ['scriptId', 'script_id']),
    shotCount: readFirstNumber(value, ['shotCount', 'shot_count', 'count']) ?? shots.length,
    shots,
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    updateTime: readFirstText(value, ['updateTime', 'updatedAt', 'update_time']) || undefined,
  }
}

function normalizeVideoTaskSubmitResult(raw: unknown): VideoTaskSubmitResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    taskId: readFirstNumber(value, ['taskId', 'task_id', 'id']) ?? 0,
  }
}

function normalizeVideoTaskStatus(raw: unknown): VideoGenerationTaskStatus {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const progress = readFirstNumber(value, ['progress', 'percent', 'progressCurrent', 'progress_current'])
  const total = readFirstNumber(value, ['progressTotal', 'progress_total', 'total'])
  return {
    taskId: readFirstNumber(value, ['taskId', 'task_id', 'id']) ?? 0,
    status: readFirstText(value, ['status', 'state']) || 'unknown',
    progressCurrent: readFirstNumber(value, ['progressCurrent', 'progress_current', 'current']) ?? progress ?? 0,
    progressTotal: total ?? (progress != null ? 100 : 0),
    message: readFirstText(value, ['message', 'statusMessage', 'status_message', 'detail']),
    errorMessage: readFirstText(value, ['errorMessage', 'error_message', 'error']) || undefined,
  }
}

function normalizeVideoTaskRow(raw: unknown): VideoGenerationTaskRow {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...(value as Partial<VideoGenerationTaskRow>),
    id: readFirstNumber(value, ['id', 'taskId', 'task_id']) ?? 0,
    taskType: readFirstText(value, ['taskType', 'task_type', 'type']) || 'unknown',
    status: readFirstText(value, ['status', 'state']) || 'unknown',
    progress: readFirstNumber(value, ['progress', 'percent', 'progressCurrent', 'progress_current']) ?? 0,
    projectId: readFirstNumber(value, ['projectId', 'project_id']),
    shotListId: readFirstNumber(value, ['shotListId', 'shot_list_id']),
    priority: readFirstNumber(value, ['priority']),
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    errorMessage: readFirstText(value, ['errorMessage', 'error_message', 'error']) || undefined,
    outputUrl: readFirstText(value, ['outputUrl', 'output_url', 'finalVideoUrl', 'final_video_url', 'videoUrl', 'video_url']) || undefined,
    videos: normalizeResponseArray(value.videos ?? value.videoResults ?? value.video_results),
  }
}

function normalizeVideoTaskPage(raw: unknown): PageResult<VideoGenerationTaskRow> {
  const page = normalizeResponsePage<unknown, VideoGenerationTaskRow>(raw, normalizeVideoTaskRow, 0, normalizeResponseArray<unknown>(raw).length)
  return {
    ...page,
    list: page.list
      .filter((item) => Number(item.id) > 0 || item.taskType !== 'unknown' || item.status !== 'unknown'),
  }
}

function normalizeAutoComposeResult(raw: unknown): AutoComposeResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...(value as Partial<AutoComposeResult>),
    finalVideoUrl: readFirstText(value, ['finalVideoUrl', 'final_video_url', 'videoUrl', 'video_url', 'outputUrl', 'output_url', 'url']) || undefined,
    bosKey: readFirstText(value, ['bosKey', 'bos_key', 'key']) || undefined,
    duration: readFirstNumber(value, ['duration', 'durationSec', 'duration_sec']),
    thumbnail: readFirstText(value, ['thumbnail', 'thumbnailUrl', 'thumbnail_url', 'coverUrl', 'cover_url']) || undefined,
  }
}

function normalizeWorkflowRuntimeStatus(raw: unknown): WorkflowRuntimeStatus {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...value,
    taskId: readFirstText(value, ['taskId', 'task_id', 'id']) || undefined,
    projectId: readFirstNumber(value, ['projectId', 'project_id']),
    status: readFirstText(value, ['status', 'state']) || 'unknown',
    currentStep: readFirstText(value, ['currentStep', 'current_step', 'step']) || 'unknown',
    progress: readFirstNumber(value, ['progress', 'percent']) ?? 0,
    pipeline: readFirstText(value, ['pipeline']) || undefined,
    steps: normalizeResponseArray(value.steps ?? value.stepList ?? value.step_list),
    scriptId: readFirstNumber(value, ['scriptId', 'script_id']),
    shotListId: readFirstNumber(value, ['shotListId', 'shot_list_id']),
    digitalHumanVideoUrl: readFirstText(value, ['digitalHumanVideoUrl', 'digital_human_video_url']) || undefined,
    digitalHumanPendingUrl: readFirstText(value, ['digitalHumanPendingUrl', 'digital_human_pending_url']) || undefined,
    digitalHumanSkipped: value.digitalHumanSkipped === true || value.digital_human_skipped === true,
    digitalHumanSkipReason: readFirstText(value, ['digitalHumanSkipReason', 'digital_human_skip_reason']) || undefined,
    productBrollKeyframeCount: readFirstNumber(value, ['productBrollKeyframeCount', 'product_broll_keyframe_count']),
    productBrollVideoCount: readFirstNumber(value, ['productBrollVideoCount', 'product_broll_video_count']),
    voiceClipCount: readFirstNumber(value, ['voiceClipCount', 'voice_clip_count']),
    composeVideoCount: readFirstNumber(value, ['composeVideoCount', 'compose_video_count']),
    finalVideoUrl: readFirstText(value, ['finalVideoUrl', 'final_video_url', 'videoUrl', 'video_url']) || undefined,
    errorMessage: readFirstText(value, ['errorMessage', 'error_message', 'error']) || undefined,
    message: readFirstText(value, ['message', 'msg']) || undefined,
  }
}

function normalizeMaterialRow(raw: unknown): Record<string, unknown> {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...value,
    id: readFirstNumber(value, ['id', 'materialId', 'material_id']) ?? value.id,
    projectId: readFirstNumber(value, ['projectId', 'project_id']) ?? value.projectId,
    url: readFirstText(value, ['url', 'fileUrl', 'file_url', 'cdnUrl', 'cdn_url']) || value.url,
    fileUrl: readFirstText(value, ['fileUrl', 'file_url', 'url', 'cdnUrl', 'cdn_url']) || value.fileUrl,
    materialType: readFirstText(value, ['materialType', 'material_type', 'fileType', 'file_type', 'mimeType', 'mime_type']) || value.materialType,
    fileType: readFirstText(value, ['fileType', 'file_type', 'materialType', 'material_type', 'mimeType', 'mime_type']) || value.fileType,
    fileSize: readFirstNumber(value, ['fileSize', 'file_size', 'size']) ?? value.fileSize,
    duration: readFirstNumber(value, ['duration', 'durationSec', 'duration_sec']) ?? value.duration,
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || value.createTime,
  }
}

function normalizeMaterialPage(raw: unknown): PageResult<Record<string, unknown>> {
  return normalizeResponsePage<unknown, Record<string, unknown>>(raw, normalizeMaterialRow, 0, normalizeResponseArray<unknown>(raw).length)
}

function normalizeAccountCollectTask(raw: unknown): AccountCollectTask {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...(value as Partial<AccountCollectTask>),
    id: readFirstNumber(value, ['id', 'taskId', 'task_id']) ?? 0,
    ownerId: readFirstNumber(value, ['ownerId', 'owner_id']),
    accountId: readFirstNumber(value, ['accountId', 'account_id']),
    svAccountId: readFirstNumber(value, ['svAccountId', 'sv_account_id']),
    accountUrl: readFirstText(value, ['accountUrl', 'account_url', 'url']) || null,
    accountName: readFirstText(value, ['accountName', 'account_name', 'nickname', 'nickName']) || null,
    secUid: readFirstText(value, ['secUid', 'sec_uid', 'secUserId']) || null,
    inputType: readFirstText(value, ['inputType', 'input_type', 'type']) || null,
    originalInput: readFirstText(value, ['originalInput', 'original_input', 'input', 'keyword']) || null,
    status: readFirstText(value, ['status', 'state']) || 'pending',
    totalVideos: readFirstNumber(value, ['totalVideos', 'total_videos', 'total']),
    collectedVideos: readFirstNumber(value, ['collectedVideos', 'collected_videos', 'collected']),
    analyzedVideos: readFirstNumber(value, ['analyzedVideos', 'analyzed_videos', 'analyzed']),
    indexedVideos: readFirstNumber(value, ['indexedVideos', 'indexed_videos', 'indexed']),
    targetKbId: readFirstNumber(value, ['targetKbId', 'target_kb_id']),
    errorMessage: readFirstText(value, ['errorMessage', 'error_message', 'error', 'message']) || null,
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    updateTime: readFirstText(value, ['updateTime', 'updatedAt', 'update_time']) || undefined,
  }
}

function normalizeAccountCollectTaskPage(raw: unknown): PageResult<AccountCollectTask> {
  return normalizeResponsePage<unknown, AccountCollectTask>(raw, normalizeAccountCollectTask, 0, normalizeResponseArray<unknown>(raw).length)
}

function normalizeCollectedVideo(raw: unknown): CollectedVideo {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    ...(value as Partial<CollectedVideo>),
    id: readFirstNumber(value, ['id', 'viralVideoId', 'viral_video_id']) ?? 0,
    taskId: readFirstNumber(value, ['taskId', 'task_id']) ?? 0,
    videoId: readFirstText(value, ['videoId', 'video_id', 'awemeId', 'aweme_id']),
    title: readFirstText(value, ['title', 'desc', 'description']),
    playCount: readFirstNumber(value, ['playCount', 'play_count', 'viewCount', 'view_count']) ?? 0,
    likeCount: readFirstNumber(value, ['likeCount', 'like_count', 'diggCount', 'digg_count']) ?? 0,
    coverUrl: readFirstText(value, ['coverUrl', 'cover_url', 'cover']),
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']),
    evidenceLevel: readFirstText(value, ['evidenceLevel', 'evidence_level']) || undefined,
    transcriptEvidenceLevel: readFirstText(value, ['transcriptEvidenceLevel', 'transcript_evidence_level']) || undefined,
    sceneEvidenceLevel: readFirstText(value, ['sceneEvidenceLevel', 'scene_evidence_level']) || undefined,
    transcriptLabel: readFirstText(value, ['transcriptLabel', 'transcript_label']) || undefined,
    sceneLabel: readFirstText(value, ['sceneLabel', 'scene_label']) || undefined,
    inferenceRisk: value.inferenceRisk != null || value.inference_risk != null
      ? readBooleanAlias(value.inferenceRisk ?? value.inference_risk, false)
      : undefined,
  }
}

function normalizeCollectedVideoPage(raw: unknown): PageResult<CollectedVideo> {
  return normalizeResponsePage<unknown, CollectedVideo>(raw, normalizeCollectedVideo, 0, normalizeResponseArray<unknown>(raw).length)
}

function normalizeMusicRow(raw: unknown): Record<string, unknown> {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const durationSec = readFirstNumber(value, ['durationSec', 'duration_sec'])
  const durationMs = readFirstNumber(value, ['durationMs', 'duration_ms'])
  const duration = readFirstText(value, ['duration', 'durationLabel', 'duration_label'])
    || (durationSec != null ? `${durationSec}秒` : '')
    || (durationMs != null ? `${Math.max(1, Math.round(durationMs / 1000))}秒` : '')
  return {
    ...value,
    id: readFirstNumber(value, ['id', 'logId', 'log_id', 'taskId', 'task_id']) ?? value.id,
    name: readFirstText(value, ['name', 'title', 'description', 'prompt', 'fileName', 'file_name']) || value.name,
    duration: duration || value.duration,
    url: readFirstText(value, ['url', 'audioUrl', 'audio_url', 'musicUrl', 'music_url', 'fileUrl', 'file_url']) || value.url,
    audioUrl: readFirstText(value, ['audioUrl', 'audio_url', 'musicUrl', 'music_url', 'url', 'fileUrl', 'file_url']) || value.audioUrl,
    type: readFirstText(value, ['type', 'musicType', 'music_type', 'generationType', 'generation_type']) || value.type,
    provider: readFirstText(value, ['provider', 'modelProvider', 'model_provider', 'vendor']) || value.provider,
    source: readFirstText(value, ['source', 'sourceType', 'source_type']) || value.source,
    bpm: readFirstNumber(value, ['bpm']) ?? value.bpm,
    degraded: value.degraded != null || value.isDegraded != null || value.is_degraded != null
      ? readBooleanAlias(value.degraded ?? value.isDegraded ?? value.is_degraded, false)
      : value.degraded,
  }
}

function normalizeMusicRows(raw: unknown): Record<string, unknown>[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeMusicRow)
    .filter((item) => Object.keys(item).length > 0)
}

function normalizeBgmResult(raw: unknown): Record<string, unknown> {
  return normalizeMusicRow(raw)
}

function normalizeMaterialGenerationResult(raw: unknown): Record<string, unknown> {
  const value = normalizeObject<Record<string, unknown>>(raw)
  if (isRecord(value.result)) return normalizeObject<Record<string, unknown>>(value.result)
  return value
}

function normalizeBooleanObject(raw: unknown): Record<string, unknown> {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const success = readBooleanAlias(value.ok ?? value.success ?? value.status, false)
  return {
    ...value,
    ...(value.ok == null && value.success == null && value.status == null ? {} : { ok: success }),
    id: readFirstNumber(value, ['id', 'viralVideoId', 'viral_video_id', 'taskId', 'task_id']) ?? value.id,
  }
}

function normalizeRemakeTemplate(raw: unknown): RemakeTemplate {
  const value = normalizeFirstRecord(raw, [
    'id', 'templateId', 'template_id', 'templateName', 'template_name', 'name',
    'remakeType', 'remake_type', 'templateType', 'template_type',
    'structureTemplate', 'structure_template', 'adaptationGuide', 'adaptation_guide',
  ])
  const structureTemplate = value.structureTemplate ?? value.structure_template
  return {
    ...(value as Partial<RemakeTemplate>),
    id: readFirstNumber(value, ['id', 'templateId', 'template_id']) ?? 0,
    templateName: readFirstText(value, ['templateName', 'template_name', 'name', 'title']),
    remakeType: readFirstText(value, ['remakeType', 'remake_type', 'templateType', 'template_type']) || undefined,
    templateType: readFirstText(value, ['templateType', 'template_type', 'remakeType', 'remake_type']) || undefined,
    structureTemplate: parseJsonValue(structureTemplate),
    content: readFirstText(value, ['content', 'adaptationGuide', 'adaptation_guide']) || undefined,
    adaptationGuide: readFirstText(value, ['adaptationGuide', 'adaptation_guide', 'content']) || undefined,
    emotionCurve: readFirstText(value, ['emotionCurve', 'emotion_curve']) || undefined,
    bgmStyle: readFirstText(value, ['bgmStyle', 'bgm_style']) || undefined,
    durationRange: readFirstText(value, ['durationRange', 'duration_range']) || undefined,
    usageCount: readFirstNumber(value, ['usageCount', 'usage_count']),
    avgViralScore: readFirstNumber(value, ['avgViralScore', 'avg_viral_score', 'viralScore', 'viral_score']),
    status: readFirstNumber(value, ['status']),
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || '',
  }
}

function normalizeRemakeTemplatePage(raw: unknown): PageResult<RemakeTemplate> {
  const page = normalizeResponsePage<unknown, unknown>(raw, item => item, 0, normalizeResponseArray<unknown>(raw).length)
  return {
    ...page,
    list: page.list
      .map(normalizeRemakeTemplate)
      .filter((item) => Number(item.id) > 0 || !!item.templateName),
  }
}

function normalizeViralVideo(raw: unknown): ViralVideo {
  const value = normalizeFirstRecord(raw, [
    'id', 'viralVideoId', 'viral_video_id', 'title', 'authorName', 'author_name',
    'viewCount', 'view_count', 'playCount', 'play_count', 'deepAnalysisResult', 'deep_analysis_result',
  ])
  const keyframeBosUrlsRaw = value.keyframeBosUrls ?? value.keyframe_bos_urls
  const keyframeBosUrls = typeof keyframeBosUrlsRaw === 'string' || Array.isArray(keyframeBosUrlsRaw)
    ? keyframeBosUrlsRaw as string | string[]
    : undefined
  return {
    ...(value as Partial<ViralVideo>),
    id: readFirstNumber(value, ['id', 'viralVideoId', 'viral_video_id']) ?? 0,
    title: readFirstText(value, ['title', 'videoTitle', 'video_title', 'desc', 'description']),
    authorName: readFirstText(value, ['authorName', 'author_name', 'nickname', 'accountName', 'account_name']),
    viewCount: readFirstNumber(value, ['viewCount', 'view_count', 'playCount', 'play_count']),
    playCount: readFirstNumber(value, ['playCount', 'play_count', 'viewCount', 'view_count']),
    likeCount: readFirstNumber(value, ['likeCount', 'like_count']),
    commentCount: readFirstNumber(value, ['commentCount', 'comment_count']),
    favoriteCount: readFirstNumber(value, ['favoriteCount', 'favorite_count', 'collectCount', 'collect_count']),
    collectCount: readFirstNumber(value, ['collectCount', 'collect_count', 'favoriteCount', 'favorite_count']),
    shareCount: readFirstNumber(value, ['shareCount', 'share_count']),
    videoUrl: readFirstText(value, ['videoUrl', 'video_url', 'shareUrl', 'share_url']) || undefined,
    coverUrl: readFirstText(value, ['coverUrl', 'cover_url']) || undefined,
    coverBosUrl: readFirstText(value, ['coverBosUrl', 'cover_bos_url']) || undefined,
    videoBosUrl: readFirstText(value, ['videoBosUrl', 'video_bos_url']) || undefined,
    keyframeBosUrls,
    sceneDescriptions: readFirstText(value, ['sceneDescriptions', 'scene_descriptions']) || undefined,
    videoDuration: readFirstNumber(value, ['videoDuration', 'video_duration', 'duration']),
    description: readFirstText(value, ['description', 'desc']) || undefined,
    douyinVideoId: readFirstText(value, ['douyinVideoId', 'douyin_video_id', 'awemeId', 'aweme_id']) || undefined,
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    viralScore: readFirstNumber(value, ['viralScore', 'viral_score', 'score']),
    deepAnalyzeStatus: readFirstText(value, ['deepAnalyzeStatus', 'deep_analyze_status', 'analyzeStatus', 'status']) || undefined,
    deepAnalyzeProgress: readFirstText(value, ['deepAnalyzeProgress', 'deep_analyze_progress', 'progress']) || undefined,
    deepAnalyzeSteps: readFirstText(value, ['deepAnalyzeSteps', 'deep_analyze_steps']) || undefined,
    deepAnalysisResult: readFirstText(value, ['deepAnalysisResult', 'deep_analysis_result', 'analysisResult', 'analysis_result']) || undefined,
    lightAnalysisResult: readFirstText(value, ['lightAnalysisResult', 'light_analysis_result']) || undefined,
    transcript: readFirstText(value, ['transcript', 'asrText', 'asr_text']) || undefined,
  }
}

function normalizeViralVideos(raw: unknown): ViralVideo[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeViralVideo)
    .filter((item) => Number(item.id) > 0 || !!item.title)
}

function normalizeHotTopicRows(raw: unknown): Array<Record<string, unknown>> {
  return normalizeResponseArray<Record<string, unknown>>(raw)
}

function readOptionalNumber(raw: unknown): number | undefined {
  if (raw == null || raw === '') return undefined
  const n = Number(raw)
  return Number.isFinite(n) ? n : undefined
}

function readBooleanAlias(raw: unknown, fallback = false): boolean {
  if (typeof raw === 'boolean') return raw
  if (typeof raw === 'number') return raw !== 0
  if (typeof raw === 'string') {
    const value = raw.trim().toLowerCase()
    if (['true', '1', 'success', 'succeeded', 'ok', 'pass', 'passed', 'approved', 'published'].includes(value)) {
      return true
    }
    if (['false', '0', 'fail', 'failed', 'error', 'rejected', 'needs_revision'].includes(value)) {
      return false
    }
  }
  return fallback
}

function readBoolean(obj: Record<string, unknown>, keys: string[]): boolean | undefined {
  for (const key of keys) {
    if (obj[key] != null) return readBooleanAlias(obj[key])
  }
  return undefined
}

function readFirstText(obj: Record<string, unknown>, keys: string[], fallback = ''): string {
  for (const key of keys) {
    const value = obj[key]
    if (typeof value === 'string' && value.trim()) return value.trim()
    if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  }
  return fallback
}

function readFirstNumber(obj: Record<string, unknown>, keys: string[]): number | undefined {
  for (const key of keys) {
    const n = readOptionalNumber(obj[key])
    if (n != null) return n
  }
  return undefined
}

function pickWrappedValue(raw: unknown, keys: string[]): unknown {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) return value
  for (const key of keys) {
    if (value[key] != null) return value[key]
  }
  if (value.data != null) return pickWrappedValue(value.data, keys)
  return value
}

function normalizePublishTitleResult(raw: unknown): PublishTitleResult {
  const source = pickWrappedValue(raw, ['titles', 'titleSuggestions', 'suggestions', 'list', 'records', 'items', 'rows', 'content'])
  const rows = normalizeResponseArray<unknown>(source)
  const titles = rows
    .map((item): PublishTitleResult['titles'][number] | null => {
      if (typeof item === 'string' || typeof item === 'number') {
        const text = String(item).trim()
        return text ? { text } : null
      }
      const value = parseJsonValue(item)
      if (!isRecord(value)) return null
      const text = readFirstText(value, ['text', 'title', 'content', 'name', 'value'])
      if (!text) return null
      const score = readFirstNumber(value, ['score', 'qualityScore', 'confidence', 'weight'])
      return {
        text,
        ...(score != null ? { score } : {}),
      }
    })
    .filter((item): item is PublishTitleResult['titles'][number] => !!item)

  if (titles.length > 0) return { titles }

  const value = normalizeObject<Record<string, unknown>>(raw)
  const text = readFirstText(value, ['text', 'title', 'content'])
  const score = readFirstNumber(value, ['score', 'confidence'])
  return { titles: text ? [{ text, ...(score != null ? { score } : {}) }] : [] }
}

function normalizePublishReviewResult(raw: unknown): PublishReviewResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const passed = readBooleanAlias(
    value.passed ?? value.pass ?? value.ok ?? value.success ?? value.approved ?? value.status,
    false,
  )
  const projectId = readFirstNumber(value, ['projectId', 'id'])
  const officialReferenceRequired = readBoolean(value, ['officialReferenceRequired', 'official_reference_required'])
  const officialReferenceSatisfied = readBoolean(value, ['officialReferenceSatisfied', 'official_reference_satisfied'])
  const officialReferenceStatus = readFirstText(value, ['officialReferenceStatus', 'official_reference_status'])
  return {
    passed,
    issues: normalizeStringArray(value.issues ?? value.problems ?? value.risks ?? value.errors ?? value.violationReasons),
    suggestions: normalizeStringArray(value.suggestions ?? value.advice ?? value.recommendations ?? value.tips),
    projectId,
    officialReferences: normalizeOfficialReferences(value.officialReferences ?? value.official_references ?? value.references),
    ...(officialReferenceRequired !== undefined ? { officialReferenceRequired } : {}),
    ...(officialReferenceSatisfied !== undefined ? { officialReferenceSatisfied } : {}),
    ...(officialReferenceStatus ? { officialReferenceStatus } : {}),
  }
}

function normalizePublishPlatformResult(raw: unknown, fallbackPlatform?: string): PublishResult['results'][number] | null {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) {
    return fallbackPlatform
      ? { platform: fallbackPlatform, success: false, error: value == null ? '未返回平台结果' : String(value) }
      : null
  }
  const platform = readFirstText(value, ['platform', 'channel', 'name'], fallbackPlatform ?? 'unknown')
  const success = readBooleanAlias(value.success ?? value.ok ?? value.passed ?? value.status, false)
  const itemId = readFirstText(value, ['itemId', 'videoId', 'awemeId', 'publishId', 'id'])
  const error = readFirstText(value, ['error', 'message', 'reason', 'errorMessage'])
  return {
    platform,
    success,
    itemId: itemId || undefined,
    error: error || undefined,
  }
}

function normalizePublishResult(raw: unknown): PublishResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const source = pickWrappedValue(raw, ['results', 'platformResults', 'list', 'records', 'items', 'rows', 'content'])
  let results = normalizeResponseArray<unknown>(source)
    .map((item) => normalizePublishPlatformResult(item, readFirstText(value, ['platform', 'channel'])))
    .filter((item): item is PublishResult['results'][number] => !!item)

  if (results.length === 0) {
    const single = normalizePublishPlatformResult(value, readFirstText(value, ['platform', 'channel'], 'douyin'))
    results = single ? [single] : []
  }

  const explicitSuccess = value.success ?? value.ok ?? value.passed ?? value.status
  const success = explicitSuccess != null
    ? readBooleanAlias(explicitSuccess, results.some((item) => item.success))
    : results.some((item) => item.success)
  const degraded = value.degraded != null
    ? readBooleanAlias(value.degraded, false)
    : results.some((item) => !item.success) || !success

  return {
    success,
    degraded,
    projectId: readFirstNumber(value, ['projectId', 'id']),
    platform: readFirstText(value, ['platform', 'channel']) || undefined,
    results,
  }
}

function normalizeFirstRecord(raw: unknown, domainKeys: string[]): Record<string, unknown> {
  const value = parseJsonValue(raw)
  if (Array.isArray(value)) return normalizeFirstRecord(value[0], domainKeys)
  if (!isRecord(value)) return {}
  if (domainKeys.some((key) => value[key] != null)) return value

  for (const key of ['record', 'item', 'template', 'detail', 'data', 'list', 'records', 'items', 'rows', 'content']) {
    if (value[key] == null) continue
    const nested = normalizeFirstRecord(value[key], domainKeys)
    if (Object.keys(nested).length > 0) return nested
  }

  return value
}

function normalizeWorkflowTemplate(raw: unknown): WorkflowTemplate {
  const value = normalizeFirstRecord(raw, [
    'id', 'templateId', 'template_id', 'templateName', 'template_name', 'name', 'title',
    'steps', 'workflowSteps', 'stepList', 'step_list',
  ])
  const stepsRaw = value.steps ?? value.workflowSteps ?? value.stepList ?? value.step_list
  const parsedSteps = parseJsonValue(stepsRaw)
  let steps: WorkflowTemplate['steps'] | undefined
  if (Array.isArray(parsedSteps)) {
    steps = parsedSteps as WorkflowTemplate['steps']
  } else if (typeof stepsRaw === 'string') {
    steps = stepsRaw
  } else {
    const rows = normalizeResponseArray<unknown>(stepsRaw)
    if (rows.length > 0) steps = rows as WorkflowTemplate['steps']
  }

  const isSystemRaw = value.isSystem ?? value.system ?? value.is_system
  const isSystem = typeof isSystemRaw === 'boolean'
    ? (isSystemRaw ? 1 : 0)
    : readOptionalNumber(isSystemRaw)
  const templateName = readFirstText(value, ['templateName', 'name', 'title', 'template_name'])

  return {
    id: readFirstNumber(value, ['id', 'templateId', 'template_id']) ?? 0,
    templateName: templateName || undefined,
    name: readFirstText(value, ['name', 'templateName', 'title', 'template_name']) || undefined,
    description: readFirstText(value, ['description', 'desc', 'summary']) || undefined,
    steps,
    isSystem,
    createTime: readFirstText(value, ['createTime', 'createdAt', 'create_time']) || undefined,
    updateTime: readFirstText(value, ['updateTime', 'updatedAt', 'update_time']) || undefined,
  }
}

function normalizeWorkflowTemplates(raw: unknown): WorkflowTemplate[] {
  const rows = normalizeResponseArray<unknown>(raw)
  if (rows.length > 0) {
    return rows
      .map(normalizeWorkflowTemplate)
      .filter((item) => Number(item.id) > 0 || !!item.templateName || !!item.name)
  }
  const single = normalizeWorkflowTemplate(raw)
  return Number(single.id) > 0 || !!single.templateName || !!single.name ? [single] : []
}

function normalizePublishTimeRows(raw: unknown): Array<Record<string, unknown>> {
  return normalizeResponseArray<unknown>(raw)
    .map((item): Record<string, unknown> => {
      if (typeof item === 'string' || typeof item === 'number') {
        return { label: String(item).trim() }
      }
      const value = normalizeObject<Record<string, unknown>>(item)
      const label = readFirstText(value, ['label', 'timeLabel', 'time_label', 'publishTime', 'publish_time', 'slot', 'value'])
      const hour = readFirstNumber(value, ['hour', 'publishHour', 'publish_hour'])
      const weekday = readFirstNumber(value, ['weekday', 'dayOfWeek', 'day_of_week'])
      return {
        ...value,
        ...(label ? { label } : {}),
        ...(hour != null ? { hour } : {}),
        ...(weekday != null ? { weekday } : {}),
        avgViewCount: readFirstNumber(value, ['avgViewCount', 'avg_view_count', 'expectedViews', 'expected_views']) ?? value.avgViewCount,
        score: readFirstNumber(value, ['score', 'weight']) ?? value.score,
      }
    })
    .filter((item) => Object.keys(item).length > 0 && String(item.label ?? item.hour ?? item.value ?? '').trim() !== '')
}

export const shortvideoApi = {
  list: (params: SvProjectQuery) => request.post<unknown>('/short-video/project/list', params).then(normalizeSvProjectPage),
  get: (id: number) => request.post<unknown>('/short-video/project/get', { id }).then(normalizeSvProject),
  save: (params: SvProjectSave) => request.post<number>('/short-video/project/save', params),
  delete: (id: number) => request.post<void>('/short-video/project/delete', { id }),
  generateDaily: (params: GenerateDailyParams) =>
    request.post<unknown>('/short-video/project/generate-daily', params).then(normalizeObject<Record<string, unknown>>),
  exportScript: (id: number) => request.post<string>('/short-video/project/export-script', { projectId: id }),
  quickGenerate: (params: { theme: string; keywords?: string; style?: string }) =>
    request.post<unknown>('/short-video/quick/generate', params).then(normalizeQuickGenerateResult),

  publishGenerateTitle: (params: { projectId?: number; videoUrl?: string; script?: string; keywords?: string[]; count?: number }) =>
    request.post<unknown>('/short-video/publish/generate-title', params).then(normalizePublishTitleResult),
  publishAiReview: (params: { projectId?: number; videoUrl?: string; title?: string; cover?: string; coverUrl?: string }) =>
    request.post<unknown>('/short-video/publish/ai-review', params).then(normalizePublishReviewResult),
  publishSubmit: (params: { projectId?: number; videoUrl?: string; title?: string; platforms?: string[]; publishTime?: string }) =>
    request.post<unknown>('/short-video/publish/publish', params).then(normalizePublishResult),
  publishDouyin: (params: { projectId?: number; videoUrl?: string; title?: string; publishTime?: string }) =>
    request.post<unknown>('/short-video/publish/douyin', params).then(normalizePublishResult),

  videoSearch: (params: SvVideoQuery) =>
    request.post<unknown>('/short-video/content/search', params)
      .then(raw => normalizeResponsePage<SvVideo, SvVideo>(raw, item => item, Number(params.page ?? 0), Number(params.rows ?? 20))),
  videoGet: (id: number) => request.post<unknown>('/short-video/content/get', { id }).then(normalizeObject<SvVideo>),
  videoSave: (params: Record<string, unknown>) => request.post<unknown>('/short-video/content/save', params).then(normalizeNumberResult),
  videoDelete: (id: number) => request.post<void>('/short-video/content/delete', { id }),
  /** 单条视频指标趋势（需 videoId） */
  videoDataTrend: (videoId: number) => request.post<unknown>('/short-video/content/data-trend', { videoId }).then(normalizeResponseArray<Record<string, unknown>>),

  collectStart: (params: {
    input: string
    maxCount?: number
    accountId?: number
    targetKbId?: number
    /** keyword_video：按关键词打开抖音视频搜索页采集 */
    collectMode?: 'keyword_video'
  }) => request.post<unknown>('/short-video/account-collect/start', params).then(normalizeAccountCollectTask),
  collectList: (params: { page?: number; rows?: number }) =>
    request.post<unknown>('/short-video/account-collect/list', params).then(normalizeAccountCollectTaskPage),
  collectStatus: (taskId: number) => request.post<unknown>('/short-video/account-collect/status', { taskId }).then(normalizeAccountCollectTask),
  collectCancel: (taskId: number) => request.post<void>('/short-video/account-collect/cancel', { taskId }),
  collectRetry: (taskId: number) => request.post<unknown>('/short-video/account-collect/retry', { taskId }).then(normalizeAccountCollectTask),
  collectVideos: (taskId: number, params?: { page?: number; rows?: number }) =>
    request.post<unknown>('/short-video/account-collect/videos', { taskId, ...params }).then(normalizeCollectedVideoPage),

  remakeTemplateList: (params: RemakeTemplateQuery) =>
    request.post<unknown>('/short-video/remake-template/list', {
      ...params,
      remakeType: params.remakeType ?? params.templateType,
      templateType: undefined,
    }).then(normalizeRemakeTemplatePage),
  remakeTemplateSave: (params: Partial<RemakeTemplate>) =>
    request.post<unknown>('/short-video/remake-template/save', params).then(normalizeRemakeTemplate),
  remakeTemplateDelete: (id: number) => request.post<void>('/short-video/remake-template/delete', { id }),
  remakeTemplateGenerate: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/remake-template/generate', params).then(normalizeTextResult),
  remakeTemplateCreateFromViral: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/remake-template/create-from-viral', params).then(normalizeRemakeTemplate),

  /** 爆款列表（后端返回数组，无分页包装） */
  viralList: (params: ViralVideoQuery) =>
    request.post<unknown>('/short-video/viral/list', {
      mode: params.mode ?? 'my',
      category: params.keyword ?? params.category,
      sortBy: params.sortBy ?? 'viralScore',
      page: params.page ?? 0,
      rows: params.rows ?? 20,
    }).then(normalizeViralVideos),
  /** 单条详情（含深度拆解字段，列表可能截断或字段不全时用于刷新） */
  viralGet: (id: number) => request.post<unknown>('/short-video/viral/get', { id }).then(normalizeViralVideo),
  viralAnalyze: (id: number) => request.post<unknown>('/short-video/viral/analyze', { id }).then(normalizeBooleanObject),
  /** 收藏爆款（写入 sv_viral_favorite） */
  viralCollect: (viralVideoId: number) => request.post<unknown>('/short-video/viral/collect', { viralVideoId }).then(normalizeNumberResult),
  /** @deprecated 使用 viralCollect */
  viralFavorite: (viralVideoId: number) => request.post<unknown>('/short-video/viral/collect', { viralVideoId }).then(normalizeNumberResult),

  aiGenerateCopy: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/ai/generate-copy', params).then(normalizeTextResult),
  aiGenerateCopyRich: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/ai/generate-copy-rich', params).then(normalizeAiTextGenerateResult),
  aiGenerateScript: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/ai/generate-script', params).then(normalizeTextResult),
  aiGenerateScriptRich: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/ai/generate-script-rich', params).then(normalizeAiTextGenerateResult),
  aiGenerateTitle: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/ai/generate-title', params).then(normalizeTextResult),
  aiGeneratePlanRich: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/ai/generate-plan-rich', params).then(normalizeAiTextGenerateResult),
  aiCheckViolation: (text: string) => request.post<Record<string, unknown>>('/short-video/ai/check-violation', { text }),
  aiCheckViolationRich: (text: string) => request.post<Record<string, unknown>>('/short-video/ai/check-violation-rich', { text }),

  svScriptList: (params: SvScriptQuery) =>
    request.post<unknown>('/short-video/script/list', params)
      .then(raw => normalizeResponsePage<SvScript, SvScript>(raw, item => item, Number(params.page ?? 0), Number(params.rows ?? 20))),
  svScriptGet: (id: number) => request.post<unknown>('/short-video/script/get', { id }).then(normalizeObject<SvScript>),
  svScriptSave: (params: SvScriptSaveRequest) =>
    request.post<unknown>('/short-video/script/save', params).then(normalizeNumberResult),
  svScriptDelete: (id: number) => request.post<void>('/short-video/script/delete', { id }),
  svScriptGenerate: (params: SvScriptGenerateRequest) =>
    request.post<unknown>('/short-video/script/generate', params).then(normalizeTextResult),

  shotListGet: (id: number) =>
    request.post<unknown>('/short-video/shot-list/get', { id }).then(normalizeSvShotList),
  shotListGetByScript: (scriptId: number) =>
    request.post<unknown>('/short-video/shot-list/get-by-script', { scriptId }).then(normalizeSvShotList),
  shotListGenerate: (params: { scriptId?: number; scriptContent: string; shotCount?: number; style?: string }) =>
    request.post<unknown>('/short-video/shot-list/generate', params).then(normalizeGenerateShotListResult),
  shotListSave: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/shot-list/save', params).then(normalizeNumberResult),
  shotDelete: (shotId: number) => request.post<void>('/short-video/shot-list/delete-shot', { shotId }),
  materialGenerateKeyframes: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/material/generate-keyframes', params).then(normalizeMaterialGenerationResult),
  materialGenerateVoiceBatch: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/material/generate-voice-batch', params).then(normalizeMaterialGenerationResult),
  materialImg2VideoBatch: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/material/img2video-batch', params).then(normalizeMaterialGenerationResult),

  /** 素材库分页（非 material 生产接口） */
  materialList: (params: { page?: number; rows?: number; materialType?: string; projectId?: number }) =>
    request.post<unknown>('/short-video/library/list', params).then(normalizeMaterialPage),
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

  dramaList: () => request.post<unknown>('/short-video/drama/list', {}).then(normalizeResponseArray<SvDrama>),
  dramaCreate: (body: { title: string; description?: string; genre?: string; totalEpisodes?: number }) =>
    request.post<unknown>('/short-video/drama/create', body).then(normalizeObject<SvDrama>),
  dramaUpdate: (body: { id: number; title?: string; description?: string; genre?: string; totalEpisodes?: number }) =>
    request.post<unknown>('/short-video/drama/update', body).then(normalizeObject<SvDrama>),
  dramaDelete: (id: number) => request.post<void>('/short-video/drama/delete', { id }),
  dramaGenerateScript: (body: { dramaId: number; theme?: string; style?: string }) =>
    request.post<unknown>('/short-video/drama/generate-script', body).then(normalizeTextResult),

  videoTaskSubmit: (params: VideoTaskSubmitBody) =>
    request.post<unknown>('/short-video/video-task/submit', params).then(normalizeVideoTaskSubmitResult),
  videoTaskStatus: (taskId: number) => request.post<unknown>('/short-video/video-task/status', { taskId }).then(normalizeVideoTaskStatus),
  videoTaskList: (params: { page?: number; rows?: number; projectId?: number }) =>
    request.post<unknown>('/short-video/video-task/list', params).then(normalizeVideoTaskPage),
  videoTaskCancel: (taskId: number) => request.post<void>('/short-video/video-task/cancel', { taskId }),
  videoTaskRetry: (taskId: number) => request.post<unknown>('/short-video/video-task/retry', { taskId }).then(normalizeObject<Record<string, unknown>>),

  /** Dashboard 播放量等趋势 */
  dataTrend: (params: { days?: number }) =>
    request.post<unknown>('/short-video/dashboard/trend', params).then(normalizeResponseArray<Record<string, unknown>>),
  dashboardStats: () =>
    request.post<unknown>('/short-video/dashboard/stats', {}).then(raw => normalizeRecord(raw, ['dashboardStats', 'stats', 'summary']) as Record<string, unknown>),
  dashboardProjects: (params: { page?: number; rows?: number; status?: string }) =>
    request.post<unknown>('/short-video/dashboard/projects', params).then(normalizeResponseArray<Record<string, unknown>>),
  dashboardCostBreakdown: (params?: { projectId?: number }) =>
    request.post<unknown>('/short-video/dashboard/cost-breakdown', params ?? {}).then(raw => normalizeRecord(raw, ['costBreakdown', 'breakdown', 'costs']) as Record<string, unknown>),

  /** 内容日历聚合（月视图 raw Map，含 days） */
  contentCalendarView: (year: number, month: number) =>
    request.post<unknown>('/short-video/content/calendar', { year, month }).then(normalizeObject<Record<string, unknown>>),
  contentCalendarMonthStats: (year: number, month: number) =>
    request.post<unknown>('/short-video/content/calendar-stats', { year, month }).then(normalizeObject<ContentCalendarMonthStats>),
  /** accountId 必填 */
  contentPublishTimeRecommend: (accountId: number) =>
    request.post<unknown>('/short-video/content/publish-time-recommend', { accountId }).then(normalizePublishTimeRows),

  generateBgm: (params: { styleDescription?: string; durationSec?: number; instrumental?: boolean }) =>
    request.post<unknown>('/short-video/music/generate-bgm', {
      styleDescription: params.styleDescription ?? 'cinematic background music',
      durationSec: params.durationSec ?? 30,
      instrumental: params.instrumental !== false,
    }).then(normalizeBgmResult),
  generateSfx: (params: { sceneDescription?: string; durationSec?: number }) =>
    request.post<unknown>('/short-video/music/generate-sfx', {
      sceneDescription: params.sceneDescription ?? '',
      durationSec: params.durationSec ?? 5,
    }).then(normalizeMusicRows),
  musicHistory: (params?: Record<string, unknown>) =>
    request.post<unknown>('/short-video/music/history', params ?? {}).then(normalizeMusicRows),

  seoSuggestTags: (params: { title: string; description?: string; industry?: string }) =>
    request.post<unknown>('/short-video/seo/suggest-tags', params).then(normalizeStringArray),
  seoSuggestPublishTime: (params: { accountId?: number }) =>
    request.post<unknown>('/short-video/seo/suggest-publish-time', params).then(normalizeStringArray),
  seoSuggestCover: (params: { frameUrls: string[] }) =>
    request.post<string>('/short-video/seo/suggest-cover', params),
  seoSuggestAbTitles: (params: { baseTitle: string }) =>
    request.post<unknown>('/short-video/seo/suggest-ab-titles', params).then(normalizeStringArray),

  contentCalendarCrudList: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/content-calendar/list', params)
      .then(raw => normalizeResponsePage<Record<string, unknown>, Record<string, unknown>>(
        raw,
        item => item,
        Number(params.page ?? 0),
        Number(params.rows ?? 20),
      )),
  contentCalendarSave: (params: ContentCalendarSaveParams) =>
    request.post<unknown>('/short-video/content-calendar/save', params).then(normalizeNumberResult),
  contentCalendarDateRange: (params: { from: string; to: string; personaId?: number }) =>
    request.post<unknown>('/short-video/content-calendar/date-range', params).then(normalizeResponseArray<Record<string, unknown>>),
  contentCalendarAutoGenerate: (params: ContentCalendarAutoGenerateParams) =>
    request.post<unknown>('/short-video/content-calendar/auto-generate', params).then(normalizeNumberResult),

  /** 质量看板 / 反馈 / 工作流模板 */
  qualityOverview: () =>
    request.post<unknown>('/short-video/quality-dashboard/overview', {}).then(normalizeObject<Record<string, unknown>>),
  qualityTrend: (params: { days?: number }) =>
    request.post<unknown>('/short-video/quality-dashboard/trend', params).then(normalizeResponseArray<Record<string, unknown>>),
  qualityModelRanking: (params: { days?: number }) =>
    request.post<unknown>('/short-video/quality-dashboard/model-ranking', params).then(normalizeResponseArray<Record<string, unknown>>),
  qualityCameraRanking: (params: { days?: number }) =>
    request.post<unknown>('/short-video/quality-dashboard/camera-ranking', params).then(normalizeResponseArray<Record<string, unknown>>),
  qualityAiReflections: () =>
    request.post<unknown>('/short-video/quality-dashboard/ai-reflections', {}).then(normalizeStringArray),
  feedbackWeeklyReport: () =>
    request.post<unknown>('/short-video/feedback/weekly-report', {}).then(normalizeObject<Record<string, unknown>>),
  workflowTemplateList: () =>
    request.post<unknown>('/short-video/workflow-template/list', {}).then(normalizeWorkflowTemplates),
  workflowTemplateGet: (id: number) =>
    request.post<unknown>('/short-video/workflow-template/get', { id }).then(normalizeWorkflowTemplate),
  workflowDigitalHumanCommerceStart: (params: Record<string, unknown>) =>
    request.post<unknown>('/short-video/workflow/digital-human-commerce/start', params).then(normalizeWorkflowRuntimeStatus),
  workflowStatus: (taskId: string) =>
    request.post<unknown>('/short-video/workflow/status', { taskId }).then(normalizeWorkflowRuntimeStatus),

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
      personaId: params.personaId != null ? Number(params.personaId) : undefined,
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
  return request.post<unknown>('/short-video/viral/recommended', params ?? {}).then(normalizeResponseArray<Record<string, unknown>>)
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
  return request.post<unknown>('/short-video/seo/suggest-publish-time', {
    accountId: params.accountId,
  }).then(normalizeStringArray)
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
  return request.post<unknown>('/short-video/video-task/status', { taskId }).then(normalizeVideoTaskStatus)
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
    const list = normalizeHotTopicRows(raw)
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

export function autoCompose(params: AutoComposeParams): Promise<AutoComposeResult> {
  return request.post<unknown>('/short-video/edit/auto-compose', params).then(normalizeAutoComposeResult)
}

export function generateSubtitles(params: { scriptText?: string; language?: string; videoUrl?: string }): Promise<Record<string, unknown>> {
  return request.post<unknown>('/short-video/edit/generate-subtitles', params).then(normalizeObject<Record<string, unknown>>)
}

export interface PersonaFusionScript {
  original: string
  fused: string
  predictedConversionRate: number
}

export interface PersonaFusionConstraintsApplied {
  productId?: number
  productName?: string
  productCategory?: string
  topic?: string
  durationSeconds?: number
  count?: number
  [key: string]: unknown
}

export interface PersonaFusionResult {
  script?: string
  error?: string
  tokensUsed?: number
  remakeType?: string
  scriptId?: number
  fusionScore?: number
  usedVariableTable?: boolean
  violationCheck?: unknown
  constraintsApplied?: PersonaFusionConstraintsApplied
}

function normalizePersonaMatch(raw: unknown): PersonaMatchResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  return {
    personaId: readFirstNumber(value, ['personaId', 'id']),
    id: readFirstNumber(value, ['id', 'personaId']),
    personaName: readFirstText(value, ['personaName', 'name', 'nickname']),
    matchScore: readFirstNumber(value, ['matchScore', 'score', 'confidence']),
    reason: readFirstText(value, ['reason', 'matchReason', 'description']),
    tags: Array.isArray(value.tags) ? value.tags.map(String) : typeof value.tags === 'string' ? value.tags : undefined,
  }
}

function normalizePersonaFusionResult(raw: unknown): PersonaFusionResult {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const constraints = normalizeObject<PersonaFusionConstraintsApplied>(
    value.constraintsApplied ?? value.constraints ?? value.appliedConstraints ?? {},
  )
  return {
    script: readFirstText(value, ['script', 'content', 'result', 'output']) || undefined,
    error: readFirstText(value, ['error', 'message', 'errorMessage']) || undefined,
    tokensUsed: readFirstNumber(value, ['tokensUsed', 'tokenUsage', 'tokens']),
    remakeType: readFirstText(value, ['remakeType', 'type']) || undefined,
    scriptId: readFirstNumber(value, ['scriptId', 'id']),
    fusionScore: readFirstNumber(value, ['fusionScore', 'score', 'viralPotentialScore']),
    usedVariableTable: value.usedVariableTable != null ? readBooleanAlias(value.usedVariableTable, false) : undefined,
    violationCheck: value.violationCheck,
    constraintsApplied: Object.keys(constraints).length > 0 ? constraints : undefined,
  }
}

/** 使用 persona-fusion 后端；商品、话题、时长和数量会写入后端 Prompt 约束 */
export function personaViralFusion(params: {
  personaId: number
  viralVideoId: number
  productId?: number
  topic?: string
  duration?: number
  count?: number
  fusionMode?: 'full_viral' | 'hybrid' | 'persona_led'
}): Promise<PersonaFusionResult> {
  const remakeType = params.fusionMode === 'persona_led' ? 'persona_led' : 'form_imitation'
  return request.post<unknown>('/short-video/persona-fusion/generate-fused-script', {
    viralVideoId: params.viralVideoId,
    personaId: params.personaId,
    remakeType,
    productId: params.productId,
    topic: params.topic,
    duration: params.duration,
    count: params.count,
  }).then(normalizePersonaFusionResult)
}

export function matchPersonasForViral(viralVideoId: number): Promise<PersonaMatchResult[]> {
  return request.post<unknown>('/short-video/persona-fusion/match-personas', { viralVideoId })
    .then(raw => normalizeResponseArray<unknown>(raw).map(normalizePersonaMatch))
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

function normalizeSubtitleSegment(raw: unknown, index: number): SubtitleSegment {
  const value = normalizeObject<Record<string, unknown>>(raw)
  const startTime = readFirstNumber(value, ['startTime', 'start', 'start_time', 'begin']) ?? 0
  const endTime = readFirstNumber(value, ['endTime', 'end', 'end_time']) ?? Math.max(startTime, startTime + 3)
  const id = readFirstText(value, ['id', 'segmentId', 'segment_id']) || `${index + 1}`
  const fontSize = readFirstNumber(value, ['fontSize', 'font_size'])
  const color = readFirstText(value, ['color', 'fontColor', 'font_color'])
  const fontFamily = readFirstText(value, ['fontFamily', 'font_family'])
  const position = readFirstText(value, ['position', 'location', 'align'])

  return {
    id,
    startTime,
    endTime,
    text: readFirstText(value, ['text', 'content', 'subtitle', 'caption']),
    ...(fontSize != null ? { fontSize } : {}),
    ...(color ? { color } : {}),
    ...(fontFamily ? { fontFamily } : {}),
    ...(position ? { position } : {}),
  }
}

function normalizeSubtitleSegments(raw: unknown): SubtitleSegment[] {
  return normalizeResponseArray<unknown>(raw)
    .map((item, index) => normalizeSubtitleSegment(item, index))
    .filter((item) => item.text || item.startTime > 0 || item.endTime > 0)
}

export function subtitleGet(videoId: number): Promise<SubtitleSegment[]> {
  return request.post<unknown>('/short-video/edit/subtitles/get', { videoId }).then(normalizeSubtitleSegments)
}

export function subtitleSave(videoId: number, segments: SubtitleSegment[]): Promise<void> {
  return request.post<void>('/short-video/edit/subtitles/save', { videoId, segments })
}

/** 将字幕段转为 SRT */
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

export async function subtitleExportSrt(videoId: number, segments?: SubtitleSegment[]): Promise<string> {
  if (segments && segments.length > 0) return subtitlesToSrt(segments)
  return request.post<unknown>('/short-video/edit/subtitles/export-srt', { videoId }).then(normalizeTextResult)
}
