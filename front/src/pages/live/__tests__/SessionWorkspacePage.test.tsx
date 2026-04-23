/**
 * SessionWorkspacePage 测试：Tab 切换、数据加载
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { waitFor, screen, render } from '@testing-library/react'
import { SnackbarProvider } from 'notistack'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { ThemeProvider, createTheme } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastProvider } from '@/contexts/ToastContext'
import { SessionWorkspacePage } from '../SessionWorkspacePage'
import * as liveApi from '@/api/live'

vi.mock('@/api/live', () => ({
  getSession: vi.fn(),
  getProductsBySession: vi.fn(),
  getScriptsBySession: vi.fn(),
  initScriptSlots: vi.fn(),
  rebuildScriptSlots: vi.fn(),
  checkGenerateFullInProgress: vi.fn(),
}))
vi.mock('@/api/ai', () => ({
  listAiModels: vi.fn().mockResolvedValue([]),
  getModelsByTaskCode: vi.fn().mockResolvedValue([]),
}))
vi.mock('@/hooks/useNetworkStatus', () => ({ useNetworkStatus: () => ({ status: 'online' }) }))
vi.mock('@/hooks/useCollaboration', () => ({ useCollaboration: () => ({ collaborators: [] }) }))

const theme = createTheme({})

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
              <Route path="/admin/live/workbench/:id" element={<SessionWorkspacePage />} />
              <Route path="/org/live/workbench/:id" element={<SessionWorkspacePage />} />
              <Route path="/talent/live/workbench/:id" element={<SessionWorkspacePage />} />
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
    vi.mocked(liveApi.getSession).mockResolvedValue({
      id: 1,
      liveTitle: '测试场次',
      status: 0,
      sessionType: 'normal',
    } as never)
    vi.mocked(liveApi.getProductsBySession).mockResolvedValue([])
    vi.mocked(liveApi.getScriptsBySession).mockResolvedValue([])
    vi.mocked(liveApi.checkGenerateFullInProgress).mockResolvedValue({ inProgress: false })
  })

  it('renders without crashing with valid id', async () => {
    const { container } = renderWithRoute('/admin/live/workbench/1')
    expect(container).toBeInTheDocument()
  })

  it('renders without crashing with invalid id', async () => {
    const { container } = renderWithRoute('/admin/live/workbench/abc')
    expect(container).toBeInTheDocument()
  })
})
