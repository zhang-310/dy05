/**
 * Monitoring API Client (W-10)
 * 监控、告警、日志、健康检查 API 接口
 */

import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  RealtimeMetrics,
  AlertRule,
  AnomalyAlert,
  LogEntry,
  SystemHealthStatus,
  PerformanceTrendData,
  DashboardData,
  AlertRuleSearchVO,
  AlertSearchVO,
  LogSearchVO,
  MetricsTimeRange,
  MonitoringStatistics,
} from '@/types/monitoring'

// ─── 实时指标 ───────────────────────────────────────────────────────

/**
 * 获取实时指标
 */
export function getRealtimeMetrics() {
  return request.post<RealtimeMetrics>('/monitoring/metrics/realtime', {})
}

/**
 * 获取历史指标
 */
export function getHistoricalMetrics(
  metricName: string,
  startTime: number,
  endTime: number,
  step?: number
) {
  return request.post<PerformanceTrendData>('/monitoring/metrics/historical', {
    metricName,
    startTime,
    endTime,
    step,
  })
}

/**
 * 获取性能趋势
 */
export function getPerformanceTrend(
  metricName: string,
  timeRange: MetricsTimeRange = 'hour',
  dataPoints?: number
) {
  return request.post<PerformanceTrendData>('/monitoring/metrics/trend', {
    metricName,
    timeRange,
    dataPoints,
  })
}

// ─── 告警规则 ───────────────────────────────────────────────────────

/**
 * 查询告警规则列表
 */
export function searchAlertRules(data: AlertRuleSearchVO) {
  return request.post<PageResult<AlertRule>>('/monitoring/alert-rules/search', data)
}

/**
 * 获取告警规则详情
 */
export function getAlertRuleDetail(ruleId: number) {
  return request.post<AlertRule>('/monitoring/alert-rules/detail', { ruleId })
}

/**
 * 创建告警规则
 */
export function createAlertRule(data: AlertRule) {
  return request.post<AlertRule>('/monitoring/alert-rules/create', data)
}

/**
 * 更新告警规则
 */
