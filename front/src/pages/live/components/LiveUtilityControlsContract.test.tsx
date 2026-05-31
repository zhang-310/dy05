import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { PanelResizer, getStoredLeftWidth, getStoredRightWidth, setStoredLeftWidth, setStoredRightWidth } from './PanelResizer'
import { SortStrategyPanel } from './SortStrategyPanel'
import { FirstTimeGuide } from './FirstTimeGuide'

describe('live utility controls contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('marks panel resizer as props/localStorage owned and emits drag/reset callbacks', () => {
    const onResize = vi.fn()
    const onReset = vi.fn()
    renderWithProviders(<PanelResizer onResize={onResize} onReset={onReset} />)

    const resizer = screen.getByTestId('live-panel-resizer')
    expect(resizer).toHaveAttribute('data-contract-scope', 'live-panel-resizer-props')
    expect(resizer).toHaveAttribute('data-ready-sources', 'onResize-prop|onReset-prop|localStorage-widths')
    expect(resizer).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(resizer).toHaveAttribute('data-reset-enabled', 'true')

    fireEvent.mouseDown(resizer, { button: 0, clientX: 100 })
    fireEvent.mouseMove(document, { clientX: 125 })
    fireEvent.mouseUp(document)
    expect(onResize).toHaveBeenCalledWith(25)

    fireEvent.doubleClick(resizer)
    expect(onReset).toHaveBeenCalled()
  })

  it('keeps panel width storage clamped to valid values', () => {
    setStoredLeftWidth(320)
    setStoredRightWidth(420)
    expect(getStoredLeftWidth()).toBe(320)
    expect(getStoredRightWidth()).toBe(420)

    window.localStorage.setItem('live-script-left-width', '999')
    window.localStorage.setItem('live-script-right-width', '1')
    expect(getStoredLeftWidth()).toBe(280)
    expect(getStoredRightWidth()).toBe(380)
  })

  it('marks sort strategy panel callbacks, AI endpoint owner, and disabled reasons', () => {
    const onChange = vi.fn()
    const onReverse = vi.fn()
    const onAiSort = vi.fn()
    renderWithProviders(
      <SortStrategyPanel
        value="manual"
        onChange={onChange}
        onReverse={onReverse}
        onAiSort={onAiSort}
      />,
    )

    const panel = screen.getByTestId('sort-strategy-panel')
    expect(panel).toHaveAttribute('data-contract-scope', 'live-sort-strategy-props')
    expect(panel).toHaveAttribute('data-ready-endpoints', '/live/ai/sort-suggest')
    expect(panel).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(screen.getByTestId('sort-strategy-ai-button')).toHaveAttribute('data-disabled-reason', 'ready')

    fireEvent.click(screen.getByTestId('sort-strategy-type-button'))
    fireEvent.click(screen.getByTestId('sort-strategy-reverse-button'))
    fireEvent.click(screen.getByTestId('sort-strategy-ai-button'))
    expect(onChange).toHaveBeenCalledWith('type')
    expect(onReverse).toHaveBeenCalled()
    expect(onAiSort).toHaveBeenCalled()
  })

  it('marks sort strategy AI disabled and loading states distinctly', () => {
    const { rerender } = renderWithProviders(
      <SortStrategyPanel value="manual" onChange={vi.fn()} onAiSort={vi.fn()} aiDisabled />,
    )
    expect(screen.getByTestId('sort-strategy-ai-button')).toHaveAttribute('data-disabled-reason', 'ai-disabled')

    rerender(<SortStrategyPanel value="manual" onChange={vi.fn()} onAiSort={vi.fn()} aiLoading />)
    expect(screen.getByTestId('sort-strategy-ai-button')).toHaveAttribute('data-disabled-reason', 'ai-loading')
  })

  it('marks first-time guide step flow and stores completion locally', async () => {
    renderWithProviders(<FirstTimeGuide showInStandalone />)

    const dialog = await screen.findByTestId('first-time-guide-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-first-time-guide-dialog')
    expect(dialog).toHaveAttribute('data-ready-sources', 'showInStandalone-prop|localStorage:live-script-first-guide-done')
    expect(dialog).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(dialog).toHaveAttribute('data-step', '0')

    fireEvent.click(screen.getByTestId('first-time-guide-next-button'))
    await waitFor(() => {
      expect(screen.getByTestId('first-time-guide-dialog')).toHaveAttribute('data-step', '1')
    })
    fireEvent.click(screen.getByTestId('first-time-guide-next-button'))
    await waitFor(() => {
      expect(screen.getByTestId('first-time-guide-next-button')).toHaveAttribute('data-action', 'finish')
    })
    fireEvent.click(screen.getByTestId('first-time-guide-next-button'))
    await waitFor(() => {
      expect(window.localStorage.getItem('live-script-first-guide-done')).toBe('1')
    })
  })

  it('keeps first-time guide hidden state auditable when disabled or already dismissed', async () => {
    const { rerender } = renderWithProviders(<FirstTimeGuide showInStandalone={false} />)
    expect(screen.getByTestId('first-time-guide-hidden-state')).toHaveAttribute('data-open', 'false')
    expect(screen.getByTestId('first-time-guide-hidden-state')).toHaveAttribute('data-show-in-standalone', 'false')

    window.localStorage.setItem('live-script-first-guide-done', '1')
    rerender(<FirstTimeGuide showInStandalone />)
    await waitFor(() => {
      expect(screen.getByTestId('first-time-guide-hidden-state')).toHaveAttribute('data-open', 'false')
    })
  })
})
