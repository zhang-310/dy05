import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LiveSessionSearchVO, LiveSessionSaveVO, LiveSessionVO } from '@/types/live'

export function searchSessions(data?: LiveSessionSearchVO) {
  return request.post<PageResult<LiveSessionVO>>('/live/session/search', data || {})
}

export function getSession(id: number) {
  return request.post<LiveSessionVO>('/live/session/get', { id })
}

/** 场次数据概览 */
export function getSessionOverview(id: number) {
  return request.post<Record<string, unknown>>('/live/session/overview', { id })
}

export function saveSession(data: LiveSessionSaveVO) {
  return request.post<number>('/live/session/save', data)
}

export function deleteSession(id: number) {
  return request.post<void>('/live/session/delete', { id })
}

/** 更新场次状态（0=preparing 1=live 2=ended 3=cancelled） */
export function updateSessionStatus(id: number, status: number) {
  return request.post<void>('/live/session/status', { id, status })
}

/** 开播准备清单 */
export interface LiveReadinessVO {
  ready: boolean
  checks: {
    products: { passed: boolean; count?: number; message?: string }
    persona: { passed: boolean; personaName?: string; message?: string }
    scripts: { passed: boolean; count?: number; message?: string }
    compliance: { passed: boolean; message?: string }
  }
}

export function getReadiness(sessionId: number) {
  return request.post<LiveReadinessVO>('/live/session/readiness', { id: sessionId })
}

/** 场次汇总数据（观众、点赞、销售额等） */
export function getSessionData(sessionId: number) {
  return request.post<Record<string, unknown> | null>('/live/data/session', { sessionId })
}

/** 场次数据含上一场（环比用） */
export function getSessionDataWithCompare(sessionId: number) {
  return request.post<{ current: Record<string, unknown>; previous: Record<string, unknown> | null }>('/live/data/session/with-compare', { sessionId })
}

/** 同步场次数据（从 LiveMonitor 汇总） */
export function syncSessionData(sessionId: number) {
  return request.post<Record<string, unknown>>('/live/data/session/sync', { sessionId })
}

/** 从抖音开放平台 API 同步直播数据（需 roomId、accessToken） */
export function syncFromDouyin(sessionId: number, roomId: string, accessToken: string) {
  return request.post<Record<string, unknown>>('/live/data/session/sync-from-douyin', { sessionId, roomId, accessToken })
}

/** 历史数据对比：最近 N 场已结束直播 */
export interface LiveHistoryItemVO {
  sessionId: number
  liveTitle?: string
  startTime?: string
  endTime?: string
  status?: number
  sessionData?: Record<string, unknown>
}

export function getHistory(accountId?: number, limit?: number, days?: number) {
  const body: Record<string, unknown> = {}
  if (accountId != null) body.accountId = accountId
  if (limit != null) body.limit = limit
  if (days != null) body.days = days
  return request.post<LiveHistoryItemVO[]>('/live/data/history', body)
}

/** LiveMonitor 时序数据（观众/点赞/评论等） */
export function getMonitorBySession(sessionId: number) {
  return request.post<Array<{ timestamp?: string | number; viewers?: number; likes?: number; comments?: number; shares?: number }>>(
    '/live/monitor/by-session',
    { sessionId }
  )
}

/** 获取最新监控快照（观众/点赞/评论等汇总） */
export function getMonitorSnapshot(sessionId: number) {
  return request.post<Record<string, unknown>>('/live/monitor/snapshot', { sessionId })
}

/** 获取当前查看者列表 */
export function getSessionViewers(sessionId: number) {
  return request.post<Array<{ userId: number; userName: string; joinedAt: string }>>('/live/collaboration/viewers', { sessionId })
}

/** CONTENT-04：直播话术合并为短视频脚本并创建项目 */
export function exportSessionToShortVideo(sessionId: number) {
  return request.post<{ scriptId: number; projectId: number }>('/live/session/export-to-short-video', {
    id: sessionId,
  })
}

/** 克隆场次（新 ID，准备中） */
export function cloneLiveSession(sourceSessionId: number) {
  return request.post<number>('/live/session/clone', { sourceSessionId })
}

// Re-export types for backward compatibility
export type { LiveSessionSearchVO, LiveSessionSaveVO, LiveSessionVO } from '@/types/live'
export type { LiveSessionVO as LiveSession } from '@/types/live'

/** ��ֱ����ʽ�ۺ� ROI ���� */
export function getSessionFormatRoi(data?: { accountId?: number; days?: number }) {
  return request.post<Array<{
    liveFormat: string
    sessionCount: number
    totalViewers: number
    avgViewers: number
    avgDurationMin: number
    completionRate: number
  }>>('/live/session/format-roi', data || {})
}
