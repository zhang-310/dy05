import { useState } from 'react'
import {
  Box, Card, CardContent, Button, Typography, Tab, Tabs,
  CircularProgress, Grid, Chip, Stack, Divider,
  Select, MenuItem, FormControl, InputLabel,
  LinearProgress, Paper, List, ListItem, ListItemIcon, ListItemText,
  Accordion, AccordionSummary, AccordionDetails,
  TextField, Alert,
} from '@mui/material'
import {
  LocalHospital as DiagnoseIcon,
  CheckCircle as OkIcon,
  ExpandMore as ExpandMoreIcon,
  TrendingUp as TrendIcon,
  History as HistoryIcon,
} from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  brainApi,
  type BrainLlmDiagnosisResult,
  type BrainAccountDiagnosisResult,
} from '@/api/brain'
import { useToast } from '@/contexts/ToastContext'

const CONTENT_CATEGORIES = ['护肤', '彩妆', '美容仪器', '香氛', '个护', 'general']

const DIAG_TABS = [
  { label: '内容诊断', key: 'content', desc: '话术效果与行业对比（LLM）', color: '#1976d2' },
  { label: '选品诊断', key: 'product', desc: '商品讲解与选品策略（LLM）', color: '#388e3c' },
  { label: '节奏诊断', key: 'rhythm', desc: '时段与流失节奏（LLM）', color: '#f57c00' },
  { label: '账号诊断', key: 'account', desc: '抖音账号定位与增长（数据模型）', color: '#7b1fa2' },
]

const SCORE_COLOR = (s: number) => s >= 80 ? 'success' : s >= 60 ? 'warning' : 'error'
const SCORE_LABEL = (s: number) => s >= 80 ? '优秀' : s >= 60 ? '良好' : '待改善'

function ScoreGauge({ score }: { score: number }) {
  const color = score >= 80 ? '#4caf50' : score >= 60 ? '#ff9800' : '#f44336'
  return (
    <Box sx={{ textAlign: 'center', py: 2 }}>
      <Box sx={{ position: 'relative', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
        <Box sx={{ width: 120, height: 120, borderRadius: '50%', border: `8px solid ${color}22`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', bgcolor: `${color}08` }}>
          <Typography variant="h3" fontWeight={700} sx={{ color }}>{score}</Typography>
          <Typography variant="caption" sx={{ color }}>/ 100</Typography>
        </Box>
      </Box>
      <Typography variant="subtitle1" fontWeight={600} mt={1}>
        <Chip label={SCORE_LABEL(score)} color={SCORE_COLOR(score)} size="small" />
      </Typography>
    </Box>
  )
}

function AccountDiagPanel({ result }: { result: BrainAccountDiagnosisResult }) {
  const score = Math.round(
    ((result.positioningClarity ?? 0) * 0.35
      + (result.contentCompetitiveness ?? 0) * 0.35
      + (result.growthHealth ?? 0) * 0.3) * 100
  )
  return (
    <Stack spacing={2}>
      <ScoreGauge score={score} />
      <Divider />
      <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>{result.summary}</Typography>
      <Chip label={`风险：${result.riskLevel}`} size="small" variant="outlined" />
      {(result.estimated || result.isEstimated) ? <Alert severity="warning">部分指标为估算值，数据完善后更准确</Alert> : null}
      {(result.suggestedPriorities?.length ?? 0) > 0 && (
        <Box>
          <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 1 }}>优先行动</Typography>
          <List dense disablePadding>
            {result.suggestedPriorities!.map((iss, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.3 }}>
                <ListItemIcon sx={{ minWidth: 28 }}><OkIcon fontSize="small" color="success" /></ListItemIcon>
                <ListItemText primary={iss} primaryTypographyProps={{ variant: 'body2' }} />
              </ListItem>
            ))}
          </List>
        </Box>
      )}
      <Typography variant="caption" color="text.secondary">
        定位清晰度 {(result.positioningClarity * 100).toFixed(0)}% · 内容竞争力 {(result.contentCompetitiveness * 100).toFixed(0)}% · 增长健康 {(result.growthHealth * 100).toFixed(0)}%
      </Typography>
    </Stack>
  )
}

function LlmDiagPanel({ result }: { result: BrainLlmDiagnosisResult }) {
  const ok = result.status === 'success'
  return (
    <Stack spacing={2}>
      <Chip label={result.diagnosisType ?? '诊断'} size="small" />
      {!ok && (
        <Alert severity="warning">{result.message ?? result.status ?? '未完成'}</Alert>
      )}
      {ok && result.analysis && (
        <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
          <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', m: 0 }}>
            {result.analysis}
          </Typography>
        </Paper>
      )}
      {result.tokensUsed != null && (
        <Typography variant="caption" color="text.secondary">Tokens: {result.tokensUsed}</Typography>
      )}
    </Stack>
  )
}

type HistoryEntry =
  | { kind: 'llm'; tab: number; result: BrainLlmDiagnosisResult; time: string }
  | { kind: 'account'; tab: number; result: BrainAccountDiagnosisResult; time: string }

export default function IndustryBrainDiagnosisPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [tab, setTab] = useState(0)
  const [category, setCategory] = useState('护肤')
  const [accountIdStr, setAccountIdStr] = useState('')
  const [result, setResult] = useState<BrainLlmDiagnosisResult | BrainAccountDiagnosisResult | null>(null)
  const [history, setHistory] = useState<HistoryEntry[]>([])

  const diagMut = useMutation({
    mutationFn: async () => {
      if (tab === 0) return brainApi.contentDiagnosis({ category })
      if (tab === 1) return brainApi.productDiagnosis()
      if (tab === 2) return brainApi.rhythmDiagnosis()
      const id = parseInt(accountIdStr, 10)
      if (Number.isNaN(id) || id <= 0) throw new Error('请输入有效的抖音账号 ID')
      return brainApi.accountDiagnose(id)
    },
    onSuccess: (res) => {
      setResult(res)
      const time = new Date().toLocaleTimeString()
      if ('positioningClarity' in res) {
        setHistory(prev => [{ kind: 'account', tab, result: res, time }, ...prev.slice(0, 4)])
      } else {
        setHistory(prev => [{ kind: 'llm', tab, result: res as BrainLlmDiagnosisResult, time }, ...prev.slice(0, 4)])
      }
      toast('诊断完成', 'success')
      qc.invalidateQueries({ queryKey: ['brain-diag-history'] })
    },
    onError: (e: Error) => toast(e.message || '诊断失败', 'error'),
  })

  const accountTabValid = tab !== 3 || (parseInt(accountIdStr, 10) > 0 && !Number.isNaN(parseInt(accountIdStr, 10)))

  return (
    <Box>
      <PageHeader
        title="行业大脑 · 诊断中心"
        breadcrumbs={[{ label: 'AI中心' }, { label: '行业大脑' }, { label: '诊断中心' }]}
      />

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>诊断参数</Typography>
              {tab === 0 && (
                <FormControl size="small" fullWidth>
                  <InputLabel>行业分类</InputLabel>
                  <Select value={category} label="行业分类" onChange={e => setCategory(String(e.target.value))}>
                    {CONTENT_CATEGORIES.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
                  </Select>
                </FormControl>
              )}
              {tab === 3 && (
                <TextField
                  fullWidth
                  size="small"
                  label="抖音账号 ID（douyin_account.id）"
                  value={accountIdStr}
                  onChange={e => setAccountIdStr(e.target.value)}
                  helperText="与直播场次 ID 不同，请在账号管理页查看抖音账号主键"
                />
              )}
              {(tab === 1 || tab === 2) && (
                <Alert severity="info" sx={{ mt: 0 }}>将基于当前登录用户上下文调用 LLM 分析</Alert>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>诊断类型</Typography>
              <Tabs
                value={tab} onChange={(_, v) => { setTab(v); setResult(null) }}
                orientation="vertical"
                sx={{ '& .MuiTab-root': { alignItems: 'flex-start', textAlign: 'left' } }}
              >
                {DIAG_TABS.map((t, i) => (
                  <Tab
                    key={i}
                    label={
                      <Box>
                        <Typography variant="body2" fontWeight={600}>{t.label}</Typography>
                        <Typography variant="caption" color="text.secondary">{t.desc}</Typography>
                      </Box>
                    }
                    sx={{ borderLeft: tab === i ? `3px solid ${t.color}` : '3px solid transparent', mb: 1, borderRadius: 1 }}
                  />
                ))}
              </Tabs>
              <Button
                fullWidth variant="contained" sx={{ mt: 2 }}
                startIcon={diagMut.isPending ? <CircularProgress size={16} color="inherit" /> : <DiagnoseIcon />}
                onClick={() => diagMut.mutate()}
                disabled={diagMut.isPending || !accountTabValid}
              >
                {diagMut.isPending ? '诊断中...' : `开始${DIAG_TABS[tab].label}`}
              </Button>
              {diagMut.isPending && <LinearProgress sx={{ mt: 1, borderRadius: 1 }} />}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card sx={{ minHeight: 400 }}>
            <CardContent>
              {!result && !diagMut.isPending && (
                <Paper variant="outlined" sx={{ p: 4, textAlign: 'center', bgcolor: 'grey.50' }}>
                  <DiagnoseIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                  <Typography color="text.secondary">选择诊断类型并配置参数后，点击「开始诊断」</Typography>
                </Paper>
              )}
              {diagMut.isPending && (
                <Box sx={{ textAlign: 'center', py: 6 }}>
                  <CircularProgress size={48} />
                  <Typography mt={2} color="text.secondary">分析中...</Typography>
                  <LinearProgress sx={{ mt: 2, mx: 4, borderRadius: 1 }} />
                </Box>
              )}
              {result && !diagMut.isPending && (
                <>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
                    <Typography variant="subtitle1" fontWeight={600} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TrendIcon color="primary" fontSize="small" />
                      {DIAG_TABS[tab].label}结果
                    </Typography>
                    {tab === 0 && <Chip label={category} size="small" variant="outlined" />}
                  </Stack>
                  {'positioningClarity' in result
                    ? <AccountDiagPanel result={result} />
                    : <LlmDiagPanel result={result} />}
                </>
              )}
            </CardContent>
          </Card>

          {history.length > 0 && (
            <Card sx={{ mt: 2 }}>
              <CardContent>
                <Typography variant="subtitle2" fontWeight={600} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <HistoryIcon fontSize="small" /> 本次会话历史
                </Typography>
                {history.map((h, i) => (
                  <Accordion key={i} disableGutters elevation={0} sx={{ border: 1, borderColor: 'divider', mb: 0.5, borderRadius: 1, '&:before': { display: 'none' } }}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                      <Stack direction="row" spacing={2} alignItems="center">
                        <Chip label={DIAG_TABS[h.tab].label} size="small" />
                        <Typography variant="caption" color="text.secondary">{h.time}</Typography>
                      </Stack>
                    </AccordionSummary>
                    <AccordionDetails>
                      {h.kind === 'account'
                        ? <AccountDiagPanel result={h.result} />
                        : <LlmDiagPanel result={h.result} />}
                    </AccordionDetails>
                  </Accordion>
                ))}
              </CardContent>
            </Card>
          )}
        </Grid>
      </Grid>
    </Box>
  )
}
