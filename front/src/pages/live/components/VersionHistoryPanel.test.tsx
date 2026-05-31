import { describe, expect, it, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { VersionHistoryPanel } from './VersionHistoryPanel'
import { searchScriptVersions, getScriptVersion, rollbackScriptVersion } from '@/api/live.versions'

vi.mock('@/api/live.versions', () => ({
  searchScriptVersions: vi.fn(),
  getScriptVersion: vi.fn(),
  rollbackScriptVersion: vi.fn(),
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPanel() {
  renderWithProviders(
    <AppThemeProvider>
      <VersionHistoryPanel
        scripts={[
          { id: 18, scriptType: 'product', scriptContent: '当前话术内容' },
        ]}
        sessionId={18}
        selectedScriptId={18}
        onScriptSelect={vi.fn()}
      />
    </AppThemeProvider>,
  )
}

describe('VersionHistoryPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(searchScriptVersions).mockResolvedValue({
      total: 2,
      list: [
        { id: 2, versionNumber: 2, scriptContent: '当前版本', isCurrent: 1, source: 'manual' },
        { id: 1, versionNumber: 1, scriptContent: '旧版本', isCurrent: 0, source: 'auto_improve' },
      ],
      pageNum: 0,
      pageSize: 50,
    } as never)
    vi.mocked(getScriptVersion).mockImplementation(async (_scriptId: number, versionNumber: number) => ({
      scriptContent: versionNumber === 1 ? '旧版本：先讲成分。' : '当前版本：先讲痛点，再讲成分。',
    }) as never)
    vi.mocked(rollbackScriptVersion).mockResolvedValue(undefined as never)
  })

  it('loads versions and opens a theme-aware inline diff in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPanel()

    expect(await screen.findByText('v2')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '对比' }))

    await waitFor(() => {
      expect(getScriptVersion).toHaveBeenCalledWith(18, 1)
      expect(getScriptVersion).toHaveBeenCalledWith(18, 2)
    })
    expect(await screen.findByText('当前版本：先讲痛点，再讲成分。')).toBeInTheDocument()

    const oldSurface = screen.getByTestId('version-history-inline-diff-old-surface')
    const newSurface = screen.getByTestId('version-history-inline-diff-new-surface')
    expect(oldSurface).toHaveAttribute('data-diff-tone', 'error')
    expect(newSurface).toHaveAttribute('data-diff-tone', 'success')
    expect(oldSurface).not.toHaveStyle({ backgroundColor: 'rgba(211, 47, 47, 0.04)' })
    expect(newSurface).not.toHaveStyle({ backgroundColor: 'rgba(46, 125, 50, 0.04)' })
    expect(document.body.textContent).not.toMatch(/rgba\(211, 47, 47, 0\.04\)|rgba\(46, 125, 50, 0\.04\)/i)
  })
})
