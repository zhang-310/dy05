import { useState, useEffect, useCallback } from 'react'

export type DataTableRow = Record<string, unknown>

export interface DataTableFetchParams {
  page?: number
  rows?: number
  keyword?: string
}

export interface DataTableFetchResult<T extends DataTableRow> {
  total: number
  list: T[]
}

export interface UseDataTableParams<T extends DataTableRow = DataTableRow> {
  /** 获取分页数据的函数 */
  fetchData: (params: DataTableFetchParams) => Promise<DataTableFetchResult<T>>
  /** 初始页码（0-indexed） */
  initialPage?: number
  /** 初始每页行数 */
  initialRowsPerPage?: number
  /** 行主键字段名 */
  idKey?: string
}

export interface UseDataTableReturn<T extends DataTableRow = DataTableRow> {
  data: T[]
  total: number
  loading: boolean
  error: string
  setError: (s: string) => void
  page: number
  setPage: (p: number) => void
  rowsPerPage: number
  setRowsPerPage: (n: number) => void
  keyword: string
  searchInput: string
  setSearchInput: (s: string) => void
  selected: Set<unknown>
  setSelected: (s: Set<unknown>) => void
  loadData: () => void
  handleSearch: () => void
  handleReset: () => void
  handleRefresh: () => void
  handleSelectAll: (checked: boolean) => void
  handleSelectOne: (id: unknown, checked: boolean) => void
  idKey: string
  allSelected: boolean
}

function getRowSelectionValue(row: DataTableRow, idKey: string): unknown {
  return row[idKey] ?? row.id
}

/**
 * 通用表格分页 + 搜索 + 批量选择 hook
 * 用于 DataTablePage 及需要自定义表格 UI 的页面
 */
export function useDataTable<T extends DataTableRow = DataTableRow>({
  fetchData,
  initialPage = 0,
  initialRowsPerPage = 10,
  idKey = 'id',
}: UseDataTableParams<T>): UseDataTableReturn<T> {
  const [data, setData] = useState<T[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(initialPage)
  const [rowsPerPage, setRowsPerPage] = useState(initialRowsPerPage)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [selected, setSelected] = useState<Set<unknown>>(new Set())

  const loadData = useCallback(() => {
    setLoading(true)
    setError('')
    fetchData({ page, rows: rowsPerPage, keyword: keyword || undefined })
      .then((res) => {
        setData(res.list || [])
        setTotal(res.total || 0)
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : '加载失败')
      })
      .finally(() => {
        setLoading(false)
      })
  }, [fetchData, page, rowsPerPage, keyword])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = useCallback(() => {
    setKeyword(searchInput.trim())
    setPage(0)
  }, [searchInput])

  const handleReset = useCallback(() => {
    setSearchInput('')
    setKeyword('')
    setPage(0)
  }, [])

  const handleRefresh = useCallback(() => {
    loadData()
  }, [loadData])

  const handleSelectAll = useCallback(
    (checked: boolean) => {
      if (checked) setSelected(new Set(data.map((row) => getRowSelectionValue(row, idKey))))
      else setSelected(new Set())
    },
    [data, idKey]
  )

  const handleSelectOne = useCallback((id: unknown, checked: boolean) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }, [])

  const allSelected = data.length > 0 && selected.size === data.length

  return {
    data,
    total,
    loading,
    error,
    setError,
    page,
    setPage,
    rowsPerPage,
    setRowsPerPage,
    keyword,
    searchInput,
    setSearchInput,
    selected,
    setSelected,
    loadData,
    handleSearch,
    handleReset,
    handleRefresh,
    handleSelectAll,
    handleSelectOne,
    idKey,
    allSelected,
  }
}
