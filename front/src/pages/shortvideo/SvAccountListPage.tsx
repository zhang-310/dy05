import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  TextField,
  Button,
  Chip,
  Typography,
  MenuItem,
  Stack,
  Avatar,
  Tooltip,
  IconButton,
  Alert,
  Grid,
  Card,
  CardContent,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import DeleteIcon from '@mui/icons-material/Delete'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader, DataGridEmptyOverlay } from '@/components/base'
import { accountList, accountDelete, type SvAccount } from '@/api/sv-account'
import { shortvideoRoutes, shortvideoAccountDetailPath } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getErrorMessage } from '@/utils/errorHandler'

const SOURCE_TYPE_LABEL: Record<string, string> = {
  manual: '手动添加',
  keyword_search: '关键词采集',
  recommend: '推荐',
  import: '导入',
}

const STATUS_VISUAL: Record<string, { label: string; color: 'default' | 'success' | 'warning' | 'error' }> = {
  active: { label: '活跃', color: 'success' },
  archived: { label: '归档', color: 'default' },
  blocked: { label: '屏蔽', color: 'error' },
}

const ACCOUNT_ENDPOINTS = {
  list: '/short-video/account/list',
  delete: '/short-video/account/delete',
} as const
const ACCOUNT_READY_ENDPOINTS = [
  ACCOUNT_ENDPOINTS.list,
  ACCOUNT_ENDPOINTS.delete,
].join('|')
const ACCOUNT_READY_ROUTES = [
  shortvideoRoutes.collect,
  '/admin/shortvideo/accounts/:id?tab=videos',
  '/admin/shortvideo/accounts/:id',
].join('|')
const ACCOUNT_SUPPORTED_ACTIONS = [
  'server-filter-account-list',
  'navigate-account-videos',
  'navigate-account-info',
  'logical-delete-account',
  'navigate-account-collect',
].join('|')
const ACCOUNT_UNSUPPORTED_ENDPOINTS = [
  '/short-video/account/mock',
  '/short-video/account/local-list',
  '/short-video/account/local-delete',
  '/short-video/account/physical-delete',
  '/short-video/account/delete-videos',
  '/short-video/account/browser-scrape',
  '/short-video/account/export',
  '/short-video/account/get',
  '/short-video/account/videos',
  '/short-video/account/analytics',
].join('|')

function formatNumber(num: number | null | undefined): string {
  if (num == null) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return num.toString()
}

