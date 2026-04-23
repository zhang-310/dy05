import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
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
})
