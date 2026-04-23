import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
} from '@mui/material'

export interface FormFieldDef {
  key: string
  label: string
  type?: 'text' | 'number' | 'password' | 'select' | 'datetime-local'
  required?: boolean
  options?: { value: string | number; label: string }[]
  multiline?: boolean
  /** 编辑时隐藏（如密码） */
  hideOnEdit?: boolean
  /** 自定义渲染（用于图片上传等） */
  render?: (value: unknown, onChange: (v: unknown) => void, error?: string) => React.ReactNode
}

interface FormDialogProps {
  open: boolean
  onClose: () => void
  title: string
  fields: FormFieldDef[]
  initialValues?: Record<string, unknown>
  onSubmit: (data: Record<string, unknown>) => Promise<void>
  submitLabel?: string
  /** 表单顶部额外内容，可用于「从链接提取」等，updateValues 可批量更新表单 */
  extraTopContent?: (values: Record<string, unknown>, updateValues: (updates: Record<string, unknown>) => void) => React.ReactNode
}

export function FormDialog({
  open,
  onClose,
  title,
  fields,
  initialValues = {},
  onSubmit,
  submitLabel = '保存',
  extraTopContent,
}: FormDialogProps) {
  const [values, setValues] = useState<Record<string, unknown>>({})
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})

  const isEdit = !!initialValues?.id

  useEffect(() => {
    if (open) {
      setValues(initialValues)
      setErrors({})
    }
  }, [open, initialValues])

  const handleChange = (key: string, value: unknown) => {
    setValues((prev) => ({ ...prev, [key]: value }))
    if (errors[key]) setErrors((prev) => ({ ...prev, [key]: '' }))
  }

  const updateValues = (updates: Record<string, unknown>) => {
    setValues((prev) => ({ ...prev, ...updates }))
    Object.keys(updates).forEach((k) => {
      if (errors[k]) setErrors((prev) => ({ ...prev, [k]: '' }))
    })
  }

  const validate = (): boolean => {
    const next: Record<string, string> = {}
    for (const f of fields) {
      if (f.required && (values[f.key] === undefined || values[f.key] === '')) {
        next[f.key] = `请输入${f.label}`
      }
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = async () => {
    if (!validate()) return
    setLoading(true)
    try {
      await onSubmit(values)
      onClose()
    } catch (e) {
      setErrors({ _form: e instanceof Error ? e.message : '保存失败' })
    } finally {
      setLoading(false)
    }
  }

  const visibleFields = fields.filter((f) => !(isEdit && f.hideOnEdit))

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {extraTopContent?.(values, updateValues)}
          {errors._form && (
            <Box sx={{ color: 'error.main', fontSize: 14 }}>{errors._form}</Box>
          )}
          {visibleFields.map((f) => (
            <Box key={f.key}>
              {f.render ? (
                f.render(values[f.key], (v) => handleChange(f.key, v), errors[f.key])
              ) : f.type === 'select' ? (
                <FormControl fullWidth size="small">
                  <InputLabel>{f.label}</InputLabel>
                  <Select
                    value={values[f.key] ?? ''}
                    label={f.label}
                    onChange={(e) => handleChange(f.key, e.target.value)}
                  >
                    {f.options?.map((o) => (
                      <MenuItem key={String(o.value)} value={o.value}>
                        {o.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              ) : (
                <TextField
                  fullWidth
                  size="small"
                  label={f.label}
                  type={f.type === 'password' ? 'password' : f.type === 'number' ? 'number' : f.type === 'datetime-local' ? 'datetime-local' : 'text'}
                  value={values[f.key] ?? ''}
                  onChange={(e) => handleChange(f.key, f.type === 'number' ? Number(e.target.value) : e.target.value)}
                  InputLabelProps={f.type === 'datetime-local' ? { shrink: true } : undefined}
                  error={!!errors[f.key]}
                  helperText={errors[f.key]}
                  multiline={f.multiline}
                  rows={f.multiline ? 3 : undefined}
                />
              )}
            </Box>
          ))}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={loading}>
          {loading ? '保存中...' : submitLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
