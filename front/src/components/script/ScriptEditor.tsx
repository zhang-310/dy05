import { Box, TextField, Typography } from '@mui/material'
import { estimateDurationFromText } from '@/utils/script'

export interface ScriptEditorProps {
  value: string
  onChange: (v: string) => void
  readOnly?: boolean
  minRows?: number
  size?: 'small' | 'medium'
  showStats?: boolean
  violations?: string[]
}

/** 共享话术编辑器：支持字数、时长估算、违规高亮 */
export function ScriptEditor({
  value,
  onChange,
  readOnly = false,
  minRows = 3,
  size = 'small',
  showStats = true,
  violations = [],
}: ScriptEditorProps) {
  const charCount = (value ?? '').replace(/\s/g, '').length
  const estimatedSec = estimateDurationFromText(value)

  return (
    <Box>
      <TextField
        multiline
        fullWidth
        minRows={minRows}
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
        disabled={readOnly}
        size={size}
        placeholder="输入话术内容..."
      />
      {showStats && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
          <Typography variant="caption" color="text.secondary">
            {charCount} 字
          </Typography>
          {estimatedSec > 0 && (
            <Typography variant="caption" color="text.secondary">
              约 {estimatedSec} 秒
            </Typography>
          )}
        </Box>
      )}
      {violations.length > 0 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
          {violations.map((v, i) => (
            <Typography key={i} component="span" variant="caption" color="warning.main" sx={{ mr: 0.5 }}>
              [{v}]
            </Typography>
          ))}
        </Box>
      )}
    </Box>
  )
}
