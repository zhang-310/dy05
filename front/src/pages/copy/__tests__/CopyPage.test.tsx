import { describe, it, expect } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test/utils'
import CopyPage from '../CopyPage'

describe('CopyPage', () => {
  it('renders page header and default tabs', () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '文案管理' })).toBeInTheDocument()
    expect(screen.getAllByText('文案库').length).toBeGreaterThan(0)
    expect(screen.getByText('文案审批')).toBeInTheDocument()
    expect(screen.getAllByText('文案模板').length).toBeGreaterThan(0)
  })
})
