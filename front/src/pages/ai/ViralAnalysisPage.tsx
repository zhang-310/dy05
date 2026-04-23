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
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { aiApi, type EvolutionStatsVO, type EvolutionViralItem } from '@/api/ai'
import { douyinApi, type DyVideo } from '@/api/douyin'
import { StandardDataGrid, PageHeader, ConfirmDialog } from '@/components/base'
import { useToast } from '@/contexts/ToastContext'
import type { GridColDef } from '@mui/x-data-grid'
import { useDebouncedValue } from '@/hooks/useDebouncedCallback'

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

function num(v: unknown): number {
  if (v == null) return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function isValidViralItem(o: unknown): o is EvolutionViralItem {
  if (!o || typeof o !== 'object') return false
  return num((o as Partial<EvolutionViralItem>).id) > 0
}

function normalizeViralRow(raw: Partial<EvolutionViralItem>): EvolutionViralItem {
  return {
    id: num(raw.id),
    videoId: raw.videoId ?? undefined,
    videoTitle: raw.videoTitle ?? undefined,
    accountId: raw.accountId ?? null,
    ownerId: raw.ownerId ?? undefined,
    viralScore: raw.viralScore ?? undefined,
    viewCount: raw.viewCount ?? undefined,
    avgViewCount: raw.avgViewCount ?? undefined,
    successFactors: raw.successFactors ?? null,
    replicableMethods: raw.replicableMethods ?? null,
    reportContent: raw.reportContent ?? null,
    qualityScore: raw.qualityScore ?? undefined,
    modelUsed: raw.modelUsed ?? null,
    tokensUsed: raw.tokensUsed ?? undefined,
    status: num(raw.status),
    createTime: raw.createTime ?? undefined,
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

  const statusParam = statusFilter === 'all' ? undefined : statusFilter

  const { data, isLoading } = useQuery({
    queryKey: ['viral-analysis-list', page, statusParam],
    queryFn: async () => {
      const res = await aiApi.evolveList({ page, rows: 20, status: statusParam })
      const list = (res.list ?? [])
        .filter(isValidViralItem)
        .map(normalizeViralRow)
      return { ...res, list }
    },
    refetchInterval: (query) => {
      const list = query.state.data?.list ?? []
      const anyPending = list.some((r) => r.status === 0)
      return anyPending ? 8000 : false
    },
  })
  const list = data?.list ?? []
  const total = data?.total ?? 0

  const { data: statsData } = useQuery({
    queryKey: ['viral-stats'],
    queryFn: () => aiApi.evolveStats(),
  })
  const stats = statsData

  const { data: detailFetched, isLoading: detailLoading } = useQuery({
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
      return
    }
    setVideoLoading(true)
    void douyinApi
      .videoSearch({ page: 0, rows: 30, keyword: q })
      .then((res) => {
        if (!cancelled) setVideoOptions(res.list ?? [])
      })
      .catch(() => {
        if (!cancelled) setVideoOptions([])
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
    onError: (e: Error) => toast(e.message || '触发失败', 'error'),
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
    onError: (e: Error) => toast(e.message || '删除失败', 'error'),
  })

  const openDetail = (row: EvolutionViralItem) => {
    setDetailId(row.id)
    setDetailOpen(true)
  }

  const columns: GridColDef<EvolutionViralItem>[] = useMemo(
    () => [
      { field: 'id', headerName: 'ID', width: 72 },
      { field: 'videoId', headerName: '视频ID', width: 100 },
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
            <Button size="small" color="error" onClick={() => setDeleteTargetId(r.id)}>
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
    <Box data-testid="viral-analysis-page" sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <PageHeader
        title="爆款分析"
        subtitle="基于 AI 进化引擎对抖音视频做轻量爆款拆解（ai_viral_analysis，结构化 JSON 报告）。短视频「爆款库深度分析」走 /short-video/viral，与本页接口不同。"
      />

      {stats ? (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} flexWrap="wrap" useFlexGap>
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

      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
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

      <Stack direction="row" spacing={2} justifyContent="flex-end" flexWrap="wrap" useFlexGap>
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
        <Button variant="contained" startIcon={<PlayArrowIcon />} onClick={() => setTriggerOpen(true)}>
          触发拆解
        </Button>
      </Stack>

      <Box sx={{ flex: 1, minHeight: 420 }}>
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
        />
      </Box>

      <Dialog open={triggerOpen} onClose={() => !triggerMutation.isPending && setTriggerOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>触发爆款拆解</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Autocomplete
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
                  helperText="选择后自动填入下方视频主键；也可手动填写"
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

      <ConfirmDialog
        open={deleteTargetId != null}
        title="删除爆款分析"
        content="确定删除该条拆解记录？此操作不可恢复。"
        loading={deleteMutation.isPending}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={() => {
          if (deleteTargetId != null) deleteMutation.mutate(deleteTargetId)
        }}
      />

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
        <DialogContent>
          {detailLoading ? (
            <Typography color="text.secondary">加载中…</Typography>
          ) : detail ? (
            <Stack spacing={2} sx={{ pt: 1 }}>
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
                    sx={{
                      fontSize: 12,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      bgcolor: 'grey.50',
                      p: 2,
                      borderRadius: 1,
                      m: 0,
                    }}
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
