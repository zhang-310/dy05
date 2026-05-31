import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import KnowledgeSearchPage from '../KnowledgeSearchPage'
import { aiApi } from '@/api/ai'

const toast = vi.fn()

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
    kbSearch: vi.fn(),
    kbFeedback: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn(),
  },
})

describe('KnowledgeSearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(aiApi.kbList).mockResolvedValue([
      { id: 7, kbName: '护肤知识库', description: '', totalDocuments: 3, status: 1, createTime: '' },
    ] as never)
    vi.mocked(aiApi.kbSearch).mockResolvedValue([
      {
        docId: 11,
        title: '面膜 FAQ',
        content: '# 补水\n\n面膜使用后注意锁水。',
        score: 0.91,
        source: 'hybrid',
        chunkId: 110001,
        labels: ['护肤'],
        explain: '向量与全文均命中',
      },
    ] as never)
    vi.mocked(aiApi.kbFeedback).mockResolvedValue(undefined as never)
  })

  it('searches selected knowledge base, renders trace chips, and posts feedback', async () => {
    renderWithProviders(<KnowledgeSearchPage />)

    expect(await screen.findByRole('heading', { name: '知识库检索' })).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-search-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/knowledge-base/list,/ai/knowledge-base/{kbId}/search,/ai/knowledge-base/feedback',
    )
    expect(screen.getByTestId('knowledge-search-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/knowledge-base/search-all,/ai/knowledge-base/mock-search,/ai/knowledge-base/local-cache,/ai/knowledge-base/static-results',
    )
    expect(screen.getByTestId('knowledge-search-page')).toHaveAttribute('data-no-local-search-fallback', 'true')
    expect(screen.getByTestId('knowledge-search-cross-kb-downgrade')).toHaveAttribute(
      'data-degrade-strategy',
      'require-single-kb-selection',
    )
    fireEvent.mouseDown(screen.getByLabelText('选择知识库'))
    fireEvent.click(await screen.findByRole('option', { name: '护肤知识库' }))
    fireEvent.change(screen.getByPlaceholderText('输入搜索关键词...'), {
      target: { value: '面膜补水' },
    })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    await waitFor(() => {
      expect(aiApi.kbSearch).toHaveBeenCalledWith(7, {
        query: '面膜补水',
        topK: 10,
        queryRewrite: false,
      })
    })

    expect(await screen.findByText('面膜 FAQ')).toBeInTheDocument()
    expect(screen.getByText('相关度 91%')).toBeInTheDocument()
    expect(screen.getByText('hybrid')).toBeInTheDocument()
    expect(screen.getByText('文档 #11')).toBeInTheDocument()
    expect(screen.getByText('chunk 110001')).toBeInTheDocument()
    expect(screen.getByText('向量与全文均命中')).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-search-results')).toHaveAttribute('data-no-static-search-fallback', 'true')
    expect(screen.getByTestId('knowledge-search-result-card')).toHaveAttribute('data-feedback-endpoint', '/ai/knowledge-base/feedback')
    expect(screen.getByTestId('knowledge-search-result-markdown')).toHaveAttribute('data-renderer', 'MarkdownViewer')

    const resultCard = screen.getByText('面膜 FAQ').closest('.MuiCard-root') as HTMLElement
    fireEvent.click(within(resultCard).getByRole('button', { name: '有用' }))
    await waitFor(() => {
      expect(aiApi.kbFeedback).toHaveBeenCalledWith({
        docId: 11,
        rating: 1,
        query: '面膜补水',
        searchMode: 'knowledge-search',
      })
      expect(toast).toHaveBeenCalledWith('反馈已提交', 'success')
    })
  })

  it('keeps failed feedback visible with endpoint context', async () => {
    vi.mocked(aiApi.kbFeedback).mockRejectedValue(new Error('feedback offline') as never)

    renderWithProviders(<KnowledgeSearchPage />)

    fireEvent.mouseDown(await screen.findByLabelText('选择知识库'))
    fireEvent.click(await screen.findByRole('option', { name: '护肤知识库' }))
    fireEvent.change(screen.getByPlaceholderText('输入搜索关键词...'), {
      target: { value: '面膜补水' },
    })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    const resultCard = (await screen.findByText('面膜 FAQ')).closest('.MuiCard-root') as HTMLElement
    fireEvent.click(within(resultCard).getByRole('button', { name: '无用' }))

    expect(await screen.findByText(/反馈提交失败（\/ai\/knowledge-base\/feedback）：feedback offline/)).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-search-feedback-error')).toHaveAttribute('data-no-optimistic-feedback-fallback', 'true')
    expect(screen.getByText('面膜 FAQ')).toBeInTheDocument()
  })

  it('shows docker ollama diagnostics when search fails', async () => {
    vi.mocked(aiApi.kbSearch).mockRejectedValue(new Error('ConnectException') as never)

    renderWithProviders(<KnowledgeSearchPage />)

    fireEvent.mouseDown(await screen.findByLabelText('选择知识库'))
    fireEvent.click(await screen.findByRole('option', { name: '护肤知识库' }))
    fireEvent.change(screen.getByPlaceholderText('输入搜索关键词...'), {
      target: { value: '向量检索' },
    })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    expect(await screen.findByText(/检索失败（\/ai\/knowledge-base\/7\/search）：ConnectException/)).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-search-error')).toHaveAttribute('data-no-local-search-fallback', 'true')
    expect(screen.getByText(/OLLAMA_URL=http:\/\/host.docker.internal:11434/)).toBeInTheDocument()
  })

  it('does not trigger unsupported cross-kb search from hot terms before a kb is selected', async () => {
    renderWithProviders(<KnowledgeSearchPage />)

    expect(await screen.findByRole('heading', { name: '知识库检索' })).toBeInTheDocument()
    fireEvent.click(screen.getByText('直播话术'))

    await waitFor(() => {
      expect(aiApi.kbSearch).not.toHaveBeenCalled()
    })
    expect(screen.getByTestId('knowledge-search-cross-kb-downgrade')).toHaveAttribute(
      'data-unsupported-endpoint',
      '/ai/knowledge-base/search-all',
    )
  })

  it('uses theme tokens for search action, hot icon, and score chip in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderWithProviders(
      <AppThemeProvider>
        <KnowledgeSearchPage />
      </AppThemeProvider>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('选择知识库'))
    fireEvent.click(await screen.findByRole('option', { name: '护肤知识库' }))
    fireEvent.change(screen.getByPlaceholderText('输入搜索关键词...'), {
      target: { value: '面膜补水' },
    })

    const searchButton = screen.getByTestId('knowledge-search-primary-action-surface')
    const hotIcon = screen.getByTestId('knowledge-search-hot-icon-surface')
    expect(window.getComputedStyle(searchButton).color).not.toBe('rgb(0, 0, 0)')
    expect(window.getComputedStyle(hotIcon).color).not.toBe('rgb(245, 124, 0)')

    fireEvent.click(searchButton)

    expect(await screen.findByText('面膜 FAQ')).toBeInTheDocument()
    const scoreChip = screen.getByTestId('knowledge-search-score-chip-surface')
    expect(window.getComputedStyle(scoreChip).backgroundColor).not.toBe('rgb(76, 175, 80)')
    expect(window.getComputedStyle(scoreChip).color).not.toBe('rgb(255, 255, 255)')
  })
})
