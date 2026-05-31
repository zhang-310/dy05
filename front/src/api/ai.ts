import request from '@/utils/request'
import {
  isRecord,
  normalizeArray as normalizeResponseArray,
  normalizePage as normalizeResponsePage,
  normalizeRecord,
  readNumberValue,
} from '@/utils/response-normalize'
import type { PageResult } from '@/types/common'
import type {
  AiModelVO,
  AiModelAdminVO,
  AiModelConnectionTestResult,
  AiModelSavePayload,
  AiLiveReviewVO,
  AiEvolveTaskVO,
  AiEvolveTopicVO,
  AiEvolveTopicSaveVO,
  AiKnowledgeSourceVO,
  AiQuotaHistoryVO,
  AiQuotaItemRow,
  AiTaskModelConfigRow,
  AiTaskModelConfigSavePayload,
  AiModelBenchmarkComparisonRow,
  AiModelBenchmarkBestModelVO,
  KbIndexQueueRow,
  EvolutionFitnessRecordVO,
  EvolutionFitnessListParams,
  AiMediaImageResult,
  AiMediaAudioResult,
  AiMediaVideoResult,
  AiMediaTtsVoiceInfo,
  AiMediaImageHistory,
  AiAdminQuotaOverviewVO,
  AiQuotaTrendItem,
  AiCallVolumeTrendItem,
  AiCallTypeDistributionItem,
  AiCostBreakdownItem,
  AiDashboardStatsVO,
  AiInfraHealthItem,
  AiCacheStatsVO,
  AiCacheDiagnosticsVO,
  AiSearchStatsVO,
  AiInfraDetailFullVO,
  AiMonitoringConfigVO,
  AiCallVolumeTrendParams,
  AiCallLogVO,
} from '@/types/ai'
import type { EvolveRoiPayload, EvolutionReviewTaskRow, EvolutionReviewStats, QualityScoreTrendPoint } from '@/types/evolutionEngine'
import type {
  EvolutionAnalysisResultVO,
  KnowledgeEvolutionAnalyzeRequest,
  KnowledgeEvolutionAutoOptimizeRequest,
  KnowledgeEvolutionAutoOptimizeResult,
  KnowledgeEvolutionReportVO,
} from '@/types/knowledgeEvolutionAnalysis'
import { getDigitalHumanStatus, generateDigitalHumanVideo } from '@/api/digital-human'

export interface KnowledgeBase {
  id: number; kbName: string; description: string; totalDocuments: number; status: number; createTime: string
  totalTokens?: number; embeddingModel?: string; kbType?: string; updateTime?: string
}
export interface KbDocument {
  id: number; kbId: number; title: string; content?: string; fileType: string; status: number
  chunkCount: number; tokenCount: number; qualityHeuristicScore?: number
  expiryStatus?: number; createTime: string; updateTime?: string
  syncRetryCount?: number; lastSyncRetryAt?: string; metadata?: string
  sourceType?: string; boostFactor?: number; retrievalCount?: number; citationCount?: number
  lastQualityEvalAt?: string
}
export interface KbQuery { page?: number; rows?: number; name?: string }

export interface KbSearchPayload {
  query: string
  topK?: number
  queryRewrite?: boolean
}

export interface KbImportFromPathPayload {
  sourcePath: string
  kbId?: number
  kbName?: string
  autoClassify?: boolean
}

export interface KbFeedbackPayload {
  docId: number
  query: string
  rating: -1 | 0 | 1
  comment?: string
  searchMode?: string
}

/** 与后端 EvolutionServiceImpl.viralToMap 一致 */
export interface EvolutionViralItem {
  id: number
  videoId?: number
  /** 详情接口可能附带，列表通常无 */
  videoTitle?: string | null
  accountId?: number | null
  ownerId?: number
  viralScore?: number
  viewCount?: number
  avgViewCount?: number
  successFactors?: string | null
  replicableMethods?: string | null
  reportContent?: string | null
  qualityScore?: number
  modelUsed?: string | null
  tokensUsed?: number
  status: number
  createTime?: string
}

/** /ai/evolution/stats 返回键 */
export interface EvolutionStatsVO {
  viralAnalysisTotal?: number
  viralAnalysisDone?: number
  liveReviewTotal?: number
  liveReviewDone?: number
  indexQueuePending?: number
}

export interface PromptTemplate {
  id: number
  /** @deprecated 后端真实字段为 templateCode；保留给旧页面兼容 */
  templateType?: string
  templateCode?: string
  variantName?: string
  templateName: string
  templateContent: string
  isActive?: number | boolean
  isDefault?: number | boolean
  status?: number
  variables?: string
  category?: string
  systemPrompt?: string
  userPromptTpl?: string
  modelHint?: string
  temperature?: number
  maxTokens?: number
  usageCount?: number
  lastUsedAt?: string
  tags?: string
  createTime: string
  updateTime?: string
}
export interface PromptTemplateQuery {
  page?: number
  rows?: number
  templateType?: string
  templateCode?: string
  variantName?: string
  isActive?: boolean | number
  ownerId?: number
}

export interface PromptTemplateSavePayload extends Partial<Omit<PromptTemplate, 'isActive' | 'isDefault'>> {
  isActive?: boolean | number
  isDefault?: boolean | number
}

function toPromptTemplateSearch(params: PromptTemplateQuery): Record<string, unknown> {
  return {
    ...params,
    templateCode: params.templateCode ?? params.templateType,
    templateType: undefined,
    isActive: typeof params.isActive === 'number' ? params.isActive === 1 : params.isActive,
  }
}

function toPromptTemplateSave(params: PromptTemplateSavePayload): Record<string, unknown> {
  const templateCode = params.templateCode ?? params.templateType
  return {
    ...params,
    templateCode,
    templateType: undefined,
    isActive: typeof params.isActive === 'number' ? params.isActive === 1 : params.isActive,
    isDefault: typeof params.isDefault === 'number' ? params.isDefault === 1 : params.isDefault,
  }
}

type UnknownRecord = Record<string, unknown>

function readStringValue(value: unknown): string | undefined {
  if (value === null || value === undefined) return undefined
  return String(value)
}

function readBooleanValue(value: unknown): boolean | undefined {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value === 1
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase()
    if (['true', '1', 'yes', 'ok', 'success'].includes(normalized)) return true
    if (['false', '0', 'no', 'fail', 'failed', 'error'].includes(normalized)) return false
  }
  return undefined
}

function readFlagNumber(value: unknown): number | undefined {
  const n = readNumberValue(value)
  if (n !== undefined) return n === 0 ? 0 : 1
  const b = readBooleanValue(value)
  if (b !== undefined) return b ? 1 : 0
  return undefined
}

function normalizeObject<T>(raw: unknown): T {
  return normalizeRecord(raw) as T
}

function normalizeDashboardStats(raw: unknown): AiDashboardStatsVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    todayCalls: Number(record.todayCalls ?? 0),
    monthTokens: Number(record.monthTokens ?? 0),
    successRate: Number(record.successRate ?? 0),
    avgQualityScore: Number(record.avgQualityScore ?? 0),
    qualityDocCount: record.qualityDocCount == null ? undefined : Number(record.qualityDocCount),
    evaluatedQualityDocCount: record.evaluatedQualityDocCount == null ? undefined : Number(record.evaluatedQualityDocCount),
    unevaluatedQualityDocCount: record.unevaluatedQualityDocCount == null ? undefined : Number(record.unevaluatedQualityDocCount),
    lowQualityDocCount: record.lowQualityDocCount == null ? undefined : Number(record.lowQualityDocCount),
    qualityEvaluationCoverage: record.qualityEvaluationCoverage == null ? undefined : Number(record.qualityEvaluationCoverage),
  }
}

function normalizeKnowledgeBase(raw: unknown): KnowledgeBase {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    id: readNumberValue(record.id ?? record.kbId ?? record.kb_id) ?? 0,
    kbName: readStringValue(record.kbName ?? record.name ?? record.title ?? record.kb_name) ?? '',
    description: readStringValue(record.description ?? record.desc ?? record.summary) ?? '',
    totalDocuments: readNumberValue(record.totalDocuments ?? record.documentCount ?? record.docCount ?? record.totalDocs ?? record.total_docs) ?? 0,
    status: readNumberValue(record.status ?? record.enabled) ?? 0,
    createTime: readStringValue(record.createTime ?? record.createdAt ?? record.create_time) ?? '',
    totalTokens: readNumberValue(record.totalTokens ?? record.tokenCount ?? record.tokens),
    embeddingModel: readStringValue(record.embeddingModel ?? record.embedding_model ?? record.modelName ?? record.model),
    kbType: readStringValue(record.kbType ?? record.type ?? record.kb_type),
    updateTime: readStringValue(record.updateTime ?? record.updatedAt ?? record.update_time),
  }
}

