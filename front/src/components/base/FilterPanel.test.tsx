import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/utils'
import { FilterPanel } from './FilterPanel'

describe('FilterPanel', () => {
  it('renders children', () => {
    renderWithProviders(
      <FilterPanel>
        <span data-testid="child">筛选内容</span>
      </FilterPanel>
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
    expect(screen.getByText('筛选内容')).toBeInTheDocument()
  })

  it('uses theme surface tone and clears the legacy fixed palette', () => {
    renderWithProviders(
      <FilterPanel>
        <span data-testid="child">筛选内容</span>
      </FilterPanel>
    )

    const surface = screen.getByTestId('base-filter-panel-surface')
    const resetAction = screen.getByTestId('base-filter-panel-reset-action')

    expect(surface).toHaveAttribute('data-filter-tone', 'surface')
    expect(surface.outerHTML).not.toContain('#1E293B')
    expect(surface.outerHTML).not.toContain('#334155')
    expect(surface.outerHTML).not.toContain('#F1F5F9')
    expect(surface.outerHTML).not.toContain('#00D084')
    expect(surface.outerHTML).not.toContain('rgba(0, 208, 132')
    expect(resetAction).toBeInTheDocument()
  })

  it('keeps built-in search and reset actions wired', async () => {
    const onSearch = vi.fn()
    const onReset = vi.fn()
    const user = userEvent.setup()

    renderWithProviders(
      <FilterPanel onSearch={onSearch} onReset={onReset}>
        <span>筛选内容</span>
      </FilterPanel>
    )

    await user.click(screen.getByRole('button', { name: /查询/ }))
    await user.click(screen.getByRole('button', { name: /重置/ }))

    expect(onSearch).toHaveBeenCalledTimes(1)
    expect(onReset).toHaveBeenCalledTimes(1)
  })
})
