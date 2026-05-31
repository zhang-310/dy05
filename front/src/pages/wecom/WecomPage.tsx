import { useState } from 'react'
import {
  Box, Typography, Stack, Button, Chip, Tab, Tabs, Paper,
  Drawer, TextField, FormControl, InputLabel, Select, MenuItem,
  Switch, FormControlLabel, Divider, Grid, Dialog, DialogTitle,
  DialogContent, DialogActions, Alert, LinearProgress, Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import SendIcon from '@mui/icons-material/Send'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { alpha } from '@mui/material/styles'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay } from '@/components/base'
import {
  wecomApi,
  type WcRobot, type WcRule, type WcPushLog,
  type RobotSave, type RuleSave,
} from '@/api/wecom'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeArray, readTotal } from '@/utils/response-normalize'
import { getErrorMessage } from '@/utils/errorHandler'

// ─── Constants ───────────────────────────────────────────────────────────────

type TriggerTone = 'success' | 'warning' | 'primary' | 'error' | 'secondary' | 'info' | 'default'

const TRIGGER_TYPES = [
  { value: 'live_start',     label: '开播提醒',   tone: 'success' },
  { value: 'gmv_milestone',  label: 'GMV里程碑',  tone: 'warning' },
  { value: 'script_approve', label: '话术审批',   tone: 'primary' },
  { value: 'alert',          label: '系统告警',   tone: 'error' },
  { value: 'schedule',       label: '定时触发',   tone: 'default' },
  { value: 'manual',         label: '手动触发',   tone: 'secondary' },
  { value: 'event',          label: '事件触发',   tone: 'warning' },
] satisfies Array<{ value: string; label: string; tone: TriggerTone }>
const TRIGGER_MAP = Object.fromEntries(TRIGGER_TYPES.map(t => [t.value, t]))

const WECOM_READY_ENDPOINTS = [
  '/wecom/robot/list',
  '/wecom/robot/save',
  '/wecom/robot/delete',
  '/wecom/robot/update-status',
  '/wecom/rule/list',
  '/wecom/rule/save',
  '/wecom/rule/delete',
  '/wecom/rule/update-status',
  '/wecom/push',
  '/wecom/log/list',
] as const
const WECOM_UNSUPPORTED_ACTIONS = 'template-crud,push-stats,log-retry'
const WECOM_TEMPLATE_ENDPOINT = '/wecom/template/*'
const WECOM_STATS_ENDPOINT = '/wecom/statistics/summary'
const WECOM_LOG_RETRY_ENDPOINT = '/wecom/log/retry'

// ─── Helpers ─────────────────────────────────────────────────────────────────

function WecomCapabilityCards() {
  const items = [
    { label: '机器人 CRUD', status: 'ready', action: 'robot-crud', endpoint: '/wecom/robot/*', detail: '/wecom/robot/list|get|save|delete|update-status' },
    { label: '规则 CRUD', status: 'ready', action: 'rule-crud', endpoint: '/wecom/rule/*', detail: '/wecom/rule/list|get|save|delete|update-status' },
    { label: '直连推送', status: 'ready', action: 'direct-push', endpoint: '/wecom/push', detail: '/wecom/push 写入消息日志' },
    { label: '模板中心', status: 'degraded', action: 'template-crud', endpoint: WECOM_TEMPLATE_ENDPOINT, detail: '未落库，暂由规则 messageTemplate 承接' },
    { label: '推送统计', status: 'degraded', action: 'push-stats', endpoint: WECOM_STATS_ENDPOINT, detail: '未提供独立统计接口，页面基于日志列表聚合' },
    { label: '失败重发', status: 'unsupported', action: 'log-retry', endpoint: WECOM_LOG_RETRY_ENDPOINT, detail: '未提供 /wecom/log/retry，不展示可点击假动作' },
  ] as const
  return (
    <Grid container spacing={2} sx={{ mb: 2 }}>
      {items.map(item => (
        <Grid item xs={12} sm={6} md={4} lg={2} key={item.label}>
          <Paper
            variant="outlined"
            data-testid="wecom-capability-card"
            data-contract-scope="wecom-push"
            data-contract-status={item.status}
            data-contract-action={item.action}
            data-contract-endpoint={item.endpoint}
            sx={{ p: 1.5, height: '100%', borderColor: item.status === 'ready' ? 'success.light' : 'warning.light' }}
          >
            <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
              {item.status === 'ready'
                ? <CheckCircleIcon sx={{ fontSize: 17, color: 'success.main' }} />
                : <WarningAmberIcon sx={{ fontSize: 17, color: 'warning.main' }} />}
              <Typography variant="subtitle2" fontWeight={700}>{item.label}</Typography>
            </Stack>
            <Chip size="small" label={item.status === 'ready' ? '已接入' : '显式降级'} color={item.status === 'ready' ? 'success' : 'warning'} variant="outlined" />
            <Typography variant="caption" color="text.secondary" display="block" mt={0.75}>{item.detail}</Typography>
          </Paper>
        </Grid>
      ))}
    </Grid>
  )
}

