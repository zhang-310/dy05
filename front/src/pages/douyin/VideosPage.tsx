import { useState } from 'react'
import { Box, TextField, Button, Chip } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import { useQuery } from '@tanstack/react-query'
import { formatDate } from '@/utils/date'
import request from '@/utils/request'
import type { PageResult } from '@/types/common'

interface DyVideo {
  id: number
  accountId: number
  videoId: string
  title: string
  coverUrl: string
  playCount: number
  likeCount: number
  commentCount: number
  shareCount: number
  duration: number
  status: number
  publishTime: string
  createTime: string
}
interface VideoQuery { page?: number; rows?: number; accountId?: number; title?: string }

const videoApi = {
  list: (p: VideoQuery) => request.post<PageResult<DyVideo>>('/douyin/video/search', p),
}

export default function VideosPage() {
  const [search, setSearch] = useState<VideoQuery>({ page: 0, rows: 20 })
  const [query, setQuery] = useState(search)
  const { data, isFetching } = useQuery({ queryKey: ['dy-videos', search], queryFn: () => videoApi.list(search) })

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: '视频标题', flex: 1, minWidth: 200 },
    { field: 'playCount', headerName: '播放量', width: 100 },
    { field: 'likeCount', headerName: '点赞数', width: 100 },
    { field: 'commentCount', headerName: '评论数', width: 100 },
    { field: 'shareCount', headerName: '分享数', width: 100 },
    { field: 'duration', headerName: '时长(s)', width: 90 },
    {
      field: 'status', headerName: '状态', width: 90,
      renderCell: ({ value }) => <Chip label={value === 1 ? '发布' : '下架'} color={value === 1 ? 'success' : 'default'} size="small" />,
    },
    { field: 'publishTime', headerName: '发布时间', width: 160, valueFormatter: (v: string) => formatDate(v) },
  ]

  const searchSlot = (
    <>
      <TextField label="视频标题" size="small" value={query.title ?? ''} onChange={e => setQuery(q => ({ ...q, title: e.target.value }))} sx={{ width: 180 }} />
      <Button variant="contained" onClick={() => setSearch({ ...query, page: 0 })}>搜索</Button>
      <Button onClick={() => { setQuery({ page: 0, rows: 20 }); setSearch({ page: 0, rows: 20 }) }}>重置</Button>
    </>
  )

  return (
    <Box sx={{ height: 'calc(100vh - 48px - 32px)', display: 'flex', flexDirection: 'column' }}>
      <StandardDataGrid
        rows={data?.list ?? []}
        columns={columns}
        loading={isFetching}
        rowCount={data?.total ?? 0}
        paginationMode="server"
        paginationModel={{ page: search.page ?? 0, pageSize: search.rows ?? 20 }}
        onPaginationModelChange={m => setSearch(s => ({ ...s, page: m.page, rows: m.pageSize }))}
        searchSlot={searchSlot}
      />
    </Box>
  )
}
