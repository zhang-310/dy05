import request from '@/utils/request'
import type { LiveScriptSearchVO, LiveScriptSaveVO, LiveScriptVO } from '@/types/live'
import { isRecord, normalizeArray, normalizePage } from '@/utils/response-normalize'

/** 直播话术完整类型（保留向后兼容） */
export type LiveScript = LiveScriptVO & { [key: string]: unknown }

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

function normalizeLiveScriptRow(raw: unknown): LiveScript {
  const row = isRecord(raw) ? raw : {}
  const sequenceNo = numberOrUndefined(row.sequenceNo ?? row.sortOrder ?? row.position)
  const durationLimitSec = numberOrUndefined(row.durationLimitSec ?? row.duration ?? row.estimatedDurationSeconds)

  return {
    ...(row as unknown as LiveScript),
    id: Number(row.id ?? 0),
    sessionId: Number(row.sessionId ?? 0),
    scriptContent: String(row.scriptContent ?? row.content ?? ''),
    scriptType: row.scriptType == null ? undefined : String(row.scriptType),
    style: row.style == null ? undefined : String(row.style),
    requirement: row.requirement == null ? undefined : String(row.requirement),
    durationLimitSec,
    duration: durationLimitSec,
    productId: numberOrUndefined(row.productId),
    aiGenerated: boolish(row.aiGenerated) ?? Boolean(row.aiGenerated),
    violationChecked: boolish(row.violationChecked) ?? Boolean(row.violationChecked),
    viewerDelta: numberOrUndefined(row.viewerDelta),
    interactionDelta: numberOrUndefined(row.interactionDelta),
    conversionDelta: numberOrUndefined(row.conversionDelta),
    effectivenessScore: numberOrUndefined(row.effectivenessScore ?? row.score),
    sequenceNo,
    sortOrder: sequenceNo,
    executed: numberOrUndefined(row.executed) ?? 0,
    createTime: String(row.createTime ?? ''),
    updateTime: String(row.updateTime ?? ''),
  } as LiveScript
}

function normalizeLiveScriptArray(raw: unknown): LiveScript[] {
  return normalizeArray<unknown>(raw).map(normalizeLiveScriptRow)
}

function normalizeVersionRow(raw: unknown): LiveScriptVersionVO {
  const row = isRecord(raw) ? raw : {}
  return {
    ...(row as unknown as LiveScriptVersionVO),
    id: Number(row.id ?? 0),
    scriptId: Number(row.scriptId ?? 0),
    versionNumber: Number(row.versionNumber ?? row.versionNo ?? 0),
    scriptContent: String(row.scriptContent ?? row.content ?? ''),
    scriptType: row.scriptType == null ? undefined : String(row.scriptType),
    style: row.style == null ? undefined : String(row.style),
    durationLimitSec: numberOrUndefined(row.durationLimitSec ?? row.duration),
    requirement: row.requirement == null ? undefined : String(row.requirement),
    effectivenessScore: numberOrUndefined(row.effectivenessScore),
    editorId: numberOrUndefined(row.editorId),
    editorName: row.editorName == null ? undefined : String(row.editorName),
    isCurrent: Number(row.isCurrent ?? (row.versionStatus === 'active' ? 1 : 0)),
    createTime: String(row.createTime ?? ''),
  }
}

export function searchScripts(data?: LiveScriptSearchVO) {
  const params = data || {}
  return request.post<unknown>('/live/script/search', params)
    .then(raw => normalizePage<unknown, LiveScript>(raw, normalizeLiveScriptRow, params.page ?? 0, params.rows ?? 20))
}

/** 批量排序话术（按 scriptIds 顺序更新 sequenceNo） */
export function batchSortScripts(sessionId: number, scriptIds: number[]) {
  return request.post<void>('/live/script/batch-sort', { sessionId, scriptIds })
}

/** 场次话术列表 */
export function getScriptsBySession(sessionId: number) {
  return request.post<unknown>('/live/script/by-session', { sessionId }).then(normalizeLiveScriptArray)
}

/** 初始化话术槽位（有产品无话术时创建占位槽位） */
export function initScriptSlots(sessionId: number) {
  return request.post<void>('/live/script/init-slots', { sessionId })
}

