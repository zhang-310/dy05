import request from '@/utils/request'

export interface RhythmSlotVO {
  scriptId: number
  sequenceNo: number
  scriptType: string
  label?: string
  durationLimitSec?: number
}

export interface RhythmOptimizeResult {
  sessionId: number
  suggestedOrder: number[]
  slots: Array<{
    scriptId: number
    scriptType: string
    suggestedDurationSec: number
    reason?: string
  }>
  totalDurationSec?: number
  notes?: string[]
}

export interface ProductStrategyResult {
  productId: number
  strategy?: string
  suggestedDurationSec?: number
  suggestedSlotType?: string
  highlights?: string[]
}

export interface BatchOrderResult {
  sessionId: number
  orderedProductIds: number[]
  reason?: string
}

/** 优化直播节奏（AI 推荐排期） */
export function optimizeRhythm(sessionId: number) {
  return request.post<RhythmOptimizeResult>('/live/rhythm/optimize', { sessionId })
}

/** 商品讲解策略推荐 */
export function getProductStrategy(productId: number) {
  return request.post<ProductStrategyResult>('/live/rhythm/product-strategy', { productId })
}

/** 排品顺序推荐 */
export function recommendBatchOrder(sessionId: number) {
  return request.post<BatchOrderResult>('/live/rhythm/batch-order', { sessionId })
}

/**
 * 保存节奏方案（P1-02 可视化节奏编排器）
 * 将前端拖拽调整后的槽位顺序和时长保存到后端
 */
export function saveRhythm(sessionId: number, slots: RhythmSlotVO[]) {
  return request.post<{ sessionId: number; updatedSlots: number; status: string }>(
    '/live/rhythm/save-rhythm',
    { sessionId, slots },
  )
}
