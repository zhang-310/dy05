import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import ContentCalendarPage from '../ContentCalendarPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', async () => {
  const actual = await vi.importActual<typeof import('@/api/shortvideo')>('@/api/shortvideo')
  return {
    ...actual,
    shortvideoApi: {
      contentCalendarView: vi.fn(),
      contentCalendarMonthStats: vi.fn(),
      seoSuggestPublishTime: vi.fn(),
      contentCalendarAutoGenerate: vi.fn(),
      contentCalendarSave: vi.fn(),
    },
  }
})

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('ContentCalendarPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(shortvideoApi.contentCalendarView).mockResolvedValue({
      data: {
        records: [
          { plan_date: '2026-05-22', title: '包装月历计划', status: 0 },
        ],
      },
    } as never)
    vi.mocked(shortvideoApi.contentCalendarMonthStats).mockResolvedValue({
      plannedCount: 3,
      publishedCount: 1,
      completionRate: 33.3,
    } as never)
    vi.mocked(shortvideoApi.seoSuggestPublishTime).mockResolvedValue(['今晚 20:00'] as never)
    vi.mocked(shortvideoApi.contentCalendarAutoGenerate).mockResolvedValue(5 as never)
    vi.mocked(shortvideoApi.contentCalendarSave).mockResolvedValue(11 as never)
  })

  it('renders wrapped calendar row lists, publish-time suggestions and writes manual plans', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ContentCalendarPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '内容日历' })).toBeInTheDocument()
    expect(screen.getByTestId('content-calendar-page')).toHaveAttribute('data-ready-endpoints', [
      '/short-video/content/calendar',
      '/short-video/content/calendar-stats',
      '/short-video/content-calendar/save',
      '/short-video/content-calendar/auto-generate',
      '/short-video/seo/suggest-publish-time',
    ].join('|'))
    expect(screen.getByTestId('content-calendar-page')).toHaveAttribute('data-no-local-calendar-fallback', 'true')
    expect(await screen.findByText('包装月历计划')).toBeInTheDocument()
    expect(await screen.findByText('今晚 20:00')).toBeInTheDocument()
    expect(screen.getByTestId('content-calendar-stats-contract')).toHaveAttribute('data-no-static-stats-fallback', 'true')
    expect(screen.getByTestId('content-calendar-month-grid-contract')).toHaveAttribute('data-no-local-calendar-fallback', 'true')

    fireEvent.change(screen.getByLabelText('达人 Persona ID'), { target: { value: '12' } })
    fireEvent.click(screen.getByRole('button', { name: '自动生成本月排期' }))
    await waitFor(() => {
      expect(shortvideoApi.contentCalendarAutoGenerate).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
      }))
    })

    fireEvent.click(screen.getByRole('button', { name: '新建计划' }))
    const dialog = await screen.findByRole('dialog', { name: '新建内容计划' })
    fireEvent.change(within(dialog).getByLabelText(/计划标题/), { target: { value: '手动月历计划' } })
    fireEvent.change(within(dialog).getByLabelText(/项目 ID/), { target: { value: '88' } })
    fireEvent.change(within(dialog).getByLabelText(/计划说明/), { target: { value: '包装响应手动保存' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.contentCalendarSave).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
        title: '手动月历计划',
        projectId: 88,
        brief: '包装响应手动保存',
      }))
    })
  })

  it('keeps explicit endpoint failures without local calendar fallback', async () => {
    vi.mocked(shortvideoApi.contentCalendarView).mockRejectedValueOnce(new Error('calendar rows down') as never)
    vi.mocked(shortvideoApi.contentCalendarMonthStats).mockRejectedValueOnce(new Error('stats down') as never)
    vi.mocked(shortvideoApi.seoSuggestPublishTime).mockRejectedValueOnce(new Error('time down') as never)
    vi.mocked(shortvideoApi.contentCalendarAutoGenerate).mockRejectedValueOnce(new Error('auto down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ContentCalendarPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/内容日历加载失败（POST \/short-video\/content\/calendar）：calendar rows down/)).toBeInTheDocument()
    expect(await screen.findByText(/月统计不可用（POST \/short-video\/content\/calendar-stats）：stats down/)).toBeInTheDocument()
    expect(await screen.findByText(/发布时间推荐不可用（POST \/short-video\/seo\/suggest-publish-time）：time down/)).toBeInTheDocument()
    expect(screen.getByTestId('content-calendar-view-error')).toHaveAttribute('data-no-local-calendar-fallback', 'true')
    expect(screen.getByTestId('content-calendar-stats-error')).toHaveAttribute('data-no-static-stats-fallback', 'true')
    expect(screen.getByTestId('content-calendar-publish-time-error')).toHaveAttribute('data-no-local-publish-time-fallback', 'true')
    expect(screen.queryByText('模拟排期')).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('达人 Persona ID'), { target: { value: '12' } })
    fireEvent.click(screen.getByRole('button', { name: '自动生成本月排期' }))
    expect(await screen.findByText(/自动排期失败（POST \/short-video\/content-calendar\/auto-generate）：auto down/)).toBeInTheDocument()
    expect(screen.getByTestId('content-calendar-auto-error')).toHaveAttribute('data-no-local-schedule-mutation', 'true')
  })

  it('uses theme-aware today cell background in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <ContentCalendarPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByTestId('content-calendar-today-cell')).toHaveStyle({
      backgroundColor: 'rgba(144, 202, 249, 0.18)',
    })
  })
})
