import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { act, fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import LiveRealtimePanelPage from '../LiveRealtimePanelPage'
import { initializePanel, nextSlot, prevSlot, subscribeWithRetry } from '@/api/live-realtime'

vi.mock('@/api/live-realtime', () => ({
  initializePanel: vi.fn(),
  nextSlot: vi.fn(),
  prevSlot: vi.fn(),
  subscribeWithRetry: vi.fn(),
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

const panelData = {
  liveSessionId: 18,
  currentSlotIndex: 0,
  targetGmv: 2500,
  slots: [
    {
      id: 1,
      liveSessionId: 18,
      slotIndex: 0,
      content: '开场介绍修护精华',
      durationSeconds: 60,
      scriptType: 'opening',
      isCurrent: true,
      isCompleted: false,
    },
    {
      id: 2,
      liveSessionId: 18,
      slotIndex: 1,
      content: '第二段讲解优惠',
      durationSeconds: 45,
      scriptType: 'discount',
      isCurrent: false,
      isCompleted: false,
    },
  ],
  realtimeData: {
    id: 1,
    liveSessionId: 18,
    watchedCount: 2000,
    viewerCount: 320,
    likeCount: 1500,
    commentCount: 88,
    shareCount: 20,
    followCount: 12,
    giftAmount: 0,
    productClickCount: 80,
    productPurchaseCount: 8,
    productPurchaseAmount: 1688,
    currentSlotIndex: 0,
  },
}

function renderPage() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/admin/live/sessions/18/realtime']}>
      <Routes>
        <Route path="/admin/live/sessions/:id/realtime" element={<LiveRealtimePanelPage />} />
      </Routes>
    </MemoryRouter>
  )
}

function renderPageWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={['/admin/live/sessions/18/realtime']}>
        <Routes>
          <Route path="/admin/live/sessions/:id/realtime" element={<LiveRealtimePanelPage />} />
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>
  )
}

