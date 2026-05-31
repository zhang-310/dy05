import { memo, useEffect, useMemo, useState, useCallback } from 'react'
import {
  Box, Typography, Chip, Button, IconButton, TextField,
  Collapse, LinearProgress, Tooltip, Stack,
  ToggleButtonGroup, ToggleButton, Alert, CircularProgress, Menu, MenuItem,
  Dialog, DialogTitle, DialogContent, DialogActions,
  List, ListItem, ListItemText, Divider, Paper, Badge, Popover,
  InputAdornment,
} from '@mui/material'
import { alpha, useTheme, type Theme } from '@mui/material/styles'
import EditIcon from '@mui/icons-material/Edit'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/Cancel'
import DeleteIcon from '@mui/icons-material/Delete'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import SecurityIcon from '@mui/icons-material/Security'
import LibraryAddIcon from '@mui/icons-material/LibraryAdd'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import HistoryIcon from '@mui/icons-material/History'
import ChatIcon from '@mui/icons-material/Chat'
import SendIcon from '@mui/icons-material/Send'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import TuneIcon from '@mui/icons-material/Tune'
import CommentIcon from '@mui/icons-material/Comment'
import CheckIcon from '@mui/icons-material/Check'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import AccessTimeIcon from '@mui/icons-material/AccessTime'
import SearchIcon from '@mui/icons-material/Search'
import ClearIcon from '@mui/icons-material/Clear'
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn'
import { useCoreData, useEditor } from '../contexts'
import { liveApi } from '@/api/live'
import type { LiveScript, LiveScriptVersion, LiveScriptComment } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEditProgress } from '../hooks/useEditProgress'
import { useScriptSearch } from '../hooks/useScriptSearch'
import { useAutoQualityCheck } from '../hooks/useAutoQualityCheck'
import { sortLiveScripts } from '../utils/order'

type EditFilter = 'all' | 'generated' | 'edited' | 'unfilled' | 'needsReview'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已激活', color: 'success' },
  2: { label: '已归档', color: 'warning' },
}

const SCRIPT_TAB_READY_ENDPOINTS = [
  '/live/script/save',
  '/live/script/delete',
  '/live/ai/refine-script',
  '/live/ai/refine-segment',
  '/live/ai/chat-for-script',
  '/live/ai/suggest-improvement',
  '/live/ai/check-violation',
  '/live/ai/save-to-copy-if-passed',
  '/live/script/version/getByScriptId',
  '/live/script/version/save',
  '/live/script/version/updateStatus',
  '/live/script/version/delete',
  '/live/script-comment/unresolved-by-script',
  '/live/script-comment/by-script',
  '/live/script-comment/save',
  '/live/script-comment/resolve',
  '/live/script-comment/delete',
] as const

const SCRIPT_TAB_CONTEXT_ENDPOINTS = [
  '/live/session/get',
  '/live/script/by-session',
] as const

const SCRIPT_TAB_UNSUPPORTED_ACTIONS = [
  'full-script-generation',
  'sse-generation',
  'product-mutation',
  'shortvideo-export',
  'approval-submit',
  'local-script-list-fallback',
  'local-ai-refine-fallback',
  'local-comment-fallback',
  'local-version-fallback',
] as const

const INITIAL_SCRIPT_RENDER_LIMIT = 40
const SCRIPT_RENDER_BATCH_SIZE = 40
const AUTO_COLLAPSE_SCRIPT_THRESHOLD = 20

// 全量话术类型 — 精确对齐数据库 live_script.script_type（共 15 种）
// 来源：LiveScriptPostProcessor.defaultRequirement() + migration-script-type-expand.sql
const SCRIPT_TYPE_LABELS: Record<string, { label: string; color: 'default' | 'primary' | 'secondary' | 'info' | 'success' | 'warning' | 'error' }> = {
  opening:      { label: '开场',     color: 'info' },
  product:      { label: '产品介绍', color: 'primary' },
  transition:   { label: '转场',     color: 'secondary' },
  closing:      { label: '收尾',     color: 'warning' },
  chat:         { label: '聊家常',   color: 'default' },
  interaction:  { label: '互动引导', color: 'success' },
  welfare:      { label: '福利话术', color: 'info' },
  closing_deal: { label: '逼单促单', color: 'error' },
  hold_back:    { label: '憋单蓄水', color: 'warning' },
  emotional:    { label: '情绪价值', color: 'secondary' },
  rapid_intro:  { label: '快速过品', color: 'default' },
  deep_sell:    { label: '深度单品', color: 'primary' },
  pain_point:   { label: '痛点放大', color: 'error' },
  testimony:    { label: '用户证言', color: 'success' },
  custom:       { label: '自定义',   color: 'default' },
}

