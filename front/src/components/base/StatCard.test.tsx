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
})
