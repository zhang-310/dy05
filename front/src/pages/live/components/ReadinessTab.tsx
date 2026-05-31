import {
  Box, Typography, Chip, CircularProgress, Alert, List, ListItem,
  ListItemIcon, ListItemText, Divider, Button, Stack, Card, CardContent, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import WarningIcon from '@mui/icons-material/Warning'
import ErrorIcon from '@mui/icons-material/Error'
import RefreshIcon from '@mui/icons-material/Refresh'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import BookmarkAddIcon from '@mui/icons-material/BookmarkAdd'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import AssignmentIcon from '@mui/icons-material/Assignment'
import { useEffect, useState } from 'react'
import { useCoreData } from '../contexts'
import { liveApi } from '@/api/live'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { useLocation, useNavigate } from 'react-router-dom'
import { inferLiveRouteScope, liveSessionPath } from '../liveRouteScope'
import { getErrorMessage } from '@/utils/errorHandler'

const READINESS_TAB_READY_ENDPOINTS = {
  readiness: '/live/session/readiness',
  templateSaveAs: '/live/session-template/save-as',
  sessionClone: '/live/session/clone',
  scriptExport: '/live/script/export',
} as const

const READINESS_TAB_CONTEXT_ENDPOINTS = [
  '/live/session/get',
  '/live/product/by-session',
  '/live/script/by-session',
] as const

const READINESS_TAB_UNSUPPORTED_ACTIONS = [
  'session-status-start',
  'shortvideo-project-create',
  'live-ai-generation',
  'direct-product-mutation',
  'direct-script-mutation',
  'local-backend-readiness-score-fallback',
  'local-template-fallback',
  'local-clone-fallback',
  'local-export-fallback',
] as const

interface ReadinessItem {
  key: string
  label: string
  status: 'ok' | 'warn' | 'error'
  detail?: string
}

function getReadinessScore(data: unknown): number | '' {
  if (!data || typeof data !== 'object') return ''
  const score = Number((data as { score?: unknown; readiness?: unknown }).score ?? (data as { readiness?: unknown }).readiness)
  return Number.isFinite(score) ? score : ''
}

function ReadinessIcon({ status }: { status: ReadinessItem['status'] }) {
  if (status === 'ok') return <CheckCircleIcon color="success" />
  if (status === 'warn') return <WarningIcon color="warning" />
  return <ErrorIcon color="error" />
}

function SaveAsTemplateDialog({ sessionId, open, onClose }: { sessionId: number; open: boolean; onClose: () => void }) {
  const toast = useToast()
  const [name, setName] = useState('')
  const [desc, setDesc] = useState('')
  const [saveError, setSaveError] = useState<string | null>(null)
  useEffect(() => {
    if (open) setSaveError(null)
  }, [open])
  const mut = useMutation({
    mutationFn: () => liveApi.templateSaveAsFromSession({ sessionId, name: name.trim(), description: desc.trim() }),
    onMutate: () => setSaveError(null),
    onSuccess: () => { toast('已保存为模板', 'success'); onClose() },
    onError: (e: unknown) => {
      const message = `${READINESS_TAB_READY_ENDPOINTS.templateSaveAs} 保存模板失败：${getErrorMessage(e)}`
      setSaveError(message)
      toast(message, 'error')
    },
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>保存为模板</DialogTitle>
      <DialogContent
        data-testid="readiness-template-dialog"
        data-contract-source={READINESS_TAB_READY_ENDPOINTS.templateSaveAs}
        data-no-local-template-fallback="true"
        sx={{ pt: 2 }}
      >
        <Stack spacing={2}>
          {saveError && (
            <Alert
              severity="error"
              data-testid="readiness-template-save-error"
              data-contract-source={READINESS_TAB_READY_ENDPOINTS.templateSaveAs}
              data-no-local-template-fallback="true"
            >
              {saveError}
            </Alert>
          )}
          <TextField label="模板名称" value={name} onChange={e => setName(e.target.value)} size="small" fullWidth required />
          <TextField label="描述（可选）" value={desc} onChange={e => setDesc(e.target.value)} size="small" fullWidth multiline rows={2} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} size="small">取消</Button>
        <Button
          onClick={() => mut.mutate()}
          disabled={!name.trim() || mut.isPending}
          variant="contained"
          size="small"
          data-testid="readiness-template-save-button"
        >
          {mut.isPending ? <CircularProgress size={14} /> : '保存'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
export function ReadinessTab() {
  const { session, products, scripts } = useCoreData()
  const toast = useToast()
  const navigate = useNavigate()
  const routeScope = inferLiveRouteScope(useLocation().pathname)
  const sessionId = session?.id
  const [showSaveTemplate, setShowSaveTemplate] = useState(false)
  const [operationError, setOperationError] = useState<string | null>(null)
  const [operationErrorSource, setOperationErrorSource] = useState<string | null>(null)

  const { data: readinessData, isFetching, refetch, isError: readinessIsError, error: readinessError } = useQuery({
    queryKey: ['wb-readiness-detail', sessionId],
    queryFn: () => liveApi.sessionReadiness(sessionId!),
    enabled: !!sessionId,
  })

  const cloneMut = useMutation({
    mutationFn: () => {
      if (!sessionId) throw new Error('缺少场次 ID')
      return liveApi.sessionClone(sessionId)
    },
    onSuccess: (newId) => {
      toast('场次已克隆', 'success')
      navigate(liveSessionPath(routeScope, newId))
    },
    onMutate: () => {
      setOperationError(null)
      setOperationErrorSource(null)
    },
    onError: (e: unknown) => {
      const message = `${READINESS_TAB_READY_ENDPOINTS.sessionClone} 克隆场次失败：${getErrorMessage(e)}`
      setOperationError(message)
      setOperationErrorSource(READINESS_TAB_READY_ENDPOINTS.sessionClone)
      toast(message, 'error')
    },
  })

  const exportMut = useMutation({
    mutationFn: () => {
      if (!sessionId) throw new Error('缺少场次 ID')
      return liveApi.scriptExport({ sessionId, format: 'txt' })
    },
    onSuccess: (content) => {
      const text = typeof content === 'string' ? content : JSON.stringify(content ?? '')
      const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${session?.liveTitle ?? '话术'}.txt`
      a.click()
      URL.revokeObjectURL(url)
      toast('话术已导出', 'success')
    },
    onMutate: () => {
      setOperationError(null)
      setOperationErrorSource(null)
    },
    onError: (e: unknown) => {
      const message = `${READINESS_TAB_READY_ENDPOINTS.scriptExport} 导出话术失败：${getErrorMessage(e)}`
      setOperationError(message)
      setOperationErrorSource(READINESS_TAB_READY_ENDPOINTS.scriptExport)
      toast(message, 'error')
    },
  })

  const filledScripts = scripts.filter(s => (s.scriptContent ?? '').trim().length > 0)
  const activatedScripts = scripts.filter(s => s.status === 1)

  const localItems: ReadinessItem[] = [
    {
      key: 'products',
      label: '商品已配置',
      status: products.length > 0 ? 'ok' : 'error',
      detail: products.length > 0 ? `已选 ${products.length} 个商品` : '请先在「选品排品」步骤添加商品',
    },
    {
      key: 'scripts',
      label: '话术已生成',
      status: scripts.length > 0 ? 'ok' : 'warn',
      detail: scripts.length > 0 ? `已有 ${scripts.length} 条话术` : '建议先生成话术再开播',
    },
    {
      key: 'title',
      label: '直播标题已填写',
      status: session?.liveTitle ? 'ok' : 'warn',
      detail: session?.liveTitle || '未填写直播标题',
    },
    {
      key: 'scheduled',
      label: '已设置开播时间',
      status: session?.scheduledTime ? 'ok' : 'warn',
      detail: session?.scheduledTime || '未设置开播时间',
    },
    {
      key: 'script_content',
      label: '话术内容已编辑',
      status: filledScripts.length === scripts.length && scripts.length > 0
        ? 'ok'
        : scripts.length > 0 ? 'warn' : 'error',
      detail: scripts.length === 0
        ? '暂无话术'
        : `${filledScripts.length}/${scripts.length} 条已填写内容`,
    },
    {
      key: 'activated',
      label: '话术已激活',
      status: activatedScripts.length > 0 ? 'ok' : 'warn',
      detail: activatedScripts.length > 0
        ? `已激活 ${activatedScripts.length}/${scripts.length} 条`
        : '建议激活话术后开播',
    },
  ]

  const errorCount = localItems.filter(i => i.status === 'error').length
  const warnCount = localItems.filter(i => i.status === 'warn').length
  const allOk = errorCount === 0 && warnCount === 0
  const completionPct = scripts.length === 0 ? 0 : Math.round((filledScripts.length / scripts.length) * 100)
  const backendReadinessScore = getReadinessScore(readinessData)
  const backendReadinessStatus = readinessIsError ? 'error' : isFetching ? 'checking' : readinessData ? 'ready' : 'empty'

  return (
    <Box
      data-testid="readiness-tab-workbench"
      data-contract-scope="live-readiness-release-check"
      data-ready-endpoints={Object.values(READINESS_TAB_READY_ENDPOINTS).join('|')}
      data-context-endpoints={READINESS_TAB_CONTEXT_ENDPOINTS.join('|')}
      data-unsupported-actions={READINESS_TAB_UNSUPPORTED_ACTIONS.join('|')}
      data-route-scope={routeScope}
      data-session-id={sessionId ?? ''}
      data-products-count={products.length}
      data-scripts-count={scripts.length}
      data-local-error-count={errorCount}
      data-local-warn-count={warnCount}
      data-local-all-ok={String(allOk)}
      data-backend-readiness-status={backendReadinessStatus}
      data-backend-readiness-score={backendReadinessScore}
      sx={{ height: '100%', overflow: 'auto', p: 2 }}
    >
      <Box sx={{ maxWidth: 640, mx: 'auto' }}>
        <Alert
          severity="info"
          data-testid="readiness-contract-alert"
          data-contract-source={`${READINESS_TAB_CONTEXT_ENDPOINTS.join('|')}|${Object.values(READINESS_TAB_READY_ENDPOINTS).join('|')}`}
          data-no-local-backend-readiness-score-fallback="true"
          sx={{ mb: 2 }}
        >
          准备发布页只读取工作台上下文和后端准备度检查；模板保存、克隆和话术导出失败时保留当前页面状态，不创建本地模板、场次或短视频项目。
        </Alert>

        {/* 操作栏 */}
        <Stack
          direction="row"
          spacing={1}
          sx={{ mb: 2 }}
          flexWrap="wrap"
          data-testid="readiness-action-toolbar"
          data-contract-source={Object.values(READINESS_TAB_READY_ENDPOINTS).join('|')}
        >
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={() => { setOperationError(null); setOperationErrorSource(null); refetch() }}
            disabled={!sessionId || isFetching}
            variant="outlined"
            data-testid="readiness-refresh-button"
            data-contract-source={READINESS_TAB_READY_ENDPOINTS.readiness}
          >
            {isFetching ? <CircularProgress size={12} /> : '刷新检查'}
          </Button>
          <Button
            size="small"
            startIcon={<FileDownloadIcon />}
            onClick={() => exportMut.mutate()}
            disabled={!sessionId || exportMut.isPending}
            variant="outlined"
            data-testid="readiness-export-button"
            data-contract-source={READINESS_TAB_READY_ENDPOINTS.scriptExport}
            data-no-local-export-fallback="true"
          >
            {exportMut.isPending ? <CircularProgress size={12} /> : '导出话术'}
          </Button>
          <Button
            size="small"
            startIcon={<BookmarkAddIcon />}
            onClick={() => setShowSaveTemplate(true)}
            disabled={!sessionId}
            variant="outlined"
            data-testid="readiness-save-template-button"
            data-contract-source={READINESS_TAB_READY_ENDPOINTS.templateSaveAs}
            data-no-local-template-fallback="true"
          >
            保存为模板
          </Button>
          <Button
            size="small"
            startIcon={<ContentCopyIcon />}
            onClick={() => cloneMut.mutate()}
            disabled={!sessionId || cloneMut.isPending}
            variant="outlined"
            data-testid="readiness-clone-button"
            data-contract-source={READINESS_TAB_READY_ENDPOINTS.sessionClone}
            data-no-local-clone-fallback="true"
          >
            {cloneMut.isPending ? <CircularProgress size={12} /> : '克隆场次'}
          </Button>
        </Stack>

        {readinessIsError && (
          <Alert
            severity="warning"
            sx={{ mb: 2 }}
            data-testid="readiness-backend-error"
            data-contract-source={READINESS_TAB_READY_ENDPOINTS.readiness}
            data-no-local-backend-readiness-score-fallback="true"
          >
            {READINESS_TAB_READY_ENDPOINTS.readiness} 后端准备度检测失败：{getErrorMessage(readinessError)}。页面只保留本地检查项，不伪造后端准备度分数。
          </Alert>
        )}
        {operationError && (
          <Alert
            severity="error"
            sx={{ mb: 2 }}
            data-testid="readiness-operation-error"
            data-contract-source={operationErrorSource ?? ''}
            data-no-local-template-fallback={operationErrorSource === READINESS_TAB_READY_ENDPOINTS.templateSaveAs ? 'true' : undefined}
            data-no-local-clone-fallback={operationErrorSource === READINESS_TAB_READY_ENDPOINTS.sessionClone ? 'true' : undefined}
            data-no-local-export-fallback={operationErrorSource === READINESS_TAB_READY_ENDPOINTS.scriptExport ? 'true' : undefined}
          >
            {operationError}
          </Alert>
        )}

        {/* 统计概览 */}
        <Grid container spacing={1.5} sx={{ mb: 2 }}>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined" data-testid="readiness-kpi-products" data-contract-source="/live/product/by-session"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">商品数</Typography>
              <Typography variant="h5" fontWeight={700}>{products.length}</Typography>
            </CardContent></Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined" data-testid="readiness-kpi-scripts" data-contract-source="/live/script/by-session"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">话术数</Typography>
              <Typography variant="h5" fontWeight={700}>{scripts.length}</Typography>
            </CardContent></Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined" data-testid="readiness-kpi-completion" data-contract-source="/live/script/by-session"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">内容完成率</Typography>
              <Typography variant="h5" fontWeight={700}>{completionPct}%</Typography>
            </CardContent></Card>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Card variant="outlined" data-testid="readiness-kpi-activated" data-contract-source="/live/script/by-session"><CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="caption" color="text.secondary">已激活</Typography>
              <Typography variant="h5" fontWeight={700}>{activatedScripts.length}</Typography>
            </CardContent></Card>
          </Grid>
        </Grid>

        {/* 总体状态 */}
        <Box
          data-testid="readiness-overall-status-surface"
          data-contract-source={READINESS_TAB_CONTEXT_ENDPOINTS.join('|')}
          data-local-error-count={errorCount}
          data-local-warn-count={warnCount}
          data-local-all-ok={String(allOk)}
          data-no-backend-score-fallback="true"
          sx={(theme) => ({
          p: 2, mb: 2, borderRadius: 2,
          bgcolor: alpha(
            allOk
              ? theme.palette.success.main
              : errorCount > 0
                ? theme.palette.error.main
                : theme.palette.warning.main,
            theme.palette.mode === 'dark' ? 0.16 : 0.1,
          ),
          border: '1px solid',
          borderColor: alpha(
            allOk
              ? theme.palette.success.main
              : errorCount > 0
                ? theme.palette.error.main
                : theme.palette.warning.main,
            theme.palette.mode === 'dark' ? 0.45 : 0.28,
          ),
          display: 'flex', alignItems: 'center', gap: 2,
        })}
        >
          {allOk
            ? <CheckCircleIcon color="success" sx={{ fontSize: 40 }} />
            : errorCount > 0
            ? <ErrorIcon color="error" sx={{ fontSize: 40 }} />
            : <WarningIcon color="warning" sx={{ fontSize: 40 }} />}
          <Box sx={{ flex: 1 }}>
            <Typography variant="subtitle1" fontWeight={700}>
              {allOk ? '准备就绪，可以开播！' : errorCount > 0 ? '有问题需要修复' : '有警告项，建议处理'}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
              {errorCount > 0 && <Chip label={`${errorCount} 个错误`} color="error" size="small" />}
              {warnCount > 0 && <Chip label={`${warnCount} 个警告`} color="warning" size="small" />}
              {allOk && <Chip label="全部通过" color="success" size="small" />}
            </Box>
          </Box>
          <AssignmentIcon sx={{ fontSize: 32, opacity: 0.15 }} />
        </Box>

        {/* 检查项列表 */}
        <Typography variant="subtitle2" sx={{ mb: 1 }}>准备检查项</Typography>
        <List
          dense
          data-testid="readiness-local-check-list"
          data-contract-source={READINESS_TAB_CONTEXT_ENDPOINTS.join('|')}
          sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, overflow: 'hidden' }}
        >
          {localItems.map((item, idx) => (
            <Box key={item.key}>
              <ListItem
                sx={{ py: 1 }}
                data-testid={`readiness-local-check-${item.key}`}
                data-readiness-status={item.status}
                data-contract-source={READINESS_TAB_CONTEXT_ENDPOINTS.join('|')}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <ReadinessIcon status={item.status} />
                </ListItemIcon>
                <ListItemText
                  primary={<Typography variant="body2" fontWeight={600}>{item.label}</Typography>}
                  secondary={item.detail}
                />
              </ListItem>
              {idx < localItems.length - 1 && <Divider />}
            </Box>
          ))}
        </List>

        {/* 后端就绪数据 */}
        {isFetching && <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}><CircularProgress size={24} /></Box>}
        {readinessData && !isFetching && (
          <Alert
            severity="success"
            sx={{ mt: 2, fontSize: 12 }}
            data-testid="readiness-backend-success"
            data-contract-source={READINESS_TAB_READY_ENDPOINTS.readiness}
            data-backend-readiness-score={backendReadinessScore}
          >
            后端就绪检测已完成{backendReadinessScore !== '' ? `，分数 ${backendReadinessScore}` : ''}
          </Alert>
        )}
      </Box>

      {typeof sessionId === 'number' && (
        <SaveAsTemplateDialog sessionId={sessionId} open={showSaveTemplate} onClose={() => setShowSaveTemplate(false)} />
      )}
    </Box>
  )
}



