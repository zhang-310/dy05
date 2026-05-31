import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AiChatPanel, type AiChatPanelProps } from './AiChatPanel'
import { AiAnalystPanel, type AiAnalystPanelProps } from './AiAnalystPanel'

function makeChatProps(overrides?: Partial<AiChatPanelProps>): AiChatPanelProps {
  return {
    editingScript: { id: 1, scriptType: 'product', sequenceNo: 2 },
    chatPersona: '护肤主播',
    chatScene: '屏障修护专场',
    chatDurationSec: 90,
    chatDimensions: ['拉停留'],
    chatMessage: '强化促单',
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

function makeAnalystProps(overrides?: Partial<AiAnalystPanelProps>): AiAnalystPanelProps {
  return {
    analystScript: { id: 2, sequenceNo: 3, scriptType: 'product' },
    analystContent: '这是一段已生成的分析话术',
    analystDuration: 60,
    analystRequirement: 'highlight_benefit',
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

describe('AI chat and analyst panel contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks chat panel as a props-owned bridge for AI chat, save and copy actions', () => {
    const props = makeChatProps({
      chatResponse: '直播间停留话术结果',
      chatHistoryLength: 2,
    })

    renderWithProviders(<AiChatPanel {...props} />)

    const root = screen.getByTestId('ai-chat-panel-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-ai-chat-props-bridge')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/chat-for-script'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script/save'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/save-to-copy-if-passed'))
    expect(root).toHaveAttribute('data-context-sources', 'editing-script-prop|chat-state-props|selected-model-owned-by-parent')
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(root).toHaveAttribute('data-no-local-chat-fallback', 'true')
    expect(root).toHaveAttribute('data-response-state', 'ready')
    expect(root).toHaveAttribute('data-script-id', '1')
    expect(screen.getByTestId('ai-chat-send-button')).toHaveAttribute('data-endpoint-owner', 'useAiChat.handleChatSend')
    expect(screen.getByTestId('ai-chat-apply-button')).toHaveAttribute('data-contract-source', '/live/script/save|local-edit-content')
    expect(screen.getByTestId('ai-chat-save-copy-button')).toHaveAttribute('data-contract-source', '/live/ai/save-to-copy-if-passed')
    expect(screen.getByTestId('ai-chat-clear-history-button')).toHaveAttribute('data-contract-source', 'onHistoryClear-prop')

    fireEvent.click(screen.getByTestId('ai-chat-send-button'))
    fireEvent.click(screen.getByTestId('ai-chat-apply-button'))
    fireEvent.click(screen.getByTestId('ai-chat-save-copy-button'))
    fireEvent.click(screen.getByTestId('ai-chat-clear-history-button'))

    expect(props.onSend).toHaveBeenCalled()
    expect(props.onApply).toHaveBeenCalled()
    expect(props.onSaveToCopy).toHaveBeenCalled()
    expect(props.onHistoryClear).toHaveBeenCalled()
  })

  it('keeps chat empty/loading states visible without local response fallback', () => {
    const props = makeChatProps({
      chatPersona: '',
      chatScene: '',
      chatDurationSec: '',
      chatDimensions: [],
      chatMessage: '',
      chatResponse: '',
    })

    renderWithProviders(<AiChatPanel {...props} />)

    expect(screen.getByTestId('ai-chat-panel-root')).toHaveAttribute('data-response-state', 'empty')
    expect(screen.getByTestId('ai-chat-panel-root')).toHaveAttribute('data-can-send', 'false')
    expect(screen.getByTestId('ai-chat-send-button')).toBeDisabled()
    expect(screen.getByTestId('ai-chat-send-button')).toHaveAttribute('data-disabled-reason', 'no-context-or-message')
    expect(screen.getByTestId('ai-chat-response-empty')).toHaveAttribute('data-no-local-chat-fallback', 'true')
    expect(screen.queryByTestId('ai-chat-response-surface')).not.toBeInTheDocument()
  })

  it('routes chat field edits and refine suggestions through props', () => {
    const props = makeChatProps({ chatMessage: '原始需求', chatDimensions: [] })

    renderWithProviders(<AiChatPanel {...props} />)

    fireEvent.change(screen.getByTestId('ai-chat-persona-input'), { target: { value: '新主播' } })
    fireEvent.change(screen.getByTestId('ai-chat-scene-input'), { target: { value: '新品专场' } })
    fireEvent.change(screen.getByTestId('ai-chat-duration-input'), { target: { value: '120' } })
    fireEvent.click(screen.getAllByTestId('ai-chat-dimension-chip')[0])
    fireEvent.click(screen.getAllByTestId('ai-chat-refine-suggestion-chip')[0])

    expect(props.onPersonaChange).toHaveBeenCalledWith('新主播')
    expect(props.onSceneChange).toHaveBeenCalledWith('新品专场')
    expect(props.onDurationChange).toHaveBeenCalledWith(120)
    expect(props.onDimensionsChange).toHaveBeenCalledWith(['拉停留'])
    expect(props.onMessageChange).toHaveBeenCalledWith(expect.stringContaining('改成30秒以内'))
  })

  it('marks analyst panel as a props-owned bridge for slot generation and script save', () => {
    const props = makeAnalystProps()

    renderWithProviders(<AiAnalystPanel {...props} />)

    const root = screen.getByTestId('ai-analyst-panel-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-ai-analyst-props-bridge')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/generate-slot'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script/save'))
    expect(root).toHaveAttribute('data-context-sources', 'analyst-script-prop|analyst-state-props|selected-model-owned-by-parent')
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(root).toHaveAttribute('data-no-local-analyst-fallback', 'true')
    expect(root).toHaveAttribute('data-content-state', 'ready')
    expect(root).toHaveAttribute('data-script-id', '2')
    expect(screen.getByTestId('ai-analyst-autofill-button')).toHaveAttribute('data-endpoint-owner', 'useAiChat.handleAnalystAutoFill')
    expect(screen.getByTestId('ai-analyst-apply-button')).toHaveAttribute('data-contract-source', '/live/script/save')
    expect(screen.getByTestId('ai-analyst-close-button')).toHaveAttribute('data-contract-source', 'setAnalystScript-prop')

    fireEvent.click(screen.getByTestId('ai-analyst-autofill-button'))
    fireEvent.click(screen.getByTestId('ai-analyst-apply-button'))
    fireEvent.click(screen.getByTestId('ai-analyst-close-button'))

    expect(props.onAutoFill).toHaveBeenCalled()
    expect(props.onApply).toHaveBeenCalled()
    expect(props.onClose).toHaveBeenCalled()
  })

  it('keeps analyst empty/loading states visible without local content fallback', () => {
    const props = makeAnalystProps({ analystContent: '', analystLoading: true })

    renderWithProviders(<AiAnalystPanel {...props} />)

    expect(screen.getByTestId('ai-analyst-panel-root')).toHaveAttribute('data-content-state', 'loading')
    expect(screen.getByTestId('ai-analyst-autofill-button')).toBeDisabled()
    expect(screen.getByTestId('ai-analyst-autofill-button')).toHaveAttribute('data-disabled-reason', 'analyst-loading')
    expect(screen.getByTestId('ai-analyst-content-empty')).toHaveAttribute('data-no-local-analyst-fallback', 'true')
  })
})
