import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  Grid,
  InputLabel,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import GavelIcon from '@mui/icons-material/Gavel'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { PageHeader } from '@/components/base'
import { scriptApi } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'

const SCOPE_OPTIONS = [
  { value: 'all', label: '全部场景' },
  { value: 'live', label: '直播' },
  { value: 'video', label: '短视频' },
]

const LEVEL_LABELS: Record<number, { label: string; color: 'info' | 'warning' | 'error' }> = {
  1: { label: '低', color: 'info' },
  2: { label: '中', color: 'warning' },
  3: { label: '高', color: 'error' },
}

const VIOLATION_CHECK_ROUTE = '/admin/script/violation-check'
const VIOLATION_CHECK_READY_ENDPOINTS = ['/script/violation/check'] as const
const VIOLATION_CHECK_RELATED_ENDPOINTS = [
  '/script/violation/check-batch',
  '/script/violation/public/list',
  '/script/violation/suggest-replacement',
] as const
const VIOLATION_CHECK_UNSUPPORTED_ACTIONS = [
  'local-rule-fallback',
  'inline-public-list',
  'inline-batch-check',
  'inline-suggest-replacement',
] as const

const contractList = (items: readonly string[]) => items.join('|')

interface ViolationItem {
  word: string
  position?: number
  length?: number
  reason?: string
  level?: number
  replacement?: string
  source?: string
}

interface ViolationResult {
  hasViolation?: boolean
  totalCount?: number
  violations: ViolationItem[]
}

