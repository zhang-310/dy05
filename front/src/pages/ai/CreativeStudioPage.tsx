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
  Alert,
  AlertTitle,
  Divider,
  Chip,
} from '@mui/material'
import ImageIcon from '@mui/icons-material/Image'
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import HistoryIcon from '@mui/icons-material/History'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useMutation, useQuery } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'
import { PageHeader } from '@/components/base'
import type {
  AiMediaImageResult,
  AiMediaAudioResult,
  AiMediaVideoResult,
  AiMediaTtsVoiceInfo,
  AiMediaImageHistory,
} from '@/types/ai'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray } from '@/utils/response-normalize'

const MEDIA_ENDPOINTS = {
  text2img: '/ai/media/image/text2img',
  imageHistory: '/ai/media/image/history',
  ttsVoices: '/ai/media/tts/voices',
  ttsGenerate: '/ai/media/tts/generate',
  videoFrames: '/ai/media/video/generate-from-frames',
} as const
const MEDIA_READY_ENDPOINTS = Object.values(MEDIA_ENDPOINTS).join('|')
const MEDIA_UNSUPPORTED_ENDPOINTS = [
  '/ai/media/mock',
  '/ai/media/local-image',
  '/ai/media/local-history',
  '/ai/media/local-tts',
  '/ai/media/local-video',
  '/ai/media/text2video',
  '/ai/media/video/text2video',
  '/ai/media/video/generate-from-prompt',
  '/ai/media/video/local-render',
  '/ai/media/tts/local-voices',
  '/ai/media/image/local-history',
  '/ai/media/image/local-save',
  '/ai/generate/image',
  '/ai/generate/video',
].join('|')

function ContractAlert() {
  return (
    <Alert
      severity="info"
      data-testid="creative-studio-boundary-contract"
      data-ready-endpoints={MEDIA_READY_ENDPOINTS}
      data-unsupported-endpoints={MEDIA_UNSUPPORTED_ENDPOINTS}
      data-no-local-media-fallback="true"
      data-no-text-to-video-synthesis="true"
    >
      <AlertTitle>真实接口边界</AlertTitle>
      文生图、语音合成、音色列表和图片历史均走 <code>/api/v1/ai/media</code>；
      视频仅支持 <code>POST {MEDIA_ENDPOINTS.videoFrames}</code> 首尾帧生成片段。
      若供应商、额度或存储未配置，页面会展示后端错误，不再伪造生成结果。
    </Alert>
  )
}

function ResultFallback({ value }: { value: unknown }) {
  return (
    <Box component="pre" sx={{ fontSize: 12, whiteSpace: 'pre-wrap', m: 0 }}>
      {JSON.stringify(value, null, 2)}
    </Box>
  )
}

