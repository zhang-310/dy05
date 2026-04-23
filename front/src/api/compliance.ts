import request from '@/utils/request'

export interface ComplianceCheckResult {
  passed: boolean
  violations: { word: string; type: string; suggestion: string }[]
  riskLevel: 'low' | 'medium' | 'high'
  checkedAt: string
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

export function complianceCheck(text: string, type?: string) {
  return request.post<ComplianceCheckResult>('/compliance/check', { text, type })
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
