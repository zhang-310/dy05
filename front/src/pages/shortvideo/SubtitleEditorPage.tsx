import { useState, useRef, useEffect, useMemo } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Button, TextField,
  IconButton, CircularProgress, Divider, Chip, Paper, Grid, Alert,
  Slider, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import SaveIcon from '@mui/icons-material/Save'
import DownloadIcon from '@mui/icons-material/Download'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { subtitleGet, subtitleSave, subtitleExportSrt, type SubtitleSegment } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'

function formatTime(sec: number): string {
  const m = Math.floor(sec / 60).toString().padStart(2, '0')
  const s = Math.floor(sec % 60).toString().padStart(2, '0')
  const ms = Math.floor((sec % 1) * 100).toString().padStart(2, '0')
  return `${m}:${s}.${ms}`
}

function parseTime(str: string): number {
  const parts = str.split(':')
  if (parts.length !== 2) return 0
  const [mStr, sStr] = parts
  const [s, ms] = (sStr ?? '0').split('.')
  return parseInt(mStr ?? '0') * 60 + parseInt(s ?? '0') + parseInt(ms ?? '0') / 100
}

const EMPTY_SUBTITLES: SubtitleSegment[] = []
const SUBTITLE_GET_ENDPOINT = '/short-video/edit/subtitles/get'
const SUBTITLE_SAVE_ENDPOINT = '/short-video/edit/subtitles/save'
const SUBTITLE_EXPORT_ENDPOINT = '/short-video/edit/subtitles/export-srt'
const SUBTITLE_READY_ENDPOINTS = [
  SUBTITLE_GET_ENDPOINT,
  SUBTITLE_SAVE_ENDPOINT,
  SUBTITLE_EXPORT_ENDPOINT,
] as const
const SUBTITLE_READY_ROUTES = [
  shortvideoRoutes.subtitles,
  '/admin/shortvideo/subtitle-editor/:id',
  `${shortvideoRoutes.editing}?projectId=:id`,
].join('|')
const SUBTITLE_SUPPORTED_ACTIONS = [
  'refresh-subtitles',
  'save-subtitles',
  'export-srt',
  'add-subtitle-segment',
  'delete-subtitle-segment',
  'edit-subtitle-style',
].join('|')
const SUBTITLE_UNSUPPORTED_ENDPOINTS = [
  '/short-video/edit/subtitles/mock',
  '/short-video/edit/subtitles/local-get',
  '/short-video/edit/subtitles/local-save',
  '/short-video/edit/subtitles/static-subtitles',
  '/short-video/edit/subtitles/placeholder-video',
] as const
const SUBTITLE_PREVIEW_FALLBACK_COLOR = '#ffffff'
const SUBTITLE_PREVIEW_BACKGROUND = '#000000'

