import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ProjectsPage from '../ProjectsPage'
import { shortvideoApi } from '@/api/shortvideo'

const navigate = vi.hoisted(() => vi.fn())
let mockPathname = '/admin/shortvideo/projects'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
    useLocation: () => ({ pathname: mockPathname, search: '', hash: '', state: null, key: 'test' }),
  }
})

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ProjectsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    navigate.mockClear()
    mockPathname = '/admin/shortvideo/projects'
    vi.mocked(shortvideoApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          title: '爆款切片项目',
          projectType: 'viral_clone',
          status: 'processing',
          scriptId: 11,
          shotListId: undefined,
          finalVideoUrl: 'https://cdn.test/final.mp4',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(shortvideoApi.save).mockResolvedValue(1 as never)
    vi.mocked(shortvideoApi.delete).mockResolvedValue(undefined as never)
  })

  it('loads shortvideo projects and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('短视频项目')).toBeInTheDocument()
    expect(screen.getByText(/项目 CRUD、工作台入口/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-projects-page')).toHaveAttribute('data-contract-scope', 'shortvideo-projects-admin')
    expect(screen.getByTestId('shortvideo-projects-page')).toHaveAttribute('data-ready-endpoints', '/short-video/project/list|/short-video/project/save|/short-video/project/delete')
    expect(screen.getByTestId('shortvideo-projects-page')).toHaveAttribute('data-ready-routes', expect.stringContaining('/shortvideo/workbench?projectId=:id'))
    expect(screen.getByTestId('shortvideo-projects-page')).toHaveAttribute('data-supported-actions', expect.stringContaining('navigate-project-workbench'))
    expect(screen.getByTestId('shortvideo-projects-page')).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/short-video/project/local-list'))
    expect(screen.getByTestId('shortvideo-projects-boundary-contract')).toHaveAttribute('data-no-local-project-fallback', 'true')
    expect(screen.getByTestId('shortvideo-projects-search-button')).toHaveAttribute('data-source-endpoint', '/short-video/project/list')
    expect(screen.getByTestId('shortvideo-projects-create-button')).toHaveAttribute('data-source-endpoint', '/short-video/project/save')

    await waitFor(() => {
      expect(shortvideoApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, title: '', status: undefined })
    })

    await waitFor(() => {
      expect(screen.getByText('爆款切片项目')).toBeInTheDocument()
      expect(screen.getAllByText('进行中').length).toBeGreaterThan(0)
    })
    expect(screen.getByText('已有成片')).toBeInTheDocument()
    expect(screen.getByText('缺分镜')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-projects-summary')).toHaveAttribute('data-no-static-summary', 'true')
    expect(screen.getByTestId('shortvideo-projects-grid')).toHaveAttribute('data-server-pagination', 'true')
  })

  it('saves project chain fields with real project save payload', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '新建项目' }))
    const dialog = await screen.findByRole('dialog', { name: '新建项目' })
    fireEvent.change(within(dialog).getByLabelText(/项目标题/), { target: { value: '手动项目' } })
    fireEvent.change(within(dialog).getByLabelText('Persona ID（可选）'), { target: { value: '12' } })
    fireEvent.change(within(dialog).getByLabelText('脚本 ID（可选）'), { target: { value: '34' } })
    fireEvent.change(within(dialog).getByLabelText('分镜 ID（可选）'), { target: { value: '56' } })
    fireEvent.change(within(dialog).getByLabelText('成片 URL（可选）'), { target: { value: 'https://cdn.test/final.mp4' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.save).toHaveBeenCalledWith(expect.objectContaining({
        title: '手动项目',
        projectType: 'viral_clone',
        personaId: 12,
        scriptId: 34,
        shotListId: 56,
        finalVideoUrl: 'https://cdn.test/final.mp4',
      }))
    })
  })

  it('keeps project form input visible when save fails with endpoint', async () => {
    vi.mocked(shortvideoApi.save).mockRejectedValueOnce(new Error('save blocked') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '新建项目' }))
    const dialog = await screen.findByRole('dialog', { name: '新建项目' })
    fireEvent.change(within(dialog).getByLabelText(/项目标题/), { target: { value: '失败项目' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    const saveErrors = await screen.findAllByText(/项目保存失败（POST \/short-video\/project\/save）：save blocked/)
    expect(saveErrors.length).toBeGreaterThan(0)
    expect(screen.getAllByText(/projectId=-; title=失败项目; projectType=viral_clone/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/scope=admin; shell=管理员端/).length).toBeGreaterThan(0)
    expect(within(dialog).getByDisplayValue('失败项目')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-projects-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('shortvideo-projects-operation-error')).toHaveAttribute('data-no-local-project-mutation', 'true')
  })

  it('keeps delete confirmation and row visible when delete fails with endpoint', async () => {
    vi.mocked(shortvideoApi.delete).mockRejectedValueOnce(new Error('delete blocked') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('爆款切片项目')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/项目删除失败（POST \/short-video\/project\/delete）：delete blocked/)).toBeInTheDocument()
    expect(screen.getAllByText(/projectId=1; title=爆款切片项目; projectType=viral_clone/).length).toBeGreaterThan(0)
    expect(screen.getByText(/上次删除失败（POST \/short-video\/project\/delete）：delete blocked/)).toBeInTheDocument()
    expect(screen.getByText('爆款切片项目')).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-projects-operation-error')).toHaveAttribute('data-source-endpoints', '/short-video/project/save|/short-video/project/delete')
  })

  it('shows project list endpoint when loading fails without local rows', async () => {
    vi.mocked(shortvideoApi.list).mockRejectedValueOnce(new Error('project table down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('项目列表加载失败')).toBeInTheDocument()
    expect(screen.getByText(/POST \/short-video\/project\/list：project table down/)).toBeInTheDocument()
    expect(screen.getByText(/scope=admin; shell=管理员端; titleFilter=空; statusFilter=全部/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-projects-list-error')).toHaveAttribute('data-no-local-project-fallback', 'true')
    expect(screen.getByTestId('shortvideo-projects-list-error')).toHaveAttribute('data-input-retained', 'true')
  })

  it('keeps talent shortvideo workbench navigation on the talent entry url with projectId', async () => {
    mockPathname = '/talent/shortvideo'

    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '达人短视频项目' })).toBeInTheDocument()
    expect(screen.getByText(/当前为达人端路由/)).toBeInTheDocument()
    expect(screen.getByTestId('shortvideo-projects-page')).toHaveAttribute('data-contract-scope', 'shortvideo-projects-talent')
    expect(screen.getByTestId('shortvideo-projects-boundary-contract')).toHaveAttribute('data-workbench-route-scope', 'talent')

    const row = await screen.findByText('爆款切片项目')
    expect(row).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('进入工作台'))

    expect(navigate).toHaveBeenCalledWith('/talent/shortvideo?projectId=1')
    expect(screen.getByTestId('shortvideo-projects-open-workbench-button')).toHaveAttribute('data-target-route', '/talent/shortvideo?projectId=1')
  })

  it('shows talent scope in save and delete failures', async () => {
    mockPathname = '/talent/shortvideo'
    vi.mocked(shortvideoApi.save).mockRejectedValueOnce(new Error('talent save blocked') as never)
    vi.mocked(shortvideoApi.delete).mockRejectedValueOnce(new Error('talent delete blocked') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '新建项目' }))
    const dialog = await screen.findByRole('dialog', { name: '新建项目' })
    fireEvent.change(within(dialog).getByLabelText(/项目标题/), { target: { value: '达人失败项目' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/项目保存失败（POST \/short-video\/project\/save）：talent save blocked/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/scope=talent; shell=达人端/).length).toBeGreaterThan(0)

    fireEvent.click(within(dialog).getByRole('button', { name: '取消' }))
    fireEvent.click(await screen.findByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/项目删除失败（POST \/short-video\/project\/delete）：talent delete blocked/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/scope=talent; shell=达人端/).length).toBeGreaterThan(0)
  }, 20000)
})
