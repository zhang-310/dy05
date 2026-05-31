import { Box, Typography } from '@mui/material'
import NavigateNextIcon from '@mui/icons-material/NavigateNext'
import { Link } from 'react-router-dom'
import { getBreadcrumbs } from '@/utils/breadcrumbs'

interface LayoutBreadcrumbsProps {
  pathname: string
}

export function LayoutBreadcrumbs({ pathname }: LayoutBreadcrumbsProps) {
  const crumbs = getBreadcrumbs(pathname)
  if (crumbs.length === 0) return null
  return (
    <Box
      aria-label="页面路径"
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 0.5,
        color: 'text.secondary',
        fontSize: 14,
        minWidth: 0,
        overflow: 'hidden',
      }}
    >
      {crumbs.map((crumb, i) => {
        const isLast = i === crumbs.length - 1
        return (
          <Box key={`${crumb.path ?? crumb.label}-${i}`} sx={{ display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0 }}>
            {i > 0 && <NavigateNextIcon sx={{ fontSize: 18, color: 'text.disabled', flexShrink: 0 }} />}
            {isLast || !crumb.path ? (
              <Typography
                component="span"
                noWrap
                sx={{ color: isLast ? 'text.primary' : 'text.secondary', fontWeight: isLast ? 500 : 400, minWidth: 0 }}
              >
                {crumb.label}
              </Typography>
            ) : (
              <Link to={crumb.path} style={{ color: 'inherit', textDecoration: 'none', whiteSpace: 'nowrap' }}>
                {crumb.label}
              </Link>
            )}
          </Box>
        )
      })}
    </Box>
  )
}
