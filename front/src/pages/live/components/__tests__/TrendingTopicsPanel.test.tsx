import { describe, it, expect, vi } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { TrendingTopicsPanel } from '../TrendingTopicsPanel'
import { brainTrendsCurrent } from '@/api/brain'

vi.mock('@/api/brain', () => ({
  brainTrendsCurrent: vi.fn(),
}))

describe('TrendingTopicsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(brainTrendsCurrent).mockResolvedValue([])
  })

  it('renders without crashing', () => {
    const onInject = vi.fn()
    const { container } = renderWithProviders(<TrendingTopicsPanel onInjectKeyword={onInject} />)
    expect(container).toBeInTheDocument()
  })

  it('loads trends with readonly contract and injects through prop callback', async () => {
    const onInject = vi.fn()
    vi.mocked(brainTrendsCurrent).mockResolvedValue([
      { id: 't1', title: '换季敏感肌', category: '护肤', heatScore: 12888, source: 'douyin' },
      { id: 't2', title: '测试关键词', category: '彩妆', heatScore: 99.2, source: 'weibo' },
    ])

    renderWithProviders(
      <TrendingTopicsPanel onInjectKeyword={onInject} selectedKeywords={['测试关键词']} />,
    )

    fireEvent.click(screen.getByTestId('trending-topics-open-button'))

    expect(await screen.findByTestId('trending-topics-panel')).toHaveAttribute('data-contract-scope', 'live-trending-topics-readonly')
    expect(screen.getByTestId('trending-topics-panel')).toHaveAttribute('data-ready-endpoints', '/ai/brain/trends/current')
    expect(screen.getByTestId('trending-topics-panel')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-trend-fallback'))
    expect(await screen.findByText('换季敏感肌')).toBeInTheDocument()
    expect(screen.getAllByTestId('trending-topics-item')).toHaveLength(2)

    const buttons = screen.getAllByTestId('trending-topics-inject-button')
    expect(buttons[1]).toHaveAttribute('data-selected', 'true')
    fireEvent.click(buttons[0])

    expect(onInject).toHaveBeenCalledWith('换季敏感肌')
  })

  it('shows trend loading errors without local fallback topics', async () => {
    vi.mocked(brainTrendsCurrent).mockRejectedValue(new Error('trend down'))

    renderWithProviders(<TrendingTopicsPanel onInjectKeyword={vi.fn()} />)

    fireEvent.click(screen.getByTestId('trending-topics-open-button'))

    expect(await screen.findByTestId('trending-topics-error')).toHaveTextContent('/ai/brain/trends/current 热点趋势加载失败：trend down')
    expect(screen.getByTestId('trending-topics-error')).toHaveAttribute('data-no-local-trend-fallback', 'true')
    expect(screen.queryByTestId('trending-topics-item')).not.toBeInTheDocument()
    expect(screen.queryByText('暂无热点数据')).not.toBeInTheDocument()
  })

  it('shows empty state as real empty trend response', async () => {
    renderWithProviders(<TrendingTopicsPanel onInjectKeyword={vi.fn()} />)

    fireEvent.click(screen.getByTestId('trending-topics-open-button'))

    expect(await screen.findByTestId('trending-topics-empty-state')).toHaveAttribute('data-no-local-trend-fallback', 'true')
    expect(brainTrendsCurrent).toHaveBeenCalledWith({ limit: 15 })
  })
})
