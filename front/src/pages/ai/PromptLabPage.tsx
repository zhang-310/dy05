import { useState } from 'react'
import {
  Box, Typography, Stack, Button, TextField, Chip, Card, CardContent,
  Dialog, DialogTitle, DialogContent, DialogActions, Select, MenuItem,
} from '@mui/material'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog } from '@/components/base'
import { aiApi, type PromptTemplate } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'

const TYPE_OPTIONS = ['chat', 'rag', 'summary', 'extraction', 'classification', 'generation']

const TYPE_LABELS: Record<string, string> = {
  chat: '对话',
  rag: 'RAG检索',
  summary: '摘要',
  extraction: '提取',
  classification: '分类',
  generation: '生成',
  script_generate: '话术生成',
  copy_generate: '文案生成',
  viral_analyze: '爆款分析',
  quality_check: '质量检查',
  agent_chat: '智能对话',
  image_prompt: '图片提示词',
}

export default function PromptLabPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, templateType: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<PromptTemplate>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [testOpen, setTestOpen] = useState(false)
  const [testTemplate, setTestTemplate] = useState<PromptTemplate | null>(null)
  const [testVars, setTestVars] = useState('')
  const [testResult, setTestResult] = useState('')

  const { data, isFetching } = useQuery({
    queryKey: ['prompt-lab', search],
    queryFn: () => aiApi.promptTemplateList(search),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<PromptTemplate>) => aiApi.promptTemplateSave(p),
    onSuccess: () => { toast('保存成功', 'success'); setFormOpen(false); qc.invalidateQueries({ queryKey: ['prompt-lab'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.promptTemplateDelete(id),
    onSuccess: () => { toast('删除成功', 'success'); setDeleteId(null); qc.invalidateQueries({ queryKey: ['prompt-lab'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const activateMut = useMutation({
    mutationFn: (id: number) => aiApi.promptTemplateGetActive(String(id)),
    onSuccess: () => { toast('已激活', 'success'); qc.invalidateQueries({ queryKey: ['prompt-lab'] }) },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const renderMut = useMutation({
    mutationFn: ({ templateContent, vars }: { templateContent: string; vars: Record<string, string> }) =>
      aiApi.promptTemplateTestRender({ templateContent, variables: vars }),
    onSuccess: (res) => setTestResult(res.rendered || JSON.stringify(res, null, 2)),
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const openTest = (row: PromptTemplate) => {
    setTestTemplate(row)
    setTestVars('{}')
    setTestResult('')
    setTestOpen(true)
  }
  const openEdit = (row: PromptTemplate) => {
    setForm(row)
    setFormOpen(true)
  }
  const openAdd = () => {
    setForm({})
    setFormOpen(true)
  }

  const handleRender = () => {
    if (!testTemplate) return
    let vars: Record<string, string> = {}
    try {
      const parsed = JSON.parse(testVars)
      vars = parsed as Record<string, string>
    } catch {
      toast('变量 JSON 格式错误', 'error')
      return
    }
    renderMut.mutate({ templateContent: testTemplate.templateContent, vars })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 160 },
    {
      field: 'templateType',
      headerName: '类型',
      width: 120,
      renderCell: ({ value }) => {
        const label = TYPE_LABELS[value as string] || value || '未分类'
        return <Chip label={label} size="small" variant="outlined" />
      },
    },
    {
      field: 'isActive',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => <Chip label={value ? '已激活' : '未激活'} color={value ? 'success' : 'default'} size="small" />,
    },
    { field: 'variables', headerName: '变量', width: 160 },
    { field: 'createTime', headerName: '创建时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
    {
      field: 'actions',
      headerName: '操作',
      width: 220,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" startIcon={<PlayArrowIcon fontSize="small" />} onClick={() => openTest(row as PromptTemplate)}>
            测试
          </Button>
          <Button size="small" onClick={() => activateMut.mutate((row as PromptTemplate).id)}>
            激活
          </Button>
          <Button size="small" onClick={() => openEdit(row as PromptTemplate)}>编辑</Button>
          <Button size="small" color="error" onClick={() => setDeleteId((row as PromptTemplate).id)}>删除</Button>
        </Stack>
      ) },
  ]

  const rows = data?.list ?? []
  const total = data?.total ?? rows.length

  const searchSlot = (
    <>
      <Select
        size="small"
        value={search.templateType}
        onChange={(e) => setSearch((s) => ({ ...s, templateType: e.target.value, page: 0 }))}
        displayEmpty
        sx={{ minWidth: 130 }}
      >
        <MenuItem value="">全部类型</MenuItem>
        {TYPE_OPTIONS.map((t) => (
          <MenuItem key={t} value={t}>
            {TYPE_LABELS[t] || t}
          </MenuItem>
        ))}
      </Select>
      <Button variant="contained" onClick={() => qc.invalidateQueries({ queryKey: ['prompt-lab'] })}>
        刷新
      </Button>
    </>
  )

  const actionSlot = <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>新建模板</Button>

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={rows as { id?: number }[]}
        columns={columns} rowCount={total} loading={isFetching}
        paginationMode="server"
        paginationModel={{ page: search.page, pageSize: search.rows }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
      />

      <FormDialog open={formOpen} title={form.id ? '编辑模板' : '新建模板'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="模板名称" value={form.templateName ?? ''} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} fullWidth />
          <Select value={form.templateType ?? ''} onChange={e => setForm(f => ({ ...f, templateType: e.target.value }))} displayEmpty fullWidth size="small">
            <MenuItem value="">选择类型</MenuItem>
            {TYPE_OPTIONS.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
          </Select>
          <TextField label="模板内容（支持 {{变量名}}）" value={form.templateContent ?? ''} onChange={e => setForm(f => ({ ...f, templateContent: e.target.value }))} fullWidth multiline minRows={6} />
          <TextField label="变量说明（逗号分隔）" value={form.variables ?? ''} onChange={e => setForm(f => ({ ...f, variables: e.target.value }))} fullWidth />
        </Stack>
      </FormDialog>

      <ConfirmDialog open={deleteId !== null} content="确定删除该 Prompt 模板？"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMut.mutate(deleteId)}
        loading={deleteMut.isPending} />

      <Dialog open={testOpen} onClose={() => setTestOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Prompt 测试 — {testTemplate?.templateName}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Card variant="outlined"><CardContent sx={{ py: 1 }}>
              <Typography variant="caption" color="text.secondary">模板内容</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5, fontFamily: 'monospace', fontSize: 12 }}>
                {testTemplate?.templateContent}
              </Typography>
            </CardContent></Card>
            <TextField label="变量 (JSON)" value={testVars} onChange={e => setTestVars(e.target.value)} fullWidth multiline minRows={3} sx={{ fontFamily: 'monospace' }} />
            {testResult && (
              <Card variant="outlined" sx={{ bgcolor: 'success.50' }}><CardContent sx={{ py: 1 }}>
                <Typography variant="caption" color="text.secondary">渲染结果</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 12 }}>{testResult}</Typography>
              </CardContent></Card>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestOpen(false)}>关闭</Button>
          <Button variant="contained" onClick={handleRender} disabled={renderMut.isPending}>
            {renderMut.isPending ? '渲染中...' : '渲染测试'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}