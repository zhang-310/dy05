import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { AiRightPanel } from './AiRightPanel'
import { AiAssistTab } from './AiAssistTab'
import { generateAnalysis, getAnalysis, getScriptEffectiveness, saveBatchToLibrary } from '@/api/live'

const toast = vi.hoisted(() => vi.fn())

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/api/live', () => ({
  generateAnalysis: vi.fn(),
  getAnalysis: vi.fn(),
  getScriptEffectiveness: vi.fn(),
  saveBatchToLibrary: vi.fn(),
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('AI right panel and assist tab surfaces', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(generateAnalysis).mockResolvedValue({ summary: '生成后的复盘' } as never)
    vi.mocked(saveBatchToLibrary).mockResolvedValue(1 as never)
  })

  it('marks right panel as props-only and avoids local quality fallback', () => {
    const onRunQualityCheck = vi.fn()
    renderDark(
      <AiRightPanel
        activeTab="quality"
        chatAvailable={false}
        chatProps={{
          editingScript: null,
          chatPersona: '',
          chatScene: '',
          chatDurationSec: '',
          chatDimensions: [],
          chatMessage: '',
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
        }}
        analystAvailable={false}
        analystProps={{
          analystScript: {},
          analystContent: '',
          analystDuration: '',
          analystRequirement: '',
          analystLoading: false,
          onContentChange: vi.fn(),
          onDurationChange: vi.fn(),
          onRequirementChange: vi.fn(),
          onAutoFill: vi.fn(),
          onApply: vi.fn(),
          onClose: vi.fn(),
        }}
        qualityResults={[]}
        onRunQualityCheck={onRunQualityCheck}
      />,
    )

    expect(screen.getByTestId('ai-right-panel-workbench')).toHaveAttribute('data-contract-scope', 'live-ai-side-panel')
    expect(screen.getByTestId('ai-right-panel-workbench')).toHaveAttribute('data-ready-sources', 'chat-props|analyst-props|quality-results-props')
    expect(screen.getByTestId('ai-right-panel-workbench')).toHaveAttribute('data-no-local-quality-fallback', 'true')
    expect(screen.getByTestId('ai-right-quality-empty-state')).toHaveAttribute('data-no-local-quality-fallback', 'true')
    fireEvent.click(screen.getByTestId('ai-right-quality-run-button'))
    expect(onRunQualityCheck).toHaveBeenCalled()
  })

  it('uses theme-aware quality result surfaces in dark mode', () => {
    renderDark(
      <AiRightPanel
        activeTab="quality"
        chatAvailable={false}
        chatProps={{
          editingScript: null,
          chatPersona: '',
          chatScene: '',
          chatDurationSec: '',
          chatDimensions: [],
          chatMessage: '',
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
        }}
        analystAvailable={false}
        analystProps={{
          analystScript: {},
          analystContent: '',
          analystDuration: '',
          analystRequirement: '',
          analystLoading: false,
          onContentChange: vi.fn(),
          onDurationChange: vi.fn(),
          onRequirementChange: vi.fn(),
          onAutoFill: vi.fn(),
          onApply: vi.fn(),
          onClose: vi.fn(),
        }}
        qualityResults={[
          { scriptId: 21701, passed: true, score: 92 },
          { scriptId: 21702, passed: false, score: 64, violations: ['禁用夸大词'] },
        ]}
      />,
    )

    expect(screen.getByTestId('ai-right-panel-workbench')).toHaveAttribute('data-quality-count', '2')
    expect(screen.getByTestId('ai-right-quality-passed-surface')).toHaveAttribute('data-contract-source', 'quality-results-props')
    expect(screen.getByTestId('ai-right-quality-passed-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
    expect(screen.getByTestId('ai-right-quality-failed-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })

  it('uses a theme-aware effectiveness table header in dark mode', async () => {
    vi.mocked(getAnalysis).mockResolvedValue({
      rating: 88,
      summary: '复盘总结',
      highlights: ['节奏稳定'],
    } as never)
    vi.mocked(getScriptEffectiveness).mockResolvedValue([
      {
        id: 1,
        scriptType: 'product',
        viewerDelta: 120,
        interactionDelta: 36,
        effectivenessScore: 91.2,
      },
    ] as never)

    renderDark(<AiAssistTab sessionId={18} />)

    expect(await screen.findByTestId('ai-assist-workbench')).toHaveAttribute('data-contract-scope', 'live-ai-review-assist')
    expect(screen.getByTestId('ai-assist-workbench')).toHaveAttribute('data-ready-endpoints', '/live/analysis/get|/live/script/effectiveness|/live/analysis/generate|/live/script/save-batch-to-library')
    expect(screen.getByTestId('ai-assist-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-next-session-suggestion'))
    expect(await screen.findByTestId('ai-assist-effectiveness-table-head-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    expect(screen.getByTestId('ai-assist-effectiveness-section')).toHaveAttribute('data-no-local-effectiveness-fallback', 'true')
  })

  it('shows analysis and effectiveness endpoint errors without local fallback', async () => {
    vi.mocked(getAnalysis).mockRejectedValue(new Error('analysis down'))
    vi.mocked(getScriptEffectiveness).mockRejectedValue(new Error('effectiveness down'))

    renderDark(<AiAssistTab sessionId={18} />)

    expect(await screen.findByTestId('ai-assist-analysis-error')).toHaveTextContent('/live/analysis/get AI 复盘报告加载失败：analysis down')
    expect(screen.getByTestId('ai-assist-analysis-error')).toHaveAttribute('data-no-local-analysis-fallback', 'true')
    expect(screen.getByTestId('ai-assist-effectiveness-error')).toHaveTextContent('/live/script/effectiveness 话术效果加载失败：effectiveness down')
    expect(screen.getByTestId('ai-assist-effectiveness-error')).toHaveAttribute('data-no-local-effectiveness-fallback', 'true')
    expect(screen.queryByTestId('ai-assist-effectiveness-row')).not.toBeInTheDocument()
  })

  it('uses endpoint toasts for generate and save failures and labels refresh as local callback', async () => {
    const onRefresh = vi.fn()
    vi.mocked(getAnalysis).mockResolvedValue({
      summary: '复盘总结',
    } as never)
    vi.mocked(getScriptEffectiveness).mockResolvedValue([
      { id: 1, scriptType: 'product', effectivenessScore: 91.2 },
    ] as never)
    vi.mocked(generateAnalysis).mockRejectedValue(new Error('generate down'))
    vi.mocked(saveBatchToLibrary).mockRejectedValue(new Error('save down'))

    renderDark(<AiAssistTab sessionId={18} onRefresh={onRefresh} />)

    expect(await screen.findByTestId('ai-assist-generate-analysis-button')).toHaveAttribute('data-contract-source', '/live/analysis/generate')
    fireEvent.click(screen.getByTestId('ai-assist-generate-analysis-button'))
    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith('/live/analysis/generate AI 复盘报告生成失败：generate down', 'error')
    })

    fireEvent.click(screen.getByTestId('ai-assist-save-batch-library-button'))
    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith('/live/script/save-batch-to-library 保存高效话术失败：save down', 'error')
    })

    expect(screen.getByTestId('ai-assist-refresh-button')).toHaveAttribute('data-contract-source', 'local-refresh-callback')
    expect(screen.getByTestId('ai-assist-refresh-button')).toHaveAttribute('data-no-local-next-session-suggestion', 'true')
    fireEvent.click(screen.getByTestId('ai-assist-refresh-button'))
    expect(onRefresh).toHaveBeenCalled()
  })
})
