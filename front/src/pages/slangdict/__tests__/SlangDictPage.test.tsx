import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import SlangDictPage from '../SlangDictPage'
import { slangApi } from '@/api/slangdict'

vi.mock('@/api/slangdict', () => ({
  slangApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    enable: vi.fn(),
    disable: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, searchSlot, showExport }: any) => (
      <div data-testid="slang-dict-grid" data-show-export={String(showExport)}>
        <div>{searchSlot}</div>
        {rows.length === 0 ? <div>暂无数据</div> : null}
        {rows.map((row: any) => (
          <div key={row.id} data-testid={`slang-row-${row.id}`}>
            {columns.map((col: any) => (
              <div key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueFormatter ? col.valueFormatter(row[col.field]) : row[col.field] ?? '')}
              </div>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

describe('SlangDictPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(slangApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          phrase: '破圈',
          meaning: '突破原有受众圈层',
          example: '这条视频有机会破圈',
          category: '内容',
          usageScene: '短视频',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads slang entries and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('行业俚语词典')).toBeInTheDocument()
    expect(screen.getByText(/\/slangdict\/entry\/search/)).toBeInTheDocument()

    await waitFor(() => {
      expect(slangApi.list).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('破圈')).toBeInTheDocument()
      expect(screen.getByText('突破原有受众圈层')).toBeInTheDocument()
    })
  })

  it('marks main page contract boundaries and hides unsupported export', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    const workbench = await screen.findByTestId('slang-dict-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'content-slangdict-main')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/slangdict/entry/search|/slangdict/entry/save|/slangdict/entry/delete')
    expect(workbench).toHaveAttribute(
      'data-related-context-endpoints',
      '/slangdict/entry/by-product|/slangdict/entry/bind-product|/slangdict/entry/unbind-product|/slangdict/entry/ai-generate',
    )
    expect(workbench).toHaveAttribute(
      'data-unsupported-endpoints',
      '/slangdict/entry/enable|/slangdict/entry/disable|/slangdict/entry/export|/slangdict/entry/batch-import',
    )
    expect(workbench).toHaveAttribute(
      'data-unsupported-actions',
      'independent-enable-disable|server-export|batch-import|inline-product-binding|main-page-ai-generate',
    )
    expect(workbench).toHaveAttribute('data-no-local-slang-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-local-status-mutation', 'true')
    expect(workbench).toHaveAttribute('data-api-normalize', 'slangApi.list:normalizePage(slangEntries|entries)')

    const alert = screen.getByTestId('slang-dict-contract-alert')
    expect(alert).toHaveAttribute('data-enable-disable-strategy', 'save-status')
    expect(alert).toHaveAttribute('data-no-inline-product-binding-request', 'true')
    expect(alert).toHaveAttribute('data-no-ai-generate-request', 'true')
    expect(alert).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getByTestId('slang-dict-grid')).toHaveAttribute('data-show-export', 'false')
    expect(screen.getByTestId('slang-dict-kpi-enabled')).toHaveAttribute('data-contract-source', 'local-derived status=1')
    expect(screen.getByTestId('slang-dict-kpi-total')).toHaveAttribute('data-contract-source', '/slangdict/entry/search total')

    expect(slangApi.save).not.toHaveBeenCalled()
  })

  it('searches only after query action', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(slangApi.list).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    fireEvent.change(screen.getByPlaceholderText('搜索术语...'), { target: { value: '破圈' } })
    expect(slangApi.list).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => {
      expect(slangApi.list).toHaveBeenLastCalledWith({ page: 0, rows: 20, keyword: '破圈' })
    })
  })

  it('keeps save failure visible with endpoint context and preserves form input', async () => {
    vi.mocked(slangApi.save).mockRejectedValueOnce(new Error('slang save down') as never)

    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    await screen.findByText('破圈')
    fireEvent.click(screen.getByRole('button', { name: '新增词条' }))
    const dialog = screen.getByRole('dialog', { name: '新增词条' })
    fireEvent.change(within(dialog).getByLabelText('梗/暗语'), { target: { value: '种草' } })
    fireEvent.change(within(dialog).getByLabelText('释义'), { target: { value: '内容激发购买兴趣' } })
    fireEvent.change(within(dialog).getByLabelText('分类'), { target: { value: '转化' } })
    fireEvent.change(within(dialog).getByLabelText('使用场景'), { target: { value: '直播' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(slangApi.save).toHaveBeenCalledWith(expect.objectContaining({
        phrase: '种草',
        meaning: '内容激发购买兴趣',
        category: '转化',
        usageScene: '直播',
        status: 1,
      }))
    })
    expect((await screen.findAllByText(/\/slangdict\/entry\/save 词条保存失败/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('slang-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('slang-form-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getAllByText(/route=\/admin\/slangdict/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/entryId=新增; phrase=种草; category=转化; usageScene=直播/).length).toBeGreaterThan(0)
    expect(within(dialog).getByLabelText('梗/暗语')).toHaveValue('种草')
    expect(within(dialog).getByLabelText('释义')).toHaveValue('内容激发购买兴趣')
  })

  it('keeps delete failure visible with target entry context and preserves row', async () => {
    vi.mocked(slangApi.delete).mockRejectedValueOnce(new Error('slang delete down') as never)

    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    const row = await screen.findByTestId('slang-row-1')
    fireEvent.click(within(row).getByRole('button', { name: '删除' }))
    expect(screen.getByText(/endpoint=\/slangdict\/entry\/delete\?id=1/)).toBeInTheDocument()
    expect(screen.getByText(/entryId=1; phrase=破圈; category=内容; usageScene=短视频/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/\/slangdict\/entry\/delete\?id=1 词条删除失败/)).toBeInTheDocument()
    expect(screen.getByTestId('slang-delete-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getAllByText(/entryId=1; phrase=破圈; category=内容; usageScene=短视频/).length).toBeGreaterThan(0)
    expect(screen.getByText('破圈')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '确认操作' })).toBeInTheDocument()
  })

  it('keeps toggle failure visible and does not fake local status change', async () => {
    vi.mocked(slangApi.save).mockRejectedValueOnce(new Error('slang toggle down') as never)

    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    const row = await screen.findByTestId('slang-row-1')
    const checkbox = within(row).getByRole('checkbox')
    expect(checkbox).toBeChecked()
    fireEvent.click(checkbox)

    await waitFor(() => {
      expect(slangApi.save).toHaveBeenCalledWith(expect.objectContaining({
        id: 1,
        phrase: '破圈',
        status: 0,
      }))
    })
    expect(await screen.findByText(/\/slangdict\/entry\/save 词条启停失败/)).toBeInTheDocument()
    expect(screen.getByTestId('slang-toggle-error')).toHaveAttribute('data-no-local-status-mutation', 'true')
    expect(screen.getByText(/targetStatus=0\/停用/)).toBeInTheDocument()
    expect(screen.getByText(/启停通过保存 status 降级实现/)).toBeInTheDocument()
    expect(checkbox).toBeChecked()
  })

  it('keeps list failure tied to search endpoint and filter context', async () => {
    vi.mocked(slangApi.list).mockRejectedValueOnce(new Error('slang list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/\/slangdict\/entry\/search 加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/slangdict; keyword=空; page=0; rows=20/)).toBeInTheDocument()
  })
})
