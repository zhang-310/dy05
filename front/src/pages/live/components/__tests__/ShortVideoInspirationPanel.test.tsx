import { describe, it, expect, vi } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { ShortVideoInspirationPanel } from '../ShortVideoInspirationPanel'
import { getRecommendedVirals } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  getRecommendedVirals: vi.fn(),
}))

describe('ShortVideoInspirationPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(getRecommendedVirals).mockResolvedValue([])
  })

  it('renders without crashing', () => {
    const { container } = renderWithProviders(<ShortVideoInspirationPanel />)
    expect(container).toBeInTheDocument()
  })

  it('loads recommended virals with readonly contract and adapts through prop callback', async () => {
    const onAdapt = vi.fn()
    vi.mocked(getRecommendedVirals).mockResolvedValue([
      { id: 101, title: '爆款开头', playCount: 22000, viralScore: 91, hookLine: '你是不是也踩过这个护肤坑？' },
    ])

    renderWithProviders(<ShortVideoInspirationPanel onAdaptHook={onAdapt} />)

    fireEvent.click(screen.getByTestId('shortvideo-inspiration-open-button'))

    expect(await screen.findByTestId('shortvideo-inspiration-panel')).toHaveAttribute('data-contract-scope', 'live-shortvideo-inspiration-readonly')
    expect(screen.getByTestId('shortvideo-inspiration-panel')).toHaveAttribute('data-ready-endpoints', '/short-video/viral/recommended')
    expect(screen.getByTestId('shortvideo-inspiration-panel')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-viral-video-fallback'))
    expect(await screen.findByText('爆款开头')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-inspiration-item')).toHaveAttribute('data-contract-source', '/short-video/viral/recommended')

    fireEvent.click(screen.getByTestId('shortvideo-inspiration-adapt-button'))

    expect(onAdapt).toHaveBeenCalledWith('你是不是也踩过这个护肤坑？')
  })

  it('shows recommended viral errors without local fallback videos', async () => {
    vi.mocked(getRecommendedVirals).mockRejectedValue(new Error('viral down'))

    renderWithProviders(<ShortVideoInspirationPanel />)

    fireEvent.click(screen.getByTestId('shortvideo-inspiration-open-button'))

    expect(await screen.findByTestId('shortvideo-inspiration-error')).toHaveTextContent('/short-video/viral/recommended 短视频灵感加载失败：viral down')
    expect(screen.getByTestId('shortvideo-inspiration-error')).toHaveAttribute('data-no-local-viral-video-fallback', 'true')
    expect(screen.queryByTestId('shortvideo-inspiration-item')).not.toBeInTheDocument()
    expect(screen.queryByText('暂无推荐视频')).not.toBeInTheDocument()
  })

  it('shows empty state as real empty recommended viral response', async () => {
    renderWithProviders(<ShortVideoInspirationPanel />)

    fireEvent.click(screen.getByTestId('shortvideo-inspiration-open-button'))

    expect(await screen.findByTestId('shortvideo-inspiration-empty-state')).toHaveAttribute('data-no-local-viral-video-fallback', 'true')
    expect(getRecommendedVirals).toHaveBeenCalledWith({ limit: 10 })
  })
})
