import { useState } from 'react'
import {
  Box, Card, CardContent, Typography, Grid, Stack,
  Button, MenuItem, TextField, Chip, CircularProgress, Divider,
} from '@mui/material'
import MusicNoteIcon from '@mui/icons-material/MusicNote'
import GraphicEqIcon from '@mui/icons-material/GraphicEq'
import DownloadIcon from '@mui/icons-material/Download'
import { PageHeader } from '@/components/base'
import { shortvideoApi } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { useQuery } from '@tanstack/react-query'

const VIDEO_TYPES = ['护肤教程', '彩妆教程', '产品测评', '成分科普', '使用教程', '好物分享']
const BGM_MOODS = ['轻松愉快', '温暖治愈', '专业严肃', '活力动感', '清新自然']
const BGM_DURATIONS = ['15秒', '30秒', '60秒', '90秒']
const BGM_PROVIDERS = ['Suno AI', '网易云音乐', '本地生成']
const SFX_SCENES = ['产品展示', '购买成功', '倒计时', '开场过渡', '结尾引导']
const SFX_DURATIONS = ['1秒', '3秒', '5秒', '10秒']

interface MusicItem {
  id?: number
  name: string
  duration: string
  url?: string
  type: 'bgm' | 'sfx'
}

interface BgmGenerateResult {
  name?: string
  url?: string
  audioUrl?: string
  name2?: string
  url2?: string
  [key: string]: unknown
}

export default function AiMusicPage() {
  const toast = useToast()

  // BGM state
  const [bgmVideoType, setBgmVideoType] = useState('护肤教程')
  const [bgmMood, setBgmMood] = useState('轻松愉快')
  const [bgmDuration, setBgmDuration] = useState('30秒')
  const [bgmProvider, setBgmProvider] = useState('Suno AI')
  const [bgmLoading, setBgmLoading] = useState(false)
  const [bgmResults, setBgmResults] = useState<MusicItem[]>([])

  // SFX state
  const [sfxScene, setSfxScene] = useState('产品展示')
  const [sfxDuration, setSfxDuration] = useState('3秒')
  const [sfxLoading, setSfxLoading] = useState(false)
  const [sfxResults, setSfxResults] = useState<MusicItem[]>([])

  const { data: history = [] } = useQuery({
    queryKey: ['music-history'],
    queryFn: () => shortvideoApi.musicHistory({ rows: 10 }),
  })
  const historyItems: MusicItem[] = history.map(h => ({
    name: String(h.name ?? ''),
    duration: String(h.duration ?? ''),
    url: h.url != null ? String(h.url) : undefined,
    type: (h.type === 'bgm' || h.type === 'sfx') ? h.type : 'bgm',
    id: h.id != null ? Number(h.id) : undefined,
  }))

  const handleGenerateBgm = async () => {
    setBgmLoading(true)
    try {
      const sec = parseInt(bgmDuration.replace(/\D/g, ''), 10) || 30
      const styleDescription = `${bgmVideoType}，情绪：${bgmMood}，服务商偏好：${bgmProvider}`
      const res = await shortvideoApi.generateBgm({
        styleDescription,
        durationSec: sec,
        instrumental: true,
      }) as BgmGenerateResult
      const items: MusicItem[] = [
        { name: String(res.name ?? `${bgmMood}BGM`), duration: bgmDuration, url: String(res.url ?? res.audioUrl ?? ''), type: 'bgm' as const },
        { name: String(res.name2 ?? ''), duration: bgmDuration, url: String(res.url2 ?? ''), type: 'bgm' as const },
      ].filter(i => i.name && i.url)
      setBgmResults(items.length > 0 ? items : [{ name: `${bgmMood} BGM`, duration: bgmDuration, url: '', type: 'bgm' }])
      toast('背景音乐生成成功', 'success')
    } catch {
      toast('生成失败', 'error')
    } finally {
      setBgmLoading(false)
    }
  }

  const handleGenerateSfx = async () => {
    setSfxLoading(true)
    try {
      const sec = parseInt(sfxDuration.replace(/\D/g, ''), 10) || 5
      const res = await shortvideoApi.generateSfx({ sceneDescription: sfxScene, durationSec: sec })
      const arr = Array.isArray(res) ? res : [res]
      const items: MusicItem[] = arr.map((r) => ({
        name: String(r.name ?? '音效'),
        duration: sfxDuration,
        url: String(r.url ?? ''),
        type: 'sfx' as const,
      }))
      setSfxResults(items)
      toast('音效生成成功', 'success')
    } catch {
      toast('生成失败', 'error')
    } finally {
      setSfxLoading(false)
    }
  }
  const MusicCard = ({ item }: { item: MusicItem }) => (
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
      <Stack direction="row" spacing={1} alignItems="center">
        {item.type === 'bgm' ? <MusicNoteIcon fontSize="small" color="primary" /> : <GraphicEqIcon fontSize="small" color="secondary" />}
        <Box>
          <Typography variant="body2" fontWeight={600}>{item.name}</Typography>
          <Typography variant="caption" color="text.secondary">时长：{item.duration}</Typography>
        </Box>
      </Stack>
      <Stack direction="row" spacing={0.5}>
        <Button size="small" variant="outlined">试听▶</Button>
        <Button size="small" startIcon={<DownloadIcon fontSize="small" />}>下载</Button>
        <Button size="small" variant="contained" color="primary">使用</Button>
      </Stack>
    </Box>
  )

  return (
    <Box>
      <PageHeader
        title="AI 配乐"
        breadcrumbs={[{ label: '短视频' }, { label: 'AI配乐' }]}
        subtitle="AI 自动生成背景音乐与音效，提升视频氛围感"
      />

      <Grid container spacing={3}>
        {/* 背景音乐生成 */}
        <Grid item xs={12} md={6}>
          <Card>
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
                  onClick={handleGenerateBgm} disabled={bgmLoading}>
                  {bgmLoading ? '生成中...' : '生成背景音乐 ✨'}
                </Button>
              </Stack>
              {bgmResults.length > 0 && (
                <Box sx={{ mt: 2 }}>
                  <Divider sx={{ mb: 1 }} />
                  <Typography variant="subtitle2" gutterBottom>生成结果：</Typography>
                  {bgmResults.map((item, i) => <MusicCard key={i} item={item} />)}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 音效生成 */}
        <Grid item xs={12} md={6}>
          <Card>
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
                  onClick={handleGenerateSfx} disabled={sfxLoading}>
                  {sfxLoading ? '生成中...' : '生成音效 ✨'}
                </Button>
              </Stack>
              {sfxResults.length > 0 && (
                <Box sx={{ mt: 2 }}>
                  <Divider sx={{ mb: 1 }} />
                  <Typography variant="subtitle2" gutterBottom>生成结果：</Typography>
                  {sfxResults.map((item, i) => <MusicCard key={i} item={item} />)}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 历史记录 */}
        {historyItems.length > 0 && (
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                  <Typography variant="subtitle1" fontWeight={600}>历史记录</Typography>
                  <Chip label={`共 ${historyItems.length} 条`} size="small" variant="outlined" />
                </Stack>
                <Divider sx={{ mb: 1 }} />
                {historyItems.map((item, i) => <MusicCard key={i} item={item} />)}
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>
    </Box>
  )
}
