import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ViralVideoPage from '../ViralVideoPage'
import { shortvideoApi, type ViralVideo } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    viralList: vi.fn(),
    viralGet: vi.fn(),
    viralAnalyze: vi.fn(),
    viralCollect: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

const listVideo: ViralVideo = {
  id: 1,
  title: '三秒建立信任的护肤爆款',
  authorName: '护肤达人',
  viewCount: 20340,
  likeCount: 1200,
  commentCount: 88,
  favoriteCount: 360,
  shareCount: 42,
  coverUrl: 'https://cdn.test/cover.jpg',
  videoUrl: 'https://www.douyin.com/video/1',
  deepAnalyzeStatus: 'completed',
}

const listVideoWithoutCover: ViralVideo = {
  ...listVideo,
  id: 2,
  title: '缺封面的采集视频',
  coverUrl: undefined,
  coverBosUrl: undefined,
}

const detailVideo: ViralVideo = {
  ...listVideo,
  title: '三秒建立信任的护肤爆款详情',
  videoBosUrl: '//cdn.test/video.mp4',
  keyframeBosUrls: JSON.stringify([
    'https://cdn.test/frame-1.jpg',
    'not-a-url',
    '//cdn.test/frame-3.jpg',
  ]),
  videoDuration: 18,
  transcript: 'ASR 实录口播',
  sceneDescriptions: '镜头从洗手台推进到精华液特写。',
  deepAnalyzeProgress: JSON.stringify({
    steps: {
      download: { status: 'done', detail: 'cached' },
      scene: { status: 'skipped', detail: 'using existing keyframes' },
    },
  }),
  deepAnalysisResult: JSON.stringify({
    transcript: { fullText: '模型对齐口播' },
    scenes: [
      {
        time: '0-3s',
        environment: '浴室镜头',
        person: '达人',
        props: '精华液',
        camera: '近景',
        mood: '信任',
      },
    ],
    keyframeUrls: ['//cdn.test/frame-2.jpg', 'https://cdn.test/frame-1.jpg'],
    opening: '前三秒痛点',
    emotion_curve: ['好奇', '信任'],
    viral_elements: ['反差承诺'],
    structure: { hook: '痛点切入' },
    viralHypotheses: ['强承诺降低理解成本'],
    remakeVariableTable: { hook: '替换为目标人群痛点' },
  }),
}

function renderPage(initialEntry = '/shortvideo/viral') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[initialEntry]}>
      <ViralVideoPage />
    </MemoryRouter>,
  )
}

