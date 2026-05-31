import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  Chip,
  TextField,
  MenuItem,
  Grid,
  IconButton,
  Tooltip,
  Stack,
} from '@mui/material';
import {
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  Delete as DeleteIcon,
  Star as StarIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { StandardDataGrid, PageHeader, ConfirmDialog, ErrorAlert, DataGridEmptyOverlay, FormDialog } from '@/components/base';
import { benchmarkQualityScriptApi } from '@/api/benchmark';
import { shortvideoBenchmarkQualityScriptPath, shortvideoRoutes } from '@/constants/shortvideoRoutes';
import type { BenchmarkQualityScriptVO, BenchmarkQualityScriptSearchVO, BenchmarkQualityScriptSaveVO } from '@/types/benchmark';
import type { GridColDef } from '@mui/x-data-grid';

const defaultForm: BenchmarkQualityScriptSaveVO = {
  videoId: 0,
  analysisId: 0,
  scriptContent: '',
  scriptType: '',
  industry: '',
  sceneType: '',
  qualityScore: 50,
  engagementRate: undefined,
  viralScore: undefined,
  completionRate: undefined,
  aiRating: undefined,
  likesCount: undefined,
  commentsCount: undefined,
  sharesCount: undefined,
  collectionsCount: undefined,
  viewsCount: undefined,
  videoDuration: undefined,
  hookStrategy: '',
  contentStructure: '',
};

const QUALITY_SCRIPT_LIST_ENDPOINTS = [
  '/benchmark/quality-script/search',
  '/benchmark/quality-script/save',
  '/benchmark/quality-script/delete',
  '/benchmark/quality-script/calculate-quality-score',
].join('|');
const QUALITY_SCRIPT_LIST_READY_ROUTES = [
  shortvideoRoutes.benchmarkQualityScripts,
  '/admin/shortvideo/benchmark/quality-scripts/:id',
  shortvideoRoutes.insights,
].join('|');
const QUALITY_SCRIPT_LIST_SUPPORTED_ACTIONS = [
  'search-quality-scripts',
  'create-quality-script',
  'calculate-quality-score',
  'save-quality-script',
  'delete-quality-script',
  'navigate-quality-script-detail',
].join('|');
const QUALITY_SCRIPT_LIST_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/quality-script/export',
  '/benchmark/quality-script/batch-delete',
  '/benchmark/quality-script/generate-local',
  '/benchmark/script-similarity/generate-embedding',
  '/benchmark/script-similarity/batch-generate-embeddings',
  '/benchmark/script-similarity/index-to-milvus',
  '/benchmark/script-similarity/batch-index-to-milvus',
].join('|');
const QUALITY_SCRIPT_LIST_UNSUPPORTED_ACTIONS = [
  'local-quality-script-fallback',
  'local-score-calculation',
  'local-delete-mutation',
  'batch-vector-indexing',
  'server-export',
].join('|');

