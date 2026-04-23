/**
 * useLiveScriptCore — 话术工作台核心共享 Hook
 *
 * 抽取 useLiveScriptBuilder 与 useScriptBuilderPageState 的公共核心逻辑：
 * - 场次/商品/话术数据加载（含竞态保护 + 请求去重）
 * - 槽位自动初始化
 * - AI 模型列表加载与持久化选择
 * - 热搜词注入（行业大脑 → 话术生成）
 *
 * 使用规范：
 * - useLiveScriptBuilder（场次内嵌工作台）→ 在此 hook 基础上组合 composition hooks
 * - useScriptBuilderPageState（独立话术工作台）→ 在此 hook 基础上扩展跨场次能力
 */

import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react'
import {
  getSession,
  type LiveSessionVO,
} from '@/api/live'
import { getModelsByTaskCode, listAiModels } from '@/api/ai'
import { getProductsBySession, type LiveProduct } from '@/api/live-product'
import { getScriptsBySession, initScriptSlots, type LiveScript } from '@/api/live-script'
import type { AiModelVO } from '@/types/ai'
import { useIndustryBrainStore } from '@/stores/industryBrainStore'
import { useLiveGenStore } from '@/stores/liveGenStore'

export interface LiveScriptCoreState {
  /** 当前场次详情 */
  session: LiveSessionVO | null
  /** 商品列表 */
  products: LiveProduct[]
  /** 话术列表 */
  scripts: LiveScript[]
  /** 首次加载中 */
  loading: boolean
  /** 刷新中（非首次） */
  dataReloading: boolean
  /** 可用 AI 模型 */
  aiModels: AiModelVO[]
  /** 当前选中模型 ID（持久化，string 类型来自 Zustand store） */
  selectedModelId: string
  /** 当前注入的热搜词 */
  hotKeywords: string[]
  /** 更新选中模型 */
  setSelectedModelId: (id: string) => void
  /** 更新热搜词 */
  setHotKeywords: (v: string[] | ((prev: string[]) => string[])) => void
  /** 直接更新话术列表（用于即时 UI 响应，loadData 会完整刷新） */
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  /** 直接更新商品列表（用于即时 UI 响应，loadData 会完整刷新） */
  setProducts: React.Dispatch<React.SetStateAction<LiveProduct[]>>
  /** 重新加载数据 */
  loadData: () => Promise<void>
  /** 刷新话术列表 */
  reloadScripts: () => Promise<void>
}

/**
 * 话术工作台核心 Hook
 *
 * @param sessionId 场次 ID（'' 表示未选择）
 * @param onStyleSync 场次加载完成后同步风格的回调（可选）
 */
