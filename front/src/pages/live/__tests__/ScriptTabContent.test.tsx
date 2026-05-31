import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { ScriptTabContent } from '../components/ScriptTabContent'
import { CoreDataContext } from '../contexts/CoreDataContext'
import { EditorContext } from '../contexts/EditorContext'
import { GenerationContext } from '../contexts/GenerationContext'
import { liveApi, type LiveScript } from '@/api/live'

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    scriptCommentUnresolvedByScript: vi.fn(),
    aiChatForScript: vi.fn(),
    aiRefineSegment: vi.fn(),
    aiRefineScript: vi.fn(),
    aiSuggestImprovement: vi.fn(),
    aiCheckViolation: vi.fn(),
    aiSaveToCopyIfPassed: vi.fn(),
    scriptSave: vi.fn(),
    scriptDelete: vi.fn(),
    versionSave: vi.fn(),
    versionList: vi.fn(),
    versionActivate: vi.fn(),
    versionDelete: vi.fn(),
    scriptCommentList: vi.fn(),
    scriptCommentSave: vi.fn(),
    scriptCommentResolve: vi.fn(),
    scriptCommentDelete: vi.fn(),
    scriptCommentByScript: vi.fn(),
  },
}))

const scripts: LiveScript[] = [
  {
    id: 21501,
    sessionId: 18,
    scriptType: 'product',
    scriptContent: '第一段话术，先讲用户痛点。\n\n第二段话术，承接福利和互动。',
    status: 0,
    durationLimitSec: 60,
    createTime: '2026-05-22T10:00:00',
    updateTime: '2026-05-22T10:00:00',
  },
]

let editorValue: {
  editingId: number | null
  editContent: string
  setEditContent: ReturnType<typeof vi.fn>
  handleStartEdit: ReturnType<typeof vi.fn>
  handleCancelEdit: ReturnType<typeof vi.fn>
  handleSaveEdit: ReturnType<typeof vi.fn>
  handleScriptSave: ReturnType<typeof vi.fn>
  handleScriptDelete: ReturnType<typeof vi.fn>
  isSaving: boolean
}

function renderScriptTab(scriptRows: LiveScript[] = scripts) {
  editorValue = {
    editingId: null,
    editContent: '',
    setEditContent: vi.fn(),
    handleStartEdit: vi.fn(),
    handleCancelEdit: vi.fn(),
    handleSaveEdit: vi.fn().mockResolvedValue(undefined),
    handleScriptSave: vi.fn().mockResolvedValue(undefined),
    handleScriptDelete: vi.fn().mockResolvedValue(undefined),
    isSaving: false,
  }

  return renderWithProviders(
    <AppThemeProvider>
      <CoreDataContext.Provider value={{
        session: {
          id: 18,
          userId: 1,
          accountId: 1,
          personaId: 1,
          liveTitle: '第215批直播话术审计场次',
          sessionCover: '',
          scriptStyle: 'natural',
          liveDescription: '',
          scheduledTime: '',
          scheduledEndTime: '',
          startTime: '',
          endTime: '',
          liveUrl: '',
          viewers: 0,
          likes: 0,
          status: 0,
          sessionType: 'standard',
          liveFormat: 'live',
          createTime: '',
          updateTime: '',
        },
        sessionLoading: false,
        dependencyIssues: [],
        products: [],
        scripts: scriptRows,
        readiness: null,
        refetchSession: vi.fn(),
        refetchProducts: vi.fn(),
        refetchScripts: vi.fn(),
      }}>
        <GenerationContext.Provider value={{
          isGenerating: false,
          generationProgress: 0,
          generationMessage: '',
          genJustCompleted: false,
          slotTimeline: [],
          startGeneration: vi.fn().mockResolvedValue(undefined),
          cancelGeneration: vi.fn(),
        }}>
          <EditorContext.Provider value={editorValue}>
            <ScriptTabContent />
          </EditorContext.Provider>
        </GenerationContext.Provider>
      </CoreDataContext.Provider>
    </AppThemeProvider>,
  )
}

