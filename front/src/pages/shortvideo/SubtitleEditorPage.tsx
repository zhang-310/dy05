import { useState, useRef, useEffect } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Button, TextField,
  IconButton, CircularProgress, Divider, Chip, Paper, Grid, Alert,
  Slider, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material'
import SaveIcon from '@mui/icons-material/Save'
import DownloadIcon from '@mui/icons-material/Download'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { subtitleGet, subtitleSave, subtitleExportSrt, type SubtitleSegment } from '@/api/shortvideo'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'

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

// 时间轴可视化组件
function TimelineBar({ segments, selectedId, onSelect, totalDuration }: {
  segments: SubtitleSegment[]
  selectedId: string | null
  onSelect: (id: string) => void
  totalDuration: number
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [hoverId, setHoverId] = useState<string | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const w = canvas.width
    const h = canvas.height
    ctx.clearRect(0, 0, w, h)

    // 背景网格
    ctx.strokeStyle = '#e0e0e0'
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

      ctx.fillStyle = isSelected ? '#1976d2' : isHover ? '#42a5f5' : '#90caf9'
      ctx.fillRect(x, 10, width, h - 20)

      ctx.strokeStyle = isSelected ? '#0d47a1' : '#1976d2'
      ctx.lineWidth = isSelected ? 2 : 1
      ctx.strokeRect(x, 10, width, h - 20)
    })
  }, [segments, selectedId, hoverId, totalDuration])

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
    <Box sx={{ position: 'relative', width: '100%', height: 80, bgcolor: '#fafafa', borderRadius: 1, overflow: 'hidden' }}>
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
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const [segments, setSegments] = useState<SubtitleSegment[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)

  const { isLoading } = useQuery({
    queryKey: ['subtitles', videoId],
    queryFn: () => subtitleGet(videoId),
    enabled: videoId > 0,
    onSuccess: (data: SubtitleSegment[]) => setSegments(data),
  } as Parameters<typeof useQuery>[0])

  const saveMut = useMutation({
    mutationFn: () => subtitleSave(videoId, segments),
    onSuccess: () => { toast('字幕已保存', 'success'); qc.invalidateQueries({ queryKey: ['subtitles', videoId] }) },
    onError: () => toast('保存失败', 'error'),
  })

  const totalDuration = segments.length > 0 ? Math.max(...segments.map(s => s.endTime)) : 60

  const selected = segments.find(s => s.id === selectedId) ?? null

  const updateSegment = (id: string, patch: Partial<SubtitleSegment>) => {
    setSegments(prev => prev.map(s => s.id === id ? { ...s, ...patch } : s))
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
  }

  const deleteSegment = (id: string) => {
    setSegments(prev => prev.filter(s => s.id !== id))
    if (selectedId === id) setSelectedId(null)
  }

  const handleExportSrt = async () => {
    setExporting(true)
    try {
      const srt = await subtitleExportSrt(videoId, segments)
      const blob = new Blob([srt], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `subtitles-${videoId}.srt`; a.click()
      URL.revokeObjectURL(url)
      toast('SRT 导出成功', 'success')
    } catch {
      toast('导出失败', 'error')
    } finally {
      setExporting(false)
    }
  }

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="字幕编辑器"
        subtitle={`视频 #${videoId} · ${segments.length} 条字幕`}
        actions={
          <Stack direction="row" spacing={1}>
            <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>返回</Button>
            <Button variant="outlined" startIcon={<DownloadIcon />}
              onClick={handleExportSrt} disabled={exporting || segments.length === 0}>
              {exporting ? <CircularProgress size={16} /> : '导出 SRT'}
            </Button>
            <Button variant="contained" startIcon={<SaveIcon />}
              onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>
              保存
            </Button>
          </Stack>
        }
      />

      <Alert severity="info" sx={{ mb: 2 }}>
        字幕数据当前为浏览器内编辑；持久化请通过项目时间线 /剪辑流程对接后端。
      </Alert>

      <Grid container spacing={2}>
        {/* 时间轴列表 */}
        <Grid item xs={12} md={5}>
          <Card>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle2">字幕时间轴</Typography>
                <Button size="small" startIcon={<AddIcon />} onClick={addSegment}>添加</Button>
              </Stack>
              <Divider sx={{ mb: 1 }} />
              {isLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress />
                </Box>
              ) : segments.length === 0 ? (
                <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
                  暂无字幕，点击「添加」新建
                </Typography>
              ) : (
                <Stack spacing={0.5} sx={{ maxHeight: 520, overflowY: 'auto' }}>
                  {segments.map((seg, idx) => (
                    <Paper key={seg.id}
                      onClick={() => setSelectedId(seg.id)}
                      sx={{
                        p: 1.5, cursor: 'pointer',
                        bgcolor: selectedId === seg.id ? 'primary.50' : 'background.paper',
                        border: 1,
                        borderColor: selectedId === seg.id ? 'primary.main' : 'divider',
                        '&:hover': { borderColor: 'primary.light' },
                      }}
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
            <Card>
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
                        value={selected.color ?? '#ffffff'}
                        onChange={e => updateSegment(selected.id, { color: e.target.value })}
                        InputProps={{ startAdornment: (
                          <Box sx={{ width: 20, height: 20, borderRadius: 0.5, bgcolor: selected.color ?? '#ffffff', border: '1px solid', borderColor: 'divider', mr: 1 }} />
                        )}}
                      />
                    </Grid>
                  </Grid>
                  {/* 预览 */}
                  <Paper sx={{
                    height: 80, bgcolor: '#000', display: 'flex', alignItems:
                      selected.position === 'top' ? 'flex-start' : selected.position === 'center' ? 'center' : 'flex-end',
                    justifyContent: 'center', borderRadius: 1, px: 2,
                  }}>
                    <Typography sx={{
                      fontSize: selected.fontSize ?? 16,
                      color: selected.color ?? '#ffffff',
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