import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AgentSharePage from '../AgentSharePage'
import { shareApi } from '@/api/agent'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

const navigate = vi.fn()
let routeShareCode = 'share-abc'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useParams: () => ({ shareCode: routeShareCode }),
  }
})

vi.mock('@/api/agent', () => ({
  shareApi: {
    getData: vi.fn(),
  },
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <AgentSharePage />
    </MemoryRouter>,
  )
}

function renderPageWithAppTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <AgentSharePage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('AgentSharePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    routeShareCode = 'share-abc'
    vi.mocked(shareApi.getData).mockResolvedValue({
      share: {
        id: 1,
        shareCode: 'share-abc',
        conversationId: 101,
        agentId: 7,
        title: '敏感肌直播脚本',
        summary: '脚本讨论摘要',
        messageCount: 1,
        viewCount: 3,
        isPublic: 1,
      },
      messages: {
        records: [
          {
            id: 2,
            role: 'assistant',
            content: '## 回复标题\n\n- 要点',
            createdAt: '2026-05-21 10:00:00',
            toolCalls: '[{"type":"function","function":{"name":"kb_rag_search","arguments":"{}"}}]',
          },
        ],
      },
    } as never)
  })

  it('renders wrapped markdown messages and uses admin-prefixed navigation', async () => {
    renderPage()

    expect(await screen.findAllByText('敏感肌直播脚本')).toHaveLength(2)
    expect(screen.getByTestId('agent-share-page')).toHaveAttribute('data-ready-endpoints', '/agent/share/data')
    expect(screen.getByTestId('agent-share-page')).toHaveAttribute('data-no-static-message-fallback', 'true')
    expect(screen.getByTestId('agent-share-message-list')).toHaveAttribute('data-source-endpoint', '/agent/share/data')
    expect(screen.getByRole('heading', { name: '回复标题' })).toBeInTheDocument()
    expect(screen.getByTestId('agent-share-assistant-message-bubble')).toHaveAttribute('data-markdown-renderer', 'MarkdownViewer')
    expect(screen.getByText('kb_rag_search')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '返回' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/market')

    fireEvent.click(screen.getByRole('button', { name: '与智能体对话' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/chat/7')
  })

  it('handles non-array messages as empty conversation', async () => {
    vi.mocked(shareApi.getData).mockResolvedValueOnce({
      share: {
        id: 1,
        shareCode: 'share-abc',
        conversationId: 101,
        agentId: 7,
        title: '空分享',
        messageCount: 0,
        viewCount: 0,
        isPublic: 1,
      },
      messages: {} as never,
    } as never)

    renderPage()

    expect(await screen.findAllByText('空分享')).toHaveLength(2)
    expect(screen.getByTestId('agent-share-empty-messages')).toHaveTextContent('暂无消息记录')
    expect(screen.getByTestId('agent-share-empty-messages')).toHaveAttribute('data-no-static-message-fallback', 'true')
  })

  it('returns to agent market when share loading fails', async () => {
    vi.mocked(shareApi.getData).mockRejectedValueOnce(new Error('share expired'))

    renderPage()

    expect(await screen.findByTestId('agent-share-load-error')).toHaveTextContent(/分享内容加载失败（POST \/agent\/share\/data）：share expired/)
    expect(screen.getByTestId('agent-share-page-error')).toHaveAttribute('data-no-local-share-fallback', 'true')
    fireEvent.click(screen.getByRole('button', { name: '返回智能体市场' }))
    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/market')
    })
  })

  it('uses dark theme surfaces for shared assistant message bubbles', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithAppTheme()

    expect(await screen.findByRole('heading', { name: '回复标题' })).toBeInTheDocument()
    expect(screen.getByTestId('agent-share-assistant-message-bubble')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })
})
