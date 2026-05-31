/**
 * Monitoring API Client (W-10)
 * 监控、告警、日志、健康检查 API 接口
 */

import request from '@/utils/request'
import { isRecord, normalizeArray, normalizePage, parseJsonValue, readTotal } from '@/utils/response-normalize'
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

type UnknownRecord = Record<string, unknown>

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function toOptionalNumber(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : undefined
  }
  return undefined
}

function toStringValue(value: unknown, fallback = ''): string {
  if (value == null) return fallback
  return String(value)
}

function toTimestamp(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const text = value.trim()
    if (/^-?\d+(\.\d+)?$/.test(text)) {
      const parsed = Number(text)
      return Number.isFinite(parsed) ? parsed : fallback
    }
    const parsedDate = Date.parse(text)
    return Number.isFinite(parsedDate) ? parsedDate : fallback
  }
  return fallback
}

function unwrapRecord(raw: unknown): UnknownRecord {
  const value = parseJsonValue(raw)
  if (!isRecord(value) || Array.isArray(value)) return {}
  if (isRecord(value.data) && Object.keys(value).some(key => ['data', 'list', 'records', 'items', 'rows', 'content'].includes(key))) {
    return unwrapRecord(value.data)
  }
  return value
}

function normalizeRealtimeMetrics(raw: unknown): RealtimeMetrics {
  const record = unwrapRecord(raw)
  return {
    timestamp: toOptionalNumber(record.timestamp),
    cpuUsage: toOptionalNumber(record.cpuUsage ?? record.cpu ?? record.cpuPercent),
    memoryUsage: toOptionalNumber(record.memoryUsage ?? record.memory ?? record.memoryPercent),
    diskUsage: toOptionalNumber(record.diskUsage ?? record.disk ?? record.diskPercent),
    networkIn: toOptionalNumber(record.networkIn ?? record.netIn),
    networkOut: toOptionalNumber(record.networkOut ?? record.netOut),
    activeConnections: toOptionalNumber(record.activeConnections ?? record.connections),
    requestsPerSecond: toOptionalNumber(record.requestsPerSecond ?? record.rps),
    errorRate: toOptionalNumber(record.errorRate),
    responseTime: toOptionalNumber(record.responseTime ?? record.avgResponseTime),
    queueDepth: toOptionalNumber(record.queueDepth),
    cacheHitRate: toOptionalNumber(record.cacheHitRate ?? record.redisHitRate),
    databaseConnections: toOptionalNumber(record.databaseConnections ?? record.dbConnections),
  }
}

function normalizeTrend(raw: unknown): PerformanceTrendData {
  const record = unwrapRecord(raw)
  const points = normalizeArray<UnknownRecord>(record.dataPoints ?? record.points ?? record.records ?? record.rows ?? raw)
  return {
    metricName: toStringValue(record.metricName ?? record.metric, 'unknown'),
    unit: record.unit == null ? undefined : toStringValue(record.unit),
    timeRange: toStringValue(record.timeRange ?? record.range, ''),
    dataPoints: points.map((point) => ({
      timestamp: toTimestamp(point.timestamp ?? point.time),
      value: toNumber(point.value),
      label: point.label == null ? undefined : toStringValue(point.label),
    })),
    summary: isRecord(record.summary)
      ? {
          average: toNumber(record.summary.average),
          min: toNumber(record.summary.min),
          max: toNumber(record.summary.max),
          percentile95: toNumber(record.summary.percentile95 ?? record.summary.p95),
          percentile99: toNumber(record.summary.percentile99 ?? record.summary.p99),
        }
      : undefined,
  }
}

