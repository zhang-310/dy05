/**
 * 短视频模块类型定义
 */

export interface SvProject {
  id: number
  ownerId?: number
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
  publishTitle?: string
  progress?: number
  stage?: string
  updateTime?: string
}

export interface SvScript {
  id: number
  title: string
  content: string
  scriptType: string
  theme?: string
  updateTime?: string
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
  keyframeUrl?: string
  endFrameUrl?: string
  videoUrl?: string
  audioUrl?: string
  duration?: number
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
