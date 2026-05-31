import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeArray,
  normalizePage as normalizeResponsePage,
  normalizeRecord,
  parseJsonValue,
  readNumber,
} from '@/utils/response-normalize'

type UnknownRecord = Record<string, unknown>

export interface SystemInfo {
  jvmMemory: number
  cpuUsage: number
  dbConnections: number
  uptime: number
  jvm?: UnknownRecord
  threads?: UnknownRecord
  runtime?: UnknownRecord
}

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
  module?: string
  apiName?: string
  requestUrl?: string
}
export interface ApiLogQuery {
  page?: number
  rows?: number
  module?: string
  apiName?: string
  apiPath?: string
  status?: number
  statusCode?: number
  startTime?: string
  endTime?: string
  sortName?: string
  sortOrder?: 'asc' | 'desc'
}
export interface ApiLogStats {
  totalCalls: number
  successCalls: number
  errorCalls: number
  avgResponseTime: number
  p99ResponseTime: number
  successRate?: number
  byModule?: UnknownRecord[]
  byApiName?: UnknownRecord[]
}

export interface SystemDiagnosticReport {
  generatedAt: string
  source: string
  degraded: boolean
  failures: string[]
  health: UnknownRecord
  info: UnknownRecord
  apiStats: ApiLogStats
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
  userId?: number | null
  accountId?: number | null
}
export interface SyncLogQuery {
  page?: number
  rows?: number
  syncType?: string
  status?: string
  userId?: number
  startTime?: string
  endTime?: string
}

export interface AlertRule {
  id: number
  ruleName: string
  metric: string
  threshold: number
  operator: string
  severity: string
  status: number
  createTime?: string
  name?: string
  metricName?: string
  type?: string
  duration?: number
  description?: string
  enabled?: boolean
}
export interface AlertRuleSave {
  id?: number
  ruleName?: string
  metric?: string
  threshold: number
  operator?: string
  severity?: string
  status?: number
  name?: string
  metricName?: string
  type?: string
  duration?: number
  description?: string
  enabled?: boolean
}
export interface AlertRuleQuery {
  page?: number
  rows?: number
  ruleName?: string
  metric?: string
  severity?: string
  status?: number
}

export interface AlertRecord {
  id: number
  ruleId: number
  ruleName: string
  metric: string
  value: number
  severity: string
  status: string
  createTime: string
  metricName?: string
  message?: string
  threshold?: number
  triggeredAt?: string
  resolvedAt?: string
}
export interface AlertActiveResult {
  alerts: AlertRecord[]
  total: number
  length?: number
  map?: never
}
export interface AlertQuery {
  page?: number
  rows?: number
  severity?: string
  status?: string
  startTime?: string
  endTime?: string
}

export interface TaxonomyNode {
  id: number
  parentId: number
  moduleScope: string
  code: string
  name: string
  sortOrder: number
  enabled: number
  createTime: string
}
export interface TaxonomySave {
  id?: number
  parentId?: number
  moduleScope: string
  code: string
  name: string
  sortOrder?: number
  enabled?: number
}

export interface ExternalApiConfig {
  id: number
  providerCode: string
  providerName: string
  category?: string
  baseUrl?: string
  apiKeyEncrypted?: string
  apiSecretEncrypted?: string
  isEnabled: boolean
  priority?: number
  rateLimitPerMin?: number
  dailyQuota?: number
  monthlyQuota?: number
  lastHealthCheck?: string
  healthStatus?: string
  avgLatencyMs?: number
  successRatePct?: number
  extraConfig?: string
  createTime?: string
  updateTime?: string
  apiName?: string
  authType?: string
  status?: number
}

export interface ExternalApiConfigSave {
  id?: number
  providerCode?: string
  providerName?: string
  category?: string
  baseUrl?: string
  apiKey?: string
  apiSecret?: string
  isEnabled?: boolean
  priority?: number
  rateLimitPerMin?: number
  dailyQuota?: number
  monthlyQuota?: number
  extraConfig?: string
  apiName?: string
  status?: number
}

export interface ExternalApiHealthStatusPayload {
  providerCode: string
  status?: string
  latencyMs?: number
  successRate?: number
}

export interface ExternalApiProbeResult {
  providerCode: string
  status: string
}

