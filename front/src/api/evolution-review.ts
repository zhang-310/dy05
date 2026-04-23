import request from '@/utils/request'

export interface EvolutionReviewTask {
  id: number
  evolveTaskId: number
  contentPreview: string
  qualityScore: number
  reviewerId: number | null
  reviewStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVISED'
  reviewComment: string | null
  revisedContent: string | null
  reviewedAt: string | null
  autoExpired: boolean
  createTime: string
}

export interface ReviewStats {
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  revisedCount: number
  approvalRate7d: number
}

export interface PageResult<T> {
  total: number
  list: T[]
  pageNum: number
  pageSize: number
}

export function listReviewTasks(data: { status?: string; page?: number; rows?: number }) {
  return request.post<PageResult<EvolutionReviewTask>>(
    '/ai/evolution-review/list',
    data,
  )
}

export function approveReview(taskId: number, comment?: string) {
  return request.post<void>('/ai/evolution-review/approve', { taskId, comment })
}

export function rejectReview(taskId: number, comment?: string) {
  return request.post<void>('/ai/evolution-review/reject', { taskId, comment })
}

export function reviseReview(taskId: number, revisedContent: string, comment?: string) {
  return request.post<void>('/ai/evolution-review/revise', {
    taskId,
    revisedContent,
    comment,
  })
}

export function getReviewStats() {
  return request.post<ReviewStats>('/ai/evolution-review/stats')
}
