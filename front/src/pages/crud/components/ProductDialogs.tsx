/**
 * ProductDialogs — image-preview dialog extracted from ProductPage.
 */
import { Box, Dialog, DialogContent } from '@mui/material'

/* ───── Image Preview Dialog ───── */

export interface ImagePreviewDialogProps {
  imageUrl: string | null
  onClose: () => void
}

export function ImagePreviewDialog({ imageUrl, onClose }: ImagePreviewDialogProps) {
  return (
    <Dialog open={!!imageUrl} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogContent sx={{ p: 0, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        {imageUrl && (
          <Box
            component="img"
            src={imageUrl + '@!300X250'}
            alt="商品大图"
            sx={{ maxWidth: '100%', maxHeight: '80vh', objectFit: 'contain' }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}