function normalizeKnowledgeBaseList(raw: unknown): KnowledgeBase[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeKnowledgeBase)
    .filter(kb => kb.id !== 0 || kb.kbName !== '' || kb.description !== '')
}

function normalizeKbDocument(raw: unknown): KbDocument {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    id: readNumberValue(record.id ?? record.docId ?? record.documentId ?? record.doc_id) ?? 0,
    kbId: readNumberValue(record.kbId ?? record.kb_id ?? record.knowledgeBaseId) ?? 0,
    title: readStringValue(record.title ?? record.fileName ?? record.name ?? record.documentTitle) ?? '',
    content: readStringValue(record.content ?? record.text ?? record.body),
    fileType: readStringValue(record.fileType ?? record.type ?? record.ext ?? record.contentType) ?? 'text',
    status: readNumberValue(record.status ?? record.indexStatus ?? record.syncStatus) ?? 0,
    chunkCount: readNumberValue(record.chunkCount ?? record.chunks ?? record.chunk_count) ?? 0,
    tokenCount: readNumberValue(record.tokenCount ?? record.tokens ?? record.token_count) ?? 0,
    qualityHeuristicScore: readNumberValue(record.qualityHeuristicScore ?? record.qualityScore ?? record.quality_score),
    expiryStatus: readNumberValue(record.expiryStatus ?? record.expiry_status),
    createTime: readStringValue(record.createTime ?? record.createdAt ?? record.create_time) ?? '',
    updateTime: readStringValue(record.updateTime ?? record.updatedAt ?? record.update_time),
    syncRetryCount: readNumberValue(record.syncRetryCount ?? record.retryCount ?? record.sync_retry_count),
    lastSyncRetryAt: readStringValue(record.lastSyncRetryAt ?? record.last_sync_retry_at),
    metadata: readStringValue(record.metadata),
    sourceType: readStringValue(record.sourceType ?? record.source ?? record.source_type),
    boostFactor: readNumberValue(record.boostFactor ?? record.boost_factor),
    retrievalCount: readNumberValue(record.retrievalCount ?? record.retrieval_count),
    citationCount: readNumberValue(record.citationCount ?? record.citation_count),
    lastQualityEvalAt: readStringValue(record.lastQualityEvalAt ?? record.last_quality_eval_at),
  }
}

function normalizeKbDocumentPage(raw: unknown, params?: { page?: number; rows?: number }): PageResult<KbDocument> {
  const normalized = normalizeResponsePage<unknown, unknown>(raw, item => item, Number(params?.page ?? 0), Number(params?.rows ?? 20))
  return {
    ...normalized,
    list: normalized.list
      .map(normalizeKbDocument)
      .filter(doc => doc.id !== 0 || doc.title !== '' || doc.content != null),
  }
}

function normalizeKbSearchHit(raw: unknown): KbSearchHit {
  const record = normalizeObject<UnknownRecord>(raw)
  const labels = normalizeResponseArray<unknown>(record.labels ?? record.tags ?? record.categories)
    .map(label => String(label))
    .filter(label => label.trim() !== '')
  return {
    docId: readNumberValue(record.docId ?? record.documentId ?? record.id ?? record.doc_id),
    title: readStringValue(record.title ?? record.docTitle ?? record.documentTitle ?? record.name),
    content: readStringValue(record.content ?? record.text ?? record.chunkText ?? record.body) ?? '',
    score: readNumberValue(record.score ?? record.similarity ?? record.rankScore ?? record.relevance) ?? 0,
    source: readStringValue(record.source ?? record.sourceType ?? record.matchType),
    chunkId: readNumberValue(record.chunkId ?? record.chunk_id),
    labels,
    explain: readStringValue(record.explain ?? record.explanation ?? record.reason),
  }
}

function normalizeKbSearchHits(raw: unknown): KbSearchHit[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeKbSearchHit)
    .filter(hit => (hit.content ?? '') !== '' || (hit.title ?? '') !== '' || hit.docId != null)
}

function normalizeIndexQueueRow(raw: unknown): KbIndexQueueRow {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    id: readNumberValue(record.id ?? record.queueId ?? record.queue_id) ?? 0,
    sourceType: readStringValue(record.sourceType ?? record.source_type ?? record.type) ?? '',
    sourceId: readNumberValue(record.sourceId ?? record.source_id ?? record.docId) ?? 0,
    targetKbId: readNumberValue(record.targetKbId ?? record.target_kb_id ?? record.kbId) ?? 0,
    priority: readNumberValue(record.priority) ?? 0,
    status: readStringValue(record.status ?? record.state) ?? 'pending',
    retryCount: readNumberValue(record.retryCount ?? record.retry_count) ?? 0,
    errorMsg: readStringValue(record.errorMsg ?? record.errorMessage ?? record.error_msg ?? record.message) ?? null,
    createTime: readStringValue(record.createTime ?? record.createdAt ?? record.create_time) ?? null,
    contentPreview: readStringValue(record.contentPreview ?? record.preview ?? record.content_preview) ?? null,
  }
}

function normalizeIndexQueuePage(raw: unknown, params?: { page?: number; rows?: number }): PageResult<KbIndexQueueRow> {
  const normalized = normalizeResponsePage<unknown, unknown>(raw, item => item, Number(params?.page ?? 0), Number(params?.rows ?? 50))
  return {
    ...normalized,
    list: normalized.list
      .map(normalizeIndexQueueRow)
      .filter(row => row.id !== 0 || row.contentPreview != null || row.errorMsg != null),
  }
}

function normalizeDashboardList<T>(raw: unknown): T[] {
  return normalizeResponseArray<T>(raw)
}

function normalizeAdminModel(raw: unknown): AiModelAdminVO {
  const record = normalizeObject<UnknownRecord>(raw)
  const provider = readStringValue(record.modelProvider ?? record.provider ?? record.vendor) ?? ''
  const version = readStringValue(record.modelVersion ?? record.endpoint ?? record.modelId ?? record.modelCode ?? record.version) ?? ''
  const status = readNumberValue(record.status ?? record.enabled)
  const isDefault = readNumberValue(record.isDefault ?? record.defaultModel ?? record.default)
  return {
    ...record,
    id: readNumberValue(record.id ?? record.modelId) ?? 0,
    modelName: readStringValue(record.modelName ?? record.name ?? record.displayName) ?? '',
    modelProvider: provider,
    modelVersion: version,
    apiBaseUrl: readStringValue(record.apiBaseUrl ?? record.baseUrl ?? record.customBaseUrl) ?? null,
    apiKeyMasked: readStringValue(record.apiKeyMasked ?? record.apiKeyMask ?? record.maskedApiKey ?? record.maskedKey) ?? '',
    resolvedBaseUrl: readStringValue(record.resolvedBaseUrl ?? record.effectiveBaseUrl ?? record.baseUrl),
    maxTokens: readNumberValue(record.maxTokens ?? record.maxToken ?? record.max_tokens),
    temperature: readNumberValue(record.temperature ?? record.temp),
    status,
    isDefault,
    costPer1kTokens: readNumberValue(record.costPer1kTokens ?? record.costPer1k ?? record.cost),
    quotaLimit: readNumberValue(record.quotaLimit ?? record.quotaMax ?? record.limit),
    quotaUsed: readNumberValue(record.quotaUsed ?? record.usedQuota ?? record.used),
    createTime: readStringValue(record.createTime ?? record.createdAt),
    updateTime: readStringValue(record.updateTime ?? record.updatedAt),
  }
}

function normalizeAdminModels(raw: unknown): AiModelAdminVO[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeAdminModel)
    .filter(model => model.id !== 0 || model.modelName !== '' || model.modelProvider !== '' || model.modelVersion !== '')
}

function normalizeAiModel(raw: unknown): AiModelVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    id: readNumberValue(record.id ?? record.modelId ?? record.model_id) ?? 0,
    modelName: readStringValue(record.modelName ?? record.name ?? record.displayName ?? record.modelDisplayName ?? record.modelVersion ?? record.modelCode ?? record.model) ?? '',
    modelProvider: readStringValue(record.modelProvider ?? record.provider ?? record.vendor),
    modelVersion: readStringValue(record.modelVersion ?? record.version ?? record.modelCode ?? record.modelId ?? record.model),
    maxTokens: readNumberValue(record.maxTokens ?? record.maxToken ?? record.max_tokens),
    temperature: readNumberValue(record.temperature ?? record.temp),
    status: readNumberValue(record.status ?? record.enabled),
    costPer1kTokens: readNumberValue(record.costPer1kTokens ?? record.costPer1k ?? record.cost),
    quotaLimit: readNumberValue(record.quotaLimit ?? record.quotaMax ?? record.limit),
    quotaUsed: readNumberValue(record.quotaUsed ?? record.usedQuota ?? record.used),
    createTime: readStringValue(record.createTime ?? record.createdAt ?? record.create_time),
    updateTime: readStringValue(record.updateTime ?? record.updatedAt ?? record.update_time),
  }
}