describe('ScriptTabContent', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(liveApi.scriptCommentUnresolvedByScript).mockResolvedValue({ 21501: 1 })
    vi.mocked(liveApi.aiChatForScript).mockResolvedValue('AI 已将福利承接改得更清晰。')
    vi.mocked(liveApi.aiRefineSegment).mockResolvedValue('第二段话术，升级成更自然的福利承接。')
    vi.mocked(liveApi.aiRefineScript).mockResolvedValue('60秒内的精修后话术')
    vi.mocked(liveApi.aiSuggestImprovement).mockResolvedValue({ skipped: true, reason: '暂无改写稿' })
    vi.mocked(liveApi.versionList).mockResolvedValue([] as never)
    vi.mocked(liveApi.scriptCommentByScript).mockResolvedValue([] as never)
  })

  it('marks script edit workbench contract boundaries and local filtering fallback', async () => {
    renderScriptTab()

    const root = screen.getByTestId('live-script-tab-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'live-script-edit-review')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script/save'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/refine-script'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script-comment/by-script'))
    expect(root).toHaveAttribute('data-context-endpoints', '/live/session/get|/live/script/by-session')
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shortvideo-export'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('full-script-generation'))
    expect(root).toHaveAttribute('data-script-count', '1')
    expect(root).toHaveAttribute('data-filtered-count', '1')
    expect(root).toHaveAttribute('data-no-local-script-list-fallback', 'true')
    expect(screen.getByTestId('live-script-list-surface')).toHaveAttribute('data-contract-source', '/live/script/by-session')
    expect(await screen.findByTestId('live-script-card')).toHaveAttribute('data-script-id', '21501')
    expect(screen.getByTestId('live-script-quick-actions')).toHaveAttribute('data-contract-source', '/live/ai/refine-script|/live/ai/refine-segment|/live/ai/suggest-improvement|/live/ai/check-violation|/live/ai/save-to-copy-if-passed')
  })

  it('batches large script lists and requests unresolved comment counts once per session', async () => {
    const manyScripts = Array.from({ length: 45 }, (_, index) => ({
      ...scripts[0],
      id: 30000 + index,
      scriptContent: `第 ${index + 1} 条话术内容`,
    }))

    renderScriptTab(manyScripts)

    const root = screen.getByTestId('live-script-tab-workbench')
    expect(root).toHaveAttribute('data-script-count', '45')
    expect(root).toHaveAttribute('data-filtered-count', '45')
    expect(root).toHaveAttribute('data-rendered-count', '40')
    expect(root).toHaveAttribute('data-batched-rendering', 'true')
    expect(root).toHaveAttribute('data-default-expanded', 'false')
    expect(root).toHaveAttribute('data-default-first-expanded', 'true')
    const firstBatchCards = await screen.findAllByTestId('live-script-card')
    expect(firstBatchCards).toHaveLength(40)
    expect(firstBatchCards[0]).toHaveAttribute('data-script-expanded', 'true')
    expect(firstBatchCards[1]).toHaveAttribute('data-script-expanded', 'false')
    expect(screen.getAllByTestId('live-script-quick-actions')).toHaveLength(1)
    await waitFor(() => expect(liveApi.scriptCommentUnresolvedByScript).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByTestId('live-script-load-more-button'))

    await waitFor(() => expect(screen.getByTestId('live-script-tab-workbench')).toHaveAttribute('data-rendered-count', '45'))
    expect(await screen.findAllByTestId('live-script-card')).toHaveLength(45)
    expect(screen.getAllByTestId('live-script-quick-actions')).toHaveLength(1)
    expect(liveApi.scriptCommentUnresolvedByScript).toHaveBeenCalledTimes(1)
  }, 20000)

  it('mounts heavy script actions only after a collapsed row is expanded', async () => {
    const manyScripts = Array.from({ length: 30 }, (_, index) => ({
      ...scripts[0],
      id: 31000 + index,
      scriptContent: `第 ${index + 1} 条话术内容`,
    }))

    renderScriptTab(manyScripts)

    const cards = await screen.findAllByTestId('live-script-card')
    expect(cards).toHaveLength(30)
    expect(screen.getAllByTestId('live-script-quick-actions')).toHaveLength(1)

    fireEvent.click(screen.getAllByTestId('live-script-expand-button')[1])

    await waitFor(() => expect(cards[1]).toHaveAttribute('data-script-expanded', 'true'))
    expect(screen.getAllByTestId('live-script-quick-actions')).toHaveLength(2)
  })

  it('shows filtered empty state without local script fallback', async () => {
    renderScriptTab()

    fireEvent.change(screen.getByTestId('live-script-search-input'), { target: { value: '没有这个词' } })

    const root = screen.getByTestId('live-script-tab-workbench')
    expect(root).toHaveAttribute('data-query', '没有这个词')
    expect(root).toHaveAttribute('data-filtered-count', '0')
    expect(screen.getByTestId('live-script-empty-state')).toHaveAttribute('data-no-local-script-list-fallback', 'true')
    expect(screen.getByText(/未找到包含「没有这个词」的话术/)).toBeInTheDocument()
  })

  it('exposes version and comment dialog sources with empty fallback markers', async () => {
    renderScriptTab()

    fireEvent.click(screen.getByTestId('live-script-version-button'))
    const versionDialog = await screen.findByTestId('live-script-version-dialog')
    expect(versionDialog).toHaveAttribute('data-contract-source', '/live/script/version/getByScriptId|/live/script/version/updateStatus|/live/script/version/delete')
    expect(versionDialog).toHaveAttribute('data-no-local-version-fallback', 'true')
    expect(await screen.findByTestId('live-script-version-empty')).toHaveAttribute('data-contract-source', '/live/script/version/getByScriptId')
    fireEvent.click(screen.getByRole('button', { name: '关闭' }))

    fireEvent.click(screen.getByTestId('live-script-comment-button'))
    const commentDialog = await screen.findByTestId('live-script-comment-dialog')
    expect(commentDialog).toHaveAttribute('data-contract-source', '/live/script-comment/by-script|/live/script-comment/save|/live/script-comment/resolve|/live/script-comment/delete')
    expect(commentDialog).toHaveAttribute('data-no-local-comment-fallback', 'true')
    expect(await screen.findByTestId('live-script-comment-empty')).toHaveAttribute('data-contract-source', '/live/script-comment/by-script')
  })

  it('uses theme-aware card and AI chat surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderScriptTab()

    expect(screen.getByTestId('live-script-card-header-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })

    fireEvent.click(screen.getByTestId('live-script-ai-chat-button'))
    expect(await screen.findByText('AI 话术对话优化')).toBeInTheDocument()
    expect(screen.getByTestId('live-script-ai-chat-dialog')).toHaveAttribute('data-contract-source', '/live/ai/chat-for-script|/live/script/save')
    const initialAiBubble = screen.getByTestId('live-script-chat-bubble-ai')
    expect(initialAiBubble).toHaveStyle({ backgroundColor: 'rgb(18, 18, 18)' })

    fireEvent.change(screen.getByPlaceholderText('描述你的修改需求…'), {
      target: { value: '把福利承接讲得更自然' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送 AI 对话' }))

    await waitFor(() => expect(liveApi.aiChatForScript).toHaveBeenCalledWith({
      scriptId: 21501,
      message: '把福利承接讲得更自然',
    }))
    expect(screen.getByTestId('live-script-chat-bubble-user')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })

  it('uses theme-aware segment refine selection surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderScriptTab()

    fireEvent.click(screen.getByRole('button', { name: '段内微调' }))

    expect(await screen.findByTestId('live-script-segment-refine-list-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    fireEvent.click(screen.getByText('第二段话术，承接福利和互动。'))
    expect(screen.getByTestId('live-script-selected-segment-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })

  it('uses theme-aware style refine group titles in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderScriptTab()

    fireEvent.click(screen.getByRole('button', { name: '换风格精修' }))

    const titles = await screen.findAllByTestId('live-script-style-group-title-surface')
    expect(titles).toHaveLength(5)
    expect(Object.fromEntries(titles.map(title => [
      title.getAttribute('data-style-group'),
      title.getAttribute('data-style-tone'),
    ]))).toEqual({
      '基础': 'primary',
      '激情': 'error',
      '专业': 'success',
      '情感': 'secondary',
      '创意': 'warning',
    })

    expect(document.body.textContent).not.toMatch(/#42a5f5|#ef5350|#66bb6a|#ab47bc|#ff7043/i)
  })

  it('does not apply suggestion-only AI results as script content', async () => {
    vi.mocked(liveApi.aiSuggestImprovement).mockResolvedValue({
      suggestion: '建议缩短开头并增加互动问题',
      skipped: false,
    })
    renderScriptTab()

    fireEvent.click(screen.getByTestId('live-script-more-button'))
    fireEvent.click(await screen.findByTestId('live-script-menu-suggest'))

    const alert = await screen.findByTestId('live-script-suggestion-alert')
    expect(alert).toHaveAttribute('data-has-improved', 'false')
    expect(alert).toHaveAttribute('data-no-suggestion-as-script', 'true')
    expect(screen.queryByTestId('live-script-apply-improved-button')).not.toBeInTheDocument()
    expect(screen.getByText('建议缩短开头并增加互动问题')).toBeInTheDocument()
    expect(editorValue.setEditContent).not.toHaveBeenCalled()
  })

  it('applies only improved AI text when the suggestion result includes a rewrite', async () => {
    vi.mocked(liveApi.aiSuggestImprovement).mockResolvedValue({
      improved: '改写后的60秒口播正文',
      suggestion: '说明：已压缩节奏并补充互动',
      skipped: false,
    })
    renderScriptTab()

    fireEvent.click(screen.getByTestId('live-script-more-button'))
    fireEvent.click(await screen.findByTestId('live-script-menu-suggest'))

    const alert = await screen.findByTestId('live-script-suggestion-alert')
    expect(alert).toHaveAttribute('data-has-improved', 'true')
    expect(screen.getByTestId('live-script-improved-content')).toHaveTextContent('改写后的60秒口播正文')

    fireEvent.click(screen.getByTestId('live-script-apply-improved-button'))

    expect(editorValue.handleStartEdit).toHaveBeenCalledWith(expect.objectContaining({ id: 21501 }))
    expect(editorValue.setEditContent).toHaveBeenCalledWith('改写后的60秒口播正文')
    expect(editorValue.setEditContent).not.toHaveBeenCalledWith(expect.stringContaining('说明：'))
  })

  it('passes the script duration limit to one-click AI refine', async () => {
    renderScriptTab()

    fireEvent.click(screen.getByTestId('live-script-more-button'))
    fireEvent.click(await screen.findByTestId('live-script-menu-refine'))

    await waitFor(() => expect(liveApi.aiRefineScript).toHaveBeenCalledWith({
      scriptId: 21501,
      question: expect.stringContaining('必须控制在 60 秒以内'),
    }))
    expect(vi.mocked(liveApi.aiRefineScript).mock.calls[0][0].question).toEqual(expect.stringContaining('180-240'))
  })
})
