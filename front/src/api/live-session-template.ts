import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LiveSessionTemplateSaveVO, LiveSessionTemplateSearchVO, LiveSessionTemplateVO } from '@/types/live'

export function searchSessionTemplates(data?: LiveSessionTemplateSearchVO) {
  return request.post<PageResult<LiveSessionTemplateVO>>('/live/session-template/search', data || {})
}

export function getSessionTemplate(id: number) {
  return request.post<LiveSessionTemplateVO>('/live/session-template/get', { id })
}

export function saveSessionTemplate(data: LiveSessionTemplateSaveVO) {
  return request.post<number>('/live/session-template/save', data)
}

export function deleteSessionTemplate(id: number) {
  return request.post<void>('/live/session-template/delete', { id })
}
