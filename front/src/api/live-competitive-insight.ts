import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  LiveCompetitiveInsightSaveVO,
  LiveCompetitiveInsightSearchVO,
  LiveCompetitiveInsightVO,
} from '@/types/live'

export function searchCompetitiveInsights(data?: LiveCompetitiveInsightSearchVO) {
  return request.post<PageResult<LiveCompetitiveInsightVO>>('/live/competitive-insight/search', data || {})
}

export function getCompetitiveInsight(id: number) {
  return request.post<LiveCompetitiveInsightVO>('/live/competitive-insight/get', { id })
}

export function saveCompetitiveInsight(data: LiveCompetitiveInsightSaveVO) {
  return request.post<number>('/live/competitive-insight/save', data)
}

export function deleteCompetitiveInsight(id: number) {
  return request.post<void>('/live/competitive-insight/delete', { id })
}
