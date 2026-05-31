import request from '@/utils/request'

export interface DouyinOpsBriefResult {
  role?: string
  summary?: string
  actions?: string[]
  [key: string]: unknown
}

export function fetchDouyinOpsBrief() {
  return request.post<DouyinOpsBriefResult>('/ai/douyin-ops-commander/brief', {})
}
