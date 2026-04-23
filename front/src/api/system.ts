import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface SystemInfo { jvmMemory: number; cpuUsage: number; dbConnections: number; uptime: number }

export interface ApiLog {
  id: number
  apiPath: string
  method: string
  statusCode: number
  responseTime: number
  userId: number | null
  ip: string
  errorMsg: string | null
  createTime: string
}
export interface ApiLogQuery { page?: number; rows?: number; apiPath?: string; statusCode?: number; startTime?: string; endTime?: string }
export interface ApiLogStats {
  totalCalls: number; successCalls: number; errorCalls: number
  avgResponseTime: number; p99ResponseTime: number
}

export interface SyncLog {
  id: number
  syncType: string
  status: string
  totalCount: number
  successCount: number
  failCount: number
  errorMessage: string | null
  startTime: string
  endTime: string
  createTime: string
}
export interface SyncLogQuery { page?: number; rows?: number; syncType?: string; status?: string }

export interface AlertRule {
  id: number; ruleName: string; metric: string; threshold: number
  operator: string; severity: string; status: number; createTime: string
}
export interface AlertRuleSave { id?: number; ruleName: string; metric: string; threshold: number; operator?: string; severity?: string; status?: number }

export interface AlertRecord {
  id: number; ruleId: number; ruleName: string; metric: string
  value: number; severity: string; status: string; createTime: string
}
export interface AlertQuery { page?: number; rows?: number; severity?: string; status?: string; startTime?: string; endTime?: string }

export interface TaxonomyNode {
  id: number; parentId: number; moduleScope: string
  code: string; name: string; sortOrder: number; enabled: number; createTime: string
}
export interface TaxonomySave { id?: number; parentId?: number; moduleScope: string; code: string; name: string; sortOrder?: number; enabled?: number }

export interface ExternalApiConfig {
  id: number; apiName: string; category: string; baseUrl: string
  authType: string; status: number; createTime: string
}

export const systemApi = {
  info: () => request.post<SystemInfo>('/system/info', {}),
  health: () => request.post<Record<string, unknown>>('/system/health', {}),

  // API 日志
  apiLogList: (params: ApiLogQuery) => request.post<PageResult<ApiLog>>('/system/api-log/list', params),
  apiLogStats: () => request.post<ApiLogStats>('/system/api-log/stats', {}),
  apiLogGet: (id: number) => request.post<ApiLog>('/system/api-log/get', { id }),

  // 同步日志
  syncLogList: (params: SyncLogQuery) => request.post<PageResult<SyncLog>>('/system/sync-log/list', params),

  // 告警规则（Monitoring）
  alertRuleSearch: (params: Record<string, unknown>) => request.post<PageResult<AlertRule>>('/monitoring/alert-rules/search', params),
  alertRuleCreate: (params: Partial<AlertRuleSave>) => request.post<AlertRule>('/monitoring/alert-rules/create', params),
  alertRuleUpdate: (params: Partial<AlertRuleSave>) => request.post<void>('/monitoring/alert-rules/update', params),
  alertRuleDelete: (id: number) => request.post<void>('/monitoring/alert-rules/delete', { id }),
  alertRuleEnable: (id: number) => request.post<void>('/monitoring/alert-rules/enable', { id }),
  alertRuleDisable: (id: number) => request.post<void>('/monitoring/alert-rules/disable', { id }),

  // 告警记录
  alertSearch: (params: AlertQuery) => request.post<PageResult<AlertRecord>>('/monitoring/alerts/search', params),
  alertActive: () => request.post<AlertRecord[]>('/monitoring/alerts/active', {}),
  alertAcknowledge: (id: number) => request.post<void>('/monitoring/alerts/acknowledge', { id }),
  alertResolve: (id: number) => request.post<void>('/monitoring/alerts/resolve', { id }),

  // 监控指标
  metricsRealtime: () => request.post<Record<string, unknown>>('/monitoring/metrics/realtime', {}),
  metricsHistorical: (params: Record<string, unknown>) => request.post<Record<string, unknown>[]>('/monitoring/metrics/historical', params),

  // 行业分类
  taxonomyList: (moduleScope?: string) => request.post<TaxonomyNode[]>('/system/taxonomy/list', { moduleScope }),
  taxonomySave: (params: Partial<TaxonomySave>) => request.post<void>('/system/taxonomy/save', params),
  taxonomyDelete: (id: number) => request.post<void>('/system/taxonomy/delete', { id }),

  // 外部 API 配置
  externalApiList: (params?: Record<string, unknown>) => request.post<PageResult<ExternalApiConfig>>('/system/external-api/list', params ?? {}),
  externalApiSave: (params: Partial<ExternalApiConfig>) => request.post<void>('/system/external-api/save', params),
  externalApiDelete: (id: number) => request.post<void>('/system/external-api/delete', { id }),
  externalApiHealthStatus: (id: number) => request.post<Record<string, unknown>>('/system/external-api/health-status', { id }),
  externalApiReplay: (id: number) => request.post<{ success: boolean; responseBody?: string; statusCode?: number; costMs?: number }>('/system/external-api/replay', { id }),
  diagnosticReport: () => request.post<{ downloadUrl: string }>('/system/diagnostic/report', {}),

  // Performance Monitoring (13-system v3.0)
  performanceCurrent: () =>
    request.post<Record<string, unknown>>('/system/performance/current', {}),
  performanceApiList: (params?: { page?: number; rows?: number; path?: string; minAvgMs?: number }) =>
    request.post<PageResult<Record<string, unknown>>>('/system/performance/api/list', params ?? {}),
  performanceTimeseries: (params?: { metric?: string; hours?: number }) =>
    request.post<Record<string, unknown>[]>('/system/performance/timeseries', params ?? {}),
  performanceSlowQuery: <T = Record<string, unknown>>(params?: { page?: number; rows?: number; minMs?: number }) =>
    request.post<PageResult<T>>('/system/performance/slow-query/list', params ?? {}),
  performanceNPlusOne: (params?: { page?: number; rows?: number }) =>
    request.post<PageResult<Record<string, unknown>>>('/system/performance/n-plus-one/list', params ?? {}),
  performanceAnalysis: () =>
    request.post<Record<string, unknown>>('/system/performance/analysis', {}),

  // Prometheus / Metrics
  metricsPrometheus: () => request.post<string>('/system/metrics/prometheus', {}),
  metricsAll: () => request.post<Record<string, unknown>>('/system/metrics/all', {}),
  metricsCpu: () => request.post<Record<string, unknown>>('/system/metrics/cpu', {}),
  metricsMemory: () => request.post<Record<string, unknown>>('/system/metrics/memory', {}),
  metricsDisk: () => request.post<Record<string, unknown>>('/system/metrics/disk', {}),
  metricsJvm: () => request.post<Record<string, unknown>>('/system/metrics/jvm', {}),
  metricsDatabase: () => request.post<Record<string, unknown>>('/system/metrics/database', {}),
}