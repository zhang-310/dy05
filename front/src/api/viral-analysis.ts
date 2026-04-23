import request from '@/utils/request'
import { ssePost } from '@/utils/sse-client'

const VIRAL_BASE = '/short-video/viral'

export interface ViralComment {
  id?: number
  videoId?: number
  douyinCommentId?: string
  content?: string
  authorName?: string
  authorAvatar?: string
  likeCount?: number
  replyCount?: number
  sentiment?: string
  sentimentScore?: number
  commentTime?: string
  videoSource?: string
}

export interface DeepAnalyzeResult {
  viralVideoId?: number
  transcript?: string
  sceneDescriptions?: string
  analysis?: string
  evidence?: DeepAnalyzeEvidenceSummary
  tokensUsed?: number
  status?: string
  error?: string
}

export interface DeepAnalyzeStartResult {
  taskId: number
  status: string
  message?: string
}

export interface DeepAnalyzeStatusResult {
  taskId?: number
  viralVideoId?: number
  status?: string
  error?: string
  transcript?: string
  sceneDescriptions?: string
  analysisResult?: string
  lightAnalysisResult?: string
  deepAnalysisResult?: string
  deepAnalyzeSteps?: string
  deepAnalyzeProgress?: string
  remakeVariableTable?: string
  deepAnalyzeStartedAt?: string
  deepAnalyzeFinishedAt?: string
  deepAnalyzedAt?: string
  evidenceLevel?: string
  evidenceDetails?: DeepAnalyzeEvidenceDetails
  transcriptEvidenceLevel?: string
  sceneEvidenceLevel?: string
  commentEvidenceLevel?: string
  hasCommentSamples?: boolean
  transcriptDisplayLabel?: string
  sceneDisplayLabel?: string
  inferenceRisk?: boolean
}

export interface DeepAnalyzeEvidenceDetails {
  overallLevel?: string
  transcriptLevel?: string
  sceneLevel?: string
  commentLevel?: string
  hasCommentSamples?: boolean
}

export interface DeepAnalyzeEvidenceSummary extends DeepAnalyzeEvidenceDetails {
  transcriptDisplayLabel?: string
  sceneDisplayLabel?: string
  inferenceRisk?: boolean
}

function sleep(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms))
}

export function startDeepAnalyze(id: number) {
  return request.post<DeepAnalyzeStartResult>(`${VIRAL_BASE}/deep-analyze`, { id })
}

export function getDeepAnalyzeStatus(id: number) {
  return request.post<DeepAnalyzeStatusResult>(`${VIRAL_BASE}/deep-analyze/status`, { id })
}

/** 重跑多轮深度分析中的某一文本轮次（需服务端结果含 _roundsRaw） */
export function retryDeepAnalyzeRound(id: number, round: string) {
  return request.post<Record<string, unknown>>(`${VIRAL_BASE}/deep-analyze/retry-round`, { id, round })
}

function mapStatusToDeepResult(id: number, s: DeepAnalyzeStatusResult): DeepAnalyzeResult {
  const st = s.status ?? ''
  const analysis =
    typeof s.deepAnalysisResult === 'string' && s.deepAnalysisResult.trim()
      ? s.deepAnalysisResult
      : typeof s.analysisResult === 'string'
        ? s.analysisResult
        : undefined
  const evidence = resolveDeepAnalyzeEvidence(s)
  return {
    viralVideoId: id,
    transcript: s.transcript,
    sceneDescriptions: s.sceneDescriptions,
    analysis,
    evidence,
    status: st === 'failed' ? 'failed' : s.error ? 'partial' : 'completed',
    error: s.error,
  }
}

