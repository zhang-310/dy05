import { useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Button, Typography, CircularProgress,
  Grid, Alert, Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material'
import { Person as PersonIcon, Landscape as SceneIcon, CloudUpload as UploadIcon } from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

type RefType = 'character' | 'scene'

export default function MaterialPreparationPage() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null
  const [uploadingChar, setUploadingChar] = useState(false)
  const [uploadingScene, setUploadingScene] = useState(false)
  const [charUrl, setCharUrl] = useState('')
  const [sceneUrl, setSceneUrl] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewUrl, setPreviewUrl] = useState('')
  const [previewType, setPreviewType] = useState<RefType>('character')
  const charInputRef = useRef<HTMLInputElement>(null)
  const sceneInputRef = useRef<HTMLInputElement>(null)

  const handleUpload = async (file: File, type: RefType) => {
    if (!projectId) {
      toast('请先关联项目', 'warning')
      return
    }
    const setter = type === 'character' ? setUploadingChar : setUploadingScene
    setter(true)
    try {
      const baseId = `p${projectId}-default`
      const data = type === 'character'
        ? await shortvideoApi.uploadReferenceCharacter({ characterId: baseId, projectId, file })
        : await shortvideoApi.uploadReferenceScene({ sceneId: baseId, projectId, file })
      const url = String(data.url ?? '')
      if (type === 'character') setCharUrl(url)
      else setSceneUrl(url)
      toast('上传成功', 'success')
    } catch {
      toast('上传失败（需登录且 BOS 已配置）', 'error')
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
    <Card variant="outlined">
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
        >
          {url ? '重新上传' : '上传参考图'}
        </Button>
      </CardContent>
    </Card>
  )

  return (
    <Box>
      <PageHeader
        title="素材准备"
        breadcrumbs={[{ label: '短视频' }, { label: '素材准备' }]}
        subtitle={projectId ? `项目 #${projectId}` : undefined}
      />

      {!projectId && (
        <Alert severity="warning" sx={{ mb: 2 }}>请从项目工作台进入，以关联项目</Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6}>
          <UploadCard type="character" label="角色参考图" icon={<PersonIcon fontSize="inherit" />} uploading={uploadingChar} url={charUrl} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <UploadCard type="scene" label="场景参考图" icon={<SceneIcon fontSize="inherit" />} uploading={uploadingScene} url={sceneUrl} />
        </Grid>
      </Grid>

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
