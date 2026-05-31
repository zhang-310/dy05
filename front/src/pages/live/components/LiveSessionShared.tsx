import { Box, Chip, Typography } from '@mui/material'
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'

export const STATUS_MAP: Record<number, { label: string; color: 'default' | 'success' | 'warning' | 'error' }> = {
  0: { label: '准备中', color: 'default' },
  1: { label: '直播中', color: 'success' },
  2: { label: '已结束', color: 'warning' },
  3: { label: '已取消', color: 'error' },
}

export function formatDate(val: unknown): string {
  if (val == null) return '-'
  const s = String(val)
  if (s.length >= 16) return s.slice(0, 16).replace('T', ' ')
  return s
}

/** 脉冲动画 keyframes（直播中状态） */
export const pulseKeyframes = {
  '@keyframes pulse': {
    '0%': { opacity: 1, transform: 'scale(1)' },
    '50%': { opacity: 0.5, transform: 'scale(1.4)' },
    '100%': { opacity: 1, transform: 'scale(1)' },
  },
}

export function ReadinessSteps({ row }: { row: Record<string, unknown> }) {
  const pc = Number(row.productCount ?? 0)
  const sc = Number(row.scriptCount ?? 0)
  const progress = pc === 0 ? 0 : sc === 0 ? 1 : 2
  const steps = ['选品', '话术', '就绪']
  return (
    <Box
      data-testid="live-session-readiness-steps"
      data-contract-scope="live-session-readiness-steps-derived"
      data-contract-source="session-row-productCount-scriptCount"
      data-product-count={pc}
      data-script-count={sc}
      data-progress={progress}
      data-no-direct-api="true"
      sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
    >
      {steps.map((label, i) => (
        <Box
          key={label}
          data-testid="live-session-readiness-step"
          data-step-index={i}
          data-step-label={label}
          data-step-state={i < progress ? 'done' : i === progress ? 'current' : 'pending'}
          sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}
        >
          {i > 0 && (
            <Box sx={{ width: 12, height: 1, bgcolor: i <= progress ? 'success.main' : 'divider' }} />
          )}
          {i < progress ? (
            <CheckCircleOutlineIcon sx={{ fontSize: 14, color: 'success.main' }} />
          ) : (
            <RadioButtonUncheckedIcon
              sx={{ fontSize: 14, color: i === progress ? 'primary.main' : 'text.disabled' }}
            />
          )}
          <Typography
            variant="caption"
            color={i < progress ? 'success.main' : i === progress ? 'primary.main' : 'text.disabled'}
            sx={{ fontWeight: i === progress ? 600 : 400 }}
          >
            {label}
          </Typography>
        </Box>
      ))}
    </Box>
  )
}

export function StatusChip({ status }: { status: number }) {
  const info = STATUS_MAP[status] ?? { label: '未知', color: 'default' as const }
  if (status === 1) {
    return (
      <Chip
        data-testid="live-session-status-chip"
        data-contract-scope="live-session-status-chip-derived"
        data-contract-source="session-row-status"
        data-status={status}
        data-status-label={info.label}
        data-status-tone={info.color}
        data-live-pulse="true"
        data-no-direct-api="true"
        icon={
          <FiberManualRecordIcon
            sx={{
              fontSize: 10,
              color: 'success.main',
              animation: 'pulse 1.5s ease-in-out infinite',
              ...pulseKeyframes,
            }}
          />
        }
        label={info.label}
        size="small"
        color="success"
        variant="outlined"
      />
    )
  }
  return (
    <Chip
      data-testid="live-session-status-chip"
      data-contract-scope="live-session-status-chip-derived"
      data-contract-source="session-row-status"
      data-status={status}
      data-status-label={info.label}
      data-status-tone={info.color}
      data-live-pulse="false"
      data-no-direct-api="true"
      label={info.label}
      size="small"
      color={info.color}
      variant="outlined"
    />
  )
}

export function TimeTip({ scheduledTime }: { scheduledTime: unknown }) {
  if (scheduledTime == null) return null
  const ts = new Date(String(scheduledTime)).getTime()
  if (Number.isNaN(ts)) return null
  const diff = ts - Date.now()
  if (diff > 0 && diff < 30 * 60 * 1000) {
    const mins = Math.ceil(diff / 60000)
    return (
      <Typography
        data-testid="live-session-time-tip"
        data-contract-scope="live-session-time-tip-derived"
        data-contract-source="scheduledTime-prop"
        data-minutes-left={mins}
        data-no-direct-api="true"
        variant="caption"
        color="warning.main"
        sx={{ fontWeight: 600 }}
      >
        {mins} 分钟后开播
      </Typography>
    )
  }
  return null
}
