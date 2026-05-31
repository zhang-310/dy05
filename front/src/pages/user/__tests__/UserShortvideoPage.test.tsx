import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { shortvideoApi } from '@/api/shortvideo'
import UserShortvideoPage from '../UserShortvideoPage'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    list: vi.fn(),
    save: vi.fn(),
    publishAiReview: vi.fn(),
  },
}))

function renderPage(path = '/user/shortvideo') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/user/shortvideo" element={<UserShortvideoPage />} />
        <Route path="/user/shortvideo/create" element={<UserShortvideoPage />} />
        <Route path="/user/shortvideo/planning" element={<UserShortvideoPage />} />
        <Route path="/user/shortvideo/materials" element={<UserShortvideoPage />} />
        <Route path="/user/shortvideo/publish" element={<UserShortvideoPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('UserShortvideoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.list).mockResolvedValue({
      total: 1,
      pageNum: 0,
      pageSize: 8,
      list: [{
        id: 9,
        ownerId: 3,
        title: '个人测评短视频',
        projectType: 'daily',
        status: 'draft',
        reviewStatus: 'pending',
        scheduleDate: '2026-05-27',
      }],
    })
    vi.mocked(shortvideoApi.save).mockResolvedValue(10)
    vi.mocked(shortvideoApi.publishAiReview).mockResolvedValue({
      passed: false,
      issues: ['标题存在绝对化表达'],
      suggestions: ['改为更温和的功效描述'],
      officialReferences: [{
        kbName: 'douyin_weigui',
        title: '直播及短视频功效宣传规则',
        contentPreview: '不得使用绝对化、夸大化表述。',
        score: 0.91,
      }],
    })
  })

  it('renders the independent user shortvideo line without talent redirect', async () => {
    renderPage()

    const page = await screen.findByTestId('user-shortvideo-page')
    expect(page).toHaveAttribute('data-contract-scope', 'user-shortvideo-creator-line')
    expect(page).toHaveAttribute('data-route-prefix', '/user/shortvideo')
    expect(page).toHaveAttribute('data-no-talent-shell-redirect', 'true')
    expect(page).toHaveAttribute(
      'data-real-project-endpoints',
      '/short-video/project/list,/short-video/project/save,/short-video/publish/ai-review',
    )
    expect(screen.getByText('我的短视频')).toBeInTheDocument()
    expect(await screen.findByText('个人测评短视频')).toBeInTheDocument()
  })

  it('navigates user workflow entries inside the user shell', async () => {
    renderPage()

    const cards = await screen.findAllByTestId('user-shortvideo-workflow-card')
    const planningCard = cards.find(card => within(card).queryByText('脚本策划'))
    expect(planningCard).toBeTruthy()
    fireEvent.click(within(planningCard!).getByRole('button', { name: /进入/ }))

    expect(screen.getByText('脚本策划 · AI 审核')).toBeInTheDocument()
    expect(screen.getByTestId('user-shortvideo-page')).toHaveAttribute('data-no-talent-shell-redirect', 'true')
  })

  it('loads real user projects and creates drafts through the project API', async () => {
    renderPage('/user/shortvideo/create')

    expect(await screen.findByText('个人测评短视频')).toBeInTheDocument()
    expect(shortvideoApi.list).toHaveBeenCalledWith({ page: 0, rows: 8, sortName: 'createTime', sortOrder: 'desc' })

    fireEvent.click(screen.getByRole('button', { name: /创建草稿/ }))

    await waitFor(() => {
      expect(shortvideoApi.save).toHaveBeenCalledWith(expect.objectContaining({
        projectType: 'daily',
        status: 'draft',
        shootStatus: 'not_started',
      }))
    })
  })

  it('runs publish review and renders official rule references', async () => {
    renderPage('/user/shortvideo/publish')

    const project = await screen.findByText('个人测评短视频')
    const projectCard = project.closest('.MuiPaper-root')
    expect(projectCard).toBeTruthy()
    fireEvent.click(within(projectCard as HTMLElement).getByRole('button', { name: /发布检查/ }))

    expect(await screen.findByTestId('user-shortvideo-review-result')).toBeInTheDocument()
    expect(shortvideoApi.publishAiReview).toHaveBeenCalledWith({
      projectId: 9,
      title: '个人测评短视频',
      videoUrl: undefined,
    })
    expect(screen.getByText(/标题存在绝对化表达/)).toBeInTheDocument()
    expect(screen.getByText('直播及短视频功效宣传规则')).toBeInTheDocument()
  })

  it('shows empty state instead of fake local project data', async () => {
    vi.mocked(shortvideoApi.list).mockResolvedValueOnce({
      total: 0,
      pageNum: 0,
      pageSize: 8,
      list: [],
    })

    renderPage()

    expect(await screen.findByTestId('user-shortvideo-empty')).toBeInTheDocument()
    expect(screen.queryByText('个人测评短视频')).not.toBeInTheDocument()
  })
})
