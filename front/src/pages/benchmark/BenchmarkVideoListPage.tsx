import { useState } from 'react';
import { Alert, Box, Button, Card, CardContent, Chip, Stack, TextField, MenuItem, Typography } from '@mui/material';
import { PlayArrow, Download, Refresh } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { StandardDataGrid, PageHeader, FormDialog, ConfirmDialog, ErrorAlert, DataGridEmptyOverlay } from '@/components/base';
import { benchmarkVideoApi, benchmarkAnalysisApi } from '@/api/benchmark';
import { shortvideoBenchmarkAnalysisPath } from '@/constants/shortvideoRoutes';
import { shortvideoRoutes } from '@/constants/shortvideoRoutes';
import type { BenchmarkVideoVO, CollectAccountVideosVO, AnalyzeVideoVO } from '@/types/benchmark';
import type { GridColDef } from '@mui/x-data-grid';

const BENCHMARK_VIDEO_READY_ENDPOINTS = [
  '/benchmark/video/list',
  '/benchmark/video/collect',
  '/benchmark/video/delete',
  '/benchmark/analysis/analyze',
].join('|');
const BENCHMARK_VIDEO_READY_ROUTES = [
  shortvideoRoutes.benchmarkVideos,
  '/admin/shortvideo/benchmark/analysis/:id',
  shortvideoRoutes.benchmarkAccounts,
].join('|');
const BENCHMARK_VIDEO_SUPPORTED_ACTIONS = [
  'refresh-benchmark-videos',
  'collect-benchmark-videos',
  'analyze-benchmark-video',
  'view-benchmark-analysis',
  'delete-benchmark-video',
].join('|');

const BENCHMARK_VIDEO_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/video/mock',
  '/benchmark/video/local-list',
  '/benchmark/video/static-video',
  '/benchmark/video/local-collect',
  '/benchmark/analysis/static-analysis',
  '/benchmark/analysis/local-analyze',
].join('|');

