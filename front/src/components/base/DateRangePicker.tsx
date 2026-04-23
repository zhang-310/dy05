import { Box, TextField } from '@mui/material'

interface DateRangePickerProps {
  start: string
  end: string
  onStartChange: (v: string) => void
  onEndChange: (v: string) => void
  labelStart?: string
  labelEnd?: string
}

/** 日期范围选择器（基于 HTML date input，无需 @mui/x-date-pickers） */
export function DateRangePicker({
  start,
  end,
  onStartChange,
  onEndChange,
  labelStart = '开始日期',
  labelEnd = '结束日期',
}: DateRangePickerProps) {
  return (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
      <TextField
        label={labelStart}
        type="date"
        value={start}
        onChange={(e) => onStartChange(e.target.value)}
        size="small"
        InputLabelProps={{ shrink: true }}
        inputProps={{ max: end || undefined }}
      />
      <TextField
        label={labelEnd}
        type="date"
        value={end}
        onChange={(e) => onEndChange(e.target.value)}
        size="small"
        InputLabelProps={{ shrink: true }}
        inputProps={{ min: start || undefined }}
      />
    </Box>
  )
}
