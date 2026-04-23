import { renderHook } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { useResponsive } from '../useResponsive'
import { ThemeProvider, createTheme } from '@mui/material'

// Mock useMediaQuery
vi.mock('@mui/material', async () => {
  const actual = await vi.importActual('@mui/material')
  return {
    ...actual,
    useMediaQuery: vi.fn(),
  }
})

import { useMediaQuery } from '@mui/material'

describe('useResponsive', () => {
  const theme = createTheme()
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <ThemeProvider theme={theme}>{children}</ThemeProvider>
  )

  it('returns isMobile true on small screens', () => {
    vi.mocked(useMediaQuery).mockImplementation((query) => {
      if (query === theme.breakpoints.down('sm')) return true
      return false
    })

    const { result } = renderHook(() => useResponsive(), { wrapper })

    expect(result.current.isMobile).toBe(true)
    expect(result.current.isTablet).toBe(false)
    expect(result.current.isDesktop).toBe(false)
  })

  it('returns isTablet true on medium screens', () => {
    vi.mocked(useMediaQuery).mockImplementation((query) => {
      if (query === theme.breakpoints.between('sm', 'md')) return true
      return false
    })

    const { result } = renderHook(() => useResponsive(), { wrapper })

    expect(result.current.isMobile).toBe(false)
    expect(result.current.isTablet).toBe(true)
    expect(result.current.isDesktop).toBe(false)
  })

  it('returns isDesktop true on large screens', () => {
    vi.mocked(useMediaQuery).mockImplementation((query) => {
      if (query === theme.breakpoints.up('md')) return true
      return false
    })

    const { result } = renderHook(() => useResponsive(), { wrapper })

    expect(result.current.isMobile).toBe(false)
    expect(result.current.isTablet).toBe(false)
    expect(result.current.isDesktop).toBe(true)
  })
})
