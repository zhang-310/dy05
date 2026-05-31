import { Box, Card, CardActionArea, CardContent, Grid, Stack, Typography, Alert, Chip } from '@mui/material'
import BusinessIcon from '@mui/icons-material/Business'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import PsychologyIcon from '@mui/icons-material/Psychology'
import CloudDownloadIcon from '@mui/icons-material/CloudDownload'
import ArticleIcon from '@mui/icons-material/Article'
import RecommendIcon from '@mui/icons-material/Recommend'
import { useNavigate, Link } from 'react-router-dom'
import { useCallback } from 'react'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { CREDITS_GOVERNANCE_PATH, commercialDenialMessage } from '@/utils/commercialError'
import {
  shortvideoRoutes,
  shortvideoAccountDetailPath,
  ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION,
} from '@/constants/shortvideoRoutes'

const CARDS: Array<{
  title: string
  desc: string
  path: string
  icon: typeof BusinessIcon
  status: 'real' | 'partial' | 'external'
}> = [
  {
    title: '短视频账号',
    desc: '已采集账号与「采集短视频」Tab，可跳转爆款库并带 videoId。',
    path: shortvideoRoutes.accounts,
    icon: BusinessIcon,
    status: 'real',
  },
  {
    title: '爆款视频库',
    desc: '入库视频列表、抽屉详情与 LF 深度拆解（异步）。',
    path: shortvideoRoutes.viralVideos,
    icon: VideoLibraryIcon,
    status: 'real',
  },
  {
    title: '进化爆款分析',
    desc: '进化引擎侧任务与报告，与爆款库拆解为两套能力。',
    path: shortvideoRoutes.viralAnalysisEvolution,
    icon: SmartToyIcon,
    status: 'external',
  },
  {
    title: '人设融合爆款',
    desc: '将拆解洞见与人设结合，商品/话题/时长仍为前端降级约束。',
    path: shortvideoRoutes.personaFusion,
    icon: PsychologyIcon,
    status: 'partial',
  },
  {
    title: '账号采集',
    desc: '拉取作品入库，衔接账号与爆款库。',
    path: shortvideoRoutes.collect,
    icon: CloudDownloadIcon,
    status: 'real',
  },
  {
    title: '对标视频',
    desc: '对标账号下的视频采集、分析状态和素材归档入口。',
    path: shortvideoRoutes.benchmarkVideos,
    icon: VideoLibraryIcon,
    status: 'real',
  },
  {
    title: '质量脚本库',
    desc: '高质量脚本、向量索引状态和推荐引用来源。',
    path: shortvideoRoutes.benchmarkQualityScripts,
    icon: ArticleIcon,
    status: 'real',
  },
  {
    title: '脚本推荐',
    desc: '依赖 embedding/Milvus 的相似脚本与改写推荐。',
    path: shortvideoRoutes.benchmarkScriptRecommendation,
    icon: RecommendIcon,
    status: 'partial',
  },
]

const STATUS_META = {
  real: { label: '真实链路', color: 'success' },
  partial: { label: '部分降级', color: 'warning' },
  external: { label: '跨模块', color: 'info' },
} as const

const INSIGHTS_HUB_ROUTES = CARDS.map((card) => card.path).join('|')
const INSIGHTS_HUB_READY_ENDPOINTS = ['navigation-only:no-page-api-request'] as const
const INSIGHTS_HUB_UNSUPPORTED_ENDPOINTS = [
  '/short-video/insights/mock',
  '/short-video/insights/local-ranking',
  '/short-video/insights/static-summary',
  '/short-video/insights/local-quality-script-generation',
  '/short-video/viral/local-analysis-fallback',
] as const
const INSIGHTS_HUB_UNSUPPORTED_ACTIONS = [
  'page-api-request',
  'inline-business-aggregation',
  'local-insight-ranking',
  'local-quality-script-generation',
  'local-viral-analysis-fallback',
].join('|')
const INSIGHTS_HUB_SUPPORTED_ACTIONS = [
  'navigate-account-library',
  'navigate-viral-library',
  'navigate-viral-analysis-evolution',
  'navigate-persona-fusion',
  'navigate-account-collect',
  'navigate-benchmark-videos',
  'navigate-quality-scripts',
  'navigate-script-recommendation',
].join('|')