// 时间轴可视化组件
function TimelineBar({ segments, selectedId, onSelect, totalDuration }: {
  segments: SubtitleSegment[]
  selectedId: string | null
  onSelect: (id: string) => void
  totalDuration: number
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const theme = useTheme()
  const [hoverId, setHoverId] = useState<string | null>(null)
  const timelineBg = theme.palette.mode === 'dark' ? alpha(theme.palette.common.white, 0.04) : theme.palette.grey[50]
  const gridStroke = theme.palette.divider
  const defaultFill = alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.24 : 0.18)
  const hoverFill = alpha(theme.palette.primary.light, theme.palette.mode === 'dark' ? 0.42 : 0.32)
  const selectedFill = alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.62 : 0.55)
  const defaultStroke = alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.72 : 0.45)
  const selectedStroke = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.dark

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const w = canvas.width
    const h = canvas.height
    ctx.clearRect(0, 0, w, h)

    // 背景网格
    ctx.strokeStyle = gridStroke
    ctx.lineWidth = 1
    for (let i = 0; i <= 10; i++) {
      const x = (w / 10) * i
      ctx.beginPath()
      ctx.moveTo(x, 0)
      ctx.lineTo(x, h)
      ctx.stroke()
    }

    // 绘制字幕段
    segments.forEach(seg => {
      const x = (seg.startTime / totalDuration) * w
      const width = ((seg.endTime - seg.startTime) / totalDuration) * w
      const isSelected = seg.id === selectedId
      const isHover = seg.id === hoverId

      ctx.fillStyle = isSelected ? selectedFill : isHover ? hoverFill : defaultFill
      ctx.fillRect(x, 10, width, h - 20)

      ctx.strokeStyle = isSelected ? selectedStroke : defaultStroke
      ctx.lineWidth = isSelected ? 2 : 1
      ctx.strokeRect(x, 10, width, h - 20)
    })
  }, [segments, selectedId, hoverId, totalDuration, gridStroke, defaultFill, hoverFill, selectedFill, defaultStroke, selectedStroke])

  const handleClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const x = e.clientX - rect.left
    const clickTime = (x / canvas.width) * totalDuration

    const clicked = segments.find(s => clickTime >= s.startTime && clickTime <= s.endTime)
    if (clicked) onSelect(clicked.id)
  }

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const x = e.clientX - rect.left
    const hoverTime = (x / canvas.width) * totalDuration

    const hovered = segments.find(s => hoverTime >= s.startTime && hoverTime <= s.endTime)
    setHoverId(hovered?.id ?? null)
  }

  return (
    <Box
      data-testid="subtitle-timeline-surface"
      data-grid-color={gridStroke}
      data-fill-colors={[defaultFill, hoverFill, selectedFill].join('|')}
      data-stroke-colors={[defaultStroke, selectedStroke].join('|')}
      sx={{ position: 'relative', width: '100%', height: 80, bgcolor: timelineBg, borderRadius: 1, overflow: 'hidden' }}
    >
      <canvas
        ref={canvasRef}
        width={800}
        height={80}
        style={{ width: '100%', height: '100%', cursor: 'pointer' }}
        onClick={handleClick}
        onMouseMove={handleMouseMove}
        onMouseLeave={() => setHoverId(null)}
      />
      <Typography variant="caption" sx={{ position: 'absolute', bottom: 2, right: 4, color: 'text.secondary' }}>
        总时长: {formatTime(totalDuration)}
      </Typography>
    </Box>
  )
}

