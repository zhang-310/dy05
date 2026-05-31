import { useState, useCallback, useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import { useToast } from '@/contexts/ToastContext'
import { ConfirmDialog, ErrorAlert, PageHeader } from '@/components/base'
import {
  exportProductToShortVideo,
  generateMultiStyleScriptsSse,
  previewStyles,
  productApi,
  type DyProduct,
  type ProductScript,
  type StylePreview,
} from '@/api/product'
import { scriptApi } from '@/api/script'
import { ScriptGeneratePanel, ScriptStyleList, type ScriptGenStep } from '@/components/product/script-manage'
import { StylePreviewDialog } from '@/components/product/script-manage/StylePreviewDialog'
import type { ScriptGeneratePanelProps } from '@/components/product/script-manage'
import { copyToClipboard } from '@/components/product/script-manage/types'
import { ALL_SCRIPT_CATEGORIES } from '@/constants/scriptCategories'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { ensureProductScriptOptimizationVersion, type EnsureOptimizationVersionResult } from '@/api/product-script-version'
import {
  analyzeProductScriptVersion,
  getProductScriptOptimizationSuggestions,
  regenerateProductScriptBySuggestion,
} from '@/api/optimization'
import type {
  OptimizationSuggestionVO,
  RegeneratedScriptVO,
  ScriptAnalysisResultVO,
} from '@/types/optimization'

interface StylePresetItem {
  presetCode: string
  presetName: string
  category: string
}

const CORE_PRESETS: readonly StylePresetItem[] = [
  { presetCode: 'professional', presetName: '专业', category: 'core' },
  { presetCode: 'warm', presetName: '温暖', category: 'core' },
  { presetCode: 'enthusiastic', presetName: '热情', category: 'core' },
  { presetCode: 'casual', presetName: '随意', category: 'core' },
]

const EXTENDED_PRESETS: readonly StylePresetItem[] = [
  { presetCode: 'friendly', presetName: '亲和', category: 'extended' },
  { presetCode: 'passionate', presetName: '激情', category: 'extended' },
  { presetCode: 'elegant', presetName: '优雅', category: 'extended' },
  { presetCode: 'trendy', presetName: '时尚', category: 'extended' },
]

const SV_STYLES = ['种草', '测评', '成分解析', '对比实测']
const SV_DURATIONS = [30, 60, 90, 120]
const PRODUCT_SCRIPT_READY_ENDPOINTS = [
  '/product/get',
  '/product/script/search',
  '/product/script/activate/{scriptId}',
  '/product/script/{scriptId}',
  '/product/script/generate-multi-sse',
  '/product/script/preview-styles',
  '/product/script/export-to-shortvideo',
] as const
const PRODUCT_SCRIPT_CONTEXT_ENDPOINTS = [
  '/product/style-preset/recommend',
  '/script/categories',
  '/product/script-version/ensure-optimization-version',
  '/product/script/analyze',
  '/product/script/suggestions',
  '/product/script/regenerate',
  '/product/script-version/list-by-product',
] as const
const PRODUCT_SCRIPT_UNSUPPORTED_ACTIONS = [
  'static-script-fallback',
  'local-activation-toggle',
  'local-delete-on-error',
  'local-shortvideo-project',
  'auto-preview-fallback-generation',
  'version-rollback-from-main-page',
] as const

interface ExportDialogState {
  open: boolean
  script: ProductScript | null
  style: string
  duration: number
  loading: boolean
}

interface RefineDialogState {
  open: boolean
  script: ProductScript | null
  loading: boolean
  error: string
  ensureResult: EnsureOptimizationVersionResult | null
  analysis: ScriptAnalysisResultVO | null
  suggestions: OptimizationSuggestionVO[]
  variants: RegeneratedScriptVO[]
}

function isScriptActive(script: ProductScript) {
  return script.isActive === true || script.status === 1
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}

function formatScore(value: unknown) {
  const n = Number(value ?? 0)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '-'
}

function priorityColor(priority?: string): 'error' | 'warning' | 'info' | 'default' {
  const normalized = String(priority || '').toUpperCase()
  if (normalized === 'CRITICAL' || normalized === 'HIGH') return 'error'
  if (normalized === 'MEDIUM') return 'warning'
  if (normalized === 'LOW') return 'info'
  return 'default'
}

export default function ProductScriptManagePage() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const [product, setProduct] = useState<DyProduct | null>(null)
  const [scripts, setScripts] = useState<ProductScript[]>([])
  const [scriptType, setScriptType] = useState('formal')
  const [loading, setLoading] = useState(false)
  const [productLoading, setProductLoading] = useState(false)
  const [productError, setProductError] = useState('')
  const [scriptError, setScriptError] = useState('')
  const [categoryError, setCategoryError] = useState('')
  const [actionError, setActionError] = useState('')
  const [exportError, setExportError] = useState('')
  const [previewError, setPreviewError] = useState('')
  const [recommendedStyles, setRecommendedStyles] = useState<string[]>([])
  const [scriptCategories, setScriptCategories] = useState<string[]>([])

  const [expandedStyles, setExpandedStyles] = useState<Record<string, boolean>>({})
  const [expandedScript, setExpandedScript] = useState<number | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ProductScript | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)

  const [genStyles, setGenStyles] = useState<string[]>([])
  const [genPersonaId, setGenPersonaId] = useState<number | ''>('')
  const [genScene, setGenScene] = useState('')
  const [genDuration, setGenDuration] = useState(60)
  const [fusionMode, setFusionMode] = useState(false)
  const [fusionStrategy, setFusionStrategy] = useState('blended')
  const [styleWeights, setStyleWeights] = useState<Record<string, number>>({})
  const [useKbRef, setUseKbRef] = useState(true)
  const [showExtended, setShowExtended] = useState(false)
  const [selectedKbCategories, setSelectedKbCategories] = useState<string[]>([])

  const [steps, setSteps] = useState<ScriptGenStep[]>([])
  const [isGenerating, setIsGenerating] = useState(false)
  const [doneCount, setDoneCount] = useState(0)
  const [failCount, setFailCount] = useState(0)
  const [abExperimentId, setAbExperimentId] = useState<number | null>(null)
  const [abVariantId, setAbVariantId] = useState<number | null>(null)
  const [generateError, setGenerateError] = useState('')

  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previews, setPreviews] = useState<StylePreview[]>([])

  const [exportDialog, setExportDialog] = useState<ExportDialogState>({
    open: false,
    script: null,
    style: '种草',
    duration: 60,
    loading: false,
  })
  const [refineDialog, setRefineDialog] = useState<RefineDialogState>({
    open: false,
    script: null,
    loading: false,
    error: '',
    ensureResult: null,
    analysis: null,
    suggestions: [],
    variants: [],
  })

  const currentProductId = Number(productId)

  const loadProduct = useCallback(async () => {
    if (!productId) return
    setProductLoading(true)
    setProductError('')
    setCategoryError('')
    try {
      const p = await productApi.get(Number(productId))
      setProduct(p)
      try {
        const recommended = await productApi.stylePresetRecommend(Number(productId))
        setRecommendedStyles(Array.isArray(recommended) ? recommended : [])
      } catch (error) {
        setRecommendedStyles([])
        setCategoryError(getErrorMessage(error, '/product/style-preset/recommend 不可用，已跳过推荐风格。'))
      }
      try {
        const dynamicCategories = await scriptApi.categories()
        const allCategories = Array.from(new Set([...ALL_SCRIPT_CATEGORIES, ...(dynamicCategories || [])]))
        setScriptCategories(allCategories)
      } catch (error) {
        setScriptCategories([...ALL_SCRIPT_CATEGORIES])
        setCategoryError(getErrorMessage(error, '话术分类接口不可用，已降级为内置分类。'))
      }
    } catch (error) {
      setProduct(null)
      setProductError(getErrorMessage(error, '加载商品失败，请检查 /product/get。'))
    } finally {
      setProductLoading(false)
    }
  }, [productId])

  const loadScripts = useCallback(async () => {
    if (!productId) return
    setLoading(true)
    setScriptError('')
    try {
      const res = await productApi.scriptList({ productId: Number(productId), rows: 100 })
      setScripts(Array.isArray(res.list) ? res.list : [])
    } catch (error) {
      setScripts([])
      setScriptError(getErrorMessage(error, '加载话术失败，请检查 /product/script/search。'))
    } finally {
      setLoading(false)
    }
  }, [productId])

  const refreshAll = useCallback(() => {
    void loadProduct()
    void loadScripts()
  }, [loadProduct, loadScripts])

  useEffect(() => {
    refreshAll()
  }, [refreshAll])

  const { byStyle, activeByStyle } = useMemo(() => {
    const groups: Record<string, ProductScript[]> = {}
    const active: Record<string, ProductScript> = {}
    scripts.filter(s => s.scriptType === scriptType).forEach(script => {
      const style = script.style || 'default'
      if (!groups[style]) groups[style] = []
      groups[style].push(script)
      if (isScriptActive(script)) active[style] = script
    })
    return { byStyle: groups, activeByStyle: active }
  }, [scripts, scriptType])

  const nameMap = useMemo(() => {
    const map: Record<string, string> = {}
    ;[...CORE_PRESETS, ...EXTENDED_PRESETS].forEach(p => {
      map[p.presetCode] = p.presetName
    })
    return map
  }, [])

  const activeCount = Object.keys(activeByStyle).length
  const styleCount = Object.keys(byStyle).length
  const hasAnyScript = scripts.length > 0
  const currentTypeScriptCount = scripts.filter(s => s.scriptType === scriptType).length

  const handleToggleStyle = useCallback((style: string) => {
    setGenStyles(prev => prev.includes(style) ? prev.filter(s => s !== style) : [...prev, style])
  }, [])

  const handleActivate = useCallback(async (script: ProductScript) => {
    setActionError('')
    try {
      await productApi.scriptActivate(script.id)
      toast('已激活', 'success')
      void loadScripts()
    } catch (error) {
      const message = `/product/script/activate/${script.id}：${getErrorMessage(error, '激活失败')}`
      setActionError(message)
      toast(message, 'error')
    }
  }, [toast, loadScripts])

  const handleDeleteConfirm = useCallback(async () => {
    if (!deleteTarget) return
    setDeleteLoading(true)
    setActionError('')
    try {
      await productApi.scriptDelete(deleteTarget.id)
      toast('已删除', 'success')
      setDeleteTarget(null)
      void loadScripts()
    } catch (error) {
      const message = `/product/script/${deleteTarget.id}：${getErrorMessage(error, '删除失败')}`
      setActionError(message)
      toast(message, 'error')
      setDeleteTarget(null)
    } finally {
      setDeleteLoading(false)
    }
  }, [deleteTarget, toast, loadScripts])

  const handleStart = useCallback(() => {
    if (!productId || genStyles.length === 0) return
    setGenerateError('')
    setActionError('')

    const initialSteps: ScriptGenStep[] = genStyles.map(style => {
      const preset = [...CORE_PRESETS, ...EXTENDED_PRESETS].find(p => p.presetCode === style)
      return {
        style,
        styleName: preset?.presetName || style,
        status: 'pending' as const,
      }
    })

    setSteps(initialSteps)
    setIsGenerating(true)
    setDoneCount(0)
    setFailCount(0)
    setAbExperimentId(null)
    setAbVariantId(null)

    generateMultiStyleScriptsSse(
      {
        productId: Number(productId),
        styles: genStyles,
        scriptType,
        fusionMode,
        styleWeights: fusionMode && Object.keys(styleWeights).length > 0 ? styleWeights : undefined,
        fusionStrategy: fusionMode ? fusionStrategy : undefined,
        personaId: genPersonaId === '' ? undefined : genPersonaId,
        duration: genDuration,
        scene: genScene || undefined,
        useKbRef,
        kbCategories: selectedKbCategories,
      },
      {
        onProgress: (event) => {
          setSteps(prev => prev.map(step =>
            step.style === event.style
              ? { ...step, status: event.status, message: event.message }
              : step
          ))
          if (event.status === 'done') {
            setDoneCount(prev => prev + 1)
          } else if (event.status === 'failed') {
            setFailCount(prev => prev + 1)
          }
          if (event.abExperimentId && !abExperimentId) {
            setAbExperimentId(event.abExperimentId)
            setAbVariantId(event.abVariantId || null)
          }
        },
        onDone: () => {
          setIsGenerating(false)
          toast('话术生成完成', 'success')
          void loadScripts()
        },
        onError: (err) => {
          setIsGenerating(false)
          const message = `/product/script/generate-multi-sse：${err.message || 'SSE error'}`
          setGenerateError(message)
          toast(message, 'error')
        },
      }
    )
  }, [productId, genStyles, scriptType, fusionMode, styleWeights, fusionStrategy, genPersonaId, genDuration, genScene, useKbRef, selectedKbCategories, toast, loadScripts, abExperimentId])

  const handleCancel = useCallback(() => {
    setIsGenerating(false)
    toast('已取消生成', 'info')
  }, [toast])

  const handleRetry = useCallback(() => {
    const failedStyles = steps.filter(s => s.status === 'failed').map(s => s.style)
    setGenStyles(failedStyles)
    handleStart()
  }, [steps, handleStart])

  const handleReset = useCallback(() => {
    setSteps([])
    setDoneCount(0)
    setFailCount(0)
    setGenerateError('')
  }, [])

  const handlePreview = useCallback(async () => {
    if (!productId || genStyles.length === 0) return

    setPreviewOpen(true)
    setPreviewLoading(true)
    setPreviews([])
    setPreviewError('')

    try {
      const result = await previewStyles({
        productId: Number(productId),
        styles: genStyles,
        scriptType,
        personaId: genPersonaId === '' ? undefined : genPersonaId,
        scene: genScene || undefined,
        useKbRef,
        kbCategories: selectedKbCategories,
      })
      setPreviews(result.previews)
    } catch (error) {
      const message = `/product/script/preview-styles：${getErrorMessage(error, '预览生成失败')}`
      setPreviewError(message)
      toast(message, 'error')
      setPreviewOpen(false)
    } finally {
      setPreviewLoading(false)
    }
  }, [productId, genStyles, scriptType, genPersonaId, genScene, useKbRef, selectedKbCategories, toast])

  const openExportDialog = useCallback((script?: ProductScript | null) => {
    const selected = script ?? scripts.find(isScriptActive) ?? null
    setExportDialog({
      open: true,
      script: selected,
      style: selected?.style || '种草',
      duration: selected?.duration || genDuration || 60,
      loading: false,
    })
  }, [scripts, genDuration])

  const handleExport = useCallback(async () => {
    if (!productId) return
    setExportDialog(prev => ({ ...prev, loading: true }))
    setExportError('')
    try {
      const result = await exportProductToShortVideo({
        productId: Number(productId),
        style: exportDialog.style,
        duration: exportDialog.duration,
      })
      toast('已导出短视频项目', 'success')
      setExportDialog(prev => ({ ...prev, open: false, loading: false }))
      navigate(`${shortvideoRoutes.workbench}?projectId=${result.projectId}`)
    } catch (error) {
      const message = `/product/script/export-to-shortvideo：${getErrorMessage(error, '导出失败，请确认该商品有激活话术或可用话术版本。')}`
      setExportError(message)
      toast(message, 'error')
      setExportDialog(prev => ({ ...prev, loading: false }))
    }
  }, [productId, exportDialog.style, exportDialog.duration, toast, navigate])

  const handleRefine = useCallback(async (script: ProductScript) => {
    setRefineDialog({
      open: true,
      script,
      loading: true,
      error: '',
      ensureResult: null,
      analysis: null,
      suggestions: [],
      variants: [],
    })

    try {
      const ensureResult = await ensureProductScriptOptimizationVersion(script.id)
      const analysis = await analyzeProductScriptVersion(ensureResult.scriptVersionId)
      const analysisResultId = analysis.id
      const suggestions = analysisResultId
        ? await getProductScriptOptimizationSuggestions(ensureResult.scriptVersionId, analysisResultId, 5)
        : []
      const topSuggestion = suggestions[0]
      const regen = topSuggestion?.id
        ? await regenerateProductScriptBySuggestion(ensureResult.scriptVersionId, topSuggestion.id, ['FRIENDLY', 'HUMOROUS', 'PREMIUM'])
        : { variants: [] }

      setRefineDialog(prev => ({
        ...prev,
        loading: false,
        ensureResult,
        analysis,
        suggestions: Array.isArray(suggestions) ? suggestions : [],
        variants: Array.isArray(regen.variants) ? regen.variants : [],
      }))
      toast(ensureResult.created ? '已创建优化版本并生成精修候选' : '已复用优化版本并生成精修候选', 'success')
    } catch (error) {
      setRefineDialog(prev => ({
        ...prev,
        loading: false,
        error: getErrorMessage(error, 'AI 精修链路失败，请检查 /product/script-version/ensure-optimization-version 与 /product/script/analyze。'),
      }))
    }
  }, [toast])

  const generatePanelProps: ScriptGeneratePanelProps = {
    personas: [],
    genPersonaId,
    setGenPersonaId,
    genScene,
    setGenScene,
    corePresets: CORE_PRESETS,
    extendedPresets: EXTENDED_PRESETS,
    genStyles,
    recommendedStyleCodes: recommendedStyles,
    showExtended,
    setShowExtended,
    onToggleStyle: handleToggleStyle,
    fusionMode,
    setFusionMode,
    fusionStrategy,
    setFusionStrategy,
    styleWeights,
    setStyleWeights,
    genDuration,
    setGenDuration,
    useKbRef,
    setUseKbRef,
    scriptCategories,
    selectedKbCategories,
    setSelectedKbCategories,
    onStart: handleStart,
    onPreview: handlePreview,
    steps,
    isGenerating,
    doneCount,
    failCount,
    hasSteps: steps.length > 0,
    onCancel: handleCancel,
    onRetry: handleRetry,
    onReset: handleReset,
    abExperimentId,
    abVariantId,
  }

  return (
    <Box
      data-testid="product-script-workbench"
      data-contract-scope="product-script-management"
      data-product-id={productId ?? ''}
      data-ready-endpoints={PRODUCT_SCRIPT_READY_ENDPOINTS.join('|')}
      data-context-endpoints={PRODUCT_SCRIPT_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={PRODUCT_SCRIPT_UNSUPPORTED_ACTIONS.join('|')}
      data-script-type={scriptType}
      data-total-scripts={scripts.length}
      data-current-type-scripts={currentTypeScriptCount}
      data-style-count={styleCount}
      data-active-style-count={activeCount}
      data-recommended-style-count={recommendedStyles.length}
      data-category-count={scriptCategories.length}
      data-generation-step-count={steps.length}
      data-generation-done-count={doneCount}
      data-generation-fail-count={failCount}
      data-no-static-script-fallback="true"
      sx={{ p: 2, minHeight: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title={product?.productName || '商品话术管理'}
        subtitle={`商品 ID: ${productId ?? '-'} · 真实接口 /product/script/search 返回数组，激活/删除使用 PUT/DELETE 路径。`}
        breadcrumbs={[{ label: '商品' }, { label: '商品话术' }]}
        actions={(
          <>
            <Button size="small" variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate('/org/product/list')}>
              返回商品
            </Button>
            <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={refreshAll} disabled={loading || productLoading}>
              刷新
            </Button>
            <Button size="small" variant="contained" startIcon={<VideoLibraryIcon />} onClick={() => openExportDialog()} disabled={!hasAnyScript}>
              导出短视频
            </Button>
          </>
        )}
      />

      <Grid container spacing={2}>
        {[
          { label: '话术总数', value: scripts.length, hint: '来自 /product/script/search' },
          { label: '当前类型风格', value: styleCount, hint: scriptType },
          { label: '激活风格', value: activeCount, hint: '按 isActive/status 兼容判断' },
          { label: '知识库分类', value: scriptCategories.length, hint: categoryError ? '已降级' : '可用于生成参考' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper
              variant="outlined"
              data-testid="product-script-kpi-card"
              data-contract-source={item.label === '知识库分类' ? '/script/categories' : '/product/script/search'}
              sx={{ p: 1.5, height: '100%' }}
            >
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Alert
        severity="info"
        data-testid="product-script-contract-alert"
        data-list-source="/product/script/search"
        data-generation-source="/product/script/generate-multi-sse"
        data-export-source="/product/script/export-to-shortvideo"
        data-no-local-shortvideo-project="true"
        data-no-static-script-fallback="true"
        data-no-version-rollback-from-main-page="true"
      >
        话术生成走 `/product/script/generate-multi-sse`；短视频导出走 `/product/script/export-to-shortvideo`，后端会优先使用激活话术，没有激活话术时使用最近有效话术。
      </Alert>

      {productError && (
        <Box data-testid="product-script-product-error" data-contract-source="/product/get" data-no-local-product-fallback="true">
          <ErrorAlert title="商品加载失败" message={productError} onRetry={loadProduct} />
        </Box>
      )}
      {scriptError && (
        <Box data-testid="product-script-list-error" data-contract-source="/product/script/search" data-no-static-script-fallback="true">
          <ErrorAlert title="话术加载失败" message={scriptError} onRetry={loadScripts} />
        </Box>
      )}
      {categoryError && (
        <Box data-testid="product-script-category-error" data-contract-source="/product/style-preset/recommend|/script/categories" data-fallback-scope="manual-style-and-built-in-categories">
          <ErrorAlert severity="warning" title="分类/推荐降级" message={categoryError} onRetry={loadProduct} />
        </Box>
      )}
      {actionError && (
        <Box data-testid="product-script-action-error" data-contract-source="/product/script/activate/{scriptId}|/product/script/{scriptId}" data-no-local-state-change="true">
          <ErrorAlert severity="warning" title="话术操作失败" message={`${actionError}。失败时不会本地切换激活状态或移除话术。`} onRetry={loadScripts} />
        </Box>
      )}
      {exportError && (
        <Box data-testid="product-script-export-error-page" data-contract-source="/product/script/export-to-shortvideo" data-no-local-shortvideo-project="true">
          <ErrorAlert severity="warning" title="短视频导出失败" message={`${exportError}。导出失败不会创建本地假项目。`} />
        </Box>
      )}
      {previewError && (
        <Box data-testid="product-script-preview-error" data-contract-source="/product/script/preview-styles" data-no-auto-generation-fallback="true">
          <ErrorAlert severity="warning" title="预览生成失败" message={`${previewError}。预览失败不会自动启动完整生成。`} />
        </Box>
      )}
      {generateError && (
        <Box data-testid="product-script-generate-error" data-contract-source="/product/script/generate-multi-sse" data-no-static-script-fallback="true">
          <ErrorAlert severity="warning" title="生成链路异常" message={generateError} />
        </Box>
      )}

      <Grid container spacing={2} sx={{ flex: 1, minHeight: 560 }}>
        <Grid item xs={12} lg={7} sx={{ minHeight: 520 }}>
          <Paper
            variant="outlined"
            data-testid="product-script-list-panel"
            data-contract-source="/product/script/search"
            data-script-type={scriptType}
            data-row-count={currentTypeScriptCount}
            data-active-style-count={activeCount}
            sx={{ p: 0, height: '100%', overflow: 'hidden' }}
          >
            <ScriptStyleList
              scriptType={scriptType}
              onScriptTypeChange={setScriptType}
              byStyle={byStyle}
              activeByStyle={activeByStyle}
              loading={loading}
              expandedStyles={expandedStyles}
              onToggleStyleExpand={(style) => setExpandedStyles(prev => ({ ...prev, [style]: !prev[style] }))}
              expandedScript={expandedScript}
              onToggleScriptExpand={setExpandedScript}
              nameMap={nameMap}
              totalScripts={scripts.length}
              activeCount={activeCount}
              styleCount={styleCount}
              onActivate={handleActivate}
              onDelete={setDeleteTarget}
              onHistory={(script) => navigate(`/org/product/${productId}/script-versions?scriptId=${script.id}`)}
              onExportShortVideo={openExportDialog}
              onRefine={handleRefine}
              onCopy={(s) => { copyToClipboard(s.scriptContent); toast('已复制', 'success') }}
            />
          </Paper>
        </Grid>

        <Grid item xs={12} lg={5} sx={{ minHeight: 520 }}>
          <Paper
            variant="outlined"
            data-testid="product-script-generate-panel"
            data-contract-source="/product/script/generate-multi-sse"
            data-preview-source="/product/script/preview-styles"
            data-selected-style-count={genStyles.length}
            data-use-kb-ref={String(useKbRef)}
            data-no-static-script-fallback="true"
            sx={{ p: 2.5, height: '100%', overflow: 'auto' }}
          >
            <ScriptGeneratePanel {...generatePanelProps} />
          </Paper>
        </Grid>
      </Grid>

      <StylePreviewDialog
        open={previewOpen}
        onClose={() => setPreviewOpen(false)}
        previews={previews}
        loading={previewLoading}
        onConfirm={() => { setPreviewOpen(false); handleStart() }}
        onAdjust={() => setPreviewOpen(false)}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        title="删除商品话术"
        content={`确定删除${deleteTarget?.style ? `「${nameMap[deleteTarget.style] ?? deleteTarget.style}」` : '该'}话术？删除会调用 /product/script/{scriptId}，不可直接恢复。`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        loading={deleteLoading}
      />

      <Dialog open={exportDialog.open} onClose={() => !exportDialog.loading && setExportDialog(prev => ({ ...prev, open: false }))} maxWidth="xs" fullWidth>
        <DialogTitle>导出为短视频项目</DialogTitle>
        <DialogContent
          dividers
          data-testid="product-script-export-dialog"
          data-contract-source="/product/script/export-to-shortvideo"
          data-product-id={productId ?? ''}
          data-selected-script-id={exportDialog.script?.id ?? ''}
          data-no-local-shortvideo-project="true"
        >
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            {exportDialog.script ? (
              <Alert severity="info">
                将使用商品 {currentProductId} 的当前可用话术导出；如需指定 ProductScriptVersion，请在版本页选择版本导出。
              </Alert>
            ) : (
              <Alert severity="warning">
                当前没有激活话术，后端会尝试选择最近一条有效话术；若没有有效话术会返回业务错误。
              </Alert>
            )}
            {exportError ? (
              <Alert
                severity="error"
                data-testid="product-script-export-error-dialog"
                data-contract-source="/product/script/export-to-shortvideo"
                data-dialog-input-preserved="true"
              >
                {exportError}。请确认后端能解析激活话术或指定版本。
              </Alert>
            ) : null}
            <TextField select label="短视频风格" value={exportDialog.style} onChange={e => setExportDialog(prev => ({ ...prev, style: e.target.value }))} size="small" fullWidth>
              {SV_STYLES.map(style => <MenuItem key={style} value={style}>{style}</MenuItem>)}
            </TextField>
            <TextField select label="目标时长" value={exportDialog.duration} onChange={e => setExportDialog(prev => ({ ...prev, duration: Number(e.target.value) }))} size="small" fullWidth>
              {SV_DURATIONS.map(duration => <MenuItem key={duration} value={duration}>{duration}秒</MenuItem>)}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExportDialog(prev => ({ ...prev, open: false }))} disabled={exportDialog.loading}>取消</Button>
          <Button variant="contained" onClick={handleExport} disabled={exportDialog.loading}>
            {exportDialog.loading ? <CircularProgress size={16} sx={{ mr: 1 }} /> : null}
            确认导出
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={refineDialog.open}
        onClose={() => !refineDialog.loading && setRefineDialog(prev => ({ ...prev, open: false }))}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>AI 精修结果</DialogTitle>
        <DialogContent
          dividers
          data-testid="product-script-refine-dialog"
          data-contract-source="/product/script-version/ensure-optimization-version|/product/script/analyze|/product/script/suggestions|/product/script/regenerate"
          data-script-id={refineDialog.script?.id ?? ''}
          data-version-id={refineDialog.ensureResult?.scriptVersionId ?? ''}
          data-suggestion-count={refineDialog.suggestions.length}
          data-variant-count={refineDialog.variants.length}
        >
          <Stack spacing={2}>
            <Alert severity="info">
              精修链路会先把 dy_product_script 主话术镜像为 ProductScriptVersion，再调用 `/product/script/analyze`、`/product/script/suggestions` 和 `/product/script/regenerate`。
            </Alert>

            {refineDialog.script && (
              <Paper variant="outlined" sx={{ p: 1.5 }}>
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                    <Typography variant="subtitle2" fontWeight={700}>原始话术</Typography>
                    <Chip size="small" label={`scriptId=${refineDialog.script.id}`} />
                    <Chip size="small" label={nameMap[refineDialog.script.style] ?? refineDialog.script.style ?? 'default'} variant="outlined" />
                  </Stack>
                  <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                    {refineDialog.script.scriptContent}
                  </Typography>
                </Stack>
              </Paper>
            )}

            {refineDialog.loading && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 2 }}>
                <CircularProgress size={20} />
                <Typography variant="body2">正在创建优化版本、分析弱点并生成精修候选...</Typography>
              </Box>
            )}

            {refineDialog.error && (
              <Alert
                severity="error"
                data-testid="product-script-refine-error"
                data-contract-source="/product/script-version/ensure-optimization-version|/product/script/analyze|/product/script/suggestions|/product/script/regenerate"
                data-input-preserved="true"
              >
                {refineDialog.error}
              </Alert>
            )}

            {refineDialog.ensureResult && (
              <Grid container spacing={1.5}>
                <Grid item xs={12} md={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, height: '100%' }}>
                    <Typography variant="caption" color="text.secondary">优化版本</Typography>
                    <Typography variant="h6" fontWeight={700}>{refineDialog.ensureResult.scriptVersionId}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {refineDialog.ensureResult.created ? '本次新建镜像' : '复用已有镜像'}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, height: '100%' }}>
                    <Typography variant="caption" color="text.secondary">综合评分</Typography>
                    <Typography variant="h6" fontWeight={700}>{formatScore(refineDialog.analysis?.overallScore)}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {refineDialog.analysis?.styleProfile?.dominantStyle ?? '未识别风格'}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, height: '100%' }}>
                    <Typography variant="caption" color="text.secondary">候选版本</Typography>
                    <Typography variant="h6" fontWeight={700}>{refineDialog.variants.length}</Typography>
                    <Typography variant="caption" color="text.secondary">来自最高优先级建议</Typography>
                  </Paper>
                </Grid>
              </Grid>
            )}

            {refineDialog.analysis?.weakPoints && refineDialog.analysis.weakPoints.length > 0 && (
              <Stack spacing={1}>
                <Typography variant="subtitle2" fontWeight={700}>分析弱点</Typography>
                {refineDialog.analysis.weakPoints.map((weakPoint, index) => (
                  <Alert key={`${weakPoint.type ?? 'weak'}-${index}`} severity={String(weakPoint.severity || '').toUpperCase() === 'HIGH' ? 'warning' : 'info'}>
                    {weakPoint.description || weakPoint.type || '后端返回未命名弱点'}
                  </Alert>
                ))}
              </Stack>
            )}

            {refineDialog.suggestions.length > 0 && (
              <Stack spacing={1}>
                <Typography variant="subtitle2" fontWeight={700}>优化建议</Typography>
                {refineDialog.suggestions.map(suggestion => (
                  <Paper key={suggestion.id} variant="outlined" sx={{ p: 1.5 }}>
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.75 }}>
                      <Chip size="small" label={suggestion.priority || 'UNKNOWN'} color={priorityColor(suggestion.priority)} />
                      <Chip size="small" label={suggestion.category || 'CONTENT'} variant="outlined" />
                      <Typography variant="caption" color="text.secondary">suggestionId={suggestion.id}</Typography>
                    </Stack>
                    <Typography variant="body2">{suggestion.suggestionContent || suggestion.description || '无建议内容'}</Typography>
                  </Paper>
                ))}
              </Stack>
            )}

            {refineDialog.variants.length > 0 && (
              <Stack spacing={1}>
                <Typography variant="subtitle2" fontWeight={700}>精修候选</Typography>
                {refineDialog.variants.map(variant => (
                  <Paper key={variant.id} variant="outlined" sx={{ p: 1.5 }}>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
                      <Chip size="small" label={variant.generationStyle || 'DEFAULT'} color="primary" variant="outlined" />
                      <Chip size="small" label={`质量 ${formatScore(variant.aiQualityScore)}`} />
                      <Chip size="small" label={variant.approvalStatus || 'PENDING'} variant="outlined" />
                      <Typography variant="caption" color="text.secondary">regeneratedId={variant.id}</Typography>
                    </Stack>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{variant.regeneratedContent}</Typography>
                  </Paper>
                ))}
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          {refineDialog.ensureResult && (
            <Button onClick={() => navigate(`/org/product/${productId}/script-versions?scriptId=${refineDialog.script?.id ?? ''}`)}>
              查看版本页
            </Button>
          )}
          <Button onClick={() => setRefineDialog(prev => ({ ...prev, open: false }))} disabled={refineDialog.loading}>
            关闭
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
