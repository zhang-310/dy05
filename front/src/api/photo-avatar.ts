import request from '@/utils/request'
import type { PageResult, BasicQuery } from '@/types/common'

export interface PhotoAvatarTaskVO {
  id: number
  userId: number
  photoUrl: string
  outfitStyle: string
  background: string
  status: 'pending' | 'processing' | 'completed' | 'failed'
  outputUrl: string | null
  errorMessage: string | null
  progress: number
  costCredits: number
  createTime: string
  updateTime: string | null
}

export interface PhotoAvatarOverview {
  productCode: string
  taskCount: number
  byStatus: Record<string, number>
}

export interface PhotoAvatarSaveVO {
  photoUrl: string
  outfitStyle?: string
  background?: string
  portraitConsentConfirmed?: boolean
  insightReportId?: number
}

export interface PhotoAvatarSearchVO extends BasicQuery {
  status?: string
  outfitStyle?: string
  background?: string
}

export function getPhotoAvatarOverview() {
  return request.post<PhotoAvatarOverview>('/photo-avatar/overview', {})
}

export function searchPhotoAvatarTasks(params: PhotoAvatarSearchVO) {
  return request.post<PageResult<PhotoAvatarTaskVO>>('/photo-avatar/search', params)
}

export function createPhotoAvatarTask(data: PhotoAvatarSaveVO) {
  return request.post<number>('/photo-avatar/create', data)
}

export function getPhotoAvatarTaskStatus(id: number) {
  return request.post<PhotoAvatarTaskVO>('/photo-avatar/status', { id })
}

export function deletePhotoAvatarTask(id: number) {
  return request.post<null>('/photo-avatar/delete', { id })
}

export function retryPhotoAvatarTask(id: number) {
  return request.post<number>('/photo-avatar/retry', { id })
}
