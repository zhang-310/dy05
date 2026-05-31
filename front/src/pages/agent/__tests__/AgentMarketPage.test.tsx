import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import AgentMarketPage from '../AgentMarketPage'
import { agentApi, reviewApi } from '@/api/agent'

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/api/agent', () => ({
  agentApi: {
    list: vi.fn(),
  },
  reviewApi: {
    submit: vi.fn(),
    myReview: vi.fn(),
  },
}))

vi.mock('../AgentChatPage', () => ({
  SKILL_MAP: {
    kb_rag_search: {
      label: '知识库检索',
      description: '检索知识库',
      icon: null,
    },
  },
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <AgentMarketPage />
    </MemoryRouter>,
  )
}

describe('AgentMarketPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(agentApi.list).mockResolvedValue({
      total: 1,
      list: [{
        id: 7,
        agentName: '话术生成助手',
        description: '专业的直播话术生成工具',
        agentType: 1,
        responseMode: 1,
        status: 1,
        createTime: '2026-05-21 09:00:00',
        averageRating: 4.5,
        ratingCount: 12,
        conversationCount: 88,
        availableTools: '["kb_rag_search"]',
      }],
      pageNum: 0,
      pageSize: 100,
    } as never)
    vi.mocked(reviewApi.myReview).mockResolvedValue(null as never)
    vi.mocked(reviewApi.submit).mockResolvedValue(1 as never)
  })

  it('renders real list contract and admin-prefixed actions', async () => {
    renderPage()

    expect(await screen.findByText('话术生成助手')).toBeInTheDocument()
    expect(screen.getByTestId('agent-market-page')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/agent/review/submit'))
    expect(screen.getByTestId('agent-market-page')).toHaveAttribute('data-no-static-rating-fallback', 'true')
    expect(screen.getByTestId('agent-market-grid')).toHaveAttribute('data-no-local-agent-fallback', 'true')
    expect(screen.getByText('列表来自 `/agent/list`，排序参数由后端消费；评分提交走 `/agent/review/submit`，失败会保留在弹窗内提示。')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '开始对话' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/chat/7')

    fireEvent.click(screen.getByRole('button', { name: '管理智能体' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/list')
  })

  it('shows list load error with retry', async () => {
    vi.mocked(agentApi.list).mockRejectedValueOnce(new Error('Network error'))

    renderPage()

    expect(await screen.findByText('智能体市场加载失败')).toBeInTheDocument()
    expect(screen.getByTestId('agent-market-list-error')).toHaveAttribute('data-no-local-agent-fallback', 'true')
    expect(screen.getByText(/Network error/)).toBeInTheDocument()

    vi.mocked(agentApi.list).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 100,
    } as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    await waitFor(() => {
      expect(agentApi.list).toHaveBeenCalledTimes(2)
    })
  })

  it('keeps review submit errors visible in dialog', async () => {
    vi.mocked(reviewApi.submit).mockRejectedValueOnce(new Error('rating failed'))

    renderPage()

    expect(await screen.findByText('话术生成助手')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '评价 话术生成助手' }))
    expect(await screen.findByText('评价「话术生成助手」')).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: /评价内容/ }), {
      target: { value: '这个智能体适合直播间话术改写' },
    })
    fireEvent.click(within(screen.getByRole('dialog')).getAllByRole('button')[0])
    fireEvent.click(screen.getByRole('button', { name: '提交评价' }))

    expect(await screen.findByTestId('agent-review-submit-error')).toHaveTextContent(/提交评分失败（POST \/agent\/review\/submit）：rating failed/)
    expect(screen.getByTestId('agent-review-submit-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('这个智能体适合直播间话术改写')).toBeInTheDocument()
  })

  it('shows my review load endpoint while keeping submit available', async () => {
    vi.mocked(reviewApi.myReview).mockRejectedValueOnce(new Error('review read failed'))

    renderPage()

    expect(await screen.findByText('话术生成助手')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '评价 话术生成助手' }))

    expect(await screen.findByTestId('agent-review-my-error')).toHaveTextContent(/读取已有评分失败（POST \/agent\/review\/my）：review read failed/)
    expect(screen.getByTestId('agent-review-my-error')).toHaveAttribute('data-no-local-review-fallback', 'true')
    expect(screen.getByRole('button', { name: '提交评价' })).toBeDisabled()
  })

  it('uses theme-aware agent type surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(agentApi.list).mockResolvedValue({
      total: 1,
      list: [{
        id: 8,
        agentName: '商品分析助手',
        description: '分析商品卖点和转化',
        agentType: 3,
        responseMode: 1,
        status: 1,
        createTime: '2026-05-21 09:00:00',
        averageRating: 4.8,
        ratingCount: 9,
        conversationCount: 66,
        availableTools: '["kb_rag_search"]',
      }],
      pageNum: 0,
      pageSize: 100,
    } as never)

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AgentMarketPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByText('商品分析助手')).toBeInTheDocument()
    expect(screen.getByTestId('agent-market-type-avatar-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
    expect(screen.getByTestId('agent-market-type-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
  })
})