export function useLiveScriptCore(
  sessionId: number | '',
  onStyleSync?: (style: string) => void
): LiveScriptCoreState {
  const [session, setSession] = useState<LiveSessionVO | null>(null)
  const [products, setProducts] = useState<LiveProduct[]>([])
  const [scripts, setScripts] = useState<LiveScript[]>([])
  const [loading, setLoading] = useState(true)
  const [dataReloading, setDataReloading] = useState(false)
  const [aiModels, setAiModels] = useState<AiModelVO[]>([])

  // 模型选择持久化（Zustand）
  const selectedModelId = useLiveGenStore((s) => s.selectedModelId)
  const setSelectedModelId = useLiveGenStore((s) => s.setSelectedModelId)

  // 热搜词（行业大脑注入）
  const [hotKeywords, setHotKeywordsState] = useState<string[]>([])
  useEffect(() => {
    const pending = useIndustryBrainStore.getState().consumePendingHotKeywords()
    if (pending.length > 0) {
      setHotKeywordsState((prev) => Array.from(new Set([...prev, ...pending])))
    }
  }, [])
  const setHotKeywords = useCallback((v: string[] | ((prev: string[]) => string[])) => {
    setHotKeywordsState(v)
  }, [])

  // ── 模型列表加载 ──────────────────────────────────────────────────────────────

  useEffect(() => {
    getModelsByTaskCode('copy_processing')
      .then(setAiModels)
      .catch(() => listAiModels(1).then(setAiModels).catch(() => setAiModels([])))
  }, [])

  const defaultModelId = useMemo(() => {
    return aiModels.length > 0 ? (aiModels[0]?.id ?? null) : null
  }, [aiModels])

  useEffect(() => {
    if (aiModels.length === 0) return
    if (selectedModelId && aiModels.some((m) => String(m.id) === String(selectedModelId))) return
    if (defaultModelId != null) setSelectedModelId(String(defaultModelId))
  }, [defaultModelId, selectedModelId, aiModels, setSelectedModelId])

  // ── 数据加载（竞态保护 + 请求去重）─────────────────────────────────────────────

  const loadDataRequestIdRef = useRef(0)
  const loadDataInFlightRef = useRef<{ sessionId: number; requestId: number; promise: Promise<void> } | null>(null)
  const lastLoadedSessionIdRef = useRef<number | null>(null)

  const loadData = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    const sid = sessionId
    const existing = loadDataInFlightRef.current
    if (existing && existing.sessionId === sid) {
      return existing.promise
    }
    const requestId = ++loadDataRequestIdRef.current
    const isRefresh = lastLoadedSessionIdRef.current === sid
    if (isRefresh) {
      setDataReloading(true)
    } else {
      setLoading(true)
    }
    const promise = (async () => {
      try {
        const [s, p, sc] = await Promise.all([
          getSession(sid).catch(() => null),
          getProductsBySession(sid).catch(() => []),
          getScriptsBySession(sid).catch(() => []),
        ])
        if (requestId !== loadDataRequestIdRef.current) return
        setSession(s ?? null)
        setProducts(p)
        setScripts(sc)

        // 自动初始化槽位：无话术但有商品，或2小时聊天场次
        const needInitSlots =
          sc.length === 0 &&
          (p.length > 0 || s?.sessionType === 'chat_2h')
        if (needInitSlots) {
          await initScriptSlots(sid)
          if (requestId !== loadDataRequestIdRef.current) return
          const sc2 = await getScriptsBySession(sid).catch(() => [])
          if (requestId !== loadDataRequestIdRef.current) return
          setScripts(sc2)
        }

        // 同步场次风格
        if (s && s.scriptStyle && onStyleSync) {
          onStyleSync(String(s.scriptStyle ?? ''))
        }

        if (requestId === loadDataRequestIdRef.current) {
          lastLoadedSessionIdRef.current = sid
        }
      } catch {
        if (requestId !== loadDataRequestIdRef.current) return
        setSession(null)
        setProducts([])
        setScripts([])
      } finally {
        if (requestId === loadDataRequestIdRef.current) {
          setLoading(false)
          setDataReloading(false)
        }
        if (loadDataInFlightRef.current?.requestId === requestId) {
          loadDataInFlightRef.current = null
        }
      }
    })()
    loadDataInFlightRef.current = { sessionId: sid, requestId, promise }
    return promise
    // onStyleSync 故意不加入依赖，避免频繁触发
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId])

  // 仅刷新话术列表（不重置商品/场次）
  const reloadScripts = useCallback(async () => {
    if (!sessionId || typeof sessionId !== 'number') return
    const sid = sessionId
    try {
      const sc = await getScriptsBySession(sid).catch(() => [])
      setScripts(sc)
    } catch {
      // 静默失败
    }
  }, [sessionId])

  // sessionId 变化时自动触发数据加载
  useEffect(() => {
    loadData()
  }, [loadData])

  return {
    session,
    products,
    scripts,
    loading,
    dataReloading,
    aiModels,
    selectedModelId,
    hotKeywords,
    setSelectedModelId,
    setHotKeywords,
    setScripts,
    setProducts,
    loadData,
    reloadScripts,
  }
}
