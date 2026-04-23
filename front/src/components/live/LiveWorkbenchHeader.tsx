import { Box, Typography, Button, Chip, LinearProgress, IconButton, Tooltip } from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' | 'error' }> = {
  0: { label: '准备中', color: 'default' },
  1: { label: '直播中', color: 'success' },
  2: { label: '已结束', color: 'warning' },
  3: { label: '已取消', color: 'error' },
}

function formatDate(val: unknown): string {
  if (val == null) return '-'
  const s = String(val)
  if (s.length >= 16) return s.slice(0, 16).replace('T', ' ')
  return s
}

export function LiveWorkbenchHeader({
  session,
  sessionId,
  productsCount,
  scriptsCount,
  readinessPercent,
  onBack,
  onRefresh,
  refreshing,
}: {
  session: Record<string, unknown> | null
  sessionId: number
  productsCount: number
  scriptsCount: number
  readinessPercent: number | null
  onBack: () => void
  onRefresh: () => void
  refreshing?: boolean
}) {
  const status = (session?.status as number) ?? 0
  const statusInfo = STATUS_MAP[status] ?? { label: '未知', color: 'default' as const }
  const title = session ? String(session.liveTitle ?? `场次 #${sessionId}`) : `场次 #${sessionId}`

  return (
    <Box sx={{ mb: 2 }}>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, flexWrap: 'wrap' }}>
        <Button size="small" startIcon={<ArrowBackIcon />} onClick={onBack} sx={{ mt: 0.25 }}>
          返回场次列表
        </Button>
        <Box sx={{ flex: 1, minWidth: 200 }}>
          <Typography variant="h6" fontWeight={700}>
            {title}
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center', mt: 0.5 }}>
            <Chip size="small" label={statusInfo.label} color={statusInfo.color} variant="outlined" />
            <Typography variant="body2" color="text.secondary">
              场次 ID: {sessionId}
            </Typography>
            {session?.scheduledTime != null && (
              <Typography variant="body2" color="text.secondary">
                计划：{formatDate(session.scheduledTime)}
              </Typography>
            )}
            <Typography variant="caption" color="text.secondary">
              商品 {productsCount} · 话术 {scriptsCount}
            </Typography>
          </Box>
          {readinessPercent != null && (
            <Box sx={{ mt: 1.5, maxWidth: 360 }}>
              <Typography variant="caption" color="text.secondary">
                就绪度
              </Typography>
              <LinearProgress variant="determinate" value={readinessPercent} sx={{ height: 8, borderRadius: 1, mt: 0.5 }} />
            </Box>
          )}
        </Box>
        <Tooltip title="刷新">
          <span>
            <IconButton size="small" onClick={onRefresh} disabled={refreshing}>
              <RefreshIcon />
            </IconButton>
          </span>
        </Tooltip>
      </Box>
    </Box>
  )
}
