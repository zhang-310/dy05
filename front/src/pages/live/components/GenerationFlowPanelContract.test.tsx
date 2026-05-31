import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { GenerationFlowPanel, type FlowStep } from './GenerationFlowPanel'

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

function renderFlow(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

const steps: FlowStep[] = [
  { label: '生成开场中', status: 'done', content: '欢迎来到直播间', stepKey: 'opening', scriptId: 101, confidence: 92, qualityScore: 'pass' },
  { label: '生成产品1中', status: 'failed', errorMsg: '模型超时', stepKey: 'product-1', scriptId: 102, qualityScore: 'fail', qualityIssues: ['内容为空'] },
  { label: '生成转场1中', status: 'loading', streamingContent: '正在生成转场...', stepKey: 'transition-1', scriptId: 103 },
  { label: '生成收尾中', status: 'pending', stepKey: 'closing', scriptId: 104 },
]

describe('GenerationFlowPanel contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks inline empty state without local step fallback', () => {
    renderFlow(<GenerationFlowPanel variant="inline" open steps={[]} />)

    const empty = screen.getByTestId('generation-flow-empty-state')
    expect(empty).toHaveAttribute('data-contract-scope', 'live-generation-flow-props-orchestrator')
    expect(empty).toHaveAttribute('data-generation-state', 'empty')
    expect(empty).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(empty).toHaveAttribute('data-no-local-step-fallback', 'true')
    expect(screen.queryByTestId('generation-flow-panel-root')).not.toBeInTheDocument()
  })

  it('marks running state and routes cancel/collapse through props', () => {
    const onCancel = vi.fn()
    const onClose = vi.fn()

    renderFlow(
      <GenerationFlowPanel
        variant="inline"
        open
        steps={steps}
        total={4}
        currentLabel="生成产品1中"
        isGenerating
        sseReconnecting
        onCancel={onCancel}
        onClose={onClose}
        modelName="DeepSeek"
      />,
    )

    const root = screen.getByTestId('generation-flow-panel-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-generation-flow-props-orchestrator')
    expect(root).toHaveAttribute('data-contract-source', expect.stringContaining('/live/ai/generate-full-pipelined-sse'))
    expect(root).toHaveAttribute('data-generation-state', 'running')
    expect(root).toHaveAttribute('data-step-count', '4')
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(screen.getByTestId('flow-progress-header')).toHaveAttribute('data-sse-reconnecting', 'true')
    expect(screen.getByTestId('flow-progress-reconnecting-banner')).toHaveAttribute('data-no-local-step-fallback', 'true')
    expect(screen.getByTestId('flow-action-running-bar')).toHaveAttribute('data-contract-source', 'onCancel-prop|onClose-prop')

    fireEvent.click(screen.getByTestId('flow-action-cancel-button'))
    fireEvent.click(screen.getByTestId('flow-action-collapse-button'))

    expect(onCancel).toHaveBeenCalled()
    expect(onClose).toHaveBeenCalled()
  })

  it('routes retry, expand, collapse and copy actions through props', () => {
    const onRetryStep = vi.fn()
    const onRetryAllFailed = vi.fn()

    renderFlow(
      <GenerationFlowPanel
        variant="inline"
        open
        steps={steps}
        total={4}
        onRetryStep={onRetryStep}
        onRetryAllFailed={onRetryAllFailed}
      />,
    )

    expect(screen.getByTestId('flow-progress-summary')).toHaveAttribute('data-percent', '25')
    expect(screen.getByTestId('flow-step-flat-failed-surface')).toHaveAttribute('data-step-status', 'failed')
    expect(screen.getByTestId('flow-step-flat-failed-surface')).toHaveAttribute('data-no-local-step-fallback', 'true')

    fireEvent.click(screen.getByTestId('flow-progress-retry-all-failed-button'))
    fireEvent.click(screen.getByTestId('flow-step-flat-retry-button'))
    fireEvent.click(screen.getAllByTestId('flow-step-flat-toggle-button')[0])
    fireEvent.click(screen.getAllByTestId('flow-step-flat-copy-button')[0])

    expect(onRetryAllFailed).toHaveBeenCalled()
    expect(onRetryStep).toHaveBeenCalledWith(expect.objectContaining({ stepKey: 'product-1' }))
  })

  it('marks complete save/export/dismiss actions as parent-owned endpoints', () => {
    const onSaveToLibrary = vi.fn()
    const onExport = vi.fn()
    const onDismiss = vi.fn()

    renderFlow(
      <GenerationFlowPanel
        variant="inline"
        open
        steps={steps.filter((step) => step.status !== 'loading')}
        total={4}
        onSaveToLibrary={onSaveToLibrary}
        onExport={onExport}
        onDismiss={onDismiss}
        justCompleted
      />,
    )

    expect(screen.getByTestId('generation-flow-panel-root')).toHaveAttribute('data-generation-state', 'completed')
    expect(screen.getByTestId('flow-action-complete-bar')).toHaveAttribute('data-contract-source', '/live/script/save-batch-to-library|/live/script/export|onDismiss-prop')
    expect(screen.getByTestId('flow-action-save-library-button')).toHaveAttribute('data-action-owner', 'onSaveToLibrary-prop')
    expect(screen.getByTestId('flow-action-export-button')).toHaveAttribute('data-action-owner', 'onExport-prop')

    fireEvent.click(screen.getByTestId('flow-action-save-library-button'))
    fireEvent.click(screen.getByTestId('flow-action-export-button'))
    fireEvent.click(screen.getByTestId('flow-action-dismiss-button'))

    expect(onSaveToLibrary).toHaveBeenCalled()
    expect(onExport).toHaveBeenCalled()
    expect(onDismiss).toHaveBeenCalled()
  })

  it('marks grouped steps as props data and retries through parent callback', () => {
    const onRetryStep = vi.fn()
    renderFlow(
      <GenerationFlowPanel
        variant="inline"
        open
        steps={[]}
        groupedSteps={[
          {
            groupLabel: '产品组',
            steps: [
              { label: '生成产品1中', status: 'done', content: '完成', stepKey: 'g1', scriptId: 201 },
              { label: '生成产品2中', status: 'failed', errorMsg: '供应商限流', stepKey: 'g2', scriptId: 202 },
            ],
          },
        ]}
        onRetryStep={onRetryStep}
      />,
    )

    expect(screen.getByTestId('generation-flow-panel-root')).toHaveAttribute('data-group-count', '1')
    expect(screen.getByTestId('flow-step-grouped-failed-surface')).toHaveAttribute('data-contract-source', 'groupedSteps-prop')
    expect(screen.getByTestId('flow-step-grouped-failed-surface')).toHaveAttribute('data-no-local-step-fallback', 'true')

    fireEvent.click(screen.getByTestId('flow-step-grouped-retry-button'))
    expect(onRetryStep).toHaveBeenCalledWith(expect.objectContaining({ stepKey: 'g2' }))
  })
})
