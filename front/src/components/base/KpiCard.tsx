
import { Card, CardContent, Typography, Box } from '@mui/material'

interface KpiCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon?: React.ReactNode
  color?: string
}

/**
 * KpiCard - KPI 卡片组件
 * 应用设计系统规范：
 * - 背景：color-surface #1E293B
 * - 圆角：12px (border-radius-xl)
 * - 阴影：elevation-1
 * - 内边距：16px (spacing-md)
 */
export function KpiCard({ title, value, subtitle, icon, color = '#00D084' }: KpiCardProps) {
  return (
    <Card
      variant="outlined"
      sx={{
        backgroundColor: '#1E293B', // color-surface
        border: '1px solid #334155', // color-surface-light
        borderRadius: '12px', // border-radius-xl
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)', // shadow-elevation-1
        transition: 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)', // transition-base
        '&:hover': {
          borderColor: '#00D084', // color-primary
          boxShadow: '0 8px 24px rgba(0, 0, 0, 0.35)', // shadow-elevation-2
        },
      }}
    >
      <CardContent sx={{ padding: '16px' }}> {/* spacing-md */}
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography
              variant="body2"
              color="text.secondary"
              gutterBottom
              sx={{
                fontSize: '12px', // font-size-sm
                fontWeight: 500, // font-weight-medium
                color: '#475569', // color-text-secondary
                marginBottom: '8px', // spacing-sm
              }}
            >
              {title}
            </Typography>
            <Typography
              variant="h4"
              fontWeight={700}
              sx={{
                fontSize: '24px', // font-size-xl
                lineHeight: 1.3, // line-height-xl
                color: color,
                marginBottom: '4px', // spacing-xs
              }}
            >
              {value}
            </Typography>
            {subtitle && (
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                  fontSize: '11px', // font-size-xs
                  color: '#475569', // color-text-secondary
                }}
              >
                {subtitle}
              </Typography>
            )}
          </Box>
          {icon && (
            <Box sx={{ color, opacity: 0.8, fontSize: 40, flexShrink: 0 }}>{icon}</Box>
          )}
        </Box>
      </CardContent>
    </Card>
  )
}