export default function SubtitleEditorPage() {
  const { id } = useParams<{ id: string }>()
  const videoId = Number(id ?? 0)
  const hasValidVideoId = Number.isFinite(videoId) && videoId > 0
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const [segments, setSegments] = useState<SubtitleSegment[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState('')
  const [localDirty, setLocalDirty] = useState(false)

  const {
    data: loadedSegments = EMPTY_SUBTITLES,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['subtitles', videoId],
    queryFn: () => subtitleGet(videoId),
    enabled: hasValidVideoId,
  })

  useEffect(() => {
    if (localDirty) return
    setSegments(loadedSegments)
    setSelectedId((current) => loadedSegments.some((seg) => seg.id === current) ? current : null)
  }, [localDirty, loadedSegments])

  const saveMut = useMutation({
    mutationFn: () => subtitleSave(videoId, segments),
    onSuccess: () => {
      qc.setQueryData(['subtitles', videoId], segments)
      setLocalDirty(false)
      toast('字幕已保存到 sv_subtitle_segment', 'success')
      qc.invalidateQueries({ queryKey: ['subtitles', videoId] })
    },
    onError: (saveError) => toast(`保存失败：${getErrorMessage(saveError)}`, 'error'),
  })

  const totalDuration = segments.length > 0 ? Math.max(...segments.map(s => s.endTime)) : 60

  const selected = segments.find(s => s.id === selectedId) ?? null
  const isDirty = useMemo(() => localDirty || JSON.stringify(segments) !== JSON.stringify(loadedSegments), [localDirty, segments, loadedSegments])

  const updateSegment = (id: string, patch: Partial<SubtitleSegment>) => {
    setSegments(prev => prev.map(s => s.id === id ? { ...s, ...patch } : s))
    setLocalDirty(true)
  }

  const addSegment = () => {
    const last = segments[segments.length - 1]
    const newSeg: SubtitleSegment = {
      id: Date.now().toString(),
      startTime: last ? last.endTime : 0,
      endTime: last ? last.endTime + 3 : 3,
      text: '新字幕',
    }
    setSegments(prev => [...prev, newSeg])
    setSelectedId(newSeg.id)
    setLocalDirty(true)
  }

  const deleteSegment = (id: string) => {
    setSegments(prev => prev.filter(s => s.id !== id))
    if (selectedId === id) setSelectedId(null)
    setLocalDirty(true)
  }

  const handleExportSrt = async () => {
    setExporting(true)
    setExportError('')
    try {
      const srt = await subtitleExportSrt(videoId, isDirty ? segments : undefined)
      const blob = new Blob([srt], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `subtitles-${videoId}.srt`; a.click()
      URL.revokeObjectURL(url)
      toast('SRT 导出成功', 'success')
    } catch (error) {
      const message = getErrorMessage(error)
      setExportError(message)
      toast(`导出失败：${message}`, 'error')
    } finally {
      setExporting(false)
    }
  }

  return (
    <Box
      data-testid="subtitle-editor-page"
      data-ready-endpoints={SUBTITLE_READY_ENDPOINTS.join('|')}
      data-ready-routes={SUBTITLE_READY_ROUTES}
      data-supported-actions={SUBTITLE_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={SUBTITLE_UNSUPPORTED_ENDPOINTS.join('|')}
      data-no-mock-subtitle-fallback="true"
      data-video-id-valid={hasValidVideoId ? 'true' : 'false'}
      sx={{ p: 3 }}
    >
      <PageHeader
        title="字幕编辑器"
        subtitle={`${hasValidVideoId ? `视频 #${videoId}` : '未选择有效视频'} · ${segments.length} 条字幕`}
        actions={
          <Stack direction="row" spacing={1}>
            <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)} data-testid="subtitle-editor-back-button">返回</Button>
            <Button
              startIcon={<RefreshIcon />}
              onClick={() => refetch()}
              disabled={isLoading || !hasValidVideoId}
              data-testid="subtitle-editor-refresh-button"
              data-source-endpoint={SUBTITLE_GET_ENDPOINT}
            >
              刷新
            </Button>
            <Button
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={handleExportSrt}
              disabled={exporting || segments.length === 0 || !hasValidVideoId}
              data-testid="subtitle-editor-export-button"
              data-source-endpoint={SUBTITLE_EXPORT_ENDPOINT}
            >
              {exporting ? <CircularProgress size={16} /> : '导出 SRT'}
            </Button>
            <Button
              variant="contained"
              startIcon={saveMut.isPending ? <CircularProgress size={16} color="inherit" /> : <SaveIcon />}
              onClick={() => saveMut.mutate()}
              disabled={saveMut.isPending || !hasValidVideoId}
              data-testid="subtitle-editor-save-button"
              data-source-endpoint={SUBTITLE_SAVE_ENDPOINT}
            >
              {saveMut.isPending ? '保存中...' : '保存'}
            </Button>
          </Stack>
        }
      />

      <Alert data-testid="subtitle-editor-boundary-contract" severity="info" data-supported-actions={SUBTITLE_SUPPORTED_ACTIONS} sx={{ mb: 2 }}>
        字幕已接入后端 POST {SUBTITLE_GET_ENDPOINT}|{SUBTITLE_SAVE_ENDPOINT}|{SUBTITLE_EXPORT_ENDPOINT}，保存会写入 `sv_subtitle_segment`；当前页面仍允许先在浏览器内批量编辑后一次性提交，未保存编辑会本地导出 SRT，已同步状态会走后端导出接口。
      </Alert>

      {!hasValidVideoId && (
        <Alert data-testid="subtitle-editor-invalid-video-id" data-no-placeholder-video="true" severity="warning" sx={{ mb: 2 }}>
          当前 URL 未携带有效视频 ID，字幕查询、保存和导出接口会被禁用。
        </Alert>
      )}

      {isError && (
        <Alert
          data-testid="subtitle-editor-load-error"
          data-no-mock-subtitle-fallback="true"
          severity="error"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
        >
          字幕加载失败（POST {SUBTITLE_GET_ENDPOINT}）：{getErrorMessage(error)}。请检查视频 ID、登录态和 `sv_subtitle_segment` 表；页面不会用模拟字幕覆盖本地编辑。
        </Alert>
      )}

      {saveMut.isError && (
        <Alert data-testid="subtitle-editor-save-error" data-input-retained="true" data-no-local-save="true" severity="error" sx={{ mb: 2 }}>
          字幕保存失败（POST {SUBTITLE_SAVE_ENDPOINT}）：{getErrorMessage(saveMut.error)}。当前字幕段、选中项和样式编辑会保留，可修复后重试。
        </Alert>
      )}

      {exportError && (
        <Alert data-testid="subtitle-editor-export-error" data-input-retained="true" severity="error" sx={{ mb: 2 }}>
          SRT 导出失败（POST {SUBTITLE_EXPORT_ENDPOINT}）：{exportError}。本地字幕段会保留，可先保存或修正视频 ID 后重试。
        </Alert>
      )}

      <Alert data-testid="subtitle-editor-dirty-state" data-dirty={isDirty ? 'true' : 'false'} severity={isDirty ? 'warning' : 'success'} sx={{ mb: 2 }}>
        {isDirty ? '当前字幕有未保存更改，点击「保存」会写入 sv_subtitle_segment。' : '当前字幕已与后端数据同步。'}
      </Alert>

      <Grid container spacing={2}>
        {/* 时间轴列表 */}
        <Grid item xs={12} md={5}>
          <Card data-testid="subtitle-editor-timeline-list">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle2">字幕时间轴</Typography>
                <Button size="small" startIcon={<AddIcon />} onClick={addSegment} data-testid="subtitle-editor-add-segment-button">添加</Button>
              </Stack>
              <Divider sx={{ mb: 1 }} />
              {isLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress />
                </Box>
              ) : segments.length === 0 ? (
                <Alert data-testid="subtitle-editor-empty" data-no-static-subtitles="true" severity="info" sx={{ my: 2 }}>
                  暂无字幕。可点击「添加」手工新建；若需要自动生成，请先在剪辑流程调用 `/short-video/edit/generate-subtitles`。
                </Alert>
              ) : (
                <Stack spacing={0.5} sx={{ maxHeight: 520, overflowY: 'auto' }}>
                  {segments.map((seg, idx) => (
                    <Paper key={seg.id}
                      data-testid={selectedId === seg.id ? 'subtitle-selected-segment' : undefined}
                      onClick={() => setSelectedId(seg.id)}
                      sx={(theme) => ({
                        p: 1.5, cursor: 'pointer',
                        bgcolor: selectedId === seg.id
                          ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.18 : 0.08)
                          : 'background.paper',
                        border: 1,
                        borderColor: selectedId === seg.id ? 'primary.main' : 'divider',
                        '&:hover': { borderColor: 'primary.light' },
                      })}
                    >
                      <Stack direction="row" alignItems="center" spacing={1}>
                        <Chip label={idx + 1} size="small" sx={{ minWidth: 28 }} />
                        <Box flex={1}>
                          <Typography variant="caption" color="text.secondary">
                            {formatTime(seg.startTime)} → {formatTime(seg.endTime)}
                          </Typography>
                          <Typography variant="body2" noWrap>{seg.text}</Typography>
                        </Box>
                        <IconButton size="small" onClick={e => { e.stopPropagation(); deleteSegment(seg.id) }}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Stack>
                    </Paper>
                  ))}
                </Stack>
              )}
              {segments.length > 0 && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Box>
                    <Typography variant="caption" color="text.secondary" gutterBottom display="block">
                      可视化时间轴（点击选择字幕段）
                    </Typography>
                    <TimelineBar
                      segments={segments}
                      selectedId={selectedId}
                      onSelect={setSelectedId}
                      totalDuration={totalDuration}
                    />
                  </Box>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* 编辑面板 */}
        <Grid item xs={12} md={7}>
          {selected ? (
            <Card data-testid="subtitle-editor-panel">
              <CardContent>
                <Typography variant="subtitle2" gutterBottom>编辑字幕段</Typography>
                <Divider sx={{ mb: 2 }} />
                <Stack spacing={2}>
                  <TextField
                    label="字幕文字"
                    value={selected.text}
                    onChange={e => updateSegment(selected.id, { text: e.target.value })}
                    multiline minRows={2} fullWidth
                  />
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <TextField label="开始时间 (mm:ss.xx)" fullWidth size="small"
                        value={formatTime(selected.startTime)}
                        onChange={e => { const t = parseTime(e.target.value); if (!isNaN(t)) updateSegment(selected.id, { startTime: t }) }}
                      />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField label="结束时间 (mm:ss.xx)" fullWidth size="small"
                        value={formatTime(selected.endTime)}
                        onChange={e => { const t = parseTime(e.target.value); if (!isNaN(t)) updateSegment(selected.id, { endTime: t }) }}
                      />
                    </Grid>
                  </Grid>
                  <Box>
                    <Typography variant="caption" gutterBottom>字体大小：{selected.fontSize ?? 16}px</Typography>
                    <Slider
                      value={selected.fontSize ?? 16} min={10} max={40} step={1}
                      onChange={(_e, v) => updateSegment(selected.id, { fontSize: v as number })}
                    />
                  </Box>
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <FormControl fullWidth size="small">
                        <InputLabel>位置</InputLabel>
                        <Select value={selected.position ?? 'bottom'} label="位置"
                          onChange={e => updateSegment(selected.id, { position: e.target.value })}>
                          <MenuItem value="top">顶部</MenuItem>
                          <MenuItem value="center">居中</MenuItem>
                          <MenuItem value="bottom">底部</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>
                    <Grid item xs={6}>
                      <TextField label="颜色" fullWidth size="small"
                        value={selected.color ?? SUBTITLE_PREVIEW_FALLBACK_COLOR}
                        onChange={e => updateSegment(selected.id, { color: e.target.value })}
                        InputProps={{ startAdornment: (
                          <Box
                            data-testid="subtitle-color-swatch"
                            data-preview-tone="media-subtitle-fallback"
                            data-preview-color={selected.color ?? SUBTITLE_PREVIEW_FALLBACK_COLOR}
                            sx={{ width: 20, height: 20, borderRadius: 0.5, bgcolor: selected.color ?? SUBTITLE_PREVIEW_FALLBACK_COLOR, border: '1px solid', borderColor: 'divider', mr: 1 }}
                          />
                        )}}
                      />
                    </Grid>
                  </Grid>
                  {/* 预览 */}
                  <Paper
                    data-testid="subtitle-media-preview-surface"
                    data-preview-tone="media-preview"
                    data-preview-background={SUBTITLE_PREVIEW_BACKGROUND}
                    data-subtitle-color={selected.color ?? SUBTITLE_PREVIEW_FALLBACK_COLOR}
                    sx={{
                    height: 80, bgcolor: SUBTITLE_PREVIEW_BACKGROUND, display: 'flex', alignItems:
                      selected.position === 'top' ? 'flex-start' : selected.position === 'center' ? 'center' : 'flex-end',
                    justifyContent: 'center', borderRadius: 1, px: 2,
                  }}>
                    <Typography
                      data-testid="subtitle-media-preview-text"
                      data-preview-tone="media-subtitle"
                      sx={{
                      fontSize: selected.fontSize ?? 16,
                      color: selected.color ?? SUBTITLE_PREVIEW_FALLBACK_COLOR,
                      textAlign: 'center',
                    }}>
                      {selected.text}
                    </Typography>
                  </Paper>
                </Stack>
              </CardContent>
            </Card>
          ) : (
            <Paper sx={{ p: 6, textAlign: 'center', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Typography color="text.secondary">点击左侧字幕段进行编辑</Typography>
            </Paper>
          )}
        </Grid>
      </Grid>
    </Box>
  )
}
