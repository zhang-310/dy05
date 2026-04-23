/**
 * 直播实时面板 Hook
 * 管理面板的状态、SSE 订阅、话术导航等逻辑
 */

import { useState, useEffect, useRef, useCallback } from 'react'
import * as liveRealtimeAPI from '@/api/live-realtime'
import type {
  LiveSessionScriptSlotVO,
  LiveSessionRealtimeDataVO,
  PanelInitVO
} from '@/types/live-realtime'

/**
 * 直播实时面板 Hook
 * 负责初始化面板、管理状态、订阅实时推送
 *
 * @param sessionId 直播场次 ID
 * @returns 面板状态和操作函数
 *
 * @example
 * ```typescript
 * const {
 *   panel,
 *   currentSlot,
 *   realtimeData,
 *   loading,
 *   error,
 *   navigateNext,
 *   navigatePrev,
 *   jumpTo,
 *   completeSlot
 * } = useLiveRealtimePanel(sessionId);
 * ```
 */
export function useLiveRealtimePanel(sessionId: number) {
  // 状态
  const [panel, setPanel] = useState<PanelInitVO | null>(null)
  const [currentSlot, setCurrentSlot] = useState<LiveSessionScriptSlotVO | null>(null)
  const [realtimeData, setRealtimeData] = useState<LiveSessionRealtimeDataVO | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 引用
  const eventSourceRef = useRef<EventSource | null>(null)
  const reconnectRef = useRef<(() => void) | null>(null)

  /**
   * 初始化面板
   */
  const initializePanel = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      const data = await liveRealtimeAPI.initializePanel(sessionId)
      setPanel(data)

      // 设置当前话术
      if (data.slots && data.slots.length > 0) {
        const current = data.slots[data.currentSlotIndex] || data.slots[0]
        setCurrentSlot(current)
      }

      // 设置实时数据
      if (data.realtimeData) {
        setRealtimeData(data.realtimeData)
      }

      return data
    } catch (err) {
      const message = err instanceof Error ? err.message : '初始化面板失败'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }, [sessionId])

  /**
   * 订阅 SSE 实时推送
   */
  const subscribeToRealtime = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const { close, reconnect } = liveRealtimeAPI.subscribeWithRetry(
      sessionId,
      {
        onDataUpdate: (data) => {
          setRealtimeData((prev) =>
            prev
              ? {
                  ...prev,
                  likeCount: data.likeCount,
                  commentCount: data.commentCount,
                  viewerCount: data.viewerCount,
                  watchedCount: data.watchedCount
                }
              : null
          )
        },
        onSlotChange: (slot) => {
          // 根据推送的 currentSlotIndex 更新当前话术
          if (panel && panel.slots) {
            const newSlot = panel.slots[slot.currentSlotIndex]
            if (newSlot) {
              setCurrentSlot(newSlot)
            }
          }
        },
        onSlotCompleted: (data) => {
          // 更新对应段落的完成状态
          setPanel((prev) => {
            if (!prev) return null

            return {
              ...prev,
              slots: prev.slots.map((s) =>
                s.slotIndex === data.completedSlotIndex
                  ? { ...s, isCompleted: true, completedAt: new Date(data.completedAt).toISOString() }
                  : s
              )
            }
          })
        },
        onConnected: () => {
          console.log('SSE 实时推送已连接')
        },
        onError: (error) => {
          console.error('SSE 推送异常:', error)
          setError(`实时推送异常: ${error.message}`)
        }
      },
      3,
      1000
    )

    reconnectRef.current = reconnect

    return close
  }, [sessionId, panel])

  /**
   * 话术导航 - 下一个
   */
  const navigateNext = useCallback(async () => {
    setError(null)
    try {
      const slot = await liveRealtimeAPI.nextSlot(sessionId)
      setCurrentSlot(slot)
      return slot
    } catch (err) {
      const message = err instanceof Error ? err.message : '无法跳转到下一话术'
      setError(message)
      throw err
    }
  }, [sessionId])

  /**
   * 话术导航 - 上一个
   */
  const navigatePrev = useCallback(async () => {
    setError(null)
    try {
      const slot = await liveRealtimeAPI.prevSlot(sessionId)
      setCurrentSlot(slot)
      return slot
    } catch (err) {
      const message = err instanceof Error ? err.message : '无法返回到上一话术'
      setError(message)
      throw err
    }
  }, [sessionId])

  /**
   * 话术导航 - 跳转到指定位置
   */
  const jumpTo = useCallback(
    async (slotIndex: number) => {
      setError(null)
      try {
        const slot = await liveRealtimeAPI.jumpSlot(sessionId, slotIndex)
        setCurrentSlot(slot)
        return slot
      } catch (err) {
        const message = err instanceof Error ? err.message : `无法跳转到话术 ${slotIndex}`
        setError(message)
        throw err
      }
    },
    [sessionId]
  )

  /**
   * 标记话术完成
   */
  const completeCurrentSlot = useCallback(async () => {
    if (!currentSlot) {
      setError('没有当前话术可完成')
      return
    }

    setError(null)
    try {
      const slot = await liveRealtimeAPI.completeSlot(sessionId, currentSlot.slotIndex)
      setCurrentSlot(slot)
      return slot
    } catch (err) {
      const message = err instanceof Error ? err.message : '标记完成失败'
      setError(message)
      throw err
    }
  }, [sessionId, currentSlot])

  /**
   * 更新实时数据
   */
  const updateRealtimeData = useCallback(
    async (data: Partial<LiveSessionRealtimeDataVO>) => {
      setError(null)
      try {
        const updated = await liveRealtimeAPI.updateRealtimeData({
          liveSessionId: sessionId,
          ...data
        })
        setRealtimeData(updated)
        return updated
      } catch (err) {
        const message = err instanceof Error ? err.message : '更新实时数据失败'
        setError(message)
        throw err
      }
    },
    [sessionId]
  )

  /**
   * 初始化和清理
   */
  useEffect(() => {
    // 初始化面板
    const init = async () => {
      try {
        await initializePanel()
      } catch (err) {
        console.error('初始化失败:', err)
      }
    }

    init()

    // 订阅实时推送
    const unsubscribe = subscribeToRealtime()

    // 清理
    return () => {
      unsubscribe()
    }
  }, [sessionId, initializePanel, subscribeToRealtime])

  return {
    panel,
    currentSlot,
    realtimeData,
    loading,
    error,
    navigateNext,
    navigatePrev,
    jumpTo,
    completeCurrentSlot,
    updateRealtimeData,
    reconnect: reconnectRef.current
  }
}
