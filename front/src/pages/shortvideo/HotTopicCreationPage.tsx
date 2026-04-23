import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Typography,
  Button,
  Skeleton,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
} from '@mui/material'
import WhatshotIcon from '@mui/icons-material/Whatshot'
import RocketLaunchIcon from '@mui/icons-material/RocketLaunch'
import RefreshIcon from '@mui/icons-material/Refresh'
import PsychologyIcon from '@mui/icons-material/Psychology'
import { PageHeader } from '@/components/base'
import { useRolePrefix } from '@/hooks/useRolePrefix'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { fetchHotTopicPool } from '@/api/viral-analysis'
import { useToast } from '@/contexts/ToastContext'
import request from '@/utils/request'

interface HotTopic {
  id: number
  topic: string
  heat?: number
  source?: string
  createdAt?: string
}

type TierLabel = '黄金' | '白银' | '青铜' | '过时'

function classifyTier(createdAt?: string): { label: TierLabel; color: 'error' | 'warning' | 'info' | 'default' } {
  if (!createdAt) return { label: '青铜', color: 'info' }
  const hours = (Date.now() - new Date(createdAt).getTime()) / (1000 * 60 * 60)
  if (hours <= 24) return { label: '黄金', color: 'error' }
  if (hours <= 72) return { label: '白银', color: 'warning' }
  if (hours <= 168) return { label: '青铜', color: 'info' }
  return { label: '过时', color: 'default' }
}

