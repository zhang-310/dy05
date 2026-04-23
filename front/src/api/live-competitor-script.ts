import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LiveCompetitorScriptSaveVO, LiveCompetitorScriptSearchVO, LiveCompetitorScriptVO } from '@/types/live'

export function searchCompetitorScripts(data?: LiveCompetitorScriptSearchVO) {
  return request.post<PageResult<LiveCompetitorScriptVO>>('/live/competitor-script/search', data || {})
}

export function getCompetitorScript(id: number) {
  return request.post<LiveCompetitorScriptVO>('/live/competitor-script/get', { id })
}

export function saveCompetitorScript(data: LiveCompetitorScriptSaveVO) {
  return request.post<number>('/live/competitor-script/save', data)
}

export function deleteCompetitorScript(id: number) {
  return request.post<void>('/live/competitor-script/delete', { id })
}
