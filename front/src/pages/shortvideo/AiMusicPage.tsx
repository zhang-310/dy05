import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Grid, Stack,
  Button, MenuItem, TextField, Chip, CircularProgress, Divider,
  Alert, Tooltip,
} from '@mui/material'
import MusicNoteIcon from '@mui/icons-material/MusicNote'
import GraphicEqIcon from '@mui/icons-material/GraphicEq'
import DownloadIcon from '@mui/icons-material/Download'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import RefreshIcon from '@mui/icons-material/Refresh'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getErrorMessage } from '@/utils/errorHandler'

const VIDEO_TYPES = ['护肤教程', '彩妆教程', '产品测评', '成分科普', '使用教程', '好物分享']
const BGM_MOODS = ['轻松愉快', '温暖治愈', '专业严肃', '活力动感', '清新自然']
const BGM_DURATIONS = ['15秒', '30秒', '60秒', '90秒']
const BGM_PROVIDERS = ['Suno AI', '网易云音乐', '本地生成']
const SFX_SCENES = ['产品展示', '购买成功', '倒计时', '开场过渡', '结尾引导']
const SFX_DURATIONS = ['1秒', '3秒', '5秒', '10秒']
const MUSIC_BGM_ENDPOINT = '/short-video/music/generate-bgm'
const MUSIC_SFX_ENDPOINT = '/short-video/music/generate-sfx'
const MUSIC_HISTORY_ENDPOINT = '/short-video/music/history'
const MUSIC_READY_ENDPOINTS = [
  MUSIC_BGM_ENDPOINT,
  MUSIC_SFX_ENDPOINT,
  MUSIC_HISTORY_ENDPOINT,
].join('|')
const MUSIC_UNSUPPORTED_ENDPOINTS = [
  '/short-video/music/mock',
  '/short-video/music/local-bgm',
  '/short-video/music/local-sfx',
  '/short-video/music/local-history',
  '/short-video/music/static-history',
  '/short-video/music/placeholder-audio',
  '/short-video/music/browser-synthesis',
].join('|')
const MUSIC_READY_ROUTES = [
  shortvideoRoutes.aiMusic,
  shortvideoRoutes.editing,
].join('|')
const MUSIC_SUPPORTED_ACTIONS = [
  'refresh-music-history',
  'generate-bgm',
  'generate-sfx',
  'preview-audio',
  'download-audio',
  'copy-audio-url',
].join('|')

interface MusicItem {
  id?: number
  name: string
  duration: string
  url?: string
  type: 'bgm' | 'sfx'
  provider?: string
  bpm?: number
  source?: string
  degraded?: boolean
}

interface BgmGenerateResult {
  name?: string
  url?: string
  audioUrl?: string
  musicUrl?: string
  name2?: string
  url2?: string
  provider?: string
  durationMs?: number
  bpm?: number
  [key: string]: unknown
}

interface SfxGenerateResult {
  name?: string
  description?: string
  url?: string
  audioUrl?: string
  durationSec?: number
  [key: string]: unknown
}

function normalizeDuration(value: unknown, fallback: string) {
  if (typeof value === 'number' && value > 0) {
    if (value > 1000) return `${Math.max(1, Math.round(value / 1000))}秒`
    return `${Math.max(1, Math.round(value))}秒`
  }
  return fallback
}

