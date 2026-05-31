import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import SessionsPage from './SessionsPage'
import { liveApi } from '@/api/live'
import { ToastProvider } from '@/contexts/ToastContext'

vi.mock('@/api/live')

const navigate = vi.hoisted(() => vi.fn())
let mockPathname = '/admin/live/sessions'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useLocation: () => ({ pathname: mockPathname, search: '', hash: '', state: null, key: 'test' }),
  }
})

const mockSessions = [
  {
    id: 1,
    liveTitle: '护肤品专场',
    status: 0,
    sessionType: '品牌专场',
    liveFormat: '单人',
    scriptStyle: '专业',
    scheduledTime: '2026-05-15T19:00:00',
    totalGmv: 150000,
    viewers: 5000,
    likes: 1200,
    accountId: 101,
    personaId: 201,
    liveDescription: '春季护肤专场',
    createTime: '2026-05-10T10:00:00',
  },
  {
    id: 2,
    liveTitle: '彩妆大促',
    status: 1,
    sessionType: '大促',
    liveFormat: '多人',
    scriptStyle: '激情',
    scheduledTime: '2026-05-16T20:00:00',
    totalGmv: 280000,
    viewers: 8000,
    likes: 2500,
    accountId: 102,
    personaId: 202,
    liveDescription: '618预热',
    createTime: '2026-05-11T11:00:00',
  },
]

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: Infinity },
      mutations: { retry: false }
    },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastProvider>
          {ui}
        </ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

