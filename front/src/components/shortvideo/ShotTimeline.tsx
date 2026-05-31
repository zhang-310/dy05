import { Box, Typography, Slider, Paper } from '@mui/material'

export interface ShotTimelineItem {
  shotNumber: number
  duration: number
  label?: string
}

interface ShotTimelineProps {
  shots: ShotTimelineItem[]
  totalDuration: number
  onDurationChange: (shotIndex: number, duration: number) => void
}

/** 分镜时间轴：可拖拽调整每镜时长 */
export function ShotTimeline({ shots, totalDuration, onDurationChange }: ShotTimelineProps) {
  if (shots.length === 0) return null

  return (
    <Paper
      variant="outlined"
      data-testid="shortvideo-shot-timeline"
      sx={(theme) => ({
        p: 2,
        bgcolor: theme.palette.mode === 'dark' ? theme.palette.background.paper : theme.palette.grey[50],
      })}
    >
      <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
        时间轴 · 总时长 {totalDuration}s · 拖拽调整每镜时长
      </Typography>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
        {shots.map((shot, i) => {
          const widthPercent = (shot.duration / totalDuration) * 100
          return (
            <Box
              key={i}
              sx={{
                flex: `0 0 ${Math.max(widthPercent, 8)}%`,
                minWidth: 48,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 0.5,
              }}
            >
              <Box
                sx={{
                  width: '100%',
                  height: 36,
                  borderRadius: 1,
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 12,
                }}
              >
                #{shot.shotNumber}
              </Box>
              <Slider
                size="small"
                value={shot.duration}
                min={1}
                max={Math.max(15, totalDuration)}
                step={1}
                onChange={(_, v) => onDurationChange(i, typeof v === 'number' ? v : v[0])}
                sx={{ width: 80 }}
                valueLabelDisplay="auto"
                valueLabelFormat={(v) => `${v}s`}
              />
            </Box>
          )
        })}
      </Box>
    </Paper>
  )
}

/** 从 timeRange 解析时长，如 "0-3s" -> 3, "3-10s" -> 7 */
export function parseDurationFromTimeRange(timeRange?: string): number {
  if (!timeRange) return 5
  const m = timeRange.match(/(\d+)-(\d+)s?/)
  if (m) return Math.max(1, parseInt(m[2], 10) - parseInt(m[1], 10))
  const single = timeRange.match(/(\d+)s?/)
  return single ? Math.max(1, parseInt(single[1], 10)) : 5
}

/** 根据 durations 生成 timeRange 数组 */
export function buildTimeRanges(durations: number[]): string[] {
  let start = 0
  return durations.map((d) => {
    const end = start + d
    const s = `${start}-${end}s`
    start = end
    return s
  })
}
