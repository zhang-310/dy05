import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import AgentListPage from '../AgentListPage'
import { agentApi } from '@/api/agent'
import { aiApi } from '@/api/ai'

vi.mock('@/api/agent', () => ({
  agentApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
  },
}))

vi.mock('notistack', () => ({
  useSnackbar: () => ({ enqueueSnackbar: vi.fn() }),
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('AgentListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(agentApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          agentName: '话术助手',
          description: '生成直播话术',
          agentType: 1,
          responseMode: 0,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.kbList).mockResolvedValue([] as never)
  })

  it('renders title and loads agent list', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '智能体管理' })).toBeInTheDocument()

    await waitFor(() => {
      expect(agentApi.list).toHaveBeenCalled()
    })

    await waitFor(
      () => {
        expect(screen.getByText('话术助手')).toBeInTheDocument()
        expect(screen.getByText('话术生成')).toBeInTheDocument()
      },
      { timeout: 25000 },
    )
  }, 30000)
})
