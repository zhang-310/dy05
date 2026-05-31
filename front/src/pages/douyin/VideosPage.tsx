import { useState } from 'react'
import { Alert, Box, Button, Chip, Grid, MenuItem, Paper, TextField, Typography } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { useQuery } from '@tanstack/react-query'
import { PageHeader, StandardDataGrid, DataGridEmptyOverlay } from '@/components/base'
import { douyinApi, type DyVideo, type DyVideoQuery } from '@/api/douyin'
import { formatDate } from '@/utils/date'

const VIDEO_ROUTE = '/talent/douyin/videos'
const VIDEO_SEARCH_ENDPOINT = '/douyin/video/search'
const VIDEO_READY_ENDPOINTS = [VIDEO_SEARCH_ENDPOINT].join('|')
const VIDEO_UNSUPPORTED_ENDPOINTS = [
  '/douyin/video/mock',
  '/douyin/video/local-search',
  '/douyin/video/local-rank',
  '/douyin/video/local-cover',
  '/douyin/video/browser-scrape',
  '/douyin/video/get',
  '/douyin/video/save',
  '/douyin/video/sync',
  '/douyin/account/get',
  '/douyin/account/local-list',
  '/douyin/aweme/direct-fetch',
].join('|')

function fmt(n: unknown): string {
  const num = Number(n ?? 0)
  if (!Number.isFinite(num)) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return String(num)
}

function getVideoTypeLabel(type?: string) {
  if (!type) return '未分类'
  const map: Record<string, string> = {
    normal: '普通视频',
    product: '带货视频',
    live: '直播切片',
    short: '短视频',
  }
  return map[type] ?? type
}

