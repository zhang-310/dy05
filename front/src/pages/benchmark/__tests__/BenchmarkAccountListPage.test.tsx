import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import BenchmarkAccountListPage from '../BenchmarkAccountListPage'
import { benchmarkAccountApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  benchmarkAccountApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    searchByKeyword: vi.fn(),
    analyzeByUrl: vi.fn(),
  },
}))

vi.mock('notistack', () => ({
  useSnackbar: () => ({ enqueueSnackbar: vi.fn() }),
}))

describe('BenchmarkAccountListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(benchmarkAccountApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          accountName: '护肤竞品号',
          followerCount: 120000,
          videoCount: 320,
          likeCount: 890000,
          tags: '护肤,直播',
          lastCollectTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 30,
    } as never)
  })

  it('loads benchmark accounts and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <BenchmarkAccountListPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '对标账号管理' })).toBeInTheDocument()

    await waitFor(() => {
      expect(benchmarkAccountApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('护肤竞品号')).toBeInTheDocument()
      expect(screen.getByText('护肤')).toBeInTheDocument()
    })
  })
})