describe('SessionsPage - 核心功能测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    navigate.mockClear()
    mockPathname = '/admin/live/sessions'
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      list: mockSessions,
      total: 2,
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('应该正确加载并显示场次列表', async () => {
    renderWithProviders(<SessionsPage />)

    await waitFor(() => {
      expect(screen.getByText('护肤品专场')).toBeInTheDocument()
      expect(screen.getByText('彩妆大促')).toBeInTheDocument()
    })

    expect(liveApi.sessionSearch).toHaveBeenCalled()
  })

  it('应该暴露场次列表工作台契约和禁用邻域链路', async () => {
    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const root = screen.getByTestId('live-sessions-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'live-session-list')
    expect(root).toHaveAttribute('data-route-scope', 'admin')
    expect(root).toHaveAttribute('data-is-admin-shell', 'true')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/live/session/search')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/live/session/save')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/live/session/delete')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/live/session/status')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/live/session/clone')
    expect(root.getAttribute('data-context-endpoints')).toContain('/live/session/search')
    expect(root.getAttribute('data-unsupported-actions')).toContain('list-shortvideo-export')
    expect(root.getAttribute('data-unsupported-actions')).toContain('list-realtime-panel-direct')
    expect(root.getAttribute('data-unsupported-actions')).toContain('local-session-fallback')
    expect(root).toHaveAttribute('data-no-local-session-fallback', 'true')

    const filterPanel = screen.getByTestId('live-sessions-filter-panel')
    expect(filterPanel).toHaveAttribute('data-contract-source', '/live/session/search')
    expect(filterPanel).toHaveAttribute('data-scope', 'admin')
    expect(filterPanel).toHaveAttribute('data-keyword', 'empty')
    expect(filterPanel).toHaveAttribute('data-status-filter', 'all')
    expect(filterPanel).toHaveAttribute('data-no-local-session-fallback', 'true')

    expect(screen.getByTestId('live-sessions-kpi-grid')).toHaveAttribute('data-contract-source', '/live/session/search')
    expect(screen.getByTestId('live-sessions-table-surface')).toHaveAttribute('data-no-server-export', 'true')
  })

  it('GMV 单元格使用主题色并清理旧固定色', async () => {
    renderWithProviders(<SessionsPage />)

    expect(await screen.findByText('¥15.0万')).toBeInTheDocument()
    const gmvValues = screen.getAllByTestId('live-session-gmv-value-surface')
    expect(gmvValues[0]).toHaveAttribute('data-gmv-tone', 'success')
    expect(document.body.innerHTML).not.toContain('#bbb')
    expect(document.body.innerHTML).not.toContain('#1a7f3c')
  })

  it('应该验证创建场次的必填字段', async () => {
    const user = userEvent.setup()
    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))
    await user.click(screen.getByRole('button', { name: '新建场次' }))

    const dialog = screen.getByRole('dialog')
    expect(dialog).toBeInTheDocument()

    const saveButton = screen.getByRole('button', { name: '保存' })
    await user.click(saveButton)

    await waitFor(() => {
      expect(screen.getByText('请输入场次标题')).toBeInTheDocument()
    })
    expect(liveApi.sessionSave).not.toHaveBeenCalled()
  })

  it('应该成功删除单个场次', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionDelete).mockResolvedValue(undefined)

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const deleteButtons = screen.getAllByLabelText('删除')
    await user.click(deleteButtons[0])
    await user.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(liveApi.sessionDelete).toHaveBeenCalledWith(1)
    })
  })

  it('应该支持批量删除', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionDelete).mockResolvedValue(undefined)

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[1])
    await user.click(checkboxes[2])

    await waitFor(() => {
      expect(screen.getByText('已选 2 条')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '批量删除' }))

    await waitFor(() => {
      expect(liveApi.sessionDelete).toHaveBeenCalledTimes(2)
    })
  })

  it('批量删除把后端空载荷 204 视为成功，避免实际已删却提示失败', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionDelete).mockRejectedValue(new Error('资源或者信息为空!'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[1])
    await user.click(checkboxes[2])
    await user.click(screen.getByRole('button', { name: '批量删除' }))

    await waitFor(() => {
      expect(liveApi.sessionDelete).toHaveBeenCalledTimes(2)
    })
    expect(await screen.findByText(/批量删除成功/)).toBeInTheDocument()
    expect(screen.queryByTestId('live-sessions-operation-error')).not.toBeInTheDocument()
  })

  it('批量删除遇到已不存在的场次按幂等成功处理', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionDelete)
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error('直播场次不存在'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[1])
    await user.click(checkboxes[2])
    await user.click(screen.getByRole('button', { name: '批量删除' }))

    await waitFor(() => {
      expect(liveApi.sessionDelete).toHaveBeenCalledTimes(2)
    })
    expect(await screen.findByText(/批量删除成功/)).toBeInTheDocument()
    expect(screen.queryByTestId('live-sessions-operation-error')).not.toBeInTheDocument()
  })

  it('应该成功开播', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionStart).mockResolvedValue(undefined)

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const startButton = screen.getByLabelText('开播')
    await user.click(startButton)

    await waitFor(() => {
      expect(liveApi.sessionStart).toHaveBeenCalledWith(1)
    })
  })

  it('开播把后端空载荷 204 视为成功', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionStart).mockRejectedValue(new Error('资源或者信息为空!'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    await user.click(screen.getByLabelText('开播'))

    await waitFor(() => {
      expect(liveApi.sessionStart).toHaveBeenCalledWith(1)
    })
    expect(await screen.findByText('已开播')).toBeInTheDocument()
    expect(screen.queryByTestId('live-sessions-operation-error')).not.toBeInTheDocument()
  })

  it('应该支持批量开播', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionStart).mockResolvedValue(undefined)

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[1])

    await user.click(screen.getByRole('button', { name: '批量开播' }))

    await waitFor(() => {
      expect(liveApi.sessionStart).toHaveBeenCalledWith(1)
    })
  })

  it('应该支持批量结束', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionEnd).mockResolvedValue(undefined)

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('彩妆大促'))

    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[2])

    await user.click(screen.getByRole('button', { name: '批量结束' }))

    await waitFor(() => {
      expect(liveApi.sessionEnd).toHaveBeenCalledWith(2)
    })
  })

  it('应该成功克隆场次', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionClone).mockResolvedValue({ id: 5 })

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const cloneButtons = screen.getAllByLabelText('克隆')
    await user.click(cloneButtons[0])

    await waitFor(() => {
      expect(liveApi.sessionClone).toHaveBeenCalledWith(1)
    })
  })

  it('应该处理删除失败', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionDelete).mockRejectedValue(new Error('Network error'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const deleteButtons = screen.getAllByLabelText('删除')
    await user.click(deleteButtons[0])
    await user.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(screen.getByText('删除失败')).toBeInTheDocument()
    })
    expect(screen.getByText(/\/live\/session\/delete 删除场次失败：Network error/)).toBeInTheDocument()
    expect(screen.getAllByText(/sessionId=1; liveTitle=护肤品专场/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/scope=admin; shell=管理员端/).length).toBeGreaterThan(0)
  })

  it('应该处理开播失败', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionStart).mockRejectedValue(new Error('Network error'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const startButton = screen.getByLabelText('开播')
    await user.click(startButton)

    await waitFor(() => {
      expect(screen.getByText('操作失败')).toBeInTheDocument()
    })
    expect(screen.getByText(/\/live\/session\/status 开播失败：Network error/)).toBeInTheDocument()
    expect(screen.getAllByText(/sessionId=1; liveTitle=护肤品专场/).length).toBeGreaterThan(0)
    expect(screen.getByText(/keyword=空; statusFilter=全部/)).toBeInTheDocument()
  })

  it('应该处理克隆失败', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionClone).mockRejectedValue(new Error('Network error'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const cloneButtons = screen.getAllByLabelText('克隆')
    await user.click(cloneButtons[0])

    await waitFor(() => {
      expect(screen.getByText('克隆失败')).toBeInTheDocument()
    })
    expect(screen.getByText(/\/live\/session\/clone 克隆场次失败：Network error/)).toBeInTheDocument()
    expect(screen.getAllByText(/sessionId=1; liveTitle=护肤品专场/).length).toBeGreaterThan(0)
  })

  it('应该在保存失败时保留弹窗和接口来源', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionSave).mockRejectedValue(new Error('save down'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))
    await user.click(screen.getByRole('button', { name: '新建场次' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText(/场次标题/), '新品场')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/\/live\/session\/save 保存场次失败：save down/)).toBeInTheDocument()
    expect(screen.getByText(/sessionId=new; liveTitle=新品场/)).toBeInTheDocument()
    expect(screen.getAllByText(/scope=admin; shell=管理员端/).length).toBeGreaterThan(0)
    expect(dialog).toBeInTheDocument()
    expect(dialog).toHaveAttribute('data-testid', 'live-sessions-save-dialog')
    expect(dialog).toHaveAttribute('data-contract-source', '/live/session/save')
    expect(dialog).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('live-sessions-save-error')).toHaveAttribute('data-no-local-session-mutation', 'true')
  })

  it('应该处理批量开播失败并显示接口来源', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.sessionStart).mockRejectedValue(new Error('batch start down'))

    renderWithProviders(<SessionsPage />)

    await waitFor(() => screen.getByText('护肤品专场'))

    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[1])
    await user.click(screen.getByRole('button', { name: '批量开播' }))

    expect(await screen.findByText(/\/live\/session\/status 批量开播失败：batch start down/)).toBeInTheDocument()
    expect(screen.getByText(/selectedIds=1/)).toBeInTheDocument()
    expect(screen.getByText(/selectedTitles=1:护肤品专场/)).toBeInTheDocument()
  })

  it('应该显示空状态引导', async () => {
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    renderWithProviders(<SessionsPage />)

    await waitFor(() => {
      expect(screen.getByText('还没有直播场次')).toBeInTheDocument()
      expect(screen.getByText('创建第一个直播场次，开始您的直播运营之旅')).toBeInTheDocument()
    })
  })

  it('机构端进入工作台时留在 org 路由壳', async () => {
    const user = userEvent.setup()
    mockPathname = '/org/live/sessions'

    renderWithProviders(<SessionsPage />)

    expect(await screen.findByRole('heading', { name: '机构端直播场次' })).toBeInTheDocument()
    expect(screen.getByText(/当前是 机构端 路由/)).toBeInTheDocument()
    expect(await screen.findByText('护肤品专场')).toBeInTheDocument()

    const workbenchButtons = screen.getAllByLabelText('进入工作台')
    await user.click(workbenchButtons[0])

    expect(navigate).toHaveBeenCalledWith('/org/live/sessions/1')
  })

  it('机构端隐藏管理员批量和删除动作', async () => {
    mockPathname = '/org/live/sessions'

    renderWithProviders(<SessionsPage />)

    expect(await screen.findByRole('heading', { name: '机构端直播场次' })).toBeInTheDocument()
    expect(screen.getByText(/批量开播、批量结束、批量删除和删除场次只在管理员端展示/)).toBeInTheDocument()
    const shellAlert = screen.getByTestId('live-sessions-role-shell-alert')
    expect(shellAlert).toHaveAttribute('data-route-scope', 'org')
    expect(shellAlert).toHaveAttribute('data-no-admin-batch-actions', 'true')
    expect(shellAlert).toHaveAttribute('data-no-admin-delete', 'true')
    expect(shellAlert).toHaveAttribute('data-workbench-path-template', '/org/live/sessions/:id')
    expect(await screen.findByText('护肤品专场')).toBeInTheDocument()
    expect(screen.queryByLabelText('删除')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '批量删除' })).not.toBeInTheDocument()
  })

  it('达人端进入工作台时留在 talent 路由壳', async () => {
    const user = userEvent.setup()
    mockPathname = '/talent/live/sessions'

    renderWithProviders(<SessionsPage />)

    expect(await screen.findByRole('heading', { name: '达人端直播场次' })).toBeInTheDocument()
    expect(await screen.findByText('护肤品专场')).toBeInTheDocument()
    const workbenchButtons = screen.getAllByLabelText('进入工作台')
    await user.click(workbenchButtons[0])

    expect(navigate).toHaveBeenCalledWith('/talent/live/sessions/1')
  })

  it('达人端隐藏管理员批量和删除动作', async () => {
    mockPathname = '/talent/live/sessions'

    renderWithProviders(<SessionsPage />)

    expect(await screen.findByRole('heading', { name: '达人端直播场次' })).toBeInTheDocument()
    expect(await screen.findByText('彩妆大促')).toBeInTheDocument()
    expect(screen.queryByLabelText('删除')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '批量开播' })).not.toBeInTheDocument()
    expect(screen.getByText(/当前壳保留查看、编辑、开播、结束和克隆/)).toBeInTheDocument()
  })

  it('达人端开播失败时显示路由壳和场次上下文', async () => {
    const user = userEvent.setup()
    mockPathname = '/talent/live/sessions'
    vi.mocked(liveApi.sessionStart).mockRejectedValue(new Error('talent status down'))

    renderWithProviders(<SessionsPage />)

    expect(await screen.findByRole('heading', { name: '达人端直播场次' })).toBeInTheDocument()
    expect(await screen.findByText('护肤品专场')).toBeInTheDocument()
    await user.click(screen.getByLabelText('开播'))

    expect(await screen.findByText(/\/live\/session\/status 开播失败：talent status down/)).toBeInTheDocument()
    expect(screen.getAllByText(/scope=talent; shell=达人端/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/sessionId=1; liveTitle=护肤品专场/).length).toBeGreaterThan(0)
  })

  it('列表失败时显示接口和筛选上下文', async () => {
    vi.mocked(liveApi.sessionSearch).mockRejectedValueOnce(new Error('live list down'))

    renderWithProviders(<SessionsPage />)

    expect(await screen.findByText(/直播场次加载失败：POST \/live\/session\/search：live list down/)).toBeInTheDocument()
    const errorAlert = screen.getByTestId('live-sessions-list-error')
    expect(errorAlert).toHaveAttribute('data-contract-source', '/live/session/search')
    expect(errorAlert).toHaveAttribute('data-no-local-session-fallback', 'true')
    expect(screen.getByText(/scope=admin; shell=管理员端; keyword=空; statusFilter=全部/)).toBeInTheDocument()
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      list: mockSessions,
      total: 2,
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('空状态不注入本地静态场次兜底', async () => {
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({
      list: [],
      total: 0,
      pageNum: 0,
      pageSize: 20,
    })

    renderWithProviders(<SessionsPage />)

    const empty = await screen.findByTestId('live-sessions-empty-state')
    expect(empty).toHaveAttribute('data-contract-source', '/live/session/search')
    expect(empty).toHaveAttribute('data-no-local-session-fallback', 'true')
    expect(screen.queryByText('护肤品专场')).not.toBeInTheDocument()
    expect(screen.queryByText('彩妆大促')).not.toBeInTheDocument()
  })
})
