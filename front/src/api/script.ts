import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import {
  isRecord,
  normalizeArray as normalizeResponseArray,
  normalizePage as normalizeResponsePage,
  normalizeStringArray,
  parseJsonValue,
  readNumber,
} from '@/utils/response-normalize'

export interface ScriptItem {
  id: number; userId: number; title: string; content: string
  category: string; tags: string; scriptType?: string; industry?: string
  source?: string; sourceId?: number
  duration?: number; useCount: number; rating?: number; status: number
  createTime: string; updateTime: string
}
export interface ScriptQuery { page?: number; rows?: number; keyword?: string; category?: string; scriptType?: string; industry?: string; source?: string; status?: number }
export interface ScriptSave {
  id?: number; title: string; content: string; category?: string
  tags?: string; scriptType?: string; industry?: string; source?: string; sourceId?: number; duration?: number; status?: number
}

export interface ScriptTemplate {
  id: number
  templateName: string
  templateType?: string
  scene?: string
  content?: string
  templateContent?: string
  description?: string
  industry?: string
  tags?: string
  userId?: number
  useCount?: number
  status?: number
  createTime?: string
  updateTime?: string
}
export interface ScriptTemplateQuery { page?: number; rows?: number; keyword?: string; templateType?: string; scene?: string; status?: number; userId?: number; industry?: string }

export interface ViolationWord {
  id: number; word: string; category?: string; severity?: number
  level?: number; reason?: string; scope?: string
  replacement: string; status: number; createTime: string
}
export interface ViolationWordQuery { page?: number; rows?: number; keyword?: string; word?: string; category?: string; severity?: number; level?: number; reason?: string; status?: number }
export interface ViolationWordSave { id?: number; word: string; category?: string; severity?: number; level?: number; reason?: string; scope?: string; replacement?: string; status?: number }

export interface BatchCheckRow {
  id: number; title: string; count: number; maxSeverity: number; status: string
}

interface ViolationCheckBatchApiResult {
  results?: Record<string, {
    hasViolation?: boolean
    totalCount?: number
    violations?: Array<{ level?: number; severity?: number }>
  }>
  totalViolations?: number
}

interface SearchSuggestionApiResult {
  suggestions?: Array<{ text?: string }>
  hotTopics?: Array<{ topic?: string }>
}

export interface ScriptHybridSearchItem extends ScriptItem {
  scriptId?: number
  style?: string
  author?: string
  vectorScore?: number
  lexicalScore?: number
  hybridScore?: number
  effectivenessScore?: number
  usageCount?: number
  favoriteCount?: number
  createdAt?: string
  score?: number
  module?: string
  ownerUserId?: number
}

export interface ScriptHybridSearchQuery {
  query: string
  page?: number
  rows?: number
  topK?: number
  category?: string
  style?: string
  mode?: 'hybrid' | 'semantic' | 'keyword'
  sources?: string[]
  vectorWeight?: number
  lexicalWeight?: number
  withCrossEncoder?: boolean
}

export interface ScriptHybridSearchResult extends PageResult<ScriptHybridSearchItem> {
  searchTime?: number
  executedAt?: string
}

export interface ScriptGenerationRequest {
  productName: string
  productPrice: number
  keyFeatures: string[]
  duration: number
  style: string
  variants?: number
}

export interface ScriptVariant {
  id: string
  content: string
  score?: number
  keyPoints?: string
  likes?: number
  uses?: number
}

export interface ScriptGenerationResult {
  id: number
  variants: ScriptVariant[]
  generationTime?: number
}

export interface ScriptOptimizationRequest {
  originalContent: string
  goal?: string
  style?: string
}

export interface ScriptOptimizationResult {
  optimizedContent: string
  originalScore?: number
  optimizedScore?: number
  suggestions: string[]
  generationTime?: number
  style?: string
  goal?: string
}

function idParam(id: number) {
  return { params: { id } }
}

function unwrapData(raw: unknown): unknown {
  const value = parseJsonValue(raw)
  if (isRecord(value) && value.data != null) return parseJsonValue(value.data)
  return value
}

function readOptionalNumber(raw: unknown, keys: string[]): number | undefined {
  const value = readNumber(raw, keys, Number.NaN)
  return Number.isFinite(value) ? value : undefined
}

