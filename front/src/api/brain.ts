import request from '@/utils/request'

/** 与后端 TrendMonitorService.TrendSignal 一致 */
export interface BrainTrendSignal {
  id: string
  title: string
  category: string
  heatScore: number
  detectedAt: number
  source: string
  description: string
}

export interface BrainTrendLifecycle {
  phase: string
  momentum: number
  estimatedPeakHours: number
  currentHeat: number
  predictedPeakHeat: number
}

export interface BrainHotspotWindow {
  windowType: string
  remainingHours: number
  advice: string
}

export interface BrainTrendPrediction {
  signal: BrainTrendSignal
  lifecycle: BrainTrendLifecycle
  window: BrainHotspotWindow
}

export interface BrainCausalInferenceResult {
  expectedConversionRate: number
  keyFactors: string[]
  riskPoints: string[]
  explanation: string
}

/** 与 UserCognitiveProfileService.UserProfile 一致 */
export interface BrainCognitiveProfile {
  userId: number
  contentPreferences: Record<string, number>
  expressionStyleTags: string[]
  learningProgress: number
  interactionPattern: Record<string, unknown>
  lastUpdatedAt: number
}

export interface BrainGraphNode {
  id: string
  label: string
  type: string
  weight: number
}

export interface BrainGraphEdge {
  source: string
  target: string
  relation: string
  weight: number
}

export interface BrainGraphSubgraph {
  nodes: BrainGraphNode[]
  edges: BrainGraphEdge[]
  contradictions?: unknown[]
}

/** 账号全方位诊断（AccountDiagnosisService.DiagnosisResult） */
export interface BrainAccountDiagnosisResult {
  accountId: number
  positioningClarity: number
  contentCompetitiveness: number
  growthHealth: number
  riskLevel: string
  suggestedPriorities: string[]
  summary: string
  /** Jackson 可能对 boolean 记录组件序列化为 estimated 或 isEstimated */
  estimated?: boolean
  isEstimated?: boolean
}

export interface BrainIndustryInsights {
  行业分类?: string
  趋势热点?: string[]
  趋势来源?: string[]
  近7天竞品洞察?: string[]
  差异化建议?: string
  用户偏好焦点?: string[]
  优先动作?: string[]
  数据口径?: string
}

export interface BrainHostPersona {
  id: number
  hostCode: string
  hostName: string
  age?: number
  orientation?: string
  positioning?: string
  targetCategory?: string
  flowPhase?: number
}

export interface BrainRiskItem {
  level: number
  type: string
  message: string
  startOffset: number
  endOffset: number
  suggestion: string
}

export interface BrainStrategicPlan {
  accountId: number
  industryAnalysis?: string
  competitorAnalysis?: string
  opportunityPoints?: string[]
  swotScores?: Record<string, number>
  contentMatrix?: Array<{ type: string; strategy: string; priority: number }>
  growthPhases?: Array<{ phase: string; goal: string; strategies: string[] }>
  diagnoses?: Array<{ title: string; detail: string }>
}

export interface BrainGrowthPathResult {
  phases?: Array<{
    phaseOrder: number
    phaseName: string
    targetFans: number
    strategies: string[]
    keyMetrics: string[]
    estimatedDuration: string
  }>
  summary?: string
  criticalSuccessFactors?: string[]
}

export interface BrainLlmDiagnosisResult {
  diagnosisType?: string
  status?: string
  message?: string
  analysis?: string
  tokensUsed?: number
}

/** G-2 关系建议行（与 IndustryKnowledgeGraphServiceImpl#listRelationSuggestions 一致） */
export interface BrainRelationSuggestion {
  id: number
  ownerId?: number
  sourceEntityKey: string
  targetEntityKey: string
  relationType: string
  confidence?: number
  status?: string
  evidenceJson?: string | null
  createTime?: string
}

export interface BrainGraphRagResponse {
  context: string
  available?: boolean
  hops?: number
}

export interface BrainCounterfactualResult {
  expectedConversionRateBefore?: number
  expectedConversionRateAfter?: number
  conversionRateDelta?: number
  impactPath?: string[]
  confidenceLower?: number
  confidenceUpper?: number
  suggestion?: string
}

export interface BrainRiskStats {
  totalChecks: number
  violationCount: number
  accuracyEstimate: number
}

export function brainTrendsCurrent(params?: { category?: string | null; limit?: number }) {
  return brainApi.trendsCurrent(params)
}

