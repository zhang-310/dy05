/**
 * W-08: Performance Monitoring API Client
 * 性能监控与告警相关 API 调用
 */

import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  PerformanceMetricsVO,
  QueryAnalysisVO,
  CacheStatisticsVO,
  AlertRuleVO,
  AlertHistoryVO,
  HealthCheckStatusVO,
  PerformanceMetricsSearchVO,
  AlertRuleSearchVO,
  AlertHistorySearchVO,
  AlertRuleSaveVO,
  AlertAcknowledgeVO,
  PerformanceExportVO,
} from '@/types/performance'

/**
 * 获取实时性能指标
 */
export function getPerformanceMetrics(data?: { metricType?: string }) {
  return request.post<PerformanceMetricsVO>('/system/performance/metrics/current', data || {})
}

/**
 * 获取性能指标历史数据（用于绘图）
 */
export function searchPerformanceMetrics(data: PerformanceMetricsSearchVO) {
  return request.post<PageResult<PerformanceMetricsVO>>('/system/performance/metrics/search', data || {})
}

/**
 * 获取 API 性能时间序列数据
 */
export function getApiPerformanceTimeSeries(data: {
  startTime: string
  endTime: string
  interval?: 'minute' | 'hour' | 'day'
  metrics?: string[] // ['latency', 'error_rate', 'throughput', 'p95', 'p99']
}) {
  return request.post<{
    timestamp: string
    latency: number
    errorRate: number
    throughput: number
    p95: number
    p99: number
  }[]>('/system/performance/api/timeseries', data)
}

/**
 * 获取查询分析报告
 */
export function getQueryAnalysis(data?: { limit?: number; kbId?: number }) {
  return request.post<QueryAnalysisVO>('/system/performance/query/analysis', data || {})
}

/**
 * 获取慢查询列表
 */
export function getSlowQueries(data?: { limit?: number; minDurationMs?: number }) {
  return request.post<Array<{
    id: string
    sql: string
    executionTimeMs: number
    count: number
    affectedRows: number
    lastExecutedAt: string
  }>>('/system/performance/query/slow', data || {})
}

/**
 * 获取 N+1 问题列表
 */
export function getNPlusOneIssues(data?: { limit?: number; severity?: string }) {
  return request.post<Array<{
    id: string
    description: string
    apiEndpoint: string
    estimatedExtraQueries: number
    affectedUsers: number
    severity: 'low' | 'medium' | 'high'
    suggestion: string
  }>>('/system/performance/query/n-plus-one', data || {})
}

/**
 * 获取索引建议
 */
export function getIndexSuggestions(data?: { limit?: number; priority?: string }) {
  return request.post<Array<{
    tableName: string
    columnNames: string[]
    estimatedImpactPercent: number
    estimatedQueryTimeReductionMs: number
    priority: 'low' | 'medium' | 'high'
    createSql?: string
  }>>('/system/performance/index/suggestions', data || {})
}

/**
 * 获取缓存统计
 */
export function getCacheStatistics(data?: { cacheType?: 'redis' | 'http' | 'all' }) {
  return request.post<CacheStatisticsVO>('/system/performance/cache/statistics', data || {})
}

/**
 * 获取缓存热 key 分析
 */
export function getCacheHotKeys(data?: { limit?: number; topN?: number }) {
  return request.post<Array<{
    key: string
    accessCount: number
    accessRate: number
    sizeMB: number
    ttlSeconds: number
  }>>('/system/performance/cache/hot-keys', data || {})
}

/**
 * 获取缓存时间序列数据（命中率趋势）
 */
export function getCacheHitRateTrend(data: {
  startTime: string
  endTime: string
  interval?: 'minute' | 'hour' | 'day'
}) {
  return request.post<Array<{
    timestamp: string
    hitRate: number
    hitCount: number
    missCount: number
    evictionCount: number
  }>>('/system/performance/cache/trend', data)
}

/**
 * 清除特定缓存 key
 */
export function clearCacheKey(keys: string[]) {
  return request.post<{ clearedCount: number }>('/system/performance/cache/clear', { keys })
}

/**
 * 重建缓存索引
 */
export function rebuildCacheIndex(data?: { types?: string[] }) {
  return request.post<{ jobId: string }>('/system/performance/cache/rebuild', data || {})
}

/**
 * 获取告警规则列表
 */
export function searchAlertRules(data: AlertRuleSearchVO) {
  return request.post<PageResult<AlertRuleVO>>('/system/alert/rules/search', data || {})
}

/**
 * 获取单个告警规则详情
 */
export function getAlertRule(id: number) {
  return request.post<AlertRuleVO>(`/system/alert/rules/${id}`, {})
}

/**
 * 创建或更新告警规则
 */
