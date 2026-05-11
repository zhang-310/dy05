import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import SessionsPage from './SessionsPage'
import { liveApi } from '@/api/live'
import { ToastProvider } from '@/contexts/ToastContext'

vi.mock('@/api/live')

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

    await waitFor(() => {
      expect(screen.getByText('删除失败')).toBeInTheDocument()
    })
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
})
