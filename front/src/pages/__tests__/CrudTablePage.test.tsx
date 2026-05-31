import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { CrudTablePage } from '../CrudTablePage'
import { DataTablePage } from '../DataTablePage'

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('CrudTablePage', () => {
  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: '名称' },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('keeps delete failures visible in the confirm dialog', async () => {
    const onDelete = vi.fn().mockRejectedValue(new Error('delete down'))

    renderWithProviders(
      <MemoryRouter>
        <CrudTablePage
          title="资源"
          fetchData={() => Promise.resolve({ total: 1, list: [{ id: 3, name: '测试资源' }] })}
          columns={columns}
          showAdd={false}
          showEdit={false}
          onDelete={onDelete}
        />
      </MemoryRouter>,
    )

    expect(await screen.findByText('测试资源')).toBeInTheDocument()
    const root = screen.getByTestId('crud-table-page-shell')
    expect(root).toHaveAttribute('data-contract-scope', 'shared-crud-table-props-wrapper')
    expect(root).toHaveAttribute('data-contract-source', 'fetchData|onSave|onDelete-props')
    expect(root).toHaveAttribute('data-has-delete-api', 'true')
    expect(root).toHaveAttribute('data-no-direct-api', 'true')
    fireEvent.click(screen.getByLabelText('删除'))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByTestId('crud-delete-dialog')).toHaveAttribute('data-contract-source', 'onDelete-prop')
    expect(screen.getByTestId('crud-delete-dialog')).toHaveAttribute('data-target-id', '3')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))

    expect(await screen.findByText(/删除失败：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('crud-delete-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('crud-delete-error')).toHaveAttribute('data-no-local-delete-fallback', 'true')
    expect(toast).toHaveBeenCalledWith('delete down', 'error')
  })

  it('exposes shared datatable contract and delegates actions to props', async () => {
    const onAdd = vi.fn()
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    const onBatchDelete = vi.fn().mockResolvedValue(undefined)

    renderWithProviders(
      <MemoryRouter>
        <DataTablePage
          title="共享资源"
          fetchData={() => Promise.resolve({ total: 2, list: [{ id: 1, name: '资源 A' }, { id: 2, name: '资源 B' }] })}
          columns={columns}
          onAdd={onAdd}
          onEdit={onEdit}
          onDelete={onDelete}
          onBatchDelete={onBatchDelete}
        />
      </MemoryRouter>,
    )

    expect(await screen.findByText('资源 A')).toBeInTheDocument()
    const root = screen.getByTestId('datatable-page-shell')
    expect(root).toHaveAttribute('data-contract-scope', 'shared-data-table-props-grid')
    expect(root).toHaveAttribute('data-contract-source', 'fetchData-prop')
    expect(root).toHaveAttribute('data-no-direct-api', 'true')
    expect(root).toHaveAttribute('data-no-local-row-fallback', 'true')
    expect(root).toHaveAttribute('data-no-server-export', 'true')
    expect(screen.getByTestId('datatable-table-surface')).toHaveAttribute('data-row-count', '2')
    expect(screen.getAllByTestId('datatable-body-row')[0]).toHaveAttribute('data-contract-source', 'fetchData-prop')

    fireEvent.click(screen.getByTestId('datatable-add-button'))
    fireEvent.click(screen.getAllByTestId('datatable-row-edit-button')[0])
    fireEvent.click(screen.getAllByTestId('datatable-row-delete-button')[0])

    expect(onAdd).toHaveBeenCalled()
    expect(onEdit).toHaveBeenCalledWith(expect.objectContaining({ id: 1, name: '资源 A' }))
    expect(onDelete).toHaveBeenCalledWith(expect.objectContaining({ id: 1, name: '资源 A' }))
  })

  it('keeps fetch failures and malformed list responses explicit without local rows', async () => {
    const failingFetch = vi.fn().mockRejectedValue(new Error('list down'))

    const { unmount } = renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="失败资源" fetchData={failingFetch} columns={columns} />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('datatable-load-error')).toHaveTextContent('list down')
    expect(screen.getByTestId('datatable-load-error')).toHaveAttribute('data-contract-source', 'fetchData-prop')
    expect(screen.getByTestId('datatable-load-error')).toHaveAttribute('data-no-local-row-fallback', 'true')
    expect(screen.queryByText('资源 A')).not.toBeInTheDocument()
    unmount()

    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="异常结构" fetchData={() => Promise.resolve({ total: 1, rows: [{ id: 1, name: '旧字段' }] } as never)} columns={columns} />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('datatable-data-issue')).toHaveTextContent('列表接口未返回 list 数组')
    expect(screen.getByTestId('datatable-empty-state')).toHaveAttribute('data-no-local-row-fallback', 'true')
    expect(screen.queryByText('旧字段')).not.toBeInTheDocument()
  })

  it('keeps batch delete failures visible and retains selected ids', async () => {
    const onBatchDelete = vi.fn().mockRejectedValue(new Error('batch down'))

    renderWithProviders(
      <MemoryRouter>
        <DataTablePage
          title="批量资源"
          fetchData={() => Promise.resolve({ total: 2, list: [{ id: 1, name: '资源 A' }, { id: 2, name: '资源 B' }] })}
          columns={columns}
          onBatchDelete={onBatchDelete}
        />
      </MemoryRouter>,
    )

    expect(await screen.findByText('资源 A')).toBeInTheDocument()
    const checkboxes = screen.getAllByRole('checkbox')
    fireEvent.click(checkboxes[1])
    expect(screen.getByTestId('datatable-page-shell')).toHaveAttribute('data-selected-count', '1')
    fireEvent.click(screen.getByTestId('datatable-batch-delete-open-button'))
    expect(screen.getByTestId('datatable-batch-delete-dialog')).toHaveAttribute('data-selected-count', '1')
    fireEvent.click(screen.getByTestId('datatable-batch-delete-confirm-button'))

    await waitFor(() => {
      expect(onBatchDelete).toHaveBeenCalledWith([1])
    })
    expect(await screen.findByTestId('datatable-batch-delete-error')).toHaveTextContent('batch down')
    expect(screen.getByTestId('datatable-batch-delete-error')).toHaveAttribute('data-selected-retained', 'true')
    expect(screen.getByTestId('datatable-batch-delete-dialog')).toHaveAttribute('data-input-retained', 'true')
  })
})