function normalizeAiModels(raw: unknown): AiModelVO[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeAiModel)
    .filter(model => model.id !== 0 || model.modelName !== '' || model.modelProvider != null || model.modelVersion != null)
}

function normalizeConnectionTestResult(raw: unknown): AiModelConnectionTestResult {
  const record = normalizeObject<UnknownRecord>(raw)
  const success = readBooleanValue(record.success ?? record.ok ?? record.connected ?? record.pass) ?? false
  return {
    success,
    errorMsg: readStringValue(record.errorMsg ?? record.errorMessage ?? record.message ?? record.reason) ?? null,
    tokensUsed: readNumberValue(record.tokensUsed ?? record.totalTokens ?? record.tokens),
  }
}

function normalizeTaskModelConfigRow(raw: unknown): AiTaskModelConfigRow {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    id: readNumberValue(record.id) ?? 0,
    taskCode: readStringValue(record.taskCode ?? record.code) ?? '',
    taskName: readStringValue(record.taskName ?? record.name) ?? '',
    taskGroup: readStringValue(record.taskGroup ?? record.group) ?? 'evolve',
    primaryModelId: readNumberValue(record.primaryModelId ?? record.primaryModel ?? record.modelId) ?? null,
    fallbackModelId: readNumberValue(record.fallbackModelId ?? record.fallbackModel ?? record.backupModelId) ?? null,
    fallback2ModelId: readNumberValue(record.fallback2ModelId ?? record.fallback2Model ?? record.backup2ModelId) ?? null,
    primaryModelName: readStringValue(record.primaryModelName ?? record.modelName) ?? null,
    fallbackModelName: readStringValue(record.fallbackModelName ?? record.backupModelName) ?? null,
    fallback2ModelName: readStringValue(record.fallback2ModelName ?? record.backup2ModelName) ?? null,
    timeoutSeconds: readNumberValue(record.timeoutSeconds ?? record.timeoutSec ?? record.timeout) ?? null,
    maxRetries: readNumberValue(record.maxRetries ?? record.retries) ?? 1,
    sortOrder: readNumberValue(record.sortOrder ?? record.orderNo ?? record.sort) ?? 0,
    status: readNumberValue(record.status ?? record.enabled) ?? 1,
    createTime: readStringValue(record.createTime ?? record.createdAt),
    updateTime: readStringValue(record.updateTime ?? record.updatedAt),
  }
}

function normalizeTaskModelConfigRows(raw: unknown): AiTaskModelConfigRow[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeTaskModelConfigRow)
    .filter(row => row.id !== 0 || row.taskCode !== '' || row.taskName !== '')
}

function readRateValue(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') return undefined
  if (typeof value === 'string') {
    const text = value.trim()
    if (!text) return undefined
    const n = Number(text.endsWith('%') ? text.slice(0, -1) : text)
    if (!Number.isFinite(n)) return undefined
    return text.endsWith('%') || n > 1 ? n / 100 : n
  }
  const n = Number(value)
  if (!Number.isFinite(n)) return undefined
  return n > 1 ? n / 100 : n
}

function normalizeModelBenchmarkComparisonRow(raw: unknown): AiModelBenchmarkComparisonRow {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    modelId: readNumberValue(record.modelId ?? record.id ?? record.model_id ?? record.model) ?? 0,
    modelName: readStringValue(record.modelName ?? record.name ?? record.modelDisplayName ?? record.displayName),
    taskCode: readStringValue(record.taskCode ?? record.code ?? record.task ?? record.task_code) ?? '',
    avgLatencyMs: readNumberValue(record.avgLatencyMs ?? record.avgLatency ?? record.latencyMs ?? record.latency ?? record.avg_latency_ms) ?? 0,
    successRate: readRateValue(record.successRate ?? record.successRatio ?? record.rate ?? record.success_rate) ?? 0,
    avgTokens: readNumberValue(record.avgTokens ?? record.averageTokens ?? record.tokensUsed ?? record.tokens ?? record.avg_tokens) ?? 0,
    totalCalls: readNumberValue(record.totalCalls ?? record.calls ?? record.count ?? record.total ?? record.total_calls) ?? 0,
  }
}

function normalizeModelBenchmarkComparisonRows(raw: unknown): AiModelBenchmarkComparisonRow[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeModelBenchmarkComparisonRow)
    .filter(row => row.modelId !== 0 || row.taskCode !== '' || row.modelName)
}

function normalizeModelBenchmarkBestModel(raw: unknown): AiModelBenchmarkBestModelVO {
  const first = normalizeResponseArray<unknown>(raw)[0]
  const record = normalizeObject<UnknownRecord>(first ?? raw)
  return {
    ...record,
    modelId: readNumberValue(record.modelId ?? record.id ?? record.model_id ?? record.model) ?? 0,
    modelName: readStringValue(record.modelName ?? record.name ?? record.modelDisplayName ?? record.displayName),
    taskCode: readStringValue(record.taskCode ?? record.code ?? record.task ?? record.task_code) ?? '',
    priority: readStringValue(record.priority ?? record.strategy ?? record.metric ?? record.sortBy),
  }
}

function normalizeNumberArray(raw: unknown): number[] {
  if (typeof raw === 'number') {
    return Number.isFinite(raw) ? [raw] : []
  }
  if (typeof raw === 'string') {
    const text = raw.trim()
    if (!text) return []
    try {
      return normalizeNumberArray(JSON.parse(text))
    } catch {
      const numeric = readNumberValue(text)
      if (numeric != null) return [numeric]
      return text.split(',')
        .map(part => readNumberValue(part))
        .filter((value): value is number => value != null)
    }
  }
  return normalizeResponseArray<unknown>(raw)
    .map(readNumberValue)
    .filter((value): value is number => value != null)
}

function normalizeEvolveRoiPayload(raw: unknown): EvolveRoiPayload {
  const record = normalizeObject<UnknownRecord>(raw)
  const successRateRaw = record.successRate ?? record.success_rate ?? record.rate
  const successRate = typeof successRateRaw === 'string' && successRateRaw.trim().endsWith('%')
    ? readNumberValue(successRateRaw.trim().slice(0, -1))
    : readNumberValue(successRateRaw)
  return {
    ...record,
    newKnowledge: readNumberValue(record.newKnowledge ?? record.new_knowledge ?? record.generatedCount ?? record.indexedCount),
    avgScore: readNumberValue(record.avgScore ?? record.averageScore ?? record.avg_score ?? record.score),
    totalRuns: readNumberValue(record.totalRuns ?? record.completedTasks ?? record.completed ?? record.runs),
    coveredDocs: readNumberValue(record.coveredDocs ?? record.coveredKbs ?? record.coveredKbCount ?? record.covered_docs),
    totalTasks: readNumberValue(record.totalTasks ?? record.taskTotal ?? record.total),
    successRate,
  }
}

function normalizeScoreTrendPoint(raw: unknown): QualityScoreTrendPoint {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    date: readStringValue(record.date ?? record.day ?? record.period ?? record.time) ?? '',
    score: readNumberValue(record.score ?? record.avgScore ?? record.averageScore ?? record.qualityScore ?? record.value) ?? 0,
    count: readNumberValue(record.count ?? record.taskCount ?? record.total),
  }
}

function normalizeScoreTrendRows(raw: unknown): QualityScoreTrendPoint[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeScoreTrendPoint)
    .filter(point => point.date !== '' || point.score !== 0 || point.count != null)
}

function normalizeTtsVoice(raw: unknown): AiMediaTtsVoiceInfo {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    id: readStringValue(record.id ?? record.voiceId ?? record.voice ?? record.code) ?? '',
    name: readStringValue(record.name ?? record.voiceName ?? record.label ?? record.displayName ?? record.voiceId ?? record.voice) ?? '',
    language: readStringValue(record.language ?? record.lang ?? record.locale),
    gender: readStringValue(record.gender ?? record.sex),
    description: readStringValue(record.description ?? record.desc ?? record.remark),
  }
}

function normalizeTtsVoices(raw: unknown): AiMediaTtsVoiceInfo[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeTtsVoice)
    .filter(voice => voice.id !== '' || voice.name !== '')
}

function normalizeMediaImageHistoryRow(raw: unknown): AiMediaImageHistory {
  const record = normalizeObject<UnknownRecord>(raw)
  const createTime = readNumberValue(record.createTime ?? record.createdAt ?? record.create_time ?? record.timestamp)
  const parameters = isRecord(record.parameters)
    ? record.parameters
    : isRecord(record.params)
      ? record.params
      : undefined
  return {
    id: readNumberValue(record.id ?? record.historyId ?? record.history_id) ?? 0,
    imageUrl: readStringValue(record.imageUrl ?? record.url ?? record.image_url ?? record.fileUrl ?? record.outputUrl) ?? '',
    prompt: readStringValue(record.prompt ?? record.text ?? record.inputPrompt),
    type: readStringValue(record.type ?? record.style ?? record.mediaType),
    parameters,
    createTime,
  }
}

