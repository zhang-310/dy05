import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders } from '@/test/utils'
import { ScriptFlowCanvas } from './ScriptFlowCanvas'
import ScriptFlowNode from './ScriptFlowNode'
import type { LiveProduct, LiveScript } from '@/api/live'

let latestNodes: Array<{ id: string; data?: unknown }> = []
let latestEdges: Array<{ id: string; source: string; target: string; animated?: boolean }> = []

vi.mock('@xyflow/react', () => ({
  Handle: () => <span data-testid="flow-handle" />,
  ReactFlow: ({
    nodes,
    edges,
    children,
    onNodeClick,
  }: {
    nodes: Array<{ id: string; data?: unknown }>
    edges: Array<{ id: string; source: string; target: string; animated?: boolean }>
    children: React.ReactNode
    onNodeClick?: (event: React.MouseEvent, node: { id: string; data?: unknown }) => void
  }) => {
    latestNodes = nodes
    latestEdges = edges
    return (
      <div data-testid="mock-react-flow" data-node-count={nodes.length} data-edge-count={edges.length}>
        {nodes.map((node) => (
          <button
            data-testid={`mock-node-${node.id}`}
            data-label={String((node.data as { label?: string })?.label ?? '')}
            data-preview={String((node.data as { preview?: string })?.preview ?? '')}
            data-collapsed={String((node.data as { collapsed?: boolean })?.collapsed ?? false)}
            data-selected={String((node.data as { selected?: boolean })?.selected ?? false)}
            key={node.id}
            type="button"
            onClick={(event) => onNodeClick?.(event, node)}
          >
            {String((node.data as { label?: string })?.label ?? node.id)}
          </button>
        ))}
        {children}
      </div>
    )
  },
  Background: () => <div data-testid="mock-flow-background" />,
  Controls: () => <div data-testid="mock-flow-controls" />,
  MiniMap: ({ nodeColor }: { nodeColor: (node: { data?: unknown }) => string }) => (
    <div
      data-testid="script-flow-minimap-contract"
      data-opening-color={nodeColor({ data: { scriptType: 'opening' } })}
      data-product-color={nodeColor({ data: { scriptType: 'product' } })}
    />
  ),
  useNodesState: (initialNodes: unknown[]) => [initialNodes, vi.fn(), vi.fn()],
  useEdgesState: (initialEdges: unknown[]) => [initialEdges, vi.fn(), vi.fn()],
  Position: { Top: 'top', Bottom: 'bottom' },
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(<AppThemeProvider>{ui}</AppThemeProvider>)
}

const products: LiveProduct[] = [
  { id: 11, productId: 101, productName: '屏障修护精华', position: 1 } as LiveProduct,
  { id: 12, productId: 102, productName: '敏感肌面霜', position: 2 } as LiveProduct,
]

const scripts: LiveScript[] = [
  { id: 1, scriptType: 'opening', scriptContent: '欢迎来到直播间', sequenceNo: 1 } as LiveScript,
  { id: 2, scriptType: 'product', productId: 101, scriptContent: '屏障修护精华产品讲解', sequenceNo: 2 } as LiveScript,
  { id: 3, scriptType: 'transition', scriptContent: '从精华自然过渡到面霜', sequenceNo: 3 } as LiveScript,
  { id: 4, scriptType: 'closing', scriptContent: '', sequenceNo: 4 } as LiveScript,
]

describe('ScriptFlowCanvas and ScriptFlowNode contract', () => {
  beforeEach(() => {
    latestNodes = []
    latestEdges = []
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('marks canvas as a props-only visualizer and selects script nodes by id', () => {
    const onSelect = vi.fn()

    renderDark(
      <ScriptFlowCanvas
        scripts={scripts}
        sortedProducts={products}
        selectedId={3}
        onSelect={onSelect}
      />,
    )

    const root = screen.getByTestId('script-flow-canvas-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-script-flow-canvas-props-visualizer')
    expect(root).toHaveAttribute('data-ready-endpoints', '/live/script/by-session|/live/product/by-session')
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('direct-api-call'))
    expect(root).toHaveAttribute('data-no-direct-api', 'true')
    expect(root).toHaveAttribute('data-no-local-script-fallback', 'true')
    expect(root).toHaveAttribute('data-script-count', '4')
    expect(root).toHaveAttribute('data-product-count', '2')
    expect(root).toHaveAttribute('data-selected-id', '3')

    expect(screen.getByTestId('script-flow-react-flow-surface')).toHaveAttribute('data-edge-count', '3')
    expect(screen.getByTestId('mock-node-script-2')).toHaveAttribute('data-label', '屏障修护精华')
    expect(screen.getByTestId('mock-node-script-3')).toHaveAttribute('data-label', '衔接 屏障修护精华 → B')
    expect(screen.getByTestId('mock-node-script-3')).toHaveAttribute('data-selected', 'true')
    expect(latestEdges.find(edge => edge.target === 'script-3')?.animated).toBe(true)

    fireEvent.click(screen.getByTestId('mock-node-script-3'))
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 3, scriptType: 'transition' }))
  })

  it('shows empty state without local fallback and delegates generation to parent', () => {
    const onGenerateFull = vi.fn()

    renderDark(
      <ScriptFlowCanvas
        scripts={[]}
        sortedProducts={products}
        selectedId={null}
        onSelect={vi.fn()}
        onGenerateFull={onGenerateFull}
      />,
    )

    const empty = screen.getByTestId('script-flow-empty-state')
    expect(empty).toHaveAttribute('data-contract-scope', 'live-script-flow-empty-state')
    expect(empty).toHaveAttribute('data-can-generate', 'true')
    expect(empty).toHaveAttribute('data-no-local-script-fallback', 'true')
    expect(screen.queryByTestId('mock-react-flow')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('script-flow-empty-generate-button'))
    expect(onGenerateFull).toHaveBeenCalledTimes(1)
  })

  it('auto-collapses large script lists except the selected node', () => {
    const largeScripts = Array.from({ length: 26 }, (_, index) => ({
      id: index + 1,
      scriptType: index === 0 ? 'opening' : 'product',
      productId: 101,
      scriptContent: `第 ${index + 1} 段话术`,
      sequenceNo: index + 1,
    })) as LiveScript[]

    renderDark(
      <ScriptFlowCanvas
        scripts={largeScripts}
        sortedProducts={products}
        selectedId={7}
        onSelect={vi.fn()}
      />,
    )

    expect(screen.getByTestId('script-flow-canvas-root')).toHaveAttribute('data-collapsed-count', '25')
    expect(screen.getByTestId('mock-node-script-1')).toHaveAttribute('data-collapsed', 'true')
    expect(screen.getByTestId('mock-node-script-7')).toHaveAttribute('data-collapsed', 'false')
    expect(latestNodes).toHaveLength(26)
  })

  it('marks node renderer contract, collapse action and preview fallback', () => {
    const onCollapseToggle = vi.fn()

    renderDark(
      <ScriptFlowNode
        id="script-9"
        type="scriptFlow"
        selected={false}
        zIndex={0}
        isConnectable
        positionAbsoluteX={0}
        positionAbsoluteY={0}
        dragging={false}
        data={{
          scriptId: 9,
          scriptType: 'unknown-type',
          label: '未知话术',
          preview: '',
          selected: false,
          collapsed: false,
          onCollapseToggle,
        }}
      />,
    )

    const node = screen.getByTestId('script-flow-node-surface')
    expect(node).toHaveAttribute('data-contract-scope', 'live-script-flow-node-props-renderer')
    expect(node).toHaveAttribute('data-contract-source', 'react-flow-node-data')
    expect(node).toHaveAttribute('data-script-id', '9')
    expect(node).toHaveAttribute('data-script-type', 'unknown-type')
    expect(node).toHaveAttribute('data-selected', 'false')
    expect(node).toHaveAttribute('data-collapsed', 'false')
    expect(node).toHaveAttribute('data-has-preview', 'false')
    expect(node).toHaveAttribute('data-no-direct-api', 'true')
    expect(screen.getByTestId('script-flow-node-preview')).toHaveTextContent('[待填写]')

    const button = screen.getByTestId('script-flow-node-collapse-button')
    expect(button).toHaveAttribute('data-contract-source', 'onCollapseToggle-prop')
    expect(button).toHaveAttribute('data-action', 'collapse')
    fireEvent.click(button)
    expect(onCollapseToggle).toHaveBeenCalledTimes(1)
  })

  it('keeps collapsed nodes compact and hides the preview body', () => {
    renderDark(
      <ScriptFlowNode
        id="script-10"
        type="scriptFlow"
        selected={false}
        zIndex={0}
        isConnectable
        positionAbsoluteX={0}
        positionAbsoluteY={0}
        dragging={false}
        data={{
          scriptId: 10,
          scriptType: 'closing',
          label: '结尾话术',
          preview: '结尾福利说明',
          selected: true,
          collapsed: true,
          onCollapseToggle: vi.fn(),
        }}
      />,
    )

    const node = screen.getByTestId('script-flow-node-selected-surface')
    expect(node).toHaveAttribute('data-contract-scope', 'live-script-flow-node-props-renderer')
    expect(node).toHaveAttribute('data-script-type', 'closing')
    expect(node).toHaveAttribute('data-selected', 'true')
    expect(node).toHaveAttribute('data-collapsed', 'true')
    expect(node).toHaveAttribute('data-has-preview', 'true')
    expect(screen.getByTestId('script-flow-node-collapse-button')).toHaveAttribute('data-action', 'expand')
    expect(screen.queryByTestId('script-flow-node-preview')).not.toBeInTheDocument()
  })
})
