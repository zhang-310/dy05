import { useState, useEffect, useCallback } from 'react'
import {
  Drawer,
  Box,
  Typography,
  TextField,
  Button,
  IconButton,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import type { AuthUserVO, AuthUserSaveVO, AuthRoleVO } from '@/types/auth'

interface UserEditDrawerProps {
  open: boolean
  user: AuthUserVO | null
  roles: AuthRoleVO[]
  onClose: () => void
  onSave: (data: AuthUserSaveVO) => Promise<void>
}

const EMPTY_FORM: AuthUserSaveVO = {
  username: '',
  nickname: '',
  mobile: '',
  email: '',
  roleCode: '',
  password: '',
}

export function UserEditDrawer({ open, user, roles, onClose, onSave }: UserEditDrawerProps) {
  const [form, setForm] = useState<AuthUserSaveVO>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  const isEdit = user != null

  useEffect(() => {
    if (!open) return
    if (user) {
      setForm({
        id: user.id,
        username: user.username,
        nickname: user.nickname ?? '',
        mobile: user.mobile ?? '',
        email: user.email ?? '',
        roleCode: user.roleCode,
        password: '',
      })
    } else {
      setForm({ ...EMPTY_FORM })
    }
  }, [open, user])

  const updateField = useCallback(<K extends keyof AuthUserSaveVO>(key: K, value: AuthUserSaveVO[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }, [])

  const handleSave = async () => {
    if (!form.username.trim()) return
    if (!form.roleCode) return
    if (!isEdit && !form.password?.trim()) return
    setSaving(true)
    try {
      const payload: AuthUserSaveVO = { ...form }
      if (isEdit && !payload.password?.trim()) {
        delete payload.password
      }
      await onSave(payload)
      onClose()
    } catch {
      // error handled by hook
    } finally {
      setSaving(false)
    }
  }

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{ sx: { width: { xs: '100%', sm: 500 } } }}
    >
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            {isEdit ? '编辑用户' : '新增用户'}
          </Typography>
          <IconButton size="small" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>

        <Box sx={{ flex: 1, overflow: 'auto', px: 2, py: 1.5 }}>
          {saving ? (
            <Box sx={{ py: 8, display: 'flex', justifyContent: 'center' }}>
              <CircularProgress />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField
                size="small"
                fullWidth
                label="用户名 *"
                value={form.username}
                onChange={(e) => updateField('username', e.target.value)}
                disabled={isEdit}
              />
              {!isEdit && (
                <TextField
                  size="small"
                  fullWidth
                  label="密码 *"
                  type="password"
                  value={form.password ?? ''}
                  onChange={(e) => updateField('password', e.target.value)}
                />
              )}
              <TextField
                size="small"
                fullWidth
                label="昵称"
                value={form.nickname ?? ''}
                onChange={(e) => updateField('nickname', e.target.value)}
              />
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5 }}>
                <TextField
                  size="small"
                  fullWidth
                  label="手机号"
                  value={form.mobile ?? ''}
                  onChange={(e) => updateField('mobile', e.target.value)}
                />
                <TextField
                  size="small"
                  fullWidth
                  label="邮箱"
                  value={form.email ?? ''}
                  onChange={(e) => updateField('email', e.target.value)}
                />
              </Box>
              <FormControl size="small" fullWidth>
                <InputLabel>角色 *</InputLabel>
                <Select
                  value={form.roleCode}
                  label="角色 *"
                  onChange={(e) => updateField('roleCode', e.target.value)}
                >
                  {roles.map((r) => (
                    <MenuItem key={r.roleCode} value={r.roleCode}>
                      {r.roleName || r.roleCode}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          )}
        </Box>

        <Box sx={{ px: 2, py: 1, borderTop: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
          <Button onClick={onClose}>取消</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存'}
          </Button>
        </Box>
      </Box>
    </Drawer>
  )
}
