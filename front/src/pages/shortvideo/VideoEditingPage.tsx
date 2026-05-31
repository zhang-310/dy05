import { useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Box, Button, Stack, Typography, Chip, LinearProgress, TextField, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions, FormControlLabel, Switch, Divider, Alert, Card, CardContent } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import MovieIcon from '@mui/icons-material/Movie'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shortvideoApi, autoCompose } from '@/api/shortvideo'
import { useGaifanEntitlementGate } from '@/hooks/useGaifanEntitlementGate'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { DataGridEmptyOverlay, PageHeader, StandardDataGrid } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import type { GridColDef } from '@mui/x-data-grid'
import type { AutoComposeResult } from '@/types/shortvideo'
import { getErrorMessage } from '@/utils/errorHandler'

const STATUS_MAP: Record<string, { label: string; color: 'default' | 'info' | 'warning' | 'success' | 'error' }> = {
  pending: { label: '待处理', color: 'default' },
  running: { label: '处理中', color: 'info' },
  completed: { label: '已完成', color: 'success' },
  failed: { label: '失败', color: 'error' },
}
const PROJECT_GET_ENDPOINT = '/short-video/project/get'
const VIDEO_TASK_LIST_ENDPOINT = '/short-video/video-task/list'
const VIDEO_TASK_CANCEL_ENDPOINT = '/short-video/video-task/cancel'
const VIDEO_TASK_RETRY_ENDPOINT = '/short-video/video-task/retry'
const AUTO_COMPOSE_ENDPOINT = '/short-video/edit/auto-compose'
const DIGITAL_HUMAN_COMMERCE_START_ENDPOINT = '/short-video/workflow/digital-human-commerce/start'
const WORKFLOW_STATUS_ENDPOINT = '/short-video/workflow/status'
const VIDEO_EDITING_READY_ENDPOINTS = [
  PROJECT_GET_ENDPOINT,
  VIDEO_TASK_LIST_ENDPOINT,
  VIDEO_TASK_CANCEL_ENDPOINT,
  VIDEO_TASK_RETRY_ENDPOINT,
  AUTO_COMPOSE_ENDPOINT,
  DIGITAL_HUMAN_COMMERCE_START_ENDPOINT,
  WORKFLOW_STATUS_ENDPOINT,
] as const
const VIDEO_EDITING_UNSUPPORTED_ENDPOINTS = [
  '/short-video/video-task/mock',
  '/short-video/video-task/local-list',
  '/short-video/video-task/local-cancel',
  '/short-video/video-task/local-retry',
  '/short-video/edit/local-auto-compose',
  '/short-video/edit/mock-final-video',
  '/short-video/workflow/local-digital-human-commerce',
  '/short-video/workflow/mock-status',
  '/short-video/edit/static-task',
  '/short-video/edit/subtitles/local-list',
] as const
const VIDEO_EDITING_READY_ROUTES = [
  shortvideoRoutes.editing,
  `${shortvideoRoutes.editing}?projectId=:id`,
  `${shortvideoRoutes.publish}?projectId=:id`,
  `${shortvideoRoutes.workbench}?projectId=:id`,
].join('|')
const VIDEO_EDITING_SUPPORTED_ACTIONS = [
  'refresh-video-tasks',
  'open-auto-compose-dialog',
  'submit-auto-compose',
  'start-digital-human-commerce-pipeline',
  'poll-workflow-status',
  'cancel-video-task',
  'retry-video-task',
  'open-final-video',
  'navigate-workbench',
  'navigate-publish',
].join('|')

