import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  AiModelVO,
  AiModelAdminVO,
  AiModelSavePayload,
  AiLiveReviewVO,
  AiEvolveTaskVO,
  AiEvolveTopicVO,
  AiEvolveTopicSaveVO,
  AiKnowledgeSourceVO,
  AiQuotaHistoryVO,
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
  AiSearchStatsVO,
  AiInfraDetailFullVO,
  AiMonitoringConfigVO,
  AiCallVolumeTrendParams,
} from '@/types/ai'
import type { EvolveRoiPayload, EvolutionReviewTaskRow, EvolutionReviewStats, QualityScoreTrendPoint } from '@/types/evolutionEngine'
import type {
  EvolutionAnalysisResultVO,
  KnowledgeEvolutionAnalyzeRequest,
  KnowledgeEvolutionAutoOptimizeRequest,
  KnowledgeEvolutionAutoOptimizeResult,
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
}
export interface KbQuery { page?: number; rows?: number; name?: string }

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
  templateType: string
  templateName: string
  templateContent: string
  isActive: number
  variables?: string
  usageCount?: number
  lastUsedAt?: string
  tags?: string
  createTime: string
  updateTime?: string
}
export interface PromptTemplateQuery { page?: number; rows?: number; templateType?: string }

export const getModelsByTaskCode = (taskCode: string) =>
  request.post<AiModelVO[]>('/ai/model/list-by-task', { taskCode })

export const listAiModels = (page: number) =>
  request.post<AiModelVO[]>('/ai/model/list', { page, rows: 100 })

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

