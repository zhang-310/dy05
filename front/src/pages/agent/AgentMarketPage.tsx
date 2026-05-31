import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Card,
  CardContent,
  CardActions,
  Grid,
  Typography,
  Chip,
  Button,
  TextField,
  Stack,
  Avatar,
  Tooltip,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Divider,
  ToggleButton,
  ToggleButtonGroup,
  Alert,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import ChatIcon from '@mui/icons-material/Chat'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import BuildIcon from '@mui/icons-material/Build'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import StarIcon from '@mui/icons-material/Star'
import StarBorderIcon from '@mui/icons-material/StarBorder'
import WhatshotIcon from '@mui/icons-material/Whatshot'
import { alpha } from '@mui/material/styles'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { agentApi, reviewApi, type Agent } from '@/api/agent'
import { CardSkeleton, EmptyState, ErrorAlert } from '@/components/base'
import { SKILL_MAP } from './AgentChatPage'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray } from '@/utils/response-normalize'

// 智能体类型配置
type AgentTypeTone = 'default' | 'primary' | 'error' | 'warning' | 'secondary' | 'success' | 'info'

const AGENT_TYPE_CONFIG: Record<number, { label: string; tone: AgentTypeTone; emoji: string }> = {
  0: { label: '自定义', tone: 'default', emoji: '🤖' },
  1: { label: '话术生成', tone: 'primary', emoji: '🎤' },
  2: { label: '违规检测', tone: 'error', emoji: '🔍' },
  3: { label: '商品分析', tone: 'warning', emoji: '📦' },
  4: { label: '场次规划', tone: 'secondary', emoji: '📅' },
  5: { label: '数据分析', tone: 'success', emoji: '📊' },
  6: { label: '客户服务', tone: 'info', emoji: '💬' },
}

function StarRating({ rating, onRate, readonly = false, size = 20 }: {
  rating: number; onRate?: (r: number) => void; readonly?: boolean; size?: number
}) {
  return (
    <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.25 }}>
      {[1, 2, 3, 4, 5].map((star) => (
        <IconButton
          key={star}
          size="small"
          disabled={readonly}
          onClick={() => !readonly && onRate?.(star)}
          sx={{ p: 0.25 }}
        >
          {star <= rating
            ? <StarIcon sx={{ fontSize: size, color: 'warning.main' }} />
            : <StarBorderIcon sx={{ fontSize: size, color: 'text.disabled' }} />}
        </IconButton>
      ))}
    </Box>
  )
}

