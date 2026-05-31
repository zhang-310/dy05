import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { LayoutBreadcrumbs } from '../LayoutBreadcrumbs'
import { LayoutTopBar } from '../LayoutTopBar'

describe('LayoutBreadcrumbs and LayoutTopBar', () => {
  it('uses the shared breadcrumb resolver for dynamic routes', () => {
    renderWithProviders(
      <MemoryRouter>
        <LayoutBreadcrumbs pathname="/admin/ai/agent/workflow/edit/12" />
      </MemoryRouter>,
    )

    expect(screen.getByLabelText('页面路径')).toHaveTextContent('首页')
    expect(screen.getByLabelText('页面路径')).toHaveTextContent('AI 中心')
    expect(screen.getByLabelText('页面路径')).toHaveTextContent('工作流编辑')
    expect(screen.queryByText('edit')).not.toBeInTheDocument()
  })

  it('keeps role shell breadcrumbs distinct from admin home', () => {
    renderWithProviders(
      <MemoryRouter>
        <LayoutBreadcrumbs pathname="/talent/live/sessions/18" />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: '达人首页' })).toHaveAttribute('href', '/talent/dashboard')
    expect(screen.getByLabelText('页面路径')).toHaveTextContent('达人')
    expect(screen.getByLabelText('页面路径')).toHaveTextContent('达人场次工作台')
    expect(screen.queryByRole('link', { name: '首页' })).not.toBeInTheDocument()
  })

  it('exposes accessible sidebar, theme, and user menu controls', () => {
    const onToggleTheme = vi.fn()
    const onToggleSidebar = vi.fn()
    const onUserMenuClick = vi.fn()

    renderWithProviders(
      <MemoryRouter>
        <LayoutTopBar
          pathname="/admin/shortvideo/benchmark/quality-scripts/9"
          themeMode="light"
          onToggleTheme={onToggleTheme}
          onToggleSidebar={onToggleSidebar}
          useTopModules={false}
          userDisplayName="审计员"
          roleName="管理员"
          onUserMenuClick={onUserMenuClick}
        />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '打开侧边栏' }))
    fireEvent.click(screen.getByRole('button', { name: '切换暗色主题' }))
    fireEvent.click(screen.getByRole('button', { name: '打开用户菜单' }))

    expect(onToggleSidebar).toHaveBeenCalled()
    expect(onToggleTheme).toHaveBeenCalled()
    expect(onUserMenuClick).toHaveBeenCalled()
    expect(screen.getByLabelText('页面路径')).toHaveTextContent('质量脚本详情')
  })
})
