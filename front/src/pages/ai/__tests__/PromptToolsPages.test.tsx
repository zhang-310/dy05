import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import PromptLabPage from '../PromptLabPage'
import PromptTemplatePage from '../PromptTemplatePage'
import PromptToolsPage from '../PromptToolsPage'
import { aiApi, type PromptTemplate } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    promptTemplateList: vi.fn(),
    promptTemplateSave: vi.fn(),
    promptTemplateDelete: vi.fn(),
    promptTemplateExtractVariables: vi.fn(),
    promptTemplateTestRender: vi.fn(),
    promptTemplateRecordUsage: vi.fn(),
    promptTemplateGetActive: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, actionSlot, searchSlot }: any) => (
      <div>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueGetter ? col.valueGetter(row[col.field], row) : row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

const promptTemplate: PromptTemplate = {
  id: 11,
  templateCode: 'agent_chat',
  variantName: 'default',
  templateName: '智能对话模板',
  templateContent: '你好 {{name}}',
  isActive: 1,
  status: 1,
  usageCount: 3,
  variables: 'name',
  createTime: '2026-05-21 10:00:00',
}

function renderPage(ui: React.ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

function renderPageWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('Prompt tools pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(aiApi.promptTemplateList).mockResolvedValue({
      total: 1,
      list: [promptTemplate],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.promptTemplateSave).mockResolvedValue(promptTemplate as never)
    vi.mocked(aiApi.promptTemplateDelete).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.promptTemplateExtractVariables).mockResolvedValue(['name'] as never)
    vi.mocked(aiApi.promptTemplateTestRender).mockResolvedValue({
      rendered: '你好 张三',
      variables: ['name'],
      missingVariables: [],
    } as never)
    vi.mocked(aiApi.promptTemplateRecordUsage).mockResolvedValue(undefined as never)
  })

  it('PromptTemplatePage uses templateCode contract and renders preview', async () => {
    renderPage(<PromptTemplatePage />)

    expect(screen.getByRole('heading', { name: 'Prompt 模板' })).toBeInTheDocument()
    expect(screen.getByTestId('prompt-template-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/prompt-template/list,/ai/prompt-template/save,/ai/prompt-template/delete,/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage',
    )
    expect(screen.getByTestId('prompt-template-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/prompt-template/mock,/ai/prompt-template/local-cache,/ai/prompt-template/static-template,/ai/prompt-template/activate-by-id,/ai/prompt-template/static-render',
    )
    expect(screen.getByTestId('prompt-template-boundary-contract')).toHaveAttribute('data-activation-strategy', 'save-isActive-status')
    expect(screen.getByText(/templateCode\/variantName\/isActive/)).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.promptTemplateList).toHaveBeenCalledWith({ templateCode: undefined, rows: 100 })
    })

    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    expect(screen.getByTestId('prompt-template-grid')).toHaveAttribute('data-source-endpoint', '/ai/prompt-template/list')
    expect(screen.getByTestId('prompt-template-summary-cards')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('prompt-template-filter-bar')).toHaveAttribute('data-server-type-filter', 'templateCode')
    expect(screen.getByTestId('prompt-template-refresh-list')).toHaveAttribute('data-refresh-scope', 'template-list-only')
    expect(screen.getByTestId('prompt-template-create-open')).toHaveAttribute('data-source-endpoint', '/ai/prompt-template/save')
    expect(screen.getByTestId('prompt-template-test-open')).toHaveAttribute('data-template-id', '11')
    expect(screen.getByTestId('prompt-template-edit-open')).toHaveAttribute('data-preserves-form-input', 'true')
    expect(screen.getByTestId('prompt-template-delete-open')).toHaveAttribute('data-no-local-mutation-fallback', 'true')
    fireEvent.click(screen.getByLabelText('测试模板'))
    expect(screen.getByTestId('prompt-template-test-dialog-contract')).toHaveAttribute(
      'data-render-chain',
      '/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage',
    )
    fireEvent.change(await screen.findByLabelText('name'), { target: { value: '张三' } })
    fireEvent.click(screen.getByRole('button', { name: '渲染预览' }))

    await waitFor(() => {
      expect(aiApi.promptTemplateExtractVariables).toHaveBeenCalledWith('你好 {{name}}')
      expect(aiApi.promptTemplateTestRender).toHaveBeenCalledWith({
        templateContent: '你好 {{name}}',
        variables: { name: '张三' },
      })
      expect(aiApi.promptTemplateRecordUsage).toHaveBeenCalledWith(11)
    })
    expect(await screen.findByText('你好 张三')).toBeInTheDocument()
    expect(screen.getByTestId('prompt-template-render-submit')).toHaveAttribute('data-no-static-render-fallback', 'true')
    expect(screen.getByTestId('prompt-template-render-preview-surface')).toHaveAttribute('data-no-static-render-fallback', 'true')
    expect(screen.getByText(/检测到的变量：name/)).toBeInTheDocument()
  })

  it('PromptTemplatePage uses a theme-aware render preview surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme(<PromptTemplatePage />)

    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('测试模板'))
    fireEvent.change(await screen.findByLabelText('name'), { target: { value: '张三' } })
    fireEvent.click(screen.getByRole('button', { name: '渲染预览' }))

    expect(await screen.findByText('你好 张三')).toBeInTheDocument()
    expect(screen.getByTestId('prompt-template-render-preview-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('PromptTemplatePage saves active flag through save endpoint', async () => {
    renderPage(<PromptTemplatePage />)

    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    expect(screen.getByTestId('prompt-template-save-submit')).toHaveAttribute('data-source-endpoint', '/ai/prompt-template/save')
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(aiApi.promptTemplateSave).toHaveBeenCalledWith(expect.objectContaining({
        id: 11,
        templateCode: 'agent_chat',
        variantName: 'default',
        isActive: true,
        status: 1,
      }))
    })
  })

  it('Prompt pages render wrapped records through shared row normalization', async () => {
    vi.mocked(aiApi.promptTemplateList).mockResolvedValue({
      data: {
        records: [promptTemplate],
        totalElements: 1,
      },
    } as never)

    renderPage(<PromptTemplatePage />)
    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()

    renderPage(<PromptLabPage />)
    await waitFor(() => {
      expect(aiApi.promptTemplateList).toHaveBeenCalledTimes(2)
    })
    expect(screen.getAllByText('智能对话模板').length).toBeGreaterThanOrEqual(1)
  })

  it('PromptTemplatePage shows operation errors and empty list downgrade inline', async () => {
    vi.mocked(aiApi.promptTemplateList).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderPage(<PromptTemplatePage />)

    expect(await screen.findByText(/暂无 Prompt 模板/)).toBeInTheDocument()
    expect(screen.getByTestId('prompt-template-empty-state')).toHaveAttribute('data-no-static-template-fallback', 'true')
    expect(screen.getByText(/不会填充静态模板占位/)).toBeInTheDocument()

    vi.mocked(aiApi.promptTemplateList).mockResolvedValueOnce({
      total: 1,
      list: [promptTemplate],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.promptTemplateSave).mockRejectedValueOnce(new Error('templateCode duplicate') as never)

    renderPage(<PromptTemplatePage />)
    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/Prompt 模板保存失败：templateCode duplicate/)).toBeInTheDocument()
    expect(screen.getByTestId('prompt-template-action-error')).toHaveAttribute('data-no-local-mutation-fallback', 'true')
    expect(screen.getByText(/\/ai\/prompt-template\/save/)).toBeInTheDocument()
  })

  it('PromptLabPage enables template by saving it instead of get-active', async () => {
    renderPage(<PromptLabPage />)

    expect(screen.getByRole('heading', { name: 'Prompt 实验室' })).toBeInTheDocument()
    expect(screen.getByTestId('prompt-lab-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/prompt-template/list,/ai/prompt-template/save,/ai/prompt-template/delete,/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage',
    )
    expect(screen.getByTestId('prompt-lab-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/prompt-template/mock,/ai/prompt-template/local-cache,/ai/prompt-template/static-template,/ai/prompt-template/get-active-for-enable,/ai/prompt-template/static-render',
    )
    expect(screen.getByTestId('prompt-lab-boundary-contract')).toHaveAttribute('data-activation-strategy', 'save-isActive-status')
    expect(screen.getByText(/不再误调用 `get-active`/)).toBeInTheDocument()
    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    expect(screen.getByTestId('prompt-lab-grid')).toHaveAttribute('data-pagination-mode', 'server')
    expect(screen.getByTestId('prompt-lab-summary-cards')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('prompt-lab-refresh-list')).toHaveAttribute('data-refresh-scope', 'template-list-only')
    expect(screen.getByTestId('prompt-lab-create-open')).toHaveAttribute('data-source-endpoint', '/ai/prompt-template/save')
    expect(screen.getByTestId('prompt-lab-test-open')).toHaveAttribute('data-template-id', '11')
    expect(screen.getByTestId('prompt-lab-activate-save')).toHaveAttribute('data-no-get-active-for-enable', 'true')
    expect(screen.getByTestId('prompt-lab-edit-open')).toHaveAttribute('data-preserves-form-input', 'true')
    expect(screen.getByTestId('prompt-lab-delete-open')).toHaveAttribute('data-no-local-mutation-fallback', 'true')

    fireEvent.click(screen.getByRole('button', { name: '启用' }))

    await waitFor(() => {
      expect(aiApi.promptTemplateSave).toHaveBeenCalledWith(expect.objectContaining({
        id: 11,
        templateCode: 'agent_chat',
        isActive: true,
        status: 1,
      }))
    })
    expect(toast).toHaveBeenCalledWith('已启用', 'success')
  })

  it('PromptLabPage exposes save/delete/render failures by endpoint source', async () => {
    vi.mocked(aiApi.promptTemplateSave).mockRejectedValueOnce(new Error('save denied') as never)
    vi.mocked(aiApi.promptTemplateDelete).mockRejectedValueOnce(new Error('无权删除此模板') as never)

    renderPage(<PromptLabPage />)

    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '启用' }))
    expect(await screen.findByText(/Prompt 模板启用失败：save denied/)).toBeInTheDocument()
    expect(screen.getByTestId('prompt-lab-action-error')).toHaveAttribute('data-no-local-mutation-fallback', 'true')
    expect(screen.getByText(/启用不是 get-active/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))
    expect(await screen.findByText(/Prompt 模板删除失败：无权删除此模板/)).toBeInTheDocument()
    expect(screen.getByText(/\/ai\/prompt-template\/delete/)).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '确认操作' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '测试' }))
    fireEvent.change(screen.getByLabelText('变量 (JSON)'), { target: { value: '{"name":"张三"}' } })
    vi.mocked(aiApi.promptTemplateExtractVariables).mockRejectedValueOnce(new Error('extract timeout') as never)
    fireEvent.click(screen.getByRole('button', { name: '渲染测试' }))
    expect(await screen.findByText(/渲染失败：extract timeout/)).toBeInTheDocument()
    expect(screen.getByTestId('prompt-lab-render-error')).toHaveAttribute('data-no-static-render-fallback', 'true')
    expect(screen.getByText(/\/ai\/prompt-template\/extract-variables/)).toBeInTheDocument()
  })

  it('PromptLabPage renders with extract-variables and records usage', async () => {
    renderPage(<PromptLabPage />)

    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '测试' }))
    fireEvent.change(screen.getByLabelText('变量 (JSON)'), { target: { value: '{"name":"张三"}' } })
    expect(screen.getByTestId('prompt-lab-render-submit')).toHaveAttribute('data-no-static-render-fallback', 'true')
    fireEvent.click(screen.getByRole('button', { name: '渲染测试' }))

    await waitFor(() => {
      expect(aiApi.promptTemplateExtractVariables).toHaveBeenCalledWith('你好 {{name}}')
      expect(aiApi.promptTemplateTestRender).toHaveBeenCalledWith({
        templateContent: '你好 {{name}}',
        variables: { name: '张三' },
      })
      expect(aiApi.promptTemplateRecordUsage).toHaveBeenCalledWith(11)
    })
    expect(await screen.findByText('你好 张三')).toBeInTheDocument()
    expect(screen.getByTestId('prompt-lab-render-result-surface')).toHaveAttribute('data-no-static-render-fallback', 'true')
    expect(screen.getByText(/后端变量：name/)).toBeInTheDocument()
  })

  it('PromptLabPage uses a theme-aware render result surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme(<PromptLabPage />)

    expect(await screen.findByText('智能对话模板')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '测试' }))
    fireEvent.change(screen.getByLabelText('变量 (JSON)'), { target: { value: '{"name":"张三"}' } })
    fireEvent.click(screen.getByRole('button', { name: '渲染测试' }))

    expect(await screen.findByText('你好 张三')).toBeInTheDocument()
    expect(screen.getByTestId('prompt-lab-render-result-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(232, 245, 233)',
    })
  })

  it('PromptToolsPage documents wrapper boundary and switches lazy child pages', async () => {
    renderPage(<PromptToolsPage />)

    expect(screen.getByRole('heading', { name: 'Prompt 工具箱' })).toBeInTheDocument()
    expect(screen.getByTestId('prompt-tools-page')).toHaveAttribute('data-wrapper-only', 'true')
    expect(screen.getByTestId('prompt-tools-page')).toHaveAttribute('data-no-page-api-request', 'true')
    expect(screen.getByTestId('prompt-tools-boundary-contract')).toHaveAttribute('data-wrapper-only', 'true')
    expect(screen.getByText(/工具箱边界/)).toBeInTheDocument()
    expect(screen.getByText(/\/ai\/prompt-template\/list\|save\|delete\|get-active\|extract-variables\|test-render\|record-usage/)).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Prompt 实验室' }, { timeout: 15000 })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: 'Prompt 模板' }))

    expect(await screen.findByRole('heading', { name: 'Prompt 模板' }, { timeout: 15000 })).toBeInTheDocument()
  }, 20000)
})
