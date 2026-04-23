import { DataGrid, type GridColDef, type GridRowsProp } from '@mui/x-data-grid'

interface DataTableProps {
  rows: GridRowsProp
  columns: GridColDef[]
  loading?: boolean
  pageSize?: number
  pageSizeOptions?: number[]
  onRowClick?: (params: { id: string | number }) => void
}

export function DataTable({
  rows,
  columns,
  loading = false,
  pageSize = 20,
  pageSizeOptions = [10, 20, 50],
  onRowClick,
}: DataTableProps) {
  return (
    <DataGrid
      rows={rows}
      columns={columns}
      loading={loading}
      pageSizeOptions={pageSizeOptions}
      initialState={{ pagination: { paginationModel: { pageSize } } }}
      onRowClick={onRowClick}
      disableRowSelectionOnClick
      autoHeight
      sx={{ minHeight: 400 }}
    />
  )
}