function unwrapRecord(raw: unknown): UnknownRecord {
  return normalizeRecord(raw)
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function toStringValue(value: unknown, fallback = ''): string {
  if (value == null) return fallback
  return String(value)
}

function compactRecord(record: UnknownRecord): UnknownRecord {
  return Object.fromEntries(Object.entries(record).filter(([, value]) => value !== undefined))
}

function parseMemoryToMb(value: unknown): number {
  if (typeof value === 'number') return Math.round(value / 1024 / 1024)
  if (typeof value !== 'string') return 0
  const n = Number(value.replace(/[^\d.]/g, ''))
  if (!Number.isFinite(n)) return 0
  if (value.toUpperCase().includes('GB')) return Math.round(n * 1024)
  if (value.toUpperCase().includes('KB')) return Math.round(n / 1024)
  return Math.round(n)
}

function parseUptimeSeconds(value: unknown): number {
  if (typeof value === 'number') return value
  if (typeof value !== 'string') return 0
  const h = Number(value.match(/(\d+)\s*h/)?.[1] ?? 0)
  const m = Number(value.match(/(\d+)\s*m/)?.[1] ?? 0)
  const s = Number(value.match(/(\d+)\s*s/)?.[1] ?? 0)
  return h * 3600 + m * 60 + s
}

function readPage<T, R>(
  page: unknown,
  mapper: (item: T) => R,
  requestedPage = 0,
  requestedRows = 30,
): PageResult<R> {
  return normalizeResponsePage(page, mapper, requestedPage, requestedRows)
}

function mapSystemInfo(raw: UnknownRecord): SystemInfo {
  const jvm = isRecord(raw.jvm) ? raw.jvm : undefined
  const runtime = isRecord(raw.runtime) ? raw.runtime : undefined
  const threads = isRecord(raw.threads) ? raw.threads : undefined
  return {
    jvmMemory: toNumber(raw.jvmMemory, parseMemoryToMb(jvm?.usedMemory)),
    cpuUsage: toNumber(raw.cpuUsage),
    dbConnections: toNumber(raw.dbConnections, toNumber(threads?.activeCount)),
    uptime: toNumber(raw.uptime, parseUptimeSeconds(runtime?.uptime)),
    jvm,
    threads,
    runtime,
  }
}

function mapApiLog(raw: UnknownRecord): ApiLog {
  const apiPath = toStringValue(raw.apiPath ?? raw.requestUrl ?? raw.apiName)
  const responseStatus = toNumber(raw.statusCode ?? raw.responseStatus ?? raw.status)
  const businessStatus = toNumber(raw.status)
  const statusCode = responseStatus > 1 ? responseStatus : businessStatus === 1 ? 200 : 500
  return {
    id: toNumber(raw.id),
    apiPath,
    method: toStringValue(raw.method ?? raw.requestMethod, '--'),
    statusCode,
    responseTime: toNumber(raw.responseTime ?? raw.durationMs),
    userId: raw.userId == null ? null : toNumber(raw.userId),
    ip: toStringValue(raw.ip, '--'),
    errorMsg: raw.errorMsg == null && raw.errorMessage == null ? null : toStringValue(raw.errorMsg ?? raw.errorMessage),
    createTime: toStringValue(raw.createTime),
    module: raw.module == null ? undefined : toStringValue(raw.module),
    apiName: raw.apiName == null ? undefined : toStringValue(raw.apiName),
    requestUrl: raw.requestUrl == null ? undefined : toStringValue(raw.requestUrl),
  }
}

function mapApiLogQuery(params: ApiLogQuery): UnknownRecord {
  const status = params.status ?? (params.statusCode == null ? undefined : params.statusCode >= 400 ? 0 : 1)
  const sortNameMap: Record<string, string> = {
    apiPath: 'apiName',
    statusCode: 'status',
    responseTime: 'durationMs',
  }
  return compactRecord({
    page: params.page,
    rows: params.rows,
    module: params.module,
    apiName: params.apiName ?? params.apiPath,
    status,
    startTime: params.startTime,
    endTime: params.endTime,
    sortName: params.sortName ? sortNameMap[params.sortName] ?? params.sortName : undefined,
    sortOrder: params.sortOrder,
  })
}

function mapApiLogStats(raw: UnknownRecord): ApiLogStats {
  const totalCalls = toNumber(raw.totalCalls)
  const successCalls = toNumber(raw.successCalls ?? raw.successCount)
  const errorCalls = toNumber(raw.errorCalls ?? raw.failCount, Math.max(totalCalls - successCalls, 0))
  return {
    totalCalls,
    successCalls,
    errorCalls,
    avgResponseTime: toNumber(raw.avgResponseTime ?? raw.avgDurationMs),
    p99ResponseTime: toNumber(raw.p99ResponseTime ?? raw.p95ResponseTime ?? raw.avgDurationMs),
    successRate: raw.successRate == null ? undefined : toNumber(raw.successRate),
    byModule: Array.isArray(raw.byModule) ? raw.byModule.filter(isRecord) : undefined,
    byApiName: Array.isArray(raw.byApiName) ? raw.byApiName.filter(isRecord) : undefined,
  }
}

function mapDiagnosticReport(raw: unknown): SystemDiagnosticReport {
  const record = unwrapRecord(raw)
  const failures = normalizeArray<unknown>(record.failures).map(item => String(item))
  return {
    generatedAt: toStringValue(record.generatedAt),
    source: toStringValue(record.source, '/system/diagnostic/report'),
    degraded: Boolean(record.degraded),
    failures,
    health: isRecord(record.health) ? record.health : {},
    info: isRecord(record.info) ? record.info : {},
    apiStats: mapApiLogStats(isRecord(record.apiStats) ? record.apiStats : {}),
  }
}

function hasAlertShape(value: unknown): value is { alerts: unknown[]; total: number } {
  return isRecord(value) && Array.isArray(value.alerts) && typeof value.total === 'number'
}

function normalizeSyncStatus(status: unknown): string {
  const value = toStringValue(status).toLowerCase()
  if (value === 'success') return 'SUCCESS'
  if (value === 'failed' || value === 'fail') return 'FAILED'
  if (value === 'running') return 'RUNNING'
  return toStringValue(status, '--').toUpperCase()
}

function mapSyncLog(raw: UnknownRecord): SyncLog {
  return {
    id: toNumber(raw.id),
    syncType: toStringValue(raw.syncType),
    status: normalizeSyncStatus(raw.status),
    totalCount: toNumber(raw.totalCount),
    successCount: toNumber(raw.successCount),
    failCount: toNumber(raw.failCount),
    errorMessage: raw.errorMessage == null ? null : toStringValue(raw.errorMessage),
    startTime: toStringValue(raw.startTime),
    endTime: toStringValue(raw.endTime),
    createTime: toStringValue(raw.createTime),
    userId: raw.userId == null ? null : toNumber(raw.userId),
    accountId: raw.accountId == null ? null : toNumber(raw.accountId),
  }
}

function mapAlertRule(raw: UnknownRecord): AlertRule {
  const enabled = typeof raw.enabled === 'boolean' ? raw.enabled : toNumber(raw.status, 1) === 1
  const name = toStringValue(raw.name ?? raw.ruleName)
  const metricName = toStringValue(raw.metricName ?? raw.metric)
  return {
    id: toNumber(raw.id),
    ruleName: name,
    metric: metricName,
    threshold: toNumber(raw.threshold),
    operator: toStringValue(raw.operator, '>'),
    severity: toStringValue(raw.severity, 'warning'),
    status: enabled ? 1 : 0,
    createTime: raw.createTime == null ? undefined : toStringValue(raw.createTime),
    name,
    metricName,
    type: raw.type == null ? undefined : toStringValue(raw.type),
    duration: raw.duration == null ? undefined : toNumber(raw.duration),
    description: raw.description == null ? undefined : toStringValue(raw.description),
    enabled,
  }
}

function mapAlertRuleSave(params: Partial<AlertRuleSave>): UnknownRecord {
  const id = params.id
  return compactRecord({
    id,
    ruleId: id,
    name: params.name ?? params.ruleName ?? '',
    metricName: params.metricName ?? params.metric ?? '',
    type: params.type ?? 'threshold',
    threshold: params.threshold ?? 0,
    operator: params.operator ?? '>',
    duration: params.duration ?? 60,
    severity: params.severity ?? 'warning',
    description: params.description,
    enabled: params.enabled ?? params.status !== 0,
  })
}

function normalizeAlertStatus(status: unknown): string {
  const value = toStringValue(status).toLowerCase()
  if (value === 'triggered' || value === 'active') return 'ACTIVE'
  if (value === 'acknowledged') return 'ACKNOWLEDGED'
  if (value === 'resolved') return 'RESOLVED'
  if (value === 'closed') return 'CLOSED'
  return toStringValue(status, '--').toUpperCase()
}

function normalizeAlertSeverity(severity: unknown): string {
  const value = toStringValue(severity).toLowerCase()
  if (value === 'critical' || value === 'high') return 'HIGH'
  if (value === 'warning' || value === 'medium') return 'MEDIUM'
  if (value === 'info' || value === 'low') return 'LOW'
  return toStringValue(severity, 'LOW').toUpperCase()
}

function mapAlertRecord(raw: UnknownRecord): AlertRecord {
  return {
    id: toNumber(raw.id),
    ruleId: toNumber(raw.ruleId),
    ruleName: toStringValue(raw.ruleName),
    metric: toStringValue(raw.metric ?? raw.metricName),
    value: toNumber(raw.value),
    severity: normalizeAlertSeverity(raw.severity),
    status: normalizeAlertStatus(raw.status),
    createTime: toStringValue(raw.createTime ?? raw.triggeredAt),
    metricName: raw.metricName == null ? undefined : toStringValue(raw.metricName),
    message: raw.message == null ? undefined : toStringValue(raw.message),
    threshold: raw.threshold == null ? undefined : toNumber(raw.threshold),
    triggeredAt: raw.triggeredAt == null ? undefined : toStringValue(raw.triggeredAt),
    resolvedAt: raw.resolvedAt == null ? undefined : toStringValue(raw.resolvedAt),
  }
}

function mapExternalApiConfig(raw: UnknownRecord): ExternalApiConfig {
  const providerCode = toStringValue(raw.providerCode ?? raw.apiName)
  const providerName = toStringValue(raw.providerName ?? raw.apiName ?? providerCode)
  const isEnabled = typeof raw.isEnabled === 'boolean' ? raw.isEnabled : toNumber(raw.status, 1) === 1
  return {
    id: toNumber(raw.id),
    providerCode,
    providerName,
    category: raw.category == null ? undefined : toStringValue(raw.category),
    baseUrl: raw.baseUrl == null ? undefined : toStringValue(raw.baseUrl),
    apiKeyEncrypted: raw.apiKeyEncrypted == null ? undefined : toStringValue(raw.apiKeyEncrypted),
    apiSecretEncrypted: raw.apiSecretEncrypted == null ? undefined : toStringValue(raw.apiSecretEncrypted),
    isEnabled,
    priority: raw.priority == null ? undefined : toNumber(raw.priority),
    rateLimitPerMin: raw.rateLimitPerMin == null ? undefined : toNumber(raw.rateLimitPerMin),
    dailyQuota: raw.dailyQuota == null ? undefined : toNumber(raw.dailyQuota),
    monthlyQuota: raw.monthlyQuota == null ? undefined : toNumber(raw.monthlyQuota),
    lastHealthCheck: raw.lastHealthCheck == null ? undefined : toStringValue(raw.lastHealthCheck),
    healthStatus: raw.healthStatus == null ? undefined : toStringValue(raw.healthStatus),
    avgLatencyMs: raw.avgLatencyMs == null ? undefined : toNumber(raw.avgLatencyMs),
    successRatePct: raw.successRatePct == null ? undefined : toNumber(raw.successRatePct),
    extraConfig: raw.extraConfig == null ? undefined : toStringValue(raw.extraConfig),
    createTime: raw.createTime == null ? undefined : toStringValue(raw.createTime),
    updateTime: raw.updateTime == null ? undefined : toStringValue(raw.updateTime),
    apiName: providerName,
    authType: raw.authType == null ? undefined : toStringValue(raw.authType),
    status: isEnabled ? 1 : 0,
  }
}

function mapExternalApiSave(params: Partial<ExternalApiConfigSave>): UnknownRecord {
  return compactRecord({
    id: params.id,
    providerCode: params.providerCode ?? params.apiName ?? '',
    providerName: params.providerName ?? params.apiName ?? params.providerCode ?? '',
    category: params.category,
    baseUrl: params.baseUrl,
    apiKey: params.apiKey,
    apiSecret: params.apiSecret,
    isEnabled: params.isEnabled ?? params.status !== 0,
    priority: params.priority ?? 0,
    rateLimitPerMin: params.rateLimitPerMin,
    dailyQuota: params.dailyQuota,
    monthlyQuota: params.monthlyQuota,
    extraConfig: params.extraConfig,
  })
}

function mapHealthStatusPayload(target: ExternalApiHealthStatusPayload | string): ExternalApiHealthStatusPayload {
  if (typeof target === 'string') return { providerCode: target, status: 'unknown' }
  const status = typeof target.status === 'string' ? target.status : 'unknown'
  return compactRecord({
    providerCode: target.providerCode,
    status,
    latencyMs: target.latencyMs,
    successRate: target.successRate,
  }) as unknown as ExternalApiHealthStatusPayload
}

function mapSlowQueryPage<T>(raw: unknown, params?: { page?: number; rows?: number; minMs?: number }): PageResult<T> {
  const list = normalizeArray<T>(raw)
  return {
    total: readNumber(raw, ['total', 'totalElements', 'count', 'totalCount', 'totalRecords'], list.length),
    list,
    pageNum: readNumber(raw, ['pageNum', 'page', 'pageNumber', 'current'], params?.page ?? 0),
    pageSize: readNumber(raw, ['pageSize', 'size', 'rows'], params?.rows ?? 10),
  }
}

export const systemApi = {
  info: () => request.post<UnknownRecord>('/system/info', {}).then((raw) => mapSystemInfo(unwrapRecord(raw))),
  health: () => request.post<UnknownRecord>('/system/health', {}).then(unwrapRecord),

  // API 日志
  apiLogList: (params: ApiLogQuery) =>
    request.post<unknown>('/system/api-log/list', mapApiLogQuery(params))
      .then((page) => readPage(page, mapApiLog, params.page ?? 0, params.rows ?? 20)),
  apiLogStats: (params?: Pick<ApiLogQuery, 'module' | 'startTime' | 'endTime'>) =>
    request.post<UnknownRecord>('/system/api-log/stats', params ?? {}).then((raw) => mapApiLogStats(unwrapRecord(raw))),
  apiLogGet: (id: number) => request.post<UnknownRecord>('/system/api-log/get', { id }).then((raw) => mapApiLog(unwrapRecord(raw))),

  // 同步日志
  syncLogList: (params: SyncLogQuery) =>
    request.post<unknown>('/system/sync-log/list', params)
      .then((page) => readPage(page, mapSyncLog, params.page ?? 0, params.rows ?? 20)),

  // 告警规则（Monitoring）
  alertRuleSearch: (params: AlertRuleQuery) =>
    request.post<unknown>('/monitoring/alert-rules/search', params)
      .then((page) => readPage(page, mapAlertRule, params.page ?? 0, params.rows ?? 20)),
  alertRuleCreate: (params: Partial<AlertRuleSave>) =>
    request.post<UnknownRecord>('/monitoring/alert-rules/create', mapAlertRuleSave(params)).then((raw) => mapAlertRule(unwrapRecord(raw))),
  alertRuleUpdate: (params: Partial<AlertRuleSave>) =>
    request.post<void>('/monitoring/alert-rules/update', mapAlertRuleSave(params)),
  alertRuleDelete: (id: number) => request.post<void>('/monitoring/alert-rules/delete', { ruleId: id }),
  alertRuleEnable: (id: number) => request.post<void>('/monitoring/alert-rules/enable', { ruleId: id }),
  alertRuleDisable: (id: number) => request.post<void>('/monitoring/alert-rules/disable', { ruleId: id }),

  // 告警记录
  alertSearch: (params: AlertQuery) =>
    request.post<unknown>('/monitoring/alerts/search', params)
      .then((page) => readPage(page, mapAlertRecord, params.page ?? 0, params.rows ?? 20)),
  alertActive: (limit = 100) =>
    request.post<unknown>('/monitoring/alerts/active', { limit })
      .then((data): AlertActiveResult => {
        const value = parseJsonValue(data)
        if (Array.isArray(value)) return { alerts: value.filter(isRecord).map(mapAlertRecord), total: value.length }
        const wrapper = unwrapRecord(value)
        if (hasAlertShape(wrapper)) {
          return { alerts: wrapper.alerts.filter(isRecord).map(mapAlertRecord), total: wrapper.total, length: wrapper.total }
        }
        const alerts = Array.isArray((wrapper as { alerts?: UnknownRecord[] }).alerts)
          ? ((wrapper as { alerts?: UnknownRecord[] }).alerts ?? []).map(mapAlertRecord)
          : normalizeArray<UnknownRecord>(wrapper).map(mapAlertRecord)
        return { alerts, total: Number(wrapper.total ?? alerts.length) }
      }),
  alertAcknowledge: (id: number) => request.post<void>('/monitoring/alerts/acknowledge', { alertId: id }),
  alertResolve: (id: number) => request.post<void>('/monitoring/alerts/resolve', { alertId: id }),

  // 监控指标
  metricsRealtime: () => request.post<UnknownRecord>('/monitoring/metrics/realtime', {}),
  metricsHistorical: (params: UnknownRecord) => request.post<UnknownRecord>('/monitoring/metrics/historical', params),

  // 行业分类
  taxonomyList: (moduleScope?: string) => request.post<TaxonomyNode[]>('/system/taxonomy/list', { moduleScope }),
  taxonomySave: (params: Partial<TaxonomySave>) => request.post<void>('/system/taxonomy/save', params),
  taxonomyDelete: (id: number) => request.post<void>('/system/taxonomy/delete', { id }),

  // 外部 API 配置
  externalApiList: (params?: UnknownRecord) =>
    request.post<unknown>('/system/external-api/list', params ?? {})
      .then((page) => readPage(page, mapExternalApiConfig, Number(params?.page ?? 0), Number(params?.rows ?? 20))),
  externalApiSave: (params: Partial<ExternalApiConfigSave>) =>
    request.post<ExternalApiConfig>('/system/external-api/save', mapExternalApiSave(params)),
  externalApiDelete: (id: number) => request.post<void>('/system/external-api/delete', { id }),
  externalApiHealthStatus: (target: ExternalApiHealthStatusPayload | string) =>
    request.post<void>('/system/external-api/health-status', mapHealthStatusPayload(target)),
  externalApiProbe: (providerCode: string) =>
    request.post<ExternalApiProbeResult>('/system/external-api/probe', { providerCode }),
  diagnosticReport: () => request.post<unknown>('/system/diagnostic/report', {}).then(mapDiagnosticReport),

  // Performance Monitoring
  performanceCurrent: () =>
    request.post<UnknownRecord>('/system/performance/metrics/current', {}).then(unwrapRecord),
  performanceApiList: (params?: { page?: number; rows?: number; path?: string; minAvgMs?: number }) =>
    request.post<unknown>('/system/performance/metrics/search', params ?? {})
      .then((page) => readPage(page, (item: UnknownRecord) => item, params?.page ?? 0, params?.rows ?? 20)),
  performanceTimeseries: (params?: { metric?: string; hours?: number }) =>
    request.post<unknown>('/system/performance/api/timeseries', params ?? {}).then(normalizeArray<UnknownRecord>),
  performanceSlowQuery: <T = UnknownRecord>(params?: { page?: number; rows?: number; minMs?: number }) =>
    request.post<unknown>('/system/performance/query/slow', {
      limit: params?.rows,
      minDurationMs: params?.minMs,
      page: params?.page,
    }).then((data) => mapSlowQueryPage<T>(data, params)),
  performanceNPlusOne: (params?: { page?: number; rows?: number }) =>
    request.post<unknown>('/system/performance/query/n-plus-one', { limit: params?.rows, page: params?.page }).then(normalizeArray<UnknownRecord>),
  performanceAnalysis: () =>
    request.post<UnknownRecord>('/system/performance/query/analysis', {}).then(unwrapRecord),
  performanceCacheStatistics: () =>
    request.post<UnknownRecord>('/system/performance/cache/statistics', {}).then(unwrapRecord),

  // Prometheus / Metrics
  metricsPrometheus: () => request.post<string>('/system/metrics/prometheus', {}),
  metricsAll: () => request.post<UnknownRecord>('/system/metrics/all', {}),
  metricsCpu: () => request.post<UnknownRecord>('/system/metrics/cpu', {}),
  metricsMemory: () => request.post<UnknownRecord>('/system/metrics/memory', {}),
  metricsDisk: () => request.post<UnknownRecord>('/system/metrics/disk', {}),
  metricsJvm: () => request.post<UnknownRecord>('/system/metrics/jvm', {}),
  metricsDatabase: () => request.post<UnknownRecord>('/system/metrics/database', {}),
}