export function resolveDeepAnalyzeEvidence(s: DeepAnalyzeStatusResult): DeepAnalyzeEvidenceSummary | undefined {
  const directDetails = s.evidenceDetails
  const fallbackDetails = parseEvidenceDetailsFromDeepAnalysis(s.deepAnalysisResult)
  const details = directDetails ?? fallbackDetails

  const overallLevel = firstNonEmpty(s.evidenceLevel, details?.overallLevel)
  const transcriptLevel = firstNonEmpty(s.transcriptEvidenceLevel, details?.transcriptLevel)
  const sceneLevel = firstNonEmpty(s.sceneEvidenceLevel, details?.sceneLevel)
  const commentLevel = firstNonEmpty(s.commentEvidenceLevel, details?.commentLevel)
  const hasCommentSamples = typeof s.hasCommentSamples === 'boolean'
    ? s.hasCommentSamples
    : details?.hasCommentSamples

  const transcriptDisplayLabel = s.transcriptDisplayLabel ?? displayLabelForTranscript(transcriptLevel, s.transcript)
  const sceneDisplayLabel = s.sceneDisplayLabel ?? displayLabelForScene(sceneLevel, s.sceneDescriptions)
  const inferenceRisk = typeof s.inferenceRisk === 'boolean'
    ? s.inferenceRisk
    : transcriptLevel === 'inferred' || sceneLevel === 'inferred'

  if (!overallLevel && !transcriptLevel && !sceneLevel && !commentLevel && typeof hasCommentSamples !== 'boolean'
      && !transcriptDisplayLabel && !sceneDisplayLabel && !inferenceRisk) {
    return undefined
  }

  return {
    overallLevel,
    transcriptLevel,
    sceneLevel,
    commentLevel,
    hasCommentSamples,
    transcriptDisplayLabel,
    sceneDisplayLabel,
    inferenceRisk,
  }
}

function parseEvidenceDetailsFromDeepAnalysis(raw?: string): DeepAnalyzeEvidenceDetails | undefined {
  if (!raw || !raw.trim()) {
    return undefined
  }
  try {
    const parsed = JSON.parse(raw) as { evidenceDetails?: DeepAnalyzeEvidenceDetails; evidenceLevel?: string }
    const details = parsed.evidenceDetails
    if (details) {
      return {
        overallLevel: firstNonEmpty(details.overallLevel, parsed.evidenceLevel),
        transcriptLevel: details.transcriptLevel,
        sceneLevel: details.sceneLevel,
        commentLevel: details.commentLevel,
        hasCommentSamples: details.hasCommentSamples,
      }
    }
    if (parsed.evidenceLevel) {
      return { overallLevel: parsed.evidenceLevel }
    }
  } catch {
    /* ignore */
  }
  return undefined
}

function displayLabelForTranscript(level?: string, transcript?: string): string | undefined {
  if (level === 'empirical') return '实证转写'
  if (level === 'inferred' || transcript?.startsWith('【推演口播】')) return '推演口播稿（非 ASR 实录）'
  if (level === 'missing') return '缺失'
  return undefined
}

function displayLabelForScene(level?: string, sceneDescriptions?: string): string | undefined {
  if (level === 'empirical') return '实证场景拆解'
  if (level === 'inferred' || sceneDescriptions?.startsWith('【推演场景】')) return '推演场景（非真实抽帧）'
  if (level === 'missing') return '缺失'
  return undefined
}

function firstNonEmpty(...values: Array<string | undefined>): string | undefined {
  return values.find((value) => typeof value === 'string' && value.trim())
}

async function pollDeepAnalyzeUntilDone(id: number): Promise<DeepAnalyzeResult> {
  const maxAttempts = 300
  for (let i = 0; i < maxAttempts; i++) {
    await sleep(2000)
    const s = await getDeepAnalyzeStatus(id)
    const st = s.status ?? ''
    if (st === 'completed' || st === 'failed') {
      return mapStatusToDeepResult(id, s)
    }
  }
  throw new Error('深度分析超时，请稍后刷新列表查看状态')
}

