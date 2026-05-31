import { useState, useCallback } from 'react'
import { Alert, Box, Button, Chip, FormControl, Grid, InputLabel, MenuItem, Paper, Select, Stack, Switch, Tab, Tabs, TextField, Tooltip, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import SendIcon from '@mui/icons-material/Send'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay } from '@/components/base'
import { wecomApi, type WcRobot, type PushLogQuery, type WcPushLog } from '@/api/wecom'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { normalizeArray, readTotal } from '@/utils/response-normalize'
import { getErrorMessage } from '@/utils/errorHandler'

const WECOM_ROBOT_READY_ENDPOINTS = [
  '/wecom/robot/list',
  '/wecom/robot/save',
  '/wecom/robot/delete',
  '/wecom/robot/update-status',
  '/wecom/push',
  '/wecom/log/list',
] as const
const WECOM_ROBOT_UNSUPPORTED_ACTIONS = 'log-retry'
const WECOM_ROBOT_LOG_RETRY_ENDPOINT = '/wecom/log/retry'
const WECOM_ROBOT_PUSH_ENDPOINT = '/wecom/push'

function RobotCapabilityCards() {
  const items = [
    { label: '机器人列表', status: 'ready', action: 'robot-list', endpoint: '/wecom/robot/list', detail: '/wecom/robot/list 包装分页兼容' },
    { label: '直连推送', status: 'ready', action: 'direct-push', endpoint: WECOM_ROBOT_PUSH_ENDPOINT, detail: '/wecom/push 文本消息' },
    { label: 'Webhook 轮换', status: 'ready', action: 'webhook-rotate', endpoint: '/wecom/robot/save', detail: '编辑机器人时提交完整地址，列表脱敏显示' },
    { label: '日志重发', status: 'unsupported', action: 'log-retry', endpoint: WECOM_ROBOT_LOG_RETRY_ENDPOINT, detail: '未提供 /wecom/log/retry，不放置禁用假按钮' },
  ] as const
  return (
    <Grid container spacing={1.5}>
      {items.map(item => (
        <Grid item xs={12} sm={6} md={3} key={item.label}>
          <Paper
            variant="outlined"
            data-testid="wecom-robot-capability-card"
            data-contract-scope="wecom-robot"
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

function RobotTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, robotName: '' })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<WcRobot>>({})
  const [deleteTarget, setDeleteTarget] = useState<WcRobot | null>(null)
  const [pushOpen, setPushOpen] = useState(false)
  const [pushRobot, setPushRobot] = useState<WcRobot | null>(null)
  const [pushContent, setPushContent] = useState('')
  const [actionError, setActionError] = useState('')

  const { data, isFetching, isError, error, refetch } = useQuery({ queryKey: ['wc-robots', search], queryFn: () => wecomApi.list(search) })
  const rows = normalizeArray<WcRobot>(data)
  const rowTotal = readTotal(data, rows.length)
  const enabledCount = rows.filter(r => r.status === 1).length
  const disabledCount = rows.length - enabledCount
  const formRobotContext = `robotId=${form.id ?? '-'}，robotName=${form.robotName || '-'}`
  const saveMut = useMutation({
    mutationFn: (payload: Partial<WcRobot>) => { setActionError(''); return wecomApi.save(payload) },
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
    mutationFn: (robot: WcRobot) => { setActionError(''); return wecomApi.push({ robotId: robot.id, content: '这是一条企微机器人连通性测试消息。' }) },
    onSuccess: () => toast('测试消息已发送', 'success'),
    onError: (e, robot) => { setActionError(`/wecom/push 测试发送失败：${getErrorMessage(e)}；robotId=${robot.id}，robotName=${robot.robotName}`); toast('测试发送失败', 'error') },
  })
  const pushMut = useMutation({
    mutationFn: (p: { robotId: number; content: string }) => { setActionError(''); return wecomApi.push(p) },
    onSuccess: () => { toast('推送成功', 'success'); setPushOpen(false); setPushContent('') },
    onError: (e) => { setActionError(`/wecom/push 推送失败：${getErrorMessage(e)}；robotId=${pushRobot?.id ?? '-'}，robotName=${pushRobot?.robotName ?? '-'}，contentLength=${pushContent.trim().length}`); toast('推送失败', 'error') },
  })

  const openAdd = useCallback(() => { setForm({ status: 1, robotType: 'custom' }); setActionError(''); setFormOpen(true) }, [])
  const openEdit = useCallback((row: WcRobot) => { setForm(row); setActionError(''); setFormOpen(true) }, [])
  const openPush = useCallback((robot: WcRobot) => { setPushRobot(robot); setActionError(''); setPushOpen(true) }, [])

  const maskWebhook = (value?: string) => {
    if (!value) return '-'
    try {
      const url = new URL(value)
      const key = url.searchParams.get('key')
      return `${url.hostname}/***${key ? key.slice(-4) : ''}`
    } catch {
      return `${value.slice(0, 18)}***`
    }
  }

  const handleSave = () => {
    if (!String(form.robotName ?? '').trim()) { toast('请填写机器人名称', 'warning'); return }
    if (!String(form.webhookUrl ?? '').trim()) { toast('请填写 Webhook URL', 'warning'); return }
    saveMut.mutate(form)
  }

  const handlePush = () => {
    if (pushRobot == null) return
    if (!pushContent.trim()) { toast('请填写消息内容', 'warning'); return }
    pushMut.mutate({ robotId: pushRobot.id, content: pushContent })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'robotName', headerName: '机器人名称', flex: 1 },
    {
      field: 'webhookUrl', headerName: 'Webhook URL', flex: 1, minWidth: 200,
      renderCell: ({ value }) => (
        <Tooltip title="Webhook 已脱敏展示，编辑时可轮换完整地址">
          <Typography
            variant="body2"
            color="text.secondary"
            data-testid="wecom-robot-webhook-mask"
            data-no-plaintext-webhook-display="true"
          >
            {maskWebhook(String(value ?? ''))}
          </Typography>
        </Tooltip>
      ),
    },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ row }) => (
        <Switch
          size="small"
          checked={(row as WcRobot).status === 1}
          onChange={e => statusMut.mutate({ id: (row as WcRobot).id, status: e.target.checked ? 1 : 0 })}
        />
      ),
    },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 230, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" startIcon={<SendIcon />} onClick={() => openPush(row as WcRobot)}>推送</Button>
          <Button size="small" onClick={() => testMut.mutate(row as WcRobot)}>测试</Button>
          <Button size="small" color="error" onClick={() => setDeleteTarget(row as WcRobot)}>删除</Button>
        </Stack>
      ),
    },
  ]

  const searchSlot = (
    <>
      <TextField label="机器人名称" size="small" value={query.robotName} onChange={e => setQuery(q => ({ ...q, robotName: e.target.value }))} sx={{ width: 160 }} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
      <Button onClick={() => { setQuery({ page: 0, rows: 20, robotName: '' }); setSearch({ page: 0, rows: 20, robotName: '' }) }}>重置</Button>
    </>
  )

  return (
    <Box
      data-testid="wecom-robot-workbench"
      data-contract-scope="wecom-robot"
      data-ready-endpoints={WECOM_ROBOT_READY_ENDPOINTS.join(',')}
      data-unsupported-actions={WECOM_ROBOT_UNSUPPORTED_ACTIONS}
      data-robot-count={rows.length}
      data-total-count={rowTotal}
      data-enabled-count={enabledCount}
      data-no-local-robot-fallback="true"
      data-no-plaintext-webhook-display="true"
      sx={{ height: 'calc(100vh - 48px - 32px - 48px)', display: 'flex', flexDirection: 'column', gap: 1.5 }}
    >
      <Grid container spacing={1.5}>
        {[
          { label: '机器人总数', value: rowTotal, hint: '当前权限范围' },
          { label: '启用', value: enabledCount, hint: '可发送企微消息' },
          { label: '禁用', value: disabledCount, hint: '不会发送消息' },
          { label: '直连推送', value: '已接入', hint: '/wecom/push' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper
              variant="outlined"
              data-testid="wecom-robot-kpi-card"
              data-contract-status="ready"
              data-source-endpoint={item.label === '直连推送' ? WECOM_ROBOT_PUSH_ENDPOINT : '/wecom/robot/list'}
              sx={{ p: 1.5 }}
            >
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
          {actionError}。失败不会关闭当前弹窗或移除机器人行。
        </Alert>
      ) : null}
      {!isFetching && !isError && rows.length === 0 && (
        <Alert
          severity="warning"
          data-testid="wecom-robot-empty-no-fallback"
          data-contract-source="/wecom/robot/list"
          data-no-local-robot-fallback="true"
        >
          暂无企微机器人。新增机器人后才能配置推送规则或发送测试消息。
        </Alert>
      )}
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
          rows={rows}
          columns={columns}
          rowCount={rowTotal}
          loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          actionSlot={<Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增机器人</Button>}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>
      <FormDialog open={formOpen} title={form.id ? '编辑机器人' : '新增机器人'} onClose={() => setFormOpen(false)} onConfirm={handleSave} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="机器人名称" value={form.robotName ?? ''} onChange={e => setForm(f => ({ ...f, robotName: e.target.value }))} fullWidth />
          <TextField label="Webhook URL" value={form.webhookUrl ?? ''} onChange={e => setForm(f => ({ ...f, webhookUrl: e.target.value }))} fullWidth />
          {actionError ? <Alert severity="error">{actionError}。保存失败会保留当前输入。</Alert> : null}
        </Stack>
      </FormDialog>
      <FormDialog open={pushOpen} title="发送推送消息" onClose={() => setPushOpen(false)} onConfirm={handlePush} loading={pushMut.isPending}>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Alert
            severity="info"
            data-testid="wecom-robot-push-contract"
            data-contract-status="ready"
            data-contract-action="direct-push"
            data-contract-endpoint={WECOM_ROBOT_PUSH_ENDPOINT}
            data-robot-id={pushRobot?.id ?? ''}
          >
            将调用 `/wecom/push`，robotId={pushRobot?.id ?? '-'}，robotName={pushRobot?.robotName ?? '-'}。
          </Alert>
          <TextField label="消息内容" value={pushContent} onChange={e => setPushContent(e.target.value)} fullWidth multiline minRows={3} />
          {actionError ? <Alert severity="error">{actionError}。推送失败会保留当前消息内容。</Alert> : null}
        </Stack>
      </FormDialog>
      <ConfirmDialog
        open={deleteTarget !== null}
        content={`确定要删除机器人「${deleteTarget?.robotName ?? '-'}」吗？endpoint=/wecom/robot/delete，robotId=${deleteTarget?.id ?? '-'}`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget !== null && delMut.mutate(deleteTarget.id)}
        loading={delMut.isPending}
      />
    </Box>
  )
}

