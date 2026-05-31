import { useState } from 'react'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  MenuItem,
  Stack,
  Typography,
  Tooltip,
  IconButton,
  Alert,
  Grid,
  Card,
  CardContent,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi, type PromptTemplate } from '@/api/ai'
import { ConfirmDialog, DataGridEmptyOverlay, PageHeader, StandardDataGrid } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'
import type { GridColDef } from '@mui/x-data-grid'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeRows } from '@/utils/response-normalize'

const TEMPLATE_TYPES = ['script_generate', 'copy_generate', 'viral_analyze', 'quality_check', 'agent_chat', 'image_prompt']

const TEMPLATE_TYPE_LABELS: Record<string, string> = {
  script_generate: '话术生成',
  copy_generate: '文案生成',
  viral_analyze: '爆款分析',
  quality_check: '质量检查',
  agent_chat: '智能对话',
  image_prompt: '图片提示词',
}

function formatNumber(num: number | null | undefined): string {
  if (num == null) return '0'
  return num.toString()
}

function templateCodeOf(row: Partial<PromptTemplate>): string {
  return String(row.templateCode ?? row.templateType ?? '')
}

function isTemplateActive(value: PromptTemplate['isActive'] | string | null | undefined): boolean {
  return value === true || value === 1 || value === '1' || value === 'true'
}

