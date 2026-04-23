import {
  Box,
  Typography,
  Chip,
  CircularProgress,
  Tabs,
  Tab,
  Collapse,
  Card,
  CardContent,
  IconButton,
  Tooltip,
  alpha,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import HistoryIcon from '@mui/icons-material/History'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import DeleteIcon from '@mui/icons-material/Delete'
import EditNoteIcon from '@mui/icons-material/EditNote'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import ArticleOutlinedIcon from '@mui/icons-material/ArticleOutlined'
import StyleOutlinedIcon from '@mui/icons-material/StyleOutlined'
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline'
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined'
import { SCRIPT_TYPE_OPTIONS } from '../script-constants'
import { estimateDurationFromText } from '@/utils/script'
import { formatTime } from './types'
import type { ScriptStyleListProps, ProductScript } from './types'

/* ── Mini Stat (header bar) ── */
function MiniStat({ icon, value, label, color = 'text.secondary' }: {
  icon: React.ReactNode; value: number; label: string; color?: string
}) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
      <Box sx={{ color: color === 'success' ? 'success.main' : 'text.secondary', display: 'flex' }}>
        {icon}
      </Box>
      <Typography variant="body2" fontWeight={600} sx={{ color: color === 'success' ? 'success.main' : 'text.primary' }}>
        {value}
      </Typography>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
    </Box>
  )
}

