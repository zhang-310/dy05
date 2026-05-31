import { describe, expect, it, beforeEach, vi } from 'vitest'
import { MemoryRouter, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import { useUserStore } from '@/stores'
import { RequireAuth } from '../index'

vi.mock('@/utils/auth', () => ({
  isAuthenticated: () => true,
}))

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-probe">{location.pathname}</div>
}

function renderGate(initialPath: string, role: string) {
  useUserStore.setState({
    userInfo: { id: 1, username: role, roles: [role] },
    token: 'test-token',
  })
  return renderWithProviders(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="/admin/*" element={<Outlet />}>
            <Route path="*" element={<LocationProbe />} />
          </Route>
          <Route path="/org/*" element={<Outlet />}>
            <Route path="*" element={<LocationProbe />} />
          </Route>
          <Route path="/talent/*" element={<Outlet />}>
            <Route path="*" element={<LocationProbe />} />
          </Route>
          <Route path="/user/*" element={<Outlet />}>
            <Route path="*" element={<LocationProbe />} />
          </Route>
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAuth role gate', () => {
  beforeEach(() => {
    useUserStore.setState({ userInfo: null, token: null })
  })

  it('redirects non-admin roles away from real admin platform routes', async () => {
    renderGate('/admin/ai/dashboard', 'talent')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/talent/dashboard')
    })
  })

  it('migrates legacy admin shortvideo routes before role gate checks', async () => {
    renderGate('/admin/shortvideo/projects?page=1', 'talent')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/talent/shortvideo/projects')
    })
  })

  it('migrates legacy admin live routes into org shell', async () => {
    renderGate('/admin/live/sessions/18?tab=scripts', 'institution')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/org/live/sessions/18')
    })
  })

  it('allows normal users to stay in their own profile page', async () => {
    renderGate('/user/profile', 'user')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/user/profile')
    })
  })

  it('allows normal users to stay in their own shortvideo line', async () => {
    renderGate('/user/shortvideo/create', 'user')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/user/shortvideo/create')
    })
  })

  it('blocks talent users from entering the normal user shell', async () => {
    renderGate('/user/profile', 'talent')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/talent/dashboard')
    })
  })

  it('blocks normal users from entering the talent shell', async () => {
    renderGate('/talent/dashboard', 'user')

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/user/dashboard')
    })
  })
})
