import {
  Box,
  Button,
  TextField,
  Typography,
} from '@mui/material'
import {
  GridToolbarColumnsButton,
  GridToolbarDensitySelector,
  GridToolbarExport,
} from '@mui/x-data-grid'
import AddIcon from '@mui/icons-material/Add'
import SearchIcon from '@mui/icons-material/Search'

export interface RolePageToolbarProps {
  roleCodeFilter: string
  onRoleCodeFilterChange: (v: string) => void
  roleNameFilter: string
  onRoleNameFilterChange: (v: string) => void
  onSearch: () => void
  onReset: () => void
  hasActiveFilter: boolean
  onAddClick: () => void
}

export function RolePageToolbar(props: RolePageToolbarProps) {
  const p = props

  return (
    <Box sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
      <Box sx={{ px: 1.5, py: 0.75, display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
          角色管理
        </Typography>
        <Box sx={{ flex: 1 }} />
        <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={p.onAddClick}>
          新增角色
        </Button>
        <Box sx={{ borderLeft: '1px solid', borderColor: 'divider', height: 24, mx: 0.25 }} />
        <GridToolbarColumnsButton />
        <GridToolbarDensitySelector />
        <GridToolbarExport />
      </Box>
      <Box sx={{ px: 1.5, pb: 1, display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          size="small"
          placeholder="角色编码"
          value={p.roleCodeFilter}
          onChange={(e) => p.onRoleCodeFilterChange(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && p.onSearch()}
          sx={{ minWidth: 120 }}
        />
        <TextField
          size="small"
          placeholder="角色名"
          value={p.roleNameFilter}
          onChange={(e) => p.onRoleNameFilterChange(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && p.onSearch()}
          sx={{ minWidth: 120 }}
        />
        <Button size="small" variant="contained" startIcon={<SearchIcon />} onClick={p.onSearch}>
          搜索
        </Button>
        {p.hasActiveFilter && (
          <Button size="small" variant="outlined" onClick={p.onReset}>
            重置
          </Button>
        )}
      </Box>
    </Box>
  )
}
