import { useState, useRef } from 'react'
import {
  Box, Typography, Stack, Button, Chip, Tab, Tabs, Paper,
  Drawer, TextField, FormControl, InputLabel, Select, MenuItem,
  Switch, FormControlLabel, Divider, Grid, Dialog, DialogTitle,
  DialogContent, DialogActions, Alert, LinearProgress, Tooltip,
  IconButton,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import SendIcon from '@mui/icons-material/Send'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import RefreshIcon from '@mui/icons-material/Refresh'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import {
  wecomApi,
  type WcRobot, type WcRule, type WcPushLog, type WcMessageTemplate,
  type RobotSave, type RuleSave,
} from '@/api/wecom'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

// ─── Constants ───────────────────────────────────────────────────────────────

const TRIGGER_TYPES = [
  { value: 'live_start',     label: '开播提醒',   color: '#3ba272' },
  { value: 'gmv_milestone',  label: 'GMV里程碑',  color: '#eab308' },
  { value: 'script_approve', label: '话术审批',   color: '#5470c6' },
  { value: 'alert',          label: '系统告警',   color: '#ee6666' },
  { value: 'schedule',       label: '定时触发',   color: '#9ca3af' },
  { value: 'manual',         label: '手动触发',   color: '#8b5cf6' },
  { value: 'event',          label: '事件触发',   color: '#f97316' },
]
const TRIGGER_MAP = Object.fromEntries(TRIGGER_TYPES.map(t => [t.value, t]))

// ─── Helpers ─────────────────────────────────────────────────────────────────

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
          ? <Box key={i} component="span" sx={{ color: '#f97316', fontWeight: 600 }}>{p}</Box>
          : p
      )}
    </Typography>
  )
}

function renderPreview(template: string, exampleValues: Record<string, string>): string {
  return template.replace(/{([^}]+)}/g, (_, key) => exampleValues[key] ?? `[${key}]`)
}

// ─── Tab 1: 机器人管理 ──────────────────────────────────────────────────────

