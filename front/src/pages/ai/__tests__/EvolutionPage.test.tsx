import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import EvolutionPage from '../EvolutionPage'
import { aiApi } from '@/api/ai'

vi.mock('@/pages/ai/evolution/tabs/TaskQueueTab', () => ({
  TaskQueueTab: ({ scopeKbId }: { scopeKbId: string }) => <div>任务队列 Mock {scopeKbId || 'all'}</div>,
}))
vi.mock('@/pages/ai/evolution/tabs/TopicsTab', () => ({
  TopicsTab: () => <div>主题池 Mock</div>,
}))
vi.mock('@/pages/ai/evolution/tabs/QualityHeatmapTab', () => ({
  QualityHeatmapTab: () => <div>质量热力图 Mock</div>,
}))
vi.mock('@/pages/ai/evolution/tabs/EvolutionMapTab', () => ({
  EvolutionMapTab: () => <div>进化地图 Mock</div>,
}))
vi.mock('@/pages/ai/evolution/tabs/RoiTab', () => ({
  RoiTab: () => <div>ROI Mock</div>,
}))
vi.mock('@/pages/ai/evolution/tabs/ReviewTab', () => ({
  ReviewTab: () => <div>审核 Mock</div>,
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
    evolveStatus: vi.fn(),
    evolutionExecute: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <EvolutionPage />
    </MemoryRouter>,
  )
}

function renderPageWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <EvolutionPage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('EvolutionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(aiApi.kbList).mockResolvedValue([
      { id: 7, kbName: '进化测试库', description: '', totalDocuments: 4, status: 1, createTime: '' },
    ] as never)
    vi.mocked(aiApi.evolveStatus).mockResolvedValue({
      running: false,
      circuitBroken: false,
      lastRunTime: '2026-05-20 23:00:00',
      nextRunTime: '2026-05-21 00:00:00',
    } as never)
    vi.mocked(aiApi.evolutionExecute).mockResolvedValue({ executionId: 99, started: 1 } as never)
  })

  it('renders engine diagnostics and default all-KB task scope', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '进化引擎' })).toBeInTheDocument()
    expect(screen.getByTestId('evolution-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/knowledge-base/list,/ai/evolution/status,/ai/evolution/execution/execute',
    )
    expect(screen.getByTestId('evolution-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/evolution/mock,/ai/evolution/local-status,/ai/evolution/local-execute,/ai/evolution/static-status',
    )
    expect(screen.getByTestId('evolution-page')).toHaveAttribute('data-no-local-status-fallback', 'true')
    expect(screen.getByTestId('evolution-page')).toHaveAttribute('data-no-local-execute-fallback', 'true')
    expect(screen.getByTestId('evolution-page')).toHaveAttribute('data-no-static-status-fallback', 'true')
    expect(screen.getByText('任务队列 Mock all')).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.kbList).toHaveBeenCalledWith({ page: 0, rows: 200 })
      expect(aiApi.evolveStatus).toHaveBeenCalled()
    })
    expect(await screen.findByText('熔断正常')).toBeInTheDocument()
    expect(screen.getByText('可选 1 个知识库')).toBeInTheDocument()
    expect(screen.getByText(/上次：2026-05-20 23:00:00/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-status-cards')).toHaveAttribute('data-no-local-status-fallback', 'true')
    expect(screen.getByTestId('evolution-scope-run-surface')).toHaveAttribute('data-no-local-execute-fallback', 'true')
    expect(screen.getByTestId('evolution-tab-host')).toHaveAttribute('data-no-page-level-tab-api', 'true')
  })

  it('runs all agents with the selected knowledge base scope', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByLabelText('知识库范围'))
    await user.click(await screen.findByRole('option', { name: '进化测试库' }))

    expect(screen.getByText('任务队列 Mock 7')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '立即运行' }))
    await waitFor(() => {
      expect(aiApi.evolutionExecute).toHaveBeenCalledWith({
        agentType: 'all',
        targetKbId: 7,
      })
      expect(toast).toHaveBeenCalledWith('进化引擎已启动', 'success')
    })
  })

  it('shows execution endpoint and keeps selected scope when immediate run fails', async () => {
    const user = userEvent.setup()
    vi.mocked(aiApi.evolutionExecute).mockRejectedValueOnce(new Error('executor offline'))
    renderPage()

    await user.click(await screen.findByLabelText('知识库范围'))
    await user.click(await screen.findByRole('option', { name: '进化测试库' }))
    await user.click(screen.getByRole('button', { name: '立即运行' }))

    expect(await screen.findByText(/立即运行失败（POST \/ai\/evolution\/execution\/execute）：executor offline/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-execute-error')).toHaveAttribute('data-no-local-execute-fallback', 'true')
    expect(screen.getByTestId('evolution-execute-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/当前知识库范围会保留/)).toBeInTheDocument()
    expect(screen.getByText('任务队列 Mock 7')).toBeInTheDocument()
  })

  it('disables immediate execution when backend reports circuit breaker', async () => {
    vi.mocked(aiApi.evolveStatus).mockResolvedValue({
      running: false,
      circuitBroken: true,
      message: '连续失败超过阈值',
    } as never)
    renderPage()

    expect(await screen.findByText('熔断触发')).toBeInTheDocument()
    expect(screen.getByText('后端状态说明：连续失败超过阈值')).toBeInTheDocument()
    expect(screen.getByTestId('evolution-run-disabled-downgrade')).toHaveAttribute('data-no-local-execute-fallback', 'true')
    expect(screen.getByTestId('evolution-status-message')).toHaveAttribute('data-no-static-status-fallback', 'true')
    expect(screen.getByRole('button', { name: '立即运行' })).toBeDisabled()
  })

  it('uses theme-aware immediate run action text in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme()

    expect(await screen.findByText('熔断正常')).toBeInTheDocument()
    const runAction = screen.getByTestId('evolution-run-action-surface')
    expect(runAction).not.toHaveStyle({ color: 'rgb(0, 0, 0)' })
  })

  it('shows retryable infrastructure error and refreshes header data', async () => {
    vi.mocked(aiApi.evolveStatus).mockRejectedValueOnce(new Error('status offline'))
    renderPage()

    expect(await screen.findByText(/进化引擎基础数据加载失败/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-base-data-error')).toHaveAttribute('data-no-local-status-fallback', 'true')
    expect(screen.getByTestId('evolution-base-data-error')).toHaveAttribute('data-no-local-kb-fallback', 'true')
    vi.mocked(aiApi.evolveStatus).mockResolvedValue({
      running: true,
      circuitBroken: false,
    } as never)

    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    await waitFor(() => {
      expect(aiApi.evolveStatus).toHaveBeenCalledTimes(2)
    })
  })

  it('keeps selected scope and blocks local execution when knowledge base list fails', async () => {
    vi.mocked(aiApi.kbList).mockRejectedValueOnce(new Error('kb offline'))
    renderPage()

    expect(await screen.findByText(/进化引擎基础数据加载失败/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-base-data-error')).toHaveAttribute('data-no-local-kb-fallback', 'true')
    expect(screen.getByTestId('evolution-run-disabled-downgrade')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByRole('button', { name: '立即运行' })).toBeDisabled()
    expect(aiApi.evolutionExecute).not.toHaveBeenCalled()
  })
})
