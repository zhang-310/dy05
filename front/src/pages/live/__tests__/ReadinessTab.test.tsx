import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { ReadinessTab } from '../components/ReadinessTab'
import { liveApi } from '@/api/live'

const navigate = vi.hoisted(() => vi.fn())
const toast = vi.hoisted(() => vi.fn())
let mockPathname = '/admin/live/sessions/18'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useLocation: () => ({ pathname: mockPathname, search: '', hash: '', state: null, key: 'test' }),
  }
})

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionReadiness: vi.fn(),
    sessionClone: vi.fn(),
    scriptExport: vi.fn(),
    templateSaveAsFromSession: vi.fn(),
  },
}))

vi.mock('../contexts', () => ({
  useCoreData: () => ({
    session: {
      id: 18,
      liveTitle: '测试直播',
      scheduledTime: '2026-05-22 20:00:00',
    },
    products: [{ id: 1, productId: 10, productName: '精华液' }],
    scripts: [{ id: 2, sessionId: 18, scriptContent: '开场话术', status: 1 }],
  }),
}))

describe('ReadinessTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockPathname = '/admin/live/sessions/18'
    navigate.mockClear()
    toast.mockClear()
    vi.mocked(liveApi.sessionReadiness).mockResolvedValue({ score: 90 } as never)
    vi.mocked(liveApi.sessionClone).mockResolvedValue(19 as never)
    vi.mocked(liveApi.scriptExport).mockResolvedValue('开场话术' as never)
    vi.mocked(liveApi.templateSaveAsFromSession).mockResolvedValue(88 as never)
  })

  it('marks readiness tab contract boundaries and context-only local checks', async () => {
    renderWithProviders(<ReadinessTab />)

    const root = screen.getByTestId('readiness-tab-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'live-readiness-release-check')
    expect(root).toHaveAttribute('data-ready-endpoints', '/live/session/readiness|/live/session-template/save-as|/live/session/clone|/live/script/export')
    expect(root).toHaveAttribute('data-context-endpoints', '/live/session/get|/live/product/by-session|/live/script/by-session')
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shortvideo-project-create'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-backend-readiness-score-fallback'))
    expect(root).toHaveAttribute('data-route-scope', 'admin')
    expect(root).toHaveAttribute('data-products-count', '1')
    expect(root).toHaveAttribute('data-scripts-count', '1')

    expect(screen.getByTestId('readiness-contract-alert')).toHaveAttribute('data-no-local-backend-readiness-score-fallback', 'true')
    expect(screen.getByTestId('readiness-action-toolbar')).toHaveAttribute('data-contract-source', '/live/session/readiness|/live/session-template/save-as|/live/session/clone|/live/script/export')
    expect(screen.getByTestId('readiness-export-button')).toHaveAttribute('data-no-local-export-fallback', 'true')
    expect(screen.getByTestId('readiness-save-template-button')).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(screen.getByTestId('readiness-clone-button')).toHaveAttribute('data-no-local-clone-fallback', 'true')
    expect(screen.getByTestId('readiness-local-check-list')).toHaveAttribute('data-contract-source', '/live/session/get|/live/product/by-session|/live/script/by-session')
    expect(screen.getByTestId('readiness-local-check-products')).toHaveAttribute('data-readiness-status', 'ok')
    expect(await screen.findByTestId('readiness-backend-success')).toHaveAttribute('data-backend-readiness-score', '90')
  })

  it('clones a session and routes to the real admin live session workspace', async () => {
    renderWithProviders(<ReadinessTab />)

    fireEvent.click(screen.getByRole('button', { name: /克隆场次/ }))

    await waitFor(() => {
      expect(liveApi.sessionClone).toHaveBeenCalledWith(18)
      expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/live/sessions/19'))
    })
    expect(toast).toHaveBeenCalledWith('场次已克隆', 'success')
  })

  it('clones a session and stays inside organization shell', async () => {
    mockPathname = '/org/live/sessions/18'
    renderWithProviders(<ReadinessTab />)

    fireEvent.click(screen.getByRole('button', { name: /克隆场次/ }))

    await waitFor(() => {
      expect(liveApi.sessionClone).toHaveBeenCalledWith(18)
      expect(navigate).toHaveBeenCalledWith('/org/live/sessions/19')
    })
  })

  it('keeps backend readiness failure visible with endpoint source', async () => {
    vi.mocked(liveApi.sessionReadiness).mockRejectedValue(new Error('readiness down') as never)

    renderWithProviders(<ReadinessTab />)

    expect(await screen.findByText(/\/live\/session\/readiness 后端准备度检测失败：readiness down/)).toBeInTheDocument()
    expect(screen.getByText('准备检查项')).toBeInTheDocument()
  })

  it('shows clone failure source inside readiness page', async () => {
    vi.mocked(liveApi.sessionClone).mockRejectedValue(new Error('clone down') as never)

    renderWithProviders(<ReadinessTab />)

    fireEvent.click(screen.getByRole('button', { name: /克隆场次/ }))

    const error = await screen.findByTestId('readiness-operation-error')
    expect(error).toHaveTextContent('/live/session/clone 克隆场次失败：clone down')
    expect(error).toHaveAttribute('data-contract-source', '/live/session/clone')
    expect(error).toHaveAttribute('data-no-local-clone-fallback', 'true')
  })

  it('shows export failure source inside readiness page', async () => {
    vi.mocked(liveApi.scriptExport).mockRejectedValue(new Error('export down') as never)

    renderWithProviders(<ReadinessTab />)

    fireEvent.click(screen.getByRole('button', { name: /导出话术/ }))

    const error = await screen.findByTestId('readiness-operation-error')
    expect(error).toHaveTextContent('/live/script/export 导出话术失败：export down')
    expect(error).toHaveAttribute('data-contract-source', '/live/script/export')
    expect(error).toHaveAttribute('data-no-local-export-fallback', 'true')
  })

  it('shows save-template failure source inside dialog', async () => {
    vi.mocked(liveApi.templateSaveAsFromSession).mockRejectedValue(new Error('template down') as never)

    renderWithProviders(<ReadinessTab />)

    fireEvent.click(screen.getByRole('button', { name: /保存为模板/ }))
    fireEvent.change(await screen.findByRole('textbox', { name: /模板名称/ }), { target: { value: '复盘模板' } })
    fireEvent.click(screen.getByRole('button', { name: /^保存$/ }))

    const error = await screen.findByTestId('readiness-template-save-error')
    expect(error).toHaveTextContent('/live/session-template/save-as 保存模板失败：template down')
    expect(error).toHaveAttribute('data-contract-source', '/live/session-template/save-as')
    expect(error).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(screen.getByRole('textbox', { name: /模板名称/ })).toHaveValue('复盘模板')
    expect(liveApi.templateSaveAsFromSession).toHaveBeenCalledWith({ sessionId: 18, name: '复盘模板', description: '' })
  })

  it('uses a theme-aware overall status surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <ReadinessTab />
      </AppThemeProvider>,
    )

    expect(await screen.findByTestId('readiness-overall-status-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })
})
