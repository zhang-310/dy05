import { useState, useCallback } from 'react'
import { useToast } from '@/contexts/ToastContext'
import { liveApi } from '@/api/live'
import type { LiveProduct } from '@/api/live-product'

interface SortSuggestion {
  productIds: number[]
  reason: string
  isFallback?: boolean
}

export interface SmartSortState {
  suggestion: SortSuggestion | null
  loading: boolean
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

  const requestSmartSort = useCallback(async () => {
    if (!sessionId || products.length < 2) {
      toast('至少需要 2 个商品才能使用 AI 排序', 'warning')
      return
    }
    setLoading(true)
    try {
      const result = await liveApi.productAiSortSuggest({
        sessionId,
        productIds: products.map(p => p.productId),
      })
      const ids = (result.productIds ?? result.sortedIds ?? []) as number[]
      const reason = String(result.reason ?? result.explanation ?? 'AI 推荐排序')
      if (ids.length > 0) {
        setSuggestion({ productIds: ids, reason })
      } else {
        toast('AI 未返回排序建议', 'info')
      }
    } catch {
      // 本地 fallback：爆品→控单→利润→亏品→平价
      const typeOrder: Record<string, number> = { hot: 1, control: 2, profit: 3, loss: 4, flat: 5 }
      const sorted = [...products].sort((a, b) => {
        const typesA = (a.productType ?? '').split(',').filter(Boolean)
        const typesB = (b.productType ?? '').split(',').filter(Boolean)
        const ta = Math.min(...typesA.map(t => typeOrder[t.trim()] ?? 99), 99)
        const tb = Math.min(...typesB.map(t => typeOrder[t.trim()] ?? 99), 99)
        return ta - tb
      })
      const ids = sorted.map(p => p.productId).filter(Boolean)
      if (ids.length > 0) {
        setSuggestion({
          productIds: ids,
          reason: '基于商品类型推荐：爆品→控单→利润→亏品→平价（AI 不可用，使用本地策略）',
          isFallback: true,
        })
      }
    } finally {
      setLoading(false)
    }
  }, [sessionId, products, toast])

  const applySuggestion = useCallback(async () => {
    if (!suggestion || !sessionId) return
    try {
      await liveApi.productBatchSort(sessionId, suggestion.productIds)
      onApplied()
      toast('已应用 AI 推荐排序', 'success')
      setSuggestion(null)
    } catch (e) {
      toast(e instanceof Error ? e.message : '应用排序失败', 'error')
    }
  }, [suggestion, sessionId, onApplied, toast])

  const clearSuggestion = useCallback(() => setSuggestion(null), [])

  return { suggestion, loading, requestSmartSort, applySuggestion, clearSuggestion }
}
