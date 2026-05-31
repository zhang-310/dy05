import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LiveScript } from './live-script'
import type { LiveProductVO, LiveSessionOverviewVO, LiveReadinessVO } from '@/types/live'
import { isRecord, normalizeArray, normalizePage, normalizeStringArray } from '@/utils/response-normalize'

// 重新导出 LiveScript 类型供其他模块使用
export type { LiveScript }

// ===== Session =====
export interface LiveSession {
  id: number; userId: number; accountId: number; personaId: number
  liveTitle: string; sessionCover: string; scriptStyle: string
  liveDescription: string; scheduledTime: string; scheduledEndTime: string
  startTime: string; endTime: string; liveUrl: string
  viewers: number; likes: number; status: number
  sessionType: string; liveFormat: string; createTime: string; updateTime: string
  totalGmv?: number | string; cumulativeGmv?: number | string; totalRevenue?: number | string
  productCount?: number; scriptCount?: number
}
export interface LiveSessionQuery {
  page?: number; rows?: number; keyword?: string; accountId?: number
  personaId?: number; status?: number; sessionType?: string; liveFormat?: string
  scheduledTimeFrom?: string; scheduledTimeTo?: string
}
export interface LiveSessionSave {
  id?: number; accountId?: number; personaId?: number; liveTitle: string
  scriptStyle?: string; liveDescription?: string; scheduledTime?: string
  scheduledEndTime?: string; sessionType?: string; liveFormat?: string; status?: number
}
export interface LiveSessionShortVideoExportResult {
  scriptId: number
  projectId: number
}
export interface LiveSessionCloneParams {
  id: number
  newTitle?: string
}

// ===== Script ===== (使用 live-script.ts 中的 LiveScript 类型)
export interface LiveScriptQuery {
  page?: number
  rows?: number
  sessionId?: number
  keyword?: string
  scriptType?: string
  status?: number
  executed?: number
}
export interface LiveScriptSave {
  id?: number; sessionId?: number; scriptTitle?: string; scriptContent?: string
  scriptType?: string; sortOrder?: number; duration?: number; durationLimitSec?: number
  openingLine?: string; closingLine?: string; productId?: number; status?: number
  style?: string; requirement?: string; sequenceNo?: number; executed?: number
}

// ===== Product =====
export interface LiveProduct extends LiveProductVO {
  sortOrder?: number
  status?: number
  scriptSource?: string
}
export interface LiveProductSave {
  id?: number; sessionId: number; productId: number; sortOrder?: number; status?: number
  productType?: string; productName?: string; saleQuantity?: number; position?: number
  scriptSource?: string; productScriptId?: number
}
export interface LiveProductBatchAddItem {
  productId: number; productName: string; productType: string
  imageUrl?: string; price?: number; productScriptId?: number
}

// ===== ScriptVersion =====
export interface LiveScriptVersion {
  id: number; scriptId: number; sessionId?: number
  versionNo: number | string; versionLabel?: string
  scriptContent?: string; content: string
  scriptType?: string; remark?: string; versionStatus?: string
  status?: number; isActive?: boolean
  effectivenessScore?: number; likedCount?: number; usageCount?: number
  lastUsedTime?: string; ownerId?: number; isRecommended?: number
  recommendReason?: string; recommendScore?: number
  basedOnVersionId?: number; changeSummary?: string
  createdBy?: number; createTime: string; updateTime?: string
}
export interface LiveScriptVersionSave {
  id?: number; scriptId: number
  content?: string; scriptContent?: string
  versionNo?: number | string; versionLabel?: string
  scriptType?: string; remark?: string; versionStatus?: string
  status?: number; effectivenessScore?: number; basedOnVersionId?: number
  recommendReason?: string; isRecommended?: number
}
export interface VersionDiffResult {
  oldVersionId?: number
  newVersionId?: number
  oldVersionNo?: number
  newVersionNo?: number
  oldContent?: string
  newContent?: string
  changedFields?: string[]
  diffHtml?: string
  similarity?: number
  recommendNew?: boolean
  recommendation?: string
  leftContent?: string
  rightContent?: string
  versionA?: { id: number; content: string; versionNo: string }
  versionB?: { id: number; content: string; versionNo: string }
}

// ===== SessionTemplate =====
export interface LiveSessionTemplate {
  id: number; templateName: string; scriptStyle: string
  sessionType: string; liveFormat: string; description: string
  status: number; createTime: string
}
export interface LiveSessionTemplateSave {
  id?: number; templateName: string; scriptStyle?: string
  sessionType?: string; liveFormat?: string; description?: string; status?: number
}

// ===== Monitor =====
export interface LiveMonitor {
  id: number; sessionId: number; viewerCount: number; likeCount: number
  commentCount: number; shareCount: number; gmv: number
  recordTime: string; createTime: string
}

// ===== Effectiveness =====
export interface LiveEffectivenessConfig {
  id: number; configName: string; weights: string; isDefault: number; createTime: string
}
export interface LiveEffectivenessConfigSave {
  id?: number; configName: string; weights?: string; isDefault?: number
}

