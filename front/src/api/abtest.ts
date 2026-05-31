import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import { normalizeArray as normalizeResponseArray, normalizePage as normalizeResponsePage, normalizeRecord } from '@/utils/response-normalize'

export interface AbVariant {
  id: number
  experimentId: number
  variantName: string
  variantType?: 'A' | 'B' | string
  content?: string
  entityType?: string
  entityId?: number
  styleCode?: string
  viewCount?: number
  clickCount?: number
  conversionCount?: number
  conversionRate: number
  isWinner: boolean

  // Backward-compatible aliases used by older pages.
  trafficRatio: number
  scriptStyle: string
  conversions: number
  exposures: number
}

export interface AbExperiment {
  id: number
  ownerId?: number
  name: string
  experimentName: string
  description: string
  experimentType: 'video' | 'live' | 'copy' | 'script_style' | string
  targetEntityType?: string
  targetEntityId?: number
  status: number // backend: 0=草稿 1=运行中 2=已完成 3=已暂停
  winnerVariantId: number | null
  conclusion?: string
  startTime: string
  endTime: string
  createTime: string
  updateTime?: string
  variants?: AbVariant[]

  // Backward-compatible alias. Real backend has no traffic split column.
  trafficSplit: number
}

export interface AbQuery {
  page?: number
  rows?: number
  keyword?: string
  experimentName?: string
  experimentType?: string
  status?: number
}

export interface AbVariantSave {
  id?: number
  experimentId?: number
  variantName: string
  variantType: 'A' | 'B' | string
  content?: string
  entityType?: string
  entityId?: number
  styleCode?: string
  scriptStyle?: string
}

export interface AbExperimentSave {
  id?: number
  name?: string
  experimentName?: string
  description?: string
  experimentType?: 'video' | 'live' | 'copy' | 'script_style' | string
  status?: number
  conclusion?: string
  targetEntityType?: string
  targetEntityId?: number
  variants?: AbVariantSave[]
}

export interface AbStatisticalTest {
  chiSquare?: number
  pValue?: number
  confidenceLevel?: number
  isSignificant?: boolean
  recommendedWinnerId?: number
  recommendedWinnerName?: string
  conclusion?: string
}

export interface AbExperimentResult {
  experimentId: number
  experimentName: string
  status?: number
  variants: AbVariant[]
  variantStats?: AbVariant[]
  statisticalTest?: AbStatisticalTest
  winnerVariantId: number | null
  totalExposures: number
  totalConversions: number
  totalSamples?: number
  overallConversionRate: number
  dailyTrends?: AbDailyTrend[]
}

export interface AbDailyTrend {
  date: string
  variantId?: number
  variantName?: string
  exposures?: number
  conversions?: number
  conversionRate?: number
  variantAViews?: number
  variantBViews?: number
  variantAConversions?: number
  variantBConversions?: number
  variantAConversionRate?: number
  variantBConversionRate?: number
}

export interface AbSegmentAnalysisRow {
  segment: string
  variants: Array<{
    variantId?: number
    variantName?: string
    exposures?: number
    conversions?: number
    conversionRate?: number
  }>
}

function toNumber(value: unknown, fallback = 0): number {
  return typeof value === 'number' ? value : Number(value ?? fallback) || fallback
}

function normalizeVariant(raw: unknown): AbVariant {
  const v = normalizeRecord(raw, ['variant', 'variantStat'])
  const conversionRateRaw = toNumber(v.conversionRate)
  const conversionRate = conversionRateRaw > 1 ? conversionRateRaw / 100 : conversionRateRaw
  const viewCount = toNumber(v.viewCount ?? v.exposures)
  const conversionCount = toNumber(v.conversionCount ?? v.conversions)
  const isWinner = v.isWinner === true || v.isWinner === 1
  return {
    ...(v as unknown as AbVariant),
    id: toNumber(v.id ?? v.variantId),
    experimentId: toNumber(v.experimentId),
    variantName: String(v.variantName ?? v.name ?? '-'),
    variantType: v.variantType == null ? undefined : String(v.variantType),
    content: v.content == null ? undefined : String(v.content),
    entityType: v.entityType == null ? undefined : String(v.entityType),
    entityId: v.entityId == null ? undefined : toNumber(v.entityId),
    styleCode: v.styleCode == null ? undefined : String(v.styleCode),
    viewCount,
    clickCount: toNumber(v.clickCount),
    conversionCount,
    conversionRate,
    isWinner,
    exposures: viewCount,
    conversions: conversionCount,
    trafficRatio: toNumber(v.trafficRatio, 50),
    scriptStyle: String(v.styleCode ?? v.scriptStyle ?? v.content ?? ''),
  }
}

