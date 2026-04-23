import { useCallback } from 'react'
import { Box, Typography } from '@mui/material'
import { CloudUpload as CloudUploadIcon } from '@mui/icons-material'
import type { ReactNode } from 'react'

interface UploadZoneProps {
  onDrop: (files: File[]) => void
  accept?: string
  maxFiles?: number
  disabled?: boolean
  children?: ReactNode
}

export function UploadZone({
  onDrop,
  accept,
  maxFiles = 10,
  disabled = false,
  children,
}: UploadZoneProps) {
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      if (disabled) return
      const files = Array.from(e.dataTransfer.files).slice(0, maxFiles)
      if (files.length) onDrop(files)
    },
    [disabled, maxFiles, onDrop],
  )

  const handleClick = () => {
    if (disabled) return
    const input = document.createElement('input')
    input.type = 'file'
    input.multiple = maxFiles > 1
    if (accept) input.accept = accept
    input.onchange = () => {
      const files = input.files ? Array.from(input.files) : []
      if (files.length) onDrop(files)
    }
    input.click()
  }

  return (
    <Box
      onClick={handleClick}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      sx={{
        border: '2px dashed',
        borderColor: disabled ? 'action.disabled' : 'divider',
        borderRadius: 2,
        p: 4,
        textAlign: 'center',
        cursor: disabled ? 'not-allowed' : 'pointer',
        bgcolor: disabled ? 'action.hover' : 'action.selected',
        '&:hover': disabled ? {} : { borderColor: 'primary.main', bgcolor: 'action.hover' },
      }}
    >
      {children ?? (
        <>
          <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
          <Typography color="text.secondary">拖拽文件到此处，或点击选择</Typography>
          {accept && (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
              支持: {accept}
            </Typography>
          )}
        </>
      )}
    </Box>
  )
}
