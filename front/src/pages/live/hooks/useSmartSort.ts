import { useState, useCallback } from 'react'
import { useToast } from '@/contexts/ToastContext'
import { liveApi } from '@/api/live'
import type { LiveProduct } from '@/api/live-product'
import { sortLiveProducts } from '../utils/order'

interface SortSuggestion {
  productIds: number[]
  liveProductIds: number[]
  reason: string
  isFallback?: boolean
}

export interface SmartSortState {
  suggestion: SortSuggestion | null
  loading: boolean
  lastError: string | null
  requestSmartSort: () => Promise<void>
  applySuggestion: () => Promise<void>
  clearSuggestion: () => void
}

interface UseSmartSortDeps {
  sessionId: number
  products: LiveProduct[]
  onApplied: () => void
}

export function useSmartSort({ sessionId, products, onApplied }: UseSmartSortDeps): SmartSortState {
  const toast = useToast()
  const [suggestion, setSuggestion] = useState<SortSuggestion | null>(null)
  const [loading, setLoading] = useState(false)
  const [lastError, setLastError] = useState<string | null>(null)

  const requestSmartSort = useCallback(async () => {
    if (!sessionId || products.length < 2) {
      toast('至少需要 2 个商品才能使用 AI 排序', 'warning')
      return
    }
    setLoading(true)
    setLastError(null)
    try {
      const result = await liveApi.productAiSortSuggest({
        sessionId,
        productIds: products.map(p => p.productId),
      })
      const orderedProducts = sortLiveProducts(products)
      const byLiveId = new Map(orderedProducts.map(p => [Number(p.id), p]))
      const byProductId = new Map(orderedProducts.map(p => [Number(p.productId), p]))
      const rawIds = (result.productIds ?? result.sortedIds ?? []) as number[]
      const picked = rawIds
        .map(Number)
        .map(id => byLiveId.get(id) ?? byProductId.get(id))
        .filter((p): p is LiveProduct => Boolean(p))
      const seen = new Set<number>()
      const normalized = picked.filter(p => {
        const id = Number(p.id)
        if (!id || seen.has(id)) return false
        seen.add(id)
        return true
      })
      const remaining = orderedProducts.filter(p => !seen.has(Number(p.id)))
      const all = [...normalized, ...remaining]
      const liveProductIds = all.map(p => Number(p.id)).filter(Boolean)
      const productIds = all.map(p => Number(p.productId)).filter(Boolean)
      const reason = String(result.reason ?? result.explanation ?? 'AI 推荐排序')
      if (liveProductIds.length > 0) {
        setSuggestion({ productIds, liveProductIds, reason })
      } else {
        toast('AI 未返回排序建议', 'info')
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : 'AI 排品服务不可用'
      setSuggestion(null)
      setLastError(`/live/ai/sort-suggest AI 排品失败：${message}`)
      toast(`/live/ai/sort-suggest AI 排品失败：${message}`, 'error')
    } finally {
      setLoading(false)
    }
  }, [sessionId, products, toast])

  const applySuggestion = useCallback(async () => {
    if (!suggestion || !sessionId) return
    try {
      await liveApi.productBatchSort(sessionId, suggestion.liveProductIds)
      onApplied()
      toast('已应用 AI 推荐排序', 'success')
      setSuggestion(null)
      setLastError(null)
    } catch (e) {
      const message = e instanceof Error ? e.message : '应用排序失败'
      setLastError(`/live/product/batch-sort 应用 AI 排序失败：${message}`)
      toast(`/live/product/batch-sort 应用 AI 排序失败：${message}`, 'error')
    }
  }, [suggestion, sessionId, onApplied, toast])

  const clearSuggestion = useCallback(() => {
    setSuggestion(null)
    setLastError(null)
  }, [])

  return { suggestion, loading, lastError, requestSmartSort, applySuggestion, clearSuggestion }
}
