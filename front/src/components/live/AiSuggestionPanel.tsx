import { useState, useCallback } from 'react'
import {
  Card, CardContent, CardHeader, Typography, Box, Chip, IconButton,
  List, ListItem, Tooltip, Collapse,
  LinearProgress,
} from '@mui/material'
import {
  Psychology as AiIcon, ShoppingCart as BuyIcon, HelpOutline as QuestionIcon,
  ThumbDown as NegativeIcon, Celebration as ActiveIcon, ExpandMore, ExpandLess,
  ContentCopy as CopyIcon, Check as CheckIcon,
} from '@mui/icons-material'

interface DanmakuIntent {
  type: 'purchase' | 'doubt' | 'urge' | 'negative' | 'active'
  count: number
  samples: string[]
}

interface AiSuggestion {
  intent: DanmakuIntent
  suggestion: string
  timestamp: number
}

interface AiSuggestionPanelProps {
  sessionId: number
  suggestions?: AiSuggestion[]
  isLive?: boolean
}

const INTENT_CONFIG: Record<string, { label: string; color: 'error' | 'warning' | 'info' | 'success' | 'primary'; icon: typeof AiIcon }> = {
  purchase: { label: '购买意向', color: 'success', icon: BuyIcon },
  doubt: { label: '质疑/对比', color: 'warning', icon: QuestionIcon },
  urge: { label: '催促/期待', color: 'info', icon: ActiveIcon },
  negative: { label: '负面/投诉', color: 'error', icon: NegativeIcon },
  active: { label: '互动/活跃', color: 'primary', icon: ActiveIcon },
}

export function AiSuggestionPanel({ sessionId: _sessionId, suggestions = [], isLive = false }: AiSuggestionPanelProps) {
  const [expanded, setExpanded] = useState(true)
  const [copiedIdx, setCopiedIdx] = useState<number | null>(null)

  const handleCopy = useCallback((text: string, idx: number) => {
    navigator.clipboard.writeText(text)
    setCopiedIdx(idx)
    setTimeout(() => setCopiedIdx(null), 2000)
  }, [])

  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardHeader
        avatar={<AiIcon color="primary" />}
        title="AI 实时建议"
        subheader={isLive ? '基于弹幕分析' : '离线模式'}
        action={
          <IconButton onClick={() => setExpanded(!expanded)} size="small">
            {expanded ? <ExpandLess /> : <ExpandMore />}
          </IconButton>
        }
        sx={{ pb: 0 }}
      />
      <Collapse in={expanded}>
        <CardContent sx={{ pt: 1 }}>
          {suggestions.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {isLive ? '等待弹幕数据...' : '暂无建议'}
              </Typography>
              {isLive && <LinearProgress sx={{ mt: 2 }} />}
            </Box>
          ) : (
            <List dense disablePadding>
              {suggestions.map((s, idx) => {
                const config = INTENT_CONFIG[s.intent.type] || INTENT_CONFIG.active
                const IntentIcon = config.icon
                return (
                  <ListItem
                    key={idx}
                    sx={{ flexDirection: 'column', alignItems: 'flex-start', py: 1, borderBottom: '1px solid', borderColor: 'divider' }}
                    secondaryAction={
                      <Tooltip title={copiedIdx === idx ? '已复制' : '复制建议'}>
                        <IconButton edge="end" size="small" onClick={() => handleCopy(s.suggestion, idx)}>
                          {copiedIdx === idx ? <CheckIcon fontSize="small" color="success" /> : <CopyIcon fontSize="small" />}
                        </IconButton>
                      </Tooltip>
                    }
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                      <Chip icon={<IntentIcon />} label={`${config.label} (${s.intent.count}条)`} size="small" color={config.color} variant="outlined" />
                    </Box>
                    <Typography variant="body2" sx={{ pr: 4 }}>{s.suggestion}</Typography>
                    {s.intent.samples.length > 0 && (
                      <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                        弹幕样本: {s.intent.samples.slice(0, 3).join(' | ')}
                      </Typography>
                    )}
                  </ListItem>
                )
              })}
            </List>
          )}
        </CardContent>
      </Collapse>
    </Card>
  )
}
