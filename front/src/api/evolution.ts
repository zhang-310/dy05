/**
 * W-07: Knowledge Evolution API Client（历史规划草案）
 *
 * SSOT 说明（避免 404）：
 * - 进化引擎工作台（任务/主题/ROI/审核）：请用 `front/src/api/ai.ts`（aiApi），对应后端
 *   EvolutionController → `/api/v1/ai/evolution/*`。
 * - 知识库进化分析/报告/自动优化：后端为 KnowledgeEvolutionController →
 *   `/api/v1/ai/knowledge-evolution/*`；前端可用 aiApi.knowledgeEvolutionReport 等。
 *
 * 本文件内大量 `/ai/evolution/opportunities/*`、`/reports/search` 等路径**未**挂在
 * `EvolutionController` 上；新功能请勿复制这些 path。调用方应迁移到上述两处门面。
 */

import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  EvolutionOpportunityVO,
  EvolutionReportVO,
  EvolutionLogVO,
  RuleStatisticsVO,
  QualityMetricsVO,
  EvolutionOpportunitySearchVO,
  EvolutionReportSearchVO,
  EvolutionLogSearchVO,
  AutoOptimizeRequestVO,
  AutoOptimizeResultVO,
} from '@/types/evolution'

/**
 * 进化机会列表
 */
export function searchEvolutionOpportunities(data: EvolutionOpportunitySearchVO) {
  return request.post<PageResult<EvolutionOpportunityVO>>('/ai/evolution/opportunities/search', data || {})
}

/**
 * 获取单个进化机会详情
 */
export function getEvolutionOpportunity(id: number) {
  return request.post<EvolutionOpportunityVO>(`/ai/evolution/opportunities/${id}`, {})
}

/**
 * 确认和应用进化机会
 */
export function applyEvolutionOpportunity(id: number, data?: { comment?: string }) {
  return request.post<void>(`/ai/evolution/opportunities/${id}/apply`, data || {})
}

/**
 * 拒绝进化机会
 */
export function rejectEvolutionOpportunity(id: number, data?: { reason?: string }) {
  return request.post<void>(`/ai/evolution/opportunities/${id}/reject`, data || {})
}

/**
 * 进化报告列表
 */
export function searchEvolutionReports(data: EvolutionReportSearchVO) {
  return request.post<PageResult<EvolutionReportVO>>('/ai/evolution/reports/search', data || {})
}

/**
 * 获取单个进化报告详情
 */
export function getEvolutionReport(id: number) {
  return request.post<EvolutionReportVO>(`/ai/evolution/reports/${id}`, {})
}

/**
 * 生成进化报告（指定时间范围）
 */
export function generateEvolutionReport(data: { kbId?: number; startDate: string; endDate: string; reportType?: 'weekly' | 'monthly' }) {
  return request.post<EvolutionReportVO>('/ai/evolution/reports/generate', data)
}

/**
 * 导出进化报告（CSV/Excel/PDF）
 */
export function exportEvolutionReport(id: number, format: 'csv' | 'excel' | 'pdf') {
  return request.post<Blob>(`/ai/evolution/reports/${id}/export`, { format }, { responseType: 'blob' })
}

/**
 * 进化历史日志列表
 */
export function searchEvolutionLogs(data: EvolutionLogSearchVO) {
  return request.post<PageResult<EvolutionLogVO>>('/ai/evolution/logs/search', data || {})
}

/**
 * 获取最近的进化历史
 */
export function getRecentEvolutionLogs(limit = 50, kbId?: number) {
  return request.post<EvolutionLogVO[]>('/ai/evolution/logs/recent', { limit, kbId })
}

/**
 * 知识库质量指标
 */
export function getQualityMetrics(kbId?: number) {
  return request.post<QualityMetricsVO>('/ai/evolution/quality/metrics', { kbId })
}

/**
 * 质量指标历史趋势（用于绘图）
 */
