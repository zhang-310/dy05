import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import TaskModelConfigPage from '../TaskModelConfigPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    taskModelConfigList: vi.fn(),
    adminModelsList: vi.fn(),
    taskModelConfigSave: vi.fn(),
    taskModelConfigDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('TaskModelConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.taskModelConfigList).mockResolvedValue([
      {
        id: 1,
        taskCode: 'kb_search',
        taskName: '知识库检索',
        taskGroup: 'ai',
        primaryModelId: 10,
        fallbackModelId: 11,
        fallback2ModelId: null,
        timeoutSeconds: 30,
        maxRetries: 1,
        sortOrder: 1,
        status: 1,
      },
    ] as never)
    vi.mocked(aiApi.adminModelsList).mockResolvedValue([
      {
        id: 10,
        modelName: 'Ollama Qwen',
        modelProvider: 'ollama',
        modelVersion: 'qwen2.5:7b',
        apiKeyMasked: '***',
        status: 1,
      },
      {
        id: 11,
        modelName: 'DeepSeek',
        modelProvider: 'deepseek',
        modelVersion: 'deepseek-chat',
        apiKeyMasked: '***',
        status: 1,
      },
    ] as never)
  })

  it('renders task model mapping coverage', async () => {
    renderWithProviders(
      <MemoryRouter>
        <TaskModelConfigPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '任务模型映射配置' })).toBeInTheDocument()
    expect(screen.getByTestId('task-model-config-page')).toHaveAttribute('data-no-local-mapping-fallback', 'true')
    expect(screen.getByTestId('task-model-config-page')).toHaveAttribute('data-no-optimistic-inline-mutation', 'true')

    await waitFor(() => {
      expect(aiApi.taskModelConfigList).toHaveBeenCalled()
      expect(aiApi.adminModelsList).toHaveBeenCalled()
    })

    expect(await screen.findByText('任务配置')).toBeInTheDocument()
    expect(screen.getByText('主模型覆盖率')).toBeInTheDocument()
    expect(screen.getByText('备用模型覆盖')).toBeInTheDocument()
    expect(screen.getByText('启用 2 个，禁用 0 个')).toBeInTheDocument()
    expect(screen.getByText('映射覆盖诊断')).toBeInTheDocument()
    expect(screen.getByTestId('task-model-config-diagnostics-contract')).toHaveAttribute('data-no-plaintext-key-display', 'true')
    expect(screen.getByText('主模型缺口 0')).toBeInTheDocument()
    expect(screen.getByText('备用缺口 0')).toBeInTheDocument()
    expect(screen.getByText('引用禁用模型 0')).toBeInTheDocument()
    expect(screen.getByText(/启用任务已配置主模型与备用模型/)).toBeInTheDocument()
  })

  it('shows mapping gaps, model-list failure and save failure inline', async () => {
    vi.mocked(aiApi.taskModelConfigList).mockResolvedValueOnce([
      {
        id: 2,
        taskCode: 'script_gen',
        taskName: '话术生成',
        taskGroup: 'ai',
        primaryModelId: null,
        fallbackModelId: null,
        fallback2ModelId: null,
        timeoutSeconds: 30,
        maxRetries: 1,
        sortOrder: 2,
        status: 1,
      },
    ] as never)
    vi.mocked(aiApi.adminModelsList).mockRejectedValue(new Error('models unavailable'))
    vi.mocked(aiApi.taskModelConfigSave).mockRejectedValueOnce(new Error('主模型 对应的模型未启用'))

    renderWithProviders(
      <MemoryRouter>
        <TaskModelConfigPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/模型列表加载失败：models unavailable/)).toBeInTheDocument()
    expect(screen.getByTestId('task-model-config-model-list-error')).toHaveAttribute('data-no-local-model-option-fallback', 'true')
    expect(await screen.findByText('主模型缺口 1')).toBeInTheDocument()
    expect(screen.getByText(/未配置主模型：话术生成 \(script_gen\)/)).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('task-model-config-add'))
    fireEvent.change(screen.getByTestId('task-model-config-field-taskCode').querySelector('input')!, {
      target: { value: 'script_gen' },
    })
    fireEvent.change(screen.getByTestId('task-model-config-field-taskName').querySelector('input')!, {
      target: { value: '话术生成' },
    })
    fireEvent.click(screen.getByTestId('task-model-config-dialog-save'))

    expect(await screen.findByText(/任务模型配置保存失败（\/ai\/admin\/task-model-config\/save）：主模型 对应的模型未启用/)).toBeInTheDocument()
    expect(screen.getByTestId('task-model-config-save-error')).toHaveAttribute('data-no-optimistic-inline-mutation', 'true')
    expect(screen.getByTestId('task-model-config-form-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('script_gen')).toBeInTheDocument()
    expect(screen.getByDisplayValue('话术生成')).toBeInTheDocument()
  })

  it('shows disabled model references and excludes disabled models from form selects', async () => {
    vi.mocked(aiApi.taskModelConfigList).mockResolvedValueOnce([
      {
        id: 3,
        taskCode: 'live_script',
        taskName: '直播话术',
        taskGroup: 'live',
        primaryModelId: 12,
        primaryModelName: 'Disabled GPT',
        fallbackModelId: null,
        fallback2ModelId: null,
        timeoutSeconds: 30,
        maxRetries: 1,
        sortOrder: 3,
        status: 1,
      },
    ] as never)
    vi.mocked(aiApi.adminModelsList).mockResolvedValueOnce([
      {
        id: 10,
        modelName: 'Ollama Qwen',
        modelProvider: 'ollama',
        modelVersion: 'qwen2.5:7b',
        apiKeyMasked: '',
        status: 1,
      },
      {
        id: 12,
        modelName: 'Disabled GPT',
        modelProvider: 'openai',
        modelVersion: 'gpt-4o',
        apiKeyMasked: '****gpt',
        status: 0,
      },
    ] as never)

    renderWithProviders(
      <MemoryRouter>
        <TaskModelConfigPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('引用禁用模型 1')).toBeInTheDocument()
    expect(screen.getByText(/有配置引用了已禁用模型：直播话术 \(live_script\)/)).toBeInTheDocument()
    expect(screen.getByText('启用 1 个，禁用 1 个')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('task-model-config-add'))
    const dialog = await screen.findByRole('dialog', { name: '新增任务模型配置' })
    fireEvent.mouseDown(within(dialog).getByLabelText('主模型'))
    expect(await screen.findByRole('option', { name: 'Ollama Qwen (#10)' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Disabled GPT (#12)' })).not.toBeInTheDocument()
  })

  it('saves inline model changes and reports delete errors by source', async () => {
    vi.mocked(aiApi.taskModelConfigSave).mockResolvedValueOnce(1 as never)
    vi.mocked(aiApi.taskModelConfigDelete).mockRejectedValueOnce(new Error('delete denied'))

    renderWithProviders(
      <MemoryRouter>
        <TaskModelConfigPage />
      </MemoryRouter>,
    )

    await screen.findByText('映射覆盖诊断')
    fireEvent.mouseDown(screen.getByTestId('task-model-config-inline-primary-1').querySelector('[role="combobox"]')!)
    fireEvent.click(await screen.findByRole('option', { name: 'DeepSeek (#11)' }))

    await waitFor(() => {
      expect(aiApi.taskModelConfigSave).toHaveBeenCalledWith(expect.objectContaining({
        id: 1,
        taskCode: 'kb_search',
        taskName: '知识库检索',
        primaryModelId: 11,
      }))
    })

    fireEvent.click(screen.getByTestId('task-model-config-delete-1'))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))
    expect(await screen.findByText(/任务模型配置删除失败（\/ai\/admin\/task-model-config\/delete，id=1）：delete denied/)).toBeInTheDocument()
    expect(screen.getByTestId('task-model-config-delete-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByText('知识库检索')).toBeInTheDocument()
  })
})
