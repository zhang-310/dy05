import { useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box, Typography, Card, CardContent, Grid, Chip,
  Button, CircularProgress, Alert, Table, TableBody,
  TableCell, TableHead, TableRow, Stack,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'
import LinkIcon from '@mui/icons-material/Link'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { douyinApi, type DyAccount, type DyAccountStats } from '@/api/douyin'

const ACCOUNT_DETAIL_ROUTE = '/talent/douyin/accounts/:id'
const ACCOUNT_ENDPOINTS = {
  get: '/douyin/account/get',
  statistics: '/douyin/account/statistics',
  tokenStatus: '/douyin/oauth/token-status',
  oauthUrl: '/douyin/oauth/auth-url',
} as const
const ACCOUNT_READY_ENDPOINTS = Object.values(ACCOUNT_ENDPOINTS).join('|')
const ACCOUNT_UNSUPPORTED_ENDPOINTS = [
  '/douyin/account/mock',
  '/douyin/account/local-get',
  '/douyin/account/local-statistics',
  '/douyin/account/local-token-status',
  '/douyin/oauth/local-auth-url',
  '/douyin/account/browser-scrape',
  '/douyin/video/search',
  '/douyin/video/sync',
  '/douyin/fan-profile/get',
  '/douyin/fan-profile/sync',
].join('|')

function formatNum(n: unknown): string {
  if (n == null || n === '') return '-'
  const num = Number(n)
  if (Number.isNaN(num)) return '-'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return String(num)
}

function safeText(value?: string | number | null) {
  return value == null || value === '' ? '-' : String(value)
}

export default function DouyinAccountDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [oauthError, setOauthError] = useState<string | null>(null)
  const accountId = Number(id)
  const validId = Number.isFinite(accountId)

  const accountQuery = useQuery({
    queryKey: ['douyin-account-detail', accountId],
    queryFn: () => douyinApi.accountGet(accountId),
    enabled: validId,
  })
  const statsQuery = useQuery({
    queryKey: ['douyin-account-stats', accountId],
    queryFn: () => douyinApi.accountStats(accountId),
    enabled: validId,
  })
  const tokenQuery = useQuery({
    queryKey: ['douyin-account-token-status', accountId],
    queryFn: () => douyinApi.tokenStatus(accountId),
    enabled: validId,
  })

  const account = accountQuery.data as DyAccount | undefined
  const stats = statsQuery.data as DyAccountStats | undefined
  const context = `route=${ACCOUNT_DETAIL_ROUTE}; accountPk=${accountId}; accountName=${account?.accountName ?? '未知账号'}; douyinAccount=${account?.accountId ?? '-'}`
  const errorMessage = accountQuery.error instanceof Error
    ? `${ACCOUNT_ENDPOINTS.get} 账号详情加载失败：${accountQuery.error.message}（${context}）`
    : statsQuery.error instanceof Error
      ? `${ACCOUNT_ENDPOINTS.statistics} 账号统计加载失败：${statsQuery.error.message}（${context}）`
      : `${ACCOUNT_ENDPOINTS.get}|${ACCOUNT_ENDPOINTS.statistics} 账号详情加载失败（${context}）`

  const statusChip = useMemo(() => {
    if (!tokenQuery.data) return <Chip label="未知" color="default" size="small" />
    const color = tokenQuery.data.status === 'valid' ? 'success' : tokenQuery.data.status === 'expired' ? 'error' : 'warning'
    const label = tokenQuery.data.status === 'valid' ? '有效' : tokenQuery.data.status === 'expired' ? '已过期' : '刷新中'
    return <Chip label={label} color={color} size="small" />
  }, [tokenQuery.data])

  if (!validId) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">无效的账号 ID</Alert>
        <Button sx={{ mt: 2 }} startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>返回</Button>
      </Box>
    )
  }

  if (accountQuery.isLoading || statsQuery.isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (accountQuery.isError || statsQuery.isError) {
    return (
      <Box
        data-testid="douyin-account-detail-error"
        data-ready-endpoints={ACCOUNT_READY_ENDPOINTS}
        data-unsupported-endpoints={ACCOUNT_UNSUPPORTED_ENDPOINTS}
        data-no-local-account-fallback="true"
        data-no-static-statistics-fallback="true"
        sx={{ p: 3 }}
      >
        <Alert
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => { void accountQuery.refetch(); void statsQuery.refetch(); void tokenQuery.refetch() }}>重试</Button>}
        >
          {errorMessage}
        </Alert>
        <Button sx={{ mt: 2 }} startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>返回</Button>
      </Box>
    )
  }

  return (
    <Box
      data-testid="douyin-account-detail-page"
      data-ready-endpoints={ACCOUNT_READY_ENDPOINTS}
      data-unsupported-endpoints={ACCOUNT_UNSUPPORTED_ENDPOINTS}
      data-readonly-detail-page="true"
      data-no-local-account-fallback="true"
      data-no-static-statistics-fallback="true"
      data-no-browser-direct-scrape="true"
      sx={{ p: 3 }}
    >
      <PageHeader
        title={account?.accountName ?? '账号详情'}
        subtitle={account ? `${safeText(account.accountId)} · 粉丝 ${formatNum(account.fanCount)}` : ''}
        breadcrumbs={[{ label: '抖音运营' }, { label: '账号管理' }, { label: '账号详情' }]}
        actions={
          <Stack direction="row" spacing={1}>
            <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>返回列表</Button>
            <Button startIcon={<RefreshIcon />} onClick={() => { void accountQuery.refetch(); void statsQuery.refetch(); void tokenQuery.refetch() }} disabled={accountQuery.isFetching || statsQuery.isFetching || tokenQuery.isFetching}>
              刷新
            </Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="douyin-account-detail-boundary-contract"
        data-source-endpoints={`${ACCOUNT_ENDPOINTS.get}|${ACCOUNT_ENDPOINTS.statistics}`}
        data-oauth-endpoints={`${ACCOUNT_ENDPOINTS.tokenStatus}|${ACCOUNT_ENDPOINTS.oauthUrl}`}
        data-readonly-detail-page="true"
        data-no-video-prefetch="true"
        data-no-browser-direct-scrape="true"
        sx={{ mt: 2 }}
      >
        账号详情页只读展示账号、统计和 OAuth 生命周期；不会在详情页同步视频、直连浏览器抓取抖音，也不会在统计失败时补静态指标。
      </Alert>

      <Grid container spacing={3} sx={{ mt: 1 }}>
        {account && (
          <Grid item xs={12} md={6}>
            <Card
              variant="outlined"
              data-testid="douyin-account-basic-card"
              data-source-endpoint={ACCOUNT_ENDPOINTS.get}
              data-no-local-account-fallback="true"
            >
              <CardContent>
                <Typography variant="h6" gutterBottom>基本信息</Typography>
                {[
                  { label: '账号名称', value: account.accountName },
                  { label: '抖音号', value: account.accountId },
                  { label: '粉丝数', value: formatNum(account.fanCount) },
                  { label: '状态', value: <Chip label={account.status === 1 ? '正常' : '停用'} size="small" color={account.status === 1 ? 'success' : 'error'} /> },
                  { label: '创建时间', value: safeText(account.createTime) },
                ].map(row => (
                  <Box key={row.label} sx={{ display: 'flex', py: 0.75, borderBottom: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="body2" color="text.secondary" sx={{ width: 100, flexShrink: 0 }}>{row.label}</Typography>
                    <Typography variant="body2">{row.value}</Typography>
                  </Box>
                ))}
              </CardContent>
            </Card>
          </Grid>
        )}

        {stats && (
          <Grid item xs={12}>
            <Card
              variant="outlined"
              data-testid="douyin-account-stats-card"
              data-source-endpoint={ACCOUNT_ENDPOINTS.statistics}
              data-no-static-statistics-fallback="true"
              data-no-client-statistics-synthesis="true"
            >
              <CardContent>
                <Typography variant="h6" gutterBottom>数据统计</Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>视频总数</TableCell>
                      <TableCell>总播放</TableCell>
                      <TableCell>总点赞</TableCell>
                      <TableCell>总分享</TableCell>
                      <TableCell>总评论</TableCell>
                      <TableCell>平均播放/条</TableCell>
                      <TableCell>平均点赞/条</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <TableRow>
                      <TableCell>{formatNum(stats.totalVideos)}</TableCell>
                      <TableCell>{formatNum(stats.totalViews)}</TableCell>
                      <TableCell>{formatNum(stats.totalLikes)}</TableCell>
                      <TableCell>{formatNum(stats.totalShares)}</TableCell>
                      <TableCell>{formatNum(stats.totalComments)}</TableCell>
                      <TableCell>{stats.avgViewsPerVideo?.toFixed(0) ?? '-'}</TableCell>
                      <TableCell>{stats.avgLikesPerVideo?.toFixed(0) ?? '-'}</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
        )}

        <Grid item xs={12}>
          <Card
            variant="outlined"
            data-testid="douyin-account-token-contract"
            data-source-endpoint={ACCOUNT_ENDPOINTS.tokenStatus}
            data-token-error-non-blocking="true"
            data-no-local-token-fallback="true"
          >
            <CardContent>
              <Stack spacing={1.5}>
                <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="h6">Token 状态</Typography>
                  {tokenQuery.isLoading ? <CircularProgress size={20} /> : statusChip}
                </Stack>
                <Typography variant="body2" color="text.secondary">
                  {tokenQuery.data?.expireTime ? `过期时间：${tokenQuery.data.expireTime}` : '当前未返回 Token 过期时间'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {tokenQuery.data?.daysLeft != null ? `剩余天数：${tokenQuery.data.daysLeft} 天` : '剩余天数暂不可用'}
                </Typography>
                {tokenQuery.isError && (
                  <Alert
                    severity="warning"
                    data-testid="douyin-account-token-error"
                    data-token-error-non-blocking="true"
                    data-no-local-token-fallback="true"
                    action={<Button color="inherit" size="small" onClick={() => void tokenQuery.refetch()}>重试</Button>}
                  >
                    {ACCOUNT_ENDPOINTS.tokenStatus} Token 状态暂不可用：{tokenQuery.error instanceof Error ? tokenQuery.error.message : '查询失败'}（{context}）。页面会继续展示账号与统计信息，但授权生命周期需要在账号列表页或 OAuth 页面重新核验。
                  </Alert>
                )}
                {oauthError && (
                  <Alert
                    severity="error"
                    data-testid="douyin-account-oauth-error"
                    data-no-local-oauth-url-fallback="true"
                    data-input-retained="true"
                    onClose={() => setOauthError(null)}
                  >
                    {oauthError}
                  </Alert>
                )}
                <Alert severity="info">
                  账号详情和统计来自 `/douyin/account/get|statistics`；Token 生命周期来自独立 OAuth 接口，失败时不阻断账号基础信息。
                </Alert>
                <Stack direction="row" spacing={1}>
                  <Button variant="outlined" startIcon={<LinkIcon />} onClick={async () => {
                    setOauthError(null)
                    try {
                      const r = await douyinApi.oauthUrl(accountId)
                      if (!r.authUrl) throw new Error('后端未返回 authUrl')
                      window.open(r.authUrl, '_blank')
                    } catch (e) {
                      const message = e instanceof Error ? e.message : 'OAuth 服务异常'
                      setOauthError(`${ACCOUNT_ENDPOINTS.oauthUrl} 获取授权链接失败：${message}（${context}）`)
                    }
                  }}>
                    重新授权
                  </Button>
                  <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => void tokenQuery.refetch()}>
                    刷新 Token 状态
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
