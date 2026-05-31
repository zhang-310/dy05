import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import BenchmarkQualityScriptListPage from '../BenchmarkQualityScriptListPage'
import { benchmarkQualityScriptApi } from '@/api/benchmark'

const navigate = vi.fn()

vi.mock('@/api/benchmark', () => ({
  benchmarkQualityScriptApi: {
    search: vi.fn(),
    delete: vi.fn(),
    calculateQualityScore: vi.fn(),
    save: vi.fn(),
  },
}))

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

describe('BenchmarkQualityScriptListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    navigate.mockClear()
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
    vi.mocked(benchmarkQualityScriptApi.calculateQualityScore).mockResolvedValue(87.25 as never)
    vi.mocked(benchmarkQualityScriptApi.save).mockResolvedValue({
      id: 99,
      videoId: 100,
      analysisId: 200,
      scriptContent: '新增脚本',
      qualityScore: 87.25,
      createTime: '2026-05-22 10:00:00',
      updateTime: '2026-05-22 10:00:00',
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
    expect(screen.getByTestId('benchmark-quality-script-list-workbench')).toHaveAttribute('data-contract-scope', 'benchmark-quality-script-library')
    expect(screen.getByTestId('benchmark-quality-script-list-workbench')).toHaveAttribute('data-no-local-quality-script-fallback', 'true')
    expect(screen.getByTestId('benchmark-quality-script-list-workbench').getAttribute('data-unsupported-endpoints')).toContain('/benchmark/script-similarity/generate-embedding')
    expect(screen.getByTestId('benchmark-quality-script-list-source-contract')).toHaveAttribute('data-no-local-score-calculation', 'true')
    expect(screen.getByTestId('benchmark-quality-script-grid-contract')).toHaveAttribute('data-row-count', '1')
  })

  it('creates a quality script through real save and score endpoints', async () => {
    renderWithProviders(
      <MemoryRouter>
        <BenchmarkQualityScriptListPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增脚本' }))
    fireEvent.change(screen.getByLabelText('视频 ID'), { target: { value: '100' } })
    fireEvent.change(screen.getByLabelText('分析 ID'), { target: { value: '200' } })
    fireEvent.change(screen.getByLabelText('脚本内容'), { target: { value: '新增脚本' } })
    fireEvent.change(screen.getByLabelText('互动率'), { target: { value: '12' } })
    fireEvent.change(screen.getByLabelText('传播力'), { target: { value: '90' } })

    fireEvent.click(screen.getByRole('button', { name: '计算评分' }))

    await waitFor(() => {
      expect(benchmarkQualityScriptApi.calculateQualityScore).toHaveBeenCalledWith(expect.objectContaining({
        videoId: 100,
        analysisId: 200,
        scriptContent: '新增脚本',
        engagementRate: 12,
        viralScore: 90,
      }))
    })

    await waitFor(() => {
      expect(screen.getByDisplayValue('87.25')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '保存并查看详情' }))

    await waitFor(() => {
      expect(benchmarkQualityScriptApi.save).toHaveBeenCalledWith(expect.objectContaining({
        videoId: 100,
        analysisId: 200,
        scriptContent: '新增脚本',
        qualityScore: 87.25,
      }))
      expect(navigate).toHaveBeenCalledWith('/admin/shortvideo/benchmark/quality-scripts/99')
    })
    expect(screen.getByTestId('benchmark-quality-script-form-contract')).toHaveAttribute('data-no-local-score-calculation', 'true')
  })

  it('keeps form input when score calculation or save fails', async () => {
    vi.mocked(benchmarkQualityScriptApi.calculateQualityScore).mockRejectedValueOnce(new Error('score down') as never)
    vi.mocked(benchmarkQualityScriptApi.save).mockRejectedValueOnce(new Error('save down') as never)

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkQualityScriptListPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增脚本' }))
    fireEvent.change(screen.getByLabelText('视频 ID'), { target: { value: '100' } })
    fireEvent.change(screen.getByLabelText('分析 ID'), { target: { value: '200' } })
    fireEvent.change(screen.getByLabelText('脚本内容'), { target: { value: '失败仍保留脚本' } })
    fireEvent.click(screen.getByRole('button', { name: '计算评分' }))

    expect(await screen.findByTestId('benchmark-quality-script-form-error')).toHaveAttribute('data-no-local-score-calculation', 'true')
    expect(screen.getByTestId('benchmark-quality-script-form-contract')).toHaveAttribute('data-script-length', '7')

    fireEvent.click(screen.getByRole('button', { name: '保存并查看详情' }))

    expect(await screen.findByText(/\/benchmark\/quality-script\/save 保存失败：save down/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-quality-script-form-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('失败仍保留脚本')).toBeInTheDocument()
  })

  it('keeps row context when delete fails and does not locally remove rows', async () => {
    vi.mocked(benchmarkQualityScriptApi.delete).mockRejectedValue(new Error('delete down') as never)

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkQualityScriptListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('护肤品直播话术')).toBeInTheDocument()
    fireEvent.click(screen.getByTitle('删除'))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByTestId('benchmark-quality-script-operation-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByTestId('benchmark-quality-script-operation-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByText('护肤品直播话术')).toBeInTheDocument()
  })

  it('shows list endpoint errors without local quality-script fallback', async () => {
    vi.mocked(benchmarkQualityScriptApi.search).mockRejectedValue(new Error('search down') as never)

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkQualityScriptListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('benchmark-quality-script-list-error')).toHaveAttribute('data-no-local-quality-script-fallback', 'true')
    expect(screen.getByTestId('benchmark-quality-script-list-workbench')).toHaveAttribute('data-list-error', 'true')
  })
})
