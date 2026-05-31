import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import GmvCounter from '../GmvCounter'

describe('GmvCounter', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.useFakeTimers()
    let now = 0
    vi.spyOn(Date, 'now').mockImplementation(() => {
      now += 2
      return now
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('uses theme-aware value and flying digit colors in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <GmvCounter value={12000000} duration={1} />
      </AppThemeProvider>,
    )

    vi.runOnlyPendingTimers()

    expect(screen.getByTestId('gmv-counter-value-surface')).toHaveAttribute('data-gmv-tone', 'success')
    expect(screen.getByTestId('gmv-counter-value-surface')).toHaveAttribute('data-gmv-color', '#81c784')
    const flyingDigit = screen.getAllByTestId('gmv-counter-flying-digit-surface')[0]
    expect(flyingDigit).toHaveAttribute('data-gmv-color', '#81c784')
    expect(screen.getByTestId('gmv-counter-value-surface')).not.toHaveAttribute('data-gmv-color', '#4caf50')
  })

  it('maps medium and low GMV values to semantic tones', () => {
    const { rerender } = renderWithProviders(
      <AppThemeProvider>
        <GmvCounter value={6000000} duration={1} />
      </AppThemeProvider>,
    )
    vi.runOnlyPendingTimers()
    expect(screen.getByTestId('gmv-counter-value-surface')).toHaveAttribute('data-gmv-tone', 'warning')

    rerender(
      <AppThemeProvider>
        <GmvCounter value={1000000} duration={1} />
      </AppThemeProvider>,
    )
    vi.runOnlyPendingTimers()
    expect(screen.getByTestId('gmv-counter-value-surface')).toHaveAttribute('data-gmv-tone', 'text')
  })
})
