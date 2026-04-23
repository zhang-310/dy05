import {
  DataGrid,
  DataGridProps,
  GridToolbarContainer,
  GridToolbarColumnsButton,
  GridToolbarFilterButton,
  GridToolbarDensitySelector,
  GridToolbarExport,
} from '@mui/x-data-grid'
import type { GridSlotsComponent, GridToolbarContainerProps } from '@mui/x-data-grid'
import type { GridSlotProps } from '@mui/x-data-grid'
import { Box, Divider } from '@mui/material'
import { dataGridLocale } from '@/utils/datagrid-locale'

// MUI DataGrid's toolbar slot accepts GridToolbarProps. We extend it with our custom props.
// BuiltinToolbar satisfies the GridToolbarContainerProps interface (React.HTMLAttributes<HTMLDivElement>).
interface BuiltinToolbarProps extends GridToolbarContainerProps {
  searchSlot?: React.ReactNode
  actionSlot?: React.ReactNode
}

function BuiltinToolbar(props: BuiltinToolbarProps) {
  const { searchSlot, actionSlot } = props
  return (
    <GridToolbarContainer>
      {searchSlot && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', flex: 1, minWidth: 0 }}>
          {searchSlot}
        </Box>
      )}

      {!searchSlot && <Box sx={{ flex: 1 }} />}

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexShrink: 0 }}>
        {actionSlot}
        {actionSlot && <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />}
        <GridToolbarColumnsButton />
        <GridToolbarFilterButton />
        <GridToolbarDensitySelector />
        <GridToolbarExport />
      </Box>
    </GridToolbarContainer>
  )
}

export interface StandardDataGridProps extends Omit<DataGridProps, 'localeText'> {
  toolbar?: React.ComponentType<any> | null
  searchSlot?: React.ReactNode
  actionSlot?: React.ReactNode
}

export function StandardDataGrid({
  toolbar,
  searchSlot,
  actionSlot,
  slots,
  slotProps,
  paginationMode,
  rowCount,
  ...rest
}: StandardDataGridProps) {
  const ToolbarComponent = toolbar !== undefined ? toolbar : BuiltinToolbar

  /** MUI X：rowCount 仅在与 paginationMode="server" 联用时有效，否则会控制台告警 */
  const serverPaginationProps = paginationMode === 'server' ? { rowCount } : {}

  return (
    <Box sx={{ position: 'relative', height: '100%', width: '100%', minHeight: 240 }}>
      <DataGrid
        localeText={dataGridLocale}
        pageSizeOptions={[10, 20, 30, 50, 100]}
        disableRowSelectionOnClick
        autoHeight={false}
        style={{ position: 'absolute', inset: 0, minHeight: 240 }}
        slots={{
          toolbar: ToolbarComponent ?? undefined,
          ...slots,
        } as GridSlotsComponent}
        slotProps={{
          toolbar: { searchSlot, actionSlot } as GridSlotProps['toolbar'],
          ...slotProps,
        }}
        paginationMode={paginationMode}
        {...serverPaginationProps}
        {...rest}
      />
    </Box>
  )
}

export default StandardDataGrid