export default function VideoEditingPage() {
  const toast = useToast()
  const gate = useGaifanEntitlementGate()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const urlProjectId = searchParams.get('projectId') ?? ''
  const entryMode = location.pathname === shortvideoRoutes.subtitles ? 'subtitles' : 'editing'
  const [page, setPage] = useState(0)
  const [composeOpen, setComposeOpen] = useState(false)
  const [composeForm, setComposeForm] = useState({ projectId: urlProjectId, duration: 60, style: 'standard', subtitleEnabled: true })
  const [composeResult, setComposeResult] = useState<AutoComposeResult | null>(null)
  const [workflowTaskId, setWorkflowTaskId] = useState('')
  const [taskActionError, setTaskActionError] = useState<string | null>(null)
  const parsedProjectId = composeForm.projectId.trim() ? Number(composeForm.projectId.trim()) : undefined
  const activeProjectId = parsedProjectId != null && Number.isFinite(parsedProjectId) && parsedProjectId > 0 ? parsedProjectId : undefined

  const { data: project, isError: projectIsError, error: projectError, refetch: refetchProject } = useQuery({
    queryKey: ['sv-project', activeProjectId],
    queryFn: () => shortvideoApi.get(activeProjectId!),
    enabled: activeProjectId != null,
  })

  const { data, isLoading, isError: tasksIsError, error: tasksError, refetch: refetchTasks } = useQuery({
    queryKey: ['video-tasks', activeProjectId, page],
    queryFn: () => shortvideoApi.videoTaskList({ projectId: activeProjectId, page, rows: 20 }),
    refetchInterval: 10000,
  })
  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const { data: workflowStatus, isError: workflowIsError, error: workflowError } = useQuery({
    queryKey: ['shortvideo-workflow-status', workflowTaskId],
    queryFn: () => shortvideoApi.workflowStatus(workflowTaskId),
    enabled: !!workflowTaskId,
    refetchInterval: (query) => {
      const status = String(query.state.data?.status ?? '')
      return status === 'completed' || status === 'failed' ? false : 3000
    },
  })

  const cancelMutation = useMutation({
    mutationFn: async (taskId: number): Promise<void> => { await shortvideoApi.videoTaskCancel(taskId) },
    onMutate: () => { setTaskActionError(null) },
    onSuccess: () => { toast('已取消', 'success'); qc.invalidateQueries({ queryKey: ['video-tasks'] }) },
    onError: (error, taskId) => {
      const message = getErrorMessage(error)
      setTaskActionError(`取消任务失败（POST ${VIDEO_TASK_CANCEL_ENDPOINT}）：${message}。任务 #${taskId} 会保留在列表中，避免误判已取消。`)
      toast(`取消失败：${message}`, 'error')
    },
  })
  const composeMutation = useMutation({
    mutationFn: async (): Promise<AutoComposeResult> => {
      return autoCompose({
        projectId: Number(composeForm.projectId),
        duration: composeForm.duration,
        style: composeForm.style,
        subtitleEnabled: composeForm.subtitleEnabled,
      })
    },
    onSuccess: (result) => {
      setComposeResult(result)
      toast(result.finalVideoUrl ? '自动合成完成，成片已回写项目' : '自动合成已完成，但未返回成片地址', 'success')
      qc.invalidateQueries({ queryKey: ['video-tasks'] })
      if (activeProjectId) qc.invalidateQueries({ queryKey: ['sv-project', activeProjectId] })
      setComposeOpen(false)
    },
    onError: (e) => {
      toast(`提交失败：${getErrorMessage(e)}`, 'error')
    },
  })
  const digitalHumanCommerceMutation = useMutation({
    mutationFn: async () => shortvideoApi.workflowDigitalHumanCommerceStart({
      projectId: Number(composeForm.projectId),
      duration: composeForm.duration,
      style: '数字人口播带货 产品细节展示',
      productInfo: `数字人口播带货，口播 + 产品细节展示，目标风格：${composeForm.style}`,
    }),
    onSuccess: (result) => {
      setWorkflowTaskId(result.taskId ?? '')
      toast('数字人口播带货成片流水线已启动', 'success')
      qc.invalidateQueries({ queryKey: ['sv-project'] })
      setComposeOpen(false)
    },
    onError: (e) => {
      toast(`启动失败：${getErrorMessage(e)}`, 'error')
    },
  })
  const startDigitalHumanCommerce = async () => {
    try {
      const ent = await checkGaifanEntitlement('digital-human', 'digital-human.generate')
      if (ent && ent.granted === false) {
        toast(commercialDenialMessage({ code: 4421, message: ent.reason ?? '无数字人生成权益' }), 'warning')
        return
      }
    } catch (e) {
      if (isCommercialDenial(e)) {
        toast(commercialDenialMessage(e), 'warning')
        return
      }
    }
    digitalHumanCommerceMutation.mutate()
  }
  const retryMutation = useMutation({
    mutationFn: async (taskId: number): Promise<void> => { await shortvideoApi.videoTaskRetry(taskId) },
    onMutate: () => { setTaskActionError(null) },
    onSuccess: () => { toast('已重试', 'success'); qc.invalidateQueries({ queryKey: ['video-tasks'] }) },
    onError: (error, taskId) => {
      const message = getErrorMessage(error)
      setTaskActionError(`重试任务失败（POST ${VIDEO_TASK_RETRY_ENDPOINT}）：${message}。任务 #${taskId} 会保留在列表中，避免伪造新任务。`)
      toast(`重试失败：${message}`, 'error')
    },
  })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'taskType', headerName: '任务类型', width: 130 },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const key = String(value ?? '')
        const s = STATUS_MAP[key] ?? { label: key || '未知', color: 'default' as const }
        return <Chip label={s.label} size="small" color={s.color} />
      } },
    { field: 'progress', headerName: '进度', width: 160,
      renderCell: ({ value }) => (
        <Box sx={{ width: '100%', display: 'flex', alignItems: 'center', gap: 1 }}>
          <LinearProgress variant="determinate" value={Number(value ?? 0)} sx={{ flex: 1, height: 6, borderRadius: 3 }} />
          <Typography variant="caption">{Number(value ?? 0)}%</Typography>
        </Box>
      ) },
    { field: 'priority', headerName: '优先级', width: 80 },
    { field: 'createTime', headerName: '创建时间', width: 160 },
    { field: '_actions', headerName: '操作', width: 160, sortable: false,
      renderCell: ({ row: r }) => {
        const status = String(r.status ?? '')
        return (
          <Stack direction="row" spacing={0.5}>
            {(status === 'running' || status === 'pending') && (
              <Button
                size="small"
                color="warning"
                onClick={() => cancelMutation.mutate(Number(r.id))}
                data-testid="video-editing-cancel-task-button"
                data-source-endpoint={VIDEO_TASK_CANCEL_ENDPOINT}
              >
                取消
              </Button>
            )}
            {status === 'failed' && (
              <Button
                size="small"
                color="primary"
                onClick={() => retryMutation.mutate(Number(r.id))}
                data-testid="video-editing-retry-task-button"
                data-source-endpoint={VIDEO_TASK_RETRY_ENDPOINT}
              >
                重试
              </Button>
            )}
            {status === 'completed' && r.outputUrl && (
              <Button
                size="small"
                color="success"
                href={String(r.outputUrl)}
                target="_blank"
                rel="noreferrer"
                data-testid="video-editing-open-task-output-button"
              >
                查看
              </Button>
            )}
          </Stack>
        )
      } },
  ]

  const finalVideoUrl = composeResult?.finalVideoUrl || project?.finalVideoUrl
  const workflowFinalVideoUrl = workflowStatus?.finalVideoUrl

  return (
    <Box
      data-testid="shortvideo-video-editing-page"
      data-entry-mode={entryMode}
      data-ready-endpoints={VIDEO_EDITING_READY_ENDPOINTS.join('|')}
      data-ready-routes={VIDEO_EDITING_READY_ROUTES}
      data-supported-actions={VIDEO_EDITING_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={VIDEO_EDITING_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-local-video-task-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 120px)' }}
    >
      <PageHeader
        title={entryMode === 'subtitles' ? '字幕编辑入口' : '视频剪辑与成片'}
        breadcrumbs={[{ label: '短视频' }, { label: entryMode === 'subtitles' ? '字幕编辑' : '视频剪辑' }]}
        subtitle={activeProjectId ? `项目 #${activeProjectId}` : '按项目查看图生视频任务，并将已有视频片段自动合成为最终成片。'}
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => {
              refetchTasks()
              if (activeProjectId) refetchProject()
            }}
            data-testid="video-editing-refresh-button"
            data-source-endpoint={VIDEO_TASK_LIST_ENDPOINT}
          >
            刷新
          </Button>
        }
      />
      <Alert data-testid="video-editing-contract-alert" severity="info">
        {entryMode === 'subtitles'
          ? '字幕编辑必须绑定真实视频 ID。本入口用于先定位项目与成片任务；进入具体成片或字幕详情后再打开字幕时间轴，避免全局导航跳到占位视频 ID。'
          : `异步图生视频任务走 POST ${VIDEO_TASK_LIST_ENDPOINT}|${VIDEO_TASK_CANCEL_ENDPOINT}|${VIDEO_TASK_RETRY_ENDPOINT}；自动合成走 POST ${AUTO_COMPOSE_ENDPOINT} 并读取项目分镜中的 videoUrl，若项目未关联分镜或视频片段为空，后端会返回 “videos 不能为空”。`}
      </Alert>
      {entryMode === 'subtitles' && (
        <Alert data-testid="video-editing-subtitle-entry-warning" data-no-local-subtitle-list="true" severity="warning">
          当前后端没有“字幕列表/按项目查字幕”的独立接口；页面不伪造字幕清单，只保留真实项目任务、成片回写和具体视频 ID 字幕编辑入口。
        </Alert>
      )}
      {!activeProjectId && (
        <Alert data-testid="video-editing-no-project-warning" severity="warning">请填写或从项目工作台带入项目 ID。未选择项目时，本页只能查看全局任务，无法自动回写成片。</Alert>
      )}
      {projectIsError && (
        <Alert
          data-testid="video-editing-project-error"
          data-input-retained="true"
          data-no-local-project-fallback="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetchProject()}>重试</Button>}
        >
          项目详情加载失败（POST {PROJECT_GET_ENDPOINT}）：{getErrorMessage(projectError)}。请检查项目权限，当前项目 ID 输入会保留。
        </Alert>
      )}
      {tasksIsError && (
        <Alert
          data-testid="video-editing-tasks-error"
          data-input-retained="true"
          data-no-local-video-task-fallback="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetchTasks()}>重试</Button>}
        >
          视频任务加载失败（POST {VIDEO_TASK_LIST_ENDPOINT}）：{getErrorMessage(tasksError)}。列表不会填充模拟任务，当前分页和项目筛选会保留。
        </Alert>
      )}
      {taskActionError && <Alert data-testid="video-editing-task-action-error" data-no-local-video-task-mutation="true" severity="error">{taskActionError}</Alert>}
      {project && !activeProjectId && (
        <Alert severity="warning">
          当前项目仅从 URL 读取，未填写项目 ID 时只显示全局任务。建议从项目工作台进入，避免自动合成写回错误项目。
        </Alert>
      )}
      {finalVideoUrl && (
        <Card data-testid="video-editing-final-video-card" variant="outlined">
          <CardContent>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'stretch', sm: 'center' }}>
              <MovieIcon color="success" />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="subtitle1" fontWeight={600}>成片已生成</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
                  {finalVideoUrl}
                </Typography>
              </Box>
              <Button variant="outlined" href={finalVideoUrl} target="_blank" rel="noreferrer">查看成片</Button>
              {activeProjectId && (
                <>
                  <Button
                    variant="outlined"
                    onClick={() => navigate(`${shortvideoRoutes.workbench}?projectId=${activeProjectId}`)}
                    data-testid="video-editing-back-workbench-button"
                    data-target-route={`${shortvideoRoutes.workbench}?projectId=${activeProjectId}`}
                  >
                    返回工作台
                  </Button>
                  <Button
                    variant="contained"
                    onClick={() => navigate(`${shortvideoRoutes.publish}?projectId=${activeProjectId}`)}
                    data-testid="video-editing-go-publish-button"
                    data-target-route={`${shortvideoRoutes.publish}?projectId=${activeProjectId}`}
                  >
                    去发布
                  </Button>
                </>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}
      {(workflowTaskId || workflowStatus) && (
        <Card
          data-testid="video-editing-digital-human-workflow-card"
          data-source-endpoints={`${DIGITAL_HUMAN_COMMERCE_START_ENDPOINT}|${WORKFLOW_STATUS_ENDPOINT}`}
          data-no-local-workflow-status="true"
          variant="outlined"
        >
          <CardContent>
            <Stack spacing={1.5}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <Typography variant="subtitle1" fontWeight={700}>数字人口播带货成片流水线</Typography>
                {workflowTaskId && <Chip size="small" label={workflowTaskId} variant="outlined" />}
                <Chip size="small" label={workflowStatus?.status ?? 'processing'} color={workflowStatus?.status === 'failed' ? 'error' : workflowStatus?.status === 'completed' ? 'success' : 'info'} />
                <Chip size="small" label={workflowStatus?.currentStep ?? 'pipeline:init'} />
              </Stack>
              <LinearProgress variant="determinate" value={Number(workflowStatus?.progress ?? 5)} />
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {workflowStatus?.scriptId && <Chip size="small" label={`脚本 #${workflowStatus.scriptId}`} />}
                {workflowStatus?.shotListId && <Chip size="small" label={`分镜 #${workflowStatus.shotListId}`} />}
                {workflowStatus?.productBrollKeyframeCount != null && <Chip size="small" label={`关键帧 ${workflowStatus.productBrollKeyframeCount}`} />}
                {workflowStatus?.productBrollVideoCount != null && <Chip size="small" label={`B-roll ${workflowStatus.productBrollVideoCount}`} />}
                {workflowStatus?.voiceClipCount != null && <Chip size="small" label={`配音 ${workflowStatus.voiceClipCount}`} />}
                {workflowStatus?.composeVideoCount != null && <Chip size="small" label={`合成素材 ${workflowStatus.composeVideoCount}`} />}
              </Stack>
              {workflowStatus?.digitalHumanSkipped && (
                <Alert severity="warning" variant="outlined">{workflowStatus.digitalHumanSkipReason || '数字人服务未配置，已跳过数字人口播片段。'}</Alert>
              )}
              {workflowStatus?.digitalHumanPendingUrl && (
                <Alert severity="info" variant="outlined">数字人供应商任务已提交：{workflowStatus.digitalHumanPendingUrl}。供应商回调完成后可重新合成。</Alert>
              )}
              {workflowStatus?.errorMessage && <Alert severity="error">{workflowStatus.errorMessage}</Alert>}
              {workflowIsError && <Alert severity="error">工作流状态查询失败（POST {WORKFLOW_STATUS_ENDPOINT}）：{getErrorMessage(workflowError)}</Alert>}
              {workflowFinalVideoUrl && (
                <Button variant="contained" href={workflowFinalVideoUrl} target="_blank" rel="noreferrer" sx={{ alignSelf: 'flex-start' }}>
                  查看数字人成片
                </Button>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}
      <Stack direction="row" spacing={2} justifyContent="flex-end">
        <Button
          variant="outlined"
          startIcon={<AutoAwesomeIcon />}
          onClick={() => setComposeOpen(true)}
          data-testid="video-editing-open-compose-button"
          data-source-endpoint={AUTO_COMPOSE_ENDPOINT}
        >
          AI自动合成
        </Button>
      </Stack>

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={m => setPage(m.page)}
        pageSizeOptions={[20]}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
      />
      {!isLoading && !tasksIsError && rows.length === 0 && (
        <Alert data-testid="video-editing-task-empty" data-no-static-task="true" severity="info">
          暂无视频生成任务。请先到素材生产页提交关键帧生成视频片段，再回到这里执行自动合成。
        </Alert>
      )}

      {/* AI 自动合成对话框 */}
      <Dialog open={composeOpen} onClose={() => setComposeOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Stack direction="row" alignItems="center" spacing={1}>
            <AutoAwesomeIcon color="primary" />
            <span>AI自动合成视频</span>
          </Stack>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="项目 ID" type="number" required
              value={composeForm.projectId}
              onChange={e => setComposeForm(p => ({ ...p, projectId: e.target.value }))}
              size="small" fullWidth helperText="将基于该项目的脚本和素材自动合成视频" />
            <Divider />
            <TextField select label="目标时长" value={composeForm.duration}
              onChange={e => setComposeForm(p => ({ ...p, duration: Number(e.target.value) }))}
              size="small" fullWidth>
              {[15, 30, 60, 90, 120].map(d => (
                <MenuItem key={d} value={d}>{d}秒</MenuItem>
              ))}
            </TextField>
            <TextField select label="风格" value={composeForm.style}
              onChange={e => setComposeForm(p => ({ ...p, style: e.target.value }))}
              size="small" fullWidth>
              {['standard', 'dynamic', 'storytelling', 'tutorial'].map(s => (
                <MenuItem key={s} value={s}>{{ standard: '标准', dynamic: '动感', storytelling: '叙事', tutorial: '教程' }[s]}</MenuItem>
              ))}
            </TextField>
            <FormControlLabel
              control={<Switch checked={composeForm.subtitleEnabled}
                onChange={e => setComposeForm(p => ({ ...p, subtitleEnabled: e.target.checked }))} />}
              label="自动生成字幕"
            />
            <Alert severity="warning" variant="outlined">
              自动合成当前只消费项目中已有的 videoUrl、脚本文本和可用音频片段；BGM 风格、字幕开关等参数暂不直接落到 `/short-video/edit/auto-compose` 的 materials 结构。
            </Alert>
            <Alert severity="info" variant="outlined">
              数字人口播带货会串联脚本、分镜、数字人口播、产品 B-roll、配音和自动合成。数字人供应商未配置时会跳过头像口播片段，只用产品 B-roll 继续成片。
            </Alert>
            {project && !project.finalVideoUrl && (
              <Alert severity="info" variant="outlined">
                当前项目尚未生成成片地址，自动合成成功后会尝试回写项目并刷新列表。
              </Alert>
            )}
            {composeMutation.isError && (
              <Alert data-testid="video-editing-compose-error" data-input-retained="true" data-no-mock-final-video="true" severity="error">
                自动合成失败（POST {AUTO_COMPOSE_ENDPOINT}）：{getErrorMessage(composeMutation.error)}。弹窗输入会保留，不会伪造成片地址或关闭当前对话框。
              </Alert>
            )}
            {digitalHumanCommerceMutation.isError && (
              <Alert data-testid="video-editing-dh-commerce-error" data-input-retained="true" data-no-local-workflow-status="true" severity="error">
                数字人口播带货流水线启动失败（POST {DIGITAL_HUMAN_COMMERCE_START_ENDPOINT}）：{getErrorMessage(digitalHumanCommerceMutation.error)}。
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setComposeOpen(false)}>取消</Button>
          <Button
            variant="outlined"
            startIcon={<MovieIcon />}
            onClick={() => void startDigitalHumanCommerce()}
            disabled={digitalHumanCommerceMutation.isPending || !composeForm.projectId}
            data-testid="video-editing-start-dh-commerce-button"
            data-source-endpoint={DIGITAL_HUMAN_COMMERCE_START_ENDPOINT}
          >
            数字人口播带货成片
          </Button>
          <Button
            variant="contained"
            startIcon={<AutoAwesomeIcon />}
            onClick={async () => {
              if (!(await gate('shortvideo-maker', 'shortvideo-maker.export'))) return
              composeMutation.mutate()
            }}
            disabled={composeMutation.isPending || !composeForm.projectId}>
            开始合成
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
