import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import {
  Box, Stack, Typography, TextField, Button, Paper, Chip, CircularProgress,
  Divider, List, ListItemButton, ListItemText, Card, CardContent, IconButton,
  Accordion, AccordionSummary, AccordionDetails, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions,
  InputAdornment, Alert,
} from '@mui/material'
import SendIcon from '@mui/icons-material/Send'
import AddIcon from '@mui/icons-material/Add'
import DownloadIcon from '@mui/icons-material/Download'
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep'
import ThumbUpIcon from '@mui/icons-material/ThumbUp'
import ThumbDownIcon from '@mui/icons-material/ThumbDown'
import BuildIcon from '@mui/icons-material/Build'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import PsychologyIcon from '@mui/icons-material/Psychology'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import ShareIcon from '@mui/icons-material/Share'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { agentApi, shareApi, type Agent, type ChatMessage } from '@/api/agent'
import { useToast } from '@/contexts/ToastContext'
import { ssePost } from '@/utils/sse-client'

const AGENT_TYPE_LABELS: Record<number, string> = {
  0: '自定义', 1: '话术生成', 2: '违规检测', 3: '商品分析',
  4: '场次规划', 5: '数据分析', 6: '客户服务',
}

// ============================================================
// 统一技能（Skill）元数据配置
// ============================================================
export interface SkillMeta {
  name: string          // 技术名称，如 kb_rag_search
  label: string         // 中文展示名，如 知识库检索
  description: string   // 一句话功能描述
  icon: React.ReactElement // Chip icon 必须为 ReactElement
  keywords: string[]    // 触发关键词（用于 matches()）
  example: string       // 使用示例
}

const KB_SKILL_ICON = <AutoAwesomeIcon sx={{ fontSize: 14 }} color="primary" />
const PRODUCT_SKILL_ICON = <Typography sx={{ fontSize: 12, lineHeight: 1 }}>📦</Typography>
const COMPLIANCE_SKILL_ICON = <ErrorIcon sx={{ fontSize: 14 }} color="error" />
const LIVE_SKILL_ICON = <Typography sx={{ fontSize: 12, lineHeight: 1 }}>📅</Typography>
const SCRIPT_SKILL_ICON = <PsychologyIcon sx={{ fontSize: 14 }} color="secondary" />

export const SKILL_CONFIG: SkillMeta[] = [
  {
    name: 'kb_rag_search',
    label: '知识库检索',
    description: '从企业知识库中检索相关文档与资料',
    icon: KB_SKILL_ICON,
    keywords: ['搜索', '查找', '知识库', '资料', '文档', '检索'],
    example: '搜索玻尿酸护肤品的功效说明',
  },
  {
    name: 'product_search',
    label: '商品搜索',
    description: '查询商品信息、价格、库存及销售数据',
    icon: PRODUCT_SKILL_ICON,
    keywords: ['商品', '产品', '价格', '库存', '搜索商品'],
    example: '查找玻尿酸精华液的价格',
  },
  {
    name: 'compliance_check',
    label: '违规检测',
    description: '检测话术中的敏感词、极限词及合规风险',
    icon: COMPLIANCE_SKILL_ICON,
    keywords: ['检测', '违规', '敏感词', '合规', '过滤', '风险'],
    example: '检测这段话术是否违规',
  },
  {
    name: 'live_session_query',
    label: '场次查询',
    description: '查询直播场次数据、排期及运营指标',
    icon: LIVE_SKILL_ICON,
    keywords: ['场次', '直播', '排期', 'GMV', '观看', '数据查询'],
    example: '查询最近一周的直播场次数据',
  },
  {
    name: 'script_generate',
    label: '话术生成',
    description: '根据模板和上下文生成直播话术脚本',
    icon: SCRIPT_SKILL_ICON,
    keywords: ['生成', '话术', '脚本', '开场白', '促销', '文案'],
    example: '生成一个护肤品直播开场白',
  },
]

export const SKILL_MAP: Record<string, SkillMeta> = Object.fromEntries(SKILL_CONFIG.map(s => [s.name, s]))

