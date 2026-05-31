import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { ScriptPanel, type ScriptPanelProps, type ScriptSectionData } from './ScriptPanel'
import type { ScriptSectionProps } from './ScriptSection'

vi.mock('@/components/script/ScriptEditor', () => ({
  ScriptEditor: ({ value, onChange }: { value: string; onChange: (v: string) => void }) => (
    <textarea data-testid="script-editor" value={value} onChange={(e) => onChange(e.target.value)} />
  ),
}))

const scripts = [
  { id: 1, sessionId: 18, scriptContent: '欢迎来到直播间', scriptType: 'opening', sequenceNo: 1, executed: 0, aiGenerated: 1, estimatedDurationSeconds: 30 },
  { id: 2, sessionId: 18, scriptContent: '这款精华主打屏障修护', scriptType: 'product', sequenceNo: 2, executed: 0, aiGenerated: 1, estimatedDurationSeconds: 60 },
]

const sectionProps: Omit<ScriptSectionProps, 'title' | 'subtitle' | 'scripts' | 'expanded' | 'onToggle'> = {
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
  onOpenAnalyst: vi.fn(),
  selectedScriptIds: new Set([1]),
  onToggleSelect: vi.fn(),
}

function makeProps(overrides?: Partial<ScriptPanelProps>): ScriptPanelProps {
  const scriptSections: ScriptSectionData[] = [
    { key: 'opening', title: '开场话术', scripts: [scripts[0] as never] },
    { key: 'product-0', title: '1. 屏障修护精华', subtitle: '爆款', scripts: [scripts[1] as never] },
  ]
  return {
    genStyle: 'natural',
    genLoading: false,
    fullGenProgress: null,
    totalEstSeconds: 90,
    scripts: scripts as never,
    scriptSections,
    expandedSections: new Set(['opening', 'product-0']),
    chainPrompt: null,
    onGenStyleChange: vi.fn(),
    onGenerateOpening: vi.fn(),
    onProductGenOpen: vi.fn(),
    onEmotionalOpen: vi.fn(),
    onGenerateFull: vi.fn(),
    onCheckSimilarity: vi.fn(),
    onGenerateSkeleton: vi.fn(),
    onSaveToLibrary: vi.fn(),
    onExport: vi.fn(),
    onToggleSection: vi.fn(),
    onChainPromptAdjust: vi.fn(),
    onChainPromptDismiss: vi.fn(),
    onBatchOpen: vi.fn(),
    productsCount: 1,
    selectedScriptIdsCount: 1,
    similarityLoading: false,
    skeletonLoading: false,
    saveLibLoading: false,
    exportLoading: false,
    sectionProps,
    useKbRef: true,
    onUseKbRefChange: vi.fn(),
    ...overrides,
  }
}

function renderPanel(overrides?: Partial<ScriptPanelProps>) {
  const props = makeProps(overrides)
  renderWithProviders(<ScriptPanel {...props} />)
  return props
}

