import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import TianApiPanelPage from '../TianApiPanelPage'
import { tianapi } from '@/api/tianapi'

vi.mock('@/api/tianapi', () => ({
  tianapi: {
    status: vi.fn(),
    hotDouyin: vi.fn(),
    hotToutiao: vi.fn(),
    hotWeibo: vi.fn(),
    hotNetwork: vi.fn(),
  },
}))

describe('TianApiPanelPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(tianapi.status).mockResolvedValue({ enabled: true } as never)
    vi.mocked(tianapi.hotDouyin).mockResolvedValue([{ word: '护肤热榜', hotIndex: 100000, link: 'https://example.com/hot', position: 7, label: '抖音' }] as never)
    vi.mocked(tianapi.hotToutiao).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotWeibo).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotNetwork).mockResolvedValue([] as never)
  })

  it('loads tianapi status and renders hot list panel', async () => {
    renderWithProviders(
      <MemoryRouter>
        <TianApiPanelPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('天API 数据面板')).toBeInTheDocument()

    await waitFor(() => {
      expect(tianapi.status).toHaveBeenCalled()
      expect(tianapi.hotDouyin).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('抖音热榜')).toBeInTheDocument()
      expect(screen.getByText('护肤热榜')).toBeInTheDocument()
      expect(screen.getAllByText('已配置').length).toBeGreaterThan(0)
      expect(screen.getByText('有数据来源')).toBeInTheDocument()
      expect(screen.getByText('异常来源')).toBeInTheDocument()
    })
    expect(screen.getByTestId('tianapi-panel-page-workbench')).toHaveAttribute('data-contract-scope', 'system-tianapi-hot-sources')
    expect(screen.getByTestId('tianapi-panel-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/tianapi/quota')
    expect(screen.getByTestId('tianapi-panel-page-workbench')).toHaveAttribute('data-no-plaintext-key-display', 'true')
    expect(screen.getByTestId('tianapi-source-contract')).toHaveAttribute('data-no-synthetic-quota-display', 'true')
    expect(screen.getByTestId('tianapi-source-contract').getAttribute('data-unsupported-endpoints')).toContain('/tianapi/config/get-secret')
    expect(screen.getByTestId('tianapi-hot-source-douyin')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('tianapi-hot-source-toutiao')).toHaveAttribute('data-no-local-hot-list-fallback', 'true')
    expect(screen.getByTestId('tianapi-refresh-all')).toHaveAttribute('data-refresh-scope', 'status-and-all-hot-sources')
    expect(screen.getByTestId('tianapi-hot-item-douyin')).toHaveAttribute('data-contract-source', '/tianapi/hot/douyin')
    expect(screen.getByTestId('tianapi-hot-item-douyin')).toHaveAttribute('data-rank', '7')
    expect(screen.getByTestId('tianapi-hot-item-douyin')).toHaveAttribute('data-has-link', 'true')
    expect(screen.getByTestId('tianapi-hot-item-douyin')).toHaveAttribute('data-no-mock-hot-item-injection', 'true')
    expect(screen.getByTestId('tianapi-hot-item-link-douyin')).toHaveAttribute('href', 'https://example.com/hot')
    expect(screen.getByTestId('tianapi-hot-item-source-chip-douyin')).toBeInTheDocument()
  })

  it('keeps source failures isolated and refreshes all tianapi sources', async () => {
    vi.mocked(tianapi.status).mockResolvedValue({ enabled: false } as never)
    vi.mocked(tianapi.hotDouyin).mockResolvedValue([{ word: '抖音热点', hotIndex: 1000 }] as never)
    vi.mocked(tianapi.hotToutiao).mockRejectedValue(new Error('quota exhausted') as never)
    vi.mocked(tianapi.hotWeibo).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotNetwork).mockResolvedValue([{ word: '全网热点', hotIndex: 900 }] as never)

    renderWithProviders(
      <MemoryRouter>
        <TianApiPanelPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('TianAPI 未配置 API Key，热点数据会按来源加载失败并显示具体错误。')).toBeInTheDocument()
    expect(await screen.findByText(/quota exhausted/)).toBeInTheDocument()
    expect(screen.getByText('抖音热点')).toBeInTheDocument()
    expect(screen.getAllByText('全网热点').length).toBeGreaterThan(0)
    expect(screen.getByText('异常来源')).toBeInTheDocument()
    expect(screen.getByTestId('tianapi-disabled-warning')).toHaveAttribute('data-error-isolated', 'true')
    expect(screen.getByTestId('tianapi-hot-source-error-toutiao')).toHaveAttribute('data-error-isolated', 'true')
    expect(screen.getByTestId('tianapi-hot-source-douyin')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('tianapi-hot-source-network')).toHaveAttribute('data-row-count', '1')

    const statusCalls = vi.mocked(tianapi.status).mock.calls.length
    const douyinCalls = vi.mocked(tianapi.hotDouyin).mock.calls.length
    const toutiaoCalls = vi.mocked(tianapi.hotToutiao).mock.calls.length
    const weiboCalls = vi.mocked(tianapi.hotWeibo).mock.calls.length
    const networkCalls = vi.mocked(tianapi.hotNetwork).mock.calls.length
    expect(screen.getByTestId('tianapi-hot-source-refresh-douyin')).toHaveAttribute('data-refresh-scope', 'single-source')
    fireEvent.click(screen.getByTestId('tianapi-hot-source-refresh-douyin'))

    await waitFor(() => {
      expect(tianapi.hotDouyin).toHaveBeenCalledTimes(douyinCalls + 1)
      expect(tianapi.hotToutiao).toHaveBeenCalledTimes(toutiaoCalls)
      expect(tianapi.hotWeibo).toHaveBeenCalledTimes(weiboCalls)
      expect(tianapi.hotNetwork).toHaveBeenCalledTimes(networkCalls)
    })

    fireEvent.click(screen.getByTestId('tianapi-refresh-all'))

    await waitFor(() => {
      expect(tianapi.status).toHaveBeenCalledTimes(statusCalls + 1)
      expect(tianapi.hotDouyin).toHaveBeenCalledTimes(douyinCalls + 2)
      expect(tianapi.hotToutiao).toHaveBeenCalledTimes(toutiaoCalls + 1)
      expect(tianapi.hotWeibo).toHaveBeenCalledTimes(weiboCalls + 1)
      expect(tianapi.hotNetwork).toHaveBeenCalledTimes(networkCalls + 1)
    })
  })

  it('renders wrapped tianapi hot list payloads', async () => {
    vi.mocked(tianapi.hotDouyin).mockResolvedValue({
      records: [{ word: '包装抖音热点', hotIndex: 1200 }],
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <TianApiPanelPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装抖音热点')).toBeInTheDocument()
    expect(screen.getByText('热点条目')).toBeInTheDocument()
    expect(screen.getByTestId('tianapi-hot-source-douyin')).toHaveAttribute('data-contract-source', '/tianapi/hot/douyin')
  })

  it('shows status error and empty source contracts without local hot fallback', async () => {
    vi.mocked(tianapi.status).mockRejectedValue(new Error('status down') as never)
    vi.mocked(tianapi.hotDouyin).mockResolvedValue([] as never)

    renderWithProviders(
      <MemoryRouter>
        <TianApiPanelPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('tianapi-status-error')).toHaveAttribute('data-no-synthetic-quota-display', 'true')
    expect(await screen.findByTestId('tianapi-hot-source-empty-douyin')).toHaveAttribute('data-no-local-hot-list-fallback', 'true')
    expect(screen.getByTestId('tianapi-hot-source-empty-douyin')).toHaveAttribute('data-no-mock-hot-item-injection', 'true')
    expect(screen.getByTestId('tianapi-panel-page-workbench')).toHaveAttribute('data-error-isolated', 'true')
  })
})
