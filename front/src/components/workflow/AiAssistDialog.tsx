import { useState, useRef, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Chip,
  Typography,
} from '@mui/material'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const NODE_AI_CONFIG: Record<
  string,
  { title: string; placeholder: string; quickActions: string[] }
> = {
  script: {
    title: '脚本 AI 助手',
    placeholder: '描述你想要的修改，比如"增加悬念"、"改为搞笑风格"...',
    quickActions: ['增加悬念钩子', '改为轻松搞笑', '加强冲突', '缩短篇幅', '增加对话'],
  },
  shotList: {
    title: '分镜 AI 助手',
    placeholder: '调整分镜，比如"第3镜改为特写"、"增加一个过渡镜头"...',
    quickActions: ['增加过渡镜头', '合并相似分镜', '调整节奏', '增加特写', '增加全景'],
  },
  keyframe: {
    title: '关键帧 AI 助手',
    placeholder: '调整画面，比如"更暗的光线"、"换成暖色调"...',
    quickActions: ['暖色调', '冷色调', '增加景深', '更亮', '更暗', '复古风格'],
  },
  videoGen: {
    title: '视频生成 AI 助手',
    placeholder: '调整运镜，比如"改为慢速推进"、"用Runway模型"...',
    quickActions: ['换为推轨', '换为环绕', '换为手持', '用MiniMax模型', '用Runway模型', '延长到10秒'],
  },
  postProcess: {
    title: '后期处理 AI 助手',
    placeholder: '调整后期，比如"电影调色"、"增加黑边"...',
    quickActions: ['电影暖调', '电影冷调', '增加黑边', '提高锐度', '降低噪点', '复古胶片'],
  },
  compose: {
    title: '合成 AI 助手',
    placeholder: '调整成片，比如"换BGM"、"修改字幕样式"...',
    quickActions: ['换BGM', '调整字幕位置', '加片头', '加片尾', '调整语速'],
  },
  publish: {
    title: '发布 AI 助手',
    placeholder: '发布前检查，比如"生成5个标题"、"AI审核"...',
    quickActions: ['生成标题', 'AI审核', '生成封面', '推荐发布时间', '生成描述'],
  },
}

interface Props {
  open: boolean
  onClose: () => void
  nodeId: string | null
  onExecute: (nodeId: string) => void
}

export default function AiAssistDialog({ open, onClose, nodeId, onExecute }: Props) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const config = nodeId ? NODE_AI_CONFIG[nodeId] : null

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (open) setMessages([])
  }, [open, nodeId])

  const sendMessage = async (text: string) => {
    if (!text.trim() || !nodeId) return
    const userMsg: Message = { role: 'user', content: text }
    setMessages((prev) => [...prev, userMsg])
    setInput('')
    setLoading(true)

    try {
      // TODO: 调用后端 aiAssist API
      const assistantMsg: Message = {
        role: 'assistant',
        content: `[AI] 已理解你的要求："${text}"。参数已更新，点击"重新执行"生效。`,
      }
      setMessages((prev) => [...prev, assistantMsg])
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', content: '处理失败，请重试' }])
    } finally {
      setLoading(false)
    }
  }

  if (!open || !config) return null

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { maxHeight: '80vh' } }}>
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        {config.title}
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
          {config.quickActions.map((action) => (
            <Chip
              key={action}
              label={action}
              size="small"
              onClick={() => sendMessage(action)}
              sx={{ cursor: 'pointer' }}
            />
          ))}
        </Box>
        <Box sx={{ minHeight: 200, maxHeight: 300, overflow: 'auto' }}>
          {messages.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
              点击快捷操作或输入你的需求
            </Typography>
          )}
          {messages.map((msg, i) => (
            <Box
              key={i}
              sx={{
                display: 'flex',
                justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                mb: 1,
              }}
            >
              <Box
                sx={{
                  px: 2,
                  py: 1,
                  borderRadius: 2,
                  maxWidth: '80%',
                  bgcolor: msg.role === 'user' ? 'primary.main' : 'grey.200',
                  color: msg.role === 'user' ? 'white' : 'text.primary',
                }}
              >
                {msg.content}
              </Box>
            </Box>
          ))}
          {loading && (
            <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
              AI 思考中...
            </Typography>
          )}
          <div ref={messagesEndRef} />
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
        <TextField
          size="small"
          fullWidth
          placeholder={config.placeholder}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && sendMessage(input)}
          sx={{ flex: 1 }}
        />
        <Button variant="contained" onClick={() => sendMessage(input)} disabled={loading}>
          发送
        </Button>
        <Button variant="contained" color="success" onClick={() => nodeId && onExecute(nodeId)}>
          重新执行
        </Button>
      </DialogActions>
    </Dialog>
  )
}