export const brainApi = {
  trendsCurrent: (params?: { category?: string | null; limit?: number }) =>
    request.post<BrainTrendSignal[]>('/ai/brain/trends/current', params ?? {}),

  trendsWithLifecycle: (params?: { category?: string | null; limit?: number }) =>
    request.post<BrainTrendPrediction[]>('/ai/brain/trends/with-lifecycle', params ?? {}),

  trendsForHost: (params?: { hostCode?: string | null; limit?: number }) =>
    request.post<BrainTrendSignal[]>('/ai/brain/trends/for-host', params ?? {}),

  hostPersonas: () => request.post<BrainHostPersona[]>('/ai/brain/host-personas', {}),

  causalInfer: (params: Record<string, unknown>) =>
    request.post<BrainCausalInferenceResult>('/ai/brain/causal/infer', params),

  userProfile: (params?: { accountId?: number }) =>
    request.post<BrainCognitiveProfile>('/ai/brain/user-profile', params ?? {}),

  accountDiagnose: (accountId: number) =>
    request.post<BrainAccountDiagnosisResult>('/ai/brain/account/diagnose', { accountId }),

  contentDiagnosis: (params: { category?: string }) =>
    request.post<BrainLlmDiagnosisResult>('/ai/brain/content-diagnosis', params),

  productDiagnosis: () =>
    request.post<BrainLlmDiagnosisResult>('/ai/brain/product-diagnosis', {}),

  rhythmDiagnosis: () =>
    request.post<BrainLlmDiagnosisResult>('/ai/brain/rhythm-diagnosis', {}),

  knowledgeGraphSubgraph: (params?: { query?: string; limit?: number }) =>
    request.post<BrainGraphSubgraph>('/ai/brain/knowledge-graph/subgraph-json', params ?? {}),

  knowledgeGraphQuery: (params?: { entityType?: string; keyword?: string; limit?: number }) =>
    request.post<Record<string, unknown>[]>('/ai/brain/knowledge-graph/query', params ?? {}),

  graphRagContext: (params: { query: string; limit?: number }) =>
    request.post<BrainGraphRagResponse>('/ai/brain/knowledge-graph/graphrag-context', params),

  relationSuggestionsList: (params?: { status?: string; limit?: number }) =>
    request.post<BrainRelationSuggestion[]>('/ai/brain/knowledge-graph/relation-suggestions/list', params ?? {}),

  relationSuggestionsMaterialize: (pairs: Array<Record<string, unknown>>) =>
    request.post<{ inserted: number }>('/ai/brain/knowledge-graph/relation-suggestions/materialize', { pairs }),

  relationSuggestionsUpdateStatus: (id: number, status: 'approved' | 'rejected' | 'pending') =>
    request.post<null>('/ai/brain/knowledge-graph/relation-suggestions/update-status', { id, status }),

  industryInsights: (category: string) =>
    request.post<BrainIndustryInsights>('/ai/brain/industry/insights', { category }),

  riskWarn: (content: string) =>
    request.post<BrainRiskItem[]>('/ai/brain/risk/warn', { content }),

  strategicPlan: (params: { accountId?: number | null; goals: string[] }) =>
    request.post<BrainStrategicPlan>('/ai/brain/strategic/plan', params),

  growthPath: (params: {
    accountId?: number | null
    currentState?: Record<string, unknown>
    targetFans?: number
  }) => request.post<BrainGrowthPathResult>('/ai/brain/growth-path', params),

  causalCounterfactual: (params: {
    currentState: Record<string, unknown>
    intervention: Record<string, unknown>
  }) => request.post<BrainCounterfactualResult>('/ai/brain/causal/counterfactual', params),

  causalExplainStrategy: (params: { strategyId: string; context?: Record<string, unknown> }) =>
    request.post<{ strategyId: string; explanation: string }>('/ai/brain/causal/explain-strategy', params),

  trendsDetectNew: () => request.post<BrainTrendSignal[]>('/ai/brain/trends/detect-new', {}),

  riskWarnBatch: (contents: string[]) =>
    request.post<BrainRiskItem[]>('/ai/brain/risk/warn-batch', { contents }),

  riskStats: () => request.post<BrainRiskStats>('/ai/brain/risk/stats', {}),

  synergy: () => request.post<Record<string, unknown>>('/ai/brain/synergy', {}),

  styleConsistency: (params: { hostCode?: string; content?: string }) =>
    request.post<{ prompt: string; styleVector?: unknown; score: number }>('/ai/brain/style-consistency', params),

  ipGrowthStage: (params: { ipType?: string; followerCount?: number; operatingMonths?: number }) =>
    request.post<Record<string, unknown>>('/ai/brain/ip-growth-stage', params),

  ipMetricsBaseline: (params: { ipType?: string }) =>
    request.post<Record<string, unknown>>('/ai/brain/ip-metrics-baseline', params),
}