function Text2ImgTab() {
  const toast = useToast()
  const [prompt, setPrompt] = useState('')
  const [style, setStyle] = useState('realistic')
  const [result, setResult] = useState<AiMediaImageResult | null>(null)
  const [historyOpen, setHistoryOpen] = useState(false)

  const {
    data: imageHistory,
    isLoading: historyLoading,
    isError: historyIsError,
    error: historyError,
    refetch: refetchHistory,
  } = useQuery({
    queryKey: ['media-image-history', 0, 10],
    queryFn: async () => aiApi.mediaImageHistory({ page: 0, size: 10 }),
    enabled: historyOpen,
  })

  const mutation = useMutation({
    mutationFn: async (): Promise<AiMediaImageResult> => aiApi.text2img({ prompt, style }),
    onSuccess: (data) => {
      setResult(data)
    },
    onError: (e) => toast(`图片生成失败：${getErrorMessage(e)}`, 'error'),
  })

  return (
    <Stack
      spacing={2}
      sx={{ mt: 2 }}
      data-testid="creative-studio-image-tab"
      data-ready-endpoints={`${MEDIA_ENDPOINTS.text2img}|${MEDIA_ENDPOINTS.imageHistory}`}
      data-no-local-image-fallback="true"
    >
      <Alert
        severity="info"
        data-testid="creative-studio-image-contract"
        data-source-endpoint={MEDIA_ENDPOINTS.text2img}
        data-history-endpoint={MEDIA_ENDPOINTS.imageHistory}
        data-no-local-image-fallback="true"
      >
        图片链路调用 <code>POST {MEDIA_ENDPOINTS.text2img}</code>，历史记录调用 <code>POST {MEDIA_ENDPOINTS.imageHistory}</code>。
        历史只展示当前登录用户最近生成记录。
      </Alert>
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

      {mutation.isError ? (
        <Alert
          severity="error"
          data-testid="creative-studio-image-error"
          data-source-endpoint={MEDIA_ENDPOINTS.text2img}
          data-no-local-image-fallback="true"
          data-input-retained="true"
        >
          图片生成失败（POST {MEDIA_ENDPOINTS.text2img}）：{getErrorMessage(mutation.error)}。请检查图像生成供应商配置、AI 额度和对象存储写入权限；当前 Prompt 和风格会保留。
        </Alert>
      ) : null}

      <Collapse in={historyOpen}>
        <Card
          variant="outlined"
          data-testid="creative-studio-image-history"
          data-source-endpoint={MEDIA_ENDPOINTS.imageHistory}
          data-no-local-history-fallback="true"
        >
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="subtitle2">图像生成历史（本页拉取）</Typography>
              <Button
                size="small"
                startIcon={<RefreshIcon fontSize="small" />}
                onClick={() => void refetchHistory()}
                disabled={historyLoading}
              >
                刷新
              </Button>
            </Stack>
            {historyLoading ? (
              <Typography color="text.secondary">加载中…</Typography>
            ) : historyIsError ? (
              <Alert
                severity="error"
                data-testid="creative-studio-image-history-error"
                data-no-local-history-fallback="true"
              >
                历史记录加载失败（POST {MEDIA_ENDPOINTS.imageHistory}）：{getErrorMessage(historyError)}。请检查登录态和 `ai_image_generation` 历史写入。
              </Alert>
            ) : normalizeArray<AiMediaImageHistory>(imageHistory).length > 0 ? (
              <Stack spacing={1}>
                {normalizeArray<AiMediaImageHistory>(imageHistory).map((h) => (
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
              <Typography color="text.secondary" data-testid="creative-studio-image-history-empty" data-no-local-history-fallback="true">暂无历史</Typography>
            )}
          </CardContent>
        </Card>
      </Collapse>

      {result && (
        <Card
          variant="outlined"
          data-testid="creative-studio-image-result"
          data-source-endpoint={MEDIA_ENDPOINTS.text2img}
          data-no-client-image-synthesis="true"
        >
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
              <ResultFallback value={result} />
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

  const {
    data: voices = [],
    isLoading: voicesLoading,
    isError: voicesIsError,
    error: voicesError,
    refetch: refetchVoices,
  } = useQuery({
    queryKey: ['media-tts-voices'],
    queryFn: () => aiApi.mediaTtsVoices(),
  })

  const mutation = useMutation({
    mutationFn: async (): Promise<AiMediaAudioResult> => aiApi.tts({ text, voice: voice || undefined }),
    onSuccess: (data) => setResult(data),
    onError: (e) => toast(`语音合成失败：${getErrorMessage(e)}`, 'error'),
  })

  const voiceOptions: AiMediaTtsVoiceInfo[] = normalizeArray<AiMediaTtsVoiceInfo>(voices)

  return (
    <Stack
      spacing={2}
      sx={{ mt: 2 }}
      data-testid="creative-studio-tts-tab"
      data-ready-endpoints={`${MEDIA_ENDPOINTS.ttsVoices}|${MEDIA_ENDPOINTS.ttsGenerate}`}
      data-no-local-tts-fallback="true"
    >
      <Alert
        severity="info"
        data-testid="creative-studio-tts-contract"
        data-voices-endpoint={MEDIA_ENDPOINTS.ttsVoices}
        data-generate-endpoint={MEDIA_ENDPOINTS.ttsGenerate}
        data-manual-voice-is-server-param="true"
        data-no-local-tts-fallback="true"
      >
        音色列表调用 <code>POST {MEDIA_ENDPOINTS.ttsVoices}</code>；若服务端未返回列表，页面允许手动填写 voice，
        语音合成仍由 <code>POST {MEDIA_ENDPOINTS.ttsGenerate}</code> 校验。
      </Alert>
      {voicesIsError ? (
        <Alert
          severity="warning"
          data-testid="creative-studio-tts-voices-error"
          data-no-local-voice-list-fallback="true"
          data-manual-voice-is-server-param="true"
          action={
            <Button color="inherit" size="small" onClick={() => void refetchVoices()}>
              重试
            </Button>
          }
        >
          音色列表加载失败（POST {MEDIA_ENDPOINTS.ttsVoices}）：{getErrorMessage(voicesError)}。可先手动填写 voice；提交后以后端供应商返回为准。
        </Alert>
      ) : null}
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
      {mutation.isError ? (
        <Alert
          severity="error"
          data-testid="creative-studio-tts-error"
          data-source-endpoint={MEDIA_ENDPOINTS.ttsGenerate}
          data-no-local-tts-fallback="true"
          data-input-retained="true"
        >
          语音合成失败（POST {MEDIA_ENDPOINTS.ttsGenerate}）：{getErrorMessage(mutation.error)}。请检查 TTS Provider、额度和音色 ID；当前文本和 voice 会保留。
        </Alert>
      ) : null}
      {result && (
        <Card
          variant="outlined"
          data-testid="creative-studio-tts-result"
          data-source-endpoint={MEDIA_ENDPOINTS.ttsGenerate}
          data-no-client-audio-synthesis="true"
        >
          <CardContent>
            <Typography variant="subtitle2" mb={1}>
              合成结果
            </Typography>
            {result.audioUrl ? (
              <Box component="audio" controls src={result.audioUrl} sx={{ width: '100%' }} />
            ) : (
              <ResultFallback value={result} />
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
    onError: (e) => toast(`视频生成失败：${getErrorMessage(e)}`, 'error'),
  })

  const submit = () => {
    if (!startFrameUrl.trim() || !endFrameUrl.trim()) {
      toast('请填写首帧与尾帧图片 URL', 'error')
      return
    }
    mutation.mutate()
  }

  return (
    <Stack
      spacing={2}
      sx={{ mt: 2 }}
      data-testid="creative-studio-video-tab"
      data-ready-endpoints={MEDIA_ENDPOINTS.videoFrames}
      data-no-text-to-video-synthesis="true"
      data-no-local-video-fallback="true"
    >
      <Typography variant="body2" color="text.secondary">
        本能力对应后端「首尾帧生成视频」，需可访问的图片 URL（非文生视频）。接口：POST {MEDIA_ENDPOINTS.videoFrames}
      </Typography>
      <Alert
        severity="info"
        data-testid="creative-studio-video-contract"
        data-source-endpoint={MEDIA_ENDPOINTS.videoFrames}
        data-no-text-to-video-synthesis="true"
        data-no-local-video-fallback="true"
      >
        当前后端没有“纯文本生成视频”接口；这里必须提交首帧和尾帧 URL，后端会生成过渡片段并记录 AI 调用日志。
      </Alert>
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
      {mutation.isError ? (
        <Alert
          severity="error"
          data-testid="creative-studio-video-error"
          data-source-endpoint={MEDIA_ENDPOINTS.videoFrames}
          data-no-local-video-fallback="true"
          data-input-retained="true"
        >
          视频生成失败（POST {MEDIA_ENDPOINTS.videoFrames}）：{getErrorMessage(mutation.error)}。请检查首尾帧 URL 是否可访问，以及视频生成 Provider 是否配置；当前首尾帧和时长会保留。
        </Alert>
      ) : null}
      {result && (
        <Card
          variant="outlined"
          data-testid="creative-studio-video-result"
          data-source-endpoint={MEDIA_ENDPOINTS.videoFrames}
          data-no-client-video-synthesis="true"
        >
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
              <ResultFallback value={result} />
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
    <Box
      data-testid="creative-studio-page"
      data-ready-endpoints={MEDIA_READY_ENDPOINTS}
      data-unsupported-endpoints={MEDIA_UNSUPPORTED_ENDPOINTS}
      data-no-local-media-fallback="true"
      data-no-text-to-video-synthesis="true"
      sx={{ p: 2 }}
    >
      <PageHeader
        title="创意工坊"
        subtitle="文生图、语音合成和首尾帧视频均走 AI 多媒体真实接口；未配置供应商时展示降级错误。"
        breadcrumbs={[{ label: 'AI中心' }, { label: '创意工坊' }]}
      />
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <Card variant="outlined" sx={{ flex: 1 }} data-testid="creative-studio-capability-image" data-source-endpoint={MEDIA_ENDPOINTS.text2img}>
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <ImageIcon color="primary" fontSize="small" />
              <Typography variant="subtitle2">图片</Typography>
              <Chip size="small" label="text2img" />
            </Stack>
            <Typography variant="body2" color="text.secondary">
              依赖图像生成供应商、AI 额度和对象存储回写。
            </Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }} data-testid="creative-studio-capability-tts" data-source-endpoint={MEDIA_ENDPOINTS.ttsGenerate}>
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <RecordVoiceOverIcon color="primary" fontSize="small" />
              <Typography variant="subtitle2">语音</Typography>
              <Chip size="small" label="tts" />
            </Stack>
            <Typography variant="body2" color="text.secondary">
              音色列表可降级为手动 voice，合成仍以后端校验为准。
            </Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }} data-testid="creative-studio-capability-video" data-source-endpoint={MEDIA_ENDPOINTS.videoFrames} data-no-text-to-video-synthesis="true">
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <VideoLibraryIcon color="primary" fontSize="small" />
              <Typography variant="subtitle2">视频</Typography>
              <Chip size="small" label="frames" />
            </Stack>
            <Typography variant="body2" color="text.secondary">
              当前只支持首尾帧生成，不伪装纯文案成片。
            </Typography>
          </CardContent>
        </Card>
      </Stack>
      <ContractAlert />
      <Divider sx={{ mt: 2 }} />
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
