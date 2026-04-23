import { useState } from 'react'
import {
  Box, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  MenuItem, TextField,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid, PageHeader } from '@/components/base'
import { productApi, type SalesHistory } from '@/api/product'
import { useToast } from '@/contexts/ToastContext'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const CHANNEL_OPTIONS = [
  { value: 'douyin_live', label: '抖音直播' },
  { value: 'store', label: '门店' },
  { value: 'manual', label: '手动录入' },
]

interface ToolbarProps {
  onAdd: () => void
}

function Toolbar(props: ToolbarProps) {
  const { onAdd } = props
  return (
    <Box sx={{ px: 1, py: 0.5, display: 'flex', gap: 1 }}>
      <Button size="small" startIcon={<AddIcon />} onClick={onAdd}>录入记录</Button>
    </Box>
  )
}

function buildToolbar(onAdd: () => void) {
  return function ToolbarWrapper() {
    return <Toolbar onAdd={onAdd} />
  }
}

export default function SalesHistoryPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [productId, setProductId] = useState<number | ''>('')
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({ productId: '' as number | '', revenue: '', quantity: '', platform: 'manual', saleDate: '' })

  const { data, isFetching } = useQuery({
    queryKey: ['sales-history', page, pageSize, productId],
    queryFn: () => productApi.salesHistorySearch({
      page, rows: pageSize,
      productId: productId !== '' ? productId : undefined,
    }),
  })

  const { data: products } = useQuery({
    queryKey: ['products-simple'],
    queryFn: () => productApi.list({ page: 0, rows: 200 }),
  })

  const saveMut = useMutation({
    mutationFn: () => productApi.salesHistorySave({
      productId: form.productId as number,
      revenue: Number(form.revenue),
      quantity: Number(form.quantity),
      platform: form.platform,
      saleDate: form.saleDate,
    }),
    onSuccess: () => {
      toast('保存成功', 'success')
      setFormOpen(false)
      setForm({ productId: '', revenue: '', quantity: '', platform: 'manual', saleDate: '' })
      qc.invalidateQueries({ queryKey: ['sales-history'] })
    },
    onError: (e: Error) => toast(e.message, 'error'),
  })

  const rows: SalesHistory[] = data?.list ?? []
  const total = data?.total ?? 0
  const productList = products?.list ?? []

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 80 },
    {
      field: 'productId', headerName: '商品', flex: 1,
      renderCell: (p) => {
        const prod = productList.find((x) => x.id === p.value)
        return prod ? String(prod.productName ?? prod.id) : String(p.value)
      },
    },
    {
      field: 'revenue', headerName: '销售金额', width: 130,
      renderCell: (p) => `¥${Number(p.value ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
    },
    { field: 'quantity', headerName: '数量', width: 90 },
    {
      field: 'platform', headerName: '渠道', width: 120,
      renderCell: (p) => CHANNEL_OPTIONS.find((o) => o.value === p.value)?.label ?? String(p.value ?? '-'),
    },
    { field: 'saleDate', headerName: '销售时间', width: 180 },
    { field: 'createTime', headerName: '录入时间', width: 180 },
  ]

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="销售记录" subtitle="商品销售历史数据" />

      <Box sx={{ mb: 2, display: 'flex', gap: 2, alignItems: 'center' }}>
        <TextField
          select size="small" label="商品筛选" value={productId}
          onChange={(e) => { setProductId(e.target.value === '' ? '' : Number(e.target.value)); setPage(0) }}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">全部商品</MenuItem>
          {productList.map((p) => (
            <MenuItem key={p.id} value={p.id}>{String(p.productName ?? p.id)}</MenuItem>
          ))}
        </TextField>
      </Box>

      <StandardDataGrid
        rows={rows}
        columns={columns}
        loading={isFetching}
        rowCount={total}
        paginationMode="server"
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
        pageSizeOptions={[10, 20, 50]}
        slots={{ toolbar: buildToolbar(() => setFormOpen(true)) }}
        slotProps={undefined}
        disableRowSelectionOnClick
        getRowId={(r) => (r as SalesHistory).id ?? 0}
        autoHeight
      />

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>录入销售记录</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField
            select label="商品" value={form.productId}
            onChange={(e) => setForm((f) => ({ ...f, productId: Number(e.target.value) }))}
            size="small" fullWidth
          >
            {productList.map((p) => (
              <MenuItem key={p.id} value={p.id}>{String(p.productName ?? p.id)}</MenuItem>
            ))}
          </TextField>
          <TextField label="销售金额" size="small" fullWidth value={form.revenue}
            onChange={(e) => setForm((f) => ({ ...f, revenue: e.target.value }))} />
          <TextField label="数量" size="small" fullWidth value={form.quantity}
            onChange={(e) => setForm((f) => ({ ...f, quantity: e.target.value }))} />
          <TextField select label="渠道" size="small" fullWidth value={form.platform}
            onChange={(e) => setForm((f) => ({ ...f, platform: e.target.value }))}>
            {CHANNEL_OPTIONS.map((o) => (
              <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
            ))}
          </TextField>
          <TextField label="销售时间" size="small" fullWidth type="datetime-local"
            InputLabelProps={{ shrink: true }} value={form.saleDate}
            onChange={(e) => setForm((f) => ({ ...f, saleDate: e.target.value }))} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>取消</Button>
          <Button variant="contained" onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
