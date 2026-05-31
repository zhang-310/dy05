import { useState, useCallback, useMemo } from 'react'
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import {
  Box, Button, Typography, Chip, IconButton, List, ListItem,
  ListItemText, ListItemSecondaryAction, Divider, Alert,
  Tooltip,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline'
import DeleteIcon from '@mui/icons-material/Delete'
import DragIndicatorIcon from '@mui/icons-material/DragIndicator'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { useCoreData } from '../contexts'
import { liveApi } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { useMutation } from '@tanstack/react-query'
import { SortStrategyPanel, type SortStrategy } from './SortStrategyPanel'
import { ProductGridView } from './ProductGridView'
import { BatchProductDialog, type BatchSelectedProduct } from './BatchProductDialog'
import { useSmartSort } from '../hooks/useSmartSort'
import { PRODUCT_TYPE_OPTIONS, parseProductTypes } from './constants'
import type { LiveProduct } from '@/api/live-product'
import { compareLiveProducts, sortLiveProducts } from '../utils/order'

const SELECT_TAB_READY_ENDPOINTS = [
  '/live/product/by-session',
  '/product/search',
  '/live/product/batch-add',
  '/live/product/delete',
  '/live/ai/sort-suggest',
  '/live/product/batch-sort',
]

const SELECT_TAB_CONTEXT_ENDPOINTS = [
  '/live/session/get',
  '/live/product/by-session',
  '/live/script/by-session',
]

const SELECT_TAB_UNSUPPORTED_ACTIONS = [
  'local-product-list-fallback',
  'local-ai-sort-fallback',
  'product-library-mutation',
  'script-generation',
  'script-mutation',
  'session-status-mutation',
  'shortvideo-export',
]

function applySortStrategy(products: LiveProduct[], strategy: SortStrategy): LiveProduct[] {
  const sorted = sortLiveProducts(products)
  if (strategy === 'manual') return sorted
  switch (strategy) {
    case 'type': {
      const typeOrder: Record<string, number> = { hot: 1, control: 2, profit: 3, loss: 4, flat: 5 }
      sorted.sort((a, b) => {
        const ta = Math.min(...parseProductTypes(a.productType).map(t => typeOrder[t] ?? 99), 99)
        const tb = Math.min(...parseProductTypes(b.productType).map(t => typeOrder[t] ?? 99), 99)
        return ta - tb || compareLiveProducts(a, b)
      })
      break
    }
    case 'alpha':
      sorted.sort((a, b) => (a.productName ?? '').localeCompare(b.productName ?? '', 'zh-Hans') || compareLiveProducts(a, b))
      break
    case 'price':
      sorted.sort((a, b) => Number(b.price ?? 0) - Number(a.price ?? 0) || compareLiveProducts(a, b))
      break
  }
  return sorted
}

export function reorderLiveProductRelationIdsForDrag(
  products: LiveProduct[],
  activeId: string | number,
  overId: string | number,
): number[] {
  const oldIndex = products.findIndex(p => String(p.id) === String(activeId))
  const newIndex = products.findIndex(p => String(p.id) === String(overId))
  if (oldIndex < 0 || newIndex < 0 || oldIndex === newIndex) {
    return products.map(p => Number(p.id)).filter(Number.isFinite)
  }
  return arrayMove(products, oldIndex, newIndex).map(p => Number(p.id)).filter(Number.isFinite)
}

interface SortableProductRowProps {
  product: LiveProduct
  index: number
  highlighted: boolean
  typeOptions: (typeof PRODUCT_TYPE_OPTIONS)[number][]
  scriptCount: number
  dragEnabled: boolean
  deleteDisabled: boolean
  onClick: () => void
  onDelete: () => void
}

function SortableProductRow({
  product,
  index,
  highlighted,
  typeOptions,
  scriptCount,
  dragEnabled,
  deleteDisabled,
  onClick,
  onDelete,
}: SortableProductRowProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: String(product.id),
    disabled: !dragEnabled,
  })

  return (
    <ListItem
      ref={setNodeRef}
      data-testid={highlighted ? 'select-product-highlight-surface' : 'live-select-product-row'}
      data-contract-product-id={product.productId}
      data-contract-sort-id={product.id}
      data-contract-source="/live/product/by-session"
      data-script-count={scriptCount}
      data-drag-enabled={dragEnabled ? 'true' : 'false'}
      sx={(theme) => ({
        px: 1.5,
        py: 0.75,
        cursor: 'pointer',
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.56 : 1,
        position: 'relative',
        zIndex: isDragging ? 2 : 'auto',
        boxShadow: isDragging ? theme.shadows[4] : 'none',
        bgcolor: highlighted
          ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.1)
          : isDragging
            ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.12 : 0.06)
            : 'transparent',
        '&:hover': { bgcolor: highlighted ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.2 : 0.12) : 'action.hover' },
        borderLeft: '3px solid',
        borderLeftColor: highlighted ? 'primary.main' : isDragging ? 'primary.light' : 'transparent',
      })}
      onClick={onClick}
    >
      <Tooltip title={dragEnabled ? '拖拽调整排品顺序' : '手动排序、全部商品、非倒序时可拖拽'}>
        <Box
          {...attributes}
          {...listeners}
          data-testid="live-select-product-drag-handle"
          data-contract-source="/live/product/batch-sort"
          onClick={(e) => e.stopPropagation()}
          sx={{
            display: 'flex',
            alignItems: 'center',
            mr: 0.5,
            color: dragEnabled ? 'text.secondary' : 'text.disabled',
            cursor: dragEnabled ? 'grab' : 'not-allowed',
            touchAction: 'none',
            '&:active': { cursor: dragEnabled ? 'grabbing' : 'not-allowed' },
          }}
        >
          <DragIndicatorIcon sx={{ fontSize: 18 }} />
        </Box>
      </Tooltip>
      <Typography variant="caption" sx={{ mr: 1, color: 'text.secondary', minWidth: 18 }}>{index + 1}</Typography>
      <ListItemText
        primary={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0 }}>
            <Typography variant="body2" noWrap sx={{ maxWidth: 150, fontWeight: highlighted ? 700 : 500 }}>{product.productName}</Typography>
            {typeOptions.map(opt => (
              <Chip key={opt.value} label={opt.label} size="small" color={opt.color} sx={{ height: 16, fontSize: '0.68rem', '& .MuiChip-label': { px: 0.4 } }} />
            ))}
          </Box>
        }
        secondary={product.price ? `¥${product.price}` : ''}
        secondaryTypographyProps={{ variant: 'caption' }}
      />
      <ListItemSecondaryAction>
        <Tooltip title="移除">
          <IconButton
            size="small"
            color="error"
            onClick={(e) => {
              e.stopPropagation()
              onDelete()
            }}
            disabled={deleteDisabled}
            data-testid="live-select-product-delete-button"
            data-contract-source="/live/product/delete"
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </ListItemSecondaryAction>
    </ListItem>
  )
}

