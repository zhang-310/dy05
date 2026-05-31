import { Alert, Box, Button, Chip, Grid, Paper, Stack, Typography } from '@mui/material'
import ApprovalIcon from '@mui/icons-material/Approval'
import ArticleIcon from '@mui/icons-material/Article'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/base'

interface CopyCapability {
  title: string
  route: string
  description: string
  endpoints: string[]
  available: string[]
  degraded: string[]
  icon: React.ReactNode
}

const COPY_UNSUPPORTED_ACTIONS = [
  'csv-export',
  'batch-tag',
  'usage-detail',
  'approval-stats',
  'approval-revise',
  'batch-template-delete',
  'batch-ai-save',
]

const COPY_UNSUPPORTED_ENDPOINTS = [
  '/copy/library/export',
  '/copy/library/batch-tag',
  '/copy/library/usage',
  '/copy/approval/stats',
  '/copy/approval/revise',
  '/copy/template/batch-delete',
  '/copy/ai/batch-save',
]

const capabilities: CopyCapability[] = [
  {
    title: '文案库',
    route: '/org/copy/library',
    description: '维护可复用文案，支持搜索、新建、编辑、删除、提交审批和使用次数递增。',
    endpoints: ['/copy/library/search', '/copy/library/save', '/copy/library/delete'],
    available: ['列表分页', '新增编辑', '删除', '提交审批'],
    degraded: ['CSV 导出', '批量打标签', '使用明细'],
    icon: <ContentCopyIcon />,
  },
  {
    title: '文案审批',
    route: '/org/copy/approval',
    description: '按 copy_approval 状态流转审批记录，管理员可通过或拒绝待审核项。',
    endpoints: ['/copy/approval/search', '/copy/approval/get', '/copy/approval/save'],
    available: ['审批看板', '通过', '拒绝', '分页'],
    degraded: ['在线改稿', '审批统计接口'],
    icon: <ApprovalIcon />,
  },
  {
    title: '文案模板',
    route: '/org/copy/templates',
    description: '维护变量化文案模板，模板详情和删除按请求体 `{ id }` 调用后端。',
    endpoints: ['/copy/template/search', '/copy/template/save', '/copy/template/delete'],
    available: ['模板分页', '新增编辑', '预览', '删除'],
    degraded: ['批量模板事务删除'],
    icon: <LibraryBooksIcon />,
  },
  {
    title: 'AI 生成文案',
    route: '/org/copy/library',
    description: '文案库内调用 `/copy/ai/generate`，由短视频域 AI 服务按 `copy_processing` 任务配置生成候选文案。',
    endpoints: ['/copy/ai/generate'],
    available: ['AI 候选生成', '保存入库', '失败提示'],
    degraded: ['批量一键入库'],
    icon: <AutoFixHighIcon />,
  },
]

const COPY_READY_ENDPOINTS = capabilities.flatMap(item => item.endpoints)
const contractValue = (items: string[]) => items.join(',')

