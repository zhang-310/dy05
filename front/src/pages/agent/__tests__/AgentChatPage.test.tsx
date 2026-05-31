import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AgentChatPage from '../AgentChatPage'
import { agentApi, shareApi } from '@/api/agent'
import { ssePost } from '@/utils/sse-client'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useParams: () => ({ id: '7' }),
  }
})

vi.mock('@/api/agent', () => ({
  agentApi: {
    get: vi.fn(),
    conversationCreate: vi.fn(),
    conversationList: vi.fn(),
    conversationDelete: vi.fn(),
    messageSend: vi.fn(),
    messageList: vi.fn(),
    messageRate: vi.fn(),
    exportConversation: vi.fn(),
  },
  shareApi: {
    create: vi.fn(),
  },
}))

vi.mock('@/utils/sse-client', () => ({
  ssePost: vi.fn(),
}))

const toast = vi.fn()
const writeText = vi.fn()
const openSpy = vi.fn()
let messageListCalls = 0
let lastSseCallbacks: any

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <AgentChatPage />
    </MemoryRouter>,
  )
}

function renderPageWithAppTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <AgentChatPage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('AgentChatPage UI', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    messageListCalls = 0
    lastSseCallbacks = undefined
    Object.defineProperty(window, 'open', {
      configurable: true,
      value: openSpy,
    })
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, origin: 'http://localhost:3000' },
    })
    Object.assign(navigator, {
      clipboard: { writeText },
    })
    Element.prototype.scrollIntoView = vi.fn()

    vi.mocked(agentApi.get).mockResolvedValue({
      id: 7,
      agentName: '话术生成助手',
      description: '专业直播话术生成',
      agentType: 1,
      responseMode: 1,
      status: 1,
      createTime: '2026-05-15 09:00:00',
      availableTools: '["kb_rag_search","product_search","compliance_check"]',
    } as never)
    vi.mocked(agentApi.conversationList).mockResolvedValue({
      records: [
        {
          id: 101,
          title: '敏感肌直播脚本',
          lastMessage: '上一条消息',
          createTime: '2026-05-15 08:00:00',
          updateTime: '2026-05-15 08:30:00',
        },
      ],
    } as never)
    vi.mocked(agentApi.messageList).mockImplementation(async () => {
      messageListCalls += 1
      if (messageListCalls > 1) {
        return {
          items: [
            {
              id: 1,
              role: 'user',
              content: '帮我生成开场白',
              createdAt: '2026-05-15 08:01:00',
              tokenUsage: { input: 12, output: 0 },
            },
            {
              id: 2,
              role: 'assistant',
              content: '## 这是已有回复\n\n- 要点一\n- 要点二',
              createdAt: '2026-05-15 08:01:10',
              tokenUsage: { input: 0, output: 30 },
              rating: null,
              toolCalls: '[{"id":"tool-1","type":"function","function":{"name":"kb_rag_search","arguments":"{}"}}]',
            },
            {
              id: 3,
              role: 'assistant',
              content: '这是流式完成后的新回复。',
              createdAt: '2026-05-15 08:02:10',
              tokenUsage: { input: 0, output: 18 },
              rating: null,
            },
          ],
        } as never
      }
      return {
        list: [
          {
            id: 1,
            role: 'user',
            content: '帮我生成开场白',
            createdAt: '2026-05-15 08:01:00',
            tokenUsage: { input: 12, output: 0 },
          },
            {
              id: 2,
              role: 'assistant',
              content: '## 这是已有回复\n\n- 要点一\n- 要点二',
              createdAt: '2026-05-15 08:01:10',
              tokenUsage: { input: 0, output: 30 },
              rating: null,
            toolCalls: [
              {
                id: 'tool-1',
                type: 'function',
                function: { name: 'kb_rag_search', arguments: '{}' },
              },
            ],
          },
        ],
      } as never
    })
    vi.mocked(agentApi.conversationCreate).mockResolvedValue(202 as never)
    vi.mocked(agentApi.conversationDelete).mockResolvedValue(undefined as never)
    vi.mocked(agentApi.messageSend).mockResolvedValue(10 as never)
    vi.mocked(agentApi.messageRate).mockResolvedValue(undefined as never)
    vi.mocked(agentApi.exportConversation).mockResolvedValue({ downloadUrl: '/download/chat.md' } as never)
    vi.mocked(shareApi.create).mockResolvedValue({
      id: 9,
      shareCode: 'share-abc',
      conversationId: 101,
      agentId: 7,
      title: '敏感肌直播脚本',
      messageCount: 3,
      viewCount: 0,
      isPublic: 1,
      createTime: '2026-05-15 08:00:00',
    } as never)
    vi.mocked(ssePost).mockImplementation((_url, _body, callbacks: any) => {
      lastSseCallbacks = callbacks
      callbacks.onSkillStart({ tool: 'kb_rag_search', description: '检索知识库' })
      callbacks.onChunk(JSON.stringify({ type: 'chunk', content: '正在生成' }))
      return { abort: vi.fn(), signal: { aborted: false } } as unknown as AbortController
    })
  })

  it('runs the main chat workflow with streaming, rating, export, share, copy, clear, and conversation mutations', async () => {
    renderPage()

    expect(screen.getByText('智能体对话')).toBeInTheDocument()
    expect(screen.getByTestId('agent-chat-page')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/agent/chat-stream'))
    expect(screen.getByTestId('agent-chat-page')).toHaveAttribute('data-no-local-message-fallback', 'true')
    await waitFor(() => {
      expect(agentApi.get).toHaveBeenCalledWith(7)
      expect(agentApi.conversationList).toHaveBeenCalledWith(7)
    })

    fireEvent.click(await screen.findByText('敏感肌直播脚本'))
    await waitFor(() => {
      expect(agentApi.messageList).toHaveBeenCalledWith(101)
    })
    expect(await screen.findByRole('heading', { name: '这是已有回复' })).toBeInTheDocument()
    expect(screen.getByText('要点一')).toBeInTheDocument()
    expect(screen.getByText('名称: 话术生成助手')).toBeInTheDocument()
    expect(screen.getByText('类型: 话术生成')).toBeInTheDocument()
    expect(screen.getByText('模式: 流式')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getAllByText('知识库检索').length).toBeGreaterThan(0)

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '生成敏感肌产品开场白' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await waitFor(() => {
      expect(agentApi.messageSend).not.toHaveBeenCalled()
      expect(ssePost).toHaveBeenCalledWith(
        '/agent/chat-stream',
        { agentId: 7, conversationId: 101, content: '生成敏感肌产品开场白' },
        expect.objectContaining({
          onSkillStart: expect.any(Function),
          onSkillEnd: expect.any(Function),
          onStatus: expect.any(Function),
          onChunk: expect.any(Function),
          onDone: expect.any(Function),
          onError: expect.any(Function),
        }),
        { maxReconnectAttempts: 0, idleTimeoutMs: 360000 },
      )
    })
    lastSseCallbacks.onStatus({ status: '思考中...' })
    await waitFor(() => {
      expect(screen.getByTestId('agent-stream-status-bar')).toHaveTextContent('思考中...')
    })
    lastSseCallbacks.onStatus(JSON.stringify({ status: '正在分析需求...', type: 'status' }))
    await waitFor(() => {
      expect(screen.getByTestId('agent-stream-status-bar')).toHaveTextContent('正在分析需求...')
    })
    lastSseCallbacks.onStatus('无需调用工具')
    await waitFor(() => {
      expect(screen.getByTestId('agent-stream-status-bar')).toHaveTextContent('无需调用工具')
    })
    const statusBar = screen.getByTestId('agent-stream-status-bar')
    expect(statusBar).toHaveAttribute('data-streaming', 'true')
    expect(statusBar).toHaveAttribute('data-surface-tone', 'primary')
    expect(statusBar).toHaveAttribute('data-no-local-status-fallback', 'true')
    expect(statusBar).toHaveTextContent('执行状态')
    expect(statusBar).toHaveTextContent('无需调用工具')
    expect(statusBar.getAttribute('style') ?? '').not.toContain('rgba(25, 118, 210, 0.06)')
    expect(await screen.findByText('正在生成')).toBeInTheDocument()
    expect(screen.getByText('执行中...')).toBeInTheDocument()
    lastSseCallbacks.onSkillEnd({ tool: 'kb_rag_search', status: 'done' })
    expect(await screen.findByText('完成')).toBeInTheDocument()
    lastSseCallbacks.onDone({ ok: true })
    await waitFor(() => {
      expect(screen.queryByText('正在生成')).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '点赞第 2 条消息' }))
    await waitFor(() => {
      expect(agentApi.messageRate).toHaveBeenCalledWith(2, 'up')
    })
    fireEvent.click(screen.getByRole('button', { name: '点踩第 2 条消息' }))
    await waitFor(() => {
      expect(agentApi.messageRate).toHaveBeenCalledWith(2, 'down')
    })

    fireEvent.click(screen.getByRole('button', { name: '导出对话.md' }))
    await waitFor(() => {
      expect(agentApi.exportConversation).toHaveBeenCalledWith(101)
      expect(openSpy).toHaveBeenCalledWith('/download/chat.md', '_blank')
    })

    fireEvent.click(screen.getByRole('button', { name: '分享对话' }))
    await waitFor(() => {
      expect(shareApi.create).toHaveBeenCalledWith({
        conversationId: 101,
        title: '敏感肌直播脚本',
      })
    })
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByDisplayValue('http://localhost:3000/admin/ai/agent/share/share-abc')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '复制分享链接' }))
    await waitFor(() => {
      expect(writeText).toHaveBeenCalledWith('http://localhost:3000/admin/ai/agent/share/share-abc')
      expect(toast).toHaveBeenCalledWith('链接已复制到剪贴板', 'success')
    })
    fireEvent.click(screen.getByRole('button', { name: '关闭' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '清空上下文' }))
    await waitFor(() => {
      expect(agentApi.conversationDelete).toHaveBeenCalledWith(101)
    })

    fireEvent.click(screen.getByRole('button', { name: '新建对话' }))
    await waitFor(() => {
      expect(agentApi.conversationCreate).toHaveBeenCalledWith(7, expect.stringMatching(/^对话 /))
    })

    fireEvent.click(screen.getByRole('button', { name: '删' }))
    await waitFor(() => {
      expect(agentApi.conversationDelete).toHaveBeenCalledWith(101)
    })
  }, 30000)

  it('keeps context and renders inline errors for stream, rating, export, share, and delete failures', async () => {
    vi.mocked(agentApi.messageRate).mockRejectedValueOnce(new Error('rate denied') as never)
    vi.mocked(agentApi.exportConversation).mockRejectedValueOnce(new Error('export denied') as never)
    vi.mocked(shareApi.create).mockRejectedValueOnce(new Error('share denied') as never)
    vi.mocked(agentApi.conversationDelete).mockRejectedValueOnce(new Error('delete denied') as never)
    vi.mocked(ssePost).mockImplementation((_url, _body, callbacks: any) => {
      lastSseCallbacks = callbacks
      return { abort: vi.fn(), signal: { aborted: false } } as unknown as AbortController
    })

    renderPage()

    fireEvent.click(await screen.findByText('敏感肌直播脚本'))
    expect(await screen.findByRole('heading', { name: '这是已有回复' })).toBeInTheDocument()
    vi.mocked(agentApi.messageList).mockResolvedValue({
      list: [
        {
          id: 1,
          role: 'user',
          content: '帮我生成开场白',
          createdAt: '2026-05-15 08:01:00',
          tokenUsage: { input: 12, output: 0 },
        },
        {
          id: 2,
          role: 'assistant',
          content: '## 这是已有回复\n\n- 要点一\n- 要点二',
          createdAt: '2026-05-15 08:01:10',
          tokenUsage: { input: 0, output: 30 },
          rating: null,
        },
      ],
    } as never)

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '触发模型失败' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await waitFor(() => {
      expect(ssePost).toHaveBeenCalled()
    })
    await lastSseCallbacks.onError(new Error('模型连接失败'))
    expect(await screen.findByText(/流式对话失败：模型连接失败。来源：\/agent\/chat-stream/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '这是已有回复' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '点赞第 2 条消息' }))
    expect(await screen.findByTestId('agent-chat-action-error')).toHaveTextContent(/评价消息失败：rate denied。来源：\/agent\/message\/rate/)

    fireEvent.click(screen.getByRole('button', { name: '导出对话.md' }))
    expect(await screen.findByText(/导出对话失败：export denied。来源：\/agent\/conversation\/export/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '分享对话' }))
    expect(await screen.findByText(/创建分享失败：share denied。来源：\/agent\/share\/create/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '删' }))
    expect(await screen.findByTestId('agent-chat-action-error')).toHaveTextContent(/删除对话失败：delete denied。来源：\/agent\/conversation\/delete/)
    expect(screen.getByText('敏感肌直播脚本')).toBeInTheDocument()
  }, 30000)

  it('recovers persisted assistant reply when the SSE connection drops before done', async () => {
    vi.mocked(ssePost).mockImplementation((_url, _body, callbacks: any) => {
      lastSseCallbacks = callbacks
      return { abort: vi.fn(), signal: { aborted: false } } as unknown as AbortController
    })

    renderPage()

    fireEvent.click(await screen.findByText('敏感肌直播脚本'))
    expect(await screen.findByRole('heading', { name: '这是已有回复' })).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '复测断流恢复' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await waitFor(() => {
      expect(ssePost).toHaveBeenCalled()
    })

    await lastSseCallbacks.onError(new Error('SSE 连接已中断，未收到完成事件'))

    expect(await screen.findByText('这是流式完成后的新回复。')).toBeInTheDocument()
    expect(screen.queryByText(/流式对话失败/)).not.toBeInTheDocument()
    expect(toast).toHaveBeenCalledWith('模型回复已生成，已从服务端同步回来', 'success')
  })

  it('renders explicit no-local fallback states for conversation and message load failures', async () => {
    vi.mocked(agentApi.conversationList).mockRejectedValueOnce(new Error('conversation down') as never)

    renderPage()

    const conversationError = await screen.findByTestId('agent-chat-conversation-list-error')
    expect(conversationError).toHaveAttribute('data-no-local-conversation-fallback', 'true')
    expect(conversationError).toHaveTextContent(/POST \/agent\/conversation\/list/)
    expect(screen.queryByText('敏感肌直播脚本')).not.toBeInTheDocument()

    vi.clearAllMocks()
    vi.mocked(agentApi.get).mockResolvedValue({
      id: 7,
      agentName: '话术生成助手',
      description: '专业直播话术生成',
      agentType: 1,
      responseMode: 1,
      status: 1,
      createTime: '2026-05-15 09:00:00',
      availableTools: '["kb_rag_search"]',
    } as never)
    vi.mocked(agentApi.conversationList).mockResolvedValue({
      records: [{ id: 101, title: '敏感肌直播脚本', createTime: '2026-05-15 08:00:00' }],
    } as never)
    vi.mocked(agentApi.messageList).mockRejectedValueOnce(new Error('message down') as never)

    renderPage()
    fireEvent.click(await screen.findByText('敏感肌直播脚本'))
    const messageError = await screen.findByTestId('agent-chat-message-list-error')
    expect(messageError).toHaveAttribute('data-no-local-message-fallback', 'true')
    expect(messageError).toHaveTextContent(/POST \/agent\/message\/list/)
    expect(screen.queryByRole('heading', { name: '这是已有回复' })).not.toBeInTheDocument()
  })

  it('uses dark theme surfaces for assistant and streaming message bubbles', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(agentApi.messageList).mockResolvedValue({
      list: [
        {
          id: 1,
          role: 'user',
          content: '帮我生成开场白',
          createdAt: '2026-05-15 08:01:00',
          tokenUsage: { input: 12, output: 0 },
        },
        {
          id: 2,
          role: 'assistant',
          content: '## 这是已有回复\n\n- 要点一\n- 要点二',
          createdAt: '2026-05-15 08:01:10',
          tokenUsage: { input: 0, output: 30 },
          rating: null,
        },
      ],
    } as never)

    renderPageWithAppTheme()

    fireEvent.click(await screen.findByText('敏感肌直播脚本'))
    expect(await screen.findByRole('heading', { name: '这是已有回复' })).toBeInTheDocument()

    expect(screen.getByTestId('agent-assistant-message-bubble')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '生成暗色主题预览' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    expect(await screen.findByText('正在生成')).toBeInTheDocument()
    lastSseCallbacks.onStatus({ status: '暗色状态条审计' })
    expect(screen.getByTestId('agent-streaming-message-bubble')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    const statusBar = await screen.findByTestId('agent-stream-status-bar')
    expect(statusBar).toHaveAttribute('data-streaming', 'true')
    expect(statusBar).toHaveAttribute('data-surface-tone', 'primary')
    expect(statusBar.getAttribute('style') ?? '').not.toContain('rgba(25, 118, 210, 0.06)')
  })
})
