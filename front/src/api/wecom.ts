import request from '@/utils/request'
import {
  isRecord,
  normalizeArray,
  normalizePage,
  parseJsonValue,
  readNumber,
} from '@/utils/response-normalize'

export interface WcRobot {
  id: number; robotName: string; webhookUrl: string
  robotType: string; description: string; status: number; createTime: string
  ruleCount?: number
}
export interface RobotQuery { page?: number; rows?: number; robotName?: string; status?: number }
export interface RobotSave { id?: number; robotName: string; webhookUrl: string; robotType?: string; description?: string; status?: number }

export interface WcRule {
  id: number
  robotId: number
  ruleName: string
  triggerType: string   // manual / schedule / event
  triggerConfig: string
  messageTemplate: string
  status: number
  createTime: string
}
export interface RuleQuery { page?: number; rows?: number; robotId?: number; triggerType?: string; status?: number }
export interface RuleSave { id?: number; robotId: number; ruleName: string; triggerType: string; triggerConfig?: string; messageTemplate: string; status?: number }

export interface WcPushLog {
  id: number
  robotId: number
  robotName?: string
  ruleId: number | null
  content?: string
  messageContent?: string
  messageType?: string
  status: number        // 0=失败 1=成功
  errMsg?: string | null
  errorMessage?: string | null
  sendTime?: string
  createTime: string
}
export interface PushLogQuery { page?: number; rows?: number; robotId?: number; status?: number; startTime?: string; endTime?: string }

export interface WcMessageTemplate {
  id: number
  robotId: number
  templateName: string
  templateContent: string   // 含 {变量名} 占位符
  variables: string[]       // 解析出的变量名列表
  exampleValues?: Record<string, string>
  status: number
  createTime: string
}

type UnknownRecord = Record<string, unknown>

function asRecord(value: unknown): UnknownRecord {
  const parsed = parseJsonValue(value)
  return isRecord(parsed) ? parsed : {}
}

function readString(record: UnknownRecord, keys: string[], fallback = ''): string {
  for (const key of keys) {
    const value = record[key]
    if (value != null && String(value).trim()) return String(value)
  }
  return fallback
}

function readNullableNumber(record: UnknownRecord, keys: string[]): number | null {
  for (const key of keys) {
    if (record[key] == null) continue
    const value = Number(record[key])
    if (Number.isFinite(value)) return value
  }
  return null
}

function normalizeRobot(raw: unknown): WcRobot {
  const record = asRecord(raw)
  return {
    ...(record as Partial<WcRobot>),
    id: readNumber(record, ['id', 'robotId'], 0),
    robotName: readString(record, ['robotName', 'name', 'robot_name']),
    webhookUrl: readString(record, ['webhookUrl', 'webhook', 'url']),
    robotType: readString(record, ['robotType', 'type'], 'custom'),
    description: readString(record, ['description', 'remark']),
    status: readNumber(record, ['status', 'enabled'], 0),
    createTime: readString(record, ['createTime', 'createdAt', 'created_time']),
    ruleCount: readNumber(record, ['ruleCount', 'rules', 'rule_count'], Number(record.ruleCount ?? 0)),
  }
}

function normalizeRule(raw: unknown): WcRule {
  const record = asRecord(raw)
  return {
    ...(record as Partial<WcRule>),
    id: readNumber(record, ['id', 'ruleId'], 0),
    robotId: readNumber(record, ['robotId', 'robotConfigId'], 0),
    ruleName: readString(record, ['ruleName', 'name', 'title']),
    triggerType: readString(record, ['triggerType', 'type'], 'manual'),
    triggerConfig: readString(record, ['triggerConfig', 'config'], '{}'),
    messageTemplate: readString(record, ['messageTemplate', 'templateContent', 'content']),
    status: readNumber(record, ['status', 'enabled'], 0),
    createTime: readString(record, ['createTime', 'createdAt', 'created_time']),
  }
}

function normalizePushLog(raw: unknown): WcPushLog {
  const record = asRecord(raw)
  const messageContent = readString(record, ['messageContent', 'content', 'summary'])
  const errorMessage = readString(record, ['errorMessage', 'errMsg', 'error'])
  return {
    ...(record as Partial<WcPushLog>),
    id: readNumber(record, ['id', 'logId'], 0),
    robotId: readNumber(record, ['robotId', 'robotConfigId'], 0),
    robotName: readString(record, ['robotName', 'robot_name']),
    ruleId: readNullableNumber(record, ['ruleId']),
    content: readString(record, ['content', 'messageContent', 'summary']),
    messageContent,
    messageType: readString(record, ['messageType', 'type'], 'text'),
    status: readNumber(record, ['status', 'success'], 0),
    errMsg: errorMessage,
    errorMessage,
    sendTime: readString(record, ['sendTime', 'sentAt']),
    createTime: readString(record, ['createTime', 'createdAt', 'created_time']),
  }
}

