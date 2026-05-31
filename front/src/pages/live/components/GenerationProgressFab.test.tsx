import { describe, expect, it, vi } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { GenerationProgressFab } from './GenerationProgressFab'

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('GenerationProgressFab', () => {
  it('uses theme-aware pulse surface and keeps click behavior', () => {
    const onClick = vi.fn()

    renderDark(
      <GenerationProgressFab
        visible
        genLoading
        doneCount={2}
        totalCount={5}
        onClick={onClick}
      />,
    )

    const fab = screen.getByTestId('generation-progress-fab-surface')
    expect(fab).toHaveAttribute('data-generation-loading', 'true')
    expect(fab.getAttribute('style') ?? '').not.toContain('rgba(25, 118, 210')
    expect(screen.getByText('2/5')).toBeInTheDocument()

    fireEvent.click(fab)
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('does not expose loading state when generation is idle', () => {
    renderDark(
      <GenerationProgressFab
        visible
        genLoading={false}
        doneCount={0}
        totalCount={0}
        onClick={vi.fn()}
      />,
    )

    expect(screen.getByTestId('generation-progress-fab-surface')).toHaveAttribute('data-generation-loading', 'false')
    expect(screen.queryByText('0/0')).not.toBeInTheDocument()
  })
})
