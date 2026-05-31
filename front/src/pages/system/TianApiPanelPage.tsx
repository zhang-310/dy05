import { useMemo } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid, Chip, Button,
  CircularProgress, Alert, Divider, Link,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQueries, useQuery } from '@tanstack/react-query'
import { tianapi, type HotItem } from '@/api/tianapi'
import { PageHeader, EmptyState, ErrorAlert } from '@/components/base'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'

interface TianApiStatus {
  enabled?: boolean
}

const SOURCES = [
  { label: '抖音热榜', key: 'douyin' as const, fn: () => tianapi.hotDouyin() },
  { label: '头条热榜', key: 'toutiao' as const, fn: () => tianapi.hotToutiao() },
  { label: '微博热搜', key: 'weibo' as const, fn: () => tianapi.hotWeibo() },
  { label: '全网热点', key: 'network' as const, fn: () => tianapi.hotNetwork() },
]
const TIANAPI_ENDPOINTS = [
  '/tianapi/status',
  '/tianapi/hot/douyin',
  '/tianapi/hot/toutiao',
  '/tianapi/hot/weibo',
  '/tianapi/hot/network',
].join('|')
const TIANAPI_UNSUPPORTED_ENDPOINTS = [
  '/tianapi/quota',
  '/tianapi/hot/mock',
  '/tianapi/hot/all',
  '/tianapi/config/get-secret',
  '/tianapi/config/save-key',
].join('|')
const TIANAPI_UNSUPPORTED_ACTIONS = [
  'local-hot-list-fallback',
  'synthetic-quota-display',
  'cross-source-error-propagation',
  'mock-hot-item-injection',
  'plaintext-key-display',
].join('|')

interface HotResult {
  data?: HotItem[]
  isLoading: boolean
  isFetching: boolean
  isError: boolean
  error: unknown
  refetch: () => unknown
}