export function getQualityMetricsTrend(data: {
  kbId?: number
  startDate: string
  endDate: string
  interval?: 'daily' | 'weekly' | 'monthly'
}) {
  return request.post<Array<{ date: string; score: number; completeness: number; accuracy: number; timeliness: number }>>(
    '/ai/evolution/quality/trend',
    data,
  )
}

/**
 * 规则统计列表
 */
export function getRuleStatistics(kbId?: number) {
  return request.post<RuleStatisticsVO[]>('/ai/evolution/rules/statistics', { kbId })
}

/**
 * 获取单个规则统计详情
 */
export function getRuleDetail(ruleId: number) {
  return request.post<RuleStatisticsVO>(`/ai/evolution/rules/${ruleId}/detail`, {})
}

/**
 * 启用/禁用规则
 */
export function toggleRule(ruleId: number, enabled: boolean) {
  return request.post<void>(`/ai/evolution/rules/${ruleId}/toggle`, { enabled })
}

/**
 * 更新规则配置
 */
export function updateRuleConfig(ruleId: number, config: Record<string, unknown>) {
  return request.post<void>(`/ai/evolution/rules/${ruleId}/config`, { config })
}

/**
 * 手动触发规则执行
 */
export function executeRule(ruleId: number, data?: { kbId?: number; dryRun?: boolean }) {
  return request.post<{ jobId: string }>(`/ai/evolution/rules/${ruleId}/execute`, data || {})
}

/**
 * 查询规则执行进度
 */
export function getRuleExecutionStatus(jobId: string) {
  return request.post<{
    jobId: string
    status: 'running' | 'success' | 'partial' | 'failed'
    progress: number // 百分比
    processedItems: number
    totalItems: number
    errorCount: number
    startTime: string
    endTime?: string
    summary?: string
  }>('/ai/evolution/rules/execution/status', { jobId })
}

/**
 * 自动优化知识库
 */
export function autoOptimizeKnowledge(data: AutoOptimizeRequestVO) {
  return request.post<AutoOptimizeResultVO>('/ai/evolution/optimize/auto', data)
}

/**
 * 查询优化进度
 */
export function getOptimizeProgress(jobId: string) {
  return request.post<{
    jobId: string
    status: 'running' | 'success' | 'partial' | 'failed'
    progress: number
    currentStep: string
    totalSteps: number
    startTime: string
    estimatedRemainTime?: number
    log?: string
  }>('/ai/evolution/optimize/progress', { jobId })
}

/**
 * 进化机会分析（根据时间周期）
 */
export function analyzeEvolutionOpportunities(data: {
  kbId?: number
  period?: 'daily' | 'weekly' | 'monthly'
  lookbackDays?: number
  limit?: number
}) {
  return request.post<{
    period: string
    totalOpportunities: number
    byType: Record<string, number>
    byPriority: Record<string, number>
    topOpportunities: EvolutionOpportunityVO[]
  }>('/ai/evolution/opportunities/analyze', data)
}

/**
 * 知识库进化状态摘要
 */
export function getEvolutionStatus(kbId?: number) {
  return request.post<{
    kbId?: number
    lastEvolutionTime: string
    nextScheduledTime?: string
    pendingOpportunities: number
    recentImprovements: string[]
    qualityScore: number
    completenessPercent: number
  }>('/ai/evolution/status', { kbId })
}

/**
 * 进化报告批量导出
 */
export function exportEvolutionReports(data: {
  startDate: string
  endDate: string
  format: 'csv' | 'excel' | 'pdf'
  includeDetails?: boolean
}) {
  return request.post<Blob>('/ai/evolution/reports/batch-export', data, { responseType: 'blob' })
}

/**
 * 获取进化推荐（AI 智能建议）
 */
export function getEvolutionRecommendations(kbId?: number) {
  return request.post<{
    recommendations: {
      id: string
      title: string
      description: string
      expectedBenefit: string
      requiredActions: string[]
      estimatedTime: number // 分钟
      priority: 'high' | 'medium' | 'low'
      confidence: number // 0-100
    }[]
  }>('/ai/evolution/recommendations', { kbId })
}
