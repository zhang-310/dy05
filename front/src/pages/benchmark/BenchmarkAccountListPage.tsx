import { useState } from 'react';
import { Alert, Box, Button, Card, CardContent, Chip, Stack, TextField, Typography } from '@mui/material';
import { Add, Refresh, Search, Link as LinkIcon } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { useNavigate } from 'react-router-dom';
import { StandardDataGrid, PageHeader, FormDialog, ConfirmDialog, ErrorAlert, DataGridEmptyOverlay } from '@/components/base';
import { benchmarkAccountApi } from '@/api/benchmark';
import { shortvideoBenchmarkVideosPath } from '@/constants/shortvideoRoutes';
import type { BenchmarkAccountVO, BenchmarkAccountSaveVO, SearchAccountByKeywordVO, AnalyzeAccountByUrlVO } from '@/types/benchmark';
import type { GridColDef } from '@mui/x-data-grid';

const BENCHMARK_ACCOUNT_READY_ENDPOINTS = [
  '/benchmark/account/list',
  '/benchmark/account/save',
  '/benchmark/account/delete',
  '/benchmark/account/search-by-keyword',
  '/benchmark/account/analyze-by-url',
].join('|');

const BENCHMARK_ACCOUNT_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/account/mock',
  '/benchmark/account/local-list',
  '/benchmark/account/static-account',
  '/benchmark/account/local-search',
  '/benchmark/account/local-url-analyze',
].join('|');