describe('LiveRealtimePanelPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.stubGlobal('EventSource', class EventSource {
      close() {}
    })
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => {
      cb(performance.now() + 500)
      return 1
    })
    vi.mocked(initializePanel).mockResolvedValue(panelData)
    vi.mocked(nextSlot).mockResolvedValue(panelData.slots[1])
    vi.mocked(prevSlot).mockResolvedValue(panelData.slots[0])
    vi.mocked(subscribeWithRetry).mockReturnValue({ close: vi.fn(), reconnect: vi.fn() })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('renders realtime panel data from /live/realtime-panel/init', async () => {
    renderPage()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-contract-scope', 'live-realtime-panel')
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-ready-endpoints', '/live/realtime-panel/init|/live/realtime-panel/stream/:liveSessionId|/live/realtime-panel/next-slot|/live/realtime-panel/prev-slot')
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('jump-slot-from-panel'))
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-panel-mock-fallback'))
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-session-id', '18')
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-current-slot-index', '0')
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-total-slots', '2')
    expect(screen.getByTestId('live-realtime-stream-mode-chip')).toHaveAttribute('data-contract-source', '/live/realtime-panel/stream/:liveSessionId')
    expect(screen.getByTestId('live-realtime-next-button')).toHaveAttribute('data-contract-source', '/live/realtime-panel/next-slot')
    expect(screen.getByTestId('live-realtime-prev-button')).toHaveAttribute('data-contract-source', '/live/realtime-panel/prev-slot')
    expect(screen.getByText('320')).toBeInTheDocument()
    expect(screen.getByText('1,500')).toBeInTheDocument()
    expect(initializePanel).toHaveBeenCalledWith(18)
    expect(subscribeWithRetry).toHaveBeenCalledWith(18, expect.objectContaining({
      onConnected: expect.any(Function),
      onError: expect.any(Function),
    }), 2, 1500)
  })

  it('shows SSE stream endpoint when realtime stream downgrades to polling', async () => {
    vi.mocked(subscribeWithRetry).mockImplementation((_sessionId, callbacks) => {
      callbacks.onError?.(new Error('stream down'))
      return { close: vi.fn(), reconnect: vi.fn() }
    })

    renderPage()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    expect(screen.getByTestId('live-realtime-stream-downgrade-alert')).toHaveTextContent('/live/realtime-panel/stream/18 SSE 连接异常：stream down')
    expect(screen.getByTestId('live-realtime-stream-downgrade-alert')).toHaveAttribute('data-contract-source', '/live/realtime-panel/stream/:liveSessionId')
    expect(screen.getByTestId('live-realtime-stream-downgrade-alert')).toHaveAttribute('data-stream-mode', 'polling')
    expect(screen.getByTestId('live-realtime-stream-downgrade-alert')).toHaveAttribute('data-polling-fallback-sec', '10')
    expect(screen.getByTestId('live-realtime-stream-downgrade-alert')).toHaveAttribute('data-no-local-panel-fallback', 'true')
    expect(screen.getByText(/route=\/admin\/live\/sessions\/18\/realtime; liveSessionId=18; retry=2/)).toBeInTheDocument()
    expect(screen.getByText(/已降级为 10 秒轮询/)).toBeInTheDocument()
  })

  it('surfaces SSE retry diagnostics without relying on console logs', async () => {
    vi.mocked(subscribeWithRetry).mockImplementation((_sessionId, callbacks) => {
      callbacks.onRetryScheduled?.({
        retryCount: 1,
        maxRetries: 2,
        retryDelayMs: 1500,
        error: new Error('temporary close'),
      })
      return { close: vi.fn(), reconnect: vi.fn() }
    })

    renderPage()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    const alert = screen.getByTestId('live-realtime-stream-downgrade-alert')
    expect(alert).toHaveTextContent('SSE 正在重试：1500ms 后第 1/2 次重连')
    expect(alert).toHaveAttribute('data-stream-retry-count', '1')
    expect(alert).toHaveAttribute('data-stream-retry-diagnostic', expect.stringContaining('temporary close'))
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-stream-retry-count', '1')
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-stream-retry-diagnostic', expect.stringContaining('temporary close'))
  })

  it('uses theme-aware realtime KPI and GMV progress colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    expect(screen.getByText('目标 ¥2,500')).toBeInTheDocument()
    expect(screen.getByTestId('live-realtime-gmv-progress-surface')).toHaveAttribute('data-progress-tone', 'warning')
    expect(screen.getByTestId('live-realtime-gmv-progress-surface')).toHaveAttribute('data-progress-color', '#ffb74d')
    expect(screen.getByTestId('live-realtime-gmv-progress-label')).toHaveAttribute('data-progress-color', '#ffb74d')
    expect(screen.getByTestId('live-realtime-gmv-card-surface')).toHaveAttribute('data-card-tone', 'warning')
    expect(screen.getByTestId('live-realtime-gmv-card-surface')).toHaveAttribute('data-card-bg-tone', 'paper')
    expect(screen.getByTestId('live-realtime-gmv-card-surface')).toHaveAttribute('data-card-accent-color', '#ffb74d')
    expect(screen.getByTestId('live-realtime-gmv-value-surface')).toHaveAttribute('data-gmv-tone', 'warning')
    expect(screen.getByTestId('live-realtime-gmv-value-surface')).toHaveAttribute('data-gmv-color', '#ffb74d')
    expect(screen.getByTestId('live-realtime-gmv-caption-surface')).toHaveAttribute('data-caption-tone', 'text-secondary')
    expect(screen.getByTestId('live-realtime-gmv-target-caption-surface')).toHaveAttribute('data-caption-tone', 'text-secondary')

    const kpiValues = screen.getAllByTestId('live-realtime-kpi-value-surface')
    expect(kpiValues.map(node => node.getAttribute('data-kpi-tone'))).toEqual(['info', 'secondary', 'success'])
    expect(kpiValues.map(node => node.getAttribute('data-kpi-color'))).toEqual(['#4fc3f7', '#f3e5f5', '#81c784'])
    expect(screen.getAllByTestId('live-realtime-kpi-card-surface').map(node => node.getAttribute('data-card-bg-tone'))).toEqual(['paper', 'paper', 'paper'])
    expect(screen.getAllByTestId('live-realtime-kpi-caption-surface').map(node => node.getAttribute('data-caption-tone'))).toEqual(['text-secondary', 'text-secondary', 'text-secondary'])

    for (const legacy of ['rgb(25, 118, 210)', 'rgb(255, 152, 0)', 'rgb(76, 175, 80)']) {
      expect(window.getComputedStyle(screen.getByTestId('live-realtime-gmv-progress-label')).color).not.toBe(legacy)
    }
    expect(document.body.innerHTML).not.toContain('#1a1a1a')
    expect(document.body.innerHTML).not.toContain('#ffe08244')
    expect(document.body.innerHTML).not.toContain('#ffe082')
  })

  it('uses theme-aware topbar, slot progress and teleprompter surfaces in dark fullscreen mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('全屏提词'))

    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-fullscreen', 'true')
    expect(screen.getByTestId('live-realtime-topbar-surface')).toHaveAttribute('data-surface-tone', 'fullscreen')
    expect(screen.getByTestId('live-realtime-live-status-dot')).toHaveAttribute('data-status-tone', 'error')
    expect(screen.getByTestId('live-realtime-live-status-dot')).toHaveAttribute('data-status-color', '#e57373')
    expect(screen.getByTestId('live-realtime-elapsed-chip-surface')).toHaveAttribute('data-chip-tone', 'fullscreen')
    expect(screen.getByTestId('live-realtime-slot-count-chip-surface')).toHaveAttribute('data-chip-tone', 'fullscreen-muted')
    expect(screen.getByTestId('live-realtime-slot-progress-surface')).toHaveAttribute('data-progress-tone', 'primary')
    expect(screen.getByTestId('live-realtime-slot-progress-surface')).toHaveAttribute('data-track-tone', 'fullscreen')
    expect(screen.getByTestId('live-realtime-slot-progress-surface')).toHaveAttribute('data-progress-color', '#e3f2fd')
    expect(screen.getByTestId('live-realtime-teleprompter-surface')).toHaveAttribute('data-prompter-tone', 'fullscreen')
    expect(screen.getByTestId('live-realtime-teleprompter-text-surface')).toHaveAttribute('data-text-tone', 'inverse')
    expect(screen.getByTestId('live-realtime-gmv-card-surface')).toHaveAttribute('data-card-bg-tone', 'fullscreen')
    expect(screen.getByTestId('live-realtime-gmv-caption-surface')).toHaveAttribute('data-caption-tone', 'fullscreen-muted')
    expect(screen.getByTestId('live-realtime-gmv-target-caption-surface')).toHaveAttribute('data-caption-tone', 'fullscreen-muted')
    expect(screen.getAllByTestId('live-realtime-kpi-card-surface').map(node => node.getAttribute('data-card-bg-tone'))).toEqual(['fullscreen', 'fullscreen', 'fullscreen'])
    expect(screen.getAllByTestId('live-realtime-kpi-caption-surface').map(node => node.getAttribute('data-caption-tone'))).toEqual(['fullscreen-muted', 'fullscreen-muted', 'fullscreen-muted'])

    const topbarBg = window.getComputedStyle(screen.getByTestId('live-realtime-topbar-surface')).backgroundColor
    const prompterBg = window.getComputedStyle(screen.getByTestId('live-realtime-teleprompter-surface')).backgroundColor
    const progressBg = window.getComputedStyle(screen.getByTestId('live-realtime-slot-progress-surface')).backgroundColor
    const gmvCardBg = window.getComputedStyle(screen.getByTestId('live-realtime-gmv-card-surface')).backgroundColor
    expect(screen.getByTestId('live-realtime-live-status-dot')).not.toHaveAttribute('data-status-color', '#f44336')
    expect(screen.getByTestId('live-realtime-slot-progress-surface')).not.toHaveAttribute('data-progress-color', '#1976d2')
    expect([topbarBg, prompterBg, progressBg]).not.toContain('rgb(17, 17, 17)')
    expect(gmvCardBg).not.toBe('rgb(26, 26, 26)')
    expect(progressBg).not.toBe('rgb(51, 51, 51)')
  })

  it('keeps current slot and shows endpoint source when next slot fails', async () => {
    vi.mocked(nextSlot).mockRejectedValue(new Error('next down'))

    renderPage()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '下一条' }))

    expect(await screen.findByText(/\/live\/realtime-panel\/next-slot 切换话术失败：next down/)).toBeInTheDocument()
    expect(screen.getByTestId('live-realtime-operation-error')).toHaveAttribute('data-contract-source', '/live/realtime-panel/next-slot')
    expect(screen.getByTestId('live-realtime-operation-error')).toHaveAttribute('data-current-slot-index', '0')
    expect(screen.getByTestId('live-realtime-operation-error')).toHaveAttribute('data-no-local-slot-fallback', 'true')
    expect(screen.getByText(/liveSessionId=18; direction=next; currentSlotIndex=0; totalSlots=2/)).toBeInTheDocument()
    expect(screen.getByText('开场介绍修护精华')).toBeInTheDocument()
  })

  it('keeps current slot and shows endpoint source when prev slot fails', async () => {
    vi.mocked(initializePanel).mockResolvedValue({
      ...panelData,
      currentSlotIndex: 1,
      realtimeData: { ...panelData.realtimeData, currentSlotIndex: 1 },
    })
    vi.mocked(prevSlot).mockRejectedValue(new Error('prev down'))

    renderPage()

    expect(await screen.findByText('第二段讲解优惠')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '上一条' }))

    expect(await screen.findByTestId('live-realtime-operation-error')).toHaveTextContent('/live/realtime-panel/prev-slot 切换话术失败：prev down')
    expect(screen.getByTestId('live-realtime-operation-error')).toHaveAttribute('data-contract-source', '/live/realtime-panel/prev-slot')
    expect(screen.getByTestId('live-realtime-operation-error')).toHaveAttribute('data-current-slot-index', '1')
    expect(screen.getByText(/direction=prev; currentSlotIndex=1; totalSlots=2/)).toBeInTheDocument()
    expect(screen.getByText('第二段讲解优惠')).toBeInTheDocument()
  })

  it('updates metrics and current slot from SSE callbacks without unsupported writes', async () => {
    let sseCallbacks: Parameters<typeof subscribeWithRetry>[1] | undefined
    vi.mocked(subscribeWithRetry).mockImplementation((_sessionId, callbacks) => {
      sseCallbacks = callbacks
      callbacks.onConnected?.()
      return { close: vi.fn(), reconnect: vi.fn() }
    })

    renderPage()

    expect(await screen.findByText('开场介绍修护精华')).toBeInTheDocument()
    act(() => {
      sseCallbacks?.onDataUpdate?.({ viewerCount: 388, likeCount: 1800, commentCount: 99 })
      sseCallbacks?.onSlotChange?.({ currentSlotIndex: 1, content: 'SSE 切到优惠段', durationSeconds: 45 })
    })

    expect(await screen.findByText('SSE 切到优惠段')).toBeInTheDocument()
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-stream-mode', 'sse')
    expect(screen.getByTestId('live-realtime-panel-root-surface')).toHaveAttribute('data-current-slot-index', '1')
    expect(screen.getByTestId('live-realtime-stream-mode-chip')).toHaveAttribute('data-stream-mode', 'sse')
    expect(screen.getByText('388')).toBeInTheDocument()
    expect(screen.getByText('1,800')).toBeInTheDocument()
    expect(screen.getByText('99')).toBeInTheDocument()
  })

  it('shows init endpoint when panel initialization fails', async () => {
    vi.mocked(initializePanel).mockRejectedValue(new Error('init down'))

    renderPage()

    expect(await screen.findByTestId('live-realtime-init-error')).toHaveTextContent('/live/realtime-panel/init 获取实时面板失败：init down')
    expect(screen.getByTestId('live-realtime-init-failure')).toHaveAttribute('data-contract-scope', 'live-realtime-panel')
    expect(screen.getByTestId('live-realtime-init-failure')).toHaveAttribute('data-no-local-panel-fallback', 'true')
    expect(screen.getByTestId('live-realtime-init-error')).toHaveAttribute('data-contract-source', '/live/realtime-panel/init')
    expect(screen.getByTestId('live-realtime-init-error')).toHaveAttribute('data-no-local-panel-fallback', 'true')
    expect(screen.getByText(/route=\/admin\/live\/sessions\/18\/realtime; liveSessionId=18/)).toBeInTheDocument()
  })
})