describe('ScriptPanel contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks script panel orchestration contract and toolbar endpoint ownership', () => {
    const props = renderPanel()

    const root = screen.getByTestId('live-script-panel-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-script-panel-props-orchestrator')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/generate-full-pipelined-sse'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script/export'))
    expect(root).toHaveAttribute('data-context-endpoints', '/live/session/get|/live/product/by-session|/live/script/by-session')
    expect(root).toHaveAttribute('data-no-local-script-fallback', 'true')
    expect(screen.getByTestId('live-script-panel-product-generate-open-button')).toHaveAttribute('data-disabled-reason', 'ready')
    expect(screen.getByTestId('live-script-panel-generate-full-button')).toHaveAttribute('data-contract-source', '/live/ai/generate-full-pipelined-sse')
    expect(screen.getByTestId('live-script-panel-save-library-button')).toHaveAttribute('data-contract-source', '/live/script/save-batch-to-library')

    fireEvent.click(screen.getByTestId('live-script-panel-generate-opening-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-product-generate-open-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-emotional-open-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-generate-full-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-similarity-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-skeleton-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-save-library-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-export-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-batch-open-button'))

    expect(props.onGenerateOpening).toHaveBeenCalled()
    expect(props.onProductGenOpen).toHaveBeenCalled()
    expect(props.onEmotionalOpen).toHaveBeenCalled()
    expect(props.onGenerateFull).toHaveBeenCalled()
    expect(props.onCheckSimilarity).toHaveBeenCalled()
    expect(props.onGenerateSkeleton).toHaveBeenCalled()
    expect(props.onSaveToLibrary).toHaveBeenCalled()
    expect(props.onExport).toHaveBeenCalled()
    expect(props.onBatchOpen).toHaveBeenCalled()
  })

  it('exposes disabled reasons and empty state without local scripts', () => {
    renderPanel({
      scripts: [],
      scriptSections: [],
      productsCount: 0,
      selectedScriptIdsCount: 0,
      totalEstSeconds: 0,
      expandedSections: new Set(),
    })

    expect(screen.getByTestId('live-script-panel-root')).toHaveAttribute('data-script-count', '0')
    expect(screen.getByTestId('live-script-panel-product-generate-open-button')).toBeDisabled()
    expect(screen.getByTestId('live-script-panel-product-generate-open-button')).toHaveAttribute('data-disabled-reason', 'no-products')
    expect(screen.getByTestId('live-script-panel-generate-full-button')).toHaveAttribute('data-disabled-reason', 'no-products')
    expect(screen.getByTestId('live-script-panel-empty')).toHaveAttribute('data-no-local-script-fallback', 'true')
    expect(screen.queryByTestId('script-section-root')).not.toBeInTheDocument()
  })

  it('marks progress and chain prompt as local callback surfaces', () => {
    const props = renderPanel({
      fullGenProgress: { current: 2, total: 5, slotType: 'product' },
      chainPrompt: { scriptId: 2, label: '开场已修改，是否调整产品话术？' },
    })

    expect(screen.getByTestId('live-script-panel-progress')).toHaveAttribute('data-contract-source', '/live/ai/generate-full-pipelined-sse')
    expect(screen.getByTestId('live-script-panel-progress')).toHaveAttribute('data-current', '2')
    expect(screen.getByTestId('live-script-panel-chain-prompt')).toHaveAttribute('data-contract-source', 'local-chain-adjustment-callback')

    fireEvent.click(screen.getByTestId('live-script-panel-chain-adjust-button'))
    fireEvent.click(screen.getByTestId('live-script-panel-chain-dismiss-button'))

    expect(props.onChainPromptAdjust).toHaveBeenCalledWith(2)
    expect(props.onChainPromptDismiss).toHaveBeenCalled()
  })

  it('marks section row actions as props-owned endpoint bridges', () => {
    renderPanel()

    expect(screen.getAllByTestId('script-section-root')[0]).toHaveAttribute('data-contract-scope', 'live-script-section-props-editor')
    expect(screen.getAllByTestId('script-section-root')[0]).toHaveAttribute('data-no-local-script-fallback', 'true')

    fireEvent.click(screen.getAllByTestId('script-section-select-checkbox')[0])
    fireEvent.click(screen.getAllByTestId('script-section-edit-button')[0])
    fireEvent.click(screen.getAllByTestId('script-section-mark-executed-button')[0])
    fireEvent.click(screen.getAllByTestId('script-section-analyst-button')[0])
    fireEvent.click(screen.getAllByTestId('script-section-refine-button')[0])
    fireEvent.click(screen.getAllByTestId('script-section-check-violation-button')[0])
    fireEvent.click(screen.getAllByTestId('script-section-save-library-button')[0])
    fireEvent.click(screen.getAllByTestId('script-section-delete-button')[0])

    expect(sectionProps.onToggleSelect).toHaveBeenCalledWith(1)
    expect(sectionProps.onEdit).toHaveBeenCalledWith(1, '欢迎来到直播间', scripts[0])
    expect(sectionProps.onMarkExecuted).toHaveBeenCalledWith(1, 0)
    expect(sectionProps.onOpenAnalyst).toHaveBeenCalledWith(scripts[0])
    expect(sectionProps.onRefineOpen).toHaveBeenCalledWith({ scriptId: 1 })
    expect(sectionProps.onCheckViolation).toHaveBeenCalledWith(1)
    expect(sectionProps.onSaveToLibrary).toHaveBeenCalledWith(1)
    expect(sectionProps.onDelete).toHaveBeenCalledWith(scripts[0])
  })

  it('marks edit save/cancel controls as script save props bridge', () => {
    renderPanel({
      sectionProps: {
        ...sectionProps,
        editingId: 1,
        editContent: '编辑后的开场',
        editDurationLimit: 60,
        editRequirement: '促单',
      },
    })

    expect(screen.getByTestId('script-editor')).toBeInTheDocument()
    expect(screen.getByTestId('script-section-save-edit-button')).toHaveAttribute('data-contract-source', '/live/script/save')
    fireEvent.click(screen.getByTestId('script-section-save-edit-button'))
    fireEvent.click(screen.getByTestId('script-section-cancel-edit-button'))

    expect(sectionProps.onSaveEdit).toHaveBeenCalled()
    expect(sectionProps.onCancelEdit).toHaveBeenCalled()
  })
})
