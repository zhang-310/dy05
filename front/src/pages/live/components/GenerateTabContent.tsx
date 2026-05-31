import { useMemo, useState } from 'react'
import {
  Box, Button, Typography, LinearProgress, Alert,
  FormControl, InputLabel, Select, MenuItem, Slider,
  Chip, CircularProgress, FormControlLabel, Switch, Divider, Stack,
  TextField, Accordion, AccordionSummary, AccordionDetails,
  Tooltip, Paper, Tab, Tabs, IconButton,
} from '@mui/material'
import type { ChipProps } from '@mui/material'
import { alpha } from '@mui/material/styles'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import StopIcon from '@mui/icons-material/Stop'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import AccessTimeIcon from '@mui/icons-material/AccessTime'
import LocalFireDepartmentIcon from '@mui/icons-material/LocalFireDepartment'
import PsychologyIcon from '@mui/icons-material/Psychology'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import RefreshIcon from '@mui/icons-material/Refresh'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import { useCoreData, useGeneration, useEditor } from '../contexts'
import { liveApi } from '@/api/live'
import type { LiveScript, LiveScriptSave } from '@/api/live'
import { douyinApi } from '@/api/douyin'
import { tianapi } from '@/api/tianapi'
import { useToast } from '@/contexts/ToastContext'
import { useQuery } from '@tanstack/react-query'
import { getErrorMessage } from '@/utils/errorHandler'
import { sortLiveProducts, sortLiveScripts } from '../utils/order'

const GENERATE_TAB_STREAM_ENDPOINTS = [
  '/live/ai/generate-full-pipelined-sse',
  '/live/ai/generate-skeleton-sse',
] as const

const GENERATE_TAB_READY_ENDPOINTS = [
  ...GENERATE_TAB_STREAM_ENDPOINTS,
  '/live/generation-preset/list',
  '/douyin/persona/list',
  '/ai/knowledge-base/huashu/search',
  '/tianapi/hot/douyin',
  '/tianapi/hot/toutiao',
  '/tianapi/hot/weibo',
  '/tianapi/hot/network',
  '/live/script/save',
] as const

const GENERATE_TAB_CONTEXT_ENDPOINTS = [
  '/live/session/get',
  '/live/product/by-session',
  '/live/script/by-session',
] as const

const GENERATE_TAB_UNSUPPORTED_ACTIONS = [
  'direct-ai-rest-generate-full',
  'direct-ai-product-script',
  'direct-product-mutation',
  'shortvideo-project-create',
  'session-status-start',
  'local-generated-script-fallback',
  'local-hotword-fallback',
  'local-preset-fallback',
  'local-persona-fallback',
] as const

const HOT_SOURCE_ENDPOINTS: Record<HotSourceKey, string> = {
  douyin: '/tianapi/hot/douyin',
  toutiao: '/tianapi/hot/toutiao',
  weibo: '/tianapi/hot/weibo',
  network: '/tianapi/hot/network',
}

const MATERIAL_TYPE_OPTIONS = [
  { value: '', label: '默认', desc: '不指定素材，AI 按场次自动生成' },
  { value: 'jingle', label: '顺口溜', desc: '检索 huashu/TianAPI 顺口溜，融合押韵口播记忆点' },
  { value: 'proverb', label: '歇后语', desc: '融合歇后语、俗语、接地气类比' },
  { value: 'quote', label: '名言金句', desc: '融合名言、金句、女性情绪价值表达' },
  { value: 'joke', label: '段子神回复', desc: '融合幽默段子、热梗、神回复' },
  { value: 'chicken_soup', label: '鸡汤共鸣', desc: '融合疗愈、励志、情绪共鸣语料' },
  { value: 'interactive_game', label: '互动玩法', desc: '设计猜价格、扣口令、评论区互动' },
] as const

// 全量风格列表 — 精确对齐后端 LivePromptFormatServiceImpl.expandSingleStyleFallback
const SCRIPT_STYLES = [
  // 基础风格
  { value: 'natural',        label: '自然流畅',   group: '基础', desc: '真实亲切，像朋友聊天' },
  { value: 'friendly',       label: '友好亲切',   group: '基础', desc: '亲切感强，拉近距离' },
  { value: 'warm',           label: '温暖贴心',   group: '基础', desc: '暖意十足，照顾情绪' },
  { value: 'gentle',         label: '温和闺蜜',   group: '基础', desc: '不施压，像闺蜜推荐' },
  { value: 'casual',         label: '轻松随意',   group: '基础', desc: '聊天式，降低防备' },
  // 激情促销
  { value: 'enthusiastic',   label: '激情热情',   group: '激情', desc: '活力四射，感染力强' },
  { value: 'passionate',     label: '热血激昂',   group: '激情', desc: '情绪饱满，催单有力' },
  { value: 'promotion',      label: '促销冲量',   group: '激情', desc: '限时抢购，制造紧迫' },
  { value: 'seeding',        label: '种草安利',   group: '激情', desc: '突出场景和体验感' },
  // 专业权威
  { value: 'professional',   label: '专业权威',   group: '专业', desc: '专业严谨，建立信任' },
  // 情感共鸣
  { value: 'emotional',      label: '情感共鸣',   group: '情感', desc: '触动情绪，引发共鸣' },
  { value: 'storytelling',   label: '故事叙述',   group: '情感', desc: '真实案例，增强说服力' },
  { value: 'empathy',        label: '共情理解',   group: '情感', desc: '感同身受，消解疑虑' },
] as const
const SCRIPT_TYPE_OPTIONS = [
  { value: 'intro',      label: '开场',   color: 'primary'   },
  { value: 'product',    label: '讲品',   color: 'secondary' },
  { value: 'promo',      label: '促销',   color: 'error'     },
  { value: 'interaction',label: '互动',   color: 'warning'   },
  { value: 'retention',  label: '留人',   color: 'success'   },
  { value: 'closing',    label: '结尾',   color: 'default'   },
] as const

/** 带 info 图标的 Tooltip 标签 */
function LabelTip({ label, tip }: { label: string; tip: React.ReactNode }) {
  return (
    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.4 }}>
      {label}
      <Tooltip arrow placement="right" title={tip}>
        <InfoOutlinedIcon sx={{ fontSize: 13, color: 'text.disabled', cursor: 'help', flexShrink: 0 }} />
      </Tooltip>
    </Box>
  )
}

