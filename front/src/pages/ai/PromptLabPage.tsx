import { useState } from 'react'
import {
  Box, Typography, Stack, Button, TextField, Chip, Card, CardContent,
  Dialog, DialogTitle, DialogContent, DialogActions, Select, MenuItem,
  Alert, Grid,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, FormDialog, ConfirmDialog, PageHeader, DataGridEmptyOverlay } from '@/components/base'
import { aiApi, type PromptTemplate } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows, readTotal } from '@/utils/response-normalize'

const TYPE_OPTIONS = ['script_generate', 'copy_generate', 'viral_analyze', 'quality_check', 'agent_chat', 'image_prompt', 'chat', 'rag', 'summary', 'extraction', 'classification', 'generation']

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

function templateCodeOf(row: Partial<PromptTemplate>): string {
  return String(row.templateCode ?? row.templateType ?? '')
}

function isTemplateActive(value: PromptTemplate['isActive'] | string | null | undefined): boolean {
  return value === true || value === 1 || value === '1' || value === 'true'
}

export default function PromptLabPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [search, setSearch] = useState({ page: 0, rows: 20, templateCode: '' })
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<Partial<PromptTemplate>>({})
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [testOpen, setTestOpen] = useState(false)
  const [testTemplate, setTestTemplate] = useState<PromptTemplate | null>(null)
  const [testVars, setTestVars] = useState('')
  const [testResult, setTestResult] = useState('')
  const [testMeta, setTestMeta] = useState<{ variables: string[]; missingVariables: string[] } | null>(null)
  const [testError, setTestError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['prompt-lab', search],
    queryFn: () => aiApi.promptTemplateList(search),
  })

  const saveMut = useMutation({
    mutationFn: (p: Partial<PromptTemplate>) => aiApi.promptTemplateSave({
      ...p,
      templateCode: templateCodeOf(p),
      variantName: p.variantName?.trim() || 'default',
      isActive: isTemplateActive(p.isActive),
      status: isTemplateActive(p.isActive) ? 1 : 0,
    }),
    onSuccess: () => {
      toast('保存成功', 'success')
      setActionError(null)
      setFormOpen(false)
      qc.invalidateQueries({ queryKey: ['prompt-lab'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`Prompt 模板保存失败：${message}。来源：/ai/prompt-template/save。`)
      toast(`保存失败：${message}`, 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => aiApi.promptTemplateDelete(id),
    onSuccess: () => {
      toast('删除成功', 'success')
      setActionError(null)
      setDeleteId(null)
      qc.invalidateQueries({ queryKey: ['prompt-lab'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`Prompt 模板删除失败：${message}。来源：/ai/prompt-template/delete；失败时页面不会移除本地行。`)
      setDeleteId(null)
      toast(`删除失败：${message}`, 'error')
    },
  })

  const activateMut = useMutation({
    mutationFn: (row: PromptTemplate) => aiApi.promptTemplateSave({
      ...row,
      templateCode: templateCodeOf(row),
      isActive: true,
      status: 1,
    }),
    onSuccess: () => {
      toast('已启用', 'success')
      setActionError(null)
      qc.invalidateQueries({ queryKey: ['prompt-lab'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`Prompt 模板启用失败：${message}。来源：/ai/prompt-template/save；启用不是 get-active，而是保存 isActive/status。`)
      toast(`启用失败：${message}`, 'error')
    },
  })

  const renderMut = useMutation({
    mutationFn: async ({ id, templateContent, vars }: { id?: number; templateContent: string; vars: Record<string, string> }) => {
      const extracted = await aiApi.promptTemplateExtractVariables(templateContent)
      const res = await aiApi.promptTemplateTestRender({ templateContent, variables: vars })
      if (id != null) {
        await aiApi.promptTemplateRecordUsage(id)
        void qc.invalidateQueries({ queryKey: ['prompt-lab'] })
      }
      return {
        ...res,
        variables: res.variables?.length ? res.variables : extracted,
      }
    },
    onSuccess: (res) => {
      setTestError(null)
      setTestMeta({ variables: res.variables || [], missingVariables: res.missingVariables || [] })
      setTestResult(res.rendered || JSON.stringify(res, null, 2))
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setTestError(`${message}。来源：/ai/prompt-template/extract-variables、/test-render 或 /record-usage。`)
      toast(`渲染失败：${message}`, 'error')
    },
  })

  const openTest = (row: PromptTemplate) => {
    setTestTemplate(row)
    setTestVars('{}')
    setTestResult('')
    setTestMeta(null)
    setTestError(null)
    setTestOpen(true)
  }
  const openEdit = (row: PromptTemplate) => {
    setActionError(null)
    setForm(row)
    setFormOpen(true)
  }
  const openAdd = () => {
    setActionError(null)
    setForm({ variantName: 'default', isActive: true })
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
    renderMut.mutate({ id: testTemplate.id, templateContent: testTemplate.templateContent, vars })
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 160 },
    {
      field: 'templateCode',
      headerName: '模板编码',
      width: 120,
      renderCell: ({ row }) => {
        const code = templateCodeOf(row as PromptTemplate)
        const label = TYPE_LABELS[code] || code || '未分类'
        return <Chip label={label} size="small" variant="outlined" />
      },
    },
    { field: 'variantName', headerName: '变体', width: 100, valueGetter: (_, row) => row.variantName ?? 'default' },
    {
      field: 'isActive',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => <Chip label={isTemplateActive(value as PromptTemplate['isActive']) ? '已启用' : '未启用'} color={isTemplateActive(value as PromptTemplate['isActive']) ? 'success' : 'default'} size="small" />,
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
          <Button
            size="small"
            startIcon={<PlayArrowIcon fontSize="small" />}
            data-testid="prompt-lab-test-open"
            data-template-id={String((row as PromptTemplate).id)}
            data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
            data-no-static-render-fallback="true"
            onClick={() => openTest(row as PromptTemplate)}
          >
            测试
          </Button>
          <Button
            size="small"
            data-testid="prompt-lab-activate-save"
            data-template-id={String((row as PromptTemplate).id)}
            data-source-endpoint="/ai/prompt-template/save"
            data-activation-strategy="save-isActive-status"
            data-no-get-active-for-enable="true"
            onClick={() => activateMut.mutate(row as PromptTemplate)}
          >
            启用
          </Button>
          <Button
            size="small"
            data-testid="prompt-lab-edit-open"
            data-template-id={String((row as PromptTemplate).id)}
            data-source-endpoint="/ai/prompt-template/save"
            data-preserves-form-input="true"
            onClick={() => openEdit(row as PromptTemplate)}
          >编辑</Button>
          <Button
            size="small"
            color="error"
            data-testid="prompt-lab-delete-open"
            data-template-id={String((row as PromptTemplate).id)}
            data-source-endpoint="/ai/prompt-template/delete"
            data-no-local-mutation-fallback="true"
            onClick={() => setDeleteId((row as PromptTemplate).id)}
          >删除</Button>
        </Stack>
      ) },
  ]

  const rows = normalizeRows<PromptTemplate>(data)
  const total = readTotal(data, rows.length)

  const searchSlot = (
    <>
      <Select
        size="small"
        value={search.templateCode}
        onChange={(e) => setSearch((s) => ({ ...s, templateCode: e.target.value, page: 0 }))}
        displayEmpty
        inputProps={{
          'data-testid': 'prompt-lab-template-code-filter',
          'data-source-endpoint': '/ai/prompt-template/list',
          'data-server-filter': 'templateCode',
        }}
        sx={{ minWidth: 130 }}
      >
        <MenuItem value="">全部类型</MenuItem>
        {TYPE_OPTIONS.map((t) => (
          <MenuItem key={t} value={t}>
            {TYPE_LABELS[t] || t}
          </MenuItem>
        ))}
      </Select>
      <Button
        variant="contained"
        data-testid="prompt-lab-refresh-list"
        data-source-endpoint="/ai/prompt-template/list"
        data-refresh-scope="template-list-only"
        onClick={() => qc.invalidateQueries({ queryKey: ['prompt-lab'] })}
      >
        刷新
      </Button>
    </>
  )

  const actionSlot = (
    <Button
      variant="contained"
      startIcon={<AddIcon />}
      data-testid="prompt-lab-create-open"
      data-source-endpoint="/ai/prompt-template/save"
      data-no-local-mutation-fallback="true"
      data-preserves-form-input="true"
      onClick={openAdd}
    >
      新建模板
    </Button>
  )

  return (
    <Box
      data-testid="prompt-lab-page"
      data-ready-endpoints="/ai/prompt-template/list,/ai/prompt-template/save,/ai/prompt-template/delete,/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
      data-unsupported-endpoints="/ai/prompt-template/mock,/ai/prompt-template/local-cache,/ai/prompt-template/static-template,/ai/prompt-template/get-active-for-enable,/ai/prompt-template/static-render"
      data-no-local-template-fallback="true"
      data-no-static-template-fallback="true"
      sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}
    >
      <PageHeader
        title="Prompt 实验室"
        breadcrumbs={[{ label: 'AI 中心' }, { label: 'Prompt 实验室' }]}
        subtitle="Prompt 模板 CRUD、变量渲染和启用状态管理；启用通过保存 isActive/status 完成。"
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="prompt-lab-boundary-contract"
        data-activation-strategy="save-isActive-status"
        data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
        sx={{ mb: 2 }}
      >
        真实契约为 `/ai/prompt-template/*`。查询字段是 `templateCode/variantName/isActive`；本页“启用”会保存当前模板，不再误调用 `get-active`。
        测试模板按后端顺序执行 <code>extract-variables</code> → <code>test-render</code> → <code>record-usage</code>。
      </Alert>

      <Grid
        container
        spacing={2}
        data-testid="prompt-lab-summary-cards"
        data-row-count={String(rows.length)}
        data-active-count={String(rows.filter(r => isTemplateActive((r as PromptTemplate).isActive)).length)}
        data-code-count={String(new Set(rows.map(r => templateCodeOf(r as PromptTemplate)).filter(Boolean)).size)}
        sx={{ mb: 2 }}
      >
        {[
          ['当前页模板', rows.length],
          ['已启用', rows.filter(r => isTemplateActive((r as PromptTemplate).isActive)).length],
          ['模板编码', new Set(rows.map(r => templateCodeOf(r as PromptTemplate)).filter(Boolean)).size],
          ['累计使用', rows.reduce((sum, r) => sum + Number((r as PromptTemplate).usageCount ?? 0), 0)],
        ].map(([label, value]) => (
          <Grid item xs={6} md={3} key={String(label)}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight={700}>{value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Alert
          severity="error"
          data-testid="prompt-lab-list-error"
          data-source-endpoint="/ai/prompt-template/list"
          data-no-local-template-fallback="true"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          Prompt 模板加载失败：{getErrorMessage(error)}。请检查 `/ai/prompt-template/list`。
        </Alert>
      )}

      {actionError ? (
        <Alert
          severity="error"
          data-testid="prompt-lab-action-error"
          data-no-local-mutation-fallback="true"
          data-preserves-form-input="true"
          sx={{ mb: 2 }}
          onClose={() => setActionError(null)}
        >
          {actionError}
        </Alert>
      ) : null}

      <Box
        data-testid="prompt-lab-grid"
        data-source-endpoint="/ai/prompt-template/list"
        data-pagination-mode="server"
        data-no-local-template-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows as { id?: number }[]}
          columns={columns} rowCount={total} loading={isFetching}
          paginationMode="server"
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          searchSlot={searchSlot} actionSlot={actionSlot} sx={{ flex: 1 }}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      {!isFetching && rows.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="prompt-lab-empty-state"
          data-source-endpoint="/ai/prompt-template/list"
          data-no-static-template-fallback="true"
          sx={{ mt: 2 }}
        >
          暂无 Prompt 模板。请检查 <code>/ai/prompt-template/list</code> 是否返回 `ai_prompt_template` 数据；页面不会填充静态模板占位。
        </Alert>
      ) : null}

      <FormDialog open={formOpen} title={form.id ? '编辑模板' : '新建模板'}
        onClose={() => setFormOpen(false)} onConfirm={() => saveMut.mutate(form)} loading={saveMut.isPending}>
        <Stack
          spacing={2}
          data-testid="prompt-lab-edit-dialog-contract"
          data-source-endpoint="/ai/prompt-template/save"
          data-preserves-form-input="true"
          sx={{ pt: 1 }}
        >
          <TextField label="模板名称" value={form.templateName ?? ''} onChange={e => setForm(f => ({ ...f, templateName: e.target.value }))} fullWidth />
          <Select value={templateCodeOf(form)} onChange={e => setForm(f => ({ ...f, templateCode: e.target.value }))} displayEmpty fullWidth size="small">
            <MenuItem value="">选择模板编码</MenuItem>
            {TYPE_OPTIONS.map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
          </Select>
          <TextField label="变体 variantName" value={form.variantName ?? 'default'} onChange={e => setForm(f => ({ ...f, variantName: e.target.value }))} fullWidth />
          <Select value={isTemplateActive(form.isActive) ? 1 : 0} onChange={e => setForm(f => ({ ...f, isActive: Number(e.target.value) === 1 }))} fullWidth size="small">
            <MenuItem value={1}>启用</MenuItem>
            <MenuItem value={0}>停用</MenuItem>
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
          <Stack
            spacing={2}
            data-testid="prompt-lab-test-dialog-contract"
            data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
            data-no-static-render-fallback="true"
            sx={{ mt: 1 }}
          >
            <Card variant="outlined"><CardContent sx={{ py: 1 }}>
              <Typography variant="caption" color="text.secondary">模板内容</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5, fontFamily: 'monospace', fontSize: 12 }}>
                {testTemplate?.templateContent}
              </Typography>
            </CardContent></Card>
            <TextField label="变量 (JSON)" value={testVars} onChange={e => setTestVars(e.target.value)} fullWidth multiline minRows={3} sx={{ fontFamily: 'monospace' }} />
            {testResult && (
              <Card
                variant="outlined"
                data-testid="prompt-lab-render-result-surface"
                data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
                data-no-static-render-fallback="true"
                sx={(theme) => ({
                  bgcolor: theme.palette.mode === 'dark'
                    ? alpha(theme.palette.success.main, 0.12)
                    : alpha(theme.palette.success.main, 0.08),
                  borderColor: alpha(theme.palette.success.main, theme.palette.mode === 'dark' ? 0.35 : 0.24),
                })}
              ><CardContent sx={{ py: 1 }}>
                <Typography variant="caption" color="text.secondary">渲染结果</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 12 }}>{testResult}</Typography>
              </CardContent></Card>
            )}
            {testMeta && (
              <Alert severity={testMeta.missingVariables.length > 0 ? 'warning' : 'success'} variant="outlined">
                后端变量：{testMeta.variables.length > 0 ? testMeta.variables.join(', ') : '无'}
                {testMeta.missingVariables.length > 0 ? `；缺少变量：${testMeta.missingVariables.join(', ')}` : '；已记录一次模板使用'}
              </Alert>
            )}
            {testError ? (
              <Alert
                severity="error"
                data-testid="prompt-lab-render-error"
                data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
                data-no-static-render-fallback="true"
              >
                渲染失败：{testError}
              </Alert>
            ) : null}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestOpen(false)}>关闭</Button>
          <Button
            variant="contained"
            data-testid="prompt-lab-render-submit"
            data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
            data-no-static-render-fallback="true"
            onClick={handleRender}
            disabled={renderMut.isPending}
          >
            {renderMut.isPending ? '渲染中...' : '渲染测试'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
