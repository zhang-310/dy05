import { useState } from 'react'
import {
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Typography,
  Button,
  Stack,
  Box,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  FormControlLabel,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TableContainer,
  Paper,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type {
  EvolutionAnalysisResultVO,
  KnowledgeEvolutionAnalysisScope,
} from '@/types/knowledgeEvolutionAnalysis'

const ANALYSIS_SCOPE_OPTIONS: { value: KnowledgeEvolutionAnalysisScope; label: string }[] = [
  { value: 'LAST_7_DAYS', label: '最近 7 天' },
  { value: 'LAST_14_DAYS', label: '最近 14 天' },
  { value: 'LAST_30_DAYS', label: '最近 30 天' },
  { value: 'LAST_90_DAYS', label: '最近 90 天' },
]

export interface AnalysisResultSectionProps {
  onAfterOptimize?: () => void
}

export function AnalysisResultSection({ onAfterOptimize }: AnalysisResultSectionProps) {
  const toast = useToast()
  const qc = useQueryClient()
  const [scope, setScope] = useState<KnowledgeEvolutionAnalysisScope>('LAST_7_DAYS')
  const [analysis, setAnalysis] = useState<EvolutionAnalysisResultVO | null>(null)
  const [autoInclude, setAutoInclude] = useState(true)
  const [autoMerge, setAutoMerge] = useState(true)
  const [autoArchive, setAutoArchive] = useState(false)

  const analyzeMut = useMutation({
    mutationFn: () => aiApi.knowledgeEvolutionAnalyze({ analysisScope: scope }),
    onSuccess: (data) => {
      setAnalysis(data)
      toast('分析已完成', 'success')
    },
    onError: (e: Error) => toast(e.message || '分析失败', 'error'),
  })

  const optimizeMut = useMutation({
    mutationFn: () => {
      const id = analysis?.analysisId
      if (!id) throw new Error('请先运行分析')
      return aiApi.knowledgeEvolutionAutoOptimize({
        analysisId: id,
        actions: { autoInclude, autoMerge, autoArchive },
        approvalRequired: false,
      })
    },
    onSuccess: () => {
      toast('已提交自动优化', 'success')
      onAfterOptimize?.()
      qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
      qc.invalidateQueries({ queryKey: ['evolve-roi'] })
      qc.invalidateQueries({ queryKey: ['evolve-score-trend'] })
    },
    onError: (e: Error) => toast(e.message || '自动优化失败', 'error'),
  })

  const inc = analysis?.readyForInclusion?.length ?? 0
  const opt = analysis?.needsOptimization?.length ?? 0
  const dup = analysis?.duplicatesDetected?.length ?? 0
  const arc = analysis?.readyForArchival?.length ?? 0
  const totalRows = inc + opt + dup + arc
  const degraded = analysis?.degraded === true
  const impact = analysis?.expectedImpact

  return (
    <Paper variant="outlined" sx={{ p: 2, flexShrink: 0, width: '100%', minWidth: 0 }}>
      <Typography variant="subtitle1" fontWeight={600} color="text.primary" gutterBottom>
        知识库进化分析
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        基于话术规则引擎的入库/优化/去重/归档机会分析；与上方「进化任务」数据源不同。
      </Typography>

      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel id="ke-analysis-scope">分析周期</InputLabel>
          <Select
            labelId="ke-analysis-scope"
            label="分析周期"
            value={scope}
            onChange={e => setScope(e.target.value as KnowledgeEvolutionAnalysisScope)}
          >
            {ANALYSIS_SCOPE_OPTIONS.map(o => (
              <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button
          variant="contained"
          size="small"
          startIcon={<PlayArrowIcon />}
          disabled={analyzeMut.isPending}
          onClick={() => analyzeMut.mutate()}
        >
          运行分析
        </Button>
      </Stack>

      {analysis ? (
        <>
          {degraded ? (
            <Alert severity="warning" sx={{ mb: 2 }}>
              当前环境未启用规则引擎（EvolutionRuleEngineService），分析结果为占位结构。部署完整规则能力后可得到真实机会列表。
            </Alert>
          ) : null}
          {!degraded && totalRows === 0 ? (
            <Alert severity="info" sx={{ mb: 2 }}>
              分析完成：当前周期内未检出待处理机会。
            </Alert>
          ) : null}

          <Box sx={{ mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
              分析 ID：<strong>{analysis.analysisId ?? '—'}</strong>
              {' · '}
              周期 {analysis.periodStart ?? '—'} ~ {analysis.periodEnd ?? '—'}
            </Typography>
            {impact ? (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                预期：入库 {impact.newInclusionsCount ?? 0} · 去重 {impact.deduplicationCount ?? 0}
                {' · '}
                改善率 {impact.improvementRate != null ? String(impact.improvementRate) : '—'}
              </Typography>
            ) : null}
          </Box>

          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" sx={{ mb: 2 }}>
            <FormControlLabel
              control={<Checkbox checked={autoInclude} onChange={e => setAutoInclude(e.target.checked)} />}
              label="自动入库"
            />
            <FormControlLabel
              control={<Checkbox checked={autoMerge} onChange={e => setAutoMerge(e.target.checked)} />}
              label="自动合并去重"
            />
            <FormControlLabel
              control={<Checkbox checked={autoArchive} onChange={e => setAutoArchive(e.target.checked)} />}
              label="自动归档（谨慎）"
            />
            <Button
              variant="outlined"
              size="small"
              disabled={optimizeMut.isPending || degraded || !analysis.analysisId}
              onClick={() => optimizeMut.mutate()}
            >
              应用优化
            </Button>
          </Stack>
          {degraded ? (
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 2 }}>
              降级模式下已禁用「应用优化」。
            </Typography>
          ) : null}

          <Accordion defaultExpanded>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography fontWeight={600}>待入库 ({inc})</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <TableContainer sx={{ maxHeight: 260 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>话术版本</TableCell>
                      <TableCell>标题</TableCell>
                      <TableCell align="right">分数</TableCell>
                      <TableCell>原因</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(analysis.readyForInclusion ?? []).map((r, i) => (
                      <TableRow key={`${r.scriptVersionId ?? i}-inc`}>
                        <TableCell>{r.scriptVersionId ?? '—'}</TableCell>
                        <TableCell>{r.title ?? '—'}</TableCell>
                        <TableCell align="right">{r.score ?? '—'}</TableCell>
                        <TableCell>{r.reason ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </AccordionDetails>
          </Accordion>

          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography fontWeight={600}>待优化 ({opt})</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <TableContainer sx={{ maxHeight: 260 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>话术版本</TableCell>
                      <TableCell>标题</TableCell>
                      <TableCell>建议</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(analysis.needsOptimization ?? []).map((r, i) => (
                      <TableRow key={`${r.scriptVersionId ?? i}-opt`}>
                        <TableCell>{r.scriptVersionId ?? '—'}</TableCell>
                        <TableCell>{r.title ?? '—'}</TableCell>
                        <TableCell>{r.suggestion ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </AccordionDetails>
          </Accordion>

          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography fontWeight={600}>去重候选 ({dup})</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <TableContainer sx={{ maxHeight: 260 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>主话术</TableCell>
                      <TableCell align="right">重复数</TableCell>
                      <TableCell>建议</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(analysis.duplicatesDetected ?? []).map((r, i) => (
                      <TableRow key={`${r.masterScriptId ?? i}-dup`}>
                        <TableCell>{r.masterTitle ?? r.masterScriptId ?? '—'}</TableCell>
                        <TableCell align="right">{r.duplicateScriptIds?.length ?? 0}</TableCell>
                        <TableCell>{r.recommendation ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </AccordionDetails>
          </Accordion>

          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography fontWeight={600}>待归档 ({arc})</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <TableContainer sx={{ maxHeight: 260 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>话术版本</TableCell>
                      <TableCell>标题</TableCell>
                      <TableCell>原因</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(analysis.readyForArchival ?? []).map((r, i) => (
                      <TableRow key={`${r.scriptVersionId ?? i}-arc`}>
                        <TableCell>{r.scriptVersionId ?? '—'}</TableCell>
                        <TableCell>{r.title ?? '—'}</TableCell>
                        <TableCell>{r.reason ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </AccordionDetails>
          </Accordion>
        </>
      ) : (
        <Typography variant="body2" color="text.secondary">
          点击「运行分析」加载机会列表。
        </Typography>
      )}
    </Paper>
  )
}
