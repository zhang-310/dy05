import { memo } from 'react'
import { Box, Chip } from '@mui/material'
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
  if (products.length === 0) return null

  return (
    <Box
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
      {products.map((p, idx) => {
        const pid = p.productId as number
        const isHighlighted = highlightedProductId === pid
        return (
          <Chip
            key={String(p.id ?? idx)}
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
