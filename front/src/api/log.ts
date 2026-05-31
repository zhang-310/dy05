import request from '@/utils/request'
import { normalizePage } from '@/utils/response-normalize'

export interface OperationLog {
  id: number
  traceId?: string
  userId?: number
  username?: string
  action?: string
  module?: string
  requestUri?: string
  requestMethod?: string
  ip?: string
  durationMs?: number
  status?: number
  errorMsg?: string
  createTime?: string
}

export interface OperationLogQuery {
  page?: number
  rows?: number
  username?: string
  module?: string
  action?: string
  status?: number
  startTime?: string
  endTime?: string
}

export interface SystemLog {
  id: number
  module?: string
  eventType?: string
  summary?: string
  detail?: string
  status?: number
  createTime?: string
}

export interface SystemLogQuery {
  page?: number
  rows?: number
  module?: string
  eventType?: string
  status?: number
  startTime?: string
  endTime?: string
}

export interface AuditLog {
  id: number
  userId?: number
  username?: string
  auditType?: string
  module?: string
  action?: string
  entity?: string
  entityId?: number | null
  targetId?: number | null
  targetType?: string
  oldValue?: string | null
  newValue?: string | null
  beforeValue?: string | null
  afterValue?: string | null
  ip?: string
  userAgent?: string
  status?: number
  errorMsg?: string | null
  createTime?: string
}

export interface AuditLogQuery {
  page?: number
  rows?: number
  keyword?: string
  username?: string
  entity?: string
  action?: string
  status?: number
  startTime?: string
  endTime?: string
}

function toNumber(value: unknown, fallback = 0): number {
  const n = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(n) ? n : fallback
}

function toStringValue(value: unknown, fallback = ''): string {
  if (value == null) return fallback
  return String(value)
}

function mapOperationLog(raw: unknown): OperationLog {
  const record = (raw ?? {}) as Record<string, unknown>
  const base = record as unknown as Partial<OperationLog>
  return {
    ...base,
    id: toNumber(record.id),
    traceId: record.traceId == null ? base.traceId : toStringValue(record.traceId),
    userId: record.userId == null ? base.userId : toNumber(record.userId),
    username: toStringValue(record.username, base.username ?? ''),
    action: toStringValue(record.action ?? record.operation, base.action ?? ''),
    module: toStringValue(record.module, base.module ?? ''),
    requestUri: toStringValue(record.requestUri ?? record.uri ?? record.requestUrl, base.requestUri ?? ''),
    requestMethod: toStringValue(record.requestMethod ?? record.method, base.requestMethod ?? ''),
    ip: toStringValue(record.ip, base.ip ?? ''),
    durationMs: record.durationMs == null && record.responseTime == null ? base.durationMs : toNumber(record.durationMs ?? record.responseTime),
    status: record.status == null ? base.status : toNumber(record.status),
    errorMsg: record.errorMsg == null && record.errorMessage == null ? base.errorMsg : toStringValue(record.errorMsg ?? record.errorMessage),
    createTime: toStringValue(record.createTime, base.createTime ?? ''),
  }
}

function mapSystemLog(raw: unknown): SystemLog {
  const record = (raw ?? {}) as Record<string, unknown>
  const base = record as unknown as Partial<SystemLog>
  return {
    ...base,
    id: toNumber(record.id),
    module: toStringValue(record.module, base.module ?? ''),
    eventType: toStringValue(record.eventType ?? record.type, base.eventType ?? ''),
    summary: toStringValue(record.summary ?? record.message, base.summary ?? ''),
    detail: toStringValue(record.detail ?? record.details, base.detail ?? ''),
    status: record.status == null ? base.status : toNumber(record.status),
    createTime: toStringValue(record.createTime ?? record.timestamp, base.createTime ?? ''),
  }
}

function mapAuditLog(raw: unknown): AuditLog {
  const record = (raw ?? {}) as Record<string, unknown>
  const base = record as unknown as Partial<AuditLog>
  return {
    ...base,
    id: toNumber(record.id),
    userId: record.userId == null ? base.userId : toNumber(record.userId),
    username: toStringValue(record.username, base.username ?? ''),
    auditType: record.auditType == null ? base.auditType : toStringValue(record.auditType),
    module: record.module == null ? base.module : toStringValue(record.module),
    action: toStringValue(record.action, base.action ?? ''),
    entity: toStringValue(record.entity ?? record.targetType, base.entity ?? base.targetType ?? ''),
    entityId: record.entityId == null && record.targetId == null ? base.entityId : toNumber(record.entityId ?? record.targetId),
    targetId: record.targetId == null ? base.targetId : toNumber(record.targetId),
    targetType: record.targetType == null ? base.targetType : toStringValue(record.targetType),
    oldValue: record.oldValue == null && record.beforeValue == null ? base.oldValue : toStringValue(record.oldValue ?? record.beforeValue),
    newValue: record.newValue == null && record.afterValue == null ? base.newValue : toStringValue(record.newValue ?? record.afterValue),
    beforeValue: record.beforeValue == null ? base.beforeValue : toStringValue(record.beforeValue),
    afterValue: record.afterValue == null ? base.afterValue : toStringValue(record.afterValue),
    ip: toStringValue(record.ip, base.ip ?? ''),
    userAgent: toStringValue(record.userAgent, base.userAgent ?? ''),
    status: record.status == null ? base.status : toNumber(record.status),
    errorMsg: record.errorMsg == null && record.errorMessage == null ? base.errorMsg : toStringValue(record.errorMsg ?? record.errorMessage),
    createTime: toStringValue(record.createTime ?? record.timestamp, base.createTime ?? ''),
  }
}

export const logApi = {
  list: (params: OperationLogQuery) =>
    request.post<unknown>('/log/operation/page', params)
      .then((data) => normalizePage(data, mapOperationLog, params.page ?? 0, params.rows ?? 20)),
  systemList: (params: SystemLogQuery) =>
    request.post<unknown>('/log/system/page', params)
      .then((data) => normalizePage(data, mapSystemLog, params.page ?? 0, params.rows ?? 20)),
  auditLogPage: (params: AuditLogQuery) =>
    request.post<unknown>('/log/audit/search', params)
      .then((data) => normalizePage(data, mapAuditLog, params.page ?? 0, params.rows ?? 20)),
}
