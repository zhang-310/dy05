import { useState } from 'react'
import {
  Box, Card, CardContent, TextField, Button, Typography,
  CircularProgress, Alert, Stack, Chip, Divider,
} from '@mui/material'
import { TipsAndUpdates as OptimizeIcon, ContentCopy as CopyIcon } from '@mui/icons-material'
import { PageHeader } from '@/components/base'
import { scriptApi } from '@/api/script'
import { useToast } from '@/contexts/ToastContext'

export default function ScriptOptimizationPage() {
  const toast = useToast()
  const [original, setOriginal] = useState('')
  const [goal, setGoal] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<ScriptOptimizationResult | null>(null)

interface ScriptOptimizationResult {
  optimizedScript?: string
  improvements?: string[]
  score?: number
  analysis?: string
}

interface ScriptOptimizationResponse {
  content?: string
  improvements?: string[]
  score?: number
  analysis?: string
}

  const handleOptimize = async () => {
    if (!original) { toast('请输入原始话术', 'warning'); return }
    setLoading(true)
    setResult(null)
    try {
      const res = await scriptApi.generate({ originalScript: original, optimizationGoal: goal, mode: 'optimize' }) as ScriptOptimizationResponse
      setResult({
        optimizedScript: res.content != null ? String(res.content) : undefined,
        improvements: res.improvements,
        score: res.score,
      })
    } catch {
      toast('优化失败，请重试', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text).then(() => toast('已复制', 'success'))
  }

  return (
    <Box sx={{ bgcolor: 'var(--color-surface-dark)', minHeight: '100vh', p: 'var(--spacing-lg)' }}>
      <PageHeader
        title="话术优化"
        breadcrumbs={[{ label: '话术管理' }, { label: '话术优化' }]}
      />
      <Card sx={{ mb: 'var(--spacing-lg)', bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
        <CardContent sx={{ p: 'var(--spacing-lg)' }}>
          <Stack spacing={2}>
            <TextField
              label="原始话术" required multiline rows={6}
              value={original} onChange={(e) => setOriginal(e.target.value)}
              placeholder="粘贴需要优化的话术内容..."
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 'var(--border-radius-lg)',
                  bgcolor: 'var(--color-surface-dark)',
                  color: 'var(--color-text-primary)',
                  '& fieldset': { borderColor: 'var(--color-surface-light)' },
                  '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                }
              }}
            />
            <TextField
              label="优化目标（可选）"
              value={goal} onChange={(e) => setGoal(e.target.value)}
              placeholder="例如：增强紧迫感、提升亲和力、突出价格优势..."
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 'var(--border-radius-lg)',
                  bgcolor: 'var(--color-surface-dark)',
                  color: 'var(--color-text-primary)',
                  '& fieldset': { borderColor: 'var(--color-surface-light)' },
                  '&:hover fieldset': { borderColor: 'var(--color-primary)' }
                }
              }}
            />
            <Button
              variant="contained"
              startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <OptimizeIcon />}
              onClick={handleOptimize}
              disabled={loading || !original}
              sx={{
                alignSelf: 'flex-start',
                height: '44px',
                borderRadius: 'var(--border-radius-lg)',
                bgcolor: 'var(--color-primary)',
                color: '#000',
                fontWeight: 600,
                '&:hover': { bgcolor: 'var(--color-primary-dark)' }
              }}
            >
              {loading ? 'AI 优化中...' : 'AI 优化话术'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {loading && <CircularProgress sx={{ display: 'block', mx: 'auto', my: 4 }} />}

      {!!result && !loading && (
        <Card sx={{ bgcolor: 'var(--color-surface)', borderRadius: 'var(--border-radius-xl)', border: '1px solid var(--color-surface-light)', boxShadow: 'var(--shadow-elevation-1)' }}>
          <CardContent sx={{ p: 'var(--spacing-lg)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 'var(--spacing-md)' }}>
              <Typography variant="h6" sx={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>优化结果</Typography>
              <Stack direction="row" spacing={1}>
                {!!result.improvements && (
                  <Chip size="small" sx={{ bgcolor: 'var(--color-success)', color: '#fff', fontWeight: 600 }} label={`${result.improvements.length} 处改进`} />
                )}
                <Button
                  size="small" startIcon={<CopyIcon />}
                  onClick={() => handleCopy(String(result.optimizedScript ?? JSON.stringify(result)))}
                  sx={{ color: 'var(--color-primary)' }}
                >
                  复制
                </Button>
              </Stack>
            </Box>
            <Divider sx={{ mb: 'var(--spacing-md)', borderColor: 'var(--color-surface-light)' }} />
            {!!result.analysis && (
              <Alert severity="info" sx={{ mb: 'var(--spacing-md)', bgcolor: 'rgba(59, 130, 246, 0.1)' }}>{String(result.analysis)}</Alert>
            )}
            <Typography component="pre" sx={{
              whiteSpace: 'pre-wrap',
              fontFamily: 'monospace',
              lineHeight: 'var(--line-height-base)',
              color: 'var(--color-text-primary)',
              p: 'var(--spacing-md)',
              bgcolor: 'var(--color-surface-dark)',
              borderRadius: 'var(--border-radius-lg)',
              border: '1px solid var(--color-surface-light)'
            }}>
              {String(result.optimizedScript ?? JSON.stringify(result, null, 2))}
            </Typography>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}
