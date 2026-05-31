import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ExternalApiConfigPage from '../ExternalApiConfigPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    externalApiList: vi.fn(),
    externalApiSave: vi.fn(),
    externalApiDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, searchSlot, actionSlot }: any) => (
      <div>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

describe('ExternalApiConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.externalApiList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          providerCode: 'openai',
          providerName: 'OpenAI',
          category: 'llm',
          baseUrl: 'https://api.openai.com',
          apiKeyEncrypted: '***',
          apiSecretEncrypted: '***',
          isEnabled: true,
          healthStatus: 'healthy',
          priority: 5,
          rateLimitPerMin: 60,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads external api configs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(systemApi.externalApiList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        category: undefined,
      })
    })

    await waitFor(() => {
      expect(screen.getByText('OpenAI')).toBeInTheDocument()
      expect(screen.getByText('https://api.openai.com')).toBeInTheDocument()
      expect(screen.getByText('LLM')).toBeInTheDocument()
    })
    expect(screen.getByTestId('external-api-config-page-workbench')).toHaveAttribute('data-contract-scope', 'system-external-api-config-management')
    expect(screen.getByTestId('external-api-config-page-workbench')).toHaveAttribute('data-no-local-api-config-fallback', 'true')
    expect(screen.getByTestId('external-api-config-page-workbench')).toHaveAttribute('data-no-plaintext-secret-display', 'true')
    expect(screen.getByTestId('external-api-config-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/external-api/get-secret')
    expect(screen.getByTestId('external-api-config-page-workbench').getAttribute('data-unsupported-endpoints')).toContain('/system/external-api/probe')
  })

  it('toggles config with save payload only and keeps secret fields out', async () => {
    vi.mocked(systemApi.externalApiSave).mockResolvedValue({} as never)

    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    const toggle = await screen.findByLabelText('切换 OpenAI 启用状态')
    fireEvent.click(toggle)

    await waitFor(() => {
      expect(systemApi.externalApiSave).toHaveBeenCalledWith({
        id: 1,
        providerCode: 'openai',
        providerName: 'OpenAI',
        category: 'llm',
        baseUrl: 'https://api.openai.com',
        isEnabled: false,
        priority: 5,
        rateLimitPerMin: 60,
        dailyQuota: undefined,
        monthlyQuota: undefined,
        extraConfig: undefined,
      })
    })
    expect(vi.mocked(systemApi.externalApiSave).mock.calls[0][0]).not.toHaveProperty('apiKeyEncrypted')
    expect(vi.mocked(systemApi.externalApiSave).mock.calls[0][0]).not.toHaveProperty('apiSecretEncrypted')
    expect(vi.mocked(systemApi.externalApiSave).mock.calls[0][0]).not.toHaveProperty('healthStatus')
    expect(screen.getByTestId('external-api-config-toggle-contract')).toHaveAttribute('data-no-optimistic-enable-toggle', 'true')
  })

  it('shows encrypted secret storage in detail drawer', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: 'OpenAI' }))

    expect(await screen.findByText('已加密保存，明文不可回显')).toBeInTheDocument()
    expect(screen.getByTestId('external-api-config-detail-drawer')).toHaveAttribute('data-no-plaintext-secret-display', 'true')
    expect(screen.getByTestId('external-api-config-detail-drawer')).toHaveAttribute('data-no-direct-secret-read', 'true')
    expect(screen.getByTestId('external-api-config-detail-drawer').getAttribute('data-unsupported-endpoints')).toContain('/system/external-api/get-secret')
  })

  it('marks API key and secret inputs as write-only and keeps category filtering server-side', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    const search = await screen.findByTestId('external-api-config-search-contract')
    fireEvent.mouseDown(within(search).getByLabelText('配置分类筛选'))
    fireEvent.click(await screen.findByRole('option', { name: 'LLM' }))

    await waitFor(() => {
      expect(systemApi.externalApiList).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        category: 'llm',
      })
    })
    expect(search).toHaveAttribute('data-no-local-category-filter', 'true')

    fireEvent.click(screen.getByRole('button', { name: '新增配置' }))
    expect(await screen.findByTestId('external-api-config-secret-write-contract')).toHaveAttribute('data-secret-write-only', 'true')
    expect(screen.getByTestId('external-api-config-secret-write-contract')).toHaveAttribute('data-no-direct-secret-read', 'true')
  })

  it('keeps form input and avoids local fallback when save fails', async () => {
    vi.mocked(systemApi.externalApiSave).mockRejectedValue(new Error('save down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增配置' }))
    const dialog = await screen.findByRole('dialog', { name: '新增外部API配置' })
    fireEvent.change(within(dialog).getByLabelText(/供应商编码/), { target: { value: 'deepseek' } })
    fireEvent.change(within(dialog).getByLabelText(/供应商名称/), { target: { value: 'DeepSeek' } })
    fireEvent.change(within(dialog).getByLabelText(/Base URL/), { target: { value: 'https://api.deepseek.com' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await screen.findByTestId('external-api-config-operation-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('external-api-config-form-contract')).toHaveAttribute('data-provider-code', 'deepseek')
    expect(vi.mocked(systemApi.externalApiSave).mock.calls[0][0]).toEqual(expect.objectContaining({
      providerCode: 'deepseek',
      providerName: 'DeepSeek',
      baseUrl: 'https://api.deepseek.com',
    }))
  })

  it('keeps row context when delete or toggle fails', async () => {
    vi.mocked(systemApi.externalApiDelete).mockRejectedValue(new Error('delete down') as never)
    vi.mocked(systemApi.externalApiSave).mockRejectedValue(new Error('toggle down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ExternalApiConfigPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '删除' }))
    expect(screen.getByTestId('external-api-config-delete-contract')).toHaveAttribute('data-target-id', '1')
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByTestId('external-api-config-operation-error')).toHaveAttribute('data-row-retained', 'true')

    fireEvent.click(await screen.findByLabelText('切换 OpenAI 启用状态'))
    expect(await screen.findByText(/启停失败/)).toBeInTheDocument()
    expect(screen.getByTestId('external-api-config-operation-error')).toHaveAttribute('data-row-retained', 'true')
  })
})
