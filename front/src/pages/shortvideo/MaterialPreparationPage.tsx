import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Button, Typography, CircularProgress,
  Grid, Alert, Dialog, DialogTitle, DialogContent, DialogActions,
  Stack, Chip, LinearProgress,
} from '@mui/material'
import { Person as PersonIcon, Landscape as SceneIcon, CloudUpload as UploadIcon } from '@mui/icons-material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { getErrorMessage } from '@/utils/errorHandler'

type RefType = 'character' | 'scene'
const PROJECT_GET_ENDPOINT = '/short-video/project/get'
const CHARACTER_UPLOAD_ENDPOINT = '/short-video/upload/reference/character'
const SCENE_UPLOAD_ENDPOINT = '/short-video/upload/reference/scene'
const PREP_READY_ENDPOINTS = [
  PROJECT_GET_ENDPOINT,
  CHARACTER_UPLOAD_ENDPOINT,
  SCENE_UPLOAD_ENDPOINT,
].join('|')
const PREP_READY_ROUTES = [
  shortvideoRoutes.materialPrepare,
  `${shortvideoRoutes.materialPrepare}?projectId=:id`,
  `${shortvideoRoutes.materialProduction}?projectId=:id`,
  `${shortvideoRoutes.workbench}?projectId=:id`,
].join('|')
const PREP_SUPPORTED_ACTIONS = [
  'refresh-project-reference-assets',
  'upload-character-reference',
  'upload-scene-reference',
  'preview-reference-image',
  'navigate-material-production',
].join('|')
const PREP_UNSUPPORTED_ENDPOINTS = [
  '/short-video/project/mock',
  '/short-video/project/local-get',
  '/short-video/project/local-save',
  '/short-video/upload/reference/mock',
  '/short-video/upload/reference/local-character',
  '/short-video/upload/reference/local-scene',
  '/short-video/upload/reference/browser-read',
  '/short-video/upload/reference/browser-scrape',
  '/short-video/material/local-preview',
].join('|')

function uploadEndpointFor(type: RefType) {
  return type === 'character' ? CHARACTER_UPLOAD_ENDPOINT : SCENE_UPLOAD_ENDPOINT
}

function uploadLabelFor(type: RefType) {
  return type === 'character' ? '角色参考图' : '场景参考图'
}

