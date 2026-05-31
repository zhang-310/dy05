import request from '@/utils/request'
import { isRecord, normalizeArray, parseJsonValue } from '@/utils/response-normalize'

type UnknownRecord = Record<string, unknown>

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
  [key: string]: unknown
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

function unwrapRecord(raw: unknown): UnknownRecord {
  const parsed = parseJsonValue(raw)
  if (!isRecord(parsed)) return {}

  for (const key of ['data', 'result', 'payload', 'item', 'record'] as const) {
    const next = parseJsonValue(parsed[key])
    if (isRecord(next)) return unwrapRecord(next)
  }

  return parsed
}

function textValue(raw: unknown, fallback = ''): string {
  const parsed = parseJsonValue(raw)
  if (parsed === null || parsed === undefined) return fallback
  if (typeof parsed === 'string') return parsed
  if (typeof parsed === 'number' || typeof parsed === 'boolean') return String(parsed)
  return fallback
}

function numberValue(raw: unknown, fallback = 0): number {
  const parsed = parseJsonValue(raw)
  if (typeof parsed === 'number' && Number.isFinite(parsed)) return parsed
  if (typeof parsed === 'boolean') return parsed ? 1 : 0
  if (typeof parsed === 'string') {
    const normalized = parsed.trim().replace('%', '')
    if (!normalized) return fallback
    const n = Number(normalized)
    return Number.isFinite(n) ? n : fallback
  }
  return fallback
}

function percentOrRateValue(raw: unknown, fallback = 0): number {
  const n = numberValue(raw, fallback)
  return Math.abs(n) > 1 ? n / 100 : n
}

function booleanValue(raw: unknown, fallback = false): boolean {
  const parsed = parseJsonValue(raw)
  if (typeof parsed === 'boolean') return parsed
  if (typeof parsed === 'number') return parsed !== 0
  if (typeof parsed === 'string') {
    const value = parsed.trim().toLowerCase()
    if (['true', '1', 'yes', 'y', 'enabled', 'available'].includes(value)) return true
    if (['false', '0', 'no', 'n', 'disabled', 'unavailable'].includes(value)) return false
  }
  return fallback
}

function timestampValue(raw: unknown, fallback = 0): number {
  const parsed = parseJsonValue(raw)
  if (typeof parsed === 'number' && Number.isFinite(parsed)) return parsed
  if (typeof parsed === 'string') {
    const direct = Number(parsed)
    if (Number.isFinite(direct)) return direct
    const time = new Date(parsed).getTime()
    return Number.isFinite(time) ? time : fallback
  }
  return fallback
}

function stringArray(raw: unknown): string[] {
  const parsed = parseJsonValue(raw)
  if (typeof parsed === 'string') {
    const trimmed = parsed.trim()
    if (!trimmed) return []
    if (trimmed.includes(',') || trimmed.includes('，')) {
      return trimmed.split(/[,，]/).map(item => item.trim()).filter(Boolean)
    }
    return [trimmed]
  }
  return normalizeArray<unknown>(parsed).map(item => textValue(item).trim()).filter(Boolean)
}

function arrayFrom<T>(raw: unknown, keys: string[] = []): T[] {
  const direct = normalizeArray<T>(raw)
  if (direct.length > 0) return direct
  const record = unwrapRecord(raw)
  for (const key of keys) {
    const rows = normalizeArray<T>(record[key])
    if (rows.length > 0 || Array.isArray(parseJsonValue(record[key]))) return rows
  }
  return []
}

function numericRecord(raw: unknown): Record<string, number> {
  const record = unwrapRecord(raw)
  return Object.fromEntries(
    Object.entries(record).map(([key, value]) => [key, numberValue(value)]),
  )
}

