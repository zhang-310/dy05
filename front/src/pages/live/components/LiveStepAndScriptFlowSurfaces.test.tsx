import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders } from '@/test/utils'
import LiveWorkbenchPage from '../LiveWorkbenchPage'
import { SessionWorkspacePage } from '../SessionWorkspacePage'
import { STEP_COLORS, STEP_COLOR_TONES, STEP_TO_TAB, getStepThemeColors } from '../sessionWorkbenchNav'
import { liveApi } from '@/api/live'
import { productApi } from '@/api/product'
import { createTheme } from '@mui/material/styles'

const toast = vi.hoisted(() => vi.fn())

vi.mock('@/contexts/ToastContext', () => ({
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useToast: () => toast,
}))

vi.mock('@/hooks/useNetworkStatus', () => ({ useNetworkStatus: () => ({ status: 'online' }) }))
vi.mock('@/hooks/useCollaboration', () => ({ useCollaboration: () => ({ collaborators: [] }) }))

vi.mock('../components/SelectTabContent', () => ({
  SelectTabContent: () => <div>选品主体</div>,
}))
vi.mock('../components/GenerateTabContent', () => ({
  GenerateTabContent: () => <div>生成主体</div>,
}))
vi.mock('../components/ScriptTabContent', () => ({
  ScriptTabContent: () => <div>话术主体</div>,
}))
vi.mock('../components/ReadinessTab', () => ({
  ReadinessTab: () => <div>准备度主体</div>,
}))
vi.mock('../components/DataAnalysisTab', () => ({
  DataAnalysisTab: () => <div>复盘主体</div>,
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionGet: vi.fn(),
    sessionReadiness: vi.fn(),
    sessionOverview: vi.fn(),
    productBySession: vi.fn(),
    scriptBySession: vi.fn(),
  },
}))

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
  },
}))

function renderDark(path: string, element: React.ReactElement, routePath: string) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path={routePath} element={element} />
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('live step and script flow surfaces', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(liveApi.sessionGet).mockResolvedValue({
      id: 18,
      liveTitle: '第221批直播场次',
      status: 0,
      sessionType: 'standard',
    } as never)
    vi.mocked(liveApi.sessionReadiness).mockResolvedValue({ score: 86 } as never)
    vi.mocked(liveApi.sessionOverview).mockResolvedValue({ gmv: 12000, orderCount: 42, scriptCount: 6, productCount: 2, readiness: 86 } as never)
    vi.mocked(liveApi.productBySession).mockResolvedValue([] as never)
    vi.mocked(liveApi.scriptBySession).mockResolvedValue([] as never)
    vi.mocked(productApi.list).mockResolvedValue({ list: [], total: 0, page: 0, rows: 200 } as never)
  })

  it('uses theme-aware top step chip colors in the live workbench shell', async () => {
    renderDark('/admin/live/workbench/18?step=2&tab=scripts', <LiveWorkbenchPage />, '/admin/live/workbench/:sessionId')

    const activeStep = await screen.findByTestId('live-workbench-step-chip-active-surface')
    expect(activeStep).toHaveAttribute('data-step-tab', 'scripts')
    expect(activeStep).toHaveAttribute('data-step-tone', 'warning')
    expect(activeStep).toHaveAttribute('data-step-active', 'true')
    expect(activeStep).toHaveStyle({
      '--live-workbench-step-tone': 'warning',
    })
    expect(activeStep).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
  })

  it('uses theme-aware tab badge colors inside the session workspace', async () => {
    renderDark(
      '/admin/live/workbench/18?step=2&tab=scripts',
      <SessionWorkspacePage sessionId={18} step={2} onStepChange={vi.fn()} />,
      '/admin/live/workbench/:sessionId',
    )

    await waitFor(() => {
      const activeBadge = screen.getByTestId('session-workspace-tab-badge-active-surface')
      expect(activeBadge).toHaveAttribute('data-step-tab', 'scripts')
      expect(activeBadge).toHaveAttribute('data-step-tone', 'warning')
      expect(activeBadge).toHaveAttribute('data-step-active', 'true')
      expect(activeBadge).toHaveStyle({
        '--session-workspace-tab-badge-tone': 'warning',
      })
      expect(activeBadge).not.toHaveStyle({
        backgroundColor: 'rgb(255, 243, 224)',
      })
    })
  })

  it('keeps step color configuration semantic instead of fixed legacy hex tokens', () => {
    expect(STEP_TO_TAB.map(tab => STEP_COLORS[tab].tone)).toEqual(['primary', 'secondary', 'warning', 'success', 'info'])
    expect(STEP_COLOR_TONES).toEqual({
      products: 'primary',
      generate: 'secondary',
      scripts: 'warning',
      readiness: 'success',
      data: 'info',
    })

    const serialized = JSON.stringify(STEP_COLORS)
    for (const legacy of ['#e3f2fd', '#1565c0', '#f3e5f5', '#7b1fa2', '#fff3e0', '#e65100', '#e8f5e9', '#2e7d32', '#e0f7fa', '#00838f']) {
      expect(serialized).not.toContain(legacy)
    }

    const darkTheme = createTheme({ palette: { mode: 'dark' } })
    const scriptColors = getStepThemeColors(darkTheme, 'scripts', true)
    expect(scriptColors.tone).toBe('warning')
    expect(scriptColors.chipBg).toBe(darkTheme.palette.warning.main)
    expect(scriptColors.chipColor).toBe(darkTheme.palette.warning.contrastText)
  })
})
