import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Divider,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import SaveIcon from '@mui/icons-material/Save'
import LockResetIcon from '@mui/icons-material/LockReset'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useMutation, useQuery } from '@tanstack/react-query'
import { authApi, type AuthProfile, type ProfileUpdateParams } from '@/api/auth'
import { roleScopeFromRoleCode } from '@/constants/roleRoutes'
import { useUserStore } from '@/stores'
import { getErrorMessage } from '@/utils/errorHandler'

const PROFILE_ENDPOINT = '/auth/profile'
const PROFILE_UPDATE_ENDPOINT = '/auth/profile/update'
const PASSWORD_ENDPOINT = '/auth/profile/change-password'

const ROLE_LABELS: Record<string, string> = {
  admin: '平台管理员',
  institution: '机构管理员',
  talent: '达人/主播',
  user: '普通用户',
}

interface ProfileForm {
  nickname: string
  email: string
  phone: string
  avatarUrl: string
}

const emptyForm: ProfileForm = {
  nickname: '',
  email: '',
  phone: '',
  avatarUrl: '',
}

function profileToForm(profile?: AuthProfile): ProfileForm {
  return {
    nickname: profile?.nickname ?? '',
    email: profile?.email ?? '',
    phone: profile?.phone ?? '',
    avatarUrl: profile?.avatarUrl ?? '',
  }
}

