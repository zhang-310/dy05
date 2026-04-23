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
} from '@mui/material'
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
  const [deleteConfirm, setDeleteConfirm] = useState<DeleteConfirmData | null>(null)
  const [sorting, setSorting] = useState(false)

  const addedProductIds = new Set(products.map((p) => p.productId as number).filter(Boolean))

  const loadProductList = useCallback(() => {
    setAddLoading(true)
    searchProducts({ page: 0, rows: 50, productName: productSearch || undefined })
      .then((res) => {
        const { list } = normalizePageResult(res)
        setProductList((list ?? []) as ProductSearchRow[])
      })
      .catch(() => setProductList([]))
      .finally(() => setAddLoading(false))
  }, [productSearch])

  useEffect(() => {
    if (addOpen) loadProductList()
  }, [addOpen, loadProductList])

  const handleAddProduct = async (product: Record<string, unknown>) => {
    const productId = product.id as number
    if (!productId) return
    const maxPos = products.reduce((m, p) => Math.max(m, (p.position as number) ?? 0), 0)
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
    }
  }

  const handleRemove = async () => {
    if (!deleteConfirm) return
    const id = deleteConfirm.id as number
    if (!id) return
    try {
      await deleteLiveProduct(id)
      toast('移除成功', 'success')
      setDeleteConfirm(null)
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '移除失败', 'error')
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
    setSorting(true)
    try {
      await batchSortProducts({ sessionId, productIds })
      toast('排序已更新', 'success')
      onRefresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '排序失败', 'error')
    } finally {
      setSorting(false)
    }
  }

  const sortedProducts = [...products].sort((a, b) => ((a.position as number) ?? 0) - ((b.position as number) ?? 0))

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="subtitle2" color="text.secondary">
          从商品库添加、排序、移除
        </Typography>
        <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)}>
          添加商品
        </Button>
      </Box>
      {products.length === 0 ? (
        <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
          暂无选品，点击「添加商品」从商品库选择
        </Typography>
      ) : (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.50' }}>
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
                <TableRow key={String(row.id)}>
                  <TableCell>
                    <Tooltip title="上移">
                      <span>
                        <IconButton
                          size="small"
                          disabled={sorting || idx === 0}
                          onClick={() => handleMove(row, 'up')}
                        >
                          <ArrowUpwardIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="下移">
                      <span>
                        <IconButton
                          size="small"
                          disabled={sorting || idx === sortedProducts.length - 1}
                          onClick={() => handleMove(row, 'down')}
                        >
                          <ArrowDownwardIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="移除">
                      <IconButton size="small" color="error" onClick={() => setDeleteConfirm({ id: row.id as number, productName: row.productName as string | undefined })}>
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

      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>从商品库添加</DialogTitle>
        <DialogContent>
          <TextField
            size="small"
            fullWidth
            placeholder="搜索商品名称"
            value={productSearch}
            onChange={(e) => setProductSearch(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && loadProductList()}
            sx={{ mb: 2 }}
          />
          <Button size="small" onClick={loadProductList} disabled={addLoading}>
            {addLoading ? '加载中...' : '搜索'}
          </Button>
          <List sx={{ maxHeight: 320, overflow: 'auto', mt: 2 }}>
            {productList
              .filter((p) => !addedProductIds.has(p.id as number))
              .map((p) => (
                <ListItemButton key={String(p.id)} onClick={() => handleAddProduct(p)}>
                  <ListItemText
                    primary={String(p.productName ?? p.id)}
                    secondary={p.price != null ? `¥${Number(p.price).toFixed(2)}` : undefined}
                  />
                </ListItemButton>
              ))}
            {productList.filter((p) => !addedProductIds.has(p.id as number)).length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                {addLoading ? '加载中...' : '无可用商品或已全部添加'}
              </Typography>
            )}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddOpen(false)}>关闭</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!deleteConfirm} onClose={() => setDeleteConfirm(null)}>
        <DialogTitle>确认移除</DialogTitle>
        <DialogContent>
          确定从本场次移除商品「{deleteConfirm ? String(deleteConfirm.productName) : ''}」？
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirm(null)}>取消</Button>
          <Button color="error" variant="contained" onClick={handleRemove}>
            移除
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
