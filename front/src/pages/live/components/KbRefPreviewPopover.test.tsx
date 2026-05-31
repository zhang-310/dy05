import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { KbRefPreviewPopover } from './KbRefPreviewPopover'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbSearch: vi.fn(),
  },
}))

describe('KbRefPreviewPopover', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.kbSearch).mockResolvedValue([
      { title: '品牌话术规范', content: '强调品牌一致性和产品卖点。', score: 0.91 },
    ] as never)
  })

  it('renders normalized knowledge references after opening preview', async () => {
    renderWithProviders(<KbRefPreviewPopover kbId={9} query="精华液卖点" />)

    fireEvent.click(screen.getByRole('button', { name: /预览引用/ }))

    expect(await screen.findByText('品牌话术规范')).toBeInTheDocument()
    expect(screen.getByTestId('kb-ref-preview-popover')).toHaveAttribute('data-contract-scope', 'live-kb-reference-preview')
    expect(screen.getByTestId('kb-ref-preview-popover')).toHaveAttribute('data-no-local-kb-search-fallback', 'true')
    expect(screen.getByTestId('kb-ref-preview-item')).toHaveAttribute('data-contract-source', '/ai/knowledge-base/9/search')
    expect(screen.getByText('91%')).toBeInTheDocument()
    expect(aiApi.kbSearch).toHaveBeenCalledWith(9, {
      query: '精华液卖点',
      topK: 10,
      queryRewrite: false,
    })
  })

  it('distinguishes search failure from empty result', async () => {
    vi.mocked(aiApi.kbSearch).mockRejectedValue(new Error('vector down') as never)

    renderWithProviders(<KbRefPreviewPopover kbId={3} query="精华液卖点" />)

    fireEvent.click(screen.getByRole('button', { name: /预览引用/ }))

    await waitFor(() => {
      expect(screen.getByText(/\/ai\/knowledge-base\/3\/search 检索失败：vector down/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('kb-ref-preview-error')).toHaveAttribute('data-no-local-kb-search-fallback', 'true')
    expect(screen.queryByText('未找到相关文档')).not.toBeInTheDocument()
  })

  it('marks empty knowledge search as real empty response', async () => {
    vi.mocked(aiApi.kbSearch).mockResolvedValue([] as never)

    renderWithProviders(<KbRefPreviewPopover kbId={5} query="空结果" />)

    fireEvent.click(screen.getByRole('button', { name: /预览引用/ }))

    expect(await screen.findByTestId('kb-ref-preview-empty-state')).toHaveAttribute('data-contract-source', '/ai/knowledge-base/5/search')
    expect(screen.getByTestId('kb-ref-preview-empty-state')).toHaveAttribute('data-no-local-kb-search-fallback', 'true')
  })
})
