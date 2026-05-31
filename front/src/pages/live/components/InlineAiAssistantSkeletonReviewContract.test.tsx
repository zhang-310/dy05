import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { InlineAiAssistant, type InlineAiAssistantProps } from './InlineAiAssistant'
import { SkeletonReviewDialog, type SkeletonItem } from './SkeletonReviewDialog'
import type { AiChatPanelProps } from './AiChatPanel'
import type { AiAnalystPanelProps } from './AiAnalystPanel'

function chatProps(overrides?: Partial<AiChatPanelProps>): AiChatPanelProps {
  return {
    editingScript: { id: 1, scriptType: 'product' },
    chatPersona: '主播',
    chatScene: '专场',
    chatDurationSec: 60,
    chatDimensions: ['促单'],
    chatMessage: '优化话术',
    chatResponse: '',
    chatLoading: false,
    chatHistoryLength: 0,
    copySaveLoading: false,
    onPersonaChange: vi.fn(),
    onSceneChange: vi.fn(),
    onDurationChange: vi.fn(),
    onDimensionsChange: vi.fn(),
    onMessageChange: vi.fn(),
    onHistoryClear: vi.fn(),
    onSend: vi.fn(),
    onApply: vi.fn(),
    onSaveToCopy: vi.fn(),
    ...overrides,
  }
}

function analystProps(overrides?: Partial<AiAnalystPanelProps>): AiAnalystPanelProps {
  return {
    analystScript: { id: 2, scriptType: 'product', sequenceNo: 2 },
    analystContent: '分析结果',
    analystDuration: 80,
    analystRequirement: '',
    analystLoading: false,
    onContentChange: vi.fn(),
    onDurationChange: vi.fn(),
    onRequirementChange: vi.fn(),
    onAutoFill: vi.fn(),
    onApply: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  }
}

function inlineProps(overrides?: Partial<InlineAiAssistantProps>): InlineAiAssistantProps {
  return {
    mode: 'chat',
    onModeChange: vi.fn(),
    chatAvailable: true,
    analystAvailable: true,
    chatProps: chatProps(),
    analystProps: analystProps(),
    ...overrides,
  }
}

const skeletonItems: SkeletonItem[] = [
  { scriptId: 101, scriptType: 'opening', summary: '痛点开场', suggestedDurationSec: 30 },
  { scriptId: 102, scriptType: 'product', summary: '产品卖点', suggestedDurationSec: 90 },
]

describe('InlineAiAssistant and SkeletonReviewDialog contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks inline AI assistant as a props-only mode router', () => {
    const props = inlineProps()
    renderWithProviders(<InlineAiAssistant {...props} />)

    const root = screen.getByTestId('inline-ai-assistant-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-inline-ai-assistant-props-router')
    expect(root).toHaveAttribute('data-ready-sources', 'chat-props|analyst-props|mode-prop|availability-props')
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(root).toHaveAttribute('data-no-local-ai-fallback', 'true')
    expect(screen.getByTestId('inline-ai-assistant-chat-toggle')).toHaveAttribute('data-disabled-reason', 'ready')
    expect(screen.getByTestId('ai-chat-panel-root')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('inline-ai-assistant-analyst-toggle'))
    expect(props.onModeChange).toHaveBeenCalledWith('analyst')
  })

  it('shows unavailable inline states without local AI fallback', () => {
    renderWithProviders(<InlineAiAssistant {...inlineProps({ chatAvailable: false, analystAvailable: false })} />)

    expect(screen.getByTestId('inline-ai-assistant-chat-toggle')).toBeDisabled()
    expect(screen.getByTestId('inline-ai-assistant-chat-toggle')).toHaveAttribute('data-disabled-reason', 'chat-unavailable')
    expect(screen.getByTestId('inline-ai-chat-unavailable')).toHaveAttribute('data-no-local-chat-fallback', 'true')
  })

  it('marks analyst unavailable state when analyst mode lacks context', () => {
    renderWithProviders(<InlineAiAssistant {...inlineProps({ mode: 'analyst', analystAvailable: false })} />)

    expect(screen.getByTestId('inline-ai-assistant-root')).toHaveAttribute('data-mode', 'analyst')
    expect(screen.getByTestId('inline-ai-assistant-analyst-toggle')).toHaveAttribute('data-disabled-reason', 'analyst-unavailable')
    expect(screen.getByTestId('inline-ai-analyst-unavailable')).toHaveAttribute('data-no-local-analyst-fallback', 'true')
  })

  it('marks skeleton review dialog and confirms edited skeleton through props', () => {
    const onConfirm = vi.fn()
    const onClose = vi.fn()

    renderWithProviders(
      <SkeletonReviewDialog open items={skeletonItems} onClose={onClose} onConfirm={onConfirm} />,
    )

    const dialog = screen.getByTestId('skeleton-review-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-skeleton-review-props-editor')
    expect(dialog).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/generate-skeleton-sse'))
    expect(dialog).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script/save'))
    expect(dialog).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(dialog).toHaveAttribute('data-no-local-skeleton-fallback', 'true')
    expect(dialog).toHaveAttribute('data-item-count', '2')
    expect(screen.getAllByTestId('skeleton-review-row')).toHaveLength(2)

    fireEvent.change(screen.getAllByTestId('skeleton-review-summary-input')[0], { target: { value: '新版开场' } })
    fireEvent.change(screen.getAllByTestId('skeleton-review-duration-input')[0], { target: { value: '45' } })
    fireEvent.click(screen.getByTestId('skeleton-review-confirm-button'))

    expect(onConfirm).toHaveBeenCalledWith([
      expect.objectContaining({ scriptId: 101, summary: '新版开场', suggestedDurationSec: 45 }),
      expect.objectContaining({ scriptId: 102 }),
    ])
  })

  it('keeps empty skeleton review visible and disables confirm without fallback', () => {
    const onClose = vi.fn()
    renderWithProviders(<SkeletonReviewDialog open items={[]} onClose={onClose} onConfirm={vi.fn()} />)

    expect(screen.getByTestId('skeleton-review-dialog')).toHaveAttribute('data-item-count', '0')
    expect(screen.getByTestId('skeleton-review-empty-state')).toHaveAttribute('data-no-local-skeleton-fallback', 'true')
    expect(screen.getByTestId('skeleton-review-confirm-button')).toBeDisabled()
    expect(screen.getByTestId('skeleton-review-confirm-button')).toHaveAttribute('data-disabled-reason', 'no-skeleton-items')

    fireEvent.click(screen.getByTestId('skeleton-review-cancel-button'))
    expect(onClose).toHaveBeenCalled()
  })
})