function readString(raw: unknown, keys: string[]): string | undefined {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) return undefined
  for (const key of keys) {
    const item = value[key]
    if (item != null && String(item) !== '') return String(item)
  }
  if (value.data != null) return readString(value.data, keys)
  return undefined
}

function readValue(raw: unknown, keys: string[]): unknown {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) return undefined
  for (const key of keys) {
    if (value[key] != null) return value[key]
  }
  if (value.data != null) return readValue(value.data, keys)
  return undefined
}

function compactPayload<T extends Record<string, unknown>>(record: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(record).filter(([, value]) => value !== undefined && value !== null && value !== '')
  ) as Partial<T>
}

function normalizeScriptQuery(p: ScriptQuery) {
  return compactPayload({
    page: p.page,
    rows: p.rows,
    keyword: p.keyword,
    category: p.category ?? p.industry,
    source: p.source ?? p.scriptType,
    status: p.status,
  })
}

function normalizeScriptItem(raw: unknown): ScriptItem {
  const value = unwrapData(raw)
  const record = isRecord(value) ? value : {}
  const id = readNumber(record, ['id', 'scriptId'], 0)
  const category = readString(record, ['category', 'industry', 'scene']) ?? ''
  const source = readString(record, ['source', 'scriptType', 'type'])
  const createTime = readString(record, ['createTime', 'createdAt', 'createdTime']) ?? ''
  return {
    id,
    userId: readNumber(record, ['userId', 'ownerId', 'ownerUserId'], 0),
    title: readString(record, ['title', 'scriptTitle', 'name']) ?? '',
    content: readString(record, ['content', 'scriptContent', 'text']) ?? '',
    category,
    tags: readString(record, ['tags', 'tag']) ?? '',
    scriptType: source,
    industry: category,
    source,
    sourceId: readOptionalNumber(record, ['sourceId']),
    duration: readOptionalNumber(record, ['duration', 'durationSec']),
    useCount: readNumber(record, ['useCount', 'usageCount', 'usedCount'], 0),
    rating: readOptionalNumber(record, ['rating', 'score', 'effectivenessScore']),
    status: readNumber(record, ['status'], 1),
    createTime,
    updateTime: readString(record, ['updateTime', 'updatedAt', 'updatedTime']) ?? createTime,
  }
}

function normalizeScriptSave(p: Partial<ScriptSave>) {
  return compactPayload({
    id: p.id,
    title: p.title,
    content: p.content,
    category: p.category ?? p.industry,
    tags: p.tags,
    source: p.source ?? p.scriptType,
    sourceId: p.sourceId,
    status: p.status,
  })
}

function normalizeViolationQuery(p: ViolationWordQuery) {
  return compactPayload({
    page: p.page,
    rows: p.rows,
    keyword: p.keyword ?? p.word,
    level: p.level ?? p.severity,
    reason: p.reason ?? p.category,
    status: p.status,
  })
}

function normalizeViolationSave(p: Partial<ViolationWordSave>) {
  return compactPayload({
    id: p.id,
    word: p.word,
    level: p.level ?? p.severity,
    reason: p.reason ?? p.category,
    scope: p.scope ?? 'all',
    replacement: p.replacement,
    status: p.status ?? 1,
  })
}

function normalizeTemplateSave(p: Partial<ScriptTemplate>) {
  return {
    id: p.id,
    templateName: p.templateName,
    templateType: p.templateType ?? 'user',
    scene: p.scene,
    content: p.content ?? p.templateContent,
    description: p.description ?? p.tags ?? p.industry,
    userId: p.userId,
    status: p.status ?? 1,
  }
}

function normalizeTemplateSearch(p: ScriptTemplateQuery) {
  return compactPayload({
    page: p.page,
    rows: p.rows,
    keyword: p.keyword,
    templateType: p.templateType,
    scene: p.scene,
    status: p.status,
    userId: p.userId,
  })
}

function normalizeScriptTemplate(raw: unknown): ScriptTemplate {
  const value = unwrapData(raw)
  const record = isRecord(value) ? value : {}
  return {
    id: readNumber(record, ['id', 'templateId'], 0),
    templateName: readString(record, ['templateName', 'name', 'title']) ?? '',
    templateType: readString(record, ['templateType', 'type']),
    scene: readString(record, ['scene']),
    content: readString(record, ['content', 'templateContent']),
    templateContent: readString(record, ['templateContent', 'content']) ?? '',
    description: readString(record, ['description']),
    industry: readString(record, ['industry']),
    tags: readString(record, ['tags']),
    userId: readOptionalNumber(record, ['userId', 'ownerId']),
    useCount: readOptionalNumber(record, ['useCount', 'usageCount']),
    status: readOptionalNumber(record, ['status']),
    createTime: readString(record, ['createTime', 'createdAt', 'createdTime']),
    updateTime: readString(record, ['updateTime', 'updatedAt', 'updatedTime']),
  }
}

