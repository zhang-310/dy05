import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import { getToken } from '@/utils/auth'
import {
  isRecord,
  normalizePage as normalizeResponsePage,
  normalizeRows,
  parseJsonValue,
} from '@/utils/response-normalize'

export interface DyProduct {
  id: number; userId: number; productName: string; productCode: string
  sku?: string; productCategory?: string; manufacturer?: string
  category: string; brand: string; price: number; costPrice: number
  profitMarginPct: number; inventory: number; unit: string; weight: number
  mainImage: string; imageUrl?: string; description: string; highlights: string; sellingPoints: string
  productLink?: string; aiSellingPoints?: string
  status: number; createTime: string; updateTime: string
}
export interface ProductQuery {
  page?: number; rows?: number; productName?: string; category?: string; status?: number; brand?: string
  keyword?: string; productCategory?: string
}
export interface ProductSave {
  id?: number; productName: string; productCode?: string; sku?: string; category?: string; productCategory?: string
  brand?: string; manufacturer?: string; price: number; costPrice?: number; profitMarginPct?: number; inventory?: number
  unit?: string; weight?: number; mainImage?: string; imageUrl?: string; description?: string
  highlights?: string; sellingPoints?: string; aiSellingPoints?: string; productLink?: string; status: number
}

export interface ProductExtractResult {
  productName?: string
  imageUrl?: string
  description?: string
  aiSellingPoints?: string
}

export interface ProductReadinessItem {
  dimension: string
  status: 'ok' | 'warn' | 'error'
  message: string
  score: number
}

export interface ProductReadinessResult {
  productId: number
  productName: string
  overallScore: number
  readyForLive: boolean
  items: ProductReadinessItem[]
}

export interface ProductScript {
  id: number; productId: number; scriptTitle: string; scriptContent: string
  scriptType: string; style: string; duration: number; useCount: number
  rating: number; status: number; createTime: string
  version?: number
  isActive?: boolean
  personaId?: number
  source?: string
  scene?: string
}
export interface ProductScriptSave {
  id?: number; productId: number; scriptTitle: string; scriptContent: string
  scriptType?: string; style?: string; duration?: number; status?: number
}