function normalizeMediaImageHistory(raw: unknown): AiMediaImageHistory[] {
  return normalizeResponseArray<unknown>(raw)
    .map(normalizeMediaImageHistoryRow)
    .filter(row => row.id !== 0 || row.imageUrl !== '' || row.prompt != null)
}

function normalizeKnowledgeEvolutionScriptOpportunity(raw: unknown): NonNullable<EvolutionAnalysisResultVO['readyForInclusion']>[number] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    scriptVersionId: readNumberValue(record.scriptVersionId ?? record.script_version_id ?? record.id ?? record.scriptId ?? record.script_id),
    title: readStringValue(record.title ?? record.scriptTitle ?? record.name ?? record.script_name),
    score: readNumberValue(record.score ?? record.qualityScore ?? record.quality_score),
    usageCount: readNumberValue(record.usageCount ?? record.usage_count),
    reason: readStringValue(record.reason ?? record.desc ?? record.description),
  }
}

function normalizeKnowledgeEvolutionOptimizationOpportunity(raw: unknown): NonNullable<EvolutionAnalysisResultVO['needsOptimization']>[number] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    scriptVersionId: readNumberValue(record.scriptVersionId ?? record.script_version_id ?? record.id ?? record.scriptId ?? record.script_id),
    title: readStringValue(record.title ?? record.scriptTitle ?? record.name ?? record.script_name),
    score: readNumberValue(record.score ?? record.qualityScore ?? record.quality_score),
    consecutiveLowScore: readNumberValue(record.consecutiveLowScore ?? record.consecutive_low_score),
    suggestion: readStringValue(record.suggestion ?? record.reason ?? record.description),
    referenceScriptId: readNumberValue(record.referenceScriptId ?? record.reference_script_id),
  }
}

function normalizeKnowledgeEvolutionDuplicateGroup(raw: unknown): NonNullable<EvolutionAnalysisResultVO['duplicatesDetected']>[number] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    masterScriptId: readNumberValue(record.masterScriptId ?? record.master_script_id ?? record.masterId ?? record.master_id ?? record.id),
    masterTitle: readStringValue(record.masterTitle ?? record.master_title ?? record.title ?? record.name),
    masterScore: readNumberValue(record.masterScore ?? record.master_score),
    duplicateScriptIds: normalizeNumberArray(record.duplicateScriptIds ?? record.duplicate_script_ids ?? record.duplicates ?? record.duplicateIds),
    similarityScore: readNumberValue(record.similarityScore ?? record.similarity_score ?? record.similarity),
    recommendation: readStringValue(record.recommendation ?? record.suggestion ?? record.reason),
  }
}

function normalizeKnowledgeEvolutionArchivalOpportunity(raw: unknown): NonNullable<EvolutionAnalysisResultVO['readyForArchival']>[number] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    scriptVersionId: readNumberValue(record.scriptVersionId ?? record.script_version_id ?? record.id ?? record.scriptId ?? record.script_id),
    title: readStringValue(record.title ?? record.scriptTitle ?? record.name ?? record.script_name),
    currentScore: readNumberValue(record.currentScore ?? record.current_score ?? record.score),
    reason: readStringValue(record.reason ?? record.description ?? record.suggestion),
    monthsSinceDeprecation: readNumberValue(record.monthsSinceDeprecation ?? record.months_since_deprecation),
  }
}

function normalizeKnowledgeEvolutionImpact(raw: unknown): EvolutionAnalysisResultVO['expectedImpact'] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    newInclusionsCount: readNumberValue(record.newInclusionsCount ?? record.new_inclusions_count ?? record.newCount),
    deduplicationCount: readNumberValue(record.deduplicationCount ?? record.deduplication_count ?? record.dedupCount ?? record.dedup_count),
    improvementRate: readNumberValue(record.improvementRate ?? record.improvement_rate ?? record.rate),
  }
}

function normalizeKnowledgeEvolutionAnalyze(raw: unknown): EvolutionAnalysisResultVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    analysisId: readStringValue(record.analysisId ?? record.analysis_id),
    periodStart: readStringValue(record.periodStart ?? record.startDate ?? record.period_start),
    periodEnd: readStringValue(record.periodEnd ?? record.endDate ?? record.period_end),
    readyForInclusion: normalizeResponseArray<unknown>(record.readyForInclusion ?? record.inclusions ?? record.includeCandidates ?? record.inclusionCandidates ?? record.ready_for_inclusion)
      .map(normalizeKnowledgeEvolutionScriptOpportunity),
    needsOptimization: normalizeResponseArray<unknown>(record.needsOptimization ?? record.optimizationCandidates ?? record.optimizeCandidates ?? record.needOptimization ?? record.needs_optimization)
      .map(normalizeKnowledgeEvolutionOptimizationOpportunity),
    duplicatesDetected: normalizeResponseArray<unknown>(record.duplicatesDetected ?? record.duplicateGroups ?? record.duplicateCandidates ?? record.duplicates_detected)
      .map(normalizeKnowledgeEvolutionDuplicateGroup),
    readyForArchival: normalizeResponseArray<unknown>(record.readyForArchival ?? record.archiveCandidates ?? record.archivalCandidates ?? record.ready_for_archival)
      .map(normalizeKnowledgeEvolutionArchivalOpportunity),
    expectedImpact: isRecord(record.expectedImpact) ? normalizeKnowledgeEvolutionImpact(record.expectedImpact) : undefined,
    createdAt: readStringValue(record.createdAt ?? record.created_at),
    degraded: readBooleanValue(record.degraded ?? record.stubbed ?? record.downgraded),
  }
}

function normalizeKnowledgeEvolutionReportOverview(raw: unknown): NonNullable<KnowledgeEvolutionReportVO['overview']> {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    totalScriptsInLibrary: readNumberValue(record.totalScriptsInLibrary ?? record.totalScripts ?? record.total_scripts_in_library ?? record.librarySize),
    newAddedCount: readNumberValue(record.newAddedCount ?? record.newAdded ?? record.new_added_count),
    archivedCount: readNumberValue(record.archivedCount ?? record.archived ?? record.archived_count),
    deduplicatedCount: readNumberValue(record.deduplicatedCount ?? record.deduplicated ?? record.deduplicated_count),
    averageScore: readNumberValue(record.averageScore ?? record.avgScore ?? record.average_score ?? record.score),
  }
}

function normalizeKnowledgeEvolutionTopScript(raw: unknown): NonNullable<KnowledgeEvolutionReportVO['topScripts']>[number] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    rank: readNumberValue(record.rank ?? record.order ?? record.position),
    scriptId: readNumberValue(record.scriptId ?? record.script_id ?? record.id),
    title: readStringValue(record.title ?? record.scriptTitle ?? record.name ?? record.script_name),
    score: readNumberValue(record.score ?? record.qualityScore ?? record.quality_score),
    usageCount: readNumberValue(record.usageCount ?? record.usage_count),
    adoptionRate: readNumberValue(record.adoptionRate ?? record.adoption_rate),
  }
}

function normalizeKnowledgeEvolutionStyleAnalysis(raw: unknown): NonNullable<KnowledgeEvolutionReportVO['styleAnalysis']>[string] {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    count: readNumberValue(record.count ?? record.total ?? record.size),
    averageScore: readNumberValue(record.averageScore ?? record.avgScore ?? record.average_score ?? record.score),
    trend: readStringValue(record.trend ?? record.direction),
  }
}

function normalizeKnowledgeEvolutionReport(raw: unknown): KnowledgeEvolutionReportVO {
  const record = normalizeObject<UnknownRecord>(raw)
  const styleAnalysis = isRecord(record.styleAnalysis)
    ? Object.fromEntries(Object.entries(record.styleAnalysis).map(([key, value]) => [key, normalizeKnowledgeEvolutionStyleAnalysis(value)]))
    : undefined
  return {
    reportId: readStringValue(record.reportId ?? record.report_id),
    period: readStringValue(record.period ?? record.range),
    overview: isRecord(record.overview) ? normalizeKnowledgeEvolutionReportOverview(record.overview) : undefined,
    topScripts: normalizeResponseArray<unknown>(record.topScripts ?? record.top_scripts).map(normalizeKnowledgeEvolutionTopScript),
    styleAnalysis,
    recommendations: normalizeResponseArray<unknown>(record.recommendations ?? record.suggestions).map((item) => {
      const recommendation = normalizeObject<UnknownRecord>(item)
      return {
        type: readStringValue(recommendation.type ?? recommendation.kind ?? recommendation.code),
        description: readStringValue(recommendation.description ?? recommendation.desc ?? recommendation.message),
        priority: readStringValue(recommendation.priority ?? recommendation.level),
      }
    }),
    generatedAt: readStringValue(record.generatedAt ?? record.generated_at),
  }
}

