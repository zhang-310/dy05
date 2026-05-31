
import { Alert, Box, Button, Stack, Typography } from '@mui/material'
import HomeIcon from '@mui/icons-material/Home'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useLocation, useNavigate } from 'react-router-dom'

const NOT_FOUND_HOME_PATH = '/admin/dashboard'

export default function NotFoundPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const currentPath = `${location.pathname}${location.search}${location.hash}`

  return (
    <Box
      data-testid="not-found-page-surface"
      data-contract-scope="not-found-route-recovery"
      data-current-path={currentPath}
      data-home-path={NOT_FOUND_HOME_PATH}
      data-ready-actions="navigate-back|navigate-home"
      data-unsupported-actions="local-route-fallback|external-redirect|auto-retry"
      data-no-local-route-fallback="true"
      data-no-external-redirect="true"
      sx={{
        minHeight: '60vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        px: 2,
      }}
    >
      <Typography variant="h1" fontWeight={700} color="text.disabled">
        404
      </Typography>
      <Typography
        variant="h6"
        color="text.secondary"
        data-testid="not-found-title"
        data-contract-source="router-wildcard"
      >
        页面不存在
      </Typography>
      <Alert
        severity="warning"
        variant="outlined"
        sx={{ maxWidth: 560, width: '100%' }}
        data-testid="not-found-current-path-alert"
        data-contract-source="useLocation"
        data-current-path={currentPath}
        data-no-local-route-fallback="true"
      >
        当前路径未匹配任何前端路由：{currentPath}
      </Alert>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
        <Button
          variant="outlined"
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(-1)}
          data-testid="not-found-back-button"
          data-contract-source="navigate(-1)"
        >
          返回上一页
        </Button>
        <Button
          variant="contained"
          startIcon={<HomeIcon />}
          onClick={() => navigate(NOT_FOUND_HOME_PATH)}
          data-testid="not-found-home-button"
          data-contract-source="navigate-admin-dashboard"
          data-home-path={NOT_FOUND_HOME_PATH}
        >
          返回首页
        </Button>
      </Stack>
    </Box>
  )
}
