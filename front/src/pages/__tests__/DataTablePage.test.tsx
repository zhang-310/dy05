import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { fireEvent, renderWithProviders } from '@/test/utils'
import { MemoryRouter } from 'react-router-dom'
import { DataTablePage } from '../DataTablePage'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

describe('DataTablePage', () => {
  const mockFetchData = vi.fn().mockResolvedValue({ total: 2, list: [
    { id: 1, name: '测试1', status: 'active' },
    { id: 2, name: '测试2', status: 'inactive' },
  ]})

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: '名称' },
    { key: 'status', label: '状态', asChip: true },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('renders with title', () => {
    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试表格" fetchData={mockFetchData} columns={columns} />
      </MemoryRouter>
    )
    expect(screen.getByText('测试表格')).toBeInTheDocument()
  })

  it('calls fetchData on mount', async () => {
    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试" fetchData={mockFetchData} columns={columns} />
      </MemoryRouter>
    )
    await waitFor(() => {
      expect(mockFetchData).toHaveBeenCalled()
    })
  })

  it('renders column headers', async () => {
    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试" fetchData={mockFetchData} columns={columns} />
      </MemoryRouter>
    )
    await waitFor(() => {
      expect(screen.getByText('ID')).toBeInTheDocument()
      expect(screen.getByText('名称')).toBeInTheDocument()
      expect(screen.getByText('状态')).toBeInTheDocument()
    })
  })

  it('renders data rows after loading', async () => {
    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试" fetchData={mockFetchData} columns={columns} />
      </MemoryRouter>
    )
    await waitFor(() => {
      expect(screen.getByText('测试1')).toBeInTheDocument()
      expect(screen.getByText('测试2')).toBeInTheDocument()
    })
  })

  it('shows search input', () => {
    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试" fetchData={mockFetchData} columns={columns} searchPlaceholder="搜索记录" />
      </MemoryRouter>
    )
    expect(screen.getByPlaceholderText('搜索记录')).toBeInTheDocument()
  })

  it('normalizes non-standard list payloads and shows a visible warning', async () => {
    const fetchData = vi.fn().mockResolvedValue({ total: 2, rows: [{ id: 1, name: '不会渲染' }] })

    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试" fetchData={fetchData} columns={columns} />
      </MemoryRouter>
    )

    expect(await screen.findByText(/列表接口未返回 list 数组/)).toBeInTheDocument()
    expect(screen.getByText('暂无数据')).toBeInTheDocument()
  })

  it('uses an in-page dialog for batch delete instead of window confirm', async () => {
    const onBatchDelete = vi.fn().mockResolvedValue(undefined)

    renderWithProviders(
      <MemoryRouter>
        <DataTablePage title="测试" fetchData={mockFetchData} columns={columns} onBatchDelete={onBatchDelete} />
      </MemoryRouter>
    )

    await screen.findByText('测试1')
    fireEvent.click(screen.getAllByRole('checkbox')[1])
    fireEvent.click(screen.getByRole('button', { name: /批量删除/ }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/确定删除选中的 1 条记录/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '确认删除' }))
    await waitFor(() => expect(onBatchDelete).toHaveBeenCalledWith([1]))
  })

  it('uses theme surfaces for sticky header cells in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <DataTablePage title="测试" fetchData={mockFetchData} columns={columns} onEdit={vi.fn()} />
        </MemoryRouter>
      </AppThemeProvider>
    )

    await screen.findByText('测试1')
    expect(screen.getAllByTestId('datatable-header-cell')[0]).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })
})
