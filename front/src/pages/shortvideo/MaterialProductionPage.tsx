import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Box, Typography, Button, Chip, LinearProgress, Alert, Stack, CircularProgress, Card, CardContent, Grid,
} from '@mui/material'
import MovieFilterIcon from '@mui/icons-material/MovieFilter'
import ImageIcon from '@mui/icons-material/Image'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { shortvideoApi, shotsToImg2VideoKeyframes } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { useGaifanEntitlementGate } from '@/hooks/useGaifanEntitlementGate'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'
import type { GridColDef } from '@mui/x-data-grid'
import type { VideoGenerationTaskRow } from '@/types/shortvideo'

const TASK_STATUS_COLORS: Record<string, 'default' | 'primary' | 'success' | 'error' | 'warning'> = {
  pending: 'default', running: 'primary', completed: 'success', failed: 'error',
}
const PROJECT_GET_ENDPOINT = '/short-video/project/get'
const SHOT_LIST_GET_ENDPOINT = '/short-video/shot-list/get'
const SHOT_LIST_BY_SCRIPT_ENDPOINT = '/short-video/shot-list/get-by-script'
const KEYFRAME_GENERATE_ENDPOINT = '/short-video/material/generate-keyframes'
const VIDEO_TASK_SUBMIT_ENDPOINT = '/short-video/video-task/submit'
const VIDEO_TASK_LIST_ENDPOINT = '/short-video/video-task/list'
const VIDEO_TASK_CANCEL_ENDPOINT = '/short-video/video-task/cancel'
const PRODUCTION_READY_ENDPOINTS = [
  PROJECT_GET_ENDPOINT,
  SHOT_LIST_GET_ENDPOINT,
  SHOT_LIST_BY_SCRIPT_ENDPOINT,
  KEYFRAME_GENERATE_ENDPOINT,
  VIDEO_TASK_SUBMIT_ENDPOINT,
  VIDEO_TASK_LIST_ENDPOINT,
  VIDEO_TASK_CANCEL_ENDPOINT,
].join('|')
const PRODUCTION_READY_ROUTES = [
  shortvideoRoutes.materialProduction,
  `${shortvideoRoutes.materialProduction}?projectId=:id`,
  `${shortvideoRoutes.materialPrepare}?projectId=:id`,
  `${shortvideoRoutes.editing}?projectId=:id`,
  `${shortvideoRoutes.workbench}?projectId=:id`,
].join('|')
const PRODUCTION_SUPPORTED_ACTIONS = [
  'refresh-project-production-state',
  'generate-keyframes',
  'submit-img2video-tasks',
  'cancel-video-task',
  'navigate-video-editing',
].join('|')
const PRODUCTION_UNSUPPORTED_ENDPOINTS = [
  '/short-video/project/mock',
  '/short-video/project/local-get',
  '/short-video/project/local-save',
  '/short-video/shot-list/mock',
  '/short-video/shot-list/local-get',
  '/short-video/shot-list/local-generate',
  '/short-video/shot-list/local-save',
  '/short-video/material/local-preview',
  '/short-video/material/mock-keyframes',
  '/short-video/material/local-keyframes',
  '/short-video/video-task/mock',
  '/short-video/video-task/local-submit',
  '/short-video/video-task/local-list',
  '/short-video/video-task/local-cancel',
  '/short-video/video-task/local-retry',
].join('|')

