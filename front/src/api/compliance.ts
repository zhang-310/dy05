import request from '@/utils/request'
import { normalizeArray, normalizeRows } from '@/utils/response-normalize'

type UnknownRecord = Record<string, unknown>

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null
}

function toStringValue(value: unknown, fallback = ''): string {
  if (value == null) return fallback
  return String(value)
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

export interface ComplianceCheckResult {
  result: string
  riskScore: number
  matchedRules: {
    ruleCode?: string
    ruleName?: string
    severity?: string
    matchReason?: string
    punishment?: string
  }[]
  suggestions?: string
  checkDurationMs?: number
}

export interface ComplianceRule {
  id: number
  word: string
  ruleType: string
  industry: string
  suggestion: string
  enabled: boolean
  createTime: string
}

export interface ScriptComplianceViolation {
  matchedText: string
  level: string
  reason: string
  reference: string
  position: number
  source?: string
}

export interface ScriptComplianceRule {
  pattern: string
  level: string
  reason: string
  reference: string
  source?: string
}

export interface ScriptComplianceIndustryCodes {
  verticalCodes: string[]
  defaultVerticalCode: string
  note?: string
}

export interface DouyinOfficialReferences {
  notice?: string
  referenceUrls: string[]
  hint?: string
}

function normalizeScriptViolation(row: unknown): ScriptComplianceViolation {
  const record = isRecord(row) ? row : {}
  return {
    matchedText: toStringValue(record.matchedText ?? record.word),
    level: toStringValue(record.level ?? record.severity, 'warning'),
    reason: toStringValue(record.reason ?? record.matchReason ?? record.suggestion),
    reference: toStringValue(record.reference ?? record.punishment),
    position: toNumber(record.position),
    source: record.source == null ? undefined : toStringValue(record.source),
  }
}

function normalizeScriptRule(row: unknown): ScriptComplianceRule {
  const record = isRecord(row) ? row : {}
  return {
    pattern: toStringValue(record.pattern),
    level: toStringValue(record.level, 'warning'),
    reason: toStringValue(record.reason),
    reference: toStringValue(record.reference),
    source: record.source == null ? undefined : toStringValue(record.source),
  }
}

function normalizeIndustryCodes(data: unknown): ScriptComplianceIndustryCodes {
  const record = isRecord(data) && isRecord(data.data) ? data.data : isRecord(data) ? data : {}
  return {
    verticalCodes: normalizeArray<string>(record.verticalCodes ?? record.codes ?? record.items ?? data).map((v) => String(v)),
    defaultVerticalCode: toStringValue(record.defaultVerticalCode, 'cosmetics'),
    note: record.note == null ? undefined : toStringValue(record.note),
  }
}

function normalizeReferences(data: unknown): DouyinOfficialReferences {
  const record = isRecord(data) && isRecord(data.data) ? data.data : isRecord(data) ? data : {}
  return {
    notice: record.notice == null ? undefined : toStringValue(record.notice),
    referenceUrls: normalizeArray<string>(record.referenceUrls ?? record.urls ?? record.items ?? record.list).map((v) => String(v)),
    hint: record.hint == null ? undefined : toStringValue(record.hint),
  }
}

export function complianceCheck(text: string, type?: string) {
  return request.post<ComplianceCheckResult>('/compliance/check', {
    content: text,
    contentType: type ?? 'script',
  })
}

export function complianceStats() {
  return request.post<{
    totalChecks: number
    violationRate: number
    topViolations: { word: string; count: number }[]
  }>('/compliance/stats', {})
}

export function complianceRuleList(params?: Record<string, unknown>) {
  return request.post<{ total: number; list: ComplianceRule[] }>('/compliance/rule/list', params || {})
}

export function scriptComplianceCheck(params: { text: string; industryCode?: string }) {
  return request.post<unknown>('/script/compliance/check', {
    text: params.text,
    industryCode: params.industryCode ?? 'cosmetics',
  }).then((rows) => normalizeRows<unknown>(rows).map(normalizeScriptViolation))
}

export function scriptComplianceRules(industryCode = 'cosmetics') {
  return request.post<unknown>('/script/compliance/rules', { industryCode })
    .then((rows) => normalizeRows<unknown>(rows).map(normalizeScriptRule))
}

export function scriptComplianceIndustryCodes() {
  return request.post<unknown>('/script/compliance/industry-codes', {}).then(normalizeIndustryCodes)
}

export function douyinOfficialReferences() {
  return request.post<unknown>('/script/compliance/douyin-official-references', {}).then(normalizeReferences)
}
