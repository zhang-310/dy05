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
  id: number; templateName: string; content: string; templateContent?: string; category: string
  variables?: string; description?: string; status: number; createTime: string
}
export interface CopyTemplateQuery { page?: number; rows?: number; keyword?: string; templateName?: string; category?: string; status?: number }

export interface CopyApproval {
  id: number; copyId: number; approvalStatus: number; comments: string
  approvalTime: string; createTime: string; copyContent?: string; copyTitle?: string
}
export interface CopyApprovalQuery { page?: number; rows?: number; approvalStatus?: number }
export interface CopyApprovalSave {
  id?: number; copyId: number; approvalStatus?: number; comments?: string; userId?: number
}
export interface CopyUsageRecord {
  usedAt: string; sessionId?: number; userId?: number; sessionTitle?: string; userName?: string
}

export interface CopyAiGenerateParams {
  prompt?: string; topic?: string; category?: string; count?: number; copyType?: string; style?: string; keywords?: string; duration?: string; personaId?: number; length?: number
}
export interface CopyAiCandidate {
  title: string
  content: string
  category?: string
  tags?: string
  status?: number
  source?: 'copy_ai'
  createTime?: string
}

function extractTemplateVariables(content: string): string {
  const variables = new Set<string>()
  const matcher = content.matchAll(/\{([a-zA-Z_][a-zA-Z0-9_]*)}/g)
  for (const match of matcher) variables.add(match[1])
  return Array.from(variables).join(',')
}

function normalizeTemplate(item: CopyTemplate): CopyTemplate {
  const content = item.content ?? item.templateContent ?? ''
  return {
    ...item,
    content,
    templateContent: item.templateContent ?? content,
    variables: item.variables ?? extractTemplateVariables(content),
  }
}

function normalizeTemplatePage(page: PageResult<CopyTemplate>): PageResult<CopyTemplate> {
  return {
    ...page,
    list: (page.list ?? []).map(normalizeTemplate),
  }
}

function toTemplateQuery(params: CopyTemplateQuery) {
  const { templateName, keyword, ...rest } = params
  return {
    ...rest,
    keyword: keyword ?? templateName,
  }
}

function toTemplateSavePayload(params: Partial<CopyTemplate>) {
  const { content, variables: _variables, ...rest } = params
  return {
    ...rest,
    templateContent: params.templateContent ?? content,
  }
}

function normalizeAiStyle(style?: string) {
  if (!style) return undefined
  const map: Record<string, string> = {
    专业科学: 'professional',
    亲切温暖: 'warm',
    幽默活泼: 'humorous',
    时尚潮流: 'creative',
  }
  return map[style] ?? style
}

function normalizeAiLength(params: CopyAiGenerateParams) {
  if (typeof params.length === 'number') return params.length
  if (params.duration === '30s') return 1
  if (params.duration === '2min') return 3
  return 2
}

function buildAiTopic(params: CopyAiGenerateParams, index: number) {
  const parts = [
    params.topic,
    params.prompt,
    params.copyType,
    params.category,
  ].map(part => part?.trim()).filter((part): part is string => !!part)
  const base = parts.length > 0 ? parts.join(' / ') : '通用文案'
  return index > 0 ? `${base}（第 ${index + 1} 个差异化版本）` : base
}

function toAiCandidate(raw: unknown, params: CopyAiGenerateParams, index: number): CopyAiCandidate {
  const content = typeof raw === 'string'
    ? raw.trim()
    : typeof raw === 'object' && raw !== null && 'content' in raw
      ? String((raw as { content?: unknown }).content ?? '').trim()
      : ''
  const firstLine = content.split('\n').map(line => line.trim()).find(Boolean)
  const title = typeof raw === 'object' && raw !== null && 'title' in raw
    ? String((raw as { title?: unknown }).title ?? '').trim()
    : ''
  const tags = [params.copyType, params.keywords]
    .map(item => item?.trim())
    .filter((item): item is string => !!item)
    .join(',')
  return {
    title: title || firstLine?.slice(0, 24) || `AI 文案候选 ${index + 1}`,
    content,
    category: params.category,
    tags,
    status: 0,
    source: 'copy_ai',
    createTime: new Date().toISOString(),
  }
}

