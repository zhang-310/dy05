import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { CommandPalette } from '../CommandPalette'

function renderPalette(pathname = '/admin/dashboard') {
  const onNavigate = vi.fn()
  const onClose = vi.fn()

  const result = render(
    <CommandPalette
      open
      pathname={pathname}
      recentItems={[
        { path: '/admin/system/api-log', label: 'API 日志最近访问', at: 2 },
        { path: '/admin/payment/orders', label: '订单最近访问', at: 1 },
      ]}
      favoriteItems={[
        { path: '/admin/ai/knowledge', label: '知识库收藏' },
        { path: '/admin/system/api-log', label: 'API 日志收藏' },
      ]}
      onClose={onClose}
      onNavigate={onNavigate}
    />,
  )

  return { ...result, onClose, onNavigate }
}

describe('CommandPalette', () => {
  it('searches the admin platform page index without business-shell entries', () => {
    renderPalette()
    const dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '企业微信' } })
    expect(within(dialog).getByText('企业微信推送')).toBeInTheDocument()
    expect(within(dialog).getByText('/admin/wecom')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '质量脚本' } })
    expect(within(dialog).queryByText('质量脚本库')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('/admin/shortvideo/benchmark/quality-scripts')).not.toBeInTheDocument()
    expect(within(dialog).getByTestId('command-palette-results')).toHaveAttribute('data-empty-state', 'entity-search-unsupported')

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '用户管理' } })
    expect(within(dialog).getByText('用户管理')).toBeInTheDocument()
    expect(within(dialog).getByText('/admin/auth/users')).toBeInTheDocument()
  })

  it('keeps favorites and recent visits first while deduping by path', () => {
    renderPalette()

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: 'api 日志' } })
    const dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })

    expect(within(dialog).getByText('API 日志收藏')).toBeInTheDocument()
    expect(within(dialog).queryByText('API 日志最近访问')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('API 日志')).not.toBeInTheDocument()
  })

  it('exposes route-only search contracts and explicit entity-search downgrade', () => {
    renderPalette()
    const dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })
    const root = within(dialog).getByTestId('command-palette-root')

    expect(root).toHaveAttribute('data-contract-scope', 'global-route-command-palette')
    expect(root).toHaveAttribute('data-route-scope', 'admin')
    expect(root).toHaveAttribute('data-ready-search-sources', 'registered-pages|favorite-pages|recent-visits')
    expect(root).toHaveAttribute('data-unsupported-entity-search', 'live-entities|product-entities|script-entities|shortvideo-entities|douyin-entities')
    expect(root).toHaveAttribute('data-priority-order', 'favorites|recent-visits|registered-pages')
    expect(root).toHaveAttribute('data-dedupe-by', 'path')
    expect(within(dialog).getByTestId('command-palette-source-contract')).toHaveAttribute('data-no-entity-search-fallback', 'true')

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '不存在的商品实体' } })
    expect(within(dialog).getByTestId('command-palette-results')).toHaveAttribute('data-empty-state', 'entity-search-unsupported')
    expect(within(dialog).getByTestId('command-palette-entity-search-downgrade')).toHaveAttribute('data-no-local-entity-results', 'true')
    expect(within(dialog).getByText('无匹配页面，实体搜索暂未接入。')).toBeInTheDocument()
  })

  it('does not leak admin entries into org or talent shells', () => {
    const { unmount } = renderPalette('/org/dashboard')
    let dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })
    expect(within(dialog).getByText('工作台')).toBeInTheDocument()
    expect(within(dialog).getByText('复盘审核')).toBeInTheDocument()
    expect(within(dialog).queryByText('AI 仪表盘')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('订单最近访问')).not.toBeInTheDocument()
    expect(within(dialog).getByTestId('command-palette-root')).toHaveAttribute('data-route-scope', 'org')
    unmount()

    renderPalette('/talent/dashboard')
    dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })
    expect(within(dialog).getByText('工作台')).toBeInTheDocument()
    expect(within(dialog).getByText('直播场次')).toBeInTheDocument()
    expect(within(dialog).queryByText('用户管理')).not.toBeInTheDocument()
    expect(within(dialog).queryByText('知识库收藏')).not.toBeInTheDocument()
    expect(within(dialog).getByTestId('command-palette-root')).toHaveAttribute('data-route-scope', 'talent')
  })

  it('exposes shortvideo entries only inside the talent shell', () => {
    renderPalette('/talent/dashboard')
    const dialog = screen.getByRole('dialog', { name: '命令面板 · 全局搜索' })

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '爆款视频库' } })

    expect(within(dialog).getByText('爆款视频库')).toBeInTheDocument()
    expect(within(dialog).getByText('/talent/shortvideo/viral-videos')).toBeInTheDocument()
    expect(within(dialog).queryByText('/admin/shortvideo/viral-videos')).not.toBeInTheDocument()
  })

  it('navigates to a searched registered page and resets the query', () => {
    const { onNavigate } = renderPalette()

    fireEvent.change(screen.getByLabelText('搜索页面、路径、收藏或最近访问'), { target: { value: '订阅管理' } })
    fireEvent.click(screen.getByText('订阅管理'))

    expect(onNavigate).toHaveBeenCalledWith('/admin/payment/subscription')
    expect(screen.getByLabelText('搜索页面、路径、收藏或最近访问')).toHaveValue('')
  })
})
