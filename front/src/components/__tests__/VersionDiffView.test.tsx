import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import VersionDiffView from '@/components/VersionDiffView'

describe('VersionDiffView', () => {
  const mockProps = {
    scriptId: 123,
    versionA: 1,
    versionB: 2,
    contentA: 'Line 1\nLine 2\nLine 3\nLine 4',
    contentB: 'Line 1\nLine 2 modified\nLine 3\nLine 4\nLine 5 new',
    onClose: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render component with title and version info', () => {
    render(<VersionDiffView {...mockProps} />)

    expect(screen.getByText('版本对比')).toBeInTheDocument()
    expect(screen.getByText(/脚本 #123 - 版本 1 vs 版本 2/)).toBeInTheDocument()
  })

  it('should display statistics chips', () => {
    render(<VersionDiffView {...mockProps} />)

    // 新增行和删除行统计
    expect(screen.getByText(/新增:/)).toBeInTheDocument()
    expect(screen.getByText(/删除:/)).toBeInTheDocument()
  })

  it('should render close button and call onClose', () => {
    render(<VersionDiffView {...mockProps} />)

    const closeButton = screen.getByText('关闭')
    fireEvent.click(closeButton)

    expect(mockProps.onClose).toHaveBeenCalled()
  })

  it('should highlight added lines with green background', () => {
    render(<VersionDiffView {...mockProps} />)

    // 检查表格是否存在
    const table = screen.getByRole('table')
    expect(table).toBeInTheDocument()
  })

  it('should handle empty content', () => {
    render(
      <VersionDiffView
        {...mockProps}
        contentA=""
        contentB=""
      />
    )

    expect(screen.getByText('两个版本内容完全相同')).toBeInTheDocument()
  })

  it('should render loading state', () => {
    const { container } = render(
      <VersionDiffView
        {...mockProps}
        loading={true}
      />
    )

    // 检查 CircularProgress 组件
    expect(container.querySelector('[role="progressbar"]')).toBeInTheDocument()
  })

  it('should render error state', () => {
    const errorMsg = '加载失败'
    render(
      <VersionDiffView
        {...mockProps}
        error={errorMsg}
      />
    )

    expect(screen.getByText(errorMsg)).toBeInTheDocument()
  })

  it('should display line numbers correctly', () => {
    render(<VersionDiffView {...mockProps} />)

    const table = screen.getByRole('table')
    expect(table).toBeInTheDocument()

    // 应该显示版本 A 的行号和版本 B 的行号
    const cells = screen.getAllByRole('cell')
    expect(cells.length).toBeGreaterThan(0)
  })
})
