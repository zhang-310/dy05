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
import SearchIcon from '@mui/icons-material/Search'

export interface LoginLogToolbarProps {
  isAdmin: boolean
  userIdInput: string
  onUserIdInputChange: (v: string) => void
  statusFilter: number | ''
  onStatusFilterChange: (v: number | '') => void
  onSearch: () => void
  onReset: () => void
  hasActiveFilter: boolean
  total: number
}

export function LoginLogToolbar(props: LoginLogToolbarProps) {
  const p = props

  return (
    <Box sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
      <Box sx={{ px: 1.5, py: 0.75, display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
          登录日志
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
          共 {p.total} 条
        </Typography>
        <Box sx={{ flex: 1 }} />
        <GridToolbarColumnsButton />
        <GridToolbarDensitySelector />
        <GridToolbarExport />
      </Box>
      {p.isAdmin && (
        <Box sx={{ px: 1.5, pb: 1, display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="用户 ID"
            type="number"
            value={p.userIdInput}
            onChange={(e) => p.onUserIdInputChange(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && p.onSearch()}
            sx={{ minWidth: 120 }}
          />
          <FormControl size="small" sx={{ minWidth: 80 }}>
            <InputLabel>状态</InputLabel>
            <Select
              value={p.statusFilter}
              label="状态"
              onChange={(e) => p.onStatusFilterChange(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">全部</MenuItem>
              <MenuItem value={1}>成功</MenuItem>
              <MenuItem value={0}>失败</MenuItem>
            </Select>
          </FormControl>
          <Button size="small" variant="contained" startIcon={<SearchIcon />} onClick={p.onSearch}>
            查询
          </Button>
          {p.hasActiveFilter && (
            <Button size="small" variant="outlined" onClick={p.onReset}>
              重置
            </Button>
          )}
        </Box>
      )}
    </Box>
  )
}
