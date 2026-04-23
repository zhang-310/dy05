import { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import {
  rebuildScriptSlots,
  checkGenerateFullInProgress,
} from '@/api/live'
import type { LiveProduct } from '@/api/live-product'
import { type LiveScript } from '@/api/live-script'
import { useToast } from '@/contexts/ToastContext'
import {
  parseProductTypes,
  PRODUCT_TYPE_OPTIONS,
} from '@/pages/live/components/constants'

import { useProductManager } from './useProductManager'
import { useScriptEditor } from './useScriptEditor'
import { useScriptToolbar } from './useScriptToolbar'
import { useLiveGenStore } from '@/stores/liveGenStore'
import { useScriptGeneration } from './useScriptGeneration'
import { useAiChat } from './useAiChat'
import { useQualityCheck } from './useQualityCheck'
import { useSmartSort } from './useSmartSort'
import { useProgressiveQualityCheck } from './useProgressiveQualityCheck'
import { useModelPerformance } from './useModelPerformance'
import { useLiveScriptCore } from './useLiveScriptCore'

// ─── Types ──────────────────────────────────────────────

export interface ScriptSectionData {
  key: string
  title: string
  subtitle?: string
  scripts: LiveScript[]
  product?: LiveProduct
}

export interface SimilarityItem {
  scriptId1: number; scriptId2: number; type1: string; type2: string
  similarityLevel: string; suggestion: string
}

export interface SkeletonItem {
  scriptId: number; scriptType: string; summary: string; suggestedDurationSec: number
}

// ─── Hook ───────────────────────────────────────────────

export function useLiveScriptBuilder(sessionId: number | '') {
  const toast = useToast()

  // ── 使用 useLiveScriptCore 统一管理：数据加载 + 模型选择 + 热搜词 ──────────────
  // 注意：scripts 的本地 setScripts 仍在此 hook 中管理，以支持 useScriptGeneration 的即时更新
  const [scripts, setScripts] = useState<LiveScript[]>([])
  const [focusedScriptId, setFocusedScriptId] = useState<number | null>(null)

  const core = useLiveScriptCore(sessionId, (style) => {
    // 场次风格同步通过 scriptGen.syncStyle，在 scriptGen 初始化后处理
    _pendingStyleRef.current = style
  })
  const _pendingStyleRef = useRef<string | null>(null)

  const {
    session,
    products,
    loading,
    dataReloading,
    aiModels,
    selectedModelId: _selectedModelId,
    hotKeywords,
    setSelectedModelId,
    setHotKeywords,
    setScripts: setCoreScripts,
    setProducts,
    loadData: _coreLoadData,
  } = core

  // selectedModelId 类型收窄为 number | null（兼容旧代码）
  const selectedModelId = (_selectedModelId == null || _selectedModelId === '') ? null : Number(_selectedModelId)
  // selectedModelId 用于传给旧接口（使用 number | '' 类型）
  const selectedModelIdForStore: number | '' = (selectedModelId != null) ? selectedModelId : ''

  // 包装 loadData：加载完成后同步更新本地 scripts state
  const loadData = useCallback(async () => {
    await _coreLoadData()
    // core 内部已更新其自己的 scripts，但我们需要同步给本地 scripts state
    // 直接用 core.scripts 触发器同步
  }, [_coreLoadData])

  // 当 core.scripts 变化时，同步到本地 scripts
  useEffect(() => {
    setScripts(core.scripts)
    setCoreScripts(scripts)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [core.scripts])

  /** 话术构建使用 copy_processing 任务配置的模型列表 */
  const llmModels = useMemo((): import('@/types/ai').AiModelVO[] => aiModels, [aiModels])

  /** 刷新后轮询：若后端正在执行一键生成，前端需感知并展示提示，完成后自动刷新。
   *  SSE 优先：当 SSE 连接存活时暂停轮询，SSE 断开时以 15s 间隔降级轮询。 */
  const [fullGenInProgressRemotely, setFullGenInProgressRemotely] = useState(false)
  const sseConnectedRef = useRef(false)
  /** 由 useScriptGeneration 在 SSE onOpen/onError 时调用 */
  const setSseConnected = useCallback((v: boolean) => { sseConnectedRef.current = v }, [])
  const pollNowRef = useRef<(() => void) | null>(null)
  useEffect(() => {
    if (!sessionId || typeof sessionId !== 'number') return
    let prevInProgress = false
    const poll = async () => {
      // SSE 已连接时跳过轮询
      if (sseConnectedRef.current) return
      try {
        const res = await checkGenerateFullInProgress(sessionId)
        const inProgress = Boolean(res?.inProgress ?? false)
        setFullGenInProgressRemotely(inProgress)
        if (prevInProgress && !inProgress) {
          await loadData()
        }
        prevInProgress = inProgress
      } catch {
        setFullGenInProgressRemotely(false)
      }
    }
    pollNowRef.current = poll
    poll()
    const iv = setInterval(poll, 15000)
    return () => { clearInterval(iv); pollNowRef.current = null }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- loadData 稳定，仅 sessionId 变化时需重启轮询
  }, [sessionId])

  // ════════════════════════════════════════════════════════
  // Computed values
  // ════════════════════════════════════════════════════════

  const addedProductIds = useMemo(() => new Set(products.map((p) => p.productId).filter(Boolean)), [products])
  const sortedProducts = useMemo(
    () => [...products].sort((a, b) => ((a.position as number) ?? 0) - ((b.position as number) ?? 0)),
    [products]
  )
  const sortedScripts = useMemo(
    () => [...scripts].sort((a, b) => ((a.sequenceNo as number) ?? 0) - ((b.sequenceNo as number) ?? 0)),
    [scripts]
  )

  /** 当前选中模型的显示名，供生成面板展示 */
  const selectedModelName = useMemo(() => {
    if (!selectedModelId || typeof selectedModelId !== 'number') return undefined
    const m = llmModels.find((a) => a.id === selectedModelId)
    return m ? String(m.modelName ?? m.modelVersion ?? m.modelProvider ?? m.id ?? '') : undefined
  }, [selectedModelId, llmModels])

  /** 按流程分组：开场、各产品块(产品话术+转场)、结尾 — O(n+m) 预索引 */
  const scriptSections = useMemo(() => {
    // Pre-index scripts by type and productId for O(1) lookups
    const byType = new Map<string, typeof sortedScripts>()
    const productMap = new Map<number, typeof sortedScripts>()
    const transitions: typeof sortedScripts = []

    for (const s of sortedScripts) {
      const t = String(s.scriptType)
      if (!byType.has(t)) byType.set(t, [])
      byType.get(t)!.push(s)
      if (t === 'product' && s.productId != null) {
        const pid = s.productId as number
        if (!productMap.has(pid)) productMap.set(pid, [])
        productMap.get(pid)!.push(s)
      }
      if (t === 'transition') transitions.push(s)
    }

    const sections: ScriptSectionData[] = []
    const opening = byType.get('opening')
    if (opening?.length) {
      sections.push({ key: 'opening', title: '开场话术', scripts: opening })
    }

    // Pre-compute product sequence ranges
    const productSeqRanges = sortedProducts.map((row) => {
      const pScripts = productMap.get(row.productId as number) ?? []
      const seqs = pScripts.map((s) => (s.sequenceNo as number) ?? 0)
      return { minSeq: seqs.length ? Math.min(...seqs) : 0, maxSeq: seqs.length ? Math.max(...seqs) : 0 }
    })

    sortedProducts.forEach((row, idx) => {
      const pid = row.productId as number
      const productScripts = productMap.get(pid) ?? []
      const nextRange = productSeqRanges[idx + 1]
      const maxSeq = productSeqRanges[idx].maxSeq
      const transScripts = nextRange
        ? transitions.filter((s) => {
            const seq = Number(s.sequenceNo ?? 0)
            return seq > maxSeq && seq < nextRange.minSeq
          })
        : []
      const block = [...productScripts, ...transScripts].sort((a, b) => ((a.sequenceNo as number) ?? 0) - ((b.sequenceNo as number) ?? 0))
      if (block.length > 0) {
        sections.push({
          key: `product-${idx}`,
          title: `${idx + 1}. ${String(row.productName ?? '-')}`,
          subtitle: parseProductTypes(row.productType)
            .map((t) => PRODUCT_TYPE_OPTIONS.find((o) => o.value === t)?.label)
            .filter(Boolean)
            .join(' · '),
          scripts: block,
          product: row,
        })
      }
    })
    const emotional = byType.get('emotional')
    if (emotional?.length) {
      sections.push({ key: 'emotional', title: '情绪话术', scripts: emotional })
    }
    const closing = byType.get('closing')
    if (closing?.length) {
      sections.push({ key: 'closing', title: '结尾话术', scripts: closing })
    }
    const knownTypes = new Set(['opening', 'product', 'transition', 'closing', 'emotional'])
    const other = sortedScripts.filter((s) => !knownTypes.has(String(s.scriptType)))
    if (other.length > 0) {
      sections.push({ key: 'other', title: '其他', scripts: other })
    }
    return sections
  }, [sortedScripts, sortedProducts])

  const totalEstSeconds = sortedScripts.reduce(
    (sum, s) => sum + ((s.estimatedDurationSeconds as number) ?? Math.ceil(((s.scriptContent as string)?.length ?? 0) / 3)),
    0
  )

  /** 产品 ID → 该产品话术条数（用于左栏展示） */
  const productScriptCounts = useMemo(() => {
    const m = new Map<number, number>()
    scriptSections.forEach((sec) => {
      if (sec.key.startsWith('product-')) {
        const idx = parseInt(sec.key.replace('product-', ''), 10)
        const pid = sortedProducts[idx]?.productId as number | undefined
        if (pid != null) m.set(pid, sec.scripts.length)
      }
    })
    sortedProducts.forEach((p) => {
      const pid = p.productId as number
      if (pid != null && !m.has(pid)) m.set(pid, 0)
    })
    return m
  }, [scriptSections, sortedProducts])

  // ── focusedScript 派生 ──
  const focusedScript = useMemo(
    () => scripts.find((s) => s.id === focusedScriptId) ?? null,
    [scripts, focusedScriptId],
  )

  // ════════════════════════════════════════════════════════
  // Sub-hooks (order matters for cross-dependencies)
  // ════════════════════════════════════════════════════════

  // Script Editor (defined first; cross-hook callbacks wired via refs below)
  const scriptEditor = useScriptEditor({
    sessionId,
    scripts,
    setScripts,
    scriptSections,
    loadData,
    session: session as Record<string, unknown> | null,
  })

  // AI Chat (receives live editingId/editingScript from scriptEditor)
  const aiChat = useAiChat({
    editingId: scriptEditor.editingId,
    editingScript: scriptEditor.editingScript,
    focusedScriptId,
    focusedScript,
    scripts,
    setScripts,
    setEditContent: scriptEditor.setEditContent,
    session,
    loadData,
    selectedModelId: selectedModelIdForStore,
  })

  // editingId → focusedScriptId 同步（编辑时聚焦同一条，取消后保持）
  useEffect(() => {
    if (scriptEditor.editingId != null) {
      setFocusedScriptId(scriptEditor.editingId)
    }
  }, [scriptEditor.editingId])

  // Wire cross-hook callbacks via refs (avoids circular initialization)
  scriptEditor.onEditStartRef.current = aiChat.onEditStart
  scriptEditor.onEditCancelRef.current = aiChat.onEditCancel

  // Esc 关闭分析师面板（编辑时由 useScriptEditor 处理 Escape）
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && aiChat.analystScript && scriptEditor.editingId == null) {
        aiChat.setAnalystScript(null)
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [aiChat.analystScript, aiChat.setAnalystScript, scriptEditor.editingId])

  // Quality Check
  const qualityCheck = useQualityCheck({
    sessionId,
    scripts,
    setScripts,
    scriptSections,
    loadData,
    session: session as Record<string, unknown> | null,
    setEditingId: scriptEditor.setEditingId,
    setEditingScript: scriptEditor.setEditingScript,
    setEditContent: scriptEditor.setEditContent,
    setEditDurationLimit: scriptEditor.setEditDurationLimit_internal,
    setEditRequirement: scriptEditor.setEditRequirement_internal,
    prepareChatForSkeleton: aiChat.prepareChatForSkeleton,
    selectedModelId: selectedModelIdForStore,
  })

  // Progressive Quality Check (per-slot, fires as each slot completes)
  const progressiveQuality = useProgressiveQualityCheck({ scripts })

  // Model Performance tracking (must be before scriptGen for callback wiring)
  const modelPerf = useModelPerformance()

  // 高级生成选项 from Zustand store
  const ipType = useLiveGenStore((s) => s.ipType)
  const setIpType = useLiveGenStore((s) => s.setIpType)
  const materialType = useLiveGenStore((s) => s.materialType)
  const setMaterialType = useLiveGenStore((s) => s.setMaterialType)
  const scriptModule = useLiveGenStore((s) => s.scriptModule)
  const setScriptModule = useLiveGenStore((s) => s.setScriptModule)
  const retentionStrategy = useLiveGenStore((s) => s.retentionStrategy)
  const setRetentionStrategy = useLiveGenStore((s) => s.setRetentionStrategy)
  const interactionLevel = useLiveGenStore((s) => s.interactionLevel)
  const setInteractionLevel = useLiveGenStore((s) => s.setInteractionLevel)

  const fullGenOptions = useMemo(() => {
    const opts: Record<string, string> = {}
    if (ipType) opts.ipType = ipType
    if (materialType) opts.materialType = materialType
    if (scriptModule) opts.scriptModule = scriptModule
    if (retentionStrategy) opts.retentionStrategy = retentionStrategy
    if (interactionLevel) opts.interactionLevel = interactionLevel
    return Object.keys(opts).length > 0 ? opts : undefined
  }, [ipType, materialType, scriptModule, retentionStrategy, interactionLevel])

  // Script Generation
  const scriptGen = useScriptGeneration({
    sessionId,
    products,
    scripts,
    setScripts,
    loadData,
    session,
    editingId: scriptEditor.editingId,
    setEditingId: scriptEditor.setEditingId,
    setEditingScript: scriptEditor.setEditingScript,
    setEditContent: scriptEditor.setEditContent,
    selectedModelId: selectedModelIdForStore,
    hotKeywords,
    fullGenOptions,
    onSlotQualityCheck: progressiveQuality.enqueueQualityCheck,
    onRecordModelPerf: modelPerf.recordCompletion,
  })

  // 本地 SSE 生成完成后立即清除横幅并触发一次轮询确认
  const prevGenLoadingRef = useRef(false)
  useEffect(() => {
    if (prevGenLoadingRef.current && !scriptGen.genLoading) {
      setFullGenInProgressRemotely(false)
      // 延迟触发一次轮询以同步后端状态
      setTimeout(() => pollNowRef.current?.(), 2000)
    }
    prevGenLoadingRef.current = scriptGen.genLoading
  }, [scriptGen.genLoading])

  // 场次风格同步：core.loadData 完成后通过 pendingStyle 触发 scriptGen.syncStyle
  useEffect(() => {
    const pending = _pendingStyleRef.current
    if (pending != null && scriptGen.syncStyle) {
      scriptGen.syncStyle(pending)
      _pendingStyleRef.current = null
    }
  }, [core.session, scriptGen.syncStyle])

  // Wire progressive quality results back to flowSteps
  useEffect(() => {
    if (progressiveQuality.qualityResults.size === 0) return
    scriptGen.setFlowSteps((prev) => {
      let changed = false
      const next = prev.map((step) => {
        if (!step.scriptId) return step
        const qr = progressiveQuality.qualityResults.get(step.scriptId)
        if (!qr || (step.qualityScore === qr.status && JSON.stringify(step.qualityIssues) === JSON.stringify(qr.issues))) return step
        changed = true
        return { ...step, qualityScore: qr.status, qualityIssues: qr.issues }
      })
      return changed ? next : prev
    })
  }, [progressiveQuality.qualityResults, scriptGen])

  // 后台生成中 + flowSteps 为空 → 基于 scripts 的 generationStatus 构建步骤，让用户看到实时进度
  useEffect(() => {
    if (!fullGenInProgressRemotely || scriptGen.genLoading) return
    if ((scriptGen.flowSteps?.length ?? 0) > 0) return
    if (sortedScripts.length === 0) return
    const steps = sortedScripts.map((s) => {
      const gs = s.generationStatus
      const hasContent = s.scriptContent && String(s.scriptContent).trim().length > 0
      const label = s.scriptType === 'opening' ? '开场' : s.scriptType === 'closing' ? '结尾' : s.scriptType === 'transition' ? '衔接' : '产品'
      let status: 'done' | 'failed' | 'loading' | 'pending' = 'loading'
      if (gs === 'success' || hasContent) status = 'done'
      else if (gs === 'failed') status = 'failed'
      else if (gs === 'pending' || gs === 'generating') status = 'loading'
      return {
        label,
        status: status as 'done' | 'failed' | 'loading' | 'pending',
        stepKey: `remote-${s.id}`,
        scriptId: s.id,
        ...(status === 'done' ? { content: String(s.scriptContent).slice(0, 100), endTime: Date.now() } : {}),
        ...(status === 'failed' ? { errorMsg: '生成失败', endTime: Date.now() } : {}),
        ...(status === 'loading' ? { startTime: Date.now() } : {}),
      }
    })
    scriptGen.setFlowSteps(steps)
    scriptGen.setFlowPanelVisible(true)
  }, [fullGenInProgressRemotely, scriptGen.genLoading, sortedScripts, scriptGen])


  const productManager = useProductManager({
    sessionId,
    products,
    setProducts,
    scripts,
    setScripts,
    sortedProducts,
    loadData,
    onAfterProductSort: async () => {
      if (typeof sessionId === 'number') {
        await rebuildScriptSlots(sessionId)
        await loadData()
      }
    },
  })

  // Smart Sort (AI 排品)
  const smartSort = useSmartSort({
    sessionId: typeof sessionId === 'number' ? sessionId : 0,
    products: sortedProducts,
    onApplied: loadData,
  })

  // Toolbar（清空、重建、排序、场次克隆、产品-话术联动）
  const toolbar = useScriptToolbar({
    sessionId,
    scripts,
    setScripts,
    products,
    loadData,
    toast,
    productManager,
    qualityCheck,
  })

  // Wire handleDeleteScript to also clean selectedScriptIds
  const handleDeleteScript = useCallback(async () => {
    const id = scriptEditor.deleteConfirm?.id
    await scriptEditor.handleDeleteScript()
    if (id) {
      qualityCheck.removeFromSelected(id)
    }
  }, [scriptEditor, qualityCheck])

  // ════════════════════════════════════════════════════════
  // Return (flat, backward-compatible API) — memoized to reduce re-renders
  // ════════════════════════════════════════════════════════

  return useMemo(() => ({
    // Core data
    session, sessionId, products, scripts, loading, dataReloading,
    sortedProducts, sortedScripts, scriptSections,
    addedProductIds, totalEstSeconds, productScriptCounts,

    // Product dialog
    addProductOpen: productManager.addProductOpen, setAddProductOpen: productManager.setAddProductOpen,
    batchProductOpen: productManager.batchProductOpen, setBatchProductOpen: productManager.setBatchProductOpen,
    productList: productManager.productList, productSearch: productManager.productSearch, setProductSearch: productManager.setProductSearch,
    productListTotal: productManager.productListTotal, productListHasMore: productManager.productListHasMore, loadMoreProducts: productManager.loadMoreProducts,
    selectedProductToAdd: productManager.selectedProductToAdd, setSelectedProductToAdd: productManager.setSelectedProductToAdd,
    addProductTypeLoading: productManager.addProductTypeLoading, addProductTypeSelected: productManager.addProductTypeSelected, setAddProductTypeSelected: productManager.setAddProductTypeSelected,
    sorting: productManager.sorting,

    // Product handlers
    handleSelectProductToAdd: productManager.handleSelectProductToAdd, handleConfirmAddProduct: productManager.handleConfirmAddProduct,
    handleBatchAdd: productManager.handleBatchAdd, handleRemoveProduct: productManager.handleRemoveProduct, handleBatchRemoveProducts: productManager.handleBatchRemoveProducts,
    handleMoveProduct: productManager.handleMoveProduct, handleSubmitSort: productManager.handleSubmitSort, handleUpdateProductType: productManager.handleUpdateProductType,
    handleProductClick: toolbar.handleProductClick,

    // Generation
    genStyle: scriptGen.genStyle, setGenStyle: scriptGen.setGenStyle,
    useKbRef: scriptGen.useKbRef, setUseKbRef: scriptGen.setUseKbRef,
    ipType, setIpType, materialType, setMaterialType,
    scriptModule, setScriptModule, retentionStrategy, setRetentionStrategy, interactionLevel, setInteractionLevel,
    hotKeywords, setHotKeywords,
    lastRagRefs: scriptGen.lastRagRefs,
    genLoading: scriptGen.genLoading, fullGenProgress: scriptGen.fullGenProgress, flowSteps: scriptGen.flowSteps, genJustCompleted: scriptGen.genJustCompleted, sseReconnecting: scriptGen.sseReconnecting,
    flowPanelVisible: scriptGen.flowPanelVisible, setFlowPanelVisible: scriptGen.setFlowPanelVisible,
    cancelFullGeneration: scriptGen.cancelFullGeneration,
    cancellingGen: scriptGen.cancellingGen,
    handleFlowPanelClose: scriptGen.handleFlowPanelClose,
    handleFlowPanelDismiss: scriptGen.handleFlowPanelDismiss,
    lastSlotDoneScriptId: scriptGen.lastSlotDoneScriptId,
    productGenOpen: scriptGen.productGenOpen, setProductGenOpen: scriptGen.setProductGenOpen,
    handleGenerateOpeningThrottled: scriptGen.handleGenerateOpeningThrottled,
    handleGenerateProduct: scriptGen.handleGenerateProduct,
    handleGenerateFullThrottled: scriptGen.handleGenerateFullThrottled,
    handleGenerateIncremental: scriptGen.handleGenerateIncremental,
    handleGenerateEmotional: scriptGen.handleGenerateEmotional,
    handleGenerateTransitionsOnly: scriptGen.handleGenerateTransitionsOnly,
    generateForProducts: scriptGen.generateForProducts,
    handleRetryStep: scriptGen.handleRetryStep,
    handleRegenerateSingle: scriptGen.handleRegenerateSingle,

    // Diff 对比
    diffViewOpen: scriptGen.diffViewOpen, setDiffViewOpen: scriptGen.setDiffViewOpen, diffStepKey: scriptGen.diffStepKey,
    handleAcceptRegeneration: scriptGen.handleAcceptRegeneration, handleRejectRegeneration: scriptGen.handleRejectRegeneration,

    // Progressive Quality
    progressiveQualitySummary: progressiveQuality.aggregateSummary,

    // 断点续生成
    resumeAvailable: scriptGen.resumeAvailable,
    handleResumeGeneration: scriptGen.handleResumeGeneration,
    handleDismissResume: scriptGen.handleDismissResume,
    handleRetryAllFailed: scriptGen.handleRetryAllFailed,

    // 骨架先行
    skeletonReviewOpen: scriptGen.skeletonReviewOpen, setSkeletonReviewOpen: scriptGen.setSkeletonReviewOpen,
    skeletonFirstData: scriptGen.skeletonData,
    handleSkeletonFirstGeneration: scriptGen.handleSkeletonFirstGeneration,
    handleSkeletonConfirmAndGenerate: scriptGen.handleSkeletonConfirmAndGenerate,

    // Emotional dialog
    emotionalOpen: scriptGen.emotionalOpen, setEmotionalOpen: scriptGen.setEmotionalOpen,
    emotionalCategory: scriptGen.emotionalCategory, setEmotionalCategory: scriptGen.setEmotionalCategory,
    emotionalSubCategory: scriptGen.emotionalSubCategory, setEmotionalSubCategory: scriptGen.setEmotionalSubCategory,
    emotionalLoading: scriptGen.emotionalLoading,

    // Editing
    editingId: scriptEditor.editingId, editingScript: scriptEditor.editingScript, editContent: scriptEditor.editContent, setEditContent: scriptEditor.setEditContent,
    focusedScriptId, setFocusedScriptId, focusedScript,
    editDurationLimit: scriptEditor.editDurationLimit, setEditDurationLimit: scriptEditor.setEditDurationLimit,
    editRequirement: scriptEditor.editRequirement, setEditRequirement: scriptEditor.setEditRequirement,
    editPresenterNotes: scriptEditor.editPresenterNotes, setEditPresenterNotes: scriptEditor.setEditPresenterNotes,
    violationResult: scriptEditor.violationResult, checkingId: scriptEditor.checkingId,
    handleStartEdit: scriptEditor.handleStartEdit, handleCancelEdit: scriptEditor.handleCancelEdit, handleSaveEdit: scriptEditor.handleSaveEdit,
    handleDeleteScript,
    handleMarkExecuted: scriptEditor.handleMarkExecuted,
    deleteConfirm: scriptEditor.deleteConfirm, setDeleteConfirm: scriptEditor.setDeleteConfirm,
    handleEditContentChange: scriptEditor.handleEditContentChange,
    handleUndo: scriptEditor.handleUndo, handleRedo: scriptEditor.handleRedo,
    canUndo: scriptEditor.canUndo, canRedo: scriptEditor.canRedo,

    // Violation / Refine
    handleCheckViolation: scriptEditor.handleCheckViolation,
    refineOpen: qualityCheck.refineOpen, setRefineOpen: qualityCheck.setRefineOpen,
    refineQuestion: qualityCheck.refineQuestion, setRefineQuestion: qualityCheck.setRefineQuestion,
    refineLoading: qualityCheck.refineLoading, refineStreaming: qualityCheck.refineStreaming, refineContent: qualityCheck.refineContent,
    handleRefine: qualityCheck.handleRefine,
    refineSegmentOpen: qualityCheck.refineSegmentOpen, setRefineSegmentOpen: qualityCheck.setRefineSegmentOpen,
    refineSegmentInstruction: qualityCheck.refineSegmentInstruction, setRefineSegmentInstruction: qualityCheck.setRefineSegmentInstruction,
    refineSegmentLoading: qualityCheck.refineSegmentLoading, handleRefineSegment: qualityCheck.handleRefineSegment,

    // Analyst
    analystScript: aiChat.analystScript, setAnalystScript: aiChat.setAnalystScript,
    analystContent: aiChat.analystContent, setAnalystContent: aiChat.setAnalystContent,
    analystDuration: aiChat.analystDuration, setAnalystDuration: aiChat.setAnalystDuration,
    analystRequirement: aiChat.analystRequirement, setAnalystRequirement: aiChat.setAnalystRequirement,
    analystLoading: aiChat.analystLoading,
    analystStreaming: aiChat.analystStreaming,
    handleOpenAnalyst: aiChat.handleOpenAnalyst, handleAnalystAutoFill: aiChat.handleAnalystAutoFill, handleAnalystApply: aiChat.handleAnalystApply,

    // AI Chat
    chatMessage: aiChat.chatMessage, setChatMessage: aiChat.setChatMessage,
    chatResponse: aiChat.chatResponse, setChatResponse: aiChat.setChatResponse,
    chatLoading: aiChat.chatLoading, chatStreaming: aiChat.chatStreaming, chatHistory: aiChat.chatHistory,
    chatPersona: aiChat.chatPersona, setChatPersona: aiChat.setChatPersona,
    chatScene: aiChat.chatScene, setChatScene: aiChat.setChatScene,
    chatDurationSec: aiChat.chatDurationSec, setChatDurationSec: aiChat.setChatDurationSec,
    chatDimensions: aiChat.chatDimensions, setChatDimensions: aiChat.setChatDimensions,
    chatStyle: aiChat.chatStyle, setChatStyle: aiChat.setChatStyle,
    copySaveLoading: aiChat.copySaveLoading,
    handleChatSend: aiChat.handleChatSend, handleChatApply: aiChat.handleChatApply,
    handleSaveToCopy: aiChat.handleSaveToCopy,
    handleChatHistoryClear: aiChat.handleChatHistoryClear,

    // Batch
    selectedScriptIds: qualityCheck.selectedScriptIds, toggleScriptSelect: qualityCheck.toggleScriptSelect,
    lockedScriptIds: qualityCheck.lockedScriptIds, toggleScriptLock: qualityCheck.toggleScriptLock,
    batchOpen: qualityCheck.batchOpen, setBatchOpen: qualityCheck.setBatchOpen,
    batchMessage: qualityCheck.batchMessage, setBatchMessage: qualityCheck.setBatchMessage,
    batchLoading: qualityCheck.batchLoading, handleBatchApply: qualityCheck.handleBatchApply,
    batchDeleteLoading: qualityCheck.batchDeleteLoading, handleBatchDeleteSelected: qualityCheck.handleBatchDeleteSelected,
    batchDurationLoading: qualityCheck.batchDurationLoading, handleBatchSetDuration: qualityCheck.handleBatchSetDuration,

    // Quality
    similarityOpen: qualityCheck.similarityOpen, setSimilarityOpen: qualityCheck.setSimilarityOpen,
    similarityList: qualityCheck.similarityList, similarityLoading: qualityCheck.similarityLoading,
    handleCheckSimilarityThrottled: qualityCheck.handleCheckSimilarityThrottled,
    skeletonOpen: qualityCheck.skeletonOpen, setSkeletonOpen: qualityCheck.setSkeletonOpen,
    skeletonList: qualityCheck.skeletonList, skeletonLoading: qualityCheck.skeletonLoading,
    handleGenerateSkeletonThrottled: qualityCheck.handleGenerateSkeletonThrottled,
    handleSkeletonExpand: qualityCheck.handleSkeletonExpand,
    skeletonDurationsLoading: qualityCheck.skeletonDurationsLoading,
    handleSkeletonDurationsConfirm: qualityCheck.handleSkeletonDurationsConfirm,

    // Sections
    expandedSections: qualityCheck.expandedSections, toggleSection: qualityCheck.toggleSection, handleExpandSections: qualityCheck.handleExpandSections, handleCollapseSections: qualityCheck.handleCollapseSections,

    // Chain prompt
    chainPrompt: scriptEditor.chainPrompt, setChainPrompt: scriptEditor.setChainPrompt,
    handleChainPromptAdjust: scriptEditor.handleChainPromptAdjust,

    // 协同编辑冲突 Dialog
    conflictState: scriptEditor.conflictState,
    handleConflictOverwrite: scriptEditor.handleConflictOverwrite,
    handleConflictDiscard: scriptEditor.handleConflictDiscard,
    handleConflictClose: scriptEditor.handleConflictClose,

    // Toolbar
    saveLibLoading: scriptEditor.saveLibLoading, exportLoading: scriptEditor.exportLoading,
    handleSaveToLibrary: scriptEditor.handleSaveToLibrary, handleExport: scriptEditor.handleExport,
    clearAllLoading: toolbar.clearAllLoading, handleClearAllScripts: toolbar.handleClearAllScripts, clearAllConfirmOpen: toolbar.clearAllConfirmOpen, setClearAllConfirmOpen: toolbar.setClearAllConfirmOpen, doClearAllScripts: toolbar.doClearAllScripts,
    rebuildSlotsLoading: toolbar.rebuildSlotsLoading, handleRebuildSlots: toolbar.handleRebuildSlots, rebuildConfirmOpen: toolbar.rebuildConfirmOpen, setRebuildConfirmOpen: toolbar.setRebuildConfirmOpen, doRebuildSlots: toolbar.doRebuildSlots,

    // Reload
    loadData,

    // Model Performance
    modelPerf,

    // Session Clone
    sessionCloneOpen: toolbar.sessionCloneOpen, setSessionCloneOpen: toolbar.setSessionCloneOpen,
    handleApplySessionClone: toolbar.handleApplySessionClone,

    /** 刷新后若后端正在一键生成，则为 true；展示提示并在完成后自动刷新 */
    fullGenInProgressRemotely,
    /** SSE 连接状态通知（由 useScriptGeneration 调用） */
    setSseConnected,

    // Model selection (AI 写作必选)
    llmModels,
    selectedModelId,
    setSelectedModelId,
    selectedModelName,

    // 产品-话术滚动联动
    scrollToSectionKey: toolbar.scrollToSectionKey,
    clearScrollToSectionKey: toolbar.clearScrollToSectionKey,
    highlightedProductIdx: toolbar.highlightedProductIdx,
    setHighlightedProductIdx: toolbar.setHighlightedProductIdx,
    highlightedProductId: toolbar.highlightedProductId,

    // AI 排品
    smartSort,

    // Reorder
    handleReorderScripts: toolbar.handleReorderScripts, reorderLoading: toolbar.reorderLoading,
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }), [
    session, sessionId, products, scripts, loading, dataReloading,
    sortedProducts, sortedScripts, scriptSections,
    addedProductIds, totalEstSeconds, productScriptCounts,
    productManager, scriptGen, scriptEditor, aiChat, qualityCheck, smartSort, progressiveQuality, modelPerf, toolbar,
    focusedScriptId, focusedScript,
    handleDeleteScript,
    hotKeywords, setHotKeywords,
    loadData, fullGenInProgressRemotely, setSseConnected,
    llmModels, selectedModelId, setSelectedModelId, selectedModelName,
  ])
}

/** 话术构建器的完整返回类型，供 Stage View 组件使用 */
export type BuilderState = ReturnType<typeof useLiveScriptBuilder>
