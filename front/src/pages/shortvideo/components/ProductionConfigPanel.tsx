import {
  Box,
  Typography,
  Button,
  CircularProgress,
  Alert,
  LinearProgress,
} from '@mui/material'
import {
  Image as ImageIcon,
  Mic as MicIcon,
  Movie as MovieIcon,
} from '@mui/icons-material'
import CameraControlPanel, { type CameraTypeCode } from '@/components/shortvideo/CameraControlPanel'
import QualitySelector from '@/components/shortvideo/QualitySelector'

export interface ProductionConfigPanelProps {
  quality: string
  defaultCamera: CameraTypeCode
  loading: boolean
  error: string
  progress: { percent: number; message: string } | null
  shotsCount: number
  keyframeCount: number
  onQualityChange: (quality: string) => void
  onDefaultCameraChange: (camera: CameraTypeCode) => void
  onGenerateKeyframes: () => void
  onGenerateVoices: () => void
  onGenerateVideos: () => void
  onGenerateVideosAsync: () => void
}

export function ProductionConfigPanel({
  quality,
  defaultCamera,
  loading,
  error,
  progress,
  shotsCount,
  keyframeCount,
  onQualityChange,
  onDefaultCameraChange,
  onGenerateKeyframes,
  onGenerateVoices,
  onGenerateVideos,
  onGenerateVideosAsync,
}: ProductionConfigPanelProps) {
  return (
    <Box>
      {keyframeCount > 0 && (
        <Box sx={{ mb: 3, p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
          <Typography variant="subtitle2" gutterBottom>
            画质与运镜（图生视频前设置）
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                画质级别
              </Typography>
              <QualitySelector value={quality} onChange={onQualityChange} />
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                默认运镜
              </Typography>
              <CameraControlPanel value={defaultCamera} onChange={onDefaultCameraChange} />
            </Box>
          </Box>
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {progress && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {progress.message}
          </Typography>
          <LinearProgress
            variant="determinate"
            value={progress.percent}
            sx={{ height: 8, borderRadius: 1 }}
          />
        </Box>
      )}

      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <Button
          variant="contained"
          startIcon={loading ? <CircularProgress size={20} /> : <ImageIcon />}
          onClick={onGenerateKeyframes}
          disabled={loading || shotsCount === 0}
        >
          生成关键帧
        </Button>
        <Button
          variant="contained"
          color="secondary"
          startIcon={loading ? <CircularProgress size={20} /> : <MicIcon />}
          onClick={onGenerateVoices}
          disabled={loading || shotsCount === 0}
        >
          生成配音
        </Button>
        <Button
          variant="outlined"
          startIcon={loading ? <CircularProgress size={20} /> : <MovieIcon />}
          onClick={onGenerateVideos}
          disabled={loading || keyframeCount === 0}
        >
          图生视频
        </Button>
        <Button
          variant="outlined"
          color="secondary"
          startIcon={loading ? <CircularProgress size={20} /> : <MovieIcon />}
          onClick={onGenerateVideosAsync}
          disabled={loading || keyframeCount === 0}
        >
          后台生成
        </Button>
      </Box>
    </Box>
  )
}