/** 快速全部删除话术（仅删除，不重建；有产品时下次加载会自动初始化空槽位） */
export function clearAllScripts(sessionId: number) {
  return request.post<void>('/live/script/clear-slots', { sessionId })
}

/** 重建话术槽位（产品重排后按新顺序重建，会清空现有话术） */
export function rebuildScriptSlots(sessionId: number) {
  return request.post<void>('/live/script/rebuild-slots', { sessionId })
}

/** 按场次轮换/热度策略重算 product 槽位绑品（不删槽） */
export function applySlotStrategies(sessionId: number) {
  return request.post<void>('/live/script/apply-slot-strategies', { sessionId })
}

/** 保存话术到话术库（单条） */
export function saveScriptToLibrary(scriptId: number) {
  return request.post<number>('/live/script/save-to-library', { scriptId })
}

/** 批量保存话术到话术库（scriptIds 为空则保存整场） */
export function saveBatchToLibrary(sessionId: number, scriptIds?: number[]) {
  return request.post<number>('/live/script/save-batch-to-library', { sessionId, scriptIds: scriptIds ?? [] })
}

/** 导出场次话术为 Markdown 文本 */
export function exportScripts(sessionId: number) {
  return request.post<string>('/live/script/export', { sessionId })
}

/** 保存话术（返回完整 VO） */
export function saveLiveScript(data: LiveScriptSaveVO) {
  return request.post<LiveScript>('/live/script/save', data)
}

/** 批量保存话术（单次事务，最多 500 条） */
export function batchSaveLiveScripts(sessionId: number, scripts: LiveScriptSaveVO[]) {
  return request.post<unknown>('/live/script/batch-save', { sessionId, scripts }).then(normalizeLiveScriptArray)
}

/** 删除话术 */
export function deleteLiveScript(id: number) {
  return request.post<void>('/live/script/delete', { id })
}

/** 更新话术执行状态（0=未执行 1=已执行） */
export function updateScriptExecuted(id: number, executed: number) {
  return request.post<void>('/live/script/executed', { id, executed })
}

/** 话术效果排行（单场次内） */
export function getScriptEffectiveness(sessionId: number) {
  return request.post<unknown>('/live/script/effectiveness', { sessionId }).then(normalizeLiveScriptArray)
}

// ========== 话术版本管理 API ==========

export interface LiveScriptVersionVO {
  id: number
  scriptId: number
  versionNumber: number
  scriptContent: string
  scriptType?: string
  style?: string
  durationLimitSec?: number
  requirement?: string
  changeReason?: string
  effectivenessScore?: number
  editorId?: number
  editorName?: string
  isCurrent: number
  createTime?: string
}

/** 获取话术版本列表（对齐后端 /live/script/version/getByScriptId，body 为 scriptId） */
export function getScriptVersions(scriptId: number) {
  return request.post<unknown>('/live/script/version/getByScriptId', { scriptId })
    .then(raw => normalizeArray<unknown>(raw).map(normalizeVersionRow))
}

/** 获取指定版本（对齐后端 getByScriptIdAndVersionNumber） */
export function getScriptVersion(scriptId: number, versionNumber: number) {
  return request.post<LiveScriptVersionVO>('/live/script/version/getByScriptIdAndVersionNumber', {
    scriptId,
    versionNumber,
  })
}

/** 获取当前版本（对齐后端 getLatestVersion） */
export function getCurrentScriptVersion(scriptId: number) {
  return request.post<LiveScriptVersionVO>('/live/script/version/getLatestVersion', scriptId)
}

/** 回滚到指定版本（对齐后端 rollback） */
export function rollbackScriptVersion(scriptId: number, versionNumber: number, userId: number, userName: string) {
  return request.post<void>('/live/script/version/rollback', {
    scriptId,
    versionNumber,
    userId,
    userName,
  })
}

/** 获取版本总数（对齐后端 count） */
export function getScriptVersionCount(scriptId: number) {
  return request.post<number>('/live/script/version/count', { scriptId })
}

// ─── 话术审核 ─────────────────────────────────────────────

export interface ScriptApprovalVO {
  id: number
  scriptId: number
  sessionId: number
  submitterId: number
  reviewerId: number | null
  action: string
  status: number
  comments: string | null
  reviewTime: string | null
  createTime: string
  updateTime: string
  scriptContent?: string
  scriptType?: string
  sessionTitle?: string
}

