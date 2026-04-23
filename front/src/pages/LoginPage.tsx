import { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Stack,
} from '@mui/material'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores'
import { useToast } from '@/contexts/ToastContext'
import { getValidatedReturnPathFromParam, RETURN_URL_QUERY } from '@/utils/login-redirect'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { setToken, setUserInfo } = useUserStore()
  const toast = useToast()

  const handleLogin = async () => {
    if (!username || !password) {
      toast('请输入用户名和密码', 'warning')
      return
    }
    setLoading(true)
    try {
      const result = await authApi.login({ username, password })
      setToken(result.token)
      localStorage.setItem('token', result.token)
      setUserInfo({ id: result.userId, username: result.username, nickname: result.nickname, roles: result.roleCode ? [result.roleCode] : [] })
      toast('登录成功', 'success')
      const next =
        getValidatedReturnPathFromParam(searchParams.get(RETURN_URL_QUERY)) ?? '/admin/dashboard'
      navigate(next, { replace: true })
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : '登录失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'grey.100',
      }}
    >
      <Card sx={{ width: 360 }} elevation={3}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight={700} textAlign="center" mb={3}>
            DY02 运营平台
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="用户名"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              fullWidth
              autoFocus
            />
            <TextField
              label="密码"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              fullWidth
              onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
            />
            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={handleLogin}
              disabled={loading}
            >
              {loading ? '登录中...' : '登录'}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  )
}
