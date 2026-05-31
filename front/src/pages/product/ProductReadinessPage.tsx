import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Chip, Stack, LinearProgress,
  List, ListItem, ListItemIcon, ListItemText, Grid, Alert,
  Button, FormControl, InputLabel, Select, Skeleton,
} from '@mui/material'
import {
  CheckCircle as OkIcon, Warning as WarnIcon, Error as ErrorIcon,
} from '@mui/icons-material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery } from '@tanstack/react-query'
import { ErrorAlert, PageHeader } from '@/components/base'
import { productApi, type ProductReadinessItem } from '@/api/product'
import { getErrorMessage } from '@/utils/errorHandler'

const READINESS_READY_ENDPOINTS = ['/product/search', '/product/readiness']
const READINESS_CONTEXT_ENDPOINTS = [
  '/product/script/search',
  '/product/script/generate',
  '/live/product/batch-add',
  '/short-video/project/save',
  '/product/extract-from-link',
]
const READINESS_UNSUPPORTED_ACTIONS = [
  'auto-generate-product-script',
  'auto-add-live-session',
  'auto-export-shortvideo',
  'static-readiness-fallback',
  'local-product-fallback',
]

export default function ProductReadinessPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const productIdFromUrl = Number(searchParams.get('productId') ?? '')
  const initialProductId = Number.isFinite(productIdFromUrl) && productIdFromUrl > 0 ? productIdFromUrl : ''
  const [selectedProductId, setSelectedProductId] = useState<number | ''>(initialProductId)

  const {
    data: products,
    isFetching: productsFetching,
    isError: productsIsError,
    error: productsError,
    refetch: refetchProducts,
  } = useQuery({
    queryKey: ['product-readiness-products'],
    queryFn: () => productApi.list({ page: 0, rows: 200 }),
  })

  const { data: result, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['product-readiness', selectedProductId],
    queryFn: () => productApi.readiness(Number(selectedProductId)),
    enabled: selectedProductId !== '',
  })

  const productList = products?.list ?? []
  const selectedProduct = useMemo(
    () => productList.find((item) => item.id === selectedProductId),
    [productList, selectedProductId],
  )
  const items = result?.items ?? []
  const failedItems = items.filter((item) => item.status === 'error')
  const warningItems = items.filter((item) => item.status === 'warn')

  const statusIcon = (s: ProductReadinessItem['status']) => {
    if (s === 'ok') return <OkIcon color="success" />
    if (s === 'warn') return <WarnIcon color="warning" />
    return <ErrorIcon color="error" />
  }

  const statusColor = (s: ProductReadinessItem['status']): 'success' | 'warning' | 'error' => {
    if (s === 'ok') return 'success'
    if (s === 'warn') return 'warning'
    return 'error'
  }

  const readinessMessage = result
    ? result.readyForLive
      ? '商品准备度达标，可进入上播或导出话术链路。'
      : '商品仍有阻塞项，先处理检测项里的红色告警。'
    : selectedProductId === ''
      ? '请选择商品后执行准备度检测。'
      : '读取准备度结果中。'
  const readinessError = `${getErrorMessage(error)}。请检查 /product/readiness、登录态和商品数据权限。`
  const productsErrorMessage = `${getErrorMessage(productsError)}。请检查 /product/search、登录态和商品数据权限。`

  const handleProductChange = (value: number | '') => {
    setSelectedProductId(value)
    const next = new URLSearchParams(searchParams)
    if (value === '') next.delete('productId')
    else next.set('productId', String(value))
    setSearchParams(next, { replace: true })
  }

  return (
    <Box
      data-testid="product-readiness-workbench"
      data-contract-scope="product-readiness"
      data-ready-endpoints={READINESS_READY_ENDPOINTS.join('|')}
      data-context-endpoints={READINESS_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={READINESS_UNSUPPORTED_ACTIONS.join('|')}
      data-selected-product-id={selectedProductId || ''}
      data-selected-from-url={initialProductId !== '' ? 'true' : 'false'}
      data-product-count={productList.length}
      data-has-result={result ? 'true' : 'false'}
      data-ready-for-live={result ? String(result.readyForLive) : ''}
      data-overall-score={result?.overallScore ?? ''}
      data-item-count={items.length}
      data-blocking-count={failedItems.length}
      data-warning-count={warningItems.length}
      data-no-static-readiness-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="商品上播准备度"
        breadcrumbs={[{ label: '商品' }, { label: '上播准备度' }]}
        subtitle="检测商品基础信息、主图、价格库存、话术和卖点是否满足上播条件。"
        actions={(
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" size="small" startIcon={<RefreshIcon />} onClick={() => { void refetchProducts(); if (selectedProductId !== '') void refetch() }} disabled={isLoading || productsFetching}>
              刷新
            </Button>
          </Stack>
        )}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="product-readiness-contract-alert"
        data-contract-source="/product/search|/product/readiness"
        data-no-auto-script-generation="true"
        data-no-live-session-mutation="true"
        data-no-shortvideo-export="true"
        data-no-local-product-fallback="true"
      >
        真实接口：<code>/product/search</code> 用于商品选择，<code>/product/readiness</code> 用于准备度检测。
        本页支持从商品列表深链进入，也支持在页面内直接选择商品后检测。
      </Alert>

      {productsIsError && (
        <Box
          data-testid="product-readiness-products-error"
          data-contract-source="/product/search"
          data-no-local-product-fallback="true"
        >
          <ErrorAlert
            severity="warning"
            title="商品列表加载失败"
            message={productsErrorMessage}
            onRetry={() => refetchProducts()}
          />
        </Box>
      )}

      <Card
        variant="outlined"
        data-testid="product-readiness-selector-card"
        data-contract-source="/product/search"
        data-selected-product-id={selectedProductId || ''}
        data-product-count={productList.length}
      >
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={6}>
              <FormControl fullWidth size="small">
                <InputLabel id="product-readiness-select-label" htmlFor="product-readiness-select">选择商品</InputLabel>
                <Select
                  native
                  labelId="product-readiness-select-label"
                  id="product-readiness-select"
                  label="选择商品"
                  value={selectedProductId}
                  onChange={(event) => handleProductChange(event.target.value === '' ? '' : Number(event.target.value))}
                  disabled={productsFetching}
                >
                  <option value="">请选择商品</option>
                  {productList.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.productName || `商品 #${product.id}`}
                    </option>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'stretch', sm: 'center' }}>
                <Button
                  variant="contained"
                  data-testid="product-readiness-run-action"
                  data-contract-source="/product/readiness"
                  onClick={() => void refetch()}
                  disabled={selectedProductId === '' || isLoading}
                >
                  执行检测
                </Button>
                <Typography variant="body2" color="text.secondary">
                  {selectedProduct ? `当前商品：${selectedProduct.productName}` : selectedProductId !== '' ? `当前商品 ID：${selectedProductId}` : '尚未选择商品'}
                </Typography>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {productsFetching && !products && <Skeleton variant="rounded" height={64} />}
      {isLoading && <LinearProgress />}
      {isError && (
        <Alert
          severity="error"
          data-testid="product-readiness-error"
          data-contract-source="/product/readiness"
          data-selected-product-id={selectedProductId || ''}
          data-no-static-readiness-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          商品准备度加载失败：{readinessError}
        </Alert>
      )}
      <Alert
        severity={result?.readyForLive ? 'success' : selectedProductId === '' ? 'info' : 'warning'}
        data-testid="product-readiness-status-alert"
        data-contract-source={result ? '/product/readiness' : 'local-selection-state'}
        data-selected-product-id={selectedProductId || ''}
      >
        {readinessMessage}
      </Alert>

      {!!result && (
        <>
          <Grid container spacing={2}>
            {[
              { label: '检测商品', value: result.productName || `#${result.productId}`, color: 'text.primary' },
              { label: '综合得分', value: `${result.overallScore}`, color: result.overallScore >= 80 ? 'success.main' : result.overallScore >= 60 ? 'warning.main' : 'error.main' },
              { label: '阻塞项', value: String(failedItems.length), color: failedItems.length > 0 ? 'error.main' : 'success.main' },
              { label: '提醒项', value: String(warningItems.length), color: warningItems.length > 0 ? 'warning.main' : 'success.main' },
            ].map((item) => (
              <Grid item xs={6} md={3} key={item.label}>
                <Card
                  variant="outlined"
                  data-testid="product-readiness-kpi-card"
                  data-kpi-label={item.label}
                  data-contract-source={item.label === '检测商品' ? '/product/readiness' : 'local-derived-from-readiness'}
                >
                  <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                    <Typography variant="h6" fontWeight={700} color={item.color} noWrap>{item.value}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>

          <Card
            data-testid="product-readiness-result-card"
            data-contract-source="/product/readiness"
            data-product-id={result.productId}
            data-ready-for-live={String(result.readyForLive)}
            data-overall-score={result.overallScore}
          >
            <CardContent>
              <Grid container spacing={3} alignItems="center">
                <Grid item xs={12} sm={4}>
                  <Box sx={{ textAlign: 'center' }}>
                    <Typography variant="h2" fontWeight={700} color={result.overallScore >= 80 ? 'success.main' : result.overallScore >= 60 ? 'warning.main' : 'error.main'}>
                      {result.overallScore}
                    </Typography>
                    <Typography variant="subtitle2" color="text.secondary">综合得分</Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Box sx={{ textAlign: 'center' }}>
                    <Chip
                      size="medium"
                      label={result.readyForLive ? '可上播' : '待完善'}
                      color={result.readyForLive ? 'success' : 'warning'}
                      sx={{ fontSize: 16, px: 2, py: 1, height: 40 }}
                    />
                    <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 1 }}>上播状态</Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <LinearProgress
                    variant="determinate"
                    value={result.overallScore}
                    color={result.overallScore >= 80 ? 'success' : result.overallScore >= 60 ? 'warning' : 'error'}
                    sx={{ height: 12, borderRadius: 6 }}
                  />
                  <Typography variant="caption" color="text.secondary">{result.overallScore}% 准备完成</Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          <Card
            variant="outlined"
            data-testid="product-readiness-items-card"
            data-contract-source="/product/readiness"
            data-item-count={items.length}
            data-no-static-readiness-fallback="true"
          >
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>检测项明细</Typography>
              <List disablePadding>
                {items.map((item) => (
                  <ListItem
                    key={item.dimension}
                    disablePadding
                    data-testid="product-readiness-check-item"
                    data-contract-source="/product/readiness"
                    data-dimension={item.dimension}
                    data-status={item.status}
                    data-score={item.score}
                    sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}
                  >
                    <ListItemIcon sx={{ minWidth: 36 }}>{statusIcon(item.status)}</ListItemIcon>
                    <ListItemText
                      primary={
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Typography variant="body2" fontWeight={500}>{item.dimension}</Typography>
                          <Chip size="small" label={`${item.score}分`} color={statusColor(item.status)} />
                        </Stack>
                      }
                      secondary={item.message}
                    />
                  </ListItem>
                ))}
              </List>
              {items.length === 0 && (
                <Alert
                  severity="warning"
                  data-testid="product-readiness-empty-items"
                  data-contract-source="/product/readiness"
                  data-no-static-readiness-fallback="true"
                  sx={{ mt: 2 }}
                >
                  后端未返回检测项。请检查 ProductReadinessService 是否生成基础信息、主图、价格库存、话术和卖点检测结果。
                </Alert>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </Box>
  )
}
