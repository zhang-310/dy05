import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ModelsConfigPage from '../ModelsConfigPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    adminModelsList: vi.fn(),
    adminModelsSave: vi.fn(),
    adminModelsDelete: vi.fn(),
    adminModelsSetDefault: vi.fn(),
    adminModelsTestConnection: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ModelsConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.adminModelsList).mockResolvedValue([
      {
        id: 1,
        modelName: 'Ollama Qwen',
        modelProvider: 'ollama',
        modelVersion: 'qwen2.5:7b',
        apiBaseUrl: 'http://host.docker.internal:11434',
        apiKeyMasked: '***',
        resolvedBaseUrl: 'http://host.docker.internal:11434',
        status: 1,
        isDefault: 1,
      },
    ] as never)
  })

  it('renders model summary and list', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '模型配置' })).toBeInTheDocument()
    expect(screen.getByTestId('models-config-page')).toHaveAttribute('data-secret-write-only', 'true')
    expect(screen.getByTestId('models-config-page')).toHaveAttribute('data-no-plaintext-key-display', 'true')

    await waitFor(() => {
      expect(aiApi.adminModelsList).toHaveBeenCalled()
    })

    expect(await screen.findByText('模型总数')).toBeInTheDocument()
    expect(screen.getByText('默认模型')).toBeInTheDocument()
    expect(screen.getAllByText('Ollama Qwen').length).toBeGreaterThan(0)
    expect(screen.getByText('自定义网关')).toBeInTheDocument()
    expect(screen.getByText('模型可用性诊断')).toBeInTheDocument()
    expect(screen.getByTestId('models-config-diagnostics-contract')).toHaveAttribute('data-no-local-model-fallback', 'true')
    expect(screen.getByText('关键配置正常')).toBeInTheDocument()
  })

  it('shows missing default, base-url and quota diagnostics without fake fallbacks', async () => {
    vi.mocked(aiApi.adminModelsList).mockResolvedValueOnce([
      {
        id: 2,
        modelName: 'DeepSeek Chat',
        modelProvider: 'deepseek',
        modelVersion: 'deepseek-chat',
        apiBaseUrl: '',
        apiKeyMasked: '',
        resolvedBaseUrl: '',
        status: 1,
        isDefault: 0,
        quotaLimit: 100,
        quotaUsed: 96,
      },
      {
        id: 3,
        modelName: 'Ollama Vision',
        modelProvider: 'ollama',
        modelVersion: 'qwen-vl',
        apiKeyMasked: '',
        resolvedBaseUrl: 'http://host.docker.internal:11434',
        status: 1,
        isDefault: 0,
        quotaUsed: 5,
      },
    ] as never)

    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('存在关键配置风险')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '详情' }))
    expect(screen.getByText(/当前有启用模型但没有默认模型/)).toBeInTheDocument()
    expect(screen.getByText(/启用模型缺少 resolvedBaseUrl：DeepSeek Chat/)).toBeInTheDocument()
    expect(screen.getByText(/DeepSeek Chat 未返回模型级密钥/)).toBeInTheDocument()
    expect(screen.getByText(/DeepSeek Chat 96%/)).toBeInTheDocument()
    expect(screen.getByText(/有 1 个启用模型未配置模型级 quotaLimit/)).toBeInTheDocument()
    expect(screen.getByText('依赖环境变量')).toBeInTheDocument()
    expect(screen.getByText('5 / 未配置')).toBeInTheDocument()
  })

  it('tests connection and renders latest result chip', async () => {
    vi.mocked(aiApi.adminModelsTestConnection).mockResolvedValueOnce({
      success: false,
      errorMsg: 'ollama connection refused',
      tokensUsed: 0,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    await screen.findAllByText('Ollama Qwen')
    fireEvent.click(screen.getByLabelText('测试连接：Ollama Qwen'))

    await waitFor(() => {
      expect(aiApi.adminModelsTestConnection).toHaveBeenCalledWith(1)
    })
    expect(await screen.findByText('最近连通性测试')).toBeInTheDocument()
    expect(screen.getByText('Ollama Qwen：ollama connection refused')).toBeInTheDocument()
    expect(screen.getByText('失败')).toBeInTheDocument()
  })

  it('submits add form with provider endpoint and base-url contract', async () => {
    vi.mocked(aiApi.adminModelsSave).mockResolvedValueOnce(undefined as never)

    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    await screen.findByText('模型可用性诊断')
    fireEvent.click(screen.getByRole('button', { name: '新增模型' }))

    const dialog = await screen.findByRole('dialog', { name: '新增模型' })
    fireEvent.change(within(dialog).getByLabelText('模型名称（展示用）'), { target: { value: 'DeepSeek V3' } })
    fireEvent.mouseDown(within(dialog).getByLabelText('提供商'))
    fireEvent.click(await screen.findByRole('option', { name: 'DeepSeek' }))
    expect(await screen.findByText(/读取 ai.deepseek.api_url/)).toBeInTheDocument()
    fireEvent.change(within(dialog).getByLabelText('模型 ID（厂商 API 中的 model 名称）'), { target: { value: 'deepseek-chat' } })
    fireEvent.change(within(dialog).getByLabelText('自定义 API Base URL（可选）'), { target: { value: 'https://gateway.example/v1' } })
    fireEvent.change(within(dialog).getByLabelText('API Key（可选；编辑时留空不修改）'), { target: { value: 'sk-test' } })
    fireEvent.change(within(dialog).getByLabelText('Temperature'), { target: { value: '0.3' } })
    fireEvent.change(within(dialog).getByLabelText('Max Tokens'), { target: { value: '4096' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(aiApi.adminModelsSave).toHaveBeenCalledWith(expect.objectContaining({
        modelName: 'DeepSeek V3',
        provider: 'deepseek',
        endpoint: 'deepseek-chat',
        apiBaseUrl: 'https://gateway.example/v1',
        apiKey: 'sk-test',
        temperature: 0.3,
        maxTokens: 4096,
        status: 1,
      }))
    })
  })

  it('keeps model form input and shows endpoint when save fails', async () => {
    vi.mocked(aiApi.adminModelsSave).mockRejectedValueOnce(new Error('invalid base url') as never)

    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    await screen.findByText('模型可用性诊断')
    fireEvent.click(screen.getByRole('button', { name: '新增模型' }))

    const dialog = await screen.findByRole('dialog', { name: '新增模型' })
    fireEvent.change(within(dialog).getByLabelText('模型名称（展示用）'), { target: { value: 'Bad Gateway Model' } })
    fireEvent.mouseDown(within(dialog).getByLabelText('提供商'))
    fireEvent.click(await screen.findByRole('option', { name: '自定义' }))
    fireEvent.change(within(dialog).getByLabelText('模型 ID（厂商 API 中的 model 名称）'), { target: { value: 'custom-model' } })
    fireEvent.change(within(dialog).getByLabelText('自定义 API Base URL（可选）'), { target: { value: 'not-a-url' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/模型保存失败（\/ai\/admin\/models\/save）：invalid base url/)).toBeInTheDocument()
    expect(screen.getByTestId('models-config-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('models-config-form-error')).toHaveAttribute('data-secret-write-only', 'true')
    expect(screen.getByDisplayValue('Bad Gateway Model')).toBeInTheDocument()
    expect(screen.getByDisplayValue('custom-model')).toBeInTheDocument()
    expect(screen.getByDisplayValue('not-a-url')).toBeInTheDocument()
  })

  it('keeps model row and shows endpoint when default, delete or connection actions fail', async () => {
    vi.mocked(aiApi.adminModelsList).mockResolvedValueOnce([
      {
        id: 1,
        modelName: 'Ollama Qwen',
        modelProvider: 'ollama',
        modelVersion: 'qwen2.5:7b',
        apiKeyMasked: '',
        resolvedBaseUrl: 'http://host.docker.internal:11434',
        status: 1,
        isDefault: 0,
      },
    ] as never)
    vi.mocked(aiApi.adminModelsSetDefault).mockRejectedValueOnce(new Error('set default denied') as never)
    vi.mocked(aiApi.adminModelsTestConnection).mockRejectedValueOnce(new Error('ConnectException') as never)
    vi.mocked(aiApi.adminModelsDelete).mockRejectedValueOnce(new Error('delete denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    await screen.findAllByText('Ollama Qwen')
    fireEvent.click(screen.getByLabelText('设为默认：Ollama Qwen'))
    expect(await screen.findByText(/默认模型设置失败（\/ai\/admin\/models\/set-default，Ollama Qwen）：set default denied/)).toBeInTheDocument()
    expect(screen.getByTestId('models-config-default-error')).toHaveAttribute('data-no-optimistic-default-mutation', 'true')

    fireEvent.click(screen.getByLabelText('测试连接：Ollama Qwen'))
    expect(await screen.findByText(/连通性测试请求失败（\/ai\/admin\/models\/test-connection，Ollama Qwen）：ConnectException/)).toBeInTheDocument()
    expect(screen.getByTestId('models-config-test-error')).toHaveAttribute('data-no-local-connection-success', 'true')

    fireEvent.click(screen.getByLabelText('删除：Ollama Qwen'))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))
    expect((await screen.findAllByText(/模型删除失败（\/ai\/admin\/models\/delete，id=1）：delete denied/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('models-config-delete-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getAllByText('Ollama Qwen').length).toBeGreaterThan(0)
  })

  it('shows list and operation errors by source', async () => {
    vi.mocked(aiApi.adminModelsList).mockRejectedValue(new Error('models unavailable'))

    renderWithProviders(
      <MemoryRouter>
        <ModelsConfigPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/模型列表加载失败：models unavailable/, undefined, { timeout: 6000 })).toBeInTheDocument()
    expect(screen.getByTestId('models-config-list-error')).toHaveAttribute('data-no-local-model-fallback', 'true')
    expect(screen.getByText(/\/ai\/admin\/models\/list/)).toBeInTheDocument()
  })
})
