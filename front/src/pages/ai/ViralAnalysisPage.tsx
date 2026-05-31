import { useEffect, useMemo, useState } from 'react'
import {
  Autocomplete,
  Box,
  Button,
  Chip,
  Stack,
  Typography,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Alert,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi, type EvolutionStatsVO, type EvolutionViralItem } from '@/api/ai'
import { douyinApi, type DyVideo } from '@/api/douyin'
import { StandardDataGrid, PageHeader, ConfirmDialog, DataGridEmptyOverlay, ErrorAlert } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import type { GridColDef } from '@mui/x-data-grid'
import { useDebouncedValue } from '@/hooks/useDebouncedCallback'
import { getErrorMessage } from '@/utils/errorHandler'
import { alpha } from '@mui/material/styles'
import { normalizeArray, readTotal } from '@/utils/response-normalize'

/** 与 AiViralAnalysis：0=分析中 1=完成 2=失败 */
const STATUS_LABELS: Record<number, { label: string; color: 'default' | 'info' | 'success' | 'error' }> = {
  0: { label: '分析中', color: 'info' },
  1: { label: '已完成', color: 'success' },
  2: { label: '失败', color: 'error' },
}

const STAT_CARDS: { key: keyof EvolutionStatsVO; label: string }[] = [
  { key: 'viralAnalysisTotal', label: '爆款分析总数' },
  { key: 'viralAnalysisDone', label: '爆款分析已完成' },
  { key: 'liveReviewTotal', label: '直播复盘总数' },
  { key: 'liveReviewDone', label: '直播复盘已完成' },
  { key: 'indexQueuePending', label: '索引队列待处理' },
]
const VIRAL_READY_ENDPOINTS = [
  '/ai/evolution/viral/list',
  '/ai/evolution/viral/get',
  '/ai/evolution/viral/trigger',
  '/ai/evolution/viral/delete',
  '/ai/evolution/stats',
  '/douyin/video/search',
].join(',')
const VIRAL_UNSUPPORTED_ENDPOINTS = [
  '/ai/evolution/viral/mock',
  '/ai/evolution/viral/local-list',
  '/ai/evolution/viral/static-report',
  '/ai/evolution/viral/local-trigger',
  '/ai/evolution/viral/local-delete',
  '/ai/evolution/viral/download-video',
  '/ai/evolution/viral/asr-local',
].join(',')