function filterRules(list: WcRule[], params?: RuleQuery): WcRule[] {
  return list.filter(rule =>
    (params?.robotId == null || rule.robotId === params.robotId) &&
    (!params?.triggerType || rule.triggerType === params.triggerType) &&
    (params?.status == null || rule.status === params.status)
  )
}

function normalizeRobotSave(params: Partial<RobotSave>): Partial<RobotSave> {
  return {
    id: params.id,
    robotName: params.robotName?.trim(),
    webhookUrl: params.webhookUrl?.trim(),
    robotType: params.robotType?.trim() || 'custom',
    description: params.description?.trim() || undefined,
    status: params.status ?? 1,
  }
}

function normalizeRuleSave(params: Partial<RuleSave>): Partial<RuleSave> {
  return {
    id: params.id,
    robotId: params.robotId,
    ruleName: params.ruleName?.trim(),
    triggerType: params.triggerType?.trim(),
    triggerConfig: params.triggerConfig?.trim() || '{}',
    messageTemplate: params.messageTemplate?.trim(),
    status: params.status ?? 1,
  }
}

export const wecomApi = {
  // 机器人
  list: (params: RobotQuery) =>
    request.post<unknown>('/wecom/robot/list', params)
      .then(raw => normalizePage<unknown, WcRobot>(raw, normalizeRobot, params.page ?? 0, params.rows ?? 20)),
  get: (id: number) => request.post<WcRobot>('/wecom/robot/get', undefined, { params: { id } }),
  save: (params: Partial<RobotSave>) => request.post<void>('/wecom/robot/save', normalizeRobotSave(params)),
  delete: (id: number) => request.post<void>('/wecom/robot/delete', undefined, { params: { id } }),
  updateStatus: (id: number, status: number) => request.post<void>('/wecom/robot/update-status', undefined, { params: { id, status } }),
  push: (params: { robotId: number; content?: string; messageContent?: string; messageType?: string; ruleId?: number | null }) =>
    request.post<void>('/wecom/push', {
      robotId: params.robotId,
      ruleId: params.ruleId ?? undefined,
      messageType: params.messageType ?? 'text',
      messageContent: params.messageContent ?? params.content ?? '',
    }),

  // 推送规则
  ruleList: (params?: RuleQuery) =>
    request.post<unknown>('/wecom/rule/list', {}).then(raw => {
      const normalized = normalizePage<unknown, WcRule>(raw, normalizeRule, params?.page ?? 0, params?.rows ?? 200)
      const filtered = filterRules(normalized.list, params)
      return {
        ...normalized,
        total: filtered.length,
        list: filtered,
        pageSize: params?.rows ?? normalized.pageSize,
      }
    }),
  ruleGet: (id: number) => request.post<WcRule>('/wecom/rule/get', undefined, { params: { id } }),
  ruleSave: (params: Partial<RuleSave>) => request.post<void>('/wecom/rule/save', normalizeRuleSave(params)),
  ruleDelete: (id: number) => request.post<void>('/wecom/rule/delete', undefined, { params: { id } }),
  ruleUpdateStatus: (id: number, status: number) => request.post<void>('/wecom/rule/update-status', undefined, { params: { id, status } }),

  // 推送日志
  logList: (params: PushLogQuery) =>
    request.post<unknown>('/wecom/log/list', params)
      .then(raw => normalizePage<unknown, WcPushLog>(raw, normalizePushLog, params.page ?? 0, params.rows ?? 20)),
}

export const wecomResponseNormalize = {
  robots: (raw: unknown, page = 0, rows = 20) => normalizePage<unknown, WcRobot>(raw, normalizeRobot, page, rows),
  rules: (raw: unknown, params?: RuleQuery) => {
    const normalized = normalizePage<unknown, WcRule>(raw, normalizeRule, params?.page ?? 0, params?.rows ?? 200)
    const filtered = filterRules(normalized.list, params)
    return { ...normalized, total: filtered.length, list: filtered, pageSize: params?.rows ?? normalized.pageSize }
  },
  logs: (raw: unknown, page = 0, rows = 20) => normalizePage<unknown, WcPushLog>(raw, normalizePushLog, page, rows),
  array: normalizeArray,
}
