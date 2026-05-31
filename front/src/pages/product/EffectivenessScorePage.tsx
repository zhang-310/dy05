import { useMemo, useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, FormControl, Grid, InputLabel,
  LinearProgress, MenuItem, Select, Stack, Typography, Chip,
} from '@mui/material'
import { useTheme } from '@mui/material/styles'
import RefreshIcon from '@mui/icons-material/Refresh'
import CalculateIcon from '@mui/icons-material/Calculate'
import type { GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import ReactECharts from 'echarts-for-react'
import { DataGridEmptyOverlay, ErrorAlert, PageHeader, StandardDataGrid } from '@/components/base'
import { productApi, type EffectivenessScoreItem } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { getErrorMessage } from '@/utils/errorHandler'

const EFFECTIVENESS_READY_ENDPOINTS = [
  '/product/search',
  '/product/script-effectiveness/ranking',
  '/product/script-effectiveness/recalculate',
]
const EFFECTIVENESS_CONTEXT_ENDPOINTS = [
  '/product/script-effectiveness/trend',
  '/product/script-effectiveness/compare',
  '/product/script-effectiveness/style-comparison',
  '/product/sales-history/search',
  '/product/script/generate',
]
const EFFECTIVENESS_UNSUPPORTED_ACTIONS = [
  'all-product-ranking',
  'server-export',
  'auto-generate-product-script',
  'sales-history-fallback',
  'static-ranking-fallback',
]

function ScoreBar({ value }: { value: number }) {
  const color = value >= 8 ? 'success' : value >= 5 ? 'warning' : 'error'
  return (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }}>
      <LinearProgress
        variant="determinate"
        value={Math.min(100, Math.max(0, value * 10))}
        color={color}
        sx={{ flex: 1, height: 8, borderRadius: 4 }}
      />
      <Typography variant="caption" fontWeight={700} sx={{ minWidth: 28 }}>{value.toFixed(1)}</Typography>
    </Stack>
  )
}