export default function CopyPage() {
  const navigate = useNavigate()
  const degradedCount = capabilities.reduce((sum, item) => sum + item.degraded.length, 0)
  const endpointCount = capabilities.reduce((sum, item) => sum + item.endpoints.length, 0)
  const availableCount = capabilities.reduce((sum, item) => sum + item.available.length, 0)

  return (
    <Box
      sx={{ p: 3 }}
      data-testid="copy-gateway-workbench"
      data-contract-scope="copy-gateway"
      data-ready-endpoints={contractValue(COPY_READY_ENDPOINTS)}
      data-unsupported-actions={contractValue(COPY_UNSUPPORTED_ACTIONS)}
      data-unsupported-endpoints={contractValue(COPY_UNSUPPORTED_ENDPOINTS)}
      data-capability-count={capabilities.length}
      data-endpoint-count={endpointCount}
      data-degraded-count={degradedCount}
      data-no-inline-write="true"
      data-no-page-api-request="true"
    >
      <PageHeader
        title="文案管理"
        subtitle="文案域入口页只展示能力地图和降级边界，具体操作进入独立工作台。"
        breadcrumbs={[{ label: '文案运营' }, { label: '文案管理' }]}
      />

      <Alert
        severity="info"
        variant="outlined"
        sx={{ mb: 2 }}
        data-testid="copy-gateway-shell-contract"
        data-contract-status="gateway-only"
        data-ready-endpoints={contractValue(COPY_READY_ENDPOINTS)}
        data-unsupported-actions={contractValue(COPY_UNSUPPORTED_ACTIONS)}
        data-no-inline-write="true"
        data-no-page-api-request="true"
      >
        本页不再直接嵌套文案库、审批和模板完整页面，避免重复页头与多层 Tab 影响操作；入口卡片会跳转到对应真实路由。
      </Alert>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          { label: '工作台入口', value: capabilities.length, color: 'primary.main', metric: 'capability-count' },
          { label: '真实接口', value: endpointCount, color: 'success.main', metric: 'ready-endpoint-count' },
          { label: '可用能力', value: availableCount, color: 'info.main', metric: 'available-action-count' },
          { label: '明确降级', value: degradedCount, color: 'warning.main', metric: 'degraded-action-count' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper
              variant="outlined"
              sx={{ p: 1.5 }}
              data-testid="copy-gateway-summary-card"
              data-summary-metric={item.metric}
              data-contract-status="local-derived"
            >
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h4" fontWeight={700} color={item.color}>{item.value}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        {capabilities.map(item => (
          <Grid item xs={12} md={6} key={item.title}>
            <Paper
              variant="outlined"
              sx={{ p: 2, height: '100%' }}
              data-testid="copy-gateway-capability-card"
              data-capability-title={item.title}
              data-route={item.route}
              data-capability-endpoints={contractValue(item.endpoints)}
              data-available-actions={contractValue(item.available)}
              data-degraded-actions={contractValue(item.degraded)}
              data-contract-status="ready-with-degraded-actions"
              data-inline-operation="unsupported"
              data-no-inline-write="true"
            >
              <Stack spacing={1.5} sx={{ height: '100%' }}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Box sx={{ color: item.available.length > 0 ? 'primary.main' : 'warning.main', display: 'flex' }}>
                    {item.icon}
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="subtitle1" fontWeight={700}>{item.title}</Typography>
                    <Typography variant="caption" color="text.secondary">{item.description}</Typography>
                  </Box>
                </Stack>

                <Stack direction="row" spacing={0.5} flexWrap="wrap">
                  {item.endpoints.map(endpoint => (
                    <Chip
                      key={endpoint}
                      label={endpoint}
                      size="small"
                      variant="outlined"
                      sx={{ fontFamily: 'monospace' }}
                      data-testid="copy-gateway-endpoint-chip"
                      data-endpoint={endpoint}
                      data-contract-status="ready"
                    />
                  ))}
                </Stack>

                {item.available.length > 0 && (
                  <Box>
                    <Typography variant="caption" color="text.secondary">可用能力</Typography>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }}>
                      {item.available.map(capability => (
                        <Chip
                          key={capability}
                          label={capability}
                          size="small"
                          color="success"
                          variant="outlined"
                          data-testid="copy-gateway-available-chip"
                          data-action={capability}
                          data-contract-status="ready"
                        />
                      ))}
                    </Stack>
                  </Box>
                )}

                {item.degraded.length > 0 && (
                  <Box>
                    <Typography variant="caption" color="text.secondary">降级边界</Typography>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }}>
                      {item.degraded.map(capability => (
                        <Chip
                          key={capability}
                          label={capability}
                          size="small"
                          color="warning"
                          variant="outlined"
                          data-testid="copy-gateway-degraded-chip"
                          data-action={capability}
                          data-contract-status="degraded"
                        />
                      ))}
                    </Stack>
                  </Box>
                )}

                <Box sx={{ flex: 1 }} />
                <Stack direction="row" spacing={1}>
                  <Button
                    variant="contained"
                    startIcon={<ArticleIcon />}
                    onClick={() => navigate(item.route)}
                    data-testid="copy-gateway-route-button"
                    data-route={item.route}
                    data-contract-action="route-to-workbench"
                  >
                    进入工作台
                  </Button>
                </Stack>
              </Stack>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
