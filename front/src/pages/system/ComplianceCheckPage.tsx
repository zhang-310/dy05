import { useMemo, useState } from 'react'
import {
  Box, Card, CardContent, TextField, Button, Typography,
  Stack, Select, MenuItem, Chip, CircularProgress, Alert,
} from '@mui/material'
import { GppGood as ComplianceIcon } from '@mui/icons-material'
import type { GridColDef } from '@mui/x-data-grid'
import { StandardDataGrid } from '@/components/base'
import request from '@/utils/request'

const LEVEL_COLOR: Record<string, 'error' | 'warning' | 'info'> = {
  high: 'error', medium: 'warning', low: 'info',
}

interface ViolationItem {
  matchedText: string; level: string; reason: string; reference: string; position: number
}
type ViolationRow = ViolationItem & { __rid: number }

export default function ComplianceCheckPage() {
  const [industry, setIndustry] = useState('cosmetics')
  const [text, setText] = useState('')
  const [results, setResults] = useState<ViolationItem[]>([])
  const [loading, setLoading] = useState(false)
  const [checked, setChecked] = useState(false)

  const handleCheck = async () => {
    if (!text.trim()) return
    setLoading(true); setChecked(false)
    try {
      const res = await request.post<ViolationItem[]>('/script/compliance/check', { text, industryCode: industry })
      setResults(res || []); setChecked(true)
    } catch { setResults([]) }
    finally { setLoading(false) }
  }

  const rows: ViolationRow[] = useMemo(() => results.map((v, i) => ({ ...v, __rid: i })), [results])

  const columns = useMemo<GridColDef<ViolationRow>[]>(() => [
    { field: 'matchedText', headerName: '违规文本', flex: 1, minWidth: 140,
      renderCell: p => <Typography color="error" fontWeight="bold" component="span">{p.value}</Typography> },
    { field: 'level', headerName: '等级', width: 90,
      renderCell: p => <Chip label={p.value} color={LEVEL_COLOR[p.value as string] ?? 'default'} size="small" /> },
    { field: 'reason', headerName: '原因', flex: 1.5, minWidth: 160 },
    { field: 'reference', headerName: '法规依据', flex: 1, minWidth: 120 },
    { field: 'position', headerName: '位置', width: 80, type: 'number' },
  ], [])

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 3, maxWidth: 960 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>合规检测</Typography>

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Typography variant="body2" fontWeight={500}>行业：</Typography>
              <Select size="small" value={industry} onChange={e => setIndustry(e.target.value)} sx={{ minWidth: 140 }}>
                <MenuItem value="cosmetics">护肤美妆</MenuItem>
                <MenuItem value="food">食品饮料</MenuItem>
                <MenuItem value="health">保健品</MenuItem>
                <MenuItem value="general">通用</MenuItem>
              </Select>
            </Stack>
            <TextField label="待检测文本" value={text} onChange={e => setText(e.target.value)}
              fullWidth multiline rows={6} placeholder="粘贴话术/脚本文案，检测是否符合行业广告法规" />
            <Button variant="contained" startIcon={<ComplianceIcon />}
              onClick={handleCheck} disabled={loading || !text.trim()} sx={{ alignSelf: 'flex-start' }}>
              {loading ? '检测中...' : '检测合规'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {loading && <CircularProgress sx={{ display: 'block', mx: 'auto' }} />}
      {checked && results.length === 0 && <Alert severity="success">未检测到违规内容，文案合规！</Alert>}
      {results.length > 0 && (
        <Card>
          <CardContent>
            <Typography variant="h6" color="error" gutterBottom>检测到 {results.length} 处违规</Typography>
            <Box sx={{ minHeight: 200 }}>
              <StandardDataGrid
                rows={rows} columns={columns} getRowId={r => String((r as ViolationRow).__rid)}
                hideFooter autoHeight disableColumnMenu sx={{ border: 'none', minHeight: 200 }} />
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
