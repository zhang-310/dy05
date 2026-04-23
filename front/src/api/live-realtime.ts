/**
 * 直播实时辅助面板 API 客户端
 * 提供与后端的通信接口
 */

import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import type {
  LiveSessionScriptSlotVO,
  LiveSessionRealtimeDataVO,
  PanelInitVO,
  RealtimeDataSaveVO,
  SSEDataUpdateEvent,
  SSESlotChangeEvent,
  SSESlotCompletedEvent
} from '@/types/live-realtime'

/**
 * 初始化直播实时面板
 * 查询直播的所有话术段落和实时数据
 *
 * @param sessionId 直播场次 ID
 * @returns 面板初始化数据
 */
export function initializePanel(sessionId: number) {
  return request.post<PanelInitVO>('/live/realtime-panel/init', {
    liveSessionId: sessionId
  })
}

/**
 * 跳转到下一话术段落
 *
 * @param sessionId 直播场次 ID
 * @returns 下一话术段落信息
 */
export function nextSlot(sessionId: number) {
  return request.post<LiveSessionScriptSlotVO>('/live/realtime-panel/next-slot', {
    liveSessionId: sessionId
  })
}

/**
 * 返回到上一话术段落
 *
 * @param sessionId 直播场次 ID
 * @returns 上一话术段落信息
 */
export function prevSlot(sessionId: number) {
  return request.post<LiveSessionScriptSlotVO>('/live/realtime-panel/prev-slot', {
    liveSessionId: sessionId
  })
}

/**
 * 跳转到指定序号的话术段落
 *
 * @param sessionId 直播场次 ID
 * @param slotIndex 目标段落序号
 * @returns 目标话术段落信息
 */
export function jumpSlot(sessionId: number, slotIndex: number) {
  return request.post<LiveSessionScriptSlotVO>('/live/realtime-panel/jump-slot', {
    liveSessionId: sessionId,
    slotIndex
  })
}

/**
 * 标记话术段落为已完成
 *
 * @param sessionId 直播场次 ID
 * @param slotIndex 段落序号
 * @returns 完成的话术段落信息
 */
export function completeSlot(sessionId: number, slotIndex: number) {
  return request.post<LiveSessionScriptSlotVO>('/live/realtime-panel/complete-slot', {
    liveSessionId: sessionId,
    slotIndex
  })
}

/**
 * 更新实时数据
 *
 * @param data 实时数据保存参数
 * @returns 更新后的实时数据
 */
export function updateRealtimeData(data: RealtimeDataSaveVO) {
  return request.post<LiveSessionRealtimeDataVO>('/live/realtime-panel/update-data', data)
}

/**
 * 获取实时数据
 *
 * @param sessionId 直播场次 ID
 * @returns 实时数据
 */
export function getRealtimeData(sessionId: number) {
  return request.post<LiveSessionRealtimeDataVO>('/live/realtime-panel/realtime-data', {
    liveSessionId: sessionId
  })
}

/**
 * SSE 实时推送流回调函数定义
 */
export interface SSEStreamCallbacks {
  /** 数据更新事件回调 */
  onDataUpdate?: (data: SSEDataUpdateEvent) => void
  /** 话术变化事件回调 */
  onSlotChange?: (slot: SSESlotChangeEvent) => void
  /** 话术完成事件回调 */
  onSlotCompleted?: (data: SSESlotCompletedEvent) => void
  /** 连接建立回调 */
  onConnected?: () => void
  /** 错误事件回调 */
  onError?: (error: Error) => void
}

/**
 * 订阅实时推送流
 * 建立与服务端的 SSE 连接，接收实时数据和话术变化事件
 *
 * @param sessionId 直播场次 ID
 * @param callbacks 各类事件的回调函数
 * @returns EventSource 实例，用于关闭连接
 *
 * @example
 * ```typescript
 * const eventSource = subscribeToRealtimeStream(sessionId, {
 *   onDataUpdate: (data) => {
 *     console.log('实时数据更新:', data);
 *   },
 *   onSlotChange: (slot) => {
 *     console.log('话术变化:', slot);
 *   },
 *   onError: (error) => {
 *     console.error('SSE 错误:', error);
 *   }
 * });
 *
 * // 在组件卸载时关闭连接
 * eventSource.close();
 * ```
 */
export function subscribeToRealtimeStream(
  sessionId: number,
  callbacks: SSEStreamCallbacks
): EventSource {
  void getToken() // token reserved for future auth header use
  const eventSource = new EventSource(
    `/api/v1/live/realtime-panel/stream/${sessionId}`,
    { withCredentials: false }
  )

  // 连接建立
  eventSource.addEventListener('connected', () => {
    callbacks.onConnected?.()
  })

  // 实时数据更新事件
  eventSource.addEventListener('data-update', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data) as SSEDataUpdateEvent
      callbacks.onDataUpdate?.(data)
    } catch (e) {
      console.error('解析数据更新事件失败:', e)
      callbacks.onError?.(new Error('数据解析失败'))
    }
  })

  // 话术变化事件
  eventSource.addEventListener('slot-change', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data) as SSESlotChangeEvent
      callbacks.onSlotChange?.(data)
    } catch (e) {
      console.error('解析话术变化事件失败:', e)
      callbacks.onError?.(new Error('话术数据解析失败'))
    }
  })

  // 话术完成事件
  eventSource.addEventListener('slot-completed', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data) as SSESlotCompletedEvent
      callbacks.onSlotCompleted?.(data)
    } catch (e) {
      console.error('解析完成事件失败:', e)
      callbacks.onError?.(new Error('完成事件解析失败'))
    }
  })

  // 错误处理
  eventSource.onerror = () => {
    console.error('SSE 连接异常')
    callbacks.onError?.(new Error('SSE 连接异常'))
    // 不自动重连，由调用方决定重连策略
  }

  return eventSource
}

/**
 * 手动连接重试封装
 * 用于实现自动重连逻辑
 *
 * @param sessionId 直播场次 ID
 * @param callbacks 各类事件的回调函数
 * @param maxRetries 最大重试次数（默认 3）
 * @param retryDelayMs 重试延迟（默认 1000ms）
 * @returns 连接管理对象
 */
export function subscribeWithRetry(
  sessionId: number,
  callbacks: SSEStreamCallbacks,
  maxRetries = 3,
  retryDelayMs = 1000
) {
  let retryCount = 0
  let eventSource: EventSource | null = null
  let isManuallyClosed = false

  const connect = () => {
    if (isManuallyClosed) return

    eventSource = subscribeToRealtimeStream(sessionId, {
      ...callbacks,
      onError: (error) => {
        callbacks.onError?.(error)

        if (retryCount < maxRetries) {
          retryCount++
          console.log(`SSE 连接失败，${retryDelayMs}ms 后重试 (${retryCount}/${maxRetries})`)
          setTimeout(connect, retryDelayMs)
        } else {
          console.error('SSE 连接失败，已达到最大重试次数')
        }
      }
    })
  }

  const close = () => {
    isManuallyClosed = true
    if (eventSource) {
      eventSource.close()
    }
  }

  connect()

  return {
    close,
    reconnect: () => {
      isManuallyClosed = false
      retryCount = 0
      connect()
    }
  }
}