// ─── Version History Dialog ───────────────────────────────────────────────────
interface VersionDialogProps {
  scriptId: number
  sessionId: number
  open: boolean
  onClose: () => void
}
function VersionDialog({ scriptId, sessionId, open, onClose }: VersionDialogProps) {
  const toast = useToast()
  const qc = useQueryClient()
  const { data: versions = [], isLoading } = useQuery({
    queryKey: ['script-versions', scriptId],
    queryFn: () => liveApi.versionList(scriptId),
    enabled: open,
  })
  const activateMut = useMutation({
    mutationFn: (id: number) => liveApi.versionActivate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wb-scripts', sessionId] })
      toast('已切换到该版本', 'success')
      onClose()
    },
    onError: (e: Error) => toast(`/live/script/version/updateStatus 启用版本失败：${e.message}`, 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.versionDelete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['script-versions', scriptId] }),
    onError: (e: Error) => toast(`/live/script/version/delete 删除版本失败：${e.message}`, 'error'),
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>版本历史</DialogTitle>
      <DialogContent
        dividers
        data-testid="live-script-version-dialog"
        data-contract-source="/live/script/version/getByScriptId|/live/script/version/updateStatus|/live/script/version/delete"
        data-script-id={scriptId}
        data-version-count={versions.length}
        data-no-local-version-fallback="true"
        sx={{ p: 0 }}
      >
        {isLoading ? <Box sx={{ p: 3, textAlign: 'center' }}><CircularProgress /></Box> : (
          versions.length === 0 ? (
            <Box
              data-testid="live-script-version-empty"
              data-contract-source="/live/script/version/getByScriptId"
              data-no-local-version-fallback="true"
              sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}
            >
              <Typography variant="body2">暂无历史版本</Typography>
            </Box>
          ) : (
            <List disablePadding>
              {versions.map((v: LiveScriptVersion, i: number) => (
                <Box key={v.id}>
                  {i > 0 && <Divider />}
                  <ListItem alignItems="flex-start" secondaryAction={
                    <Stack direction="row" spacing={1}>
                      <Button size="small" variant="outlined" onClick={() => activateMut.mutate(v.id)}
                        disabled={activateMut.isPending}>启用此版本</Button>
                      <IconButton size="small" color="error" onClick={() => deleteMut.mutate(v.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  }>
                    <ListItemText
                      primary={<Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2" fontWeight={600}>{v.versionNo}</Typography>
                        {i === 0 && <Chip label="最新" size="small" color="primary" />}
                      </Box>}
                      secondary={
                        <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'pre-wrap', display: 'block', mt: 0.5, maxHeight: 80, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {v.content}
                        </Typography>
                      }
                    />
                  </ListItem>
                </Box>
              ))}
            </List>
          )
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>关闭</Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── AI Chat Dialog ───────────────────────────────────────────────────────────
interface AiChatDialogProps {
  script: LiveScript
  open: boolean
  onClose: () => void
}
function AiChatDialog({ script, open, onClose }: AiChatDialogProps) {
  const toast = useToast()
  const { handleScriptSave } = useEditor()
  const qc = useQueryClient()
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<{ role: 'user' | 'ai'; content: string }[]>([
    { role: 'ai', content: `我已读取话术内容，你可以告诉我：
• 哪段话术需要调整或改写
• 增加/减少某种情绪或风格
• 插入促销话术或互动话术
• 修改为更专业/更轻松的语气` },
  ])
  const [loading, setLoading] = useState(false)

  const handleSend = async () => {
    const msg = input.trim()
    if (!msg || loading) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: msg }])
    setLoading(true)
    try {
      const result = await liveApi.aiChatForScript({
        scriptId: script.id,
        message: msg,
      })
      setMessages(prev => [...prev, { role: 'ai', content: result as string }])
    } catch (e: Error | unknown) {
      toast(`/live/ai/chat-for-script AI 对话失败：${(e as Error).message}`, 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleApplyLast = async () => {
    const last = messages.filter(m => m.role === 'ai').pop()
    if (!last || last.content.startsWith('我已读取')) return
    try {
      await handleScriptSave({
        id: script.id,
        scriptContent: last.content,
        sessionId: script.sessionId,
        scriptTitle: String(script.scriptType || '话术')
      })
      qc.invalidateQueries({ queryKey: ['wb-scripts', script.sessionId] })
      toast('已应用 AI 回复到话术', 'success')
      onClose()
    } catch (e: Error | unknown) {
      toast(`/live/script/save 应用 AI 回复失败：${(e as Error).message}`, 'error')
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <AutoAwesomeIcon color="primary" fontSize="small" />
        AI 话术对话优化
        <Chip label={String(script.scriptType || '话术')} size="small" sx={{ ml: 1 }} />
      </DialogTitle>
      <DialogContent
        dividers
        data-testid="live-script-ai-chat-dialog"
        data-contract-source="/live/ai/chat-for-script|/live/script/save"
        data-script-id={script.id}
        data-no-local-ai-refine-fallback="true"
        sx={{ display: 'flex', flexDirection: 'column', gap: 1, height: 420, p: 1.5 }}
      >
        <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', gap: 1 }}>
          {messages.map((m, i) => (
            <Box key={i} sx={{ display: 'flex', justifyContent: m.role === 'user' ? 'flex-end' : 'flex-start' }}>
              <Paper
                variant="outlined"
                data-testid={`live-script-chat-bubble-${m.role}`}
                sx={(theme) => ({
                  p: 1.5,
                  maxWidth: '80%',
                  borderRadius: 2,
                  bgcolor: m.role === 'user'
                    ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
                    : theme.palette.mode === 'dark'
                      ? theme.palette.background.default
                      : alpha(theme.palette.common.black, 0.025),
                  borderColor: m.role === 'user'
                    ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.5 : 0.28)
                    : theme.palette.divider,
                })}
              >
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{m.content}</Typography>
              </Paper>
            </Box>
          ))}
          {loading && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CircularProgress size={16} />
              <Typography variant="caption" color="text.secondary">AI 正在思考...</Typography>
            </Box>
          )}
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <TextField size="small" fullWidth multiline maxRows={3}
            placeholder="描述你的修改需求…"
            value={input} onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() } }}
            disabled={loading}
          />
          <IconButton
            color="primary"
            onClick={handleSend}
            disabled={!input.trim() || loading}
            aria-label="发送 AI 对话"
          >
            <SendIcon />
          </IconButton>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>关闭</Button>
        <Button variant="contained" onClick={handleApplyLast}
          disabled={messages.filter(m => m.role === 'ai').length <= 1}>
          应用最新 AI 回复
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── Comment Panel ────────────────────────────────────────────────────────────
interface CommentPanelProps {
  script: LiveScript
  open: boolean
  onClose: () => void
}
function CommentPanel({ script, open, onClose }: CommentPanelProps) {
  const toast = useToast()
  const qc = useQueryClient()
  const [newComment, setNewComment] = useState('')
  const { data: comments = [], isLoading } = useQuery({
    queryKey: ['script-comments', script.id],
    queryFn: () => liveApi.scriptCommentByScript(script.id),
    enabled: open,
  })
  const saveMut = useMutation({
    mutationFn: () => liveApi.scriptCommentSave({ scriptId: script.id, sessionId: script.sessionId, content: newComment }),
    onSuccess: () => {
      setNewComment('')
      qc.invalidateQueries({ queryKey: ['script-comments', script.id] })
    },
    onError: (e: Error) => toast(`/live/script-comment/save 添加批注失败：${e.message}`, 'error'),
  })
  const resolveMut = useMutation({
    mutationFn: (id: number) => liveApi.scriptCommentResolve(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['script-comments', script.id] }),
    onError: (e: Error) => toast(`/live/script-comment/resolve 解决批注失败：${e.message}`, 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => liveApi.scriptCommentDelete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['script-comments', script.id] }),
    onError: (e: Error) => toast(`/live/script-comment/delete 删除批注失败：${e.message}`, 'error'),
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <CommentIcon fontSize="small" />
        批注 · {String(script.scriptType || '话术')}
      </DialogTitle>
      <DialogContent
        dividers
        data-testid="live-script-comment-dialog"
        data-contract-source="/live/script-comment/by-script|/live/script-comment/save|/live/script-comment/resolve|/live/script-comment/delete"
        data-script-id={script.id}
        data-comment-count={comments.length}
        data-no-local-comment-fallback="true"
        sx={{ p: 0, display: 'flex', flexDirection: 'column' }}
      >
        <Box sx={{ flex: 1, overflow: 'auto', maxHeight: 320 }}>
          {isLoading ? <Box sx={{ p: 2, textAlign: 'center' }}><CircularProgress size={20} /></Box> :
            comments.length === 0 ? (
              <Box
                data-testid="live-script-comment-empty"
                data-contract-source="/live/script-comment/by-script"
                data-no-local-comment-fallback="true"
                sx={{ p: 3, textAlign: 'center', color: 'text.secondary' }}
              >
                <Typography variant="body2">暂无批注</Typography>
              </Box>
            ) : (
              <List disablePadding>
                {comments.map((c: LiveScriptComment, i: number) => (
                  <Box key={c.id}>
                    {i > 0 && <Divider />}
                    <ListItem alignItems="flex-start" secondaryAction={
                      <Stack direction="row" spacing={0.5}>
                        {!c.resolved && (
                          <Tooltip title="标记为已解决">
                            <IconButton size="small" onClick={() => resolveMut.mutate(c.id)}>
                              <CheckIcon fontSize="small" color="success" />
                            </IconButton>
                          </Tooltip>
                        )}
                        <IconButton size="small" onClick={() => deleteMut.mutate(c.id)}>
                          <DeleteIcon fontSize="small" color="error" />
                        </IconButton>
                      </Stack>
                    }>
                      <ListItemText
                        primary={<Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          {c.resolved && <Chip label="已解决" size="small" color="success" />}
                          <Typography variant="body2">{c.content}</Typography>
                        </Box>}
                        secondary={new Date(c.createTime).toLocaleString('zh-CN')}
                      />
                    </ListItem>
                  </Box>
                ))}
              </List>
            )}
        </Box>
        <Divider />
        <Box sx={{ p: 1.5, display: 'flex', gap: 1 }}>
          <TextField size="small" fullWidth placeholder="添加批注…" multiline maxRows={3}
            value={newComment} onChange={e => setNewComment(e.target.value)} />
          <Button variant="contained" size="small" disabled={!newComment.trim() || saveMut.isPending}
            onClick={() => saveMut.mutate()}>
            提交
          </Button>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>关闭</Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── 22 种话术风格（与 GenerateTabContent 保持一致）────────────────────────────
const REFINE_STYLES: { value: string; label: string; group: string }[] = [
  { value: 'natural',       label: '自然',     group: '基础' },
  { value: 'friendly',      label: '亲切',     group: '基础' },
  { value: 'warm',          label: '温暖',     group: '基础' },
  { value: 'gentle',        label: '温柔',     group: '基础' },
  { value: 'casual',        label: '随性',     group: '基础' },
  { value: 'enthusiastic',  label: '热情',     group: '激情' },
  { value: 'passionate',    label: '澎湃',     group: '激情' },
  { value: 'promotion',     label: '促销',     group: '激情' },
  { value: 'seeding',       label: '种草',     group: '激情' },
  { value: 'professional',  label: '专业',     group: '专业' },
  { value: 'emotional',     label: '情感',     group: '情感' },
  { value: 'chicken_soup',  label: '鸡汤',     group: '情感' },
  { value: 'positive',      label: '正能量',   group: '情感' },
  { value: 'heart_piercing',label: '扎心',     group: '情感' },
  { value: 'family',        label: '家庭',     group: '情感' },
  { value: 'love',          label: '爱情',     group: '情感' },
  { value: 'humorous',      label: '幽默',     group: '创意' },
  { value: 'proverb',       label: '歇后语',   group: '创意' },
  { value: 'lyrical',       label: '抒情',     group: '创意' },
  { value: 'creative',      label: '创意',     group: '创意' },
  { value: 'local_flavor',  label: '地方特色', group: '创意' },
  { value: 'persona_flavor',label: '人设风格', group: '创意' },
]

type StyleGroupTone = 'primary' | 'error' | 'success' | 'secondary' | 'warning'

const STYLE_GROUP_TONES: Record<string, StyleGroupTone> = {
  '基础': 'primary',
  '激情': 'error',
  '专业': 'success',
  '情感': 'secondary',
  '创意': 'warning',
}

function getStyleGroupTone(group: string): StyleGroupTone | 'text-secondary' {
  return STYLE_GROUP_TONES[group] ?? 'text-secondary'
}

function getStyleGroupColor(theme: Theme, group: string): string {
  const tone = STYLE_GROUP_TONES[group]
  return tone ? theme.palette[tone][theme.palette.mode === 'dark' ? 'light' : 'main'] : theme.palette.text.secondary
}

// ─── StyleRefinePopover ────────────────────────────────────────────────────────
interface StyleRefinePopoverProps {
  anchorEl: HTMLElement | null
  onClose: () => void
  onRefine: (style: string) => void
  loading: boolean
}
function StyleRefinePopover({ anchorEl, onClose, onRefine, loading }: StyleRefinePopoverProps) {
  const theme = useTheme()
  const [selected, setSelected] = useState<string[]>([])
  const open = Boolean(anchorEl)

  const toggle = (v: string) => {
    setSelected(prev =>
      prev.includes(v)
        ? prev.filter(s => s !== v)
        : prev.length < 3 ? [...prev, v] : prev
    )
  }

  const handleApply = () => {
    if (selected.length === 0) return
    onRefine(selected.join(','))
    setSelected([])
    onClose()
  }

  const groups = Array.from(new Set(REFINE_STYLES.map(s => s.group)))

  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={() => { setSelected([]); onClose() }}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      PaperProps={{ sx: { p: 1.5, width: 340, maxHeight: 420, overflow: 'auto' } }}
    >
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
        选择风格（最多3种混搭）后点击「精修」
      </Typography>
      {groups.map(group => (
        <Box key={group} sx={{ mb: 1 }}>
          <Typography
            variant="caption"
            data-testid="live-script-style-group-title-surface"
            data-style-group={group}
            data-style-tone={getStyleGroupTone(group)}
            sx={{ color: getStyleGroupColor(theme, group), fontWeight: 600, display: 'block', mb: 0.5 }}
          >
            {group}
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {REFINE_STYLES.filter(s => s.group === group).map(s => (
              <Chip
                key={s.value}
                label={s.label}
                size="small"
                onClick={() => toggle(s.value)}
                color={selected.includes(s.value) ? 'primary' : 'default'}
                variant={selected.includes(s.value) ? 'filled' : 'outlined'}
                sx={{ fontSize: 11, cursor: 'pointer' }}
              />
            ))}
          </Box>
        </Box>
      ))}
      <Divider sx={{ my: 1 }} />
      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="caption" color="text.secondary">
          {selected.length === 0 ? '请选择至少1种风格' : `已选：${selected.map(v => REFINE_STYLES.find(s => s.value === v)?.label).join(' + ')}`}
        </Typography>
        <Button
          size="small" variant="contained"
          disabled={selected.length === 0 || loading}
          startIcon={loading ? <CircularProgress size={12} /> : <AutoAwesomeIcon />}
          onClick={handleApply}
          sx={{ fontSize: 11, minWidth: 80 }}
        >
          {loading ? '精修中...' : '风格精修'}
        </Button>
      </Box>
    </Popover>
  )
}

// ─── SegmentRefineDialog ─────────────────────────────────────────────────────
interface SegmentRefineDialogProps {
  script: LiveScript
  open: boolean
  onClose: () => void
  onApplied: (content: string) => void
}

const SEGMENT_QUICK_INSTRUCTIONS = [
  '保留原意，改得更像真人主播口吻',
  '压缩到原来一半，保留关键卖点',
  '加入一个自然互动问句',
  '增强画面感和使用场景',
  '把促单改得更克制，不硬催',
  '避开绝对化、夸大功效表达',
  '增加前后商品承接感',
  '用更具体的利益点替换空话',
] as const

function SegmentRefineDialog({ script, open, onClose, onApplied }: SegmentRefineDialogProps) {
  const toast = useToast()
  const [selectedSegment, setSelectedSegment] = useState<string | null>(null)
  const [instruction, setInstruction] = useState('')
  const [loading, setLoading] = useState(false)

  const paragraphs = script.scriptContent
    .split(/\n\n+/)
    .map(p => p.trim())
    .filter(p => p.length > 0)

  const handleQuick = (q: string) => {
    setInstruction(prev => prev ? prev + '，' + q : q)
  }

  const handleApply = async () => {
    if (!selectedSegment) { toast('请先点击选择一个段落', 'warning'); return }
    if (!instruction.trim()) { toast('请输入微调指令', 'warning'); return }
    setLoading(true)
    try {
      const result = await liveApi.aiRefineSegment({
        scriptId: script.id,
        segmentText: selectedSegment,
        instruction: `${instruction.trim()}。要求：只改选中片段，保留原话术其他部分；输出完整修改后的话术正文，口语自然，不要解释。`,
      })
      onApplied(result as string)
      toast('段内微调完成，已进入编辑确认模式', 'success')
      onClose()
    } catch (e: unknown) {
      toast(`/live/ai/refine-segment 段内微调失败：${(e as Error).message}`, 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    if (loading) return
    setSelectedSegment(null)
    setInstruction('')
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ pb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <AutoFixHighIcon color="primary" />
          <Box>
            <Typography variant="subtitle1" fontWeight={600}>段内微调</Typography>
            <Typography variant="caption" color="text.secondary">
              点击选择段落 → 输入指令 → 应用微调
            </Typography>
          </Box>
        </Box>
      </DialogTitle>
      <DialogContent
        data-testid="live-script-segment-refine-dialog"
        data-contract-source="/live/ai/refine-segment"
        data-script-id={script.id}
        data-no-local-ai-refine-fallback="true"
        sx={{ pt: 0 }}
      >
        {/* 段落选择区 */}
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.75, fontWeight: 600 }}>
          第一步：点击选择要微调的段落
        </Typography>
        <Paper
          variant="outlined"
          data-testid="live-script-segment-refine-list-surface"
          sx={(theme) => ({
            p: 1.5,
            mb: 2,
            maxHeight: 280,
            overflowY: 'auto',
            bgcolor: theme.palette.mode === 'dark'
              ? theme.palette.background.default
              : alpha(theme.palette.common.black, 0.025),
          })}
        >
          <Stack spacing={0.75}>
            {paragraphs.map((p, i) => (
              <Box
                key={i}
                onClick={() => setSelectedSegment(prev => prev === p ? null : p)}
                data-testid={selectedSegment === p ? 'live-script-selected-segment-surface' : undefined}
                sx={(theme) => ({
                  p: 1, borderRadius: 1, cursor: 'pointer', fontSize: 13, lineHeight: 1.7,
                  border: '1px solid',
                  borderColor: selectedSegment === p ? theme.palette.primary.main : 'transparent',
                  bgcolor: selectedSegment === p
                    ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
                    : theme.palette.background.paper,
                  '&:hover': {
                    bgcolor: selectedSegment === p
                      ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.12)
                      : theme.palette.action.hover,
                  },
                  transition: 'all 0.15s',
                  whiteSpace: 'pre-wrap',
                  position: 'relative',
                })}
              >
                {selectedSegment === p && (
                  <CheckIcon sx={{ position: 'absolute', top: 4, right: 4, fontSize: 14, color: 'primary.main' }} />
                )}
                <Typography variant="body2" component="span" sx={{ fontSize: 13, lineHeight: 1.7 }}>
                  {p}
                </Typography>
              </Box>
            ))}
          </Stack>
        </Paper>

        {/* 指令区 */}
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.75, fontWeight: 600 }}>
          第二步：输入微调指令
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
          {SEGMENT_QUICK_INSTRUCTIONS.map(q => (
            <Chip
              key={q} label={q} size="small"
              variant="outlined"
              sx={{ fontSize: 11, cursor: 'pointer' }}
              onClick={() => handleQuick(q)}
            />
          ))}
        </Box>
        <TextField
          fullWidth multiline minRows={2}
          size="small"
          placeholder="例如：保留核心卖点，改成更像直播间真人表达，少用模板腔，增加一句自然互动"
          value={instruction}
          onChange={e => setInstruction(e.target.value)}
          disabled={loading}
          InputProps={{ sx: { fontSize: 13 } }}
        />

        {selectedSegment && (
          <Alert severity="info" sx={{ mt: 1.5, py: 0.5 }}>
            <Typography variant="caption">已选段落：{selectedSegment.slice(0, 60)}{selectedSegment.length > 60 ? '…' : ''}</Typography>
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 2.5, pb: 2 }}>
        <Button onClick={handleClose} disabled={loading}>取消</Button>
        <Button
          variant="contained"
          disabled={!selectedSegment || !instruction.trim() || loading}
          startIcon={loading ? <CircularProgress size={14} /> : <AutoFixHighIcon />}
          onClick={handleApply}
        >
          {loading ? '微调中...' : '应用微调'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ─── SlotStylePopover（槽位独立风格设置）────────────────────────────────────
const SLOT_STYLE_OPTIONS = [
  { value: 'natural',      label: '自然',   group: '基础' },
  { value: 'friendly',    label: '亲切',   group: '基础' },
  { value: 'warm',        label: '温暖',   group: '基础' },
  { value: 'enthusiastic',label: '热情',   group: '激情' },
  { value: 'passionate',  label: '澎湃',   group: '激情' },
  { value: 'promotion',   label: '促销',   group: '激情' },
  { value: 'seeding',     label: '种草',   group: '激情' },
  { value: 'professional',label: '专业',   group: '专业' },
  { value: 'emotional',   label: '情感',   group: '情感' },
  { value: 'humorous',    label: '幽默',   group: '创意' },
]
interface SlotStylePopoverProps {
  anchorEl: HTMLElement | null
  onClose: () => void
  currentValue?: string
  onSave: (style: string | null) => void
  loading: boolean
}
function SlotStylePopover({ anchorEl, onClose, currentValue, onSave, loading }: SlotStylePopoverProps) {
  const open = Boolean(anchorEl)
  const groups = Array.from(new Set(SLOT_STYLE_OPTIONS.map(s => s.group)))
  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      PaperProps={{
        'data-testid': 'live-script-slot-style-popover',
        'data-contract-source': '/live/script/save',
        sx: { p: 1.5, width: 280, maxHeight: 380, overflow: 'auto' },
      }}
    >
      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
        设置此段话术独立风格（覆盖全局风格）
      </Typography>
      {groups.map(group => (
        <Box key={group} sx={{ mb: 1 }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mb: 0.5 }}>{group}</Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {SLOT_STYLE_OPTIONS.filter(s => s.group === group).map(s => (
              <Chip
                key={s.value}
                label={s.label}
                size="small"
                variant={currentValue === s.value ? 'filled' : 'outlined'}
                color={currentValue === s.value ? 'primary' : 'default'}
                onClick={() => { onSave(s.value); onClose() }}
                disabled={loading}
                sx={{ fontSize: 11, cursor: 'pointer' }}
              />
            ))}
          </Box>
        </Box>
      ))}
      {currentValue && (
        <>
          <Divider sx={{ my: 1 }} />
          <Button size="small" fullWidth variant="outlined" color="inherit"
            onClick={() => { onSave(null); onClose() }} disabled={loading}>
            清除独立风格（使用全局）
          </Button>
        </>
      )}
    </Popover>
  )
}

