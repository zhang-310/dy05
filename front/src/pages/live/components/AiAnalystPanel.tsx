import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import PsychologyIcon from '@mui/icons-material/Psychology'
import { SCRIPT_TYPE_LABEL, REQUIREMENT_OPTIONS } from './constants'

export interface AiAnalystPanelProps {
  analystScript: Record<string, unknown>
  analystContent: string
  analystDuration: number | ''
  analystRequirement: string
  analystLoading: boolean
  onContentChange: (v: string) => void
  onDurationChange: (v: number | '') => void
  onRequirementChange: (v: string) => void
  onAutoFill: () => void
  onApply: () => void
  onClose: () => void
}

export function AiAnalystPanel({
  analystScript,
  analystContent,
  analystDuration,
  analystRequirement,
  analystLoading,
  onContentChange,
  onDurationChange,
  onRequirementChange,
  onAutoFill,
  onApply,
  onClose,
}: AiAnalystPanelProps) {
  return (
    <Card variant="outlined" sx={{ width: 360, flexShrink: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
          <PsychologyIcon color="primary" />
          <Typography variant="subtitle2">AI 分析师</Typography>
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
          #{String(analystScript.sequenceNo ?? '')} · {SCRIPT_TYPE_LABEL[analystScript.scriptType as string] ?? '-'}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, mb: 1, flexWrap: 'wrap' }}>
          <TextField
            size="small"
            type="number"
            label="时长(秒)"
            placeholder="0=不限制"
            value={analystDuration === '' ? '' : analystDuration}
            onChange={(e) => onDurationChange(e.target.value === '' ? '' : (Number(e.target.value) || 0))}
            sx={{ width: 100 }}
            inputProps={{ min: 0 }}
          />
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>需求</InputLabel>
            <Select
              value={analystRequirement ?? ''}
              label="需求"
              onChange={(e) => onRequirementChange(e.target.value)}
            >
              {REQUIREMENT_OPTIONS.map((o: { value: string; label: string }) => (
                <MenuItem key={o.value || '_'} value={o.value}>{o.label}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
        <Box sx={{ display: 'flex', gap: 0.5, mb: 1 }}>
          <Button
            size="small"
            variant="contained"
            startIcon={analystLoading ? <CircularProgress size={14} /> : <AutoAwesomeIcon />}
            onClick={onAutoFill}
            disabled={analystLoading}
          >
            AI 自动填充
          </Button>
          <Button size="small" variant="outlined" onClick={onApply}>
            应用
          </Button>
          <Button size="small" onClick={onClose}>
            关闭
          </Button>
        </Box>
        <TextField
          fullWidth
          multiline
          minRows={8}
          placeholder="设置时长、需求后点击「AI 自动填充」，生成后可在此微调"
          value={analystContent}
          onChange={(e) => onContentChange(e.target.value)}
          size="small"
          sx={{ '& .MuiInputBase-root': { fontSize: '0.875rem' } }}
        />
      </CardContent>
    </Card>
  )
}
