import { Box, Typography, Button } from '@mui/material'
import { InboxOutlined } from '@mui/icons-material'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon?: ReactNode
  title?: string
  description?: string
  action?: {
    text: string
    onClick: () => void
  }
}

/**
 * EmptyState - 空状态组件
 * 应用设计系统规范：
 * - 图标颜色：color-text-secondary
 * - 标题：18px, 600 weight
 * - 描述：14px, color-text-secondary
 * - 间距：24px (spacing-lg)
 */
export function EmptyState({
  icon = <InboxOutlined sx={{ fontSize: 64, color: '#475569' }} />,
  title = '暂无数据',
  description,
  action,
}: EmptyStateProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        py: 12, // spacing-2xl (48px)
        px: 2, // spacing-md
      }}
    >
      {icon}
      <Typography
        variant="h6"
        sx={{
          mt: 3, // spacing-lg (24px)
          color: '#475569', // color-text-secondary
          fontSize: '18px', // font-size-lg
          fontWeight: 600, // font-weight-semibold
        }}
      >
        {title}
      </Typography>
      {description && (
        <Typography
          variant="body2"
          sx={{
            mt: 1, // spacing-sm (8px)
            color: '#475569', // color-text-secondary
            fontSize: '14px', // font-size-base
            textAlign: 'center',
          }}
        >
          {description}
        </Typography>
      )}
      {action && (
        <Button
          variant="contained"
          color="primary"
          sx={{
            mt: 4, // spacing-xl (32px)
            minHeight: '44px',
            padding: '12px 16px',
            borderRadius: '8px',
            fontWeight: 600,
            fontSize: '14px',
            transition: 'all 200ms cubic-bezier(0, 0, 0.2, 1)',
          }}
          onClick={action.onClick}
        >
          {action.text}
        </Button>
      )}
    </Box>
  )
}
