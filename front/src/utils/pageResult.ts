import type { PageResult } from '@/types/common'

export function emptyPageResult<T>(): PageResult<T> {
  return { total: 0, list: [], pageNum: 0, pageSize: 30 }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function toNumber(value: unknown, fallback: number): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return fallback
}

export function normalizePageResult<T>(data: unknown): PageResult<T> {
  if (!isRecord(data)) {
    return emptyPageResult<T>()
  }

  const list = Array.isArray(data.list)
    ? data.list
    : Array.isArray(data.records)
      ? data.records
      : []

  return {
    total: toNumber(data.total, 0),
    list: list as T[],
    pageNum: toNumber(data.pageNum, toNumber(data.current, 0)),
    pageSize: toNumber(data.pageSize, toNumber(data.size, 30)),
  }
}