export default function VideosPage() {
  const [search, setSearch] = useState<DyVideoQuery>({ page: 0, rows: 20 })
  const [query, setQuery] = useState<DyVideoQuery>(search)

  const { data, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['dy-videos', search],
    queryFn: () => douyinApi.videoSearch(search),
  })

  const rows = data?.list ?? []
  const totalViews = rows.reduce((sum, row) => sum + Number(row.viewCount ?? 0), 0)
  const totalLikes = rows.reduce((sum, row) => sum + Number(row.likeCount ?? 0), 0)
  const hotCount = rows.filter(row => Number(row.likeCount ?? 0) >= 50000 || Number(row.viewCount ?? 0) >= 100000).length
  const noAccountFilter = search.accountId == null
  const filterContext = `route=${VIDEO_ROUTE}; accountId=${search.accountId ?? '空'}; title=${search.title ?? '空'}; videoType=${search.videoType ?? '全部'}; page=${search.page ?? 0}; rows=${search.rows ?? 20}`
  const errorMessage = error instanceof Error
    ? `${VIDEO_SEARCH_ENDPOINT} 视频列表加载失败：${error.message}（${filterContext}）`
    : `${VIDEO_SEARCH_ENDPOINT} 视频列表接口异常（${filterContext}）`

  const columns: GridColDef<DyVideo>[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    {
      field: 'title',
      headerName: '视频标题',
      flex: 1,
      minWidth: 260,
      renderCell: ({ row }) => (
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" noWrap>{row.title || '未命名视频'}</Typography>
          <Typography variant="caption" color="text.secondary" noWrap>
            {row.videoId ? `itemId ${row.videoId}` : '后端未返回 itemId'}
          </Typography>
        </Box>
      ),
    },
    { field: 'viewCount', headerName: '播放量', width: 110, valueFormatter: (value: number) => fmt(value) },
    { field: 'likeCount', headerName: '点赞数', width: 100, valueFormatter: (value: number) => fmt(value) },
    { field: 'commentCount', headerName: '评论数', width: 100, valueFormatter: (value: number) => fmt(value) },
    { field: 'shareCount', headerName: '分享数', width: 100, valueFormatter: (value: number) => fmt(value) },
    {
      field: 'videoType',
      headerName: '视频类型',
      width: 110,
      renderCell: ({ value }) => <Chip label={getVideoTypeLabel(value)} color={value ? 'primary' : 'default'} size="small" variant="outlined" />,
    },
    { field: 'publishTime', headerName: '发布时间', width: 170, valueFormatter: (value: string) => formatDate(value) },
  ]

  const handleSearch = () => {
    setSearch({
      ...query,
      accountId: query.accountId ? Number(query.accountId) : undefined,
      title: query.title?.trim() || undefined,
      videoType: query.videoType || undefined,
      page: 0,
    })
  }

  const resetQuery = () => {
    const next = { page: 0, rows: 20 }
    setQuery(next)
    setSearch(next)
  }

  const searchSlot = (
    <>
      <TextField
        label="视频标题"
        size="small"
        value={query.title ?? ''}
        onChange={e => setQuery(q => ({ ...q, title: e.target.value }))}
        sx={{ width: 200 }}
      />
      <TextField
        label="账号ID"
        size="small"
        type="number"
        value={query.accountId ?? ''}
        onChange={e => setQuery(q => ({ ...q, accountId: e.target.value === '' ? undefined : Number(e.target.value) }))}
        sx={{ width: 130 }}
      />
      <TextField
        label="视频类型"
        size="small"
        select
        value={query.videoType ?? ''}
        onChange={e => setQuery(q => ({ ...q, videoType: e.target.value || undefined }))}
        sx={{ width: 130 }}
      >
        <MenuItem value="">全部</MenuItem>
        <MenuItem value="normal">普通视频</MenuItem>
        <MenuItem value="product">带货视频</MenuItem>
        <MenuItem value="live">直播切片</MenuItem>
        <MenuItem value="short">短视频</MenuItem>
      </TextField>
      <Button variant="contained" onClick={handleSearch}>搜索</Button>
      <Button onClick={resetQuery}>重置</Button>
    </>
  )

  return (
    <Box
      data-testid="douyin-videos-page"
      data-ready-endpoints={VIDEO_READY_ENDPOINTS}
      data-unsupported-endpoints={VIDEO_UNSUPPORTED_ENDPOINTS}
      data-search-list-only="true"
      data-no-local-video-fallback="true"
      data-no-browser-direct-scrape="true"
      sx={{ p: 2, height: 'calc(100vh - 48px)', display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <PageHeader
        title="抖音视频"
        subtitle="视频列表走抖音视频真实查询接口，账号过滤为空时展示当前权限范围内的可见视频。"
        breadcrumbs={[{ label: '抖音运营' }, { label: '视频管理' }]}
        actions={<Button variant="outlined" size="small" onClick={() => void refetch()} disabled={isFetching}>刷新</Button>}
      />

      <Grid container spacing={2}>
        {[
          { label: '当前页视频', value: rows.length, hint: `服务端总数 ${data?.total ?? 0}` },
          { label: '当前页播放', value: fmt(totalViews), hint: '按当前筛选结果汇总' },
          { label: '当前页点赞', value: fmt(totalLikes), hint: '当前页真实字段汇总' },
          { label: '爆款候选', value: hotCount, hint: '播放10万或点赞5万' },
        ].map(item => (
          <Grid item xs={6} md={3} key={item.label}>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h5" fontWeight={700}>{item.value}</Typography>
              <Typography variant="caption" color="text.secondary">{item.hint}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      {noAccountFilter && (
        <Alert severity="info" data-testid="douyin-videos-no-account-filter" data-no-single-account-sync="true">
          未选择账号时不会调用单账号同步链路，只展示当前登录用户可见账号下的视频。需要排查某个账号时请填入账号ID。
        </Alert>
      )}

      <Alert
        severity="info"
        data-testid="douyin-videos-boundary-contract"
        data-source-endpoint={VIDEO_SEARCH_ENDPOINT}
        data-server-filter-payload="true"
        data-no-detail-prefetch="true"
        data-no-local-video-fallback="true"
      >
        当前搜索条件只提交后端真实支持的 accountId、title、videoType 和分页字段。
      </Alert>

      <Alert
        severity="info"
        data-testid="douyin-videos-sync-downgrade"
        data-sync-endpoint="/douyin/video/sync"
        data-readonly-search-page="true"
        data-no-local-cover-fallback="true"
        data-no-browser-direct-scrape="true"
      >
        当前后端视频 VO 未返回封面 URL，列表展示 itemId 和互动指标；视频同步接口 `/douyin/video/sync` 每次按账号拉取开放平台首页最多 20 条视频。
      </Alert>

      {isError && (
        <Alert
          severity="error"
          data-testid="douyin-videos-list-error"
          data-no-local-video-fallback="true"
          action={<Button color="inherit" size="small" onClick={() => void refetch()}>重试</Button>}
        >
          {errorMessage}
        </Alert>
      )}

      {!isFetching && !isError && rows.length === 0 && (
        <Alert severity="warning" data-testid="douyin-videos-empty" data-no-local-video-fallback="true" data-no-browser-direct-scrape="true">
          当前筛选条件没有视频数据。若账号已授权但仍为空，请先在抖音账号页执行“同步视频”，或检查后端抖音开放平台降级日志。
        </Alert>
      )}

      <Box sx={{ flex: 1, minHeight: 0 }} data-testid="douyin-videos-grid-contract" data-source-endpoint={VIDEO_SEARCH_ENDPOINT} data-server-pagination="true" data-no-detail-prefetch="true">
        <StandardDataGrid
          rows={rows}
          columns={columns}
          loading={isFetching}
          rowCount={data?.total ?? 0}
          paginationMode="server"
          paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
          onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
          getRowId={row => row.id}
          searchSlot={searchSlot}
          slots={{ noRowsOverlay: DataGridEmptyOverlay }}
        />
      </Box>
    </Box>
  )
}