// ─── SlotRequirementPopover（槽位意图/需求设置）────────────────────────────────
const REQUIREMENT_PRESETS: Record<string, string[]> = {
  opening: ['热情欢迎，引导关注', '预告福利，制造期待', '品牌介绍，建立信任', '引导粉丝团，拉停留'],
  product: ['产品介绍，突出卖点', '成分功效，专业讲解', '使用体验，场景种草', '限时促单，制造紧迫', '对比竞品，强调优势'],
  transition: ['自然过渡，预告下款', '互动引导，拉停留', '感谢下单，催促未拍'],
  closing: ['感谢陪伴，预告下播', '强调售后，打消顾虑', '引导关注，下次不迷路'],
}
const DEFAULT_REQUIREMENT_PRESETS = ['互动引导，提升活跃', '强调优惠，制造紧迫', '温情话术，情感共鸣']

interface SlotRequirementPopoverProps {
  anchorEl: HTMLElement | null
  onClose: () => void
  currentValue?: string
  scriptType?: string
  onSave: (req: string | null) => void
  loading: boolean
}
function SlotRequirementPopover({ anchorEl, onClose, currentValue, scriptType, onSave, loading }: SlotRequirementPopoverProps) {
  const [input, setInput] = useState(currentValue ?? '')
  const open = Boolean(anchorEl)
  const presets: string[] = (scriptType ? REQUIREMENT_PRESETS[scriptType] : undefined) ?? DEFAULT_REQUIREMENT_PRESETS

  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      PaperProps={{
        'data-testid': 'live-script-slot-requirement-popover',
        'data-contract-source': '/live/script/save',
        sx: { p: 1.5, width: 300 },
      }}
    >
      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
        设置此段意图（影响 AI 生成方向）
      </Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1.5 }}>
        {presets.map(p => (
          <Chip key={p} label={p} size="small" variant="outlined"
            onClick={() => setInput(p)}
            color={input === p ? 'primary' : 'default'}
            sx={{ fontSize: 11, cursor: 'pointer' }}
          />
        ))}
      </Box>
      <TextField
        fullWidth size="small" multiline minRows={2}
        placeholder="自定义意图描述，例如：重点强调成分功效，引导立即下单"
        value={input}
        onChange={e => setInput(e.target.value)}
        disabled={loading}
        sx={{ mb: 1 }}
      />
      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'space-between' }}>
        {currentValue ? (
          <Button size="small" color="inherit" variant="outlined"
            onClick={() => { onSave(null); onClose() }} disabled={loading}>清除</Button>
        ) : <Box />}
        <Button size="small" variant="contained"
          disabled={!input.trim() || loading}
          onClick={() => { onSave(input.trim()); onClose() }}>
          确认
        </Button>
      </Box>
    </Popover>
  )
}

