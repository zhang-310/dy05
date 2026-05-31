import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
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
  Chip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import type { LiveProduct } from '@/api/live-product'
import type { ProductLibraryItemVO } from '@/types/product'
import { PRODUCT_TYPE_OPTIONS, parseProductTypes } from './constants'

const PRODUCT_PANEL_READY_ENDPOINTS = [
  '/live/product/by-session',
  '/product/search',
  '/live/product/save',
  '/live/product/delete',
  '/live/product/batch-sort',
] as const

const PRODUCT_PANEL_UNSUPPORTED_ACTIONS = [
  'direct-api-request',
  'local-product-fallback',
  'local-product-library-fallback',
  'script-generation',
  'script-mutation',
  'shortvideo-project-create',
] as const

export interface ProductPanelProps {
  products: LiveProduct[]
  sortedProducts: LiveProduct[]
  addProductOpen: boolean
  productSearch: string
  productList: ProductLibraryItemVO[]
  selectedProductToAdd: ProductLibraryItemVO | null
  addProductTypeSelected: string[]
  addProductTypeLoading: boolean
  sorting: boolean
  addedProductIds: Set<number>
  onAddProductOpen: (open: boolean) => void
  onProductSearchChange: (v: string) => void
  onSelectProductToAdd: (product: ProductLibraryItemVO | null) => void
  onAddProductTypeChange: (types: string[]) => void
  onConfirmAddProduct: () => void
  onAddProduct: (product: ProductLibraryItemVO) => void
  onRemoveProduct: (row: LiveProduct) => void
  onMoveProduct: (row: LiveProduct, direction: 'up' | 'down') => void
  onUpdateProductType: (row: LiveProduct, types: string[]) => void
}

