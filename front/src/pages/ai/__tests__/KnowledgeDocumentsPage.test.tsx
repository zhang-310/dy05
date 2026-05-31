import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, waitForElementToBeRemoved } from '@/test/utils'
import KnowledgeDocumentsPage from '../KnowledgeDocumentsPage'
import { aiApi } from '@/api/ai'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

const toast = vi.fn()

vi.mock('@/api/ai', () => ({
  aiApi: {
    docList: vi.fn(),
    docDelete: vi.fn(),
    kbImportFromPath: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
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

function renderPage() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/admin/ai/knowledge/7/documents']}>
      <Routes>
        <Route path="/admin/ai/knowledge/:kbId/documents" element={<KnowledgeDocumentsPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderPageWithAppTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={['/admin/ai/knowledge/7/documents']}>
        <Routes>
          <Route path="/admin/ai/knowledge/:kbId/documents" element={<KnowledgeDocumentsPage />} />
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('KnowledgeDocumentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(aiApi.docList).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 11,
          kbId: 7,
          title: '面膜 FAQ',
          content: '# 面膜\n\n- 使用后补水',
          fileType: 'md',
          status: 1,
          chunkCount: 3,
          tokenCount: 220,
          qualityHeuristicScore: 92,
          createTime: '2026-05-20 10:00:00',
        },
        {
          id: 12,
          kbId: 7,
          title: '低质文档',
          content: '短',
          fileType: 'txt',
          status: 0,
          chunkCount: 0,
          tokenCount: 1,
          qualityHeuristicScore: 30,
          syncRetryCount: 2,
          createTime: '2026-05-20 11:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.kbImportFromPath).mockResolvedValue({ successCount: 1 } as never)
  })

  it('renders document diagnostics, markdown preview, and imports with sourcePath', async () => {
    renderPage()

    expect(await screen.findByText('知识库文档管理')).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-documents-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/knowledge-base/{kbId}/documents,/ai/knowledge-base/import-from-path,DELETE /ai/knowledge-base/document/{docId}',
    )
    expect(screen.getByTestId('knowledge-documents-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/knowledge-base/document/upload-browser-file,/ai/knowledge-base/document/local-list,/ai/knowledge-base/document/static-content,/ai/knowledge-base/import-local-path',
    )
    expect(screen.getByTestId('knowledge-documents-page')).toHaveAttribute('data-server-path-import', 'true')
    expect(screen.getByTestId('kb-doc-import-boundary-notice')).toHaveAttribute('data-no-browser-file-read', 'true')
    expect(screen.getByText('低质/空分块')).toBeInTheDocument()
    expect(await screen.findByText('低质文档')).toBeInTheDocument()
    expect(screen.getByTestId('kb-doc-list-grid')).toHaveAttribute('data-pagination-mode', 'server')

    fireEvent.click(screen.getByRole('button', { name: '导入文档' }))
    expect(screen.getByTestId('kb-doc-import-dialog')).toHaveAttribute('data-source-endpoint', '/ai/knowledge-base/import-from-path')
    expect(screen.getByTestId('kb-doc-import-dialog')).toHaveAttribute('data-no-browser-file-read', 'true')
    fireEvent.change(screen.getByRole('textbox', { name: '容器内路径' }), {
      target: { value: '/data/knowledge-base-imports/demo.md' },
    })
    fireEvent.click(screen.getByRole('button', { name: '提交导入' }))

    await waitFor(() => {
      expect(aiApi.kbImportFromPath).toHaveBeenCalledWith({
        kbId: 7,
        sourcePath: '/data/knowledge-base-imports/demo.md',
        autoClassify: false,
      })
      expect(toast).toHaveBeenCalledWith('导入任务已提交', 'success')
    })
    await waitForElementToBeRemoved(() => screen.queryByRole('dialog', { name: '导入文档' }))

    fireEvent.click(screen.getByRole('button', { name: '低质文档' }))
    expect(await screen.findByText('文档已落库但向量未完成，请检查嵌入模型、Ollama 地址、Milvus/ES 状态或索引队列 error_msg。')).toBeInTheDocument()
    expect(screen.getByTestId('kb-doc-detail-issue-hint')).toHaveAttribute('data-docker-ollama-diagnostic', 'true')
    expect(screen.getAllByText('低质 30').length).toBeGreaterThan(0)
  }, 30000)

  it('keeps import dialog input and shows endpoint when import fails', async () => {
    vi.mocked(aiApi.kbImportFromPath).mockRejectedValue(new Error('路径不在允许的导入目录内') as never)

    renderPage()

    await screen.findByText('低质文档')
    fireEvent.click(screen.getByRole('button', { name: '导入文档' }))
    fireEvent.change(screen.getByRole('textbox', { name: '容器内路径' }), {
      target: { value: '/host/demo.md' },
    })
    fireEvent.click(screen.getByRole('button', { name: '提交导入' }))

    expect(await screen.findByText(/路径导入失败（\/ai\/knowledge-base\/import-from-path）：路径不在允许的导入目录内/)).toBeInTheDocument()
    expect(screen.getByTestId('kb-doc-import-error')).toHaveAttribute('data-preserves-form-input', 'true')
    expect(screen.getByDisplayValue('/host/demo.md')).toBeInTheDocument()
    expect(screen.getByText(/Docker 环境需先把本机目录挂载到容器内/)).toBeInTheDocument()
  })

  it('keeps document row and shows endpoint when delete fails', async () => {
    vi.mocked(aiApi.docDelete).mockRejectedValue(new Error('delete denied') as never)

    renderPage()

    await screen.findByText('面膜 FAQ')
    fireEvent.click(screen.getAllByRole('button', { name: '删除' })[0])
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/文档删除失败（DELETE \/ai\/knowledge-base\/document\/11）：delete denied/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('kb-doc-delete-error')).toHaveAttribute('data-no-local-delete-fallback', 'true')
    expect(screen.getByText('面膜 FAQ')).toBeInTheDocument()
  })

  it('uses theme error color for detail delete hover surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithAppTheme()

    fireEvent.click(await screen.findByRole('button', { name: '面膜 FAQ' }))
    expect(await screen.findByTestId('kb-doc-detail-markdown')).toHaveAttribute('data-renderer', 'MarkdownViewer')
    const deleteButton = await screen.findByTestId('kb-doc-detail-delete-button')
    expect(deleteButton).toHaveAttribute('data-source-endpoint', '/ai/knowledge-base/document/{docId}')
    expect(deleteButton).toHaveStyle({
      color: 'rgb(244, 67, 54)',
      borderColor: 'rgb(244, 67, 54)',
    })
  })

  it('uses theme tokens for document search and import actions in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithAppTheme()

    expect(await screen.findByText('低质文档')).toBeInTheDocument()
    const searchButton = screen.getByTestId('kb-doc-search-action-surface')
    const importButton = screen.getByTestId('kb-doc-import-action-surface')

    expect(window.getComputedStyle(searchButton).color).not.toBe('rgb(0, 0, 0)')
    expect(window.getComputedStyle(importButton).color).not.toBe('rgb(0, 0, 0)')

    fireEvent.click(screen.getByRole('button', { name: '低质文档' }))
    expect(await screen.findByText('文档已落库但向量未完成，请检查嵌入模型、Ollama 地址、Milvus/ES 状态或索引队列 error_msg。')).toBeInTheDocument()
  })
})