export default function MaterialPreparationPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null
  const [uploadingChar, setUploadingChar] = useState(false)
  const [uploadingScene, setUploadingScene] = useState(false)
  const [charUrl, setCharUrl] = useState('')
  const [sceneUrl, setSceneUrl] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewUrl, setPreviewUrl] = useState('')
  const [previewType, setPreviewType] = useState<RefType>('character')
  const [uploadError, setUploadError] = useState('')
  const charInputRef = useRef<HTMLInputElement>(null)
  const sceneInputRef = useRef<HTMLInputElement>(null)

  const { data: project, isLoading: projectLoading, isError: projectIsError, error: projectError, refetch } = useQuery({
    queryKey: ['sv-project', projectId],
    queryFn: () => shortvideoApi.get(projectId!),
    enabled: !!projectId,
  })

  useEffect(() => {
    if (!project) return
    setCharUrl(project.characterReferenceUrl ?? '')
    setSceneUrl(project.sceneReferenceUrl ?? '')
  }, [project])

  const handleUpload = async (file: File, type: RefType) => {
    if (!projectId) {
      toast('请先关联项目', 'warning')
      return
    }
    const setter = type === 'character' ? setUploadingChar : setUploadingScene
    setter(true)
    setUploadError('')
    try {
      const baseId = `p${projectId}-default`
      const data = type === 'character'
        ? await shortvideoApi.uploadReferenceCharacter({ characterId: baseId, projectId, file })
        : await shortvideoApi.uploadReferenceScene({ sceneId: baseId, projectId, file })
      const url = String(data.url ?? '')
      if (!url) {
        throw new Error('上传接口未返回 url')
      }
      if (type === 'character') setCharUrl(url)
      else setSceneUrl(url)
      await qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
      toast('上传成功', 'success')
    } catch (e) {
      const message = e instanceof Error ? e.message : '上传失败（需登录且 BOS 已配置）'
      const endpoint = uploadEndpointFor(type)
      const label = uploadLabelFor(type)
      setUploadError(`${label}上传失败（POST ${endpoint}）：${message}。已上传的参考图 URL 会保留，不回写空地址。`)
      toast(`${label}上传失败：${message}`, 'error')
    } finally {
      setter(false)
    }
  }

  const onFileChange = (type: RefType) => async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) await handleUpload(file, type)
  }

  const openPreview = (type: RefType) => {
    setPreviewType(type)
    setPreviewUrl(type === 'character' ? charUrl : sceneUrl)
    setPreviewOpen(true)
  }

  const UploadCard = ({ type, label, icon, uploading, url }: { type: RefType; label: string; icon: React.ReactNode; uploading: boolean; url: string }) => (
    <Card
      variant="outlined"
      data-testid={type === 'character' ? 'material-preparation-character-card' : 'material-preparation-scene-card'}
      data-source-endpoint={uploadEndpointFor(type)}
      data-no-local-upload-fallback="true"
    >
      <CardContent sx={{ textAlign: 'center', py: 4 }}>
        <Box sx={{ color: 'primary.main', mb: 1, fontSize: 48 }}>{icon}</Box>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>{label}</Typography>
        {url ? (
          <>
            <Box
              component="img" src={url} alt={label}
              sx={{ width: '100%', maxHeight: 160, objectFit: 'contain', mb: 1, borderRadius: 1, cursor: 'pointer' }}
              onClick={() => openPreview(type)}
            />
            <Typography variant="caption" color="success.main">已上传</Typography>
          </>
        ) : (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>暂未上传参考图</Typography>
        )}
        <input
          ref={type === 'character' ? charInputRef : sceneInputRef}
          type="file" accept="image/*"
          style={{ display: 'none' }}
          onChange={onFileChange(type)}
        />
        <Button
          variant="outlined"
          startIcon={uploading ? <CircularProgress size={16} /> : <UploadIcon />}
          onClick={() => (type === 'character' ? charInputRef : sceneInputRef).current?.click()}
          disabled={uploading}
          sx={{ mt: 1 }}
          data-testid={type === 'character' ? 'material-preparation-upload-character-button' : 'material-preparation-upload-scene-button'}
          data-source-endpoint={uploadEndpointFor(type)}
        >
          {url ? '重新上传' : '上传参考图'}
        </Button>
      </CardContent>
    </Card>
  )

  return (
    <Box
      data-testid="material-preparation-page"
      data-ready-endpoints={PREP_READY_ENDPOINTS}
      data-ready-routes={PREP_READY_ROUTES}
      data-supported-actions={PREP_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={PREP_UNSUPPORTED_ENDPOINTS}
      data-no-local-project-fallback="true"
      data-no-local-upload-fallback="true"
      data-no-browser-direct-scrape="true"
    >
      <PageHeader
        title="素材准备"
        breadcrumbs={[
          { label: '短视频' },
          { label: '工作台', href: projectId ? `${shortvideoRoutes.workbench}?projectId=${projectId}` : shortvideoRoutes.projects },
          { label: '素材准备' },
        ]}
        subtitle={projectId ? `项目 #${projectId}${project?.title ? ` · ${project.title}` : ''} · POST ${PROJECT_GET_ENDPOINT}` : undefined}
        actions={projectId ? (
          <Button
            variant="outlined"
            onClick={() => refetch()}
            data-testid="material-preparation-refresh-button"
            data-source-endpoint={PROJECT_GET_ENDPOINT}
          >
            刷新项目
          </Button>
        ) : undefined}
      />

      {!projectId && (
        <Alert severity="warning" data-testid="material-preparation-no-project" data-no-local-project-fallback="true" sx={{ mb: 2 }}>请从项目工作台进入，以关联项目</Alert>
      )}

      {projectLoading && <LinearProgress sx={{ mb: 2 }} />}

      {projectIsError && (
        <Alert
          severity="error"
          data-testid="material-preparation-project-error"
          data-no-local-project-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          sx={{ mb: 2 }}
        >
          项目加载失败（POST {PROJECT_GET_ENDPOINT}）：{getErrorMessage(projectError)}。无法回显已上传参考图，页面不会填充占位图。
        </Alert>
      )}

      <Alert
        severity="info"
        data-testid="material-preparation-boundary-contract"
        data-source-endpoints={`${PROJECT_GET_ENDPOINT}|${CHARACTER_UPLOAD_ENDPOINT}|${SCENE_UPLOAD_ENDPOINT}`}
        data-no-local-upload-fallback="true"
        data-supported-actions={PREP_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        上传成功后后端会把 URL 回写到项目的 characterReferenceUrl / sceneReferenceUrl。角色参考图走 POST {CHARACTER_UPLOAD_ENDPOINT}，场景参考图走 POST {SCENE_UPLOAD_ENDPOINT}。
      </Alert>

      {uploadError && (
        <Alert severity="error" data-testid="material-preparation-upload-error" data-no-local-upload-fallback="true" data-input-retained="true" sx={{ mb: 2 }}>
          {uploadError} 请检查登录态、BOS 配置和文件大小。
        </Alert>
      )}

      <Card variant="outlined" data-testid="material-preparation-status-card" data-source-endpoint={PROJECT_GET_ENDPOINT} data-no-local-project-fallback="true" sx={{ mb: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Chip label={charUrl ? '角色参考图已准备' : '角色参考图缺失'} color={charUrl ? 'success' : 'warning'} />
            <Chip label={sceneUrl ? '场景参考图已准备' : '场景参考图缺失'} color={sceneUrl ? 'success' : 'warning'} />
            <Chip label={project?.shotListId ? `分镜 #${project.shotListId}` : '未关联分镜'} variant="outlined" />
          </Stack>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <UploadCard type="character" label="角色参考图" icon={<PersonIcon fontSize="inherit" />} uploading={uploadingChar} url={charUrl} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <UploadCard type="scene" label="场景参考图" icon={<SceneIcon fontSize="inherit" />} uploading={uploadingScene} url={sceneUrl} />
        </Grid>
      </Grid>

      {!!projectId && (
        <Stack direction="row" justifyContent="flex-end" sx={{ mt: 2 }}>
          <Button
            variant="contained"
            onClick={() => navigate(`${shortvideoRoutes.materialProduction}?projectId=${projectId}`)}
            data-testid="material-preparation-open-production-button"
            data-target-route={`${shortvideoRoutes.materialProduction}?projectId=${projectId}`}
          >
            去素材生产
          </Button>
        </Stack>
      )}

      <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} maxWidth="md">
        <DialogTitle>{previewType === 'character' ? '角色参考图' : '场景参考图'}</DialogTitle>
        <DialogContent>
          <Box component="img" src={previewUrl} alt="preview" sx={{ width: '100%', borderRadius: 1 }} />
        </DialogContent>
        <DialogActions><Button onClick={() => setPreviewOpen(false)}>关闭</Button></DialogActions>
      </Dialog>
    </Box>
  )
}
