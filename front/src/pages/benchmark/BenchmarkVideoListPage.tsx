import { useState } from 'react';
import { Box, Button, Chip, Stack, TextField, MenuItem, Typography } from '@mui/material';
import { PlayArrow, Download } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { useSearchParams } from 'react-router-dom';
import { StandardDataGrid } from '@/components/base/StandardDataGrid';
import { PageHeader } from '@/components/base/PageHeader';
import { FormDialog } from '@/components/base/FormDialog';
import { ConfirmDialog } from '@/components/base/ConfirmDialog';
import { benchmarkVideoApi, benchmarkAnalysisApi } from '@/api/benchmark';
import type { BenchmarkVideoVO, CollectAccountVideosVO, AnalyzeVideoVO } from '@/types/benchmark';
import type { GridColDef } from '@mui/x-data-grid';

export default function BenchmarkVideoListPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
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

  // 查询列表
  const { data, isLoading } = useQuery({
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

  // 采集视频
  const collectMutation = useMutation({
    mutationFn: benchmarkVideoApi.collect,
    onSuccess: (videos) => {
      enqueueSnackbar(`采集到 ${videos.length} 个视频`, { variant: 'success' });
      setCollectDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkVideos'] });
    },
    onError: () => {
      enqueueSnackbar('采集失败', { variant: 'error' });
    },
  });

  // 分析视频
  const analyzeMutation = useMutation({
    mutationFn: benchmarkAnalysisApi.analyze,
    onSuccess: () => {
      enqueueSnackbar('分析任务已提交', { variant: 'success' });
      setAnalyzeDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkVideos'] });
    },
    onError: () => {
      enqueueSnackbar('分析失败', { variant: 'error' });
    },
  });

  // 删除
  const deleteMutation = useMutation({
    mutationFn: benchmarkVideoApi.delete,
    onSuccess: () => {
      enqueueSnackbar('删除成功', { variant: 'success' });
      setDeleteDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkVideos'] });
    },
    onError: () => {
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
    window.location.href = `/benchmark/analysis/${video.id}`;
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

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="对标视频管理"
        subtitle="采集和分析竞品账号的高质量视频"
      />

      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
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
          >
            采集视频
          </Button>
        )}
      </Stack>

      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        loading={isLoading}
        rowCount={data?.total ?? 0}
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={(model: { page: number; pageSize: number }) => {
          setPage(model.page);
          setPageSize(model.pageSize);
        }}
      />

      {/* 采集视频对话框 */}
      <FormDialog
        open={collectDialogOpen}
        title="采集账号视频"
        onClose={() => setCollectDialogOpen(false)}
        onConfirm={() => {
          const form = document.getElementById('collect-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: CollectAccountVideosVO = {
              benchmarkAccountId: accountId ? Number(accountId) : 0,
              minLikeCount: formData.get('minLikeCount') ? Number(formData.get('minLikeCount')) : undefined,
              maxVideos: formData.get('maxVideos') ? Number(formData.get('maxVideos')) : undefined,
            };
            handleCollect(data);
          }
        }}
      >
        <form id="collect-form">
          <Stack spacing={2}>
            <TextField name="minLikeCount" label="最低点赞数" type="number" required defaultValue={1000} fullWidth />
            <TextField name="maxVideos" label="最多采集数量" type="number" required defaultValue={50} fullWidth />
          </Stack>
        </form>
      </FormDialog>

      {/* 分析视频对话框 */}
      <FormDialog
        open={analyzeDialogOpen}
        title="分析视频"
        onClose={() => setAnalyzeDialogOpen(false)}
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
      />
    </Box>
  );
}
