import { useState, useCallback } from 'react'
import {
  generateOpening,
  generateProduct,
  generateEmotional,
  generateForSlot,
  saveLiveScript,
  type LiveRagRef,
  type LiveScript,
} from '@/api/live'
import type { FlowStep } from './useStreamGeneration'

export interface UseSlotGenerationDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  loadData: () => Promise<void>
  selectedModelId: number | ''
  genStyle: string
  useKbRef: boolean
  toast: (msg: string, severity?: 'success' | 'error' | 'info' | 'warning') => void
  setGenLoading: (v: boolean) => void
  setFlowPanelVisible: (v: boolean) => void
  setFlowSteps: React.Dispatch<React.SetStateAction<FlowStep[]>>
  setFullGenProgress: (v: { current: number; total: number; slotType: string } | null) => void
}

export function useSlotGeneration(deps: UseSlotGenerationDeps) {
  const {
    sessionId, scripts, setScripts, loadData, selectedModelId,
    genStyle, useKbRef, toast,
    setGenLoading, setFlowPanelVisible, setFlowSteps, setFullGenProgress,
  } = deps

  const [lastRagRefs, setLastRagRefs] = useState<LiveRagRef[] | null>(null)
  const [productGenOpen, setProductGenOpen] = useState(false)
  const [emotionalOpen, setEmotionalOpen] = useState(false)
  const [emotionalCategory, setEmotionalCategory] = useState('female_perspective')
  const [emotionalSubCategory, setEmotionalSubCategory] = useState('')
  const [emotionalLoading, setEmotionalLoading] = useState(false)

  const handleGenerateOpening = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setGenLoading(true)
    setLastRagRefs(null)
    setFlowPanelVisible(true)
    setFlowSteps([{ label: '开场', status: 'loading', stepKey: 'single-opening', startTime: Date.now() }])
    try {
      const res = await generateOpening({ sessionId, genStyle: genStyle || undefined, useKbRef, modelId: selectedModelId as number }) as Record<string, unknown>
      if (Array.isArray(res?.ragRefs) && res.ragRefs.length) setLastRagRefs(res.ragRefs as LiveRagRef[])
      else setLastRagRefs(null)
      setFlowSteps([{ label: '开场', status: 'done', content: res?.content as string | undefined, stepKey: 'single-opening', endTime: Date.now() }])
      toast('开场话术生成成功', 'success')
      loadData()
    } catch (e) {
      setFlowSteps([{ label: '开场', status: 'failed', errorMsg: e instanceof Error ? e.message : '生成失败', stepKey: 'single-opening' }])
      toast(e instanceof Error ? e.message : '生成失败', 'error')
    } finally {
      setGenLoading(false)
    }
  }, [sessionId, genStyle, useKbRef, selectedModelId, loadData, toast, setGenLoading, setFlowPanelVisible, setFlowSteps])

  const handleGenerateProduct = useCallback(async (productId: number) => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setGenLoading(true)
    setLastRagRefs(null)
    setFlowPanelVisible(true)
    setFlowSteps([{ label: '产品话术', status: 'loading', stepKey: 'single-product', startTime: Date.now() }])
    try {
      const res = await generateProduct({ sessionId, productId, genStyle: genStyle || undefined, useKbRef, modelId: selectedModelId as number }) as Record<string, unknown>
      const ragRefs = Array.isArray(res?.ragRefs) ? res.ragRefs as import('@/api/live').LiveRagRef[] : null
      if (ragRefs?.length) setLastRagRefs(ragRefs)
      else setLastRagRefs(null)
      setFlowSteps([{ label: '产品话术', status: 'done', content: res?.content as string | undefined, stepKey: 'single-product', endTime: Date.now() }])
      toast('产品话术生成成功', 'success')
      setProductGenOpen(false)
      loadData()
    } catch (e) {
      setFlowSteps([{ label: '产品话术', status: 'failed', errorMsg: e instanceof Error ? e.message : '生成失败', stepKey: 'single-product' }])
      toast(e instanceof Error ? e.message : '生成失败', 'error')
    } finally {
      setGenLoading(false)
    }
  }, [sessionId, genStyle, useKbRef, selectedModelId, loadData, toast, setGenLoading, setFlowPanelVisible, setFlowSteps])

  const handleGenerateEmotional = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setEmotionalLoading(true)
    setFlowPanelVisible(true)
    setFlowSteps([{ label: '情绪话术', status: 'loading', stepKey: 'single-emotional', startTime: Date.now() }])
    try {
      const res = await generateEmotional({ sessionId, category: emotionalCategory, subCategory: emotionalSubCategory || undefined, modelId: selectedModelId as number }) as Record<string, unknown>
      const content = String(res?.content ?? '')
      if (!content) {
        setFlowSteps([{ label: '情绪话术', status: 'failed', errorMsg: '生成内容为空', stepKey: 'single-emotional' }])
        toast('生成内容为空', 'error')
        return
      }
      const maxSeq = scripts.reduce((m, s) => Math.max(m, (s.sequenceNo as number) ?? 0), 0)
      const savedVO = await saveLiveScript({
        sessionId,
        scriptContent: content,
        scriptType: 'emotional',
        sequenceNo: maxSeq + 1,
      })
      if (savedVO && typeof savedVO === 'object' && 'id' in savedVO) {
        setScripts((prev) => [...prev, savedVO as LiveScript])
      } else {
        await loadData()
      }
      setFlowSteps([{ label: '情绪话术', status: 'done', content, stepKey: 'single-emotional' }])
      toast('情绪话术已插入', 'success')
      setEmotionalOpen(false)
    } catch (e) {
      setFlowSteps([{ label: '情绪话术', status: 'failed', errorMsg: e instanceof Error ? e.message : '生成失败', stepKey: 'single-emotional' }])
      toast(e instanceof Error ? e.message : '生成失败', 'error')
    } finally {
      setEmotionalLoading(false)
    }
  }, [sessionId, emotionalCategory, emotionalSubCategory, selectedModelId, scripts, setScripts, loadData, toast, setFlowPanelVisible, setFlowSteps])

  /** 仅生成衔接 */
  const handleGenerateTransitionsOnly = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    const sorted = [...scripts].sort((a, b) => ((a.sequenceNo as number) ?? 0) - ((b.sequenceNo as number) ?? 0))
    const transitions = sorted.filter((s) => s.scriptType === 'transition')
    if (transitions.length === 0) {
      toast('当前无衔接话术槽位', 'info')
      return
    }
    setGenLoading(true)
    setFullGenProgress({ current: 0, total: transitions.length, slotType: '衔接' })
    setFlowPanelVisible(true)
    setFlowSteps(transitions.map((s, i) => ({ label: `衔接 ${i + 1}`, status: 'pending' as const, stepKey: `t-${s.id}` })))
    let done = 0
    for (let i = 0; i < transitions.length; i++) {
      const s = transitions[i]
      const scriptId = s.id as number
      setFlowSteps((prev) => {
        const next = [...prev]
        const idx = next.findIndex((x) => x.stepKey === `t-${scriptId}`)
        if (idx >= 0) next[idx] = { ...next[idx], status: 'loading' as const, startTime: Date.now() }
        return next
      })
      try {
        const content = await generateForSlot({ scriptId, modelId: selectedModelId as number }) as string
        if (content) {
          await saveLiveScript({ id: scriptId, scriptContent: content })
          setScripts((prev) => prev.map((x) => (x.id === scriptId ? { ...x, scriptContent: content } : x)))
        }
        setFlowSteps((prev) => {
          const next = [...prev]
          const idx = next.findIndex((x) => x.stepKey === `t-${scriptId}`)
          if (idx >= 0) next[idx] = { ...next[idx], status: 'done' as const, content: String(content).slice(0, 100), endTime: Date.now() }
          return next
        })
      } catch (e) {
        setFlowSteps((prev) => {
          const next = [...prev]
          const idx = next.findIndex((x) => x.stepKey === `t-${scriptId}`)
          if (idx >= 0) next[idx] = { ...next[idx], status: 'failed' as const, errorMsg: e instanceof Error ? e.message : '生成失败', endTime: Date.now() }
          return next
        })
      }
      done++
      setFullGenProgress({ current: done, total: transitions.length, slotType: `衔接 ${done}/${transitions.length}` })
    }
    setGenLoading(false)
    setFullGenProgress(null)
    toast(`衔接话术已生成 ${done}/${transitions.length} 段`, 'success')
    loadData()
  }, [sessionId, scripts, selectedModelId, setScripts, loadData, toast, setGenLoading, setFullGenProgress, setFlowPanelVisible, setFlowSteps])

  /** 为指定产品 ID 列表的空槽位生成话术 */
  const generateForProducts = useCallback(async (productIds: number[]) => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    const emptyScripts = scripts.filter(
      (s) => s.scriptType === 'product' && s.productId && productIds.includes(s.productId) &&
        (!s.scriptContent || String(s.scriptContent).trim().length === 0 || String(s.scriptContent).trim() === '[待填写]')
    )
    if (emptyScripts.length === 0) {
      toast('所选产品的话术槽位均已有内容', 'info')
      return
    }
    setGenLoading(true)
    let done = 0
    for (const script of emptyScripts) {
      try {
        const content = await generateForSlot({ scriptId: script.id, modelId: selectedModelId as number })
        if (content) {
          await saveLiveScript({ id: script.id, sessionId, scriptContent: content })
          setScripts((prev) => prev.map((s) => s.id === script.id ? { ...s, scriptContent: content } : s))
        }
        done++
      } catch { /* continue */ }
    }
    setGenLoading(false)
    toast(`指定产品生成完成：${done}/${emptyScripts.length}`, done === emptyScripts.length ? 'success' : 'warning')
    await loadData()
  }, [sessionId, selectedModelId, scripts, setScripts, loadData, toast, setGenLoading])

  return {
    lastRagRefs,
    productGenOpen, setProductGenOpen,
    emotionalOpen, setEmotionalOpen,
    emotionalCategory, setEmotionalCategory,
    emotionalSubCategory, setEmotionalSubCategory,
    emotionalLoading,
    handleGenerateOpening,
    handleGenerateProduct,
    handleGenerateEmotional,
    handleGenerateTransitionsOnly,
    generateForProducts,
  }
}
