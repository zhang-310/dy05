/**
 * 与后端 KnowledgeEvolutionController.analyze / auto-optimize 对齐
 * （Java EvolutionOpportunityVO 聚合结构，非 types/evolution.ts 扁平机会）
 */

export type KnowledgeEvolutionAnalysisScope =
  | 'LAST_7_DAYS'
  | 'LAST_14_DAYS'
  | 'LAST_30_DAYS'
  | 'LAST_90_DAYS'

export interface KnowledgeEvolutionAnalyzeRequest {
  analysisScope?: KnowledgeEvolutionAnalysisScope
  includeArchived?: boolean
}

export interface EvolutionAnalysisExpectedImpact {
  newInclusionsCount?: number
  deduplicationCount?: number
  improvementRate?: number
}

export interface EvolutionScriptOpportunityRow {
  scriptVersionId?: number
  title?: string
  score?: number
  usageCount?: number
  reason?: string
}

export interface EvolutionOptimizationOpportunityRow {
  scriptVersionId?: number
  title?: string
  score?: number
  consecutiveLowScore?: number
  suggestion?: string
  referenceScriptId?: number
}

export interface EvolutionDuplicateGroupRow {
  masterScriptId?: number
  masterTitle?: string
  masterScore?: number
  duplicateScriptIds?: number[]
  similarityScore?: number
  recommendation?: string
}

export interface EvolutionArchivalOpportunityRow {
  scriptVersionId?: number
  title?: string
  currentScore?: number
  reason?: string
  monthsSinceDeprecation?: number
}

/** POST /ai/knowledge-evolution/analyze 响应体（data 解包后） */
export interface EvolutionAnalysisResultVO {
  analysisId?: string
  periodStart?: string
  periodEnd?: string
  readyForInclusion?: EvolutionScriptOpportunityRow[]
  needsOptimization?: EvolutionOptimizationOpportunityRow[]
  duplicatesDetected?: EvolutionDuplicateGroupRow[]
  readyForArchival?: EvolutionArchivalOpportunityRow[]
  expectedImpact?: EvolutionAnalysisExpectedImpact
  createdAt?: string
  /** 后端规则引擎未注入时为 true */
  degraded?: boolean
}

export interface KnowledgeEvolutionAutoOptimizeActions {
  autoInclude?: boolean
  autoMerge?: boolean
  autoArchive?: boolean
}

export interface KnowledgeEvolutionAutoOptimizeRequest {
  analysisId: string
  actions?: KnowledgeEvolutionAutoOptimizeActions
  approvalRequired?: boolean
}

/** POST /ai/knowledge-evolution/auto-optimize 响应（data 解包后，字段以后端为准） */
export interface KnowledgeEvolutionAutoOptimizeResult {
  executionId?: string
  status?: string
  results?: Record<string, { count?: number; scriptIds?: number[] }>
  summary?: Record<string, unknown>
  executedAt?: string
  degraded?: boolean
}

export interface KnowledgeEvolutionReportOverview {
  totalScriptsInLibrary?: number
  newAddedCount?: number
  archivedCount?: number
  deduplicatedCount?: number
  averageScore?: number
}

export interface KnowledgeEvolutionTopScript {
  rank?: number
  scriptId?: number
  title?: string
  score?: number
  usageCount?: number
  adoptionRate?: number
}

export interface KnowledgeEvolutionStyleAnalysis {
  count?: number
  averageScore?: number
  trend?: string
}

export interface KnowledgeEvolutionRecommendation {
  type?: string
  description?: string
  priority?: string
}

/** POST /ai/knowledge-evolution/report 响应（data 解包后） */
export interface KnowledgeEvolutionReportVO {
  reportId?: string
  period?: string
  overview?: KnowledgeEvolutionReportOverview
  topScripts?: KnowledgeEvolutionTopScript[]
  styleAnalysis?: Record<string, KnowledgeEvolutionStyleAnalysis>
  recommendations?: KnowledgeEvolutionRecommendation[]
  generatedAt?: string
}
