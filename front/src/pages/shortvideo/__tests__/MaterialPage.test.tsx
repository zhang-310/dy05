import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import MaterialPage from '../MaterialPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    materialList: vi.fn(),
    materialDelete: vi.fn(),
    materialUploadBase: '/api/v1/short-video/upload/final-video',
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('MaterialPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(shortvideoApi.materialList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 3,
          projectId: 7,
          url: 'https://cdn.test/final.mp4',
          materialType: 'video/mp4',
          duration: 30,
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('renders material diagnostics and backend library list data', async () => {
    renderWithProviders(
      <MemoryRouter>
        <MaterialPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '素材库' })).toBeInTheDocument()
    expect(screen.getByText(/成片上传走/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-material-page')).toHaveAttribute('data-ready-endpoints', '/short-video/library/list|/short-video/library/delete|/short-video/upload/final-video')
    expect(screen.getByTestId('shortvideo-material-page')).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/library/local-list'))
    expect(screen.getByTestId('shortvideo-material-boundary-contract')).toHaveAttribute('data-no-local-material-fallback', 'true')

    await waitFor(() => {
      expect(shortvideoApi.materialList).toHaveBeenCalledWith({
        materialType: undefined,
        projectId: undefined,
        page: 0,
        rows: 20,
      })
    })

    expect(await screen.findByText('final.mp4')).toBeInTheDocument()
    expect(screen.getByText('Video')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-material-grid')).toHaveAttribute('data-source-endpoint', '/short-video/library/list')
    expect(screen.getByTestId('shortvideo-material-card')).toHaveAttribute('data-material-id', '3')
  })

  it('keeps project-bound upload and filter diagnostics visible', async () => {
    renderWithProviders(
      <MemoryRouter>
        <MaterialPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('项目 ID（筛选 / 上传必填）'), { target: { value: '7' } })

    expect(await screen.findByText(/当前筛选\/上传项目 ID：7/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-material-project-context')).toHaveAttribute('data-upload-endpoint', '/short-video/upload/final-video')
    expect(screen.getByTestId('shortvideo-material-filter')).toHaveAttribute('data-input-retained', 'true')
  })

  it('shows final-video upload business errors and keeps project context', async () => {
    vi.mocked(globalThis.fetch as any).mockResolvedValueOnce(new Response(JSON.stringify({
      status: 7001,
      message: '请先在系统配置中配置 BOS',
      data: null,
    }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    }))

    renderWithProviders(
      <MemoryRouter>
        <MaterialPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('项目 ID（筛选 / 上传必填）'), { target: { value: '7' } })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['video'], 'final.mp4', { type: 'video/mp4' })
    fireEvent.change(input, { target: { files: [file] } })

    expect(await screen.findByText(/成片上传失败（POST \/short-video\/upload\/final-video）：请先在系统配置中配置 BOS/)).toBeInTheDocument()
    expect(screen.getByText(/项目 ID 与已加载素材列表会保留/)).toBeInTheDocument()
    expect(screen.getByText(/当前筛选\/上传项目 ID：7/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-material-upload-error')).toHaveAttribute('data-no-local-upload-fallback', 'true')
    expect(screen.getByTestId('shortvideo-material-upload-error')).toHaveAttribute('data-input-retained', 'true')
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/short-video/upload/final-video?projectId=7',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('keeps material rows when delete fails', async () => {
    vi.mocked(shortvideoApi.materialDelete).mockRejectedValueOnce(new Error('素材被项目引用') as never)

    renderWithProviders(
      <MemoryRouter>
        <MaterialPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('final.mp4')).toBeInTheDocument()
    fireEvent.click(screen.getByTestId('DeleteIcon').closest('button') as HTMLButtonElement)

    expect(await screen.findByText(/素材删除失败（POST \/short-video\/library\/delete）：素材被项目引用/)).toBeInTheDocument()
    expect(screen.getByText(/素材行会保留/)).toBeInTheDocument()
    expect(screen.getByText('final.mp4')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-material-delete-error')).toHaveAttribute('data-no-local-material-mutation', 'true')
  })

  it('shows material list endpoint and does not render local fallback materials when list fails', async () => {
    vi.mocked(shortvideoApi.materialList).mockRejectedValueOnce(new Error('library table down') as never)

    renderWithProviders(
      <MemoryRouter>
        <MaterialPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/素材列表加载失败（POST \/short-video\/library\/list）：library table down/)).toBeInTheDocument()
    expect(screen.getByText(/页面不会展示本地素材/)).toBeInTheDocument()
    expect(screen.queryByText('final.mp4')).not.toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-material-list-error')).toHaveAttribute('data-no-local-material-fallback', 'true')
  })

  it('uses theme-aware preview and delete surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <MaterialPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    expect(await screen.findByText('final.mp4')).toBeInTheDocument()
    expect(screen.getByTestId('material-preview-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    expect(screen.getByTestId('material-delete-button')).not.toHaveStyle({
      backgroundColor: 'rgba(255, 255, 255, 0.85)',
    })
  })
})