function normalizeTrendSignal(raw: unknown, index = 0): BrainTrendSignal {
  const row = unwrapRecord(raw)
  const title = textValue(row.title ?? row.keyword ?? row.hotword ?? row.topic ?? row.name ?? row.label, `趋势 ${index + 1}`)
  return {
    id: textValue(row.id ?? row.trendId ?? row.signalId ?? row.itemId ?? title, String(index + 1)),
    title,
    category: textValue(row.category ?? row.sourceCategory ?? row.platform ?? row.type),
    heatScore: numberValue(row.heatScore ?? row.hotScore ?? row.heat ?? row.score ?? row.value),
    detectedAt: timestampValue(row.detectedAt ?? row.detectTime ?? row.createTime ?? row.createdAt ?? row.timestamp),
    source: textValue(row.source ?? row.origin ?? row.platform ?? row.category),
    description: textValue(row.description ?? row.desc ?? row.summary ?? row.reason),
  }
}

function normalizeTrendSignals(raw: unknown): BrainTrendSignal[] {
  return arrayFrom<unknown>(raw, ['trends', 'signals']).map(normalizeTrendSignal)
}

function normalizeTrendPrediction(raw: unknown): BrainTrendPrediction {
  const row = unwrapRecord(raw)
  const signal = normalizeTrendSignal(row.signal ?? row.trend ?? row.trendSignal ?? row)
  const lifecycle = unwrapRecord(row.lifecycle ?? row.lifeCycle)
  const window = unwrapRecord(row.window ?? row.hotspotWindow ?? row.hotWindow)
  return {
    signal,
    lifecycle: {
      phase: textValue(lifecycle.phase ?? lifecycle.stage ?? lifecycle.lifecycle),
      momentum: numberValue(lifecycle.momentum),
      estimatedPeakHours: numberValue(lifecycle.estimatedPeakHours ?? lifecycle.peakHours),
      currentHeat: numberValue(lifecycle.currentHeat ?? signal.heatScore),
      predictedPeakHeat: numberValue(lifecycle.predictedPeakHeat ?? lifecycle.peakHeat),
    },
    window: {
      windowType: textValue(window.windowType ?? window.type ?? window.phase),
      remainingHours: numberValue(window.remainingHours ?? window.hoursLeft),
      advice: textValue(window.advice ?? window.suggestion ?? window.message),
    },
  }
}

function normalizeTrendPredictions(raw: unknown): BrainTrendPrediction[] {
  return arrayFrom<unknown>(raw, ['trends', 'predictions']).map(normalizeTrendPrediction)
}

function normalizeHostPersona(raw: unknown): BrainHostPersona {
  const row = unwrapRecord(raw)
  const id = numberValue(row.id ?? row.personaId ?? row.hostId)
  return {
    id,
    hostCode: textValue(row.hostCode ?? row.code ?? row.hostNo ?? id),
    hostName: textValue(row.hostName ?? row.name ?? row.nickname ?? row.hostCode, `主播 ${id}`),
    age: row.age == null ? undefined : numberValue(row.age),
    orientation: textValue(row.orientation),
    positioning: textValue(row.positioning ?? row.position ?? row.description),
    targetCategory: textValue(row.targetCategory ?? row.category),
    flowPhase: row.flowPhase == null ? undefined : numberValue(row.flowPhase),
  }
}

function normalizeHostPersonas(raw: unknown): BrainHostPersona[] {
  return normalizeArray<unknown>(raw).map(normalizeHostPersona)
}

function normalizeCausalResult(raw: unknown): BrainCausalInferenceResult {
  const row = unwrapRecord(raw)
  return {
    expectedConversionRate: percentOrRateValue(row.expectedConversionRate ?? row.conversionRate ?? row.expectedRate),
    keyFactors: stringArray(row.keyFactors ?? row.factors ?? row.drivers),
    riskPoints: stringArray(row.riskPoints ?? row.risks ?? row.warnings),
    explanation: textValue(row.explanation ?? row.summary ?? row.message),
  }
}

