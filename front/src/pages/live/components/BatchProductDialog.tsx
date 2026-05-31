import { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, TextField, Box, Typography, Checkbox, Chip,
  IconButton, Tooltip, CircularProgress, InputAdornment, Badge, Alert,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import DeleteIcon from '@mui/icons-material/Delete'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import SearchIcon from '@mui/icons-material/Search'
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined'
import ImageNotSupportedOutlinedIcon from '@mui/icons-material/ImageNotSupportedOutlined'
import { productApi } from '@/api/product'
import { PRODUCT_TYPE_OPTIONS } from './constants'
import { cdnThumb } from '@/utils/cdnImage'

interface ProductItem {
  id: number
  productName: string
  price?: number
  category?: string
  mainImage?: string
  sellingPoints?: string
  [key: string]: unknown
}

export interface BatchSelectedProduct {
  productId: number
  productName: string
  productType: string
  imageUrl?: string
  price?: number
}

interface BatchProductDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: (items: BatchSelectedProduct[]) => void
  existingProductIds: Set<number>
}

const PAGE_SIZE = 50
const BATCH_PRODUCT_READY_ENDPOINTS = ['/product/search', '/live/product/batch-add']
const BATCH_PRODUCT_UNSUPPORTED_ACTIONS = [
  'local-product-library-fallback',
  'direct-live-product-write',
  'local-batch-add-fallback',
  'product-mutation',
  'script-mutation',
]

function ProductRow({
  product, checked, isExisting, onToggle,
}: { product: ProductItem; checked: boolean; isExisting: boolean; onToggle: () => void }) {
  const imgSrc = cdnThumb(product.mainImage, 80, 80)
  return (
    <Box
      data-testid="batch-product-library-row"
      data-contract-source="/product/search"
      data-product-id={product.id}
      data-selected={checked ? 'true' : 'false'}
      data-existing-product={isExisting ? 'true' : 'false'}
      onClick={isExisting ? undefined : onToggle}
      sx={{
        display: 'flex', alignItems: 'center', gap: 1, px: 1.5, py: 1,
        cursor: isExisting ? 'not-allowed' : 'pointer',
        opacity: isExisting ? 0.4 : 1,
        '&:hover': isExisting ? {} : { bgcolor: 'action.hover' },
        borderBottom: '1px solid', borderColor: 'divider',
      }}
    >
      <Checkbox
        size="small"
        checked={checked || isExisting}
        disabled={isExisting}
        inputProps={{
          'data-testid': 'batch-product-row-checkbox',
          'data-contract-source': '/product/search',
        } as React.InputHTMLAttributes<HTMLInputElement>}
        sx={{ p: 0.5 }}
      />
      {imgSrc ? (
        <Box component="img" src={imgSrc} alt={product.productName}
          sx={{ width: 44, height: 44, objectFit: 'cover', borderRadius: 1, flexShrink: 0 }} />
      ) : (
        <Box
          data-testid="batch-product-image-placeholder-surface"
          sx={(theme) => ({
            width: 44,
            height: 44,
            bgcolor: theme.palette.mode === 'dark'
              ? alpha(theme.palette.common.white, 0.06)
              : theme.palette.action.hover,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 1,
            flexShrink: 0,
          })}
        >
          <ImageNotSupportedOutlinedIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
        </Box>
      )}
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="body2" noWrap fontWeight={500}>{product.productName}</Typography>
        <Typography variant="caption" color="text.secondary">
          {product.price ? `¥${product.price}` : ''}
          {product.category ? ` · ${product.category}` : ''}
        </Typography>
      </Box>
      {isExisting && <CheckCircleIcon sx={{ fontSize: 16, color: 'success.main' }} />}
    </Box>
  )
}

