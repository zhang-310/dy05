import { Box, Button, Stack } from '@mui/material'
import { alpha } from '@mui/material/styles'
import SearchIcon from '@mui/icons-material/Search'
import RestartAltIcon from '@mui/icons-material/RestartAlt'
import type { ReactNode } from 'react'

interface FilterPanelProps {
  children: ReactNode
  onSearch?: () => void
  onReset?: () => void
}

/**
 * FilterPanel - 筛选面板组件
 * 应用设计系统规范：
 * - 背景：background.paper
 * - 圆角：8px (border-radius-lg)
 * - 边框：divider
 * - 间距：16px (spacing-md) 内边距
 * - 按钮：primary 和 outlined 样式
 */
export function FilterPanel({ children, onSearch, onReset }: FilterPanelProps) {
  return (
    <Box
      data-testid="base-filter-panel-surface"
      data-filter-tone="surface"
      sx={(theme) => ({
        padding: '16px', // spacing-md
        marginBottom: '16px', // spacing-md
        backgroundColor: theme.palette.background.paper,
        borderRadius: '8px', // border-radius-lg
        border: '1px solid',
        borderColor: theme.palette.divider,
        boxShadow: `0 4px 12px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.28 : 0.08)}`,
        transition: 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-base + ease-in-out
      })}
    >
      <Stack direction="row" flexWrap="wrap" gap={2} alignItems="center">
        {children}
        <Stack direction="row" gap={1} ml="auto">
          <Button
            data-testid="base-filter-panel-search-action"
            variant="contained"
            color="primary"
            startIcon={<SearchIcon />}
            onClick={onSearch}
            sx={{
              minHeight: '44px',
              padding: '12px 16px', // spacing standard
              borderRadius: '8px', // border-radius-lg
              fontWeight: 600, // font-weight-semibold
              fontSize: '14px', // font-size-base
              transition: 'all 200ms cubic-bezier(0, 0, 0.2, 1)', // transition-fast + ease-out
            }}
          >
            查询
          </Button>
          <Button
            data-testid="base-filter-panel-reset-action"
            variant="outlined"
            startIcon={<RestartAltIcon />}
            onClick={onReset}
            sx={(theme) => ({
              minHeight: '44px',
              padding: '12px 16px',
              borderRadius: '8px', // border-radius-lg
              borderColor: theme.palette.divider,
              color: theme.palette.text.primary,
              fontWeight: 600,
              fontSize: '14px', // font-size-base
              transition: 'all 200ms cubic-bezier(0, 0, 0.2, 1)',
              '&:hover': {
                borderColor: theme.palette.primary.main,
                backgroundColor: alpha(theme.palette.primary.main, 0.06),
              },
            })}
          >
            重置
          </Button>
        </Stack>
      </Stack>
    </Box>
  )
}