export default function ViolationCheckPage() {
  const toast = useToast()
  const [text, setText] = useState('')
  const [scope, setScope] = useState('all')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<ViolationResult | null>(null)

  const handleCheck = async () => {
    if (!text.trim()) {
      toast('请输入待检测文本', 'warning')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const data = await scriptApi.violationCheck(text.trim(), scope)
      setResult({
        hasViolation: data.hasViolation,
        totalCount: data.totalCount,
        violations: Array.isArray(data.violations) ? data.violations : [],
      })
    } catch (e: unknown) {
      const message = getErrorMessage(e)
      setError(`${message}；上下文：route=${VIOLATION_CHECK_ROUTE}; scope=${scope}; textLength=${text.trim().length}`)
      toast(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  const hasViolation = result && result.violations.length > 0
  const highRisk = result?.violations.filter((item) => Number(item.level ?? 0) >= 3).length ?? 0
  const publicCount = result?.violations.filter((item) => item.source === 'public').length ?? 0
  const userCount = result?.violations.filter((item) => item.source === 'user').length ?? 0

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
      data-testid="violation-check-workbench"
      data-contract-scope="script-violation-check"
      data-ready-endpoints={contractList(VIOLATION_CHECK_READY_ENDPOINTS)}
      data-related-endpoints={contractList(VIOLATION_CHECK_RELATED_ENDPOINTS)}
      data-unsupported-actions={contractList(VIOLATION_CHECK_UNSUPPORTED_ACTIONS)}
      data-scope={scope}
      data-text-length={text.trim().length}
      data-result-count={result?.violations.length ?? 0}
      data-high-risk-count={highRisk}
      data-public-hit-count={publicCount}
      data-user-hit-count={userCount}
    >
      <PageHeader
        title="违规检测"
        subtitle="对齐 /script/violation/check：请求会携带 text 与 scope，结果来自公共违规词库和个人违规词库。"
        breadcrumbs={[{ label: '话术' }, { label: '违规检测' }]}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="violation-check-contract-alert"
        data-contract-gap="single-check-only"
        data-no-local-rule-fallback="true"
        data-no-public-list-request="true"
        data-no-batch-check-request="true"
        data-no-suggest-replacement-request="true"
      >
        检测范围使用后端真实枚举 <code>all/live/video</code>；违规词维护页使用 <code>all/live_only/video_only</code>，
        两者分别对应检测请求和词库适用范围。
      </Alert>

      <Grid container spacing={2}>
        {[
          { key: 'total', label: '命中总数', value: result?.totalCount ?? result?.violations.length ?? 0, source: '/script/violation/check totalCount' },
          { key: 'high-risk', label: '高风险', value: highRisk, source: 'local-derived level>=3' },
          { key: 'public', label: '公共库命中', value: publicCount, source: 'local-derived source=public' },
          { key: 'user', label: '个人库命中', value: userCount, source: 'local-derived source=user' },
        ].map((item) => (
          <Grid item xs={6} md={3} key={item.key}>
            <Card
              variant="outlined"
              data-testid={`violation-check-kpi-${item.key}`}
              data-contract-source={item.source}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card
        variant="outlined"
        data-testid="violation-check-input-card"
        data-ready-endpoint="/script/violation/check"
        data-request-fields="text|scope"
      >
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel id="violation-check-scope-label">检测范围</InputLabel>
              <Select
                labelId="violation-check-scope-label"
                value={scope}
                label="检测范围"
                onChange={(e) => setScope(e.target.value)}
              >
                {SCOPE_OPTIONS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <GavelIcon />}
              onClick={handleCheck}
              disabled={loading || !text.trim()}
              sx={{ width: { xs: '100%', sm: 120 } }}
              data-testid="violation-check-submit"
              data-ready-endpoint="/script/violation/check"
            >
              {loading ? '检测中' : '开始检测'}
            </Button>
          </Stack>

          <TextField
            fullWidth
            multiline
            minRows={7}
            label="待检测文本"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="输入直播话术、短视频脚本或商品口播文本..."
          />
        </CardContent>
      </Card>

      {error && (
        <Alert
          severity="error"
          data-testid="violation-check-error"
          data-no-local-rule-fallback="true"
          action={<Button color="inherit" size="small" onClick={handleCheck}>重试</Button>}
        >
          检测失败：{error}。接口来源：/script/violation/check；不会用本地规则伪造检测结果，当前文本和检测范围会保留。
        </Alert>
      )}

      {result && (
        <Card
          variant="outlined"
          data-testid="violation-check-result-card"
          data-contract-source="/script/violation/check"
          data-has-violation={String(Boolean(hasViolation))}
          data-result-count={result.violations.length}
        >
          <CardContent>
            {hasViolation ? (
              <>
                <Alert severity="error" sx={{ mb: 2 }}>
                  发现 {result.totalCount ?? result.violations.length} 处违规词，请按等级和替换建议处理。
                </Alert>
                <List dense disablePadding>
                  {result.violations.map((item, index) => {
                    const level = LEVEL_LABELS[Number(item.level ?? 2)] ?? LEVEL_LABELS[2]
                    return (
                      <ListItem
                        key={`${item.word}-${index}`}
                        sx={{ px: 0, borderBottom: '1px solid', borderColor: 'divider' }}
                        data-testid={`violation-check-hit-${index}`}
                        data-hit-source={item.source ?? 'unknown'}
                        data-hit-level={Number(item.level ?? 2)}
                      >
                        <ListItemText
                          primary={
                            <Stack direction="row" alignItems="center" gap={1} flexWrap="wrap" useFlexGap>
                              <Chip color="error" size="small" label={item.word} />
                              <Chip color={level.color} size="small" variant="outlined" label={level.label} />
                              <Chip size="small" variant="outlined" label={item.source === 'user' ? '个人库' : '公共库'} />
                              {typeof item.position === 'number' && (
                                <Typography variant="caption" color="text.secondary">
                                  位置 {item.position}
                                </Typography>
                              )}
                              {item.replacement && (
                                <Typography variant="body2" color="text.secondary">
                                  建议替换：{item.replacement}
                                </Typography>
                              )}
                            </Stack>
                          }
                          secondary={item.reason ? `原因：${item.reason}` : undefined}
                        />
                      </ListItem>
                    )
                  })}
                </List>
              </>
            ) : (
              <Alert severity="success" icon={<CheckCircleIcon />}>
                未发现违规词。仍建议在发布前结合平台规则、商品资质和人工审核复核。
              </Alert>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
