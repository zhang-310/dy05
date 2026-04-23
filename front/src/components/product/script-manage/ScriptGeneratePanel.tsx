import {
  Box,
  Typography,
  Button,
  Chip,
  CircularProgress,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Checkbox,
  FormControlLabel,
  Switch,
  Collapse,
  LinearProgress,
  Divider,
  Card,
  CardContent,
  alpha,
  OutlinedInput,
  ListItemText,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty'
import ReplayIcon from '@mui/icons-material/Replay'
import StarIcon from '@mui/icons-material/Star'
import MergeTypeIcon from '@mui/icons-material/MergeType'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import VisibilityIcon from '@mui/icons-material/Visibility'
import {
  DURATION_OPTIONS,
  SCENE_OPTIONS,
} from '../script-constants'
import { STYLE_WEIGHT_PRESETS, applyPresetToStyles, getApplicablePresets } from '@/constants/styleWeightPresets'
import { getApplicableFusionStrategies } from '@/constants/fusionStrategies'
import type { ScriptGeneratePanelProps, ScriptGenStep, StylePresetItem } from './types'

/* ── Style Chip (replaces checkbox list) ── */
function StyleChip({ preset, selected, recommended, onClick }: {
  preset: StylePresetItem
  selected: boolean; recommended: boolean; onClick: () => void
}) {
  return (
    <Box
      onClick={onClick}
      sx={{
        display: 'flex', alignItems: 'center', gap: 0.5,
        px: 1.5, py: 0.75, borderRadius: 1.5, cursor: 'pointer',
        border: 1, borderColor: selected ? 'primary.main' : 'divider',
        bgcolor: selected ? (t) => alpha(t.palette.primary.main, 0.06) : 'transparent',
        transition: 'all 0.15s',
        '&:hover': { borderColor: 'primary.light', bgcolor: (t) => alpha(t.palette.primary.main, 0.03) },
      }}
    >
      <Checkbox checked={selected} size="small" sx={{ p: 0, mr: 0.25 }} tabIndex={-1} />
      <Typography variant="body2" fontWeight={selected ? 600 : 400} sx={{ flex: 1 }} noWrap>
        {preset.presetName}
      </Typography>
      {recommended && <StarIcon sx={{ fontSize: 14, color: 'warning.main' }} />}
    </Box>
  )
}

/* ── Step Row ── */
function StepRow({ step }: { step: ScriptGenStep }) {
  const config = {
    pending: { icon: <HourglassEmptyIcon sx={{ fontSize: 18, color: 'text.disabled' }} />, label: '等待中', bg: 'transparent' },
    loading: { icon: <CircularProgress size={16} />, label: '生成中...', bg: 'transparent' },
    done: { icon: <CheckCircleOutlineIcon sx={{ fontSize: 18 }} color="success" />, label: '成功', bg: (t: { palette: { success: { main: string } } }) => alpha(t.palette.success.main, 0.04) },
    failed: { icon: <ErrorOutlineIcon sx={{ fontSize: 18 }} color="error" />, label: step.message || '失败', bg: (t: { palette: { error: { main: string } } }) => alpha(t.palette.error.main, 0.04) },
  }[step.status]

  return (
    <Box
      sx={{
        display: 'flex', alignItems: 'center', gap: 1.5,
        py: 0.75, px: 1.5, borderRadius: 1.5,
        bgcolor: config.bg as string,
      }}
    >
      {config.icon}
      <Typography variant="body2" fontWeight={500}>{step.styleName}</Typography>
      <Box sx={{ flex: 1 }} />
      <Typography variant="caption" color={step.status === 'failed' ? 'error' : 'text.secondary'}>
        {config.label}
      </Typography>
    </Box>
  )
}

/* ── Config Panel Props ── */
interface ConfigPanelProps {
  product: {
    personas: Record<string, unknown>[]
    genPersonaId: number | ''
    genScene: string
    genDuration: number
  }
  config: {
    corePresets: readonly StylePresetItem[]
    extendedPresets: readonly StylePresetItem[]
    genStyles: string[]
    recommendedStyleCodes: string[]
    showExtended: boolean
    fusionMode: boolean
    fusionStrategy: string
    styleWeights: Record<string, number>
    useKbRef: boolean
    scriptCategories?: string[]
    selectedKbCategories?: string[]
  }
  handlers: {
    setGenPersonaId: (v: number | '') => void
    setGenScene: (v: string) => void
    setShowExtended: (v: boolean) => void
    onToggleStyle: (s: string) => void
    setFusionMode: (v: boolean) => void
    setFusionStrategy: (v: string) => void
    setStyleWeights: (v: Record<string, number>) => void
    setGenDuration: (v: number) => void
    setUseKbRef: (v: boolean) => void
    setSelectedKbCategories?: (v: string[]) => void
    onStart: () => void
    onPreview?: () => void
  }
}

/* ── Config Panel (when no generation in progress and no refine target) ── */
function ConfigPanel(props: ConfigPanelProps) {
  const { product, config, handlers } = props
  return (
    <Box>
      {/* header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2.5 }}>
        <AutoAwesomeIcon color="primary" fontSize="small" />
        <Typography variant="subtitle1" fontWeight={700}>
          AI 生成配置
        </Typography>
      </Box>

      {/* 1. 风格选择 */}
      <Typography variant="body2" fontWeight={600} sx={{ mb: 1 }}>
        1. 选择风格
        {config.genStyles.length > 0 && (
          <Chip size="small" label={config.genStyles.length} color="primary" sx={{ ml: 1, height: 18, fontSize: '0.65rem' }} />
        )}
      </Typography>
      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0.25, mb: 1.5 }}>
        {config.corePresets.map((o) => (
          <StyleChip
            key={o.presetCode} preset={o}
            selected={config.genStyles.includes(o.presetCode)}
            recommended={config.recommendedStyleCodes.includes(o.presetCode)}
            onClick={() => handlers.onToggleStyle(o.presetCode)}
          />
        ))}
      </Box>

      {config.extendedPresets.length > 0 && (
        <>
          <Button
            size="small" onClick={() => handlers.setShowExtended(!config.showExtended)}
            endIcon={config.showExtended ? <ExpandLessIcon /> : <ExpandMoreIcon />}
            sx={{ mb: 0.5, textTransform: 'none', color: 'text.secondary', fontSize: '0.75rem' }}
          >
            更多风格 ({config.extendedPresets.length})
          </Button>
          <Collapse in={config.showExtended}>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0.25, mb: 1.5 }}>
              {config.extendedPresets.map((o) => (
                <StyleChip
                  key={o.presetCode} preset={o}
                  selected={config.genStyles.includes(o.presetCode)}
                  recommended={config.recommendedStyleCodes.includes(o.presetCode)}
                  onClick={() => handlers.onToggleStyle(o.presetCode)}
                />
              ))}
            </Box>
          </Collapse>
        </>
      )}

      {/* fusion toggle */}
      <FormControlLabel
        control={<Switch checked={config.fusionMode} onChange={(_e, v) => handlers.setFusionMode(v)} color="secondary" size="small" />}
        label={
          <Typography variant="body2" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <MergeTypeIcon sx={{ fontSize: 16 }} /> 风格融合
          </Typography>
        }
        sx={{ mb: 0.5 }}
      />
      {config.fusionMode && (
        <Typography variant="caption" color="text.disabled" sx={{ display: 'block', ml: 4, mb: 1 }}>
          多风格融合为一条话术，适合长时段混合讲解
        </Typography>
      )}

      {/* Fusion Strategy Selector */}
      {config.fusionMode && config.genStyles.length >= 2 && (
        <Card variant="outlined" sx={{ mt: 1.5, mb: 1.5, bgcolor: (t) => alpha(t.palette.secondary.main, 0.02) }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Typography variant="body2" fontWeight={600} sx={{ mb: 1.5 }}>
              融合策略
            </Typography>
            <FormControl size="small" fullWidth>
              <InputLabel>选择融合方式</InputLabel>
              <Select
                value={config.fusionStrategy}
                label="选择融合方式"
                onChange={(e) => handlers.setFusionStrategy(e.target.value)}
              >
                {getApplicableFusionStrategies(config.genStyles.length).map((strategy) => (
                  <MenuItem key={strategy.id} value={strategy.id}>
                    <Box>
                      <Typography variant="body2" fontWeight={500}>
                        {strategy.icon} {strategy.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {strategy.description}
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </CardContent>
        </Card>
      )}

      {/* Style Weights Configuration */}
      {config.fusionMode && config.genStyles.length >= 2 && (
        <Card variant="outlined" sx={{ mt: 1.5, mb: 1.5, bgcolor: (t) => alpha(t.palette.primary.main, 0.02) }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography variant="body2" fontWeight={600}>
                风格权重配置
              </Typography>
              {/* Preset Templates Dropdown */}
              {getApplicablePresets(config.genStyles.length).length > 0 && (
                <FormControl size="small" sx={{ minWidth: 120 }}>
                  <Select
                    value=""
                    displayEmpty
                    onChange={(e) => {
                      const presetId = e.target.value
                      if (presetId) {
                        const preset = STYLE_WEIGHT_PRESETS.find((p) => p.id === presetId)
                        if (preset) {
                          const newWeights = applyPresetToStyles(preset, config.genStyles)
                          handlers.setStyleWeights(newWeights)
                        }
                      }
                    }}
                    renderValue={() => (
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <AutoFixHighIcon sx={{ fontSize: 16 }} />
                        <Typography variant="caption">快速预设</Typography>
                      </Box>
                    )}
                    sx={{ fontSize: '0.75rem' }}
                  >
                    {getApplicablePresets(config.genStyles.length).map((preset) => (
                      <MenuItem key={preset.id} value={preset.id}>
                        <Box>
                          <Typography variant="body2" fontWeight={500}>
                            {preset.icon} {preset.name}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {preset.description}
                          </Typography>
                        </Box>
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>
            {config.genStyles.map((style) => {
              const preset = [...config.corePresets, ...config.extendedPresets].find((p) => p.presetCode === style)
              const weight = config.styleWeights[style] || (1.0 / config.genStyles.length)
              return (
                <Box key={style} sx={{ mb: 1.5 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}>
                    <Typography variant="body2" fontWeight={500}>
                      {preset?.presetName || style}
                    </Typography>
                    <Typography variant="body2" color="primary" fontWeight={600}>
                      {Math.round(weight * 100)}%
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={Math.round(weight * 100)}
                      onChange={(e) => {
                        const newWeight = Number(e.target.value) / 100
                        handlers.setStyleWeights({ ...config.styleWeights, [style]: newWeight })
                      }}
                      style={{ flex: 1, cursor: 'pointer' }}
                    />
                  </Box>
                </Box>
              )
            })}
            <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 1 }}>
              提示：权重越高，该风格在融合话术中的占比越大
            </Typography>
          </CardContent>
        </Card>
      )}

      <Divider sx={{ my: 2 }} />

      {/* 2. 场景 & 参数 */}
      <Typography variant="body2" fontWeight={600} sx={{ mb: 1.5 }}>
        2. 场景与参数
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5, mb: 2 }}>
        <FormControl size="small" fullWidth>
          <InputLabel>直播场景</InputLabel>
          <Select value={product.genScene} label="直播场景" onChange={(e) => handlers.setGenScene(e.target.value)}>
            {SCENE_OPTIONS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
        <FormControl size="small" fullWidth>
          <InputLabel>时长</InputLabel>
          <Select value={product.genDuration} label="时长" onChange={(e) => handlers.setGenDuration(Number(e.target.value))}>
            {DURATION_OPTIONS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
      </Box>

      <FormControl size="small" fullWidth sx={{ mb: 2 }}>
        <InputLabel>人设（可选）</InputLabel>
        <Select
          value={product.genPersonaId === '' ? '_' : product.genPersonaId} label="人设（可选）"
          onChange={(e) => handlers.setGenPersonaId(e.target.value === '_' ? '' : Number(e.target.value))}
        >
          <MenuItem value="_">默认人设</MenuItem>
          {product.personas.map((p) => <MenuItem key={String(p.id)} value={p.id as number}>{String(p.name ?? p.id)}</MenuItem>)}
        </Select>
      </FormControl>

      <FormControlLabel
        control={<Switch checked={config.useKbRef} onChange={(_e, v) => handlers.setUseKbRef(v)} size="small" />}
        label={<Typography variant="body2" color="text.secondary">参考话术知识库</Typography>}
      />

      {/* Knowledge Base Categories Multi-Select */}
      {config.useKbRef && config.scriptCategories && config.scriptCategories.length > 0 && (
        <FormControl size="small" fullWidth sx={{ mt: 1.5 }}>
          <InputLabel>参考话术分类（可多选）</InputLabel>
          <Select
            multiple
            value={config.selectedKbCategories || []}
            onChange={(e) => handlers.setSelectedKbCategories?.(typeof e.target.value === 'string' ? e.target.value.split(',') : e.target.value)}
            input={<OutlinedInput label="参考话术分类（可多选）" />}
            renderValue={(selected) => (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {selected.map((value) => (
                  <Chip key={value} label={value} size="small" sx={{ height: 22 }} />
                ))}
              </Box>
            )}
          >
            {config.scriptCategories.map((category) => (
              <MenuItem key={category} value={category}>
                <Checkbox checked={(config.selectedKbCategories || []).indexOf(category) > -1} size="small" />
                <ListItemText primary={category} />
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )}

      <Divider sx={{ my: 2 }} />

      {/* CTA */}
      <Box sx={{ display: 'flex', gap: 1 }}>
        {handlers.onPreview && (
          <Button
            variant="outlined"
            size="large"
            startIcon={<VisibilityIcon />}
            onClick={handlers.onPreview}
            disabled={config.genStyles.length === 0 || (config.fusionMode && config.genStyles.length < 2)}
            sx={{ flex: 1, borderRadius: 2, py: 1.2, fontWeight: 600 }}
          >
            预览
          </Button>
        )}
        <Button
          variant="contained"
          fullWidth={!handlers.onPreview}
          size="large"
          startIcon={config.fusionMode ? <MergeTypeIcon /> : <AutoAwesomeIcon />}
          onClick={handlers.onStart}
          disabled={config.genStyles.length === 0 || (config.fusionMode && config.genStyles.length < 2)}
          sx={{ flex: handlers.onPreview ? 2 : 1, borderRadius: 2, py: 1.2, fontWeight: 700, fontSize: '0.95rem' }}
        >
          {config.fusionMode
            ? `融合生成（${config.genStyles.length} 种风格 → 1 条话术）`
            : `生成 ${config.genStyles.length} 条话术`}
        </Button>
      </Box>

      <Typography variant="caption" color="text.disabled" sx={{ display: 'block', textAlign: 'center', mt: 1.5 }}>
        {handlers.onPreview ? '可先预览15秒片段，满意后再完整生成' : '每种风格独立生成一条话术，实时查看进度'}
      </Typography>
    </Box>
  )
}

/* ── Progress Panel (when generation in progress / done) ── */
function ProgressPanel({ steps, isGenerating, doneCount, failCount, fusionMode, genScene, abExperimentId, onCancel, onRetry, onReset }: {
  steps: ScriptGenStep[]; isGenerating: boolean
  doneCount: number; failCount: number; fusionMode: boolean; genScene: string
  abExperimentId?: number | null; abVariantId?: number | null
  onCancel: () => void; onRetry: () => void; onReset: () => void
}) {
  const total = steps.length
  const pct = total > 0 ? ((doneCount + failCount) / total) * 100 : 0
  const allDone = !isGenerating && doneCount + failCount === total

  return (
    <Box>
      {/* header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        {isGenerating
          ? <CircularProgress size={20} />
          : allDone && failCount === 0
            ? <CheckCircleIcon color="success" />
            : allDone && failCount > 0
              ? <ErrorOutlineIcon color="warning" />
              : null
        }
        <Typography variant="subtitle1" fontWeight={700}>
          {isGenerating ? '生成中...' : allDone && failCount === 0 ? '全部完成' : allDone ? '部分失败' : '生成进度'}
        </Typography>
      </Box>

      {/* tags */}
      <Box sx={{ display: 'flex', gap: 0.5, mb: 2, flexWrap: 'wrap' }}>
        {fusionMode && <Chip size="small" label="融合模式" icon={<MergeTypeIcon />} variant="outlined" sx={{ height: 22 }} />}
        {genScene && (
          <Chip size="small" label={SCENE_OPTIONS.find((s) => s.value === genScene)?.label ?? genScene} variant="outlined" sx={{ height: 22 }} />
        )}
        {abExperimentId && (
          <Chip
            size="small"
            label={`A/B实验 #${abExperimentId}`}
            color="secondary"
            variant="outlined"
            sx={{ height: 22 }}
          />
        )}
      </Box>

      {/* progress bar */}
      <Box sx={{ mb: 2.5 }}>
        <LinearProgress
          variant="determinate" value={pct}
          color={failCount > 0 && !isGenerating ? 'warning' : 'primary'}
          sx={{ height: 8, borderRadius: 1, mb: 1 }}
        />
        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography variant="caption" color="text.secondary">
            {doneCount + failCount} / {total}
          </Typography>
          {doneCount > 0 && <Typography variant="caption" color="success.main">{doneCount} 成功</Typography>}
          {failCount > 0 && <Typography variant="caption" color="error.main">{failCount} 失败</Typography>}
        </Box>
      </Box>

      {/* step list */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        {steps.map((step) => (
          <StepRow key={step.style} step={step} />
        ))}
      </Box>

      {/* summary card when done */}
      {allDone && (
        <Card variant="outlined" sx={{ mt: 2.5, bgcolor: (t) => alpha(t.palette.success.main, 0.03) }}>
          <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Typography variant="body2" fontWeight={600} sx={{ mb: 0.5 }}>
              {failCount === 0 ? '话术已生成并自动激活' : '部分话术生成失败'}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {failCount === 0
                ? '左侧列表已刷新，可直接查看和使用'
                : '可重试失败项，或关闭后手动补充'}
            </Typography>
          </CardContent>
        </Card>
      )}

      <Divider sx={{ my: 2 }} />

      {isGenerating ? (
        <Button variant="outlined" color="warning" onClick={onCancel} fullWidth sx={{ borderRadius: 2 }}>
          取消生成
        </Button>
      ) : (
        <Box sx={{ display: 'flex', gap: 1 }}>
          {failCount > 0 && (
            <Button variant="contained" startIcon={<ReplayIcon />} onClick={onRetry} sx={{ flex: 1, borderRadius: 2 }}>
              重试失败 ({failCount})
            </Button>
          )}
          <Button variant="outlined" onClick={onReset} sx={{ flex: 1, borderRadius: 2 }}>
            返回配置
          </Button>
        </Box>
      )}
    </Box>
  )
}

/* ━━━━━━━━━━━━━━━━━━━ ScriptGeneratePanel ━━━━━━━━━━━━━━━━━━━ */
export function ScriptGeneratePanel(props: ScriptGeneratePanelProps) {
  const {
    steps, isGenerating, doneCount, failCount, hasSteps,
    onCancel, onRetry, onReset,
    fusionMode, genScene,
    abExperimentId, abVariantId,
    personas, genPersonaId, setGenPersonaId, genDuration, setGenDuration,
    corePresets, extendedPresets, genStyles, recommendedStyleCodes,
    showExtended, setShowExtended, onToggleStyle,
    fusionStrategy, setFusionStrategy, styleWeights, setStyleWeights,
    useKbRef, setUseKbRef, setGenScene,
    scriptCategories, selectedKbCategories, setSelectedKbCategories,
    onStart, onPreview, setFusionMode,
  } = props

  if (hasSteps) {
    return (
      <ProgressPanel
        steps={steps} isGenerating={isGenerating}
        doneCount={doneCount} failCount={failCount}
        fusionMode={fusionMode} genScene={genScene}
        abExperimentId={abExperimentId} abVariantId={abVariantId}
        onCancel={onCancel} onRetry={onRetry} onReset={onReset}
      />
    )
  }

  return (
    <ConfigPanel
      product={{
        personas,
        genPersonaId,
        genScene,
        genDuration,
      }}
      config={{
        corePresets,
        extendedPresets,
        genStyles,
        recommendedStyleCodes,
        showExtended,
        fusionMode,
        fusionStrategy,
        styleWeights,
        useKbRef,
        scriptCategories,
        selectedKbCategories,
      }}
      handlers={{
        setGenPersonaId,
        setGenScene,
        setShowExtended,
        onToggleStyle,
        setFusionMode,
        setFusionStrategy,
        setStyleWeights,
        setGenDuration,
        setUseKbRef,
        setSelectedKbCategories,
        onStart,
        onPreview,
      }}
    />
  )
}
