import { describe, it, expect, vi, beforeEach } from 'vitest'
import type React from 'react'
import { fireEvent, screen } from '@testing-library/react'
import { createTheme } from '@mui/material/styles'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders } from '@/test/utils'
import {
  SectionPropsProvider,
  buildFlowData,
  miniMapNodeColor,
  nodeTypes,
} from './scriptflow/ScriptFlowSteps'
import { ScriptEditCard } from './scriptflow/ScriptFlowConfig'
import type { ScriptSectionProps } from './ScriptSection'

vi.mock('@xyflow/react', () => ({
  Handle: ({ style, id }: { style?: React.CSSProperties; id?: string }) => (
    <span
      data-testid={id === 'to-ai' ? 'flow-handle-to-ai' : 'flow-handle'}
      data-handle-background={String(style?.background ?? '')}
      data-handle-border={String(style?.border ?? '')}
    />
  ),
  Position: { Top: 'top', Bottom: 'bottom', Left: 'left', Right: 'right' },
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(<AppThemeProvider>{ui}</AppThemeProvider>)
}

function makeSectionProps(overrides?: Partial<ScriptSectionProps>): ScriptSectionProps {
  return {
    title: '产品话术',
    scripts: [],
    expanded: true,
    onToggle: vi.fn(),
    editingId: null,
    editContent: '编辑中的直播话术',
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
    onOpenAnalyst: vi.fn(),
    onUndo: vi.fn(),
    onRedo: vi.fn(),
    canUndo: true,
    canRedo: true,
    focusedScriptId: 301,
    onFocusScript: vi.fn(),
    onRegenerateSingle: vi.fn(),
    editDurationLimit: 60,
    editRequirement: '',
    onEditDurationLimitChange: vi.fn(),
    onEditRequirementChange: vi.fn(),
    ...overrides,
  }
}

describe('scriptflow submodule contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('marks section and product header nodes as props-only flow surfaces', () => {
    const SectionHeader = nodeTypes.sectionHeader as React.ComponentType<{
      data: {
        sectionKey: string
        title: string
        scriptCount: number
        firstScriptType?: string
      }
    }>
    const ProductHeader = nodeTypes.productHeader as React.ComponentType<{
      data: {
        product: Record<string, unknown>
        scriptCount: number
      }
    }>

    renderDark(
      <>
        <SectionHeader data={{ sectionKey: 'transition', title: '衔接话术', scriptCount: 2, firstScriptType: 'transition' }} />
        <ProductHeader data={{ product: { id: 101, productId: 101, productName: '屏障修护精华', productType: 'profit,hot', price: 129 }, scriptCount: 4 }} />
      </>,
    )

    const section = screen.getByTestId('script-flow-section-header-node')
    expect(section).toHaveAttribute('data-contract-scope', 'live-script-flow-section-header-node')
    expect(section).toHaveAttribute('data-section-key', 'transition')
    expect(section).toHaveAttribute('data-script-count', '2')
    expect(section).toHaveAttribute('data-tone', 'warning')
    expect(section).toHaveAttribute('data-no-direct-api', 'true')

    const product = screen.getByTestId('script-flow-product-header-node')
    expect(product).toHaveAttribute('data-contract-scope', 'live-script-flow-product-header-node')
    expect(product).toHaveAttribute('data-product-id', '101')
    expect(product).toHaveAttribute('data-product-name', '屏障修护精华')
    expect(product).toHaveAttribute('data-no-local-product-fallback', 'true')
  })

  it('marks script branch node actions as context-owned and never direct API calls', () => {
    const ScriptBranch = nodeTypes.scriptBranch as React.ComponentType<{
      data: {
        row: Record<string, unknown>
        idx: number
        accent?: string
        tone?: string
      }
    }>
    const props = makeSectionProps()

    renderDark(
      <SectionPropsProvider value={props as never}>
        <ScriptBranch
          data={{
            row: {
              id: 301,
              sequenceNo: 3,
              scriptType: 'product',
              scriptContent: '产品讲解话术',
              executed: 0,
              aiGenerated: 1,
            },
            idx: 0,
            accent: '#81c784',
            tone: 'success',
          }}
        />
      </SectionPropsProvider>,
    )

    const root = screen.getByTestId('script-flow-script-branch-node')
    expect(root).toHaveAttribute('data-contract-scope', 'live-script-flow-script-branch-node')
    expect(root).toHaveAttribute('data-script-id', '301')
    expect(root).toHaveAttribute('data-script-type', 'product')
    expect(root).toHaveAttribute('data-focused', 'true')
    expect(root).toHaveAttribute('data-no-direct-api', 'true')
    expect(screen.getByTestId('script-flow-branch-ai-handle-surface')).toHaveAttribute('data-contract-source', 'focusedScriptId-prop')

    fireEvent.click(screen.getByTestId('script-flow-branch-edit-button'))
    expect(props.onEdit).toHaveBeenCalledWith(301, '产品讲解话术', expect.objectContaining({ id: 301 }))
    fireEvent.click(screen.getByTestId('script-flow-branch-regenerate-button'))
    expect(props.onRegenerateSingle).toHaveBeenCalledWith(301)
    fireEvent.click(screen.getByTestId('script-flow-branch-analyst-button'))
    expect(props.onOpenAnalyst).toHaveBeenCalledWith(expect.objectContaining({ id: 301 }))
    fireEvent.click(screen.getByTestId('script-flow-branch-refine-button'))
    expect(props.onRefineOpen).toHaveBeenCalledWith({ scriptId: 301 })
    fireEvent.click(screen.getByTestId('script-flow-branch-executed-button'))
    expect(props.onMarkExecuted).toHaveBeenCalledWith(301, 0)
    fireEvent.click(screen.getByTestId('script-flow-branch-save-library-button'))
    expect(props.onSaveToLibrary).toHaveBeenCalledWith(301)
    fireEvent.click(screen.getByTestId('script-flow-branch-delete-button'))
    expect(props.onDelete).toHaveBeenCalledWith(expect.objectContaining({ id: 301 }))
  })

  it('marks edit card contract and delegates all edit actions to context callbacks', () => {
    const props = makeSectionProps({
      editingId: 301,
      editContent: '需要保存的新版话术',
      violationResult: { 301: { passed: false, violations: ['绝对化表达'] } },
    })

    renderDark(
      <SectionPropsProvider value={props as never}>
        <ScriptEditCard
          row={{ id: 301, scriptContent: '旧话术', scriptType: 'transition' }}
          seqNo="3"
          typeLabel="衔接话术"
          accent="#ffb74d"
          accentTone="warning"
        />
      </SectionPropsProvider>,
    )

    const root = screen.getByTestId('script-flow-edit-card-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-script-flow-edit-card-context-editor')
    expect(root).toHaveAttribute('data-script-id', '301')
    expect(root).toHaveAttribute('data-violation-count', '1')
    expect(root).toHaveAttribute('data-no-direct-api', 'true')

    fireEvent.click(screen.getByTestId('script-flow-edit-undo-button'))
    expect(props.onUndo).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByTestId('script-flow-edit-redo-button'))
    expect(props.onRedo).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByTestId('script-flow-edit-check-button'))
    expect(props.onCheckViolation).toHaveBeenCalledWith(301)
    fireEvent.change(screen.getByTestId('script-flow-edit-content-input'), { target: { value: '二次编辑' } })
    expect(props.onEditContentChange).toHaveBeenCalledWith('二次编辑')
    fireEvent.change(screen.getByTestId('script-flow-edit-duration-limit-input'), { target: { value: '75' } })
    expect(props.onEditDurationLimitChange).toHaveBeenCalledWith(75)
    fireEvent.change(screen.getByTestId('script-flow-edit-requirement-select'), { target: { value: 'highlight_benefit' } })
    expect(props.onEditRequirementChange).toHaveBeenCalledWith('highlight_benefit')
    fireEvent.click(screen.getByTestId('script-flow-edit-save-button'))
    expect(props.onSaveEdit).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByTestId('script-flow-edit-cancel-button'))
    expect(props.onCancelEdit).toHaveBeenCalledTimes(1)
  })

  it('builds deterministic nodes and typed edges without local fallbacks', () => {
    const theme = createTheme({ palette: { mode: 'dark' } })
    const flow = buildFlowData([
      {
        key: 'opening',
        title: '开场话术',
        scripts: [{ id: 1, scriptType: 'opening', scriptContent: '开场话术' }],
      },
      {
        key: 'product-101',
        title: '产品话术',
        product: { id: 101, productId: 101, productName: '屏障修护精华', productType: 'profit' },
        scripts: [{ id: 2, scriptType: 'product', scriptContent: '产品话术' }],
      },
    ] as never, null, theme)

    expect(flow.nodes.map(node => node.id)).toEqual(['opening', 'script-1', 'product-101', 'script-2'])
    expect(flow.edges.map(edge => edge.id)).toEqual(['branch-opening-1', 'trunk-opening-product-101', 'branch-product-101-2'])
    expect(flow.edges.map(edge => (edge.data as { edgeTone?: string } | undefined)?.edgeTone)).toEqual(['primary', 'success', 'success'])
    expect(miniMapNodeColor(flow.nodes.find(node => node.id === 'script-2') as never, theme)).toBe(theme.palette.success.main)
    expect(JSON.stringify(flow)).not.toContain('mock')
  })
})
