import React, { useMemo } from 'react'
import { Box, IconButton, Badge, Tooltip } from '@mui/material'
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
 * - 颜色：primary (#00D084)
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
              backgroundColor: '#00D084', // color-primary
              color: '#0F172A', // color-surface-dark
              fontSize: '12px', // font-size-sm
              fontWeight: 600, // font-weight-semibold
              height: '24px',
              minWidth: '24px',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '2px solid #1E293B', // color-surface
            },
          }}
        >
          <IconButton
            aria-label={ariaLabel}
            aria-expanded={ariaExpanded}
            onClick={onClick}
            sx={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: '#00D084', // color-primary
              color: '#0F172A', // color-surface-dark
              boxShadow: '0 12px 32px rgba(0, 0, 0, 0.4)', // shadow-elevation-3
              transition: 'all 200ms cubic-bezier(0, 0, 0.2, 1)', // transition-fast + ease-out
              '&:hover': {
                backgroundColor: '#00B36A', // color-primary-dark
                transform: 'scale(1.1)',
                boxShadow: '0 16px 40px rgba(0, 208, 132, 0.4)',
              },
              '&:active': {
                transform: 'scale(0.95)',
              },
              '&:focus-visible': {
                outline: `2px solid #00D084`,
                outlineOffset: '2px',
              },
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <ShoppingCartIcon sx={{ fontSize: '24px' }} />
          </IconButton>
        </Badge>
      </Box>
    </Tooltip>
  )
})

export default FloatButton
