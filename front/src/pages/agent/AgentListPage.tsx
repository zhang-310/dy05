import { useState, useCallback, useRef, useEffect } from 'react'
import {
  Box, Stack, Typography, TextField, Button, Chip, Paper, Drawer,
  FormControl, InputLabel, Select, MenuItem, IconButton, Tooltip,
  Divider, Tabs, Tab, FormGroup, FormControlLabel, Checkbox,
  CircularProgress, List, ListItemButton, ListItemText, Collapse,
  Menu,
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
import { agentApi, type AgentSave, type ChatMessage as ApiChatMessage, type ToolCall as ApiToolCall } from '@/api/agent'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

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
  return (
    <Paper variant="outlined" sx={{ mt: 0.5, borderRadius: 1, overflow: 'hidden' }}>
      <Stack direction="row" alignItems="center" sx={{ px: 1.5, py: 0.75, cursor: 'pointer', bgcolor: 'action.hover' }} onClick={() => setOpen(v => !v)}>
        <SmartToyIcon sx={{ fontSize: 14, mr: 0.75, color: 'text.secondary' }} />
        <Typography variant="caption" sx={{ flex: 1, fontFamily: 'monospace' }}>{toolCall.name}</Typography>
        {open ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
      </Stack>
      <Collapse in={open}>
        <Box sx={{ px: 1.5, py: 1, borderTop: '1px solid', borderColor: 'divider' }}>
          <Typography variant="caption" color="text.secondary">参数：</Typography>
          <Box component="pre" sx={{ fontSize: 11, mt: 0.5, overflowX: 'auto', m: 0, bgcolor: 'grey.50', p: 1, borderRadius: 1 }}>
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

// ─── AgentEditDrawer ──────────────────────────────────────────────────────────
function AgentEditDrawer({
  open, form, kbList, onClose, onSave, saving,
}: {
  open: boolean
  form: Partial<AgentSave>
  kbList: { id: number; kbName: string }[]
  onClose: () => void
  onSave: (f: Partial<AgentSave>) => void
  saving: boolean
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
                <Box sx={{ mt: 1, p: 1.5, bgcolor: 'grey.50', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
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
  const [ctxMenuAnchor, setCtxMenuAnchor] = useState<HTMLElement | null>(null)
  const [ctxConvId, setCtxConvId] = useState<number | null>(null)
  const [renameId, setRenameId] = useState<number | null>(null)
  const [renameVal, setRenameVal] = useState('')
  const abortRef = useRef<AbortController | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

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

  const { data: convList } = useQuery({
    queryKey: ['agent-conversations', agentId],
    queryFn: () => agentApi.conversationList(agentId!),
    enabled: agentId !== null && agentId > 0,
  })
  const conversations = convList ?? []

  const { data: msgList } = useQuery({
    queryKey: ['agent-messages', activeConvId],
    queryFn: () => agentApi.messageList(activeConvId!),
    enabled: activeConvId !== null,
  })

  useEffect(() => {
    if (msgList) setMessages(msgList)
  }, [msgList])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamContent])

  useEffect(() => {
    setActiveConvId(null)
    setMessages([])
  }, [agentId])

  const createConvMut = useMutation({
    mutationFn: () => agentApi.conversationCreate(agentId!, `对话 ${new Date().toLocaleTimeString()}`),
    onSuccess: (id) => {
      setActiveConvId(Number(id))
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
    },
    onError: () => toast('创建对话失败', 'error'),
  })

  const deleteConvMut = useMutation({
    mutationFn: (id: number) => agentApi.conversationDelete(id),
    onSuccess: (_data, id) => {
      if (activeConvId === id) { setActiveConvId(null); setMessages([]) }
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
    },
  })

  const rateMut = useMutation({
    mutationFn: ({ messageId, rating }: { messageId: number; rating: 'up' | 'down' }) =>
      agentApi.messageRate(messageId, rating),
    onSuccess: () => toast('已提交评分', 'success'),
  })

  const handleSend = useCallback(async () => {
    if (!input.trim() || isStreaming || !activeConvId) return
    const userMsg: ChatMessage = { id: Date.now(), role: 'user', content: input.trim(), createdAt: new Date().toISOString() }
    setMessages(m => [...m, userMsg])
    setInput('')
    setIsStreaming(true)
    setStreamContent('')
    const ctrl = new AbortController()
    abortRef.current = ctrl
    let accum = ''
    let inputTok = 0, outputTok = 0
    try {
      const res = await fetch(agentApi.chatStreamUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
        body: JSON.stringify({ agentId, conversationId: activeConvId, content: userMsg.content }),
        signal: ctrl.signal,
      })
      const reader = res.body!.getReader()
      const dec = new TextDecoder()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = dec.decode(value)
        for (const line of chunk.split('\n')) {
          if (!line.startsWith('data:')) continue
          const raw = line.slice(5).trim()
          if (!raw || raw === '[DONE]') continue
          try {
            const ev = JSON.parse(raw)
            if (ev.type === 'message') { accum += ev.content; setStreamContent(accum) }
            else if (ev.type === 'token_usage') { inputTok = ev.input ?? 0; outputTok = ev.output ?? 0 }
          } catch { /* ignore */ }
        }
      }
    } catch (e: unknown) {
      if ((e as Error).name !== 'AbortError') toast('发送失败', 'error')
    } finally {
      const assistantMsg: ChatMessage = { id: Date.now() + 1, role: 'assistant', content: accum, createdAt: new Date().toISOString(), tokenUsage: { input: inputTok, output: outputTok } }
      setMessages(m => [...m, assistantMsg])
      setStreamContent('')
      setIsStreaming(false)
      setTokenUsage(u => ({ input: u.input + inputTok, output: u.output + outputTok }))
      qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })
    }
  }, [input, isStreaming, activeConvId, agentId, toast, qc])

  const handleStop = () => { abortRef.current?.abort(); setIsStreaming(false) }

  const handleExport = useCallback(async () => {
    if (!activeConvId) return
    try {
      const res = await agentApi.exportConversation(activeConvId)
      window.open(res.downloadUrl, '_blank')
    } catch { toast('导出失败', 'error') }
  }, [activeConvId, toast])

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void handleSend() }
  }
  if (!agentId) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'text.secondary' }}>
        <Typography>请从左侧选择一个智能体开始对话</Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ display: 'flex', height: '100%' }}>
      {/* 对话列表 */}
      <Box sx={{ width: 220, borderRight: '1px solid', borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Button fullWidth size="small" variant="outlined" startIcon={<AddCommentIcon />} onClick={() => createConvMut.mutate()} disabled={createConvMut.isPending}>新建对话</Button>
        </Box>
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
                      onBlur={() => { agentApi.conversationRename(c.id, renameVal).catch(() => toast('重命名失败', 'error')); qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] }); setRenameId(null) }}
                      onKeyDown={e => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur() }}
                      onClick={e => e.stopPropagation()} sx={{ width: '100%' }} />
                  : <Typography variant="body2" noWrap>{c.title}</Typography>
                }
                secondary={<Typography variant="caption" color="text.secondary">{formatDate(c.createTime)}</Typography>}
              />
              <IconButton size="small" onClick={e => { e.stopPropagation(); setCtxMenuAnchor(e.currentTarget); setCtxConvId(c.id); setRenameVal(c.title) }}>
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
              {messages.map(msg => (
                <Box key={msg.id} sx={{ mb: 2, display: 'flex', flexDirection: msg.role === 'user' ? 'row-reverse' : 'row', gap: 1 }}>
                  <Paper elevation={0} sx={{ p: 1.5, maxWidth: '75%', borderRadius: 2, bgcolor: msg.role === 'user' ? 'primary.50' : 'grey.100', border: '1px solid', borderColor: msg.role === 'user' ? 'primary.200' : 'grey.200' }}>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{msg.content}</Typography>
                    {msg.toolCalls?.map((tc, i) => <ToolCallCard key={i} tc={tc} />)}
                    {msg.role === 'assistant' && (
                      <Stack direction="row" spacing={0.5} sx={{ mt: 0.75 }} alignItems="center">
                        <IconButton size="small" color={msg.rating === 'up' ? 'primary' : 'default'} onClick={() => rateMut.mutate({ messageId: msg.id, rating: 'up' })}><ThumbUpOutlinedIcon sx={{ fontSize: 14 }} /></IconButton>
                        <IconButton size="small" color={msg.rating === 'down' ? 'error' : 'default'} onClick={() => rateMut.mutate({ messageId: msg.id, rating: 'down' })}><ThumbDownOutlinedIcon sx={{ fontSize: 14 }} /></IconButton>
                        {msg.tokenUsage && <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>↑{msg.tokenUsage.input} ↓{msg.tokenUsage.output}</Typography>}
                      </Stack>
                    )}
                  </Paper>
                </Box>
              ))}
              {isStreaming && streamContent && (
                <Box sx={{ mb: 2, display: 'flex', gap: 1 }}>
                  <Paper elevation={0} sx={{ p: 1.5, maxWidth: '75%', borderRadius: 2, bgcolor: 'grey.100', border: '1px solid', borderColor: 'grey.200' }}>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{streamContent}</Typography>
                    <Chip label="生成中…" size="small" color="info" sx={{ mt: 0.5, fontSize: 10 }} />
                  </Paper>
                </Box>
              )}
              <div ref={messagesEndRef} />
            </Box>
            <Divider />
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
              <Chip key={name} label={name} size="small" variant="outlined" sx={{ justifyContent: 'flex-start', fontFamily: 'monospace', fontSize: 11 }} />
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
          <Button fullWidth size="small" variant="outlined" color="error" onClick={() => { setMessages([]); setTokenUsage({ input: 0, output: 0 }) }} disabled={messages.length === 0}>清空上下文</Button>
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

  const { data, isFetching } = useQuery({
    queryKey: ['agents', search],
    queryFn: () => agentApi.list({ ...search, agentType: search.agentType !== '' ? Number(search.agentType) : undefined }),
  })

  const { data: kbData } = useQuery({ queryKey: ['kb-list-agent'], queryFn: () => aiApi.kbList({}) })
  const kbList = (kbData ?? []) as { id: number; kbName: string }[]

  const saveMut = useMutation({
    mutationFn: (f: Partial<AgentSave>) => agentApi.save(f),
    onSuccess: () => { toast('保存成功', 'success'); setDrawerOpen(false); qc.invalidateQueries({ queryKey: ['agents'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: agentApi.delete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['agents'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
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
          <IconButton size="small" onClick={() => { setForm(row as AgentSave); setDrawerOpen(true) }}><EditIcon fontSize="small" /></IconButton>
          <IconButton size="small" color="error" onClick={() => setDeleteId(row.id as number)}><DeleteIcon fontSize="small" /></IconButton>
        </Stack>
      ),
    },
  ]

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
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
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setForm({}); setDrawerOpen(true) }}>新建智能体</Button>
      </Stack>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0}
        loading={isFetching} paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        sx={{ flex: 1 }}
      />
      <AgentEditDrawer open={drawerOpen} form={form} kbList={kbList} onClose={() => setDrawerOpen(false)} onSave={f => saveMut.mutate(f)} saving={saveMut.isPending} />
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
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
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
  const { data } = useQuery({
    queryKey: ['agents-selector'],
    queryFn: () => agentApi.list({ rows: 50 }),
  })
  const agents = data?.list ?? []

  return (
    <Box sx={{ px: 2, pb: 1.5, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', gap: 1, flexWrap: 'wrap' }}>
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
  )
}