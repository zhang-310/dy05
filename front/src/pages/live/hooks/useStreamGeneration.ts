import { useState, useCallback, useRef, useEffect, useMemo } from 'react'
import { useThrottledCallback } from '@/hooks/useDebouncedCallback'
import {
  generateFullStream,
  saveLiveScript,
  generateForSlot,
  type LiveRagRef,
  type LiveScript,
  type LiveSessionVO,
  type FullGenerateOptions,
} from '@/api/live'

export type FlowStep = {
  label: string
  status: 'pending' | 'loading' | 'done' | 'failed'
  content?: string
  scriptId?: number
  errorMsg?: string
  stepKey?: string
  startTime?: number
  endTime?: number
  ragRefs?: LiveRagRef[]
  streamingContent?: string
  previousContent?: string
  qualityScore?: 'checking' | 'pass' | 'warning' | 'fail'
  qualityIssues?: string[]
}

export interface UseStreamGenerationDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  loadData: () => Promise<void>
  session: LiveSessionVO | null
  editingId: number | null
  selectedModelId: number | ''
  genStyle: string
  useKbRef: boolean
  hotKeywords: string[]
  fullGenOptions?: FullGenerateOptions
  toast: (msg: string, severity?: 'success' | 'error' | 'info' | 'warning') => void
  onSlotQualityCheck?: (scriptId: number) => void
  onRecordModelPerf?: (modelId: number, durationMs: number, success: boolean) => void
}

