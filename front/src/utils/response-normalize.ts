import type { PageResult } from '@/types/common'

type UnknownRecord = Record<string, unknown>

export function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function parseJsonValue(raw: unknown): unknown {
  if (typeof raw !== 'string') return raw
  const text = raw.trim()
  if (!text || (!text.startsWith('{') && !text.startsWith('['))) return raw
  try {
    return JSON.parse(text) as unknown
  } catch {
    return raw
  }
}

export function readNumberValue(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

export function normalizeArray<T>(raw: unknown): T[] {
  const value = parseJsonValue(raw)
  if (Array.isArray(value)) return value as T[]
  if (isRecord(value)) {
    for (const key of [
      'list',
      'records',
      'items',
      'rows',
      'content',
      'data',
      'result',
      'detail',
      'record',
      'item',
      'payload',
      'body',
      'values',
      'sessions',
      'hotTopics',
      'files',
      'members',
      'talents',
      'reviews',
      'reviewTasks',
      'organizations',
      'orgs',
      'accounts',
      'videos',
      'cookies',
      'scripts',
      'analysis',
      'tasks',
      'projects',
      'trend',
      'trendPoints',
      'dashboardProjects',
      'audios',
      'history',
      'shots',
      'titles',
      'titleSuggestions',
      'platformResults',
      'topics',
      'users',
      'roles',
      'resources',
      'loginLogs',
      'configs',
      'sysConfigs',
      'storageFiles',
      'points',
      'series',
      'metrics',
      'samples',
      'slowQueries',
      'queries',
      'apiLogs',
      'syncLogs',
      'alertRecords',
      'alerts',
      'externalApis',
      'providers',
      'approvals',
      'pendingApprovals',
      'pendingReviews',
      'liveSessions',
      'slangEntries',
      'entries',
      'accountCollectTasks',
      'collectTasks',
    ] as const) {
      if (key in value) {
        const rows = normalizeArray<T>(value[key])
        if (rows.length > 0 || value[key] == null || Array.isArray(value[key])) {
          return rows
        }
      }
    }
  }
  return []
}

export function normalizeRecord(raw: unknown, businessKeys: string[] = []): UnknownRecord {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) return {}
  const wrapperKeys = ['status', 'message', 'msg', 'code', 'data', 'detail', 'record', 'item', 'result', 'payload', 'body', 'success', 'traceId', 'timestamp']
  const keys = Object.keys(value)
  const isWrapperObject = keys.length > 0 && keys.every((key) => wrapperKeys.includes(key))
  for (const nestedKey of ['data', 'detail', 'record', 'item', 'result', 'payload', 'body'] as const) {
    if (isWrapperObject && isRecord(value[nestedKey]) && !Array.isArray(value[nestedKey])) {
      const nested = normalizeRecord(value[nestedKey], businessKeys)
      if (Object.keys(nested).length > 0) return nested
    }
  }
  for (const key of businessKeys) {
    if (isRecord(value[key]) && !Array.isArray(value[key])) {
      const nested = normalizeRecord(value[key], businessKeys)
      if (Object.keys(nested).length > 0) return nested
    }
  }
  return value
}

export function normalizeStringArray(raw: unknown): string[] {
  const value = parseJsonValue(raw)
  if (Array.isArray(value)) {
    return value
      .map((item) => {
        const parsedItem = parseJsonValue(item)
        if (typeof parsedItem === 'string' || typeof parsedItem === 'number') return String(parsedItem).trim()
        if (isRecord(parsedItem)) {
          for (const key of ['label', 'value', 'text', 'title', 'keyword', 'tag', 'name', 'content', 'result'] as const) {
            const text = parsedItem[key]
            if (typeof text === 'string' || typeof text === 'number') return String(text).trim()
          }
        }
        return ''
      })
      .filter(Boolean)
  }

  if (isRecord(value)) {
    for (const key of ['list', 'records', 'items', 'rows', 'content', 'data', 'result', 'detail', 'record', 'item', 'values', 'sessions', 'tags', 'titles'] as const) {
      if (key in value) {
        const rows = normalizeStringArray(value[key])
        if (rows.length > 0 || value[key] == null || Array.isArray(value[key])) {
          return rows
        }
      }
    }
  }

  if (typeof value === 'string' || typeof value === 'number') {
    const text = String(value).trim()
    return text ? [text] : []
  }

  return []
}

export function readNumber(raw: unknown, keys: string[], fallback: number): number {
  const value = parseJsonValue(raw)
  if (!isRecord(value)) return fallback
  for (const key of keys) {
    const n = Number(value[key])
    if (Number.isFinite(n)) return n
  }
  for (const nestedKey of ['data', 'result', 'detail', 'record', 'item', 'payload', 'body', 'page'] as const) {
    if (value[nestedKey] != null) {
      const nested = readNumber(value[nestedKey], keys, Number.NaN)
      if (Number.isFinite(nested)) return nested
    }
  }
  return fallback
}

export function readTotal(raw: unknown, fallback: number): number {
  return readNumber(raw, ['total', 'totalElements', 'count', 'totalCount', 'totalRecords'], fallback)
}

export function normalizePage<T, R>(
  raw: unknown,
  mapper: (item: T) => R,
  requestedPage = 0,
  requestedRows = 20,
): PageResult<R> {
  const list = normalizeArray<T>(raw).map(mapper)
  return {
    total: readTotal(raw, list.length),
    list,
    pageNum: readNumber(raw, ['pageNum', 'page', 'pageNumber', 'current'], requestedPage),
    pageSize: readNumber(raw, ['pageSize', 'size', 'rows'], requestedRows),
  }
}

export function normalizeRows<T>(raw: unknown): T[] {
  return normalizeArray<T>(raw)
}
