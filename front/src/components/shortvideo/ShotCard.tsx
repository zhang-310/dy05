import { useState, useEffect } from 'react'
import { Box, Card, CardContent, CardMedia, Typography, IconButton, Collapse, Button } from '@mui/material'
import { Edit as EditIcon, Delete as DeleteIcon, ArrowUpward, ArrowDownward, ExpandMore as ExpandMoreIcon, AutoAwesome as AiIcon } from '@mui/icons-material'
import CameraControlPanel, { type CameraTypeCode } from './CameraControlPanel'
import { recommendCamera } from '@/api/shortvideo'

export interface ShotCardData {
  shotNumber: number
  timeRange?: string
  sceneDescription?: string
  dialogue?: string
  cameraAngle?: string
  cameraType?: string  // 运镜类型 (zoom-in, dolly-in 等)
  action?: string
  mood?: string
  keyframeUrl?: string
}

interface ShotCardProps {
  shot: ShotCardData
  onEdit?: () => void
  onDelete?: () => void
  onMoveUp?: () => void
  onMoveDown?: () => void
  onCameraTypeChange?: (code: CameraTypeCode) => void
  canMoveUp?: boolean
  canMoveDown?: boolean
}

const DEFAULT_CAMERA: CameraTypeCode = 'zoom-in'

/** 统一分镜卡片：预览图 + 场景/台词/机位/情绪 + 可展开运镜微调 */
export function ShotCard({
  shot,
  onEdit,
  onDelete,
  onMoveUp,
  onMoveDown,
  onCameraTypeChange,
  canMoveUp = true,
  canMoveDown = true,
}: ShotCardProps) {
  const [expanded, setExpanded] = useState(false)
  const [aiRecommend, setAiRecommend] = useState<{ primary: string; confidence?: number } | null>(null)
  const cameraType = (shot.cameraType as CameraTypeCode) || DEFAULT_CAMERA
  const hasSceneDesc = !!shot.sceneDescription?.trim()
  const noCameraSet = !shot.cameraType

  useEffect(() => {
    if (!hasSceneDesc || !noCameraSet || !onCameraTypeChange) return
    recommendCamera({ sceneDescription: shot.sceneDescription })
      .then((r) => {
        if (r?.primary) {
          const rec = r.recommendations?.[0]
          setAiRecommend({
            primary: r.primary,
            confidence: rec?.confidence != null ? Math.round(rec.confidence * 100) : undefined,
          })
        }
      })
      .catch((e) => console.error('Camera recommendation failed:', e))
  }, [hasSceneDesc, noCameraSet, onCameraTypeChange, shot.sceneDescription])

  return (
    <Card variant="outlined" sx={{ display: 'flex', overflow: 'hidden' }}>
      {shot.keyframeUrl && (
        <CardMedia
          component="img"
          image={shot.keyframeUrl}
          alt={`分镜 ${shot.shotNumber}`}
          sx={{ width: 120, objectFit: 'cover', flexShrink: 0 }}
        />
      )}
      <CardContent sx={{ flex: 1, py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 0.5 }}>
          <Typography variant="subtitle2">
            分镜 {shot.shotNumber} {shot.timeRange ? `· ${shot.timeRange}` : ''}
          </Typography>
          <Box>
            {onMoveUp && canMoveUp && (
              <IconButton size="small" onClick={onMoveUp}>
                <ArrowUpward fontSize="small" />
              </IconButton>
            )}
            {onMoveDown && canMoveDown && (
              <IconButton size="small" onClick={onMoveDown}>
                <ArrowDownward fontSize="small" />
              </IconButton>
            )}
            {onEdit && (
              <IconButton size="small" onClick={onEdit}>
                <EditIcon fontSize="small" />
              </IconButton>
            )}
            {onDelete && (
              <IconButton size="small" color="error" onClick={onDelete}>
                <DeleteIcon fontSize="small" />
              </IconButton>
            )}
          </Box>
        </Box>
        {shot.sceneDescription && (
          <Typography variant="body2" color="text.secondary">
            场景：{shot.sceneDescription}
          </Typography>
        )}
        {shot.cameraAngle && (
          <Typography variant="caption" color="text.secondary" display="block">
            机位：{shot.cameraAngle}
          </Typography>
        )}
        {shot.action && (
          <Typography variant="caption" color="text.secondary" display="block">
            动作：{shot.action}
          </Typography>
        )}
        {shot.dialogue && (
          <Typography variant="body2" sx={{ mt: 0.5, fontStyle: 'italic' }}>
            {shot.dialogue}
          </Typography>
        )}
        {shot.mood && (
          <Typography variant="caption" color="text.secondary" display="block">
            情绪：{shot.mood}
          </Typography>
        )}
        {onCameraTypeChange && (
          <Box sx={{ mt: 1 }}>
            <IconButton
              size="small"
              onClick={() => setExpanded(!expanded)}
              sx={{ transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)' }}
              aria-label={expanded ? '收起运镜' : '展开运镜'}
            >
              <ExpandMoreIcon fontSize="small" />
            </IconButton>
            <Typography variant="caption" component="span" color="text.secondary">
              运镜：{cameraType}
            </Typography>
            {aiRecommend && noCameraSet && (
              <Button
                size="small"
                startIcon={<AiIcon fontSize="small" />}
                onClick={() => onCameraTypeChange(aiRecommend.primary as CameraTypeCode)}
                sx={{ ml: 0.5, py: 0, minWidth: 'auto', fontSize: '0.7rem' }}
              >
                AI 推荐 {aiRecommend.primary}
                {aiRecommend.confidence != null ? ` (${aiRecommend.confidence}%)` : ''}
              </Button>
            )}
            <Collapse in={expanded}>
              <Box sx={{ mt: 1, p: 1, bgcolor: 'action.hover', borderRadius: 1 }}>
                <CameraControlPanel value={cameraType} onChange={(code) => onCameraTypeChange(code)} />
              </Box>
            </Collapse>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}
