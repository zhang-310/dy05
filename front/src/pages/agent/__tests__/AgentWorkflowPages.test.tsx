import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import AgentWorkflowListPage from '../AgentWorkflowListPage'
import AgentWorkflowEditorPage from '../AgentWorkflowEditorPage'
import { workflowApi, agentApi } from '@/api/agent'

const navigate = vi.fn()
let routeId: string | undefined

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useParams: () => ({ id: routeId }),
  }
})

vi.mock('@/api/agent', () => ({
  workflowApi: {
    list: vi.fn(),
    executionList: vi.fn(),
    delete: vi.fn(),
    execute: vi.fn(),
    get: vi.fn(),
    save: vi.fn(),
  },
  agentApi: {
    list: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns }: any) => (
      <div>
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

describe('Agent workflow pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeId = undefined
    vi.mocked(workflowApi.list).mockResolvedValue({
      total: 1,
      list: [{
        id: 12,
        userId: 1,
        name: '爆款脚本工作流',
        description: '多智能体生成脚本',
        version: 1,
        status: 1,
        createTime: '2026-05-21 10:00:00',
        steps: [],
      }],
      pageNum: 0,
      pageSize: 100,
    } as never)
    vi.mocked(workflowApi.executionList).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 100 } as never)
    vi.mocked(workflowApi.save).mockResolvedValue(66 as never)
    vi.mocked(agentApi.list).mockResolvedValue({
      total: 1,
      records: [{
        id: 7,
        agentName: '话术助手',
        description: '生成脚本',
        agentType: 1,
        responseMode: 1,
        status: 1,
        createTime: '2026-05-21 09:00:00',
      }],
      pageNum: 0,
      pageSize: 1000,
    } as never)
  })

  it('uses admin-prefixed routes from workflow list actions', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowListPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('agent-workflow-list-page')).toHaveAttribute('data-no-local-execution-record', 'true')
    expect(screen.getByTestId('agent-workflow-list-boundary-contract')).toBeInTheDocument()
    expect(await screen.findByText('爆款脚本工作流')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '新建工作流' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/workflow/edit')

    fireEvent.click(screen.getByRole('button', { name: '编辑工作流 爆款脚本工作流' }))
    expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/workflow/edit/12')

    fireEvent.click(screen.getByRole('button', { name: '删除工作流 爆款脚本工作流' }))
    expect(await screen.findByText('确认删除工作流')).toBeInTheDocument()
    expect(screen.getByText('确定要删除「爆款脚本工作流」吗？删除后无法继续执行该工作流。')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(workflowApi.delete).toHaveBeenCalledWith(12)
    })
  })

  it('keeps workflow rows when execute or delete operations fail', async () => {
    vi.mocked(workflowApi.execute).mockRejectedValueOnce(new Error('execute denied') as never)
    vi.mocked(workflowApi.delete).mockRejectedValueOnce(new Error('delete denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('爆款脚本工作流')).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-list-grid-contract')).toHaveAttribute('data-no-local-workflow-fallback', 'true')

    fireEvent.click(screen.getByRole('button', { name: '执行工作流 爆款脚本工作流' }))
    expect(await screen.findByText(/启动工作流失败：execute denied。来源：\/agent\/workflow\/execute/)).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-list-action-error')).toHaveAttribute('data-no-local-execution-record', 'true')
    expect(screen.getByText('爆款脚本工作流')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '删除工作流 爆款脚本工作流' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))
    expect(await screen.findByText(/删除工作流失败：delete denied。来源：\/agent\/workflow\/delete/)).toBeInTheDocument()
    expect(screen.getByText('爆款脚本工作流')).toBeInTheDocument()
  })

  it('redirects newly saved workflow to admin-prefixed edit route', async () => {
    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowEditorPage />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('agent-workflow-editor-page')).toHaveAttribute('data-no-local-workflow-mutation', 'true')
    expect(screen.getByTestId('agent-workflow-editor-boundary-contract')).toHaveAttribute('data-no-local-execution-record', 'true')
    fireEvent.change(screen.getByRole('textbox', { name: /工作流名称/ }), { target: { value: '新品脚本工作流' } })
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加步骤' })).toBeEnabled()
    })
    fireEvent.click(screen.getByRole('button', { name: '添加步骤' }))
    await waitFor(() => {
      expect(screen.getByText('步骤 1')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(workflowApi.save).toHaveBeenCalledWith(expect.objectContaining({
        name: '新品脚本工作流',
      }))
      expect(navigate).toHaveBeenCalledWith('/admin/ai/agent/workflow/edit/66', { replace: true })
    })
  })

  it('keeps workflow editor form and steps when save fails', async () => {
    vi.mocked(workflowApi.save).mockRejectedValueOnce(new Error('save denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowEditorPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByRole('textbox', { name: /工作流名称/ }), { target: { value: '失败保留工作流' } })
    fireEvent.change(screen.getByRole('textbox', { name: /描述/ }), { target: { value: '保存失败也不能清空配置' } })
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '添加步骤' })).toBeEnabled()
    })
    fireEvent.click(screen.getByRole('button', { name: '添加步骤' }))
    await waitFor(() => {
      expect(screen.getByText('步骤 1')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText(/保存工作流失败（POST \/agent\/workflow\/save）：save denied/)).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-editor-action-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('失败保留工作流')).toBeInTheDocument()
    expect(screen.getByDisplayValue('保存失败也不能清空配置')).toBeInTheDocument()
    expect(screen.getByText('步骤 1')).toBeInTheDocument()
  })

  it('keeps quick execute input when workflow execution fails in editor', async () => {
    routeId = '12'
    vi.mocked(workflowApi.get).mockResolvedValue({
      id: 12,
      userId: 1,
      name: '可执行工作流',
      description: '执行失败回归',
      version: 1,
      status: 1,
      createTime: '2026-05-21 10:00:00',
      steps: [{
        id: 1,
        stepOrder: 1,
        agentId: 7,
        agentName: '话术助手',
        stepName: '生成',
        outputKey: 'script',
      }],
    } as never)
    vi.mocked(workflowApi.execute).mockRejectedValueOnce(new Error('execute failed') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowEditorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByDisplayValue('可执行工作流')).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-quick-execute-contract')).toHaveAttribute('data-no-local-execution-record', 'true')
    fireEvent.change(screen.getByPlaceholderText('输入内容，敲击回车执行'), { target: { value: '生成新品直播脚本' } })
    fireEvent.click(screen.getAllByRole('button', { name: '执行' })[1])

    expect(await screen.findByText(/执行工作流失败（POST \/agent\/workflow\/execute）：execute failed/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('生成新品直播脚本')).toBeInTheDocument()
    expect(screen.getByText('步骤 1')).toBeInTheDocument()
  })

  it('renders wrapped workflow detail steps and serialized dependencies', async () => {
    routeId = '12'
    vi.mocked(workflowApi.get).mockResolvedValue({
      id: 12,
      userId: 1,
      name: '包装详情工作流',
      description: '编辑包装步骤',
      version: 1,
      status: 1,
      createTime: '2026-05-21 10:00:00',
      steps: {
        records: [
          {
            id: 1,
            stepOrder: 1,
            agentId: 7,
            agentName: '话术助手',
            stepName: '分析',
            outputKey: 'analysis',
            dependsOn: '[]',
            executionMode: 0,
            retryCount: 1,
            timeoutSeconds: 300,
          },
          {
            id: 2,
            stepOrder: 2,
            agentId: 7,
            agentName: '话术助手',
            stepName: '改写',
            outputKey: 'rewrite',
            dependsOn: '["analysis"]',
            executionMode: 1,
            retryCount: 2,
            timeoutSeconds: 180,
          },
        ],
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowEditorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByDisplayValue('包装详情工作流')).toBeInTheDocument()
    expect(screen.getByDisplayValue('编辑包装步骤')).toBeInTheDocument()
    expect(screen.getByText('步骤 1')).toBeInTheDocument()
    expect(screen.getByText('步骤 2')).toBeInTheDocument()
    expect(screen.getByText('依赖: analysis')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: 'DAG 预览' }))
    expect(await screen.findByText('DAG 执行预览')).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-dag-preview')).toHaveAttribute('data-form-only-preview', 'true')
    expect(screen.getByText('分析')).toBeInTheDocument()
    expect(screen.getByText('改写')).toBeInTheDocument()
  })

  it('shows concrete editor load endpoints when dependencies fail', async () => {
    routeId = '12'
    vi.mocked(agentApi.list).mockRejectedValueOnce(new Error('agent list down') as never)
    vi.mocked(workflowApi.get).mockRejectedValueOnce(new Error('workflow missing') as never)

    renderWithProviders(
      <MemoryRouter>
        <AgentWorkflowEditorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/智能体列表加载失败（POST \/agent\/list）：agent list down/)).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-editor-agents-error')).toHaveAttribute('data-no-local-agent-fallback', 'true')
    expect(await screen.findByText(/工作流详情加载失败（POST \/agent\/workflow\/get）：workflow missing/)).toBeInTheDocument()
    expect(screen.getByTestId('agent-workflow-editor-detail-error')).toHaveAttribute('data-no-local-workflow-fallback', 'true')
  })
})
