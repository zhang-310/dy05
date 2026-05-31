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

const AI_ANALYST_READY_ENDPOINTS = [
  '/live/ai/generate-slot',
  '/live/ai/generate-slot-sse',
  '/live/script/save',
]

const AI_ANALYST_CONTEXT_SOURCES = [
  'analyst-script-prop',
  'analyst-state-props',
  'selected-model-owned-by-parent',
]

const AI_ANALYST_UNSUPPORTED_ACTIONS = [
  'direct-network-call',
  'local-analyst-content-fallback',
  'script-mutation-in-panel',
  'shortvideo-mutation',
  'copy-library-mutation',
]

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
  const scriptId = analystScript?.id == null ? '' : String(analystScript.id)
  const scriptType = String(analystScript?.scriptType ?? '')
  const contentState = analystLoading ? 'loading' : analystContent.trim() ? 'ready' : 'empty'

  return (
    <Card
      variant="outlined"
      data-testid="ai-analyst-panel-root"
      data-contract-scope="live-ai-analyst-props-bridge"
      data-ready-endpoints={AI_ANALYST_READY_ENDPOINTS.join('|')}
      data-context-sources={AI_ANALYST_CONTEXT_SOURCES.join('|')}
      data-unsupported-actions={AI_ANALYST_UNSUPPORTED_ACTIONS.join('|')}
      data-script-id={scriptId}
      data-script-type={scriptType}
      data-content-state={contentState}
      data-analyst-loading={analystLoading ? 'true' : 'false'}
      data-no-direct-api-request="true"
      data-no-local-analyst-fallback="true"
      sx={{
        width: { xs: '100%', sm: 360 },
        maxWidth: '100%',
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
          <PsychologyIcon color="primary" />
          <Typography variant="subtitle2">AI 分析师</Typography>
        </Box>
        <Typography
          variant="caption"
          color="text.secondary"
          data-testid="ai-analyst-context-caption"
          data-contract-source="analyst-script-prop"
          sx={{ display: 'block', mb: 1 }}
        >
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
            inputProps={{
              min: 0,
              'data-testid': 'ai-analyst-duration-input',
              'data-contract-source': 'analystDuration-prop',
            }}
          />
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>需求</InputLabel>
            <Select
              data-testid="ai-analyst-requirement-select"
              data-contract-source="analystRequirement-prop"
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
            data-testid="ai-analyst-autofill-button"
            data-contract-source="/live/ai/generate-slot"
            data-endpoint-owner="useAiChat.handleAnalystAutoFill"
            data-disabled-reason={analystLoading ? 'analyst-loading' : 'ready'}
            data-no-direct-api-request="true"
          >
            AI 自动填充
          </Button>
          <Button
            size="small"
            variant="outlined"
            onClick={onApply}
            data-testid="ai-analyst-apply-button"
            data-contract-source="/live/script/save"
            data-endpoint-owner="useAiChat.handleAnalystApply"
          >
            应用
          </Button>
          <Button
            size="small"
            onClick={onClose}
            data-testid="ai-analyst-close-button"
            data-contract-source="setAnalystScript-prop"
          >
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
          inputProps={{
            'data-testid': 'ai-analyst-content-input',
            'data-contract-source': 'analystContent-prop',
            'data-no-local-analyst-fallback': 'true',
          }}
          sx={{ '& .MuiInputBase-root': { fontSize: '0.875rem' } }}
        />
        {!analystContent.trim() && (
          <Typography
            variant="caption"
            color="text.secondary"
            data-testid="ai-analyst-content-empty"
            data-contract-source="/live/ai/generate-slot"
            data-endpoint-owner="useAiChat.handleAnalystAutoFill"
            data-no-local-analyst-fallback="true"
            sx={{ display: 'block', mt: 0.75 }}
          >
            暂无 AI 分析内容
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}
