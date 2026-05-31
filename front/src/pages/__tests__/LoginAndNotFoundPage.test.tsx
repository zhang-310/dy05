import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import LoginPage from '../LoginPage'
import NotFoundPage from '../NotFoundPage'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
  },
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

function renderLogin(path = '/login') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderLoginWithTheme(path = '/login') {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

function renderNotFound(path = '/missing/page?from=a#section') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('LoginPage and NotFoundPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('logs in and redirects to a safe returnUrl', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'token-1',
      userId: 1,
      username: 'admin',
      nickname: '管理员',
      avatarUrl: '',
      roleCode: 'admin',
      organizationId: 1,
      organizationName: 'DY05',
    })

    renderLogin('/login?returnUrl=%2Fadmin%2Fai%2Fdashboard%3Ftab%3Dcost')

    const root = screen.getByTestId('login-page-surface')
    expect(root).toHaveAttribute('data-contract-scope', 'login-auth-route')
    expect(root).toHaveAttribute('data-ready-endpoints', '/auth/login')
    expect(root).toHaveAttribute('data-return-url-state', 'valid')
    expect(root).toHaveAttribute('data-validated-return-path', '/admin/ai/dashboard?tab=cost')
    expect(root).toHaveAttribute('data-no-open-redirect', 'true')
    expect(root).toHaveAttribute('data-no-login-loop-return', 'true')
    expect(root).toHaveAttribute('data-no-local-auth-fallback', 'true')
    expect(screen.getByText('登录后返回：/admin/ai/dashboard?tab=cost')).toBeInTheDocument()
    expect(screen.getByTestId('login-return-url-valid-alert')).toHaveAttribute('data-contract-source', 'returnUrl')
    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'admin' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'secret' } })
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({ username: 'admin', password: 'secret' })
      expect(window.localStorage.getItem('token')).toBe('token-1')
      expect(navigate).toHaveBeenCalledWith('/admin/ai/dashboard?tab=cost', { replace: true })
    })
  })

  it('shows invalid returnUrl and login failures on the page', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('bad credentials'))

    renderLogin('/login?returnUrl=https%3A%2F%2Fevil.example%2Fadmin')

    expect(screen.getByText(/回跳地址无效/)).toBeInTheDocument()
    expect(screen.getByTestId('login-page-surface')).toHaveAttribute('data-return-url-state', 'invalid')
    expect(screen.getByTestId('login-return-url-invalid-alert')).toHaveAttribute('data-fallback-path', '/admin/dashboard')
    expect(screen.getByTestId('login-return-url-invalid-alert')).toHaveAttribute('data-no-open-redirect', 'true')
    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'admin' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'bad' } })
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    expect(await screen.findByText(/登录失败：bad credentials/)).toBeInTheDocument()
    expect(screen.getByTestId('login-error-alert')).toHaveAttribute('data-contract-source', '/auth/login')
    expect(screen.getByTestId('login-error-alert')).toHaveAttribute('data-input-retained', 'true')
    expect(navigate).not.toHaveBeenCalled()
  })

  it('keeps field values and blocks unsafe login-page return paths', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'token-2',
      userId: 2,
      username: 'ops',
      nickname: '运营',
      avatarUrl: '',
      roleCode: 'ops',
      organizationId: 1,
      organizationName: 'DY05',
    })

    renderLogin('/login?returnUrl=%2Flogin%3FreturnUrl%3D%252Fadmin%252Fdashboard')

    expect(screen.getByText(/回跳地址无效/)).toBeInTheDocument()
    expect(screen.getByTestId('login-page-surface')).toHaveAttribute('data-return-url-state', 'invalid')
    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'ops' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'secret' } })
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({ username: 'ops', password: 'secret' })
      expect(navigate).toHaveBeenCalledWith('/admin/dashboard', { replace: true })
    })
    expect(screen.getByLabelText('用户名')).toHaveValue('ops')
  })

  it('uses the app background surface in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderLoginWithTheme()

    expect(screen.getByTestId('login-page-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('shows the unmatched route and offers recovery actions', () => {
    renderNotFound()

    const root = screen.getByTestId('not-found-page-surface')
    expect(root).toHaveAttribute('data-contract-scope', 'not-found-route-recovery')
    expect(root).toHaveAttribute('data-current-path', '/missing/page?from=a#section')
    expect(root).toHaveAttribute('data-home-path', '/admin/dashboard')
    expect(root).toHaveAttribute('data-ready-actions', 'navigate-back|navigate-home')
    expect(root).toHaveAttribute('data-no-local-route-fallback', 'true')
    expect(root).toHaveAttribute('data-no-external-redirect', 'true')
    expect(screen.getByText('页面不存在')).toBeInTheDocument()
    expect(screen.getByTestId('not-found-current-path-alert')).toHaveAttribute('data-contract-source', 'useLocation')
    expect(screen.getByText(/当前路径未匹配任何前端路由：\/missing\/page\?from=a#section/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '返回上一页' }))
    expect(navigate).toHaveBeenCalledWith(-1)
    fireEvent.click(screen.getByRole('button', { name: '返回首页' }))
    expect(navigate).toHaveBeenCalledWith('/admin/dashboard')
  })
})
