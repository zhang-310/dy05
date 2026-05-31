import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import AccountsPage from '../AccountsPage'
import { douyinApi, type DyAccount } from '@/api/douyin'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    accountList: vi.fn(),
    accountSave: vi.fn(),
    accountDelete: vi.fn(),
    videoSync: vi.fn(),
    personaGetByAccount: vi.fn(),
    personaSave: vi.fn(),
    personaDelete: vi.fn(),
    fanProfileGet: vi.fn(),
    fanProfileStats: vi.fn(),
    fanProfileSync: vi.fn(),
    videoSearch: vi.fn(),
    tokenStatus: vi.fn(),
    tokenRefresh: vi.fn(),
    oauthUrl: vi.fn(),
    oauthRevoke: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({
      rows,
      columns,
      searchSlot,
      actionSlot,
      onPaginationModelChange,
    }: any) => (
      <div>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
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
                          field: col.field,
                        })
                      : col.valueFormatter
                        ? col.valueFormatter(row[col.field])
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

const openSpy = vi.fn()
const writeText = vi.fn()
const createObjectUrl = vi.fn(() => 'blob:oauth-report')
const revokeObjectUrl = vi.fn()
const anchorClick = vi.fn()

const validAccount: DyAccount = {
  id: 1,
  userId: 1,
  accountName: '美妆达人号',
  accountId: 'beauty001',
  followCount: 12,
  fanCount: 125000,
  videoCount: 18,
  totalLikes: 320000,
  description: '专注敏感肌修护',
  status: 1,
  createTime: '2026-05-01 10:00:00',
  updateTime: '2026-05-14 10:00:00',
  authStatus: 'valid',
  lastSyncTime: '2026-05-14 10:00:00',
}

const expiredAccount: DyAccount = {
  id: 2,
  userId: 1,
  accountName: '过期账号',
  accountId: 'old002',
  followCount: 0,
  fanCount: 9000,
  videoCount: 0,
  totalLikes: 1200,
  description: '',
  status: 1,
  createTime: '2026-04-01 10:00:00',
  updateTime: '2026-05-10 10:00:00',
  authStatus: 'expired',
}

const unboundAccount: DyAccount = {
  id: 3,
  userId: 1,
  accountName: '未授权账号',
  accountId: 'new003',
  followCount: 0,
  fanCount: 1200,
  videoCount: 2,
  totalLikes: 300,
  description: '待授权测试号',
  status: 1,
  createTime: '2026-05-03 10:00:00',
  updateTime: '2026-05-11 10:00:00',
  authStatus: 'unbound',
}

const accounts = [validAccount, expiredAccount, unboundAccount]

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <AccountsPage />
    </MemoryRouter>,
  )
}

function mockAccountList() {
  vi.mocked(douyinApi.accountList).mockImplementation(async (params: any) => {
    const filtered = accounts.filter((account) => {
      const matchName = !params?.accountName || account.accountName.includes(params.accountName)
      const matchAccountId = !params?.accountId || account.accountId.includes(params.accountId)
      return matchName && matchAccountId
    })
    return {
      total: filtered.length,
      list: filtered,
      pageNum: params?.page ?? 0,
      pageSize: params?.rows ?? 20,
    } as never
  })
}

