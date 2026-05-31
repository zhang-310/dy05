import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { useUserStore } from '@/stores'
import { authApi } from '@/api/auth'
import ProfilePage from '../ProfilePage'

vi.mock('@/api/auth', () => ({
  authApi: {
    profile: vi.fn(),
    profileUpdate: vi.fn(),
    changePassword: vi.fn(),
  },
}))

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useUserStore.setState({
      token: 'token-1',
      userInfo: { id: 1, username: 'alice', nickname: 'Alice', roles: ['talent'] },
    })
    vi.mocked(authApi.profile).mockResolvedValue({
      userId: 1,
      username: 'alice',
      nickname: 'Alice',
      email: 'alice@example.com',
      phone: '13800000000',
      avatarUrl: '',
    })
    vi.mocked(authApi.profileUpdate).mockResolvedValue(undefined)
    vi.mocked(authApi.changePassword).mockResolvedValue(undefined)
  })

  it('loads profile and exposes the real profile endpoints', async () => {
    renderWithProviders(<ProfilePage />)

    expect(await screen.findByDisplayValue('Alice')).toBeInTheDocument()
    expect(screen.getByTestId('profile-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/auth/profile,/auth/profile/update,/auth/profile/change-password',
    )
    expect(screen.getByTestId('profile-page')).toHaveAttribute('data-role-scope', 'talent')
  })

  it('saves profile and syncs nickname to the login store', async () => {
    renderWithProviders(<ProfilePage />)

    await screen.findByDisplayValue('Alice')
    const nicknameInput = await screen.findByTestId('profile-nickname-input')
    fireEvent.change(nicknameInput, { target: { value: 'New Nick' } })
    await waitFor(() => {
      expect(screen.getByDisplayValue('New Nick')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '保存资料' }))

    await waitFor(() => {
      expect(authApi.profileUpdate).toHaveBeenCalledWith({
        nickname: 'New Nick',
        email: 'alice@example.com',
        phone: '13800000000',
        avatarUrl: '',
      })
    })
    expect(useUserStore.getState().userInfo?.nickname).toBe('New Nick')
  })

  it('retains profile input when update fails', async () => {
    vi.mocked(authApi.profileUpdate).mockRejectedValueOnce(new Error('save down'))
    renderWithProviders(<ProfilePage />)

    await screen.findByDisplayValue('alice@example.com')
    const emailInput = await screen.findByTestId('profile-email-input')
    fireEvent.change(emailInput, { target: { value: 'kept@example.com' } })
    await waitFor(() => {
      expect(screen.getByDisplayValue('kept@example.com')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '保存资料' }))

    expect(await screen.findByTestId('profile-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('kept@example.com')).toBeInTheDocument()
  })

  it('validates password confirmation before calling the API', async () => {
    renderWithProviders(<ProfilePage />)

    fireEvent.change(await screen.findByTestId('profile-old-password-input'), { target: { value: 'old-pass' } })
    fireEvent.change(screen.getByTestId('profile-new-password-input'), { target: { value: 'new-pass' } })
    fireEvent.change(screen.getByTestId('profile-confirm-password-input'), { target: { value: 'bad-pass' } })
    fireEvent.click(screen.getByRole('button', { name: '修改密码' }))

    expect(await screen.findByTestId('profile-password-error')).toHaveAttribute('data-input-retained', 'true')
    expect(authApi.changePassword).not.toHaveBeenCalled()
  })
})
