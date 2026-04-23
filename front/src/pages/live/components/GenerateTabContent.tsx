import { useState } from 'react'
import {
  Box, Button, Typography, LinearProgress, Alert,
  FormControl, InputLabel, Select, MenuItem, Slider,
  Chip, CircularProgress, FormControlLabel, Switch, Divider, Stack,
  TextField, Accordion, AccordionSummary, AccordionDetails,
  Tooltip, Paper, Tab, Tabs, IconButton,
} from '@mui/material'
import type { ChipProps } from '@mui/material'
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
    <Box sx={{ px: 1.5, py: 1.25, borderBottom: '1px solid', borderColor: 'divider', '&:last-child': { borderBottom: 'none' } }}>
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

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      {/* 标题 */}
      <Box sx={{ px: 1.5, pt: 1.5, pb: 1, flexShrink: 0 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 1 }}>
          <LocalFireDepartmentIcon sx={{ fontSize: 14, color: 'error.main' }} />
          <Typography variant="caption" fontWeight={700} color="error.main" letterSpacing={0.5}>热词采集</Typography>
          <Box sx={{ flex: 1 }} />
          <Tooltip title="刷新榜单">
            <span>
              <IconButton size="small" onClick={() => refetch()} disabled={isFetching} sx={{ p: 0.25 }}>
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
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'center', pt: 2 }}>暂无数据（需配置 TianAPI）</Typography>
        ) : (
          <Stack spacing={0}>
            {hotItems.slice(0, 30).map((item, idx) => {
              const isSelected = selected.includes(item.word)
              return (
                <Box
                  key={item.word + idx}
                  onClick={() => isSelected ? onRemove(item.word) : onAdd(item.word)}
                  sx={{
                    display: 'flex', alignItems: 'center', gap: 0.75, px: 0.75, py: 0.6,
                    cursor: 'pointer', borderRadius: 0.75,
                    bgcolor: isSelected ? 'warning.50' : 'transparent',
                    border: '1px solid',
                    borderColor: isSelected ? 'warning.300' : 'transparent',
                    '&:hover': { bgcolor: isSelected ? 'warning.100' : 'action.hover' },
                    transition: 'all 0.15s',
                  }}
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
  const [selectedPreset, setSelectedPreset] = useState<number | ''>('')
  const [focusScriptTypes, setFocusScriptTypes] = useState<string[]>([])
  const [showTimeline, setShowTimeline] = useState(false)

  const { data: presets = [] } = useQuery({
    queryKey: ['generation-presets'],
    queryFn: () => liveApi.presetList(),
    staleTime: 5 * 60 * 1000,
  })

  const { data: personas = [] } = useQuery({
    queryKey: ['dy-personas'],
    queryFn: () => douyinApi.personaList(),
    staleTime: 10 * 60 * 1000,
  })

  const canGenerate = products.length > 0 && !isGenerating
  const hasScripts = scripts.length > 0

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
        modelId: modelId !== '' ? modelId : undefined,
      })
      setShowTimeline(true)
    } catch (e: unknown) {
      toast((e as Error).message ?? '生成失败', 'error')
    }
  }

  const applyPreset = (presetId: number) => {
    const p = presets.find(x => x.id === presetId)
    if (!p) return
    if (p.style) setPrimaryStyle(p.style)
    if (p.modelId) setModelId(p.modelId)
    if (typeof p.useKbRef === 'boolean') setUseKbRef(p.useKbRef)
    setSelectedPreset(presetId)
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      {/* 顶部操作条 */}
      <Box sx={{
        display: 'flex', alignItems: 'center', gap: 1.5, px: 2, py: 1.25,
        borderBottom: '1px solid', borderColor: 'divider', flexShrink: 0, bgcolor: 'background.paper',
      }}>
        <AutoAwesomeIcon sx={{ color: 'primary.main', fontSize: 20 }} />
        <Typography variant="subtitle2" fontWeight={700}>AI 话术生成</Typography>
        <Chip label={`${products.length} 个商品`} size="small"
          color={products.length > 0 ? 'primary' : 'default'} variant="outlined" />
        {hasScripts && <Chip label={`已有 ${scripts.length} 条话术`} size="small" color="success" variant="outlined" />}
        {products.length === 0 && (
          <Alert severity="warning" sx={{ py: 0, px: 1, fontSize: 11, '& .MuiAlert-message': { py: 0.25 } }}>请先在「选品排品」添加商品</Alert>
        )}
        <Box sx={{ flex: 1 }} />
        {presets.length > 0 && (
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel>快速预设</InputLabel>
            <Select label="快速预设" value={selectedPreset}
              onChange={e => e.target.value !== '' && applyPreset(Number(e.target.value))}>
              <MenuItem value="">自定义配置</MenuItem>
              {presets.map(p => <MenuItem key={p.id} value={p.id}>{p.presetName}</MenuItem>)}
            </Select>
          </FormControl>
        )}
        {isGenerating && (
          <Button variant="outlined" color="error" size="medium" startIcon={<StopIcon />} onClick={cancelGeneration}>
            取消生成
          </Button>
        )}
        <Button
          variant="contained" size="medium"
          startIcon={isGenerating ? <CircularProgress size={16} color="inherit" /> : <AutoAwesomeIcon />}
          onClick={handleGenerate} disabled={!canGenerate}
          sx={{ minWidth: 120, fontWeight: 700 }}
        >
          {isGenerating ? '生成中...' : hasScripts ? '重新生成' : '开始生成'}
        </Button>
      </Box>

      {/* 四列主内容区 */}
      <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

        {/* ── 第一列：热词采集 ── */}
        <Box sx={{
          width: 220, flexShrink: 0,
          borderRight: '1px solid', borderColor: 'divider',
          overflow: 'hidden', display: 'flex', flexDirection: 'column',
        }}>
          <HotwordsPanel
            selected={hotKeywords}
            onAdd={handleAddKeyword}
            onRemove={handleRemoveKeyword}
          />
        </Box>

        {/* ── 第二列：基础设置 ── */}
        <Box sx={{
          width: 480, flexShrink: 0,
          borderRight: '1px solid', borderColor: 'divider',
          overflow: 'auto',
        }}>
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
                <Select label="主播人设" value={personaId} onChange={e => setPersonaId(e.target.value as number | '')}>
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
                <Select label="AI 模型" value={modelId} onChange={e => setModelId(e.target.value as number | '')}>
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
              <Select label="主风格" value={primaryStyle} onChange={e => setPrimaryStyle(e.target.value)}>
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
                  inputProps={{ maxLength: 300 }}
                  helperText={`${extraPrompt.length}/300`}
                />

              </AccordionDetails>
            </Accordion>

            {/* 引用知识库 + 骨架模式 */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
              <FormControlLabel
                control={<Switch size="small" checked={useKbRef} onChange={e => setUseKbRef(e.target.checked)} />}
                label={
                  <Tooltip arrow placement="right" title={<span>开启后 AI 生成时会检索知识库（RAG），引用品牌话术规范、产品卖点文档、历史优质话术等内容，提升准确性和品牌一致性。<br />关闭则仅依赖大模型本身的知识，速度略快但可能缺乏品牌专属表达。</span>}>
                    <Typography variant="body2" sx={{ cursor: 'help', borderBottom: '1px dashed', borderColor: 'text.secondary' }}>引用知识库</Typography>
                  </Tooltip>
                }
              />
              <FormControlLabel
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
        <Box sx={{
          width: 520, flexShrink: 0,
          borderRight: '1px solid', borderColor: 'divider',
          overflow: 'hidden', display: 'flex', flexDirection: 'column',
        }}>
          {/* 标题栏 */}
          <Box sx={{ px: 1.5, pt: 1.5, pb: 1, flexShrink: 0, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
              <AccessTimeIcon sx={{ fontSize: 14, color: 'warning.main' }} />
              <Typography variant="caption" fontWeight={700} color="warning.main" letterSpacing={0.5}>槽位配置</Typography>
              <Typography variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>（每商品覆盖全局设置）</Typography>
            </Box>
          </Box>
          {/* 批量操作栏 */}
          {products.length > 0 && (
            <Box sx={{ px: 1.5, py: 1, borderBottom: '1px solid', borderColor: 'divider', bgcolor: 'grey.50', flexShrink: 0 }}>
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
                    products.forEach(p => {
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
          {products.length === 0 ? (
            <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2 }}>
              <Typography variant="caption" color="text.disabled" textAlign="center">暂无商品</Typography>
            </Box>
          ) : (
            <Stack divider={<Divider />} sx={{ flex: 1, overflow: 'auto' }}>
              {products.map((prod, idx) => {
                const dur = slotDurations[prod.id] ?? 0
                const slotStyle = slotStyles[prod.id] ?? ''
                const DUR_PRESETS = [60, 120, 180, 300]
                return (
                  <Box key={prod.id} sx={{ px: 1.5, py: 1 }}>
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
        <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
          {/* 进度区 */}
          {(isGenerating || genJustCompleted) && (
            <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider', flexShrink: 0 }}>
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
            </Box>
          )}

          {/* 槽位时间线 */}
          {slotTimeline.length > 0 && (
            <Box sx={{ p: 1.5, flexShrink: 0 }}>
              <Typography variant="caption" fontWeight={600} color="text.secondary" display="block" mb={1}>
                生成时间线（{slotTimeline.length} 个槽位）
              </Typography>
              <Stack spacing={0.5}>
                {slotTimeline.map((t, i) => (
                  <Box key={i} sx={{
                    display: 'flex', gap: 1, alignItems: 'flex-start',
                    p: 0.75, borderRadius: 0.75,
                    bgcolor: t.failed ? 'error.50' : 'success.50',
                    border: '1px solid',
                    borderColor: t.failed ? 'error.200' : 'success.200',
                  }}>
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
                    </Box>
                    {!t.failed && <CheckCircleIcon sx={{ fontSize: 14, color: 'success.main', flexShrink: 0, mt: 0.1 }} />}
                  </Box>
                ))}
              </Stack>
            </Box>
          )}

          {/* 槽位精调（生成完成后） */}
          {genJustCompleted && showTimeline && scripts.length > 0 && (
            <Box sx={{ p: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
              <Typography variant="caption" fontWeight={600} color="text.secondary" display="block" mb={1}>
                精调各槽位参数
              </Typography>
              <Paper variant="outlined" sx={{ borderRadius: 1, overflow: 'hidden' }}>
                {scripts.map(s => (
                  <SlotConfigRow key={s.id} script={s} sessionId={session!.id} onSave={handleScriptSave} />
                ))}
              </Paper>
            </Box>
          )}

          {/* 空状态 */}
          {!isGenerating && !genJustCompleted && slotTimeline.length === 0 && (
            <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 1.5, p: 3 }}>
              <AutoAwesomeIcon sx={{ fontSize: 40, color: 'text.disabled' }} />
              <Typography variant="body2" color="text.secondary" textAlign="center">
                配置左侧参数后点击「开始生成」<br />实时进度将在此显示
              </Typography>
              {products.length === 0 && (
                <Alert severity="warning" sx={{ fontSize: 12 }}>请先在「选品排品」标签页添加商品</Alert>
              )}
            </Box>
          )}
        </Box>{/* end 第四列 */}

      </Box>{/* end 四列 */}
    </Box>
  )
}





