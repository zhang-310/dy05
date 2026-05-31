import { describe, expect, it, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { RegenerationDiffDialog } from './RegenerationDiffDialog'

function renderDialog(props?: Partial<React.ComponentProps<typeof RegenerationDiffDialog>>) {
  const callbacks = {
    onAccept: vi.fn(),
    onReject: vi.fn(),
    onCancel: vi.fn(),
  }
  renderWithProviders(
    <AppThemeProvider>
      <RegenerationDiffDialog
        open
        oldContent="原版本话术：先讲成分，节奏略慢。"
        newContent="新版本话术：先讲痛点，再讲成分和限时福利。"
        slotLabel="产品讲解"
        {...callbacks}
        {...props}
      />
    </AppThemeProvider>,
  )
  return callbacks
}

describe('RegenerationDiffDialog', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('renders diff actions and calls selected handlers', () => {
    const callbacks = renderDialog()

    expect(screen.getByText('重新生成对比 — 产品讲解')).toBeInTheDocument()
    expect(screen.getByText('原版本')).toBeInTheDocument()
    expect(screen.getByText('新版本')).toBeInTheDocument()
    expect(screen.getByText('差异：+5 字')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '保留原版' }))
    fireEvent.click(screen.getByRole('button', { name: '使用新版本' }))
    fireEvent.click(screen.getByRole('button', { name: '取消' }))

    expect(callbacks.onReject).toHaveBeenCalledTimes(1)
    expect(callbacks.onAccept).toHaveBeenCalledTimes(1)
    expect(callbacks.onCancel).toHaveBeenCalledTimes(1)
  })

  it('uses theme-aware diff surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderDialog()

    const oldSurface = screen.getByTestId('regeneration-diff-old-surface')
    const newSurface = screen.getByTestId('regeneration-diff-new-surface')

    expect(oldSurface).toHaveAttribute('data-diff-tone', 'error')
    expect(newSurface).toHaveAttribute('data-diff-tone', 'success')
    expect(oldSurface).not.toHaveStyle({ backgroundColor: 'rgba(211, 47, 47, 0.04)' })
    expect(newSurface).not.toHaveStyle({ backgroundColor: 'rgba(46, 125, 50, 0.04)' })
    expect(document.body.textContent).not.toMatch(/rgba\(211, 47, 47, 0\.04\)|rgba\(46, 125, 50, 0\.04\)/i)
  })
})
