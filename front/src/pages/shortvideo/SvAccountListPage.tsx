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
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import DeleteIcon from '@mui/icons-material/Delete'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, ConfirmDialog, PageHeader } from '@/components/base'
import { accountList, accountDelete, type SvAccount } from '@/api/sv-account'
import { shortvideoRoutes, shortvideoAccountDetailPath } from '@/constants/shortvideoRoutes'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

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

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['sv-accounts', search],
    queryFn: () => accountList(search),
  })

  const list = data?.list ?? []

  const deleteMut = useMutation({
    mutationFn: (id: number) => accountDelete(id),
    onSuccess: () => {
      toast('账号已删除', 'success')
      setDeleteId(null)
      void qc.invalidateQueries({ queryKey: ['sv-accounts'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

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
            >
              <VideoLibraryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="账号资料、编辑标签与综合分析">
            <IconButton
              size="small"
              onClick={() => navigate(shortvideoAccountDetailPath(row.id, 'info'))}
              aria-label="account-detail"
            >
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="删除账号记录（任务与库内视频不删）">
            <IconButton size="small" color="error" onClick={() => setDeleteId(row.id)} aria-label="delete-account">
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title="短视频账号"
        subtitle="按账号管理其下已采集入库的短视频：从列表进入「采集短视频」可筛选、发起深度拆解并跳转爆款库；「账号资料」查看编辑与综合分析。"
      />

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }} action={
          <Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>
        }>
          加载失败：{error instanceof Error ? error.message : String(error)}
        </Alert>
      )}

      {!isFetching && !isError && (data?.total ?? 0) === 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          当前还没有账号数据。请打开{' '}
          <Button size="small" variant="outlined" onClick={() => navigate(shortvideoRoutes.collect)}>
            账号视频采集
          </Button>
          ，使用「智能识别」粘贴<strong>抖音主页链接或抖音号</strong>（非仅关键词搜索），采集成功后会自动在此创建账号。有数据后请点击操作列中的<strong>视频库图标</strong>或「已入库短视频」数字，进入该账号下的短视频管理。
        </Alert>
      )}

      {/* 搜索栏 */}
      <Box sx={{ mb: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
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
        >
          刷新
        </Button>
      </Box>

      {/* 数据表格 */}
      <StandardDataGrid
        rows={list}
        columns={columns}
        loading={isFetching}
        paginationMode="server"
        rowCount={Number(data?.total ?? 0)}
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
      />

      {/* 删除确认 */}
      <ConfirmDialog
        open={deleteId !== null}
        title="删除账号"
        content="确定要删除这个账号吗？删除后该账号的采集任务和视频不会被删除。"
        onConfirm={() => deleteId && deleteMut.mutate(deleteId)}
        onClose={() => setDeleteId(null)}
        loading={deleteMut.isPending}
      />
    </Box>
  )
}