// ===== GenerationPreset =====
export interface LiveGenerationPreset {
  id: number; presetName: string; style: string; tone: string
  modelId: number; useKbRef: boolean; isDefault: boolean; createTime: string
  genStyle?: string; ipType?: string; materialType?: string
  scriptModule?: string; retentionStrategy?: string; interactionLevel?: string
}
export interface LiveGenerationPresetSave {
  id?: number; presetName: string; style?: string; tone?: string
  modelId?: number; useKbRef?: boolean
  genStyle?: string; ipType?: string; materialType?: string
  scriptModule?: string; retentionStrategy?: string; interactionLevel?: string
}

// ===== GenerationTask =====
export interface LiveGenerationTask {
  id: number; sessionId: number; status: string; progress: number
  totalSlots: number; completedSlots: number; errorMessage: string
  createTime: string; updateTime: string
}

// ===== CompetitiveInsight =====
export interface LiveCompetitiveInsight {
  id: number; ownerId: number; sessionId: number; competitorLabel: string
  productPrice: number; marketSharePercent: number; gmvEstimate: number
  winLossNotes: string; createTime: string
}
export interface LiveCompetitiveInsightSave {
  id?: number; sessionId?: number; competitorLabel: string
  productPrice?: number; marketSharePercent?: number
  gmvEstimate?: number; winLossNotes?: string
}

// ===== CompetitorScript =====
export interface LiveCompetitorScript {
  id: number; ownerId: number; title: string; competitorName: string
  platform: string; scriptContent: string; createTime: string
}
export interface LiveCompetitorScriptSave {
  id?: number; title: string; competitorName?: string
  platform?: string; scriptContent: string
}

// ===== ScriptApproval =====
export interface LiveScriptApproval {
  id: number; sessionId: number; scriptId: number; status: string
  submittedBy: number; reviewedBy: number; comment: string
  createTime: string
}
export interface LiveScriptApprovalSave {
  id?: number; sessionId?: number; scriptId?: number
  comment?: string
}

// ===== ScriptComment =====
export interface LiveScriptComment {
  id: number; scriptId: number; sessionId: number; content: string
  resolved: boolean; createdBy: number; createTime: string
}
export interface LiveScriptCommentSave {
  id?: number; scriptId: number; sessionId?: number; content: string
}

// ===== Rhythm =====
export interface LiveRhythm {
  sessionId: number; rhythm: Record<string, unknown>; createTime: string
}

function normalizeLiveScriptVersion(v: Record<string, unknown>): LiveScriptVersion {
  const versionStatus = typeof v.versionStatus === 'string' ? v.versionStatus : undefined
  const legacyStatus = versionStatus === 'active' ? 1 : versionStatus === 'archived' ? 0 : undefined
  const versionNo = (v.versionNo ?? v.versionNumber ?? '') as number | string
  const scriptContent = typeof v.scriptContent === 'string'
    ? v.scriptContent
    : typeof v.content === 'string'
      ? v.content
      : ''

  return {
    ...(v as unknown as LiveScriptVersion),
    id: Number(v.id ?? 0),
    scriptId: Number(v.scriptId ?? 0),
    sessionId: v.sessionId == null ? undefined : Number(v.sessionId),
    versionNo,
    scriptContent,
    content: scriptContent,
    versionStatus,
    status: typeof v.status === 'number' ? v.status : legacyStatus,
    isActive: versionStatus === 'active' || v.isActive === true || v.isCurrent === 1,
    createTime: String(v.createTime ?? ''),
    updateTime: v.updateTime == null ? undefined : String(v.updateTime),
  }
}

function normalizeLiveScriptVersions(list: unknown): LiveScriptVersion[] {
  const rows = Array.isArray(list)
    ? list
    : list && typeof list === 'object'
      ? (['list', 'records', 'items', 'data']
          .map((key) => (list as Record<string, unknown>)[key])
          .find(Array.isArray) as unknown[] | undefined) ?? []
      : []
  return rows.length > 0
    ? rows.map((v) => normalizeLiveScriptVersion((v ?? {}) as Record<string, unknown>))
    : []
}

function boolish(value: unknown): boolean | undefined {
  if (value === true || value === 1 || value === '1' || value === 'true') return true
  if (value === false || value === 0 || value === '0' || value === 'false') return false
  return undefined
}

