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
import { alpha } from '@mui/material/styles'
import SaveIcon from '@mui/icons-material/Save'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'
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

const AI_ASSIST_READY_ENDPOINTS = [
  '/live/analysis/get',
  '/live/script/effectiveness',
  '/live/analysis/generate',
  '/live/script/save-batch-to-library',
]

const AI_ASSIST_UNSUPPORTED_ACTIONS = [
  'local-analysis-fallback',
  'local-effectiveness-fallback',
  'local-next-session-suggestion',
  'analysis-review',
  'script-mutation',
  'shortvideo-export',
]

export function AiAssistTab({ sessionId, onRefresh }: AiAssistTabProps) {
  const toast = useToast()
  const [analysis, setAnalysis] = useState<AiAnalysisReport | null>(null)
  const [effectiveness, setEffectiveness] = useState<ScriptEffectivenessItem[]>([])
  const [analysisError, setAnalysisError] = useState<string | null>(null)
  const [effectivenessError, setEffectivenessError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [genLoading, setGenLoading] = useState(false)
  const [saveLibLoading, setSaveLibLoading] = useState(false)

  const loadAnalysis = useCallback(async () => {
    if (!sessionId || Number.isNaN(sessionId)) return
    setLoading(true)
    setAnalysisError(null)
    setEffectivenessError(null)
    try {
      const [analysisResult, effectivenessResult] = await Promise.allSettled([
        getAnalysis({ sessionId }),
        getScriptEffectiveness({ sessionId }),
      ])

      if (analysisResult.status === 'fulfilled') {
        setAnalysis(analysisResult.value as AiAnalysisReport)
      } else {
        setAnalysis(null)
        setAnalysisError(`/live/analysis/get AI 复盘报告加载失败：${getErrorMessage(analysisResult.reason)}`)
      }

      if (effectivenessResult.status === 'fulfilled') {
        const eff = effectivenessResult.value
        setEffectiveness(Array.isArray(eff) ? (eff as ScriptEffectivenessItem[]) : [])
      } else {
        setEffectiveness([])
        setEffectivenessError(`/live/script/effectiveness 话术效果加载失败：${getErrorMessage(effectivenessResult.reason)}`)
      }
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
      setAnalysisError(null)
      toast('AI 复盘报告生成成功', 'success')
    } catch (e: unknown) {
      toast(`/live/analysis/generate AI 复盘报告生成失败：${getErrorMessage(e)}`, 'error')
    } finally {
      setGenLoading(false)
    }
  }

  if (loading) {
    return (
      <Box
        data-testid="ai-assist-workbench"
        data-contract-scope="live-ai-review-assist"
        data-ready-endpoints={AI_ASSIST_READY_ENDPOINTS.join('|')}
        data-unsupported-actions={AI_ASSIST_UNSUPPORTED_ACTIONS.join('|')}
        data-session-id={sessionId}
        data-state="loading"
        data-no-local-analysis-fallback="true"
        data-no-local-effectiveness-fallback="true"
        sx={{ display: 'flex', justifyContent: 'center', py: 4 }}
      >
        <CircularProgress size={32} />
      </Box>
    )
  }

  return (
    <Box
      data-testid="ai-assist-workbench"
      data-contract-scope="live-ai-review-assist"
      data-ready-endpoints={AI_ASSIST_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={AI_ASSIST_UNSUPPORTED_ACTIONS.join('|')}
      data-session-id={sessionId}
      data-state={analysisError || effectivenessError ? 'partial-error' : 'ready'}
      data-effectiveness-count={effectiveness.length}
      data-has-analysis={analysis ? 'true' : 'false'}
      data-no-local-analysis-fallback="true"
      data-no-local-effectiveness-fallback="true"
    >
      <Box
        data-testid="ai-assist-toolbar"
        data-contract-source="/live/analysis/get|/live/analysis/generate"
        data-no-local-next-session-suggestion="true"
        sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="subtitle2" color="text.secondary">
          AI 复盘报告
        </Typography>
        <Button
          size="small"
          variant="contained"
          startIcon={<AutoAwesomeIcon />}
          onClick={handleGenerate}
          disabled={genLoading}
          data-testid="ai-assist-generate-analysis-button"
          data-contract-source="/live/analysis/generate"
        >
          {genLoading ? '生成中...' : analysis ? '重新生成' : '生成报告'}
        </Button>
      </Box>

      {analysisError && (
        <Box
          data-testid="ai-assist-analysis-error"
          data-contract-source="/live/analysis/get"
          data-no-local-analysis-fallback="true"
          sx={{ mb: 2 }}
        >
          <Typography variant="body2" color="error">{analysisError}</Typography>
        </Box>
      )}

      {effectivenessError && (
        <Box
          data-testid="ai-assist-effectiveness-error"
          data-contract-source="/live/script/effectiveness"
          data-no-local-effectiveness-fallback="true"
          sx={{ mb: 2 }}
        >
          <Typography variant="body2" color="error">{effectivenessError}</Typography>
        </Box>
      )}

      {!analysis ? (
        <Typography
          data-testid="ai-assist-analysis-empty-state"
          data-contract-source="/live/analysis/get"
          data-no-local-analysis-fallback="true"
          color="text.secondary"
          sx={{ py: 4, textAlign: 'center' }}
        >
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
            <Box
              data-testid="ai-assist-effectiveness-section"
              data-contract-source="/live/script/effectiveness"
              data-no-local-effectiveness-fallback="true"
            >
              <Typography variant="caption" color="text.secondary">话术效果排行</Typography>
              <TableContainer component={Paper} variant="outlined" sx={{ mt: 1 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow
                      data-testid="ai-assist-effectiveness-table-head-surface"
                      sx={(theme) => ({
                        bgcolor: theme.palette.mode === 'dark'
                          ? theme.palette.background.default
                          : alpha(theme.palette.common.black, 0.025),
                      })}
                    >
                      <TableCell>排名</TableCell>
                      <TableCell>话术类型</TableCell>
                      <TableCell>观众变化</TableCell>
                      <TableCell>互动变化</TableCell>
                      <TableCell>评分</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {effectiveness.map((row, i) => (
                      <TableRow
                        key={String(row.id ?? i)}
                        data-testid="ai-assist-effectiveness-row"
                        data-contract-source="/live/script/effectiveness"
                      >
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
          ) : (
            <Typography
              data-testid="ai-assist-effectiveness-empty-state"
              data-contract-source="/live/script/effectiveness"
              data-no-local-effectiveness-fallback="true"
              variant="body2"
              color="text.secondary"
            >
              暂无真实话术效果排行。
            </Typography>
          )}
          <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<SaveIcon />}
              data-testid="ai-assist-save-batch-library-button"
              data-contract-source="/live/script/save-batch-to-library"
              onClick={async () => {
                setSaveLibLoading(true)
                try {
                  await saveBatchToLibrary({ sessionId })
                  toast('高效话术已保存到话术库', 'success')
                  onRefresh?.()
                } catch (e) {
                  toast(`/live/script/save-batch-to-library 保存高效话术失败：${getErrorMessage(e)}`, 'error')
                } finally {
                  setSaveLibLoading(false)
                }
              }}
              disabled={saveLibLoading || effectiveness.length === 0}
            >
              {saveLibLoading ? '保存中...' : '保存高效话术到话术库'}
            </Button>
            <Button
              size="small"
              variant="outlined"
              onClick={() => onRefresh?.()}
              disabled={!onRefresh}
              data-testid="ai-assist-refresh-button"
              data-contract-source="local-refresh-callback"
              data-no-local-next-session-suggestion="true"
            >
              刷新复盘数据
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  )
}
