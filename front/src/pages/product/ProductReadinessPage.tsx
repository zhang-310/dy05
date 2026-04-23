import { useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Chip, Stack, LinearProgress,
  List, ListItem, ListItemIcon, ListItemText, Grid, Alert,
} from '@mui/material'
import {
  CheckCircle as OkIcon, Warning as WarnIcon, Error as ErrorIcon,
} from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/base'
import { request } from '@/utils/request'

interface ReadinessItem {
  dimension: string
  status: 'ok' | 'warn' | 'error'
  message: string
  score: number
}

interface ReadinessResult {
  productId: number
  productName: string
  overallScore: number
  readyForLive: boolean
  items: ReadinessItem[]
}

export default function ProductReadinessPage() {
  const [searchParams] = useSearchParams()
  const productId = searchParams.get('productId') ? Number(searchParams.get('productId')) : null

  const { data: result, isLoading } = useQuery({
    queryKey: ['product-readiness', productId],
    queryFn: () => request.post<ReadinessResult>('/product/readiness', { productId }),
    enabled: !!productId,
  })

  const statusIcon = (s: ReadinessItem['status']) => {
    if (s === 'ok') return <OkIcon color="success" />
    if (s === 'warn') return <WarnIcon color="warning" />
    return <ErrorIcon color="error" />
  }

  const statusColor = (s: ReadinessItem['status']): 'success' | 'warning' | 'error' => {
    if (s === 'ok') return 'success'
    if (s === 'warn') return 'warning'
    return 'error'
  }

  if (!productId) {
    return (
      <Box>
        <PageHeader title="商品上播准备度" breadcrumbs={[{ label: '商品' }, { label: '上播准备度' }]} />
        <Alert severity="info">请在商品列表点击「准备度检测」进入本页，或在 URL 中携带 ?productId=xxx</Alert>
      </Box>
    )
  }

  return (
    <Box>
      <PageHeader
        title="商品上播准备度"
        breadcrumbs={[{ label: '商品' }, { label: '上播准备度' }]}
        subtitle={result ? result.productName : `商品 #${productId}`}
      />

      {isLoading && <LinearProgress sx={{ mb: 2 }} />}

      {!!result && (
        <>
          {/* 总体评分 */}
          <Card sx={{ mb: 3 }}>
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

          {/* 检测项明细 */}
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>检测项明细</Typography>
              <List disablePadding>
                {(result.items ?? []).map((item) => (
                  <ListItem key={item.dimension} disablePadding sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
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
            </CardContent>
          </Card>
        </>
      )}
    </Box>
  )
}
