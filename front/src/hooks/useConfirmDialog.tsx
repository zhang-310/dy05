import { useState, useCallback, useRef, type ReactNode } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
} from '@mui/material'

interface ConfirmState {
  title: string
  message: ReactNode
  resolve: (ok: boolean) => void
}

/**
 * MUI 确认对话框 hook — 替代浏览器原生 confirm()
 *
 * @example
 * const { confirm, ConfirmDialog } = useConfirmDialog()
 *
 * const ok = await confirm('确认删除', '此操作不可撤销')
 * if (!ok) return
 *
 * // JSX 末尾渲染：
 * {ConfirmDialog}
 */
export function useConfirmDialog() {
  const [state, setState] = useState<ConfirmState | null>(null)
  const resolveRef = useRef<((ok: boolean) => void) | null>(null)

  const confirm = useCallback((title: string, message: ReactNode): Promise<boolean> => {
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve
      setState({ title, message, resolve })
    })
  }, [])

  const handleClose = useCallback((ok: boolean) => {
    resolveRef.current?.(ok)
    resolveRef.current = null
    setState(null)
  }, [])

  const ConfirmDialog = state ? (
    <Dialog open onClose={() => handleClose(false)} maxWidth="xs" fullWidth>
      <DialogTitle>{state.title}</DialogTitle>
      <DialogContent>
        {typeof state.message === 'string' ? (
          <DialogContentText>{state.message}</DialogContentText>
        ) : (
          state.message
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={() => handleClose(false)}>取消</Button>
        <Button variant="contained" color="error" onClick={() => handleClose(true)}>
          确认
        </Button>
      </DialogActions>
    </Dialog>
  ) : null

  return { confirm, ConfirmDialog }
}