// ─── DurationPopover ────────────────────────────────────────────────────────
const DURATION_PRESETS = [
  { label: '30秒', value: 30 },
  { label: '1分钟', value: 60 },
  { label: '1.5分钟', value: 90 },
  { label: '2分钟', value: 120 },
  { label: '3分钟', value: 180 },
  { label: '5分钟', value: 300 },
]

interface DurationPopoverProps {
  anchorEl: HTMLElement | null
  onClose: () => void
  currentValue: number
  onSave: (sec: number) => void
  loading: boolean
}
function DurationPopover({ anchorEl, onClose, currentValue, onSave, loading }: DurationPopoverProps) {
  const [input, setInput] = useState<string>(currentValue > 0 ? String(Math.round(currentValue / 60 * 10) / 10) : '')
  const open = Boolean(anchorEl)

  const handlePreset = (sec: number) => {
    onSave(sec)
    onClose()
  }

  const handleCustom = () => {
    const min = parseFloat(input)
    if (!isNaN(min) && min > 0) {
      onSave(Math.round(min * 60))
      onClose()
    }
  }

  const handleClear = () => {
    onSave(0)
    onClose()
  }

  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      PaperProps={{
        'data-testid': 'live-script-duration-popover',
        'data-contract-source': '/live/script/save',
        sx: { p: 1.5, width: 240 },
      }}
    >
      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ mb: 1, display: 'block' }}>
        设置此段话术时长上限
      </Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1.5 }}>
        {DURATION_PRESETS.map(p => (
          <Chip
            key={p.value}
            label={p.label}
            size="small"
            variant={currentValue === p.value ? 'filled' : 'outlined'}
            color={currentValue === p.value ? 'primary' : 'default'}
            onClick={() => handlePreset(p.value)}
            disabled={loading}
            sx={{ fontSize: 11 }}
          />
        ))}
      </Box>
      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
        <TextField
          size="small"
          label="自定义(分钟)"
          type="number"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleCustom()}
          inputProps={{ min: 0.5, max: 30, step: 0.5 }}
          sx={{ flex: 1 }}
          disabled={loading}
        />
        <Button size="small" variant="contained" onClick={handleCustom}
          disabled={!input || isNaN(parseFloat(input)) || loading}>
          确定
        </Button>
      </Box>
      {currentValue > 0 && (
        <Button size="small" color="inherit" onClick={handleClear} sx={{ mt: 0.75, fontSize: 11 }} disabled={loading}>
          清除时长限制
        </Button>
      )}
    </Popover>
  )
}

interface SuggestImprovementResult { improved?: string | null; suggestion?: string; advice?: string; content?: string; reason?: string; skipped?: boolean }
interface ImprovementSuggestionState {
  improved: string | null
  note: string
  skipped: boolean
}
interface CheckViolationResult { passed?: boolean; issues?: string[] }

function normalizeAiText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeImprovementSuggestion(result: unknown): ImprovementSuggestionState {
  if (typeof result === 'string') {
    const note = result.trim()
    return { improved: null, note, skipped: true }
  }
  const r = (result ?? {}) as SuggestImprovementResult
  const improved = normalizeAiText(r.improved)
  const note = normalizeAiText(r.suggestion) || normalizeAiText(r.advice) || normalizeAiText(r.reason) || normalizeAiText(r.content)
  return {
    improved: improved || null,
    note,
    skipped: Boolean(r.skipped) || !improved,
  }
}

function buildDurationRefineInstruction(script: LiveScript): string {
  const seconds = Number(script.durationLimitSec ?? 0)
  if (!Number.isFinite(seconds) || seconds <= 0) return ''
  const minChars = Math.max(8, Math.round(seconds * 3))
  const maxChars = Math.max(minChars, Math.round(seconds * 4))
  return `必须控制在 ${seconds} 秒以内，按每秒约3-4个中文字估算，正文建议 ${minChars}-${maxChars} 个中文字；信息过多时优先保留关键卖点、互动钩子和合规表达。`
}

