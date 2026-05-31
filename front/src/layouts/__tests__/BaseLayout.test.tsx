import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, renderWithProviders, screen, within } from '@/test/utils'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { BaseLayout } from '../BaseLayout'
import { AdminLayout } from '../AdminLayout'
import { useRecentVisitsStore } from '@/stores/recentVisits'
import { useUserStore } from '@/stores'
import i18n from '@/i18n'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

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

function ShellPage({ title }: { title: string }) {
  return <div>{title}</div>
}

function renderBase(path = '/admin/ai/agent/chat/18') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path="/admin/ai/agent/chat/:id"
          element={
            <BaseLayout drawerContent={({ onNavigate }) => (
              <button type="button" onClick={onNavigate}>测试导航项</button>
            )} />
          }
        >
          <Route index element={<ShellPage title="智能体详情内容" />} />
        </Route>
        <Route path="/admin/dashboard" element={<ShellPage title="首页内容" />} />
        <Route path="/admin/profile" element={<ShellPage title="管理员个人中心" />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderBaseWithRealTheme(path = '/admin/ai/agent/chat/18') {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            path="/admin/ai/agent/chat/:id"
            element={
              <BaseLayout drawerContent={({ onNavigate }) => (
                <button type="button" onClick={onNavigate}>测试导航项</button>
              )} />
            }
          >
            <Route index element={<ShellPage title="智能体详情内容" />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

function renderOrg(path = '/org/dashboard') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path="/org/dashboard"
          element={
            <BaseLayout drawerContent={<button type="button">机构导航项</button>} />
          }
        >
          <Route index element={<ShellPage title="机构首页内容" />} />
        </Route>
        <Route path="/org/live/sessions" element={<ShellPage title="机构直播场次内容" />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderAdmin(path: string) {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/admin" element={<AdminLayout />}>
          <Route path="ai/agent/workflow/edit/:id" element={<ShellPage title="工作流编辑内容" />} />
          <Route path="ai/agent/workflow/list" element={<ShellPage title="工作流列表内容" />} />
          <Route path="shortvideo/subtitles" element={<ShellPage title="字幕工具入口" />} />
          <Route path="shortvideo/editing" element={<ShellPage title="视频剪辑内容" />} />
          <Route path="shortvideo/subtitle-editor/:id" element={<ShellPage title="字幕详情内容" />} />
          <Route path="dashboard" element={<ShellPage title="首页内容" />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('BaseLayout shell navigation', () => {
  beforeEach(() => {
    mediaState.mobile = false
    window.localStorage.clear()
    void i18n.changeLanguage('zh')
    useRecentVisitsStore.setState({ items: [], favorites: [] })
    useUserStore.setState({
      token: 'token-1',
      userInfo: { id: 1, username: 'admin', nickname: '管理员', roles: ['admin'] },
    })
  })

  it('records current page, supports favorite toggle, and navigates from recent menu', () => {
    renderBase()

    expect(screen.getByText('智能体对话')).toBeInTheDocument()
    expect(useRecentVisitsStore.getState().items[0]).toMatchObject({
      path: '/admin/ai/agent/chat/18',
      label: '智能体对话',
    })

    fireEvent.click(screen.getByRole('button', { name: '收藏当前页' }))
    fireEvent.click(screen.getByRole('button', { name: '打开常用与最近访问' }))

    const menu = screen.getByRole('menu')
    expect(within(menu).getAllByText('智能体对话')).toHaveLength(2)
    fireEvent.click(within(menu).getAllByText('智能体对话')[0])

    expect(screen.getByText('智能体详情内容')).toBeInTheDocument()
    expect(useRecentVisitsStore.getState().favorites[0]).toMatchObject({
      path: '/admin/ai/agent/chat/18',
      label: '智能体对话',
    })
  })

  it('opens command palette, searches registered routes, and navigates without fake entity results', () => {
    renderBase()

    fireEvent.click(screen.getByRole('button', { name: '打开命令面板' }))
    const dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })
    expect(dialog).toBeInTheDocument()
    expect(screen.getByText(/当前仅检索已注册页面、收藏和最近访问/)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '智能体' } })
    expect(within(dialog).getByText('智能体对话')).toBeInTheDocument()
    expect(within(dialog).getByText('智能体列表')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '不存在实体' } })
    expect(screen.getByText('无匹配页面，实体搜索暂未接入。')).toBeInTheDocument()
  })

  it('shows whats new popover and persists the local read marker', () => {
    renderBase()

    expect(window.localStorage.getItem('dy-whats-new-last')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: '更新说明' }))

    expect(screen.getByText('近期更新')).toBeInTheDocument()
    expect(screen.getByText(/统一 KPI 四维度接口/)).toBeInTheDocument()
    expect(window.localStorage.getItem('dy-whats-new-last')).toBe('2026-03-21')
  })

  it('switches language from the top bar without affecting navigation state', async () => {
    renderBase()

    fireEvent.click(screen.getByRole('button', { name: '语言' }))
    fireEvent.click(screen.getByRole('menuitem', { name: 'English' }))

    expect(i18n.language).toBe('en')
    expect(screen.getByText('智能体详情内容')).toBeInTheDocument()
  })

  it('opens profile center from the top bar user menu using current shell prefix', () => {
    renderBase()

    fireEvent.click(screen.getByRole('button', { name: '打开用户菜单' }))
    fireEvent.click(screen.getByRole('menuitem', { name: '个人中心' }))

    expect(screen.getByText('管理员个人中心')).toBeInTheDocument()
  })

  it('toggles the real app theme mode from the top bar and persists the preference', () => {
    renderBaseWithRealTheme()

    const sidebar = screen.getByTestId('layout-desktop-sidebar-surface')
    expect(sidebar).toHaveAttribute('data-shadow-tone', 'sidebar-elevation')
    expect(sidebar.getAttribute('style') ?? '').not.toContain('rgba(0,0,0,0.06)')
    expect(screen.getByRole('button', { name: '切换暗色主题' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '切换暗色主题' }))

    expect(screen.getByRole('button', { name: '切换亮色主题' })).toBeInTheDocument()
    expect(window.localStorage.getItem('dy-theme-mode')).toBe('dark')
    expect(sidebar).toHaveAttribute('data-shadow-tone', 'sidebar-elevation')
    expect(screen.getByText('智能体详情内容')).toBeInTheDocument()
  })

  it('toggles command palette with Ctrl+K and keeps org shell shortcuts inside org routes', () => {
    renderOrg()

    fireEvent.keyDown(window, { key: 'k', ctrlKey: true })
    const dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })
    expect(dialog).toBeInTheDocument()
    expect(within(dialog).getByText('/org/dashboard')).toBeInTheDocument()
    expect(within(dialog).getByText('直播场次')).toBeInTheDocument()
    expect(within(dialog).queryByText('AI 仪表盘')).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '直播' } })
    fireEvent.click(screen.getByText('直播场次'))
    expect(screen.getByText('机构直播场次内容')).toBeInTheDocument()
  })

  it('closes the mobile drawer after a drawer navigation callback', () => {
    mediaState.mobile = true
    renderBase()

    fireEvent.click(screen.getByRole('button', { name: '打开导航菜单' }))
    expect(screen.getAllByText('测试导航项').length).toBeGreaterThan(0)

    const drawerItems = screen.getAllByText('测试导航项')
    fireEvent.click(drawerItems[drawerItems.length - 1])
    expect(screen.queryByRole('presentation')).not.toBeInTheDocument()
  })

  it('highlights workflow edit routes under workflow navigation', () => {
    renderAdmin('/admin/ai/agent/workflow/edit/12')

    expect(screen.getByText('工作流编辑')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /工作流编排/ })).toHaveClass('Mui-selected')
  })

  it('keeps shortvideo tools out of the admin shell after role split', () => {
    renderAdmin('/admin/dashboard')

    expect(screen.queryByText('短视频')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /字幕编辑/ })).not.toBeInTheDocument()
    expect(screen.queryByText('字幕详情内容')).not.toBeInTheDocument()
  })

  it('does not render legacy admin shortvideo details from the admin navigation shell', () => {
    renderAdmin('/admin/shortvideo/subtitle-editor/18')

    expect(screen.getByText('字幕详情内容')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /字幕编辑/ })).not.toBeInTheDocument()
  })
})
