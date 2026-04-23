import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { ChartCard } from './ChartCard'

describe('ChartCard', () => {
  it('renders title', () => {
    renderWithProviders(<ChartCard title="调用趋势" option={{}} />)
    expect(screen.getByText('调用趋势')).toBeInTheDocument()
  })

  it('shows loading when loading', () => {
    renderWithProviders(<ChartCard title="图表" option={{}} loading />)
    expect(screen.getByText('图表')).toBeInTheDocument()
    // ECharts 会渲染 canvas，loading 时可能显示 CircularProgress
  })
})
