
import { Box, Button, Stack } from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import RestartAltIcon from '@mui/icons-material/RestartAlt'

interface FilterPanelProps {
  children: React.ReactNode
  onSearch: () => void
  onReset: () => void
}

/**
 * FilterPanel - 筛选面板组件
 * 应用设计系统规范：
 * - 背景：color-surface #1E293B
 * - 圆角：8px (border-radius-lg)
 * - 边框：color-surface-light #334155
 * - 间距：16px (spacing-md) 内边距
 * - 按钮：primary 和 outlined 样式
 */
export function FilterPanel({ children, onSearch, onReset }: FilterPanelProps) {
  return (
    <Box
      sx={{
        padding: '16px', // spacing-md
        marginBottom: '16px', // spacing-md
        backgroundColor: '#1E293B', // color-surface
        borderRadius: '8px', // border-radius-lg
        border: '1px solid',
        borderColor: '#334155', // color-surface-light
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)', // shadow-elevation-1
        transition: 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-base + ease-in-out
      }}
    >
      <Stack direction="row" flexWrap="wrap" gap={2} alignItems="center">
        {children}
        <Stack direction="row" gap={1} ml="auto">
          <Button
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
            variant="outlined"
            startIcon={<RestartAltIcon />}
            onClick={onReset}
            sx={{
              minHeight: '44px',
              padding: '12px 16px',
              borderRadius: '8px', // border-radius-lg
              borderColor: '#334155', // color-surface-light
              color: '#F1F5F9', // color-text-primary
              fontWeight: 600,
              fontSize: '14px', // font-size-base
              transition: 'all 200ms cubic-bezier(0, 0, 0.2, 1)',
              '&:hover': {
                borderColor: '#00D084', // color-primary
                backgroundColor: 'rgba(0, 208, 132, 0.05)',
              },
            }}
          >
            重置
          </Button>
        </Stack>
      </Stack>
    </Box>
  )
}
