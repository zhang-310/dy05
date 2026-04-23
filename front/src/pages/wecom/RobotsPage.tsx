import { useState, useCallback } from 'react'
import { Box, TextField, Button, MenuItem, Select, FormControl, InputLabel, Chip, Stack, Tab, Tabs } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import SendIcon from '@mui/icons-material/Send'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { wecomApi, type WcRobot, type PushLogQuery } from '@/api/wecom'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

function RobotTab() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, robotName: '' })
  const [query, setQuery] = useState(search)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<WcRobot>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [pushOpen, setPushOpen] = useState(false)
  const [pushRobotId, setPushRobotId] = useState<number | null>(null)
  const [pushContent, setPushContent] = useState('')

  const { data, isFetching } = useQuery({ queryKey: ['wc-robots', search], queryFn: () => wecomApi.list(search) })
  const saveMut = useMutation({ mutationFn: wecomApi.save, onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['wc-robots'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const delMut = useMutation({ mutationFn: wecomApi.delete, onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['wc-robots'] }) }, onError: (e: Error) => toast(e.message, 'error') })
  const testMut = useMutation({ mutationFn: wecomApi.test, onSuccess: () => toast('测试消息已发送', 'success'), onError: (e: Error) => toast(e.message, 'error') })
  const pushMut = useMutation({
    mutationFn: (p: { robotId: number; content: string }) => wecomApi.push(p),
    onSuccess: () => { toast('推送成功', 'success'); setPushOpen(false); setPushContent('') },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openAdd = useCallback(() => { setForm({}); setFormOpen(true) }, [])
  const openEdit = useCallback((row: WcRobot) => { setForm(row); setFormOpen(true) }, [])
  const openPush = useCallback((id: number) => { setPushRobotId(id); setPushOpen(true) }, [])

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'robotName', headerName: '机器人名称', flex: 1 },
    { field: 'webhookUrl', headerName: 'Webhook URL', flex: 1, minWidth: 200 },
    { field: 'status', headerName: '状态', width: 90, renderCell: ({ value }) => <Chip label={value === 1 ? '启用' : '禁用'} color={value === 1 ? 'success' : 'default'} size="small" /> },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions', headerName: '操作', width: 230, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button size="small" startIcon={<SendIcon />} onClick={() => openPush(row.id)}>推送</Button>
          <Button size="small" onClick={() => testMut.mutate(row.id)}>测试</Button>
          <Button size="small" color="error" onClick={() => setDeleteId(row.id)}>删除</Button>
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
    <Box sx={{ height: 'calc(100vh - 48px - 32px - 48px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid rows={data?.list ?? []} columns={columns} rowCount={data?.total ?? 0} loading={isFetching} paginationMode="server" paginationModel={{ page: search.page, pageSize: search.rows }} onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))} searchSlot={searchSlot} actionSlot={<Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新增机器人</Button>} sx={{ flex: 1 }} />
      <FormDialog open={formOpen} title={form.id ? '编辑机器人' : '新增机器人'} onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="机器人名称" value={form.robotName ?? ''} onChange={e => setForm(f => ({ ...f, robotName: e.target.value }))} fullWidth />
          <TextField label="Webhook URL" value={form.webhookUrl ?? ''} onChange={e => setForm(f => ({ ...f, webhookUrl: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>
      <FormDialog open={pushOpen} title="发送推送消息" onClose={() => setPushOpen(false)} onConfirm={() => pushRobotId !== null && pushMut.mutate({ robotId: pushRobotId, content: pushContent })} loading={pushMut.isPending}>
        <TextField label="消息内容" value={pushContent} onChange={e => setPushContent(e.target.value)} fullWidth multiline minRows={3} sx={{ mt: 1 }} />
      </FormDialog>
      <ConfirmDialog open={deleteId !== null} content="确定要删除该机器人吗？" onClose={() => setDeleteId(null)} onConfirm={() => deleteId !== null && delMut.mutate(deleteId)} loading={delMut.isPending} />
    </Box>
  )
}

function PushLogTab() {
  const [search, setSearch] = useState<PushLogQuery>({ page: 0, rows: 20 })
  const [query, setQuery] = useState(search)
  const { data, isFetching } = useQuery({ queryKey: ['wc-push-logs', search], queryFn: () => wecomApi.logList(search) })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'robotName', headerName: '机器人', width: 140 },
    { field: 'content', headerName: '消息内容', flex: 1, minWidth: 200 },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => <Chip label={value === 1 ? '成功' : '失败'} color={value === 1 ? 'success' : 'error'} size="small" />,
    },
    { field: 'errMsg', headerName: '错误信息', width: 200 },
    { field: 'createTime', headerName: '推送时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
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
    <Box sx={{ height: 'calc(100vh - 48px - 32px - 48px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        loading={isFetching}
        rowCount={data?.total ?? 0}
        paginationMode="server"
        paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
      />
    </Box>
  )
}

export default function RobotsPage() {
  const [tab, setTab] = useState(0)
  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
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


