import {
  Box,
  Typography,
  Card,
  Button,
  IconButton,
  Tooltip,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Collapse,
  Paper,
  Checkbox,
} from '@mui/material'
import { ScriptEditor } from '@/components/script/ScriptEditor'
import { estimateDurationFromText } from '@/utils/script'
import EditIcon from '@mui/icons-material/Edit'
import CheckIcon from '@mui/icons-material/Check'
import GavelIcon from '@mui/icons-material/Gavel'
import PsychologyIcon from '@mui/icons-material/Psychology'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import SaveIcon from '@mui/icons-material/Save'
import DeleteIcon from '@mui/icons-material/Delete'
import PlayCircleIcon from '@mui/icons-material/PlayCircle'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import { SCRIPT_TYPE_LABEL, REQUIREMENT_OPTIONS } from './constants'

export interface ScriptSectionProps {
  title: string
  subtitle?: string
  scripts: Record<string, unknown>[]
  expanded: boolean
  onToggle: () => void
  editingId: number | null
  editContent: string
  violationResult: Record<number, { passed: boolean; violations?: string[] }>
  checkingId: number | null
  saveLibLoading: boolean
  onEdit: (id: number, content: string, row?: Record<string, unknown>) => void
  onSaveEdit: () => void
  onCancelEdit: () => void
  onEditContentChange: (v: string) => void
  onCheckViolation: (id: number) => void
  onSaveToLibrary: (id?: number) => void
  onMarkExecuted: (id: number, executed: number) => void
  onDelete: (row: Record<string, unknown>) => void
  onRefineOpen: (v: { scriptId: number } | null) => void
  onOpenAnalyst?: (row: Record<string, unknown>) => void
  editDurationLimit?: number | ''
  editRequirement?: string
  onEditDurationLimitChange?: (v: number | '') => void
  onEditRequirementChange?: (v: string) => void
  analystScriptId?: number | null
  selectedScriptIds?: Set<number>
  onToggleSelect?: (id: number) => void
  // Undo/redo support
  onUndo?: () => void
  onRedo?: () => void
  canUndo?: boolean
  canRedo?: boolean
  // Focus/regen
  focusedScriptId?: number | null
  onFocusScript?: (id: number) => void
  onRegenerateSingle?: (id: number) => void
}

