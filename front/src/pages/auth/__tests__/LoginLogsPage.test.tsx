import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import LoginLogsPage from '../LoginLogsPage'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    loginLogs: vi.fn(),
  },
}))

describe('LoginLogsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(authApi.loginLogs).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          userId: 1,
          username: 'admin',
          loginType: 'web',
          deviceType: 'desktop',
          ip: '127.0.0.1',
          userAgent: 'Chrome',
          status: 1,
          loginTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('loads login logs with real paged contract', async () => {
    renderWithProviders(
      <MemoryRouter>
        <LoginLogsPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('登录日志')).toBeInTheDocument()
    expect(screen.getByTestId('auth-login-logs-workbench')).toHaveAttribute('data-contract-scope', 'auth-login-logs')
    expect(screen.getByTestId('auth-login-logs-workbench')).toHaveAttribute('data-ready-endpoints', '/auth/user/login-logs')
    expect(screen.getByTestId('auth-login-logs-workbench')).toHaveAttribute('data-no-local-login-log-fallback', 'true')
    expect(screen.getByTestId('auth-login-logs-workbench')).toHaveAttribute('data-no-local-export-csv-fallback', 'true')
    expect(screen.getByTestId('auth-login-logs-workbench')).toHaveAttribute('data-no-client-admin-scope-bypass', 'true')
    expect(screen.getByTestId('auth-login-logs-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')

    await waitFor(() => {
      expect(authApi.loginLogs).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        username: undefined,
        userId: undefined,
        status: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('Chrome')).toBeInTheDocument()
      expect(screen.getByText('登录日志来自 /auth/user/login-logs；管理员可按 userId、username、status 筛选，普通用户后端会强制只返回本人记录。')).toBeInTheDocument()
    })
  })

  it('renders wrapped login log payloads', async () => {
    vi.mocked(authApi.loginLogs).mockResolvedValue({
      data: {
        loginLogs: [
          {
            id: 2,
            userId: 2,
            username: 'bob',
            loginType: 'web',
            deviceType: 'mobile',
            ip: '10.0.0.2',
            userAgent: 'Safari',
            status: 0,
            failReason: 'bad password',
            loginTime: '2026-04-11 10:00:00',
          },
        ],
        totalElements: 1,
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <LoginLogsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('bob')).toBeInTheDocument()
    expect(screen.getByText('Safari')).toBeInTheDocument()
    expect(screen.getByText('本页失败')).toBeInTheDocument()
  })

  it('shows login log load failure with endpoint and filter context', async () => {
    vi.mocked(authApi.loginLogs).mockRejectedValueOnce(new Error('login logs down') as never)

    renderWithProviders(
      <MemoryRouter>
        <LoginLogsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('auth-login-logs-list-error')).toHaveAttribute('data-no-local-login-log-fallback', 'true')
    expect(await screen.findByText(/\/auth\/user\/login-logs 登录日志加载失败：login logs down/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/auth\/login-logs; username=空; userId=空; status=全部; page=0; rows=20/)).toBeInTheDocument()
  })

  it('submits username, userId and status filters to login logs endpoint', async () => {
    renderWithProviders(
      <MemoryRouter>
        <LoginLogsPage />
      </MemoryRouter>,
    )

    await screen.findByText('admin')
    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'alice' } })
    fireEvent.change(screen.getByLabelText('用户ID'), { target: { value: '18x' } })
    fireEvent.mouseDown(screen.getByLabelText('状态'))
    fireEvent.click(await screen.findByRole('option', { name: '失败' }))
    fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => {
      expect(authApi.loginLogs).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        username: 'alice',
        userId: 18,
        status: 0,
      })
    })
  })
})
