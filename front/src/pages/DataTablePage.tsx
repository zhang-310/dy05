import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Paper,
  CircularProgress,
  Alert,
  TextField,
  InputAdornment,
  Button,
  IconButton,
  Tooltip,
  Chip,
  Checkbox,
  Card,
  CardContent,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import RefreshIcon from '@mui/icons-material/Refresh'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import DownloadIcon from '@mui/icons-material/Download'
import { useSnackbar } from 'notistack'
import { PageHeader, FilterPanel } from '@/components/base'

export interface ColumnDef {
  key: string
  label: string
  width?: number
  /** 作为状态 Chip 渲染（如 status、roleCode） */
  asChip?: boolean
  /** Chip 颜色映射 */
  chipColorMap?: Record<string, 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'>
}

interface DataTablePageProps {
  title: string
  /** 副标题（与 toolbarVariant='live' 配合使用） */
  subtitle?: string
  /** 工具栏风格：live=直播场次风格（PageHeader+Card+FilterPanel） */
  toolbarVariant?: 'live' | 'default'
  fetchData: (params: { page?: number; rows?: number; keyword?: string }) => Promise<{ total: number; list: Record<string, unknown>[] }>
  columns: ColumnDef[]
  searchPlaceholder?: string
  onAdd?: () => void
  onEdit?: (row: Record<string, unknown>) => void
  onDelete?: (row: Record<string, unknown>) => void
  idKey?: string
  /** 顶部操作（如导出 PDF） */
  topActions?: React.ReactNode
  /** 批量删除（需配合 onDelete 使用） */
  onBatchDelete?: (ids: unknown[]) => Promise<void>
}

