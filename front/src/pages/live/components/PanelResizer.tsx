import { memo, useCallback } from 'react'
import { Box } from '@mui/material'

export interface PanelResizerProps {
  /** 拖拽时回调，deltaX 正值表示向右拖 */
  onResize: (deltaX: number) => void
  /** 双击恢复默认宽度 */
  onReset?: () => void
}

const STORAGE_LEFT = 'live-script-left-width'
const STORAGE_RIGHT = 'live-script-right-width'
export const DEFAULT_LEFT = 280
export const DEFAULT_RIGHT = 380
const MIN_LEFT = 200
const MAX_LEFT = 400
const MIN_RIGHT = 320
const MAX_RIGHT = 500

export function getStoredLeftWidth(): number {
  try {
    const v = localStorage.getItem(STORAGE_LEFT)
    if (v) {
      const n = parseInt(v, 10)
      if (n >= MIN_LEFT && n <= MAX_LEFT) return n
    }
  } catch { /* ignore */ }
  return DEFAULT_LEFT
}

export function getStoredRightWidth(): number {
  try {
    const v = localStorage.getItem(STORAGE_RIGHT)
    if (v) {
      const n = parseInt(v, 10)
      if (n >= MIN_RIGHT && n <= MAX_RIGHT) return n
    }
  } catch { /* ignore */ }
  return DEFAULT_RIGHT
}

export function setStoredLeftWidth(w: number) {
  try {
    localStorage.setItem(STORAGE_LEFT, String(Math.round(w)))
  } catch { /* ignore */ }
}

export function setStoredRightWidth(w: number) {
  try {
    localStorage.setItem(STORAGE_RIGHT, String(Math.round(w)))
  } catch { /* ignore */ }
}

/** 可拖拽调整相邻面板大小的分隔条 */
export const PanelResizer = memo(function PanelResizer({ onResize, onReset }: PanelResizerProps) {
  const handleDoubleClick = useCallback(() => {
    onReset?.()
  }, [onReset])

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      if (e.button !== 0) return
      e.preventDefault()
      let startX = e.clientX
      const handleMouseMove = (ev: MouseEvent) => {
        const delta = ev.clientX - startX
        startX = ev.clientX
        onResize(delta)
      }
      const handleMouseUp = () => {
        document.body.style.cursor = ''
        document.body.style.userSelect = ''
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
      }
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
    },
    [onResize]
  )

  return (
    <Box
      component="div"
      role="separator"
      aria-orientation="vertical"
      onMouseDown={handleMouseDown}
      onDoubleClick={handleDoubleClick}
      title={onReset ? '拖拽调节宽度，双击恢复默认' : '拖拽调节宽度'}
      sx={{
        width: 6,
        flexShrink: 0,
        cursor: 'col-resize',
        bgcolor: 'transparent',
        '&:hover': { bgcolor: 'action.hover' },
        transition: 'background-color 0.15s',
      }}
    />
  )
})
