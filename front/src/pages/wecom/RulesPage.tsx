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
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { wecomApi, type WcRobot, type WcRule } from '@/api/wecom'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ConfirmDialog, PageHeader } from '@/components/base'
import { normalizeArray } from '@/utils/response-normalize'
import { getErrorMessage } from '@/utils/errorHandler'
import { alpha, useTheme, type Palette } from '@mui/material/styles'

const TRIGGER_TYPES = [
  { value: 'manual', label: '手动触发', tone: 'primary' },
  { value: 'schedule', label: '定时触发', tone: 'success' },
  { value: 'event', label: '事件触发', tone: 'warning' },
  { value: 'gmv_milestone', label: 'GMV里程碑', tone: 'error' },
  { value: 'script_approve', label: '话术审批', tone: 'info' },
  { value: 'live_start', label: '开播提醒', tone: 'success' },
  { value: 'alert', label: '异常告警', tone: 'warning' },
] as const

const TRIGGER_MAP = Object.fromEntries(TRIGGER_TYPES.map(t => [t.value, t]))
type TriggerTone = typeof TRIGGER_TYPES[number]['tone']

const WECOM_RULE_READY_ENDPOINTS = [
  '/wecom/rule/list',
  '/wecom/rule/save',
  '/wecom/rule/delete',
  '/wecom/rule/update-status',
  '/wecom/robot/list',
  '/wecom/push',
] as const
const WECOM_RULE_UNSUPPORTED_ACTIONS = 'template-crud,template-reference'
const WECOM_RULE_TEMPLATE_ENDPOINT = '/wecom/template/*'
const WECOM_RULE_PUSH_ENDPOINT = '/wecom/push'

function triggerToneColor(palette: Palette, tone?: TriggerTone) {
  if (!tone) return palette.text.disabled
  return palette.mode === 'dark' ? palette[tone].light : palette[tone].main
}

