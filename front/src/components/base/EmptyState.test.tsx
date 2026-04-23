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
})
