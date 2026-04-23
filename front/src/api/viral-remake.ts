import request from '@/utils/request'

const BASE = '/short-video/viral-remake'

export function recommendRemake(viralVideoId: number) {
  return request.post<void>(`${BASE}/recommend`, { viralVideoId })
}

export function batchRecommendRemake(scoreThreshold = 70, limit = 20) {
  return request.post<number>(`${BASE}/batch-recommend`, { scoreThreshold, limit })
}

export function confirmRemake(viralVideoId: number, remakeType: string, personaId?: number) {
  return request.post<void>(`${BASE}/confirm`, { viralVideoId, remakeType, personaId })
}

export function generateRemakeScript(viralVideoId: number, scriptMode = 'sop') {
  return request.post<number>(`${BASE}/generate-script`, { viralVideoId, scriptMode })
}

export function assignRemakeTask(
  viralVideoId: number,
  photographerId: number | undefined,
  shootDate: string
) {
  return request.post<number>(`${BASE}/assign-task`, { viralVideoId, photographerId, shootDate })
}

export function completeRemake(viralVideoId: number) {
  return request.post<void>(`${BASE}/complete`, { viralVideoId })
}
