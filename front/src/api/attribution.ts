import request from '@/utils/request'
import { isRecord, normalizeArray, parseJsonValue } from '@/utils/response-normalize'

/**
 * AI 归因分析 API
 * Backend: /api/v1/ai/attribution/*
 */
export interface AttributionSummary {
  channel: string
  gmv: number
  orders: number
  conversionRate: number
  contribution: number
  momChange?: number
}

export interface AttributionTrend {
  date: string
  channel: string
  gmv: number
}

export interface ScriptAttribution {
  segmentIndex: number
  segmentName: string
  conversionRate: number
  gmvContribution: number
  orderCount: number
  versionCount: number
  scriptId?: number
}

export interface TimeHeatmap {
  hour: number
  weekday: number
  gmv: number
  orders: number
  conversionRate: number
}

export interface SessionAnalysis {
  sessionId: number
  liveTitle: string
  gmv: number
  viewers: number
  conversionRate: number
  avgOrderValue: number
  orderCount: number
  likesCount: number
}

export interface AttributionDetail {
  id: number
  sessionId: number
  attributionType: string
  scriptId?: number
  productId?: number
  scriptContent?: string
  productName?: string
  contributedGmv: number
  contributedSales: number
  conversionRate: number
  contributionRatio: number
  effectScore: number
  analysis?: string
  modelUsed?: string
  status: number
  createTime: string
}

export interface AttributionSummaryVO {
  sessionId: number
  totalGmv: number
  totalSales: number
  productAttributions: number
  scriptAttributions: number
  overallScore: number
  aiAnalysis?: string
  status: string
}

type UnknownRecord = Record<string, unknown>

function toNumber(value: unknown, fallback = 0): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function toStringValue(value: unknown, fallback = ''): string {
  if (value == null) return fallback
  return String(value)
}

function unwrapRecord(raw: unknown): UnknownRecord {
  const value = parseJsonValue(raw)
  if (!isRecord(value) || Array.isArray(value)) return {}
  if (isRecord(value.data)) return unwrapRecord(value.data)
  return value
}

function normalizeDetail(raw: unknown): AttributionDetail {
  const record = unwrapRecord(raw)
  return {
    ...record,
    id: toNumber(record.id),
    sessionId: toNumber(record.sessionId),
    attributionType: toStringValue(record.attributionType ?? record.type),
    scriptId: record.scriptId == null ? undefined : toNumber(record.scriptId),
    productId: record.productId == null ? undefined : toNumber(record.productId),
    scriptContent: record.scriptContent == null ? undefined : toStringValue(record.scriptContent),
    productName: record.productName == null ? undefined : toStringValue(record.productName),
    contributedGmv: toNumber(record.contributedGmv ?? record.gmv ?? record.gmvContribution),
    contributedSales: toNumber(record.contributedSales ?? record.sales ?? record.orderCount),
    conversionRate: toNumber(record.conversionRate),
    contributionRatio: toNumber(record.contributionRatio ?? record.ratio),
    effectScore: toNumber(record.effectScore ?? record.score),
    analysis: record.analysis == null ? undefined : toStringValue(record.analysis),
    modelUsed: record.modelUsed == null ? undefined : toStringValue(record.modelUsed),
    status: toNumber(record.status),
    createTime: toStringValue(record.createTime ?? record.createdAt),
  }
}

function normalizeSummary(raw: unknown): AttributionSummaryVO {
  const record = unwrapRecord(raw)
  return {
    sessionId: toNumber(record.sessionId),
    totalGmv: toNumber(record.totalGmv ?? record.gmv),
    totalSales: toNumber(record.totalSales ?? record.sales),
    productAttributions: toNumber(record.productAttributions ?? record.productCount),
    scriptAttributions: toNumber(record.scriptAttributions ?? record.scriptCount),
    overallScore: toNumber(record.overallScore ?? record.score),
    aiAnalysis: record.aiAnalysis == null ? undefined : toStringValue(record.aiAnalysis),
    status: toStringValue(record.status, 'unknown'),
  }
}

export const attributionApi = {
  /** 触发归因分析（异步） */
  trigger: (sessionId: number) =>
    request.post<number>('/ai/attribution/trigger', { sessionId }),

  /** 获取场次归因数据列表 */
  session: (sessionId: number) =>
    request.post<unknown>('/ai/attribution/session', { sessionId })
      .then(raw => normalizeArray<unknown>(raw).map(normalizeDetail)),

  /** 获取归因汇总 */
  summary: (sessionId: number) =>
    request.post<unknown>('/ai/attribution/summary', { sessionId }).then(normalizeSummary),

  /** 获取归因详情 */
  get: (id: number) =>
    request.post<unknown>('/ai/attribution/get', { id }).then(normalizeDetail),

  /** 删除场次归因数据 */
  deleteSession: (sessionId: number) =>
    request.post('/ai/attribution/session/delete', { sessionId }),
}
