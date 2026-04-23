import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  TextField,
  MenuItem,
  Grid,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Add as AddIcon,
  Visibility as ViewIcon,
  Delete as DeleteIcon,
  Star as StarIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { StandardDataGrid } from '@/components/base/StandardDataGrid';
import { PageHeader } from '@/components/base/PageHeader';
import { ConfirmDialog } from '@/components/base/ConfirmDialog';
import { benchmarkQualityScriptApi } from '@/api/benchmark';
import type { BenchmarkQualityScriptVO, BenchmarkQualityScriptSearchVO } from '@/types/benchmark';

export default function BenchmarkQualityScriptListPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [searchParams, setSearchParams] = useState<BenchmarkQualityScriptSearchVO>({
    page: 0,
    rows: 30,
    keyword: '',
    industry: '',
    sceneType: '',
    scriptType: '',
    minQualityScore: undefined,
  });

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  // 查询质量脚本列表
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['benchmarkQualityScripts', searchParams],
    queryFn: () => benchmarkQualityScriptApi.search(searchParams),
  });

  // 删除质量脚本
  const deleteMutation = useMutation({
    mutationFn: (id: number) => benchmarkQualityScriptApi.delete(id),
    onSuccess: () => {
      enqueueSnackbar('删除成功', { variant: 'success' });
      queryClient.invalidateQueries({ queryKey: ['benchmarkQualityScripts'] });
      setDeleteDialogOpen(false);
    },
    onError: () => {
      enqueueSnackbar('删除失败', { variant: 'error' });
    },
  });

  const handleSearch = () => {
    setSearchParams((prev) => ({ ...prev, page: 0 }));
    refetch();
  };

  const handleReset = () => {
    setSearchParams({
      page: 0,
      rows: 30,
      keyword: '',
      industry: '',
      sceneType: '',
      scriptType: '',
      minQualityScore: undefined,
    });
  };

  const handleDelete = (id: number) => {
    setSelectedId(id);
    setDeleteDialogOpen(true);
  };

  const handleView = (id: number) => {
    navigate(`/benchmark/quality-script/${id}`);
  };

  const columns = [
    {
      field: 'id',
      headerName: 'ID',
      width: 80,
    },
    {
      field: 'scriptContent',
      headerName: '脚本内容',
      flex: 1,
      minWidth: 300,
      renderCell: (params: { row: BenchmarkQualityScriptVO }) => (
        <Tooltip title={params.row.scriptContent}>
          <Typography variant="body2" noWrap>
            {params.row.scriptContent}
          </Typography>
        </Tooltip>
      ),
    },
    {
      field: 'qualityScore',
      headerName: '质量评分',
      width: 120,
      renderCell: (params: { row: BenchmarkQualityScriptVO }) => (
        <Chip
          label={params.row.qualityScore.toFixed(1)}
          color={params.row.qualityScore >= 80 ? 'success' : params.row.qualityScore >= 70 ? 'primary' : 'default'}
          size="small"
          icon={params.row.qualityScore >= 80 ? <StarIcon /> : undefined}
        />
      ),
    },
    {
      field: 'scriptType',
      headerName: '脚本类型',
      width: 120,
    },
    {
      field: 'industry',
      headerName: '行业',
      width: 100,
    },
    {
      field: 'sceneType',
      headerName: '场景',
      width: 100,
    },
    {
      field: 'engagementRate',
      headerName: '互动率',
      width: 100,
      renderCell: (params: { row: BenchmarkQualityScriptVO }) =>
        params.row.engagementRate ? `${params.row.engagementRate.toFixed(2)}%` : '-',
    },
    {
      field: 'viralScore',
      headerName: '传播力',
      width: 100,
      renderCell: (params: { row: BenchmarkQualityScriptVO }) =>
        params.row.viralScore ? params.row.viralScore.toFixed(1) : '-',
    },
    {
      field: 'referenceCount',
      headerName: '引用次数',
      width: 100,
    },
    {
      field: 'createTime',
      headerName: '创建时间',
      width: 160,
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 120,
      sortable: false,
      renderCell: (params: { row: BenchmarkQualityScriptVO }) => (
        <Box>
          <IconButton size="small" onClick={() => handleView(params.row.id)} title="查看详情">
            <ViewIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" onClick={() => handleDelete(params.row.id)} title="删除">
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      ),
    },
  ];

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="质量脚本知识库"
        subtitle="高质量脚本的管理和推荐"
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/benchmark/quality-script/new')}>
            新增脚本
          </Button>
        }
      />

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                size="small"
                label="关键词"
                value={searchParams.keyword}
                onChange={(e) => setSearchParams((prev) => ({ ...prev, keyword: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                fullWidth
                size="small"
                select
                label="行业"
                value={searchParams.industry}
                onChange={(e) => setSearchParams((prev) => ({ ...prev, industry: e.target.value }))}
              >
                <MenuItem value="">全部</MenuItem>
                <MenuItem value="护肤">护肤</MenuItem>
                <MenuItem value="彩妆">彩妆</MenuItem>
                <MenuItem value="服饰">服饰</MenuItem>
                <MenuItem value="食品">食品</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                fullWidth
                size="small"
                select
                label="场景"
                value={searchParams.sceneType}
                onChange={(e) => setSearchParams((prev) => ({ ...prev, sceneType: e.target.value }))}
              >
                <MenuItem value="">全部</MenuItem>
                <MenuItem value="直播">直播</MenuItem>
                <MenuItem value="短视频">短视频</MenuItem>
                <MenuItem value="图文">图文</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} md={2}>
              <TextField
                fullWidth
                size="small"
                type="number"
                label="最低质量评分"
                value={searchParams.minQualityScore || ''}
                onChange={(e) =>
                  setSearchParams((prev) => ({ ...prev, minQualityScore: e.target.value ? Number(e.target.value) : undefined }))
                }
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button variant="contained" startIcon={<SearchIcon />} onClick={handleSearch} fullWidth>
                  搜索
                </Button>
                <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleReset}>
                  重置
                </Button>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <StandardDataGrid
        rows={data?.list || []}
        columns={columns}
        loading={isLoading}
        rowCount={data?.total || 0}
        paginationModel={{
          page: searchParams.page || 0,
          pageSize: searchParams.rows || 30,
        }}
        onPaginationModelChange={(model) => {
          setSearchParams((prev) => ({
            ...prev,
            page: model.page,
            rows: model.pageSize,
          }));
        }}
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        title="确认删除"
        content="确定要删除这个质量脚本吗？此操作不可恢复。"
        onConfirm={() => selectedId && deleteMutation.mutate(selectedId)}
        onClose={() => setDeleteDialogOpen(false)}
      />
    </Box>
  );
}
