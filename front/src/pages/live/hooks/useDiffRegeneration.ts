import { useState, useCallback, useRef } from 'react'
import {
  generateForSlot,
  generateForSlotStream,
  saveLiveScript,
  type LiveScript,
} from '@/api/live'
import type { FlowStep } from './useStreamGeneration'

export interface UseDiffRegenerationDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  loadData: () => Promise<void>
  selectedModelId: number | ''
  toast: (msg: string, severity?: 'success' | 'error' | 'info' | 'warning') => void
  flowSteps: FlowStep[]
  setFlowSteps: React.Dispatch<React.SetStateAction<FlowStep[]>>
  setGenLoading: (v: boolean) => void
}

export function useDiffRegeneration(deps: UseDiffRegenerationDeps) {
  const {
    sessionId, scripts, setScripts, loadData, selectedModelId,
    toast, flowSteps, setFlowSteps, setGenLoading,
  } = deps

  const [diffViewOpen, setDiffViewOpen] = useState(false)
  const [diffStepKey, setDiffStepKey] = useState<string | null>(null)
  const retryAbortRef = useRef<{ abort: () => void } | null>(null)
  const handleRegenerateSingle = useCallback(async (scriptId: number) => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setGenLoading(true)
    try {
      const content = String(await generateForSlot({ scriptId, modelId: selectedModelId }))
      if (content) {
        await saveLiveScript({ id: scriptId, scriptContent: content })
        setScripts((prev) => prev.map((s) => s.id === scriptId ? { ...s, scriptContent: content } : s))
      }
      toast('重新生成成功', 'success')
      await loadData()
    } catch (e) {
      toast(e instanceof Error ? e.message : '重新生成失败', 'error')
    } finally {
      setGenLoading(false)
    }
  }, [sessionId, selectedModelId, setScripts, loadData, toast, setGenLoading])
  const handleRetryStep = useCallback(
    async (step: { scriptId?: number; label?: string; stepKey?: string }) => {
      if (!step.scriptId || !selectedModelId) return
      const scriptId = step.scriptId
      const stepKey = step.stepKey ?? String(scriptId)
      const script = scripts.find((s) => s.id === scriptId)
      const previousContent = script?.scriptContent

      setFlowSteps((prev) => {
        const next = [...prev]
        const idx = next.findIndex((s) => s.stepKey === stepKey)
        if (idx >= 0) next[idx] = { ...next[idx], status: 'loading', content: '' }
        return next
      })

      retryAbortRef.current?.abort()
      let accumulated = ''
      // generateForSlotStream returns a Promise, not an abort handle
      void generateForSlotStream({ scriptId, modelId: selectedModelId })
      // Store a dummy abort handle since we can't actually abort the stream
      retryAbortRef.current = { abort: () => { /* no-op */ } }

      // poll result via generateForSlot since generateForSlotStream returns Promise
      try {
        const content = String(await generateForSlot({ scriptId, modelId: selectedModelId }))
        accumulated = content ?? ''
        await saveLiveScript({ id: scriptId, scriptContent: accumulated })
        setScripts((prev) => prev.map((s) => s.id === scriptId ? { ...s, scriptContent: accumulated } : s))
        setFlowSteps((prev) => {
          const next = [...prev]
          const idx = next.findIndex((s) => s.stepKey === stepKey)
          if (idx >= 0) next[idx] = { ...next[idx], status: 'done', content: accumulated, previousContent }
          return next
        })
        setDiffStepKey(stepKey)
        setDiffViewOpen(true)
      } catch (e) {
        toast(e instanceof Error ? e.message : '重新生成失败', 'error')
        setFlowSteps((prev) => {
          const next = [...prev]
          const idx = next.findIndex((s) => s.stepKey === stepKey)
          if (idx >= 0) next[idx] = { ...next[idx], status: 'failed' as const }
          return next
        })
      }
    },
    [selectedModelId, scripts, setScripts, loadData, toast, setFlowSteps]
  )
  const handleAcceptRegeneration = useCallback(async () => {
    if (!diffStepKey) return
    const step = flowSteps.find((s) => s.stepKey === diffStepKey)
    if (!step?.scriptId || !step.content) return
    await saveLiveScript({ id: step.scriptId, scriptContent: step.content })
    setScripts((prev) => prev.map((x) => (x.id === step.scriptId ? { ...x, scriptContent: step.content! } : x)))
    setDiffViewOpen(false)
    setDiffStepKey(null)
    toast('已应用新版本', 'success')
    loadData()
  }, [diffStepKey, flowSteps, setScripts, loadData, toast])

  const handleRejectRegeneration = useCallback(() => {
    if (!diffStepKey) return
    setFlowSteps((prev) => {
      const next = [...prev]
      const idx = next.findIndex((s) => s.stepKey === diffStepKey)
      if (idx >= 0 && next[idx].previousContent) {
        next[idx] = { ...next[idx], content: next[idx].previousContent, previousContent: undefined }
      }
      return next
    })
    setDiffViewOpen(false)
    setDiffStepKey(null)
    toast('已保留原版本', 'info')
  }, [diffStepKey, toast, setFlowSteps])

  return {
    diffViewOpen, setDiffViewOpen, diffStepKey,
    handleRegenerateSingle,
    handleRetryStep,
    handleAcceptRegeneration,
    handleRejectRegeneration,
  }
}



