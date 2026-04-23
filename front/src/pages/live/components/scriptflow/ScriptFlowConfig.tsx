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
  accent: string
}

export const ScriptEditCard = memo(function ScriptEditCard({
  row,
  seqNo,
  typeLabel,
  accent,
}: ScriptEditCardProps) {
  const sp = useSP()
  const id = row.id as number
  const dur = estimateDurationFromText(sp.editContent)
  const lim = typeof sp.editDurationLimit === 'number' ? sp.editDurationLimit : 0
  const violations = sp.violationResult[id]?.passed === false ? (sp.violationResult[id]?.violations ?? []) : []

  return (
    <Box sx={{ width: SCRIPT_NODE_W }} className="nopan nodrag nowheel"
      onPointerDown={stopRFCapture} onMouseDown={stopRFCapture}>
      <Handle type="target" position={Position.Left}
        style={{ background: accent, width: 8, height: 8, border: '2px solid #fff' }} />
      <Card variant="outlined" sx={{
        borderRadius: 2.5, border: '2px solid', borderColor: 'primary.main',
        bgcolor: 'background.paper', overflow: 'hidden',
        boxShadow: '0 3px 16px rgba(25,118,210,0.14)',
      }}>
        {/* Header toolbar: sequence, char count, undo/redo, compliance, save/cancel */}
        <Box sx={{
          display: 'flex', alignItems: 'center', gap: 0.75,
          px: 1, py: 0.5,
          background: 'linear-gradient(90deg, #e8f0fe 0%, #f5f7ff 100%)',
          borderBottom: '1px solid', borderColor: '#d0dff5',
        }}>
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
                <UndoIcon sx={{ fontSize: 15 }} />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="重做">
            <span>
              <IconButton size="small" onClick={sp.onRedo} disabled={!sp.canRedo} sx={{ p: '3px' }}>
                <RedoIcon sx={{ fontSize: 15 }} />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="合规检测">
            <span>
              <IconButton size="small" onClick={() => sp.onCheckViolation(id)} disabled={sp.checkingId != null} sx={{ p: '3px' }}>
                <GavelIcon sx={{ fontSize: 15 }} />
              </IconButton>
            </span>
          </Tooltip>
          <Box sx={{ width: 1, height: 16, bgcolor: '#c5d4e8', mx: 0.25 }} />
          <Tooltip title="保存">
            <IconButton size="small" color="primary" onClick={sp.onSaveEdit} sx={{ p: '3px' }}>
              <CheckIcon sx={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
          <Tooltip title="取消">
            <IconButton size="small" onClick={sp.onCancelEdit} sx={{ p: '3px' }}>
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
            inputProps={{ min: 0 }}
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
      <Handle type="source" position={Position.Right} id="to-ai"
        style={{ background: '#1976d2', width: 6, height: 6, border: '2px solid #e8f0fe' }} />
    </Box>
  )
})
