import { useMediaQuery, useTheme } from '@mui/material'

/**
 * 响应式断点 hook：统一移动端/平板/桌面判断
 */
export function useResponsive() {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))
  const isTablet = useMediaQuery(theme.breakpoints.between('sm', 'md'))
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'))

  return { isMobile, isTablet, isDesktop }
}