function ReviewDialog({ agent, open, onClose }: { agent: Agent | null; open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [rating, setRating] = useState(0)
  const [content, setContent] = useState('')

  useEffect(() => {
    if (open) {
      setRating(0)
      setContent('')
    }
  }, [agent?.id, open])

  // Load user's existing review
  const { isError: reviewLoadFailed, error: reviewLoadError } = useQuery({
    queryKey: ['agent-review-my', agent?.id],
    queryFn: async () => {
      if (!agent) return null
      const r = await reviewApi.myReview(agent.id)
      if (r) {
        setRating(r.rating)
        setContent(r.content ?? '')
      }
      return r
    },
    enabled: open && agent !== null,
  })

  const submitMutation = useMutation({
    mutationFn: async () => {
      if (!agent || rating === 0) return
      return reviewApi.submit(agent.id, rating, content || undefined)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agent-market'] })
      queryClient.invalidateQueries({ queryKey: ['agent-review-my', agent?.id] })
      queryClient.invalidateQueries({ queryKey: ['agent-review-stats', agent?.id] })
      onClose()
    },
  })

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        'data-testid': 'agent-review-dialog',
        'data-ready-endpoints': '/agent/review/my|/agent/review/submit',
        'data-input-retained': 'true',
      }}
    >
      <DialogTitle>
        <Stack direction="row" alignItems="center" spacing={1}>
          <SmartToyIcon color="primary" />
          <Typography variant="h6">评价「{agent?.agentName}」</Typography>
        </Stack>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ pt: 2 }}>
        {reviewLoadFailed && (
          <Alert
            severity="warning"
            data-testid="agent-review-my-error"
            data-no-local-review-fallback="true"
            sx={{ mb: 2 }}
          >
            读取已有评分失败（POST /agent/review/my）：{getErrorMessage(reviewLoadError)}。仍可重新提交本次评分。
          </Alert>
        )}
        {submitMutation.isError && (
          <Alert
            severity="error"
            data-testid="agent-review-submit-error"
            data-input-retained="true"
            data-no-local-review-mutation="true"
            sx={{ mb: 2 }}
          >
            提交评分失败（POST /agent/review/submit）：{getErrorMessage(submitMutation.error)}。评分和评价内容已保留在弹窗内。
          </Alert>
        )}
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>选择评分</Typography>
          <StarRating rating={rating} onRate={setRating} size={32} />
          {rating > 0 && (
            <Typography variant="caption" sx={{ ml: 1 }}>
              {['很差', '较差', '一般', '较好', '很好'][rating - 1]}
            </Typography>
          )}
        </Box>
        <TextField
          label="评价内容（可选）"
          multiline
          rows={3}
          fullWidth
          placeholder="分享您的使用体验..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
          inputProps={{ maxLength: 1000 }}
          helperText={`${content.length}/1000`}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          disabled={rating === 0 || submitMutation.isPending}
          onClick={() => submitMutation.mutate()}
        >
          {submitMutation.isPending ? '提交中...' : '提交评价'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AgentMarketPage() {
  const navigate = useNavigate()
  const [searchKeyword, setSearchKeyword] = useState('')
  const [typeFilter, setTypeFilter] = useState<number | null>(null)
  const [reviewDialogAgent, setReviewDialogAgent] = useState<Agent | null>(null)
  const [sortBy, setSortBy] = useState<string>('default')

  const { data: agents = [], isLoading, isError, error, refetch } = useQuery({
    queryKey: ['agent-market', sortBy],
    queryFn: async () => {
      const result = await agentApi.list({ page: 0, rows: 100, sortBy: sortBy === 'default' ? undefined : sortBy })
      return normalizeArray<Agent>(result)
    },
  })

  // 过滤智能体
  const filteredAgents = agents.filter((agent) => {
    if (typeFilter !== null && agent.agentType !== typeFilter) return false
    if (searchKeyword) {
      const keyword = searchKeyword.toLowerCase()
      return (
        agent.agentName?.toLowerCase().includes(keyword) ||
        agent.description?.toLowerCase().includes(keyword)
      )
    }
    return true
  })

  // 按类型分组统计
  const typeStats = agents.reduce((acc, agent) => {
    acc[agent.agentType] = (acc[agent.agentType] || 0) + 1
    return acc
  }, {} as Record<number, number>)

  const handleChat = (agentId: number) => {
    navigate(`/admin/ai/agent/chat/${agentId}`)
  }

  const parseTools = (availableTools: string | null | undefined): string[] => {
    if (!availableTools) return []
    try {
      const parsed = JSON.parse(availableTools)
      return Array.isArray(parsed) ? parsed.filter((tool): tool is string => typeof tool === 'string') : []
    } catch {
      return []
    }
  }

  return (
    <Box
      data-testid="agent-market-page"
      data-ready-endpoints="/agent/list|/agent/review/my|/agent/review/submit"
      data-unsupported-endpoints="/agent/market/mock|/agent/market/local-list|/agent/market/static-rating|/agent/review/local|/agent/review/static|/agent/local-conversation-count"
      data-no-local-agent-fallback="true"
      data-no-static-rating-fallback="true"
      data-server-sort="true"
      sx={{ p: 3 }}
    >
      {/* 页头 */}
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight={600} gutterBottom>
            智能体市场
          </Typography>
          <Typography variant="body2" color="text.secondary">
            发现和使用 {agents.length} 个专业智能体，提升运营效率
          </Typography>
        </Box>
        <Stack direction="row" spacing={2} alignItems="center">
          <ToggleButtonGroup
            value={sortBy}
            exclusive
            onChange={(_, v) => v !== null && setSortBy(v)}
            size="small"
          >
            <ToggleButton value="default">最新</ToggleButton>
            <ToggleButton value="rating">
              <StarIcon sx={{ fontSize: 14, mr: 0.5 }} />评分
            </ToggleButton>
            <ToggleButton value="popular">
              <WhatshotIcon sx={{ fontSize: 14, mr: 0.5 }} />热门
            </ToggleButton>
            <ToggleButton value="name">名称</ToggleButton>
          </ToggleButtonGroup>
          <Button variant="outlined" startIcon={<SmartToyIcon />} onClick={() => navigate('/admin/ai/agent/list')}>
            管理智能体
          </Button>
        </Stack>
      </Stack>

      <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
        列表来自 `/agent/list`，排序参数由后端消费；评分提交走 `/agent/review/submit`，失败会保留在弹窗内提示。
      </Alert>

      {/* 搜索和筛选 */}
      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <TextField
          placeholder="搜索智能体名称或描述..."
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          size="small"
          sx={{ width: 400 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <Chip
            label={`全部 (${agents.length})`}
            onClick={() => setTypeFilter(null)}
            color={typeFilter === null ? 'primary' : 'default'}
            variant={typeFilter === null ? 'filled' : 'outlined'}
          />
          {Object.entries(AGENT_TYPE_CONFIG).map(([type, config]) => {
            const count = typeStats[Number(type)] || 0
            if (count === 0) return null
            return (
              <Chip
                key={type}
                label={`${config.emoji} ${config.label} (${count})`}
                onClick={() => setTypeFilter(Number(type))}
                color={typeFilter === Number(type) ? 'primary' : 'default'}
                variant={typeFilter === Number(type) ? 'filled' : 'outlined'}
              />
            )
          })}
        </Stack>
      </Stack>

      {isError && (
        <Box data-testid="agent-market-list-error" data-no-local-agent-fallback="true">
          <ErrorAlert
            title="智能体市场加载失败"
            message={`${getErrorMessage(error)}。请检查 /agent/list、登录态和智能体数据权限。页面不会补本地市场智能体。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}

      {/* 智能体卡片网格 */}
      {isError ? null : isLoading ? (
        <CardSkeleton count={6} variant="detailed" />
      ) : agents.length === 0 ? (
        <Box data-testid="agent-market-empty" data-no-local-agent-fallback="true" data-no-static-rating-fallback="true">
          <EmptyState
            title="还没有智能体"
            description="创建第一个智能体，开始您的 AI 助手之旅"
            action={{
              text: '创建智能体',
              onClick: () => navigate('/admin/ai/agent/list'),
            }}
          />
        </Box>
      ) : filteredAgents.length === 0 ? (
        <Card
          variant="outlined"
          data-testid="agent-market-filter-empty"
          data-no-local-agent-fallback="true"
          sx={{ textAlign: 'center', py: 8 }}
        >
          <Typography color="text.secondary">未找到匹配的智能体</Typography>
        </Card>
      ) : (
        <Grid
          container
          spacing={2}
          data-testid="agent-market-grid"
          data-source-endpoint="/agent/list"
          data-no-local-agent-fallback="true"
        >
          {filteredAgents.map((agent) => {
            const typeConfig = AGENT_TYPE_CONFIG[agent.agentType] || AGENT_TYPE_CONFIG[0]
            const tools = parseTools(agent.availableTools)

            return (
              <Grid item xs={12} sm={6} md={4} key={agent.id}>
                <Card
                  variant="outlined"
                  data-testid="agent-market-card"
                  data-agent-id={agent.id}
                  data-source-endpoint="/agent/list"
                  sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    transition: 'all 0.2s',
                    '&:hover': {
                      boxShadow: 3,
                      transform: 'translateY(-4px)',
                    },
                  }}
                >
                  <CardContent sx={{ flex: 1 }}>
                    {/* 头像和类型 */}
                    <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 1 }}>
                      <Avatar
                        data-testid="agent-market-type-avatar-surface"
                        sx={{
                          bgcolor: (theme) => typeConfig.tone === 'default'
                            ? alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.14 : 0.08)
                            : alpha(theme.palette[typeConfig.tone].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                          color: typeConfig.tone === 'default' ? 'text.secondary' : `${typeConfig.tone}.main`,
                          width: 48,
                          height: 48,
                          fontSize: 24,
                        }}
                      >
                        {typeConfig.emoji}
                      </Avatar>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography variant="h6" fontWeight={600} noWrap>
                          {agent.agentName}
                        </Typography>
                        <Chip
                          label={typeConfig.label}
                          size="small"
                          data-testid="agent-market-type-chip-surface"
                          sx={{
                            bgcolor: (theme) => typeConfig.tone === 'default'
                              ? alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.14 : 0.08)
                              : alpha(theme.palette[typeConfig.tone].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                            color: typeConfig.tone === 'default' ? 'text.secondary' : `${typeConfig.tone}.main`,
                            fontWeight: 500,
                            height: 20,
                            fontSize: 11,
                          }}
                        />
                      </Box>
                    </Stack>

                    {/* 评分和热度 */}
                    <Box sx={{ mb: 1.5 }}>
                      <Stack direction="row" alignItems="center" spacing={1}>
                        <StarRating rating={Math.round(agent.averageRating ?? 0)} readonly size={14} />
                        <Typography variant="caption" color="text.secondary">
                          {(agent.averageRating ?? 0) > 0
                            ? `${agent.averageRating!.toFixed(1)} (${agent.ratingCount}条)`
                            : '暂无评分'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
                          <WhatshotIcon sx={{ fontSize: 12, color: 'warning.main' }} />
                          {agent.conversationCount ?? 0}次对话
                        </Typography>
                        <Button
                          size="small"
                          variant="text"
                          aria-label={`评价 ${agent.agentName}`}
                          sx={{ ml: 'auto', minWidth: 0, p: 0.25, fontSize: 11, textTransform: 'none' }}
                          onClick={(e) => { e.stopPropagation(); setReviewDialogAgent(agent) }}
                        >
                          评分
                        </Button>
                      </Stack>
                    </Box>

                    {/* 描述 */}
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{
                        mb: 1.5,
                        height: 40,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                      }}
                    >
                      {agent.description || '暂无描述'}
                    </Typography>

                    {/* 技能标签 */}
                    {tools.length > 0 && (
                      <Stack spacing={0.5}>
                        <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                          <Tooltip title="点击技能查看详情">
                            <InfoOutlinedIcon sx={{ fontSize: 14, color: 'primary.main' }} />
                          </Tooltip>
                          {tools.slice(0, 3).map((tool) => {
                            const skill = SKILL_MAP[tool]
                            return (
                              <Tooltip
                                key={tool}
                                title={skill ? `${skill.label}: ${skill.description}` : tool}
                                arrow placement="top"
                              >
                                <Chip
                                  icon={skill?.icon ?? <BuildIcon sx={{ fontSize: 10 }} />}
                                  label={skill?.label ?? tool}
                                  size="small"
                                  variant="outlined"
                                  sx={{ height: 20, fontSize: 10 }}
                                />
                              </Tooltip>
                            )
                          })}
                          {tools.length > 3 && (
                            <Tooltip
                              title={
                                <Box>
                                  {tools.slice(3).map(t => {
                                    const s = SKILL_MAP[t]
                                    return <Box key={t} sx={{ fontSize: 11 }}>• {s?.label ?? t}: {s?.description}</Box>
                                  })}
                                </Box>
                              }
                              arrow placement="top"
                            >
                              <Chip label={`+${tools.length - 3}`} size="small" variant="outlined" sx={{ height: 20, fontSize: 10 }} />
                            </Tooltip>
                          )}
                        </Stack>
                        <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>
                          配备 {tools.length} 个技能 · 根据输入自动触发
                        </Typography>
                      </Stack>
                    )}
                  </CardContent>

                  <CardActions sx={{ px: 2, pb: 2 }}>
                    <Button
                      variant="contained"
                      fullWidth
                      startIcon={<ChatIcon />}
                      onClick={() => handleChat(agent.id)}
                      sx={{ textTransform: 'none' }}
                    >
                      开始对话
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}

      {/* 评分对话框 */}
      <ReviewDialog
        agent={reviewDialogAgent}
        open={reviewDialogAgent !== null}
        onClose={() => setReviewDialogAgent(null)}
      />
    </Box>
  )
}