export default function SvAccountListPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()

  const [search, setSearch] = useState({
    keyword: '',
    accountCategory: '',
    sourceType: '',
    status: '',
    page: 0,
    rows: 20,
    sortName: 'updateTime',
    sortOrder: 'desc',
  })

  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sv-accounts', search],
    queryFn: () => accountList(search),
  })

  const list = data?.list ?? []

  const deleteMut = useMutation({
    mutationFn: (id: number) => accountDelete(id),
    onSuccess: () => {
      toast('账号已删除', 'success')
      setDeleteError(null)
      setDeleteId(null)
      void qc.invalidateQueries({ queryKey: ['sv-accounts'] })
    },
    onError: (e) => {
      const message = getErrorMessage(e)
      setDeleteError(message)
      toast(`删除失败：${message}`, 'error')
    },
  })

  const total = Number(data?.total ?? 0)
  const activeCount = list.filter(row => row.status === 'active').length
  const collectedVideoCount = list.reduce((sum, row) => sum + Number(row.totalCollectedVideos ?? 0), 0)
  const avgViralScore = list.length > 0
    ? list.reduce((sum, row) => sum + Number(row.avgViralScore ?? 0), 0) / list.length
    : 0

  const columns: GridColDef<SvAccount>[] = [
    { field: 'id', headerName: 'ID', width: 72 },
    {
      field: 'account',
      headerName: '账号',
      flex: 1,
      minWidth: 200,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ py: 0.5 }}>
          <Avatar src={row.avatarUrl} sx={{ width: 40, height: 40 }}>
            {row.nickname?.charAt(0) || '?'}
          </Avatar>
          <Box sx={{ overflow: 'hidden' }}>
            <Typography variant="body2" noWrap fontWeight={500}>
              {row.nickname || row.douyinId || row.secUid.slice(0, 12)}
            </Typography>
            {row.signature && (
              <Typography variant="caption" color="text.secondary" noWrap display="block">
                {row.signature.slice(0, 30)}
              </Typography>
            )}
          </Box>
        </Stack>
      ),
    },
    {
      field: 'followerCount',
      headerName: '粉丝数',
      width: 100,
      renderCell: ({ value }) => (
        <Typography variant="body2">{formatNumber(value as number)}</Typography>
      ),
    },
    {
      field: 'totalCollectedVideos',
      headerName: '已入库短视频',
      width: 118,
      sortable: false,
      renderCell: ({ row, value }) => {
        const n = Number(value) || 0
        return (
          <Tooltip title="点击进入该账号下的采集短视频列表（筛选、深度拆解、跳转爆款库）">
            <Button
              size="small"
              variant="text"
              sx={{ minWidth: 40, fontWeight: 600 }}
              onClick={() => navigate(shortvideoAccountDetailPath(row.id, 'videos'))}
              data-testid="sv-account-list-videos-count-button"
              data-target-route={shortvideoAccountDetailPath(row.id, 'videos')}
              data-no-account-detail-prefetch="true"
            >
              {n}
            </Button>
          </Tooltip>
        )
      },
    },
    {
      field: 'avgViralScore',
      headerName: '平均评分',
      width: 100,
      renderCell: ({ value }) => {
        const score = Number(value) || 0
        const color = score >= 80 ? 'success' : score >= 60 ? 'warning' : 'default'
        return <Chip label={score.toFixed(1)} color={color} size="small" />
      },
    },
    {
      field: 'collectCount',
      headerName: '采集次数',
      width: 100,
      renderCell: ({ value }) => (
        <Typography variant="body2">{value || 0}</Typography>
      ),
    },
    {
      field: 'sourceType',
      headerName: '来源',
      width: 110,
      renderCell: ({ value, row }) => (
        <Tooltip title={row.sourceKeyword || ''} placement="top">
          <Chip
            label={SOURCE_TYPE_LABEL[value as string] || value}
            size="small"
            variant="outlined"
          />
        </Tooltip>
      ),
    },
    {
      field: 'status',
      headerName: '状态',
      width: 90,
      renderCell: ({ value }) => {
        const s = STATUS_VISUAL[value as string] || { label: value, color: 'default' as const }
        return <Chip label={s.label} color={s.color} size="small" />
      },
    },
    {
      field: 'actions',
      headerName: '操作',
      width: 168,
      sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="管理该账号下的短视频：列表、筛选、批量/单条深度拆解、打开爆款库">
            <IconButton
              size="small"
              color="primary"
              onClick={() => navigate(shortvideoAccountDetailPath(row.id, 'videos'))}
              aria-label="manage-account-videos"
              data-testid="sv-account-list-manage-videos-button"
              data-target-route={shortvideoAccountDetailPath(row.id, 'videos')}
              data-no-account-detail-prefetch="true"
            >
              <VideoLibraryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="账号资料、编辑标签与综合分析">
            <IconButton
              size="small"
              onClick={() => navigate(shortvideoAccountDetailPath(row.id, 'info'))}
              aria-label="account-detail"
              data-testid="sv-account-list-detail-button"
              data-target-route={shortvideoAccountDetailPath(row.id, 'info')}
              data-no-account-detail-prefetch="true"
            >
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="删除账号记录（任务与库内视频不删）">
            <IconButton
              size="small"
              color="error"
              onClick={() => {
                setDeleteError(null)
                setDeleteId(row.id)
              }}
              aria-label="delete-account"
              data-testid="sv-account-list-delete-button"
              data-source-endpoint={ACCOUNT_ENDPOINTS.delete}
              data-logical-delete-only="true"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <Box
      data-testid="sv-account-list-page"
      data-ready-endpoints={ACCOUNT_READY_ENDPOINTS}
      data-ready-routes={ACCOUNT_READY_ROUTES}
      data-supported-actions={ACCOUNT_SUPPORTED_ACTIONS}
      data-unsupported-endpoints={ACCOUNT_UNSUPPORTED_ENDPOINTS}
      data-no-local-account-fallback="true"
      data-logical-delete-only="true"
      data-list-page-only="true"
    >
      <PageHeader
        title="短视频账号"
        subtitle="按账号管理其下已采集入库的短视频：从列表进入「采集短视频」可筛选、发起深度拆解并跳转爆款库；「账号资料」查看编辑与综合分析。"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => void refetch()}
            disabled={isFetching}
            data-testid="sv-account-list-header-refresh-button"
            data-source-endpoint={ACCOUNT_ENDPOINTS.list}
          >
            刷新
          </Button>
        }
      />

      <Alert
        severity="info"
        variant="outlined"
        data-testid="sv-account-list-boundary-contract"
        data-no-local-account-fallback="true"
        data-logical-delete-only="true"
        data-no-account-detail-prefetch="true"
        data-supported-actions={ACCOUNT_SUPPORTED_ACTIONS}
        sx={{ mb: 2 }}
      >
        列表、详情、视频列表、综合分析与删除均对齐 `/short-video/account/*`。账号来源主要来自账号视频采集任务，删除账号为逻辑删除，不会删除已入库爆款视频。
      </Alert>

      <Grid
        container
        spacing={2}
        data-testid="sv-account-list-diagnostics"
        data-current-page-only="true"
        data-no-client-total-synthesis="true"
        sx={{ mb: 2 }}
      >
        {[
          ['账号总数', total],
          ['当前页活跃', activeCount],
          ['当前页已入库视频', collectedVideoCount],
          ['当前页平均评分', avgViralScore.toFixed(1)],
        ].map(([label, value]) => (
          <Grid item xs={6} md={3} key={String(label)}>
            <Card variant="outlined">
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight={700}>{value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {isError && (
        <Alert
          severity="error"
          data-testid="sv-account-list-error"
          data-no-local-account-fallback="true"
          sx={{ mb: 2 }}
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>
          }
        >
          短视频账号加载失败（POST {ACCOUNT_ENDPOINTS.list}）：{getErrorMessage(error)}。请检查账号采集入库链路；页面不会补本地账号。
        </Alert>
      )}

      {deleteError && (
        <Alert
          severity="error"
          data-testid="sv-account-delete-error"
          data-no-local-delete-mutation="true"
          data-logical-delete-only="true"
          sx={{ mb: 2 }}
        >
          删除账号失败（POST {ACCOUNT_ENDPOINTS.delete}）：{deleteError}。当前账号行已保留，可重新确认或取消操作。
        </Alert>
      )}

      {!isFetching && !isError && (data?.total ?? 0) === 0 && (
        <Alert
          severity="info"
          data-testid="sv-account-list-empty"
          data-no-local-account-fallback="true"
          data-no-browser-direct-scrape="true"
          sx={{ mb: 2 }}
        >
          当前还没有账号数据。请打开{' '}
          <Button
            size="small"
            variant="outlined"
            onClick={() => navigate(shortvideoRoutes.collect)}
            data-testid="sv-account-list-open-collect-button"
            data-target-route={shortvideoRoutes.collect}
            data-no-browser-direct-scrape="true"
          >
            账号视频采集
          </Button>
          ，使用「智能识别」粘贴<strong>抖音主页链接或抖音号</strong>（非仅关键词搜索），采集成功后会自动在此创建账号。有数据后请点击操作列中的<strong>视频库图标</strong>或「已入库短视频」数字，进入该账号下的短视频管理。
        </Alert>
      )}

      {/* 搜索栏 */}
      <Box
        data-testid="sv-account-list-filter-contract"
        data-server-filter-payload="true"
        sx={{ mb: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}
      >
        <TextField
          size="small"
          placeholder="搜索昵称、抖音号、sec_uid"
          value={search.keyword}
          onChange={(e) => setSearch((s) => ({ ...s, keyword: e.target.value, page: 0 }))}
          sx={{ width: 280 }}
        />

        <TextField
          select
          size="small"
          label="来源类型"
          value={search.sourceType}
          onChange={(e) => setSearch((s) => ({ ...s, sourceType: e.target.value, page: 0 }))}
          sx={{ width: 140 }}
        >
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="manual">手动添加</MenuItem>
          <MenuItem value="keyword_search">关键词采集</MenuItem>
          <MenuItem value="recommend">推荐</MenuItem>
        </TextField>

        <TextField
          select
          size="small"
          label="状态"
          value={search.status}
          onChange={(e) => setSearch((s) => ({ ...s, status: e.target.value, page: 0 }))}
          sx={{ width: 120 }}
        >
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="active">活跃</MenuItem>
          <MenuItem value="archived">归档</MenuItem>
          <MenuItem value="blocked">屏蔽</MenuItem>
        </TextField>

        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => void refetch()}
          disabled={isFetching}
          data-testid="sv-account-list-filter-refresh-button"
          data-source-endpoint={ACCOUNT_ENDPOINTS.list}
        >
          刷新
        </Button>
      </Box>

      {/* 数据表格 */}
      <Box
        data-testid="sv-account-list-grid-contract"
        data-no-local-account-fallback="true"
        data-no-local-delete-mutation="true"
        data-no-account-detail-prefetch="true"
      >
        <StandardDataGrid
          rows={list}
          columns={columns}
          loading={isFetching}
          paginationMode="server"
          rowCount={total}
          paginationModel={{ page: search.page, pageSize: search.rows }}
          onPaginationModelChange={(model) =>
            setSearch((s) => ({ ...s, page: model.page, rows: model.pageSize }))
          }
          sortModel={[{ field: search.sortName, sort: search.sortOrder as 'asc' | 'desc' }]}
          onSortModelChange={(model) => {
            if (model.length > 0) {
              setSearch((s) => ({
                ...s,
                sortName: model[0].field,
                sortOrder: model[0].sort || 'desc',
              }))
            }
          }}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>

      {/* 删除确认 */}
      <ConfirmDialog
        open={deleteId !== null}
        title="删除账号"
        content={
          deleteError
            ? `确定要删除这个账号吗？删除后该账号的采集任务和视频不会被删除。上次删除失败（POST ${ACCOUNT_ENDPOINTS.delete}）：${deleteError}`
            : '确定要删除这个账号吗？删除后该账号的采集任务和视频不会被删除。'
        }
        onConfirm={() => deleteId && deleteMut.mutate(deleteId)}
        onClose={() => {
          setDeleteId(null)
          setDeleteError(null)
        }}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
