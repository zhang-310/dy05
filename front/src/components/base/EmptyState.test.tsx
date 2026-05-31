import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/utils'
import { EmptyState } from './EmptyState'

describe('EmptyState', () => {
  it('renders title and optional action', () => {
    const onClick = vi.fn()
    renderWithProviders(
      <EmptyState title="暂无数据" action={{ text: '去创建', onClick }} />
    )
    expect(screen.getByText('暂无数据')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '去创建' })).toBeInTheDocument()
  })

  it('calls action onClick when clicked', async () => {
    const onClick = vi.fn()
    const user = userEvent.setup()
    renderWithProviders(
      <EmptyState title="空" action={{ text: '新建', onClick }} />
    )
    await user.click(screen.getByRole('button', { name: '新建' }))
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('renders without action', () => {
    renderWithProviders(<EmptyState title="空列表" />)
    expect(screen.getByText('空列表')).toBeInTheDocument()
  })

  it('uses theme-aware neutral tone for the default surface', () => {
    renderWithProviders(<EmptyState title="空列表" description="暂无可展示内容" />)

    const surface = screen.getByTestId('base-empty-state-surface')
    const title = screen.getByTestId('empty-state-title')
    const description = screen.getByTestId('empty-state-description')
    const defaultIcon = screen.getByTestId('empty-state-default-icon')

    expect(surface).toHaveAttribute('data-empty-tone', 'neutral')
    expect(title).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(description).toHaveStyle({ color: 'rgba(0, 0, 0, 0.6)' })
    expect(title.outerHTML).not.toContain('#475569')
    expect(description.outerHTML).not.toContain('#475569')
    expect(defaultIcon.getAttribute('style') ?? '').not.toContain('#475569')
  })
})
