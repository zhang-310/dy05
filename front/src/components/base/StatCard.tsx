import { Card, CardContent, Typography, Box, Chip } from '@mui/material'
import { alpha } from '@mui/material/styles'
import { TrendingUp, TrendingDown } from '@mui/icons-material'
import type { ReactNode } from 'react'

export interface StatCardProps {
  title: string
  value: string | number
  unit?: string
  trend?: {
    value: number
    label?: string
  }
  icon?: ReactNode
  color?: 'primary' | 'success' | 'error' | 'warning' | 'info'
  onClick?: () => void
}

/**
 * StatCard - 统计卡片组件
 * 应用设计系统规范：
 * - 背景：background.paper
 * - 圆角：12px (border-radius-xl)
 * - 阴影：theme-aware elevation
 * - Hover：当前语义色边框，阴影增强
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
      data-testid="base-stat-card-surface"
      data-stat-tone={color}
      sx={(theme) => ({
        flex: 1,
        minHeight: 120,
        minWidth: 0,
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        cursor: onClick ? 'pointer' : 'default',
        borderRadius: '12px', // border-radius-xl
        border: '1px solid',
        borderColor: theme.palette.divider,
        backgroundColor: theme.palette.background.paper,
        boxShadow: `0 4px 12px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.28 : 0.08)}`,
        transition: 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-base + ease-in-out
        '&:hover': onClick
          ? {
              borderColor: theme.palette[color].main,
              boxShadow: `0 8px 24px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.34 : 0.14)}`,
            }
          : undefined,
      })}
      onClick={onClick}
    >
      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, padding: '16px' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2, minHeight: 24 }}>
          <Typography
            data-testid="base-stat-card-title"
            variant="body2"
            color="text.secondary"
            sx={{
              fontSize: '12px', // font-size-sm
              fontWeight: 500, // font-weight-medium
              color: 'text.secondary',
            }}
          >
            {title}
          </Typography>
          {icon && (
            <Box sx={{ color: `${color}.main`, flexShrink: 0 }}>{icon}</Box>
          )}
        </Box>

        <Typography
          data-testid="base-stat-card-value"
          variant="h4"
          fontWeight={600}
          sx={{
            minHeight: 36,
            display: 'flex',
            alignItems: 'baseline',
            fontSize: '24px', // font-size-xl
            lineHeight: 1.3, // line-height-xl
            color: 'text.primary',
          }}
        >
          {value}
          {unit && (
            <Typography
              data-testid="base-stat-card-unit"
              component="span"
              variant="h6"
              color="text.secondary"
              sx={{
                ml: 0.5,
                fontSize: '14px', // font-size-base
                color: 'text.secondary',
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
                data-testid="base-stat-card-trend-label"
                variant="caption"
                color="text.secondary"
                sx={{
                  ml: 1,
                  fontSize: '11px', // font-size-xs
                  color: 'text.secondary',
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
