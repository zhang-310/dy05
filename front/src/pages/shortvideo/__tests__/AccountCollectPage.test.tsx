import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import AccountCollectPage from '../AccountCollectPage'
import { shortvideoApi, type AccountCollectTask } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    collectList: vi.fn(),
    collectStart: vi.fn(),
    collectCancel: vi.fn(),
    collectRetry: vi.fn(),
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

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, actionSlot, onPaginationModelChange }: any) => (
      <div>
        {actionSlot}
        <button type="button" onClick={() => onPaginationModelChange?.({ page: 1, pageSize: 30 })}>
          mock-next-page
        </button>
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({
                          row,
                          value: row[col.field],
                        })
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

const collectingTask: AccountCollectTask = {
  id: 1,
  svAccountId: 7,
  accountUrl: 'https://www.douyin.com/user/skin-lab',
  accountName: '护肤实验室',
  secUid: 'sec-7',
  inputType: 'account_url',
  originalInput: 'https://www.douyin.com/user/skin-lab',
  status: 'collecting',
  totalVideos: 10,
  collectedVideos: 4,
  analyzedVideos: 2,
  indexedVideos: 1,
  createTime: '2026-05-14 10:00:00',
}

const failedKeywordTask: AccountCollectTask = {
  id: 2,
  svAccountId: null,
  inputType: 'search_video',
  originalInput: '精华液测评',
  status: 'failed',
  totalVideos: 0,
  collectedVideos: 0,
  errorMessage: '验证码拦截',
  createTime: '2026-05-15 10:00:00',
}

const completedTask: AccountCollectTask = {
  id: 3,
  svAccountId: 8,
  accountName: '彩妆账号',
  inputType: 'douyin_id',
  originalInput: 'makeup-ops',
  status: 'completed',
  totalVideos: 5,
  collectedVideos: 5,
  analyzedVideos: 5,
  indexedVideos: 5,
  createTime: '2026-05-13 10:00:00',
}

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <AccountCollectPage />
    </MemoryRouter>,
  )
}

