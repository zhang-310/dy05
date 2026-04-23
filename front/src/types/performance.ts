/**
 * W-08: Performance Monitoring Types
 * 性能监控与告警相关类型定义
 */

import { BasicQuery } from './common'

/**
 * 性能指标 VO
 */
export interface PerformanceMetricsVO {
  // API 性能指标
  apiRequestsPerMinute: number
  apiAverageLatencyMs: number
  apiP50LatencyMs: number
  apiP95LatencyMs: number
  apiP99LatencyMs: number
  apiErrorRate: number // 百分比
  apiErrorCount: number
  apiSuccessRate: number // 百分比
  // 系统资源
  cpuUsagePercent: number
  memoryUsagePercent: number
  diskUsagePercent: number
  // 数据库
  databaseConnectionCount: number
  databaseActiveConnections: number
  databaseQueriesPerSecond: number
  databaseSlowQueryCount: number
  // 缓存
  redisCacheHitRate: number // 百分比
  redisCacheMissRate: number
  redisMemoryUsageMB: number
  redisKeysCount: number
  // 消息队列
  rabbitmqMessagesQueued: number
  rabbitmqMessagesProcessed: number
  rabbitmqProcessingRate: number // msg/s
  // 用户相关
  activeUserCount: number
  onlineSessionCount: number
  // 系统整体
  systemAvailabilityPercent: number
  systemUptimeSeconds: number
  // 时间戳
  timestamp: string
  collectionTime: string
}

/**
 * 查询分析 VO
 */
export interface QueryAnalysisVO {
  reportTime: string
  // 慢查询
  slowQueries: {
    id: string
    sql: string
    executionTimeMs: number
    count: number
    affectedRows: number
    lastExecutedAt: string
  }[]
  // N+1 问题
  nPlusOneIssues: {
    id: string
    description: string
    apiEndpoint: string
    estimatedExtraQueries: number
    affectedUsers: number
    severity: 'low' | 'medium' | 'high'
    suggestion: string
  }[]
  // 索引建议
  indexSuggestions: {
    tableName: string
    columnNames: string[]
    estimatedImpactPercent: number
    estimatedQueryTimeReductionMs: number
    priority: 'low' | 'medium' | 'high'
  }[]
  // 统计
  totalQueriesAnalyzed: number
  averageQueryTimeMs: number
}

/**
 * 缓存统计 VO
 */
export interface CacheStatisticsVO {
  reportTime: string
  // 命中率相关
  totalRequests: number
  cacheHits: number
  cacheMisses: number
  hitRate: number // 百分比
  // 数据量
  keysCount: number
  memoryUsageMB: number
  memoryLimitMB: number
  memoryUsagePercent: number
  // 淘汰策略
  evictionPolicy: string // e.g., "allkeys-lru"
  evictionsPerHour: number
  // 热 key 分析
  hotKeys: {
    key: string
    accessCount: number
    accessRate: number // accesses per minute
    sizeMB: number
    ttlSeconds: number
  }[]
  // 分布统计
  keysByType: {
    type: string // string, hash, list, set, zset
    count: number
    memorySizeMB: number
  }[]
}

/**
 * 告警规则 VO
 */
export interface AlertRuleVO {
  id: number
  name: string
  description: string
  metric: string // e.g., "api_p95_latency", "error_rate", "memory_usage"
  condition: 'greater_than' | 'less_than' | 'equals' | 'in_range'
  threshold: number
  thresholdMax?: number // for in_range condition
  comparisonDuration: number // 秒数
  severity: 'info' | 'warning' | 'critical'
  isEnabled: boolean
  // 告警动作
  actions: {
    type: 'email' | 'webhook' | 'dingtalk' | 'wecom' | 'sms' | 'log'
    target: string
    retryCount?: number
    retryIntervalSeconds?: number
  }[]
  // 静默期
  silenceDuration?: number // 秒数
  lastSilencedAt?: string
  // 统计
  triggeredCount: number
  lastTriggeredAt?: string
  acknowledgedCount: number
  // 元数据
  createdAt: string
  updatedAt: string
  createdBy?: string
}

/**
 * 告警历史 VO
 */
export interface AlertHistoryVO {
  id: number
  ruleId: number
  ruleName: string
  metric: string
  metricValue: number
  threshold: number
  severity: 'info' | 'warning' | 'critical'
  status: 'triggered' | 'resolved' | 'acknowledged' | 'silenced'
  triggeredAt: string
  resolvedAt?: string
  acknowledgedAt?: string
  acknowledgedBy?: string
  message: string
  detail?: string
  relatedIncidents?: number[]
}

/**
 * 健康检查状态 VO
 */
export interface HealthCheckStatusVO {
  overallStatus: 'healthy' | 'warning' | 'critical'
  lastCheckTime: string
  components: {
    name: string // e.g., "PostgreSQL", "Redis", "RabbitMQ", "Elasticsearch"
    status: 'healthy' | 'warning' | 'critical' | 'unknown'
    message?: string
    latencyMs?: number
    version?: string
    details?: Record<string, unknown>
  }[]
}

/**
 * 性能指标查询 VO
 */
export interface PerformanceMetricsSearchVO extends BasicQuery {
  metricType?: string // 'api' | 'system' | 'database' | 'cache' | 'all'
  startTime?: string
  endTime?: string
  interval?: 'minute' | 'hour' | 'day' // 聚合间隔
}

/**
 * 告警规则查询 VO
 */
export interface AlertRuleSearchVO extends BasicQuery {
  severity?: string
  isEnabled?: boolean
  metric?: string
  keyword?: string
}

/**
 * 告警历史查询 VO
 */
export interface AlertHistorySearchVO extends BasicQuery {
  ruleId?: number
  severity?: string
  status?: string
  startTime?: string
  endTime?: string
  keyword?: string
}

/**
 * 告警规则保存 VO
 */
export interface AlertRuleSaveVO {
  id?: number
  name: string
  description?: string
  metric: string
  condition: 'greater_than' | 'less_than' | 'equals' | 'in_range'
  threshold: number
  thresholdMax?: number
  comparisonDuration: number
  severity: 'info' | 'warning' | 'critical'
  actions: {
    type: 'email' | 'webhook' | 'dingtalk' | 'wecom' | 'sms' | 'log'
    target: string
    retryCount?: number
    retryIntervalSeconds?: number
  }[]
  silenceDuration?: number
  isEnabled: boolean
}

/**
 * 告警确认请求 VO
 */
export interface AlertAcknowledgeVO {
  alertId: number
  acknowledgedBy?: string
  comment?: string
}

/**
 * 性能指标导出请求 VO
 */
export interface PerformanceExportVO {
  startTime: string
  endTime: string
  metrics: string[] // 要导出的指标列表
  format: 'csv' | 'excel' | 'json'
  includeCharts?: boolean
}
