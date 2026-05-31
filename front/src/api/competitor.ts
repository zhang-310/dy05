import request from '@/utils/request'
import { normalizeArray, normalizeRecord } from '@/utils/response-normalize'

type CompetitorRow = Record<string, unknown>

function normalizeCompetitorRows(raw: unknown): CompetitorRow[] {
  return normalizeArray<unknown>(raw)
    .map((item) => normalizeRecord(item))
    .filter((item) => Object.keys(item).length > 0)
}

function normalizeAnalysisResult(raw: unknown): CompetitorRow {
  return normalizeRecord(raw)
}

function normalizeWeeklyReport(raw: unknown): string {
  const record = normalizeRecord(raw)
  for (const key of ['report', 'content', 'result', 'text', 'summary', 'message'] as const) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) return value.trim()
    if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  }
  if (typeof raw === 'string' || typeof raw === 'number') return String(raw)
  return Object.keys(record).length > 0 ? JSON.stringify(record, null, 2) : ''
}

export function addCompetitor(data: { accountId: string; accountName: string; platform?: string }) {
  return request.post<string>('/short-video/competitor/add', data)
}

export function listCompetitors() {
  return request.post<unknown>('/short-video/competitor/list', {}).then(normalizeCompetitorRows)
}

export function analyzeCompetitor(competitorId: number) {
  return request.post<unknown>('/short-video/competitor/analyze', { competitorId }).then(normalizeAnalysisResult)
}

export function generateWeeklyReport() {
  return request.post<unknown>('/short-video/competitor/weekly-report', {}).then(normalizeWeeklyReport)
}

export function removeCompetitor(competitorId: number) {
  return request.post<void>('/short-video/competitor/remove', { competitorId })
}
