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
  TextField,
  Stack,
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
import { generateHotspotFused } from '@/api/viral-analysis'
import { getErrorMessage } from '@/utils/errorHandler'

interface HotTopic {
  id: number
  topic: string
  heat?: number
  source?: string
  createdAt?: string
}

type TierLabel = '黄金' | '白银' | '青铜' | '过时'
const HOT_TOPIC_POOL_ENDPOINT = '/short-video/cross/hot-topic-pool'
const HOTSPOT_FUSION_ENDPOINT = '/short-video/persona-fusion/generate-hotspot-fused'
const HOT_TOPIC_READY_ENDPOINTS = [
  HOT_TOPIC_POOL_ENDPOINT,
  HOTSPOT_FUSION_ENDPOINT,
].join('|')
const HOT_TOPIC_CREATION_READY_ROUTES = [
  shortvideoRoutes.hotTopicCreate,
  shortvideoRoutes.quickGenerate,
  shortvideoRoutes.scriptPlanning,
].join('|')
const HOT_TOPIC_CREATION_SUPPORTED_ACTIONS = [
  'refresh-hot-topic-pool',
  'navigate-quick-generate',
  'generate-hotspot-fused-script',
  'write-fused-script-to-script-planning',
].join('|')
const HOT_TOPIC_UNSUPPORTED_ENDPOINTS = [
  '/short-video/cross/hot-topic-mock',
  '/short-video/cross/local-hot-topic-pool',
  '/short-video/persona-fusion/local-generate-hotspot-fused',
  '/short-video/persona-fusion/mock-hotspot-fused',
  '/short-video/hot-topic/local-template',
  '/short-video/hot-topic/export',
].join('|')

function classifyTier(createdAt?: string): { label: TierLabel; color: 'error' | 'warning' | 'info' | 'default' } {
  if (!createdAt) return { label: '青铜', color: 'info' }
  const hours = (Date.now() - new Date(createdAt).getTime()) / (1000 * 60 * 60)
  if (hours <= 24) return { label: '黄金', color: 'error' }
  if (hours <= 72) return { label: '白银', color: 'warning' }
  if (hours <= 168) return { label: '青铜', color: 'info' }
  return { label: '过时', color: 'default' }
}

