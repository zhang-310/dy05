import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { PageHeader } from './PageHeader'

describe('PageHeader', () => {
  it('renders title and subtitle', () => {
    renderWithProviders(<PageHeader title="测试标题" subtitle="测试副标题" />)
    expect(screen.getByText('测试标题')).toBeInTheDocument()
    expect(screen.getByText('测试副标题')).toBeInTheDocument()
  })

  it('renders actions when provided', () => {
    renderWithProviders(
      <PageHeader
        title="标题"
        actions={<button>操作按钮</button>}
      />
    )
    expect(screen.getByRole('button', { name: '操作按钮' })).toBeInTheDocument()
  })

  it('renders breadcrumbs when provided', () => {
    renderWithProviders(
      <PageHeader
        title="页面"
        breadcrumbs={[{ label: '首页', href: '/' }, { label: '当前页' }]}
      />
    )
    expect(screen.getByText('首页')).toBeInTheDocument()
    expect(screen.getByText('当前页')).toBeInTheDocument()
  })
})