// ─── ScriptCard ───────────────────────────────────────────────────────────────
interface ScriptCardProps {
  script: LiveScript
  isEdited: boolean
  onEdited: (id: number) => void
  unresolvedCount: number
  defaultExpanded: boolean
}
const ScriptCard = memo(function ScriptCard({ script, isEdited, onEdited, unresolvedCount, defaultExpanded }: ScriptCardProps) {
  const toast = useToast()
  const qc = useQueryClient()
  const { editingId, editContent, setEditContent, handleStartEdit, handleCancelEdit, handleSaveEdit, handleScriptDelete, isSaving } = useEditor()
  const [expanded, setExpanded] = useState(defaultExpanded)
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
  const [violationResult, setViolationResult] = useState<{ passed: boolean; issues?: string[] } | null>(null)
  const [showVersions, setShowVersions] = useState(false)
  const [showChat, setShowChat] = useState(false)
  const [showComments, setShowComments] = useState(false)
  const [suggestion, setSuggestion] = useState<ImprovementSuggestionState | null>(null)
  const [styleAnchor, setStyleAnchor] = useState<HTMLElement | null>(null)
  const [showSegmentRefine, setShowSegmentRefine] = useState(false)
  const [durationAnchor, setDurationAnchor] = useState<HTMLElement | null>(null)
  const [slotStyleAnchor, setSlotStyleAnchor] = useState<HTMLElement | null>(null)
  const [slotReqAnchor, setSlotReqAnchor] = useState<HTMLElement | null>(null)
  const isEditing = editingId === script.id
  const status = STATUS_MAP[Number(script.status ?? 0)] ?? STATUS_MAP[0]
  const typeInfo = script.scriptType ? SCRIPT_TYPE_LABELS[script.scriptType] : null

  const refineMut = useMutation({
    mutationFn: () => liveApi.aiRefineScript({
      scriptId: script.id,
      question: `${buildDurationRefineInstruction(script)}请在保留事实、卖点和直播节奏的前提下精修这段话术：减少模板腔，增加真人主播的停顿、承接、互动问句和具体利益点；避免绝对化和夸大功效表达；只输出修改后的正文。`,
    }),
    onSuccess: (refined) => {
      handleStartEdit(script)
      setEditContent(refined as string)
      toast('AI 已优化话术，请确认后保存', 'success')
    },
    onError: (e: Error) => toast(`/live/ai/refine-script AI 精修失败：${e.message}`, 'error'),
  })

  const styleRefineMut = useMutation({
    mutationFn: (style: string) => {
      const labels = style.split(',').map(v => REFINE_STYLES.find(s => s.value === v)?.label ?? v).join('、')
      return liveApi.aiRefineScript({
        scriptId: script.id,
        question: `${buildDurationRefineInstruction(script)}请把这段话术改成「${labels}」风格，同时保留原有卖点和直播转化意图。要求表达自然、有现场感，避免套话、硬广和绝对化承诺，只输出修改后的正文。`,
      })
    },
    onSuccess: (refined, style) => {
      handleStartEdit(script)
      setEditContent(refined as string)
      const labels = style.split(',').map(v => REFINE_STYLES.find(s => s.value === v)?.label ?? v).join(' + ')
      toast(`已按「${labels}」风格精修，请确认后保存`, 'success')
    },
    onError: (e: Error) => toast(`/live/ai/refine-script 风格精修失败：${e.message}`, 'error'),
  })

  const suggestMut = useMutation({
    mutationFn: () => liveApi.aiSuggestImprovement({ scriptId: script.id }),
    onSuccess: (result) => {
      const next = normalizeImprovementSuggestion(result)
      setSuggestion(next)
      toast(
        next.improved
          ? '已生成可套用的改写稿'
          : next.note
            ? 'AI 仅返回建议，未生成可套用改写稿'
            : '当前话术暂无可用改进版',
        next.improved ? 'success' : 'info',
      )
    },
    onError: (e: Error) => toast(`/live/ai/suggest-improvement 生成改进建议失败：${e.message}`, 'error'),
  })

  const checkMut = useMutation({
    mutationFn: () => liveApi.aiCheckViolation({ scriptId: script.id, content: script.scriptContent }),
    onSuccess: (result) => {
      const r = result as CheckViolationResult
      const passed = r.passed ?? false
      const issues = r.issues
      setViolationResult({ passed, issues })
      toast(passed ? '✅ 话术合规' : '⚠️ 发现违规词', passed ? 'success' : 'warning')
    },
    onError: (e: Error) => toast(`/live/ai/check-violation 违规检测失败：${e.message}`, 'error'),
  })

  const saveToCopyMut = useMutation({
    mutationFn: () => liveApi.aiSaveToCopyIfPassed({ scriptId: script.id, content: script.scriptContent }),
    onSuccess: () => toast('已保存到文案库', 'success'),
    onError: (e: Error) => toast(`/live/ai/save-to-copy-if-passed 存入文案库失败：${e.message}`, 'error'),
  })

  const activateMut = useMutation({
    mutationFn: () => liveApi.scriptSave({
      id: script.id, sessionId: script.sessionId,
      scriptTitle: String(script.scriptType || '话术'), scriptContent: script.scriptContent,
      status: script.status === 1 ? 0 : 1,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wb-scripts', script.sessionId] })
      toast(script.status === 1 ? '已取消激活' : '已激活', 'success')
    },
    onError: (e: Error) => toast(`/live/script/save 激活状态保存失败：${e.message}`, 'error'),
  })

  const saveVersionMut = useMutation({
    mutationFn: () => liveApi.versionSave({ scriptId: script.id, content: script.scriptContent }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['script-versions', script.id] })
      toast('已保存为新版本', 'success')
    },
    onError: (e: Error) => toast(`/live/script/version/save 保存版本失败：${e.message}`, 'error'),
  })

  const durationMut = useMutation({
    mutationFn: (sec: number) => liveApi.scriptSave({
      id: script.id, sessionId: script.sessionId,
      scriptTitle: String(script.scriptType || '话术'), scriptContent: script.scriptContent,
      durationLimitSec: sec,
    }),
    onSuccess: (_r, sec) => {
      qc.invalidateQueries({ queryKey: ['wb-scripts', script.sessionId] })
      toast(sec > 0 ? `时长上限已设为 ${sec >= 60 ? Math.round(sec / 60 * 10) / 10 + ' 分钟' : sec + ' 秒'}` : '已清除时长限制', 'success')
    },
    onError: (e: Error) => toast(`/live/script/save 时长保存失败：${e.message}`, 'error'),
  })

  const slotStyleMut = useMutation({
    mutationFn: (style: string | null) => liveApi.scriptSave({
      id: script.id, sessionId: script.sessionId,
      scriptTitle: String(script.scriptType || '话术'), scriptContent: script.scriptContent,
      style: style ?? undefined,
    }),
    onSuccess: (_r, style) => {
      qc.invalidateQueries({ queryKey: ['wb-scripts', script.sessionId] })
      toast(style ? `此段风格已设为「${SLOT_STYLE_OPTIONS.find(s => s.value === style)?.label ?? style}」` : '已清除独立风格', 'success')
    },
    onError: (e: Error) => toast(`/live/script/save 风格保存失败：${e.message}`, 'error'),
  })

  const slotReqMut = useMutation({
    mutationFn: (req: string | null) => liveApi.scriptSave({
      id: script.id, sessionId: script.sessionId,
      scriptTitle: String(script.scriptType || '话术'), scriptContent: script.scriptContent,
      requirement: req ?? undefined,
    }),
    onSuccess: (_r, req) => {
      qc.invalidateQueries({ queryKey: ['wb-scripts', script.sessionId] })
      toast(req ? '意图已保存' : '已清除意图设置', 'success')
    },
    onError: (e: Error) => toast(`/live/script/save 意图保存失败：${e.message}`, 'error'),
  })

  const handleDelete = async () => {
    setMenuAnchor(null)
    try {
      await handleScriptDelete(script.id)
      toast('已删除话术', 'success')
    } catch (e: unknown) {
      toast(`/live/script/delete 删除话术失败：${(e as Error).message}`, 'error')
    }
  }

  const handleSave = async () => {
    try {
      onEdited(script.id)
      await handleSaveEdit()
      toast('保存成功', 'success')
    } catch (e: unknown) {
      toast(`/live/script/save 保存话术失败：${(e as Error).message}`, 'error')
    }
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(script.scriptContent)
    toast('已复制到剪贴板', 'success')
  }

  const anyLoading = refineMut.isPending || styleRefineMut.isPending || checkMut.isPending ||
    saveToCopyMut.isPending || activateMut.isPending || suggestMut.isPending || saveVersionMut.isPending ||
    durationMut.isPending || slotStyleMut.isPending || slotReqMut.isPending

  useEffect(() => {
    if (isEditing) setExpanded(true)
  }, [isEditing])

  return (
    <Box
      data-testid="live-script-card"
      data-contract-source="/live/script/save|/live/script/delete|/live/ai/refine-script|/live/ai/refine-segment|/live/ai/chat-for-script|/live/ai/suggest-improvement|/live/ai/check-violation|/live/ai/save-to-copy-if-passed|/live/script/version/save|/live/script-comment/unresolved-by-script"
      data-script-id={script.id}
      data-script-status={script.status ?? ''}
      data-script-type={script.scriptType ?? ''}
      data-unresolved-comment-count={unresolvedCount}
      data-script-expanded={expanded ? 'true' : 'false'}
      sx={{
      border: '1px solid',
      borderColor: isEditing ? 'primary.main' : 'divider',
      borderRadius: 1, overflow: 'hidden', transition: 'border-color 0.2s',
    }}>
      {/* Header */}
      <Box
        data-testid="live-script-card-header-surface"
        data-contract-source="/live/script/save|/live/script/delete|/live/script-comment/unresolved-by-script"
        sx={(theme) => ({
          display: 'flex', alignItems: 'center', gap: 0.75,
          px: 1.5, py: 0.75,
          bgcolor: isEditing
            ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
            : theme.palette.mode === 'dark'
              ? theme.palette.background.default
              : alpha(theme.palette.common.black, 0.025),
          borderBottom: '1px solid', borderColor: 'divider',
        })}
      >
        {/* 激活按钮 */}
        <Tooltip title={script.status === 1 ? '点击取消激活' : '点击激活话术'}>
          <IconButton size="small" onClick={() => activateMut.mutate()} disabled={anyLoading} data-testid="live-script-activate-button" data-contract-source="/live/script/save">
            {activateMut.isPending ? <CircularProgress size={14} /> :
              script.status === 1
                ? <CheckCircleIcon fontSize="small" color="success" />
                : <RadioButtonUncheckedIcon fontSize="small" color="action" />}
          </IconButton>
        </Tooltip>

        {/* 类型 + 标题 */}
        {typeInfo && <Chip label={typeInfo.label} size="small" color={typeInfo.color} sx={{ fontSize: 10, height: 18 }} />}
        {isEdited && <Chip label="已编辑" size="small" color="info" variant="outlined" sx={{ fontSize: 10, height: 18 }} />}
        <Typography variant="body2" fontWeight={600} sx={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {String(script.scriptType || '话术')}
        </Typography>
        <Chip label={status.label} size="small" color={status.color} variant="outlined" sx={{ fontSize: 10, height: 18 }} />
        {typeof script.duration === 'number' && script.duration > 0 && (
          <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
            {Math.floor(script.duration / 60)}′{script.duration % 60}″
          </Typography>
        )}
        {/* 时长上限 badge */}
        <Tooltip title={(script.durationLimitSec ?? 0) > 0 ? `时长上限 ${(script.durationLimitSec ?? 0) >= 60 ? Math.round((script.durationLimitSec ?? 0) / 60 * 10) / 10 + '分钟' : (script.durationLimitSec ?? 0) + '秒'}，点击修改` : '点击设置时长上限'}>
          <Chip
            label={(script.durationLimitSec ?? 0) > 0
              ? `≤${(script.durationLimitSec ?? 0) >= 60 ? Math.round((script.durationLimitSec ?? 0) / 60 * 10) / 10 + 'min' : (script.durationLimitSec ?? 0) + 's'}`
              : '设时长'}
            size="small"
            icon={<AccessTimeIcon sx={{ fontSize: '11px !important' }} />}
            variant={(script.durationLimitSec ?? 0) > 0 ? 'filled' : 'outlined'}
            color={(script.durationLimitSec ?? 0) > 0 ? 'warning' : 'default'}
            onClick={e => setDurationAnchor(e.currentTarget)}
            disabled={durationMut.isPending}
            sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
          />
        </Tooltip>

        {/* 槽位独立风格 */}
        <Tooltip title={script.style ? `独立风格：${SLOT_STYLE_OPTIONS.find(s => s.value === script.style)?.label ?? script.style}，点击修改` : '点击设置此段独立风格'}>
          <Chip
            label={script.style ? (SLOT_STYLE_OPTIONS.find(s => s.value === script.style)?.label ?? script.style) : '设风格'}
            size="small"
            variant={script.style ? 'filled' : 'outlined'}
            color={script.style ? 'secondary' : 'default'}
            onClick={e => setSlotStyleAnchor(e.currentTarget)}
            disabled={slotStyleMut.isPending}
            sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
          />
        </Tooltip>

        {/* 槽位意图 */}
        <Tooltip title={script.requirement ? `意图：${script.requirement}，点击修改` : '点击设置此段生成意图'}>
          <Chip
            label={script.requirement ? (script.requirement.length > 8 ? script.requirement.slice(0, 8) + '…' : script.requirement) : '设意图'}
            size="small"
            variant={script.requirement ? 'filled' : 'outlined'}
            color={script.requirement ? 'info' : 'default'}
            onClick={e => setSlotReqAnchor(e.currentTarget)}
            disabled={slotReqMut.isPending}
            sx={{ fontSize: 10, height: 18, cursor: 'pointer' }}
          />
        </Tooltip>

        {/* 操作按钮组 */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
          <Tooltip title="复制话术">
            <IconButton size="small" onClick={handleCopy} data-testid="live-script-copy-button" data-contract-action="clipboard-copy"><ContentCopyIcon sx={{ fontSize: 14 }} /></IconButton>
          </Tooltip>
          <Tooltip title="版本历史">
            <IconButton size="small" onClick={() => setShowVersions(true)} data-testid="live-script-version-button" data-contract-source="/live/script/version/getByScriptId"><HistoryIcon sx={{ fontSize: 14 }} /></IconButton>
          </Tooltip>
          <Tooltip title="AI 对话优化">
            <IconButton
              size="small"
              onClick={() => setShowChat(true)}
              color="primary"
              aria-label="AI 对话优化"
              data-testid="live-script-ai-chat-button"
              data-contract-source="/live/ai/chat-for-script"
            >
              <ChatIcon sx={{ fontSize: 14 }} />
            </IconButton>
          </Tooltip>
          <Tooltip title="批注">
            <IconButton size="small" onClick={() => setShowComments(true)} data-testid="live-script-comment-button" data-contract-source="/live/script-comment/by-script">
              <Badge badgeContent={unresolvedCount || undefined} color="error" sx={{ '& .MuiBadge-badge': { fontSize: 9, minWidth: 14, height: 14 } }}>
                <CommentIcon sx={{ fontSize: 14 }} />
              </Badge>
            </IconButton>
          </Tooltip>
          {!isEditing && (
            <Tooltip title="编辑">
              <IconButton
                size="small"
                onClick={() => {
                  setExpanded(true)
                  handleStartEdit(script)
                }}
                disabled={anyLoading}
                data-testid="live-script-edit-button"
                data-contract-source="/live/script/save"
              >
                <EditIcon sx={{ fontSize: 14 }} />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title={expanded ? '折叠' : '展开'}>
            <IconButton
              size="small"
              onClick={() => setExpanded(v => !v)}
              aria-expanded={expanded}
              data-testid="live-script-expand-button"
            >
              {expanded ? <ExpandLessIcon sx={{ fontSize: 14 }} /> : <ExpandMoreIcon sx={{ fontSize: 14 }} />}
            </IconButton>
          </Tooltip>
          <IconButton size="small" onClick={e => setMenuAnchor(e.currentTarget)} data-testid="live-script-more-button">
            <MoreVertIcon sx={{ fontSize: 14 }} />
          </IconButton>
        </Box>

        {/* 更多菜单 */}
        <Menu
          anchorEl={menuAnchor}
          open={Boolean(menuAnchor)}
          onClose={() => setMenuAnchor(null)}
          PaperProps={{
            'data-testid': 'live-script-more-menu',
            'data-contract-source': '/live/ai/refine-script|/live/ai/suggest-improvement|/live/ai/check-violation|/live/ai/save-to-copy-if-passed|/live/script/version/save|/live/script/delete',
          }}
        >
          <MenuItem onClick={() => { setMenuAnchor(null); setExpanded(true); refineMut.mutate() }} disabled={anyLoading} data-testid="live-script-menu-refine" data-contract-source="/live/ai/refine-script">
            <AutoAwesomeIcon fontSize="small" sx={{ mr: 1 }} />AI 一键精修
          </MenuItem>
          <MenuItem onClick={() => { setMenuAnchor(null); setExpanded(true); suggestMut.mutate() }} disabled={anyLoading} data-testid="live-script-menu-suggest" data-contract-source="/live/ai/suggest-improvement">
            <AutoAwesomeIcon fontSize="small" sx={{ mr: 1 }} />AI 改进建议
          </MenuItem>
          <MenuItem onClick={() => { setMenuAnchor(null); setExpanded(true); checkMut.mutate() }} disabled={anyLoading} data-testid="live-script-menu-check" data-contract-source="/live/ai/check-violation">
            <SecurityIcon fontSize="small" sx={{ mr: 1 }} />违规词检测
          </MenuItem>
          <MenuItem onClick={() => { setMenuAnchor(null); saveToCopyMut.mutate() }} disabled={anyLoading} data-testid="live-script-menu-save-copy" data-contract-source="/live/ai/save-to-copy-if-passed">
            <LibraryAddIcon fontSize="small" sx={{ mr: 1 }} />存入文案库
          </MenuItem>
          <MenuItem onClick={() => { setMenuAnchor(null); saveVersionMut.mutate() }} disabled={anyLoading} data-testid="live-script-menu-save-version" data-contract-source="/live/script/version/save">
            <HistoryIcon fontSize="small" sx={{ mr: 1 }} />保存为新版本
          </MenuItem>
          <Divider />
          <MenuItem onClick={handleDelete} sx={{ color: 'error.main' }} disabled={anyLoading} data-testid="live-script-menu-delete" data-contract-source="/live/script/delete">
            <DeleteIcon fontSize="small" sx={{ mr: 1 }} />删除话术
          </MenuItem>
        </Menu>
      </Box>

      {/* Body */}
      <Collapse in={expanded} mountOnEnter unmountOnExit>
        <Box sx={{ p: 1.5, display: 'flex', flexDirection: 'column', gap: 1 }}>

          {/* Loading bar */}
          {anyLoading && <LinearProgress sx={{ borderRadius: 1 }} />}

          {/* AI 改进建议 */}
          {suggestion && (
            <Alert severity="info" onClose={() => setSuggestion(null)}
              data-testid="live-script-suggestion-alert"
              data-contract-source="/live/ai/suggest-improvement"
              data-has-improved={suggestion.improved ? 'true' : 'false'}
              data-no-suggestion-as-script={suggestion.improved ? 'false' : 'true'}
              action={suggestion.improved ? (
                <Button
                  size="small"
                  onClick={() => {
                    handleStartEdit(script)
                    setEditContent(suggestion.improved ?? '')
                    setSuggestion(null)
                  }}
                  data-testid="live-script-apply-improved-button"
                  data-contract-source="/live/ai/suggest-improvement|/live/script/save"
                >
                  套用改写稿
                </Button>
              ) : undefined}
            >
              <Typography variant="caption" fontWeight={600} display="block" mb={0.5}>
                {suggestion.improved ? 'AI 改写稿' : 'AI 改进建议'}
              </Typography>
              {suggestion.note && (
                <Typography variant="caption" display="block" sx={{ whiteSpace: 'pre-wrap', mb: suggestion.improved ? 0.75 : 0 }}>
                  {suggestion.note}
                </Typography>
              )}
              {suggestion.improved ? (
                <Box
                  data-testid="live-script-improved-content"
                  data-contract-source="/live/ai/suggest-improvement"
                  sx={{ whiteSpace: 'pre-wrap', fontSize: 12.5, lineHeight: 1.7 }}
                >
                  {suggestion.improved}
                </Box>
              ) : (
                <Typography variant="caption" color="text.secondary">
                  该结果只是建议，不会被当作话术保存；请重新生成改写稿或使用 AI 精修。
                </Typography>
              )}
            </Alert>
          )}

          {/* 违规检测结果 */}
          {violationResult && (
            <Alert severity={violationResult.passed ? 'success' : 'warning'}
              data-testid="live-script-violation-alert"
              data-contract-source="/live/ai/check-violation"
              onClose={() => setViolationResult(null)}
              icon={violationResult.passed ? <CheckCircleIcon /> : <WarningAmberIcon />}>
              {violationResult.passed ? '话术合规，无违规词' : (
                <Box>
                  <Typography variant="caption" fontWeight={600}>发现以下问题：</Typography>
                  {violationResult.issues?.map((issue, i) => (
                    <Typography key={i} variant="caption" display="block">• {issue}</Typography>
                  ))}
                </Box>
              )}
            </Alert>
          )}

          {/* 编辑模式 */}
          {isEditing ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }} data-testid="live-script-edit-surface" data-contract-source="/live/script/save">
              <TextField
                multiline fullWidth size="small"
                value={editContent}
                onChange={e => setEditContent(e.target.value)}
                minRows={4}
                autoFocus
                helperText={`${editContent.length} 字 · 约 ${Math.round(editContent.length / 3)} 秒`}
                sx={{ '& .MuiInputBase-root': { fontFamily: 'monospace', fontSize: 13 } }}
              />
              <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                <Button size="small" startIcon={<CancelIcon />} onClick={handleCancelEdit} disabled={isSaving}>取消</Button>
                <Button size="small" variant="contained" startIcon={isSaving ? <CircularProgress size={14} /> : <SaveIcon />}
                  onClick={handleSave} disabled={isSaving}>
                  {isSaving ? '保存中...' : '保存'}
                </Button>
              </Box>
            </Box>
          ) : (
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.8, color: 'text.primary', fontSize: 13 }}>
              {script.scriptContent}
            </Typography>
          )}

          {/* 快捷 AI 操作条 */}
          {!isEditing && (
            <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', borderTop: '1px solid', borderColor: 'divider', pt: 1 }} data-testid="live-script-quick-actions" data-contract-source="/live/ai/refine-script|/live/ai/refine-segment|/live/ai/suggest-improvement|/live/ai/check-violation|/live/ai/save-to-copy-if-passed">
              <Button size="small" variant="outlined" startIcon={refineMut.isPending ? <CircularProgress size={12} /> : <AutoAwesomeIcon />}
                onClick={() => refineMut.mutate()} disabled={anyLoading} sx={{ fontSize: 11 }} data-testid="live-script-refine-button" data-contract-source="/live/ai/refine-script">
                AI精修
              </Button>
              <Tooltip title="选择风格后对本条话术进行风格化精修" describeChild>
                <Button size="small" variant="outlined"
                  startIcon={styleRefineMut.isPending ? <CircularProgress size={12} /> : <TuneIcon />}
                  onClick={e => setStyleAnchor(e.currentTarget)}
                  disabled={anyLoading}
                  data-testid="live-script-style-refine-button"
                  data-contract-source="/live/ai/refine-script"
                  sx={{ fontSize: 11, color: 'secondary.main', borderColor: 'secondary.main' }}>
                  换风格精修
                </Button>
              </Tooltip>
              <Tooltip title="选择段落后针对该段落进行精准微调" describeChild>
                <Button size="small" variant="outlined"
                  startIcon={<AutoFixHighIcon />}
                  onClick={() => setShowSegmentRefine(true)}
                  disabled={anyLoading}
                  data-testid="live-script-segment-refine-button"
                  data-contract-source="/live/ai/refine-segment"
                  sx={{ fontSize: 11, color: 'info.main', borderColor: 'info.main' }}>
                  段内微调
                </Button>
              </Tooltip>
              <Button size="small" variant="outlined" startIcon={suggestMut.isPending ? <CircularProgress size={12} /> : <AutoAwesomeIcon />}
                onClick={() => suggestMut.mutate()} disabled={anyLoading} sx={{ fontSize: 11 }} data-testid="live-script-suggest-button" data-contract-source="/live/ai/suggest-improvement">
                改进建议
              </Button>
              <Button size="small" variant="outlined" startIcon={checkMut.isPending ? <CircularProgress size={12} /> : <SecurityIcon />}
                onClick={() => checkMut.mutate()} disabled={anyLoading} sx={{ fontSize: 11 }} data-testid="live-script-check-button" data-contract-source="/live/ai/check-violation">
                违规检测
              </Button>
              <Button size="small" variant="outlined" startIcon={<LibraryAddIcon />}
                onClick={() => saveToCopyMut.mutate()} disabled={anyLoading} sx={{ fontSize: 11 }} data-testid="live-script-save-copy-button" data-contract-source="/live/ai/save-to-copy-if-passed">
                存文案库
              </Button>
            </Box>
          )}

          {/* 风格精修 Popover */}
          <StyleRefinePopover
            anchorEl={styleAnchor}
            onClose={() => setStyleAnchor(null)}
            onRefine={style => styleRefineMut.mutate(style)}
            loading={styleRefineMut.isPending}
          />
        </Box>
      </Collapse>

      {/* Dialogs */}
      {showVersions && <VersionDialog scriptId={script.id} sessionId={script.sessionId} open={showVersions} onClose={() => setShowVersions(false)} />}
      {showChat && <AiChatDialog script={script} open={showChat} onClose={() => setShowChat(false)} />}
      {showComments && <CommentPanel script={script} open={showComments} onClose={() => setShowComments(false)} />}
      {showSegmentRefine && (
        <SegmentRefineDialog
          script={script}
          open={showSegmentRefine}
          onClose={() => setShowSegmentRefine(false)}
          onApplied={(content) => {
            handleStartEdit(script)
            setEditContent(content)
            onEdited(script.id)
          }}
        />
      )}
      <DurationPopover
        anchorEl={durationAnchor}
        onClose={() => setDurationAnchor(null)}
        currentValue={script.durationLimitSec ?? 0}
        onSave={sec => durationMut.mutate(sec)}
        loading={durationMut.isPending}
      />
      <SlotStylePopover
        anchorEl={slotStyleAnchor}
        onClose={() => setSlotStyleAnchor(null)}
        currentValue={script.style}
        onSave={style => slotStyleMut.mutate(style)}
        loading={slotStyleMut.isPending}
      />
      <SlotRequirementPopover
        anchorEl={slotReqAnchor}
        onClose={() => setSlotReqAnchor(null)}
        currentValue={script.requirement}
        scriptType={script.scriptType}
        onSave={req => slotReqMut.mutate(req)}
        loading={slotReqMut.isPending}
      />
    </Box>
  )
})

