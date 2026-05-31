import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  Alert, Box, Button, Card, CardContent, CircularProgress, MenuItem, Stack, TextField, Typography,
} from '@mui/material'
import Grid from '@mui/material/Grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'
import { liveApi, type LiveSessionSave } from '@/api/live'
import { douyinApi, type DyPersona } from '@/api/douyin'
import { getErrorMessage } from '@/utils/errorHandler'
import { inferLiveRouteScope, liveSessionListPath } from './liveRouteScope'

const SCRIPT_STYLES = [
  { value: 'conversational', label: '对话式' },
  { value: 'storytelling', label: '故事式' },
  { value: 'promotional', label: '促销式' },
  { value: 'educational', label: '知识分享' },
]

const LIVE_SESSION_FORM_READY_ENDPOINTS = {
  accounts: '/douyin/account/search',
  personas: '/douyin/persona/list',
  sessionGet: '/live/session/get',
  save: '/live/session/save',
} as const

const LIVE_SESSION_FORM_CONTEXT_ENDPOINTS = [
  LIVE_SESSION_FORM_READY_ENDPOINTS.accounts,
  LIVE_SESSION_FORM_READY_ENDPOINTS.personas,
  LIVE_SESSION_FORM_READY_ENDPOINTS.save,
]

const LIVE_SESSION_FORM_UNSUPPORTED_ACTIONS = [
  'product-selection',
  'script-generation',
  'shortvideo-project-create',
  'douyin-video-sync',
  'session-status-change',
]

