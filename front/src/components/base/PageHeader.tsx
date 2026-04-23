import { Box, Typography, Breadcrumbs, Link, Stack } from '@mui/material'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'

interface Crumb {
  label: string
  href?: string
}

interface PageHeaderProps {
  title: string
  subtitle?: string
  breadcrumbs?: Crumb[]
  actions?: React.ReactNode
}

/**
 * 页面标题区：颜色与排版跟随 MUI 主题（text.primary / text.secondary / primary）
 */
export function PageHeader({ title, subtitle, breadcrumbs, actions }: PageHeaderProps) {
  return (
    <Box sx={{ mb: 3 }}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumbs
          separator={<NavigateNextIcon fontSize="small" sx={{ color: 'text.secondary' }} />}
          sx={{
            mb: 1,
            '& .MuiBreadcrumbs-li': {
              fontSize: '0.75rem',
            },
          }}
        >
          {breadcrumbs.map((crumb, i) =>
            crumb.href ? (
              <Link
                key={i}
                href={crumb.href}
                underline="hover"
                color="inherit"
                sx={{ fontSize: '0.75rem', color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
              >
                {crumb.label}
              </Link>
            ) : (
              <Typography key={i} variant="caption" color="text.secondary" component="span">
                {crumb.label}
              </Typography>
            ),
          )}
        </Breadcrumbs>
      )}
      <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
        <Box>
          <Typography
            component="h1"
            variant="h4"
            sx={{
              fontWeight: 700,
              lineHeight: 1.2,
              color: 'text.primary',
            }}
          >
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions && <Box sx={{ display: 'flex', gap: 1, flexShrink: 0 }}>{actions}</Box>}
      </Stack>
    </Box>
  )
}