describe('AccountsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    mockAccountList()
    vi.mocked(douyinApi.accountSave).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.accountDelete).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.videoSync).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.personaGetByAccount).mockResolvedValue({
      id: 11,
      accountId: 1,
      personaName: '敏感肌顾问',
      personaType: '专家型',
      description: '专业护肤建议',
      tone: '温和专业',
      targetAudience: '敏感肌用户',
      contentStyle: '知识科普',
      keywords: '修护,屏障',
      isDefault: 1,
      status: 1,
      interactionStyle: '',
      languageStyle: '',
      contentRatio: '',
      localFlavor: '',
      liveStyle: '',
      personaTraits: '',
      ipType: '',
      ageRange: '',
      positioningTags: '',
      createTime: '2026-05-01 10:00:00',
      updateTime: '2026-05-10 10:00:00',
    } as never)
    vi.mocked(douyinApi.personaSave).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.personaDelete).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.fanProfileGet).mockResolvedValue({
      id: 21,
      accountId: 1,
      accountName: '美妆达人号',
      totalFans: 125000,
      syncTime: 1770000000000,
      genderDistribution: [
        { key: 'female', value: '女', percentage: 80 },
        { key: 'male', value: '男', percentage: 20 },
      ],
      ageDistribution: [
        { key: '25-34', value: '25-34岁', percentage: 46 },
        { key: '35-44', value: '35-44岁', percentage: 30 },
      ],
      provinceDistribution: [
        { key: 'GD', value: '广东', percentage: 31 },
        { key: 'SH', value: '上海', percentage: 18 },
      ],
      interestTags: [
        { key: '护肤', value: '护肤', percentage: 52 },
      ],
    } as never)
    vi.mocked(douyinApi.fanProfileStats).mockResolvedValue([
      { statType: 'device', statKey: 'ios', statValue: 'iOS', percentage: 62 },
      { statType: 'active_time', statKey: '20:00', statValue: '20:00-22:00', percentage: 41 },
    ] as never)
    vi.mocked(douyinApi.fanProfileSync).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.videoSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 31,
          accountId: 1,
          videoId: 'v-31',
          title: '屏障修护实测',
          description: '实测视频',
          viewCount: 160000,
          likeCount: 68000,
          shareCount: 1200,
          commentCount: 900,
          downloadCount: 0,
          videoType: 'normal',
          publishTime: '2026-05-12 10:00:00',
          createTime: '2026-05-12 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 10,
    } as never)
    vi.mocked(douyinApi.tokenStatus).mockResolvedValue({
      status: 'valid',
      expireTime: '2026-06-01 10:00:00',
      daysLeft: 17,
    } as never)
    vi.mocked(douyinApi.tokenRefresh).mockResolvedValue(undefined as never)
    vi.mocked(douyinApi.oauthUrl).mockResolvedValue({ authUrl: 'https://douyin.example/oauth' } as never)
    vi.mocked(douyinApi.oauthRevoke).mockResolvedValue(undefined as never)

    Object.defineProperty(window, 'open', { value: openSpy, writable: true })
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectUrl, writable: true })
    Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectUrl, writable: true })
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(anchorClick)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
    })
  })

  it('loads account list, filters rows, and supports account actions', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: '抖音账号' })).toBeInTheDocument()
    const page = screen.getByTestId('douyin-accounts-page')
    expect(page).toHaveAttribute('data-contract-scope', 'douyin-account-oauth-persona-fan-profile-workbench')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/douyin/account/search'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/douyin/oauth/token-refresh'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/douyin/account/local-list'))
    expect(page).toHaveAttribute('data-no-local-account-fallback', 'true')
    const listTab = screen.getByTestId('douyin-account-list-tab')
    expect(listTab).toHaveAttribute('data-contract-scope', 'douyin-account-list-server-pagination')
    expect(listTab).toHaveAttribute('data-server-pagination', 'true')
    expect(listTab).toHaveAttribute('data-no-static-account-fallback', 'true')
    expect(await screen.findByText('美妆达人号')).toBeInTheDocument()
    expect(screen.getByText('12.5万')).toBeInTheDocument()
    expect(screen.getAllByText('Token过期').length).toBeGreaterThan(0)

    fireEvent.change(screen.getByPlaceholderText('搜索账号名称'), {
      target: { value: '美妆' },
    })
    await waitFor(() => {
      expect(douyinApi.accountList).toHaveBeenLastCalledWith({
        accountName: '美妆',
        accountId: undefined,
        page: 0,
        rows: 20,
      })
    })

    fireEvent.change(screen.getByPlaceholderText('搜索抖音号'), {
      target: { value: 'beauty001' },
    })
    await waitFor(() => {
      expect(douyinApi.accountList).toHaveBeenLastCalledWith({
        accountName: '美妆',
        accountId: 'beauty001',
        page: 0,
        rows: 20,
      })
    })

    fireEvent.click(screen.getByRole('button', { name: '已授权' }))
    await waitFor(() => {
      expect(douyinApi.accountList).toHaveBeenLastCalledWith({
        accountName: '美妆',
        accountId: 'beauty001',
        page: 0,
        rows: 20,
      })
    })
    expect(screen.getByText(/授权状态由前端按当前页本地筛选/)).toBeInTheDocument()
    expect(screen.queryByText('过期账号')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'mock-next-page' }))
    await waitFor(() => {
      expect(douyinApi.accountList).toHaveBeenLastCalledWith({
        accountName: '美妆',
        accountId: 'beauty001',
        page: 1,
        rows: 30,
      })
    })

    await waitFor(() => expect(screen.getByRole('button', { name: '批量同步' })).not.toBeDisabled())
    fireEvent.click(screen.getByRole('button', { name: '批量同步' }))
    await waitFor(() => {
      expect(douyinApi.videoSync).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('批量同步完成：成功 1，失败 0', 'success')
    })
    expect(screen.getByText(/当前页批量同步结果：共 1 个账号，成功 1 个，失败 0 个/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '全部' }))
    await waitFor(() => {
      expect(screen.getByText('美妆达人号')).toBeInTheDocument()
    })

    fireEvent.click(screen.getAllByRole('button', { name: '同步' })[0])
    await waitFor(() => {
      expect(douyinApi.videoSync).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('同步完成', 'success')
    })

    fireEvent.click(screen.getAllByRole('button', { name: '删除' })[0])
    expect(screen.getByRole('dialog', { name: '确认删除' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(douyinApi.accountDelete).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('删除成功', 'success')
    })
  })

  it('opens account detail drawer and exercises detail tabs', async () => {
    renderPage()

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getAllByRole('button', { name: '详情' })[0])

    expect(await screen.findByText('@beauty001 | 粉丝 12.5万')).toBeInTheDocument()
    expect(screen.getByTestId('douyin-account-detail-contract')).toHaveAttribute('data-no-static-fan-profile-fallback', 'true')
    expect(screen.getByTestId('douyin-account-detail-contract')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/douyin/fan-profile/stats'))
    expect(await screen.findByText('健康评分 100/100')).toBeInTheDocument()
    expect(screen.getByText('健康评分依据')).toBeInTheDocument()
    expect(screen.getByText(/粉丝画像同步状态需要进入“粉丝画像”页读取/)).toBeInTheDocument()
    expect(screen.getByText('专注敏感肌修护')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '人设配置' }))
    expect(await screen.findByText('敏感肌顾问')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    expect(screen.getByRole('heading', { name: '编辑人设' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('话术风格'), {
      target: { value: '真诚克制' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    await waitFor(() => {
      expect(douyinApi.personaSave).toHaveBeenCalledWith(
        expect.objectContaining({ id: 11, tone: '真诚克制' }),
      )
      expect(toast).toHaveBeenCalledWith('人设保存成功', 'success')
    })

    fireEvent.click(screen.getByRole('tab', { name: '粉丝画像' }))
    expect(await screen.findByText('性别分布')).toBeInTheDocument()
    expect(screen.getByText('省份 TOP10')).toBeInTheDocument()
    expect(screen.getByText('兴趣标签 TOP10')).toBeInTheDocument()
    expect(screen.getByText('活跃时段')).toBeInTheDocument()
    expect(screen.getByText('设备分布')).toBeInTheDocument()
    expect(screen.getByText(/消费力和趋势未由后端返回时不再使用静态图表补齐/)).toBeInTheDocument()
    expect(screen.queryByText('消费力分布')).not.toBeInTheDocument()
    expect(screen.queryByText(/近3个月/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '同步画像' }))
    await waitFor(() => {
      expect(douyinApi.fanProfileSync).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('粉丝画像同步完成', 'success')
    })

    fireEvent.click(screen.getByRole('tab', { name: '近期视频' }))
    expect(await screen.findByText('屏障修护实测')).toBeInTheDocument()
    expect(screen.getByText('爆款')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: 'Token管理' }))
    expect(await screen.findByText('剩余天数：17 天')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '刷新 Token' }))
    await waitFor(() => {
      expect(douyinApi.tokenRefresh).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('Token 刷新成功', 'success')
    })
    fireEvent.click(screen.getByRole('button', { name: '重新授权' }))
    await waitFor(() => {
      expect(douyinApi.oauthUrl).toHaveBeenCalledWith(1)
      expect(openSpy).toHaveBeenCalledWith('https://douyin.example/oauth', '_blank')
    })
  })

  it('uses theme-aware account metric cards in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AccountsPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getAllByRole('button', { name: '详情' })[0])

    const metricSurfaces = await screen.findAllByTestId('douyin-account-core-metric-surface')
    expect(metricSurfaces).toHaveLength(4)
    expect(metricSurfaces[0]).not.toHaveStyle({ backgroundColor: 'rgb(227, 242, 253)' })
    expect(metricSurfaces[1]).not.toHaveStyle({ backgroundColor: 'rgb(232, 245, 233)' })
    expect(metricSurfaces[2]).not.toHaveStyle({ backgroundColor: 'rgb(255, 248, 225)' })
    expect(metricSurfaces[3]).not.toHaveStyle({ backgroundColor: 'rgb(225, 245, 254)' })
  })

  it('uses theme-aware fan profile chart colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <AccountsPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getAllByRole('button', { name: '详情' })[0])
    fireEvent.click(screen.getByRole('tab', { name: '粉丝画像' }))

    expect(await screen.findByText('性别分布')).toBeInTheDocument()
    const chartSurfaces = await screen.findAllByTestId('douyin-fan-profile-chart-surface')
    expect(chartSurfaces.map(node => node.getAttribute('data-chart-kind'))).toEqual([
      'gender',
      'age',
      'province',
      'interest',
      'active-time',
      'device',
    ])
    expect(chartSurfaces[0]).toHaveAttribute('data-chart-colors', '#f3e5f5|#e3f2fd')
    expect(chartSurfaces[1]).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(chartSurfaces[2]).toHaveAttribute('data-chart-color', '#4fc3f7')
    expect(chartSurfaces[3]).toHaveAttribute('data-chart-color', '#f3e5f5')
    expect(chartSurfaces[4]).toHaveAttribute('data-chart-color', '#ffb74d')
    expect(chartSurfaces[5]).toHaveAttribute('data-chart-color', '#81c784')

    const chartText = screen.getAllByTestId('mock-echarts').map(node => node.textContent ?? '').join('\n')
    for (const legacy of ['#1976d2', '#42a5f5', '#26a69a', '#7e57c2', '#ff7043', '#66bb6a']) {
      expect(chartText).not.toContain(legacy)
    }
  })

  it('creates and edits accounts through the account drawer', async () => {
    renderPage()

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getByRole('button', { name: '绑定新账号' }))
    expect(screen.getByRole('heading', { name: '新建账号' })).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: /账号名称/ }), {
      target: { value: '新品观察号' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: /抖音号/ }), {
      target: { value: 'new-beauty' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: /描述/ }), {
      target: { value: '新品测评' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(douyinApi.accountSave).toHaveBeenCalledWith(
        expect.objectContaining({
          accountName: '新品观察号',
          accountId: 'new-beauty',
          description: '新品测评',
        }),
      )
      expect(toast).toHaveBeenCalledWith('保存成功', 'success')
    })

    fireEvent.click(screen.getAllByRole('button', { name: '编辑' })[0])
    expect(screen.getByRole('heading', { name: '编辑账号' })).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: /账号名称/ }), {
      target: { value: '美妆达人号 Pro' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(douyinApi.accountSave).toHaveBeenLastCalledWith(
        expect.objectContaining({
          id: 1,
          accountName: '美妆达人号 Pro',
        }),
      )
    })
  })

  it('manages OAuth authorization list actions', async () => {
    renderPage()

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getByRole('tab', { name: 'OAuth授权' }))

    expect(await screen.findByText('正常 1')).toBeInTheDocument()
    const oauthTab = screen.getByTestId('douyin-oauth-tab')
    expect(oauthTab).toHaveAttribute('data-contract-scope', 'douyin-oauth-current-page-actions')
    expect(oauthTab).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/douyin/oauth/revoke'))
    expect(oauthTab).toHaveAttribute('data-no-local-token-fallback', 'true')
    expect(oauthTab).toHaveAttribute('data-server-pagination', 'true')
    expect(screen.getByText('未授权 1')).toBeInTheDocument()
    expect(screen.getByText('已过期 1')).toBeInTheDocument()
    expect(screen.getAllByText('列表接口未返回 scope').length).toBeGreaterThan(0)
    expect(screen.getByText(/Token 精确到期时间需要进入详情调用/)).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: '复制账号ID' })[0])
    expect(writeText).toHaveBeenCalledWith('beauty001')
    expect(toast).toHaveBeenCalledWith('已复制', 'success')

    fireEvent.click(screen.getAllByRole('button', { name: '重新授权' })[0])
    await waitFor(() => {
      expect(douyinApi.oauthUrl).toHaveBeenCalledWith(1)
      expect(openSpy).toHaveBeenCalledWith('https://douyin.example/oauth', '_blank')
    })

    fireEvent.click(screen.getAllByRole('button', { name: '撤销' })[0])
    await waitFor(() => {
      expect(douyinApi.oauthRevoke).toHaveBeenCalledWith(1)
      expect(toast).toHaveBeenCalledWith('授权已撤销', 'success')
    })

    fireEvent.click(screen.getByRole('button', { name: '批量刷新' }))
    await waitFor(() => {
      expect(douyinApi.tokenRefresh).toHaveBeenCalledWith(1)
      expect(douyinApi.tokenRefresh).toHaveBeenCalledWith(2)
      expect(toast).toHaveBeenCalledWith('批量刷新完成：成功 2，失败 0，跳过 1', 'success')
    })
    expect(screen.getByText(/当前页 Token 刷新结果：共 3 个账号，成功 2 个，失败 0 个，跳过 1 个/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '导出报告' }))
    expect(createObjectUrl).toHaveBeenCalled()
    expect(anchorClick).toHaveBeenCalled()
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:oauth-report')
    expect(toast).toHaveBeenCalledWith('已导出当前页 3 个 OAuth 账号', 'success')
  })

  it('keeps account save, sync, and delete failures visible with account context', async () => {
    vi.mocked(douyinApi.accountSave).mockRejectedValueOnce(new Error('account save down') as never)
    vi.mocked(douyinApi.videoSync).mockRejectedValueOnce(new Error('video sync down') as never)
    vi.mocked(douyinApi.accountDelete).mockRejectedValueOnce(new Error('account delete down') as never)

    renderPage()

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getByRole('button', { name: '绑定新账号' }))
    fireEvent.change(screen.getByRole('textbox', { name: /账号名称/ }), {
      target: { value: '失败账号' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: /抖音号/ }), {
      target: { value: 'fail001' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(await screen.findByText(/\/douyin\/account\/save 账号保存失败/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-account-action-error')).toHaveAttribute('data-no-local-mutation', 'true')
    expect(screen.getByTestId('douyin-account-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/accountPk=新增; accountName=失败账号; douyinAccount=fail001/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '新建账号' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /账号名称/ })).toHaveValue('失败账号')
    fireEvent.click(screen.getByRole('button', { name: '取消' }))

    fireEvent.click(screen.getAllByRole('button', { name: '同步' })[0])
    expect(await screen.findByText(/\/douyin\/video\/sync 账号视频同步失败/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-account-action-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByText(/accountPk=1; accountName=美妆达人号; douyinAccount=beauty001/)).toBeInTheDocument()
    expect(screen.getByText('美妆达人号')).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: '删除' })[0])
    expect(screen.getByText(/endpoint=\/douyin\/account\/delete/)).toBeInTheDocument()
    expect(screen.getByText(/accountPk=1; accountName=美妆达人号; douyinAccount=beauty001/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/\/douyin\/account\/delete 账号删除失败/)).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '确认删除' })).toBeInTheDocument()
  })

  it('keeps detail drawer and OAuth failures visible with endpoint context', async () => {
    vi.mocked(douyinApi.personaSave).mockRejectedValueOnce(new Error('persona save down') as never)
    vi.mocked(douyinApi.fanProfileSync).mockRejectedValueOnce(new Error('profile sync down') as never)
    vi.mocked(douyinApi.tokenRefresh).mockRejectedValueOnce(new Error('token refresh down') as never)
    vi.mocked(douyinApi.oauthUrl).mockRejectedValueOnce(new Error('oauth url down') as never)

    renderPage()

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getAllByRole('button', { name: '详情' })[0])
    expect(await screen.findByText('@beauty001 | 粉丝 12.5万')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '重新授权' }))
    expect(await screen.findByText(/\/douyin\/oauth\/auth-url 获取授权链接失败/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-account-detail-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('douyin-account-detail-action-error')).toHaveAttribute('data-no-local-mutation', 'true')
    expect(screen.getByText(/accountPk=1; accountName=美妆达人号; douyinAccount=beauty001/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '人设配置' }))
    expect(await screen.findByText('敏感肌顾问')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    fireEvent.change(screen.getByLabelText('话术风格'), {
      target: { value: '失败风格' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(await screen.findByText(/\/douyin\/persona\/save 人设保存失败/)).toBeInTheDocument()
    expect(screen.getByText(/personaId=11; personaName=敏感肌顾问/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '编辑人设' })).toBeInTheDocument()
    expect(screen.getByLabelText('话术风格')).toHaveValue('失败风格')
    fireEvent.click(screen.getByRole('button', { name: '取消' }))

    fireEvent.click(screen.getByRole('tab', { name: '粉丝画像' }))
    expect(await screen.findByText('性别分布')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '同步画像' }))
    expect(await screen.findByText(/\/douyin\/fan-profile\/sync\/\{accountId\} 粉丝画像同步失败/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: 'Token管理' }))
    expect(await screen.findByText('剩余天数：17 天')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '刷新 Token' }))
    expect(await screen.findByText(/\/douyin\/oauth\/token-refresh Token 刷新失败/)).toBeInTheDocument()
  })

  it('keeps OAuth tab revoke and auth-url failures visible with account context', async () => {
    vi.mocked(douyinApi.oauthUrl).mockRejectedValueOnce(new Error('oauth url down') as never)
    vi.mocked(douyinApi.oauthRevoke).mockRejectedValueOnce(new Error('revoke down') as never)

    renderPage()

    await screen.findByText('美妆达人号')
    fireEvent.click(screen.getByRole('tab', { name: 'OAuth授权' }))
    await screen.findByText('正常 1')

    fireEvent.click(screen.getAllByRole('button', { name: '重新授权' })[0])
    expect(await screen.findByText(/\/douyin\/oauth\/auth-url 获取授权链接失败/)).toBeInTheDocument()
    expect(screen.getByTestId('douyin-oauth-action-error')).toHaveAttribute('data-no-local-token-mutation', 'true')
    expect(screen.getByTestId('douyin-oauth-action-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByText(/accountPk=1; accountName=美妆达人号; douyinAccount=beauty001/)).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: '撤销' })[0])
    expect(await screen.findByText(/\/douyin\/oauth\/revoke OAuth 撤销失败/)).toBeInTheDocument()
    expect(screen.getByText(/失败不会本地改写授权状态/)).toBeInTheDocument()
  })
})
