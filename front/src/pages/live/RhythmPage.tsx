import { useState } from 'react'
import { Box, Card, CardContent, Typography, Stack, Button, TextField, Slider, Divider, LinearProgress, Chip } from '@mui/material'
import SaveIcon from '@mui/icons-material/Save'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import { liveApi } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation } from '@tanstack/react-query'

interface TimeSlot {
  label?: string
  duration?: number
  [key: string]: unknown
}

interface TopicRatio {
  topic?: string
  ratio?: number
  [key: string]: unknown
}

interface RhythmData {
  timeSlots?: TimeSlot[]
  topicRatios?: TopicRatio[]
  [key: string]: unknown
}

export default function RhythmPage() {
  const toast = useToast()
  const [sessionId, setSessionId] = useState<number>(0)
  const [draftId, setDraftId] = useState('')

  const { data: rhythm, isLoading, refetch } = useQuery({
    queryKey: ['live-rhythm', sessionId],
    queryFn: () => liveApi.rhythmGet(sessionId),
    enabled: sessionId > 0,
  })

  const saveMut = useMutation({
    mutationFn: (p: Record<string, unknown>) => liveApi.rhythmSave(p),
    onSuccess: () => { toast('节奏配置已保存', 'success'); refetch() },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const suggestMut = useMutation({
    mutationFn: (sid: number) => liveApi.rhythmSuggest(sid),
    onSuccess: () => { toast('AI 建议已生成', 'success'); refetch() },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const handleLoad = () => {
    const id = Number(draftId)
    if (id > 0) setSessionId(id)
    else toast('请输入有效的场次 ID', 'error')
  }

  const rhythmData = (rhythm ?? undefined) as RhythmData | undefined

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', py: 2 }}>
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField size="small" label="直播场次 ID" value={draftId}
              onChange={e => setDraftId(e.target.value)} sx={{ width: 160 }} />
            <Button variant="outlined" size="small" onClick={handleLoad}>加载场次节奏</Button>
          </Stack>
        </CardContent>
      </Card>

      {isLoading && <LinearProgress />}

      {rhythmData && (
        <Card variant="outlined">
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">节奏配置</Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="outlined" startIcon={<AutoFixHighIcon />}
                  onClick={() => suggestMut.mutate(sessionId)} disabled={suggestMut.isPending}>
                  AI 建议
                </Button>
                <Button size="small" variant="contained" startIcon={<SaveIcon />}
                  onClick={() => saveMut.mutate({ ...rhythmData, sessionId })} disabled={saveMut.isPending}>
                  保存
                </Button>
              </Stack>
            </Stack>
            <Divider sx={{ mb: 2 }} />

            {/* 时间段配置 */}
            {Array.isArray(rhythmData.timeSlots) && (
              <Box mb={3}>
                <Typography variant="subtitle2" mb={1}>时间段分配</Typography>
                <Stack spacing={2}>
                  {(rhythmData.timeSlots ?? []).map((slot, i) => (
                    <Box key={i}>
                      <Stack direction="row" justifyContent="space-between" mb={0.5}>
                        <Chip label={String(slot.label ?? `时段 ${i + 1}`)} size="small" />
                        <Typography variant="caption">{String(slot.duration ?? 0)} 分钟</Typography>
                      </Stack>
                      <Slider
                        value={Number(slot.duration ?? 0)}
                        min={0} max={120} step={5}
                        valueLabelDisplay="auto"
                        size="small"
                      />
                    </Box>
                  ))}
                </Stack>
              </Box>
            )}

            {/* 话题占比 */}
            {Array.isArray(rhythmData.topicRatios) && (
              <Box>
                <Typography variant="subtitle2" mb={1}>话题占比</Typography>
                <Stack spacing={2}>
                  {(rhythmData.topicRatios ?? []).map((topic, i) => (
                    <Box key={i}>
                      <Stack direction="row" justifyContent="space-between" mb={0.5}>
                        <Typography variant="body2">{String(topic.topic ?? `话题 ${i + 1}`)}</Typography>
                        <Typography variant="caption">{String(topic.ratio ?? 0)}%</Typography>
                      </Stack>
                      <LinearProgress variant="determinate" value={Number(topic.ratio ?? 0)} sx={{ borderRadius: 1, height: 8 }} />
                    </Box>
                  ))}
                </Stack>
              </Box>
            )}

            {/* 原始数据展示（当无结构化字段时） */}
            {!Array.isArray(rhythmData.timeSlots) && !Array.isArray(rhythmData.topicRatios) && (
              <Box component="pre" sx={{ fontSize: 12, bgcolor: 'grey.100', p: 2, borderRadius: 1, overflow: 'auto' }}>
                {JSON.stringify(rhythmData, null, 2)}
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {!rhythmData && !isLoading && sessionId > 0 && (
        <Typography color="text.secondary">该场次暂无节奏配置</Typography>
      )}
    </Box>
  )
}
