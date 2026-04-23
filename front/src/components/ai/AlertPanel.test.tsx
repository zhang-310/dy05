import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { AlertPanel } from './AlertPanel'

describe('AlertPanel', () => {
  it('renders title', () => {
    renderWithProviders(<AlertPanel items={[]} title="异常告警" />)
    expect(screen.getByText('异常告警')).toBeInTheDocument()
  })

  it('shows empty message when no items', () => {
    renderWithProviders(<AlertPanel items={[]} emptyMessage="暂无告警" />)
    expect(screen.getByText('暂无告警')).toBeInTheDocument()
  })

  it('shows success message when all items ok', () => {
    renderWithProviders(
      <AlertPanel
        items={[{ component: 'redis', ok: true }]}
        successMessage="运行正常"
      />
    )
    expect(screen.getByText('运行正常')).toBeInTheDocument()
  })

  it('shows warning when unhealthy items exist', () => {
    renderWithProviders(
      <AlertPanel
        items={[
          { component: 'milvus', ok: false, message: '连接超时' },
        ]}
      />
    )
    expect(screen.getByText(/Milvus 异常/)).toBeInTheDocument()
    expect(screen.getByText(/连接超时/)).toBeInTheDocument()
  })
})