function HotList({ source, result }: { source: typeof SOURCES[0]; result: HotResult }) {
  const data = normalizeRows<HotItem>(result.data)
  const isLoading = result.isLoading
  const isError = result.isError
  const error = result.error
  const refetch = result.refetch

  return (
    <Card
      variant="outlined"
      data-testid={`tianapi-hot-source-${source.key}`}
      data-contract-source={`/tianapi/hot/${source.key}`}
      data-source-key={source.key}
      data-row-count={data.length}
      data-loading={String(isLoading)}
      data-error={String(isError)}
      data-no-local-hot-list-fallback="true"
      data-error-isolated="true"
      data-no-mock-hot-item-injection="true"
    >
      <CardContent sx={{ pb: '12px !important' }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="subtitle2" fontWeight={600}>{source.label}</Typography>
            <Chip
              size="small"
              label={isError ? '异常' : data.length > 0 ? '有数据' : isLoading ? '加载中' : '空数据'}
              color={isError ? 'error' : data.length > 0 ? 'success' : isLoading ? 'default' : 'warning'}
              variant="outlined"
            />
          </Stack>
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            disabled={result.isFetching}
            data-testid={`tianapi-hot-source-refresh-${source.key}`}
            data-contract-source={`/tianapi/hot/${source.key}`}
            data-source-key={source.key}
            data-refresh-scope="single-source"
            data-error-isolated="true"
            onClick={() => refetch()}
          >
            刷新
          </Button>
        </Stack>
        {isLoading && <CircularProgress size={20} sx={{ mx: 'auto', display: 'block' }} />}
        {isError && (
          <Box
            data-testid={`tianapi-hot-source-error-${source.key}`}
            data-contract-source={`/tianapi/hot/${source.key}`}
            data-source-key={source.key}
            data-error-isolated="true"
            data-no-local-hot-list-fallback="true"
            data-no-mock-hot-item-injection="true"
          >
            <ErrorAlert
              severity="warning"
              title={`${source.label}加载失败`}
              message={`${getErrorMessage(error)}。请检查 TianAPI key、配额和 /tianapi/hot/${source.key}。`}
              onRetry={() => refetch()}
            />
          </Box>
        )}
        {!isLoading && !isError && data.length === 0 && (
          <Box
            data-testid={`tianapi-hot-source-empty-${source.key}`}
            data-contract-source={`/tianapi/hot/${source.key}`}
            data-no-local-hot-list-fallback="true"
            data-no-mock-hot-item-injection="true"
          >
            <EmptyState title="暂无热点" description="该来源当前未返回热点数据，可能是 TianAPI 未配置、配额耗尽或上游无数据。" />
          </Box>
        )}
        <Stack spacing={0.5}>
          {data.slice(0, 10).map((item, i) => (
            <Stack
              key={`${item.word}-${i}`}
              data-testid={`tianapi-hot-item-${source.key}`}
              data-contract-source={`/tianapi/hot/${source.key}`}
              data-source-key={source.key}
              data-rank={String(item.position ?? i + 1)}
              data-has-link={String(Boolean(item.link))}
              data-has-hot-index={String(item.hotIndex != null)}
              data-no-mock-hot-item-injection="true"
              direction="row"
              justifyContent="space-between"
              alignItems="center"
              sx={{ py: 0.25, borderBottom: '1px solid', borderColor: 'divider', gap: 1 }}
            >
              <Stack direction="row" spacing={1} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="caption" fontWeight={700}
                  color={i < 3 ? 'error.main' : 'text.secondary'} sx={{ minWidth: 20 }}>
                  {item.position ?? i + 1}
                </Typography>
                {item.link ? (
                  <Link
                    href={item.link}
                    target="_blank"
                    rel="noreferrer"
                    underline="hover"
                    variant="body2"
                    noWrap
                    data-testid={`tianapi-hot-item-link-${source.key}`}
                    data-contract-source={`/tianapi/hot/${source.key}`}
                    sx={{ flex: 1, minWidth: 0 }}
                  >
                    {item.word}
                  </Link>
                ) : (
                  <Typography
                    variant="body2"
                    noWrap
                    sx={{ flex: 1 }}
                    data-testid={`tianapi-hot-item-text-${source.key}`}
                    data-contract-source={`/tianapi/hot/${source.key}`}
                  >
                    {item.word}
                  </Typography>
                )}
              </Stack>
              <Stack direction="row" spacing={0.5} alignItems="center" sx={{ flexShrink: 0 }}>
                {(item.label ?? item.hotZh ?? item.source) && (
                  <Chip
                    size="small"
                    variant="outlined"
                    label={item.label ?? item.hotZh ?? item.source}
                    data-testid={`tianapi-hot-item-source-chip-${source.key}`}
                  />
                )}
                {item.hotIndex != null && (
                  <Typography variant="caption" color="text.secondary">
                    {Number(item.hotIndex).toLocaleString()}
                  </Typography>
                )}
              </Stack>
            </Stack>
          ))}
        </Stack>
      </CardContent>
    </Card>
  )
}

export default function TianApiPanelPage() {
  const { data: status, isLoading: statusLoading, isError: statusIsError, error: statusError, refetch: refetchStatus } = useQuery({
    queryKey: ['tianapi-status'],
    queryFn: () => tianapi.status(),
    refetchInterval: 60000,
  })

  const s: TianApiStatus = status ?? {}
  const hotResults = useQueries({
    queries: SOURCES.map(source => ({
      queryKey: ['tianapi-panel', source.key],
      queryFn: source.fn,
      refetchInterval: 5 * 60 * 1000,
    })),
  }) as HotResult[]

  const sourceStats = useMemo(() => {
    const failed = hotResults.filter(result => result.isError).length
    const loading = hotResults.filter(result => result.isLoading || result.isFetching).length
    const rowsBySource = hotResults.map(result => normalizeRows<HotItem>(result.data))
    const withData = rowsBySource.filter(rows => rows.length > 0).length
    const empty = hotResults.filter((result, index) => !result.isLoading && !result.isError && rowsBySource[index].length === 0).length
    const totalItems = rowsBySource.reduce((sum, rows) => sum + rows.length, 0)
    return { failed, loading, withData, empty, totalItems }
  }, [hotResults])

  const refreshAll = () => {
    void refetchStatus()
    hotResults.forEach(result => void result.refetch())
  }

  return (
    <Box
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
      data-testid="tianapi-panel-page-workbench"
      data-contract-scope="system-tianapi-hot-sources"
      data-ready-endpoints={TIANAPI_ENDPOINTS}
      data-unsupported-endpoints={TIANAPI_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={TIANAPI_UNSUPPORTED_ACTIONS}
      data-status-enabled={String(Boolean(s.enabled))}
      data-status-loading={String(statusLoading)}
      data-status-error={String(statusIsError)}
      data-source-failed-count={sourceStats.failed}
      data-source-with-data-count={sourceStats.withData}
      data-source-empty-count={sourceStats.empty}
      data-total-items={sourceStats.totalItems}
      data-no-local-hot-list-fallback="true"
      data-no-synthetic-quota-display="true"
      data-error-isolated="true"
      data-no-mock-hot-item-injection="true"
      data-no-plaintext-key-display="true"
    >
      <PageHeader
        title="天API 数据面板"
        subtitle="实时查看 TianAPI 热点接口状态和四类热榜数据；数据来自外部供应商，失败时按来源单独降级。"
        breadcrumbs={[{ label: '系统' }, { label: 'TianAPI' }]}
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={refreshAll}
            data-testid="tianapi-refresh-all"
            data-contract-source={TIANAPI_ENDPOINTS}
            data-refresh-scope="status-and-all-hot-sources"
            data-error-isolated="true"
          >
            刷新全部
          </Button>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="tianapi-source-contract"
        data-contract-source={TIANAPI_ENDPOINTS}
        data-unsupported-endpoints={TIANAPI_UNSUPPORTED_ENDPOINTS}
        data-no-local-hot-list-fallback="true"
        data-no-synthetic-quota-display="true"
        data-no-plaintext-key-display="true"
      >
        真实接口：<code>/tianapi/status</code>、<code>/tianapi/hot/douyin</code>、<code>/toutiao</code>、<code>/weibo</code>、<code>/network</code>。
        当前状态接口只返回是否已配置；配额和上游失败按各热榜来源单独展示，不伪造剩余额度。
      </Alert>

      <Card
        variant="outlined"
        data-testid="tianapi-status-contract"
        data-contract-source="/tianapi/status"
        data-status-enabled={String(Boolean(s.enabled))}
        data-no-synthetic-quota-display="true"
        data-no-plaintext-key-display="true"
      >
        <CardContent sx={{ py: 1.5 }}>
          <Stack direction="row" spacing={3} alignItems="center">
            <Typography variant="body2" color="text.secondary">API 状态</Typography>
            {statusLoading
              ? <CircularProgress size={16} />
              : <Chip label={s.enabled ? '已配置' : '未配置'} color={s.enabled ? 'success' : 'warning'} size="small" />}
            {!statusLoading && (
              <>
                <Divider orientation="vertical" flexItem />
                <Typography variant="body2" color="text.secondary">状态来源</Typography>
                <Typography variant="body2" fontWeight={700}>/tianapi/status.enabled</Typography>
              </>
            )}
          </Stack>
        </CardContent>
      </Card>

      {statusIsError && (
        <Box
          data-testid="tianapi-status-error"
          data-contract-source="/tianapi/status"
          data-no-synthetic-quota-display="true"
          data-no-plaintext-key-display="true"
        >
          <ErrorAlert
            title="TianAPI 状态加载失败"
            message={`${getErrorMessage(statusError)}。请检查 /tianapi/status、TianAPI 配置和登录态。`}
            onRetry={() => refetchStatus()}
          />
        </Box>
      )}

      {s.enabled === false && !statusLoading && (
        <Alert
          severity="warning"
          data-testid="tianapi-disabled-warning"
          data-contract-source="/tianapi/status"
          data-error-isolated="true"
          data-no-plaintext-key-display="true"
        >
          TianAPI 未配置 API Key，热点数据会按来源加载失败并显示具体错误。
        </Alert>
      )}

      <Grid container spacing={1.5}>
        {[
          { label: '配置状态', value: statusLoading ? '加载中' : s.enabled ? '已配置' : '未配置', hint: '/tianapi/status.enabled' },
          { label: '有数据来源', value: sourceStats.withData, hint: `共 ${SOURCES.length} 个来源` },
          { label: '异常来源', value: sourceStats.failed, hint: sourceStats.loading > 0 ? `刷新中 ${sourceStats.loading}` : '按来源隔离失败' },
          { label: '热点条目', value: sourceStats.totalItems, hint: `空来源 ${sourceStats.empty}` },
        ].map(card => (
          <Grid item xs={6} md={3} key={card.label}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{card.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{card.value}</Typography>
                <Typography variant="caption" color="text.secondary">{card.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        {SOURCES.map((src, index) => (
          <Grid item xs={12} sm={6} key={src.key}>
            <HotList source={src} result={hotResults[index]} />
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