function RuleCapabilityCards() {
  const items = [
    { label: '规则列表', status: 'ready', action: 'rule-list', endpoint: '/wecom/rule/list', detail: '/wecom/rule/list，前端可按机器人/触发类型兜底过滤' },
    { label: '规则启停', status: 'ready', action: 'rule-status', endpoint: '/wecom/rule/update-status', detail: '/wecom/rule/update-status' },
    { label: '手动触发', status: 'degraded', action: 'manual-trigger-via-push', endpoint: WECOM_RULE_PUSH_ENDPOINT, sourceField: 'rule.messageTemplate', detail: '复用 /wecom/push 直发规则模板内容' },
    { label: '模板表', status: 'degraded', action: 'template-crud', endpoint: WECOM_RULE_TEMPLATE_ENDPOINT, sourceField: 'rule.messageTemplate', detail: '未落库，暂不展示伪 CRUD' },
  ] as const
  return (
    <Grid container spacing={2} sx={{ mb: 2 }}>
      {items.map(item => (
        <Grid item xs={12} sm={6} md={3} key={item.label}>
          <Card
            variant="outlined"
            data-testid="wecom-rule-capability-card"
            data-contract-scope="wecom-rule"
            data-contract-status={item.status}
            data-contract-action={item.action}
            data-contract-endpoint={item.endpoint}
            data-source-field={'sourceField' in item ? item.sourceField : undefined}
            sx={{ height: '100%', borderColor: item.status === 'ready' ? 'success.light' : 'warning.light' }}
          >
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                {item.status === 'ready'
                  ? <CheckCircleIcon sx={{ fontSize: 17, color: 'success.main' }} />
                  : <WarningAmberIcon sx={{ fontSize: 17, color: 'warning.main' }} />}
                <Typography variant="subtitle2" fontWeight={700}>{item.label}</Typography>
              </Stack>
              <Chip size="small" label={item.status === 'ready' ? '已接入' : '显式降级'} color={item.status === 'ready' ? 'success' : 'warning'} variant="outlined" />
              <Typography variant="caption" color="text.secondary" display="block" mt={0.75}>{item.detail}</Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  )
}

// ===== Tab 1: 推送规则 =====
function RulesTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const theme = useTheme()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<WcRule>>({})
  const [deleteTarget, setDeleteTarget] = useState<WcRule | null>(null)
  const [manualDialogOpen, setManualDialogOpen] = useState(false)
  const [manualRule, setManualRule] = useState<WcRule | null>(null)
  const [manualResult, setManualResult] = useState<string | null>(null)
  const [actionError, setActionError] = useState('')

  const { data: robots, isError: robotsError, error: robotsErr, refetch: refetchRobots } = useQuery({
    queryKey: ['wc-robots-select'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => normalizeArray<WcRobot>(d),
  })

  const { data: rulesData, isLoading, isError: rulesError, error: rulesErr, refetch: refetchRules } = useQuery({
    queryKey: ['wc-rules-all'],
    queryFn: () => wecomApi.ruleList({ rows: 100 }),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<WcRule>) => { setActionError(''); return wecomApi.ruleSave(p as Parameters<typeof wecomApi.ruleSave>[0]) },
    onSuccess: () => { toast('保存成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }); setDrawerOpen(false) },
    onError: (e) => { setActionError(`/wecom/rule/save 保存失败：${getErrorMessage(e)}；ruleId=${form.id ?? '-'}，ruleName=${form.ruleName || '-'}，robotId=${form.robotId ?? '-'}`); toast('保存失败', 'error') },
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => { setActionError(''); return wecomApi.ruleDelete(id) },
    onSuccess: () => { toast('删除成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }); setDeleteTarget(null) },
    onError: (e) => { setActionError(`/wecom/rule/delete 删除失败：${getErrorMessage(e)}；ruleId=${deleteTarget?.id ?? '-'}，ruleName=${deleteTarget?.ruleName ?? '-'}，robotId=${deleteTarget?.robotId ?? '-'}`); toast('删除失败', 'error') },
  })
  const toggleMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => { setActionError(''); return wecomApi.ruleUpdateStatus(id, status) },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wc-rules-all'] }),
    onError: (e, variables) => {
      const rule = rules.find(item => item.id === variables.id)
      setActionError(`/wecom/rule/update-status 启停失败：${getErrorMessage(e)}；ruleId=${variables.id}，ruleName=${rule?.ruleName ?? '-'}，targetStatus=${variables.status}`)
      toast('启停失败', 'error')
    },
  })
  const manualMut = useMutation({
    mutationFn: (ruleId: number) => {
      setActionError('')
      const rule = rules.find(item => item.id === ruleId)
      if (!rule) throw new Error('规则不存在')
      return wecomApi.push({ robotId: rule.robotId, ruleId: rule.id, content: rule.messageTemplate })
    },
    onSuccess: () => {
      setManualResult('推送成功')
    },
    onError: (e) => setManualResult(`/wecom/push 手动触发失败：${getErrorMessage(e)}；ruleId=${manualRule?.id ?? '-'}，ruleName=${manualRule?.ruleName ?? '-'}，robotId=${manualRule?.robotId ?? '-'}`),
  })

  const rules = normalizeArray<WcRule>(rulesData)
  const enabledRules = rules.filter(rule => rule.status === 1).length

  function openAdd() { setForm({ robotId: robots?.[0]?.id, triggerType: 'manual', triggerConfig: '{}', status: 1 }); setActionError(''); setDrawerOpen(true) }
  function openEdit(r: WcRule) { setForm(r); setActionError(''); setDrawerOpen(true) }
  function openManual(rule: WcRule) { setManualRule(rule); setManualResult(null); setManualDialogOpen(true) }

  function handleSave() {
    const robotId = form.robotId ?? robots?.[0]?.id
    if (!robotId) { toast('请选择目标机器人', 'warning'); return }
    if (!String(form.ruleName ?? '').trim()) { toast('请填写规则名称', 'warning'); return }
    if (!String(form.triggerType ?? '').trim()) { toast('请选择触发类型', 'warning'); return }
    if (!String(form.messageTemplate ?? '').trim()) { toast('请填写消息模板', 'warning'); return }
    saveMut.mutate({ ...form, robotId, triggerConfig: String(form.triggerConfig ?? '').trim() || '{}' })
  }

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
    <Box
      data-testid="wecom-rules-workbench"
      data-contract-scope="wecom-rule"
      data-ready-endpoints={WECOM_RULE_READY_ENDPOINTS.join(',')}
      data-unsupported-actions={WECOM_RULE_UNSUPPORTED_ACTIONS}
      data-rule-count={rules.length}
      data-enabled-rule-count={enabledRules}
      data-no-local-rule-fallback="true"
      data-no-template-reference-fallback="true"
    >
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          { label: '规则总数', value: rules.length, hint: '后端 /wecom/rule/list' },
          { label: '启用规则', value: enabledRules, hint: '可用于直接推送' },
          { label: '机器人', value: robots?.length ?? 0, hint: '可绑定目标' },
          { label: '手动触发', value: '降级', hint: '使用 /wecom/push 直发模板内容' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Card
              variant="outlined"
              data-testid="wecom-rule-kpi-card"
              data-contract-status={item.label === '手动触发' ? 'degraded' : 'ready'}
              data-source-endpoint={item.label === '手动触发' ? WECOM_RULE_PUSH_ENDPOINT : undefined}
              data-source-field={item.label === '手动触发' ? 'rule.messageTemplate' : undefined}
            >
              <CardContent>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
                <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      {robotsError && (
        <Alert
          severity="warning"
          data-testid="wecom-rule-robot-select-error"
          data-contract-source="/wecom/robot/list"
          data-no-local-robot-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetchRobots()}>重试</Button>}
          sx={{ mb: 2 }}
        >
          /wecom/robot/list 机器人下拉加载失败：{getErrorMessage(robotsErr)}。保存规则前需要先恢复机器人接口。
        </Alert>
      )}
      {rulesError && (
        <Alert
          severity="error"
          data-testid="wecom-rule-list-error"
          data-contract-source="/wecom/rule/list"
          data-no-local-rule-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetchRules()}>重试</Button>}
          sx={{ mb: 2 }}
        >
          /wecom/rule/list 推送规则加载失败：{getErrorMessage(rulesErr)}
        </Alert>
      )}
      {actionError ? (
        <Alert
          severity="error"
          data-testid="wecom-rule-action-error"
          data-input-retained="true"
          data-row-retained-on-action-error="true"
          data-no-local-rule-fallback="true"
          sx={{ mb: 2 }}
        >
          {actionError}。失败不会本地移除规则或切换启停状态。
        </Alert>
      ) : null}
      <Alert
        severity="info"
        data-testid="wecom-rule-manual-trigger-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="degraded"
        data-contract-action="manual-trigger-via-push"
        data-contract-endpoint={WECOM_RULE_PUSH_ENDPOINT}
        data-source-field="rule.messageTemplate"
        sx={{ mb: 2 }}
      >
        手动触发当前复用 `/wecom/push` 直发规则的 `messageTemplate`，后端尚未提供独立规则执行审计流；页面不伪造模板引用或独立触发记录。
      </Alert>
      <Stack direction="row" justifyContent="flex-end" mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增规则</Button>
      </Stack>

      {isLoading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
      {!isLoading && !rulesError && rules.length === 0 && (
        <Alert
          severity="info"
          data-testid="wecom-rule-empty-no-fallback"
          data-contract-source="/wecom/rule/list"
          data-no-local-rule-fallback="true"
        >
          暂无推送规则，点击「新增规则」开始配置。
        </Alert>
      )}

      <Grid container spacing={2}>
        {rules.map(rule => {
          const tt = TRIGGER_MAP[rule.triggerType]
          const color = triggerToneColor(theme.palette, tt?.tone)
          return (
            <Grid item xs={12} sm={6} md={4} key={rule.id}>
              <Card
                variant="outlined"
                data-testid="wecom-rule-trigger-card-surface"
                data-contract-source="/wecom/rule/list"
                data-trigger-type={rule.triggerType}
                data-trigger-tone={tt?.tone ?? 'default'}
                data-trigger-color={color}
                data-rule-id={rule.id}
                data-robot-id={rule.robotId}
                data-no-template-reference-fallback="true"
                sx={{ borderLeft: '4px solid', borderLeftColor: color, height: '100%' }}
              >
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={1}>
                    <Box sx={{ flex: 1, mr: 1 }}>
                      <Typography variant="subtitle2" fontWeight={700} noWrap>{rule.ruleName}</Typography>
                      <Chip
                        label={tt?.label ?? rule.triggerType}
                        size="small"
                        data-testid="wecom-rule-trigger-chip-surface"
                        data-trigger-type={rule.triggerType}
                        data-trigger-tone={tt?.tone ?? 'default'}
                        data-trigger-color={color}
                        sx={{
                          mt: 0.5,
                          bgcolor: alpha(color, theme.palette.mode === 'dark' ? 0.2 : 0.1),
                          color,
                          border: '1px solid',
                          borderColor: alpha(color, theme.palette.mode === 'dark' ? 0.5 : 0.28),
                          fontSize: 11,
                          fontWeight: 700,
                        }}
                      />
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
                      onClick={() => openManual(rule)}
                      disabled={rule.status !== 1}
                      data-testid="wecom-rule-manual-trigger-action"
                      data-contract-status="degraded"
                      data-contract-action="manual-trigger-via-push"
                      data-contract-endpoint={WECOM_RULE_PUSH_ENDPOINT}
                      data-source-field="rule.messageTemplate"
                    >触发</Button>
                    <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(rule)}>编辑</Button>
                    <Button size="small" color="error" startIcon={<DeleteIcon />}
                      onClick={() => setDeleteTarget(rule)}>删除</Button>
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
          {actionError ? <Alert severity="error">{actionError}。保存失败会保留当前输入。</Alert> : null}
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button variant="contained" onClick={handleSave} disabled={saveMut.isPending}>保存</Button>
          </Stack>
        </Stack>
      </Drawer>

      {/* Manual push dialog */}
      <Dialog
        open={manualDialogOpen}
        onClose={() => setManualDialogOpen(false)}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          'data-testid': 'wecom-rule-manual-trigger-dialog',
          'data-contract-status': 'degraded',
          'data-contract-action': 'manual-trigger-via-push',
          'data-contract-endpoint': WECOM_RULE_PUSH_ENDPOINT,
          'data-source-field': 'rule.messageTemplate',
        } as Record<string, string>}
      >
        <DialogTitle>手动触发推送</DialogTitle>
        <DialogContent>
          {manualResult
            ? <Alert severity={manualResult.includes('成功') ? 'success' : 'error'}>{manualResult}</Alert>
            : <Typography variant="body2">
                确认手动触发规则「{manualRule?.ruleName ?? '-'}」的推送？endpoint=/wecom/push，ruleId={manualRule?.id ?? '-'}，robotId={manualRule?.robotId ?? '-'}。
              </Typography>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setManualDialogOpen(false)}>关闭</Button>
          {!manualResult && (
            <Button variant="contained" onClick={() => manualRule && manualMut.mutate(manualRule.id)}
              disabled={manualMut.isPending}>确认触发</Button>
          )}
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteTarget !== null} content={`确定要删除规则「${deleteTarget?.ruleName ?? '-'}」吗？endpoint=/wecom/rule/delete，ruleId=${deleteTarget?.id ?? '-'}，robotId=${deleteTarget?.robotId ?? '-'}`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending} />
    </Box>
  )
}
// ===== Tab 2: 消息模板 =====
function TemplatesTab() {
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

  const exampleTemplate = '【{直播间}】{主播} 即将开播，当前目标 GMV：{目标GMV}。'
  const currentVars = parseVars(exampleTemplate)

  return (
    <Box
      data-testid="wecom-rule-template-workbench"
      data-contract-scope="wecom-rule-template"
      data-contract-status="degraded"
      data-contract-endpoint={WECOM_RULE_TEMPLATE_ENDPOINT}
      data-fallback-field="rule.messageTemplate"
    >
      <Alert
        severity="warning"
        data-testid="wecom-rule-template-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="degraded"
        data-contract-endpoint={WECOM_RULE_TEMPLATE_ENDPOINT}
        data-fallback-field="rule.messageTemplate"
        sx={{ mb: 2 }}
      >
        消息模板 CRUD 后端接口尚未接入：当前 `WecomController` 只提供机器人、规则、日志和直接推送。模板能力暂以规则的 `messageTemplate` 字段承接。
      </Alert>
      <Card
        variant="outlined"
        data-testid="wecom-rule-template-fallback-surface"
        data-contract-status="local-planning"
        data-contract-endpoint="unavailable"
        data-source-field="rule.messageTemplate"
      >
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} gutterBottom>模板变量约定</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            规则消息可以继续使用变量占位符，后续接入模板表后可迁移为独立模板。
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>{renderHighlighted(exampleTemplate)}</Typography>
          <Stack direction="row" flexWrap="wrap" gap={0.5}>
            {currentVars.map(v => (
              <Chip
                key={v}
                label={`{${v}}`}
                size="small"
                color="warning"
                variant="outlined"
                data-testid="wecom-rule-template-variable-chip"
                data-contract-status="local-planning"
                data-contract-field="template-variable"
              />
            ))}
          </Stack>
        </CardContent>
      </Card>
    </Box>
  )
}

// ===== Main Page =====
export default function RulesPage() {
  const [tab, setTab] = useState(0)
  return (
    <Box
      data-testid="wecom-rule-page"
      data-contract-scope="wecom-rule"
      data-ready-endpoints={WECOM_RULE_READY_ENDPOINTS.join(',')}
      data-unsupported-actions={WECOM_RULE_UNSUPPORTED_ACTIONS}
      sx={{ p: 3 }}
    >
      <PageHeader
        title="企微推送规则"
        subtitle="规则 CRUD 和启停走真实接口；模板表尚未落库，当前以规则消息模板字段承接。"
        breadcrumbs={[{ label: '企微推送' }, { label: '规则' }]}
      />
      <RuleCapabilityCards />
      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="推送规则" />
        <Tab label="消息模板" />
      </Tabs>
      {tab === 0 && <RulesTab />}
      {tab === 1 && <TemplatesTab />}
    </Box>
  )
}
