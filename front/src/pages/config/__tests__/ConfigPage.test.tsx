import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ConfigPage from '../ConfigPage'
import { configApi } from '@/api/config'

vi.mock('@/api/config', () => ({
  configApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(configApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          configKey: 'feature.enabled',
          configValue: 'true',
          configName: '功能开关',
          configType: 'feature',
          valueType: 'boolean',
          isSensitive: 0,
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads config list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('系统配置')).toBeInTheDocument()
    const root = screen.getByTestId('config-page-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'system-config-keyvalue-management')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/config/list')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/config/save')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/config/delete')
    expect(root).toHaveAttribute('data-no-local-config-fallback', 'true')
    expect(root).toHaveAttribute('data-no-plaintext-sensitive-value', 'true')
    expect(screen.getByTestId('config-search-contract')).toHaveAttribute('data-contract-source', '/config/list')
    expect(screen.getByTestId('config-action-contract')).toHaveAttribute('data-contract-source', '/config/save')

    await waitFor(() => {
      expect(configApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, configKey: '' })
    })

    await waitFor(() => {
      expect(screen.getByText('feature.enabled')).toBeInTheDocument()
      expect(screen.getByText('功能开关')).toBeInTheDocument()
      expect(screen.getByText('feature')).toBeInTheDocument()
      expect(screen.getByText('boolean')).toBeInTheDocument()
    })
  })

  it('tolerates wrapped config list mocks', async () => {
    vi.mocked(configApi.list).mockResolvedValue({
      records: [
        {
          id: 2,
          configKey: 'wrapped.secret',
          configValue: 'hidden',
          configName: '包装配置',
          configType: 'security',
          valueType: 'string',
          isSensitive: 1,
        },
      ],
      totalElements: 4,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('wrapped.secret')).toBeInTheDocument()
    expect(screen.getByText('包装配置')).toBeInTheDocument()
    expect(screen.getByText('已脱敏')).toBeInTheDocument()
    expect(screen.getByTestId('config-page-workbench')).toHaveAttribute('data-sensitive-count', '1')
    expect(screen.getByText('******')).toBeInTheDocument()
    expect(screen.queryByText('hidden')).not.toBeInTheDocument()
  })

  it('keeps config form open and shows endpoint when save fails', async () => {
    vi.mocked(configApi.save).mockRejectedValue(new Error('save down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: '新增配置' }))
    const dialog = screen.getByRole('dialog', { name: '新增配置' })
    expect(screen.getByTestId('config-form-contract')).toHaveAttribute('data-contract-source', '/config/save')
    expect(screen.getByTestId('config-form-contract')).toHaveAttribute('data-mode', 'create')
    const inputs = within(dialog).getAllByRole('textbox')
    fireEvent.change(inputs[0], { target: { value: 'feature.fail' } })
    fireEvent.change(inputs[3], { target: { value: 'true' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await screen.findAllByText('/config/save 保存失败：save down')).toHaveLength(2)
    expect(screen.getByTestId('config-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('config-form-save-error')).toHaveAttribute('data-contract-source', '/config/save')
    expect(inputs[0]).toHaveValue('feature.fail')
  })

  it('does not prefill sensitive values when editing and blocks blank overwrite', async () => {
    vi.mocked(configApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 2,
          configKey: 'api.secret',
          configValue: 'plain-secret-from-backend',
          configName: '敏感配置',
          configType: 'security',
          valueType: 'string',
          isSensitive: 1,
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    await screen.findByText('api.secret')
    expect(screen.queryByText('plain-secret-from-backend')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '编辑' }))

    const dialog = screen.getByRole('dialog', { name: '编辑配置' })
    const formContract = screen.getByTestId('config-form-contract')
    expect(formContract).toHaveAttribute('data-sensitive-value-redacted-on-edit', 'true')
    expect(formContract).toHaveAttribute('data-sensitive-value-prefilled', 'false')
    const inputs = within(dialog).getAllByRole('textbox')
    expect(inputs[3]).toHaveValue('')

    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))
    expect(await screen.findByTestId('config-form-validation-error')).toHaveAttribute('data-no-plaintext-sensitive-value', 'true')
    expect(configApi.save).not.toHaveBeenCalled()
    expect(inputs[3]).toHaveValue('')
  })

  it('keeps delete confirm context and row when delete fails', async () => {
    vi.mocked(configApi.delete).mockRejectedValue(new Error('delete down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    await screen.findByText('feature.enabled')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(screen.getByTestId('config-delete-contract')).toHaveAttribute('data-contract-source', '/config/delete')
    expect(screen.getByTestId('config-delete-contract')).toHaveAttribute('data-target-id', '1')
    expect(screen.getByTestId('config-delete-contract')).toHaveAttribute('data-target-key', 'feature.enabled')
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findAllByText(/\/config\/delete 删除失败：delete down/)).toHaveLength(2)
    expect(screen.getByTestId('config-delete-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByTestId('config-delete-contract')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getAllByText(/feature.enabled/).length).toBeGreaterThan(0)
  })

  it('keeps list errors explicit without local config fallback', async () => {
    vi.mocked(configApi.list).mockRejectedValue(new Error('config list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ConfigPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('config-list-error')).toHaveAttribute('data-contract-source', '/config/list')
    expect(screen.getByTestId('config-list-error')).toHaveAttribute('data-no-local-config-fallback', 'true')
    expect(screen.queryByText('feature.enabled')).not.toBeInTheDocument()
  })
})
