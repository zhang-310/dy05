import { describe, it, expect, vi, beforeEach } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders } from '@/test/utils'
import { ScriptTab } from './ScriptTab'
import {
  checkViolation,
  deleteLiveScript,
  exportScripts,
  generateFull,
  generateOpening,
  generateProduct,
  saveBatchToLibrary,
  saveLiveScript,
  saveScriptToLibrary,
  updateScriptExecuted,
} from '@/api/live'

vi.mock('@/hooks/useDebouncedCallback', () => ({
  useThrottledCallback: (fn: (...args: unknown[]) => void) => fn,
}))

vi.mock('@/api/live', () => ({
  generateOpening: vi.fn(),
  generateProduct: vi.fn(),
  generateFull: vi.fn(),
  checkViolation: vi.fn(),
  saveLiveScript: vi.fn(),
  deleteLiveScript: vi.fn(),
  updateScriptExecuted: vi.fn(),
  saveScriptToLibrary: vi.fn(),
  saveBatchToLibrary: vi.fn(),
  exportScripts: vi.fn(),
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(<AppThemeProvider>{ui}</AppThemeProvider>)
}

const session = {
  id: 18,
  scriptStyle: 'friendly',
}

const products = [
  { id: 11, productId: 101, productName: '屏障修护精华', position: 1 },
  { id: 12, productId: 102, productName: '敏感肌面霜', position: 2 },
]

const scripts = [
  {
    id: 201,
    sequenceNo: 1,
    scriptType: 'opening',
    scriptContent: '欢迎来到直播间',
    executed: 0,
    estimatedDurationSeconds: 45,
  },
  {
    id: 202,
    sequenceNo: 2,
    scriptType: 'product',
    scriptContent: '屏障修护精华讲解话术',
    executed: 1,
    estimatedDurationSeconds: 90,
  },
  {
    id: 203,
    sequenceNo: 3,
    scriptType: 'transition',
    scriptContent: '从精华自然过渡到面霜',
    executed: 0,
    estimatedDurationSeconds: 30,
  },
]

function renderScriptTab(overrides?: Partial<React.ComponentProps<typeof ScriptTab>>) {
  const toast = vi.fn()
  const onRefresh = vi.fn()
  renderDark(
    <ScriptTab
      scripts={scripts}
      sessionId={18}
      session={session}
      products={products}
      onRefresh={onRefresh}
      toast={toast}
      {...overrides}
    />,
  )
  return { toast, onRefresh }
}

describe('ScriptTab legacy contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(generateOpening).mockResolvedValue({
      ragRefs: [{ title: '修护知识', score: 0.82, contentPreview: '强调修护体验' }],
    } as never)
    vi.mocked(generateProduct).mockResolvedValue({
      ragRefs: [{ title: '产品知识', score: 0.91, contentPreview: '产品卖点来源' }],
    } as never)
    vi.mocked(generateFull).mockResolvedValue([] as never)
    vi.mocked(checkViolation).mockResolvedValue({ passed: false, violations: ['绝对化表达'], violationCount: 1 } as never)
    vi.mocked(saveLiveScript).mockResolvedValue(201 as never)
    vi.mocked(deleteLiveScript).mockResolvedValue(undefined as never)
    vi.mocked(updateScriptExecuted).mockResolvedValue(undefined as never)
    vi.mocked(saveScriptToLibrary).mockResolvedValue(1 as never)
    vi.mocked(saveBatchToLibrary).mockResolvedValue(3 as never)
    vi.mocked(exportScripts).mockResolvedValue('# 直播话术' as never)
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:script-export')
    globalThis.URL.revokeObjectURL = vi.fn()
  })

  it('marks legacy root, toolbar, script list and empty no-fallback contract', () => {
    renderScriptTab()

    const root = screen.getByTestId('script-tab-legacy-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-script-tab-legacy-api-panel')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/generate-opening'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-script-fallback'))
    expect(root).toHaveAttribute('data-no-local-script-fallback', 'true')
    expect(root).toHaveAttribute('data-no-mock-rag-refs', 'true')
    expect(root).toHaveAttribute('data-script-count', '3')
    expect(root).toHaveAttribute('data-product-count', '2')
    expect(screen.getByTestId('script-tab-script-list')).toHaveAttribute('data-row-count', '3')
    expect(screen.getByTestId('script-tab-estimated-duration')).toHaveAttribute('data-estimated-seconds', '165')
    expect(screen.getAllByTestId('script-tab-script-card')[0]).toHaveAttribute('data-contract-scope', 'live-script-tab-script-card')

    cleanup()
    renderScriptTab({ scripts: [], products: [] })
    expect(screen.getByTestId('script-tab-empty-state')).toHaveAttribute('data-contract-scope', 'live-script-tab-empty-state')
    expect(screen.getByTestId('script-tab-generate-product-open-button')).toHaveAttribute('data-disabled-reason', 'no-products')
    expect(screen.getByTestId('script-tab-generate-full-button')).toHaveAttribute('data-disabled-reason', 'no-products')
  })

  it('delegates generation calls and renders real rag refs from backend response', async () => {
    const { toast, onRefresh } = renderScriptTab()

    fireEvent.click(screen.getByTestId('script-tab-generate-opening-button'))

    await waitFor(() => {
      expect(generateOpening).toHaveBeenCalledWith({ sessionId: 18, genStyle: 'friendly', useKbRef: true })
    })
    expect(toast).toHaveBeenCalledWith('开场话术生成成功', 'success')
    expect(onRefresh).toHaveBeenCalledTimes(1)
    expect(await screen.findByTestId('script-tab-rag-ref-panel')).toHaveAttribute('data-contract-scope', 'live-script-tab-rag-ref-readonly')
    expect(screen.getByTestId('script-tab-rag-ref-row')).toHaveAttribute('data-score', '0.82')

    fireEvent.click(screen.getByTestId('script-tab-generate-full-button'))
    await waitFor(() => {
      expect(generateFull).toHaveBeenCalledWith({ sessionId: 18, genStyle: 'friendly', useKbRef: true })
    })
  })

  it('opens product generation dialog and delegates selected product generation', async () => {
    const { toast, onRefresh } = renderScriptTab()

    fireEvent.click(screen.getByTestId('script-tab-generate-product-open-button'))
    const dialog = await screen.findByTestId('script-tab-product-generate-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-script-tab-product-generate-dialog')
    expect(dialog).toHaveAttribute('data-product-count', '2')

    fireEvent.click(screen.getAllByTestId('script-tab-product-generate-row')[0])
    await waitFor(() => {
      expect(generateProduct).toHaveBeenCalledWith({ sessionId: 18, productId: 101, genStyle: 'friendly', useKbRef: true })
    })
    expect(toast).toHaveBeenCalledWith('产品话术生成成功', 'success')
    expect(onRefresh).toHaveBeenCalledTimes(1)
  })

  it('handles edit save, violation check, executed toggle and library save through declared endpoints', async () => {
    const { toast, onRefresh } = renderScriptTab()

    fireEvent.click(screen.getAllByTestId('script-tab-edit-open-button')[0])
    expect(screen.getAllByTestId('script-tab-script-card')[0]).toHaveAttribute('data-editing', 'true')
    fireEvent.change(screen.getByTestId('script-tab-edit-content-input'), { target: { value: '更新后的开场话术' } })
    fireEvent.click(screen.getByTestId('script-tab-save-edit-button'))
    await waitFor(() => {
      expect(saveLiveScript).toHaveBeenCalledWith({ id: 201, sessionId: 18, scriptContent: '更新后的开场话术' })
    })
    expect(toast).toHaveBeenCalledWith('保存成功', 'success')

    fireEvent.click(screen.getAllByTestId('script-tab-check-violation-button')[0])
    await waitFor(() => {
      expect(checkViolation).toHaveBeenCalledWith({ scriptId: 201 })
    })
    expect(await screen.findByTestId('script-tab-violation-result')).toHaveAttribute('data-passed', 'false')

    fireEvent.click(screen.getAllByTestId('script-tab-executed-toggle-button')[0])
    await waitFor(() => {
      expect(updateScriptExecuted).toHaveBeenCalledWith(201, 1)
    })

    fireEvent.click(screen.getAllByTestId('script-tab-save-one-library-button')[0])
    await waitFor(() => {
      expect(saveScriptToLibrary).toHaveBeenCalledWith({ scriptId: 201 })
    })

    fireEvent.click(screen.getByTestId('script-tab-save-batch-library-button'))
    await waitFor(() => {
      expect(saveBatchToLibrary).toHaveBeenCalledWith({ sessionId: 18 })
    })

    expect(onRefresh).toHaveBeenCalled()
  })

  it('delegates delete and export through declared endpoints', async () => {
    const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    const { toast, onRefresh } = renderScriptTab()

    fireEvent.click(screen.getAllByTestId('script-tab-delete-open-button')[0])
    const dialog = await screen.findByTestId('script-tab-delete-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-script-tab-delete-dialog')
    expect(dialog).toHaveAttribute('data-script-id', '201')
    fireEvent.click(screen.getByTestId('script-tab-delete-confirm-button'))
    await waitFor(() => {
      expect(deleteLiveScript).toHaveBeenCalledWith(201)
    })
    expect(toast).toHaveBeenCalledWith('删除成功', 'success')
    expect(onRefresh).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByTestId('script-tab-export-button'))
    await waitFor(() => {
      expect(exportScripts).toHaveBeenCalledWith(18)
    })
    expect(anchorClick).toHaveBeenCalled()
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith('blob:script-export')
  })
})
