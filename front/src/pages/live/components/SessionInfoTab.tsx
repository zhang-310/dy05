import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  List,
  ListItemButton,
  ListItemText,
  Alert,
  CircularProgress,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import {
  saveLiveProduct,
  deleteLiveProduct,
  batchSortProducts,
} from '@/api/live'
import { searchProducts } from '@/api/product'
import { normalizePageResult } from '@/utils/pageResult'

function formatDate(val: unknown): string {
  if (val == null) return '-'
  const s = String(val)
  if (s.length >= 16) return s.slice(0, 16).replace('T', ' ')
  return s
}

function formatMoney(val: unknown): string {
  if (val == null) return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

interface ProductSearchRow {
  id: number
  productName?: string
  price?: number
  [key: string]: unknown
}

interface SessionInfoTabProps {
  products: Record<string, unknown>[]
  sessionId: number
  onRefresh: () => void
  toast: (msg: string, severity?: 'success' | 'error' | 'info' | 'warning') => void
}

interface DeleteConfirmData {
  id: number
  productName?: string
  [key: string]: unknown
}

const READY_ENDPOINTS = '/live/product/save|/live/product/delete|/live/product/batch-sort|/product/search'
const UNSUPPORTED_ACTIONS = 'local-product-library-fallback|direct-script-update|direct-session-update'

export function SessionInfoTab({
  products,
  sessionId,
  onRefresh,
  toast,
}: SessionInfoTabProps) {
  const [addOpen, setAddOpen] = useState(false)
  const [productList, setProductList] = useState<ProductSearchRow[]>([])
  const [productSearch, setProductSearch] = useState('')
  const [addLoading, setAddLoading] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<'add' | 'delete' | 'sort' | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<DeleteConfirmData | null>(null)

  const addedProductIds = new Set(products.map((p) => p.productId as number).filter(Boolean))

  const loadProductList = useCallback(() => {
    setAddLoading(true)
    setAddError(null)
    searchProducts({ page: 0, rows: 50, productName: productSearch || undefined })
      .then((res) => {
        const { list } = normalizePageResult(res)
        setProductList((list ?? []) as ProductSearchRow[])
      })
      .catch((e) => {
        setProductList([])
        setAddError(e instanceof Error ? e.message : '商品库搜索失败')
      })
      .finally(() => setAddLoading(false))
  }, [productSearch])

  useEffect(() => {
    if (addOpen) loadProductList()
  }, [addOpen, loadProductList])

  const handleAddProduct = async (product: Record<string, unknown>) => {
    const productId = product.id as number
    if (!productId) return
    const maxPos = products.reduce((m, p) => Math.max(m, (p.position as number) ?? 0), 0)
    setActionLoading('add')
    try {
      await saveLiveProduct({
        sessionId,
        productId,
        productName: String(product.productName ?? ''),
        position: maxPos + 1,
      })
      toast('添加成功', 'success')
      setAddOpen(false)
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '添加失败', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const handleRemove = async () => {
    if (!deleteConfirm) return
    const id = deleteConfirm.id as number
    if (!id) return
    setActionLoading('delete')
    try {
      await deleteLiveProduct(id)
      toast('移除成功', 'success')
      setDeleteConfirm(null)
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '移除失败', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const handleMove = async (row: Record<string, unknown>, direction: 'up' | 'down') => {
    const sorted = [...products].sort((a, b) => ((a.position as number) ?? 0) - ((b.position as number) ?? 0))
    const idx = sorted.findIndex((p) => p.id === row.id)
    if (idx < 0) return
    const swapIdx = direction === 'up' ? idx - 1 : idx + 1
    if (swapIdx < 0 || swapIdx >= sorted.length) return
    const newOrder = [...sorted]
    ;[newOrder[idx], newOrder[swapIdx]] = [newOrder[swapIdx], newOrder[idx]]
    const productIds = newOrder.map((p) => p.id as number).filter(Boolean)
    setActionLoading('sort')
    try {
      await batchSortProducts({ sessionId, productIds })
      toast('排序已更新', 'success')
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '排序失败', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const sortedProducts = [...products].sort((a, b) => ((a.position as number) ?? 0) - ((b.position as number) ?? 0))
  const availableProducts = productList.filter((p) => !addedProductIds.has(p.id as number))

  return (
    <Box
      data-testid="session-info-tab-root"
      data-contract-scope="live-session-info-product-maintenance"
      data-contract-source="products-prop|sessionId-prop|onRefresh-prop|toast-prop"
      data-ready-endpoints={READY_ENDPOINTS}
      data-unsupported-actions={UNSUPPORTED_ACTIONS}
      data-product-count={products.length}
      data-session-id={sessionId}
      data-add-open={addOpen ? 'true' : 'false'}
      data-add-loading={addLoading ? 'true' : 'false'}
      data-action-loading={actionLoading ?? ''}
      data-no-local-product-library-fallback="true"
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="subtitle2" color="text.secondary">
          从商品库添加、排序、移除
        </Typography>
        <Button
          data-testid="session-info-add-open-button"
          data-contract-source="/product/search"
          size="small"
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setAddOpen(true)}
        >
          添加商品
        </Button>
      </Box>
      {products.length === 0 ? (
        <Typography
          data-testid="session-info-product-empty-state"
          data-contract-scope="live-session-info-products-empty"
          data-contract-source="products-prop"
          data-no-local-product-fallback="true"
          color="text.secondary"
          sx={{ py: 4, textAlign: 'center' }}
        >
          暂无选品，点击「添加商品」从商品库选择
        </Typography>
      ) : (
        <TableContainer
          data-testid="session-info-product-table"
          data-contract-source="products-prop"
          data-row-count={sortedProducts.length}
          component={Paper}
          variant="outlined"
        >
          <Table size="small">
            <TableHead>
              <TableRow
                data-testid="session-info-product-table-head-surface"
                sx={(theme) => ({
                  bgcolor: theme.palette.mode === 'dark'
                    ? alpha(theme.palette.common.white, 0.04)
                    : theme.palette.action.hover,
                })}
              >
                <TableCell sx={{ width: 100 }}>操作</TableCell>
                <TableCell>序号</TableCell>
                <TableCell>产品名称</TableCell>
                <TableCell>价格</TableCell>
                <TableCell>话术来源</TableCell>
                <TableCell>销售数量</TableCell>
                <TableCell>销售额</TableCell>
                <TableCell>添加时间</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sortedProducts.map((row, idx) => (
                <TableRow
                  key={String(row.id)}
                  data-testid="session-info-product-row"
                  data-product-id={String(row.productId ?? '')}
                  data-row-id={String(row.id ?? '')}
                  data-position={String(typeof row.position === 'number' ? row.position : idx + 1)}
                  data-script-source={String(row.scriptSource ?? '')}
                >
                  <TableCell>
                    <Tooltip title="上移">
                      <span>
                        <IconButton
                          data-testid="session-info-product-move-up-button"
                          data-contract-source="/live/product/batch-sort"
                          data-disabled-reason={actionLoading === 'sort' ? 'sorting' : idx === 0 ? 'first-row' : 'ready'}
                          size="small"
                          disabled={actionLoading === 'sort' || idx === 0}
                          onClick={() => handleMove(row, 'up')}
                        >
                          <ArrowUpwardIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="下移">
                      <span>
                        <IconButton
                          data-testid="session-info-product-move-down-button"
                          data-contract-source="/live/product/batch-sort"
                          data-disabled-reason={actionLoading === 'sort' ? 'sorting' : idx === sortedProducts.length - 1 ? 'last-row' : 'ready'}
                          size="small"
                          disabled={actionLoading === 'sort' || idx === sortedProducts.length - 1}
                          onClick={() => handleMove(row, 'down')}
                        >
                          <ArrowDownwardIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="移除">
                      <IconButton
                        data-testid="session-info-product-delete-open-button"
                        data-contract-source="/live/product/delete"
                        size="small"
                        color="error"
                        onClick={() => setDeleteConfirm({ id: row.id as number, productName: row.productName as string | undefined })}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                  <TableCell>{String(typeof row.position === 'number' ? row.position : idx + 1)}</TableCell>
                  <TableCell>{String(row.productName ?? '-')}</TableCell>
                  <TableCell>{row.price != null ? `¥${Number(row.price).toFixed(2)}` : '-'}</TableCell>
                  <TableCell>{row.scriptSource === 'product' ? '产品话术' : row.scriptSource === 'session' ? '本场生成' : '未设置'}</TableCell>
                  <TableCell>{String(row.saleQuantity ?? '-')}</TableCell>
                  <TableCell>{formatMoney(row.revenue)}</TableCell>
                  <TableCell>{formatDate(row.createTime)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog
        open={addOpen}
        onClose={() => setAddOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'session-info-add-product-dialog',
          'data-contract-scope': 'live-session-info-add-product-dialog',
          'data-contract-source': '/product/search|/live/product/save',
          'data-ready-endpoints': READY_ENDPOINTS,
          'data-product-count': productList.length,
          'data-available-count': availableProducts.length,
          'data-loading': addLoading ? 'true' : 'false',
          'data-error': addError ? 'true' : 'false',
          'data-no-local-product-library-fallback': 'true',
        } as Record<string, string | number>}
      >
        <DialogTitle>从商品库添加</DialogTitle>
        <DialogContent>
          <TextField
            inputProps={{
              'data-testid': 'session-info-product-search-input',
              'data-contract-source': '/product/search',
            }}
            size="small"
            fullWidth
            placeholder="搜索商品名称"
            value={productSearch}
            onChange={(e) => setProductSearch(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && loadProductList()}
            sx={{ mb: 2 }}
          />
          <Button
            data-testid="session-info-product-search-button"
            data-contract-source="/product/search"
            data-disabled-reason={addLoading ? 'search-loading' : 'ready'}
            size="small"
            onClick={loadProductList}
            disabled={addLoading}
          >
            {addLoading ? '加载中...' : '搜索'}
          </Button>
          {addError && (
            <Alert
              data-testid="session-info-product-search-error"
              data-contract-source="/product/search"
              data-no-local-product-library-fallback="true"
              severity="error"
              sx={{ mt: 2 }}
            >
              {addError}
            </Alert>
          )}
          <List
            data-testid="session-info-product-library-list"
            data-contract-source="/product/search"
            data-row-count={availableProducts.length}
            sx={{ maxHeight: 320, overflow: 'auto', mt: 2 }}
          >
            {availableProducts
              .map((p) => (
                <ListItemButton
                  data-testid="session-info-product-library-row"
                  data-product-id={p.id}
                  data-disabled-reason={actionLoading === 'add' ? 'add-loading' : 'ready'}
                  key={String(p.id)}
                  disabled={actionLoading === 'add'}
                  onClick={() => handleAddProduct(p)}
                >
                  <ListItemText
                    primary={String(p.productName ?? p.id)}
                    secondary={p.price != null ? `¥${Number(p.price).toFixed(2)}` : undefined}
                  />
                </ListItemButton>
              ))}
            {availableProducts.length === 0 && (
              <Typography
                data-testid={addLoading ? 'session-info-product-library-loading' : 'session-info-product-library-empty'}
                data-contract-source="/product/search"
                data-no-local-product-library-fallback="true"
                variant="body2"
                color="text.secondary"
                sx={{ py: 2, textAlign: 'center' }}
              >
                {addLoading ? (
                  <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 1 }}>
                    <CircularProgress size={14} /> 加载中...
                  </Box>
                ) : addError ? '商品库搜索失败，请重试' : '无可用商品或已全部添加'}
              </Typography>
            )}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)}>关闭</Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        PaperProps={{
          'data-testid': 'session-info-delete-product-dialog',
          'data-contract-scope': 'live-session-info-delete-product-dialog',
          'data-contract-source': '/live/product/delete',
          'data-product-id': deleteConfirm?.id ?? '',
          'data-loading': actionLoading === 'delete' ? 'true' : 'false',
        } as Record<string, string | number>}
      >
        <DialogTitle>确认移除</DialogTitle>
        <DialogContent>
          确定从本场次移除商品「{deleteConfirm ? String(deleteConfirm.productName) : ''}」？
        </DialogContent>
        <DialogActions>
          <Button data-testid="session-info-delete-cancel-button" onClick={() => setDeleteConfirm(null)}>取消</Button>
          <Button
            data-testid="session-info-delete-confirm-button"
            data-contract-source="/live/product/delete"
            data-disabled-reason={actionLoading === 'delete' ? 'delete-loading' : 'ready'}
            color="error"
            variant="contained"
            onClick={handleRemove}
            disabled={actionLoading === 'delete'}
          >
            {actionLoading === 'delete' ? '移除中...' : '移除'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
