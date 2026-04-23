import { useState, useCallback, useMemo } from 'react'
import {
  Box, Button, Typography, Chip, IconButton, List, ListItem,
  ListItemText, ListItemSecondaryAction, Divider, Alert,
  Tooltip,
} from '@mui/material'
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

function applySortStrategy(products: LiveProduct[], strategy: SortStrategy): LiveProduct[] {
  if (strategy === 'manual') return products
  const sorted = [...products]
  switch (strategy) {
    case 'type': {
      const typeOrder: Record<string, number> = { hot: 1, control: 2, profit: 3, loss: 4, flat: 5 }
      sorted.sort((a, b) => {
        const ta = Math.min(...parseProductTypes(a.productType).map(t => typeOrder[t] ?? 99), 99)
        const tb = Math.min(...parseProductTypes(b.productType).map(t => typeOrder[t] ?? 99), 99)
        return ta - tb
      })
      break
    }
    case 'alpha':
      sorted.sort((a, b) => (a.productName ?? '').localeCompare(b.productName ?? '', 'zh-Hans'))
      break
    case 'price':
      sorted.sort((a, b) => Number(b.price ?? 0) - Number(a.price ?? 0))
      break
  }
  return sorted
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

  const smartSort = useSmartSort({
    sessionId,
    products,
    onApplied: refetchProducts,
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.productDelete(id),
    onSuccess: () => { toast('已移除商品', 'success'); refetchProducts() },
    onError: (e: Error) => toast(e.message, 'error'),
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
    onError: (e: Error) => toast(e.message, 'error'),
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

  const strategySorted = useMemo(() => applySortStrategy(products, sortStrategy), [products, sortStrategy])

  const filteredProducts = useMemo(() => {
    let list = activeTypeFilter
      ? strategySorted.filter(p => parseProductTypes(p.productType).includes(activeTypeFilter))
      : strategySorted
    if (reversed) list = [...list].reverse()
    return list
  }, [strategySorted, activeTypeFilter, reversed])

  const [highlightedId, setHighlightedId] = useState<number | null>(null)

  if (products.length === 0) {
    return (
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2, py: 8 }}>
        <Typography variant="h6" color="text.secondary">添加商品开始构建话术</Typography>
        <Typography variant="body2" color="text.secondary">从商品库选择，支持单选和批量添加</Typography>
        <Button variant="contained" startIcon={<AddCircleOutlineIcon />} onClick={() => setBatchOpen(true)}>
          批量添加商品
        </Button>
        <BatchProductDialog open={batchOpen} onClose={() => setBatchOpen(false)} onConfirm={handleBatchConfirm} existingProductIds={existingProductIds} />
      </Box>
    )
  }
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      {/* 工具栏 */}
      <Box sx={{ px: 2, py: 1, flexShrink: 0, borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper', display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}>
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
            variant={activeTypeFilter === null ? 'filled' : 'outlined'}
            color={activeTypeFilter === null ? 'primary' : 'default'}
            onClick={() => setActiveTypeFilter(null)}
            sx={{ cursor: 'pointer' }}
          />
          {Object.entries(typeDistribution).map(([type, count]) => {
            const opt = PRODUCT_TYPE_OPTIONS.find(o => o.value === type)
            return (
              <Chip key={type}
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
          <Button size="small" variant="outlined" startIcon={<AddCircleOutlineIcon />} onClick={() => setBatchOpen(true)}>
            批量添加
          </Button>
          {onNext && (
            <Button size="small" variant="contained" endIcon={<ArrowForwardIcon />} onClick={onNext} disabled={products.length === 0}>
              下一步：生成
            </Button>
          )}
        </Box>
      </Box>

      {/* AI排品建议条 */}
      {smartSort.suggestion && (
        <Alert
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

      {/* 主体：左侧列表 + 右侧卡片 */}
      <Box sx={{ flex: 1, display: 'flex', minHeight: 0, overflow: 'hidden' }}>
        {/* 左侧：拖拽列表 */}
        <Box sx={{ width: 320, flexShrink: 0, display: 'flex', flexDirection: 'column', borderRight: '1px solid', borderColor: 'divider', overflow: 'hidden' }}>
          <List dense sx={{ flex: 1, overflow: 'auto', p: 0 }}>
            {filteredProducts.map((p, idx) => {
              const types = parseProductTypes(p.productType)
              const typeOpts = types.map(t => PRODUCT_TYPE_OPTIONS.find(o => o.value === t)).filter(Boolean) as (typeof PRODUCT_TYPE_OPTIONS)[number][]
              return (
                <Box key={p.id}>
                  <ListItem
                    sx={{ px: 1.5, py: 0.75, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' }, bgcolor: highlightedId === p.productId ? 'primary.50' : 'transparent' }}
                    onClick={() => setHighlightedId(prev => prev === p.productId ? null : p.productId)}
                  >
                    <Tooltip title="拖拽排序">
                      <DragIndicatorIcon sx={{ color: 'text.disabled', mr: 0.5, cursor: 'grab', fontSize: 18 }} />
                    </Tooltip>
                    <Typography variant="caption" sx={{ mr: 1, color: 'text.secondary', minWidth: 18 }}>{idx + 1}</Typography>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <Typography variant="body2" noWrap sx={{ maxWidth: 130 }}>{p.productName}</Typography>
                          {typeOpts.map(opt => (
                            <Chip key={opt.value} label={opt.label} size="small" color={opt.color} sx={{ height: 16, fontSize: '0.68rem', '& .MuiChip-label': { px: 0.4 } }} />
                          ))}
                        </Box>
                      }
                      secondary={p.price ? `¥${p.price}` : ''}
                    />
                    <ListItemSecondaryAction>
                      <Tooltip title="移除">
                        <IconButton size="small" color="error" onClick={() => deleteMut.mutate(p.id)} disabled={deleteMut.isPending}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </ListItemSecondaryAction>
                  </ListItem>
                  {idx < filteredProducts.length - 1 && <Divider />}
                </Box>
              )
            })}
          </List>
        </Box>

        {/* 右侧：商品卡片网格 */}
        <Box sx={{ flex: 1, overflow: 'auto' }}>
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