export function DataTablePage({
  title,
  subtitle,
  toolbarVariant = 'default',
  fetchData,
  columns,
  searchPlaceholder = '搜索...',
  onAdd,
  onEdit,
  onDelete,
  idKey = 'id',
  topActions,
  onBatchDelete,
}: DataTablePageProps) {
  const [data, setData] = useState<Record<string, unknown>[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const { enqueueSnackbar } = useSnackbar()
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

  const handleSearch = () => {
    setKeyword(searchInput.trim())
    setPage(0)
  }

  const handleReset = () => {
    setSearchInput('')
    setKeyword('')
    setPage(0)
  }

  const handleRefresh = () => {
    loadData()
  }

  const handleSelectAll = (checked: boolean) => {
    if (checked) setSelected(new Set(data.map((r) => r[idKey])))
    else setSelected(new Set())
  }

  const handleSelectOne = (id: unknown, checked: boolean) => {
    const next = new Set(selected)
    if (checked) next.add(id)
    else next.delete(id)
    setSelected(next)
  }

  const hasActions = onAdd || onEdit || onDelete
  const allSelected = data.length > 0 && selected.size === data.length

  const actionsContent = (
    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
      {topActions}
      {toolbarVariant === 'default' && (
        <Tooltip title="导出 CSV">
          <span>
            <IconButton
              onClick={() => {
                const header = columns.map((c) => c.label).join(',')
                const rows = data.map((r) => columns.map((c) => `"${String(r[c.key] ?? '').replace(/"/g, '""')}"`).join(','))
                const csv = [header, ...rows].join('\n')
                const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
                const a = document.createElement('a')
                a.href = URL.createObjectURL(blob)
                a.download = `${title}-${new Date().toISOString().slice(0, 10)}.csv`
                a.click()
                URL.revokeObjectURL(a.href)
              }}
              disabled={data.length === 0}
            >
              <DownloadIcon />
            </IconButton>
          </span>
        </Tooltip>
      )}
      {onBatchDelete && selected.size > 0 && (
        <Button
          variant="outlined"
          color="error"
          size="small"
          onClick={() => {
            const ids = Array.from(selected)
            if (ids.length && confirm(`确定删除选中的 ${ids.length} 条记录？`)) {
              onBatchDelete(ids).then(() => {
                setSelected(new Set())
                loadData()
              }).catch((e) => enqueueSnackbar(`批量删除失败: ${e instanceof Error ? e.message : String(e)}`, { variant: 'error' }))
            }
          }}
        >
          批量删除 ({selected.size})
        </Button>
      )}
      {onAdd && (
        <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={onAdd}>
          新增
        </Button>
      )}
    </Box>
  )

  const toolbarContent = toolbarVariant === 'live' ? (
    <>
      <PageHeader
        title={title}
        subtitle={subtitle}
        actions={actionsContent}
      />
      <Card sx={{ mb: 2 }}>
        <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
          <FilterPanel onSearch={handleSearch} onReset={handleReset}>
            <TextField
              size="small"
              label="关键词"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              placeholder={searchPlaceholder}
              sx={{ minWidth: 180 }}
            />
            <Button size="small" variant="contained" startIcon={<SearchIcon />} onClick={handleSearch}>
              搜索
            </Button>
            <Button size="small" variant="outlined" onClick={handleReset}>
              重置
            </Button>
            <Tooltip title="刷新">
              <IconButton size="small" onClick={handleRefresh} disabled={loading}>
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          </FilterPanel>
        </CardContent>
      </Card>
    </>
  ) : (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2, mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600, fontSize: 24 }}>
          {title}
        </Typography>
        {actionsContent}
      </Box>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2, alignItems: 'center' }}>
        <TextField
          placeholder={searchPlaceholder}
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          sx={{ minWidth: 240 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" sx={{ color: 'text.secondary' }} />
              </InputAdornment>
            ),
          }}
        />
        <Button variant="outlined" onClick={handleSearch}>
          搜索
        </Button>
        <Tooltip title="刷新">
          <span>
            <IconButton onClick={handleRefresh} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </span>
        </Tooltip>
      </Box>
    </>
  )

  return (
    <Box>
      {toolbarContent}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Paper sx={{ overflow: 'hidden' }}>
        {loading ? (
          <Box sx={{ p: 6, display: 'flex', justifyContent: 'center' }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <TableContainer sx={{ maxHeight: 'calc(100vh - 320px)' }}>
              <Table size="medium" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox" sx={{ bgcolor: 'grey.50', fontWeight: 600 }}>
                      <Checkbox
                        indeterminate={selected.size > 0 && selected.size < data.length}
                        checked={allSelected}
                        onChange={(_, c) => handleSelectAll(c)}
                      />
                    </TableCell>
                    {columns.map((col) => (
                      <TableCell key={col.key} sx={{ fontWeight: 600, bgcolor: 'grey.50', minWidth: col.width }}>
                        {col.label}
                      </TableCell>
                    ))}
                    {hasActions && (
                      <TableCell sx={{ fontWeight: 600, bgcolor: 'grey.50', width: 80, textAlign: 'center' }}>
                        操作
                      </TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={columns.length + (hasActions ? 1 : 0) + 1} align="center" sx={{ py: 6, color: 'text.secondary' }}>
                        暂无数据
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.map((row, index) => {
                      const rawId = row[idKey] ?? row.id
                      const rowId = (typeof rawId === 'number' || typeof rawId === 'string') ? rawId : `row-${index}`
                      const isSelectedRow = selected.has(rowId)
                      return (
                        <TableRow key={String(rowId)} hover selected={isSelectedRow}>
                          <TableCell padding="checkbox">
                            <Checkbox checked={isSelectedRow} onChange={(_, c) => handleSelectOne(rowId, c)} />
                          </TableCell>
                          {columns.map((col) => (
                            <TableCell key={col.key} sx={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {col.asChip && col.chipColorMap ? (
                                <Chip
                                  label={formatCellValue(row[col.key])}
                                  size="small"
                                  color={col.chipColorMap[String(row[col.key])] ?? 'default'}
                                />
                              ) : (
                                formatCellValue(row[col.key])
                              )}
                            </TableCell>
                          ))}
                          {hasActions && (
                            <TableCell sx={{ textAlign: 'center' }}>
                              {onEdit && (
                                <Tooltip title="编辑">
                                  <IconButton size="small" onClick={() => onEdit(row)}>
                                    <EditIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              )}
                              {onDelete && (
                                <Tooltip title="删除">
                                  <IconButton size="small" color="error" onClick={() => onDelete(row)}>
                                    <DeleteIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              )}
                            </TableCell>
                          )}
                        </TableRow>
                      )
                    })
                  )}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination
              component="div"
              count={total}
              page={page}
              onPageChange={(_, p) => setPage(p)}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={(e) => {
                setRowsPerPage(parseInt(e.target.value, 10))
                setPage(0)
              }}
              rowsPerPageOptions={[10, 25, 50, 100]}
              labelRowsPerPage="每页"
              labelDisplayedRows={({ from, to, count }) => `${from}-${to} / 共 ${count} 条`}
              sx={{ borderTop: '1px solid', borderColor: 'divider' }}
            />
          </>
        )}
      </Paper>
    </Box>
  )
}

function formatCellValue(val: unknown): string {
  if (val == null) return '-'
  if (typeof val === 'object') {
    interface CellObj { createTime?: unknown; [key: string]: unknown }
    const obj = val as CellObj
    const t = obj.createTime
    if (typeof t === 'string') return t.length > 19 ? t.slice(0, 19) : t
  }
  const s = String(val)
  return s.length > 80 ? s.slice(0, 80) + '...' : s
}