export default function ShortVideoInsightsHubPage() {
  const navigate = useNavigate()
  const toast = useToast()

  const handleCardNavigate = useCallback(async (path: string) => {
    if (path === shortvideoRoutes.viralVideos || path === shortvideoRoutes.benchmarkVideos) {
      try {
        const decision = await checkGaifanEntitlement('video-insight', 'video-insight.breakdown')
        if (!decision?.granted) {
          toast(commercialDenialMessage(decision), 'warning')
          return
        }
      } catch (e) {
        toast(commercialDenialMessage(e), 'error')
        return
      }
    }
    navigate(path)
  }, [navigate, toast])

  return (
    <Box
      sx={{ py: 2, px: { xs: 2, md: 3 } }}
      data-testid="shortvideo-insights-hub-workbench"
      data-contract-scope="shortvideo-insights-navigation-hub"
      data-ready-endpoints={INSIGHTS_HUB_READY_ENDPOINTS.join('|')}
      data-unsupported-endpoints={INSIGHTS_HUB_UNSUPPORTED_ENDPOINTS.join('|')}
      data-ready-routes={INSIGHTS_HUB_ROUTES}
      data-supported-actions={INSIGHTS_HUB_SUPPORTED_ACTIONS}
      data-unsupported-actions={INSIGHTS_HUB_UNSUPPORTED_ACTIONS}
      data-card-count={CARDS.length}
      data-real-route-count={CARDS.filter(card => card.status === 'real').length}
      data-partial-route-count={CARDS.filter(card => card.status === 'partial').length}
      data-external-route-count={CARDS.filter(card => card.status === 'external').length}
      data-no-page-api-request="true"
      data-no-inline-business-aggregation="true"
      data-no-local-insight-ranking="true"
      data-no-local-quality-script-generation="true"
      data-navigation-hub-only="true"
    >
      <PageHeader title="洞见中心" breadcrumbs={[{ label: '短视频' }, { label: '洞见中心' }]} />

      <Alert severity="warning" variant="outlined" sx={{ mb: 2 }}>
        产品 <strong>video-insight</strong>：深度拆解将消耗权益与积分。无权益或积分不足时请到{' '}
        <Link to={CREDITS_GOVERNANCE_PATH}>积分治理</Link>。
      </Alert>

      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 3 }}
        data-testid="shortvideo-insights-hub-route-contract"
        data-ready-routes={INSIGHTS_HUB_ROUTES}
        data-no-page-api-request="true"
        data-supported-actions={INSIGHTS_HUB_SUPPORTED_ACTIONS}
      >
        <Typography variant="body2" component="div" gutterBottom>
          <strong>推荐路径：</strong>账号采集 → 短视频账号 → 账号详情「采集短视频」→ 深度拆解 / 打开爆款库（可带 <code>?videoId=</code>）。
        </Typography>
        <Typography variant="caption" color="text.secondary" component="div">
          深链示例：账号详情 <code>{shortvideoAccountDetailPath(1, 'videos')}</code>（将 1 换为真实 id）；爆款库{' '}
          <code>{`${shortvideoRoutes.viralVideos}?videoId=`}</code>
          ；进化分析 canonical：<code>{ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION}</code>
        </Typography>
      </Alert>

      <Alert
        severity="warning"
        variant="outlined"
        sx={{ mb: 3 }}
        data-testid="shortvideo-insights-hub-downgrade-contract"
        data-unsupported-actions={INSIGHTS_HUB_UNSUPPORTED_ACTIONS}
        data-no-inline-business-aggregation="true"
        data-no-local-insight-ranking="true"
      >
        洞见中心只保留导航和链路诊断，不在本页聚合业务数据；卡片标签标明真实链路、跨模块跳转或部分降级。
      </Alert>

      <Grid container spacing={2}>
        {CARDS.map((c) => {
          const Icon = c.icon
          return (
            <Grid item xs={12} sm={6} md={4} key={c.path}>
              <Card
                variant="outlined"
                sx={{ height: '100%' }}
                data-testid="shortvideo-insights-hub-card"
                data-card-title={c.title}
                data-target-route={c.path}
                data-capability-status={c.status}
                data-no-page-api-request="true"
                data-navigation-only="true"
              >
                <CardActionArea
                  onClick={() => { void handleCardNavigate(c.path) }}
                  sx={{ height: '100%', alignItems: 'stretch' }}
                  data-testid={`shortvideo-insights-hub-card-action-${c.status}-${c.title}`}
                  data-target-route={c.path}
                >
                  <CardContent>
                    <Stack direction="row" spacing={1.5} alignItems="flex-start">
                      <Box sx={{ color: 'primary.main', pt: 0.25 }}>
                        <Icon />
                      </Box>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                          {c.title}
                        </Typography>
                        <Chip
                          size="small"
                          label={STATUS_META[c.status].label}
                          color={STATUS_META[c.status].color}
                          variant="outlined"
                          sx={{ mb: 1 }}
                        />
                        <Typography variant="body2" color="text.secondary">
                          {c.desc}
                        </Typography>
                      </Box>
                    </Stack>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          )
        })}
      </Grid>
    </Box>
  )
}
