import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { EffectivenessTrendChart } from '../EffectivenessTrendChart'
import type { TrendAnalysis, TrendPoint } from '@/types/effectiveness'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

const trendData: TrendPoint[] = [
  {
    date: '2026-05-20',
    score: 82,
    scoreLevel: 'B',
    usageCount: 42,
    conversionRate: 0.18,
    likesCount: 120,
  },
  {
    date: '2026-05-21',
    score: 88,
    scoreLevel: 'A',
    usageCount: 68,
    conversionRate: 0.23,
    likesCount: 180,
  },
]

const trendAnalysis: TrendAnalysis = {
  overallTrend: 'up',
  scoreChange: 6.2,
  highestDate: '2026-05-21',
  lowestDate: '2026-05-20',
  volatility: 0.12,
}

describe('EffectivenessTrendChart', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware analysis and chart colors in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <EffectivenessTrendChart data={trendData} analysis={trendAnalysis} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('effectiveness-trend-icon-surface')).toHaveAttribute('data-trend-color', '#81c784')
    expect(screen.getByTestId('effectiveness-score-change-card-surface')).toHaveAttribute('data-card-color', '#81c784')
    expect(screen.getByTestId('effectiveness-score-change-value-surface')).toHaveAttribute('data-score-tone', 'success')
    expect(screen.getByTestId('effectiveness-score-trend-chart-surface')).toHaveAttribute('data-chart-color', '#f3e5f5')
    expect(screen.getByTestId('effectiveness-usage-conversion-chart-surface')).toHaveAttribute('data-chart-colors', '#81c784|#e3f2fd')
    expect(screen.getByTestId('effectiveness-likes-chart-surface')).toHaveAttribute('data-chart-color', '#ffb74d')

    const serializedCharts = screen.getAllByTestId('mock-echarts').map(node => node.textContent ?? '').join('\n')
    for (const legacy of ['#4caf50', '#f44336', '#ff9800', '#ff7043', '#2196f3']) {
      expect(serializedCharts).not.toContain(legacy)
    }
  })

  it('keeps the empty state when no trend data is available', () => {
    renderWithProviders(<EffectivenessTrendChart data={[]} analysis={null} />)

    expect(screen.getByText('暂无趋势数据')).toBeInTheDocument()
  })
})