function normalizeProfile(raw: unknown): BrainCognitiveProfile {
  const row = unwrapRecord(raw)
  const rawPreferences = unwrapRecord(row.contentPreferences ?? row.preferences)
  const contentPreferences = Object.fromEntries(
    Object.entries(rawPreferences).map(([key, value]) => [key, percentOrRateValue(value)]),
  )
  return {
    userId: numberValue(row.userId ?? row.id ?? row.accountId),
    contentPreferences,
    expressionStyleTags: stringArray(row.expressionStyleTags ?? row.styleTags ?? row.tags),
    learningProgress: percentOrRateValue(row.learningProgress ?? row.progress),
    interactionPattern: unwrapRecord(row.interactionPattern ?? row.pattern),
    lastUpdatedAt: timestampValue(row.lastUpdatedAt ?? row.updateTime ?? row.updatedAt),
  }
}

function normalizeGraphNode(raw: unknown, index = 0): BrainGraphNode {
  const row = unwrapRecord(raw)
  const id = textValue(row.id ?? row.nodeId ?? row.key, `node-${index + 1}`)
  return {
    id,
    label: textValue(row.label ?? row.name ?? row.title ?? id),
    type: textValue(row.type ?? row.entityType ?? row.category, 'unknown'),
    weight: numberValue(row.weight ?? row.confidence ?? row.score ?? row.value, 1),
  }
}

function normalizeGraphEdge(raw: unknown): BrainGraphEdge {
  const row = unwrapRecord(raw)
  return {
    source: textValue(row.source ?? row.sourceNodeId ?? row.sourceId ?? row.from),
    target: textValue(row.target ?? row.targetNodeId ?? row.targetId ?? row.to),
    relation: textValue(row.relation ?? row.relationType ?? row.type),
    weight: numberValue(row.weight ?? row.confidence ?? row.score, 0.5),
  }
}

function normalizeGraphSubgraph(raw: unknown): BrainGraphSubgraph {
  const row = unwrapRecord(raw)
  return {
    nodes: normalizeArray<unknown>(row.nodes ?? row.nodeList).map(normalizeGraphNode),
    edges: normalizeArray<unknown>(row.edges ?? row.links ?? row.edgeList).map(normalizeGraphEdge),
    contradictions: normalizeArray<unknown>(row.contradictions),
  }
}

function normalizeGraphRag(raw: unknown): BrainGraphRagResponse {
  const row = unwrapRecord(raw)
  const hops = row.hops ?? row.maxHops
  return {
    context: textValue(row.context ?? row.text ?? row.summary),
    available: row.available == null ? undefined : booleanValue(row.available),
    hops: hops == null ? undefined : numberValue(hops),
  }
}

function normalizeIndustryInsights(raw: unknown): BrainIndustryInsights {
  const row = unwrapRecord(raw)
  const result = { ...row } as BrainIndustryInsights
  result.行业分类 = textValue(row.行业分类 ?? row.category ?? row.industryCategory)
  result.趋势热点 = stringArray(row.趋势热点 ?? row.hotTopics ?? row.trendHotspots ?? row.topics)
  result.趋势来源 = stringArray(row.趋势来源 ?? row.trendSources ?? row.sources)
  result.近7天竞品洞察 = stringArray(row.近7天竞品洞察 ?? row.competitorInsights ?? row.recentInsights)
  result.差异化建议 = textValue(row.差异化建议 ?? row.differentiationAdvice ?? row.advice)
  result.用户偏好焦点 = stringArray(row.用户偏好焦点 ?? row.profileFocus ?? row.userFocus)
  result.优先动作 = stringArray(row.优先动作 ?? row.actions ?? row.nextActions)
  result.数据口径 = textValue(row.数据口径 ?? row.dataScope ?? row.scope)
  return result
}

function normalizeAccountDiagnosis(raw: unknown): BrainAccountDiagnosisResult {
  const row = unwrapRecord(raw)
  return {
    accountId: numberValue(row.accountId ?? row.id),
    positioningClarity: percentOrRateValue(row.positioningClarity ?? row.positioningScore),
    contentCompetitiveness: percentOrRateValue(row.contentCompetitiveness ?? row.contentScore),
    growthHealth: percentOrRateValue(row.growthHealth ?? row.growthScore),
    riskLevel: textValue(row.riskLevel ?? row.risk),
    suggestedPriorities: stringArray(row.suggestedPriorities ?? row.priorities ?? row.actions),
    summary: textValue(row.summary ?? row.message ?? row.analysis),
    estimated: row.estimated == null ? undefined : booleanValue(row.estimated),
    isEstimated: row.isEstimated == null ? undefined : booleanValue(row.isEstimated),
  }
}

