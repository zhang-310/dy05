import type { ReactNode } from 'react'
import {
  Box,
  Button,
  Chip,
  IconButton,
  TextField,
  Tooltip,
  InputAdornment,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import {
  GridToolbarContainer,
  GridToolbarColumnsButton,
  GridToolbarDensitySelector,
  GridToolbarExport,
} from '@mui/x-data-grid'

export interface StandardToolbarAction {
  label: string
  icon?: ReactNode
  onClick: () => void
  disabled?: boolean
}

export interface StandardToolbarProps {
  title?: string
  total?: number
  onRefresh?: () => void
  keyword?: string
  onKeywordChange?: (v: string) => void
  onSearch?: () => void
  searchPlaceholder?: string
  filters?: ReactNode
  primaryAction?: StandardToolbarAction
  secondaryActions?: StandardToolbarAction[]
  showColumnsButton?: boolean
  showDensitySelector?: boolean
  showExportButton?: boolean
}

/** 列表页统一工具栏（与 DataGrid slots.toolbar 配合） */
export function StandardToolbar({
  total,
  onRefresh,
  keyword,
  onKeywordChange,
  onSearch,
  searchPlaceholder = '搜索…',
  filters,
  primaryAction,
  secondaryActions,
  showColumnsButton = true,
  showDensitySelector = true,
  showExportButton = true,
}: StandardToolbarProps) {
  return (
    <GridToolbarContainer
      sx={{
        px: 1.5,
        py: 1,
        gap: 1,
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1, flex: 1, minWidth: 200 }}>
        {total != null && <Chip size="small" label={`共 ${total} 条`} variant="outlined" />}
        {keyword != null && onKeywordChange != null && (
          <TextField
            size="small"
            placeholder={searchPlaceholder}
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onSearch?.()}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
            sx={{ minWidth: 200 }}
          />
        )}
        {onSearch != null && (
          <Button size="small" variant="outlined" onClick={onSearch}>
            搜索
          </Button>
        )}
        {onRefresh != null && (
          <Tooltip title="刷新">
            <IconButton size="small" onClick={onRefresh}>
              <RefreshIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
        {filters}
      </Box>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 0.5 }}>
        {primaryAction && (
          <Button
            size="small"
            variant="contained"
            startIcon={primaryAction.icon}
            onClick={primaryAction.onClick}
          >
            {primaryAction.label}
          </Button>
        )}
        {secondaryActions?.map((a) => (
          <Button key={a.label} size="small" variant="outlined" startIcon={a.icon} onClick={a.onClick} disabled={a.disabled}>
            {a.label}
          </Button>
        ))}
        {(secondaryActions?.length || primaryAction) && (
          <Box sx={{ borderLeft: '1px solid', borderColor: 'divider', height: 24, mx: 0.5 }} />
        )}
        {showColumnsButton && <GridToolbarColumnsButton />}
        {showDensitySelector && <GridToolbarDensitySelector />}
        {showExportButton && <GridToolbarExport />}
      </Box>
    </GridToolbarContainer>
  )
}
