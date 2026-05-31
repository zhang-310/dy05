import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import WorkflowEditorPage from '../WorkflowEditorPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    workflowTemplateList: vi.fn(),
    workflowTemplateGet: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

describe('WorkflowEditorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    vi.mocked(shortvideoApi.workflowTemplateList).mockResolvedValue([
      { id: 3, templateName: '短视频标准链路', description: '脚本到成片' },
    ] as never)
    vi.mocked(shortvideoApi.workflowTemplateGet).mockResolvedValue({
      id: 3,
      templateName: '短视频标准链路',
      steps: JSON.stringify([
        { id: 'a', type: 'script', label: '写脚本', durationDays: 1 },
        { id: 'b', type: 'videoGen', label: '生成视频', durationDays: 2 },
      ]),
    } as never)
  })

  it('loads backend templates and saves explicit browser draft', async () => {
    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workflow-editor?projectId=7']}>
        <WorkflowEditorPage />
      </MemoryRouter>,
    )

    const root = screen.getByTestId('workflow-editor-page')
    expect(root).toHaveAttribute('data-ready-endpoints', '/short-video/workflow-template/list|/short-video/workflow-template/get|browser-local-draft:sv-workflow-draft')
    expect(root).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/workflow-editor?projectId=:id'))
    expect(root).toHaveAttribute('data-supported-actions', expect.stringContaining('save-browser-workflow-draft'))
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/workflow/project-save'))
    expect(root).toHaveAttribute('data-explicit-browser-draft', 'true')
    expect(root).toHaveAttribute('data-no-db-workflow-save', 'true')
    expect(screen.getByTestId('workflow-editor-save-draft-button')).toBeInTheDocument()
    expect(screen.getByTestId('workflow-editor-boundary-contract')).toHaveAttribute('data-explicit-browser-draft', 'true')
    expect(screen.getByText(/保存的是浏览器草稿/)).toBeInTheDocument()
    await waitFor(() => {
      expect(shortvideoApi.workflowTemplateList).toHaveBeenCalled()
    })

    fireEvent.mouseDown(screen.getByLabelText('后端工作流模板'))
    fireEvent.click(await screen.findByRole('option', { name: '短视频标准链路' }))
    fireEvent.click(screen.getByTestId('workflow-editor-apply-template-button'))

    await waitFor(() => {
      expect(shortvideoApi.workflowTemplateGet).toHaveBeenCalledWith(3)
    })
    await waitFor(() => {
      expect(screen.getAllByText('写脚本').length).toBeGreaterThan(0)
      expect(screen.getAllByText('生成视频').length).toBeGreaterThan(0)
    })

    fireEvent.click(screen.getByRole('button', { name: '保存浏览器草稿' }))
    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith('已保存到浏览器草稿', 'success')
    })
    expect(window.localStorage.getItem('sv-workflow-draft:7')).toContain('写脚本')
    expect(screen.getByTestId('workflow-editor-summary')).toHaveAttribute('data-derived-from', 'page-draft-nodes')
  })

  it('shows invalid backend template steps as explicit page error', async () => {
    vi.mocked(shortvideoApi.workflowTemplateGet).mockResolvedValueOnce({
      id: 3,
      templateName: '短视频标准链路',
      steps: '{bad json',
    } as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workflow-editor?projectId=7']}>
        <WorkflowEditorPage />
      </MemoryRouter>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('后端工作流模板'))
    fireEvent.click(await screen.findByRole('option', { name: '短视频标准链路' }))
    fireEvent.click(screen.getByRole('button', { name: '载入步骤' }))

    expect(await screen.findByText(/模板 steps 为空或不是有效 JSON 数组/)).toBeInTheDocument()
    expect(screen.getByTestId('workflow-editor-template-apply-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('workflow-editor-template-apply-error')).toHaveAttribute('data-no-static-template-fallback', 'true')
    expect(screen.getByText(/POST \/short-video\/workflow-template\/get/)).toBeInTheDocument()
  })

  it('shows backend template list endpoint and keeps local draft editor usable', async () => {
    vi.mocked(shortvideoApi.workflowTemplateList).mockRejectedValueOnce(new Error('template list down') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workflow-editor?projectId=7']}>
        <WorkflowEditorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/工作流模板加载失败（POST \/short-video\/workflow-template\/list）/)).toBeInTheDocument()
    expect(screen.getByTestId('workflow-editor-template-list-error')).toHaveAttribute('data-no-static-template-fallback', 'true')
    expect(screen.getAllByText('脚本策划').length).toBeGreaterThan(0)
  })

  it('shows backend template get endpoint when apply fails and preserves draft', async () => {
    vi.mocked(shortvideoApi.workflowTemplateGet).mockRejectedValueOnce(new Error('template detail down') as never)

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workflow-editor?projectId=7']}>
        <WorkflowEditorPage />
      </MemoryRouter>,
    )

    fireEvent.mouseDown(await screen.findByLabelText('后端工作流模板'))
    fireEvent.click(await screen.findByRole('option', { name: '短视频标准链路' }))
    fireEvent.click(screen.getByRole('button', { name: '载入步骤' }))

    expect(await screen.findByText(/工作流模板应用失败：POST \/short-video\/workflow-template\/get：template detail down/)).toBeInTheDocument()
    expect(screen.getByTestId('workflow-editor-template-apply-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getAllByText('脚本策划').length).toBeGreaterThan(0)
  })

  it('loads explicit browser draft without backend project save', async () => {
    window.localStorage.setItem('sv-workflow-draft:7', JSON.stringify([
      { id: 'local-1', type: 'script', label: '本地草稿脚本', durationDays: 1 },
      { id: 'local-2', type: 'publish', label: '本地草稿发布', durationDays: 1 },
    ]))

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/shortvideo/workflow-editor?projectId=7']}>
        <WorkflowEditorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('workflow-editor-draft-loaded')).toHaveAttribute('data-source', 'browser-local-draft')
    expect(screen.getAllByText('本地草稿脚本').length).toBeGreaterThan(0)
    expect(screen.getByTestId('workflow-editor-page')).toHaveAttribute('data-no-db-workflow-save', 'true')
    expect(shortvideoApi.workflowTemplateList).toHaveBeenCalled()
    expect(shortvideoApi.workflowTemplateGet).not.toHaveBeenCalled()
  })
})
