import { Card, CardContent, Typography, Box, Chip } from '@mui/material'
import { TrendingUp, TrendingDown } from '@mui/icons-material'

export interface StatCardProps {
  title: string
  value: string | number
  unit?: string
  trend?: {
    value: number
    label?: string
  }
  icon?: React.ReactNode
  color?: 'primary' | 'success' | 'error' | 'warning' | 'info'
  onClick?: () => void
}

/**
 * StatCard - 统计卡片组件
 * 应用设计系统规范：
 * - 背景：var(--color-surface) #1E293B
 * - 圆角：12px (border-radius-xl)
 * - 阴影：elevation-1 (0 4px 12px rgba(0,0,0,0.3))
 * - Hover：边框绿色，阴影增强
 */
export function StatCard({
  title,
  value,
  unit,
  trend,
  icon,
  color = 'primary',
  onClick,
}: StatCardProps) {
  const trendColor = trend && trend.value > 0 ? 'success' : 'error'
  const TrendIcon = trend && trend.value > 0 ? TrendingUp : TrendingDown

  return (
    <Card
      sx={{
        flex: 1,
        minHeight: 120,
        minWidth: 0,
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        cursor: onClick ? 'pointer' : 'default',
        borderRadius: '12px', // border-radius-xl
        border: '1px solid #334155', // surface-light
        backgroundColor: '#1E293B', // color-surface
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)', // shadow-elevation-1
        transition: 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-base + ease-in-out
        '&:hover': onClick
          ? {
              borderColor: '#00D084', // color-primary
              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.35)', // shadow-elevation-2
            }
          : undefined,
      }}
      onClick={onClick}
    >
      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, padding: '16px' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2, minHeight: 24 }}>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{
              fontSize: '12px', // font-size-sm
              fontWeight: 500, // font-weight-medium
              color: '#475569', // color-text-secondary
            }}
          >
            {title}
          </Typography>
          {icon && (
            <Box sx={{ color: `${color}.main`, flexShrink: 0 }}>{icon}</Box>
          )}
        </Box>

        <Typography
          variant="h4"
          fontWeight={600}
          sx={{
            minHeight: 36,
            display: 'flex',
            alignItems: 'baseline',
            fontSize: '24px', // font-size-xl
            lineHeight: 1.3, // line-height-xl
            color: '#F1F5F9', // color-text-primary
          }}
        >
          {value}
          {unit && (
            <Typography
              component="span"
              variant="h6"
              color="text.secondary"
              sx={{
                ml: 0.5,
                fontSize: '14px', // font-size-base
                color: '#475569', // color-text-secondary
              }}
            >
              {unit}
            </Typography>
          )}
        </Typography>

        {trend && (
          <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
            <Chip
              icon={<TrendIcon sx={{ fontSize: 16 }} />}
              label={`${trend.value > 0 ? '+' : ''}${trend.value}%`}
              size="small"
              color={trendColor as 'success' | 'error'}
              sx={{
                fontWeight: 600,
                height: '24px', // badge height
                fontSize: '12px', // font-size-sm
              }}
            />
            {trend.label && (
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                  ml: 1,
                  fontSize: '11px', // font-size-xs
                  color: '#475569', // color-text-secondary
                }}
              >
                {trend.label}
              </Typography>
            )}
          </Box>
        )}
      </CardContent>
    </Card>
  )
}