/** 提交话术审核 */
export function submitScriptApproval(scriptId: number, comments?: string) {
  return request.post<ScriptApprovalVO>('/live/script-approval/submit', { scriptId, comments })
}

/** 审批话术（通过/拒绝） */
export function reviewScriptApproval(approvalId: number, action: 'approve' | 'reject', comments?: string) {
  return request.post<ScriptApprovalVO>('/live/script-approval/review', { approvalId, action, comments })
}

/** 撤回审核 */
export function revokeScriptApproval(scriptId: number) {
  return request.post<void>('/live/script-approval/revoke', { scriptId })
}

/** 分页查询审核记录 */
export function searchScriptApprovals(params: { scriptId?: number; sessionId?: number; status?: number; page?: number; rows?: number }) {
  return request.post<unknown>('/live/script-approval/search', params)
    .then(raw => normalizePage<unknown, ScriptApprovalVO>(raw, item => item as ScriptApprovalVO, params.page ?? 0, params.rows ?? 20))
}

/** 查询话术审核历史 */
export function getScriptApprovalHistory(scriptId: number) {
  return request.post<unknown>('/live/script-approval/history', { scriptId }).then(raw => normalizeArray<ScriptApprovalVO>(raw))
}

// ─── 话术模板库 ─────────────────────────────────────────────

export interface ScriptTemplate {
  id: number
  templateName: string
  scriptType: string
  category: string | null
  content: string
  effectivenessScore: number
  usageCount: number
  industryTags: string | null
  autoCollected: number
  /** P3-04: 行业编码（cosmetics/food/clothing/jewelry/digital 等） */
  industryCode?: string | null
  /** P3-04: 是否系统预置行业模板（0=用户创建, 1=预置） */
  isPreset?: number
  createTime: string
}

/** 搜索模板 */
export function searchTemplates(params: {
  scriptType?: string
  category?: string
  keyword?: string
  /** P3-04 行业模板：行业编码（cosmetics/food/clothing/jewelry/digital）*/
  industryCode?: string
  /** P3-04 仅查预置行业模板 */
  presetOnly?: boolean
  page?: number
  rows?: number
}) {
  return request.post<unknown>('/live/template/search', params)
    .then(raw => normalizePage<unknown, ScriptTemplate>(raw, item => item as ScriptTemplate, params.page ?? 0, params.rows ?? 20))
}

/** 应用模板 */
export function applyTemplate(templateId: number) {
  return request.post<{ content: string; scriptType: string; templateName: string }>('/live/template/apply', { templateId })
}

/** 保存话术为模板 */
export function saveScriptAsTemplate(scriptId: number, templateName?: string, category?: string) {
  return request.post<ScriptTemplate>('/live/template/save-from-script', { scriptId, templateName, category })
}

/** 将当前场次话术保存为模板 */
export function saveSessionAsTemplate(sessionId: number, templateName: string, scriptTypes?: string[]) {
  return request.post<ScriptTemplate>('/live/session-template/save-from-session', {
    sessionId,
    templateName,
    scriptTypes: scriptTypes ?? [],
  })
}

// ─── 脚本行内评论 ─────────────────────────────────────────────

export interface ScriptComment {
  id: number
  scriptId: number
  sessionId?: number
  content: string
  authorId?: number
  authorName?: string
  resolved?: boolean
  resolvedBy?: number
  resolvedAt?: string
  parentId?: number
  createTime?: string
}

/** 获取脚本评论列表 */
export function listScriptComments(scriptId: number) {
  return request.post<unknown>('/live/script-comment/by-script', { scriptId }).then(raw => normalizeArray<ScriptComment>(raw))
}

/** 添加评论 */
export function addScriptComment(data: { scriptId: number; sessionId: number; content: string; parentId?: number }) {
  return request.post<ScriptComment>('/live/script-comment/save', data)
}

/** 标记评论已解决 */
export function resolveScriptComment(id: number) {
  return request.post<void>('/live/script-comment/resolve', { commentId: id })
}

/** 获取场次未解决评论数 */
export function countUnresolvedComments(sessionId: number) {
  return request.post<number>('/live/script-comment/unresolved-count', { sessionId })
}
