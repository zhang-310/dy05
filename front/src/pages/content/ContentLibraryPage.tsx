import { Alert, Box, Button, Chip, Grid, LinearProgress, Paper, Stack, Typography } from '@mui/material'
import ArticleIcon from '@mui/icons-material/Article'
import GavelIcon from '@mui/icons-material/Gavel'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import LocalOfferIcon from '@mui/icons-material/LocalOffer'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/base'
import { scriptApi } from '@/api/script'
import { copyApi } from '@/api/copy'
import { slangApi } from '@/api/slangdict'
import { getErrorMessage } from '@/utils/errorHandler'

interface ContentCapability {
  title: string
  description: string
  route: string
  owner: string
  endpoints: string[]
  status: 'ready'
  statusText: string
  icon: React.ReactNode
  diagnosticKey: DiagnosticKey
}

type DiagnosticKey = 'scripts' | 'templates' | 'copy' | 'violation' | 'compliance' | 'slang'
const CONTENT_DIAGNOSTIC_ENDPOINTS = [
  '/script/list',
  '/script/template/search',
  '/copy/library/search',
  '/script/admin/violation/list',
  '/script/compliance/rules',
  '/slangdict/entry/search',
] as const
const CONTENT_UNSUPPORTED_ACTIONS = ['inline-crud', 'nested-workbench', 'cross-domain-write'] as const
const CONTENT_DIAGNOSTIC_ENDPOINTS_ATTR = CONTENT_DIAGNOSTIC_ENDPOINTS.join(',')
const CONTENT_UNSUPPORTED_ACTIONS_ATTR = CONTENT_UNSUPPORTED_ACTIONS.join(',')

const capabilities: ContentCapability[] = [
  {
    title: '话术库',
    description: '直播话术列表、生成结果复用、删除与模板入口。',
    route: '/org/script/list',
    owner: 'script',
    endpoints: ['/script/list', '/script/save', '/script/delete'],
    status: 'ready',
    statusText: '真实接口',
    icon: <ArticleIcon />,
    diagnosticKey: 'scripts',
  },
  {
    title: '话术模板',
    description: '维护场景化话术模板，保存字段按 templateName/templateType/scene/content 对齐。',
    route: '/org/script/templates',
    owner: 'script',
    endpoints: ['/script/template/search', '/script/template/save', '/script/template/delete'],
    status: 'ready',
    statusText: '真实接口',
    icon: <LibraryBooksIcon />,
    diagnosticKey: 'templates',
  },
  {
    title: '文案管理',
    description: '文案库、审批和变量模板的统一入口，独立页面承接具体编辑操作。',
    route: '/org/copy/all',
    owner: 'copy',
    endpoints: ['/copy/library/search', '/copy/approval/search', '/copy/template/search'],
    status: 'ready',
    statusText: '真实接口',
    icon: <ArticleIcon />,
    diagnosticKey: 'copy',
  },
  {
    title: '违规词库',
    description: '违规词维护、等级和原因字段以 script admin 违规词接口为准。',
    route: '/org/script/violation-words',
    owner: 'script',
    endpoints: ['/script/admin/violation/list', '/script/admin/violation/save', '/script/admin/violation/delete'],
    status: 'ready',
    statusText: '真实接口',
    icon: <GavelIcon />,
    diagnosticKey: 'violation',
  },
  {
    title: '合规检测',
    description: '面向话术内容的检测入口，展示命中词、等级、来源和建议。',
    route: '/org/script/violation-check',
    owner: 'script',
    endpoints: ['/script/violation/check', '/script/compliance/check'],
    status: 'ready',
    statusText: '真实接口',
    icon: <SearchIcon />,
    diagnosticKey: 'compliance',
  },
  {
    title: '梗库',
    description: '运营热梗和使用场景词库，增删改查走真实接口；启停通过保存 status 完成。',
    route: '/org/slangdict',
    owner: 'slangdict',
    endpoints: ['/slangdict/entry/search', '/slangdict/entry/save', '/slangdict/entry/delete'],
    status: 'ready',
    statusText: '真实接口',
    icon: <LocalOfferIcon />,
    diagnosticKey: 'slang',
  },
]

function getPageTotal(data: unknown) {
  if (Array.isArray(data)) return data.length
  if (typeof data === 'object' && data !== null && 'total' in data) {
    return Number((data as { total?: unknown }).total ?? 0)
  }
  return undefined
}

