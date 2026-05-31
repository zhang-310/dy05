import React, { useMemo } from 'react'
import { Box, IconButton, Badge, Tooltip } from '@mui/material'
import { alpha } from '@mui/material/styles'
import { ShoppingCart as ShoppingCartIcon } from '@mui/icons-material'

export interface FloatButtonProps {
  count?: number
  onClick?: () => void
  'aria-label'?: string
  'aria-expanded'?: boolean
}

/**
 * FloatButton - 购物车浮球按钮
 * 设计规范应用：
 * - 大小：48px 圆形
 * - 颜色：theme primary
 * - 阴影：elevation-3
 * - 动画：Hover 放大 1.1x (200ms)
 */
const FloatButton = React.memo(function FloatButton({
  count = 0,
  onClick,
  'aria-label': ariaLabel = '购物车',
  'aria-expanded': ariaExpanded = false,
}: FloatButtonProps) {
  const displayCount = useMemo(() => (count > 99 ? '99+' : count.toString()), [count])

  return (
    <Tooltip title={`购物车 (${count} 件商品)`}>
      <Box
        data-testid="shopping-cart-float-anchor"
        sx={{
          position: 'fixed',
          bottom: '20px',
          right: '20px',
          zIndex: 1000,
        }}
      >
        <Badge
          badgeContent={count > 0 ? displayCount : 0}
          color="primary"
          overlap="circular"
          sx={{
            '& .MuiBadge-badge': {
              backgroundColor: 'primary.main',
              color: 'primary.contrastText',
              fontSize: '12px', // font-size-sm
              fontWeight: 600, // font-weight-semibold
              height: '24px',
              minWidth: '24px',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '2px solid',
              borderColor: 'background.paper',
            },
          }}
        >
          <IconButton
            data-testid="shopping-cart-float-button"
            data-cart-tone="primary"
            aria-label={ariaLabel}
            aria-expanded={ariaExpanded}
            onClick={onClick}
            sx={(theme) => ({
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: theme.palette.primary.main,
              color: theme.palette.primary.contrastText,
              boxShadow: `0 12px 32px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.4 : 0.18)}`,
              transition: 'all 200ms cubic-bezier(0, 0, 0.2, 1)', // transition-fast + ease-out
              '&:hover': {
                backgroundColor: theme.palette.primary.dark,
                transform: 'scale(1.1)',
                boxShadow: `0 16px 40px ${alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.4 : 0.28)}`,
              },
              '&:active': {
                transform: 'scale(0.95)',
              },
              '&:focus-visible': {
                outline: `2px solid ${theme.palette.primary.main}`,
                outlineOffset: '2px',
              },
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            })}
          >
            <ShoppingCartIcon sx={{ fontSize: '24px' }} />
          </IconButton>
        </Badge>
      </Box>
    </Tooltip>
  )
})

export default FloatButton
