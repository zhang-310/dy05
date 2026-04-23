import request from '@/utils/request'

/** 与 DigitalHumanController /status 解包后的 data 对齐 */
export interface DigitalHumanStatusVO {
  available: boolean
  provider: string
}

/** 与 DigitalHumanController /generate 解包后的 data 对齐 */
export interface DigitalHumanGenerateResult {
  videoUrl: string
  provider: string
  success: boolean
  message: string
}

export function getDigitalHumanStatus() {
  return request.post<DigitalHumanStatusVO>('/ai/digital-human/status', {})
}

export function generateDigitalHumanVideo(data: {
  avatarId?: string
  scriptText: string
  voiceId?: string
}) {
  return request.post<DigitalHumanGenerateResult>('/ai/digital-human/generate', data)
}