export function HotTopicCreationPage() {
  const navigate = useNavigate()
  const prefix = useRolePrefix()
  const toast = useToast()
  const [topics, setTopics] = useState<HotTopic[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [fusionOpen, setFusionOpen] = useState(false)
  const [fusionTopic, setFusionTopic] = useState<HotTopic | null>(null)
  const [fusionPersonaCode, setFusionPersonaCode] = useState('')
  const [fusionResult, setFusionResult] = useState<string | null>(null)
  const [fusionLoading, setFusionLoading] = useState(false)

  const loadTopics = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetchHotTopicPool(50)
      setTopics(res?.hotTopics ?? [])
    } catch {
      setError('加载热点话题失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTopics()
  }, [loadTopics])

  const handleQuickGenerate = (topic: HotTopic) => {
    const params = new URLSearchParams({ keyword: topic.topic, hotTopicId: String(topic.id) })
    navigate(`${shortvideoRoutes.quickGenerate}?${params.toString()}`)
  }

  const handleOpenFusion = (topic: HotTopic) => {
    setFusionTopic(topic)
    setFusionResult(null)
    setFusionOpen(true)
  }

  const handleFusion = async () => {
    if (!fusionTopic) return
    setFusionLoading(true)
    try {
      const res = await request.post<{ script?: string; fusedScript?: string }>(
        '/short-video/persona-fusion/generate-hotspot-fused',
        {
          hotTopicId: fusionTopic.id,
          hotTopic: fusionTopic.topic,
          personaCode: fusionPersonaCode || undefined,
        }
      )
      setFusionResult((res as Record<string, string>)?.fusedScript ?? (res as Record<string, string>)?.script ?? '生成完成')
      toast('热点三要素融合完成！', 'success')
    } catch {
      toast('融合生成失败', 'error')
    } finally {
      setFusionLoading(false)
    }
  }

  // SV-02: 将融合脚本直接写入脚本策划页
  const handleWriteToScript = () => {
    if (!fusionResult) return
    const params = new URLSearchParams({ preset: fusionResult, hotTopic: fusionTopic?.topic ?? '' })
    navigate(`${shortvideoRoutes.scriptPlanning}?${params.toString()}`)
    setFusionOpen(false)
    toast('已跳转到脚本策划，融合内容已预填', 'success')
  }

  const handleRefresh = () => {
    loadTopics()
    toast('正在刷新热点数据...', 'info')
  }

  return (
    <Box>
      <PageHeader
        title="热点借势"
        subtitle="抓住当前热搜话题，快速创作爆款内容"
        breadcrumbs={[
          { label: '短视频', href: prefix === '/admin' ? shortvideoRoutes.dashboard : `${prefix}/shortvideo` },
          { label: '热点借势' },
        ]}
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
            size="small"
          >
            刷新
          </Button>
        }
      />

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Grid container spacing={2}>
          {Array.from({ length: 6 }).map((_, i) => (
            <Grid item xs={12} sm={6} md={4} key={i}>
              <Skeleton variant="rounded" height={160} />
            </Grid>
          ))}
        </Grid>
      ) : topics.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <WhatshotIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
            <Typography color="text.secondary">
              暂无热点话题，请先在「数据同步」中同步抖音热搜
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={2}>
          {topics.map((topic) => {
            const tier = classifyTier(topic.createdAt)
            return (
              <Grid item xs={12} sm={6} md={4} key={topic.id}>
                <Card
                  sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    transition: 'box-shadow 0.2s',
                    '&:hover': { boxShadow: 4 },
                  }}
                >
                  <CardContent sx={{ flex: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                      <Chip
                        label={tier.label}
                        color={tier.color}
                        size="small"
                        variant={tier.label === '黄金' ? 'filled' : 'outlined'}
                      />
                      {topic.heat != null && topic.heat > 0 && (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <WhatshotIcon sx={{ fontSize: 14, color: 'error.main' }} />
                          <Typography variant="caption" color="error.main" fontWeight={600}>
                            {topic.heat >= 10000
                              ? `${(topic.heat / 10000).toFixed(1)}万`
                              : topic.heat.toLocaleString()}
                          </Typography>
                        </Box>
                      )}
                    </Box>
                    <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 0.5, lineHeight: 1.4 }}>
                      {topic.topic}
                    </Typography>
                    {topic.source && (
                      <Typography variant="caption" color="text.secondary">
                        来源：{topic.source}
                      </Typography>
                    )}
                  </CardContent>
                  <Box sx={{ px: 2, pb: 2, display: 'flex', gap: 1 }}>
                    <Button
                      variant="contained"
                      size="small"
                      fullWidth
                      startIcon={<RocketLaunchIcon />}
                      onClick={() => handleQuickGenerate(topic)}
                    >
                      一键创作
                    </Button>
                    <Button
                      variant="outlined"
                      size="small"
                      startIcon={<PsychologyIcon />}
                      onClick={() => handleOpenFusion(topic)}
                      title="热点三要素融合（热点×人设×产品）"
                    >
                      融合
                    </Button>
                  </Box>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}

      {/* 热点三要素融合对话框（热点 × 人设 × 产品） */}
      <Dialog open={fusionOpen} onClose={() => setFusionOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PsychologyIcon color="primary" />
          热点三要素融合创作
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            将热点话题「{fusionTopic?.topic}」与主播人设、产品卖点三要素融合，生成差异化爆款脚本。
          </Typography>
          <FormControl fullWidth size="small" sx={{ mb: 2 }}>
            <InputLabel>选择主播人设</InputLabel>
            <Select
              value={fusionPersonaCode}
              label="选择主播人设"
              onChange={(e) => setFusionPersonaCode(e.target.value)}
            >
              <MenuItem value="">通用（不指定人设）</MenuItem>
              <MenuItem value="professional">专业达人</MenuItem>
              <MenuItem value="everyday">素人真实</MenuItem>
              <MenuItem value="celebrity">明星感</MenuItem>
              <MenuItem value="local">地方特色</MenuItem>
            </Select>
          </FormControl>
          {fusionResult && (
            <TextField
              multiline
              rows={6}
              fullWidth
              size="small"
              label="融合生成结果"
              value={fusionResult}
              InputProps={{ readOnly: true }}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFusionOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleFusion}
            disabled={fusionLoading}
            startIcon={fusionLoading ? <CircularProgress size={16} /> : <PsychologyIcon />}
          >
            {fusionLoading ? '融合中...' : '开始融合'}
          </Button>
          {fusionResult && (
            <Button
              variant="outlined"
              color="success"
              onClick={handleWriteToScript}
            >
              写入脚本策划 →
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default HotTopicCreationPage
