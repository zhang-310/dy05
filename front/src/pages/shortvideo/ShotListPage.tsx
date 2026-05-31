import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  LinearProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import DeleteIcon from '@mui/icons-material/Delete'
import MovieFilterIcon from '@mui/icons-material/MovieFilter'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ConfirmDialog, PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import type { SvShot } from '@/types/shortvideo'

type ShotDraft = SvShot & {
  localId?: string
}

const SHOT_TYPES = ['opening', 'product', 'demo', 'testimonial', 'transition', 'closing', 'normal']
const CAMERA_ANGLES = ['特写', '近景', '中景', '全景', '俯拍', '仰拍', '侧拍']
const PROJECT_GET_ENDPOINT = '/short-video/project/get'
const PROJECT_SAVE_ENDPOINT = '/short-video/project/save'
const SHOT_GET_ENDPOINT = '/short-video/shot-list/get'
const SHOT_GET_BY_SCRIPT_ENDPOINT = '/short-video/shot-list/get-by-script'
const SHOT_GENERATE_ENDPOINT = '/short-video/shot-list/generate'
const SHOT_SAVE_ENDPOINT = '/short-video/shot-list/save'
const SHOT_DELETE_ENDPOINT = '/short-video/shot-list/delete-shot'
const SCRIPT_GET_ENDPOINT = '/short-video/script/get'
const SHOT_LIST_READY_ENDPOINTS = [
  PROJECT_GET_ENDPOINT,
  PROJECT_SAVE_ENDPOINT,
  SCRIPT_GET_ENDPOINT,
  SHOT_GET_ENDPOINT,
  SHOT_GET_BY_SCRIPT_ENDPOINT,
  SHOT_GENERATE_ENDPOINT,
  SHOT_SAVE_ENDPOINT,
  SHOT_DELETE_ENDPOINT,
].join('|')
const SHOT_LIST_UNSUPPORTED_ENDPOINTS = [
  '/short-video/shot-list/mock',
  '/short-video/shot-list/local-generate',
  '/short-video/shot-list/local-save',
  '/short-video/shot-list/local-delete',
  '/short-video/shot-list/export',
  '/short-video/project/local-save',
  '/short-video/script/local-get',
  '/short-video/material/mock',
].join('|')
const SHOT_LIST_READY_ROUTES = [
  shortvideoRoutes.shotList,
  `${shortvideoRoutes.shotList}?projectId=:id`,
  `${shortvideoRoutes.scriptPlanning}?projectId=:id`,
  `${shortvideoRoutes.materialProduction}?projectId=:id`,
  `${shortvideoRoutes.workbench}?projectId=:id`,
].join('|')
const SHOT_LIST_SUPPORTED_ACTIONS = [
  'load-project-shot-list',
  'generate-shot-list-from-script',
  'save-shot-list',
  'delete-shot',
  'navigate-material-production',
  'navigate-script-planning',
].join('|')

