import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import WorkflowNode from '../WorkflowNode'

vi.mock('@xyflow/react', () => ({
  Handle: ({ type, position }: { type: string; position: string }) => (
    <span data-testid={`workflow-handle-${type}-${position}`} />
  ),
  Position: {
    Left: 'left',
    Right: 'right',
  },
}))

function renderNode(data: Record<string, unknown>) {
  return renderWithProviders(
    <AppThemeProvider>
      <WorkflowNode id="node-1" data={data} type="workflow" selected={false} dragging={false} zIndex={1} isConnectable xPos={0} yPos={0} />
    </AppThemeProvider>,
  )
}

describe('WorkflowNode', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
  })

  it('uses theme-aware status surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    const { rerender } = renderNode({ status: 'processing', label: '生成脚本', icon: 'AI', model: 'deepseek' })
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-tone', 'primary')
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-border', '#e3f2fd')
    expect(screen.getByText('执行中...')).toBeInTheDocument()
    expect(screen.getByText('模型: deepseek')).toBeInTheDocument()

    rerender(
      <AppThemeProvider>
        <WorkflowNode id="node-1" data={{ status: 'completed', label: '生成脚本' }} type="workflow" selected={false} dragging={false} zIndex={1} isConnectable xPos={0} yPos={0} />
      </AppThemeProvider>,
    )
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-tone', 'success')
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-border', '#81c784')
    expect(screen.getByText('已完成')).toBeInTheDocument()

    rerender(
      <AppThemeProvider>
        <WorkflowNode id="node-1" data={{ status: 'failed', label: '生成脚本' }} type="workflow" selected={false} dragging={false} zIndex={1} isConnectable xPos={0} yPos={0} />
      </AppThemeProvider>,
    )
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-tone', 'error')
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-border', '#e57373')

    const serialized = document.body.innerHTML
    for (const legacy of ['#e0e0e0', '#fff', '#1976d2', '#2e7d32', '#d32f2f', 'rgba(25, 118, 210', 'rgba(46, 125, 50', 'rgba(211, 47, 47']) {
      expect(serialized).not.toContain(legacy)
    }
  })

  it('keeps selected and idle nodes explicit', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    const { rerender } = renderNode({ status: 'idle', label: '待处理' })
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-tone', 'default')
    expect(screen.getByText('待执行')).toBeInTheDocument()

    rerender(
      <AppThemeProvider>
        <WorkflowNode id="node-1" data={{ status: 'idle', selected: true, label: '待处理' }} type="workflow" selected={false} dragging={false} zIndex={1} isConnectable xPos={0} yPos={0} />
      </AppThemeProvider>,
    )
    expect(screen.getByTestId('workflow-node-surface')).toHaveAttribute('data-workflow-border', '#e3f2fd')
  })
})
