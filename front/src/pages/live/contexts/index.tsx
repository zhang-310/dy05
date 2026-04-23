import { useState, useCallback, useRef, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { CoreDataContext } from './CoreDataContext'
import { GenerationContext } from './GenerationContext'
import { EditorContext } from './EditorContext'
import { liveApi } from '@/api/live'
import { productApi } from '@/api/product'
import { ssePost } from '@/utils/sse-client'
import type { LiveScript, LiveScriptSave } from '@/api/live'
import type { LiveProduct } from '@/api/live-product'

export { useCoreData } from './CoreDataContext'
export { useGeneration } from './GenerationContext'
export { useEditor } from './EditorContext'

interface WorkspaceProvidersProps {
  sessionId: number
  children: React.ReactNode
}

interface SseProgressData {
  percent?: number
  current?: number
  total?: number
  slotType?: string
  slotLabel?: string
  [key: string]: unknown
}

export function WorkspaceProviders({ sessionId, children }: WorkspaceProvidersProps) {
  const qc = useQueryClient()

  // ── CoreData ──────────────────────────────────────────────────────────────
  const { data: session, isLoading: sessionLoading, refetch: refetchSession } =
    useQuery({ queryKey: ['wb-session', sessionId], queryFn: () => liveApi.sessionGet(sessionId) })

  const { data: rawProducts = [], refetch: refetchProducts } =
    useQuery({ queryKey: ['wb-products', sessionId], queryFn: () => liveApi.productBySession(sessionId) })

  const { data: scripts = [], refetch: refetchScripts } =
    useQuery({ queryKey: ['wb-scripts', sessionId], queryFn: () => liveApi.scriptBySession(sessionId) })

  const { data: readiness = null } =
    useQuery({ queryKey: ['wb-readiness', sessionId], queryFn: () => liveApi.sessionReadiness(sessionId) })

  // ── 富化 LiveProduct：合并 DyProduct 字段 ─────────────────────────────────
  // 只在有 productId 时才请求，且仅请求一次（所有商品批量 search）
  const productIds = useMemo(() => rawProducts.map(p => p.productId), [rawProducts])

  const { data: dyProducts = [] } = useQuery({
    queryKey: ['wb-dy-products', productIds.join(',')],
    queryFn: async () => {
      if (productIds.length === 0) return []
      // 分批拉取，每次 50 个
      const result = await productApi.list({ page: 0, rows: 200 })
      return result.list ?? []
    },
    enabled: productIds.length > 0,
    staleTime: 5 * 60 * 1000,
  })

  const products: LiveProduct[] = useMemo(() => {
    if (dyProducts.length === 0) return rawProducts
    const dyMap = new Map(dyProducts.map(d => [d.id, d]))
    return rawProducts.map(lp => {
      const dy = dyMap.get(lp.productId)
      if (!dy) return lp
      return {
        ...lp,
        imageUrl: lp.imageUrl ?? dy.mainImage,
        aiSellingPoints: lp.aiSellingPoints ?? dy.sellingPoints,
        productCategory: lp.productCategory ?? dy.category,
        price: (lp.price ?? 0) > 0 ? lp.price : dy.price,
        profitMarginPct: lp.profitMarginPct ?? dy.profitMarginPct,
      }
    })
  }, [rawProducts, dyProducts])

  // ── Generation ────────────────────────────────────────────────────────────
  const [isGenerating, setIsGenerating] = useState(false)
  const [generationProgress, setGenerationProgress] = useState(0)
  const [generationMessage, setGenerationMessage] = useState('')
  const [genJustCompleted, setGenJustCompleted] = useState(false)
  const [slotTimeline, setSlotTimeline] = useState<import('./GenerationContext').SlotTimelineEntry[]>([])
  const abortRef = useRef<(() => void) | null>(null)
  const startGeneration = useCallback((options: Record<string, unknown>): Promise<void> => {
    return new Promise((resolve, reject) => {
      setIsGenerating(true)
      setGenerationProgress(0)
      setGenerationMessage('正在连接 AI...')
      setGenJustCompleted(false)
      setSlotTimeline([])

      // 字段名对齐后端 LiveAiGenerateVO
      const body: Record<string, unknown> = {
        sessionId,
        style: options.style ?? 'natural',
        durationLimitSec: options.durationLimitSec ?? options.durationPerSlot ?? 180,
        useKbRef: options.useKbRef ?? true,
        modelId: options.modelId || undefined,
        personaId: options.personaId || undefined,
        extraPrompt: options.extraPrompt || undefined,
        hotKeywords: options.hotKeywords || undefined,
        interactionLevel: options.interactionLevel || undefined,
        retentionStrategy: options.retentionStrategy || undefined,
        requirement: options.requirement || undefined,
      }

      // 骨架模式用 generate-skeleton-sse，正常模式用 generate-full-pipelined-sse
      const endpoint = options.mode === 'skeleton'
        ? '/live/ai/generate-skeleton-sse'
        : '/live/ai/generate-full-pipelined-sse'

      const ctrl = ssePost(
        endpoint,
        body,
        {
          onStatus: (status) => setGenerationMessage(status),
          onProgress: (data) => {
            const d = data as SseProgressData
            const pct = typeof d.percent === 'number' ? d.percent
              : typeof d.current === 'number' && typeof d.total === 'number' && d.total > 0
                ? Math.round(d.current / d.total)
                : 0
            setGenerationProgress(pct)
            const label = d.slotType ?? d.slotLabel ?? ''
            setGenerationMessage(label ? `正在生成：${label}` : `已完成 ${d.current ?? 0}/${d.total ?? '?'} 个商品`)
          },
          onSlotDone: (data) => {
            setGenerationMessage(`✓ ${data.slotLabel} 已完成`)
            setSlotTimeline(prev => [...prev, { scriptId: data.scriptId, slotLabel: data.slotLabel, scriptType: data.scriptType, sequenceNo: data.sequenceNo }])
          },
          onSlotFailed: (data) => {
            setGenerationMessage(`✗ ${data.slotLabel} 失败：${data.errorMsg}`)
            setSlotTimeline(prev => [...prev, { scriptId: data.scriptId, slotLabel: data.slotLabel, scriptType: '', sequenceNo: data.index, failed: true, errorMsg: data.errorMsg }])
          },
          onDone: () => {
            setGenerationProgress(100)
            setGenerationMessage('生成完成！')
            setGenJustCompleted(true)
            setIsGenerating(false)
            abortRef.current = null
            qc.invalidateQueries({ queryKey: ['wb-scripts', sessionId] })
            resolve()
          },
          onError: (err) => {
            setGenerationMessage(`生成失败：${err.message}`)
            setIsGenerating(false)
            abortRef.current = null
            reject(err)
          },
        },
        { idleTimeoutMs: 30 * 60 * 1000 }, // 30 分钟超时
      )

      abortRef.current = () => ctrl.abort()
    })
  }, [sessionId, qc])

  const cancelGeneration = useCallback(() => {
    abortRef.current?.()
    setIsGenerating(false)
    setGenerationMessage('已取消')
  }, [])

  // ── Editor ────────────────────────────────────────────────────────────────
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  const saveMut = useMutation({
    mutationFn: (p: Partial<LiveScriptSave>) => liveApi.scriptSave(p),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['wb-scripts', sessionId] }) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.scriptDelete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['wb-scripts', sessionId] }) },
  })

  const handleStartEdit = useCallback((script: LiveScript) => {
    setEditingId(script.id)
    setEditContent(script.scriptContent)
  }, [])

  const handleCancelEdit = useCallback(() => {
    setEditingId(null)
    setEditContent('')
  }, [])

  const handleSaveEdit = useCallback(async () => {
    if (!editingId) return
    setIsSaving(true)
    try {
      await saveMut.mutateAsync({ id: editingId, scriptContent: editContent, sessionId })
      setEditingId(null)
    } finally {
      setIsSaving(false)
    }
  }, [editingId, editContent, sessionId, saveMut])

  const handleScriptSave = useCallback(async (p: Partial<LiveScriptSave>) => {
    await saveMut.mutateAsync({ ...p, sessionId })
  }, [sessionId, saveMut])

  const handleScriptDelete = useCallback(async (id: number) => {
    await deleteMut.mutateAsync(id)
  }, [deleteMut])

  return (
    <CoreDataContext.Provider value={{
      session: session ?? null,
      sessionLoading,
      products,
      scripts,
      readiness,
      refetchSession,
      refetchProducts,
      refetchScripts,
    }}>
      <GenerationContext.Provider value={{
        isGenerating,
        generationProgress,
        generationMessage,
        genJustCompleted,
        slotTimeline,
        startGeneration,
        cancelGeneration,
      }}>
        <EditorContext.Provider value={{
          editingId,
          editContent,
          setEditContent,
          handleStartEdit,
          handleCancelEdit,
          handleSaveEdit,
          handleScriptSave,
          handleScriptDelete,
          isSaving,
        }}>
          {children}
        </EditorContext.Provider>
      </GenerationContext.Provider>
    </CoreDataContext.Provider>
  )
}
