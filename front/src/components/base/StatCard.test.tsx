import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/utils'
import { StatCard } from './StatCard'

describe('StatCard', () => {
  it('renders title and value', () => {
    renderWithProviders(<StatCard title="总数" value={100} />)
    expect(screen.getByText('总数')).toBeInTheDocument()
    expect(screen.getByText('100')).toBeInTheDocument()
  })

  it('renders unit when provided', () => {
    renderWithProviders(<StatCard title="金额" value={99} unit="元" />)
    expect(screen.getByText('99')).toBeInTheDocument()
    expect(screen.getByText('元')).toBeInTheDocument()
  })

  it('calls onClick when clicked', async () => {
    const onClick = vi.fn()
    const user = userEvent.setup()
    renderWithProviders(<StatCard title="可点击" value={1} onClick={onClick} />)
    await user.click(screen.getByText('1'))
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('uses theme surface tone and removes legacy fixed colors', () => {
    renderWithProviders(<StatCard title="转化率" value={12.5} unit="%" trend={{ value: 8, label: '较昨日' }} color="success" />)

    const surface = screen.getByTestId('base-stat-card-surface')
    expect(surface).toHaveAttribute('data-stat-tone', 'success')
    expect(screen.getByTestId('base-stat-card-title')).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(screen.getByTestId('base-stat-card-value')).toHaveStyle({ color: 'rgba(0, 0, 0, 0.87)' })
    expect(screen.getByTestId('base-stat-card-unit')).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(screen.getByTestId('base-stat-card-trend-label')).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(surface.outerHTML).not.toContain('#1E293B')
    expect(surface.outerHTML).not.toContain('#334155')
    expect(surface.outerHTML).not.toContain('#F1F5F9')
    expect(surface.outerHTML).not.toContain('#475569')
    expect(surface.outerHTML).not.toContain('#00D084')
  })
})
