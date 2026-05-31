import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { SegmentRefinePanel } from '../SegmentRefinePanel'
import { refineSegment } from '@/api/live-ai'

vi.mock('@/api/live-ai', () => ({
  refineSegment: vi.fn(),
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPanel(ui: React.ReactElement) {
  return renderWithProviders(ui)
}

function renderPanelWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('SegmentRefinePanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(refineSegment).mockResolvedValue('微调后：开场更口语，卖点更明确。' as never)
  })

  it('calls refine endpoint and applies the refined segment', async () => {
    const onApply = vi.fn()
    renderPanel(
      <SegmentRefinePanel
        scriptId={18}
        modelId={3}
        segmentText="原始话术：这款精华可以帮助改善干燥。"
        onApply={onApply}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'AI 单段微调' }))
    fireEvent.click(screen.getByText('更口语化'))
    fireEvent.click(screen.getByRole('button', { name: '开始微调' }))

    await waitFor(() => {
      expect(refineSegment).toHaveBeenCalledWith(
        18,
        '原始话术：这款精华可以帮助改善干燥。',
        '改为更口语、生活化的表达，去掉书面用语',
        3,
      )
    })
    expect(await screen.findByText(/微调后：开场更口语/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '采用' }))
    expect(onApply).toHaveBeenCalledWith('微调后：开场更口语，卖点更明确。')
    expect(toast).toHaveBeenCalledWith('已应用微调结果', 'success')
  })

  it('keeps the panel open and disables refine when instruction is empty', () => {
    renderPanel(
      <SegmentRefinePanel
        scriptId={18}
        segmentText="原始话术"
        onApply={vi.fn()}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'AI 单段微调' }))

    expect(screen.getByRole('button', { name: '开始微调' })).toBeDisabled()
    expect(refineSegment).not.toHaveBeenCalled()
    expect(toast).not.toHaveBeenCalled()
    expect(screen.getByText('原文')).toBeInTheDocument()
  })

  it('uses theme-aware original and result surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPanelWithTheme(
      <SegmentRefinePanel
        scriptId={18}
        segmentText="原始话术：屏障修护先稳住，再谈提亮。"
        onApply={vi.fn()}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'AI 单段微调' }))
    const originalSurface = screen.getByTestId('segment-refine-original-surface')
    expect(originalSurface).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })

    fireEvent.click(screen.getByText('突出卖点'))
    fireEvent.click(screen.getByRole('button', { name: '开始微调' }))
    expect(await screen.findByText(/微调后：开场更口语/)).toBeInTheDocument()

    const resultSurface = screen.getByTestId('segment-refine-result-surface')
    expect(resultSurface).not.toHaveStyle({
      backgroundColor: 'rgb(232, 245, 233)',
    })
  })
})