export default function MaterialProductionPage() {
  const toast = useToast()
  const gate = useGaifanEntitlementGate()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null
  const [operationError, setOperationError] = useState('')

  const {
    data: project,
    isLoading: projectLoading,
    isError: projectIsError,
    error: projectError,
    refetch: refetchProject,
  } = useQuery({
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

  const {
    data: shotList,
    isLoading: shotListLoading,
    isError: shotListError,
    error: shotListErrorDetail,
    refetch: refetchShotList,
  } = useQuery({
    queryKey: ['sv-shot-list', shotListSource],
    queryFn: () => {
      if (shotListSource?.kind === 'id') return shortvideoApi.shotListGet(shotListSource.shotListId)
      if (shotListSource?.kind === 'script') return shortvideoApi.shotListGetByScript(shotListSource.scriptId)
      throw new Error('no shot list source')
    },
    enabled: !!projectId && shotListSource != null,
  })

  const keyframes = shotsToImg2VideoKeyframes(shotList?.shots ?? [])
  const totalShots = shotList?.shots?.length ?? 0
  const shotsWithVideo = (shotList?.shots ?? []).filter((shot) => !!shot.videoUrl).length
  const missingKeyframes = Math.max(totalShots - keyframes.length, 0)

  const submitMutation = useMutation({
    mutationFn: () =>
      shortvideoApi.videoTaskSubmit({
        projectId: projectId!,
        shotListId: shotList != null && shotList.id > 0 ? shotList.id : undefined,
        keyframes,
      }),
    onSuccess: (res) => {
      toast(`已提交图生视频任务，taskId=${res.taskId}`, 'success')
      qc.invalidateQueries({ queryKey: ['sv-video-tasks', projectId] })
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setOperationError(`图生视频任务提交失败（POST ${VIDEO_TASK_SUBMIT_ENDPOINT}）：${message}。失败时不会创建本地假任务，当前分镜和 keyframes 仍保留。`)
      toast(message, 'error')
    },
  })

  const keyframeMutation = useMutation({
    mutationFn: () =>
      shortvideoApi.materialGenerateKeyframes({
        projectId: projectId!,
        shotListId: shotList != null && shotList.id > 0 ? shotList.id : undefined,
        characterReferenceUrl: project?.characterReferenceUrl,
        sceneReferenceUrl: project?.sceneReferenceUrl,
        shots: (shotList?.shots ?? []).map((shot) => ({
          shotId: shot.id,
          shotNumber: shot.shotNumber,
          sceneDescription: shot.sceneDescription,
          style: shot.mood ?? shot.cameraType,
        })),
      }),
    onSuccess: async () => {
      toast('关键帧已生成', 'success')
      await qc.invalidateQueries({ queryKey: ['sv-shot-list', shotListSource] })
      await qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
    },
    onError: (e: Error) => {
      const message = getErrorMessage(e)
      setOperationError(`关键帧生成失败（POST ${KEYFRAME_GENERATE_ENDPOINT}）：${message}。请检查图像供应商、参考图和分镜输入；页面不会写入占位关键帧。`)
      toast(message, 'error')
    },
  })

  const { data: taskResult, isLoading, isError: taskListIsError, error: taskListError, refetch: refetchTasks } = useQuery({
    queryKey: ['sv-video-tasks', projectId],
    queryFn: () => shortvideoApi.videoTaskList({ projectId: projectId ?? undefined, page: 0, rows: 20 }),
    refetchInterval: 10000,
  })

  const tasks = (taskResult?.list ?? []).map((t, i) => ({ ...t, id: (t.id as number) ?? i }))
  const taskSummary = tasks.reduce(
    (acc, task: VideoGenerationTaskRow) => {
      const status = String(task.status ?? '')
      if (status === 'completed') acc.completed += 1
      else if (status === 'failed') acc.failed += 1
      else if (status === 'running' || status === 'pending') acc.running += 1
      return acc
    },
    { completed: 0, failed: 0, running: 0 },
  )

  const handleCancel = async (taskId: number) => {
    try {
      await shortvideoApi.videoTaskCancel(taskId)
      toast('已取消', 'success')
      qc.invalidateQueries({ queryKey: ['sv-video-tasks', projectId] })
    } catch (e) {
      const message = getErrorMessage(e)
      setOperationError(`取消图生视频任务失败（POST ${VIDEO_TASK_CANCEL_ENDPOINT}）：${message}。失败时保留任务原状态。`)
      toast(message, 'error')
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'taskType', headerName: '类型', width: 120 },
    { field: 'errorMessage', headerName: '失败原因', flex: 1, minWidth: 180, renderCell: ({ value }) => String(value ?? '-') },
    {
      field: 'status', headerName: '状态', width: 120,
      renderCell: ({ value }) => (
        <Chip size="small" color={TASK_STATUS_COLORS[String(value)] ?? 'default'} label={String(value ?? '')} />
      ),
    },
    {
      field: 'progress', headerName: '进度', width: 160,
      renderCell: ({ value }) => (
        <Box sx={{ width: '100%' }}>
          <LinearProgress variant="determinate" value={Number(value ?? 0)} />
          <Typography variant="caption">{Number(value ?? 0)}%</Typography>
        </Box>
      ),
    },
    { field: 'createTime', headerName: '提交时间', width: 160, renderCell: ({ value }) => String(value ?? '').slice(0, 16) },
    {
      field: '_actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          {(String(row.status) === 'running' || String(row.status) === 'pending') && (
            <Button size="small" color="error" onClick={() => handleCancel(row.id as number)}>取消</Button>
          )}
          {String(row.status) === 'completed' && row.outputUrl && (
            <Button size="small" color="success" href={String(row.outputUrl)} target="_blank" rel="noreferrer">查看</Button>
          )}
        </Stack>
      ),
    },
  ]

  const canSubmit =
    !!projectId &&
    shotList != null &&
    keyframes.length > 0 &&
    !submitMutation.isPending

  const canGenerateKeyframes =
    !!projectId &&
    shotList != null &&
    totalShots > 0 &&
    !keyframeMutation.isPending

  const shotListBlockedReason =
    projectId && project && !projectLoading && shotListSource == null
      ? '项目未关联 scriptId / shotListId，请先在项目或脚本侧生成分镜列表'
      : null

  const submitBlockedReason = (() => {
    if (!projectId) return '请从项目工作台进入素材生产页。'
    if (!shotList) return '暂无可用分镜，需先生成分镜列表。'
    if (totalShots === 0) return '分镜列表为空，无法生成素材。'
    if (keyframes.length === 0) return '还没有关键帧 URL，请先生成关键帧。'
    return null
  })()

  return (
    <Box
      data-testid="material-production-page"
      data-ready-endpoints={PRODUCTION_READY_ENDPOINTS}
      data-ready-routes={PRODUCTION_READY_ROUTES}
      data-supported-actions={PRODUCTION_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={PRODUCTION_UNSUPPORTED_ENDPOINTS}
      data-no-local-task-fallback="true"
      data-no-local-keyframe-fallback="true"
      data-no-local-project-fallback="true"
    >
      <PageHeader
        title="素材生产"
        breadcrumbs={[{ label: '短视频' }, { label: '素材生产' }]}
        subtitle={projectId ? `项目 #${projectId} · POST ${PROJECT_GET_ENDPOINT}` : undefined}
        actions={projectId ? (
          <Button
            variant="outlined"
            size="small"
            onClick={() => {
              refetchProject()
              refetchShotList()
              refetchTasks()
            }}
            data-testid="material-production-refresh-button"
            data-source-endpoint={PROJECT_GET_ENDPOINT}
          >
            刷新
          </Button>
        ) : undefined}
      />

      {!projectId && <Alert severity="warning" data-testid="material-production-no-project" data-no-local-project-fallback="true" sx={{ mb: 2 }}>请从项目工作台进入，以关联项目</Alert>}
      {projectIsError && (
        <Alert
          severity="error"
          data-testid="material-production-project-error"
          data-no-local-project-fallback="true"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetchProject()}>重试</Button>}
        >
          项目加载失败（POST {PROJECT_GET_ENDPOINT}）：{getErrorMessage(projectError)}。分镜和素材生产状态可能不完整，页面不会补默认项目。
        </Alert>
      )}

      {!!projectId && (
        <Grid container spacing={2} sx={{ mb: 2 }} data-testid="material-production-summary" data-no-local-keyframe-fallback="true" data-no-local-task-fallback="true">
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined" data-testid="material-production-summary-card" data-source-endpoint={PROJECT_GET_ENDPOINT} data-no-local-project-fallback="true">
              <CardContent>
                <Typography variant="caption" color="text.secondary">分镜总数</Typography>
                <Typography variant="h5" fontWeight={700}>{totalShots}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">可提交关键帧</Typography>
                <Typography variant="h5" fontWeight={700}>{keyframes.length}</Typography>
                {missingKeyframes > 0 && <Typography variant="caption" color="warning.main">缺 {missingKeyframes} 条</Typography>}
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">已有视频片段</Typography>
                <Typography variant="h5" fontWeight={700}>{shotsWithVideo}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">任务状态</Typography>
                <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap sx={{ mt: 0.5 }}>
                  <Chip size="small" label={`进行中 ${taskSummary.running}`} color={taskSummary.running > 0 ? 'primary' : 'default'} />
                  <Chip size="small" label={`完成 ${taskSummary.completed}`} color={taskSummary.completed > 0 ? 'success' : 'default'} />
                  <Chip size="small" label={`失败 ${taskSummary.failed}`} color={taskSummary.failed > 0 ? 'error' : 'default'} />
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {!!projectId && (
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" useFlexGap data-testid="material-production-actions" data-no-local-keyframe-fallback="true" data-no-local-task-fallback="true">
          <Button
            variant="outlined"
            color="primary"
            startIcon={keyframeMutation.isPending ? <CircularProgress size={18} /> : <ImageIcon />}
            disabled={!canGenerateKeyframes}
            onClick={async () => {
              setOperationError('')
              if (!(await gate('shortvideo-maker', 'shortvideo-maker.script.generate'))) return
              keyframeMutation.mutate()
            }}
            data-testid="material-production-generate-keyframes-button"
            data-source-endpoint={KEYFRAME_GENERATE_ENDPOINT}
          >
            生成关键帧
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={submitMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <MovieFilterIcon />}
            disabled={!canSubmit}
            onClick={async () => {
              setOperationError('')
              if (!(await gate('shortvideo-maker', 'shortvideo-maker.script.generate'))) return
              submitMutation.mutate()
            }}
            data-testid="material-production-submit-video-button"
            data-source-endpoint={VIDEO_TASK_SUBMIT_ENDPOINT}
          >
            一键提交图生视频（keyframes）
          </Button>
          {(projectLoading || shotListLoading) && <CircularProgress size={24} />}
          <Typography variant="body2" color="text.secondary">
            {shotList != null && (
              <>分镜共 {totalShots} 条，已有关键帧可提交 {keyframes.length} 条</>
            )}
            {shotList == null && !projectLoading && !shotListLoading && shotListSource != null && '加载分镜中或暂无数据…'}
          </Typography>
        </Stack>
      )}

      {shotListBlockedReason && (
        <Alert severity="warning" data-testid="material-production-shotlist-blocked" data-no-local-project-fallback="true" sx={{ mb: 2 }}>{shotListBlockedReason}</Alert>
      )}

      {shotListError && (
        <Alert severity="error" data-testid="material-production-shotlist-error" data-no-local-keyframe-fallback="true" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => refetchShotList()}>重试</Button>}>
          分镜加载失败（POST {shotListSource?.kind === 'script' ? SHOT_LIST_BY_SCRIPT_ENDPOINT : SHOT_LIST_GET_ENDPOINT}）：{getErrorMessage(shotListErrorDetail)}。请确认已生成分镜列表。
        </Alert>
      )}

      {taskListIsError && (
        <Alert severity="error" data-testid="material-production-tasklist-error" data-no-local-task-fallback="true" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => refetchTasks()}>重试</Button>}>
          图生视频任务列表加载失败（POST {VIDEO_TASK_LIST_ENDPOINT}）：{getErrorMessage(taskListError)}。页面不会补静态任务。
        </Alert>
      )}

      <Alert severity="info" data-testid="material-production-boundary-contract" data-source-endpoints={`${KEYFRAME_GENERATE_ENDPOINT}|${VIDEO_TASK_SUBMIT_ENDPOINT}|${VIDEO_TASK_LIST_ENDPOINT}|${VIDEO_TASK_CANCEL_ENDPOINT}`} data-no-local-keyframe-fallback="true" data-no-local-task-fallback="true" data-supported-actions={PRODUCTION_SUPPORTED_ACTIONS} sx={{ mb: 2 }}>
        根据项目关联的 shot-list 自动组装 <code>keyframes</code>（<code>keyframeUrl</code> → <code>imageUrl</code>）。
        无关键帧 URL 的镜头不会提交。
      </Alert>

      {operationError && (
        <Alert severity="error" data-testid="material-production-operation-error" data-no-local-keyframe-fallback="true" data-no-local-task-fallback="true" data-input-retained="true" sx={{ mb: 2 }} onClose={() => setOperationError('')}>
          {operationError}
        </Alert>
      )}

      {submitBlockedReason && (
        <Alert severity="warning" data-testid="material-production-submit-blocked" data-no-local-task-fallback="true" sx={{ mb: 2 }}>
          {submitBlockedReason}
        </Alert>
      )}

      {taskSummary.failed > 0 && (
        <Alert severity="error" data-testid="material-production-task-failed-summary" data-no-local-task-fallback="true" sx={{ mb: 2 }}>
          存在失败的视频生成任务，请查看表格中的失败原因；通常与图生视频供应商、回调或素材 URL 可访问性有关。
        </Alert>
      )}

      <StandardDataGrid
        rows={tasks}
        columns={columns}
        loading={isLoading}
        data-testid="material-production-task-grid"
      />

      {!!projectId && (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
          <Button
            variant="outlined"
            onClick={() => navigate(`${shortvideoRoutes.editing}?projectId=${projectId}`)}
            data-testid="material-production-open-editing-button"
            data-target-route={`${shortvideoRoutes.editing}?projectId=${projectId}`}
          >
            去视频剪辑与成片
          </Button>
        </Box>
      )}
    </Box>
  )
}