export default function EffectivenessScorePage() {
  const theme = useTheme()
  const toast = useToast()
  const qc = useQueryClient()
  const [productId, setProductId] = useState<number | ''>('')
  const [sortBy, setSortBy] = useState('score')
  const [page, setPage] = useState(0)

  const {
    data: products,
    isFetching: productsFetching,
    isError: productsIsError,
    error: productsError,
    refetch: refetchProducts,
  } = useQuery({
    queryKey: ['effectiveness-products'],
    queryFn: () => productApi.list({ page: 0, rows: 200 }),
  })

  const {
    data,
    isFetching,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['product-effectiveness', productId, sortBy, page],
    queryFn: () => productApi.effectivenessRanking({ productId: Number(productId), sortBy, page, rows: 20, topN: 0 }),
    enabled: productId !== '',
  })

  const recalculateMut = useMutation({
    mutationFn: () => productApi.effectivenessRecalculate(Number(productId)),
    onSuccess: (count) => {
      toast(`已重新计算 ${count} 个话术版本`, 'success')
      qc.invalidateQueries({ queryKey: ['product-effectiveness', productId] })
    },
    onError: (e: Error) => toast(`重新计算失败：${getErrorMessage(e)}`, 'error'),
  })

  const productList = products?.list ?? []
  const selectedProduct = productList.find((item) => item.id === productId)
  const rows = data?.list ?? []
  const total = data?.total ?? 0
  const summary = data?.summary ?? { dates: [], avgScores: [], avgScore: 0, maxScore: 0, scoredCount: 0, avgConversionRate: 0 }
  const lowScoreCount = rows.filter((row) => Number(row.avgScore ?? 0) > 0 && Number(row.avgScore ?? 0) < 5).length
  const noUsageCount = rows.filter((row) => Number(row.useCount ?? 0) === 0).length
  const recommendedCount = rows.filter((row) => row.isRecommended).length
  const hasRanking = rows.length > 0
  const scoreProblem = productId === ''
    ? '请选择商品后查看该商品的话术版本评分。'
    : !hasRanking
      ? '暂无话术版本评分样本。通常是商品尚未生成话术版本，或评分任务尚未执行。'
      : Number(summary.avgScore ?? 0) < 5
        ? '平均评分低于 5 分。优先检查话术版本使用次数、转化率和互动数据回流。'
        : '评分样本可用，继续关注低分版本和未使用版本。'
  const rankingError = `${getErrorMessage(error)}。请检查 /product/script-effectiveness/ranking、productId、登录态和商品数据权限。`
  const productError = `${getErrorMessage(productsError)}。请检查 /product/search、登录态和商品数据权限。`
  const scoreChartColor = theme.palette.mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main

  const trendOption = useMemo(() => ({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map((row) => row.productName ?? `版本 ${row.versionId ?? '-'}`) },
    yAxis: { type: 'value', min: 0, max: 10 },
    series: [{
      type: 'bar',
      data: rows.map((row) => Number(row.avgScore ?? 0)),
      name: '版本评分',
      itemStyle: { color: scoreChartColor },
    }],
  }), [rows, scoreChartColor])

  const columns: GridColDef[] = [
    { field: 'versionId', headerName: '版本 ID', width: 90, valueFormatter: (v) => v ?? '-' },
    { field: 'productName', headerName: '话术版本', flex: 1, minWidth: 180 },
    { field: 'style', headerName: '风格', width: 110, renderCell: ({ value }) => <Chip size="small" variant="outlined" label={String(value ?? '-')} /> },
    {
      field: 'avgScore', headerName: '效果评分', width: 210,
      renderCell: ({ value }) => <ScoreBar value={Number(value ?? 0)} />,
    },
    { field: 'scoreLevel', headerName: '等级', width: 80, renderCell: ({ value }) => <Chip size="small" label={String(value ?? '-')} /> },
    { field: 'useCount', headerName: '使用次数', width: 95 },
    {
      field: 'conversionRate', headerName: '转化率', width: 95,
      renderCell: ({ value }) => <Typography variant="body2">{value != null ? `${Number(value).toFixed(2)}%` : '--'}</Typography>,
    },
    { field: 'likesCount', headerName: '点赞', width: 80, valueFormatter: (v) => v ?? 0 },
    { field: 'commentsCount', headerName: '评论', width: 80, valueFormatter: (v) => v ?? 0 },
    { field: 'isRecommended', headerName: '推荐', width: 80, renderCell: ({ value }) => <Chip size="small" color={value ? 'success' : 'default'} label={value ? '是' : '否'} /> },
  ]

  const searchSlot = (
    <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
      <FormControl size="small" sx={{ minWidth: 220 }}>
        <InputLabel id="effectiveness-product-label" htmlFor="effectiveness-product-select">商品</InputLabel>
        <Select
          native
          labelId="effectiveness-product-label"
          id="effectiveness-product-select"
          label="商品"
          value={productId}
          onChange={(event) => { setProductId(event.target.value === '' ? '' : Number(event.target.value)); setPage(0) }}
          disabled={productsFetching}
        >
          <option value="">请选择商品</option>
          {productList.map((product) => (
            <option key={product.id} value={product.id}>{product.productName || `商品 #${product.id}`}</option>
          ))}
        </Select>
      </FormControl>
      <FormControl size="small" sx={{ minWidth: 140 }}>
        <InputLabel id="effectiveness-sort-label">排序</InputLabel>
        <Select value={sortBy} label="排序" labelId="effectiveness-sort-label" onChange={(event) => { setSortBy(event.target.value); setPage(0) }}>
          <MenuItem value="score">评分</MenuItem>
          <MenuItem value="usage">使用次数</MenuItem>
          <MenuItem value="conversion">转化率</MenuItem>
          <MenuItem value="interaction">互动</MenuItem>
        </Select>
      </FormControl>
    </Stack>
  )

  const actionSlot = (
    <Stack direction="row" spacing={1}>
      <Button size="small" startIcon={<CalculateIcon />} onClick={() => recalculateMut.mutate()} disabled={productId === '' || recalculateMut.isPending}>
        重新计算评分
      </Button>
      <Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={productId === '' || isFetching}>刷新</Button>
    </Stack>
  )

  return (
    <Box
      data-testid="product-effectiveness-workbench"
      data-contract-scope="product-effectiveness-ranking"
      data-ready-endpoints={EFFECTIVENESS_READY_ENDPOINTS.join('|')}
      data-context-endpoints={EFFECTIVENESS_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={EFFECTIVENESS_UNSUPPORTED_ACTIONS.join('|')}
      data-selected-product-id={productId || ''}
      data-sort-by={sortBy}
      data-page={page}
      data-product-count={productList.length}
      data-result-count={rows.length}
      data-total={total}
      data-low-score-count={lowScoreCount}
      data-no-usage-count={noUsageCount}
      data-recommended-count={recommendedCount}
      data-no-static-ranking-fallback="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="商品效果评分"
        subtitle="按商品查看真实 ProductScriptVersion 评分排行，接口对齐 /product/script-effectiveness/ranking。"
        breadcrumbs={[{ label: '商品' }, { label: '效果评分' }]}
        actions={(
          <Stack direction="row" spacing={1}>
            <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={() => { void refetchProducts(); if (productId !== '') void refetch() }} disabled={productsFetching || isFetching}>
              刷新
            </Button>
            <Button size="small" variant="contained" startIcon={<CalculateIcon />} onClick={() => recalculateMut.mutate()} disabled={productId === '' || recalculateMut.isPending}>
              重新计算评分
            </Button>
          </Stack>
        )}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="product-effectiveness-contract-alert"
        data-contract-source="/product/search|/product/script-effectiveness/ranking"
        data-no-all-product-ranking="true"
        data-no-server-export-request="true"
        data-no-auto-script-generation="true"
        data-no-sales-history-fallback="true"
      >
        真实接口：<code>/product/search</code> 用于商品选择，<code>/product/script-effectiveness/ranking</code> 返回该商品的话术版本评分。
        该接口不是全商品排行榜；必须先选择商品。
      </Alert>

      {productsIsError && (
        <Box
          data-testid="product-effectiveness-products-error"
          data-contract-source="/product/search"
          data-no-local-product-fallback="true"
        >
          <ErrorAlert severity="warning" title="商品列表加载失败" message={productError} onRetry={() => refetchProducts()} />
        </Box>
      )}

      <Grid container spacing={2}>
        {[
          { label: '当前商品', value: selectedProduct?.productName ?? (productId === '' ? '未选择' : `#${productId}`) },
          { label: '平均评分', value: summary.avgScore != null ? Number(summary.avgScore).toFixed(1) : '--' },
          { label: '最高评分', value: summary.maxScore != null ? Number(summary.maxScore).toFixed(1) : '--' },
          { label: '评分版本数', value: String(summary.scoredCount ?? rows.length) },
        ].map((kpi) => (
          <Grid item xs={6} sm={3} key={kpi.label}>
            <Card
              variant="outlined"
              data-testid="product-effectiveness-kpi-card"
              data-kpi-label={kpi.label}
              data-contract-source={kpi.label === '当前商品' ? 'product-selection-state' : 'local-derived-from-ranking'}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
                <Typography variant="h6" fontWeight={700} noWrap>{kpi.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        {[
          { label: '低分版本', value: String(lowScoreCount), hint: '当前页 0-5 分' },
          { label: '无使用记录', value: String(noUsageCount), hint: '评分只含基础分' },
          { label: '推荐版本', value: String(recommendedCount), hint: '后端 isRecommended' },
          { label: '平均转化率', value: summary.avgConversionRate != null ? `${Number(summary.avgConversionRate).toFixed(2)}%` : '--', hint: '来自版本 conversionRate' },
        ].map((item) => (
          <Grid item xs={6} md={3} key={item.label}>
            <Card
              variant="outlined"
              data-testid="product-effectiveness-diagnostic-card"
              data-diagnostic-label={item.label}
              data-contract-source="local-derived-from-ranking"
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700}>{item.value}</Typography>
                <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Alert
        severity={productId === '' || !hasRanking || Number(summary.avgScore ?? 0) < 5 ? 'warning' : 'info'}
        data-testid="product-effectiveness-status-alert"
        data-contract-source={hasRanking ? '/product/script-effectiveness/ranking' : 'local-selection-or-empty-state'}
        data-selected-product-id={productId || ''}
      >
        {scoreProblem}
      </Alert>

      {isError && (
        <Alert
          severity="error"
          data-testid="product-effectiveness-ranking-error"
          data-contract-source="/product/script-effectiveness/ranking"
          data-selected-product-id={productId || ''}
          data-no-static-ranking-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          商品效果评分加载失败：{rankingError}
        </Alert>
      )}
      {recalculateMut.isError && (
        <Alert
          severity="error"
          data-testid="product-effectiveness-recalculate-error"
          data-contract-source="/product/script-effectiveness/recalculate"
          data-selected-product-id={productId || ''}
        >
          重新计算失败：{getErrorMessage(recalculateMut.error)}
        </Alert>
      )}

      {hasRanking ? (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" fontWeight={600} gutterBottom>版本评分分布</Typography>
            <Box
              data-testid="effectiveness-score-chart-surface"
              data-contract-source="/product/script-effectiveness/ranking"
              data-chart-color={scoreChartColor}
              data-row-count={rows.length}
            >
              <ReactECharts option={trendOption} style={{ height: 220 }} />
            </Box>
          </CardContent>
        </Card>
      ) : (
        <Alert
          severity="info"
          data-testid="product-effectiveness-empty-ranking"
          data-contract-source="/product/script-effectiveness/ranking"
          data-no-static-ranking-fallback="true"
        >
          暂无评分分布。请先生成商品话术版本，或点击“重新计算评分”让后端写入评分与快照。
        </Alert>
      )}

      <StandardDataGrid
        rows={rows}
        columns={columns}
        rowCount={total}
        loading={isFetching}
        paginationMode="server"
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={(model) => setPage(model.page)}
        getRowId={(row) => (row as EffectivenessScoreItem).versionId ?? `${(row as EffectivenessScoreItem).style}-${(row as EffectivenessScoreItem).productName}`}
        searchSlot={searchSlot}
        actionSlot={actionSlot}
        showExport={false}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ height: 480 }}
      />
    </Box>
  )
}
