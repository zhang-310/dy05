import { useState, useEffect, useCallback } from 'react'
import {
  Drawer,
  Box,
  Typography,
  TextField,
  Button,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import type { AuthRoleVO, AuthRoleSaveVO } from '@/types/auth'

interface RoleEditDrawerProps {
  open: boolean
  role: AuthRoleVO | null
  onClose: () => void
  onSave: (data: AuthRoleSaveVO) => Promise<void>
}

const EMPTY_FORM: AuthRoleSaveVO = {
  roleCode: '',
  roleName: '',
  sortOrder: 0,
  status: 1,
}

export function RoleEditDrawer({ open, role, onClose, onSave }: RoleEditDrawerProps) {
  const [form, setForm] = useState<AuthRoleSaveVO>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  const isEdit = role != null

  useEffect(() => {
    if (!open) return
    if (role) {
      setForm({
        id: role.id,
        roleCode: role.roleCode,
        roleName: role.roleName,
        sortOrder: role.sortOrder ?? 0,
        status: role.status ?? 1,
      })
    } else {
      setForm({ ...EMPTY_FORM })
    }
  }, [open, role])

  const updateField = useCallback(<K extends keyof AuthRoleSaveVO>(key: K, value: AuthRoleSaveVO[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }, [])

  const handleSave = async () => {
    if (!form.roleCode.trim()) return
    if (!form.roleName.trim()) return
    setSaving(true)
    try {
      await onSave(form)
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
      PaperProps={{ sx: { width: { xs: '100%', sm: 450 } } }}
    >
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            {isEdit ? '编辑角色' : '新增角色'}
          </Typography>
          <IconButton size="small" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>

        <Box sx={{ flex: 1, overflow: 'auto', px: 2, py: 1.5 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              size="small"
              fullWidth
              label="角色编码 *"
              value={form.roleCode}
              onChange={(e) => updateField('roleCode', e.target.value)}
              disabled={isEdit}
              placeholder="如 admin"
            />
            <TextField
              size="small"
              fullWidth
              label="角色名 *"
              value={form.roleName}
              onChange={(e) => updateField('roleName', e.target.value)}
              placeholder="如 管理员"
            />
            <TextField
              size="small"
              fullWidth
              label="排序号"
              type="number"
              value={form.sortOrder ?? 0}
              onChange={(e) => updateField('sortOrder', Number(e.target.value) || 0)}
            />
            <FormControl size="small" fullWidth>
              <InputLabel>状态</InputLabel>
              <Select
                value={form.status ?? 1}
                label="状态"
                onChange={(e) => updateField('status', Number(e.target.value))}
              >
                <MenuItem value={1}>正常</MenuItem>
                <MenuItem value={0}>禁用</MenuItem>
              </Select>
            </FormControl>
          </Box>
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
