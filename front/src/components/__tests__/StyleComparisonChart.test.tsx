import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { StyleComparisonChart } from '../StyleComparisonChart'
import type { StyleMetrics } from '@/types/effectiveness'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => <div data-testid="mock-echarts">{JSON.stringify(option)}</div>,
}))

const styles: StyleMetrics[] = [
  { style: '专业型', versionCount: 3, avgScore: 91, avgConversion: 0.24, topVersion: { versionNumber: 4, score: 94 } },
  { style: '情绪型', versionCount: 2, avgScore: 72, avgConversion: 0.18, topVersion: { versionNumber: 2, score: 78 } },
  { style: '促销型', versionCount: 1, avgScore: 48, avgConversion: 0.1, topVersion: { versionNumber: 1, score: 48 } },
]

describe('StyleComparisonChart', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware chart, table and summary surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    const onStyleClick = vi.fn()

    renderWithProviders(
      <AppThemeProvider>
        <StyleComparisonChart styles={styles} onStyleClick={onStyleClick} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('style-comparison-chart-surface')).toHaveAttribute('data-chart-colors', '#f3e5f5|#e3f2fd')
    expect(screen.getByTestId('style-comparison-table-head-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })
    expect(screen.getByTestId('style-comparison-max-score-cell-surface')).not.toHaveStyle({ backgroundColor: 'rgb(255, 243, 224)' })

    const progressBars = screen.getAllByTestId('style-comparison-progress-surface')
    expect(progressBars.map(node => node.getAttribute('data-score-tone'))).toEqual(['success', 'warning', 'error'])
    expect(progressBars.map(node => node.getAttribute('data-score-color'))).toEqual(['#81c784', '#ffb74d', '#e57373'])
    expect(screen.getAllByTestId('style-comparison-summary-surface')).toHaveLength(5)

    const serialized = document.body.innerHTML
    const chartText = screen.getByTestId('mock-echarts').textContent ?? ''
    for (const legacy of ['#ff7043', '#2196f3', '#4caf50', '#ff9800', '#f44336', '#f5f5f5', '#fff3e0', '#ffebee', '#e0e0e0']) {
      expect(serialized).not.toContain(legacy)
      expect(chartText).not.toContain(legacy)
    }

    fireEvent.click(screen.getAllByText('专业型')[0])
    expect(onStyleClick).toHaveBeenCalledWith('专业型')
  })

  it('keeps the empty state when no style data exists', () => {
    renderWithProviders(<StyleComparisonChart styles={[]} />)

    expect(screen.getByText('暂无风格数据')).toBeInTheDocument()
  })
})
