import { memo } from 'react'
import {
  Box, Card, CardActionArea, CardContent, CardMedia,
  Chip, Typography, Tooltip,
} from '@mui/material'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { PRODUCT_TYPE_OPTIONS, parseProductTypes } from './constants'
import { cdnThumb } from '@/utils/cdnImage'
import type { LiveProduct } from '@/api/live-product'

export interface ProductGridViewProps {
  products: LiveProduct[]
  highlightedProductId?: number | null
  productScriptCounts?: Map<number, number>
  onProductClick?: (productId: number) => void
}

export const ProductGridView = memo(function ProductGridView({
  products,
  highlightedProductId,
  productScriptCounts,
  onProductClick,
}: ProductGridViewProps) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
        gap: 1.5,
        p: 2,
      }}
    >
      {products.map((p) => {
        const isActive = highlightedProductId === p.productId
        const types = parseProductTypes(p.productType)
        const typeOptions = types
          .map(t => PRODUCT_TYPE_OPTIONS.find(o => o.value === t))
          .filter(Boolean) as (typeof PRODUCT_TYPE_OPTIONS)[number][]
        const scriptCount = productScriptCounts?.get(p.productId) ?? 0
        const imgSrc = cdnThumb(p.imageUrl, 300, 250)
        const price = Number(p.price ?? 0)

        const missingFields: string[] = []
        if (!p.aiSellingPoints) missingFields.push('卖点')
        if (p.profitMarginPct == null && p.lossPerUnit == null) missingFields.push('利润/亏损')
        if (!p.productCategory) missingFields.push('分类')

        return (
          <Card
            key={p.id}
            variant="outlined"
            sx={{
              display: 'flex',
              flexDirection: 'column',
              borderRadius: 2,
              overflow: 'hidden',
              transition: 'box-shadow 0.2s, border-color 0.2s, transform 0.15s',
              ...(isActive
                ? { borderColor: 'primary.main', boxShadow: '0 0 0 1px rgba(25,118,210,0.3)' }
                : { '&:hover': { boxShadow: 2, transform: 'translateY(-1px)' } }),
            }}
          >
            <CardActionArea
              onClick={() => onProductClick?.(p.productId)}
              sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'stretch', position: 'relative' }}
            >
              {/* 图片 */}
              {imgSrc ? (
                <CardMedia
                  component="img"
                  image={imgSrc}
                  alt={p.productName}
                  sx={{ height: 120, objectFit: 'cover' }}
                />
              ) : (
                <Box sx={{ height: 120, bgcolor: 'grey.100', display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: '1px solid', borderColor: 'divider' }}>
                  <Typography variant="caption" color="text.disabled">无图片</Typography>
                </Box>
              )}

              {/* 完整度警告 */}
              {missingFields.length > 0 && (
                <Box sx={{ position: 'absolute', top: 0, left: 0, right: 0, bgcolor: 'rgba(237,108,2,0.9)', color: '#fff', display: 'flex', alignItems: 'center', gap: 0.5, px: 1, py: 0.25 }}>
                  <WarningAmberIcon sx={{ fontSize: 14 }} />
                  <Typography variant="caption" sx={{ fontSize: '0.72rem', fontWeight: 600 }}>缺: {missingFields.join('、')}</Typography>
                </Box>
              )}

              <CardContent sx={{ p: 1.25, '&:last-child': { pb: 1.25 }, flex: 1, display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                {/* 商品名 */}
                <Tooltip title={p.productName}>
                  <Typography variant="body2" fontWeight={600} sx={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden', lineHeight: 1.3, fontSize: '0.8rem' }}>
                    {p.productName}
                  </Typography>
                </Tooltip>

                {/* 类型标签 */}
                {typeOptions.length > 0 && (
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                    {typeOptions.map(opt => (
                      <Chip key={opt.value} label={opt.label} size="small" color={opt.color} sx={{ height: 18, fontSize: '0.7rem', '& .MuiChip-label': { px: 0.5 } }} />
                    ))}
                  </Box>
                )}

                {/* 价格 */}
                {price > 0 && (
                  <Typography variant="caption" color="error" fontWeight={700}>¥{price}</Typography>
                )}

                {/* 卖点 */}
                {p.aiSellingPoints && (
                  <Tooltip title={p.aiSellingPoints}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.75rem', lineHeight: 1.3, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {p.aiSellingPoints}
                    </Typography>
                  </Tooltip>
                )}

                {/* 话术状态 */}
                <Box sx={{ mt: 'auto', pt: 0.5 }}>
                  <Chip
                    label={scriptCount > 0 ? `${scriptCount} 条话术` : '待生成'}
                    size="small"
                    color={scriptCount > 0 ? 'success' : 'default'}
                    variant={scriptCount > 0 ? 'filled' : 'outlined'}
                    sx={{ height: 20, fontSize: '0.72rem', '& .MuiChip-label': { px: 0.5 } }}
                  />
                </Box>
              </CardContent>
            </CardActionArea>
          </Card>
        )
      })}

      {products.length === 0 && (
        <Box sx={{ gridColumn: '1 / -1', textAlign: 'center', py: 8, color: 'text.secondary' }}>
          <Typography variant="body2">暂无商品</Typography>
        </Box>
      )}
    </Box>
  )
})
