import { useState } from 'react'
import {
  Box, Card, CardContent, Button, Typography, Tab, Tabs,
  CircularProgress, Grid, Chip, Stack, Divider,
  Select, MenuItem, FormControl, InputLabel,
  LinearProgress, Paper, List, ListItem, ListItemIcon, ListItemText,
  Accordion, AccordionSummary, AccordionDetails,
  TextField, Alert,
} from '@mui/material'
import { alpha, useTheme, type Theme } from '@mui/material/styles'
import {
  LocalHospital as DiagnoseIcon,
  CheckCircle as OkIcon,
  ExpandMore as ExpandMoreIcon,
  TrendingUp as TrendIcon,
  History as HistoryIcon,
  Storage as StorageIcon,
  AccessTime as AccessTimeIcon,
} from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  brainApi,
  type BrainLlmDiagnosisResult,
  type BrainAccountDiagnosisResult,
} from '@/api/brain'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'

const CONTENT_CATEGORIES = ['护肤', '彩妆', '美容仪器', '香氛', '个护', 'general']
const INDUSTRY_BRAIN_DIAGNOSIS_READY_ENDPOINTS = '/ai/brain/content-diagnosis,/ai/brain/product-diagnosis,/ai/brain/rhythm-diagnosis,/ai/brain/account/diagnose'
const INDUSTRY_BRAIN_DIAGNOSIS_UNSUPPORTED_ENDPOINTS = '/ai/brain/mock-diagnosis,/ai/brain/local-history,/ai/brain/static-diagnosis,/ai/brain/account/local-score'

type DiagnosisTone = 'primary' | 'success' | 'warning' | 'secondary'
type ScoreTone = 'success' | 'warning' | 'error'

const DIAG_TABS = [
  { label: '内容诊断', key: 'content', desc: '话术效果与行业对比（LLM）', tone: 'primary', endpoint: '/ai/brain/content-diagnosis' },
  { label: '选品诊断', key: 'product', desc: '商品讲解与选品策略（LLM）', tone: 'success', endpoint: '/ai/brain/product-diagnosis' },
  { label: '节奏诊断', key: 'rhythm', desc: '时段与流失节奏（LLM）', tone: 'warning', endpoint: '/ai/brain/rhythm-diagnosis' },
  { label: '账号诊断', key: 'account', desc: '抖音账号定位与增长（数据模型）', tone: 'secondary', endpoint: '/ai/brain/account/diagnose' },
] satisfies Array<{
  label: string
  key: string
  desc: string
  tone: DiagnosisTone
  endpoint: string
}>

const SCORE_COLOR = (s: number): ScoreTone => s >= 80 ? 'success' : s >= 60 ? 'warning' : 'error'
const SCORE_LABEL = (s: number) => s >= 80 ? '优秀' : s >= 60 ? '良好' : '待改善'

function themeToneColor(theme: Theme, tone: DiagnosisTone | ScoreTone) {
  const palette = theme.palette[tone]
  return theme.palette.mode === 'dark' ? palette.light : palette.main
}

function isAccountDiagnosisResult(
  value: BrainLlmDiagnosisResult | BrainAccountDiagnosisResult
): value is BrainAccountDiagnosisResult {
  return (
    typeof (value as BrainAccountDiagnosisResult).positioningClarity === 'number'
    && typeof (value as BrainAccountDiagnosisResult).contentCompetitiveness === 'number'
    && typeof (value as BrainAccountDiagnosisResult).growthHealth === 'number'
  )
}

