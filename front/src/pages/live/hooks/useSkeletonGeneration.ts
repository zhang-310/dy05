import { useState, useCallback } from 'react'
import {
  generateSkeletonStream,
  saveLiveScript,
  type LiveScript,
} from '@/api/live'

export interface UseSkeletonGenerationDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  selectedModelId: number | ''
  toast: (msg: string, severity?: 'success' | 'error' | 'info' | 'warning') => void
  handleGenerateFull: () => Promise<void>
}

export function useSkeletonGeneration(deps: UseSkeletonGenerationDeps) {
  const { sessionId, scripts, setScripts, selectedModelId, toast, handleGenerateFull } = deps

  const [skeletonReviewOpen, setSkeletonReviewOpen] = useState(false)
  const [skeletonData, setSkeletonData] = useState<Array<{ scriptId: number; scriptType: string; summary: string; suggestedDurationSec: number }>>([])

  const handleSkeletonFirstGeneration = useCallback(() => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    generateSkeletonStream({ sessionId, modelId: selectedModelId as number })
      .then((data) => {
        const result = data as { list?: Array<{ scriptId: number; scriptType: string; summary: string; suggestedDurationSec: number }> } | undefined
        if (result?.list && result.list.length > 0) {
          setSkeletonData(result.list)
          setSkeletonReviewOpen(true)
        } else {
          toast('骨架生成无结果', 'warning')
        }
      })
      .catch((e: Error) => {
        toast(e?.message ?? '骨架生成失败', 'error')
      })
  }, [sessionId, selectedModelId, toast])

  const handleSkeletonConfirmAndGenerate = useCallback(async (editedSkeleton: Array<{ scriptId: number; summary: string; suggestedDurationSec: number }>) => {
    setSkeletonReviewOpen(false)
    for (const item of editedSkeleton) {
      const existing = scripts.find((s) => s.id === item.scriptId)
      if (existing) {
        await saveLiveScript({ id: item.scriptId, requirement: item.summary, durationLimitSec: item.suggestedDurationSec })
        setScripts((prev) => prev.map((s) => s.id === item.scriptId ? { ...s, requirement: item.summary, durationLimitSec: item.suggestedDurationSec } : s))
      }
    }
    handleGenerateFull()
  }, [scripts, setScripts, handleGenerateFull])

  return {
    skeletonReviewOpen, setSkeletonReviewOpen, skeletonData,
    handleSkeletonFirstGeneration, handleSkeletonConfirmAndGenerate,
  }
}