function normalizeLlmDiagnosis(raw: unknown): BrainLlmDiagnosisResult {
  return unwrapRecord(raw) as BrainLlmDiagnosisResult
}

function normalizeRiskItem(raw: unknown): BrainRiskItem {
  const row = unwrapRecord(raw)
  return {
    level: numberValue(row.level ?? row.riskLevel ?? row.severity, 1),
    type: textValue(row.type ?? row.riskType ?? row.category, 'unknown'),
    message: textValue(row.message ?? row.text ?? row.content ?? row.summary),
    startOffset: numberValue(row.startOffset ?? row.start ?? row.startIndex),
    endOffset: numberValue(row.endOffset ?? row.end ?? row.endIndex),
    suggestion: textValue(row.suggestion ?? row.advice ?? row.recommendation),
  }
}

function normalizeRiskItems(raw: unknown): BrainRiskItem[] {
  return arrayFrom<unknown>(raw, ['risks', 'riskItems', 'violations']).map(normalizeRiskItem)
}

function normalizeStrategicPlan(raw: unknown): BrainStrategicPlan {
  const row = unwrapRecord(raw)
  return {
    accountId: numberValue(row.accountId ?? row.id),
    industryAnalysis: textValue(row.industryAnalysis ?? row.industry),
    competitorAnalysis: textValue(row.competitorAnalysis ?? row.competitor),
    opportunityPoints: stringArray(row.opportunityPoints ?? row.opportunities),
    swotScores: numericRecord(row.swotScores ?? row.swot),
    contentMatrix: normalizeArray<unknown>(row.contentMatrix).map((item) => {
      const matrix = unwrapRecord(item)
      return {
        type: textValue(matrix.type ?? matrix.contentType),
        strategy: textValue(matrix.strategy ?? matrix.advice),
        priority: numberValue(matrix.priority ?? matrix.sortOrder),
      }
    }),
    growthPhases: normalizeArray<unknown>(row.growthPhases ?? row.phases).map((item) => {
      const phase = unwrapRecord(item)
      return {
        phase: textValue(phase.phase ?? phase.phaseName),
        goal: textValue(phase.goal ?? phase.target),
        strategies: stringArray(phase.strategies ?? phase.actions),
      }
    }),
    diagnoses: normalizeArray<unknown>(row.diagnoses).map((item) => {
      const diagnosis = unwrapRecord(item)
      return {
        title: textValue(diagnosis.title ?? diagnosis.name),
        detail: textValue(diagnosis.detail ?? diagnosis.description ?? diagnosis.summary),
      }
    }),
  }
}

function normalizeGrowthPath(raw: unknown): BrainGrowthPathResult {
  const row = unwrapRecord(raw)
  return {
    summary: textValue(row.summary ?? row.description),
    criticalSuccessFactors: stringArray(row.criticalSuccessFactors ?? row.factors),
    phases: normalizeArray<unknown>(row.phases).map((item) => {
      const phase = unwrapRecord(item)
      return {
        phaseOrder: numberValue(phase.phaseOrder ?? phase.order),
        phaseName: textValue(phase.phaseName ?? phase.phase),
        targetFans: numberValue(phase.targetFans ?? phase.fansTarget),
        strategies: stringArray(phase.strategies ?? phase.actions),
        keyMetrics: stringArray(phase.keyMetrics ?? phase.metrics),
        estimatedDuration: textValue(phase.estimatedDuration ?? phase.duration),
      }
    }),
  }
}