export function updateAlertRule(data: AlertRule) {
  return request.post<void>('/monitoring/alert-rules/update', data)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(ruleId: number) {
  return request.post<void>('/monitoring/alert-rules/delete', { ruleId })
}

/**
 * 启用告警规则
 */
export function enableAlertRule(ruleId: number) {
  return request.post<void>('/monitoring/alert-rules/enable', { ruleId })
}

/**
 * 禁用告警规则
 */
export function disableAlertRule(ruleId: number) {
  return request.post<void>('/monitoring/alert-rules/disable', { ruleId })
}

// ─── 异常告警 ───────────────────────────────────────────────────────

/**
 * 查询告警列表
 */
export function searchAlerts(data: AlertSearchVO) {
  return request.post<PageResult<AnomalyAlert>>('/monitoring/alerts/search', data)
}

/**
 * 获取活跃告警
 */
export function getActiveAlerts(severity?: string, limit: number = 100) {
  return request.post<{ alerts: AnomalyAlert[]; total: number }>(
    '/monitoring/alerts/active',
    { severity, limit }
  )
}

/**
 * 获取告警详情
 */
export function getAlertDetail(alertId: number) {
  return request.post<AnomalyAlert>('/monitoring/alerts/detail', { alertId })
}

/**
 * 确认告警
 */
export function acknowledgeAlert(alertId: number, notes?: string) {
  return request.post<void>('/monitoring/alerts/acknowledge', { alertId, notes })
}

/**
 * 解决告警
 */
export function resolveAlert(alertId: number, resolution?: string) {
  return request.post<void>('/monitoring/alerts/resolve', { alertId, resolution })
}

/**
 * 关闭告警
 */
export function closeAlert(alertId: number) {
  return request.post<void>('/monitoring/alerts/close', { alertId })
}

/**
 * 批量确认告警
 */
export function acknowledgeAlerts(alertIds: number[]) {
  return request.post<void>('/monitoring/alerts/batch-acknowledge', { alertIds })
}

/**
 * 获取告警统计
 */
export function getAlertStatistics(startDate?: string, endDate?: string) {
  return request.post<Record<string, number>>('/monitoring/alerts/statistics', {
    startDate,
    endDate,
  })
}

// ─── 日志查看 ───────────────────────────────────────────────────────

/**
 * 查询日志
 */
export function searchLogs(data: LogSearchVO) {
  return request.post<PageResult<LogEntry>>('/monitoring/logs/search', data)
}

/**
 * 获取日志详情
 */
export function getLogDetail(logId: number) {
  return request.post<LogEntry>('/monitoring/logs/detail', { logId })
}

/**
 * 导出日志
 */
export function exportLogs(data: LogSearchVO) {
  return request.post<Blob>('/monitoring/logs/export', data, {
    responseType: 'blob',
  })
}

/**
 * 获取日志统计
 */
export function getLogStatistics(startDate?: string, endDate?: string) {
  return request.post<Record<string, unknown>>('/monitoring/logs/statistics', {
    startDate,
    endDate,
  })
}

/**
 * 获取错误日志聚合
 */
export function getErrorAggregation(limit: number = 20) {
  return request.post<Array<{ errorType: string; count: number; lastOccurrence: string }>>(
    '/monitoring/logs/error-aggregation',
    { limit }
  )
}

// ─── 系统健康检查 ───────────────────────────────────────────────────────

/**
 * 获取系统健康状态
 */
export function getHealthStatus() {
  return request.post<SystemHealthStatus>('/monitoring/health/status', {})
}

/**
 * 获取组件健康状态
 */
export function getComponentHealth(componentName: string) {
  return request.post<Record<string, unknown>>(
    '/monitoring/health/component',
    { componentName }
  )
}

/**
 * 获取数据库连接池状态
 */
export function getDatabasePoolStatus() {
  return request.post<Record<string, unknown>>('/monitoring/health/database', {})
}

/**
 * 获取缓存状态
 */
export function getCacheStatus() {
  return request.post<Record<string, unknown>>('/monitoring/health/cache', {})
}

// ─── 仪表板 ───────────────────────────────────────────────────────

/**
 * 获取仪表板数据
 */
export function getDashboardData(period: MetricsTimeRange = 'hour') {
  return request.post<DashboardData>('/monitoring/dashboard/data', { period })
}

/**
 * 获取监控统计
 */
export function getMonitoringStatistics() {
  return request.post<MonitoringStatistics>('/monitoring/statistics', {})
}

/**
 * 获取顶部错误端点
 */
export function getTopErrorEndpoints(limit: number = 10) {
  return request.post<Array<{ endpoint: string; errorCount: number; errorRate: number }>>(
    '/monitoring/top-error-endpoints',
    { limit }
  )
}

/**
 * 获取顶部慢查询端点
 */
export function getTopSlowEndpoints(limit: number = 10) {
  return request.post<Array<{ endpoint: string; averageResponseTime: number; requestCount: number }>>('/monitoring/top-slow-endpoints', { limit })
}

// ─── WebSocket/SSE 流 ───────────────────────────────────────────────────────

/**
 * 建立 EventSource 连接以接收实时指标
 * 使用 EventSource 处理服务器推送事件
 * 事件类型：metrics, alert, health, log
 */
export function subscribeToRealtimeMetrics(): EventSource {
  return new EventSource('/api/v1/monitoring/stream/realtime')
}

/**
 * 建立 EventSource 连接以接收告警通知
 */
export function subscribeToAlerts(): EventSource {
  return new EventSource('/api/v1/monitoring/stream/alerts')
}

// ─── 批量操作 ───────────────────────────────────────────────────────

/**
 * 导出告警规则
 */
export function exportAlertRules(data: AlertRuleSearchVO) {
  return request.post<Blob>('/monitoring/alert-rules/export', data, {
    responseType: 'blob',
  })
}

/**
 * 导入告警规则
 */
export function importAlertRules(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ importedCount: number; failedCount: number }>(
    '/monitoring/alert-rules/import',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

/**
 * 清理旧日志
 */
export function cleanupOldLogs(daysOld: number) {
  return request.post<{ deletedCount: number }>('/monitoring/logs/cleanup', {
    daysOld,
  })
}

/**
 * 获取系统资源使用情况
 */
export function getSystemResources() {
  return request.post<Record<string, unknown>>('/monitoring/system/resources', {})
}
