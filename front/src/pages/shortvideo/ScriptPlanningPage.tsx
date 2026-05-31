import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import DeleteIcon from '@mui/icons-material/Delete'
import SaveIcon from '@mui/icons-material/Save'
import ViewColumnIcon from '@mui/icons-material/ViewColumn'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ConfirmDialog, PageHeader, StandardDataGrid } from '@/components/base'
import { shortvideoApi, type SvProject, type SvProjectSave } from '@/api/shortvideo'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { useGaifanEntitlementGate } from '@/hooks/useGaifanEntitlementGate'
import { getErrorMessage } from '@/utils/errorHandler'
import type { GridColDef } from '@mui/x-data-grid'
import type { SvScript } from '@/types/shortvideo'

const TYPE_OPTIONS = [
  { value: 'daily', label: '日常内容' },
  { value: 'viral_clone', label: '爆款复刻' },
  { value: 'soft_ad', label: '软广植入' },
  { value: 'digital_human_commerce', label: '数字人口播带货' },
]
const STYLE_OPTIONS = [
  { value: 'professional', label: '专业干货' },
  { value: 'digital_human_product_detail', label: '数字人口播 + 产品细节展示' },
  { value: 'seeding_review', label: '种草测评' },
  { value: 'opening_review', label: '开箱测评' },
  { value: 'high_conversion', label: '强转化带货' },
  { value: 'warm', label: '温和信任' },
] as const
const SCRIPT_PLANNING_ENDPOINTS = {
  projectGet: '/short-video/project/get',
  projectSave: '/short-video/project/save',
  scriptGet: '/short-video/script/get',
  scriptList: '/short-video/script/list',
  scriptGenerate: '/short-video/script/generate',
  scriptSave: '/short-video/script/save',
  scriptDelete: '/short-video/script/delete',
  shotListGenerate: '/short-video/shot-list/generate',
} as const
const SCRIPT_PLANNING_READY_ENDPOINTS = Object.values(SCRIPT_PLANNING_ENDPOINTS).join('|')
const SCRIPT_PLANNING_UNSUPPORTED_ENDPOINTS = [
  '/short-video/script/mock',
  '/short-video/script/local-generate',
  '/short-video/script/local-save',
  '/short-video/script/local-delete',
  '/short-video/project/local-save',
  '/short-video/shot-list/local-generate',
  '/short-video/shot-list/local-save',
  '/short-video/remake-template/local-apply',
].join('|')
const SCRIPT_PLANNING_READY_ROUTES = [
  shortvideoRoutes.scriptPlanning,
  `${shortvideoRoutes.scriptPlanning}?projectId=:id`,
  `${shortvideoRoutes.shotList}?projectId=:id`,
  `${shortvideoRoutes.workbench}?projectId=:id`,
].join('|')
const SCRIPT_PLANNING_SUPPORTED_ACTIONS = [
  'generate-script',
  'save-script-and-bind-project',
  'generate-shot-list',
  'navigate-shot-list',
  'switch-script-planning-mode',
  'load-script-list',
  'delete-script',
].join('|')

function mergeProjectSave(project: SvProject, patch: Partial<SvProjectSave>): SvProjectSave {
  return {
    id: project.id,
    accountId: project.accountId,
    title: project.title,
    projectType: project.projectType,
    personaId: project.personaId,
    scheduleDate: project.scheduleDate,
    shootStatus: project.shootStatus,
    status: project.status,
    scriptId: project.scriptId,
    shotListId: project.shotListId,
    finalVideoUrl: project.finalVideoUrl,
    thumbnailUrl: project.thumbnailUrl,
    characterReferenceUrl: project.characterReferenceUrl,
    sceneReferenceUrl: project.sceneReferenceUrl,
    duration: project.duration,
    publishTitle: project.publishTitle,
    publishPlatforms: project.publishPlatforms,
    publishTime: project.publishTime,
    reviewStatus: project.reviewStatus,
    relatedProductIds: project.relatedProductIds,
    ...patch,
  }
}