export default function ProfilePage() {
  const { userInfo, setUserInfo } = useUserStore()
  const [form, setForm] = useState<ProfileForm>(emptyForm)
  const [profileSaved, setProfileSaved] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [passwordForm, setPasswordForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [passwordSaved, setPasswordSaved] = useState(false)
  const [passwordError, setPasswordError] = useState('')

  const roleCode = userInfo?.roles?.[0] ?? 'user'
  const roleScope = roleScopeFromRoleCode(roleCode)
  const roleLabel = ROLE_LABELS[roleCode] ?? roleCode

  const {
    data: profile,
    error: loadError,
    isError: isLoadError,
    isFetching,
    refetch,
  } = useQuery({
    queryKey: ['auth-profile'],
    queryFn: authApi.profile,
  })

  useEffect(() => {
    if (profile) setForm(profileToForm(profile))
  }, [profile])

  const displayName = useMemo(
    () => form.nickname.trim() || profile?.username || userInfo?.username || '账号',
    [form.nickname, profile?.username, userInfo?.username],
  )

  const updateProfileMutation = useMutation({
    mutationFn: (payload: ProfileUpdateParams) => authApi.profileUpdate(payload),
    onSuccess: () => {
      setProfileSaved(true)
      setProfileError('')
      if (userInfo) {
        setUserInfo({ ...userInfo, nickname: form.nickname.trim() || userInfo.nickname })
      }
      void refetch()
    },
    onError: (error) => {
      setProfileSaved(false)
      setProfileError(`资料保存失败（POST ${PROFILE_UPDATE_ENDPOINT}）：${getErrorMessage(error)}。当前输入已保留。`)
    },
  })

  const changePasswordMutation = useMutation({
    mutationFn: () => authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    }),
    onSuccess: () => {
      setPasswordSaved(true)
      setPasswordError('')
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
    },
    onError: (error) => {
      setPasswordSaved(false)
      setPasswordError(`密码修改失败（POST ${PASSWORD_ENDPOINT}）：${getErrorMessage(error)}。当前输入已保留。`)
    },
  })

  const updateForm = (field: keyof ProfileForm, value: string) => {
    setProfileSaved(false)
    setProfileError('')
    setForm(prev => ({ ...prev, [field]: value }))
  }

  const updatePassword = (field: keyof typeof passwordForm, value: string) => {
    setPasswordSaved(false)
    setPasswordError('')
    setPasswordForm(prev => ({ ...prev, [field]: value }))
  }

  const handleSaveProfile = () => {
    updateProfileMutation.mutate({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      avatarUrl: form.avatarUrl.trim(),
    })
  }

  const handleChangePassword = () => {
    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      setPasswordSaved(false)
      setPasswordError('密码修改失败：旧密码和新密码不能为空。当前输入已保留。')
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordSaved(false)
      setPasswordError('密码修改失败：两次输入的新密码不一致。当前输入已保留。')
      return
    }
    changePasswordMutation.mutate()
  }

  return (
    <Box
      data-testid="profile-page"
      data-contract-scope="role-profile-center"
      data-role-scope={roleScope}
      data-ready-endpoints={[PROFILE_ENDPOINT, PROFILE_UPDATE_ENDPOINT, PASSWORD_ENDPOINT].join(',')}
      data-input-retained-on-error="true"
      sx={{ width: '100%', maxWidth: 1120, mx: 'auto' }}
    >
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'flex-start', sm: 'center' }}>
            <Avatar src={form.avatarUrl} sx={{ width: 56, height: 56, bgcolor: 'primary.main' }}>
              {displayName[0]}
            </Avatar>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h6" fontWeight={700}>个人中心</Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {profile?.username ?? userInfo?.username ?? '-'}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Chip size="small" label={roleLabel} color="primary" variant="outlined" />
              <Chip size="small" label={`后台：/${roleScope === 'org' ? 'org' : roleScope}`} variant="outlined" />
            </Stack>
          </Stack>
        </Paper>

        {isLoadError ? (
          <Alert
            data-testid="profile-load-error"
            data-input-retained="true"
            severity="warning"
            action={<Button size="small" startIcon={<RefreshIcon />} onClick={() => void refetch()}>重试</Button>}
          >
            个人资料加载失败（POST {PROFILE_ENDPOINT}）：{getErrorMessage(loadError)}。页面不会使用本地假资料覆盖你的输入。
          </Alert>
        ) : null}

        <Grid container spacing={2}>
          <Grid item xs={12} md={7}>
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="subtitle1" fontWeight={700}>账号资料</Typography>
                  <Typography variant="body2" color="text.secondary">
                    修改昵称、联系方式和头像地址，保存后同步到当前登录态。
                  </Typography>
                </Box>
                <Divider />
                {profileSaved ? <Alert severity="success">个人资料已保存。</Alert> : null}
                {profileError ? <Alert data-testid="profile-save-error" data-input-retained="true" severity="error">{profileError}</Alert> : null}
                <TextField
                  label="登录账号"
                  value={profile?.username ?? userInfo?.username ?? ''}
                  size="small"
                  fullWidth
                  disabled
                />
                <TextField
                  label="昵称"
                  value={form.nickname}
                  onChange={(event) => updateForm('nickname', event.target.value)}
                  size="small"
                  fullWidth
                  slotProps={{ htmlInput: { 'data-testid': 'profile-nickname-input' } }}
                />
                <TextField
                  label="邮箱"
                  value={form.email}
                  onChange={(event) => updateForm('email', event.target.value)}
                  size="small"
                  fullWidth
                  slotProps={{ htmlInput: { 'data-testid': 'profile-email-input' } }}
                />
                <TextField
                  label="手机号"
                  value={form.phone}
                  onChange={(event) => updateForm('phone', event.target.value)}
                  size="small"
                  fullWidth
                />
                <TextField
                  label="头像 URL"
                  value={form.avatarUrl}
                  onChange={(event) => updateForm('avatarUrl', event.target.value)}
                  size="small"
                  fullWidth
                />
                <Stack direction="row" justifyContent="flex-end">
                  <Button
                    variant="contained"
                    startIcon={<SaveIcon />}
                    disabled={updateProfileMutation.isPending || isFetching}
                    onClick={handleSaveProfile}
                  >
                    保存资料
                  </Button>
                </Stack>
              </Stack>
            </Paper>
          </Grid>

          <Grid item xs={12} md={5}>
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 1 }}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="subtitle1" fontWeight={700}>安全设置</Typography>
                  <Typography variant="body2" color="text.secondary">
                    修改密码只调用真实账号接口，失败时保留输入便于重试。
                  </Typography>
                </Box>
                <Divider />
                {passwordSaved ? <Alert severity="success">密码已修改。</Alert> : null}
                {passwordError ? <Alert data-testid="profile-password-error" data-input-retained="true" severity="error">{passwordError}</Alert> : null}
                <TextField
                  label="旧密码"
                  value={passwordForm.oldPassword}
                  onChange={(event) => updatePassword('oldPassword', event.target.value)}
                  type="password"
                  size="small"
                  fullWidth
                  slotProps={{ htmlInput: { 'data-testid': 'profile-old-password-input' } }}
                />
                <TextField
                  label="新密码"
                  value={passwordForm.newPassword}
                  onChange={(event) => updatePassword('newPassword', event.target.value)}
                  type="password"
                  size="small"
                  fullWidth
                  slotProps={{ htmlInput: { 'data-testid': 'profile-new-password-input' } }}
                />
                <TextField
                  label="确认新密码"
                  value={passwordForm.confirmPassword}
                  onChange={(event) => updatePassword('confirmPassword', event.target.value)}
                  type="password"
                  size="small"
                  fullWidth
                  slotProps={{ htmlInput: { 'data-testid': 'profile-confirm-password-input' } }}
                />
                <Stack direction="row" justifyContent="flex-end">
                  <Button
                    variant="outlined"
                    startIcon={<LockResetIcon />}
                    disabled={changePasswordMutation.isPending}
                    onClick={handleChangePassword}
                  >
                    修改密码
                  </Button>
                </Stack>
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      </Stack>
    </Box>
  )
}