function normalizeAlertRule(raw: unknown): AlertRule {
  const record = isRecord(raw) ? raw : {}
  return {
    ruleId: record.ruleId == null && record.id == null ? undefined : toNumber(record.ruleId ?? record.id),
    ruleName: toStringValue(record.ruleName ?? record.name),
    description: record.description == null ? undefined : toStringValue(record.description),
    metricName: toStringValue(record.metricName ?? record.metric),
    operator: toStringValue(record.operator, 'gt') as AlertRule['operator'],
    threshold: toNumber(record.threshold),
    severity: toStringValue(record.severity, 'medium') as AlertRule['severity'],
    duration: record.duration == null ? undefined : toNumber(record.duration),
    enabled: typeof record.enabled === 'boolean' ? record.enabled : toNumber(record.status, 1) === 1,
    notificationChannels: normalizeArray<string>(record.notificationChannels),
    createdAt: record.createdAt == null && record.createTime == null ? undefined : toStringValue(record.createdAt ?? record.createTime),
    updatedAt: record.updatedAt == null && record.updateTime == null ? undefined : toStringValue(record.updatedAt ?? record.updateTime),
  }
}

function normalizeAlert(raw: unknown): AnomalyAlert {
  const record = isRecord(raw) ? raw : {}
  return {
    alertId: record.alertId == null && record.id == null ? undefined : toNumber(record.alertId ?? record.id),
    alertType: toStringValue(record.alertType ?? record.type, 'metric_threshold'),
    ruleName: record.ruleName == null ? undefined : toStringValue(record.ruleName),
    severity: toStringValue(record.severity, 'medium') as AnomalyAlert['severity'],
    status: toStringValue(record.status, 'active').toLowerCase() as AnomalyAlert['status'],
    metric: record.metric == null && record.metricName == null ? undefined : toStringValue(record.metric ?? record.metricName),
    currentValue: record.currentValue == null && record.value == null ? undefined : toNumber(record.currentValue ?? record.value),
    threshold: record.threshold == null ? undefined : toNumber(record.threshold),
    message: toStringValue(record.message ?? record.ruleName, ''),
    firstDetectedAt: record.firstDetectedAt == null && record.createTime == null ? undefined : toStringValue(record.firstDetectedAt ?? record.createTime),
    lastDetectedAt: record.lastDetectedAt == null && record.triggeredAt == null ? undefined : toStringValue(record.lastDetectedAt ?? record.triggeredAt),
    acknowledgedAt: record.acknowledgedAt == null ? undefined : toStringValue(record.acknowledgedAt),
    acknowledgedBy: record.acknowledgedBy == null ? undefined : toStringValue(record.acknowledgedBy),
    resolution: record.resolution == null ? undefined : toStringValue(record.resolution),
    resolvedAt: record.resolvedAt == null ? undefined : toStringValue(record.resolvedAt),
    resolvedBy: record.resolvedBy == null ? undefined : toStringValue(record.resolvedBy),
    additionalInfo: isRecord(record.additionalInfo) ? record.additionalInfo : undefined,
  }
}

function normalizeActiveAlerts(raw: unknown): { alerts: AnomalyAlert[]; total: number } {
  const record = unwrapRecord(raw)
  const rawAlerts = Array.isArray(record.alerts) ? record.alerts : normalizeArray<unknown>(record)
  const alerts = rawAlerts.map(normalizeAlert)
  return { alerts, total: readTotal(raw, alerts.length) }
}

function normalizeLogEntry(raw: unknown): LogEntry {
  const record = isRecord(raw) ? raw : {}
  return {
    logId: record.logId == null && record.id == null ? undefined : toNumber(record.logId ?? record.id),
    timestamp: record.timestamp == null && record.createTime == null ? undefined : toStringValue(record.timestamp ?? record.createTime),
    level: toStringValue(record.level, 'INFO') as LogEntry['level'],
    logger: toStringValue(record.logger ?? record.module),
    message: toStringValue(record.message ?? record.summary),
    threadName: record.threadName == null ? undefined : toStringValue(record.threadName),
    userId: record.userId == null ? undefined : toNumber(record.userId),
    username: record.username == null ? undefined : toStringValue(record.username),
    traceId: record.traceId == null ? undefined : toStringValue(record.traceId),
    module: record.module == null ? undefined : toStringValue(record.module),
    endpoint: record.endpoint == null && record.requestUri == null ? undefined : toStringValue(record.endpoint ?? record.requestUri),
    statusCode: record.statusCode == null ? undefined : toNumber(record.statusCode),
    responseTime: record.responseTime == null && record.durationMs == null ? undefined : toNumber(record.responseTime ?? record.durationMs),
    exception: record.exception == null ? undefined : toStringValue(record.exception),
    stackTrace: record.stackTrace == null ? undefined : toStringValue(record.stackTrace),
  }
}