export default function ScriptPlanningPage() {
  const toast = useToast()
  const gate = useGaifanEntitlementGate()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId') ? Number(searchParams.get('projectId')) : undefined
  const [tab, setTab] = useState(0)
  const [type, setType] = useState('daily')
  const [theme, setTheme] = useState('')
  const [style, setStyle] = useState('professional')
  const [duration, setDuration] = useState(60)
  const [viralVideoId, setViralVideoId] = useState('')
  const [productInfo, setProductInfo] = useState('')
  const [loading, setLoading] = useState(false)
  const [shotLoading, setShotLoading] = useState(false)
  const [generated, setGenerated] = useState('')
  const [savedScriptId, setSavedScriptId] = useState<number | null>(null)
  const [deleteScriptId, setDeleteScriptId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [operationError, setOperationError] = useState('')

  const {
    data: project,
    isError: projectIsError,
    error: projectError,
    refetch: refetchProject,
  } = useQuery({
    queryKey: ['sv-project', projectId],
    queryFn: () => shortvideoApi.get(projectId!),
    enabled: !!projectId,
  })

  const projectScriptId = project?.scriptId != null && project.scriptId > 0 ? project.scriptId : undefined
  const activeScriptId = savedScriptId ?? projectScriptId

  const { data: projectScript } = useQuery({
    queryKey: ['sv-script', projectScriptId],
    queryFn: () => shortvideoApi.svScriptGet(projectScriptId!),
    enabled: !!projectScriptId,
  })

  const {
    data: scripts = [],
    isLoading,
    isError: scriptsIsError,
    error: scriptsError,
    refetch: refetchScripts,
  } = useQuery({
    queryKey: ['sv-scripts', activeScriptId],
    queryFn: async () => {
      if (activeScriptId) {
        const script = await shortvideoApi.svScriptGet(activeScriptId)
        return script ? [script] : []
      }
      return shortvideoApi.svScriptList({ page: 0, rows: 50 }).then((r) => r.list ?? [])
    },
    enabled: tab === 1,
  })

  const currentScriptContent = useMemo(() => {
    if (generated.trim()) return generated.trim()
    const content = projectScript?.content
    return typeof content === 'string' ? content.trim() : ''
  }, [generated, projectScript])

  const wordCount = useMemo(() => currentScriptContent.replace(/\s/g, '').length, [currentScriptContent])
  const canGenerate = type === 'viral_clone' ? !!viralVideoId.trim() : !!theme.trim()
  const canSave = !!currentScriptContent
  const canGenerateShots = !!activeScriptId && !!currentScriptContent
  const projectHasScript = !!projectScriptId || !!savedScriptId
  const projectHasShotList = !!project?.shotListId

  const diagnostics = [
    {
      label: '项目关联',
      value: projectId ? `#${projectId}` : '未关联',
      color: projectId ? 'success' : 'warning',
    },
    {
      label: '脚本',
      value: projectHasScript ? `#${activeScriptId}` : '未保存',
      color: projectHasScript ? 'success' : canSave ? 'warning' : 'default',
    },
    {
      label: '分镜',
      value: projectHasShotList ? `#${project?.shotListId}` : '未生成',
      color: projectHasShotList ? 'success' : canGenerateShots ? 'warning' : 'default',
    },
    {
      label: '字数',
      value: wordCount ? `${wordCount}` : '0',
      color: wordCount >= 80 ? 'success' : wordCount > 0 ? 'warning' : 'default',
    },
  ] as const

  const handleGenerate = async () => {
    if (!theme.trim() && type !== 'viral_clone') {
      toast('请输入视频主题', 'warning')
      return
    }
    if (type === 'viral_clone' && !viralVideoId.trim()) {
      toast('请输入爆款视频 ID', 'warning')
      return
    }
    if (!(await gate('shortvideo-maker', 'shortvideo-maker.script.generate'))) return
    setLoading(true)
    setOperationError('')
    try {
      const content = await shortvideoApi.svScriptGenerate({
        type,
        theme: theme.trim(),
        viralVideoId: viralVideoId.trim() ? Number(viralVideoId.trim()) : undefined,
        productInfo: productInfo.trim() || undefined,
        style,
        duration,
      })
      setGenerated(content)
      setSavedScriptId(null)
      toast('脚本已生成', 'success')
    } catch (e) {
      const message = getErrorMessage(e)
      setOperationError(`脚本生成失败：${message}。接口来源：${SCRIPT_PLANNING_ENDPOINTS.scriptGenerate}。`)
      toast(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleTypeChange = (value: string) => {
    setType(value)
    if (value === 'digital_human_commerce' && style === 'professional') {
      setStyle('digital_human_product_detail')
    }
  }

  const handleSave = async () => {
    if (!currentScriptContent) {
      toast('没有可保存的脚本内容', 'warning')
      return
    }
    setOperationError('')
    try {
      const scriptId = await shortvideoApi.svScriptSave({
        id: activeScriptId,
        title: theme.trim() || project?.title || `短视频脚本 ${new Date().toLocaleDateString()}`,
        content: currentScriptContent,
        scriptType: type,
        generationType: generated ? 'ai' : 'manual',
        referenceViralId: viralVideoId.trim() ? Number(viralVideoId.trim()) : undefined,
        theme: theme.trim() || project?.title,
        style,
        duration,
        wordCount,
      })
      setSavedScriptId(Number(scriptId))
      if (projectId && project) {
        await shortvideoApi.save(mergeProjectSave(project, {
          scriptId: Number(scriptId),
          status: project.status || 'processing',
        }))
        await qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
      }
      await qc.invalidateQueries({ queryKey: ['sv-scripts'] })
      await qc.invalidateQueries({ queryKey: ['sv-script'] })
      await qc.invalidateQueries({ queryKey: ['sv-script', scriptId] })
      setTab(1)
      toast('脚本已保存并关联项目', 'success')
    } catch (e) {
      const message = getErrorMessage(e)
      setOperationError(`脚本保存或项目关联失败：${message}。接口来源：${SCRIPT_PLANNING_ENDPOINTS.scriptSave} 或 ${SCRIPT_PLANNING_ENDPOINTS.projectSave}。失败时不会本地伪造项目关联。`)
      toast(message, 'error')
    }
  }

  const handleGenerateShots = async () => {
    if (!activeScriptId) {
      toast('请先保存脚本', 'warning')
      return
    }
    if (!currentScriptContent) {
      toast('脚本内容为空，无法生成分镜', 'warning')
      return
    }
    if (!(await gate('shortvideo-maker', 'shortvideo-maker.script.generate'))) return
    setShotLoading(true)
    setOperationError('')
    try {
      const result = await shortvideoApi.shotListGenerate({
        scriptId: activeScriptId,
        scriptContent: currentScriptContent,
        shotCount: Math.max(3, Math.min(12, Math.ceil(duration / 8))),
        style,
      })
      if (projectId && project && result.shotListId) {
        await shortvideoApi.save(mergeProjectSave(project, {
          scriptId: activeScriptId,
          shotListId: result.shotListId,
          status: 'processing',
        }))
        await qc.invalidateQueries({ queryKey: ['sv-project', projectId] })
      }
      toast(`已生成 ${result.shots?.length ?? 0} 条分镜`, 'success')
      if (projectId) navigate(`${shortvideoRoutes.shotList}?projectId=${projectId}`)
    } catch (e) {
      const message = getErrorMessage(e)
      setOperationError(`分镜生成或项目回写失败：${message}。接口来源：${SCRIPT_PLANNING_ENDPOINTS.shotListGenerate} 或 ${SCRIPT_PLANNING_ENDPOINTS.projectSave}。失败时保留当前脚本内容。`)
      toast(message, 'error')
    } finally {
      setShotLoading(false)
    }
  }

  const handleConfirmDelete = async () => {
    if (!deleteScriptId) return
    setDeleting(true)
    try {
      await shortvideoApi.svScriptDelete(deleteScriptId)
      toast('已删除', 'success')
      await qc.invalidateQueries({ queryKey: ['sv-scripts'] })
    } catch (e) {
      const message = getErrorMessage(e)
      setOperationError(`脚本删除失败：${message}。接口来源：${SCRIPT_PLANNING_ENDPOINTS.scriptDelete}。失败时不会从列表中移除脚本。`)
      toast(message, 'error')
    } finally {
      setDeleting(false)
      setDeleteScriptId(null)
    }
  }

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'title', headerName: '标题', flex: 1 },
    { field: 'scriptType', headerName: '类型', width: 120 },
    { field: 'style', headerName: '风格', width: 120 },
    { field: 'duration', headerName: '时长', width: 90, renderCell: ({ value }) => value ? `${value}s` : '-' },
    { field: 'createTime', headerName: '创建时间', width: 160, renderCell: ({ value }) => String(value ?? '').slice(0, 16) },
    {
      field: '_del',
      headerName: '操作',
      width: 100,
      sortable: false,
      renderCell: ({ row }) => (
        <Button
          size="small"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={() => setDeleteScriptId(row.id as number)}
          data-testid="script-planning-delete-button"
          data-source-endpoint={SCRIPT_PLANNING_ENDPOINTS.scriptDelete}
        >
          删除
        </Button>
      ),
    },
  ]

  return (
    <Box
      data-testid="script-planning-page"
      data-ready-endpoints={SCRIPT_PLANNING_READY_ENDPOINTS}
      data-ready-routes={SCRIPT_PLANNING_READY_ROUTES}
      data-supported-actions={SCRIPT_PLANNING_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={SCRIPT_PLANNING_UNSUPPORTED_ENDPOINTS}
      data-no-local-script-fallback="true"
      data-no-local-project-mutation="true"
      data-no-local-shot-generation="true"
    >
      <PageHeader
        title="脚本策划"
        breadcrumbs={[
          { label: '短视频', href: shortvideoRoutes.dashboard },
          { label: '工作台', href: projectId ? `${shortvideoRoutes.workbench}?projectId=${projectId}` : shortvideoRoutes.projects },
          { label: '脚本策划' },
        ]}
        subtitle={projectId ? `项目 #${projectId}${project?.title ? ` · ${project.title}` : ''}` : '未关联项目时仅保存到脚本库'}
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="script-planning-boundary-contract"
        data-no-local-script-fallback="true"
        data-no-local-project-mutation="true"
        data-no-local-shot-generation="true"
        data-supported-actions={SCRIPT_PLANNING_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        脚本生成、保存、删除和分镜生成均以短视频后端接口为准；项目关联只在 `/short-video/project/save` 成功后生效。
      </Alert>
      {!projectId && <Alert severity="info" data-testid="script-planning-no-project-note" sx={{ mb: 2 }}>建议从项目工作台进入，保存后才能自动回写项目脚本与分镜状态。</Alert>}
      {projectIsError && (
        <Alert
          severity="error"
          data-testid="script-planning-project-error"
          data-no-local-project-fallback="true"
          sx={{ mb: 2 }}
          action={<Button color="inherit" size="small" onClick={() => refetchProject()}>重试</Button>}
        >
          项目加载失败：{getErrorMessage(projectError)}。脚本仍可保存到脚本库，但无法自动回写项目链路。
        </Alert>
      )}
      {operationError && (
        <Alert
          severity="error"
          data-testid="script-planning-operation-error"
          data-input-retained="true"
          data-no-local-script-mutation="true"
          data-no-local-project-mutation="true"
          data-no-local-shot-generation="true"
          sx={{ mb: 2 }}
          onClose={() => setOperationError('')}
        >
          {operationError}
        </Alert>
      )}

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }} data-testid="script-planning-tabs">
        <Tab label="AI 生成" />
        <Tab label="脚本列表" />
      </Tabs>

      {tab === 0 && (
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}>
            <Stack spacing={2}>
              <Card variant="outlined" data-testid="script-planning-diagnostics" data-no-client-link-synthesis="true">
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>链路诊断</Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1.5 }}>
                    {diagnostics.map((item) => (
                      <Chip
                        key={item.label}
                        label={`${item.label} ${item.value}`}
                        color={item.color}
                        size="small"
                        variant={item.color === 'default' ? 'outlined' : 'filled'}
                      />
                    ))}
                  </Stack>
                  <Stack spacing={1}>
                    {!canGenerate && (
                      <Alert severity="warning">
                        {type === 'viral_clone' ? '爆款复刻需要先填写库内爆款视频 ID。' : '请输入视频主题后再生成脚本。'}
                      </Alert>
                    )}
                    {canSave && !projectHasScript && (
                      <Alert severity="info">已有脚本文本但还未保存；保存后才会写入 `/short-video/script/save` 并关联项目。</Alert>
                    )}
                    {projectHasScript && !projectHasShotList && (
                      <Alert severity="info">脚本已就绪，可以生成分镜进入下一步。</Alert>
                    )}
                    <Alert severity="info">
                      后端生成接口当前主要使用主题/爆款 ID 组织提示词；风格、时长、字数会随保存结果入库，并在生成分镜时继续使用。
                    </Alert>
                  </Stack>
                </CardContent>
              </Card>
            </Stack>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card variant="outlined" data-testid="script-planning-generate-card" data-input-retained="true">
              <CardContent>
                <Stack spacing={2}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <FormControl size="small" sx={{ minWidth: 160 }}>
                  <InputLabel id="script-planning-type-label">生成模式</InputLabel>
                  <Select
                    labelId="script-planning-type-label"
                    id="script-planning-type"
                    value={type}
                    label="生成模式"
                    onChange={(e) => handleTypeChange(e.target.value)}
                    data-testid="script-planning-type-select"
                  >
                    {TYPE_OPTIONS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
                  </Select>
                </FormControl>
                <TextField
                  size="small"
                  label="视频主题"
                  value={theme}
                  onChange={(e) => setTheme(e.target.value)}
                  placeholder="例如：护肤品开箱测评"
                  sx={{ flex: 1 }}
                />
                <TextField
                  size="small"
                  label="目标时长"
                  type="number"
                  value={duration}
                  onChange={(e) => setDuration(Number(e.target.value) || 60)}
                  sx={{ width: 120 }}
                />
              </Stack>

              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  select
                  size="small"
                  label="脚本风格"
                  value={style}
                  onChange={(e) => setStyle(e.target.value)}
                  sx={{ minWidth: 240 }}
                >
                  {STYLE_OPTIONS.map((item) => <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>)}
                </TextField>
                <TextField
                  size="small"
                  label="爆款视频 ID"
                  value={viralVideoId}
                  onChange={(e) => setViralVideoId(e.target.value)}
                  placeholder="爆款复刻时填写库内 ID"
                  sx={{ minWidth: 180 }}
                />
                <TextField
                  size="small"
                  label="商品/卖点补充"
                  value={productInfo}
                  onChange={(e) => setProductInfo(e.target.value)}
                  sx={{ flex: 1 }}
                />
              </Stack>

              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Button
                  variant="contained"
                  startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <AutoAwesomeIcon />}
                  onClick={handleGenerate}
                  disabled={loading}
                  data-testid="script-planning-generate-button"
                  data-source-endpoint={SCRIPT_PLANNING_ENDPOINTS.scriptGenerate}
                >
                  {loading ? '生成中...' : '生成脚本'}
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<SaveIcon />}
                  onClick={handleSave}
                  disabled={!canSave}
                  data-testid="script-planning-save-button"
                  data-source-endpoint={SCRIPT_PLANNING_ENDPOINTS.scriptSave}
                >
                  保存并关联项目
                </Button>
                <Button
                  variant="outlined"
                  startIcon={shotLoading ? <CircularProgress size={16} /> : <ViewColumnIcon />}
                  onClick={handleGenerateShots}
                  disabled={shotLoading || !canGenerateShots}
                  data-testid="script-planning-generate-shots-button"
                  data-source-endpoint={SCRIPT_PLANNING_ENDPOINTS.shotListGenerate}
                >
                  生成分镜并进入下一步
                </Button>
                {activeScriptId && <Chip label={`当前脚本 #${activeScriptId}`} size="small" color="primary" variant="outlined" />}
              </Stack>

              {!!currentScriptContent && (
                <Box data-testid="script-planning-script-editor" data-input-retained="true" data-no-local-script-mutation="true">
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>脚本内容</Typography>
                  <TextField
                    value={currentScriptContent}
                    onChange={(e) => setGenerated(e.target.value)}
                    multiline
                    minRows={12}
                    fullWidth
                    sx={{
                      '& textarea': {
                        fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
                        fontSize: 13,
                        lineHeight: 1.7,
                      },
                    }}
                  />
                </Box>
              )}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tab === 1 && (
        <>
          {scriptsIsError && (
            <Alert
              severity="error"
              data-testid="script-planning-list-error"
              data-no-local-script-fallback="true"
              sx={{ mb: 2 }}
              action={<Button color="inherit" size="small" onClick={() => refetchScripts()}>重试</Button>}
            >
              脚本列表加载失败：{getErrorMessage(scriptsError)}。请检查 `/short-video/script/list|get`。
            </Alert>
          )}
          <Box data-testid="script-planning-list-contract" data-no-local-script-fallback="true" data-no-local-delete-mutation="true">
            <StandardDataGrid
              rows={(scripts as SvScript[]).map((r, index) => ({
                ...r,
                id: r.id ?? `${String(r.title ?? 'script')}-${String(r.createTime ?? '')}-${index}`,
              }))}
              columns={columns}
              loading={isLoading}
            />
          </Box>
        </>
      )}
      <ConfirmDialog
        open={deleteScriptId !== null}
        title="删除脚本"
        content="确认删除该短视频脚本吗？如果项目仍关联该脚本，后续分镜生成会失去脚本来源。"
        onClose={() => setDeleteScriptId(null)}
        onConfirm={handleConfirmDelete}
        loading={deleting}
      />
    </Box>
  )
}
