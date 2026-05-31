import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import SubtitleEditorPage from '../SubtitleEditorPage'
import { subtitleGet, subtitleSave, subtitleExportSrt } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', async () => {
  const actual = await vi.importActual<typeof import('@/api/shortvideo')>('@/api/shortvideo')
  return {
    ...actual,
    subtitleGet: vi.fn(),
    subtitleSave: vi.fn(),
    subtitleExportSrt: vi.fn(),
  }
})

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

describe('SubtitleEditorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(subtitleGet).mockResolvedValue([
      { id: 'seg-1', startTime: 0, endTime: 3, text: '前三秒痛点', position: 'bottom' },
    ] as never)
    vi.mocked(subtitleSave).mockResolvedValue(undefined as never)
    vi.mocked(subtitleExportSrt).mockResolvedValue('1\n00:00:00,000 --> 00:00:03,000\n前三秒痛点\n' as never)
    URL.createObjectURL = vi.fn(() => 'blob:test')
    URL.revokeObjectURL = vi.fn()
    HTMLAnchorElement.prototype.click = vi.fn()
    HTMLCanvasElement.prototype.getContext = vi.fn(() => ({
      clearRect: vi.fn(),
      beginPath: vi.fn(),
      moveTo: vi.fn(),
      lineTo: vi.fn(),
      stroke: vi.fn(),
      fillRect: vi.fn(),
      strokeRect: vi.fn(),
      fillStyle: '',
      strokeStyle: '',
      lineWidth: 1,
    })) as never
  })

  it('loads persisted subtitle segments and saves to backend table contract', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    const root = screen.getByTestId('subtitle-editor-page')
    expect(root).toHaveAttribute('data-ready-endpoints', '/short-video/edit/subtitles/get|/short-video/edit/subtitles/save|/short-video/edit/subtitles/export-srt')
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/edit/subtitles/static-subtitles'))
    expect(root).toHaveAttribute('data-no-mock-subtitle-fallback', 'true')
    expect(root).toHaveAttribute('data-video-id-valid', 'true')
    expect(screen.getByTestId('subtitle-editor-boundary-contract')).toHaveTextContent(/sv_subtitle_segment/)
    expect(screen.getByText(/保存会写入 `sv_subtitle_segment`/)).toBeInTheDocument()
    await waitFor(() => {
      expect(subtitleGet).toHaveBeenCalledWith(18)
    })
    expect(await screen.findByText('前三秒痛点')).toBeInTheDocument()

    fireEvent.click(screen.getByText('前三秒痛点'))
    fireEvent.change(screen.getByLabelText('字幕文字'), { target: { value: '前三秒直接说痛点' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(subtitleSave).toHaveBeenCalledWith(18, [
        expect.objectContaining({ id: 'seg-1', text: '前三秒直接说痛点' }),
      ])
    })
  })

  it('shows dirty-state diagnostics before saving', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await screen.findByText('前三秒痛点')
    expect(screen.getByText(/当前字幕有未保存更改|当前字幕已与后端数据同步/)).toBeInTheDocument()
    expect(screen.getByTestId('subtitle-editor-dirty-state')).toHaveAttribute('data-dirty', 'false')
    fireEvent.click(screen.getByText('前三秒痛点'))
    fireEvent.change(screen.getByLabelText('字幕文字'), { target: { value: '前三秒直接说痛点' } })
    expect(screen.getByText(/当前字幕有未保存更改/)).toBeInTheDocument()
    expect(screen.getByTestId('subtitle-editor-dirty-state')).toHaveAttribute('data-dirty', 'true')
  })

  it('keeps edited segments and shows endpoint when save fails', async () => {
    vi.mocked(subtitleSave).mockRejectedValueOnce(new Error('db locked') as never)
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await screen.findByText('前三秒痛点')
    fireEvent.click(screen.getByText('前三秒痛点'))
    fireEvent.change(screen.getByLabelText('字幕文字'), { target: { value: '前三秒直接说痛点' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/字幕保存失败（POST \/short-video\/edit\/subtitles\/save）：db locked/)).toBeInTheDocument()
    expect(screen.getByTestId('subtitle-editor-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByLabelText('字幕文字')).toHaveValue('前三秒直接说痛点')
  })

  it('shows export endpoint errors while keeping local subtitle edits', async () => {
    vi.mocked(subtitleExportSrt).mockRejectedValueOnce(new Error('export denied') as never)
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await screen.findByText('前三秒痛点')
    fireEvent.click(screen.getByText('前三秒痛点'))
    fireEvent.change(screen.getByLabelText('字幕文字'), { target: { value: '前三秒直接说痛点' } })
    fireEvent.click(screen.getByRole('button', { name: '导出 SRT' }))

    expect(await screen.findByText(/SRT 导出失败（POST \/short-video\/edit\/subtitles\/export-srt）：export denied/)).toBeInTheDocument()
    expect(screen.getByTestId('subtitle-editor-export-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByLabelText('字幕文字')).toHaveValue('前三秒直接说痛点')
  })

  it('uses backend SRT export when subtitles are already synchronized', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await screen.findByText('前三秒痛点')
    fireEvent.click(screen.getByRole('button', { name: '导出 SRT' }))

    await waitFor(() => {
      expect(subtitleExportSrt).toHaveBeenCalledWith(18, undefined)
    })
  })

  it('shows load endpoint error without mock subtitle fallback', async () => {
    vi.mocked(subtitleGet).mockRejectedValueOnce(new Error('subtitle table down') as never)
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/字幕加载失败（POST \/short-video\/edit\/subtitles\/get）：subtitle table down/)).toBeInTheDocument()
    expect(screen.getByTestId('subtitle-editor-load-error')).toHaveAttribute('data-no-mock-subtitle-fallback', 'true')
    expect(screen.queryByText('模拟字幕')).not.toBeInTheDocument()
  })

  it('disables backend operations and avoids NaN labels for invalid video id', () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/not-a-video']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByTestId('subtitle-editor-page')).toHaveAttribute('data-video-id-valid', 'false')
    expect(screen.getByTestId('subtitle-editor-invalid-video-id')).toHaveAttribute('data-no-placeholder-video', 'true')
    expect(screen.getByText(/未选择有效视频 · 0 条字幕/)).toBeInTheDocument()
    expect(screen.getByText(/当前 URL 未携带有效视频 ID/)).toBeInTheDocument()
    expect(screen.queryByText(/视频 #NaN/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '刷新' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '导出 SRT' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '保存' })).toBeDisabled()
    expect(subtitleGet).not.toHaveBeenCalled()
  })

  it('uses theme-aware selected segment surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
          <Routes>
            <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
          </Routes>
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(await screen.findByText('前三秒痛点'))
    expect(screen.getByTestId('subtitle-selected-segment')).toHaveStyle({
      backgroundColor: 'rgba(144, 202, 249, 0.18)',
    })
  })

  it('uses theme-aware timeline canvas colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
          <Routes>
            <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
          </Routes>
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(await screen.findByText('前三秒痛点'))
    const timeline = screen.getByTestId('subtitle-timeline-surface')
    expect(timeline).toHaveStyle({
      backgroundColor: 'rgba(255, 255, 255, 0.04)',
    })
    expect(timeline.getAttribute('data-grid-color')).not.toBe('#e0e0e0')
    expect(timeline.getAttribute('data-fill-colors')).not.toContain('#1976d2')
    expect(timeline.getAttribute('data-fill-colors')).not.toContain('#42a5f5')
    expect(timeline.getAttribute('data-fill-colors')).not.toContain('#90caf9')
    expect(timeline.getAttribute('data-stroke-colors')).not.toContain('#0d47a1')
  })

  it('marks media preview fallback colors as explicit subtitle rendering policy', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/subtitle-editor/18']}>
        <Routes>
          <Route path="/admin/shortvideo/subtitle-editor/:id" element={<SubtitleEditorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByText('前三秒痛点'))

    const swatch = screen.getByTestId('subtitle-color-swatch')
    const preview = screen.getByTestId('subtitle-media-preview-surface')
    const previewText = screen.getByTestId('subtitle-media-preview-text')

    expect(swatch).toHaveAttribute('data-preview-tone', 'media-subtitle-fallback')
    expect(swatch).toHaveAttribute('data-preview-color', '#ffffff')
    expect(preview).toHaveAttribute('data-preview-tone', 'media-preview')
    expect(preview).toHaveAttribute('data-preview-background', '#000000')
    expect(preview).toHaveAttribute('data-subtitle-color', '#ffffff')
    expect(previewText).toHaveAttribute('data-preview-tone', 'media-subtitle')
  })
})