// ─── ScriptTabContent ─────────────────────────────────────────────────────────
export function ScriptTabContent() {
  const { scripts, session } = useCoreData()
  const orderedScripts = useMemo(() => sortLiveScripts(scripts), [scripts])
  const [filter, setFilter] = useState<EditFilter>('all')
  const [typeFilter, setTypeFilter] = useState<string>('all')
  const [editedIds, setEditedIds] = useState<Set<number>>(new Set())
  const [renderLimit, setRenderLimit] = useState(INITIAL_SCRIPT_RENDER_LIMIT)

  const handleEdited = useCallback((id: number) => {
    setEditedIds(prev => new Set([...prev, id]))
  }, [])

  const { query, setQuery, filtered: searchFiltered, clear: clearSearch } = useScriptSearch(orderedScripts)
  const progress_data = useEditProgress(orderedScripts, editedIds)
  useAutoQualityCheck(orderedScripts)

  const { data: unresolvedCountMap = {} } = useQuery({
    queryKey: ['script-comment-unresolved-map', session?.id],
    queryFn: () => liveApi.scriptCommentUnresolvedByScript(Number(session?.id)),
    enabled: Boolean(session?.id) && orderedScripts.length > 0,
    staleTime: 60 * 1000,
  })

  const activatedCount = orderedScripts.filter(s => s.status === 1).length
  const progress = orderedScripts.length === 0 ? 0 : Math.round((activatedCount / orderedScripts.length) * 100)

  const filteredScripts = useMemo(() => searchFiltered.filter(s => {
    const typeOk = typeFilter === 'all' ? true : s.scriptType === typeFilter
    let editOk = true
    if (filter === 'generated') editOk = (s.scriptContent ?? '').trim().length > 0
    else if (filter === 'edited') editOk = editedIds.has(s.id)
    else if (filter === 'unfilled') editOk = (s.scriptContent ?? '').trim().length === 0
    else if (filter === 'needsReview') editOk = s.status === 0 && (s.scriptContent ?? '').trim().length > 0
    return editOk && typeOk
  }), [editedIds, filter, searchFiltered, typeFilter])

  useEffect(() => {
    setRenderLimit(INITIAL_SCRIPT_RENDER_LIMIT)
  }, [filter, query, orderedScripts.length, typeFilter])

  const visibleScripts = filteredScripts.slice(0, renderLimit)
  const hasMoreScripts = visibleScripts.length < filteredScripts.length
  const shouldAutoExpandAll = orderedScripts.length <= AUTO_COLLAPSE_SCRIPT_THRESHOLD
  const shouldAutoExpandFirst = !shouldAutoExpandAll && query.trim().length === 0 && filter === 'all' && typeFilter === 'all'

  // 统计各类型数量
  const typeCounts = useMemo(() => orderedScripts.reduce<Record<string, number>>((acc, s) => {
    const t = s.scriptType ?? 'unknown'
    acc[t] = (acc[t] ?? 0) + 1
    return acc
  }, {}), [orderedScripts])

  return (
    <Box
      data-testid="live-script-tab-workbench"
      data-contract-scope="live-script-edit-review"
      data-ready-endpoints={SCRIPT_TAB_READY_ENDPOINTS.join('|')}
      data-context-endpoints={SCRIPT_TAB_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={SCRIPT_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={session?.id ?? ''}
      data-script-count={orderedScripts.length}
      data-filtered-count={filteredScripts.length}
      data-rendered-count={visibleScripts.length}
      data-render-limit={renderLimit}
      data-batched-rendering="true"
      data-default-expanded={shouldAutoExpandAll ? 'true' : 'false'}
      data-default-first-expanded={shouldAutoExpandFirst ? 'true' : 'false'}
      data-active-filter={filter}
      data-type-filter={typeFilter}
      data-query={query}
      data-activated-count={activatedCount}
      data-no-local-script-list-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}
    >
      {/* Toolbar row 1 */}
      <Box
        data-testid="live-script-tab-toolbar"
        data-contract-source={SCRIPT_TAB_CONTEXT_ENDPOINTS.join('|')}
        sx={{
          px: 2, pt: 1, pb: 0.5, flexShrink: 0, borderBottom: 0,
          display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap',
        }}
      >
        <Typography variant="body2" fontWeight={600}>话术微调</Typography>
        <Chip label={`${orderedScripts.length} 条`} size="small" color="primary" variant="outlined" />
        {activatedCount > 0 && (
          <Chip label={`已激活 ${activatedCount}`} size="small" color="success" variant="outlined" />
        )}

        {/* 搜索框 */}
        <TextField
          size="small"
          placeholder="搜索话术标题/内容…"
          value={query}
          onChange={e => setQuery(e.target.value)}
          sx={{ width: 200, '& .MuiInputBase-root': { fontSize: 12 } }}
          inputProps={{
            'data-testid': 'live-script-search-input',
            'data-contract-source': '/live/script/by-session',
          }}
          InputProps={{
            startAdornment: <InputAdornment position="start"><SearchIcon sx={{ fontSize: 16 }} /></InputAdornment>,
            endAdornment: query ? (
              <InputAdornment position="end">
                <IconButton size="small" onClick={clearSearch}><ClearIcon sx={{ fontSize: 14 }} /></IconButton>
              </InputAdornment>
            ) : null,
          }}
        />

        <Box sx={{ flex: 1 }} />

        {/* 编写进度统计 */}
        {orderedScripts.length > 0 && (
          <Tooltip title={`已填写 ${progress_data.filled}/${progress_data.total} 条，已编辑 ${progress_data.edited} 条，平均字数 ${progress_data.avgWordCount}`}>
            <Stack direction="row" alignItems="center" spacing={0.5} sx={{ cursor: 'default' }}>
              <AssignmentTurnedInIcon sx={{ fontSize: 14, color: 'text.secondary' }} />
              <Typography variant="caption" color="text.secondary">
                {progress_data.completionRate}%
              </Typography>
            </Stack>
          </Tooltip>
        )}

        {/* 激活进度 */}
        {orderedScripts.length > 0 && (
          <Stack direction="row" alignItems="center" spacing={1} sx={{ minWidth: 160 }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
              激活 {activatedCount}/{orderedScripts.length}
            </Typography>
            <LinearProgress variant="determinate" value={progress}
              sx={{ flex: 1, height: 6, borderRadius: 3 }}
              color={progress === 100 ? 'success' : 'primary'}
            />
            {progress === 100 && <CheckCircleIcon sx={{ fontSize: 16, color: 'success.main' }} />}
          </Stack>
        )}
      </Box>

      {/* Toolbar row 2: 状态筛选 + 类型筛选 */}
      <Box
        data-testid="live-script-tab-filterbar"
        data-contract-source="/live/script/by-session"
        sx={{
          px: 2, py: 0.75, flexShrink: 0, borderBottom: 1, borderColor: 'divider',
          display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap',
        }}
      >
        <ToggleButtonGroup size="small" value={filter} exclusive onChange={(_, v) => v && setFilter(v)}
          sx={{ '& .MuiToggleButton-root': { fontSize: 11, py: 0.25, px: 1 } }}>
          <ToggleButton value="all">全部</ToggleButton>
          <ToggleButton value="generated">已生成</ToggleButton>
          <ToggleButton value="edited">已编辑</ToggleButton>
          <ToggleButton value="unfilled">未填写</ToggleButton>
          <ToggleButton value="needsReview">待审核</ToggleButton>
        </ToggleButtonGroup>

        <Box sx={{ width: 1, height: 16, borderLeft: 1, borderColor: 'divider', mx: 0.5 }} />

        {/* 类型筛选 */}
        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
          <Chip
            label="全部类型"
            size="small"
            variant={typeFilter === 'all' ? 'filled' : 'outlined'}
            color={typeFilter === 'all' ? 'primary' : 'default'}
            onClick={() => setTypeFilter('all')}
            sx={{ cursor: 'pointer', fontSize: 11 }}
          />
          {Object.entries(typeCounts).map(([t, cnt]) => {
            const info = SCRIPT_TYPE_LABELS[t]
            return (
              <Chip key={t}
                label={`${info?.label ?? t} ${cnt}`}
                size="small"
                variant={typeFilter === t ? 'filled' : 'outlined'}
                color={typeFilter === t ? (info?.color ?? 'default') : 'default'}
                onClick={() => setTypeFilter(t)}
                sx={{ cursor: 'pointer', fontSize: 11 }}
              />
            )
          })}
        </Box>

        {/* 场次信息 */}
        {session && (
          <Tooltip title={`场次：${session.liveTitle}`}>
            <Chip label={session.liveTitle} size="small" variant="outlined" sx={{ maxWidth: 120, fontSize: 10 }} />
          </Tooltip>
        )}
      </Box>

      {/* Script list */}
      <Box
        data-testid="live-script-list-surface"
        data-contract-source="/live/script/by-session"
        data-no-local-script-list-fallback="true"
        sx={{ flex: 1, overflow: 'auto', p: 1.5 }}
      >
        {filteredScripts.length === 0 ? (
          <Box
            data-testid="live-script-empty-state"
            data-contract-source="/live/script/by-session"
            data-no-local-script-list-fallback="true"
            sx={{ textAlign: 'center', py: 8, color: 'text.secondary' }}
          >
            <Typography variant="body2">
              {orderedScripts.length === 0
                ? '暂无话术，请先在「AI生成」步骤生成话术'
                : query
                  ? `未找到包含「${query}」的话术`
                  : '没有符合条件的话术'}
            </Typography>
          </Box>
        ) : (
          <Stack spacing={1.5}>
            {visibleScripts.map((s, index) => (
              <ScriptCard
                key={s.id}
                script={s}
                isEdited={editedIds.has(s.id)}
                unresolvedCount={Number(unresolvedCountMap[String(s.id)] ?? 0)}
                defaultExpanded={shouldAutoExpandAll || (shouldAutoExpandFirst && index === 0)}
                onEdited={handleEdited}
              />
            ))}
            {hasMoreScripts && (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 1 }}>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => setRenderLimit(limit => limit + SCRIPT_RENDER_BATCH_SIZE)}
                  data-testid="live-script-load-more-button"
                >
                  加载更多话术（{visibleScripts.length}/{filteredScripts.length}）
                </Button>
              </Box>
            )}
          </Stack>
        )}
      </Box>
    </Box>
  )
}






