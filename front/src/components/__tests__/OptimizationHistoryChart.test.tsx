import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { OptimizationHistoryChart } from '../OptimizationHistoryChart'
import type { EvolutionMetricsVO } from '@/types/optimization'

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

const metrics: EvolutionMetricsVO = {
  versionId: 18,
  totalAnalysis: 24,
  totalSuggestions: 68,
  acceptedSuggestions: 41,
  rejectedSuggestions: 11,
  appliedRegenerations: 9,
  averageScoreImprovement: 12.35,
  latestEffectivenessScore: 86.8,
  optimizationSuccessRate: 72.5,
  lastOptimizedAt: '2026-05-22T10:00:00',
  evolutionTrend: [
    { timestamp: '2026-05-20T00:00:00', overallScore: 68, suggestionsApplied: 6, averageImprovement: 4.2 },
    { timestamp: '2026-05-21T00:00:00', overallScore: 76, suggestionsApplied: 9, averageImprovement: 7.6 },
    { timestamp: '2026-05-22T00:00:00', overallScore: 86, suggestionsApplied: 12, averageImprovement: 12.4 },
  ],
}

describe('OptimizationHistoryChart', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware chart and stat surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <OptimizationHistoryChart metrics={metrics} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('optimization-history-chart-surface')).toHaveAttribute('data-chart-colors', '#e3f2fd|#81c784|#f3e5f5')
    expect(screen.getAllByTestId('optimization-history-stat-surface').map(node => node.getAttribute('data-stat-tone'))).toEqual([
      'primary',
      'success',
      'secondary',
      'warning',
    ])
    expect(screen.getAllByTestId('optimization-history-stat-surface').map(node => node.getAttribute('data-stat-color'))).toEqual([
      '#e3f2fd',
      '#81c784',
      '#f3e5f5',
      '#ffb74d',
    ])

    const serialized = document.body.innerHTML
    for (const legacy of ['#6a7985', '#667eea', '#e0e0e0', '#764ba2', '#4ecdc4', '#4caf50', '#ff9800', '#f0f0f0', '#999']) {
      expect(serialized).not.toContain(legacy)
    }
    const options = setOption.mock.calls.map(call => JSON.stringify(call[0])).join('\n')
    for (const legacy of ['#6a7985', '#667eea', '#e0e0e0', '#764ba2', '#4ecdc4', '#4caf50', '#ff9800']) {
      expect(options).not.toContain(legacy)
    }
  })

  it('keeps loading, error and empty-trend states explicit', () => {
    const { rerender } = renderWithProviders(<OptimizationHistoryChart metrics={null} isLoading />)
    expect(screen.getByRole('progressbar')).toBeInTheDocument()

    rerender(<OptimizationHistoryChart metrics={null} error="优化历史接口失败" />)
    expect(screen.getByText('优化历史接口失败')).toBeInTheDocument()

    rerender(<OptimizationHistoryChart metrics={{ ...metrics, evolutionTrend: [] }} />)
    expect(screen.getByText('暂无优化历史数据')).toBeInTheDocument()
  })
})
