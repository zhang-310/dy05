import {
  Box, Typography, Stack, Card, CardContent, Grid, Chip, Button,
  CircularProgress, Alert, Divider,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery } from '@tanstack/react-query'
import { tianapi, type HotItem } from '@/api/tianapi'

interface TianApiStatus {
  enabled?: boolean
  status?: string
  remainingQuota?: number
  todayUsed?: number
}

const SOURCES = [
  { label: '抖音热榜', key: 'douyin' as const, fn: () => tianapi.hotDouyin() },
  { label: '头条热榜', key: 'toutiao' as const, fn: () => tianapi.hotToutiao() },
  { label: '微博热搜', key: 'weibo' as const, fn: () => tianapi.hotWeibo() },
  { label: '全网热点', key: 'network' as const, fn: () => tianapi.hotNetwork() },
]

function HotList({ source }: { source: typeof SOURCES[0] }) {
  const { data = [], isLoading, refetch } = useQuery({
    queryKey: ['tianapi-panel', source.key],
    queryFn: source.fn,
    refetchInterval: 5 * 60 * 1000,
  })

  return (
    <Card variant="outlined">
      <CardContent sx={{ pb: '12px !important' }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
          <Typography variant="subtitle2" fontWeight={600}>{source.label}</Typography>
          <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetch()}>刷新</Button>
        </Stack>
        {isLoading && <CircularProgress size={20} sx={{ mx: 'auto', display: 'block' }} />}
        <Stack spacing={0.5}>
          {(data as HotItem[]).slice(0, 10).map((item, i) => (
            <Stack key={i} direction="row" justifyContent="space-between" alignItems="center"
              sx={{ py: 0.25, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="caption" fontWeight={700}
                  color={i < 3 ? 'error.main' : 'text.secondary'} sx={{ minWidth: 20 }}>
                  {i + 1}
                </Typography>
                <Typography variant="body2" noWrap sx={{ flex: 1 }}>{item.word}</Typography>
              </Stack>
              {item.hotIndex != null && (
                <Typography variant="caption" color="text.secondary">
                  {Number(item.hotIndex).toLocaleString()}
                </Typography>
              )}
            </Stack>
          ))}
        </Stack>
      </CardContent>
    </Card>
  )
}

export default function TianApiPanelPage() {
  const { data: status, isLoading: statusLoading } = useQuery({
    queryKey: ['tianapi-status'],
    queryFn: () => tianapi.status(),
    refetchInterval: 60000,
  })

  const s: TianApiStatus = status ?? {}

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6" fontWeight={600}>天API 数据面板</Typography>

      <Card variant="outlined">
        <CardContent sx={{ py: 1.5 }}>
          <Stack direction="row" spacing={3} alignItems="center">
            <Typography variant="body2" color="text.secondary">API 状态</Typography>
            {statusLoading
              ? <CircularProgress size={16} />
              : <Chip label={s.status === 'ok' ? '正常' : '异常'} color={s.status === 'ok' ? 'success' : 'error'} size="small" />}
            {s.remainingQuota != null && (
              <>
                <Divider orientation="vertical" flexItem />
                <Typography variant="body2" color="text.secondary">剩余配额</Typography>
                <Typography variant="body2" fontWeight={700}>{String(s.remainingQuota)}</Typography>
              </>
            )}
            {s.todayUsed != null && (
              <>
                <Divider orientation="vertical" flexItem />
                <Typography variant="body2" color="text.secondary">今日已用</Typography>
                <Typography variant="body2" fontWeight={700}>{String(s.todayUsed)}</Typography>
              </>
            )}
          </Stack>
        </CardContent>
      </Card>

      {s.status !== 'ok' && !statusLoading && (
        <Alert severity="warning">天API 服务状态异常，热点数据可能无法正常获取</Alert>
      )}

      <Grid container spacing={2}>
        {SOURCES.map(src => (
          <Grid item xs={12} sm={6} key={src.key}>
            <HotList source={src} />
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
