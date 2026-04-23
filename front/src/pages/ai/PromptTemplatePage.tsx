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
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import RefreshIcon from '@mui/icons-material/Refresh'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi, type PromptTemplate } from '@/api/ai'
import { StandardDataGrid } from '@/components/base/StandardDataGrid'
import { useToast } from '@/contexts/ToastContext'
import { formatDate } from '@/utils/date'
import type { GridColDef } from '@mui/x-data-grid'

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

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['prompt-templates', typeFilter],
    queryFn: () => aiApi.promptTemplateList({ templateType: typeFilter || undefined, rows: 100 }),
  })

  // 前端搜索过滤
  const rows = (data?.list ?? []).filter((row) => {
    if (!searchKeyword) return true
    const keyword = searchKeyword.toLowerCase()
    return (
      row.templateName?.toLowerCase().includes(keyword) ||
      row.templateContent?.toLowerCase().includes(keyword) ||
      row.templateType?.toLowerCase().includes(keyword)
    )
  })

  const saveMutation = useMutation({
    mutationFn: async (p: Partial<PromptTemplate>): Promise<void> => {
      await aiApi.promptTemplateSave(p)
    },
    onSuccess: () => {
      toast('保存成功', 'success')
      void qc.invalidateQueries({ queryKey: ['prompt-templates'] })
      setOpen(false)
    },
    onError: () => toast('保存失败', 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number): Promise<void> => {
      await aiApi.promptTemplateDelete(id)
    },
    onSuccess: () => {
      toast('删除成功', 'success')
      void qc.invalidateQueries({ queryKey: ['prompt-templates'] })
    },
    onError: () => toast('删除失败', 'error'),
  })

  const renderMutation = useMutation({
    mutationFn: async (): Promise<void> => {
      const result = await aiApi.promptTemplateTestRender({
        templateContent: testTemplate?.templateContent || editing.templateContent || '',
        variables: testVars,
      })
      setPreviewResult(result.rendered || '(空结果)')
      setExtractedVars(result.variables || [])
      setMissingVars(result.missingVariables || [])
      setPreviewOpen(true)
    },
    onError: (error: Error) => toast(error.message || '渲染失败', 'error'),
  })

  const handleOpen = (row?: PromptTemplate) => {
    setEditing(row ? { ...row } : { templateType: '', templateName: '', templateContent: '', isActive: 1 })
    setOpen(true)
  }

  const handleTest = (row: PromptTemplate) => {
    setTestTemplate(row)
    setTestVars({})
    setTestDialogOpen(true)
  }

  const handleTestRender = () => {
    renderMutation.mutate()
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    {
      field: 'templateType',
      headerName: '类型',
      width: 120,
      renderCell: ({ value }) => (
        <Chip label={TEMPLATE_TYPE_LABELS[value as string] || value || '未分类'} size="small" variant="outlined" />
      ),
    },
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
      renderCell: ({ value }) => <Chip label={value ? '启用' : '停用'} size="small" color={value ? 'success' : 'default'} />,
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
              <IconButton size="small" color="primary" onClick={() => handleTest(row)}>
                <PlayArrowIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Button size="small" onClick={() => handleOpen(row)}>
              编辑
            </Button>
            <Button size="small" color="error" onClick={() => deleteMutation.mutate(row.id)}>
              删除
            </Button>
          </Stack>
        )
      },
    },
  ]

  return (
    <Box sx={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', gap: 1 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Prompt 模板
      </Typography>

      {/* 搜索和筛选栏 */}
      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
        <TextField
          size="small"
          placeholder="搜索模板名称或内容"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          sx={{ width: 300 }}
        />
        <TextField
          select
          size="small"
          label="模板类型"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
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
          <IconButton size="small" onClick={() => void refetch()}>
            <RefreshIcon />
          </IconButton>
        </Tooltip>
        <Box sx={{ flex: 1 }} />
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpen()}>
          新建模板
        </Button>
      </Stack>

      {/* 数据表格 */}
      <StandardDataGrid rows={rows} columns={columns} loading={isLoading} pageSizeOptions={[20, 50, 100]} />

      {/* 编辑对话框 */}
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editing.id ? '编辑模板' : '新建模板'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="模板类型"
                value={editing.templateType ?? ''}
                onChange={(e) => setEditing((p) => ({ ...p, templateType: e.target.value }))}
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
                label="模板名称"
                value={editing.templateName ?? ''}
                size="small"
                sx={{ flex: 1 }}
                onChange={(e) => setEditing((p) => ({ ...p, templateName: e.target.value }))}
              />
              <TextField
                select
                label="状态"
                value={editing.isActive ?? 1}
                size="small"
                sx={{ width: 100 }}
                onChange={(e) => setEditing((p) => ({ ...p, isActive: Number(e.target.value) }))}
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
          <Button variant="contained" onClick={() => saveMutation.mutate(editing)} disabled={saveMutation.isPending}>
            保存
          </Button>
        </DialogActions>
      </Dialog>

      {/* 测试对话框 */}
      <Dialog open={testDialogOpen} onClose={() => setTestDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>测试模板：{testTemplate?.templateName}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info" variant="outlined">
              模板内容：
              <Box component="pre" sx={{ fontSize: 12, mt: 1, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {testTemplate?.templateContent}
              </Box>
            </Alert>

            <Typography variant="subtitle2">填入变量值：</Typography>
            {testTemplate?.templateContent &&
              Array.from(testTemplate.templateContent.matchAll(/\{\{([^}]+)\}\}/g)).map(([, varName]) => {
                const cleanVarName = varName.trim()
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
              sx={{
                fontSize: 13,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                bgcolor: 'grey.50',
                p: 2,
                borderRadius: 1,
                m: 0,
                border: '1px solid',
                borderColor: 'divider',
              }}
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

