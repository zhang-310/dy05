import { Box, Drawer, IconButton, Typography } from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import type { ReactNode } from 'react'

export interface EntityDetailDrawerProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  width?: { xs?: number | string; sm?: number | string }
}

/**
 * UX-02A：统一右侧详情抽屉壳（Tab 内容放在 children 内）
 */
export function EntityDetailDrawer({
  open,
  title,
  onClose,
  children,
  width = { xs: '100%', sm: 520 },
}: EntityDetailDrawerProps) {
  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width } }}>
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Box
          sx={{
            px: 2,
            py: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: 1,
            borderColor: 'divider',
          }}
        >
          <Typography variant="subtitle1" fontWeight={600} noWrap sx={{ flex: 1, pr: 1 }}>
            {title}
          </Typography>
          <IconButton size="small" onClick={onClose} aria-label="关闭">
            <CloseIcon />
          </IconButton>
        </Box>
        <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>{children}</Box>
      </Box>
    </Drawer>
  )
}