function SlotConfigRow({ script, sessionId, onSave }: {
  script: LiveScript
  sessionId: number
  onSave: (p: Partial<LiveScriptSave>) => Promise<void>
}) {
  const [durInput, setDurInput] = useState((script.durationLimitSec ?? 0) > 0 ? (script.durationLimitSec ?? 0) : 0)
  const [localStyle, setLocalStyle] = useState(script.style ?? '')
  const [localReq, setLocalReq] = useState(script.requirement ?? '')
  const typeOpt = SCRIPT_TYPE_OPTIONS.find(o => o.value === script.scriptType)
  const DUR_PRESETS = [60, 120, 180, 300]
  const save = (patch: Partial<LiveScriptSave>) => onSave({ id: script.id, sessionId, ...patch })
  return (
    <Box
      data-testid="generate-finetune-slot-row"
      data-contract-source="/live/script/save"
      data-script-id={script.id}
      sx={{ px: 1.5, py: 1.25, borderBottom: '1px solid', borderColor: 'divider', '&:last-child': { borderBottom: 'none' } }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 0.75 }}>
        <Chip label={typeOpt?.label ?? script.scriptType} size="small" color={(typeOpt?.color ?? 'default') as ChipProps['color']} sx={{ fontSize: 10, height: 18, flexShrink: 0 }} />
        <Typography variant="caption" fontWeight={600} noWrap sx={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis' }}>{String(script.scriptType || `#${script.sequenceNo}`)}</Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap', mb: 0.75 }}>
        <AccessTimeIcon sx={{ fontSize: 13, color: 'text.secondary', flexShrink: 0 }} />
        <Typography variant="caption" color="text.secondary" sx={{ mr: 0.25 }}>时长</Typography>
        {DUR_PRESETS.map(sec => (
          <Chip key={sec} label={`${sec / 60}m`} size="small"
            variant={durInput === sec ? 'filled' : 'outlined'}
            color={durInput === sec ? 'primary' : 'default'}
            onClick={() => { setDurInput(sec); save({ durationLimitSec: sec }) }}
            sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
          />
        ))}
        <Chip label="AI自定" size="small"
          variant={durInput === 0 ? 'filled' : 'outlined'}
          color={durInput === 0 ? 'secondary' : 'default'}
          onClick={() => { setDurInput(0); save({ durationLimitSec: 0 }) }}
          sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
        />
      </Box>
      <Box sx={{ display: 'flex', gap: 0.5, mb: 0.75 }}>
        <FormControl size="small" sx={{ flex: 1 }}>
          <InputLabel sx={{ fontSize: 11 }}>专属风格</InputLabel>
          <Select label="专属风格" value={localStyle} onChange={e => { setLocalStyle(e.target.value); save({ style: e.target.value || undefined }) }} sx={{ fontSize: 11 }}>
            <MenuItem value=""><em>继承全局</em></MenuItem>
            {SCRIPT_STYLES.map(s => <MenuItem key={s.value} value={s.value} sx={{ fontSize: 11 }}>{s.label}</MenuItem>)}
          </Select>
        </FormControl>
      </Box>
      <TextField
        size="small" fullWidth multiline minRows={1} maxRows={3}
        placeholder="槽位专属要求（留空继承全局）"
        value={localReq}
        onChange={e => setLocalReq(e.target.value)}
        onBlur={() => save({ requirement: localReq.trim() || undefined })}
        inputProps={{ style: { fontSize: 11 } }}
        sx={{ '& .MuiOutlinedInput-root': { py: 0.5 } }}
      />
    </Box>
  )
}
// ── 热词采集面板（第一列）────────────────────────────────────────────
const HOT_SOURCES = [
  { key: 'douyin',  label: '抖音',  fn: () => tianapi.hotDouyin() },
  { key: 'toutiao', label: '头条',  fn: () => tianapi.hotToutiao() },
  { key: 'weibo',   label: '微博',  fn: () => tianapi.hotWeibo() },
  { key: 'network', label: '全网',  fn: () => tianapi.hotNetwork() },
] as const
type HotSourceKey = typeof HOT_SOURCES[number]['key']

