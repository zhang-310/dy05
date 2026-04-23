import request from '@/utils/request'

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

export const attributionApi = {
  /** 触发归因分析（异步） */
  trigger: (sessionId: number) =>
    request.post<{ data: number }>('/ai/attribution/trigger', { sessionId }),

  /** 获取场次归因数据列表 */
  session: (sessionId: number) =>
    request.post<AttributionDetail[]>('/ai/attribution/session', { sessionId }),

  /** 获取归因汇总 */
  summary: (sessionId: number) =>
    request.post<AttributionSummaryVO>('/ai/attribution/summary', { sessionId }),

  /** 获取归因详情 */
  get: (id: number) =>
    request.post<AttributionDetail>('/ai/attribution/get', { id }),

  /** 删除场次归因数据 */
  deleteSession: (sessionId: number) =>
    request.delete(`/ai/attribution/session/${sessionId}`),
}