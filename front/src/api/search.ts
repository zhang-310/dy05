import request from '@/utils/request'

export interface VectorSearchRequest {
  query: string
  searchType: 'semantic' | 'keyword' | 'hybrid'
  topK: number
  filters?: {
    moduleType?: string
    ownerIds?: number[]
    createdAfter?: string
    createdBefore?: string
  }
  useReranking?: boolean
  similarityThreshold?: number
}

export interface SearchResultItem {
  id: number
  title: string
  content: string
  moduleType: 'script' | 'product' | 'shortvideo' | 'live'
  moduleId: number
  ownerUserId: number
  semanticScore: number
  keywordScore: number
  finalScore: number
  createdAt: string
  updatedAt: string
}

export function hybridSearch(params: VectorSearchRequest) {
  return request.post<{ total: number; results: SearchResultItem[]; executionTimeMs: number }>(
    '/ai/search/hybrid',
    params
  )
}

export function getSearchSuggestions(prefix: string, limit = 10) {
  return request.post<{ suggestions: { text: string; type: string }[] }>(
    '/ai/search/suggestions',
    { prefix, limit }
  )
}

export function getTrendingSearches(moduleType?: string, topN = 20) {
  return request.post<{ trending: { query: string; count: number }[] }>(
    '/ai/search/trending',
    { moduleType, topN }
  )
}

export function getSearchHistory(limit = 50) {
  return request.post<{ history: { query: string; timestamp: string }[] }>(
    '/ai/search/history',
    { limit }
  )
}

export function clearSearchHistory() {
  return request.post<void>('/ai/search/history/clear', {})
}

export function recordSearch(query: string, moduleType?: string, resultCount?: number) {
  return request.post<void>('/ai/search/record', { query, moduleType, resultCount })
}
