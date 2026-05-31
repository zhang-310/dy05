/**
 * GenerateStageView 顶部导航栏：两层布局
 * 第一层：核心导航（返回/标题/模型/话术库/预设/热搜/灵感/下一步）
 * 第二层：高级配置（IP/素材/模块/留人/互动），可收起
 */
import { memo, useState } from 'react'
import {
  Box,
  Button,
  Chip,
  Collapse,
  FormControl,
  FormControlLabel,
  IconButton,
  MenuItem,
  Select,
  Switch,
  Tooltip,
  Typography,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import TuneIcon from '@mui/icons-material/Tune'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import { KbRefPreviewPopover } from './KbRefPreviewPopover'
import { PresetSelector } from './PresetSelector'
import { TrendingTopicsPanel } from './TrendingTopicsPanel'
import { ShortVideoInspirationPanel } from './ShortVideoInspirationPanel'
import type { BuilderState } from '../hooks/useLiveScriptBuilder'

const GENERATE_STAGE_HEADER_READY_ENDPOINTS = [
  '/ai/model/list-by-task',
  '/ai/model/list',
  '/ai/knowledge-base/:kbId/search',
  '/live/generation-preset/list',
  '/live/generation-preset/save',
  '/live/generation-preset/delete',
  '/ai/brain/trends/current',
  '/short-video/viral/recommended',
] as const

const GENERATE_STAGE_HEADER_UNSUPPORTED_ACTIONS = [
  'local-model-fallback',
  'local-preset-fallback',
  'local-kb-search-fallback',
  'local-trend-fallback',
  'local-viral-video-fallback',
  'direct-script-generation',
  'direct-script-mutation',
  'direct-product-mutation',
  'shortvideo-project-create',
  'session-status-mutation',
] as const

export interface GenerateStageHeaderProps {
  builder: BuilderState
  onPrev: () => void
  onNext: () => void
  canAdvance: boolean
  kbRefQuery: string
}

/** 高级配置的当前选中项摘要（收起时显示） */
function getAdvancedSummary(builder: BuilderState): string {
  const parts: string[] = []
  if (builder.ipType) parts.push(`IP:${builder.ipType === 'phenomenal' ? '现象级' : '顶级'}`)
  if (builder.materialType) {
    const labels: Record<string, string> = { joke: '段子', chicken_soup: '鸡汤', quote: '名言', jingle: '顺口溜', proverb: '歇后语', interactive_game: '互动' }
    parts.push(`素材:${labels[builder.materialType] ?? builder.materialType}`)
  }
  if (builder.scriptModule) {
    const labels: Record<string, string> = { emotion_drive: '情绪驱动', value_creation: '价值塑造', conversion_engine: '转化引擎', trust_reinforcement: '信任加固' }
    parts.push(labels[builder.scriptModule] ?? builder.scriptModule)
  }
  if (builder.retentionStrategy) {
    const labels: Record<string, string> = { high_suspense: '高频悬念', high_practical: '干货密集', high_climax: '情绪高潮' }
    parts.push(labels[builder.retentionStrategy] ?? builder.retentionStrategy)
  }
  if (builder.interactionLevel) {
    const labels: Record<string, string> = { light: '轻互动', medium: '中互动', heavy: '强互动' }
    parts.push(labels[builder.interactionLevel] ?? builder.interactionLevel)
  }
  return parts.join(' · ')
}

export const GenerateStageHeader = memo(function GenerateStageHeader({
  builder,
  onPrev,
  onNext,
  canAdvance,
  kbRefQuery,
}: GenerateStageHeaderProps) {
  const [advancedOpen, setAdvancedOpen] = useState(false)
  const advancedSummary = getAdvancedSummary(builder)
  const nextDisabledReason = builder.genLoading ? 'generation-running' : canAdvance ? 'ready' : 'blocked-by-parent-context'
  const hotKeywords = builder.hotKeywords ?? []

  return (
    <Box
      data-testid="generate-stage-header-root"
      data-contract-scope="live-generate-stage-header-orchestrator"
      data-ready-endpoints={GENERATE_STAGE_HEADER_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={GENERATE_STAGE_HEADER_UNSUPPORTED_ACTIONS.join('|')}
      data-model-count={builder.llmModels.length}
      data-selected-model-id={builder.selectedModelId ?? ''}
      data-kb-ref-enabled={String(builder.useKbRef)}
      data-kb-query-empty={String(kbRefQuery.trim().length === 0)}
      data-hot-keyword-count={hotKeywords.length}
      data-next-disabled-reason={nextDisabledReason}
      data-no-local-model-fallback="true"
      data-no-direct-generation="true"
      sx={{ flexShrink: 0, borderBottom: 1, borderColor: 'divider' }}
    >
      {/* ━━━ 第一层：核心导航 ━━━ */}
      <Box
        data-testid="generate-stage-header-toolbar"
        data-contract-source={GENERATE_STAGE_HEADER_READY_ENDPOINTS.join('|')}
        sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 2, pt: 1, pb: 0.5 }}
      >
        <Button
          size="small"
          startIcon={<ArrowBackIcon />}
          onClick={onPrev}
          data-testid="generate-stage-prev-button"
          data-contract-source="onPrev-prop"
        >
          返回选品
        </Button>
        <Typography variant="subtitle1" fontWeight={700} sx={{ flexShrink: 0 }}>生成话术</Typography>
        {builder.llmModels.length > 0 && (
          <FormControl
            size="small"
            sx={{ minWidth: 130 }}
            data-testid="generate-stage-model-selector"
            data-contract-source="/ai/model/list-by-task|/ai/model/list"
            data-no-local-model-fallback="true"
          >
            <Select
              value={builder.selectedModelId ?? ''}
              displayEmpty
              onChange={(e) => builder.setSelectedModelId(String(e.target.value))}
              disabled={builder.genLoading}
              sx={{ height: 32, fontSize: '0.85rem' }}
            >
              <MenuItem value="" disabled><Typography variant="body2" color="text.secondary">选择模型</Typography></MenuItem>
              {builder.llmModels.map((m) => (
                <MenuItem key={String(m.id)} value={String(m.id ?? '')}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Typography variant="body2" sx={{ fontSize: '0.85rem' }}>{String(m.modelName ?? m.modelVersion ?? m.id ?? '-')}</Typography>
                    {m.modelProvider && <Chip label={m.modelProvider} size="small" sx={{ height: 18, fontSize: '0.75rem' }} />}
                  </Box>
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        )}
        <FormControlLabel
          data-testid="generate-stage-kb-switch"
          data-contract-source="local-generation-options-store"
          control={<Switch size="small" checked={builder.useKbRef} onChange={(e) => builder.setUseKbRef(e.target.checked)} />}
          label={<Typography variant="body2">话术库</Typography>}
          sx={{ mr: 0, ml: 0 }}
        />
        {builder.useKbRef && (
          <KbRefPreviewPopover query={kbRefQuery} disabled={builder.genLoading} />
        )}
        <PresetSelector
          currentStyle={builder.genStyle}
          currentModelId={builder.selectedModelId ?? ''}
          currentUseKbRef={builder.useKbRef}
          currentIpType={builder.ipType}
          currentMaterialType={builder.materialType}
          currentScriptModule={builder.scriptModule}
          currentRetentionStrategy={builder.retentionStrategy}
          currentInteractionLevel={builder.interactionLevel}
          onApplyPreset={(preset) => {
            if (preset.style) builder.setGenStyle(preset.style)
            // 兼容旧数据
            if (!preset.style && preset.genStyle) builder.setGenStyle(preset.genStyle)
            if (preset.modelId) builder.setSelectedModelId(String(preset.modelId))
            if (preset.useKbRef != null) builder.setUseKbRef(Boolean(preset.useKbRef))
            if (preset.ipType != null) builder.setIpType(preset.ipType)
            if (preset.materialType != null) builder.setMaterialType(preset.materialType)
            if (preset.scriptModule != null) builder.setScriptModule(preset.scriptModule)
            if (preset.retentionStrategy != null) builder.setRetentionStrategy(preset.retentionStrategy)
            if (preset.interactionLevel != null) builder.setInteractionLevel(preset.interactionLevel)
          }}
          disabled={builder.genLoading}
        />
        <TrendingTopicsPanel
          onInjectKeyword={(kw) => {
            const current = builder.hotKeywords ?? []
            if (!current.includes(kw)) builder.setHotKeywords([...current, kw])
          }}
          selectedKeywords={hotKeywords}
        />
        <ShortVideoInspirationPanel
          onAdaptHook={(hookLine) => {
            const current = builder.hotKeywords ?? []
            if (!current.includes(hookLine)) builder.setHotKeywords([...current, hookLine])
          }}
        />
        <Box sx={{ flex: 1 }} />
        {/* 高级配置展开按钮 + 摘要 */}
        <Tooltip title={advancedOpen ? '收起高级配置' : '展开高级配置'}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, cursor: 'pointer' }} onClick={() => setAdvancedOpen((v) => !v)}>
            {!advancedOpen && advancedSummary && (
              <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.75rem', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {advancedSummary}
              </Typography>
            )}
            <IconButton data-testid="generate-stage-advanced-toggle" size="small" sx={{ p: 0.25 }}>
              <TuneIcon sx={{ fontSize: 18 }} />
              <ExpandMoreIcon sx={{ fontSize: 14, transition: 'transform 0.2s', transform: advancedOpen ? 'rotate(180deg)' : 'rotate(0deg)' }} />
            </IconButton>
          </Box>
        </Tooltip>
        <Button
          variant="contained"
          size="small"
          endIcon={<ArrowForwardIcon />}
          disabled={!canAdvance || builder.genLoading}
          onClick={onNext}
          data-testid="generate-stage-next-button"
          data-contract-source="onNext-prop"
          data-disabled-reason={nextDisabledReason}
          sx={{ flexShrink: 0 }}
        >
          下一步：编辑
        </Button>
      </Box>

      {/* ━━━ 第二层：高级配置（可收起） ━━━ */}
      <Collapse in={advancedOpen}>
        <Box
          data-testid="generate-stage-advanced-surface"
          sx={(theme) => ({
            display: 'flex',
            flexWrap: 'wrap',
            gap: 1.5,
            px: 2,
            py: 0.75,
            bgcolor: theme.palette.mode === 'dark'
              ? alpha(theme.palette.common.white, 0.04)
              : theme.palette.action.hover,
            borderTop: 1,
            borderColor: 'divider',
          })}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>IP:</Typography>
            {([
              { label: '通用', value: '' },
              { label: '现象级', value: 'phenomenal' },
              { label: '顶级', value: 'top' },
            ] as const).map((item) => (
              <Chip
                key={item.value}
                label={item.label}
                size="small"
                color={builder.ipType === item.value ? 'primary' : 'default'}
                variant={builder.ipType === item.value ? 'filled' : 'outlined'}
                onClick={() => builder.setIpType(item.value)}
                disabled={builder.genLoading}
                sx={{ height: 22, fontSize: '0.75rem' }}
              />
            ))}
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>素材:</Typography>
            {([
              { label: '默认', value: '' },
              { label: '段子', value: 'joke' },
              { label: '鸡汤', value: 'chicken_soup' },
              { label: '名言', value: 'quote' },
              { label: '顺口溜', value: 'jingle' },
              { label: '歇后语', value: 'proverb' },
              { label: '互动', value: 'interactive_game' },
            ] as const).map((item) => (
              <Chip
                key={item.value}
                label={item.label}
                size="small"
                color={builder.materialType === item.value ? 'secondary' : 'default'}
                variant={builder.materialType === item.value ? 'filled' : 'outlined'}
                onClick={() => builder.setMaterialType(item.value)}
                disabled={builder.genLoading}
                sx={{ height: 22, fontSize: '0.75rem' }}
              />
            ))}
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>模块:</Typography>
            {([
              { label: '自动', value: '' },
              { label: '情绪驱动', value: 'emotion_drive' },
              { label: '价值塑造', value: 'value_creation' },
              { label: '转化引擎', value: 'conversion_engine' },
              { label: '信任加固', value: 'trust_reinforcement' },
            ] as const).map((item) => (
              <Chip key={item.value} label={item.label} size="small"
                color={builder.scriptModule === item.value ? 'info' : 'default'}
                variant={builder.scriptModule === item.value ? 'filled' : 'outlined'}
                onClick={() => builder.setScriptModule(item.value)}
                disabled={builder.genLoading} sx={{ height: 22, fontSize: '0.75rem' }} />
            ))}
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>留人:</Typography>
            {([
              { label: '自动', value: '' },
              { label: '高频悬念', value: 'high_suspense' },
              { label: '干货密集', value: 'high_practical' },
              { label: '情绪高潮', value: 'high_climax' },
            ] as const).map((item) => (
              <Chip key={item.value} label={item.label} size="small"
                color={builder.retentionStrategy === item.value ? 'warning' : 'default'}
                variant={builder.retentionStrategy === item.value ? 'filled' : 'outlined'}
                onClick={() => builder.setRetentionStrategy(item.value)}
                disabled={builder.genLoading} sx={{ height: 22, fontSize: '0.75rem' }} />
            ))}
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>互动:</Typography>
            {([
              { label: '自动', value: '' },
              { label: '轻互动', value: 'light' },
              { label: '中互动', value: 'medium' },
              { label: '强互动', value: 'heavy' },
            ] as const).map((item) => (
              <Chip key={item.value} label={item.label} size="small"
                color={builder.interactionLevel === item.value ? 'success' : 'default'}
                variant={builder.interactionLevel === item.value ? 'filled' : 'outlined'}
                onClick={() => builder.setInteractionLevel(item.value)}
                disabled={builder.genLoading} sx={{ height: 22, fontSize: '0.75rem' }} />
            ))}
          </Box>
        </Box>
      </Collapse>
    </Box>
  )
})
