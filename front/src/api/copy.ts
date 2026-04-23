import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface CopyItem {
  id: number; title: string; content: string; category: string
  tags: string; useCount: number; rating?: number; status: number; createTime: string
}
export interface CopyQuery {
  page?: number; rows?: number
  keyword?: string; title?: string
  category?: string; status?: number
  tags?: string; minScore?: number
}
export interface CopySave {
  id?: number; title: string; content: string; category?: string; tags?: string; status?: number
}

export interface CopyTemplate {
  id: number; templateName: string; content: string; category: string
  variables?: string; status: number; createTime: string
}
export interface CopyTemplateQuery { page?: number; rows?: number; templateName?: string; category?: string; status?: number }

export interface CopyApproval {
  id: number; copyId: number; approvalStatus: number; comments: string
  approvalTime: string; createTime: string; copyContent?: string
}
export interface CopyApprovalQuery { page?: number; rows?: number; approvalStatus?: number }

export const copyApi = {
  // Library
  list: (params: CopyQuery) => request.post<PageResult<CopyItem>>('/copy/library/search', params),
  get: (id: number) => request.post<CopyItem>('/copy/library/get', { id }),
  save: (params: Partial<CopySave>) => request.post<number>('/copy/library/save', params),
  delete: (id: number) => request.post<void>('/copy/library/delete', { id }),
  updateStatus: (id: number, status: number) => request.post<void>('/copy/library/update-status', { id, status }),
  incrementUseCount: (id: number) => request.post<void>('/copy/library/increment-use-count', { id }),

  // Template
  templateList: (params: CopyTemplateQuery) => request.post<PageResult<CopyTemplate>>('/copy/template/search', params),
  templateGet: (id: number) => request.post<CopyTemplate>('/copy/template/get', { id }),
  templateSave: (params: Partial<CopyTemplate>) => request.post<number>('/copy/template/save', params),
  templateDelete: (id: number) => request.post<void>('/copy/template/delete', { id }),
  templateUpdateStatus: (id: number, status: number) => request.post<void>('/copy/template/update-status', { id, status }),

  // Approval
  approvalSearch: (params: CopyApprovalQuery) => request.post<PageResult<CopyApproval>>('/copy/approval/search', params),
  approvalGet: (id: number) => request.post<CopyApproval>('/copy/approval/get', { id }),
  approvalSave: (params: Record<string, unknown>) => request.post<number>('/copy/approval/save', params),
  approvalDelete: (id: number) => request.post<void>('/copy/approval/delete', { id }),
  approvalApprove: (id: number, comment?: string) => request.post<void>('/copy/approval/approve', { id, comment }),
  approvalReject: (id: number, comment?: string) => request.post<void>('/copy/approval/reject', { id, comment }),
  approvalRevise: (id: number, content: string) => request.post<void>('/copy/approval/revise', { id, content }),

  // Batch operations (06-copy v3.0)
  batchSubmitApproval: (ids: number[]) => request.post<void>('/copy/library/batch-submit-approval', { ids }),
  batchDelete: (ids: number[]) => request.post<void>('/copy/library/batch-delete', { ids }),
  batchTag: (ids: number[], tags: string[]) => request.post<void>('/copy/library/batch-tag', { ids, tags }),
  exportCsv: (params: CopyQuery) => request.post<{ downloadUrl: string }>('/copy/library/export', params),

  // Usage records (点击「使用次数」列)
  usageList: (copyId: number) => request.post<Array<{ usedAt: string; sessionId?: number; userId?: number; sessionTitle?: string; userName?: string }>>('/copy/library/usage-list', { copyId }),

  // Semantic search (向量检索)
  semanticSearch: (query: string, params?: { page?: number; rows?: number }) =>
    request.post<PageResult<CopyItem>>('/copy/library/semantic-search', { query, ...params }),

  // AI generate
  aiGenerate: (params: { prompt?: string; category?: string; count?: number }) =>
    request.post<CopyItem[]>('/copy/library/ai-generate', params),

  // Usage stats / SLA monitoring
  approvalStats: () => request.post<Record<string, unknown>>('/copy/approval/stats', {}),
}