function normalizeBatchRows(raw: ViolationCheckBatchApiResult | BatchCheckRow[] | undefined): BatchCheckRow[] {
  if (Array.isArray(raw)) return raw
  const results = raw?.results ?? {}
  return Object.entries(results).map(([key, result]) => {
    const violations = result.violations ?? []
    const maxSeverity = violations.reduce((max, item) => {
      const level = Number(item.level ?? item.severity ?? 0)
      return level > 0 ? Math.max(max, level) : max
    }, 0)
    const count = Number(result.totalCount ?? violations.length)
    return {
      id: Number(key) || 0,
      title: key,
      count,
      maxSeverity: count > 0 ? maxSeverity : 0,
      status: count > 0 ? '需修改' : '通过',
    }
  })
}

function normalizeSuggestions(raw: SearchSuggestionApiResult | string[] | undefined): string[] {
  if (Array.isArray(raw)) return raw
  const suggestions = raw?.suggestions?.map((item) => item.text).filter((item): item is string => !!item) ?? []
  const hotTopics = raw?.hotTopics?.map((item) => item.topic).filter((item): item is string => !!item) ?? []
  return [...suggestions, ...hotTopics]
}

function normalizeHybridPayload(p: ScriptHybridSearchQuery) {
  const mode = p.mode ?? 'hybrid'
  const vectorWeight = p.vectorWeight ?? (mode === 'keyword' ? 0 : mode === 'semantic' ? 1 : 0.5)
  const lexicalWeight = p.lexicalWeight ?? (mode === 'keyword' ? 1 : mode === 'semantic' ? 0 : 0.5)
  return {
    query: p.query,
    page: p.page ?? 0,
    rows: p.rows ?? p.topK ?? 20,
    topK: p.topK ?? p.rows ?? 20,
    category: p.category,
    style: p.style,
    vectorWeight,
    lexicalWeight,
    withCrossEncoder: p.withCrossEncoder ?? false,
  }
}

function normalizeViolationWord(raw: unknown): ViolationWord {
  const value = unwrapData(raw)
  const record = isRecord(value) ? value : {}
  const level = readOptionalNumber(record, ['level', 'severity'])
  return {
    id: readNumber(record, ['id', 'wordId'], 0),
    word: readString(record, ['word', 'keyword']) ?? '',
    category: readString(record, ['category', 'reason']),
    severity: level,
    level,
    reason: readString(record, ['reason', 'category']),
    scope: readString(record, ['scope']),
    replacement: readString(record, ['replacement', 'suggestion', 'replaceWord']) ?? '',
    status: readNumber(record, ['status'], 1),
    createTime: readString(record, ['createTime', 'createdAt', 'createdTime']) ?? '',
  }
}

function normalizeViolationCheckResult(raw: unknown): { hasViolation?: boolean; totalCount?: number; violations: ViolationWord[]; score?: number } {
  const value = unwrapData(raw)
  const violations = normalizeResponseArray<unknown>(readValue(value, ['violations', 'items', 'list', 'records'])).map(normalizeViolationWord)
  return {
    hasViolation: isRecord(value) ? Boolean(value.hasViolation ?? value.has_violation ?? violations.length > 0) : violations.length > 0,
    totalCount: readOptionalNumber(value, ['totalCount', 'count']) ?? violations.length,
    violations,
    score: readOptionalNumber(value, ['score', 'complianceScore']),
  }
}

