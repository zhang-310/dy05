import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Typography,
  Button,
  Chip,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
} from '@mui/material'
import SaveIcon from '@mui/icons-material/Save'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useToast } from '@/contexts/ToastContext'
import {
  generateAnalysis,
  getAnalysis,
  getScriptEffectiveness,
  saveBatchToLibrary,
} from '@/api/live'

const SCRIPT_TYPE_LABEL: Record<string, string> = {
  opening: '开场',
  product: '产品',
  transition: '转场',
  closing: '结尾',
  custom: '自定义',
}

interface AiAssistTabProps {
  sessionId: number
  onRefresh?: () => void
}
interface AiAnalysisReport { rating?: unknown; summary?: unknown; highlights?: string[]; issues?: string[]; suggestions?: string[] }
interface ScriptEffectivenessItem { id?: unknown; scriptType?: string; viewerDelta?: number; interactionDelta?: number; effectivenessScore?: number }

export function AiAssistTab({ sessionId, onRefresh }: AiAssistTabProps) {
  const toast = useToast()
  const [analysis, setAnalysis] = useState<AiAnalysisReport | null>(null)
  const [effectiveness, setEffectiveness] = useState<ScriptEffectivenessItem[]>([])
  const [loading, setLoading] = useState(false)
  const [genLoading, setGenLoading] = useState(false)
  const [saveLibLoading, setSaveLibLoading] = useState(false)

  const loadAnalysis = useCallback(async () => {
    if (!sessionId || Number.isNaN(sessionId)) return
    setLoading(true)
    try {
      const [data, eff] = await Promise.all([
        getAnalysis({ sessionId }).catch(() => null),
        getScriptEffectiveness({ sessionId }).catch(() => []),
      ])
      setAnalysis(data as AiAnalysisReport)
      setEffectiveness(Array.isArray(eff) ? (eff as ScriptEffectivenessItem[]) : [])
    } catch {
      setAnalysis(null)
      setEffectiveness([])
    } finally {
      setLoading(false)
    }
  }, [sessionId])

  useEffect(() => {
    loadAnalysis()
  }, [loadAnalysis])

  const handleGenerate = async () => {
    setGenLoading(true)
    try {
      const data = await generateAnalysis({ sessionId })
      setAnalysis(data as AiAnalysisReport)
      toast('AI 复盘报告生成成功', 'success')
    } catch (e: unknown) {
      const msg = e && typeof e === 'object' && 'message' in e ? String((e as { message: unknown }).message) : '生成失败'
      toast(msg, 'error')
    } finally {
      setGenLoading(false)
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress size={32} />
      </Box>
    )
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="subtitle2" color="text.secondary">
          AI 复盘报告
        </Typography>
        <Button
          size="small"
          variant="contained"
          startIcon={<AutoAwesomeIcon />}
          onClick={handleGenerate}
          disabled={genLoading}
        >
          {genLoading ? '生成中...' : analysis ? '重新生成' : '生成报告'}
        </Button>
      </Box>

      {!analysis ? (
        <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
          暂无 AI 复盘报告，点击「生成报告」基于场次数据生成分析
        </Typography>
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {analysis.rating != null && analysis.rating !== '' ? (
            <Box>
              <Typography variant="caption" color="text.secondary">综合评分</Typography>
              <Chip label={String(analysis.rating)} color="primary" sx={{ ml: 1 }} />
            </Box>
          ) : null}
          {analysis.summary != null && analysis.summary !== '' ? (
            <Box>
              <Typography variant="caption" color="text.secondary">总结</Typography>
              <Typography sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{String(analysis.summary)}</Typography>
            </Box>
          ) : null}
          {Array.isArray(analysis.highlights) && analysis.highlights.length > 0 ? (
            <Box>
              <Typography variant="caption" color="text.secondary">亮点</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                {(analysis.highlights ?? []).map((h, i) => (
                  <Chip key={i} label={h} size="small" color="success" variant="outlined" />
                ))}
              </Box>
            </Box>
          ) : null}
          {Array.isArray(analysis.issues) && analysis.issues.length > 0 ? (
            <Box>
              <Typography variant="caption" color="text.secondary">问题</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                {(analysis.issues ?? []).map((h, i) => (
                  <Chip key={i} label={h} size="small" color="warning" variant="outlined" />
                ))}
              </Box>
            </Box>
          ) : null}
          {Array.isArray(analysis.suggestions) && analysis.suggestions.length > 0 ? (
            <Box>
              <Typography variant="caption" color="text.secondary">建议</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                {(analysis.suggestions ?? []).map((h, i) => (
                  <Chip key={i} label={h} size="small" variant="outlined" />
                ))}
              </Box>
            </Box>
          ) : null}
          {effectiveness.length > 0 ? (
            <Box>
              <Typography variant="caption" color="text.secondary">话术效果排行</Typography>
              <TableContainer component={Paper} variant="outlined" sx={{ mt: 1 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ bgcolor: 'grey.50' }}>
                      <TableCell>排名</TableCell>
                      <TableCell>话术类型</TableCell>
                      <TableCell>观众变化</TableCell>
                      <TableCell>互动变化</TableCell>
                      <TableCell>评分</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {effectiveness.map((row, i) => (
                      <TableRow key={String(row.id ?? i)}>
                        <TableCell>{i + 1}</TableCell>
                        <TableCell>{SCRIPT_TYPE_LABEL[String(row.scriptType ?? '')] ?? row.scriptType ?? '-'}</TableCell>
                        <TableCell>{row.viewerDelta != null ? (Number(row.viewerDelta) >= 0 ? '+' : '') + row.viewerDelta : '-'}</TableCell>
                        <TableCell>{row.interactionDelta != null ? (Number(row.interactionDelta) >= 0 ? '+' : '') + row.interactionDelta : '-'}</TableCell>
                        <TableCell>{row.effectivenessScore != null ? Number(row.effectivenessScore).toFixed(1) : '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          ) : null}
          <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<SaveIcon />}
              onClick={async () => {
                setSaveLibLoading(true)
                try {
                  await saveBatchToLibrary({ sessionId })
                  toast('高效话术已保存到话术库', 'success')
                  onRefresh?.()
                } catch (e) {
                  toast(e instanceof Error ? e.message : '保存失败', 'error')
                } finally {
                  setSaveLibLoading(false)
                }
              }}
              disabled={saveLibLoading || effectiveness.length === 0}
            >
              {saveLibLoading ? '保存中...' : '保存高效话术到话术库'}
            </Button>
            <Button size="small" variant="outlined" onClick={() => onRefresh?.()} disabled={!onRefresh}>
              生成下场直播建议
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  )
}
