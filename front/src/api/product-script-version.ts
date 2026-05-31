import request from '@/utils/request'
import { isRecord, normalizePage as normalizeResponsePage, normalizeRows } from '@/utils/response-normalize'

export interface ProductScriptVersion {
  id: number
  productId: number
  scriptId?: number
  versionNumber: number
  content: string
  style?: string
  effectivenessScore?: number
  usageCount?: number
  conversionRate?: number
  likesCount?: number
  commentsCount?: number
  isActive?: boolean
  isRecommended?: boolean
  archived?: boolean
  ownerId?: number
  createdAt?: string
  updatedAt?: string

  /** @deprecated 兼容旧页面字段，真实后端字段为 content */
  scriptContent?: string
  /** @deprecated 兼容旧页面字段，真实后端字段为 isActive */
  status?: string
  /** @deprecated 兼容旧页面字段，真实后端字段为 effectivenessScore */
  score?: number
  /** @deprecated 兼容旧页面字段，真实后端字段为 createdAt */
  createTime?: string
}

export interface ProductScriptVersionQuery {
  productId?: number
  keyword?: string
  style?: string
  minScore?: number
  isActive?: boolean
  isRecommended?: boolean
  archived?: boolean
  page?: number
  rows?: number
}

export interface ProductScriptVersionSave {
  id?: number
  productId: number
  scriptId?: number
  content: string
  style?: string
  effectivenessScore?: number
  conversionRate?: number
  isActive?: boolean
  isRecommended?: boolean
  remark?: string
}

export interface ProductScriptRecommendResult {
  versions?: Array<Pick<ProductScriptVersion, 'id' | 'versionNumber' | 'style' | 'effectivenessScore' | 'usageCount' | 'conversionRate'> & {
    recommendScore?: number
  }>
  scores?: number[]
}

export interface EnsureOptimizationVersionResult {
  scriptId: number
  scriptVersionId: number
  created: boolean
}

type VersionRecord = Record<string, unknown>

function numberOrUndefined(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

function stringOrUndefined(value: unknown): string | undefined {
  if (value == null) return undefined
  const text = String(value)
  return text === '' ? undefined : text
}

function boolish(value: unknown): boolean | undefined {
  if (value === true || value === 1 || value === '1') return true
  if (value === false || value === 0 || value === '0') return false
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase()
    if (['true', 'active', 'enabled', 'enable', 'yes'].includes(normalized)) return true
    if (['false', 'inactive', 'disabled', 'disable', 'no'].includes(normalized)) return false
  }
  return undefined
}

function normalizeVersion(row: unknown, fallbackProductId?: number): ProductScriptVersion {
  const source: VersionRecord = isRecord(row) ? row : {}
  const isActive = boolish(source.isActive ?? source.active ?? source.current ?? source.status) ?? false
  const isRecommended = boolish(source.isRecommended ?? source.recommended)
  const archived = boolish(source.archived ?? source.isArchived)
  const content = stringOrUndefined(source.content ?? source.scriptContent ?? source.script ?? source.text) ?? ''
  const effectivenessScore = numberOrUndefined(source.effectivenessScore ?? source.score ?? source.rating) ?? 0
  const conversionRate = numberOrUndefined(source.conversionRate ?? source.conversion ?? source.convertRate) ?? 0
  const createdAt = stringOrUndefined(source.createdAt ?? source.createTime ?? source.createdTime)
  const updatedAt = stringOrUndefined(source.updatedAt ?? source.updateTime ?? source.updatedTime)
  const id = numberOrUndefined(source.id ?? source.versionId ?? source.scriptVersionId) ?? 0
  const productId = numberOrUndefined(source.productId) ?? fallbackProductId ?? 0
  const versionNumber = numberOrUndefined(source.versionNumber ?? source.versionNo ?? source.version) ?? id

  return {
    ...(source as Partial<ProductScriptVersion>),
    id,
    productId,
    scriptId: numberOrUndefined(source.scriptId ?? source.productScriptId),
    versionNumber,
    content,
    scriptContent: content,
    style: stringOrUndefined(source.style ?? source.styleCode),
    isActive,
    status: isActive ? 'active' : 'inactive',
    effectivenessScore,
    score: effectivenessScore,
    usageCount: numberOrUndefined(source.usageCount ?? source.useCount ?? source.usedCount) ?? 0,
    conversionRate,
    likesCount: numberOrUndefined(source.likesCount ?? source.likeCount) ?? 0,
    commentsCount: numberOrUndefined(source.commentsCount ?? source.commentCount) ?? 0,
    isRecommended,
    archived,
    ownerId: numberOrUndefined(source.ownerId ?? source.userId),
    createdAt,
    createTime: createdAt,
    updatedAt,
  }
}

