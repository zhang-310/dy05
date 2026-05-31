import { useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import TipsAndUpdatesIcon from '@mui/icons-material/TipsAndUpdates'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { scriptApi, type ScriptOptimizationResult } from '@/api/script'

const OPTIMIZE_ENDPOINT = '/script/optimize'
const RELATED_ENDPOINTS = ['/script/generate', '/script/list', '/script/save']
const UNSUPPORTED_ENDPOINTS = [
  '/script/generate',
  '/short-video/script/generate',
  '/product/script/generate',
  '/copy/ai/generate',
  '/live/ai/generate-product-script',
  '/script/optimize/mock',
]
const UNSUPPORTED_ACTIONS = [
  'generate-instead-of-optimize',
  'inline-library-save',
  'shortvideo-script-generate',
  'product-script-generate',
  'copy-ai-generate',
  'live-product-script-generate',
  'static-optimized-content',
]

const STYLE_OPTIONS = [
  { value: 'conversion', label: '转化强化' },
  { value: 'friendly', label: '亲和口播' },
  { value: 'urgent', label: '紧迫促销' },
  { value: 'storytelling', label: '故事化' },
]

function buildChecks(text: string, goal: string) {
  const issues: string[] = []
  const suggestions: string[] = []
  const normalized = text.trim()
  if (normalized.length < 80) issues.push('话术偏短，可能缺少卖点展开和转化引导。')
  if (!/[？?]/.test(normalized)) suggestions.push('可以加入一个问题式开场，提高停留。')
  if (!/(立即|马上|今天|限时|现在)/.test(normalized)) suggestions.push('如适合当前活动，可补充轻量紧迫感。')
  if (!/(点击|下单|领取|关注|咨询)/.test(normalized)) issues.push('缺少明确行动号召。')
  if (goal.trim()) suggestions.push(`按目标「${goal.trim()}」重写时，应保留原始卖点并只强化表达方式。`)
  return { issues, suggestions }
}

export default function ScriptOptimizationPage() {
  const toast = useToast()
  const [original, setOriginal] = useState('')
  const [goal, setGoal] = useState('')
  const [style, setStyle] = useState('conversion')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<ScriptOptimizationResult | null>(null)

  const checks = useMemo(() => buildChecks(original, goal), [original, goal])
  const score = useMemo(() => {
    if (!original.trim()) return 0
    return Math.max(20, 100 - checks.issues.length * 20 - checks.suggestions.length * 8)
  }, [checks.issues.length, checks.suggestions.length, original])

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => toast('已复制', 'success'))
  }

  const handleOptimize = async () => {
    if (!original.trim()) {
      toast('请先输入原始话术', 'warning')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await scriptApi.optimize({
        originalContent: original.trim(),
        goal: goal.trim() || undefined,
        style,
      })
      setResult(res)
      toast('话术已优化', 'success')
    } catch (e) {
      const msg = e instanceof Error ? e.message : '优化失败，请检查 AI 服务配置'
      setError(`话术优化失败：${msg}。接口来源：${OPTIMIZE_ENDPOINT}；上下文：route=/admin/script/optimization; style=${style}; goal=${goal.trim() || '空'}; originalLength=${original.trim().length}。失败会保留原文和本地结构诊断，不伪造 AI 优化结果。`)
      toast(msg, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box
      data-testid="script-optimization-workbench"
      data-contract-scope="script-optimization"
      data-ready-endpoints={OPTIMIZE_ENDPOINT}
      data-related-endpoints={RELATED_ENDPOINTS.join('|')}
      data-unsupported-endpoints={UNSUPPORTED_ENDPOINTS.join('|')}
      data-unsupported-actions={UNSUPPORTED_ACTIONS.join('|')}
      data-original-length={original.trim().length}
      data-goal-length={goal.trim().length}
      data-style={style}
      data-local-score={score}
      data-local-issue-count={checks.issues.length}
      data-local-suggestion-count={checks.suggestions.length}
      data-result-has-content={String(Boolean(result?.optimizedContent))}
      data-generation-time-ms={result?.generationTime ?? ''}
      data-no-static-optimized-content="true"
    >
      <PageHeader
        title="话术优化"
        breadcrumbs={[{ label: '话术管理' }, { label: '话术优化' }]}
        subtitle="对齐后端 /script/optimize 真实契约优化已有话术；本地诊断只作为接口失败或人工复核时的辅助提示。"
      />

      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="script-optimization-contract-alert"
        data-contract-source={OPTIMIZE_ENDPOINT}
        data-no-script-generate-request="true"
        data-no-library-save-request="true"
        data-no-shortvideo-generate-request="true"
        data-no-product-script-generate-request="true"
        data-no-copy-ai-generate-request="true"
        data-no-live-script-generate-request="true"
        data-no-static-optimized-content="true"
      >
        数据源：{OPTIMIZE_ENDPOINT}。该接口实时调用内容域 AI 优化能力，当前不写生成记录；失败时保留本地结构诊断，不伪造 AI 优化结果。
      </Alert>

      <Stack spacing={2}>
        <Card
          variant="outlined"
          data-testid="script-optimization-form-card"
          data-ready-endpoint={OPTIMIZE_ENDPOINT}
          data-contract-payload="originalContent|goal|style"
        >
          <CardContent>
            <Stack spacing={2}>
              <TextField
                label="原始话术"
                required
                multiline
                minRows={7}
                value={original}
                onChange={(e) => setOriginal(e.target.value)}
                placeholder="粘贴需要优化的话术内容..."
                inputProps={{ 'data-contract-param': 'originalContent' }}
              />
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <FormControl sx={{ minWidth: 180 }}>
                  <InputLabel id="script-optimization-style-label">优化风格</InputLabel>
                  <Select
                    labelId="script-optimization-style-label"
                    id="script-optimization-style"
                    value={style}
                    label="优化风格"
                    onChange={(e) => setStyle(e.target.value)}
                    inputProps={{ 'data-contract-param': 'style' }}
                  >
                    {STYLE_OPTIONS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
                  </Select>
                </FormControl>
                <TextField
                  label="优化目标（可选）"
                  value={goal}
                  onChange={(e) => setGoal(e.target.value)}
                  placeholder="例如：增强紧迫感、提升亲和力、突出价格优势..."
                  inputProps={{ 'data-contract-param': 'goal' }}
                  sx={{ flex: 1 }}
                />
              </Stack>
              <Button
                variant="contained"
                startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <AutoFixHighIcon />}
                disabled={loading}
                onClick={handleOptimize}
                data-contract-action="optimizeScript"
                data-ready-endpoint={OPTIMIZE_ENDPOINT}
              >
                {loading ? 'AI 优化中...' : 'AI 优化话术'}
              </Button>
            </Stack>
          </CardContent>
        </Card>

        {error && (
          <Alert
            severity="error"
            data-testid="script-optimization-error"
            data-contract-source={OPTIMIZE_ENDPOINT}
            data-no-static-optimized-content="true"
            data-style={style}
            data-goal={goal.trim() || '空'}
            data-original-length={original.trim().length}
          >
            {error}
          </Alert>
        )}

        {result && (
          <Card
            variant="outlined"
            data-testid="script-optimization-result-card"
            data-contract-source={OPTIMIZE_ENDPOINT}
            data-result-has-content={String(Boolean(result.optimizedContent))}
            data-original-score={result.originalScore ?? ''}
            data-optimized-score={result.optimizedScore ?? ''}
            data-generation-time-ms={result.generationTime ?? ''}
          >
            <CardContent>
              <Stack spacing={1.5}>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Typography variant="subtitle1" fontWeight={700}>AI 优化结果</Typography>
                  {result.originalScore != null && <Chip label={`原评分 ${Number(result.originalScore).toFixed(1)}`} variant="outlined" />}
                  {result.optimizedScore != null && <Chip label={`优化后 ${Number(result.optimizedScore).toFixed(1)}`} color="success" />}
                  {result.generationTime != null && <Chip label={`${result.generationTime}ms`} color="primary" variant="outlined" />}
                  <Box sx={{ flex: 1 }} />
                  <Button
                    size="small"
                    startIcon={<ContentCopyIcon />}
                    disabled={!result.optimizedContent}
                    onClick={() => handleCopy(result.optimizedContent)}
                    data-contract-action="copy-optimized-content"
                  >
                    复制优化稿
                  </Button>
                </Stack>
                {result.optimizedContent ? (
                  <Typography
                    component="pre"
                    sx={{ m: 0, p: 1.5, bgcolor: 'action.hover', borderRadius: 1, whiteSpace: 'pre-wrap', lineHeight: 1.7 }}
                  >
                    {result.optimizedContent}
                  </Typography>
                ) : (
                  <Alert
                    severity="warning"
                    data-testid="script-optimization-empty-result"
                    data-contract-source={OPTIMIZE_ENDPOINT}
                    data-no-static-optimized-content="true"
                  >
                    {OPTIMIZE_ENDPOINT} 已返回但缺少 optimizedContent，不使用本地文本伪造成优化结果。
                  </Alert>
                )}
                {result.suggestions.length > 0 && (
                  <Stack spacing={1}>
                    {result.suggestions.map((item, index) => (
                      <Alert
                        key={item}
                        severity="info"
                        data-testid={`script-optimization-result-suggestion-${index}`}
                        data-contract-source={`${OPTIMIZE_ENDPOINT} suggestions`}
                      >
                        {item}
                      </Alert>
                    ))}
                  </Stack>
                )}
              </Stack>
            </CardContent>
          </Card>
        )}

        <Card
          variant="outlined"
          data-testid="script-optimization-local-diagnosis"
          data-contract-source="local-structure-diagnosis"
          data-local-score={score}
          data-local-issue-count={checks.issues.length}
          data-local-suggestion-count={checks.suggestions.length}
          data-no-ai-result-fabrication="true"
        >
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
              <Typography variant="subtitle1" fontWeight={700}>本地结构诊断</Typography>
              <Chip label={`${score}/100`} color={score >= 80 ? 'success' : score >= 60 ? 'warning' : 'error'} />
              <Chip label={`问题 ${checks.issues.length}`} variant="outlined" />
              <Chip label={`建议 ${checks.suggestions.length}`} variant="outlined" />
              <Box sx={{ flex: 1 }} />
              <Button
                size="small"
                startIcon={<ContentCopyIcon />}
                disabled={!original.trim()}
                onClick={() => handleCopy(original)}
                data-contract-action="copy-original-content"
              >
                复制原文
              </Button>
            </Stack>
            <Divider sx={{ my: 1.5 }} />
            {!original.trim() ? (
              <Typography variant="body2" color="text.secondary">输入话术后显示结构诊断；点击 AI 优化会调用真实后端接口。</Typography>
            ) : (
              <Stack spacing={1}>
                {checks.issues.length === 0 && checks.suggestions.length === 0 && (
                  <Alert severity="success">当前文本结构完整，可进入人工润色或调用后端优化接口做风格增强。</Alert>
                )}
                {checks.issues.map((item) => <Alert key={item} severity="warning">{item}</Alert>)}
                {checks.suggestions.map((item) => <Alert key={item} severity="info" icon={<TipsAndUpdatesIcon />}>{item}</Alert>)}
              </Stack>
            )}
          </CardContent>
        </Card>
      </Stack>
    </Box>
  )
}
