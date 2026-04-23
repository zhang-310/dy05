import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box, Typography, Paper, Stack, Chip, CircularProgress, Button,
  Card, CardContent, Divider, Alert,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import ShareIcon from '@mui/icons-material/Share'
import VisibilityIcon from '@mui/icons-material/Visibility'
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline'
import { shareApi, type AgentShare, type ChatMessage } from '@/api/agent'

export default function AgentSharePage() {
  const { shareCode } = useParams<{ shareCode: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [share, setShare] = useState<AgentShare | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!shareCode) {
      setError('分享链接无效')
      setLoading(false)
      return
    }

    shareApi.getData(shareCode)
      .then(data => {
        setShare(data.share)
        setMessages(data.messages)
      })
      .catch((err: Error) => {
        console.error('获取分享数据失败:', err)
        setError(err.message || '分享不存在或已失效')
      })
      .finally(() => setLoading(false))
  }, [shareCode])

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <Stack alignItems="center" spacing={2}>
          <CircularProgress />
          <Typography color="text.secondary">加载分享内容...</Typography>
        </Stack>
      </Box>
    )
  }

  if (error || !share) {
    return (
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4, p: 3 }}>
        <Alert severity="error" sx={{ mb: 2 }}>
          {error || '分享不存在或已失效'}
        </Alert>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/ai/agent/market')}>
          返回智能体市场
        </Button>
      </Box>
    )
  }

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'user': return '用户'
      case 'assistant': return 'AI 助手'
      default: return role
    }
  }

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', mt: 3, p: 3 }}>
      {/* 顶部导航 */}
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/ai/agent/market')}
          sx={{ color: 'text.secondary' }}
        >
          返回
        </Button>
        <Divider orientation="vertical" flexItem />
        <Typography variant="h6" fontWeight={600}>{share.title || '分享对话'}</Typography>
      </Stack>

      {/* 分享信息卡片 */}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" alignItems="flex-start" spacing={2}>
            <ShareIcon sx={{ color: 'primary.main', fontSize: 32, mt: 0.5 }} />
            <Box sx={{ flex: 1 }}>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                {share.title || '分享对话'}
              </Typography>
              {share.summary && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                  {share.summary}
                </Typography>
              )}
              <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
                <Chip
                  icon={<VisibilityIcon sx={{ fontSize: 14 }} />}
                  label={`${share.viewCount} 次浏览`}
                  size="small"
                  variant="outlined"
                />
                <Chip
                  icon={<ChatBubbleOutlineIcon sx={{ fontSize: 14 }} />}
                  label={`${share.messageCount} 条消息`}
                  size="small"
                  variant="outlined"
                />
                {share.expiresAt && (
                  <Chip
                    label={`有效期至 ${share.expiresAt.slice(0, 10)}`}
                    size="small"
                    color="warning"
                    variant="outlined"
                  />
                )}
              </Stack>
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* 操作提示 */}
      <Alert severity="info" sx={{ mb: 3 }}>
        此对话仅供查看，如需体验完整功能请前往
        <Button size="small" onClick={() => navigate(`/ai/agent/chat/${share.agentId}`)}>
          与智能体对话
        </Button>
      </Alert>

      {/* 对话记录 */}
      <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 2 }}>
        对话记录
      </Typography>
      <Stack spacing={2}>
        {messages.map((msg) => (
          <Box
            key={msg.id}
            sx={{ display: 'flex', flexDirection: 'column', alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start' }}
          >
            <Paper
              elevation={0}
              sx={{
                p: 1.5, maxWidth: '80%', borderRadius: 2,
                bgcolor: msg.role === 'user' ? 'primary.main' : 'grey.100',
                color: msg.role === 'user' ? 'white' : 'text.primary',
              }}
            >
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                <Typography variant="caption" fontWeight={600} sx={{ opacity: 0.8 }}>
                  {getRoleLabel(msg.role)}
                </Typography>
              </Stack>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {msg.content}
              </Typography>
            </Paper>
            {msg.toolCalls && msg.toolCalls.length > 0 && (
              <Stack direction="row" spacing={0.5} sx={{ mt: 0.5, maxWidth: '80%', flexWrap: 'wrap', gap: 0.5 }}>
                {msg.toolCalls.map((tc, idx) => (
                  <Chip
                    key={idx}
                    label={tc.function?.name || tc.type}
                    size="small"
                    color="info"
                    variant="outlined"
                    sx={{ fontSize: 10, height: 22 }}
                  />
                ))}
              </Stack>
            )}
          </Box>
        ))}
      </Stack>

      {messages.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
          暂无消息记录
        </Typography>
      )}
    </Box>
  )
}