export const copyApi = {
  // Library
  list: (params: CopyQuery) => request.post<PageResult<CopyItem>>('/copy/library/search', params),
  get: (id: number) => request.post<CopyItem>('/copy/library/get', undefined, { params: { id } }),
  save: (params: Partial<CopySave>) => request.post<number>('/copy/library/save', params),
  delete: (id: number) => request.post<void>('/copy/library/delete', undefined, { params: { id } }),
  updateStatus: (id: number, status: number) => request.post<void>('/copy/library/update-status', undefined, { params: { id, status } }),
  incrementUseCount: (id: number) => request.post<void>('/copy/library/increment-use-count', undefined, { params: { id } }),

  // Template
  templateList: async (params: CopyTemplateQuery) =>
    normalizeTemplatePage(await request.post<PageResult<CopyTemplate>>('/copy/template/search', toTemplateQuery(params))),
  templateGet: async (id: number) => normalizeTemplate(await request.post<CopyTemplate>('/copy/template/get', { id })),
  templateSave: (params: Partial<CopyTemplate>) => request.post<number>('/copy/template/save', toTemplateSavePayload(params)),
  templateDelete: (id: number) => request.post<void>('/copy/template/delete', { id }),
  templateUpdateStatus: (id: number, status: number) => request.post<void>('/copy/template/update-status', { id, status }),

  // Approval
  approvalSearch: (params: CopyApprovalQuery) => request.post<PageResult<CopyApproval>>('/copy/approval/search', params),
  approvalGet: (id: number) => request.post<CopyApproval>('/copy/approval/get', { id }),
  approvalSave: (params: CopyApprovalSave) => request.post<number>('/copy/approval/save', params),
  approvalDelete: (id: number) => request.post<void>('/copy/approval/delete', { id }),
  approvalApprove: async (id: number, comment?: string) => {
    const approval = await copyApi.approvalGet(id)
    await copyApi.approvalSave({ id, copyId: approval.copyId, approvalStatus: 1, comments: comment })
  },
  approvalReject: async (id: number, comment?: string) => {
    const approval = await copyApi.approvalGet(id)
    await copyApi.approvalSave({ id, copyId: approval.copyId, approvalStatus: 0, comments: comment })
  },
  approvalRevise: async (_id: number, _content: string) => {
    throw new Error('后端未提供文案审批改稿接口，请先在文案库编辑文案后重新提交审批')
  },

  // Batch operations (06-copy v3.0)
  batchSubmitApproval: async (ids: number[]) => {
    await Promise.all(ids.map(copyId => copyApi.approvalSave({ copyId, approvalStatus: 2 })))
  },
  batchDelete: async (ids: number[]) => {
    await Promise.all(ids.map(id => copyApi.delete(id)))
  },
  batchTag: async (_ids: number[], _tags: string[]): Promise<void> => {
    throw new Error('后端未提供文案批量打标签接口')
  },
  exportCsv: async (_params: CopyQuery): Promise<{ downloadUrl: string }> => {
    throw new Error('后端未提供文案 CSV 导出接口')
  },

  // Usage records (点击「使用次数」列)
  usageList: async (_copyId: number): Promise<CopyUsageRecord[]> => {
    throw new Error('后端未提供文案使用明细接口')
  },

  // Semantic search (向量检索)
  semanticSearch: (query: string, params?: { page?: number; rows?: number }) =>
    request.post<PageResult<CopyItem>>('/copy/library/search', { keyword: query, ...params }),

  // AI generate
  aiGenerate: async (params: CopyAiGenerateParams): Promise<CopyAiCandidate[]> => {
    const count = Math.min(Math.max(Number(params.count ?? 1), 1), 5)
    const results = await Promise.all(
      Array.from({ length: count }, (_, index) => request.post<string | Partial<CopyItem>>('/copy/ai/generate', {
        topic: buildAiTopic(params, index),
        category: params.category,
        style: normalizeAiStyle(params.style),
        keywords: params.keywords,
        personaId: params.personaId,
        length: normalizeAiLength(params),
      })),
    )
    return results.map((result, index) => toAiCandidate(result, params, index))
  },

  // Usage stats / SLA monitoring
  approvalStats: async (): Promise<Record<string, unknown>> => {
    throw new Error('后端未提供文案审批统计接口')
  },
}