export default function BenchmarkVideoListPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const accountId = searchParams.get('accountId');

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(30);
  const [keyword, setKeyword] = useState('');
  const [minLikeCount, setMinLikeCount] = useState<number | undefined>(1000);
  const [analysisStatus, setAnalysisStatus] = useState<string>('');

  const [collectDialogOpen, setCollectDialogOpen] = useState(false);
  const [analyzeDialogOpen, setAnalyzeDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [currentVideo, setCurrentVideo] = useState<BenchmarkVideoVO | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['benchmarkVideos', page, pageSize, accountId, keyword, minLikeCount, analysisStatus],
    queryFn: () => benchmarkVideoApi.list({
      page,
      rows: pageSize,
      benchmarkAccountId: accountId ? Number(accountId) : undefined,
      keyword,
      minLikeCount,
      analysisStatus,
    }),
  });

  const collectMutation = useMutation({
    mutationFn: benchmarkVideoApi.collect,
    onSuccess: (videos) => {
      enqueueSnackbar(`采集到 ${videos.length} 个视频`, { variant: 'success' });
      setActionError(null);
      setCollectDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkVideos'] });
    },
    onError: (err) => {
      setActionError(`视频采集失败：${getErrorMessage(err)}。来源：/benchmark/video/collect；请检查账号归属、抖音 Cookie、Playwright 和风控状态。`);
      enqueueSnackbar('采集失败', { variant: 'error' });
    },
  });

  const analyzeMutation = useMutation({
    mutationFn: benchmarkAnalysisApi.analyze,
    onSuccess: () => {
      enqueueSnackbar('分析任务已提交', { variant: 'success' });
      setActionError(null);
      setAnalyzeDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkVideos'] });
    },
    onError: (err) => {
      setActionError(`视频分析失败：${getErrorMessage(err)}。来源：/benchmark/analysis/analyze；请检查视频归属、下载、BOS、ASR/OCR 和 LLM 链路。`);
      enqueueSnackbar('分析失败', { variant: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: benchmarkVideoApi.delete,
    onSuccess: () => {
      enqueueSnackbar('删除成功', { variant: 'success' });
      setActionError(null);
      setDeleteDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkVideos'] });
    },
    onError: (err) => {
      setActionError(`视频删除失败：${getErrorMessage(err)}。来源：/benchmark/video/delete`);
      enqueueSnackbar('删除失败', { variant: 'error' });
    },
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': return 'success';
      case 'processing': return 'info';
      case 'failed': return 'error';
      default: return 'default';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'pending': return '待分析';
      case 'processing': return '分析中';
      case 'completed': return '已完成';
      case 'failed': return '失败';
      default: return status;
    }
  };

  const columns: GridColDef<BenchmarkVideoVO>[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    {
      field: 'title',
      headerName: '视频标题',
      width: 300,
      renderCell: (params) => (
        <Stack direction="row" spacing={1} alignItems="center">
          {params.row.coverUrl && (
            <img src={params.row.coverUrl} alt="" style={{ width: 60, height: 80, objectFit: 'cover', borderRadius: 4 }} />
          )}
          <span>{params.value || '无标题'}</span>
        </Stack>
      ),
    },
    {
      field: 'duration',
      headerName: '时长',
      width: 100,
      valueFormatter: (value: number | undefined) => {
        if (!value) return '-';
        const minutes = Math.floor(value / 60);
        const seconds = value % 60;
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
      },
    },
    {
      field: 'likeCount',
      headerName: '点赞数',
      width: 120,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'commentCount',
      headerName: '评论数',
      width: 100,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'shareCount',
      headerName: '分享数',
      width: 100,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'analysisStatus',
      headerName: '分析状态',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={getStatusLabel(params.value)}
          color={getStatusColor(params.value)}
          size="small"
        />
      ),
    },
    {
      field: 'publishTime',
      headerName: '发布时间',
      width: 180,
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 250,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            startIcon={<PlayArrow />}
            disabled={!params.row.videoUrl}
            onClick={() => window.open(params.row.videoUrl, '_blank')}
          >
            播放
          </Button>
          {params.row.analysisStatus === 'completed' ? (
            <Button size="small" onClick={() => handleViewAnalysis(params.row)}>查看分析</Button>
          ) : (
            <Button size="small" onClick={() => handleAnalyze(params.row)}>分析</Button>
          )}
          <Button size="small" color="error" onClick={() => handleDelete(params.row)}>删除</Button>
        </Stack>
      ),
    },
  ];

  const handleAnalyze = (video: BenchmarkVideoVO) => {
    setCurrentVideo(video);
    setAnalyzeDialogOpen(true);
  };

  const handleViewAnalysis = (video: BenchmarkVideoVO) => {
    navigate(shortvideoBenchmarkAnalysisPath(video.id));
  };

  const handleDelete = (video: BenchmarkVideoVO) => {
    setCurrentVideo(video);
    setDeleteDialogOpen(true);
  };

  const handleCollect = (data: CollectAccountVideosVO) => {
    collectMutation.mutate(data);
  };

  const handleAnalyzeSubmit = (data: AnalyzeVideoVO) => {
    analyzeMutation.mutate(data);
  };

  const rows = data?.list ?? [];
  const completedCount = rows.filter((item) => item.analysisStatus === 'completed').length;
  const failedCount = rows.filter((item) => item.analysisStatus === 'failed').length;
  const pendingCount = rows.filter((item) => item.analysisStatus === 'pending' || !item.analysisStatus).length;
  const downloadableCount = rows.filter((item) => item.localVideoPath || item.localPath || item.bosVideoUrl || item.bosUrl).length;
  const qualifiedCount = rows.filter((item) => item.isQualified).length;
  const listErrorMessage = error instanceof Error ? error.message : '对标视频列表加载失败，请检查 /benchmark/video/list。';

  return (
    <Box
      data-testid="benchmark-video-list-page"
      data-contract-scope="benchmark-video-server-collect-analysis"
      data-ready-endpoints={BENCHMARK_VIDEO_READY_ENDPOINTS}
      data-ready-routes={BENCHMARK_VIDEO_READY_ROUTES}
      data-supported-actions={BENCHMARK_VIDEO_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={BENCHMARK_VIDEO_UNSUPPORTED_ENDPOINTS}
      data-no-local-video-fallback="true"
      data-no-static-video-fallback="true"
      data-no-static-analysis-fallback="true"
      data-server-pagination="true"
      data-row-count={rows.length}
      data-total-count={data?.total ?? 0}
      data-list-error={isError ? 'true' : 'false'}
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="对标视频管理"
        subtitle="从对标账号采集视频，并提交 ASR/OCR/API/LLM 深度拆解；采集依赖 Cookie 和 Playwright。"
        actions={
          <Button size="small" variant="outlined" startIcon={<Refresh />} onClick={() => void refetch()} disabled={isFetching} data-testid="benchmark-video-refresh-button" data-source-endpoint="/benchmark/video/list">
            刷新
          </Button>
        }
      />

      {isError && (
        <Box data-testid="benchmark-video-list-error" data-no-local-video-fallback="true" data-input-retained="true">
          <ErrorAlert title="对标视频加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {actionError && (
        <Box data-testid="benchmark-video-action-error" data-input-retained="true" data-row-retained="true" data-no-local-video-mutation="true">
          <ErrorAlert title="对标视频操作失败" message={actionError} onRetry={() => setActionError(null)} />
        </Box>
      )}

      {!accountId && (
        <Alert severity="warning" variant="outlined">
          当前未从对标账号进入，后端会按登录用户隔离返回全部可见视频；采集按钮需要先进入某个对标账号的视频页。
        </Alert>
      )}

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">视频总数</Typography>
            <Typography variant="h5">{data?.total ?? rows.length}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">已完成分析</Typography>
            <Typography variant="h5" color="success.main">{completedCount}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">待分析</Typography>
            <Typography variant="h5">{pendingCount}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">分析失败</Typography>
            <Typography variant="h5" color={failedCount > 0 ? 'error.main' : 'text.primary'}>{failedCount}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">符合阈值</Typography>
            <Typography variant="h5">{qualifiedCount}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">已落地/BOS</Typography>
            <Typography variant="h5">{downloadableCount}</Typography>
          </CardContent>
        </Card>
      </Stack>

      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
        <TextField
          size="small"
          placeholder="搜索视频标题"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 300 }}
        />
        <TextField
          size="small"
          type="number"
          label="最低点赞数"
          value={minLikeCount ?? ''}
          onChange={(e) => setMinLikeCount(e.target.value ? Number(e.target.value) : undefined)}
          sx={{ width: 150 }}
        />
        <TextField
          select
          size="small"
          label="分析状态"
          value={analysisStatus}
          onChange={(e) => setAnalysisStatus(e.target.value)}
          sx={{ width: 150 }}
        >
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="pending">待分析</MenuItem>
          <MenuItem value="processing">分析中</MenuItem>
          <MenuItem value="completed">已完成</MenuItem>
          <MenuItem value="failed">失败</MenuItem>
        </TextField>
        {accountId && (
          <Button
            variant="contained"
            startIcon={<Download />}
            onClick={() => setCollectDialogOpen(true)}
            data-testid="benchmark-video-open-collect-button"
          >
            采集视频
          </Button>
        )}
      </Stack>

      <StandardDataGrid
        data-testid="benchmark-video-grid-contract"
        data-contract-scope="benchmark-video-grid-server-pagination"
        data-ready-endpoints="/benchmark/video/list"
        data-no-local-video-fallback="true"
        data-server-pagination="true"
        data-row-count={rows.length}
        rows={rows}
        columns={columns}
        loading={isFetching}
        rowCount={data?.total ?? 0}
        paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={(model: { page: number; pageSize: number }) => {
          setPage(model.page);
          setPageSize(model.pageSize);
        }}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
      />

      {/* 采集视频对话框 */}
      <FormDialog
        open={collectDialogOpen}
        title="采集账号视频"
        onClose={() => setCollectDialogOpen(false)}
        confirmText="开始采集"
        loading={collectMutation.isPending}
        onConfirm={() => {
          const form = document.getElementById('collect-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: CollectAccountVideosVO = {
              benchmarkAccountId: accountId ? Number(accountId) : 0,
              minLikeCount: formData.get('minLikeCount') ? Number(formData.get('minLikeCount')) : undefined,
              maxVideos: formData.get('maxVideos') ? Number(formData.get('maxVideos')) : undefined,
              cookieId: formData.get('cookieId') ? Number(formData.get('cookieId')) : undefined,
            };
            handleCollect(data);
          }
        }}
      >
        <form id="collect-form">
          <Stack spacing={2}>
            <TextField name="minLikeCount" label="最低点赞数" type="number" required defaultValue={1000} fullWidth />
            <TextField name="maxVideos" label="最多采集数量" type="number" required defaultValue={50} fullWidth />
            <TextField name="cookieId" label="指定 Cookie ID（可选）" type="number" fullWidth />
          </Stack>
        </form>
      </FormDialog>

      {/* 分析视频对话框 */}
      <FormDialog
        open={analyzeDialogOpen}
        title="分析视频"
        onClose={() => setAnalyzeDialogOpen(false)}
        confirmText="提交分析"
        loading={analyzeMutation.isPending}
        onConfirm={() => {
          const form = document.getElementById('analyze-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: AnalyzeVideoVO = {
              benchmarkVideoId: currentVideo?.id ?? 0,
              forceReanalyze: formData.get('forceReanalyze') === 'true',
              enableAsr: formData.get('enableAsr') === 'true',
              enableOcr: formData.get('enableOcr') === 'true',
              enableApi: formData.get('enableApi') === 'true',
              aiModel: formData.get('aiModel') as string || 'gpt-4',
              cookieId: formData.get('cookieId') ? Number(formData.get('cookieId')) : undefined,
            };
            handleAnalyzeSubmit(data);
          }
        }}
      >
        <form id="analyze-form">
          <Stack spacing={2}>
            <Typography variant="body2" color="text.secondary">
              视频：{currentVideo?.title}
            </Typography>

            <Typography variant="subtitle2" sx={{ mt: 2 }}>内容提取配置</Typography>
            <Stack direction="row" spacing={2}>
              <label>
                <input type="checkbox" name="enableAsr" value="true" defaultChecked />
                {' '}ASR 语音识别
              </label>
              <label>
                <input type="checkbox" name="enableOcr" value="true" defaultChecked />
                {' '}OCR 字幕识别
              </label>
              <label>
                <input type="checkbox" name="enableApi" value="true" />
                {' '}抖音 API
              </label>
            </Stack>

            <TextField
              select
              name="aiModel"
              label="AI 模型"
              defaultValue="gpt-4"
              fullWidth
            >
              <MenuItem value="gpt-4">GPT-4</MenuItem>
              <MenuItem value="gpt-3.5-turbo">GPT-3.5 Turbo</MenuItem>
              <MenuItem value="claude-3-opus">Claude 3 Opus</MenuItem>
              <MenuItem value="claude-3-sonnet">Claude 3 Sonnet</MenuItem>
            </TextField>
            <TextField name="cookieId" label="指定 Cookie ID（可选）" type="number" fullWidth />

            <label>
              <input type="checkbox" name="forceReanalyze" value="true" />
              {' '}强制重新分析（覆盖已有结果）
            </label>
          </Stack>
        </form>
      </FormDialog>

      {/* 删除确认对话框 */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="确认删除"
        content={`确定要删除视频"${currentVideo?.title}"吗？`}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={() => currentVideo && deleteMutation.mutate(currentVideo.id)}
        loading={deleteMutation.isPending}
      />
    </Box>
  );
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '未知错误';
}
