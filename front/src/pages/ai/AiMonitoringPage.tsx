import { useMemo } from 'react'
import { Box, Card, CardContent, Typography, Stack, Chip, LinearProgress, Button } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { aiApi } from '@/api/ai'

/** 与后端 InfraHealthItem 对齐：component / ok / message */
interface InfraHealthItem {
  component?: string
  ok?: boolean
  message?: string | null
}

interface CacheStatsData {
  hitRate?: number
  hit_rate?: number
  [key: string]: unknown
}

interface SearchStatsData {
  [key: string]: unknown
}

function formatStatValue(v: unknown): string {
  if (v === null || v === undefined) return '—'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

/** 后端 hitRate：通常为 0–100 的百分数；兼容 0–1 小数 */
function toCacheHitPercent(hitRateRaw: unknown): number {
  const n = Number(hitRateRaw)
  if (Number.isNaN(n)) return 0
  if (n > 1) return Math.min(100, n)
  return Math.min(100, n * 100)
}

function HealthCard({ item }: { item: InfraHealthItem }) {
  const title = item.component ?? '组件'
  const isOk = item.ok === true
  return (
    <Card variant="outlined" sx={{ flex: 1, minWidth: 200 }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
          <Typography variant="subtitle2">{title}</Typography>
          <Chip label={isOk ? '正常' : '异常'} size="small" color={isOk ? 'success' : 'error'} />
        </Stack>
        {item.message ? (
          <Typography variant="caption" color="text.secondary">{item.message}</Typography>
        ) : null}
      </CardContent>
    </Card>
  )
}

function CacheStatsCard({ data }: { data: CacheStatsData | undefined }) {
  if (!data) return null
  const pct = toCacheHitPercent(data.hitRate ?? data.hit_rate)
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" mb={1}>缓存统计</Typography>
        <Stack spacing={1}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="body2">命中率</Typography>
            <Typography variant="body2" fontWeight={600}>{pct.toFixed(1)}%</Typography>
          </Stack>
          <LinearProgress variant="determinate" value={pct} sx={{ height: 8, borderRadius: 4 }}
            color={pct >= 80 ? 'success' : pct >= 50 ? 'warning' : 'error'} />
          {Object.entries(data).filter(([k]) => !['hitRate', 'hit_rate'].includes(k)).map(([k, v]) => (
            <Stack key={k} direction="row" justifyContent="space-between">
              <Typography variant="caption" color="text.secondary">{k}</Typography>
              <Typography variant="caption">{formatStatValue(v)}</Typography>
            </Stack>
          ))}
        </Stack>
      </CardContent>
    </Card>
  )
}

function SearchStatsCard({ data }: { data: SearchStatsData | undefined }) {
  if (!data) return null
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" mb={1}>搜索引擎统计</Typography>
        <Stack spacing={0.5}>
          {Object.entries(data).map(([k, v]) => (
            <Stack key={k} direction="row" justifyContent="space-between">
              <Typography variant="caption" color="text.secondary">{k}</Typography>
              <Typography variant="caption" sx={{ textAlign: 'right', maxWidth: '65%', wordBreak: 'break-word' }}>{formatStatValue(v)}</Typography>
            </Stack>
          ))}
        </Stack>
      </CardContent>
    </Card>
  )
}

export default function AiMonitoringPage() {
  const qc = useQueryClient()

  const { data: healthData, isLoading: healthLoading } = useQuery({
    queryKey: ['ai-infra-health'],
    queryFn: () => aiApi.infraHealth(),
    refetchInterval: 30000,
  })
  const { data: cacheData } = useQuery({
    queryKey: ['ai-cache-stats'],
    queryFn: () => aiApi.cacheStats(),
    refetchInterval: 30000,
  })
  const { data: searchData } = useQuery({
    queryKey: ['ai-search-stats'],
    queryFn: () => aiApi.searchStats(),
    refetchInterval: 30000,
  })

  const cache = (cacheData ?? undefined) as CacheStatsData | undefined
  const search = (searchData ?? undefined) as SearchStatsData | undefined

  const infraItems = useMemo((): InfraHealthItem[] => {
    const h = healthData
    if (Array.isArray(h)) return h as InfraHealthItem[]
    return []
  }, [healthData])

  const handleRefresh = () => {
    qc.invalidateQueries({ queryKey: ['ai-infra-health'] })
    qc.invalidateQueries({ queryKey: ['ai-cache-stats'] })
    qc.invalidateQueries({ queryKey: ['ai-search-stats'] })
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Stack direction="row" justifyContent="flex-end">
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefresh}>刷新</Button>
      </Stack>

      {healthLoading && <LinearProgress />}

      <Typography variant="subtitle2">基础设施健康状态</Typography>
      <Stack direction="row" spacing={2} flexWrap="wrap">
        {infraItems.length > 0
          ? infraItems.map((item, idx) => (
              <HealthCard key={item.component ?? `idx-${idx}`} item={item} />
            ))
          : (
              <Typography variant="body2" color="text.secondary">暂无健康检查数据</Typography>
            )}
      </Stack>

      <Stack direction="row" spacing={2}>
        <Box sx={{ flex: 1 }}><CacheStatsCard data={cache} /></Box>
        <Box sx={{ flex: 1 }}><SearchStatsCard data={search} /></Box>
      </Stack>
    </Box>
  )
}