export function saveAlertRule(data: AlertRuleSaveVO) {
  return request.post<AlertRuleVO>('/system/alert/rules/save', data)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(id: number) {
  return request.delete<void>(`/system/alert/rules/${id}`)
}

/**
 * 克隆告警规则
 */
export function cloneAlertRule(id: number, data?: { name?: string }) {
  return request.post<AlertRuleVO>(`/system/alert/rules/${id}/clone`, data || {})
}

/**
 * 测试告警规则
 */
export function testAlertRule(data: AlertRuleSaveVO) {
  return request.post<{ success: boolean; message: string; actionsExecuted: string[] }>('/system/alert/rules/test', data)
}

/**
 * 获取告警历史列表
 */
export function searchAlertHistory(data: AlertHistorySearchVO) {
  return request.post<PageResult<AlertHistoryVO>>('/system/alert/history/search', data || {})
}

/**
 * 获取最近的告警
 */
export function getRecentAlerts(limit = 30, severity?: string) {
  return request.post<AlertHistoryVO[]>('/system/alert/history/recent', { limit, severity })
}

/**
 * 确认告警
 */
export function acknowledgeAlert(data: AlertAcknowledgeVO) {
  return request.post<void>('/system/alert/history/acknowledge', data)
}

/**
 * 批量确认告警
 */
export function acknowledgeAlerts(ids: number[]) {
  return request.post<void>('/system/alert/history/acknowledge-batch', { ids })
}

/**
 * 解决告警
 */
export function resolveAlert(id: number, data?: { message?: string }) {
  return request.post<void>(`/system/alert/history/${id}/resolve`, data || {})
}

/**
 * 获取告警统计摘要
 */
export function getAlertSummary(data?: { period?: 'hour' | 'day' | 'week' }) {
  return request.post<{
    totalAlerts: number
    bySeverity: Record<string, number>
    byStatus: Record<string, number>
    unacknowledged: number
    averageResolutionTimeMinutes: number
    topRules: Array<{ name: string; count: number }>
  }>('/system/alert/summary', data || {})
}

/**
 * 获取健康检查状态
 */
export function getHealthCheckStatus() {
  return request.post<HealthCheckStatusVO>('/system/health/check', {})
}

/**
 * 获取基础设施详情
 */
export function getInfrastructureDetail() {
  return request.post<{
    database: {
      status: 'healthy' | 'warning' | 'critical'
      version: string
      activeConnections: number
      maxConnections: number
      uptime: number
      replicationLag?: number
    }
    redis: {
      status: 'healthy' | 'warning' | 'critical'
      version: string
      memory_usage: number
      memory_max: number
      connected_clients: number
      commands_per_sec: number
    }
    elasticsearch: {
      status: 'healthy' | 'warning' | 'critical'
      version: string
      active_shards: number
      node_count: number
      indices_count: number
      docs_count: number
    }
    rabbitmq: {
      status: 'healthy' | 'warning' | 'critical'
      version: string
      queues_count: number
      consumers_count: number
      messages_ready: number
      messages_unacked: number
    }
    milvus?: {
      status: 'healthy' | 'warning' | 'critical'
      version: string
      collections: number
      entities: number
    }
  }>('/system/infra/detail', {})
}

/**
 * 导出性能指标报告
 */
export function exportPerformanceMetrics(data: PerformanceExportVO) {
  return request.post<Blob>('/system/performance/export', data, { responseType: 'blob' })
}

/**
 * 导出告警历史报告
 */
export function exportAlertHistory(data: {
  startTime: string
  endTime: string
  format: 'csv' | 'excel' | 'pdf'
}) {
  return request.post<Blob>('/system/alert/history/export', data, { responseType: 'blob' })
}

/**
 * 获取性能基准数据（用于对比）
 */
export function getPerformanceBenchmark(data?: { period?: string }) {
  return request.post<{
    apiLatencyP95Ms: number
    apiLatencyP99Ms: number
    errorRate: number
    cpuUsagePercent: number
    memoryUsagePercent: number
    cacheHitRate: number
    databaseQPS: number
  }>('/system/performance/benchmark', data || {})
}

/**
 * 启用/禁用告警
 */
export function toggleAlertRule(id: number, enabled: boolean) {
  return request.post<void>(`/system/alert/rules/${id}/toggle`, { enabled })
}

/**
 * 批量启用/禁用告警
 */
export function toggleAlertRules(ids: number[], enabled: boolean) {
  return request.post<void>('/system/alert/rules/toggle-batch', { ids, enabled })
}

/**
 * 手动触发告警规则测试
 */
export function triggerAlertRuleTest(id: number) {
  return request.post<{ success: boolean; message: string; actionsTriggered: string[] }>(`/system/alert/rules/${id}/trigger-test`, {})
}