function normalizeHybridSearchResult(raw: unknown): ScriptHybridSearchResult {
  const list = normalizeResponseArray<Partial<ScriptHybridSearchItem> & Partial<ScriptItem>>(raw).map((item) => {
    const scriptId = Number((item as ScriptHybridSearchItem).scriptId ?? item.id ?? 0)
    const score = Number((item as ScriptHybridSearchItem).hybridScore ?? (item as ScriptHybridSearchItem).score ?? 0)
    const createTime = (item as ScriptHybridSearchItem).createdAt ?? item.createTime ?? ''
    return {
      ...item,
      id: scriptId,
      scriptId,
      title: item.title ?? '',
      content: item.content ?? '',
      userId: Number((item as ScriptItem).userId ?? (item as ScriptHybridSearchItem).ownerUserId ?? 0),
      category: item.category ?? '',
      tags: (item as ScriptItem).tags ?? '',
      useCount: Number((item as ScriptHybridSearchItem).usageCount ?? (item as ScriptItem).useCount ?? 0),
      status: (item as ScriptItem).status ?? 1,
      createTime,
      updateTime: item.updateTime ?? createTime,
      score,
      vectorScore: Number((item as ScriptHybridSearchItem).vectorScore ?? 0),
      lexicalScore: Number((item as ScriptHybridSearchItem).lexicalScore ?? 0),
      hybridScore: score,
      source: (item as ScriptHybridSearchItem).source ?? 'script',
      module: (item as ScriptHybridSearchItem).module ?? 'script',
    } as ScriptHybridSearchItem
  })
  return {
    total: readNumber(raw, ['total', 'totalElements', 'count', 'totalCount', 'totalRecords'], list.length),
    list,
    pageNum: readNumber(raw, ['pageNum', 'page', 'pageNumber', 'current'], 0),
    pageSize: readNumber(raw, ['pageSize', 'size', 'rows'], list.length),
    searchTime: readNumber(raw, ['searchTime', 'search_time', 'executionTimeMs'], 0),
    executedAt: readString(raw, ['executedAt', 'executed_at']),
  }
}

function normalizeStringList(raw: unknown): string[] {
  const value = parseJsonValue(raw)
  if (Array.isArray(value)) {
    return normalizeStringArray(value)
  }
  if (typeof value === 'string') {
    return value
      .split(/\n|；|;/)
      .map((item) => item.replace(/^[-*\d.\s]+/, '').trim())
      .filter(Boolean)
  }
  if (isRecord(value)) {
    const nested = value.suggestions ?? value.recommendations ?? value.items ?? value.list ?? value.data ?? value.result ?? value.payload ?? value.body
    return nested == null ? normalizeStringArray(value) : normalizeStringList(nested)
  }
  if (typeof value === 'number') {
    return [String(value)]
  }
  return []
}

function normalizeScriptVariant(raw: unknown, index: number): ScriptVariant {
  const value = unwrapData(raw)
  if (!isRecord(value)) {
    return {
      id: `variant-${index + 1}`,
      content: String(value ?? ''),
    }
  }
  return {
    id: readString(value, ['id', 'variantId', 'uuid']) ?? `variant-${index + 1}`,
    content: readString(value, ['content', 'scriptContent', 'text', 'result']) ?? '',
    score: readOptionalNumber(value, ['score', 'qualityScore', 'rating']),
    keyPoints: readString(value, ['keyPoints', 'keyPoint', 'summary', 'reason']),
    likes: readOptionalNumber(value, ['likes', 'likeCount']),
    uses: readOptionalNumber(value, ['uses', 'useCount', 'usageCount']),
  }
}

function normalizeGenerationResult(raw: unknown): ScriptGenerationResult {
  const value = unwrapData(raw)
  const record = isRecord(value) ? value : {}
  const variantsRaw = record.variants ?? record.variantList ?? record.list ?? record.records ?? record.items ?? record.content ?? value
  const variants = normalizeResponseArray<unknown>(variantsRaw).map(normalizeScriptVariant)
  return {
    id: readNumber(value, ['id', 'generationId'], 0),
    variants,
    generationTime: readOptionalNumber(value, ['generationTime', 'generationTimeMs', 'elapsedMs', 'costMs', 'durationMs']),
  }
}

function normalizeOptimizationResult(raw: unknown): ScriptOptimizationResult {
  const value = unwrapData(raw)
  if (!isRecord(value)) {
    return {
      optimizedContent: String(value ?? ''),
      suggestions: [],
    }
  }
  return {
    optimizedContent: readString(value, ['optimizedContent', 'content', 'result', 'text', 'optimized']) ?? '',
    originalScore: readOptionalNumber(value, ['originalScore', 'beforeScore']),
    optimizedScore: readOptionalNumber(value, ['optimizedScore', 'afterScore', 'score']),
    suggestions: normalizeStringList(value.suggestions ?? value.recommendations ?? value.suggestion),
    generationTime: readOptionalNumber(value, ['generationTime', 'generationTimeMs', 'elapsedMs', 'costMs', 'durationMs']),
    style: readString(value, ['style']),
    goal: readString(value, ['goal']),
  }
}

