import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import LiveSessionFormPage from '../LiveSessionFormPage'
import LiveWorkbenchPage from '../LiveWorkbenchPage'
import { liveApi } from '@/api/live'
import { douyinApi } from '@/api/douyin'

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionGet: vi.fn(),
    sessionSave: vi.fn(),
    sessionReadiness: vi.fn(),
    sessionOverview: vi.fn(),
    sessionClone: vi.fn(),
    sessionExportToShortVideo: vi.fn(),
    productBySession: vi.fn(),
    scriptBySession: vi.fn(),
  },
  getSession: vi.fn(),
  getProductsBySession: vi.fn(),
  getScriptsBySession: vi.fn(),
  checkGenerateFullInProgress: vi.fn(),
}))

vi.mock('@/api/douyin', () => ({
  douyinApi: {
    accountList: vi.fn(),
    personaList: vi.fn(),
  },
}))

vi.mock('../SessionWorkspacePage', () => ({
  SessionWorkspacePage: ({ sessionId }: { sessionId: number }) => <div>工作台主体 #{sessionId}</div>,
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

function renderForm(path = '/admin/live/sessions/create') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/admin/live/sessions/create" element={<LiveSessionFormPage />} />
        <Route path="/admin/live/sessions/:id/edit" element={<LiveSessionFormPage />} />
        <Route path="/org/live/sessions/create" element={<LiveSessionFormPage />} />
        <Route path="/org/live/sessions/:id/edit" element={<LiveSessionFormPage />} />
        <Route path="/talent/live/sessions/create" element={<LiveSessionFormPage />} />
        <Route path="/talent/live/sessions/:id/edit" element={<LiveSessionFormPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderWorkbench(path = '/admin/live/workbench/18') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/admin/live/workbench/:sessionId" element={<LiveWorkbenchPage />} />
        <Route path="/admin/live/sessions/:sessionId" element={<LiveWorkbenchPage />} />
        <Route path="/org/live/sessions/:sessionId" element={<LiveWorkbenchPage />} />
        <Route path="/talent/live/sessions/:sessionId" element={<LiveWorkbenchPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function renderDarkWorkbench(path = '/admin/live/workbench/18') {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/admin/live/workbench/:sessionId" element={<LiveWorkbenchPage />} />
          <Route path="/admin/live/sessions/:sessionId" element={<LiveWorkbenchPage />} />
          <Route path="/org/live/sessions/:sessionId" element={<LiveWorkbenchPage />} />
          <Route path="/talent/live/sessions/:sessionId" element={<LiveWorkbenchPage />} />
        </Routes>
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('live session entry pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(douyinApi.accountList).mockResolvedValue({
      total: 1,
      list: [{ id: 7, accountName: '直播账号', accountId: 'dy-7' }],
      pageNum: 0,
      pageSize: 200,
    } as never)
    vi.mocked(douyinApi.personaList).mockResolvedValue([
      { id: 9, personaName: '专业主播' },
    ] as never)
    vi.mocked(liveApi.sessionGet).mockResolvedValue({
      id: 18,
      liveTitle: '测试直播',
      accountId: 7,
      personaId: 9,
      scriptStyle: 'educational',
      status: 1,
    } as never)
    vi.mocked(liveApi.sessionSave).mockResolvedValue(18 as never)
    vi.mocked(liveApi.sessionReadiness).mockResolvedValue({ score: 72 } as never)
    vi.mocked(liveApi.sessionOverview).mockResolvedValue({ gmv: 1000, orderCount: 3, scriptCount: 4, productCount: 2, readiness: 72 } as never)
    vi.mocked(liveApi.sessionClone).mockRejectedValue(new Error('clone failed') as never)
    vi.mocked(liveApi.sessionExportToShortVideo).mockRejectedValue(new Error('export failed') as never)
    vi.mocked(liveApi.productBySession).mockResolvedValue([] as never)
    vi.mocked(liveApi.scriptBySession).mockResolvedValue([] as never)
  })

  it('creates a live session through real session save payload', async () => {
    renderForm()

    expect(screen.getByRole('heading', { name: '新建直播场次' })).toBeInTheDocument()
    expect(screen.getByTestId('live-session-form-workbench')).toHaveAttribute('data-contract-scope', 'live-session-create')
    expect(screen.getByTestId('live-session-form-workbench')).toHaveAttribute('data-route-scope', 'admin')
    expect(screen.getByTestId('live-session-form-workbench')).toHaveAttribute('data-ready-endpoints', '/douyin/account/search|/douyin/persona/list|/live/session/get|/live/session/save')
    expect(screen.getByTestId('live-session-form-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shortvideo-project-create'))
    expect(screen.getByTestId('live-session-form-contract-alert')).toHaveAttribute('data-no-product-selection', 'true')
    expect(screen.getByTestId('live-session-form-contract-alert')).toHaveAttribute('data-no-script-generation', 'true')
    expect(screen.getByTestId('live-session-form-contract-alert')).toHaveAttribute('data-no-shortvideo-project-create', 'true')
    expect(screen.getByText(/写入 `\/live\/session\/save`/)).toBeInTheDocument()
    await waitFor(() => expect(douyinApi.accountList).toHaveBeenCalled())

    fireEvent.change(screen.getByLabelText(/直播标题/), { target: { value: '新品专场' } })
    fireEvent.mouseDown(screen.getByLabelText(/抖音账号/))
    fireEvent.click(await screen.findByRole('option', { name: '直播账号' }))
    fireEvent.mouseDown(screen.getByLabelText(/主播人设/))
    fireEvent.click(await screen.findByRole('option', { name: '专业主播' }))
    fireEvent.change(screen.getByLabelText(/直播描述/), { target: { value: '今晚重点讲解新品' } })
    fireEvent.click(screen.getByRole('button', { name: '创建' }))

    await waitFor(() => {
      expect(liveApi.sessionSave).toHaveBeenCalledWith(expect.objectContaining({
        liveTitle: '新品专场',
        accountId: 7,
        personaId: 9,
        scriptStyle: 'conversational',
        liveDescription: '今晚重点讲解新品',
      }))
    })
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/live/sessions'))
  })

  it('shows dependency load errors on form page', async () => {
    vi.mocked(douyinApi.accountList).mockRejectedValueOnce(new Error('account down') as never)

    renderForm()

    expect(await screen.findByTestId('live-session-form-dependency-error')).toHaveTextContent('依赖加载异常')
    expect(screen.getByTestId('live-session-form-dependency-error')).toHaveAttribute('data-save-disabled', 'true')
    expect(screen.getByText(/\/douyin\/account\/search 抖音账号=account down/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '创建' })).toBeDisabled()
  })

  it('keeps form save errors visible with endpoint source', async () => {
    vi.mocked(liveApi.sessionSave).mockRejectedValueOnce(new Error('save failed') as never)

    renderForm()

    await waitFor(() => expect(douyinApi.accountList).toHaveBeenCalled())
    fireEvent.change(screen.getByLabelText(/直播标题/), { target: { value: '新品专场' } })
    fireEvent.mouseDown(screen.getByLabelText(/抖音账号/))
    fireEvent.click(await screen.findByRole('option', { name: '直播账号' }))
    fireEvent.click(screen.getByRole('button', { name: '创建' }))

    expect(await screen.findByTestId('live-session-form-save-error')).toHaveTextContent('/live/session/save 创建场次失败：save failed')
    expect(screen.getByTestId('live-session-form-save-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getByDisplayValue('新品专场')).toBeInTheDocument()
  })

  it('keeps organization form navigation inside organization shell', async () => {
    renderForm('/org/live/sessions/create')

    await waitFor(() => expect(douyinApi.accountList).toHaveBeenCalled())
    expect(screen.getByTestId('live-session-form-workbench')).toHaveAttribute('data-route-scope', 'org')
    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(navigate).toHaveBeenLastCalledWith('/org/live/sessions')

    navigate.mockClear()
    fireEvent.change(screen.getByLabelText(/直播标题/), { target: { value: '机构专场' } })
    fireEvent.mouseDown(screen.getByLabelText(/抖音账号/))
    fireEvent.click(await screen.findByRole('option', { name: '直播账号' }))
    fireEvent.click(screen.getByRole('button', { name: '创建' }))

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/org/live/sessions')
    })
  })

  it('keeps live workbench clone and export errors visible', async () => {
    renderWorkbench()

    expect(await screen.findByText('工作台主体 #18')).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-contract-scope', 'live-workbench-entry-shell')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-ready-endpoints', '/live/session/get|/live/session/readiness|/live/session/overview|/live/session/clone|/live/session/export-to-short-video')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-context-endpoints', expect.stringContaining('/live/product/by-session'))
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-session-header-fallback'))
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-route-scope', 'admin')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-realtime-path', 'unsupported')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-shortvideo-target-template', expect.stringContaining('/shortvideo/workbench?projectId=:id'))
    expect(screen.getByTestId('live-workbench-header-surface')).toHaveAttribute('data-contract-source', '/live/session/get|/live/session/readiness|/live/session/overview')
    expect(screen.getByTestId('live-workbench-realtime-button')).toHaveAttribute('data-contract-status', 'unsupported')

    fireEvent.click(screen.getByLabelText('导出为短视频项目'))
    expect(await screen.findByTestId('live-workbench-export-dialog')).toHaveAttribute('data-contract-source', '/live/session/export-to-short-video')
    fireEvent.click(await screen.findByRole('button', { name: '确认导出' }))
    expect(await screen.findByTestId('live-workbench-export-error')).toHaveTextContent('/live/session/export-to-short-video 导出短视频项目失败：export failed')
    expect(screen.getByTestId('live-workbench-export-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('live-workbench-export-error')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    expect(screen.getByTestId('live-workbench-operation-alert')).toHaveAttribute('data-contract-sources', '/live/session/export-to-short-video')
    expect(screen.getByTestId('live-workbench-export-dialog')).toHaveAttribute('data-input-retained', 'true')
    fireEvent.click(screen.getByRole('button', { name: '取消' }))

    fireEvent.click(screen.getByLabelText('克隆场次'))
    expect(await screen.findByTestId('live-workbench-clone-dialog')).toHaveAttribute('data-contract-source', '/live/session/clone')
    expect(screen.getByTestId('live-workbench-clone-dialog')).toHaveAttribute('data-target-path-template', expect.stringContaining('/live/sessions/:id'))
    fireEvent.change(await screen.findByLabelText('新场次名称'), { target: { value: '复盘复制场' } })
    fireEvent.click(await screen.findByRole('button', { name: '确认克隆' }))
    expect(await screen.findByTestId('live-workbench-clone-error')).toHaveTextContent('/live/session/clone 克隆场次失败：clone failed')
    expect(screen.getByTestId('live-workbench-clone-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('live-workbench-clone-error')).toHaveAttribute('data-no-local-session-clone', 'true')
    expect(screen.getByTestId('live-workbench-operation-alert')).toHaveAttribute('data-contract-sources', '/live/session/clone')
    expect(liveApi.sessionClone).toHaveBeenCalledWith({ id: 18, newTitle: '复盘复制场' })
  })

  it('keeps live workbench header dependency errors explicit without local fallback', async () => {
    vi.mocked(liveApi.sessionGet).mockRejectedValueOnce(new Error('session down') as never)
    vi.mocked(liveApi.sessionReadiness).mockRejectedValueOnce(new Error('readiness down') as never)
    vi.mocked(liveApi.sessionOverview).mockRejectedValueOnce(new Error('overview down') as never)

    renderWorkbench()

    const alert = await screen.findByTestId('live-workbench-operation-alert')
    expect(alert).toHaveAttribute('data-contract-sources', '/live/session/get|/live/session/readiness|/live/session/overview')
    expect(alert).toHaveAttribute('data-no-local-session-header-fallback', 'true')
    expect(alert).toHaveAttribute('data-no-local-readiness-fallback', 'true')
    expect(alert).toHaveAttribute('data-no-local-overview-fallback', 'true')
    expect(alert).toHaveTextContent('场次头信息加载失败：session down')
    expect(alert).toHaveTextContent('就绪度不可用：readiness down')
    expect(alert).toHaveTextContent('总览指标不可用：overview down')
  })

  it('keeps org workbench navigation inside organization shell', async () => {
    vi.mocked(liveApi.sessionClone).mockResolvedValueOnce(28 as never)

    renderWorkbench('/org/live/sessions/18')

    expect(await screen.findByText('机构端直播场次')).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-route-scope', 'org')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-list-path', '/org/live/sessions')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-realtime-path', expect.stringContaining('/org/live/sessions/18/realtime'))
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-shortvideo-target-template', 'unsupported')
    fireEvent.click(screen.getByLabelText('克隆场次'))
    fireEvent.change(await screen.findByLabelText('新场次名称'), { target: { value: '机构复制场' } })
    fireEvent.click(await screen.findByRole('button', { name: '确认克隆' }))

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/org/live/sessions/28')
    })
    expect(liveApi.sessionClone).toHaveBeenCalledWith({ id: 18, newTitle: '机构复制场' })
  })

  it('keeps talent workbench export inside talent shortvideo shell', async () => {
    vi.mocked(liveApi.sessionExportToShortVideo).mockResolvedValueOnce({ scriptId: 7, projectId: 66 } as never)

    renderWorkbench('/talent/live/sessions/18')

    expect(await screen.findByText('达人端直播场次')).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-route-scope', 'talent')
    expect(screen.getByTestId('live-workbench-shell')).toHaveAttribute('data-shortvideo-target-template', '/talent/shortvideo?projectId=:id')
    fireEvent.click(screen.getByLabelText('导出为短视频项目'))
    fireEvent.click(await screen.findByRole('button', { name: '确认导出' }))

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/talent/shortvideo?projectId=66')
    })
  })

  it('shows explicit downgrade when organization shell exports shortvideo project', async () => {
    vi.mocked(liveApi.sessionExportToShortVideo).mockResolvedValueOnce({ scriptId: 7, projectId: 66 } as never)

    renderWorkbench('/org/live/sessions/18')

    expect(await screen.findByText('机构端直播场次')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('导出为短视频项目'))
    expect(await screen.findByTestId('live-workbench-export-dialog')).toHaveAttribute('data-shortvideo-target-template', 'unsupported')
    fireEvent.click(await screen.findByRole('button', { name: '确认导出' }))

    expect(await screen.findByText(/\/live\/session\/export-to-short-video 导出成功，但组织端暂未开放短视频项目工作台/)).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-operation-alert')).toHaveAttribute('data-no-admin-route-leak', 'true')
    expect(navigate).not.toHaveBeenCalledWith(expect.stringContaining('/admin/shortvideo'))
  })

  it('maps status 2 to ended in workbench header', async () => {
    vi.mocked(liveApi.sessionGet).mockResolvedValueOnce({
      id: 18,
      liveTitle: '已结束场次',
      accountId: 7,
      personaId: 9,
      scriptStyle: 'educational',
      status: 2,
    } as never)

    renderWorkbench('/admin/live/workbench/18')

    expect(await screen.findByText('已结束场次')).toBeInTheDocument()
    expect(screen.getByText('已结束')).toBeInTheDocument()
    expect(screen.queryByText('直播中')).not.toBeInTheDocument()
  })

  it('uses theme-aware status and readiness colors in dark workbench header', async () => {
    vi.mocked(liveApi.sessionReadiness).mockResolvedValueOnce({ score: 92 } as never)

    renderDarkWorkbench('/admin/live/workbench/18')

    expect(await screen.findByText('测试直播')).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-status-chip-surface')).toHaveAttribute('data-status-tone', 'error')
    expect(screen.getByTestId('live-workbench-readiness-progress-surface')).toHaveAttribute('data-readiness-tone', 'success')
    expect(screen.getByTestId('live-workbench-readiness-label')).toHaveTextContent('92%')

    for (const legacy of ['#f44336', '#607d8b', '#9e9e9e', '#4caf50', '#ff9800']) {
      expect(document.body.textContent).not.toContain(legacy)
    }
  })

  it('maps ended and draft workbench header tones without legacy colors', async () => {
    vi.mocked(liveApi.sessionGet).mockResolvedValueOnce({
      id: 18,
      liveTitle: '已结束场次',
      accountId: 7,
      personaId: 9,
      scriptStyle: 'educational',
      status: 2,
    } as never)
    vi.mocked(liveApi.sessionReadiness).mockResolvedValueOnce({ score: 72 } as never)

    const ended = renderDarkWorkbench('/admin/live/workbench/18')

    expect(await screen.findByText('已结束场次')).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-status-chip-surface')).toHaveAttribute('data-status-tone', 'text')
    expect(screen.getByTestId('live-workbench-readiness-progress-surface')).toHaveAttribute('data-readiness-tone', 'warning')
    ended.unmount()
    window.localStorage.clear()

    vi.mocked(liveApi.sessionGet).mockResolvedValueOnce({
      id: 18,
      liveTitle: '草稿场次',
      accountId: 7,
      personaId: 9,
      scriptStyle: 'educational',
      status: 0,
    } as never)
    vi.mocked(liveApi.sessionReadiness).mockResolvedValueOnce({ score: 42 } as never)

    renderDarkWorkbench('/admin/live/workbench/18')

    expect(await screen.findByText('草稿场次')).toBeInTheDocument()
    expect(screen.getByTestId('live-workbench-status-chip-surface')).toHaveAttribute('data-status-tone', 'warning')
    expect(screen.getByTestId('live-workbench-readiness-progress-surface')).toHaveAttribute('data-readiness-tone', 'error')

    for (const legacy of ['#f44336', '#607d8b', '#9e9e9e', '#4caf50', '#ff9800']) {
      expect(document.body.textContent).not.toContain(legacy)
    }
  })

})
