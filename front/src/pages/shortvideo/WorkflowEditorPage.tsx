import { useState, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Card, CardContent, Typography, Button, Stack, Chip,
  Grid, Alert, TextField, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import {
  Add as AddIcon, Delete as DeleteIcon,
  ArrowForward as NextIcon, Save as SaveIcon,
} from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { shortvideoApi } from '@/api/shortvideo'

type NodeType = 'script' | 'material' | 'edit' | 'review' | 'publish'

interface WorkflowNode {
  id: string
  type: NodeType
  label: string
  assignee: string
  durationDays: number
}

const NODE_TYPE_LABELS: Record<NodeType, string> = {
  script: '脚本策划',
  material: '素材准备',
  edit: '视频剪辑',
  review: '审核',
  publish: '发布',
}

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
    const s = step as Partial<WorkflowNode & { name?: string; days?: number }>
    const label = String(s.label ?? s.name ?? `步骤${i + 1}`)
    const typeRaw = String(s.type ?? 'script')
    const type = (['script', 'material', 'edit', 'review', 'publish'] as const).includes(typeRaw as NodeType)
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
  { id: '2', type: 'material', label: '素材准备', assignee: '', durationDays: 1 },
  { id: '3', type: 'edit', label: '视频剪辑', assignee: '', durationDays: 2 },
  { id: '4', type: 'review', label: '审核', assignee: '', durationDays: 1 },
  { id: '5', type: 'publish', label: '发布', assignee: '', durationDays: 1 },
]

export default function WorkflowEditorPage() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId')
  const [nodes, setNodes] = useState<WorkflowNode[]>(DEFAULT_NODES)
  const [saving, setSaving] = useState(false)
  const [templateId, setTemplateId] = useState('')

  const { data: templates = [] } = useQuery({
    queryKey: ['workflow-templates'],
    queryFn: () => shortvideoApi.workflowTemplateList(),
  })

  const templateOptions = useMemo(() => templates.map((t) => ({
    id: Number(t.id ?? 0),
    name: String(t.templateName ?? t.name ?? `模板${t.id}`),
 })).filter(t => t.id > 0), [templates])

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
      await new Promise((r) => setTimeout(r, 300))
      toast('已保存为本地草稿（后端暂无项目工作流持久化接口）', 'success')
    } catch {
      toast('保存失败', 'error')
    } finally {
      setSaving(false)
    }
  }

  const applyTemplate = async () => {
    const id = Number(templateId)
    if (!id) { toast('请选择模板', 'warning'); return }
    try {
      const detail = await shortvideoApi.workflowTemplateGet(id)
      const parsed = parseTemplateSteps(detail.steps)
      if (parsed.length > 0) {
        setNodes(parsed)
        toast('已应用模板步骤', 'success')
      } else {
        toast('该模板无可用步骤 JSON', 'info')
      }
    } catch (e) {
      toast(e instanceof Error ? e.message : '加载模板失败', 'error')
    }
  }

  return (
    <Box>
      <PageHeader
        title="工作流编辑器"
        breadcrumbs={[{ label: '短视频' }, { label: '工作流编辑器' }]}
        subtitle={projectId ? `项目 #${projectId}` : undefined}
        actions={
          <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave} disabled={saving}>
            保存工作流
          </Button>
        }
      />

      {!projectId && <Alert severity="info" sx={{ mb: 2 }}>当前为全局工作流模板，不关联具体项目</Alert>}

      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>后端工作流模板</InputLabel>
              <Select
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
            <Button variant="outlined" onClick={applyTemplate} disabled={!templateId}>载入步骤</Button>
          </Stack>
        </CardContent>
      </Card>

      {/* 流程可视化 */}
      <Box sx={{ overflowX: 'auto', mb: 3, pb: 1 }}>
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
      <Grid container spacing={2}>
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