interface SelectTabContentProps {
  sessionId: number
  onNext?: () => void
}
export function SelectTabContent({ sessionId, onNext }: SelectTabContentProps) {
  const toast = useToast()
  const { products, scripts, refetchProducts } = useCoreData()
  const [sortStrategy, setSortStrategy] = useState<SortStrategy>('manual')
  const [activeTypeFilter, setActiveTypeFilter] = useState<string | null>(null)
  const [batchOpen, setBatchOpen] = useState(false)
  const [reversed, setReversed] = useState(false)
  const [manualOrderIds, setManualOrderIds] = useState<number[] | null>(null)
  const [sortError, setSortError] = useState('')
  const [dragSaving, setDragSaving] = useState(false)
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  const smartSort = useSmartSort({
    sessionId,
    products,
    onApplied: refetchProducts,
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.productDelete(id),
    onSuccess: () => { toast('已移除商品', 'success'); refetchProducts() },
    onError: (e: Error) => toast(`/live/product/delete 移除商品失败：${e.message}`, 'error'),
  })

  const batchAddMut = useMutation({
    mutationFn: (items: BatchSelectedProduct[]) =>
      liveApi.productBatchAdd(sessionId, items.map(i => ({
        productId: i.productId,
        productName: i.productName,
        productType: i.productType,
        imageUrl: i.imageUrl,
        price: i.price,
      }))),
    onSuccess: (count) => {
      toast(`成功添加 ${count} 个商品`, 'success')
      refetchProducts()
      setBatchOpen(false)
    },
    onError: (e: Error) => toast(`/live/product/batch-add 批量添加商品失败：${e.message}`, 'error'),
  })

  const handleBatchConfirm = useCallback((items: BatchSelectedProduct[]) => {
    if (items.length === 0) return
    batchAddMut.mutate(items)
  }, [batchAddMut])

  const handleReverse = useCallback(() => setReversed(v => !v), [])

  const typeDistribution = useMemo(() => {
    const counts: Record<string, number> = {}
    for (const p of products) {
      for (const t of parseProductTypes(p.productType)) {
        counts[t] = (counts[t] || 0) + 1
      }
    }
    return counts
  }, [products])

  const productScriptCounts = useMemo(() => {
    const map = new Map<number, number>()
    for (const s of scripts) {
      if (s.productId) map.set(s.productId, (map.get(s.productId) ?? 0) + 1)
    }
    return map
  }, [scripts])

  const existingProductIds = useMemo(() => new Set(products.map(p => p.productId)), [products])

  const strategySorted = useMemo(() => {
    const sorted = applySortStrategy(products, sortStrategy)
    if (sortStrategy !== 'manual' || !manualOrderIds || manualOrderIds.length === 0) return sorted
    const order = new Map(manualOrderIds.map((id, index) => [id, index]))
    return [...sorted].sort((a, b) => {
      const ai = order.get(Number(a.id))
      const bi = order.get(Number(b.id))
      if (ai == null && bi == null) return compareLiveProducts(a, b)
      if (ai == null) return 1
      if (bi == null) return -1
      return ai - bi
    })
  }, [products, sortStrategy, manualOrderIds])

  const filteredProducts = useMemo(() => {
    let list = activeTypeFilter
      ? strategySorted.filter(p => parseProductTypes(p.productType).includes(activeTypeFilter))
      : strategySorted
    if (reversed) list = [...list].reverse()
    return list
  }, [strategySorted, activeTypeFilter, reversed])

  const [highlightedId, setHighlightedId] = useState<number | null>(null)
  const canDragSort = sortStrategy === 'manual' && activeTypeFilter === null && !reversed && filteredProducts.length > 1
  const sortableItemIds = useMemo(() => filteredProducts.map(p => String(p.id)), [filteredProducts])

  const handleDragEnd = useCallback(async (event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id || !canDragSort) return
    const productIds = reorderLiveProductRelationIdsForDrag(filteredProducts, active.id, over.id)
    if (productIds.length !== filteredProducts.length) return
    const prevManualOrderIds = manualOrderIds

    setSortError('')
    setManualOrderIds(productIds)
    setDragSaving(true)
    try {
      await liveApi.productBatchSort(sessionId, productIds)
      toast('排品顺序已保存', 'success')
      refetchProducts()
    } catch (e) {
      setManualOrderIds(prevManualOrderIds)
      const message = `/live/product/batch-sort 拖拽保存失败：${e instanceof Error ? e.message : '排序失败'}`
      setSortError(message)
      toast(message, 'error')
    } finally {
      setDragSaving(false)
    }
  }, [canDragSort, filteredProducts, manualOrderIds, refetchProducts, sessionId, toast])

  if (products.length === 0) {
    return (
      <Box
        data-testid="live-select-tab-workbench"
        data-contract-scope="live-product-selection-sorting"
        data-ready-endpoints={SELECT_TAB_READY_ENDPOINTS.join('|')}
        data-context-endpoints={SELECT_TAB_CONTEXT_ENDPOINTS.join('|')}
        data-unsupported-actions={SELECT_TAB_UNSUPPORTED_ACTIONS.join('|')}
        data-session-id={sessionId}
        data-product-count={products.length}
        data-script-count={scripts.length}
        data-filtered-count={filteredProducts.length}
        data-sort-strategy={sortStrategy}
        data-active-type-filter={activeTypeFilter ?? 'all'}
        data-no-local-product-list-fallback="true"
        data-no-local-ai-sort-fallback="true"
        sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}
      >
        <Alert
          severity="info"
          data-testid="live-select-contract-alert"
          data-contract-ready-endpoints={SELECT_TAB_READY_ENDPOINTS.join('|')}
          data-no-local-product-list-fallback="true"
          sx={{ borderRadius: 0 }}
        >
          选品排品仅使用直播商品真实上下文；空列表不注入本地商品，批量添加来自商品库搜索并写入本场直播。
        </Alert>
        <Box
          data-testid="live-select-empty-state"
          data-contract-source="/live/product/by-session"
          data-no-local-product-list-fallback="true"
          sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2, py: 8 }}
        >
          <Typography variant="h6" color="text.secondary">添加商品开始构建话术</Typography>
          <Typography variant="body2" color="text.secondary">从商品库选择，支持单选和批量添加</Typography>
          <Button
            variant="contained"
            startIcon={<AddCircleOutlineIcon />}
            onClick={() => setBatchOpen(true)}
            data-testid="live-select-empty-batch-add-button"
            data-contract-source="/product/search|/live/product/batch-add"
          >
            批量添加商品
          </Button>
        </Box>
        <BatchProductDialog open={batchOpen} onClose={() => setBatchOpen(false)} onConfirm={handleBatchConfirm} existingProductIds={existingProductIds} />
      </Box>
    )
  }
  return (
    <Box
      data-testid="live-select-tab-workbench"
      data-contract-scope="live-product-selection-sorting"
      data-ready-endpoints={SELECT_TAB_READY_ENDPOINTS.join('|')}
      data-context-endpoints={SELECT_TAB_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SELECT_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={sessionId}
      data-product-count={products.length}
      data-script-count={scripts.length}
      data-filtered-count={filteredProducts.length}
      data-sort-strategy={sortStrategy}
      data-active-type-filter={activeTypeFilter ?? 'all'}
      data-ai-sort-state={smartSort.loading ? 'loading' : smartSort.suggestion ? 'suggested' : smartSort.lastError ? 'error' : 'idle'}
      data-no-local-product-list-fallback="true"
      data-no-local-ai-sort-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}
    >
      <Alert
        severity="info"
        data-testid="live-select-contract-alert"
        data-contract-ready-endpoints={SELECT_TAB_READY_ENDPOINTS.join('|')}
        data-no-local-product-list-fallback="true"
        data-no-local-ai-sort-fallback="true"
        sx={{ flexShrink: 0, borderRadius: 0, borderBottom: 1, borderColor: 'divider' }}
      >
        选品排品使用 `/live/product/by-session` 上下文，批量添加写入 `/live/product/batch-add`，AI 排品失败只提示 `/live/ai/sort-suggest` 来源，不生成本地建议。
      </Alert>
      {/* 工具栏 */}
      <Box
        data-testid="live-select-toolbar"
        data-contract-source="/live/product/by-session|/live/ai/sort-suggest|/live/product/batch-sort"
        data-filtered-count={filteredProducts.length}
        sx={{ px: 2, py: 1, flexShrink: 0, borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper', display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}
      >
        <SortStrategyPanel
          value={sortStrategy}
          onChange={setSortStrategy}
          onReverse={handleReverse}
          aiLoading={smartSort.loading}
          onAiSort={smartSort.requestSmartSort}
          aiDisabled={products.length < 2}
        />
        <Divider orientation="vertical" flexItem />
        {/* 统计 + 类型筛选 */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
          <Typography variant="body2" fontWeight={700}>{products.length}</Typography>
          <Typography variant="caption" color="text.secondary">商品</Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', alignItems: 'center' }}>
          <Chip label="全部" size="small"
            data-testid="live-select-type-filter-all"
            data-contract-filter-value="all"
            variant={activeTypeFilter === null ? 'filled' : 'outlined'}
            color={activeTypeFilter === null ? 'primary' : 'default'}
            onClick={() => setActiveTypeFilter(null)}
            sx={{ cursor: 'pointer' }}
          />
          {Object.entries(typeDistribution).map(([type, count]) => {
            const opt = PRODUCT_TYPE_OPTIONS.find(o => o.value === type)
            return (
              <Chip key={type}
                data-testid="live-select-type-filter-chip"
                data-contract-filter-value={type}
                label={`${opt?.label ?? type} (${count})`}
                size="small"
                variant={activeTypeFilter === type ? 'filled' : 'outlined'}
                color={activeTypeFilter === type ? (opt?.color ?? 'primary') : 'default'}
                onClick={() => setActiveTypeFilter(prev => prev === type ? null : type)}
                sx={{ cursor: 'pointer' }}
              />
            )
          })}
        </Box>
        <Box sx={{ ml: 'auto', display: 'flex', gap: 1 }}>
          <Button
            size="small"
            variant="outlined"
            startIcon={<AddCircleOutlineIcon />}
            onClick={() => setBatchOpen(true)}
            data-testid="live-select-batch-add-button"
            data-contract-source="/product/search|/live/product/batch-add"
          >
            批量添加
          </Button>
          {onNext && (
            <Button
              size="small"
              variant="contained"
              endIcon={<ArrowForwardIcon />}
              onClick={onNext}
              disabled={products.length === 0}
              data-testid="live-select-next-generate-button"
              data-contract-next="generate"
            >
              下一步：生成
            </Button>
          )}
        </Box>
      </Box>

      {/* AI排品建议条 */}
      {smartSort.suggestion && (
        <Alert
          data-testid="live-select-ai-sort-suggestion"
          data-contract-source="/live/ai/sort-suggest|/live/product/batch-sort"
          data-ai-sort-fallback={smartSort.suggestion.isFallback ? 'true' : 'false'}
          severity={smartSort.suggestion.isFallback ? 'warning' : 'info'}
          icon={smartSort.suggestion.isFallback ? <InfoOutlinedIcon /> : undefined}
          sx={{ flexShrink: 0, borderBottom: 1, borderColor: 'divider', borderRadius: 0 }}
          action={
            <Box sx={{ display: 'flex', gap: 0.5 }}>
              <Button size="small" color="inherit" onClick={smartSort.applySuggestion}>应用</Button>
              <Button size="small" color="inherit" onClick={smartSort.clearSuggestion}>忽略</Button>
            </Box>
          }
        >
          {smartSort.suggestion.reason}
        </Alert>
      )}

      {smartSort.lastError && (
        <Alert
          severity="error"
          data-testid="live-select-ai-sort-error"
          data-contract-source="/live/ai/sort-suggest|/live/product/batch-sort"
          data-no-local-ai-sort-fallback="true"
          sx={{ flexShrink: 0, borderBottom: 1, borderColor: 'divider', borderRadius: 0 }}
        >
          {smartSort.lastError}
        </Alert>
      )}

      {sortError && (
        <Alert
          severity="error"
          data-testid="live-select-drag-sort-error"
          data-contract-source="/live/product/batch-sort"
          data-no-local-product-list-fallback="true"
          sx={{ flexShrink: 0, borderBottom: 1, borderColor: 'divider', borderRadius: 0 }}
        >
          {sortError}
        </Alert>
      )}

      {!canDragSort && products.length > 1 && (
        <Alert
          severity="info"
          data-testid="live-select-drag-sort-hint"
          data-contract-source="/live/product/batch-sort"
          sx={{ flexShrink: 0, borderBottom: 1, borderColor: 'divider', borderRadius: 0, py: 0.25 }}
        >
          拖拽排品在“手动排序 + 全部商品 + 非倒序”下启用，避免把筛选视图写成整场排序。
        </Alert>
      )}

      {filteredProducts.length === 0 && (
        <Alert
          severity="warning"
          data-testid="live-select-filter-empty-state"
          data-contract-source="/live/product/by-session"
          data-no-local-product-list-fallback="true"
          sx={{ flexShrink: 0, borderBottom: 1, borderColor: 'divider', borderRadius: 0 }}
        >
          当前筛选没有商品，页面不会注入本地商品占位。
        </Alert>
      )}

      {/* 主体：左侧列表 + 右侧卡片 */}
      <Box
        data-testid="live-select-main-surface"
        data-contract-source="/live/product/by-session|/live/script/by-session"
        sx={{ flex: 1, display: 'flex', minHeight: 0, overflow: 'hidden' }}
      >
        {/* 左侧：拖拽列表 */}
        <Box
          data-testid="live-select-list-surface"
          data-filtered-count={filteredProducts.length}
          data-drag-sort-enabled={canDragSort ? 'true' : 'false'}
          data-drag-saving={dragSaving ? 'true' : 'false'}
          data-no-local-product-list-fallback="true"
          sx={{ width: { xs: 300, xl: 340 }, flexShrink: 0, display: 'flex', flexDirection: 'column', borderRight: '1px solid', borderColor: 'divider', overflow: 'hidden', bgcolor: 'background.paper' }}
        >
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
            <SortableContext items={sortableItemIds} strategy={verticalListSortingStrategy}>
              <List dense sx={{ flex: 1, overflow: 'auto', p: 0 }}>
                {filteredProducts.map((p, idx) => {
                  const types = parseProductTypes(p.productType)
                  const typeOpts = types.map(t => PRODUCT_TYPE_OPTIONS.find(o => o.value === t)).filter(Boolean) as (typeof PRODUCT_TYPE_OPTIONS)[number][]
                  return (
                    <Box key={p.id}>
                      <SortableProductRow
                        product={p}
                        index={idx}
                        highlighted={highlightedId === p.productId}
                        typeOptions={typeOpts}
                        scriptCount={productScriptCounts.get(p.productId) ?? 0}
                        dragEnabled={canDragSort && !dragSaving}
                        deleteDisabled={deleteMut.isPending || dragSaving}
                        onClick={() => setHighlightedId(prev => prev === p.productId ? null : p.productId)}
                        onDelete={() => deleteMut.mutate(p.id)}
                      />
                      {idx < filteredProducts.length - 1 && <Divider />}
                    </Box>
                  )
                })}
              </List>
            </SortableContext>
          </DndContext>
        </Box>

        {/* 右侧：商品卡片网格 */}
        <Box
          data-testid="live-select-grid-surface"
          data-contract-source="/live/product/by-session|/live/script/by-session"
          data-filtered-count={filteredProducts.length}
          sx={{ flex: 1, overflow: 'auto' }}
        >
          <ProductGridView
            products={filteredProducts}
            highlightedProductId={highlightedId}
            productScriptCounts={productScriptCounts}
            onProductClick={setHighlightedId}
          />
        </Box>
      </Box>

      <BatchProductDialog
        open={batchOpen}
        onClose={() => setBatchOpen(false)}
        onConfirm={handleBatchConfirm}
        existingProductIds={existingProductIds}
      />
    </Box>
  )
}