export default function AgentChatPage() {
  const { id } = useParams<{ id: string }>()
  const agentId = Number(id)
  const toast = useToast()
  const qc = useQueryClient()
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const abortControllerRef = useRef<AbortController | null>(null)
  const [activeConvId, setActiveConvId] = useState<number | null>(null)
  const [input, setInput] = useState('')
  const [streamContent, setStreamContent] = useState('')
  const [isStreaming, setIsStreaming] = useState(false)
  const [tokenStats, setTokenStats] = useState({ input: 0, output: 0, total: 0 })
  const [toolStatus, setToolStatus] = useState<{
    tool: string; status: 'calling' | 'done' | 'error'; description?: string; result?: string
  }[]>([])
  const [shareOpen, setShareOpen] = useState(false)
  const [shareLink, setShareLink] = useState('')
  const [shareCopying, setShareCopying] = useState(false)

  const { data: agentData } = useQuery({
    queryKey: ['agent-detail', agentId],
    queryFn: () => agentApi.get(agentId),
    enabled: agentId > 0,
  })
  const agent: Agent | undefined = agentData

  const { data: convList, isLoading: convLoading } = useQuery({
    queryKey: ['agent-conversations', agentId],
    queryFn: () => agentApi.conversationList(agentId),
    enabled: agentId > 0,
  })
  const conversations = convList ?? []

  const { data: msgList, isLoading: msgLoading } = useQuery({
    queryKey: ['agent-messages', activeConvId],
    queryFn: () => agentApi.messageList(activeConvId!),
    enabled: activeConvId !== null,
  })
  const messages: ChatMessage[] = msgList ?? []

  useEffect(() => {
    if (messages.length > 0) {
      const inputTokens = messages
        .filter(m => m.role === 'user')
        .reduce((sum, m) => sum + (m.tokenUsage?.input ?? 0), 0)
      const outputTokens = messages
        .filter(m => m.role === 'assistant')
        .reduce((sum, m) => sum + (m.tokenUsage?.output ?? 0), 0)
      setTokenStats({ input: inputTokens, output: outputTokens, total: inputTokens + outputTokens })
    }
  }, [messages])

  const createConvMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      const newId = await agentApi.conversationCreate(agentId, `对话 ${new Date().toLocaleTimeString()}`)
      setActiveConvId(Number(newId))
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
    },
    onError: () => toast('创建对话失败', 'error'),
  })

  const deleteConvMutation = useMutation({
    mutationFn: async (cid: number): Promise<void> => { await agentApi.conversationDelete(cid) },
    onSuccess: (_d, cid) => {
      if (activeConvId === cid) setActiveConvId(null)
      qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] })
    },
  })

  const rateMessageMutation = useMutation({
    mutationFn: async ({ messageId, rating }: { messageId: number; rating: 'up' | 'down' }) => {
      await agentApi.messageRate(messageId, rating)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] }) },
    onError: () => toast('评价失败', 'error'),
  })

  const exportMutation = useMutation({
    mutationFn: async () => {
      const result = await agentApi.exportConversation(activeConvId!)
      window.open(result.downloadUrl, '_blank')
    },
    onError: () => toast('导出失败', 'error'),
  })

  const clearMutation = useMutation({
    mutationFn: async () => {
      await agentApi.conversationDelete(activeConvId!)
      setActiveConvId(null)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['agent-conversations', agentId] }) },
    onError: () => toast('清空失败', 'error'),
  })

  const createShareMutation = useMutation({
    mutationFn: async () => {
      const result = await shareApi.create({
        conversationId: activeConvId!,
        title: conversations.find(c => c.id === activeConvId)?.title,
      })
      return result
    },
    onSuccess: (share) => {
      const link = `${window.location.origin}/agent/share/${share.shareCode}`
      setShareLink(link)
      setShareOpen(true)
    },
    onError: () => toast('创建分享失败', 'error'),
  })

  const copyShareLinkMutation = useMutation({
    mutationFn: async (link: string) => {
      await navigator.clipboard.writeText(link)
    },
    onSuccess: () => {
      toast('链接已复制到剪贴板', 'success')
      setShareCopying(false)
    },
    onError: () => {
      toast('复制失败', 'error')
      setShareCopying(false)
    },
  })

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamContent])

  const handleSend = async () => {
    if (!input.trim() || !activeConvId || isStreaming) return
    const content = input.trim()
    setInput('')
    setIsStreaming(true)
    setStreamContent('')

    try {
      await agentApi.messageSend(activeConvId, content)
      qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })

      // 使用 ssePost 进行 POST 流式请求
      setToolStatus([])
      abortControllerRef.current = ssePost(
        '/agent/chat-stream',
        { conversationId: activeConvId, content },
        {
          onSkillStart: ({ tool, description }) => {
            setToolStatus(prev => [...prev.filter(t => t.tool !== tool), { tool, status: 'calling', description }])
          },
          onSkillEnd: ({ tool, status, error }) => {
            setToolStatus(prev => prev.map(t => t.tool === tool ? {
              ...t, status: status as 'done' | 'error',
              description: error ?? t.description,
              result: status === 'done' ? '执行成功' : `错误: ${error ?? ''}`,
            } : t))
          },
          onChunk: (chunkData) => {
            try {
              const parsed = typeof chunkData === 'string' ? JSON.parse(chunkData) : chunkData
              if (parsed.type === 'chunk' && parsed.content) {
                setStreamContent(prev => prev + parsed.content)
              }
            } catch {
              // 非 JSON 数据直接追加
              setStreamContent(prev => prev + String(chunkData))
            }
          },
          onDone: () => {
            setIsStreaming(false)
            setStreamContent('')
            setToolStatus([])
            qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })
            abortControllerRef.current = null
          },
          onError: (err) => {
            console.error('SSE Error:', err)
            setIsStreaming(false)
            setStreamContent('')
            setToolStatus([])
            toast('请求失败: ' + err.message, 'error')
            qc.invalidateQueries({ queryKey: ['agent-messages', activeConvId] })
            abortControllerRef.current = null
          },
        }
      )
    } catch {
      setIsStreaming(false)
      toast('发送失败', 'error')
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
  }

  const getAgentTools = (): string[] => {
    if (!agent?.availableTools) return []
    try { return JSON.parse(agent.availableTools) as string[] }
    catch { return [] }
  }
  const agentTools = getAgentTools()

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 120px)', gap: 1 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>智能体对话</Typography>
      <Box sx={{ display: 'flex', flex: 1, gap: 0, border: '1px solid', borderColor: 'divider', borderRadius: 1, overflow: 'hidden' }}>

        {/* 左栏：对话列表 240px */}
        <Box sx={{ width: 240, borderRight: '1px solid', borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>
          <Box sx={{ p: 1.5, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Button
              fullWidth variant="outlined" size="small" startIcon={<AddIcon />}
              onClick={() => createConvMutation.mutate()}
              disabled={createConvMutation.isPending || agentId <= 0}
            >
              新建对话
            </Button>
          </Box>
          {convLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <List dense sx={{ flex: 1, overflow: 'auto', p: 0 }}>
              {conversations.map((c) => (
                <ListItemButton
                  key={String(c.id)}
                  selected={activeConvId === Number(c.id)}
                  onClick={() => setActiveConvId(Number(c.id))}
                  sx={{ py: 0.75 }}
                >
                  <ListItemText
                    primary={String(c.title ?? c.id ?? '对话')}
                    primaryTypographyProps={{ variant: 'body2', noWrap: true }}
                    secondary={String(c.createTime ?? '').slice(0, 10)}
                    secondaryTypographyProps={{ variant: 'caption' }}
                  />
                  <Button
                    size="small" color="error" sx={{ minWidth: 32, p: 0.25, fontSize: 10 }}
                    onClick={e => { e.stopPropagation(); deleteConvMutation.mutate(Number(c.id)) }}
                  >
                    删
                  </Button>
                </ListItemButton>
              ))}
              {conversations.length === 0 && (
                <Typography variant="caption" color="text.secondary" sx={{ p: 2, display: 'block', textAlign: 'center' }}>
                  暂无对话
                </Typography>
              )}
            </List>
          )}
        </Box>

        {/* 中栏：消息流 flex */}
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          {activeConvId === null ? (
            <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary">请选择或新建一个对话</Typography>
            </Box>
          ) : (
            <>
              <Box sx={{ flex: 1, overflow: 'auto', p: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                {/* 技能调用状态面板（对话流中） */}
                {toolStatus.length > 0 && (
                  <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 2, mb: 0.5 }}>
                    <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
                      <PsychologyIcon sx={{ fontSize: 16, color: 'primary.main' }} />
                      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                        技能调用中
                      </Typography>
                    </Stack>
                    <Stack spacing={0.75}>
                      {toolStatus.map((t) => {
                        const skill = SKILL_MAP[t.tool]
                        return (
                          <Box key={t.tool} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            {/* 技能图标 */}
                            {skill?.icon ?? <BuildIcon sx={{ fontSize: 14 }} />}
                            {/* 技能名称 */}
                            <Typography variant="body2" sx={{ fontWeight: 500, minWidth: 80 }}>
                              {skill?.label ?? t.tool}
                            </Typography>
                            {/* 状态 */}
                            {t.status === 'calling' && (
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                <CircularProgress size={12} color="info" thickness={5} />
                                <Typography variant="caption" color="text.secondary">执行中...</Typography>
                              </Box>
                            )}
                            {t.status === 'done' && (
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                <CheckCircleIcon sx={{ fontSize: 14, color: 'success.main' }} />
                                <Typography variant="caption" color="success.main">完成</Typography>
                                {t.result && (
                                  <Tooltip title={t.result}>
                                    <Typography variant="caption" color="text.secondary" sx={{ maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', cursor: 'help' }}>
                                      → {t.result}
                                    </Typography>
                                  </Tooltip>
                                )}
                              </Box>
                            )}
                            {t.status === 'error' && (
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                <ErrorIcon sx={{ fontSize: 14, color: 'error.main' }} />
                                <Typography variant="caption" color="error.main">失败: {t.description ?? '未知错误'}</Typography>
                              </Box>
                            )}
                          </Box>
                        )
                      })}
                    </Stack>
                  </Box>
                )}
                {msgLoading && <CircularProgress size={24} sx={{ alignSelf: 'center' }} />}

                {messages.map((msg) => {
                  const isUser = msg.role === 'user'
                  const toolCalls = msg.toolCalls

                  return (
                    <Box key={msg.id} sx={{ display: 'flex', flexDirection: 'column', alignItems: isUser ? 'flex-end' : 'flex-start' }}>
                      <Paper
                        elevation={0}
                        sx={{
                          p: 1.5, maxWidth: '75%', borderRadius: 2,
                          bgcolor: isUser ? 'primary.main' : 'grey.100',
                          color: isUser ? 'white' : 'text.primary',
                        }}
                      >
                        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                          {msg.content}
                        </Typography>
                      </Paper>

                      {/* 已调用的技能展示（消息下方） */}
                      {toolCalls && toolCalls.length > 0 && (
                        <Box sx={{ mt: 0.5, display: 'flex', flexWrap: 'wrap', gap: 0.5, maxWidth: '75%' }}>
                          {toolCalls.map((tc, idx) => {
                            const skill = SKILL_MAP[tc.function?.name ?? '']
                            return (
                              <Tooltip
                                key={idx}
                                title={skill ? `${skill.label}: ${skill.description}` : tc.function?.name ?? '工具'}
                                arrow
                                placement="top"
                              >
                                <Chip
                                  icon={skill?.icon ?? <BuildIcon sx={{ fontSize: 12 }} />}
                                  label={skill?.label ?? tc.function?.name ?? 'tool'}
                                  size="small"
                                  color="info"
                                  variant="outlined"
                                  sx={{ fontSize: 10, height: 22 }}
                                />
                              </Tooltip>
                            )
                          })}
                        </Box>
                      )}

                      {/* Rating buttons */}
                      {!isUser && (
                        <Stack direction="row" spacing={0.5} sx={{ mt: 0.25 }}>
                          <IconButton
                            size="small"
                            onClick={() => rateMessageMutation.mutate({ messageId: msg.id, rating: 'up' })}
                            disabled={rateMessageMutation.isPending || msg.rating === 'up'}
                            sx={{ color: msg.rating === 'up' ? 'success.main' : 'text.secondary', p: 0.25 }}
                          >
                            <ThumbUpIcon sx={{ fontSize: 14 }} />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => rateMessageMutation.mutate({ messageId: msg.id, rating: 'down' })}
                            disabled={rateMessageMutation.isPending || msg.rating === 'down'}
                            sx={{ color: msg.rating === 'down' ? 'error.main' : 'text.secondary', p: 0.25 }}
                          >
                            <ThumbDownIcon sx={{ fontSize: 14 }} />
                          </IconButton>
                        </Stack>
                      )}
                    </Box>
                  )
                })}

                {isStreaming && streamContent && (
                  <Box sx={{ display: 'flex', justifyContent: 'flex-start' }}>
                    <Paper elevation={0} sx={{ p: 1.5, maxWidth: '75%', borderRadius: 2, bgcolor: 'grey.100' }}>
                      <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                        {streamContent}
                      </Typography>
                      <Chip label="生成中..." size="small" color="info" sx={{ mt: 0.5, fontSize: 10 }} />
                    </Paper>
                  </Box>
                )}
                <div ref={messagesEndRef} />
              </Box>

              <Divider />
              <Stack direction="row" spacing={1} sx={{ p: 1.5 }}>
                <TextField
                  fullWidth size="small"
                  placeholder="输入消息，Shift+Enter 换行，Enter 发送"
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  multiline maxRows={4}
                  disabled={isStreaming}
                />
                <Button
                  variant="contained"
                  onClick={handleSend}
                  disabled={!input.trim() || isStreaming}
                  sx={{ minWidth: 80 }}
                  startIcon={isStreaming ? <CircularProgress size={16} color="inherit" /> : <SendIcon />}
                >
                  发送
                </Button>
              </Stack>
            </>
          )}
        </Box>

        {/* 右栏：信息面板 280px */}
        {activeConvId !== null && (
          <Box sx={{ width: 280, borderLeft: '1px solid', borderColor: 'divider', display: 'flex', flexDirection: 'column', overflow: 'auto', p: 2, gap: 2 }}>
            <Card variant="outlined">
              <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="subtitle2" fontWeight={600} gutterBottom>智能体信息</Typography>
                <Stack spacing={0.5}>
                  <Typography variant="body2" color="text.secondary">名称: {agent?.agentName ?? '未知'}</Typography>
                  <Typography variant="body2" color="text.secondary">类型: {AGENT_TYPE_LABELS[agent?.agentType ?? 0] ?? '未知'}</Typography>
                  <Typography variant="body2" color="text.secondary">模式: {agent?.responseMode === 1 ? '流式' : '普通'}</Typography>
                </Stack>
              </CardContent>
            </Card>

            <Card variant="outlined">
              <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="subtitle2" fontWeight={600} gutterBottom>Token 用量（本次对话）</Typography>
                <Stack spacing={0.5}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">输入:</Typography>
                    <Typography variant="body2" fontWeight={600}>{tokenStats.input.toLocaleString()}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">输出:</Typography>
                    <Typography variant="body2" fontWeight={600}>{tokenStats.output.toLocaleString()}</Typography>
                  </Box>
                  <Divider sx={{ my: 0.5 }} />
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" fontWeight={600}>合计:</Typography>
                    <Typography variant="body2" fontWeight={700} color="primary.main">{tokenStats.total.toLocaleString()}</Typography>
                  </Box>
                  <Typography variant="caption" color="text.secondary">约 ¥{(tokenStats.total * 0.00002).toFixed(3)}</Typography>
                </Stack>
              </CardContent>
            </Card>

            {agentTools.length > 0 && (
              <Card variant="outlined">
                <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" alignItems="center" spacing={0.5} sx={{ mb: 1 }}>
                    <PsychologyIcon sx={{ fontSize: 16, color: 'primary.main' }} />
                    <Typography variant="subtitle2" fontWeight={600}>可用技能</Typography>
                  </Stack>
                  <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1, fontSize: 10 }}>
                    根据您的输入自动触发匹配技能
                  </Typography>
                  <Stack spacing={0.5}>
                    {agentTools.map((tool) => {
                      const skill = SKILL_MAP[tool]
                      if (!skill) return null
                      return (
                        <Accordion
                          key={tool}
                          disableGutters
                          elevation={0}
                          sx={{ border: '1px solid', borderColor: 'divider', borderRadius: '4px !important', '&:before': { display: 'none' }, '&.Mui-expanded': { m: 0 } }}
                        >
                          <AccordionSummary
                            expandIcon={<ExpandMoreIcon sx={{ fontSize: 14 }} />}
                            sx={{ minHeight: 32, '& .MuiAccordionSummary-content': { my: 0.5, gap: 0.5, alignItems: 'center' } }}
                          >
                            <Stack direction="row" spacing={0.5} alignItems="center" sx={{ flex: 1 }}>
                              {skill.icon}
                              <Typography variant="body2" fontWeight={500} sx={{ fontSize: 12 }}>{skill.label}</Typography>
                            </Stack>
                          </AccordionSummary>
                          <AccordionDetails sx={{ pt: 0, pb: 1 }}>
                            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5, fontSize: 10 }}>
                              {skill.description}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5, fontSize: 10 }}>
                              <b>触发词:</b> {skill.keywords.join('、')}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" display="block" sx={{ fontSize: 10 }}>
                              <b>示例:</b> {skill.example}
                            </Typography>
                          </AccordionDetails>
                        </Accordion>
                      )
                    })}
                  </Stack>
                </CardContent>
              </Card>
            )}

            <Stack spacing={1}>
              <Button
                size="small" variant="outlined" startIcon={<DownloadIcon />} fullWidth
                onClick={() => exportMutation.mutate()} disabled={exportMutation.isPending}
              >
                导出对话.md
              </Button>
              <Button
                size="small" variant="outlined" color="info" startIcon={<ShareIcon />} fullWidth
                onClick={() => createShareMutation.mutate()} disabled={createShareMutation.isPending}
              >
                分享对话
              </Button>
              <Button
                size="small" variant="outlined" color="warning" startIcon={<DeleteSweepIcon />} fullWidth
                onClick={() => clearMutation.mutate()} disabled={clearMutation.isPending}
              >
                清空上下文
              </Button>
            </Stack>
          </Box>
        )}
      </Box>

      {/* 分享对话框 */}
      <Dialog open={shareOpen} onClose={() => setShareOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Stack direction="row" alignItems="center" spacing={1}>
            <ShareIcon color="primary" />
            <Typography variant="subtitle1" fontWeight={600}>分享对话</Typography>
          </Stack>
        </DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            分享链接可让其他人查看此对话内容，消息将仅供查看，不可回复。
          </Alert>
          <TextField
            fullWidth size="small" label="分享链接" value={shareLink}
            onFocus={e => e.target.select()}
            slotProps={{
              htmlInput: { readOnly: true },
              input: {
                endAdornment: (
                  <InputAdornment position="end">
                    <Tooltip title={shareCopying ? '复制中' : '复制链接'}>
                      <IconButton
                        size="small" onClick={() => {
                          setShareCopying(true)
                          copyShareLinkMutation.mutate(shareLink)
                        }}
                        disabled={copyShareLinkMutation.isPending}
                      >
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </InputAdornment>
                ),
              }
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button size="small" onClick={() => setShareOpen(false)}>关闭</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}