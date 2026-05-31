import { useEffect, useState, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Button, Stack, Chip,
  Grid, Alert, TextField, Select, MenuItem, FormControl, InputLabel,
  LinearProgress,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import {
  Add as AddIcon, Delete as DeleteIcon,
  ArrowForward as NextIcon, Save as SaveIcon,
} from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { shortvideoApi } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import type { WorkflowTemplate, WorkflowTemplateStep } from '@/types/shortvideo'
import { getErrorMessage } from '@/utils/errorHandler'

type NodeType = 'script' | 'shotList' | 'material' | 'videoGen' | 'edit' | 'compose' | 'review' | 'publish'

interface WorkflowNode {
  id: string
  type: NodeType
  label: string
  assignee: string
  durationDays: number
}

const NODE_TYPE_LABELS: Record<NodeType, string> = {
  script: '脚本策划',
  shotList: '分镜设计',
  material: '素材准备',
  videoGen: '图生视频',
  edit: '视频剪辑',
  compose: '智能合成',
  review: '审核',
  publish: '发布',
}

const WORKFLOW_TEMPLATE_ENDPOINTS = {
  list: '/short-video/workflow-template/list',
  get: '/short-video/workflow-template/get',
} as const
const WORKFLOW_READY_ENDPOINTS = [
  WORKFLOW_TEMPLATE_ENDPOINTS.list,
  WORKFLOW_TEMPLATE_ENDPOINTS.get,
  'browser-local-draft:sv-workflow-draft',
] as const
const WORKFLOW_UNSUPPORTED_ENDPOINTS = [
  '/short-video/workflow-template/mock',
  '/short-video/workflow-template/local-list',
  '/short-video/workflow-template/local-get',
  '/short-video/workflow/project-save',
  '/short-video/workflow/project-execute',
  '/short-video/workflow/static-template',
] as const
const WORKFLOW_READY_ROUTES = [
  shortvideoRoutes.workflowEditor,
  `${shortvideoRoutes.workflowEditor}?projectId=:id`,
].join('|')
const WORKFLOW_SUPPORTED_ACTIONS = [
  'load-workflow-template',
  'save-browser-workflow-draft',
  'edit-workflow-nodes',
  'remove-workflow-node',
  'append-workflow-node',
].join('|')

function parseTemplateSteps(raw: unknown): WorkflowNode[] {
  if (raw == null) return []
  let arr: unknown[] = []
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      arr = Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  } else if (Array.isArray(raw)) {
    arr = raw
  }
  return arr.map((step, i) => {
    const s = step as WorkflowTemplateStep
    const label = String(s.label ?? s.name ?? `步骤${i + 1}`)
    const typeRaw = String(s.type ?? 'script')
    const type = (Object.keys(NODE_TYPE_LABELS) as NodeType[]).includes(typeRaw as NodeType)
      ? (typeRaw as NodeType)
      : 'script'
    return {
      id: String(s.id ?? `tpl-${i}`),
      type,
      label,
      assignee: String(s.assignee ?? ''),
      durationDays: Number(s.durationDays ?? s.days ?? 1),
    }
  })
}

const DEFAULT_NODES: WorkflowNode[] = [
  { id: '1', type: 'script', label: '脚本策划', assignee: '', durationDays: 1 },
  { id: '2', type: 'shotList', label: '分镜设计', assignee: '', durationDays: 1 },
  { id: '3', type: 'material', label: '素材准备', assignee: '', durationDays: 1 },
  { id: '4', type: 'videoGen', label: '素材生产', assignee: '', durationDays: 2 },
  { id: '5', type: 'compose', label: '智能合成', assignee: '', durationDays: 1 },
  { id: '6', type: 'publish', label: '审核发布', assignee: '', durationDays: 1 },
]

function buildDraftKey(projectId: string | null) {
  return `sv-workflow-draft:${projectId || 'global'}`
}

function readDraft(projectId: string | null): WorkflowNode[] | null {
  try {
    const raw = window.localStorage.getItem(buildDraftKey(projectId))
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    return parseTemplateSteps(parsed)
  } catch {
    return null
  }
}

function writeDraft(projectId: string | null, nodes: WorkflowNode[]) {
  window.localStorage.setItem(buildDraftKey(projectId), JSON.stringify(nodes))
}

