import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ScriptPlanningPage from '../ScriptPlanningPage'
import { shortvideoApi } from '@/api/shortvideo'

vi.mock('@/api/shortvideo', () => ({
  shortvideoApi: {
    get: vi.fn(),
    save: vi.fn(),
    svScriptGet: vi.fn(),
    svScriptList: vi.fn(),
    svScriptGenerate: vi.fn(),
    svScriptSave: vi.fn(),
    svScriptDelete: vi.fn(),
    shotListGenerate: vi.fn(),
  },
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, loading }: any) => (
      <div>
        {loading && <span>loading</span>}
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({ row, value: row[col.field] })
                      : String(row[col.field] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  }
})

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

function renderPage() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/admin/shortvideo/script-planning?projectId=7']}>
      <ScriptPlanningPage />
    </MemoryRouter>,
  )
}

describe('ScriptPlanningPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(shortvideoApi.get).mockResolvedValue({
      id: 7,
      title: '屏障修护短视频',
      projectType: 'daily',
      status: 'draft',
    } as never)
    vi.mocked(shortvideoApi.save).mockResolvedValue(7 as never)
    vi.mocked(shortvideoApi.svScriptGenerate).mockResolvedValue('前三秒提出敏感肌痛点，然后讲屏障修护方案。' as never)
    vi.mocked(shortvideoApi.svScriptSave).mockResolvedValue(22 as never)
    vi.mocked(shortvideoApi.svScriptGet).mockResolvedValue({
      id: 22,
      title: '屏障修护短视频',
      content: '前三秒提出敏感肌痛点，然后讲屏障修护方案。',
      scriptType: 'daily',
      style: 'professional',
      duration: 45,
      createTime: '2026-05-20 10:00:00',
    } as never)
    vi.mocked(shortvideoApi.svScriptList).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 50,
    } as never)
    vi.mocked(shortvideoApi.shotListGenerate).mockResolvedValue({
      shotListId: 33,
      shots: [{ id: 1, shotNumber: 1, sceneDescription: '痛点开场' }],
    } as never)
  })

  it('generates, saves, associates project, and generates shot list for the next step', async () => {
    renderPage()

    expect(await screen.findByText('项目 #7 · 屏障修护短视频')).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/script/generate'),
    )
    expect(screen.getByTestId('script-planning-page')).toHaveAttribute(
      'data-ready-routes',
      expect.stringContaining('/shortvideo/shot-list?projectId=:id'),
    )
    expect(screen.getByTestId('script-planning-page')).toHaveAttribute(
      'data-supported-actions',
      expect.stringContaining('generate-shot-list'),
    )
    expect(screen.getByTestId('script-planning-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/project/local-save'),
    )
    expect(screen.getByTestId('script-planning-boundary-contract')).toHaveAttribute('data-no-local-project-mutation', 'true')
    expect(screen.getByTestId('script-planning-type-select')).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-diagnostics')).toHaveAttribute('data-no-client-link-synthesis', 'true')
    expect(screen.getByText('项目关联 #7')).toBeInTheDocument()
    expect(screen.getByText(/后端生成接口当前主要使用主题\/爆款 ID/)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('视频主题'), { target: { value: '敏感肌屏障修护' } })
    fireEvent.change(screen.getByLabelText('目标时长'), { target: { value: '45' } })
    fireEvent.change(screen.getByLabelText('商品/卖点补充'), { target: { value: '舒缓泛红，修护屏障' } })
    fireEvent.click(screen.getByTestId('script-planning-generate-button'))

    await waitFor(() => {
      expect(shortvideoApi.svScriptGenerate).toHaveBeenCalledWith({
        type: 'daily',
        theme: '敏感肌屏障修护',
        viralVideoId: undefined,
        productInfo: '舒缓泛红，修护屏障',
        style: 'professional',
        duration: 45,
      })
    })
    expect(await screen.findByDisplayValue('前三秒提出敏感肌痛点，然后讲屏障修护方案。')).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-script-editor')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText('脚本 未保存')).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-generate-button')).toHaveAttribute('data-source-endpoint', '/short-video/script/generate')

    fireEvent.click(screen.getByTestId('script-planning-save-button'))
    await waitFor(() => {
      expect(shortvideoApi.svScriptSave).toHaveBeenCalledWith(expect.objectContaining({
        title: '敏感肌屏障修护',
        content: '前三秒提出敏感肌痛点，然后讲屏障修护方案。',
        scriptType: 'daily',
        generationType: 'ai',
        style: 'professional',
        duration: 45,
      }))
      expect(shortvideoApi.save).toHaveBeenCalledWith(expect.objectContaining({
        id: 7,
        title: '屏障修护短视频',
        projectType: 'daily',
        scriptId: 22,
        status: 'draft',
      }))
    })

    expect(await screen.findByText('屏障修护短视频')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'AI 生成' }))
    expect(await screen.findByText('当前脚本 #22')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('script-planning-generate-shots-button'))
    await waitFor(() => {
      expect(shortvideoApi.shotListGenerate).toHaveBeenCalledWith({
        scriptId: 22,
        scriptContent: '前三秒提出敏感肌痛点，然后讲屏障修护方案。',
        shotCount: 6,
        style: 'professional',
      })
      expect(shortvideoApi.save).toHaveBeenLastCalledWith(expect.objectContaining({
        id: 7,
        scriptId: 22,
        shotListId: 33,
        status: 'processing',
      }))
      expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/shot-list?projectId=7'))
    })
  })

  it('switches commerce mode to digital human product detail style', async () => {
    renderPage()

    expect(await screen.findByText('项目 #7 · 屏障修护短视频')).toBeInTheDocument()
    fireEvent.mouseDown(screen.getByLabelText('生成模式'))
    fireEvent.click(screen.getByRole('option', { name: '数字人口播带货' }))
    fireEvent.change(screen.getByLabelText('视频主题'), { target: { value: '平价精华带货' } })
    fireEvent.change(screen.getByLabelText('商品/卖点补充'), { target: { value: '产品质地、包装细节、上脸演示' } })
    fireEvent.click(screen.getByRole('button', { name: '生成脚本' }))

    await waitFor(() => {
      expect(shortvideoApi.svScriptGenerate).toHaveBeenCalledWith(expect.objectContaining({
        type: 'digital_human_commerce',
        theme: '平价精华带货',
        productInfo: '产品质地、包装细节、上脸演示',
        style: 'digital_human_product_detail',
      }))
    })
  })

  it('keeps script operation failures visible with endpoint context', async () => {
    vi.mocked(shortvideoApi.svScriptGenerate).mockRejectedValueOnce(new Error('AI 模型未配置') as never)
    renderPage()

    fireEvent.change(screen.getByLabelText('视频主题'), { target: { value: '敏感肌屏障修护' } })
    fireEvent.click(screen.getByRole('button', { name: '生成脚本' }))

    expect(await screen.findByText(/脚本生成失败：AI 模型未配置/)).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-operation-error')).toHaveAttribute('data-no-local-script-mutation', 'true')
    expect(screen.getByText(/接口来源：\/short-video\/script\/generate/)).toBeInTheDocument()

    vi.mocked(shortvideoApi.svScriptGenerate).mockResolvedValueOnce('可保存但保存失败的脚本正文' as never)
    vi.mocked(shortvideoApi.svScriptSave).mockRejectedValueOnce(new Error('脚本库写入失败') as never)
    fireEvent.click(screen.getByRole('button', { name: '生成脚本' }))
    expect(await screen.findByDisplayValue('可保存但保存失败的脚本正文')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '保存并关联项目' }))

    expect(await screen.findByText(/脚本保存或项目关联失败：脚本库写入失败/)).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-operation-error')).toHaveAttribute('data-no-local-project-mutation', 'true')
    expect(screen.getAllByText(/\/short-video\/script\/save/).length).toBeGreaterThan(0)
  })

  it('shows shot-list generation failures without leaving the page', async () => {
    vi.mocked(shortvideoApi.get).mockResolvedValueOnce({
      id: 7,
      title: '屏障修护短视频',
      projectType: 'daily',
      status: 'draft',
      scriptId: 22,
    } as never)
    vi.mocked(shortvideoApi.shotListGenerate).mockRejectedValueOnce(new Error('分镜模型超时') as never)

    renderPage()

    expect(await screen.findByDisplayValue('前三秒提出敏感肌痛点，然后讲屏障修护方案。')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '生成分镜并进入下一步' }))

    expect(await screen.findByText(/分镜生成或项目回写失败：分镜模型超时/)).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-operation-error')).toHaveAttribute('data-no-local-shot-generation', 'true')
    expect(screen.getByText(/\/short-video\/shot-list\/generate/)).toBeInTheDocument()
    expect(navigate).not.toHaveBeenCalledWith(expect.stringContaining('/shortvideo/shot-list?projectId=7'))
  })

  it('shows script list endpoint failure without local scripts', async () => {
    vi.mocked(shortvideoApi.svScriptList).mockRejectedValueOnce(new Error('script list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ScriptPlanningPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('tab', { name: '脚本列表' }))

    expect(await screen.findByText(/脚本列表加载失败：script list down/)).toBeInTheDocument()
    expect(screen.getByTestId('script-planning-list-error')).toHaveAttribute('data-no-local-script-fallback', 'true')
    expect(screen.getByTestId('script-planning-list-contract')).toHaveAttribute('data-no-local-delete-mutation', 'true')
  })
})