function maskWebhook(url: string): string {
  try {
    const u = new URL(url)
    return `${u.hostname}/***`
  } catch {
    return url.slice(0, 20) + '***'
  }
}

/** Render text with {variable} highlighted in orange */
function HighlightedText({ text, maxLen }: { text: string; maxLen?: number }) {
  const display = maxLen && text.length > maxLen ? text.slice(0, maxLen) + '…' : text
  const parts = display.split(/({[^}]+})/g)
  return (
    <Typography component="span" variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
      {parts.map((p, i) =>
        /^{.+}$/.test(p)
          ? <Box key={i} component="span" sx={{ color: 'warning.main', fontWeight: 600 }}>{p}</Box>
          : p
      )}
    </Typography>
  )
}

// ─── Tab 1: 机器人管理 ──────────────────────────────────────────────────────

function RobotTab({ onFilterByRobot }: { onFilterByRobot?: (id: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, robotName: '' })
  const [query, setQuery] = useState({ robotName: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<RobotSave>>({})
  const [deleteTarget, setDeleteTarget] = useState<WcRobot | null>(null)
  const [testOpen, setTestOpen] = useState(false)
  const [testRobot, setTestRobot] = useState<WcRobot | null>(null)
  const [testMsg, setTestMsg] = useState('这是一条测试消息，请忽略。')
  const [testResult, setTestResult] = useState<{ ok: boolean; msg: string } | null>(null)
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({ queryKey: ['wc-robots', search], queryFn: () => wecomApi.list(search) })
  const rows = normalizeArray<WcRobot>(data)
  const rowTotal = readTotal(data, rows.length)
  const enabledCount = rows.filter(row => row.status === 1).length
  const formRobotContext = `robotId=${form.id ?? '-'}，robotName=${form.robotName || '-'}`

  const saveMut = useMutation({
    mutationFn: (p: Partial<RobotSave>) => { setActionError(''); return wecomApi.save(p) },
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['wc-robots'] }) },
    onError: (e) => { setActionError(`/wecom/robot/save 保存失败：${getErrorMessage(e)}；${formRobotContext}`); toast('保存失败', 'error') },
  })
  const delMut = useMutation({
    mutationFn: (id: number) => { setActionError(''); return wecomApi.delete(id) },
    onSuccess: () => { toast('删除成功', 'success'); setDeleteTarget(null); qc.invalidateQueries({ queryKey: ['wc-robots'] }) },
    onError: (e) => { setActionError(`/wecom/robot/delete 删除失败：${getErrorMessage(e)}；robotId=${deleteTarget?.id ?? '-'}，robotName=${deleteTarget?.robotName ?? '-'}`); toast('删除失败', 'error') },
  })
  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => { setActionError(''); return wecomApi.updateStatus(id, status) },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wc-robots'] }),
    onError: (e, variables) => {
      const row = rows.find(item => item.id === variables.id)
      setActionError(`/wecom/robot/update-status 启停失败：${getErrorMessage(e)}；robotId=${variables.id}，robotName=${row?.robotName ?? '-'}，targetStatus=${variables.status}`)
      toast('启停失败', 'error')
    },
  })
  const testMut = useMutation({
    mutationFn: ({ id, content }: { id: number; content: string }) => { setActionError(''); return wecomApi.push({ robotId: id, content }) },
    onSuccess: () => setTestResult({ ok: true, msg: '发送成功' }),
    onError: (e) => setTestResult({ ok: false, msg: `/wecom/push 测试发送失败：${getErrorMessage(e)}；robotId=${testRobot?.id ?? '-'}，robotName=${testRobot?.robotName ?? '-'}` }),
  })

  const openAdd = () => { setForm({ status: 1, robotType: 'custom' }); setActionError(''); setFormOpen(true) }
  const openEdit = (row: WcRobot) => { setForm(row); setActionError(''); setFormOpen(true) }
  const openTest = (robot: WcRobot) => { setTestRobot(robot); setTestMsg('这是一条测试消息，请忽略。'); setTestResult(null); setTestOpen(true) }

  const handleSave = () => {
    if (!String(form.robotName ?? '').trim()) { toast('请填写机器人名称', 'warning'); return }
    if (!String(form.webhookUrl ?? '').trim()) { toast('请填写 Webhook URL', 'warning'); return }
    saveMut.mutate(form)
  }

  const handleTestSend = () => {
    if (!testRobot) return
    if (!testMsg.trim()) { setTestResult({ ok: false, msg: '请填写测试消息内容' }); return }
    testMut.mutate({ id: testRobot.id, content: testMsg })
  }

  const columns: GridColDef[] = [
    { field: 'robotName', headerName: '机器人名称', flex: 2 },
    {
      field: 'webhookUrl', headerName: 'Webhook URL', width: 200,
      renderCell: ({ value }) => (
        <Tooltip title="Webhook 已脱敏展示；完整地址只允许在编辑表单中轮换提交，不在列表复制或回显。">
          <Typography
            data-testid="wecom-robot-webhook-mask"
            data-no-plaintext-webhook-display="true"
            data-secret-copy-disabled="true"
            variant="body2"
            sx={{ color: 'text.secondary' }}
          >
            {maskWebhook(value ?? '')}
          </Typography>
        </Tooltip>
      ),
    },
    {
      field: '_ruleCount', headerName: '关联规则数', width: 100,
      renderCell: ({ row }) => (
        <Button
          size="small"
          variant="text"
          aria-label={`查看 ${row.robotName} 的规则`}
          onClick={() => onFilterByRobot?.(row.id)}
        >
          {(row as WcRobot & { ruleCount?: number }).ruleCount ?? 0}
        </Button>
      ),
    },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: ({ row }) => (
        <Switch
          size="small"
          checked={row.status === 1}
          onChange={e => statusMut.mutate({ id: row.id, status: e.target.checked ? 1 : 0 })}
        />
      ),
    },
    {
      field: 'actions', headerName: '操作', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => openTest(row as WcRobot)}>测试发送</Button>
          <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteTarget(row as WcRobot)}>删除</Button>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="wecom-robot-tab-workbench"
      data-contract-scope="wecom-robot"
      data-ready-endpoints="/wecom/robot/list,/wecom/robot/save,/wecom/robot/delete,/wecom/robot/update-status,/wecom/push"
      data-row-count={rows.length}
      data-total-count={rowTotal}
      data-enabled-count={enabledCount}
      data-no-local-robot-fallback="true"
      data-no-plaintext-webhook-display="true"
      sx={{ height: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column', gap: 1.5 }}
    >
      <Grid container spacing={2}>
        {[
          { label: '机器人总数', value: rowTotal, hint: '当前用户范围' },
          { label: '启用机器人', value: enabledCount, hint: '可发送消息' },
          { label: '禁用机器人', value: rows.length - enabledCount, hint: '暂停发送' },
          { label: '测试发送', value: '已接入', hint: '走 /wecom/push' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      {isError && (
        <Alert
          severity="error"
          data-testid="wecom-robot-list-error"
          data-contract-source="/wecom/robot/list"
          data-no-local-robot-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          /wecom/robot/list 机器人列表加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      {actionError ? (
        <Alert
          severity="error"
          data-testid="wecom-robot-action-error"
          data-input-retained="true"
          data-row-retained-on-action-error="true"
          data-no-local-robot-fallback="true"
        >
          {actionError}。失败不会关闭当前编辑弹窗或移除机器人行。
        </Alert>
      ) : null}
      {!isFetching && !isError && rows.length === 0 && (
        <Alert
          severity="warning"
          data-testid="wecom-robot-empty-no-fallback"
          data-contract-source="/wecom/robot/list"
          data-no-local-robot-fallback="true"
        >
          暂无企微机器人。添加机器人后才能配置规则和发送测试消息。
        </Alert>
      )}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1 }}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>添加机器人</Button>
      </Box>
      <Box
        data-testid="wecom-robot-grid-contract"
        data-contract-source="/wecom/robot/list"
        data-row-count={rows.length}
        data-total-count={rowTotal}
        data-query-robot-name={search.robotName}
        data-no-local-robot-fallback="true"
        data-no-plaintext-webhook-display="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} loading={isFetching}
          rowCount={rowTotal} paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={
            <>
              <TextField label="名称" size="small" value={query.robotName}
                onChange={e => setQuery(q => ({ ...q, robotName: e.target.value }))} sx={{ width: 160 }} />
              <Button variant="contained" onClick={() => setSearch({ ...query, page: 0, rows: 20 })}>搜索</Button>
              <Button onClick={() => { setQuery({ robotName: '' }); setSearch({ robotName: '', page: 0, rows: 20 }) }}>重置</Button>
            </>
          }
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      {/* 新建/编辑对话框 */}
      <FormDialog open={formOpen} title={form.id ? '编辑机器人' : '添加机器人'}
        onClose={() => setFormOpen(false)}
        onConfirm={handleSave}
        loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1, minWidth: 400 }}>
          <TextField label="机器人名称" required size="small" value={form.robotName ?? ''}
            onChange={e => setForm(f => ({ ...f, robotName: e.target.value }))} />
          <TextField label="Webhook URL" required size="small" value={form.webhookUrl ?? ''}
            placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
            onChange={e => setForm(f => ({ ...f, webhookUrl: e.target.value }))}
            helperText="必须以 https://qyapi.weixin.qq.com 开头"
            error={!!form.webhookUrl && !form.webhookUrl.startsWith('https://qyapi.weixin.qq.com')} />
          <TextField label="描述" size="small" multiline minRows={2} value={form.description ?? ''}
            onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          <FormControlLabel control={
            <Switch checked={(form.status ?? 1) === 1}
              onChange={e => setForm(f => ({ ...f, status: e.target.checked ? 1 : 0 }))} />
          } label="启用" />
          {actionError ? <Alert severity="error">{actionError}。保存失败会保留当前输入。</Alert> : null}
        </Stack>
      </FormDialog>

      {/* 测试发送对话框 */}
      <Dialog open={testOpen} onClose={() => setTestOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>测试发送</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <Alert severity="info">将调用 `/wecom/push`，robotId={testRobot?.id ?? '-'}，robotName={testRobot?.robotName ?? '-'}。</Alert>
            <TextField label="测试消息内容" multiline minRows={3} fullWidth
              value={testMsg} onChange={e => setTestMsg(e.target.value)} />
            {testResult && (
              <Alert severity={testResult.ok ? 'success' : 'error'}>
                {testResult.ok ? `✓ ${testResult.msg}` : `✗ ${testResult.msg}`}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestOpen(false)}>取消</Button>
          <Button variant="contained" startIcon={<SendIcon />}
            disabled={testMut.isPending}
            onClick={handleTestSend}>
            发送测试消息
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteTarget !== null} content={`确定要删除机器人「${deleteTarget?.robotName ?? '-'}」吗？endpoint=/wecom/robot/delete，robotId=${deleteTarget?.id ?? '-'}`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget.id)}
        loading={delMut.isPending} />
    </Box>
  )
}

// ─── Tab 2: 推送规则 ──────────────────────────────────────────────────────────

function RulesTab({ filterRobotId, onClearRobotFilter }: { filterRobotId?: number; onClearRobotFilter?: () => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [filterType, setFilterType] = useState<string>('all')
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<RuleSave>>({})
  const [deleteTarget, setDeleteTarget] = useState<WcRule | null>(null)
  const [manualOpen, setManualOpen] = useState(false)
  const [manualRule, setManualRule] = useState<WcRule | null>(null)
  const [manualResult, setManualResult] = useState<{ ok: boolean; msg: string } | null>(null)
  const [actionError, setActionError] = useState('')

  const { data: robots, isError: robotsError, error: robotsErr, refetch: refetchRobots } = useQuery({
    queryKey: ['wc-robots-sel'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => normalizeArray<WcRobot>(d),
  })

  const { data: allRules, isLoading, isError: rulesError, error: rulesErr, refetch: refetchRules } = useQuery({
    queryKey: ['wc-rules-all'],
    queryFn: () => wecomApi.ruleList({ rows: 200 }),
    select: d => normalizeArray<WcRule>(d),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<RuleSave>) => { setActionError(''); return wecomApi.ruleSave(p) },
    onSuccess: () => { toast('保存成功', 'success'); setDrawerOpen(false); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }) },
    onError: (e) => { setActionError(`/wecom/rule/save 保存失败：${getErrorMessage(e)}；ruleId=${form.id ?? '-'}，ruleName=${form.ruleName || '-'}，robotId=${form.robotId ?? filterRobotId ?? '-'}`); toast('保存失败', 'error') },
  })
  const delMut = useMutation({
    mutationFn: (id: number) => { setActionError(''); return wecomApi.ruleDelete(id) },
    onSuccess: () => { toast('删除成功', 'success'); setDeleteTarget(null); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }) },
    onError: (e) => { setActionError(`/wecom/rule/delete 删除失败：${getErrorMessage(e)}；ruleId=${deleteTarget?.id ?? '-'}，ruleName=${deleteTarget?.ruleName ?? '-'}，robotId=${deleteTarget?.robotId ?? '-'}`); toast('删除失败', 'error') },
  })
  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => { setActionError(''); return wecomApi.ruleUpdateStatus(id, status) },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wc-rules-all'] }),
    onError: (e, variables) => {
      const rule = (allRules ?? []).find(item => item.id === variables.id)
      setActionError(`/wecom/rule/update-status 启停失败：${getErrorMessage(e)}；ruleId=${variables.id}，ruleName=${rule?.ruleName ?? '-'}，targetStatus=${variables.status}`)
      toast('启停失败', 'error')
    },
  })
  const manualMut = useMutation({
    mutationFn: (ruleId: number) => {
      setActionError('')
      const rule = (allRules ?? []).find(item => item.id === ruleId)
      if (!rule) throw new Error('规则不存在')
      return wecomApi.push({ robotId: rule.robotId, ruleId: rule.id, content: rule.messageTemplate })
    },
    onSuccess: () => setManualResult({ ok: true, msg: '推送成功' }),
    onError: (e) => setManualResult({ ok: false, msg: `/wecom/push 手动触发失败：${getErrorMessage(e)}；ruleId=${manualRule?.id ?? '-'}，ruleName=${manualRule?.ruleName ?? '-'}，robotId=${manualRule?.robotId ?? '-'}` }),
  })

  const filteredRules = (allRules ?? []).filter(r =>
    (filterRobotId == null || r.robotId === filterRobotId) &&
    (filterType === 'all' || r.triggerType === filterType)
  )

  const openAdd = () => { setForm({ robotId: filterRobotId ?? robots?.[0]?.id, triggerType: 'manual', triggerConfig: '{}', status: 1 }); setActionError(''); setDrawerOpen(true) }
  const openEdit = (rule: WcRule) => { setForm(rule); setActionError(''); setDrawerOpen(true) }
  const openManual = (rule: WcRule) => { setManualRule(rule); setManualResult(null); setManualOpen(true) }

  const handleRuleSave = () => {
    const robotId = form.robotId ?? filterRobotId ?? robots?.[0]?.id
    if (!robotId) { toast('请选择目标机器人', 'warning'); return }
    if (!String(form.ruleName ?? '').trim()) { toast('请填写规则名称', 'warning'); return }
    if (!String(form.triggerType ?? '').trim()) { toast('请选择触发类型', 'warning'); return }
    if (!String(form.messageTemplate ?? '').trim()) { toast('请填写消息模板', 'warning'); return }
    saveMut.mutate({ ...form, robotId, triggerConfig: String(form.triggerConfig ?? '').trim() || '{}' })
  }

  const robotName = (id: number) => robots?.find(r => r.id === id)?.robotName ?? String(id)

  return (
    <Box
      data-testid="wecom-rules-workbench"
      data-contract-scope="wecom-rule"
      data-ready-endpoints="/wecom/rule/list,/wecom/rule/save,/wecom/rule/delete,/wecom/rule/update-status,/wecom/push"
      data-rule-count={allRules?.length ?? 0}
      data-visible-rule-count={filteredRules.length}
      data-filter-robot-id={filterRobotId ?? ''}
      data-filter-trigger-type={filterType}
      data-filter-mode="client-view-filter-after-backend-list"
      data-no-local-rule-fallback="true"
      data-no-template-reference-fallback="true"
    >
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {[
          { label: '当前规则', value: filteredRules.length, hint: `全部 ${allRules?.length ?? 0}` },
          { label: '启用规则', value: filteredRules.filter(rule => rule.status === 1).length, hint: '可触发推送' },
          { label: '机器人', value: robots?.length ?? 0, hint: '规则目标' },
          { label: '手动触发', value: '降级', hint: '使用 /wecom/push 直发规则消息' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
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
          /wecom/robot/list 机器人下拉加载失败：{getErrorMessage(robotsErr)}。新增/编辑规则前需要恢复机器人接口。
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
      {filterRobotId != null && (
        <Alert
          severity="info"
          sx={{ mb: 2 }}
          data-testid="wecom-rule-robot-view-filter"
          data-filter-mode="client-view-filter-after-backend-list"
          data-filter-robot-id={filterRobotId}
          data-no-backend-filter-claim="true"
          action={<Button color="inherit" size="small" onClick={() => onClearRobotFilter?.()}>返回机器人</Button>}
        >
          当前仅显示机器人「{robotName(filterRobotId)}」的规则。点击机器人总数可重新切换。
        </Alert>
      )}
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="body2" color="text.secondary">触发类型：</Typography>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <Select value={filterType} onChange={e => setFilterType(e.target.value)}>
              <MenuItem value="all">全部</MenuItem>
              {TRIGGER_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
            </Select>
          </FormControl>
        </Stack>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建规则</Button>
      </Stack>

      {isLoading && <LinearProgress />}

      {filteredRules.length === 0 && !isLoading && !rulesError && (
        <Box
          data-testid="wecom-rule-empty-no-fallback"
          data-contract-source="/wecom/rule/list"
          data-no-local-rule-fallback="true"
          sx={{ textAlign: 'center', py: 8, color: 'text.secondary' }}
        >
          <Typography>暂无推送规则，点击「新建规则」创建第一条</Typography>
        </Box>
      )}

      <Stack spacing={2}>
        {filteredRules.map(rule => {
          const tt = TRIGGER_MAP[rule.triggerType] ?? { label: rule.triggerType, tone: 'default' as const }
          return (
            <Paper
              key={rule.id}
              variant="outlined"
              data-testid="wecom-rule-card-surface"
              data-contract-source="/wecom/rule/list"
              data-rule-id={rule.id}
              data-robot-id={rule.robotId}
              data-no-template-reference-fallback="true"
              sx={{
                p: 2,
                borderLeft: '4px solid',
                borderLeftColor: (theme) => tt.tone === 'default' ? theme.palette.text.disabled : theme.palette[tt.tone].main,
              }}
            >
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                <Typography fontWeight={700}>{rule.ruleName}</Typography>
                <FormControlLabel
                  control={
                    <Switch size="small" checked={rule.status === 1}
                      onChange={e => statusMut.mutate({ id: rule.id, status: e.target.checked ? 1 : 0 })} />
                  } label={rule.status === 1 ? '启用' : '禁用'} labelPlacement="start" />
              </Stack>
              <Stack direction="row" spacing={1} alignItems="center" my={0.5}>
                <Chip
                  label={tt.label}
                  size="small"
                  data-testid="wecom-rule-trigger-chip-surface"
                  sx={{
                    bgcolor: (theme) => tt.tone === 'default'
                      ? alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.14 : 0.08)
                      : alpha(theme.palette[tt.tone].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                    color: tt.tone === 'default' ? 'text.secondary' : `${tt.tone}.main`,
                    fontWeight: 600,
                  }}
                />
                <Typography variant="body2" color="text.secondary">→</Typography>
                <Typography variant="body2">机器人：{robotName(rule.robotId)}</Typography>
              </Stack>
              {rule.messageTemplate && (
                <Box sx={{ bgcolor: 'action.hover', p: 1, borderRadius: 1, mb: 1 }}>
                  <HighlightedText text={rule.messageTemplate} maxLen={100} />
                </Box>
              )}
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button size="small" startIcon={<PlayArrowIcon />} onClick={() => openManual(rule)}>手动触发</Button>
                <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(rule)}>编辑</Button>
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteTarget(rule)}>删除</Button>
              </Stack>
            </Paper>
          )
        })}
      </Stack>

      {/* 规则编辑抽屉 */}
      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}
        PaperProps={{ sx: { width: 700, p: 3 } }}>
        <Typography variant="h6" fontWeight={700} mb={2}>{form.id ? '编辑推送规则' : '新建推送规则'}</Typography>
        <Divider sx={{ mb: 2 }} />
        <Stack spacing={2}>
          <TextField label="规则名称" required size="small" value={form.ruleName ?? ''}
            onChange={e => setForm(f => ({ ...f, ruleName: e.target.value }))} />
          <FormControl size="small" fullWidth>
            <InputLabel>触发类型</InputLabel>
            <Select label="触发类型" value={form.triggerType ?? ''}
              onChange={e => setForm(f => ({ ...f, triggerType: e.target.value }))}>
              {TRIGGER_TYPES.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" fullWidth>
            <InputLabel>目标机器人</InputLabel>
            <Select label="目标机器人" value={form.robotId ?? ''}
              onChange={e => setForm(f => ({ ...f, robotId: Number(e.target.value) }))}>
              {(robots ?? []).map(r => <MenuItem key={r.id} value={r.id}>{r.robotName}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="消息模板" multiline minRows={4} value={form.messageTemplate ?? ''}
            onChange={e => setForm(f => ({ ...f, messageTemplate: e.target.value }))}
            helperText="使用 {变量名} 作为占位符，如 {sessionName}" />
          <TextField label="触发配置（JSON）" multiline minRows={2} value={form.triggerConfig ?? ''}
            onChange={e => setForm(f => ({ ...f, triggerConfig: e.target.value }))}
            helperText="可选，定时或事件触发时填写" />
          <FormControlLabel control={
            <Switch checked={(form.status ?? 1) === 1}
              onChange={e => setForm(f => ({ ...f, status: e.target.checked ? 1 : 0 }))} />
          } label="启用" />
          {actionError ? <Alert severity="error">{actionError}。保存失败会保留当前输入。</Alert> : null}
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button variant="contained" onClick={handleRuleSave} disabled={saveMut.isPending}>保存</Button>
          </Stack>
        </Stack>
      </Drawer>

      {/* 手动触发对话框 */}
      <Dialog open={manualOpen} onClose={() => setManualOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>手动触发推送</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={1}>
            将立即触发规则「{manualRule?.ruleName ?? '-'}」并发送消息到对应机器人，endpoint=/wecom/push，ruleId={manualRule?.id ?? '-'}，robotId={manualRule?.robotId ?? '-'}。
          </Typography>
          {manualResult && (
            <Alert severity={manualResult.ok ? 'success' : 'error'} sx={{ mt: 1 }}>
              {manualResult.ok ? `✓ ${manualResult.msg}` : `✗ ${manualResult.msg}`}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setManualOpen(false)}>关闭</Button>
          {!manualResult && (
            <Button variant="contained" startIcon={<PlayArrowIcon />}
              disabled={manualMut.isPending}
              onClick={() => manualRule && manualMut.mutate(manualRule.id)}>
              确认触发
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteTarget !== null} content={`确定要删除规则「${deleteTarget?.ruleName ?? '-'}」吗？endpoint=/wecom/rule/delete，ruleId=${deleteTarget?.id ?? '-'}，robotId=${deleteTarget?.robotId ?? '-'}`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget.id)}
        loading={delMut.isPending} />
    </Box>
  )
}

// ─── Tab 3: 推送日志 ──────────────────────────────────────────────────────────

function LogTab() {
  const [search, setSearch] = useState<{
    page: number; rows: number; status?: number; robotId?: number
    startTime?: string; endTime?: string
  }>({ page: 0, rows: 20 })
  const [query, setQuery] = useState<{ status?: string; robotId?: string; timeRange: string }>({
    timeRange: 'today',
  })

  const { data: robots } = useQuery({
    queryKey: ['wc-robots-sel2'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['wc-log', search],
    queryFn: () => wecomApi.logList(search),
  })

  const getTimeRange = (range: string): { startTime?: string; endTime?: string } => {
    const now = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} 00:00:00`
    if (range === 'today') return { startTime: fmt(now) }
    if (range === '7d') { const d = new Date(now); d.setDate(d.getDate() - 7); return { startTime: fmt(d) } }
    if (range === '30d') { const d = new Date(now); d.setDate(d.getDate() - 30); return { startTime: fmt(d) } }
    return {}
  }

  const handleSearch = () => {
    const timeParams = getTimeRange(query.timeRange)
    setSearch(s => ({
      ...s,
      page: 0,
      status: query.status ? Number(query.status) : undefined,
      robotId: query.robotId ? Number(query.robotId) : undefined,
      ...timeParams,
    }))
  }

  const rows = normalizeArray<WcPushLog>(data)
  const rowTotal = readTotal(data, rows.length)
  const successCount = rows.filter(row => row.status === 1).length
  const failedCount = rows.filter(row => row.status === 0).length
  const successRate = rows.length > 0 ? (successCount / rows.length) * 100 : 0
  const kpiCards = [
    { label: '当前页日志', value: rows.length, color: 'text.primary' },
    { label: '成功数', value: successCount, color: 'success.main' },
    { label: '失败数', value: failedCount, color: failedCount > 0 ? 'error.main' : 'text.primary' },
    { label: '当前页成功率', value: `${successRate.toFixed(1)}%`, color: successRate < 95 && rows.length > 0 ? 'warning.main' : 'success.main' },
  ]

  const columns: GridColDef[] = [
    { field: 'createTime', headerName: '推送时间', width: 170, valueFormatter: (_: string, row?: WcPushLog) => formatDate(row?.sendTime ?? row?.createTime) },
    { field: 'robotName', headerName: '目标机器人', width: 150 },
    {
      field: 'content', headerName: '消息摘要', flex: 2,
      renderCell: ({ row }) => (
        <Typography variant="body2" noWrap sx={{ maxWidth: '100%' }}>
          {String((row as WcPushLog).messageContent ?? (row as WcPushLog).content ?? '').slice(0, 60)}
        </Typography>
      ),
    },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => (
        <Chip label={value === 1 ? '成功' : '失败'} size="small"
          color={value === 1 ? 'success' : 'error'} />
      ),
    },
    {
      field: 'errMsg', headerName: '错误原因', width: 160,
      renderCell: ({ row }) => {
        const value = (row as WcPushLog).errorMessage ?? (row as WcPushLog).errMsg
        return value ? <Typography variant="body2" color="error" noWrap>{String(value)}</Typography> : null
      },
    },
    {
      field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          {(row as WcPushLog).status === 0 && (
            <Tooltip title="后端暂无 /wecom/log/retry 接口，仅展示失败原因">
              <Chip
                label="不可重发"
                size="small"
                color="warning"
                variant="outlined"
                data-testid="wecom-log-retry-action"
                data-contract-status="unsupported"
                data-contract-action="log-retry"
                data-contract-endpoint={WECOM_LOG_RETRY_ENDPOINT}
                data-no-clickable-retry="true"
              />
            </Tooltip>
          )}
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="wecom-log-workbench"
      data-contract-scope="wecom-log"
      data-contract-status="degraded"
      data-ready-endpoint="/wecom/log/list"
      data-degraded-endpoint={WECOM_STATS_ENDPOINT}
      data-row-count={rows.length}
      data-total-count={rowTotal}
      data-no-local-log-fallback="true"
      data-no-synthetic-stats-fallback="true"
      data-no-log-retry-action="true"
      sx={{ height: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column', gap: 1.5 }}
    >
      {/* KPI 卡片 */}
      <Grid container spacing={2} mb={2}>
        {kpiCards.map(k => (
          <Grid item xs={6} sm={3} key={k.label}>
            <Paper
              variant="outlined"
              data-testid="wecom-log-kpi-card"
              data-contract-status="local-derived"
              data-source-endpoint="/wecom/log/list"
              data-degraded-endpoint={WECOM_STATS_ENDPOINT}
              sx={{ p: 2, textAlign: 'center' }}
            >
              <Typography variant="h4" fontWeight={700} color={k.color}>{k.value}</Typography>
              <Typography variant="caption" color="text.secondary">{k.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      <Alert
        severity="info"
        data-testid="wecom-log-stats-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="degraded"
        data-contract-endpoint={WECOM_STATS_ENDPOINT}
        data-source-endpoint="/wecom/log/list"
        data-unsupported-actions="log-retry"
        data-unsupported-endpoint={WECOM_LOG_RETRY_ENDPOINT}
      >
        推送统计接口尚未接入，当前 KPI 基于本页 `/wecom/log/list` 查询结果计算；失败日志暂不支持页面重发。
      </Alert>
      {isError && (
        <Alert
          severity="error"
          data-testid="wecom-log-list-error"
          data-contract-source="/wecom/log/list"
          data-no-local-log-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          /wecom/log/list 推送日志加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      {!isFetching && !isError && rows.length === 0 && (
        <Alert
          severity="info"
          data-testid="wecom-log-empty-no-fallback"
          data-contract-source="/wecom/log/list"
          data-no-local-log-fallback="true"
        >
          当前筛选条件下暂无推送日志。
        </Alert>
      )}

      <Box
        data-testid="wecom-log-grid-contract"
        data-contract-source="/wecom/log/list"
        data-row-count={rows.length}
        data-total-count={rowTotal}
        data-query-status={search.status ?? ''}
        data-query-robot-id={search.robotId ?? ''}
        data-no-local-log-fallback="true"
        data-no-log-retry-action="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows} columns={columns} loading={isFetching}
          rowCount={rowTotal} paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          searchSlot={
            <>
              <FormControl size="small" sx={{ minWidth: 100 }}>
                <InputLabel>状态</InputLabel>
                <Select label="状态" value={query.status ?? ''}
                  onChange={e => setQuery(q => ({ ...q, status: e.target.value as string }))}>
                  <MenuItem value="">全部</MenuItem>
                  <MenuItem value="1">成功</MenuItem>
                  <MenuItem value="0">失败</MenuItem>
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>机器人</InputLabel>
                <Select label="机器人" value={query.robotId ?? ''}
                  onChange={e => setQuery(q => ({ ...q, robotId: e.target.value as string }))}>
                  <MenuItem value="">全部</MenuItem>
                  {(robots ?? []).map(r => <MenuItem key={r.id} value={String(r.id)}>{r.robotName}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 100 }}>
                <InputLabel>时间</InputLabel>
                <Select label="时间" value={query.timeRange}
                  onChange={e => setQuery(q => ({ ...q, timeRange: e.target.value }))}>
                  <MenuItem value="today">今日</MenuItem>
                  <MenuItem value="7d">近7天</MenuItem>
                  <MenuItem value="30d">近30天</MenuItem>
                </Select>
              </FormControl>
              <Button variant="contained" onClick={handleSearch}>查询</Button>
              <Button onClick={() => { setQuery({ timeRange: 'today' }); setSearch({ page: 0, rows: 20 }) }}>重置</Button>
            </>
          }
        />
      </Box>
    </Box>
  )
}

// ─── Tab 4: 消息模板 ──────────────────────────────────────────────────────────

function TemplatesTab() {
  const extractVars = (content: string): string[] => {
    const matches = content.match(/{([^}]+)}/g) ?? []
    return [...new Set(matches.map(m => m.slice(1, -1)))]
  }

  const exampleTemplate = '【{直播间}】{主播} 即将开播，当前目标 GMV：{目标GMV}。'
  const currentVars = extractVars(exampleTemplate)

  return (
    <Box
      data-testid="wecom-template-workbench"
      data-contract-scope="wecom-template"
      data-contract-status="degraded"
      data-contract-endpoint={WECOM_TEMPLATE_ENDPOINT}
      data-fallback-field="rule.messageTemplate"
      sx={{ minHeight: 320 }}
    >
      <Alert
        severity="warning"
        data-testid="wecom-template-crud-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="degraded"
        data-contract-endpoint={WECOM_TEMPLATE_ENDPOINT}
        data-fallback-field="rule.messageTemplate"
        sx={{ mb: 2 }}
      >
        消息模板 CRUD 后端接口尚未接入：当前 `WecomController` 没有 `/wecom/template/*`。模板能力暂以推送规则的 `messageTemplate` 字段承接。
      </Alert>
      <Paper
        variant="outlined"
        data-testid="wecom-template-fallback-surface"
        data-contract-status="local-planning"
        data-contract-endpoint="unavailable"
        data-source-field="rule.messageTemplate"
        sx={{ p: 2 }}
      >
        <Typography variant="subtitle1" fontWeight={700} gutterBottom>模板变量约定</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          可以先在规则消息中使用变量占位符；后续接入模板表时，规则可迁移为引用模板。
        </Typography>
        <Box sx={{ bgcolor: 'action.hover', p: 1.5, borderRadius: 1, mb: 1 }}>
          <HighlightedText text={exampleTemplate} />
        </Box>
        <Stack direction="row" flexWrap="wrap" gap={0.5}>
          {currentVars.map(v => (
            <Chip
              key={v}
              label={`{${v}}`}
              size="small"
              data-testid="wecom-template-variable-chip-surface"
              data-contract-status="local-planning"
              data-contract-field="template-variable"
              sx={{
                bgcolor: (theme) => alpha(theme.palette.warning.main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                color: 'warning.main',
                fontWeight: 600,
              }}
            />
          ))}
        </Stack>
      </Paper>
    </Box>
  )
}

// ─── Main Page ───────────────────────────────────────────────────────────────

export default function WecomPage() {
  const [tab, setTab] = useState(0)
  const [filterRobotId, setFilterRobotId] = useState<number | undefined>(undefined)

  const handleFilterByRobot = (id: number) => {
    setFilterRobotId(id)
    setTab(1)
  }
  const handleClearRobotFilter = () => {
    setFilterRobotId(undefined)
    setTab(0)
  }

  return (
    <Box
      data-testid="wecom-workbench"
      data-contract-scope="wecom-push"
      data-ready-endpoints={WECOM_READY_ENDPOINTS.join(',')}
      data-unsupported-actions={WECOM_UNSUPPORTED_ACTIONS}
      sx={{ p: 3 }}
    >
      <PageHeader
        title="企微推送"
        subtitle="机器人、规则、日志和直连推送走真实后端接口；模板、统计、日志重发等未接入能力已明确降级。"
        breadcrumbs={[{ label: '企微推送' }, { label: '工作台' }]}
      />
      <WecomCapabilityCards />
      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="机器人管理" />
        <Tab label="推送规则" />
        <Tab label="推送日志" />
        <Tab label="消息模板" />
      </Tabs>
      {tab === 0 && <RobotTab onFilterByRobot={handleFilterByRobot} />}
      {tab === 1 && <RulesTab filterRobotId={filterRobotId} onClearRobotFilter={handleClearRobotFilter} />}
      {tab === 2 && <LogTab />}
      {tab === 3 && <TemplatesTab />}
    </Box>
  )
}





