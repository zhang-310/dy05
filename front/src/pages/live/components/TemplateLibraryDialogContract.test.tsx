import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { TemplateLibraryDialog } from './TemplateLibraryDialog'
import { searchTemplates, applyTemplate } from '@/api/live-script'

vi.mock('@/api/live-script', () => ({
  searchTemplates: vi.fn(),
  applyTemplate: vi.fn(),
}))

const templates = [
  {
    id: 1,
    templateName: '屏障修护开场',
    scriptType: 'opening',
    category: '护肤',
    content: '换季敏感先讲痛点，再引出屏障修护。',
    effectivenessScore: 91,
    usageCount: 12,
    industryTags: null,
    autoCollected: 1,
    createTime: '',
  },
  {
    id: 2,
    templateName: '产品卖点模板',
    scriptType: 'product',
    category: null,
    content: '成分、肤感、权益依次展开。',
    effectivenessScore: 0,
    usageCount: 3,
    industryTags: null,
    autoCollected: 0,
    createTime: '',
  },
]

describe('TemplateLibraryDialog contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(searchTemplates).mockResolvedValue({ list: templates, total: 42, page: 0, rows: 20 } as never)
    vi.mocked(applyTemplate).mockResolvedValue({ content: '应用后的模板内容', scriptType: 'opening', templateName: '屏障修护开场' } as never)
  })

  it('marks template library API contract and renders real template rows', async () => {
    renderWithProviders(<TemplateLibraryDialog open onClose={vi.fn()} onApply={vi.fn()} />)

    const dialog = await screen.findByTestId('template-library-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-template-library-dialog')
    expect(dialog).toHaveAttribute('data-ready-endpoints', '/live/template/search|/live/template/apply')
    expect(dialog).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(await screen.findAllByTestId('template-library-item')).toHaveLength(2)
    expect(screen.getByTestId('template-library-pagination')).toHaveAttribute('data-total', '42')
    expect(screen.getByTestId('template-library-auto-chip')).toHaveAttribute('data-contract-source', '/live/template/search')
    expect(searchTemplates).toHaveBeenCalledWith(expect.objectContaining({ page: 0, rows: 20 }))
  })

  it('surfaces search endpoint errors without local templates', async () => {
    vi.mocked(searchTemplates).mockRejectedValue(new Error('template search down') as never)

    renderWithProviders(<TemplateLibraryDialog open onClose={vi.fn()} onApply={vi.fn()} />)

    const error = await screen.findByTestId('template-library-load-error')
    expect(error).toHaveTextContent('/live/template/search 模板加载失败：template search down')
    expect(error).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(screen.getByTestId('template-library-empty-state')).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(screen.queryByTestId('template-library-item')).not.toBeInTheDocument()
  })

  it('keeps true empty result distinct from endpoint failure', async () => {
    vi.mocked(searchTemplates).mockResolvedValue({ list: [], total: 0, page: 0, rows: 20 } as never)

    renderWithProviders(<TemplateLibraryDialog open onClose={vi.fn()} onApply={vi.fn()} />)

    expect(await screen.findByTestId('template-library-empty-state')).toHaveAttribute('data-contract-source', '/live/template/search')
    expect(screen.queryByTestId('template-library-load-error')).not.toBeInTheDocument()
  })

  it('applies template through endpoint and delegates content to parent callback', async () => {
    const onApply = vi.fn()
    const onClose = vi.fn()

    renderWithProviders(<TemplateLibraryDialog open onClose={onClose} onApply={onApply} />)

    const applyButton = (await screen.findAllByTestId('template-library-apply-button'))[0]
    expect(applyButton).toHaveAttribute('data-contract-source', '/live/template/apply')
    fireEvent.click(applyButton)

    await waitFor(() => {
      expect(applyTemplate).toHaveBeenCalledWith(1)
      expect(onApply).toHaveBeenCalledWith('应用后的模板内容', 'opening')
      expect(onClose).toHaveBeenCalled()
    })
  })

  it('surfaces apply endpoint errors and keeps dialog open', async () => {
    const onApply = vi.fn()
    const onClose = vi.fn()
    vi.mocked(applyTemplate).mockRejectedValue(new Error('apply down') as never)

    renderWithProviders(<TemplateLibraryDialog open onClose={onClose} onApply={onApply} />)

    fireEvent.click((await screen.findAllByTestId('template-library-apply-button'))[0])

    const error = await screen.findByTestId('template-library-apply-error')
    expect(error).toHaveTextContent('/live/template/apply 模板应用失败：apply down')
    expect(error).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(onApply).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })
})
