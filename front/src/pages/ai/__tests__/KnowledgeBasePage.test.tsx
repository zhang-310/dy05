import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import KnowledgeBasePage from '../KnowledgeBasePage'
import { aiApi } from '@/api/ai'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
    kbCreate: vi.fn(),
    kbDelete: vi.fn(),
    docList: vi.fn(),
    indexQueueList: vi.fn(),
    evolutionFitnessList: vi.fn(),
    docDelete: vi.fn(),
    kbSearch: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('KnowledgeBasePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(aiApi.kbList).mockResolvedValue([
      {
        id: 1,
        kbName: '护肤知识库',
        description: '护肤话术与产品知识',
        totalDocuments: 12,
        status: 1,
        createTime: '2026-04-10 10:00:00',
      },
    ] as never)
    vi.mocked(aiApi.kbCreate).mockResolvedValue({ id: 2, kbName: '新知识库' } as never)
    vi.mocked(aiApi.kbDelete).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.docList).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 } as never)
    vi.mocked(aiApi.indexQueueList).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 50 } as never)
    vi.mocked(aiApi.evolutionFitnessList).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 50 } as never)
    vi.mocked(aiApi.docDelete).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.kbSearch).mockResolvedValue([] as never)
  })

  it('loads knowledge bases and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '知识库' })).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-base-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/knowledge-base/list,/ai/knowledge-base/create,DELETE /ai/knowledge-base/{id},/ai/knowledge-base/{kbId}/documents,/ai/knowledge-base/{kbId}/search,/ai/knowledge-base/{kbId}/index-queue/list,/ai/knowledge-base/{kbId}/evolution-fitness/list,DELETE /ai/knowledge-base/document/{docId}',
    )
    expect(screen.getByTestId('knowledge-base-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/knowledge-base/mock,/ai/knowledge-base/local-list,/ai/knowledge-base/static-documents,/ai/knowledge-base/static-rag,/ai/knowledge-base/local-index-queue,/ai/knowledge-base/local-fitness',
    )
    expect(screen.getByTestId('knowledge-base-page')).toHaveAttribute('data-no-local-kb-fallback', 'true')
    expect(screen.getByTestId('kb-index-pipeline-notice')).toHaveAttribute('data-docker-ollama-diagnostic', 'true')

    await waitFor(() => {
      expect(aiApi.kbList).toHaveBeenCalledWith({ page: 0, rows: 20, name: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('护肤知识库')).toBeInTheDocument()
      expect(screen.getAllByText('12').length).toBeGreaterThan(0)
    })
    expect(screen.getByTestId('kb-list-grid')).toHaveAttribute('data-source-endpoint', '/ai/knowledge-base/list')
  })

  it('keeps create dialog input and shows endpoint when create fails', async () => {
    vi.mocked(aiApi.kbCreate).mockRejectedValue(new Error('owner invalid') as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新建知识库' }))
    fireEvent.change(screen.getByRole('textbox', { name: '名称' }), { target: { value: '向量知识库' } })
    fireEvent.change(screen.getByRole('textbox', { name: '描述' }), { target: { value: 'Docker Ollama 检索' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/知识库保存失败（\/ai\/knowledge-base\/create）：owner invalid/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('kb-create-error')).toHaveAttribute('data-preserves-form-input', 'true')
    expect(screen.getByDisplayValue('向量知识库')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Docker Ollama 检索')).toBeInTheDocument()
  })

  it('keeps knowledge base row and confirm context when delete fails', async () => {
    vi.mocked(aiApi.kbDelete).mockRejectedValue(new Error('forbidden') as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤知识库')
    const deleteButtons = await screen.findAllByRole('button', { name: '删除' })
    fireEvent.click(deleteButtons[0])
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/知识库删除失败（DELETE \/ai\/knowledge-base\/1）：forbidden/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('kb-operation-error')).toHaveAttribute('data-no-local-mutation-fallback', 'true')
    expect(screen.getByText('护肤知识库')).toBeInTheDocument()
  })

  it('shows drawer document list endpoint failures', async () => {
    vi.mocked(aiApi.docList).mockRejectedValue(new Error('docs offline') as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤知识库')
    const row = screen.getByText('护肤知识库').closest('[role="row"]') as HTMLElement
    fireEvent.click(within(row).getByRole('button', { name: '文档' }))

    expect(await screen.findByText(/文档列表加载失败（\/ai\/knowledge-base\/1\/documents）：docs offline/)).toBeInTheDocument()
    expect(screen.getByTestId('kb-drawer-documents-error')).toHaveAttribute('data-no-local-document-fallback', 'true')
    expect(screen.getByTestId('kb-detail-drawer')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/knowledge-base/{kbId}/documents,/ai/knowledge-base/{kbId}/search,/ai/knowledge-base/{kbId}/index-queue/list,/ai/knowledge-base/{kbId}/evolution-fitness/list',
    )
  })

  it('renders RAG results through real kb search and MarkdownViewer', async () => {
    vi.mocked(aiApi.kbSearch).mockResolvedValue([
      {
        docId: 11,
        title: '面膜 FAQ',
        content: '# 面膜\n\n- 使用后补水',
        score: 0.91,
        source: 'hybrid',
        chunkId: 110001,
        labels: ['护肤'],
        explain: '向量与全文均命中',
      },
    ] as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤知识库')
    const row = screen.getByText('护肤知识库').closest('[role="row"]') as HTMLElement
    fireEvent.click(within(row).getByRole('button', { name: '检索' }))
    fireEvent.change(screen.getByPlaceholderText('输入检索问题…'), { target: { value: '面膜补水' } })
    fireEvent.click(screen.getByTestId('kb-rag-search-action'))

    await waitFor(() => {
      expect(aiApi.kbSearch).toHaveBeenCalledWith(1, { query: '面膜补水', topK: 5, queryRewrite: false })
    })
    expect(await screen.findByText('面膜 FAQ')).toBeInTheDocument()
    expect(screen.getByTestId('kb-rag-result-card')).toHaveAttribute('data-source-endpoint', '/ai/knowledge-base/1/search')
    expect(screen.getByTestId('kb-rag-result-markdown')).toHaveAttribute('data-renderer', 'MarkdownViewer')
  })

  it('shows RAG and index queue failures without static fallbacks', async () => {
    vi.mocked(aiApi.kbSearch).mockRejectedValue(new Error('ConnectException') as never)
    vi.mocked(aiApi.indexQueueList).mockResolvedValue({
      total: 1,
      list: [{
        id: 51,
        status: 'failed',
        contentPreview: '上传文档失败',
        errorMsg: '生成嵌入向量失败: ConnectException',
        retryCount: 3,
        createTime: '2026-05-22 10:00:00',
        sourceType: 'file',
      }],
      pageNum: 0,
      pageSize: 50,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeBasePage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤知识库')
    const row = screen.getByText('护肤知识库').closest('[role="row"]') as HTMLElement
    fireEvent.click(within(row).getByRole('button', { name: '检索' }))
    fireEvent.change(screen.getByPlaceholderText('输入检索问题…'), { target: { value: '向量检索' } })
    fireEvent.click(screen.getByTestId('kb-rag-search-action'))

    expect(await screen.findByText(/RAG 检索失败：ConnectException/)).toBeInTheDocument()
    expect(screen.getByTestId('kb-rag-error')).toHaveAttribute('data-docker-ollama-diagnostic', 'true')

    fireEvent.click(await screen.findByRole('tab', { name: '索引队列' }))
    expect(await screen.findByText(/生成嵌入向量失败: ConnectException/)).toBeInTheDocument()
    expect(screen.getByTestId('kb-index-queue-row')).toHaveAttribute('data-index-status', 'failed')
    expect(screen.getByTestId('kb-index-queue-error-hint')).toHaveAttribute('data-docker-ollama-diagnostic', 'true')
  })

  it('uses a dark theme surface for evolution fitness payload JSON', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(aiApi.evolutionFitnessList).mockResolvedValueOnce({
      total: 1,
      list: [{
        id: 31,
        kbId: 1,
        taskId: 'evolve-task-1',
        metricName: 'quality_delta',
        metricValue: 0.18,
        payloadJson: '{"before":30.8,"after":72.4}',
        experimentId: 'exp-1',
        createTime: '2026-05-22 10:00:00',
      }],
      pageNum: 0,
      pageSize: 50,
    } as never)

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <KnowledgeBasePage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('护肤知识库')
    const row = screen.getByText('护肤知识库').closest('[role="row"]') as HTMLElement
    fireEvent.click(within(row).getByRole('button', { name: '文档' }))
    fireEvent.click(await screen.findByRole('tab', { name: '适应度' }))

    expect(await screen.findByText('quality_delta')).toBeInTheDocument()
    expect(screen.getByTestId('kb-evolution-fitness-panel')).toHaveAttribute('data-source-endpoint', '/ai/knowledge-base/1/evolution-fitness/list')
    expect(screen.getByTestId('kb-fitness-payload-preview')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
      color: 'rgb(255, 255, 255)',
    })
  })

  it('uses a theme-aware surface for expired documents in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(aiApi.docList).mockResolvedValueOnce({
      total: 1,
      list: [{
        id: 41,
        kbId: 1,
        title: '旧版护肤话术',
        content: '旧版话术内容',
        fileType: 'text',
        status: 1,
        chunkCount: 3,
        tokenCount: 256,
        qualityHeuristicScore: 55,
        expiryStatus: 2,
        createTime: '2026-04-10 10:00:00',
      }],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <KnowledgeBasePage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('护肤知识库')
    const row = screen.getByText('护肤知识库').closest('[role="row"]') as HTMLElement
    fireEvent.click(within(row).getByRole('button', { name: '文档' }))

    expect(await screen.findByText('旧版护肤话术')).toBeInTheDocument()
    expect(screen.getByTestId('kb-drawer-documents-panel')).toHaveAttribute('data-pagination-mode', 'server')
    expect(screen.getByTestId('kb-expired-document-row-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(253, 237, 237)',
    })
  })

  it('uses theme tokens for knowledge base search and create actions in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <KnowledgeBasePage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByText('护肤知识库')).toBeInTheDocument()
    const searchButton = screen.getByTestId('kb-page-search-action-surface')
    const createButton = screen.getByTestId('kb-page-create-action-surface')

    expect(window.getComputedStyle(searchButton).color).not.toBe('rgb(0, 0, 0)')
    expect(window.getComputedStyle(createButton).color).not.toBe('rgb(0, 0, 0)')
  })
})