function normalizeExperiment(raw: unknown): AbExperiment {
  const e = normalizeRecord(raw, ['experiment'])
  const variants = normalizeResponseArray(e.variants ?? e.variantList ?? e.variantStats).map(normalizeVariant)
  const name = String(e.name ?? e.experimentName ?? '-')
  return {
    ...(e as unknown as AbExperiment),
    id: toNumber(e.id),
    ownerId: e.ownerId == null ? undefined : toNumber(e.ownerId),
    name,
    experimentName: name,
    description: String(e.description ?? ''),
    experimentType: String(e.experimentType ?? 'script_style'),
    targetEntityType: e.targetEntityType == null ? undefined : String(e.targetEntityType),
    targetEntityId: e.targetEntityId == null ? undefined : toNumber(e.targetEntityId),
    status: toNumber(e.status),
    winnerVariantId: e.winnerVariantId == null ? null : toNumber(e.winnerVariantId),
    conclusion: e.conclusion == null ? undefined : String(e.conclusion),
    startTime: String(e.startTime ?? ''),
    endTime: String(e.endTime ?? ''),
    createTime: String(e.createTime ?? ''),
    updateTime: e.updateTime == null ? undefined : String(e.updateTime),
    variants,
    trafficSplit: toNumber(e.trafficSplit, 0),
  }
}

function normalizeResult(raw: unknown): AbExperimentResult {
  const r = normalizeRecord(raw, ['result', 'experimentResult', 'statistics'])
  const stats = normalizeResponseArray(r.variantStats ?? r.stats).map(normalizeVariant)
  const variants = normalizeResponseArray(r.variants).map(normalizeVariant)
  const finalVariants = variants.length > 0 ? variants : stats
  const overallRaw = toNumber(r.overallConversionRate)
  return {
    ...(r as unknown as AbExperimentResult),
    experimentId: toNumber(r.experimentId),
    experimentName: String(r.experimentName ?? ''),
    status: r.status == null ? undefined : toNumber(r.status),
    variants: finalVariants,
    variantStats: stats,
    statisticalTest: r.statisticalTest as AbStatisticalTest | undefined,
    winnerVariantId: r.winnerVariantId == null ? null : toNumber(r.winnerVariantId),
    totalExposures: toNumber(r.totalSamples ?? r.totalExposures),
    totalSamples: toNumber(r.totalSamples ?? r.totalExposures),
    totalConversions: toNumber(r.totalConversions),
    overallConversionRate: overallRaw > 1 ? overallRaw / 100 : overallRaw,
    dailyTrends: normalizeResponseArray(r.dailyTrends ?? r.trends).map(normalizeDailyTrend),
  }
}

function normalizeDailyTrend(raw: unknown): AbDailyTrend {
  const d = normalizeRecord(raw, ['trend', 'dailyTrend'])
  return {
    date: String(d.date ?? ''),
    variantId: d.variantId == null ? undefined : toNumber(d.variantId),
    variantName: d.variantName == null ? undefined : String(d.variantName),
    exposures: d.exposures == null ? undefined : toNumber(d.exposures),
    conversions: d.conversions == null ? undefined : toNumber(d.conversions),
    conversionRate: d.conversionRate == null ? undefined : toNumber(d.conversionRate),
    variantAViews: d.variantAViews == null ? undefined : toNumber(d.variantAViews),
    variantBViews: d.variantBViews == null ? undefined : toNumber(d.variantBViews),
    variantAConversions: d.variantAConversions == null ? undefined : toNumber(d.variantAConversions),
    variantBConversions: d.variantBConversions == null ? undefined : toNumber(d.variantBConversions),
    variantAConversionRate: d.variantAConversionRate == null ? undefined : toNumber(d.variantAConversionRate),
    variantBConversionRate: d.variantBConversionRate == null ? undefined : toNumber(d.variantBConversionRate),
  }
}

