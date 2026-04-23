import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface OperationLog { id: number; userId: number; username: string; action: string; module: string; ip: string; createTime: string }
export interface LogQuery { page?: number; rows?: number; username?: string; module?: string; startTime?: string; endTime?: string }

export interface AuditLog {
  id: number; userId: number; username: string; module: string; action: string
  targetId: number | null; targetType: string; beforeValue: string | null
  afterValue: string | null; ip: string; createTime: string
}
export interface AuditLogQuery { page?: number; rows?: number; username?: string; module?: string; action?: string; startTime?: string; endTime?: string }

export const logApi = {
  list: (params: LogQuery) => request.post<PageResult<OperationLog>>('/log/operation/page', params),
  systemList: (params: LogQuery) => request.post<PageResult<OperationLog>>('/log/system/page', params),
  auditLogPage: (params: AuditLogQuery) => request.post<PageResult<AuditLog>>('/log/audit/page', params),
  auditLogGet: (id: number) => request.post<AuditLog>('/log/audit/get', { id }),
}
