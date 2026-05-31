import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import PublishPage from '../PublishPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    get: vi.fn(),
    save: vi.fn(),
    publishGenerateTitle: vi.fn(),
    publishAiReview: vi.fn(),
    publishSubmit: vi.fn(),
    contentCalendarView: vi.fn(),
    contentPublishTimeRecommend: vi.fn(),
    seoSuggestPublishTime: vi.fn(),
  },
  adaptContentCalendarMonthView: vi.fn(() => []),
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPage(initialEntry = '/admin/shortvideo/publish?projectId=7') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[initialEntry]}>
      <PublishPage />
    </MemoryRouter>,
  )
}

function renderPageWithTheme(initialEntry = '/admin/shortvideo/publish?projectId=7') {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <PublishPage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('PublishPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      ownerId: 1,
      title: '精华液短视频',
      projectType: 'soft_ad',
      status: 'completed',
      finalVideoUrl: 'https://cdn.example.com/final.mp4',
      thumbnailUrl: 'https://cdn.example.com/cover.jpg',
      publishTitle: '旧标题',
      publishPlatforms: '["douyin"]',
      duration: 30,
    } as never)
    vi.mocked(shortvideoApi.save).mockResolvedValue(7 as never)
    vi.mocked(shortvideoApi.publishGenerateTitle).mockResolvedValue({
      titles: [{ text: 'AI 爆款标题', score: 0.9 }],
    } as never)
    vi.mocked(shortvideoApi.publishAiReview).mockResolvedValue({
      passed: true,
      issues: [],
      suggestions: ['建议增加字幕'],
      projectId: 7,
    } as never)
    vi.mocked(shortvideoApi.publishSubmit).mockResolvedValue({
      success: false,
      degraded: true,
      projectId: 7,
      results: [{ platform: 'douyin', success: false, error: '抖音开放平台未配置' }],
    } as never)
    vi.mocked(shortvideoApi.seoSuggestPublishTime).mockResolvedValue(['今晚 20:00'] as never)
  })

  it('loads project publish workspace and runs save, review, and publish actions', async () => {
    renderPage()

    expect(screen.getByText('项目发布')).toBeInTheDocument()
    await waitFor(() => {
      expect(shortvideoApi.get).toHaveBeenCalledWith(7)
    })

    expect(await screen.findByDisplayValue('旧标题')).toBeInTheDocument()
    expect(screen.getByText('https://cdn.example.com/final.mp4')).toBeInTheDocument()
    expect(screen.getByText(/平台发布能力/)).toBeInTheDocument()
    expect(screen.getByText(/实际提交平台：抖音/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-contract-scope', 'shortvideo-publish')
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-contract-endpoint', '/short-video/publish/publish')
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/publish?projectId=:id'))
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-supported-actions', expect.stringContaining('submit-supported-platform-publish'))
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/short-video/publish/ai-review'))
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/publish/local-publish'))
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-no-local-publish-fallback', 'true')
    expect(screen.getByTestId('shortvideo-publish-contract-summary')).toHaveAttribute('data-contract-status', 'partial')
    expect(screen.getByTestId('shortvideo-publish-contract-summary')).toHaveAttribute('data-no-local-publish-fallback', 'true')
    expect(screen.getByTestId('shortvideo-publish-platform-matrix')).toHaveAttribute('data-supported-platforms', 'douyin')
    expect(screen.getByTestId('shortvideo-publish-payload-platforms')).toHaveAttribute('data-payload-platforms', 'douyin')
    expect(screen.getByTestId('shortvideo-publish-save-button')).toHaveAttribute('data-source-endpoint', '/short-video/project/save')
    expect(screen.getByTestId('shortvideo-publish-title-button')).toHaveAttribute('data-source-endpoint', '/short-video/publish/generate-title')
    expect(screen.getByTestId('shortvideo-publish-review-button')).toHaveAttribute('data-source-endpoint', '/short-video/publish/ai-review')
    expect(screen.getByTestId('shortvideo-publish-submit-button')).toHaveAttribute('data-source-endpoint', '/short-video/publish/publish')
    const platformContracts = screen.getAllByTestId('shortvideo-publish-platform-contract')
    expect(platformContracts.some(card =>
      card.getAttribute('data-platform') === 'douyin'
      && card.getAttribute('data-contract-status') === 'ready'
      && card.getAttribute('data-contract-endpoint') === '/short-video/publish/publish',
    )).toBe(true)
    expect(platformContracts.some(card =>
      card.getAttribute('data-platform') === 'weixin-video'
      && card.getAttribute('data-contract-status') === 'degraded'
      && card.getAttribute('data-contract-endpoint') === '/short-video/publish/publish',
    )).toBe(true)

    fireEvent.change(screen.getByLabelText('发布标题'), { target: { value: '新版发布标题' } })
    fireEvent.click(screen.getByRole('button', { name: '保存计划' }))
    await waitFor(() => {
      expect(shortvideoApi.save).toHaveBeenCalledWith(expect.objectContaining({
        id: 7,
        publishTitle: '新版发布标题',
        publishPlatforms: '["douyin"]',
      }))
    })

    fireEvent.click(screen.getByRole('button', { name: 'AI 标题' }))
    await waitFor(() => {
      expect(shortvideoApi.publishGenerateTitle).toHaveBeenCalledWith({
        projectId: 7,
        videoUrl: 'https://cdn.example.com/final.mp4',
        count: 5,
      })
    })
    expect(await screen.findByDisplayValue('AI 爆款标题')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'AI 审核' }))
    await waitFor(() => {
      expect(shortvideoApi.publishAiReview).toHaveBeenCalledWith(expect.objectContaining({
        projectId: 7,
        videoUrl: 'https://cdn.example.com/final.mp4',
        title: 'AI 爆款标题',
      }))
    })
    expect(await screen.findByText('审核通过')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '发布' }))
    await waitFor(() => {
      expect(shortvideoApi.publishSubmit).toHaveBeenCalledWith(expect.objectContaining({
        projectId: 7,
        title: 'AI 爆款标题',
        platforms: ['douyin'],
      }))
    })
    expect(await screen.findByText(/抖音开放平台未配置/)).toBeInTheDocument()
  })

  it('keeps unsupported platforms out of the real publish payload and marks them as skipped', async () => {
    vi.mocked(shortvideoApi.publishSubmit).mockResolvedValue({
      success: true,
      degraded: false,
      projectId: 7,
      results: [{ platform: 'douyin', success: true, itemId: 'aweme-1' }],
    } as never)
    renderPage()

    await screen.findByDisplayValue('旧标题')
    fireEvent.click(screen.getByLabelText('视频号'))

    expect(await screen.findByText(/视频号 当前未接入真实发布能力/)).toBeInTheDocument()
    expect(screen.getByText(/实际提交平台：抖音/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-publish-workbench')).toHaveAttribute('data-unsupported-platforms', 'weixin-video')
    expect(screen.getByTestId('shortvideo-publish-platform-matrix')).toHaveAttribute('data-unsupported-platforms', 'weixin-video')
    expect(screen.getByTestId('shortvideo-publish-platform-downgrade')).toHaveAttribute('data-downgrade-tone', 'platform-gap')
    expect(screen.getByTestId('shortvideo-publish-platform-downgrade')).toHaveAttribute('data-contract-status', 'degraded')
    expect(screen.getByTestId('shortvideo-publish-platform-downgrade')).toHaveAttribute('data-contract-endpoint', '/short-video/publish/publish')

    fireEvent.click(screen.getByRole('button', { name: '发布' }))
    await waitFor(() => {
      expect(shortvideoApi.publishSubmit).toHaveBeenCalledWith(expect.objectContaining({
        projectId: 7,
        title: '旧标题',
        platforms: ['douyin'],
      }))
    })
    expect(await screen.findByText(/抖音: 发布成功 aweme-1/)).toBeInTheDocument()
    expect(screen.getByText(/视频号: 前端已跳过/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-publish-result-downgrade')).toHaveAttribute('data-downgrade-tone', 'platform-gap')
    const resultRows = screen.getAllByTestId('shortvideo-publish-platform-result')
    expect(resultRows.some(row =>
      row.getAttribute('data-platform') === 'douyin'
      && row.getAttribute('data-platform-contract-status') === 'ready'
      && row.getAttribute('data-publish-success') === 'true',
    )).toBe(true)
    expect(resultRows.some(row =>
      row.getAttribute('data-platform') === 'weixin-video'
      && row.getAttribute('data-platform-contract-status') === 'degraded'
      && row.getAttribute('data-publish-success') === 'false',
    )).toBe(true)
  })

  it('disables publishing when only unsupported platforms remain selected', async () => {
    renderPage()

    await screen.findByDisplayValue('旧标题')
    fireEvent.click(screen.getByLabelText('视频号'))
    fireEvent.click(screen.getByLabelText('抖音'))

    expect(await screen.findByText(/实际提交平台：无可提交平台/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '发布' })).toBeDisabled()
  })

  it('falls back to calendar mode when projectId is missing', async () => {
    vi.mocked(shortvideoApi.contentCalendarView).mockResolvedValue({ days: {} } as never)
    renderPage('/admin/shortvideo/publish')

    expect(await screen.findByRole('heading', { name: '审核发布' })).toBeInTheDocument()
    await waitFor(() => {
      expect(shortvideoApi.contentCalendarView).toHaveBeenCalled()
    })
    expect(shortvideoApi.get).not.toHaveBeenCalled()
    expect(screen.getByTestId('shortvideo-publish-calendar-page')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/short-video/content/calendar'))
    expect(screen.getByTestId('shortvideo-publish-calendar-page')).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/publish'))
    expect(screen.getByTestId('shortvideo-publish-calendar-page')).toHaveAttribute('data-supported-actions', expect.stringContaining('view-calendar-publish-surface'))
    expect(screen.getByTestId('shortvideo-publish-calendar-page')).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/content/local-calendar'))
    expect(screen.getByTestId('shortvideo-publish-calendar-boundary-contract')).toHaveAttribute('data-no-local-calendar-fallback', 'true')
  })

  it('shows endpoint errors for project publish actions while keeping form state', async () => {
    vi.mocked(shortvideoApi.save).mockRejectedValueOnce(new Error('save denied') as never)
    vi.mocked(shortvideoApi.publishGenerateTitle).mockRejectedValueOnce(new Error('title llm down') as never)
    vi.mocked(shortvideoApi.publishAiReview).mockRejectedValueOnce(new Error('audit down') as never)
    vi.mocked(shortvideoApi.publishSubmit).mockRejectedValueOnce(new Error('publish gateway down') as never)
    renderPage()

    await screen.findByDisplayValue('旧标题')
    fireEvent.change(screen.getByLabelText('发布标题'), { target: { value: '失败保留标题' } })

    fireEvent.click(screen.getByRole('button', { name: '保存计划' }))
    expect(await screen.findByText(/保存发布计划失败（POST \/short-video\/project\/save）：save denied/)).toBeInTheDocument()
    expect(screen.getByLabelText('发布标题')).toHaveValue('失败保留标题')
    expect(screen.getByTestId('shortvideo-publish-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('shortvideo-publish-action-error')).toHaveAttribute('data-no-local-publish-fallback', 'true')

    fireEvent.click(screen.getByRole('button', { name: 'AI 标题' }))
    expect(await screen.findByText(/AI 标题生成失败（POST \/short-video\/publish\/generate-title）：title llm down/)).toBeInTheDocument()
    expect(screen.getByLabelText('发布标题')).toHaveValue('失败保留标题')

    fireEvent.click(screen.getByRole('button', { name: 'AI 审核' }))
    expect(await screen.findByText(/AI 审核失败（POST \/short-video\/publish\/ai-review）：audit down/)).toBeInTheDocument()
    expect(screen.queryByText('审核通过')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '发布' }))
    expect(await screen.findByText(/发布失败（POST \/short-video\/publish\/publish）：publish gateway down/)).toBeInTheDocument()
    expect(screen.queryByText(/^抖音: 发布成功/)).not.toBeInTheDocument()
  })

  it('shows project load endpoint errors', async () => {
    vi.mocked(shortvideoApi.get).mockRejectedValueOnce(new Error('project denied') as never)
    renderPage()

    expect(await screen.findByText(/项目详情加载失败（POST \/short-video\/project\/get）：project denied/)).toBeInTheDocument()
    expect(screen.queryByText('旧标题')).not.toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-publish-project-error')).toHaveAttribute('data-no-local-project-fallback', 'true')
  })

  it('shows calendar and publish-time recommendation endpoint errors without fallback data', async () => {
    vi.mocked(shortvideoApi.contentCalendarView).mockRejectedValueOnce(new Error('calendar down') as never)
    vi.mocked(shortvideoApi.seoSuggestPublishTime).mockRejectedValueOnce(new Error('seo time down') as never)
    renderPage('/admin/shortvideo/publish')

    expect(await screen.findByText(/内容日历加载失败（POST \/short-video\/content\/calendar）：calendar down/)).toBeInTheDocument()
    expect(await screen.findByText(/发布时间建议加载失败（POST \/short-video\/seo\/suggest-publish-time）：seo time down/)).toBeInTheDocument()
    expect(screen.queryByText('模拟排期')).not.toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-publish-calendar-error')).toHaveAttribute('data-no-local-calendar-fallback', 'true')
    expect(screen.getByTestId('shortvideo-publish-recommend-error')).toHaveAttribute('data-no-static-publish-time', 'true')
  })

  it('uses theme-aware calendar today surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(shortvideoApi.contentCalendarView).mockResolvedValue({ days: {} } as never)

    renderPageWithTheme('/admin/shortvideo/publish')

    expect(await screen.findByTestId('publish-calendar-today-cell')).toHaveStyle({
      backgroundColor: 'rgba(144, 202, 249, 0.18)',
    })
  })
})