function toNumberOrUndefined(value: string): number | undefined {
  if (value.trim() === '') return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function toNumberOrZero(value: string): number {
  return toNumberOrUndefined(value) ?? 0;
}

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
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<BenchmarkQualityScriptSaveVO>(defaultForm);
  const [formError, setFormError] = useState('');
  const [operationError, setOperationError] = useState('');

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['benchmarkQualityScripts', searchParams],
    queryFn: () => benchmarkQualityScriptApi.search(searchParams),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => benchmarkQualityScriptApi.delete(id),
    onSuccess: () => {
      enqueueSnackbar('删除成功', { variant: 'success' });
      queryClient.invalidateQueries({ queryKey: ['benchmarkQualityScripts'] });
      setDeleteDialogOpen(false);
      setSelectedId(null);
      setOperationError('');
    },
    onError: (error: Error) => {
      enqueueSnackbar('删除失败', { variant: 'error' });
      setOperationError(`/benchmark/quality-script/delete 删除失败：${error.message || 'unknown'}。脚本 ID ${selectedId ?? '-'} 的行已保留。`);
    },
  });

  const calculateMutation = useMutation({
    mutationFn: () => benchmarkQualityScriptApi.calculateQualityScore(form),
    onSuccess: (score) => {
      setForm((prev) => ({ ...prev, qualityScore: Number(score.toFixed(2)) }));
      setFormError('');
      enqueueSnackbar('质量评分已计算', { variant: 'success' });
    },
    onError: (e: Error) => {
      setFormError(`/benchmark/quality-script/calculate-quality-score 计算失败：${e.message || 'unknown'}。表单输入已保留，页面不做本地评分。`);
    },
  });

  const saveMutation = useMutation({
    mutationFn: (payload: BenchmarkQualityScriptSaveVO) => benchmarkQualityScriptApi.save(payload),
    onSuccess: (saved) => {
      enqueueSnackbar('质量脚本已保存', { variant: 'success' });
      queryClient.invalidateQueries({ queryKey: ['benchmarkQualityScripts'] });
      setFormOpen(false);
      setForm(defaultForm);
      setFormError('');
      setOperationError('');
      if (saved.id) navigate(shortvideoBenchmarkQualityScriptPath(saved.id));
    },
    onError: (e: Error) => {
      setFormError(`/benchmark/quality-script/save 保存失败：${e.message || 'unknown'}。表单输入已保留。`);
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
    navigate(shortvideoBenchmarkQualityScriptPath(id));
  };

  const openAdd = () => {
    setForm(defaultForm);
    setFormError('');
    setFormOpen(true);
  };

  const handleFormSave = () => {
    if (!form.videoId || form.videoId <= 0) {
      setFormError('请填写有效的视频 ID。');
      return;
    }
    if (!form.analysisId || form.analysisId <= 0) {
      setFormError('请填写有效的分析 ID。');
      return;
    }
    if (!form.scriptContent.trim()) {
      setFormError('请填写脚本内容。');
      return;
    }
    if (form.qualityScore < 0 || form.qualityScore > 100) {
      setFormError('质量评分必须在 0 到 100 之间。');
      return;
    }
    setFormError('');
    saveMutation.mutate({
      ...form,
      scriptContent: form.scriptContent.trim(),
      scriptType: form.scriptType?.trim() || undefined,
      industry: form.industry?.trim() || undefined,
      sceneType: form.sceneType?.trim() || undefined,
      hookStrategy: form.hookStrategy?.trim() || undefined,
      contentStructure: form.contentStructure?.trim() || undefined,
    });
  };

  const columns: GridColDef<BenchmarkQualityScriptVO>[] = [
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
      renderCell: (params) => (
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
      renderCell: (params) => (
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
      renderCell: (params) =>
        params.row.engagementRate ? `${params.row.engagementRate.toFixed(2)}%` : '-',
    },
    {
      field: 'viralScore',
      headerName: '传播力',
      width: 100,
      renderCell: (params) =>
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
      renderCell: (params) => (
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

  const rows = data?.list ?? [];
  const highQualityCount = rows.filter((item) => item.qualityScore >= 80).length;
  const missingEmbeddingCount = rows.filter((item) => !item.embeddingVector).length;
  const referencedCount = rows.filter((item) => (item.referenceCount ?? 0) > 0).length;
  const listErrorMessage = error instanceof Error ? error.message : '质量脚本列表加载失败，请检查 /benchmark/quality-script/search。';

  return (
    <Box
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
      data-testid="benchmark-quality-script-list-workbench"
      data-contract-scope="benchmark-quality-script-library"
      data-ready-endpoints={QUALITY_SCRIPT_LIST_ENDPOINTS}
      data-ready-routes={QUALITY_SCRIPT_LIST_READY_ROUTES}
      data-supported-actions={QUALITY_SCRIPT_LIST_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={QUALITY_SCRIPT_LIST_UNSUPPORTED_ENDPOINTS}
      data-unsupported-actions={QUALITY_SCRIPT_LIST_UNSUPPORTED_ACTIONS}
      data-row-count={rows.length}
      data-total-count={data?.total ?? 0}
      data-high-quality-count={highQualityCount}
      data-missing-embedding-count={missingEmbeddingCount}
      data-form-open={String(formOpen)}
      data-delete-id={selectedId ?? ''}
      data-list-error={String(isError)}
      data-operation-error={operationError}
      data-no-local-quality-script-fallback="true"
      data-no-local-score-calculation="true"
      data-no-local-delete-mutation="true"
      data-no-server-export="true"
    >
      <PageHeader
        title="质量脚本知识库"
        subtitle="沉淀爆款拆解后的可复用脚本，支撑相似检索、推荐和短视频脚本策划。"
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd} data-testid="benchmark-quality-script-open-create-button">
              新增脚本
            </Button>
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={isFetching} data-testid="benchmark-quality-script-refresh-button" data-source-endpoint="/benchmark/quality-script/search">
              刷新
            </Button>
          </Stack>
        }
      />

      {isError && (
        <Box
          data-testid="benchmark-quality-script-list-error"
          data-contract-source="/benchmark/quality-script/search"
          data-no-local-quality-script-fallback="true"
        >
          <ErrorAlert title="质量脚本加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}

      {operationError && (
        <Box
          data-testid="benchmark-quality-script-operation-error"
          data-contract-source="/benchmark/quality-script/delete"
          data-row-retained="true"
          data-no-local-delete-mutation="true"
        >
          <ErrorAlert title="质量脚本操作失败" message={operationError} onRetry={() => void refetch()} />
        </Box>
      )}

      <Alert
        severity="info"
        variant="outlined"
        data-testid="benchmark-quality-script-list-source-contract"
        data-contract-source={QUALITY_SCRIPT_LIST_ENDPOINTS}
        data-unsupported-endpoints={QUALITY_SCRIPT_LIST_UNSUPPORTED_ENDPOINTS}
        data-no-local-score-calculation="true"
        data-supported-actions={QUALITY_SCRIPT_LIST_SUPPORTED_ACTIONS}
      >
        质量脚本可由视频深度分析自动落库，也可在本页手工新增。新增保存调用 `/benchmark/quality-script/save`，评分计算调用 `/benchmark/quality-script/calculate-quality-score`；保存成功后直接进入详情页。
      </Alert>

      <Grid container spacing={2}>
        <Grid item xs={12} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">脚本总数</Typography>
              <Typography variant="h5">{data?.total ?? rows.length}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">高质量脚本</Typography>
              <Typography variant="h5" color="success.main">{highQualityCount}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">已被引用</Typography>
              <Typography variant="h5">{referencedCount}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="caption" color="text.secondary">未生成向量</Typography>
              <Typography variant="h5" color={missingEmbeddingCount > 0 ? 'warning.main' : 'text.primary'}>{missingEmbeddingCount}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card
        sx={{ mb: 2 }}
        data-testid="benchmark-quality-script-search-contract"
        data-contract-source="/benchmark/quality-script/search"
        data-keyword={searchParams.keyword || ''}
        data-industry={searchParams.industry || ''}
        data-scene-type={searchParams.sceneType || ''}
        data-min-quality-score={searchParams.minQualityScore ?? ''}
        data-no-local-quality-script-fallback="true"
      >
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

      <Box
        data-testid="benchmark-quality-script-grid-contract"
        data-contract-source="/benchmark/quality-script/search"
        data-row-count={rows.length}
        data-total-count={data?.total ?? 0}
        data-no-local-quality-script-fallback="true"
        sx={{ flex: 1, minHeight: 0 }}
      >
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={data?.total ?? 0}
          paginationMode="server"
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
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
          sx={{ flex: 1 }}
        />
      </Box>

      <ConfirmDialog
        open={deleteDialogOpen}
        title="确认删除"
        content="确定要删除这个质量脚本吗？此操作不可恢复。"
        onConfirm={() => selectedId && deleteMutation.mutate(selectedId)}
        onClose={() => setDeleteDialogOpen(false)}
        loading={deleteMutation.isPending}
      />

      <FormDialog
        open={formOpen}
        title="新增质量脚本"
        onClose={() => setFormOpen(false)}
        onConfirm={handleFormSave}
        confirmText="保存并查看详情"
        loading={saveMutation.isPending}
        maxWidth="md"
      >
        <Stack
          spacing={2}
          sx={{ pt: 0.5 }}
          data-testid="benchmark-quality-script-form-contract"
          data-contract-source="/benchmark/quality-script/save|/benchmark/quality-script/calculate-quality-score"
          data-video-id={form.videoId || ''}
          data-analysis-id={form.analysisId || ''}
          data-script-length={form.scriptContent.length}
          data-score={form.qualityScore}
          data-input-retained={String(Boolean(formError))}
          data-no-local-score-calculation="true"
        >
          {formError && (
            <Alert
              severity="error"
              data-testid="benchmark-quality-script-form-error"
              data-input-retained="true"
              data-no-local-score-calculation={String(formError.includes('calculate-quality-score'))}
            >
              {formError}
            </Alert>
          )}
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <TextField
                label="视频 ID"
                type="number"
                size="small"
                fullWidth
                value={form.videoId || ''}
                onChange={(e) => setForm((prev) => ({ ...prev, videoId: toNumberOrZero(e.target.value) }))}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                label="分析 ID"
                type="number"
                size="small"
                fullWidth
                value={form.analysisId || ''}
                onChange={(e) => setForm((prev) => ({ ...prev, analysisId: toNumberOrZero(e.target.value) }))}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                label="质量评分"
                type="number"
                size="small"
                fullWidth
                value={form.qualityScore}
                onChange={(e) => setForm((prev) => ({ ...prev, qualityScore: toNumberOrZero(e.target.value) }))}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Button
                variant="outlined"
                fullWidth
                sx={{ height: 40 }}
                onClick={() => calculateMutation.mutate()}
                disabled={calculateMutation.isPending}
                data-testid="benchmark-quality-script-calculate-button"
                data-source-endpoint="/benchmark/quality-script/calculate-quality-score"
              >
                {calculateMutation.isPending ? '计算中...' : '计算评分'}
              </Button>
            </Grid>
          </Grid>
          <TextField
            label="脚本内容"
            multiline
            minRows={4}
            fullWidth
            value={form.scriptContent}
            onChange={(e) => setForm((prev) => ({ ...prev, scriptContent: e.target.value }))}
          />
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField label="脚本类型" size="small" fullWidth value={form.scriptType ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, scriptType: e.target.value }))} />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField label="行业" size="small" fullWidth value={form.industry ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, industry: e.target.value }))} />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField label="场景" size="small" fullWidth value={form.sceneType ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, sceneType: e.target.value }))} />
            </Grid>
          </Grid>
          <Grid container spacing={2}>
            <Grid item xs={6} md={3}>
              <TextField label="互动率" type="number" size="small" fullWidth value={form.engagementRate ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, engagementRate: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} md={3}>
              <TextField label="传播力" type="number" size="small" fullWidth value={form.viralScore ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, viralScore: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} md={3}>
              <TextField label="完播率" type="number" size="small" fullWidth value={form.completionRate ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, completionRate: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} md={3}>
              <TextField label="AI 评分" type="number" size="small" fullWidth value={form.aiRating ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, aiRating: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
          </Grid>
          <Grid container spacing={2}>
            <Grid item xs={6} md={3}>
              <TextField label="播放数" type="number" size="small" fullWidth value={form.viewsCount ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, viewsCount: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} md={3}>
              <TextField label="点赞数" type="number" size="small" fullWidth value={form.likesCount ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, likesCount: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} md={3}>
              <TextField label="评论数" type="number" size="small" fullWidth value={form.commentsCount ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, commentsCount: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} md={3}>
              <TextField label="视频时长（秒）" type="number" size="small" fullWidth value={form.videoDuration ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, videoDuration: toNumberOrUndefined(e.target.value) }))} />
            </Grid>
          </Grid>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField label="钩子策略" size="small" fullWidth value={form.hookStrategy ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, hookStrategy: e.target.value }))} />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField label="内容结构" size="small" fullWidth value={form.contentStructure ?? ''} onChange={(e) => setForm((prev) => ({ ...prev, contentStructure: e.target.value }))} />
            </Grid>
          </Grid>
        </Stack>
      </FormDialog>
    </Box>
  );
}
