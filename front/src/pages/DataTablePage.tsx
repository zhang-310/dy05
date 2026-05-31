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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
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

const DATATABLE_UNSUPPORTED_ACTIONS = [
  'direct-api-call',
  'local-row-fallback',
  'server-export',
  'implicit-mutation',
]

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

interface NormalizedTableResult {
  total: number
  list: Record<string, unknown>[]
  issue?: string
}

function normalizeTableResult(result: unknown): NormalizedTableResult {
  if (Array.isArray(result)) {
    return { total: result.length, list: result as Record<string, unknown>[] }
  }
  if (result && typeof result === 'object') {
    const data = result as { total?: unknown; list?: unknown }
    const list = Array.isArray(data.list) ? data.list as Record<string, unknown>[] : []
    const total = Number(data.total ?? list.length)
    return {
      total: Number.isFinite(total) ? total : list.length,
      list,
      issue: Array.isArray(data.list) ? undefined : '列表接口未返回 list 数组，已降级为空列表。',
    }
  }
  return { total: 0, list: [], issue: '列表接口返回非对象结构，已降级为空列表。' }
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
  const [dataIssue, setDataIssue] = useState('')
  const { enqueueSnackbar } = useSnackbar()
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [selected, setSelected] = useState<Set<unknown>>(new Set())
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false)
  const [batchDeleting, setBatchDeleting] = useState(false)
  const [batchDeleteError, setBatchDeleteError] = useState('')

  const loadData = useCallback(() => {
    setLoading(true)
    setError('')
    setDataIssue('')
    fetchData({ page, rows: rowsPerPage, keyword: keyword || undefined })
      .then((res) => {
        const normalized = normalizeTableResult(res)
        setData(normalized.list)
        setTotal(normalized.total)
        setDataIssue(normalized.issue ?? '')
        setSelected(new Set())
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : '加载失败')
        setData([])
        setTotal(0)
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
  const selectedIds = Array.from(selected)
  const headerCellSx = {
    bgcolor: (theme: import('@mui/material/styles').Theme) =>
      theme.palette.mode === 'dark' ? theme.palette.background.default : theme.palette.grey[50],
    fontWeight: 600,
  }

  const handleConfirmBatchDelete = async () => {
    if (!onBatchDelete || selectedIds.length === 0) return
    setBatchDeleting(true)
    setBatchDeleteError('')
    try {
      await onBatchDelete(selectedIds)
      setSelected(new Set())
      setBatchDeleteOpen(false)
      enqueueSnackbar(`已删除 ${selectedIds.length} 条记录`, { variant: 'success' })
      loadData()
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e)
      setBatchDeleteError(message)
      enqueueSnackbar(`批量删除失败: ${message}`, { variant: 'error' })
    } finally {
      setBatchDeleting(false)
    }
  }

  const actionsContent = (
    <Box
      sx={{ display: 'flex', gap: 1, alignItems: 'center' }}
      data-testid="datatable-actions-surface"
      data-contract-source="props-actions"
      data-has-add={String(Boolean(onAdd))}
      data-has-batch-delete={String(Boolean(onBatchDelete))}
      data-selected-count={selected.size}
      data-no-direct-api="true"
    >
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
              data-testid="datatable-client-export-button"
              data-contract-source="client-csv-current-page"
              data-disabled-reason={data.length === 0 ? 'empty-current-page' : 'none'}
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
          onClick={() => { setBatchDeleteError(''); setBatchDeleteOpen(true) }}
          data-testid="datatable-batch-delete-open-button"
          data-contract-source="onBatchDelete-prop"
          data-selected-count={selected.size}
        >
          批量删除 ({selected.size})
        </Button>
      )}
      {onAdd && (
        <Button
          size="small"
          variant="contained"
          startIcon={<AddIcon />}
          onClick={onAdd}
          data-testid="datatable-add-button"
          data-contract-source="onAdd-prop"
        >
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
            <Box
              data-testid="datatable-live-filter-contract"
              data-contract-source="fetchData-prop"
              data-keyword={searchInput.trim() || 'empty'}
              data-applied-keyword={keyword || 'empty'}
              data-page={page}
              data-page-size={rowsPerPage}
              data-no-direct-api="true"
              sx={{ display: 'contents' }}
            />
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
        <Box
          data-testid="datatable-default-filter-contract"
          data-contract-source="fetchData-prop"
          data-keyword={searchInput.trim() || 'empty'}
          data-applied-keyword={keyword || 'empty'}
          data-page={page}
          data-page-size={rowsPerPage}
          data-no-direct-api="true"
          sx={{ display: 'contents' }}
        />
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
    <Box
      data-testid="datatable-page-shell"
      data-contract-scope="shared-data-table-props-grid"
      data-contract-source="fetchData-prop"
      data-toolbar-variant={toolbarVariant}
      data-ready-actions="fetchData|search|reset|refresh|select|client-export|add|edit|delete|batch-delete"
      data-unsupported-actions={DATATABLE_UNSUPPORTED_ACTIONS.join('|')}
      data-row-count={data.length}
      data-total-count={total}
      data-page={page}
      data-page-size={rowsPerPage}
      data-keyword={keyword || 'empty'}
      data-search-input={searchInput.trim() || 'empty'}
      data-selected-count={selected.size}
      data-loading={String(loading)}
      data-has-error={String(Boolean(error))}
      data-has-data-issue={String(Boolean(dataIssue))}
      data-no-direct-api="true"
      data-no-local-row-fallback="true"
      data-no-server-export="true"
    >
      {toolbarContent}

      {error && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setError('')}
          action={<Button color="inherit" size="small" onClick={loadData}>重试</Button>}
          data-testid="datatable-load-error"
          data-contract-source="fetchData-prop"
          data-no-local-row-fallback="true"
          data-filter-context={`keyword=${keyword || 'empty'};page=${page};rows=${rowsPerPage}`}
        >
          {error}
        </Alert>
      )}

      {dataIssue && (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          onClose={() => setDataIssue('')}
          data-testid="datatable-data-issue"
          data-contract-source="normalizeTableResult"
          data-no-local-row-fallback="true"
        >
          {dataIssue}
        </Alert>
      )}

      <Paper
        sx={{ overflow: 'hidden' }}
        data-testid="datatable-table-surface"
        data-contract-source="fetchData-prop"
        data-row-count={data.length}
        data-total-count={total}
        data-selected-count={selected.size}
        data-no-local-row-fallback="true"
      >
        {loading ? (
          <Box
            sx={{ p: 6, display: 'flex', justifyContent: 'center' }}
            data-testid="datatable-loading-state"
            data-contract-source="fetchData-prop"
          >
            <CircularProgress />
          </Box>
        ) : (
          <>
            <TableContainer sx={{ maxHeight: 'calc(100vh - 320px)' }}>
              <Table size="medium" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox" data-testid="datatable-header-cell" sx={headerCellSx}>
                      <Checkbox
                        indeterminate={selected.size > 0 && selected.size < data.length}
                        checked={allSelected}
                        onChange={(_, c) => handleSelectAll(c)}
                      />
                    </TableCell>
                    {columns.map((col) => (
                      <TableCell key={col.key} data-testid="datatable-header-cell" sx={{ ...headerCellSx, minWidth: col.width }}>
                        {col.label}
                      </TableCell>
                    ))}
                    {hasActions && (
                      <TableCell data-testid="datatable-header-cell" sx={{ ...headerCellSx, width: 80, textAlign: 'center' }}>
                        操作
                      </TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={columns.length + (hasActions ? 1 : 0) + 1}
                        align="center"
                        sx={{ py: 6, color: 'text.secondary' }}
                        data-testid="datatable-empty-state"
                        data-contract-source="fetchData-prop"
                        data-no-local-row-fallback="true"
                      >
                        暂无数据
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.map((row, index) => {
                      const rawId = row[idKey] ?? row.id
                      const rowId = (typeof rawId === 'number' || typeof rawId === 'string') ? rawId : `row-${index}`
                      const isSelectedRow = selected.has(rowId)
                      return (
                        <TableRow
                          key={String(rowId)}
                          hover
                          selected={isSelectedRow}
                          data-testid="datatable-body-row"
                          data-row-id={String(rowId)}
                          data-selected={String(isSelectedRow)}
                          data-contract-source="fetchData-prop"
                        >
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
                                  <IconButton
                                    size="small"
                                    onClick={() => onEdit(row)}
                                    data-testid="datatable-row-edit-button"
                                    data-contract-source="onEdit-prop"
                                  >
                                    <EditIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              )}
                              {onDelete && (
                                <Tooltip title="删除">
                                  <IconButton
                                    size="small"
                                    color="error"
                                    onClick={() => onDelete(row)}
                                    data-testid="datatable-row-delete-button"
                                    data-contract-source="onDelete-prop"
                                  >
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

      <Dialog
        open={batchDeleteOpen}
        onClose={() => !batchDeleting && setBatchDeleteOpen(false)}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          'data-testid': 'datatable-batch-delete-dialog',
          'data-contract-source': 'onBatchDelete-prop',
          'data-selected-count': selectedIds.length,
          'data-loading': String(batchDeleting),
          'data-input-retained': batchDeleteError ? 'true' : 'false',
          'data-no-direct-api': 'true',
        } as never}
      >
        <DialogTitle>批量删除</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            确定删除选中的 {selectedIds.length} 条记录吗？删除后会自动刷新列表。
          </Typography>
          {batchDeleteError && (
            <Alert
              severity="error"
              sx={{ mt: 2 }}
              data-testid="datatable-batch-delete-error"
              data-contract-source="onBatchDelete-prop"
              data-selected-retained="true"
            >
              批量删除失败：{batchDeleteError}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBatchDeleteOpen(false)} disabled={batchDeleting} data-testid="datatable-batch-delete-cancel-button">取消</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleConfirmBatchDelete}
            disabled={batchDeleting}
            data-testid="datatable-batch-delete-confirm-button"
            data-disabled-reason={batchDeleting ? 'batch-delete-pending' : 'none'}
          >
            {batchDeleting ? '删除中...' : '确认删除'}
          </Button>
        </DialogActions>
      </Dialog>
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
