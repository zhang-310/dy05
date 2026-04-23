import { useCallback, useState } from 'react'
import { Box, Typography, LinearProgress, IconButton } from '@mui/material'
import { CloudUpload as UploadIcon, Delete as DeleteIcon } from '@mui/icons-material'

interface ImageUploaderProps {
  accept?: string
  multiple?: boolean
  maxFiles?: number
  onFilesSelected: (files: File[]) => void | Promise<void>
  previewUrls?: string[]
  onRemovePreview?: (index: number) => void
}

/** 图片上传：拖拽、多文件、进度 */
export function ImageUploader({
  accept = 'image/*',
  multiple = true,
  maxFiles = 10,
  onFilesSelected,
  previewUrls = [],
  onRemovePreview,
}: ImageUploaderProps) {
  const [dragging, setDragging] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)

  const handleDrop = useCallback(
    async (e: React.DragEvent) => {
      e.preventDefault()
      setDragging(false)
      const files = Array.from(e.dataTransfer.files).filter((f) => f.type.startsWith('image/'))
      if (files.length === 0) return
      const toAdd = files.slice(0, maxFiles - previewUrls.length)
      if (toAdd.length === 0) return
      setUploading(true)
      setProgress(0)
      try {
        await onFilesSelected(toAdd)
        setProgress(100)
      } finally {
        setUploading(false)
      }
    },
    [maxFiles, previewUrls.length, onFilesSelected]
  )

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
  }, [])

  const handleFileInput = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = Array.from(e.target.files || [])
      e.target.value = ''
      if (files.length === 0) return
      const toAdd = files.slice(0, maxFiles - previewUrls.length)
      if (toAdd.length === 0) return
      setUploading(true)
      setProgress(0)
      try {
        await onFilesSelected(toAdd)
        setProgress(100)
      } finally {
        setUploading(false)
      }
    },
    [maxFiles, previewUrls.length, onFilesSelected]
  )

  return (
    <Box>
      <Box
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        sx={{
          border: '2px dashed',
          borderColor: dragging ? 'primary.main' : 'divider',
          borderRadius: 2,
          p: 3,
          textAlign: 'center',
          bgcolor: dragging ? 'action.hover' : 'grey.50',
          cursor: 'pointer',
          transition: 'all 0.2s',
        }}
        onClick={() => document.getElementById('image-uploader-input')?.click()}
      >
        <input
          id="image-uploader-input"
          type="file"
          accept={accept}
          multiple={multiple}
          onChange={handleFileInput}
          style={{ display: 'none' }}
        />
        <UploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
        <Typography variant="body2" color="text.secondary">
          {dragging ? '松开以上传' : '拖拽图片到此处，或点击选择'}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block">
          支持多选，最多 {maxFiles} 张
        </Typography>
        {uploading && (
          <LinearProgress variant="determinate" value={progress} sx={{ mt: 2, height: 6, borderRadius: 1 }} />
        )}
      </Box>
      {previewUrls.length > 0 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 2 }}>
          {previewUrls.map((url, i) => (
            <Box key={i} sx={{ position: 'relative' }}>
              <Box
                component="img"
                src={url}
                alt={`预览 ${i + 1}`}
                sx={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 1 }}
              />
              {onRemovePreview && (
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation()
                    onRemovePreview(i)
                  }}
                  sx={{
                    position: 'absolute',
                    top: -8,
                    right: -8,
                    bgcolor: 'error.main',
                    color: 'white',
                    '&:hover': { bgcolor: 'error.dark' },
                  }}
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
          ))}
        </Box>
      )}
    </Box>
  )
}
