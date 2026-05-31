import { describe, expect, it } from 'vitest'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { KpiCard } from './KpiCard'

describe('KpiCard', () => {
  it('renders title, value, subtitle, and icon', () => {
    renderWithProviders(<KpiCard title="GMV" value="12.8万" subtitle="较昨日 +12%" icon={<TrendingUpIcon />} />)

    expect(screen.getByText('GMV')).toBeInTheDocument()
    expect(screen.getByText('12.8万')).toBeInTheDocument()
    expect(screen.getByText('较昨日 +12%')).toBeInTheDocument()
    expect(screen.getByTestId('base-kpi-card-icon')).toBeInTheDocument()
  })

  it('uses theme primary tone by default and clears legacy fixed colors', () => {
    renderWithProviders(<KpiCard title="转化率" value={18.2} subtitle="较昨日 +2%" />)

    const surface = screen.getByTestId('base-kpi-card-surface')
    expect(surface).toHaveAttribute('data-kpi-tone', 'primary')
    expect(screen.getByTestId('base-kpi-card-title')).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(screen.getByTestId('base-kpi-card-subtitle')).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(surface.outerHTML).not.toContain('#1E293B')
    expect(surface.outerHTML).not.toContain('#334155')
    expect(surface.outerHTML).not.toContain('#475569')
    expect(surface.outerHTML).not.toContain('#00D084')
  })

  it('keeps explicit custom color support for domain-specific cards', () => {
    renderWithProviders(<KpiCard title="异常数" value={3} color="#d32f2f" />)

    const surface = screen.getByTestId('base-kpi-card-surface')
    expect(surface).toHaveAttribute('data-kpi-tone', 'custom')
    expect(screen.getByTestId('base-kpi-card-value')).toHaveStyle({ color: 'rgb(211, 47, 47)' })
  })
})
