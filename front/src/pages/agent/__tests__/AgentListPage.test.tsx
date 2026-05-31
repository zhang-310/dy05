import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import AgentListPage from '../AgentListPage'
import { agentApi } from '@/api/agent'
import { aiApi } from '@/api/ai'
import { ssePost } from '@/utils/sse-client'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

vi.mock('@/api/agent', () => ({
  agentApi: {
    list: vi.fn(),
    get: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    conversationList: vi.fn(),
    conversationCreate: vi.fn(),
    conversationDelete: vi.fn(),
    conversationRename: vi.fn(),
    messageList: vi.fn(),
    messageRate: vi.fn(),
    exportConversation: vi.fn(),
  },
}))

vi.mock('@/utils/sse-client', () => ({
  ssePost: vi.fn(),
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

let lastSseCallbacks: any
const openSpy = vi.fn()

describe('AgentListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    lastSseCallbacks = undefined
    Element.prototype.scrollIntoView = vi.fn()
    Object.defineProperty(window, 'open', {
      configurable: true,
      value: openSpy,
    })
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
    vi.mocked(agentApi.get).mockResolvedValue({
      id: 1,
      agentName: '话术助手',
      description: '生成直播话术',
      agentType: 1,
      responseMode: 1,
      status: 1,
      createTime: '2026-04-10 10:00:00',
      modelConfig: '{"kbId":1}',
    } as never)
    vi.mocked(agentApi.conversationList).mockResolvedValue({
      records: [
        {
          id: 101,
          title: '直播脚本会话',
          createTime: '2026-05-22 10:00:00',
          updateTime: '2026-05-22 10:30:00',
        },
      ],
    } as never)
    vi.mocked(agentApi.messageList).mockResolvedValue({
      items: [
        {
          id: 11,
          role: 'user',
          content: '生成开场白',
          createdAt: '2026-05-22 10:01:00',
          tokenUsage: { input: 8, output: 0 },
        },
        {
          id: 12,
          role: 'assistant',
          content: '## 已有话术\n\n- 强调痛点\n- 给出利益点',
          createdAt: '2026-05-22 10:02:00',
          tokenUsage: { input: 0, output: 42 },
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
    } as never)
    vi.mocked(agentApi.conversationCreate).mockResolvedValue(102 as never)
    vi.mocked(agentApi.conversationDelete).mockResolvedValue(undefined as never)
    vi.mocked(agentApi.conversationRename).mockResolvedValue(undefined as never)
    vi.mocked(agentApi.messageRate).mockResolvedValue(undefined as never)
    vi.mocked(agentApi.exportConversation).mockResolvedValue({ downloadUrl: '/download/embedded-chat.md' } as never)
    vi.mocked(ssePost).mockImplementation((_url, _body, callbacks: any) => {
      lastSseCallbacks = callbacks
      callbacks.onSkillStart({ tool: 'kb_rag_search', description: '检索知识库' })
      callbacks.onChunk(JSON.stringify({ type: 'chunk', content: '## 生成中\n\n- 第一段' }))
      return { abort: vi.fn(), signal: { aborted: false } } as unknown as AbortController
    })
    vi.mocked(aiApi.kbList).mockResolvedValue([] as never)
  })

  it('renders title and loads agent list', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '智能体管理' })).toBeInTheDocument()
    expect(screen.getByTestId('agent-list-page')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/agent/chat-stream'))
    expect(screen.getByTestId('agent-list-page')).toHaveAttribute('data-no-local-agent-fallback', 'true')

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

  it('renders wrapped agent list responses', async () => {
    vi.mocked(agentApi.list).mockResolvedValueOnce({
      records: [
        {
          id: 2,
          agentName: '包装智能体',
          description: '兼容 records 包装',
          agentType: 3,
          responseMode: 1,
          status: 1,
          createTime: '2026-05-22 10:00:00',
        },
      ],
      totalElements: 1,
      page: 0,
      size: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装智能体')).toBeInTheDocument()
    expect(screen.getByText('商品分析')).toBeInTheDocument()
  }, 30000)

  it('runs embedded chat with markdown, stream status, skill status, rating, export, create, and delete actions', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '对话中心' }))
    fireEvent.click(await screen.findByText(/话术助手/))
    expect(await screen.findByTestId('embedded-agent-chat-panel')).toHaveAttribute('data-no-local-message-fallback', 'true')

    await waitFor(() => {
      expect(agentApi.get).toHaveBeenCalledWith(1)
      expect(agentApi.conversationList).toHaveBeenCalledWith(1)
    })

    fireEvent.click(await screen.findByText('直播脚本会话'))
    await waitFor(() => {
      expect(agentApi.messageList).toHaveBeenCalledWith(101)
    })
    expect(await screen.findByRole('heading', { name: '已有话术' })).toBeInTheDocument()
    expect(screen.getByText('强调痛点')).toBeInTheDocument()
    expect(screen.getAllByText('知识库 RAG 检索').length).toBeGreaterThan(0)

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '继续生成短视频口播' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await waitFor(() => {
      expect(ssePost).toHaveBeenCalledWith(
        '/agent/chat-stream',
        { agentId: 1, conversationId: 101, content: '继续生成短视频口播' },
        expect.objectContaining({
          onStatus: expect.any(Function),
          onSkillStart: expect.any(Function),
          onSkillEnd: expect.any(Function),
          onChunk: expect.any(Function),
          onDone: expect.any(Function),
          onError: expect.any(Function),
        }),
      )
    })

    lastSseCallbacks.onStatus({ status: '思考中...' })
    await waitFor(() => {
      expect(screen.getByTestId('embedded-agent-stream-status-bar')).toHaveTextContent('思考中...')
    })
    lastSseCallbacks.onStatus(JSON.stringify({ status: '正在分析需求...', type: 'status' }))
    lastSseCallbacks.onStatus('无需调用工具')
    await waitFor(() => {
      const statusBar = screen.getByTestId('embedded-agent-stream-status-bar')
      expect(statusBar).toHaveAttribute('data-streaming', 'true')
      expect(statusBar).toHaveAttribute('data-surface-tone', 'primary')
      expect(statusBar).toHaveAttribute('data-no-local-status-fallback', 'true')
      expect(statusBar).toHaveTextContent('正在分析需求...')
      expect(statusBar).toHaveTextContent('无需调用工具')
      expect(statusBar.getAttribute('style') ?? '').not.toContain('rgba(25, 118, 210, 0.06)')
    })
    expect(screen.getByText('技能调用状态')).toBeInTheDocument()
    expect(screen.getAllByText('知识库 RAG 检索').length).toBeGreaterThan(0)
    expect(screen.getByText('检索知识库')).toBeInTheDocument()
    expect(screen.getByText('执行中')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: '生成中' })).toBeInTheDocument()
    lastSseCallbacks.onSkillEnd({ tool: 'kb_rag_search', status: 'success' })
    expect(await screen.findByText('完成')).toBeInTheDocument()
    lastSseCallbacks.onDone({ type: 'done', content: '## 最终回复\n\n- 完成' })
    await waitFor(() => {
      expect(screen.queryByText('生成中...')).not.toBeInTheDocument()
    })
    expect(screen.getByText('技能调用状态')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '点赞内嵌第 2 条消息' }))
    await waitFor(() => {
      expect(agentApi.messageRate).toHaveBeenCalledWith(12, 'up')
    })
    fireEvent.click(screen.getByRole('button', { name: '点踩内嵌第 2 条消息' }))
    await waitFor(() => {
      expect(agentApi.messageRate).toHaveBeenCalledWith(12, 'down')
    })

    fireEvent.click(screen.getByRole('button', { name: '导出对话 .md' }))
    await waitFor(() => {
      expect(agentApi.exportConversation).toHaveBeenCalledWith(101)
      expect(openSpy).toHaveBeenCalledWith('/download/embedded-chat.md', '_blank')
    })

    fireEvent.click(screen.getByRole('button', { name: '新建对话' }))
    await waitFor(() => {
      expect(agentApi.conversationCreate).toHaveBeenCalledWith(1, expect.stringMatching(/^对话 /))
    })

    const conversationRow = screen.getByText('直播脚本会话').closest('.MuiListItemButton-root')
    expect(conversationRow).not.toBeNull()
    fireEvent.click(within(conversationRow as HTMLElement).getByRole('button', { name: '内嵌会话 直播脚本会话 更多操作' }))
    fireEvent.click(await screen.findByRole('menuitem', { name: '删除' }))
    await waitFor(() => {
      expect(agentApi.conversationDelete).toHaveBeenCalledWith(101)
    })

    fireEvent.click(await screen.findByText('直播脚本会话'))
    expect(await screen.findByRole('heading', { name: '已有话术' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '清空上下文' }))
    await waitFor(() => {
      expect(agentApi.conversationDelete).toHaveBeenCalledWith(101)
    })
  }, 30000)

  it('keeps embedded chat context when stream, rating, export, delete, and clear fail', async () => {
    vi.mocked(agentApi.messageRate).mockRejectedValueOnce(new Error('rate denied') as never)
    vi.mocked(agentApi.exportConversation).mockRejectedValueOnce(new Error('export denied') as never)
    vi.mocked(agentApi.conversationDelete)
      .mockRejectedValueOnce(new Error('delete denied') as never)
      .mockRejectedValueOnce(new Error('clear denied') as never)
    vi.mocked(ssePost).mockImplementation((_url, _body, callbacks: any) => {
      lastSseCallbacks = callbacks
      return { abort: vi.fn(), signal: { aborted: false } } as unknown as AbortController
    })

    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '对话中心' }))
    fireEvent.click(await screen.findByText(/话术助手/))
    fireEvent.click(await screen.findByText('直播脚本会话'))
    expect(await screen.findByRole('heading', { name: '已有话术' })).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '触发失败' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    await waitFor(() => {
      expect(ssePost).toHaveBeenCalled()
    })
    lastSseCallbacks.onError(new Error('模型连接失败'))
    expect(await screen.findByText('流式对话失败：模型连接失败')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '已有话术' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '点赞内嵌第 2 条消息' }))
    expect(await screen.findByText(/评分失败：rate denied。来源：\/agent\/message\/rate/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '导出对话 .md' }))
    expect(await screen.findByText(/导出对话失败：export denied。来源：\/agent\/conversation\/export/)).toBeInTheDocument()

    const conversationRow = screen.getByText('直播脚本会话').closest('.MuiListItemButton-root')
    expect(conversationRow).not.toBeNull()
    fireEvent.click(within(conversationRow as HTMLElement).getByRole('button', { name: '内嵌会话 直播脚本会话 更多操作' }))
    fireEvent.click(await screen.findByRole('menuitem', { name: '删除' }))
    expect(await screen.findByText(/删除对话失败：delete denied。来源：\/agent\/conversation\/delete/)).toBeInTheDocument()
    expect(screen.getByText('直播脚本会话')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '清空上下文' }))
    expect(await screen.findByText(/清空上下文失败：clear denied。来源：\/agent\/conversation\/delete/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '已有话术' })).toBeInTheDocument()
    expect(screen.getByText('直播脚本会话')).toBeInTheDocument()
  }, 30000)

  it('shows agent management failure states without local fallback rows', async () => {
    vi.mocked(agentApi.list).mockRejectedValueOnce(new Error('list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/智能体列表加载失败（POST \/agent\/list）：list down/)).toBeInTheDocument()
    expect(screen.getByTestId('agent-list-load-error')).toHaveAttribute('data-no-local-agent-fallback', 'true')
    expect(screen.queryByText('话术助手')).not.toBeInTheDocument()
  }, 30000)

  it('keeps agent drawer input when save fails', async () => {
    vi.mocked(agentApi.save).mockRejectedValueOnce(new Error('save denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    await screen.findByText('话术助手')
    fireEvent.click(screen.getByRole('button', { name: '新建智能体' }))
    fireEvent.change(screen.getByLabelText('名称 *'), {
      target: { value: '失败智能体' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findAllByText(/保存智能体失败（POST \/agent\/save）：save denied/)).toHaveLength(2)
    expect(screen.getByDisplayValue('失败智能体')).toBeInTheDocument()
    expect(agentApi.save).toHaveBeenCalledWith(expect.objectContaining({ agentName: '失败智能体' }))
  }, 30000)

  it('keeps agent row when delete fails', async () => {
    vi.mocked(agentApi.delete).mockRejectedValueOnce(new Error('delete agent denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    await screen.findByText('话术助手')
    const row = screen.getByText('话术助手').closest('.MuiDataGrid-row')
    expect(row).not.toBeNull()
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '删除智能体 话术助手' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    expect(await screen.findByText(/删除智能体失败（POST \/agent\/delete）：delete agent denied/)).toBeInTheDocument()
    expect(screen.getByText('话术助手')).toBeInTheDocument()
  }, 30000)

  it('shows selector load failure on conversation center', async () => {
    vi.mocked(agentApi.list)
      .mockResolvedValueOnce({
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
      .mockRejectedValueOnce(new Error('selector down') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '对话中心' }))
    expect(await screen.findByTestId('agent-selector-list-error')).toHaveTextContent(/智能体选择器加载失败（POST \/agent\/list）：selector down/)
    expect(screen.getByTestId('agent-selector-list-error')).toHaveAttribute('data-no-local-agent-fallback', 'true')
    expect(screen.getByText('请从左侧选择一个智能体开始对话')).toBeInTheDocument()
  }, 30000)

  it('uses dark theme surfaces for tool call args and system prompt preview', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AgentListPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('话术助手')
    fireEvent.click(screen.getByRole('button', { name: '新建智能体' }))
    fireEvent.change(screen.getByPlaceholderText('支持 {变量名} 占位符'), {
      target: { value: '请根据 {productName} 生成直播开场白' },
    })
    expect(screen.getByTestId('agent-system-prompt-preview')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByTestId('agent-system-prompt-preview')).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('tab', { name: '对话中心' }))
    fireEvent.click(await screen.findByText(/话术助手/))
    fireEvent.click(await screen.findByText('直播脚本会话'))
    expect(await screen.findByRole('heading', { name: '已有话术' })).toBeInTheDocument()
    fireEvent.click(screen.getAllByText('知识库 RAG 检索')[0])
    expect(screen.getByTestId('agent-tool-call-args-preview')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  }, 30000)

  it('uses theme-aware embedded stream status bar in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AgentListPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '对话中心' }))
    fireEvent.click(await screen.findByText(/话术助手/))
    fireEvent.click(await screen.findByText('直播脚本会话'))
    expect(await screen.findByRole('heading', { name: '已有话术' })).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('输入消息，Shift+Enter 换行，Enter 发送'), {
      target: { value: '检查内嵌状态条' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送' }))
    expect(await screen.findByRole('heading', { name: '生成中' })).toBeInTheDocument()
    lastSseCallbacks.onStatus({ status: '内嵌暗色状态条审计' })

    const statusBar = await screen.findByTestId('embedded-agent-stream-status-bar')
    expect(statusBar).toHaveAttribute('data-streaming', 'true')
    expect(statusBar).toHaveAttribute('data-surface-tone', 'primary')
    expect(statusBar.getAttribute('style') ?? '').not.toContain('rgba(25, 118, 210, 0.06)')
  }, 30000)
})
