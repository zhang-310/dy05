import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { createTheme } from '@mui/material/styles'
import { renderWithProviders } from '@/test/utils'
import { GenerateStageHeader } from './GenerateStageHeader'
import { ScriptFlowCanvas } from './ScriptFlowCanvas'
import ScriptFlowNode from './ScriptFlowNode'
import { ScriptSection, type ScriptSectionProps } from './ScriptSection'
import { SessionInfoTab } from './SessionInfoTab'
import { SectionPropsProvider, buildFlowData, flowToneColor, miniMapNodeColor, nodeTypes } from './scriptflow/ScriptFlowSteps'
import { ScriptEditCard } from './scriptflow/ScriptFlowConfig'
import { liveApi } from '@/api/live'
import { aiApi } from '@/api/ai'
import { brainTrendsCurrent } from '@/api/brain'
import { getRecommendedVirals } from '@/api/shortvideo'

vi.mock('@xyflow/react', () => ({
  Handle: ({ style, id }: { style?: React.CSSProperties; id?: string }) => (
    <span
      data-testid={id === 'to-ai' ? 'flow-handle-to-ai' : 'flow-handle'}
      data-handle-background={String(style?.background ?? '')}
      data-handle-border={String(style?.border ?? '')}
    />
  ),
  ReactFlow: ({ nodes, children, onNodeClick }: { nodes: Array<{ id: string }>; children: React.ReactNode; onNodeClick?: (event: React.MouseEvent, node: { id: string; data?: unknown }) => void }) => (
    <div data-testid="mock-react-flow">
      <button type="button" onClick={(event) => nodes[0] && onNodeClick?.(event, nodes[0])}>select first</button>
      {children}
    </div>
  ),
  Background: () => <div data-testid="mock-flow-background" />,
  Controls: () => <div data-testid="mock-flow-controls" />,
  MiniMap: ({ nodeColor }: { nodeColor: (node: { data?: unknown }) => string }) => (
    <div
      data-testid="script-flow-canvas-minimap-surface"
      data-opening-color={nodeColor({ data: { scriptType: 'opening' } })}
      data-closing-color={nodeColor({ data: { scriptType: 'closing' } })}
      data-transition-color={nodeColor({ data: { scriptType: 'transition' } })}
      data-product-color={nodeColor({ data: { scriptType: 'product' } })}
    />
  ),
  useNodesState: (initialNodes: unknown[]) => [initialNodes, vi.fn(), vi.fn()],
  useEdgesState: (initialEdges: unknown[]) => [initialEdges, vi.fn(), vi.fn()],
  Position: { Top: 'top', Bottom: 'bottom', Left: 'left', Right: 'right' },
}))

vi.mock('@/components/script/ScriptEditor', () => ({
  ScriptEditor: ({ value, onChange }: { value: string; onChange: (v: string) => void }) => (
    <textarea data-testid="script-editor" value={value} onChange={(e) => onChange(e.target.value)} />
  ),
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    presetList: vi.fn(),
    presetSave: vi.fn(),
    presetDelete: vi.fn(),
  },
  saveLiveProduct: vi.fn(),
  deleteLiveProduct: vi.fn(),
  batchSortProducts: vi.fn(),
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbSearch: vi.fn(),
  },
}))

vi.mock('@/api/brain', () => ({
  brainTrendsCurrent: vi.fn(),
}))

vi.mock('@/api/shortvideo', () => ({
  getRecommendedVirals: vi.fn(),
}))

