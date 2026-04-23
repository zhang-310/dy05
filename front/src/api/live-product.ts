import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LiveProductSearchVO, LiveProductSaveVO, LiveProductVO } from '@/types/live'

/** 直播产品类型（保留向后兼容） */
export type LiveProduct = LiveProductVO & { [key: string]: unknown }

export function searchProducts(data?: LiveProductSearchVO) {
  return request.post<PageResult<LiveProduct>>('/live/product/search', data || {})
}

export function getProductsBySession(sessionId: number) {
  return request.post<LiveProduct[]>('/live/product/by-session', { sessionId })
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
  return request.post<Record<string, unknown>[]>('/live/data/product', { sessionId })
}

// Re-export types for backward compatibility
export type { LiveProductSearchVO, LiveProductSaveVO, LiveProductVO } from '@/types/live'
