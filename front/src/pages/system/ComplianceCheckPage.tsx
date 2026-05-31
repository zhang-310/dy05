import { useMemo, useState } from 'react'
import {
  Box, Card, CardContent, TextField, Button, Typography,
  Stack, Select, MenuItem, Chip, CircularProgress, Alert,
  Grid,
} from '@mui/material'
import { GppGood as ComplianceIcon, Refresh as RefreshIcon } from '@mui/icons-material'
import type { GridColDef } from '@mui/x-data-grid'
import { useQuery } from '@tanstack/react-query'
import { DataGridEmptyOverlay, ErrorAlert, PageHeader, StandardDataGrid } from '@/components/base'
import {
  scriptComplianceCheck,
  scriptComplianceIndustryCodes,
  scriptComplianceRules,
  douyinOfficialReferences,
  type ScriptComplianceViolation,
} from '@/api/compliance'
import { getErrorMessage } from '@/utils/errorHandler'

const LEVEL_COLOR: Record<string, 'error' | 'warning' | 'info'> = {
  error: 'error', high: 'error', warning: 'warning', medium: 'warning', low: 'info', info: 'info',
}

const INDUSTRY_LABELS: Record<string, string> = {
  cosmetics: '护肤美妆',
  food: '食品饮料',
  health_supplement: '保健品',
  apparel: '服饰鞋包',
  digital_3c: '数码 3C',
  mother_baby: '母婴',
  jewelry: '珠宝',
  pet: '宠物',
  medical_device: '医疗器械',
  education: '教育培训',
  finance: '金融',
  real_estate: '房产',
  general: '通用规则',
}

const FALLBACK_CODES = [
  'cosmetics',
  'food',
  'health_supplement',
  'apparel',
  'digital_3c',
  'mother_baby',
  'jewelry',
  'pet',
  'medical_device',
  'education',
  'finance',
  'real_estate',
  'general',
]
const COMPLIANCE_ENDPOINTS = [
  '/script/compliance/industry-codes',
  '/script/compliance/rules',
  '/script/compliance/douyin-official-references',
  '/script/compliance/check',
].join('|')
const COMPLIANCE_UNSUPPORTED_ENDPOINTS = [
  '/compliance/check',
  '/compliance/stats',
  '/compliance/rule/list',
  '/script/compliance/local-check',
  '/script/compliance/mock-rules',
].join('|')
const COMPLIANCE_UNSUPPORTED_ACTIONS = [
  'general-compliance-check-endpoint',
  'local-violation-fallback',
  'local-rule-fallback-for-check',
  'local-official-reference-fallback',
  'client-side-industry-rule-match',
].join('|')

type ViolationRow = ScriptComplianceViolation & { __rid: number }

function MetricCard({ label, value, helper }: { label: string; value: string | number; helper?: string }) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" fontWeight={700}>{value}</Typography>
        {helper && <Typography variant="caption" color="text.secondary">{helper}</Typography>}
      </CardContent>
    </Card>
  )
}

