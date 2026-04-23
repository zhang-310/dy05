import { useMemo, useCallback } from 'react'
import { Box, Typography, Chip } from '@mui/material'
import ReactECharts from 'echarts-for-react'

const PRESETS: Record<string, { label: string; curve: string }> = {
  hook_climax: { label: 'Hook高潮型', curve: '90→60→40→80→100→70' },
  slow_build: { label: '渐进升温型', curve: '40→50→60→75→90→100' },
  rollercoaster: { label: '过山车型', curve: '80→40→90→30→100→60' },
  suspense: { label: '悬念爆发型', curve: '60→70→50→40→30→100' },
  emotional_wave: { label: '情感波浪型', curve: '70→90→50→85→40→95' },
}

export interface EmotionCurveEditorProps {
  value: string
  onChange: (curve: string) => void
  duration?: number
}

function parseCurve(curve: string): number[] {
  if (!curve?.trim()) return []
  return curve.split('→').map((s) => parseInt(s.trim(), 10)).filter((n) => !Number.isNaN(n))
}

export function EmotionCurveEditor({ value, onChange, duration = 30 }: EmotionCurveEditorProps) {
  const points = useMemo(() => parseCurve(value), [value])

  const handlePresetClick = useCallback(
    (presetKey: string) => {
      const preset = PRESETS[presetKey]
      if (preset) onChange(preset.curve)
    },
    [onChange]
  )

  const chartOption = useMemo(() => {
    const pts = points.length > 0 ? points : [50, 50]
    const step = duration / Math.max(1, pts.length - 1)
    const xData = pts.map((_, i) => (i * step).toFixed(1))
    return {
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: xData,
        name: '时间(秒)',
        nameLocation: 'middle',
        nameGap: 25,
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        name: '情绪值',
        nameLocation: 'middle',
        nameGap: 35,
      },
      series: [
        {
          type: 'line',
          data: pts,
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 2 },
          areaStyle: { opacity: 0.2 },
        },
      ],
    }
  }, [points, duration])

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
        情绪曲线设计
      </Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
        {Object.entries(PRESETS).map(([key, preset]) => (
          <Chip
            key={key}
            label={preset.label}
            size="small"
            color={value === preset.curve ? 'secondary' : 'default'}
            variant={value === preset.curve ? 'filled' : 'outlined'}
            onClick={() => handlePresetClick(key)}
          />
        ))}
      </Box>
      {points.length > 0 && (
        <Box sx={{ height: 200, width: '100%' }}>
          <ReactECharts option={chartOption} style={{ height: '100%', width: '100%' }} opts={{ renderer: 'canvas' }} />
        </Box>
      )}
      {value && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          当前曲线：{value}
        </Typography>
      )}
    </Box>
  )
}
