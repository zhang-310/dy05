import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import SeoOptimizePage from '../SeoOptimizePage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    seoSuggestTags: vi.fn(),
    seoSuggestAbTitles: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('SeoOptimizePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
    vi.mocked(shortvideoApi.seoSuggestTags).mockResolvedValue({ records: ['敏感肌', '屏障修护', '护肤教程'] } as never)
    vi.mocked(shortvideoApi.seoSuggestAbTitles).mockResolvedValue({ data: ['敏感肌屏障修护教程'] } as never)
  })

  it('uses real SEO endpoints and renders explicit recommendation diagnostics from wrapped data', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SeoOptimizePage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('shortvideo-seo-optimize-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/short-video/seo/suggest-tags|/short-video/seo/suggest-ab-titles',
    )
    expect(screen.getByTestId('shortvideo-seo-boundary-contract')).toHaveAttribute('data-no-play-count-prediction', 'true')
    expect(screen.getByText(/当前后端提供标签、标题 A\/B/)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('视频标题'), { target: { value: '敏感肌如何修护屏障' } })
    fireEvent.change(screen.getByLabelText('内容类别'), { target: { value: '护肤教程' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI SEO 分析' }))

    await waitFor(() => {
      expect(shortvideoApi.seoSuggestTags).toHaveBeenCalledWith({
        title: '敏感肌如何修护屏障',
        description: undefined,
        industry: '护肤教程',
      })
      expect(shortvideoApi.seoSuggestAbTitles).toHaveBeenCalledWith({
        baseTitle: '敏感肌如何修护屏障',
      })
    })

    expect(await screen.findByText(/推荐话题标签：敏感肌、屏障修护、护肤教程/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-seo-result-contract')).toHaveAttribute('data-no-static-seo-score', 'true')
    expect(screen.getByTestId('shortvideo-seo-score-chip')).toHaveAttribute('data-score-source', 'tag-count-diagnostic')
    expect(screen.getByText('敏感肌屏障修护教程')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '获取关键词' }))
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-seo-keyword-contract')).toHaveAttribute('data-no-local-keyword-fallback', 'true')
  })

  it('shows tags endpoint failures and keeps SEO inputs', async () => {
    vi.mocked(shortvideoApi.seoSuggestTags).mockRejectedValueOnce(new Error('tags down') as never)
    renderWithProviders(
      <MemoryRouter>
        <SeoOptimizePage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('视频标题'), { target: { value: '敏感肌如何修护屏障' } })
    fireEvent.change(screen.getByLabelText('内容类别'), { target: { value: '护肤教程' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI SEO 分析' }))

    expect(await screen.findByText(/标签推荐失败（POST \/short-video\/seo\/suggest-tags）：tags down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-seo-analysis-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByLabelText('视频标题')).toHaveValue('敏感肌如何修护屏障')
    expect(screen.queryByText(/^SEO 评分：/)).not.toBeInTheDocument()
  })

  it('shows A/B title endpoint failures and keeps title inputs', async () => {
    vi.mocked(shortvideoApi.seoSuggestAbTitles).mockRejectedValueOnce(new Error('ab title down') as never)
    renderWithProviders(
      <MemoryRouter>
        <SeoOptimizePage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('视频标题'), { target: { value: '敏感肌如何修护屏障' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI SEO 分析' }))

    expect(await screen.findByText(/标题 A\/B 变体生成失败（POST \/short-video\/seo\/suggest-ab-titles）：ab title down/)).toBeInTheDocument()
    expect(screen.getByLabelText('视频标题')).toHaveValue('敏感肌如何修护屏障')
  })

  it('shows keyword endpoint failures without static keyword fallback', async () => {
    vi.mocked(shortvideoApi.seoSuggestTags).mockRejectedValueOnce(new Error('keyword down') as never)
    renderWithProviders(
      <MemoryRouter>
        <SeoOptimizePage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('内容类别'), { target: { value: '护肤教程' } })
    fireEvent.click(screen.getByRole('button', { name: '获取关键词' }))

    expect(await screen.findByText(/关键词获取失败（POST \/short-video\/seo\/suggest-tags）：keyword down/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-seo-keyword-error')).toHaveAttribute('data-no-local-keyword-fallback', 'true')
    expect(screen.queryByText('模拟关键词')).not.toBeInTheDocument()
  })

  it('uses a theme-aware optimized title surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <SeoOptimizePage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.change(screen.getByLabelText('视频标题'), { target: { value: '敏感肌如何修护屏障' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI SEO 分析' }))

    expect(await screen.findByText('敏感肌屏障修护教程')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-seo-optimized-title-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(232, 245, 233)',
    })
  })
})
