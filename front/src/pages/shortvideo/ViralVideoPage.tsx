import { useEffect, useMemo, useState } from 'react'
import {
  Box, Typography, Card, CardMedia, CardContent, CardActions,
  Button, Stack, Chip, TextField, Grid, InputAdornment,
  IconButton, Tooltip, Drawer, Divider, LinearProgress,
  Link as MuiLink, Accordion, AccordionSummary, AccordionDetails, Paper,
  Tabs, Tab, Alert,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import FavoriteIcon from '@mui/icons-material/Favorite'
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline'
import ShareIcon from '@mui/icons-material/Share'
import BookmarkBorderIcon from '@mui/icons-material/BookmarkBorder'
import CloseIcon from '@mui/icons-material/Close'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { shortvideoApi, type ViralVideo } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'

/** 无数据时显示 —，避免与真实 0 混淆 */
function formatCount(n: number | null | undefined): string {
  if (n == null) return '—'
  const x = Number(n)
  if (!Number.isFinite(x)) return '—'
  if (x >= 10000) return (x / 10000).toFixed(1) + 'w'
  return String(x)
}

function formatJsonBlock(raw: string | undefined): string {
  if (raw == null || raw.trim() === '') return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

/** 归一化 CDN 地址（兼容 // 协议相对 URL） */
function normalizeMediaUrl(s: string): string {
  const t = s.trim()
  if (t.startsWith('//')) return `https:${t}`
  return t
}

/**
 * 解析关键帧 URL 列表：库表为 JSON 字符串；亦兼容直接数组、// 开头的 CDN。
 */
function parseKeyframeUrls(raw: unknown): string[] {
  let arr: unknown[] = []
  if (Array.isArray(raw)) {
    arr = raw
  } else if (typeof raw === 'string' && raw.trim() !== '') {
    const t = raw.trim()
    // 兼容误存为「单个 URL 纯文本」而非 JSON 数组
    if (t.startsWith('http://') || t.startsWith('https://') || t.startsWith('//')) {
      return [normalizeMediaUrl(t)]
    }
    try {
      const p = JSON.parse(raw)
      arr = Array.isArray(p) ? p : []
    } catch {
      return []
    }
  } else {
    return []
  }
  const ok = (s: string): boolean => {
    const t = s.trim()
    if (!t) return false
    return /^https?:\/\//i.test(t) || t.startsWith('//')
  }
  return arr
    .filter((x): x is string => typeof x === 'string' && ok(x))
    .map(normalizeMediaUrl)
}

function mergeKeyframeSources(v: ViralVideo, deepParsed: Partial<Record<string, unknown>> | null): string[] {
  const a = parseKeyframeUrls(v.keyframeBosUrls)
  const fromJson = deepParsed
    ? parseKeyframeUrls(deepParsed.keyframeBosUrls ?? deepParsed.keyframeUrls)
    : []
  const seen = new Set<string>()
  const out: string[] = []
  for (const u of [...a, ...fromJson]) {
    if (!seen.has(u)) {
      seen.add(u)
      out.push(u)
    }
  }
  return out
}

function extractFullTextFromTranscriptNode(node: unknown): string {
  if (node == null) return ''
  if (typeof node === 'string') return node.trim()
  if (typeof node === 'object' && !Array.isArray(node)) {
    const o = node as Partial<{ fullText?: unknown; text?: unknown }>
    if (typeof o.fullText === 'string' && o.fullText.trim()) return o.fullText.trim()
    if (typeof o.text === 'string' && o.text.trim()) return o.text.trim()
  }
  return ''
}

function tryParseDeepAnalysisJson(raw?: string): Partial<Record<string, unknown>> | null {
  if (raw == null || raw.trim() === '') return null
  try {
    const p = JSON.parse(raw)
    if (p !== null && typeof p === 'object' && !Array.isArray(p)) {
      return p as Partial<Record<string, unknown>>
    }
  } catch {
    /* ignore */
  }
  return null
}

/** 后端 deep_analyze_progress 列 JSON：含 steps.download / asr / scene / bos 等 */
interface ProgressStepNode {
  status?: string
  detail?: string
}

function parseDeepAnalyzeProgressSteps(raw?: string): Partial<Record<string, ProgressStepNode>> | null {
  if (raw == null || String(raw).trim() === '') return null
  try {
    const o = JSON.parse(String(raw))
    const steps = o.steps
    if (steps != null && typeof steps === 'object' && !Array.isArray(steps)) {
      return steps as Partial<Record<string, ProgressStepNode>>
    }
  } catch {
    /* ignore */
  }
  return null
}

function describeStep(steps: Partial<Record<string, ProgressStepNode>> | null | undefined, key: string): string {
  const s = steps?.[key]
  if (!s?.status) return '无记录（未跑深度分析或进度未写入）。'
  const d = s.detail ? ` ${s.detail}` : ''
  if (s.status === 'done') return `是 / 已完成 —${d}`
  if (s.status === 'skipped') return `否（跳过）—${d}`
  if (s.status === 'failed') return `失败 —${d}`
  if (s.status === 'running') return `进行中 —${d}`
  return `${s.status} —${d}`
}

const DEEP_SECTION_LABELS: Array<{ keys: string[]; label: string }> = [
  { keys: ['opening'], label: '开场结构' },
  { keys: ['climax'], label: '高潮结构' },
  { keys: ['ending'], label: '结尾结构' },
  { keys: ['emotionCurve', 'emotion_curve'], label: '情绪曲线' },
  { keys: ['viralElements', 'viral_elements'], label: '爆款元素' },
  { keys: ['copywriting'], label: '文案技巧' },
  { keys: ['transitions'], label: '转场设计' },
  { keys: ['bgm'], label: '音乐 / BGM' },
  { keys: ['remakeAdvice', 'remake_advice'], label: '二创建议' },
]

function stringifySectionValue(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'string') return v
  try {
    return JSON.stringify(v, null, 2)
  } catch {
    return String(v)
  }
}

function pickFirst(obj: Record<string, unknown>, keys: string[]): unknown {
  for (const k of keys) {
    if (k in obj && obj[k] != null) return obj[k]
  }
  return undefined
}

/** deepAnalysisResult JSON 中 scenes 数组的元素结构 */
export interface ViralSceneItem {
  time?: string
  environment?: string
  person?: string
  props?: string
  camera?: string
  mood?: string
  [key: string]: unknown
}

function ViralTranscriptTab({
  v,
  parsed,
  toast,
}: {
  v: ViralVideo
  parsed: Record<string, unknown> | null
  toast: (msg: string, severity: 'success' | 'error') => void
}) {
  const db = (v.transcript && String(v.transcript).trim()) || ''
  const jsonFt = extractFullTextFromTranscriptNode(parsed?.transcript)

  const copy = (text: string) => {
    void navigator.clipboard.writeText(text).then(
      () => toast('已复制', 'success'),
      () => toast('复制失败', 'error'),
    )
  }

  if (!db && !jsonFt) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
        暂无口播正文：未启用本机下载+ASR 时，口播多为模型根据标题/互动等推演，请以「拆解结论」中的证据口径为准；启用 ASR 并跑完深度任务后刷新可看到实录转写。
      </Typography>
    )
  }

  if (db && jsonFt && db !== jsonFt) {
    return (
      <Stack spacing={2}>
        <Typography variant="caption" color="text.secondary">
          以下两处来源不同：左为数据库字段 transcript（偏 ASR/管线）；右为拆解 JSON 内 transcript.fullText（模型对齐稿）。若不一致，请以左侧实录为准。
        </Typography>
        <Paper variant="outlined" sx={{ p: 1.5 }}>
          <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
            <Chip size="small" color="primary" label="库表 transcript" />
            <Chip size="small" variant="outlined" label="ASR / 写入稿" />
            <Tooltip title="复制">
              <IconButton size="small" aria-label="复制库表口播" onClick={() => copy(db)}>
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', maxHeight: 360, overflow: 'auto' }}>
            {db}
          </Typography>
        </Paper>
        <Paper variant="outlined" sx={{ p: 1.5 }}>
          <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
            <Chip size="small" label="拆解 JSON · fullText" />
            <Chip size="small" variant="outlined" label="模型整理" />
            <Tooltip title="复制">
              <IconButton size="small" aria-label="复制 JSON 口播" onClick={() => copy(jsonFt)}>
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', maxHeight: 360, overflow: 'auto' }}>
            {jsonFt}
          </Typography>
        </Paper>
      </Stack>
    )
  }

  const text = db || jsonFt
  const singleLabel = db ? '库表 transcript（ASR / 管线）' : '拆解 JSON 口播全文'
  return (
    <Paper variant="outlined" sx={{ p: 1.5 }}>
      <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <Chip size="small" color={db ? 'primary' : 'default'} label={singleLabel} />
        <Tooltip title="复制全文">
          <IconButton size="small" aria-label="复制口播" onClick={() => copy(text)}>
            <ContentCopyIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Stack>
      <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', maxHeight: 480, overflow: 'auto' }}>
        {text}
      </Typography>
    </Paper>
  )
}

