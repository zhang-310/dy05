import {
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
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
import type { AuthRoleVO } from '@/types/auth'

export interface UserPageToolbarProps {
  searchInput: string
  onSearchInputChange: (v: string) => void
  onSearch: () => void
  onReset: () => void
  roleFilter: string
  onRoleFilterChange: (v: string) => void
  roles: AuthRoleVO[]
  statusFilter: number | ''
  onStatusFilterChange: (v: number | '') => void
  statusCounts: { all: number; normal: number; banned: number }
  hasActiveFilter: boolean
  onAddClick: () => void
}

export function UserPageToolbar(props: UserPageToolbarProps) {
  const p = props

  return (
    <Box sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
      <Box sx={{ px: 1.5, py: 0.75, display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
          用户管理
        </Typography>
        <Box sx={{ flex: 1 }} />
        <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={p.onAddClick}>
          新增用户
        </Button>
        <Box sx={{ borderLeft: '1px solid', borderColor: 'divider', height: 24, mx: 0.25 }} />
        <GridToolbarColumnsButton />
        <GridToolbarDensitySelector />
        <GridToolbarExport />
      </Box>
      <Box sx={{ px: 1.5, pb: 1, display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          size="small"
          placeholder="用户名/昵称"
          value={p.searchInput}
          onChange={(e) => p.onSearchInputChange(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && p.onSearch()}
          sx={{ minWidth: 160 }}
        />
        <FormControl size="small" sx={{ minWidth: 100 }}>
          <InputLabel>角色</InputLabel>
          <Select
            value={p.roleFilter}
            label="角色"
            onChange={(e) => p.onRoleFilterChange(e.target.value)}
          >
            <MenuItem value="">全部</MenuItem>
            {p.roles.map((r) => (
              <MenuItem key={r.roleCode} value={r.roleCode}>
                {r.roleName || r.roleCode}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 80 }}>
          <InputLabel>状态</InputLabel>
          <Select
            value={p.statusFilter}
            label="状态"
            onChange={(e) => p.onStatusFilterChange(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <MenuItem value="">全部</MenuItem>
            <MenuItem value={0}>正常</MenuItem>
            <MenuItem value={1}>封禁</MenuItem>
          </Select>
        </FormControl>
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
