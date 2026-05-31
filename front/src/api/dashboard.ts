import request from '@/utils/request'
import { isRecord, normalizeArray, normalizeRecord, parseJsonValue } from '@/utils/response-normalize'

type UnknownRecord = Record<string, unknown>

function unwrapData(value: unknown): unknown {
  const parsed = parseJsonValue(value)
  if (!isRecord(parsed)) return parsed
  const status = parsed.status ?? parsed.code
  if ('data' in parsed && (status != null || 'message' in parsed || 'traceId' in parsed)) {
    return unwrapData(parsed.data)
  }
  if ('result' in parsed && status != null) {
    return unwrapData(parsed.result)
  }
  return parsed
}

function recordOf(data: unknown): UnknownRecord {
  const value = normalizeRecord(unwrapData(data), ['stats'])
  if (isRecord(value.stats)) return value.stats
  if (isRecord(value.result) && isRecord(value.result.stats)) return value.result.stats
  if (isRecord(value.detail) && isRecord(value.detail.stats)) return value.detail.stats
  if (isRecord(value.payload) && isRecord(value.payload.stats)) return value.payload.stats
  return value
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function toStringValue(value: unknown, fallback = ''): string {
  if (value == null) return fallback
  return String(value)
}

function rowsOf(data: unknown): UnknownRecord[] {
  const value = unwrapData(data)
  return normalizeArray<unknown>(value).filter(isRecord)
}

function numberArrayOf(value: unknown): number[] | undefined {
  const parsed = parseJsonValue(value)
  if (Array.isArray(parsed)) return parsed.map((n) => toNumber(n))
  if (typeof parsed === 'string' && parsed.includes(',')) {
    return parsed.split(',').map((n) => toNumber(n)).filter(Number.isFinite)
  }
  return undefined
}

function dateFromLookback(lookbackDays: number): string {
  const days = Number.isFinite(lookbackDays) && lookbackDays > 0 ? lookbackDays : 30
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

export interface AdminStats {
  // 用户
  totalUsers: number
  activeUsers: number
  todayUsers: number
  // 抖音视频
  totalVideos: number
  publishedVideos: number
  todayVideos: number
  // 直播
  totalLiveSessions: number
  completedSessions: number
  todaySessions: number
  // 短视频
  totalShortVideos: number
  publishedShortVideos: number
  todayShortVideos: number
  // 文案库
  totalCopyItems: number
  approvedCopyItems: number
  todayCopyItems: number
  // AI
  todayAiCalls: number
  todayAiAttempts: number
  aiSuccessRate: number
  // 收入
  todayRevenue: number
}

export interface KpiUnified {
  gmvToday: number
  gmvMom: number           // 环比 %
  gmvYoy: number           // 同比 %
  gmvTarget?: number       // 今日目标
  gmvForecast?: number[]   // 未来3天AI预测（v3.0）
  ordersToday: number
  avgOrderValue: number
  conversionRate: number
  liveSessions: number
  activeSessionCount?: number
  aiCallsToday: number
  aiCallCount?: number
  publishedVideos?: number
  docCount?: number
}

export interface LiveFormatGmv {
  format: string
  gmv: number
  sessions: number
  avgGmv: number
}

export interface ProductGmvSummary {
  productId: number
  productName: string
  totalGmv: number
  totalOrders: number
  sessionCount: number
  avgPrice: number
}

export interface CockpitPreview {
  date: string
  gmv: number
  orders: number
  sessions: number
}

export interface CockpitPreviewRowsParams {
  lookbackDays?: number
  dateFrom?: string
  sessionStatus?: number | ''
}

export interface CockpitSessionRow {
  sessionId?: number
  liveTitle?: string
  accountId?: number
  userId?: number
  status?: number
  startTime?: string
  endTime?: string
  gmv: number
  productLineCount: number
}

export interface CockpitExportResult {
  csv: string
  filename: string
  rowCount: number
}

export interface ProfitMatrixRow {
  liveFormat: string
  sessionCount: number
  totalGmv: number
  estimatedMarginRate: number
  isEstimated: boolean
  note: string
}

export interface ProfitMatrixPreview {
  lookbackDays: number
  since?: string
  rows: ProfitMatrixRow[]
}

export interface ConversionFunnel {
  exposure: number
  clicks: number
  addToCart: number
  orders: number
  payments: number
  steps?: ConversionFunnelStep[]
}

export interface ConversionFunnelStep {
  name: string
  value: number
  rate?: number
}

function normalizeKpiUnified(raw: unknown): KpiUnified {
  const record = recordOf(raw)
  const revenue = isRecord(record.revenue) ? record.revenue : {}
  const conversion = isRecord(record.conversion) ? record.conversion : {}
  const content = isRecord(record.content) ? record.content : {}
  const traffic = isRecord(record.traffic) ? record.traffic : {}
  const todayGmv = toNumber(record.gmvToday ?? revenue.todayGmv)
  const yesterdayGmv = toNumber(revenue.yesterdayGmv)
  const gmvMom = record.gmvMom == null && yesterdayGmv > 0
    ? ((todayGmv - yesterdayGmv) / yesterdayGmv) * 100
    : toNumber(record.gmvMom)
  const forecast = numberArrayOf(record.gmvForecast ?? record.forecast ?? record.gmvPrediction)

  return {
    gmvToday: todayGmv,
    gmvMom,
    gmvYoy: toNumber(record.gmvYoy),
    gmvTarget: record.gmvTarget == null ? undefined : toNumber(record.gmvTarget),
    gmvForecast: forecast,
    ordersToday: toNumber(record.ordersToday ?? conversion.saleQuantitySinceToday),
    avgOrderValue: toNumber(record.avgOrderValue ?? revenue.avgOrderValueToday),
    conversionRate: toNumber(record.conversionRate),
    liveSessions: toNumber(record.liveSessions ?? record.activeSessionCount ?? traffic.liveSessionCount ?? traffic.sessionCount),
    activeSessionCount: record.activeSessionCount == null ? undefined : toNumber(record.activeSessionCount),
    aiCallsToday: toNumber(record.aiCallsToday ?? record.aiCallCount),
    aiCallCount: record.aiCallCount == null ? undefined : toNumber(record.aiCallCount),
    publishedVideos: record.publishedVideos == null ? toNumber(content.shortVideoCount) : toNumber(record.publishedVideos),
    docCount: record.docCount == null ? toNumber(content.kbDocumentCount) : toNumber(record.docCount),
  }
}

function normalizeAdminStats(raw: unknown): AdminStats {
  const record = recordOf(raw)
  return {
    totalUsers: toNumber(record.totalUsers),
    activeUsers: toNumber(record.activeUsers),
    todayUsers: toNumber(record.todayUsers),
    totalVideos: toNumber(record.totalVideos),
    publishedVideos: toNumber(record.publishedVideos),
    todayVideos: toNumber(record.todayVideos),
    totalLiveSessions: toNumber(record.totalLiveSessions ?? record.totalLives ?? record.liveSessions),
    completedSessions: toNumber(record.completedSessions),
    todaySessions: toNumber(record.todaySessions),
    totalShortVideos: toNumber(record.totalShortVideos),
    publishedShortVideos: toNumber(record.publishedShortVideos),
    todayShortVideos: toNumber(record.todayShortVideos),
    totalCopyItems: toNumber(record.totalCopyItems),
    approvedCopyItems: toNumber(record.approvedCopyItems),
    todayCopyItems: toNumber(record.todayCopyItems),
    todayAiCalls: toNumber(record.todayAiCalls),
    todayAiAttempts: toNumber(record.todayAiAttempts),
    aiSuccessRate: toNumber(record.aiSuccessRate ?? record.todayAiSuccessRate ?? record.successRate),
    todayRevenue: toNumber(record.todayRevenue),
  }
}

function normalizeLiveFormatRows(raw: unknown): LiveFormatGmv[] {
  return rowsOf(raw).map((row) => {
    const sessions = toNumber(row.sessions ?? row.sessionCount)
    const gmv = toNumber(row.gmv ?? row.totalGmv)
    return {
      format: toStringValue(row.format ?? row.liveFormat, '未分类'),
      gmv,
      sessions,
      avgGmv: toNumber(row.avgGmv, sessions > 0 ? gmv / sessions : 0),
    }
  })
}

function normalizeProductGmvRows(raw: unknown): ProductGmvSummary[] {
  return rowsOf(raw).map((row) => {
    const totalGmv = toNumber(row.totalGmv ?? row.gmv)
    const sessionCount = toNumber(row.sessionCount ?? row.sessions)
    const totalOrders = row.totalOrders == null && row.orderCount == null && row.orders == null
      ? 0
      : toNumber(row.totalOrders ?? row.orderCount ?? row.orders)
    return {
      productId: toNumber(row.productId),
      productName: toStringValue(row.productName, `商品${toNumber(row.productId)}`),
      totalGmv,
      totalOrders,
      sessionCount,
      avgPrice: toNumber(row.avgPrice, totalOrders > 0 ? totalGmv / totalOrders : 0),
    }
  })
}

function normalizeCockpitRows(raw: unknown): CockpitPreview[] {
  const byDate = new Map<string, CockpitPreview>()
  rowsOf(raw).forEach((row, index) => {
    const time = toStringValue(row.date ?? row.startTime ?? row.createTime)
    const date = time ? time.slice(0, 10) : `未标日期${index + 1}`
    const current = byDate.get(date) ?? { date, gmv: 0, orders: 0, sessions: 0 }
    current.gmv += toNumber(row.gmv ?? row.totalGmv)
    current.orders += toNumber(row.orders ?? row.orderCount ?? row.productLineCount)
    current.sessions += toNumber(row.sessions ?? row.sessionCount, 1)
    byDate.set(date, current)
  })
  return Array.from(byDate.values()).sort((a, b) => a.date.localeCompare(b.date, 'zh-CN'))
}

function cockpitFilters(params: CockpitPreviewRowsParams = {}): Record<string, unknown> {
  const lookbackDays = Number.isFinite(params.lookbackDays) && Number(params.lookbackDays) > 0
    ? Number(params.lookbackDays)
    : 30
  return {
    lookbackDays,
    dateFrom: params.dateFrom || dateFromLookback(lookbackDays),
    sessionStatus: params.sessionStatus === '' ? undefined : params.sessionStatus,
  }
}

function normalizeCockpitSessionRows(raw: unknown): CockpitSessionRow[] {
  return rowsOf(raw).map((row) => ({
    sessionId: row.sessionId == null ? undefined : toNumber(row.sessionId),
    liveTitle: row.liveTitle == null ? undefined : toStringValue(row.liveTitle),
    accountId: row.accountId == null ? undefined : toNumber(row.accountId),
    userId: row.userId == null ? undefined : toNumber(row.userId),
    status: row.status == null ? undefined : toNumber(row.status),
    startTime: row.startTime == null ? undefined : toStringValue(row.startTime),
    endTime: row.endTime == null ? undefined : toStringValue(row.endTime),
    gmv: toNumber(row.gmv ?? row.totalGmv),
    productLineCount: toNumber(row.productLineCount ?? row.productLines ?? row.orders),
  }))
}

function normalizeCockpitExport(raw: unknown): CockpitExportResult {
  const record = recordOf(raw)
  return {
    csv: toStringValue(record.csv),
    filename: toStringValue(record.filename, `cockpit_${new Date().toISOString().slice(0, 10)}.csv`),
    rowCount: toNumber(record.rowCount),
  }
}

function normalizeProfitMatrix(raw: unknown): ProfitMatrixPreview {
  const record = recordOf(raw)
  return {
    lookbackDays: toNumber(record.lookbackDays, 30),
    since: record.since == null ? undefined : toStringValue(record.since),
    rows: rowsOf(raw).map((row) => ({
      liveFormat: toStringValue(row.liveFormat ?? row.format, '普通直播'),
      sessionCount: toNumber(row.sessionCount ?? row.sessions),
      totalGmv: toNumber(row.totalGmv ?? row.gmv),
      estimatedMarginRate: toNumber(row.estimatedMarginRate ?? row.marginRate),
      isEstimated: row.isEstimated == null ? true : Boolean(row.isEstimated),
      note: toStringValue(row.note),
    })),
  }
}

function normalizeFunnelSteps(raw: unknown): ConversionFunnelStep[] | undefined {
  const value = unwrapData(raw)
  if (Array.isArray(value)) {
    return value.filter(isRecord).map((step) => ({
      name: toStringValue(step.name ?? step.label ?? step.stage),
      value: toNumber(step.value ?? step.count),
      rate: step.rate == null ? undefined : toNumber(step.rate),
    }))
  }
  if (!isRecord(value)) return undefined
  const stepSource = Array.isArray(value.steps)
    ? value.steps
    : Array.isArray(value.stages)
      ? value.stages
      : undefined
  if (stepSource) {
    return stepSource.filter(isRecord).map((step) => ({
      name: toStringValue(step.name ?? step.label ?? step.stage),
      value: toNumber(step.value ?? step.count),
      rate: step.rate == null ? undefined : toNumber(step.rate),
    }))
  }
  if (isRecord(value.stages)) {
    return Object.entries(value.stages).map(([name, count]) => ({
      name,
      value: toNumber(count),
    }))
  }
  return undefined
}

function normalizeConversionFunnel(raw: unknown): ConversionFunnel {
  const record = recordOf(raw)
  const steps = normalizeFunnelSteps(raw)

  if (steps && steps.length > 0) {
    return {
      exposure: steps[0]?.value ?? 0,
      clicks: steps[1]?.value ?? 0,
      addToCart: steps[2]?.value ?? 0,
      orders: steps[3]?.value ?? 0,
      payments: steps[4]?.value ?? 0,
      steps,
    }
  }

  return {
    exposure: toNumber(record.exposure ?? record.impressions ?? record.views ?? record.viewers),
    clicks: toNumber(record.clicks ?? record.likes),
    addToCart: toNumber(record.addToCart ?? record.productLines),
    orders: toNumber(record.orders ?? record.orderCount),
    payments: toNumber(record.payments ?? record.payCount),
  }
}

export const dashboardApi = {
  adminStats: () =>
    request.post<unknown>('/dashboard/admin/stats', {}).then(normalizeAdminStats),

  // Alias used by v3.0 prototype (01-dashboard)
  adminDashboard: (params?: { orgId?: number; lookbackDays?: number }) =>
    request.post<unknown>('/dashboard/admin/stats', params ?? {}).then(normalizeAdminStats),

  kpiUnified: (lookbackDays = 30) =>
    request.post<unknown>('/dashboard/kpi-unified', { lookbackDays }).then(normalizeKpiUnified),

  liveFormatGmv: (lookbackDays = 30) =>
    request.post<unknown>('/dashboard/live-format-gmv', { lookbackDays }).then(normalizeLiveFormatRows),

  productGmvSummary: (lookbackDays = 30) =>
    request.post<unknown>('/dashboard/product-gmv-summary', { lookbackDays }).then(normalizeProductGmvRows),

  cockpitPreview: (lookbackDays = 30) =>
    request.post<unknown>('/dashboard/cockpit-preview', { lookbackDays, dateFrom: dateFromLookback(lookbackDays) }).then(normalizeCockpitRows),

  cockpitPreviewRows: (params: CockpitPreviewRowsParams = {}) =>
    request.post<unknown>('/dashboard/cockpit-preview', cockpitFilters(params)).then(normalizeCockpitSessionRows),

  conversionFunnel: (lookbackDays = 30) =>
    request.post<unknown>('/dashboard/conversion-funnel', { lookbackDays }).then(normalizeConversionFunnel),

  orgStats: () =>
    request.post<unknown>('/dashboard/org/stats', {}).then(normalizeAdminStats),

  profitMatrixPreview: (lookbackDays = 30) =>
    request.post<unknown>('/dashboard/profit-matrix-preview', { lookbackDays }).then(normalizeProfitMatrix),

  cockpitExport: (params: CockpitPreviewRowsParams = {}) =>
    request.post<unknown>('/dashboard/cockpit-export', cockpitFilters(params)).then(normalizeCockpitExport),
}
