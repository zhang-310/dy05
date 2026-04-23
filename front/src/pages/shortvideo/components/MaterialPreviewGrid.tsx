import {
  Box,
  Typography,
  Card,
  CardContent,
  CardMedia,
  Grid,
  Checkbox,
  Button,
} from '@mui/material'
import { Star as StarIcon } from '@mui/icons-material'
import { BatchOperationPanel } from '@/components/shortvideo/BatchOperationPanel'

export interface KeyframeItem {
  shotId?: number
  shotNumber?: number
  imageUrl?: string
  endFrameUrl?: string
}

export interface VideoItem {
  shotNumber?: number
  videoUrl?: string
}

export interface VoiceItem {
  shotNumber?: number
  audioUrl?: string
}

export interface MaterialPreviewGridProps {
  keyframes: KeyframeItem[]
  videos: VideoItem[]
  voices: VoiceItem[]
  selectedKeyframeIds: Set<string>
  qualityScores: Record<number, { grade: string; score: number }>
  loading: boolean
  onSelectKeyframe: (id: string, checked: boolean) => void
  onSelectAllKeyframes: (checked: boolean) => void
  onBatchDeleteKeyframes: () => void
  onBatchRetryKeyframes: () => void
  onClearSelection: () => void
  onEvaluateQuality: (videoUrl: string, shotNumber: number) => void
}

export function MaterialPreviewGrid({
  keyframes,
  videos,
  voices,
  selectedKeyframeIds,
  qualityScores,
  loading,
  onSelectKeyframe,
  onSelectAllKeyframes,
  onBatchDeleteKeyframes,
  onBatchRetryKeyframes,
  onClearSelection,
  onEvaluateQuality,
}: MaterialPreviewGridProps) {
  return (
    <Box>
      {keyframes.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>
            关键帧预览
          </Typography>
          <BatchOperationPanel
            selectedIds={Array.from(selectedKeyframeIds)}
            totalCount={keyframes.length}
            onSelectAll={onSelectAllKeyframes}
            onBatchDelete={onBatchDeleteKeyframes}
            onBatchRegenerate={onBatchRetryKeyframes}
            onClear={onClearSelection}
            loading={loading}
          />
          <Grid container spacing={2}>
            {keyframes.map((k, i) => {
              const kid = String(k.shotId ?? `idx-${i}`)
              const hasBoth = !!(k.imageUrl && k.endFrameUrl)
              return (
                <Grid item xs={6} md={4} key={kid}>
                  <Card variant="outlined" sx={{ display: 'flex', overflow: 'hidden' }}>
                    <Checkbox
                      size="small"
                      checked={selectedKeyframeIds.has(kid)}
                      onChange={(e) => onSelectKeyframe(kid, e.target.checked)}
                      sx={{ alignSelf: 'flex-start', pt: 1 }}
                    />
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                        {k.imageUrl && (
                          <Box sx={{ flex: 1, minWidth: 0 }}>
                            <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>
                              首帧
                            </Typography>
                            <CardMedia
                              component="img"
                              height="80"
                              image={k.imageUrl}
                              alt={`shot ${k.shotNumber} 首帧`}
                              sx={{ objectFit: 'cover' }}
                            />
                          </Box>
                        )}
                        {k.endFrameUrl && (
                          <Box sx={{ flex: 1, minWidth: 0 }}>
                            <Typography variant="caption" color="text.secondary" sx={{ fontSize: 10 }}>
                              尾帧
                            </Typography>
                            <CardMedia
                              component="img"
                              height="80"
                              image={k.endFrameUrl}
                              alt={`shot ${k.shotNumber} 尾帧`}
                              sx={{ objectFit: 'cover' }}
                            />
                          </Box>
                        )}
                      </Box>
                      <CardContent sx={{ py: 0.5 }}>
                        <Typography variant="caption">
                          分镜 {k.shotNumber} {hasBoth ? '首尾帧' : k.imageUrl ? '缺尾帧' : ''}
                        </Typography>
                      </CardContent>
                    </Box>
                  </Card>
                </Grid>
              )
            })}
          </Grid>
        </Box>
      )}

      {videos.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>
            视频片段
          </Typography>
          <Grid container spacing={2}>
            {videos.map((v, i) => {
              const sn = v.shotNumber ?? i + 1
              const q = qualityScores[sn]
              return (
                <Grid item xs={6} md={4} key={i}>
                  <Card variant="outlined">
                    {v.videoUrl && (
                      <CardMedia
                        component="video"
                        height="120"
                        src={v.videoUrl}
                        controls
                        sx={{ objectFit: 'cover' }}
                      />
                    )}
                    <CardContent sx={{ py: 1 }}>
                      <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          flexWrap: 'wrap',
                          gap: 0.5,
                        }}
                      >
                        <Typography variant="caption">
                          分镜 {sn} {v.videoUrl ? '' : '-'}
                        </Typography>
                        {v.videoUrl && (
                          <Button
                            size="small"
                            startIcon={<StarIcon fontSize="small" />}
                            onClick={() => onEvaluateQuality(v.videoUrl!, sn)}
                            sx={{ minWidth: 'auto', py: 0, fontSize: '0.7rem' }}
                          >
                            {q ? `质量 ${q.grade} (${q.score})` : '质量评分'}
                          </Button>
                        )}
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              )
            })}
          </Grid>
        </Box>
      )}

      {voices.length > 0 && (
        <Box>
          <Typography variant="subtitle2" gutterBottom>
            配音列表
          </Typography>
          {voices.map((v, i) => (
            <Card key={i} variant="outlined" sx={{ mb: 1 }}>
              <CardContent sx={{ py: 1 }}>
                <Typography variant="body2">
                  分镜 {v.shotNumber}：{v.audioUrl ? '已生成' : '-'}
                </Typography>
                {v.audioUrl && (
                  <Box
                    component="audio"
                    src={v.audioUrl}
                    controls
                    sx={{ width: '100%', mt: 1 }}
                  />
                )}
              </CardContent>
            </Card>
          ))}
        </Box>
      )}
    </Box>
  )
}
