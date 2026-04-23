import { memo } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material'

export interface RegenerationDiffDialogProps {
  open: boolean
  oldContent: string
  newContent: string
  slotLabel?: string
  onAccept: () => void
  onReject: () => void
  onCancel: () => void
}

export const RegenerationDiffDialog = memo(function RegenerationDiffDialog({
  open,
  oldContent,
  newContent,
  slotLabel,
  onAccept,
  onReject,
  onCancel,
}: RegenerationDiffDialogProps) {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth="lg" fullWidth>
      <DialogTitle>
        重新生成对比{slotLabel ? ` — ${slotLabel}` : ''}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', gap: 2, minHeight: 300 }}>
          {/* 旧版本 */}
          <Box
            sx={{
              flex: 1,
              p: 2,
              border: 2,
              borderColor: 'error.light',
              borderRadius: 1,
              bgcolor: 'rgba(211, 47, 47, 0.04)',
              overflow: 'auto',
              maxHeight: 500,
            }}
          >
            <Typography variant="subtitle2" color="error.main" sx={{ mb: 1 }}>
              原版本
            </Typography>
            <Typography
              variant="body2"
              sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', lineHeight: 1.7 }}
            >
              {oldContent || '（无内容）'}
            </Typography>
          </Box>

          {/* 新版本 */}
          <Box
            sx={{
              flex: 1,
              p: 2,
              border: 2,
              borderColor: 'success.light',
              borderRadius: 1,
              bgcolor: 'rgba(46, 125, 50, 0.04)',
              overflow: 'auto',
              maxHeight: 500,
            }}
          >
            <Typography variant="subtitle2" color="success.main" sx={{ mb: 1 }}>
              新版本
            </Typography>
            <Typography
              variant="body2"
              sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', lineHeight: 1.7 }}
            >
              {newContent || '（无内容）'}
            </Typography>
          </Box>
        </Box>

        {/* 字数对比 */}
        <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
          <Typography variant="caption" color="text.secondary">
            原版：{oldContent.length} 字
          </Typography>
          <Typography variant="caption" color="text.secondary">
            新版：{newContent.length} 字
          </Typography>
          <Typography
            variant="caption"
            color={newContent.length > oldContent.length ? 'success.main' : newContent.length < oldContent.length ? 'warning.main' : 'text.secondary'}
          >
            差异：{newContent.length - oldContent.length > 0 ? '+' : ''}{newContent.length - oldContent.length} 字
          </Typography>
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onCancel} color="inherit">取消</Button>
        <Button onClick={onReject} variant="outlined">保留原版</Button>
        <Button onClick={onAccept} variant="contained" color="primary">使用新版本</Button>
      </DialogActions>
    </Dialog>
  )
})