function RobotTab({ onFilterByRobot }: { onFilterByRobot?: (id: number) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, robotName: '' })
  const [query, setQuery] = useState({ robotName: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<RobotSave>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [testOpen, setTestOpen] = useState(false)
  const [testRobotId, setTestRobotId] = useState<number | null>(null)
  const [testMsg, setTestMsg] = useState('这是一条测试消息，请忽略。')
  const [testResult, setTestResult] = useState<{ ok: boolean; msg: string } | null>(null)

  const { data, isFetching } = useQuery({ queryKey: ['wc-robots', search], queryFn: () => wecomApi.list(search) })

  const saveMut = useMutation({
    mutationFn: (p: Partial<RobotSave>) => wecomApi.save(p),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['wc-robots'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: wecomApi.delete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['wc-robots'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => wecomApi.updateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wc-robots'] }),
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const testMut = useMutation({
    mutationFn: ({ id, content }: { id: number; content: string }) => wecomApi.push({ robotId: id, content }),
    onSuccess: () => setTestResult({ ok: true, msg: '发送成功' }),
    onError: (e: Error) => setTestResult({ ok: false, msg: e.message }),
  })

  const openAdd = () => { setForm({}); setFormOpen(true) }
  const openEdit = (row: WcRobot) => { setForm(row); setFormOpen(true) }
  const openTest = (id: number) => { setTestRobotId(id); setTestMsg('这是一条测试消息，请忽略。'); setTestResult(null); setTestOpen(true) }

  const columns: GridColDef[] = [
    { field: 'robotName', headerName: '机器人名称', flex: 2 },
    {
      field: 'webhookUrl', headerName: 'Webhook URL', width: 200,
      renderCell: ({ value, row }) => (
        <Tooltip title={value}>
          <Typography
            variant="body2"
            sx={{ cursor: 'pointer', textDecoration: 'underline dotted', color: 'text.secondary' }}
            onClick={() => navigator.clipboard.writeText(row.webhookUrl).then(() => toast('已复制', 'success'))}
          >
            {maskWebhook(value ?? '')}
          </Typography>
        </Tooltip>
      ),
    },
    {
      field: '_ruleCount', headerName: '关联规则数', width: 100,
      renderCell: ({ row }) => (
        <Button size="small" variant="text" onClick={() => onFilterByRobot?.(row.id)}>
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
          <Button size="small" onClick={() => openTest(row.id)}>测试发送</Button>
          <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteId(row.id)}>删除</Button>
        </Stack>
      ),
    },
  ]

  return (
    <Box sx={{ height: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1 }}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>添加机器人</Button>
      </Box>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} loading={isFetching}
        rowCount={data?.total ?? 0} paginationMode="server"
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
        slotProps={undefined}
      />

      {/* 新建/编辑对话框 */}
      <FormDialog open={formOpen} title={form.id ? '编辑机器人' : '添加机器人'}
        onClose={() => setFormOpen(false)}
        onConfirm={() => saveMut.mutate(form)}
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
        </Stack>
      </FormDialog>

      {/* 测试发送对话框 */}
      <Dialog open={testOpen} onClose={() => setTestOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>测试发送</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
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
            onClick={() => testRobotId && testMut.mutate({ id: testRobotId, content: testMsg })}>
            发送测试消息
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该机器人吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
    </Box>
  )
}

// ─── Tab 2: 推送规则 ──────────────────────────────────────────────────────────

function RulesTab({ filterRobotId }: { filterRobotId?: number }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [filterType, setFilterType] = useState<string>('all')
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<RuleSave>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [manualOpen, setManualOpen] = useState(false)
  const [manualRuleId, setManualRuleId] = useState<number | null>(null)
  const [manualResult, setManualResult] = useState<{ ok: boolean; msg: string } | null>(null)

  const { data: robots } = useQuery({
    queryKey: ['wc-robots-sel'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data: allRules, isLoading } = useQuery({
    queryKey: ['wc-rules-all', filterRobotId],
    queryFn: () => wecomApi.ruleList({ rows: 200, robotId: filterRobotId }),
    select: d => d?.list ?? [],
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<RuleSave>) => wecomApi.ruleSave(p),
    onSuccess: () => { toast('保存成功', 'success'); setDrawerOpen(false); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: wecomApi.ruleDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['wc-rules-all'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => wecomApi.ruleUpdateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['wc-rules-all'] }),
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const manualMut = useMutation({
    mutationFn: (ruleId: number) => wecomApi.manualPush(ruleId),
    onSuccess: () => setManualResult({ ok: true, msg: '推送成功' }),
    onError: (e: Error) => setManualResult({ ok: false, msg: e.message }),
  })

  const filteredRules = (allRules ?? []).filter(r =>
    filterType === 'all' || r.triggerType === filterType
  )

  const openAdd = () => { setForm({ robotId: robots?.[0]?.id, status: 1 }); setDrawerOpen(true) }
  const openEdit = (rule: WcRule) => { setForm(rule); setDrawerOpen(true) }
  const openManual = (id: number) => { setManualRuleId(id); setManualResult(null); setManualOpen(true) }

  const robotName = (id: number) => robots?.find(r => r.id === id)?.robotName ?? String(id)

  return (
    <Box>
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

      {filteredRules.length === 0 && !isLoading && (
        <Box sx={{ textAlign: 'center', py: 8, color: 'text.secondary' }}>
          <Typography>暂无推送规则，点击「新建规则」创建第一条</Typography>
        </Box>
      )}

      <Stack spacing={2}>
        {filteredRules.map(rule => {
          const tt = TRIGGER_MAP[rule.triggerType] ?? { label: rule.triggerType, color: '#9ca3af' }
          return (
            <Paper key={rule.id} variant="outlined" sx={{ p: 2, borderLeft: `4px solid ${tt.color}` }}>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                <Typography fontWeight={700}>{rule.ruleName}</Typography>
                <FormControlLabel
                  control={
                    <Switch size="small" checked={rule.status === 1}
                      onChange={e => statusMut.mutate({ id: rule.id, status: e.target.checked ? 1 : 0 })} />
                  } label={rule.status === 1 ? '启用' : '禁用'} labelPlacement="start" />
              </Stack>
              <Stack direction="row" spacing={1} alignItems="center" my={0.5}>
                <Chip label={tt.label} size="small" sx={{ bgcolor: tt.color + '22', color: tt.color, fontWeight: 600 }} />
                <Typography variant="body2" color="text.secondary">→</Typography>
                <Typography variant="body2">机器人：{robotName(rule.robotId)}</Typography>
              </Stack>
              {rule.messageTemplate && (
                <Box sx={{ bgcolor: 'action.hover', p: 1, borderRadius: 1, mb: 1 }}>
                  <HighlightedText text={rule.messageTemplate} maxLen={100} />
                </Box>
              )}
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button size="small" startIcon={<PlayArrowIcon />} onClick={() => openManual(rule.id)}>手动触发</Button>
                <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(rule)}>编辑</Button>
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteId(rule.id)}>删除</Button>
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
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button variant="contained" onClick={() => saveMut.mutate(form)} disabled={saveMut.isPending}>保存</Button>
          </Stack>
        </Stack>
      </Drawer>

      {/* 手动触发对话框 */}
      <Dialog open={manualOpen} onClose={() => setManualOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>手动触发推送</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" mb={1}>
            将立即触发该规则并发送消息到对应机器人，确认继续？
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
              onClick={() => manualRuleId && manualMut.mutate(manualRuleId)}>
              确认触发
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该规则吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
    </Box>
  )
}

// ─── Tab 3: 推送日志 ──────────────────────────────────────────────────────────

function LogTab() {
  const toast = useToast()
  const qc = useQueryClient()
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

  const { data: stats } = useQuery({
    queryKey: ['wc-push-stats'],
    queryFn: () => wecomApi.pushStats(),
  })

  const { data, isFetching } = useQuery({
    queryKey: ['wc-log', search],
    queryFn: () => wecomApi.logList(search),
  })

  const retryMut = useMutation({
    mutationFn: wecomApi.retryPush,
    onSuccess: () => { toast('重新发送成功', 'success'); qc.invalidateQueries({ queryKey: ['wc-log'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
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

  const successRate = stats?.successRate ?? 0
  const kpiCards = [
    { label: '今日推送总数', value: stats?.total ?? 0, color: 'text.primary' },
    { label: '成功数', value: stats?.success ?? 0, color: 'success.main' },
    { label: '失败数', value: stats?.failed ?? 0, color: (stats?.failed ?? 0) > 0 ? 'error.main' : 'text.primary' },
    { label: '成功率', value: `${successRate.toFixed(1)}%`, color: successRate < 95 ? 'warning.main' : 'success.main' },
  ]

  const columns: GridColDef[] = [
    { field: 'createTime', headerName: '推送时间', width: 170, valueFormatter: (v: string) => formatDate(v) },
    { field: 'robotName', headerName: '目标机器人', width: 150 },
    {
      field: 'content', headerName: '消息摘要', flex: 2,
      renderCell: ({ value }) => (
        <Typography variant="body2" noWrap sx={{ maxWidth: '100%' }}>
          {String(value ?? '').slice(0, 60)}
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
      renderCell: ({ value }) => value ? <Typography variant="body2" color="error" noWrap>{String(value)}</Typography> : null,
    },
    {
      field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          {(row as WcPushLog).status === 0 && (
            <Button size="small" startIcon={<RefreshIcon />}
              onClick={() => retryMut.mutate((row as WcPushLog).id)}>重发</Button>
          )}
        </Stack>
      ),
    },
  ]

  return (
    <Box sx={{ height: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column' }}>
      {/* KPI 卡片 */}
      <Grid container spacing={2} mb={2}>
        {kpiCards.map(k => (
          <Grid item xs={6} sm={3} key={k.label}>
            <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h4" fontWeight={700} color={k.color}>{k.value}</Typography>
              <Typography variant="caption" color="text.secondary">{k.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} loading={isFetching}
        rowCount={data?.total ?? 0} paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        slotProps={undefined}
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
  )
}

// ─── Tab 4: 消息模板 ──────────────────────────────────────────────────────────

function TemplatesTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20 })
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form, setForm] = useState<Partial<WcMessageTemplate>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [previewVars, setPreviewVars] = useState<Record<string, string>>({})
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const { data: robots } = useQuery({
    queryKey: ['wc-robots-sel3'],
    queryFn: () => wecomApi.list({ rows: 100 }),
    select: d => d?.list ?? [],
  })

  const { data, isFetching } = useQuery({
    queryKey: ['wc-templates', search],
    queryFn: () => wecomApi.templateList(search),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<WcMessageTemplate>) => wecomApi.templateSave(p),
    onSuccess: () => { toast('保存成功', 'success'); setDrawerOpen(false); qc.invalidateQueries({ queryKey: ['wc-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })
  const delMut = useMutation({
    mutationFn: wecomApi.templateDelete,
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['wc-templates'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const extractVars = (content: string): string[] => {
    const matches = content.match(/{([^}]+)}/g) ?? []
    return [...new Set(matches.map(m => m.slice(1, -1)))]
  }

  const handleInsertVar = (varName: string) => {
    const el = textareaRef.current
    if (!el) return
    const start = el.selectionStart
    const end = el.selectionEnd
    const val = form.templateContent ?? ''
    const newVal = val.slice(0, start) + `{${varName}}` + val.slice(end)
    setForm(f => ({ ...f, templateContent: newVal }))
  }

  const openAdd = () => { setForm({ robotId: robots?.[0]?.id, status: 1 }); setPreviewVars({}); setDrawerOpen(true) }
  const openEdit = (t: WcMessageTemplate) => {
    setForm(t)
    setPreviewVars(t.exampleValues ?? {})
    setDrawerOpen(true)
  }

  const columns: GridColDef[] = [
    { field: 'templateName', headerName: '模板名称', flex: 1 },
    {
      field: 'templateContent', headerName: '内容预览', flex: 2,
      renderCell: ({ value }) => <HighlightedText text={String(value ?? '')} maxLen={80} />,
    },
    {
      field: 'variables', headerName: '变量列表', width: 200,
      renderCell: ({ value }) => (
        <Stack direction="row" spacing={0.5} flexWrap="wrap">
          {(Array.isArray(value) ? value : []).map((v: string) => (
            <Chip key={v} label={`{${v}}`} size="small"
              sx={{ bgcolor: '#fff7ed', color: '#f97316', fontWeight: 600, fontSize: 11 }} />
          ))}
        </Stack>
      ),
    },
    {
      field: 'status', headerName: '状态', width: 80,
      renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} size="small" color={value === 1 ? 'success' : 'default'} />,
    },
    {
      field: 'actions', headerName: '操作', width: 140, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" startIcon={<EditIcon />} onClick={() => openEdit(row as WcMessageTemplate)}>编辑</Button>
          <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => setDeleteId((row as WcMessageTemplate).id)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const currentVars = extractVars(form.templateContent ?? '')

  return (
    <Box sx={{ height: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1 }}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建模板</Button>
      </Box>
      <StandardDataGrid
        rows={data?.list ?? []} columns={columns} loading={isFetching}
        rowCount={data?.total ?? 0} paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        slotProps={undefined}
      />

      {/* 编辑抽屉 */}
      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}
        PaperProps={{ sx: { width: 720, p: 3 } }}>
        <Typography variant="h6" fontWeight={700} mb={2}>{form.id ? '编辑消息模板' : '新建消息模板'}</Typography>
        <Divider sx={{ mb: 2 }} />
        <Grid container spacing={2}>
          {/* 左：编辑区 */}
          <Grid item xs={8}>
            <Stack spacing={2}>
              <TextField label="模板名称" required size="small" value={form.templateName ?? ''}
                onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} />
              <FormControl size="small" fullWidth>
                <InputLabel>关联机器人</InputLabel>
                <Select label="关联机器人" value={form.robotId ?? ''}
                  onChange={e => setForm(f => ({ ...f, robotId: Number(e.target.value) }))}>
                  {(robots ?? []).map(r => <MenuItem key={r.id} value={r.id}>{r.robotName}</MenuItem>)}
                </Select>
              </FormControl>
              <TextField
                label="模板内容"
                multiline minRows={6}
                value={form.templateContent ?? ''}
                onChange={e => setForm(f => ({ ...f, templateContent: e.target.value }))}
                inputProps={{ ref: textareaRef }}
                helperText={`${(form.templateContent ?? '').length}/4096 字`}
                error={(form.templateContent ?? '').length > 4096}
              />
              {/* 预览区 */}
              {form.templateContent && (
                <Box sx={{ bgcolor: '#f8f9fa', p: 1.5, borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
                  <Typography variant="caption" fontWeight={600} display="block" mb={0.5}>实时预览：</Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {renderPreview(form.templateContent, previewVars)}
                  </Typography>
                </Box>
              )}
              <FormControlLabel control={
                <Switch checked={(form.status ?? 1) === 1}
                  onChange={e => setForm(f => ({ ...f, status: e.target.checked ? 1 : 0 }))} />
              } label="启用" />
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button onClick={() => setDrawerOpen(false)}>取消</Button>
                <Button variant="contained" onClick={() => saveMut.mutate(form)} disabled={saveMut.isPending}>保存</Button>
              </Stack>
            </Stack>
          </Grid>
          {/* 右：变量面板 */}
          <Grid item xs={4}>
            <Typography variant="subtitle2" fontWeight={700} mb={1}>变量列表</Typography>
            <Typography variant="caption" color="text.secondary" display="block" mb={1}>
              点击变量名插入到光标位置
            </Typography>
            {currentVars.length === 0 ? (
              <Typography variant="body2" color="text.secondary">在内容中输入 {'{'} 变量名 {'}'} 以创建变量</Typography>
            ) : (
              <Stack spacing={1}>
                {currentVars.map(v => (
                  <Box key={v} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1 }}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" mb={0.5}>
                      <Box component="span"
                        sx={{ color: '#f97316', fontWeight: 600, cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                        onClick={() => handleInsertVar(v)}>
                        {`{${v}}`}
                      </Box>
                      <Tooltip title="插入">
                        <IconButton size="small" onClick={() => handleInsertVar(v)}>
                          <ContentCopyIcon fontSize="inherit" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                    <TextField
                      label="示例值" size="small" fullWidth
                      value={previewVars[v] ?? ''}
                      onChange={e => setPreviewVars(prev => ({ ...prev, [v]: e.target.value }))}
                    />
                  </Box>
                ))}
              </Stack>
            )}
          </Grid>
        </Grid>
      </Drawer>

      <ConfirmDialog open={deleteId !== null} content="确定要删除该消息模板吗？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && delMut.mutate(deleteId)}
        loading={delMut.isPending} />
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

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" fontWeight={700} mb={2}>企微推送</Typography>
      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="机器人管理" />
        <Tab label="推送规则" />
        <Tab label="推送日志" />
        <Tab label="消息模板" />
      </Tabs>
      {tab === 0 && <RobotTab onFilterByRobot={handleFilterByRobot} />}
      {tab === 1 && <RulesTab filterRobotId={filterRobotId} />}
      {tab === 2 && <LogTab />}
      {tab === 3 && <TemplatesTab />}
    </Box>
  )
}