vi.mock('@/api/product', () => ({
  searchProducts: vi.fn(),
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

const builder = {
  llmModels: [
    { id: 11, modelName: 'DeepSeek', modelProvider: 'local' },
    { id: 12, modelName: 'Qwen', modelProvider: 'cloud' },
  ],
  selectedModelId: 11,
  setSelectedModelId: vi.fn(),
  genLoading: false,
  useKbRef: true,
  setUseKbRef: vi.fn(),
  genStyle: 'natural',
  setGenStyle: vi.fn(),
  ipType: 'phenomenal',
  setIpType: vi.fn(),
  materialType: 'joke',
  setMaterialType: vi.fn(),
  scriptModule: 'emotion_drive',
  setScriptModule: vi.fn(),
  retentionStrategy: 'high_suspense',
  setRetentionStrategy: vi.fn(),
  interactionLevel: 'medium',
  setInteractionLevel: vi.fn(),
  hotKeywords: [],
  setHotKeywords: vi.fn(),
} as never

const mockScripts = [
  { id: 1, scriptContent: '大家好欢迎来到直播间', scriptType: 'opening', sequenceNo: 1, executed: 0, estimatedDurationSeconds: 30 },
]

function makeSectionProps(overrides?: Partial<ScriptSectionProps>): ScriptSectionProps {
  return {
    title: '开场区',
    scripts: mockScripts,
    expanded: true,
    onToggle: vi.fn(),
    editingId: null,
    editContent: '',
    violationResult: {},
    checkingId: null,
    saveLibLoading: false,
    onEdit: vi.fn(),
    onSaveEdit: vi.fn(),
    onCancelEdit: vi.fn(),
    onEditContentChange: vi.fn(),
    onCheckViolation: vi.fn(),
    onSaveToLibrary: vi.fn(),
    onMarkExecuted: vi.fn(),
    onDelete: vi.fn(),
    onRefineOpen: vi.fn(),
    ...overrides,
  }
}

describe('live structure surfaces', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(liveApi.presetList).mockResolvedValue([
      {
        id: 91,
        presetName: '大促预设',
        style: 'promotion',
        tone: '',
        modelId: 11,
        useKbRef: true,
        isDefault: false,
        createTime: '2026-05-22T10:00:00',
        ipType: 'phenomenal',
      },
    ] as never)
    vi.mocked(liveApi.presetSave).mockResolvedValue({ id: 92 } as never)
    vi.mocked(liveApi.presetDelete).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.kbSearch).mockResolvedValue([
      { title: '知识库话术', content: '直播间表达要强调修护体验。', score: 0.83 },
    ] as never)
    vi.mocked(brainTrendsCurrent).mockResolvedValue([
      { id: 't1', title: '换季修护', category: '护肤', heatScore: 1234, source: 'douyin' },
    ] as never)
    vi.mocked(getRecommendedVirals).mockResolvedValue([
      { id: 7, title: '爆款开头', hookLine: '你是不是也需要修护？', playCount: 12000, viralScore: 88 },
    ] as never)
  })

  it('marks generate stage header boundaries and prop-only actions', async () => {
    const onNext = vi.fn()
    const localBuilder = {
      ...builder,
      setHotKeywords: vi.fn(),
      setSelectedModelId: vi.fn(),
    } as never

    renderDark(
      <GenerateStageHeader
        builder={localBuilder}
        onPrev={vi.fn()}
        onNext={onNext}
        canAdvance
        kbRefQuery="修护精华"
      />,
    )

    const root = screen.getByTestId('generate-stage-header-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-generate-stage-header-orchestrator')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/model/list-by-task'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/generation-preset/save'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/short-video/viral/recommended'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('direct-script-generation'))
    expect(root).toHaveAttribute('data-no-local-model-fallback', 'true')
    expect(root).toHaveAttribute('data-no-direct-generation', 'true')
    expect(screen.getByTestId('generate-stage-model-selector')).toHaveAttribute('data-contract-source', '/ai/model/list-by-task|/ai/model/list')
    expect(screen.getByTestId('generate-stage-kb-switch')).toHaveAttribute('data-contract-source', 'local-generation-options-store')

    fireEvent.click(screen.getByTestId('trending-topics-open-button'))
    await screen.findByTestId('trending-topics-item')
    fireEvent.click(screen.getByTestId('trending-topics-inject-button'))
    expect((localBuilder as { setHotKeywords: ReturnType<typeof vi.fn> }).setHotKeywords).toHaveBeenCalledWith(['换季修护'])

    fireEvent.click(screen.getByTestId('generate-stage-next-button'))
    expect(onNext).toHaveBeenCalledTimes(1)
  })

  it('blocks next step while generation is running and exposes disabled reason', () => {
    renderDark(
      <GenerateStageHeader
        builder={{ ...builder, genLoading: true } as never}
        onPrev={vi.fn()}
        onNext={vi.fn()}
        canAdvance
        kbRefQuery="修护精华"
      />,
    )

    const next = screen.getByTestId('generate-stage-next-button')
    expect(next).toBeDisabled()
    expect(next).toHaveAttribute('data-disabled-reason', 'generation-running')
    expect(screen.getByTestId('generate-stage-header-root')).toHaveAttribute('data-next-disabled-reason', 'generation-running')
  })

  it('uses a theme-aware generate stage advanced surface in dark mode', () => {
    renderDark(
      <GenerateStageHeader
        builder={builder}
        onPrev={vi.fn()}
        onNext={vi.fn()}
        canAdvance
        kbRefQuery="修护精华"
      />,
    )

    fireEvent.click(screen.getByTestId('generate-stage-advanced-toggle'))

    expect(screen.getByTestId('generate-stage-advanced-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
  })

  it('uses theme-aware script flow node surfaces in dark mode', () => {
    renderDark(
      <ScriptFlowNode
        id="script-1"
        type="scriptFlow"
        selected={false}
        zIndex={0}
        isConnectable
        positionAbsoluteX={0}
        positionAbsoluteY={0}
        dragging={false}
        data={{
          scriptId: 1,
          scriptType: 'product',
          label: '屏障修护精华',
          preview: '产品讲解话术',
          selected: true,
        }}
      />,
    )

    expect(screen.getByTestId('script-flow-node-selected-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })

  it('uses a theme-aware script section header in dark mode', () => {
    renderDark(<ScriptSection {...makeSectionProps()} />)

    expect(screen.getByTestId('script-section-header-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
  })

  it('uses a theme-aware session product table head in dark mode', () => {
    renderDark(
      <SessionInfoTab
        sessionId={18}
        products={[
          {
            id: 1,
            productId: 101,
            productName: '屏障修护精华',
            price: 129,
            position: 1,
            createTime: '2026-05-22T20:00:00',
          },
        ]}
        onRefresh={vi.fn()}
        toast={vi.fn()}
      />,
    )

    expect(screen.getByTestId('session-info-product-table-head-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
  })

  it('uses theme-aware script flow step header surfaces in dark mode', () => {
    const SectionHeader = nodeTypes.sectionHeader as React.ComponentType<{
      data: {
        sectionKey: string
        title: string
        scriptCount: number
        firstScriptType?: string
      }
    }>

    renderDark(
      <SectionHeader
        data={{
          sectionKey: 'transition',
          title: '衔接话术',
          scriptCount: 2,
          firstScriptType: 'transition',
        }}
      />,
    )

    expect(screen.getByTestId('script-flow-section-header-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
    expect(screen.getByTestId('script-flow-section-count-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
  })

  it('uses theme-aware script flow product header surfaces in dark mode', () => {
    const ProductHeader = nodeTypes.productHeader as React.ComponentType<{
      data: {
        product: Record<string, unknown>
        scriptCount: number
      }
    }>

    renderDark(
      <ProductHeader
        data={{
          product: {
            productName: '屏障修护精华',
            productType: 'profit,hot',
            productCategory: '护肤',
            price: 129,
            costPrice: 199,
            profitMarginPct: 0.42,
          },
          scriptCount: 4,
        }}
      />,
    )

    expect(screen.getByTestId('script-flow-product-header-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(230, 244, 234)',
    })
    expect(screen.getByTestId('script-flow-product-count-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(230, 244, 234)',
    })
  })

  it('uses theme-aware script flow canvas minimap colors in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderDark(
      <ScriptFlowCanvas
        scripts={[
          { id: 1, scriptContent: '开场话术', scriptType: 'opening', sequenceNo: 1 },
          { id: 2, scriptContent: '产品话术', scriptType: 'product', sequenceNo: 2 },
          { id: 3, scriptContent: '衔接话术', scriptType: 'transition', sequenceNo: 3 },
          { id: 4, scriptContent: '结尾话术', scriptType: 'closing', sequenceNo: 4 },
        ] as never}
        sortedProducts={[]}
        selectedId={null}
        onSelect={vi.fn()}
      />,
    )

    const minimap = screen.getByTestId('script-flow-canvas-minimap-surface')
    expect(minimap).toHaveAttribute('data-opening-color', '#e3f2fd')
    expect(minimap).toHaveAttribute('data-product-color', '#81c784')
    expect(minimap).toHaveAttribute('data-transition-color', '#ffb74d')
    expect(minimap).toHaveAttribute('data-closing-color', '#f3e5f5')
    for (const legacy of ['#1976d2', '#2e7d32', '#ed6c02', '#9c27b0']) {
      expect([
        minimap.getAttribute('data-opening-color'),
        minimap.getAttribute('data-product-color'),
        minimap.getAttribute('data-transition-color'),
        minimap.getAttribute('data-closing-color'),
      ].join('|')).not.toContain(legacy)
    }
  })

  it('uses theme tone tokens for script flow edges and minimap helpers', () => {
    const theme = createTheme({ palette: { mode: 'dark' } })
    const flow = buildFlowData([
      {
        key: 'opening',
        title: '开场话术',
        scripts: [{ id: 1, scriptType: 'opening', scriptContent: '开场话术' }],
      },
      {
        key: 'transition',
        title: '衔接话术',
        scripts: [{ id: 2, scriptType: 'transition', scriptContent: '衔接话术' }],
      },
      {
        key: 'product-101',
        title: '产品话术',
        product: { productName: '屏障修护精华' },
        scripts: [{ id: 3, scriptType: 'product', scriptContent: '产品话术' }],
      },
    ] as never, null, theme)

    const edgeStrokes = flow.edges.map(edge => String(edge.style?.stroke))
    expect(edgeStrokes).toContain(theme.palette.warning.main)
    expect(edgeStrokes).toContain(theme.palette.success.main)
    expect(flow.edges.map(edge => (edge.data as { edgeTone?: string } | undefined)?.edgeTone)).toContain('warning')
    expect(flow.edges.map(edge => (edge.data as { edgeTone?: string } | undefined)?.edgeTone)).toContain('success')

    const scriptNode = flow.nodes.find(node => node.id === 'script-2')
    expect(scriptNode?.data).toMatchObject({ tone: 'warning', accent: theme.palette.warning.main })
    expect(miniMapNodeColor(scriptNode as never, theme)).toBe(theme.palette.warning.main)
    expect(miniMapNodeColor({ type: 'productHeader', data: {} } as never, theme)).toBe(theme.palette.success.main)
    expect(flowToneColor('secondary', theme)).toBe(theme.palette.secondary.main)

    const serialized = JSON.stringify(flow)
    for (const legacy of ['#1565c0', '#2e7d32', '#ed6c02', '#c2185b', '#7b1fa2', '#64748b', '#94a3b8']) {
      expect(serialized).not.toContain(legacy)
    }
  })

  it('uses the script accent for edit-card AI handles in dark mode', () => {
    renderDark(
      <SectionPropsProvider value={{
        ...makeSectionProps({
          editingId: 1,
          editContent: '编辑中的话术',
          editDurationLimit: 60,
        } as Partial<ScriptSectionProps>),
        canUndo: false,
        canRedo: false,
        onUndo: vi.fn(),
        onRedo: vi.fn(),
        onSaveEdit: vi.fn(),
        onCancelEdit: vi.fn(),
        onEditDurationLimitChange: vi.fn(),
        onEditRequirementChange: vi.fn(),
      } as never}>
        <ScriptEditCard
          row={{ id: 1, scriptContent: '编辑中的话术', scriptType: 'transition' }}
          seqNo="1"
          typeLabel="衔接话术"
          accent="#ffb74d"
        />
      </SectionPropsProvider>,
    )

    expect(screen.getByTestId('script-flow-edit-card-surface')).toHaveAttribute('data-shadow-tone', 'primary')
    const divider = screen.getByTestId('script-flow-edit-toolbar-divider-surface')
    expect(divider).toHaveAttribute('data-divider-tone', 'primary')
    expect(divider).not.toHaveStyle({
      backgroundColor: 'rgb(197, 212, 232)',
    })

    const handleSurface = screen.getByTestId('script-flow-edit-ai-handle-surface')
    expect(handleSurface).toHaveAttribute('data-handle-color', '#ffb74d')
    expect(handleSurface.getAttribute('data-handle-border-color')).not.toContain('#1976d22e')
    expect(screen.getByTestId('flow-handle-to-ai')).toHaveAttribute('data-handle-background', '#ffb74d')
  })
})
