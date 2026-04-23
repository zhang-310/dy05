import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface DyProduct {
  id: number; userId: number; productName: string; productCode: string
  category: string; brand: string; price: number; costPrice: number
  profitMarginPct: number; inventory: number; unit: string; weight: number
  mainImage: string; description: string; highlights: string; sellingPoints: string
  status: number; createTime: string; updateTime: string
}
export interface ProductQuery { page?: number; rows?: number; productName?: string; category?: string; status?: number; brand?: string }
export interface ProductSave {
  id?: number; productName: string; productCode?: string; category?: string; brand?: string
  price: number; costPrice?: number; profitMarginPct?: number; inventory?: number
  unit?: string; weight?: number; mainImage?: string; description?: string
  highlights?: string; sellingPoints?: string; status: number
}

export interface ProductScript {
  id: number; productId: number; scriptTitle: string; scriptContent: string
  scriptType: string; style: string; duration: number; useCount: number
  rating: number; status: number; createTime: string
  version?: number
}
export interface ProductScriptSave {
  id?: number; productId: number; scriptTitle: string; scriptContent: string
  scriptType?: string; style?: string; duration?: number; status?: number
}

export interface ProductScriptVersion {
  id: number; scriptId: number; versionNo: string; content: string
  changeNote: string; isActive: number; createTime: string
}

export interface SalesHistory {
  id: number; productId: number; saleDate: string; quantity: number
  revenue: number; platform: string; createTime: string
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

export interface EffectivenessScoreItem {
  productId: number; productName: string; imageUrl?: string
  avgScore: number; useCount: number; conversionRate: number
  tag: string; trend: 'up' | 'down' | 'flat'
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
  return request.post<string>('/product/infer-type', { id: productId })
}

export const productApi = {
  list: (p: ProductQuery) => request.post<PageResult<DyProduct>>('/product/search', p),
  get: (id: number) => request.post<DyProduct>('/product/get', { id }),
  save: (p: Partial<ProductSave>) => request.post<void>('/product/save', p),
  delete: (id: number) => request.post<void>('/product/delete', { id }),
  batchDelete: (ids: number[]) => request.post<void>('/product/batch-delete', { ids }),
  publish: (id: number) => request.post<void>('/product/publish', { id }),
  unpublish: (id: number) => request.post<void>('/product/unpublish', { id }),
  setFeatured: (id: number) => request.post<void>('/product/set-featured', { id }),
  updateInventory: (id: number, delta: number) => request.post<void>('/product/update-inventory', { id, delta }),
  extractFromLink: (url: string) => request.post<Record<string, string>>('/product/extract-from-link', { url }),
  importPaiping: (data: Record<string, unknown>) => request.post<Record<string, unknown>>('/product/import-paiping', data),
  salesHistorySearch: (p: { productId?: number; page?: number; rows?: number }) => request.post<PageResult<SalesHistory>>('/product/sales-history/search', p),
  salesHistorySave: (p: Partial<SalesHistory>) => request.post<number>('/product/sales-history/save', p),

  scriptList: (p: { productId: number; page?: number; rows?: number }) => request.post<PageResult<ProductScript>>('/product/script/search', p),
  scriptGet: (id: number) => request.post<ProductScript>('/product/script/get', { id }),
  scriptSave: (p: Partial<ProductScriptSave>) => request.post<void>('/product/script/save', p),
  scriptDelete: (id: number) => request.post<void>('/product/script/delete', { id }),
  scriptGenerate: (productId: number, style?: string) => request.post<ProductScript>('/product/script/generate', { productId, style }),
  scriptStatistics: (productId: number) => request.post<Record<string, unknown>>('/product/script/statistics', { productId }),
  scriptActivate: (id: number) => request.post<void>('/product/script/activate', { id }),

  scriptVersionList: (scriptId: number) => request.post<ProductScriptVersion[]>('/product/script-version/list', { scriptId }),
  scriptVersionSave: (p: Partial<ProductScriptVersion>) => request.post<number>('/product/script-version/save', p),
  scriptVersionActivate: (id: number) => request.post<void>('/product/script-version/activate', { id }),
  scriptVersionDelete: (id: number) => request.post<void>('/product/script-version/delete', { id }),

  stylePresetList: () => request.post<StylePreset[]>('/product/style-preset/list', {}),
  stylePresetSave: (p: Partial<StylePreset>) => request.post<StylePreset>('/product/style-preset/save', p),
  stylePresetDelete: (id: number) => request.post<void>('/product/style-preset/delete', { id }),
  stylePresetRecommend: (productId: number) => request.post<string[]>('/product/style-preset/recommend', { productId }),

  effectivenessRanking: (p: Record<string, unknown>) => request.post<EffectivenessRankingResult>('/product/script-effectiveness/ranking', p),
  effectivenessCompare: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/product/script-effectiveness/compare', p),

  scriptUsageList: (productId: number) => request.post<GmvContribItem[]>('/product/script/usage-list', { productId }),
  effectivenessTrend: (productId: number, params?: { days?: number }) => request.post<GmvTrendItem[]>('/product/script-effectiveness/trend', { productId, ...params }),
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
): Promise<{ projectId: number; projectName: string }> {
  return request.post('/product/script/export-to-shortvideo', params)
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

  const es = new EventSource(`/api/v1/product/script/generate-multi-sse?${queryParams.toString()}`)
  es.addEventListener('progress', (e: MessageEvent) => {
    try { callbacks.onProgress?.(JSON.parse(e.data)) } catch { /* ignore */ }
  })
  es.addEventListener('done', () => { callbacks.onDone?.(); es.close() })
  es.onerror = () => { callbacks.onError?.(new Error('SSE error')); es.close() }
  return { abort: () => es.close() }
}

