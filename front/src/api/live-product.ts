import request from '@/utils/request'
import type { LiveProductSearchVO, LiveProductSaveVO, LiveProductVO } from '@/types/live'
import { isRecord, normalizeArray, normalizePage } from '@/utils/response-normalize'

/** 直播产品类型（保留向后兼容） */
export type LiveProduct = LiveProductVO & { [key: string]: unknown }

function numberOrUndefined(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

function normalizeLiveProductRow(raw: unknown): LiveProduct {
  const row = isRecord(raw) ? raw : {}
  return {
    ...(row as unknown as LiveProduct),
    id: Number(row.id ?? 0),
    sessionId: Number(row.sessionId ?? 0),
    productId: Number(row.productId ?? row.id ?? 0),
    productName: row.productName == null ? undefined : String(row.productName),
    saleQuantity: numberOrUndefined(row.saleQuantity ?? row.sales ?? row.quantity),
    revenue: numberOrUndefined(row.revenue ?? row.totalRevenue ?? row.gmv),
    position: numberOrUndefined(row.position ?? row.sortOrder),
    productType: row.productType == null ? undefined : String(row.productType),
    productScriptId: numberOrUndefined(row.productScriptId ?? row.scriptId),
    price: numberOrUndefined(row.price),
    imageUrl: row.imageUrl == null ? undefined : String(row.imageUrl),
    productCategory: row.productCategory == null ? undefined : String(row.productCategory),
    description: row.description == null ? undefined : String(row.description),
    createTime: String(row.createTime ?? ''),
  }
}

function normalizeLiveProductArray(raw: unknown): LiveProduct[] {
  return normalizeArray<unknown>(raw).map(normalizeLiveProductRow)
}

export function searchProducts(data?: LiveProductSearchVO) {
  const params = data || {}
  return request.post<unknown>('/live/product/search', params)
    .then(raw => normalizePage<unknown, LiveProduct>(raw, normalizeLiveProductRow, params.page ?? 0, params.rows ?? 20))
}

export function getProductsBySession(sessionId: number) {
  return request.post<unknown>('/live/product/by-session', { sessionId }).then(normalizeLiveProductArray)
}

export function saveLiveProduct(data: LiveProductSaveVO) {
  return request.post<number>('/live/product/save', data)
}

export function deleteLiveProduct(id: number) {
  return request.post<void>('/live/product/delete', { id })
}

/** 批量排序产品（按 productIds 顺序更新 position） */
export function batchSortProducts(sessionId: number, productIds: number[]) {
  return request.post<void>('/live/product/batch-sort', { sessionId, productIds })
}

/** 批量添加产品到场次 */
export function batchAddProducts(
  sessionId: number,
  items: Array<{ productId: number; productName: string; productType: string; productScriptId?: number }>
) {
  return request.post<number>('/live/product/batch-add', { sessionId, items })
}

/** 场次商品数据列表（同步后的销售数据） */
export function getProductDataBySession(sessionId: number) {
  return request.post<unknown>('/live/data/product', { sessionId }).then(raw => normalizeArray<Record<string, unknown>>(raw))
}

// Re-export types for backward compatibility
export type { LiveProductSearchVO, LiveProductSaveVO, LiveProductVO } from '@/types/live'
