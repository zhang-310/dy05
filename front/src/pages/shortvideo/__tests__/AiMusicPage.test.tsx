import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AiMusicPage from '../AiMusicPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    musicHistory: vi.fn(),
    generateBgm: vi.fn(),
    generateSfx: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('AiMusicPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
    vi.mocked(shortvideoApi.musicHistory).mockResolvedValue([
      {
        id: 1,
        name: '历史 BGM',
        duration: '30秒',
        url: 'https://cdn.test/history.mp3',
        type: 'bgm',
        provider: 'suno',
        source: 'sv_generation_log',
      },
      {
        id: 2,
        name: '降级音效',
        duration: '3秒',
        type: 'sfx',
        source: 'service_degraded',
        degraded: true,
      },
    ] as never)
    vi.mocked(shortvideoApi.generateBgm).mockResolvedValue({
      musicUrl: 'https://cdn.test/bgm.mp3',
      provider: 'suno',
      durationMs: 30000,
      bpm: 96,
    } as never)
    vi.mocked(shortvideoApi.generateSfx).mockResolvedValue([
      {
        description: '产品展示音效',
        audioUrl: 'https://cdn.test/sfx.mp3',
        durationSec: 3,
      },
    ] as never)
  })

  it('maps backend musicUrl/audioUrl fields and exposes usable audio actions', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AiMusicPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('shortvideo-ai-music-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/short-video/music/generate-bgm|/short-video/music/generate-sfx|/short-video/music/history',
    )
    expect(screen.getByTestId('shortvideo-ai-music-page')).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/ai-music'))
    expect(screen.getByTestId('shortvideo-ai-music-page')).toHaveAttribute('data-supported-actions', expect.stringContaining('copy-audio-url'))
    expect(screen.getByTestId('shortvideo-ai-music-boundary-contract')).toHaveAttribute('data-no-local-audio-fallback', 'true')
    expect(screen.getByTestId('shortvideo-ai-music-refresh-button')).toHaveAttribute('data-source-endpoint', '/short-video/music/history')
    expect(await screen.findByText('历史 BGM')).toBeInTheDocument()
    expect(screen.getByText('降级音效')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-ai-music-history-contract')).toHaveAttribute('data-no-local-history-fallback', 'true')
    expect(screen.getByText('来源：sv_generation_log')).toBeInTheDocument()
    expect(screen.getByText('服务侧降级')).toBeInTheDocument()
    expect(screen.getByText(/仅返回任务结果，未拿到可播放 URL/)).toBeInTheDocument()
    expect(screen.getAllByTestId('shortvideo-ai-music-empty-url')[0]).toHaveAttribute('data-no-local-audio-fallback', 'true')

    fireEvent.click(screen.getByRole('button', { name: '生成背景音乐' }))
    expect(screen.getByTestId('shortvideo-ai-music-generate-bgm-button')).toHaveAttribute('data-source-endpoint', '/short-video/music/generate-bgm')

    await waitFor(() => {
      expect(shortvideoApi.generateBgm).toHaveBeenCalledWith(expect.objectContaining({
        durationSec: 30,
        instrumental: true,
      }))
    })

    expect(await screen.findByText('轻松愉快BGM')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-ai-music-bgm-result')).toHaveAttribute('data-source-endpoint', '/short-video/music/generate-bgm')
    expect(screen.getAllByText('Provider：suno').length).toBeGreaterThan(0)
    expect(screen.getByText('BPM：96')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '生成音效' }))
    expect(screen.getByTestId('shortvideo-ai-music-generate-sfx-button')).toHaveAttribute('data-source-endpoint', '/short-video/music/generate-sfx')

    await waitFor(() => {
      expect(shortvideoApi.generateSfx).toHaveBeenCalledWith({
        sceneDescription: '产品展示',
        durationSec: 3,
      })
    })

    expect(await screen.findByText('产品展示音效')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-ai-music-sfx-result')).toHaveAttribute('data-source-endpoint', '/short-video/music/generate-sfx')
  })

  it('shows endpoint-specific failures and empty-url degraded results', async () => {
    vi.mocked(shortvideoApi.generateBgm).mockRejectedValueOnce(new Error('Suno 网关不可用') as never)
    vi.mocked(shortvideoApi.generateSfx).mockResolvedValueOnce([
      {
        description: '只返回任务',
        durationSec: 3,
      },
    ] as never)

    renderWithProviders(
      <MemoryRouter>
        <AiMusicPage />
      </MemoryRouter>,
    )

    await screen.findByText('历史 BGM')
    fireEvent.click(screen.getByRole('button', { name: '生成背景音乐' }))
    expect(await screen.findByText(/背景音乐生成失败（POST \/short-video\/music\/generate-bgm）：Suno 网关不可用/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-ai-music-bgm-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText('护肤教程')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '生成音效' }))
    expect(await screen.findByText('只返回任务')).toBeInTheDocument()
    expect(screen.getAllByText(/仅返回任务结果，未拿到可播放 URL/).length).toBeGreaterThan(0)
  })

  it('shows history and SFX endpoint failures without fallback audio', async () => {
    vi.mocked(shortvideoApi.musicHistory).mockRejectedValueOnce(new Error('history table down') as never)
    vi.mocked(shortvideoApi.generateSfx).mockRejectedValueOnce(new Error('sfx provider down') as never)

    renderWithProviders(
      <MemoryRouter>
        <AiMusicPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/历史记录加载失败（POST \/short-video\/music\/history）：history table down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-ai-music-history-error')).toHaveAttribute('data-no-local-history-fallback', 'true')
    expect(screen.queryByText('模拟历史')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '生成音效' }))
    expect(await screen.findByText(/音效生成失败（POST \/short-video\/music\/generate-sfx）：sfx provider down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-ai-music-sfx-error')).toHaveAttribute('data-no-local-audio-fallback', 'true')
    expect(screen.getByText('产品展示')).toBeInTheDocument()
    expect(screen.queryByText('占位音效')).not.toBeInTheDocument()
  })
})