export function ScriptSection({
  title,
  subtitle,
  scripts,
  expanded,
  onToggle,
  editingId,
  editContent,
  violationResult,
  checkingId,
  saveLibLoading,
  onEdit,
  onSaveEdit,
  onCancelEdit,
  onEditContentChange,
  onCheckViolation,
  onSaveToLibrary,
  onMarkExecuted,
  onDelete,
  onRefineOpen,
  onOpenAnalyst,
  editDurationLimit = '',
  editRequirement = '',
  onEditDurationLimitChange,
  onEditRequirementChange,
  analystScriptId,
  selectedScriptIds = new Set(),
  onToggleSelect,
}: ScriptSectionProps) {
  return (
    <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 1.5,
          py: 1,
          bgcolor: 'grey.50',
          cursor: 'pointer',
        }}
        onClick={onToggle}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {expanded ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
          <Typography variant="subtitle2">{title}</Typography>
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        <Typography variant="caption" color="text.secondary">
          {scripts.length} 条
        </Typography>
      </Box>
      <Collapse in={expanded}>
        <Box sx={{ p: 1.5, pt: 0 }}>
          {scripts.map((row, idx) => {
            const id = row.id as number
            const isEditing = editingId === id
            const content = isEditing ? editContent : (row.scriptContent as string) ?? '-'
            const vr = violationResult[id]
            return (
              <Card key={String(id)} variant="outlined" sx={{ p: 1.5, mb: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1, flexWrap: 'wrap', gap: 0.5 }}>
                  {!isEditing && onToggleSelect && (
                    <Checkbox
                      size="small"
                      checked={selectedScriptIds.has(id)}
                      onChange={() => onToggleSelect(id)}
                      sx={{ p: 0, mr: 0.5 }}
                    />
                  )}
                  <Typography variant="caption" color="text.secondary" component="span" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap', flex: 1 }}>
                    <span>#{String(row.sequenceNo ?? idx + 1)} · {SCRIPT_TYPE_LABEL[row.scriptType as string] ?? String(row.scriptType ?? '-')}</span>
                    {row.requirement ? <Chip label={String(row.requirement)} size="small" variant="outlined" sx={{ height: 20 }} /> : null}
                    {row.style ? <span>· {String(row.style)}</span> : null}
                    <span>· {row.executed ? '已执行' : '未执行'}</span>
                    {(() => {
                      const sec = Number(row.estimatedDurationSeconds ?? 0) > 0 ? Number(row.estimatedDurationSeconds) : estimateDurationFromText(row.scriptContent as string)
                      return sec > 0 ? <span>· 约 {sec}s</span> : null
                    })()}
                    {Number(row.durationLimitSec ?? 0) > 0 ? <span>· 限制 {Number(row.durationLimitSec)}s</span> : null}
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    {isEditing ? (
                      <>
                        <Button size="small" startIcon={<CheckIcon />} onClick={onSaveEdit}>
                          保存
                        </Button>
                        <Button size="small" onClick={onCancelEdit}>
                          取消
                        </Button>
                      </>
                    ) : (
                      <>
                        <Tooltip title="编辑">
                          <IconButton size="small" onClick={() => onEdit(id, (row.scriptContent as string) ?? '', row)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title={row.executed ? '已执行' : '标记已执行'}>
                          <IconButton
                            size="small"
                            onClick={() => onMarkExecuted(id, (row.executed as number) ?? 0)}
                            color={row.executed ? 'success' : 'default'}
                          >
                            {row.executed ? <CheckCircleIcon fontSize="small" /> : <PlayCircleIcon fontSize="small" />}
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="AI 分析师（自动填充+微调）">
                          <IconButton
                            size="small"
                            onClick={() => onOpenAnalyst?.(row)}
                            color={analystScriptId === id ? 'primary' : 'default'}
                          >
                            <PsychologyIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="AI 修改（提问式）">
                          <IconButton size="small" onClick={() => onRefineOpen({ scriptId: id })}>
                            <AutoAwesomeIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="违规检测">
                          <IconButton size="small" onClick={() => onCheckViolation(id)} disabled={checkingId != null}>
                            <GavelIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="保存到话术库">
                          <IconButton size="small" onClick={() => onSaveToLibrary(id)} disabled={saveLibLoading}>
                            <SaveIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="删除">
                          <IconButton size="small" color="error" onClick={() => onDelete(row)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                  </Box>
                </Box>
                {isEditing ? (
                  <Box>
                    <Box sx={{ display: 'flex', gap: 2, mb: 1.5, flexWrap: 'wrap' }}>
                      <TextField
                        size="small"
                        type="number"
                        label="时长限制(秒)"
                        placeholder="0=不限制"
                        value={editDurationLimit === '' ? '' : editDurationLimit}
                        onChange={(e) => {
                          const v = e.target.value
                          onEditDurationLimitChange?.(v === '' ? '' : (Number(v) || 0))
                        }}
                        sx={{ width: 120 }}
                        inputProps={{ min: 0 }}
                      />
                      <FormControl size="small" sx={{ minWidth: 140 }}>
                        <InputLabel>需求</InputLabel>
                        <Select
                          value={editRequirement ?? ''}
                          label="需求"
                          onChange={(e) => onEditRequirementChange?.(e.target.value)}
                        >
                          {REQUIREMENT_OPTIONS.map((o) => (
                            <MenuItem key={o.value || '_'} value={o.value}>{o.label}</MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    <ScriptEditor value={editContent} onChange={onEditContentChange} showStats minRows={3} violations={violationResult[id]?.passed === false ? (violationResult[id]?.violations ?? []) : []} />
                  </Box>
                ) : (
                  <Typography sx={{ whiteSpace: 'pre-wrap', fontSize: '0.875rem' }}>{content}</Typography>
                )}
                {vr && !isEditing && (
                  <Box sx={{ mt: 1 }}>
                    {vr.passed ? (
                      <Chip label="无违规" size="small" color="success" variant="outlined" />
                    ) : (
                      <Box>
                        <Chip label={`${vr.violations?.length ?? 0} 处违规`} size="small" color="warning" variant="outlined" sx={{ mr: 1 }} />
                        {vr.violations?.map((v, i) => (
                          <Chip key={i} label={v} size="small" sx={{ mr: 0.5, mt: 0.5 }} />
                        ))}
                      </Box>
                    )}
                  </Box>
                )}
              </Card>
            )
          })}
        </Box>
      </Collapse>
    </Paper>
  )
}