function normalizeKnowledgeEvolutionAutoOptimizeResult(raw: unknown): KnowledgeEvolutionAutoOptimizeResult {
  const record = normalizeObject<UnknownRecord>(raw)
  const resultsSource = isRecord(record.results) ? record.results : {}
  const summarySource = isRecord(record.summary) ? record.summary : {}
  const results = Object.fromEntries(
    Object.entries(resultsSource).map(([key, value]) => {
      const row = isRecord(value) ? value : {}
      return [
        key,
        {
          count: readNumberValue(row.count ?? row.total ?? row.size ?? row.num),
          scriptIds: normalizeNumberArray(row.scriptIds ?? row.script_ids ?? row.ids),
        },
      ]
    }),
  )
  return {
    executionId: readStringValue(record.executionId ?? record.execution_id),
    status: readStringValue(record.status ?? record.state),
    results,
    summary: {
      ...summarySource,
      totalProcessed: readNumberValue(summarySource.totalProcessed ?? summarySource.total_processed ?? summarySource.total ?? summarySource.count),
      qualityImprovement: readNumberValue(summarySource.qualityImprovement ?? summarySource.quality_improvement ?? summarySource.improvement),
      estimatedUserBenefit: readStringValue(summarySource.estimatedUserBenefit ?? summarySource.estimated_user_benefit ?? summarySource.benefit),
    },
    executedAt: readStringValue(record.executedAt ?? record.executed_at),
    degraded: readBooleanValue(record.degraded ?? summarySource.degraded),
  }
}

function normalizeCallLog(raw: unknown): AiCallLogVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    id: readNumberValue(record.id ?? record.logId ?? record.log_id) ?? 0,
    userId: readNumberValue(record.userId ?? record.user_id ?? record.ownerId ?? record.owner_id),
    outputLength: readNumberValue(record.outputLength ?? record.output_length),
    promptTokens: readNumberValue(record.promptTokens ?? record.prompt_tokens),
    completionTokens: readNumberValue(record.completionTokens ?? record.completion_tokens),
    totalTokens: readNumberValue(record.totalTokens ?? record.total_tokens ?? record.tokensUsed ?? record.tokens_used),
    durationMs: readNumberValue(record.durationMs ?? record.duration_ms ?? record.latencyMs ?? record.latency_ms),
    status: readNumberValue(record.status),
    isFallback: readNumberValue(record.isFallback ?? record.is_fallback ?? record.fallback),
    linkedVideoId: readNumberValue(record.linkedVideoId ?? record.linked_video_id ?? record.videoId ?? record.video_id),
    linkedSessionId: readNumberValue(record.linkedSessionId ?? record.linked_session_id ?? record.sessionId ?? record.session_id),
    effectScore: readNumberValue(record.effectScore ?? record.effect_score),
    callType: record.callType == null && record.call_type == null && record.type == null ? undefined : String(record.callType ?? record.call_type ?? record.type),
    templateCode: record.templateCode == null && record.template_code == null ? undefined : String(record.templateCode ?? record.template_code),
    modelCode: record.modelCode == null && record.model_code == null && record.model == null ? undefined : String(record.modelCode ?? record.model_code ?? record.model),
    inputSummary: record.inputSummary == null && record.input_summary == null && record.prompt == null ? undefined : String(record.inputSummary ?? record.input_summary ?? record.prompt),
    errorMessage: record.errorMessage == null && record.error_message == null && record.message == null ? undefined : String(record.errorMessage ?? record.error_message ?? record.message),
    referencedChunkIds: record.referencedChunkIds == null && record.referenced_chunk_ids == null ? undefined : String(record.referencedChunkIds ?? record.referenced_chunk_ids),
    contentEffect: record.contentEffect == null && record.content_effect == null ? undefined : String(record.contentEffect ?? record.content_effect),
    stageTimings: record.stageTimings == null && record.stage_timings == null ? undefined : String(record.stageTimings ?? record.stage_timings),
    createTime: record.createTime == null && record.create_time == null && record.createdAt == null ? undefined : String(record.createTime ?? record.create_time ?? record.createdAt),
  }
}

function normalizeCallLogPage(raw: unknown, params: Record<string, unknown>): PageResult<AiCallLogVO> {
  const page = Number(params.page ?? 0)
  const rows = Number(params.rows ?? 20)
  const normalized = normalizeResponsePage<unknown, unknown>(raw, item => item, page, rows)
  return {
    ...normalized,
    list: normalized.list.map(normalizeCallLog),
  }
}

function normalizeInfraHealthItem(raw: unknown): AiInfraHealthItem {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    component: String(record.component ?? record.name ?? ''),
    ok: record.ok === true || record.status === 'ok' || record.status === 'UP' || record.status === 'online',
    message: record.message == null ? null : String(record.message),
  }
}

function normalizeCacheStats(raw: unknown): AiCacheStatsVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    hit: record.hit == null ? undefined : Number(record.hit),
    miss: record.miss == null ? undefined : Number(record.miss),
    total: record.total == null ? undefined : Number(record.total),
    hitRate: record.hitRate == null ? undefined : Number(record.hitRate),
    keyCount: record.keyCount == null ? undefined : Number(record.keyCount),
  } as AiCacheStatsVO
}

function normalizeCacheDiagnostics(raw: unknown): AiCacheDiagnosticsVO {
  const record = normalizeObject<UnknownRecord>(raw)
  const redisStats = isRecord(record.redisStats) ? record.redisStats : undefined
  const businessStats = isRecord(record.businessStats) ? normalizeCacheStats(record.businessStats) : undefined
  const scan = isRecord(record.scan) ? record.scan : undefined
  return {
    ...record,
    ok: typeof record.ok === 'boolean' ? record.ok : undefined,
    host: record.host == null ? undefined : String(record.host),
    lastUpdate: record.lastUpdate == null ? undefined : String(record.lastUpdate),
    dbSize: record.dbSize == null ? undefined : Number(record.dbSize),
    kbCacheTtlSeconds: record.kbCacheTtlSeconds == null ? undefined : Number(record.kbCacheTtlSeconds),
    embeddingCacheTtlDays: record.embeddingCacheTtlDays == null ? undefined : Number(record.embeddingCacheTtlDays),
    message: record.message == null ? undefined : String(record.message),
    redisStats: redisStats ? {
      ...redisStats,
      keyspaceHits: redisStats.keyspaceHits == null ? undefined : Number(redisStats.keyspaceHits),
      keyspaceMisses: redisStats.keyspaceMisses == null ? undefined : Number(redisStats.keyspaceMisses),
      globalHitRate: redisStats.globalHitRate == null ? undefined : Number(redisStats.globalHitRate),
      expiredKeys: redisStats.expiredKeys == null ? undefined : Number(redisStats.expiredKeys),
      evictedKeys: redisStats.evictedKeys == null ? undefined : Number(redisStats.evictedKeys),
    } : undefined,
    businessStats: businessStats ? {
      ...businessStats,
      scope: record.businessStats && isRecord(record.businessStats) && record.businessStats.scope != null ? String(record.businessStats.scope) : undefined,
      description: record.businessStats && isRecord(record.businessStats) && record.businessStats.description != null ? String(record.businessStats.description) : undefined,
    } : undefined,
    scan: scan ? {
      ...scan,
      scanned: scan.scanned == null ? undefined : Number(scan.scanned),
      truncated: typeof scan.truncated === 'boolean' ? scan.truncated : undefined,
      error: scan.error == null ? undefined : String(scan.error),
      prefixCounts: isRecord(scan.prefixCounts) ? Object.fromEntries(Object.entries(scan.prefixCounts).map(([k, v]) => [k, Number(v)])) : undefined,
      ttlBuckets: isRecord(scan.ttlBuckets) ? Object.fromEntries(Object.entries(scan.ttlBuckets).map(([k, v]) => [k, Number(v)])) : undefined,
      examples: normalizeResponseArray<string>(scan.examples).map(value => String(value)),
    } : undefined,
    suggestions: normalizeResponseArray<string>(record.suggestions).map(value => String(value)),
  }
}