export default function PromptTemplatePage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [typeFilter, setTypeFilter] = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [open, setOpen] = useState(false)
  const [testDialogOpen, setTestDialogOpen] = useState(false)
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewResult, setPreviewResult] = useState('')
  const [extractedVars, setExtractedVars] = useState<string[]>([])
  const [missingVars, setMissingVars] = useState<string[]>([])
  const [editing, setEditing] = useState<Partial<PromptTemplate>>({})
  const [testTemplate, setTestTemplate] = useState<PromptTemplate | null>(null)
  const [testVars, setTestVars] = useState<Record<string, string>>({})
  const [testExtractedVars, setTestExtractedVars] = useState<string[]>([])
  const [testError, setTestError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['prompt-templates', typeFilter],
    queryFn: () => aiApi.promptTemplateList({ templateCode: typeFilter || undefined, rows: 100 }),
  })

  // 前端搜索过滤
  const rows = normalizeRows<PromptTemplate>(data).filter((row) => {
    if (!searchKeyword) return true
    const keyword = searchKeyword.toLowerCase()
    return (
      row.templateName?.toLowerCase().includes(keyword) ||
      row.templateContent?.toLowerCase().includes(keyword) ||
      templateCodeOf(row).toLowerCase().includes(keyword) ||
      row.variantName?.toLowerCase().includes(keyword)
    )
  })

  const saveMutation = useMutation({
    mutationFn: async (p: Partial<PromptTemplate>): Promise<void> => {
      await aiApi.promptTemplateSave({
        ...p,
        templateCode: templateCodeOf(p),
        variantName: p.variantName?.trim() || 'default',
        isActive: isTemplateActive(p.isActive),
        status: isTemplateActive(p.isActive) ? 1 : 0,
      })
    },
    onSuccess: () => {
      toast('保存成功', 'success')
      setActionError(null)
      void qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      setOpen(false)
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`Prompt 模板保存失败：${message}。来源：/ai/prompt-template/save。`)
      toast(`保存失败：${message}`, 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number): Promise<void> => {
      await aiApi.promptTemplateDelete(id)
    },
    onSuccess: () => {
      toast('删除成功', 'success')
      setActionError(null)
      void qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      setDeleteId(null)
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setActionError(`Prompt 模板删除失败：${message}。来源：/ai/prompt-template/delete；失败时页面不会移除本地行。`)
      setDeleteId(null)
      toast(`删除失败：${message}`, 'error')
    },
  })

  const renderMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      const source = testTemplate?.templateContent || editing.templateContent || ''
      const vars = await aiApi.promptTemplateExtractVariables(source)
      setTestExtractedVars(vars)
      const result = await aiApi.promptTemplateTestRender({
        templateContent: source,
        variables: testVars,
      })
      setPreviewResult(result.rendered || '(空结果)')
      setExtractedVars(result.variables?.length ? result.variables : vars)
      setMissingVars(result.missingVariables || [])
      if (testTemplate?.id != null) {
        await aiApi.promptTemplateRecordUsage(testTemplate.id)
        void qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      }
      setPreviewOpen(true)
    },
    onError: (error) => {
      const message = getErrorMessage(error)
      setTestError(`${message}。来源：/ai/prompt-template/extract-variables、/test-render 或 /record-usage。`)
      toast(`渲染失败：${message}`, 'error')
    },
  })

  const handleOpen = (row?: PromptTemplate) => {
    setActionError(null)
    setEditing(row ? { ...row } : { templateCode: '', variantName: 'default', templateName: '', templateContent: '', isActive: true })
    setOpen(true)
  }

  const handleTest = (row: PromptTemplate) => {
    setTestTemplate(row)
    setTestVars({})
    setTestExtractedVars([])
    setTestError(null)
    setTestDialogOpen(true)
  }

  const handleTestRender = () => {
    renderMutation.mutate()
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    {
      field: 'templateCode',
      headerName: '模板编码',
      width: 120,
      renderCell: ({ row }) => {
        const code = templateCodeOf(row as PromptTemplate)
        return <Chip label={TEMPLATE_TYPE_LABELS[code] || code || '未分类'} size="small" variant="outlined" />
      },
    },
    { field: 'variantName', headerName: '变体', width: 100, valueGetter: (_, row) => row.variantName ?? 'default' },
    { field: 'templateName', headerName: '模板名称', flex: 1, minWidth: 160 },
    {
      field: 'templateContent',
      headerName: '内容预览',
      flex: 2,
      minWidth: 300,
      renderCell: ({ value }) => {
        const content = String(value ?? '')
        return (
          <Tooltip title={content} placement="top-start">
            <Box
              sx={{
                fontSize: 12,
                color: 'text.secondary',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                cursor: 'pointer',
              }}
            >
              {content.slice(0, 100)}
              {content.length > 100 && '...'}
            </Box>
          </Tooltip>
        )
      },
    },
    {
      field: 'usageCount',
      headerName: '使用次数',
      width: 100,
      renderCell: ({ value }) => <Typography variant="body2">{formatNumber(value as number)}</Typography>,
    },
    {
      field: 'lastUsedAt',
      headerName: '最后使用',
      width: 160,
      renderCell: ({ value }) => (
        <Typography variant="body2" color="text.secondary">
          {value ? formatDate(String(value)) : '未使用'}
        </Typography>
      ),
    },
    {
      field: 'isActive',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => <Chip label={isTemplateActive(value as PromptTemplate['isActive']) ? '启用' : '停用'} size="small" color={isTemplateActive(value as PromptTemplate['isActive']) ? 'success' : 'default'} />,
    },
    { field: 'createTime', headerName: '创建时间', width: 160 },
    {
      field: '_actions',
      headerName: '操作',
      width: 200,
      sortable: false,
      renderCell: ({ row: r }) => {
        const row = r as PromptTemplate
        return (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="测试模板">
              <IconButton
                size="small"
                color="primary"
                data-testid="prompt-template-test-open"
                data-template-id={String(row.id)}
                data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
                data-no-static-render-fallback="true"
                onClick={() => handleTest(row)}
              >
                <PlayArrowIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Button
              size="small"
              data-testid="prompt-template-edit-open"
              data-template-id={String(row.id)}
              data-source-endpoint="/ai/prompt-template/save"
              data-preserves-form-input="true"
              onClick={() => handleOpen(row)}
            >
              编辑
            </Button>
            <Button
              size="small"
              color="error"
              data-testid="prompt-template-delete-open"
              data-template-id={String(row.id)}
              data-source-endpoint="/ai/prompt-template/delete"
              data-no-local-mutation-fallback="true"
              onClick={() => setDeleteId(row.id)}
            >
              删除
            </Button>
          </Stack>
        )
      },
    },
  ]

  return (
    <Box
      data-testid="prompt-template-page"
      data-ready-endpoints="/ai/prompt-template/list,/ai/prompt-template/save,/ai/prompt-template/delete,/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
      data-unsupported-endpoints="/ai/prompt-template/mock,/ai/prompt-template/local-cache,/ai/prompt-template/static-template,/ai/prompt-template/activate-by-id,/ai/prompt-template/static-render"
      data-no-local-template-fallback="true"
      data-no-static-template-fallback="true"
      sx={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', gap: 1 }}
    >
      <PageHeader
        title="Prompt 模板"
        breadcrumbs={[{ label: 'AI 中心' }, { label: 'Prompt 模板' }]}
        subtitle="对齐 `/ai/prompt-template/*`：查询使用 templateCode/variantName/isActive，启停通过保存模板状态完成。"
        actions={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            data-testid="prompt-template-create-open"
            data-source-endpoint="/ai/prompt-template/save"
            data-no-local-mutation-fallback="true"
            data-preserves-form-input="true"
            onClick={() => handleOpen()}
          >
            新建模板
          </Button>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="prompt-template-boundary-contract"
        data-activation-strategy="save-isActive-status"
        data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
      >
        后端没有“按 ID 激活模板”的独立接口；模板是否启用由 `isActive/status` 保存。获取运行中模板使用 `get-active(templateCode, variantName)` 回退用户模板和系统模板。
        测试模板会先调用 `extract-variables`，再调用 `test-render`，成功后通过 `record-usage` 更新使用次数。
      </Alert>

      <Grid
        container
        spacing={2}
        data-testid="prompt-template-summary-cards"
        data-row-count={String(rows.length)}
        data-active-count={String(rows.filter(r => isTemplateActive(r.isActive)).length)}
        data-code-count={String(new Set(rows.map(templateCodeOf).filter(Boolean)).size)}
        sx={{ my: 1 }}
      >
        {[
          ['当前页模板', rows.length],
          ['已启用', rows.filter(r => isTemplateActive(r.isActive)).length],
          ['系统/共享编码', new Set(rows.map(templateCodeOf).filter(Boolean)).size],
          ['累计使用', rows.reduce((sum, r) => sum + Number(r.usageCount ?? 0), 0)],
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
          data-testid="prompt-template-list-error"
          data-source-endpoint="/ai/prompt-template/list"
          data-no-local-template-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          Prompt 模板加载失败：{getErrorMessage(error)}。请检查 `/ai/prompt-template/list`。
        </Alert>
      )}

      {actionError ? (
        <Alert
          severity="error"
          data-testid="prompt-template-action-error"
          data-no-local-mutation-fallback="true"
          data-preserves-form-input="true"
          onClose={() => setActionError(null)}
        >
          {actionError}
        </Alert>
      ) : null}

      {/* 搜索和筛选栏 */}
      <Stack
        direction="row"
        spacing={2}
        alignItems="center"
        flexWrap="wrap"
        useFlexGap
        data-testid="prompt-template-filter-bar"
        data-client-keyword-filter="true"
        data-server-type-filter="templateCode"
      >
        <TextField
          size="small"
          placeholder="搜索模板名称或内容"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          inputProps={{
            'data-testid': 'prompt-template-keyword-filter',
            'data-client-filter-only': 'true',
          }}
          sx={{ width: 300 }}
        />
        <TextField
          select
          size="small"
          label="模板类型"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          inputProps={{
            'data-testid': 'prompt-template-type-filter',
            'data-source-endpoint': '/ai/prompt-template/list',
            'data-server-filter': 'templateCode',
          }}
          sx={{ width: 180 }}
        >
          <MenuItem value="">全部类型</MenuItem>
          {TEMPLATE_TYPES.map((t) => (
            <MenuItem key={t} value={t}>
              {TEMPLATE_TYPE_LABELS[t] || t}
            </MenuItem>
          ))}
        </TextField>
        <Tooltip title="刷新列表">
          <IconButton
            size="small"
            data-testid="prompt-template-refresh-list"
            data-source-endpoint="/ai/prompt-template/list"
            data-refresh-scope="template-list-only"
            onClick={() => void refetch()}
          >
            <RefreshIcon />
          </IconButton>
        </Tooltip>
        <Box sx={{ flex: 1 }} />
      </Stack>

      {/* 数据表格 */}
      <Box
        data-testid="prompt-template-grid"
        data-source-endpoint="/ai/prompt-template/list"
        data-pagination-mode="client-filtered-server-list"
        data-no-local-template-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isLoading}
          pageSizeOptions={[20, 50, 100]}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      {!isLoading && rows.length === 0 ? (
        <Alert
          severity="warning"
          data-testid="prompt-template-empty-state"
          data-source-endpoint="/ai/prompt-template/list"
          data-no-static-template-fallback="true"
        >
          暂无 Prompt 模板。请检查 <code>/ai/prompt-template/list</code> 是否返回 `ai_prompt_template` 数据；页面不会填充静态模板占位。
        </Alert>
      ) : null}

      {/* 编辑对话框 */}
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editing.id ? '编辑模板' : '新建模板'}</DialogTitle>
        <DialogContent>
          <Stack
            spacing={2}
            data-testid="prompt-template-edit-dialog-contract"
            data-source-endpoint="/ai/prompt-template/save"
            data-preserves-form-input="true"
            sx={{ mt: 1 }}
          >
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="模板编码 templateCode"
                value={templateCodeOf(editing)}
                onChange={(e) => setEditing((p) => ({ ...p, templateCode: e.target.value }))}
                size="small"
                sx={{ width: 200 }}
              >
                {TEMPLATE_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {TEMPLATE_TYPE_LABELS[t] || t}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="变体 variantName"
                value={editing.variantName ?? 'default'}
                size="small"
                sx={{ width: 160 }}
                onChange={(e) => setEditing((p) => ({ ...p, variantName: e.target.value }))}
              />
              <TextField
                label="模板名称"
                value={editing.templateName ?? ''}
                size="small"
                sx={{ flex: 1 }}
                onChange={(e) => setEditing((p) => ({ ...p, templateName: e.target.value }))}
              />
              <TextField
                select
                label="状态"
                value={isTemplateActive(editing.isActive) ? 1 : 0}
                size="small"
                sx={{ width: 100 }}
                onChange={(e) => setEditing((p) => ({ ...p, isActive: Number(e.target.value) === 1 }))}
              >
                <MenuItem value={1}>启用</MenuItem>
                <MenuItem value={0}>停用</MenuItem>
              </TextField>
            </Stack>
            <TextField
              label="模板内容（支持 {{变量}} 占位符）"
              value={editing.templateContent ?? ''}
              onChange={(e) => setEditing((p) => ({ ...p, templateContent: e.target.value }))}
              multiline
              minRows={10}
              fullWidth
              size="small"
              placeholder="例如：为 {{product}} 生成一段 {{style}} 风格的文案..."
            />
            <Alert severity="info" variant="outlined">
              使用 <code>{'{{'}</code>变量名<code>{'}}'}</code> 格式定义变量占位符，测试时可以填入具体值预览效果
            </Alert>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>取消</Button>
          <Button
            variant="contained"
            data-testid="prompt-template-save-submit"
            data-source-endpoint="/ai/prompt-template/save"
            data-preserves-form-input="true"
            onClick={() => saveMutation.mutate(editing)}
            disabled={saveMutation.isPending}
          >
            保存
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={deleteId !== null}
        title="删除 Prompt 模板"
        content="确定删除该 Prompt 模板？删除后运行链路无法再命中此模板。"
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId !== null && deleteMutation.mutate(deleteId)}
        loading={deleteMutation.isPending}
      />

      {/* 测试对话框 */}
      <Dialog open={testDialogOpen} onClose={() => setTestDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>测试模板：{testTemplate?.templateName}</DialogTitle>
        <DialogContent>
          <Stack
            spacing={2}
            data-testid="prompt-template-test-dialog-contract"
            data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
            data-no-static-render-fallback="true"
            sx={{ mt: 1 }}
          >
            <Alert severity="info" variant="outlined">
              模板内容：
              <Box component="pre" sx={{ fontSize: 12, mt: 1, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {testTemplate?.templateContent}
              </Box>
            </Alert>

            <Typography variant="subtitle2">填入变量值：</Typography>
            {testTemplate?.templateContent &&
              Array.from(new Set(Array.from(testTemplate.templateContent.matchAll(/\{\{([^}]+)\}\}/g)).map(([, varName]) => varName.trim()))).map((cleanVarName) => {
                return (
                  <TextField
                    key={cleanVarName}
                    label={cleanVarName}
                    value={testVars[cleanVarName] || ''}
                    onChange={(e) => setTestVars((prev) => ({ ...prev, [cleanVarName]: e.target.value }))}
                    size="small"
                    fullWidth
                    placeholder={`输入 ${cleanVarName} 的值`}
                  />
                )
              })}

            {testExtractedVars.length > 0 && (
              <Alert severity="info" variant="outlined">
                后端变量提取结果：{testExtractedVars.join(', ')}
              </Alert>
            )}

            {testError ? (
              <Alert
                severity="error"
                data-testid="prompt-template-render-error"
                data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
                data-no-static-render-fallback="true"
              >
                渲染失败：{testError}
              </Alert>
            ) : null}

            {testTemplate?.templateContent && !/\{\{[^}]+\}\}/.test(testTemplate.templateContent) && (
              <Alert severity="warning">此模板没有定义变量</Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestDialogOpen(false)}>取消</Button>
          <Button
            variant="contained"
            startIcon={<PlayArrowIcon />}
            data-testid="prompt-template-render-submit"
            data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
            data-no-static-render-fallback="true"
            onClick={handleTestRender}
            disabled={renderMutation.isPending}
          >
            渲染预览
          </Button>
        </DialogActions>
      </Dialog>

      {/* 预览结果对话框 */}
      <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>渲染结果</DialogTitle>
        <DialogContent>
          <Stack spacing={2}>
            {missingVars.length > 0 && (
              <Alert severity="warning">缺少变量值：{missingVars.join(', ')}</Alert>
            )}

            {extractedVars.length > 0 && (
              <Alert severity="info" variant="outlined">
                检测到的变量：{extractedVars.join(', ')}
              </Alert>
            )}

            <Box
              component="pre"
              data-testid="prompt-template-render-preview-surface"
              data-render-chain="/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
              data-no-static-render-fallback="true"
              sx={(theme) => ({
                fontSize: 13,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                bgcolor: theme.palette.mode === 'dark'
                  ? theme.palette.background.default
                  : alpha(theme.palette.common.black, 0.025),
                p: 2,
                borderRadius: 1,
                m: 0,
                border: '1px solid',
                borderColor: 'divider',
              })}
            >
              {previewResult}
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreviewOpen(false)}>关闭</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
