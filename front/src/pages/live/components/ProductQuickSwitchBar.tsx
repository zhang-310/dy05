import { memo } from 'react'
import { Box, Chip, Typography } from '@mui/material'
import type { LiveProduct } from '@/api/live'

export interface ProductQuickSwitchBarProps {
  products: LiveProduct[]
  highlightedProductId: number | null
  onProductClick: (productId: number) => void
}

export const ProductQuickSwitchBar = memo(function ProductQuickSwitchBar({
  products,
  highlightedProductId,
  onProductClick,
}: ProductQuickSwitchBarProps) {
  return (
    <Box
      data-testid="product-quick-switch-bar"
      data-contract-scope="live-product-quick-switch-props"
      data-contract-source="/live/product/by-session"
      data-unsupported-actions="local-product-fallback|product-mutation|script-mutation|direct-api-request"
      data-product-count={products.length}
      data-highlighted-product-id={highlightedProductId ?? ''}
      data-no-local-product-fallback="true"
      sx={{
        px: 2,
        py: 0.5,
        flexShrink: 0,
        display: 'flex',
        gap: 0.5,
        overflowX: 'auto',
        bgcolor: 'action.hover',
        borderTop: 1,
        borderBottom: 1,
        borderColor: 'divider',
        '&::-webkit-scrollbar': { height: 3 },
      }}
    >
      {products.length === 0 ? (
        <Typography
          variant="caption"
          color="text.secondary"
          data-testid="product-quick-switch-empty"
          data-contract-source="/live/product/by-session"
          data-no-local-product-fallback="true"
          sx={{ py: 0.25 }}
        >
          暂无可快速切换的商品
        </Typography>
      ) : products.map((p, idx) => {
        const pid = p.productId as number
        const isHighlighted = highlightedProductId === pid
        return (
          <Chip
            key={String(p.id ?? idx)}
            data-testid={isHighlighted ? 'product-quick-switch-active-chip' : 'product-quick-switch-chip'}
            data-contract-source="onProductClick-prop"
            data-contract-product-id={pid}
            label={String(p.productName ?? '-')}
            size="small"
            variant={isHighlighted ? 'filled' : 'outlined'}
            color={isHighlighted ? 'primary' : 'default'}
            onClick={() => onProductClick(pid)}
            sx={{ flexShrink: 0, bgcolor: isHighlighted ? undefined : 'background.paper' }}
          />
        )
      })}
    </Box>
  )
})