describe('ViralVideoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
    vi.mocked(shortvideoApi.viralList).mockResolvedValue([listVideo] as never)
    vi.mocked(shortvideoApi.viralGet).mockResolvedValue(detailVideo as never)
    vi.mocked(shortvideoApi.viralAnalyze).mockResolvedValue({ ok: true, id: 1 } as never)
    vi.mocked(shortvideoApi.viralCollect).mockResolvedValue(1 as never)
  })

  it('loads viral videos, searches, and triggers card actions', async () => {
    renderPage()

    await waitFor(() => {
      expect(shortvideoApi.viralList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        keyword: undefined,
        minPlayCount: undefined,
      })
    })

    expect(await screen.findByText('三秒建立信任的护肤爆款')).toBeInTheDocument()
    expect(screen.getByTestId('viral-video-page')).toHaveAttribute('data-no-local-viral-fallback', 'true')
    expect(screen.getByTestId('viral-video-page')).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/viral-videos?videoId=:id'))
    expect(screen.getByTestId('viral-video-page')).toHaveAttribute('data-supported-actions', expect.stringContaining('analyze-viral-video'))
    expect(screen.getByTestId('viral-video-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/viral/local-list'),
    )
    expect(screen.getByTestId('viral-video-search-contract')).toHaveAttribute('data-server-filter-payload', 'true')
    expect(screen.getByTestId('viral-video-search-button')).toHaveAttribute('data-source-endpoint', '/short-video/viral/list')
    expect(screen.getByTestId('viral-video-refresh-list-button')).toHaveAttribute('data-source-endpoint', '/short-video/viral/list')
    expect(screen.getByTestId('viral-video-grid-contract')).toHaveAttribute('data-no-detail-prefetch', 'true')
    expect(screen.getAllByTestId('viral-video-card')[0]).toHaveAttribute('data-no-local-cover-fallback', 'true')
    expect(screen.getByText('护肤达人')).toBeInTheDocument()
    expect(screen.getByText('2.0w')).toBeInTheDocument()
    expect(screen.getByText('1200')).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('搜索关键词/作者'), {
      target: { value: '护肤' },
    })
    fireEvent.change(screen.getByPlaceholderText('播放量≥(可选)'), {
      target: { value: '10000' },
    })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    await waitFor(() => {
      expect(shortvideoApi.viralList).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        keyword: '护肤',
        minPlayCount: 10000,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('三秒建立信任的护肤爆款')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '收藏' }))
    await waitFor(() => {
      expect(shortvideoApi.viralCollect).toHaveBeenCalledWith(1)
    })

    fireEvent.click(screen.getByRole('button', { name: '拆解分析' }))
    await waitFor(() => {
      expect(shortvideoApi.viralAnalyze).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith(expect.stringContaining('分析任务已提交'), 'success')
    })
  })

  it('shows list failures and missing cover without fake placeholder images', async () => {
    vi.mocked(shortvideoApi.viralList).mockRejectedValueOnce(new Error('viral api down') as never)

    renderPage()

    expect(await screen.findByText(/爆款列表加载失败（POST \/short-video\/viral\/list）：viral api down/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-video-list-error')).toHaveAttribute('data-no-local-viral-fallback', 'true')
    expect(screen.queryByText('暂无数据')).not.toBeInTheDocument()

    vi.mocked(shortvideoApi.viralList).mockResolvedValueOnce([listVideoWithoutCover] as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))

    expect(await screen.findByText('缺封面的采集视频')).toBeInTheDocument()
    expect(screen.getByText('无封面')).toBeInTheDocument()
    expect(screen.getByText(/采集未返回 coverUrl/)).toBeInTheDocument()
    expect(document.querySelector('img[src*="picsum"]')).toBeNull()
  })

  it('renders detail drawer media, transcript, and structured analysis tabs', async () => {
    renderPage()

    await screen.findByText('三秒建立信任的护肤爆款')
    fireEvent.click(screen.getByRole('button', { name: '详情' }))

    await waitFor(() => {
      expect(shortvideoApi.viralGet).toHaveBeenCalledWith(1)
    })

    expect(await screen.findByText('爆款详情')).toBeInTheDocument()
    expect(screen.getByTestId('viral-video-refresh-detail-button')).toHaveAttribute('data-source-endpoint', '/short-video/viral/get')
    expect(screen.getByTestId('viral-video-open-detail-button')).toHaveAttribute('data-target-route', expect.stringContaining('/shortvideo/viral-videos?videoId=1'))
    expect(screen.getByText('三秒建立信任的护肤爆款详情')).toBeInTheDocument()
    expect(screen.getByText('缓存视频（BOS）')).toBeInTheDocument()
    expect(screen.getByText('关键帧')).toBeInTheDocument()
    expect(screen.getByAltText('关键帧 1')).toHaveAttribute('src', 'https://cdn.test/frame-1.jpg')
    expect(screen.getByAltText('关键帧 2')).toHaveAttribute('src', 'https://cdn.test/frame-3.jpg')
    expect(screen.getByAltText('关键帧 3')).toHaveAttribute('src', 'https://cdn.test/frame-2.jpg')
    expect(screen.getByText(/镜头从洗手台推进/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '口播脚本' }))
    expect(screen.getByText('ASR 实录口播')).toBeInTheDocument()
    expect(screen.getByText('模型对齐口播')).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('复制库表口播'))
    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('ASR 实录口播')
    })

    fireEvent.click(screen.getByRole('tab', { name: '拆解结论' }))
    expect(screen.getByText('分镜时间轴')).toBeInTheDocument()
    expect(screen.getByText(/场景 1/)).toBeInTheDocument()
    expect(screen.getByText('浴室镜头')).toBeInTheDocument()
    expect(screen.getByText('开场结构')).toBeInTheDocument()
    expect(screen.getByText('前三秒痛点')).toBeInTheDocument()
    expect(screen.getByText('二创变量表')).toBeInTheDocument()
    expect(screen.getAllByText(/替换为目标人群痛点/).length).toBeGreaterThan(0)

    const drawer = screen.getByText('爆款详情').closest('.MuiDrawer-paper')
    expect(drawer).not.toBeNull()
    fireEvent.click(within(drawer as HTMLElement).getByRole('button', { name: '收藏' }))
    await waitFor(() => {
      expect(shortvideoApi.viralCollect).toHaveBeenCalledWith(1)
    })
  })

  it('renders wrapped snake-case viral rows and detail fields from SDK contract', async () => {
    vi.mocked(shortvideoApi.viralList).mockResolvedValue([
      {
        id: 8,
        title: '包装爆款卡片',
        authorName: '包装达人',
        viewCount: 30000,
        likeCount: 2400,
        favoriteCount: 700,
        coverBosUrl: '//cdn.test/wrapped-cover.jpg',
        deepAnalyzeStatus: 'completed',
      },
    ] as never)
    vi.mocked(shortvideoApi.viralGet).mockResolvedValue({
      id: 8,
      title: '包装爆款详情',
      authorName: '包装达人',
      videoBosUrl: '//cdn.test/wrapped-video.mp4',
      keyframeBosUrls: '["//cdn.test/wrapped-frame.jpg"]',
      deepAnalyzeStatus: 'completed',
      transcript: '包装 ASR 口播',
      deepAnalysisResult: '{"opening":"包装前三秒"}',
    } as never)

    renderPage()

    expect(await screen.findByText('包装爆款卡片')).toBeInTheDocument()
    expect(screen.getByText('包装达人')).toBeInTheDocument()
    expect(screen.getByText('3.0w')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '详情' }))

    expect(await screen.findByText('包装爆款详情')).toBeInTheDocument()
    expect(screen.getByText('缓存视频（BOS）')).toBeInTheDocument()
    expect(screen.getByAltText('关键帧 1')).toHaveAttribute('src', 'https://cdn.test/wrapped-frame.jpg')
    fireEvent.click(screen.getByRole('tab', { name: '口播脚本' }))
    expect(screen.getByText('包装 ASR 口播')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: '拆解结论' }))
    expect(screen.getByText('包装前三秒')).toBeInTheDocument()
  })

  it('shows collect and analyze failures inline while keeping card context', async () => {
    vi.mocked(shortvideoApi.viralCollect).mockRejectedValueOnce(new Error('favorite table down') as never)
    vi.mocked(shortvideoApi.viralAnalyze).mockRejectedValueOnce(new Error('analyzer queue down') as never)

    renderPage()

    expect(await screen.findByText('三秒建立信任的护肤爆款')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '收藏' }))
    expect(await screen.findByText(/收藏失败（POST \/short-video\/viral\/collect）：favorite table down/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-video-action-error')).toHaveAttribute('data-no-local-viral-mutation', 'true')
    expect(screen.getByText('三秒建立信任的护肤爆款')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拆解分析' }))
    expect(await screen.findByText(/拆解分析失败（POST \/short-video\/viral\/analyze）：analyzer queue down/)).toBeInTheDocument()
    expect(screen.getByText('三秒建立信任的护肤爆款')).toBeInTheDocument()
  })

  it('shows detail action failures inline while keeping drawer context', async () => {
    vi.mocked(shortvideoApi.viralCollect).mockRejectedValueOnce(new Error('favorite denied') as never)
    vi.mocked(shortvideoApi.viralAnalyze).mockRejectedValueOnce(new Error('queue denied') as never)

    renderPage()

    await screen.findByText('三秒建立信任的护肤爆款')
    fireEvent.click(screen.getByRole('button', { name: '详情' }))
    expect(await screen.findByText('爆款详情')).toBeInTheDocument()

    const drawer = screen.getByText('爆款详情').closest('.MuiDrawer-paper')
    expect(drawer).not.toBeNull()
    fireEvent.click(within(drawer as HTMLElement).getByRole('button', { name: '收藏' }))
    expect(await screen.findByText(/收藏失败（POST \/short-video\/viral\/collect）：favorite denied/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-video-detail-action-error')).toHaveAttribute('data-no-local-viral-mutation', 'true')
    expect(screen.getByText('三秒建立信任的护肤爆款详情')).toBeInTheDocument()

    fireEvent.click(within(drawer as HTMLElement).getByRole('button', { name: '拆解分析' }))
    expect(await screen.findByText(/拆解分析失败（POST \/short-video\/viral\/analyze）：queue denied/)).toBeInTheDocument()
    expect(screen.getByText('爆款详情')).toBeInTheDocument()
  })

  it('opens a deep-linked video detail and shows missing media guidance', async () => {
    vi.mocked(shortvideoApi.viralGet).mockResolvedValue({
      id: 99,
      title: '深链视频',
      authorName: '运营账号',
      deepAnalyzeStatus: 'completed',
      deepAnalysisResult: 'not json',
    } as never)

    renderPage('/shortvideo/viral?videoId=99')

    await waitFor(() => {
      expect(shortvideoApi.viralGet).toHaveBeenCalledWith(99)
    })

    expect(await screen.findByText('深链视频')).toBeInTheDocument()
    expect(screen.getByText(/分析已完成，但未返回 BOS 视频地址与关键帧/)).toBeInTheDocument()
    expect(screen.getByTestId('viral-video-missing-media-downgrade')).toHaveAttribute('data-no-local-keyframe-fallback', 'true')
    expect(screen.getByTestId('viral-video-cover-missing')).toHaveAttribute('data-no-local-cover-fallback', 'true')
    expect(screen.getByText(/页面不会使用随机图片替代真实素材/)).toBeInTheDocument()
    expect(screen.getByText(/暂无抖音原链接/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '拆解结论' }))
    expect(screen.getByText('非 JSON 结果，请展开下方「原始拆解 JSON」查看。')).toBeInTheDocument()
  })
})
