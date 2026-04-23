import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { MemoryRouter } from 'react-router-dom'
import { DataTablePage } from '../DataTablePage'

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
})
