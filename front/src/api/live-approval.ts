import request from '@/utils/request'
import { normalizeArray } from '@/utils/response-normalize'

export interface LiveApprovalHistoryRecord {
  id: number
  sessionId: number
  scriptId?: number
  action: string
  operatorId: number
  comment?: string
  createTime?: string
}

export interface PendingApprovalSession {
  id: number
  userId: number
  accountId?: number
  personaId?: number
  liveTitle: string
  sessionCover?: string
  scriptStyle?: string
  liveDescription?: string
  scheduledTime?: string
  scheduledEndTime?: string
  startTime?: string
  endTime?: string
  liveUrl?: string
  viewers?: number
  likes?: number
  status?: number
  orgId?: number
  createTime?: string
  updateTime?: string
}

/**
 * 直播话术审批 API（与 `LiveApprovalController` 对齐）。
 * approve/reject 必须传 `scriptId`；仅 `submit` / `history` / `pending` 仅需 session 或无需 body。
 */
export function submitApproval(data: { sessionId: number }) {
  return request.post<void>('/live/approval/submit', data)
}

export function approveScript(data: { sessionId: number; scriptId: number; comment?: string }) {
  return request.post<void>('/live/approval/approve', data)
}

export function rejectScript(data: { sessionId: number; scriptId: number; comment?: string }) {
  return request.post<void>('/live/approval/reject', data)
}

export function getApprovalHistory(data: { sessionId: number }) {
  return request.post<unknown>('/live/approval/history', data).then(raw => normalizeArray<LiveApprovalHistoryRecord>(raw))
}

/** 待审批场次列表；后端无需请求体。 */
export function getPendingApprovals() {
  return request.post<unknown>('/live/approval/pending').then(raw => normalizeArray<PendingApprovalSession>(raw))
}
