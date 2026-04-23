import { useMemo, useState } from 'react'
import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  Typography,
  Button,
  CircularProgress,
  Chip,
  Stack,
  Divider,
} from '@mui/material'
import { useTheme } from '@mui/material/styles'
import {
  Movie as VideoIcon,
  Rocket as RocketIcon,
  LocalFireDepartment as ViralIcon,
  CalendarMonth as CalendarIcon,
  Collections as BatchIcon,
  Theaters as DramaIcon,
  TrackChanges as TrackChangesIcon,
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import ReactECharts from 'echarts-for-react'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const QUICK_CARDS = [
  { id: 'insights', label: '洞见中心', sub: '账号·爆款库·进化分析', icon: TrackChangesIcon, path: shortvideoRoutes.insights },
  { id: 'quick', label: '一键生成', sub: '3分钟成片', icon: RocketIcon, path: shortvideoRoutes.quickGenerate },
  { id: 'viral', label: '爆款视频库', sub: '入库与深度拆解', icon: ViralIcon, path: shortvideoRoutes.viralVideos },
  { id: 'drama', label: '短剧创作', sub: '多集剧本 AI 生成', icon: DramaIcon, path: shortvideoRoutes.drama },
  { id: 'batch', label: '批量生产', sub: '高效批量', icon: BatchIcon, path: shortvideoRoutes.projects },
  { id: 'calendar', label: '内容日历', sub: '排期与发布', icon: CalendarIcon, path: shortvideoRoutes.contentCalendar },
]

const TREND_OPTIONS = [{ label: '近7天', days: 7 }, { label: '近30天', days: 30 }]

export default function ShortVideoDashboardPage() {
  const theme = useTheme()
  const navigate = useNavigate()
  const [days, setDays] = useState(7)

  const { data: projects = [], isLoading: loadingProjects } = useQuery({
    queryKey: ['sv-projects-top'],
    queryFn: () => shortvideoApi.list({ page: 0, rows: 5 }).then((r) => r.list ?? []),
  })

  const { data: trend = [], isLoading: loadingTrend } = useQuery({
    queryKey: ['sv-trend', days],
    queryFn: () => shortvideoApi.dataTrend({ days }),
  })

  const trendData = Array.isArray(trend) ? trend : []

  const chartOption = useMemo(
    () => ({
      color: [theme.palette.primary.main, theme.palette.secondary.main, theme.palette.warning.main],
      tooltip: { trigger: 'axis' },
      legend: { data: ['播放量', '点赞数', '评论数'], textStyle: { color: theme.palette.text.secondary } },
      grid: { left: 48, right: 24, top: 40, bottom: 32 },
      xAxis: {
        type: 'category',
        data: trendData.map((d) => String(d.date ?? '').slice(5)),
        axisLine: { lineStyle: { color: theme.palette.divider } },
        axisLabel: { color: theme.palette.text.secondary },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: theme.palette.divider } },
        axisLabel: { color: theme.palette.text.secondary },
      },
      series: [
        { name: '播放量', type: 'line', smooth: true, data: trendData.map((d) => Number(d.playCount ?? 0)) },
        { name: '点赞数', type: 'line', smooth: true, data: trendData.map((d) => Number(d.likeCount ?? 0)) },
        { name: '评论数', type: 'line', smooth: true, data: trendData.map((d) => Number(d.commentCount ?? 0)) },
      ],
    }),
    [theme, trendData],
  )

  return (
    <Box sx={{ bgcolor: 'background.default', minHeight: '100%', py: 2, px: { xs: 2, md: 3 } }}>
      <PageHeader title="短视频总览" breadcrumbs={[{ label: '短视频' }, { label: '总览' }]} />

      <Stack direction="row" flexWrap="wrap" useFlexGap spacing={2} sx={{ mb: 3 }}>
        {QUICK_CARDS.map((card) => {
          const Icon = card.icon
          return (
            <Box
              key={card.id}
              sx={{
                flex: '1 1 140px',
                minWidth: { xs: '100%', sm: 'calc(50% - 8px)', md: 'calc(33.333% - 11px)', lg: 'calc(20% - 13px)' },
                maxWidth: { lg: 'calc(20% - 13px)' },
              }}
            >
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardActionArea onClick={() => navigate(card.path)} sx={{ py: 1 }}>
                  <CardContent>
                    <Box sx={{ color: 'primary.main', mb: 1, display: 'flex', justifyContent: 'center' }}>
                      <Icon sx={{ fontSize: 32 }} />
                    </Box>
                    <Typography variant="subtitle2" align="center" fontWeight={600}>
                      {card.label}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" align="center" display="block">
                      {card.sub}
                    </Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Box>
          )
        })}
      </Stack>

      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1} sx={{ mb: 2 }}>
            <Typography variant="h6" component="h2">
              内容数据趋势
            </Typography>
            <Stack direction="row" spacing={1}>
              {TREND_OPTIONS.map((o) => (
                <Chip
                  key={o.days}
                  label={o.label}
                  size="small"
                  onClick={() => setDays(o.days)}
                  color={days === o.days ? 'primary' : 'default'}
                  variant={days === o.days ? 'filled' : 'outlined'}
                />
              ))}
            </Stack>
          </Stack>
          {loadingTrend ? (
            <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />
          ) : (
            <ReactECharts option={chartOption} style={{ height: 300 }} />
          )}
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6" component="h2">
              近期项目
            </Typography>
            <Button size="small" onClick={() => navigate(shortvideoRoutes.projects)}>
              查看全部
            </Button>
          </Stack>
          <Divider sx={{ mb: 2 }} />
          {loadingProjects ? (
            <CircularProgress sx={{ display: 'block', mx: 'auto', my: 2 }} />
          ) : (
            <Stack divider={<Divider flexItem />} spacing={0}>
              {projects.map((p, i) => (
                <Stack
                  key={i}
                  direction="row"
                  justifyContent="space-between"
                  alignItems="center"
                  spacing={2}
                  sx={{ py: 1.5 }}
                >
                  <Stack direction="row" alignItems="center" spacing={1.5} sx={{ minWidth: 0, flex: 1 }}>
                    <VideoIcon fontSize="small" color="action" />
                    <Typography variant="body2" noWrap title={String(p.title ?? '')}>
                      {String(p.title ?? `项目${i + 1}`)}
                    </Typography>
                  </Stack>
                  <Chip size="small" label={String(p.status ?? 'draft')} variant="outlined" />
                </Stack>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}