function num(v: unknown): number {
  if (v == null) return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function readAlias<T = unknown>(raw: Record<string, unknown>, keys: string[]): T | undefined {
  for (const key of keys) {
    if (raw[key] != null) return raw[key] as T
  }
  return undefined
}

function isValidViralItem(o: unknown): o is EvolutionViralItem {
  if (!o || typeof o !== 'object') return false
  const row = o as Record<string, unknown>
  return num(readAlias(row, ['id', 'analysisId', 'analysis_id'])) > 0
}

function normalizeViralRow(raw: Partial<EvolutionViralItem>): EvolutionViralItem {
  const record = raw as Record<string, unknown>
  return {
    id: num(readAlias(record, ['id', 'analysisId', 'analysis_id'])),
    videoId: num(readAlias(record, ['videoId', 'video_id'])) || undefined,
    videoTitle: readAlias<string>(record, ['videoTitle', 'video_title', 'title']) ?? undefined,
    accountId: num(readAlias(record, ['accountId', 'account_id'])) || null,
    ownerId: num(readAlias(record, ['ownerId', 'owner_id'])) || undefined,
    viralScore: num(readAlias(record, ['viralScore', 'viral_score', 'score'])) || undefined,
    viewCount: num(readAlias(record, ['viewCount', 'view_count', 'playCount', 'play_count'])) || undefined,
    avgViewCount: num(readAlias(record, ['avgViewCount', 'avg_view_count'])) || undefined,
    successFactors: readAlias<string>(record, ['successFactors', 'success_factors']) ?? null,
    replicableMethods: readAlias<string>(record, ['replicableMethods', 'replicable_methods']) ?? null,
    reportContent: readAlias<string>(record, ['reportContent', 'report_content', 'report']) ?? null,
    qualityScore: num(readAlias(record, ['qualityScore', 'quality_score'])) || undefined,
    modelUsed: readAlias<string>(record, ['modelUsed', 'model_used']) ?? null,
    tokensUsed: num(readAlias(record, ['tokensUsed', 'tokens_used'])) || undefined,
    status: num(readAlias(record, ['status', 'analysisStatus', 'analysis_status'])),
    createTime: readAlias<string>(record, ['createTime', 'create_time', 'createdAt', 'created_at']) ?? undefined,
  }
}

type StatusFilter = 'all' | 0 | 1 | 2

export default function ViralAnalysisPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all')
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailId, setDetailId] = useState<number | null>(null)
  const [triggerOpen, setTriggerOpen] = useState(false)
  const [videoIdInput, setVideoIdInput] = useState('')
  const [accountIdInput, setAccountIdInput] = useState('')
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null)
  const [videoPickInput, setVideoPickInput] = useState('')
  const debouncedPick = useDebouncedValue(videoPickInput, 400)
  const [videoOptions, setVideoOptions] = useState<DyVideo[]>([])
  const [videoLoading, setVideoLoading] = useState(false)
  const [videoSearchError, setVideoSearchError] = useState<string | null>(null)

  const statusParam = statusFilter === 'all' ? undefined : statusFilter

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['viral-analysis-list', page, statusParam],
    queryFn: async () => {
      const res = await aiApi.evolveList({ page, rows: 20, status: statusParam })
      const normalizedRows = normalizeArray<Partial<EvolutionViralItem>>(res)
      const list = normalizedRows
        .filter(isValidViralItem)
        .map(normalizeViralRow)
      return { ...res, total: readTotal(res, normalizedRows.length), list }
    },
    refetchInterval: (query) => {
      const list = query.state.data?.list ?? []
      const anyPending = list.some((r) => r.status === 0)
      return anyPending ? 8000 : false
    },
  })
  const list = data?.list ?? []
  const total = data?.total ?? 0
  const deleteTarget = useMemo(() => list.find(row => row.id === deleteTargetId), [deleteTargetId, list])

  const { data: statsData, isError: statsIsError, error: statsError } = useQuery({
    queryKey: ['viral-stats'],
    queryFn: () => aiApi.evolveStats(),
  })
  const stats = statsData

  const { data: detailFetched, isLoading: detailLoading, isError: detailIsError, error: detailError, refetch: refetchDetail } = useQuery({
    queryKey: ['viral-analysis-detail', detailId],
    queryFn: async () => {
      if (detailId == null) return null
      const raw = await aiApi.evolveViralGet(detailId)
      return normalizeViralRow(raw) ?? null
    },
    enabled: detailOpen && detailId != null,
  })

  useEffect(() => {
    let cancelled = false
    const q = debouncedPick.trim()
    if (!q) {
      setVideoOptions([])
      setVideoSearchError(null)
      return
    }
    setVideoLoading(true)
    setVideoSearchError(null)
    void douyinApi
      .videoSearch({ page: 0, rows: 30, title: q })
      .then((res) => {
        if (!cancelled) setVideoOptions(res.list ?? [])
      })
      .catch((e) => {
        if (!cancelled) {
          setVideoOptions([])
          setVideoSearchError(getErrorMessage(e))
        }
      })
      .finally(() => {
        if (!cancelled) setVideoLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [debouncedPick])

  const triggerMutation = useMutation({
    mutationFn: async (payload: { videoId: number; accountId?: number | null }) => {
      await aiApi.evolveTrigger(payload)
    },
    onSuccess: () => {
      toast('已触发爆款拆解任务', 'success')
      setTriggerOpen(false)
      setVideoIdInput('')
      setAccountIdInput('')
      setVideoPickInput('')
      void qc.invalidateQueries({ queryKey: ['viral-analysis-list'] })
      void qc.invalidateQueries({ queryKey: ['viral-stats'] })
    },
    onError: (e: Error) => toast(`触发失败：${getErrorMessage(e) || '请检查 videoId/accountId'}`, 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await aiApi.evolveDelete(id)
    },
    onSuccess: () => {
      toast('删除成功', 'success')
      setDeleteTargetId(null)
      void qc.invalidateQueries({ queryKey: ['viral-analysis-list'] })
      void qc.invalidateQueries({ queryKey: ['viral-stats'] })
    },
    onError: (e: Error) => toast(`删除失败：${getErrorMessage(e) || '请检查当前登录用户权限'}`, 'error'),
  })

  const triggerErrorText = triggerMutation.isError
    ? `触发失败（POST /ai/evolution/viral/trigger）：${getErrorMessage(triggerMutation.error)}。videoId=${videoIdInput || '未填写'}、accountId=${accountIdInput || '空'} 会保留。`
    : null
  const deleteErrorText = deleteMutation.isError && deleteTargetId != null
    ? `删除失败（POST /ai/evolution/viral/delete?id=${deleteTargetId}）：${getErrorMessage(deleteMutation.error)}。拆解记录 #${deleteTarget?.id ?? deleteTargetId} 会保留。`
    : null

  const openDetail = (row: EvolutionViralItem) => {
    setDetailId(row.id)
    setDetailOpen(true)
  }

  const columns: GridColDef<EvolutionViralItem>[] = useMemo(
    () => [
      { field: 'id', headerName: 'ID', width: 72 },
      { field: 'videoId', headerName: '视频ID', width: 100 },
      {
        field: 'videoTitle',
        headerName: '视频标题',
        minWidth: 160,
        flex: 0.7,
        renderCell: ({ value }) => {
          const title = typeof value === 'string' ? value.trim() : ''
          return title ? (
            <Typography variant="body2" noWrap title={title}>
              {title}
            </Typography>
          ) : (
            <Typography variant="body2" color="text.secondary">—</Typography>
          )
        },
      },
      { field: 'accountId', headerName: '账号ID', width: 100,
        valueFormatter: (v) => (v == null || v === '' ? '—' : String(v)) },
      { field: 'viewCount', headerName: '播放量', width: 100, type: 'number' },
      { field: 'qualityScore', headerName: '质量分', width: 88, type: 'number' },
      { field: 'viralScore', headerName: '爆款分', width: 88, type: 'number' },
      {
        field: 'status',
        headerName: '状态',
        width: 100,
        renderCell: ({ value }) => {
          const s = STATUS_LABELS[Number(value)] ?? { label: '未知', color: 'default' as const }
          return <Chip label={s.label} size="small" color={s.color} />
        },
      },
      {
        field: 'reportContent',
        headerName: '报告摘要',
        flex: 1,
        minWidth: 160,
        sortable: false,
        renderCell: ({ value }) => {
          const t = typeof value === 'string' ? value.trim() : ''
          if (!t) return <Typography variant="body2" color="text.secondary">—</Typography>
          const short = t.length > 80 ? `${t.slice(0, 80)}…` : t
          return (
            <Typography variant="body2" noWrap title={t}>
              {short}
            </Typography>
          )
        },
      },
      { field: 'createTime', headerName: '创建时间', width: 170 },
      {
        field: '_actions',
        headerName: '操作',
        width: 168,
        sortable: false,
        renderCell: ({ row: r }) => (
          <Stack direction="row" spacing={0.5}>
            <Button size="small" onClick={() => openDetail(r)}>
              详情
            </Button>
            <Button
              size="small"
              color="error"
              onClick={() => {
                deleteMutation.reset()
                setDeleteTargetId(r.id)
              }}
            >
              删除
            </Button>
          </Stack>
        ),
      },
    ],
    []
  )

  const submitTrigger = () => {
    const vid = Number(videoIdInput.trim())
    if (!Number.isFinite(vid) || vid <= 0) {
      toast('请填写有效的抖音视频 ID（videoId）', 'error')
      return
    }
    const aidRaw = accountIdInput.trim()
    const payload: { videoId: number; accountId?: number | null } = { videoId: vid }
    if (aidRaw !== '') {
      const aid = Number(aidRaw)
      if (!Number.isFinite(aid) || aid <= 0) {
        toast('账号 ID 无效，可留空', 'error')
        return
      }
      payload.accountId = aid
    }
    triggerMutation.mutate(payload)
  }

  const detail = detailFetched

  return (
    <Box
      data-testid="viral-analysis-page"
      data-ready-endpoints={VIRAL_READY_ENDPOINTS}
      data-unsupported-endpoints={VIRAL_UNSUPPORTED_ENDPOINTS}
      data-no-local-list="true"
      data-no-static-report="true"
      data-no-local-trigger="true"
      data-no-local-delete="true"
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="爆款分析"
        subtitle="基于 AI 进化引擎对抖音视频做轻量爆款拆解（ai_viral_analysis，结构化 JSON 报告）。短视频「爆款库深度分析」走 /short-video/viral，与本页接口不同。"
      />

      <Alert data-testid="viral-analysis-boundary-contract" data-no-local-analysis="true" data-no-static-report="true" severity="info" variant="outlined">
        真实接口：<code>POST /ai/evolution/viral/list</code>、<code>/get</code>、<code>/trigger</code>、<code>/delete</code>、<code>/ai/evolution/stats</code>。
        本页分析仅基于已同步的 DouyinVideo 元数据；不会下载视频流，也不会生成 ASR 或逐帧画面结论。
      </Alert>

      {stats ? (
        <Stack data-testid="viral-analysis-stats-cards" data-ready-endpoint="/ai/evolution/stats" direction={{ xs: 'column', sm: 'row' }} spacing={2} flexWrap="wrap" useFlexGap>
          {STAT_CARDS.map(({ key, label }) => (
            <Card key={key} variant="outlined" sx={{ flex: '1 1 140px', minWidth: 140 }}>
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">
                  {label}
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {String(stats[key] ?? 0)}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </Stack>
      ) : null}

      {statsIsError && (
        <Box data-testid="viral-analysis-stats-error" data-no-static-stats="true">
          <ErrorAlert
            severity="warning"
            title="统计数据不可用"
            message={`${getErrorMessage(statsError)}。请检查 POST /ai/evolution/stats；列表仍可继续使用，统计卡片按空值降级。`}
            onRetry={() => qc.invalidateQueries({ queryKey: ['viral-stats'] })}
          />
        </Box>
      )}

      <Stack data-testid="viral-analysis-filter-surface" direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
        <Typography variant="body2" color="text.secondary">
          状态筛选
        </Typography>
        <ToggleButtonGroup
          size="small"
          exclusive
          value={statusFilter}
          onChange={(_, v) => {
            if (v == null) return
            setStatusFilter(v)
            setPage(0)
          }}
        >
          <ToggleButton value="all">全部</ToggleButton>
          <ToggleButton value={0}>分析中</ToggleButton>
          <ToggleButton value={1}>已完成</ToggleButton>
          <ToggleButton value={2}>失败</ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      <Stack data-testid="viral-analysis-action-surface" direction="row" spacing={2} justifyContent="flex-end" flexWrap="wrap" useFlexGap>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => {
            void qc.invalidateQueries({ queryKey: ['viral-analysis-list'] })
            void qc.invalidateQueries({ queryKey: ['viral-stats'] })
          }}
        >
          刷新
        </Button>
        <Button
          variant="contained"
          startIcon={<PlayArrowIcon />}
          onClick={() => {
            triggerMutation.reset()
            setTriggerOpen(true)
          }}
        >
          触发拆解
        </Button>
      </Stack>

      {isError && (
        <Box data-testid="viral-analysis-list-error" data-no-local-list="true">
          <ErrorAlert
            title="爆款分析列表加载失败"
            message={`${getErrorMessage(error)}。请检查登录态、POST /ai/evolution/viral/list 和 ai_viral_analysis 数据权限。`}
            onRetry={() => refetch()}
          />
        </Box>
      )}

      {deleteErrorText && (
        <Alert data-testid="viral-analysis-delete-error" data-no-local-delete-mutation="true" severity="error">{deleteErrorText}</Alert>
      )}

      <Box data-testid="viral-analysis-grid" data-pagination-mode="server" data-no-local-list="true" sx={{ flex: 1, minHeight: 420 }}>
        <StandardDataGrid
          rows={list}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => String(r.id)}
          rowCount={total}
          paginationMode="server"
          paginationModel={{ page, pageSize: 20 }}
          onPaginationModelChange={(m) => setPage(m.page)}
          pageSizeOptions={[20]}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      <Dialog open={triggerOpen} onClose={() => !triggerMutation.isPending && setTriggerOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>触发爆款拆解</DialogTitle>
        <DialogContent>
          <Stack
            data-testid="viral-analysis-trigger-dialog"
            data-ready-endpoints="/ai/evolution/viral/trigger,/douyin/video/search"
            data-no-local-trigger="true"
            spacing={2}
            sx={{ mt: 1 }}
          >
            <Autocomplete
              data-testid="viral-analysis-video-search"
              loading={videoLoading}
              options={videoOptions}
              getOptionLabel={(o) => (o.title ? `${o.title} (id:${o.id})` : `id:${o.id}`)}
              isOptionEqualToValue={(a, b) => a.id === b.id}
              onChange={(_, v) => {
                if (v) setVideoIdInput(String(v.id))
              }}
              inputValue={videoPickInput}
              onInputChange={(_, v) => setVideoPickInput(v)}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="搜索已入库视频（标题）"
                  size="small"
                  placeholder="输入关键词筛选 DouyinVideo"
                  helperText={videoSearchError ? '视频搜索不可用，可继续手动填写视频主键' : '选择后自动填入下方视频主键；也可手动填写'}
                  error={Boolean(videoSearchError)}
                />
              )}
            />
            <TextField
              label="抖音视频 ID（DouyinVideo 主键）"
              value={videoIdInput}
              onChange={(e) => setVideoIdInput(e.target.value)}
              fullWidth
              size="small"
              required
              type="number"
              inputProps={{ min: 1 }}
            />
            <TextField
              label="账号 ID（可选）"
              value={accountIdInput}
              onChange={(e) => setAccountIdInput(e.target.value)}
              fullWidth
              size="small"
              type="number"
              inputProps={{ min: 1 }}
            />
            {videoSearchError ? (
              <Alert data-testid="viral-analysis-video-search-error" data-no-local-video-search="true" severity="warning">
                视频搜索失败（POST /douyin/video/search）：{videoSearchError}。视频 ID 输入会保留，可继续手动填写。
              </Alert>
            ) : null}
            {triggerErrorText && <Alert data-testid="viral-analysis-trigger-error" data-input-retained="true" data-no-local-trigger="true" severity="error">{triggerErrorText}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTriggerOpen(false)} disabled={triggerMutation.isPending}>
            取消
          </Button>
          <Button variant="contained" onClick={submitTrigger} disabled={triggerMutation.isPending}>
            提交
          </Button>
        </DialogActions>
      </Dialog>

      <Box data-testid="viral-analysis-delete-dialog-contract" data-ready-endpoint="/ai/evolution/viral/delete" data-no-local-delete-mutation="true">
        <ConfirmDialog
          open={deleteTargetId != null}
          title="删除爆款分析"
          content={deleteErrorText ? `${deleteErrorText}\n\n确定继续重试删除该条拆解记录？` : '确定删除该条拆解记录？此操作不可恢复。'}
          loading={deleteMutation.isPending}
          onClose={() => setDeleteTargetId(null)}
          onConfirm={() => {
            if (deleteTargetId != null) deleteMutation.mutate(deleteTargetId)
          }}
        />
      </Box>

      <Dialog
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false)
          setDetailId(null)
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>爆款拆解详情 {detail ? `#${detail.id}` : detailId != null ? `#${detailId}` : ''}</DialogTitle>
        <DialogContent data-testid="viral-analysis-detail-dialog" data-ready-endpoint="/ai/evolution/viral/get" data-no-static-report="true">
          {detailIsError ? (
            <Box data-testid="viral-analysis-detail-error" data-no-static-report="true">
              <ErrorAlert
                title="详情加载失败"
                message={`${getErrorMessage(detailError)}。请检查 POST /ai/evolution/viral/get?id=${detailId ?? ''} 和当前用户是否拥有该记录。`}
                onRetry={() => refetchDetail()}
              />
            </Box>
          ) : detailLoading ? (
            <Typography color="text.secondary">加载中…</Typography>
          ) : detail ? (
            <Stack data-testid="viral-analysis-detail-result" data-no-static-report="true" spacing={2} sx={{ pt: 1 }}>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Chip size="small" label={`videoId: ${detail.videoId ?? '—'}`} />
                {detail.videoTitle ? (
                  <Chip size="small" variant="outlined" label={detail.videoTitle} title={detail.videoTitle} />
                ) : null}
                <Chip size="small" label={`质量分: ${detail.qualityScore ?? '—'}`} />
                <Chip size="small" label={`爆款分: ${detail.viralScore ?? '—'}`} />
                <Chip size="small" label={`tokens: ${detail.tokensUsed ?? '—'}`} />
                {detail.modelUsed ? <Chip size="small" variant="outlined" label={detail.modelUsed} /> : null}
              </Stack>
              {detail.reportContent ? (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    综合报告（JSON 或文本）
                  </Typography>
                  <Box
                    component="pre"
                    data-testid="viral-analysis-report-preview-surface"
                    data-no-static-report="true"
                    sx={(theme) => ({
                      fontSize: 12,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      bgcolor: theme.palette.mode === 'dark'
                        ? theme.palette.background.default
                        : alpha(theme.palette.common.black, 0.025),
                      border: `1px solid ${theme.palette.divider}`,
                      p: 2,
                      borderRadius: 1,
                      m: 0,
                    })}
                  >
                    {(() => {
                      try {
                        return JSON.stringify(JSON.parse(detail.reportContent), null, 2)
                      } catch {
                        return detail.reportContent
                      }
                    })()}
                  </Box>
                </Box>
              ) : (
                <Typography color="text.secondary">暂无报告内容</Typography>
              )}
              {detail.successFactors ? (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    成功因素
                  </Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {detail.successFactors}
                  </Typography>
                </Box>
              ) : null}
              {detail.replicableMethods ? (
                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    可复制方法
                  </Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {detail.replicableMethods}
                  </Typography>
                </Box>
              ) : null}
            </Stack>
          ) : (
            <Typography color="text.secondary">无法加载详情</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setDetailOpen(false)
              setDetailId(null)
            }}
          >
            关闭
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