function normalizeTopics(raw: unknown): HotTopic[] {
  if (Array.isArray(raw)) return raw as HotTopic[]
  if (raw && typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    return normalizeTopics(obj.hotTopics ?? obj.list ?? obj.records ?? obj.items ?? obj.rows ?? obj.content ?? obj.data)
  }
  return []
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
  const [fusionPersonaId, setFusionPersonaId] = useState('')
  const [fusionProductId, setFusionProductId] = useState('')
  const [fusionResult, setFusionResult] = useState<string | null>(null)
  const [fusionLoading, setFusionLoading] = useState(false)
  const [fusionError, setFusionError] = useState('')

  const loadTopics = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetchHotTopicPool(50)
      setTopics(normalizeTopics(res))
    } catch (e) {
      setError(`加载热点话题失败（POST ${HOT_TOPIC_POOL_ENDPOINT}）：${getErrorMessage(e)}。页面不会使用本地模拟热榜，请检查热点同步任务和 sv_hot_topic。`)
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
    setFusionError('')
    setFusionOpen(true)
  }

  const handleFusion = async () => {
    if (!fusionTopic) return
    const personaId = Number(fusionPersonaId)
    const productId = fusionProductId.trim() ? Number(fusionProductId) : undefined
    if (!Number.isFinite(personaId) || personaId <= 0) {
      toast('请填写有效 personaId', 'warning')
      return
    }
    setFusionLoading(true)
    setFusionError('')
    try {
      const res = await generateHotspotFused(fusionTopic.id, personaId, productId)
      setFusionResult(String(res.fusedScript ?? res.script ?? res.content ?? JSON.stringify(res, null, 2)))
      toast('热点三要素融合完成！', 'success')
    } catch (e) {
      const message = getErrorMessage(e)
      setFusionError(message)
      toast(`融合生成失败：${message}`, 'error')
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
    <Box
      data-testid="hot-topic-creation-page"
      data-ready-endpoints={HOT_TOPIC_READY_ENDPOINTS}
      data-ready-routes={HOT_TOPIC_CREATION_READY_ROUTES}
      data-supported-actions={HOT_TOPIC_CREATION_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={HOT_TOPIC_UNSUPPORTED_ENDPOINTS}
      data-no-local-hot-topic-fallback="true"
      data-no-local-fusion-template="true"
    >
      <PageHeader
        title="热点借势"
        subtitle={`抓住当前热搜话题，通过 POST ${HOTSPOT_FUSION_ENDPOINT} 生成差异化脚本。`}
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
            data-testid="hot-topic-creation-refresh-button"
            data-source-endpoint={HOT_TOPIC_POOL_ENDPOINT}
          >
            刷新
          </Button>
        }
      />
      <Alert
        severity="info"
        variant="outlined"
        data-testid="hot-topic-creation-boundary-contract"
        data-no-local-hot-topic-fallback="true"
        data-no-local-fusion-template="true"
        data-script-prefill-navigation-only="true"
        data-supported-actions={HOT_TOPIC_CREATION_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        热点列表来自 POST {HOT_TOPIC_POOL_ENDPOINT}；热点三要素融合走 POST {HOTSPOT_FUSION_ENDPOINT}，后端必填 hotTopicId 和 personaId，productId 可选。
      </Alert>

      {error && (
        <Alert
          severity="error"
          data-testid="hot-topic-pool-error"
          data-no-local-hot-topic-fallback="true"
          data-no-mock-hot-topic-card="true"
          sx={{ mb: 2 }}
        >
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
        <Card data-testid="hot-topic-empty" data-no-mock-hot-topic-card="true">
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
                  data-testid="hot-topic-card"
                  data-no-local-tier-source="client-time-only"
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
                      data-testid="hot-topic-quick-generate-link"
                      data-navigation-only="true"
                      data-target-route={shortvideoRoutes.quickGenerate}
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
        <DialogContent data-testid="hot-topic-fusion-dialog" data-input-retained="true" data-no-local-fusion-template="true">
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            将热点话题「{fusionTopic?.topic}」与主播人设、产品卖点三要素融合，生成差异化爆款脚本。
          </Typography>
          <Stack spacing={2} sx={{ mb: 2 }}>
            <TextField
              label="Persona ID"
              size="small"
              type="number"
              required
              value={fusionPersonaId}
              onChange={(e) => setFusionPersonaId(e.target.value)}
              helperText="后端按 personaId 查询人设，不支持 personaCode。"
            />
            <TextField
              label="Product ID（可选）"
              size="small"
              type="number"
              value={fusionProductId}
              onChange={(e) => setFusionProductId(e.target.value)}
              helperText="未填写时只做热点 × 人设融合。"
            />
          </Stack>
          {fusionError && (
            <Alert
              severity="error"
              data-testid="hot-topic-fusion-error"
              data-input-retained="true"
              data-no-local-fusion-template="true"
              sx={{ mb: 2 }}
            >
              融合生成失败（POST {HOTSPOT_FUSION_ENDPOINT}）：{fusionError}。请检查热点池、人设 ID、产品 ID；当前热点和输入会保留，不生成本地模板脚本。
            </Alert>
          )}
          {fusionResult && (
            <TextField
              data-testid="hot-topic-fusion-result"
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
            disabled={fusionLoading || !fusionPersonaId}
            startIcon={fusionLoading ? <CircularProgress size={16} /> : <PsychologyIcon />}
            data-testid="hot-topic-fusion-confirm-button"
            data-source-endpoint={HOTSPOT_FUSION_ENDPOINT}
          >
            {fusionLoading ? '融合中...' : '开始融合'}
          </Button>
          {fusionResult && (
            <Button
              variant="outlined"
              color="success"
              data-testid="hot-topic-write-to-script-link"
              data-navigation-only="true"
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
