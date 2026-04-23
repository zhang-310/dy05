/**
 * AI 剧本生成与应用到剧集
 */
import { Box, Typography, Button, Alert, TextField } from '@mui/material'
import { AutoStories as ScriptIcon, CheckCircle as ApplyIcon, ArrowForward as NextIcon } from '@mui/icons-material'

export interface ScriptGeneratorProps {
  scriptTheme: string
  scriptStyle: string
  onThemeChange: (v: string) => void
  onStyleChange: (v: string) => void
  onGenerate: () => void
  generating: boolean
  loading: boolean
  scriptResult: string | null
  applySuccess: number | null
  onApply: () => void
  onNextStep: () => void
  onApplySuccessClose: () => void
}

export function ScriptGenerator({
  scriptTheme,
  scriptStyle,
  onThemeChange,
  onStyleChange,
  onGenerate,
  generating,
  loading,
  scriptResult,
  applySuccess,
  onApply,
  onNextStep,
  onApplySuccessClose,
}: ScriptGeneratorProps) {
  return (
    <Box>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2, alignItems: 'center' }}>
        <TextField
          size="small"
          label="题材"
          value={scriptTheme}
          onChange={(e) => onThemeChange(e.target.value)}
          placeholder="可选"
          sx={{ width: 100 }}
        />
        <TextField
          size="small"
          label="风格"
          value={scriptStyle}
          onChange={(e) => onStyleChange(e.target.value)}
          placeholder="可选"
          sx={{ width: 100 }}
        />
        <Button
          size="small"
          variant="contained"
          startIcon={<ScriptIcon />}
          onClick={onGenerate}
          disabled={loading}
        >
          {generating ? '生成中…' : 'AI 生成剧本'}
        </Button>
      </Box>
      {scriptResult && (
        <Box sx={{ mt: 2, p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
          <Typography variant="subtitle2" gutterBottom>
            AI 剧本结果
          </Typography>
          <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap' }}>
            {scriptResult}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mt: 2, flexWrap: 'wrap' }}>
            <Button variant="contained" startIcon={<ApplyIcon />} onClick={onApply} disabled={loading}>
              应用到剧集
            </Button>
            <Button variant="outlined" startIcon={<NextIcon />} onClick={onNextStep}>
              下一步：前往创作工作台
            </Button>
          </Box>
          {applySuccess != null && applySuccess > 0 && (
            <Alert severity="success" sx={{ mt: 2 }} onClose={onApplySuccessClose}>
              已成功应用到 {applySuccess} 集。可点击任意剧集的「开始制作」进入分镜设计，或点击「已关联项目」跳转已有项目。
            </Alert>
          )}
        </Box>
      )}
    </Box>
  )
}