function normalizeSearchStats(raw: unknown): AiSearchStatsVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    p50Ms: record.p50Ms == null && record.p50_ms == null && record.p50 == null ? undefined : Number(record.p50Ms ?? record.p50_ms ?? record.p50),
    p95Ms: record.p95Ms == null && record.p95_ms == null && record.p95 == null ? undefined : Number(record.p95Ms ?? record.p95_ms ?? record.p95),
    p99Ms: record.p99Ms == null && record.p99_ms == null && record.p99 == null && record.p99LatencyMs == null && record.p99_latency_ms == null
      ? undefined
      : Number(record.p99Ms ?? record.p99_ms ?? record.p99 ?? record.p99LatencyMs ?? record.p99_latency_ms),
    qps: record.qps == null ? undefined : Number(record.qps),
    totalQueries: record.totalQueries == null && record.total_queries == null ? undefined : Number(record.totalQueries ?? record.total_queries),
    totalRequests: record.totalRequests == null && record.total_requests == null ? undefined : Number(record.totalRequests ?? record.total_requests),
    avgLatencyMs: record.avgLatencyMs == null && record.avg_latency_ms == null && record.avgMs == null ? undefined : Number(record.avgLatencyMs ?? record.avg_latency_ms ?? record.avgMs),
    message: record.message == null ? undefined : String(record.message),
  }
}

function normalizeQuotaItem(raw: unknown): AiQuotaItemRow {
  const record = normalizeObject<UnknownRecord>(raw)
  const limit = readNumberValue(record.limit ?? record.maxCount ?? record.max ?? record.totalQuota)
  return {
    feature: String(record.feature ?? record.callType ?? record.type ?? ''),
    limit: limit ?? 0,
    hasLimit: limit !== undefined,
    used: readNumberValue(record.used ?? record.usedCount ?? record.usedQuota ?? record.count) ?? 0,
    unit: record.unit == null ? undefined : String(record.unit),
    period: record.period == null ? undefined : String(record.period),
  }
}

function normalizeQuotaOverview(raw: unknown): AiAdminQuotaOverviewVO {
  const record = normalizeObject<UnknownRecord>(raw)
  let items = normalizeResponseArray<unknown>(record.items).map(normalizeQuotaItem)
  if (items.length === 0) {
    items = ['overall', 'script_gen', 'kb_search', 'image_gen', 'tts']
      .map(key => record[key])
      .filter(Boolean)
      .map(normalizeQuotaItem)
  }
  const dailyMax = readNumberValue(record.dailyMax ?? record.maxCount ?? record.totalQuota)
  const overview = {
    ...record,
    dailyMax,
    items,
  } as unknown as AiAdminQuotaOverviewVO
  for (const item of items) {
    if (item.feature) {
      overview[String(item.feature)] = item
    }
  }
  return overview
}

function normalizeQuotaHistoryRow(raw: unknown): AiQuotaHistoryVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    id: readNumberValue(record.id),
    feature: record.feature == null ? undefined : String(record.feature),
    used: readNumberValue(record.used ?? record.usedCount ?? record.usedQuota ?? record.count),
    limit: readNumberValue(record.limit ?? record.maxCount ?? record.max ?? record.totalQuota),
    period: record.period == null ? undefined : String(record.period),
    unit: record.unit == null ? undefined : String(record.unit),
    createTime: record.createTime == null ? undefined : String(record.createTime),
    date: record.date == null ? undefined : String(record.date),
    count: readNumberValue(record.count),
  }
}

function normalizeQuotaHistoryPage(raw: unknown, params?: { page?: number; rows?: number }): PageResult<AiQuotaHistoryVO> {
  const normalized = normalizeResponsePage<unknown, unknown>(raw, item => item as unknown, Number(params?.page ?? 0), Number(params?.rows ?? 20))
  return {
    ...normalized,
    list: normalized.list.map(normalizeQuotaHistoryRow),
  }
}

function normalizeInfraDetail(raw: unknown): AiInfraDetailFullVO {
  return normalizeObject<AiInfraDetailFullVO>(raw)
}

function normalizeMonitoringConfig(raw: unknown): AiMonitoringConfigVO {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    grafanaUrl: record.grafanaUrl == null ? undefined : String(record.grafanaUrl),
    creditEnforce: record.creditEnforce === true,
    allowHeaderIdentity: record.allowHeaderIdentity !== false,
  }
}

function normalizeQualityRescan(raw: unknown): { updated: number; completed: boolean } {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    updated: Number(record.updated ?? 0),
    completed: record.completed === true,
  }
}

function normalizePromptTemplate(raw: unknown): PromptTemplate {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    ...record,
    id: readNumberValue(record.id) ?? 0,
    templateType: record.templateType == null ? undefined : String(record.templateType),
    templateCode: String(record.templateCode ?? record.templateType ?? record.code ?? ''),
    variantName: record.variantName == null ? undefined : String(record.variantName),
    templateName: String(record.templateName ?? record.name ?? ''),
    templateContent: String(record.templateContent ?? record.userPromptTpl ?? record.content ?? ''),
    isActive: readFlagNumber(record.isActive ?? record.active ?? record.enabled ?? record.is_active),
    isDefault: readFlagNumber(record.isDefault ?? record.default ?? record.is_default),
    status: readNumberValue(record.status ?? record.enabled),
    usageCount: readNumberValue(record.usageCount ?? record.usage_count ?? record.useCount),
    temperature: readNumberValue(record.temperature ?? record.temp),
    maxTokens: readNumberValue(record.maxTokens ?? record.max_tokens ?? record.maxToken),
    avgScore: readNumberValue(record.avgScore ?? record.avg_score),
    p50Score: readNumberValue(record.p50Score ?? record.p50_score),
    p90Score: readNumberValue(record.p90Score ?? record.p90_score),
    ownerId: readNumberValue(record.ownerId ?? record.owner_id),
    userId: readNumberValue(record.userId ?? record.user_id),
    lastUsedAt: record.lastUsedAt == null && record.last_used_at == null ? undefined : String(record.lastUsedAt ?? record.last_used_at),
    updateTime: record.updateTime == null && record.updatedAt == null ? undefined : String(record.updateTime ?? record.updatedAt),
    createTime: String(record.createTime ?? ''),
  } as PromptTemplate
}

function normalizePromptRenderResult(raw: unknown): { rendered: string; variables: string[]; missingVariables: string[] } {
  const record = normalizeObject<UnknownRecord>(raw)
  return {
    rendered: String(record.rendered ?? record.result ?? record.output ?? record.content ?? ''),
    variables: normalizeResponseArray<string>(record.variables ?? record.extractedVariables ?? record.variableNames)
      .map(value => String(value)),
    missingVariables: normalizeResponseArray<string>(record.missingVariables ?? record.missing ?? record.requiredMissing)
      .map(value => String(value)),
  }
}

function normalizeReviewStats(raw: unknown): EvolutionReviewStats {
  const source = isRecord(raw) ? raw : {}
  return {
    ...source,
    pendingCount: Number(source.pendingCount ?? source.pending ?? 0),
    pending: Number(source.pending ?? source.pendingCount ?? 0),
    approvedCount: Number(source.approvedCount ?? source.approved ?? 0),
    approved: Number(source.approved ?? source.approvedCount ?? 0),
    rejectedCount: Number(source.rejectedCount ?? source.rejected ?? 0),
    rejected: Number(source.rejected ?? source.rejectedCount ?? 0),
    revisedCount: Number(source.revisedCount ?? source.revised ?? 0),
    revised: Number(source.revised ?? source.revisedCount ?? 0),
    approvalRate7d: Number(source.approvalRate7d ?? 0),
  }
}

export const getModelsByTaskCode = (taskCode: string) =>
  request.post<unknown>('/ai/model/list-by-task', { taskCode }).then(normalizeAiModels)

export const listAiModels = (page: number) =>
  request.post<unknown>('/ai/model/list', { page, rows: 100 }).then(normalizeAiModels)

/** 与后端 KnowledgeBaseService.SearchResult 对齐；/knowledge-base/:id/search 解包后为数组 */
export interface KbSearchHit {
  docId?: number
  title?: string
  content?: string
  score: number
  source?: string
  chunkId?: number
  labels?: string[]
  explain?: string
}

export interface AiRuntimeStatusPayload {
  checkedAt?: string
  evolution?: Record<string, unknown>
  officialCollect?: Record<string, unknown>
  indexQueue?: Record<string, unknown>
}