export default function WorkflowEditorPage() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId')
  const [nodes, setNodes] = useState<WorkflowNode[]>(DEFAULT_NODES)
  const [saving, setSaving] = useState(false)
  const [templateId, setTemplateId] = useState('')
  const [draftLoaded, setDraftLoaded] = useState(false)
  const [pageError, setPageError] = useState('')

  const { data: templates = [], isLoading: templateLoading, isError: templateError, refetch } = useQuery({
    queryKey: ['workflow-templates'],
    queryFn: () => shortvideoApi.workflowTemplateList(),
  })

  useEffect(() => {
    const draft = readDraft(projectId)
    if (draft && draft.length > 0) {
      setNodes(draft)
      setDraftLoaded(true)
    }
  }, [projectId])

  const templateOptions = useMemo(() => templates.map((t) => ({
    id: Number(t.id ?? 0),
    name: String(t.templateName ?? t.name ?? `模板${t.id}`),
    description: String(t.description ?? ''),
 })).filter(t => t.id > 0), [templates])

  const selectedTemplate = useMemo<WorkflowTemplate | undefined>(
    () => templates.find((t) => Number(t.id) === Number(templateId)),
    [templates, templateId],
  )
  const totalDays = nodes.reduce((sum, node) => sum + Math.max(0, Number(node.durationDays) || 0), 0)
  const typedCount = new Set(nodes.map((node) => node.type)).size

  const updateNode = (id: string, patch: Partial<WorkflowNode>) => {
    setNodes((prev) => prev.map((n) => (n.id === id ? { ...n, ...patch } : n)))
  }

  const addNode = () => {
    const newNode: WorkflowNode = {
      id: String(Date.now()),
      type: 'review',
      label: '新步骤',
      assignee: '',
      durationDays: 1,
    }
    setNodes((prev) => [...prev, newNode])
  }

  const removeNode = (id: string) => {
    setNodes((prev) => prev.filter((n) => n.id !== id))
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      writeDraft(projectId, nodes)
      setDraftLoaded(true)
      toast('已保存到浏览器草稿', 'success')
    } catch {
      toast('保存失败', 'error')
    } finally {
      setSaving(false)
    }
  }

  const applyTemplate = async () => {
    const id = Number(templateId)
    if (!id) { toast('请选择模板', 'warning'); return }
    setPageError('')
    try {
      const detail = await shortvideoApi.workflowTemplateGet(id)
      const parsed = parseTemplateSteps(detail.steps)
      if (parsed.length > 0) {
        setNodes(parsed)
        setDraftLoaded(false)
        toast('已应用模板步骤', 'success')
      } else {
        setPageError(`模板 steps 为空或不是有效 JSON 数组（POST ${WORKFLOW_TEMPLATE_ENDPOINTS.get}），已保留当前草稿。`)
        toast('该模板无可用步骤 JSON', 'info')
      }
    } catch (e) {
      const message = getErrorMessage(e)
      setPageError(`POST ${WORKFLOW_TEMPLATE_ENDPOINTS.get}：${message}`)
      toast(message, 'error')
    }
  }

  return (
    <Box
      data-testid="workflow-editor-page"
      data-ready-endpoints={WORKFLOW_READY_ENDPOINTS.join('|')}
      data-ready-routes={WORKFLOW_READY_ROUTES}
      data-supported-actions={WORKFLOW_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={WORKFLOW_UNSUPPORTED_ENDPOINTS.join('|')}
      data-local-draft-key={buildDraftKey(projectId)}
      data-explicit-browser-draft="true"
      data-no-db-workflow-save="true"
      data-no-static-template-fallback="true"
    >
      <PageHeader
        title="工作流编辑器"
        breadcrumbs={[{ label: '短视频' }, { label: '工作流编辑器' }]}
        subtitle={projectId ? `项目 #${projectId}` : '全局模板预览'}
        actions={
          <Button
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={handleSave}
            disabled={saving}
            data-testid="workflow-editor-save-draft-button"
          >
            保存浏览器草稿
          </Button>
        }
      />

      <Alert
        data-testid="workflow-editor-boundary-contract"
        data-explicit-browser-draft="true"
        data-no-db-workflow-save="true"
        data-supported-actions={WORKFLOW_SUPPORTED_ACTIONS}
        severity="warning"
        sx={{ mb: 2 }}
      >
        当前后端只提供工作流模板读取接口，没有项目级工作流保存/执行 Controller；本页保存的是浏览器草稿，不会写入数据库。
      </Alert>

      {!projectId && <Alert data-testid="workflow-editor-global-preview" severity="info" sx={{ mb: 2 }}>当前为全局工作流模板预览，不关联具体项目。</Alert>}
      {draftLoaded && <Alert data-testid="workflow-editor-draft-loaded" data-source="browser-local-draft" severity="success" sx={{ mb: 2 }}>已载入本机浏览器草稿。</Alert>}
      {templateError && (
        <Alert
          data-testid="workflow-editor-template-list-error"
          data-no-static-template-fallback="true"
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => refetch()}>重试</Button>}
          sx={{ mb: 2 }}
        >
          工作流模板加载失败（POST {WORKFLOW_TEMPLATE_ENDPOINTS.list}），仍可编辑本地草稿。
        </Alert>
      )}
      {pageError && (
        <Alert data-testid="workflow-editor-template-apply-error" data-input-retained="true" data-no-static-template-fallback="true" severity="error" sx={{ mb: 2 }}>
          工作流模板应用失败：{pageError}。当前不会写入数据库，已保留页面内草稿。
        </Alert>
      )}
      {templateLoading && <LinearProgress sx={{ mb: 2 }} />}

      <Grid container spacing={2} sx={{ mb: 2 }} data-testid="workflow-editor-summary" data-derived-from="page-draft-nodes">
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">步骤数</Typography>
              <Typography variant="h5" fontWeight={700}>{nodes.length}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">预计工期</Typography>
              <Typography variant="h5" fontWeight={700}>{totalDays} 天</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">步骤类型覆盖</Typography>
              <Typography variant="h5" fontWeight={700}>{typedCount}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card data-testid="workflow-editor-template-loader" data-source-endpoint={WORKFLOW_TEMPLATE_ENDPOINTS.list} variant="outlined" sx={{ mb: 2 }}>
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel id="workflow-template-label">后端工作流模板</InputLabel>
              <Select
                labelId="workflow-template-label"
                id="workflow-template-select"
                label="后端工作流模板"
                value={templateId}
                onChange={e => setTemplateId(e.target.value)}
              >
                <MenuItem value="">请选择</MenuItem>
                {templateOptions.map(t => (
                  <MenuItem key={t.id} value={String(t.id)}>{t.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button
              variant="outlined"
              onClick={applyTemplate}
              disabled={!templateId}
              data-testid="workflow-editor-apply-template-button"
              data-source-endpoint={WORKFLOW_TEMPLATE_ENDPOINTS.get}
            >
              载入步骤
            </Button>
            {selectedTemplate?.description && (
              <Typography variant="body2" color="text.secondary">{selectedTemplate.description}</Typography>
            )}
          </Stack>
        </CardContent>
      </Card>

      {/* 流程可视化 */}
      <Box data-testid="workflow-editor-flow-preview" data-derived-from="page-draft-nodes" sx={{ overflowX: 'auto', mb: 3, pb: 1 }}>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: nodes.length * 160 }}>
          {nodes.map((node, idx) => (
            <Stack key={node.id} direction="row" alignItems="center" spacing={1}>
              <Chip
                label={node.label}
                color="primary"
                variant={idx === 0 ? 'filled' : 'outlined'}
                size="medium"
              />
              {idx < nodes.length - 1 && <NextIcon color="disabled" fontSize="small" />}
            </Stack>
          ))}
        </Stack>
      </Box>

      {/* 节点编辑 */}
      <Grid container spacing={2} data-testid="workflow-editor-node-editor" data-explicit-browser-draft="true">
        {nodes.map((node) => (
          <Grid item xs={12} sm={6} md={4} key={node.id}>
            <Card variant="outlined">
              <CardContent>
                <Stack spacing={2}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="subtitle2" fontWeight={600}>{node.label}</Typography>
                    <Button
                      size="small" color="error"
                      startIcon={<DeleteIcon />}
                      onClick={() => removeNode(node.id)}
                      disabled={nodes.length <= 2}
                      data-testid="workflow-editor-remove-node-button"
                    >
                      移除
                    </Button>
                  </Stack>
                  <TextField
                    size="small" label="步骤名称"
                    value={node.label}
                    onChange={(e) => updateNode(node.id, { label: e.target.value })}
                  />
                  <FormControl size="small">
                    <InputLabel>步骤类型</InputLabel>
                    <Select
                      value={node.type}
                      label="步骤类型"
                      onChange={(e) => updateNode(node.id, { type: e.target.value as NodeType })}
                    >
                      {Object.entries(NODE_TYPE_LABELS).map(([k, v]) => (
                        <MenuItem key={k} value={k}>{v}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField
                    size="small" label="负责人"
                    value={node.assignee}
                    onChange={(e) => updateNode(node.id, { assignee: e.target.value })}
                    placeholder="留空表示不指定"
                  />
                  <TextField
                    size="small" label="预计天数" type="number"
                    value={node.durationDays}
                    onChange={(e) => updateNode(node.id, { durationDays: Number(e.target.value) })}
                    inputProps={{ min: 1, max: 30 }}
                  />
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
        <Grid item xs={12} sm={6} md={4}>
          <Card
            data-testid="workflow-editor-add-node-card"
            variant="outlined"
            sx={{ height: '100%', minHeight: 120, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', '&:hover': { borderColor: 'primary.main' } }}
            onClick={addNode}
          >
            <Stack alignItems="center" spacing={1}>
              <AddIcon color="primary" />
              <Typography variant="body2" color="primary">添加步骤</Typography>
            </Stack>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
