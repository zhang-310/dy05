/**
 * W-08: usePerformanceMonitoring Hook
 * 管理性能监控数据和状态
 */

import { useState, useCallback, useEffect } from 'react'
import type {
  PerformanceMetricsVO,
  AlertHistoryVO,
  AlertRuleVO,
  HealthCheckStatusVO,
} from '@/types/performance'
import {
  getPerformanceMetrics,
  searchPerformanceMetrics,
  searchAlertHistory,
  searchAlertRules,
  getHealthCheckStatus,
  getAlertSummary,
} from '@/api/performance'

export interface UsePerformanceMonitoringOptions {
  autoRefresh?: boolean
  refreshInterval?: number
  timeRange?: 'realtime' | '1h' | '24h' | '7d'
}

/**
 * 性能监控 Hook
 */
export function usePerformanceMonitoring(options: UsePerformanceMonitoringOptions = {}) {
  const { autoRefresh = true, refreshInterval = 5000, timeRange = '24h' } = options

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 数据状态
  const [currentMetrics, setCurrentMetrics] = useState<PerformanceMetricsVO | null>(null)
  const [metricsHistory, setMetricsHistory] = useState<PerformanceMetricsVO[]>([])
  const [alerts, setAlerts] = useState<AlertHistoryVO[]>([])
  const [rules, setRules] = useState<AlertRuleVO[]>([])
  const [healthStatus, setHealthStatus] = useState<HealthCheckStatusVO | null>(null)
  const [alertSummary, setAlertSummary] = useState<any>(null)

  // 分页状态
  const [alertPage, setAlertPage] = useState(0)
  const [rulePage, setRulePage] = useState(0)

  /**
   * 获取起始时间
   */
  const getStartDate = useCallback(() => {
    const now = new Date()
    switch (timeRange) {
      case 'realtime':
        return new Date(now.getTime() - 60 * 60 * 1000).toISOString()
      case '1h':
        return new Date(now.getTime() - 60 * 60 * 1000).toISOString()
      case '24h':
        return new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString()
      case '7d':
        return new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString()
      default:
        return new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString()
    }
  }, [timeRange])

  /**
   * 获取聚合间隔
   */
  const getInterval = useCallback(
    (): 'minute' | 'hour' | 'day' => {
      switch (timeRange) {
        case 'realtime':
          return 'minute'
        case '1h':
          return 'minute'
        case '24h':
          return 'hour'
        case '7d':
          return 'day'
        default:
          return 'hour'
      }
    },
    [timeRange]
  )

  /**
   * 加载当前指标
   */
  const loadCurrentMetrics = useCallback(async () => {
    try {
      const result = await getPerformanceMetrics()
      setCurrentMetrics(result)
    } catch (err) {
      console.error('Failed to load current metrics:', err)
    }
  }, [])

  /**
   * 加载指标历史
   */
  const loadMetricsHistory = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await searchPerformanceMetrics({
        startTime: getStartDate(),
        endTime: new Date().toISOString(),
        interval: getInterval(),
      })
      setMetricsHistory(result.list || [])
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载失败'
      setError(message)
      console.error('Failed to load metrics history:', err)
    } finally {
      setLoading(false)
    }
  }, [getStartDate, getInterval])

  /**
   * 加载告警历史
   */
  const loadAlerts = useCallback(async (page = 0, rows = 100) => {
    try {
      setLoading(true)
      setError(null)
      const result = await searchAlertHistory({
        page,
        rows,
      })
      setAlerts(result.list || [])
      setAlertPage(page)
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载失败'
      setError(message)
      console.error('Failed to load alerts:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  /**
   * 加载告警规则
   */
  const loadRules = useCallback(async (page = 0, rows = 100) => {
    try {
      setLoading(true)
      setError(null)
      const result = await searchAlertRules({
        page,
        rows,
      })
      setRules(result.list || [])
      setRulePage(page)
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载失败'
      setError(message)
      console.error('Failed to load rules:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  /**
   * 加载健康检查状态
   */
  const loadHealthStatus = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await getHealthCheckStatus()
      setHealthStatus(result)
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载失败'
      setError(message)
      console.error('Failed to load health status:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  /**
   * 加载告警统计
   */
  const loadAlertSummary = useCallback(async () => {
    try {
      const result = await getAlertSummary()
      setAlertSummary(result)
    } catch (err) {
      console.error('Failed to load alert summary:', err)
    }
  }, [])

  /**
   * 加载全部数据
   */
  const loadAll = useCallback(async () => {
    await Promise.all([
      loadCurrentMetrics(),
      loadMetricsHistory(),
      loadAlerts(),
      loadRules(),
      loadHealthStatus(),
      loadAlertSummary(),
    ])
  }, [loadCurrentMetrics, loadMetricsHistory, loadAlerts, loadRules, loadHealthStatus, loadAlertSummary])

  /**
   * 刷新数据
   */
  const refresh = useCallback(async () => {
    await Promise.all([
      loadCurrentMetrics(),
      loadMetricsHistory(),
      loadAlerts(alertPage),
      loadRules(rulePage),
      loadHealthStatus(),
      loadAlertSummary(),
    ])
  }, [alertPage, rulePage, loadCurrentMetrics, loadMetricsHistory, loadAlerts, loadRules, loadHealthStatus, loadAlertSummary])

  /**
   * 自动刷新效果
   */
  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(() => {
        loadCurrentMetrics()
        if (timeRange === 'realtime') {
          loadMetricsHistory()
        }
        loadAlertSummary()
      }, refreshInterval)

      return () => clearInterval(interval)
    }
  }, [autoRefresh, refreshInterval, timeRange, loadCurrentMetrics, loadMetricsHistory, loadAlertSummary])

  return {
    // 状态
    loading,
    error,

    // 数据
    currentMetrics,
    metricsHistory,
    alerts,
    rules,
    healthStatus,
    alertSummary,

    // 分页
    alertPage,
    rulePage,

    // 操作
    loadCurrentMetrics,
    loadMetricsHistory,
    loadAlerts,
    loadRules,
    loadHealthStatus,
    loadAlertSummary,
    loadAll,
    refresh,

    // 分页操作
    setAlertPage,
    setRulePage,
  }
}