export default function AiMusicPage() {
  const toast = useToast()
  const queryClient = useQueryClient()

  // BGM state
  const [bgmVideoType, setBgmVideoType] = useState('护肤教程')
  const [bgmMood, setBgmMood] = useState('轻松愉快')
  const [bgmDuration, setBgmDuration] = useState('30秒')
  const [bgmProvider, setBgmProvider] = useState('Suno AI')
  const [bgmLoading, setBgmLoading] = useState(false)
  const [bgmResults, setBgmResults] = useState<MusicItem[]>([])
  const [bgmError, setBgmError] = useState('')

  // SFX state
  const [sfxScene, setSfxScene] = useState('产品展示')
  const [sfxDuration, setSfxDuration] = useState('3秒')
  const [sfxLoading, setSfxLoading] = useState(false)
  const [sfxResults, setSfxResults] = useState<MusicItem[]>([])
  const [sfxError, setSfxError] = useState('')

  const {
    data: history = [],
    isLoading: historyLoading,
    isError: historyIsError,
    error: historyError,
    refetch: refetchHistory,
  } = useQuery({
    queryKey: ['music-history'],
    queryFn: () => shortvideoApi.musicHistory({ rows: 10 }),
  })
  const historyItems: MusicItem[] = history.map(h => ({
    name: String(h.name ?? ''),
    duration: String(h.duration ?? ''),
    url: h.url != null ? String(h.url) : undefined,
    type: (h.type === 'bgm' || h.type === 'sfx') ? h.type : 'bgm',
    id: h.id != null ? Number(h.id) : undefined,
    provider: h.provider != null ? String(h.provider) : undefined,
    source: h.source != null ? String(h.source) : undefined,
    degraded: h.degraded === true,
  }))

  const handleGenerateBgm = async () => {
    setBgmLoading(true)
    setBgmError('')
    try {
      const sec = parseInt(bgmDuration.replace(/\D/g, ''), 10) || 30
      const styleDescription = `${bgmVideoType}，情绪：${bgmMood}，服务商偏好：${bgmProvider}`
      const res = await shortvideoApi.generateBgm({
        styleDescription,
        durationSec: sec,
        instrumental: true,
      }) as BgmGenerateResult
      const primaryUrl = String(res.musicUrl ?? res.url ?? res.audioUrl ?? '')
      const items: MusicItem[] = [
        {
          name: String(res.name ?? `${bgmMood}BGM`),
          duration: normalizeDuration(res.durationMs, bgmDuration),
          url: primaryUrl || undefined,
          type: 'bgm' as const,
          provider: res.provider != null ? String(res.provider) : bgmProvider,
          bpm: typeof res.bpm === 'number' ? res.bpm : undefined,
        },
        { name: String(res.name2 ?? ''), duration: bgmDuration, url: String(res.url2 ?? ''), type: 'bgm' as const },
      ].filter(i => i.name)
      setBgmResults(items)
      queryClient.invalidateQueries({ queryKey: ['music-history'] })
      toast(items.length > 0 ? '背景音乐生成成功' : '生成成功，但服务未返回可播放 URL', items.length > 0 ? 'success' : 'warning')
    } catch (error) {
      const message = getErrorMessage(error)
      setBgmError(`背景音乐生成失败（POST ${MUSIC_BGM_ENDPOINT}）：${message}。视频类型、情绪、时长和服务商输入会保留，不使用占位音频。`)
      toast(`背景音乐生成失败：${message}`, 'error')
    } finally {
      setBgmLoading(false)
    }
  }

  const handleGenerateSfx = async () => {
    setSfxLoading(true)
    setSfxError('')
    try {
      const sec = parseInt(sfxDuration.replace(/\D/g, ''), 10) || 5
      const res = await shortvideoApi.generateSfx({ sceneDescription: sfxScene, durationSec: sec })
      const arr = Array.isArray(res) ? res : [res]
      const items: MusicItem[] = (arr as SfxGenerateResult[]).map((r) => ({
        name: String(r.name ?? r.description ?? '音效'),
        duration: normalizeDuration(r.durationSec, sfxDuration),
        url: String(r.audioUrl ?? r.url ?? '') || undefined,
        type: 'sfx' as const,
        provider: 'elevenlabs',
      })).filter(i => i.name)
      setSfxResults(items)
      queryClient.invalidateQueries({ queryKey: ['music-history'] })
      toast(items.length > 0 ? '音效生成成功' : '生成成功，但服务未返回可播放 URL', items.length > 0 ? 'success' : 'warning')
    } catch (error) {
      const message = getErrorMessage(error)
      setSfxError(`音效生成失败（POST ${MUSIC_SFX_ENDPOINT}）：${message}。当前场景和时长会保留，不使用占位音效。`)
      toast(`音效生成失败：${message}`, 'error')
    } finally {
      setSfxLoading(false)
    }
  }
  const handleCopyUrl = (item: MusicItem) => {
    if (!item.url) return
    navigator.clipboard.writeText(item.url).catch(() => {})
    toast('音频地址已复制，可粘贴到成片流程使用', 'success')
  }

  const MusicCard = ({ item }: { item: MusicItem }) => (
    <Box
      data-testid="shortvideo-ai-music-item"
      data-audio-type={item.type}
      data-has-url={item.url ? 'true' : 'false'}
      data-no-local-audio-fallback="true"
      data-supported-actions={MUSIC_SUPPORTED_ACTIONS}
      sx={{ display: 'flex', alignItems: { xs: 'stretch', md: 'center' }, justifyContent: 'space-between', gap: 1.5, py: 1, borderBottom: '1px solid', borderColor: 'divider', flexDirection: { xs: 'column', md: 'row' } }}
    >
      <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: 0, flex: 1 }}>
        {item.type === 'bgm' ? <MusicNoteIcon fontSize="small" color="primary" /> : <GraphicEqIcon fontSize="small" color="secondary" />}
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" fontWeight={600}>{item.name}</Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Typography variant="caption" color="text.secondary">时长：{item.duration || '未知'}</Typography>
            {item.provider && <Typography variant="caption" color="text.secondary">Provider：{item.provider}</Typography>}
            {item.bpm != null && <Typography variant="caption" color="text.secondary">BPM：{item.bpm}</Typography>}
            {item.source && <Typography variant="caption" color="text.secondary">来源：{item.source}</Typography>}
            {item.degraded && <Chip label="服务侧降级" size="small" color="warning" variant="outlined" />}
          </Stack>
        </Box>
      </Stack>
      {item.url ? (
        <Box component="audio" controls src={item.url} sx={{ width: { xs: '100%', md: 240 }, height: 32 }} />
      ) : (
        <Alert severity="warning" data-testid="shortvideo-ai-music-empty-url" data-no-local-audio-fallback="true" sx={{ width: { xs: '100%', md: 240 } }}>
          仅返回任务结果，未拿到可播放 URL
        </Alert>
      )}
      <Stack direction="row" spacing={0.5} justifyContent={{ xs: 'flex-end', md: 'flex-start' }}>
        <Tooltip title={item.url ? '浏览器内试听' : '服务未返回可播放 URL'}>
          <span>
            <Button size="small" variant="outlined" startIcon={<PlayArrowIcon fontSize="small" />} disabled={!item.url}>
              试听
            </Button>
          </span>
        </Tooltip>
        <Button size="small" startIcon={<DownloadIcon fontSize="small" />} disabled={!item.url} component="a" href={item.url} target="_blank" rel="noreferrer" data-testid="shortvideo-ai-music-download-button">
          下载
        </Button>
        <Button size="small" variant="contained" color="primary" startIcon={<ContentCopyIcon fontSize="small" />} disabled={!item.url} onClick={() => handleCopyUrl(item)} data-testid="shortvideo-ai-music-copy-button">
          使用
        </Button>
      </Stack>
    </Box>
  )

  return (
    <Box
      data-testid="shortvideo-ai-music-page"
      data-ready-endpoints={MUSIC_READY_ENDPOINTS}
      data-ready-routes={MUSIC_READY_ROUTES}
      data-supported-actions={MUSIC_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={MUSIC_UNSUPPORTED_ENDPOINTS}
      data-no-local-audio-fallback="true"
      data-input-retained-on-error="true"
    >
      <PageHeader
        title="AI 配乐"
        breadcrumbs={[{ label: '短视频' }, { label: 'AI配乐' }]}
        subtitle={`对接 POST ${MUSIC_BGM_ENDPOINT}|${MUSIC_SFX_ENDPOINT}|${MUSIC_HISTORY_ENDPOINT}，返回音频 URL 后可试听、下载或复制到成片流程。`}
        actions={
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetchHistory()} disabled={historyLoading} data-testid="shortvideo-ai-music-refresh-button" data-source-endpoint={MUSIC_HISTORY_ENDPOINT}>
            刷新历史
          </Button>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="shortvideo-ai-music-boundary-contract"
        data-source-endpoints={MUSIC_READY_ENDPOINTS}
        data-no-local-audio-fallback="true"
        data-supported-actions={MUSIC_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        当前页面负责生成与记录音频素材；项目级 BGM 自动挂载仍需在剪辑/成片流程中选择或粘贴音频 URL。接口失败时保留当前输入和已有结果。
      </Alert>
      <Alert
        severity="warning"
        variant="outlined"
        data-testid="shortvideo-ai-music-degrade-contract"
        data-no-local-audio-fallback="true"
        sx={{ mb: 2 }}
      >
        历史记录里若只看到任务状态或来源标记为降级，说明服务端只写入了任务结果或回退链路，没有返回可直接播放的音频地址。
      </Alert>

      <Grid container spacing={3}>
        {/* 背景音乐生成 */}
        <Grid item xs={12} md={6}>
          <Card data-testid="shortvideo-ai-music-bgm-contract" data-source-endpoint={MUSIC_BGM_ENDPOINT} data-no-local-audio-fallback="true" data-input-retained-on-error="true">
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                <MusicNoteIcon color="primary" />
                <Typography variant="h6" fontWeight={700}>背景音乐生成</Typography>
              </Stack>
              <Stack spacing={2}>
                <TextField label="视频类型" value={bgmVideoType} onChange={e => setBgmVideoType(e.target.value)} size="small" fullWidth select>
                  {VIDEO_TYPES.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                </TextField>
                <TextField label="情绪风格" value={bgmMood} onChange={e => setBgmMood(e.target.value)} size="small" fullWidth select>
                  {BGM_MOODS.map(m => <MenuItem key={m} value={m}>{m}</MenuItem>)}
                </TextField>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <TextField label="时长" value={bgmDuration} onChange={e => setBgmDuration(e.target.value)} size="small" fullWidth select>
                      {BGM_DURATIONS.map(d => <MenuItem key={d} value={d}>{d}</MenuItem>)}
                    </TextField>
                  </Grid>
                  <Grid item xs={6}>
                    <TextField label="服务商" value={bgmProvider} onChange={e => setBgmProvider(e.target.value)} size="small" fullWidth select>
                      {BGM_PROVIDERS.map(p => <MenuItem key={p} value={p}>{p}</MenuItem>)}
                    </TextField>
                  </Grid>
                </Grid>
                <Button variant="contained" startIcon={bgmLoading ? <CircularProgress size={16} color="inherit" /> : <MusicNoteIcon />}
                  onClick={handleGenerateBgm} disabled={bgmLoading} data-testid="shortvideo-ai-music-generate-bgm-button" data-source-endpoint={MUSIC_BGM_ENDPOINT}>
                  {bgmLoading ? '生成中...' : '生成背景音乐'}
                </Button>
                {bgmError && <Alert severity="error" data-testid="shortvideo-ai-music-bgm-error" data-no-local-audio-fallback="true" data-input-retained="true">{bgmError}</Alert>}
              </Stack>
              {bgmResults.length > 0 ? (
                <Box sx={{ mt: 2 }} data-testid="shortvideo-ai-music-bgm-result" data-source-endpoint={MUSIC_BGM_ENDPOINT}>
                  <Divider sx={{ mb: 1 }} />
                  <Typography variant="subtitle2" gutterBottom>生成结果：</Typography>
                  {bgmResults.map((item, i) => <MusicCard key={i} item={item} />)}
                </Box>
              ) : (
                <Alert severity="info" data-testid="shortvideo-ai-music-bgm-empty" data-no-local-audio-fallback="true" sx={{ mt: 2 }}>
                  生成后会在这里显示可播放音频。若只返回任务状态而没有 URL，页面会提示为服务侧降级。
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 音效生成 */}
        <Grid item xs={12} md={6}>
          <Card data-testid="shortvideo-ai-music-sfx-contract" data-source-endpoint={MUSIC_SFX_ENDPOINT} data-no-local-audio-fallback="true" data-input-retained-on-error="true">
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                <GraphicEqIcon color="secondary" />
                <Typography variant="h6" fontWeight={700}>音效生成</Typography>
              </Stack>
              <Stack spacing={2}>
                <TextField label="场景" value={sfxScene} onChange={e => setSfxScene(e.target.value)} size="small" fullWidth select>
                  {SFX_SCENES.map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </TextField>
                <TextField label="时长" value={sfxDuration} onChange={e => setSfxDuration(e.target.value)} size="small" fullWidth select>
                  {SFX_DURATIONS.map(d => <MenuItem key={d} value={d}>{d}</MenuItem>)}
                </TextField>
                <Button variant="contained" color="secondary"
                  startIcon={sfxLoading ? <CircularProgress size={16} color="inherit" /> : <GraphicEqIcon />}
                  onClick={handleGenerateSfx} disabled={sfxLoading} data-testid="shortvideo-ai-music-generate-sfx-button" data-source-endpoint={MUSIC_SFX_ENDPOINT}>
                  {sfxLoading ? '生成中...' : '生成音效'}
                </Button>
                {sfxError && <Alert severity="error" data-testid="shortvideo-ai-music-sfx-error" data-no-local-audio-fallback="true" data-input-retained="true">{sfxError}</Alert>}
              </Stack>
              {sfxResults.length > 0 ? (
                <Box sx={{ mt: 2 }} data-testid="shortvideo-ai-music-sfx-result" data-source-endpoint={MUSIC_SFX_ENDPOINT}>
                  <Divider sx={{ mb: 1 }} />
                  <Typography variant="subtitle2" gutterBottom>生成结果：</Typography>
                  {sfxResults.map((item, i) => <MusicCard key={i} item={item} />)}
                </Box>
              ) : (
                <Alert severity="info" data-testid="shortvideo-ai-music-sfx-empty" data-no-local-audio-fallback="true" sx={{ mt: 2 }}>
                  音效接口返回 `audioUrl` 后可直接试听；没有 URL 时请检查 ElevenLabs 或本地音效服务配置。
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 历史记录 */}
        <Grid item xs={12}>
          <Card data-testid="shortvideo-ai-music-history-contract" data-source-endpoint={MUSIC_HISTORY_ENDPOINT} data-no-local-history-fallback="true">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle1" fontWeight={600}>历史记录</Typography>
                <Chip label={`共 ${historyItems.length} 条`} size="small" variant="outlined" />
              </Stack>
              <Divider sx={{ mb: 1 }} />
              {historyLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress />
                </Box>
              ) : historyIsError ? (
                <Alert
                  severity="error"
                  data-testid="shortvideo-ai-music-history-error"
                  data-no-local-history-fallback="true"
                  action={<Button size="small" color="inherit" onClick={() => refetchHistory()}>重试</Button>}
                >
                  历史记录加载失败（POST {MUSIC_HISTORY_ENDPOINT}）：{getErrorMessage(historyError)}。请检查 `sv_generation_log`；页面不会用静态历史音频补齐。
                </Alert>
              ) : historyItems.length === 0 ? (
                <Alert severity="info" data-testid="shortvideo-ai-music-history-empty" data-no-local-history-fallback="true">暂无历史记录。生成 BGM 或音效后会写入 `sv_generation_log` 并在此展示。</Alert>
              ) : (
                historyItems.map((item, i) => <MusicCard key={item.id ?? i} item={item} />)
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
