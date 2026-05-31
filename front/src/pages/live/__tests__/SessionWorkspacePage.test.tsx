/**
 * SessionWorkspacePage 测试：Tab 切换、数据加载
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { waitFor, screen, render } from '@testing-library/react'
import { SnackbarProvider } from 'notistack'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { ThemeProvider, createTheme } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastProvider } from '@/contexts/ToastContext'
import SessionWorkspacePageWrapper, { SessionWorkspacePage } from '../SessionWorkspacePage'
import { liveApi } from '@/api/live'
import { productApi } from '@/api/product'

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionGet: vi.fn(),
    productBySession: vi.fn(),
    scriptBySession: vi.fn(),
    sessionReadiness: vi.fn(),
    aiGenerateFullInProgress: vi.fn(),
  },
}))
vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
  },
}))
vi.mock('@/api/ai', () => ({
  listAiModels: vi.fn().mockResolvedValue([]),
  getModelsByTaskCode: vi.fn().mockResolvedValue([]),
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

const theme = createTheme({})

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-probe">{location.pathname}{location.search}</div>
}

function renderWithRoute(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <SnackbarProvider maxSnack={3}>
          <ToastProvider>
          <MemoryRouter initialEntries={[path]} initialIndex={0}>
            <Routes>
              <Route path="/admin/live/workbench/:id" element={<SessionWorkspacePage sessionId={1} step={0} onStepChange={() => undefined} />} />
              <Route path="/org/live/workbench/:id" element={<SessionWorkspacePage sessionId={1} step={0} onStepChange={() => undefined} />} />
              <Route path="/talent/live/workbench/:id" element={<SessionWorkspacePage sessionId={1} step={0} onStepChange={() => undefined} />} />
            </Routes>
          </MemoryRouter>
          </ToastProvider>
        </SnackbarProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

function renderWrapperWithRoute(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <SnackbarProvider maxSnack={3}>
          <ToastProvider>
          <MemoryRouter initialEntries={[path]} initialIndex={0}>
            <Routes>
              <Route path="/admin/live/sessions/:sessionId" element={<SessionWorkspacePageWrapper />} />
              <Route path="/org/live/sessions/:sessionId" element={<SessionWorkspacePageWrapper />} />
              <Route path="/talent/live/sessions/:sessionId" element={<SessionWorkspacePageWrapper />} />
              <Route path="/admin/live/sessions" element={<LocationProbe />} />
              <Route path="/org/live/sessions" element={<LocationProbe />} />
              <Route path="/talent/live/sessions" element={<LocationProbe />} />
            </Routes>
          </MemoryRouter>
          </ToastProvider>
        </SnackbarProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

describe('SessionWorkspacePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(liveApi.sessionGet).mockResolvedValue({
      id: 1,
      liveTitle: '测试场次',
      status: 0,
      sessionType: 'normal',
    } as never)
    vi.mocked(liveApi.productBySession).mockResolvedValue([] as never)
    vi.mocked(liveApi.scriptBySession).mockResolvedValue([] as never)
    vi.mocked(liveApi.sessionReadiness).mockResolvedValue({ score: 0 } as never)
    vi.mocked(liveApi.aiGenerateFullInProgress).mockResolvedValue({ inProgress: false } as never)
    vi.mocked(productApi.list).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 200 } as never)
  })

  it('renders without crashing with valid id', async () => {
    const { container } = renderWithRoute('/admin/live/workbench/1')
    expect(container).toBeInTheDocument()
    expect(await screen.findByText('选品主体')).toBeInTheDocument()
    const root = screen.getByTestId('session-workspace-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-session-workspace-core')
    expect(root).toHaveAttribute('data-ready-endpoints', '/live/session/get|/live/product/by-session|/live/script/by-session|/live/session/readiness')
    expect(root).toHaveAttribute('data-context-endpoints', '/live/session/get|/live/product/by-session|/live/script/by-session|/live/session/readiness|/product/list')
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shell-clone-session'))
    expect(root).toHaveAttribute('data-route-scope', 'admin')
    expect(root).toHaveAttribute('data-tab', 'products')
    expect(root).toHaveAttribute('data-no-local-session-fallback', 'true')
    expect(root).toHaveAttribute('data-no-local-product-array-fallback', 'true')
    expect(screen.getByTestId('session-workspace-tabs')).toHaveAttribute('data-contract-source', '/live/session/get|/live/product/by-session|/live/script/by-session|/live/session/readiness')
    expect(screen.getByTestId('session-workspace-products-pane')).toHaveAttribute('data-contract-owner', 'SelectTabContent')
  })

  it('renders without crashing with invalid id', async () => {
    const { container } = renderWrapperWithRoute('/admin/live/sessions/abc')
    expect(container).toBeInTheDocument()
    expect(await screen.findByTestId('location-probe')).toHaveTextContent('/live/sessions')
  })

  it('keeps workbench visible when product or script dependencies return non-array payloads', async () => {
    vi.mocked(liveApi.productBySession).mockResolvedValue({ total: 1, list: [{ id: 1, productId: 11 }] } as never)
    vi.mocked(liveApi.scriptBySession).mockResolvedValue({ broken: true } as never)

    renderWithRoute('/admin/live/workbench/1')

    expect(await screen.findByText('选品主体')).toBeInTheDocument()
    const downgrade = await screen.findByTestId('session-workspace-dependency-downgrade-alert')
    expect(downgrade).toHaveTextContent(/直播工作台依赖降级/)
    expect(downgrade).toHaveAttribute('data-contract-sources', '/live/product/by-session|/live/script/by-session')
    expect(downgrade).toHaveAttribute('data-issue-count', '2')
    expect(downgrade).toHaveAttribute('data-no-local-product-array-fallback', 'true')
    expect(screen.getByText(/\/live\/product\/by-session 接口返回分页结构/)).toBeInTheDocument()
    expect(screen.getByText(/\/live\/script\/by-session 接口返回 object/)).toBeInTheDocument()
    expect(screen.getByTestId('session-workspace-root')).toHaveAttribute('data-dependency-issue-count', '2')
  })

  it('honors direct session route step query so approval links open the script tab', async () => {
    renderWrapperWithRoute('/admin/live/sessions/1?step=2&tab=scripts')

    expect(await screen.findByText('话术主体')).toBeInTheDocument()
    expect(screen.getByTestId('session-workspace-root')).toHaveAttribute('data-step', '2')
    expect(screen.getByTestId('session-workspace-root')).toHaveAttribute('data-tab', 'scripts')
    expect(screen.getByTestId('session-workspace-scripts-pane')).toHaveAttribute('data-contract-owner', 'ScriptTabContent')
    expect(screen.queryByText('选品主体')).not.toBeInTheDocument()
  })

  it('honors direct session route tab query and keeps role scope', async () => {
    renderWrapperWithRoute('/org/live/sessions/1?tab=readiness')

    expect(await screen.findByText('准备度主体')).toBeInTheDocument()
    expect(screen.getByTestId('session-workspace-root')).toHaveAttribute('data-route-scope', 'org')
    expect(screen.getByTestId('session-workspace-root')).toHaveAttribute('data-tab', 'readiness')
    expect(screen.getByTestId('session-workspace-readiness-pane')).toHaveAttribute('data-contract-owner', 'ReadinessTab')
  })

  it('renders not found without local session fallback when session is absent', async () => {
    vi.mocked(liveApi.sessionGet).mockResolvedValueOnce(null as never)

    renderWrapperWithRoute('/talent/live/sessions/1')

    const notFound = await screen.findByTestId('session-workspace-not-found')
    expect(notFound).toHaveAttribute('data-contract-source', '/live/session/get')
    expect(notFound).toHaveAttribute('data-no-local-session-fallback', 'true')
    expect(notFound).toHaveTextContent('场次不存在')
  })
})
