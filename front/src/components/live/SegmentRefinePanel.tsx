/**
 * 单句/单段话术微调组件
 * 对应后端 POST /api/v1/live/ai/refine-segment（LiveAiController）
 * 实现「话术生成与微调升级计划.md」中的单段粒度 UI
 */

import { useState } from 'react'
import {
  Box,
  Typography,
  TextField,
  Button,
  CircularProgress,
  Chip,
  Paper,
  Tooltip,
  IconButton,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import { alpha } from '@mui/material/styles'
import { refineSegment } from '@/api/live-ai'
import { useToast } from '@/contexts/ToastContext'

interface SegmentRefineProps {
  scriptId: number
  segmentText: string
  onApply: (refined: string) => void
  modelId?: number
  compact?: boolean
}

const QUICK_INSTRUCTIONS = [
  { label: '更口语化', value: '改为更口语、生活化的表达，去掉书面用语' },
  { label: '增加情感', value: '增加情感词和情绪感染力，让表达更有温度' },
  { label: '缩短精简', value: '精简内容，保留核心信息，压缩到原来的70%' },
  { label: '突出卖点', value: '突出产品卖点和用户利益，增加购买冲动' },
  { label: '合规化', value: '移除可能违规的绝对化用语，改为合规表达' },
]

export function SegmentRefinePanel({ scriptId, segmentText, onApply, modelId, compact = false }: SegmentRefineProps) {
  const toast = useToast()
  const [isOpen, setIsOpen] = useState(false)
  const [instruction, setInstruction] = useState('')
  const [refined, setRefined] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleRefine = async () => {
    if (!instruction.trim()) {
      toast('请输入微调指令', 'warning')
      return
    }
    setLoading(true)
    setRefined(null)
    try {
      const result = await refineSegment(scriptId, segmentText, instruction, modelId)
      setRefined(typeof result === 'string' ? result : JSON.stringify(result))
    } catch {
      toast('单段微调失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleApply = () => {
    if (refined) {
      onApply(refined)
      setIsOpen(false)
      setRefined(null)
      setInstruction('')
      toast('已应用微调结果', 'success')
    }
  }

  if (compact && !isOpen) {
    return (
      <Tooltip title="单段AI微调">
        <IconButton size="small" onClick={() => setIsOpen(true)} color="primary">
          <AutoFixHighIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    )
  }

  return (
    <Box>
      {!isOpen ? (
        <Button
          size="small"
          variant="outlined"
          startIcon={<AutoFixHighIcon />}
          onClick={() => setIsOpen(true)}
        >
          AI 单段微调
        </Button>
      ) : (
        <Paper variant="outlined" sx={{ p: 1.5, mt: 1 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="caption" fontWeight={600} color="primary">
              <AutoFixHighIcon sx={{ fontSize: 14, mr: 0.5, verticalAlign: 'middle' }} />
              单段微调
            </Typography>
            <IconButton size="small" onClick={() => { setIsOpen(false); setRefined(null) }}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* 原文预览 */}
          <Box
            data-testid="segment-refine-original-surface"
            sx={(theme) => ({
              bgcolor: theme.palette.mode === 'dark'
                ? theme.palette.background.default
                : alpha(theme.palette.common.black, 0.025),
              border: `1px solid ${theme.palette.divider}`,
              p: 1,
              borderRadius: 0.5,
              mb: 1,
            })}
          >
            <Typography variant="caption" color="text.secondary" display="block">原文</Typography>
            <Typography variant="body2" sx={{ fontSize: 12 }}>
              {segmentText.length > 100 ? segmentText.slice(0, 100) + '...' : segmentText}
            </Typography>
          </Box>

          {/* 快捷指令 */}
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
            {QUICK_INSTRUCTIONS.map((q) => (
              <Chip
                key={q.label}
                label={q.label}
                size="small"
                variant="outlined"
                onClick={() => setInstruction(q.value)}
                color={instruction === q.value ? 'primary' : 'default'}
                sx={{ cursor: 'pointer' }}
              />
            ))}
          </Box>

          {/* 自定义指令 */}
          <TextField
            size="small"
            fullWidth
            multiline
            rows={2}
            placeholder="输入微调指令，如：更口语化、突出成分功效、压缩到50字以内..."
            value={instruction}
            onChange={(e) => setInstruction(e.target.value)}
            sx={{ mb: 1 }}
          />

          <Button
            size="small"
            variant="contained"
            onClick={handleRefine}
            disabled={loading || !instruction.trim()}
            startIcon={loading ? <CircularProgress size={14} /> : <EditIcon />}
            fullWidth
          >
            {loading ? '微调中...' : '开始微调'}
          </Button>

          {/* 微调结果 */}
          {refined && (
            <Box sx={{ mt: 1.5 }}>
              <Box
                data-testid="segment-refine-result-surface"
                sx={(theme) => ({
                  bgcolor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.18 : 0.08),
                  border: '1px solid',
                  borderColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.5 : 0.28),
                  borderRadius: 0.5,
                  p: 1,
                })}
              >
                <Typography variant="caption" color="success.main" display="block" fontWeight={600} sx={{ mb: 0.5 }}>
                  微调结果
                </Typography>
                <Typography variant="body2" sx={{ fontSize: 12, whiteSpace: 'pre-wrap' }}>
                  {refined}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                <Button
                  size="small"
                  variant="contained"
                  color="success"
                  startIcon={<CheckIcon />}
                  onClick={handleApply}
                  sx={{ flex: 1 }}
                >
                  采用
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => { setRefined(null); setInstruction('') }}
                >
                  重做
                </Button>
              </Box>
            </Box>
          )}
        </Paper>
      )}
    </Box>
  )
}
