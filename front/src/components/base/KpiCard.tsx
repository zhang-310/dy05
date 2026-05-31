import { Card, CardContent, Typography, Box } from '@mui/material'
import { alpha, type Theme } from '@mui/material/styles'
import type { ReactNode } from 'react'

interface KpiCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon?: ReactNode
  color?: string
}

function resolveKpiColor(theme: Theme, color?: string) {
  if (!color) return theme.palette.primary.main
  return color
}

/**
 * KpiCard - KPI 卡片组件
 * 应用设计系统规范：
 * - 背景：background.paper
 * - 圆角：12px (border-radius-xl)
 * - 阴影：theme-aware elevation
 * - 内边距：16px (spacing-md)
 */
export function KpiCard({ title, value, subtitle, icon, color }: KpiCardProps) {
  return (
    <Card
      variant="outlined"
      data-testid="base-kpi-card-surface"
      data-kpi-tone={color ? 'custom' : 'primary'}
      sx={(theme) => ({
        backgroundColor: theme.palette.background.paper,
        border: '1px solid',
        borderColor: theme.palette.divider,
        borderRadius: '12px', // border-radius-xl
        boxShadow: `0 4px 12px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.28 : 0.08)}`,
        transition: 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-base
        '&:hover': {
          borderColor: resolveKpiColor(theme, color),
          boxShadow: `0 8px 24px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.34 : 0.14)}`,
        },
      })}
    >
      <CardContent sx={{ padding: '16px' }}> {/* spacing-md */}
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography
              data-testid="base-kpi-card-title"
              variant="body2"
              color="text.secondary"
              gutterBottom
              sx={{
                fontSize: '12px', // font-size-sm
                fontWeight: 500, // font-weight-medium
                color: 'text.secondary',
                marginBottom: '8px', // spacing-sm
              }}
            >
              {title}
            </Typography>
            <Typography
              data-testid="base-kpi-card-value"
              variant="h4"
              fontWeight={700}
              sx={(theme) => ({
                fontSize: '24px', // font-size-xl
                lineHeight: 1.3, // line-height-xl
                color: resolveKpiColor(theme, color),
                marginBottom: '4px', // spacing-xs
              })}
            >
              {value}
            </Typography>
            {subtitle && (
              <Typography
                data-testid="base-kpi-card-subtitle"
                variant="caption"
                color="text.secondary"
                sx={{
                  fontSize: '11px', // font-size-xs
                  color: 'text.secondary',
                }}
              >
                {subtitle}
              </Typography>
            )}
          </Box>
          {icon && (
            <Box
              data-testid="base-kpi-card-icon"
              sx={(theme) => ({ color: resolveKpiColor(theme, color), opacity: 0.8, fontSize: 40, flexShrink: 0 })}
            >
              {icon}
            </Box>
          )}
        </Box>
      </CardContent>
    </Card>
  )
}
