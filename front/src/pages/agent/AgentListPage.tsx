import { useState, useCallback, useRef, useEffect } from 'react'
import {
  Box, Stack, Typography, TextField, Button, Chip, Paper, Drawer,
  FormControl, InputLabel, Select, MenuItem, IconButton, Tooltip,
  Divider, Tabs, Tab, FormGroup, FormControlLabel, Checkbox,
  CircularProgress, List, ListItemButton, ListItemText, Collapse,
  Menu, Alert,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import ChatIcon from '@mui/icons-material/Chat'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import SendIcon from '@mui/icons-material/Send'
import StopIcon from '@mui/icons-material/Stop'
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined'
import ThumbDownOutlinedIcon from '@mui/icons-material/ThumbDownOutlined'
import DownloadIcon from '@mui/icons-material/Download'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import AddCommentIcon from '@mui/icons-material/AddComment'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog } from '@/components/base'
import { agentApi, type Agent, type AgentSave, type ChatMessage as ApiChatMessage, type ToolCall as ApiToolCall } from '@/api/agent'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { ssePost } from '@/utils/sse-client'
import MarkdownViewer from '@/components/MarkdownViewer'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray, readTotal } from '@/utils/response-normalize'
import { agentMessageBubbleSx, agentStreamingBubbleSx, agentStreamStatusBarSx } from './agentMessageBubbleStyles'

// ─── Agent type config ───────────────────────────────────────────────────────
const AGENT_TYPES = [
  { value: 0, label: '自定义', color: 'default' as const, emoji: '🤖' },
  { value: 1, label: '话术生成', color: 'info' as const, emoji: '🤖' },
  { value: 2, label: '违规检测', color: 'error' as const, emoji: '🔍' },
  { value: 3, label: '商品分析', color: 'warning' as const, emoji: '📦' },
  { value: 4, label: '场次规划', color: 'secondary' as const, emoji: '📅' },
]

const AVAILABLE_TOOLS = [
  { key: 'kb_rag_search', label: '知识库 RAG 检索' },
  { key: 'product_search', label: '商品搜索' },
  { key: 'compliance_check', label: '违规检测' },
  { key: 'live_session_query', label: '场次查询' },
  { key: 'script_generate', label: '话术生成' },
]

function getTypeConfig(val: number) {
  return AGENT_TYPES.find(t => t.value === val) ?? AGENT_TYPES[0]
}

function getToolLabel(name: string) {
  return AVAILABLE_TOOLS.find(t => t.key === name)?.label ?? name
}

function formatStreamStatus(raw: unknown): string {
  if (typeof raw === 'string') {
    const text = raw.trim()
    if (!text) return ''
    if (text.startsWith('{') || text.startsWith('[')) {
      try {
        return formatStreamStatus(JSON.parse(text))
      } catch {
        return text
      }
    }
    return text
  }
  if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
    const row = raw as Record<string, unknown>
    const value = row.status ?? row.message ?? row.content ?? row.text
    if (value !== undefined && value !== null) return String(value).trim()
    return JSON.stringify(row)
  }
  return raw === undefined || raw === null ? '' : String(raw)
}

function formatActionError(action: string, endpoint: string, error: unknown): string {
  return `${action}失败：${getErrorMessage(error)}。来源：${endpoint}，页面已保留当前会话和历史消息。`
}

// ─── SystemPromptHighlight ────────────────────────────────────────────────────
function SystemPromptHighlight({ text }: { text: string }) {
  const parts = text.split(/(\{[^}]+\})/g)
  return (
    <Box sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 13, lineHeight: 1.6 }}>
      {parts.map((part, i) =>
        /^\{[^}]+\}$/.test(part)
          ? <Box component="span" key={i} sx={{ color: 'warning.main', fontWeight: 600 }}>{part}</Box>
          : part
      )}
    </Box>
  )
}

