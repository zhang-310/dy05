import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import DailyContentPage from '../DailyContentPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    calendar: vi.fn(),
    dashboardStats: vi.fn(),
    generateDaily: vi.fn(),
    contentCalendarAutoGenerate: vi.fn(),
    contentCalendarSave: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('DailyContentPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(shortvideoApi.calendar).mockResolvedValue([
      { id: 1, date: '2026-05-21', title: '自动规划 · review', status: 0 },
    ] as never)
    vi.mocked(shortvideoApi.dashboardStats).mockResolvedValue({ totalVideoCount: 2 } as never)
    vi.mocked(shortvideoApi.generateDaily).mockResolvedValue({ ok: true } as never)
    vi.mocked(shortvideoApi.contentCalendarAutoGenerate).mockResolvedValue(7 as never)
    vi.mocked(shortvideoApi.contentCalendarSave).mockResolvedValue(9 as never)
  })

  it('uses personaId contract for daily generation and content calendar auto generate', async () => {
    renderWithProviders(
      <MemoryRouter>
        <DailyContentPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '日更内容排期' })).toBeInTheDocument()
    expect(screen.getByTestId('daily-content-page')).toHaveAttribute('data-ready-endpoints', [
      '/short-video/project/generate-daily',
      '/short-video/content-calendar/date-range',
      '/short-video/content-calendar/save',
      '/short-video/content-calendar/auto-generate',
      '/short-video/dashboard/stats',
    ].join('|'))
    expect(screen.getByTestId('daily-content-page')).toHaveAttribute('data-no-local-calendar-fallback', 'true')
    expect(screen.getByText(/手动排期直接写入/)).toBeInTheDocument()
    expect(screen.getByText(/请先填写达人 Persona ID/)).toBeInTheDocument()
    expect(screen.getByTestId('daily-content-boundary-contract')).toHaveAttribute('data-no-local-daily-fallback', 'true')
    expect(screen.getByTestId('daily-content-summary-contract')).toHaveAttribute('data-no-local-calendar-fallback', 'true')

    fireEvent.change(screen.getByLabelText('达人 Persona ID'), { target: { value: '12' } })
    fireEvent.change(screen.getByLabelText('主题（可选）'), { target: { value: '屏障修护' } })
    fireEvent.change(screen.getByLabelText('风格（可选）'), { target: { value: '专业' } })

    await waitFor(() => {
      expect(shortvideoApi.calendar).toHaveBeenLastCalledWith(expect.objectContaining({
        personaId: 12,
      }))
    })

    fireEvent.click(screen.getByRole('button', { name: 'AI 生成日排' }))
    await waitFor(() => {
      expect(shortvideoApi.generateDaily).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
        count: 1,
        style: '专业',
        topic: '屏障修护',
      }))
    })

    fireEvent.click(screen.getByRole('button', { name: '自动排期' }))
    await waitFor(() => {
      expect(shortvideoApi.contentCalendarAutoGenerate).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
      }))
    })

    fireEvent.click(screen.getByRole('button', { name: '手动排期' }))
    fireEvent.change(await screen.findByLabelText(/计划标题/), { target: { value: '手动选题' } })
    fireEvent.change(screen.getByLabelText('项目 ID（可选）'), { target: { value: '88' } })
    fireEvent.change(screen.getByLabelText('计划说明'), { target: { value: '拍摄前准备脚本' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.contentCalendarSave).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
        title: '手动选题',
        projectId: 88,
        contentType: 'video',
        brief: '拍摄前准备脚本',
      }))
    })
  })

  it('keeps operation failures visible with backend endpoint sources', async () => {
    vi.mocked(shortvideoApi.generateDaily).mockRejectedValueOnce(new Error('LLM 不可用') as never)
    vi.mocked(shortvideoApi.contentCalendarAutoGenerate).mockRejectedValueOnce(new Error('排期规则失败') as never)
    vi.mocked(shortvideoApi.contentCalendarSave).mockRejectedValueOnce(new Error('标题重复') as never)

    renderWithProviders(
      <MemoryRouter>
        <DailyContentPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('达人 Persona ID'), { target: { value: '12' } })
    await waitFor(() => expect(shortvideoApi.calendar).toHaveBeenLastCalledWith(expect.objectContaining({ personaId: 12 })))

    fireEvent.click(screen.getByRole('button', { name: 'AI 生成日排' }))
    expect(await screen.findByText(/POST \/short-video\/project\/generate-daily 生成失败：LLM 不可用/)).toBeInTheDocument()
    expect(screen.getByText(/不生成本地占位日排/)).toBeInTheDocument()
    expect(screen.getByTestId('daily-content-generate-error')).toHaveAttribute('data-no-local-daily-fallback', 'true')

    fireEvent.click(screen.getByRole('button', { name: '自动排期' }))
    expect(await screen.findByText(/POST \/short-video\/content-calendar\/auto-generate 自动排期失败：排期规则失败/)).toBeInTheDocument()
    expect(screen.getByText(/不追加本地假记录/)).toBeInTheDocument()
    expect(screen.getByTestId('daily-content-auto-error')).toHaveAttribute('data-no-local-schedule-mutation', 'true')

    fireEvent.click(screen.getByRole('button', { name: '手动排期' }))
    fireEvent.change(await screen.findByLabelText(/计划标题/), { target: { value: '重复标题' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/POST \/short-video\/content-calendar\/save 手动排期保存失败：标题重复/)).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '手动排期' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('重复标题')).toBeInTheDocument()
    expect(screen.getByTestId('daily-content-save-error')).toHaveAttribute('data-input-retained', 'true')
  })

  it('uses theme-aware schedule item surface in dark mode', async () => {
    vi.mocked(shortvideoApi.calendar).mockResolvedValue([
      { id: 1, date: new Date().toISOString().slice(0, 10), title: '暗色排期', status: 0 },
    ] as never)
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <DailyContentPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByText('暗色排期')).toBeInTheDocument()
    expect(screen.getByTestId('daily-content-schedule-item')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })
})
