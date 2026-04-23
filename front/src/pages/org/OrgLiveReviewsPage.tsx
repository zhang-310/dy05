import { useState } from 'react'
import { Box, Tab, Tabs, Chip, Button, Stack } from '@mui/material'
import type { GridColDef } from '@mui/x-data-grid'
import { useQuery } from '@tanstack/react-query'
import { StandardDataGrid } from '@/components/base/StandardDataGrid'
import request from '@/utils/request'

interface LiveReview {
  id: number
  sessionId: number
  sessionTitle: string
  talentName: string
  reviewStatus: number
  score: number
  gmv: number
  createTime: string
}

const reviewApi = {
  list: (params: Record<string, unknown>) => request.post<{ list: LiveReview[]; total: number }>('/org/live-review/list', params),
}

const STATUS_TABS = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
]

export default function OrgLiveReviewsPage() {
  const [tab, setTab] = useState(0)
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['org-live-reviews', tab, page],
    queryFn: () => reviewApi.list({ reviewStatus: tab, page, rows: 20 }),
  })
  const rows: LiveReview[] = data?.list ?? []
  const total = data?.total ?? 0

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'sessionTitle', headerName: '场次名称', flex: 1.5, minWidth: 160 },
    { field: 'talentName', headerName: '主播', width: 120 },
    { field: 'score', headerName: '评分', width: 90,
      renderCell: ({ value }) => (
        <Chip
          label={value ? `${Number(value).toFixed(1)}分` : '—'}
          size="small"
          color={Number(value) >= 8 ? 'success' : Number(value) >= 6 ? 'warning' : 'error'}
        />
      ) },
    { field: 'gmv', headerName: 'GMV', width: 120,
      renderCell: ({ value }) => `¥${Number(value ?? 0).toLocaleString()}` },
    { field: 'reviewStatus', headerName: '状态', width: 100,
      renderCell: ({ value }) => {
        const s = STATUS_TABS.find(t => t.value === Number(value))
        return <Chip label={s?.label ?? '未知'} size="small"
          color={Number(value) === 1 ? 'success' : Number(value) === 2 ? 'error' : 'default'} />
      } },
    { field: 'createTime', headerName: '创建时间', width: 160 },
    { field: '_actions', headerName: '操作', width: 180, sortable: false,
      renderCell: ({ row: r }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small">查看详情</Button>
          {Number((r as LiveReview).reviewStatus) === 0 && (
            <>
              <Button size="small" color="success">通过</Button>
              <Button size="small" color="error">拒绝</Button>
            </>
          )}
        </Stack>
      ) },
  ]

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, height: 'calc(100vh - 120px)' }}>
      <Tabs value={tab} onChange={(_, v) => { setTab(v); setPage(0) }} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        {STATUS_TABS.map(t => <Tab key={t.value} label={t.label} value={t.value} />)}
      </Tabs>

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isLoading}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize: 20 }}
        onPaginationModelChange={m => setPage(m.page)}
        pageSizeOptions={[20]}
      />
    </Box>
  )
}
