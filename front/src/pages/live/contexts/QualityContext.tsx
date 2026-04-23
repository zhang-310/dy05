import { createContext, useContext } from 'react'
import type { BuilderState } from '../hooks/useLiveScriptBuilder'

export type QualityContextValue = Pick<
  BuilderState,
  | 'selectedScriptIds' | 'toggleScriptSelect'
  | 'lockedScriptIds' | 'toggleScriptLock'
  | 'batchOpen' | 'setBatchOpen'
  | 'batchMessage' | 'setBatchMessage'
  | 'batchLoading' | 'handleBatchApply'
  | 'batchDeleteLoading' | 'handleBatchDeleteSelected'
  | 'batchDurationLoading' | 'handleBatchSetDuration'
  | 'similarityOpen' | 'setSimilarityOpen'
  | 'similarityList' | 'similarityLoading' | 'handleCheckSimilarityThrottled'
  | 'skeletonOpen' | 'setSkeletonOpen'
  | 'skeletonList' | 'skeletonLoading'
  | 'handleGenerateSkeletonThrottled' | 'handleSkeletonExpand'
  | 'skeletonDurationsLoading' | 'handleSkeletonDurationsConfirm'
  | 'expandedSections' | 'toggleSection'
  | 'handleExpandSections' | 'handleCollapseSections'
  | 'refineOpen' | 'setRefineOpen'
  | 'refineQuestion' | 'setRefineQuestion'
  | 'refineLoading' | 'refineStreaming' | 'refineContent' | 'handleRefine'
>

export const QualityContext = createContext<QualityContextValue | null>(null)

export function useQualityCtx(): QualityContextValue {
  const ctx = useContext(QualityContext)
  if (!ctx) throw new Error('useQualityCtx must be used within QualityProvider')
  return ctx
}
