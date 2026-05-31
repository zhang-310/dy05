import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { AdminLayout } from '../AdminLayout'
import { useUserStore } from '@/stores'

const mediaState = {
  mobile: false,
}

vi.mock('@mui/material', async () => {
  const actual = await vi.importActual<typeof import('@mui/material')>('@mui/material')
  return {
    ...actual,
    useMediaQuery: () => mediaState.mobile,
  }
})

function ShellPage() {
  return <div>管理员暗色侧栏审计内容</div>
}

function renderAdmin(path = '/admin/shortvideo/projects') {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route path="shortvideo/projects" element={<ShellPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('AdminLayout theme mode', () => {
  beforeEach(() => {
    mediaState.mobile = false
    window.localStorage.clear()
    useUserStore.setState({
      token: 'token-1',
      userInfo: { id: 1, username: 'admin', nickname: '管理员', roles: ['admin'] },
    })
  })

  it('keeps the desktop rail and submenu on theme surfaces after switching dark mode', () => {
    renderAdmin()

    fireEvent.click(screen.getByRole('button', { name: '切换暗色主题' }))

    expect(screen.getAllByTestId('admin-nav-rail')[0]).toHaveStyle({ backgroundColor: 'rgb(18, 18, 18)' })
    expect(screen.getAllByTestId('admin-nav-submenu')[0]).toHaveStyle({ backgroundColor: 'rgb(18, 18, 18)' })
    expect(screen.getByText('管理员暗色侧栏审计内容')).toBeInTheDocument()
  })

  it('uses the same themed navigation inside the mobile drawer', () => {
    mediaState.mobile = true
    renderAdmin()

    fireEvent.click(screen.getByRole('button', { name: '切换暗色主题' }))
    fireEvent.click(screen.getByRole('button', { name: '打开导航菜单' }))

    const rails = screen.getAllByTestId('admin-nav-rail')
    expect(rails[rails.length - 1]).toHaveStyle({ backgroundColor: 'rgb(18, 18, 18)' })
    expect(screen.getAllByText('短视频项目').length).toBeGreaterThan(0)
  })
})
