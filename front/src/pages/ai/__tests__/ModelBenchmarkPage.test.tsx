import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ModelBenchmarkPage from '../ModelBenchmarkPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    taskModelConfigList: vi.fn(),
    modelBenchmarkComparison: vi.fn(),
    modelBenchmarkBestModel: vi.fn(),
    modelBenchmarkRecord: vi.fn(),
  },
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns }: any) => (
      <div data-testid="model-benchmark-grid">
        {rows.length === 0 ? <div>暂无数据</div> : null}
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell ? col.renderCell({ row, value: row[col.field] }) : String(row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

function renderPage() {
  renderWithProviders(
    <MemoryRouter>
      <ModelBenchmarkPage />
    </MemoryRouter>,
  )
}

describe('ModelBenchmarkPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.taskModelConfigList).mockResolvedValue([
      {
        id: 1,
        taskCode: 'script_gen',
        taskName: '话术生成',
        taskGroup: 'ai',
        maxRetries: 1,
        sortOrder: 1,
        status: 1,
      },
      {
        id: 2,
        taskCode: 'agent_chat',
        taskName: '智能体对话',
        taskGroup: 'ai',
        maxRetries: 1,
        sortOrder: 2,
        status: 1,
      },
    ] as never)
    vi.mocked(aiApi.modelBenchmarkComparison).mockResolvedValue([
      {
        modelId: 9,
        modelName: 'Ollama Qwen',
        taskCode: 'script_gen',
        avgLatencyMs: 800,
        successRate: 0.92,
        avgTokens: 300,
        totalCalls: 12,
      },
    ] as never)
    vi.mocked(aiApi.modelBenchmarkBestModel).mockResolvedValue({
      modelId: 9,
      modelName: 'Ollama Qwen',
      taskCode: 'script_gen',
      priority: 'latency',
    } as never)
    vi.mocked(aiApi.modelBenchmarkRecord).mockResolvedValue(undefined as never)
  })

  it('renders benchmark rows, coverage diagnostics and a real recommendation', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '模型基准测试' })).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-page')).toHaveAttribute('data-no-mock-ranking-fallback', 'true')
    expect(screen.getByTestId('model-benchmark-page')).toHaveAttribute('data-no-plaintext-key-display', 'true')

    await waitFor(() => {
      expect(aiApi.taskModelConfigList).toHaveBeenCalled()
      expect(aiApi.modelBenchmarkComparison).toHaveBeenCalledWith({ taskCode: undefined })
    })

    expect(await screen.findByText('Ollama Qwen')).toBeInTheDocument()
    expect(screen.getByText('基准覆盖诊断')).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-boundary-contract')).toHaveAttribute('data-no-auto-sample-record', 'true')
    expect(screen.getByTestId('model-benchmark-diagnostics-contract')).toHaveAttribute('data-no-local-task-option-fallback', 'true')
    expect(screen.getByText('配置无样本 1')).toBeInTheDocument()
    expect(screen.getByText(/智能体对话 \(agent_chat\)/)).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByTestId('model-benchmark-task-filter').querySelector('[role="combobox"]')!)
    fireEvent.click(await screen.findByRole('option', { name: '话术生成 (script_gen)' }))

    await waitFor(() => {
      expect(aiApi.modelBenchmarkBestModel).toHaveBeenCalledWith('script_gen', 'latency')
    })
    expect(await screen.findByText(/当前任务推荐模型/)).toBeInTheDocument()
    expect(screen.getAllByText('Ollama Qwen').length).toBeGreaterThan(0)
    expect(screen.getByTestId('model-benchmark-filter-contract')).toHaveAttribute('data-no-local-best-model-fallback', 'true')
  })

  it('records a benchmark sample through the real record endpoint contract', async () => {
    renderPage()

    await screen.findByText('基准覆盖诊断')
    fireEvent.mouseDown(screen.getByTestId('model-benchmark-task-filter').querySelector('[role="combobox"]')!)
    fireEvent.click(await screen.findByRole('option', { name: '话术生成 (script_gen)' }))
    fireEvent.click(screen.getByRole('button', { name: '录入样本' }))
    expect(screen.getByTestId('model-benchmark-record-form')).toHaveAttribute('data-no-auto-sample-record', 'true')
    fireEvent.change(screen.getByLabelText('模型 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '提交' }))

    await waitFor(() => {
      expect(vi.mocked(aiApi.modelBenchmarkRecord).mock.calls[0]?.[0]).toEqual({
        modelId: 9,
        taskCode: 'script_gen',
        latencyMs: 800,
        tokensUsed: 120,
        success: true,
      })
    })
  })

  it('shows explicit service-not-injected downgrade for empty comparison and modelId zero', async () => {
    vi.mocked(aiApi.modelBenchmarkComparison).mockResolvedValueOnce([] as never)
    vi.mocked(aiApi.modelBenchmarkBestModel).mockResolvedValueOnce({
      modelId: 0,
      modelName: '',
      taskCode: 'script_gen',
      priority: 'latency',
    } as never)

    renderPage()

    expect(await screen.findByText(/后端未返回 benchmark 样本/)).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-empty')).toHaveAttribute('data-no-mock-ranking-fallback', 'true')
    expect(screen.getAllByText(/页面不会用 mock 排行替代/).length).toBeGreaterThan(0)
    expect(screen.getByTestId('model-benchmark-no-rows')).toHaveAttribute('data-source-endpoint', '/ai/model-benchmark/comparison')

    fireEvent.mouseDown(screen.getByTestId('model-benchmark-task-filter').querySelector('[role="combobox"]')!)
    fireEvent.click(await screen.findByRole('option', { name: '话术生成 (script_gen)' }))

    expect(await screen.findByText('暂无聚合数据')).toBeInTheDocument()
    expect(screen.getByText(/后端会返回 modelId=0/)).toBeInTheDocument()
  })

  it('surfaces comparison, best-model and record errors with endpoint sources', async () => {
    vi.mocked(aiApi.modelBenchmarkComparison).mockRejectedValueOnce(new Error('comparison timeout') as never)
    vi.mocked(aiApi.modelBenchmarkBestModel).mockRejectedValueOnce(new Error('best unavailable') as never)
    vi.mocked(aiApi.modelBenchmarkRecord).mockRejectedValueOnce(new Error('modelId 不能为空') as never)

    renderPage()

    expect(await screen.findByText(/模型基准聚合加载失败：comparison timeout/)).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-comparison-error')).toHaveAttribute('data-no-mock-ranking-fallback', 'true')
    expect(screen.getAllByText(/\/ai\/model-benchmark\/comparison/).length).toBeGreaterThan(0)

    fireEvent.mouseDown(screen.getByTestId('model-benchmark-task-filter').querySelector('[role="combobox"]')!)
    fireEvent.click(await screen.findByRole('option', { name: '话术生成 (script_gen)' }))
    expect(await screen.findByText(/推荐模型加载失败：best unavailable/)).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-best-error')).toHaveAttribute('data-no-local-best-model-fallback', 'true')
    expect(screen.getAllByText(/\/ai\/model-benchmark\/best-model/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '录入样本' }))
    fireEvent.change(screen.getByLabelText('模型 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '提交' }))
    expect(await screen.findByText(/基准样本录入失败：modelId 不能为空/)).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-record-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getAllByText(/\/ai\/model-benchmark\/record/).length).toBeGreaterThan(0)
    expect(screen.getByText(/录入面板和输入值会保留/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('9')).toBeInTheDocument()
  })

  it('surfaces benchmark rows without task config and low-quality rows', async () => {
    vi.mocked(aiApi.modelBenchmarkComparison).mockResolvedValueOnce([
      {
        modelId: 8,
        modelName: 'Slow Model',
        taskCode: 'unknown_task',
        avgLatencyMs: 4200,
        successRate: 0.55,
        avgTokens: 900,
        totalCalls: 4,
      },
    ] as never)

    renderPage()

    expect(await screen.findByText('样本无配置 1')).toBeInTheDocument()
    expect(screen.getByTestId('model-benchmark-diagnostics-contract')).toHaveAttribute('data-no-mock-ranking-fallback', 'true')
    expect(screen.getByText('低质样本 1')).toBeInTheDocument()
    expect(screen.getAllByText(/Slow Model\/unknown_task/).length).toBeGreaterThan(0)
  })
})