function normalizeRelationSuggestion(raw: unknown): BrainRelationSuggestion {
  const row = unwrapRecord(raw)
  return {
    id: numberValue(row.id ?? row.suggestionId),
    ownerId: row.ownerId == null ? undefined : numberValue(row.ownerId ?? row.userId),
    sourceEntityKey: textValue(row.sourceEntityKey ?? row.source ?? row.sourceKey),
    targetEntityKey: textValue(row.targetEntityKey ?? row.target ?? row.targetKey),
    relationType: textValue(row.relationType ?? row.relation ?? row.type),
    confidence: row.confidence == null ? undefined : percentOrRateValue(row.confidence),
    status: textValue(row.status),
    evidenceJson: row.evidenceJson == null ? null : textValue(row.evidenceJson),
    createTime: textValue(row.createTime ?? row.createdAt),
  }
}

function normalizeRelationSuggestions(raw: unknown): BrainRelationSuggestion[] {
  return arrayFrom<unknown>(raw, ['suggestions', 'relationSuggestions']).map(normalizeRelationSuggestion)
}

function normalizeInserted(raw: unknown): { inserted: number } {
  const row = unwrapRecord(raw)
  return { inserted: numberValue(row.inserted ?? row.count ?? row.total) }
}

function normalizeCounterfactual(raw: unknown): BrainCounterfactualResult {
  const row = unwrapRecord(raw)
  return {
    expectedConversionRateBefore: row.expectedConversionRateBefore == null ? undefined : percentOrRateValue(row.expectedConversionRateBefore),
    expectedConversionRateAfter: row.expectedConversionRateAfter == null ? undefined : percentOrRateValue(row.expectedConversionRateAfter),
    conversionRateDelta: row.conversionRateDelta == null ? undefined : percentOrRateValue(row.conversionRateDelta),
    impactPath: stringArray(row.impactPath ?? row.path),
    confidenceLower: row.confidenceLower == null ? undefined : percentOrRateValue(row.confidenceLower),
    confidenceUpper: row.confidenceUpper == null ? undefined : percentOrRateValue(row.confidenceUpper),
    suggestion: textValue(row.suggestion ?? row.advice),
  }
}

function normalizeExplainStrategy(raw: unknown): { strategyId: string; explanation: string } {
  const row = unwrapRecord(raw)
  return {
    strategyId: textValue(row.strategyId ?? row.id),
    explanation: textValue(row.explanation ?? row.message),
  }
}

function normalizeRiskStats(raw: unknown): BrainRiskStats {
  const row = unwrapRecord(raw)
  return {
    totalChecks: numberValue(row.totalChecks ?? row.total ?? row.count),
    violationCount: numberValue(row.violationCount ?? row.violations),
    accuracyEstimate: percentOrRateValue(row.accuracyEstimate ?? row.accuracy),
  }
}

function normalizeStyleConsistency(raw: unknown): { prompt: string; styleVector?: unknown; score: number } {
  const row = unwrapRecord(raw)
  return {
    prompt: textValue(row.prompt),
    styleVector: row.styleVector,
    score: percentOrRateValue(row.score, 1),
  }
}

export function brainTrendsCurrent(params?: { category?: string | null; limit?: number }) {
  return brainApi.trendsCurrent(params)
}

