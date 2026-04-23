import { useState, useCallback } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Collapse,
  Divider,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import StarIcon from '@mui/icons-material/Star'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { recommendScripts, type ScriptRecommendVO, type ScriptRecommendRequest } from '@/api/live-ai'
import { useToast } from '@/contexts/ToastContext'

const SCRIPT_TYPE_LABEL: Record<string, string> = {
  opening: '开场',
  product: '产品',
  transition: '过渡',
  closing: '结尾',
  interaction: '互动',
  welfare: '福利',
  closing_deal: '逼单',
  hold_back: '憋单',
  emotional: '情绪',
  chat: '聊天',
}

export interface ScriptRecommendPanelProps {
  sessionId?: number
  productId?: number
  currentSlot?: number
  viewerCount?: number
  timeElapsedSec?: number
  onApply?: (content: string) => void
}

/**
 * P1-2: 多场景话术智能推荐面板
 */
export function ScriptRecommendPanel({
  sessionId,
  productId,
  currentSlot,
  viewerCount,
  timeElapsedSec,
  onApply,
}: ScriptRecommendPanelProps) {
  const toast = useToast()
  const [loading, setLoading] = useState(false)
  const [items, setItems] = useState<ScriptRecommendVO[]>([])
  const [expanded, setExpanded] = useState(true)
  const [expandedItem, setExpandedItem] = useState<number | null>(null)

  const handleFetch = useCallback(async () => {
    if (!sessionId) {
      toast('请先选择场次', 'warning')
      return
    }
    setLoading(true)
    try {
      const req: ScriptRecommendRequest = {
        sessionId,
        productId,
        currentSlot,
        viewerCount,
        timeElapsed: timeElapsedSec,
        topN: 5,
      }
      const result = await recommendScripts(req)
      setItems(result ?? [])
    } catch {
      toast('获取推荐失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [sessionId, productId, currentSlot, viewerCount, timeElapsedSec, toast])

  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content).catch(() => {})
    toast('已复制话术', 'success')
  }

  return (
    <Card variant="outlined" sx={{ mb: 1 }}>
      <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <AutoAwesomeIcon fontSize="small" color="primary" />
          <Typography variant="body2" sx={{ fontWeight: 600, flex: 1 }}>
            智能推荐话术
          </Typography>
          <Button
            size="small"
            variant="contained"
            onClick={handleFetch}
            disabled={loading || !sessionId}
            startIcon={loading ? <CircularProgress size={12} /> : <AutoAwesomeIcon />}
          >
            {loading ? '推荐中...' : '获取推荐'}
          </Button>
          <IconButton size="small" onClick={() => setExpanded((e) => !e)}>
            {expanded ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
          </IconButton>
        </Box>

        <Collapse in={expanded}>
          {items.length === 0 && !loading && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, textAlign: 'center' }}>
              点击「获取推荐」查看智能推荐话术
            </Typography>
          )}

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, mt: 1 }}>
            {items.map((item, idx) => (
              <Box
                key={item.scriptId}
                sx={{
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 1,
                  overflow: 'hidden',
                }}
              >
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    px: 1,
                    py: 0.5,
                    cursor: 'pointer',
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                  onClick={() => setExpandedItem(expandedItem === idx ? null : idx)}
                >
                  <Typography variant="caption" color="text.secondary" sx={{ minWidth: 16 }}>
                    {idx + 1}.
                  </Typography>
                  {item.scriptType && (
                    <Chip
                      label={SCRIPT_TYPE_LABEL[item.scriptType] ?? item.scriptType}
                      size="small"
                      sx={{ height: 18, fontSize: '0.65rem' }}
                    />
                  )}
                  {item.effectivenessScore != null && (
                    <Chip
                      label={`${item.effectivenessScore.toFixed(0)}分`}
                      size="small"
                      color="success"
                      icon={<StarIcon />}
                      sx={{ height: 18, fontSize: '0.65rem' }}
                    />
                  )}
                  <Typography variant="caption" sx={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.contentPreview}
                  </Typography>
                  <IconButton size="small" onClick={() => setExpandedItem(expandedItem === idx ? null : idx)}>
                    {expandedItem === idx ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
                  </IconButton>
                </Box>

                <Collapse in={expandedItem === idx}>
                  <Divider />
                  <Box sx={{ px: 1.5, py: 1 }}>
                    {item.reason && (
                      <Typography variant="caption" color="primary.main" sx={{ display: 'block', mb: 0.5 }}>
                        推荐理由：{item.reason}
                      </Typography>
                    )}
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mb: 1 }}>
                      {item.contentPreview}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                      <Tooltip title="复制话术内容">
                        <IconButton size="small" onClick={() => handleCopy(item.contentPreview ?? '')}>
                          <ContentCopyIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {onApply && (
                        <Button
                          size="small"
                          variant="contained"
                          onClick={() => {
                            onApply(item.contentPreview ?? '')
                            toast('话术已应用', 'success')
                          }}
                        >
                          应用
                        </Button>
                      )}
                    </Box>
                  </Box>
                </Collapse>
              </Box>
            ))}
          </Box>
        </Collapse>
      </CardContent>
    </Card>
  )
}
