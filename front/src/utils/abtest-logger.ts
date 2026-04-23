/**
 * ABTestLogger - A/B 测试事件上报工具
 *
 * 功能：
 * - 本地存储事件（IndexedDB/localStorage）
 * - 批量上报事件到服务器
 * - 自动防抖和重试机制
 * - 页面卸载时强制上报
 */

export interface ABTestEvent {
  experimentId: string
  variant: string
  eventName: string
  data?: Record<string, any>
  timestamp: number
  userId?: string
}

export interface ABTestLogger {
  logEvent(event: ABTestEvent): void
  uploadEvents(experimentId: string): Promise<void>
  getLocalEvents(experimentId: string): ABTestEvent[]
  clearEvents(experimentId: string): void
}

class ABTestLoggerImpl implements ABTestLogger {
  private uploadTimeouts: Map<string, ReturnType<typeof setTimeout>> = new Map()
  private readonly BATCH_UPLOAD_DELAY = 30000 // 30秒
  private readonly MAX_LOCAL_EVENTS = 1000

  /**
   * 记录 A/B 测试事件
   */
  logEvent(event: ABTestEvent): void {
    if (typeof window === 'undefined') return

    // 本地存储
    const events = this.getLocalEvents(event.experimentId)
    events.push(event)

    // 限制本地存储数量
    if (events.length > this.MAX_LOCAL_EVENTS) {
      events.shift()
    }

    this.setLocalEvents(event.experimentId, events)

    // 调度批量上报
    this.scheduleBatchUpload(event.experimentId)
  }

  /**
   * 上报事件到服务器
   */
  async uploadEvents(experimentId: string): Promise<void> {
    if (typeof window === 'undefined') return

    const events = this.getLocalEvents(experimentId)
    if (events.length === 0) return

    try {
      const response = await fetch('/api/v1/analytics/ab-test-events', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          experimentId,
          events,
          timestamp: Date.now(),
        }),
      })

      if (response.ok) {
        // 上报成功，清除本地存储
        this.clearEvents(experimentId)
      } else {
        console.warn(`Failed to upload events: ${response.status}`)
      }
    } catch (error) {
      console.error('Failed to upload AB test events:', error)
      // 失败时保留本地存储，稍后重试
    }
  }

  /**
   * 获取本地存储的事件
   */
  getLocalEvents(experimentId: string): ABTestEvent[] {
    if (typeof window === 'undefined') return []

    try {
      const key = this.getStorageKey(experimentId)
      const data = localStorage.getItem(key)
      return data ? JSON.parse(data) : []
    } catch (error) {
      console.error('Failed to parse local events:', error)
      return []
    }
  }

  /**
   * 保存事件到本地存储
   */
  private setLocalEvents(experimentId: string, events: ABTestEvent[]): void {
    if (typeof window === 'undefined') return

    try {
      const key = this.getStorageKey(experimentId)
      localStorage.setItem(key, JSON.stringify(events))
    } catch (error) {
      console.error('Failed to save local events:', error)
    }
  }

  /**
   * 清除本地存储的事件
   */
  clearEvents(experimentId: string): void {
    if (typeof window === 'undefined') return

    try {
      const key = this.getStorageKey(experimentId)
      localStorage.removeItem(key)
    } catch (error) {
      console.error('Failed to clear local events:', error)
    }
  }

  /**
   * 调度批量上报
   */
  private scheduleBatchUpload(experimentId: string): void {
    // 清除旧的定时器
    const previousTimeout = this.uploadTimeouts.get(experimentId)
    if (previousTimeout) {
      clearTimeout(previousTimeout)
    }

    // 设置新的定时器
    const newTimeout = setTimeout(() => {
      this.uploadEvents(experimentId).catch(console.error)
      this.uploadTimeouts.delete(experimentId)
    }, this.BATCH_UPLOAD_DELAY)

    this.uploadTimeouts.set(experimentId, newTimeout)
  }

  /**
   * 获取存储 key
   */
  private getStorageKey(experimentId: string): string {
    return `ab_test_events_${experimentId}`
  }
}

// 单例实例
export const abTestLogger = new ABTestLoggerImpl()

// 页面卸载时上报所有待发送事件
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    const keys = Object.keys(localStorage)
    keys.forEach((key) => {
      if (key.startsWith('ab_test_events_')) {
        const experimentId = key.replace('ab_test_events_', '')
        abTestLogger.uploadEvents(experimentId).catch(console.error)
      }
    })
  })
}
