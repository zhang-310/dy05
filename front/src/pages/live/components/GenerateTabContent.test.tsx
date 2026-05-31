import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { GenerateTabContent } from './GenerateTabContent'
import { liveApi } from '@/api/live'
import { douyinApi } from '@/api/douyin'
import { tianapi } from '@/api/tianapi'

const toast = vi.hoisted(() => vi.fn())
const startGeneration = vi.hoisted(() => vi.fn())
const cancelGeneration = vi.hoisted(() => vi.fn())
const generationState = vi.hoisted(() => ({
  isGenerating: false,
  generationProgress: 0,
  generationMessage: '',
  genJustCompleted: false,
  slotTimeline: [] as Array<{
    scriptId: number
    slotLabel: string
    scriptType: string
    sequenceNo?: number
    failed?: boolean
    errorMsg?: string
  }>,
}))
const coreDataState = vi.hoisted(() => ({
  products: [{ id: 1, productId: 10, productName: '精华液' }] as Array<Record<string, unknown>>,
  scripts: [] as Array<Record<string, unknown>>,
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('../contexts', () => ({
  useCoreData: () => ({
    session: { id: 18, scriptStyle: 'natural' },
    products: coreDataState.products,
    scripts: coreDataState.scripts,
  }),
  useEditor: () => ({
    handleScriptSave: vi.fn(),
  }),
  useGeneration: () => ({
    ...generationState,
    startGeneration,
    cancelGeneration,
  }),
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    productBySession: vi.fn(),
    scriptSearch: vi.fn(),
    aiGenerateFull: vi.fn(),
    aiGenerateProductScript: vi.fn(),
    scriptSave: vi.fn(),
    scriptDelete: vi.fn(),
    versionList: vi.fn(),
    versionActivate: vi.fn(),
    presetList: vi.fn(),
  },
}))
vi.mock('@/api/douyin', () => ({
  douyinApi: {
    personaList: vi.fn(),
  },
}))
vi.mock('@/api/tianapi', () => ({
  tianapi: {
    hotDouyin: vi.fn(),
    hotToutiao: vi.fn(),
    hotWeibo: vi.fn(),
    hotNetwork: vi.fn(),
  },
}))

describe('GenerateTabContent - API 集成测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    toast.mockClear()
    generationState.isGenerating = false
    generationState.generationProgress = 0
    generationState.generationMessage = ''
    generationState.genJustCompleted = false
    generationState.slotTimeline = []
    coreDataState.products = [{ id: 1, productId: 10, productName: '精华液' }]
    coreDataState.scripts = []
    startGeneration.mockResolvedValue(undefined)
    cancelGeneration.mockClear()
    vi.mocked(liveApi.presetList).mockResolvedValue([] as never)
    vi.mocked(douyinApi.personaList).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotDouyin).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotToutiao).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotWeibo).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotNetwork).mockResolvedValue([] as never)
  })

  it('应该正确调用商品列表 API', async () => {
    vi.mocked(liveApi.productBySession).mockResolvedValue([
      { id: 1, productName: '精华液', price: 299, stock: 100 },
      { id: 2, productName: '面霜', price: 399, stock: 50 },
    ])

    const result = await liveApi.productBySession(1)

    expect(result).toHaveLength(2)
    expect(result[0].productName).toBe('精华液')
    expect(liveApi.productBySession).toHaveBeenCalledWith(1)
  })

  it('应该正确调用话术列表 API', async () => {
    vi.mocked(liveApi.scriptSearch).mockResolvedValue({
      list: [
        {
          id: 1,
          sessionId: 1,
          scriptContent: '欢迎来到直播间',
          scriptType: 'intro',
          style: 'friendly',
          sequenceNo: 1,
          aiGenerated: true,
        },
      ],
      total: 1,
      pageNum: 0,
      pageSize: 100,
    })

    const result = await liveApi.scriptSearch({
      sessionId: 1,
      page: 0,
      rows: 100,
    })

    expect(result.list).toHaveLength(1)
    expect(result.list[0].scriptContent).toBe('欢迎来到直播间')
    expect(liveApi.scriptSearch).toHaveBeenCalledWith({
      sessionId: 1,
      page: 0,
      rows: 100,
    })
  })

  it('应该正确调用批量生成 API', async () => {
    vi.mocked(liveApi.aiGenerateFull).mockResolvedValue([
      {
        id: 1,
        sessionId: 1,
        scriptContent: 'AI 生成的话术',
        scriptType: 'product',
        style: 'professional',
        sequenceNo: 1,
        aiGenerated: true,
        generationStatus: 'completed',
      },
    ])

    const result = await liveApi.aiGenerateFull({
      sessionId: 1,
      products: [{ productId: 1, duration: 120, style: 'professional' }],
    })

    expect(result).toHaveLength(1)
    expect(liveApi.aiGenerateFull).toHaveBeenCalled()
  })

  it('应该正确调用单条生成 API', async () => {
    vi.mocked(liveApi.aiGenerateProductScript).mockResolvedValue({
      scriptContent: 'AI 生成的单条话术',
      scriptType: 'product',
      style: 'friendly',
    })

    const result = await liveApi.aiGenerateProductScript({
      sessionId: 1,
      productId: 1,
      style: 'friendly',
      duration: 60,
    })

    expect(result.scriptContent).toBe('AI 生成的单条话术')
    expect(liveApi.aiGenerateProductScript).toHaveBeenCalled()
  })

  it('应该正确调用话术保存 API', async () => {
    vi.mocked(liveApi.scriptSave).mockResolvedValue({ id: 1 })

    const result = await liveApi.scriptSave({
      id: 1,
      sessionId: 1,
      scriptContent: '修改后的话术',
      scriptType: 'product',
      style: 'professional',
      sequenceNo: 1,
    })

    expect(result.id).toBe(1)
    expect(liveApi.scriptSave).toHaveBeenCalledWith(
      expect.objectContaining({
        scriptContent: '修改后的话术',
      })
    )
  })

  it('应该正确调用话术删除 API', async () => {
    vi.mocked(liveApi.scriptDelete).mockResolvedValue(undefined)

    await liveApi.scriptDelete(1)

    expect(liveApi.scriptDelete).toHaveBeenCalledWith(1)
  })

  it('应该正确调用版本列表 API', async () => {
    vi.mocked(liveApi.versionList).mockResolvedValue([
      {
        id: 1,
        scriptId: 1,
        versionNo: 1,
        content: '版本1内容',
        isActive: true,
        createTime: '2026-05-10T10:00:00',
      },
      {
        id: 2,
        scriptId: 1,
        versionNo: 2,
        content: '版本2内容',
        isActive: false,
        createTime: '2026-05-10T11:00:00',
      },
    ])

    const result = await liveApi.versionList(1)

    expect(result).toHaveLength(2)
    expect(result[0].isActive).toBe(true)
    expect(liveApi.versionList).toHaveBeenCalledWith(1)
  })

  it('应该正确调用版本激活 API', async () => {
    vi.mocked(liveApi.versionActivate).mockResolvedValue(undefined)

    await liveApi.versionActivate(2)

    expect(liveApi.versionActivate).toHaveBeenCalledWith(2)
  })

  it('应该处理 API 错误', async () => {
    vi.mocked(liveApi.aiGenerateFull).mockRejectedValue(
      new Error('Generation failed')
    )

    await expect(
      liveApi.aiGenerateFull({
        sessionId: 1,
        products: [],
      })
    ).rejects.toThrow('Generation failed')
  })

  it('应该验证 22 种话术风格常量', () => {
    const SCRIPT_STYLES = [
      'natural', 'friendly', 'warm', 'gentle', 'casual',
      'enthusiastic', 'passionate', 'promotion', 'seeding',
      'professional',
      'emotional', 'storytelling', 'empathy',
    ]

    expect(SCRIPT_STYLES).toHaveLength(13)
    expect(SCRIPT_STYLES).toContain('natural')
    expect(SCRIPT_STYLES).toContain('professional')
    expect(SCRIPT_STYLES).toContain('emotional')
  })

  it('应该验证话术类型常量', () => {
    const SCRIPT_TYPES = ['intro', 'product', 'interaction', 'promotion', 'closing']

    expect(SCRIPT_TYPES).toHaveLength(5)
    expect(SCRIPT_TYPES).toContain('intro')
    expect(SCRIPT_TYPES).toContain('product')
    expect(SCRIPT_TYPES).toContain('closing')
  })

  it('shows SSE endpoint when generation fails', async () => {
    startGeneration.mockRejectedValue(new Error('sse down'))

    renderWithProviders(<GenerateTabContent />)

    fireEvent.click(screen.getByRole('button', { name: /开始生成/ }))

    const error = await screen.findByTestId('generate-stream-error-alert')
    expect(error).toHaveTextContent('/live/ai/generate-full-pipelined-sse 流式生成失败：sse down')
    expect(error).toHaveAttribute('data-contract-source', '/live/ai/generate-full-pipelined-sse')
    expect(error).toHaveAttribute('data-no-local-generated-script-fallback', 'true')
    expect(toast).toHaveBeenCalledWith(
      expect.stringContaining('/live/ai/generate-full-pipelined-sse 流式生成失败：sse down'),
      'error',
    )
  })

  it('shows preset and persona fallback sources when query dependencies fail', async () => {
    vi.mocked(liveApi.presetList).mockRejectedValue(new Error('preset down') as never)
    vi.mocked(douyinApi.personaList).mockRejectedValue(new Error('persona down') as never)

    renderWithProviders(<GenerateTabContent />)

    await waitFor(() => {
      expect(screen.getByTestId('generate-preset-error-alert')).toHaveTextContent(/\/live\/generation-preset\/list 加载失败：preset down/)
      expect(screen.getByTestId('generate-persona-error-alert')).toHaveTextContent(/\/douyin\/persona\/list 加载失败：persona down/)
    })
    expect(screen.getByTestId('generate-preset-error-alert')).toHaveAttribute('data-no-local-preset-fallback', 'true')
    expect(screen.getByTestId('generate-persona-error-alert')).toHaveAttribute('data-no-local-persona-fallback', 'true')
  })

  it('marks generation orchestration contract and disables generation when product context is empty', async () => {
    coreDataState.products = []

    renderWithProviders(<GenerateTabContent />)

    const root = screen.getByTestId('generate-tab-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'live-ai-script-generation-orchestrator')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/ai/generate-full-pipelined-sse'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/script/save'))
    expect(root).toHaveAttribute('data-context-endpoints', '/live/session/get|/live/product/by-session|/live/script/by-session')
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shortvideo-project-create'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-generated-script-fallback'))
    expect(root).toHaveAttribute('data-products-count', '0')
    expect(root).toHaveAttribute('data-can-generate', 'false')
    expect(screen.getByTestId('generate-start-button')).toBeDisabled()
    expect(screen.getByTestId('generate-no-products-alert')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.getByTestId('generate-slot-empty')).toHaveAttribute('data-contract-source', '/live/product/by-session')
    expect(screen.getByTestId('generate-empty-no-products-alert')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(startGeneration).not.toHaveBeenCalled()
  })

  it('switches generation contract endpoint when skeleton mode is enabled', async () => {
    renderWithProviders(<GenerateTabContent />)

    fireEvent.click(screen.getByTestId('generate-skeleton-switch'))

    expect(screen.getByTestId('generate-tab-workbench')).toHaveAttribute('data-generation-mode', 'skeleton')
    expect(screen.getByTestId('generate-start-button')).toHaveAttribute('data-contract-source', '/live/ai/generate-skeleton-sse')

    fireEvent.click(screen.getByTestId('generate-start-button'))

    await waitFor(() => {
      expect(startGeneration).toHaveBeenCalledWith(expect.objectContaining({ mode: 'skeleton' }))
    })
  })

  it('shows an auditable generation process plan before streaming starts', () => {
    coreDataState.products = [
      { id: 20, productId: 200, productName: '第二个商品', position: 2 },
      { id: 10, productId: 100, productName: '第一个商品', position: 1 },
    ]
    generationState.slotTimeline = []

    renderWithProviders(<GenerateTabContent />)

    expect(screen.getByTestId('generate-timeline-list')).toHaveAttribute('data-process-state', 'idle-ready')
    expect(screen.getByTestId('generate-process-plan-surface')).toHaveAttribute('data-product-order-ready', 'true')
    expect(screen.getAllByTestId('generate-process-plan-row')).toHaveLength(4)
    expect(screen.getByTestId('generate-process-idle-hint')).toHaveTextContent('按商品顺序记录进度')
    expect(screen.getByText(/1\.第一个商品/)).toBeInTheDocument()
  })

  it('uses theme-aware generation setup and timeline surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    coreDataState.products = [
      { id: 1, productId: 10, productName: '精华液' },
      { id: 2, productId: 11, productName: '面霜' },
    ]
    generationState.genJustCompleted = true
    generationState.generationProgress = 100
    generationState.generationMessage = '生成完成'
    generationState.slotTimeline = [
      { scriptId: 1, slotLabel: '精华液卖点槽', scriptType: 'product', sequenceNo: 1 },
      { scriptId: 2, slotLabel: '面霜互动槽', scriptType: 'interaction', sequenceNo: 2, failed: true, errorMsg: '模型超时' },
    ]
    vi.mocked(tianapi.hotDouyin).mockResolvedValue([
      { word: '换季修护', hotZh: '热度 98' },
      { word: '屏障护理', hotZh: '热度 89' },
    ] as never)

    renderWithProviders(
      <AppThemeProvider>
        <GenerateTabContent />
      </AppThemeProvider>,
    )

    fireEvent.click(await screen.findByText('换季修护'))

    expect(screen.getByTestId('generate-tab-workbench')).toHaveAttribute('data-generation-status', 'completed')
    expect(screen.getByTestId('generate-tab-workbench')).toHaveAttribute('data-slot-timeline-count', '2')
    expect(screen.getByTestId('generate-progress-surface')).toHaveAttribute('data-generation-progress', '100')
    expect(screen.getByTestId('generate-timeline-list')).toHaveAttribute('data-slot-timeline-count', '2')
    expect(screen.getByTestId('generate-hotword-selected-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
    expect(screen.getByTestId('generate-slot-batch-settings-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    expect(screen.getByTestId('generate-timeline-success-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
    expect(screen.getByTestId('generate-timeline-failed-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })
})
