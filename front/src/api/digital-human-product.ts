import request from '@/utils/request'

/** 六产品 M4 轨：/api/v1/digital-human/*（与 admin HeyGen /ai/digital-human 不同） */
export interface DigitalHumanProductTask {
  taskId?: number
  id?: number
  status?: string
  [key: string]: unknown
}

export function createDigitalHumanProductTask(body: {
  scriptContent: string
  voiceType?: string
}) {
  return request.post<DigitalHumanProductTask>('/digital-human/create', body)
}
