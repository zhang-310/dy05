import { useState, useCallback, useRef, useEffect } from 'react'
import { useThrottledCallback } from '@/hooks/useDebouncedCallback'
import { useToast } from '@/contexts/ToastContext'
import {
  batchChatForScript,
  checkSimilarity,
  generateSkeletonStream,
  refineScriptStream,
  refineSegment,
  deleteLiveScript,
  saveLiveScript,
} from '@/api/live'
import { batchSaveLiveScripts, type LiveScript } from '@/api/live-script'
import type { SimilarityItem, SkeletonItem, ScriptSectionData } from './useLiveScriptBuilder'

export interface UseQualityCheckDeps {
  sessionId: number | ''
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  scriptSections: ScriptSectionData[]
  loadData: () => Promise<void>
  session: Record<string, unknown> | null
  // Cross-hook: editing state setters from scriptEditor
  setEditingId: (id: number | null) => void
  setEditingScript: (s: LiveScript | null) => void
  setEditContent: (c: string) => void
  setEditDurationLimit: (v: number | '') => void
  setEditRequirement: (v: string) => void
  // Cross-hook: chat state
  prepareChatForSkeleton: (summary: string, liveTitle: string | undefined) => void
  selectedModelId: number | ''
}

export function useQualityCheck({
  sessionId,
  scripts: _scripts,
  setScripts,
  scriptSections,
  loadData,
  session,
  setEditingId,
  setEditingScript,
  setEditContent,
  setEditDurationLimit,
  setEditRequirement,
  prepareChatForSkeleton,
  selectedModelId,
}: UseQualityCheckDeps) {
  const toast = useToast()

  const [selectedScriptIds, setSelectedScriptIds] = useState<Set<number>>(new Set())
  const [lockedScriptIds, setLockedScriptIds] = useState<Set<number>>(new Set())
  const [batchOpen, setBatchOpen] = useState(false)
  const [batchMessage, setBatchMessage] = useState('')
  const [batchLoading, setBatchLoading] = useState(false)
  const [similarityOpen, setSimilarityOpen] = useState(false)
  const [similarityList, setSimilarityList] = useState<SimilarityItem[]>([])
  const [similarityLoading, setSimilarityLoading] = useState(false)
  const [skeletonOpen, setSkeletonOpen] = useState(false)
  const [skeletonList, setSkeletonList] = useState<SkeletonItem[]>([])
  const [skeletonLoading, setSkeletonLoading] = useState(false)
  const [refineOpen, setRefineOpen] = useState<{ scriptId: number } | null>(null)
  const [refineQuestion, setRefineQuestion] = useState('')
  const [refineLoading, setRefineLoading] = useState(false)
  const [refineSegmentOpen, setRefineSegmentOpen] = useState<{ scriptId: number; segmentText: string } | null>(null)
  const [refineSegmentInstruction, setRefineSegmentInstruction] = useState('')
  const [refineSegmentLoading, setRefineSegmentLoading] = useState(false)
  const [refineStreaming, setRefineStreaming] = useState(false)
  const [refineContent, setRefineContent] = useState('')
  const refineAbortRef = useRef<AbortController | null>(null)
  const refineStreamedRef = useRef('')
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(['opening', 'closing']))

  // Cleanup SSE AbortControllers on unmount to prevent memory leaks
  useEffect(() => () => {
    refineAbortRef.current?.abort()
    skeletonAbortRef.current?.abort()
  }, [])

  const toggleScriptSelect = (id: number) => {
    setSelectedScriptIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const toggleScriptLock = useCallback((id: number) => {
    setLockedScriptIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }, [])

  // Expose for deleteScript cross-hook usage
  const removeFromSelected = useCallback((id: number) => {
    setSelectedScriptIds((prev) => { const next = new Set(prev); next.delete(id); return next })
  }, [])

  const handleBatchApply = async () => {
    if (selectedScriptIds.size === 0 || !batchMessage.trim() || !sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setBatchLoading(true)
    try {
      const ids = Array.from(selectedScriptIds).filter((id) => !lockedScriptIds.has(id))
      if (ids.length === 0) {
        toast('所选话术均已锁定，无法批量操作', 'warning')
        setBatchLoading(false)
        return
      }
      const result = await batchChatForScript({ ids, message: batchMessage.trim(), modelId: selectedModelId as number }) as Record<string, string>
      const saveItems = Object.entries(result).map(([scriptId, content]) => ({
        id: Number(scriptId),
        sessionId,
        scriptContent: content,
      }))
      const savedScripts = await batchSaveLiveScripts(sessionId, saveItems)
      if (Array.isArray(savedScripts) && savedScripts.length > 0) {
        const savedMap = new Map(savedScripts.map((s) => [s.id, s]))
        setScripts((prev) => prev.map((s) => {
          const updated = savedMap.get(s.id)
          return updated ? updated : s
        }))
      } else {
        await loadData()
      }
      toast(`已批量更新 ${ids.length} 段话术`, 'success')
      setBatchOpen(false)
      setBatchMessage('')
      setSelectedScriptIds(new Set())
    } catch (e) {
      toast(e instanceof Error ? e.message : '批量生成失败', 'error')
    } finally {
      setBatchLoading(false)
    }
  }

  const handleCheckSimilarity = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    setSimilarityLoading(true)
    setSimilarityOpen(true)
    setSimilarityList([])
    try {
      const list = (await checkSimilarity({ sessionId }) as { list?: SimilarityItem[] })?.list ?? []
      setSimilarityList(list)
      toast(list.length ? `发现 ${list.length} 组相似段落` : '未发现明显相似段落', list.length ? 'warning' : 'success')
    } catch (e) {
      toast(e instanceof Error ? e.message : '相似度检测失败', 'error')
    } finally {
      setSimilarityLoading(false)
    }
  }, [sessionId, toast])
  const handleCheckSimilarityThrottled = useThrottledCallback(handleCheckSimilarity, 500)

  const skeletonAbortRef = useRef<AbortController | null>(null)
  const handleGenerateSkeleton = useCallback(() => {
    if (!sessionId || typeof sessionId !== 'number') return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setSkeletonLoading(true)
    setSkeletonOpen(true)
    setSkeletonList([])
    skeletonAbortRef.current?.abort()
    generateSkeletonStream({ sessionId, modelId: selectedModelId as number })
      .then((data) => {
        const result = data as { list?: SkeletonItem[] } | undefined
        const list = result?.list ?? []
        setSkeletonList(list)
        setSkeletonLoading(false)
        void loadData()
        toast('骨架生成成功', 'success')
      })
      .catch((e: unknown) => {
        setSkeletonLoading(false)
        toast(e instanceof Error ? e.message : '骨架生成失败', 'error')
      })
    skeletonAbortRef.current = null
  }, [sessionId, selectedModelId, loadData, toast])
  const handleGenerateSkeletonThrottled = useThrottledCallback(handleGenerateSkeleton, 500)

  const handleSkeletonExpand = (scriptId: number, summary: string, duration: number) => {
    const sec = scriptSections.find((s) => s.scripts.some((r) => r.id === scriptId))
    if (sec) {
      setExpandedSections((prev) => new Set([...prev, sec.key]))
    }
    const row = scriptSections.flatMap((s) => s.scripts).find((r) => r.id === scriptId)
    setEditingId(scriptId)
    setEditingScript(row ?? null)
    setEditContent(row?.scriptContent ? String(row.scriptContent) : '')
    setEditDurationLimit(duration > 0 ? duration : (typeof row?.durationLimitSec === 'number' ? row.durationLimitSec : ''))
    setEditRequirement(summary || (row?.requirement ? String(row.requirement) : ''))
    prepareChatForSkeleton(summary, session?.liveTitle ? String(session.liveTitle) : undefined)
    setSkeletonOpen(false)
  }

  const handleRefine = () => {
    if (!refineOpen || !refineQuestion.trim()) return
    if (!selectedModelId) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    const scriptId = refineOpen.scriptId
    const row = scriptSections.flatMap((s) => s.scripts).find((r) => r.id === scriptId)
    setRefineLoading(true)
    setRefineStreaming(true)
    setRefineContent('')
    refineStreamedRef.current = ''
    refineAbortRef.current?.abort()
    refineScriptStream({ scriptId, question: refineQuestion.trim(), modelId: selectedModelId as number })
      .then((result: unknown) => {
        refineAbortRef.current = null
        const final = (typeof result === 'string' ? result : '').trim()
        setRefineStreaming(false)
        setRefineLoading(false)
        setEditContent(final)
        setEditingId(scriptId)
        setEditDurationLimit(typeof row?.durationLimitSec === 'number' ? row.durationLimitSec : '')
        setEditRequirement(row?.requirement ? String(row.requirement) : '')
        setRefineOpen(null)
        setRefineQuestion('')
        toast('AI 迭代成功', 'success')
      })
      .catch((e: unknown) => {
        refineAbortRef.current = null
        setRefineStreaming(false)
        setRefineLoading(false)
        toast(e instanceof Error ? e.message : 'AI 迭代失败', 'error')
      })
    refineAbortRef.current = null
  }

  const handleRefineSegment = useCallback(async () => {
    if (!refineSegmentOpen || !refineSegmentInstruction.trim()) return
    const modelId = typeof selectedModelId === 'number' ? selectedModelId : null
    if (modelId == null) {
      toast('请先选择 AI 模型', 'error')
      return
    }
    setRefineSegmentLoading(true)
    try {
      const newContent = await refineSegment({
        scriptId: refineSegmentOpen.scriptId,
        segmentText: refineSegmentOpen.segmentText,
        instruction: refineSegmentInstruction.trim(),
        modelId,
      })
      const content = typeof newContent === 'string' ? newContent : String(newContent)
      await saveLiveScript({ id: refineSegmentOpen.scriptId, scriptContent: content })
      await loadData()
      setRefineSegmentOpen(null)
      setRefineSegmentInstruction('')
      toast('段内微调成功', 'success')
    } catch (e) {
      toast(e instanceof Error ? e.message : '段内微调失败', 'error')
    } finally {
      setRefineSegmentLoading(false)
    }
  }, [refineSegmentOpen, refineSegmentInstruction, selectedModelId, loadData, toast])

  const toggleSection = (key: string) => {
    setExpandedSections((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  const handleExpandSections = useCallback((keys: string[]) => {
    setExpandedSections((prev) => {
      const next = new Set(prev)
      keys.forEach((k) => next.add(k))
      return next
    })
  }, [])

  const handleCollapseSections = useCallback(() => {
    setExpandedSections(new Set())
  }, [])

  // For product click (adds section key to expanded)
  const expandSection = useCallback((key: string) => {
    setExpandedSections((prev) => new Set([...prev, key]))
  }, [])

  // ── 批量删除 ──
  const [batchDeleteLoading, setBatchDeleteLoading] = useState(false)

  // ── 批量时长设置 ──
  const [batchDurationLoading, setBatchDurationLoading] = useState(false)
  const handleBatchSetDuration = useCallback(async (duration: number) => {
    if (selectedScriptIds.size === 0 || !sessionId || typeof sessionId !== 'number') return
    setBatchDurationLoading(true)
    try {
      const items = Array.from(selectedScriptIds).map((id) => ({
        id,
        sessionId: sessionId as number,
        durationLimitSec: duration,
      }))
      await batchSaveLiveScripts(sessionId, items)
      toast(`已为 ${items.length} 段话术设置时长 ${duration}s`, 'success')
      await loadData()
    } catch (e) {
      toast(e instanceof Error ? e.message : '批量设置时长失败', 'error')
    } finally {
      setBatchDurationLoading(false)
    }
  }, [selectedScriptIds, sessionId, loadData, toast])
  const handleBatchDeleteSelected = useCallback(async () => {
    if (selectedScriptIds.size === 0) return
    setBatchDeleteLoading(true)
    try {
      const ids = Array.from(selectedScriptIds)
      await Promise.all(ids.map((id) => deleteLiveScript(id)))
      toast(`已删除 ${ids.length} 段话术`, 'success')
      setSelectedScriptIds(new Set())
      await loadData()
    } catch (e) {
      toast(e instanceof Error ? e.message : '批量删除失败', 'error')
    } finally {
      setBatchDeleteLoading(false)
    }
  }, [selectedScriptIds, loadData, toast])

  // ── 骨架时间分配批量确认 ──
  const [skeletonDurationsLoading, setSkeletonDurationsLoading] = useState(false)
  const handleSkeletonDurationsConfirm = useCallback(async (items: SkeletonItem[]) => {
    setSkeletonDurationsLoading(true)
    try {
      await Promise.all(items.map((item) => saveLiveScript({ id: item.scriptId, durationLimitSec: item.suggestedDurationSec })))
      toast('时间分配已保存', 'success')
      await loadData()
      setSkeletonOpen(false)
    } catch (e) {
      toast(e instanceof Error ? e.message : '保存时间分配失败', 'error')
    } finally {
      setSkeletonDurationsLoading(false)
    }
  }, [loadData, toast])

  return {
    selectedScriptIds, toggleScriptSelect,
    lockedScriptIds, toggleScriptLock,
    batchOpen, setBatchOpen,
    batchMessage, setBatchMessage,
    batchLoading, handleBatchApply,
    similarityOpen, setSimilarityOpen,
    similarityList, similarityLoading,
    handleCheckSimilarityThrottled,
    skeletonOpen, setSkeletonOpen,
    skeletonList, skeletonLoading,
    handleGenerateSkeletonThrottled,
    handleSkeletonExpand,
    refineOpen, setRefineOpen,
    refineQuestion, setRefineQuestion,
    refineLoading, refineStreaming, refineContent,
    handleRefine,
    refineSegmentOpen, setRefineSegmentOpen,
    refineSegmentInstruction, setRefineSegmentInstruction,
    refineSegmentLoading, handleRefineSegment,
    expandedSections, toggleSection, handleExpandSections, handleCollapseSections,
    expandSection,
    removeFromSelected,
    batchDeleteLoading, handleBatchDeleteSelected,
    batchDurationLoading, handleBatchSetDuration,
    skeletonDurationsLoading, handleSkeletonDurationsConfirm,
  }
}