export default function ShotListPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const urlProjectId = searchParams.get('projectId')
  const [projectIdInput, setProjectIdInput] = useState(urlProjectId ?? '')
  const [loadedProjectId, setLoadedProjectId] = useState(() => {
    const n = urlProjectId ? Number(urlProjectId) : 0
    return n > 0 ? n : 0
  })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ShotDraft>({})
  const [generating, setGenerating] = useState(false)
  const [deleteShotId, setDeleteShotId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [generateError, setGenerateError] = useState<string | null>(null)

  useEffect(() => {
    if (!urlProjectId) return
    const n = Number(urlProjectId)
    if (n > 0) {
      setProjectIdInput(String(n))
      setLoadedProjectId(n)
    }
  }, [urlProjectId])

  const {
    data: project,
    isLoading: projectLoading,
    isError: projectIsError,
    error: projectError,
    refetch: refetchProject,
  } = useQuery({
    queryKey: ['sv-project', loadedProjectId],
    queryFn: () => shortvideoApi.get(loadedProjectId),
    enabled: loadedProjectId > 0,
  })

  const shotSource = useMemo(() => {
    if (!project) return null
    if (project.shotListId != null && project.shotListId > 0) return { kind: 'shotList' as const, id: project.shotListId }
    if (project.scriptId != null && project.scriptId > 0) return { kind: 'script' as const, id: project.scriptId }
    return null
  }, [project])

  const {
    data: shotList,
    isLoading: shotLoading,
    isError: shotError,
    error: shotLoadError,
    refetch: refetchShotList,
  } = useQuery({
    queryKey: ['sv-shot-list-for-project', loadedProjectId, shotSource],
    queryFn: () => {
      if (shotSource?.kind === 'shotList') return shortvideoApi.shotListGet(shotSource.id)
      if (shotSource?.kind === 'script') return shortvideoApi.shotListGetByScript(shotSource.id)
      throw new Error('no shot source')
    },
    enabled: loadedProjectId > 0 && shotSource != null,
  })

  const { data: script } = useQuery({
    queryKey: ['sv-script', project?.scriptId],
    queryFn: () => shortvideoApi.svScriptGet(project!.scriptId!),
    enabled: !!project?.scriptId,
  })

  const shots = shotList?.shots ?? []
  const currentShotListId = shotList?.id ?? project?.shotListId
  const keyframeCount = shots.filter((shot) => !!shot.keyframeUrl).length
  const videoClipCount = shots.filter((shot) => !!shot.videoUrl).length
  const reviewedCount = shots.filter((shot) => !!shot.reviewStatus).length
  const totalDuration = shots.reduce((sum, shot) => sum + Number(shot.duration ?? 0), 0)
  const completionPercent = shots.length > 0
    ? Math.round(((keyframeCount + videoClipCount) / (shots.length * 2)) * 100)
    : 0

  const handleLoad = () => {
    const id = Number(projectIdInput)
    setDeleteError(null)
    setSaveError(null)
    setGenerateError(null)
    if (id > 0) setLoadedProjectId(id)
  }

  const handleOpen = (shot?: SvShot) => {
    setEditing(shot ? { ...shot } : {
      localId: crypto.randomUUID(),
      shotNumber: shots.length + 1,
      duration: 5,
      cameraAngle: '中景',
      cameraType: 'normal',
    })
    setOpen(true)
  }

  const handleSaveShot = async () => {
    if (!currentShotListId) {
      toast('请先生成分镜列表', 'warning')
      return
    }
    setSaveError(null)
    const merged = mergeShot(shots, editing)
    try {
      await shortvideoApi.shotListSave({
        shotListId: currentShotListId,
        shots: merged.map(toSaveShot),
      })
      toast('分镜已保存', 'success')
      setOpen(false)
      await qc.invalidateQueries({ queryKey: ['sv-shot-list-for-project'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setSaveError(`分镜保存失败（POST ${SHOT_SAVE_ENDPOINT}）：${message}。编辑弹窗、当前分镜列表和 BOS 引用会保留。`)
      toast(`保存失败：${message}`, 'error')
    }
  }

  const handleDelete = async () => {
    if (!currentShotListId || deleteShotId == null) return
    setDeleting(true)
    setDeleteError(null)
    try {
      await shortvideoApi.shotDelete(deleteShotId)
      toast('分镜已移除', 'success')
      setDeleteShotId(null)
      await qc.invalidateQueries({ queryKey: ['sv-shot-list-for-project'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setDeleteError(`删除分镜失败（POST ${SHOT_DELETE_ENDPOINT}）：${message}。分镜行会保留，避免误判已删除。`)
      toast(`删除失败：${message}`, 'error')
    } finally {
      setDeleting(false)
    }
  }

  const handleGenerateFromScript = async () => {
    if (!loadedProjectId || !project) {
      toast('请先加载项目', 'warning')
      return
    }
    setGenerateError(null)
    if (!project.scriptId) {
      toast('项目未关联脚本，请先完成脚本策划', 'warning')
      return
    }
    const scriptContent = typeof script?.content === 'string' ? script.content : ''
    if (!scriptContent.trim()) {
      toast('脚本内容为空，无法生成分镜', 'warning')
      return
    }
    setGenerating(true)
    try {
      const result = await shortvideoApi.shotListGenerate({
        scriptId: project.scriptId,
        scriptContent,
        shotCount: Math.max(3, Math.min(12, Math.ceil(Number(project.duration ?? 60) / 8))),
        style: typeof script?.style === 'string' ? script.style : undefined,
      })
      if (result.shotListId) {
        await shortvideoApi.save({
          id: project.id,
          accountId: project.accountId,
          title: project.title,
          projectType: project.projectType,
          personaId: project.personaId,
          scheduleDate: project.scheduleDate,
          shootStatus: project.shootStatus,
          scriptId: project.scriptId,
          shotListId: result.shotListId,
          status: 'processing',
          finalVideoUrl: project.finalVideoUrl,
          thumbnailUrl: project.thumbnailUrl,
          characterReferenceUrl: project.characterReferenceUrl,
          sceneReferenceUrl: project.sceneReferenceUrl,
          duration: project.duration,
          publishTitle: project.publishTitle,
          publishPlatforms: project.publishPlatforms,
          publishTime: project.publishTime,
          reviewStatus: project.reviewStatus,
          relatedProductIds: project.relatedProductIds,
        })
      }
      toast(`已生成 ${result.shots?.length ?? 0} 条分镜`, 'success')
      await qc.invalidateQueries({ queryKey: ['sv-project', loadedProjectId] })
      await qc.invalidateQueries({ queryKey: ['sv-shot-list-for-project'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setGenerateError(`分镜生成或项目回写失败（POST ${SHOT_GENERATE_ENDPOINT} 或 POST ${PROJECT_SAVE_ENDPOINT}）：${message}。项目上下文、脚本文本和当前分镜行会保留，可修复后重试。`)
      toast(`分镜生成失败：${message}`, 'error')
    } finally {
      setGenerating(false)
    }
  }

  return (
    <Box
      data-testid="shot-list-page"
      data-ready-endpoints={SHOT_LIST_READY_ENDPOINTS}
      data-ready-routes={SHOT_LIST_READY_ROUTES}
      data-supported-actions={SHOT_LIST_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={SHOT_LIST_UNSUPPORTED_ENDPOINTS}
      data-no-local-shot-fallback="true"
      data-no-local-shot-generation="true"
      data-no-local-project-mutation="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="分镜设计"
        breadcrumbs={[
          { label: '短视频', href: shortvideoRoutes.dashboard },
          { label: '工作台', href: loadedProjectId ? `${shortvideoRoutes.workbench}?projectId=${loadedProjectId}` : shortvideoRoutes.projects },
          { label: '分镜设计' },
        ]}
        subtitle={project ? `${project.title} · 项目 #${project.id}` : undefined}
      />

      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
        <TextField size="small" label="项目 ID" value={projectIdInput}
          onChange={e => setProjectIdInput(e.target.value)} sx={{ width: 160 }} />
        <Button
          variant="outlined"
          size="small"
          onClick={handleLoad}
          data-testid="shot-list-load-project-button"
          data-source-endpoint={PROJECT_GET_ENDPOINT}
        >
          加载项目
        </Button>
        <Box sx={{ flex: 1 }} />
        {loadedProjectId > 0 && (
          <>
            <Button
              variant="outlined"
              size="small"
              startIcon={generating ? <AutoAwesomeIcon /> : <AutoAwesomeIcon />}
              onClick={handleGenerateFromScript}
              disabled={generating || projectLoading || !project?.scriptId}
              data-testid="shot-list-generate-button"
              data-source-endpoint={SHOT_GENERATE_ENDPOINT}
            >
              {generating ? '生成中...' : '从脚本生成分镜'}
            </Button>
            <Button
              variant="outlined"
              size="small"
              startIcon={<MovieFilterIcon />}
              onClick={() => navigate(`${shortvideoRoutes.materialProduction}?projectId=${loadedProjectId}`)}
              disabled={!currentShotListId}
              data-testid="shot-list-open-material-production-button"
              data-target-route={`${shortvideoRoutes.materialProduction}?projectId=${loadedProjectId}`}
            >
              去素材生产
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpen()}
              disabled={!currentShotListId}
              data-testid="shot-list-open-create-button"
            >
              新增分镜
            </Button>
          </>
        )}
      </Stack>

      <Alert
        severity="info"
        variant="outlined"
        data-testid="shot-list-boundary-contract"
        data-no-local-shot-fallback="true"
        data-no-local-project-mutation="true"
        data-no-local-material-fallback="true"
        data-supported-actions={SHOT_LIST_SUPPORTED_ACTIONS}
      >
        本页使用 `/short-video/project/get`、`/short-video/shot-list/get|get-by-script|generate|save|delete-shot`。新增和编辑会整体保存当前分镜列表；BOS key 会随 URL 一起保留，避免素材生产后再次编辑丢失对象存储引用。
      </Alert>

      {loadedProjectId <= 0 && <Alert severity="info">请从项目工作台进入，或输入项目 ID 加载。</Alert>}
      {projectIsError && (
        <Alert
          severity="error"
          data-testid="shot-list-project-error"
          data-no-local-project-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetchProject()}>重试</Button>}
        >
          项目详情加载失败（POST {PROJECT_GET_ENDPOINT}）：{getErrorMessage(projectError)}。请确认项目是否存在或当前账号是否有权限。
        </Alert>
      )}
      {project && (
        <Grid
          container
          spacing={2}
          data-testid="shot-list-diagnostics"
          data-no-client-shot-synthesis="true"
          data-no-local-material-fallback="true"
        >
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">分镜数</Typography>
                <Typography variant="h5" fontWeight={700}>{shots.length}</Typography>
                <Typography variant="caption" color="text.secondary">shotList #{currentShotListId ?? '-'}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">关键帧 / 视频片段</Typography>
                <Typography variant="h5" fontWeight={700}>{keyframeCount} / {videoClipCount}</Typography>
                <LinearProgress variant="determinate" value={completionPercent} sx={{ mt: 1, height: 6, borderRadius: 3 }} />
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">预计时长</Typography>
                <Typography variant="h5" fontWeight={700}>{totalDuration || Number(project.duration ?? 0)}s</Typography>
                <Typography variant="caption" color="text.secondary">来自分镜时长汇总</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary">审核标记</Typography>
                <Typography variant="h5" fontWeight={700}>{reviewedCount}</Typography>
                <Typography variant="caption" color="text.secondary">仅展示已有分镜审核字段</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
      {project && !project.scriptId && (
        <Alert severity="warning" action={
          <Button size="small" onClick={() => navigate(`${shortvideoRoutes.scriptPlanning}?projectId=${project.id}`)}>去脚本策划</Button>
        }>
          项目未关联脚本，无法自动生成分镜。
        </Alert>
      )}
      {project && project.scriptId && !currentShotListId && !shotLoading && (
        <Alert severity="info">项目已有脚本但暂无分镜，点击「从脚本生成分镜」继续。</Alert>
      )}
      {shotError && (
        <Alert
          severity="warning"
          data-testid="shot-list-load-error"
          data-no-local-shot-fallback="true"
          data-no-mock-shot-row="true"
          action={<Button color="inherit" size="small" onClick={() => refetchShotList()}>重试</Button>}
        >
          分镜加载失败（POST {shotSource?.kind === 'shotList' ? SHOT_GET_ENDPOINT : SHOT_GET_BY_SCRIPT_ENDPOINT}）：{getErrorMessage(shotLoadError)}。未展示模拟分镜，当前项目和脚本上下文会保留，可从脚本重新生成。
        </Alert>
      )}
      {generateError && (
        <Alert
          severity="error"
          data-testid="shot-list-generate-error"
          data-input-retained="true"
          data-no-local-shot-generation="true"
          data-no-local-project-mutation="true"
        >
          {generateError}
        </Alert>
      )}
      {saveError && (
        <Alert
          severity="error"
          data-testid="shot-list-save-error"
          data-input-retained="true"
          data-no-local-shot-mutation="true"
        >
          {saveError}
        </Alert>
      )}
      {deleteError && (
        <Alert
          severity="error"
          data-testid="shot-list-delete-error"
          data-no-local-delete-mutation="true"
        >
          {deleteError}
        </Alert>
      )}
      {shotList && shots.length > 0 && keyframeCount < shots.length && (
        <Alert severity="warning" action={
          <Button
            size="small"
            color="inherit"
            onClick={() => navigate(`${shortvideoRoutes.materialProduction}?projectId=${loadedProjectId}`)}
            data-testid="shot-list-empty-open-material-button"
            data-target-route={`${shortvideoRoutes.materialProduction}?projectId=${loadedProjectId}`}
          >
            去素材生产
          </Button>
        }>
          还有 {shots.length - keyframeCount} 条分镜没有关键帧。图生视频只会提交已有关键帧 URL 的镜头。
        </Alert>
      )}
      {(projectLoading || shotLoading) && <Typography color="text.secondary">加载中...</Typography>}

      {shots.length > 0 && (
        <Stack spacing={1.5}>
          {shots.map((shot, idx) => (
            <Card
              key={shot.id ?? `${shot.shotNumber}-${idx}`}
              variant="outlined"
              data-testid="shot-list-row"
              data-no-local-shot-row-synthesis="true"
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack direction="row" alignItems="flex-start" spacing={2}>
                  <Box sx={{ width: 36, height: 36, bgcolor: 'primary.main', color: 'white', borderRadius: '50%',
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 700, flexShrink: 0 }}>
                    {shot.shotNumber ?? idx + 1}
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} alignItems="center" mb={0.5} flexWrap="wrap" useFlexGap>
                      <Chip label={shot.cameraType || 'normal'} size="small" color="primary" variant="outlined" />
                      <Chip label={shot.cameraAngle || '中景'} size="small" />
                      <Chip label={`${shot.duration ?? 5}s`} size="small" color="secondary" />
                      {shot.reviewStatus && <Chip label={`审核 ${shot.reviewStatus}`} size="small" variant="outlined" />}
                      {shot.keyframeUrl && <Chip label="关键帧" size="small" color="success" variant="outlined" />}
                      {shot.videoUrl && <Chip label="视频片段" size="small" color="success" />}
                      {(shot.keyframeBosKey || shot.videoBosKey || shot.audioBosKey) && <Chip label="BOS" size="small" variant="outlined" />}
                    </Stack>
                    <Typography variant="body2" fontWeight={500} mb={0.25}>{shot.sceneDescription || '（无画面描述）'}</Typography>
                    {shot.dialogue && (
                      <Typography variant="caption" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                        台词：{shot.dialogue}
                      </Typography>
                    )}
                  </Box>
                  <Stack direction="row" spacing={0.5}>
                    <Button size="small" onClick={() => handleOpen(shot)} data-testid="shot-list-edit-button">编辑</Button>
                    <IconButton
                      size="small"
                      color="error"
                      aria-label={`删除分镜 ${shot.shotNumber ?? idx + 1}`}
                      onClick={() => {
                        setDeleteError(null)
                        setDeleteShotId(shot.id ?? null)
                      }}
                      disabled={!shot.id}
                      data-testid="shot-list-delete-button"
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}

      {loadedProjectId > 0 && shots.length === 0 && !projectLoading && !shotLoading && (
        <Typography
          color="text.secondary"
          textAlign="center"
          data-testid="shot-list-empty"
          data-no-mock-shot-fallback="true"
          sx={{ py: 4 }}
        >
          暂无分镜。
        </Typography>
      )}

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing.id ? '编辑分镜' : '新增分镜'}</DialogTitle>
        <DialogContent data-testid="shot-list-edit-dialog" data-input-retained="true" data-no-local-shot-mutation="true">
          <Stack spacing={2} sx={{ mt: 1 }}>
            {saveError && (
              <Alert severity="error">{saveError}</Alert>
            )}
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField label="分镜编号" type="number" value={editing.shotNumber ?? ''}
                onChange={e => setEditing(p => ({ ...p, shotNumber: Number(e.target.value) }))}
                size="small" sx={{ width: 120 }} />
              <TextField select label="镜头类型" value={editing.cameraType ?? 'normal'}
                onChange={e => setEditing(p => ({ ...p, cameraType: e.target.value }))}
                size="small" sx={{ flex: 1 }}>
                {SHOT_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </TextField>
              <TextField select label="景别" value={editing.cameraAngle ?? '中景'}
                onChange={e => setEditing(p => ({ ...p, cameraAngle: e.target.value }))}
                size="small" sx={{ width: 120 }}>
                {CAMERA_ANGLES.map(a => <MenuItem key={a} value={a}>{a}</MenuItem>)}
              </TextField>
              <TextField label="时长(s)" type="number" value={editing.duration ?? 5}
                onChange={e => setEditing(p => ({ ...p, duration: Number(e.target.value) }))}
                size="small" sx={{ width: 100 }} />
            </Stack>
            <TextField label="时间段" value={editing.timeRange ?? ''}
              onChange={e => setEditing(p => ({ ...p, timeRange: e.target.value }))}
              fullWidth size="small" placeholder="0-5s" />
            <TextField label="画面描述" value={editing.sceneDescription ?? ''}
              onChange={e => setEditing(p => ({ ...p, sceneDescription: e.target.value }))}
              multiline minRows={2} fullWidth size="small" />
            <TextField label="台词/旁白" value={editing.dialogue ?? ''}
              onChange={e => setEditing(p => ({ ...p, dialogue: e.target.value }))}
              multiline minRows={2} fullWidth size="small" />
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField label="关键帧 URL" value={editing.keyframeUrl ?? ''}
                onChange={e => setEditing(p => ({ ...p, keyframeUrl: e.target.value }))}
                size="small" fullWidth />
              <TextField label="关键帧 BOS Key" value={editing.keyframeBosKey ?? ''}
                onChange={e => setEditing(p => ({ ...p, keyframeBosKey: e.target.value }))}
                size="small" fullWidth />
            </Stack>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField label="尾帧 URL" value={editing.endFrameUrl ?? ''}
                onChange={e => setEditing(p => ({ ...p, endFrameUrl: e.target.value }))}
                size="small" fullWidth />
              <TextField label="尾帧 BOS Key" value={editing.endFrameBosKey ?? ''}
                onChange={e => setEditing(p => ({ ...p, endFrameBosKey: e.target.value }))}
                size="small" fullWidth />
            </Stack>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField label="视频片段 URL" value={editing.videoUrl ?? ''}
                onChange={e => setEditing(p => ({ ...p, videoUrl: e.target.value }))}
                size="small" fullWidth />
              <TextField label="视频 BOS Key" value={editing.videoBosKey ?? ''}
                onChange={e => setEditing(p => ({ ...p, videoBosKey: e.target.value }))}
                size="small" fullWidth />
            </Stack>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField label="音频 URL" value={editing.audioUrl ?? ''}
                onChange={e => setEditing(p => ({ ...p, audioUrl: e.target.value }))}
                size="small" fullWidth />
              <TextField label="音频 BOS Key" value={editing.audioBosKey ?? ''}
                onChange={e => setEditing(p => ({ ...p, audioBosKey: e.target.value }))}
                size="small" fullWidth />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleSaveShot}
            data-testid="shot-list-save-button"
            data-source-endpoint={SHOT_SAVE_ENDPOINT}
          >
            保存
          </Button>
        </DialogActions>
      </Dialog>
      <ConfirmDialog
        open={deleteShotId !== null}
        title="删除分镜"
        content={deleteError ?? '确定要删除该分镜吗？删除后素材生产和成片合成将不再包含该镜头。'}
        onClose={() => setDeleteShotId(null)}
        onConfirm={handleDelete}
        loading={deleting}
      />
    </Box>
  )
}

function mergeShot(shots: SvShot[], draft: ShotDraft): SvShot[] {
  const normalized: SvShot = {
    ...draft,
    shotNumber: draft.shotNumber ?? shots.length + 1,
    duration: draft.duration ?? 5,
  }
  const next = draft.id
    ? shots.map((shot) => (shot.id === draft.id ? normalized : shot))
    : [...shots, normalized]
  return next
    .slice()
    .sort((a, b) => Number(a.shotNumber ?? 9999) - Number(b.shotNumber ?? 9999))
    .map((shot, index) => ({ ...shot, shotNumber: index + 1 }))
}

function toSaveShot(shot: SvShot): Record<string, unknown> {
  return {
    id: shot.id,
    shotNumber: shot.shotNumber,
    timeRange: shot.timeRange,
    sceneDescription: shot.sceneDescription,
    cameraAngle: shot.cameraAngle,
    cameraType: shot.cameraType,
    action: shot.action,
    dialogue: shot.dialogue,
    mood: shot.mood,
    keyframeUrl: shot.keyframeUrl,
    keyframeBosKey: shot.keyframeBosKey,
    endFrameUrl: shot.endFrameUrl,
    endFrameBosKey: shot.endFrameBosKey,
    videoUrl: shot.videoUrl,
    videoBosKey: shot.videoBosKey,
    audioUrl: shot.audioUrl,
    audioBosKey: shot.audioBosKey,
    duration: shot.duration,
  }
}