function toExperimentSavePayload(params: Partial<AbExperimentSave>) {
  return {
    id: params.id,
    name: params.name ?? params.experimentName,
    description: params.description,
    experimentType: params.experimentType ?? 'script_style',
    status: params.status ?? 0,
    conclusion: params.conclusion,
    targetEntityType: params.targetEntityType,
    targetEntityId: params.targetEntityId,
    variants: params.variants?.map((variant, index) => ({
      id: variant.id,
      experimentId: variant.experimentId ?? 0,
      variantName: variant.variantName,
      variantType: variant.variantType ?? (index === 0 ? 'A' : 'B'),
      content: variant.content,
      entityType: variant.entityType,
      entityId: variant.entityId,
      styleCode: variant.styleCode ?? variant.scriptStyle,
    })),
  }
}

function dateRange(days: number) {
  const end = new Date()
  const start = new Date(end)
  start.setDate(end.getDate() - Math.max(days - 1, 0))
  const toIsoDate = (date: Date) => date.toISOString().slice(0, 10)
  return { startDate: toIsoDate(start), endDate: toIsoDate(end) }
}

export const abtestApi = {
  list: (params: AbQuery) =>
    request.post<PageResult<unknown>>('/abtest/experiment/list', {
      page: params.page,
      rows: params.rows,
      keyword: params.keyword ?? params.experimentName,
      experimentType: params.experimentType,
      status: params.status,
    }).then((page) => normalizeResponsePage<unknown, AbExperiment>(page, normalizeExperiment, params.page ?? 0, params.rows ?? 20)),
  get: (id: number) =>
    request.post<unknown>('/abtest/experiment/get', undefined, { params: { id } }).then(normalizeExperiment),
  save: (params: Partial<AbExperimentSave>) =>
    request.post<number>('/abtest/experiment/save', toExperimentSavePayload(params)),
  delete: (id: number) => request.post<void>('/abtest/experiment/delete', undefined, { params: { id } }),
  updateStatus: (id: number, status: number) => request.post<void>('/abtest/experiment/update-status', undefined, { params: { id, status } }),
  setWinner: (experimentId: number, variantId: number, conclusion?: string) =>
    request.post<void>('/abtest/experiment/set-winner', { experimentId, variantId, conclusion }),
  result: (experimentId: number) =>
    request.post<unknown>('/abtest/experiment/result', undefined, { params: { experimentId } }).then(normalizeResult),
  dailyTrend: (experimentId: number, days = 7) => {
    const { startDate, endDate } = dateRange(days)
    return request.post<unknown>('/abtest/experiment/daily-trend', undefined, { params: { experimentId, startDate, endDate } })
      .then((list) => normalizeResponseArray(list).map(normalizeDailyTrend))
  },
  segmentAnalysis: (_experimentId: number, _dimension: string): Promise<AbSegmentAnalysisRow[]> =>
    Promise.reject(new Error('后端暂无 /abtest/experiment/segment-analysis 接口')),

  variantSave: (params: Partial<AbVariantSave>) => request.post<number>('/abtest/variant/save', {
    id: params.id,
    experimentId: params.experimentId,
    variantName: params.variantName,
    variantType: params.variantType,
    content: params.content,
    entityType: params.entityType,
    entityId: params.entityId,
    styleCode: params.styleCode ?? params.scriptStyle,
  }),
  variantDelete: (id: number) => request.post<void>('/abtest/variant/delete', undefined, { params: { id } }),

  // Convenience status actions: backend mapping is 0=draft, 1=running, 2=completed, 3=paused.
  start: (id: number) => request.post<void>('/abtest/experiment/update-status', undefined, { params: { id, status: 1 } }),
  pause: (id: number) => request.post<void>('/abtest/experiment/update-status', undefined, { params: { id, status: 3 } }),
  stop: (id: number) => request.post<void>('/abtest/experiment/update-status', undefined, { params: { id, status: 2 } }),

  eventRecord: (params: { experimentId: number; variantId: number; eventType: string; userFingerprint?: string; sessionId?: string }) =>
    request.post<void>('/abtest/event/record', {
      experimentId: params.experimentId,
      variantId: params.variantId,
      eventType: params.eventType,
      userFingerprint: params.userFingerprint ?? `browser:${Date.now()}`,
      sessionId: params.sessionId,
    }),
}
