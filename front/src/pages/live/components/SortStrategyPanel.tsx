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
    <Box
      sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
      data-testid="sort-strategy-panel"
      data-contract-scope="live-sort-strategy-props"
      data-ready-sources="value-prop|onChange-prop|onReverse-prop|onAiSort-prop"
      data-ready-endpoints="/live/ai/sort-suggest"
      data-no-direct-api-request="true"
      data-sort-strategy={value}
      data-ai-loading={aiLoading ? 'true' : 'false'}
      data-ai-disabled={aiDisabled ? 'true' : 'false'}
    >
      <SortIcon fontSize="small" color="action" />
      <Typography variant="caption" color="text.secondary">排序:</Typography>
      <ToggleButtonGroup
        data-testid="sort-strategy-toggle-group"
        data-contract-source="client-view-sort"
        data-no-local-ai-sort-fallback="true"
        size="small"
        value={value}
        exclusive
        onChange={(_, v) => v && onChange(v)}
        sx={{ '& .MuiToggleButton-root': { py: 0.25, px: 1, fontSize: 12 } }}
      >
        <ToggleButton value="manual" data-testid="sort-strategy-manual-button" data-contract-source="onChange-prop" data-sort-strategy="manual">手动</ToggleButton>
        <ToggleButton value="type" data-testid="sort-strategy-type-button" data-contract-source="onChange-prop" data-sort-strategy="type">按类型</ToggleButton>
        <ToggleButton value="alpha" data-testid="sort-strategy-alpha-button" data-contract-source="onChange-prop" data-sort-strategy="alpha">字母序</ToggleButton>
        <ToggleButton value="price" data-testid="sort-strategy-price-button" data-contract-source="onChange-prop" data-sort-strategy="price">按价格</ToggleButton>
      </ToggleButtonGroup>

      {onReverse && (
        <Tooltip title="一键倒序">
          <IconButton size="small" onClick={onReverse} data-testid="sort-strategy-reverse-button" data-contract-source="onReverse-prop">
            <SwapVertIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      )}

      {onAiSort && (
        <Button
          size="small"
          variant="outlined"
          color="secondary"
          data-testid="sort-strategy-ai-button"
          data-contract-source="/live/ai/sort-suggest|onAiSort-prop"
          data-no-local-ai-sort-fallback="true"
          data-disabled-reason={aiLoading ? 'ai-loading' : (aiDisabled ? 'ai-disabled' : 'ready')}
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
