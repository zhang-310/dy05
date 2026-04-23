import { Box, Card, CardContent, Typography } from '@mui/material'
import type { SxProps, Theme } from '@mui/material'

interface MetricCardProps {
  title: string
  value: string | number
  trend?: string
  icon: React.ReactNode
  onClick?: () => void
  sx?: SxProps<Theme>
}

export function MetricCard({ title, value, trend, icon, onClick, sx }: MetricCardProps) {
  return (
    <Card
      sx={{
        cursor: onClick ? 'pointer' : 'default',
        transition: 'box-shadow 0.2s',
        '&:hover': onClick ? { boxShadow: 2 } : {},
        ...sx,
      }}
      onClick={onClick}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              bgcolor: 'primary.main',
              color: 'white',
              borderRadius: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {icon}
          </Box>
          <Box>
            <Typography color="text.secondary" variant="body2">
              {title}
            </Typography>
            <Typography variant="h6">{value}</Typography>
            {trend && (
              <Typography variant="caption" color={trend.startsWith('↑') ? 'success.main' : trend.startsWith('↓') ? 'error.main' : 'text.secondary'}>
                {trend}
              </Typography>
            )}
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}
