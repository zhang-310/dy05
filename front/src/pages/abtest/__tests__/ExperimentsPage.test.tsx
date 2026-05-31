import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ExperimentsPage from '../ExperimentsPage'
import { abtestApi } from '@/api/abtest'

vi.mock('@/api/abtest', () => ({
  abtestApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    start: vi.fn(),
    pause: vi.fn(),
    stop: vi.fn(),
    updateStatus: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ExperimentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(abtestApi.list).mockResolvedValue({
      total: 1,
      records: [
        {
          id: 1,
          name: '直播开场 A/B',
          experimentName: '直播开场 A/B',
          description: '测试两种开场风格',
          status: 1,
          experimentType: 'script_style',
          targetEntityType: 'live_session',
          targetEntityId: 18,
          trafficSplit: 50,
          winnerVariantId: null,
          startTime: '2026-04-10 10:00:00',
          endTime: '',
          createTime: '2026-04-10 09:00:00',
          variants: {
            items: [
              { id: 1, experimentId: 1, variantName: 'A', trafficRatio: 50, scriptStyle: 'warm', conversions: 12, exposures: 300, conversionRate: 0.04, isWinner: false },
              { id: 2, experimentId: 1, variantName: 'B', trafficRatio: 50, scriptStyle: 'direct', conversions: 18, exposures: 300, conversionRate: 0.06, isWinner: false },
            ],
          },
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads wrapped experiment list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExperimentsPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'A/B 实验管理' })).toBeInTheDocument()
    const root = screen.getByTestId('abtest-experiments-page')
    expect(root).toHaveAttribute('data-ready-endpoints', '/abtest/experiment/list|/abtest/experiment/save|/abtest/experiment/delete|/abtest/experiment/update-status')
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/abtest/experiment/segment-analysis'))
    expect(root).toHaveAttribute('data-no-local-experiment-fallback', 'true')

    await waitFor(() => {
      expect(abtestApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, keyword: undefined, status: undefined })
    })

    await waitFor(() => {
      expect(screen.getByText('直播开场 A/B')).toBeInTheDocument()
      expect(screen.getByText('进行中')).toBeInTheDocument()
      expect(screen.getByText('script_style')).toBeInTheDocument()
    })
    expect(screen.getByTestId('abtest-experiments-grid')).toHaveAttribute('data-source-endpoint', '/abtest/experiment/list')
  })

  it('saves experiment wizard with real backend fields and variants', async () => {
    vi.mocked(abtestApi.save).mockResolvedValue(8 as never)

    renderWithProviders(
      <MemoryRouter>
        <ExperimentsPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '新建实验' }))
    fireEvent.change(screen.getByLabelText('实验名称 *'), { target: { value: '新话术风格实验' } })
    fireEvent.change(screen.getByLabelText('目标实体 ID'), { target: { value: '18' } })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))

    await waitFor(() => expect(screen.getAllByLabelText('风格编码')).toHaveLength(2))
    fireEvent.change(screen.getAllByLabelText('风格编码')[0], { target: { value: 'warm' } })
    fireEvent.change(screen.getAllByLabelText('风格编码')[1], { target: { value: 'direct' } })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    fireEvent.click(screen.getByRole('button', { name: '保存实验' }))

    await waitFor(() => {
      expect(abtestApi.save).toHaveBeenCalledWith(expect.objectContaining({
        name: '新话术风格实验',
        experimentType: 'script_style',
        targetEntityType: 'live_session',
        targetEntityId: 18,
        status: 0,
        variants: [
          expect.objectContaining({ variantName: '对照组 A', variantType: 'A', styleCode: 'warm' }),
          expect.objectContaining({ variantName: '实验组 B', variantType: 'B', styleCode: 'direct' }),
        ],
      }))
    })
  })

  it('shows source endpoint when status update or delete fails', async () => {
    vi.mocked(abtestApi.pause).mockRejectedValue(new Error('status down') as never)
    vi.mocked(abtestApi.list).mockResolvedValueOnce({
      total: 2,
      records: [
        {
          id: 1,
          name: '运行实验',
          status: 1,
          experimentType: 'script_style',
          variants: [],
        },
        {
          id: 2,
          name: '草稿实验',
          status: 0,
          experimentType: 'copy',
          variants: [],
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ExperimentsPage />
      </MemoryRouter>,
    )

    await screen.findByText('运行实验')
    fireEvent.click(screen.getByRole('button', { name: '暂停 运行实验' }))

    expect(await screen.findByText(/暂停实验失败（POST \/abtest\/experiment\/update-status）：status down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-experiments-action-error')).toHaveAttribute('data-no-local-experiment-mutation', 'true')
    expect(screen.getByText(/targetStatus=3\/已暂停; experimentId=1; experimentName=运行实验/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/ai\/abtest\/experiments; keyword=空; statusTab=全部/)).toBeInTheDocument()
    expect(screen.getByText('运行实验')).toBeInTheDocument()

    vi.mocked(abtestApi.delete).mockRejectedValue(new Error('delete down') as never)
    fireEvent.click(screen.getByRole('button', { name: '删除 草稿实验' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    expect(await screen.findByText(/删除实验失败（POST \/abtest\/experiment\/delete）：delete down/)).toBeInTheDocument()
    expect(screen.getAllByText(/experimentId=2; experimentName=草稿实验/).length).toBeGreaterThan(0)
    expect(screen.getByText('草稿实验')).toBeInTheDocument()
  })

  it('shows source endpoint when experiment creation fails and keeps wizard input', async () => {
    vi.mocked(abtestApi.save).mockRejectedValue(new Error('save down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ExperimentsPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '新建实验' }))
    fireEvent.change(screen.getByLabelText('实验名称 *'), { target: { value: '失败实验' } })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    fireEvent.click(screen.getByRole('button', { name: '保存实验' }))

    expect(await screen.findByText(/创建实验失败（POST \/abtest\/experiment\/save）：save down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-experiment-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/experimentName=失败实验; experimentType=script_style/)).toBeInTheDocument()
    expect(screen.getByText(/variants=A:对照组 A,B:实验组 B/)).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '新建实验' })).toBeInTheDocument()
    expect(screen.getByText('失败实验')).toBeInTheDocument()
  })

  it('shows list endpoint when experiment list fails', async () => {
    vi.mocked(abtestApi.list).mockRejectedValueOnce(new Error('list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ExperimentsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/实验列表加载失败（POST \/abtest\/experiment\/list）：list down/)).toBeInTheDocument()
    expect(screen.getByTestId('abtest-experiments-list-error')).toHaveAttribute('data-no-local-experiment-fallback', 'true')
    expect(screen.getByText(/route=\/admin\/ai\/abtest\/experiments; keyword=空; statusTab=全部; page=0; rows=20/)).toBeInTheDocument()
  })

  it('uses a theme-aware sample size result surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <ExperimentsPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: '新建实验' }))
    fireEvent.click(screen.getByText('样本量计算器'))

    expect(await screen.findByTestId('abtest-sample-size-result-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(227, 242, 253)',
    })
  })
})
