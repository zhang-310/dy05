import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Card,
  CardContent,
  Typography,
  IconButton,
  CircularProgress,
} from '@mui/material'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import EditIcon from '@mui/icons-material/Edit'
import { StylePreview } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'

interface StylePreviewDialogProps {
  open: boolean
  onClose: () => void
  previews: StylePreview[]
  loading: boolean
  onConfirm: () => void
  onAdjust: () => void
}

export function StylePreviewDialog({
  open,
  onClose,
  previews,
  loading,
  onConfirm,
  onAdjust,
}: StylePreviewDialogProps) {
  const toast = useToast()

  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content)
    toast('已复制到剪贴板', 'success')
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6" fontWeight={700}>
            风格预览
          </Typography>
          <Typography variant="caption" color="text.secondary">
            （15秒片段，仅供参考）
          </Typography>
        </Box>
      </DialogTitle>

      <DialogContent>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}>
            <CircularProgress />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 2 }}>
              正在生成预览...
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {previews.map((preview) => (
              <Card key={preview.style} variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                    <Typography variant="subtitle1" fontWeight={600}>
                      {preview.styleName}
                    </Typography>
                    <IconButton
                      size="small"
                      onClick={() => handleCopy(preview.content)}
                      title="复制内容"
                    >
                      <ContentCopyIcon fontSize="small" />
                    </IconButton>
                  </Box>
                  <Typography
                    variant="body2"
                    sx={{
                      whiteSpace: 'pre-wrap',
                      lineHeight: 1.8,
                      color: 'text.secondary',
                    }}
                  >
                    {preview.content}
                  </Typography>
                </CardContent>
              </Card>
            ))}
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 2, gap: 1 }}>
        <Button
          variant="outlined"
          startIcon={<EditIcon />}
          onClick={onAdjust}
          disabled={loading}
        >
          调整配置
        </Button>
        <Button
          variant="contained"
          startIcon={<CheckCircleIcon />}
          onClick={onConfirm}
          disabled={loading}
        >
          满意，开始生成
        </Button>
      </DialogActions>
    </Dialog>
  )
}
