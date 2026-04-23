import { useState } from 'react'
import { IconButton, Menu, MenuItem, ListItemIcon, ListItemText, Tooltip } from '@mui/material'
import MoreHorizIcon from '@mui/icons-material/MoreHoriz'
import type { ReactNode } from 'react'

export interface RowAction {
  key: string
  icon: ReactNode
  label: string
  onClick: () => void
  color?: 'default' | 'primary' | 'error'
  confirm?: string
  disabled?: boolean
}

export interface RowActionsProps {
  actions: RowAction[]
  /** 内联展示的最多操作数，默认 3 */
  inlineMax?: number
}

/** 行内操作 + 溢出菜单 */
export function RowActions({ actions, inlineMax = 3 }: RowActionsProps) {
  const [anchor, setAnchor] = useState<null | HTMLElement>(null)
  const open = Boolean(anchor)

  const run = (a: RowAction) => {
    setAnchor(null)
    if (a.confirm && !window.confirm(a.confirm)) return
    a.onClick()
  }

  if (!actions.length) return null

  const inline = actions.slice(0, inlineMax)
  const overflow = actions.slice(inlineMax)

  return (
    <span onClick={(e) => e.stopPropagation()}>
      {inline.map((a) => (
        <Tooltip key={a.key} title={a.label}>
          <span>
            <IconButton
              size="small"
              color={a.color === 'error' ? 'error' : a.color === 'primary' ? 'primary' : 'default'}
              disabled={a.disabled}
              onClick={() => run(a)}
              aria-label={a.label}
            >
              {a.icon}
            </IconButton>
          </span>
        </Tooltip>
      ))}
      {overflow.length > 0 && (
        <>
          <Tooltip title="更多">
            <IconButton size="small" onClick={(e) => setAnchor(e.currentTarget)} aria-label="更多操作">
              <MoreHorizIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Menu anchorEl={anchor} open={open} onClose={() => setAnchor(null)}>
            {overflow.map((a) => (
              <MenuItem
                key={a.key}
                disabled={a.disabled}
                onClick={() => run(a)}
                sx={a.color === 'error' ? { color: 'error.main' } : undefined}
              >
                <ListItemIcon sx={{ minWidth: 32, color: 'inherit' }}>{a.icon}</ListItemIcon>
                <ListItemText primary={a.label} />
              </MenuItem>
            ))}
          </Menu>
        </>
      )}
    </span>
  )
}
