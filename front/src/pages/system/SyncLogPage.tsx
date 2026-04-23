import { useState } from 'react'
import {
  Box, Chip, Drawer, IconButton, Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader } from '@/components/base'
import { systemApi, type SyncLog } from '@/api/system'
import { useQuery } from '@tanstack/react-query'

function StatusChip({ status }: { status: string }) {
  const color = status === 'SUCCESS' ? 'success' : status === 'FAILED' ? 'error' : 'default'
  const label = status === 'SUCCESS' ? '成功' : status === 'FAILED' ? '失败' : status
  return <Chip label={label} color={color} size="small" />
}

export default function SyncLogPage() {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [selected, setSelected] = useState<SyncLog | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['sync-log', page, pageSize],
    queryFn: () => systemApi.syncLogList({ page, rows: pageSize }),
  })

  const rows: SyncLog[] = data?.list ?? []
  const total = data?.total ?? 0

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    { field: 'syncType', headerName: '同步类型', width: 150 },
    {
      field: 'status', headerName: '状态', width: 100,
      renderCell: (p) => <StatusChip status={String(p.value ?? '')} />,
    },
    { field: 'totalCount', headerName: '总数', width: 90 },
    { field: 'successCount', headerName: '成功', width: 90 },
    { field: 'failCount', headerName: '失败', width: 90 },
    { field: 'startTime', headerName: '开始时间', width: 180 },
    { field: 'endTime', headerName: '结束时间', width: 180 },
    { field: 'createTime', headerName: '创建时间', width: 180 },
  ]

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="同步日志" subtitle="数据同步执行记录" />
      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isFetching}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
        pageSizeOptions={[10, 20, 50]}
        disableRowSelectionOnClick
        getRowId={(r) => (r as SyncLog).id}
        onRowClick={(p) => setSelected(p.row as SyncLog)}
        autoHeight
        sx={{ mt: 2, cursor: 'pointer' }}
      />

      <Drawer anchor="right" open={!!selected} onClose={() => setSelected(null)}
        PaperProps={{ sx: { width: 480, p: 3 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" sx={{ flex: 1 }}>同步详情</Typography>
          <IconButton onClick={() => setSelected(null)}><CloseIcon /></IconButton>
        </Box>
        {selected && [
          { label: 'ID', value: String(selected.id) },
          { label: '同步类型', value: selected.syncType },
          { label: '状态', value: <StatusChip status={selected.status} /> },
          { label: '总数', value: String(selected.totalCount) },
          { label: '成功数', value: String(selected.successCount) },
          { label: '失败数', value: String(selected.failCount) },
          { label: '开始时间', value: selected.startTime },
          { label: '结束时间', value: selected.endTime },
          { label: '错误信息', value: selected.errorMessage ?? '-' },
        ].map((row) => (
          <Box key={row.label} sx={{ display: 'flex', py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography variant="body2" color="text.secondary" sx={{ width: 100, flexShrink: 0 }}>{row.label}</Typography>
            <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
              {typeof row.value === 'string' ? row.value : row.value}
            </Typography>
          </Box>
        ))}
      </Drawer>
    </Box>
  )
}
