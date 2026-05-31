import { useState } from 'react'
import {
  Alert,
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Stack,
  CircularProgress,
} from '@mui/material'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores'
import { useToast } from '@/contexts/ToastContext'
import { getValidatedReturnPathFromParam, RETURN_URL_QUERY } from '@/utils/login-redirect'
import { getErrorMessage } from '@/utils/errorHandler'
import { roleDefaultPath } from '@/constants/roleRoutes'

const LOGIN_ENDPOINT = '/auth/login'
const LOGIN_UNSUPPORTED_ACTIONS = [
  'open-redirect',
  'login-loop-return',
  'local-auth-fallback',
  'anonymous-dashboard-entry',
]

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [loginError, setLoginError] = useState('')
  const [loginErrorSource, setLoginErrorSource] = useState('')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { setToken, setUserInfo } = useUserStore()
  const toast = useToast()
  const rawReturnUrl = searchParams.get(RETURN_URL_QUERY)
  const validatedReturnPath = getValidatedReturnPathFromParam(rawReturnUrl)
  const invalidReturnPath = Boolean(rawReturnUrl && !validatedReturnPath)

  const handleLogin = async () => {
    if (!username || !password) {
      const message = '请输入用户名和密码'
      setLoginError(message)
      setLoginErrorSource('client-validation')
      toast(message, 'warning')
      return
    }
    setLoading(true)
    setLoginError('')
    setLoginErrorSource('')
    try {
      const result = await authApi.login({ username, password })
      setToken(result.token)
      localStorage.setItem('token', result.token)
      setUserInfo({ id: result.userId, username: result.username, nickname: result.nickname, roles: result.roleCode ? [result.roleCode] : [] })
      toast('登录成功', 'success')
      const next = validatedReturnPath ?? roleDefaultPath(result.roleCode)
      navigate(next, { replace: true })
    } catch (e: unknown) {
      const message = getErrorMessage(e)
      setLoginError(message)
      setLoginErrorSource(LOGIN_ENDPOINT)
      toast(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box
      data-testid="login-page-surface"
      data-contract-scope="login-auth-route"
      data-ready-endpoints={LOGIN_ENDPOINT}
      data-unsupported-actions={LOGIN_UNSUPPORTED_ACTIONS.join('|')}
      data-return-url-query={RETURN_URL_QUERY}
      data-return-url-state={validatedReturnPath ? 'valid' : invalidReturnPath ? 'invalid' : 'empty'}
      data-validated-return-path={validatedReturnPath ?? 'none'}
      data-default-success-path="role-based"
      data-login-loading={String(loading)}
      data-error-source={loginErrorSource || 'none'}
      data-input-retained={loginError ? 'true' : 'false'}
      data-no-open-redirect="true"
      data-no-login-loop-return="true"
      data-no-local-auth-fallback="true"
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        px: 2,
      }}
    >
      <Card
        sx={{ width: '100%', maxWidth: 400 }}
        elevation={3}
        data-testid="login-auth-card"
        data-contract-source={LOGIN_ENDPOINT}
        data-return-url-state={validatedReturnPath ? 'valid' : invalidReturnPath ? 'invalid' : 'empty'}
      >
        <CardContent sx={{ p: 4 }}>
          <Typography
            variant="h5"
            fontWeight={700}
            textAlign="center"
            mb={3}
            data-testid="login-page-title"
            data-contract-source="static-brand-title"
          >
            DY05 运营平台
          </Typography>
          <Stack spacing={2}>
            {validatedReturnPath && (
              <Alert
                severity="info"
                variant="outlined"
                data-testid="login-return-url-valid-alert"
                data-contract-source={RETURN_URL_QUERY}
                data-validated-return-path={validatedReturnPath}
                data-no-open-redirect="true"
              >
                登录后返回：{validatedReturnPath}
              </Alert>
            )}
            {invalidReturnPath && (
              <Alert
                severity="warning"
                variant="outlined"
                data-testid="login-return-url-invalid-alert"
                data-contract-source={RETURN_URL_QUERY}
                data-fallback-path="role-based"
                data-no-open-redirect="true"
                data-no-login-loop-return="true"
              >
                回跳地址无效，登录成功后将进入控制台首页。
              </Alert>
            )}
            {loginError && (
              <Alert
                severity="error"
                data-testid="login-error-alert"
                data-contract-source={loginErrorSource || 'client-validation'}
                data-input-retained="true"
                data-no-local-auth-fallback="true"
              >
                登录失败：{loginError}
              </Alert>
            )}
            <TextField
              label="用户名"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              fullWidth
              autoFocus
              autoComplete="username"
              disabled={loading}
              onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
              inputProps={{
                'data-testid': 'login-username-input',
                'data-contract-source': 'user-input',
              }}
            />
            <TextField
              label="密码"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              fullWidth
              autoComplete="current-password"
              onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
              disabled={loading}
              inputProps={{
                'data-testid': 'login-password-input',
                'data-contract-source': 'user-input',
              }}
            />
            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={handleLogin}
              disabled={loading}
              startIcon={loading ? <CircularProgress color="inherit" size={16} /> : undefined}
              data-testid="login-submit-button"
              data-contract-source={LOGIN_ENDPOINT}
              data-disabled-reason={loading ? 'auth-login-pending' : 'none'}
            >
              {loading ? '登录中...' : '登录'}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  )
}
