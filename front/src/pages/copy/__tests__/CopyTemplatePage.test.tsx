import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import CopyTemplatePage from '../CopyTemplatePage'
import { copyApi } from '@/api/copy'

vi.mock('@/api/copy', () => ({
  copyApi: {
    templateList: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('CopyTemplatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(copyApi.templateList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          templateName: '开场模板',
          content: '大家好，欢迎来到直播间',
          category: '开场白',
          variables: 'productName',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('renders title and loads copy templates', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    expect(screen.getByText('文案模板')).toBeInTheDocument()

    await waitFor(() => {
      expect(copyApi.templateList).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('开场模板')).toBeInTheDocument()
      expect(screen.getByText('开场白')).toBeInTheDocument()
    })

    const root = screen.getByTestId('copy-template-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'copy-template-standalone')
    expect(root).toHaveAttribute('data-ready-endpoints', '/copy/template/search,/copy/template/save,/copy/template/delete')
    expect(root).toHaveAttribute('data-unsupported-actions', 'server-side-batch-delete,server-export')
    expect(root).toHaveAttribute('data-unsupported-endpoints', '/copy/template/batch-delete,/copy/template/export')
    expect(root).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('copy-template-contract-alert')).toHaveAttribute('data-no-batch-delete-request', 'true')
    expect(screen.queryByRole('button', { name: /导出/ })).not.toBeInTheDocument()
  })

  it('shows empty state when no templates exist', async () => {
    vi.mocked(copyApi.templateList).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    })

    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('暂无文案模板')).toBeInTheDocument()
    expect(screen.getByText(/新增模板后可用/)).toBeInTheDocument()
  })

  it('shows retryable error when template list fails', async () => {
    vi.mocked(copyApi.templateList).mockRejectedValueOnce(new Error('template backend down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/\/copy\/template\/search 文案模板加载失败：template backend down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-template-list-error')).toHaveAttribute('data-no-local-templates', 'true')
  })

  it('shows source endpoint when template save or delete fails', async () => {
    vi.mocked(copyApi.templateSave).mockRejectedValue(new Error('save down'))
    vi.mocked(copyApi.templateDelete).mockRejectedValue(new Error('delete down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('开场模板')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '新增模板' }))
    const dialog = screen.getByRole('dialog', { name: '新增模板' })
    expect(screen.getByTestId('copy-template-form-contract')).toHaveAttribute('data-ready-endpoint', '/copy/template/save')
    fireEvent.change(within(dialog).getByRole('textbox', { name: /模板名称/ }), { target: { value: '失败模板' } })
    fireEvent.change(within(dialog).getByRole('textbox', { name: /模板内容/ }), { target: { value: '你好 {productName}' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))
    expect(await within(dialog).findByText(/\/copy\/template\/save 保存失败：save down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '新增模板' })).not.toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/\/copy\/template\/delete 删除失败：delete down/)).toBeInTheDocument()
    expect(screen.getByText('开场模板')).toBeInTheDocument()
  })

  it('previews template from server list source', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByText('开场模板'))

    const drawer = await screen.findByTestId('copy-template-preview-drawer')
    expect(drawer).toHaveAttribute('data-template-id', '1')
    expect(drawer).toHaveAttribute('data-contract-status', 'server-source-preview')
    expect(drawer).toHaveAttribute('data-source-endpoint', '/copy/template/search')
    expect(within(drawer).getByText('{productName}')).toBeInTheDocument()
  })

  it('uses local sequential delete instead of unsupported batch delete endpoint', async () => {
    vi.mocked(copyApi.templateDelete).mockResolvedValue(undefined as never)
    vi.mocked(copyApi.templateList).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 1,
          templateName: '开场模板',
          content: '大家好，欢迎来到直播间',
          category: '开场白',
          variables: 'productName',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
        {
          id: 2,
          templateName: '促单模板',
          content: '今天 {productName} 限时福利',
          category: '促销话术',
          variables: 'productName',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })

    renderWithProviders(
      <MemoryRouter>
        <CopyTemplatePage />
      </MemoryRouter>,
    )

    await screen.findByText('促单模板')
    const rowCheckboxes = screen.getAllByRole('checkbox').slice(1, 3)
    fireEvent.click(rowCheckboxes[0])
    fireEvent.click(rowCheckboxes[1])

    const sequentialButton = await screen.findByTestId('copy-template-sequential-delete-button')
    expect(sequentialButton).toHaveAttribute('data-contract-action', 'local-sequential-delete')
    expect(sequentialButton).toHaveAttribute('data-unsupported-endpoint', '/copy/template/batch-delete')
    fireEvent.click(sequentialButton)
    expect(screen.getByText(/逐条调用 \/copy\/template\/delete/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(copyApi.templateDelete).toHaveBeenCalledWith(1)
      expect(copyApi.templateDelete).toHaveBeenCalledWith(2)
    })
  })
})
