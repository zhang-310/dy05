/**
 * 短视频模块类型定义
 */

export interface SvProject {
  id: number
  ownerId?: number
  accountId?: number
  title: string
  projectType: string
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
  reviewComment?: string
  progress?: number
  stage?: string
  createTime?: string
  updateTime?: string
}

export interface SvScript {
  id: number
  ownerId?: number
  title: string
  content: string
  scriptType: string
  generationType?: string
  referenceViralId?: number
  theme?: string
  style?: string
  duration?: number
  wordCount?: number
  tags?: string
  aiPrompt?: string
  aiModel?: string
  createTime?: string
  updateTime?: string
}

export interface SvScriptQuery {
  page?: number
  rows?: number
  ownerId?: number
  scriptType?: string
  style?: string
  title?: string
  sortName?: string
  sortOrder?: string
}

export interface SvScriptSaveRequest {
  id?: number
  title: string
  content: string
  scriptType: string
  generationType?: string
  referenceViralId?: number
  theme?: string
  style?: string
  duration?: number
  wordCount?: number
  tags?: string
  aiPrompt?: string
  aiModel?: string
  personaId?: number
}

export interface SvScriptGenerateRequest {
  type?: string
  theme?: string
  viralVideoId?: number
  productInfo?: string
  style?: string
  duration?: number
}

export interface SvShot {
  id?: number
  shotListId?: number
  shotNumber?: number
  timeRange?: string
  sceneDescription?: string
  dialogue?: string
  cameraAngle?: string
  cameraType?: string  // 运镜类型 (zoom-in, dolly-in 等)
  action?: string
  mood?: string
  reviewStatus?: string
  reviewerNote?: string
  keyframeUrl?: string
  keyframeBosKey?: string
  endFrameUrl?: string
  endFrameBosKey?: string
  videoUrl?: string
  videoBosKey?: string
  audioUrl?: string
  audioBosKey?: string
  duration?: number
  createTime?: string
  updateTime?: string
}

export interface SvShotListVO {
  id: number
  scriptId?: number
  shotCount?: number
  shots?: SvShot[]
  createTime?: string
  updateTime?: string
}

export interface GenerateShotListResult {
  shots: SvShot[]
  shotListId?: number
}

export interface QuickGenerateResult {
  projectId: number
  scriptId?: number
  shotListId?: number
  scriptContent?: string
  title?: string
  creativeBrief?: ShortVideoCreativeBrief
  productionPlan?: ShortVideoProductionPlan
}

export interface CreativePlanItem {
  type?: string
  requirement?: string
  time?: string
  label?: string
  goal?: string
  [key: string]: unknown
}

export interface ShortVideoCreativeBrief {
  theme?: string
  keywords?: string[]
  style?: string
  durationSeconds?: number
  aspectRatio?: string
  targetAudience?: string
  corePromise?: string
  hookOptions?: string[]
  storyBeats?: CreativePlanItem[]
  shotStrategy?: CreativePlanItem[]
  materialPlan?: CreativePlanItem[]
  audioPlan?: CreativePlanItem[]
  subtitlePlan?: CreativePlanItem[]
  publishPlan?: CreativePlanItem[]
  riskChecklist?: string[]
  acceptanceCriteria?: string[]
  [key: string]: unknown
}

export interface ShortVideoProductionPlan {
  projectId?: number
  scriptId?: number
  shotListId?: number
  currentStage?: string
  nextActions?: string[]
  acceptanceCriteria?: string[]
  [key: string]: unknown
}

export interface WorkflowTemplateStep {
  id?: string
  type?: string
  label?: string
  name?: string
  assignee?: string
  durationDays?: number
  days?: number
}

export interface WorkflowTemplate {
  id: number
  templateName?: string
  name?: string
  description?: string
  steps?: string | WorkflowTemplateStep[]
  isSystem?: number
  createTime?: string
  updateTime?: string
}

export interface KeyframeResult {
  shotId?: number
  shotNumber?: number
  imageUrl?: string
  bosKey?: string
  prompt?: string
  endFrameUrl?: string
  endFrameBosKey?: string
}

export interface VoiceResult {
  shotId?: number
  shotNumber?: number
  audioUrl?: string
  bosKey?: string
  duration?: number
}

export interface AutoComposeResult {
  finalVideoUrl?: string
  bosKey?: string
  duration?: number
  thumbnail?: string
}

export interface WorkflowRuntimeStatus {
  taskId?: string
  projectId?: number
  status: string
  currentStep: string
  progress: number
  pipeline?: string
  steps?: Array<{ step?: string; label?: string; progress?: number }>
  scriptId?: number
  shotListId?: number
  digitalHumanVideoUrl?: string
  digitalHumanPendingUrl?: string
  digitalHumanSkipped?: boolean
  digitalHumanSkipReason?: string
  productBrollKeyframeCount?: number
  productBrollVideoCount?: number
  voiceClipCount?: number
  composeVideoCount?: number
  finalVideoUrl?: string
  errorMessage?: string
  message?: string
  [key: string]: unknown
}

export interface GeneratedVideoResult {
  shotId?: number
  shotNumber?: number
  videoUrl?: string
  bosKey?: string
  duration?: number
}

/** 与后端 VideoGenerationTaskServiceImpl processTask 中 keyframe map 字段一致 */
export interface Img2VideoKeyframeMap {
  shotId: number
  shotNumber: number
  imageUrl: string
  endFrameUrl?: string | null
  duration?: number
  motion?: string
  sceneDescription?: string | null
  quality?: string | null
  aspectRatio?: string | null
  cameraType?: string | null
  mood?: string | null
  action?: string | null
}

export interface VideoTaskSubmitBody {
  projectId?: number
  shotListId?: number
  quality?: string
  aspectRatio?: string
  keyframes: Img2VideoKeyframeMap[]
}

export interface VideoTaskSubmitResult {
  taskId: number
}

export interface VideoGenerationTaskRow {
  id: number
  taskType: string
  status: string
  progress: number
  projectId?: number
  shotListId?: number
  priority?: number
  createTime?: string
  errorMessage?: string
  outputUrl?: string
  videos?: GeneratedVideoResult[]
}

export interface PublishTitleSuggestion {
  text: string
  score?: number
}

export interface PublishTitleResult {
  titles: PublishTitleSuggestion[]
}

export interface PublishReviewResult {
  passed: boolean
  issues: string[]
  suggestions: string[]
  projectId?: number
  officialReferences?: {
    kbName?: string
    refType?: string
    docId?: number
    chunkId?: number
    title?: string
    contentPreview?: string
    score?: number
  }[]
  officialReferenceRequired?: boolean
  officialReferenceSatisfied?: boolean
  officialReferenceStatus?: string
}

export interface PublishPlatformResult {
  platform: string
  success: boolean
  itemId?: string
  error?: string
}

export interface PublishResult {
  success: boolean
  degraded?: boolean
  projectId?: number
  platform?: string
  results: PublishPlatformResult[]
}
