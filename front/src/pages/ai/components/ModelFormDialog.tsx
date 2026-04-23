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
  Grid,
} from '@mui/material'

const PROVIDERS = ['ollama', 'deepseek', 'openai', 'glm', 'openclaw', '580ai', 'minimax', 'qwen']

export interface ModelFormData {
  id?: number
  modelName: string
  modelProvider: string
  modelVersion: string
  apiKey?: string
  maxTokens?: number
  temperature?: number
  status?: number
}

interface ModelFormDialogProps {
  open: boolean
  onClose: () => void
  initial?: ModelFormData | null
  onSubmit: (data: ModelFormData) => Promise<void>
}

export function ModelFormDialog({ open, onClose, initial, onSubmit }: ModelFormDialogProps) {
  const isEdit = !!initial?.id

  const getDefault = (): ModelFormData => ({
    modelName: '',
    modelProvider: 'ollama',
    modelVersion: '',
    apiKey: '',
    maxTokens: 2048,
    temperature: 0.7,
    status: 1,
  })

  const [form, setForm] = useState<ModelFormData>(getDefault())
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) {
      setForm(initial ? { ...getDefault(), ...initial } : getDefault())
    }
  }, [open, initial])

  const handleSubmit = async () => {
    if (!form.modelName?.trim() || !form.modelProvider?.trim() || !form.modelVersion?.trim()) return
    setSaving(true)
    try {
      await onSubmit(form)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? '编辑模型' : '添加模型'}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.5 }}>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="模型名称"
              value={form.modelName}
              onChange={(e) => setForm((f) => ({ ...f, modelName: e.target.value }))}
              required
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth required>
              <InputLabel>提供商</InputLabel>
              <Select
                label="提供商"
                value={form.modelProvider}
                onChange={(e) => setForm((f) => ({ ...f, modelProvider: e.target.value }))}
              >
                {PROVIDERS.map((p) => (
                  <MenuItem key={p} value={p}>
                    {p}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="模型版本"
              value={form.modelVersion}
              onChange={(e) => setForm((f) => ({ ...f, modelVersion: e.target.value }))}
              placeholder="如 deepseek-chat、qwen3:8b"
              required
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="API Key"
              type="password"
              value={form.apiKey ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, apiKey: e.target.value }))}
              placeholder="Ollama 可留空"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="Max Tokens"
              type="number"
              value={form.maxTokens ?? 2048}
              onChange={(e) => setForm((f) => ({ ...f, maxTokens: Number(e.target.value) || 2048 }))}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="Temperature"
              type="number"
              inputProps={{ step: 0.1, min: 0, max: 2 }}
              value={form.temperature ?? 0.7}
              onChange={(e) => setForm((f) => ({ ...f, temperature: Number(e.target.value) || 0.7 }))}
            />
          </Grid>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>状态</InputLabel>
              <Select
                label="状态"
                value={form.status ?? 1}
                onChange={(e) => setForm((f) => ({ ...f, status: Number(e.target.value) }))}
              >
                <MenuItem value={1}>可用</MenuItem>
                <MenuItem value={0}>不可用</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>取消</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={saving}>
          {saving ? '保存中...' : '保存'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