function numberOrUndefined(value: unknown): number | undefined {
  if (value == null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

function metricValue(value: unknown): number | string | undefined {
  if (typeof value === 'number' || typeof value === 'string') return value
  return undefined
}

function normalizeLiveScriptRow(raw: unknown): LiveScript {
  const row = isRecord(raw) ? raw : {}
  const sequenceNo = numberOrUndefined(row.sequenceNo ?? row.sortOrder ?? row.position)
  const durationLimitSec = numberOrUndefined(row.durationLimitSec ?? row.duration ?? row.estimatedDurationSeconds)
  const scriptType = row.scriptType == null ? undefined : String(row.scriptType)
  const scriptTitle = row.scriptTitle == null
    ? (row.requirement == null ? (scriptType ? `${scriptType} #${sequenceNo ?? ''}`.trim() : '话术') : String(row.requirement))
    : String(row.scriptTitle)

  return {
    ...(row as unknown as LiveScript),
    id: Number(row.id ?? row.sessionId ?? row.session_id ?? 0),
    sessionId: Number(row.sessionId ?? 0),
    scriptTitle,
    scriptContent: String(row.scriptContent ?? row.content ?? ''),
    scriptType,
    sequenceNo,
    sortOrder: sequenceNo,
    durationLimitSec,
    duration: durationLimitSec,
    executed: numberOrUndefined(row.executed) ?? 0,
    aiGenerated: boolish(row.aiGenerated) ?? Boolean(row.aiGenerated),
    violationChecked: boolish(row.violationChecked) ?? Boolean(row.violationChecked),
    createTime: String(row.createTime ?? ''),
    updateTime: row.updateTime == null ? undefined : String(row.updateTime),
  } as LiveScript
}

function normalizeLiveScriptSearch(raw: unknown, params: LiveScriptQuery): PageResult<LiveScript> {
  return normalizePage<unknown, LiveScript>(raw, normalizeLiveScriptRow, params.page ?? 0, params.rows ?? 20)
}

function normalizeLiveSessionRow(raw: unknown): LiveSession {
  const row = isRecord(raw) ? raw : {}
  return {
    ...(row as unknown as LiveSession),
    id: Number(row.id ?? row.sessionId ?? row.session_id ?? 0),
    userId: Number(row.userId ?? row.user_id ?? row.ownerId ?? row.owner_id ?? 0),
    accountId: Number(row.accountId ?? row.account_id ?? 0),
    personaId: Number(row.personaId ?? row.persona_id ?? 0),
    liveTitle: String(row.liveTitle ?? row.live_title ?? row.title ?? ''),
    sessionCover: String(row.sessionCover ?? row.session_cover ?? ''),
    scriptStyle: String(row.scriptStyle ?? row.script_style ?? ''),
    liveDescription: String(row.liveDescription ?? row.live_description ?? row.description ?? ''),
    scheduledTime: String(row.scheduledTime ?? row.scheduled_time ?? ''),
    scheduledEndTime: String(row.scheduledEndTime ?? row.scheduled_end_time ?? ''),
    startTime: String(row.startTime ?? row.start_time ?? ''),
    endTime: String(row.endTime ?? row.end_time ?? ''),
    liveUrl: String(row.liveUrl ?? row.live_url ?? ''),
    viewers: numberOrUndefined(row.viewers ?? row.totalViewers ?? row.total_viewers) ?? 0,
    likes: numberOrUndefined(row.likes) ?? 0,
    status: numberOrUndefined(row.status) ?? 0,
    sessionType: String(row.sessionType ?? row.session_type ?? ''),
    liveFormat: String(row.liveFormat ?? row.live_format ?? ''),
    createTime: String(row.createTime ?? row.create_time ?? ''),
    updateTime: String(row.updateTime ?? row.update_time ?? ''),
    totalGmv: metricValue(row.totalGmv ?? row.total_gmv ?? row.gmv),
    cumulativeGmv: metricValue(row.cumulativeGmv ?? row.cumulative_gmv),
    totalRevenue: metricValue(row.totalRevenue ?? row.total_revenue),
  }
}

function normalizeLiveProductRow(raw: unknown): LiveProduct {
  const row = isRecord(raw) ? raw : {}
  return {
    ...(row as unknown as LiveProduct),
    id: Number(row.id ?? 0),
    sessionId: Number(row.sessionId ?? 0),
    productId: Number(row.productId ?? row.id ?? 0),
    productName: row.productName == null ? undefined : String(row.productName),
    saleQuantity: numberOrUndefined(row.saleQuantity ?? row.sales ?? row.quantity),
    revenue: numberOrUndefined(row.revenue ?? row.totalRevenue ?? row.gmv),
    position: numberOrUndefined(row.position ?? row.sortOrder),
    productType: row.productType == null ? undefined : String(row.productType),
    scriptSource: row.scriptSource == null ? undefined : String(row.scriptSource),
    productScriptId: numberOrUndefined(row.productScriptId ?? row.scriptId),
    createTime: String(row.createTime ?? ''),
  }
}

function normalizeLiveProductSearch(raw: unknown, params: Record<string, unknown>): PageResult<LiveProduct> {
  return normalizePage<unknown, LiveProduct>(raw, normalizeLiveProductRow, Number(params.page ?? 0), Number(params.rows ?? 20))
}

function normalizeLiveSessionSearch(raw: unknown, params: LiveSessionQuery): PageResult<LiveSession> {
  return normalizePage<unknown, LiveSession>(raw, normalizeLiveSessionRow, params.page ?? 0, params.rows ?? 20)
}

function normalizeRecordArray(raw: unknown): Record<string, unknown>[] {
  return normalizeArray<unknown>(raw).filter(isRecord)
}

function normalizeLiveScriptSavePayload(p: Partial<LiveScriptSave>): Record<string, unknown> {
  return {
    ...(p.id != null ? { id: p.id } : {}),
    ...(p.sessionId != null ? { sessionId: p.sessionId } : {}),
    ...(p.scriptContent != null ? { scriptContent: p.scriptContent } : {}),
    ...(p.scriptType != null ? { scriptType: p.scriptType } : {}),
    ...(p.style != null ? { style: p.style } : {}),
    ...(p.requirement != null ? { requirement: p.requirement } : {}),
    ...(p.productId != null ? { productId: p.productId } : {}),
    ...(p.sequenceNo != null ? { sequenceNo: p.sequenceNo } : {}),
    ...(p.sortOrder != null && p.sequenceNo == null ? { sequenceNo: p.sortOrder } : {}),
    ...(p.durationLimitSec != null ? { durationLimitSec: p.durationLimitSec } : {}),
    ...(p.duration != null && p.durationLimitSec == null ? { durationLimitSec: p.duration } : {}),
    ...(p.executed != null ? { executed: p.executed } : {}),
  }
}

function normalizeLiveSessionClonePayload(p: number | LiveSessionCloneParams): Record<string, unknown> {
  const id = typeof p === 'number' ? p : p.id
  return {
    id,
    sessionId: id,
    sourceSessionId: id,
    ...(typeof p === 'number' || p.newTitle == null ? {} : { newTitle: p.newTitle }),
  }
}

// ===== DataSync =====
export interface LiveSessionData {
  sessionId: number; viewerPeak: number; gmv: number; orderCount: number
  conversionRate: number; avgOrderValue: number; createTime: string
}
export interface LiveProductData {
  id: number; sessionId: number; productId: number; productName: string
  exposures: number; clicks: number; orders: number; gmv: number
}

export const liveApi = {
  // ===== Session =====
  sessionSearch: (p: LiveSessionQuery) => request.post<unknown>('/live/session/search', p).then(raw => normalizeLiveSessionSearch(raw, p)),
  sessionGet: (id: number) => request.post<LiveSession>('/live/session/get', { id }),
  sessionSave: (p: Partial<LiveSessionSave>) => request.post<number>('/live/session/save', p),
  sessionDelete: (id: number) => request.post<void>('/live/session/delete', { id }),
  sessionStart: (id: number) => request.post<void>('/live/session/status', { id, status: 1 }),
  sessionEnd: (id: number) => request.post<void>('/live/session/status', { id, status: 2 }),
  sessionClone: (p: number | LiveSessionCloneParams) =>
    request.post<number>('/live/session/clone', normalizeLiveSessionClonePayload(p)),
  sessionTrend: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/session/trend', p).then(normalizeRecordArray),
  sessionExportToShortVideo: (sessionId: number, style?: string) =>
    request.post<LiveSessionShortVideoExportResult>('/live/session/export-to-short-video', { sessionId, style }),
  sessionOverview: (id: number) => request.post<LiveSessionOverviewVO>('/live/session/overview', { id }),
  sessionReadiness: (id: number) => request.post<LiveReadinessVO>('/live/session/readiness', { id }),
  sessionMultiMetrics: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/session/multi-metrics', p),

  // ===== SessionTemplate =====
  templateSearch: (p: Record<string, unknown>) => request.post<PageResult<LiveSessionTemplate>>('/live/session-template/search', p),
  templateGet: (id: number) => request.post<LiveSessionTemplate>('/live/session-template/get', { id }),
  templateSave: (p: Partial<LiveSessionTemplateSave>) => request.post<number>('/live/session-template/save', p),
  templateDelete: (id: number) => request.post<void>('/live/session-template/delete', { id }),
  templateSaveAsFromSession: (p: Record<string, unknown>) => request.post<number>('/live/session-template/save-as', p),

  // ===== Script =====
  scriptSearch: (p: LiveScriptQuery) => request.post<unknown>('/live/script/search', p).then(raw => normalizeLiveScriptSearch(raw, p)),
  scriptGet: (id: number) => request.post<LiveScript>('/live/script/get', { id }),
  scriptSave: (p: Partial<LiveScriptSave>) => request.post<number>('/live/script/save', normalizeLiveScriptSavePayload(p)),
  scriptDelete: (id: number) => request.post<void>('/live/script/delete', { id }),
  scriptBySession: (sessionId: number) =>
    request.post<unknown>('/live/script/by-session', { sessionId }).then(raw => normalizeArray<unknown>(raw).map(normalizeLiveScriptRow)),
  scriptEffectiveness: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/script/effectiveness', p).then(raw => normalizeArray<unknown>(raw).map(normalizeLiveScriptRow)),
  scriptUpdateExecuted: (p: Record<string, unknown>) => request.post<void>('/live/script/executed', p),
  scriptSaveToLibrary: (p: Record<string, unknown>) => request.post<number>('/live/script/save-to-library', p),
  scriptSaveBatchToLibrary: (p: Record<string, unknown>) => request.post<number>('/live/script/save-batch-to-library', p),
  scriptExport: (p: Record<string, unknown>) => request.post<string>('/live/script/export', p),

  // ===== Script Custom Template =====
  scriptTemplateSaveFromSession: (p: Record<string, unknown>) => request.post<number>('/live/script-template/save-from-session', p),

  // ===== Product =====
  productSearch: (p: Record<string, unknown>) => request.post<unknown>('/live/product/search', p).then(raw => normalizeLiveProductSearch(raw, p)),
  productGet: (id: number) => request.post<LiveProduct>('/live/product/get', { id }),
  productSave: (p: Partial<LiveProductSave>) => request.post<number>('/live/product/save', p),
  productDelete: (id: number) => request.post<void>('/live/product/delete', { id }),
  productBySession: (sessionId: number) =>
    request.post<unknown>('/live/product/by-session', { sessionId }).then(raw => normalizeArray<unknown>(raw).map(normalizeLiveProductRow)),
  productBatchSort: (sessionId: number, productIds: number[]) => request.post<void>('/live/product/batch-sort', { sessionId, productIds }),
  productBatchAdd: (sessionId: number, items: LiveProductBatchAddItem[]) => request.post<number>('/live/product/batch-add', { sessionId, items }),
  productAiSortSuggest: (p: { sessionId: number; productIds: number[] }) => request.post<Record<string, unknown>>('/live/ai/sort-suggest', p),

  // ===== ScriptVersion =====
  versionList: (scriptId: number) =>
    request.post<unknown[]>('/live/script/version/getByScriptId', scriptId).then(normalizeLiveScriptVersions),
  versionSave: (p: Partial<LiveScriptVersionSave>) => request.post<number>('/live/script/version/save', {
    scriptId: p.scriptId,
    versionNo: typeof p.versionNo === 'string' ? Number(p.versionNo) || undefined : p.versionNo,
    versionLabel: p.versionLabel,
    scriptContent: p.scriptContent ?? p.content,
    scriptType: p.scriptType,
    remark: p.remark,
    versionStatus: p.versionStatus ?? (p.status === 1 ? 'active' : undefined) ?? 'draft',
    effectivenessScore: p.effectivenessScore,
    basedOnVersionId: p.basedOnVersionId,
    recommendReason: p.recommendReason,
    isRecommended: p.isRecommended,
  }),
  versionActivate: (id: number) => request.post<void>('/live/script/version/updateStatus', { versionId: id, versionStatus: 'active' }),
  versionDelete: (id: number) => request.post<void>('/live/script/version/delete', id),
  versionDiff: (p: { versionId1: number; versionId2: number }) =>
    request.post<VersionDiffResult>('/live/script/version/diff', { oldVersionId: p.versionId1, newVersionId: p.versionId2 })
      .then((diff) => ({
        ...diff,
        leftContent: diff.leftContent ?? diff.oldContent,
        rightContent: diff.rightContent ?? diff.newContent,
        versionA: diff.versionA ?? (diff.oldVersionId ? {
          id: diff.oldVersionId,
          content: diff.oldContent ?? '',
          versionNo: String(diff.oldVersionNo ?? ''),
        } : undefined),
        versionB: diff.versionB ?? (diff.newVersionId ? {
          id: diff.newVersionId,
          content: diff.newContent ?? '',
          versionNo: String(diff.newVersionNo ?? ''),
        } : undefined),
      })),

  // ===== Monitor =====
  monitorSearch: (p: Record<string, unknown>) => request.post<PageResult<LiveMonitor>>('/live/monitor/search', p),
  monitorSave: (p: Record<string, unknown>) => request.post<number>('/live/monitor/save', p),
  monitorBySession: (sessionId: number) =>
    request.post<unknown>('/live/monitor/by-session', { sessionId }).then(raw => normalizeArray<LiveMonitor>(raw)),
  monitorSnapshot: (p: Record<string, unknown>) => request.post<void>('/live/monitor/snapshot', p),
  monitorPush: (p: Record<string, unknown>) => request.post<void>('/live/monitor/push', p),

  // ===== Realtime Panel =====
  realtimePanelInit: (sessionId: number) => request.post<Record<string, unknown>>('/live/realtime-panel/init', { sessionId }),
  realtimePanelNextSlot: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/realtime-panel/next-slot', p),
  realtimePanelSaveTimer: (p: Record<string, unknown>) => request.post<void>('/live/realtime-panel/save-timer', p),
  realtimePanelSaveData: (p: Record<string, unknown>) => request.post<void>('/live/realtime-panel/save-data', p),

  // ===== Effectiveness =====
  effectivenessCalculate: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/effectiveness/calculate', p),
  effectivenessSessionRanking: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/effectiveness/session-ranking', p).then(normalizeRecordArray),
  effectivenessCompare: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/effectiveness/compare', p),
  effectivenessRanking: (p: Record<string, unknown>) => request.post<PageResult<Record<string, unknown>>>('/live/effectiveness/ranking', p),
  effectivenessTopScripts: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/effectiveness/top-scripts', p).then(normalizeRecordArray),
  effectivenessRecommended: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/effectiveness/recommended-scripts', p).then(normalizeRecordArray),
  effectivenessEmerged: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/effectiveness/emerged-scripts', p).then(normalizeRecordArray),
  effectivenessScriptDetail: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/effectiveness/script-effectiveness', p),

  // ===== Effectiveness Config =====
  effectivenessConfigList: () => request.post<unknown>('/live/effectiveness-config/list', {}).then(raw => normalizeArray<LiveEffectivenessConfig>(raw)),
  effectivenessConfigSave: (p: Partial<LiveEffectivenessConfigSave>) => request.post<LiveEffectivenessConfig>('/live/effectiveness-config/save', p),
  effectivenessConfigDefault: () => request.post<LiveEffectivenessConfig>('/live/effectiveness-config/default', {}),
  effectivenessConfigSetDefault: (id: number) => request.post<void>('/live/effectiveness-config/set-default', { id }),
  effectivenessConfigDelete: (id: number) => request.post<void>('/live/effectiveness-config/delete', { id }),

  // ===== Generation Preset =====
  presetList: () => request.post<unknown>('/live/generation-preset/list', {}).then(raw => normalizeArray<LiveGenerationPreset>(raw)),
  presetSave: (p: Partial<LiveGenerationPresetSave>) => request.post<LiveGenerationPreset>('/live/generation-preset/save', p),
  presetDelete: (id: number) => request.post<void>('/live/generation-preset/delete', { id }),
  presetGetDefault: () => request.post<LiveGenerationPreset>('/live/generation-preset/getDefault', {}),
  presetSetDefault: (id: number) => request.post<void>('/live/generation-preset/set-default', { id }),

  // ===== Generation Task =====
  generationTaskLatest: (sessionId: number) => request.post<LiveGenerationTask>('/live/generation-task/latest', { sessionId }),
  generationTaskCreate: (p: Record<string, unknown>) => request.post<LiveGenerationTask>('/live/generation-task/create', p),
  generationTaskUpdateProgress: (p: Record<string, unknown>) => request.post<void>('/live/generation-task/update-progress', p),

  // ===== AI Generation =====
  aiGenerateOpening: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-opening', p),
  aiGenerateProduct: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-product', p),
  aiGenerateTransition: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-transition', p),
  aiGenerateClosing: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-closing', p),
  aiGenerateSlot: (p: Record<string, unknown>) => request.post<string>('/live/ai/generate-slot', p),
  aiGenerateFull: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/ai/generate-full', p).then(normalizeRecordArray),
  aiGenerateFullAsync: (p: Record<string, unknown>) => request.post<LiveGenerationTask>('/live/ai/generate-full-async', p),
  aiGenerateFullInProgress: (sessionId?: number) => request.post<Record<string, unknown>>('/live/ai/generate-full-in-progress', sessionId ? { sessionId } : {}),
  aiGenerationTaskActive: (sessionId?: number) => request.post<LiveGenerationTask>('/live/ai/generation-task/active', sessionId ? { sessionId } : {}),
  aiGenerateParallel: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-parallel', p),
  aiGenerateSkeleton: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/ai/generate-skeleton', p).then(normalizeRecordArray),
  aiGenerateSkeletonSse: '/live/ai/generate-skeleton-sse', // SSE endpoint path only
  aiGenerateFullPipelinedSse: '/live/ai/generate-full-pipelined-sse', // SSE endpoint path only
  aiGenerateFullSse: '/live/ai/generate-full-sse', // SSE endpoint path only
  aiGenerateProductScript: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-product-script', p),
  aiGenerateEmotional: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/generate-emotional', p),
  aiSortSuggest: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/sort-suggest', p),

  // ===== AI Quality =====
  aiCheckViolation: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/check-violation', p),
  aiCheckViolationEnhanced: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/check-violation-enhanced', p),
  aiSaveToCopyIfPassed: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/save-to-copy-if-passed', p),

  // ===== AI Refine =====
  aiRefineScript: (p: Record<string, unknown>) => request.post<string>('/live/ai/refine-script', p),
  aiRefineSegment: (p: Record<string, unknown>) => request.post<string>('/live/ai/refine-segment', p),
  aiChatForScript: (p: Record<string, unknown>) => request.post<string>('/live/ai/chat-for-script', p),
  aiBatchChatForScript: (p: Record<string, unknown>) => request.post<Record<string, string>>('/live/ai/batch-chat-for-script', p),
  aiSuggestImprovement: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/suggest-improvement', p),
  aiCheckSimilarity: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/check-similarity', p),

  // ===== AI Recommend =====
  aiRecommendScripts: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/ai/recommend-scripts', p).then(normalizeRecordArray),
  aiChat2hStrategy: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/ai/chat-2h-strategy', p),

  // ===== Danmaku =====
  danmakuAnalyze: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/danmaku/analyze', p),
  danmakuSuggest: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/danmaku/suggest', p).then(normalizeStringArray),

  // ===== Script Approval (new path) =====
  scriptApprovalSubmit: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/script-approval/submit', p),
  scriptApprovalSubmitBySession: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/script-approval/submit-by-session', p),
  scriptApprovalReview: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/script-approval/review', p),
  scriptApprovalRevoke: (p: Record<string, unknown>) => request.post<void>('/live/script-approval/revoke', p),
  scriptApprovalSearch: (p: Record<string, unknown>) => request.post<PageResult<LiveScriptApproval>>('/live/script-approval/search', p),
  scriptApprovalHistory: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/script-approval/history', p).then(raw => normalizeArray<LiveScriptApproval>(raw)),

  // ===== Approval (session-level) =====
  approvalSubmit: (p: Record<string, unknown>) => request.post<void>('/live/approval/submit', p),
  approvalApprove: (p: Record<string, unknown>) => request.post<void>('/live/approval/approve', p),
  approvalReject: (p: Record<string, unknown>) => request.post<void>('/live/approval/reject', p),
  approvalHistory: (sessionId: number) =>
    request.post<unknown>('/live/approval/history', { sessionId }).then(normalizeRecordArray),
  approvalPending: () =>
    request.post<unknown>('/live/approval/pending', {}).then(raw => normalizeArray<unknown>(raw).map(normalizeLiveSessionRow)),

  // ===== Script Comment =====
  scriptCommentByScript: (scriptId: number) =>
    request.post<unknown>('/live/script-comment/by-script', { scriptId }).then(raw => normalizeArray<LiveScriptComment>(raw)),
  scriptCommentBySession: (sessionId: number) =>
    request.post<unknown>('/live/script-comment/by-session', { sessionId }).then(raw => normalizeArray<LiveScriptComment>(raw)),
  scriptCommentSave: (p: Partial<LiveScriptCommentSave>) => request.post<LiveScriptComment>('/live/script-comment/save', p),
  scriptCommentResolve: (id: number) => request.post<void>('/live/script-comment/resolve', { id }),
  scriptCommentDelete: (id: number) => request.post<number>('/live/script-comment/delete', { id }),
  scriptCommentUnresolvedCount: (sessionId: number) => request.post<number>('/live/script-comment/unresolved-count', { sessionId }),
  scriptCommentUnresolvedByScript: (sessionId: number) => request.post<Record<string, number>>('/live/script-comment/unresolved-by-script', { sessionId }),

  // ===== Competitive Insight =====
  competitiveInsightSearch: (p: Record<string, unknown>) => request.post<PageResult<LiveCompetitiveInsight>>('/live/competitive-insight/search', p),
  competitiveInsightGet: (id: number) => request.post<LiveCompetitiveInsight>('/live/competitive-insight/get', { id }),
  competitiveInsightSave: (p: Partial<LiveCompetitiveInsightSave>) => request.post<number>('/live/competitive-insight/save', p),
  competitiveInsightDelete: (id: number) => request.post<void>('/live/competitive-insight/delete', { id }),

  // ===== Competitor Script =====
  competitorScriptSearch: (p: Record<string, unknown>) => request.post<PageResult<LiveCompetitorScript>>('/live/competitor-script/search', p),
  competitorScriptGet: (id: number) => request.post<LiveCompetitorScript>('/live/competitor-script/get', { id }),
  competitorScriptSave: (p: Partial<LiveCompetitorScriptSave>) => request.post<number>('/live/competitor-script/save', p),
  competitorScriptDelete: (id: number) => request.post<void>('/live/competitor-script/delete', { id }),

  // ===== Competitor Monitor Bridge =====
  competitorMonitorList: () =>
    request.post<unknown>('/live/competitor-monitor/list', {}).then(normalizeRecordArray),

  // ===== Collaboration =====
  collaborationJoin: (p: Record<string, unknown>) => request.post<void>('/live/collaboration/join', p),
  collaborationLeave: (p: Record<string, unknown>) => request.post<void>('/live/collaboration/leave', p),
  collaborationViewers: (sessionId: number) =>
    request.post<unknown>('/live/collaboration/viewers', { sessionId }).then(normalizeRecordArray),

  // ===== Data Sync =====
  dataSession: (sessionId: number) => request.post<LiveSessionData>('/live/data/session', { sessionId }),
  dataSessionWithCompare: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/data/session/with-compare', p),
  dataSessionSave: (p: Record<string, unknown>) => request.post<LiveSessionData>('/live/data/session/save', p),
  dataSessionSync: (sessionId: number) => request.post<LiveSessionData>('/live/data/session/sync', { sessionId }),
  dataSessionSyncFromDouyin: (sessionId: number) => request.post<LiveSessionData>('/live/data/session/sync-from-douyin', { sessionId }),
  dataProduct: (sessionId: number) =>
    request.post<unknown>('/live/data/product', { sessionId }).then(raw => normalizeArray<LiveProductData>(raw)),
  dataHistory: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/data/history', p).then(normalizeRecordArray),
  dataProductSave: (p: Record<string, unknown>) => request.post<LiveProductData>('/live/data/product/save', p),

  // ===== Platform Rule =====
  platformList: () => request.post<unknown>('/live/platform/list', {}).then(normalizeRecordArray),
  platformViolationCheck: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/platform/violation-check', p).then(normalizeRecordArray),
  platformPromptTemplate: (platformCode: string) => request.post<string>('/live/platform/prompt-template', { platformCode }),

  // ===== Rhythm =====
  rhythmSave: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/rhythm/save-rhythm', p),
  rhythmOptimize: (sessionId: number) => request.post<Record<string, unknown>>('/live/rhythm/optimize', { sessionId }),

  // ===== Version Diff (extended) =====
  getVersionsByScriptId: (scriptId: number) =>
    request.post<unknown[]>('/live/script/version/getByScriptId', scriptId).then(normalizeLiveScriptVersions),

  // ===== Script Pipeline =====
  pipelineStart: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/pipeline/start', p),
  pipelineStatus: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/pipeline/status', p),
  pipelineCancel: (p: Record<string, unknown>) => request.post<void>('/live/pipeline/cancel', p),

  // ===== Script Navigation =====
  navigationNextSlot: (sessionId: number) => request.post<Record<string, unknown>>(`/live/script-navigation/next/${sessionId}`, {}),
  navigationSkipToSlot: (sessionId: number, p: Record<string, unknown>) => request.post<Record<string, unknown>>(`/live/script-navigation/skip/${sessionId}`, p),

  // ===== AB Test Analysis =====
  abTestRecord: (p: Record<string, unknown>) => request.post<void>('/live/ab-analysis/record', p),
  abTestRecommend: () => request.post<Record<string, unknown>>('/live/ab-analysis/recommend', {}),
  abTestSummary: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/ab-analysis/summary', p).then(normalizeRecordArray),

  // ===== Analysis =====
  analysisGenerate: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/analysis/generate', p),
  analysisGet: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/analysis/get', p),
  analysisReview: (p: Record<string, unknown>) => request.post<Record<string, unknown>>('/live/analysis/review', p),

  // ===== Content Material =====
  materialRandom: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/material/random', p).then(normalizeRecordArray),
  materialByPersona: (p: Record<string, unknown>) =>
    request.post<unknown>('/live/material/by-persona', p).then(normalizeRecordArray),
  materialCategories: () => request.post<Record<string, string[]>>('/live/material/categories', {}),
  materialPrompt: (p: Record<string, string>) => request.post<string>('/live/material/prompt', p),
  materialPerformancePrompt: (p: Record<string, string>) => request.post<string>('/live/material/performance-prompt', p),
  materialRiskMatch: (p: Record<string, string>) =>
    request.post<unknown>('/live/material/risk-match', p).then(normalizeStringArray),

  // ===== Attribution =====
  scriptAttributionList: (sessionId: number) =>
    request.post<unknown>('/live/attribution/script-list', { sessionId }).then(normalizeRecordArray),
  sessionAnalysis: (sessionId: number) => request.post<Record<string, unknown>>('/live/session/analysis', { sessionId }),
}

// ===== dy01-style named exports (for migrated components) =====
export type FullGenerateOptions = Record<string, unknown>
export interface LiveRagRef {
  docId?: number
  chunkId?: number
  title?: string
  contentPreview?: string
  score?: number
  source?: string
  labels?: string[]
  docTitle?: string
  snippet?: string
}

export interface LiveOfficialReference {
  kbName?: string
  refType?: string
  docId?: number
  chunkId?: number
  title?: string
  contentPreview?: string
  score?: number
}

export const getSession = (id: number) => liveApi.sessionGet(id)
export const getProductsBySession = (p: Record<string, unknown>) => liveApi.productSearch(p)
export const getScriptsBySession = (p: Record<string, unknown>) => liveApi.scriptSearch(p as LiveScriptQuery)
export const checkGenerateFullInProgress = (sessionId: number) => liveApi.aiGenerateFullInProgress(sessionId)
export const deleteLiveProduct = (id: number) => liveApi.productDelete(id)
export const deleteLiveScript = (id: number) => liveApi.scriptDelete(id)
export const batchSortProducts = (p: Record<string, unknown>) => request.post('/live/product/batch-sort', p)
export const batchSortScripts = (p: Record<string, unknown>) => request.post('/live/script/batch-sort', p)
export const batchChatForScript = (p: Record<string, unknown>) => liveApi.aiBatchChatForScript(p)
export const chatForScriptStream = (p: Record<string, unknown>) => liveApi.aiChatForScript(p)
export const clearAllScripts = (sessionId: number) => request.post('/live/script/clear-by-session', { sessionId })
export const exportScripts = (sessionId: number) => request.post('/live/script/export', { sessionId })
export const generateFull = (p: Record<string, unknown>) => liveApi.aiGenerateFull(p)
export const generateFullStream = (p: Record<string, unknown>) => request.post('/live/ai/generate-full-sse', p)
export const generateSkeletonStream = (p: Record<string, unknown>) => request.post('/live/ai/generate-skeleton-sse', p)
export const generateForSlot = (p: Record<string, unknown>) => liveApi.aiGenerateSlot(p)
export const generateForSlotStream = (p: Record<string, unknown>) => liveApi.aiGenerateSlot(p)
export const generateProduct = (p: Record<string, unknown>) => liveApi.aiGenerateProduct(p)
export const generateOpening = (p: Record<string, unknown>) => liveApi.aiGenerateOpening(p)
export const generateEmotional = (p: Record<string, unknown>) => liveApi.aiGenerateEmotional(p)
export const generateAnalysis = (p: Record<string, unknown>) => liveApi.analysisGenerate(p)
export const getAnalysis = (p: Record<string, unknown>) => liveApi.analysisGet(p)
export const getScriptEffectiveness = (p: Record<string, unknown>) => liveApi.scriptEffectiveness(p)
export const refineScriptStream = (p: Record<string, unknown>) => liveApi.aiRefineScript(p)
export const refineSegment = (p: Record<string, unknown>) => liveApi.aiRefineSegment(p)
export const checkSimilarity = (p: Record<string, unknown>) => liveApi.aiCheckSimilarity(p)
export const checkViolation = (p: Record<string, unknown>) => liveApi.aiCheckViolation(p)
export const saveBatchToLibrary = (p: Record<string, unknown>) => liveApi.scriptSaveBatchToLibrary(p)
export const saveScriptToLibrary = (p: Record<string, unknown>) => liveApi.scriptSaveToLibrary(p)
export const saveToCopyIfPassed = (p: Record<string, unknown>) => liveApi.aiSaveToCopyIfPassed(p)
export const updateScriptExecuted = (id: number, executed = 1) => request.post('/live/script/executed', { id, executed })
export const initScriptSlots = (sessionId: number) => request.post('/live/script/init-slots', { sessionId })
export const rebuildScriptSlots = (sessionId: number) => request.post('/live/script/rebuild-slots', { sessionId })
export const saveLiveScript = (p: Partial<LiveScriptSave>) => liveApi.scriptSave(p)
export const saveLiveProduct = (p: Partial<LiveProductSave>) => liveApi.productSave(p)
export type LiveSessionVO = LiveSession
