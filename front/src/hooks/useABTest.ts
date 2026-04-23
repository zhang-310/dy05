import { useState, useEffect, useCallback } from 'react'
import { request } from '@/utils/request'

export interface ABTestEvent {
  experimentId: string
  variant: string
  eventName: string
  data?: Record<string, any>
  timestamp: number
}

export interface UseABTestOptions<V extends string> {
  experimentId: string
  variants: Record<V, string>
  userId?: string
}

export interface UseABTestResult<V extends string> {
  variant: V | null
  isLoading: boolean
  logEvent: (eventName: string, data?: Record<string, any>) => void
}

/**
 * useABTest Hook - A/B 测试逻辑
 *
 * 使用示例：
 * ```tsx
 * const { variant, logEvent } = useABTest('live_room_redesign', {
 *   a: '原始设计',
 *   b: '新设计'
 * })
 *
 * if (variant === 'a') {
 *   return <OriginalLayout onAddCart={() => logEvent('add_to_cart')} />
 * } else {
 *   return <NewLayout onAddCart={() => logEvent('add_to_cart')} />
 * }
 * ```
 */
export function useABTest<V extends string = string>({
  experimentId,
  variants,
  userId,
}: UseABTestOptions<V>): UseABTestResult<V> {
  const [variant, setVariant] = useState<V | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // 从 localStorage 获取本地 userId
  const localUserId = userId || (typeof window !== 'undefined' ? localStorage.getItem('ab_test_user_id') : null)

  // 初始化用户 ID
  useEffect(() => {
    if (!localUserId && typeof window !== 'undefined') {
      const newId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      localStorage.setItem('ab_test_user_id', newId)
    }
  }, [])

  // 从后端获取用户分配的变体
  useEffect(() => {
    const fetchVariant = async () => {
      try {
        const finalUserId = localUserId || localStorage.getItem('ab_test_user_id')
        if (!finalUserId) return

        const response = await request.post<{ variant: V }>('/api/v1/analytics/ab-test/variant', {
          experimentId,
          userId: finalUserId,
        })

        if (response?.variant) {
          setVariant(response.variant)
          // 记录分配事件
          logEvent('ab_test_assigned')
        }
      } catch (error) {
        console.error('Failed to fetch AB test variant:', error)
        // Fallback: 本地随机分配
        const variantKeys = Object.keys(variants) as V[]
        const fallbackVariant = variantKeys[Math.floor(Math.random() * variantKeys.length)]
        setVariant(fallbackVariant)
      } finally {
        setIsLoading(false)
      }
    }

    fetchVariant()
  }, [experimentId])

  // 记录事件
  const logEvent = useCallback(
    (eventName: string, data?: Record<string, any>) => {
      if (!variant) return

      const event: ABTestEvent = {
        experimentId,
        variant,
        eventName,
        data,
        timestamp: Date.now(),
      }

      // 本地存储事件
      const events = getLocalEvents(experimentId)
      events.push(event)
      setLocalEvents(experimentId, events)

      // 批量上报（防抖）
      scheduleBatchUpload(experimentId)
    },
    [experimentId, variant]
  )

  return { variant: variant as V | null, isLoading, logEvent }
}

// ==================== 事件本地存储 ====================

const EVENTS_STORAGE_KEY = (experimentId: string) => `ab_test_events_${experimentId}`
const uploadTimeoutMap = new Map<string, ReturnType<typeof setTimeout>>()

function getLocalEvents(experimentId: string): ABTestEvent[] {
  if (typeof window === 'undefined') return []
  const data = localStorage.getItem(EVENTS_STORAGE_KEY(experimentId))
  return data ? JSON.parse(data) : []
}

function setLocalEvents(experimentId: string, events: ABTestEvent[]): void {
  if (typeof window === 'undefined') return
  localStorage.setItem(EVENTS_STORAGE_KEY(experimentId), JSON.stringify(events))
}

function scheduleBatchUpload(experimentId: string): void {
  if (typeof window === 'undefined') return

  // 清除之前的定时器
  const previousTimeout = uploadTimeoutMap.get(experimentId)
  if (previousTimeout) clearTimeout(previousTimeout)

  // 设置新的上报定时器（30秒批量上报）
  const newTimeout = setTimeout(() => {
    uploadEvents(experimentId)
  }, 30000)

  uploadTimeoutMap.set(experimentId, newTimeout)
}

async function uploadEvents(experimentId: string): Promise<void> {
  if (typeof window === 'undefined') return

  const events = getLocalEvents(experimentId)
  if (events.length === 0) return

  try {
    await request.post('/api/v1/analytics/ab-test-events', {
      experimentId,
      events,
    })
    // 上报成功，清除本地存储
    localStorage.removeItem(EVENTS_STORAGE_KEY(experimentId))
  } catch (error) {
    console.error('Failed to upload AB test events:', error)
    // 失败时保留本地存储，稍后重试
  }
}

// 页面卸载时上报剩余事件
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    // 上报所有实验的事件
    const keys = Object.keys(localStorage)
    keys.forEach((key) => {
      if (key.startsWith('ab_test_events_')) {
        const experimentId = key.replace('ab_test_events_', '')
        uploadEvents(experimentId).catch(console.error)
      }
    })
  })
}
