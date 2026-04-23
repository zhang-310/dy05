import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ProjectsPage from '../ProjectsPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ProjectsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          title: '爆款切片项目',
          projectType: 'viral_clone',
          status: 'processing',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads shortvideo projects and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('短视频项目')).toBeInTheDocument()

    await waitFor(() => {
      expect(shortvideoApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, title: '', status: undefined })
    })

    await waitFor(() => {
      expect(screen.getByText('爆款切片项目')).toBeInTheDocument()
      expect(screen.getByText('进行中')).toBeInTheDocument()
    })
  })
})