export default function BenchmarkAccountListPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(30);
  const [keyword, setKeyword] = useState('');
  const [minFollowerCount, setMinFollowerCount] = useState<number | undefined>(50000);

  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [searchDialogOpen, setSearchDialogOpen] = useState(false);
  const [urlDialogOpen, setUrlDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [currentAccount, setCurrentAccount] = useState<BenchmarkAccountVO | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['benchmarkAccounts', page, pageSize, keyword, minFollowerCount],
    queryFn: () => benchmarkAccountApi.list({
      page,
      rows: pageSize,
      keyword,
      minFanCount: minFollowerCount,
    }),
  });

  const saveMutation = useMutation({
    mutationFn: benchmarkAccountApi.save,
    onSuccess: () => {
      enqueueSnackbar('保存成功', { variant: 'success' });
      setActionError(null);
      setSaveDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: (err) => {
      setActionError(`账号保存失败：${getErrorMessage(err)}。来源：/benchmark/account/save`);
      enqueueSnackbar('保存失败', { variant: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: benchmarkAccountApi.delete,
    onSuccess: () => {
      enqueueSnackbar('删除成功', { variant: 'success' });
      setActionError(null);
      setDeleteDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: (err) => {
      setActionError(`账号删除失败：${getErrorMessage(err)}。来源：/benchmark/account/delete`);
      enqueueSnackbar('删除失败', { variant: 'error' });
    },
  });

  const searchMutation = useMutation({
    mutationFn: benchmarkAccountApi.searchByKeyword,
    onSuccess: (accounts) => {
      enqueueSnackbar(`搜索到 ${accounts.length} 个账号`, { variant: 'success' });
      setActionError(null);
      setSearchDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: (err) => {
      setActionError(`关键词搜索失败：${getErrorMessage(err)}。来源：/benchmark/account/search-by-keyword；请检查抖音 Cookie、Playwright 与风控状态。`);
      enqueueSnackbar('搜索失败', { variant: 'error' });
    },
  });

  const urlMutation = useMutation({
    mutationFn: benchmarkAccountApi.analyzeByUrl,
    onSuccess: () => {
      enqueueSnackbar('分析成功', { variant: 'success' });
      setActionError(null);
      setUrlDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: (err) => {
      setActionError(`URL 分析失败：${getErrorMessage(err)}。来源：/benchmark/account/analyze-by-url；请确认链接不是 /user/self，且 Cookie 可用。`);
      enqueueSnackbar('分析失败', { variant: 'error' });
    },
  });

  const columns: GridColDef<BenchmarkAccountVO>[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    {
      field: 'accountName',
      headerName: '账号名称',
      width: 200,
      renderCell: (params) => (
        <Stack direction="row" spacing={1} alignItems="center">
          {params.row.avatarUrl && (
            <img src={params.row.avatarUrl} alt="" style={{ width: 32, height: 32, borderRadius: '50%' }} />
          )}
          <span>{params.value}</span>
        </Stack>
      ),
    },
    {
      field: 'fanCount',
      headerName: '粉丝数',
      width: 120,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'videoCount',
      headerName: '视频数',
      width: 100,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'avgLikeCount',
      headerName: '平均点赞',
      width: 120,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'category',
      headerName: '分类',
      width: 160,
      renderCell: (params) => params.value ? (
        <Stack direction="row" spacing={0.5}>
          {String(params.value).split(',').slice(0, 3).map((tag: string, idx: number) => (
            <Chip key={idx} label={tag} size="small" />
          ))}
        </Stack>
      ) : null,
    },
    {
      field: 'lastCollectTime',
      headerName: '最后采集时间',
      width: 180,
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 200,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row" spacing={1}>
          <Button size="small" onClick={() => handleEdit(params.row)}>编辑</Button>
          <Button size="small" onClick={() => handleViewVideos(params.row)}>查看视频</Button>
          <Button size="small" color="error" onClick={() => handleDelete(params.row)}>删除</Button>
        </Stack>
      ),
    },
  ];

  const handleEdit = (account: BenchmarkAccountVO) => {
    setCurrentAccount(account);
    setSaveDialogOpen(true);
  };

  const handleDelete = (account: BenchmarkAccountVO) => {
    setCurrentAccount(account);
    setDeleteDialogOpen(true);
  };

  const handleViewVideos = (account: BenchmarkAccountVO) => {
    navigate(shortvideoBenchmarkVideosPath(account.id));
  };

  const handleSave = (data: BenchmarkAccountSaveVO) => {
    saveMutation.mutate(data);
  };

  const handleSearch = (data: SearchAccountByKeywordVO) => {
    searchMutation.mutate(data);
  };

  const handleAnalyzeUrl = (data: AnalyzeAccountByUrlVO) => {
    urlMutation.mutate(data);
  };

  const rows = data?.list ?? [];
  const neverCollected = rows.filter((item) => !item.lastCollectTime).length;
  const readyAccounts = rows.filter((item) => item.accountUrl && item.secUid).length;
  const highFollowerAccounts = rows.filter((item) => (item.fanCount ?? item.followerCount ?? 0) >= 100000).length;
  const listErrorMessage = error instanceof Error ? error.message : '对标账号列表加载失败，请检查 /benchmark/account/list。';

  return (
    <Box
      data-testid="benchmark-account-list-page"
      data-contract-scope="benchmark-account-server-collection"
      data-ready-endpoints={BENCHMARK_ACCOUNT_READY_ENDPOINTS}
      data-unsupported-endpoints={BENCHMARK_ACCOUNT_UNSUPPORTED_ENDPOINTS}
      data-no-local-account-fallback="true"
      data-no-static-account-fallback="true"
      data-server-pagination="true"
      data-row-count={rows.length}
      data-total-count={data?.total ?? 0}
      data-list-error={isError ? 'true' : 'false'}
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="对标账号管理"
        subtitle="管理竞品账号，沉淀可采集的视频来源；采集能力依赖抖音 Cookie、Playwright 和当前登录用户。"
        actions={
          <Button size="small" variant="outlined" startIcon={<Refresh />} onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        }
      />

      {isError && (
        <Box data-testid="benchmark-account-list-error" data-no-local-account-fallback="true" data-input-retained="true">
          <ErrorAlert title="对标账号加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}
      {actionError && (
        <Box data-testid="benchmark-account-action-error" data-input-retained="true" data-row-retained="true" data-no-local-account-mutation="true">
          <ErrorAlert title="对标账号操作失败" message={actionError} onRetry={() => setActionError(null)} />
        </Box>
      )}

      <Alert severity="info" variant="outlined">
        账号搜索与 URL 分析走真实后端采集链路；如果抖音风控、Cookie 失效或 Playwright 不可用，页面会保留已入库账号并提示采集失败原因。
      </Alert>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">账号总数</Typography>
            <Typography variant="h5">{data?.total ?? rows.length}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">可进入采集链路</Typography>
            <Typography variant="h5">{readyAccounts}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">本页 10 万粉以上</Typography>
            <Typography variant="h5">{highFollowerAccounts}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">未采集</Typography>
            <Typography variant="h5" color={neverCollected > 0 ? 'warning.main' : 'text.primary'}>{neverCollected}</Typography>
          </CardContent>
        </Card>
      </Stack>

      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
        <TextField
          size="small"
          placeholder="搜索账号名称"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 300 }}
        />
        <TextField
          size="small"
          type="number"
          label="最低粉丝数"
          value={minFollowerCount ?? ''}
          onChange={(e) => setMinFollowerCount(e.target.value ? Number(e.target.value) : undefined)}
          sx={{ width: 150 }}
        />
        <Button
          variant="contained"
          startIcon={<Search />}
          onClick={() => setSearchDialogOpen(true)}
        >
          按关键词搜索
        </Button>
        <Button
          variant="contained"
          startIcon={<LinkIcon />}
          onClick={() => setUrlDialogOpen(true)}
        >
          按URL分析
        </Button>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => {
            setCurrentAccount(null);
            setSaveDialogOpen(true);
          }}
        >
          手动添加
        </Button>
      </Stack>

      <StandardDataGrid
        data-testid="benchmark-account-grid-contract"
        data-contract-scope="benchmark-account-grid-server-pagination"
        data-ready-endpoints="/benchmark/account/list"
        data-no-local-account-fallback="true"
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

      {/* 保存对话框 */}
      <FormDialog
        open={saveDialogOpen}
        title={currentAccount ? '编辑账号' : '添加账号'}
        onClose={() => setSaveDialogOpen(false)}
        loading={saveMutation.isPending}
        onConfirm={() => {
          const form = document.getElementById('account-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: BenchmarkAccountSaveVO = {
              id: currentAccount?.id,
              accountName: formData.get('accountName') as string,
              accountUrl: formData.get('accountUrl') as string,
              secUid: formData.get('secUid') as string,
              douyinId: formData.get('douyinId') as string || undefined,
              category: formData.get('category') as string || undefined,
              fanCount: formData.get('fanCount') ? Number(formData.get('fanCount')) : undefined,
              avgViewCount: formData.get('avgViewCount') ? Number(formData.get('avgViewCount')) : undefined,
              avgLikeCount: formData.get('avgLikeCount') ? Number(formData.get('avgLikeCount')) : undefined,
              notes: formData.get('notes') as string || undefined,
              isActive: true,
            };
            handleSave(data);
          }
        }}
      >
        <form id="account-form">
          <Stack spacing={2}>
            <TextField name="accountName" label="账号名称" required defaultValue={currentAccount?.accountName} fullWidth />
            <TextField name="accountUrl" label="账号URL" required defaultValue={currentAccount?.accountUrl} fullWidth />
            <TextField name="secUid" label="sec_uid" defaultValue={currentAccount?.secUid} fullWidth />
            <TextField name="douyinId" label="抖音号" defaultValue={currentAccount?.douyinId} fullWidth />
            <TextField name="category" label="分类" defaultValue={currentAccount?.category ?? currentAccount?.tags} fullWidth />
            <TextField name="fanCount" label="粉丝数" type="number" defaultValue={currentAccount?.fanCount ?? currentAccount?.followerCount} fullWidth />
            <TextField name="avgViewCount" label="平均播放量" type="number" defaultValue={currentAccount?.avgViewCount} fullWidth />
            <TextField name="avgLikeCount" label="平均点赞数" type="number" defaultValue={currentAccount?.avgLikeCount} fullWidth />
            <TextField name="notes" label="备注" multiline rows={3} defaultValue={currentAccount?.notes ?? currentAccount?.description} fullWidth />
          </Stack>
        </form>
      </FormDialog>

      {/* 关键词搜索对话框 */}
      <FormDialog
        open={searchDialogOpen}
        title="按关键词搜索账号"
        onClose={() => setSearchDialogOpen(false)}
        confirmText="开始搜索"
        loading={searchMutation.isPending}
        onConfirm={() => {
          const form = document.getElementById('search-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: SearchAccountByKeywordVO = {
              keyword: formData.get('keyword') as string,
              minFanCount: formData.get('minFanCount') ? Number(formData.get('minFanCount')) : undefined,
              maxResults: formData.get('maxResults') ? Number(formData.get('maxResults')) : undefined,
              cookieId: formData.get('cookieId') ? Number(formData.get('cookieId')) : undefined,
            };
            handleSearch(data);
          }
        }}
      >
        <form id="search-form">
          <Stack spacing={2}>
            <TextField name="keyword" label="搜索关键词" required defaultValue="" fullWidth />
            <TextField name="minFanCount" label="最低粉丝数" type="number" defaultValue={50000} fullWidth />
            <TextField name="maxResults" label="最多结果数" type="number" defaultValue={20} fullWidth />
            <TextField name="cookieId" label="指定 Cookie ID（可选）" type="number" fullWidth />
          </Stack>
        </form>
      </FormDialog>

      {/* URL分析对话框 */}
      <FormDialog
        open={urlDialogOpen}
        title="按URL分析账号"
        onClose={() => setUrlDialogOpen(false)}
        confirmText="开始分析"
        loading={urlMutation.isPending}
        onConfirm={() => {
          const form = document.getElementById('url-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: AnalyzeAccountByUrlVO = {
              accountUrl: formData.get('accountUrl') as string,
              minLikeCount: formData.get('minLikeCount') ? Number(formData.get('minLikeCount')) : undefined,
              maxVideos: formData.get('maxVideos') ? Number(formData.get('maxVideos')) : undefined,
              cookieId: formData.get('cookieId') ? Number(formData.get('cookieId')) : undefined,
              autoAnalyze: formData.get('autoAnalyze') === 'true',
            };
            handleAnalyzeUrl(data);
          }
        }}
      >
        <form id="url-form">
          <Stack spacing={2}>
            <TextField name="accountUrl" label="账号URL" required placeholder="https://www.douyin.com/user/..." fullWidth />
            <TextField name="minLikeCount" label="最低点赞数" type="number" defaultValue={1000} fullWidth />
            <TextField name="maxVideos" label="最多采集数量" type="number" defaultValue={50} fullWidth />
            <TextField name="cookieId" label="指定 Cookie ID（可选）" type="number" fullWidth />
            <label>
              <input type="checkbox" name="autoAnalyze" value="true" defaultChecked />
              {' '}自动分析采集到的视频
            </label>
          </Stack>
        </form>
      </FormDialog>

      {/* 删除确认对话框 */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="确认删除"
        content={`确定要删除账号"${currentAccount?.accountName}"吗？`}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={() => currentAccount && deleteMutation.mutate(currentAccount.id)}
        loading={deleteMutation.isPending}
      />
    </Box>
  );
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '未知错误';
}
