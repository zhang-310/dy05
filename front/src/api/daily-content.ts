import request from '@/utils/request'

export function generateDailyBatch(data: { personaId?: number; sourceType?: string; batchSize?: number }) {
  return request.post('/short-video/daily/generate-batch', data)
}
export function getDailyBatchStatus(data: { batchId: number }) {
  return request.post('/short-video/daily/batch-status', data)
}
export function listDailyBatches(data?: { page?: number; rows?: number }) {
  return request.post('/short-video/daily/batch-list', data || {})
}
