import { useState } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Box, Divider, Chip, Alert,
} from '@mui/material'
import { Warning as WarningIcon, CompareArrows as DiffIcon } from '@mui/icons-material'

interface ConflictDialogProps {
  open: boolean
  onClose: () => void
  localContent: string
  serverContent: string
  onOverwrite: () => void
  onDiscard: () => void
  lastEditor?: string
}

export function ConflictDialog({
  open, onClose, localContent, serverContent, onOverwrite, onDiscard, lastEditor,
}: ConflictDialogProps) {
  const [showDiff, setShowDiff] = useState(false)

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningIcon color="warning" />
        编辑冲突
      </DialogTitle>
      <DialogContent>
        <Alert severity="warning" sx={{ mb: 2 }}>
          {lastEditor ? `协作者「${lastEditor}」已修改此话术` : '其他协作者已修改此话术'}，
          请选择处理方式。
        </Alert>

        {showDiff && (
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, mt: 2 }}>
            <Box>
              <Chip label="我的版本" color="primary" size="small" sx={{ mb: 1 }} />
              <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1, maxHeight: 300, overflow: 'auto', fontSize: '0.875rem', whiteSpace: 'pre-wrap' }}>
                {localContent || '（空）'}
              </Box>
            </Box>
            <Box>
              <Chip label="服务器版本" color="secondary" size="small" sx={{ mb: 1 }} />
              <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1, maxHeight: 300, overflow: 'auto', fontSize: '0.875rem', whiteSpace: 'pre-wrap' }}>
                {serverContent || '（空）'}
              </Box>
            </Box>
          </Box>
        )}
      </DialogContent>
      <Divider />
      <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
        <Button startIcon={<DiffIcon />} onClick={() => setShowDiff(!showDiff)} color="info">
          {showDiff ? '隐藏差异' : '查看差异'}
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button onClick={onDiscard} color="inherit">放弃我的修改</Button>
        <Button onClick={onOverwrite} variant="contained" color="warning">覆盖保存</Button>
      </DialogActions>
    </Dialog>
  )
}