function ViralBreakdownStructured({
  parsed,
}: {
  parsed: Record<string, unknown> | null
}) {
  if (!parsed) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        非 JSON 结果，请展开下方「原始拆解 JSON」查看。
      </Typography>
    )
  }
  const scenes = parsed.scenes
  const structureNode = parsed.structure
  const viralH = parsed.viralHypotheses ?? parsed.viral_hypotheses
  const remakeT = parsed.remakeVariableTable ?? parsed.remake_variable_table

  return (
    <Stack spacing={1.5} sx={{ mb: 2 }}>
      {Array.isArray(scenes) && scenes.length > 0 ? (
        <Box>
          <Typography variant="subtitle2" gutterBottom>分镜时间轴</Typography>
          <Stack spacing={1}>
            {scenes.map((sc, i) => {
              const row = sc as ViralSceneItem
              const time = typeof row.time === 'string' ? row.time : ''
              const env = typeof row.environment === 'string' ? row.environment : stringifySectionValue(row.environment)
              const extra = ['person', 'props', 'camera', 'mood']
                .map((k) => {
                  const val = row[k]
                  if (val == null || String(val).trim() === '') return null
                  return `${k}：${typeof val === 'string' ? val : stringifySectionValue(val)}`
                })
                .filter(Boolean)
                .join('\n')
              return (
                <Paper key={i} variant="outlined" sx={{ p: 1.25 }}>
                  <Typography variant="caption" color="primary" fontWeight={600}>
                    场景 {i + 1}
                    {time ? ` · ${time}` : ''}
                  </Typography>
                  {env ? (
                    <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{env}</Typography>
                  ) : null}
                  {extra ? (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, whiteSpace: 'pre-wrap', fontSize: 12 }}>
                      {extra}
                    </Typography>
                  ) : null}
                </Paper>
              )
            })}
          </Stack>
        </Box>
      ) : null}

      {DEEP_SECTION_LABELS.map(({ keys, label }) => {
        const val = pickFirst(parsed, keys)
        if (val == null || val === '') return null
        const text = stringifySectionValue(val)
        if (!text.trim()) return null
        return (
          <Box key={label}>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>{label}</Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{text}</Typography>
          </Box>
        )
      })}

      {structureNode != null ? (
        <Box>
          <Typography variant="subtitle2" gutterBottom>叙事结构</Typography>
          <Box component="pre" sx={{ m: 0, p: 1, bgcolor: 'action.hover', borderRadius: 1, fontSize: 12, overflow: 'auto', maxHeight: 240 }}>
            {stringifySectionValue(structureNode)}
          </Box>
        </Box>
      ) : null}

      {viralH != null ? (
        <Box>
          <Typography variant="subtitle2" gutterBottom>爆款假设</Typography>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
            {stringifySectionValue(viralH)}
          </Typography>
        </Box>
      ) : null}

      {remakeT != null ? (
        <Box>
          <Typography variant="subtitle2" gutterBottom>二创变量表</Typography>
          <Box component="pre" sx={{ m: 0, p: 1, bgcolor: 'action.hover', borderRadius: 1, fontSize: 12, overflow: 'auto', maxHeight: 280 }}>
            {stringifySectionValue(remakeT)}
          </Box>
        </Box>
      ) : null}
    </Stack>
  )
}

