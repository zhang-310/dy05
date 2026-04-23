import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Box, Button, CircularProgress, MenuItem, Paper, TextField, Typography,
} from '@mui/material'
import Grid from '@mui/material/Grid'
import { useToast } from '@/contexts/ToastContext'
import { liveApi } from '@/api/live'
import { douyinApi, type DyAccount, type DyPersona } from '@/api/douyin'

const SCRIPT_STYLES = [
  { value: 'conversational', label: '对话式' },
  { value: 'storytelling', label: '故事式' },
  { value: 'promotional', label: '促销式' },
  { value: 'educational', label: '知识分享' },
]

export default function LiveSessionFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const toast = useToast()

  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [accounts, setAccounts] = useState<DyAccount[]>([])
  const [personas, setPersonas] = useState<DyPersona[]>([])

  const [form, setForm] = useState({
    liveTitle: '',
    accountId: '',
    personaId: '',
    scriptStyle: 'conversational',
    scheduledTime: '',
    scheduledEndTime: '',
    liveDescription: '',
  })

  useEffect(() => {
    Promise.all([
      douyinApi.accountList({ page: 0, rows: 200 }),
      douyinApi.personaList(),
    ]).then(([accRes, personaRes]) => {
      const accList = accRes.list || []
      setAccounts(accList)
      setPersonas(personaRes || [])
    }).catch((e) => { console.error('Failed to load accounts/personas:', e); toast('加载账号和人设失败', 'error') })

    if (isEdit && id) {
      setLoading(true)
      liveApi.sessionGet(Number(id))
        .then((res) => {
          setForm({
            liveTitle: res.liveTitle || '',
            accountId: String(res.accountId || ''),
            personaId: String(res.personaId || ''),
            scriptStyle: res.scriptStyle || 'conversational',
            scheduledTime: res.scheduledTime || '',
            scheduledEndTime: res.scheduledEndTime || '',
            liveDescription: res.liveDescription || '',
          })
        })
        .catch(() => toast('加载场次信息失败', 'error'))
        .finally(() => setLoading(false))
    }
  }, [id, isEdit])

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  const handleSubmit = async () => {
    if (!form.liveTitle.trim()) { toast('请填写直播标题', 'warning'); return }
    if (!form.accountId) { toast('请选择抖音账号', 'warning'); return }
    setSaving(true)
    try {
      await liveApi.sessionSave({
        ...(isEdit ? { id: Number(id) } : {}),
        liveTitle: form.liveTitle,
        accountId: Number(form.accountId),
        personaId: form.personaId ? Number(form.personaId) : undefined,
        scriptStyle: form.scriptStyle,
        scheduledTime: form.scheduledTime || undefined,
        scheduledEndTime: form.scheduledEndTime || undefined,
        liveDescription: form.liveDescription || undefined,
      })
      toast(isEdit ? '场次已更新' : '场次已创建', 'success')
      navigate('/admin/live/sessions')
    } catch {
      toast(isEdit ? '更新失败' : '创建失败', 'error')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Box sx={{ p: 4, display: 'flex', justifyContent: 'center' }}><CircularProgress /></Box>

  return (
    <Box sx={{ p: 3, maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h5" sx={{ mb: 3 }}>
        {isEdit ? '编辑直播场次' : '新建直播场次'}
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField fullWidth label="直播标题" required value={form.liveTitle} onChange={handleChange('liveTitle')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="抖音账号" required value={form.accountId} onChange={handleChange('accountId')}>
              {accounts.map((a) => <MenuItem key={a.id} value={a.id}>{a.accountName}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="主播人设" value={form.personaId} onChange={handleChange('personaId')}>
              <MenuItem value="">不指定</MenuItem>
              {personas.map((p) => <MenuItem key={p.id} value={p.id}>{p.personaName}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="话术风格" value={form.scriptStyle} onChange={handleChange('scriptStyle')}>
              {SCRIPT_STYLES.map((s) => <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="计划开始时间" type="datetime-local" value={form.scheduledTime} onChange={handleChange('scheduledTime')} InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="计划结束时间" type="datetime-local" value={form.scheduledEndTime} onChange={handleChange('scheduledEndTime')} InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={3} label="直播描述" value={form.liveDescription} onChange={handleChange('liveDescription')} />
          </Grid>
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button variant="outlined" onClick={() => navigate('/admin/live/sessions')}>取消</Button>
              <Button variant="contained" onClick={handleSubmit} disabled={saving}>
                {saving ? <CircularProgress size={20} /> : (isEdit ? '保存' : '创建')}
              </Button>
            </Box>
          </Grid>
        </Grid>
      </Paper>
    </Box>
  )
}