/* ── Script Card ── */
function ScriptCard({ script: s, isActive, isExpanded, onToggleExpand, onActivate, onDelete, onHistory, onRefine, onCopy }: {
  script: ProductScript; isActive: boolean; isExpanded: boolean
  onToggleExpand: () => void; onActivate: () => void; onDelete: () => void
  onHistory: () => void; onRefine: () => void; onCopy: () => void
}) {
  const dur = estimateDurationFromText(s.scriptContent)

  return (
    <Card
      variant="outlined"
      sx={{
        my: 0.75,
        borderColor: isActive ? 'success.main' : 'divider',
        bgcolor: isActive ? (t) => alpha(t.palette.success.main, 0.03) : 'background.paper',
        cursor: 'pointer',
        transition: 'all 0.15s',
        '&:hover': {
          borderColor: isActive ? 'success.main' : 'primary.light',
          boxShadow: 1,
        },
      }}
      onClick={onToggleExpand}
    >
      <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
        {/* top row */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 0.5 }}>
          <Typography variant="body2" fontWeight={700} sx={{ fontFamily: 'monospace' }}>
            V{s.version}
          </Typography>
          {isActive && (
            <Chip size="small" label="当前激活" color="success" sx={{ height: 20, fontSize: '0.65rem', fontWeight: 600 }} />
          )}
          {dur > 0 && (
            <Chip
              size="small" variant="outlined"
              icon={<TimerOutlinedIcon />}
              label={`${dur}s`}
              sx={{ height: 20, fontSize: '0.65rem', '& .MuiChip-icon': { fontSize: 13 } }}
            />
          )}
          <Typography variant="caption" color="text.disabled" sx={{ ml: 'auto' }}>
            {formatTime(s.createTime)}
          </Typography>
        </Box>

        {/* content */}
        <Typography
          variant="body2" color="text.secondary"
          sx={isExpanded ? { whiteSpace: 'pre-wrap', mt: 0.5 } : {
            mt: 0.5, overflow: 'hidden', textOverflow: 'ellipsis',
            display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
          }}
        >
          {s.scriptContent}
        </Typography>

        {/* actions — only show when expanded */}
        {isExpanded && (
          <Box sx={{ display: 'flex', gap: 0.5, mt: 1.5, pt: 1, borderTop: 1, borderColor: 'divider' }}>
            <Tooltip title="AI 精修"><IconButton size="small" onClick={(e) => { e.stopPropagation(); onRefine() }}><EditNoteIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="复制"><IconButton size="small" onClick={(e) => { e.stopPropagation(); onCopy() }}><ContentCopyIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="版本历史"><IconButton size="small" onClick={(e) => { e.stopPropagation(); onHistory() }}><HistoryIcon fontSize="small" /></IconButton></Tooltip>
            {!isActive && (
              <Tooltip title="设为激活"><IconButton size="small" color="success" onClick={(e) => { e.stopPropagation(); onActivate() }}><RadioButtonUncheckedIcon fontSize="small" /></IconButton></Tooltip>
            )}
            <Box sx={{ flex: 1 }} />
            <Tooltip title="删除"><IconButton size="small" color="error" onClick={(e) => { e.stopPropagation(); onDelete() }}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

/* ━━━━━━━━━━━━━━━━━━━ ScriptStyleList ━━━━━━━━━━━━━━━━━━━ */
export function ScriptStyleList({
  scriptType,
  onScriptTypeChange,
  byStyle,
  activeByStyle,
  loading,
  expandedStyles,
  onToggleStyleExpand,
  expandedScript,
  onToggleScriptExpand,
  nameMap,
  totalScripts,
  activeCount,
  styleCount,
  onActivate,
  onDelete,
  onHistory,
  onRefine,
  onCopy,
}: ScriptStyleListProps) {
  return (
    <Box sx={{ width: '55%', borderRight: 1, borderColor: 'divider', display: 'flex', flexDirection: 'column' }}>

      {/* Tab + stats row */}
      <Box sx={{ px: 2, pt: 1.5 }}>
        <Tabs
          value={scriptType}
          onChange={(_e, v: string) => onScriptTypeChange(v)}
          sx={{ minHeight: 36, mb: 1.5 }}
        >
          {SCRIPT_TYPE_OPTIONS.map((o) => (
            <Tab key={o.value} value={o.value} label={o.label} sx={{ minHeight: 36, py: 0 }} />
          ))}
        </Tabs>

        {/* stats bar */}
        {!loading && totalScripts > 0 && (
          <Box
            sx={{
              display: 'flex', gap: 2.5, mb: 1.5, px: 1.5, py: 1,
              bgcolor: (t) => alpha(t.palette.info.main, 0.04),
              borderRadius: 1.5,
            }}
          >
            <MiniStat icon={<StyleOutlinedIcon sx={{ fontSize: 16 }} />} value={styleCount} label="风格" />
            <MiniStat icon={<ArticleOutlinedIcon sx={{ fontSize: 16 }} />} value={totalScripts} label="话术" />
            <MiniStat icon={<PlayCircleOutlineIcon sx={{ fontSize: 16 }} />} value={activeCount} label="激活" color="success" />
          </Box>
        )}
      </Box>

      {/* script list */}
      <Box sx={{ flex: 1, overflow: 'auto', px: 2, pb: 2 }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : styleCount === 0 ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 8 }}>
            <ArticleOutlinedIcon sx={{ fontSize: 56, color: 'text.disabled', mb: 2 }} />
            <Typography color="text.secondary">暂无话术</Typography>
            <Typography variant="caption" color="text.disabled" sx={{ mt: 0.5 }}>
              在右侧选择风格，点击 AI 生成
            </Typography>
          </Box>
        ) : (
          Object.entries(byStyle).map(([style, scripts]) => {
            const expanded = expandedStyles[style] !== false
            const activeSc = activeByStyle[style]
            const sName = nameMap[style] ?? style

            return (
              <Box key={style} sx={{ mb: 1.5 }}>
                {/* group header */}
                <Box
                  onClick={() => onToggleStyleExpand(style)}
                  sx={{
                    display: 'flex', alignItems: 'center', cursor: 'pointer',
                    py: 0.75, px: 1.5, borderRadius: 1.5,
                    bgcolor: (t) => alpha(t.palette.text.primary, 0.02),
                    '&:hover': { bgcolor: 'action.hover' },
                    transition: 'background-color 0.15s',
                  }}
                >
                  {expanded
                    ? <ExpandLessIcon fontSize="small" sx={{ color: 'text.secondary', mr: 0.5 }} />
                    : <ExpandMoreIcon fontSize="small" sx={{ color: 'text.secondary', mr: 0.5 }} />
                  }
                  <Typography variant="subtitle2" fontWeight={600} sx={{ flex: 1 }}>
                    {sName}
                  </Typography>
                  <Chip
                    size="small"
                    label={`${scripts.length}`}
                    sx={{ height: 20, fontSize: '0.7rem', minWidth: 28 }}
                  />
                  {activeSc && (
                    <Chip
                      size="small" label="V" icon={<CheckCircleIcon />} color="success" variant="outlined"
                      sx={{ height: 20, ml: 0.5, fontSize: '0.7rem', '& .MuiChip-icon': { fontSize: 14 } }}
                    />
                  )}
                </Box>

                <Collapse in={expanded}>
                  <Box sx={{ pt: 0.5 }}>
                    {scripts.map((s) => (
                      <ScriptCard
                        key={s.id}
                        script={s}
                        isActive={activeSc?.id === s.id}
                        isExpanded={expandedScript === s.id}
                        onToggleExpand={() => onToggleScriptExpand(s.id)}
                        onActivate={() => onActivate(s)}
                        onDelete={() => onDelete(s)}
                        onHistory={() => onHistory(s)}
                        onRefine={() => onRefine(s)}
                        onCopy={() => onCopy(s)}
                      />
                    ))}
                  </Box>
                </Collapse>
              </Box>
            )
          })
        )}
      </Box>
    </Box>
  )
}
