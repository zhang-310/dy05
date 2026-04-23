import { useState } from 'react'
import {
  Box, Typography, Stack, Button, Chip, Tab, Tabs, Card, CardContent,
  Drawer, TextField, FormControl, InputLabel, Select, MenuItem,
  Switch, FormControlLabel, Divider, Grid, Dialog, DialogTitle,
  DialogContent, DialogActions, Alert, CircularProgress,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { wecomApi, type WcRule, type WcMessageTemplate } from '@/api/wecom'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ConfirmDialog } from '@/components/base'

const TRIGGER_TYPES = [
  { value: 'manual', label: '手动触发', color: '#5470c6' },
  { value: 'schedule', label: '定时触发', color: '#91cc75' },
  { value: 'event', label: '事件触发', color: '#fac858' },
  { value: 'gmv_milestone', label: 'GMV里程碑', color: '#ee6666' },
  { value: 'script_approve', label: '话术审批', color: '#73c0de' },
  { value: 'live_start', label: '开播提醒', color: '#3ba272' },
  { value: 'alert', label: '异常告警', color: '#fc8452' },
]

const TRIGGER_MAP = Object.fromEntries(TRIGGER_TYPES.map(t => [t.value, t]))

// ===== Tab 1: 推送规则 =====
function RulesTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<WcRule>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [manualDialogOpen, setManualDialogOpen] = useState(false)
  const [manualRuleId, setManualRuleId] = useState<number | null>(null)
  const [manualResult, setManualResult] = useState<string | null>(null)

  const { data: robots } = useQuery({
    queryKey: ['wc-robots-select'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data: rulesData, isLoading } = useQuery({
    queryKey: ['wc-rules-all'],
    queryFn: () => wecomApi.ruleList({ rows: 100 }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<WcRule>) => wecomApi.ruleSave(p as Parameters<typeof wecomApi.ruleSave>[0]),
    onSuccess: () => { toast('保存成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }); setDrawerOpen(false) },
    onError: () => toast('保存失败', 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => wecomApi.ruleDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }); setDeleteId(null) },
    onError: () => toast('删除失败', 'error'),
  })
  const toggleMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => wecomApi.ruleUpdateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wc-rules-all'] }),
    onError: () => toast('操作失败', 'error'),
  })
  const manualMut = useMutation({
    mutationFn: (ruleId: number) => wecomApi.manualPush(ruleId),
    onSuccess: (res) => {
      setManualResult(res.success ? `推送成功，耗时 ${res.costMs ?? 0}ms` : '推送失败')
    },
    onError: () => setManualResult('推送失败'),
  })

  const rules = rulesData?.list ?? []

  function openAdd() { setForm({}); setDrawerOpen(true) }
  function openEdit(r: WcRule) { setForm(r); setDrawerOpen(true) }
  function openManual(id: number) { setManualRuleId(id); setManualResult(null); setManualDialogOpen(true) }

  function renderDynamicConditions() {
    const tt = form.triggerType
    if (tt === 'schedule') {
      return (
        <TextField label="Cron 表达式" fullWidth size="small"
          placeholder="例：0 9 * * 1-5（工作日9点）"
          value={form.triggerConfig ?? ''}
          onChange={e => setForm(p => ({ ...p, triggerConfig: e.target.value }))} />
      )
    }
    if (tt === 'gmv_milestone') {
      return (
        <TextField label="GMV 里程碑（元）" fullWidth size="small" type="number"
          placeholder="例：100000"
          value={form.triggerConfig ?? ''}
          onChange={e => setForm(p => ({ ...p, triggerConfig: e.target.value }))} />
      )
    }
    if (tt === 'event' || tt === 'alert') {
      return (
        <TextField label="事件标识符" fullWidth size="small"
          placeholder="例：live.error / order.refund"
          value={form.triggerConfig ?? ''}
          onChange={e => setForm(p => ({ ...p, triggerConfig: e.target.value }))} />
      )
    }
    return null
  }
  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增规则</Button>
      </Stack>

      {isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {!isLoading && rules.length === 0 && (
        <Alert severity="info">暂无推送规则，点击「新增规则」开始配置。</Alert>
      )}

      <Grid container spacing={2}>
        {rules.map(rule => {
          const tt = TRIGGER_MAP[rule.triggerType]
          const color = tt?.color ?? '#999'
          return (
            <Grid item xs={12} sm={6} md={4} key={rule.id}>
              <Card variant="outlined" sx={{ borderLeft: `4px solid ${color}`, height: '100%' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={1}>
                    <Box sx={{ flex: 1, mr: 1 }}>
                      <Typography variant="subtitle2" fontWeight={700} noWrap>{rule.ruleName}</Typography>
                      <Chip label={tt?.label ?? rule.triggerType} size="small"
                        sx={{ mt: 0.5, bgcolor: color, color: '#fff', fontSize: 11 }} />
                    </Box>
                    <Switch size="small" checked={rule.status === 1}
                      onChange={e => toggleMut.mutate({ id: rule.id, status: e.target.checked ? 1 : 0 })} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5, fontSize: 12,
                    overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box',
                    WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                    {rule.messageTemplate}
                  </Typography>
                  <Divider sx={{ mb: 1 }} />
                  <Stack direction="row" spacing={0.5}>
                    <Button size="small" startIcon={<PlayArrowIcon />}
                      onClick={() => openManual(rule.id)} disabled={rule.status !== 1}>触发</Button>
                    <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(rule)}>编辑</Button>
                    <Button size="small" color="error" startIcon={<DeleteIcon />}
                      onClick={() => setDeleteId(rule.id)}>删除</Button>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>

      {/* Edit Drawer */}
      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}
        PaperProps={{ sx: { width: 440, p: 3 } }}>
        <Typography variant="h6" fontWeight={700} mb={2}>{form.id ? '编辑规则' : '新增规则'}</Typography>
        <Stack spacing={2}>
          <TextField label="规则名称" fullWidth size="small" value={form.ruleName ?? ''}
            onChange={e => setForm(p => ({ ...p, ruleName: e.target.value }))} />
          <FormControl fullWidth size="small">
            <InputLabel>目标机器人</InputLabel>
            <Select label="目标机器人" value={form.robotId ?? ''}
              onChange={e => setForm(p => ({ ...p, robotId: e.target.value as number }))}>
              {(robots ?? []).map((r) => <MenuItem key={r.id} value={r.id}>{r.robotName}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl fullWidth size="small">
            <InputLabel>触发类型</InputLabel>
            <Select label="触发类型" value={form.triggerType ?? ''}
              onChange={e => setForm(p => ({ ...p, triggerType: e.target.value, triggerConfig: '' }))}>
              {TRIGGER_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
            </Select>
          </FormControl>
          {renderDynamicConditions()}
          <TextField label="消息模板（支持 {变量} 占位符）" fullWidth multiline minRows={4} size="small"
            value={form.messageTemplate ?? ''}
            onChange={e => setForm(p => ({ ...p, messageTemplate: e.target.value }))} />
          <FormControlLabel
            control={<Switch checked={form.status === 1} onChange={e => setForm(p => ({ ...p, status: e.target.checked ? 1 : 0 }))} />}
            label="启用规则" />
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button variant="contained" onClick={() => saveMut.mutate(form)} disabled={saveMut.isPending}>保存</Button>
          </Stack>
        </Stack>
      </Drawer>

      {/* Manual push dialog */}
      <Dialog open={manualDialogOpen} onClose={() => setManualDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>手动触发推送</DialogTitle>
        <DialogContent>
          {manualResult
            ? <Alert severity={manualResult.includes('成功') ? 'success' : 'error'}>{manualResult}</Alert>
            : <Typography variant="body2">确认手动触发该规则的推送？</Typography>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setManualDialogOpen(false)}>关闭</Button>
          {!manualResult && (
            <Button variant="contained" onClick={() => manualRuleId && manualMut.mutate(manualRuleId)}
              disabled={manualMut.isPending}>确认触发</Button>
          )}
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该规则吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending} />
    </Box>
  )
}
// ===== Tab 2: 消息模板 =====
function TemplatesTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<WcMessageTemplate>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [previewVars, setPreviewVars] = useState<Record<string, string>>({})

  const { data: robots } = useQuery({
    queryKey: ['wc-robots-select'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => d?.list ?? [],
  })
  const { data: tplData, isLoading } = useQuery({
    queryKey: ['wc-templates'],
    queryFn: () => wecomApi.templateList({ rows: 100 }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<WcMessageTemplate>) => wecomApi.templateSave(p),
    onSuccess: () => { toast('保存成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-templates'] }); setDrawerOpen(false) },
    onError: () => toast('保存失败', 'error'),
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => wecomApi.templateDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-templates'] }); setDeleteId(null) },
    onError: () => toast('删除失败', 'error'),
  })

  const templates = tplData?.list ?? []

  function parseVars(content: string): string[] {
    const matches = content.match(/\{([^}]+)\}/g) ?? []
    return [...new Set(matches.map(m => m.slice(1, -1)))]
  }

  function renderHighlighted(content: string) {
    const parts = content.split(/(\{[^}]+\})/g)
    return parts.map((p, i) =>
      /^\{[^}]+\}$/.test(p)
        ? <Box key={i} component="span" sx={{ color: 'warning.main', fontWeight: 600 }}>{p}</Box>
        : <span key={i}>{p}</span>
    )
  }

  function renderPreview(content: string, vars: Record<string, string>) {
    let result = content
    Object.entries(vars).forEach(([k, v]) => {
      result = result.split(`{${k}}`).join(v || `{${k}}`)
    })
    return result
  }

  function openEdit(tpl: WcMessageTemplate) {
    setForm(tpl)
    const vars = parseVars(tpl.templateContent)
    const initVars: Record<string, string> = {}
    vars.forEach(v => { initVars[v] = tpl.exampleValues?.[v] ?? '' })
    setPreviewVars(initVars)
    setDrawerOpen(true)
  }
  function openAdd() { setForm({}); setPreviewVars({}); setDrawerOpen(true) }

  const currentVars = parseVars(form.templateContent ?? '')

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建模板</Button>
      </Stack>

      {isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {!isLoading && templates.length === 0 && <Alert severity="info">暂无消息模板。</Alert>}

      <Grid container spacing={2}>
        {templates.map(tpl => (
          <Grid item xs={12} sm={6} md={4} key={tpl.id}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                  <Typography variant="subtitle2" fontWeight={700} noWrap sx={{ flex: 1, mr: 1 }}>{tpl.templateName}</Typography>
                  <Chip label={tpl.status === 1 ? '启用' : '禁用'} size="small"
                    color={tpl.status === 1 ? 'success' : 'default'} variant="outlined" />
                </Stack>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1, fontSize: 12,
                  overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box',
                  WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                  {renderHighlighted(tpl.templateContent)}
                </Typography>
                {tpl.variables?.length > 0 && (
                  <Stack direction="row" flexWrap="wrap" gap={0.5} mb={1}>
                    {tpl.variables.map(v => (
                      <Chip key={v} label={`{${v}}`} size="small"
                        sx={{ bgcolor: 'warning.50', color: 'warning.dark', fontSize: 11 }} />
                    ))}
                  </Stack>
                )}
                <Divider sx={{ mb: 1 }} />
                <Stack direction="row" spacing={0.5}>
                  <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(tpl)}>编辑</Button>
                  <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteId(tpl.id)}>删除</Button>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Edit Drawer */}
      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}
        PaperProps={{ sx: { width: 500, p: 3, overflowY: 'auto' } }}>
        <Typography variant="h6" fontWeight={700} mb={2}>{form.id ? '编辑模板' : '新建模板'}</Typography>
        <Stack spacing={2}>
          <TextField label="模板名称" fullWidth size="small" value={form.templateName ?? ''}
            onChange={e => setForm(p => ({ ...p, templateName: e.target.value }))} />
          <FormControl fullWidth size="small">
            <InputLabel>绑定机器人</InputLabel>
            <Select label="绑定机器人" value={form.robotId ?? ''}
              onChange={e => setForm(p => ({ ...p, robotId: e.target.value as number }))}>
              {(robots ?? []).map((r) => <MenuItem key={r.id} value={r.id}>{r.robotName}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="模板内容（变量用 {变量名} 表示）" fullWidth multiline minRows={4} size="small"
            value={form.templateContent ?? ''}
            onChange={e => {
              const content = e.target.value
              setForm(p => ({ ...p, templateContent: content }))
              const vars = parseVars(content)
              setPreviewVars(prev => {
                const next: Record<string, string> = {}
                vars.forEach(v => { next[v] = prev[v] ?? '' })
                return next
              })
            }} />
          {currentVars.length > 0 && (
            <Box sx={{ p: 1.5, bgcolor: 'grey.50', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
              <Typography variant="caption" fontWeight={600} display="block" mb={1}>变量预览值：</Typography>
              <Stack spacing={1}>
                {currentVars.map(v => (
                  <Stack key={v} direction="row" alignItems="center" spacing={1}>
                    <Box component="span" sx={{ color: 'warning.main', fontWeight: 600, minWidth: 80, fontSize: 13 }}>{`{${v}}`}</Box>
                    <TextField size="small" placeholder={`示例值`} sx={{ flex: 1 }}
                      value={previewVars[v] ?? ''}
                      onChange={e => setPreviewVars(prev => ({ ...prev, [v]: e.target.value }))} />
                  </Stack>
                ))}
              </Stack>
            </Box>
          )}
          {form.templateContent && (
            <Box sx={{ p: 1.5, bgcolor: 'primary.50', borderRadius: 1, border: '1px solid', borderColor: 'primary.200' }}>
              <Typography variant="caption" fontWeight={600} display="block" mb={0.5}>实时预览：</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {renderPreview(form.templateContent, previewVars)}
              </Typography>
            </Box>
          )}
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button variant="contained" onClick={() => saveMut.mutate(form)} disabled={saveMut.isPending}>保存</Button>
          </Stack>
        </Stack>
      </Drawer>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该模板吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending} />
    </Box>
  )
}

// ===== Main Page =====
export default function RulesPage() {
  const [tab, setTab] = useState(0)
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" fontWeight={700} mb={2}>企微推送规则</Typography>
      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="推送规则" />
        <Tab label="消息模板" />
      </Tabs>
      {tab === 0 && <RulesTab />}
      {tab === 1 && <TemplatesTab />}
    </Box>
  )
}