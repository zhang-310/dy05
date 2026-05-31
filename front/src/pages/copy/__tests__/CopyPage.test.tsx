import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import CopyPage from '../CopyPage'

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

describe('CopyPage', () => {
  it('renders copy capability map and routes to independent workbenches', () => {
    navigate.mockClear()

    renderWithProviders(
      <MemoryRouter>
        <CopyPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '文案管理' })).toBeInTheDocument()
    expect(screen.getByText(/本页不再直接嵌套文案库/)).toBeInTheDocument()
    expect(screen.getByText('文案审批')).toBeInTheDocument()
    expect(screen.getByText('AI 生成文案')).toBeInTheDocument()
    expect(screen.getByText('/copy/approval/save')).toBeInTheDocument()
    expect(screen.getByText('AI 候选生成')).toBeInTheDocument()
    expect(screen.queryByText('后端接口未落库')).not.toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: '进入工作台' })[0])
    expect(navigate).toHaveBeenCalledWith('/admin/copy/library')
  })

  it('exposes gateway contract and degraded copy actions without inline operations', () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyPage />
      </MemoryRouter>,
    )

    const root = screen.getByTestId('copy-gateway-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'copy-gateway')
    expect(root).toHaveAttribute(
      'data-ready-endpoints',
      '/copy/library/search,/copy/library/save,/copy/library/delete,/copy/approval/search,/copy/approval/get,/copy/approval/save,/copy/template/search,/copy/template/save,/copy/template/delete,/copy/ai/generate',
    )
    expect(root).toHaveAttribute(
      'data-unsupported-actions',
      'csv-export,batch-tag,usage-detail,approval-stats,approval-revise,batch-template-delete,batch-ai-save',
    )
    expect(root).toHaveAttribute(
      'data-unsupported-endpoints',
      '/copy/library/export,/copy/library/batch-tag,/copy/library/usage,/copy/approval/stats,/copy/approval/revise,/copy/template/batch-delete,/copy/ai/batch-save',
    )
    expect(root).toHaveAttribute('data-capability-count', '4')
    expect(root).toHaveAttribute('data-endpoint-count', '10')
    expect(root).toHaveAttribute('data-degraded-count', '7')
    expect(root).toHaveAttribute('data-no-inline-write', 'true')
    expect(root).toHaveAttribute('data-no-page-api-request', 'true')

    expect(screen.getByTestId('copy-gateway-shell-contract')).toHaveAttribute('data-contract-status', 'gateway-only')
    expect(screen.getByTestId('copy-gateway-shell-contract')).toHaveAttribute('data-no-page-api-request', 'true')
    expect(screen.getAllByTestId('copy-gateway-summary-card')).toHaveLength(4)
    expect(screen.getAllByTestId('copy-gateway-summary-card')[0]).toHaveAttribute('data-summary-metric', 'capability-count')
    expect(screen.getAllByTestId('copy-gateway-summary-card')[0]).toHaveAttribute('data-contract-status', 'local-derived')

    const cards = screen.getAllByTestId('copy-gateway-capability-card')
    expect(cards).toHaveLength(4)
    expect(cards[0]).toHaveAttribute('data-capability-title', '文案库')
    expect(cards[0]).toHaveAttribute('data-route', '/admin/copy/library')
    expect(cards[0]).toHaveAttribute('data-inline-operation', 'unsupported')
    expect(cards[0]).toHaveAttribute('data-contract-status', 'ready-with-degraded-actions')
    expect(cards[0]).toHaveAttribute('data-no-inline-write', 'true')
    expect(cards[0]).toHaveAttribute('data-capability-endpoints', '/copy/library/search,/copy/library/save,/copy/library/delete')
    expect(cards[0]).toHaveAttribute('data-available-actions', '列表分页,新增编辑,删除,提交审批')
    expect(cards[0]).toHaveAttribute('data-degraded-actions', 'CSV 导出,批量打标签,使用明细')

    expect(screen.getAllByTestId('copy-gateway-endpoint-chip')).toHaveLength(10)
    expect(screen.getAllByTestId('copy-gateway-available-chip')).toHaveLength(15)
    const degradedChips = screen.getAllByTestId('copy-gateway-degraded-chip')
    expect(degradedChips).toHaveLength(7)
    expect(degradedChips.some(chip => chip.getAttribute('data-action') === 'CSV 导出')).toBe(true)
    expect(degradedChips.every(chip => chip.getAttribute('data-contract-status') === 'degraded')).toBe(true)

    const routeButtons = screen.getAllByTestId('copy-gateway-route-button')
    expect(routeButtons).toHaveLength(4)
    expect(routeButtons[0]).toHaveAttribute('data-contract-action', 'route-to-workbench')
    expect(routeButtons[0]).toHaveAttribute('data-route', '/admin/copy/library')
    expect(screen.queryByRole('button', { name: /新建|保存|删除|导出|批量/ })).not.toBeInTheDocument()
  })
})