export const aiApi = {
  // Knowledge Base
  kbList: (params: KbQuery) => request.post<unknown>('/ai/knowledge-base/list', params).then(normalizeKnowledgeBaseList),
  kbCreate: (params: { name?: string; description?: string }) =>
    request.post<unknown>('/ai/knowledge-base/create', params).then(normalizeKnowledgeBase),
  kbDelete: (id: number) => request.post<void>('/ai/knowledge-base/delete', { id }),
  kbSearch: (kbId: number, p: KbSearchPayload) =>
    request.post<unknown>(`/ai/knowledge-base/${kbId}/search`, p).then(normalizeKbSearchHits),
  kbDedupPreview: (kbId: number) => request.post<Record<string, unknown>>(`/ai/knowledge-base/${kbId}/dedup-preview`, {}),
  kbImportFromPath: (params: KbImportFromPathPayload) => request.post<Record<string, unknown>>('/ai/knowledge-base/import-from-path', params),
  kbImportActiveJobs: () => request.post<{ jobIds: string[] }>('/ai/knowledge-base/import-active-jobs', {}),
  kbFeedback: (params: KbFeedbackPayload) => request.post<void>('/ai/knowledge-base/feedback', params),

  // KB Documents
  docList: (kbId: number, params: { page?: number; rows?: number; keyword?: string; sourceType?: string }) =>
    request.post<unknown>(`/ai/knowledge-base/${kbId}/documents`, params).then(data => normalizeKbDocumentPage(data, params)),
  docDelete: (docId: number) => request.post<void>('/ai/knowledge-base/document/delete', { id: docId }),

  // Evolution (Viral)
  evolveList: (params: { page?: number; rows?: number; status?: number }) =>
    request.post<PageResult<EvolutionViralItem>>('/ai/evolution/viral/list', params),
  evolveViralGet: (id: number) =>
    request.post<EvolutionViralItem>(`/ai/evolution/viral/get?id=${encodeURIComponent(String(id))}`, {}),
  evolveTrigger: (body: { videoId: number; accountId?: number | null }) =>
    request.post<number>('/ai/evolution/viral/trigger', body),
  evolveComplete: (body: Record<string, unknown>) => request.post<void>('/ai/evolution/viral/complete', body),
  evolveDelete: (id: number) =>
    request.post<void>(`/ai/evolution/viral/delete?id=${encodeURIComponent(String(id))}`, {}),
  evolveStats: () => request.post<EvolutionStatsVO>('/ai/evolution/stats', {}),

  // Evolution (Live Review)
  liveReviewList: (params: { page?: number; rows?: number }) => request.post<PageResult<AiLiveReviewVO>>('/ai/evolution/live-review/list', params),
  liveReviewTrigger: (body: Record<string, unknown>) => request.post<number>('/ai/evolution/live-review/trigger', body),
  liveReviewDelete: (id: number) => request.post<void>('/ai/evolution/live-review/delete', { id }),

  // Prompt Template
  promptTemplateList: (params: PromptTemplateQuery) =>
    request.post<unknown>('/ai/prompt-template/list', toPromptTemplateSearch(params))
      .then(data => normalizeResponsePage<unknown, PromptTemplate>(
        data,
        normalizePromptTemplate,
        Number(params.page ?? 0),
        Number(params.rows ?? 20),
      )),
  promptTemplateSave: (params: PromptTemplateSavePayload) =>
    request.post<unknown>('/ai/prompt-template/save', toPromptTemplateSave(params)).then(normalizePromptTemplate),
  promptTemplateDelete: (id: number) => request.post<void>('/ai/prompt-template/delete', { id }),
  promptTemplateGetActive: (templateCode: string, variantName = 'default') =>
    request.post<unknown>('/ai/prompt-template/get-active', { templateCode, variantName }).then(normalizePromptTemplate),
  promptTemplateTestRender: (params: { templateContent: string; variables?: Record<string, string> }) =>
    request.post<unknown>('/ai/prompt-template/test-render', params).then(normalizePromptRenderResult),
  promptTemplateExtractVariables: (templateContent: string) =>
    request.post<unknown>('/ai/prompt-template/extract-variables', { templateContent })
      .then(data => normalizeResponseArray<string>(data).map(value => String(value))),
  promptTemplateRecordUsage: (id: number) =>
    request.post<void>('/ai/prompt-template/record-usage', { id }),

  // AI Chat / Generate
  chat: (params: Record<string, unknown>) => request.post<Record<string, unknown>>('/ai/chat', params),
  generate: (params: Record<string, unknown>) => request.post<Record<string, unknown>>('/ai/generate', params),

  // AI Admin
  runtimeStatus: () => request.get<AiRuntimeStatusPayload>('/ai/admin/runtime-status'),
  adminCallLogList: (params: Record<string, unknown>) =>
    request.post<unknown>('/ai/admin/call-log/search', params).then(data => normalizeCallLogPage(data, params)),
  adminInfraModelList: (params: Record<string, unknown>) => request.post<PageResult<Record<string, unknown>>>('/ai/admin/infra/model/list', params),

  /** 模型配置（管理端）：列表为掩码 VO */
  adminModelsList: () => request.post<unknown>('/ai/admin/models/list', {}).then(normalizeAdminModels),
  adminModelsSave: (params: AiModelSavePayload) => request.post<void>('/ai/admin/models/save', params),
  adminModelsDelete: (id: number) => request.post<void>('/ai/admin/models/delete', { id }),
  adminModelsSetDefault: (id: number) => request.post<void>('/ai/admin/models/set-default', { id }),
  adminModelsTestConnection: (id: number) =>
    request.post<unknown>('/ai/admin/models/test-connection', { id }).then(normalizeConnectionTestResult),

  // Industry Brain
  brainKnowledgeGraphQuery: (params: Record<string, unknown>) => request.post<Record<string, unknown>[]>('/ai/brain/knowledge-graph/query', params),
  brainCausalInfer: (params: Record<string, unknown>) => request.post<Record<string, unknown>>('/ai/brain/causal/infer', params),
  brainTrendsCurrent: () => request.post<Record<string, unknown>[]>('/ai/brain/trends/current', {}),

  // Creative Studio（与 MediaController /ai/media 对齐）
  text2img: (params: {
    prompt: string
    style?: string
    negativePrompt?: string
    width?: number
    height?: number
    steps?: number
    cfgScale?: number
    seed?: number
  }) => request.post<AiMediaImageResult>('/ai/media/image/text2img', params),
  tts: (params: {
    text: string
    voice?: string
    voiceId?: string
    language?: string
    speed?: number
    pitch?: number
    format?: string
  }) => request.post<AiMediaAudioResult>('/ai/media/tts/generate', params),
  /** 首尾帧图片 URL → 过渡视频片段 */
  videoGenerateFromFrames: (params: { startFrameUrl: string; endFrameUrl: string; durationSec?: number }) =>
    request.post<AiMediaVideoResult>('/ai/media/video/generate-from-frames', params),
  mediaTtsVoices: () => request.post<unknown>('/ai/media/tts/voices', {}).then(normalizeTtsVoices),
  mediaImageHistory: (params?: { page?: number; size?: number }) =>
    request.post<unknown>('/ai/media/image/history', params ?? {}).then(normalizeMediaImageHistory),

  // Call Analytics
  callVolumeTrend: (params?: AiCallVolumeTrendParams) =>
    request.post<unknown>('/ai/admin/dashboard/call-volume-trend', params ?? {})
      .then(data => normalizeDashboardList<AiCallVolumeTrendItem>(data)),
  callTypeDistribution: (params?: Record<string, unknown>) =>
    request.post<unknown>('/ai/admin/dashboard/call-type-distribution', params ?? {})
      .then(data => normalizeDashboardList<AiCallTypeDistributionItem>(data)),
  /** 额度按日汇总（date / usedCount / maxCount），与 ai_call_quota 聚合一致 */
  quotaTrend: (params?: { days?: number }) =>
    request.post<unknown>('/ai/admin/dashboard/quota-trend', params ?? {})
      .then(data => normalizeDashboardList<AiQuotaTrendItem>(data)),
  /** 按 call_type 汇总 tokens、calls、tokenSharePct */
  dashboardCostBreakdown: (params?: { days?: number }) =>
    request.post<unknown>('/ai/admin/dashboard/cost-breakdown', params ?? {})
      .then(data => normalizeDashboardList<AiCostBreakdownItem>(data)),

  // Infra Health
  infraHealth: () =>
    request.post<unknown>('/ai/admin/infra/health', {})
      .then(data => normalizeResponseArray<unknown>(data).map(normalizeInfraHealthItem).filter(item => item.component)),
  cacheStats: () =>
    request.post<unknown>('/ai/admin/infra/cache/stats', {}).then(normalizeCacheStats),
  cacheDiagnostics: () =>
    request.post<unknown>('/ai/admin/infra/cache/diagnostics', {}).then(normalizeCacheDiagnostics),
  searchStats: () =>
    request.post<unknown>('/ai/admin/infra/search/stats', {}).then(normalizeSearchStats),
  infraDetail: () =>
    request.post<unknown>('/ai/admin/infra/detail', {}).then(normalizeInfraDetail),
  monitoringConfig: () =>
    request.post<unknown>('/ai/admin/infra/monitoring-config', {}).then(normalizeMonitoringConfig),

  // Digital Human（实现见 digital-human.ts，避免与后端字段漂移）
  digitalHumanStatus: () => getDigitalHumanStatus(),
  digitalHumanGenerate: (params: {
    scriptText: string
    voiceId?: string
    avatarId?: string
  }) => generateDigitalHumanVideo(params),

  // Topic (Evolution)
  topicList: (params?: { kbId?: number; accountId?: number; scopeGlobal?: boolean }) =>
    request.post<unknown>('/ai/evolution/topic/list', params ?? {}).then((data) => normalizeResponseArray<AiEvolveTopicVO>(data)),
  topicSave: (params: AiEvolveTopicSaveVO) => request.post<void>('/ai/evolution/topic/save', params),
  topicDelete: (id: number) => request.post<void>('/ai/evolution/topic/delete', { id }),
  topicImport: (filePath: string) => request.post<Record<string, unknown>>('/ai/evolution/topic/import', { filePath }),
  evolveRoi: (params?: { kbId?: number }) =>
    request.post<unknown>('/ai/evolution/roi', params ?? {}).then(normalizeEvolveRoiPayload),
  scoreTrend: (params?: { days?: number; kbId?: number }) =>
    request.post<unknown>('/ai/evolution/score-trend', params ?? {}).then(normalizeScoreTrendRows),
  evolveStatus: () => request.post<Record<string, unknown>>('/ai/evolution/status', {}),
  knowledgeEvolutionReport: (params: Record<string, unknown>) =>
    request.post<unknown>('/ai/knowledge-evolution/report', params).then(normalizeKnowledgeEvolutionReport),
  knowledgeEvolutionAnalyze: (params?: KnowledgeEvolutionAnalyzeRequest) =>
    request.post<unknown>('/ai/knowledge-evolution/analyze', params ?? {}).then(normalizeKnowledgeEvolutionAnalyze),
  knowledgeEvolutionAutoOptimize: (params: KnowledgeEvolutionAutoOptimizeRequest) =>
    request.post<unknown>('/ai/knowledge-evolution/auto-optimize', params).then(normalizeKnowledgeEvolutionAutoOptimizeResult),
  evolveTaskReport: (taskId: number) =>
    request.post<Record<string, unknown>>('/ai/admin/evolve/report/by-task', { taskId }),

  // Evolution Task Queue (08-evolution v3.0)
  evolveTaskList: (params?: { page?: number; rows?: number; taskType?: string; status?: number | string; kbId?: number }) =>
    request.post<unknown>('/ai/evolution/task/list', params ?? {})
      .then((data) => normalizeResponsePage<AiEvolveTaskVO, AiEvolveTaskVO>(data, item => item as AiEvolveTaskVO, Number(params?.page ?? 0), Number(params?.rows ?? 20))),
  evolveTaskTrigger: (params: { taskType: string; targetKbId?: number; targetId?: number; priority?: number }) =>
    request.post<{ taskId: string; message?: string; taskType?: string }>('/ai/evolution/task/trigger', params),
  evolveTaskCancel: (taskId: number) =>
    request.post<void>('/ai/evolution/task/cancel', { taskId }),

  // Pending Deepen
  pendingDeepenList: (params?: { page?: number; rows?: number; status?: number }) =>
    request.post<PageResult<Record<string, unknown>>>('/ai/evolution/pending-deepen/list', params ?? {}),
  pendingDeepenTrigger: (id: number) =>
    request.post<void>('/ai/evolution/pending-deepen/trigger', { id }),

  // Quality Score History
  qualityScoreHistory: (params?: { kbId?: number; days?: number }) =>
    request.post<unknown>('/ai/evolution/quality-score/history', params ?? {}).then(normalizeScoreTrendRows),

  // Evolution Execution Log
  evolutionExecutionList: (params?: { page?: number; rows?: number; agentType?: string }) =>
    request.post<PageResult<Record<string, unknown>>>('/ai/evolution/execution/list', params ?? {}),
  evolutionExecute: (params: { agentType: string; targetKbId?: number }) =>
    request.post<{ executionId: number; started?: number; message?: string }>('/ai/evolution/execution/execute', params),
  evolutionCancel: (executionId: number) =>
    request.post<void>('/ai/evolution/execution/cancel', { executionId }),
  evolutionExecutionReport: (executionId: number) =>
    request.post<Record<string, unknown>>('/ai/evolution/execution/report', { executionId }),

  // KB Source Management（KnowledgeSourceController：管理员知识源目录，不是知识库文档同步任务）
  kbSourceList: (params?: { page?: number; rows?: number; keyword?: string; sourceName?: string; sourceType?: string; status?: number }) =>
    request.post<PageResult<AiKnowledgeSourceVO>>('/ai/admin/knowledge-source/search', params ?? {}),
  kbSourceSave: (params: { id?: number; sourceName: string; sourcePath: string; sourceType?: string; status?: number }) =>
    request.post<number>('/ai/admin/knowledge-source/save', params),
  kbSourceDelete: (id: number) =>
    request.post<void>(`/ai/admin/knowledge-source/delete?id=${encodeURIComponent(String(id))}`, {}),

  // AI Dashboard Stats
  dashboardStats: () =>
    request.post<unknown>('/ai/admin/dashboard/stats', {}).then(normalizeDashboardStats),
  dashboardKbQualityRescan: (params?: { limit?: number }) =>
    request.post<unknown>('/ai/admin/dashboard/kb-quality/rescan', params ?? {}).then(normalizeQualityRescan),

  // Quota Management
  quotaGet: () =>
    request.post<unknown>('/ai/admin/quota/get', {}).then(normalizeQuotaOverview),
  quotaUpdate: (params: Record<string, unknown>) =>
    request.post<void>('/ai/admin/quota/update', params),
  quotaHistory: (params?: { page?: number; rows?: number; feature?: string }) =>
    request.post<unknown>('/ai/admin/quota/history', params ?? {}).then(data => normalizeQuotaHistoryPage(data, params)),

  // Task Model Config
  taskModelConfigList: (params?: { taskCode?: string }) =>
    request.post<unknown>('/ai/admin/task-model-config/list', params ?? {}).then(normalizeTaskModelConfigRows),
  taskModelConfigGet: (id: number) =>
    request.post<unknown>('/ai/admin/task-model-config/get', { id }).then(normalizeTaskModelConfigRow),
  taskModelConfigSave: (params: AiTaskModelConfigSavePayload) =>
    request.post<number>('/ai/admin/task-model-config/save', params),
  taskModelConfigDelete: (id: number) =>
    request.post<void>('/ai/admin/task-model-config/delete', { id }),

  // Index Queue（知识库归属用户可访问，见 POST /ai/knowledge-base/{kbId}/index-queue/list）
  indexQueueList: (kbId: number, params?: { page?: number; rows?: number; status?: string; keyword?: string }) =>
    request.post<unknown>(`/ai/knowledge-base/${kbId}/index-queue/list`, params ?? {})
      .then(data => normalizeIndexQueuePage(data, params)),

  /** 进化适应度记录（POST /ai/knowledge-base/{kbId}/evolution-fitness/list） */
  evolutionFitnessList: (kbId: number, params?: EvolutionFitnessListParams) =>
    request.post<PageResult<EvolutionFitnessRecordVO>>(`/ai/knowledge-base/${kbId}/evolution-fitness/list`, params ?? {}),

  // Evolution Review（后端基路径 /ai/evolution-review）
  evolutionReviewList: (params?: { page?: number; rows?: number; status?: number | string }) =>
    request.post<unknown>('/ai/evolution-review/list', params ?? {})
      .then((data) => normalizeResponsePage<EvolutionReviewTaskRow, EvolutionReviewTaskRow>(data, item => item as EvolutionReviewTaskRow, Number(params?.page ?? 0), Number(params?.rows ?? 20))),
  evolutionReviewApprove: (taskId: number, comment?: string) =>
    request.post<void>('/ai/evolution-review/approve', { taskId, comment }),
  evolutionReviewReject: (taskId: number, reason: string) =>
    request.post<void>('/ai/evolution-review/reject', { taskId, reason, comment: reason }),
  evolutionReviewStats: () =>
    request.post<unknown>('/ai/evolution-review/stats', {}).then(normalizeReviewStats),

  // Model Benchmark（POST /api/v1/ai/model-benchmark/*）
  modelBenchmarkComparison: (params: { taskCode?: string }) =>
    request.post<unknown>('/ai/model-benchmark/comparison', params).then(normalizeModelBenchmarkComparisonRows),
  modelBenchmarkBestModel: (taskCode: string, priority?: string) =>
    request.post<unknown>('/ai/model-benchmark/best-model', {
      taskCode,
      priority: priority ?? 'latency',
    }).then(normalizeModelBenchmarkBestModel),
  modelBenchmarkRecord: (params: {
    modelId: number
    taskCode?: string
    latencyMs?: number
    tokensUsed?: number
    success?: boolean
  }) => request.post<void>('/ai/model-benchmark/record', params),
}
