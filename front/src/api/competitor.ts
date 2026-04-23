import request from '@/utils/request'

export function addCompetitor(data: { accountId: string; accountName: string; platform?: string }) {
  return request.post<string>('/short-video/competitor/add', data)
}

export function listCompetitors() {
  return request.post<Record<string, unknown>[]>('/short-video/competitor/list', {})
}

export function analyzeCompetitor(competitorId: number) {
  return request.post<Record<string, unknown>>('/short-video/competitor/analyze', { competitorId })
}

export function generateWeeklyReport() {
  return request.post<string>('/short-video/competitor/weekly-report', {})
}