function normalizeHealth(raw: unknown): SystemHealthStatus {
  const record = unwrapRecord(raw)
  const rawComponents = isRecord(record.components) ? record.components : {}
  return {
    status: toStringValue(record.status, 'UNKNOWN') as SystemHealthStatus['status'],
    statusCode: record.statusCode == null ? undefined : toNumber(record.statusCode),
    message: record.message == null ? undefined : toStringValue(record.message),
    timestamp: record.timestamp == null ? undefined : toStringValue(record.timestamp),
    components: Object.fromEntries(Object.entries(rawComponents).map(([key, value]) => {
      const detail = isRecord(value) ? value : { status: value }
      return [key, {
        status: toStringValue(detail.status, 'UNKNOWN'),
        responseTime: detail.responseTime == null ? undefined : toNumber(detail.responseTime),
        lastCheck: detail.lastCheck == null ? undefined : toStringValue(detail.lastCheck),
        message: detail.message == null ? undefined : toStringValue(detail.message),
        details: isRecord(detail.details) ? detail.details : undefined,
      }]
    })) as SystemHealthStatus['components'],
  }
}

function identityRecord(raw: unknown): Record<string, unknown> {
  return unwrapRecord(raw)
}

function normalizeStats(raw: unknown): MonitoringStatistics {
  const record = unwrapRecord(raw)
  return {
    totalAlerts: toNumber(record.totalAlerts),
    activeAlerts: toNumber(record.activeAlerts),
    resolvedAlerts: toNumber(record.resolvedAlerts),
    criticalAlerts: toNumber(record.criticalAlerts),
    uptime: toNumber(record.uptime),
    averageResponseTime: toNumber(record.averageResponseTime),
    peakLoad: toNumber(record.peakLoad),
    errorRate: toNumber(record.errorRate),
  }
}

function normalizeDashboardData(raw: unknown): DashboardData {
  return unwrapRecord(raw) as unknown as DashboardData
}

// ─── 实时指标 ───────────────────────────────────────────────────────

/**
 * 获取实时指标
 */
export function getRealtimeMetrics() {
  return request.post<unknown>('/monitoring/metrics/realtime', {}).then(normalizeRealtimeMetrics)
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
  return request.post<unknown>('/monitoring/metrics/historical', {
    metricName,
    startTime,
    endTime,
    step,
  }).then(normalizeTrend)
}

/**
 * 获取性能趋势
 */
export function getPerformanceTrend(
  metricName: string,
  timeRange: MetricsTimeRange = 'hour',
  dataPoints?: number
) {
  return request.post<unknown>('/monitoring/metrics/trend', {
    metricName,
    timeRange,
    dataPoints,
  }).then(normalizeTrend)
}

// ─── 告警规则 ───────────────────────────────────────────────────────

/**
 * 查询告警规则列表
 */
export function searchAlertRules(data: AlertRuleSearchVO) {
  return request.post<unknown>('/monitoring/alert-rules/search', data)
    .then(raw => normalizePage(raw, normalizeAlertRule, data.page ?? 0, data.rows ?? 20))
}

/**
 * 获取告警规则详情
 */
export function getAlertRuleDetail(ruleId: number) {
  return request.post<unknown>('/monitoring/alert-rules/detail', { ruleId }).then(normalizeAlertRule)
}

/**
 * 创建告警规则
 */
export function createAlertRule(data: AlertRule) {
  return request.post<unknown>('/monitoring/alert-rules/create', data).then(normalizeAlertRule)
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
  return request.post<unknown>('/monitoring/alerts/search', data)
    .then(raw => normalizePage(raw, normalizeAlert, data.page ?? 0, data.rows ?? 20))
}

/**
 * 获取活跃告警
 */
export function getActiveAlerts(severity?: string, limit: number = 100) {
  return request.post<unknown>(
    '/monitoring/alerts/active',
    { severity, limit }
  ).then(normalizeActiveAlerts)
}

