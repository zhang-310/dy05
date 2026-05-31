import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import {
  Box,
  Typography,
  Card,
  IconButton,
  Tooltip,
  TextField,
  LinearProgress,
} from '@mui/material'
import { alpha, useTheme } from '@mui/material/styles'
import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import UndoIcon from '@mui/icons-material/Undo'
import RedoIcon from '@mui/icons-material/Redo'
import GavelIcon from '@mui/icons-material/Gavel'
import { estimateDurationFromText } from '@/utils/script'
import { REQUIREMENT_OPTIONS } from '../constants'
import { useSP, stopRFCapture, SCRIPT_NODE_W } from './ScriptFlowSteps'

/* ══════════════════════════════════════════════
   ScriptEditCard — inline editing form for a script node
   Extracted from ScriptBranchNode editing mode
   ══════════════════════════════════════════════ */
export interface ScriptEditCardProps {
  row: Record<string, unknown>
  seqNo: string
  typeLabel: string
  accent?: string
  accentTone?: 'primary' | 'success' | 'warning' | 'error' | 'secondary'
}

export const ScriptEditCard = memo(function ScriptEditCard({
  row,
  seqNo,
  typeLabel,
  accent,
  accentTone = 'primary',
}: ScriptEditCardProps) {
  const theme = useTheme()
  const sp = useSP()
  const id = row.id as number
  const dur = estimateDurationFromText(sp.editContent)
  const lim = typeof sp.editDurationLimit === 'number' ? sp.editDurationLimit : 0
  const violations = sp.violationResult[id]?.passed === false ? (sp.violationResult[id]?.violations ?? []) : []
  const accentColor = accent && !accent.startsWith('var(') ? accent : theme.palette[accentTone].main
  const handleBorderColor = alpha(accentColor, theme.palette.mode === 'dark' ? 0.3 : 0.14)

  return (
    <Box sx={{ width: SCRIPT_NODE_W }} className="nopan nodrag nowheel"
      data-testid="script-flow-edit-card-root"
      data-contract-scope="live-script-flow-edit-card-context-editor"
      data-contract-source="SectionPropsProvider-context|ScriptEditCard-props"
      data-ready-endpoints="/live/script/save|/live/ai/check-violation|/live/ai/refine-script"
      data-unsupported-actions="direct-api-request|local-script-fallback|mock-violation-result"
      data-script-id={id}
      data-sequence-no={seqNo}
      data-type-label={typeLabel}
      data-edit-length={sp.editContent.length}
      data-duration-limit={String(lim)}
      data-violation-count={violations.length}
      data-no-direct-api="true"
      data-no-local-script-fallback="true"
      onPointerDown={stopRFCapture} onMouseDown={stopRFCapture}>
      <Handle type="target" position={Position.Left}
        style={{ background: accentColor, width: 8, height: 8, border: `2px solid ${handleBorderColor}` }} />
      <Card
        variant="outlined"
        data-testid="script-flow-edit-card-surface"
        data-shadow-tone="primary"
        sx={(theme) => ({
          borderRadius: 2.5,
          border: '2px solid',
          borderColor: 'primary.main',
          bgcolor: 'background.paper',
          overflow: 'hidden',
          boxShadow: `0 3px 16px ${alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.22 : 0.14)}`,
        })}
      >
        {/* Header toolbar: sequence, char count, undo/redo, compliance, save/cancel */}
        <Box
          data-testid="script-flow-edit-header-surface"
          sx={(theme) => ({
          display: 'flex', alignItems: 'center', gap: 0.75,
          px: 1, py: 0.5,
          background: theme.palette.mode === 'dark'
            ? `linear-gradient(90deg, ${alpha(theme.palette.primary.main, 0.22)} 0%, ${alpha(theme.palette.primary.main, 0.1)} 100%)`
            : `linear-gradient(90deg, ${alpha(theme.palette.primary.main, 0.12)} 0%, ${alpha(theme.palette.primary.main, 0.05)} 100%)`,
          borderBottom: '1px solid',
          borderColor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.35 : 0.18),
        })}
        >
          <Typography sx={{ fontSize: 13, fontWeight: 700, color: 'primary.main', flexShrink: 0 }}>
            #{seqNo} {typeLabel}
          </Typography>
          <Typography sx={{ fontSize: 12, color: 'text.secondary', flexShrink: 0 }}>
            {sp.editContent.length}字 ~{dur}s{lim > 0 ? ` / ${lim}s` : ''}
          </Typography>
          <Box sx={{ flex: 1 }} />
          <Tooltip title="撤销">
            <span>
              <IconButton size="small" onClick={sp.onUndo} disabled={!sp.canUndo} sx={{ p: '3px' }}>
                <Box component="span" data-testid="script-flow-edit-undo-button" data-contract-source="onUndo-prop" data-disabled-reason={!sp.canUndo ? 'no-undo' : 'ready'} sx={{ display: 'contents' }}>
                <UndoIcon sx={{ fontSize: 15 }} />
                </Box>
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="重做">
            <span>
              <IconButton size="small" onClick={sp.onRedo} disabled={!sp.canRedo} sx={{ p: '3px' }}>
                <Box component="span" data-testid="script-flow-edit-redo-button" data-contract-source="onRedo-prop" data-disabled-reason={!sp.canRedo ? 'no-redo' : 'ready'} sx={{ display: 'contents' }}>
                <RedoIcon sx={{ fontSize: 15 }} />
                </Box>
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="合规检测">
            <span>
              <IconButton size="small" onClick={() => sp.onCheckViolation(id)} disabled={sp.checkingId != null} sx={{ p: '3px' }}>
                <Box component="span" data-testid="script-flow-edit-check-button" data-contract-source="/live/ai/check-violation" data-action-owner="onCheckViolation-prop" data-disabled-reason={sp.checkingId != null ? 'checking' : 'ready'} sx={{ display: 'contents' }}>
                <GavelIcon sx={{ fontSize: 15 }} />
                </Box>
              </IconButton>
            </span>
          </Tooltip>
          <Box
            data-testid="script-flow-edit-toolbar-divider-surface"
            data-divider-tone="primary"
            sx={(theme) => ({
              width: 1,
              height: 16,
              bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.32 : 0.18),
              mx: 0.25,
            })}
          />
          <Tooltip title="保存">
            <IconButton data-testid="script-flow-edit-save-button" data-contract-source="/live/script/save" data-action-owner="onSaveEdit-prop" size="small" color="primary" onClick={sp.onSaveEdit} sx={{ p: '3px' }}>
              <CheckIcon sx={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
          <Tooltip title="取消">
            <IconButton data-testid="script-flow-edit-cancel-button" data-contract-source="onCancelEdit-prop" size="small" onClick={sp.onCancelEdit} sx={{ p: '3px' }}>
              <CloseIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
            </IconButton>
          </Tooltip>
        </Box>

        {/* Content editor */}
        <TextField
          multiline fullWidth autoFocus
          minRows={3} maxRows={8}
          value={sp.editContent}
          onChange={e => sp.onEditContentChange(e.target.value)}
          size="small"
          placeholder="输入话术内容..."
          inputProps={{
            'data-testid': 'script-flow-edit-content-input',
            'data-contract-source': 'editContent-context',
          }}
          sx={{
            '& .MuiOutlinedInput-root': {
              fontSize: '0.88rem', lineHeight: 1.7, p: '8px 10px',
              '& fieldset': { border: 'none' },
            },
          }}
        />

        {/* Footer: duration progress, duration limit input, requirement select */}
        <Box sx={{
          display: 'flex', alignItems: 'center', gap: 0.75,
          px: 1, py: 0.5,
          borderTop: '1px solid', borderColor: 'divider',
        }}>
          {lim > 0 && dur > 0 && (
            <LinearProgress variant="determinate" sx={{ flex: 1, height: 3, borderRadius: 1.5, maxWidth: 90 }}
              value={Math.min((dur / lim) * 100, 100)}
              color={dur > lim ? 'error' : dur > lim * 0.8 ? 'warning' : 'primary'} />
          )}
          <Box sx={{ flex: 1 }} />
          <TextField size="small" type="number" placeholder="限时"
            value={sp.editDurationLimit === '' ? '' : sp.editDurationLimit}
            onChange={e => sp.onEditDurationLimitChange?.(e.target.value === '' ? '' : (Number(e.target.value) || 0))}
            inputProps={{ min: 0, 'data-testid': 'script-flow-edit-duration-limit-input', 'data-contract-source': 'onEditDurationLimitChange-prop' }}
            sx={{
              width: 56,
              '& .MuiInputBase-root': { height: 24, fontSize: 12 },
              '& .MuiInputBase-input': { py: 0, px: '4px' },
            }}
          />
          <Box
            component="select"
            value={sp.editRequirement ?? ''}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => sp.onEditRequirementChange?.(e.target.value)}
            data-testid="script-flow-edit-requirement-select"
            data-contract-source="onEditRequirementChange-prop"
            sx={{
              height: 24, fontSize: 12,
              border: '1px solid', borderColor: 'divider', borderRadius: 0.5,
              bgcolor: 'background.paper', px: 0.75,
              outline: 'none', color: 'text.secondary',
            }}
          >
            {REQUIREMENT_OPTIONS.map(o => <option key={o.value || '_'} value={o.value}>{o.label || '需求'}</option>)}
          </Box>
        </Box>

        {/* Violation warnings */}
        {violations.length > 0 && (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.375, px: 1, pb: 0.5 }}>
            {violations.map((v, i) => (
              <Typography key={i} component="span" sx={{ fontSize: 11, color: 'warning.main' }}>[{v}]</Typography>
            ))}
          </Box>
        )}
      </Card>

      {/* Right handle for AI connection */}
      <Box
        data-testid="script-flow-edit-ai-handle-surface"
        data-handle-color={accentColor}
        data-handle-border-color={handleBorderColor}
      >
        <Handle type="source" position={Position.Right} id="to-ai"
          style={{ background: accentColor, width: 6, height: 6, border: `2px solid ${handleBorderColor}` }} />
      </Box>
    </Box>
  )
})
