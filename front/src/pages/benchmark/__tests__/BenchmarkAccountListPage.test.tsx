import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor, within } from '@/test/utils'
import userEvent from '@testing-library/user-event'
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
          fanCount: 120000,
          videoCount: 320,
          avgLikeCount: 5800,
          category: '护肤,直播',
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
    const page = screen.getByTestId('benchmark-account-list-page')
    expect(page).toHaveAttribute('data-contract-scope', 'benchmark-account-server-collection')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/account/list'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/benchmark/account/search-by-keyword'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/benchmark/account/local-list'))
    expect(page).toHaveAttribute('data-no-local-account-fallback', 'true')
    expect(page).toHaveAttribute('data-server-pagination', 'true')

    await waitFor(() => {
      expect(benchmarkAccountApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('护肤竞品号')).toBeInTheDocument()
      expect(screen.getByText('护肤')).toBeInTheDocument()
    })
  })

  it('shows endpoint diagnostics when keyword search fails', async () => {
    const user = userEvent.setup()
    vi.mocked(benchmarkAccountApi.searchByKeyword).mockRejectedValueOnce(new Error('没有可用的Cookie'))

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkAccountListPage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤竞品号')

    await user.click(screen.getByRole('button', { name: '按关键词搜索' }))
    const dialog = await screen.findByRole('dialog', { name: '按关键词搜索账号' })
    await user.type(within(dialog).getByLabelText(/搜索关键词/), '护肤')
    await user.click(within(dialog).getByRole('button', { name: '开始搜索' }))

    expect(await screen.findByText(/\/benchmark\/account\/search-by-keyword/)).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-account-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('benchmark-account-action-error')).toHaveAttribute('data-no-local-account-mutation', 'true')
    expect(screen.getByText(/没有可用的Cookie/)).toBeInTheDocument()
  })

  it('submits backend fan fields when saving an account', async () => {
    const user = userEvent.setup()
    vi.mocked(benchmarkAccountApi.save).mockResolvedValue({ id: 2, accountName: '手动账号' } as never)

    renderWithProviders(
      <MemoryRouter>
        <BenchmarkAccountListPage />
      </MemoryRouter>,
    )

    await screen.findByText('护肤竞品号')

    await user.click(screen.getByRole('button', { name: '手动添加' }))
    const dialog = await screen.findByRole('dialog', { name: '添加账号' })
    await user.type(within(dialog).getByLabelText(/账号名称/), '手动账号')
    await user.type(within(dialog).getByLabelText(/账号URL/), 'https://www.douyin.com/user/test')
    await user.type(within(dialog).getByLabelText(/sec_uid/), 'MS4wLjABAAAAtest')
    await user.type(within(dialog).getByLabelText(/粉丝数/), '88000')
    await user.type(within(dialog).getByLabelText(/平均点赞数/), '5600')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(vi.mocked(benchmarkAccountApi.save).mock.calls[0]?.[0]).toEqual(expect.objectContaining({
        accountName: '手动账号',
        fanCount: 88000,
        avgLikeCount: 5600,
      }))
    })
  })
})
