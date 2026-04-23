import { useCallback } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import {
  Box, Button, Typography, Card, CardContent, Grid,
  Chip, Stack, LinearProgress, Divider, CircularProgress,
} from '@mui/material'
import {
  Description as ScriptIcon, ViewColumn as ShotListIcon,
  Image as PrepareIcon, Videocam as MaterialIcon,
  Movie as EditIcon, Publish as PublishIcon,
  CheckCircle as DoneIcon, RadioButtonUnchecked as TodoIcon,
  ArrowForward as NextIcon,
} from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

const FLOW_STEPS = [
  { id: 'script', label: '脚本策划', icon: <ScriptIcon />, path: shortvideoRoutes.scriptPlanning },
  { id: 'shot-list', label: '分镜设计', icon: <ShotListIcon />, path: shortvideoRoutes.shotList },
  { id: 'prepare', label: '素材准备', icon: <PrepareIcon />, path: shortvideoRoutes.materialPrepare },
  { id: 'material', label: '素材生产', icon: <MaterialIcon />, path: shortvideoRoutes.materialProduction },
  { id: 'edit', label: '视频剪辑', icon: <EditIcon />, path: shortvideoRoutes.editing },
  { id: 'publish', label: '审核发布', icon: <PublishIcon />, path: shortvideoRoutes.publish },
]

export default function SvProjectWorkbenchPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null

  const { data: project, isLoading } = useQuery({
    queryKey: ['sv-project', projectId],
    queryFn: () => shortvideoApi.get(projectId!),
    enabled: !!projectId,
  })

  const p = project ? {
    title: project.title,
    subtitle: project.publishTitle ?? '',
    status: project.status,
    progress: 0,
  } : null

  const goToStep = useCallback((path: string) => {
    const url = projectId ? `${path}?projectId=${projectId}` : path
    navigate(url)
  }, [projectId, navigate])

  if (!projectId) {
    return (
      <Box sx={{ p: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">请从项目列表进入工作台</Typography>
        <Button sx={{ mt: 2 }} variant="outlined" component={Link} to={shortvideoRoutes.projects}>返回项目列表</Button>
      </Box>
    )
  }

  return (
    <Box>
      <PageHeader
        title={p ? String(p.title ?? `项目 #${projectId}`) : `项目 #${projectId}`}
        breadcrumbs={[{ label: '短视频' }, { label: '项目列表', href: shortvideoRoutes.projects }, { label: '工作台' }]}
        subtitle={p ? String(p.subtitle ?? '') : ''}
      />

      {isLoading && <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />}

      {/* 进度总览 */}
      {!!p && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle1" fontWeight={600}>项目进度</Typography>
              <Chip size="small" label={String(p.status ?? 'draft')} />
            </Box>
            <LinearProgress
              variant="determinate"
              value={Number(p.progress ?? 0)}
              sx={{ height: 8, borderRadius: 4 }}
            />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
              {Number(p.progress ?? 0)}% 完成
            </Typography>
          </CardContent>
        </Card>
      )}

      {/* 流程步骤 */}
      <Grid container spacing={2}>
        {FLOW_STEPS.map((step, idx) => {
          const done = false // 实际可根据 p.completedSteps 判断
          return (
            <Grid item xs={12} sm={6} md={4} key={step.id}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <Box sx={{ color: done ? 'success.main' : 'primary.main' }}>{step.icon}</Box>
                    <Typography variant="subtitle2" fontWeight={600}>{step.label}</Typography>
                    {done ? <DoneIcon color="success" fontSize="small" /> : <TodoIcon color="disabled" fontSize="small" />}
                  </Stack>
                  <Divider sx={{ mb: 1 }} />
                  <Typography variant="caption" color="text.secondary">步骤 {idx + 1} / {FLOW_STEPS.length}</Typography>
                </CardContent>
                <Box sx={{ px: 2, pb: 2 }}>
                  <Button
                    fullWidth variant={idx === 0 ? 'contained' : 'outlined'}
                    endIcon={<NextIcon />}
                    onClick={() => goToStep(step.path)}
                    size="small"
                  >
                    进入{step.label}
                  </Button>
                </Box>
              </Card>
            </Grid>
          )
        })}
      </Grid>
    </Box>
  )
}