function PushLogTab() {
  const [search, setSearch] = useState<PushLogQuery>({ page: 0, rows: 20 })
  const [query, setQuery] = useState(search)
  const { data, isFetching, isError, error, refetch } = useQuery({ queryKey: ['wc-push-logs', search], queryFn: () => wecomApi.logList(search) })
  const rows = normalizeArray<WcPushLog>(data)
  const rowTotal = readTotal(data, rows.length)
  const successCount = rows.filter(row => row.status === 1).length
  const failedCount = rows.filter(row => row.status === 0).length

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'robotName', headerName: '机器人', width: 140 },
    { field: 'content', headerName: '消息内容', flex: 1, minWidth: 200, renderCell: ({ row }) => (row as WcPushLog).messageContent ?? (row as WcPushLog).content ?? '' },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => <Chip label={value === 1 ? '成功' : '失败'} color={value === 1 ? 'success' : 'error'} size="small" />,
    },
    { field: 'errMsg', headerName: '错误信息', width: 200, renderCell: ({ row }) => (row as WcPushLog).errorMessage ?? (row as WcPushLog).errMsg ?? '' },
    { field: 'createTime', headerName: '推送时间', width: 160, valueFormatter: (_: string, row?: WcPushLog) => formatDate(row?.sendTime ?? row?.createTime) },
  ]

  const searchSlot = (
    <>
      <FormControl size="small" sx={{ minWidth: 100 }}>
        <InputLabel>状态</InputLabel>
        <Select label="状态" value={query.status ?? ''} onChange={e => setQuery(q => ({ ...q, status: e.target.value === '' ? undefined : Number(e.target.value) }))}>
          <MenuItem value="">全部</MenuItem>
          <MenuItem value={1}>成功</MenuItem>
          <MenuItem value={0}>失败</MenuItem>
        </Select>
      </FormControl>
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
      <Button onClick={() => { setQuery({ page: 0, rows: 20 }); setSearch({ page: 0, rows: 20 }) }}>重置</Button>
    </>
  )

  return (
    <Box
      data-testid="wecom-robot-log-workbench"
      data-contract-scope="wecom-robot-log"
      data-contract-status="degraded"
      data-ready-endpoint="/wecom/log/list"
      data-unsupported-actions={WECOM_ROBOT_UNSUPPORTED_ACTIONS}
      data-unsupported-endpoint={WECOM_ROBOT_LOG_RETRY_ENDPOINT}
      data-row-count={rows.length}
      data-total-count={rowTotal}
      data-no-local-log-fallback="true"
      data-no-log-retry-action="true"
      sx={{ height: 'calc(100vh - 48px - 32px - 48px)', display: 'flex', flexDirection: 'column', gap: 1.5 }}
    >
      <Grid container spacing={1.5}>
        {[
          { label: '当前页日志', value: rows.length, hint: `服务端总数 ${rowTotal}` },
          { label: '成功', value: successCount, hint: 'status=1' },
          { label: '失败', value: failedCount, hint: 'status=0' },
          { label: '失败重发', value: '显式降级', hint: '后端暂无 /wecom/log/retry' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper
              variant="outlined"
              data-testid="wecom-robot-log-kpi-card"
              data-contract-status={item.label === '失败重发' ? 'unsupported' : 'ready'}
              data-source-endpoint="/wecom/log/list"
              data-unsupported-endpoint={item.label === '失败重发' ? WECOM_ROBOT_LOG_RETRY_ENDPOINT : undefined}
              sx={{ p: 1.5 }}
            >
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
          data-testid="wecom-robot-log-list-error"
          data-contract-source="/wecom/log/list"
          data-no-local-log-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          /wecom/log/list 推送日志加载失败：{getErrorMessage(error)}
        </Alert>
      )}
      <Alert
        severity="info"
        data-testid="wecom-robot-log-retry-downgrade"
        data-downgrade-tone="contract-gap"
        data-contract-status="unsupported"
        data-contract-action="log-retry"
        data-contract-endpoint={WECOM_ROBOT_LOG_RETRY_ENDPOINT}
        data-source-endpoint="/wecom/log/list"
      >
        后端暂无 /wecom/log/retry 接口，失败日志当前仅展示原因；页面不再提供不可执行的重发按钮。
      </Alert>
      {!isFetching && !isError && rows.length === 0 && (
        <Alert
          severity="info"
          data-testid="wecom-robot-log-empty-no-fallback"
          data-contract-source="/wecom/log/list"
          data-no-local-log-fallback="true"
        >
          暂无推送日志。发送测试消息或业务规则触发后会在这里出现记录。
        </Alert>
      )}
      <Box
        data-testid="wecom-robot-log-grid-contract"
        data-contract-source="/wecom/log/list"
        data-row-count={rows.length}
        data-total-count={rowTotal}
        data-query-status={search.status ?? ''}
        data-no-local-log-fallback="true"
        data-no-log-retry-action="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={rowTotal}
          paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>
    </Box>
  )
}

export default function RobotsPage() {
  const [tab, setTab] = useState(0)
  return (
    <Box
      data-testid="wecom-robot-page"
      data-contract-scope="wecom-robot"
      data-ready-endpoints={WECOM_ROBOT_READY_ENDPOINTS.join(',')}
      data-unsupported-actions={WECOM_ROBOT_UNSUPPORTED_ACTIONS}
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column', p: 2, gap: 2 }}
    >
      <PageHeader
        title="企微机器人"
        subtitle="机器人列表、测试推送和日志查询走真实企微接口；日志重发等未接入能力会明确标注。"
        breadcrumbs={[{ label: '企微推送' }, { label: '机器人' }]}
      />
      <RobotCapabilityCards />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="机器人管理" />
        <Tab label="推送日志" />
      </Tabs>
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {tab === 0 && <RobotTab />}
        {tab === 1 && <PushLogTab />}
      </Box>
    </Box>
  )
}
