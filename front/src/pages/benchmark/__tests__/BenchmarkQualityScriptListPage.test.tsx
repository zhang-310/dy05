import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import BenchmarkQualityScriptListPage from '../BenchmarkQualityScriptListPage'
import { benchmarkQualityScriptApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  benchmarkQualityScriptApi: {
    search: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('notistack', () => ({
  useSnackbar: () => ({ enqueueSnackbar: vi.fn() }),
}))

describe('BenchmarkQualityScriptListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(benchmarkQualityScriptApi.search).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          scriptContent: '护肤品直播话术',
          qualityScore: 92,
          scriptType: '产品介绍',
          industry: '护肤',
          sceneType: '直播',
          engagementRate: 15.2,
          viralScore: 88,
          referenceCount: 3,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 30,
    } as never)
  })

  it('renders title and loads quality scripts', async () => {
    renderWithProviders(
      <MemoryRouter>
        <BenchmarkQualityScriptListPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('质量脚本知识库')).toBeInTheDocument()

    await waitFor(() => {
      expect(benchmarkQualityScriptApi.search).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('护肤品直播话术')).toBeInTheDocument()
      expect(screen.getByText('产品介绍')).toBeInTheDocument()
    })
  })
})
