/**
 * TypeScript Types for Monitoring Module (W-10)
 * 监控、告警、日志、健康检查相关类型定义
 */

import type { BasicQuery } from '@/types/common'

/** 告警严重级别 */
export enum AlertSeverity {
  INFO = 'info',           // 信息
  LOW = 'low',             // 低
  MEDIUM = 'medium',       // 中
  HIGH = 'high',           // 高
  CRITICAL = 'critical',   // 严重
}

/** 告警状态 */
export enum AlertStatus {
  ACTIVE = 'active',       // 活跃
  ACKNOWLEDGED = 'acknowledged', // 已确认
  RESOLVED = 'resolved',   // 已解决
  CLOSED = 'closed',       // 已关闭
}

/** 日志级别 */
export enum LogLevel {
  TRACE = 'TRACE',
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR',
  FATAL = 'FATAL',
}

/** 系统组件状态 */
export enum ComponentStatus {
  UP = 'UP',               // 正常
  DEGRADED = 'DEGRADED',   // 性能下降
  DOWN = 'DOWN',           // 不可用
  UNKNOWN = 'UNKNOWN',     // 未知
}

/** 实时指标 */
export interface RealtimeMetrics {
  timestamp?: number
  cpuUsage?: number
  memoryUsage?: number
  diskUsage?: number
  networkIn?: number
  networkOut?: number
  activeConnections?: number
  requestsPerSecond?: number
  errorRate?: number
  responseTime?: number
  queueDepth?: number
  cacheHitRate?: number
  databaseConnections?: number
}

/** 告警规则 */
export interface AlertRule {
  ruleId?: number
  ruleName: string
  description?: string
  metricName: string
  operator: 'gt' | 'gte' | 'lt' | 'lte' | 'eq' | 'ne'
  threshold: number
  severity: AlertSeverity
  duration?: number // 持续时间（秒）
  enabled: boolean
  notificationChannels?: string[] // email, sms, webhook, dingtalk
  createdAt?: string
  updatedAt?: string
}

/** 异常告警 */
export interface AnomalyAlert {
  alertId?: number
  alertType: string // metric_threshold, anomaly_detection, health_check
  ruleName?: string
  severity: AlertSeverity
  status: AlertStatus
  metric?: string
  currentValue?: number
  threshold?: number
  message: string
  firstDetectedAt?: string
  lastDetectedAt?: string
  acknowledgedAt?: string
  acknowledgedBy?: string
  resolution?: string
  resolvedAt?: string
  resolvedBy?: string
  additionalInfo?: Record<string, unknown>
}

/** 日志条目 */
export interface LogEntry {
  logId?: number
  timestamp?: string
  level: LogLevel
  logger: string
  message: string
  threadName?: string
  userId?: number
  username?: string
  traceId?: string
  module?: string
  endpoint?: string
  statusCode?: number
  responseTime?: number
  exception?: string
  stackTrace?: string
}

/** 系统健康状态 */
export interface SystemHealthStatus {
  status: ComponentStatus
  statusCode?: number
  message?: string
  timestamp?: string
  components: {
    database?: ComponentStatusDetail
    cache?: ComponentStatusDetail
    messageQueue?: ComponentStatusDetail
    elasticsearch?: ComponentStatusDetail
    milvus?: ComponentStatusDetail
    storage?: ComponentStatusDetail
    external?: ComponentStatusDetail
  }
}

/** 组件状态详情 */
export interface ComponentStatusDetail {
  status: ComponentStatus
  responseTime?: number
  lastCheck?: string
  message?: string
  details?: Record<string, unknown>
}

/** 性能趋势数据 */
export interface PerformanceTrendData {
  metricName: string
  unit?: string
  timeRange: string
  dataPoints: PerformanceDataPoint[]
  summary?: {
    average: number
    min: number
    max: number
    percentile95: number
    percentile99: number
  }
}

/** 性能数据点 */
export interface PerformanceDataPoint {
  timestamp: number
  value: number
  label?: string
}

/** 仪表板数据 */
export interface DashboardData {
  period: string
  metrics: {
    totalRequests: number
    successRequests: number
    failedRequests: number
    averageResponseTime: number
    errorRate: number
    uptime: number
  }
  alerts: {
    critical: number
    high: number
    medium: number
    low: number
  }
  components: ComponentStatusDetail[]
  topErrors?: {
    errorType: string
    count: number
    percentage: number
  }[]
  topSlowEndpoints?: {
    endpoint: string
    averageResponseTime: number
    requestCount: number
  }[]
}

/** 告警规则搜索 VO */
export interface AlertRuleSearchVO extends BasicQuery {
  ruleName?: string
  metricName?: string
  severity?: AlertSeverity
  enabled?: boolean
}

/** 告警搜索 VO */
export interface AlertSearchVO extends BasicQuery {
  severity?: AlertSeverity
  status?: AlertStatus
  startDate?: string
  endDate?: string
  keyword?: string
}

/** 日志搜索 VO */
export interface LogSearchVO extends BasicQuery {
  level?: LogLevel
  logger?: string
  module?: string
  traceId?: string
  userId?: number
  keyword?: string
  startDate?: string
  endDate?: string
  statusCode?: number
}

/** 指标时间范围 */
export type MetricsTimeRange = 'hour' | 'day' | 'week' | 'month'

/** 监控统计 */
export interface MonitoringStatistics {
  totalAlerts: number
  activeAlerts: number
  resolvedAlerts: number
  criticalAlerts: number
  uptime: number
  averageResponseTime: number
  peakLoad: number
  errorRate: number
}

/** WebSocket 事件类型 */
export type MonitoringEventType = 'metrics' | 'alert' | 'health' | 'log' | 'error'

/** 监控事件 */
export interface MonitoringEvent {
  type: MonitoringEventType
  data: unknown
  timestamp: string
}
