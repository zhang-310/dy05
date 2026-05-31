import { Box, Typography, Button, Paper } from '@mui/material'
import { PictureAsPdf as PdfIcon } from '@mui/icons-material'
import MarkdownViewer from '@/components/MarkdownViewer'

export interface EvolveReportViewerProps {
  /** Markdown 内容 */
  content: string
  /** 标题（可选） */
  title?: string
  /** 是否显示导出按钮 */
  showExport?: boolean
  /** 导出回调（默认使用 window.print） */
  onExport?: () => void
  /** 最大高度，超出可滚动 */
  maxHeight?: number | string
}

export function EvolveReportViewer({
  content,
  title,
  showExport = true,
  onExport,
  maxHeight = 480,
}: EvolveReportViewerProps) {
  const handleExport = () => {
    if (onExport) {
      onExport()
    } else {
      window.print()
    }
  }

  return (
    <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
      {(title != null || showExport) && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            px: 2,
            py: 1.5,
            borderBottom: 1,
            borderColor: 'divider',
          }}
        >
          {title != null && (
            <Typography variant="subtitle1" fontWeight={600}>
              {title}
            </Typography>
          )}
          <Box sx={{ flex: 1 }} />
          {showExport && (
            <Button size="small" startIcon={<PdfIcon />} onClick={handleExport}>
              导出 / 打印
            </Button>
          )}
        </Box>
      )}
      <Box
        sx={{
          px: 2,
          py: 2,
          maxHeight: typeof maxHeight === 'number' ? `${maxHeight}px` : maxHeight,
          overflow: 'auto',
        }}
      >
        <MarkdownViewer content={content} />
      </Box>
    </Paper>
  )
}