export function ProductPanel({
  products,
  sortedProducts,
  addProductOpen,
  productSearch,
  productList,
  selectedProductToAdd,
  addProductTypeSelected,
  addProductTypeLoading,
  sorting,
  addedProductIds,
  onAddProductOpen,
  onProductSearchChange,
  onSelectProductToAdd,
  onAddProductTypeChange,
  onConfirmAddProduct,
  onAddProduct,
  onRemoveProduct,
  onMoveProduct,
  onUpdateProductType,
}: ProductPanelProps) {
  const visibleLibraryProducts = productList.filter((p) => !addedProductIds.has(p.id))

  return (
    <>
      <Card
        variant="outlined"
        data-testid="live-product-panel-root"
        data-contract-scope="live-product-panel-props-bridge"
        data-ready-endpoints={PRODUCT_PANEL_READY_ENDPOINTS.join('|')}
        data-unsupported-actions={PRODUCT_PANEL_UNSUPPORTED_ACTIONS.join('|')}
        data-contract-source="/live/product/by-session"
        data-product-count={products.length}
        data-sorted-product-count={sortedProducts.length}
        data-no-local-product-fallback="true"
        data-no-direct-api-request="true"
        sx={{ width: 300, flexShrink: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
      >
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="subtitle2">选品 ({products.length})</Typography>
            <Button
              size="small"
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => onAddProductOpen(true)}
              data-testid="live-product-panel-add-open-button"
              data-contract-source="onAddProductOpen-prop"
            >
              添加
            </Button>
          </Box>
        </CardContent>
        <Box
          sx={{ flex: 1, overflow: 'auto', minHeight: 0 }}
          data-testid="live-product-panel-list"
          data-contract-source="/live/product/by-session"
          data-no-local-product-fallback="true"
        >
          {products.length === 0 ? (
            <Typography
              variant="body2"
              color="text.secondary"
              data-testid="live-product-panel-empty"
              data-contract-source="/live/product/by-session"
              data-no-local-product-fallback="true"
              sx={{ p: 2, textAlign: 'center' }}
            >
              暂无选品，点击「添加」从商品库选择
            </Typography>
          ) : (
            <List dense disablePadding>
              {sortedProducts.map((row, idx) => {
                const types = parseProductTypes(row.productType)
                return (
                  <Box
                    key={String(row.id)}
                    data-testid="live-product-panel-row"
                    data-contract-source="/live/product/by-session"
                    data-contract-product-id={row.productId ?? ''}
                    sx={{
                      borderBottom: '1px solid',
                      borderColor: 'divider',
                      py: 0.5,
                      px: 1,
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      <Tooltip title="上移">
                        <span>
                          <IconButton
                            size="small"
                            disabled={sorting || idx === 0}
                            onClick={() => onMoveProduct(row, 'up')}
                            data-testid="live-product-panel-move-up-button"
                            data-contract-source="onMoveProduct-prop"
                          >
                            <ArrowUpwardIcon sx={{ fontSize: 16 }} />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="下移">
                        <span>
                          <IconButton
                            size="small"
                            disabled={sorting || idx === sortedProducts.length - 1}
                            onClick={() => onMoveProduct(row, 'down')}
                            data-testid="live-product-panel-move-down-button"
                            data-contract-source="onMoveProduct-prop"
                          >
                            <ArrowDownwardIcon sx={{ fontSize: 16 }} />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="移除">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => onRemoveProduct(row)}
                          data-testid="live-product-panel-remove-button"
                          data-contract-source="onRemoveProduct-prop"
                        >
                          <DeleteIcon sx={{ fontSize: 16 }} />
                        </IconButton>
                      </Tooltip>
                      <Typography variant="body2" sx={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {String(row.productName ?? '-')}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.25, mt: 0.25, pl: 5 }}>
                      {PRODUCT_TYPE_OPTIONS.map((o) => {
                        const active = types.includes(o.value)
                        return (
                          <Chip
                            key={o.value}
                            data-testid={active ? 'live-product-panel-type-chip-active' : 'live-product-panel-type-chip'}
                            data-contract-source="onUpdateProductType-prop"
                            data-contract-product-id={row.productId ?? ''}
                            data-contract-product-type={o.value}
                            label={o.label}
                            size="small"
                            color={active ? o.color : 'default'}
                            variant={active ? 'filled' : 'outlined'}
                            sx={{ height: 20, fontSize: '0.7rem' }}
                            onClick={() => {
                              const next = active ? types.filter((t) => t !== o.value) : [...types, o.value]
                              onUpdateProductType(row, next)
                            }}
                          />
                        )
                      })}
                    </Box>
                  </Box>
                )
              })}
            </List>
          )}
        </Box>
      </Card>

      <Dialog
        open={addProductOpen}
        onClose={() => { onAddProductOpen(false); onSelectProductToAdd(null) }}
        maxWidth="sm"
        fullWidth
        data-testid="live-product-panel-add-dialog"
        data-contract-scope="live-product-panel-library-selector"
        data-ready-endpoints="/product/search|/live/product/save"
        data-contract-source="/product/search|onConfirmAddProduct-prop"
        data-library-count={visibleLibraryProducts.length}
        data-selected-product-id={selectedProductToAdd?.id ?? ''}
        data-no-local-product-library-fallback="true"
      >
        <DialogTitle>{selectedProductToAdd ? '确认添加 - 选择产品分类' : '从商品库添加'}</DialogTitle>
        <DialogContent>
          {selectedProductToAdd ? (
            <Box
              data-testid="live-product-panel-add-confirm-surface"
              data-contract-source="onConfirmAddProduct-prop"
              data-selected-product-id={selectedProductToAdd.id}
            >
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                产品：{String(selectedProductToAdd.productName ?? selectedProductToAdd.id)}
              </Typography>
              <Typography variant="caption" color="info.main" sx={{ display: 'block', mb: 1 }}>
                💡 系统建议：根据利润率、控单策略等自动推断，可修改
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {PRODUCT_TYPE_OPTIONS.map((o) => {
                  const active = addProductTypeSelected.includes(o.value)
                  return (
                    <Chip
                      key={o.value}
                      data-testid={active ? 'live-product-panel-add-type-chip-active' : 'live-product-panel-add-type-chip'}
                      data-contract-source="onAddProductTypeChange-prop"
                      data-contract-product-type={o.value}
                      label={o.label}
                      size="small"
                      color={active ? o.color : 'default'}
                      variant={active ? 'filled' : 'outlined'}
                      sx={{ height: 28 }}
                      onClick={() => {
                        const next = active ? addProductTypeSelected.filter((t) => t !== o.value) : [...addProductTypeSelected, o.value]
                        if (next.length === 0) next.push('flat')
                        onAddProductTypeChange(next)
                      }}
                    />
                  )
                })}
              </Box>
            </Box>
          ) : (
            <>
              <TextField
                size="small"
                fullWidth
                label="搜索"
                value={productSearch}
                onChange={(e) => onProductSearchChange(e.target.value)}
                inputProps={{
                  'data-testid': 'live-product-panel-search-input',
                  'data-contract-source': 'onProductSearchChange-prop',
                }}
                sx={{ mb: 2 }}
              />
              <List
                sx={{ maxHeight: 320, overflow: 'auto' }}
                data-testid="live-product-panel-library-list"
                data-contract-source="/product/search"
                data-no-local-product-library-fallback="true"
              >
                {visibleLibraryProducts
                  .map((p) => (
                    <ListItemButton
                      key={String(p.id)}
                      onClick={() => onAddProduct(p)}
                      disabled={addProductTypeLoading}
                      data-testid="live-product-panel-library-row"
                      data-contract-source="onAddProduct-prop"
                      data-contract-product-id={p.id}
                    >
                      <ListItemText primary={String(p.productName ?? p.id)} secondary={p.price != null ? `¥${Number(p.price).toFixed(2)}` : ''} />
                    </ListItemButton>
                  ))}
                {visibleLibraryProducts.length === 0 && (
                  <Typography
                    color="text.secondary"
                    data-testid="live-product-panel-library-empty"
                    data-contract-source="/product/search"
                    data-no-local-product-library-fallback="true"
                    sx={{ py: 2, textAlign: 'center' }}
                  >
                    暂无可选商品，或已全部添加
                  </Typography>
                )}
              </List>
            </>
          )}
        </DialogContent>
        <DialogActions>
          {selectedProductToAdd ? (
            <>
              <Button onClick={() => onSelectProductToAdd(null)} data-testid="live-product-panel-add-back-button" data-contract-source="onSelectProductToAdd-prop">返回</Button>
              <Button
                variant="contained"
                onClick={onConfirmAddProduct}
                data-testid="live-product-panel-confirm-add-button"
                data-contract-source="onConfirmAddProduct-prop"
              >
                确认添加
              </Button>
            </>
          ) : (
            <Button onClick={() => onAddProductOpen(false)} data-testid="live-product-panel-add-close-button" data-contract-source="onAddProductOpen-prop">关闭</Button>
          )}
        </DialogActions>
      </Dialog>
    </>
  )
}