export function useStreamGeneration(deps: UseStreamGenerationDeps) {
  const {
    sessionId, scripts, setScripts, loadData, session, editingId,
    selectedModelId, genStyle, useKbRef, hotKeywords, fullGenOptions, toast,
    onSlotQualityCheck, onRecordModelPerf,
  } = deps

  const [genLoading, setGenLoading] = useState(false)
  const [genJustCompleted, setGenJustCompleted] = useState(false)
  const [sseReconnecting, setSseReconnecting] = useState(false)
  void setSseReconnecting // reserved for future SSE reconnect logic
  const [fullGenProgress, setFullGenProgress] = useState<{ current: number; total: number; slotType: string } | null>(null)
  const [flowSteps, setFlowSteps] = useState<FlowStep[]>([])
  const [flowPanelVisible, setFlowPanelVisible] = useState(false)
  const [lastSlotDoneScriptId, setLastSlotDoneScriptId] = useState<number | null>(null)
  const [cancellingGen, setCancellingGen] = useState(false)
  const [resumeAvailable, setResumeAvailable] = useState(false)

  const fullGenAbortRef = useRef<AbortController | null>(null)
  const progressEvtRef = useRef<{ current: number; total: number; slotType: string } | null>(null)
  const progressTimerRef = useRef<number | null>(null)
  const genMetaRef = useRef<{ startTime: number; slotCount: number } | null>(null)

  const CHECKPOINT_KEY = `gen-checkpoint-${sessionId}`
  const FLOW_STEPS_KEY = useMemo(() => `gen-flow-steps-${sessionId}`, [sessionId])
  const flowStepsRef = useRef(flowSteps)
  flowStepsRef.current = flowSteps

  // ── flowSteps 持久化到 sessionStorage ──
  useEffect(() => {
    if (!sessionId || flowSteps.length === 0) return
    if (genLoading) return
    const timer = setTimeout(() => {
      try {
        const lite = flowSteps.map(({ streamingContent, previousContent, ragRefs, ...rest }) => rest)
        sessionStorage.setItem(FLOW_STEPS_KEY, JSON.stringify({ steps: lite, ts: Date.now() }))
      } catch { /* ignore quota */ }
    }, 800)
    return () => clearTimeout(timer)
  }, [flowSteps, sessionId, FLOW_STEPS_KEY, genLoading])

  // 刷新/关闭页面时立即同步写入
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (!sessionId || flowStepsRef.current.length === 0) return
      try {
        const lite = flowStepsRef.current.map(({ streamingContent, previousContent, ragRefs, ...rest }) => rest)
        sessionStorage.setItem(FLOW_STEPS_KEY, JSON.stringify({ steps: lite, ts: Date.now() }))
      } catch { /* ignore */ }
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [sessionId, FLOW_STEPS_KEY])

  // 恢复：初始化时读取
  const restoredRef = useRef(false)
  useEffect(() => {
    if (!sessionId || flowSteps.length > 0 || restoredRef.current) return
    try {
      const raw = sessionStorage.getItem(FLOW_STEPS_KEY)
      if (!raw) return
      const data = JSON.parse(raw)
      if (Date.now() - data.ts > 30 * 60 * 1000) {
        sessionStorage.removeItem(FLOW_STEPS_KEY)
        return
      }
      if (!Array.isArray(data.steps) || data.steps.length === 0) return
      const hasMeaningfulSteps = data.steps.some((s: FlowStep) =>
        s.scriptId || (s.status === 'done' && s.content),
      )
      if (!hasMeaningfulSteps) {
        sessionStorage.removeItem(FLOW_STEPS_KEY)
        return
      }
      restoredRef.current = true
      const restored = (data.steps as FlowStep[]).map((step) => {
        if (step.status === 'loading') {
          return { ...step, status: 'pending' as const, streamingContent: undefined }
        }
        return step
      })
      setFlowSteps(restored)
      setFlowPanelVisible(true)
    } catch { /* ignore */ }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId])

  // scripts 加载/更新后 → 补标 done/failed
  useEffect(() => {
    if (flowStepsRef.current.length === 0 || scripts.length === 0) return
    const statusMap = new Map<number, { content: string; gs: string }>()
    for (const s of scripts) {
      const content = s.scriptContent != null ? String(s.scriptContent).trim() : ''
      const statusRecord = s as Record<string, unknown>
      const gs = statusRecord.generationStatus != null ? String(statusRecord.generationStatus) : ''
      if (content.length > 0 || gs === 'failed') statusMap.set(s.id as number, { content, gs })
    }
    if (statusMap.size === 0) return
    setFlowSteps((prev) => {
      let changed = false
      const next = prev.map((step) => {
        if (!step.scriptId) return step
        if (step.status === 'done' || step.status === 'failed') return step
        if (step.status === 'loading' && genLoading) return step
        const info = statusMap.get(step.scriptId)
        if (!info) return step
        if (info.gs === 'failed' && !info.content) {
          changed = true
          return { ...step, status: 'failed' as const, errorMsg: '生成失败', endTime: Date.now() }
        }
        if (info.content) {
          changed = true
          return { ...step, status: 'done' as const, content: info.content.slice(0, 100), endTime: Date.now() }
        }
        return step
      })
      return changed ? next : prev
    })
  }, [scripts, genLoading])

  // 断点续生成检测
  useEffect(() => {
    if (!sessionId || typeof sessionId !== 'number') return
    try {
      const raw = sessionStorage.getItem(CHECKPOINT_KEY)
      if (raw) {
        const cp = JSON.parse(raw)
        if (cp.timestamp && Date.now() - cp.timestamp < 30 * 60 * 1000 && Array.isArray(cp.completedSlotIds) && cp.totalSlots > cp.completedSlotIds.length) {
          setResumeAvailable(true)
        } else {
          sessionStorage.removeItem(CHECKPOINT_KEY)
        }
      }
    } catch { /* ignore */ }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId])

  const applyProgressToFlowSteps = useCallback((evt: { current: number; total: number; slotType: string }) => (prev: FlowStep[]) => {
    const next = [...prev]
    const lbl = evt.slotType || '...'
    if (evt.current === 0 && lbl) {
      if (next.length === 0) {
        if (evt.total > 1) {
          for (let i = 0; i < evt.total; i++) {
            next.push({ label: i === 0 ? lbl : `步骤 ${i + 1}`, status: i === 0 ? 'loading' as const : 'pending' as const, stepKey: `skeleton-${i}`, ...(i === 0 ? { startTime: Date.now() } : {}) })
          }
        } else {
          next.push({ label: lbl, status: 'loading' as const, stepKey: 'l-init-0', startTime: Date.now() })
        }
      } else if (evt.total > 1 && next.length < evt.total) {
        while (next.length < evt.total) {
          next.push({ label: `步骤 ${next.length + 1}`, status: 'pending' as const, stepKey: `skeleton-${next.length}` })
        }
        next[0] = { ...next[0], label: lbl, status: 'loading' as const, startTime: next[0].startTime ?? Date.now() }
      } else {
        next[0] = { ...next[0], label: lbl, status: 'loading' as const, startTime: next[0].startTime ?? Date.now() }
      }
    } else {
      const isStarting = lbl.startsWith('生成') && lbl.endsWith('中')
      if (isStarting) {
        if (evt.total > 0 && next.length < evt.total) {
          while (next.length < evt.total) {
            next.push({ label: `步骤 ${next.length + 1}`, status: 'pending' as const, stepKey: `skeleton-${next.length}` })
          }
        }
        while (next.length <= evt.current) {
          next.push({ label: lbl, status: 'pending' as const, stepKey: `l-${lbl}-${next.length}` })
        }
        next[evt.current] = { ...next[evt.current], label: lbl, status: 'loading' as const, startTime: next[evt.current]?.startTime ?? Date.now() }
      } else {
        if (evt.current > 0 && next[evt.current - 1]) {
          next[evt.current - 1] = { ...next[evt.current - 1], label: lbl }
        }
      }
    }
    return next
  }, [])

  const flushProgress = useCallback(() => {
    const evt = progressEvtRef.current
    if (evt) {
      setFullGenProgress({ current: evt.current, total: evt.total, slotType: evt.slotType })
      setFlowSteps(applyProgressToFlowSteps(evt))
    }
    progressTimerRef.current = null
  }, [applyProgressToFlowSteps])

  useEffect(() => () => {
    if (progressTimerRef.current) cancelAnimationFrame(progressTimerRef.current)
    fullGenAbortRef.current?.abort()
    fullGenAbortRef.current = null
  }, [])

  const handleGenerateFull = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    if (editingId != null) return
    setGenLoading(true)
    setFullGenProgress({ current: 0, total: 1, slotType: '准备中' })
    setFlowSteps([])
    setFlowPanelVisible(true)
    setLastSlotDoneScriptId(null)
    fullGenAbortRef.current = null
    const styleToUse = genStyle || (session?.scriptStyle as string) || undefined
    genMetaRef.current = { startTime: Date.now(), slotCount: scripts.length }
    // dy02 uses async/await - generateFullStream is a simple POST, no streaming callbacks
    generateFullStream({
      sessionId,
      genStyle: styleToUse,
      useKbRef,
      modelId: selectedModelId as number,
      hotKeywords,
      ...fullGenOptions,
    }).then(() => {
      setGenJustCompleted(true)
      setTimeout(() => setGenJustCompleted(false), 300)
      toast('整场话术生成成功', 'success')
      loadData()
      setFlowSteps((prev) => prev.map((s) => (s.status === 'loading' ? { ...s, status: 'done' as const } : s)))
      setFullGenProgress(null)
      setGenLoading(false)
      fullGenAbortRef.current = null
      try { sessionStorage.removeItem(CHECKPOINT_KEY) } catch { /* ignore */ }
      if (onRecordModelPerf && selectedModelId && typeof selectedModelId === 'number' && genMetaRef.current) {
        const elapsed = Date.now() - genMetaRef.current.startTime
        const slotCount = genMetaRef.current.slotCount || 1
        onRecordModelPerf(selectedModelId, elapsed / slotCount, true)
      }
      genMetaRef.current = null
    }).catch((e: Error) => {
      toast(e?.message ?? '生成失败', 'error')
      setFlowSteps((prev) => prev.map((s) => {
        if (s.status === 'loading') return { ...s, status: 'failed' as const, errorMsg: e?.message ?? '生成失败', endTime: Date.now() }
        if (s.status === 'pending') return { ...s, status: 'failed' as const, errorMsg: '生成中断', endTime: Date.now() }
        return s
      }))
      setFullGenProgress(null)
      setGenLoading(false)
      fullGenAbortRef.current = null
      setResumeAvailable(true)
    })

  }, [sessionId, genStyle, session?.scriptStyle, useKbRef, selectedModelId, hotKeywords, fullGenOptions, loadData, toast, editingId, setScripts, flushProgress, scripts.length, onSlotQualityCheck, onRecordModelPerf, CHECKPOINT_KEY])
  const handleGenerateFullThrottled = useThrottledCallback(handleGenerateFull, 500)

  const cancelFullGeneration = useCallback(() => {
    if (fullGenAbortRef.current) {
      setCancellingGen(true)
      fullGenAbortRef.current.abort()
      fullGenAbortRef.current = null
      setFlowSteps((prev) => prev.map((s) => s.status === 'loading' ? { ...s, status: 'failed' as const, errorMsg: '已取消', endTime: Date.now() } : s))
      setTimeout(() => setCancellingGen(false), 600)
    }
    setGenLoading(false)
    setFullGenProgress(null)
  }, [])

  const handleFlowPanelClose = useCallback(() => {
    setFlowPanelVisible(false)
  }, [])

  const handleFlowPanelDismiss = useCallback(() => {
    setFlowPanelVisible(false)
    setFlowSteps([])
    setFullGenProgress(null)
    setLastSlotDoneScriptId(null)
    if (fullGenAbortRef.current) {
      fullGenAbortRef.current.abort()
      fullGenAbortRef.current = null
    }
    setGenLoading(false)
  }, [])

  /** 增量生成 */
  const handleGenerateIncremental = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    const emptyScripts = scripts.filter((s) => !s.scriptContent || String(s.scriptContent).trim().length === 0)
    if (emptyScripts.length === 0) {
      toast('所有槽位均已有内容', 'info')
      return
    }
    setGenLoading(true)
    setFlowPanelVisible(true)
    setFlowSteps(emptyScripts.map((s, i) => ({
      label: s.scriptType === 'opening' ? '开场' : s.scriptType === 'closing' ? '结尾' : s.scriptType === 'transition' ? `衔接 ${i + 1}` : `产品 ${i + 1}`,
      status: 'pending' as const,
      stepKey: `inc-${s.id}`,
      scriptId: s.id,
    })))
    setFullGenProgress({ current: 0, total: emptyScripts.length, slotType: '增量生成' })
    let done = 0
    for (let i = 0; i < emptyScripts.length; i++) {
      const script = emptyScripts[i]
      setFlowSteps((prev) => prev.map((s) => s.stepKey === `inc-${script.id}` ? { ...s, status: 'loading' as const, startTime: Date.now() } : s))
      try {
        const content = String(await generateForSlot({ scriptId: script.id, modelId: selectedModelId as number }))
        if (content) {
          await saveLiveScript({ id: script.id, sessionId, scriptContent: content })
          setScripts((prev) => prev.map((s) => s.id === script.id ? { ...s, scriptContent: content } : s))
        }
        setFlowSteps((prev) => prev.map((s) => s.stepKey === `inc-${script.id}` ? { ...s, status: 'done' as const, content: String(content).slice(0, 100), endTime: Date.now() } : s))
        done++
      } catch (e) {
        setFlowSteps((prev) => prev.map((s) => s.stepKey === `inc-${script.id}` ? { ...s, status: 'failed' as const, errorMsg: e instanceof Error ? e.message : '生成失败', endTime: Date.now() } : s))
      }
      setFullGenProgress({ current: i + 1, total: emptyScripts.length, slotType: `增量 ${i + 1}/${emptyScripts.length}` })
    }
    setGenLoading(false)
    setFullGenProgress(null)
    toast(`增量生成完成：${done}/${emptyScripts.length}`, done === emptyScripts.length ? 'success' : 'warning')
    await loadData()
  }, [sessionId, selectedModelId, scripts, setScripts, loadData, toast])

  /** 断点续生成 */
  const handleResumeGeneration = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    let completedIds: number[] = []
    try {
      const raw = sessionStorage.getItem(CHECKPOINT_KEY)
      if (raw) {
        const cp = JSON.parse(raw)
        completedIds = cp.completedSlotIds ?? []
      }
    } catch { /* ignore */ }
    const unfinished = scripts.filter((s) => !completedIds.includes(s.id) && (!s.scriptContent || String(s.scriptContent).trim().length === 0))
    if (unfinished.length === 0) {
      toast('所有槽位均已完成', 'info')
      setResumeAvailable(false)
      try { sessionStorage.removeItem(CHECKPOINT_KEY) } catch { /* ignore */ }
      return
    }
    setGenLoading(true)
    setFlowPanelVisible(true)
    setFlowSteps(unfinished.map((s, i) => ({
      label: s.scriptType === 'opening' ? '开场' : s.scriptType === 'closing' ? '结尾' : s.scriptType === 'transition' ? `衔接 ${i + 1}` : `产品 ${i + 1}`,
      status: 'pending' as const,
      stepKey: `resume-${s.id}`,
      scriptId: s.id,
    })))
    setFullGenProgress({ current: 0, total: unfinished.length, slotType: '恢复生成' })
    let done = 0
    for (let i = 0; i < unfinished.length; i++) {
      const script = unfinished[i]
      setFlowSteps((prev) => prev.map((s) => s.stepKey === `resume-${script.id}` ? { ...s, status: 'loading' as const, startTime: Date.now() } : s))
      try {
        const content = String(await generateForSlot({ scriptId: script.id, modelId: selectedModelId as number }))
        if (content) {
          await saveLiveScript({ id: script.id, sessionId, scriptContent: content })
          setScripts((prev) => prev.map((s) => s.id === script.id ? { ...s, scriptContent: content } : s))
        }
        setFlowSteps((prev) => prev.map((s) => s.stepKey === `resume-${script.id}` ? { ...s, status: 'done' as const, content: String(content).slice(0, 100), endTime: Date.now() } : s))
        done++
      } catch (e) {
        setFlowSteps((prev) => prev.map((s) => s.stepKey === `resume-${script.id}` ? { ...s, status: 'failed' as const, errorMsg: e instanceof Error ? e.message : '生成失败', endTime: Date.now() } : s))
      }
      setFullGenProgress({ current: i + 1, total: unfinished.length, slotType: `恢复 ${i + 1}/${unfinished.length}` })
    }
    setGenLoading(false)
    setFullGenProgress(null)
    setResumeAvailable(false)
    try { sessionStorage.removeItem(CHECKPOINT_KEY) } catch { /* ignore */ }
    toast(`恢复生成完成：${done}/${unfinished.length}`, done === unfinished.length ? 'success' : 'warning')
    await loadData()
  }, [sessionId, selectedModelId, scripts, setScripts, loadData, toast, CHECKPOINT_KEY])

  const handleDismissResume = useCallback(() => {
    setResumeAvailable(false)
    try { sessionStorage.removeItem(CHECKPOINT_KEY) } catch { /* ignore */ }
  }, [CHECKPOINT_KEY])

  /** 批量重试所有失败步骤 */
  const handleRetryAllFailed = useCallback(async () => {
    const failedSteps = flowStepsRef.current.filter((s) => s.status === 'failed' && s.scriptId)
    if (failedSteps.length === 0) {
      toast('没有失败的步骤', 'info')
      return
    }
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setGenLoading(true)
    setFlowSteps((prev) => prev.map((s) => s.status === 'failed' && s.scriptId
      ? { ...s, status: 'pending' as const, errorMsg: undefined, endTime: undefined }
      : s
    ))
    setFullGenProgress({ current: 0, total: failedSteps.length, slotType: '重试失败' })
    let done = 0
    for (let i = 0; i < failedSteps.length; i++) {
      const step = failedSteps[i]
      const scriptId = step.scriptId!
      const stepKey = step.stepKey ?? `s-${scriptId}`
      setFlowSteps((prev) => prev.map((s) => (s.scriptId === scriptId || s.stepKey === stepKey)
        ? { ...s, status: 'loading' as const, startTime: Date.now() }
        : s
      ))
      try {
        const content = String(await generateForSlot({ scriptId, modelId: selectedModelId as number }))
        if (content) {
          await saveLiveScript({ id: scriptId, sessionId: sessionId as number, scriptContent: content })
          setScripts((prev) => prev.map((s) => s.id === scriptId ? { ...s, scriptContent: content } : s))
        }
        setFlowSteps((prev) => prev.map((s) => (s.scriptId === scriptId || s.stepKey === stepKey)
          ? { ...s, status: 'done' as const, content: String(content).slice(0, 100), endTime: Date.now() }
          : s
        ))
        done++
      } catch (e) {
        setFlowSteps((prev) => prev.map((s) => (s.scriptId === scriptId || s.stepKey === stepKey)
          ? { ...s, status: 'failed' as const, errorMsg: e instanceof Error ? e.message : '重试失败', endTime: Date.now() }
          : s
        ))
      }
      setFullGenProgress({ current: i + 1, total: failedSteps.length, slotType: `重试 ${i + 1}/${failedSteps.length}` })
    }
    setGenLoading(false)
    setFullGenProgress(null)
    toast(`重试完成：${done}/${failedSteps.length}`, done === failedSteps.length ? 'success' : 'warning')
    await loadData()
  }, [selectedModelId, sessionId, setScripts, loadData, toast])

  return {
    genLoading, setGenLoading,
    genJustCompleted,
    sseReconnecting,
    fullGenProgress, setFullGenProgress,
    flowSteps, setFlowSteps, flowStepsRef,
    flowPanelVisible, setFlowPanelVisible,
    lastSlotDoneScriptId,
    cancellingGen,
    cancelFullGeneration,
    handleFlowPanelClose,
    handleFlowPanelDismiss,
    handleGenerateFullThrottled,
    handleGenerateFull,
    handleGenerateIncremental,
    resumeAvailable, handleResumeGeneration, handleDismissResume,
    handleRetryAllFailed,
  }
}
