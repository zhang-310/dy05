import {
  Dialog,
  DialogTitle,
  DialogContent,
  Typography,
  Box,
  Chip,
  Divider,
} from '@mui/material'

export interface TestDetailDialogProps {
  open: boolean
  onClose: () => void
  modelName?: string
  result?: {
    ok: boolean
    message: string
    latencyMs?: number
    content?: string
    tokensUsed?: number
  } | null
}

export function TestDetailDialog({ open, onClose, modelName, result }: TestDetailDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>测试详情 - {modelName ?? '-'}</DialogTitle>
      <DialogContent>
        {!result ? (
          <Typography color="text.secondary">暂无测试结果</Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body2" fontWeight={600}>状态：</Typography>
              <Chip
                label={result.ok ? '连通正常' : '失败'}
                size="small"
                color={result.ok ? 'success' : 'error'}
              />
            </Box>
            <Box>
              <Typography variant="body2" fontWeight={600}>耗时：</Typography>
              <Typography variant="body2" color="text.secondary">
                {result.latencyMs != null ? `${result.latencyMs} ms` : '-'}
              </Typography>
            </Box>
            {result.tokensUsed != null && result.tokensUsed > 0 && (
              <Box>
                <Typography variant="body2" fontWeight={600}>Token 消耗：</Typography>
                <Typography variant="body2" color="text.secondary">{result.tokensUsed}</Typography>
              </Box>
            )}
            {result.message && (
              <Box>
                <Typography variant="body2" fontWeight={600}>消息：</Typography>
                <Typography variant="body2" color="text.secondary">{result.message}</Typography>
              </Box>
            )}
            {result.content && (
              <>
                <Divider />
                <Box>
                  <Typography variant="body2" fontWeight={600}>模型回复：</Typography>
                  <Typography
                    variant="body2"
                    sx={{
                      mt: 0.5,
                      p: 1,
                      bgcolor: 'action.hover',
                      borderRadius: 1,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {result.content}
                  </Typography>
                </Box>
              </>
            )}
          </Box>
        )}
      </DialogContent>
    </Dialog>
  )
}