export default function ComplianceCheckPage() {
  const [industry, setIndustry] = useState('cosmetics')
  const [text, setText] = useState('')
  const [results, setResults] = useState<ScriptComplianceViolation[]>([])
  const [loading, setLoading] = useState(false)
  const [checked, setChecked] = useState(false)
  const [checkError, setCheckError] = useState('')

  const industryCodesQuery = useQuery({
    queryKey: ['script-compliance-industry-codes'],
    queryFn: scriptComplianceIndustryCodes,
    staleTime: 10 * 60 * 1000,
  })

  const rulesQuery = useQuery({
    queryKey: ['script-compliance-rules', industry],
    queryFn: () => scriptComplianceRules(industry),
    staleTime: 10 * 60 * 1000,
  })

  const referencesQuery = useQuery({
    queryKey: ['script-compliance-douyin-references'],
    queryFn: douyinOfficialReferences,
    staleTime: 10 * 60 * 1000,
  })

  const industryCodes = useMemo(() => {
    const remote = industryCodesQuery.data?.verticalCodes ?? []
    const merged = Array.from(new Set([...remote, 'general']))
    return merged.length > 1 ? merged : FALLBACK_CODES
  }, [industryCodesQuery.data?.verticalCodes])
  const industryCodeSource = (industryCodesQuery.data?.verticalCodes?.length ?? 0) > 0 ? 'backend' : 'static-selector-fallback'

  const handleCheck = async () => {
    if (!text.trim()) return
    setLoading(true); setChecked(false); setCheckError('')
    try {
      const res = await scriptComplianceCheck({ text, industryCode: industry })
      setResults(res || []); setChecked(true)
    } catch (error) {
      setResults([])
      setCheckError(getErrorMessage(error))
    }
    finally { setLoading(false) }
  }

  const rows: ViolationRow[] = useMemo(() => results.map((v, i) => ({ ...v, __rid: i })), [results])
  const highCount = results.filter(v => ['error', 'high'].includes(v.level.toLowerCase())).length
  const warningCount = results.filter(v => ['warning', 'medium'].includes(v.level.toLowerCase())).length

  const columns = useMemo<GridColDef<ViolationRow>[]>(() => [
    { field: 'matchedText', headerName: '违规文本', flex: 1, minWidth: 140,
      renderCell: p => <Typography color="error" fontWeight="bold" component="span">{p.value}</Typography> },
    { field: 'level', headerName: '等级', width: 90,
      renderCell: p => <Chip label={p.value} color={LEVEL_COLOR[p.value as string] ?? 'default'} size="small" /> },
    { field: 'source', headerName: '来源', width: 120,
      renderCell: p => <Chip label={p.value === 'douyin_public_summary' ? '抖音公开摘要' : '行业规则'} size="small" variant="outlined" /> },
    { field: 'reason', headerName: '原因', flex: 1.5, minWidth: 160 },
    { field: 'reference', headerName: '法规依据', flex: 1, minWidth: 120 },
    { field: 'position', headerName: '位置', width: 80, type: 'number' },
  ], [])

  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 3 }}
      data-testid="compliance-check-page-workbench"
      data-contract-scope="script-compliance-industry-check"
      data-ready-endpoints={COMPLIANCE_ENDPOINTS}
      data-unsupported-endpoints={COMPLIANCE_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={COMPLIANCE_UNSUPPORTED_ACTIONS}
      data-industry={industry}
      data-industry-code-source={industryCodeSource}
      data-rule-count={rulesQuery.data?.length ?? ''}
      data-reference-count={referencesQuery.data?.referenceUrls.length ?? ''}
      data-result-count={results.length}
      data-checked={String(checked)}
      data-loading={String(loading)}
      data-check-error={checkError}
      data-no-local-violation-fallback="true"
      data-no-client-side-rule-match="true"
      data-no-general-compliance-endpoint="true"
      data-no-local-official-reference-fallback="true"
    >
      <PageHeader
        title="合规检测"
        subtitle="对齐行业合规真实接口：检测走 /script/compliance/check，规则来源为通用广告规则、垂直行业规则和抖音公开规则摘要。"
        breadcrumbs={[{ label: '系统' }, { label: '合规检测' }]}
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => {
              void industryCodesQuery.refetch()
              void rulesQuery.refetch()
              void referencesQuery.refetch()
            }}
          >
            刷新规则
          </Button>
        }
      />

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard label="当前行业" value={INDUSTRY_LABELS[industry] ?? industry} helper={industry === 'general' ? '仅通用 + 平台摘要' : '叠加垂直规则'} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard label="规则条目" value={rulesQuery.data?.length ?? '--'} helper="由后端规则服务返回" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard label="高风险命中" value={highCount} helper="error/high" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard label="中风险命中" value={warningCount} helper="warning/medium" />
        </Grid>
      </Grid>

      <Card
        data-testid="compliance-input-contract"
        data-contract-source="/script/compliance/check"
        data-industry={industry}
        data-text-length={text.length}
        data-no-general-compliance-endpoint="true"
        data-no-client-side-rule-match="true"
      >
        <CardContent>
          <Stack spacing={2}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Typography variant="body2" fontWeight={500}>行业：</Typography>
              <Select size="small" value={industry} onChange={e => setIndustry(e.target.value)} sx={{ minWidth: 140 }}>
                {industryCodes.map(code => (
                  <MenuItem key={code} value={code}>{INDUSTRY_LABELS[code] ?? code}</MenuItem>
                ))}
              </Select>
              {rulesQuery.isFetching && <Chip size="small" label="规则刷新中" variant="outlined" />}
            </Stack>
            <TextField label="待检测文本" value={text} onChange={e => setText(e.target.value)}
              fullWidth multiline rows={6} placeholder="粘贴话术/脚本文案，检测是否符合行业广告法规" />
            <Button variant="contained" startIcon={<ComplianceIcon />}
              onClick={handleCheck} disabled={loading || !text.trim()} sx={{ alignSelf: 'flex-start' }}>
              {loading ? '检测中...' : '检测合规'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {industryCodesQuery.isError && (
        <Box
          data-testid="compliance-industry-codes-error"
          data-contract-source="/script/compliance/industry-codes"
          data-fallback-source="static-selector-only"
          data-no-local-rule-fallback-for-check="true"
        >
          <ErrorAlert
            severity="warning"
            title="行业编码加载失败"
            message={`${getErrorMessage(industryCodesQuery.error)}。页面已使用内置行业列表兜底，但检测仍以后端返回为准。`}
            onRetry={() => void industryCodesQuery.refetch()}
          />
        </Box>
      )}
      {rulesQuery.isError && (
        <Box
          data-testid="compliance-rules-error"
          data-contract-source="/script/compliance/rules"
          data-no-local-rule-fallback-for-check="true"
        >
          <ErrorAlert
            severity="warning"
            title="行业规则加载失败"
            message={`${getErrorMessage(rulesQuery.error)}。不影响手动检测，但无法展示当前行业规则条目数。`}
            onRetry={() => void rulesQuery.refetch()}
          />
        </Box>
      )}
      {referencesQuery.isError && (
        <Box
          data-testid="compliance-official-references-error"
          data-contract-source="/script/compliance/douyin-official-references"
          data-no-local-official-reference-fallback="true"
          data-no-client-side-rule-match="true"
        >
          <ErrorAlert
            severity="warning"
            title="抖音公开参考加载失败"
            message={`${getErrorMessage(referencesQuery.error)}。页面不会用本地摘要伪造官方规则，检测仍以 /script/compliance/check 的后端结果为准。`}
            onRetry={() => void referencesQuery.refetch()}
          />
        </Box>
      )}
      {checkError && (
        <Box
          data-testid="compliance-check-error"
          data-contract-source="/script/compliance/check"
          data-input-retained="true"
          data-text-length={text.length}
          data-no-local-violation-fallback="true"
        >
          <ErrorAlert title="合规检测失败" message={checkError} onRetry={handleCheck} />
        </Box>
      )}

      <Alert
        severity="info"
        variant="outlined"
        data-testid="compliance-source-contract"
        data-contract-source="/script/compliance/check|/script/compliance/rules|/script/compliance/industry-codes|/script/compliance/douyin-official-references"
        data-unsupported-endpoints={COMPLIANCE_UNSUPPORTED_ENDPOINTS}
        data-no-general-compliance-endpoint="true"
        data-industry-code-source={industryCodeSource}
        data-no-local-official-reference-fallback="true"
      >
        通用 `/compliance/check` 与行业 `/script/compliance/check` 请求体不同；本页只调用行业检测。未知行业编码会按后端策略降级为通用规则加抖音公开摘要。
      </Alert>

      {loading && <CircularProgress sx={{ display: 'block', mx: 'auto' }} />}
      {checked && results.length === 0 && (
        <Alert
          severity="success"
          data-testid="compliance-check-clean"
          data-contract-source="/script/compliance/check"
          data-result-count="0"
          data-no-local-violation-fallback="true"
        >
          未检测到违规内容，文案合规！
        </Alert>
      )}
      <Card
        data-testid="compliance-result-contract"
        data-contract-source="/script/compliance/check"
        data-result-count={results.length}
        data-high-count={highCount}
        data-warning-count={warningCount}
        data-no-local-violation-fallback="true"
        data-no-client-side-rule-match="true"
      >
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="h6" color={results.length > 0 ? 'error' : 'text.primary'}>
              检测结果 {checked ? `(${results.length})` : ''}
            </Typography>
            {referencesQuery.data?.referenceUrls.length ? (
              <Chip size="small" variant="outlined" label={`公开参考 ${referencesQuery.data.referenceUrls.length} 条`} />
            ) : null}
          </Stack>
          <Box sx={{ minHeight: 280 }}>
            <StandardDataGrid
              rows={rows}
              columns={columns}
              getRowId={r => String((r as ViolationRow).__rid)}
              hideFooter
              disableColumnMenu
              slots={{ noRowsOverlay: DataGridEmptyOverlay }}
              sx={{ border: 'none' }}
            />
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}
