import { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Alert,
  List,
  ListItem,
  ListItemText,
  CircularProgress,
} from '@mui/material'
import GavelIcon from '@mui/icons-material/Gavel'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { PageHeader } from '@/components/base'
import { scriptApi } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'

const SCOPE_OPTIONS = [
  { value: 'all', label: '全部场景' },
  { value: 'live', label: '直播' },
  { value: 'video', label: '短视频' },
]

interface ViolationItem {
  word: string
  position?: number
  length?: number
  reason?: string
  level?: number
  replacement?: string
  source?: string
}

export default function ViolationCheckPage() {
  const toast = useToast()
  const [text, setText] = useState('')
  const [scope, setScope] = useState('all')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<{ violations: ViolationItem[] } | null>(null)

  const handleCheck = async () => {
    if (!text.trim()) {
      toast('请输入待检测文本', 'error')
      return
    }
    setLoading(true)
    setResult(null)
    try {
      const data = await scriptApi.violationCheck(text.trim())
      setResult(data as { violations: ViolationItem[] })
    } catch (e: unknown) {
      toast((e as { message?: string })?.message ?? '检测失败', 'error')
    } finally {
      setLoading(false)
    }
  }

  const hasViolation = result && result.violations.length > 0

  return (
    <Box>
      <PageHeader
        title="违规检测"
        breadcrumbs={[{ label: '话术' }, { label: '违规检测' }]}
      />
      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 2 }}>
            输入文本或话术内容，系统将检测公共违规词库和个人违规词库中的违规词
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>检测范围</InputLabel>
              <Select value={scope} label="检测范围" onChange={(e) => setScope(e.target.value)}>
                {SCOPE_OPTIONS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
              </Select>
            </FormControl>
          </Box>
          <TextField
            fullWidth
            multiline
            minRows={6}
            label="待检测文本"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="输入话术文本…"
          />
          <Box sx={{ mt: 2 }}>
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <GavelIcon />}
              onClick={handleCheck}
              disabled={loading || !text.trim()}
            >
              开始检测
            </Button>
          </Box>
        </CardContent>
      </Card>

      {result && (
        <Card variant="outlined">
          <CardContent>
            {hasViolation ? (
              <>
                <Alert severity="error" sx={{ mb: 2 }}>
                  发现 {result.violations.length} 处违规词
                </Alert>
                <List dense>
                  {result.violations.map((v, i) => (
                    <ListItem key={i} sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                            <Chip color="error" size="small" label={v.word} />
                            {v.replacement && (
                              <Typography variant="body2" color="text.secondary">
                                建议替换: {v.replacement}
                              </Typography>
                            )}
                            {v.source && (
                              <Chip size="small" variant="outlined" label={v.source === 'public' ? '公共库' : '个人库'} />
                            )}
                          </Box>
                        }
                        secondary={v.reason ? `原因: ${v.reason}` : undefined}
                      />
                    </ListItem>
                  ))}
                </List>
              </>
            ) : (
              <Alert severity="success" icon={<CheckCircleIcon />}>
                未发现违规词
              </Alert>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