// ─── ToolCallCard ─────────────────────────────────────────────────────────────
interface ToolCall { name: string; args: unknown; result?: string }
function ToolCallCard({ tc }: { tc: ApiToolCall }) {
  const [open, setOpen] = useState(false)
  // Map ApiToolCall to local ToolCall format
  const toolCall: ToolCall = {
    name: tc.function?.name ?? tc.type,
    args: tc.function?.arguments ?? '',
    result: tc.result,
  }
  const displayName = getToolLabel(toolCall.name)
  return (
    <Paper variant="outlined" sx={{ mt: 0.5, borderRadius: 1, overflow: 'hidden' }}>
      <Stack direction="row" alignItems="center" sx={{ px: 1.5, py: 0.75, cursor: 'pointer', bgcolor: 'action.hover' }} onClick={() => setOpen(v => !v)}>
        <SmartToyIcon sx={{ fontSize: 14, mr: 0.75, color: 'text.secondary' }} />
        <Typography variant="caption" sx={{ flex: 1, fontWeight: 700 }}>{displayName}</Typography>
        {displayName !== toolCall.name && (
          <Typography variant="caption" color="text.secondary" sx={{ mr: 0.75, fontFamily: 'monospace' }}>{toolCall.name}</Typography>
        )}
        {open ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
      </Stack>
      <Collapse in={open}>
        <Box sx={{ px: 1.5, py: 1, borderTop: '1px solid', borderColor: 'divider' }}>
          <Typography variant="caption" color="text.secondary">参数：</Typography>
          <Box
            component="pre"
            data-testid="agent-tool-call-args-preview"
            sx={(theme) => ({
              fontSize: 11,
              mt: 0.5,
              overflowX: 'auto',
              m: 0,
              bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.default : theme.palette.grey[50],
              p: 1,
              borderRadius: 1,
            })}
          >
            {JSON.stringify(toolCall.args, null, 2)}
          </Box>
          {toolCall.result && (
            <>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>结果：</Typography>
              <Box sx={{ fontSize: 12, mt: 0.5, color: 'text.secondary', maxHeight: 80, overflow: 'auto' }}>{tc.result}</Box>
            </>
          )}
        </Box>
      </Collapse>
    </Paper>
  )
}
// ─── Types ───────────────────────────────────────────────────────────────────
type ChatMessage = ApiChatMessage
interface StreamStatusItem {
  id: number
  text: string
  done: boolean
}

// ─── AgentEditDrawer ──────────────────────────────────────────────────────────
function AgentEditDrawer({
  open, form, kbList, onClose, onSave, saving, errorText,
}: {
  open: boolean
  form: Partial<AgentSave>
  kbList: { id: number; kbName: string }[]
  onClose: () => void
  onSave: (f: Partial<AgentSave>) => void
  saving: boolean
  errorText?: string
}) {
  const [local, setLocal] = useState<Partial<AgentSave>>(form)
  useEffect(() => { setLocal(form) }, [form])

  const selectedTools: string[] = (() => {
    try { return JSON.parse(local.modelConfig ?? '{}').tools ?? [] } catch { return [] }
  })()

  const toggleTool = (key: string) => {
    const next = selectedTools.includes(key)
      ? selectedTools.filter(t => t !== key)
      : [...selectedTools, key]
    const cfg = (() => { try { return JSON.parse(local.modelConfig ?? '{}') } catch { return {} } })()
    setLocal(l => ({ ...l, modelConfig: JSON.stringify({ ...cfg, tools: next }) }))
  }

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: 560 } }}>
      <Stack sx={{ height: '100%' }}>
        <Box sx={{ px: 3, py: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="h6">{local.id ? '编辑智能体' : '新建智能体'}</Typography>
        </Box>
        <Box sx={{ flex: 1, overflow: 'auto', px: 3, py: 2 }}>
          <Stack spacing={2.5}>
            {errorText && (
              <Alert severity="error">
                {errorText}
              </Alert>
            )}
            <TextField label="名称 *" value={local.agentName ?? ''} onChange={e => setLocal(l => ({ ...l, agentName: e.target.value }))} fullWidth size="small" />
            <FormControl fullWidth size="small">
              <InputLabel>类型</InputLabel>
              <Select label="类型" value={local.agentType ?? 0} onChange={e => setLocal(l => ({ ...l, agentType: Number(e.target.value) }))}>
                {AGENT_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.emoji} {t.label}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>绑定知识库</InputLabel>
              <Select label="绑定知识库" value={(() => { try { return JSON.parse(local.modelConfig ?? '{}').kbId ?? '' } catch { return '' } })()} onChange={e => { const cfg = (() => { try { return JSON.parse(local.modelConfig ?? '{}') } catch { return {} } })(); setLocal(l => ({ ...l, modelConfig: JSON.stringify({ ...cfg, kbId: e.target.value }) })) }}>
                <MenuItem value="">不绑定</MenuItem>
                {kbList.map(kb => <MenuItem key={kb.id} value={kb.id}>{kb.kbName}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="描述" value={local.description ?? ''} onChange={e => setLocal(l => ({ ...l, description: e.target.value }))} fullWidth size="small" multiline minRows={2} />
            <Box>
              <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>系统提示词</Typography>
              <TextField
                fullWidth multiline minRows={5}
                value={local.systemPrompt ?? ''}
                onChange={e => setLocal(l => ({ ...l, systemPrompt: e.target.value }))}
                placeholder="支持 {变量名} 占位符"
                size="small"
              />
              {local.systemPrompt && (
                <Box
                  data-testid="agent-system-prompt-preview"
                  sx={(theme) => ({
                    mt: 1,
                    p: 1.5,
                    bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.default : theme.palette.grey[50],
                    borderRadius: 1,
                    border: '1px solid',
                    borderColor: 'divider',
                  })}
                >
                  <Typography variant="caption" color="text.secondary">预览：</Typography>
                  <SystemPromptHighlight text={local.systemPrompt} />
                </Box>
              )}
            </Box>
            <Box>
              <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>工具调用</Typography>
              <FormGroup>
                {AVAILABLE_TOOLS.map(t => (
                  <FormControlLabel key={t.key} control={<Checkbox size="small" checked={selectedTools.includes(t.key)} onChange={() => toggleTool(t.key)} />} label={<Typography variant="body2">{t.label}</Typography>} />
                ))}
              </FormGroup>
            </Box>
          </Stack>
        </Box>
        <Box sx={{ px: 3, py: 2, borderTop: '1px solid', borderColor: 'divider' }}>
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={onClose}>取消</Button>
            <Button variant="contained" onClick={() => onSave(local)} disabled={saving || !local.agentName}>
              {saving ? <CircularProgress size={16} /> : '保存'}
            </Button>
          </Stack>
        </Box>
      </Stack>
    </Drawer>
  )
}
// ─── ChatPanel ───────────────────────────────────────────────────────────────
function ChatPanel({ agentId }: { agentId: number | null }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [activeConvId, setActiveConvId] = useState<number | null>(null)
  const [input, setInput] = useState('')
  const [streamContent, setStreamContent] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [tokenUsage, setTokenUsage] = useState({ input: 0, output: 0 })
  const [streamStatuses, setStreamStatuses] = useState<StreamStatusItem[]>([])
  const [toolStatus, setToolStatus] = useState<Array<{ tool: string; status: 'calling' | 'done' | 'error'; description?: string; result?: string }>>([])
  const [streamError, setStreamError] = useState('')
  const [actionError, setActionError] = useState('')
  const [ctxMenuAnchor, setCtxMenuAnchor] = useState<HTMLElement | null>(null)
  const [ctxConvId, setCtxConvId] = useState<number | null>(null)
  const [renameId, setRenameId] = useState<number | null>(null)
  const [renameVal, setRenameVal] = useState('')
  const abortRef = useRef<AbortController | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const streamContentRef = useRef('')

  const { data: agentInfo } = useQuery({
    queryKey: ['agent-detail', agentId],
    queryFn: () => agentApi.get(agentId!),
    enabled: agentId !== null && agentId > 0,
  })

  const toolCallsUsed = messages.flatMap(m => (m.toolCalls ?? [])).map(tc => tc.function?.name ?? tc.type).filter((v, i, a) => a.indexOf(v) === i)
  const ratedMsgs = messages.filter(m => m.rating === 'up' || m.rating === 'down')
  const qualityScore = ratedMsgs.length > 0 ? (ratedMsgs.filter(m => m.rating === 'up').length / ratedMsgs.length * 5) : null
  const totalTokens = tokenUsage.input + tokenUsage.output
  const costEstimate = (totalTokens / 1000 * 0.002).toFixed(3)

  const { data: convList, isError: convListError, error: convListLoadError, refetch: refetchConversations } = useQuery({
    queryKey: ['agent-conversations', agentId],
    queryFn: () => agentApi.conversationList(agentId!),
    enabled: agentId !== null && agentId > 0,
  })
  const conversations = normalizeArray<Awaited<ReturnType<typeof agentApi.conversationList>>[number]>(convList)

  const { data: msgList, isError: msgListError, error: msgListLoadError, refetch: refetchMessages } = useQuery({
    queryKey: ['agent-messages', activeConvId],
    queryFn: () => agentApi.messageList(activeConvId!),
    enabled: activeConvId !== null,
  })

  useEffect(() => {
    if (msgList) setMessages(normalizeArray<ChatMessage>(msgList))
  }, [msgList])

  useEffect(() => {
    const input = messages.reduce((sum, msg) => sum + Number(msg.tokenUsage?.input ?? 0), 0)
    const output = messages.reduce((sum, msg) => sum + Number(msg.tokenUsage?.output ?? 0), 0)
    setTokenUsage({ input, output })
  }, [messages])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamContent, streamStatuses])

  useEffect(() => {
    setActiveConvId(null)
    setMessages([])
    setStreamStatuses([])
    setToolStatus([])
    setStreamError('')
    setActionError('')
  }, [agentId])

  const createConvMut = useMutation({
    mutationFn: () => agentApi.conversationCreate(agentId!, `对话 ${new Date().toLocaleTimeString()}`),
    onSuccess: (id) => {
      setActiveConvId(Number(id))
      setActionError('')
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
    },
    onError: (error) => {
      setActionError(formatActionError('创建对话', '/agent/conversation/create', error))
      toast('创建对话失败', 'error')
    },
  })

  const deleteConvMut = useMutation({
    mutationFn: (id: number) => agentApi.conversationDelete(id),
    onSuccess: (_data, id) => {
      if (activeConvId === id) { setActiveConvId(null); setMessages([]) }
      setActionError('')
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
    },
    onError: (error) => {
      setActionError(formatActionError('删除对话', '/agent/conversation/delete', error))
      toast('删除对话失败', 'error')
    },
  })

  const clearConversationMut = useMutation({
    mutationFn: (id: number) => agentApi.conversationDelete(id),
    onSuccess: (_data, id) => {
      if (activeConvId === id) {
        setActiveConvId(null)
        setMessages([])
        setTokenUsage({ input: 0, output: 0 })
        setStreamStatuses([])
        setToolStatus([])
        setStreamError('')
      }
      setActionError('')
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
      qc.removeQueries({ queryKey: ['agent-messages', id] })
    },
    onError: (error) => {
      setActionError(formatActionError('清空上下文', '/agent/conversation/delete', error))
      toast('清空上下文失败', 'error')
    },
  })

  const rateMut = useMutation({
    mutationFn: ({ messageId, rating }: { messageId: number; rating: 'up' | 'down' }) =>
      agentApi.messageRate(messageId, rating),
    onSuccess: () => {
      toast('已提交评分', 'success')
      setActionError('')
      qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })
    },
    onError: (e: Error) => {
      setActionError(formatActionError('评分', '/agent/message/rate', e))
      toast(e.message || '评分失败', 'error')
    },
  })

  const handleSend = useCallback(async () => {
    if (!input.trim() || isStreaming || !activeConvId) return
    const userMsg: ChatMessage = { id: Date.now(), role: 'user', content: input.trim(), createdAt: new Date().toISOString() }
    setMessages(m => [...m, userMsg])
    setInput('')
    setIsStreaming(true)
    setStreamContent('')
    setStreamStatuses([])
    setToolStatus([])
    setStreamError('')
    setActionError('')
    streamContentRef.current = ''
    try {
      abortRef.current = ssePost(
        '/agent/chat-stream',
        { agentId, conversationId: activeConvId, content: userMsg.content },
        {
          onStatus: (statusText) => {
            const text = formatStreamStatus(statusText)
            if (!text) return
            setStreamStatuses(prev => {
              const next = prev.map(item => ({ ...item, done: true }))
              const last = next[next.length - 1]
              if (last?.text === text) return [...next.slice(0, -1), { ...last, done: false }]
              return [...next, { id: Date.now() + next.length, text, done: false }]
            })
          },
          onSkillStart: ({ tool, description }) => {
            setToolStatus(prev => [...prev.filter(t => t.tool !== tool), { tool, status: 'calling', description }])
          },
          onSkillEnd: ({ tool, status, error }) => {
            const nextStatus = status === 'success' || status === 'done' ? 'done' : 'error'
            setToolStatus(prev => prev.map(t => t.tool === tool ? {
              ...t,
              status: nextStatus,
              description: error ?? t.description,
              result: nextStatus === 'done' ? '执行成功' : `错误: ${error ?? ''}`,
            } : t))
          },
          onChunk: (chunkData) => {
            let chunk = ''
            const raw = String(chunkData ?? '')
            if (!raw) return
            try {
              const ev = JSON.parse(raw)
              chunk = String(ev.content ?? ev.delta ?? ev.message ?? '')
            } catch {
              chunk = raw
            }
            if (!chunk) return
            streamContentRef.current += chunk
            setStreamContent(prev => prev + chunk)
          },
          onDone: (doneData) => {
            let finalContent = streamContentRef.current
            if (!finalContent && doneData && typeof doneData === 'object' && 'content' in doneData) {
              finalContent = String((doneData as { content?: unknown }).content ?? '')
            }
            if (finalContent.trim()) {
              setMessages(m => [...m, {
                id: Date.now() + 1,
                role: 'assistant',
                content: finalContent,
                createdAt: new Date().toISOString(),
                tokenUsage: { input: 0, output: 0 },
              }])
            }
            setStreamContent('')
            streamContentRef.current = ''
            setIsStreaming(false)
            setStreamStatuses(prev => prev.map(item => ({ ...item, done: true })))
            qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })
            qc.refetchQueries({ queryKey: ['agent-messages', activeConvId], type: 'active' })
            abortRef.current = null
          },
          onError: (err) => {
            const message = err.message || '流式对话失败'
            setStreamError(message)
            setStreamContent('')
            streamContentRef.current = ''
            setIsStreaming(false)
            setStreamStatuses(prev => prev.map(item => ({ ...item, done: true })))
            setToolStatus(prev => prev.map(item => item.status === 'calling'
              ? { ...item, status: 'error', result: `错误: ${message}` }
              : item
            ))
            toast(`发送失败：${message}`, 'error')
            qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })
            abortRef.current = null
          },
        }
      )
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '发送失败'
      setStreamError(message)
      setIsStreaming(false)
      toast(message, 'error')
    }
  }, [input, isStreaming, activeConvId, agentId, toast, qc])

  const handleStop = () => {
    abortRef.current?.abort()
    setIsStreaming(false)
    setStreamStatuses(prev => prev.map(item => ({ ...item, done: true })))
  }

  const currentStreamStatus = streamStatuses.find(item => !item.done) ?? streamStatuses[streamStatuses.length - 1]

  const handleExport = useCallback(async () => {
    if (!activeConvId) return
    try {
      const res = await agentApi.exportConversation(activeConvId)
      window.open(res.downloadUrl, '_blank')
      setActionError('')
    } catch (error) {
      setActionError(formatActionError('导出对话', '/agent/conversation/export', error))
      toast('导出失败', 'error')
    }
  }, [activeConvId, toast])

  const handleRename = useCallback((conversationId: number, title: string) => {
    const nextTitle = title.trim()
    if (!nextTitle) {
      setRenameId(null)
      return
    }
    agentApi.conversationRename(conversationId, nextTitle)
      .then(() => {
        setActionError('')
        qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
      })
      .catch((error) => {
        setActionError(formatActionError('重命名对话', '/agent/conversation/rename', error))
        toast('重命名失败', 'error')
      })
      .finally(() => setRenameId(null))
  }, [agentId, qc, toast])

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void handleSend() }
  }
  if (!agentId) {
    return (
      <Box
        data-testid="embedded-agent-chat-empty-agent"
        data-no-local-agent-fallback="true"
        sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'text.secondary' }}
      >
        <Typography>请从左侧选择一个智能体开始对话</Typography>
      </Box>
    )
  }

  return (
    <Box
      data-testid="embedded-agent-chat-panel"
      data-ready-endpoints="/agent/get|/agent/conversation/list|/agent/conversation/create|/agent/conversation/delete|/agent/conversation/rename|/agent/message/list|/agent/message/rate|/agent/conversation/export|/agent/chat-stream"
      data-unsupported-endpoints="/agent/local-conversation|/agent/local-message|/agent/static-message|/agent/message/send-fallback|/agent/status/local|/agent/markdown/static-render"
      data-no-local-conversation-fallback="true"
      data-no-local-message-fallback="true"
      data-sse-status-visible="true"
      data-markdown-renderer="MarkdownViewer"
      sx={{ display: 'flex', height: '100%' }}
    >
      {/* 对话列表 */}
      <Box sx={{ width: 220, borderRight: '1px solid', borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Button fullWidth size="small" variant="outlined" startIcon={<AddCommentIcon />} onClick={() => createConvMut.mutate()} disabled={createConvMut.isPending}>新建对话</Button>
        </Box>
        {convListError && (
          <Alert
            severity="error"
            data-testid="embedded-agent-conversation-list-error"
            data-no-local-conversation-fallback="true"
            action={<Button color="inherit" size="small" onClick={() => refetchConversations()}>重试</Button>}
            sx={{ m: 1 }}
          >
            对话列表加载失败（POST /agent/conversation/list）：{getErrorMessage(convListLoadError)}。页面不会补本地对话。
          </Alert>
        )}
        <List dense sx={{ flex: 1, overflow: 'auto' }}>
          {conversations.map(c => (
            <ListItemButton
              key={c.id}
              selected={activeConvId === c.id}
              onClick={() => setActiveConvId(c.id)}
              sx={{ pr: 0.5 }}
            >
              <ListItemText
                primary={renameId === c.id
                  ? <TextField size="small" autoFocus value={renameVal} onChange={e => setRenameVal(e.target.value)}
                      onBlur={() => handleRename(c.id, renameVal)}
                      onKeyDown={e => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur() }}
                      onClick={e => e.stopPropagation()} sx={{ width: '100%' }} />
                  : <Typography variant="body2" noWrap>{c.title}</Typography>
                }
                secondary={<Typography variant="caption" color="text.secondary">{formatDate(c.createTime)}</Typography>}
              />
              <IconButton
                size="small"
                aria-label={`内嵌会话 ${c.title} 更多操作`}
                onClick={e => { e.stopPropagation(); setCtxMenuAnchor(e.currentTarget); setCtxConvId(c.id); setRenameVal(c.title) }}
              >
                <MoreVertIcon fontSize="small" />
              </IconButton>
            </ListItemButton>
          ))}
        </List>
        <Menu anchorEl={ctxMenuAnchor} open={Boolean(ctxMenuAnchor)} onClose={() => setCtxMenuAnchor(null)}>
          <MenuItem onClick={() => { setRenameId(ctxConvId); setCtxMenuAnchor(null) }}>重命名</MenuItem>
          <MenuItem sx={{ color: 'error.main' }} onClick={() => { if (ctxConvId) deleteConvMut.mutate(ctxConvId); setCtxMenuAnchor(null) }}>删除</MenuItem>
        </Menu>
      </Box>

      {/* 聊天区 */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        {!activeConvId ? (
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, color: 'text.secondary' }}>
            <Typography>选择或新建一个对话</Typography>
          </Box>
        ) : (
          <>
            {/* Token usage bar */}
            <Box sx={{ px: 2, py: 0.75, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="caption" color="text.secondary">Token 用量：输入 {tokenUsage.input} / 输出 {tokenUsage.output}</Typography>
              <Box sx={{ flex: 1 }} />
              <Tooltip title="导出为 .md">
                <IconButton size="small" onClick={handleExport}><DownloadIcon fontSize="small" /></IconButton>
              </Tooltip>
            </Box>
            {/* Messages */}
            <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
              {actionError && (
                <Alert
                  severity="error"
                  data-testid="embedded-agent-action-error"
                  data-input-retained="true"
                  data-no-local-mutation="true"
                  sx={{ mb: 1.5 }}
                  onClose={() => setActionError('')}
                >
                  {actionError}
                </Alert>
              )}
              {streamError && (
                <Alert
                  severity="error"
                  data-testid="embedded-agent-stream-error"
                  data-input-retained="true"
                  data-no-static-message-fallback="true"
                  sx={{ mb: 1.5 }}
                  onClose={() => setStreamError('')}
                >
                  流式对话失败：{streamError}
                </Alert>
              )}
              {msgListError && (
                <Alert
                  severity="error"
                  data-testid="embedded-agent-message-list-error"
                  data-no-local-message-fallback="true"
                  action={<Button color="inherit" size="small" onClick={() => refetchMessages()}>重试</Button>}
                  sx={{ mb: 1.5 }}
                >
                  消息列表加载失败（POST /agent/message/list）：{getErrorMessage(msgListLoadError)}。页面不会补本地消息或静态 Markdown。
                </Alert>
              )}
              {toolStatus.length > 0 && (
                <Paper variant="outlined" sx={{ p: 1.25, mb: 1.5, bgcolor: 'action.hover' }}>
                  <Typography variant="caption" fontWeight={700} color="text.secondary">技能调用状态</Typography>
                  <Stack spacing={0.75} sx={{ mt: 0.75 }}>
                    {toolStatus.map((t) => (
                      <Stack key={t.tool} direction="row" alignItems="center" spacing={1}>
                        <SmartToyIcon sx={{ fontSize: 14, color: t.status === 'error' ? 'error.main' : 'primary.main' }} />
                        <Typography variant="body2" fontWeight={600}>{getToolLabel(t.tool)}</Typography>
                        {t.status === 'calling' && <CircularProgress size={13} />}
                        <Chip size="small" label={t.status === 'calling' ? '执行中' : t.status === 'done' ? '完成' : '失败'} color={t.status === 'error' ? 'error' : t.status === 'done' ? 'success' : 'info'} />
                        {t.description && <Typography variant="caption" color="text.secondary" noWrap>{t.description}</Typography>}
                      </Stack>
                    ))}
                  </Stack>
                </Paper>
              )}
              {messages.map((msg, index) => (
                <Box key={msg.id} sx={{ mb: 2, display: 'flex', flexDirection: msg.role === 'user' ? 'row-reverse' : 'row', gap: 1 }}>
                  <Paper
                    elevation={0}
                    data-testid={`embedded-agent-${msg.role === 'user' ? 'user' : 'assistant'}-message-bubble`}
                    data-message-source="server"
                    data-markdown-renderer={msg.role === 'assistant' ? 'MarkdownViewer' : undefined}
                    data-no-static-message-fallback="true"
                    sx={agentMessageBubbleSx(msg.role, { bordered: true, userVariant: 'soft' })}
                  >
                    {msg.role === 'user'
                      ? <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{msg.content}</Typography>
                      : <MarkdownViewer content={msg.content} compact />}
                    {msg.toolCalls?.map((tc, i) => <ToolCallCard key={i} tc={tc} />)}
                    {msg.role === 'assistant' && (
                      <Stack direction="row" spacing={0.5} sx={{ mt: 0.75 }} alignItems="center">
                        <IconButton
                          size="small"
                          aria-label={`点赞内嵌第 ${index + 1} 条消息`}
                          color={msg.rating === 'up' ? 'primary' : 'default'}
                          onClick={() => rateMut.mutate({ messageId: msg.id, rating: 'up' })}
                          disabled={msg.id <= 0 || rateMut.isPending}
                        >
                          <ThumbUpOutlinedIcon sx={{ fontSize: 14 }} />
                        </IconButton>
                        <IconButton
                          size="small"
                          aria-label={`点踩内嵌第 ${index + 1} 条消息`}
                          color={msg.rating === 'down' ? 'error' : 'default'}
                          onClick={() => rateMut.mutate({ messageId: msg.id, rating: 'down' })}
                          disabled={msg.id <= 0 || rateMut.isPending}
                        >
                          <ThumbDownOutlinedIcon sx={{ fontSize: 14 }} />
                        </IconButton>
                        {msg.tokenUsage && <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>↑{msg.tokenUsage.input} ↓{msg.tokenUsage.output}</Typography>}
                      </Stack>
                    )}
                  </Paper>
                </Box>
              ))}
              {isStreaming && streamContent && (
                <Box sx={{ mb: 2, display: 'flex', gap: 1 }}>
                  <Paper
                    elevation={0}
                    data-testid="embedded-agent-streaming-message-bubble"
                    data-source-endpoint="/agent/chat-stream"
                    data-markdown-renderer="MarkdownViewer"
                    sx={agentStreamingBubbleSx({ bordered: true })}
                  >
                    <MarkdownViewer content={streamContent} compact />
                    <Chip label="生成中..." size="small" color="info" sx={{ mt: 0.5, fontSize: 10 }} />
                  </Paper>
                </Box>
              )}
              <div ref={messagesEndRef} />
            </Box>
            <Divider />
            {streamStatuses.length > 0 && (
              <Box sx={{ px: 1.5, pt: 1.25, pb: 0.25, bgcolor: 'background.paper', borderTop: '1px solid', borderColor: 'divider' }}>
                <Box
                  data-testid="embedded-agent-stream-status-bar"
                  data-streaming={isStreaming ? 'true' : 'false'}
                  data-surface-tone={isStreaming ? 'primary' : 'neutral'}
                  data-source-endpoint="/agent/chat-stream"
                  data-no-local-status-fallback="true"
                  sx={agentStreamStatusBarSx(isStreaming)}
                >
                  <Stack direction="row" alignItems="center" spacing={1} sx={{ minWidth: 0 }}>
                    <SmartToyIcon sx={{ fontSize: 16, color: 'primary.main', flexShrink: 0 }} />
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, flexShrink: 0 }}>执行状态</Typography>
                    {currentStreamStatus?.done ? <Chip size="small" label="完成" color="success" /> : <CircularProgress size={13} />}
                    <Typography variant="body2" sx={{ flex: 1, minWidth: 0, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {currentStreamStatus?.text ?? '准备中...'}
                    </Typography>
                    <Stack direction="row" spacing={0.5} sx={{ flexShrink: 1, minWidth: 0, overflow: 'hidden' }}>
                      {streamStatuses.slice(-3).map((item) => (
                        <Chip
                          key={item.id}
                          size="small"
                          label={item.text}
                          color={item.done ? 'default' : 'info'}
                          variant={item.done ? 'outlined' : 'filled'}
                          sx={{ maxWidth: 150, '& .MuiChip-label': { overflow: 'hidden', textOverflow: 'ellipsis' } }}
                        />
                      ))}
                    </Stack>
                  </Stack>
                </Box>
              </Box>
            )}
            <Stack direction="row" spacing={1} sx={{ p: 1.5 }}>
              <TextField fullWidth size="small" placeholder="输入消息，Shift+Enter 换行，Enter 发送" value={input} onChange={e => setInput(e.target.value)} onKeyDown={handleKeyDown} multiline maxRows={4} disabled={isStreaming} />
              {isStreaming
                ? <Button variant="outlined" color="error" onClick={handleStop} sx={{ minWidth: 80 }} startIcon={<StopIcon />}>停止</Button>
                : <Button variant="contained" onClick={() => void handleSend()} disabled={!input.trim()} sx={{ minWidth: 80 }} startIcon={<SendIcon />}>发送</Button>
              }
            </Stack>
          </>
        )}
      </Box>

      {/* 右栏：对话信息面板 280px */}
      <Box sx={{ width: 280, borderLeft: '1px solid', borderColor: 'divider', p: 2, display: 'flex', flexDirection: 'column', gap: 2, overflow: 'auto' }}>
        {/* 智能体信息 */}
        <Box>
          <Stack direction="row" spacing={0.5} alignItems="center" mb={0.5}>
            <Typography sx={{ fontSize: 20 }}>{agentInfo ? getTypeConfig(agentInfo.agentType).emoji : '🤖'}</Typography>
            <Typography variant="subtitle2" fontWeight={700}>{agentInfo?.agentName ?? '—'}</Typography>
          </Stack>
          <Stack direction="row" spacing={0.5} flexWrap="wrap">
            {agentInfo && <Chip label={getTypeConfig(agentInfo.agentType).label} size="small" color={getTypeConfig(agentInfo.agentType).color} />}
            {agentInfo?.modelConfig && <Chip label={agentInfo.modelConfig} size="small" variant="outlined" />}
          </Stack>
        </Box>
        <Divider />
        {/* Token 用量 */}
        <Box>
          <Typography variant="caption" color="text.secondary" fontWeight={600}>Token 用量（本次对话）</Typography>
          <Box sx={{ mt: 0.5 }}>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="caption">输入</Typography>
              <Typography variant="caption" fontWeight={600}>{tokenUsage.input.toLocaleString()}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="caption">输出</Typography>
              <Typography variant="caption" fontWeight={600}>{tokenUsage.output.toLocaleString()}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="caption">合计</Typography>
              <Typography variant="caption" fontWeight={600}>{totalTokens.toLocaleString()}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="caption">费用估算</Typography>
              <Typography variant="caption" fontWeight={600} color="warning.main">≈ ¥{costEstimate}</Typography>
            </Stack>
          </Box>
        </Box>
        <Divider />
        {/* 工具调用 */}
        <Box>
          <Typography variant="caption" color="text.secondary" fontWeight={600}>工具调用 {toolCallsUsed.length} 次</Typography>
          <Stack spacing={0.5} mt={0.5}>
            {toolCallsUsed.length === 0 ? (
              <Typography variant="caption" color="text.secondary">暂无</Typography>
            ) : toolCallsUsed.map(name => (
              <Chip key={name} label={getToolLabel(name)} size="small" variant="outlined" sx={{ justifyContent: 'flex-start', fontSize: 11 }} />
            ))}
          </Stack>
        </Box>
        <Divider />
        {/* 对话质量评分 */}
        <Box>
          <Typography variant="caption" color="text.secondary" fontWeight={600}>对话质量</Typography>
          {qualityScore !== null ? (
            <Stack direction="row" spacing={1} alignItems="center" mt={0.5}>
              <Typography variant="h6" fontWeight={700} color="warning.main">{qualityScore.toFixed(1)}</Typography>
              <Typography variant="caption" color="text.secondary">基于 {ratedMsgs.length} 次反馈</Typography>
            </Stack>
          ) : (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>暂无评分</Typography>
          )}
        </Box>
        <Divider />
        {/* 操作按钮 */}
        <Stack spacing={1}>
          <Button fullWidth size="small" variant="outlined" startIcon={<DownloadIcon />} onClick={handleExport} disabled={!activeConvId}>导出对话 .md</Button>
          <Button
            fullWidth
            size="small"
            variant="outlined"
            color="error"
            onClick={() => activeConvId && clearConversationMut.mutate(activeConvId)}
            disabled={!activeConvId || messages.length === 0 || clearConversationMut.isPending}
          >
            {clearConversationMut.isPending ? '清空中...' : '清空上下文'}
          </Button>
        </Stack>
      </Box>
    </Box>
  )
}
// ─── AgentManageTab ──────────────────────────────────────────────────────────
function AgentManageTab({ onChat }: { onChat: (id: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, agentName: '', agentType: '' })
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<AgentSave>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [operationError, setOperationError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['agents', search],
    queryFn: () => agentApi.list({ ...search, agentType: search.agentType !== '' ? Number(search.agentType) : undefined }),
  })

  const { data: kbData } = useQuery({ queryKey: ['kb-list-agent'], queryFn: () => aiApi.kbList({}) })
  const kbList = normalizeArray<{ id: number; kbName: string }>(kbData)
  const agentRows = normalizeArray<Agent>(data)
  const agentPage = { list: agentRows, total: readTotal(data, agentRows.length) }

  const saveMut = useMutation({
    mutationFn: (f: Partial<AgentSave>) => agentApi.save(f),
    onSuccess: () => {
      toast('保存成功', 'success')
      setOperationError('')
      setDrawerOpen(false)
      qc.invalidateQueries({ queryKey: ['agents'] })
    },
    onError: (e: Error) => {
      setOperationError(`保存智能体失败（POST /agent/save）：${getErrorMessage(e)}。抽屉输入已保留，页面不会补本地智能体。`)
      toast(e.message, 'error')
    },
  })
  const delMut = useMutation({
    mutationFn: agentApi.delete,
    onSuccess: () => {
      toast('删除成功', 'success')
      setOperationError('')
      setDeleteId(null)
      qc.invalidateQueries({ queryKey: ['agents'] })
    },
    onError: (e: Error) => {
      setOperationError(`删除智能体失败（POST /agent/delete）：${getErrorMessage(e)}。页面已保留当前智能体行。`)
      toast(e.message, 'error')
    },
  })

  const columns: GridColDef[] = [
    {
      field: 'agentName', headerName: '名称', flex: 2,
      renderCell: ({ row }) => {
        const cfg = getTypeConfig(row.agentType)
        return (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, cursor: 'pointer' }} onClick={() => onChat(row.id)}>
            <Typography sx={{ fontSize: 16 }}>{cfg.emoji}</Typography>
            <Typography variant="body2">{row.agentName}</Typography>
          </Box>
        )
      },
    },
    {
      field: 'agentType', headerName: '类型', width: 120,
      renderCell: ({ value }) => {
        const cfg = getTypeConfig(Number(value))
        return <Chip label={cfg.label} size="small" color={cfg.color} />
      },
    },
    {
      field: 'modelConfig', headerName: '绑定KB', width: 140,
      renderCell: ({ value }) => {
        try {
          const cfg = JSON.parse(String(value ?? '{}'))
          const kb = kbList.find(k => k.id === cfg.kbId)
          return <Typography variant="body2">{kb?.kbName ?? '-'}</Typography>
        } catch { return '-' }
      },
    },
    { field: 'responseMode', headerName: '模型', width: 110, renderCell: ({ value }) => <Typography variant="body2">{value === 1 ? 'DeepSeek' : value === 2 ? 'Qwen' : value === 3 ? 'GPT-4o' : '默认'}</Typography> },
    {
      field: 'status', headerName: '状态', width: 80,
      renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} size="small" color={value === 1 ? 'success' : 'default'} />,
    },
    { field: 'createTime', headerName: '创建时间', width: 150, renderCell: ({ value }) => formatDate(String(value ?? '')) },
    {
      field: 'actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" variant="outlined" startIcon={<ChatIcon />} onClick={() => onChat(row.id as number)}>对话</Button>
          <IconButton
            size="small"
            aria-label={`编辑智能体 ${row.agentName}`}
            onClick={() => { setOperationError(''); setForm(row as AgentSave); setDrawerOpen(true) }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            color="error"
            aria-label={`删除智能体 ${row.agentName}`}
            onClick={() => setDeleteId(row.id as number)}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="agent-list-manage-tab"
      data-ready-endpoints="/agent/list|/agent/save|/agent/delete|/ai/knowledge-base/list"
      data-unsupported-endpoints="/agent/mock|/agent/local-list|/agent/local-save|/agent/local-delete|/agent/static-agent|/agent/local-kb"
      data-no-local-agent-fallback="true"
      data-server-pagination="true"
      sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}
    >
      <Stack direction="row" spacing={1} sx={{ mb: 1.5 }} alignItems="center">
        <TextField size="small" placeholder="搜索智能体名称..." value={search.agentName} onChange={e => setSearch(s => ({ ...s, agentName: e.target.value, page: 0 }))} sx={{ width: 220 }} />
        <FormControl size="small" sx={{ width: 140 }}>
          <InputLabel>类型</InputLabel>
          <Select label="类型" value={search.agentType} onChange={e => setSearch(s => ({ ...s, agentType: String(e.target.value), page: 0 }))}>
            <MenuItem value="">全部</MenuItem>
            {AGENT_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
          </Select>
        </FormControl>
        <Box sx={{ flex: 1 }} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setOperationError(''); setForm({}); setDrawerOpen(true) }}>新建智能体</Button>
      </Stack>
      {isError && (
        <Alert
          severity="error"
          data-testid="agent-list-load-error"
          data-no-local-agent-fallback="true"
          sx={{ mb: 1.5 }}
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          智能体列表加载失败（POST /agent/list）：{getErrorMessage(error)}。页面不会补本地智能体。
        </Alert>
      )}
      {operationError && (
        <Alert
          severity="error"
          data-testid="agent-list-operation-error"
          data-input-retained="true"
          data-no-local-agent-mutation="true"
          sx={{ mb: 1.5 }}
          onClose={() => setOperationError('')}
        >
          {operationError}
        </Alert>
      )}
      <Box data-testid="agent-list-grid" data-server-pagination="true" sx={{ flex: 1, minHeight: 0 }}>
        <StandardDataGrid
          rows={agentPage.list} columns={columns} rowCount={agentPage.total}
          loading={isFetching} paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          sx={{ flex: 1 }}
        />
      </Box>
      <AgentEditDrawer
        open={drawerOpen}
        form={form}
        kbList={kbList}
        onClose={() => setDrawerOpen(false)}
        onSave={f => saveMut.mutate(f)}
        saving={saveMut.isPending}
        errorText={drawerOpen && operationError.startsWith('保存智能体失败') ? operationError : undefined}
      />
      <ConfirmDialog open={deleteId !== null} content="确定要删除该智能体吗？" onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}
