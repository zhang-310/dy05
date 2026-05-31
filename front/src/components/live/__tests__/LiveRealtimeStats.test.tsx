import { describe, it, expect, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import LiveRealtimeStats from '../LiveRealtimeStats'
import type { LiveSessionRealtimeDataVO } from '@/types/live-realtime'

const data: LiveSessionRealtimeDataVO = {
  id: 1,
  liveSessionId: 18,
  watchedCount: 18200,
  viewerCount: 960,
  likeCount: 38000,
  commentCount: 1280,
  shareCount: 240,
  followCount: 430,
  giftAmount: 1234.5,
  productClickCount: 9800,
  productPurchaseCount: 128,
  productPurchaseAmount: 6890.75,
  currentSlotIndex: 2,
}

describe('LiveRealtimeStats', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('uses theme-aware stat card surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <LiveRealtimeStats data={data} />
      </AppThemeProvider>,
    )

    const cards = screen.getAllByTestId('live-realtime-stat-card-surface')
    expect(cards.map(node => node.getAttribute('data-stat-tone'))).toEqual([
      'secondary',
      'info',
      'warning',
      'secondary',
      'info',
      'success',
      'success',
      'warning',
      'error',
      'error',
    ])
    expect(cards.map(node => node.getAttribute('data-stat-color'))).toEqual([
      '#f3e5f5',
      '#4fc3f7',
      '#ffb74d',
      '#f3e5f5',
      '#4fc3f7',
      '#81c784',
      '#81c784',
      '#ffb74d',
      '#e57373',
      '#e57373',
    ])
    expect(screen.getByText('1.8万')).toBeInTheDocument()
    expect(screen.getByText('¥6890.75')).toBeInTheDocument()

    const serialized = document.body.innerHTML
    for (const legacy of ['#1976d2', '#f5f5f5', '#ff6b6b', '#4ecdc4', '#ffd93d', '#95e1d3', '#a8e6cf', '#ffd3b6', '#ffaaa5', '#ff8b94']) {
      expect(serialized).not.toContain(legacy)
    }
  })
})
