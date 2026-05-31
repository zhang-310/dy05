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
import { alpha } from '@mui/material/styles'

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
  const lengthDelta = newContent.length - oldContent.length
  return (
    <Dialog
      open={open}
      onClose={onCancel}
      maxWidth="lg"
      fullWidth
      data-testid="regeneration-diff-dialog"
      data-contract-scope="live-regeneration-diff-props-review"
      data-ready-sources="oldContent-prop|newContent-prop|onAccept-prop|onReject-prop|onCancel-prop"
      data-no-direct-api-request="true"
      data-old-length={oldContent.length}
      data-new-length={newContent.length}
      data-length-delta={lengthDelta}
      data-slot-label={slotLabel ?? ''}
    >
      <DialogTitle data-testid="regeneration-diff-title" data-contract-source="oldContent-prop|newContent-prop">
        重新生成对比{slotLabel ? ` — ${slotLabel}` : ''}
      </DialogTitle>
      <DialogContent data-testid="regeneration-diff-content" data-contract-source="oldContent-prop|newContent-prop">
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 2, minHeight: { xs: 'auto', md: 300 } }}>
          {/* 旧版本 */}
          <Box
            data-testid="regeneration-diff-old-surface"
            data-contract-source="oldContent-prop"
            data-diff-tone="error"
            data-content-empty={oldContent ? 'false' : 'true'}
            sx={(theme) => ({
              flex: 1,
              p: 2,
              border: 2,
              borderColor: alpha(theme.palette.error[theme.palette.mode === 'dark' ? 'light' : 'main'], 0.56),
              borderRadius: 1,
              bgcolor: alpha(theme.palette.error.main, theme.palette.mode === 'dark' ? 0.16 : 0.06),
              overflow: 'auto',
              maxHeight: 500,
            })}
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
            data-testid="regeneration-diff-new-surface"
            data-contract-source="newContent-prop"
            data-diff-tone="success"
            data-content-empty={newContent ? 'false' : 'true'}
            sx={(theme) => ({
              flex: 1,
              p: 2,
              border: 2,
              borderColor: alpha(theme.palette.success[theme.palette.mode === 'dark' ? 'light' : 'main'], 0.56),
              borderRadius: 1,
              bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.16 : 0.06),
              overflow: 'auto',
              maxHeight: 500,
            })}
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
        <Box
          sx={{ display: 'flex', gap: 2, mt: 2 }}
          data-testid="regeneration-diff-summary"
          data-contract-source="oldContent-prop|newContent-prop"
          data-old-length={oldContent.length}
          data-new-length={newContent.length}
          data-length-delta={lengthDelta}
        >
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
            差异：{lengthDelta > 0 ? '+' : ''}{lengthDelta} 字
          </Typography>
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onCancel} color="inherit" data-testid="regeneration-diff-cancel-button" data-contract-source="onCancel-prop">取消</Button>
        <Button onClick={onReject} variant="outlined" data-testid="regeneration-diff-reject-button" data-contract-source="onReject-prop">保留原版</Button>
        <Button onClick={onAccept} variant="contained" color="primary" data-testid="regeneration-diff-accept-button" data-contract-source="onAccept-prop">使用新版本</Button>
      </DialogActions>
    </Dialog>
  )
})
