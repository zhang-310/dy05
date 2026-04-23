import { useSearchParams } from 'react-router-dom'
import {
  Box, Typography, Button, Chip, LinearProgress, Alert, Stack, CircularProgress,
} from '@mui/material'
import MovieFilterIcon from '@mui/icons-material/MovieFilter'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid } from '@/components/base'
import { shortvideoApi, shotsToImg2VideoKeyframes } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import type { GridColDef } from '@mui/x-data-grid'

const TASK_STATUS_COLORS: Record<string, 'default' | 'primary' | 'success' | 'error' | 'warning'> = {
  pending: 'default', running: 'primary', completed: 'success', failed: 'error',
}

export default function MaterialProductionPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null

  const { data: project, isLoading: projectLoading } = useQuery({
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

  const { data: shotList, isLoading: shotListLoading, isError: shotListError } = useQuery({
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
      toast(e.message || '提交失败', 'error')
    },
  })

  const { data: taskResult, isLoading } = useQuery({
    queryKey: ['sv-video-tasks', projectId],
    queryFn: () => shortvideoApi.videoTaskList({ projectId: projectId ?? undefined, page: 0, rows: 20 }),
    refetchInterval: 10000,
  })

  const tasks = (taskResult?.list ?? []).map((t, i) => ({ ...t, id: (t.id as number) ?? i }))

  const handleCancel = async (taskId: number) => {
    try {
      await shortvideoApi.videoTaskCancel(taskId)
      toast('已取消', 'success')
      qc.invalidateQueries({ queryKey: ['sv-video-tasks', projectId] })
    } catch { toast('取消失败', 'error') }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'taskType', headerName: '类型', width: 120 },
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
      field: '_actions', headerName: '操作', width: 100, sortable: false,
      renderCell: ({ row }) => (
        String(row.status) === 'running' || String(row.status) === 'pending' ? (
          <Button size="small" color="error" onClick={() => handleCancel(row.id as number)}>取消</Button>
        ) : null
      ),
    },
  ]

  const canSubmit =
    !!projectId &&
    shotList != null &&
    keyframes.length > 0 &&
    !submitMutation.isPending

  const shotListBlockedReason =
    projectId && project && !projectLoading && shotListSource == null
      ? '项目未关联 scriptId / shotListId，请先在项目或脚本侧生成分镜列表'
      : null

  return (
    <Box>
      <PageHeader
        title="素材生产"
        breadcrumbs={[{ label: '短视频' }, { label: '素材生产' }]}
        subtitle={projectId ? `项目 #${projectId}` : undefined}
      />

      {!projectId && <Alert severity="warning" sx={{ mb: 2 }}>请从项目工作台进入，以关联项目</Alert>}

      {!!projectId && (
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
          <Button
            variant="contained"
            color="primary"
            startIcon={submitMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <MovieFilterIcon />}
            disabled={!canSubmit}
            onClick={() => submitMutation.mutate()}
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
        <Alert severity="warning" sx={{ mb: 2 }}>{shotListBlockedReason}</Alert>
      )}

      {shotListError && (
        <Alert severity="error" sx={{ mb: 2 }}>分镜加载失败，请确认已生成分镜列表</Alert>
      )}

      <Alert severity="info" sx={{ mb: 2 }}>
        根据项目关联的 shot-list 自动组装 <code>keyframes</code>（<code>keyframeUrl</code> → <code>imageUrl</code>）。
        无关键帧 URL 的镜头不会提交。
      </Alert>

      <StandardDataGrid
        rows={tasks}
        columns={columns}
        loading={isLoading}
      />
    </Box>
  )
}
