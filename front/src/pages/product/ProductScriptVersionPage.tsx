import { useCallback, useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Box, Button, Chip, IconButton, Tooltip } from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { DataGrid, GridColDef, GridToolbarContainer } from '@mui/x-data-grid'
import { useToast } from '@/contexts/ToastContext'
import { PageHeader } from '@/components/base'
import {
  getProductScriptVersionHistory,
  rollbackProductScriptVersion,
  type ProductScriptVersion,
} from '@/api/product-script-version'

interface ToolbarProps {
  onBack: () => void
}

function Toolbar(props: ToolbarProps) {
  const { onBack } = props
  return (
    <GridToolbarContainer sx={{ px: 1, py: 0.5 }}>
      <Button size="small" startIcon={<ArrowBackIcon />} onClick={onBack}>返回商品列表</Button>
    </GridToolbarContainer>
  )
}

function buildToolbar(onBack: () => void) {
  return function ToolbarWrapper() {
    return <Toolbar onBack={onBack} />
  }
}

export default function ProductScriptVersionPage() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const [rows, setRows] = useState<ProductScriptVersion[]>([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(() => {
    if (!productId) return
    setLoading(true)
    getProductScriptVersionHistory(Number(productId))
      .then((res) => setRows(res || []))
      .catch(() => toast('加载版本失败', 'error'))
      .finally(() => setLoading(false))
  }, [productId, toast])

  useEffect(() => { load() }, [load])

  const handleRollback = async (versionNumber: number) => {
    if (!productId) return
    try {
      await rollbackProductScriptVersion(Number(productId), versionNumber)
      toast('已回滚到该版本', 'success')
      load()
    } catch {
      toast('回滚失败', 'error')
    }
  }

  const columns: GridColDef[] = [
    { field: 'versionNumber', headerName: '版本号', width: 100 },
    {
      field: 'scriptContent', headerName: '话术内容', flex: 1,
      renderCell: (p) => (
        <Tooltip title={String(p.value || '')}>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block', maxWidth: 400 }}>
            {String(p.value || '').slice(0, 80)}{String(p.value || '').length > 80 ? '…' : ''}
          </span>
        </Tooltip>
      ),
    },
    {
      field: 'status', headerName: '状态', width: 120,
      renderCell: (p) => (
        <Chip label={p.value === 'active' ? '当前版本' : String(p.value || '历史')} size="small"
          color={p.value === 'active' ? 'success' : 'default'} />
      ),
    },
    {
      field: 'score', headerName: '评分', width: 100,
      renderCell: (p) => p.value ? `${Number(p.value).toFixed(1)}分` : '–',
    },
    { field: 'createTime', headerName: '创建时间', width: 180 },
    {
      field: 'actions', headerName: '操作', width: 120, sortable: false,
      renderCell: (p) => (
        <Button size="small" variant="outlined"
          disabled={(p.row as ProductScriptVersion).status === 'active'}
          onClick={() => handleRollback((p.row as ProductScriptVersion).versionNumber)}>
          激活
        </Button>
      ),
    },
  ]

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
        <IconButton onClick={() => navigate('/admin/product/list')}><ArrowBackIcon /></IconButton>
        <PageHeader title={`商品话术版本`} subtitle={`商品 ID: ${productId}`} />
      </Box>
      <Box sx={{ height: 600 }}>
        <DataGrid
          rows={rows}
          columns={columns}
          loading={loading}
          slots={{ toolbar: buildToolbar(() => navigate('/admin/product/list')) }}
          slotProps={undefined}
          disableRowSelectionOnClick
          getRowId={(r) => (r as ProductScriptVersion).id}
        />
      </Box>
    </Box>
  )
}
