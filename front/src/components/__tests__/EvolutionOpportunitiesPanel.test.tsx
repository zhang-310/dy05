import { describe, expect, it, vi, beforeEach } from 'vitest'
import userEvent from '@testing-library/user-event'
import { renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { EvolutionOpportunitiesPanel } from '../EvolutionOpportunitiesPanel'
import type { EvolutionOpportunityVO } from '@/types/evolution'

const toast = vi.hoisted(() => vi.fn())

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function createOpportunity(overrides: Partial<EvolutionOpportunityVO> = {}): EvolutionOpportunityVO {
  return {
    id: 311,
    kbId: 8,
    type: 'high_effectiveness',
    typeLabel: '高效话术入库',
    title: '成交开场话术',
    description: '近期高转化话术可沉淀到知识库。',
    scriptId: 18,
    scriptContent: '开场先用场景痛点留人，再给出产品卖点和限时权益。',
    relatedScripts: ['保湿开场', '限时权益'],
    confidenceScore: 92,
    priority: 'high',
    actionRequired: '加入高效话术库',
    estimatedImpact: '提升直播间转化率',
    createdAt: '2026-05-22T10:00:00',
    updatedAt: '2026-05-22T10:00:00',
    status: 'pending',
    ...overrides,
  }
}

describe('EvolutionOpportunitiesPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders grouped opportunities with theme tone markers', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <EvolutionOpportunitiesPanel
        opportunities={[
          createOpportunity(),
          createOpportunity({
            id: 312,
            type: 'quality_gap',
            typeLabel: '质量缺口',
            priority: 'medium',
            scriptContent: undefined,
            confidenceScore: 78,
          }),
        ]}
      />,
    )

    expect(screen.getByTestId('evolution-opportunities-panel')).toHaveAttribute('data-evolution-tone', 'surface')
    expect(screen.getAllByTestId('evolution-opportunity-summary-card')).toHaveLength(2)

    const scriptPreview = screen.getByTestId('evolution-opportunity-script-preview')
    expect(scriptPreview).toHaveAttribute('data-evolution-tone', 'script-preview')
    expect(scriptPreview.outerHTML).not.toContain('#f5f5f5')
    expect(scriptPreview.outerHTML).not.toContain('#eee')
    expect(scriptPreview.outerHTML).not.toContain('#fafafa')

    await user.click(screen.getAllByRole('button', { name: '展开机会详情' })[0])
    expect(screen.getAllByTestId('evolution-opportunity-details')[0]).toHaveAttribute('data-evolution-tone', 'details')
    expect(screen.getByText('保湿开场')).toBeInTheDocument()
  })

  it('passes apply comments and reject reasons to callbacks', async () => {
    const user = userEvent.setup()
    const onApply = vi.fn()
    const onReject = vi.fn()
    renderWithProviders(
      <EvolutionOpportunitiesPanel
        opportunities={[createOpportunity()]}
        onApply={onApply}
        onReject={onReject}
      />,
    )

    await user.click(screen.getByRole('button', { name: '展开机会详情' }))
    await user.click(screen.getByRole('button', { name: '应用' }))
    await user.type(screen.getByLabelText('备注（可选）'), '复盘通过，进入素材池')
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '应用' }))

    expect(onApply).toHaveBeenCalledWith(311, '复盘通过，进入素材池')
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '拒绝' }))
    await user.type(screen.getByLabelText('拒绝理由'), '样本量不足')
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '确认拒绝' }))

    expect(onReject).toHaveBeenCalledWith(311, '样本量不足')
  })

  it('shows explicit degraded toasts when apply or reject callbacks are not configured', async () => {
    const user = userEvent.setup()
    renderWithProviders(<EvolutionOpportunitiesPanel opportunities={[createOpportunity()]} />)

    await user.click(screen.getByRole('button', { name: '展开机会详情' }))
    await user.click(screen.getByRole('button', { name: '应用' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '应用' }))

    expect(toast).toHaveBeenCalledWith(
      '未配置 onApply：机会应用请接入 POST /ai/knowledge-evolution/auto-optimize 等业务接口',
      'info',
    )
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: '拒绝' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '确认拒绝' }))

    expect(toast).toHaveBeenCalledWith(
      '未配置 onReject：旧版 /ai/evolution/opportunities 路径后端未实现',
      'warning',
    )
  })

  it('keeps empty and loading states observable without fake opportunity data', () => {
    const onRefresh = vi.fn()
    renderWithProviders(
      <EvolutionOpportunitiesPanel opportunities={[]} loading={true} onRefresh={onRefresh} />,
    )

    expect(screen.getByTestId('evolution-opportunities-empty')).toHaveAttribute('data-evolution-tone', 'empty')
    expect(screen.getByText('暂无进化机会')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '刷新' })).toBeDisabled()
  })
})
