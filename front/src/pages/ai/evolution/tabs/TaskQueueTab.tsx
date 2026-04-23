import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { Link as RouterLink } from 'react-router-dom'
import {
  Alert, Box, Button, Chip, Stack, Typography, Link, Paper,
  FormControl, InputLabel, Select, MenuItem, LinearProgress, IconButton,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import AssessmentIcon from '@mui/icons-material/Assessment'
import DescriptionIcon from '@mui/icons-material/Description'
import CloseIcon from '@mui/icons-material/Close'
import type { GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { aiApi } from '@/api/ai'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import { AGENT_TYPES, parseScopeKbId, TASK_STATUS_MAP } from '@/pages/ai/evolution/engineConstants'
import { EVOLVE_TASK_STATUS_FILTER_OPTIONS, statusToUi } from '@/pages/ai/evolution/taskQueueListFilters'
import type { AiEvolveTaskVO } from '@/types/ai'

function formatScoreDetailText(raw: string): string {
  const t = raw.trim()
  if (!t.startsWith('{') && !t.startsWith('[')) return raw
  try {
    return JSON.stringify(JSON.parse(t), null, 2)
  } catch {
    return raw
  }
}

export interface TaskQueueTabProps {
  scopeKbId: string
}

export function TaskQueueTab({ scopeKbId }: TaskQueueTabProps) {
  const toast = useToast()
  const qc = useQueryClient()
  const scopeKbNumeric = parseScopeKbId(scopeKbId)
  const [page, setPage] = useState(0)
  const [taskType, setTaskType] = useState('')
  const [statusCategory, setStatusCategory] = useState('')
  const [detailItem, setDetailItem] = useState<AiEvolveTaskVO | null>(null)
  const [taskReport, setTaskReport] = useState<unknown | null>(null)

  const closeDetail = () => {
    setDetailItem(null)
    setTaskReport(null)
  }

  useEffect(() => {
    if (!detailItem) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setDetailItem(null)
        setTaskReport(null)
      }
    }
    document.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [detailItem])

  const statusParam = statusCategory === '' ? undefined : Number(statusCategory)

  const { data, isFetching, isError, error, isLoading } = useQuery({
    queryKey: ['evolve-task-list', page, taskType, scopeKbId || 'all', statusCategory || 'all'],
    queryFn: () => aiApi.evolveTaskList({
      page,
      rows: 20,
      taskType: taskType || undefined,
      kbId: scopeKbNumeric,
      status: statusParam,
    }),
    refetchInterval: 8000,
  })

  const triggerMut = useMutation({
    mutationFn: (at: string) => aiApi.evolveTaskTrigger({
      taskType: at,
      targetKbId: scopeKbNumeric,
    }),
    onSuccess: (res) => {
      const sid = res?.taskId ? `（进度会话 ${res.taskId}）` : ''
      toast(`已提交后台执行${sid}，列表将自动刷新`, 'success')
      // runEvolution 为异步：立即 refetch 时库内可能尚未插入任务
      window.setTimeout(() => {
        qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
      }, 1200)
      window.setTimeout(() => {
        qc.invalidateQueries({ queryKey: ['evolve-task-list'] })
      }, 3500)
    },
    onError: () => toast('触发失败', 'error'),
  })
  const cancelMut = useMutation({
    mutationFn: (id: number) => aiApi.evolveTaskCancel(id),
    onSuccess: () => { toast('已取消', 'success'); qc.invalidateQueries({ queryKey: ['evolve-task-list'] }) },
    onError: () => toast('取消失败', 'error'),
  })

  const reportMut = useMutation({
    mutationFn: (id: number) => aiApi.evolveTaskReport(id),
    onSuccess: (data) => {
      setTaskReport((data as Record<string, unknown>) ?? null)
      toast('已加载后台报告', 'success')
    },
    onError: () => toast('报告加载失败（可能无权限或尚无报告）', 'error'),
  })

  const rows = data?.list ?? []
  const total = data?.total ?? 0

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'taskType', headerName: 'Agent 类型', width: 140,
      renderCell: (p: GridRenderCellParams) => {
        const agent = AGENT_TYPES.find(a => a.code === String(p.value ?? ''))
        return <Typography variant="body2">{agent?.label ?? String(p.value ?? '')}</Typography>
      } },
    { field: 'status', headerName: '状态', width: 100,
      renderCell: (p: GridRenderCellParams) => {
        const ui = statusToUi(p.value)
        const s = TASK_STATUS_MAP[ui] ?? TASK_STATUS_MAP[0]
        return <Chip label={s.label} size="small" color={s.color} />
      } },
    { field: 'progress', headerName: '进度', width: 120,
      renderCell: (p: GridRenderCellParams) => (
        <Box sx={{ width: '100%', pr: 1 }}>
          <LinearProgress variant="determinate" value={Number(p.value ?? 0)}
            sx={{ height: 6, borderRadius: 3 }} />
          <Typography variant="caption">{Number(p.value ?? 0)}%</Typography>
        </Box>
      ) },
    { field: 'kbId', headerName: 'KB', width: 72,
      valueGetter: (_v, row) => row.kbId ?? '—' },
    { field: 'scoreTotal', headerName: '得分', width: 72,
      valueGetter: (_v, row) => row.scoreTotal ?? '—' },
    { field: 'targetId', headerName: '目标', width: 80 },
    { field: 'createTime', headerName: '创建时间', width: 160,
      renderCell: (p: GridRenderCellParams) => (
        <Typography variant="body2">{formatDate(String(p.value ?? ''))}</Typography>
      ) },
    { field: 'actions', headerName: '操作', width: 180, sortable: false,
      renderCell: (p: GridRenderCellParams) => {
        const row = p.row as AiEvolveTaskVO
        const ui = statusToUi(row.status)
        return (
          <Stack direction="row" spacing={0.5}>
            <Button size="small" startIcon={<AssessmentIcon />} onClick={() => { setDetailItem(row); setTaskReport(null) }}>详情</Button>
            {ui === 1 && (
              <Button size="small" color="error" onClick={() => cancelMut.mutate(Number(row.id))}>取消</Button>
            )}
          </Stack>
        )
      } },
  ]

  return (
    <>
    <Box sx={{ bgcolor: 'var(--color-surface-dark)', p: 'var(--spacing-lg)', borderRadius: 'var(--border-radius-xl)' }}>
      {isError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          加载任务列表失败：{error instanceof Error ? error.message : '请检查登录与后端服务'}
        </Alert>
      ) : null}
      {!isFetching && !isError && total === 0 ? (
        <Alert severity="info" sx={{ mb: 2 }}>
          {scopeKbNumeric != null ? (
            <>
              当前按知识库 <strong>KB #{scopeKbNumeric}</strong> 筛选。若刚点过「触发」仍为空：任务异步入库，请等待 2～3 秒或点右上角刷新；若仍无数据，可能是任务落在<strong>默认知识库</strong>，请把顶部「知识库范围」改为「全部知识库」再看。
            </>
          ) : (
            <>
              暂无任务。点击「触发 xxx Agent」后约 1～3 秒出现在列表（后台异步创建）。若主题池为空，引擎会跳过且不会产生新行。
            </>
          )}
        </Alert>
      ) : null}
      {triggerMut.isPending ? <LinearProgress sx={{ mb: 1 }} /> : null}
      <Stack direction="row" spacing={2} mb={2} alignItems="center" flexWrap="wrap">
        <FormControl size="small" sx={{ minWidth: 160, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
          <InputLabel sx={{ color: 'var(--color-text-secondary)' }}>Agent 类型</InputLabel>
          <Select label="Agent 类型" value={taskType} onChange={e => { setTaskType(e.target.value); setPage(0) }} sx={{ color: 'var(--color-text-primary)' }}>
            <MenuItem value="">全部</MenuItem>
            {AGENT_TYPES.map(a => <MenuItem key={a.code} value={a.code}>{a.label}</MenuItem>)}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140, '& .MuiOutlinedInput-root': { borderRadius: 'var(--border-radius-lg)', bgcolor: 'var(--color-surface)', color: 'var(--color-text-primary)', '& fieldset': { borderColor: 'var(--color-surface-light)' }, '&:hover fieldset': { borderColor: 'var(--color-primary)' } } }}>
          <InputLabel sx={{ color: 'var(--color-text-secondary)' }}>状态</InputLabel>
          <Select label="状态" value={statusCategory} onChange={e => { setStatusCategory(e.target.value); setPage(0) }} sx={{ color: 'var(--color-text-primary)' }}>
            {EVOLVE_TASK_STATUS_FILTER_OPTIONS.map(o => <MenuItem key={o.value || 'all'} value={o.value}>{o.label}</MenuItem>)}
          </Select>
        </FormControl>
        <Box sx={{ flex: 1 }} />
        {AGENT_TYPES.map(a => (
          <Button key={a.code} size="small" variant="outlined" startIcon={<PlayArrowIcon />}
            onClick={() => triggerMut.mutate(a.code)} disabled={triggerMut.isPending}
            sx={{ borderColor: 'var(--color-surface-light)', color: 'var(--color-text-primary)', '&:hover': { borderColor: 'var(--color-primary)', bgcolor: 'rgba(0, 208, 132, 0.04)' } }}>
            触发 {a.label}
          </Button>
        ))}
      </Stack>
      {!isError && total > 0 ? (
        <Typography variant="body2" sx={{ mb: 1, color: 'var(--color-text-secondary)' }}>
          共 <strong>{total}</strong> 条任务（服务器分页，每页 20 条）
        </Typography>
      ) : null}
      {/*
        与主题池相同：autoHeight + StandardDataGrid 内部 height:100%/absolute 会导致父链无高度时表格可视区域为 0。
      */}
      <Box sx={{ width: '100%', height: 520, minHeight: 360 }}>
        <StandardDataGrid
          rows={rows} columns={columns} loading={isFetching || isLoading}
          paginationMode="server" rowCount={total}
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={m => setPage(m.page)}
          pageSizeOptions={[20]}
          getRowId={r => String((r as AiEvolveTaskVO).id)}
          slotProps={{ toolbar: {} as import('@mui/x-data-grid').GridToolbarProps }}
          autoHeight={false}
        />
      </Box>
    </Box>

    {detailItem && typeof document !== 'undefined'
      ? createPortal(
          <>
            <Box
              role="presentation"
              aria-hidden
              onClick={closeDetail}
              sx={(theme) => ({
                position: 'fixed',
                inset: 0,
                zIndex: 49999,
                bgcolor: alpha(theme.palette.common.black, 0.5),
              })}
            />
            <Paper
              component="div"
              role="dialog"
              aria-modal="true"
              aria-labelledby="evolve-task-detail-title"
              elevation={16}
              square
              sx={{
                position: 'fixed',
                top: 0,
                right: 0,
                bottom: 0,
                width: { xs: '100%', sm: 520 },
                maxWidth: '100vw',
                zIndex: 50000,
                borderRadius: 0,
                borderLeft: 1,
                borderColor: 'divider',
                bgcolor: 'background.paper',
                color: 'text.primary',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                isolation: 'isolate',
              }}
            >
              <Box
                sx={{
                  flexShrink: 0,
                  px: 3,
                  pt: 2.5,
                  pb: 2,
                  borderBottom: 1,
                  borderColor: 'divider',
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
                  <Typography id="evolve-task-detail-title" variant="h6" sx={{ lineHeight: 1.3, pr: 1 }}>
                    任务详情 #{detailItem.id}
                  </Typography>
                  <IconButton aria-label="关闭" size="small" onClick={closeDetail}>
                    <CloseIcon />
                  </IconButton>
                </Box>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mt: 2 }}>
                  <Button size="small" variant="outlined" startIcon={<DescriptionIcon />}
                    disabled={reportMut.isPending}
                    onClick={() => reportMut.mutate(Number(detailItem.id))}>
                    拉取后台报告
                  </Button>
                  <Link component={RouterLink} to="/admin/ai/knowledge-evolution" variant="body2" sx={{ lineHeight: 2 }}>
                    打开进化监控看板
                  </Link>
                </Stack>
              </Box>
              <Box
                sx={{
                  flex: 1,
                  minHeight: 0,
                  overflowY: 'auto',
                  overflowX: 'hidden',
                  px: 3,
                  py: 2,
                }}
              >
                <Stack spacing={2.5}>
              <Box>
                <Typography variant="caption" color="text.secondary" display="block" gutterBottom>taskNo</Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
                  {String(detailItem.taskNo ?? '—')}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" display="block" gutterBottom>知识库 / 得分</Typography>
                <Typography variant="body2">
                  kbId：{detailItem.kbId ?? '—'}　得分：{detailItem.scoreTotal ?? '—'}
                </Typography>
              </Box>
              {detailItem.topicTexts ? (
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block" gutterBottom>主题摘要</Typography>
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      overflowWrap: 'anywhere',
                      lineHeight: 1.6,
                    }}
                  >
                    {String(detailItem.topicTexts).length > 4000
                      ? `${String(detailItem.topicTexts).slice(0, 4000)}…`
                      : String(detailItem.topicTexts)}
                  </Typography>
                </Box>
              ) : null}
              {detailItem.scoreDetail ? (
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block" gutterBottom>评分明细</Typography>
                  <Box
                    component="pre"
                    sx={(theme) => ({
                      m: 0,
                      p: 1.5,
                      borderRadius: 1,
                      bgcolor: theme.palette.mode === 'dark' ? 'grey.900' : 'grey.100',
                      border: '1px solid',
                      borderColor: 'divider',
                      fontFamily: 'ui-monospace, monospace',
                      fontSize: 12,
                      lineHeight: 1.5,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      overflowWrap: 'anywhere',
                      maxHeight: 320,
                      overflow: 'auto',
                      color: 'text.primary',
                    })}
                  >
                    {formatScoreDetailText(String(detailItem.scoreDetail))}
                  </Box>
                </Box>
              ) : null}
              {detailItem.errorMessage ? (
                <Box>
                  <Typography variant="caption" color="error" display="block" gutterBottom>错误信息</Typography>
                  <Typography variant="body2" sx={{ color: 'error.main', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {detailItem.errorMessage}
                  </Typography>
                </Box>
              ) : null}
              {taskReport && Object.keys(taskReport).length > 0 ? (
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block" gutterBottom>后台报告（摘要）</Typography>
                  <Box
                    component="pre"
                    sx={(theme) => ({
                      m: 0,
                      p: 1.5,
                      borderRadius: 1,
                      bgcolor: theme.palette.mode === 'dark' ? 'grey.900' : 'grey.100',
                      border: '1px solid',
                      borderColor: 'divider',
                      fontFamily: 'ui-monospace, monospace',
                      fontSize: 11,
                      lineHeight: 1.5,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      maxHeight: 280,
                      overflow: 'auto',
                      color: 'text.primary',
                    })}
                  >
                    {JSON.stringify(taskReport, null, 2).slice(0, 8000)}
                    {JSON.stringify(taskReport).length > 8000 ? '\n…' : ''}
                  </Box>
                </Box>
              ) : null}
            </Stack>
              </Box>
            </Paper>
          </>,
          document.body,
        )
      : null}
    </>
  )
}
