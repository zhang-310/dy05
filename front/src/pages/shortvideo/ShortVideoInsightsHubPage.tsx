import { Box, Card, CardActionArea, CardContent, Grid, Stack, Typography, Alert } from '@mui/material'
import BusinessIcon from '@mui/icons-material/Business'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import PsychologyIcon from '@mui/icons-material/Psychology'
import CloudDownloadIcon from '@mui/icons-material/CloudDownload'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/base'
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
}> = [
  {
    title: '短视频账号',
    desc: '已采集账号与「采集短视频」Tab，可跳转爆款库并带 videoId。',
    path: shortvideoRoutes.accounts,
    icon: BusinessIcon,
  },
  {
    title: '爆款视频库',
    desc: '入库视频列表、抽屉详情与 LF 深度拆解（异步）。',
    path: shortvideoRoutes.viralVideos,
    icon: VideoLibraryIcon,
  },
  {
    title: '进化爆款分析',
    desc: '进化引擎侧任务与报告，与爆款库拆解为两套能力。',
    path: shortvideoRoutes.viralAnalysisEvolution,
    icon: SmartToyIcon,
  },
  {
    title: '人设融合爆款',
    desc: '将拆解洞见与人设结合，产出二创方向。',
    path: shortvideoRoutes.personaFusion,
    icon: PsychologyIcon,
  },
  {
    title: '账号采集',
    desc: '拉取作品入库，衔接账号与爆款库。',
    path: shortvideoRoutes.collect,
    icon: CloudDownloadIcon,
  },
]

export default function ShortVideoInsightsHubPage() {
  const navigate = useNavigate()

  return (
    <Box sx={{ py: 2, px: { xs: 2, md: 3 } }}>
      <PageHeader title="洞见中心" breadcrumbs={[{ label: '短视频' }, { label: '洞见中心' }]} />

      <Alert severity="info" variant="outlined" sx={{ mb: 3 }}>
        <Typography variant="body2" component="div" gutterBottom>
          <strong>推荐路径：</strong>账号采集 → 短视频账号 → 账号详情「采集短视频」→ 深度拆解 / 打开爆款库（可带 <code>?videoId=</code>）。
        </Typography>
        <Typography variant="caption" color="text.secondary" component="div">
          深链示例：账号详情 <code>{shortvideoAccountDetailPath(1, 'videos')}</code>（将 1 换为真实 id）；爆款库{' '}
          <code>{`${shortvideoRoutes.viralVideos}?videoId=`}</code>
          ；进化分析 canonical：<code>{ADMIN_AI_VIRAL_ANALYSIS_EVOLUTION}</code>
        </Typography>
      </Alert>

      <Grid container spacing={2}>
        {CARDS.map((c) => {
          const Icon = c.icon
          return (
            <Grid item xs={12} sm={6} md={4} key={c.path}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardActionArea onClick={() => navigate(c.path)} sx={{ height: '100%', alignItems: 'stretch' }}>
                  <CardContent>
                    <Stack direction="row" spacing={1.5} alignItems="flex-start">
                      <Box sx={{ color: 'primary.main', pt: 0.25 }}>
                        <Icon />
                      </Box>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                          {c.title}
                        </Typography>
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