async function deepAnalyzeViaPolling(id: number): Promise<DeepAnalyzeResult> {
  await startDeepAnalyze(id)
  return pollDeepAnalyzeUntilDone(id)
}

async function deepAnalyzeViaSse(
  id: number,
  onProgress?: (data: Record<string, unknown>) => void
): Promise<DeepAnalyzeResult> {
  return new Promise((resolve, reject) => {
    ssePost<Record<string, unknown>, Record<string, unknown>>(
      `${VIRAL_BASE}/deep-analyze-stream`,
      { id },
      {
        onProgress: (data) => onProgress?.(data),
        onDone: async () => {
          try {
            const s = await getDeepAnalyzeStatus(id)
            resolve(mapStatusToDeepResult(id, s))
          } catch (e) {
            reject(e instanceof Error ? e : new Error(String(e)))
          }
        },
        onError: (err) => reject(err),
      },
      { idleTimeoutMs: 35 * 60 * 1000 }
    )
  })
}

export type DeepAnalyzeOptions = {
  /** SSE progress 事件（细粒度步骤 JSON） */
  onProgress?: (data: Record<string, unknown>) => void
}

/**
 * 优先使用 SSE 实时进度（/deep-analyze-stream），失败时降级为 POST /deep-analyze + 轮询。
 */
export async function deepAnalyze(id: number, options?: DeepAnalyzeOptions): Promise<DeepAnalyzeResult> {
  try {
    return await deepAnalyzeViaSse(id, options?.onProgress)
  } catch {
    try {
      const s = await getDeepAnalyzeStatus(id)
      const st = s.status ?? ''
      if (st === 'processing' || st === 'queued') {
        return await pollDeepAnalyzeUntilDone(id)
      }
    } catch {
      /* ignore */
    }
    return await deepAnalyzeViaPolling(id)
  }
}

export function batchDeepAnalyze(ids: number[]) {
  return request.post<DeepAnalyzeStartResult[]>(`${VIRAL_BASE}/deep-analyze/batch`, { ids })
}

/** 批量查询深度分析状态（最多 30 条） */
export function batchDeepAnalyzeStatus(ids: number[]) {
  return request.post<DeepAnalyzeStatusResult[]>(`${VIRAL_BASE}/deep-analyze/batch/status`, { ids })
}

/** 统一热点池（与 ViralLibrary 热点借势共用） */
export function fetchHotTopicPool(limit?: number) {
  return request.post<{ hotTopics?: Array<{ id: number; topic: string; heat?: number; source?: string }> }>(
    '/short-video/cross/hot-topic-pool',
    { limit: limit ?? 30 }
  )
}

/** 仅 ASR 口播 */
export function extractTranscript(id: number) {
  return request.post<string>(`${VIRAL_BASE}/extract-transcript`, { id })
}

/** 人设匹配 */
export function matchPersonas(viralVideoId: number) {
  return request.post<Record<string, unknown>[]>('/short-video/persona-fusion/match-personas', {
    viralVideoId,
  })
}

/** 爆款×人设 融合脚本 */
export function generateFusedScript(viralVideoId: number, personaId: number, remakeType?: string) {
  return request.post<Record<string, unknown>>('/short-video/persona-fusion/generate-fused-script', {
    viralVideoId,
    personaId,
    remakeType: remakeType ?? 'form_imitation',
  })
}

/** 热点×人设×产品 */
export function generateHotspotFused(hotTopicId: number, personaId: number, productId?: number) {
  return request.post<Record<string, unknown>>('/short-video/persona-fusion/generate-hotspot-fused', {
    hotTopicId,
    personaId,
    productId,
  })
}

/** 获取爆款视频评论（分页） */
export function getViralComments(viralVideoId: number, page = 0, rows = 20) {
  return request.post<{ total: number; list: ViralComment[]; pageNum: number; pageSize: number }>(
    `${VIRAL_BASE}/comments`,
    { viralVideoId, page, rows }
  )
}
