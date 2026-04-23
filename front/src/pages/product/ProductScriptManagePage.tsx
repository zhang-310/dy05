import { useState, useCallback, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Box, Typography, IconButton, Paper, Grid } from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useToast } from '@/contexts/ToastContext'
import { productApi, generateMultiStyleScriptsSse, previewStyles, type DyProduct, type ProductScript, type StylePreview } from '@/api/product'
import { scriptApi } from '@/api/script'
import { ScriptStyleList, ScriptGeneratePanel, type ScriptGenStep } from '@/components/product/script-manage'
import { StylePreviewDialog } from '@/components/product/script-manage/StylePreviewDialog'
import type { ScriptGeneratePanelProps } from '@/components/product/script-manage'
import { copyToClipboard } from '@/components/product/script-manage/types'
import { ALL_SCRIPT_CATEGORIES } from '@/constants/scriptCategories'

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

export default function ProductScriptManagePage() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const [product, setProduct] = useState<DyProduct | null>(null)
  const [scripts, setScripts] = useState<ProductScript[]>([])
  const [scriptType, setScriptType] = useState('formal')
  const [loading, setLoading] = useState(false)
  const [recommendedStyles, setRecommendedStyles] = useState<string[]>([])
  const [scriptCategories, setScriptCategories] = useState<string[]>([])

  // Script list state
  const [expandedStyles, setExpandedStyles] = useState<Record<string, boolean>>({})
  const [expandedScript, setExpandedScript] = useState<number | null>(null)

  // Generate panel state
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

  // Generation progress
  const [steps, setSteps] = useState<ScriptGenStep[]>([])
  const [isGenerating, setIsGenerating] = useState(false)
  const [doneCount, setDoneCount] = useState(0)
  const [failCount, setFailCount] = useState(0)
  const [abExperimentId, setAbExperimentId] = useState<number | null>(null)
  const [abVariantId, setAbVariantId] = useState<number | null>(null)

  // Preview dialog state
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previews, setPreviews] = useState<StylePreview[]>([])

  const loadProduct = useCallback(async () => {
    if (!productId) return
    try {
      const p = await productApi.get(Number(productId))
      setProduct(p)
      // Load recommended styles
      try {
        const recommended = await productApi.stylePresetRecommend(Number(productId))
        setRecommendedStyles(recommended || [])
      } catch {
        // Ignore recommendation errors
      }
      // Load script categories - merge predefined and dynamic categories
      try {
        const dynamicCategories = await scriptApi.categories()
        // Combine predefined categories with dynamic ones, remove duplicates
        const allCategories = Array.from(new Set([...ALL_SCRIPT_CATEGORIES, ...(dynamicCategories || [])]))
        setScriptCategories(allCategories)
      } catch {
        // Fallback to predefined categories only
        setScriptCategories([...ALL_SCRIPT_CATEGORIES])
      }
    } catch {
      toast('加载商品失败', 'error')
    }
  }, [productId, toast])

  const loadScripts = useCallback(async () => {
    if (!productId) return
    setLoading(true)
    try {
      const res = await productApi.scriptList({ productId: Number(productId), rows: 100 })
      setScripts(res.list || [])
    } catch {
      toast('加载话术失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [productId, toast])

  useEffect(() => {
    loadProduct()
    loadScripts()
  }, [loadProduct, loadScripts])

  // Group scripts by style
  const byStyle: Record<string, ProductScript[]> = {}
  const activeByStyle: Record<string, ProductScript> = {}
  scripts.filter(s => s.scriptType === scriptType).forEach(script => {
    const style = script.style || 'default'
    if (!byStyle[style]) byStyle[style] = []
    byStyle[style].push(script)
    if (script.status === 1) activeByStyle[style] = script
  })

  const nameMap: Record<string, string> = {}
  ;[...CORE_PRESETS, ...EXTENDED_PRESETS].forEach(p => {
    nameMap[p.presetCode] = p.presetName
  })

  const handleToggleStyle = useCallback((style: string) => {
    setGenStyles(prev => prev.includes(style) ? prev.filter(s => s !== style) : [...prev, style])
  }, [])

  const handleActivate = useCallback(async (script: ProductScript) => {
    try {
      await productApi.scriptActivate(script.id)
      toast('已激活', 'success')
      loadScripts()
    } catch {
      toast('激活失败', 'error')
    }
  }, [toast, loadScripts])

  const handleDelete = useCallback(async (script: ProductScript) => {
    if (!confirm('确认删除该话术？')) return
    try {
      await productApi.scriptDelete(script.id)
      toast('已删除', 'success')
      loadScripts()
    } catch {
      toast('删除失败', 'error')
    }
  }, [toast, loadScripts])

  const handleStart = useCallback(() => {
    if (!productId || genStyles.length === 0) return

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
          // Capture A/B experiment info from first event
          if (event.abExperimentId && !abExperimentId) {
            setAbExperimentId(event.abExperimentId)
            setAbVariantId(event.abVariantId || null)
          }
        },
        onDone: () => {
          setIsGenerating(false)
          toast('话术生成完成', 'success')
          loadScripts()
        },
        onError: (err) => {
          setIsGenerating(false)
          toast(`生成失败: ${err.message}`, 'error')
        },
      }
    )
  }, [productId, genStyles, scriptType, fusionMode, genPersonaId, genDuration, genScene, useKbRef, selectedKbCategories, toast, loadScripts])

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
  }, [])

  const handlePreview = useCallback(async () => {
    if (!productId || genStyles.length === 0) return

    setPreviewOpen(true)
    setPreviewLoading(true)
    setPreviews([])

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
    } catch (err) {
      toast('预览生成失败', 'error')
      setPreviewOpen(false)
    } finally {
      setPreviewLoading(false)
    }
  }, [productId, genStyles, scriptType, genPersonaId, genScene, useKbRef, selectedKbCategories, toast])

  const handlePreviewConfirm = useCallback(() => {
    setPreviewOpen(false)
    handleStart()
  }, [handleStart])

  const handlePreviewAdjust = useCallback(() => {
    setPreviewOpen(false)
  }, [])

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
    <Box sx={{ p: 3, height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: '#f5f5f5' }}>
      {/* Header */}
      <Paper sx={{ p: 2, mb: 2, borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <IconButton onClick={() => navigate('/admin/product/list')} size="small">
            <ArrowBackIcon />
          </IconButton>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h5" fontWeight={700}>
              {product?.productName || '商品话术管理'}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              商品 ID: {productId} · 共 {scripts.length} 条话术 · {Object.keys(activeByStyle).length} 个风格已激活
            </Typography>
          </Box>
        </Box>
      </Paper>

      {/* Main Content */}
      <Grid container spacing={2} sx={{ flex: 1, overflow: 'hidden' }}>
        {/* Left: Script List */}
        <Grid item xs={12} md={7} sx={{ height: '100%', overflow: 'auto' }}>
          <Paper sx={{ p: 2.5, height: '100%', borderRadius: 2 }}>
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
              activeCount={Object.keys(activeByStyle).length}
              styleCount={Object.keys(byStyle).length}
              onActivate={handleActivate}
              onDelete={handleDelete}
              onHistory={() => navigate(`/admin/product/${productId}/script-versions`)}
              onRefine={() => {}}
              onCopy={(s) => { copyToClipboard(s.scriptContent); toast('已复制', 'success') }}
            />
          </Paper>
        </Grid>

        {/* Right: Generate Panel */}
        <Grid item xs={12} md={5} sx={{ height: '100%', overflow: 'auto' }}>
          <Paper sx={{ p: 2.5, height: '100%', borderRadius: 2 }}>
            <ScriptGeneratePanel {...generatePanelProps} />
          </Paper>
        </Grid>
      </Grid>

      {/* Preview Dialog */}
      <StylePreviewDialog
        open={previewOpen}
        onClose={() => setPreviewOpen(false)}
        previews={previews}
        loading={previewLoading}
        onConfirm={handlePreviewConfirm}
        onAdjust={handlePreviewAdjust}
      />
    </Box>
  )
}
