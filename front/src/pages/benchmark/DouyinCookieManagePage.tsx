import { useEffect, useState } from 'react';
import {
  AlertTitle,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Stack,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Add, CheckCircle, Error as ErrorIcon } from '@mui/icons-material';
import RefreshIcon from '@mui/icons-material/Refresh';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { StandardDataGrid, PageHeader, FormDialog, ConfirmDialog, ErrorAlert, DataGridEmptyOverlay } from '@/components/base';
import { douyinCookieApi } from '@/api/benchmark';
import type { DouyinCookieVO, DouyinCookieSaveVO } from '@/types/benchmark';
import type { GridColDef } from '@mui/x-data-grid';

const COOKIE_READY_ENDPOINTS = [
  '/benchmark/cookie/list',
  '/benchmark/cookie/save',
  '/benchmark/cookie/delete',
  '/benchmark/cookie/validate',
  '/benchmark/cookie/qr-login/start',
  '/benchmark/cookie/qr-login/poll',
  '/benchmark/cookie/qr-login/cancel',
].join('|');

const COOKIE_UNSUPPORTED_ENDPOINTS = [
  '/benchmark/cookie/mock',
  '/benchmark/cookie/local-list',
  '/benchmark/cookie/static-cookie',
  '/benchmark/cookie/local-validate',
  '/benchmark/cookie/local-qr-login',
].join('|');

