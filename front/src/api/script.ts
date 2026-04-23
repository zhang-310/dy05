import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface ScriptItem {
  id: number; userId: number; title: string; content: string
  category: string; tags: string; scriptType: string; industry: string
  duration: number; useCount: number; rating: number; status: number
  createTime: string; updateTime: string
}
export interface ScriptQuery { page?: number; rows?: number; keyword?: string; category?: string; scriptType?: string; status?: number }
export interface ScriptSave {
  id?: number; title: string; content: string; category?: string
  tags?: string; scriptType?: string; industry?: string; duration?: number; status?: number
}

export interface ScriptTemplate {
  id: number; templateName: string; templateContent: string; scene: string
  industry: string; tags: string; useCount: number; status: number; createTime: string
}
export interface ScriptTemplateQuery { page?: number; rows?: number; keyword?: string; scene?: string; industry?: string }

export interface ViolationWord {
  id: number; word: string; category: string; severity: number
  replacement: string; status: number; createTime: string
}
export interface ViolationWordQuery { page?: number; rows?: number; word?: string; category?: string; severity?: number; status?: number }
export interface ViolationWordSave { id?: number; word: string; category?: string; severity?: number; replacement?: string; status?: number }

export interface BatchCheckRow {
  id: number; title: string; count: number; maxSeverity: number; status: string
}

export const scriptApi = {
  list: (p: ScriptQuery) => request.post<PageResult<ScriptItem>>('/script/list', p),
  get: (id: number) => request.post<ScriptItem>('/script/get', { id }),
  save: (p: Partial<ScriptSave>) => request.post<void>('/script/save', p),
  delete: (id: number) => request.post<void>('/script/delete', { id }),
  generate: (p: Record<string, unknown>) => request.post<ScriptItem>('/script/generate', p),
  updateUseCount: (id: number) => request.post<void>('/script/use-count', { id }),
  categories: () => request.post<string[]>('/script/categories', {}),

  templateSearch: (p: ScriptTemplateQuery) => request.post<PageResult<ScriptTemplate>>('/script/template/search', p),
  templateGet: (id: number) => request.post<ScriptTemplate>('/script/template/get', { id }),
  templateSave: (p: Partial<ScriptTemplate>) => request.post<void>('/script/template/save', p),
  templateDelete: (id: number) => request.post<void>('/script/template/delete', { id }),
  templateByScene: (scene: string) => request.post<ScriptTemplate[]>('/script/template/by-scene', { scene }),

  violationList: (p: ViolationWordQuery) => request.post<PageResult<ViolationWord>>('/script/admin/violation/list', p),
  violationSave: (p: Partial<ViolationWordSave>) => request.post<void>('/script/admin/violation/save', p),
  violationDelete: (id: number) => request.post<void>('/script/admin/violation/delete', { id }),
  violationToggle: (id: number) => request.post<void>('/script/admin/violation/active', { id }),
  violationPublicList: (p: ViolationWordQuery) => request.post<PageResult<ViolationWord>>('/script/violation/public/list', p),
  violationCheck: (text: string) => request.post<{ violations: ViolationWord[] }>('/script/violation/check', { text }),
  violationCheckBatch: (texts: string[]) => request.post<BatchCheckRow[]>('/script/violation/check-batch', { texts }),
  violationSuggestReplacement: (word: string) => request.post<{ replacement: string }>('/script/violation/suggest-replacement', { word }),

  complianceCheck: (text: string) => request.post<Record<string, unknown>>('/script/compliance/check', { text }),
  complianceRules: () => request.post<Record<string, unknown>[]>('/script/compliance/rules', {}),

  searchHybrid: (p: Record<string, unknown>) => request.post<PageResult<ScriptItem>>('/script/search/hybrid', p),
  searchSemantic: (p: Record<string, unknown>) => request.post<PageResult<ScriptItem>>('/script/search/semantic', p),
  searchSuggest: (keyword: string) => request.post<string[]>('/script/search/suggest', { keyword }),
}