function SelectedRow({
  item, onRemove, onTypeChange,
}: { item: BatchSelectedProduct; onRemove: () => void; onTypeChange: (t: string) => void }) {
  const imgSrc = cdnThumb(item.imageUrl, 80, 80)
  return (
    <Box
      data-testid="batch-product-selected-row"
      data-contract-source="/live/product/batch-add"
      data-product-id={item.productId}
      data-product-type={item.productType}
      sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 1.5, py: 1, borderBottom: '1px solid', borderColor: 'divider' }}
    >
      {imgSrc ? (
        <Box component="img" src={imgSrc} alt={item.productName}
          sx={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 1, flexShrink: 0 }} />
      ) : (
        <Box
          data-testid="batch-product-selected-placeholder-surface"
          sx={(theme) => ({
            width: 40,
            height: 40,
            bgcolor: theme.palette.mode === 'dark'
              ? alpha(theme.palette.common.white, 0.06)
              : theme.palette.action.hover,
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 1,
            flexShrink: 0,
          })}
        />
      )}
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="body2" noWrap fontWeight={500}>{item.productName}</Typography>
        <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5, flexWrap: 'wrap' }}>
          {PRODUCT_TYPE_OPTIONS.map(opt => (
            <Chip
              key={opt.value}
              data-testid="batch-product-selected-type-chip"
              data-contract-source="/live/product/batch-add"
              data-contract-product-type={opt.value}
              label={opt.label}
              size="small"
              color={item.productType === opt.value ? opt.color : 'default'}
              variant={item.productType === opt.value ? 'filled' : 'outlined'}
              onClick={() => onTypeChange(opt.value)}
              sx={{ height: 20, fontSize: '0.7rem', cursor: 'pointer', '& .MuiChip-label': { px: 0.5 } }}
            />
          ))}
        </Box>
      </Box>
      <Tooltip title="移除">
        <IconButton
          size="small"
          onClick={onRemove}
          data-testid="batch-product-selected-remove-button"
          data-contract-source="/live/product/batch-add"
        >
          <DeleteIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  )
}
export function BatchProductDialog({ open, onClose, onConfirm, existingProductIds }: BatchProductDialogProps) {
  const [keyword, setKeyword] = useState('')
  const [products, setProducts] = useState<ProductItem[]>([])
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<BatchSelectedProduct[]>([])
  const [bulkType, setBulkType] = useState('flat')
  const [loadError, setLoadError] = useState<string | null>(null)

  const loadProducts = useCallback(async (reset = false) => {
    setLoading(true)
    setLoadError(null)
    try {
      const currentPage = reset ? 0 : page
      const result = await productApi.list({ page: currentPage, rows: PAGE_SIZE, productName: keyword || undefined })
      const list = (result.list ?? []).map(p => ({
        id: Number(p.id ?? 0),
        productName: String(p.productName ?? ''),
        price: p.price != null ? Number(p.price) : undefined,
        category: p.category != null ? String(p.category) : undefined,
        mainImage: p.mainImage != null ? String(p.mainImage) : undefined,
        sellingPoints: p.sellingPoints != null ? String(p.sellingPoints) : undefined,
      }))
      if (reset) {
        setProducts(list)
        setPage(1)
      } else {
        setProducts(prev => [...prev, ...list])
        setPage(p => p + 1)
      }
      setHasMore(list.length === PAGE_SIZE)
    } catch (e) {
      const message = e instanceof Error ? e.message : '商品库加载失败'
      setLoadError(`/product/search 商品库加载失败：${message}`)
      if (reset) {
        setProducts([])
        setPage(0)
      }
      setHasMore(false)
    } finally {
      setLoading(false)
    }
  }, [keyword, page])

  useEffect(() => {
    if (open) { setSelected([]); loadProducts(true) }
  }, [open]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (open) loadProducts(true)
  }, [keyword]) // eslint-disable-line react-hooks/exhaustive-deps

  const availableProducts = useMemo(
    () => products.filter(p => !existingProductIds.has(p.id)),
    [products, existingProductIds]
  )

  const selectedIds = useMemo(() => new Set(selected.map(s => s.productId)), [selected])

  const handleToggle = useCallback((product: ProductItem) => {
    setSelected(prev => {
      if (prev.some(s => s.productId === product.id)) {
        return prev.filter(s => s.productId !== product.id)
      }
      return [...prev, {
        productId: product.id,
        productName: product.productName,
        productType: bulkType,
        imageUrl: product.mainImage,
        price: product.price,
      }]
    })
  }, [bulkType])

  const handleSelectAll = useCallback(() => {
    const allSelected = availableProducts.every(p => selectedIds.has(p.id))
    if (allSelected) {
      setSelected([])
    } else {
      setSelected(availableProducts.map(p => ({
        productId: p.id,
        productName: p.productName,
        productType: bulkType,
        imageUrl: p.mainImage,
        price: p.price,
      })))
    }
  }, [availableProducts, selectedIds, bulkType])

  const handleRemove = (productId: number) => setSelected(prev => prev.filter(s => s.productId !== productId))
  const handleTypeChange = (productId: number, type: string) =>
    setSelected(prev => prev.map(s => s.productId === productId ? { ...s, productType: type } : s))

  const allSelected = availableProducts.length > 0 && availableProducts.every(p => selectedIds.has(p.id))

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      data-testid="live-select-batch-product-dialog"
      data-contract-scope="live-batch-product-library-dialog"
      data-contract-source={BATCH_PRODUCT_READY_ENDPOINTS.join('|')}
      data-ready-endpoints={BATCH_PRODUCT_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={BATCH_PRODUCT_UNSUPPORTED_ACTIONS.join('|')}
      data-selected-count={selected.length}
      data-existing-count={existingProductIds.size}
      data-product-count={products.length}
      data-available-count={availableProducts.length}
      data-page={page}
      data-keyword={keyword}
      data-loading={loading ? 'true' : 'false'}
      data-has-more={hasMore ? 'true' : 'false'}
      data-load-state={loadError ? 'error' : (loading ? 'loading' : 'ready')}
      data-no-local-product-library-fallback="true"
      PaperProps={{ sx: { height: '80vh' } }}
    >
      <DialogTitle sx={{ pb: 1 }} data-testid="batch-product-title" data-contract-source="/product/search|/live/product/batch-add">
        批量添加商品
        <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>从商品库选择添加到本场直播</Typography>
      </DialogTitle>
      <DialogContent data-testid="batch-product-content" data-contract-source="/product/search|/live/product/batch-add" sx={{ display: 'flex', gap: 2, p: 0, overflow: 'hidden' }}>
        {/* 左侧：商品库 */}
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', borderRight: '1px solid', borderColor: 'divider', minWidth: 0 }}>
          <Box sx={{ p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <TextField
              size="small" fullWidth placeholder="搜索商品名称..."
              value={keyword} onChange={e => setKeyword(e.target.value)}
              data-testid="batch-product-search-input"
              inputProps={{ 'data-contract-source': '/product/search' }}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            />
          </Box>
          {loadError && (
            <Alert
              severity="error"
              data-testid="batch-product-search-error"
              data-contract-source="/product/search"
              data-no-local-product-library-fallback="true"
              sx={{ borderRadius: 0 }}
            >
              {loadError}
            </Alert>
          )}
          <Box
            data-testid="batch-product-library-toolbar"
            data-contract-source="/product/search"
            data-available-count={availableProducts.length}
            sx={{ px: 1.5, py: 0.75, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1 }}
          >
            <Checkbox
              size="small"
              checked={allSelected}
              indeterminate={selected.length > 0 && !allSelected}
              onChange={handleSelectAll}
              inputProps={{
                'data-testid': 'batch-product-select-all-checkbox',
                'data-contract-source': '/product/search|/live/product/batch-add',
              } as React.InputHTMLAttributes<HTMLInputElement>}
              sx={{ p: 0.5 }}
            />
            <Typography variant="caption" color="text.secondary">全选当前页 ({availableProducts.length})</Typography>
            {loading && <CircularProgress size={14} sx={{ ml: 'auto' }} />}
          </Box>
          <Box
            data-testid="batch-product-library-list"
            data-contract-source="/product/search"
            data-no-local-product-library-fallback="true"
            sx={{ flex: 1, overflow: 'auto' }}
          >
            {availableProducts.map(p => (
              <ProductRow
                key={p.id} product={p}
                checked={selectedIds.has(p.id)}
                isExisting={existingProductIds.has(p.id)}
                onToggle={() => handleToggle(p)}
              />
            ))}
            {hasMore && !loading && (
              <Box sx={{ p: 1.5, textAlign: 'center' }}>
                <Button size="small" onClick={() => loadProducts(false)} data-testid="batch-product-load-more-button" data-contract-source="/product/search">加载更多</Button>
              </Box>
            )}
            {!loading && !loadError && availableProducts.length === 0 && (
              <Box
                data-testid="batch-product-library-empty-state"
                data-contract-source="/product/search"
                data-no-local-product-library-fallback="true"
                sx={{ textAlign: 'center', py: 6, color: 'text.secondary' }}
              >
                <Typography variant="body2">商品库暂无可添加商品</Typography>
              </Box>
            )}
          </Box>
        </Box>

        {/* 右侧：已选 */}
        <Box
          data-testid="batch-product-selected-panel"
          data-selected-count={selected.length}
          data-contract-source="/live/product/batch-add"
          sx={{ width: 320, flexShrink: 0, display: 'flex', flexDirection: 'column' }}
        >
          <Box sx={{ px: 1.5, py: 1, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1 }}>
            <Badge badgeContent={selected.length} color="primary">
              <ShoppingCartOutlinedIcon fontSize="small" />
            </Badge>
            <Typography variant="subtitle2">已选商品</Typography>
            <Box sx={{ flex: 1 }} />
            <Typography variant="caption" color="text.secondary">默认类型:</Typography>
            <Box sx={{ display: 'flex', gap: 0.5 }}>
              {PRODUCT_TYPE_OPTIONS.slice(0, 3).map(opt => (
                <Chip key={opt.value} label={opt.label} size="small"
                  data-testid="batch-product-default-type-chip"
                  data-contract-source="/live/product/batch-add"
                  data-contract-product-type={opt.value}
                  color={bulkType === opt.value ? opt.color : 'default'}
                  variant={bulkType === opt.value ? 'filled' : 'outlined'}
                  onClick={() => setBulkType(opt.value)}
                  sx={{ height: 20, fontSize: '0.7rem', cursor: 'pointer', '& .MuiChip-label': { px: 0.5 } }}
                />
              ))}
            </Box>
          </Box>
          <Box sx={{ flex: 1, overflow: 'auto' }}>
            {selected.map(s => (
              <SelectedRow key={s.productId} item={s}
                onRemove={() => handleRemove(s.productId)}
                onTypeChange={(t) => handleTypeChange(s.productId, t)}
              />
            ))}
            {selected.length === 0 && (
              <Box
                data-testid="batch-product-selected-empty-state"
                data-contract-source="/live/product/batch-add"
                sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 6, gap: 1 }}
              >
                <ShoppingCartOutlinedIcon sx={{ fontSize: 40, color: 'text.disabled' }} />
                <Typography variant="body2" color="text.secondary">从左侧勾选商品</Typography>
                <Typography variant="caption" color="text.disabled">支持单选、全选，添加后可设置商品类型</Typography>
              </Box>
            )}
          </Box>
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 1.5 }}>
        <Button onClick={onClose} data-testid="batch-product-cancel-button" data-contract-source="onClose-prop">取消</Button>
        <Button
          variant="contained"
          onClick={() => onConfirm(selected)}
          disabled={selected.length === 0}
          data-testid="batch-product-confirm-button"
          data-contract-source="/live/product/batch-add|onConfirm-prop"
          data-disabled-reason={selected.length === 0 ? 'empty-selection' : 'ready'}
        >
          添加 {selected.length} 个商品
        </Button>
      </DialogActions>
    </Dialog>
  )
}

