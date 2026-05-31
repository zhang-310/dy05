import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { SearchAnalyticsChart } from '../SearchAnalyticsChart'
import type { SearchAnalyticsVO } from '@/types/search'

const setOption = vi.fn()
const resize = vi.fn()
const dispose = vi.fn()

vi.mock('echarts', () => ({
  init: vi.fn(() => ({ setOption, resize, dispose })),
  graphic: {
    LinearGradient: vi.fn((x0, y0, x1, y1, colorStops) => ({
      type: 'linear',
      x0,
      y0,
      x1,
      y1,
      colorStops,
    })),
  },
}))

const analytics: SearchAnalyticsVO = {
  period: 'week',
  totalSearches: 1280,
  uniqueUsers: 96,
  averageResultsReturned: 18,
  averageClickThroughRate: 42.6,
  topSearchQueries: [
    { query: '修护精华', count: 320, clickThroughRate: 47, averageResultsViewed: 6 },
    { query: '直播间福利', count: 220, clickThroughRate: 39, averageResultsViewed: 5 },
    { query: '短视频脚本', count: 180, clickThroughRate: 35, averageResultsViewed: 4 },
  ],
  searchTrend: [
    { timestamp: '2026-05-20T00:00:00', count: 120 },
    { timestamp: '2026-05-21T00:00:00', count: 180 },
    { timestamp: '2026-05-22T00:00:00', count: 260 },
  ],
  resultQualityScores: {
    excellent: 42,
    good: 35,
    fair: 18,
    poor: 5,
  },
}

describe('SearchAnalyticsChart', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware trend, keyword, quality and stat surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <SearchAnalyticsChart analytics={analytics} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('search-analytics-trend-chart-surface')).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(screen.getAllByTestId('search-analytics-stat-surface').map(node => node.getAttribute('data-stat-tone'))).toEqual([
      'primary',
      'success',
      'warning',
      'secondary',
    ])
    expect(screen.getAllByTestId('search-analytics-stat-surface').map(node => node.getAttribute('data-stat-color'))).toEqual([
      '#e3f2fd',
      '#81c784',
      '#ffb74d',
      '#f3e5f5',
    ])

    fireEvent.click(screen.getByRole('button', { name: '热词' }))
    await waitFor(() => expect(screen.getByTestId('search-analytics-keywords-chart-surface')).toHaveAttribute('data-chart-colors', '#e3f2fd|#f3e5f5'))

    fireEvent.click(screen.getByRole('button', { name: '质量分布' }))
    await waitFor(() => expect(screen.getByTestId('search-analytics-quality-chart-surface')).toHaveAttribute('data-chart-colors', '#81c784|#e3f2fd|#ffb74d|#e57373'))

    const serialized = document.body.innerHTML
    for (const legacy of ['#667eea', '#764ba2', '#4caf50', '#2196f3', '#ff9800', '#f44336', '#f0f0f0', '#999']) {
      expect(serialized).not.toContain(legacy)
    }
    const options = setOption.mock.calls.map(call => JSON.stringify(call[0])).join('\n')
    for (const legacy of ['#667eea', '#764ba2', '#4caf50', '#2196f3', '#ff9800', '#f44336', '#f0f0f0']) {
      expect(options).not.toContain(legacy)
    }
  })

  it('keeps loading, error and empty states explicit', () => {
    const { rerender } = renderWithProviders(<SearchAnalyticsChart analytics={null} isLoading />)
    expect(screen.getByRole('progressbar')).toBeInTheDocument()

    rerender(<SearchAnalyticsChart analytics={null} error="搜索分析接口失败" />)
    expect(screen.getByText('搜索分析接口失败')).toBeInTheDocument()

    rerender(<SearchAnalyticsChart analytics={null} />)
    expect(screen.getByText('暂无分析数据')).toBeInTheDocument()
  })
})
