import { useState, useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box, Button, Stack, TextField, Typography, Paper, Card, CardContent,
  Grid, Chip, IconButton, Collapse, Divider, Alert,
  FormControl, InputLabel, Select, MenuItem, FormGroup, FormControlLabel,
  Checkbox, Tabs, Tab,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import SaveIcon from '@mui/icons-material/Save'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { PageHeader } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import { workflowApi, agentApi, type Agent, type WorkflowSave } from '@/api/agent'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getErrorMessage } from '@/utils/errorHandler'
import { normalizeArray, normalizeStringArray } from '@/utils/response-normalize'

interface StepForm {
  id?: number
  stepOrder: number
  agentId: number
  agentName?: string
  stepName?: string
  inputTemplate?: string
  outputKey?: string
  skipCondition?: string
  dependsOn: string[]
  executionMode: number
  retryCount: number
  timeoutSeconds?: number
}

const EXEC_MODE_OPTIONS = [
  { value: 0, label: '顺序执行' },
  { value: 1, label: '并行执行' },
]

const SKIP_CONDITION_OPTIONS = [
  { value: '', label: '不跳过' },
  { value: 'always:', label: 'always:（始终跳过）' },
  { value: 'contains:', label: 'contains:（包含关键词时跳过）' },
  { value: 'not-contains:', label: 'not-contains:（不包含关键词时跳过）' },
  { value: 'has-result:', label: 'has-result:（上一步无结果时跳过）' },
]

const WORKFLOW_EDITOR_READY_ENDPOINTS = [
  '/agent/list',
  '/agent/workflow/get',
  '/agent/workflow/save',
  '/agent/workflow/execute',
].join('|')

const WORKFLOW_EDITOR_UNSUPPORTED_ENDPOINTS = [
  '/agent/workflow/mock',
  '/agent/workflow/local-save',
  '/agent/workflow/local-execute',
  '/agent/workflow/execution/local-create',
  '/agent/workflow/execution/get',
  '/agent/workflow/export',
  '/agent/workflow/import-local',
].join('|')

