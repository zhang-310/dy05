import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ContentLibraryPage from '../ContentLibraryPage'
import { scriptApi } from '@/api/script'
import { copyApi } from '@/api/copy'
import { slangApi } from '@/api/slangdict'

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/api/script', () => ({
  scriptApi: {
    list: vi.fn(),
    templateSearch: vi.fn(),
    violationList: vi.fn(),
    complianceRules: vi.fn(),
  },
}))

vi.mock('@/api/copy', () => ({
  copyApi: {
    list: vi.fn(),
  },
}))

vi.mock('@/api/slangdict', () => ({
  slangApi: {
    list: vi.fn(),
  },
}))

describe('ContentLibraryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.list).mockResolvedValue({ total: 12, list: [], pageNum: 0, pageSize: 1 } as never)
    vi.mocked(scriptApi.templateSearch).mockResolvedValue({ total: 3, list: [], pageNum: 0, pageSize: 1 } as never)
    vi.mocked(copyApi.list).mockResolvedValue({ total: 5, list: [], pageNum: 0, pageSize: 1 } as never)
    vi.mocked(scriptApi.violationList).mockResolvedValue({ total: 7, list: [], pageNum: 0, pageSize: 1 } as never)
    vi.mocked(scriptApi.complianceRules).mockResolvedValue([{ id: 1 }, { id: 2 }] as never)
    vi.mocked(slangApi.list).mockResolvedValue({ total: 4, list: [], pageNum: 0, pageSize: 1 } as never)
  })

  it('renders real API diagnostics and routes to workbenches', async () => {
    navigate.mockClear()

    renderWithProviders(
      <MemoryRouter>
        <ContentLibraryPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '内容库' })).toBeInTheDocument()
    const workbench = screen.getByTestId('content-library-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'content-library')
    expect(workbench).toHaveAttribute('data-diagnostic-endpoints', '/script/list,/script/template/search,/copy/library/search,/script/admin/violation/list,/script/compliance/rules,/slangdict/entry/search')
    expect(workbench).toHaveAttribute('data-unsupported-actions', 'inline-crud,nested-workbench,cross-domain-write')
    expect(workbench).toHaveAttribute('data-no-inline-write', 'true')
    expect(workbench).toHaveAttribute('data-no-local-asset-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-cross-domain-write', 'true')
    expect(screen.getByTestId('content-library-shell-contract')).toHaveAttribute('data-contract-status', 'gateway-only')
    expect(screen.getByTestId('content-library-shell-contract')).toHaveAttribute('data-no-inline-write', 'true')
    expect(screen.getByText(/本页不再嵌套完整子页面/)).toBeInTheDocument()
    expect(screen.getAllByText('真实接口').length).toBeGreaterThan(0)
    expect(screen.queryByText('明确降级')).not.toBeInTheDocument()
    expect(screen.getByText('/script/template/search')).toBeInTheDocument()
    expect(screen.getByText('/slangdict/entry/search')).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getAllByText(/接口可用/).length).toBeGreaterThanOrEqual(6)
      expect(screen.getByText('33')).toBeInTheDocument()
    })
    expect(workbench).toHaveAttribute('data-healthy-count', '6')
    expect(workbench).toHaveAttribute('data-error-count', '0')
    expect(workbench).toHaveAttribute('data-total-assets', '33')
    const summaryCards = screen.getAllByTestId('content-library-summary-card')
    expect(summaryCards).toHaveLength(4)
    expect(summaryCards.find(card => card.getAttribute('data-summary-metric') === 'asset-total')).toHaveAttribute('data-contract-status', 'local-derived')
    const cards = screen.getAllByTestId('content-library-capability-card')
    expect(cards).toHaveLength(6)
    expect(cards.find(card => card.getAttribute('data-capability-title') === '话术库')).toHaveAttribute('data-diagnostic-endpoint', '/script/list')
    expect(cards.find(card => card.getAttribute('data-capability-title') === '话术库')).toHaveAttribute('data-inline-crud', 'unsupported')
    expect(cards.find(card => card.getAttribute('data-capability-title') === '话术库')).toHaveAttribute('data-no-inline-write', 'true')
    expect(cards.find(card => card.getAttribute('data-capability-title') === '文案管理')).toHaveAttribute('data-no-local-asset-fallback', 'true')
    expect(cards.find(card => card.getAttribute('data-capability-title') === '梗库')).toHaveAttribute('data-route', '/admin/slangdict')
    expect(screen.getAllByTestId('content-library-diagnostic-success')).toHaveLength(6)
    expect(screen.getAllByTestId('content-library-endpoint-chip').length).toBeGreaterThanOrEqual(12)
    expect(scriptApi.list).toHaveBeenCalledWith({ page: 0, rows: 1 })
    expect(scriptApi.templateSearch).toHaveBeenCalledWith({ page: 0, rows: 1 })
    expect(copyApi.list).toHaveBeenCalledWith({ page: 0, rows: 1 })
    expect(scriptApi.violationList).toHaveBeenCalledWith({ page: 0, rows: 1 })
    expect(scriptApi.complianceRules).toHaveBeenCalled()
    expect(slangApi.list).toHaveBeenCalledWith({ page: 0, rows: 1 })

    fireEvent.click(screen.getAllByRole('button', { name: '进入工作台' })[0])
    expect(screen.getAllByTestId('content-library-route-button')[0]).toHaveAttribute('data-contract-action', 'route-to-workbench')
    expect(navigate).toHaveBeenCalledWith('/admin/script/list')
  })

  it('shows diagnostic failures and allows manual refresh', async () => {
    vi.mocked(scriptApi.templateSearch).mockRejectedValueOnce(new Error('template service down'))

    renderWithProviders(
      <MemoryRouter>
        <ContentLibraryPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/有 1 个内容接口诊断失败/)).toBeInTheDocument()
    expect(screen.getByTestId('content-library-workbench')).toHaveAttribute('data-error-count', '1')
    expect(screen.getByTestId('content-library-diagnostic-error-summary')).toHaveAttribute('data-no-fake-health', 'true')
    expect(screen.getByTestId('content-library-diagnostic-error')).toHaveAttribute('data-diagnostic-endpoint', '/script/template/search')
    expect(screen.getByTestId('content-library-diagnostic-error')).toHaveAttribute('data-no-fake-total', 'true')
    expect(screen.getAllByTestId('content-library-capability-card').find(card => card.getAttribute('data-capability-title') === '话术模板')).toHaveAttribute('data-contract-status', 'source-error')
    expect(screen.getByText(/\/script\/template\/search 诊断失败：template service down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '刷新诊断' }))
    await waitFor(() => {
      expect(scriptApi.templateSearch).toHaveBeenCalledTimes(2)
    })
  })
})
