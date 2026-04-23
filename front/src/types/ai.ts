/**
 * AI 模块 - TypeScript 类型定义
 * 与后端 module/ai/ Entity/VO 对齐
 */

import type { BasicQuery } from './common'

// ─── AI 模型 ──────────────────────────────────────────────────────────────

export interface AiModelVO {
  id: number
  modelName: string
  modelProvider?: string
  modelVersion?: string
  maxTokens?: number
  temperature?: number
  status?: number
  costPer1kTokens?: number
  quotaLimit?: number
  quotaUsed?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

/** 管理端 GET /ai/admin/models/list：掩码 apiKey，含 resolvedBaseUrl */
export interface AiModelAdminVO {
  id: number
  modelName: string
  modelProvider: string
  /** 请求体中的 model 名称，对应库字段 model_version */
  modelVersion: string
  /** 库内自定义 Base URL，编辑表单回显 */
  apiBaseUrl?: string | null
  apiKeyMasked: string
  resolvedBaseUrl?: string
  maxTokens?: number
  temperature?: number
  status?: number
  isDefault?: number
  costPer1kTokens?: number
  quotaLimit?: number
  quotaUsed?: number
  createTime?: string
  updateTime?: string
}

/** 与 AiModelSaveVO 对齐；endpoint 写入后端 model_version（非 Base URL） */
export interface AiModelSavePayload {
  id?: number
  modelName: string
  provider: string
  /** 厂商 API 中的 model 名称，非网关 Base URL */
  endpoint: string
  /** 可选；OpenAI 兼容 API 根地址。编辑时传空字符串可清除 */
  apiBaseUrl?: string | null
  apiKey?: string
  maxTokens: number
  temperature: number
  status?: number
  isDefault?: number
}

// ─── AI 任务 ──────────────────────────────────────────────────────────────

export interface AiTaskSearchVO extends BasicQuery {
  keyword?: string
  taskType?: string
  taskStatus?: number
}

export interface AiTaskVO {
  id: number
  userId?: number
  taskType?: string
  inputContent?: string
  prompt?: string
  modelUsed?: string
  outputContent?: string
  tokensUsed?: number
  taskStatus?: number
  errorMsg?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── Prompt 模板 ──────────────────────────────────────────────────────────

export interface AiPromptTemplateVO {
  id: number
  userId?: number
  templateName: string
  templateContent?: string
  category?: string
  variables?: string
  status?: number
  templateCode?: string
  variantName?: string
  version?: number
  systemPrompt?: string
  userPromptTpl?: string
  modelHint?: string
  temperature?: number
  maxTokens?: number
  isActive?: boolean
  isDefault?: boolean
  usageCount?: number
  avgScore?: number
  p50Score?: number
  p90Score?: number
  ownerId?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 知识库 ───────────────────────────────────────────────────────────────

export interface AiKnowledgeBaseVO {
  id: number
  userId?: number
  kbName: string
  kbType?: string
  description?: string
  totalDocuments?: number
  totalTokens?: number
  embeddingModel?: string
  status?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 知识库文档 ───────────────────────────────────────────────────────────

/** 与 AiAdminInfraServiceImpl.queueToMap 对齐（知识库索引队列列表） */
export interface KbIndexQueueRow {
  id: number
  sourceType: string
  sourceId: number
  targetKbId: number
  priority: number
  status: string
  retryCount: number
  errorMsg?: string | null
  createTime?: string | null
  contentPreview?: string | null
}

/** 进化适应度记录（evolution_fitness_record） */
export interface EvolutionFitnessRecordVO {
  id: number
  kbId?: number
  taskId: string
  parentTaskId?: string | null
  metricName: string
  metricValue?: number | null
  payloadJson?: string | null
  experimentId?: string | null
  createTime?: string | null
}

export interface EvolutionFitnessListParams {
  page?: number
  rows?: number
  sortName?: string
  sortOrder?: 'asc' | 'desc'
  taskId?: string
  metricName?: string
  startTimeMs?: number
  endTimeMs?: number
  experimentId?: string
}

export interface AiKbDocumentVO {
  id: number
  kbId: number
  title: string
  content?: string
  fileType?: string
  fileSize?: number
  chunkCount?: number
  tokenCount?: number
  status?: number
  boostFactor?: number
  sourceType?: string
  expiryStatus?: number
  lastExpiryCheck?: string
  contentFingerprint?: string
  retrievalCount?: number
  citationCount?: number
  lastRetrievalAt?: string
  lastCitationAt?: string
  qualityTier?: number
  qualityHeuristicScore?: number
  lastQualityEvalAt?: string
  metadata?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 知识源 ───────────────────────────────────────────────────────────────

export interface AiKnowledgeSourceSearchVO extends BasicQuery {
  keyword?: string
  sourceType?: string
  status?: number
}

export interface AiKnowledgeSourceVO {
  id: number
  sourceName: string
  sourcePath?: string
  sourceType?: string
  fileCount?: number
  indexCount?: number
  lastIndexTime?: string
  status?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 进化主题 ─────────────────────────────────────────────────────────────

export interface AiEvolveTopicVO {
  id: number
  kbId?: number
  accountId?: number
  /** 后端 JSON 字段名 topicName，对应实体列 topic */
  topicName?: string
  topic?: string
  category?: string
  priority?: number
  source?: string
  usedCount?: number
  lastUsedTime?: string
  scoreAvg?: number
  status?: number
  ruleConfig?: Record<string, unknown>
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface AiEvolveTopicSaveVO {
  id?: number
  kbId?: number
  accountId?: number
  topicName?: string
  topic?: string
  category?: string
  priority?: number
  source?: string
  status?: number
  ruleConfig?: Record<string, unknown>
  [key: string]: unknown
}

// ─── 进化任务 ─────────────────────────────────────────────────────────────

export interface AiEvolveTaskVO {
  id: number
  kbId?: number
  taskNo: string
  topicIds?: string
  topicTexts?: string
  evolveAngle?: string
  gatherMode?: string
  contextLength?: number
  modelUsed?: string
  fallbackTier?: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  durationMs?: number
  scoreTotal?: number
  scoreDetail?: string
  expandedCount?: number
  deepenedCount?: number
  hadQualityHint?: number
  status?: number | string
  dependsOnTaskNos?: string
  blockedReason?: string
  errorMessage?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 进化引擎状态 ─────────────────────────────────────────────────────────

export interface AiEvolveStatusVO {
  totalTopics?: number
  totalTasks?: number
  recentTasks?: AiEvolveTaskVO[]
  scoreTrend?: Array<{ date: string; score: number }>
  lastRunTime?: string
  [key: string]: unknown
}

// ─── 额度信息 ─────────────────────────────────────────────────────────────

/** 管理端 GET /ai/admin/quota/get 中 items 单项（与后端 buildQuotaItem 对齐） */
export interface AiQuotaItemRow {
  feature: string
  limit: number
  used: number
  unit?: string
  period?: string
}

/** 管理端配额总览（items + dailyMax；后端另将各 item 平铺到顶层同名 key） */
export interface AiAdminQuotaOverviewVO {
  items: AiQuotaItemRow[]
  dailyMax?: number
  [key: string]: unknown
}

export interface AiQuotaInfoVO {
  totalQuota?: number
  usedQuota?: number
  remainingQuota?: number
  remaining?: number
  usedCount?: number
  maxCount?: number
  resetDate?: string
  quotaByModel?: Record<string, { limit: number; used: number }>
  [key: string]: unknown
}

// ─── 直播审核记录 ─────────────────────────────────────────────────────────

export interface AiLiveReviewVO {
  id?: number
  sessionId?: number
  sessionTitle?: string
  reviewType?: string
  status?: string
  score?: number
  feedback?: string
  reviewerId?: number
  reviewerName?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 额度历史记录 ─────────────────────────────────────────────────────────

export interface AiQuotaHistoryVO {
  id?: number
  feature?: string
  used?: number
  limit?: number
  period?: string
  unit?: string
  createTime?: string
  date?: string
  count?: number
  [key: string]: unknown
}

// ─── 任务-模型配置（ai_task_model_config + 列表行模型名）────────────────────

export interface AiTaskModelConfigRow {
  id: number
  taskCode: string
  taskName: string
  taskGroup: string
  primaryModelId?: number | null
  fallbackModelId?: number | null
  fallback2ModelId?: number | null
  primaryModelName?: string | null
  fallbackModelName?: string | null
  fallback2ModelName?: string | null
  timeoutSeconds?: number | null
  maxRetries: number
  sortOrder: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface AiTaskModelConfigSavePayload {
  id?: number
  taskCode: string
  taskName: string
  taskGroup?: string
  primaryModelId?: number | null
  fallbackModelId?: number | null
  fallback2ModelId?: number | null
  timeoutSeconds?: number | null
  maxRetries?: number
  sortOrder?: number
  status?: number
}

// ─── 模型基准（ai_model_benchmark 聚合）────────────────────────────────────

export interface AiModelBenchmarkComparisonRow {
  modelId: number
  modelName?: string
  taskCode: string
  avgLatencyMs: number
  successRate: number
  avgTokens: number
  totalCalls: number
}

export interface AiModelBenchmarkBestModelVO {
  modelId: number
  modelName?: string
  taskCode: string
  priority?: string
}

// ─── AI 看板 ──────────────────────────────────────────────────────────────

export interface AiCallVolumeTrendItem {
  date?: string
  hour?: number
  count?: number
  callType?: string
  [key: string]: unknown
}

/** POST /ai/admin/dashboard/quota-trend 单日行（后端 usedCount / maxCount） */
export interface AiQuotaTrendItem {
  date: string
  usedCount?: number
  maxCount?: number
  used?: number
  remaining?: number
  [key: string]: unknown
}

export interface AiCallTypeDistributionItem {
  callType: string
  count: number
  percentage?: number
  [key: string]: unknown
}

/** POST /ai/admin/dashboard/cost-breakdown（按 call_type 汇总） */
export interface AiCostBreakdownItem {
  callType: string
  tokens: number
  calls: number
  tokenSharePct: number
}

/** POST /ai/admin/dashboard/stats */
export interface AiDashboardStatsVO {
  todayCalls: number
  monthTokens: number
  successRate: number
  avgQualityScore: number
}

/** POST /ai/admin/infra/health 单项 */
export interface AiInfraHealthItem {
  component: string
  ok: boolean
  message?: string | null
}

/** POST /ai/admin/infra/cache/stats */
export interface AiCacheStatsVO {
  hit?: number
  miss?: number
  total?: number
  hitRate?: number
  keyCount?: number
  [key: string]: unknown
}

/** POST /api/v1/ai/admin/infra/detail 扩展（与 AiAdminInfraServiceImpl 对齐） */
export interface AiInfraPostgresDetail {
  ok?: boolean
  host?: string
  database?: string
  poolActive?: number | string
  poolTotal?: number | string
  responseMs?: number | string
  error?: string
  version?: string
  connections?: number
  dbSize?: string
}

export interface AiInfraMilvusDetail {
  ok?: boolean
  totalVectors?: number | string
  collectionCount?: number | string
  version?: string
  collections?: number
  entities?: number
  status?: string
  error?: string
}

export interface AiInfraElasticsearchDetail {
  ok?: boolean
  status?: string
  clusterName?: string
  version?: string
  indices?: number
  docCount?: number
  error?: string
}

export interface AiInfraSuggestion {
  component?: string
  message?: string | null
}

export interface AiInfraDetailFullVO {
  postgresql?: AiInfraPostgresDetail
  milvus?: AiInfraMilvusDetail
  elasticsearch?: AiInfraElasticsearchDetail
  recommendations?: string[]
  suggestions?: AiInfraSuggestion[]
  lastUpdate?: string
  [key: string]: unknown
}

/** POST /ai/admin/infra/monitoring-config */
export interface AiMonitoringConfigVO {
  grafanaUrl?: string
  [key: string]: unknown
}

/** 调用量趋势参数（day 或 hour 模式） */
export interface AiCallVolumeTrendParams {
  days?: number
  hours?: number
  callType?: string
}

// ─── 导入相关 ─────────────────────────────────────────────────────────────

export interface AiImportReportVO {
  id: number
  kbId?: number
  sourcePath?: string
  totalFiles?: number
  processedFiles?: number
  failedFiles?: number
  totalChunks?: number
  status?: string
  errorMessage?: string
  createTime?: string
  [key: string]: unknown
}

export interface AiImportStatusVO {
  jobId: string
  status: string
  progress?: number
  totalFiles?: number
  processedFiles?: number
  failedFiles?: number
  message?: string
  [key: string]: unknown
}

// ─── 去重预览 ─────────────────────────────────────────────────────────────

export interface AiDedupPreviewVO {
  duplicateCount?: number
  duplicates?: Array<{
    docId: number
    title: string
    similarity: number
  }>
  [key: string]: unknown
}

// ─── 搜索结果 ─────────────────────────────────────────────────────────────

export interface AiSearchResultItem {
  docId?: number
  chunkId?: number
  title?: string
  content?: string
  score?: number
  source?: string
  labels?: string[]
  [key: string]: unknown
}

// ─── 爆款拆解 ─────────────────────────────────────────────────────────────

export interface AiViralAnalysisSearchVO extends BasicQuery {
  status?: number
}

export interface AiViralAnalysisVO {
  id: number
  title?: string
  videoUrl?: string
  platform?: string
  playCount?: number
  likeCount?: number
  commentCount?: number
  shareCount?: number
  analysisContent?: string
  keyElements?: string
  status?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

// ─── 媒体生成 ─────────────────────────────────────────────────────────────

export interface AiText2ImgResultVO {
  imageUrl?: string
  width?: number
  height?: number
  model?: string
  tokensUsed?: number
  [key: string]: unknown
}

export interface AiTtsResultVO {
  audioUrl?: string
  duration?: number
  format?: string
  voice?: string
  [key: string]: unknown
}

export interface AiTtsVoiceVO {
  voiceId?: string
  name?: string
  language?: string
  gender?: string
  previewUrl?: string
  [key: string]: unknown
}

// ─── 视频编辑 ─────────────────────────────────────────────────────────────

export interface AiVideoResultVO {
  videoUrl?: string
  duration?: number
  format?: string
  resolution?: string
  [key: string]: unknown
}

// ─── 基础设施 ─────────────────────────────────────────────────────────────

export interface AiInfraDetailVO {
  postgres?: { version?: string; connections?: number; dbSize?: string }
  postgresql?: { version?: string; connections?: number; dbSize?: string }
  milvus?: { version?: string; collections?: number; entities?: number; status?: string }
  elasticsearch?: { version?: string; indices?: number; docCount?: number; status?: string }
  redis?: { version?: string; usedMemory?: string; connectedClients?: number }
  recommendations?: string[]
  suggestions?: string[]
  lastUpdate?: string
  [key: string]: unknown
}

export interface AiSearchStatsVO {
  p50Ms?: number
  p95Ms?: number
  p99Ms?: number
  qps?: number
  totalQueries?: number
  totalRequests?: number
  avgLatencyMs?: number
  latencyDistribution?: Record<string, number>
  message?: string
  [key: string]: unknown
}

// ─── 调用日志 ─────────────────────────────────────────────────────────────

export interface AiCallLogSearchVO extends BasicQuery {
  callType?: string
  status?: number
  keyword?: string
  startTime?: string
  endTime?: string
}

export interface AiCallLogVO {
  id: number
  userId?: number
  callType?: string
  templateCode?: string
  modelCode?: string
  inputSummary?: string
  outputLength?: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  durationMs?: number
  status?: number
  errorMessage?: string
  isFallback?: number
  referencedChunkIds?: string
  linkedVideoId?: number
  linkedSessionId?: number
  contentEffect?: string
  effectScore?: number
  stageTimings?: string
  createTime?: string
  [key: string]: unknown
}

// ─── AI 多媒体（MediaController /api/v1/ai/media）────────────────────────

/** 与 ImageGenerationService.ImageResult 对齐 */
export interface AiMediaImageResult {
  imageUrl: string
  prompt?: string
  parameters?: Record<string, unknown>
  generationTime?: number
}

/** 与 TtsService.AudioResult 对齐 */
export interface AiMediaAudioResult {
  audioUrl: string
  text?: string
  duration?: number
  fileSize?: number
}

/** 与 VideoEditService.VideoResult 对齐（首尾帧生成等） */
export interface AiMediaVideoResult {
  videoUrl: string
  duration?: number
  fileSize?: number
  format?: string
}

/** 与 TtsService.VoiceInfo 对齐 */
export interface AiMediaTtsVoiceInfo {
  id: string
  name: string
  language?: string
  gender?: string
  description?: string
}

/** 与 ImageGenerationService.ImageGenerationHistory 对齐 */
export interface AiMediaImageHistory {
  id: number
  imageUrl: string
  prompt?: string
  type?: string
  parameters?: Record<string, unknown>
  createTime?: number
}

// ─── 视频对比 ─────────────────────────────────────────────────────────────

export interface AiVideoCompareResultVO {
  mainVideoId?: number
  comparisons?: Array<{
    videoId: number
    similarity?: number
    differences?: string[]
    recommendations?: string[]
  }>
  [key: string]: unknown
}