export const aiApi = {
  // Knowledge Base
  kbList: (params: KbQuery) => request.post<KnowledgeBase[]>('/ai/knowledge-base/list', params),
  kbCreate: (params: { name?: string; description?: string }) => request.post<KnowledgeBase>('/ai/knowledge-base/create', params),
  kbDelete: (id: number) => request.delete<void>(`/ai/knowledge-base/${id}`),
  kbSearch: (kbId: number, p: Record<string, unknown>) => request.post<KbSearchHit[]>(`/ai/knowledge-base/${kbId}/search`, p),
  kbDedupPreview: (kbId: number) => request.post<Record<string, unknown>>(`/ai/knowledge-base/${kbId}/dedup-preview`, {}),
  kbImportFromPath: (params: Record<string, unknown>) => request.post<void>('/ai/knowledge-base/import-from-path', params),
  kbImportActiveJobs: () => request.post<Record<string, unknown>[]>('/ai/knowledge-base/import-active-jobs', {}),

  // KB Documents
  docList: (kbId: number, params: { page?: number; rows?: number; keyword?: string; sourceType?: string }) => request.post<PageResult<KbDocument>>(`/ai/knowledge-base/${kbId}/documents`, params),
  docDelete: (docId: number) => request.delete<void>(`/ai/knowledge-base/document/${docId}`),

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
  promptTemplateList: (params: PromptTemplateQuery) => request.post<PageResult<PromptTemplate>>('/ai/prompt-template/list', params),
  promptTemplateSave: (params: Partial<PromptTemplate>) => request.post<void>('/ai/prompt-template/save', params),
  promptTemplateDelete: (id: number) => request.post<void>('/ai/prompt-template/delete', { id }),
  promptTemplateGetActive: (templateType: string) => request.post<PromptTemplate>('/ai/prompt-template/get-active', { templateType }),
  promptTemplateTestRender: (params: { templateContent: string; variables?: Record<string, string> }) =>
    request.post<{ rendered: string; variables: string[]; missingVariables: string[] }>('/ai/prompt-template/test-render', params),
  promptTemplateExtractVariables: (templateContent: string) =>
    request.post<string[]>('/ai/prompt-template/extract-variables', { templateContent }),
  promptTemplateRecordUsage: (id: number) =>
    request.post<void>('/ai/prompt-template/record-usage', { id }),

  // AI Chat / Generate
  chat: (params: Record<string, unknown>) => request.post<Record<string, unknown>>('/ai/chat', params),
  generate: (params: Record<string, unknown>) => request.post<Record<string, unknown>>('/ai/generate', params),

  // AI Admin
  adminCallLogList: (params: Record<string, unknown>) => request.post<PageResult<Record<string, unknown>>>('/ai/admin/call-log/list', params),
  adminInfraModelList: (params: Record<string, unknown>) => request.post<PageResult<Record<string, unknown>>>('/ai/admin/infra/model/list', params),

  /** 模型配置（管理端）：列表为掩码 VO */
  adminModelsList: () => request.post<AiModelAdminVO[]>('/ai/admin/models/list', {}),
  adminModelsSave: (params: AiModelSavePayload) => request.post<void>('/ai/admin/models/save', params),
  adminModelsDelete: (id: number) => request.post<void>('/ai/admin/models/delete', { id }),
  adminModelsSetDefault: (id: number) => request.post<void>('/ai/admin/models/set-default', { id }),
  adminModelsTestConnection: (id: number) =>
    request.post<{ success: boolean; errorMsg?: string | null; tokensUsed?: number }>('/ai/admin/models/test-connection', { id }),

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
  mediaTtsVoices: () => request.post<AiMediaTtsVoiceInfo[]>('/ai/media/tts/voices', {}),
  mediaImageHistory: (params?: { page?: number; size?: number }) =>
    request.post<AiMediaImageHistory[]>('/ai/media/image/history', params ?? {}),

  // Call Analytics
  callVolumeTrend: (params?: AiCallVolumeTrendParams) => request.post<AiCallVolumeTrendItem[]>('/ai/admin/dashboard/call-volume-trend', params ?? {}),
  callTypeDistribution: (params?: Record<string, unknown>) => request.post<AiCallTypeDistributionItem[]>('/ai/admin/dashboard/call-type-distribution', params ?? {}),
  /** 额度按日汇总（date / usedCount / maxCount），与 ai_call_quota 聚合一致 */
  quotaTrend: (params?: { days?: number }) =>
    request.post<AiQuotaTrendItem[]>('/ai/admin/dashboard/quota-trend', params ?? {}),
  /** 按 call_type 汇总 tokens、calls、tokenSharePct */
  dashboardCostBreakdown: (params?: { days?: number }) =>
    request.post<AiCostBreakdownItem[]>('/ai/admin/dashboard/cost-breakdown', params ?? {}),

  // Infra Health
  infraHealth: () => request.post<AiInfraHealthItem[]>('/ai/admin/infra/health', {}),
  cacheStats: () => request.post<AiCacheStatsVO>('/ai/admin/infra/cache/stats', {}),
  searchStats: () => request.post<AiSearchStatsVO>('/ai/admin/infra/search/stats', {}),
  infraDetail: () => request.post<AiInfraDetailFullVO>('/ai/admin/infra/detail', {}),
  monitoringConfig: () => request.post<AiMonitoringConfigVO>('/ai/admin/infra/monitoring-config', {}),

  // Digital Human（实现见 digital-human.ts，避免与后端字段漂移）
  digitalHumanStatus: () => getDigitalHumanStatus(),
  digitalHumanGenerate: (params: {
    scriptText: string
    voiceId?: string
    avatarId?: string
  }) => generateDigitalHumanVideo(params),

  // Topic (Evolution)
  topicList: (params?: { kbId?: number; accountId?: number; scopeGlobal?: boolean }) =>
    request.post<AiEvolveTopicVO[]>('/ai/evolution/topic/list', params ?? {}),
  topicSave: (params: AiEvolveTopicSaveVO) => request.post<void>('/ai/evolution/topic/save', params),
  topicDelete: (id: number) => request.post<void>('/ai/evolution/topic/delete', { id }),
  topicImport: (filePath: string) => request.post<Record<string, unknown>>('/ai/evolution/topic/import', { filePath }),
  evolveRoi: (params?: { kbId?: number }) =>
    request.post<EvolveRoiPayload>('/ai/evolution/roi', params ?? {}),
  scoreTrend: (params?: { days?: number; kbId?: number }) =>
    request.post<QualityScoreTrendPoint[]>('/ai/evolution/score-trend', params ?? {}),
  evolveStatus: () => request.post<Record<string, unknown>>('/ai/evolution/status', {}),
  knowledgeEvolutionReport: (params: Record<string, unknown>) =>
    request.post<Record<string, unknown>>('/ai/knowledge-evolution/report', params),
  knowledgeEvolutionAnalyze: (params?: KnowledgeEvolutionAnalyzeRequest) =>
    request.post<EvolutionAnalysisResultVO>('/ai/knowledge-evolution/analyze', params ?? {}),
  knowledgeEvolutionAutoOptimize: (params: KnowledgeEvolutionAutoOptimizeRequest) =>
    request.post<KnowledgeEvolutionAutoOptimizeResult>('/ai/knowledge-evolution/auto-optimize', params),
  evolveTaskReport: (taskId: number) =>
    request.post<Record<string, unknown>>('/ai/admin/evolve/report/by-task', { taskId }),

  // Evolution Task Queue (08-evolution v3.0)
  evolveTaskList: (params?: { page?: number; rows?: number; taskType?: string; status?: number | string; kbId?: number }) =>
    request.post<PageResult<AiEvolveTaskVO>>('/ai/evolution/task/list', params ?? {}),
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
    request.post<QualityScoreTrendPoint[]>('/ai/evolution/quality-score/history', params ?? {}),

  // Evolution Execution Log
  evolutionExecutionList: (params?: { page?: number; rows?: number; agentType?: string }) =>
    request.post<PageResult<Record<string, unknown>>>('/ai/evolution/execution/list', params ?? {}),
  evolutionExecute: (params: { agentType: string; targetKbId?: number }) =>
    request.post<{ executionId: number; started?: number; message?: string }>('/ai/evolution/execution/execute', params),
  evolutionCancel: (executionId: number) =>
    request.post<void>('/ai/evolution/execution/cancel', { executionId }),
  evolutionExecutionReport: (executionId: number) =>
    request.post<Record<string, unknown>>('/ai/evolution/execution/report', { executionId }),

  // KB Source Management (04-knowledge-ai v3.0)
  kbSourceList: (params?: { kbId?: number; page?: number; rows?: number }) =>
    request.post<PageResult<AiKnowledgeSourceVO>>('/ai/knowledge-base/source/list', params ?? {}),
  kbSourceSave: (params: Record<string, unknown>) =>
    request.post<void>('/ai/knowledge-base/source/save', params),
  kbSourceDelete: (id: number) =>
    request.post<void>('/ai/knowledge-base/source/delete', { id }),
  kbSourceSync: (id: number) =>
    request.post<{ jobId: string }>('/ai/knowledge-base/source/sync', { id }),
  kbSourceTestConnection: (id: number) =>
    request.post<{ success: boolean; message?: string }>('/ai/knowledge-base/source/test-connection', { id }),

  // AI Dashboard Stats
  dashboardStats: () =>
    request.post<AiDashboardStatsVO>('/ai/admin/dashboard/stats', {}),

  // Quota Management
  quotaGet: () =>
    request.post<AiAdminQuotaOverviewVO>('/ai/admin/quota/get', {}),
  quotaUpdate: (params: Record<string, unknown>) =>
    request.post<void>('/ai/admin/quota/update', params),
  quotaHistory: (params?: { page?: number; rows?: number; feature?: string }) =>
    request.post<PageResult<AiQuotaHistoryVO>>('/ai/admin/quota/history', params ?? {}),

  // Task Model Config
  taskModelConfigList: (params?: { taskCode?: string }) =>
    request.post<AiTaskModelConfigRow[]>('/ai/admin/task-model-config/list', params ?? {}),
  taskModelConfigGet: (id: number) =>
    request.post<AiTaskModelConfigRow>('/ai/admin/task-model-config/get', { id }),
  taskModelConfigSave: (params: AiTaskModelConfigSavePayload) =>
    request.post<number>('/ai/admin/task-model-config/save', params),
  taskModelConfigDelete: (id: number) =>
    request.post<void>('/ai/admin/task-model-config/delete', { id }),

  // Index Queue（知识库归属用户可访问，见 POST /ai/knowledge-base/{kbId}/index-queue/list）
  indexQueueList: (kbId: number, params?: { page?: number; rows?: number; status?: string; keyword?: string }) =>
    request.post<PageResult<KbIndexQueueRow>>(`/ai/knowledge-base/${kbId}/index-queue/list`, params ?? {}),

  /** 进化适应度记录（POST /ai/knowledge-base/{kbId}/evolution-fitness/list） */
  evolutionFitnessList: (kbId: number, params?: EvolutionFitnessListParams) =>
    request.post<PageResult<EvolutionFitnessRecordVO>>(`/ai/knowledge-base/${kbId}/evolution-fitness/list`, params ?? {}),

  // Evolution Review（后端基路径 /ai/evolution-review）
  evolutionReviewList: (params?: { page?: number; rows?: number; status?: number | string }) =>
    request.post<PageResult<EvolutionReviewTaskRow>>('/ai/evolution-review/list', params ?? {}),
  evolutionReviewApprove: (taskId: number, comment?: string) =>
    request.post<void>('/ai/evolution-review/approve', { taskId, comment }),
  evolutionReviewReject: (taskId: number, reason: string) =>
    request.post<void>('/ai/evolution-review/reject', { taskId, reason }),
  evolutionReviewStats: () =>
    request.post<EvolutionReviewStats>('/ai/evolution-review/stats', {}),

  // Model Benchmark（POST /api/v1/ai/model-benchmark/*）
  modelBenchmarkComparison: (params: { taskCode?: string }) =>
    request.post<AiModelBenchmarkComparisonRow[]>('/ai/model-benchmark/comparison', params),
  modelBenchmarkBestModel: (taskCode: string, priority?: string) =>
    request.post<AiModelBenchmarkBestModelVO>('/ai/model-benchmark/best-model', {
      taskCode,
      priority: priority ?? 'latency',
    }),
  modelBenchmarkRecord: (params: {
    modelId: number
    taskCode?: string
    latencyMs?: number
    tokensUsed?: number
    success?: boolean
  }) => request.post<void>('/ai/model-benchmark/record', params),
}
