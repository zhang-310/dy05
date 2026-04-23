/**
 * 话术工具栏操作 hook（P2-1 从 useLiveScriptBuilder 拆分）
 * 清空、重建槽位、拖拽排序、场次克隆、产品-话术联动
 */

import { useState, useCallback } from 'react'
import {
  clearAllScripts,
  rebuildScriptSlots,
  batchSortScripts,
  type LiveScript,
} from '@/api/live'
import type { LiveProduct } from '@/api/live-product'

export interface UseScriptToolbarDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: (v: LiveScript[] | ((prev: LiveScript[]) => LiveScript[])) => void
  products: LiveProduct[]
  loadData: () => Promise<void>
  toast: (msg: string, type: 'success' | 'error') => void
  productManager: { handleProductClick: (productId: number) => string | undefined }
  qualityCheck: { expandSection: (key: string) => void }
}

export interface UseScriptToolbarResult {
  // 清空全部
  clearAllLoading: boolean
  handleClearAllScripts: () => void
  clearAllConfirmOpen: boolean
  setClearAllConfirmOpen: (v: boolean) => void
  doClearAllScripts: () => Promise<void>
  // 重建槽位
  rebuildSlotsLoading: boolean
  handleRebuildSlots: () => void
  rebuildConfirmOpen: boolean
  setRebuildConfirmOpen: (v: boolean) => void
  doRebuildSlots: () => Promise<void>
  // 拖拽排序
  handleReorderScripts: (activeId: number, overId: number) => Promise<void>
  reorderLoading: boolean
  // 场次克隆
  sessionCloneOpen: boolean
  setSessionCloneOpen: (v: boolean) => void
  handleApplySessionClone: (
    clonedScripts: Array<{ scriptType?: string; scriptContent?: string; durationLimitSec?: number; requirement?: string }>,
    mode: 'structure' | 'content'
  ) => Promise<void>
  // 产品-话术联动
  scrollToSectionKey: string | null
  clearScrollToSectionKey: () => void
  highlightedProductIdx: number | null
  setHighlightedProductIdx: (v: number | null) => void
  highlightedProductId: number | null
  handleProductClick: (productId: number) => void
}

export function useScriptToolbar(deps: UseScriptToolbarDeps): UseScriptToolbarResult {
  const {
    sessionId,
    scripts,
    setScripts,
    products,
    loadData,
    toast,
    productManager,
    qualityCheck,
  } = deps

  const [scrollToSectionKey, setScrollToSectionKeyState] = useState<string | null>(null)
  const clearScrollToSectionKey = useCallback(() => setScrollToSectionKeyState(null), [])
  const [highlightedProductIdx, setHighlightedProductIdx] = useState<number | null>(null)
  const [highlightedProductId, setHighlightedProductId] = useState<number | null>(null)

  const handleProductClick = useCallback(
    (productId: number) => {
      setHighlightedProductId(productId)
      const sectionKey = productManager.handleProductClick(productId)
      if (sectionKey) {
        qualityCheck.expandSection(sectionKey)
        setScrollToSectionKeyState(sectionKey)
      }
    },
    [productManager, qualityCheck]
  )

  const [sessionCloneOpen, setSessionCloneOpen] = useState(false)
  const handleApplySessionClone = useCallback(
    async (
      clonedScripts: Array<{
        scriptType?: string
        scriptContent?: string
        durationLimitSec?: number
        requirement?: string
      }>,
      mode: 'structure' | 'content'
    ) => {
      if (!sessionId || typeof sessionId !== 'number') return
      const { batchSaveLiveScripts } = await import('@/api/live-script')
      const mapped = clonedScripts.map((s, i) => ({
        sessionId,
        scriptType: s.scriptType ?? 'product',
        sequenceNo: i + 1,
        ...(mode === 'content' && { scriptContent: s.scriptContent ?? '' }),
        ...(s.durationLimitSec && { durationLimitSec: s.durationLimitSec }),
        ...(s.requirement && { requirement: s.requirement }),
      }))
      try {
        await batchSaveLiveScripts(sessionId, mapped)
        toast('场次克隆成功', 'success')
        await loadData()
      } catch (e) {
        toast(e instanceof Error ? e.message : '克隆失败', 'error')
      }
      setSessionCloneOpen(false)
    },
    [sessionId, loadData, toast]
  )

  const [clearAllLoading, setClearAllLoading] = useState(false)
  const [clearAllConfirmOpen, setClearAllConfirmOpen] = useState(false)
  const handleClearAllScripts = useCallback(() => {
    if (!sessionId || typeof sessionId !== 'number' || scripts.length === 0) return
    setClearAllConfirmOpen(true)
  }, [sessionId, scripts.length])
  const doClearAllScripts = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    setClearAllLoading(true)
    try {
      await clearAllScripts(sessionId)
      await loadData()
      toast('已清空全部话术', 'success')
      setClearAllConfirmOpen(false)
    } catch (e) {
      toast(e instanceof Error ? e.message : '清空失败', 'error')
    } finally {
      setClearAllLoading(false)
    }
  }, [sessionId, loadData, toast])

  const [rebuildSlotsLoading, setRebuildSlotsLoading] = useState(false)
  const [rebuildConfirmOpen, setRebuildConfirmOpen] = useState(false)
  const handleRebuildSlots = useCallback(() => {
    if (!sessionId || typeof sessionId !== 'number' || products.length === 0) return
    setRebuildConfirmOpen(true)
  }, [sessionId, products.length])
  const doRebuildSlots = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    setRebuildSlotsLoading(true)
    try {
      await rebuildScriptSlots(sessionId)
      await loadData()
      toast('槽位已重建，话术已按产品顺序重置', 'success')
      setRebuildConfirmOpen(false)
    } catch (e) {
      toast(e instanceof Error ? e.message : '重建失败', 'error')
    } finally {
      setRebuildSlotsLoading(false)
    }
  }, [sessionId, loadData, toast])

  const [reorderLoading, setReorderLoading] = useState(false)
  const handleReorderScripts = useCallback(
    async (activeId: number, overId: number) => {
      if (!sessionId || activeId === overId) return
      const oldIndex = scripts.findIndex((s) => s.id === activeId)
      const newIndex = scripts.findIndex((s) => s.id === overId)
      if (oldIndex === -1 || newIndex === -1) return

      const reordered = [...scripts]
      const [moved] = reordered.splice(oldIndex, 1)
      reordered.splice(newIndex, 0, moved)
      const updated = reordered.map((s, i) => ({ ...s, sequenceNo: i + 1 }))
      setScripts(updated as LiveScript[])

      setReorderLoading(true)
      try {
        await batchSortScripts({ sessionId: sessionId as number, ids: updated.map((s) => s.id as number) })
      } catch (e) {
        toast(e instanceof Error ? e.message : '排序失败', 'error')
        await loadData()
      } finally {
        setReorderLoading(false)
      }
    },
    [sessionId, scripts, setScripts, loadData, toast]
  )

  return {
    clearAllLoading,
    handleClearAllScripts,
    clearAllConfirmOpen,
    setClearAllConfirmOpen,
    doClearAllScripts,
    rebuildSlotsLoading,
    handleRebuildSlots,
    rebuildConfirmOpen,
    setRebuildConfirmOpen,
    doRebuildSlots,
    handleReorderScripts,
    reorderLoading,
    sessionCloneOpen,
    setSessionCloneOpen,
    handleApplySessionClone,
    scrollToSectionKey,
    clearScrollToSectionKey,
    highlightedProductIdx,
    setHighlightedProductIdx,
    highlightedProductId,
    handleProductClick,
  }
}
