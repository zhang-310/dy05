/**
 * W-07: Knowledge Evolution Types
 * 话术知识库自进化引擎相关类型定义
 */

import { BasicQuery } from './common'

/**
 * 进化机会 VO
 */
export interface EvolutionOpportunityVO {
  id: number
  kbId: number
  type: 'high_effectiveness' | 'low_effectiveness' | 'duplicate_candidate' | 'quality_gap' | 'timeliness' | 'cross_domain'
  typeLabel: string
  title: string
  description: string
  scriptId?: number
  scriptContent?: string
  relatedScripts?: string[] // 相关话术列表
  confidenceScore: number // 置信度 0-100
  priority: 'high' | 'medium' | 'low' // 优先级
  actionRequired: string // 建议操作
  estimatedImpact: string // 预期效果
  createdAt: string
  updatedAt: string
  status: 'pending' | 'processing' | 'completed' | 'rejected'
}

/**
 * 进化报告 VO
 */
export interface EvolutionReportVO {
  id: number
  kbId: number
  reportType: 'weekly' | 'monthly' // 周报或月报
  period: string // 时间周期 e.g., "2026-03-01 ~ 2026-03-07"
  startDate: string
  endDate: string
  // 统计信息
  totalScripts: number
  newScripts: number
  removedScripts: number
  deduplicatedCount: number
  qualityImprovementPercent: number
  effectivenessGain: number // 效率提升百分比
  // 规则执行统计
  ruleExecutionStats: {
    ruleName: string
    timesExecuted: number
    itemsProcessed: number
    successRate: number
  }[]
  // 关键指标
  metrics: {
    avgEffectivenessScore: number
    scriptQualityScore: number
    knowledgeCompleteness: number
    topicCoverage: number
  }
  // 最佳实践和建议
  bestPractices: string[]
  recommendations: string[]
  highlights: string[] // 亮点总结
  // 趋势数据
  trendsData: {
    date: string
    scriptsAdded: number
    scriptsRemoved: number
    averageScore: number
  }[]
  createdAt: string
  updatedAt: string
}

/**
 * 进化历史日志 VO
 */
export interface EvolutionLogVO {
  id: number
  kbId: number
  actionType: 'add_script' | 'remove_script' | 'dedup' | 'rule_execute' | 'manual_review' | 'quality_check'
  actionLabel: string
  scriptId?: number
  scriptContent?: string
  ruleId?: number
  ruleName?: string
  affectedItems: number
  resultSummary: string
  executorId?: number
  executorName?: string
  status: 'success' | 'partial' | 'failed'
  errorMessage?: string
  performanceMs: number // 执行耗时（毫秒）
  createdAt: string
}

/**
 * 规则执行统计 VO
 */
export interface RuleStatisticsVO {
  id: number
  ruleId: number
  ruleName: string
  ruleDescription: string
  category: 'effectiveness' | 'quality' | 'dedup' | 'timeliness' | 'completeness' | 'cross_domain'
  categoryLabel: string
  isEnabled: boolean
  totalExecutions: number
  successfulExecutions: number
  successRate: number // 成功率百分比
  totalItemsProcessed: number
  averageExecutionTimeMs: number
  lastExecutedAt: string
  nextScheduledAt: string
  impactedScripts: number
  qualityImprovement: number // 质量提升百分比
  config: Record<string, unknown> // 规则配置
}

/**
 * 知识库质量评分 VO
 */
export interface QualityMetricsVO {
  kbId: number
  overallScore: number // 综合评分 0-100
  completenessScore: number // 完整性评分
  accuracyScore: number // 准确性评分
  timelinessScore: number // 时效性评分
  coverageScore: number // 覆盖度评分
  qualityTrend: 'improving' | 'stable' | 'declining' // 趋势
  trendsData: {
    date: string
    score: number
  }[]
  // 问题统计
  issues: {
    type: string
    count: number
    severity: 'low' | 'medium' | 'high'
  }[]
  // 规则执行状态
  ruleExecutionStatus: {
    ruleName: string
    status: 'running' | 'success' | 'failed' | 'pending'
    lastExecutedAt: string
  }[]
  lastUpdatedAt: string
}

/**
 * 进化机会查询 VO
 */
export interface EvolutionOpportunitySearchVO extends BasicQuery {
  kbId?: number
  type?: string
  priority?: string
  status?: string
  keyword?: string
  startDate?: string
  endDate?: string
}

/**
 * 进化报告查询 VO
 */
export interface EvolutionReportSearchVO extends BasicQuery {
  kbId?: number
  reportType?: string
  startDate?: string
  endDate?: string
}

/**
 * 进化历史查询 VO
 */
export interface EvolutionLogSearchVO extends BasicQuery {
  kbId?: number
  actionType?: string
  status?: string
  keyword?: string
  startDate?: string
  endDate?: string
}

/**
 * 自动优化请求 VO
 */
export interface AutoOptimizeRequestVO {
  kbId?: number
  includeDedup?: boolean
  includeQualityCheck?: boolean
  includeTimeliness?: boolean
  includeCompleteness?: boolean
  dryRun?: boolean // 演练模式，不真实执行
}

/**
 * 自动优化结果 VO
 */
export interface AutoOptimizeResultVO {
  jobId: string
  status: 'running' | 'success' | 'partial' | 'failed'
  startTime: string
  endTime?: string
  durationMs?: number
  summary: string
  details: {
    dedup?: {
      processed: number
      removed: number
      merged: number
    }
    quality?: {
      processed: number
      improved: number
      issues: number
    }
    timeliness?: {
      processed: number
      outdated: number
      updated: number
    }
    completeness?: {
      gapsFound: number
      gapsFilled: number
    }
  }
  errors?: string[]
}
