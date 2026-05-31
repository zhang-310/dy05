import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import userEvent from '@testing-library/user-event'
import BenchmarkVideoListPage from '../BenchmarkVideoListPage'
import { benchmarkAnalysisApi, benchmarkVideoApi } from '@/api/benchmark'

vi.mock('@/api/benchmark', () => ({
  benchmarkVideoApi: {
    list: vi.fn(),
    collect: vi.fn(),
    delete: vi.fn(),
  },
  benchmarkAnalysisApi: {
    analyze: vi.fn(),
  },
}))

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('notistack', () => ({
  useSnackbar: () => ({ enqueueSnackbar: vi.fn() }),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, loading }: any) => (
      <div>
        {loading && <span>loading</span>}
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({ row, value: row[col.field] })
                      : String(row[col.field] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  }
})

describe('BenchmarkVideoListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(benchmarkVideoApi.list).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 11,
          videoUrl: 'https://www.douyin.com/video/11',
          title: '护肤爆款视频',
          likeCount: 24000,
          commentCount: 800,
          shareCount: 120,
          analysisStatus: 'completed',
          isQualified: true,
          bosVideoUrl: 'https://cdn.test/video.mp4',
        },
        {
          id: 12,
          videoUrl: 'https://www.douyin.com/video/12',
          title: '待分析视频',
          analysisStatus: 'pending',
        },
      ],
      pageNum: 0,
      pageSize: 30,
    } as never)
    vi.mocked(benchmarkVideoApi.collect).mockResolvedValue([] as never)
    vi.mocked(benchmarkVideoApi.delete).mockResolvedValue(undefined as never)
    vi.mocked(benchmarkAnalysisApi.analyze).mockResolvedValue({ id: 1 } as never)
  })

  it('loads benchmark videos and shows analysis diagnostics', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/videos?accountId=7']}>
        <BenchmarkVideoListPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '对标视频管理' })).toBeInTheDocument()
    const page = screen.getByTestId('benchmark-video-list-page')
    expect(page).toHaveAttribute('data-contract-scope', 'benchmark-video-server-collect-analysis')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/video/list'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/analysis/analyze'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/benchmark/video/local-list'))
    expect(page).toHaveAttribute('data-no-static-analysis-fallback', 'true')
    expect(page).toHaveAttribute('data-server-pagination', 'true')

    await waitFor(() => {
      expect(benchmarkVideoApi.list).toHaveBeenCalledWith(expect.objectContaining({ benchmarkAccountId: 7 }))
    })

    expect(await screen.findByText('护肤爆款视频')).toBeInTheDocument()
    expect(screen.getByText('已完成分析')).toBeInTheDocument()
    expect(screen.getAllByText('待分析').length).toBeGreaterThan(0)
    expect(screen.getByText('已落地/BOS')).toBeInTheDocument()
  })

  it('submits collect cookie id and shows collect endpoint errors', async () => {
    const user = userEvent.setup()
    vi.mocked(benchmarkVideoApi.collect).mockRejectedValueOnce(new Error('Playwright 不可用'))

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/videos?accountId=7']}>
        <BenchmarkVideoListPage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤爆款视频')
    await user.click(screen.getByRole('button', { name: '采集视频' }))
    await user.type(screen.getByLabelText('指定 Cookie ID（可选）'), '3')
    await user.click(screen.getByRole('button', { name: '开始采集' }))

    await waitFor(() => {
      expect(vi.mocked(benchmarkVideoApi.collect).mock.calls[0]?.[0]).toEqual(expect.objectContaining({
        benchmarkAccountId: 7,
        minLikeCount: 1000,
        maxVideos: 50,
        cookieId: 3,
      }))
    })
    expect(await screen.findByText(/\/benchmark\/video\/collect/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-video-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('benchmark-video-action-error')).toHaveAttribute('data-no-local-video-mutation', 'true')
    expect(screen.getByText(/Playwright 不可用/)).toBeInTheDocument()
  })

  it('submits analysis cookie id and shows analysis endpoint errors', async () => {
    const user = userEvent.setup()
    vi.mocked(benchmarkAnalysisApi.analyze).mockRejectedValueOnce(new Error('下载视频失败'))

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/benchmark/videos?accountId=7']}>
        <BenchmarkVideoListPage />
      </MemoryRouter>,
    )

    await screen.findByText('待分析视频')
    await user.click(screen.getAllByRole('button', { name: '分析' })[0])
    await user.type(screen.getByLabelText('指定 Cookie ID（可选）'), '4')
    await user.click(screen.getByRole('button', { name: '提交分析' }))

    await waitFor(() => {
      expect(vi.mocked(benchmarkAnalysisApi.analyze).mock.calls[0]?.[0]).toEqual(expect.objectContaining({
        benchmarkVideoId: 12,
        cookieId: 4,
      }))
    })
    expect(await screen.findByText(/\/benchmark\/analysis\/analyze/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-video-action-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByText(/下载视频失败/)).toBeInTheDocument()
  })
})