function normalizeRecommendResult(raw: unknown, productId: number): ProductScriptRecommendResult {
  const source = isRecord(raw) ? raw : {}
  const data = isRecord(source.data) ? source.data : {}
  const rowsSource = source.versions ?? data.versions ?? data.records ?? data.list ?? raw
  const versions = normalizeRows<unknown>(rowsSource).map((row) => {
    const sourceRow = isRecord(row) ? row : {}
    const version = normalizeVersion(row, productId)
    return {
      id: version.id,
      versionNumber: version.versionNumber,
      style: version.style,
      effectivenessScore: version.effectivenessScore,
      usageCount: version.usageCount,
      conversionRate: version.conversionRate,
      recommendScore: numberOrUndefined(sourceRow.recommendScore ?? sourceRow.recommend_score ?? sourceRow.score),
    }
  })
  const scores = normalizeRows<unknown>(source.scores)
    .map(score => numberOrUndefined(score))
    .filter((score): score is number => score !== undefined)

  return {
    versions,
    scores: scores.length > 0 ? scores : versions.map(item => item.recommendScore ?? 0),
  }
}

export async function searchProductScriptVersions(data: ProductScriptVersionQuery = {}) {
  const res = await request.post<unknown>(
    '/product/script-version/search',
    undefined,
    {
      params: {
        keyword: data.keyword,
        style: data.style,
        minScore: data.minScore,
        page: data.page ?? 0,
        rows: data.rows ?? 20,
      },
    },
  )
  return normalizeResponsePage<unknown, ProductScriptVersion>(
    res,
    (item) => normalizeVersion(item, data.productId),
    data.page ?? 0,
    data.rows ?? 20,
  )
}

export async function listProductScriptVersions(data: ProductScriptVersionQuery = {}) {
  const res = await request.post<unknown>('/product/script-version/list', {
    productId: data.productId,
    keyword: data.keyword,
    style: data.style,
    isActive: data.isActive,
    isRecommended: data.isRecommended,
    archived: data.archived,
    minEffectivenessScore: data.minScore,
    page: data.page ?? 0,
    rows: data.rows ?? 20,
  })
  return normalizeResponsePage<unknown, ProductScriptVersion>(
    res,
    (item) => normalizeVersion(item, data.productId),
    data.page ?? 0,
    data.rows ?? 20,
  )
}

export async function getCurrentProductScriptVersion(productId: number) {
  const res = await request.post<unknown>(
    '/product/script-version/best',
    undefined,
    { params: { productId } },
  )
  return normalizeVersion(res, productId)
}

export async function getProductScriptVersion(id: number) {
  const res = await request.post<unknown>(`/product/script-version/detail/${id}`)
  return normalizeVersion(res)
}

export async function getProductScriptVersionHistory(productId: number) {
  const res = await request.post<unknown>(
    '/product/script-version/list-by-product',
    undefined,
    { params: { productId } },
  )
  return normalizeRows<unknown>(res).map(row => normalizeVersion(row, productId))
}

export async function recommendProductScriptVersions(productId: number, _style?: string, topN = 5) {
  const res = await request.post<unknown>(
    '/product/script-version/recommend',
    undefined,
    { params: { productId, topN } },
  )
  return normalizeRecommendResult(res, productId)
}

export async function updateProductScriptVersionStatus(id: number, isActive: boolean) {
  const res = await request.post<unknown>(
    '/product/script-version/update-status',
    undefined,
    { params: { id, isActive } },
  )
  return normalizeVersion(res)
}

export async function saveProductScriptVersion(data: ProductScriptVersionSave) {
  const res = await request.post<unknown>('/product/script-version/save', data)
  return normalizeVersion(res, data.productId)
}

export function ensureProductScriptOptimizationVersion(scriptId: number, forceNew = false) {
  return request.post<EnsureOptimizationVersionResult>('/product/script-version/ensure-optimization-version', {
    scriptId,
    forceNew,
  })
}

export function deleteProductScriptVersion(id: number) {
  return request.post<boolean>(`/product/script-version/delete/${id}`)
}

export function increaseProductScriptVersionUsage(id: number) {
  return request.post<ProductScriptVersion>(
    '/product/script-version/increase-usage',
    undefined,
    { params: { id } },
  )
}

export function updateProductScriptVersionScore(_productScriptId: number, _score: number, _conversionRate?: number): Promise<ProductScriptVersion> {
  return Promise.reject(new Error('后端 update-effectiveness 当前要求完整 ProductScriptVersionSaveVO，请使用 saveProductScriptVersion 提交 productId/content/effectivenessScore。'))
}

export function diffProductScriptVersions(): Promise<{ added: string[]; removed: string[]; changed: string[] }> {
  return Promise.reject(new Error('后端暂无商品话术版本 diff 接口，请在直播话术版本或商品版本详情中人工对比。'))
}

export function rollbackProductScriptVersion(): Promise<number> {
  return Promise.reject(new Error('后端暂无按 productId + versionNumber 回滚商品话术版本的接口，请使用启用/停用版本状态。'))
}