export const scriptApi = {
  list: (p: ScriptQuery) => request.post<unknown>('/script/list', normalizeScriptQuery(p)).then(raw => normalizeResponsePage(raw, normalizeScriptItem, p.page ?? 0, p.rows ?? 20)),
  get: (id: number) => request.post<ScriptItem>('/script/get', undefined, idParam(id)),
  save: (p: Partial<ScriptSave>) => request.post<void>('/script/save', normalizeScriptSave(p)),
  delete: (id: number) => request.post<void>('/script/delete', undefined, idParam(id)),
  generate: (p: ScriptGenerationRequest) => request.post<unknown>('/script/generate', p).then(normalizeGenerationResult),
  optimize: (p: ScriptOptimizationRequest) =>
    request.post<unknown>('/script/optimize', compactPayload({
      originalContent: p.originalContent,
      goal: p.goal,
      style: p.style,
    })).then(normalizeOptimizationResult),
  updateUseCount: (id: number) => request.post<void>('/script/use-count', undefined, idParam(id)),
  categories: () => request.post<string[]>('/script/categories', {}),

  templateSearch: (p: ScriptTemplateQuery) => request.post<unknown>('/script/template/search', normalizeTemplateSearch(p)).then(raw => normalizeResponsePage(raw, normalizeScriptTemplate, p.page ?? 0, p.rows ?? 20)),
  templateGet: (id: number) => request.post<ScriptTemplate>('/script/template/get', undefined, idParam(id)),
  templateSave: (p: Partial<ScriptTemplate>) => request.post<number>('/script/template/save', normalizeTemplateSave(p)),
  templateDelete: (id: number) => request.post<void>('/script/template/delete', undefined, idParam(id)),
  templateByScene: (scene: string) => request.post<ScriptTemplate[]>('/script/template/by-scene', { scene }),

  violationList: (p: ViolationWordQuery) => request.post<unknown>('/script/admin/violation/list', normalizeViolationQuery(p)).then(raw => normalizeResponsePage(raw, normalizeViolationWord, p.page ?? 0, p.rows ?? 20)),
  violationSave: (p: Partial<ViolationWordSave>) => request.post<void>('/script/admin/violation/save', normalizeViolationSave(p)),
  violationDelete: (id: number) => request.post<void>('/script/admin/violation/delete', undefined, idParam(id)),
  violationPublicList: (p: ViolationWordQuery) => request.post<unknown>('/script/violation/public/list', normalizeViolationQuery(p)).then(raw => normalizeResponsePage(raw, normalizeViolationWord, p.page ?? 0, p.rows ?? 20)),
  violationCheck: (text: string, scope?: string) =>
    request.post<unknown>('/script/violation/check', scope ? { text, scope } : { text }).then(normalizeViolationCheckResult),
  violationCheckBatch: (items: Array<string | { key: string; text: string }>, scope = 'all') =>
    request.post<ViolationCheckBatchApiResult | BatchCheckRow[]>('/script/violation/check-batch', {
      texts: items.map((item, index) => typeof item === 'string' ? { key: String(index + 1), text: item } : item),
      scope,
    }).then(normalizeBatchRows),
  violationSuggestReplacement: (word: string) => request.post<{ replacement: string }>('/script/violation/suggest-replacement', { word }),

  complianceCheck: (text: string) => request.post<Record<string, unknown>>('/script/compliance/check', { text }),
  complianceRules: () => request.post<Record<string, unknown>[]>('/script/compliance/rules', {}),

  searchHybrid: (p: ScriptHybridSearchQuery) =>
    request.post<unknown>('/script/search/hybrid', normalizeHybridPayload(p)).then(normalizeHybridSearchResult),
  searchSemantic: (p: Record<string, unknown>) => request.post<unknown>('/script/search/semantic', p).then(raw => normalizeResponsePage(raw, normalizeScriptItem, Number(p.page ?? 0), Number(p.rows ?? 20))),
  searchSuggest: (keyword: string) => request.post<SearchSuggestionApiResult | string[]>('/script/search/suggest', { prefix: keyword, limit: 10 }).then(normalizeSuggestions),
}

