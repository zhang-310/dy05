import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import CreativeStudioPage from '../CreativeStudioPage'
import DigitalHumanPage from '../DigitalHumanPage'
import ModelBenchmarkPage from '../ModelBenchmarkPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    mediaImageHistory: vi.fn(),
    text2img: vi.fn(),
    mediaTtsVoices: vi.fn(),
    tts: vi.fn(),
    videoGenerateFromFrames: vi.fn(),
    digitalHumanStatus: vi.fn(),
    digitalHumanGenerate: vi.fn(),
    taskModelConfigList: vi.fn(),
    modelBenchmarkComparison: vi.fn(),
    modelBenchmarkBestModel: vi.fn(),
    modelBenchmarkRecord: vi.fn(),
  },
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

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, actionSlot, searchSlot }: any) => (
      <div>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
        {rows.length === 0 ? <div>暂无数据</div> : null}
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

function renderPage(ui: React.ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('AI tool pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.mediaImageHistory).mockResolvedValue([
      { id: 1, imageUrl: 'https://cdn.test/image.png', prompt: '护肤产品海报' },
    ] as never)
    vi.mocked(aiApi.text2img).mockResolvedValue({
      imageUrl: 'https://cdn.test/generated.png',
      generationTime: 1200,
    } as never)
    vi.mocked(aiApi.mediaTtsVoices).mockResolvedValue([
      { id: 'voice-a', name: '女声 A', language: 'zh-CN' },
    ] as never)
    vi.mocked(aiApi.tts).mockResolvedValue({
      audioUrl: 'https://cdn.test/audio.mp3',
      duration: 2300,
    } as never)
    vi.mocked(aiApi.videoGenerateFromFrames).mockResolvedValue({
      videoUrl: 'https://cdn.test/video.mp4',
      format: 'mp4',
      duration: 5,
    } as never)
    vi.mocked(aiApi.digitalHumanStatus).mockResolvedValue({
      available: true,
      provider: 'heygen',
    } as never)
    vi.mocked(aiApi.digitalHumanGenerate).mockResolvedValue({
      videoUrl: 'https://cdn.test/talking-head.mp4',
      provider: 'heygen',
      success: true,
      message: '生成成功',
    } as never)
    vi.mocked(aiApi.taskModelConfigList).mockResolvedValue([
      {
        id: 1,
        taskCode: 'script_gen',
        taskName: '话术生成',
        taskGroup: 'ai',
        maxRetries: 1,
        sortOrder: 1,
        status: 1,
      },
      {
        id: 2,
        taskCode: 'agent_chat',
        taskName: '智能体对话',
        taskGroup: 'ai',
        maxRetries: 1,
        sortOrder: 2,
        status: 1,
      },
    ] as never)
    vi.mocked(aiApi.modelBenchmarkComparison).mockResolvedValue([
      {
        modelId: 9,
        modelName: 'Ollama Qwen',
        taskCode: 'script_gen',
        avgLatencyMs: 800,
        successRate: 0.92,
        avgTokens: 300,
        totalCalls: 12,
      },
    ] as never)
    vi.mocked(aiApi.modelBenchmarkBestModel).mockResolvedValue({
      modelId: 9,
      modelName: 'Ollama Qwen',
      taskCode: 'script_gen',
      priority: 'latency',
    } as never)
    vi.mocked(aiApi.modelBenchmarkRecord).mockResolvedValue(undefined as never)
  })

  it('CreativeStudioPage uses media contracts and renders image generation errors inline', async () => {
    vi.mocked(aiApi.text2img).mockRejectedValueOnce(new Error('image provider missing'))
    renderPage(<CreativeStudioPage />)

    expect(screen.getByRole('heading', { name: '创意工坊' })).toBeInTheDocument()
    expect(screen.getByText(/真实接口边界/)).toBeInTheDocument()
    expect(screen.getAllByText(/首尾帧生成/).length).toBeGreaterThan(0)
    expect(screen.getByTestId('creative-studio-page')).toHaveAttribute('data-no-local-media-fallback', 'true')
    expect(screen.getByTestId('creative-studio-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/ai/media/video/text2video'),
    )
    expect(screen.getByTestId('creative-studio-boundary-contract')).toHaveAttribute('data-no-text-to-video-synthesis', 'true')
    expect(screen.getByTestId('creative-studio-image-contract')).toHaveAttribute('data-source-endpoint', '/ai/media/image/text2img')

    fireEvent.change(screen.getByLabelText('描述词 Prompt'), { target: { value: '护肤产品海报' } })
    fireEvent.click(screen.getByRole('button', { name: '生成图片' }))

    await waitFor(() => {
      expect(aiApi.text2img).toHaveBeenCalledWith({
        prompt: '护肤产品海报',
        style: 'realistic',
      })
    })
    expect(await screen.findByText(/图片生成失败（POST \/ai\/media\/image\/text2img）：image provider missing/)).toBeInTheDocument()
    expect(screen.getByTestId('creative-studio-image-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('creative-studio-image-error')).toHaveAttribute('data-no-local-image-fallback', 'true')
    expect(screen.getByDisplayValue('护肤产品海报')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '最近生成记录' }))
    await waitFor(() => {
      expect(aiApi.mediaImageHistory).toHaveBeenCalledWith({ page: 0, size: 10 })
    })
    expect(screen.getByTestId('creative-studio-image-history')).toHaveAttribute('data-no-local-history-fallback', 'true')
    expect((await screen.findAllByText('护肤产品海报')).length).toBeGreaterThan(1)
  })

  it('CreativeStudioPage normalizes wrapped media list responses', async () => {
    vi.mocked(aiApi.mediaImageHistory).mockResolvedValueOnce({
      records: [{ id: 2, imageUrl: 'https://cdn.test/wrapped.png', prompt: '包装历史记录' }],
      total: 1,
    } as never)
    vi.mocked(aiApi.mediaTtsVoices).mockResolvedValueOnce({
      list: [{ id: 'voice-b', name: '女声 B', language: 'zh-CN' }],
    } as never)

    renderPage(<CreativeStudioPage />)

    fireEvent.click(screen.getByRole('button', { name: '最近生成记录' }))
    expect(await screen.findByText('包装历史记录')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: /语音合成/ }))
    expect(await screen.findByLabelText('音色')).toBeInTheDocument()
    expect(screen.getByTestId('creative-studio-tts-contract')).toHaveAttribute('data-manual-voice-is-server-param', 'true')
  })

  it('CreativeStudioPage keeps inputs and endpoint diagnostics for history, TTS and frame-video failures', async () => {
    vi.mocked(aiApi.mediaImageHistory).mockRejectedValueOnce(new Error('history table locked') as never)
    vi.mocked(aiApi.mediaTtsVoices).mockRejectedValueOnce(new Error('voices provider missing') as never)
    vi.mocked(aiApi.tts).mockRejectedValueOnce(new Error('tts quota exhausted') as never)
    vi.mocked(aiApi.videoGenerateFromFrames).mockRejectedValueOnce(new Error('frame url unreachable') as never)

    renderPage(<CreativeStudioPage />)

    fireEvent.click(screen.getByRole('button', { name: '最近生成记录' }))
    expect(await screen.findByText(/历史记录加载失败（POST \/ai\/media\/image\/history）：history table locked/)).toBeInTheDocument()
    expect(screen.getByTestId('creative-studio-image-history-error')).toHaveAttribute('data-no-local-history-fallback', 'true')

    fireEvent.click(screen.getByRole('tab', { name: /语音合成/ }))
    expect(await screen.findByText(/音色列表加载失败（POST \/ai\/media\/tts\/voices）：voices provider missing/)).toBeInTheDocument()
    expect(screen.getByTestId('creative-studio-tts-voices-error')).toHaveAttribute('data-no-local-voice-list-fallback', 'true')
    fireEvent.change(screen.getByLabelText('文本内容'), { target: { value: '这是一段口播脚本' } })
    fireEvent.change(screen.getByLabelText('音色 voice（可选）'), { target: { value: 'voice-x' } })
    fireEvent.click(screen.getByRole('button', { name: '合成语音' }))
    expect(await screen.findByText(/语音合成失败（POST \/ai\/media\/tts\/generate）：tts quota exhausted/)).toBeInTheDocument()
    expect(screen.getByTestId('creative-studio-tts-error')).toHaveAttribute('data-no-local-tts-fallback', 'true')
    expect(screen.getByDisplayValue('这是一段口播脚本')).toBeInTheDocument()
    expect(screen.getByDisplayValue('voice-x')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: /首尾帧视频/ }))
    expect(screen.getByTestId('creative-studio-video-contract')).toHaveAttribute('data-no-text-to-video-synthesis', 'true')
    fireEvent.change(screen.getByLabelText('首帧图片 URL'), { target: { value: 'https://cdn.test/start.png' } })
    fireEvent.change(screen.getByLabelText('尾帧图片 URL'), { target: { value: 'https://cdn.test/end.png' } })
    fireEvent.change(screen.getByLabelText('时长（秒）'), { target: { value: '8' } })
    fireEvent.click(screen.getByRole('button', { name: '生成过渡视频' }))
    expect(await screen.findByText(/视频生成失败（POST \/ai\/media\/video\/generate-from-frames）：frame url unreachable/)).toBeInTheDocument()
    expect(screen.getByTestId('creative-studio-video-error')).toHaveAttribute('data-no-local-video-fallback', 'true')
    expect(screen.getByDisplayValue('https://cdn.test/start.png')).toBeInTheDocument()
    expect(screen.getByDisplayValue('https://cdn.test/end.png')).toBeInTheDocument()
  })

  it('DigitalHumanPage gates generation by status and renders real provider result', async () => {
    renderPage(<DigitalHumanPage />)

    expect(screen.getByRole('heading', { name: '数字人' })).toBeInTheDocument()
    expect(screen.getByTestId('digital-human-page')).toHaveAttribute('data-no-local-video-fallback', 'true')
    expect(screen.getByTestId('digital-human-boundary-contract')).toHaveAttribute('data-no-shortvideo-async-task', 'true')
    expect(screen.getByText(/\/ai\/digital-human\/status/)).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.digitalHumanStatus).toHaveBeenCalled()
    })
    expect(await screen.findByText('heygen')).toBeInTheDocument()
    expect(screen.getByTestId('digital-human-status-contract')).toHaveAttribute('data-source-endpoint', '/ai/digital-human/status')

    fireEvent.change(screen.getByLabelText('播报脚本 scriptText'), {
      target: { value: '今天讲清楚精华液使用顺序。' },
    })
    fireEvent.click(screen.getByRole('button', { name: '提交生成' }))

    await waitFor(() => {
      expect(aiApi.digitalHumanGenerate).toHaveBeenCalledWith({
        scriptText: '今天讲清楚精华液使用顺序。',
        voiceId: 'zh-CN-XiaoxiaoNeural',
        avatarId: 'default',
      })
    })
    expect(await screen.findByText(/heygen · 生成成功/)).toBeInTheDocument()
    expect(screen.getByTestId('digital-human-result-contract')).toHaveAttribute('data-no-local-video-fallback', 'true')
  })

  it('DigitalHumanPage shows unavailable provider downgrade', async () => {
    vi.mocked(aiApi.digitalHumanStatus).mockResolvedValueOnce({
      available: false,
      provider: 'none',
    } as never)

    renderPage(<DigitalHumanPage />)

    expect(await screen.findByText('未配置或不可用')).toBeInTheDocument()
    expect(screen.getByTestId('digital-human-status-contract')).toHaveAttribute('data-available', 'false')
    expect(screen.getByText(/服务未配置（available=false）/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '提交生成' })).toBeDisabled()
  })

  it('DigitalHumanPage keeps form values and endpoint diagnostics when status or generation fails', async () => {
    vi.mocked(aiApi.digitalHumanStatus).mockRejectedValueOnce(new Error('status timeout') as never)

    renderPage(<DigitalHumanPage />)

    expect(await screen.findByText(/数字人服务状态加载失败（POST \/ai\/digital-human\/status）：status timeout/)).toBeInTheDocument()
    expect(screen.getByTestId('digital-human-status-error')).toHaveAttribute('data-no-mock-provider-fallback', 'true')

    vi.mocked(aiApi.digitalHumanStatus).mockResolvedValueOnce({
      available: true,
      provider: 'heygen',
    } as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(await screen.findByText('heygen')).toBeInTheDocument()

    vi.mocked(aiApi.digitalHumanGenerate).mockRejectedValueOnce(new Error('generate denied') as never)
    fireEvent.change(screen.getByLabelText('voiceId（音色）'), { target: { value: 'voice-ops' } })
    fireEvent.change(screen.getByLabelText('avatarId（形象）'), { target: { value: 'avatar-ops' } })
    fireEvent.change(screen.getByLabelText('播报脚本 scriptText'), {
      target: { value: '请讲解爆款短视频开场。' },
    })
    fireEvent.click(screen.getByRole('button', { name: '提交生成' }))

    expect(await screen.findByText(/数字人生成失败（POST \/ai\/digital-human\/generate）：generate denied/)).toBeInTheDocument()
    expect(screen.getByTestId('digital-human-generate-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByDisplayValue('voice-ops')).toBeInTheDocument()
    expect(screen.getByDisplayValue('avatar-ops')).toBeInTheDocument()
    expect(screen.getByDisplayValue('请讲解爆款短视频开场。')).toBeInTheDocument()
  })

  it('ModelBenchmarkPage renders diagnostics, recommendation and records benchmark sample', async () => {
    renderPage(<ModelBenchmarkPage />)

    expect(screen.getByRole('heading', { name: '模型基准测试' })).toBeInTheDocument()
    expect(screen.getAllByText(/ai_model_benchmark/).length).toBeGreaterThan(0)

    await waitFor(() => {
      expect(aiApi.taskModelConfigList).toHaveBeenCalled()
      expect(aiApi.modelBenchmarkComparison).toHaveBeenCalledWith({ taskCode: undefined })
    })
    expect(await screen.findByText('Ollama Qwen')).toBeInTheDocument()
    expect(screen.getByText('样本总量')).toBeInTheDocument()
    expect(screen.getAllByText('12').length).toBeGreaterThan(0)
    expect(screen.getByText('基准覆盖诊断')).toBeInTheDocument()
    expect(screen.getByText('配置无样本 1')).toBeInTheDocument()
    expect(screen.getByText(/智能体对话 \(agent_chat\)/)).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByTestId('model-benchmark-task-filter').querySelector('[role="combobox"]')!)
    fireEvent.click(await screen.findByRole('option', { name: '话术生成 (script_gen)' }))

    await waitFor(() => {
      expect(aiApi.modelBenchmarkBestModel).toHaveBeenCalledWith('script_gen', 'latency')
    })
    expect(await screen.findByText(/当前任务推荐模型/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '录入样本' }))
    fireEvent.change(screen.getByLabelText('模型 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '提交' }))

    await waitFor(() => {
      expect(aiApi.modelBenchmarkRecord).toHaveBeenCalled()
    })
    expect(vi.mocked(aiApi.modelBenchmarkRecord).mock.calls[0][0]).toEqual({
        modelId: 9,
        taskCode: 'script_gen',
        latencyMs: 800,
        tokensUsed: 120,
        success: true,
      })
  })

  it('ModelBenchmarkPage documents empty benchmark downgrade instead of mock rows', async () => {
    vi.mocked(aiApi.modelBenchmarkComparison).mockResolvedValueOnce([] as never)

    renderPage(<ModelBenchmarkPage />)

    await waitFor(() => {
      expect(aiApi.modelBenchmarkComparison).toHaveBeenCalled()
    })
    expect(await screen.findByText(/暂无模型基准样本/)).toBeInTheDocument()
    expect(screen.getByText(/不会用静态 mock 填充排行/)).toBeInTheDocument()
  })

  it('ModelBenchmarkPage surfaces benchmark rows without task config and low-quality rows', async () => {
    vi.mocked(aiApi.modelBenchmarkComparison).mockResolvedValueOnce([
      {
        modelId: 8,
        modelName: 'Slow Model',
        taskCode: 'unknown_task',
        avgLatencyMs: 4200,
        successRate: 0.55,
        avgTokens: 900,
        totalCalls: 4,
      },
    ] as never)

    renderPage(<ModelBenchmarkPage />)

    expect(await screen.findByText('基准覆盖诊断')).toBeInTheDocument()
    expect(screen.getByText('样本无配置 1')).toBeInTheDocument()
    expect(screen.getByText('低质样本 1')).toBeInTheDocument()
    expect(screen.getAllByText(/unknown_task/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/Slow Model\/unknown_task/).length).toBeGreaterThan(0)
  })

  it('ModelBenchmarkPage keeps record form open and values when record endpoint fails', async () => {
    vi.mocked(aiApi.modelBenchmarkRecord).mockRejectedValueOnce(new Error('modelId 不能为空') as never)

    renderPage(<ModelBenchmarkPage />)

    await screen.findByText('基准覆盖诊断')
    fireEvent.click(screen.getByRole('button', { name: '录入样本' }))
    fireEvent.change(screen.getByLabelText('模型 ID'), { target: { value: '9' } })
    fireEvent.change(screen.getByLabelText('任务代码'), { target: { value: 'script_gen' } })
    fireEvent.change(screen.getByLabelText('延迟 ms'), { target: { value: '1300' } })
    fireEvent.click(screen.getByRole('button', { name: '提交' }))

    expect(await screen.findByText(/基准样本录入失败：modelId 不能为空/)).toBeInTheDocument()
    expect(screen.getByText(/\/ai\/model-benchmark\/record/)).toBeInTheDocument()
    expect(screen.getByText(/录入面板和输入值会保留/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('9')).toBeInTheDocument()
    expect(screen.getByDisplayValue('script_gen')).toBeInTheDocument()
    expect(screen.getByDisplayValue('1300')).toBeInTheDocument()
  })
})
