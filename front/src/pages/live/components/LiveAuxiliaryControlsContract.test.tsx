import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { GenerationProgressFab } from './GenerationProgressFab'
import { RegenerationDiffDialog } from './RegenerationDiffDialog'
import { VersionHistoryPanel } from './VersionHistoryPanel'
import { ApprovalStatusBadge } from './ApprovalStatusBadge'
import { searchScriptVersions, getScriptVersion, rollbackScriptVersion } from '@/api/live.versions'

vi.mock('@/api/live.versions', () => ({
  searchScriptVersions: vi.fn(),
  getScriptVersion: vi.fn(),
  rollbackScriptVersion: vi.fn(),
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderVersionPanel(overrides?: Partial<React.ComponentProps<typeof VersionHistoryPanel>>) {
  const onScriptSelect = vi.fn()
  renderWithProviders(
    <VersionHistoryPanel
      scripts={[{ id: 18, scriptType: 'product', scriptContent: '当前话术内容' }]}
      sessionId={18}
      selectedScriptId={18}
      onScriptSelect={onScriptSelect}
      {...overrides}
    />,
  )
  return { onScriptSelect }
}

describe('live auxiliary controls contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(searchScriptVersions).mockResolvedValue({
      total: 2,
      list: [
        { id: 2, versionNumber: 2, scriptContent: '当前版本', isCurrent: 1, source: 'manual' },
        { id: 1, versionNumber: 1, scriptContent: '旧版本', isCurrent: 0, source: 'auto_improve' },
      ],
      pageNum: 0,
      pageSize: 50,
    } as never)
    vi.mocked(getScriptVersion).mockImplementation(async (_scriptId: number, versionNumber: number) => ({
      scriptContent: versionNumber === 1 ? '旧版本：先讲成分。' : '当前版本：先讲痛点，再讲成分。',
    }) as never)
    vi.mocked(rollbackScriptVersion).mockResolvedValue(undefined as never)
  })

  it('marks generation progress FAB as props-owned and exposes progress counts', () => {
    const onClick = vi.fn()
    renderWithProviders(<GenerationProgressFab visible genLoading doneCount={2} totalCount={5} onClick={onClick} />)

    const fab = screen.getByTestId('generation-progress-fab-surface')
    expect(fab).toHaveAttribute('data-contract-scope', 'live-generation-progress-fab')
    expect(fab).toHaveAttribute('data-ready-sources', 'generation-progress-props|onClick-prop')
    expect(fab).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(fab).toHaveAttribute('data-done-count', '2')
    expect(fab).toHaveAttribute('data-total-count', '5')
    fireEvent.click(fab)
    expect(onClick).toHaveBeenCalled()
  })

  it('marks regeneration diff dialog as props-only and delegates actions', () => {
    const callbacks = { onAccept: vi.fn(), onReject: vi.fn(), onCancel: vi.fn() }
    renderWithProviders(
      <RegenerationDiffDialog
        open
        oldContent="原版本话术：先讲成分，节奏略慢。"
        newContent="新版本话术：先讲痛点，再讲成分和限时福利。"
        slotLabel="产品讲解"
        {...callbacks}
      />,
    )

    const dialog = screen.getByTestId('regeneration-diff-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-regeneration-diff-props-review')
    expect(dialog).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(dialog).toHaveAttribute('data-length-delta', '5')
    expect(screen.getByTestId('regeneration-diff-old-surface')).toHaveAttribute('data-contract-source', 'oldContent-prop')
    expect(screen.getByTestId('regeneration-diff-new-surface')).toHaveAttribute('data-contract-source', 'newContent-prop')

    fireEvent.click(screen.getByTestId('regeneration-diff-reject-button'))
    fireEvent.click(screen.getByTestId('regeneration-diff-accept-button'))
    fireEvent.click(screen.getByTestId('regeneration-diff-cancel-button'))
    expect(callbacks.onReject).toHaveBeenCalled()
    expect(callbacks.onAccept).toHaveBeenCalled()
    expect(callbacks.onCancel).toHaveBeenCalled()
  })

  it('marks version history list, inline diff, rollback, empty, and error states', async () => {
    renderVersionPanel()

    const panel = await screen.findByTestId('version-history-panel')
    expect(panel).toHaveAttribute('data-contract-scope', 'live-version-history-panel')
    expect(panel).toHaveAttribute('data-ready-endpoints', '/live/script-version/search|/live/script-version/get|/live/script-version/rollback')
    expect(panel).toHaveAttribute('data-no-local-version-fallback', 'true')
    expect(await screen.findAllByTestId('version-history-item')).toHaveLength(2)

    fireEvent.click(screen.getByTestId('version-history-compare-button'))
    expect(await screen.findByTestId('version-history-inline-diff-panel')).toHaveAttribute('data-contract-scope', 'live-version-history-inline-diff')
    expect(screen.getByTestId('version-history-inline-diff-old-surface')).toHaveAttribute('data-contract-source', '/live/script-version/get')

    fireEvent.click(screen.getByTestId('version-history-rollback-button'))
    await waitFor(() => {
      expect(rollbackScriptVersion).toHaveBeenCalledWith({ scriptId: 18, versionNumber: 1 })
    })

    vi.mocked(searchScriptVersions).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 50 } as never)
    renderVersionPanel()
    expect(await screen.findByTestId('version-history-empty-state')).toHaveAttribute('data-no-local-version-fallback', 'true')

    vi.mocked(searchScriptVersions).mockRejectedValue(new Error('version search down') as never)
    renderVersionPanel()
    expect(await screen.findByTestId('version-history-load-error')).toHaveTextContent('/live/script-version/search 版本历史加载失败')
  })

  it('marks approval submit and revoke flows as props-owned', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const onRevoke = vi.fn().mockResolvedValue(undefined)

    renderWithProviders(
      <>
        <ApprovalStatusBadge approvalStatus={0} onSubmit={onSubmit} />
        <ApprovalStatusBadge approvalStatus={1} onRevoke={onRevoke} />
      </>,
    )

    const roots = screen.getAllByTestId('approval-status-badge-root')
    expect(roots[0]).toHaveAttribute('data-contract-scope', 'live-script-approval-status-props')
    expect(roots[0]).toHaveAttribute('data-can-submit', 'true')
    expect(roots[1]).toHaveAttribute('data-can-revoke', 'true')

    fireEvent.click(screen.getByTestId('approval-submit-open-button'))
    expect(screen.getByTestId('approval-submit-dialog')).toHaveAttribute('data-contract-scope', 'live-script-approval-submit-dialog')
    fireEvent.change(screen.getByTestId('approval-submit-comment-input'), { target: { value: '请审核' } })
    fireEvent.click(screen.getByTestId('approval-submit-confirm-button'))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith('请审核'))

    fireEvent.click(screen.getByTestId('approval-revoke-button'))
    await waitFor(() => expect(onRevoke).toHaveBeenCalled())
  })
})