export default function ContentLibraryPage() {
  const navigate = useNavigate()
  const scriptsQuery = useQuery({
    queryKey: ['content-library-diagnostics', 'scripts'],
    queryFn: () => scriptApi.list({ page: 0, rows: 1 }),
    retry: false,
  })
  const templatesQuery = useQuery({
    queryKey: ['content-library-diagnostics', 'templates'],
    queryFn: () => scriptApi.templateSearch({ page: 0, rows: 1 }),
    retry: false,
  })
  const copyQuery = useQuery({
    queryKey: ['content-library-diagnostics', 'copy'],
    queryFn: () => copyApi.list({ page: 0, rows: 1 }),
    retry: false,
  })
  const violationQuery = useQuery({
    queryKey: ['content-library-diagnostics', 'violation'],
    queryFn: () => scriptApi.violationList({ page: 0, rows: 1 }),
    retry: false,
  })
  const complianceQuery = useQuery({
    queryKey: ['content-library-diagnostics', 'compliance'],
    queryFn: () => scriptApi.complianceRules(),
    retry: false,
  })
  const slangQuery = useQuery({
    queryKey: ['content-library-diagnostics', 'slang'],
    queryFn: () => slangApi.list({ page: 0, rows: 1 }),
    retry: false,
  })

  const diagnostics = {
    scripts: scriptsQuery,
    templates: templatesQuery,
    copy: copyQuery,
    violation: violationQuery,
    compliance: complianceQuery,
    slang: slangQuery,
  }

  const queries = Object.values(diagnostics)
  const loadingCount = queries.filter(q => q.isFetching).length
  const errorCount = queries.filter(q => q.isError).length
  const healthyCount = queries.filter(q => q.isSuccess).length
  const checkedCount = healthyCount + errorCount
  const totalAssets = queries.reduce((sum, query) => sum + (getPageTotal(query.data) ?? 0), 0)

  const ownerSummary = [
    { label: '真实接口', value: capabilities.length, color: 'primary.main', hint: '当前卡片均有后端路径', metric: 'capability-count' },
    { label: '接口可用', value: healthyCount, color: 'success.main', hint: `${checkedCount}/${queries.length} 已完成诊断`, metric: 'healthy-count' },
    { label: '接口异常', value: errorCount, color: 'error.main', hint: errorCount > 0 ? '查看下方卡片错误' : '暂无异常', metric: 'error-count' },
    { label: '资产总量', value: totalAssets, color: 'warning.main', hint: '按各接口 total 聚合', metric: 'asset-total' },
  ]

  const refreshAll = () => {
    queries.forEach(query => void query.refetch())
  }

  return (
    <Box
      data-testid="content-library-workbench"
      data-contract-scope="content-library"
      data-diagnostic-endpoints={CONTENT_DIAGNOSTIC_ENDPOINTS_ATTR}
      data-unsupported-actions={CONTENT_UNSUPPORTED_ACTIONS_ATTR}
      data-capability-count={capabilities.length}
      data-loading-count={loadingCount}
      data-healthy-count={healthyCount}
      data-error-count={errorCount}
      data-checked-count={checkedCount}
      data-total-assets={totalAssets}
      data-no-inline-write="true"
      data-no-local-asset-fallback="true"
      data-no-cross-domain-write="true"
      sx={{ p: 3 }}
    >
      <PageHeader
        title="内容库"
        subtitle="内容域入口页展示能力地图、真实接口健康和资产数量；具体增删改查进入各独立工作台完成。"
        breadcrumbs={[{ label: '内容运营' }, { label: '内容库' }]}
        actions={(
          <Button variant="outlined" size="small" startIcon={<RefreshIcon />} onClick={refreshAll} disabled={loadingCount > 0}>
            刷新诊断
          </Button>
        )}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="content-library-shell-contract"
        data-contract-status="gateway-only"
        data-unsupported-actions={CONTENT_UNSUPPORTED_ACTIONS_ATTR}
        data-diagnostic-endpoints={CONTENT_DIAGNOSTIC_ENDPOINTS_ATTR}
        data-no-inline-write="true"
        data-no-local-asset-fallback="true"
        sx={{ mb: 2 }}
      >
        本页不再嵌套完整子页面，避免话术、文案、合规和梗库工作台互相套壳导致布局过深；每张卡片会先探测对应真实接口，再跳转到真实路由。
      </Alert>
      {loadingCount > 0 && <LinearProgress sx={{ mb: 2 }} />}
      {errorCount > 0 && (
        <Alert
          severity="warning"
          data-testid="content-library-diagnostic-error-summary"
          data-contract-status="partial-error"
          data-error-count={errorCount}
          data-no-fake-health="true"
          sx={{ mb: 2 }}
        >
          有 {errorCount} 个内容接口诊断失败。入口仍可进入对应工作台，工作台内会继续展示更具体的错误和重试动作。
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mb: 2 }}>
        {ownerSummary.map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper
              variant="outlined"
              data-testid="content-library-summary-card"
              data-summary-metric={item.metric}
              data-contract-status="local-derived"
              data-source-endpoints={CONTENT_DIAGNOSTIC_ENDPOINTS_ATTR}
              sx={{ p: 1.5 }}
            >
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h4" fontWeight={700} color={item.color}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        {capabilities.map(item => (
          <Grid item xs={12} md={6} xl={4} key={item.title}>
            <Paper
              variant="outlined"
              data-testid="content-library-capability-card"
              data-capability-title={item.title}
              data-owner={item.owner}
              data-route={item.route}
              data-contract-status={diagnostics[item.diagnosticKey].isError ? 'source-error' : diagnostics[item.diagnosticKey].isSuccess ? 'ready' : 'checking'}
              data-diagnostic-key={item.diagnosticKey}
              data-diagnostic-endpoint={item.endpoints[0]}
              data-capability-endpoints={item.endpoints.join(',')}
              data-inline-crud="unsupported"
              data-no-inline-write="true"
              data-no-local-asset-fallback="true"
              sx={{ p: 2, height: '100%' }}
            >
              <Stack spacing={1.5} sx={{ height: '100%' }}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Box sx={{ color: item.status === 'ready' ? 'primary.main' : 'warning.main', display: 'flex' }}>
                    {item.icon}
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle1" fontWeight={700}>{item.title}</Typography>
                    <Typography variant="caption" color="text.secondary">{item.owner}</Typography>
                  </Box>
                  <Chip
                    size="small"
                    label={item.statusText}
                    color={item.status === 'ready' ? 'success' : 'warning'}
                    variant="outlined"
                  />
                </Stack>
                <Typography variant="body2" color="text.secondary" sx={{ minHeight: 44 }}>{item.description}</Typography>
                {(() => {
                  const diagnostic = diagnostics[item.diagnosticKey]
                  const total = getPageTotal(diagnostic.data)
                  if (diagnostic.isFetching) {
                    return (
                      <Chip
                        label="诊断中"
                        size="small"
                        color="info"
                        variant="outlined"
                        data-testid="content-library-diagnostic-chip"
                        data-diagnostic-status="checking"
                        data-diagnostic-endpoint={item.endpoints[0]}
                        sx={{ alignSelf: 'flex-start' }}
                      />
                    )
                  }
                  if (diagnostic.isError) {
                    return (
                      <Alert
                        severity="warning"
                        data-testid="content-library-diagnostic-error"
                        data-diagnostic-status="source-error"
                        data-diagnostic-endpoint={item.endpoints[0]}
                        data-no-fake-total="true"
                        sx={{ py: 0.5 }}
                      >
                        {item.endpoints[0]} 诊断失败：{getErrorMessage(diagnostic.error)}
                      </Alert>
                    )
                  }
                  return (
                    <Alert
                      severity="success"
                      data-testid="content-library-diagnostic-success"
                      data-diagnostic-status="ready"
                      data-diagnostic-endpoint={item.endpoints[0]}
                      data-total={total ?? ''}
                      sx={{ py: 0.5 }}
                    >
                      接口可用{total !== undefined ? ` · 当前总量 ${total}` : ' · 已返回数据'}
                    </Alert>
                  )
                })()}
                <Stack direction="row" spacing={0.5} flexWrap="wrap">
                  {item.endpoints.map(endpoint => (
                    <Chip
                      key={endpoint}
                      label={endpoint}
                      size="small"
                      variant="outlined"
                      data-testid="content-library-endpoint-chip"
                      data-endpoint={endpoint}
                      sx={{ fontFamily: 'monospace' }}
                    />
                  ))}
                </Stack>
                <Box sx={{ flex: 1 }} />
                <Button
                  variant="contained"
                  data-testid="content-library-route-button"
                  data-route={item.route}
                  data-owner={item.owner}
                  data-contract-action="route-to-workbench"
                  onClick={() => navigate(item.route)}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  进入工作台
                </Button>
              </Stack>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