/** 抖音 Web Cookie：供 Playwright 采集等使用；支持服务端打开抖音页扫码登录后写入。 */
export default function DouyinCookieManagePage() {
  const { enqueueSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(30);
  const [keyword, setKeyword] = useState('');
  const [isValid, setIsValid] = useState<boolean | undefined>(undefined);

  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [currentCookie, setCurrentCookie] = useState<DouyinCookieVO | null>(null);

  const [qrOpen, setQrOpen] = useState(false);
  const [qrSessionId, setQrSessionId] = useState<string | null>(null);
  const [qrImageB64, setQrImageB64] = useState<string | null>(null);
  const [qrHint, setQrHint] = useState<string>('');
  const [qrPhase, setQrPhase] = useState<'idle' | 'polling' | 'success' | 'expired'>('idle');
  const [qrPollNote, setQrPollNote] = useState('');
  const [qrCookieName, setQrCookieName] = useState('抖音扫码');
  const [qrCookieValue, setQrCookieValue] = useState('');
  const [actionError, setActionError] = useState('');

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['douyinCookies', page, pageSize, keyword, isValid],
    queryFn: () => douyinCookieApi.list({
      page,
      rows: pageSize,
      keyword,
      isValid,
    }),
  });

  const saveMutation = useMutation({
    mutationFn: douyinCookieApi.save,
    onSuccess: () => {
      setActionError('');
      enqueueSnackbar('保存成功', { variant: 'success' });
      setSaveDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['douyinCookies'] });
    },
    onError: (e) => {
      const message = e instanceof Error ? e.message : '保存失败';
      setActionError(`保存失败：${message}`);
      enqueueSnackbar('保存失败', { variant: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: douyinCookieApi.delete,
    onSuccess: () => {
      setActionError('');
      enqueueSnackbar('删除成功', { variant: 'success' });
      setDeleteDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['douyinCookies'] });
    },
    onError: (e) => {
      const message = e instanceof Error ? e.message : '删除失败';
      setActionError(`删除失败：${message}`);
      enqueueSnackbar('删除失败', { variant: 'error' });
    },
  });

  const validateMutation = useMutation({
    mutationFn: (cookieId: number) => douyinCookieApi.validate({ cookieId }),
    onSuccess: (result) => {
      setActionError('');
      enqueueSnackbar(result ? 'Cookie有效' : 'Cookie已失效', {
        variant: result ? 'success' : 'error',
      });
      queryClient.invalidateQueries({ queryKey: ['douyinCookies'] });
    },
    onError: (e) => {
      const message = e instanceof Error ? e.message : '验证失败';
      setActionError(`验证失败：${message}`);
      enqueueSnackbar('验证失败', { variant: 'error' });
    },
  });

  const resetQrState = () => {
    setQrSessionId(null);
    setQrImageB64(null);
    setQrHint('');
    setQrPhase('idle');
    setQrPollNote('');
    setQrCookieValue('');
    setQrCookieName('抖音扫码');
  };

  const qrStartMutation = useMutation({
    mutationFn: () => douyinCookieApi.qrLoginStart(),
    onSuccess: (res) => {
      setActionError('');
      setQrSessionId(res.sessionId);
      setQrImageB64(res.qrImageBase64);
      setQrHint(res.message ?? '');
      setQrPhase('polling');
      setQrPollNote('等待扫码…');
      setQrCookieValue('');
    },
    onError: (e: Error) => {
      setActionError(`扫码登录启动失败：${e.message || '请确认服务端已安装 Playwright Chromium'}`);
      enqueueSnackbar(e.message || '无法启动扫码（请确认服务端已安装 Playwright Chromium）', { variant: 'error' });
      setQrOpen(false);
      resetQrState();
    },
  });

  useEffect(() => {
    if (!qrOpen || !qrSessionId || qrPhase !== 'polling') {
      return undefined;
    }
    const tick = async () => {
      try {
        const r = await douyinCookieApi.qrLoginPoll(qrSessionId);
        if (r.status === 'success' && r.cookieValue) {
          setQrPhase('success');
          setQrCookieValue(r.cookieValue);
          setQrPollNote(r.message ?? '已获取 Cookie，请填写名称后保存');
        } else if (r.status === 'expired' || r.status === 'error') {
          setQrPhase('expired');
          setQrPollNote(r.message ?? '会话已结束');
        } else {
          setQrPollNote(r.message ?? '等待扫码登录…');
        }
      } catch {
        setQrPollNote('轮询失败，请稍后重试');
      }
    };
    void tick();
    const id = window.setInterval(() => void tick(), 2000);
    return () => window.clearInterval(id);
  }, [qrOpen, qrSessionId, qrPhase]);

  const handleQrDialogClose = () => {
    if (qrSessionId && qrPhase === 'polling') {
      void douyinCookieApi.qrLoginCancel(qrSessionId).catch((e) => console.error('QR cancel failed:', e));
    }
    setQrOpen(false);
    resetQrState();
  };

  const handleSaveQrCookie = () => {
    const name = qrCookieName.trim();
    const val = qrCookieValue.trim();
    if (!name || !val) {
      enqueueSnackbar('请填写 Cookie 名称并确保已获取到 Cookie', { variant: 'warning' });
      return;
    }
    saveMutation.mutate(
      { cookieName: name, cookieValue: val },
      {
        onSuccess: () => {
          handleQrDialogClose();
        },
      },
    );
  };

  const columns: GridColDef<DouyinCookieVO>[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'cookieName', headerName: 'Cookie名称', width: 200 },
    {
      field: 'cookieValue',
      headerName: 'Cookie值',
      width: 300,
      renderCell: (params) => {
        const v = params.value != null ? String(params.value) : '';
        return (
          <span style={{ fontFamily: 'monospace', fontSize: '0.85em' }}>
            {v.length > 50 ? `${v.slice(0, 50)}...` : v || '—'}
          </span>
        );
      },
    },
    {
      field: 'isValid',
      headerName: '状态',
      width: 100,
      renderCell: (params) => (
        <Chip
          icon={params.value ? <CheckCircle /> : <ErrorIcon />}
          label={params.value ? '有效' : '失效'}
          color={params.value ? 'success' : 'error'}
          size="small"
        />
      ),
    },
    {
      field: 'useCount',
      headerName: '使用次数',
      width: 100,
    },
    {
      field: 'lastValidateTime',
      headerName: '最后验证时间',
      width: 180,
    },
    {
      field: 'createTime',
      headerName: '创建时间',
      width: 180,
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 200,
      sortable: false,
      renderCell: (params) => (
        <Stack direction="row" spacing={1}>
          <Button size="small" onClick={() => handleValidate(params.row)}>验证</Button>
          <Button size="small" onClick={() => handleEdit(params.row)}>编辑</Button>
          <Button size="small" color="error" onClick={() => handleDelete(params.row)}>删除</Button>
        </Stack>
      ),
    },
  ];

  const handleEdit = (cookie: DouyinCookieVO) => {
    setCurrentCookie(cookie);
    setSaveDialogOpen(true);
  };

  const handleDelete = (cookie: DouyinCookieVO) => {
    setCurrentCookie(cookie);
    setDeleteDialogOpen(true);
  };

  const handleValidate = (cookie: DouyinCookieVO) => {
    validateMutation.mutate(cookie.id);
  };

  const handleSave = (data: DouyinCookieSaveVO) => {
    saveMutation.mutate(data);
  };

  const rows = data?.list ?? [];
  const total = data?.total ?? rows.length;
  const validCount = rows.filter((item) => item.isValid).length;
  const invalidCount = rows.filter((item) => !item.isValid).length;
  const usedCount = rows.filter((item) => (item.useCount ?? 0) > 0).length;
  const listErrorMessage = error instanceof Error ? error.message : 'Cookie 列表加载失败，请检查 /benchmark/cookie/list。';

  return (
    <Box
      data-testid="douyin-cookie-page"
      data-contract-scope="benchmark-douyin-cookie-server-browser-session"
      data-ready-endpoints={COOKIE_READY_ENDPOINTS}
      data-unsupported-endpoints={COOKIE_UNSUPPORTED_ENDPOINTS}
      data-no-local-cookie-fallback="true"
      data-no-static-cookie-fallback="true"
      data-no-local-qr-fallback="true"
      data-server-pagination="true"
      data-row-count={rows.length}
      data-total-count={total}
      data-list-error={isError ? 'true' : 'false'}
      sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 48px - 32px)' }}
    >
      <PageHeader
        title="抖音 Cookie 管理"
        subtitle="扫码登录：服务器会打开抖音网页并尽量点击「登录」露出扫码框再截图；请用抖音 App 扫图中的登录码并在手机上确认。若仍无反应，多半是页面未弹出登录二维码（可让运维将 app.douyin-cookie.qr-headless 设为 false 排查），或直接本机登录抖音后手动复制 Cookie。"
        actions={
          <Button size="small" variant="outlined" startIcon={<RefreshIcon />} onClick={() => void refetch()} disabled={isFetching}>
            刷新
          </Button>
        }
      />

      {isError && (
        <Box data-testid="douyin-cookie-list-error" data-no-local-cookie-fallback="true" data-input-retained="true">
          <ErrorAlert title="Cookie 列表加载失败" message={listErrorMessage} onRetry={() => void refetch()} />
        </Box>
      )}

      {actionError && (
        <Alert
          data-testid="douyin-cookie-action-error"
          data-input-retained="true"
          data-row-retained="true"
          data-no-local-cookie-mutation="true"
          severity="error"
          onClose={() => setActionError('')}
        >
          {actionError}
        </Alert>
      )}

      <Alert severity={invalidCount > 0 ? 'warning' : 'info'} variant="outlined">
        <AlertTitle>采集链路依赖说明</AlertTitle>
        Cookie 供对标账号搜索、对标视频采集和抖音网页解析使用；失效时不会阻断已入库数据浏览，但采集/分析会降级或失败。
      </Alert>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">Cookie 总数</Typography>
            <Typography variant="h5">{total}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">有效</Typography>
            <Typography variant="h5" color="success.main">{validCount}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">失效</Typography>
            <Typography variant="h5" color={invalidCount > 0 ? 'error.main' : 'text.primary'}>{invalidCount}</Typography>
          </CardContent>
        </Card>
        <Card variant="outlined" sx={{ flex: 1 }}>
          <CardContent>
            <Typography variant="caption" color="text.secondary">已被采集使用</Typography>
            <Typography variant="h5">{usedCount}</Typography>
          </CardContent>
        </Card>
      </Stack>

      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
        <TextField
          size="small"
          placeholder="搜索Cookie名称"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          sx={{ width: 300 }}
        />
        <Button
          variant={isValid === true ? 'contained' : 'outlined'}
          onClick={() => setIsValid(isValid === true ? undefined : true)}
        >
          有效
        </Button>
        <Button
          variant={isValid === false ? 'contained' : 'outlined'}
          onClick={() => setIsValid(isValid === false ? undefined : false)}
        >
          失效
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button
          variant="outlined"
          startIcon={<OpenInNewIcon />}
          onClick={() => window.open('https://www.douyin.com', '_blank', 'noopener,noreferrer')}
        >
          本机打开抖音
        </Button>
        <Button
          variant="contained"
          color="secondary"
          startIcon={<QrCode2Icon />}
          disabled={qrStartMutation.isPending}
          onClick={() => {
            resetQrState();
            setQrOpen(true);
            qrStartMutation.mutate();
          }}
        >
          扫码登录获取 Cookie
        </Button>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => {
            setCurrentCookie(null);
            setSaveDialogOpen(true);
          }}
        >
          手动添加
        </Button>
      </Stack>

      <StandardDataGrid
        data-testid="douyin-cookie-grid-contract"
        data-contract-scope="benchmark-douyin-cookie-grid"
        data-ready-endpoints="/benchmark/cookie/list"
        data-no-local-cookie-fallback="true"
        data-server-pagination="true"
        data-row-count={rows.length}
        rows={rows}
        columns={columns}
        loading={isFetching}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={(model: { page: number; pageSize: number }) => {
          setPage(model.page);
          setPageSize(model.pageSize);
        }}
        slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        sx={{ flex: 1 }}
      />

      <Dialog
        open={qrOpen}
        onClose={handleQrDialogClose}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          'data-testid': 'douyin-cookie-qr-dialog',
          'data-contract-scope': 'server-playwright-qr-login',
          'data-ready-endpoints': '/benchmark/cookie/qr-login/start|/benchmark/cookie/qr-login/poll|/benchmark/cookie/qr-login/cancel|/benchmark/cookie/save',
          'data-no-local-qr-fallback': 'true',
          'data-input-retained': 'true',
        }}
      >
        <DialogTitle>抖音扫码登录</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {qrHint && (
              <Alert severity="info" variant="outlined">{qrHint}</Alert>
            )}
            {(qrStartMutation.isPending || (qrOpen && !qrImageB64 && qrPhase !== 'success' && qrPhase !== 'expired')) && (
              <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}>
                <CircularProgress />
                <Typography sx={{ ml: 2 }} color="text.secondary">正在打开抖音页面…</Typography>
              </Box>
            )}
            {!qrStartMutation.isPending && qrImageB64 && qrPhase !== 'success' && (
              <Box sx={{ textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  请使用抖音 App 扫描图中二维码（页面由服务器浏览器渲染）
                </Typography>
                <Box
                  component="img"
                  alt="抖音登录页截图"
                  src={`data:image/png;base64,${qrImageB64}`}
                  sx={{ maxWidth: '100%', borderRadius: 1, border: 1, borderColor: 'divider' }}
                />
                <Alert severity="info" sx={{ mt: 1, textAlign: 'left' }}>
                  {qrPollNote || '正在连接服务器轮询登录状态…'}
                </Alert>
                {qrPhase === 'expired' && (
                  <Button
                    sx={{ mt: 2 }}
                    variant="outlined"
                    onClick={() => {
                      if (qrSessionId) void douyinCookieApi.qrLoginCancel(qrSessionId);
                      resetQrState();
                      setQrOpen(true);
                      qrStartMutation.mutate();
                    }}
                  >
                    重新获取
                  </Button>
                )}
              </Box>
            )}
            {qrPhase === 'success' && (
              <Stack spacing={2}>
                <Alert severity="success">{qrPollNote}</Alert>
                <TextField
                  label="Cookie 名称"
                  value={qrCookieName}
                  onChange={(e) => setQrCookieName(e.target.value)}
                  fullWidth
                  size="small"
                />
                <TextField
                  label="Cookie 请求头（整串）"
                  value={qrCookieValue}
                  onChange={(e) => setQrCookieValue(e.target.value)}
                  multiline
                  minRows={4}
                  fullWidth
                  size="small"
                  helperText="与浏览器 DevTools 里「Cookie」一致：多条 name=value 用分号连接，不是单个值"
                />
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleQrDialogClose}>关闭</Button>
          {qrPhase === 'success' && (
            <Button variant="contained" onClick={handleSaveQrCookie} disabled={saveMutation.isPending}>
              保存到系统
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <FormDialog
        open={saveDialogOpen}
        title={currentCookie ? '编辑Cookie' : '添加Cookie'}
        onClose={() => setSaveDialogOpen(false)}
        loading={saveMutation.isPending}
        onConfirm={() => {
          const form = document.getElementById('cookie-form') as HTMLFormElement;
          if (form) {
            const formData = new FormData(form);
            const data: DouyinCookieSaveVO = {
              id: currentCookie?.id,
              cookieName: formData.get('cookieName') as string,
              cookieValue: formData.get('cookieValue') as string,
            };
            handleSave(data);
          }
        }}
      >
        <form id="cookie-form">
          <Stack spacing={2}>
            <TextField name="cookieName" label="Cookie名称" required defaultValue={currentCookie?.cookieName} fullWidth />
            <TextField
              name="cookieValue"
              label="Cookie 请求头（整串）"
              required
              multiline
              rows={6}
              placeholder="粘贴完整 Cookie 串：name1=…; name2=…（扫码获取的也是整串）"
              defaultValue={currentCookie?.cookieValue}
              fullWidth
              helperText="存的是整条 Cookie 头，包含该会话下多枚 Cookie，不是单个键"
            />
          </Stack>
        </form>
      </FormDialog>

      <ConfirmDialog
        open={deleteDialogOpen}
        title="确认删除"
        content={`确定要删除Cookie"${currentCookie?.cookieName}"吗？`}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={() => currentCookie && deleteMutation.mutate(currentCookie.id)}
        loading={deleteMutation.isPending}
      />
    </Box>
  );
}
