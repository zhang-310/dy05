import { useCallback } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { useLocation } from 'react-router-dom'
import {
  Box, Button, Typography, Card, CardContent, Grid,
  Chip, Stack, LinearProgress, Divider, CircularProgress, Alert,
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
import { getErrorMessage } from '@/utils/errorHandler'
import type { SvShotListVO } from '@/types/shortvideo'

const FLOW_STEPS = [
  { id: 'script', label: '脚本策划', icon: <ScriptIcon />, path: shortvideoRoutes.scriptPlanning, hint: '生成或关联短视频脚本' },
  { id: 'shot-list', label: '分镜设计', icon: <ShotListIcon />, path: shortvideoRoutes.shotList, hint: '按脚本生成可生产的镜头列表' },
  { id: 'prepare', label: '素材准备', icon: <PrepareIcon />, path: shortvideoRoutes.materialPrepare, hint: '上传角色/场景参考图' },
  { id: 'material', label: '素材生产', icon: <MaterialIcon />, path: shortvideoRoutes.materialProduction, hint: '生成关键帧与视频片段' },
  { id: 'edit', label: '视频剪辑', icon: <EditIcon />, path: shortvideoRoutes.editing, hint: '合成为 finalVideoUrl' },
  { id: 'publish', label: '审核发布', icon: <PublishIcon />, path: shortvideoRoutes.publish, hint: 'AI 审核并提交平台发布' },
]

const WORKBENCH_ENDPOINTS = {
  projectGet: '/short-video/project/get',
  shotListGet: '/short-video/shot-list/get',
  shotListGetByScript: '/short-video/shot-list/get-by-script',
} as const
const WORKBENCH_READY_ENDPOINTS = [
  WORKBENCH_ENDPOINTS.projectGet,
  WORKBENCH_ENDPOINTS.shotListGet,
  WORKBENCH_ENDPOINTS.shotListGetByScript,
] as const
const WORKBENCH_UNSUPPORTED_ENDPOINTS = [
  '/short-video/project/mock',
  '/short-video/project/local-get',
  '/short-video/workbench/static-progress',
  '/short-video/workbench/local-step-state',
  '/short-video/shot-list/local-get',
  '/short-video/shot-list/static-shots',
] as const
const WORKBENCH_READY_ROUTES = [
  shortvideoRoutes.projects,
  `${shortvideoRoutes.workbench}?projectId=:id`,
  `${shortvideoRoutes.scriptPlanning}?projectId=:id`,
  `${shortvideoRoutes.shotList}?projectId=:id`,
  `${shortvideoRoutes.materialPrepare}?projectId=:id`,
  `${shortvideoRoutes.materialProduction}?projectId=:id`,
  `${shortvideoRoutes.editing}?projectId=:id`,
  `${shortvideoRoutes.publish}?projectId=:id`,
].join('|')
const WORKBENCH_SUPPORTED_ACTIONS = [
  'refresh-project-status',
  'navigate-project-list',
  'navigate-script-planning',
  'navigate-shot-list',
  'navigate-material-prepare',
  'navigate-material-production',
  'navigate-video-editing',
  'navigate-publish',
  'open-final-video',
].join('|')

function inferWorkbenchScope(pathname: string): 'admin' | 'talent' {
  return pathname.startsWith('/talent') ? 'talent' : 'admin'
}

function workbenchScopeLabel(scope: 'admin' | 'talent') {
  return scope === 'talent' ? '达人端' : '管理员端'
}

function scopedProjectListPath(scope: 'admin' | 'talent') {
  return scope === 'talent' ? '/talent/shortvideo' : shortvideoRoutes.projects
}

function scopedStepPath(scope: 'admin' | 'talent', adminPath: string) {
  if (scope === 'admin') return adminPath
  return adminPath === shortvideoRoutes.workbench ? '/talent/shortvideo' : adminPath
}

function resolveProjectProgress(project?: Awaited<ReturnType<typeof shortvideoApi.get>>, shotList?: SvShotListVO) {
  if (!project) {
    return {
      progress: 0,
      completed: new Set<string>(),
      current: 'script',
      blockers: ['项目尚未加载，无法判断当前链路状态。'],
      hasReference: false,
      hasFinalVideo: false,
      published: false,
      shotCount: 0,
      keyframeCount: 0,
      videoClipCount: 0,
      materialPartial: false,
    }
  }
  const completed = new Set<string>()
  const shots = shotList?.shots ?? []
  const hasScript = project.scriptId != null && project.scriptId > 0
  const hasShotList = (project.shotListId != null && project.shotListId > 0) || (shotList?.id != null && shotList.id > 0) || shots.length > 0
  const hasAnyKeyframe = shots.some((shot) => !!shot.keyframeUrl)
  const hasAnyVideoClip = shots.some((shot) => !!shot.videoUrl)
  const hasReference = !!project.characterReferenceUrl || !!project.sceneReferenceUrl
  const hasFinalVideo = !!project.finalVideoUrl
  const published = project.reviewStatus === 'approved' || project.status === 'published' || project.status === 'completed'
  if (hasScript) completed.add('script')
  if (hasShotList) completed.add('shot-list')
  if (hasReference || hasAnyKeyframe || hasAnyVideoClip || hasFinalVideo) completed.add('prepare')
  if (hasAnyVideoClip || hasFinalVideo) completed.add('material')
  if (hasFinalVideo) {
    completed.add('edit')
    completed.add('material')
  }
  if (published) {
    completed.add('publish')
  }
  const current = FLOW_STEPS.find((step) => !completed.has(step.id))?.id ?? 'publish'
  const blockers: string[] = []
  if (!hasScript) blockers.push('缺少脚本：请先生成或关联脚本。')
  else if (!hasShotList) blockers.push('缺少分镜：请从脚本生成分镜列表。')
  else if (!hasAnyKeyframe) blockers.push('缺少关键帧：请进入素材生产生成关键帧。')
  else if (!hasAnyVideoClip && !hasFinalVideo) blockers.push('缺少视频片段：请提交图生视频任务并等待完成。')
  else if (!hasFinalVideo) blockers.push('缺少成片 URL：请进入视频剪辑生成或上传成片。')
  else if (!published) blockers.push('成片未发布：请执行 AI 审核并提交发布。')
  if (hasShotList && !hasReference && !hasAnyKeyframe) blockers.push('未上传角色/场景参考图，关键帧可生成但一致性会降低。')
  return {
    progress: Math.round((completed.size / FLOW_STEPS.length) * 100),
    completed,
    current,
    blockers,
    hasReference,
    hasFinalVideo,
    published,
    shotCount: shots.length,
    keyframeCount: shots.filter((shot) => !!shot.keyframeUrl).length,
    videoClipCount: shots.filter((shot) => !!shot.videoUrl).length,
    materialPartial: hasAnyKeyframe && !hasAnyVideoClip && !hasFinalVideo,
  }
}

export default function SvProjectWorkbenchPage() {
  const navigate = useNavigate()
  const routeScope = inferWorkbenchScope(useLocation().pathname)
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null

  const { data: project, isLoading, isError, error: projectError, refetch } = useQuery({
    queryKey: ['sv-project', projectId],
    queryFn: () => shortvideoApi.get(projectId!),
    enabled: !!projectId,
  })

  const shotListSource =
    project != null
      ? project.shotListId != null && project.shotListId > 0
        ? ({ kind: 'id' as const, shotListId: project.shotListId })
        : project.scriptId != null && project.scriptId > 0
          ? ({ kind: 'script' as const, scriptId: project.scriptId })
          : null
      : null

  const { data: shotList, isError: shotListError, error: shotListErrorDetail, refetch: refetchShotList } = useQuery({
    queryKey: ['sv-shot-list', shotListSource],
    queryFn: () => {
      if (shotListSource?.kind === 'id') return shortvideoApi.shotListGet(shotListSource.shotListId)
      if (shotListSource?.kind === 'script') return shortvideoApi.shotListGetByScript(shotListSource.scriptId)
      throw new Error('no shot list source')
    },
    enabled: shotListSource != null,
  })

  const flowState = resolveProjectProgress(project, shotList)

  const p = project ? {
    title: project.title,
    subtitle: project.publishTitle ?? '',
    status: project.status,
    progress: flowState.progress,
  } : null

  const goToStep = useCallback((path: string) => {
    const scopedPath = scopedStepPath(routeScope, path)
    const url = projectId ? `${scopedPath}?projectId=${projectId}` : scopedPath
    navigate(url)
  }, [projectId, navigate, routeScope])

  if (!projectId) {
    return (
      <Box
        data-testid="sv-project-workbench-page"
        data-ready-endpoints={WORKBENCH_READY_ENDPOINTS.join('|')}
        data-ready-routes={WORKBENCH_READY_ROUTES}
        data-supported-actions={WORKBENCH_SUPPORTED_ACTIONS}
        data-unsupported-endpoints={WORKBENCH_UNSUPPORTED_ENDPOINTS.join('|')}
        data-no-local-project-fallback="true"
        data-no-static-progress="true"
        sx={{ p: 4, textAlign: 'center' }}
      >
        <Typography color="text.secondary">请从项目列表进入工作台</Typography>
        <Button
          sx={{ mt: 2 }}
          variant="outlined"
          component={Link}
          to={scopedProjectListPath(routeScope)}
          data-testid="sv-workbench-back-to-projects-button"
          data-target-route={scopedProjectListPath(routeScope)}
        >
          返回项目列表
        </Button>
      </Box>
    )
  }

  return (
    <Box
      data-testid="sv-project-workbench-page"
      data-ready-endpoints={WORKBENCH_READY_ENDPOINTS.join('|')}
      data-ready-routes={WORKBENCH_READY_ROUTES}
      data-supported-actions={WORKBENCH_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={WORKBENCH_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-project-fallback="true"
      data-no-static-progress="true"
    >
      <PageHeader
        title={p ? String(p.title ?? `项目 #${projectId}`) : `项目 #${projectId}`}
        breadcrumbs={[
          { label: routeScope === 'talent' ? '达人短视频' : '短视频' },
          { label: '项目列表', href: scopedProjectListPath(routeScope) },
          { label: '工作台' },
        ]}
        subtitle={p ? String(p.subtitle ?? '') : ''}
        actions={
          <Button
            variant="outlined"
            onClick={() => refetch()}
            data-testid="sv-workbench-refresh-button"
            data-source-endpoint={WORKBENCH_ENDPOINTS.projectGet}
          >
            刷新状态
          </Button>
        }
      />

      {routeScope === 'talent' && (
        <Alert data-testid="sv-workbench-talent-shell-contract" severity="info" variant="outlined" sx={{ mb: 2 }}>
          当前为 {workbenchScopeLabel(routeScope)} 项目工作台。项目状态、分镜和成片状态仍读取真实短视频接口；素材生产、剪辑、发布等链路按钮会留在达人短视频目录内，避免跨回管理员目录。
        </Alert>
      )}

      {isLoading && <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />}

      {isError && (
        <Alert
          data-testid="sv-workbench-project-error"
          data-no-local-project-fallback="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          sx={{ mb: 2 }}
        >
          项目加载失败（POST {WORKBENCH_ENDPOINTS.projectGet}）：{getErrorMessage(projectError)}。请确认项目存在且当前账号有权限；页面不会补造项目进度。
        </Alert>
      )}

      {shotListError && (
        <Alert
          data-testid="sv-workbench-shot-list-error"
          data-no-static-progress="true"
          severity="warning"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetchShotList()}>重试</Button>}
        >
          分镜详情加载失败（POST {shotListSource?.kind === 'id' ? WORKBENCH_ENDPOINTS.shotListGet : WORKBENCH_ENDPOINTS.shotListGetByScript}）：{getErrorMessage(shotListErrorDetail)}。页面仍会展示项目基础状态，但关键帧/视频片段统计可能不完整，不会补静态分镜。
        </Alert>
      )}

      {!!project && shotListSource == null && (
        <Alert data-testid="sv-workbench-no-shot-source" data-no-local-shot-list-fallback="true" severity="info" sx={{ mb: 2 }}>
          项目尚未关联脚本或分镜。请从脚本策划开始，保存脚本后再生成分镜。
        </Alert>
      )}

      {/* 进度总览 */}
      {!!p && (
        <Card data-testid="sv-workbench-progress-summary" data-no-static-progress="true" sx={{ mb: 3 }}>
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
            <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap" useFlexGap>
              <Chip size="small" variant="outlined" label={`脚本 ${project?.scriptId ? 1 : 0}`} />
              <Chip size="small" variant="outlined" label={`分镜 ${flowState.shotCount ?? 0}`} />
              <Chip
                size="small"
                variant="outlined"
                color={flowState.hasReference ? 'success' : 'default'}
                label={flowState.hasReference ? '参考图已准备' : '参考图未上传'}
              />
              <Chip size="small" variant="outlined" label={`关键帧 ${flowState.keyframeCount ?? 0}`} />
              <Chip
                size="small"
                variant="outlined"
                color={(flowState.videoClipCount ?? 0) > 0 ? 'success' : 'default'}
                label={`视频片段 ${flowState.videoClipCount ?? 0}`}
              />
              {flowState.materialPartial && <Chip size="small" color="warning" label="素材待成片" />}
              <Chip
                size="small"
                color={flowState.hasFinalVideo ? 'success' : 'default'}
                variant={flowState.hasFinalVideo ? 'filled' : 'outlined'}
                label={flowState.hasFinalVideo ? '成片已就绪' : '成片未生成'}
              />
            </Stack>
          </CardContent>
        </Card>
      )}

      {!!p && flowState.blockers.length > 0 && (
        <Alert data-testid="sv-workbench-blockers" severity={flowState.hasFinalVideo ? 'info' : 'warning'} sx={{ mb: 3 }}>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>下一步诊断</Typography>
          <Stack spacing={0.5}>
            {flowState.blockers.map((item) => (
              <Typography key={item} variant="body2">{item}</Typography>
            ))}
          </Stack>
        </Alert>
      )}

      {project?.finalVideoUrl && (
        <Card data-testid="sv-workbench-final-video-card" sx={{ mb: 3 }} variant="outlined">
          <CardContent>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'stretch', sm: 'center' }}>
              <Box sx={{ flex: 1 }}>
                <Typography variant="subtitle1" fontWeight={600}>成片已生成</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                  {project.finalVideoUrl}
                </Typography>
              </Box>
              <Button
                variant="outlined"
                href={project.finalVideoUrl}
                target="_blank"
                rel="noreferrer"
                data-testid="sv-workbench-open-final-video-button"
              >
                查看成片
              </Button>
              <Button
                variant="contained"
                onClick={() => goToStep(shortvideoRoutes.publish)}
                data-testid="sv-workbench-go-publish-button"
                data-target-route={`${scopedStepPath(routeScope, shortvideoRoutes.publish)}?projectId=${projectId}`}
              >
                去发布
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* 流程步骤 */}
      <Grid container spacing={2}>
        {FLOW_STEPS.map((step, idx) => {
          const done = flowState.completed.has(step.id)
          const active = flowState.current === step.id
          return (
            <Grid item xs={12} sm={6} md={4} key={step.id}>
              <Card
                data-testid="sv-workbench-flow-step-card"
                data-step-id={step.id}
                data-step-state={done ? 'done' : active ? 'active' : 'todo'}
                variant="outlined"
                sx={{ height: '100%', borderColor: active ? 'primary.main' : 'divider' }}
              >
                <CardContent>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <Box sx={{ color: done ? 'success.main' : active ? 'primary.main' : 'text.secondary' }}>{step.icon}</Box>
                    <Typography variant="subtitle2" fontWeight={600}>{step.label}</Typography>
                    {done ? <DoneIcon color="success" fontSize="small" /> : <TodoIcon color="disabled" fontSize="small" />}
                  </Stack>
                  <Divider sx={{ mb: 1 }} />
                  <Typography variant="caption" color="text.secondary">
                    {done ? '已完成' : active ? '当前步骤' : `步骤 ${idx + 1} / ${FLOW_STEPS.length}`}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {step.hint}
                  </Typography>
                </CardContent>
                <Box sx={{ px: 2, pb: 2 }}>
                  <Button
                    fullWidth variant={active ? 'contained' : 'outlined'}
                    endIcon={<NextIcon />}
                    onClick={() => goToStep(step.path)}
                    size="small"
                    data-testid="sv-workbench-flow-step-button"
                    data-step-id={step.id}
                    data-target-route={`${scopedStepPath(routeScope, step.path)}?projectId=${projectId}`}
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
