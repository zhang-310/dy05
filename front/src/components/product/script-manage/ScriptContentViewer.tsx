import {
  Box,
  Typography,
  Button,
  Chip,
  CircularProgress,
  TextField,
  alpha,
} from '@mui/material'
import EditNoteIcon from '@mui/icons-material/EditNote'
import SendIcon from '@mui/icons-material/Send'
import type { ScriptContentViewerProps } from './types'

/* ━━━━━━━━━━━━━━━━━━━ ScriptContentViewer (AI Refine Panel) ━━━━━━━━━━━━━━━━━━━ */
export function ScriptContentViewer({
  script,
  styleName,
  prompt,
  onPromptChange,
  result,
  refining,
  onStart,
  onAccept,
  onCancel,
}: ScriptContentViewerProps) {
  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <EditNoteIcon color="primary" />
        <Typography variant="subtitle1" fontWeight={700}>AI 精修</Typography>
        <Chip size="small" label={`V${script.version} · ${styleName}`} variant="outlined" sx={{ height: 22 }} />
        <Box sx={{ flex: 1 }} />
        <Button size="small" variant="text" onClick={onCancel}>返回</Button>
      </Box>

      {/* original */}
      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ mb: 0.5, display: 'block' }}>
        原文
      </Typography>
      <Box
        sx={{
          p: 1.5, mb: 2, borderRadius: 1.5, maxHeight: 180, overflow: 'auto',
          border: 1, borderColor: 'divider', bgcolor: (t) => alpha(t.palette.grey[500], 0.03),
        }}
      >
        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
          {script.scriptContent}
        </Typography>
      </Box>

      {/* prompt */}
      <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ mb: 0.5, display: 'block' }}>
        修改指令
      </Typography>
      <TextField
        fullWidth size="small" multiline minRows={2} maxRows={4}
        placeholder="如：改得更幽默、增加痛点、缩短到30秒..."
        value={prompt} onChange={(e) => onPromptChange(e.target.value)} disabled={refining}
        sx={{ mb: 1.5, '& .MuiOutlinedInput-root': { borderRadius: 1.5 } }}
        onKeyDown={(e) => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) { e.preventDefault(); onStart() } }}
      />

      {!result && !refining && (
        <Button
          variant="contained" fullWidth startIcon={<SendIcon />}
          onClick={onStart} disabled={!prompt.trim()} sx={{ borderRadius: 2 }}
        >
          发送（Ctrl+Enter）
        </Button>
      )}

      {/* result */}
      {(result || refining) && (
        <>
          <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ mb: 0.5, display: 'block', mt: 2 }}>
            修改结果 {refining && <CircularProgress size={12} sx={{ ml: 0.5, verticalAlign: 'middle' }} />}
          </Typography>
          <Box
            sx={{
              p: 1.5, mb: 2, borderRadius: 1.5, maxHeight: 260, overflow: 'auto',
              border: 1, borderColor: 'primary.200',
              bgcolor: (t) => alpha(t.palette.primary.main, 0.02),
            }}
          >
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
              {result || (refining ? '生成中...' : '')}
            </Typography>
          </Box>

          {!refining && result && (
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button variant="contained" onClick={onAccept} sx={{ flex: 1, borderRadius: 2 }}>应用修改</Button>
              <Button variant="outlined" onClick={onCancel} sx={{ flex: 1, borderRadius: 2 }}>放弃</Button>
            </Box>
          )}
          {refining && (
            <Button variant="outlined" color="warning" onClick={onCancel} fullWidth sx={{ borderRadius: 2 }}>取消</Button>
          )}
        </>
      )}
    </Box>
  )
}