function HotwordsPanel({ selected, onAdd, onRemove }: {
  selected: string[]
  onAdd: (kw: string) => void
  onRemove: (kw: string) => void
}) {
  const [activeSource, setActiveSource] = useState<HotSourceKey>('douyin')
  const source = HOT_SOURCES.find(s => s.key === activeSource)!

  const { data: hotItems = [], isFetching, refetch } = useQuery({
    queryKey: ['hot-words', activeSource],
    queryFn: source.fn,
    staleTime: 5 * 60 * 1000,
    retry: 1,
  })
  const endpoint = HOT_SOURCE_ENDPOINTS[activeSource]

  return (
    <Box
      data-testid="generate-hotwords-panel"
      data-contract-source={Object.values(HOT_SOURCE_ENDPOINTS).join('|')}
      data-active-source={activeSource}
      data-active-endpoint={endpoint}
      data-selected-count={selected.length}
      data-hotword-count={hotItems.length}
      data-no-local-hotword-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}
    >
      {/* 标题 */}
      <Box sx={{ px: 1.5, pt: 1.5, pb: 1, flexShrink: 0 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 1 }}>
          <LocalFireDepartmentIcon sx={{ fontSize: 14, color: 'error.main' }} />
          <Typography variant="caption" fontWeight={700} color="error.main" letterSpacing={0.5}>热词采集</Typography>
          <Box sx={{ flex: 1 }} />
          <Tooltip title="刷新榜单">
            <span>
              <IconButton
                size="small"
                onClick={() => refetch()}
                disabled={isFetching}
                sx={{ p: 0.25 }}
                data-testid="generate-hotword-refresh-button"
                data-contract-source={endpoint}
              >
                {isFetching ? <CircularProgress size={12} /> : <RefreshIcon sx={{ fontSize: 14 }} />}
              </IconButton>
            </span>
          </Tooltip>
        </Box>
        {/* 来源 Tab */}
        <Tabs
          value={activeSource}
          onChange={(_e, v) => setActiveSource(v)}
          variant="scrollable"
          scrollButtons={false}
          sx={{ minHeight: 28, '& .MuiTab-root': { minHeight: 28, py: 0, px: 1, fontSize: 11 } }}
        >
          {HOT_SOURCES.map(s => (
            <Tab key={s.key} value={s.key} label={s.label} />
          ))}
        </Tabs>
      </Box>
      <Divider />
      {/* 已选热词 */}
      {selected.length > 0 && (
        <Box sx={{ px: 1.5, py: 0.75, flexShrink: 0, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="caption" color="warning.main" fontWeight={600} display="block" mb={0.5}>已植入 ({selected.length})</Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.4 }}>
            {selected.map(kw => (
              <Chip key={kw} label={kw} size="small" color="warning" variant="filled"
                onDelete={() => onRemove(kw)}
                sx={{ fontSize: 10, height: 20 }}
              />
            ))}
          </Box>
        </Box>
      )}
      {/* 榜单列表 */}
      <Box sx={{ flex: 1, overflow: 'auto', px: 1, py: 0.5 }}>
        {isFetching && hotItems.length === 0 ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', pt: 3 }}>
            <CircularProgress size={20} />
          </Box>
        ) : hotItems.length === 0 ? (
          <Typography
            variant="caption"
            color="text.secondary"
            data-testid="generate-hotword-empty"
            data-contract-source={endpoint}
            data-no-local-hotword-fallback="true"
            sx={{ display: 'block', textAlign: 'center', pt: 2 }}
          >
            暂无数据（需配置 TianAPI）
          </Typography>
        ) : (
          <Stack spacing={0}>
            {hotItems.slice(0, 30).map((item, idx) => {
              const isSelected = selected.includes(item.word)
              return (
                <Box
                  key={item.word + idx}
                  onClick={() => isSelected ? onRemove(item.word) : onAdd(item.word)}
                  data-testid={isSelected ? 'generate-hotword-selected-surface' : undefined}
                  sx={(theme) => ({
                    display: 'flex', alignItems: 'center', gap: 0.75, px: 0.75, py: 0.6,
                    cursor: 'pointer', borderRadius: 0.75,
                    bgcolor: isSelected
                      ? alpha(theme.palette.warning.main, theme.palette.mode === 'dark' ? 0.18 : 0.1)
                      : 'transparent',
                    border: '1px solid',
                    borderColor: isSelected
                      ? alpha(theme.palette.warning.main, theme.palette.mode === 'dark' ? 0.5 : 0.35)
                      : 'transparent',
                    '&:hover': {
                      bgcolor: isSelected
                        ? alpha(theme.palette.warning.main, theme.palette.mode === 'dark' ? 0.24 : 0.16)
                        : theme.palette.action.hover,
                    },
                    transition: 'all 0.15s',
                  })}
                >
                  <Typography
                    variant="caption"
                    sx={{
                      width: 16, textAlign: 'center', flexShrink: 0, fontWeight: 700,
                      color: idx < 3 ? 'error.main' : 'text.disabled',
                      fontSize: 10,
                    }}
                  >{idx + 1}</Typography>
                  <Typography variant="caption" sx={{ flex: 1, fontWeight: isSelected ? 600 : 400, lineHeight: 1.3 }}>{item.word}</Typography>
                  {item.hotZh && (
                    <Typography variant="caption" sx={{ fontSize: 9, color: 'text.disabled', flexShrink: 0 }}>{item.hotZh}</Typography>
                  )}
                  {isSelected && <TrendingUpIcon sx={{ fontSize: 12, color: 'warning.main', flexShrink: 0 }} />}
                </Box>
              )
            })}
          </Stack>
        )}
      </Box>
    </Box>
  )
}
export function GenerateTabContent() {
  const toast = useToast()
  const { session, products, scripts } = useCoreData()
  const { handleScriptSave } = useEditor()
  const { isGenerating, generationProgress, generationMessage, genJustCompleted, slotTimeline, startGeneration, cancelGeneration } = useGeneration()
  const orderedProducts = useMemo(() => sortLiveProducts(products), [products])
  const orderedScripts = useMemo(() => sortLiveScripts(scripts), [scripts])
  const orderedTimeline = useMemo(
    () => [...slotTimeline].sort((a, b) => {
      const ai = a.index ?? a.sequenceNo ?? Number.MAX_SAFE_INTEGER
      const bi = b.index ?? b.sequenceNo ?? Number.MAX_SAFE_INTEGER
      return ai - bi || (a.timestamp ?? 0) - (b.timestamp ?? 0)
    }),
    [slotTimeline],
  )
  const completedSlotCount = orderedTimeline.filter(t => t.event === 'slot_done' && !t.failed).length
  const failedSlotCount = orderedTimeline.filter(t => t.failed).length
  const progressEventCount = orderedTimeline.filter(t => t.event === 'progress').length
  const previewTimeline = orderedTimeline.filter(t => t.event !== 'progress')
  const latestProgressSteps = orderedTimeline.filter(t => t.event === 'progress').slice(-8)

  // 基础配置
  const [primaryStyle, setPrimaryStyle] = useState(session?.scriptStyle ?? 'natural')
  const [styleBlend, setStyleBlend] = useState<string[]>([])
  const [durationPerSlot, setDurationPerSlot] = useState(180)
  const [useKbRef, setUseKbRef] = useState(true)
  const [useSkeleton, setUseSkeleton] = useState(false)
  const [personaId, setPersonaId] = useState<number | ''>(session?.personaId ?? '')
  const [modelId, setModelId] = useState<number | ''>()
  const [slotDurations, setSlotDurations] = useState<Record<number, number>>({})
  const [slotStyles, setSlotStyles] = useState<Record<number, string>>({})
  const [batchDur, setBatchDur] = useState(0)
  const [batchStyleVal, setBatchStyleVal] = useState('')

  // 组合风格 = 主风格 + 混搭（最多2个）
  const style = styleBlend.length > 0
    ? [primaryStyle, ...styleBlend].join(',')
    : primaryStyle

  // 高级配置
  const [extraPrompt, setExtraPrompt] = useState('')
  const [hotKeywords, setHotKeywords] = useState<string[]>([])
  const [interactionLevel, setInteractionLevel] = useState<string>('')
  const [retentionStrategy, setRetentionStrategy] = useState<string>('')
  const [materialType, setMaterialType] = useState<string>('')
  const [selectedPreset, setSelectedPreset] = useState<number | ''>('')
  const [focusScriptTypes, setFocusScriptTypes] = useState<string[]>([])
  const [showTimeline, setShowTimeline] = useState(false)
  const [generationError, setGenerationError] = useState<string | null>(null)

  const { data: presets = [], isError: presetsIsError, error: presetsError } = useQuery({
    queryKey: ['generation-presets'],
    queryFn: () => liveApi.presetList(),
    staleTime: 5 * 60 * 1000,
  })

  const { data: personas = [], isError: personasIsError, error: personasError } = useQuery({
    queryKey: ['dy-personas'],
    queryFn: () => douyinApi.personaList(),
    staleTime: 10 * 60 * 1000,
  })

  const canGenerate = orderedProducts.length > 0 && !isGenerating
  const hasScripts = orderedScripts.length > 0
  const activeGenerationEndpoint = useSkeleton
    ? GENERATE_TAB_STREAM_ENDPOINTS[1]
    : GENERATE_TAB_STREAM_ENDPOINTS[0]
  const pendingProcessSteps = useMemo(() => [
    {
      label: '读取场次、商品和已有话术',
      value: `${orderedProducts.length} 个商品 / ${orderedScripts.length} 条话术`,
      status: orderedProducts.length > 0 ? 'ready' : 'blocked',
    },
    {
      label: '确认生成接口',
      value: useSkeleton ? '骨架模式' : '完整流水线',
      status: 'ready',
    },
    {
      label: '锁定商品顺序',
      value: orderedProducts.length > 0
        ? orderedProducts.slice(0, 3).map((p, index) => `${index + 1}.${p.productName ?? `商品${p.productId}`}`).join(' / ')
        : '等待选品排品',
      status: orderedProducts.length > 0 ? 'ready' : 'blocked',
    },
    {
      label: '合成生成约束',
      value: `${SCRIPT_STYLES.find(s => s.value === primaryStyle)?.label ?? primaryStyle} · ${durationPerSlot}s · ${useKbRef ? '引用知识库' : '不引用知识库'}`,
      status: 'ready',
    },
  ], [durationPerSlot, orderedProducts, orderedScripts.length, primaryStyle, useKbRef, useSkeleton])

  const handleAddKeyword = (kw: string) => {
    const trimmed = kw.trim()
    if (trimmed && !hotKeywords.includes(trimmed)) {
      setHotKeywords(prev => [...prev, trimmed])
    }
  }

  const handleRemoveKeyword = (kw: string) => {
    setHotKeywords(prev => prev.filter(k => k !== kw))
  }

  const handleGenerate = async () => {
    try {
      setGenerationError(null)
      await startGeneration({
        style,
        durationLimitSec: durationPerSlot,
        useKbRef,
        mode: useSkeleton ? 'skeleton' : 'full',
        personaId: personaId !== '' ? personaId : undefined,
        extraPrompt: extraPrompt.trim() || undefined,
        hotKeywords: hotKeywords.length > 0 ? hotKeywords : undefined,
        interactionLevel: interactionLevel || undefined,
        retentionStrategy: retentionStrategy || undefined,
        materialType: materialType || undefined,
        modelId: modelId !== '' ? modelId : undefined,
      })
      setShowTimeline(true)
    } catch (e: unknown) {
      const message = `${activeGenerationEndpoint} 流式生成失败：${getErrorMessage(e)}`
      setGenerationError(message)
      toast(message, 'error')
    }
  }

  const applyPreset = (presetId: number) => {
    const p = presets.find(x => x.id === presetId)
    if (!p) return
    if (p.style) setPrimaryStyle(p.style)
    if (p.modelId) setModelId(p.modelId)
    if (typeof p.useKbRef === 'boolean') setUseKbRef(p.useKbRef)
    if (p.materialType) setMaterialType(p.materialType)
    setSelectedPreset(presetId)
  }

  return (
    <Box
      data-testid="generate-tab-workbench"
      data-contract-scope="live-ai-script-generation-orchestrator"
      data-ready-endpoints={GENERATE_TAB_READY_ENDPOINTS.join('|')}
      data-context-endpoints={GENERATE_TAB_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={GENERATE_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={session?.id ?? ''}
      data-products-count={orderedProducts.length}
      data-scripts-count={orderedScripts.length}
      data-can-generate={String(canGenerate)}
      data-generation-mode={useSkeleton ? 'skeleton' : 'full'}
      data-generation-endpoint={activeGenerationEndpoint}
      data-generation-status={isGenerating ? 'running' : genJustCompleted ? 'completed' : generationError ? 'error' : 'idle'}
      data-generation-progress={generationProgress}
      data-slot-timeline-count={slotTimeline.length}
      data-no-local-generated-script-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}
    >
      {/* 顶部操作条 */}
      <Box
        data-testid="generate-toolbar"
        data-contract-source={`${GENERATE_TAB_STREAM_ENDPOINTS.join('|')}|/live/generation-preset/list|/douyin/persona/list`}
        sx={{
          display: 'flex', alignItems: 'center', gap: 1.5, px: 2, py: 1.25,
          borderBottom: '1px solid', borderColor: 'divider', flexShrink: 0, bgcolor: 'background.paper',
        }}
      >
        <AutoAwesomeIcon sx={{ color: 'primary.main', fontSize: 20 }} />
        <Typography variant="subtitle2" fontWeight={700}>AI 话术生成</Typography>
        <Chip label={`${orderedProducts.length} 个商品`} size="small"
          color={orderedProducts.length > 0 ? 'primary' : 'default'} variant="outlined" />
        {hasScripts && <Chip label={`已有 ${orderedScripts.length} 条话术`} size="small" color="success" variant="outlined" />}
        {orderedProducts.length === 0 && (
          <Alert
            severity="warning"
            data-testid="generate-no-products-alert"
            data-contract-source="/live/product/by-session"
            data-no-local-product-fallback="true"
            sx={{ py: 0, px: 1, fontSize: 11, '& .MuiAlert-message': { py: 0.25 } }}
          >
            请先在「选品排品」添加商品
          </Alert>
        )}
        {generationError && (
          <Alert
            severity="error"
            data-testid="generate-stream-error-alert"
            data-contract-source={activeGenerationEndpoint}
            data-no-local-generated-script-fallback="true"
            sx={{ py: 0, px: 1, fontSize: 11, '& .MuiAlert-message': { py: 0.25 } }}
            onClose={() => setGenerationError(null)}
          >
            {generationError}
          </Alert>
        )}
        {presetsIsError && (
          <Alert
            severity="warning"
            data-testid="generate-preset-error-alert"
            data-contract-source="/live/generation-preset/list"
            data-no-local-preset-fallback="true"
            sx={{ py: 0, px: 1, fontSize: 11, '& .MuiAlert-message': { py: 0.25 } }}
          >
            /live/generation-preset/list 加载失败：{getErrorMessage(presetsError)}，已降级为自定义配置。
          </Alert>
        )}
        {personasIsError && (
          <Alert
            severity="warning"
            data-testid="generate-persona-error-alert"
            data-contract-source="/douyin/persona/list"
            data-no-local-persona-fallback="true"
            sx={{ py: 0, px: 1, fontSize: 11, '& .MuiAlert-message': { py: 0.25 } }}
          >
            /douyin/persona/list 加载失败：{getErrorMessage(personasError)}，已降级为默认人设。
          </Alert>
        )}
        <Box sx={{ flex: 1 }} />
        {presets.length > 0 && (
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel>快速预设</InputLabel>
            <Select
              label="快速预设"
              value={selectedPreset}
              inputProps={{ 'data-testid': 'generate-preset-select', 'data-contract-source': '/live/generation-preset/list' }}
              onChange={e => e.target.value !== '' && applyPreset(Number(e.target.value))}>
              <MenuItem value="">自定义配置</MenuItem>
              {presets.map(p => <MenuItem key={p.id} value={p.id}>{p.presetName}</MenuItem>)}
            </Select>
          </FormControl>
        )}
        {isGenerating && (
          <Button
            variant="outlined"
            color="error"
            size="medium"
            startIcon={<StopIcon />}
            onClick={cancelGeneration}
            data-testid="generate-cancel-button"
            data-contract-action="cancel-local-stream"
          >
            取消生成
          </Button>
        )}
        <Button
          variant="contained" size="medium"
          startIcon={isGenerating ? <CircularProgress size={16} color="inherit" /> : <AutoAwesomeIcon />}
          onClick={handleGenerate} disabled={!canGenerate}
          data-testid="generate-start-button"
          data-contract-source={activeGenerationEndpoint}
          data-no-local-generated-script-fallback="true"
          sx={{ minWidth: 120, fontWeight: 700 }}
        >
          {isGenerating ? '生成中...' : hasScripts ? '重新生成' : '开始生成'}
        </Button>
      </Box>

      {/* 四列主内容区 */}
      <Box
        data-testid="generate-main-columns"
        data-contract-source={`${GENERATE_TAB_CONTEXT_ENDPOINTS.join('|')}|${GENERATE_TAB_READY_ENDPOINTS.join('|')}`}
        sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}
      >

        {/* ── 第一列：热词采集 ── */}
        <Box
          data-testid="generate-hotwords-column"
          sx={{
            width: 220, flexShrink: 0,
            borderRight: '1px solid', borderColor: 'divider',
            overflow: 'hidden', display: 'flex', flexDirection: 'column',
          }}
        >
          <HotwordsPanel
            selected={hotKeywords}
            onAdd={handleAddKeyword}
            onRemove={handleRemoveKeyword}
          />
        </Box>

        {/* ── 第二列：基础设置 ── */}
        <Box
          data-testid="generate-settings-column"
          data-contract-source="/live/generation-preset/list|/douyin/persona/list"
          sx={{
            width: 480, flexShrink: 0,
            borderRight: '1px solid', borderColor: 'divider',
            overflow: 'auto',
          }}
        >
          <Box sx={{ p: 1.5, display: 'flex', flexDirection: 'column', gap: 1.75 }}>

            {/* 基础配置标题 */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
              <PsychologyIcon sx={{ fontSize: 14, color: 'primary.main' }} />
              <Typography variant="caption" fontWeight={700} color="primary.main" letterSpacing={0.5}>基础配置</Typography>
            </Box>

            {/* 人设 + 模型 */}
            <Box sx={{ display: 'flex', gap: 1 }}>
              <FormControl size="small" sx={{ flex: 1 }}>
                <InputLabel sx={{ fontSize: 12 }}><LabelTip label="主播人设" tip="决定话术的口吻、措辞风格和自我定位。不选则使用场次默认人设。" /></InputLabel>
                <Select
                  label="主播人设"
                  value={personaId}
                  inputProps={{ 'data-testid': 'generate-persona-select', 'data-contract-source': '/douyin/persona/list' }}
                  onChange={e => setPersonaId(e.target.value as number | '')}
                >
                  <MenuItem value="">默认人设</MenuItem>
                  {personas.map(p => (
                    <MenuItem key={p.id} value={p.id}>
                      <Box>
                        <Typography variant="body2">{p.personaName}</Typography>
                        {p.tone && <Typography variant="caption" color="text.secondary" display="block" noWrap sx={{ maxWidth: 100 }}>{p.tone}</Typography>}
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ flex: 1 }}>
                <InputLabel sx={{ fontSize: 12 }}><LabelTip label="AI 模型" tip={<span>指定生成所用的大模型。不选则使用系统默认（推荐）。<br />不同模型在话术风格、速度和成本上有所差异。</span>} /></InputLabel>
                <Select
                  label="AI 模型"
                  value={modelId}
                  inputProps={{ 'data-testid': 'generate-model-select', 'data-contract-source': '/live/generation-preset/list' }}
                  onChange={e => setModelId(e.target.value as number | '')}
                >
                  <MenuItem value="">默认模型</MenuItem>
                  {presets.filter(p => p.modelId).map(p => p.modelId).filter((v, i, a) => a.indexOf(v) === i).map(mid => (
                    <MenuItem key={mid} value={mid}>模型 #{mid}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>

            {/* 主风格 */}
            <FormControl size="small" fullWidth>
              <InputLabel><LabelTip label="主风格" tip={<span>话术的整体情感基调和表达方式。<br />· 自然流畅/友好亲切：适合日常种草<br />· 激情热情/促销冲量：适合大促节点<br />· 专业权威：适合功效型产品</span>} /></InputLabel>
              <Select
                label="主风格"
                value={primaryStyle}
                inputProps={{ 'data-testid': 'generate-primary-style-select' }}
                onChange={e => setPrimaryStyle(e.target.value)}
              >
                {(() => {
                  const groups = Array.from(new Set(SCRIPT_STYLES.map(s => s.group)))
                  return groups.flatMap(g => [
                    <MenuItem key={`g-${g}`} disabled sx={{ fontSize: 11, opacity: 0.6, py: 0.25 }}>{g}</MenuItem>,
                    ...SCRIPT_STYLES.filter(s => s.group === g).map(s => (
                      <MenuItem key={s.value} value={s.value}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', gap: 1 }}>
                          <Typography variant="body2">{s.label}</Typography>
                          <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>{s.desc}</Typography>
                        </Box>
                      </MenuItem>
                    )),
                  ])
                })()}
              </Select>
            </FormControl>

            {/* 混搭风格 */}
            <Box>
              <Typography variant="caption" color="text.secondary" display="block" mb={0.5}>
                <LabelTip label="混搭风格（最多2个）" tip={<span>在主风格基础上叠加1-2个辅助风格，让话术更有层次感。<br />例：主风格「专业权威」+混搭「种草安利」→ 既有专业背书，又有亲和力。<br />不选则纯主风格生成。</span>} />
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {SCRIPT_STYLES.filter(s => s.value !== primaryStyle).map(s => (
                  <Chip key={s.value} label={s.label} size="small"
                    variant={styleBlend.includes(s.value) ? 'filled' : 'outlined'}
                    color={styleBlend.includes(s.value) ? 'primary' : 'default'}
                    sx={{ fontSize: 10, cursor: 'pointer' }}
                    onClick={() => {
                      setStyleBlend(prev =>
                        prev.includes(s.value)
                          ? prev.filter(v => v !== s.value)
                          : prev.length < 2 ? [...prev, s.value] : prev
                      )
                    }}
                  />
                ))}
              </Box>
            </Box>

            {/* 每商品时长 */}
            <Box>
              <Typography variant="caption" color="text.secondary" display="block" mb={0.5}>
                <LabelTip label={`每商品时长：${durationPerSlot}s`} tip={<span>每个商品话术槽位的目标时长（秒）。AI 会根据此时长控制话术字数。<br />· 60s ≈ 150字（快节奏带货）<br />· 180s ≈ 450字（标准种草）<br />· 300s ≈ 750字（深度讲品）</span>} />
              </Typography>
              <Slider
                value={durationPerSlot}
                onChange={(_e, v) => setDurationPerSlot(v as number)}
                min={30} max={600} step={30}
                marks={[{ value: 60, label: '1m' }, { value: 180, label: '3m' }, { value: 300, label: '5m' }, { value: 600, label: '10m' }]}
                valueLabelDisplay="auto"
                valueLabelFormat={v => `${v}s`}
                size="small"
              />
            </Box>

            <Divider />

            {/* 高级配置折叠 */}
            <Accordion disableGutters elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, '&:before': { display: 'none' } }}>
              <AccordionSummary expandIcon={<ExpandMoreIcon sx={{ fontSize: 16 }} />} sx={{ minHeight: 36, px: 1.5, '& .MuiAccordionSummary-content': { my: 0.5 } }}>
                <Typography variant="caption" fontWeight={600} color="text.secondary">高级配置</Typography>
              </AccordionSummary>
              <AccordionDetails sx={{ px: 1.5, pb: 1.5, pt: 0.5, display: 'flex', flexDirection: 'column', gap: 1.5 }}>

                {/* 互动频率 */}
                <FormControl size="small" fullWidth>
                  <InputLabel sx={{ fontSize: 12 }}><LabelTip label="互动频率" tip={<span>控制话术中互动指令（点赞、关注、评论、加购）的密度。<br />· 低频：专注内容，减少打断，适合深度讲品<br />· 中频：标准节奏，均衡种草和互动<br />· 高频：频繁引导，适合大促冲单节点</span>} /></InputLabel>
                  <Select label="互动频率" value={interactionLevel} onChange={e => setInteractionLevel(e.target.value)}>
                    <MenuItem value="">AI 自动</MenuItem>
                    <MenuItem value="low">低频</MenuItem>
                    <MenuItem value="medium">中频</MenuItem>
                    <MenuItem value="high">高频</MenuItem>
                  </Select>
                </FormControl>

                {/* 留人策略 */}
                <FormControl size="small" fullWidth>
                  <InputLabel sx={{ fontSize: 12 }}><LabelTip label="留人策略" tip={<span>话术结尾/过渡部分的留观引导方式。<br />· 福利留：预告下一件超值产品<br />· 情感留：建立情感连接，唤起归属感<br />· 悬念留：埋下钩子，吊足胃口</span>} /></InputLabel>
                  <Select label="留人策略" value={retentionStrategy} onChange={e => setRetentionStrategy(e.target.value)}>
                    <MenuItem value="">AI 自动</MenuItem>
                    <MenuItem value="benefit">福利留</MenuItem>
                    <MenuItem value="emotional">情感留</MenuItem>
                    <MenuItem value="suspense">悬念留</MenuItem>
                  </Select>
                </FormControl>

                <Box
                  data-testid="generate-material-type-options"
                  data-contract-source="/ai/knowledge-base/huashu/search|/tianapi/material/import"
                  data-selected-material-type={materialType}
                >
                  <Typography variant="caption" color="text.secondary" display="block" mb={0.5}>
                    <LabelTip label="素材融合" tip={<span>选择后会把素材类型传给后端 RAG：优先检索 huashu 话术库里的 TianAPI/本地素材，再改写融合进直播话术。<br />顺口溜、歇后语、金句等不会直接当原文照搬，而是变成口播记忆点。</span>} />
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {MATERIAL_TYPE_OPTIONS.map(item => (
                      <Tooltip key={item.value} title={item.desc} arrow>
                        <Chip
                          label={item.label}
                          size="small"
                          color={materialType === item.value ? 'secondary' : 'default'}
                          variant={materialType === item.value ? 'filled' : 'outlined'}
                          onClick={() => setMaterialType(item.value)}
                          data-testid={`generate-material-type-${item.value || 'default'}`}
                          sx={{ height: 24, fontSize: 10, cursor: 'pointer' }}
                        />
                      </Tooltip>
                    ))}
                  </Box>
                </Box>

                {/* 重点话术类型 */}
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block" mb={0.5}>
                    <LabelTip label="重点话术类型" tip={<span>指定需要 AI 重点强化的话术环节。不选则 AI 自动均衡分配。<br />· 开场：冷启动吸粉<br />· 讲品：产品深度介绍<br />· 促销：限时优惠催单<br />· 互动：点赞/评论引导<br />· 留人：防流失钩子<br />· 结尾：转化收口</span>} />
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {SCRIPT_TYPE_OPTIONS.map(t => (
                      <Chip key={t.value} label={t.label}
                        size="small"
                        variant={focusScriptTypes.includes(t.value) ? 'filled' : 'outlined'}
                        color={focusScriptTypes.includes(t.value) ? (t.color as ChipProps['color']) : 'default'}
                        sx={{ fontSize: 10, cursor: 'pointer' }}
                        onClick={() => {
                          setFocusScriptTypes(prev =>
                            prev.includes(t.value)
                              ? prev.filter(v => v !== t.value)
                              : [...prev, t.value]
                          )
                        }}
                      />
                    ))}
                  </Box>
                </Box>

                {/* 自定义要求 */}
                <TextField
                  size="small" fullWidth multiline minRows={2}
                  label={<LabelTip label="额外生成要求" tip={<span>自由输入对本次生成的补充指令，直接传递给 AI。<br />示例：<br />· 「结合双十一氛围，每个产品末尾加限时倒计时」<br />· 「不要提竞品，多强调回购率」<br />优先级高于风格设置，最多300字</span>} />}
                  placeholder="例：结合双十一活动氛围…"
                  value={extraPrompt}
                  onChange={e => setExtraPrompt(e.target.value)}
                  inputProps={{ maxLength: 300, 'data-testid': 'generate-extra-prompt-input' }}
                  helperText={`${extraPrompt.length}/300`}
                />

              </AccordionDetails>
            </Accordion>

            {/* 引用知识库 + 骨架模式 */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
              <FormControlLabel
                data-testid="generate-kb-ref-switch"
                control={<Switch size="small" checked={useKbRef} onChange={e => setUseKbRef(e.target.checked)} />}
                label={
                  <Tooltip arrow placement="right" title={<span>开启后 AI 生成时会检索知识库（RAG），引用品牌话术规范、产品卖点文档、历史优质话术等内容，提升准确性和品牌一致性。<br />关闭则仅依赖大模型本身的知识，速度略快但可能缺乏品牌专属表达。</span>}>
                    <Typography variant="body2" sx={{ cursor: 'help', borderBottom: '1px dashed', borderColor: 'text.secondary' }}>引用知识库</Typography>
                  </Tooltip>
                }
              />
              <FormControlLabel
                data-testid="generate-skeleton-switch"
                control={<Switch size="small" checked={useSkeleton} onChange={e => setUseSkeleton(e.target.checked)} />}
                label={
                  <Tooltip arrow placement="right" title={
                    <Box sx={{ p: 0.5, maxWidth: 260 }}>
                      <Typography variant="caption" fontWeight={700} display="block" mb={0.5}>骨架模式（两阶段生成）</Typography>
                      <Typography variant="caption" display="block" mb={0.5}>
                        第一阶段：生成话术骨架（结构提纲 + 关键词）<br />
                        第二阶段：逐段填充完整话术内容
                      </Typography>
                      <Typography variant="caption" color="warning.light" display="block" mb={0.5}>
                        ✦ 适用：逻辑结构复杂、需要人工校对骨架后再展开的场景
                      </Typography>
                      <Typography variant="caption" color="error.light" display="block">
                        ✗ 不适用：快速批量生成 — 速度约为普通模式的 1/2
                      </Typography>
                    </Box>
                  }>
                    <Typography variant="body2" sx={{ cursor: 'help', borderBottom: '1px dashed', borderColor: 'text.secondary' }}>骨架模式</Typography>
                  </Tooltip>
                }
              />
            </Box>

          </Box>{/* end p:1.5 */}
        </Box>{/* end 第二列 */}

        {/* ── 第三列：槽位配置（时长 + 风格）── */}
        <Box
          data-testid="generate-slot-config-column"
          data-contract-source="/live/product/by-session"
          data-no-direct-product-mutation="true"
          sx={{
            width: 520, flexShrink: 0,
            borderRight: '1px solid', borderColor: 'divider',
            overflow: 'hidden', display: 'flex', flexDirection: 'column',
          }}
        >
          {/* 标题栏 */}
          <Box sx={{ px: 1.5, pt: 1.5, pb: 1, flexShrink: 0, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
              <AccessTimeIcon sx={{ fontSize: 14, color: 'warning.main' }} />
              <Typography variant="caption" fontWeight={700} color="warning.main" letterSpacing={0.5}>槽位配置</Typography>
              <Typography variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>（每商品覆盖全局设置）</Typography>
            </Box>
          </Box>
          {/* 批量操作栏 */}
          {orderedProducts.length > 0 && (
            <Box
              data-testid="generate-slot-batch-settings-surface"
              sx={(theme) => ({
                px: 1.5,
                py: 1,
                borderBottom: '1px solid',
                borderColor: 'divider',
                bgcolor: theme.palette.mode === 'dark'
                  ? theme.palette.background.default
                  : alpha(theme.palette.common.black, 0.025),
                flexShrink: 0,
              })}
            >
              <Typography variant="caption" fontWeight={600} color="text.secondary" display="block" mb={0.75}>批量设置</Typography>
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                <FormControl size="small" sx={{ minWidth: 110 }}>
                  <InputLabel sx={{ fontSize: 11 }}>批量时长</InputLabel>
                  <Select
                    label="批量时长"
                    value={batchDur}
                    onChange={e => setBatchDur(Number(e.target.value))}
                    sx={{ fontSize: 11 }}
                  >
                    <MenuItem value={0}><em>不设置</em></MenuItem>
                    {[60, 120, 180, 300, 600].map(s => <MenuItem key={s} value={s} sx={{ fontSize: 11 }}>{s}s</MenuItem>)}
                  </Select>
                </FormControl>
                <FormControl size="small" sx={{ minWidth: 130 }}>
                  <InputLabel sx={{ fontSize: 11 }}>批量风格</InputLabel>
                  <Select
                    label="批量风格"
                    value={batchStyleVal}
                    onChange={e => setBatchStyleVal(e.target.value)}
                    sx={{ fontSize: 11 }}
                  >
                    <MenuItem value=""><em>不设置</em></MenuItem>
                    {SCRIPT_STYLES.map(s => <MenuItem key={s.value} value={s.value} sx={{ fontSize: 11 }}>{s.label}</MenuItem>)}
                  </Select>
                </FormControl>
                <Button
                  size="small" variant="outlined"
                  onClick={() => {
                    const newDurs: Record<number, number> = {}
                    const newStyles: Record<number, string> = {}
                    orderedProducts.forEach(p => {
                      if (batchDur > 0) newDurs[p.id] = batchDur
                      if (batchStyleVal) newStyles[p.id] = batchStyleVal
                    })
                    if (batchDur > 0) setSlotDurations(prev => ({ ...prev, ...newDurs }))
                    if (batchStyleVal) setSlotStyles(prev => ({ ...prev, ...newStyles }))
                  }}
                  disabled={batchDur === 0 && batchStyleVal === ''}
                  sx={{ fontSize: 11, height: 32 }}
                >应用全部</Button>
                <Button
                  size="small" variant="text" color="inherit"
                  onClick={() => { setSlotDurations({}); setSlotStyles({}) }}
                  sx={{ fontSize: 11, height: 32, color: 'text.secondary' }}
                >清空覆盖</Button>
              </Box>
            </Box>
          )}
          {/* 列表 */}
          {orderedProducts.length === 0 ? (
            <Box
              data-testid="generate-slot-empty"
              data-contract-source="/live/product/by-session"
              data-no-local-product-fallback="true"
              sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2 }}
            >
              <Typography variant="caption" color="text.disabled" textAlign="center">暂无商品</Typography>
            </Box>
          ) : (
            <Stack
              divider={<Divider />}
              data-testid="generate-product-slot-list"
              data-contract-source="/live/product/by-session"
              sx={{ flex: 1, overflow: 'auto' }}
            >
              {orderedProducts.map((prod, idx) => {
                const dur = slotDurations[prod.id] ?? 0
                const slotStyle = slotStyles[prod.id] ?? ''
                const DUR_PRESETS = [60, 120, 180, 300]
                return (
                  <Box key={prod.id} data-testid="generate-product-slot-row" data-product-id={prod.productId} sx={{ px: 1.5, py: 1 }}>
                    {/* 商品名 */}
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 0.75 }}>
                      <Typography variant="caption" sx={{ width: 16, textAlign: 'center', fontWeight: 700, color: 'text.disabled', fontSize: 10 }}>{idx + 1}</Typography>
                      <Typography variant="caption" fontWeight={600} noWrap sx={{ flex: 1 }}>
                        {prod.productName ?? `商品${idx + 1}`}
                      </Typography>
                      {(dur > 0 || slotStyle) && (
                        <Chip label="已覆盖" size="small" color="warning" variant="outlined" sx={{ fontSize: 9, height: 16 }} />
                      )}
                    </Box>
                    {/* 时长 + 风格两行 */}
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                      {/* 时长 */}
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="caption" color="text.secondary" display="block" mb={0.4}>时长</Typography>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.4 }}>
                          {DUR_PRESETS.map(sec => (
                            <Chip key={sec} label={`${sec}s`} size="small"
                              variant={dur === sec ? 'filled' : 'outlined'}
                              color={dur === sec ? 'warning' : 'default'}
                              onClick={() => setSlotDurations(prev => ({ ...prev, [prod.id]: dur === sec ? 0 : sec }))}
                              sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
                            />
                          ))}
                          <Chip label="全局" size="small"
                            variant={dur === 0 ? 'filled' : 'outlined'}
                            color="default"
                            onClick={() => setSlotDurations(prev => ({ ...prev, [prod.id]: 0 }))}
                            sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
                          />
                        </Box>
                      </Box>
                      {/* 风格 */}
                      <Box sx={{ minWidth: 130 }}>
                        <Typography variant="caption" color="text.secondary" display="block" mb={0.4}>风格</Typography>
                        <FormControl size="small" fullWidth>
                          <Select
                            value={slotStyle}
                            onChange={e => setSlotStyles(prev => ({ ...prev, [prod.id]: e.target.value }))}
                            displayEmpty
                            sx={{ fontSize: 11 }}
                          >
                            <MenuItem value=""><em>继承全局</em></MenuItem>
                            {SCRIPT_STYLES.map(s => <MenuItem key={s.value} value={s.value} sx={{ fontSize: 11 }}>{s.label}</MenuItem>)}
                          </Select>
                        </FormControl>
                      </Box>
                    </Box>
                  </Box>
                )
              })}
            </Stack>
          )}
        </Box>{/* end 第三列 */}

        {/* ── 第四列：流式生成可视化进度 ── */}
        <Box
          data-testid="generate-progress-column"
          data-contract-source={GENERATE_TAB_STREAM_ENDPOINTS.join('|')}
          data-no-local-generated-script-fallback="true"
          sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column' }}
        >
          {/* 进度区 */}
          {(isGenerating || genJustCompleted) && (
            <Box
              data-testid="generate-progress-surface"
              data-contract-source={activeGenerationEndpoint}
              data-generation-progress={generationProgress}
              data-generation-status={isGenerating ? 'running' : 'completed'}
              data-progress-event-count={progressEventCount}
              data-completed-slot-count={completedSlotCount}
              data-failed-slot-count={failedSlotCount}
              sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider', flexShrink: 0 }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                {isGenerating
                  ? <CircularProgress size={14} />
                  : <CheckCircleIcon sx={{ fontSize: 16, color: 'success.main' }} />
                }
                <Typography variant="caption" fontWeight={600}>
                  {isGenerating ? '生成中' : '生成完成'}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>{generationMessage}</Typography>
              </Box>
              <LinearProgress
                variant={isGenerating ? 'determinate' : 'determinate'}
                value={generationProgress}
                color={genJustCompleted ? 'success' : 'primary'}
                sx={{ height: 6, borderRadius: 3, mb: 0.5 }}
              />
              <Typography variant="caption" color="text.secondary">{generationProgress}%</Typography>
              <Stack direction="row" spacing={0.75} sx={{ mt: 1, flexWrap: 'wrap', rowGap: 0.75 }}>
                <Chip label={`过程 ${progressEventCount}`} size="small" variant="outlined" sx={{ fontSize: 10, height: 20 }} />
                <Chip label={`完成 ${completedSlotCount}`} size="small" color="success" variant="outlined" sx={{ fontSize: 10, height: 20 }} />
                {failedSlotCount > 0 && <Chip label={`失败 ${failedSlotCount}`} size="small" color="error" variant="outlined" sx={{ fontSize: 10, height: 20 }} />}
              </Stack>
            </Box>
          )}

          {/* 槽位时间线 */}
          <Box
            data-testid="generate-timeline-list"
            data-contract-source={activeGenerationEndpoint}
            data-slot-timeline-count={orderedTimeline.length}
            data-progress-event-count={progressEventCount}
            data-slot-result-count={previewTimeline.length}
            data-process-state={orderedTimeline.length > 0 ? 'streaming-or-completed' : 'idle-ready'}
            sx={{ p: 1.5, flexShrink: 0 }}
          >
            <Typography variant="caption" fontWeight={600} color="text.secondary" display="block" mb={1}>
              生成过程（{orderedTimeline.length} 条事件）
            </Typography>
            <Paper
              variant="outlined"
              data-testid="generate-process-plan-surface"
              data-contract-source="/live/session/get|/live/product/by-session|/live/script/by-session"
              data-product-order-ready={String(orderedProducts.length > 0)}
              sx={{ borderRadius: 1, overflow: 'hidden', mb: 1 }}
            >
              {pendingProcessSteps.map((step, index) => (
                <Box
                  key={step.label}
                  data-testid="generate-process-plan-row"
                  data-process-step-status={step.status}
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: '30px 1fr',
                    gap: 1,
                    px: 1,
                    py: 0.75,
                    borderBottom: index < pendingProcessSteps.length - 1 ? '1px solid' : 0,
                    borderColor: 'divider',
                  }}
                >
                  <Chip
                    label={index + 1}
                    size="small"
                    color={step.status === 'blocked' ? 'warning' : 'primary'}
                    variant="outlined"
                    sx={{ fontSize: 9, height: 18, minWidth: 24 }}
                  />
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" fontWeight={600} display="block" noWrap>{step.label}</Typography>
                    <Typography variant="caption" color="text.secondary" display="block" noWrap>{step.value}</Typography>
                  </Box>
                </Box>
              ))}
            </Paper>
            {orderedTimeline.length === 0 && (
              <Alert
                severity={orderedProducts.length > 0 ? 'info' : 'warning'}
                data-testid="generate-process-idle-hint"
                data-contract-source={activeGenerationEndpoint}
                sx={{ fontSize: 12, mb: 1 }}
              >
                {orderedProducts.length > 0
                  ? '开始生成后，这里会按商品顺序记录进度、槽位完成、失败原因和内容预览。'
                  : '当前没有可生成商品，请先在「选品排品」添加商品。'}
              </Alert>
            )}
            {latestProgressSteps.length > 0 && (
                <Paper
                  variant="outlined"
                  data-testid="generate-process-detail-surface"
                  data-progress-event-count={progressEventCount}
                  sx={{ borderRadius: 1, overflow: 'hidden', mb: 1 }}
                >
                  {latestProgressSteps.map((t, i) => (
                    <Box
                      key={`${t.timestamp ?? i}-${t.current ?? i}`}
                      data-testid="generate-process-step-row"
                      sx={{
                        display: 'grid',
                        gridTemplateColumns: '44px 1fr auto',
                        gap: 1,
                        alignItems: 'center',
                        px: 1,
                        py: 0.75,
                        borderBottom: i < latestProgressSteps.length - 1 ? '1px solid' : 0,
                        borderColor: 'divider',
                      }}
                    >
                      <Typography variant="caption" color="text.secondary">#{t.current ?? i + 1}</Typography>
                      <Typography variant="caption" sx={{ minWidth: 0 }} noWrap>{t.stage || t.slotLabel}</Typography>
                      <Chip label={`${t.percent ?? 0}%`} size="small" variant="outlined" sx={{ fontSize: 9, height: 18 }} />
                    </Box>
                  ))}
                </Paper>
            )}
            {previewTimeline.length > 0 && (
              <Stack spacing={0.5}>
                {previewTimeline.map((t, i) => (
                  <Box key={i} data-testid={t.failed ? 'generate-timeline-failed-surface' : 'generate-timeline-success-surface'} sx={(theme) => ({
                    display: 'flex', gap: 1, alignItems: 'flex-start',
                    p: 0.75, borderRadius: 0.75,
                    bgcolor: alpha(
                      t.failed ? theme.palette.error.main : theme.palette.success.main,
                      theme.palette.mode === 'dark' ? 0.16 : 0.1,
                    ),
                    border: '1px solid',
                    borderColor: alpha(
                      t.failed ? theme.palette.error.main : theme.palette.success.main,
                      theme.palette.mode === 'dark' ? 0.45 : 0.28,
                    ),
                  })}>
                    <Chip
                      label={`#${t.sequenceNo ?? i + 1}`}
                      size="small"
                      variant="outlined"
                      color={t.failed ? 'error' : 'success'}
                      sx={{ fontSize: 10, minWidth: 36, height: 18 }}
                    />
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <Typography variant="caption" fontWeight={600} color={t.failed ? 'error.main' : 'inherit'} noWrap>{t.slotLabel}</Typography>
                        {t.scriptType && (
                          <Chip
                            label={SCRIPT_TYPE_OPTIONS.find(o => o.value === t.scriptType)?.label ?? t.scriptType}
                            size="small" variant="outlined"
                            sx={{ fontSize: 9, height: 16 }}
                          />
                        )}
                      </Box>
                      {t.errorMsg && <Typography variant="caption" color="error.main" display="block">{t.errorMsg}</Typography>}
                      {t.content && (
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.25, whiteSpace: 'pre-wrap' }}>
                          {t.content.length > 90 ? `${t.content.slice(0, 90)}...` : t.content}
                        </Typography>
                      )}
                    </Box>
                    {!t.failed && <CheckCircleIcon sx={{ fontSize: 14, color: 'success.main', flexShrink: 0, mt: 0.1 }} />}
                  </Box>
                ))}
              </Stack>
            )}
          </Box>

          {/* 槽位精调（生成完成后） */}
          {genJustCompleted && showTimeline && orderedScripts.length > 0 && (
            <Box
              data-testid="generate-finetune-panel"
              data-contract-source="/live/script/save"
              data-no-direct-script-create="true"
              sx={{ p: 1.5, borderTop: '1px solid', borderColor: 'divider' }}
            >
              <Typography variant="caption" fontWeight={600} color="text.secondary" display="block" mb={1}>
                精调各槽位参数
              </Typography>
              <Paper variant="outlined" sx={{ borderRadius: 1, overflow: 'hidden' }}>
                {orderedScripts.map(s => (
                  <SlotConfigRow key={s.id} script={s} sessionId={session!.id} onSave={handleScriptSave} />
                ))}
              </Paper>
            </Box>
          )}

          {/* 空状态 */}
          {!isGenerating && !genJustCompleted && orderedTimeline.length === 0 && orderedProducts.length === 0 && (
            <Box
              data-testid="generate-idle-empty"
              data-contract-source={activeGenerationEndpoint}
              data-no-local-generated-script-fallback="true"
              sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 1.5, p: 3 }}
            >
              <AutoAwesomeIcon sx={{ fontSize: 40, color: 'text.disabled' }} />
              <Typography variant="body2" color="text.secondary" textAlign="center">
                配置左侧参数后点击「开始生成」<br />实时进度将在此显示
              </Typography>
              {orderedProducts.length === 0 && (
                <Alert
                  severity="warning"
                  data-testid="generate-empty-no-products-alert"
                  data-contract-source="/live/product/by-session"
                  data-no-local-product-fallback="true"
                  sx={{ fontSize: 12 }}
                >
                  请先在「选品排品」标签页添加商品
                </Alert>
              )}
            </Box>
          )}
        </Box>{/* end 第四列 */}

      </Box>{/* end 四列 */}
    </Box>
  )
}
