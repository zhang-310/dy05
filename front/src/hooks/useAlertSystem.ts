/**
 * useAlertSystem Hook (W-10)
 * 告警规则管理、告警操作、通知设置
 */

import { useState, useCallback, useRef, useEffect } from 'react'
import {
  searchAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  enableAlertRule,
  disableAlertRule,
  searchAlerts,
  acknowledgeAlert,
  resolveAlert,
  getAlertStatistics,
} from '@/api/monitoring'
import type {
  AlertRule,
  AnomalyAlert,
  AlertRuleSearchVO,
  AlertSearchVO,
} from '@/types/monitoring'
import { AlertStatus } from '@/types/monitoring'
import type { PageResult } from '@/types/common'

interface UseAlertSystemState {
  alertRules: AlertRule[]
  alerts: AnomalyAlert[]
  selectedRule: AlertRule | null
  selectedAlert: AnomalyAlert | null
  isLoading: boolean
  isSaving: boolean
  error: string | null
  totalRules: number
  totalAlerts: number
  pageNum: number
  pageSize: number
  alertStatistics: Record<string, number> | null
}

interface UseAlertSystemOptions {
  initialPageSize?: number
  autoLoadStatistics?: boolean
}

/**
 * Hook: 告警系统管理
 */
export function useAlertSystem(options: UseAlertSystemOptions = {}) {
  const { initialPageSize = 20, autoLoadStatistics = true } = options

  const [state, setState] = useState<UseAlertSystemState>({
    alertRules: [],
    alerts: [],
    selectedRule: null,
    selectedAlert: null,
    isLoading: false,
    isSaving: false,
    error: null,
    totalRules: 0,
    totalAlerts: 0,
    pageNum: 0,
    pageSize: initialPageSize,
    alertStatistics: null,
  })

  const abortControllerRef = useRef<AbortController>()

  /**
   * 加载告警规则
   */
  const loadAlertRules = useCallback(async (params?: AlertRuleSearchVO) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    try {
      const searchParams = params || {
        page: 0,
        rows: state.pageSize,
      }

      const result = (await searchAlertRules(searchParams)) as PageResult<AlertRule>

      setState((prev) => ({
        ...prev,
        alertRules: result.list,
        totalRules: result.total,
        isLoading: false,
      }))
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to load alert rules'
      setState((prev) => ({ ...prev, error: errorMsg, isLoading: false }))
    }
  }, [state.pageSize])

  /**
   * 加载告警列表
   */
  const loadAlerts = useCallback(async (params?: AlertSearchVO) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    try {
      const searchParams = params || {
        page: 0,
        rows: state.pageSize,
      }

      const result = (await searchAlerts(searchParams)) as PageResult<AnomalyAlert>

      setState((prev) => ({
        ...prev,
        alerts: result.list,
        totalAlerts: result.total,
        isLoading: false,
      }))
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to load alerts'
      setState((prev) => ({ ...prev, error: errorMsg, isLoading: false }))
    }
  }, [state.pageSize])

  /**
   * 创建告警规则
   */
  const createNewAlertRule = useCallback(async (data: AlertRule) => {
    setState((prev) => ({ ...prev, isSaving: true, error: null }))

    try {
      const newRule = (await createAlertRule(data)) as AlertRule
      setState((prev) => ({
        ...prev,
        alertRules: [newRule, ...prev.alertRules],
        totalRules: prev.totalRules + 1,
        isSaving: false,
      }))
      return newRule
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to create alert rule'
      setState((prev) => ({ ...prev, error: errorMsg, isSaving: false }))
      throw err
    }
  }, [])

  /**
   * 更新告警规则
   */
  const updateExistingAlertRule = useCallback(async (data: AlertRule) => {
    setState((prev) => ({ ...prev, isSaving: true, error: null }))

    try {
      await updateAlertRule(data)
      setState((prev) => ({
        ...prev,
        alertRules: prev.alertRules.map((rule) => (rule.ruleId === data.ruleId ? data : rule)),
        selectedRule: prev.selectedRule?.ruleId === data.ruleId ? data : prev.selectedRule,
        isSaving: false,
      }))
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to update alert rule'
      setState((prev) => ({ ...prev, error: errorMsg, isSaving: false }))
      throw err
    }
  }, [])

  /**
   * 删除告警规则
   */
  const deleteExistingAlertRule = useCallback(async (ruleId: number) => {
    setState((prev) => ({ ...prev, isSaving: true, error: null }))

    try {
      await deleteAlertRule(ruleId)
      setState((prev) => ({
        ...prev,
        alertRules: prev.alertRules.filter((rule) => rule.ruleId !== ruleId),
        totalRules: prev.totalRules - 1,
        isSaving: false,
      }))
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to delete alert rule'
      setState((prev) => ({ ...prev, error: errorMsg, isSaving: false }))
      throw err
    }
  }, [])

  /**
   * 启用告警规则
   */
  const enableRule = useCallback(async (ruleId: number) => {
    try {
      await enableAlertRule(ruleId)
      setState((prev) => ({
        ...prev,
        alertRules: prev.alertRules.map((rule) =>
          rule.ruleId === ruleId ? { ...rule, enabled: true } : rule
        ),
      }))
    } catch (err) {
      console.error('Failed to enable alert rule:', err)
    }
  }, [])

  /**
   * 禁用告警规则
   */
  const disableRule = useCallback(async (ruleId: number) => {
    try {
      await disableAlertRule(ruleId)
      setState((prev) => ({
        ...prev,
        alertRules: prev.alertRules.map((rule) =>
          rule.ruleId === ruleId ? { ...rule, enabled: false } : rule
        ),
      }))
    } catch (err) {
      console.error('Failed to disable alert rule:', err)
    }
  }, [])

  /**
   * 确认告警
   */
  const acknowledgeExistingAlert = useCallback(async (alertId: number, notes?: string) => {
    try {
      await acknowledgeAlert(alertId, notes)
      setState((prev) => ({
        ...prev,
        alerts: prev.alerts.map((alert) =>
          alert.alertId === alertId
            ? { ...alert, status: AlertStatus.ACKNOWLEDGED }
            : alert
        ),
      }))
    } catch (err) {
      console.error('Failed to acknowledge alert:', err)
    }
  }, [])

  /**
   * 解决告警
   */
  const resolveExistingAlert = useCallback(async (alertId: number, resolution?: string) => {
    try {
      await resolveAlert(alertId, resolution)
      setState((prev) => ({
        ...prev,
        alerts: prev.alerts.map((alert) =>
          alert.alertId === alertId
            ? { ...alert, status: AlertStatus.RESOLVED }
            : alert
        ),
      }))
    } catch (err) {
      console.error('Failed to resolve alert:', err)
    }
  }, [])

  /**
   * 加载告警统计
   */
  const loadStatistics = useCallback(async (startDate?: string, endDate?: string) => {
    try {
      const stats = await getAlertStatistics(startDate, endDate)
      setState((prev) => ({ ...prev, alertStatistics: stats }))
      return stats
    } catch (err) {
      console.error('Failed to load alert statistics:', err)
    }
  }, [])

  /**
   * 初始化加载
   */
  useEffect(() => {
    loadAlertRules()
    loadAlerts()
    if (autoLoadStatistics) {
      loadStatistics()
    }
  }, []) // Only run once

  /**
   * 清理
   */
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
      }
    }
  }, [])

  return {
    ...state,
    loadAlertRules,
    loadAlerts,
    createNewAlertRule,
    updateExistingAlertRule,
    deleteExistingAlertRule,
    enableRule,
    disableRule,
    acknowledgeExistingAlert,
    resolveExistingAlert,
    loadStatistics,
    selectRule: (rule: AlertRule) => setState((prev) => ({ ...prev, selectedRule: rule })),
    selectAlert: (alert: AnomalyAlert) => setState((prev) => ({ ...prev, selectedAlert: alert })),
    clearError: () => setState((prev) => ({ ...prev, error: null })),
  }
}
