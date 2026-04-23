import { useState } from 'react';
import { Box, Button, Chip, Stack, TextField } from '@mui/material';
import { Add, Search, Link as LinkIcon } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { StandardDataGrid } from '@/components/base/StandardDataGrid';
import { PageHeader } from '@/components/base/PageHeader';
import { FormDialog } from '@/components/base/FormDialog';
import { ConfirmDialog } from '@/components/base/ConfirmDialog';
import { benchmarkAccountApi } from '@/api/benchmark';
import type { BenchmarkAccountVO, BenchmarkAccountSaveVO, SearchAccountByKeywordVO, AnalyzeAccountByUrlVO } from '@/types/benchmark';
import type { GridColDef } from '@mui/x-data-grid';

export default function BenchmarkAccountListPage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(30);
  const [keyword, setKeyword] = useState('');
  const [minFollowerCount, setMinFollowerCount] = useState<number | undefined>(50000);

  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [searchDialogOpen, setSearchDialogOpen] = useState(false);
  const [urlDialogOpen, setUrlDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [currentAccount, setCurrentAccount] = useState<BenchmarkAccountVO | null>(null);

  // 查询列表
  const { data, isLoading } = useQuery({
    queryKey: ['benchmarkAccounts', page, pageSize, keyword, minFollowerCount],
    queryFn: () => benchmarkAccountApi.list({
      page,
      rows: pageSize,
      keyword,
      minFollowerCount,
    }),
  });

  // 保存
  const saveMutation = useMutation({
    mutationFn: benchmarkAccountApi.save,
    onSuccess: () => {
      enqueueSnackbar('保存成功', { variant: 'success' });
      setSaveDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: () => {
      enqueueSnackbar('保存失败', { variant: 'error' });
    },
  });

  // 删除
  const deleteMutation = useMutation({
    mutationFn: benchmarkAccountApi.delete,
    onSuccess: () => {
      enqueueSnackbar('删除成功', { variant: 'success' });
      setDeleteDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: () => {
      enqueueSnackbar('删除失败', { variant: 'error' });
    },
  });

  // 按关键词搜索
  const searchMutation = useMutation({
    mutationFn: benchmarkAccountApi.searchByKeyword,
    onSuccess: (accounts) => {
      enqueueSnackbar(`搜索到 ${accounts.length} 个账号`, { variant: 'success' });
      setSearchDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: () => {
      enqueueSnackbar('搜索失败', { variant: 'error' });
    },
  });

  // 按URL分析
  const urlMutation = useMutation({
    mutationFn: benchmarkAccountApi.analyzeByUrl,
    onSuccess: () => {
      enqueueSnackbar('分析成功', { variant: 'success' });
      setUrlDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['benchmarkAccounts'] });
    },
    onError: () => {
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
      field: 'followerCount',
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
      field: 'likeCount',
      headerName: '获赞数',
      width: 120,
      valueFormatter: (value: number | undefined) => value ? value.toLocaleString() : '-',
    },
    {
      field: 'tags',
      headerName: '标签',
      width: 200,
      renderCell: (params) => params.value ? (
        <Stack direction="row" spacing={0.5}>
          {params.value.split(',').slice(0, 3).map((tag: string, idx: number) => (
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
    window.location.href = `/benchmark/videos?accountId=${account.id}`;
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

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="对标账号管理"
        subtitle="管理竞品账号，采集高质量视频进行深度分析"
      />

      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
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

      {/* 保存对话框 */}
      <FormDialog
        open={saveDialogOpen}
        title={currentAccount ? '编辑账号' : '添加账号'}
        onClose={() => setSaveDialogOpen(false)}
        onConfirm={() => {
          const form = document.getElementById('account-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: BenchmarkAccountSaveVO = {
              id: currentAccount?.id,
              accountName: formData.get('accountName') as string,
              accountUrl: formData.get('accountUrl') as string,
              secUid: formData.get('secUid') as string,
              avatarUrl: formData.get('avatarUrl') as string || undefined,
              followerCount: formData.get('followerCount') ? Number(formData.get('followerCount')) : undefined,
              description: formData.get('description') as string || undefined,
              tags: formData.get('tags') as string || undefined,
            };
            handleSave(data);
          }
        }}
      >
        <form id="account-form">
          <Stack spacing={2}>
            <TextField name="accountName" label="账号名称" required defaultValue={currentAccount?.accountName} fullWidth />
            <TextField name="accountUrl" label="账号URL" required defaultValue={currentAccount?.accountUrl} fullWidth />
            <TextField name="secUid" label="sec_uid" required defaultValue={currentAccount?.secUid} fullWidth />
            <TextField name="avatarUrl" label="头像URL" defaultValue={currentAccount?.avatarUrl} fullWidth />
            <TextField name="followerCount" label="粉丝数" type="number" defaultValue={currentAccount?.followerCount} fullWidth />
            <TextField name="description" label="简介" multiline rows={3} defaultValue={currentAccount?.description} fullWidth />
            <TextField name="tags" label="标签（逗号分隔）" defaultValue={currentAccount?.tags} fullWidth />
          </Stack>
        </form>
      </FormDialog>

      {/* 关键词搜索对话框 */}
      <FormDialog
        open={searchDialogOpen}
        title="按关键词搜索账号"
        onClose={() => setSearchDialogOpen(false)}
        onConfirm={() => {
          const form = document.getElementById('search-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: SearchAccountByKeywordVO = {
              keyword: formData.get('keyword') as string,
              minFollowerCount: formData.get('minFollowerCount') ? Number(formData.get('minFollowerCount')) : undefined,
              maxResults: formData.get('maxResults') ? Number(formData.get('maxResults')) : undefined,
            };
            handleSearch(data);
          }
        }}
      >
        <form id="search-form">
          <Stack spacing={2}>
            <TextField name="keyword" label="搜索关键词" required defaultValue="" fullWidth />
            <TextField name="minFollowerCount" label="最低粉丝数" type="number" defaultValue={50000} fullWidth />
            <TextField name="maxResults" label="最多结果数" type="number" defaultValue={20} fullWidth />
          </Stack>
        </form>
      </FormDialog>

      {/* URL分析对话框 */}
      <FormDialog
        open={urlDialogOpen}
        title="按URL分析账号"
        onClose={() => setUrlDialogOpen(false)}
        onConfirm={() => {
          const form = document.getElementById('url-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: AnalyzeAccountByUrlVO = {
              accountUrl: formData.get('accountUrl') as string,
            };
            handleAnalyzeUrl(data);
          }
        }}
      >
        <form id="url-form">
          <TextField name="accountUrl" label="账号URL" required placeholder="https://www.douyin.com/user/..." fullWidth />
        </form>
      </FormDialog>

      {/* 删除确认对话框 */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="确认删除"
        content={`确定要删除账号"${currentAccount?.accountName}"吗？`}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={() => currentAccount && deleteMutation.mutate(currentAccount.id)}
      />
    </Box>
  );
}
