import { beforeEach, describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import OnboardingPage from '../OnboardingPage'
import { GettingStartedChecklist } from '@/components/onboarding/GettingStartedChecklist'
import { aiApi } from '@/api/ai'
import { dashboardApi } from '@/api/dashboard'
import { productApi } from '@/api/product'
import { storageApi } from '@/api/storage'

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
  },
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    adminModelsList: vi.fn(),
  },
}))

vi.mock('@/api/storage', () => ({
  storageApi: {
    configured: vi.fn(),
  },
}))

vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    kpiUnified: vi.fn(),
  },
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => ({
      'onboarding.live': '直播准备',
      'onboarding.readiness': '商品就绪',
      'onboarding.sv': '短视频准备',
      'onboarding.kpi': '经营 KPI',
      'onboarding.show': '显示引导',
      'onboarding.checklistTitle': '新手清单',
      'onboarding.collapse': '收起',
      'onboarding.go': '前往',
      'onboarding.guide': '步骤向导',
      'onboarding.backToWorkbench': '返回工作台',
      'onboarding.adminOnlyGuideNote': '完整初始化向导目前仅管理员端开放。',
      'onboarding.adminOnlyReadinessNote': '商品就绪度页面仅管理员端开放，当前角色返回工作台查看可用入口。',
      'onboarding.orgShortvideoFallbackNote': '机构端暂未开放短视频项目页，当前降级回机构工作台。',
      'onboarding.talentKpiFallbackNote': '达人端暂未开放统一 KPI 页，当前降级回达人工作台。',
    }[key] ?? key),
  }),
}))

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(productApi.list).mockResolvedValue({ total: 2, list: [], pageNum: 0, pageSize: 1 } as never)
    vi.mocked(aiApi.adminModelsList).mockResolvedValue([{ id: 1, modelName: 'qwen' }] as never)
    vi.mocked(storageApi.configured).mockResolvedValue(true as never)
    vi.mocked(dashboardApi.kpiUnified).mockResolvedValue({ gmvToday: 99 } as never)
  })

  it('routes AI model setup to existing models-config page', () => {
    renderWithProviders(
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '初始化引导' })).toBeInTheDocument()

    for (let i = 0; i < 4; i += 1) {
      fireEvent.click(screen.getAllByRole('button', { name: '稍后完成' })[0])
    }

    fireEvent.click(screen.getByRole('button', { name: '去模型配置' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/models-config')
  })

  it('renders real dependency health cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>,
    )

    const workbench = screen.getByTestId('onboarding-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'admin-onboarding-dependency-readiness')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/product/search')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/ai/admin/models/list')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/storage/configured')
    expect(workbench.getAttribute('data-ready-endpoints')).toContain('/dashboard/kpi-unified')
    expect(workbench).toHaveAttribute('data-no-local-readiness-complete', 'true')
    expect(workbench).toHaveAttribute('data-no-local-dependency-fallback', 'true')
    expect(workbench).toHaveAttribute('data-navigation-only', 'true')
    expect(await screen.findByText(/当前可见 2 个商品/)).toBeInTheDocument()
    expect(screen.getByText(/当前 1 个模型配置/)).toBeInTheDocument()
    expect(screen.getByText(/BOS 已配置/)).toBeInTheDocument()
    expect(screen.getByText(/已接入 \/dashboard\/kpi-unified/)).toBeInTheDocument()
    expect(screen.getByText(/依赖状态：基础依赖已连接/)).toBeInTheDocument()

    const dependencyCards = screen.getAllByTestId('onboarding-dependency-card')
    expect(dependencyCards).toHaveLength(4)
    expect(dependencyCards.map(card => card.getAttribute('data-contract-source'))).toEqual([
      '/product/search',
      '/ai/admin/models/list',
      '/storage/configured',
      '/dashboard/kpi-unified',
    ])
    dependencyCards.forEach(card => {
      expect(card).toHaveAttribute('data-no-local-dependency-fallback', 'true')
    })
    expect(screen.getAllByTestId('onboarding-step-contract')).toHaveLength(5)
  })

  it('shows checking state before dependency health requests resolve', () => {
    vi.mocked(productApi.list).mockReturnValue(new Promise(() => {}) as never)
    vi.mocked(aiApi.adminModelsList).mockReturnValue(new Promise(() => {}) as never)
    vi.mocked(storageApi.configured).mockReturnValue(new Promise(() => {}) as never)
    vi.mocked(dashboardApi.kpiUnified).mockReturnValue(new Promise(() => {}) as never)

    renderWithProviders(
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('正在检查商品检索接口...')).toBeInTheDocument()
    expect(screen.getByText('正在检查模型配置接口...')).toBeInTheDocument()
    expect(screen.getByText('正在检查 BOS 存储配置...')).toBeInTheDocument()
    expect(screen.getByText('正在检查统一 KPI 接口...')).toBeInTheDocument()
    expect(screen.queryByText(/当前可见 0 个商品/)).not.toBeInTheDocument()
  })

  it('names the failing dependency endpoint in health cards', async () => {
    vi.mocked(productApi.list).mockRejectedValue(new Error('product down') as never)
    vi.mocked(aiApi.adminModelsList).mockRejectedValue(new Error('model down') as never)
    vi.mocked(storageApi.configured).mockRejectedValue(new Error('storage down') as never)
    vi.mocked(dashboardApi.kpiUnified).mockRejectedValue(new Error('kpi down') as never)

    renderWithProviders(
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/检查失败：\/product\/search - product down/)).toBeInTheDocument()
    expect(screen.getByText(/检查失败：\/ai\/admin\/models\/list - model down/)).toBeInTheDocument()
    expect(screen.getByText(/检查失败：\/storage\/configured - storage down/)).toBeInTheDocument()
    expect(screen.getByText(/检查失败：\/dashboard\/kpi-unified - kpi down/)).toBeInTheDocument()
    expect(screen.getByText(/依赖状态：4 项异常/)).toBeInTheDocument()
  })

  it('rechecks every dependency from the onboarding header', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>,
    )

    await screen.findByText(/依赖状态：基础依赖已连接/)
    fireEvent.click(screen.getByRole('button', { name: '重新检查依赖' }))

    await waitFor(() => {
      expect(productApi.list).toHaveBeenCalledTimes(2)
      expect(aiApi.adminModelsList).toHaveBeenCalledTimes(2)
      expect(storageApi.configured).toHaveBeenCalledTimes(2)
      expect(dashboardApi.kpiUnified).toHaveBeenCalledTimes(2)
    })
  })

  it('allows the final step to reach the completion panel', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>,
    )

    await screen.findByText(/依赖状态：基础依赖已连接/)

    for (let i = 0; i < 4; i += 1) {
      fireEvent.click(screen.getAllByRole('button', { name: '稍后完成' })[0])
    }
    fireEvent.click(screen.getByRole('button', { name: '完成浏览' }))

    expect(screen.getByText('引导已浏览')).toBeInTheDocument()
    expect(screen.getByTestId('onboarding-completion-panel')).toHaveAttribute('data-no-local-readiness-complete', 'true')
    expect(screen.getByText(/当前依赖状态为：基础依赖已连接/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '进入控制台' }))
    expect(navigate).toHaveBeenCalledWith('/admin/dashboard')
  })

  it('routes checklist KPI action to the existing admin KPI page', () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <GettingStartedChecklist />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '前往：经营 KPI' }))

    expect(navigate).toHaveBeenCalledWith('/admin/kpi')
  })

  it('routes org checklist actions only to registered org pages or explicit fallbacks', () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/org/dashboard']}>
        <GettingStartedChecklist />
      </MemoryRouter>,
    )

    expect(screen.getByText('完整初始化向导目前仅管理员端开放。')).toBeInTheDocument()
    expect(screen.getByText('商品就绪度页面仅管理员端开放，当前角色返回工作台查看可用入口。')).toBeInTheDocument()
    expect(screen.getByText('机构端暂未开放短视频项目页，当前降级回机构工作台。')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '步骤向导' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '前往：直播准备' }))
    expect(navigate).toHaveBeenLastCalledWith('/org/live/sessions')
    fireEvent.click(screen.getByRole('button', { name: '返回工作台：商品就绪' }))
    expect(navigate).toHaveBeenLastCalledWith('/org/dashboard')
    fireEvent.click(screen.getByRole('button', { name: '返回工作台：短视频准备' }))
    expect(navigate).toHaveBeenLastCalledWith('/org/dashboard')
    fireEvent.click(screen.getByRole('button', { name: '前往：经营 KPI' }))
    expect(navigate).toHaveBeenLastCalledWith('/org/analytics')
  })

  it('routes talent checklist actions only to registered talent pages or explicit fallbacks', () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/talent/dashboard']}>
        <GettingStartedChecklist />
      </MemoryRouter>,
    )

    expect(screen.getByText('完整初始化向导目前仅管理员端开放。')).toBeInTheDocument()
    expect(screen.getByText('商品就绪度页面仅管理员端开放，当前角色返回工作台查看可用入口。')).toBeInTheDocument()
    expect(screen.getByText('达人端暂未开放统一 KPI 页，当前降级回达人工作台。')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '前往：直播准备' }))
    expect(navigate).toHaveBeenLastCalledWith('/talent/live/sessions')
    fireEvent.click(screen.getByRole('button', { name: '返回工作台：商品就绪' }))
    expect(navigate).toHaveBeenLastCalledWith('/talent/dashboard')
    fireEvent.click(screen.getByRole('button', { name: '前往：短视频准备' }))
    expect(navigate).toHaveBeenLastCalledWith('/talent/shortvideo')
    fireEvent.click(screen.getByRole('button', { name: '返回工作台：经营 KPI' }))
    expect(navigate).toHaveBeenLastCalledWith('/talent/dashboard')
  })
})