// ─── Main Page ───────────────────────────────────────────────────────────────
export default function AgentListPage() {
  const [tab, setTab] = useState(0)
  const [chatAgentId, setChatAgentId] = useState<number | null>(null)

  const handleChatFromManage = (id: number) => {
    setChatAgentId(id)
    setTab(1)
  }

  return (
    <Box
      data-testid="agent-list-page"
      data-ready-endpoints="/agent/list|/agent/get|/agent/save|/agent/delete|/agent/conversation/list|/agent/conversation/create|/agent/conversation/delete|/agent/conversation/rename|/agent/message/list|/agent/message/rate|/agent/conversation/export|/agent/chat-stream|/ai/knowledge-base/list"
      data-unsupported-endpoints="/agent/mock|/agent/local-list|/agent/local-save|/agent/local-delete|/agent/local-conversation|/agent/local-message|/agent/static-agent|/agent/static-message|/agent/message/send-fallback|/agent/status/local|/agent/markdown/static-render|/agent/local-kb"
      data-no-local-agent-fallback="true"
      data-no-local-conversation-fallback="true"
      data-no-local-message-fallback="true"
      data-markdown-renderer="MarkdownViewer"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <Typography variant="h5" sx={{ mb: 2 }}>智能体管理</Typography>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 0 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label="智能体管理" />
          <Tab label="对话中心" />
        </Tabs>
      </Box>
      <Box sx={{ flex: 1, overflow: 'hidden', pt: 2 }}>
        {tab === 0 && (
          <AgentManageTab onChat={handleChatFromManage} />
        )}
        {tab === 1 && (
          <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            {/* Agent selector */}
            <AgentSelectorBar selectedId={chatAgentId} onSelect={setChatAgentId} />
            <Box sx={{ flex: 1, overflow: 'hidden' }}>
              <ChatPanel agentId={chatAgentId} />
            </Box>
          </Box>
        )}
      </Box>
    </Box>
  )
}