function ScoreGauge({ score }: { score: number }) {
  const theme = useTheme()
  const tone = SCORE_COLOR(score)
  const color = themeToneColor(theme, tone)
  return (
    <Box sx={{ textAlign: 'center', py: 2 }}>
      <Box sx={{ position: 'relative', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
        <Box
          data-testid="industry-brain-score-gauge-surface"
          sx={{
            width: 120,
            height: 120,
            borderRadius: '50%',
            border: '8px solid',
            borderColor: alpha(color, 0.18),
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            bgcolor: alpha(color, theme.palette.mode === 'dark' ? 0.14 : 0.06),
          }}
        >
          <Typography
            data-testid="industry-brain-score-value-surface"
            variant="h3"
            fontWeight={700}
            sx={{ color }}
          >
            {score}
          </Typography>
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
    <Stack
      data-testid="industry-brain-account-diagnosis-surface"
      data-ready-endpoints="/ai/brain/account/diagnose"
      data-no-local-account-score-fallback="true"
      spacing={2}
    >
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

function formatValue(value: unknown): string {
  if (value == null || value === '') return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return JSON.stringify(value, null, 2)
}

function pickText(result: BrainLlmDiagnosisResult): string {
  const keys = ['analysis', 'summary', 'diagnosis', 'recommendation', 'message', 'explanation', 'content']
  for (const key of keys) {
    const value = result[key]
    if (typeof value === 'string' && value.trim() !== '') return value
  }
  return formatValue(result)
}

function structuredEntries(result: BrainLlmDiagnosisResult) {
  const hidden = new Set(['analysis', 'summary', 'diagnosis', 'recommendation', 'message', 'explanation', 'content', 'tokensUsed'])
  return Object.entries(result).filter(([key, value]) => !hidden.has(key) && value != null && value !== '')
}

function LlmDiagPanel({ result, tab }: { result: BrainLlmDiagnosisResult; tab: number }) {
  const ok = result.status == null || result.status === 'success'
  const entries = structuredEntries(result)
  const mainText = pickText(result)
  return (
    <Stack
      data-testid="industry-brain-llm-result-surface"
      data-ready-endpoints={DIAG_TABS[tab].endpoint}
      data-no-static-diagnosis-fallback="true"
      spacing={2}
    >
      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        <Chip label={result.diagnosisType ?? DIAG_TABS[tab].key} size="small" />
        <Chip icon={<StorageIcon />} label={DIAG_TABS[tab].endpoint} size="small" variant="outlined" />
      </Stack>
      {!ok && (
        <Alert severity="warning">{result.message ?? result.status ?? '未完成'}</Alert>
      )}
      {mainText && (
        <Paper
          variant="outlined"
          data-testid="industry-brain-llm-diagnosis-surface"
          sx={(theme) => ({
            p: 2,
            bgcolor: theme.palette.mode === 'dark'
              ? theme.palette.background.default
              : alpha(theme.palette.common.black, 0.025),
          })}
        >
          <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', m: 0 }}>
            {mainText}
          </Typography>
        </Paper>
      )}
      {entries.length > 0 && (
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 1 }}>结构化诊断字段</Typography>
          <Grid container spacing={1}>
            {entries.map(([key, value]) => (
              <Grid item xs={12} sm={6} key={key}>
                <Box sx={{ p: 1, border: '1px solid', borderColor: 'divider', borderRadius: 1, minHeight: 72 }}>
                  <Typography variant="caption" color="text.secondary">{key}</Typography>
                  <Typography variant="body2" component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', m: 0 }}>
                    {formatValue(value)}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Paper>
      )}
      {result.tokensUsed != null && (
        <Typography variant="caption" color="text.secondary">Tokens: {result.tokensUsed}</Typography>
      )}
    </Stack>
  )
}

type HistoryEntry =
  | { kind: 'llm'; tab: number; result: BrainLlmDiagnosisResult; time: string; endpoint: string }
  | { kind: 'account'; tab: number; result: BrainAccountDiagnosisResult; time: string; endpoint: string }

export default function IndustryBrainDiagnosisPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const theme = useTheme()
  const [tab, setTab] = useState(0)
  const [category, setCategory] = useState('护肤')
  const [accountIdStr, setAccountIdStr] = useState('')
  const [result, setResult] = useState<BrainLlmDiagnosisResult | BrainAccountDiagnosisResult | null>(null)
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [pageError, setPageError] = useState<string | null>(null)

  const diagMut = useMutation({
    mutationFn: async () => {
      const activeTab = tab
      const endpoint = DIAG_TABS[activeTab].endpoint
      try {
        if (activeTab === 0) return await brainApi.contentDiagnosis({ category })
        if (activeTab === 1) return await brainApi.productDiagnosis()
        if (activeTab === 2) return await brainApi.rhythmDiagnosis()
        const id = parseInt(accountIdStr, 10)
        if (Number.isNaN(id) || id <= 0) throw new Error('请输入有效的抖音账号 ID')
        return await brainApi.accountDiagnose(id)
      } catch (e) {
        throw Object.assign(e instanceof Error ? e : new Error(String(e)), { endpoint, activeTab })
      }
    },
    onSuccess: (res) => {
      setPageError(null)
      setResult(res)
      const time = new Date().toLocaleTimeString()
      const endpoint = DIAG_TABS[tab].endpoint
      if (isAccountDiagnosisResult(res)) {
        setHistory(prev => [{ kind: 'account', tab, result: res, time, endpoint }, ...prev.slice(0, 4)])
      } else {
        setHistory(prev => [{ kind: 'llm', tab, result: res as BrainLlmDiagnosisResult, time, endpoint }, ...prev.slice(0, 4)])
      }
      toast('诊断完成', 'success')
      qc.invalidateQueries({ queryKey: ['brain-diag-history'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      const err = e as Error & { endpoint?: string; activeTab?: number }
      const endpoint = err.endpoint ?? DIAG_TABS[tab].endpoint
      setPageError(`诊断失败（POST ${endpoint}）：${message}。当前诊断类型和参数会保留。`)
      toast(`诊断失败：${message}`, 'error')
    },
  })

  const accountTabValid = tab !== 3 || (parseInt(accountIdStr, 10) > 0 && !Number.isNaN(parseInt(accountIdStr, 10)))

  return (
    <Box
      data-testid="industry-brain-diagnosis-page"
      data-ready-endpoints={INDUSTRY_BRAIN_DIAGNOSIS_READY_ENDPOINTS}
      data-unsupported-endpoints={INDUSTRY_BRAIN_DIAGNOSIS_UNSUPPORTED_ENDPOINTS}
      data-no-local-history-fallback="true"
      data-no-static-diagnosis-fallback="true"
      data-no-local-account-score-fallback="true"
    >
      <PageHeader
        title="行业大脑 · 诊断中心"
        subtitle="内容、选品、节奏诊断走 LLM 诊断接口；账号诊断走账号数据模型，未启用时展示后端降级错误。"
        breadcrumbs={[{ label: 'AI中心' }, { label: '行业大脑' }, { label: '诊断中心' }]}
      />

      <Alert
        data-testid="industry-brain-diagnosis-boundary-contract"
        data-ready-endpoints={INDUSTRY_BRAIN_DIAGNOSIS_READY_ENDPOINTS}
        data-unsupported-endpoints={INDUSTRY_BRAIN_DIAGNOSIS_UNSUPPORTED_ENDPOINTS}
        data-session-history-only="true"
        data-no-local-history-fallback="true"
        severity="info"
        sx={{ mb: 2 }}
      >
        真实接口：<code>/ai/brain/content-diagnosis</code>、<code>/ai/brain/product-diagnosis</code>、
        <code>/ai/brain/rhythm-diagnosis</code>、<code>/ai/brain/account/diagnose</code>。
        历史记录仅保存在当前浏览器会话，不伪装为后端审计记录。
      </Alert>

      {pageError ? (
        <Alert
          data-testid="industry-brain-diagnosis-page-error"
          data-input-retained="true"
          data-no-static-diagnosis-fallback="true"
          severity="error"
          sx={{ mb: 2 }}
        >
          {pageError}。请检查 <code>app.ai.brain.enabled</code>、账号诊断服务和模型配置。
        </Alert>
      ) : null}

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Card
            data-testid="industry-brain-diagnosis-params-panel"
            data-ready-endpoints={DIAG_TABS[tab].endpoint}
            data-input-retained="true"
            sx={{ mb: 2 }}
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>诊断参数</Typography>
              <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
                当前接口：<code>{DIAG_TABS[tab].endpoint}</code>
              </Alert>
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

          <Card
            data-testid="industry-brain-diagnosis-type-panel"
            data-ready-endpoints={INDUSTRY_BRAIN_DIAGNOSIS_READY_ENDPOINTS}
            data-no-static-diagnosis-fallback="true"
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>诊断类型</Typography>
              <Tabs
                data-testid="industry-brain-diagnosis-tabs"
                value={tab} onChange={(_, v) => { setTab(v); setResult(null) }}
                orientation="vertical"
                sx={{ '& .MuiTab-root': { alignItems: 'flex-start', textAlign: 'left' } }}
              >
                {DIAG_TABS.map((t, i) => (
                  <Tab
                    key={i}
                    data-testid="industry-brain-diagnosis-tab-surface"
                    label={
                      <Box>
                        <Typography variant="body2" fontWeight={600}>{t.label}</Typography>
                        <Typography variant="caption" color="text.secondary">{t.desc}</Typography>
                      </Box>
                    }
                    sx={{
                      borderLeft: '3px solid',
                      borderLeftColor: tab === i ? themeToneColor(theme, t.tone) : 'transparent',
                      mb: 1,
                      borderRadius: 1,
                    }}
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
              {!accountTabValid && tab === 3 ? (
                <Alert
                  data-testid="industry-brain-account-validation"
                  data-input-retained="true"
                  data-no-local-account-score-fallback="true"
                  severity="warning"
                  sx={{ mt: 1 }}
                >
                  账号诊断必须填写 <code>douyin_account.id</code>，不是直播场次 ID 或抖音号文本。
                </Alert>
              ) : null}
              {diagMut.isPending && <LinearProgress sx={{ mt: 1, borderRadius: 1 }} />}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card
            data-testid="industry-brain-diagnosis-result-panel"
            data-ready-endpoints={DIAG_TABS[tab].endpoint}
            data-no-static-diagnosis-fallback="true"
            sx={{ minHeight: 400 }}
          >
            <CardContent>
              {!result && !diagMut.isPending && (
                <Paper
                  variant="outlined"
                  data-testid="industry-brain-diagnosis-empty-surface"
                  sx={(theme) => ({
                    p: 4,
                    textAlign: 'center',
                    bgcolor: theme.palette.mode === 'dark'
                      ? theme.palette.background.default
                      : alpha(theme.palette.common.black, 0.025),
                  })}
                >
                  <DiagnoseIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                  <Typography color="text.secondary">
                    选择诊断类型并配置参数后，点击「开始诊断」。若行业大脑未启用，错误会展示在页面顶部。
                  </Typography>
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
                    <Stack direction="row" spacing={1} alignItems="center">
                      {tab === 0 && <Chip label={category} size="small" variant="outlined" />}
                      <Chip label={DIAG_TABS[tab].endpoint} size="small" variant="outlined" />
                    </Stack>
                  </Stack>
                  {isAccountDiagnosisResult(result)
                    ? <AccountDiagPanel result={result} />
                    : <LlmDiagPanel result={result} tab={tab} />}
                </>
              )}
            </CardContent>
          </Card>

          {history.length > 0 && (
            <Card
              data-testid="industry-brain-session-history"
              data-session-history-only="true"
              data-no-local-history-fallback="true"
              sx={{ mt: 2 }}
            >
              <CardContent>
                <Typography variant="subtitle2" fontWeight={600} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <HistoryIcon fontSize="small" /> 本次会话历史
                </Typography>
                {history.map((h, i) => (
                  <Accordion key={i} disableGutters elevation={0} sx={{ border: 1, borderColor: 'divider', mb: 0.5, borderRadius: 1, '&:before': { display: 'none' } }}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                      <Stack direction="row" spacing={2} alignItems="center">
                        <Chip label={DIAG_TABS[h.tab].label} size="small" />
                        <Chip label={h.endpoint} size="small" variant="outlined" />
                        <AccessTimeIcon fontSize="small" color="disabled" />
                        <Typography variant="caption" color="text.secondary">{h.time}</Typography>
                      </Stack>
                    </AccordionSummary>
                    <AccordionDetails>
                      {h.kind === 'account'
                        ? <AccountDiagPanel result={h.result} />
                        : <LlmDiagPanel result={h.result} tab={h.tab} />}
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
