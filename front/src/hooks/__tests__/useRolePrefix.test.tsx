import { renderHook } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useRolePrefix } from '../useRolePrefix'
import { MemoryRouter } from 'react-router-dom'
import { useUserStore } from '@/stores'

vi.mock('@/stores', () => ({
  useUserStore: vi.fn(),
}))

describe('useRolePrefix', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function mockRole(roleCode = '') {
    vi.mocked(useUserStore).mockImplementation((selector: any) => selector({
      userInfo: roleCode ? { roles: [roleCode] } : null,
    }) as any)
  }

  it('returns /admin when pathname starts with /admin', () => {
    mockRole('user')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/admin/products']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/admin')
  })

  it('returns /org when pathname starts with /org', () => {
    mockRole('user')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/org/dashboard']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/org')
  })

  it('returns /talent when pathname starts with /talent', () => {
    mockRole('user')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/talent/videos']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/talent')
  })

  it('returns /user when pathname starts with /user', () => {
    mockRole('admin')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/user/dashboard']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/user')
  })

  it('returns /admin for admin role when pathname does not match', () => {
    mockRole('admin')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/other']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/admin')
  })

  it('returns /org for institution role when pathname does not match', () => {
    mockRole('institution')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/other']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/org')
  })

  it('returns /talent for talent role when pathname does not match', () => {
    mockRole('talent')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/other']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/talent')
  })

  it('returns /user for user role when pathname does not match', () => {
    mockRole('user')

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/other']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/user')
  })

  it('returns /user when no roles are present', () => {
    vi.mocked(useUserStore).mockImplementation((selector: any) => selector({
      userInfo: { roles: [] },
    }) as any)

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/other']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/user')
  })

  it('returns /user when userInfo is null', () => {
    mockRole()

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/other']}>{children}</MemoryRouter>
    )

    const { result } = renderHook(() => useRolePrefix(), { wrapper })

    expect(result.current).toBe('/user')
  })
})