// ─── AgentSelectorBar ─────────────────────────────────────────────────────────
function AgentSelectorBar({ selectedId, onSelect }: { selectedId: number | null; onSelect: (id: number) => void }) {
  const { data, isError, error, refetch } = useQuery({
    queryKey: ['agents-selector'],
    queryFn: () => agentApi.list({ rows: 50 }),
  })
  const agents = normalizeArray<Agent>(data)

  return (
    <Box
      data-testid="agent-selector-bar"
      data-ready-endpoints="/agent/list"
      data-no-local-agent-fallback="true"
      sx={{ px: 2, pb: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}
    >
      {isError && (
        <Alert
          severity="error"
          data-testid="agent-selector-list-error"
          data-no-local-agent-fallback="true"
          sx={{ mb: 1 }}
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          智能体选择器加载失败（POST /agent/list）：{getErrorMessage(error)}。请稍后重试，页面不会补本地智能体。
        </Alert>
      )}
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        <Typography variant="body2" sx={{ alignSelf: 'center', mr: 1, color: 'text.secondary' }}>选择智能体：</Typography>
        {agents.map(a => {
          const cfg = getTypeConfig(a.agentType)
          return (
            <Chip
              key={a.id}
              label={`${cfg.emoji} ${a.agentName}`}
              size="small"
              color={selectedId === a.id ? cfg.color : 'default'}
              variant={selectedId === a.id ? 'filled' : 'outlined'}
              onClick={() => onSelect(a.id)}
              clickable
            />
          )
        })}
      </Box>
    </Box>
  )
}
