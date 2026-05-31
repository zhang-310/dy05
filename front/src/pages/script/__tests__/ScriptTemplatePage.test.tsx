import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ScriptTemplatePage from '../ScriptTemplatePage'
import { scriptApi } from '@/api/script'

vi.mock('@/api/script', () => ({
  scriptApi: {
    templateSearch: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
    templateGet: vi.fn(),
    templateByScene: vi.fn(),
    updateUseCount: vi.fn(),
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
      <div data-testid="script-template-grid" data-show-export={String(showExport)}>
        <div>{searchSlot}</div>
        {rows.length === 0 ? <div>暂无数据</div> : null}
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueGetter ? col.valueGetter(row[col.field], row) : col.valueFormatter ? col.valueFormatter(row[col.field]) : row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

describe('ScriptTemplatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.templateSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          templateName: '直播开场模板',
          templateType: 'user',
          content: '欢迎来到直播间',
          description: '直播开场变量模板',
          scene: 'opening',
          useCount: 9,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    })
    vi.mocked(scriptApi.templateSave).mockResolvedValue(2 as never)
    vi.mocked(scriptApi.templateDelete).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.templateGet).mockResolvedValue({} as never)
    vi.mocked(scriptApi.templateByScene).mockResolvedValue([] as never)
    vi.mocked(scriptApi.updateUseCount).mockResolvedValue(undefined as never)
  })

  it('renders title and loads script templates', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ScriptTemplatePage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '话术模板' })).toBeInTheDocument()
    expect(screen.getByText(/content/)).toBeInTheDocument()
    const workbench = screen.getByTestId('script-template-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'script-template-business')
    expect(workbench).toHaveAttribute(
      'data-ready-endpoints',
      '/script/template/search|/script/template/save|/script/template/delete',
    )
    expect(workbench).toHaveAttribute(
      'data-context-endpoints',
      '/script/template/get|/script/template/by-scene|/script/template/use-count',
    )
    expect(workbench).toHaveAttribute(
      'data-admin-endpoints',
      '/script/admin/template/list|/script/admin/template/save|/script/admin/template/delete',
    )
    expect(workbench).toHaveAttribute(
      'data-unsupported-actions',
      'inline-admin-system-template-management|server-export|batch-import|inline-use-count-increment',
    )
    expect(screen.getByTestId('script-template-contract-alert')).toHaveAttribute('data-no-admin-template-request', 'true')
    expect(screen.getByTestId('script-template-contract-alert')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getByTestId('script-template-grid')).toHaveAttribute('data-show-export', 'false')

    await waitFor(() => {
      expect(scriptApi.templateSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('直播开场模板')).toBeInTheDocument()
      expect(screen.getByText('用户模板')).toBeInTheDocument()
      expect(screen.getByText('开场')).toBeInTheDocument()
    })
    expect(screen.getByTestId('script-template-kpi-current-page')).toHaveAttribute('data-contract-source', '/script/template/search list.length')
    expect(screen.getByTestId('script-template-kpi-variables')).toHaveAttribute('data-contract-source', 'local-derived content variables')
    expect(scriptApi.templateGet).not.toHaveBeenCalled()
    expect(scriptApi.templateByScene).not.toHaveBeenCalled()
    expect(scriptApi.updateUseCount).not.toHaveBeenCalled()
  })

  it('saves templates with backend content field and filters by template type', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ScriptTemplatePage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(scriptApi.templateSearch).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    fireEvent.mouseDown(screen.getByLabelText('类型'))
    fireEvent.click(await screen.findByRole('option', { name: '系统模板' }))
    fireEvent.click(screen.getByRole('button', { name: '查询' }))
    await waitFor(() => {
      expect(scriptApi.templateSearch).toHaveBeenLastCalledWith({ page: 0, rows: 20, templateType: 'system' })
    })

    fireEvent.click(screen.getByRole('button', { name: '新增模板' }))
    const dialog = screen.getByRole('dialog', { name: '新增话术模板' })
    fireEvent.change(within(dialog).getByLabelText('模板名称'), { target: { value: '成交收口' } })
    fireEvent.change(within(dialog).getByLabelText('模板内容'), { target: { value: '今天{产品名}只到今晚' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(scriptApi.templateSave).toHaveBeenCalledWith(expect.objectContaining({
        templateName: '成交收口',
        templateType: 'user',
        scene: 'general',
        content: '今天{产品名}只到今晚',
        status: 1,
      }))
    })
  })

  it('keeps template load, save, and delete failures visible with endpoint source', async () => {
    vi.mocked(scriptApi.templateSearch).mockRejectedValueOnce(new Error('template list down') as never)
    vi.mocked(scriptApi.templateSave).mockRejectedValueOnce(new Error('template save down') as never)
    vi.mocked(scriptApi.templateDelete).mockRejectedValueOnce(new Error('template delete down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ScriptTemplatePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/接口来源：\/script\/template\/search/)).toBeInTheDocument()

    vi.mocked(scriptApi.templateSearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          templateName: '直播开场模板',
          templateType: 'user',
          content: '欢迎来到直播间',
          description: '直播开场变量模板',
          scene: 'opening',
          useCount: 9,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(await screen.findByText('直播开场模板')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    fireEvent.click(within(screen.getByRole('dialog', { name: '编辑话术模板' })).getByRole('button', { name: '保存' }))
    expect(await screen.findByText(/接口来源：\/script\/template\/save/)).toBeInTheDocument()
    expect(screen.getAllByText(/route=\/admin\/script\/templates/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/templateId=1; templateName=直播开场模板/).length).toBeGreaterThan(0)
    expect(within(screen.getByRole('dialog', { name: '编辑话术模板' })).getByLabelText('模板名称')).toHaveValue('直播开场模板')
    fireEvent.click(within(screen.getByRole('dialog', { name: '编辑话术模板' })).getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '编辑话术模板' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/接口来源：\/script\/template\/delete/)).toBeInTheDocument()
    expect(screen.getAllByText(/templateId=1; templateName=直播开场模板/).length).toBeGreaterThan(0)
    expect(screen.getByText('直播开场模板')).toBeInTheDocument()
  })
})
