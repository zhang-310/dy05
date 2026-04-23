import request from '@/utils/request'

export interface ShootingTaskListParams {
  page?: number
  rows?: number
  sortName?: string
  sortOrder?: string
  title?: string
  status?: number
  shootDateFrom?: string
  shootDateTo?: string
  personaId?: number
  photographerId?: number
  anchorUserId?: number
}

export interface ShootingTaskSavePayload {
  id?: number
  title: string
  shootDate: string
  anchorUserId?: number
  personaId?: number
  photographerId?: number
  scriptId?: number
  projectId?: number
  description?: string
  scriptContent?: string
  shootingBrief?: string
  priority?: number
  status?: number
  materialUrls?: string
  reviewNotes?: string
  reviewedBy?: number
  referenceVideoUrl?: string
}

export function listShootingTasks(data?: ShootingTaskListParams) {
  return request.post<{ total: number; list: Record<string, unknown>[]; pageNum: number; pageSize: number }>(
    '/short-video/shooting-task/list',
    data || {}
  )
}

export function getShootingTask(id: number) {
  return request.post<Record<string, unknown>>('/short-video/shooting-task/get', { id })
}

export function saveShootingTask(data: ShootingTaskSavePayload) {
  return request.post<number>('/short-video/shooting-task/save', data)
}

export function deleteShootingTask(id: number) {
  return request.post<void>('/short-video/shooting-task/delete', { id })
}
