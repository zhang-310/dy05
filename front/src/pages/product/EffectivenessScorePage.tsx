import { useState } from 'react'
import {
  Box, Typography, Stack, Card, CardContent, Grid, Chip, LinearProgress,
  Button, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { useQuery } from '@tanstack/react-query'
import { productApi, type EffectivenessScoreItem } from '@/api/product'
import ReactECharts from 'echarts-for-react'

function ScoreBar({ value }: { value: number }) {
  const color = value >= 8 ? 'success' : value >= 5 ? 'warning' : 'error'
  return (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }}>
      <LinearProgress variant="determinate" value={value * 10} color={color}
        sx={{ flex: 1, height: 8, borderRadius: 4 }} />
      <Typography variant="caption" fontWeight={700} sx={{ minWidth: 28 }}>{value.toFixed(1)}</Typography>
    </Stack>
  )
}

export default function EffectivenessScorePage() {
  const [tag, setTag] = useState('')
  const [page, setPage] = useState(0)

  const { data, isFetching, refetch } = useQuery({
    queryKey: ['product-effectiveness', tag, page],
    queryFn: () => productApi.effectivenessRanking({ tag, page, rows: 20 }),
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0
  const summary = data?.summary ?? { dates: [], avgScores: [], avgScore: 0, maxScore: 0, scoredCount: 0, avgConversionRate: 0 }

  const trendOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: summary.dates ?? [] },
    yAxis: { type: 'value', min: 0, max: 10 },
    series: [{ type: 'line', smooth: true, data: summary.avgScores ?? [], name: '平均评分', areaStyle: { opacity: 0.15 } }],
  }

  const columns: GridColDef[] = [
    { field: 'productId', headerName: 'ID', width: 70 },
    { field: 'productName', headerName: '商品名称', flex: 1, minWidth: 160 },
    { field: 'avgScore', headerName: '话术效果评分', width: 200,
      renderCell: ({ value }) => <ScoreBar value={Number(value ?? 0)} /> },
    { field: 'useCount', headerName: '使用次数', width: 90 },
    { field: 'conversionRate', headerName: '转化率', width: 90,
      renderCell: ({ value }) => <Typography variant="body2">{value != null ? `${(Number(value) * 100).toFixed(1)}%` : '--'}</Typography> },
    { field: 'trend', headerName: '趋势', width: 80,
      renderCell: ({ value }) => (
        <Chip
          label={value === 'up' ? '↑ 上升' : value === 'down' ? '↓ 下降' : '→ 平稳'}
          size="small"
          color={value === 'up' ? 'success' : value === 'down' ? 'error' : 'default'}
        />
      ) },
    { field: 'tag', headerName: '类别', width: 100,
      renderCell: ({ value }) => <Chip label={String(value ?? '-')} size="small" variant="outlined" /> },
  ]

  const searchSlot = (
    <FormControl size="small" sx={{ minWidth: 120 }}>
      <InputLabel>类别</InputLabel>
      <Select value={tag} label="类别" onChange={e => { setTag(e.target.value); setPage(0) }}>
        <MenuItem value="">全部</MenuItem>
        {['护肤', '彩妆', '保健', '食品'].map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
      </Select>
    </FormControl>
  )

  const actionSlot = <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>商品效果评分</Typography>
      <Typography variant="h6" fontWeight={600}>商品话术效果评分</Typography>

      <Grid container spacing={2}>
        {[
          { label: '平均评分', value: summary.avgScore?.toFixed(1) ?? '--' },
          { label: '最高评分', value: summary.maxScore?.toFixed(1) ?? '--' },
          { label: '评分商品数', value: String(summary.scoredCount ?? '--') },
          { label: '平均转化率', value: summary.avgConversionRate != null ? `${(Number(summary.avgConversionRate) * 100).toFixed(1)}%` : '--' },
        ].map(kpi => (
          <Grid item xs={6} sm={3} key={kpi.label}>
            <Card variant="outlined"><CardContent sx={{ py: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{kpi.value}</Typography>
            </CardContent></Card>
          </Grid>
        ))}
      </Grid>

      {summary.dates?.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" fontWeight={600} gutterBottom>评分趋势</Typography>
            <ReactECharts option={trendOption} style={{ height: 200 }} />
          </CardContent>
        </Card>
      )}

      <StandardDataGrid
        rows={rows} columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={m => setPage(m.page)}
        getRowId={(r) => (r as EffectivenessScoreItem).productId}
        searchSlot={searchSlot} actionSlot={actionSlot}
        sx={{ height: 480 }}
      />
    </Box>
  )
}
