import { useState } from 'react'
import {
  Box,
  Tab,
  Tabs,
  TextField,
  Button,
  Stack,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  MenuItem,
  Collapse,
} from '@mui/material'
import ImageIcon from '@mui/icons-material/Image'
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import HistoryIcon from '@mui/icons-material/History'
import { useMutation, useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import type {
  AiMediaImageResult,
  AiMediaAudioResult,
  AiMediaVideoResult,
  AiMediaTtsVoiceInfo,
  AiMediaImageHistory,
} from '@/types/ai'
import { useToast } from '@/contexts/ToastContext'

function Text2ImgTab() {
  const toast = useToast()
  const [prompt, setPrompt] = useState('')
  const [style, setStyle] = useState('realistic')
  const [result, setResult] = useState<AiMediaImageResult | null>(null)
  const [historyOpen, setHistoryOpen] = useState(false)

  const { data: imageHistory, isLoading: historyLoading, refetch: refetchHistory } = useQuery({
    queryKey: ['media-image-history', 0, 10],
    queryFn: async () => aiApi.mediaImageHistory({ page: 0, size: 10 }),
    enabled: historyOpen,
  })

  const mutation = useMutation({
    mutationFn: async (): Promise<AiMediaImageResult> => aiApi.text2img({ prompt, style }),
    onSuccess: (data) => {
      setResult(data)
    },
    onError: (e: Error) => toast(e.message || '图片生成失败', 'error'),
  })

  return (
    <Stack spacing={2} sx={{ mt: 2 }}>
      <TextField
        label="描述词 Prompt"
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        multiline
        minRows={3}
        fullWidth
        size="small"
        placeholder="一位身着汉服的女性，背景是江南水乡..."
      />
      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap alignItems="center">
        <TextField label="风格" value={style} onChange={(e) => setStyle(e.target.value)} size="small" sx={{ width: 200 }} />
        <Button
          variant="contained"
          startIcon={<ImageIcon />}
          onClick={() => mutation.mutate()}
          disabled={!prompt || mutation.isPending}
        >
          {mutation.isPending ? <CircularProgress size={18} sx={{ mr: 1 }} /> : null}
          生成图片
        </Button>
        <Button
          variant="outlined"
          size="small"
          startIcon={<HistoryIcon />}
          onClick={() => {
            setHistoryOpen(true)
            void refetchHistory()
          }}
        >
          最近生成记录
        </Button>
      </Stack>

      <Collapse in={historyOpen}>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>
              图像生成历史（本页拉取）
            </Typography>
            {historyLoading ? (
              <Typography color="text.secondary">加载中…</Typography>
            ) : imageHistory && imageHistory.length > 0 ? (
              <Stack spacing={1}>
                {imageHistory.map((h: AiMediaImageHistory) => (
                  <Stack key={h.id} direction="row" spacing={1} alignItems="flex-start" flexWrap="wrap">
                    <Typography variant="caption" color="text.secondary">
                      #{h.id}
                    </Typography>
                    {h.imageUrl ? (
                      <Box
                        component="img"
                        src={h.imageUrl}
                        alt=""
                        sx={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 1 }}
                      />
                    ) : null}
                    <Typography variant="body2" sx={{ flex: 1, minWidth: 120 }}>
                      {h.prompt ?? '—'}
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            ) : (
              <Typography color="text.secondary">暂无历史</Typography>
            )}
          </CardContent>
        </Card>
      </Collapse>

      {result && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>
              生成结果
            </Typography>
            {result.imageUrl ? (
              <Box
                component="img"
                src={result.imageUrl}
                alt="generated"
                sx={{ maxWidth: '100%', maxHeight: 400, borderRadius: 1, display: 'block' }}
              />
            ) : (
              <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', m: 0 }}>
                {JSON.stringify(result, null, 2)}
              </Box>
            )}
            {result.generationTime != null ? (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                耗时 {result.generationTime} ms
              </Typography>
            ) : null}
          </CardContent>
        </Card>
      )}
    </Stack>
  )
}

function TtsTab() {
  const toast = useToast()
  const [text, setText] = useState('')
  const [voice, setVoice] = useState('')
  const [result, setResult] = useState<AiMediaAudioResult | null>(null)

  const { data: voices = [], isLoading: voicesLoading } = useQuery({
    queryKey: ['media-tts-voices'],
    queryFn: () => aiApi.mediaTtsVoices(),
  })

  const mutation = useMutation({
    mutationFn: async (): Promise<AiMediaAudioResult> => aiApi.tts({ text, voice: voice || undefined }),
    onSuccess: (data) => setResult(data),
    onError: (e: Error) => toast(e.message || '语音合成失败', 'error'),
  })

  const voiceOptions: AiMediaTtsVoiceInfo[] = Array.isArray(voices) ? voices : []

  return (
    <Stack spacing={2} sx={{ mt: 2 }}>
      <TextField
        label="文本内容"
        value={text}
        onChange={(e) => setText(e.target.value)}
        multiline
        minRows={4}
        fullWidth
        size="small"
        placeholder="请输入需要转语音的文字..."
      />
      {voiceOptions.length > 0 ? (
        <TextField
          select
          label="音色"
          value={voice}
          onChange={(e) => setVoice(e.target.value)}
          size="small"
          sx={{ minWidth: 320 }}
          disabled={voicesLoading}
          helperText={voicesLoading ? '加载音色列表…' : '选「默认」则传空，由服务端选择'}
        >
          <MenuItem value="">
            <em>默认（由服务端选择）</em>
          </MenuItem>
          {voiceOptions.map((v) => (
            <MenuItem key={v.id} value={v.id}>
              {v.name}
              {v.language ? ` (${v.language})` : ''}
            </MenuItem>
          ))}
        </TextField>
      ) : (
        <TextField
          label="音色 voice（可选）"
          value={voice}
          onChange={(e) => setVoice(e.target.value)}
          size="small"
          sx={{ minWidth: 320 }}
          placeholder="zh-CN-XiaoxiaoNeural"
          helperText={voicesLoading ? '加载音色列表…' : '未返回列表时可手动填写；留空则服务端默认'}
        />
      )}
      <Button
        variant="contained"
        startIcon={<RecordVoiceOverIcon />}
        onClick={() => mutation.mutate()}
        disabled={!text || mutation.isPending}
        sx={{ alignSelf: 'flex-start' }}
      >
        {mutation.isPending ? <CircularProgress size={18} sx={{ mr: 1 }} /> : null}
        合成语音
      </Button>
      {result && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>
              合成结果
            </Typography>
            {result.audioUrl ? (
              <Box component="audio" controls src={result.audioUrl} sx={{ width: '100%' }} />
            ) : (
              <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', m: 0 }}>
                {JSON.stringify(result, null, 2)}
              </Box>
            )}
            {result.duration != null ? (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                时长约 {result.duration} ms
                {result.fileSize != null ? ` · ${result.fileSize} bytes` : ''}
              </Typography>
            ) : null}
          </CardContent>
        </Card>
      )}
    </Stack>
  )
}

function VideoFromFramesTab() {
  const toast = useToast()
  const [startFrameUrl, setStartFrameUrl] = useState('')
  const [endFrameUrl, setEndFrameUrl] = useState('')
  const [durationSec, setDurationSec] = useState('5')
  const [result, setResult] = useState<AiMediaVideoResult | null>(null)

  const mutation = useMutation({
    mutationFn: async (): Promise<AiMediaVideoResult> =>
      aiApi.videoGenerateFromFrames({
        startFrameUrl: startFrameUrl.trim(),
        endFrameUrl: endFrameUrl.trim(),
        durationSec: Math.max(1, Math.min(60, Number(durationSec) || 5)),
      }),
    onSuccess: (data) => setResult(data),
    onError: (e: Error) => toast(e.message || '视频生成失败', 'error'),
  })

  const submit = () => {
    if (!startFrameUrl.trim() || !endFrameUrl.trim()) {
      toast('请填写首帧与尾帧图片 URL', 'error')
      return
    }
    mutation.mutate()
  }

  return (
    <Stack spacing={2} sx={{ mt: 2 }}>
      <Typography variant="body2" color="text.secondary">
        本能力对应后端「首尾帧生成视频」，需可访问的图片 URL（非文生视频）。接口：POST /api/v1/ai/media/video/generate-from-frames
      </Typography>
      <TextField
        label="首帧图片 URL"
        value={startFrameUrl}
        onChange={(e) => setStartFrameUrl(e.target.value)}
        fullWidth
        size="small"
        placeholder="https://..."
      />
      <TextField
        label="尾帧图片 URL"
        value={endFrameUrl}
        onChange={(e) => setEndFrameUrl(e.target.value)}
        fullWidth
        size="small"
        placeholder="https://..."
      />
      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
        <TextField
          label="时长（秒）"
          value={durationSec}
          onChange={(e) => setDurationSec(e.target.value)}
          size="small"
          sx={{ width: 140 }}
          type="number"
          inputProps={{ min: 1, max: 60 }}
        />
        <Button
          variant="contained"
          startIcon={<VideoLibraryIcon />}
          onClick={submit}
          disabled={mutation.isPending}
        >
          {mutation.isPending ? <CircularProgress size={18} sx={{ mr: 1 }} /> : null}
          生成过渡视频
        </Button>
      </Stack>
      {result && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" mb={1}>
              生成结果
            </Typography>
            {result.videoUrl ? (
              <Box
                component="video"
                controls
                src={result.videoUrl}
                sx={{ maxWidth: '100%', maxHeight: 360, display: 'block', borderRadius: 1 }}
              />
            ) : (
              <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', m: 0 }}>
                {JSON.stringify(result, null, 2)}
              </Box>
            )}
            {result.format != null ? (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                格式 {result.format}
                {result.duration != null ? ` · 时长 ${result.duration}` : ''}
              </Typography>
            ) : null}
          </CardContent>
        </Card>
      )}
    </Stack>
  )
}

export default function CreativeStudioPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        创意工坊
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        文生图、语音合成走 <code>/api/v1/ai/media</code>；视频为首尾帧插值片段，非纯文案生成视频。
      </Typography>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tab icon={<ImageIcon fontSize="small" />} iconPosition="start" label="图片生成" />
        <Tab icon={<RecordVoiceOverIcon fontSize="small" />} iconPosition="start" label="语音合成" />
        <Tab icon={<VideoLibraryIcon fontSize="small" />} iconPosition="start" label="首尾帧视频" />
      </Tabs>
      {tab === 0 && <Text2ImgTab />}
      {tab === 1 && <TtsTab />}
      {tab === 2 && <VideoFromFramesTab />}
    </Box>
  )
}