export default function LiveSessionFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const location = useLocation()
  const routeScope = inferLiveRouteScope(location.pathname)
  const listPath = liveSessionListPath(routeScope)
  const toast = useToast()
  const qc = useQueryClient()

  const [form, setForm] = useState({
    liveTitle: '',
    accountId: '',
    personaId: '',
    scriptStyle: 'conversational',
    scheduledTime: '',
    scheduledEndTime: '',
    liveDescription: '',
  })
  const [formTouched, setFormTouched] = useState(false)
  const [saveError, setSaveError] = useState('')

  const {
    data: accountPage,
    isLoading: accountsLoading,
    isError: accountsIsError,
    error: accountsError,
    refetch: refetchAccounts,
  } = useQuery({
    queryKey: ['live-form-douyin-accounts'],
    queryFn: () => douyinApi.accountList({ page: 0, rows: 200 }),
  })

  const {
    data: personas = [],
    isLoading: personasLoading,
    isError: personasIsError,
    error: personasError,
    refetch: refetchPersonas,
  } = useQuery({
    queryKey: ['live-form-personas'],
    queryFn: () => douyinApi.personaList(),
  })

  const {
    data: session,
    isLoading: sessionLoading,
    isError: sessionIsError,
    error: sessionError,
    refetch: refetchSession,
  } = useQuery({
    queryKey: ['live-form-session', id],
    enabled: isEdit && Boolean(id),
    queryFn: () => liveApi.sessionGet(Number(id)),
  })

  useEffect(() => {
    if (session && !formTouched) {
      setForm({
        liveTitle: session.liveTitle || '',
        accountId: String(session.accountId || ''),
        personaId: String(session.personaId || ''),
        scriptStyle: session.scriptStyle || 'conversational',
        scheduledTime: session.scheduledTime || '',
        scheduledEndTime: session.scheduledEndTime || '',
        liveDescription: session.liveDescription || '',
      })
    }
  }, [formTouched, session])

  const accounts = accountPage?.list ?? []
  const dependencyLoading = accountsLoading || personasLoading || sessionLoading
  const dependencyError = accountsIsError || personasIsError || sessionIsError

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormTouched(true)
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  const saveMutation = useMutation({
    mutationFn: (body: Partial<LiveSessionSave>) => liveApi.sessionSave(body),
    onMutate: () => setSaveError(''),
    onSuccess: () => {
      toast(isEdit ? '场次已更新' : '场次已创建', 'success')
      qc.invalidateQueries({ queryKey: ['live-sessions'] })
      qc.invalidateQueries({ queryKey: ['live-form-session'] })
      navigate(listPath)
    },
    onError: (error) => {
      const message = getErrorMessage(error)
      setSaveError(`${LIVE_SESSION_FORM_READY_ENDPOINTS.save} ${isEdit ? '更新' : '创建'}场次失败：${message}`)
      toast(isEdit ? `更新失败：${message}` : `创建失败：${message}`, 'error')
    },
  })

  const buildPayload = (): Partial<LiveSessionSave> | null => {
    if (!form.liveTitle.trim()) { toast('请填写直播标题', 'warning'); return null }
    if (!form.accountId) { toast('请选择抖音账号', 'warning'); return null }
    return {
        ...(isEdit ? { id: Number(id) } : {}),
      liveTitle: form.liveTitle.trim(),
        accountId: Number(form.accountId),
        personaId: form.personaId ? Number(form.personaId) : undefined,
        scriptStyle: form.scriptStyle,
        scheduledTime: form.scheduledTime || undefined,
        scheduledEndTime: form.scheduledEndTime || undefined,
      liveDescription: form.liveDescription.trim() || undefined,
    }
  }

  const handleSubmit = () => {
    const payload = buildPayload()
    if (payload) {
      saveMutation.mutate(payload)
    }
  }

  if (sessionLoading) {
    return (
      <Box
        sx={{ p: 4, display: 'flex', justifyContent: 'center' }}
        data-testid="live-session-form-loading"
        data-contract-source={LIVE_SESSION_FORM_READY_ENDPOINTS.sessionGet}
      >
        <CircularProgress />
      </Box>
    )
  }

  const selectedAccount = accounts.find((a) => String(a.id) === form.accountId)
  const selectedPersona = (personas as DyPersona[]).find((p) => String(p.id) === form.personaId)

  return (
    <Box
      sx={{ p: 3, maxWidth: 980, mx: 'auto' }}
      data-testid="live-session-form-workbench"
      data-contract-scope={isEdit ? 'live-session-edit' : 'live-session-create'}
      data-route-scope={routeScope}
      data-ready-endpoints={Object.values(LIVE_SESSION_FORM_READY_ENDPOINTS).join('|')}
      data-context-endpoints={LIVE_SESSION_FORM_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={LIVE_SESSION_FORM_UNSUPPORTED_ACTIONS.join('|')}
    >
      <PageHeader
        title={isEdit ? '编辑直播场次' : '新建直播场次'}
        breadcrumbs={[{ label: '直播' }, { label: '场次' }, { label: isEdit ? '编辑' : '新建' }]}
        subtitle="创建直播场次会写入 `/live/session/save`；账号和人设来自抖音账号域，后续选品、话术和复盘在场次工作台完成。"
        actions={<Button size="small" onClick={() => { refetchAccounts(); refetchPersonas(); if (isEdit) refetchSession() }}>刷新依赖</Button>}
      />

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert
          severity="info"
          variant="outlined"
          data-testid="live-session-form-contract-alert"
          data-contract-source={LIVE_SESSION_FORM_READY_ENDPOINTS.save}
          data-dependency-source={`${LIVE_SESSION_FORM_READY_ENDPOINTS.accounts}|${LIVE_SESSION_FORM_READY_ENDPOINTS.personas}`}
          data-no-product-selection="true"
          data-no-script-generation="true"
          data-no-shortvideo-project-create="true"
        >
          必填字段为直播标题和抖音账号；人设、风格、计划时间和描述会随场次保存，创建后进入直播场次列表继续配置选品与话术。
        </Alert>
        {dependencyError && (
          <Alert
            severity="error"
            data-testid="live-session-form-dependency-error"
            data-contract-source={[
              accountsIsError ? LIVE_SESSION_FORM_READY_ENDPOINTS.accounts : '',
              personasIsError ? LIVE_SESSION_FORM_READY_ENDPOINTS.personas : '',
              sessionIsError ? LIVE_SESSION_FORM_READY_ENDPOINTS.sessionGet : '',
            ].filter(Boolean).join('|')}
            data-save-disabled="true"
          >
            依赖加载异常：
            {accountsIsError ? ` ${LIVE_SESSION_FORM_READY_ENDPOINTS.accounts} 抖音账号=${getErrorMessage(accountsError)}；` : ''}
            {personasIsError ? ` ${LIVE_SESSION_FORM_READY_ENDPOINTS.personas} 人设=${getErrorMessage(personasError)}；` : ''}
            {sessionIsError ? ` ${LIVE_SESSION_FORM_READY_ENDPOINTS.sessionGet} 场次详情=${getErrorMessage(sessionError)}；` : ''}
          </Alert>
        )}
        {saveError && (
          <Alert
            severity="error"
            data-testid="live-session-form-save-error"
            data-contract-source={LIVE_SESSION_FORM_READY_ENDPOINTS.save}
            data-input-preserved="true"
          >
            {saveError}
          </Alert>
        )}
      </Stack>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          { label: '可选账号', value: accounts.length, hint: LIVE_SESSION_FORM_READY_ENDPOINTS.accounts },
          { label: '可选人设', value: (personas as DyPersona[]).length, hint: LIVE_SESSION_FORM_READY_ENDPOINTS.personas },
          { label: '当前账号', value: selectedAccount?.accountName ?? '未选择', hint: '保存前必填' },
          { label: '当前人设', value: selectedPersona?.personaName ?? '不指定', hint: '可选' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Card
              variant="outlined"
              data-testid="live-session-form-dependency-card"
              data-contract-source={item.hint.startsWith('/') ? item.hint : LIVE_SESSION_FORM_READY_ENDPOINTS.save}
            >
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight={700} noWrap title={String(item.value)}>{item.value}</Typography>
                <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card
        variant="outlined"
        data-testid="live-session-form-editor"
        data-contract-source={LIVE_SESSION_FORM_READY_ENDPOINTS.save}
        data-no-status-change="true"
      >
        <CardContent>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField fullWidth label="直播标题" required value={form.liveTitle} onChange={handleChange('liveTitle')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="抖音账号" required value={form.accountId} onChange={handleChange('accountId')}>
              {accounts.length === 0 && <MenuItem disabled value="">暂无可选账号</MenuItem>}
              {accounts.map((a) => <MenuItem key={a.id} value={a.id}>{a.accountName}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="主播人设" value={form.personaId} onChange={handleChange('personaId')}>
              <MenuItem value="">不指定</MenuItem>
              {(personas as DyPersona[]).map((p) => <MenuItem key={p.id} value={p.id}>{p.personaName}</MenuItem>)}
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
              <Button variant="outlined" onClick={() => navigate(listPath)}>取消</Button>
              <Button variant="contained" onClick={handleSubmit} disabled={saveMutation.isPending || dependencyLoading || dependencyError}>
                {saveMutation.isPending ? <CircularProgress size={20} /> : (isEdit ? '保存' : '创建')}
              </Button>
            </Box>
          </Grid>
        </Grid>
        </CardContent>
      </Card>
    </Box>
  )
}