/** 后端 SvViralVideo：viewCount=播放，favoriteCount=视频被收藏数 */
function viralPlayCount(v: ViralVideo): number | undefined {
  const n = v.viewCount ?? v.playCount
  if (n == null) return undefined
  const x = Number(n)
  return Number.isFinite(x) ? x : undefined
}

function viralLikeCount(v: ViralVideo): number | undefined {
  const n = v.likeCount
  if (n == null) return undefined
  const x = Number(n)
  return Number.isFinite(x) ? x : undefined
}

function viralCommentCount(v: ViralVideo): number | undefined {
  const n = v.commentCount
  if (n == null) return undefined
  const x = Number(n)
  return Number.isFinite(x) ? x : undefined
}

function viralCollectCount(v: ViralVideo): number | undefined {
  const n = v.favoriteCount ?? v.collectCount
  if (n == null) return undefined
  const x = Number(n)
  return Number.isFinite(x) ? x : undefined
}

function viralShareCount(v: ViralVideo): number | undefined {
  const n = v.shareCount
  if (n == null) return undefined
  const x = Number(n)
  return Number.isFinite(x) ? x : undefined
}

export default function ViralVideoPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [keyword, setKeyword] = useState('')
  const [searchKw, setSearchKw] = useState('')
  const [minPlay, setMinPlay] = useState<number | ''>('')
  const [detail, setDetail] = useState<ViralVideo | null>(null)
  const [page, setPage] = useState(0)
  const rows = 20

  /** 从账号详情等页面深链：?videoId=123 → 打开详情抽屉并移除 query，避免刷新重复弹出 */
  useEffect(() => {
    const raw = searchParams.get('videoId')
    if (raw == null || raw.trim() === '') return
    const vid = Number(raw)
    if (!Number.isFinite(vid) || vid <= 0) return
    setDetail({ id: vid, title: '', authorName: '' })
    const next = new URLSearchParams(searchParams)
    next.delete('videoId')
    setSearchParams(next, { replace: true })
  }, [searchParams, setSearchParams])

  const { data, isFetching } = useQuery({
    queryKey: ['viral-videos', searchKw, minPlay, page],
    queryFn: () => shortvideoApi.viralList({
      page, rows, keyword: searchKw || undefined,
      minPlayCount: minPlay !== '' ? minPlay : undefined,
    }),
  })

  const detailId = detail?.id ?? null
  const { data: detailFromApi, isFetching: detailLoading, refetch: refetchDetail } = useQuery({
    queryKey: ['viral-video-detail', detailId],
    queryFn: () => shortvideoApi.viralGet(detailId!),
    enabled: detailId != null,
    /** 深度分析为异步任务：提交后需轮询直至 completed/failed，否则 videoBosUrl / keyframeBosUrls 仍为空 */
    refetchInterval: (q) => {
      const row = q.state.data as ViralVideo | undefined
      const s = row?.deepAnalyzeStatus
      return s === 'processing' || s === 'pending' ? 4000 : false
    },
  })

  const favMut = useMutation({
    mutationFn: (id: number) => shortvideoApi.viralCollect(id),
    onSuccess: () => {
      toast('已收藏', 'success')
      void qc.invalidateQueries({ queryKey: ['viral-videos'] })
      void qc.invalidateQueries({ queryKey: ['viral-video-detail'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const analyzeMut = useMutation({
    mutationFn: shortvideoApi.viralAnalyze,
    onSuccess: (_, id) => {
      toast('分析任务已提交；详情抽屉打开时将自动轮询直至完成', 'success')
      void qc.invalidateQueries({ queryKey: ['viral-videos'] })
      void qc.invalidateQueries({ queryKey: ['viral-video-detail', id] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const handleSearch = () => { setSearchKw(keyword); setPage(0) }

  const list = Array.isArray(data) ? data : []
  const showDetail = detailFromApi ?? detail
  const deepStatus = showDetail?.deepAnalyzeStatus
  const isDeepAnalyzing = deepStatus === 'processing' || deepStatus === 'pending'
  const [detailTab, setDetailTab] = useState(0)

  useEffect(() => {
    setDetailTab(0)
  }, [detailId])

  const deepRaw = showDetail
    ? (showDetail.deepAnalysisResult || showDetail.lightAnalysisResult || '')
    : ''
  const deepParsed = useMemo(
    () => tryParseDeepAnalysisJson(deepRaw || undefined),
    [deepRaw],
  )
  const keyframeList = useMemo(
    () => (showDetail ? mergeKeyframeSources(showDetail, deepParsed) : []),
    [showDetail, deepParsed],
  )
  const pipelineSteps = useMemo(
    () => (showDetail ? parseDeepAnalyzeProgressSteps(showDetail.deepAnalyzeProgress) : null),
    [showDetail],
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap sx={{ rowGap: 1 }}>
        <TextField size="small" placeholder="搜索关键词/作者" value={keyword}
          onChange={e => setKeyword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
          sx={{ minWidth: 200, flex: '1 1 200px' }} />
        <TextField size="small" placeholder="播放量≥(可选)" type="number" value={minPlay}
          onChange={e => setMinPlay(e.target.value ? Number(e.target.value) : '')}
          sx={{ width: 140 }} />
        <Button variant="contained" size="small" onClick={handleSearch}>搜索</Button>
        <Typography variant="body2" color="text.secondary" sx={{ ml: { xs: 0, sm: 'auto' } }}>
          共 {list.length} 条{list.length >= rows ? '（可载入更多）' : ''}
        </Typography>
      </Stack>

      <Typography variant="caption" color="text.secondary" display="block">
        互动数据：已绑定抖音 Open API 并走授权账号采集时，一般有播放/点赞/评论/分享/收藏；仅 Playwright 网页采集时多为链接与封面，统计常为「—」，需授权后重新同步或走官方接口。
      </Typography>

      {isFetching ? (
        <Typography color="text.secondary" textAlign="center" py={4}>加载中...</Typography>
      ) : list.length === 0 ? (
        <Typography color="text.secondary" textAlign="center" py={4}>暂无数据</Typography>
      ) : (
        <Grid container spacing={2.5}>
          {list.map(v => (
            <Grid item xs={12} sm={6} md={4} lg={2} key={v.id}>
              <Card
                variant="outlined"
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  overflow: 'hidden',
                }}
              >
                <Box
                  onClick={() => setDetail(v)}
                  sx={{
                    position: 'relative',
                    width: '100%',
                    aspectRatio: '9 / 16',
                    maxHeight: 320,
                    bgcolor: 'action.hover',
                    cursor: 'pointer',
                  }}
                >
                  <CardMedia
                    component="img"
                    image={v.coverUrl || `https://picsum.photos/seed/${v.id}/360/640`}
                    alt={v.title ?? ''}
                    sx={{
                      position: 'absolute',
                      inset: 0,
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                    }}
                  />
                </Box>
                <CardContent sx={{ flex: '1 1 auto', display: 'flex', flexDirection: 'column', pt: 1.5, pb: 1 }}>
                  <Typography
                    variant="subtitle2"
                    title={v.title ?? undefined}
                    sx={{
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      minHeight: 40,
                      lineHeight: 1.35,
                    }}
                  >
                    {v.title || '（无标题）'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="block" noWrap>
                    {v.authorName || '—'}
                  </Typography>
                  <Stack direction="row" spacing={0.5} mt={1} flexWrap="wrap" useFlexGap sx={{ rowGap: 0.5 }}>
                    <Chip
                      icon={<PlayArrowIcon sx={{ fontSize: 16 }} />}
                      label={formatCount(viralPlayCount(v))}
                      size="small"
                      variant="outlined"
                      sx={{ '& .MuiChip-label': { px: 0.75 } }}
                    />
                    <Chip
                      icon={<FavoriteIcon sx={{ fontSize: 16 }} />}
                      label={formatCount(viralLikeCount(v))}
                      size="small"
                      variant="outlined"
                      color="error"
                      sx={{ '& .MuiChip-label': { px: 0.75 } }}
                    />
                    <Chip
                      icon={<ChatBubbleOutlineIcon sx={{ fontSize: 16 }} />}
                      label={formatCount(viralCommentCount(v))}
                      size="small"
                      variant="outlined"
                      sx={{ '& .MuiChip-label': { px: 0.75 } }}
                    />
                    <Chip
                      icon={<BookmarkBorderIcon sx={{ fontSize: 16 }} />}
                      label={formatCount(viralCollectCount(v))}
                      size="small"
                      variant="outlined"
                      sx={{ '& .MuiChip-label': { px: 0.75 } }}
                    />
                    <Chip
                      icon={<ShareIcon sx={{ fontSize: 16 }} />}
                      label={formatCount(viralShareCount(v))}
                      size="small"
                      variant="outlined"
                      sx={{ '& .MuiChip-label': { px: 0.75 } }}
                    />
                  </Stack>
                </CardContent>
                <CardActions
                  sx={{
                    mt: 'auto',
                    pt: 0,
                    px: 1.5,
                    pb: 1.5,
                    borderTop: 1,
                    borderColor: 'divider',
                    gap: 0.5,
                    flexWrap: 'wrap',
                  }}
                >
                  <Tooltip title="收藏">
                    <IconButton size="small" onClick={() => favMut.mutate(v.id)} aria-label="收藏">
                      <FavoriteBorderIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Button size="small" onClick={() => analyzeMut.mutate(v.id)}>拆解分析</Button>
                  <Button size="small" onClick={() => setDetail(v)}>详情</Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {(page > 0 || list.length >= rows) && (
        <Stack direction="row" justifyContent="center" spacing={1}>
          <Button size="small" disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</Button>
          <Typography variant="body2" alignSelf="center">第 {page + 1} 页</Typography>
          <Button size="small" disabled={list.length < rows} onClick={() => setPage(p => p + 1)}>下一页</Button>
        </Stack>
      )}

      <Drawer anchor="right" open={detail !== null} onClose={() => setDetail(null)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 1120 }, maxWidth: '100%', p: 3 } }}>
        {detail && showDetail && (
          <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap>
              <Typography variant="h6">爆款详情</Typography>
              <Stack direction="row" spacing={1} alignItems="center">
                <Button size="small" variant="outlined" onClick={() => void refetchDetail()}>
                  刷新详情
                </Button>
                <IconButton onClick={() => setDetail(null)}><CloseIcon /></IconButton>
              </Stack>
            </Stack>
            {detailLoading && <LinearProgress />}
            {isDeepAnalyzing ? (
              <Alert severity="info" sx={{ py: 0.5 }}>
                深度分析进行中（约每 4 秒自动拉取一次）。完成后此处会出现 <strong>缓存视频（BOS）</strong>与<strong>关键帧</strong>；若一直为空请点「刷新详情」或检查服务端 BOS 是否已配置且上传成功。
              </Alert>
            ) : null}
            {deepStatus === 'completed'
              && (!showDetail.videoBosUrl || String(showDetail.videoBosUrl).trim() === '')
              && keyframeList.length === 0 ? (
                <Alert severity="warning" sx={{ py: 0.5 }}>
                  分析已完成，但未返回 BOS 视频地址与关键帧：多为未开启 BOS、上传失败或仅走了推演未落库素材。请查看下方「状态说明」中的 download / scene / bos 步骤；仍异常时查服务端日志「BOS上传」。
                </Alert>
              ) : null}
            <Box
              component="img"
              src={showDetail.coverUrl || showDetail.coverBosUrl}
              alt={showDetail.title}
              sx={{ width: '100%', borderRadius: 1, maxHeight: 240, objectFit: 'cover' }}
            />
            <Divider />
            <Box><Typography variant="caption" color="text.secondary">标题</Typography>
              <Typography fontWeight={500}>{showDetail.title}</Typography></Box>
            <Box><Typography variant="caption" color="text.secondary">作者</Typography>
              <Typography>{showDetail.authorName}</Typography></Box>
            <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
              <Box><Typography variant="caption" color="text.secondary">播放量</Typography>
                <Typography>{formatCount(viralPlayCount(showDetail))}</Typography></Box>
              <Box><Typography variant="caption" color="text.secondary">点赞</Typography>
                <Typography>{formatCount(viralLikeCount(showDetail))}</Typography></Box>
              <Box><Typography variant="caption" color="text.secondary">评论</Typography>
                <Typography>{formatCount(viralCommentCount(showDetail))}</Typography></Box>
              <Box><Typography variant="caption" color="text.secondary">收藏（视频被收藏）</Typography>
                <Typography>{formatCount(viralCollectCount(showDetail))}</Typography></Box>
              <Box><Typography variant="caption" color="text.secondary">转发</Typography>
                <Typography>{formatCount(viralShareCount(showDetail))}</Typography></Box>
              {showDetail.videoDuration != null && Number.isFinite(Number(showDetail.videoDuration)) ? (
                <Box><Typography variant="caption" color="text.secondary">时长</Typography>
                  <Typography>{Number(showDetail.videoDuration)} 秒</Typography></Box>
              ) : null}
            </Stack>

            <Box>
              <Typography variant="caption" color="text.secondary">深度分析</Typography>
              <Typography variant="body2">
                {showDetail.deepAnalyzeStatus ?? '—'}
                {showDetail.deepAnalyzeProgress ? ` · ${showDetail.deepAnalyzeProgress}` : ''}
              </Typography>
            </Box>

            <Tabs
              value={detailTab}
              onChange={(_, v) => setDetailTab(v)}
              variant="scrollable"
              scrollButtons="auto"
              sx={{ borderBottom: 1, borderColor: 'divider', minHeight: 40 }}
            >
              <Tab label="素材与关键帧" sx={{ minHeight: 40, py: 0.5 }} />
              <Tab label="口播脚本" sx={{ minHeight: 40, py: 0.5 }} />
              <Tab label="拆解结论" sx={{ minHeight: 40, py: 0.5 }} />
            </Tabs>

            {detailTab === 0 && (
              <Stack spacing={1.5} sx={{ pt: 1 }}>
                <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'action.hover' }}>
                  <Typography variant="subtitle2" gutterBottom>视频 / 关键帧 — 状态说明</Typography>
                  <Stack spacing={1.25}>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        ① 本机是否下载了视频（yt-dlp）？
                      </Typography>
                      <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                        {describeStep(pipelineSteps, 'download')}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        ② 能否在下方播放缓存视频？
                      </Typography>
                      <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                        {showDetail.videoBosUrl && String(showDetail.videoBosUrl).trim() !== ''
                          ? '可以：已写入 video_bos_url，下方播放器即 BOS 缓存（若黑屏多为跨域，请复制链接到新标签页打开）。'
                          : '当前无 BOS 视频地址：通常表示未成功走「下载→上传」或仅上传了封面/关键帧。'}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        ③ 是否做了抽帧 / 场景分析？
                      </Typography>
                      <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                        {describeStep(pipelineSteps, 'scene')}
                        {keyframeList.length > 0
                          ? ` 已入库关键帧 URL ${keyframeList.length} 张，见下方网格。`
                          : ' 下方无缩略图即表示 keyframe_bos_urls 为空。'}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        ④ 页面上能展示吗？
                      </Typography>
                      <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                        能：有缓存地址则展示播放器与关键帧图；无则仅展示说明与抖音外链。口播脚本、拆解结论在其它 Tab。
                      </Typography>
                    </Box>
                  </Stack>
                </Paper>
                {showDetail.videoUrl && String(showDetail.videoUrl).trim() !== '' ? (
                  <Stack direction="row" spacing={0.5} alignItems="center" flexWrap="wrap" useFlexGap>
                    <Typography variant="caption" color="text.secondary" sx={{ width: '100%' }}>抖音原视频链接</Typography>
                    <MuiLink
                      href={showDetail.videoUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      variant="body2"
                      sx={{ wordBreak: 'break-all', display: 'inline-flex', alignItems: 'center', gap: 0.5 }}
                    >
                      {showDetail.videoUrl}
                      <OpenInNewIcon sx={{ fontSize: 16 }} />
                    </MuiLink>
                    <Tooltip title="复制链接">
                      <IconButton
                        size="small"
                        aria-label="复制原链接"
                        onClick={() => {
                          void navigator.clipboard.writeText(showDetail.videoUrl!).then(
                            () => toast('已复制链接', 'success'),
                            () => toast('复制失败', 'error'),
                          )
                        }}
                      >
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                ) : (
                  <Typography variant="body2" color="text.secondary">暂无抖音原链接（部分来源仅有人设/标题）。</Typography>
                )}

                {showDetail.videoBosUrl && String(showDetail.videoBosUrl).trim() !== '' ? (
                  <Box>
                    <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
                      缓存视频（BOS）
                    </Typography>
                    <Box
                      component="video"
                      src={normalizeMediaUrl(showDetail.videoBosUrl)}
                      controls
                      playsInline
                      sx={{
                        width: '100%',
                        maxHeight: 320,
                        borderRadius: 1,
                        bgcolor: 'black',
                      }}
                    />
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                      无法播放时多为跨域限制，可复制 URL 到新标签页打开。
                    </Typography>
                  </Box>
                ) : null}

                {keyframeList.length > 0 ? (
                  <Box>
                    <Typography variant="subtitle2" gutterBottom>关键帧</Typography>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                      来自抽帧后上传 BOS（已与拆解 JSON 对齐条数时，可与「拆解结论」分镜对照）。
                    </Typography>
                    <Grid container spacing={1}>
                      {keyframeList.map((url, idx) => (
                        <Grid item xs={6} sm={4} key={`${idx}-${url.slice(-32)}`}>
                          <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
                            <Box
                              component="img"
                              src={url}
                              alt={`关键帧 ${idx + 1}`}
                              loading="lazy"
                              sx={{ width: '100%', aspectRatio: '9 / 16', objectFit: 'cover', display: 'block', bgcolor: 'action.hover' }}
                            />
                            <Typography variant="caption" display="block" sx={{ px: 0.75, py: 0.5 }} noWrap title={url}>
                              第 {idx + 1} 帧
                            </Typography>
                          </Paper>
                        </Grid>
                      ))}
                    </Grid>
                  </Box>
                ) : (
                  <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'action.hover' }}>
                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                      暂无关键帧图：需本机下载视频并抽帧，且开启 BOS 上传后才会写入 keyframe_bos_urls。仅元数据推演或未跑抽帧步骤时此处为空，属正常。
                    </Typography>
                  </Paper>
                )}

                {showDetail.sceneDescriptions && String(showDetail.sceneDescriptions).trim() !== '' ? (
                  <Box>
                    <Typography variant="subtitle2" gutterBottom>场景 / 分镜说明（管线）</Typography>
                    <Typography
                      variant="body2"
                      sx={{
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                        p: 1.5,
                        borderRadius: 1,
                        bgcolor: 'action.hover',
                        maxHeight: 280,
                        overflow: 'auto',
                      }}
                    >
                      {showDetail.sceneDescriptions}
                    </Typography>
                  </Box>
                ) : null}
              </Stack>
            )}

            {detailTab === 1 && (
              <Box sx={{ pt: 1 }}>
                <ViralTranscriptTab v={showDetail} parsed={deepParsed} toast={toast} />
              </Box>
            )}

            {detailTab === 2 && (
              <Box sx={{ pt: 1 }}>
                {(showDetail.deepAnalysisResult || showDetail.lightAnalysisResult) ? (
                  <>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5, lineHeight: 1.6 }}>
                      口播全文请在「口播脚本」页查看；此处为结构化解说与变量表。需要调试时可展开最下方原始 JSON。
                    </Typography>
                    <ViralBreakdownStructured parsed={deepParsed} />
                    <Accordion disableGutters elevation={0} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, '&:before': { display: 'none' } }}>
                      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Typography variant="subtitle2">原始拆解 JSON</Typography>
                      </AccordionSummary>
                      <AccordionDetails>
                        <Box
                          component="pre"
                          sx={{
                            m: 0,
                            p: 1.5,
                            bgcolor: 'action.hover',
                            fontSize: 12,
                            lineHeight: 1.45,
                            maxHeight: 360,
                            overflow: 'auto',
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-word',
                          }}
                        >
                          {formatJsonBlock(showDetail.deepAnalysisResult || showDetail.lightAnalysisResult)}
                        </Box>
                      </AccordionDetails>
                    </Accordion>
                  </>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    暂无拆解正文：请先点「拆解分析」并等待任务完成（processing → completed），再点「刷新详情」。
                  </Typography>
                )}
              </Box>
            )}

            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Button
                variant="outlined"
                size="small"
                disabled={detailId == null}
                onClick={() => detailId != null && void qc.invalidateQueries({ queryKey: ['viral-video-detail', detailId] })}
              >
                刷新详情
              </Button>
              <Button variant="contained" size="small" onClick={() => analyzeMut.mutate(showDetail.id)}>拆解分析</Button>
              <Button variant="outlined" size="small" onClick={() => favMut.mutate(showDetail.id)}>收藏</Button>
            </Stack>
          </Stack>
        )}
      </Drawer>
    </Box>
  )
}
