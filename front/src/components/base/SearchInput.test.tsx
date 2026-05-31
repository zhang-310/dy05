import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/utils'
import { SearchInput } from './SearchInput'

describe('SearchInput', () => {
  it('searches on Enter and clears with the clear button', async () => {
    const onSearch = vi.fn()
    const onClear = vi.fn()
    const user = userEvent.setup()

    renderWithProviders(<SearchInput placeholder="搜索话术" onSearch={onSearch} onClear={onClear} defaultValue="口播" />)

    const input = screen.getByTestId('base-search-input-field')
    await user.clear(input)
    await user.type(input, '爆款{Enter}')
    expect(onSearch).toHaveBeenCalledWith('爆款')

    await user.click(screen.getByRole('button', { name: '清除' }))
    expect(onClear).toHaveBeenCalledTimes(1)
    expect(input).toHaveValue('')
  })

  it('uses neutral theme tone and removes the legacy fixed dark palette', () => {
    renderWithProviders(<SearchInput onSearch={vi.fn()} defaultValue="关键词" />)

    const root = screen.getByTestId('base-search-input')
    expect(root).toHaveAttribute('data-search-tone', 'neutral')
    expect(root.outerHTML).not.toContain('#0F172A')
    expect(root.outerHTML).not.toContain('#F1F5F9')
    expect(root.outerHTML).not.toContain('#334155')
    expect(root.outerHTML).not.toContain('#475569')
    expect(root.outerHTML).not.toContain('#00D084')
  })
})
