import React, { useState, useCallback, useMemo } from 'react'
import { Box, Drawer, Typography, Button, List, ListItem, ListItemText, Divider } from '@mui/material'
import { ShoppingCart as ShoppingCartIcon, Close as CloseIcon } from '@mui/icons-material'
import FloatButton from './FloatButton'

export interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
  image?: string
}

export interface ShoppingCartFloatProps {
  items?: CartItem[]
  total?: number
  onCheckout?: () => void
  onAddItem?: (item: CartItem) => void
  onRemoveItem?: (itemId: string) => void
  onUpdateQuantity?: (itemId: string, quantity: number) => void
}

/**
 * ShoppingCartFloat - 购物车浮窗组件
 * 设计规范应用：
 * - 浮球：48px 圆形，elevation-3 阴影，右下角固定
 * - 面板：从右滑入，elevation-3 阴影
 * - 动画：300ms 缓入缓出，购物车数字动画 200ms
 */
export const ShoppingCartFloat = React.memo(function ShoppingCartFloat({
  items = [],
  onCheckout,
}: ShoppingCartFloatProps) {
  const [isOpen, setIsOpen] = useState(false)

  const totalItems = useMemo(() => items.reduce((sum, item) => sum + item.quantity, 0), [items])
  const calculatedTotal = useMemo(
    () => items.reduce((sum, item) => sum + item.price * item.quantity, 0),
    [items]
  )

  const handleOpen = useCallback(() => {
    setIsOpen(true)
  }, [])

  const handleClose = useCallback(() => {
    setIsOpen(false)
  }, [])

  const handleCheckout = useCallback(() => {
    onCheckout?.()
  }, [onCheckout])

  return (
    <Box>
      {/* 浮球按钮 */}
      <FloatButton
        count={totalItems}
        onClick={handleOpen}
        aria-label="打开购物车"
        aria-expanded={isOpen}
      />

      {/* 购物车面板 */}
      <Drawer
        anchor="right"
        open={isOpen}
        onClose={handleClose}
        PaperProps={{
          sx: {
            width: { xs: '100%', sm: 400 },
            backgroundColor: '#1E293B', // color-surface
            boxShadow: '0 12px 32px rgba(0, 0, 0, 0.4)', // shadow-elevation-3
          },
        }}
        SlideProps={{
          timeout: 300, // transition-base
        }}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          {/* 头部 */}
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '24px', // spacing-lg
              borderBottom: '1px solid #334155', // color-surface-light
            }}
          >
            <Typography
              variant="h6"
              sx={{
                fontSize: '18px', // font-size-lg
                fontWeight: 600, // font-weight-semibold
                color: '#F1F5F9', // color-text-primary
              }}
            >
              购物车
            </Typography>
            <Button
              onClick={handleClose}
              sx={{
                minWidth: 0,
                padding: 0,
                color: '#475569', // color-text-secondary
                '&:hover': { color: '#F1F5F9' },
              }}
            >
              <CloseIcon />
            </Button>
          </Box>

          {/* 购物车列表 */}
          <Box sx={{ flex: 1, overflowY: 'auto', padding: '16px' }}>
            {items.length === 0 ? (
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  height: '100%',
                  color: '#475569', // color-text-secondary
                }}
              >
                <ShoppingCartIcon sx={{ fontSize: 48, mb: 1, opacity: 0.5 }} />
                <Typography>购物车为空</Typography>
              </Box>
            ) : (
              <List disablePadding>
                {items.map((item, index) => (
                  <React.Fragment key={item.id}>
                    <ListItem
                      sx={{
                        padding: '12px 16px', // spacing-md
                        backgroundColor: '#0F172A', // color-surface-dark
                        borderRadius: '8px', // border-radius-lg
                        marginBottom: '8px', // spacing-sm
                        display: 'flex',
                        justifyContent: 'space-between',
                      }}
                    >
                      <ListItemText
                        primary={item.name}
                        secondary={`¥${item.price.toFixed(2)} × ${item.quantity}`}
                        primaryTypographyProps={{
                          sx: {
                            fontSize: '14px', // font-size-base
                            color: '#F1F5F9', // color-text-primary
                            fontWeight: 500,
                          },
                        }}
                        secondaryTypographyProps={{
                          sx: {
                            fontSize: '12px', // font-size-sm
                            color: '#475569', // color-text-secondary
                          },
                        }}
                      />
                      <Typography
                        sx={{
                          fontSize: '14px',
                          fontWeight: 600,
                          color: '#00D084', // color-primary
                        }}
                      >
                        ¥{(item.price * item.quantity).toFixed(2)}
                      </Typography>
                    </ListItem>
                    {index < items.length - 1 && (
                      <Divider
                        sx={{
                          backgroundColor: '#334155', // color-surface-light
                          margin: '8px 0', // spacing-sm
                      }}
                    />
                    )}
                  </React.Fragment>
                ))}
              </List>
            )}
          </Box>

          {/* 底部 - 合计和结算 */}
          {items.length > 0 && (
            <Box
              sx={{
                padding: '24px', // spacing-lg
                borderTop: '1px solid #334155', // color-surface-light
                backgroundColor: '#0F172A', // color-surface-dark
              }}
            >
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '16px', // spacing-md
                }}
              >
                <Typography
                  sx={{
                    fontSize: '14px', // font-size-base
                    color: '#475569', // color-text-secondary
                  }}
                >
                  小计：
                </Typography>
                <Typography
                  sx={{
                    fontSize: '18px', // font-size-lg
                    fontWeight: 700, // font-weight-bold
                    color: '#00D084', // color-primary
                  }}
                >
                  ¥{calculatedTotal.toFixed(2)}
                </Typography>
              </Box>
              <Button
                fullWidth
                variant="contained"
                color="primary"
                onClick={handleCheckout}
                sx={{
                  minHeight: '44px',
                  borderRadius: '8px', // border-radius-lg
                  fontWeight: 600,
                  fontSize: '14px', // font-size-base
                  transition: 'all 200ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-fast + ease-in-out
                }}
              >
                前往支付
              </Button>
            </Box>
          )}
        </Box>
      </Drawer>
    </Box>
  )
})

export default ShoppingCartFloat
