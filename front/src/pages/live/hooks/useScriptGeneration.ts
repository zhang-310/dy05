import { useCallback } from 'react'
import { useThrottledCallback } from '@/hooks/useDebouncedCallback'
import { useToast } from '@/contexts/ToastContext'
import { useLiveGenStore } from '@/stores/liveGenStore'
import type { LiveScript, LiveSessionVO, FullGenerateOptions } from '@/api/live'
import type { LiveProduct } from '@/api/live-product'
import { useStreamGeneration, type FlowStep } from './useStreamGeneration'
import { useSlotGeneration } from './useSlotGeneration'
import { useDiffRegeneration } from './useDiffRegeneration'
import { useSkeletonGeneration } from './useSkeletonGeneration'

export type { FlowStep }

export interface UseScriptGenerationDeps {
  sessionId: number | ''
  products: LiveProduct[]
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  loadData: () => Promise<void>
  session: LiveSessionVO | null
  editingId: number | null
  setEditingId: (id: number | null) => void
  setEditingScript: (s: LiveScript | null) => void
  setEditContent: (c: string) => void
  selectedModelId: number | ''
  /** 热点驱动：选中的热搜词，生成时注入 prompt */
  hotKeywords?: string[]
  /** 全场生成附加选项（ipType/scriptModule/materialType 等） */
  fullGenOptions?: FullGenerateOptions
  /** 逐段质检回调：slot 完成时调用 */
  onSlotQualityCheck?: (scriptId: number) => void
  /** 模型性能记录回调 */
  onRecordModelPerf?: (modelId: number, durationMs: number, success: boolean) => void
}

export function useScriptGeneration({
  sessionId,
  scripts,
  setScripts,
  loadData,
  session,
  editingId,
  selectedModelId,
  hotKeywords = [],
  fullGenOptions,
  onSlotQualityCheck,
  onRecordModelPerf,
}: UseScriptGenerationDeps) {
  const toast = useToast()

  // genStyle / useKbRef 统一由 Zustand store 管理
  const genStyle = useLiveGenStore((s) => s.genStyle)
  const setGenStyle = useLiveGenStore((s) => s.setGenStyle)
  const useKbRef = useLiveGenStore((s) => s.useKbRef)
  const setUseKbRef = useLiveGenStore((s) => s.setUseKbRef)

  const syncStyle = useCallback((style: string) => {
    setGenStyle(style)
  }, [setGenStyle])

  // ── 子 Hook 1: 流式全场生成 + 进度 + 持久化 ──
  const stream = useStreamGeneration({
    sessionId, scripts, setScripts, loadData, session, editingId,
    selectedModelId, genStyle, useKbRef, hotKeywords, fullGenOptions, toast,
    onSlotQualityCheck, onRecordModelPerf,
  })

  // ── 子 Hook 2: 单槽位生成 ──
  const slot = useSlotGeneration({
    sessionId, scripts, setScripts, loadData, selectedModelId,
    genStyle, useKbRef, toast,
    setGenLoading: stream.setGenLoading,
    setFlowPanelVisible: stream.setFlowPanelVisible,
    setFlowSteps: stream.setFlowSteps,
    setFullGenProgress: stream.setFullGenProgress,
  })

  // ── 子 Hook 3: Diff 对比重新生成 ──
  const diff = useDiffRegeneration({
    sessionId, scripts, setScripts, loadData, selectedModelId, toast,
    flowSteps: stream.flowSteps,
    setFlowSteps: stream.setFlowSteps,
    setGenLoading: stream.setGenLoading,
  })

  // ── 子 Hook 4: 骨架先行 ──
  const skeleton = useSkeletonGeneration({
    sessionId, scripts, setScripts, selectedModelId, toast,
    handleGenerateFull: stream.handleGenerateFull,
  })

  const handleGenerateOpeningThrottled = useThrottledCallback(slot.handleGenerateOpening, 500)

  return {
    genStyle, setGenStyle,
    useKbRef, setUseKbRef,
    lastRagRefs: slot.lastRagRefs,
    genLoading: stream.genLoading,
    fullGenProgress: stream.fullGenProgress,
    flowSteps: stream.flowSteps,
    setFlowSteps: stream.setFlowSteps,
    genJustCompleted: stream.genJustCompleted,
    sseReconnecting: stream.sseReconnecting,
    flowPanelVisible: stream.flowPanelVisible,
    setFlowPanelVisible: stream.setFlowPanelVisible,
    lastSlotDoneScriptId: stream.lastSlotDoneScriptId,
    cancelFullGeneration: stream.cancelFullGeneration,
    cancellingGen: stream.cancellingGen,
    handleFlowPanelClose: stream.handleFlowPanelClose,
    handleFlowPanelDismiss: stream.handleFlowPanelDismiss,
    productGenOpen: slot.productGenOpen, setProductGenOpen: slot.setProductGenOpen,
    emotionalOpen: slot.emotionalOpen, setEmotionalOpen: slot.setEmotionalOpen,
    emotionalCategory: slot.emotionalCategory, setEmotionalCategory: slot.setEmotionalCategory,
    emotionalSubCategory: slot.emotionalSubCategory, setEmotionalSubCategory: slot.setEmotionalSubCategory,
    emotionalLoading: slot.emotionalLoading,
    handleGenerateOpeningThrottled,
    handleGenerateProduct: slot.handleGenerateProduct,
    handleGenerateFullThrottled: stream.handleGenerateFullThrottled,
    handleGenerateIncremental: stream.handleGenerateIncremental,
    handleGenerateEmotional: slot.handleGenerateEmotional,
    handleGenerateTransitionsOnly: slot.handleGenerateTransitionsOnly,
    handleRetryStep: diff.handleRetryStep,
    handleRegenerateSingle: diff.handleRegenerateSingle,
    generateForProducts: slot.generateForProducts,
    syncStyle,
    // Diff 对比
    diffViewOpen: diff.diffViewOpen, setDiffViewOpen: diff.setDiffViewOpen, diffStepKey: diff.diffStepKey,
    handleAcceptRegeneration: diff.handleAcceptRegeneration, handleRejectRegeneration: diff.handleRejectRegeneration,
    // 断点续生成
    resumeAvailable: stream.resumeAvailable, handleResumeGeneration: stream.handleResumeGeneration, handleDismissResume: stream.handleDismissResume, handleRetryAllFailed: stream.handleRetryAllFailed,
    // 骨架先行
    skeletonReviewOpen: skeleton.skeletonReviewOpen, setSkeletonReviewOpen: skeleton.setSkeletonReviewOpen, skeletonData: skeleton.skeletonData,
    handleSkeletonFirstGeneration: skeleton.handleSkeletonFirstGeneration, handleSkeletonConfirmAndGenerate: skeleton.handleSkeletonConfirmAndGenerate,
  }
}
