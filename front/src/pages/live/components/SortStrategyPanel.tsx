import { memo } from 'react'
import { Box, Typography, ToggleButtonGroup, ToggleButton, Button, Tooltip, IconButton, CircularProgress } from '@mui/material'
import SortIcon from '@mui/icons-material/Sort'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import SwapVertIcon from '@mui/icons-material/SwapVert'

export type SortStrategy = 'manual' | 'type' | 'alpha' | 'price'

export interface SortStrategyPanelProps {
  value: SortStrategy
  onChange: (v: SortStrategy) => void
  onReverse?: () => void
  aiLoading?: boolean
  onAiSort?: () => void
  aiDisabled?: boolean
}

export const SortStrategyPanel = memo(function SortStrategyPanel({
  value, onChange, onReverse, aiLoading, onAiSort, aiDisabled,
}: SortStrategyPanelProps) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <SortIcon fontSize="small" color="action" />
      <Typography variant="caption" color="text.secondary">排序:</Typography>
      <ToggleButtonGroup
        size="small"
        value={value}
        exclusive
        onChange={(_, v) => v && onChange(v)}
        sx={{ '& .MuiToggleButton-root': { py: 0.25, px: 1, fontSize: 12 } }}
      >
        <ToggleButton value="manual">手动</ToggleButton>
        <ToggleButton value="type">按类型</ToggleButton>
        <ToggleButton value="alpha">字母序</ToggleButton>
        <ToggleButton value="price">按价格</ToggleButton>
      </ToggleButtonGroup>

      {onReverse && (
        <Tooltip title="一键倒序">
          <IconButton size="small" onClick={onReverse}>
            <SwapVertIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      )}

      {onAiSort && (
        <Button
          size="small"
          variant="outlined"
          color="secondary"
          startIcon={aiLoading ? <CircularProgress size={14} /> : <AutoAwesomeIcon />}
          disabled={aiLoading || aiDisabled}
          onClick={onAiSort}
          sx={{ fontSize: 12 }}
        >
          AI排品
        </Button>
      )}
    </Box>
  )
})