export interface ProductScriptVersion {
  id: number
  productId?: number
  scriptId?: number
  versionNumber?: number
  versionNo?: string
  content?: string
  scriptContent?: string
  style?: string
  effectivenessScore?: number
  usageCount?: number
  conversionRate?: number
  likesCount?: number
  commentsCount?: number
  isActive?: boolean | number
  isRecommended?: boolean
  archived?: boolean
  ownerId?: number
  changeNote?: string
  createTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface SalesHistory {
  id: number; productId: number; saleTime?: string; saleQuantity?: number
  saleAmount?: number; channelSource?: string; createTime: string
  /** @deprecated 后端真实字段为 saleTime */
  saleDate?: string
  /** @deprecated 后端真实字段为 saleQuantity */
  quantity?: number
  /** @deprecated 后端真实字段为 saleAmount */
  revenue?: number
  /** @deprecated 后端真实字段为 channelSource */
  platform?: string
  sessionId?: string
}

export interface SalesHistorySave {
  id?: number
  productId: number
  saleAmount: number
  saleQuantity?: number
  saleTime?: string
  channelSource?: string
  sessionId?: string
}

export interface SalesHistorySearchParams {
  productId?: number
  page?: number
  rows?: number
  channelSource?: string
  sessionId?: string
  startTime?: string
  endTime?: string
}

export interface StylePreset {
  id: number; presetName: string; presetCode: string; styleValue: string
  category: string; description: string; isEnabled: boolean; sortOrder: number
}

export interface ScriptUsage {
  scriptId: number
  scriptTitle: string
  scriptContent: string
  style: string
  useCount: number
  lastUsedAt: string
  avgRating: number
}

export interface ScriptEffectivenessTrend {
  date: string
  useCount: number
  avgRating: number
  conversionRate: number
}

/** GMV趋势项 — 后端返回 ScriptEffectivenessTrend 并附加可选 gmv 字段 */
export interface GmvTrendItem {
  date: string
  score?: number
  scoreLevel?: string
  usageCount?: number
  likesCount?: number
  gmv?: number
  salesAmount?: number
  useCount?: number
  avgRating?: number
  conversionRate?: number
}

/** GMV贡献项 — 后端返回 ScriptUsage 并附加可选贡献字段 */
export interface GmvContribItem {
  scriptId?: number
  scriptTitle?: string
  totalGmv?: number
  gmv?: number
  sessionTitle?: string
  liveTitle?: string
  totalOrders?: number
  orders?: number
  sessionId?: number
  liveSessionId?: number
  versionLabel?: string
  gmvLift?: number
  useCount?: number
  avgRating?: number
  lastUsedAt?: string
}

export interface ProductScriptUsageStats {
  totalScripts: number
  activeScripts: number
  byType: Record<string, number>
  byStyle: Record<string, number>
  bySource: Record<string, number>
  avgDuration: number
  totalTokens: number
}

export interface ProductScriptUsageResult {
  list: GmvContribItem[]
  stats: ProductScriptUsageStats
}

export interface EffectivenessScoreItem {
  productId?: number; productName?: string; imageUrl?: string
  versionId?: number; versionNumber?: number; style?: string; score?: number; scoreLevel?: string
  avgScore: number; useCount: number; usageCount?: number; conversionRate: number
  tag: string; trend: 'up' | 'down' | 'flat'
  likesCount?: number; commentsCount?: number; isRecommended?: boolean; lastUpdated?: string; rankingType?: string
}

export interface EffectivenessRankingResult {
  list: EffectivenessScoreItem[]
  total: number
  summary: {
    dates: string[]
    avgScores: number[]
    avgScore?: number
    maxScore?: number
    scoredCount?: number
    avgConversionRate?: number
  }
}

export function searchProducts(params: ProductQuery): ReturnType<typeof productApi.list> {
  return productApi.list(params)
}

/** 推断商品产品类型（主力/引流/利润等），返回逗号分隔字符串 */
export function inferProductType(productId: number): Promise<string> {
  return request.post<string>('/product/infer-product-type', { productId })
}

type ProductScriptListResponse = unknown
type EffectivenessRankingResponse = PageResult<EffectivenessScoreItem> | EffectivenessRankingResult

type ProductSearchPayload = Omit<ProductQuery, 'productName' | 'category' | 'brand'> & {
  keyword?: string
  productCategory?: string
}

type ProductSavePayload = Omit<Partial<ProductSave>, 'productCode' | 'category' | 'brand' | 'mainImage' | 'sellingPoints'> & {
  sku?: string
  productCategory?: string
  manufacturer?: string
  imageUrl?: string
  aiSellingPoints?: string
}

function normalizeProduct(product: DyProduct): DyProduct {
  return {
    ...product,
    productCode: product.productCode ?? product.sku ?? '',
    category: product.category ?? product.productCategory ?? '',
    brand: product.brand ?? product.manufacturer ?? '',
    mainImage: product.mainImage ?? product.imageUrl ?? '',
    imageUrl: product.imageUrl ?? product.mainImage ?? '',
    sellingPoints: product.sellingPoints ?? product.aiSellingPoints ?? '',
    inventory: Number(product.inventory ?? 0),
    price: Number(product.price ?? 0),
    costPrice: Number(product.costPrice ?? 0),
    profitMarginPct: Number(product.profitMarginPct ?? 0),
    status: Number(product.status ?? 1),
    unit: product.unit ?? '件',
  }
}

function normalizeProductPage(res: unknown, fallback: ProductQuery): PageResult<DyProduct> {
  return normalizeResponsePage<DyProduct, DyProduct>(
    res,
    normalizeProduct,
    fallback.page ?? 0,
    fallback.rows ?? 20,
  )
}

function toProductSearchPayload(params: ProductQuery): ProductSearchPayload {
  const payload: ProductSearchPayload = { ...params }
  delete (payload as ProductSearchPayload & { productName?: string }).productName
  delete (payload as ProductSearchPayload & { category?: string }).category
  delete (payload as ProductSearchPayload & { brand?: string }).brand
  delete payload.productCategory
  const keyword = String(params.keyword ?? params.productName ?? '').trim()
  if (keyword) payload.keyword = keyword
  const productCategory = String(params.productCategory ?? params.category ?? '').trim()
  if (productCategory) payload.productCategory = productCategory
  return payload
}

function toProductSavePayload(params: Partial<ProductSave>): ProductSavePayload {
  const payload: ProductSavePayload = { ...params }
  delete (payload as ProductSavePayload & { productCode?: string }).productCode
  delete (payload as ProductSavePayload & { category?: string }).category
  delete (payload as ProductSavePayload & { brand?: string }).brand
  delete (payload as ProductSavePayload & { mainImage?: string }).mainImage
  delete (payload as ProductSavePayload & { sellingPoints?: string }).sellingPoints
  const sku = String(params.sku ?? params.productCode ?? '').trim()
  if (sku) payload.sku = sku
  const productCategory = String(params.productCategory ?? params.category ?? '').trim()
  if (productCategory) payload.productCategory = productCategory
  const manufacturer = String(params.manufacturer ?? params.brand ?? '').trim()
  if (manufacturer) payload.manufacturer = manufacturer
  const imageUrl = String(params.imageUrl ?? params.mainImage ?? '').trim()
  if (imageUrl) payload.imageUrl = imageUrl
  const aiSellingPoints = String(params.aiSellingPoints ?? params.sellingPoints ?? '').trim()
  if (aiSellingPoints) payload.aiSellingPoints = aiSellingPoints
  return payload
}

function numberOrUndefined(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

function boolish(value: unknown): boolean | undefined {
  if (value === true || value === 1 || value === '1' || value === 'true') return true
  if (value === false || value === 0 || value === '0' || value === 'false') return false
  return undefined
}

function stringOrUndefined(value: unknown): string | undefined {
  if (value == null) return undefined
  const text = String(value)
  return text === '' ? undefined : text
}

function normalizeNumberValue(value: unknown, fallback = 0): number {
  const parsed = parseJsonValue(value)
  if (typeof parsed === 'number' || typeof parsed === 'string') {
    const n = Number(parsed)
    return Number.isFinite(n) ? n : fallback
  }
  if (isRecord(parsed)) {
    for (const key of ['value', 'amount', 'quantity', 'total', 'data'] as const) {
      if (key in parsed) return normalizeNumberValue(parsed[key], fallback)
    }
  }
  return fallback
}

function normalizeSalesHistory(history: unknown): SalesHistory {
  const row = isRecord(history) ? history : {}
  const saleAmount = normalizeNumberValue(row.saleAmount ?? row.revenue, 0)
  const saleQuantity = numberOrUndefined(row.saleQuantity ?? row.quantity)
  const channelSource = stringOrUndefined(row.channelSource ?? row.platform)
  const saleTime = stringOrUndefined(row.saleTime ?? row.saleDate)
  return {
    ...(row as unknown as SalesHistory),
    id: numberOrUndefined(row.id ?? row.historyId) ?? 0,
    productId: numberOrUndefined(row.productId) ?? 0,
    saleAmount,
    saleQuantity,
    channelSource,
    saleTime,
    createTime: stringOrUndefined(row.createTime ?? row.createdAt) ?? '',
    saleDate: stringOrUndefined(row.saleDate ?? row.saleTime),
    quantity: saleQuantity,
    revenue: saleAmount,
    platform: channelSource,
    sessionId: stringOrUndefined(row.sessionId),
  }
}

function normalizeSalesHistoryPage(
  res: unknown,
  fallback: SalesHistorySearchParams,
): PageResult<SalesHistory> {
  return normalizeResponsePage<unknown, SalesHistory>(
    res,
    normalizeSalesHistory,
    fallback.page ?? 0,
    fallback.rows ?? 20,
  )
}

function normalizeStylePreset(preset: unknown): StylePreset {
  const row = isRecord(preset) ? preset : {}
  const enabled = boolish(row.isEnabled ?? row.enabled ?? row.active)
    ?? (numberOrUndefined(row.status) != null ? numberOrUndefined(row.status) === 1 : true)
  return {
    ...(row as unknown as StylePreset),
    id: numberOrUndefined(row.id ?? row.presetId) ?? 0,
    presetName: stringOrUndefined(row.presetName ?? row.name ?? row.label) ?? '',
    presetCode: stringOrUndefined(row.presetCode ?? row.code ?? row.styleCode ?? row.style) ?? '',
    styleValue: stringOrUndefined(row.styleValue ?? row.tone ?? row.styleName ?? row.value) ?? '',
    category: stringOrUndefined(row.category ?? row.scene) ?? '',
    description: stringOrUndefined(row.description ?? row.desc) ?? '',
    isEnabled: enabled,
    sortOrder: numberOrUndefined(row.sortOrder ?? row.orderNo ?? row.order) ?? 0,
  }
}

function normalizeStylePresetList(res: unknown): StylePreset[] {
  return normalizeRows<unknown>(res).map(normalizeStylePreset)
}

function normalizeStyleCodeList(res: unknown): string[] {
  return normalizeRows<unknown>(res)
    .map((item) => {
      if (typeof item === 'string' || typeof item === 'number') return String(item)
      const row = isRecord(item) ? item : {}
      return stringOrUndefined(row.presetCode ?? row.code ?? row.styleCode ?? row.style ?? row.value)
    })
    .filter((code): code is string => Boolean(code))
}

function normalizeProductScript(script: unknown): ProductScript {
  const row = isRecord(script) ? script : {}
  const version = numberOrUndefined(row.version ?? row.versionNo ?? row.versionNumber)
  const active = boolish(row.isActive ?? row.active ?? row.current) ?? numberOrUndefined(row.status) === 1
  const scriptContent = stringOrUndefined(row.scriptContent ?? row.content ?? row.text) ?? ''
  const style = stringOrUndefined(row.style ?? row.styleCode) ?? 'default'
  return {
    ...(row as unknown as ProductScript),
    id: numberOrUndefined(row.id ?? row.scriptId) ?? 0,
    productId: numberOrUndefined(row.productId) ?? 0,
    status: active ? 1 : numberOrUndefined(row.status) ?? 0,
    isActive: active,
    scriptTitle: stringOrUndefined(row.scriptTitle ?? row.title ?? row.name) || `V${version ?? numberOrUndefined(row.id ?? row.scriptId) ?? '-'}`,
    scriptContent,
    scriptType: stringOrUndefined(row.scriptType ?? row.type) ?? 'formal',
    style,
    version,
    useCount: numberOrUndefined(row.useCount ?? row.usageCount ?? row.usedCount) ?? 0,
    rating: numberOrUndefined(row.rating ?? row.score ?? row.effectivenessScore) ?? 0,
    duration: numberOrUndefined(row.duration ?? row.durationSec ?? row.durationSeconds) ?? 0,
    personaId: numberOrUndefined(row.personaId),
    source: stringOrUndefined(row.source),
    scene: stringOrUndefined(row.scene),
    createTime: stringOrUndefined(row.createTime ?? row.createdAt) ?? '',
  }
}

function normalizeScriptList(
  res: ProductScriptListResponse,
  fallback: { page?: number; rows?: number }
): PageResult<ProductScript> {
  return normalizeResponsePage<unknown, ProductScript>(
    res,
    normalizeProductScript,
    fallback.page ?? 0,
    fallback.rows ?? 100,
  )
}

function normalizeNumberMap(raw: unknown): Record<string, number> {
  const parsed = parseJsonValue(raw)
  if (!isRecord(parsed) || Array.isArray(parsed)) return {}
  return Object.entries(parsed).reduce<Record<string, number>>((acc, [key, value]) => {
    const n = Number(value)
    if (Number.isFinite(n)) acc[key] = n
    return acc
  }, {})
}

function normalizeGmvContribItem(item: unknown): GmvContribItem {
  const row = isRecord(item) ? item : {}
  return {
    ...(row as unknown as GmvContribItem),
    scriptId: numberOrUndefined(row.scriptId ?? row.id),
    scriptTitle: stringOrUndefined(row.scriptTitle ?? row.title ?? row.name),
    totalGmv: numberOrUndefined(row.totalGmv ?? row.gmv ?? row.salesAmount ?? row.revenue),
    gmv: numberOrUndefined(row.gmv ?? row.totalGmv ?? row.salesAmount ?? row.revenue),
    sessionTitle: stringOrUndefined(row.sessionTitle ?? row.liveTitle ?? row.title ?? row.sessionName),
    liveTitle: stringOrUndefined(row.liveTitle ?? row.sessionTitle),
    totalOrders: numberOrUndefined(row.totalOrders ?? row.orders ?? row.orderCount),
    orders: numberOrUndefined(row.orders ?? row.totalOrders ?? row.orderCount),
    sessionId: numberOrUndefined(row.sessionId ?? row.liveSessionId),
    liveSessionId: numberOrUndefined(row.liveSessionId ?? row.sessionId),
    versionLabel: stringOrUndefined(row.versionLabel ?? row.versionName),
    gmvLift: numberOrUndefined(row.gmvLift ?? row.liftPct ?? row.lift),
    useCount: numberOrUndefined(row.useCount ?? row.usageCount ?? row.usedCount),
    avgRating: numberOrUndefined(row.avgRating ?? row.rating ?? row.score),
    lastUsedAt: stringOrUndefined(row.lastUsedAt ?? row.updateTime ?? row.createTime),
  }
}

function pickScriptUsageStatsSource(raw: unknown): Record<string, unknown> {
  const parsed = parseJsonValue(raw)
  if (!isRecord(parsed) || Array.isArray(parsed)) return {}
  const candidates = [
    parsed,
    parsed.data,
    parsed.result,
    parsed.detail,
    parsed.record,
    parsed.item,
    parsed.payload,
    parsed.body,
    parsed.stats,
    parsed.summary,
  ]
  const statKeys = ['totalScripts', 'activeScripts', 'byType', 'byStyle', 'bySource', 'avgDuration', 'totalTokens']
  for (const candidate of candidates) {
    if (isRecord(candidate) && statKeys.some(key => key in candidate)) return candidate
  }
  return parsed
}

function normalizeScriptUsageStats(raw: unknown): ProductScriptUsageStats {
  const row = pickScriptUsageStatsSource(raw)
  return {
    totalScripts: numberOrUndefined(row.totalScripts ?? row.scriptCount ?? row.total) ?? 0,
    activeScripts: numberOrUndefined(row.activeScripts ?? row.activeCount) ?? 0,
    byType: normalizeNumberMap(row.byType ?? row.typeStats),
    byStyle: normalizeNumberMap(row.byStyle ?? row.styleStats),
    bySource: normalizeNumberMap(row.bySource ?? row.sourceStats),
    avgDuration: numberOrUndefined(row.avgDuration ?? row.averageDuration) ?? 0,
    totalTokens: numberOrUndefined(row.totalTokens ?? row.tokens) ?? 0,
  }
}

function normalizeScriptUsageResult(res: unknown): ProductScriptUsageResult {
  return {
    list: normalizeRows<unknown>(res).map(normalizeGmvContribItem),
    stats: normalizeScriptUsageStats(res),
  }
}

function toScore10(score: unknown): number {
  const n = Number(score ?? 0)
  if (!Number.isFinite(n)) return 0
  return n > 10 ? n / 10 : n
}

function normalizeEffectivenessRanking(
  res: EffectivenessRankingResponse,
  fallback: { productId?: number; page?: number; rows?: number }
): EffectivenessRankingResult {
  const normalizedPage = normalizeResponsePage<EffectivenessScoreItem, EffectivenessScoreItem>(
    res,
    item => item,
    fallback.page ?? 0,
    fallback.rows ?? 20,
  )
  const rawList = normalizedPage.list
  const list = rawList.map((item) => {
    const score10 = toScore10(item.avgScore ?? item.score)
    const useCount = Number(item.useCount ?? item.usageCount ?? 0)
    const conversionRate = Number(item.conversionRate ?? 0)
    return {
      ...item,
      productId: item.productId ?? fallback.productId,
      productName: item.productName ?? (item.versionNumber != null ? `V${item.versionNumber} ${item.style ?? '话术版本'}` : item.style ?? `版本 ${item.versionId ?? '-'}`),
      avgScore: score10,
      useCount,
      conversionRate,
      tag: item.tag ?? item.style ?? item.scoreLevel ?? '-',
      trend: item.trend ?? 'flat',
    }
  })
  const scores = list.map((item) => item.avgScore).filter((score) => Number.isFinite(score))
  const conversionRates = list.map((item) => Number(item.conversionRate ?? 0)).filter((rate) => Number.isFinite(rate))
  const parsed = parseJsonValue(res)
  const responseSummary = isRecord(parsed) && isRecord(parsed.summary) ? parsed.summary : null
  const summary = responseSummary ? {
    dates: Array.isArray(responseSummary.dates) ? responseSummary.dates.map(String) : [],
    avgScores: Array.isArray(responseSummary.avgScores) ? responseSummary.avgScores.map(Number).filter(Number.isFinite) : [],
    avgScore: responseSummary.avgScore != null ? Number(responseSummary.avgScore) : undefined,
    maxScore: responseSummary.maxScore != null ? Number(responseSummary.maxScore) : undefined,
    scoredCount: responseSummary.scoredCount != null ? Number(responseSummary.scoredCount) : undefined,
    avgConversionRate: responseSummary.avgConversionRate != null ? Number(responseSummary.avgConversionRate) : undefined,
  } : {
    dates: [],
    avgScores: [],
    avgScore: scores.length ? scores.reduce((sum, score) => sum + score, 0) / scores.length : 0,
    maxScore: scores.length ? Math.max(...scores) : 0,
    scoredCount: scores.filter((score) => score > 0).length,
    avgConversionRate: conversionRates.length ? conversionRates.reduce((sum, rate) => sum + rate, 0) / conversionRates.length : 0,
  }
  return {
    list,
    total: normalizedPage.total,
    summary,
  }
}

export const productApi = {
  list: async (p: ProductQuery) => normalizeProductPage(
    await request.post<PageResult<DyProduct>>('/product/search', toProductSearchPayload(p)),
    p,
  ),
  get: async (id: number) => normalizeProduct(await request.post<DyProduct>('/product/get', { id })),
  save: (p: Partial<ProductSave>) => request.post<void>('/product/save', toProductSavePayload(p)),
  delete: (id: number) => request.post<void>('/product/delete', { id }),
  batchDelete: (ids: number[]) => request.post<void>('/product/batch-delete', { ids }),
  publish: (id: number) => request.post<void>('/product/publish', { id }),
  unpublish: (id: number) => request.post<void>('/product/unpublish', { id }),
  setFeatured: (id: number) => request.post<void>('/product/set-featured', { id }),
  updateInventory: (id: number, delta: number) => request.post<void>('/product/update-inventory', { id, delta }),
  extractFromLink: (productLink: string) => request.post<ProductExtractResult>('/product/extract-from-link', { productLink: productLink.trim() }),
  readiness: (productId: number) => request.post<ProductReadinessResult>('/product/readiness', { productId }),
  importPaiping: (data: Record<string, unknown>) => request.post<Record<string, unknown>>('/product/import-paiping', data),
  salesHistorySearch: async (p: SalesHistorySearchParams) => normalizeSalesHistoryPage(
    await request.post<unknown>('/product/sales-history/search', p),
    p,
  ),
  salesHistorySave: (p: SalesHistorySave) => request.post<number>('/product/sales-history/save', p),
  salesHistoryTotalSalesAmount: async (productId: number) => normalizeNumberValue(
    await request.post<unknown>('/product/sales-history/total-sales-amount', { productId }),
    0,
  ),
  salesHistoryTotalSalesQuantity: async (productId: number) => normalizeNumberValue(
    await request.post<unknown>('/product/sales-history/total-sales-quantity', { productId }),
    0,
  ),

  scriptList: async (p: { productId: number; page?: number; rows?: number }) => {
    const res = await request.post<ProductScriptListResponse>('/product/script/search', p)
    return normalizeScriptList(res, p)
  },
  scriptGet: (id: number) => request.post<ProductScript>('/product/script/get', { id }),
  scriptSave: (p: Partial<ProductScriptSave>) => request.post<void>('/product/script/save', p),
  scriptDelete: (id: number) => request.post<void>('/product/script/delete', { id }),
  scriptGenerate: (productId: number, style?: string) => request.post<ProductScript>('/product/script/generate', { productId, style }),
  scriptStatistics: (productId: number) => request.post<Record<string, unknown>>('/product/script/statistics', { productId }),
  scriptActivate: (id: number) => request.post<void>('/product/script/activate', { id }),

  scriptVersionList: (productId: number, p?: { page?: number; rows?: number }) => request.post<PageResult<ProductScriptVersion>>('/product/script-version/list', { productId, page: p?.page ?? 0, rows: p?.rows ?? 20 }),
  scriptVersionSave: (p: Partial<ProductScriptVersion>) => request.post<ProductScriptVersion>('/product/script-version/save', p),
  scriptVersionActivate: (id: number) => request.post<ProductScriptVersion>('/product/script-version/update-status', undefined, { params: { id, isActive: true } }),
  scriptVersionDelete: (id: number) => request.post<boolean>(`/product/script-version/delete/${id}`),

  /** 管理页需要包含停用预设；生成对话框使用 /product/style-preset/list 获取启用项 */
  stylePresetList: async () => normalizeStylePresetList(await request.post<unknown>('/product/style-preset/list-all', {})),
  stylePresetSave: async (p: Partial<StylePreset>) => normalizeStylePreset(await request.post<unknown>('/product/style-preset/save', p)),
  stylePresetDelete: (id: number) => request.post<void>('/product/style-preset/delete', { id }),
  stylePresetRecommend: async (productId: number) => normalizeStyleCodeList(
    await request.post<unknown>('/product/style-preset/recommend', { productId }),
  ),

  effectivenessRanking: async (p: { productId?: number; topN?: number; sortBy?: string; page?: number; rows?: number }) => normalizeEffectivenessRanking(
    await request.post<EffectivenessRankingResponse>('/product/script-effectiveness/ranking', undefined, { params: p }),
    p,
  ),
  effectivenessCompare: (p: { versionIds: number[] }) => request.post<Record<string, unknown>>('/product/script-effectiveness/compare', undefined, { params: { versionIds: p.versionIds } }),
  effectivenessRecalculate: (productId: number) => request.post<number>('/product/script-effectiveness/recalculate', undefined, { params: { productId } }),
  effectivenessStyleComparison: (productId: number) => request.post<Record<string, unknown>>('/product/script-effectiveness/style-comparison', undefined, { params: { productId } }),
  effectivenessTrend: (versionId: number, params?: { days?: number }) => request.post<GmvTrendItem[]>('/product/script-effectiveness/trend', undefined, { params: { versionId, ...params } }),

  scriptUsageList: async (productId: number) => normalizeScriptUsageResult(
    await request.post<unknown>('/product/script/usage-list', { productId }),
  ),
}

export interface BatchScriptProgressEvent {
  style: string
  status: 'pending' | 'loading' | 'done' | 'failed'
  success?: boolean
  message?: string
  script?: ProductScript
  abExperimentId?: number
  abVariantId?: number
}

export interface MultiStyleScriptParams {
  productId: number
  styles: string[]
  scriptType?: string
  fusionMode?: boolean  // 风格融合模式：多风格融合为一条话术
  styleWeights?: Record<string, number>  // 风格权重配置（融合模式下生效），key=风格代码，value=权重（0-1）
  fusionStrategy?: string  // 融合策略：blended/sequential/layered/alternating/progressive
  personaId?: number
  duration?: number
  scene?: string
  useKbRef?: boolean
  kbCategories?: string[]
}

export interface MultiStyleScriptCallbacks {
  onProgress?: (event: BatchScriptProgressEvent) => void
  onDone?: () => void
  onError?: (err: Error) => void
}

export interface StylePreview {
  style: string
  styleName: string
  content: string
}

export interface PreviewStylesResponse {
  previews: StylePreview[]
}

/**
 * 预览多个风格的话术片段
 */
export function previewStyles(params: {
  productId: number
  styles: string[]
  scriptType: string
  personaId?: number
  scene?: string
  useKbRef?: boolean
  kbCategories?: string[]
}): Promise<PreviewStylesResponse> {
  return request.post('/product/script/preview-styles', params)
}

// ─── P1: 商品一键导出到短视频项目 ──────────────────────────────────────────
export interface ExportToShortVideoParams {
  productId: number
  versionId?: number    // 指定话术版本，不传则用激活版本
  style?: string
  duration?: number     // 目标视频时长（秒）
  personaId?: number
}

export function exportProductToShortVideo(
  params: ExportToShortVideoParams
): Promise<{ scriptId: number; projectId: number; projectName: string }> {
  return request.post('/product/script/export-to-shortvideo', params)
}

function parseProductScriptSseEvent(raw: string): { eventType: string; data: string } {
  const dataLines: string[] = []
  let eventType = 'message'

  raw.split('\n').forEach((line) => {
    if (!line || line.startsWith(':')) return
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim() || 'message'
      return
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  })

  return { eventType, data: dataLines.join('\n') }
}

function getProductScriptSseErrorMessage(payload: unknown, fallback: string): string {
  const parsed = parseJsonValue(payload)
  if (typeof parsed === 'string') {
    const text = parsed.trim()
    if (!text) return fallback
    const nested = parseJsonValue(text)
    return nested === text ? text : getProductScriptSseErrorMessage(nested, fallback)
  }
  if (isRecord(parsed) && !Array.isArray(parsed)) {
    for (const key of ['error', 'message', 'msg', 'reason'] as const) {
      const value = parsed[key]
      if (typeof value === 'string' && value.trim()) return value.trim()
    }
    for (const key of ['data', 'result', 'detail'] as const) {
      if (parsed[key] != null) {
        const message = getProductScriptSseErrorMessage(parsed[key], '')
        if (message) return message
      }
    }
  }
  return fallback
}

export function generateMultiStyleScriptsSse(
  params: MultiStyleScriptParams,
  callbacks: MultiStyleScriptCallbacks
): { abort: () => void } {
  const queryParams = new URLSearchParams({
    productId: String(params.productId),
    styles: params.styles.join(','),
  })
  if (params.scriptType) queryParams.set('scriptType', params.scriptType)
  if (params.fusionMode) queryParams.set('fusionMode', 'true')
  if (params.styleWeights) queryParams.set('styleWeights', JSON.stringify(params.styleWeights))
  if (params.fusionStrategy) queryParams.set('fusionStrategy', params.fusionStrategy)
  if (params.personaId) queryParams.set('personaId', String(params.personaId))
  if (params.duration) queryParams.set('duration', String(params.duration))
  if (params.scene) queryParams.set('scene', params.scene)
  if (params.useKbRef !== undefined) queryParams.set('useKbRef', String(params.useKbRef))
  if (params.kbCategories && params.kbCategories.length > 0) queryParams.set('kbCategories', params.kbCategories.join(','))

  const controller = new AbortController()
  const token = getToken()
  const url = `/api/v1/product/script/generate-multi-sse?${queryParams.toString()}`
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    'Cache-Control': 'no-cache',
  }
  if (token) headers.Authorization = `Bearer ${token}`

  let completed = false
  let failed = false

  const dispatchEvent = (rawEvent: string) => {
    if (!rawEvent.trim() || completed || failed) return
    const { eventType, data } = parseProductScriptSseEvent(rawEvent)
    const parsed = data ? parseJsonValue(data) : undefined

    if (eventType === 'progress' && parsed !== undefined) {
      callbacks.onProgress?.(parsed as BatchScriptProgressEvent)
      return
    }
    if (eventType === 'done') {
      completed = true
      callbacks.onDone?.()
      return
    }
    if (eventType === 'error') {
      failed = true
      callbacks.onError?.(new Error(getProductScriptSseErrorMessage(parsed, 'SSE error')))
      controller.abort()
    }
  }

  const run = async () => {
    try {
      const response = await fetch(url, {
        method: 'GET',
        headers,
        signal: controller.signal,
      })

      const fallbackError = response.ok
        ? '接口没有返回 SSE 流'
        : `HTTP ${response.status}: ${response.statusText || 'SSE 请求失败'}`
      const contentType = response.headers.get('content-type') || ''
      if (!response.ok || !contentType.toLowerCase().includes('text/event-stream')) {
        const text = await response.text().catch(() => '')
        throw new Error(getProductScriptSseErrorMessage(text, fallbackError))
      }
      if (!response.body) {
        throw new Error('浏览器不支持读取 SSE 响应流')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (!controller.signal.aborted) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
        const rawEvents = buffer.split('\n\n')
        buffer = rawEvents.pop() ?? ''

        for (const rawEvent of rawEvents) {
          dispatchEvent(rawEvent)
          if (completed || failed || controller.signal.aborted) {
            await reader.cancel().catch(() => undefined)
            return
          }
        }
      }

      const tail = decoder.decode()
      if (tail) buffer += tail
      if (buffer.trim()) dispatchEvent(buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n'))
      if (!completed && !failed && !controller.signal.aborted) {
        callbacks.onError?.(new Error('SSE 连接已中断，未收到完成事件'))
      }
    } catch (error) {
      if (completed || failed || controller.signal.aborted) return
      callbacks.onError?.(error instanceof Error ? error : new Error(String(error)))
    }
  }

  void run()
  return { abort: () => controller.abort() }
}