function StepCard({
  step, allSteps, agents, onChange, onRemove,
}: {
  step: StepForm
  allSteps: StepForm[]
  agents: Agent[]
  onChange: (updated: StepForm) => void
  onRemove: () => void
}) {
  const [expanded, setExpanded] = useState(false)

  const availableDeps = useMemo(() => {
    return allSteps.filter(s => s.stepOrder < step.stepOrder && s.outputKey)
  }, [allSteps, step.stepOrder])

  const skipPrefix = (step.skipCondition?.split(':')[0] ?? '') + ':'

  const update = (patch: Partial<StepForm>) => onChange({ ...step, ...patch })

  return (
    <Card
      variant="outlined"
      sx={{ mb: 1.5 }}
      data-testid="agent-workflow-step-card"
      data-step-order={String(step.stepOrder)}
      data-no-local-step-persistence="true"
    >
      <CardContent sx={{ pb: 1, '&:last-child': { pb: 1 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Chip label={`步骤 ${step.stepOrder}`} size="small" color="primary" />
          <TextField size="small" placeholder="步骤名称（如：商品分析）" value={step.stepName ?? ''}
            onChange={e => update({ stepName: e.target.value })} sx={{ flex: 1 }} />
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel>智能体</InputLabel>
            <Select label="智能体" value={step.agentId ?? ''}
              onChange={e => {
                const ag = agents.find(a => a.id === Number(e.target.value))
                update({ agentId: Number(e.target.value), agentName: ag?.agentName })
              }}>
              {agents.map(a => (
                <MenuItem key={a.id} value={a.id}>{a.agentName}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <IconButton size="small" onClick={() => setExpanded(v => !v)}>
            {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
          <IconButton size="small" color="error" onClick={onRemove}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>

        <Stack direction="row" spacing={0.5} sx={{ mt: 1 }} flexWrap="wrap" useFlexGap>
          {step.outputKey && <Chip label={`输出: ${step.outputKey}`} size="small" color="success" variant="outlined" />}
          {step.executionMode === 1 && <Chip label="并行" size="small" color="info" />}
          {step.retryCount > 1 && <Chip label={`重试×${step.retryCount}`} size="small" color="warning" variant="outlined" />}
          {step.timeoutSeconds && <Chip label={`超时:${step.timeoutSeconds}s`} size="small" variant="outlined" />}
          {step.dependsOn.length > 0 && (
            <Chip label={`依赖: ${step.dependsOn.join(', ')}`} size="small" variant="outlined" />
          )}
        </Stack>

        <Collapse in={expanded}>
          <Divider sx={{ my: 1.5 }} />
          <Grid container spacing={1.5}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="输出键名" placeholder="如: product_analysis"
                value={step.outputKey ?? ''}
                onChange={e => update({ outputKey: e.target.value })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth size="small">
                <InputLabel>跳过条件</InputLabel>
                <Select label="跳过条件" value={skipPrefix}
                  onChange={e => {
                    const prefix = e.target.value as string
                    const rest = step.skipCondition?.slice(prefix.length) ?? ''
                    update({ skipCondition: prefix + rest })
                  }}>
                  {SKIP_CONDITION_OPTIONS.map(o => (
                    <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            {skipPrefix && skipPrefix !== 'always:' && skipPrefix !== 'has-result:' && (
              <Grid item xs={12}>
                <TextField fullWidth size="small" label="跳过条件参数"
                  value={step.skipCondition?.slice(skipPrefix.length) ?? ''}
                  onChange={e => update({ skipCondition: skipPrefix + e.target.value })}
                  placeholder="如: 无数据 / 不合规" />
              </Grid>
            )}
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="超时秒数" type="number"
                value={step.timeoutSeconds ?? 300}
                onChange={e => update({ timeoutSeconds: parseInt(e.target.value) || 300 })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth size="small">
                <InputLabel>执行模式</InputLabel>
                <Select label="执行模式" value={step.executionMode}
                  onChange={e => update({ executionMode: Number(e.target.value) })}>
                  {EXEC_MODE_OPTIONS.map(o => (
                    <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth size="small" label="重试次数" type="number"
                value={step.retryCount}
                onChange={e => update({ retryCount: Math.max(1, parseInt(e.target.value) || 1) })} />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth size="small" label="输入模板" multiline rows={2}
                value={step.inputTemplate ?? ''}
                onChange={e => update({ inputTemplate: e.target.value })}
                placeholder="如: 分析商品 ${input} 的销售数据" />
            </Grid>
            {availableDeps.length > 0 && (
              <Grid item xs={12}>
                <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
                  依赖前置步骤（多选）
                </Typography>
                <FormGroup row>
                  {availableDeps.map(s => (
                    <FormControlLabel key={s.outputKey}
                      control={
                        <Checkbox size="small"
                          checked={step.dependsOn.includes(s.outputKey!)}
                          onChange={(_, checked) => {
                            if (checked) {
                              update({ dependsOn: [...step.dependsOn, s.outputKey!] })
                            } else {
                              update({ dependsOn: step.dependsOn.filter(k => k !== s.outputKey) })
                            }
                          }}
                        />
                      }
                      label={s.outputKey}
                    />
                  ))}
                </FormGroup>
              </Grid>
            )}
          </Grid>
        </Collapse>
      </CardContent>
    </Card>
  )
}

function DagPreview({ steps }: { steps: StepForm[] }) {
  if (steps.length === 0) {
    return (
      <Paper
        variant="outlined"
        sx={{ p: 2 }}
        data-testid="agent-workflow-dag-preview"
        data-form-only-preview="true"
        data-no-server-persistence="true"
      >
        <Typography variant="body2" color="text.secondary">添加步骤后可在 DAG 预览中查看执行层级</Typography>
      </Paper>
    )
  }

  const layers: StepForm[][] = []
  const placed = new Set<number>()
  const remaining = [...steps]

  while (remaining.length > 0) {
    const layer: StepForm[] = []
    for (const step of remaining) {
      const deps = step.dependsOn ?? []
      const allPlaced = deps.length === 0 || deps.every(d =>
        steps.some(s => s.outputKey === d && placed.has(s.stepOrder))
      )
      if (allPlaced) layer.push(step)
    }
    if (layer.length === 0) break
    layer.forEach(s => {
      placed.add(s.stepOrder)
      const idx = remaining.indexOf(s)
      if (idx >= 0) remaining.splice(idx, 1)
    })
    layers.push(layer)
  }

  return (
    <Paper
      variant="outlined"
      sx={{ p: 2 }}
      data-testid="agent-workflow-dag-preview"
      data-form-only-preview="true"
      data-no-server-persistence="true"
    >
      <Typography variant="subtitle2" sx={{ mb: 1.5 }}>DAG 执行预览</Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {layers.map((layer, li) => (
          <Box key={li}>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
              <Chip label={`层 ${li + 1}`} size="small" color={layer.length > 1 ? 'info' : 'default'} />
              {layer.map(step => (
                <Chip key={step.stepOrder} label={step.stepName || `Step ${step.stepOrder}`}
                  size="small" variant="outlined" />
              ))}
              {layer.length > 1 && (
                <Chip label="并行执行" size="small" color="info" />
              )}
            </Stack>
            {li < layers.length - 1 && (
              <Box sx={{ mt: 0.5, ml: 1, borderLeft: '2px solid', borderColor: 'divider', height: 8 }} />
            )}
          </Box>
        ))}
      </Box>
    </Paper>
  )
}

export default function AgentWorkflowEditorPage() {
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const isEditing = !!id && id !== 'new'
  const workflowId = isEditing ? parseInt(id) : undefined

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState(1)
  const [steps, setSteps] = useState<StepForm[]>([])
  const [activeTab, setActiveTab] = useState(0)
  const [userInput, setUserInput] = useState('')
  const [actionError, setActionError] = useState('')

  // Load agents for dropdown
  const { data: rawAgents, isError: agentsError, error: agentsLoadError, refetch: refetchAgents } = useQuery({
    queryKey: ['agents', 'all'],
    queryFn: () => agentApi.list({ rows: 1000 }),
  })
  const agents = normalizeArray<Agent>(rawAgents)

  // Load workflow if editing
  const { data: workflow, isError: workflowError, error: workflowLoadError, refetch: refetchWorkflow } = useQuery({
    queryKey: ['workflow', 'detail', workflowId],
    queryFn: () => workflowId ? workflowApi.get(workflowId) : Promise.resolve(null),
    enabled: !!workflowId,
  })

  useEffect(() => {
    if (workflow) {
      setName(workflow.name)
      setDescription(workflow.description ?? '')
      setStatus(workflow.status)
      setSteps(normalizeArray<typeof workflow.steps[number]>(workflow.steps).map((s, index) => ({
        id: s.id,
        stepOrder: s.stepOrder ?? index + 1,
        agentId: s.agentId,
        agentName: s.agentName,
        stepName: s.stepName ?? '',
        inputTemplate: s.inputTemplate ?? '',
        outputKey: s.outputKey ?? '',
        skipCondition: s.skipCondition ?? '',
        dependsOn: normalizeStringArray(s.dependsOn),
        executionMode: s.executionMode ?? 0,
        retryCount: s.retryCount ?? 1,
        timeoutSeconds: s.timeoutSeconds ?? 300,
      })))
    }
  }, [workflow])

  const saveMutation = useMutation({
    mutationFn: () => {
      const params: WorkflowSave = {
        id: workflowId,
        name,
        description,
        status,
        steps: steps.map((s, idx) => ({
          stepOrder: idx + 1,
          agentId: s.agentId,
          stepName: s.stepName || undefined,
          inputTemplate: s.inputTemplate || undefined,
          outputKey: s.outputKey || undefined,
          skipCondition: s.skipCondition || undefined,
          dependsOn: s.dependsOn,
          executionMode: s.executionMode,
          retryCount: s.retryCount,
          timeoutSeconds: s.timeoutSeconds,
        })),
      }
      return workflowApi.save(params)
    },
    onSuccess: (newId) => {
      toast('保存成功', 'success')
      setActionError('')
      if (!isEditing && newId) {
        navigate(`/admin/ai/agent/workflow/edit/${newId}`, { replace: true })
      }
    },
    onError: (error) => {
      setActionError(`保存工作流失败（POST /agent/workflow/save）：${getErrorMessage(error)}。页面已保留工作流名称、状态、描述和全部步骤配置。`)
      toast('保存失败', 'error')
    },
  })

  const executeMutation = useMutation({
    mutationFn: () => {
      if (!workflowId) return Promise.reject(new Error('No workflow id'))
      return workflowApi.execute(workflowId, userInput)
    },
    onSuccess: () => {
      setActionError('')
      toast('工作流已启动', 'success')
    },
    onError: (error) => {
      setActionError(`执行工作流失败（POST /agent/workflow/execute）：${getErrorMessage(error)}。页面不会伪造执行记录，已保留快速执行输入和当前步骤配置。`)
      toast('启动失败', 'error')
    },
  })

  const addStep = () => {
    const newStep: StepForm = {
      stepOrder: steps.length + 1,
      agentId: agents[0]?.id ?? 0,
      agentName: agents[0]?.agentName,
      stepName: '',
      inputTemplate: '',
      outputKey: '',
      skipCondition: '',
      dependsOn: [],
      executionMode: 0,
      retryCount: 1,
      timeoutSeconds: 300,
    }
    setSteps(prev => [...prev, newStep])
  }

  const updateStep = (index: number, updated: StepForm) => {
    setSteps(prev => prev.map((s, i) => i === index ? updated : s))
  }

  const removeStep = (index: number) => {
    setSteps(prev => {
      const next = prev.filter((_, i) => i !== index)
      return next.map((s, i) => ({ ...s, stepOrder: i + 1 }))
    })
  }

  const canSave = name.trim().length > 0 && steps.length > 0

  return (
    <Box
      data-testid="agent-workflow-editor-page"
      data-ready-endpoints={WORKFLOW_EDITOR_READY_ENDPOINTS}
      data-unsupported-endpoints={WORKFLOW_EDITOR_UNSUPPORTED_ENDPOINTS}
      data-no-local-workflow-mutation="true"
      data-no-local-execution-record="true"
    >
      <PageHeader
        title={isEditing ? `编辑工作流: ${name}` : '新建工作流'}
        actions={
          <Stack direction="row" spacing={1}>
            {isEditing && (
              <Button variant="outlined" startIcon={<PlayArrowIcon />}
                onClick={() => executeMutation.mutate()}
                disabled={executeMutation.isPending}>
                执行
              </Button>
            )}
            <Button variant="contained" startIcon={<SaveIcon />}
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending || !canSave}>
              保存
            </Button>
          </Stack>
        }
      />

      <Alert
        severity="info"
        sx={{ mb: 2 }}
        data-testid="agent-workflow-editor-boundary-contract"
        data-source-endpoints={WORKFLOW_EDITOR_READY_ENDPOINTS}
        data-no-local-workflow-mutation="true"
        data-no-local-execution-record="true"
      >
        编辑页只读取 <code>/agent/list</code> 与 <code>/agent/workflow/get</code>，保存和快速执行分别提交到 <code>/agent/workflow/save</code>、<code>/agent/workflow/execute</code>。
        DAG 预览只基于当前表单配置，不代表已经持久化或产生执行记录。
      </Alert>

      {agentsError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="agent-workflow-editor-agents-error"
          data-source-endpoint="/agent/list"
          data-no-local-agent-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetchAgents()}>重试</Button>}
        >
          智能体列表加载失败（POST /agent/list）：{getErrorMessage(agentsLoadError)}。无法选择工作流步骤执行者，已配置步骤不会被清空。
        </Alert>
      )}

      {workflowError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="agent-workflow-editor-detail-error"
          data-source-endpoint="/agent/workflow/get"
          data-no-local-workflow-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => refetchWorkflow()}>重试</Button>}
        >
          工作流详情加载失败（POST /agent/workflow/get）：{getErrorMessage(workflowLoadError)}。请确认工作流是否存在或当前账号是否有权限。
        </Alert>
      )}

      {actionError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          data-testid="agent-workflow-editor-action-error"
          data-input-retained="true"
          data-no-local-workflow-mutation="true"
          data-no-local-execution-record="true"
          onClose={() => setActionError('')}
        >
          {actionError}
        </Alert>
      )}

      {isEditing && (
        <Paper
          variant="outlined"
          sx={{ p: 2, mb: 2 }}
          data-testid="agent-workflow-quick-execute-contract"
          data-source-endpoint="/agent/workflow/execute"
          data-input-retained="true"
          data-no-local-execution-record="true"
        >
          <Typography variant="subtitle2" sx={{ mb: 1 }}>快速执行</Typography>
          <Stack direction="row" spacing={1}>
            <TextField fullWidth size="small" placeholder="输入内容，敲击回车执行"
              value={userInput}
              onChange={e => setUserInput(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Enter' && userInput.trim()) {
                  executeMutation.mutate()
                }
              }} />
            <Button variant="contained" startIcon={<PlayArrowIcon />}
              onClick={() => executeMutation.mutate()}
              disabled={executeMutation.isPending || !userInput.trim()}>
              执行
            </Button>
          </Stack>
        </Paper>
      )}

      <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} sx={{ mb: 2 }}>
        <Tab label="配置" />
        <Tab label="DAG 预览" />
      </Tabs>

      {activeTab === 0 && (
        <Box>
          <Paper
            variant="outlined"
            sx={{ p: 2, mb: 2 }}
            data-testid="agent-workflow-editor-form-contract"
            data-source-endpoint="/agent/workflow/save"
            data-input-retained="true"
            data-no-local-workflow-mutation="true"
          >
            <Grid container spacing={2}>
              <Grid item xs={12} sm={8}>
                <TextField fullWidth size="small" label="工作流名称" value={name}
                  onChange={e => setName(e.target.value)} required />
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth size="small">
                  <InputLabel>状态</InputLabel>
                  <Select label="状态" value={status}
                    onChange={e => setStatus(Number(e.target.value))}>
                    <MenuItem value={0}>草稿</MenuItem>
                    <MenuItem value={1}>已启用</MenuItem>
                    <MenuItem value={9}>已禁用</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth size="small" label="描述" value={description}
                  onChange={e => setDescription(e.target.value)} multiline rows={2} />
              </Grid>
            </Grid>
          </Paper>

          <Box
            sx={{ mb: 2 }}
            data-testid="agent-workflow-steps-contract"
            data-source-endpoint="/agent/workflow/save"
            data-no-local-step-persistence="true"
          >
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography variant="subtitle1" fontWeight={600}>步骤配置</Typography>
              <Button size="small" startIcon={<AddIcon />} onClick={addStep}
                disabled={agents.length === 0}>
                添加步骤
              </Button>
            </Box>

            {agents.length === 0 && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                请先在「智能体列表」创建至少一个智能体
              </Alert>
            )}

            {steps.length === 0 && agents.length > 0 && (
              <Alert severity="info" sx={{ mb: 2 }}>
                点击「添加步骤」开始配置工作流
              </Alert>
            )}

            {steps.map((step, idx) => (
              <StepCard key={idx} step={step} allSteps={steps} agents={agents}
                onChange={updated => updateStep(idx, updated)}
                onRemove={() => removeStep(idx)} />
            ))}
          </Box>
        </Box>
      )}

      {activeTab === 1 && <DagPreview steps={steps} />}
    </Box>
  )
}
