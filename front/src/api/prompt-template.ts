import request from '@/utils/request'

export interface PromptTemplate {
  id: number
  templateCode: string
  templateName: string
  category: string
  scriptType: string
  content: string
  variables: string
  status: number
  avgScore: number | null
  usageCount: number
  createTime: string
  updateTime: string
}

export function searchPromptTemplates(data: { page?: number; rows?: number; keyword?: string; category?: string; status?: number; templateCode?: string }) {
  return request.post<{ total: number; list: PromptTemplate[] }>('/ai/prompt-template/list', data)
}

export function getPromptTemplate(id: number) {
  return request.post<PromptTemplate>('/ai/prompt-template/get', { id })
}

export function savePromptTemplate(data: Partial<PromptTemplate> | Record<string, unknown>) {
  return request.post<number>('/ai/prompt-template/save', data)
}

export function deletePromptTemplate(idOrObj: number | { id: number }) {
  const id = typeof idOrObj === 'number' ? idOrObj : idOrObj.id
  return request.post<void>('/ai/prompt-template/delete', { id })
}

export function activatePromptTemplate(id: number) {
  return request.post<PromptTemplate>('/ai/prompt-template/get-active', { id })
}

export function testRenderTemplate(data: { id: number; variables: Record<string, string> }) {
  return request.post<unknown>('/ai/prompt-template/test-render', data)
}