export const brainApi = {
  trendsCurrent: (params?: { category?: string | null; limit?: number }) =>
    request.post<unknown>('/ai/brain/trends/current', params ?? {}).then(normalizeTrendSignals),

  trendsWithLifecycle: (params?: { category?: string | null; limit?: number }) =>
    request.post<unknown>('/ai/brain/trends/with-lifecycle', params ?? {}).then(normalizeTrendPredictions),

  trendsForHost: (params?: { hostCode?: string | null; limit?: number }) =>
    request.post<unknown>('/ai/brain/trends/for-host', params ?? {}).then(normalizeTrendSignals),

  hostPersonas: () => request.post<unknown>('/ai/brain/host-personas', {}).then(normalizeHostPersonas),

  causalInfer: (params: Record<string, unknown>) =>
    request.post<unknown>('/ai/brain/causal/infer', params).then(normalizeCausalResult),

  userProfile: (params?: { accountId?: number }) =>
    request.post<unknown>('/ai/brain/user-profile', params ?? {}).then(normalizeProfile),

  accountDiagnose: (accountId: number) =>
    request.post<unknown>('/ai/brain/account/diagnose', { accountId }).then(normalizeAccountDiagnosis),

  contentDiagnosis: (params: { category?: string }) =>
    request.post<unknown>('/ai/brain/content-diagnosis', params).then(normalizeLlmDiagnosis),

  productDiagnosis: () =>
    request.post<unknown>('/ai/brain/product-diagnosis', {}).then(normalizeLlmDiagnosis),

  rhythmDiagnosis: () =>
    request.post<unknown>('/ai/brain/rhythm-diagnosis', {}).then(normalizeLlmDiagnosis),

  knowledgeGraphSubgraph: (params?: { query?: string; limit?: number }) =>
    request.post<unknown>('/ai/brain/knowledge-graph/subgraph-json', params ?? {}).then(normalizeGraphSubgraph),

  knowledgeGraphQuery: (params?: { entityType?: string; keyword?: string; limit?: number }) =>
    request.post<unknown>('/ai/brain/knowledge-graph/query', params ?? {}).then(data => normalizeArray<Record<string, unknown>>(data)),

  graphRagContext: (params: { query: string; limit?: number }) =>
    request.post<unknown>('/ai/brain/knowledge-graph/graphrag-context', params).then(normalizeGraphRag),

  relationSuggestionsList: (params?: { status?: string; limit?: number }) =>
    request.post<unknown>('/ai/brain/knowledge-graph/relation-suggestions/list', params ?? {}).then(normalizeRelationSuggestions),

  relationSuggestionsMaterialize: (pairs: Array<Record<string, unknown>>) =>
    request.post<unknown>('/ai/brain/knowledge-graph/relation-suggestions/materialize', { pairs }).then(normalizeInserted),

  relationSuggestionsUpdateStatus: (id: number, status: 'approved' | 'rejected' | 'pending') =>
    request.post<null>('/ai/brain/knowledge-graph/relation-suggestions/update-status', { id, status }),

  industryInsights: (category: string) =>
    request.post<unknown>('/ai/brain/industry/insights', { category }).then(normalizeIndustryInsights),

  riskWarn: (content: string) =>
    request.post<unknown>('/ai/brain/risk/warn', { content }).then(normalizeRiskItems),

  strategicPlan: (params: { accountId?: number | null; goals: string[] }) =>
    request.post<unknown>('/ai/brain/strategic/plan', params).then(normalizeStrategicPlan),

  growthPath: (params: {
    accountId?: number | null
    currentState?: Record<string, unknown>
    targetFans?: number
  }) => request.post<unknown>('/ai/brain/growth-path', params).then(normalizeGrowthPath),

  causalCounterfactual: (params: {
    currentState: Record<string, unknown>
    intervention: Record<string, unknown>
  }) => request.post<unknown>('/ai/brain/causal/counterfactual', params).then(normalizeCounterfactual),

  causalExplainStrategy: (params: { strategyId: string; context?: Record<string, unknown> }) =>
    request.post<unknown>('/ai/brain/causal/explain-strategy', params).then(normalizeExplainStrategy),

  trendsDetectNew: () => request.post<unknown>('/ai/brain/trends/detect-new', {}).then(normalizeTrendSignals),

  riskWarnBatch: (contents: string[]) =>
    request.post<unknown>('/ai/brain/risk/warn-batch', { contents }).then(normalizeRiskItems),

  riskStats: () => request.post<unknown>('/ai/brain/risk/stats', {}).then(normalizeRiskStats),

  synergy: () => request.post<unknown>('/ai/brain/synergy', {}).then(unwrapRecord),

  styleConsistency: (params: { hostCode?: string; content?: string }) =>
    request.post<unknown>('/ai/brain/style-consistency', params).then(normalizeStyleConsistency),

  ipGrowthStage: (params: { ipType?: string; followerCount?: number; operatingMonths?: number }) =>
    request.post<unknown>('/ai/brain/ip-growth-stage', params).then(unwrapRecord),

  ipMetricsBaseline: (params: { ipType?: string }) =>
    request.post<unknown>('/ai/brain/ip-metrics-baseline', params).then(unwrapRecord),
}