/**
 * 获取告警详情
 */
export function getAlertDetail(alertId: number) {
  return request.post<unknown>('/monitoring/alerts/detail', { alertId }).then(normalizeAlert)
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
  return request.post<unknown>('/monitoring/alerts/statistics', {
    startDate,
    endDate,
  }).then(identityRecord) as Promise<Record<string, number>>
}

// ─── 日志查看 ───────────────────────────────────────────────────────

/**
 * 查询日志
 */
export function searchLogs(data: LogSearchVO) {
  return request.post<unknown>('/monitoring/logs/search', data)
    .then(raw => normalizePage(raw, normalizeLogEntry, data.page ?? 0, data.rows ?? 20))
}

/**
 * 获取日志详情
 */
export function getLogDetail(logId: number) {
  return request.post<unknown>('/monitoring/logs/detail', { logId }).then(normalizeLogEntry)
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
  return request.post<unknown>('/monitoring/logs/statistics', {
    startDate,
    endDate,
  }).then(identityRecord)
}

/**
 * 获取错误日志聚合
 */
export function getErrorAggregation(limit: number = 20) {
  return request.post<unknown>(
    '/monitoring/logs/error-aggregation',
    { limit }
  ).then(raw => normalizeArray<{ errorType: string; count: number; lastOccurrence: string }>(raw))
}

// ─── 系统健康检查 ───────────────────────────────────────────────────────

/**
 * 获取系统健康状态
 */
export function getHealthStatus() {
  return request.post<unknown>('/monitoring/health/status', {}).then(normalizeHealth)
}

/**
 * 获取组件健康状态
 */
export function getComponentHealth(componentName: string) {
  return request.post<unknown>(
    '/monitoring/health/component',
    { componentName }
  ).then(identityRecord)
}

/**
 * 获取数据库连接池状态
 */
export function getDatabasePoolStatus() {
  return request.post<unknown>('/monitoring/health/database', {}).then(identityRecord)
}

/**
 * 获取缓存状态
 */
export function getCacheStatus() {
  return request.post<unknown>('/monitoring/health/cache', {}).then(identityRecord)
}

// ─── 仪表板 ───────────────────────────────────────────────────────

/**
 * 获取仪表板数据
 */
export function getDashboardData(period: MetricsTimeRange = 'hour') {
  return request.post<unknown>('/monitoring/dashboard/data', { period }).then(normalizeDashboardData)
}

/**
 * 获取监控统计
 */
export function getMonitoringStatistics() {
  return request.post<unknown>('/monitoring/statistics', {}).then(normalizeStats)
}

/**
 * 获取顶部错误端点
 */
export function getTopErrorEndpoints(limit: number = 10) {
  return request.post<unknown>(
    '/monitoring/top-error-endpoints',
    { limit }
  ).then(raw => normalizeArray<{ endpoint: string; errorCount: number; errorRate: number }>(raw))
}

/**
 * 获取顶部慢查询端点
 */
export function getTopSlowEndpoints(limit: number = 10) {
  return request.post<unknown>('/monitoring/top-slow-endpoints', { limit })
    .then(raw => normalizeArray<{ endpoint: string; averageResponseTime: number; requestCount: number }>(raw))
}

// ─── WebSocket/SSE 流 ───────────────────────────────────────────────────────

/**
 * 建立 EventSource 连接以接收实时指标
 * 使用 EventSource 处理服务器推送事件
 * 事件类型：metrics, alert, health, log
 */
export function subscribeToRealtimeMetrics(): EventSource {
  const token = localStorage.getItem('token')
  const query = token ? `?token=${encodeURIComponent(token)}` : ''
  return new EventSource(`/api/v1/monitoring/stream/realtime${query}`)
}

/**
 * 建立 EventSource 连接以接收告警通知
 */
export function subscribeToAlerts(): EventSource {
  const token = localStorage.getItem('token')
  const query = token ? `?token=${encodeURIComponent(token)}` : ''
  return new EventSource(`/api/v1/monitoring/stream/alerts${query}`)
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
  return request.post<unknown>('/monitoring/system/resources', {}).then(identityRecord)
}