describe('AccountCollectPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.collectList).mockResolvedValue({
      total: 3,
      list: [collectingTask, failedKeywordTask, completedTask],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(shortvideoApi.collectStart).mockResolvedValue({ ...collectingTask, id: 9 } as never)
    vi.mocked(shortvideoApi.collectCancel).mockResolvedValue(undefined as never)
    vi.mocked(shortvideoApi.collectRetry).mockResolvedValue(undefined as never)
  })

  it('groups collection tasks by account and supports navigation actions', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: '账号视频采集' })).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-account-collect-page')).toHaveAttribute('data-ready-endpoints', '/short-video/account-collect/list|/short-video/account-collect/start|/short-video/account-collect/cancel|/short-video/account-collect/retry')
    expect(screen.getByTestId('shortvideo-account-collect-page')).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/accounts/:id?tab=analysis'))
    expect(screen.getByTestId('shortvideo-account-collect-page')).toHaveAttribute('data-supported-actions', expect.stringContaining('navigate-account-analysis'))
    expect(screen.getByTestId('shortvideo-account-collect-page')).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/account-collect/local-list'))
    expect(screen.getByTestId('account-collect-boundary-contract')).toHaveAttribute('data-no-frontend-browser-scrape', 'true')
    expect(screen.getByTestId('account-collect-summary')).toHaveAttribute('data-no-static-progress', 'true')
    expect(screen.getByTestId('account-collect-refresh-button')).toHaveAttribute('data-source-endpoint', '/short-video/account-collect/list')
    expect(shortvideoApi.collectList).toHaveBeenCalledWith({ page: 0, rows: 20 })

    expect(await screen.findByText('账号 · 护肤实验室')).toBeInTheDocument()
    expect(screen.getByText('账号 · 彩妆账号')).toBeInTheDocument()
    expect(screen.getByText('未关联抖音账号（关键词搜索等）')).toBeInTheDocument()
    expect(screen.getByText('4 / 10 · 40%')).toBeInTheDocument()
    expect(screen.getByText('精华液测评')).toBeInTheDocument()
    expect(screen.getByText('验证码拦截')).toBeInTheDocument()

    const accountSection = screen.getByText('账号 · 护肤实验室').closest('.MuiAccordion-root')
    expect(accountSection).not.toBeNull()

    fireEvent.click(within(accountSection as HTMLElement).getByRole('button', { name: '全部采集视频' }))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/accounts/7?tab=videos'))
    expect(within(accountSection as HTMLElement).getByTestId('account-collect-open-account-videos-button')).toHaveAttribute('data-target-route', expect.stringContaining('/shortvideo/accounts/7?tab=videos'))

    fireEvent.click(within(accountSection as HTMLElement).getByRole('button', { name: '综合分析' }))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/accounts/7?tab=analysis'))

    fireEvent.click(screen.getByRole('button', { name: '抖音账号库' }))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/accounts'))
  })

  it('creates keyword collection tasks and resets the dialog', async () => {
    renderPage()

    await screen.findByRole('heading', { name: '账号视频采集' })
    fireEvent.click(screen.getByRole('button', { name: '新建采集' }))

    expect(screen.getByRole('heading', { name: '新建账号采集任务' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '关键词（视频搜索）' }))
    fireEvent.change(screen.getByLabelText('搜索关键词'), {
      target: { value: '口红试色' },
    })
    fireEvent.change(screen.getByLabelText('期望最大条数（可选）'), {
      target: { value: '25' },
    })
    fireEvent.click(screen.getByRole('button', { name: '开始采集' }))

    await waitFor(() => {
      expect(shortvideoApi.collectStart).toHaveBeenCalledWith({
        input: '口红试色',
        maxCount: 25,
        collectMode: 'keyword_video',
      })
      expect(toast).toHaveBeenCalledWith('采集任务已创建', 'success')
    })
    expect(screen.getByTestId('account-collect-start-button')).toHaveAttribute('data-source-endpoint', '/short-video/account-collect/start')

    await waitFor(() => {
      expect(screen.queryByRole('heading', { name: '新建账号采集任务' })).not.toBeInTheDocument()
    })
  })

  it('keeps the create dialog open with endpoint error when start fails', async () => {
    vi.mocked(shortvideoApi.collectStart).mockRejectedValueOnce(new Error('Playwright 未启动') as never)
    renderPage()

    await screen.findByRole('heading', { name: '账号视频采集' })
    fireEvent.click(screen.getByRole('button', { name: '新建采集' }))
    fireEvent.change(screen.getByLabelText('链接或抖音号'), {
      target: { value: 'https://www.douyin.com/user/fail' },
    })
    fireEvent.click(screen.getByRole('button', { name: '开始采集' }))

    expect((await screen.findAllByText(/创建采集任务失败（POST \/short-video\/account-collect\/start）：Playwright 未启动/)).length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: '新建账号采集任务' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('https://www.douyin.com/user/fail')).toBeInTheDocument()
    expect(screen.getByTestId('account-collect-start-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('account-collect-operation-error')).toHaveAttribute('data-no-local-collect-mutation', 'true')
  })

  it('cancels active tasks through confirmation and retries failed tasks', async () => {
    renderPage()

    await screen.findByText('账号 · 护肤实验室')
    const accountSection = screen.getByText('账号 · 护肤实验室').closest('.MuiAccordion-root')
    expect(accountSection).not.toBeNull()
    fireEvent.click(within(accountSection as HTMLElement).getByRole('button', { name: '取消' }))

    expect(screen.getByText('确定取消该采集任务吗？进行中的任务将标记为失败。')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(shortvideoApi.collectCancel).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('已取消', 'success')
    })

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '确认操作' })).not.toBeInTheDocument()
    })

    const keywordSection = screen.getByText('未关联抖音账号（关键词搜索等）').closest('.MuiAccordion-root')
    expect(keywordSection).not.toBeNull()
    fireEvent.click(within(keywordSection as HTMLElement).getByRole('button', { name: '重试' }))
    await waitFor(() => {
      expect(shortvideoApi.collectRetry).toHaveBeenCalledWith(2)
      expect(toast).toHaveBeenCalledWith('已重新排队采集', 'success')
    })
  })

  it('keeps cancel and retry failures visible inline without removing rows', async () => {
    vi.mocked(shortvideoApi.collectCancel).mockRejectedValueOnce(new Error('任务已锁定') as never)
    vi.mocked(shortvideoApi.collectRetry).mockRejectedValueOnce(new Error('队列不可用') as never)
    renderPage()

    await screen.findByText('账号 · 护肤实验室')
    const accountSection = screen.getByText('账号 · 护肤实验室').closest('.MuiAccordion-root')
    fireEvent.click(within(accountSection as HTMLElement).getByRole('button', { name: '取消' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/取消采集任务失败（POST \/short-video\/account-collect\/cancel）：任务已锁定/)).toBeInTheDocument()
    expect(screen.getByText(/上次取消失败（POST \/short-video\/account-collect\/cancel）：任务已锁定/)).toBeInTheDocument()
    expect(screen.getByText('账号 · 护肤实验室')).toBeInTheDocument()
    expect(screen.getByTestId('account-collect-operation-error')).toHaveAttribute('data-source-endpoints', '/short-video/account-collect/start|/short-video/account-collect/cancel|/short-video/account-collect/retry')

    const keywordSection = screen.getByText('未关联抖音账号（关键词搜索等）').closest('.MuiAccordion-root')
    fireEvent.click(within(keywordSection as HTMLElement).getByText('重试').closest('button') as HTMLButtonElement)
    expect(await screen.findByText(/重试采集任务失败（POST \/short-video\/account-collect\/retry）：队列不可用/)).toBeInTheDocument()
    expect(screen.getByText('验证码拦截')).toBeInTheDocument()
  })

  it('renders flat task view with table actions and pagination', async () => {
    renderPage()

    await screen.findByText('账号 · 护肤实验室')
    fireEvent.click(screen.getByRole('button', { name: '全部任务' }))

    expect(screen.getByTestId('account-collect-flat-grid')).toHaveAttribute('data-server-pagination', 'true')
    expect(screen.getByText('护肤实验室')).toBeInTheDocument()
    expect(screen.getByText('主页链接')).toBeInTheDocument()
    expect(screen.getByText('采集中')).toBeInTheDocument()
    expect(screen.getByText('已完成')).toBeInTheDocument()

    const accountButtons = screen.getAllByTestId('AccountCircleIcon')
    fireEvent.click(accountButtons[0].closest('button') as HTMLButtonElement)
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/accounts/7?tab=videos'))

    fireEvent.click(screen.getByRole('button', { name: 'mock-next-page' }))

    await waitFor(() => {
      expect(shortvideoApi.collectList).toHaveBeenLastCalledWith({ page: 1, rows: 30 })
    })
  })

  it('shows the empty state when no tasks exist', async () => {
    vi.mocked(shortvideoApi.collectList).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderPage()

    expect(await screen.findByText('当前页暂无采集任务')).toBeInTheDocument()
    expect(screen.getByTestId('account-collect-empty')).toHaveAttribute('data-no-local-collect-fallback', 'true')
    expect(screen.getByText('共 0 条 · 每页 20 条')).toBeInTheDocument()
  })

  it('shows list load failures inline', async () => {
    vi.mocked(shortvideoApi.collectList).mockRejectedValueOnce(new Error('采集表不可用') as never)

    renderPage()

    expect(await screen.findByText(/采集任务加载失败（POST \/short-video\/account-collect\/list）：采集表不可用/)).toBeInTheDocument()
    expect(screen.getByTestId('account-collect-list-error')).toHaveAttribute('data-no-local-collect-fallback', 'true')
  })
})
