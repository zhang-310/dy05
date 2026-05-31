import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import OrgLiveSessionPage from '../OrgLiveSessionPage'
import { liveApi } from '@/api/live'
import { ToastProvider } from '@/contexts/ToastContext'

vi.mock('@/api/live')

const navigate = vi.hoisted(() => vi.fn())

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

const orgSessions = [
  {
    id: 18,
    liveTitle: '机构护肤专场',
    status: 0,
    sessionType: '品牌专场',
    liveFormat: '单人',
    scriptStyle: '专业',
    scheduledTime: '2026-05-24T20:00:00',
    totalGmv: 120000,
    viewers: 3000,
    likes: 900,
    accountId: 101,
    personaId: 201,
    liveDescription: '机构端直播场次',
    createTime: '2026-05-20T10:00:00',
  },
]

function renderOrgPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: Infinity },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={['/org/live/sessions']}>
          <Routes>
            <Route path="/org/live/sessions" element={<OrgLiveSessionPage />} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('OrgLiveSessionPage route wrapper contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    navigate.mockClear()
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      list: orgSessions,
      total: orgSessions.length,
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('declares org route ownership while delegating data operations to SessionsPage', async () => {
    renderOrgPage()

    const wrapper = screen.getByTestId('org-live-session-entry-workbench')
    expect(wrapper).toHaveAttribute('data-contract-scope', 'org-live-session-route-wrapper')
    expect(wrapper).toHaveAttribute('data-route-scope', 'org')
    expect(wrapper).toHaveAttribute('data-child-page', 'SessionsPage')
    expect(wrapper).toHaveAttribute('data-api-owner', 'SessionsPage')
    expect(wrapper.getAttribute('data-ready-endpoints')).toContain('/live/session/search')
    expect(wrapper.getAttribute('data-ready-endpoints')).toContain('/live/session/save')
    expect(wrapper.getAttribute('data-ready-endpoints')).toContain('/live/session/status')
    expect(wrapper).toHaveAttribute('data-workbench-path-template', '/org/live/sessions/:id')
    expect(wrapper).toHaveAttribute('data-admin-path-template', 'unsupported')
    expect(wrapper).toHaveAttribute('data-no-admin-route-leak', 'true')
    expect(wrapper).toHaveAttribute('data-no-local-session-fallback', 'true')

    expect(await screen.findByRole('heading', { name: '机构端直播场次' })).toBeInTheDocument()
    const child = screen.getByTestId('live-sessions-workbench')
    expect(child).toHaveAttribute('data-route-scope', 'org')
    expect(child).toHaveAttribute('data-is-admin-shell', 'false')
    expect(await screen.findByText('机构护肤专场')).toBeInTheDocument()
    expect(liveApi.sessionSearch).toHaveBeenCalledWith(expect.objectContaining({
      page: 0,
      rows: 20,
    }))
  })

  it('keeps org shell actions away from admin-only batch/delete and routes workbench inside org', async () => {
    const user = userEvent.setup()
    renderOrgPage()

    expect(await screen.findByText('机构护肤专场')).toBeInTheDocument()
    expect(screen.getByTestId('org-live-session-entry-workbench')).toHaveAttribute('data-no-admin-batch-actions', 'true')
    expect(screen.getByTestId('org-live-session-entry-workbench')).toHaveAttribute('data-no-admin-delete', 'true')
    expect(screen.queryByLabelText('删除')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '批量删除' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '批量开播' })).not.toBeInTheDocument()

    await user.click(screen.getByLabelText('进入工作台'))

    expect(navigate).toHaveBeenCalledWith('/org/live/sessions/18')
    expect(navigate).not.toHaveBeenCalledWith(expect.stringContaining('/admin/live/sessions/18'))
  })

  it('shows search failures through child page without local org fallback rows', async () => {
    vi.mocked(liveApi.sessionSearch).mockRejectedValueOnce(new Error('org live down'))

    renderOrgPage()

    const wrapper = screen.getByTestId('org-live-session-entry-workbench')
    expect(wrapper).toHaveAttribute('data-no-local-session-fallback', 'true')
    const error = await screen.findByTestId('live-sessions-list-error')
    expect(error).toHaveAttribute('data-contract-source', '/live/session/search')
    expect(error).toHaveAttribute('data-no-local-session-fallback', 'true')
    expect(error).toHaveTextContent('org live down')
    expect(screen.queryByText('机构护肤专场')).not.toBeInTheDocument()
  })
})
