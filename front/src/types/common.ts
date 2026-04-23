export interface RESTResult<T = unknown> {
  status: number
  message: string
  data: T
  traceId?: string
  timestamp?: string
}

export interface PageResult<T> {
  total: number
  list: T[]
  pageNum: number
  pageSize: number
}

export interface BasicQuery {
  page?: number
  rows?: number
  sortName?: string
  sortOrder?: 'asc' | 'desc'
}
